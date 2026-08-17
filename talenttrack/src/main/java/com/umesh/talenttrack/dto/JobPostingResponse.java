package com.umesh.talenttrack.dto;

import com.umesh.talenttrack.domain.JobStatus;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobPostingResponse {
    private Long id;
    private Long companyId;
    private String companyName;
    private String title;
    private String description;
    private String location;
    private Integer experienceMin;
    private Integer experienceMax;
    private Boolean remote;
    private JobStatus status;
    private Long postedById;
    private String postedByEmail;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
