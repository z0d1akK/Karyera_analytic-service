package com.zodiakk.analyticservice.assessment.calculation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SkillFitCalculator {

    public BigDecimal calculate(
            List<SkillRequirementData> requirements,
            Map<UUID, Integer> actualSkillLevels
    ) {
        if (requirements == null || requirements.isEmpty()) {
            throw new InvalidAssessmentInputException("Требование не содержит навыков");
        }

        BigDecimal weighted = BigDecimal.ZERO;
        BigDecimal weightSum = BigDecimal.ZERO;

        for (SkillRequirementData requirement : requirements) {
            if (requirement.requiredLevel() <= 0) {
                throw new InvalidAssessmentInputException("Требуемый уровень навыка должен быть больше нуля");
            }

            if (requirement.weight() == null || requirement.weight().signum() <= 0) {
                throw new InvalidAssessmentInputException("Вес навыка должен быть положительным");
            }

            int actual = actualLevel(actualSkillLevels, requirement.skillId());
            BigDecimal ratio = DecimalScale.divide(
                    BigDecimal.valueOf(actual),
                    BigDecimal.valueOf(requirement.requiredLevel())
            );

            if (ratio.compareTo(BigDecimal.ONE) > 0) {
                ratio = BigDecimal.ONE;
            }

            weighted = weighted.add(requirement.weight().multiply(ratio));
            weightSum = weightSum.add(requirement.weight());
        }

        if (weightSum.signum() <= 0) {
            throw new InvalidAssessmentInputException("Сумма весов навыков должна быть положительной");
        }

        return DecimalScale.scale(DecimalScale.divide(weighted, weightSum));
    }

    static int actualLevel(Map<UUID, Integer> actualSkillLevels, UUID skillId) {
        if (actualSkillLevels == null || skillId == null) {
            return 0;
        }

        Integer level = actualSkillLevels.get(skillId);

        if (level == null || level < 0) {
            return 0;
        }

        return level;
    }
}
