package com.zodiakk.analyticservice.recommendation.dto.request.create;

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
public class AiRecommendationCreateRequestDto {

    @Schema(description = "Assessment id")
    @NotNull(message = ValidationMessages.ASSESSMENT_ID_REQUIRED)
    private UUID assessmentId;

    @Schema(description = "Model identifier")
    @Size(max = 100, message = ValidationMessages.MODEL_IDENTIFIER_SIZE)
    private String modelIdentifier;

    @Schema(description = "Prompt version")
    @Size(max = 30, message = ValidationMessages.PROMPT_VERSION_SIZE)
    private String promptVersion;
}
