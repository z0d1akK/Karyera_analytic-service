package com.zodiakk.analyticservice.dashboard.dto;

import java.time.LocalDate;
import java.util.List;

public record ProjectActivityDashboardDto(List<Point> points) {

    public record Point(LocalDate periodStart, long approvedMinutes, long completedTasks) {
    }
}
