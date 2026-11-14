package com.zodiakk.analyticservice.integration.dto;

import java.util.List;
import java.util.UUID;

public record DevelopmentRecommendationResponse(
        UUID generationId,
        List<Item> recommendations
) {
    public record Item(
            UUID skillId,
            String type,
            String title,
            String description,
            List<String> suggestedActivities,
            String expectedOutcome
    ) {
    }
}
