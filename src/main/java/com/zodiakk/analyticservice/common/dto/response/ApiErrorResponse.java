package com.zodiakk.analyticservice.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Standard API error response")
public class ApiErrorResponse {

    @Schema(example = "404")
    private int status;

    @Schema(example = "Not Found")
    private String error;

    @Schema(example = "Career track with id ... not found")
    private String message;

    @Schema(example = "2026-05-16T15:30:00")
    private LocalDateTime timestamp;

    @Schema(example = "INSUFFICIENT_DATA")
    private String code;

    @Schema(description = "Missing assessment components")
    private List<String> missing;

    @Schema(description = "Assessment modes that can still be used")
    private List<String> suggestedModes;
}
