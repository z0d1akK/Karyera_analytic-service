package com.zodiakk.analyticservice.assessment.dto.request.action;

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
public class CareerAssessmentStartRequestDto {

    @Schema(description = "Employee identifier")
    @NotNull(message = ValidationMessages.EMPLOYEE_ID_REQUIRED)
    private UUID employeeId;

    @Schema(description = "Target position identifier")
    @NotNull(message = ValidationMessages.TARGET_POSITION_ID_REQUIRED)
    private UUID targetPositionId;

    @Schema(description = "Target grade identifier")
    @NotNull(message = ValidationMessages.TARGET_GRADE_ID_REQUIRED)
    private UUID targetGradeId;

    @Schema(description = "Assessment mode", example = "SKILLS_AND_EXPERIENCE")
    @NotBlank(message = ValidationMessages.MODE_REQUIRED)
    @Size(max = 40, message = ValidationMessages.MODE_SIZE)
    private String mode;

    @Schema(description = "Whether AI recommendations should be requested after the assessment")
    private Boolean generateRecommendations;

    @Schema(description = "Explicit requirement version for historical recalculation")
    private UUID requirementId;
}
