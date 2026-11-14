package com.zodiakk.analyticservice.job.dto.request.create;

import com.zodiakk.analyticservice.common.enums.*;
import com.zodiakk.analyticservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsJobCreateRequestDto {

    @Schema(description = "Job type")
    @NotNull(message = ValidationMessages.JOB_TYPE_REQUIRED)
    private AnalyticsJobType jobType;

    @Schema(description = "Parameters")
    @NotBlank(message = ValidationMessages.PARAMETERS_REQUIRED)
    private String parameters;
}
