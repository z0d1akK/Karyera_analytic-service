package com.zodiakk.analyticservice.projection.dto.request.create;

import com.zodiakk.analyticservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeActivityAggregateCreateRequestDto {

    @Schema(description = "Employee id")
    @NotNull(message = ValidationMessages.EMPLOYEE_ID_REQUIRED)
    private UUID employeeId;

    @Schema(description = "Project id")
    @NotNull(message = ValidationMessages.PROJECT_ID_REQUIRED)
    private UUID projectId;

    @Schema(description = "Period start")
    @NotNull(message = ValidationMessages.PERIOD_START_REQUIRED)
    private LocalDate periodStart;

    @Schema(description = "Period end")
    @NotNull(message = ValidationMessages.PERIOD_END_REQUIRED)
    private LocalDate periodEnd;

    @Schema(description = "Completed tasks")
    @NotNull(message = ValidationMessages.COMPLETED_TASKS_REQUIRED)
    private Integer completedTasks;

    @Schema(description = "Completed story points")
    private BigDecimal completedStoryPoints;

    @Schema(description = "Approved minutes")
    @NotNull(message = ValidationMessages.APPROVED_MINUTES_REQUIRED)
    private Long approvedMinutes;

    @Schema(description = "Source updated at")
    private OffsetDateTime sourceUpdatedAt;

    @Schema(description = "Last calculated at")
    @NotNull(message = ValidationMessages.LAST_CALCULATED_AT_REQUIRED)
    private OffsetDateTime lastCalculatedAt;
}
