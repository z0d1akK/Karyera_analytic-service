package com.zodiakk.analyticservice.job.dto;

import java.util.UUID;

public record AssessmentJobParameters(
        UUID employeeId,
        UUID targetPositionId,
        UUID targetGradeId,
        UUID requirementId,
        boolean generateRecommendations,
        String mode
) {
    public AssessmentJobParameters(
            UUID employeeId,
            UUID targetPositionId,
            UUID targetGradeId,
            UUID requirementId,
            boolean generateRecommendations
    ) {
        this(employeeId, targetPositionId, targetGradeId, requirementId, generateRecommendations, "FULL");
    }
}
