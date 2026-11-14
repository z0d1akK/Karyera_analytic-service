package com.zodiakk.analyticservice.screening.dto.internal;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record InternalScreeningResponse(
        UUID id,
        OffsetDateTime createdAt,
        UUID jobId,
        UUID reserveId,
        UUID targetPositionId,
        UUID targetGradeId,
        UUID requirementId,
        Integer requirementRevision,
        String methodologyVersion,
        String status,
        Integer candidateCount,
        Integer evaluatedCount,
        Integer failedCount,
        OffsetDateTime completedAt,
        List<Result> results
) {

    public record Result(
            UUID id,
            OffsetDateTime createdAt,
            UUID screeningId,
            UUID employeeId,
            UUID assessmentId,
            Integer rank,
            BigDecimal score,
            String eligibilityStatus,
            Boolean mandatoryRequirementsMet,
            String exclusionReason
    ) {
    }
}
