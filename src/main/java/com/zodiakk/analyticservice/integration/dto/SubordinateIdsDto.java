package com.zodiakk.analyticservice.integration.dto;

import java.util.List;
import java.util.UUID;

public record SubordinateIdsDto(
        UUID employeeId,
        List<UUID> subordinateIds
) {
}
