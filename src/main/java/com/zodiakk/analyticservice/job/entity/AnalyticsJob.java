package com.zodiakk.analyticservice.job.entity;

import com.zodiakk.analyticservice.common.entity.VersionedCreatedEntity;
import com.zodiakk.analyticservice.common.enums.AnalyticsJobStatus;
import com.zodiakk.analyticservice.common.enums.AnalyticsJobType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnTransformer;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "analytics_jobs")
@NoArgsConstructor
@SuperBuilder
public class AnalyticsJob extends VersionedCreatedEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 40)
    private AnalyticsJobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 25)
    private AnalyticsJobStatus status;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "parameters", nullable = false, columnDefinition = "jsonb")
    private String parameters;

    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "result", columnDefinition = "jsonb")
    private String result;

    @Column(name = "idempotency_key", length = 120)
    private String idempotencyKey;

    @Column(name = "attempts", nullable = false)
    private Integer attempts = 0;

    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts = 3;

    @Column(name = "result_id")
    private UUID resultId;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "next_attempt_at")
    private OffsetDateTime nextAttemptAt;

    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

}
