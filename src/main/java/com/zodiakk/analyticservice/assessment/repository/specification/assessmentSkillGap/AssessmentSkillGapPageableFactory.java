package com.zodiakk.analyticservice.assessment.repository.specification.assessmentSkillGap;

import com.zodiakk.analyticservice.common.dto.PageRequestDto;
import com.zodiakk.analyticservice.common.dto.SortDirection;

import lombok.experimental.UtilityClass;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

@UtilityClass
public class AssessmentSkillGapPageableFactory {

    private static final Map<String, String> ALLOWED_SORT_FIELDS = Map.ofEntries(
            Map.entry("actualLevel", "actualLevel"),
            Map.entry("requiredLevel", "requiredLevel"),
            Map.entry("weight", "weight"),
            Map.entry("absoluteGap", "absoluteGap"),
            Map.entry("normalizedGap", "normalizedGap"),
            Map.entry("priorityScore", "priorityScore"),
            Map.entry("mandatory", "mandatory"),
            Map.entry("requirementMet", "requirementMet")
    );

    public static Pageable create(PageRequestDto pageRequest) {
        String sortField = ALLOWED_SORT_FIELDS.getOrDefault(
                pageRequest.getSortBy(),
                "priorityScore"
        );

        Sort.Direction direction = pageRequest.getSortDirection() == SortDirection.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return PageRequest.of(pageRequest.getPage(), pageRequest.getSize(), Sort.by(direction, sortField));
    }
}
