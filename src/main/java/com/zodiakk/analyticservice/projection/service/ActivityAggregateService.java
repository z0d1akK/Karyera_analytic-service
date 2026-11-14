package com.zodiakk.analyticservice.projection.service;

import com.zodiakk.analyticservice.integration.client.ProjectClient;
import com.zodiakk.analyticservice.integration.client.TimeClient;
import com.zodiakk.analyticservice.integration.dto.ApprovedSummaryDto;
import com.zodiakk.analyticservice.integration.dto.TaskStatisticsDto;
import com.zodiakk.analyticservice.projection.entity.EmployeeActivityAggregate;
import com.zodiakk.analyticservice.projection.repository.EmployeeActivityAggregateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityAggregateService {

    private final EmployeeActivityAggregateRepository aggregateRepository;

    private final TimeClient timeClient;

    private final ProjectClient projectClient;

    @Transactional
    public void rebuildEmployeePeriod(UUID employeeId, LocalDate from, LocalDate to) {
        YearMonth start = YearMonth.from(from);
        YearMonth end = YearMonth.from(to);

        for (YearMonth month = start; !month.isAfter(end); month = month.plusMonths(1)) {
            rebuildMonth(employeeId, month);
        }
    }

    @Transactional
    public void rebuildProject(UUID projectId, LocalDate monthDate) {
        rebuildProject(projectId, monthDate, null);
    }

    @Transactional
    public void rebuildProject(UUID projectId, LocalDate monthDate, UUID employeeId) {
        LocalDate periodStart = monthDate == null
                ? YearMonth.now(ZoneOffset.UTC).atDay(1)
                : YearMonth.from(monthDate).atDay(1);
        Set<UUID> employees = new HashSet<>();

        for (EmployeeActivityAggregate aggregate : aggregateRepository.findAllByProjectId(projectId)) {
            if (monthDate == null || aggregate.getPeriodStart().equals(periodStart)) {
                employees.add(aggregate.getEmployeeId());
            }
        }
        if (employeeId != null) {
            employees.add(employeeId);
        }
        if (employees.isEmpty()) {
            try {
                var members = projectClient.memberIds(projectId);
                if (members != null && members.employeeIds() != null) {
                    employees.addAll(members.employeeIds());
                }
            } catch (RuntimeException ex) {
                // keep empty and skip
            }
        }

        for (UUID id : employees) {
            rebuildMonth(id, YearMonth.from(periodStart));
        }
    }

    private void rebuildMonth(UUID employeeId, YearMonth month) {
        LocalDate periodStart = month.atDay(1);
        LocalDate periodEnd = month.atEndOfMonth();
        ApprovedSummaryDto summary = timeClient.approvedSummary(employeeId, periodStart, periodEnd);
        TaskStatisticsDto statistics = projectClient.taskStatistics(employeeId, periodStart, periodEnd);
        List<ApprovedSummaryDto.ProjectMinutes> projects = summary.projects() == null ? List.of() : summary.projects();
        UUID taskOwner = taskOwner(projects);
        Set<UUID> seen = new HashSet<>();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        for (ApprovedSummaryDto.ProjectMinutes project : projects) {
            seen.add(project.projectId());
            EmployeeActivityAggregate aggregate = aggregateRepository
                    .findByEmployeeIdAndProjectIdAndPeriodStart(employeeId, project.projectId(), periodStart)
                    .orElseGet(() -> EmployeeActivityAggregate.builder()
                            .employeeId(employeeId)
                            .projectId(project.projectId())
                            .periodStart(periodStart)
                            .build());
            aggregate.setPeriodEnd(periodEnd);
            aggregate.setApprovedMinutes(project.approvedMinutes());
            aggregate.setCompletedTasks(project.projectId().equals(taskOwner) ? (int) statistics.completedTasks() : 0);
            aggregate.setLastCalculatedAt(now);
            aggregate.setSourceUpdatedAt(now);
            aggregateRepository.save(aggregate);
        }

        for (EmployeeActivityAggregate existing : aggregateRepository.findAllByEmployeeIdAndPeriodStart(employeeId, periodStart)) {
            if (!seen.contains(existing.getProjectId())) {
                existing.setApprovedMinutes(0L);
                existing.setCompletedTasks(0);
                existing.setLastCalculatedAt(now);
                existing.setSourceUpdatedAt(now);
            }
        }
    }

    private UUID taskOwner(List<ApprovedSummaryDto.ProjectMinutes> projects) {
        return projects.stream()
                .max(Comparator.comparingLong(ApprovedSummaryDto.ProjectMinutes::approvedMinutes)
                        .thenComparing(ApprovedSummaryDto.ProjectMinutes::projectId))
                .map(ApprovedSummaryDto.ProjectMinutes::projectId)
                .orElse(null);
    }
}
