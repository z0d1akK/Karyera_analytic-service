package com.zodiakk.analyticservice.assessment.entity;

import com.zodiakk.analyticservice.common.entity.CreatedEntity;
import com.zodiakk.analyticservice.common.enums.DataQualityStatus;

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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "career_assessments")
@NoArgsConstructor
@SuperBuilder
public class CareerAssessment extends CreatedEntity {

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

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

    @Column(name = "skill_score", nullable = false, precision = 8, scale = 6)
    private BigDecimal skillScore;

    @Column(name = "experience_score", precision = 8, scale = 6)
    private BigDecimal experienceScore;

    @Column(name = "total_score", nullable = false, precision = 8, scale = 6)
    private BigDecimal totalScore;

    @Column(name = "mandatory_requirements_met", nullable = false)
    private Boolean mandatoryRequirementsMet;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_quality_status", nullable = false, length = 25)
    private DataQualityStatus dataQualityStatus;

    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "input_snapshot", nullable = false, columnDefinition = "jsonb")
    private String inputSnapshot;

    @Column(name = "input_hash", nullable = false, length = 64)
    private String inputHash;

    @Column(name = "is_stale", nullable = false)
    private Boolean isStale = false;

    @Column(name = "calculated_at", nullable = false)
    private OffsetDateTime calculatedAt;

}
