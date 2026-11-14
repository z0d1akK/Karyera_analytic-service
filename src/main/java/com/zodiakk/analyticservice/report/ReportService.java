package com.zodiakk.analyticservice.report;

import com.zodiakk.analyticservice.assessment.dto.response.AssessmentSkillGapResponseDto;
import com.zodiakk.analyticservice.assessment.dto.response.CareerAssessmentResponseDto;
import com.zodiakk.analyticservice.assessment.service.AssessmentQueryService;
import com.zodiakk.analyticservice.common.exception.ReportUnavailableException;
import com.zodiakk.analyticservice.dashboard.dto.CareerReadinessDashboardDto;
import com.zodiakk.analyticservice.dashboard.dto.DashboardOverviewDto;
import com.zodiakk.analyticservice.dashboard.dto.DevelopmentDashboardDto;
import com.zodiakk.analyticservice.dashboard.dto.DevelopmentDynamicsDto;
import com.zodiakk.analyticservice.dashboard.dto.SkillDeficitDashboardDto;
import com.zodiakk.analyticservice.dashboard.service.DashboardService;
import com.zodiakk.analyticservice.integration.client.CareerClient;
import com.zodiakk.analyticservice.integration.client.EmployeeClient;
import com.zodiakk.analyticservice.integration.dto.CareerProfileDto;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final AnalyticsAccessService accessService;
    private final AssessmentQueryService assessmentQueryService;
    private final DashboardService dashboardService;
    private final ScreeningQueryService screeningQueryService;
    private final TalentScreeningRepository screeningRepository;
    private final EmployeeClient employeeClient;
    private final CareerClient careerClient;
    private final PdfReportRenderer pdfReportRenderer;
    private final ObjectMapper objectMapper;
    private final ThreadLocal<ZoneOffset> reportZone = new ThreadLocal<>();

    public List<ReportDescriptor> available() {
        if (accessService.isAdmin()) {
            return List.of(
                    descriptor(AnalyticsReportType.WORKFORCE_POTENTIAL),
                    descriptor(AnalyticsReportType.DEVELOPMENT_DYNAMICS)
            );
        }
        if (accessService.isHr() || accessService.isManager()) {
            return List.of(
                    descriptor(AnalyticsReportType.EMPLOYEE_PROFILE),
                    descriptor(AnalyticsReportType.POSITION_FIT),
                    descriptor(AnalyticsReportType.SKILL_GAPS),
                    descriptor(AnalyticsReportType.CAREER_RECOMMENDATIONS),
                    descriptor(AnalyticsReportType.DEVELOPMENT_PLAN),
                    descriptor(AnalyticsReportType.TALENT_RESERVE),
                    descriptor(AnalyticsReportType.WORKFORCE_POTENTIAL),
                    descriptor(AnalyticsReportType.DEVELOPMENT_DYNAMICS)
            );
        }
        return List.of(
                descriptor(AnalyticsReportType.EMPLOYEE_PROFILE),
                descriptor(AnalyticsReportType.POSITION_FIT),
                descriptor(AnalyticsReportType.SKILL_GAPS),
                descriptor(AnalyticsReportType.CAREER_RECOMMENDATIONS),
                descriptor(AnalyticsReportType.DEVELOPMENT_PLAN)
        );
    }

    public ReportDocument preview(
            AnalyticsReportType type,
            UUID employeeId,
            UUID assessmentId,
            UUID reserveId,
            UUID jobId,
            LocalDate periodFrom,
            LocalDate periodTo
    ) {
        return preview(type, employeeId, assessmentId, reserveId, jobId, periodFrom, periodTo, null);
    }

    public ReportDocument preview(
            AnalyticsReportType type,
            UUID employeeId,
            UUID assessmentId,
            UUID reserveId,
            UUID jobId,
            LocalDate periodFrom,
            LocalDate periodTo,
            Integer zoneOffsetMinutes
    ) {
        return withZone(zoneOffsetMinutes, () -> build(type, employeeId, assessmentId, reserveId, jobId, periodFrom, periodTo));
    }

    public byte[] pdf(
            AnalyticsReportType type,
            UUID employeeId,
            UUID assessmentId,
            UUID reserveId,
            UUID jobId,
            LocalDate periodFrom,
            LocalDate periodTo
    ) {
        return pdf(type, employeeId, assessmentId, reserveId, jobId, periodFrom, periodTo, null);
    }

    public byte[] pdf(
            AnalyticsReportType type,
            UUID employeeId,
            UUID assessmentId,
            UUID reserveId,
            UUID jobId,
            LocalDate periodFrom,
            LocalDate periodTo,
            Integer zoneOffsetMinutes
    ) {
        return withZone(zoneOffsetMinutes, () -> pdfReportRenderer.render(
                build(type, employeeId, assessmentId, reserveId, jobId, periodFrom, periodTo)
        ));
    }

    private ReportDocument build(
            AnalyticsReportType type,
            UUID employeeId,
            UUID assessmentId,
            UUID reserveId,
            UUID jobId,
            LocalDate periodFrom,
            LocalDate periodTo
    ) {
        return switch (type) {
            case EMPLOYEE_PROFILE -> profile(employeeId);
            case POSITION_FIT -> positionFit(assessmentId);
            case SKILL_GAPS -> skillGaps(assessmentId);
            case CAREER_RECOMMENDATIONS -> recommendations(assessmentId);
            case DEVELOPMENT_PLAN -> developmentPlan(employeeId);
            case TALENT_RESERVE -> reserve(reserveId, jobId);
            case WORKFORCE_POTENTIAL -> workforce();
            case DEVELOPMENT_DYNAMICS -> dynamics(employeeId, periodFrom, periodTo);
        };
    }

    private ReportDocument profile(UUID employeeId) {
        CareerProfileDto profile = loadProfile(employeeId);
        List<List<String>> skillRows = profile.skills() == null ? List.of() : profile.skills().stream()
                .map(skill -> List.of(text(skill.skillName()), String.valueOf(skill.level())))
                .toList();
        List<List<String>> experienceRows = profile.experiencePeriods() == null ? List.of() : profile.experiencePeriods().stream()
                .map(period -> List.of(date(period.startDate()), period.endDate() == null ? "по настоящее время" : date(period.endDate())))
                .toList();
        return document(
                title(AnalyticsReportType.EMPLOYEE_PROFILE),
                new ReportDocument.Section("Сотрудник", List.of(
                        "ФИО: " + employeeName(profile),
                        "Должность: " + text(profile.positionName()),
                        "Грейд: " + text(profile.gradeName())
                )),
                new ReportDocument.Section("Навыки", new ReportDocument.Table(List.of("Навык", "Уровень"), skillRows)),
                new ReportDocument.Section("Опыт", new ReportDocument.Table(List.of("Начало", "Окончание"), experienceRows))
        );
    }

    private ReportDocument positionFit(UUID assessmentId) {
        CareerAssessmentResponseDto assessment = loadAssessment(assessmentId);
        CareerProfileDto profile = safeProfile(assessment.getEmployeeId());
        return document(
                title(AnalyticsReportType.POSITION_FIT),
                new ReportDocument.Section("Сотрудник", List.of(
                        "ФИО: " + employeeName(profile, assessment.getEmployeeId()),
                        "Текущая должность: " + (profile == null ? "—" : text(profile.positionName())),
                        "Текущий грейд: " + (profile == null ? "—" : text(profile.gradeName()))
                )),
                new ReportDocument.Section("Целевая должность", List.of(
                        "Должность: " + lookupName(employeeClient.positions(new IdsDto(List.of(assessment.getTargetPositionId()))), assessment.getTargetPositionId()),
                        "Грейд: " + lookupName(employeeClient.grades(new IdsDto(List.of(assessment.getTargetGradeId()))), assessment.getTargetGradeId())
                )),
                new ReportDocument.Section("Результат", List.of(
                        "Итоговый балл: " + text(assessment.getTotalScore()),
                        "Навыки: " + text(assessment.getSkillScore()),
                        "Опыт: " + text(assessment.getExperienceScore()),
                        "Качество данных: " + text(assessment.getDataQualityStatus())
                )),
                gapsSection(assessment)
        );
    }

    private ReportDocument skillGaps(UUID assessmentId) {
        CareerAssessmentResponseDto assessment = loadAssessment(assessmentId);
        return document(title(AnalyticsReportType.SKILL_GAPS), gapsSection(assessment));
    }

    private ReportDocument recommendations(UUID assessmentId) {
        CareerAssessmentResponseDto assessment = loadAssessment(assessmentId);
        List<AiRecommendationResponseDto> items = assessment.getRecommendations() == null ? List.of() : assessment.getRecommendations();
        List<List<String>> rows = items.stream()
                .map(item -> List.of(text(item.getStatus()), recommendationText(item)))
                .toList();
        return document(
                title(AnalyticsReportType.CAREER_RECOMMENDATIONS),
                new ReportDocument.Section("Рекомендации", new ReportDocument.Table(List.of("Статус", "Текст"), rows))
        );
    }

    private ReportDocument developmentPlan(UUID employeeId) {
        UUID id = resolveEmployee(employeeId);
        accessService.requireSelfOrManager(id);
        List<List<String>> rows = List.of();
        try {
            rows = planRows(careerClient.developmentPlans(id));
        } catch (RuntimeException ex) {
            throw new ReportUnavailableException("Планы развития недоступны");
        }
        return document(
                title(AnalyticsReportType.DEVELOPMENT_PLAN),
                new ReportDocument.Section("Планы", new ReportDocument.Table(List.of("Название", "Статус", "Период"), rows))
        );
    }

    private ReportDocument reserve(UUID reserveId, UUID jobId) {
        if (!accessService.isHr() && !accessService.isManager()) {
            throw new org.springframework.security.access.AccessDeniedException("Нет доступа к отчёту о резерве");
        }
        UUID resolvedJob = jobId;
        String reserveName = "Кадровый резерв";
        if (reserveId != null) {
            try {
                ReserveContextDto context = careerClient.reserveContext(reserveId);
                if (context != null && context.name() != null) {
                    reserveName = context.name();
                }
            } catch (RuntimeException ignored) {
                // keep default name
            }
            if (resolvedJob == null) {
                List<TalentScreening> screenings = screeningRepository.findAllByReserveIdOrderByCreatedAtDesc(reserveId);
                if (!screenings.isEmpty()) {
                    resolvedJob = screenings.getFirst().getJobId();
                }
            }
        }
        if (resolvedJob == null) {
            throw new ReportUnavailableException("Для резерва нет завершённого скрининга");
        }
        TalentScreeningResponseDto screening = screeningQueryService.byJob(resolvedJob);
        Map<UUID, String> names = candidateNames(screening.getResults());
        List<List<String>> rows = screening.getResults() == null ? List.of() : screening.getResults().stream()
                .map(result -> List.of(
                        text(result.getRank()),
                        names.getOrDefault(result.getEmployeeId(), "Сотрудник"),
                        text(result.getScore()),
                        eligibility(result)
                ))
                .toList();
        return document(
                title(AnalyticsReportType.TALENT_RESERVE),
                new ReportDocument.Section(reserveName, List.of(
                        "Кандидатов: " + text(screening.getCandidateCount()),
                        "Оценено: " + text(screening.getEvaluatedCount())
                )),
                new ReportDocument.Section("Кандидаты", new ReportDocument.Table(List.of("Место", "Сотрудник", "Балл", "Статус"), rows))
        );
    }

    private ReportDocument workforce() {
        accessService.requireDashboardAccess();
        DashboardOverviewDto overview = dashboardService.overview();
        SkillDeficitDashboardDto skills = dashboardService.skills();
        CareerReadinessDashboardDto readiness = dashboardService.readiness();
        Map<UUID, String> skillNames = skillNames(skills.deficits() == null
                ? List.of()
                : skills.deficits().stream().map(SkillDeficitDashboardDto.Item::skillId).toList());
        List<List<String>> deficitRows = skills.deficits() == null ? List.of() : skills.deficits().stream()
                .map(item -> List.of(skillNames.getOrDefault(item.skillId(), "Навык"), String.valueOf(item.employees())))
                .toList();
        List<List<String>> readinessRows = readiness.buckets() == null ? List.of() : readiness.buckets().stream()
                .map(bucket -> List.of(text(bucket.label()), String.valueOf(bucket.employees())))
                .toList();
        return document(
                title(AnalyticsReportType.WORKFORCE_POTENTIAL),
                new ReportDocument.Section("Сводка", List.of(
                        "Оценённые сотрудники: " + overview.assessedEmployees(),
                        "Средний балл: " + text(overview.averageScore()),
                        "Полные профили: " + overview.employeesWithCompleteData(),
                        "Актуальные оценки: " + overview.freshAssessments()
                )),
                new ReportDocument.Section("Дефициты навыков", new ReportDocument.Table(List.of("Навык", "Сотрудников"), deficitRows)),
                new ReportDocument.Section("Готовность", new ReportDocument.Table(List.of("Диапазон", "Сотрудников"), readinessRows))
        );
    }

    private ReportDocument dynamics(UUID employeeId, LocalDate periodFrom, LocalDate periodTo) {
        accessService.requireDashboardAccess();
        List<ReportDocument.Section> sections = new ArrayList<>();
        if (periodFrom != null || periodTo != null) {
            sections.add(new ReportDocument.Section("Период", List.of(
                    "С: " + (periodFrom == null ? "—" : date(periodFrom)),
                    "По: " + (periodTo == null ? "—" : date(periodTo))
            )));
        }
        if (accessService.isAdmin() && employeeId == null) {
            DevelopmentDashboardDto development = dashboardService.development();
            List<List<String>> rows = development.points() == null ? List.of() : development.points().stream()
                    .filter(point -> inPeriod(point.period(), periodFrom, periodTo))
                    .map(point -> List.of(date(point.period()), String.valueOf(point.assessments()), text(point.averageScore())))
                    .toList();
            sections.add(new ReportDocument.Section("Динамика", new ReportDocument.Table(List.of("Период", "Оценок", "Средний балл"), rows)));
            return document(title(AnalyticsReportType.DEVELOPMENT_DYNAMICS), sections.toArray(ReportDocument.Section[]::new));
        }
        UUID id = employeeId == null ? accessService.currentEmployeeId() : employeeId;
        if (!accessService.isHr()) {
            accessService.requireSelfOrManager(id);
        }
        DevelopmentDynamicsDto dynamics = dashboardService.dynamics(id);
        List<List<String>> rows = dynamics.points() == null ? List.of() : dynamics.points().stream()
                .filter(point -> inPeriod(point.calculatedAt() == null ? null : point.calculatedAt().toLocalDate(), periodFrom, periodTo))
                .map(point -> List.of(
                        dateTime(point.calculatedAt()),
                        text(point.totalScore()),
                        text(point.skillScore())
                ))
                .toList();
        sections.add(new ReportDocument.Section("Динамика", new ReportDocument.Table(List.of("Дата", "Итоговый балл", "Навыки"), rows)));
        return document(title(AnalyticsReportType.DEVELOPMENT_DYNAMICS), sections.toArray(ReportDocument.Section[]::new));
    }

    private ReportDocument.Section gapsSection(CareerAssessmentResponseDto assessment) {
        List<AssessmentSkillGapResponseDto> gaps = assessment.getSkillGaps() == null ? List.of() : assessment.getSkillGaps();
        Map<UUID, String> names = skillNames(gaps.stream().map(AssessmentSkillGapResponseDto::getSkillId).toList());
        List<List<String>> rows = gaps.stream()
                .map(gap -> List.of(
                        names.getOrDefault(gap.getSkillId(), "Навык"),
                        text(gap.getActualLevel()),
                        text(gap.getRequiredLevel()),
                        text(gap.getAbsoluteGap())
                ))
                .toList();
        return new ReportDocument.Section("Дефициты", new ReportDocument.Table(List.of("Навык", "Факт", "Требование", "Дефицит"), rows));
    }

    private CareerAssessmentResponseDto loadAssessment(UUID assessmentId) {
        if (assessmentId == null) {
            throw new ReportUnavailableException("Выберите оценку");
        }
        return assessmentQueryService.get(assessmentId);
    }

    private CareerProfileDto loadProfile(UUID employeeId) {
        UUID id = resolveEmployee(employeeId);
        accessService.requireSelfOrManager(id);
        CareerProfileDto profile = employeeClient.careerProfile(id);
        if (profile == null) {
            throw new ReportUnavailableException("Профиль сотрудника недоступен");
        }
        return profile;
    }

    private CareerProfileDto safeProfile(UUID employeeId) {
        try {
            return employeeClient.careerProfile(employeeId);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private Map<UUID, String> skillNames(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        try {
            return names(employeeClient.skills(new IdsDto(ids)));
        } catch (RuntimeException ex) {
            return Map.of();
        }
    }

    private Map<UUID, String> candidateNames(List<ScreeningResultResponseDto> results) {
        if (results == null || results.isEmpty()) {
            return Map.of();
        }
        try {
            var lookup = employeeClient.careerProfiles(new com.zodiakk.analyticservice.integration.dto.IdListDto(
                    results.stream().map(ScreeningResultResponseDto::getEmployeeId).toList()
            ));
            if (lookup == null || lookup.profiles() == null) {
                return Map.of();
            }
            Map<UUID, String> names = new LinkedHashMap<>();
            for (CareerProfileDto profile : lookup.profiles()) {
                names.put(profile.employeeId(), employeeName(profile));
            }
            return names;
        } catch (RuntimeException ex) {
            return Map.of();
        }
    }

    private Map<UUID, String> names(ReferenceLookupDto lookup) {
        if (lookup == null || lookup.items() == null) {
            return Map.of();
        }
        return lookup.items().stream()
                .filter(item -> item.id() != null)
                .collect(Collectors.toMap(ReferenceLookupDto.Item::id, item -> text(item.name()), (left, right) -> left));
    }

    private String lookupName(ReferenceLookupDto lookup, UUID id) {
        return names(lookup).getOrDefault(id, "—");
    }

    private List<List<String>> planRows(Object raw) {
        List<List<String>> rows = new ArrayList<>();
        if (!(raw instanceof List<?> list)) {
            return rows;
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                rows.add(List.of(
                        text(first(map, "name", "title")),
                        text(first(map, "status")),
                        text(first(map, "periodStart", "startDate")) + " — " + text(first(map, "periodEnd", "endDate"))
                ));
            }
        }
        return rows;
    }

    private Object first(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key) && map.get(key) != null) {
                return map.get(key);
            }
        }
        return null;
    }

    private String recommendationText(AiRecommendationResponseDto item) {
        if (item.getContent() == null || item.getContent().isBlank()) {
            return item.getErrorCode() == null ? "—" : item.getErrorCode();
        }
        try {
            JsonNode node = objectMapper.readTree(item.getContent());
            JsonNode explanation = node == null ? null : node.get("explanation");
            List<String> parts = new ArrayList<>();
            if (explanation != null && explanation.get("summary") != null && !explanation.get("summary").asString("").isBlank()) {
                parts.add(explanation.get("summary").asString());
            } else if (node != null && node.get("summary") != null && !node.get("summary").asString("").isBlank()) {
                parts.add(node.get("summary").asString());
            }
            JsonNode recs = node == null ? null : node.path("development").path("recommendations");
            if (recs != null && recs.isArray()) {
                for (JsonNode rec : recs) {
                    String title = rec.path("title").asString("");
                    String description = rec.path("description").asString("");
                    String line = (title + ". " + description).trim();
                    if (line.startsWith(".")) {
                        line = description.trim();
                    }
                    if (!line.isBlank()) {
                        parts.add(line);
                    }
                }
            }
            if (!parts.isEmpty()) {
                return String.join("\n", parts);
            }
        } catch (RuntimeException ignored) {
            return item.getContent();
        }
        return item.getContent();
    }

    private String eligibility(ScreeningResultResponseDto result) {
        if (result.getEligibilityStatus() == null) {
            return "—";
        }
        return switch (result.getEligibilityStatus()) {
            case ELIGIBLE -> "Подходит";
            case GAPS_PRESENT -> "Есть дефициты";
            case INSUFFICIENT_DATA -> result.getExclusionReason() == null ? "Недостаточно данных" : result.getExclusionReason();
            case ASSESSMENT_FAILED -> "Оценка не выполнена";
        };
    }

    private boolean inPeriod(LocalDate value, LocalDate from, LocalDate to) {
        if (value == null) {
            return true;
        }
        if (from != null && value.isBefore(from)) {
            return false;
        }
        return to == null || !value.isAfter(to);
    }

    private UUID resolveEmployee(UUID employeeId) {
        return employeeId == null ? accessService.currentEmployeeId() : employeeId;
    }

    private ReportDocument document(String title, ReportDocument.Section... sections) {
        ZoneOffset offset = zone();
        String generated = DATE_TIME.format(OffsetDateTime.now(offset));
        String zoneLabel = utcLabel(offset);
        return new ReportDocument(
                title,
                "Сформирован: " + generated + " (" + zoneLabel + ")",
                List.of(sections)
        );
    }

    private <T> T withZone(Integer zoneOffsetMinutes, java.util.function.Supplier<T> action) {
        reportZone.set(resolveOffset(zoneOffsetMinutes));
        try {
            return action.get();
        } finally {
            reportZone.remove();
        }
    }

    private ZoneOffset zone() {
        ZoneOffset offset = reportZone.get();
        return offset == null ? ZoneOffset.ofHours(3) : offset;
    }

    private ZoneOffset resolveOffset(Integer minutes) {
        int value = minutes == null ? 180 : minutes;
        value = Math.max(-12 * 60, Math.min(14 * 60, value));
        return ZoneOffset.ofTotalSeconds(value * 60);
    }

    private String utcLabel(ZoneOffset offset) {
        int total = offset.getTotalSeconds() / 60;
        String sign = total >= 0 ? "+" : "-";
        int abs = Math.abs(total);
        int hours = abs / 60;
        int mins = abs % 60;
        return mins == 0 ? "UTC" + sign + hours : "UTC" + sign + hours + ":" + String.format("%02d", mins);
    }

    private String dateTime(OffsetDateTime value) {
        if (value == null) {
            return "—";
        }
        return DATE_TIME.format(value.withOffsetSameInstant(zone()));
    }

    private ReportDescriptor descriptor(AnalyticsReportType type) {
        return new ReportDescriptor(type, title(type), title(type));
    }

    private String title(AnalyticsReportType type) {
        return switch (type) {
            case EMPLOYEE_PROFILE -> "Профиль сотрудника";
            case POSITION_FIT -> "Отчёт о соответствии целевой должности";
            case SKILL_GAPS -> "Отчёт о дефицитах компетенций";
            case CAREER_RECOMMENDATIONS -> "Рекомендации по карьерному развитию";
            case DEVELOPMENT_PLAN -> "Индивидуальный план развития";
            case TALENT_RESERVE -> "Список кадрового резерва";
            case WORKFORCE_POTENTIAL -> "Аналитический отчёт о кадровом потенциале";
            case DEVELOPMENT_DYNAMICS -> "Отчёт о динамике профессионального развития";
        };
    }

    private String employeeName(CareerProfileDto profile) {
        return employeeName(profile, profile == null ? null : profile.employeeId());
    }

    private String employeeName(CareerProfileDto profile, UUID employeeId) {
        if (profile != null && profile.fullName() != null && !profile.fullName().isBlank()) {
            return profile.fullName();
        }
        return "Сотрудник";
    }

    private String date(LocalDate value) {
        return value == null ? "—" : DATE.format(value);
    }

    private String text(Object value) {
        return value == null ? "—" : String.valueOf(value);
    }
}
