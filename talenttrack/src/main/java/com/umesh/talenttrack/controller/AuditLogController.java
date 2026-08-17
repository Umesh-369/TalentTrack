package com.umesh.talenttrack.controller;

import com.umesh.talenttrack.domain.AuditLog;
import com.umesh.talenttrack.security.CustomUserDetails;
import com.umesh.talenttrack.service.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/companies/{companyId}/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<Page<AuditLog>> getCompanyAuditLogs(
            @PathVariable Long companyId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        // Enforce tenant isolation boundary
        if (!userDetails.getCompanyId().equals(companyId)) {
            throw new AccessDeniedException("Access denied: You cannot view audit logs for another company");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> logs = auditLogService.getAuditLogsForCompany(companyId, pageable);
        return ResponseEntity.ok(logs);
    }
}
