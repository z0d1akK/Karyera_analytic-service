package com.zodiakk.analyticservice.projection.service;

import com.zodiakk.analyticservice.assessment.repository.CareerAssessmentRepository;
import com.zodiakk.analyticservice.assessment.service.AssessmentEngine;
import com.zodiakk.analyticservice.integration.dto.CareerProfileDto;
import com.zodiakk.analyticservice.job.entity.AnalyticsJob;
import com.zodiakk.analyticservice.job.service.AnalyticsJobService;
import com.zodiakk.analyticservice.projection.entity.EmployeeAnalyticsProfile;
import com.zodiakk.analyticservice.projection.repository.EmployeeAnalyticsProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileProjectionService {

    private final EmployeeAnalyticsProfileRepository profileRepository;

    private final CareerAssessmentRepository assessmentRepository;

    private final AssessmentEngine assessmentEngine;

    private final AnalyticsJobService jobService;

    @Transactional
    @CacheEvict(cacheNames = {"assessments", "dashboards"}, allEntries = true)
    public void applyPositionChange(JsonNode payload, UUID eventId) {
        UUID employeeId = UUID.fromString(payload.path("employeeId").asString());
        UUID positionId = UUID.fromString(payload.path("positionId").asString());
        UUID gradeId = UUID.fromString(payload.path("gradeId").asString());
        upsert(employeeId, positionId, gradeId, true, eventId, null);
        assessmentRepository.markStaleByEmployee(employeeId);
    }

    @Transactional
    @CacheEvict(cacheNames = {"assessments", "dashboards"}, allEntries = true)
    public int markEmployeeStale(UUID employeeId) {
        return assessmentRepository.markStaleByEmployee(employeeId);
    }

    @Transactional
    @CacheEvict(cacheNames = {"assessments", "dashboards"}, allEntries = true)
    public int markTargetStale(UUID positionId, UUID gradeId) {
        return assessmentRepository.markStaleByTarget(positionId, gradeId);
    }

    @Transactional
    public void refreshFromJob(AnalyticsJob job) {
        UUID employeeId = UUID.fromString(jobService.read(job.getParameters(), JsonNode.class).path("employeeId").asString());
        CareerProfileDto profile = assessmentEngine.loadProfile(employeeId);
        upsert(profile.employeeId(), profile.positionId(), profile.gradeId(), profile.active(), null, profile.dataVersion());
        jobService.complete(job.getId(), profile.employeeId(), null);
    }

    private void upsert(
            UUID employeeId,
            UUID positionId,
            UUID gradeId,
            boolean active,
            UUID eventId,
            Long version
    ) {
        EmployeeAnalyticsProfile profile = profileRepository.findById(employeeId).orElseGet(() ->
                EmployeeAnalyticsProfile.builder().employeeId(employeeId).build()
        );
        profile.setPositionId(positionId);
        profile.setGradeId(gradeId);
        profile.setActive(active);
        profile.setLastSourceEventId(eventId);
        profile.setSourceVersion(version);
        profile.setLastSyncedAt(OffsetDateTime.now(ZoneOffset.UTC));
        profile.setStale(false);
        profileRepository.save(profile);
    }
}
