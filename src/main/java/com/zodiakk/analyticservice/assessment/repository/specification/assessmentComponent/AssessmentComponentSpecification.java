package com.zodiakk.analyticservice.assessment.repository.specification.assessmentComponent;

import com.zodiakk.analyticservice.assessment.dto.request.filter.AssessmentComponentFilterDto;
import com.zodiakk.analyticservice.assessment.entity.AssessmentComponent;

import jakarta.persistence.criteria.*;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

@UtilityClass
public class AssessmentComponentSpecification {

    public static Specification<AssessmentComponent> filter(AssessmentComponentFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            if (filter.getAssessmentId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("assessmentId"), filter.getAssessmentId()));
            }

            if (filter.getComponentCode() != null && !filter.getComponentCode().isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("componentCode")),
                        "%" + filter.getComponentCode().trim().toLowerCase() + "%"
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
