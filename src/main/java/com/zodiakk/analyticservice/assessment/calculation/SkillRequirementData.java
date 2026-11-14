package com.zodiakk.analyticservice.assessment.calculation;

import java.math.BigDecimal;
import java.util.UUID;

public record SkillRequirementData(
        UUID skillId,
        int requiredLevel,
        BigDecimal weight,
        boolean mandatory
) {
}
