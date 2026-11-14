package com.zodiakk.analyticservice.integration.dto;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record TaskStatisticsDto(
        UUID employeeId,
        LocalDate periodStart,
        LocalDate periodEnd,
        long completedTasks,
        Map<String, Long> completedByType,
        Map<String, Long> completedByComplexity,
        long projectsParticipated
) {
}
