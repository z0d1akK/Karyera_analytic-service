package com.zodiakk.analyticservice.assessment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerOptionsResponseDto {

    @Schema(description = "Employee identifier")
    private UUID employeeId;

    @Schema(description = "Evaluated career options")
    private List<CareerOptionResponseDto> options;
}
