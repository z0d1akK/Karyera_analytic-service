package com.zodiakk.analyticservice.integration.client;

import com.zodiakk.analyticservice.integration.config.IntegrationFeignConfig;
import com.zodiakk.analyticservice.integration.dto.ApprovedSummaryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.UUID;

@FeignClient(
        name = "timetrack-service",
        url = "${services.timetrack.url}",
        configuration = IntegrationFeignConfig.class
)
public interface TimeClient {

    @GetMapping("/internal/v1/time/employees/{employeeId}/approved-summary")
    ApprovedSummaryDto approvedSummary(
            @PathVariable UUID employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    );
}
