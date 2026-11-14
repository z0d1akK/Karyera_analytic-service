package com.zodiakk.analyticservice.common;

import com.zodiakk.analyticservice.TestcontainersConfiguration;
import com.zodiakk.analyticservice.common.constants.Headers;
import com.zodiakk.analyticservice.integration.client.CareerClient;
import com.zodiakk.analyticservice.integration.client.EmployeeClient;
import com.zodiakk.analyticservice.integration.client.IntelligenceClient;
import com.zodiakk.analyticservice.integration.client.ProjectClient;
import com.zodiakk.analyticservice.integration.client.TimeClient;
import com.zodiakk.analyticservice.integration.dto.ActiveEmployeeIdsDto;
import com.zodiakk.analyticservice.integration.dto.ApprovedSummaryDto;
import com.zodiakk.analyticservice.integration.dto.AvailableTargetsDto;
import com.zodiakk.analyticservice.integration.dto.CareerProfileDto;
import com.zodiakk.analyticservice.integration.dto.CareerProfileLookupDto;
import com.zodiakk.analyticservice.integration.dto.EmployeeAccessScopeDto;
import com.zodiakk.analyticservice.integration.dto.EmployeeLookupItemDto;
import com.zodiakk.analyticservice.integration.dto.EmployeeLookupResponseDto;
import com.zodiakk.analyticservice.integration.dto.CareerExplanationResponse;
import com.zodiakk.analyticservice.integration.dto.CareerPathInterpretationResponse;
import com.zodiakk.analyticservice.integration.dto.DevelopmentRecommendationResponse;
import com.zodiakk.analyticservice.integration.dto.IdListDto;
import com.zodiakk.analyticservice.integration.dto.IdsDto;
import com.zodiakk.analyticservice.integration.dto.ReferenceLookupDto;
import com.zodiakk.analyticservice.integration.dto.RequirementDto;
import com.zodiakk.analyticservice.integration.dto.ReserveContextDto;
import com.zodiakk.analyticservice.integration.dto.SubordinateIdsDto;
import com.zodiakk.analyticservice.integration.dto.TaskStatisticsDto;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.lenient;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    protected static final UUID POSITION_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1");

    protected static final UUID GRADE_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa2");

    protected static final UUID SKILL_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa3");

    protected static final UUID REQUIREMENT_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa4");

    protected static final UUID RESERVE_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa5");

    protected static final UUID TRACK_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa6");

    protected static final String INTERNAL_API_KEY = "test-internal-api-key";

    private final Map<UUID, EmployeeLookupItemDto> employeesByUserId = new HashMap<>();

    protected final Map<UUID, CareerProfileDto> profiles = new HashMap<>();

    protected final Set<UUID> failingProfiles = new HashSet<>();

    private final Map<UUID, Set<UUID>> subordinates = new HashMap<>();

    private final Set<UUID> activeIds = new HashSet<>();

    protected boolean intelligenceUnavailable;

    @MockitoBean
    protected EmployeeClient employeeClient;

    @MockitoBean
    protected CareerClient careerClient;

    @MockitoBean
    protected ProjectClient projectClient;

    @MockitoBean
    protected TimeClient timeClient;

    @MockitoBean
    protected IntelligenceClient intelligenceClient;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected CacheManager cacheManager;

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.kafka.bootstrap-servers",
                () -> TestcontainersConfiguration.kafka().getBootstrapServers()
        );
    }

    @BeforeEach
    void resetState() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE ai_recommendations, assessment_components, assessment_skill_gaps,
                    screening_results, talent_screenings, career_assessments, analytics_jobs,
                    employee_activity_aggregates, employee_analytics_profiles,
                    processed_events, outbox_events CASCADE
                """);
        cacheManager.getCacheNames().forEach(name -> {
            if (cacheManager.getCache(name) != null) {
                cacheManager.getCache(name).clear();
            }
        });
        employeesByUserId.clear();
        profiles.clear();
        subordinates.clear();
        activeIds.clear();
        intelligenceUnavailable = false;
        failingProfiles.clear();
        stubClients();
    }

    protected EmployeeLookupItemDto registerEmployee(UUID userId) {
        return registerEmployee(userId, true);
    }

    protected EmployeeLookupItemDto registerEmployee(UUID userId, boolean withExperience) {
        UUID employeeId = UUID.randomUUID();
        EmployeeLookupItemDto item = new EmployeeLookupItemDto(employeeId, "Ivan Petrov", true, POSITION_ID, GRADE_ID, null);
        employeesByUserId.put(userId, item);
        activeIds.add(employeeId);
        profiles.put(employeeId, new CareerProfileDto(
                employeeId,
                POSITION_ID,
                "Backend Developer",
                GRADE_ID,
                "Middle",
                true,
                1L,
                List.of(new CareerProfileDto.SkillLevel(SKILL_ID, "Java", 1)),
                withExperience
                        ? List.of(new CareerProfileDto.ExperiencePeriod(LocalDate.now().minusYears(2), null))
                        : List.of(),
                "Ivan Petrov"
        ));
        return item;
    }

    protected void assignManager(UUID managerEmployeeId, UUID employeeId) {
        subordinates.computeIfAbsent(managerEmployeeId, key -> new HashSet<>()).add(employeeId);
    }

    protected <B extends AbstractMockHttpServletRequestBuilder<B>> B asUser(B builder, UUID userId, String role) {
        return builder
                .header(Headers.USER_ID, userId.toString())
                .header(Headers.USER_ROLE, role)
                .header(Headers.USER_EMAIL, "user-" + userId + "@company.com");
    }

    protected <B extends AbstractMockHttpServletRequestBuilder<B>> B asInternal(B builder) {
        return builder.header(Headers.INTERNAL_KEY, INTERNAL_API_KEY);
    }

    private void stubClients() {
        lenient().when(employeeClient.employeeByUser(any())).thenAnswer(invocation -> {
            EmployeeLookupItemDto item = employeesByUserId.get(invocation.getArgument(0));
            if (item == null) {
                throw notFound("/internal/v1/employees/by-user");
            }
            return item;
        });
        lenient().when(employeeClient.careerProfile(any())).thenAnswer(invocation -> {
            UUID employeeId = invocation.getArgument(0);
            if (failingProfiles.contains(employeeId)) {
                throw serverError("/internal/v1/employees/career-profile");
            }
            CareerProfileDto profile = profiles.get(employeeId);
            if (profile == null) {
                throw notFound("/internal/v1/employees/career-profile");
            }
            return profile;
        });
        lenient().when(employeeClient.careerProfiles(any(IdListDto.class))).thenAnswer(invocation -> {
            IdListDto request = invocation.getArgument(0);
            List<CareerProfileDto> found = request.employeeIds() == null
                    ? List.of()
                    : request.employeeIds().stream().filter(profiles::containsKey).map(profiles::get).toList();
            return new CareerProfileLookupDto(found);
        });
        lenient().when(employeeClient.lookup(any(IdListDto.class))).thenAnswer(invocation -> {
            IdListDto request = invocation.getArgument(0);
            List<EmployeeLookupItemDto> found = request.employeeIds() == null
                    ? List.of()
                    : employeesByUserId.values().stream()
                    .filter(item -> request.employeeIds().contains(item.id()))
                    .toList();
            return new EmployeeLookupResponseDto(found);
        });
        lenient().when(employeeClient.activeIds()).thenAnswer(invocation ->
                new ActiveEmployeeIdsDto(new ArrayList<>(activeIds)));
        lenient().when(employeeClient.filterIds(anyBoolean(), any(), any(), any())).thenAnswer(invocation ->
                new ActiveEmployeeIdsDto(new ArrayList<>(activeIds)));
        lenient().when(employeeClient.accessScope(any(), any())).thenAnswer(invocation -> {
            UUID employeeId = invocation.getArgument(0);
            UUID actorId = invocation.getArgument(1);
            boolean manager = actorId != null && subordinates.getOrDefault(actorId, Set.of()).contains(employeeId);
            return new EmployeeAccessScopeDto(employeeId, actorId, employeeId.equals(actorId), manager, true);
        });
        lenient().when(employeeClient.subordinateIds(any())).thenAnswer(invocation -> {
            UUID managerId = invocation.getArgument(0);
            return new SubordinateIdsDto(managerId, new ArrayList<>(subordinates.getOrDefault(managerId, Set.of())));
        });
        lenient().when(employeeClient.positions(any(IdsDto.class))).thenAnswer(invocation -> referenceLookup(invocation.getArgument(0), "Backend Developer"));
        lenient().when(employeeClient.grades(any(IdsDto.class))).thenAnswer(invocation -> referenceLookup(invocation.getArgument(0), "Middle"));
        lenient().when(employeeClient.skills(any(IdsDto.class))).thenAnswer(invocation -> referenceLookup(invocation.getArgument(0), "Java"));

        RequirementDto requirement = requirement();
        lenient().when(careerClient.currentRequirement(any(), any())).thenReturn(requirement);
        lenient().when(careerClient.requirement(any())).thenReturn(requirement);
        lenient().when(careerClient.reserveContext(any())).thenReturn(new ReserveContextDto(
                RESERVE_ID, "Reserve", "ACTIVE", POSITION_ID, GRADE_ID, LocalDate.now(), null
        ));
        lenient().when(careerClient.developmentPlans(any())).thenReturn(List.of(Map.of(
                "name", "IDP",
                "status", "ACTIVE",
                "periodStart", "2026-01-01",
                "periodEnd", "2026-12-31"
        )));
        lenient().when(careerClient.availableTargets(any(), any())).thenReturn(new AvailableTargetsDto(
                TRACK_ID,
                null,
                null,
                List.of(new AvailableTargetsDto.Target(
                        UUID.randomUUID(), POSITION_ID, GRADE_ID, "Senior", "PROMOTION", 12
                ))
        ));
        lenient().when(projectClient.taskStatistics(any(), any(), any())).thenReturn(new TaskStatisticsDto(
                null, LocalDate.now().minusYears(1), LocalDate.now(), 3, Map.of(), Map.of("HIGH", 2L), 1
        ));
        lenient().when(timeClient.approvedSummary(any(), any(), any())).thenAnswer(invocation ->
                new ApprovedSummaryDto(
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        invocation.getArgument(2),
                        480,
                        List.of(new ApprovedSummaryDto.ProjectMinutes(UUID.randomUUID(), 480))
                ));
        lenient().when(intelligenceClient.explain(any())).thenAnswer(invocation -> {
            if (intelligenceUnavailable) {
                throw new IllegalStateException("intelligence down");
            }
            return new CareerExplanationResponse(
                    UUID.randomUUID(),
                    null,
                    "Нужно подтянуть Java",
                    List.of("Есть опыт"),
                    List.of(),
                    List.of("Не является решением о повышении"),
                    "explainer-v1",
                    1,
                    "COMPLETED"
            );
        });
        lenient().when(intelligenceClient.recommend(any())).thenAnswer(invocation -> {
            if (intelligenceUnavailable) {
                throw new IllegalStateException("intelligence down");
            }
            return new DevelopmentRecommendationResponse(
                    UUID.randomUUID(),
                    List.of(new DevelopmentRecommendationResponse.Item(
                            SKILL_ID,
                            "SELF_STUDY",
                            "Подтянуть Java",
                            "Практика по Java до требуемого уровня.",
                            List.of("Разобрать практический пример"),
                            "Увереннее закрывать требования по навыку"
                    ))
            );
        });
        lenient().when(intelligenceClient.interpret(any())).thenReturn(new CareerPathInterpretationResponse(
                UUID.randomUUID(),
                "Сравнены доступные направления",
                List.of(),
                null,
                null,
                List.of(),
                "explainer-v1",
                1
        ));
    }

    private RequirementDto requirement() {
        return new RequirementDto(
                REQUIREMENT_ID,
                POSITION_ID,
                GRADE_ID,
                3,
                "PUBLISHED",
                12,
                "Backend",
                List.of(new RequirementDto.Skill(SKILL_ID, 4, new BigDecimal("1.00"), true, "Java"))
        );
    }

    private ReferenceLookupDto referenceLookup(IdsDto request, String name) {
        List<UUID> ids = request == null || request.ids() == null || request.ids().isEmpty()
                ? List.of(UUID.randomUUID())
                : request.ids();
        return new ReferenceLookupDto(ids.stream().map(id -> new ReferenceLookupDto.Item(id, name, true)).toList());
    }

    private FeignException.NotFound notFound(String path) {
        return new FeignException.NotFound(
                "missing",
                Request.create(Request.HttpMethod.GET, path, Map.of(), null, StandardCharsets.UTF_8, null),
                null,
                Map.of()
        );
    }

    private FeignException.InternalServerError serverError(String path) {
        return new FeignException.InternalServerError(
                "unavailable",
                Request.create(Request.HttpMethod.GET, path, Map.of(), null, StandardCharsets.UTF_8, null),
                null,
                Map.of()
        );
    }
}
