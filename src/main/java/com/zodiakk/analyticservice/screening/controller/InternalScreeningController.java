package com.zodiakk.analyticservice.screening.controller;

import com.zodiakk.analyticservice.job.entity.AnalyticsJob;
import com.zodiakk.analyticservice.screening.dto.request.action.CandidateSelectionDto;
import com.zodiakk.analyticservice.job.service.AnalyticsJobService;
import com.zodiakk.analyticservice.screening.dto.internal.InternalScreeningResponse;
import com.zodiakk.analyticservice.screening.service.ScreeningQueryService;
import com.zodiakk.analyticservice.screening.service.ScreeningStartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/analytics/talent-reserve/screenings")
@RequiredArgsConstructor
public class InternalScreeningController {

    private final ScreeningStartService screeningStartService;

    private final ScreeningQueryService screeningQueryService;

    @PostMapping
    public InternalStartResponse start(@RequestBody InternalStartRequest request) {
        AnalyticsJob job = screeningStartService.start(
                AnalyticsJobService.SYSTEM_REQUESTER,
                request.reserveId(),
                request.targetPositionId(),
                request.targetGradeId(),
                request.requirementId(),
                null,
                "INTERNAL",
                null,
                request.candidateSelection()
        );
        return new InternalStartResponse(job.getId(), job.getStatus().name());
    }

    @GetMapping("/{jobId}")
    public InternalScreeningResponse get(@PathVariable UUID jobId) {
        return screeningQueryService.internalByJob(jobId);
    }

    public record InternalStartRequest(
            UUID reserveId,
            UUID targetPositionId,
            UUID targetGradeId,
            UUID requirementId,
            CandidateSelectionDto candidateSelection
    ) {
    }

    public record InternalStartResponse(UUID jobId, String status) {
    }
}
