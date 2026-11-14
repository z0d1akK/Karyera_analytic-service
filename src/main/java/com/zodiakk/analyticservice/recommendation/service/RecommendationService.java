package com.zodiakk.analyticservice.recommendation.service;

import com.zodiakk.analyticservice.assessment.entity.AssessmentSkillGap;
import com.zodiakk.analyticservice.assessment.entity.CareerAssessment;
import com.zodiakk.analyticservice.assessment.repository.AssessmentSkillGapRepository;
import com.zodiakk.analyticservice.assessment.repository.CareerAssessmentRepository;
import com.zodiakk.analyticservice.common.constants.KafkaEventConstants;
import com.zodiakk.analyticservice.common.enums.RecommendationStatus;
import com.zodiakk.analyticservice.common.exception.AssessmentNotFoundException;
import com.zodiakk.analyticservice.integration.client.EmployeeClient;
import com.zodiakk.analyticservice.integration.client.IntelligenceClient;
import com.zodiakk.analyticservice.integration.dto.CareerExplanationRequest;
import com.zodiakk.analyticservice.integration.dto.CareerExplanationResponse;
import com.zodiakk.analyticservice.integration.dto.DevelopmentRecommendationRequest;
import com.zodiakk.analyticservice.integration.dto.DevelopmentRecommendationResponse;
import com.zodiakk.analyticservice.integration.dto.IdsDto;
import com.zodiakk.analyticservice.integration.dto.ReferenceLookupDto;
import com.zodiakk.analyticservice.integration.dto.StoredRecommendation;
import com.zodiakk.analyticservice.integration.dto.CareerPathInterpretationRequest;
import com.zodiakk.analyticservice.integration.dto.CareerPathInterpretationResponse;
import com.zodiakk.analyticservice.recommendation.entity.AiRecommendation;
import com.zodiakk.analyticservice.recommendation.repository.AiRecommendationRepository;
import com.zodiakk.analyticservice.security.access.AnalyticsAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final CareerAssessmentRepository assessmentRepository;

    private final AssessmentSkillGapRepository skillGapRepository;

    private final AiRecommendationRepository recommendationRepository;

    private final IntelligenceClient intelligenceClient;

    private final EmployeeClient employeeClient;

    private final AnalyticsAccessService accessService;

    private final RecommendationFallback recommendationFallback;

    private final ObjectMapper objectMapper;

    @Transactional
    public AiRecommendation generate(UUID assessmentId) {
        CareerAssessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new AssessmentNotFoundException(assessmentId));
        accessService.requireSelfOrManager(assessment.getEmployeeId());
        return generateUnchecked(assessment);
    }

    @Transactional
    public AiRecommendation generateUnchecked(UUID assessmentId) {
        CareerAssessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new AssessmentNotFoundException(assessmentId));
        return generateUnchecked(assessment);
    }

    private AiRecommendation generateUnchecked(CareerAssessment assessment) {
        AiRecommendation recommendation = recommendationRepository.save(AiRecommendation.builder()
                .assessmentId(assessment.getId())
                .status(RecommendationStatus.RUNNING)
                .build());

        CareerExplanationRequest explanationRequest = explanationRequest(assessment);
        boolean usedFallback = false;
        CareerExplanationResponse explanation;
        try {
            explanation = intelligenceClient.explain(explanationRequest);
            if (explanation == null || explanation.summary() == null || explanation.summary().isBlank()) {
                explanation = recommendationFallback.explain(explanationRequest);
                usedFallback = true;
            }
        } catch (RuntimeException ex) {
            log.warn("Intelligence explanation failed for assessment {}, using template fallback", assessment.getId(), ex);
            explanation = recommendationFallback.explain(explanationRequest);
            usedFallback = true;
        }

        DevelopmentRecommendationResponse development = null;
        DevelopmentRecommendationRequest developmentRequest = developmentRequest(assessment);
        if (developmentRequest != null) {
            try {
                development = intelligenceClient.recommend(developmentRequest);
            } catch (RuntimeException ex) {
                log.warn("Intelligence recommendations failed for assessment {}", assessment.getId(), ex);
                development = null;
            }
            if (!hasRecommendationText(development)) {
                development = recommendationFallback.recommend(developmentRequest);
                usedFallback = true;
            }
        }

        RecommendationStatus status = usedFallback
                || "FALLBACK".equalsIgnoreCase(explanation.status())
                || (explanation.modelIdentifier() != null && explanation.modelIdentifier().contains("fallback"))
                ? RecommendationStatus.FALLBACK
                : RecommendationStatus.COMPLETED;
        recommendation.setStatus(status);
        recommendation.setModelIdentifier(explanation.modelIdentifier());
        recommendation.setPromptVersion(explanation.promptVersion() == null ? null : explanation.promptVersion().toString());
        recommendation.setGeneratedAt(OffsetDateTime.now(ZoneOffset.UTC));
        try {
            recommendation.setContent(objectMapper.writeValueAsString(new StoredRecommendation(explanation, development)));
        } catch (Exception ex) {
            recommendation.setStatus(RecommendationStatus.FAILED);
            recommendation.setErrorCode(KafkaEventConstants.ERROR_INVALID_EXPLANATION);
        }
        return recommendation;
    }

    @Transactional(readOnly = true)
    public List<AiRecommendation> list(UUID assessmentId) {
        CareerAssessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new AssessmentNotFoundException(assessmentId));
        accessService.requireSelfOrManager(assessment.getEmployeeId());
        return recommendationRepository.findAllByAssessmentIdOrderByCreatedAtDesc(assessmentId);
    }

    public String interpretCareerOptions(
            UUID employeeId,
            String currentPositionName,
            String currentGradeName,
            List<CareerAssessment> assessments
    ) {
        if (assessments == null || assessments.isEmpty()) {
            return null;
        }

        List<UUID> skillIds = assessments.stream()
                .flatMap(assessment -> skillGapRepository.findAllByAssessmentId(assessment.getId()).stream())
                .map(AssessmentSkillGap::getSkillId)
                .distinct()
                .toList();
        Map<UUID, String> skillNames = skillNames(skillIds);
        Map<UUID, String> positionNames = catalogNames(safeLookup(() -> employeeClient.positions(new IdsDto(
                assessments.stream().map(CareerAssessment::getTargetPositionId).distinct().toList()
        ))));
        Map<UUID, String> gradeNames = catalogNames(safeLookup(() -> employeeClient.grades(new IdsDto(
                assessments.stream().map(CareerAssessment::getTargetGradeId).distinct().toList()
        ))));

        CareerPathInterpretationResponse response = intelligenceClient.interpret(new CareerPathInterpretationRequest(
                UUID.randomUUID(),
                employeeId,
                "ru",
                currentPositionName,
                currentGradeName,
                assessments.stream().map(assessment -> {
                    List<AssessmentSkillGap> gaps = skillGapRepository.findAllByAssessmentId(assessment.getId());
                    return new CareerPathInterpretationRequest.Direction(
                            assessment.getTargetPositionId(),
                            positionNames.getOrDefault(assessment.getTargetPositionId(), "Цель"),
                            gradeNames.get(assessment.getTargetGradeId()),
                            assessment.getTotalScore(),
                            assessment.getMandatoryRequirementsMet(),
                            gaps.stream().filter(gap -> Boolean.TRUE.equals(gap.getRequirementMet()))
                                    .map(gap -> strength(gap, skillNames)).toList(),
                            gaps.stream().filter(gap -> !Boolean.TRUE.equals(gap.getRequirementMet()))
                                    .map(gap -> gap(gap, skillNames)).toList()
                    );
                }).toList()
        ));
        return response == null ? null : response.summary();
    }

    private boolean hasRecommendationText(DevelopmentRecommendationResponse development) {
        if (development == null || development.recommendations() == null) {
            return false;
        }
        return development.recommendations().stream().anyMatch(item ->
                (item.title() != null && !item.title().isBlank())
                        || (item.description() != null && !item.description().isBlank()));
    }

    private CareerExplanationRequest.SkillStrength strength(AssessmentSkillGap gap, Map<UUID, String> skillNames) {
        return new CareerExplanationRequest.SkillStrength(
                gap.getSkillId(),
                skillNames.getOrDefault(gap.getSkillId(), gap.getSkillId().toString()),
                gap.getActualLevel(),
                gap.getRequiredLevel()
        );
    }

    private CareerExplanationRequest.SkillGap gap(AssessmentSkillGap gap, Map<UUID, String> skillNames) {
        return new CareerExplanationRequest.SkillGap(
                gap.getSkillId(),
                skillNames.getOrDefault(gap.getSkillId(), gap.getSkillId().toString()),
                gap.getActualLevel(),
                gap.getRequiredLevel(),
                gap.getPriorityScore(),
                gap.getMandatory()
        );
    }

    private Map<UUID, String> catalogNames(ReferenceLookupDto lookup) {
        if (lookup == null || lookup.items() == null) {
            return Map.of();
        }
        return lookup.items().stream()
                .filter(item -> item.id() != null && item.name() != null)
                .collect(Collectors.toMap(ReferenceLookupDto.Item::id, ReferenceLookupDto.Item::name, (left, right) -> left));
    }

    private CareerExplanationRequest explanationRequest(CareerAssessment assessment) {
        String positionName = name(safeLookup(() -> employeeClient.positions(new IdsDto(List.of(assessment.getTargetPositionId())))));
        String gradeName = name(safeLookup(() -> employeeClient.grades(new IdsDto(List.of(assessment.getTargetGradeId())))));
        List<AssessmentSkillGap> gaps = skillGapRepository.findAllByAssessmentId(assessment.getId());
        Map<UUID, String> skillNames = skillNames(gaps.stream().map(AssessmentSkillGap::getSkillId).toList());

        return new CareerExplanationRequest(
                UUID.randomUUID(),
                assessment.getId(),
                "ru",
                new CareerExplanationRequest.Target(positionName, gradeName),
                new CareerExplanationRequest.Scores(
                        assessment.getSkillScore(),
                        assessment.getExperienceScore() == null ? BigDecimal.ZERO : assessment.getExperienceScore(),
                        assessment.getTotalScore()
                ),
                assessment.getMandatoryRequirementsMet(),
                assessment.getDataQualityStatus().name(),
                gaps.stream()
                        .filter(gap -> Boolean.TRUE.equals(gap.getRequirementMet()))
                        .map(gap -> strength(gap, skillNames))
                        .toList(),
                gaps.stream()
                        .filter(gap -> !Boolean.TRUE.equals(gap.getRequirementMet()))
                        .map(gap -> gap(gap, skillNames))
                        .toList()
        );
    }

    private DevelopmentRecommendationRequest developmentRequest(CareerAssessment assessment) {
        List<AssessmentSkillGap> gaps = skillGapRepository.findAllByAssessmentId(assessment.getId()).stream()
                .filter(gap -> !Boolean.TRUE.equals(gap.getRequirementMet()))
                .toList();
        if (gaps.isEmpty()) {
            return null;
        }
        Map<UUID, String> skillNames = skillNames(gaps.stream().map(AssessmentSkillGap::getSkillId).toList());
        String positionName = name(safeLookup(() -> employeeClient.positions(new IdsDto(List.of(assessment.getTargetPositionId())))));

        return new DevelopmentRecommendationRequest(
                UUID.randomUUID(),
                assessment.getId(),
                "ru",
                positionName,
                gaps.stream()
                        .map(gap -> new DevelopmentRecommendationRequest.Area(
                                gap.getSkillId(),
                                skillNames.getOrDefault(gap.getSkillId(), gap.getSkillId().toString()),
                                gap.getActualLevel(),
                                gap.getRequiredLevel(),
                                gap.getPriorityScore()
                        ))
                        .toList(),
                new DevelopmentRecommendationRequest.Preferences(5, true, true)
        );
    }

    private Map<UUID, String> skillNames(List<UUID> skillIds) {
        ReferenceLookupDto lookup = safeLookup(() -> employeeClient.skills(new IdsDto(skillIds)));
        if (lookup == null || lookup.items() == null) {
            return Map.of();
        }
        return lookup.items().stream()
                .filter(item -> item.id() != null && item.name() != null)
                .collect(Collectors.toMap(ReferenceLookupDto.Item::id, ReferenceLookupDto.Item::name, (left, right) -> left));
    }

    private ReferenceLookupDto safeLookup(java.util.function.Supplier<ReferenceLookupDto> call) {
        try {
            return call.get();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String name(ReferenceLookupDto lookup) {
        if (lookup == null || lookup.items() == null || lookup.items().isEmpty() || lookup.items().getFirst().name() == null) {
            return "Цель";
        }

        return lookup.items().getFirst().name();
    }
}
