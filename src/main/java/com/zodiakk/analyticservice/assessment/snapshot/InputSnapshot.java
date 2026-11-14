package com.zodiakk.analyticservice.assessment.snapshot;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record InputSnapshot(
        String methodologyVersion,
        BigDecimal skillWeight,
        BigDecimal experienceWeight,
        BigDecimal projectWeight,
        EmployeeSnapshot employee,
        RequirementSnapshot requirement,
        List<SkillSnapshot> skills,
        ExperienceSnapshot experience,
        ProjectSnapshot project
) {

    public record EmployeeSnapshot(
            UUID employeeId,
            UUID positionId,
            UUID gradeId,
            boolean active,
            long dataVersion
    ) {
    }

    public record RequirementSnapshot(
            UUID requirementId,
            int revision,
            int minExperienceMonths
    ) {
    }

    public record SkillSnapshot(
            UUID skillId,
            int actualLevel,
            int requiredLevel,
            BigDecimal weight,
            boolean mandatory
    ) {
    }

    public record ExperienceSnapshot(
            BigDecimal coveredMonths,
            boolean available
    ) {
    }

    public record ProjectSnapshot(
            boolean available,
            Map<String, Long> completedByComplexity
    ) {
    }
}
