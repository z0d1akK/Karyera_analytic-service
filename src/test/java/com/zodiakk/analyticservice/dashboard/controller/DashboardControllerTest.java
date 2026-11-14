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
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardControllerTest {

    @Test
    void delegatesEveryDashboardEndpoint() {
        DashboardService dashboardService = mock(DashboardService.class);
        DashboardController controller = new DashboardController(dashboardService);
        DashboardOverviewDto overview = new DashboardOverviewDto(
                1, BigDecimal.ONE, 1, 1, BigDecimal.ONE, 1, null, null, 0, 0, BigDecimal.ONE
        );
        SkillDeficitDashboardDto skills = new SkillDeficitDashboardDto(List.of());
        DepartmentDashboardDto departments = new DepartmentDashboardDto(List.of());
        CareerReadinessDashboardDto readiness = new CareerReadinessDashboardDto(List.of());
        DevelopmentDashboardDto development = new DevelopmentDashboardDto(List.of());
        ProjectActivityDashboardDto activity = new ProjectActivityDashboardDto(List.of());
        UUID employeeId = UUID.randomUUID();
        DevelopmentDynamicsDto dynamics = new DevelopmentDynamicsDto(employeeId, List.of());

        when(dashboardService.overview()).thenReturn(overview);
        when(dashboardService.skills()).thenReturn(skills);
        when(dashboardService.skillsTop()).thenReturn(skills);
        when(dashboardService.departments()).thenReturn(departments);
        when(dashboardService.readiness()).thenReturn(readiness);
        when(dashboardService.development(DashboardGrain.MONTH)).thenReturn(development);
        when(dashboardService.projectActivity(DashboardGrain.YEAR)).thenReturn(activity);
        when(dashboardService.dynamics(employeeId)).thenReturn(dynamics);

        assertEquals(overview, controller.overview());
        assertEquals(skills, controller.skills());
        assertEquals(skills, controller.skillDeficits());
        assertEquals(departments, controller.departments());
        assertEquals(readiness, controller.readiness());
        assertEquals(development, controller.development(DashboardGrain.MONTH));
        assertEquals(activity, controller.projectActivity(DashboardGrain.YEAR));
        assertEquals(dynamics, controller.dynamics(employeeId));
    }
}
