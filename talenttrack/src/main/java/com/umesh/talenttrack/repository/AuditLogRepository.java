package com.umesh.talenttrack.repository;

import com.umesh.talenttrack.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findAllByCompanyId(Long companyId, Pageable pageable);
    List<AuditLog> findAllByCompanyIdOrderByTimestampDesc(Long companyId);
}
