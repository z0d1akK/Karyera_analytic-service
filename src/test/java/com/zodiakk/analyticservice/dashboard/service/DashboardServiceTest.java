package com.zodiakk.analyticservice.dashboard.service;

import com.zodiakk.analyticservice.assessment.entity.AssessmentSkillGap;
import com.zodiakk.analyticservice.assessment.entity.CareerAssessment;
import com.zodiakk.analyticservice.assessment.repository.AssessmentSkillGapRepository;
import com.zodiakk.analyticservice.assessment.repository.CareerAssessmentRepository;
import com.zodiakk.analyticservice.common.enums.DataQualityStatus;
import com.zodiakk.analyticservice.dashboard.dto.DashboardGrain;
import com.zodiakk.analyticservice.integration.client.EmployeeClient;
import com.zodiakk.analyticservice.integration.dto.EmployeeLookupItemDto;
import com.zodiakk.analyticservice.integration.dto.EmployeeLookupResponseDto;
import com.zodiakk.analyticservice.integration.dto.ReferenceLookupDto;
import com.zodiakk.analyticservice.projection.entity.EmployeeActivityAggregate;
import com.zodiakk.analyticservice.projection.repository.EmployeeActivityAggregateRepository;
import com.zodiakk.analyticservice.security.access.AnalyticsAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private CareerAssessmentRepository assessmentRepository;

    @Mock
    private AssessmentSkillGapRepository skillGapRepository;

    @Mock
    private EmployeeActivityAggregateRepository aggregateRepository;

    @Mock
    private AnalyticsAccessService accessService;

    @Mock
    private EmployeeClient employeeClient;

    @InjectMocks
    private DashboardService service;

    @Test
    void buildsOverviewReadinessSkillsAndDepartments() {
        UUID employeeA = UUID.randomUUID();
        UUID employeeB = UUID.randomUUID();
        UUID employeeC = UUID.randomUUID();
        UUID hidden = UUID.randomUUID();
        UUID skillId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        CareerAssessment top = assessment(employeeA, "0.80", DataQualityStatus.COMPLETE, true);
        CareerAssessment mid = assessment(employeeB, "0.40", DataQualityStatus.PARTIAL, false);
        CareerAssessment low = assessment(employeeC, "0.10", DataQualityStatus.COMPLETE, true);
        CareerAssessment outside = assessment(hidden, "0.90", DataQualityStatus.COMPLETE, true);
        when(accessService.visibleEmployeeIds()).thenReturn(List.of(employeeA, employeeB, employeeC));
        when(assessmentRepository.findLatestFresh()).thenReturn(List.of(top, mid, low, outside));
        when(skillGapRepository.findAllByAssessmentIdInAndRequirementMetFalse(any())).thenReturn(List.of(
                gap(top.getId(), skillId),
                gap(mid.getId(), skillId),
                gap(low.getId(), skillId)
        ));
        when(employeeClient.skills(any())).thenReturn(new ReferenceLookupDto(List.of(
                new ReferenceLookupDto.Item(skillId, "Kafka", true)
        )));
        when(employeeClient.lookup(any())).thenReturn(new EmployeeLookupResponseDto(List.of(
                new EmployeeLookupItemDto(employeeA, "A", true, null, null, departmentId),
                new EmployeeLookupItemDto(employeeB, "B", true, null, null, departmentId),
                new EmployeeLookupItemDto(employeeC, "C", true, null, null, null)
        )));

        var overview = service.overview();
        assertEquals(3, overview.assessedEmployees());
        assertEquals(1, overview.topReadyCount());
        assertEquals("Kafka", overview.topDeficitSkillName());

        var readiness = service.readiness();
        assertEquals(3, readiness.buckets().size());
        assertEquals(1, service.skills().deficits().size());
        assertEquals(1, service.skillsTop().deficits().size());
        assertEquals(2, service.departments().departments().size());
        assertTrue(service.scopeKey().contains(employeeA.toString()));

        when(employeeClient.skills(any())).thenThrow(new IllegalStateException("catalog"));
        when(employeeClient.lookup(any())).thenThrow(new IllegalStateException("lookup"));
        assertNull(service.overview().topDeficitSkillName());
        assertNull(service.departments().departments().getFirst().departmentId());
    }

    @Test
    void groupsHistoryAndActivityByGrain() {
        UUID employeeId = UUID.randomUUID();
        CareerAssessment current = assessment(employeeId, "0.60", DataQualityStatus.COMPLETE, true);
        CareerAssessment undated = assessment(employeeId, "0.20", DataQualityStatus.PARTIAL, false);
        undated.setCalculatedAt(null);
        when(accessService.visibleEmployeeIds()).thenReturn(null);
        when(assessmentRepository.findFreshInRange(any(), any())).thenReturn(List.of(current, undated));
        when(assessmentRepository.findHistory(any(), any(), any())).thenReturn(List.of(current));
        when(aggregateRepository.findAllByPeriodStartGreaterThanEqualAndPeriodStartLessThanEqual(any(), any()))
                .thenReturn(List.of(
                        EmployeeActivityAggregate.builder()
                                .employeeId(employeeId)
                                .periodStart(LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1))
                                .approvedMinutes(120L)
                                .completedTasks(2)
                                .build(),
                        EmployeeActivityAggregate.builder()
                                .employeeId(UUID.randomUUID())
                                .periodStart(LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1))
                                .approvedMinutes(null)
                                .completedTasks(null)
                                .build()
                ));

        assertEquals(1, service.development().points().size());
        assertEquals(1, service.development(null).points().size());
        assertEquals(1, service.development(DashboardGrain.YEAR).points().size());
        assertEquals(1, service.development(DashboardGrain.DAY).points().size());
        assertEquals(120L, service.projectActivity().points().getFirst().approvedMinutes());
        assertEquals(120L, service.projectActivity(DashboardGrain.YEAR).points().getFirst().approvedMinutes());
        assertEquals(1, service.dynamics(employeeId).points().size());
        assertEquals("all", service.scopeKey());
    }

    @Test
    void emptyDashboardDoesNotCallCatalogs() {
        when(accessService.visibleEmployeeIds()).thenReturn(List.of());
        when(assessmentRepository.findLatestFresh()).thenReturn(List.of());

        var overview = service.overview();
        assertEquals(0, overview.assessedEmployees());
        assertNull(overview.topDeficitSkillId());
        assertTrue(service.skills().deficits().isEmpty());
        assertTrue(service.readiness().buckets().isEmpty());
        assertTrue(service.departments().departments().isEmpty());
    }

    private CareerAssessment assessment(UUID employeeId, String score, DataQualityStatus quality, boolean mandatory) {
        return CareerAssessment.builder()
                .id(UUID.randomUUID())
                .employeeId(employeeId)
                .skillScore(new BigDecimal(score))
                .totalScore(new BigDecimal(score))
                .dataQualityStatus(quality)
                .mandatoryRequirementsMet(mandatory)
                .isStale(false)
                .calculatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
    }

    private AssessmentSkillGap gap(UUID assessmentId, UUID skillId) {
        return AssessmentSkillGap.builder()
                .assessmentId(assessmentId)
                .skillId(skillId)
                .requirementMet(false)
                .build();
    }
}
