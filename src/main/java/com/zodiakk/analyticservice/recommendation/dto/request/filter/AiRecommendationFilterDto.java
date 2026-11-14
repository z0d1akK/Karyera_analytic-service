package com.zodiakk.analyticservice.recommendation.dto.request.filter;

import com.zodiakk.analyticservice.common.enums.*;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRecommendationFilterDto {

    @Schema(description = "Assessment id")
    private UUID assessmentId;

    @Schema(description = "Status")
    private RecommendationStatus status;
}
