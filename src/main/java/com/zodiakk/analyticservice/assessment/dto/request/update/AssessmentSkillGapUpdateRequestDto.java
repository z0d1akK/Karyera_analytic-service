package com.zodiakk.analyticservice.assessment.dto.request.update;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentSkillGapUpdateRequestDto {

    @Schema(description = "Actual level")
    private Integer actualLevel;

    @Schema(description = "Required level")
    private Integer requiredLevel;

    @Schema(description = "Weight")
    private BigDecimal weight;

    @Schema(description = "Absolute gap")
    private Integer absoluteGap;

    @Schema(description = "Normalized gap")
    private BigDecimal normalizedGap;

    @Schema(description = "Priority score")
    private BigDecimal priorityScore;

    @Schema(description = "Mandatory")
    private Boolean mandatory;

    @Schema(description = "Requirement met")
    private Boolean requirementMet;
}
