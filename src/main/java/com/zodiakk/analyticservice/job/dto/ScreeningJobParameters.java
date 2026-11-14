package com.zodiakk.analyticservice.job.dto;

import java.util.List;
import java.util.UUID;

public record ScreeningJobParameters(
        UUID reserveId,
        UUID targetPositionId,
        UUID targetGradeId,
        UUID requirementId,
        List<UUID> candidateIds,
        UUID initiatorEmployeeId,
        String initiatorRole,
        String selectionType
) {
}
