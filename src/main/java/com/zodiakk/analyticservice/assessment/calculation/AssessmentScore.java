package com.zodiakk.analyticservice.assessment.calculation;

import com.zodiakk.analyticservice.common.enums.DataQualityStatus;

import java.math.BigDecimal;
import java.util.List;

public record AssessmentScore(
        BigDecimal totalScore,
        DataQualityStatus dataQualityStatus,
        List<ScoreComponent> components
) {
}
