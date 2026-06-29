package com.recruitment.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resume entity representing a candidate's resume uploaded to the platform.
 * Stores both raw text for AI processing and parsed structured data.
 */
@Entity
@Table(name = "resumes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "resume_id", columnDefinition = "uuid")
    private UUID resumeId;

    @Column(name = "candidate_name", nullable = false, length = 200)
    private String candidateName;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "current_title", length = 200)
    private String currentTitle;

    @Column(name = "current_company", length = 200)
    private String currentCompany;

    @Column(name = "experience_years")
    private Double experienceYears;

    /**
     * Comma-separated list of extracted skills (e.g. "Java,Python,Spring Boot,Docker")
     */
    @Column(name = "skills", columnDefinition = "TEXT")
    private String skills;

    @Column(name = "education", columnDefinition = "TEXT")
    private String education;

    /**
     * Full raw text of the resume for AI processing
     */
    @Column(name = "raw_text", columnDefinition = "TEXT", nullable = false)
    private String rawText;

    @Column(name = "parsed_at")
    private LocalDateTime parsedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ResumeStatus status = ResumeStatus.UPLOADED;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = ResumeStatus.UPLOADED;
        }
    }

    public enum ResumeStatus {
        UPLOADED,
        PARSED,
        ANALYZED,
        SHORTLISTED,
        REJECTED
    }
}
