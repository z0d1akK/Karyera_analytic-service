package com.zodiakk.analyticservice.recommendation.repository.specification.aiRecommendation;

import com.zodiakk.analyticservice.recommendation.dto.request.filter.AiRecommendationFilterDto;
import com.zodiakk.analyticservice.recommendation.entity.AiRecommendation;

import jakarta.persistence.criteria.*;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

@UtilityClass
public class AiRecommendationSpecification {

    public static Specification<AiRecommendation> filter(AiRecommendationFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            if (filter.getAssessmentId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("assessmentId"), filter.getAssessmentId()));
            }

            if (filter.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
