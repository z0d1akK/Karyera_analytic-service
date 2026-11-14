package com.zodiakk.analyticservice.integration.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CareerPathInterpretationRequest(
        UUID correlationId,
        UUID employeeId,
        String language,
        String currentPositionName,
        String currentGradeName,
        List<Direction> directions
) {
    public record Direction(
            UUID targetPositionId,
            String targetPositionName,
            String targetGradeName,
            BigDecimal totalScore,
            Boolean mandatoryRequirementsMet,
            List<CareerExplanationRequest.SkillStrength> strengths,
            List<CareerExplanationRequest.SkillGap> skillGaps
    ) {
    }
}
