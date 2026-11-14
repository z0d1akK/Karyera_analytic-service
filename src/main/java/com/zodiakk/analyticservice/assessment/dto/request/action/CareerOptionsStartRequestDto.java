package com.zodiakk.analyticservice.assessment.dto.request.action;

import com.zodiakk.analyticservice.common.validation.ValidationMessages;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CareerOptionsStartRequestDto {

    @Schema(description = "Career track identifier")
    @NotNull(message = ValidationMessages.MODE_REQUIRED)
    private UUID trackId;

    @Schema(description = "Maximum number of options to evaluate")
    @Min(1)
    @Max(20)
    private Integer maxOptions = 5;
}
