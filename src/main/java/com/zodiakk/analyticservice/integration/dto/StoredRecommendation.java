package com.zodiakk.analyticservice.integration.dto;

import com.zodiakk.analyticservice.integration.dto.CareerExplanationResponse;
import com.zodiakk.analyticservice.integration.dto.DevelopmentRecommendationResponse;

public record StoredRecommendation(
        CareerExplanationResponse explanation,
        DevelopmentRecommendationResponse development
) {
}
