package com.zodiakk.analyticservice.job.repository;

import com.zodiakk.analyticservice.common.enums.*;
import com.zodiakk.analyticservice.job.entity.AnalyticsJob;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.*;
import java.util.*;

@Repository
public interface AnalyticsJobRepository extends JpaRepository<AnalyticsJob, UUID> {

    List<AnalyticsJob> findAllByStatus(AnalyticsJobStatus status);

    List<AnalyticsJob> findAllByJobTypeAndStatus(AnalyticsJobType jobType, AnalyticsJobStatus status);

    Optional<AnalyticsJob> findByIdempotencyKey(String idempotencyKey);

    @Query("""
            SELECT j FROM AnalyticsJob j
            WHERE j.status IN :statuses
              AND (j.nextAttemptAt IS NULL OR j.nextAttemptAt <= :now)
            ORDER BY j.createdAt
            """)
    List<AnalyticsJob> findAvailable(
            @Param("statuses") Collection<AnalyticsJobStatus> statuses,
            @Param("now") OffsetDateTime now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM AnalyticsJob j WHERE j.id = :jobId")
    Optional<AnalyticsJob> findByIdForUpdate(@Param("jobId") UUID jobId);

    @Query(value = """
            SELECT *
            FROM analytics_jobs
            WHERE status IN ('PENDING', 'RETRY_WAIT')
              AND (next_attempt_at IS NULL OR next_attempt_at <= :now)
              AND (locked_until IS NULL OR locked_until <= :now)
            ORDER BY created_at, id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<AnalyticsJob> lockAvailable(@Param("now") OffsetDateTime now, @Param("batchSize") int batchSize);

}
