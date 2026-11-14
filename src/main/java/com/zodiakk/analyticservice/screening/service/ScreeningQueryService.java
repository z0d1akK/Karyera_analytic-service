package com.zodiakk.analyticservice.screening.service;

import com.zodiakk.analyticservice.common.exception.JobNotFoundException;
import com.zodiakk.analyticservice.common.exception.ScreeningNotFoundException;
import com.zodiakk.analyticservice.job.dto.ScreeningJobParameters;
import com.zodiakk.analyticservice.job.entity.AnalyticsJob;
import com.zodiakk.analyticservice.job.repository.AnalyticsJobRepository;
import com.zodiakk.analyticservice.job.service.AnalyticsJobService;
import com.zodiakk.analyticservice.screening.dto.internal.InternalScreeningResponse;
import com.zodiakk.analyticservice.screening.dto.response.ScreeningResultResponseDto;
import com.zodiakk.analyticservice.screening.dto.response.TalentScreeningResponseDto;
import com.zodiakk.analyticservice.screening.entity.ScreeningResult;
import com.zodiakk.analyticservice.screening.entity.TalentScreening;
import com.zodiakk.analyticservice.screening.repository.ScreeningResultRepository;
import com.zodiakk.analyticservice.screening.repository.TalentScreeningRepository;
import com.zodiakk.analyticservice.security.access.AnalyticsAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScreeningQueryService {

    private final TalentScreeningRepository screeningRepository;

    private final ScreeningResultRepository resultRepository;

    private final AnalyticsJobRepository jobRepository;

    private final AnalyticsJobService jobService;

    private final AnalyticsAccessService accessService;

    public TalentScreeningResponseDto get(UUID screeningId) {
        accessService.requireHrOrAdmin();
        TalentScreening screening = screeningRepository.findById(screeningId)
                .orElseThrow(() -> new ScreeningNotFoundException(screeningId));
        return toDto(screening, visibleResults(screening.getId()));
    }

    public Page<TalentScreeningResponseDto> list(int page, int size) {
        accessService.requireHrOrAdmin();
        Page<TalentScreening> screenings = screeningRepository.findAll(PageRequest.of(page, size));
        List<TalentScreeningResponseDto> content = screenings.stream()
                .map(screening -> toDto(screening, List.of()))
                .toList();
        return new PageImpl<>(content, screenings.getPageable(), screenings.getTotalElements());
    }

    public Page<ScreeningResultResponseDto> results(UUID screeningId, int page, int size) {
        TalentScreening screening = screeningRepository.findById(screeningId)
                .orElseThrow(() -> new ScreeningNotFoundException(screeningId));
        List<ScreeningResultResponseDto> visible = visibleResults(screening.getId());
        int from = Math.min(page * size, visible.size());
        int to = Math.min(from + size, visible.size());
        return new PageImpl<>(visible.subList(from, to), PageRequest.of(page, size), visible.size());
    }

    public ScreeningResultResponseDto result(UUID screeningId, UUID employeeId) {
        screeningRepository.findById(screeningId).orElseThrow(() -> new ScreeningNotFoundException(screeningId));
        ScreeningResult result = resultRepository.findByScreeningIdAndEmployeeId(screeningId, employeeId)
                .orElseThrow(() -> new ScreeningNotFoundException(screeningId));
        accessService.requireSelfOrManager(employeeId);

        if (!accessService.isHrOrAdmin() && !accessService.canSeeEmployee(employeeId, accessService.visibleEmployeeIds())) {
            throw new org.springframework.security.access.AccessDeniedException("Нет доступа к результату скрининга");
        }

        return toResult(result);
    }

    public TalentScreeningResponseDto byJob(UUID jobId) {
        if (!accessService.isHr() && !accessService.isManager()) {
            throw new org.springframework.security.access.AccessDeniedException("Нет доступа к скринингу");
        }
        TalentScreening screening = screeningRepository.findAllByJobId(jobId).stream()
                .findFirst()
                .orElseThrow(() -> new JobNotFoundException(jobId));
        return toDto(screening, visibleResults(screening.getId()));
    }

    public InternalScreeningResponse internalByJob(UUID jobId) {
        AnalyticsJob job = jobRepository.findById(jobId).orElseThrow(() -> new JobNotFoundException(jobId));
        TalentScreening screening = screeningRepository.findAllByJobId(jobId).stream().findFirst().orElse(null);
        ScreeningJobParameters parameters = jobService.read(job.getParameters(), ScreeningJobParameters.class);
        List<InternalScreeningResponse.Result> results = screening == null
                ? List.of()
                : resultRepository.findAllByScreeningIdOrderByRankAsc(screening.getId()).stream()
                .map(this::toInternal)
                .toList();

        return new InternalScreeningResponse(
                screening == null ? null : screening.getId(),
                screening == null ? job.getCreatedAt() : screening.getCreatedAt(),
                job.getId(),
                parameters.reserveId(),
                parameters.targetPositionId(),
                parameters.targetGradeId(),
                screening == null ? parameters.requirementId() : screening.getRequirementId(),
                screening == null ? null : screening.getRequirementRevision(),
                screening == null ? null : screening.getMethodologyVersion(),
                screening == null ? job.getStatus().name() : screening.getStatus().name(),
                screening != null
                        ? screening.getCandidateCount()
                        : parameters.candidateIds() == null ? 0 : parameters.candidateIds().size(),
                screening == null ? 0 : screening.getEvaluatedCount(),
                screening == null ? 0 : screening.getFailedCount(),
                screening == null ? job.getCompletedAt() : screening.getCompletedAt(),
                results
        );
    }

    private List<ScreeningResultResponseDto> visibleResults(UUID screeningId) {
        List<UUID> visible = accessService.isHrOrAdmin() ? null : accessService.visibleEmployeeIds();

        if (!accessService.isHrOrAdmin() && visible == null) {
            accessService.requireHrOrAdmin();
        }

        return resultRepository.findAllByScreeningIdOrderByRankAsc(screeningId).stream()
                .filter(result -> visible == null || visible.contains(result.getEmployeeId()))
                .map(this::toResult)
                .toList();
    }

    private TalentScreeningResponseDto toDto(TalentScreening screening, List<ScreeningResultResponseDto> results) {
        return TalentScreeningResponseDto.builder()
                .id(screening.getId())
                .createdAt(screening.getCreatedAt())
                .jobId(screening.getJobId())
                .reserveId(screening.getReserveId())
                .targetPositionId(screening.getTargetPositionId())
                .targetGradeId(screening.getTargetGradeId())
                .requirementId(screening.getRequirementId())
                .requirementRevision(screening.getRequirementRevision())
                .methodologyVersion(screening.getMethodologyVersion())
                .status(screening.getStatus())
                .candidateCount(screening.getCandidateCount())
                .evaluatedCount(screening.getEvaluatedCount())
                .failedCount(screening.getFailedCount())
                .completedAt(screening.getCompletedAt())
                .results(results)
                .explanationText(screening.getExplanationText())
                .build();
    }

    private ScreeningResultResponseDto toResult(ScreeningResult result) {
        return ScreeningResultResponseDto.builder()
                .id(result.getId())
                .createdAt(result.getCreatedAt())
                .screeningId(result.getScreeningId())
                .employeeId(result.getEmployeeId())
                .assessmentId(result.getAssessmentId())
                .rank(result.getRank())
                .score(result.getScore())
                .eligibilityStatus(result.getEligibilityStatus())
                .mandatoryRequirementsMet(result.getMandatoryRequirementsMet())
                .exclusionReason(result.getExclusionReason())
                .build();
    }

    private InternalScreeningResponse.Result toInternal(ScreeningResult result) {
        return new InternalScreeningResponse.Result(
                result.getId(),
                result.getCreatedAt(),
                result.getScreeningId(),
                result.getEmployeeId(),
                result.getAssessmentId(),
                result.getRank(),
                result.getScore(),
                result.getEligibilityStatus().name(),
                result.getMandatoryRequirementsMet(),
                result.getExclusionReason()
        );
    }
}
