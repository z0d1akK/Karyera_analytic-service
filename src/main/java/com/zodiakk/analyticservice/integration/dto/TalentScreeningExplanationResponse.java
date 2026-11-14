package com.zodiakk.analyticservice.integration.dto;

import java.util.List;
import java.util.UUID;

public record TalentScreeningExplanationResponse(
        UUID generationId,
        String summary,
        List<String> highlights,
        List<String> limitations,
        String modelIdentifier,
        String status
) {
}
