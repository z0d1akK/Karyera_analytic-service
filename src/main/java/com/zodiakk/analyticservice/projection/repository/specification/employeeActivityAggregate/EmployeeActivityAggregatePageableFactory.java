package com.zodiakk.analyticservice.projection.repository.specification.employeeActivityAggregate;

import com.zodiakk.analyticservice.common.dto.PageRequestDto;
import com.zodiakk.analyticservice.common.dto.SortDirection;

import lombok.experimental.UtilityClass;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

@UtilityClass
public class EmployeeActivityAggregatePageableFactory {

    private static final Map<String, String> ALLOWED_SORT_FIELDS = Map.ofEntries(
            Map.entry("periodStart", "periodStart"),
            Map.entry("periodEnd", "periodEnd"),
            Map.entry("completedTasks", "completedTasks"),
            Map.entry("completedStoryPoints", "completedStoryPoints"),
            Map.entry("approvedMinutes", "approvedMinutes"),
            Map.entry("lastCalculatedAt", "lastCalculatedAt")
    );

    public static Pageable create(PageRequestDto pageRequest) {
        String sortField = ALLOWED_SORT_FIELDS.getOrDefault(
                pageRequest.getSortBy(),
                "periodStart"
        );

        Sort.Direction direction = pageRequest.getSortDirection() == SortDirection.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return PageRequest.of(pageRequest.getPage(), pageRequest.getSize(), Sort.by(direction, sortField));
    }
}
