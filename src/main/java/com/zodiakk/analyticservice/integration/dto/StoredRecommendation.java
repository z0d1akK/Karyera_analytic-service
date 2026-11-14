package com.zodiakk.analyticservice.integration.dto;

public record StoredRecommendation(
        CareerExplanationResponse explanation,
        DevelopmentRecommendationResponse development
) {
}
