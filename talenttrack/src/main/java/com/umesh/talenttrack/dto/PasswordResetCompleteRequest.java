package com.umesh.talenttrack.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetCompleteRequest {

    @NotBlank
    private String token;

    @NotBlank
    private String newPassword;
}
