package com.zodiakk.analyticservice.integration.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CareerProfileDto(
        UUID employeeId,
        UUID positionId,
        String positionName,
        UUID gradeId,
        String gradeName,
        boolean active,
        long dataVersion,
        List<SkillLevel> skills,
        List<ExperiencePeriod> experiencePeriods,
        String fullName
) {

    public record SkillLevel(UUID skillId, String skillName, int level) {
    }

    public record ExperiencePeriod(LocalDate startDate, LocalDate endDate) {
    }
}
