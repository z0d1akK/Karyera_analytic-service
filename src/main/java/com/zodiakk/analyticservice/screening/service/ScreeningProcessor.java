package com.zodiakk.analyticservice.screening.service;

import com.zodiakk.analyticservice.assessment.entity.CareerAssessment;
import com.zodiakk.analyticservice.assessment.service.AssessmentEngine;
import com.zodiakk.analyticservice.common.constants.KafkaEventConstants;
import com.zodiakk.analyticservice.common.enums.ScreeningEligibilityStatus;
import com.zodiakk.analyticservice.common.enums.ScreeningStatus;
import com.zodiakk.analyticservice.common.exception.InsufficientDataException;
import com.zodiakk.analyticservice.common.exception.TransientIntegrationException;
import com.zodiakk.analyticservice.integration.client.CareerClient;
import com.zodiakk.analyticservice.integration.client.EmployeeClient;
import com.zodiakk.analyticservice.integration.client.IntelligenceClient;
import com.zodiakk.analyticservice.integration.dto.CareerProfileDto;
import com.zodiakk.analyticservice.integration.dto.IdListDto;
import com.zodiakk.analyticservice.integration.dto.IdsDto;
import com.zodiakk.analyticservice.integration.dto.ReferenceLookupDto;
import com.zodiakk.analyticservice.integration.dto.RequirementDto;
import com.zodiakk.analyticservice.integration.dto.ReserveContextDto;
import com.zodiakk.analyticservice.integration.dto.TalentScreeningExplanationRequest;
import com.zodiakk.analyticservice.integration.dto.TalentScreeningExplanationResponse;
import com.zodiakk.analyticservice.integration.support.IntegrationCalls;
import com.zodiakk.analyticservice.job.dto.ScreeningJobParameters;
import com.zodiakk.analyticservice.job.entity.AnalyticsJob;
import com.zodiakk.analyticservice.job.service.AnalyticsJobService;
import com.zodiakk.analyticservice.outbox.service.OutboxEventService;
import com.zodiakk.analyticservice.screening.entity.ScreeningResult;
import com.zodiakk.analyticservice.screening.entity.TalentScreening;
import com.zodiakk.analyticservice.screening.repository.ScreeningResultRepository;
import com.zodiakk.analyticservice.screening.repository.TalentScreeningRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ScreeningProcessor {

    private final AssessmentEngine assessmentEngine;
    private final CareerClient careerClient;
    private final TalentScreeningRepository screeningRepository;
    private final ScreeningResultRepository resultRepository;
    private final AnalyticsJobService jobService;
    private final OutboxEventService outboxEventService;
    private final IntelligenceClient intelligenceClient;
    private final EmployeeClient employeeClient;
    private final ScreeningProcessor self;

    public ScreeningProcessor(
            AssessmentEngine assessmentEngine,
            CareerClient careerClient,
            TalentScreeningRepository screeningRepository,
            ScreeningResultRepository resultRepository,
            AnalyticsJobService jobService,
            OutboxEventService outboxEventService,
            IntelligenceClient intelligenceClient,
            EmployeeClient employeeClient,
            @Lazy ScreeningProcessor self
    ) {
        this.assessmentEngine = assessmentEngine;
        this.careerClient = careerClient;
        this.screeningRepository = screeningRepository;
        this.resultRepository = resultRepository;
        this.jobService = jobService;
        this.outboxEventService = outboxEventService;
        this.intelligenceClient = intelligenceClient;
        this.employeeClient = employeeClient;
        this.self = self;
    }

    public void process(AnalyticsJob job) {
        ScreeningJobParameters parameters = jobService.read(job.getParameters(), ScreeningJobParameters.class);
        ReserveContextDto reserve = IntegrationCalls.required(
                () -> careerClient.reserveContext(parameters.reserveId()),
                "Кадровый резерв не найден"
        );

        if (!parameters.targetPositionId().equals(reserve.targetPositionId())
                || !parameters.targetGradeId().equals(reserve.targetGradeId())) {
            throw new InsufficientDataException("Цель скрининга не совпадает с кадровым резервом");
        }

        RequirementDto requirement = assessmentEngine.loadRequirement(
                parameters.targetPositionId(),
                parameters.targetGradeId(),
                parameters.requirementId()
        );
        TalentScreening screening = self.ensureScreening(job, parameters, requirement);
        List<UUID> candidates = parameters.candidateIds() == null ? List.of() : parameters.candidateIds();

        for (UUID employeeId : candidates) {
            if (resultRepository.findByScreeningIdAndEmployeeId(screening.getId(), employeeId).isPresent()) {
                continue;
            }

            try {
                CareerProfileDto profile = assessmentEngine.loadProfile(employeeId);
                assessmentEngine.executeAsChildJob(
                        job.getRequestedBy(),
                        employeeId,
                        parameters.targetPositionId(),
                        parameters.targetGradeId(),
                        requirement,
                        profile,
                        screening.getId()
                );
            } catch (InsufficientDataException | TransientIntegrationException ex) {
                self.saveUnscored(screening.getId(), employeeId, ScreeningEligibilityStatus.INSUFFICIENT_DATA, ex.getMessage());
            } catch (RuntimeException ex) {
                self.saveUnscored(screening.getId(), employeeId, ScreeningEligibilityStatus.ASSESSMENT_FAILED, "ASSESSMENT_FAILED");
            }
            self.tickProgress(screening.getId(), candidates.size());
        }

        self.finish(job, screening);
    }

    @Transactional
    public TalentScreening ensureScreening(
            AnalyticsJob job,
            ScreeningJobParameters parameters,
            RequirementDto requirement
    ) {
        List<TalentScreening> existing = screeningRepository.findAllByJobId(job.getId());

        if (!existing.isEmpty()) {
            return existing.getFirst();
        }

        return screeningRepository.save(TalentScreening.builder()
                .jobId(job.getId())
                .reserveId(parameters.reserveId())
                .targetPositionId(parameters.targetPositionId())
                .targetGradeId(parameters.targetGradeId())
                .requirementId(requirement.requirementId())
                .requirementRevision(requirement.revision() == null ? 0 : requirement.revision())
                .methodologyVersion(requirement.revision() == null ? "1.0" : "1.0")
                .status(ScreeningStatus.RUNNING)
                .candidateCount(parameters.candidateIds() == null ? 0 : parameters.candidateIds().size())
                .evaluatedCount(0)
                .failedCount(0)
                .build());
    }

    @Transactional
    public void saveUnscored(UUID screeningId, UUID employeeId, ScreeningEligibilityStatus status, String reason) {
        if (resultRepository.findByScreeningIdAndEmployeeId(screeningId, employeeId).isPresent()) {
            return;
        }

        resultRepository.save(ScreeningResult.builder()
                .screeningId(screeningId)
                .employeeId(employeeId)
                .eligibilityStatus(status)
                .exclusionReason(reason == null ? status.name() : reason.substring(0, Math.min(100, reason.length())))
                .build());
    }

    @Transactional
    public void finish(AnalyticsJob job, TalentScreening screening) {
        TalentScreening current = screeningRepository.findById(screening.getId()).orElseThrow();
        List<ScreeningResult> results = resultRepository.findAllByScreeningIdOrderByRankAsc(current.getId());
        List<ScreeningResult> ranked = results.stream()
                .filter(result -> result.getScore() != null)
                .filter(result -> result.getEligibilityStatus() == ScreeningEligibilityStatus.ELIGIBLE
                        || result.getEligibilityStatus() == ScreeningEligibilityStatus.GAPS_PRESENT)
                .sorted(Comparator.comparing(ScreeningResult::getScore).reversed()
                        .thenComparing(ScreeningResult::getEmployeeId))
                .toList();

        int rank = 1;

        for (ScreeningResult result : ranked) {
            result.setRank(rank++);
        }

        int failed = (int) results.stream()
                .filter(result -> result.getEligibilityStatus() == ScreeningEligibilityStatus.INSUFFICIENT_DATA
                        || result.getEligibilityStatus() == ScreeningEligibilityStatus.ASSESSMENT_FAILED)
                .count();
        current.setEvaluatedCount(results.size() - failed);
        current.setFailedCount(failed);
        current.setStatus(ScreeningStatus.COMPLETED);
        current.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        current.setExplanationText(explain(current, ranked));

        outboxEventService.saveEvent(
                current.getId(),
                KafkaEventConstants.AGGREGATE_SCREENING,
                KafkaEventConstants.TALENT_RESERVE_SCREENING_COMPLETED,
                KafkaEventConstants.EVENT_VERSION_V1,
                Map.of(
                        "screeningId", current.getId(),
                        "jobId", job.getId(),
                        "reserveId", current.getReserveId(),
                        "candidateCount", current.getCandidateCount(),
                        "evaluatedCount", current.getEvaluatedCount(),
                        "failedCount", current.getFailedCount()
                )
        );
        jobService.complete(job.getId(), current.getId(), Map.of("screeningId", current.getId()));
    }

    @Transactional
    public void tickProgress(UUID screeningId, int total) {
        TalentScreening current = screeningRepository.findById(screeningId).orElseThrow();
        int done = (int) resultRepository.findAllByScreeningIdOrderByRankAsc(screeningId).size();
        current.setEvaluatedCount(Math.min(done, total));
    }

    private String explain(TalentScreening screening, List<ScreeningResult> ranked) {
        try {
            List<ScreeningResult> top = ranked.stream().limit(5).toList();
            Map<UUID, String> names = Map.of();
            try {
                var lookup = employeeClient.careerProfiles(new IdListDto(top.stream().map(ScreeningResult::getEmployeeId).toList()));
                if (lookup != null && lookup.profiles() != null) {
                    names = lookup.profiles().stream().collect(java.util.stream.Collectors.toMap(
                            CareerProfileDto::employeeId,
                            profile -> profile.fullName() == null || profile.fullName().isBlank()
                                    ? "кандидат"
                                    : profile.fullName(),
                            (left, right) -> left
                    ));
                }
            } catch (RuntimeException ignored) {
                names = Map.of();
            }
            ReferenceLookupDto positions = null;
            ReferenceLookupDto grades = null;
            try {
                positions = employeeClient.positions(new IdsDto(List.of(screening.getTargetPositionId())));
                grades = employeeClient.grades(new IdsDto(List.of(screening.getTargetGradeId())));
            } catch (RuntimeException ignored) {
                // names stay empty
            }
            Map<UUID, String> finalNames = names;
            TalentScreeningExplanationResponse response = intelligenceClient.explainScreening(new TalentScreeningExplanationRequest(
                    UUID.randomUUID(),
                    screening.getReserveId(),
                    "ru",
                    nameOf(positions, screening.getTargetPositionId()),
                    nameOf(grades, screening.getTargetGradeId()),
                    top.stream().map(result -> new TalentScreeningExplanationRequest.Candidate(
                            result.getEmployeeId(),
                            finalNames.getOrDefault(result.getEmployeeId(), "кандидат"),
                            result.getRank(),
                            result.getScore(),
                            result.getEligibilityStatus() == null ? null : result.getEligibilityStatus().name(),
                            result.getMandatoryRequirementsMet()
                    )).toList()
            ));
            return response == null ? null : response.summary();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String nameOf(ReferenceLookupDto lookup, UUID id) {
        if (lookup == null || lookup.items() == null) {
            return "не указано";
        }
        return lookup.items().stream()
                .filter(item -> id.equals(item.id()))
                .map(ReferenceLookupDto.Item::name)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse("не указано");
    }
}
