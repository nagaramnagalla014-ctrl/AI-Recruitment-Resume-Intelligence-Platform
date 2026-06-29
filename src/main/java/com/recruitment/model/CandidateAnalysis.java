package com.recruitment.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CandidateAnalysis entity storing AI analysis results for a resume-job pair.
 * Contains match scores, strengths/gaps, and AI-generated interview questions.
 */
@Entity
@Table(name = "candidate_analyses",
       uniqueConstraints = @UniqueConstraint(columnNames = {"resume_id", "job_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "analysis_id", columnDefinition = "uuid")
    private UUID analysisId;

    @Column(name = "resume_id", nullable = false, columnDefinition = "uuid")
    private UUID resumeId;

    @Column(name = "job_id", nullable = false, columnDefinition = "uuid")
    private UUID jobId;

    /**
     * Overall match score from 0 to 100
     */
    @Column(name = "match_score", nullable = false)
    private Double matchScore;

    /**
     * Percentage of required skills the candidate has (0-100)
     */
    @Column(name = "skill_match_percentage")
    private Double skillMatchPercentage;

    /**
     * Score based on how well candidate experience aligns with job range (0-100)
     */
    @Column(name = "experience_score")
    private Double experienceScore;

    /**
     * Score based on education relevance (0-100)
     */
    @Column(name = "education_score")
    private Double educationScore;

    /**
     * Rank among all candidates analyzed for this job (1 = best)
     */
    @Column(name = "overall_rank")
    private Integer overallRank;

    /**
     * JSON array of candidate strengths (e.g. ["5+ years Java", "Cloud certifications"])
     */
    @Column(name = "strengths", columnDefinition = "TEXT")
    private String strengths;

    /**
     * JSON array of skill/experience gaps (e.g. ["No Kubernetes experience", "Missing ML skills"])
     */
    @Column(name = "gaps", columnDefinition = "TEXT")
    private String gaps;

    /**
     * JSON array of AI-generated interview questions
     */
    @Column(name = "recommended_questions", columnDefinition = "TEXT")
    private String recommendedQuestions;

    /**
     * AI-generated narrative summary of the candidate's fit for the role
     */
    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "analyzed_at")
    @Builder.Default
    private LocalDateTime analyzedAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (this.analyzedAt == null) {
            this.analyzedAt = LocalDateTime.now();
        }
    }
}
