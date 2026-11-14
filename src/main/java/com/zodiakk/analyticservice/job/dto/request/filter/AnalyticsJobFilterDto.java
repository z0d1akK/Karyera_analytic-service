package com.zodiakk.analyticservice.job.dto.request.filter;

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
public class AnalyticsJobFilterDto {

    @Schema(description = "Job type")
    private AnalyticsJobType jobType;

    @Schema(description = "Status")
    private AnalyticsJobStatus status;

    @Schema(description = "Requested by")
    private UUID requestedBy;

    @Schema(description = "Creation time from (inclusive)")
    private OffsetDateTime createdAtFrom;

    @Schema(description = "Creation time to (inclusive)")
    private OffsetDateTime createdAtTo;
}
