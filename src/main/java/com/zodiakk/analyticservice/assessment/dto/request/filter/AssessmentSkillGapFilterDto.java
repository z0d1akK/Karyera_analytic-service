package com.zodiakk.analyticservice.assessment.dto.request.filter;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentSkillGapFilterDto {

    @Schema(description = "Assessment id")
    private UUID assessmentId;

    @Schema(description = "Skill id")
    private UUID skillId;

    @Schema(description = "Mandatory")
    private Boolean mandatory;

    @Schema(description = "Requirement met")
    private Boolean requirementMet;
}
