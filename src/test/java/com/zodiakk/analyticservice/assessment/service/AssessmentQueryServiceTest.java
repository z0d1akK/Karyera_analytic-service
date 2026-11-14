package com.zodiakk.analyticservice.assessment.service;

import com.zodiakk.analyticservice.assessment.entity.CareerAssessment;
import com.zodiakk.analyticservice.assessment.repository.AssessmentComponentRepository;
import com.zodiakk.analyticservice.assessment.repository.AssessmentSkillGapRepository;
import com.zodiakk.analyticservice.assessment.repository.CareerAssessmentRepository;
import com.zodiakk.analyticservice.common.enums.DataQualityStatus;
import com.zodiakk.analyticservice.common.exception.AssessmentNotFoundException;
import com.zodiakk.analyticservice.recommendation.repository.AiRecommendationRepository;
import com.zodiakk.analyticservice.security.access.AnalyticsAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentQueryServiceTest {

    @Mock
    private CareerAssessmentRepository assessmentRepository;

    @Mock
    private AssessmentSkillGapRepository skillGapRepository;

    @Mock
    private AssessmentComponentRepository componentRepository;

    @Mock
    private AnalyticsAccessService accessService;

    @Mock
    private AiRecommendationRepository recommendationRepository;

    @InjectMocks
    private AssessmentQueryService service;

    @Test
    void mapsInternalAssessmentAndThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(assessmentRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(AssessmentNotFoundException.class, () -> service.getInternal(id));

        CareerAssessment assessment = CareerAssessment.builder()
                .id(id)
                .employeeId(UUID.randomUUID())
                .skillScore(BigDecimal.ONE)
                .totalScore(BigDecimal.ONE)
                .dataQualityStatus(DataQualityStatus.COMPLETE)
                .mandatoryRequirementsMet(true)
                .calculatedAt(OffsetDateTime.now())
                .build();
        when(assessmentRepository.findById(id)).thenReturn(Optional.of(assessment));
        when(skillGapRepository.findAllByAssessmentId(id)).thenReturn(List.of());
        when(componentRepository.findAllByAssessmentId(id)).thenReturn(List.of());
        when(recommendationRepository.findAllByAssessmentIdOrderByCreatedAtDesc(id)).thenReturn(List.of());

        assertEquals(id, service.getInternal(id).getId());
        assertEquals(id, service.get(id).getId());
        assertEquals(0, service.skillGaps(id).size());
        when(assessmentRepository.findAllByEmployeeIdOrderByCalculatedAtDesc(assessment.getEmployeeId()))
                .thenReturn(List.of(assessment));
        assertEquals(1, service.history(assessment.getEmployeeId()).size());
    }
}
