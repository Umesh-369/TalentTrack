package com.umesh.talenttrack.service;

import com.umesh.talenttrack.domain.ActorType;
import com.umesh.talenttrack.domain.JobPosting;
import com.umesh.talenttrack.domain.JobStatus;
import com.umesh.talenttrack.repository.JobPostingRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class JobExpiryScheduler {

    private final JobPostingRepository jobPostingRepository;
    private final AuditLogService auditLogService;

    public JobExpiryScheduler(JobPostingRepository jobPostingRepository, AuditLogService auditLogService) {
        this.jobPostingRepository = jobPostingRepository;
        this.auditLogService = auditLogService;
    }

    // Runs daily at midnight
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void expireJobs() {
        LocalDateTime now = LocalDateTime.now();
        List<JobPosting> expiredJobs = jobPostingRepository.findExpiredJobs(now);

        for (JobPosting job : expiredJobs) {
            job.setStatus(JobStatus.EXPIRED);
            jobPostingRepository.save(job);

            auditLogService.log(
                    job.getCompany().getId(),
                    ActorType.SYSTEM,
                    0L, // Actor ID 0 for SYSTEM
                    "JOB_EXPIRED",
                    "JobPosting",
                    job.getId(),
                    Map.of("title", job.getTitle(), "expiredAt", now.toString())
            );
        }
    }
}
