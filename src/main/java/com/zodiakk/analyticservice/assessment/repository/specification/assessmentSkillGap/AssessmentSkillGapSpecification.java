package com.zodiakk.analyticservice.assessment.repository.specification.assessmentSkillGap;

import com.zodiakk.analyticservice.assessment.dto.request.filter.AssessmentSkillGapFilterDto;
import com.zodiakk.analyticservice.assessment.entity.AssessmentSkillGap;

import jakarta.persistence.criteria.*;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

@UtilityClass
public class AssessmentSkillGapSpecification {

    public static Specification<AssessmentSkillGap> filter(AssessmentSkillGapFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            if (filter.getAssessmentId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("assessmentId"), filter.getAssessmentId()));
            }

            if (filter.getSkillId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("skillId"), filter.getSkillId()));
            }

            if (filter.getMandatory() != null) {
                predicates.add(criteriaBuilder.equal(root.get("mandatory"), filter.getMandatory()));
            }

            if (filter.getRequirementMet() != null) {
                predicates.add(criteriaBuilder.equal(root.get("requirementMet"), filter.getRequirementMet()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
