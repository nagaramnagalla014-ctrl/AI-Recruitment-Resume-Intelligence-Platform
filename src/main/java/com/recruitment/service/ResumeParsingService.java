package com.recruitment.service;

import com.recruitment.model.Resume;
import com.recruitment.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for parsing resume text and extracting structured information.
 * Uses regex and keyword matching to identify skills, experience, education, etc.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeParsingService {

    private final ResumeRepository resumeRepository;

    // Comprehensive known tech skills for keyword matching
    private static final Set<String> KNOWN_SKILLS = new LinkedHashSet<>(Arrays.asList(
        // Languages
        "Java", "Python", "JavaScript", "TypeScript", "Go", "Rust", "C++", "C#", "Ruby", "PHP",
        "Kotlin", "Scala", "Swift", "R", "MATLAB", "Perl", "Shell", "Bash",
        // Frameworks
        "Spring Boot", "Spring", "React", "Angular", "Vue.js", "Vue", "Node.js", "Express",
        "Django", "Flask", "FastAPI", "Laravel", "Rails", "Hibernate", "JPA", "MyBatis",
        "Next.js", "Nuxt.js", "Svelte", "Quarkus", "Micronaut",
        // Databases
        "PostgreSQL", "MySQL", "Oracle", "SQL Server", "MongoDB", "Cassandra", "DynamoDB",
        "Redis", "Elasticsearch", "Neo4j", "CockroachDB", "Snowflake", "BigQuery",
        // Cloud & DevOps
        "AWS", "Azure", "GCP", "Docker", "Kubernetes", "Terraform", "Ansible", "Jenkins",
        "GitHub Actions", "GitLab CI", "CircleCI", "ArgoCD", "Helm", "Istio",
        // AI/ML
        "Machine Learning", "Deep Learning", "TensorFlow", "PyTorch", "Scikit-learn",
        "LangChain", "OpenAI", "NLP", "Computer Vision", "MLflow", "Hugging Face",
        "Pandas", "NumPy", "Spark", "Kafka",
        // Architecture
        "Microservices", "REST API", "GraphQL", "gRPC", "Event-Driven", "CQRS",
        "Domain-Driven Design", "Agile", "Scrum", "DevOps", "CI/CD", "TDD", "BDD",
        // Testing
        "JUnit", "Mockito", "Selenium", "Cypress", "Pytest", "Jest", "Postman"
    ));

    // Patterns for experience extraction
    private static final Pattern EXPERIENCE_PATTERN = Pattern.compile(
        "(\\d+(?:\\.\\d+)?)\\s*(?:\\+\\s*)?(?:years?|yrs?)\\s*(?:of\\s*)?(?:experience|exp)?",
        Pattern.CASE_INSENSITIVE
    );

    // Email pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}"
    );

    // Phone pattern
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "(?:\\+?1[-.]?)?\\(?\\d{3}\\)?[-.]?\\d{3}[-.]?\\d{4}"
    );

    // Name pattern (first line or after common headers)
    private static final Pattern NAME_PATTERN = Pattern.compile(
        "(?:^|Name:\\s*)([A-Z][a-z]+(?:\\s+[A-Z][a-z]+){1,3})"
    );

    // Education keywords
    private static final Map<String, Integer> EDUCATION_KEYWORDS = Map.of(
        "PhD", 5, "Ph.D", 5, "Doctorate", 5,
        "Master", 4, "M.S.", 4, "M.Tech", 4, "MBA", 4,
        "Bachelor", 3, "B.S.", 3, "B.Tech", 3, "B.E.", 3,
        "Associate", 2, "Diploma", 1
    );

    /**
     * Parse raw resume text and extract structured information.
     * Saves the parsed resume entity to the database.
     *
     * @param rawText The raw text content of the resume
     * @return The saved Resume entity with parsed fields
     */
    @Transactional
    public Resume parseResume(String rawText) {
        log.info("Starting resume parsing for text of length {}", rawText.length());

        String candidateName = extractName(rawText);
        String email = extractEmail(rawText);
        String phone = extractPhone(rawText);
        String skills = extractSkills(rawText);
        Double experienceYears = extractExperienceYears(rawText);
        String currentTitle = extractCurrentTitle(rawText);
        String currentCompany = extractCurrentCompany(rawText);
        String education = extractEducation(rawText);

        Resume resume = Resume.builder()
            .candidateName(candidateName)
            .email(email)
            .phone(phone)
            .skills(skills)
            .experienceYears(experienceYears)
            .currentTitle(currentTitle)
            .currentCompany(currentCompany)
            .education(education)
            .rawText(rawText)
            .parsedAt(LocalDateTime.now())
            .status(Resume.ResumeStatus.PARSED)
            .build();

        Resume saved = resumeRepository.save(resume);
        log.info("Resume parsed and saved with ID: {}, candidate: {}", saved.getResumeId(), candidateName);
        return saved;
    }

    /**
     * Update an existing resume's parsed data (re-parse).
     */
    @Transactional
    public Resume reparseResume(UUID resumeId, String rawText) {
        Resume existing = resumeRepository.findById(resumeId)
            .orElseThrow(() -> new IllegalArgumentException("Resume not found: " + resumeId));

        existing.setRawText(rawText);
        existing.setCandidateName(extractName(rawText));
        existing.setEmail(extractEmail(rawText));
        existing.setPhone(extractPhone(rawText));
        existing.setSkills(extractSkills(rawText));
        existing.setExperienceYears(extractExperienceYears(rawText));
        existing.setCurrentTitle(extractCurrentTitle(rawText));
        existing.setCurrentCompany(extractCurrentCompany(rawText));
        existing.setEducation(extractEducation(rawText));
        existing.setParsedAt(LocalDateTime.now());
        existing.setStatus(Resume.ResumeStatus.PARSED);

        return resumeRepository.save(existing);
    }

    // -------------------------------------------------------------------------
    // Private extraction helpers
    // -------------------------------------------------------------------------

    private String extractName(String text) {
        String[] lines = text.strip().split("\\r?\\n");
        // Try first non-empty line as candidate name (often top of resume)
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            // Skip lines that look like contact info
            if (trimmed.contains("@") || trimmed.matches(".*\\d{7,}.*")) continue;
            // Check for title-case name pattern
            if (trimmed.matches("[A-Z][a-z]+(\\s+[A-Z][a-z]+){1,3}")) {
                return trimmed;
            }
            break;
        }

        // Regex fallback
        Matcher matcher = NAME_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return "Unknown Candidate";
    }

    private String extractEmail(String text) {
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        return matcher.find() ? matcher.group().toLowerCase() : null;
    }

    private String extractPhone(String text) {
        Matcher matcher = PHONE_PATTERN.matcher(text);
        return matcher.find() ? matcher.group() : null;
    }

    private String extractSkills(String text) {
        String lowerText = text.toLowerCase();
        List<String> found = new ArrayList<>();

        // Sort by length descending to prefer multi-word matches first
        List<String> sortedSkills = KNOWN_SKILLS.stream()
            .sorted(Comparator.comparingInt(String::length).reversed())
            .collect(Collectors.toList());

        for (String skill : sortedSkills) {
            if (lowerText.contains(skill.toLowerCase())) {
                found.add(skill);
            }
        }

        if (found.isEmpty()) {
            // Fallback: look for a "Skills:" section and parse comma/pipe separated values
            Pattern skillSection = Pattern.compile(
                "skills?[:\\s]+([A-Za-z0-9,.| \\-+#]+?)(?:\\n\\n|$)", Pattern.CASE_INSENSITIVE
            );
            Matcher m = skillSection.matcher(text);
            if (m.find()) {
                String[] tokens = m.group(1).split("[,|/\\n]");
                for (String t : tokens) {
                    String skill = t.trim();
                    if (!skill.isEmpty() && skill.length() > 1) {
                        found.add(skill);
                    }
                }
            }
        }

        return found.stream().distinct().collect(Collectors.joining(","));
    }

    private Double extractExperienceYears(String text) {
        Matcher matcher = EXPERIENCE_PATTERN.matcher(text);
        double maxYears = 0.0;
        while (matcher.find()) {
            try {
                double years = Double.parseDouble(matcher.group(1));
                if (years > maxYears && years <= 50) { // sanity check
                    maxYears = years;
                }
            } catch (NumberFormatException e) {
                log.debug("Could not parse experience years: {}", matcher.group(1));
            }
        }

        if (maxYears == 0.0) {
            // Try to infer from year ranges (e.g., 2018-2023 = ~5 years)
            Pattern yearRange = Pattern.compile("(20\\d{2})\\s*[-–—]\\s*(20\\d{2}|present|current)",
                Pattern.CASE_INSENSITIVE);
            Matcher yrMatcher = yearRange.matcher(text);
            int totalYears = 0;
            while (yrMatcher.find()) {
                int start = Integer.parseInt(yrMatcher.group(1));
                String endStr = yrMatcher.group(2);
                int end = endStr.equalsIgnoreCase("present") || endStr.equalsIgnoreCase("current")
                    ? 2024 : Integer.parseInt(endStr);
                totalYears += (end - start);
            }
            if (totalYears > 0) {
                maxYears = Math.min(totalYears, 40.0);
            }
        }

        return maxYears;
    }

    private String extractCurrentTitle(String text) {
        String[] titleKeywords = {
            "Software Engineer", "Senior Software Engineer", "Staff Engineer", "Principal Engineer",
            "Engineering Manager", "Tech Lead", "Technical Lead", "Architect", "Solution Architect",
            "Data Engineer", "Data Scientist", "ML Engineer", "DevOps Engineer", "SRE",
            "Site Reliability Engineer", "Product Manager", "Full Stack Developer",
            "Backend Developer", "Frontend Developer", "Cloud Engineer", "Platform Engineer"
        };

        String lowerText = text.toLowerCase();
        for (String title : titleKeywords) {
            if (lowerText.contains(title.toLowerCase())) {
                return title;
            }
        }

        // Try to find title after "Title:" or on second line
        Pattern titlePattern = Pattern.compile("(?:title|position|role):\\s*([^\\n]+)", Pattern.CASE_INSENSITIVE);
        Matcher m = titlePattern.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }

        return "Software Professional";
    }

    private String extractCurrentCompany(String text) {
        Pattern companyPattern = Pattern.compile(
            "(?:at|@|company|employer|organization):\\s*([A-Z][A-Za-z0-9\\s&.,]+?)(?:\\n|,|\\()",
            Pattern.CASE_INSENSITIVE
        );
        Matcher m = companyPattern.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    private String extractEducation(String text) {
        String lowerText = text.toLowerCase();
        StringBuilder education = new StringBuilder();

        for (Map.Entry<String, Integer> entry : EDUCATION_KEYWORDS.entrySet()) {
            if (lowerText.contains(entry.getKey().toLowerCase())) {
                education.append(entry.getKey()).append("; ");
            }
        }

        // Look for university/college mentions
        Pattern universityPattern = Pattern.compile(
            "(University|College|Institute|School)\\s+of\\s+[A-Z][A-Za-z\\s]+",
            Pattern.CASE_INSENSITIVE
        );
        Matcher m = universityPattern.matcher(text);
        while (m.find()) {
            education.append(m.group().trim()).append("; ");
        }

        String result = education.toString().trim();
        return result.isEmpty() ? "Not specified" : result.replaceAll(";\\s*$", "");
    }
}
