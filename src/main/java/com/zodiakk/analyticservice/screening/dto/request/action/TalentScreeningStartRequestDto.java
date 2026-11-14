package com.zodiakk.analyticservice.screening.dto.request.action;

import com.zodiakk.analyticservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TalentScreeningStartRequestDto {

    @Schema(description = "Talent reserve identifier")
    @NotNull(message = ValidationMessages.RESERVE_ID_REQUIRED)
    private UUID reserveId;

    @Schema(description = "Target position identifier")
    @NotNull(message = ValidationMessages.TARGET_POSITION_ID_REQUIRED)
    private UUID targetPositionId;

    @Schema(description = "Target grade identifier")
    @NotNull(message = ValidationMessages.TARGET_GRADE_ID_REQUIRED)
    private UUID targetGradeId;

    @Schema(description = "Candidate selection rules")
    @NotNull(message = ValidationMessages.CANDIDATE_SELECTION_REQUIRED)
    @Valid
    private CandidateSelectionDto candidateSelection;
}
