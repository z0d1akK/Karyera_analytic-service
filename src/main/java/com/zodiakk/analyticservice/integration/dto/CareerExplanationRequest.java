package com.zodiakk.analyticservice.integration.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CareerExplanationRequest(
        UUID correlationId,
        UUID assessmentId,
        String language,
        Target target,
        Scores scores,
        Boolean mandatoryRequirementsMet,
        String dataQualityStatus,
        List<SkillStrength> strengths,
        List<SkillGap> skillGaps
) {
    public record Target(String positionName, String gradeName) {
    }

    public record Scores(BigDecimal skill, BigDecimal experience, BigDecimal total) {
    }

    public record SkillStrength(UUID skillId, String skillName, Integer actualLevel, Integer requiredLevel) {
    }

    public record SkillGap(
            UUID skillId,
            String skillName,
            Integer actualLevel,
            Integer requiredLevel,
            BigDecimal priorityScore,
            Boolean mandatory
    ) {
    }
}
