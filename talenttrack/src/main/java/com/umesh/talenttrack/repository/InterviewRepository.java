package com.umesh.talenttrack.repository;

import com.umesh.talenttrack.domain.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {

    // Recruiter-facing queries (tenant-scoped)
    Optional<Interview> findByIdAndApplicationJobPostingCompanyId(Long id, Long companyId);
    List<Interview> findAllByApplicationIdAndApplicationJobPostingCompanyId(Long applicationId, Long companyId);

    // Candidate-facing queries (candidate-scoped)
    Optional<Interview> findByIdAndApplicationCandidateId(Long id, Long candidateId);
    List<Interview> findAllByApplicationIdAndApplicationCandidateId(Long applicationId, Long candidateId);
    
    // Direct lookup
    List<Interview> findAllByApplicationId(Long applicationId);
}
