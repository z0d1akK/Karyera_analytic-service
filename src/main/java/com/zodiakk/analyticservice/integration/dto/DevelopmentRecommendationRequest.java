package com.zodiakk.analyticservice.integration.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record DevelopmentRecommendationRequest(
        UUID correlationId,
        UUID assessmentId,
        String language,
        String targetPositionName,
        List<Area> developmentAreas,
        Preferences preferences
) {
    public record Area(
            UUID skillId,
            String skillName,
            Integer currentLevel,
            Integer targetLevel,
            BigDecimal priorityScore
    ) {
    }

    public record Preferences(Integer maxRecommendations, Boolean includeProjectActivities, Boolean includeMentoring) {
    }
}
