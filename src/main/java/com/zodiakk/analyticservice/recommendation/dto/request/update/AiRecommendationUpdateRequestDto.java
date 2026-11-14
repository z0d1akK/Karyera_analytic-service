package com.zodiakk.analyticservice.recommendation.dto.request.update;

import com.zodiakk.analyticservice.common.enums.*;
import com.zodiakk.analyticservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRecommendationUpdateRequestDto {

    @Schema(description = "Status")
    private RecommendationStatus status;

    @Schema(description = "Model identifier")
    @Size(max = 100, message = ValidationMessages.MODEL_IDENTIFIER_SIZE)
    private String modelIdentifier;

    @Schema(description = "Prompt version")
    @Size(max = 30, message = ValidationMessages.PROMPT_VERSION_SIZE)
    private String promptVersion;

    @Schema(description = "Content")
    private String content;

    @Schema(description = "Error code")
    @Size(max = 100, message = ValidationMessages.ERROR_CODE_SIZE)
    private String errorCode;

    @Schema(description = "Generated at")
    private OffsetDateTime generatedAt;
}
