package com.zodiakk.analyticservice.screening.repository.specification.talentScreening;

import com.zodiakk.analyticservice.screening.dto.request.filter.TalentScreeningFilterDto;
import com.zodiakk.analyticservice.screening.entity.TalentScreening;

import jakarta.persistence.criteria.*;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

@UtilityClass
public class TalentScreeningSpecification {

    public static Specification<TalentScreening> filter(TalentScreeningFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            if (filter.getReserveId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("reserveId"), filter.getReserveId()));
            }

            if (filter.getTargetPositionId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("targetPositionId"), filter.getTargetPositionId()));
            }

            if (filter.getTargetGradeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("targetGradeId"), filter.getTargetGradeId()));
            }

            if (filter.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
