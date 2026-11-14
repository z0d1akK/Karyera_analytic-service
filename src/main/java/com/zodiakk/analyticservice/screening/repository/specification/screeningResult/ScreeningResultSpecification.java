package com.zodiakk.analyticservice.screening.repository.specification.screeningResult;

import com.zodiakk.analyticservice.screening.dto.request.filter.ScreeningResultFilterDto;
import com.zodiakk.analyticservice.screening.entity.ScreeningResult;

import jakarta.persistence.criteria.*;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

@UtilityClass
public class ScreeningResultSpecification {

    public static Specification<ScreeningResult> filter(ScreeningResultFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            if (filter.getScreeningId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("screeningId"), filter.getScreeningId()));
            }

            if (filter.getEmployeeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("employeeId"), filter.getEmployeeId()));
            }

            if (filter.getEligibilityStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("eligibilityStatus"), filter.getEligibilityStatus()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
