package com.umesh.talenttrack.controller;

import com.umesh.talenttrack.domain.ApplicationStatus;
import com.umesh.talenttrack.dto.ApplicationRequest;
import com.umesh.talenttrack.dto.ApplicationResponse;
import com.umesh.talenttrack.security.CustomUserDetails;
import com.umesh.talenttrack.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    // Candidate submits application
    @PostMapping("/applications")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApplicationResponse> submitApplication(
            @Valid @RequestBody ApplicationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ApplicationResponse response = applicationService.submitApplication(request, userDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Candidate views their own applications
    @GetMapping("/applications/mine")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<List<ApplicationResponse>> getMyApplications(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ApplicationResponse> response = applicationService.getMyApplications(userDetails);
        return ResponseEntity.ok(response);
    }

    // Recruiter views applications of their own company
    @GetMapping("/companies/{companyId}/applications")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<Page<ApplicationResponse>> getCompanyApplications(
            @PathVariable Long companyId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        // Controller-level cross-check
        if (!userDetails.getCompanyId().equals(companyId)) {
            throw new AccessDeniedException("Access denied: You cannot view applications for another company");
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ApplicationResponse> response = applicationService.getApplicationsForRecruiter(userDetails, pageable);
        return ResponseEntity.ok(response);
    }

    // Recruiter views applications of a specific job posting
    @GetMapping("/jobs/{jobPostingId}/applications")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<List<ApplicationResponse>> getJobApplications(
            @PathVariable Long jobPostingId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ApplicationResponse> response = applicationService.getApplicationsForRecruiterByJob(jobPostingId, userDetails);
        return ResponseEntity.ok(response);
    }

    // Recruiter updates application status
    @PatchMapping("/applications/{id}/status")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<ApplicationResponse> updateApplicationStatus(
            @PathVariable Long id,
            @RequestParam ApplicationStatus status,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ApplicationResponse response = applicationService.updateApplicationStatus(id, status, userDetails);
        return ResponseEntity.ok(response);
    }
}
