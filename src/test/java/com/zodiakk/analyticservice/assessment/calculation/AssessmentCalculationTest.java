package com.zodiakk.analyticservice.assessment.calculation;

import com.zodiakk.analyticservice.common.enums.DataQualityStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssessmentCalculationTest {

    private static final UUID JAVA = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID SPRING = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID POSTGRES = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID KAFKA = UUID.fromString("44444444-4444-4444-8444-444444444444");

    private final SkillFitCalculator skillFitCalculator = new SkillFitCalculator();
    private final SkillGapCalculator skillGapCalculator = new SkillGapCalculator();
    private final ExperienceCalculator experienceCalculator = new ExperienceCalculator();
    private final ProjectActivityCalculator projectActivityCalculator = new ProjectActivityCalculator();
    private final AssessmentCalculator assessmentCalculator = new AssessmentCalculator();

    @Test
    void skillFitMatchesDocumentedExample() {
        BigDecimal score = skillFitCalculator.calculate(exampleRequirements(), exampleLevels());

        assertEquals(new BigDecimal("0.825000"), score);
    }

    @Test
    void missingSkillIsTreatedAsZeroAndDoesNotIncreaseScoreAboveOne() {
        List<SkillRequirementData> requirements = List.of(
                new SkillRequirementData(JAVA, 4, new BigDecimal("1.00"), true)
        );

        assertEquals(new BigDecimal("0.000000"), skillFitCalculator.calculate(requirements, Map.of()));
        assertEquals(
                new BigDecimal("1.000000"),
                skillFitCalculator.calculate(requirements, Map.of(JAVA, 9))
        );
    }

    @Test
    void skillGapUsesWeightTimesNormalizedDeficit() {
        List<SkillGapResult> gaps = skillGapCalculator.calculate(exampleRequirements(), exampleLevels());
        SkillGapResult kafka = gaps.stream().filter(gap -> gap.skillId().equals(KAFKA)).findFirst().orElseThrow();
        SkillGapResult spring = gaps.stream().filter(gap -> gap.skillId().equals(SPRING)).findFirst().orElseThrow();

        assertEquals(2, kafka.absoluteGap());
        assertEquals(new BigDecimal("0.666667"), kafka.normalizedGap());
        assertEquals(new BigDecimal("0.100000"), kafka.priorityScore());
        assertEquals(new BigDecimal("0.075000"), spring.priorityScore());
        assertTrue(kafka.priorityScore().compareTo(spring.priorityScore()) > 0);
        assertEquals(false, skillGapCalculator.mandatoryRequirementsMet(gaps));
    }

    @Test
    void overlappingExperiencePeriodsAreMerged() {
        LocalDate today = LocalDate.of(2024, 12, 31);
        List<DateInterval> periods = List.of(
                new DateInterval(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30)),
                new DateInterval(LocalDate.of(2024, 6, 1), LocalDate.of(2024, 12, 31)),
                new DateInterval(LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 15))
        );

        List<DateInterval> merged = experienceCalculator.merge(periods, today);

        assertEquals(1, merged.size());
        assertEquals(LocalDate.of(2024, 1, 1), merged.getFirst().start());
        assertEquals(LocalDate.of(2024, 12, 31), merged.getFirst().endInclusive());
        assertEquals(new BigDecimal("12.024641"), experienceCalculator.coveredMonths(periods, today));
        assertEquals(new BigDecimal("1.000000"), experienceCalculator.score(new BigDecimal("40"), 36));
        assertEquals(new BigDecimal("0.833333"), experienceCalculator.score(new BigDecimal("30"), 36));
        assertEquals(new BigDecimal("1.000000"), experienceCalculator.score(BigDecimal.ZERO, 0));
    }

    @Test
    void projectScoreUsesComplexityWeightsAndMandatorySkillDenominator() {
        Map<String, Long> tasks = Map.of("LOW", 2L, "VERY_HIGH", 1L, "UNSPECIFIED", 4L);

        assertEquals(new BigDecimal("0.700000"), projectActivityCalculator.score(tasks, 2, 4));
        assertEquals(new BigDecimal("0.350000"), projectActivityCalculator.score(tasks, 0, 4));
        assertNull(projectActivityCalculator.score(tasks, 0, 0));
    }

    @Test
    void rejectsInvalidSkillAndExperienceInput() {
        assertThrows(InvalidAssessmentInputException.class,
                () -> skillFitCalculator.calculate(List.of(), Map.of()));
        assertThrows(InvalidAssessmentInputException.class, () -> skillFitCalculator.calculate(
                List.of(new SkillRequirementData(JAVA, 0, BigDecimal.ONE, true)), Map.of()));
        assertThrows(InvalidAssessmentInputException.class, () -> skillFitCalculator.calculate(
                List.of(new SkillRequirementData(JAVA, 1, null, true)), Map.of()));
        assertThrows(InvalidAssessmentInputException.class,
                () -> experienceCalculator.coveredMonths(List.of(), null));
        assertThrows(InvalidAssessmentInputException.class,
                () -> experienceCalculator.score(BigDecimal.ONE, -1));
        assertEquals(DecimalScale.ZERO, experienceCalculator.coveredMonths(
                List.of(new DateInterval(LocalDate.of(2030, 1, 1), LocalDate.of(2020, 1, 1))),
                LocalDate.of(2024, 1, 1)
        ));
        assertEquals(List.of(), experienceCalculator.merge(null, LocalDate.now()));
    }

    @Test
    void fullFormulaKeepsEveryComponent() {
        AssessmentScore score = assessmentCalculator.calculate(
                new BigDecimal("0.825"),
                new BigDecimal("0.833333"),
                new BigDecimal("0.500000"),
                new BigDecimal("0.70"),
                new BigDecimal("0.20"),
                new BigDecimal("0.10")
        );

        assertEquals(DataQualityStatus.COMPLETE, score.dataQualityStatus());
        assertEquals(3, score.components().size());
        assertEquals(new BigDecimal("0.794167"), score.totalScore());
    }

    @Test
    void missingComponentsMoveTheirWeightToSkills() {
        AssessmentScore score = assessmentCalculator.calculate(
                new BigDecimal("0.825000"),
                null,
                new BigDecimal("0.500000"),
                new BigDecimal("0.70"),
                new BigDecimal("0.20"),
                new BigDecimal("0.10")
        );

        assertEquals(DataQualityStatus.PARTIAL, score.dataQualityStatus());
        assertEquals(new BigDecimal("0.792500"), score.totalScore());
        assertEquals(2, score.components().size());
        assertEquals(new BigDecimal("0.900000"), score.components().getFirst().coefficient());
        assertEquals("PROJECT", score.components().get(1).code());
    }

    private List<SkillRequirementData> exampleRequirements() {
        return List.of(
                new SkillRequirementData(JAVA, 4, new BigDecimal("0.35"), false),
                new SkillRequirementData(SPRING, 4, new BigDecimal("0.30"), true),
                new SkillRequirementData(POSTGRES, 3, new BigDecimal("0.20"), false),
                new SkillRequirementData(KAFKA, 3, new BigDecimal("0.15"), true)
        );
    }

    private Map<UUID, Integer> exampleLevels() {
        return Map.of(JAVA, 4, SPRING, 3, POSTGRES, 3, KAFKA, 1);
    }
}
