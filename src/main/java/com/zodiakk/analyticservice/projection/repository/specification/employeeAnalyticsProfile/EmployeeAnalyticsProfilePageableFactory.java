package com.zodiakk.analyticservice.projection.repository.specification.employeeAnalyticsProfile;

import com.zodiakk.analyticservice.common.dto.PageRequestDto;
import com.zodiakk.analyticservice.common.dto.SortDirection;

import lombok.experimental.UtilityClass;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

@UtilityClass
public class EmployeeAnalyticsProfilePageableFactory {

    private static final Map<String, String> ALLOWED_SORT_FIELDS = Map.ofEntries(
            Map.entry("positionId", "positionId"),
            Map.entry("gradeId", "gradeId"),
            Map.entry("active", "active"),
            Map.entry("sourceVersion", "sourceVersion"),
            Map.entry("lastSyncedAt", "lastSyncedAt"),
            Map.entry("stale", "stale")
    );

    public static Pageable create(PageRequestDto pageRequest) {
        String sortField = ALLOWED_SORT_FIELDS.getOrDefault(
                pageRequest.getSortBy(),
                "positionId"
        );

        Sort.Direction direction = pageRequest.getSortDirection() == SortDirection.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return PageRequest.of(pageRequest.getPage(), pageRequest.getSize(), Sort.by(direction, sortField));
    }
}
