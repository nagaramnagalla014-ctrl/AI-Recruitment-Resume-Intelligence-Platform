package com.recruitment.repository;

import com.recruitment.model.JobDescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobDescriptionRepository extends JpaRepository<JobDescription, UUID> {

    List<JobDescription> findByActiveTrue();

    List<JobDescription> findByCompany(String company);

    @Query("SELECT j FROM JobDescription j WHERE j.active = true AND j.remote = true")
    List<JobDescription> findActiveRemoteJobs();

    @Query("SELECT j FROM JobDescription j WHERE j.active = true AND " +
           "j.experienceMin <= :years AND j.experienceMax >= :years")
    List<JobDescription> findJobsByExperienceRange(int years);

    @Query("SELECT COUNT(j) FROM JobDescription j WHERE j.active = true")
    long countActiveJobs();
}
