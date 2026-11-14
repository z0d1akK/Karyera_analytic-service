package com.zodiakk.analyticservice.assessment.service;

import com.zodiakk.analyticservice.assessment.dto.response.AssessmentSkillGapResponseDto;
import com.zodiakk.analyticservice.assessment.dto.response.CareerAssessmentResponseDto;
import com.zodiakk.analyticservice.assessment.entity.AssessmentComponent;
import com.zodiakk.analyticservice.assessment.entity.AssessmentSkillGap;
import com.zodiakk.analyticservice.assessment.entity.CareerAssessment;
import com.zodiakk.analyticservice.assessment.repository.AssessmentComponentRepository;
import com.zodiakk.analyticservice.assessment.repository.AssessmentSkillGapRepository;
import com.zodiakk.analyticservice.assessment.repository.CareerAssessmentRepository;
import com.zodiakk.analyticservice.common.exception.AssessmentNotFoundException;
import com.zodiakk.analyticservice.recommendation.dto.response.AiRecommendationResponseDto;
import com.zodiakk.analyticservice.recommendation.entity.AiRecommendation;
import com.zodiakk.analyticservice.recommendation.repository.AiRecommendationRepository;
import com.zodiakk.analyticservice.security.access.AnalyticsAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssessmentQueryService {

    private final CareerAssessmentRepository assessmentRepository;

    private final AssessmentSkillGapRepository skillGapRepository;

    private final AssessmentComponentRepository componentRepository;

    private final AnalyticsAccessService accessService;

    private final AiRecommendationRepository recommendationRepository;

    public CareerAssessmentResponseDto get(UUID assessmentId) {
        CareerAssessment assessment = required(assessmentId);
        accessService.requireSelfOrManager(assessment.getEmployeeId());
        return toDto(assessment);
    }

    public CareerAssessmentResponseDto getInternal(UUID assessmentId) {
        return toDto(required(assessmentId));
    }

    public List<CareerAssessmentResponseDto> history(UUID employeeId) {
        accessService.requireSelfOrManager(employeeId);
        return assessmentRepository.findAllByEmployeeIdOrderByCalculatedAtDesc(employeeId).stream()
                .map(this::toDto)
                .toList();
    }

    public List<AssessmentSkillGapResponseDto> skillGaps(UUID assessmentId) {
        CareerAssessment assessment = required(assessmentId);
        accessService.requireSelfOrManager(assessment.getEmployeeId());
        return skillGapRepository.findAllByAssessmentId(assessmentId).stream().map(this::toGap).toList();
    }

    private CareerAssessment required(UUID assessmentId) {
        return assessmentRepository.findById(assessmentId).orElseThrow(() -> new AssessmentNotFoundException(assessmentId));
    }

    private CareerAssessmentResponseDto toDto(CareerAssessment assessment) {
        List<AssessmentSkillGap> gaps = skillGapRepository.findAllByAssessmentId(assessment.getId());
        List<AssessmentComponent> components = componentRepository.findAllByAssessmentId(assessment.getId());

        return CareerAssessmentResponseDto.builder()
                .id(assessment.getId())
                .createdAt(assessment.getCreatedAt())
                .jobId(assessment.getJobId())
                .employeeId(assessment.getEmployeeId())
                .targetPositionId(assessment.getTargetPositionId())
                .targetGradeId(assessment.getTargetGradeId())
                .requirementId(assessment.getRequirementId())
                .requirementRevision(assessment.getRequirementRevision())
                .methodologyVersion(assessment.getMethodologyVersion())
                .skillScore(assessment.getSkillScore())
                .experienceScore(assessment.getExperienceScore())
                .totalScore(assessment.getTotalScore())
                .mandatoryRequirementsMet(assessment.getMandatoryRequirementsMet())
                .dataQualityStatus(assessment.getDataQualityStatus())
                .inputSnapshot(assessment.getInputSnapshot())
                .inputHash(assessment.getInputHash())
                .isStale(assessment.getIsStale())
                .calculatedAt(assessment.getCalculatedAt())
                .skillGaps(gaps.stream().map(this::toGap).toList())
                .components(components.stream().map(component -> {
                    var dto = new com.zodiakk.analyticservice.assessment.dto.response.AssessmentComponentResponseDto();
                    dto.setId(component.getId());
                    dto.setAssessmentId(component.getAssessmentId());
                    dto.setComponentCode(component.getComponentCode());
                    dto.setRawScore(component.getRawScore());
                    dto.setCoefficient(component.getCoefficient());
                    dto.setWeightedScore(component.getWeightedScore());
                    dto.setDetails(component.getDetails());
                    return dto;
                }).toList())
                .recommendations(recommendationRepository.findAllByAssessmentIdOrderByCreatedAtDesc(assessment.getId())
                        .stream()
                        .map(this::toRecommendation)
                        .toList())
                .build();
    }

    private AiRecommendationResponseDto toRecommendation(AiRecommendation recommendation) {
        return AiRecommendationResponseDto.builder()
                .id(recommendation.getId())
                .createdAt(recommendation.getCreatedAt())
                .assessmentId(recommendation.getAssessmentId())
                .status(recommendation.getStatus())
                .modelIdentifier(recommendation.getModelIdentifier())
                .promptVersion(recommendation.getPromptVersion())
                .content(recommendation.getContent())
                .errorCode(recommendation.getErrorCode())
                .generatedAt(recommendation.getGeneratedAt())
                .build();
    }

    private AssessmentSkillGapResponseDto toGap(AssessmentSkillGap gap) {
        return AssessmentSkillGapResponseDto.builder()
                .id(gap.getId())
                .assessmentId(gap.getAssessmentId())
                .skillId(gap.getSkillId())
                .actualLevel(gap.getActualLevel())
                .requiredLevel(gap.getRequiredLevel())
                .weight(gap.getWeight())
                .absoluteGap(gap.getAbsoluteGap())
                .normalizedGap(gap.getNormalizedGap())
                .priorityScore(gap.getPriorityScore())
                .mandatory(gap.getMandatory())
                .requirementMet(gap.getRequirementMet())
                .build();
    }
}
