package com.zodiakk.analyticservice.screening.dto.request.create;

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
public class ScreeningResultCreateRequestDto {

    @Schema(description = "Screening id")
    @NotNull(message = ValidationMessages.SCREENING_ID_REQUIRED)
    private UUID screeningId;

    @Schema(description = "Employee id")
    @NotNull(message = ValidationMessages.EMPLOYEE_ID_REQUIRED)
    private UUID employeeId;

    @Schema(description = "Assessment id")
    private UUID assessmentId;

    @Schema(description = "Rank")
    private Integer rank;

    @Schema(description = "Score")
    private BigDecimal score;

    @Schema(description = "Eligibility status")
    @NotNull(message = ValidationMessages.ELIGIBILITY_STATUS_REQUIRED)
    private ScreeningEligibilityStatus eligibilityStatus;

    @Schema(description = "Mandatory requirements met")
    private Boolean mandatoryRequirementsMet;

    @Schema(description = "Exclusion reason")
    @Size(max = 100, message = ValidationMessages.EXCLUSION_REASON_SIZE)
    private String exclusionReason;
}
