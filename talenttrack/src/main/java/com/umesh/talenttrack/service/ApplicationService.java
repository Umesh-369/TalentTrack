package com.umesh.talenttrack.service;

import com.umesh.talenttrack.domain.*;
import com.umesh.talenttrack.dto.ApplicationRequest;
import com.umesh.talenttrack.dto.ApplicationResponse;
import com.umesh.talenttrack.repository.ApplicationRepository;
import com.umesh.talenttrack.repository.CandidateRepository;
import com.umesh.talenttrack.repository.JobPostingRepository;
import com.umesh.talenttrack.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final CandidateRepository candidateRepository;
    private final JobPostingRepository jobPostingRepository;
    private final AuditLogService auditLogService;

    public ApplicationService(
            ApplicationRepository applicationRepository,
            CandidateRepository candidateRepository,
            JobPostingRepository jobPostingRepository,
            AuditLogService auditLogService) {
        this.applicationRepository = applicationRepository;
        this.candidateRepository = candidateRepository;
        this.jobPostingRepository = jobPostingRepository;
        this.auditLogService = auditLogService;
    }

    private ApplicationResponse mapToResponse(Application app) {
        return ApplicationResponse.builder()
                .id(app.getId())
                .candidateId(app.getCandidate().getId())
                .candidateName(app.getCandidate().getFullName())
                .candidateEmail(app.getCandidate().getEmail())
                .jobPostingId(app.getJobPosting().getId())
                .jobPostingTitle(app.getJobPosting().getTitle())
                .companyId(app.getJobPosting().getCompany().getId())
                .companyName(app.getJobPosting().getCompany().getName())
                .status(app.getStatus())
                .appliedAt(app.getAppliedAt())
                .updatedAt(app.getUpdatedAt())
                .version(app.getVersion())
                .build();
    }

    private void validateTransition(ApplicationStatus current, ApplicationStatus target) {
        // Invariant 4: Once HIRED or REJECTED, it is terminal
        if (current == ApplicationStatus.HIRED || current == ApplicationStatus.REJECTED) {
            throw new IllegalStateException("Application is in terminal state (" + current + ") and no further writes are permitted");
        }

        // Invariant 3: Transition edges
        boolean valid = false;
        if (current == ApplicationStatus.APPLIED) {
            valid = (target == ApplicationStatus.SHORTLISTED || target == ApplicationStatus.REJECTED);
        } else if (current == ApplicationStatus.SHORTLISTED) {
            valid = (target == ApplicationStatus.INTERVIEW || target == ApplicationStatus.REJECTED);
        } else if (current == ApplicationStatus.INTERVIEW) {
            valid = (target == ApplicationStatus.HIRED || target == ApplicationStatus.REJECTED);
        }

        if (!valid) {
            throw new IllegalStateException("Application status transition from " + current + " to " + target + " is rejected");
        }
    }

    @PreAuthorize("hasRole('CANDIDATE')")
    @Transactional
    public ApplicationResponse submitApplication(ApplicationRequest request, CustomUserDetails userDetails) {
        Candidate candidate = candidateRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found"));

        JobPosting job = jobPostingRepository.findById(request.getJobPostingId())
                .orElseThrow(() -> new IllegalArgumentException("Job posting not found"));

        // Invariant 10: Cannot apply to a job that is not PUBLISHED or is expired
        if (job.getStatus() != JobStatus.PUBLISHED) {
            throw new IllegalStateException("Cannot apply to a job that is not published");
        }
        if (job.getExpiresAt() != null && job.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Cannot apply to an expired job posting");
        }

        // Invariant 9: Enforce unique (candidateId, jobPostingId) in service layer too
        if (applicationRepository.findByCandidateIdAndJobPostingId(candidate.getId(), job.getId()).isPresent()) {
            throw new IllegalStateException("You have already applied to this job posting");
        }

        Application application = Application.builder()
                .candidate(candidate)
                .jobPosting(job)
                .status(ApplicationStatus.APPLIED)
                .build();

        Application savedApp = applicationRepository.save(application);

        auditLogService.log(
                job.getCompany().getId(),
                ActorType.CANDIDATE,
                candidate.getId(),
                "APPLICATION_SUBMITTED",
                "Application",
                savedApp.getId(),
                Map.of("jobId", job.getId(), "jobTitle", job.getTitle())
        );

        return mapToResponse(savedApp);
    }

    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Transactional
    public ApplicationResponse updateApplicationStatus(Long id, ApplicationStatus targetStatus, CustomUserDetails userDetails) {
        // Enforce tenant isolation at the query layer
        Application app = applicationRepository.findByIdAndJobPostingCompanyId(id, userDetails.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Application not found or access denied"));

        // Enforce state transitions (Invariants 3 and 4)
        validateTransition(app.getStatus(), targetStatus);

        app.setStatus(targetStatus);
        
        // Optimistic locking (Invariant 5) is verified upon saving/flushing when version matches
        Application savedApp = applicationRepository.save(app);

        auditLogService.log(
                savedApp.getJobPosting().getCompany().getId(),
                ActorType.RECRUITER,
                userDetails.getId(),
                "APPLICATION_STATUS_UPDATED",
                "Application",
                savedApp.getId(),
                Map.of("status", savedApp.getStatus().name())
        );

        return mapToResponse(savedApp);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationForRecruiter(Long id, CustomUserDetails userDetails) {
        Application app = applicationRepository.findByIdAndJobPostingCompanyId(id, userDetails.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Application not found or access denied"));
        return mapToResponse(app);
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getApplicationsForRecruiter(CustomUserDetails userDetails, Pageable pageable) {
        return applicationRepository.findAllByJobPostingCompanyId(userDetails.getCompanyId(), pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsForRecruiterByJob(Long jobPostingId, CustomUserDetails userDetails) {
        return applicationRepository.findAllByJobPostingIdAndJobPostingCompanyId(jobPostingId, userDetails.getCompanyId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getMyApplications(CustomUserDetails userDetails) {
        return applicationRepository.findAllByCandidateId(userDetails.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getMyApplication(Long id, CustomUserDetails userDetails) {
        Application app = applicationRepository.findByIdAndCandidateId(id, userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("Application not found or access denied"));
        return mapToResponse(app);
    }
}
