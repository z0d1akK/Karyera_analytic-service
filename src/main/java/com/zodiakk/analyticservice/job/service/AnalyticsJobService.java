package com.zodiakk.analyticservice.job.service;

import com.zodiakk.analyticservice.common.enums.AnalyticsJobStatus;
import com.zodiakk.analyticservice.common.enums.AnalyticsJobType;
import com.zodiakk.analyticservice.common.exception.JobNotFoundException;
import com.zodiakk.analyticservice.config.JobProperties;
import com.zodiakk.analyticservice.job.entity.AnalyticsJob;
import com.zodiakk.analyticservice.job.repository.AnalyticsJobRepository;
import com.zodiakk.analyticservice.security.access.AnalyticsAccessService;
import com.zodiakk.analyticservice.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyticsJobService {

    public static final UUID SYSTEM_REQUESTER = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final AnalyticsJobRepository jobRepository;

    private final JobProperties jobProperties;

    private final ObjectMapper objectMapper;

    private final AnalyticsAccessService accessService;

    @Transactional
    public AnalyticsJob create(AnalyticsJobType type, UUID requestedBy, String idempotencyKey, Object parameters) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            AnalyticsJob existing = jobRepository.findByIdempotencyKey(idempotencyKey).orElse(null);

            if (existing != null) {
                return existing;
            }
        }

        AnalyticsJob job = AnalyticsJob.builder()
                .jobType(type)
                .status(AnalyticsJobStatus.PENDING)
                .requestedBy(requestedBy)
                .parameters(write(parameters))
                .idempotencyKey(idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey)
                .attempts(0)
                .maxAttempts(3)
                .nextAttemptAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        return jobRepository.save(job);
    }

    @Transactional
    public List<AnalyticsJob> claim() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        List<AnalyticsJob> jobs = jobRepository.lockAvailable(now, jobProperties.batchSize());

        for (AnalyticsJob job : jobs) {
            job.setStatus(AnalyticsJobStatus.RUNNING);
            job.setAttempts(job.getAttempts() + 1);
            job.setStartedAt(job.getStartedAt() == null ? now : job.getStartedAt());
            job.setLockedUntil(now.plusSeconds(jobProperties.leaseSeconds()));
            job.setErrorCode(null);
            job.setErrorMessage(null);
        }

        jobRepository.flush();
        return List.copyOf(jobs);
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(cacheNames = "dashboards", allEntries = true)
    public void complete(UUID jobId, UUID resultId, Object result) {
        AnalyticsJob job = jobRepository.findByIdForUpdate(jobId).orElseThrow(() -> new JobNotFoundException(jobId));
        job.setStatus(AnalyticsJobStatus.COMPLETED);
        job.setResultId(resultId);
        job.setResult(result == null ? null : write(result));
        job.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        job.setLockedUntil(null);
        job.setErrorCode(null);
        job.setErrorMessage(null);
    }

    @Transactional
    public void fail(UUID jobId, String errorCode, String message) {
        AnalyticsJob job = jobRepository.findByIdForUpdate(jobId).orElseThrow(() -> new JobNotFoundException(jobId));
        job.setStatus(AnalyticsJobStatus.FAILED);
        job.setErrorCode(errorCode);
        job.setErrorMessage(trim(message));
        job.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        job.setLockedUntil(null);
    }

    @Transactional
    public void retry(UUID jobId, String message) {
        AnalyticsJob job = jobRepository.findByIdForUpdate(jobId).orElseThrow(() -> new JobNotFoundException(jobId));

        if (job.getAttempts() >= job.getMaxAttempts()) {
            job.setStatus(AnalyticsJobStatus.FAILED);
            job.setErrorCode("RETRY_EXHAUSTED");
            job.setErrorMessage(trim(message));
            job.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));
            job.setLockedUntil(null);
            return;
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        long delaySeconds = Math.min(300L, 1L << Math.min(job.getAttempts(), 8));
        job.setStatus(AnalyticsJobStatus.RETRY_WAIT);
        job.setErrorCode("TRANSIENT");
        job.setErrorMessage(trim(message));
        job.setLockedUntil(null);
        job.setNextAttemptAt(now.plusSeconds(delaySeconds));
    }

    @Transactional(readOnly = true)
    public AnalyticsJob getVisible(UUID jobId) {
        AnalyticsJob job = jobRepository.findById(jobId).orElseThrow(() -> new JobNotFoundException(jobId));

        if (accessService.isHr() || accessService.isAdmin()
                || job.getRequestedBy().equals(SecurityUtils.getCurrentUserId())) {
            return job;
        }

        UUID employeeId = employeeIdFromParameters(job.getParameters());
        if (employeeId != null && accessService.canAccessEmployee(employeeId)) {
            return job;
        }

        throw new org.springframework.security.access.AccessDeniedException("Нет доступа к заданию");
    }

    public <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Не удалось прочитать параметры задания", ex);
        }
    }

    public String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Не удалось сериализовать параметры задания", ex);
        }
    }

    public int progress(AnalyticsJob job, int evaluated, int total) {
        return switch (job.getStatus()) {
            case PENDING -> 0;
            case RETRY_WAIT -> 10;
            case RUNNING -> total <= 0 ? 50 : Math.min(99, evaluated * 100 / total);
            case COMPLETED, FAILED -> 100;
        };
    }

    private UUID employeeIdFromParameters(String parameters) {
        try {
            JsonNode node = objectMapper.readTree(parameters);
            if (node.get("employeeId") == null || node.get("employeeId").isNull()) {
                return null;
            }
            return UUID.fromString(node.get("employeeId").asString());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String trim(String message) {
        if (message == null) {
            return null;
        }

        String safe = message.replaceAll("[\\r\\n]+", " ");
        return safe.length() <= 300 ? safe : safe.substring(0, 300);
    }
}
