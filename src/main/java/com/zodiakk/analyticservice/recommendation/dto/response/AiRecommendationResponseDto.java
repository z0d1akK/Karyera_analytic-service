package com.zodiakk.analyticservice.recommendation.dto.response;

import com.zodiakk.analyticservice.common.enums.*;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRecommendationResponseDto {

    @Schema(description = "Id")
    private UUID id;

    @Schema(description = "Created at")
    private OffsetDateTime createdAt;

    @Schema(description = "Assessment id")
    private UUID assessmentId;

    @Schema(description = "Status")
    private RecommendationStatus status;

    @Schema(description = "Model identifier")
    private String modelIdentifier;

    @Schema(description = "Prompt version")
    private String promptVersion;

    @Schema(description = "Content")
    private String content;

    @Schema(description = "Error code")
    private String errorCode;

    @Schema(description = "Generated at")
    private OffsetDateTime generatedAt;
}
