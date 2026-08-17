package com.umesh.talenttrack.service;

import com.umesh.talenttrack.domain.*;
import com.umesh.talenttrack.dto.InterviewRequest;
import com.umesh.talenttrack.dto.InterviewResponse;
import com.umesh.talenttrack.repository.ApplicationRepository;
import com.umesh.talenttrack.repository.InterviewRepository;
import com.umesh.talenttrack.repository.RecruiterRepository;
import com.umesh.talenttrack.security.CustomUserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final ApplicationRepository applicationRepository;
    private final RecruiterRepository recruiterRepository;
    private final AuditLogService auditLogService;

    public InterviewService(
            InterviewRepository interviewRepository,
            ApplicationRepository applicationRepository,
            RecruiterRepository recruiterRepository,
            AuditLogService auditLogService) {
        this.interviewRepository = interviewRepository;
        this.applicationRepository = applicationRepository;
        this.recruiterRepository = recruiterRepository;
        this.auditLogService = auditLogService;
    }

    private InterviewResponse mapToResponse(Interview interview) {
        return InterviewResponse.builder()
                .id(interview.getId())
                .applicationId(interview.getApplication().getId())
                .candidateName(interview.getApplication().getCandidate().getFullName())
                .jobPostingTitle(interview.getApplication().getJobPosting().getTitle())
                .scheduledById(interview.getScheduledBy().getId())
                .scheduledByName(interview.getScheduledBy().getEmail()) // using email as display name
                .scheduledAt(interview.getScheduledAt())
                .mode(interview.getMode())
                .notes(interview.getNotes())
                .outcome(interview.getOutcome())
                .build();
    }

    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Transactional
    public InterviewResponse scheduleInterview(InterviewRequest request, CustomUserDetails userDetails) {
        // Enforce tenant isolation at the query layer
        Application application = applicationRepository.findByIdAndJobPostingCompanyId(
                request.getApplicationId(), userDetails.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Application not found or access denied"));

        Recruiter recruiter = recruiterRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("Recruiter not found"));

        Interview interview = Interview.builder()
                .application(application)
                .scheduledBy(recruiter)
                .scheduledAt(request.getScheduledAt())
                .mode(request.getMode())
                .notes(request.getNotes())
                .outcome(InterviewOutcome.PENDING)
                .build();

        Interview savedInterview = interviewRepository.save(interview);

        auditLogService.log(
                application.getJobPosting().getCompany().getId(),
                ActorType.RECRUITER,
                userDetails.getId(),
                "INTERVIEW_SCHEDULED",
                "Interview",
                savedInterview.getId(),
                Map.of("applicationId", application.getId(), "scheduledAt", request.getScheduledAt().toString())
        );

        return mapToResponse(savedInterview);
    }

    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Transactional
    public InterviewResponse updateInterviewOutcome(Long id, InterviewOutcome outcome, CustomUserDetails userDetails) {
        // Enforce tenant isolation at the query layer
        Interview interview = interviewRepository.findByIdAndApplicationJobPostingCompanyId(id, userDetails.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Interview not found or access denied"));

        interview.setOutcome(outcome);
        Interview savedInterview = interviewRepository.save(interview);

        auditLogService.log(
                savedInterview.getApplication().getJobPosting().getCompany().getId(),
                ActorType.RECRUITER,
                userDetails.getId(),
                "INTERVIEW_OUTCOME_UPDATED",
                "Interview",
                savedInterview.getId(),
                Map.of("outcome", outcome.name())
        );

        return mapToResponse(savedInterview);
    }

    @Transactional(readOnly = true)
    public List<InterviewResponse> getInterviewsForApplication(Long applicationId, CustomUserDetails userDetails) {
        if (userDetails.getUserType() == UserType.CANDIDATE) {
            // Verify candidate ownership
            return interviewRepository.findAllByApplicationIdAndApplicationCandidateId(applicationId, userDetails.getId())
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        } else {
            // Verify recruiter company ownership
            return interviewRepository.findAllByApplicationIdAndApplicationJobPostingCompanyId(applicationId, userDetails.getCompanyId())
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }
    }
}
