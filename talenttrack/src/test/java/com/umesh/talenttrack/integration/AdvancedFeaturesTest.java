package com.umesh.talenttrack.integration;

import com.umesh.talenttrack.TestcontainersConfiguration;
import com.umesh.talenttrack.domain.Company;
import com.umesh.talenttrack.domain.JobPosting;
import com.umesh.talenttrack.domain.JobStatus;
import com.umesh.talenttrack.domain.SubscriptionTier;
import com.umesh.talenttrack.repository.AuditLogRepository;
import com.umesh.talenttrack.repository.CompanyRepository;
import com.umesh.talenttrack.repository.JobPostingRepository;
import com.umesh.talenttrack.service.JobExpiryScheduler;
import com.umesh.talenttrack.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public class AdvancedFeaturesTest {

    @Autowired
    private JobPostingRepository jobPostingRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JobExpiryScheduler jobExpiryScheduler;

    @Autowired
    private NotificationService notificationService;

    private Company company;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        jobPostingRepository.deleteAll();
        companyRepository.deleteAll();

        company = companyRepository.save(Company.builder()
                .name("Acme")
                .slug("acme")
                .subscriptionTier(SubscriptionTier.FREE)
                .build());
    }

    @Test
    void testJobExpiryScheduler() {
        // Create an active job that is past its expiration date
        JobPosting expiredJob = jobPostingRepository.save(JobPosting.builder()
                .company(company)
                .title("Developer")
                .description("Desc")
                .location("New York")
                .remote(false)
                .status(JobStatus.PUBLISHED)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build());

        // Create an active job that has NOT expired yet
        JobPosting activeJob = jobPostingRepository.save(JobPosting.builder()
                .company(company)
                .title("Manager")
                .description("Desc")
                .location("New York")
                .remote(false)
                .status(JobStatus.PUBLISHED)
                .expiresAt(LocalDateTime.now().plusDays(2))
                .build());

        // Run scheduler
        jobExpiryScheduler.expireJobs();

        // Verify status changes
        JobPosting updatedExpiredJob = jobPostingRepository.findById(expiredJob.getId()).get();
        JobPosting updatedActiveJob = jobPostingRepository.findById(activeJob.getId()).get();

        assertThat(updatedExpiredJob.getStatus()).isEqualTo(JobStatus.EXPIRED);
        assertThat(updatedActiveJob.getStatus()).isEqualTo(JobStatus.PUBLISHED);

        // Verify audit logs were written
        assertThat(auditLogRepository.findAll()).hasSize(1);
        assertThat(auditLogRepository.findAll().get(0).getAction()).isEqualTo("JOB_EXPIRED");
    }

    @Test
    void testResilience4jCircuitBreakerFallback() {
        // Capture standard output to verify fallback print statements
        PrintStream originalOut = System.out;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(bos));

        try {
            // Trigger a failure by sending to an email that triggers an error
            notificationService.sendResetEmail("fail-external@gmail.com", "reset-token-123");

            // Verify that the fallback printed its message instead of throwing an exception
            String logOutput = bos.toString();
            assertThat(logOutput).contains("Resilience4j Fallback triggered");
        } finally {
            System.setOut(originalOut);
        }
    }
}
