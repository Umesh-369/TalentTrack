package com.umesh.talenttrack.service;

import com.umesh.talenttrack.domain.*;
import com.umesh.talenttrack.dto.JobPostingRequest;
import com.umesh.talenttrack.dto.JobPostingResponse;
import com.umesh.talenttrack.repository.JobPostingRepository;
import com.umesh.talenttrack.repository.RecruiterRepository;
import com.umesh.talenttrack.security.CustomUserDetails;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class JobPostingService {

    private final JobPostingRepository jobPostingRepository;
    private final RecruiterRepository recruiterRepository;
    private final AuditLogService auditLogService;

    public JobPostingService(
            JobPostingRepository jobPostingRepository,
            RecruiterRepository recruiterRepository,
            AuditLogService auditLogService) {
        this.jobPostingRepository = jobPostingRepository;
        this.recruiterRepository = recruiterRepository;
        this.auditLogService = auditLogService;
    }

    private JobPostingResponse mapToResponse(JobPosting job) {
        return JobPostingResponse.builder()
                .id(job.getId())
                .companyId(job.getCompany().getId())
                .companyName(job.getCompany().getName())
                .title(job.getTitle())
                .description(job.getDescription())
                .location(job.getLocation())
                .experienceMin(job.getExperienceMin())
                .experienceMax(job.getExperienceMax())
                .remote(job.getRemote())
                .status(job.getStatus())
                .postedById(job.getPostedBy() != null ? job.getPostedBy().getId() : null)
                .postedByEmail(job.getPostedBy() != null ? job.getPostedBy().getEmail() : null)
                .createdAt(job.getCreatedAt())
                .expiresAt(job.getExpiresAt())
                .build();
    }

    @CacheEvict(cacheNames = "jobs-search", allEntries = true)
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Transactional
    public JobPostingResponse createJob(JobPostingRequest request, CustomUserDetails userDetails) {
        Recruiter recruiter = recruiterRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("Recruiter not found"));

        JobPosting job = JobPosting.builder()
                .company(recruiter.getCompany())
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .experienceMin(request.getExperienceMin())
                .experienceMax(request.getExperienceMax())
                .remote(request.getRemote() != null ? request.getRemote() : false)
                .status(JobStatus.DRAFT)
                .postedBy(recruiter)
                .expiresAt(request.getExpiresAt())
                .build();

        JobPosting savedJob = jobPostingRepository.save(job);

        auditLogService.log(
                recruiter.getCompany().getId(),
                ActorType.RECRUITER,
                recruiter.getId(),
                "JOB_CREATED",
                "JobPosting",
                savedJob.getId(),
                Map.of("title", savedJob.getTitle(), "status", savedJob.getStatus().name())
        );

        return mapToResponse(savedJob);
    }

    @CacheEvict(cacheNames = "jobs-search", allEntries = true)
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Transactional
    public JobPostingResponse updateJob(Long id, JobPostingRequest request, CustomUserDetails userDetails) {
        // Enforce tenant isolation at the query layer
        JobPosting job = jobPostingRepository.findByIdAndCompanyId(id, userDetails.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Job posting not found or access denied"));

        // Enforce Invariant 8: Creator or Admin
        boolean isCreator = job.getPostedBy() != null && job.getPostedBy().getId().equals(userDetails.getId());
        boolean isAdmin = userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

        if (!isCreator && !isAdmin) {
            throw new AccessDeniedException("Only the recruiter who posted the job or a company admin can edit it");
        }

        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setLocation(request.getLocation());
        job.setExperienceMin(request.getExperienceMin());
        job.setExperienceMax(request.getExperienceMax());
        job.setRemote(request.getRemote() != null ? request.getRemote() : false);
        job.setExpiresAt(request.getExpiresAt());

        JobPosting savedJob = jobPostingRepository.save(job);

        auditLogService.log(
                userDetails.getCompanyId(),
                ActorType.RECRUITER,
                userDetails.getId(),
                "JOB_UPDATED",
                "JobPosting",
                savedJob.getId(),
                Map.of("title", savedJob.getTitle())
        );

        return mapToResponse(savedJob);
    }

    @CacheEvict(cacheNames = "jobs-search", allEntries = true)
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Transactional
    public JobPostingResponse publishJob(Long id, CustomUserDetails userDetails) {
        JobPosting job = jobPostingRepository.findByIdAndCompanyId(id, userDetails.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Job posting not found or access denied"));

        boolean isCreator = job.getPostedBy() != null && job.getPostedBy().getId().equals(userDetails.getId());
        boolean isAdmin = userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

        if (!isCreator && !isAdmin) {
            throw new AccessDeniedException("Only the recruiter who posted the job or a company admin can publish it");
        }

        // Enforce Invariant 6: Must have title, description, and remote=true or location set
        if (job.getTitle() == null || job.getTitle().isBlank() ||
            job.getDescription() == null || job.getDescription().isBlank() ||
            ((job.getLocation() == null || job.getLocation().isBlank()) && (job.getRemote() == null || !job.getRemote()))) {
            throw new IllegalStateException("A JobPosting cannot be PUBLISHED without title, description, and at least one of location or remote=true set");
        }

        job.setStatus(JobStatus.PUBLISHED);
        JobPosting savedJob = jobPostingRepository.save(job);

        auditLogService.log(
                userDetails.getCompanyId(),
                ActorType.RECRUITER,
                userDetails.getId(),
                "JOB_PUBLISHED",
                "JobPosting",
                savedJob.getId(),
                Map.of("title", savedJob.getTitle())
        );

        return mapToResponse(savedJob);
    }

    @CacheEvict(cacheNames = "jobs-search", allEntries = true)
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Transactional
    public JobPostingResponse closeJob(Long id, CustomUserDetails userDetails) {
        JobPosting job = jobPostingRepository.findByIdAndCompanyId(id, userDetails.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Job posting not found or access denied"));

        boolean isCreator = job.getPostedBy() != null && job.getPostedBy().getId().equals(userDetails.getId());
        boolean isAdmin = userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

        if (!isCreator && !isAdmin) {
            throw new AccessDeniedException("Only the recruiter who posted the job or a company admin can close it");
        }

        job.setStatus(JobStatus.CLOSED);
        JobPosting savedJob = jobPostingRepository.save(job);

        auditLogService.log(
                userDetails.getCompanyId(),
                ActorType.RECRUITER,
                userDetails.getId(),
                "JOB_CLOSED",
                "JobPosting",
                savedJob.getId(),
                Map.of("title", savedJob.getTitle())
        );

        return mapToResponse(savedJob);
    }

    @Transactional(readOnly = true)
    public JobPostingResponse getJobForRecruiter(Long id, CustomUserDetails userDetails) {
        JobPosting job = jobPostingRepository.findByIdAndCompanyId(id, userDetails.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Job posting not found or access denied"));
        return mapToResponse(job);
    }

    @Transactional(readOnly = true)
    public Page<JobPostingResponse> getJobsForRecruiter(CustomUserDetails userDetails, Pageable pageable) {
        return jobPostingRepository.findAllByCompanyId(userDetails.getCompanyId(), pageable)
                .map(this::mapToResponse);
    }

    @Cacheable(cacheNames = "jobs-search", key = "{#location, #remote, #experienceMin, #experienceMax, #pageable.pageNumber, #pageable.pageSize}")
    @Transactional(readOnly = true)
    public Page<JobPostingResponse> searchJobsForCandidate(
            String location, Boolean remote, Integer experienceMin, Integer experienceMax, Pageable pageable) {
        // Enforces Invariant 7 (expiresAt check at query time)
        return jobPostingRepository.searchActiveJobs(
                LocalDateTime.now(),
                location,
                remote,
                experienceMin,
                experienceMax,
                pageable
        ).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public JobPostingResponse getJobForCandidate(Long id) {
        JobPosting job = jobPostingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job posting not found"));
        
        // Hide if expired or not published for candidate views
        if (job.getStatus() != JobStatus.PUBLISHED || (job.getExpiresAt() != null && job.getExpiresAt().isBefore(LocalDateTime.now()))) {
            throw new IllegalArgumentException("Job posting is not active");
        }
        
        return mapToResponse(job);
    }
}
