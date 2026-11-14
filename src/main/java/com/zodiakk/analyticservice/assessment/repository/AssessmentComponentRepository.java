package com.zodiakk.analyticservice.assessment.repository;

import com.zodiakk.analyticservice.assessment.entity.AssessmentComponent;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface AssessmentComponentRepository extends JpaRepository<AssessmentComponent, UUID>, JpaSpecificationExecutor<AssessmentComponent> {

    List<AssessmentComponent> findAllByAssessmentId(UUID assessmentId);

    Optional<AssessmentComponent> findByAssessmentIdAndComponentCode(UUID assessmentId, String componentCode);

    boolean existsByAssessmentIdAndComponentCode(UUID assessmentId, String componentCode);

    void deleteAllByAssessmentId(UUID assessmentId);

}
