package com.zodiakk.analyticservice.assessment.service;

import com.zodiakk.analyticservice.assessment.calculation.DateInterval;
import com.zodiakk.analyticservice.assessment.calculation.ExperienceCalculator;
import com.zodiakk.analyticservice.common.enums.AssessmentMode;
import com.zodiakk.analyticservice.common.exception.InsufficientDataException;
import com.zodiakk.analyticservice.integration.client.CareerClient;
import com.zodiakk.analyticservice.integration.client.EmployeeClient;
import com.zodiakk.analyticservice.integration.client.ProjectClient;
import com.zodiakk.analyticservice.integration.dto.CareerProfileDto;
import com.zodiakk.analyticservice.integration.dto.RequirementDto;
import com.zodiakk.analyticservice.integration.dto.TaskStatisticsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssessmentReadinessService {

    public static final String SKILLS = "SKILLS";

    public static final String EXPERIENCE = "EXPERIENCE";

    public static final String PROJECTS = "PROJECTS";

    private final EmployeeClient employeeClient;

    private final CareerClient careerClient;

    private final ProjectClient projectClient;

    private final ExperienceCalculator experienceCalculator = new ExperienceCalculator();

    public void require(
            AssessmentMode mode,
            UUID employeeId,
            UUID positionId,
            UUID gradeId,
            UUID requirementId
    ) {
        require(mode, load(employeeId, positionId, gradeId, requirementId));
    }

    public void require(AssessmentMode mode, Snapshot snapshot) {
        List<String> missing = missingFor(mode, snapshot);
        if (missing.isEmpty()) {
            return;
        }
        List<String> suggested = suggestedModes(snapshot);
        throw new InsufficientDataException(message(missing, suggested), missing, suggested);
    }

    public Snapshot evaluate(CareerProfileDto profile, RequirementDto requirement, TaskStatisticsDto statistics) {
        return new Snapshot(
                hasMatchingSkills(profile, requirement),
                hasExperience(profile, requirement),
                hasProjects(statistics)
        );
    }

    public Snapshot load(UUID employeeId, UUID positionId, UUID gradeId, UUID requirementId) {
        CareerProfileDto profile;
        try {
            profile = employeeClient.careerProfile(employeeId);
        } catch (RuntimeException ex) {
            return new Snapshot(false, false, false);
        }

        RequirementDto requirement;
        try {
            requirement = requirementId == null
                    ? careerClient.currentRequirement(positionId, gradeId)
                    : careerClient.requirement(requirementId);
        } catch (RuntimeException ex) {
            return new Snapshot(false, hasExperience(profile, null), loadProjects(employeeId));
        }

        return evaluate(profile, requirement, loadStatistics(employeeId));
    }

    public static boolean hasMatchingSkills(CareerProfileDto profile, RequirementDto requirement) {
        if (profile == null || profile.skills() == null || profile.skills().isEmpty()) {
            return false;
        }
        if (requirement == null || requirement.skills() == null || requirement.skills().isEmpty()) {
            return false;
        }
        Set<UUID> required = requirement.skills().stream()
                .map(RequirementDto.Skill::skillId)
                .collect(Collectors.toCollection(HashSet::new));
        return profile.skills().stream().anyMatch(skill -> required.contains(skill.skillId()));
    }

    public boolean hasExperience(CareerProfileDto profile, RequirementDto requirement) {
        if (profile == null || profile.experiencePeriods() == null || profile.experiencePeriods().isEmpty()) {
            return false;
        }
        List<DateInterval> periods = profile.experiencePeriods().stream()
                .map(period -> new DateInterval(period.startDate(), period.endDate()))
                .toList();
        BigDecimal covered = experienceCalculator.coveredMonths(periods, LocalDate.now(ZoneOffset.UTC));
        int minMonths = requirement == null || requirement.minExperienceMonths() == null
                ? 0
                : requirement.minExperienceMonths();
        if (minMonths > 0 && covered.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        return true;
    }

    public static boolean hasProjects(TaskStatisticsDto statistics) {
        if (statistics == null) {
            return false;
        }
        if (statistics.completedTasks() > 0) {
            return true;
        }
        Map<String, Long> complexity = statistics.completedByComplexity();
        if (complexity == null || complexity.isEmpty()) {
            return false;
        }
        return complexity.values().stream().anyMatch(value -> value != null && value > 0);
    }

    private boolean loadProjects(UUID employeeId) {
        return hasProjects(loadStatistics(employeeId));
    }

    private TaskStatisticsDto loadStatistics(UUID employeeId) {
        try {
            return projectClient.taskStatistics(employeeId, null, null);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private List<String> missingFor(AssessmentMode mode, Snapshot snapshot) {
        List<String> missing = new ArrayList<>();
        if (!snapshot.skills()) {
            missing.add(SKILLS);
        }
        if (mode == AssessmentMode.SKILLS_AND_EXPERIENCE || mode == AssessmentMode.FULL) {
            if (!snapshot.experience()) {
                missing.add(EXPERIENCE);
            }
        }
        if (mode == AssessmentMode.FULL && !snapshot.projects()) {
            missing.add(PROJECTS);
        }
        return missing;
    }

    private List<String> suggestedModes(Snapshot snapshot) {
        if (!snapshot.skills()) {
            return List.of();
        }
        List<String> modes = new ArrayList<>();
        modes.add(AssessmentMode.SKILLS.name());
        if (snapshot.experience()) {
            modes.add(AssessmentMode.SKILLS_AND_EXPERIENCE.name());
        }
        if (snapshot.experience() && snapshot.projects()) {
            modes.add(AssessmentMode.FULL.name());
        }
        return List.copyOf(modes);
    }

    private String message(List<String> missing, List<String> suggested) {
        String lack = missing.stream().map(this::componentLabel).collect(Collectors.joining(", "));
        if (suggested.isEmpty()) {
            return "Недостаточно данных: " + lack + ". Ни один режим оценки недоступен.";
        }
        String modes = suggested.stream()
                .map(this::modeLabel)
                .map(label -> "«" + label + "»")
                .collect(Collectors.joining(" или "));
        return "Недостаточно данных: " + lack + ". Выберите режим " + modes + ".";
    }

    private String componentLabel(String code) {
        return switch (code) {
            case SKILLS -> "навыки";
            case EXPERIENCE -> "опыт";
            case PROJECTS -> "проектная активность";
            default -> code;
        };
    }

    private String modeLabel(String mode) {
        return switch (mode) {
            case "SKILLS" -> "Навыки";
            case "SKILLS_AND_EXPERIENCE" -> "Навыки и опыт";
            case "FULL" -> "Полная оценка";
            default -> mode;
        };
    }

    public record Snapshot(boolean skills, boolean experience, boolean projects) {
    }
}
