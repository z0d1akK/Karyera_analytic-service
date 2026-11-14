package com.zodiakk.analyticservice.assessment.calculation;

import java.math.BigDecimal;
import java.util.UUID;

public record SkillGapResult(
        UUID skillId,
        int actualLevel,
        int requiredLevel,
        BigDecimal weight,
        int absoluteGap,
        BigDecimal normalizedGap,
        BigDecimal priorityScore,
        boolean mandatory,
        boolean requirementMet
) {
}
