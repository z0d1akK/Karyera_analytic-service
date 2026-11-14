package com.zodiakk.analyticservice.assessment.dto.request.filter;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentComponentFilterDto {

    @Schema(description = "Assessment id")
    private UUID assessmentId;

    @Schema(description = "Component code")
    private String componentCode;
}
