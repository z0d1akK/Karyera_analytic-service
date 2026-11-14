package com.zodiakk.analyticservice.assessment.repository.specification.careerAssessment;

import com.zodiakk.analyticservice.common.dto.PageRequestDto;
import com.zodiakk.analyticservice.common.dto.SortDirection;

import lombok.experimental.UtilityClass;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

@UtilityClass
public class CareerAssessmentPageableFactory {

    private static final Map<String, String> ALLOWED_SORT_FIELDS = Map.ofEntries(
            Map.entry("employeeId", "employeeId"),
            Map.entry("targetPositionId", "targetPositionId"),
            Map.entry("targetGradeId", "targetGradeId"),
            Map.entry("totalScore", "totalScore"),
            Map.entry("dataQualityStatus", "dataQualityStatus"),
            Map.entry("calculatedAt", "calculatedAt"),
            Map.entry("createdAt", "createdAt")
    );

    public static Pageable create(PageRequestDto pageRequest) {
        String sortField = ALLOWED_SORT_FIELDS.getOrDefault(
                pageRequest.getSortBy(),
                "calculatedAt"
        );

        Sort.Direction direction = pageRequest.getSortDirection() == SortDirection.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return PageRequest.of(pageRequest.getPage(), pageRequest.getSize(), Sort.by(direction, sortField));
    }
}
