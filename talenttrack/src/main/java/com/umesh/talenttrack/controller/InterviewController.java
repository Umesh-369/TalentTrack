package com.umesh.talenttrack.controller;

import com.umesh.talenttrack.domain.InterviewOutcome;
import com.umesh.talenttrack.dto.InterviewRequest;
import com.umesh.talenttrack.dto.InterviewResponse;
import com.umesh.talenttrack.security.CustomUserDetails;
import com.umesh.talenttrack.service.InterviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<InterviewResponse> scheduleInterview(
            @Valid @RequestBody InterviewRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        InterviewResponse response = interviewService.scheduleInterview(request, userDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<List<InterviewResponse>> getInterviewsForApplication(
            @PathVariable Long applicationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<InterviewResponse> response = interviewService.getInterviewsForApplication(applicationId, userDetails);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/outcome")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<InterviewResponse> updateInterviewOutcome(
            @PathVariable Long id,
            @RequestParam InterviewOutcome outcome,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        InterviewResponse response = interviewService.updateInterviewOutcome(id, outcome, userDetails);
        return ResponseEntity.ok(response);
    }
}
