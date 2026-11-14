package com.zodiakk.analyticservice.assessment.service;

import com.zodiakk.analyticservice.common.enums.AssessmentMode;
import com.zodiakk.analyticservice.common.exception.InsufficientDataException;
import com.zodiakk.analyticservice.integration.client.CareerClient;
import com.zodiakk.analyticservice.integration.client.EmployeeClient;
import com.zodiakk.analyticservice.integration.client.ProjectClient;
import com.zodiakk.analyticservice.integration.dto.CareerProfileDto;
import com.zodiakk.analyticservice.integration.dto.RequirementDto;
import com.zodiakk.analyticservice.integration.dto.TaskStatisticsDto;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssessmentReadinessServiceTest {

    private static final UUID EMPLOYEE_ID = UUID.randomUUID();
    private static final UUID POSITION_ID = UUID.randomUUID();
    private static final UUID GRADE_ID = UUID.randomUUID();
    private static final UUID SKILL_ID = UUID.randomUUID();
    private static final UUID OTHER_SKILL = UUID.randomUUID();

    private final EmployeeClient employeeClient = mock(EmployeeClient.class);
    private final CareerClient careerClient = mock(CareerClient.class);
    private final ProjectClient projectClient = mock(ProjectClient.class);
    private AssessmentReadinessService readiness;

    @BeforeEach
    void setUp() {
        readiness = new AssessmentReadinessService(employeeClient, careerClient, projectClient);
        when(careerClient.currentRequirement(POSITION_ID, GRADE_ID)).thenReturn(requirement());
        when(projectClient.taskStatistics(any(), any(), any())).thenReturn(projects(true));
    }

    @Test
    void acceptsFullModeWhenAllComponentsPresent() {
        when(employeeClient.careerProfile(EMPLOYEE_ID)).thenReturn(profile(List.of(javaSkill()), true));
        readiness.require(AssessmentMode.FULL, EMPLOYEE_ID, POSITION_ID, GRADE_ID, null);
        readiness.require(AssessmentMode.SKILLS, new AssessmentReadinessService.Snapshot(true, false, false));
    }

    @Test
    void emptySkillsAreMissing() {
        when(employeeClient.careerProfile(EMPLOYEE_ID)).thenReturn(profile(List.of(), true));
        InsufficientDataException ex = catchInsufficient(AssessmentMode.SKILLS);
        assertThat(ex.getMissing()).containsExactly("SKILLS");
        assertThat(ex.getSuggestedModes()).isEmpty();
    }

    @Test
    void noSkillOverlapIsMissing() {
        when(employeeClient.careerProfile(EMPLOYEE_ID)).thenReturn(profile(
                List.of(new CareerProfileDto.SkillLevel(OTHER_SKILL, "SQL", 3)),
                true
        ));
        assertThat(catchInsufficient(AssessmentMode.SKILLS).getMissing()).contains("SKILLS");
    }

    @Test
    void zeroMonthsAreMissingExperience() {
        when(employeeClient.careerProfile(EMPLOYEE_ID)).thenReturn(profile(List.of(javaSkill()), false));
        InsufficientDataException ex = catchInsufficient(AssessmentMode.SKILLS_AND_EXPERIENCE);
        assertThat(ex.getMissing()).containsExactly("EXPERIENCE");
        assertThat(ex.getSuggestedModes()).containsExactly("SKILLS");
    }

    @Test
    void emptyProjectsAreMissingForFull() {
        when(employeeClient.careerProfile(EMPLOYEE_ID)).thenReturn(profile(List.of(javaSkill()), true));
        when(projectClient.taskStatistics(any(), any(), any())).thenReturn(projects(false));
        InsufficientDataException ex = catchInsufficient(AssessmentMode.FULL);
        assertThat(ex.getMissing()).containsExactly("PROJECTS");
        assertThat(ex.getSuggestedModes()).containsExactly("SKILLS", "SKILLS_AND_EXPERIENCE");
        assertThat(ex.getMessage()).contains("проектная активность").contains("Навыки");
    }

    @Test
    void profileFailureMarksAllComponentsMissing() {
        when(employeeClient.careerProfile(EMPLOYEE_ID)).thenThrow(serverError());
        InsufficientDataException ex = catchInsufficient(AssessmentMode.FULL);
        assertThat(ex.getMissing()).contains("SKILLS", "EXPERIENCE", "PROJECTS");
        assertThat(ex.getSuggestedModes()).isEmpty();
    }

    @Test
    void requirementFailureMarksSkillsMissing() {
        when(employeeClient.careerProfile(EMPLOYEE_ID)).thenReturn(profile(List.of(javaSkill()), true));
        when(careerClient.currentRequirement(POSITION_ID, GRADE_ID)).thenThrow(serverError());
        assertThat(catchInsufficient(AssessmentMode.SKILLS).getMissing()).contains("SKILLS");
    }

    @Test
    void feignFailureMarksProjectsMissing() {
        when(employeeClient.careerProfile(EMPLOYEE_ID)).thenReturn(profile(List.of(javaSkill()), true));
        when(projectClient.taskStatistics(any(), any(), any())).thenThrow(serverError());
        assertThat(catchInsufficient(AssessmentMode.FULL).getMissing()).contains("PROJECTS");
    }

    private InsufficientDataException catchInsufficient(AssessmentMode mode) {
        try {
            readiness.require(mode, EMPLOYEE_ID, POSITION_ID, GRADE_ID, null);
        } catch (InsufficientDataException ex) {
            return ex;
        }
        throw new AssertionError("expected InsufficientDataException");
    }

    private CareerProfileDto profile(List<CareerProfileDto.SkillLevel> skills, boolean withExperience) {
        return new CareerProfileDto(
                EMPLOYEE_ID,
                POSITION_ID,
                "Backend",
                GRADE_ID,
                "Middle",
                true,
                1L,
                skills,
                withExperience
                        ? List.of(new CareerProfileDto.ExperiencePeriod(LocalDate.now().minusYears(2), null))
                        : List.of(),
                "Ivan Petrov"
        );
    }

    private CareerProfileDto.SkillLevel javaSkill() {
        return new CareerProfileDto.SkillLevel(SKILL_ID, "Java", 2);
    }

    private RequirementDto requirement() {
        return new RequirementDto(
                UUID.randomUUID(),
                POSITION_ID,
                GRADE_ID,
                1,
                "PUBLISHED",
                12,
                "Backend",
                List.of(new RequirementDto.Skill(SKILL_ID, 3, new BigDecimal("1.00"), true, "Java"))
        );
    }

    private TaskStatisticsDto projects(boolean present) {
        return new TaskStatisticsDto(
                EMPLOYEE_ID,
                LocalDate.now().minusYears(1),
                LocalDate.now(),
                present ? 2 : 0,
                Map.of(),
                present ? Map.of("HIGH", 2L) : Map.of(),
                present ? 1 : 0
        );
    }

    private FeignException serverError() {
        return new FeignException.InternalServerError(
                "down",
                Request.create(Request.HttpMethod.GET, "/stats", Map.of(), null, StandardCharsets.UTF_8, null),
                null,
                Map.of()
        );
    }
}
