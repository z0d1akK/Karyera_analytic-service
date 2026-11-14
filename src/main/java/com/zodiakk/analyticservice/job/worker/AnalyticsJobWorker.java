package com.zodiakk.analyticservice.job.worker;

import com.zodiakk.analyticservice.assessment.entity.CareerAssessment;
import com.zodiakk.analyticservice.assessment.service.AssessmentEngine;
import com.zodiakk.analyticservice.common.constants.KafkaEventConstants;
import com.zodiakk.analyticservice.common.exception.InsufficientDataException;
import com.zodiakk.analyticservice.assessment.repository.AssessmentSkillGapRepository;
import com.zodiakk.analyticservice.integration.client.CareerClient;
import com.zodiakk.analyticservice.integration.dto.AvailableTargetsDto;
import com.zodiakk.analyticservice.integration.dto.CareerProfileDto;
import com.zodiakk.analyticservice.recommendation.service.RecommendationService;
import com.zodiakk.analyticservice.integration.dto.RequirementDto;
import com.zodiakk.analyticservice.integration.support.IntegrationCalls;
import com.zodiakk.analyticservice.job.dto.AssessmentJobParameters;
import com.zodiakk.analyticservice.job.dto.CareerOptionsJobParameters;
import com.zodiakk.analyticservice.job.entity.AnalyticsJob;
import com.zodiakk.analyticservice.job.service.AnalyticsJobService;
import com.zodiakk.analyticservice.job.service.JobCompletionService;
import com.zodiakk.analyticservice.projection.service.ProfileProjectionService;
import com.zodiakk.analyticservice.recommendation.service.RecommendationService;
import com.zodiakk.analyticservice.screening.service.ScreeningProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsJobWorker {

    private final AnalyticsJobService jobService;

    private final JobCompletionService completionService;

    private final AssessmentEngine assessmentEngine;

    private final ScreeningProcessor screeningProcessor;

    private final RecommendationService recommendationService;

    private final ProfileProjectionService profileProjectionService;

    private final CareerClient careerClient;

    private final AssessmentSkillGapRepository skillGapRepository;

    @Scheduled(fixedDelayString = "${app.jobs.poll-delay-ms}")
    public void poll() {
        for (AnalyticsJob job : jobService.claim()) {
            try {
                switch (job.getJobType()) {
                    case CAREER_ASSESSMENT -> runAssessment(job);
                    case TALENT_SCREENING -> screeningProcessor.process(job);
                    case CAREER_RECOMMENDATIONS -> runCareerOptions(job);
                    case PROFILE_REFRESH -> profileProjectionService.refreshFromJob(job);
                }
            } catch (InsufficientDataException ex) {
                completionService.fail(job, KafkaEventConstants.ERROR_INSUFFICIENT_DATA, ex.getMessage());
            } catch (RuntimeException ex) {
                log.warn("Analytics job {} will be retried", job.getId(), ex);
                jobService.retry(job.getId(), "Временная ошибка выполнения задания");
            }
        }
    }

    private void runAssessment(AnalyticsJob job) {
        AssessmentJobParameters parameters = jobService.read(job.getParameters(), AssessmentJobParameters.class);
        CareerAssessment assessment = assessmentEngine.executeForExistingJob(
                job,
                parameters.employeeId(),
                parameters.targetPositionId(),
                parameters.targetGradeId(),
                parameters.requirementId()
        );
        completionService.completeAssessment(job, assessment);

        if (parameters.generateRecommendations()) {
            recommendationService.generateUnchecked(assessment.getId());
        }
    }

    private void runCareerOptions(AnalyticsJob job) {
        CareerOptionsJobParameters parameters = jobService.read(job.getParameters(), CareerOptionsJobParameters.class);
        AvailableTargetsDto targets = IntegrationCalls.required(
                () -> careerClient.availableTargets(parameters.trackId(), parameters.employeeId()),
                "Карьерный трек не найден"
        );
        CareerProfileDto profile = assessmentEngine.loadProfile(parameters.employeeId());
        int limit = Math.max(parameters.maxOptions(), 1);
        List<AvailableTargetsDto.Target> selected = targets.targets() == null
                ? List.of()
                : targets.targets().stream().limit(limit).toList();
        List<CareerOptionResult> options = new ArrayList<>();

        List<CareerAssessment> assessed = new ArrayList<>();

        for (AvailableTargetsDto.Target target : selected) {
            RequirementDto requirement;
            try {
                requirement = assessmentEngine.loadRequirement(target.positionId(), target.gradeId(), null);
            } catch (InsufficientDataException ex) {
                log.info("Skip career option without published requirement: {} / {}", target.positionId(), target.gradeId());
                continue;
            }
            CareerAssessment assessment = assessmentEngine.executeAsChildJob(
                    job.getRequestedBy(),
                    parameters.employeeId(),
                    target.positionId(),
                    target.gradeId(),
                    requirement,
                    profile,
                    null
            );
            assessed.add(assessment);
            long missing = skillGapRepository.countByAssessmentIdAndRequirementMetFalse(assessment.getId());
            options.add(new CareerOptionResult(
                    target.positionId(),
                    target.gradeId(),
                    assessment.getId(),
                    assessment.getSkillScore(),
                    assessment.getTotalScore(),
                    Boolean.TRUE.equals(assessment.getMandatoryRequirementsMet()),
                    missing
            ));
        }
        if (options.isEmpty()) {
            throw new InsufficientDataException("Нет опубликованных требований для карьерных направлений");
        }

        options.sort(Comparator.comparing(CareerOptionResult::totalScore).reversed());
        String interpretation = null;
        try {
            interpretation = recommendationService.interpretCareerOptions(
                    parameters.employeeId(),
                    profile.positionName(),
                    profile.gradeName(),
                    assessed
            );
        } catch (RuntimeException ignored) {
            interpretation = null;
        }
        jobService.complete(job.getId(), null, new CareerOptionsResult(parameters.employeeId(), options, interpretation));
    }

    public record CareerOptionsResult(UUID employeeId, List<CareerOptionResult> options, String interpretationSummary) {
    }

    public record CareerOptionResult(
            UUID targetPositionId,
            UUID targetGradeId,
            UUID assessmentId,
            java.math.BigDecimal skillScore,
            java.math.BigDecimal totalScore,
            boolean mandatoryRequirementsMet,
            long missingSkillsCount
    ) {
    }
}
