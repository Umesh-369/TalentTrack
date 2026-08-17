package com.umesh.talenttrack.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobPostingRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    private String location;

    private Integer experienceMin;

    private Integer experienceMax;

    private Boolean remote;

    private LocalDateTime expiresAt;
}
