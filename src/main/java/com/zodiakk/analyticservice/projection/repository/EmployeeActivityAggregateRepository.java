package com.zodiakk.analyticservice.projection.repository;

import com.zodiakk.analyticservice.projection.entity.EmployeeActivityAggregate;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.time.*;
import java.util.*;

@Repository
public interface EmployeeActivityAggregateRepository extends JpaRepository<EmployeeActivityAggregate, UUID> {

    List<EmployeeActivityAggregate> findAllByEmployeeIdAndPeriodStartBetween(
            UUID employeeId,
            LocalDate from,
            LocalDate to
    );

    List<EmployeeActivityAggregate> findAllByProjectIdAndPeriodStartBetween(
            UUID projectId,
            LocalDate from,
            LocalDate to
    );

    Optional<EmployeeActivityAggregate> findByEmployeeIdAndProjectIdAndPeriodStart(
            UUID employeeId,
            UUID projectId,
            LocalDate periodStart
    );

    boolean existsByEmployeeIdAndProjectIdAndPeriodStart(UUID employeeId, UUID projectId, LocalDate periodStart);

    List<EmployeeActivityAggregate> findAllByProjectId(UUID projectId);

    List<EmployeeActivityAggregate> findAllByEmployeeIdAndPeriodStart(UUID employeeId, LocalDate periodStart);

    List<EmployeeActivityAggregate> findAllByPeriodStartGreaterThanEqualAndPeriodStartLessThanEqual(
            LocalDate from,
            LocalDate to
    );

}
