package com.zodiakk.analyticservice.assessment.controller;

import com.zodiakk.analyticservice.assessment.dto.response.CareerAssessmentResponseDto;
import com.zodiakk.analyticservice.assessment.service.AssessmentQueryService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InternalAssessmentControllerTest {

    @Test
    void returnsInternalAssessment() {
        AssessmentQueryService queryService = mock(AssessmentQueryService.class);
        UUID id = UUID.randomUUID();
        CareerAssessmentResponseDto dto = CareerAssessmentResponseDto.builder().id(id).build();
        when(queryService.getInternal(id)).thenReturn(dto);

        assertEquals(id, new InternalAssessmentController(queryService).get(id).getId());
    }
}
