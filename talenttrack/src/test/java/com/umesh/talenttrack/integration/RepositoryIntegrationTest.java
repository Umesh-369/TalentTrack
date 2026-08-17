package com.umesh.talenttrack.integration;

import com.umesh.talenttrack.TestcontainersConfiguration;
import com.umesh.talenttrack.domain.*;
import com.umesh.talenttrack.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
public class RepositoryIntegrationTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private RecruiterRepository recruiterRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private JobPostingRepository jobPostingRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    private Company companyA;
    private Company companyB;
    private Recruiter recruiterA;
    private Recruiter recruiterB;
    private JobPosting jobA;
    private JobPosting jobB;
    private Candidate candidate;

    @BeforeEach
    void setUp() {
        // Create Companies
        companyA = companyRepository.save(Company.builder()
                .name("Company A")
                .slug("company-a")
                .subscriptionTier(SubscriptionTier.STANDARD)
                .build());

        companyB = companyRepository.save(Company.builder()
                .name("Company B")
                .slug("company-b")
                .subscriptionTier(SubscriptionTier.PREMIUM)
                .build());

        // Create Recruiters
        recruiterA = recruiterRepository.save(Recruiter.builder()
                .company(companyA)
                .email("recruiter.a@companya.com")
                .passwordHash("hashed-pwd-a")
                .role(RecruiterRole.RECRUITER)
                .build());

        recruiterB = recruiterRepository.save(Recruiter.builder()
                .company(companyB)
                .email("recruiter.b@companyb.com")
                .passwordHash("hashed-pwd-b")
                .role(RecruiterRole.ADMIN)
                .build());

        // Create Job Postings
        jobA = jobPostingRepository.save(JobPosting.builder()
                .company(companyA)
                .title("Software Engineer")
                .description("Build awesome things at Company A")
                .location("New York")
                .remote(true)
                .status(JobStatus.PUBLISHED)
                .postedBy(recruiterA)
                .build());

        jobB = jobPostingRepository.save(JobPosting.builder()
                .company(companyB)
                .title("Product Manager")
                .description("Manage awesome products at Company B")
                .location("San Francisco")
                .remote(false)
                .status(JobStatus.PUBLISHED)
                .postedBy(recruiterB)
                .build());

        // Create Candidate
        candidate = candidateRepository.save(Candidate.builder()
                .fullName("John Doe")
                .email("john.doe@gmail.com")
                .passwordHash("hashed-pwd-candidate")
                .build());
    }

    @Test
    void testTenantIsolation_JobPosting() {
        // Recruiter from Company A should find Job A
        Optional<JobPosting> foundJobA = jobPostingRepository.findByIdAndCompanyId(jobA.getId(), companyA.getId());
        assertThat(foundJobA).isPresent();
        assertThat(foundJobA.get().getTitle()).isEqualTo("Software Engineer");

        // Recruiter from Company A should NOT find Job B (returns empty)
        Optional<JobPosting> foundJobBAsCompanyA = jobPostingRepository.findByIdAndCompanyId(jobB.getId(), companyA.getId());
        assertThat(foundJobBAsCompanyA).isEmpty();

        // Listing jobs for Company A should only return Job A
        List<JobPosting> companyAJobs = jobPostingRepository.findAllByCompanyId(companyA.getId());
        assertThat(companyAJobs).hasSize(1);
        assertThat(companyAJobs.get(0).getId()).isEqualTo(jobA.getId());
    }

    @Test
    void testTenantIsolation_Application() {
        // Create an application for Job A
        Application appA = applicationRepository.save(Application.builder()
                .candidate(candidate)
                .jobPosting(jobA)
                .status(ApplicationStatus.APPLIED)
                .build());

        // Create an application for Job B
        Application appB = applicationRepository.save(Application.builder()
                .candidate(candidate)
                .jobPosting(jobB)
                .status(ApplicationStatus.APPLIED)
                .build());

        // Recruiter from Company A should find Application A
        Optional<Application> foundAppA = applicationRepository.findByIdAndJobPostingCompanyId(appA.getId(), companyA.getId());
        assertThat(foundAppA).isPresent();

        // Recruiter from Company A should NOT find Application B (returns empty)
        Optional<Application> foundAppBAsCompanyA = applicationRepository.findByIdAndJobPostingCompanyId(appB.getId(), companyA.getId());
        assertThat(foundAppBAsCompanyA).isEmpty();

        // Listing applications for Company A should only return Application A
        List<Application> companyAApps = applicationRepository.findAllByJobPostingCompanyId(companyA.getId());
        assertThat(companyAApps).hasSize(1);
        assertThat(companyAApps.get(0).getId()).isEqualTo(appA.getId());
    }

    @Test
    void testUniqueCandidateJobConstraint() {
        // First application for Job A should succeed
        applicationRepository.saveAndFlush(Application.builder()
                .candidate(candidate)
                .jobPosting(jobA)
                .status(ApplicationStatus.APPLIED)
                .build());

        // Second application for the same Job A by the same Candidate should fail with DataIntegrityViolationException
        assertThatThrownBy(() -> {
            applicationRepository.saveAndFlush(Application.builder()
                    .candidate(candidate)
                    .jobPosting(jobA)
                    .status(ApplicationStatus.APPLIED)
                    .build());
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
