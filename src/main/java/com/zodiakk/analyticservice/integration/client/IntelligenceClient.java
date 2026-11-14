package com.zodiakk.analyticservice.integration.client;

import com.zodiakk.analyticservice.integration.config.IntegrationFeignConfig;
import com.zodiakk.analyticservice.integration.dto.CareerExplanationRequest;
import com.zodiakk.analyticservice.integration.dto.CareerExplanationResponse;
import com.zodiakk.analyticservice.integration.dto.CareerPathInterpretationRequest;
import com.zodiakk.analyticservice.integration.dto.CareerPathInterpretationResponse;
import com.zodiakk.analyticservice.integration.dto.DevelopmentRecommendationRequest;
import com.zodiakk.analyticservice.integration.dto.DevelopmentRecommendationResponse;
import com.zodiakk.analyticservice.integration.dto.TalentScreeningExplanationRequest;
import com.zodiakk.analyticservice.integration.dto.TalentScreeningExplanationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "intelligent-service",
        url = "${services.intelligence.url}",
        configuration = IntegrationFeignConfig.class
)
public interface IntelligenceClient {

    @PostMapping("/internal/v1/intelligence/career-explanations")
    CareerExplanationResponse explain(@RequestBody CareerExplanationRequest request);

    @PostMapping("/internal/v1/intelligence/development-recommendations")
    DevelopmentRecommendationResponse recommend(@RequestBody DevelopmentRecommendationRequest request);

    @PostMapping("/internal/v1/intelligence/career-path-interpretations")
    CareerPathInterpretationResponse interpret(@RequestBody CareerPathInterpretationRequest request);

    @PostMapping("/internal/v1/intelligence/talent-screening-explanations")
    TalentScreeningExplanationResponse explainScreening(@RequestBody TalentScreeningExplanationRequest request);
}
