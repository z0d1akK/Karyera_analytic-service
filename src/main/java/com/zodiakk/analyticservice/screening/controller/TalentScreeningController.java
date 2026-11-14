package com.zodiakk.analyticservice.screening.controller;

import com.zodiakk.analyticservice.job.dto.response.JobAcceptedResponseDto;
import com.zodiakk.analyticservice.job.entity.AnalyticsJob;
import com.zodiakk.analyticservice.screening.dto.request.action.TalentScreeningStartRequestDto;
import com.zodiakk.analyticservice.screening.dto.response.ScreeningResultResponseDto;
import com.zodiakk.analyticservice.screening.dto.response.TalentScreeningResponseDto;
import com.zodiakk.analyticservice.screening.service.ScreeningQueryService;
import com.zodiakk.analyticservice.screening.service.ScreeningStartService;
import com.zodiakk.analyticservice.security.access.AnalyticsAccessService;
import com.zodiakk.analyticservice.security.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics/talent-screenings")
@RequiredArgsConstructor
public class TalentScreeningController {

    private final ScreeningStartService screeningStartService;

    private final ScreeningQueryService screeningQueryService;

    private final AnalyticsAccessService accessService;

    @PostMapping
    public ResponseEntity<JobAcceptedResponseDto> start(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TalentScreeningStartRequestDto request
    ) {
        accessService.requireHrOrAdmin();
        AnalyticsJob job = screeningStartService.start(
                SecurityUtils.getCurrentUserId(),
                request.getReserveId(),
                request.getTargetPositionId(),
                request.getTargetGradeId(),
                null,
                accessService.currentEmployeeId(),
                SecurityUtils.getCurrentUser().getRole(),
                idempotencyKey,
                request.getCandidateSelection()
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(accepted(job));
    }

    @GetMapping
    public Page<TalentScreeningResponseDto> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return screeningQueryService.list(page, Math.min(size, 100));
    }

    @GetMapping("/{id}")
    public TalentScreeningResponseDto get(@PathVariable UUID id) {
        return screeningQueryService.get(id);
    }

    @GetMapping("/{id}/results")
    public Page<ScreeningResultResponseDto> results(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return screeningQueryService.results(id, page, Math.min(size, 100));
    }

    @GetMapping("/{id}/results/{employeeId}")
    public ScreeningResultResponseDto result(@PathVariable UUID id, @PathVariable UUID employeeId) {
        return screeningQueryService.result(id, employeeId);
    }

    private JobAcceptedResponseDto accepted(AnalyticsJob job) {
        return JobAcceptedResponseDto.builder()
                .jobId(job.getId())
                .status(job.getStatus())
                .statusUrl("/api/v1/analytics/jobs/" + job.getId())
                .build();
    }
}
