package com.zodiakk.analyticservice.recommendation.controller;

import com.zodiakk.analyticservice.recommendation.dto.response.AiRecommendationResponseDto;
import com.zodiakk.analyticservice.recommendation.entity.AiRecommendation;
import com.zodiakk.analyticservice.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics/assessments/{assessmentId}/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    public List<AiRecommendationResponseDto> list(@PathVariable UUID assessmentId) {
        return recommendationService.list(assessmentId).stream().map(this::toDto).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AiRecommendationResponseDto generate(@PathVariable UUID assessmentId) {
        return toDto(recommendationService.generate(assessmentId));
    }

    private AiRecommendationResponseDto toDto(AiRecommendation recommendation) {
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
}
