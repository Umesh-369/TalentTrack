package com.umesh.talenttrack.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String userType; // "RECRUITER" or "CANDIDATE"
}
