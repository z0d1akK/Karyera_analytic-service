package com.zodiakk.analyticservice.assessment.repository;

import com.zodiakk.analyticservice.assessment.entity.AssessmentSkillGap;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface AssessmentSkillGapRepository extends JpaRepository<AssessmentSkillGap, UUID>, JpaSpecificationExecutor<AssessmentSkillGap> {

    List<AssessmentSkillGap> findAllByAssessmentId(UUID assessmentId);

    List<AssessmentSkillGap> findAllByAssessmentIdAndRequirementMetFalse(UUID assessmentId);

    Optional<AssessmentSkillGap> findByAssessmentIdAndSkillId(UUID assessmentId, UUID skillId);

    boolean existsByAssessmentIdAndSkillId(UUID assessmentId, UUID skillId);

    void deleteAllByAssessmentId(UUID assessmentId);

    long countByAssessmentIdAndRequirementMetFalse(UUID assessmentId);

    List<AssessmentSkillGap> findAllByAssessmentIdInAndRequirementMetFalse(Collection<UUID> assessmentIds);

}
