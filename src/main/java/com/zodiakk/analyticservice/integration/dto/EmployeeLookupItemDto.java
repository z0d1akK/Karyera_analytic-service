package com.zodiakk.analyticservice.integration.dto;

import java.util.UUID;

public record EmployeeLookupItemDto(
        UUID id,
        String fullName,
        boolean active,
        UUID positionId,
        UUID gradeId,
        UUID departmentId
) {
}
