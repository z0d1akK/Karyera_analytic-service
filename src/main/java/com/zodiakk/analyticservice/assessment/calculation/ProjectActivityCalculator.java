package com.zodiakk.analyticservice.assessment.calculation;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

public final class ProjectActivityCalculator {

    public BigDecimal score(
            Map<String, Long> completedByComplexity,
            int mandatorySkillCount,
            int totalSkillCount
    ) {
        int denominatorSkills = mandatorySkillCount > 0 ? mandatorySkillCount : totalSkillCount;

        if (denominatorSkills <= 0) {
            return null;
        }

        long weighted = 0;

        if (completedByComplexity != null) {
            for (Map.Entry<String, Long> entry : completedByComplexity.entrySet()) {
                long count = entry.getValue() == null ? 0 : Math.max(entry.getValue(), 0);
                weighted += count * weight(entry.getKey());
            }
        }

        BigDecimal denominator = BigDecimal.valueOf(5L * denominatorSkills);
        BigDecimal ratio = DecimalScale.divide(BigDecimal.valueOf(weighted), denominator);

        if (ratio.compareTo(BigDecimal.ONE) > 0) {
            ratio = BigDecimal.ONE;
        }

        return DecimalScale.scale(ratio);
    }

    private int weight(String complexity) {
        if (complexity == null) {
            return 0;
        }

        return switch (complexity.trim().toUpperCase(Locale.ROOT)) {
            case "LOW" -> 1;
            case "MEDIUM" -> 2;
            case "HIGH" -> 3;
            case "VERY_HIGH" -> 5;
            default -> 0;
        };
    }
}
