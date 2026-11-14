package com.zodiakk.analyticservice.screening.dto.request.update;

import com.zodiakk.analyticservice.common.enums.*;
import com.zodiakk.analyticservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScreeningResultUpdateRequestDto {

    @Schema(description = "Assessment id")
    private UUID assessmentId;

    @Schema(description = "Rank")
    private Integer rank;

    @Schema(description = "Score")
    private BigDecimal score;

    @Schema(description = "Eligibility status")
    private ScreeningEligibilityStatus eligibilityStatus;

    @Schema(description = "Mandatory requirements met")
    private Boolean mandatoryRequirementsMet;

    @Schema(description = "Exclusion reason")
    @Size(max = 100, message = ValidationMessages.EXCLUSION_REASON_SIZE)
    private String exclusionReason;
}
