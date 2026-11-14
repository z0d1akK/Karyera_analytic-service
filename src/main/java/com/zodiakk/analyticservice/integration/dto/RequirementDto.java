package com.zodiakk.analyticservice.integration.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RequirementDto(
        UUID requirementId,
        UUID positionId,
        UUID gradeId,
        Integer revision,
        String status,
        Integer minExperienceMonths,
        String description,
        List<Skill> skills
) {

    public record Skill(
            UUID skillId,
            Integer requiredLevel,
            BigDecimal weight,
            Boolean mandatory,
            String description
    ) {
    }
}
