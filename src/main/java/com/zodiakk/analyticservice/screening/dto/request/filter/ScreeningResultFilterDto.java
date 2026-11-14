package com.zodiakk.analyticservice.screening.dto.request.filter;

import com.zodiakk.analyticservice.common.enums.*;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScreeningResultFilterDto {

    @Schema(description = "Screening id")
    private UUID screeningId;

    @Schema(description = "Employee id")
    private UUID employeeId;

    @Schema(description = "Eligibility status")
    private ScreeningEligibilityStatus eligibilityStatus;
}
