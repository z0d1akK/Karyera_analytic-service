package com.zodiakk.analyticservice.assessment.calculation;

import com.zodiakk.analyticservice.common.enums.DataQualityStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class AssessmentCalculator {

    public static final String SKILLS = "SKILLS";

    public static final String EXPERIENCE = "EXPERIENCE";

    public static final String PROJECT = "PROJECT";

    public AssessmentScore calculate(
            BigDecimal skillScore,
            BigDecimal experienceScore,
            BigDecimal projectScore,
            BigDecimal skillWeight,
            BigDecimal experienceWeight,
            BigDecimal projectWeight
    ) {
        if (skillScore == null) {
            throw new InvalidAssessmentInputException("Оценка навыков обязательна");
        }

        requireWeight(skillWeight, "навыков");
        requireWeight(experienceWeight, "опыта");
        requireWeight(projectWeight, "проектной активности");

        BigDecimal appliedSkillWeight = skillWeight;

        if (experienceScore == null) {
            appliedSkillWeight = appliedSkillWeight.add(experienceWeight);
        }

        if (projectScore == null) {
            appliedSkillWeight = appliedSkillWeight.add(projectWeight);
        }

        List<ScoreComponent> components = new ArrayList<>();
        components.add(component(SKILLS, skillScore, appliedSkillWeight));

        if (experienceScore != null) {
            components.add(component(EXPERIENCE, experienceScore, experienceWeight));
        }

        if (projectScore != null) {
            components.add(component(PROJECT, projectScore, projectWeight));
        }

        BigDecimal total = BigDecimal.ZERO;

        for (ScoreComponent component : components) {
            total = total.add(component.rawScore().multiply(component.coefficient()));
        }

        DataQualityStatus quality = experienceScore == null || projectScore == null
                ? DataQualityStatus.PARTIAL
                : DataQualityStatus.COMPLETE;

        return new AssessmentScore(DecimalScale.scale(total), quality, List.copyOf(components));
    }

    private ScoreComponent component(String code, BigDecimal rawScore, BigDecimal coefficient) {
        BigDecimal scaledRaw = DecimalScale.scale(rawScore);
        BigDecimal scaledCoefficient = DecimalScale.scale(coefficient);

        return new ScoreComponent(
                code,
                scaledRaw,
                scaledCoefficient,
                DecimalScale.scale(scaledRaw.multiply(scaledCoefficient))
        );
    }

    private void requireWeight(BigDecimal weight, String name) {
        if (weight == null || weight.signum() < 0) {
            throw new InvalidAssessmentInputException("Коэффициент " + name + " не задан");
        }
    }
}
