package com.zodiakk.analyticservice.projection.dto.response;

import com.zodiakk.analyticservice.common.dto.*;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeAnalyticsProfileResponseDto {

    @Schema(description = "Employee id")
    private UUID employeeId;

    @Schema(description = "Position id")
    private UUID positionId;

    @Schema(description = "Grade id")
    private UUID gradeId;

    @Schema(description = "Active")
    private Boolean active;

    @Schema(description = "Last source event id")
    private UUID lastSourceEventId;

    @Schema(description = "Source version")
    private Long sourceVersion;

    @Schema(description = "Last synced at")
    private OffsetDateTime lastSyncedAt;

    @Schema(description = "Stale")
    private Boolean stale;

    @Schema(description = "Resolved employee summary")
    private EmployeeReferenceDto employeeReference;
}
