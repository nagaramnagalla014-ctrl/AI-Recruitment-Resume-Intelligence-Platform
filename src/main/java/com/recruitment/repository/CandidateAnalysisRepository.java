package com.recruitment.repository;

import com.recruitment.model.CandidateAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidateAnalysisRepository extends JpaRepository<CandidateAnalysis, UUID> {

    List<CandidateAnalysis> findByJobIdOrderByMatchScoreDesc(UUID jobId);

    List<CandidateAnalysis> findByResumeIdOrderByMatchScoreDesc(UUID resumeId);

    Optional<CandidateAnalysis> findByResumeIdAndJobId(UUID resumeId, UUID jobId);

    @Query("SELECT ca FROM CandidateAnalysis ca WHERE ca.jobId = :jobId ORDER BY ca.matchScore DESC")
    List<CandidateAnalysis> findTopCandidatesForJob(UUID jobId);

    @Query("SELECT AVG(ca.matchScore) FROM CandidateAnalysis ca")
    Double getAverageMatchScore();

    @Query("SELECT COUNT(ca) FROM CandidateAnalysis ca WHERE ca.matchScore >= :threshold")
    long countCandidatesAboveThreshold(double threshold);

    @Query("SELECT ca FROM CandidateAnalysis ca WHERE ca.jobId = :jobId " +
           "ORDER BY ca.matchScore DESC LIMIT :limit")
    List<CandidateAnalysis> findTopNForJob(UUID jobId, int limit);
}
