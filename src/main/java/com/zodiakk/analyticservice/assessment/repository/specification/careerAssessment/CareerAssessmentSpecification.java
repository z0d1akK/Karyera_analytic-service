package com.zodiakk.analyticservice.assessment.repository.specification.careerAssessment;

import com.zodiakk.analyticservice.assessment.dto.request.filter.CareerAssessmentFilterDto;
import com.zodiakk.analyticservice.assessment.entity.CareerAssessment;

import jakarta.persistence.criteria.*;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

@UtilityClass
public class CareerAssessmentSpecification {

    public static Specification<CareerAssessment> filter(CareerAssessmentFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            if (filter.getEmployeeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("employeeId"), filter.getEmployeeId()));
            }

            if (filter.getTargetPositionId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("targetPositionId"), filter.getTargetPositionId()));
            }

            if (filter.getTargetGradeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("targetGradeId"), filter.getTargetGradeId()));
            }

            if (filter.getRequirementId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("requirementId"), filter.getRequirementId()));
            }

            if (filter.getDataQualityStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("dataQualityStatus"), filter.getDataQualityStatus()));
            }

            if (filter.getIsStale() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isStale"), filter.getIsStale()));
            }

            if (filter.getCalculatedAtFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("calculatedAt"), filter.getCalculatedAtFrom()));
            }

            if (filter.getCalculatedAtTo() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("calculatedAt"), filter.getCalculatedAtTo()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
