package com.zodiakk.analyticservice;

import com.zodiakk.analyticservice.common.AbstractIntegrationTest;
import com.zodiakk.analyticservice.integration.consumer.InboundEventConsumer;
import com.zodiakk.analyticservice.integration.dto.EmployeeLookupItemDto;
import com.zodiakk.analyticservice.job.worker.AnalyticsJobWorker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Analytics API")
class AnalyticsFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AnalyticsJobWorker worker;

    @Autowired
    private InboundEventConsumer consumer;

    @Test
    @DisplayName("rejects anonymous and incomplete callers and serves health")
    void shouldProtectApi() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/dashboard/overview"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/analytics/dashboard/overview")
                        .header("X-User-Id", "not-a-uuid")
                        .header("X-User-Role", "HR")
                        .header("X-User-Email", "hr@company.com"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/analytics/dashboard/overview")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "HR"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        mockMvc.perform(options("/api/v1/analytics/assessments")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/internal/v1/analytics/talent-reserve/screenings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/internal/v1/analytics/talent-reserve/screenings")
                        .header("X-Internal-Key", "wrong")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("assesses an employee, explains the score and keeps history")
    void shouldAssessEmployee() throws Exception {
        UUID userId = UUID.randomUUID();
        EmployeeLookupItemDto employee = registerEmployee(userId);
        String body = assessmentBody(employee.id(), true);

        MvcResult accepted = mockMvc.perform(asUser(post("/api/v1/analytics/assessments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "assessment-1"), userId, "EMPLOYEE")
                        .content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        mockMvc.perform(asUser(post("/api/v1/analytics/assessments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "assessment-1"), userId, "EMPLOYEE")
                        .content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value(json(accepted, "jobId")));

        worker.poll();

        String jobId = json(accepted, "jobId");
        MvcResult job = mockMvc.perform(asUser(get("/api/v1/analytics/jobs/{id}", jobId), userId, "EMPLOYEE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.progressPercent").value(100))
                .andReturn();
        String assessmentId = json(job, "resultId");

        mockMvc.perform(asUser(get("/api/v1/analytics/assessments/{id}", assessmentId), userId, "EMPLOYEE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(employee.id().toString()))
                .andExpect(jsonPath("$.skillScore").value(0.25))
                .andExpect(jsonPath("$.dataQualityStatus").value("COMPLETE"))
                .andExpect(jsonPath("$.mandatoryRequirementsMet").value(false))
                .andExpect(jsonPath("$.skillGaps[0].skillId").value(SKILL_ID.toString()));

        mockMvc.perform(asUser(get("/api/v1/analytics/assessments/{id}/skill-gaps", assessmentId), userId, "EMPLOYEE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].absoluteGap").value(3));

        mockMvc.perform(asUser(get("/api/v1/analytics/employees/{id}/assessments", employee.id()), userId, "EMPLOYEE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(assessmentId));

        mockMvc.perform(asUser(get("/api/v1/analytics/assessments/{id}/recommendations", assessmentId), userId, "EMPLOYEE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].modelIdentifier").value("explainer-v1"));

        mockMvc.perform(asUser(get("/api/v1/analytics/reports"), userId, "EMPLOYEE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("EMPLOYEE_PROFILE"));
        mockMvc.perform(asUser(get("/api/v1/analytics/reports/EMPLOYEE_PROFILE/preview")
                        .param("employeeId", employee.id().toString()), userId, "EMPLOYEE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Профиль сотрудника"))
                .andExpect(jsonPath("$.sections").isArray());
        mockMvc.perform(asUser(get("/api/v1/analytics/reports/POSITION_FIT")
                        .param("assessmentId", assessmentId), userId, "EMPLOYEE"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_PDF));
        mockMvc.perform(asUser(get("/api/v1/analytics/reports/DEVELOPMENT_PLAN/preview")
                        .param("employeeId", employee.id().toString()), userId, "EMPLOYEE"))
                .andExpect(status().isOk());

        UUID stranger = UUID.randomUUID();
        registerEmployee(stranger);
        mockMvc.perform(asUser(get("/api/v1/analytics/assessments/{id}", assessmentId), stranger, "EMPLOYEE"))
                .andExpect(status().isForbidden());

        mockMvc.perform(asUser(post("/api/v1/analytics/assessments")
                        .contentType(MediaType.APPLICATION_JSON), userId, "EMPLOYEE")
                        .content("""
                                {"employeeId":"%s","targetPositionId":"%s","targetGradeId":"%s","mode":"FULL","requirementId":"%s"}
                                """.formatted(employee.id(), POSITION_ID, GRADE_ID, REQUIREMENT_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("keeps the numeric assessment when the explainer is down")
    void shouldKeepAssessmentWithoutRecommendations() throws Exception {
        intelligenceUnavailable = true;
        UUID userId = UUID.randomUUID();
        EmployeeLookupItemDto employee = registerEmployee(userId, false);

        MvcResult accepted = mockMvc.perform(asUser(post("/api/v1/analytics/assessments")
                        .contentType(MediaType.APPLICATION_JSON), userId, "EMPLOYEE")
                        .content("""
                                {"employeeId":"%s","targetPositionId":"%s","targetGradeId":"%s","mode":"SKILLS","generateRecommendations":true}
                                """.formatted(employee.id(), POSITION_ID, GRADE_ID)))
                .andExpect(status().isAccepted())
                .andReturn();
        worker.poll();

        String assessmentId = json(mockMvc.perform(asUser(get("/api/v1/analytics/jobs/{id}", json(accepted, "jobId")), userId, "EMPLOYEE"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andReturn(), "resultId");

        mockMvc.perform(asUser(get("/api/v1/analytics/assessments/{id}", assessmentId), userId, "EMPLOYEE"))
                .andExpect(jsonPath("$.dataQualityStatus").value("PARTIAL"));
        mockMvc.perform(asUser(get("/api/v1/analytics/assessments/{id}/recommendations", assessmentId), userId, "EMPLOYEE"))
                .andExpect(jsonPath("$[0].status").value("FALLBACK"))
                .andExpect(jsonPath("$[0].content").isNotEmpty());

        mockMvc.perform(asUser(post("/api/v1/analytics/assessments/{id}/recommendations", assessmentId), userId, "EMPLOYEE"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("FALLBACK"))
                .andExpect(jsonPath("$.content").isNotEmpty());
    }

    @Test
    @DisplayName("screens the active staff and ranks only scored employees")
    void shouldScreenReserve() throws Exception {
        UUID hrUser = UUID.randomUUID();
        registerEmployee(hrUser);
        UUID employeeUser = UUID.randomUUID();
        registerEmployee(employeeUser);
        registerEmployee(UUID.randomUUID());

        MvcResult accepted = mockMvc.perform(asInternal(post("/internal/v1/analytics/talent-reserve/screenings")
                        .contentType(MediaType.APPLICATION_JSON))
                        .content("""
                                {"reserveId":"%s","targetPositionId":"%s","targetGradeId":"%s","requirementId":"%s"}
                                """.formatted(RESERVE_ID, POSITION_ID, GRADE_ID, REQUIREMENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        worker.poll();

        mockMvc.perform(asInternal(get("/internal/v1/analytics/talent-reserve/screenings/{id}", json(accepted, "jobId"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.candidateCount").value(3))
                .andExpect(jsonPath("$.results[0].rank").value(1));

        mockMvc.perform(asUser(post("/api/v1/analytics/talent-screenings")
                        .contentType(MediaType.APPLICATION_JSON), hrUser, "HR")
                        .content("""
                                {"reserveId":"%s","targetPositionId":"%s","targetGradeId":"%s","candidateSelection":{"type":"FILTER","activeOnly":true}}
                                """.formatted(RESERVE_ID, POSITION_ID, GRADE_ID)))
                .andExpect(status().isAccepted());

        mockMvc.perform(asUser(post("/api/v1/analytics/talent-screenings")
                        .contentType(MediaType.APPLICATION_JSON), employeeUser, "EMPLOYEE")
                        .content("""
                                {"reserveId":"%s","targetPositionId":"%s","targetGradeId":"%s","candidateSelection":{"type":"FILTER","activeOnly":true}}
                                """.formatted(RESERVE_ID, POSITION_ID, GRADE_ID)))
                .andExpect(status().isForbidden());

        MvcResult listed = mockMvc.perform(asUser(get("/api/v1/analytics/talent-screenings"), hrUser, "HR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").isNotEmpty())
                .andReturn();
        String screeningId = objectMapper.readTree(listed.getResponse().getContentAsString())
                .get("content").get(0).get("id").asString();

        mockMvc.perform(asUser(get("/api/v1/analytics/talent-screenings/{id}", screeningId), hrUser, "HR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        mockMvc.perform(asUser(get("/api/v1/analytics/talent-screenings/{id}/results", screeningId), hrUser, "HR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rank").value(1));
    }

    @Test
    @DisplayName("compares career options and builds dashboards for HR")
    void shouldCompareOptionsAndDashboards() throws Exception {
        UUID hrUser = UUID.randomUUID();
        registerEmployee(hrUser);
        EmployeeLookupItemDto employee = registerEmployee(UUID.randomUUID());
        UUID managerUser = UUID.randomUUID();
        EmployeeLookupItemDto manager = registerEmployee(managerUser);
        assignManager(manager.id(), employee.id());

        MvcResult accepted = mockMvc.perform(asUser(
                        post("/api/v1/analytics/employees/{id}/career-options", employee.id())
                                .contentType(MediaType.APPLICATION_JSON),
                        hrUser,
                        "HR")
                        .content("""
                                {"trackId":"%s","maxOptions":1}
                                """.formatted(TRACK_ID)))
                .andExpect(status().isAccepted())
                .andReturn();
        worker.poll();

        mockMvc.perform(asUser(get("/api/v1/analytics/jobs/{id}/career-options", json(accepted, "jobId")), hrUser, "HR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options[0].assessmentId").isNotEmpty())
                .andExpect(jsonPath("$.options[0].mandatoryRequirementsMet").value(false));

        mockMvc.perform(asUser(get("/api/v1/analytics/reports/WORKFORCE_POTENTIAL/preview"), hrUser, "HR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").exists());
        mockMvc.perform(asUser(get("/api/v1/analytics/reports/DEVELOPMENT_DYNAMICS/preview")
                        .param("employeeId", employee.id().toString())
                        .param("periodFrom", "2026-01-01")
                        .param("periodTo", "2026-12-31"), hrUser, "HR"))
                .andExpect(status().isOk());
        mockMvc.perform(asUser(get("/api/v1/analytics/dashboard/overview"), hrUser, "HR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assessedEmployees").value(1));
        mockMvc.perform(asUser(get("/api/v1/analytics/dashboard/skills"), hrUser, "HR"))
                .andExpect(status().isOk());
        mockMvc.perform(asUser(get("/api/v1/analytics/dashboard/career-readiness"), hrUser, "HR"))
                .andExpect(status().isOk());
        mockMvc.perform(asUser(get("/api/v1/analytics/dashboard/development"), hrUser, "HR"))
                .andExpect(status().isOk());
        mockMvc.perform(asUser(get("/api/v1/analytics/dashboard/project-activity"), hrUser, "HR"))
                .andExpect(status().isOk());
        mockMvc.perform(asUser(get("/api/v1/analytics/employees/{id}/development-dynamics", employee.id()), managerUser, "MANAGER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points").isArray());

        mockMvc.perform(asUser(post("/api/v1/analytics/assessments")
                        .contentType(MediaType.APPLICATION_JSON), UUID.randomUUID(), "EMPLOYEE")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("marks an assessment stale and rebuilds activity from events")
    void shouldConsumeEvents() throws Exception {
        UUID userId = UUID.randomUUID();
        EmployeeLookupItemDto employee = registerEmployee(userId);
        MvcResult accepted = mockMvc.perform(asUser(post("/api/v1/analytics/assessments")
                        .contentType(MediaType.APPLICATION_JSON), userId, "EMPLOYEE")
                        .content(assessmentBody(employee.id(), false)))
                .andExpect(status().isAccepted())
                .andReturn();
        worker.poll();
        String assessmentId = json(mockMvc.perform(asUser(get("/api/v1/analytics/jobs/{id}", json(accepted, "jobId")), userId, "EMPLOYEE"))
                .andReturn(), "resultId");

        consumer.consumeEmployee("""
                {"eventId":"%s","aggregateId":"%s","eventType":"EmployeeSkillUpdated","aggregateType":"EMPLOYEE","eventVersion":1,"occurredAt":"2026-10-21T12:00:00Z","payload":{"employeeId":"%s"}}
                """.formatted(UUID.randomUUID(), employee.id(), employee.id()));

        mockMvc.perform(asUser(get("/api/v1/analytics/assessments/{id}", assessmentId), userId, "EMPLOYEE"))
                .andExpect(jsonPath("$.isStale").value(true));

        consumer.consumeEmployee("""
                {"eventId":"%s","aggregateId":"%s","eventType":"EmployeePositionChanged","aggregateType":"EMPLOYEE","eventVersion":1,"occurredAt":"2026-10-21T12:00:00Z","payload":{"employeeId":"%s","positionId":"%s","gradeId":"%s"}}
                """.formatted(UUID.randomUUID(), employee.id(), employee.id(), POSITION_ID, GRADE_ID));

        consumer.consumeTime("""
                {"eventId":"%s","aggregateId":"%s","eventType":"TimesheetApproved","aggregateType":"Timesheet","eventVersion":1,"occurredAt":"2026-10-21T12:00:00Z","payload":{"employeeId":"%s","periodStart":"2026-10-01","periodEnd":"2026-10-31"}}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), employee.id()));

        consumer.consumeProject("""
                {"eventId":"%s","aggregateId":"%s","eventType":"ProjectTaskCompleted","aggregateType":"ProjectTask","eventVersion":1,"occurredAt":"2026-10-21T12:00:00Z","payload":{"projectId":"%s","completedAt":"2026-10-21T12:00:00Z"}}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));

        consumer.consumeCareer("""
                {"eventId":"%s","aggregateId":"%s","eventType":"PositionRequirementPublished","aggregateType":"POSITION_REQUIREMENT","eventVersion":1,"occurredAt":"2026-10-21T12:00:00Z","payload":{"positionId":"%s","gradeId":"%s"}}
                """.formatted(UUID.randomUUID(), REQUIREMENT_ID, POSITION_ID, GRADE_ID));

        consumer.consumeProject("""
                {"eventId":"%s","aggregateId":"%s","eventType":"ProjectCreated","aggregateType":"Project","eventVersion":1,"occurredAt":"2026-10-21T12:00:00Z","payload":{}}
                """.formatted(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    @DisplayName("rejects a full assessment when project activity is missing")
    void shouldRejectAssessmentWithoutProjects() throws Exception {
        UUID userId = UUID.randomUUID();
        EmployeeLookupItemDto employee = registerEmployee(userId);
        org.mockito.Mockito.when(projectClient.taskStatistics(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new com.zodiakk.analyticservice.integration.dto.TaskStatisticsDto(
                        employee.id(), java.time.LocalDate.now().minusYears(1), java.time.LocalDate.now(), 0, java.util.Map.of(), java.util.Map.of(), 0
                ));

        mockMvc.perform(asUser(post("/api/v1/analytics/assessments")
                        .contentType(MediaType.APPLICATION_JSON), userId, "EMPLOYEE")
                        .content(assessmentBody(employee.id(), false)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_DATA"))
                .andExpect(jsonPath("$.missing[0]").value("PROJECTS"))
                .andExpect(jsonPath("$.suggestedModes[0]").value("SKILLS"));

        Integer jobs = jdbcTemplate.queryForObject("select count(*) from analytics_jobs", Integer.class);
        org.assertj.core.api.Assertions.assertThat(jobs).isZero();
    }

    @Test
    @DisplayName("keeps screening completed when a candidate profile fails")
    void shouldKeepScreeningWhenCandidateProfileFails() throws Exception {
        UUID hrUser = UUID.randomUUID();
        registerEmployee(hrUser);
        EmployeeLookupItemDto broken = registerEmployee(UUID.randomUUID());
        failingProfiles.add(broken.id());

        MvcResult accepted = mockMvc.perform(asInternal(post("/internal/v1/analytics/talent-reserve/screenings")
                        .contentType(MediaType.APPLICATION_JSON))
                        .content("""
                                {"reserveId":"%s","targetPositionId":"%s","targetGradeId":"%s","requirementId":"%s"}
                                """.formatted(RESERVE_ID, POSITION_ID, GRADE_ID, REQUIREMENT_ID)))
                .andExpect(status().isOk())
                .andReturn();
        worker.poll();

        mockMvc.perform(asInternal(get("/internal/v1/analytics/talent-reserve/screenings/{id}", json(accepted, "jobId"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.results[*].eligibilityStatus", org.hamcrest.Matchers.hasItem("INSUFFICIENT_DATA")));
    }

    private String assessmentBody(UUID employeeId, boolean recommendations) {
        return """
                {"employeeId":"%s","targetPositionId":"%s","targetGradeId":"%s","mode":"FULL","generateRecommendations":%s}
                """.formatted(employeeId, POSITION_ID, GRADE_ID, recommendations);
    }

    private String json(MvcResult result, String field) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get(field).asString();
    }
}
