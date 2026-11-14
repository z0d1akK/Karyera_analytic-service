package com.zodiakk.analyticservice.integration.client;

import com.zodiakk.analyticservice.integration.config.IntegrationFeignConfig;
import com.zodiakk.analyticservice.integration.dto.ProjectMemberIdsDto;
import com.zodiakk.analyticservice.integration.dto.TaskStatisticsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.UUID;

@FeignClient(
        name = "project-service",
        url = "${services.project.url}",
        configuration = IntegrationFeignConfig.class
)
public interface ProjectClient {

    @GetMapping("/internal/v1/projects/{projectId}/member-ids")
    ProjectMemberIdsDto memberIds(@PathVariable UUID projectId);

    @GetMapping("/internal/v1/employees/{employeeId}/task-statistics")
    TaskStatisticsDto taskStatistics(
            @PathVariable UUID employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd
    );
}
