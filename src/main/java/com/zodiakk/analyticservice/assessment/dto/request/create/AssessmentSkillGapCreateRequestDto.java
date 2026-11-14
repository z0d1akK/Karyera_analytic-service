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
public class AssessmentSkillGapCreateRequestDto {

    @Schema(description = "Assessment id")
    @NotNull(message = ValidationMessages.ASSESSMENT_ID_REQUIRED)
    private UUID assessmentId;

    @Schema(description = "Skill id")
    @NotNull(message = ValidationMessages.SKILL_ID_REQUIRED)
    private UUID skillId;

    @Schema(description = "Actual level")
    @NotNull(message = ValidationMessages.ACTUAL_LEVEL_REQUIRED)
    private Integer actualLevel;

    @Schema(description = "Required level")
    @NotNull(message = ValidationMessages.REQUIRED_LEVEL_REQUIRED)
    private Integer requiredLevel;

    @Schema(description = "Weight")
    @NotNull(message = ValidationMessages.WEIGHT_REQUIRED)
    private BigDecimal weight;

    @Schema(description = "Absolute gap")
    @NotNull(message = ValidationMessages.ABSOLUTE_GAP_REQUIRED)
    private Integer absoluteGap;

    @Schema(description = "Normalized gap")
    @NotNull(message = ValidationMessages.NORMALIZED_GAP_REQUIRED)
    private BigDecimal normalizedGap;

    @Schema(description = "Priority score")
    @NotNull(message = ValidationMessages.PRIORITY_SCORE_REQUIRED)
    private BigDecimal priorityScore;

    @Schema(description = "Mandatory")
    @NotNull(message = ValidationMessages.MANDATORY_REQUIRED)
    private Boolean mandatory;

    @Schema(description = "Requirement met")
    @NotNull(message = ValidationMessages.REQUIREMENT_MET_REQUIRED)
    private Boolean requirementMet;
}
