package com.zodiakk.analyticservice.job.dto.response;

import com.zodiakk.analyticservice.common.enums.AnalyticsJobStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobAcceptedResponseDto {

    @Schema(description = "Identifier of the created job")
    private UUID jobId;

    @Schema(description = "Initial job status")
    private AnalyticsJobStatus status;

    @Schema(description = "URL used to poll the job status")
    private String statusUrl;
}
