package com.zodiakk.analyticservice.assessment.controller;

import com.zodiakk.analyticservice.assessment.dto.request.action.CareerAssessmentStartRequestDto;
import com.zodiakk.analyticservice.assessment.dto.request.action.CareerOptionsStartRequestDto;
import com.zodiakk.analyticservice.assessment.dto.response.AssessmentSkillGapResponseDto;
import com.zodiakk.analyticservice.assessment.dto.response.CareerAssessmentResponseDto;
import com.zodiakk.analyticservice.assessment.service.AssessmentQueryService;
import com.zodiakk.analyticservice.assessment.service.AssessmentReadinessService;
import com.zodiakk.analyticservice.common.enums.AnalyticsJobType;
import com.zodiakk.analyticservice.common.enums.AssessmentMode;
import com.zodiakk.analyticservice.job.dto.AssessmentJobParameters;
import com.zodiakk.analyticservice.job.dto.CareerOptionsJobParameters;
import com.zodiakk.analyticservice.job.dto.response.JobAcceptedResponseDto;
import com.zodiakk.analyticservice.job.entity.AnalyticsJob;
import com.zodiakk.analyticservice.job.service.AnalyticsJobService;
import com.zodiakk.analyticservice.security.access.AnalyticsAccessService;
import com.zodiakk.analyticservice.security.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AssessmentController {

    private final AnalyticsJobService jobService;

    private final AssessmentQueryService assessmentQueryService;

    private final AnalyticsAccessService accessService;

    private final AssessmentReadinessService readinessService;

    @PostMapping("/assessments")
    public ResponseEntity<JobAcceptedResponseDto> start(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CareerAssessmentStartRequestDto request
    ) {
        accessService.requireSelfOrManager(request.getEmployeeId());

        if (request.getRequirementId() != null && !accessService.isHr()) {
            throw new AccessDeniedException("Явную версию требований может выбрать только HR");
        }

        AssessmentMode mode = AssessmentMode.from(request.getMode());
        readinessService.require(
                mode,
                request.getEmployeeId(),
                request.getTargetPositionId(),
                request.getTargetGradeId(),
                request.getRequirementId()
        );

        AnalyticsJob job = jobService.create(
                AnalyticsJobType.CAREER_ASSESSMENT,
                SecurityUtils.getCurrentUserId(),
                idempotencyKey,
                new AssessmentJobParameters(
                        request.getEmployeeId(),
                        request.getTargetPositionId(),
                        request.getTargetGradeId(),
                        request.getRequirementId(),
                        Boolean.TRUE.equals(request.getGenerateRecommendations()),
                        request.getMode()
                )
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(accepted(job));
    }

    @GetMapping("/assessments/{id}")
    public CareerAssessmentResponseDto get(@PathVariable UUID id) {
        return assessmentQueryService.get(id);
    }

    @GetMapping("/employees/{id}/assessments")
    public List<CareerAssessmentResponseDto> history(@PathVariable UUID id) {
        return assessmentQueryService.history(id);
    }

    @GetMapping("/assessments/{id}/skill-gaps")
    public List<AssessmentSkillGapResponseDto> skillGaps(@PathVariable UUID id) {
        return assessmentQueryService.skillGaps(id);
    }

    @PostMapping("/employees/{employeeId}/career-options")
    public ResponseEntity<JobAcceptedResponseDto> careerOptions(
            @PathVariable UUID employeeId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CareerOptionsStartRequestDto request
    ) {
        accessService.requireSelfOrManager(employeeId);
        int maxOptions = request.getMaxOptions() == null ? 5 : request.getMaxOptions();
        AnalyticsJob job = jobService.create(
                AnalyticsJobType.CAREER_RECOMMENDATIONS,
                SecurityUtils.getCurrentUserId(),
                idempotencyKey,
                new CareerOptionsJobParameters(employeeId, request.getTrackId(), maxOptions)
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(accepted(job));
    }

    private JobAcceptedResponseDto accepted(AnalyticsJob job) {
        return JobAcceptedResponseDto.builder()
                .jobId(job.getId())
                .status(job.getStatus())
                .statusUrl("/api/v1/analytics/jobs/" + job.getId())
                .build();
    }
}
