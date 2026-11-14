package com.zodiakk.analyticservice.job.service;

import com.zodiakk.analyticservice.common.enums.AnalyticsJobStatus;
import com.zodiakk.analyticservice.common.enums.AnalyticsJobType;
import com.zodiakk.analyticservice.common.exception.JobNotFoundException;
import com.zodiakk.analyticservice.config.JobProperties;
import com.zodiakk.analyticservice.job.entity.AnalyticsJob;
import com.zodiakk.analyticservice.job.repository.AnalyticsJobRepository;
import com.zodiakk.analyticservice.security.access.AnalyticsAccessService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsJobServiceTest {

    @Mock
    private AnalyticsJobRepository jobRepository;

    @Mock
    private AnalyticsAccessService accessService;

    private AnalyticsJobService service;

    @org.junit.jupiter.api.BeforeEach
    void mapper() {
        service = new AnalyticsJobService(jobRepository, new JobProperties(1, 1000, 30), new JsonMapper(), accessService);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void reusesAnIdempotentJobAndRetriesUntilExhausted() {
        AnalyticsJob existing = job(AnalyticsJobStatus.PENDING, 0);
        when(jobRepository.findByIdempotencyKey("same")).thenReturn(Optional.of(existing));
        assertEquals(existing, service.create(AnalyticsJobType.CAREER_ASSESSMENT, UUID.randomUUID(), "same", java.util.Map.of()));

        AnalyticsJob running = job(AnalyticsJobStatus.RETRY_WAIT, 3);
        running.setMaxAttempts(3);
        when(jobRepository.findByIdForUpdate(running.getId())).thenReturn(Optional.of(running));
        service.retry(running.getId(), "again");
        assertEquals(AnalyticsJobStatus.FAILED, running.getStatus());
        assertEquals("RETRY_EXHAUSTED", running.getErrorCode());

        AnalyticsJob fresh = job(AnalyticsJobStatus.RUNNING, 1);
        fresh.setMaxAttempts(3);
        when(jobRepository.findByIdForUpdate(fresh.getId())).thenReturn(Optional.of(fresh));
        service.retry(fresh.getId(), "later");
        assertEquals(AnalyticsJobStatus.RETRY_WAIT, fresh.getStatus());
    }

    @Test
    void hidesAnotherUsersJob() {
        AnalyticsJob job = job(AnalyticsJobStatus.PENDING, 0);
        job.setRequestedBy(UUID.randomUUID());
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        org.mockito.Mockito.lenient().when(accessService.isHrOrAdmin()).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                com.zodiakk.analyticservice.security.principal.CurrentUser.builder()
                        .userId(UUID.randomUUID()).role("ROLE_EMPLOYEE").email("a@b.c").build(),
                null
        ));

        assertThrows(AccessDeniedException.class, () -> service.getVisible(job.getId()));
    }

    @Test
    void reportsProgress() {
        AnalyticsJob job = job(AnalyticsJobStatus.RUNNING, 1);
        assertEquals(50, service.progress(job, 0, 0));
        assertEquals(40, service.progress(job, 2, 5));
        job.setStatus(AnalyticsJobStatus.PENDING);
        assertEquals(0, service.progress(job, 0, 1));
        job.setStatus(AnalyticsJobStatus.FAILED);
        assertEquals(100, service.progress(job, 0, 1));
        job.setStatus(AnalyticsJobStatus.RETRY_WAIT);
        assertEquals(10, service.progress(job, 0, 1));
        job.setStatus(AnalyticsJobStatus.COMPLETED);
        assertEquals(100, service.progress(job, 0, 1));
    }

    @Test
    void claimsCreatesAndShowsJobsByRequester() {
        when(jobRepository.lockAvailable(any(), anyInt())).thenReturn(List.of(job(AnalyticsJobStatus.PENDING, 0)));
        assertEquals(AnalyticsJobStatus.RUNNING, service.claim().getFirst().getStatus());

        when(jobRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        AnalyticsJob created = service.create(AnalyticsJobType.CAREER_ASSESSMENT, UUID.randomUUID(), "  ", java.util.Map.of("employeeId", UUID.randomUUID()));
        assertEquals(AnalyticsJobStatus.PENDING, created.getStatus());

        AnalyticsJob visible = job(AnalyticsJobStatus.PENDING, 0);
        UUID requester = UUID.randomUUID();
        visible.setRequestedBy(requester);
        visible.setParameters("{\"employeeId\":\"" + UUID.randomUUID() + "\"}");
        when(jobRepository.findById(visible.getId())).thenReturn(Optional.of(visible));
        when(accessService.isHr()).thenReturn(true);
        assertEquals(visible, service.getVisible(visible.getId()));

        when(accessService.isHr()).thenReturn(false);
        when(accessService.isAdmin()).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                com.zodiakk.analyticservice.security.principal.CurrentUser.builder()
                        .userId(requester).role("ROLE_EMPLOYEE").email("a@b.c").build(),
                null
        ));
        assertEquals(visible, service.getVisible(visible.getId()));
        assertThrows(IllegalStateException.class, () -> service.read("{", Object.class));
    }

    @Test
    void throwsWhenJobIsMissing() {
        UUID id = UUID.randomUUID();
        when(jobRepository.findByIdForUpdate(id)).thenReturn(Optional.empty());
        when(jobRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(JobNotFoundException.class, () -> service.complete(id, UUID.randomUUID(), null));
        assertThrows(JobNotFoundException.class, () -> service.fail(id, "X", "nope"));
        assertThrows(JobNotFoundException.class, () -> service.getVisible(id));
    }

    private AnalyticsJob job(AnalyticsJobStatus status, int attempts) {
        return AnalyticsJob.builder()
                .id(UUID.randomUUID())
                .jobType(AnalyticsJobType.CAREER_ASSESSMENT)
                .status(status)
                .requestedBy(UUID.randomUUID())
                .parameters("{}")
                .attempts(attempts)
                .maxAttempts(3)
                .createdAt(OffsetDateTime.now())
                .build();
    }
}
