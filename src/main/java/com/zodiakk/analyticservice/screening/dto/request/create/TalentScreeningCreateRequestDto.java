package com.zodiakk.analyticservice.screening.dto.request.create;

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
public class TalentScreeningCreateRequestDto {

    @Schema(description = "Reserve id")
    @NotNull(message = ValidationMessages.RESERVE_ID_REQUIRED)
    private UUID reserveId;

    @Schema(description = "Target position id")
    @NotNull(message = ValidationMessages.TARGET_POSITION_ID_REQUIRED)
    private UUID targetPositionId;

    @Schema(description = "Target grade id")
    @NotNull(message = ValidationMessages.TARGET_GRADE_ID_REQUIRED)
    private UUID targetGradeId;
}
