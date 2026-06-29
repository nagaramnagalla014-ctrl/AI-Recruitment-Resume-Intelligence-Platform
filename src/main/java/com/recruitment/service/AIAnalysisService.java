package com.recruitment.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitment.model.CandidateAnalysis;
import com.recruitment.model.JobDescription;
import com.recruitment.model.Resume;
import com.recruitment.repository.CandidateAnalysisRepository;
import com.recruitment.repository.JobDescriptionRepository;
import com.recruitment.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI-powered analysis service that computes match scores between resumes and jobs.
 * Uses OpenAI GPT-4o for generating summaries and interview questions.
 * Falls back to rule-based analysis when API key is absent or API call fails.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIAnalysisService {

    private final ResumeRepository resumeRepository;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final CandidateAnalysisRepository analysisRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${openai.api-key:#{null}}")
    private String openAiApiKey;

    @Value("${openai.model:gpt-4o}")
    private String openAiModel;

    @Value("${openai.api-url:https://api.openai.com/v1/chat/completions}")
    private String openAiApiUrl;

    /**
     * Analyze a candidate's resume against a specific job description.
     * Computes match scores, identifies strengths/gaps, generates interview questions.
     *
     * @param resumeId The UUID of the resume to analyze
     * @param jobId    The UUID of the job description to match against
     * @return The saved CandidateAnalysis entity
     */
    @Transactional
    public CandidateAnalysis analyzeCandidate(UUID resumeId, UUID jobId) {
        log.info("Analyzing candidate {} for job {}", resumeId, jobId);

        Resume resume = resumeRepository.findById(resumeId)
            .orElseThrow(() -> new IllegalArgumentException("Resume not found: " + resumeId));
        JobDescription job = jobDescriptionRepository.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        // Compute skill match using Jaccard similarity
        Set<String> candidateSkills = parseSkillSet(resume.getSkills());
        Set<String> requiredSkills = parseSkillSet(job.getRequiredSkills());
        Set<String> preferredSkills = parseSkillSet(job.getPreferredSkills());

        double skillMatchPct = computeSkillMatchPercentage(candidateSkills, requiredSkills);
        double experienceScore = computeExperienceScore(resume.getExperienceYears(),
            job.getExperienceMin(), job.getExperienceMax());
        double educationScore = computeEducationScore(resume.getEducation());

        // Weighted overall match score
        double matchScore = (skillMatchPct * 0.5) + (experienceScore * 0.35) + (educationScore * 0.15);
        matchScore = Math.min(100.0, Math.round(matchScore * 10.0) / 10.0);

        // Identify strengths and gaps
        List<String> strengths = identifyStrengths(candidateSkills, requiredSkills, preferredSkills,
            resume.getExperienceYears(), job.getExperienceMin(), resume.getEducation());
        List<String> gaps = identifyGaps(candidateSkills, requiredSkills,
            resume.getExperienceYears(), job.getExperienceMin());

        // Generate interview questions (AI or rule-based)
        List<String> questions = generateInterviewQuestions(resume, job);

        // Generate AI summary
        String aiSummary = generateAiSummary(resume, job, matchScore, strengths, gaps);

        // Check if analysis already exists for this resume-job pair
        Optional<CandidateAnalysis> existing = analysisRepository.findByResumeIdAndJobId(resumeId, jobId);

        CandidateAnalysis analysis;
        if (existing.isPresent()) {
            analysis = existing.get();
        } else {
            analysis = new CandidateAnalysis();
            analysis.setResumeId(resumeId);
            analysis.setJobId(jobId);
        }

        analysis.setMatchScore(matchScore);
        analysis.setSkillMatchPercentage(skillMatchPct);
        analysis.setExperienceScore(experienceScore);
        analysis.setEducationScore(educationScore);
        analysis.setStrengths(toJsonArray(strengths));
        analysis.setGaps(toJsonArray(gaps));
        analysis.setRecommendedQuestions(toJsonArray(questions));
        analysis.setAiSummary(aiSummary);
        analysis.setAnalyzedAt(LocalDateTime.now());

        CandidateAnalysis saved = analysisRepository.save(analysis);

        // Update resume status
        resume.setStatus(Resume.ResumeStatus.ANALYZED);
        resumeRepository.save(resume);

        log.info("Analysis complete for resume {} / job {}: matchScore={}", resumeId, jobId, matchScore);
        return saved;
    }

    // -------------------------------------------------------------------------
    // Score computation helpers
    // -------------------------------------------------------------------------

    private Set<String> parseSkillSet(String skillsCsv) {
        if (skillsCsv == null || skillsCsv.isBlank()) return Collections.emptySet();
        return Arrays.stream(skillsCsv.split(","))
            .map(String::trim)
            .map(String::toLowerCase)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
    }

    /**
     * Computes the percentage of required skills covered by candidate skills.
     * Uses Jaccard-inspired formula: intersection / required.size()
     */
    private double computeSkillMatchPercentage(Set<String> candidateSkills, Set<String> requiredSkills) {
        if (requiredSkills.isEmpty()) return 100.0;

        long matched = candidateSkills.stream()
            .filter(cs -> requiredSkills.stream().anyMatch(rs -> rs.contains(cs) || cs.contains(rs)))
            .count();

        return Math.round((matched * 100.0 / requiredSkills.size()) * 10.0) / 10.0;
    }

    /**
     * Scores experience alignment with the job's required range.
     */
    private double computeExperienceScore(Double candidateYears, Integer minYears, Integer maxYears) {
        if (candidateYears == null || candidateYears == 0) return 30.0;

        double years = candidateYears;
        int min = minYears != null ? minYears : 0;
        int max = maxYears != null ? maxYears : 15;

        if (years >= min && years <= max) {
            return 100.0; // Perfect fit
        } else if (years < min) {
            double deficit = min - years;
            return Math.max(0, 100.0 - (deficit * 15));
        } else {
            // Overqualified - slight penalty
            double excess = years - max;
            return Math.max(70.0, 100.0 - (excess * 5));
        }
    }

    /**
     * Scores education relevance based on detected degree level.
     */
    private double computeEducationScore(String education) {
        if (education == null || education.equalsIgnoreCase("not specified")) return 50.0;
        String lowerEd = education.toLowerCase();
        if (lowerEd.contains("phd") || lowerEd.contains("doctorate")) return 100.0;
        if (lowerEd.contains("master") || lowerEd.contains("mba") || lowerEd.contains("m.s")) return 90.0;
        if (lowerEd.contains("bachelor") || lowerEd.contains("b.s") || lowerEd.contains("b.tech")
            || lowerEd.contains("b.e")) return 80.0;
        if (lowerEd.contains("associate") || lowerEd.contains("diploma")) return 60.0;
        return 50.0;
    }

    private List<String> identifyStrengths(Set<String> candidateSkills, Set<String> requiredSkills,
                                            Set<String> preferredSkills, Double expYears,
                                            Integer minYears, String education) {
        List<String> strengths = new ArrayList<>();

        // Skill strengths
        long matchedRequired = candidateSkills.stream()
            .filter(cs -> requiredSkills.stream().anyMatch(rs -> rs.contains(cs) || cs.contains(rs)))
            .count();
        if (matchedRequired >= requiredSkills.size() * 0.8 && !requiredSkills.isEmpty()) {
            strengths.add("Meets " + matchedRequired + " of " + requiredSkills.size() + " required skills");
        }

        // Preferred skills bonus
        long matchedPreferred = candidateSkills.stream()
            .filter(cs -> preferredSkills.stream().anyMatch(rs -> rs.contains(cs) || cs.contains(rs)))
            .count();
        if (matchedPreferred > 0) {
            strengths.add("Has " + matchedPreferred + " preferred skill(s): bonus points");
        }

        // Experience strength
        if (expYears != null && minYears != null && expYears >= minYears) {
            strengths.add(String.format("%.0f+ years of relevant experience (meets minimum)", expYears));
        } else if (expYears != null && expYears >= 5) {
            strengths.add(String.format("%.0f years of industry experience", expYears));
        }

        // Education
        if (education != null) {
            String lowerEd = education.toLowerCase();
            if (lowerEd.contains("master") || lowerEd.contains("phd")) {
                strengths.add("Advanced degree provides strong foundation");
            } else if (lowerEd.contains("bachelor") || lowerEd.contains("b.tech")) {
                strengths.add("Relevant educational background");
            }
        }

        // Cloud/modern skills
        if (candidateSkills.stream().anyMatch(s -> s.contains("aws") || s.contains("azure") || s.contains("gcp"))) {
            strengths.add("Cloud platform expertise");
        }
        if (candidateSkills.stream().anyMatch(s -> s.contains("docker") || s.contains("kubernetes"))) {
            strengths.add("Container orchestration skills");
        }

        return strengths.isEmpty() ? List.of("Profile submitted for review") : strengths;
    }

    private List<String> identifyGaps(Set<String> candidateSkills, Set<String> requiredSkills,
                                       Double expYears, Integer minYears) {
        List<String> gaps = new ArrayList<>();

        // Missing required skills
        List<String> missingSkills = requiredSkills.stream()
            .filter(rs -> candidateSkills.stream().noneMatch(cs -> cs.contains(rs) || rs.contains(cs)))
            .collect(Collectors.toList());

        if (!missingSkills.isEmpty()) {
            gaps.add("Missing required skills: " + String.join(", ", missingSkills.subList(0,
                Math.min(3, missingSkills.size()))));
        }

        // Experience gap
        if (expYears != null && minYears != null && expYears < minYears) {
            gaps.add(String.format("Experience gap: %.0f years (needs %d)", expYears, minYears));
        }

        // Common gaps based on what's NOT in their skills
        if (candidateSkills.stream().noneMatch(s -> s.contains("kubernetes") || s.contains("k8s"))) {
            gaps.add("No Kubernetes/container orchestration experience noted");
        }
        if (candidateSkills.stream().noneMatch(s -> s.contains("aws") || s.contains("azure")
            || s.contains("gcp") || s.contains("cloud"))) {
            gaps.add("Limited cloud platform experience");
        }

        return gaps.isEmpty() ? List.of("No significant gaps identified") : gaps;
    }

    // -------------------------------------------------------------------------
    // AI/OpenAI integration
    // -------------------------------------------------------------------------

    /**
     * Generate 5 interview questions via OpenAI API or rule-based fallback.
     */
    private List<String> generateInterviewQuestions(Resume resume, JobDescription job) {
        if (hasValidApiKey()) {
            try {
                return callOpenAiForQuestions(resume, job);
            } catch (Exception e) {
                log.warn("OpenAI API call failed, using rule-based fallback: {}", e.getMessage());
            }
        }
        return generateRuleBasedQuestions(resume, job);
    }

    /**
     * Call OpenAI API to generate interview questions.
     */
    private List<String> callOpenAiForQuestions(Resume resume, JobDescription job) {
        String prompt = String.format(
            "You are a senior technical recruiter. Generate exactly 5 targeted interview questions " +
            "for a candidate with these skills: %s and %.0f years of experience, interviewing for " +
            "a %s role requiring: %s. Format: numbered list, questions only.",
            resume.getSkills(), resume.getExperienceYears() != null ? resume.getExperienceYears() : 0,
            job.getTitle(), job.getRequiredSkills()
        );

        String responseBody = callOpenAi(prompt, 600);
        if (responseBody == null) return generateRuleBasedQuestions(resume, job);

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("choices").get(0).path("message").path("content").asText();
            return Arrays.stream(content.split("\\n"))
                .filter(line -> line.matches("\\d+\\..*"))
                .map(line -> line.replaceFirst("^\\d+\\.\\s*", "").trim())
                .limit(5)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to parse OpenAI response for questions: {}", e.getMessage());
            return generateRuleBasedQuestions(resume, job);
        }
    }

    /**
     * Generate AI narrative summary via OpenAI or rule-based fallback.
     */
    private String generateAiSummary(Resume resume, JobDescription job, double matchScore,
                                      List<String> strengths, List<String> gaps) {
        if (hasValidApiKey()) {
            try {
                return callOpenAiForSummary(resume, job, matchScore);
            } catch (Exception e) {
                log.warn("OpenAI summary failed, using rule-based: {}", e.getMessage());
            }
        }
        return generateRuleBasedSummary(resume, job, matchScore, strengths, gaps);
    }

    private String callOpenAiForSummary(Resume resume, JobDescription job, double matchScore) {
        String prompt = String.format(
            "Write a concise 3-sentence recruiter summary for: %s (%.0f yrs exp, skills: %s) " +
            "applying for %s at %s. Match score: %.1f/100. Focus on fit, strengths, and recommendation.",
            resume.getCandidateName(), resume.getExperienceYears() != null ? resume.getExperienceYears() : 0,
            resume.getSkills(), job.getTitle(), job.getCompany(), matchScore
        );

        String responseBody = callOpenAi(prompt, 300);
        if (responseBody == null) return null;

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return root.path("choices").get(0).path("message").path("content").asText().trim();
        } catch (Exception e) {
            log.error("Failed to parse OpenAI summary response: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Core method to call OpenAI Chat Completions API via RestTemplate.
     */
    private String callOpenAi(String userPrompt, int maxTokens) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", openAiModel);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("temperature", 0.7);

        Map<String, String> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", userPrompt);
        requestBody.put("messages", List.of(message));

        try {
            String requestJson = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);
            return restTemplate.postForObject(openAiApiUrl, entity, String.class);
        } catch (Exception e) {
            log.error("OpenAI API request failed: {}", e.getMessage());
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Rule-based fallbacks
    // -------------------------------------------------------------------------

    private List<String> generateRuleBasedQuestions(Resume resume, JobDescription job) {
        List<String> questions = new ArrayList<>();
        String title = job.getTitle();
        Set<String> skills = parseSkillSet(resume.getSkills());

        // Technical questions based on detected skills
        if (skills.stream().anyMatch(s -> s.contains("java") || s.contains("spring"))) {
            questions.add("Explain how Spring Boot's auto-configuration works and how you've customized it in production.");
        }
        if (skills.stream().anyMatch(s -> s.contains("microservices") || s.contains("docker") || s.contains("kubernetes"))) {
            questions.add("Describe how you've designed and deployed a microservices architecture. What challenges did you face?");
        }
        if (skills.stream().anyMatch(s -> s.contains("postgresql") || s.contains("mysql") || s.contains("database"))) {
            questions.add("How do you optimize slow database queries? Walk me through your approach with a real example.");
        }
        if (skills.stream().anyMatch(s -> s.contains("aws") || s.contains("azure") || s.contains("gcp"))) {
            questions.add("Which AWS/cloud services have you used in production and how did you manage cost optimization?");
        }

        // Experience-based questions
        double expYears = resume.getExperienceYears() != null ? resume.getExperienceYears() : 0;
        if (expYears >= 5) {
            questions.add("Tell me about a technically complex project you led. What trade-offs did you make in the architecture?");
        } else {
            questions.add("Describe a challenging technical problem you solved recently and your debugging approach.");
        }

        // Role-specific questions
        questions.add(String.format(
            "What interests you most about the %s role at %s? How does it align with your career goals?",
            title, job.getCompany()
        ));
        questions.add("How do you stay current with rapidly evolving technologies? Give me a recent example.");
        questions.add("Describe a time you disagreed with a technical decision. How did you handle it?");

        return questions.subList(0, Math.min(5, questions.size()));
    }

    private String generateRuleBasedSummary(Resume resume, JobDescription job, double matchScore,
                                             List<String> strengths, List<String> gaps) {
        String expStr = resume.getExperienceYears() != null
            ? String.format("%.0f years", resume.getExperienceYears()) : "unknown years";

        String recommendation;
        if (matchScore >= 80) recommendation = "Strong recommend for interview.";
        else if (matchScore >= 60) recommendation = "Recommend for technical screening.";
        else if (matchScore >= 40) recommendation = "Borderline fit — consider for pipeline.";
        else recommendation = "Not an immediate fit for this role.";

        return String.format(
            "%s brings %s of experience with key skills including %s. " +
            "Primary strengths include: %s. " +
            "Overall match score of %.1f%% — %s",
            resume.getCandidateName(), expStr,
            resume.getSkills() != null ? resume.getSkills().replace(",", ", ") : "general software development",
            strengths.isEmpty() ? "relevant experience" : strengths.get(0),
            matchScore, recommendation
        );
    }

    private boolean hasValidApiKey() {
        return openAiApiKey != null && !openAiApiKey.isBlank()
            && !openAiApiKey.equals("your-openai-api-key-here");
    }

    private String toJsonArray(List<String> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (Exception e) {
            return "[]";
        }
    }
}
