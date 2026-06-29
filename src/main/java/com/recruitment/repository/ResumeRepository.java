package com.recruitment.repository;

import com.recruitment.model.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, UUID> {

    Optional<Resume> findByEmail(String email);

    List<Resume> findByStatus(Resume.ResumeStatus status);

    @Query("SELECT r FROM Resume r WHERE r.status != 'REJECTED' ORDER BY r.createdAt DESC")
    List<Resume> findAllActive();

    @Query("SELECT COUNT(r) FROM Resume r WHERE r.status = :status")
    long countByStatus(Resume.ResumeStatus status);

    @Query("SELECT r FROM Resume r WHERE LOWER(r.skills) LIKE LOWER(CONCAT('%', :skill, '%'))")
    List<Resume> findBySkillContaining(String skill);
}
