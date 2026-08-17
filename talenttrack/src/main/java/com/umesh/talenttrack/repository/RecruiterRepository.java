package com.umesh.talenttrack.repository;

import com.umesh.talenttrack.domain.Recruiter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecruiterRepository extends JpaRepository<Recruiter, Long> {
    Optional<Recruiter> findByEmail(String email);
    List<Recruiter> findAllByCompanyId(Long companyId);
    Optional<Recruiter> findByIdAndCompanyId(Long id, Long companyId);
}
