package com.zodiakk.analyticservice.projection.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "employee_analytics_profiles")
@NoArgsConstructor
@SuperBuilder
public class EmployeeAnalyticsProfile {

    @jakarta.persistence.Id
    @Column(name = "employee_id", nullable = false, updatable = false)
    private UUID employeeId;

    @Column(name = "position_id", nullable = false)
    private UUID positionId;

    @Column(name = "grade_id", nullable = false)
    private UUID gradeId;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "last_source_event_id")
    private UUID lastSourceEventId;

    @Column(name = "source_version")
    private Long sourceVersion;

    @Column(name = "last_synced_at", nullable = false)
    private OffsetDateTime lastSyncedAt;

    @Column(name = "stale", nullable = false)
    private Boolean stale;

}
