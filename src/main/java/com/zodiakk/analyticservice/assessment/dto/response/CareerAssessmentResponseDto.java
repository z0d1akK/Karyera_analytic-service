package com.zodiakk.analyticservice.assessment.dto.response;

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
public class CareerAssessmentResponseDto {

    @Schema(description = "Id")
    private UUID id;

    @Schema(description = "Created at")
    private OffsetDateTime createdAt;

    @Schema(description = "Job id")
    private UUID jobId;

    @Schema(description = "Employee id")
    private UUID employeeId;

    @Schema(description = "Target position id")
    private UUID targetPositionId;

    @Schema(description = "Target grade id")
    private UUID targetGradeId;

    @Schema(description = "Requirement id")
    private UUID requirementId;

    @Schema(description = "Requirement revision")
    private Integer requirementRevision;

    @Schema(description = "Methodology version")
    private String methodologyVersion;

    @Schema(description = "Skill score")
    private BigDecimal skillScore;

    @Schema(description = "Experience score")
    private BigDecimal experienceScore;

    @Schema(description = "Total score")
    private BigDecimal totalScore;

    @Schema(description = "Mandatory requirements met")
    private Boolean mandatoryRequirementsMet;

    @Schema(description = "Data quality status")
    private DataQualityStatus dataQualityStatus;

    @Schema(description = "Input snapshot")
    private String inputSnapshot;

    @Schema(description = "Input hash")
    private String inputHash;

    @Schema(description = "Is stale")
    private Boolean isStale;

    @Schema(description = "Calculated at")
    private OffsetDateTime calculatedAt;

    @Schema(description = "Calculated skill gaps")
    private List<AssessmentSkillGapResponseDto> skillGaps;

    @Schema(description = "Assessment score components")
    private List<AssessmentComponentResponseDto> components;

    @Schema(description = "AI recommendations for the assessment")
    private List<com.zodiakk.analyticservice.recommendation.dto.response.AiRecommendationResponseDto> recommendations;
}
