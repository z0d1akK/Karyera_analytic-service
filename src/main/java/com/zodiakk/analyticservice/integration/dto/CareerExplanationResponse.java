package com.zodiakk.analyticservice.integration.dto;

import java.util.List;
import java.util.UUID;

public record CareerExplanationResponse(
        UUID generationId,
        UUID assessmentId,
        String summary,
        List<String> strengths,
        List<Area> developmentAreas,
        List<String> limitations,
        String modelIdentifier,
        Integer promptVersion,
        String status
) {
    public record Area(UUID skillId, String explanation) {
    }
}
