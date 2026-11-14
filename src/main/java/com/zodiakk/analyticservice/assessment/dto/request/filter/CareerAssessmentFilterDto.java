package com.zodiakk.analyticservice.assessment.dto.request.filter;

import com.zodiakk.analyticservice.common.enums.*;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerAssessmentFilterDto {

    @Schema(description = "Employee id")
    private UUID employeeId;

    @Schema(description = "Target position id")
    private UUID targetPositionId;

    @Schema(description = "Target grade id")
    private UUID targetGradeId;

    @Schema(description = "Requirement id")
    private UUID requirementId;

    @Schema(description = "Data quality status")
    private DataQualityStatus dataQualityStatus;

    @Schema(description = "Is stale")
    private Boolean isStale;

    @Schema(description = "Calculation time from (inclusive)")
    private OffsetDateTime calculatedAtFrom;

    @Schema(description = "Calculation time to (inclusive)")
    private OffsetDateTime calculatedAtTo;
}
