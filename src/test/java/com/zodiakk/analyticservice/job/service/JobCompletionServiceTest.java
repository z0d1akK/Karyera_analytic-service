package com.zodiakk.analyticservice.job.service;

import com.zodiakk.analyticservice.assessment.entity.CareerAssessment;
import com.zodiakk.analyticservice.common.enums.AnalyticsJobType;
import com.zodiakk.analyticservice.job.entity.AnalyticsJob;
import com.zodiakk.analyticservice.outbox.service.OutboxEventService;
import com.zodiakk.analyticservice.screening.entity.TalentScreening;
import com.zodiakk.analyticservice.screening.repository.TalentScreeningRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobCompletionServiceTest {

    @Mock
    private AnalyticsJobService jobService;

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private TalentScreeningRepository screeningRepository;

    @InjectMocks
    private JobCompletionService service;

    @Test
    void publishesAssessmentAndScreeningOutcomes() {
        AnalyticsJob assessmentJob = AnalyticsJob.builder().id(UUID.randomUUID()).jobType(AnalyticsJobType.CAREER_ASSESSMENT).build();
        CareerAssessment assessment = CareerAssessment.builder()
                .id(UUID.randomUUID())
                .employeeId(UUID.randomUUID())
                .targetPositionId(UUID.randomUUID())
                .targetGradeId(UUID.randomUUID())
                .requirementId(UUID.randomUUID())
                .totalScore(new BigDecimal("0.500000"))
                .mandatoryRequirementsMet(true)
                .build();

        service.completeAssessment(assessmentJob, assessment);
        service.fail(assessmentJob, "INSUFFICIENT_DATA", "нет данных");

        AnalyticsJob screeningJob = AnalyticsJob.builder().id(UUID.randomUUID()).jobType(AnalyticsJobType.TALENT_SCREENING).build();
        TalentScreening screening = TalentScreening.builder().id(UUID.randomUUID()).reserveId(UUID.randomUUID()).build();
        when(screeningRepository.findAllByJobId(screeningJob.getId())).thenReturn(List.of(screening));

        service.fail(screeningJob, "INSUFFICIENT_DATA", "резерв не найден");

        verify(outboxEventService, org.mockito.Mockito.atLeast(2)).saveEvent(any(), any(), any(), anyInt(), any());
        verify(jobService).complete(eq(assessmentJob.getId()), eq(assessment.getId()), any());
    }
}
