package com.zodiakk.analyticservice.integration.dto;

import java.util.List;
import java.util.UUID;

public record AvailableTargetsDto(
        UUID trackId,
        UUID employeeId,
        UUID currentNodeId,
        List<Target> targets
) {

    public record Target(
            UUID nodeId,
            UUID positionId,
            UUID gradeId,
            String label,
            String transitionType,
            Integer estimatedMonths
    ) {
    }
}
