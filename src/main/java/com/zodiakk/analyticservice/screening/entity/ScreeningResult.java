package com.zodiakk.analyticservice.screening.entity;

import com.zodiakk.analyticservice.common.entity.CreatedEntity;
import com.zodiakk.analyticservice.common.enums.ScreeningEligibilityStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "screening_results")
@NoArgsConstructor
@SuperBuilder
public class ScreeningResult extends CreatedEntity {

    @Column(name = "screening_id", nullable = false)
    private UUID screeningId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "assessment_id")
    private UUID assessmentId;

    @Column(name = "rank")
    private Integer rank;

    @Column(name = "score", precision = 8, scale = 6)
    private BigDecimal score;

    @Enumerated(EnumType.STRING)
    @Column(name = "eligibility_status", nullable = false, length = 30)
    private ScreeningEligibilityStatus eligibilityStatus;

    @Column(name = "mandatory_requirements_met")
    private Boolean mandatoryRequirementsMet;

    @Column(name = "exclusion_reason", length = 100)
    private String exclusionReason;

}
