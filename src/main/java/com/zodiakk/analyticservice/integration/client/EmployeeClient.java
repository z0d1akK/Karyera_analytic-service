package com.zodiakk.analyticservice.integration.client;

import com.zodiakk.analyticservice.integration.config.IntegrationFeignConfig;
import com.zodiakk.analyticservice.integration.dto.ActiveEmployeeIdsDto;
import com.zodiakk.analyticservice.integration.dto.CareerProfileDto;
import com.zodiakk.analyticservice.integration.dto.CareerProfileLookupDto;
import com.zodiakk.analyticservice.integration.dto.EmployeeAccessScopeDto;
import com.zodiakk.analyticservice.integration.dto.EmployeeLookupItemDto;
import com.zodiakk.analyticservice.integration.dto.EmployeeLookupResponseDto;
import com.zodiakk.analyticservice.integration.dto.IdListDto;
import com.zodiakk.analyticservice.integration.dto.IdsDto;
import com.zodiakk.analyticservice.integration.dto.ReferenceLookupDto;
import com.zodiakk.analyticservice.integration.dto.SubordinateIdsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(
        name = "employee-service",
        url = "${services.employee.url}",
        configuration = IntegrationFeignConfig.class
)
public interface EmployeeClient {

    @GetMapping("/internal/v1/employees/{employeeId}/career-profile")
    CareerProfileDto careerProfile(@PathVariable UUID employeeId);

    @PostMapping("/internal/v1/employees/career-profiles/lookup")
    CareerProfileLookupDto careerProfiles(@RequestBody IdListDto request);

    @GetMapping("/internal/v1/employees/active-ids")
    ActiveEmployeeIdsDto activeIds();

    @GetMapping("/internal/v1/employees/filter-ids")
    ActiveEmployeeIdsDto filterIds(
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @RequestParam(required = false) UUID positionId,
            @RequestParam(required = false) UUID gradeId,
            @RequestParam(required = false) UUID departmentId
    );

    @PostMapping("/internal/v1/employees/lookup")
    EmployeeLookupResponseDto lookup(@RequestBody IdListDto request);

    @GetMapping("/internal/v1/employees/by-user/{userId}")
    EmployeeLookupItemDto employeeByUser(@PathVariable UUID userId);

    @GetMapping("/internal/v1/employees/{employeeId}/access-scope")
    EmployeeAccessScopeDto accessScope(
            @PathVariable UUID employeeId,
            @RequestParam(required = false) UUID actorEmployeeId
    );

    @GetMapping("/internal/v1/employees/{employeeId}/subordinate-ids")
    SubordinateIdsDto subordinateIds(@PathVariable UUID employeeId);

    @PostMapping("/internal/v1/positions/lookup")
    ReferenceLookupDto positions(@RequestBody IdsDto request);

    @PostMapping("/internal/v1/grades/lookup")
    ReferenceLookupDto grades(@RequestBody IdsDto request);

    @PostMapping("/internal/v1/skills/lookup")
    ReferenceLookupDto skills(@RequestBody IdsDto request);
}
