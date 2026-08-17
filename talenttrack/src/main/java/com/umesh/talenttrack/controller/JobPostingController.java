package com.umesh.talenttrack.controller;

import com.umesh.talenttrack.dto.JobPostingRequest;
import com.umesh.talenttrack.dto.JobPostingResponse;
import com.umesh.talenttrack.security.CustomUserDetails;
import com.umesh.talenttrack.service.JobPostingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs")
public class JobPostingController {

    private final JobPostingService jobPostingService;

    public JobPostingController(JobPostingService jobPostingService) {
        this.jobPostingService = jobPostingService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<JobPostingResponse> createJob(
            @Valid @RequestBody JobPostingRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        JobPostingResponse response = jobPostingService.createJob(request, userDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<JobPostingResponse> updateJob(
            @PathVariable Long id,
            @Valid @RequestBody JobPostingRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        JobPostingResponse response = jobPostingService.updateJob(id, request, userDetails);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<JobPostingResponse> publishJob(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        JobPostingResponse response = jobPostingService.publishJob(id, userDetails);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<JobPostingResponse> closeJob(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        JobPostingResponse response = jobPostingService.closeJob(id, userDetails);
        return ResponseEntity.ok(response);
    }

    // Public search (PermitAll in SecurityConfig)
    @GetMapping
    public ResponseEntity<Page<JobPostingResponse>> searchJobs(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Boolean remote,
            @RequestParam(required = false) Integer experienceMin,
            @RequestParam(required = false) Integer experienceMax,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<JobPostingResponse> response = jobPostingService.searchJobsForCandidate(
                location, remote, experienceMin, experienceMax, pageable);
        return ResponseEntity.ok(response);
    }

    // Smart view details (PermitAll, but role-aware)
    @GetMapping("/{id}")
    public ResponseEntity<JobPostingResponse> getJob(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        if (userDetails != null && userDetails.getCompanyId() != null) {
            try {
                // If recruiter of the company, allow viewing private drafts/closed jobs
                return ResponseEntity.ok(jobPostingService.getJobForRecruiter(id, userDetails));
            } catch (Exception e) {
                // Fallback to active candidate view if recruiter belongs to different company
            }
        }
        return ResponseEntity.ok(jobPostingService.getJobForCandidate(id));
    }

    // Recruiter-specific dashboard job listing
    @GetMapping("/recruiter")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<Page<JobPostingResponse>> getRecruiterJobs(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<JobPostingResponse> response = jobPostingService.getJobsForRecruiter(userDetails, pageable);
        return ResponseEntity.ok(response);
    }
}
