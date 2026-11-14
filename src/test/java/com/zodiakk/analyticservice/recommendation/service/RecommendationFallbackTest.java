package com.zodiakk.analyticservice.recommendation.service;

import com.zodiakk.analyticservice.integration.dto.CareerExplanationRequest;
import com.zodiakk.analyticservice.integration.dto.DevelopmentRecommendationRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationFallbackTest {

    private final RecommendationFallback fallback = new RecommendationFallback();

    @Test
    void explainsPartialDataAndNullScores() {
        UUID skillId = UUID.randomUUID();
        var response = fallback.explain(new CareerExplanationRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "ru",
                null,
                null,
                false,
                "PARTIAL",
                List.of(new CareerExplanationRequest.SkillStrength(skillId, "Java", 4, 4)),
                List.of(new CareerExplanationRequest.SkillGap(skillId, null, 1, 3, BigDecimal.ONE, true))
        ));

        assertTrue(response.summary().contains("0%"));
        assertEquals(1, response.strengths().size());
        assertEquals(skillId, response.developmentAreas().getFirst().skillId());
        assertEquals(RecommendationFallback.MODEL_IDENTIFIER, response.modelIdentifier());
        assertEquals("FALLBACK", response.status());
    }

    @Test
    void recommendsUntilLimitAndHandlesEmptyInput() {
        assertTrue(fallback.recommend(null).recommendations().isEmpty());
        assertTrue(fallback.recommend(new DevelopmentRecommendationRequest(
                UUID.randomUUID(), UUID.randomUUID(), "ru", "Lead", List.of(), null
        )).recommendations().isEmpty());

        UUID first = UUID.randomUUID();
        var response = fallback.recommend(new DevelopmentRecommendationRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "ru",
                "Lead",
                List.of(
                        new DevelopmentRecommendationRequest.Area(first, "Kafka", 1, 3, BigDecimal.ONE),
                        new DevelopmentRecommendationRequest.Area(UUID.randomUUID(), "Spring", 2, 4, BigDecimal.ONE)
                ),
                new DevelopmentRecommendationRequest.Preferences(1, false, true)
        ));

        assertEquals(1, response.recommendations().size());
        assertEquals(first, response.recommendations().getFirst().skillId());
        assertEquals("SELF_STUDY", response.recommendations().getFirst().type());
    }
}
