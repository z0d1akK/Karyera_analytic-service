package com.zodiakk.analyticservice.screening.dto.request.action;

import com.zodiakk.analyticservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateSelectionDto {

    @Schema(description = "Candidate selection type", example = "FILTER")
    @NotBlank(message = ValidationMessages.TYPE_REQUIRED)
    @Size(max = 20, message = ValidationMessages.TYPE_SIZE)
    private String type;

    @Schema(description = "Explicit employee identifiers for EXPLICIT selection")
    private List<UUID> employeeIds;

    @Schema(description = "Whether only active employees should be selected")
    private Boolean activeOnly;

    @Schema(description = "Position filter for FILTER selection")
    private UUID positionId;

    @Schema(description = "Grade filter for FILTER selection")
    private UUID gradeId;

    @Schema(description = "Department filter for FILTER selection")
    private UUID departmentId;
}
