package com.zodiakk.analyticservice.assessment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerOptionResponseDto {

    @Schema(description = "Target position identifier")
    private UUID targetPositionId;

    @Schema(description = "Target grade identifier")
    private UUID targetGradeId;

    @Schema(description = "Assessment identifier")
    private UUID assessmentId;

    @Schema(description = "Normalized skill score")
    private BigDecimal skillScore;

    @Schema(description = "Whether all mandatory requirements are met")
    private Boolean mandatoryRequirementsMet;

    @Schema(description = "Number of missing skills")
    private Integer missingSkillsCount;
}
