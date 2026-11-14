package com.zodiakk.analyticservice.integration.dto;

import java.util.List;
import java.util.UUID;

public record CareerPathInterpretationResponse(
        UUID generationId,
        String summary,
        List<Direction> directions,
        String recommendedDirection,
        String recommendationRationale,
        List<String> limitations,
        String modelIdentifier,
        Integer promptVersion
) {
    public record Direction(UUID targetPositionId, String targetPositionName, String fitSummary) {
    }
}
