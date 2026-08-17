package com.umesh.talenttrack.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umesh.talenttrack.TestcontainersConfiguration;
import com.umesh.talenttrack.dto.CandidateRegisterRequest;
import com.umesh.talenttrack.dto.LoginRequest;
import com.umesh.talenttrack.dto.PasswordResetCompleteRequest;
import com.umesh.talenttrack.dto.PasswordResetRequest;
import com.umesh.talenttrack.dto.RecruiterRegisterRequest;
import com.umesh.talenttrack.dto.RefreshTokenRequest;
import com.umesh.talenttrack.repository.CandidateRepository;
import com.umesh.talenttrack.repository.CompanyRepository;
import com.umesh.talenttrack.repository.PasswordResetTokenRepository;
import com.umesh.talenttrack.repository.RecruiterRepository;
import com.umesh.talenttrack.repository.RefreshTokenRepository;
import com.umesh.talenttrack.security.LoginRateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RecruiterRepository recruiterRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private LoginRateLimiterService rateLimiterService;

    @BeforeEach
    void cleanDb() {
        refreshTokenRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        recruiterRepository.deleteAll();
        candidateRepository.deleteAll();
        companyRepository.deleteAll();
    }

    @Test
    void testRecruiterRegisterAndLoginFlow() throws Exception {
        RecruiterRegisterRequest register = RecruiterRegisterRequest.builder()
                .email("admin@acme.com")
                .password("secure123")
                .companyName("Acme Corp")
                .companySlug("acme-corp")
                .build();

        // 1. Register Recruiter
        mockMvc.perform(post("/api/auth/recruiter/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("admin@acme.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        // 2. Login Recruiter
        LoginRequest login = LoginRequest.builder()
                .email("admin@acme.com")
                .password("secure123")
                .build();

        mockMvc.perform(post("/api/auth/recruiter/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }

    @Test
    void testCandidateRegisterAndLoginFlow() throws Exception {
        CandidateRegisterRequest register = CandidateRegisterRequest.builder()
                .email("candidate@gmail.com")
                .password("candidate123")
                .fullName("Jane Doe")
                .resumeUrl("http://s3.com/jane-resume.pdf")
                .build();

        // 1. Register Candidate
        mockMvc.perform(post("/api/auth/candidate/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("candidate@gmail.com"))
                .andExpect(jsonPath("$.fullName").value("Jane Doe"));

        // 2. Login Candidate
        LoginRequest login = LoginRequest.builder()
                .email("candidate@gmail.com")
                .password("candidate123")
                .build();

        mockMvc.perform(post("/api/auth/candidate/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ROLE_CANDIDATE"));
    }

    @Test
    void testRefreshTokenRotation() throws Exception {
        // Register & Login Recruiter
        RecruiterRegisterRequest register = RecruiterRegisterRequest.builder()
                .email("hr@tech.com")
                .password("pass123")
                .companyName("Tech Inc")
                .companySlug("tech-inc")
                .build();
        
        mockMvc.perform(post("/api/auth/recruiter/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(register)));

        LoginRequest login = LoginRequest.builder()
                .email("hr@tech.com")
                .password("pass123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/recruiter/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        String body = loginResult.getResponse().getContentAsString();
        Map<?, ?> responseMap = objectMapper.readValue(body, Map.class);
        String rawRefreshToken = (String) responseMap.get("refreshToken");

        // 1. Refresh using old token
        RefreshTokenRequest refreshReq = RefreshTokenRequest.builder()
                .refreshToken(rawRefreshToken)
                .build();

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        String refreshBody = refreshResult.getResponse().getContentAsString();
        Map<?, ?> refreshResponseMap = objectMapper.readValue(refreshBody, Map.class);
        String newRefreshToken = (String) refreshResponseMap.get("refreshToken");
        assertThat(newRefreshToken).isNotEqualTo(rawRefreshToken); // Rotated!

        // 2. Refresh again using the OLD token (should fail since it was rotated out)
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isBadRequest()); // 400 Bad Request (Invalid refresh token)
    }

    @Test
    void testLoginLimiterLockout() throws Exception {
        // Register Recruiter
        RecruiterRegisterRequest register = RecruiterRegisterRequest.builder()
                .email("test-limit@corp.com")
                .password("correctpassword")
                .companyName("Limit Corp")
                .companySlug("limit-corp")
                .build();
        mockMvc.perform(post("/api/auth/recruiter/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(register)));

        // Try login with incorrect password 5 times
        LoginRequest wrongLogin = LoginRequest.builder()
                .email("test-limit@corp.com")
                .password("wrongpassword")
                .build();

        String ip = "127.0.0.1";
        
        // Reset rate limiter cache first to ensure a clean slate
        rateLimiterService.resetFailures("test-limit@corp.com", ip);

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/recruiter/login")
                            .with(req -> {
                                req.setRemoteAddr(ip);
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongLogin)))
                    .andExpect(status().isUnauthorized());
        }

        // The 6th attempt (even with CORRECT password) should be locked out with 429 Too Many Requests
        LoginRequest correctLogin = LoginRequest.builder()
                .email("test-limit@corp.com")
                .password("correctpassword")
                .build();

        mockMvc.perform(post("/api/auth/recruiter/login")
                        .with(req -> {
                            req.setRemoteAddr(ip);
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctLogin)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Locked"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Account locked out")));
    }

    @Test
    void testPasswordResetFlow() throws Exception {
        // Register Candidate
        CandidateRegisterRequest register = CandidateRegisterRequest.builder()
                .email("reset@gmail.com")
                .password("oldpwd123")
                .fullName("Reset User")
                .build();
        mockMvc.perform(post("/api/auth/candidate/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(register)));

        // 1. Request Password Reset
        PasswordResetRequest resetReq = PasswordResetRequest.builder()
                .email("reset@gmail.com")
                .userType("CANDIDATE")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        Map<?, ?> map = objectMapper.readValue(body, Map.class);
        String token = (String) map.get("token");

        // 2. Complete Password Reset
        PasswordResetCompleteRequest completeReq = PasswordResetCompleteRequest.builder()
                .token(token)
                .newPassword("newpwd123")
                .build();

        mockMvc.perform(post("/api/auth/password-reset/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeReq)))
                .andExpect(status().isOk());

        // 3. Login with new password should succeed
        LoginRequest newLogin = LoginRequest.builder()
                .email("reset@gmail.com")
                .password("newpwd123")
                .build();

        mockMvc.perform(post("/api/auth/candidate/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        // 4. Try reset with same token again (should fail)
        mockMvc.perform(post("/api/auth/password-reset/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeReq)))
                .andExpect(status().isBadRequest()); // Expired or used
    }
}
