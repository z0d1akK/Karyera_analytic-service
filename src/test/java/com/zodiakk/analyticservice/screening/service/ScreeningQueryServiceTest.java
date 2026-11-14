package com.zodiakk.analyticservice.screening.service;

import com.zodiakk.analyticservice.common.exception.JobNotFoundException;
import com.zodiakk.analyticservice.common.exception.ScreeningNotFoundException;
import com.zodiakk.analyticservice.job.entity.AnalyticsJob;
import com.zodiakk.analyticservice.job.repository.AnalyticsJobRepository;
import com.zodiakk.analyticservice.job.service.AnalyticsJobService;
import com.zodiakk.analyticservice.screening.entity.TalentScreening;
import com.zodiakk.analyticservice.screening.repository.ScreeningResultRepository;
import com.zodiakk.analyticservice.screening.repository.TalentScreeningRepository;
import com.zodiakk.analyticservice.security.access.AnalyticsAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScreeningQueryServiceTest {

    @Mock
    private TalentScreeningRepository screeningRepository;

    @Mock
    private ScreeningResultRepository resultRepository;

    @Mock
    private AnalyticsJobRepository jobRepository;

    @Mock
    private AnalyticsJobService jobService;

    @Mock
    private AnalyticsAccessService accessService;

    @InjectMocks
    private ScreeningQueryService service;

    @Test
    void throwsWhenScreeningOrJobIsMissing() {
        UUID screeningId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        when(screeningRepository.findById(screeningId)).thenReturn(Optional.empty());
        assertThrows(ScreeningNotFoundException.class, () -> service.get(screeningId));
        assertThrows(ScreeningNotFoundException.class, () -> service.results(screeningId, 0, 10));
        assertThrows(ScreeningNotFoundException.class, () -> service.result(screeningId, UUID.randomUUID()));

        when(accessService.isHr()).thenReturn(true);
        when(screeningRepository.findAllByJobId(jobId)).thenReturn(List.of());
        assertThrows(JobNotFoundException.class, () -> service.byJob(jobId));

        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());
        assertThrows(JobNotFoundException.class, () -> service.internalByJob(jobId));
    }

    @Test
    void mapsExistingScreening() {
        UUID screeningId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        TalentScreening screening = TalentScreening.builder()
                .id(screeningId)
                .jobId(jobId)
                .status(com.zodiakk.analyticservice.common.enums.ScreeningStatus.COMPLETED)
                .candidateCount(0)
                .evaluatedCount(0)
                .failedCount(0)
                .build();
        when(screeningRepository.findById(screeningId)).thenReturn(Optional.of(screening));
        when(resultRepository.findAllByScreeningIdOrderByRankAsc(screeningId)).thenReturn(List.of());
        when(accessService.isHrOrAdmin()).thenReturn(true);

        assertEquals(screeningId, service.get(screeningId).getId());
        when(screeningRepository.findAll(org.springframework.data.domain.PageRequest.of(0, 10)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(screening)));
        assertEquals(1, service.list(0, 10).getTotalElements());
        assertEquals(0, service.results(screeningId, 0, 10).getTotalElements());

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(AnalyticsJob.builder().id(jobId).parameters("{}").build()));
        when(jobService.read(org.mockito.ArgumentMatchers.eq("{}"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new com.zodiakk.analyticservice.job.dto.ScreeningJobParameters(
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        List.of(), UUID.randomUUID(), "HR", "ALL"
                ));
        when(screeningRepository.findAllByJobId(jobId)).thenReturn(List.of(screening));
        assertEquals(jobId, service.internalByJob(jobId).jobId());
    }
}
