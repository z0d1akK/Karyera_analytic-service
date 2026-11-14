package com.zodiakk.analyticservice.assessment.repository;

import com.zodiakk.analyticservice.assessment.entity.CareerAssessment;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.*;

@Repository
public interface CareerAssessmentRepository extends JpaRepository<CareerAssessment, UUID> {

    List<CareerAssessment> findAllByEmployeeIdOrderByCalculatedAtDesc(UUID employeeId);

    List<CareerAssessment> findAllByJobId(UUID jobId);

    List<CareerAssessment> findAllByEmployeeIdAndIsStaleTrue(UUID employeeId);

    List<CareerAssessment> findAllByTargetPositionIdAndTargetGradeIdOrderByCalculatedAtDesc(
            UUID targetPositionId,
            UUID targetGradeId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM CareerAssessment a WHERE a.id = :assessmentId")
    Optional<CareerAssessment> findByIdForUpdate(@Param("assessmentId") UUID assessmentId);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE CareerAssessment a
            SET a.isStale = true
            WHERE a.employeeId = :employeeId
              AND a.isStale = false
            """)
    int markStaleByEmployee(@Param("employeeId") UUID employeeId);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE CareerAssessment a
            SET a.isStale = true
            WHERE a.targetPositionId = :positionId
              AND a.targetGradeId = :gradeId
              AND a.isStale = false
            """)
    int markStaleByTarget(
            @Param("positionId") UUID positionId,
            @Param("gradeId") UUID gradeId
    );

    @Query("""
            SELECT a
            FROM CareerAssessment a
            WHERE a.isStale = false
              AND a.calculatedAt = (
                  SELECT MAX(other.calculatedAt)
                  FROM CareerAssessment other
                  WHERE other.employeeId = a.employeeId
                    AND other.isStale = false
              )
            """)
    List<CareerAssessment> findLatestFresh();

    @Query("""
            SELECT a
            FROM CareerAssessment a
            WHERE a.employeeId = :employeeId
              AND a.calculatedAt >= :from
              AND a.calculatedAt < :to
            ORDER BY a.calculatedAt
            """)
    List<CareerAssessment> findHistory(
            @Param("employeeId") UUID employeeId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );

    @Query("""
            SELECT a
            FROM CareerAssessment a
            WHERE a.isStale = false
              AND a.calculatedAt >= :from
              AND a.calculatedAt < :to
            """)
    List<CareerAssessment> findFreshInRange(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );

}
