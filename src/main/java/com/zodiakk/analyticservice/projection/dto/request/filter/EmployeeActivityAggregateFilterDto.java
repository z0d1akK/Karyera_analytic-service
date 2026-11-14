package com.zodiakk.analyticservice.projection.dto.request.filter;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeActivityAggregateFilterDto {

    @Schema(description = "Employee id")
    private UUID employeeId;

    @Schema(description = "Project id")
    private UUID projectId;

    @Schema(description = "Period start from (inclusive)")
    private LocalDate periodStartFrom;

    @Schema(description = "Period start to (inclusive)")
    private LocalDate periodStartTo;
}
