package com.zodiakk.analyticservice.screening.dto.response;

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
public class TalentScreeningResponseDto {

    @Schema(description = "Id")
    private UUID id;

    @Schema(description = "Created at")
    private OffsetDateTime createdAt;

    @Schema(description = "Job id")
    private UUID jobId;

    @Schema(description = "Reserve id")
    private UUID reserveId;

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

    @Schema(description = "Ranked screening results")
    private List<ScreeningResultResponseDto> results;

    @Schema(description = "AI explanation of the ranking")
    private String explanationText;
}
