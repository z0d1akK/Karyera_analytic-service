package com.zodiakk.analyticservice.report;

import com.zodiakk.analyticservice.assessment.dto.response.AssessmentSkillGapResponseDto;
import com.zodiakk.analyticservice.assessment.dto.response.CareerAssessmentResponseDto;
import com.zodiakk.analyticservice.assessment.service.AssessmentQueryService;
import com.zodiakk.analyticservice.common.exception.ReportUnavailableException;
import com.zodiakk.analyticservice.common.enums.DataQualityStatus;
import com.zodiakk.analyticservice.common.enums.RecommendationStatus;
import com.zodiakk.analyticservice.common.enums.ScreeningEligibilityStatus;
import com.zodiakk.analyticservice.dashboard.dto.CareerReadinessDashboardDto;
import com.zodiakk.analyticservice.dashboard.dto.DashboardOverviewDto;
import com.zodiakk.analyticservice.dashboard.dto.DevelopmentDashboardDto;
import com.zodiakk.analyticservice.dashboard.dto.DevelopmentDynamicsDto;
import com.zodiakk.analyticservice.dashboard.dto.SkillDeficitDashboardDto;
import com.zodiakk.analyticservice.dashboard.service.DashboardService;
import com.zodiakk.analyticservice.integration.client.CareerClient;
import com.zodiakk.analyticservice.integration.client.EmployeeClient;
import com.zodiakk.analyticservice.integration.dto.CareerProfileDto;
import com.zodiakk.analyticservice.integration.dto.CareerProfileLookupDto;
import com.zodiakk.analyticservice.integration.dto.IdListDto;
import com.zodiakk.analyticservice.integration.dto.IdsDto;
import com.zodiakk.analyticservice.integration.dto.ReferenceLookupDto;
import com.zodiakk.analyticservice.integration.dto.ReserveContextDto;
import com.zodiakk.analyticservice.recommendation.dto.response.AiRecommendationResponseDto;
import com.zodiakk.analyticservice.security.access.AnalyticsAccessService;
import com.zodiakk.analyticservice.screening.dto.response.ScreeningResultResponseDto;
import com.zodiakk.analyticservice.screening.dto.response.TalentScreeningResponseDto;
import com.zodiakk.analyticservice.screening.entity.TalentScreening;
import com.zodiakk.analyticservice.screening.repository.TalentScreeningRepository;
import com.zodiakk.analyticservice.screening.service.ScreeningQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportServiceTest {

    private final AnalyticsAccessService accessService = mock(AnalyticsAccessService.class);
    private final AssessmentQueryService assessmentQueryService = mock(AssessmentQueryService.class);
    private final DashboardService dashboardService = mock(DashboardService.class);
    private final ScreeningQueryService screeningQueryService = mock(ScreeningQueryService.class);
    private final TalentScreeningRepository screeningRepository = mock(TalentScreeningRepository.class);
    private final EmployeeClient employeeClient = mock(EmployeeClient.class);
    private final CareerClient careerClient = mock(CareerClient.class);
    private ReportService reports;

    private final UUID employeeId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID skillId = UUID.randomUUID();
    private final UUID positionId = UUID.randomUUID();
    private final UUID gradeId = UUID.randomUUID();
    private final UUID reserveId = UUID.randomUUID();
    private final UUID jobId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        reports = new ReportService(
                accessService,
                assessmentQueryService,
                dashboardService,
                screeningQueryService,
                screeningRepository,
                employeeClient,
                careerClient,
                new PdfReportRenderer(),
                new ObjectMapper()
        );
        when(accessService.isHr()).thenReturn(true);
        when(accessService.isManager()).thenReturn(true);
        when(employeeClient.careerProfile(employeeId)).thenReturn(profile());
        when(employeeClient.positions(any(IdsDto.class))).thenReturn(new ReferenceLookupDto(List.of(new ReferenceLookupDto.Item(positionId, "Backend", true))));
        when(employeeClient.grades(any(IdsDto.class))).thenReturn(new ReferenceLookupDto(List.of(new ReferenceLookupDto.Item(gradeId, "Middle", true))));
        when(employeeClient.skills(any(IdsDto.class))).thenReturn(new ReferenceLookupDto(List.of(new ReferenceLookupDto.Item(skillId, "Java", true))));
        when(assessmentQueryService.get(assessmentId)).thenReturn(assessment());
        when(careerClient.developmentPlans(employeeId)).thenReturn(List.of(Map.of("name", "ИПР", "status", "ACTIVE", "periodStart", "2026-01-01", "periodEnd", "2026-12-31")));
        when(careerClient.reserveContext(reserveId)).thenReturn(new ReserveContextDto(reserveId, "Резерв Java", "ACTIVE", positionId, gradeId, LocalDate.now(), null));
        when(screeningRepository.findAllByReserveIdOrderByCreatedAtDesc(reserveId)).thenReturn(List.of(TalentScreening.builder().jobId(jobId).build()));
        when(screeningQueryService.byJob(jobId)).thenReturn(screening());
        when(employeeClient.careerProfiles(any(IdListDto.class))).thenReturn(new CareerProfileLookupDto(List.of(profile())));
        when(dashboardService.overview()).thenReturn(new DashboardOverviewDto(
                2, new BigDecimal("0.500000"), 1, 2, new BigDecimal("0.250000"), 1, skillId, "Java", 4, 1, new BigDecimal("0.400000")
        ));
        when(dashboardService.skills()).thenReturn(new SkillDeficitDashboardDto(List.of(new SkillDeficitDashboardDto.Item(skillId, 4, "Java"))));
        when(dashboardService.readiness()).thenReturn(new CareerReadinessDashboardDto(List.of(new CareerReadinessDashboardDto.Bucket("0.8+", 1))));
        when(dashboardService.dynamics(employeeId)).thenReturn(new DevelopmentDynamicsDto(employeeId, List.of(
                new DevelopmentDynamicsDto.Point(assessmentId, OffsetDateTime.parse("2026-11-01T10:00:00Z"), new BigDecimal("0.4"), new BigDecimal("0.5"), false, false)
        )));
        when(dashboardService.development()).thenReturn(new DevelopmentDashboardDto(List.of(
                new DevelopmentDashboardDto.Point(LocalDate.of(2026, 11, 1), 3, new BigDecimal("0.6"))
        )));
    }

    @Test
    void buildsNamedEmployeeAndFitReports() {
        ReportDocument profileDoc = reports.preview(AnalyticsReportType.EMPLOYEE_PROFILE, employeeId, null, null, null, null, null);
        assertThat(profileDoc.title()).contains("Профиль");
        assertThat(profileDoc.sections().getFirst().paragraphs().getFirst()).contains("Иван Петров");

        ReportDocument fit = reports.preview(AnalyticsReportType.POSITION_FIT, employeeId, assessmentId, null, null, null, null);
        assertThat(fit.sections().stream().anyMatch(section -> "Дефициты".equals(section.heading()))).isTrue();

        byte[] pdf = reports.pdf(AnalyticsReportType.SKILL_GAPS, employeeId, assessmentId, null, null, null, null);
        assertThat(new String(pdf, java.nio.charset.StandardCharsets.ISO_8859_1)).startsWith("%PDF");
    }

    @Test
    void buildsRecommendationsPlansReserveAndDashboards() {
        ReportDocument recs = reports.preview(AnalyticsReportType.CAREER_RECOMMENDATIONS, employeeId, assessmentId, null, null, null, null);
        assertThat(recs.sections().getFirst().table().rows().getFirst().get(1)).contains("Подтянуть Java");

        ReportDocument plans = reports.preview(AnalyticsReportType.DEVELOPMENT_PLAN, employeeId, null, null, null, null, null);
        assertThat(plans.sections().getFirst().table().rows().getFirst().getFirst()).isEqualTo("ИПР");

        ReportDocument reserve = reports.preview(AnalyticsReportType.TALENT_RESERVE, null, null, reserveId, null, null, null);
        assertThat(reserve.sections().getFirst().heading()).isEqualTo("Резерв Java");

        ReportDocument workforce = reports.preview(AnalyticsReportType.WORKFORCE_POTENTIAL, null, null, null, null, null, null);
        assertThat(workforce.sections().getFirst().paragraphs().getFirst()).contains("2");

        ReportDocument dynamics = reports.preview(AnalyticsReportType.DEVELOPMENT_DYNAMICS, employeeId, null, null, null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertThat(dynamics.sections()).isNotEmpty();

        when(accessService.isAdmin()).thenReturn(true);
        ReportDocument admin = reports.preview(AnalyticsReportType.DEVELOPMENT_DYNAMICS, null, null, null, null, null, null);
        assertThat(admin.sections().getLast().table().headers()).contains("Средний балл");
    }

    @Test
    void coversReportFallbacks() {
        assertThatThrownBy(() -> reports.preview(AnalyticsReportType.POSITION_FIT, employeeId, null, null, null, null, null))
                .isInstanceOf(ReportUnavailableException.class);
        org.mockito.Mockito.doReturn(null).when(employeeClient).careerProfile(employeeId);
        assertThatThrownBy(() -> reports.preview(AnalyticsReportType.EMPLOYEE_PROFILE, employeeId, null, null, null, null, null))
                .isInstanceOf(ReportUnavailableException.class);
        org.mockito.Mockito.doThrow(new RuntimeException("down")).when(employeeClient).careerProfile(employeeId);
        org.mockito.Mockito.doThrow(new RuntimeException("plans")).when(careerClient).developmentPlans(employeeId);
        assertThatThrownBy(() -> reports.preview(AnalyticsReportType.DEVELOPMENT_PLAN, employeeId, null, null, null, null, null))
                .isInstanceOf(ReportUnavailableException.class);

        when(accessService.isHr()).thenReturn(false);
        when(accessService.isManager()).thenReturn(false);
        assertThatThrownBy(() -> reports.preview(AnalyticsReportType.TALENT_RESERVE, null, null, reserveId, null, null, null))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

        when(accessService.isHr()).thenReturn(true);
        when(screeningRepository.findAllByReserveIdOrderByCreatedAtDesc(reserveId)).thenReturn(List.of());
        when(careerClient.reserveContext(reserveId)).thenThrow(new RuntimeException("no reserve"));
        assertThatThrownBy(() -> reports.preview(AnalyticsReportType.TALENT_RESERVE, null, null, reserveId, null, null, null))
                .isInstanceOf(ReportUnavailableException.class);

        org.mockito.Mockito.doThrow(new RuntimeException("skills")).when(employeeClient).skills(any(IdsDto.class));
        org.mockito.Mockito.doThrow(new RuntimeException("down")).when(employeeClient).careerProfile(employeeId);
        when(assessmentQueryService.get(assessmentId)).thenReturn(CareerAssessmentResponseDto.builder()
                .id(assessmentId)
                .employeeId(employeeId)
                .targetPositionId(positionId)
                .targetGradeId(gradeId)
                .recommendations(List.of(
                        AiRecommendationResponseDto.builder().status(RecommendationStatus.FAILED).errorCode("EMPTY").build(),
                        AiRecommendationResponseDto.builder().status(RecommendationStatus.COMPLETED).content("plain").build(),
                        AiRecommendationResponseDto.builder().status(RecommendationStatus.COMPLETED).content("{").build()
                ))
                .skillGaps(List.of())
                .build());
        ReportDocument recs = reports.preview(AnalyticsReportType.CAREER_RECOMMENDATIONS, employeeId, assessmentId, null, null, null, null);
        assertThat(recs.sections().getFirst().table().rows()).hasSize(3);

        when(screeningQueryService.byJob(jobId)).thenReturn(TalentScreeningResponseDto.builder()
                .jobId(jobId)
                .results(List.of(
                        ScreeningResultResponseDto.builder().employeeId(employeeId).eligibilityStatus(ScreeningEligibilityStatus.ELIGIBLE).build(),
                        ScreeningResultResponseDto.builder().employeeId(UUID.randomUUID()).eligibilityStatus(ScreeningEligibilityStatus.INSUFFICIENT_DATA).exclusionReason("нет навыков").build(),
                        ScreeningResultResponseDto.builder().employeeId(UUID.randomUUID()).eligibilityStatus(ScreeningEligibilityStatus.ASSESSMENT_FAILED).build(),
                        ScreeningResultResponseDto.builder().employeeId(UUID.randomUUID()).build()
                ))
                .build());
        when(employeeClient.careerProfiles(any(IdListDto.class))).thenThrow(new RuntimeException("lookup"));
        ReportDocument reserve = reports.preview(AnalyticsReportType.TALENT_RESERVE, null, null, reserveId, jobId, null, null);
        assertThat(reserve.sections().getLast().table().rows()).hasSize(4);

        org.mockito.Mockito.doReturn("not-a-list").when(careerClient).developmentPlans(employeeId);
        org.mockito.Mockito.doReturn(new CareerProfileDto(
                employeeId, positionId, "Backend", gradeId, "Middle", true, 1L, List.of(), List.of(), null
        )).when(employeeClient).careerProfile(employeeId);
        ReportDocument plans = reports.preview(AnalyticsReportType.DEVELOPMENT_PLAN, employeeId, null, null, null, null, null);
        assertThat(plans.sections().getFirst().table().rows()).isEmpty();

        when(dashboardService.dynamics(employeeId)).thenReturn(new DevelopmentDynamicsDto(employeeId, List.of(
                new DevelopmentDynamicsDto.Point(assessmentId, OffsetDateTime.parse("2025-01-01T10:00:00Z"), BigDecimal.ONE, BigDecimal.ONE, true, false),
                new DevelopmentDynamicsDto.Point(assessmentId, null, BigDecimal.ONE, BigDecimal.ONE, true, false)
        )));
        ReportDocument filtered = reports.preview(AnalyticsReportType.DEVELOPMENT_DYNAMICS, employeeId, null, null, null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertThat(filtered.sections().getLast().table().rows()).hasSize(1);
    }

    @Test
    void listsReportsByRole() {
        when(accessService.isAdmin()).thenReturn(true);
        assertThat(reports.available()).hasSize(2);
        when(accessService.isAdmin()).thenReturn(false);
        assertThat(reports.available()).extracting(ReportDescriptor::type).contains(AnalyticsReportType.TALENT_RESERVE);
        when(accessService.isHr()).thenReturn(false);
        when(accessService.isManager()).thenReturn(false);
        assertThat(reports.available()).extracting(ReportDescriptor::type).doesNotContain(AnalyticsReportType.TALENT_RESERVE);
    }

    private CareerProfileDto profile() {
        return new CareerProfileDto(
                employeeId,
                positionId,
                "Backend",
                gradeId,
                "Middle",
                true,
                1L,
                List.of(new CareerProfileDto.SkillLevel(skillId, "Java", 2)),
                List.of(new CareerProfileDto.ExperiencePeriod(LocalDate.now().minusYears(2), null)),
                "Иван Петров"
        );
    }

    private CareerAssessmentResponseDto assessment() {
        return CareerAssessmentResponseDto.builder()
                .id(assessmentId)
                .employeeId(employeeId)
                .targetPositionId(positionId)
                .targetGradeId(gradeId)
                .totalScore(new BigDecimal("0.400000"))
                .skillScore(new BigDecimal("0.250000"))
                .experienceScore(new BigDecimal("1.000000"))
                .dataQualityStatus(DataQualityStatus.COMPLETE)
                .skillGaps(List.of(AssessmentSkillGapResponseDto.builder()
                        .skillId(skillId)
                        .actualLevel(2)
                        .requiredLevel(4)
                        .absoluteGap(2)
                        .build()))
                .recommendations(List.of(AiRecommendationResponseDto.builder()
                        .status(RecommendationStatus.COMPLETED)
                        .content("{\"summary\":\"Подтянуть Java\"}")
                        .build()))
                .build();
    }

    private TalentScreeningResponseDto screening() {
        return TalentScreeningResponseDto.builder()
                .jobId(jobId)
                .reserveId(reserveId)
                .candidateCount(1)
                .evaluatedCount(1)
                .results(List.of(ScreeningResultResponseDto.builder()
                        .employeeId(employeeId)
                        .rank(1)
                        .score(new BigDecimal("0.4"))
                        .eligibilityStatus(ScreeningEligibilityStatus.GAPS_PRESENT)
                        .build()))
                .build();
    }
}
