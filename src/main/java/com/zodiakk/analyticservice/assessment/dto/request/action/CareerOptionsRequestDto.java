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
public class CareerOptionsRequestDto {

    @Schema(description = "Career track identifier")
    @NotNull(message = ValidationMessages.TRACK_ID_REQUIRED)
    private UUID trackId;

    @Schema(description = "Maximum number of career options to evaluate", example = "5")
    @Min(value = 1, message = ValidationMessages.MAX_OPTIONS_MIN)
    @Max(value = 50, message = ValidationMessages.MAX_OPTIONS_MAX)
    private Integer maxOptions;
}
