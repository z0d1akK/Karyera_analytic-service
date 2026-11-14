package com.zodiakk.analyticservice.assessment.dto.request.update;

import com.zodiakk.analyticservice.common.enums.*;
import com.zodiakk.analyticservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerAssessmentUpdateRequestDto {

    @Schema(description = "Job id")
    private UUID jobId;

    @Schema(description = "Target position id")
    private UUID targetPositionId;

    @Schema(description = "Target grade id")
    private UUID targetGradeId;

    @Schema(description = "Requirement id")
    private UUID requirementId;

    @Schema(description = "Requirement revision")
    private Integer requirementRevision;

    @Schema(description = "Methodology version")
    @Size(max = 30, message = ValidationMessages.METHODOLOGY_VERSION_SIZE)
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
    @Size(max = 64, message = ValidationMessages.INPUT_HASH_SIZE)
    private String inputHash;

    @Schema(description = "Is stale")
    private Boolean isStale;

    @Schema(description = "Calculated at")
    private OffsetDateTime calculatedAt;
}
