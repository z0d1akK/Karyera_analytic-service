package com.zodiakk.analyticservice.recommendation.repository;

import com.zodiakk.analyticservice.common.enums.*;
import com.zodiakk.analyticservice.recommendation.entity.AiRecommendation;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface AiRecommendationRepository extends JpaRepository<AiRecommendation, UUID>, JpaSpecificationExecutor<AiRecommendation> {

    List<AiRecommendation> findAllByAssessmentIdOrderByCreatedAtDesc(UUID assessmentId);

    List<AiRecommendation> findAllByStatus(RecommendationStatus status);

    List<AiRecommendation> findAllByAssessmentIdAndStatus(UUID assessmentId, RecommendationStatus status);

}
