package com.umesh.talenttrack.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umesh.talenttrack.TestcontainersConfiguration;
import com.umesh.talenttrack.domain.*;
import com.umesh.talenttrack.repository.*;
import com.umesh.talenttrack.security.CustomUserDetails;
import com.umesh.talenttrack.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public class CoreIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JwtProvider jwtProvider;

    private Company companyA;
    private Company companyB;
    private Recruiter recruiterA;
    private Recruiter recruiterB;
    private Candidate candidate;
    private JobPosting jobB;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        applicationRepository.deleteAll();
        jobPostingRepository.deleteAll();
        recruiterRepository.deleteAll();
        candidateRepository.deleteAll();
        companyRepository.deleteAll();

        // Save Companies
        companyA = companyRepository.save(Company.builder().name("Company A").slug("company-a").subscriptionTier(SubscriptionTier.FREE).build());
        companyB = companyRepository.save(Company.builder().name("Company B").slug("company-b").subscriptionTier(SubscriptionTier.FREE).build());

        // Save Recruiters
        recruiterA = recruiterRepository.save(Recruiter.builder().company(companyA).email("recruiter.a@companya.com").passwordHash("pwd").role(RecruiterRole.RECRUITER).build());
        recruiterB = recruiterRepository.save(Recruiter.builder().company(companyB).email("recruiter.b@companyb.com").passwordHash("pwd").role(RecruiterRole.RECRUITER).build());

        // Save Candidate
        candidate = candidateRepository.save(Candidate.builder().fullName("Jane Doe").email("jane@gmail.com").passwordHash("pwd").build());

        // Save Jobs
        jobPostingRepository.save(JobPosting.builder()
                .company(companyA)
                .title("Role A")
                .description("Desc A")
                .location("Boston")
                .remote(false)
                .status(JobStatus.DRAFT)
                .postedBy(recruiterA)
                .build());

        jobB = jobPostingRepository.save(JobPosting.builder()
                .company(companyB)
                .title("Role B")
                .description("Desc B")
                .location("Austin")
                .remote(true)
                .status(JobStatus.PUBLISHED)
                .postedBy(recruiterB)
                .build());
    }

    private String getAuthHeader(Recruiter recruiter) {
        String roleName = "ROLE_" + recruiter.getRole().name();
        CustomUserDetails details = new CustomUserDetails(
                recruiter.getId(),
                recruiter.getEmail(),
                recruiter.getPasswordHash(),
                UserType.RECRUITER,
                recruiter.getCompany().getId(),
                List.of(new SimpleGrantedAuthority(roleName))
        );
        return "Bearer " + jwtProvider.generateToken(details);
    }

    @Test
    void testTenantIsolation_JobMutationRejected() throws Exception {
        String tokenA = getAuthHeader(recruiterA);

        // Recruiter A attempts to edit Job B (which belongs to Company B)
        // This should return 400 Bad Request or 403 Forbidden because findByIdAndCompanyId returns empty, throwing IllegalArgumentException
        mockMvc.perform(patch("/api/jobs/" + jobB.getId())
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hacked Title\",\"description\":\"Hacked Desc\"}"))
                .andExpect(status().isBadRequest()); // Maps to 400 because JobPostingService throws IllegalArgumentException
    }

    @Test
    void testJobPostingValidation_PublishWithoutRemoteOrLocation() throws Exception {
        String tokenA = getAuthHeader(recruiterA);

        // Create job with neither location nor remote=true
        JobPosting invalidJob = jobPostingRepository.save(JobPosting.builder()
                .company(companyA)
                .title("No Location Role")
                .description("Description")
                .remote(false) // remote false
                .location(null) // location null
                .status(JobStatus.DRAFT)
                .postedBy(recruiterA)
                .build());

        // Attempting to publish this job posting should fail with 409 Conflict (IllegalStateException)
        mockMvc.perform(post("/api/jobs/" + invalidJob.getId() + "/publish")
                        .header("Authorization", tokenA))
                .andExpect(status().isConflict());
    }

    @Test
    void testApplicationStateMachineTransitions() throws Exception {
        String tokenB = getAuthHeader(recruiterB);

        // Candidate applies to Job B (which is PUBLISHED)
        Application app = applicationRepository.save(Application.builder()
                .candidate(candidate)
                .jobPosting(jobB)
                .status(ApplicationStatus.APPLIED)
                .build());

        // 1. Valid Transition: APPLIED -> SHORTLISTED
        mockMvc.perform(patch("/api/applications/" + app.getId() + "/status")
                        .header("Authorization", tokenB)
                        .param("status", "SHORTLISTED"))
                .andExpect(status().isOk());

        // Verify status
        assertThat(applicationRepository.findById(app.getId()).get().getStatus()).isEqualTo(ApplicationStatus.SHORTLISTED);

        // 2. Invalid Transition: SHORTLISTED -> HIRED (must fail state machine edges: SHORTLISTED -> INTERVIEW first!)
        mockMvc.perform(patch("/api/applications/" + app.getId() + "/status")
                        .header("Authorization", tokenB)
                        .param("status", "HIRED"))
                .andExpect(status().isConflict()); // Returns 409 Conflict

        // 3. Valid Transition: SHORTLISTED -> INTERVIEW
        mockMvc.perform(patch("/api/applications/" + app.getId() + "/status")
                        .header("Authorization", tokenB)
                        .param("status", "INTERVIEW"))
                .andExpect(status().isOk());

        // 4. Valid Transition: INTERVIEW -> HIRED (terminal)
        mockMvc.perform(patch("/api/applications/" + app.getId() + "/status")
                        .header("Authorization", tokenB)
                        .param("status", "HIRED"))
                .andExpect(status().isOk());

        // 5. Invalid Transition: HIRED -> REJECTED (HIRED is terminal, no more writes permitted!)
        mockMvc.perform(patch("/api/applications/" + app.getId() + "/status")
                        .header("Authorization", tokenB)
                        .param("status", "REJECTED"))
                .andExpect(status().isConflict());
    }

    @Test
    void testOptimisticLockingConflict() {
        // Create application
        Application app = applicationRepository.save(Application.builder()
                .candidate(candidate)
                .jobPosting(jobB)
                .status(ApplicationStatus.APPLIED)
                .build());

        // Fetch two instances concurrently (representing two concurrent requests)
        Application appInstance1 = applicationRepository.findById(app.getId()).get();
        Application appInstance2 = applicationRepository.findById(app.getId()).get();

        // 1. First transaction updates and saves
        appInstance1.setStatus(ApplicationStatus.SHORTLISTED);
        applicationRepository.saveAndFlush(appInstance1); // succeeds, increments version

        // 2. Second transaction attempts to save with stale version
        appInstance2.setStatus(ApplicationStatus.REJECTED);
        
        assertThatThrownBy(() -> {
            applicationRepository.saveAndFlush(appInstance2);
        }).isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
