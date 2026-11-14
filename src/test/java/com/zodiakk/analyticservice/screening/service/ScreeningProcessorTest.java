package com.zodiakk.analyticservice.screening.service;

import com.zodiakk.analyticservice.assessment.service.AssessmentEngine;
import com.zodiakk.analyticservice.common.enums.ScreeningEligibilityStatus;
import com.zodiakk.analyticservice.common.enums.ScreeningStatus;
import com.zodiakk.analyticservice.common.exception.InsufficientDataException;
import com.zodiakk.analyticservice.integration.client.CareerClient;
import com.zodiakk.analyticservice.integration.client.EmployeeClient;
import com.zodiakk.analyticservice.integration.client.IntelligenceClient;
import com.zodiakk.analyticservice.integration.dto.CareerProfileDto;
import com.zodiakk.analyticservice.integration.dto.CareerProfileLookupDto;
import com.zodiakk.analyticservice.integration.dto.ReferenceLookupDto;
import com.zodiakk.analyticservice.integration.dto.RequirementDto;
import com.zodiakk.analyticservice.integration.dto.ReserveContextDto;
import com.zodiakk.analyticservice.integration.dto.TalentScreeningExplanationResponse;
import com.zodiakk.analyticservice.job.dto.ScreeningJobParameters;
import com.zodiakk.analyticservice.job.entity.AnalyticsJob;
import com.zodiakk.analyticservice.job.service.AnalyticsJobService;
import com.zodiakk.analyticservice.outbox.service.OutboxEventService;
import com.zodiakk.analyticservice.screening.entity.ScreeningResult;
import com.zodiakk.analyticservice.screening.entity.TalentScreening;
import com.zodiakk.analyticservice.screening.repository.ScreeningResultRepository;
import com.zodiakk.analyticservice.screening.repository.TalentScreeningRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScreeningProcessorTest {

    @Mock
    private AssessmentEngine assessmentEngine;
    @Mock
    private CareerClient careerClient;
    @Mock
    private TalentScreeningRepository screeningRepository;
    @Mock
    private ScreeningResultRepository resultRepository;
    @Mock
    private AnalyticsJobService jobService;
    @Mock
    private OutboxEventService outboxEventService;
    @Mock
    private IntelligenceClient intelligenceClient;
    @Mock
    private EmployeeClient employeeClient;
    @Mock
    private ScreeningProcessor self;

    @Test
    void processesCandidatesAndFinishesRanking() {
        UUID positionId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();
        UUID reserveId = UUID.randomUUID();
        UUID requirementId = UUID.randomUUID();
        UUID existing = UUID.randomUUID();
        UUID missing = UUID.randomUUID();
        UUID failed = UUID.randomUUID();
        AnalyticsJob job = AnalyticsJob.builder().id(UUID.randomUUID()).requestedBy(UUID.randomUUID()).build();
        ScreeningJobParameters parameters = new ScreeningJobParameters(
                reserveId, positionId, gradeId, requirementId, List.of(existing, missing, failed),
                UUID.randomUUID(), "HR", "ALL"
        );
        RequirementDto requirement = new RequirementDto(requirementId, positionId, gradeId, 1, "ACTIVE", 0, null, List.of());
        TalentScreening screening = TalentScreening.builder()
                .id(UUID.randomUUID())
                .reserveId(reserveId)
                .targetPositionId(positionId)
                .targetGradeId(gradeId)
                .candidateCount(3)
                .build();
        ScreeningProcessor processor = new ScreeningProcessor(
                assessmentEngine, careerClient, screeningRepository, resultRepository, jobService,
                outboxEventService, intelligenceClient, employeeClient, self
        );

        when(jobService.read(any(), eq(ScreeningJobParameters.class))).thenReturn(parameters);
        when(careerClient.reserveContext(reserveId)).thenReturn(new ReserveContextDto(
                reserveId, "R", "OPEN", positionId, gradeId, null, null
        ));
        when(assessmentEngine.loadRequirement(positionId, gradeId, requirementId)).thenReturn(requirement);
        when(self.ensureScreening(job, parameters, requirement)).thenReturn(screening);
        when(resultRepository.findByScreeningIdAndEmployeeId(screening.getId(), existing))
                .thenReturn(Optional.of(ScreeningResult.builder().build()));
        when(resultRepository.findByScreeningIdAndEmployeeId(screening.getId(), missing)).thenReturn(Optional.empty());
        when(resultRepository.findByScreeningIdAndEmployeeId(screening.getId(), failed)).thenReturn(Optional.empty());
        when(assessmentEngine.loadProfile(missing)).thenThrow(new InsufficientDataException("нет профиля"));
        when(assessmentEngine.loadProfile(failed)).thenThrow(new IllegalStateException("boom"));

        processor.process(job);

        verify(assessmentEngine, never()).executeAsChildJob(any(), eq(existing), any(), any(), any(), any(), any());
        verify(self).saveUnscored(screening.getId(), missing, ScreeningEligibilityStatus.INSUFFICIENT_DATA, "нет профиля");
        verify(self).saveUnscored(screening.getId(), failed, ScreeningEligibilityStatus.ASSESSMENT_FAILED, "ASSESSMENT_FAILED");
        verify(self).finish(job, screening);

        when(screeningRepository.findAllByJobId(job.getId())).thenReturn(List.of());
        when(screeningRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        TalentScreening created = processor.ensureScreening(job, parameters, requirement);
        assertEquals(ScreeningStatus.RUNNING, created.getStatus());
        assertEquals(3, created.getCandidateCount());

        when(screeningRepository.findAllByJobId(job.getId())).thenReturn(List.of(screening));
        assertEquals(screening, processor.ensureScreening(job, parameters, requirement));

        when(resultRepository.findByScreeningIdAndEmployeeId(screening.getId(), missing)).thenReturn(Optional.empty());
        processor.saveUnscored(screening.getId(), missing, ScreeningEligibilityStatus.INSUFFICIENT_DATA, "x".repeat(120));
        verify(resultRepository).save(any());

        UUID scoredId = UUID.randomUUID();
        ScreeningResult scored = ScreeningResult.builder()
                .screeningId(screening.getId())
                .employeeId(scoredId)
                .score(new BigDecimal("0.800000"))
                .eligibilityStatus(ScreeningEligibilityStatus.ELIGIBLE)
                .mandatoryRequirementsMet(true)
                .build();
        ScreeningResult gap = ScreeningResult.builder()
                .screeningId(screening.getId())
                .employeeId(UUID.randomUUID())
                .score(new BigDecimal("0.400000"))
                .eligibilityStatus(ScreeningEligibilityStatus.GAPS_PRESENT)
                .build();
        ScreeningResult failedResult = ScreeningResult.builder()
                .screeningId(screening.getId())
                .employeeId(UUID.randomUUID())
                .eligibilityStatus(ScreeningEligibilityStatus.ASSESSMENT_FAILED)
                .build();
        when(screeningRepository.findById(screening.getId())).thenReturn(Optional.of(screening));
        when(resultRepository.findAllByScreeningIdOrderByRankAsc(screening.getId()))
                .thenReturn(List.of(scored, gap, failedResult));
        when(employeeClient.careerProfiles(any())).thenReturn(new CareerProfileLookupDto(List.of(
                new CareerProfileDto(scoredId, positionId, "Lead", gradeId, "G", true, 1, List.of(), List.of(), "Иван")
        )));
        when(employeeClient.positions(any())).thenReturn(new ReferenceLookupDto(List.of(
                new ReferenceLookupDto.Item(positionId, "Lead", true)
        )));
        when(employeeClient.grades(any())).thenReturn(new ReferenceLookupDto(List.of(
                new ReferenceLookupDto.Item(gradeId, "Senior", true)
        )));
        when(intelligenceClient.explainScreening(any())).thenReturn(new TalentScreeningExplanationResponse(
                UUID.randomUUID(), "Рейтинг готов", List.of(), List.of(), "template-fallback", "FALLBACK"
        ));

        processor.finish(job, screening);

        assertEquals(1, scored.getRank());
        assertEquals(2, gap.getRank());
        assertEquals(ScreeningStatus.COMPLETED, screening.getStatus());
        assertEquals("Рейтинг готов", screening.getExplanationText());
        processor.tickProgress(screening.getId(), 3);
        assertEquals(3, screening.getEvaluatedCount());
    }

    @Test
    void rejectsMismatchedReserveTarget() {
        UUID positionId = UUID.randomUUID();
        AnalyticsJob job = AnalyticsJob.builder().id(UUID.randomUUID()).build();
        when(jobService.read(any(), eq(ScreeningJobParameters.class))).thenReturn(new ScreeningJobParameters(
                UUID.randomUUID(), positionId, UUID.randomUUID(), UUID.randomUUID(), List.of(),
                UUID.randomUUID(), "HR", "ALL"
        ));
        when(careerClient.reserveContext(any())).thenReturn(new ReserveContextDto(
                UUID.randomUUID(), "R", "OPEN", UUID.randomUUID(), UUID.randomUUID(), null, null
        ));

        ScreeningProcessor processor = new ScreeningProcessor(
                assessmentEngine, careerClient, screeningRepository, resultRepository, jobService,
                outboxEventService, intelligenceClient, employeeClient, self
        );
        assertThrows(InsufficientDataException.class, () -> processor.process(job));
    }
}
