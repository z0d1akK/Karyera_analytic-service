package com.zodiakk.analyticservice.integration.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ReserveContextDto(
        UUID reserveId,
        String name,
        String status,
        UUID targetPositionId,
        UUID targetGradeId,
        LocalDate effectiveFrom,
        LocalDate effectiveTo
) {
}
