package com.zodiakk.analyticservice.job.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record JobStatusResponseDto(
        UUID jobId,
        String type,
        String status,
        int progressPercent,
        UUID resultId,
        OffsetDateTime createdAt,
        OffsetDateTime completedAt,
        String errorCode
) {
}
