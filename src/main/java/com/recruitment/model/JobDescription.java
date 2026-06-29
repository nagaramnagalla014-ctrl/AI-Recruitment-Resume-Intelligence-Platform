package com.recruitment.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JobDescription entity representing a job posting on the platform.
 * Used to match against candidate resumes for intelligent ranking.
 */
@Entity
@Table(name = "job_descriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDescription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "job_id", columnDefinition = "uuid")
    private UUID jobId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "company", nullable = false, length = 200)
    private String company;

    /**
     * Comma-separated required skills (e.g. "Java,Spring Boot,PostgreSQL,Docker")
     */
    @Column(name = "required_skills", columnDefinition = "TEXT", nullable = false)
    private String requiredSkills;

    /**
     * Comma-separated preferred/nice-to-have skills
     */
    @Column(name = "preferred_skills", columnDefinition = "TEXT")
    private String preferredSkills;

    @Column(name = "experience_min")
    private Integer experienceMin;

    @Column(name = "experience_max")
    private Integer experienceMax;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "remote")
    @Builder.Default
    private Boolean remote = false;

    @Column(name = "posted_at")
    @Builder.Default
    private LocalDateTime postedAt = LocalDateTime.now();

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (this.postedAt == null) {
            this.postedAt = LocalDateTime.now();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.active == null) {
            this.active = true;
        }
        if (this.remote == null) {
            this.remote = false;
        }
    }
}
