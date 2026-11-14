package com.zodiakk.analyticservice.job.service;

import com.zodiakk.analyticservice.assessment.entity.CareerAssessment;
import com.zodiakk.analyticservice.common.constants.KafkaEventConstants;
import com.zodiakk.analyticservice.common.enums.AnalyticsJobType;
import com.zodiakk.analyticservice.common.enums.ScreeningStatus;
import com.zodiakk.analyticservice.job.entity.AnalyticsJob;
import com.zodiakk.analyticservice.outbox.service.OutboxEventService;
import com.zodiakk.analyticservice.screening.entity.TalentScreening;
import com.zodiakk.analyticservice.screening.repository.TalentScreeningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JobCompletionService {

    private final AnalyticsJobService jobService;

    private final OutboxEventService outboxEventService;

    private final TalentScreeningRepository screeningRepository;

    @Transactional
    @CacheEvict(cacheNames = "dashboards", allEntries = true)
    public void completeAssessment(AnalyticsJob job, CareerAssessment assessment) {
        jobService.complete(job.getId(), assessment.getId(), Map.of("assessmentId", assessment.getId()));
        outboxEventService.saveEvent(
                assessment.getId(),
                KafkaEventConstants.AGGREGATE_ASSESSMENT,
                KafkaEventConstants.CAREER_ASSESSMENT_COMPLETED,
                KafkaEventConstants.EVENT_VERSION_V1,
                Map.of(
                        "assessmentId", assessment.getId(),
                        "employeeId", assessment.getEmployeeId(),
                        "targetPositionId", assessment.getTargetPositionId(),
                        "targetGradeId", assessment.getTargetGradeId(),
                        "requirementId", assessment.getRequirementId(),
                        "totalScore", assessment.getTotalScore(),
                        "mandatoryRequirementsMet", assessment.getMandatoryRequirementsMet()
                )
        );
    }

    @Transactional
    public void fail(AnalyticsJob job, String errorCode, String message) {
        jobService.fail(job.getId(), errorCode, message);

        if (job.getJobType() == AnalyticsJobType.TALENT_SCREENING) {
            screeningRepository.findAllByJobId(job.getId()).forEach(screening -> markScreeningFailed(job, screening, errorCode));
            return;
        }

        if (job.getJobType() == AnalyticsJobType.CAREER_ASSESSMENT) {
            outboxEventService.saveEvent(
                    job.getId(),
                    KafkaEventConstants.AGGREGATE_ASSESSMENT,
                    KafkaEventConstants.CAREER_ASSESSMENT_FAILED,
                    KafkaEventConstants.EVENT_VERSION_V1,
                    Map.of("jobId", job.getId(), "errorCode", errorCode)
            );
        }
    }

    private void markScreeningFailed(AnalyticsJob job, TalentScreening screening, String errorCode) {
        screening.setStatus(ScreeningStatus.FAILED);
        screening.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        outboxEventService.saveEvent(
                screening.getId(),
                KafkaEventConstants.AGGREGATE_SCREENING,
                KafkaEventConstants.TALENT_RESERVE_SCREENING_FAILED,
                KafkaEventConstants.EVENT_VERSION_V1,
                Map.of(
                        "screeningId", screening.getId(),
                        "jobId", job.getId(),
                        "reserveId", screening.getReserveId(),
                        "errorCode", errorCode
                )
        );
    }
}
