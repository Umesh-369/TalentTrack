package com.umesh.talenttrack.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.umesh.talenttrack.TestcontainersConfiguration;
import com.umesh.talenttrack.dto.GoogleLoginRequest;
import com.umesh.talenttrack.repository.CandidateRepository;
import com.umesh.talenttrack.repository.RefreshTokenRepository;
import com.umesh.talenttrack.service.GoogleAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public class GoogleAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @MockBean
    private GoogleAuthService googleAuthService;

    @BeforeEach
    void clean() {
        refreshTokenRepository.deleteAll();
        candidateRepository.deleteAll();
    }

    @Test
    void testGoogleLoginOrRegisterCandidateFlow() throws Exception {
        // Setup mock response from Google Token Verifier
        GoogleIdToken.Payload mockPayload = new GoogleIdToken.Payload();
        mockPayload.setEmail("jane.google@gmail.com");
        mockPayload.set("name", "Jane Google");
        when(googleAuthService.verifyToken(anyString())).thenReturn(mockPayload);

        GoogleLoginRequest request = GoogleLoginRequest.builder()
                .idToken("dummy-google-id-token")
                .build();

        // Perform Google Social Login
        mockMvc.perform(post("/api/auth/candidate/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ROLE_CANDIDATE"))
                .andExpect(jsonPath("$.email").value("jane.google@gmail.com"));

        // Verify Candidate was created in DB
        assertThat(candidateRepository.findByEmail("jane.google@gmail.com")).isPresent();
        assertThat(candidateRepository.findByEmail("jane.google@gmail.com").get().getFullName()).isEqualTo("Jane Google");
    }
}
