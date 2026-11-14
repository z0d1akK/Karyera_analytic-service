package com.zodiakk.analyticservice.assessment.dto.request.create;

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
public class CareerAssessmentCreateRequestDto {

    @Schema(description = "Employee id")
    @NotNull(message = ValidationMessages.EMPLOYEE_ID_REQUIRED)
    private UUID employeeId;

    @Schema(description = "Target position id")
    @NotNull(message = ValidationMessages.TARGET_POSITION_ID_REQUIRED)
    private UUID targetPositionId;

    @Schema(description = "Target grade id")
    @NotNull(message = ValidationMessages.TARGET_GRADE_ID_REQUIRED)
    private UUID targetGradeId;

    @Schema(description = "Methodology version")
    @NotBlank(message = ValidationMessages.METHODOLOGY_VERSION_REQUIRED)
    @Size(max = 30, message = ValidationMessages.METHODOLOGY_VERSION_SIZE)
    private String methodologyVersion;
}
