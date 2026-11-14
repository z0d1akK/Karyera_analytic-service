package com.zodiakk.analyticservice.projection.dto.request.filter;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeAnalyticsProfileFilterDto {

    @Schema(description = "Position id")
    private UUID positionId;

    @Schema(description = "Grade id")
    private UUID gradeId;

    @Schema(description = "Active")
    private Boolean active;

    @Schema(description = "Stale")
    private Boolean stale;
}
