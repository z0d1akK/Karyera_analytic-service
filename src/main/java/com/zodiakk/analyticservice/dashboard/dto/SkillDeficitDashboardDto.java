package com.zodiakk.analyticservice.dashboard.dto;

import java.util.List;
import java.util.UUID;

public record SkillDeficitDashboardDto(List<Item> deficits) {

    public record Item(UUID skillId, long employees, String skillName) {
    }
}
