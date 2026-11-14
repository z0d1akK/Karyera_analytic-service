package com.zodiakk.analyticservice.screening.entity;

import com.zodiakk.analyticservice.common.entity.CreatedEntity;
import com.zodiakk.analyticservice.common.enums.ScreeningStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "talent_screenings")
@NoArgsConstructor
@SuperBuilder
public class TalentScreening extends CreatedEntity {

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "reserve_id", nullable = false)
    private UUID reserveId;

    @Column(name = "target_position_id", nullable = false)
    private UUID targetPositionId;

    @Column(name = "target_grade_id", nullable = false)
    private UUID targetGradeId;

    @Column(name = "requirement_id", nullable = false)
    private UUID requirementId;

    @Column(name = "requirement_revision", nullable = false)
    private Integer requirementRevision;

    @Column(name = "methodology_version", nullable = false, length = 30)
    private String methodologyVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 25)
    private ScreeningStatus status;

    @Column(name = "candidate_count", nullable = false)
    private Integer candidateCount;

    @Column(name = "evaluated_count", nullable = false)
    private Integer evaluatedCount;

    @Column(name = "failed_count", nullable = false)
    private Integer failedCount;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "explanation_text", columnDefinition = "text")
    private String explanationText;

}
