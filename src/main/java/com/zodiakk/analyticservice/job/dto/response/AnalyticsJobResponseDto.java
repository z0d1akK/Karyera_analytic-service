package com.zodiakk.analyticservice.job.dto.response;

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
public class AnalyticsJobResponseDto {

    @Schema(description = "Id")
    private UUID id;

    @Schema(description = "Version")
    private Long version;

    @Schema(description = "Created at")
    private OffsetDateTime createdAt;

    @Schema(description = "Job type")
    private AnalyticsJobType jobType;

    @Schema(description = "Status")
    private AnalyticsJobStatus status;

    @Schema(description = "Requested by")
    private UUID requestedBy;

    @Schema(description = "Parameters")
    private String parameters;

    @Schema(description = "Result")
    private String result;

    @Schema(description = "Idempotency key")
    private String idempotencyKey;

    @Schema(description = "Attempts")
    private Integer attempts;

    @Schema(description = "Max attempts")
    private Integer maxAttempts;

    @Schema(description = "Result id")
    private UUID resultId;

    @Schema(description = "Error code")
    private String errorCode;

    @Schema(description = "Error message")
    private String errorMessage;

    @Schema(description = "Started at")
    private OffsetDateTime startedAt;

    @Schema(description = "Completed at")
    private OffsetDateTime completedAt;

    @Schema(description = "Next attempt at")
    private OffsetDateTime nextAttemptAt;

    @Schema(description = "Locked until")
    private OffsetDateTime lockedUntil;
}
