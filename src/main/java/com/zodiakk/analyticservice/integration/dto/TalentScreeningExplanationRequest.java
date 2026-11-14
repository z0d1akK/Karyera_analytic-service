package com.zodiakk.analyticservice.integration.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record TalentScreeningExplanationRequest(
        UUID correlationId,
        UUID reserveId,
        String locale,
        String targetPositionName,
        String targetGradeName,
        List<Candidate> candidates
) {
    public record Candidate(
            UUID employeeId,
            String employeeName,
            Integer rank,
            BigDecimal score,
            String eligibilityStatus,
            Boolean mandatoryRequirementsMet
    ) {
    }
}
