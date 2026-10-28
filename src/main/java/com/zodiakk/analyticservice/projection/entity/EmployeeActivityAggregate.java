package com.zodiakk.analyticservice.projection.entity;

import com.zodiakk.analyticservice.common.entity.IdentifiedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "employee_activity_aggregates")
@NoArgsConstructor
@SuperBuilder
public class EmployeeActivityAggregate extends IdentifiedEntity {

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "completed_tasks", nullable = false)
    private Integer completedTasks;

    @Column(name = "completed_story_points", precision = 12, scale = 2)
    private BigDecimal completedStoryPoints;

    @Column(name = "approved_minutes", nullable = false)
    private Long approvedMinutes;

    @Column(name = "source_updated_at")
    private OffsetDateTime sourceUpdatedAt;

    @Column(name = "last_calculated_at", nullable = false)
    private OffsetDateTime lastCalculatedAt;

}
