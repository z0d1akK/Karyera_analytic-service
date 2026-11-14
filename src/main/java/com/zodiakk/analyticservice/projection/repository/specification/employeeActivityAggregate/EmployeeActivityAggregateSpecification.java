package com.zodiakk.analyticservice.projection.repository.specification.employeeActivityAggregate;

import com.zodiakk.analyticservice.projection.dto.request.filter.EmployeeActivityAggregateFilterDto;
import com.zodiakk.analyticservice.projection.entity.EmployeeActivityAggregate;

import jakarta.persistence.criteria.*;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

@UtilityClass
public class EmployeeActivityAggregateSpecification {

    public static Specification<EmployeeActivityAggregate> filter(EmployeeActivityAggregateFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            if (filter.getEmployeeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("employeeId"), filter.getEmployeeId()));
            }

            if (filter.getProjectId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("projectId"), filter.getProjectId()));
            }

            if (filter.getPeriodStartFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("periodStart"), filter.getPeriodStartFrom()));
            }

            if (filter.getPeriodStartTo() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("periodStart"), filter.getPeriodStartTo()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
