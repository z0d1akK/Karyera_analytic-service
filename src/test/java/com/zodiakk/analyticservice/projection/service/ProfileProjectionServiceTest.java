package com.zodiakk.analyticservice.projection.service;

import com.zodiakk.analyticservice.assessment.repository.CareerAssessmentRepository;
import com.zodiakk.analyticservice.assessment.service.AssessmentEngine;
import com.zodiakk.analyticservice.job.service.AnalyticsJobService;
import com.zodiakk.analyticservice.projection.entity.EmployeeAnalyticsProfile;
import com.zodiakk.analyticservice.projection.repository.EmployeeAnalyticsProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileProjectionServiceTest {

    @Mock
    private EmployeeAnalyticsProfileRepository profileRepository;

    @Mock
    private CareerAssessmentRepository assessmentRepository;

    @Mock
    private AssessmentEngine assessmentEngine;

    @Mock
    private AnalyticsJobService jobService;

    @InjectMocks
    private ProfileProjectionService service;

    @Test
    void appliesPositionChangeAndMarksStale() throws Exception {
        UUID employeeId = UUID.randomUUID();
        UUID positionId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        when(profileRepository.findById(employeeId)).thenReturn(Optional.empty());
        when(assessmentRepository.markStaleByEmployee(employeeId)).thenReturn(2);
        when(assessmentRepository.markStaleByTarget(positionId, gradeId)).thenReturn(1);

        var payload = new JsonMapper().readTree("""
                {"employeeId":"%s","positionId":"%s","gradeId":"%s"}
                """.formatted(employeeId, positionId, gradeId));
        service.applyPositionChange(payload, eventId);

        ArgumentCaptor<EmployeeAnalyticsProfile> captor = ArgumentCaptor.forClass(EmployeeAnalyticsProfile.class);
        verify(profileRepository).save(captor.capture());
        assertEquals(employeeId, captor.getValue().getEmployeeId());
        assertEquals(positionId, captor.getValue().getPositionId());
        assertEquals(eventId, captor.getValue().getLastSourceEventId());
        assertTrue(captor.getValue().getActive());
        assertEquals(2, service.markEmployeeStale(employeeId));
        assertEquals(1, service.markTargetStale(positionId, gradeId));
    }
}
