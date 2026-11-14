package com.zodiakk.analyticservice.assessment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentComponentResponseDto {

    @Schema(description = "Id")
    private UUID id;

    @Schema(description = "Assessment id")
    private UUID assessmentId;

    @Schema(description = "Component code")
    private String componentCode;

    @Schema(description = "Raw score")
    private BigDecimal rawScore;

    @Schema(description = "Coefficient")
    private BigDecimal coefficient;

    @Schema(description = "Weighted score")
    private BigDecimal weightedScore;

    @Schema(description = "Details")
    private String details;
}
