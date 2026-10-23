package com.zodiakk.analyticservice.recommendation.entity;

import com.zodiakk.analyticservice.common.entity.CreatedEntity;
import com.zodiakk.analyticservice.common.enums.RecommendationStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.ColumnTransformer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@Table(name = "ai_recommendations")
@NoArgsConstructor
@SuperBuilder
public class AiRecommendation extends CreatedEntity {

    @Column(name = "assessment_id", nullable = false)
    private UUID assessmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 25)
    private RecommendationStatus status;

    @Column(name = "model_identifier", length = 100)
    private String modelIdentifier;

    @Column(name = "prompt_version", length = 30)
    private String promptVersion;

    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "content", columnDefinition = "jsonb")
    private String content;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "generated_at")
    private OffsetDateTime generatedAt;

}
