package com.zodiakk.analyticservice.job.dto;

import java.util.UUID;

public record CareerOptionsJobParameters(
        UUID employeeId,
        UUID trackId,
        int maxOptions
) {
}
