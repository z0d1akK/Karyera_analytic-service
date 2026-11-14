package com.zodiakk.analyticservice.job.controller;

import com.zodiakk.analyticservice.common.enums.AnalyticsJobType;
import com.zodiakk.analyticservice.job.dto.response.JobStatusResponseDto;
import com.zodiakk.analyticservice.job.entity.AnalyticsJob;
import com.zodiakk.analyticservice.job.service.AnalyticsJobService;
import com.zodiakk.analyticservice.job.worker.AnalyticsJobWorker;
import com.zodiakk.analyticservice.screening.entity.TalentScreening;
import com.zodiakk.analyticservice.screening.repository.TalentScreeningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics/jobs")
@RequiredArgsConstructor
public class AnalyticsJobController {

    private final AnalyticsJobService jobService;

    private final ObjectMapper objectMapper;

    private final TalentScreeningRepository screeningRepository;

    @GetMapping("/{id}")
    public JobStatusResponseDto status(@PathVariable UUID id) {
        AnalyticsJob job = jobService.getVisible(id);
        int evaluated = 0;
        int total = 0;
        if (job.getJobType() == AnalyticsJobType.TALENT_SCREENING) {
            java.util.List<TalentScreening> screenings = screeningRepository.findAllByJobId(job.getId());
            if (!screenings.isEmpty()) {
                evaluated = screenings.getFirst().getEvaluatedCount() == null ? 0 : screenings.getFirst().getEvaluatedCount();
                total = screenings.getFirst().getCandidateCount() == null ? 0 : screenings.getFirst().getCandidateCount();
            }
        }
        return new JobStatusResponseDto(
                job.getId(),
                job.getJobType().name(),
                job.getStatus().name(),
                jobService.progress(job, evaluated, total),
                job.getResultId(),
                job.getCreatedAt(),
                job.getCompletedAt(),
                job.getErrorCode()
        );
    }

    @GetMapping("/{id}/career-options")
    public AnalyticsJobWorker.CareerOptionsResult careerOptions(@PathVariable UUID id) {
        AnalyticsJob job = jobService.getVisible(id);

        if (job.getResult() == null) {
            return new AnalyticsJobWorker.CareerOptionsResult(null, java.util.List.of(), null);
        }

        try {
            return objectMapper.readValue(job.getResult(), AnalyticsJobWorker.CareerOptionsResult.class);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Не удалось прочитать результат карьерных вариантов", ex);
        }
    }
}
