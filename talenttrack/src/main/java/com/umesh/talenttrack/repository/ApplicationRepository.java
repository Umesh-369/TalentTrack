package com.umesh.talenttrack.repository;

import com.umesh.talenttrack.domain.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    // Recruiter queries (tenant-scoped)
    Optional<Application> findByIdAndJobPostingCompanyId(Long id, Long companyId);
    Page<Application> findAllByJobPostingCompanyId(Long companyId, Pageable pageable);
    List<Application> findAllByJobPostingCompanyId(Long companyId);
    List<Application> findAllByJobPostingIdAndJobPostingCompanyId(Long jobPostingId, Long companyId);

    // Candidate queries (enforces that a candidate only sees their own applications)
    Optional<Application> findByIdAndCandidateId(Long id, Long candidateId);
    List<Application> findAllByCandidateId(Long candidateId);
    Optional<Application> findByCandidateIdAndJobPostingId(Long candidateId, Long jobPostingId);
}
