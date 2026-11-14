package com.zodiakk.analyticservice.recommendation.service;

import com.zodiakk.analyticservice.assessment.entity.AssessmentSkillGap;
import com.zodiakk.analyticservice.assessment.entity.CareerAssessment;
import com.zodiakk.analyticservice.assessment.repository.AssessmentSkillGapRepository;
import com.zodiakk.analyticservice.assessment.repository.CareerAssessmentRepository;
import com.zodiakk.analyticservice.common.enums.DataQualityStatus;
import com.zodiakk.analyticservice.common.enums.RecommendationStatus;
import com.zodiakk.analyticservice.common.exception.AssessmentNotFoundException;
import com.zodiakk.analyticservice.integration.client.EmployeeClient;
import com.zodiakk.analyticservice.integration.client.IntelligenceClient;
import com.zodiakk.analyticservice.integration.dto.CareerExplanationResponse;
import com.zodiakk.analyticservice.integration.dto.CareerPathInterpretationResponse;
import com.zodiakk.analyticservice.integration.dto.DevelopmentRecommendationResponse;
import com.zodiakk.analyticservice.integration.dto.ReferenceLookupDto;
import com.zodiakk.analyticservice.recommendation.entity.AiRecommendation;
import com.zodiakk.analyticservice.recommendation.repository.AiRecommendationRepository;
import com.zodiakk.analyticservice.security.access.AnalyticsAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private CareerAssessmentRepository assessmentRepository;
    @Mock
    private AssessmentSkillGapRepository skillGapRepository;
    @Mock
    private AiRecommendationRepository recommendationRepository;
    @Mock
    private IntelligenceClient intelligenceClient;
    @Mock
    private EmployeeClient employeeClient;
    @Mock
    private AnalyticsAccessService accessService;
    @Spy
    private RecommendationFallback recommendationFallback = new RecommendationFallback();
    @Spy
    private JsonMapper objectMapper = new JsonMapper();

    @InjectMocks
    private RecommendationService service;

    @Test
    void generatesCompletedRecommendationAndListsHistory() {
        CareerAssessment assessment = assessment();
        UUID skillId = UUID.randomUUID();
        when(assessmentRepository.findById(assessment.getId())).thenReturn(Optional.of(assessment));
        when(recommendationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(skillGapRepository.findAllByAssessmentId(assessment.getId())).thenReturn(List.of(
                AssessmentSkillGap.builder()
                        .assessmentId(assessment.getId())
                        .skillId(skillId)
                        .actualLevel(1)
                        .requiredLevel(3)
                        .requirementMet(false)
                        .build(),
                AssessmentSkillGap.builder()
                        .assessmentId(assessment.getId())
                        .skillId(UUID.randomUUID())
                        .actualLevel(4)
                        .requiredLevel(4)
                        .requirementMet(true)
                        .build()
        ));
        when(employeeClient.positions(any())).thenReturn(new ReferenceLookupDto(List.of(
                new ReferenceLookupDto.Item(assessment.getTargetPositionId(), "Lead", true)
        )));
        when(employeeClient.grades(any())).thenReturn(new ReferenceLookupDto(List.of(
                new ReferenceLookupDto.Item(assessment.getTargetGradeId(), "Senior", true)
        )));
        when(employeeClient.skills(any())).thenReturn(new ReferenceLookupDto(List.of(
                new ReferenceLookupDto.Item(skillId, "Kafka", true)
        )));
        when(intelligenceClient.explain(any())).thenReturn(new CareerExplanationResponse(
                UUID.randomUUID(), assessment.getId(), "Модель объяснила", List.of(), List.of(), List.of(),
                "qwen2.5:14b", 1, "COMPLETED"
        ));
        when(intelligenceClient.recommend(any())).thenReturn(new DevelopmentRecommendationResponse(
                UUID.randomUUID(),
                List.of(new DevelopmentRecommendationResponse.Item(skillId, "SELF_STUDY", "Kafka", "практика", List.of(), null))
        ));

        AiRecommendation recommendation = service.generate(assessment.getId());
        assertEquals(RecommendationStatus.COMPLETED, recommendation.getStatus());
        assertTrue(recommendation.getContent().contains("Модель объяснила"));

        when(recommendationRepository.findAllByAssessmentIdOrderByCreatedAtDesc(assessment.getId()))
                .thenReturn(List.of(recommendation));
        assertEquals(1, service.list(assessment.getId()).size());
    }

    @Test
    void fallsBackWhenIntelligenceFailsAndInterpretsCareerOptions() {
        CareerAssessment assessment = assessment();
        when(assessmentRepository.findById(assessment.getId())).thenReturn(Optional.of(assessment));
        when(recommendationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(skillGapRepository.findAllByAssessmentId(assessment.getId())).thenReturn(List.of());
        when(employeeClient.positions(any())).thenThrow(new IllegalStateException("down"));
        when(intelligenceClient.explain(any())).thenThrow(new IllegalStateException("down"));

        AiRecommendation recommendation = service.generateUnchecked(assessment.getId());
        assertEquals(RecommendationStatus.FALLBACK, recommendation.getStatus());

        when(intelligenceClient.interpret(any())).thenReturn(new CareerPathInterpretationResponse(
                UUID.randomUUID(), "Сравнение", List.of(), "Lead", "меньше пробелов", List.of(), "qwen", 1
        ));
        assertEquals("Сравнение", service.interpretCareerOptions(
                assessment.getEmployeeId(), "Dev", "Middle", List.of(assessment)
        ));
        assertNull(service.interpretCareerOptions(assessment.getEmployeeId(), "Dev", "Middle", List.of()));
        assertThrows(AssessmentNotFoundException.class, () -> service.generate(UUID.randomUUID()));
    }

    private CareerAssessment assessment() {
        return CareerAssessment.builder()
                .id(UUID.randomUUID())
                .employeeId(UUID.randomUUID())
                .targetPositionId(UUID.randomUUID())
                .targetGradeId(UUID.randomUUID())
                .skillScore(new BigDecimal("0.500000"))
                .experienceScore(null)
                .totalScore(new BigDecimal("0.450000"))
                .mandatoryRequirementsMet(true)
                .dataQualityStatus(DataQualityStatus.COMPLETE)
                .build();
    }
}
