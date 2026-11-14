package com.zodiakk.analyticservice.assessment.dto.request.update;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentComponentUpdateRequestDto {

    @Schema(description = "Raw score")
    private BigDecimal rawScore;

    @Schema(description = "Coefficient")
    private BigDecimal coefficient;

    @Schema(description = "Weighted score")
    private BigDecimal weightedScore;

    @Schema(description = "Details")
    private String details;
}
