package com.zodiakk.analyticservice.dashboard.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DashboardOverviewDto(
        long assessedEmployees,
        BigDecimal averageScore,
        long employeesWithCompleteData,
        long freshAssessments,
        BigDecimal topReadinessShare,
        long topReadyCount,
        UUID topDeficitSkillId,
        String topDeficitSkillName,
        long topDeficitEmployees,
        long mandatoryGaps,
        BigDecimal averageSkillScore
) {
}
