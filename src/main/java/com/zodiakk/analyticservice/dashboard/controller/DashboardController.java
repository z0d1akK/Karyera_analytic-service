package com.zodiakk.analyticservice.dashboard.controller;

import com.zodiakk.analyticservice.dashboard.dto.CareerReadinessDashboardDto;
import com.zodiakk.analyticservice.dashboard.dto.DashboardGrain;
import com.zodiakk.analyticservice.dashboard.dto.DashboardOverviewDto;
import com.zodiakk.analyticservice.dashboard.dto.DepartmentDashboardDto;
import com.zodiakk.analyticservice.dashboard.dto.DevelopmentDashboardDto;
import com.zodiakk.analyticservice.dashboard.dto.DevelopmentDynamicsDto;
import com.zodiakk.analyticservice.dashboard.dto.ProjectActivityDashboardDto;
import com.zodiakk.analyticservice.dashboard.dto.SkillDeficitDashboardDto;
import com.zodiakk.analyticservice.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard/overview")
    public DashboardOverviewDto overview() {
        return dashboardService.overview();
    }

    @GetMapping("/dashboard/skills")
    public SkillDeficitDashboardDto skills() {
        return dashboardService.skills();
    }

    @GetMapping("/dashboard/skill-deficits")
    public SkillDeficitDashboardDto skillDeficits() {
        return dashboardService.skillsTop();
    }

    @GetMapping("/dashboard/departments")
    public DepartmentDashboardDto departments() {
        return dashboardService.departments();
    }

    @GetMapping("/dashboard/career-readiness")
    public CareerReadinessDashboardDto readiness() {
        return dashboardService.readiness();
    }

    @GetMapping("/dashboard/development")
    public DevelopmentDashboardDto development(
            @RequestParam(defaultValue = "MONTH") DashboardGrain grain
    ) {
        return dashboardService.development(grain);
    }

    @GetMapping("/dashboard/project-activity")
    public ProjectActivityDashboardDto projectActivity(
            @RequestParam(defaultValue = "MONTH") DashboardGrain grain
    ) {
        return dashboardService.projectActivity(grain);
    }

    @GetMapping("/employees/{id}/development-dynamics")
    public DevelopmentDynamicsDto dynamics(@PathVariable UUID id) {
        return dashboardService.dynamics(id);
    }
}
