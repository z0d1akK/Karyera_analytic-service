package com.zodiakk.analyticservice.dashboard.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record DevelopmentDynamicsDto(
        UUID employeeId,
        List<Point> points
) {

    public record Point(
            UUID assessmentId,
            OffsetDateTime calculatedAt,
            BigDecimal totalScore,
            BigDecimal skillScore,
            boolean mandatoryRequirementsMet,
            boolean stale
    ) {
    }
}
