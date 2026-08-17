package com.umesh.talenttrack.dto;

import com.umesh.talenttrack.domain.InterviewMode;
import com.umesh.talenttrack.domain.InterviewOutcome;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewResponse {
    private Long id;
    private Long applicationId;
    private String candidateName;
    private String jobPostingTitle;
    private Long scheduledById;
    private String scheduledByName;
    private LocalDateTime scheduledAt;
    private InterviewMode mode;
    private String notes;
    private InterviewOutcome outcome;
}
