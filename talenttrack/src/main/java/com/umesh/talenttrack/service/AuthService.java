package com.umesh.talenttrack.service;

import com.umesh.talenttrack.domain.*;
import com.umesh.talenttrack.dto.*;
import com.umesh.talenttrack.repository.*;
import com.umesh.talenttrack.security.CustomUserDetails;
import com.umesh.talenttrack.security.JwtProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final CompanyRepository companyRepository;
    private final RecruiterRepository recruiterRepository;
    private final CandidateRepository candidateRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final GoogleAuthService googleAuthService;
    private final NotificationService notificationService;

    public AuthService(
            CompanyRepository companyRepository,
            RecruiterRepository recruiterRepository,
            CandidateRepository candidateRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtProvider jwtProvider,
            GoogleAuthService googleAuthService,
            NotificationService notificationService) {
        this.companyRepository = companyRepository;
        this.recruiterRepository = recruiterRepository;
        this.candidateRepository = candidateRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.googleAuthService = googleAuthService;
        this.notificationService = notificationService;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing token", e);
        }
    }

    @Transactional
    public Recruiter registerRecruiter(RecruiterRegisterRequest request) {
        if (recruiterRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Lookup or create company
        Company company = companyRepository.findBySlug(request.getCompanySlug())
                .orElseGet(() -> companyRepository.save(
                        Company.builder()
                                .name(request.getCompanyName())
                                .slug(request.getCompanySlug())
                                .subscriptionTier(SubscriptionTier.FREE)
                                .build()
                ));

        // If it's the first recruiter, make them ADMIN
        List<Recruiter> existingRecruiters = recruiterRepository.findAllByCompanyId(company.getId());
        RecruiterRole role = existingRecruiters.isEmpty() ? RecruiterRole.ADMIN : RecruiterRole.RECRUITER;

        Recruiter recruiter = Recruiter.builder()
                .company(company)
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        return recruiterRepository.save(recruiter);
    }

    @Transactional
    public Candidate registerCandidate(CandidateRegisterRequest request) {
        if (candidateRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        Candidate candidate = Candidate.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .resumeUrl(request.getResumeUrl())
                .build();

        return candidateRepository.save(candidate);
    }

    @Transactional
    public AuthResponse loginRecruiter(LoginRequest request) {
        Recruiter recruiter = recruiterRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), recruiter.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String roleName = "ROLE_" + recruiter.getRole().name();
        CustomUserDetails userDetails = new CustomUserDetails(
                recruiter.getId(),
                recruiter.getEmail(),
                recruiter.getPasswordHash(),
                UserType.RECRUITER,
                recruiter.getCompany().getId(),
                List.of(new SimpleGrantedAuthority(roleName))
        );

        return createAuthResponse(userDetails);
    }

    @Transactional
    public AuthResponse loginCandidate(LoginRequest request) {
        Candidate candidate = candidateRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), candidate.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        CustomUserDetails userDetails = new CustomUserDetails(
                candidate.getId(),
                candidate.getEmail(),
                candidate.getPasswordHash(),
                UserType.CANDIDATE,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
        );

        return createAuthResponse(userDetails);
    }

    private AuthResponse createAuthResponse(CustomUserDetails userDetails) {
        String accessToken = jwtProvider.generateToken(userDetails);
        
        // Generate and hash Refresh Token
        String rawRefreshToken = UUID.randomUUID().toString();
        String tokenHash = hashToken(rawRefreshToken);
        
        // Remove old refresh tokens for this user first
        refreshTokenRepository.deleteByUserIdAndUserType(userDetails.getId(), userDetails.getUserType());

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .userId(userDetails.getId())
                .userType(userDetails.getUserType())
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        
        refreshTokenRepository.save(refreshTokenEntity);

        String role = userDetails.getAuthorities().iterator().next().getAuthority();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .email(userDetails.getUsername())
                .role(role)
                .userId(userDetails.getId())
                .companyId(userDetails.getCompanyId())
                .build();
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String tokenHash = hashToken(request.getRefreshToken());
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (refreshToken.isRevoked() || refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            // Fail closed: revoke token if it was not already, and reject
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new IllegalArgumentException("Refresh token is expired or revoked");
        }

        // Revoke the old token (rotate on use)
        refreshTokenRepository.delete(refreshToken);

        // Find user and create new tokens
        CustomUserDetails userDetails;
        if (refreshToken.getUserType() == UserType.RECRUITER) {
            Recruiter recruiter = recruiterRepository.findById(refreshToken.getUserId())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            String roleName = "ROLE_" + recruiter.getRole().name();
            userDetails = new CustomUserDetails(
                    recruiter.getId(),
                    recruiter.getEmail(),
                    recruiter.getPasswordHash(),
                    UserType.RECRUITER,
                    recruiter.getCompany().getId(),
                    List.of(new SimpleGrantedAuthority(roleName))
            );
        } else {
            Candidate candidate = candidateRepository.findById(refreshToken.getUserId())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            userDetails = new CustomUserDetails(
                    candidate.getId(),
                    candidate.getEmail(),
                    candidate.getPasswordHash(),
                    UserType.CANDIDATE,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
            );
        }

        return createAuthResponse(userDetails);
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        String tokenHash = hashToken(request.getRefreshToken());
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(token -> refreshTokenRepository.delete(token));
    }

    @Transactional
    public String requestPasswordReset(PasswordResetRequest request) {
        UserType type = UserType.valueOf(request.getUserType().toUpperCase().trim());
        boolean userExists = false;
        
        if (type == UserType.RECRUITER) {
            userExists = recruiterRepository.findByEmail(request.getEmail()).isPresent();
        } else {
            userExists = candidateRepository.findByEmail(request.getEmail()).isPresent();
        }

        if (!userExists) {
            throw new UsernameNotFoundException("User not found with email: " + request.getEmail());
        }

        // Invalidate outstanding tokens
        List<PasswordResetToken> activeTokens = passwordResetTokenRepository
                .findAllByEmailAndUserTypeAndUsedFalse(request.getEmail(), type);
        
        for (PasswordResetToken token : activeTokens) {
            token.setUsed(true);
        }
        passwordResetTokenRepository.saveAll(activeTokens);

        // Generate new token (expires in 30 minutes)
        String resetToken = UUID.randomUUID().toString();
        PasswordResetToken newToken = PasswordResetToken.builder()
                .email(request.getEmail())
                .userType(type)
                .token(resetToken)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .build();
        
        passwordResetTokenRepository.save(newToken);

        notificationService.sendResetEmail(request.getEmail(), resetToken);

        return resetToken;
    }

    @Transactional
    public void completePasswordReset(PasswordResetCompleteRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid password reset token"));

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Password reset token has expired or already been used");
        }

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        if (resetToken.getUserType() == UserType.RECRUITER) {
            Recruiter recruiter = recruiterRepository.findByEmail(resetToken.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            recruiter.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            recruiterRepository.save(recruiter);
        } else {
            Candidate candidate = candidateRepository.findByEmail(resetToken.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            candidate.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            candidateRepository.save(candidate);
        }
    }

    @Transactional
    public AuthResponse loginOrRegisterCandidateWithGoogle(String idTokenString) {
        GoogleIdToken.Payload payload = googleAuthService.verifyToken(idTokenString);
        String email = payload.getEmail();
        String tempName = (String) payload.get("name");
        final String name = (tempName == null || tempName.isBlank()) ? "Google User" : tempName;

        Candidate candidate = candidateRepository.findByEmail(email)
                .orElseGet(() -> candidateRepository.save(
                        Candidate.builder()
                                .email(email)
                                .fullName(name)
                                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString())) // Random password
                                .build()
                ));

        CustomUserDetails userDetails = new CustomUserDetails(
                candidate.getId(),
                candidate.getEmail(),
                candidate.getPasswordHash(),
                UserType.CANDIDATE,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
        );

        return createAuthResponse(userDetails);
    }
}
