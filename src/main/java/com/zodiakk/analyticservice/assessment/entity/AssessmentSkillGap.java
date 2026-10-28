package com.zodiakk.analyticservice.assessment.entity;

import com.zodiakk.analyticservice.common.entity.IdentifiedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "assessment_skill_gaps")
@NoArgsConstructor
@SuperBuilder
public class AssessmentSkillGap extends IdentifiedEntity {

    @Column(name = "assessment_id", nullable = false)
    private UUID assessmentId;

    @Column(name = "skill_id", nullable = false)
    private UUID skillId;

    @Column(name = "actual_level", nullable = false)
    private Integer actualLevel;

    @Column(name = "required_level", nullable = false)
    private Integer requiredLevel;

    @Column(name = "weight", nullable = false, precision = 8, scale = 5)
    private BigDecimal weight;

    @Column(name = "absolute_gap", nullable = false)
    private Integer absoluteGap;

    @Column(name = "normalized_gap", nullable = false, precision = 8, scale = 6)
    private BigDecimal normalizedGap;

    @Column(name = "priority_score", nullable = false, precision = 12, scale = 6)
    private BigDecimal priorityScore;

    @Column(name = "mandatory", nullable = false)
    private Boolean mandatory;

    @Column(name = "requirement_met", nullable = false)
    private Boolean requirementMet;

}
