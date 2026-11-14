package com.zodiakk.analyticservice.projection.service;

import com.zodiakk.analyticservice.integration.client.ProjectClient;
import com.zodiakk.analyticservice.integration.client.TimeClient;
import com.zodiakk.analyticservice.integration.dto.ApprovedSummaryDto;
import com.zodiakk.analyticservice.integration.dto.TaskStatisticsDto;
import com.zodiakk.analyticservice.projection.entity.EmployeeActivityAggregate;
import com.zodiakk.analyticservice.projection.repository.EmployeeActivityAggregateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityAggregateServiceTest {

    @Mock
    private EmployeeActivityAggregateRepository aggregateRepository;

    @Mock
    private TimeClient timeClient;

    @Mock
    private ProjectClient projectClient;

    @InjectMocks
    private ActivityAggregateService service;

    @Test
    void rebuildsAMonthAndClearsProjectsThatDisappeared() {
        UUID employeeId = UUID.randomUUID();
        UUID kept = UUID.randomUUID();
        UUID dropped = UUID.randomUUID();
        LocalDate start = LocalDate.of(2026, 10, 1);
        when(timeClient.approvedSummary(any(), any(), any())).thenReturn(new ApprovedSummaryDto(
                employeeId, start, start.withDayOfMonth(31), 100,
                List.of(new ApprovedSummaryDto.ProjectMinutes(kept, 100))
        ));
        when(projectClient.taskStatistics(any(), any(), any())).thenReturn(new TaskStatisticsDto(
                employeeId, start, start.withDayOfMonth(31), 3, Map.of(), Map.of("LOW", 3L), 1
        ));
        when(aggregateRepository.findByEmployeeIdAndProjectIdAndPeriodStart(any(), any(), any())).thenReturn(Optional.empty());
        EmployeeActivityAggregate stale = EmployeeActivityAggregate.builder()
                .employeeId(employeeId).projectId(dropped).periodStart(start).approvedMinutes(10L).completedTasks(1).build();
        when(aggregateRepository.findAllByEmployeeIdAndPeriodStart(employeeId, start)).thenReturn(List.of(stale));
        when(aggregateRepository.findAllByProjectId(kept)).thenReturn(List.of(
                EmployeeActivityAggregate.builder().employeeId(employeeId).projectId(kept).periodStart(start).build()
        ));

        service.rebuildEmployeePeriod(employeeId, start, start.plusDays(10));
        service.rebuildProject(kept, start);

        verify(aggregateRepository, org.mockito.Mockito.atLeastOnce()).save(any());
    }
}
