package com.zodiakk.analyticservice.job.repository.specification.analyticsJob;

import com.zodiakk.analyticservice.job.dto.request.filter.AnalyticsJobFilterDto;
import com.zodiakk.analyticservice.job.entity.AnalyticsJob;

import jakarta.persistence.criteria.*;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

@UtilityClass
public class AnalyticsJobSpecification {

    public static Specification<AnalyticsJob> filter(AnalyticsJobFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            if (filter.getJobType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("jobType"), filter.getJobType()));
            }

            if (filter.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));
            }

            if (filter.getRequestedBy() != null) {
                predicates.add(criteriaBuilder.equal(root.get("requestedBy"), filter.getRequestedBy()));
            }

            if (filter.getCreatedAtFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), filter.getCreatedAtFrom()));
            }

            if (filter.getCreatedAtTo() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), filter.getCreatedAtTo()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
