package com.zodiakk.analyticservice.assessment.dto.request.create;

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
public class AssessmentComponentCreateRequestDto {

    @Schema(description = "Assessment id")
    @NotNull(message = ValidationMessages.ASSESSMENT_ID_REQUIRED)
    private UUID assessmentId;

    @Schema(description = "Component code")
    @NotBlank(message = ValidationMessages.COMPONENT_CODE_REQUIRED)
    @Size(max = 40, message = ValidationMessages.COMPONENT_CODE_SIZE)
    private String componentCode;

    @Schema(description = "Raw score")
    @NotNull(message = ValidationMessages.RAW_SCORE_REQUIRED)
    private BigDecimal rawScore;

    @Schema(description = "Coefficient")
    @NotNull(message = ValidationMessages.COEFFICIENT_REQUIRED)
    private BigDecimal coefficient;

    @Schema(description = "Weighted score")
    @NotNull(message = ValidationMessages.WEIGHTED_SCORE_REQUIRED)
    private BigDecimal weightedScore;

    @Schema(description = "Details")
    private String details;
}
