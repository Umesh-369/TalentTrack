package com.umesh.talenttrack.controller;

import com.umesh.talenttrack.domain.Candidate;
import com.umesh.talenttrack.domain.Recruiter;
import com.umesh.talenttrack.dto.*;
import com.umesh.talenttrack.exception.ErrorResponse;
import com.umesh.talenttrack.security.LoginRateLimiterService;
import com.umesh.talenttrack.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiterService rateLimiterService;

    public AuthController(AuthService authService, LoginRateLimiterService rateLimiterService) {
        this.authService = authService;
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping("/recruiter/register")
    public ResponseEntity<Recruiter> registerRecruiter(@Valid @RequestBody RecruiterRegisterRequest request) {
        Recruiter recruiter = authService.registerRecruiter(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(recruiter);
    }

    @PostMapping("/candidate/register")
    public ResponseEntity<Candidate> registerCandidate(@Valid @RequestBody CandidateRegisterRequest request) {
        Candidate candidate = authService.registerCandidate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(candidate);
    }

    @PostMapping("/candidate/google")
    public ResponseEntity<AuthResponse> loginGoogleCandidate(@Valid @RequestBody GoogleLoginRequest request) {
        AuthResponse response = authService.loginOrRegisterCandidateWithGoogle(request.getIdToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/recruiter/login")
    public ResponseEntity<?> loginRecruiter(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        
        // 1. General IP rate limit
        if (!rateLimiterService.checkIpRateLimit(ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                    ErrorResponse.builder()
                            .timestamp(LocalDateTime.now())
                            .status(HttpStatus.TOO_MANY_REQUESTS.value())
                            .error("Too Many Requests")
                            .message("IP rate limit exceeded. Try again in a minute.")
                            .path(httpRequest.getRequestURI())
                            .build()
            );
        }

        // 2. Lockout check
        if (rateLimiterService.isLockedOut(request.getEmail(), ip)) {
            long remainingSecs = rateLimiterService.getRemainingSeconds(request.getEmail(), ip);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                    ErrorResponse.builder()
                            .timestamp(LocalDateTime.now())
                            .status(HttpStatus.TOO_MANY_REQUESTS.value())
                            .error("Locked")
                            .message("Account locked out due to multiple failed attempts. Try again in " + remainingSecs + " seconds.")
                            .path(httpRequest.getRequestURI())
                            .build()
            );
        }

        // 3. Login attempt
        try {
            AuthResponse response = authService.loginRecruiter(request);
            rateLimiterService.resetFailures(request.getEmail(), ip);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException ex) {
            rateLimiterService.recordFailure(request.getEmail(), ip);
            throw ex; // Bubbles to exception handler
        }
    }

    @PostMapping("/candidate/login")
    public ResponseEntity<?> loginCandidate(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        
        // 1. General IP rate limit
        if (!rateLimiterService.checkIpRateLimit(ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                    ErrorResponse.builder()
                            .timestamp(LocalDateTime.now())
                            .status(HttpStatus.TOO_MANY_REQUESTS.value())
                            .error("Too Many Requests")
                            .message("IP rate limit exceeded. Try again in a minute.")
                            .path(httpRequest.getRequestURI())
                            .build()
            );
        }

        // 2. Lockout check
        if (rateLimiterService.isLockedOut(request.getEmail(), ip)) {
            long remainingSecs = rateLimiterService.getRemainingSeconds(request.getEmail(), ip);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                    ErrorResponse.builder()
                            .timestamp(LocalDateTime.now())
                            .status(HttpStatus.TOO_MANY_REQUESTS.value())
                            .error("Locked")
                            .message("Account locked out due to multiple failed attempts. Try again in " + remainingSecs + " seconds.")
                            .path(httpRequest.getRequestURI())
                            .build()
            );
        }

        // 3. Login attempt
        try {
            AuthResponse response = authService.loginCandidate(request);
            rateLimiterService.resetFailures(request.getEmail(), ip);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException ex) {
            rateLimiterService.recordFailure(request.getEmail(), ip);
            throw ex; // Bubbles to exception handler
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<?> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        String token = authService.requestPasswordReset(request);
        // In production, we send the token via email. For development/testing/API contract ease, we return it.
        return ResponseEntity.ok().body(java.util.Map.of(
                "message", "Password reset token generated successfully",
                "token", token
        ));
    }

    @PostMapping("/password-reset/complete")
    public ResponseEntity<Void> completePasswordReset(@Valid @RequestBody PasswordResetCompleteRequest request) {
        authService.completePasswordReset(request);
        return ResponseEntity.ok().build();
    }
}
