package com.zodiakk.analyticservice.screening.repository.specification.talentScreening;

import com.zodiakk.analyticservice.common.dto.PageRequestDto;
import com.zodiakk.analyticservice.common.dto.SortDirection;

import lombok.experimental.UtilityClass;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

@UtilityClass
public class TalentScreeningPageableFactory {

    private static final Map<String, String> ALLOWED_SORT_FIELDS = Map.ofEntries(
            Map.entry("status", "status"),
            Map.entry("candidateCount", "candidateCount"),
            Map.entry("evaluatedCount", "evaluatedCount"),
            Map.entry("failedCount", "failedCount"),
            Map.entry("completedAt", "completedAt"),
            Map.entry("createdAt", "createdAt")
    );

    public static Pageable create(PageRequestDto pageRequest) {
        String sortField = ALLOWED_SORT_FIELDS.getOrDefault(
                pageRequest.getSortBy(),
                "createdAt"
        );

        Sort.Direction direction = pageRequest.getSortDirection() == SortDirection.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return PageRequest.of(pageRequest.getPage(), pageRequest.getSize(), Sort.by(direction, sortField));
    }
}
