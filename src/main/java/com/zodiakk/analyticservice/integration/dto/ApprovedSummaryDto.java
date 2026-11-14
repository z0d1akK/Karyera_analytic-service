package com.zodiakk.analyticservice.integration.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ApprovedSummaryDto(
        UUID employeeId,
        LocalDate periodStart,
        LocalDate periodEnd,
        long totalApprovedMinutes,
        List<ProjectMinutes> projects
) {

    public record ProjectMinutes(UUID projectId, long approvedMinutes) {
    }
}
