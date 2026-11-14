package com.zodiakk.analyticservice.dashboard.dto;

import java.util.List;

public record CareerReadinessDashboardDto(List<Bucket> buckets) {

    public record Bucket(String label, long employees) {
    }
}
