package com.zodiakk.analyticservice.assessment.controller;

import com.zodiakk.analyticservice.assessment.dto.response.CareerAssessmentResponseDto;
import com.zodiakk.analyticservice.assessment.service.AssessmentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/analytics/assessments")
@RequiredArgsConstructor
public class InternalAssessmentController {

    private final AssessmentQueryService assessmentQueryService;

    @GetMapping("/{id}")
    public CareerAssessmentResponseDto get(@PathVariable UUID id) {
        return assessmentQueryService.getInternal(id);
    }
}
