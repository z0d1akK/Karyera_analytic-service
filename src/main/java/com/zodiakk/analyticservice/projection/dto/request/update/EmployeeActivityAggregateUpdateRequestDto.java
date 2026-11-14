package com.zodiakk.analyticservice.projection.dto.request.update;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeActivityAggregateUpdateRequestDto {

    @Schema(description = "Period end")
    private LocalDate periodEnd;

    @Schema(description = "Completed tasks")
    private Integer completedTasks;

    @Schema(description = "Completed story points")
    private BigDecimal completedStoryPoints;

    @Schema(description = "Approved minutes")
    private Long approvedMinutes;

    @Schema(description = "Source updated at")
    private OffsetDateTime sourceUpdatedAt;

    @Schema(description = "Last calculated at")
    private OffsetDateTime lastCalculatedAt;
}
