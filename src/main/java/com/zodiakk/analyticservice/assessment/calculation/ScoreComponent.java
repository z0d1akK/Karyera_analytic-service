package com.zodiakk.analyticservice.assessment.calculation;

import java.math.BigDecimal;

public record ScoreComponent(
        String code,
        BigDecimal rawScore,
        BigDecimal coefficient,
        BigDecimal weightedScore
) {
}
