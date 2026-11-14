package com.zodiakk.analyticservice.integration.dto;

import java.util.List;
import java.util.UUID;

public record ActiveEmployeeIdsDto(List<UUID> employeeIds) {
}
