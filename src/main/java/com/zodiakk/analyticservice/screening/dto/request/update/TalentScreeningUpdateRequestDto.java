package com.zodiakk.analyticservice.screening.dto.request.update;

import com.zodiakk.analyticservice.common.enums.*;
import com.zodiakk.analyticservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TalentScreeningUpdateRequestDto {

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

    @Schema(description = "Status")
    private ScreeningStatus status;

    @Schema(description = "Candidate count")
    private Integer candidateCount;

    @Schema(description = "Evaluated count")
    private Integer evaluatedCount;

    @Schema(description = "Failed count")
    private Integer failedCount;

    @Schema(description = "Completed at")
    private OffsetDateTime completedAt;
}
