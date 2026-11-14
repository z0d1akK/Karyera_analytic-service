package com.zodiakk.analyticservice.integration.client;

import com.zodiakk.analyticservice.integration.config.IntegrationFeignConfig;
import com.zodiakk.analyticservice.integration.dto.AvailableTargetsDto;
import com.zodiakk.analyticservice.integration.dto.RequirementDto;
import com.zodiakk.analyticservice.integration.dto.ReserveContextDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(
        name = "career-service",
        url = "${services.career.url}",
        configuration = IntegrationFeignConfig.class
)
public interface CareerClient {

    @GetMapping("/internal/v1/career/requirements/current")
    RequirementDto currentRequirement(
            @RequestParam UUID positionId,
            @RequestParam UUID gradeId
    );

    @GetMapping("/internal/v1/career/requirements/{requirementId}")
    RequirementDto requirement(@PathVariable UUID requirementId);

    @GetMapping("/internal/v1/career/tracks/{trackId}/available-targets")
    AvailableTargetsDto availableTargets(
            @PathVariable UUID trackId,
            @RequestParam UUID employeeId
    );

    @GetMapping("/internal/v1/career/talent-reserves/{reserveId}/context")
    ReserveContextDto reserveContext(@PathVariable UUID reserveId);

    @GetMapping("/internal/v1/career/employees/{employeeId}/development-plans")
    Object developmentPlans(@PathVariable UUID employeeId);
}
