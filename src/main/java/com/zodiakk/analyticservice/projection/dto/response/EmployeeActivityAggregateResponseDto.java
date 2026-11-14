package com.zodiakk.analyticservice.projection.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeActivityAggregateResponseDto {

    @Schema(description = "Id")
    private UUID id;

    @Schema(description = "Employee id")
    private UUID employeeId;

    @Schema(description = "Project id")
    private UUID projectId;

    @Schema(description = "Period start")
    private LocalDate periodStart;

    @Schema(description = "Period end")
    private LocalDate periodEnd;

    @Schema(description = "Completed tasks")
    private Integer completedTasks;

    @Schema(description = "Completed story points")
    private BigDecimal completedStoryPoints;

    @Schema(description = "Approved minutes")
    private Long approvedMinutes;

    @Schema(description = "Source updated at")
    private OffsetDateTime sourceUpdatedAt;

    @Schema(description = "Last calculated at")
    private OffsetDateTime lastCalculatedAt;
}
