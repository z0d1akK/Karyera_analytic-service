package com.zodiakk.analyticservice.screening.service;

import com.zodiakk.analyticservice.common.enums.AnalyticsJobType;
import com.zodiakk.analyticservice.common.exception.InsufficientDataException;
import com.zodiakk.analyticservice.integration.client.EmployeeClient;
import com.zodiakk.analyticservice.integration.dto.ActiveEmployeeIdsDto;
import com.zodiakk.analyticservice.integration.support.IntegrationCalls;
import com.zodiakk.analyticservice.job.dto.ScreeningJobParameters;
import com.zodiakk.analyticservice.job.entity.AnalyticsJob;
import com.zodiakk.analyticservice.job.service.AnalyticsJobService;
import com.zodiakk.analyticservice.screening.dto.request.action.CandidateSelectionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScreeningStartService {

    private final EmployeeClient employeeClient;

    private final AnalyticsJobService jobService;

    public AnalyticsJob start(
            UUID requestedBy,
            UUID reserveId,
            UUID targetPositionId,
            UUID targetGradeId,
            UUID requirementId,
            UUID initiatorEmployeeId,
            String initiatorRole,
            String idempotencyKey,
            CandidateSelectionDto selection
    ) {
        CandidateSelectionDto resolved = selection == null
                ? CandidateSelectionDto.builder().type("FILTER").activeOnly(true).build()
                : selection;
        List<UUID> candidateIds = resolveCandidates(resolved);

        return jobService.create(
                AnalyticsJobType.TALENT_SCREENING,
                requestedBy,
                idempotencyKey,
                new ScreeningJobParameters(
                        reserveId,
                        targetPositionId,
                        targetGradeId,
                        requirementId,
                        candidateIds,
                        initiatorEmployeeId,
                        initiatorRole,
                        resolved.getType()
                )
        );
    }

    private List<UUID> resolveCandidates(CandidateSelectionDto selection) {
        String type = selection.getType() == null ? "FILTER" : selection.getType().trim().toUpperCase();
        if ("EXPLICIT".equals(type)) {
            List<UUID> ids = selection.getEmployeeIds() == null ? List.of() : List.copyOf(selection.getEmployeeIds());
            if (ids.isEmpty()) {
                throw new InsufficientDataException("Для явного отбора нужен список сотрудников");
            }
            return ids;
        }
        if (!"FILTER".equals(type)) {
            throw new IllegalArgumentException("Неизвестный тип отбора кандидатов: " + selection.getType());
        }
        boolean activeOnly = selection.getActiveOnly() == null || selection.getActiveOnly();
        ActiveEmployeeIdsDto filtered = IntegrationCalls.required(
                () -> employeeClient.filterIds(
                        activeOnly,
                        selection.getPositionId(),
                        selection.getGradeId(),
                        selection.getDepartmentId()
                ),
                "Не удалось получить список сотрудников"
        );
        return filtered.employeeIds() == null ? List.of() : List.copyOf(filtered.employeeIds());
    }
}
