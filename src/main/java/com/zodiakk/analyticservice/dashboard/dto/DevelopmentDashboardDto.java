package com.zodiakk.analyticservice.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DevelopmentDashboardDto(List<Point> points) {

    public record Point(LocalDate period, long assessments, BigDecimal averageScore) {
    }
}
