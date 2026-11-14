package com.zodiakk.analyticservice.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record DepartmentDashboardDto(List<Item> departments) {

    public record Item(UUID departmentId, long assessedEmployees, BigDecimal averageScore) {
    }
}
