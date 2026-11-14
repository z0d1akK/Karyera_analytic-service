package com.zodiakk.analyticservice.projection.repository;

import com.zodiakk.analyticservice.projection.entity.EmployeeAnalyticsProfile;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface EmployeeAnalyticsProfileRepository extends JpaRepository<EmployeeAnalyticsProfile, UUID>, JpaSpecificationExecutor<EmployeeAnalyticsProfile> {

    List<EmployeeAnalyticsProfile> findAllByActiveTrue();

    List<EmployeeAnalyticsProfile> findAllByPositionId(UUID positionId);

    List<EmployeeAnalyticsProfile> findAllByGradeId(UUID gradeId);

    List<EmployeeAnalyticsProfile> findAllByStaleTrue();

}
