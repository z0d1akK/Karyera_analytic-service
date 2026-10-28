package com.zodiakk.analyticservice.assessment.entity;

import com.zodiakk.analyticservice.common.entity.IdentifiedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnTransformer;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "assessment_components")
@NoArgsConstructor
@SuperBuilder
public class AssessmentComponent extends IdentifiedEntity {

    @Column(name = "assessment_id", nullable = false)
    private UUID assessmentId;

    @Column(name = "component_code", nullable = false, length = 40)
    private String componentCode;

    @Column(name = "raw_score", nullable = false, precision = 8, scale = 6)
    private BigDecimal rawScore;

    @Column(name = "coefficient", nullable = false, precision = 8, scale = 6)
    private BigDecimal coefficient;

    @Column(name = "weighted_score", nullable = false, precision = 8, scale = 6)
    private BigDecimal weightedScore;

    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "details", columnDefinition = "jsonb")
    private String details;

}
