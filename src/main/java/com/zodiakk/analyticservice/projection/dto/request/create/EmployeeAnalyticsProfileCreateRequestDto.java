package com.zodiakk.analyticservice.projection.dto.request.create;

import com.zodiakk.analyticservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeAnalyticsProfileCreateRequestDto {

    @Schema(description = "Employee id")
    @NotNull(message = ValidationMessages.EMPLOYEE_ID_REQUIRED)
    private UUID employeeId;

    @Schema(description = "Position id")
    @NotNull(message = ValidationMessages.POSITION_ID_REQUIRED)
    private UUID positionId;

    @Schema(description = "Grade id")
    @NotNull(message = ValidationMessages.GRADE_ID_REQUIRED)
    private UUID gradeId;

    @Schema(description = "Active")
    @NotNull(message = ValidationMessages.ACTIVE_REQUIRED)
    private Boolean active;
}
