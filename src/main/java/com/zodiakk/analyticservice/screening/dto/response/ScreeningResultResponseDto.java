package com.zodiakk.analyticservice.screening.dto.response;

import com.zodiakk.analyticservice.common.enums.*;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScreeningResultResponseDto {

    @Schema(description = "Id")
    private UUID id;

    @Schema(description = "Created at")
    private OffsetDateTime createdAt;

    @Schema(description = "Screening id")
    private UUID screeningId;

    @Schema(description = "Employee id")
    private UUID employeeId;

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
    private String exclusionReason;
}
