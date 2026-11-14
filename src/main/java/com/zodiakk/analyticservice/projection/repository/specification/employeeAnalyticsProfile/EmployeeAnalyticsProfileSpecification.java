package com.zodiakk.analyticservice.projection.repository.specification.employeeAnalyticsProfile;

import com.zodiakk.analyticservice.projection.dto.request.filter.EmployeeAnalyticsProfileFilterDto;
import com.zodiakk.analyticservice.projection.entity.EmployeeAnalyticsProfile;

import jakarta.persistence.criteria.*;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

@UtilityClass
public class EmployeeAnalyticsProfileSpecification {

    public static Specification<EmployeeAnalyticsProfile> filter(EmployeeAnalyticsProfileFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            if (filter.getPositionId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("positionId"), filter.getPositionId()));
            }

            if (filter.getGradeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("gradeId"), filter.getGradeId()));
            }

            if (filter.getActive() != null) {
                predicates.add(criteriaBuilder.equal(root.get("active"), filter.getActive()));
            }

            if (filter.getStale() != null) {
                predicates.add(criteriaBuilder.equal(root.get("stale"), filter.getStale()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
