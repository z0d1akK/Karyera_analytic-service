package com.zodiakk.analyticservice.assessment.calculation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SkillGapCalculator {

    private final SkillFitCalculator skillFitCalculator = new SkillFitCalculator();

    public List<SkillGapResult> calculate(
            List<SkillRequirementData> requirements,
            Map<UUID, Integer> actualSkillLevels
    ) {
        skillFitCalculator.calculate(requirements, actualSkillLevels);

        List<SkillGapResult> gaps = new ArrayList<>();

        for (SkillRequirementData requirement : requirements) {
            int actual = SkillFitCalculator.actualLevel(actualSkillLevels, requirement.skillId());
            int absoluteGap = Math.max(requirement.requiredLevel() - actual, 0);
            BigDecimal ratio = DecimalScale.divide(
                    BigDecimal.valueOf(actual),
                    BigDecimal.valueOf(requirement.requiredLevel())
            );
            BigDecimal normalized = BigDecimal.ONE.subtract(ratio);

            if (normalized.signum() < 0) {
                normalized = BigDecimal.ZERO;
            }

            if (normalized.compareTo(BigDecimal.ONE) > 0) {
                normalized = BigDecimal.ONE;
            }

            BigDecimal normalizedScaled = DecimalScale.scale(normalized);
            BigDecimal priority = DecimalScale.scale(requirement.weight().multiply(normalizedScaled));

            gaps.add(new SkillGapResult(
                    requirement.skillId(),
                    actual,
                    requirement.requiredLevel(),
                    requirement.weight(),
                    absoluteGap,
                    normalizedScaled,
                    priority,
                    requirement.mandatory(),
                    actual >= requirement.requiredLevel()
            ));
        }

        return List.copyOf(gaps);
    }

    public boolean mandatoryRequirementsMet(List<SkillGapResult> gaps) {
        return gaps.stream()
                .filter(SkillGapResult::mandatory)
                .allMatch(SkillGapResult::requirementMet);
    }
}
