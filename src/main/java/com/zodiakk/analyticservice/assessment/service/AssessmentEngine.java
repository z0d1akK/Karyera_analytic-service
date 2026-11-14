package com.zodiakk.analyticservice.assessment.service;

import com.zodiakk.analyticservice.assessment.calculation.AssessmentCalculator;
import com.zodiakk.analyticservice.assessment.calculation.AssessmentScore;
import com.zodiakk.analyticservice.assessment.calculation.DateInterval;
import com.zodiakk.analyticservice.assessment.calculation.DecimalScale;
import com.zodiakk.analyticservice.assessment.calculation.ExperienceCalculator;
import com.zodiakk.analyticservice.assessment.calculation.InvalidAssessmentInputException;
import com.zodiakk.analyticservice.assessment.calculation.ProjectActivityCalculator;
import com.zodiakk.analyticservice.assessment.calculation.ScoreComponent;
import com.zodiakk.analyticservice.assessment.calculation.SkillFitCalculator;
import com.zodiakk.analyticservice.assessment.calculation.SkillGapCalculator;
import com.zodiakk.analyticservice.assessment.calculation.SkillGapResult;
import com.zodiakk.analyticservice.assessment.calculation.SkillRequirementData;
import com.zodiakk.analyticservice.assessment.entity.AssessmentComponent;
import com.zodiakk.analyticservice.assessment.entity.AssessmentSkillGap;
import com.zodiakk.analyticservice.assessment.entity.CareerAssessment;
import com.zodiakk.analyticservice.assessment.repository.AssessmentComponentRepository;
import com.zodiakk.analyticservice.assessment.repository.AssessmentSkillGapRepository;
import com.zodiakk.analyticservice.assessment.repository.CareerAssessmentRepository;
import com.zodiakk.analyticservice.assessment.snapshot.InputSnapshot;
import com.zodiakk.analyticservice.common.enums.AssessmentMode;
import com.zodiakk.analyticservice.common.exception.InsufficientDataException;
import com.zodiakk.analyticservice.config.MethodologyProperties;
import com.zodiakk.analyticservice.integration.client.CareerClient;
import com.zodiakk.analyticservice.integration.client.EmployeeClient;
import com.zodiakk.analyticservice.integration.client.ProjectClient;
import com.zodiakk.analyticservice.integration.dto.CareerProfileDto;
import com.zodiakk.analyticservice.integration.dto.RequirementDto;
import com.zodiakk.analyticservice.integration.dto.TaskStatisticsDto;
import com.zodiakk.analyticservice.integration.support.IntegrationCalls;
import com.zodiakk.analyticservice.common.enums.ScreeningEligibilityStatus;
import com.zodiakk.analyticservice.job.entity.AnalyticsJob;
import com.zodiakk.analyticservice.job.repository.AnalyticsJobRepository;
import com.zodiakk.analyticservice.screening.entity.ScreeningResult;
import com.zodiakk.analyticservice.screening.repository.ScreeningResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssessmentEngine {

    private final EmployeeClient employeeClient;
    private final CareerClient careerClient;
    private final ProjectClient projectClient;
    private final MethodologyProperties methodology;
    private final CareerAssessmentRepository assessmentRepository;
    private final AssessmentSkillGapRepository skillGapRepository;
    private final AssessmentComponentRepository componentRepository;
    private final AnalyticsJobRepository jobRepository;
    private final ScreeningResultRepository screeningResultRepository;
    private final ObjectMapper objectMapper;
    private final SkillFitCalculator skillFitCalculator = new SkillFitCalculator();
    private final SkillGapCalculator skillGapCalculator = new SkillGapCalculator();
    private final ExperienceCalculator experienceCalculator = new ExperienceCalculator();
    private final ProjectActivityCalculator projectActivityCalculator = new ProjectActivityCalculator();
    private final AssessmentCalculator assessmentCalculator = new AssessmentCalculator();
    private final AssessmentReadinessService readinessService;

    @Transactional
    public CareerAssessment executeForExistingJob(
            AnalyticsJob job,
            UUID employeeId,
            UUID targetPositionId,
            UUID targetGradeId,
            UUID requirementId
    ) {
        List<CareerAssessment> existing = assessmentRepository.findAllByJobId(job.getId());

        if (!existing.isEmpty()) {
            return existing.getFirst();
        }

        AssessmentMode mode = AssessmentMode.from(readMode(job));
        return persist(
                job,
                employeeId,
                targetPositionId,
                targetGradeId,
                loadProfile(employeeId),
                loadRequirement(targetPositionId, targetGradeId, requirementId),
                mode
        );
    }

    @Transactional
    public CareerAssessment executeAsChildJob(
            UUID requestedBy,
            UUID employeeId,
            UUID targetPositionId,
            UUID targetGradeId,
            RequirementDto requirement,
            CareerProfileDto profile,
            UUID screeningId
    ) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        AnalyticsJob child = AnalyticsJob.builder()
                .jobType(com.zodiakk.analyticservice.common.enums.AnalyticsJobType.CAREER_ASSESSMENT)
                .status(com.zodiakk.analyticservice.common.enums.AnalyticsJobStatus.COMPLETED)
                .requestedBy(requestedBy)
                .parameters(write(Map.of(
                        "employeeId", employeeId,
                        "targetPositionId", targetPositionId,
                        "targetGradeId", targetGradeId,
                        "requirementId", requirement.requirementId(),
                        "child", true
                )))
                .attempts(1)
                .maxAttempts(1)
                .startedAt(now)
                .completedAt(now)
                .build();
        jobRepository.saveAndFlush(child);

        CareerAssessment assessment = persist(
                child,
                employeeId,
                targetPositionId,
                targetGradeId,
                profile,
                requirement,
                AssessmentMode.FULL
        );
        child.setResultId(assessment.getId());
        jobRepository.save(child);

        if (screeningId != null && screeningResultRepository.findByScreeningIdAndEmployeeId(screeningId, employeeId).isEmpty()) {
            screeningResultRepository.save(ScreeningResult.builder()
                    .screeningId(screeningId)
                    .employeeId(employeeId)
                    .assessmentId(assessment.getId())
                    .score(assessment.getTotalScore())
                    .eligibilityStatus(Boolean.TRUE.equals(assessment.getMandatoryRequirementsMet())
                            ? ScreeningEligibilityStatus.ELIGIBLE
                            : ScreeningEligibilityStatus.GAPS_PRESENT)
                    .mandatoryRequirementsMet(assessment.getMandatoryRequirementsMet())
                    .build());
        }

        return assessment;
    }

    public CareerProfileDto loadProfile(UUID employeeId) {
        return IntegrationCalls.required(
                () -> employeeClient.careerProfile(employeeId),
                "Профиль сотрудника не найден"
        );
    }

    public RequirementDto loadRequirement(UUID positionId, UUID gradeId, UUID requirementId) {
        RequirementDto requirement = requirementId == null
                ? IntegrationCalls.required(
                () -> careerClient.currentRequirement(positionId, gradeId),
                "Опубликованные требования к должности не найдены"
        )
                : IntegrationCalls.required(
                () -> careerClient.requirement(requirementId),
                "Версия требований не найдена"
        );

        if (requirement.skills() == null || requirement.skills().isEmpty()) {
            throw new InsufficientDataException("В требованиях нет навыков");
        }

        if (requirement.minExperienceMonths() == null) {
            throw new InsufficientDataException("В требованиях не задан минимальный опыт");
        }

        if (!positionId.equals(requirement.positionId()) || !gradeId.equals(requirement.gradeId())) {
            throw new InsufficientDataException("Требования не соответствуют целевой должности и грейду");
        }

        return requirement;
    }

    private String readMode(AnalyticsJob job) {
        try {
            var node = objectMapper.readTree(job.getParameters());
            return node.get("mode") == null ? "FULL" : node.get("mode").asString("FULL");
        } catch (RuntimeException ex) {
            return "FULL";
        }
    }

    private CareerAssessment persist(
            AnalyticsJob job,
            UUID employeeId,
            UUID targetPositionId,
            UUID targetGradeId,
            CareerProfileDto profile,
            RequirementDto requirement,
            AssessmentMode mode
    ) {
        Map<UUID, Integer> levels = new LinkedHashMap<>();
        Map<UUID, String> skillNames = new LinkedHashMap<>();

        if (profile.skills() != null) {
            for (CareerProfileDto.SkillLevel skill : profile.skills()) {
                levels.put(skill.skillId(), skill.level());
                skillNames.put(skill.skillId(), skill.skillName());
            }
        }

        List<SkillRequirementData> requirements = requirement.skills().stream()
                .map(skill -> new SkillRequirementData(
                        skill.skillId(),
                        skill.requiredLevel() == null ? 0 : skill.requiredLevel(),
                        skill.weight(),
                        Boolean.TRUE.equals(skill.mandatory())
                ))
                .sorted(Comparator.comparing(SkillRequirementData::skillId))
                .toList();

        BigDecimal skillScore;
        List<SkillGapResult> gaps;

        try {
            skillScore = skillFitCalculator.calculate(requirements, levels);
            gaps = skillGapCalculator.calculate(requirements, levels);
        } catch (InvalidAssessmentInputException ex) {
            throw new InsufficientDataException(ex.getMessage());
        }

        boolean experienceAvailable = profile.experiencePeriods() != null && !profile.experiencePeriods().isEmpty();
        BigDecimal coveredMonths = null;
        BigDecimal experienceScore = null;

        if (experienceAvailable) {
            List<DateInterval> periods = profile.experiencePeriods().stream()
                    .map(period -> new DateInterval(period.startDate(), period.endDate()))
                    .toList();
            coveredMonths = experienceCalculator.coveredMonths(periods, LocalDate.now(ZoneOffset.UTC));
            experienceScore = experienceCalculator.score(coveredMonths, requirement.minExperienceMonths());
        }

        TaskStatisticsDto statistics;
        try {
            statistics = projectClient.taskStatistics(employeeId, null, null);
        } catch (RuntimeException ex) {
            statistics = null;
        }
        Map<String, Long> complexity = statistics == null || statistics.completedByComplexity() == null
                ? Map.of()
                : statistics.completedByComplexity();
        int mandatoryCount = (int) requirements.stream().filter(SkillRequirementData::mandatory).count();
        BigDecimal projectScore = statistics == null
                ? null
                : projectActivityCalculator.score(complexity, mandatoryCount, requirements.size());

        readinessService.require(mode, readinessService.evaluate(profile, requirement, statistics));

        BigDecimal skillWeight;
        BigDecimal experienceWeight;
        BigDecimal projectWeight;
        BigDecimal appliedExperience = experienceScore;
        BigDecimal appliedProject = projectScore;
        switch (mode) {
            case SKILLS -> {
                skillWeight = java.math.BigDecimal.ONE;
                experienceWeight = java.math.BigDecimal.ZERO;
                projectWeight = java.math.BigDecimal.ZERO;
                appliedExperience = null;
                appliedProject = null;
            }
            case SKILLS_AND_EXPERIENCE -> {
                skillWeight = new java.math.BigDecimal("0.70");
                experienceWeight = new java.math.BigDecimal("0.30");
                projectWeight = java.math.BigDecimal.ZERO;
                appliedProject = null;
            }
            default -> {
                skillWeight = methodology.skillWeight();
                experienceWeight = methodology.experienceWeight();
                projectWeight = methodology.projectWeight();
            }
        }

        AssessmentScore score = assessmentCalculator.calculate(
                skillScore,
                appliedExperience,
                appliedProject,
                skillWeight,
                experienceWeight,
                projectWeight
        );

        InputSnapshot snapshot = new InputSnapshot(
                methodology.version() + "/" + mode.name(),
                DecimalScale.scale(skillWeight),
                DecimalScale.scale(experienceWeight),
                DecimalScale.scale(projectWeight),
                new InputSnapshot.EmployeeSnapshot(
                        profile.employeeId(),
                        profile.positionId(),
                        profile.gradeId(),
                        profile.active(),
                        profile.dataVersion()
                ),
                new InputSnapshot.RequirementSnapshot(
                        requirement.requirementId(),
                        requirement.revision() == null ? 0 : requirement.revision(),
                        requirement.minExperienceMonths()
                ),
                requirements.stream()
                        .map(item -> new InputSnapshot.SkillSnapshot(
                                item.skillId(),
                                levels.getOrDefault(item.skillId(), 0),
                                item.requiredLevel(),
                                item.weight(),
                                item.mandatory()
                        ))
                        .toList(),
                new InputSnapshot.ExperienceSnapshot(coveredMonths, experienceAvailable),
                new InputSnapshot.ProjectSnapshot(statistics != null, complexity)
        );
        String json = write(snapshot);

        CareerAssessment assessment = CareerAssessment.builder()
                .jobId(job.getId())
                .employeeId(employeeId)
                .targetPositionId(targetPositionId)
                .targetGradeId(targetGradeId)
                .requirementId(requirement.requirementId())
                .requirementRevision(requirement.revision() == null ? 0 : requirement.revision())
                .methodologyVersion(methodology.version())
                .skillScore(skillScore)
                .experienceScore(experienceScore)
                .totalScore(score.totalScore())
                .mandatoryRequirementsMet(skillGapCalculator.mandatoryRequirementsMet(gaps))
                .dataQualityStatus(score.dataQualityStatus())
                .inputSnapshot(json)
                .inputHash(sha256(json))
                .isStale(false)
                .calculatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        assessmentRepository.saveAndFlush(assessment);

        for (SkillGapResult gap : gaps) {
            skillGapRepository.save(AssessmentSkillGap.builder()
                    .assessmentId(assessment.getId())
                    .skillId(gap.skillId())
                    .actualLevel(gap.actualLevel())
                    .requiredLevel(gap.requiredLevel())
                    .weight(gap.weight())
                    .absoluteGap(gap.absoluteGap())
                    .normalizedGap(gap.normalizedGap())
                    .priorityScore(gap.priorityScore())
                    .mandatory(gap.mandatory())
                    .requirementMet(gap.requirementMet())
                    .build());
        }

        for (ScoreComponent component : score.components()) {
            componentRepository.save(AssessmentComponent.builder()
                    .assessmentId(assessment.getId())
                    .componentCode(component.code())
                    .rawScore(component.rawScore())
                    .coefficient(component.coefficient())
                    .weightedScore(component.weightedScore())
                    .details(skillNames.isEmpty() ? null : write(Map.of("skillNames", skillNames)))
                    .build());
        }

        return assessment;
    }

    public String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Не удалось сериализовать данные оценки", ex);
        }
    }

    public static String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
