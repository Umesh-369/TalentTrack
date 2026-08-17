package com.umesh.talenttrack.repository;

import com.umesh.talenttrack.domain.JobPosting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {

    // Recruiter-facing queries (tenant-scoped)
    Optional<JobPosting> findByIdAndCompanyId(Long id, Long companyId);
    Page<JobPosting> findAllByCompanyId(Long companyId, Pageable pageable);
    List<JobPosting> findAllByCompanyId(Long companyId);

    // Candidate-facing queries (active, published, and not expired)
    @Query("SELECT j FROM JobPosting j WHERE j.status = 'PUBLISHED' " +
           "AND (j.expiresAt IS NULL OR j.expiresAt > :now) " +
           "AND (:location IS NULL OR j.location LIKE %:location%) " +
           "AND (:remote IS NULL OR j.remote = :remote) " +
           "AND (:experienceMin IS NULL OR j.experienceMin >= :experienceMin) " +
           "AND (:experienceMax IS NULL OR j.experienceMax <= :experienceMax)")
    Page<JobPosting> searchActiveJobs(
            LocalDateTime now,
            String location,
            Boolean remote,
            Integer experienceMin,
            Integer experienceMax,
            Pageable pageable
    );

    // For scheduled job to find all published jobs that have expired
    @Query("SELECT j FROM JobPosting j WHERE j.status = 'PUBLISHED' AND j.expiresAt IS NOT NULL AND j.expiresAt <= :now")
    List<JobPosting> findExpiredJobs(LocalDateTime now);
}
