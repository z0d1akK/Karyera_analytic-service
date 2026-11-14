package com.zodiakk.analyticservice.assessment.dto.request.action;

import com.zodiakk.analyticservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRecommendationGenerationRequestDto {

    @Schema(description = "Whether an existing recommendation should be regenerated")
    private Boolean force;

    @Schema(description = "Response language", example = "ru")
    @Size(max = 8, message = ValidationMessages.LANGUAGE_SIZE)
    private String language;
}
