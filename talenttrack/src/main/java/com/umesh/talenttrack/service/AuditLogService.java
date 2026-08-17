package com.umesh.talenttrack.service;

import com.umesh.talenttrack.domain.ActorType;
import com.umesh.talenttrack.domain.AuditLog;
import com.umesh.talenttrack.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Map;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void log(Long companyId, ActorType actorType, Long actorId, String action, 
                    String entityType, Long entityId, Map<String, Object> metadata) {
        
        AuditLog log = AuditLog.builder()
                .companyId(companyId)
                .actorType(actorType)
                .actorId(actorId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .metadata(metadata)
                .build();
        
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsForCompany(Long companyId, Pageable pageable) {
        return auditLogRepository.findAllByCompanyId(companyId, pageable);
    }
}
