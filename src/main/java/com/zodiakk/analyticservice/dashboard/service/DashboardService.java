package com.zodiakk.analyticservice.dashboard.service;

import com.zodiakk.analyticservice.assessment.entity.CareerAssessment;
import com.zodiakk.analyticservice.assessment.repository.AssessmentSkillGapRepository;
import com.zodiakk.analyticservice.assessment.repository.CareerAssessmentRepository;
import com.zodiakk.analyticservice.common.enums.DataQualityStatus;
import com.zodiakk.analyticservice.dashboard.dto.CareerReadinessDashboardDto;
import com.zodiakk.analyticservice.dashboard.dto.DashboardGrain;
import com.zodiakk.analyticservice.dashboard.dto.DashboardOverviewDto;
import com.zodiakk.analyticservice.dashboard.dto.DepartmentDashboardDto;
import com.zodiakk.analyticservice.dashboard.dto.DevelopmentDashboardDto;
import com.zodiakk.analyticservice.dashboard.dto.DevelopmentDynamicsDto;
import com.zodiakk.analyticservice.dashboard.dto.ProjectActivityDashboardDto;
import com.zodiakk.analyticservice.dashboard.dto.SkillDeficitDashboardDto;
import com.zodiakk.analyticservice.integration.client.EmployeeClient;
import com.zodiakk.analyticservice.integration.dto.EmployeeLookupItemDto;
import com.zodiakk.analyticservice.integration.dto.EmployeeLookupResponseDto;
import com.zodiakk.analyticservice.integration.dto.IdListDto;
import com.zodiakk.analyticservice.projection.entity.EmployeeActivityAggregate;
import com.zodiakk.analyticservice.projection.repository.EmployeeActivityAggregateRepository;
import com.zodiakk.analyticservice.security.access.AnalyticsAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    public static final int SMALL_GROUP = 3;

    private final CareerAssessmentRepository assessmentRepository;

    private final AssessmentSkillGapRepository skillGapRepository;

    private final EmployeeActivityAggregateRepository aggregateRepository;

    private final AnalyticsAccessService accessService;

    private final EmployeeClient employeeClient;

    @Cacheable(cacheNames = "dashboards", key = "'overview-v2:' + T(java.util.Objects).hash(#root.target.scopeKey())")
    public DashboardOverviewDto overview() {
        List<CareerAssessment> latest = visibleLatest();
        BigDecimal average = latest.isEmpty()
                ? BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP)
                : latest.stream()
                .map(CareerAssessment::getTotalScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(latest.size()), 6, RoundingMode.HALF_UP);

        long topReady = latest.stream()
                .filter(item -> item.getTotalScore() != null && item.getTotalScore().compareTo(new BigDecimal("0.75")) >= 0)
                .count();
        BigDecimal topShare = latest.isEmpty()
                ? BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(topReady).divide(BigDecimal.valueOf(latest.size()), 6, RoundingMode.HALF_UP);
        Map.Entry<UUID, Long> topDeficit = deficitCounts(latest).entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);
        BigDecimal averageSkill = latest.isEmpty()
                ? BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP)
                : latest.stream()
                .map(item -> item.getSkillScore() == null ? BigDecimal.ZERO : item.getSkillScore())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(latest.size()), 6, RoundingMode.HALF_UP);

        return new DashboardOverviewDto(
                latest.stream().map(CareerAssessment::getEmployeeId).distinct().count(),
                average,
                latest.stream().filter(item -> item.getDataQualityStatus() == DataQualityStatus.COMPLETE).count(),
                latest.size(),
                topShare,
                topReady,
                topDeficit == null ? null : topDeficit.getKey(),
                topDeficit == null ? null : skillNames(List.of(topDeficit.getKey())).get(topDeficit.getKey()),
                topDeficit == null ? 0 : topDeficit.getValue(),
                latest.stream().filter(item -> !Boolean.TRUE.equals(item.getMandatoryRequirementsMet())).count(),
                averageSkill
        );
    }

    @Cacheable(cacheNames = "dashboards", key = "'skills:' + T(java.util.Objects).hash(#root.target.scopeKey())")
    public SkillDeficitDashboardDto skills() {
        return new SkillDeficitDashboardDto(deficitItems(visibleLatest(), SMALL_GROUP, Integer.MAX_VALUE));
    }

    @Cacheable(cacheNames = "dashboards", key = "'skills-top:' + T(java.util.Objects).hash(#root.target.scopeKey())")
    public SkillDeficitDashboardDto skillsTop() {
        return new SkillDeficitDashboardDto(deficitItems(visibleLatest(), 1, 50));
    }

    @Cacheable(cacheNames = "dashboards", key = "'readiness:' + T(java.util.Objects).hash(#root.target.scopeKey())")
    public CareerReadinessDashboardDto readiness() {
        long low = 0;
        long mid = 0;
        long high = 0;
        long top = 0;

        for (CareerAssessment assessment : visibleLatest()) {
            BigDecimal score = assessment.getTotalScore();

            if (score.compareTo(new BigDecimal("0.25")) < 0) {
                low++;
            } else if (score.compareTo(new BigDecimal("0.50")) < 0) {
                mid++;
            } else if (score.compareTo(new BigDecimal("0.75")) < 0) {
                high++;
            } else {
                top++;
            }
        }

        List<CareerReadinessDashboardDto.Bucket> buckets = new ArrayList<>();
        addBucket(buckets, "0.00-0.25", low);
        addBucket(buckets, "0.25-0.50", mid);
        addBucket(buckets, "0.50-0.75", high);
        addBucket(buckets, "0.75-1.00", top);
        return new CareerReadinessDashboardDto(buckets);
    }

    public DevelopmentDashboardDto development() {
        return development(DashboardGrain.MONTH);
    }

    @Cacheable(cacheNames = "dashboards", key = "'development:' + #grain + ':' + T(java.util.Objects).hash(#root.target.scopeKey())")
    public DevelopmentDashboardDto development(DashboardGrain grain) {
        DashboardGrain resolved = grain == null ? DashboardGrain.MONTH : grain;
        Map<LocalDate, List<CareerAssessment>> byPeriod = new LinkedHashMap<>();
        for (CareerAssessment assessment : visibleHistory()) {
            if (assessment.getCalculatedAt() == null) {
                continue;
            }
            LocalDate period = periodOf(assessment.getCalculatedAt().toLocalDate(), resolved);
            byPeriod.computeIfAbsent(period, key -> new ArrayList<>()).add(assessment);
        }
        List<DevelopmentDashboardDto.Point> points = byPeriod.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new DevelopmentDashboardDto.Point(
                        entry.getKey(),
                        entry.getValue().size(),
                        average(entry.getValue())
                ))
                .toList();
        return new DevelopmentDashboardDto(points);
    }

    public ProjectActivityDashboardDto projectActivity() {
        return projectActivity(DashboardGrain.MONTH);
    }

    @Cacheable(cacheNames = "dashboards", key = "'activity:' + #grain + ':' + T(java.util.Objects).hash(#root.target.scopeKey())")
    public ProjectActivityDashboardDto projectActivity(DashboardGrain grain) {
        DashboardGrain resolved = grain == DashboardGrain.YEAR ? DashboardGrain.YEAR : DashboardGrain.MONTH;
        List<UUID> visible = accessService.visibleEmployeeIds();
        LocalDate from = LocalDate.now(ZoneOffset.UTC).minusYears(5).withDayOfMonth(1);
        LocalDate to = LocalDate.now(ZoneOffset.UTC);
        Map<LocalDate, long[]> totals = new LinkedHashMap<>();

        for (EmployeeActivityAggregate aggregate : aggregateRepository
                .findAllByPeriodStartGreaterThanEqualAndPeriodStartLessThanEqual(from, to)) {
            if (visible != null && !visible.contains(aggregate.getEmployeeId())) {
                continue;
            }
            LocalDate period = periodOf(aggregate.getPeriodStart(), resolved);
            long[] bucket = totals.computeIfAbsent(period, key -> new long[2]);
            bucket[0] += aggregate.getApprovedMinutes() == null ? 0 : aggregate.getApprovedMinutes();
            bucket[1] += aggregate.getCompletedTasks() == null ? 0 : aggregate.getCompletedTasks();
        }

        List<ProjectActivityDashboardDto.Point> points = totals.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new ProjectActivityDashboardDto.Point(entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
                .toList();
        return new ProjectActivityDashboardDto(points);
    }

    public DevelopmentDynamicsDto dynamics(UUID employeeId) {
        accessService.requireSelfOrManager(employeeId);
        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).minusYears(5);
        OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        List<DevelopmentDynamicsDto.Point> points = assessmentRepository.findHistory(employeeId, from, to).stream()
                .sorted(Comparator.comparing(CareerAssessment::getCalculatedAt))
                .map(assessment -> new DevelopmentDynamicsDto.Point(
                        assessment.getId(),
                        assessment.getCalculatedAt(),
                        assessment.getTotalScore(),
                        assessment.getSkillScore(),
                        Boolean.TRUE.equals(assessment.getMandatoryRequirementsMet()),
                        Boolean.TRUE.equals(assessment.getIsStale())
                ))
                .toList();

        return new DevelopmentDynamicsDto(employeeId, points);
    }

    public String scopeKey() {
        List<UUID> visible = accessService.visibleEmployeeIds();
        return visible == null ? "all" : visible.stream().sorted().map(UUID::toString).reduce("", (left, right) -> left + right);
    }

    private List<CareerAssessment> visibleHistory() {
        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).minusYears(5);
        OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        List<UUID> visible = accessService.visibleEmployeeIds();
        List<CareerAssessment> history = new ArrayList<>();
        for (CareerAssessment assessment : assessmentRepository.findFreshInRange(from, to)) {
            if (visible != null && !visible.contains(assessment.getEmployeeId())) {
                continue;
            }
            history.add(assessment);
        }
        return history;
    }

    private LocalDate periodOf(LocalDate day, DashboardGrain grain) {
        if (day == null || grain == DashboardGrain.DAY) {
            return day;
        }
        if (grain == DashboardGrain.YEAR) {
            return Year.from(day).atDay(1);
        }
        return YearMonth.from(day).atDay(1);
    }

    private List<CareerAssessment> visibleLatest() {
        List<UUID> visible = accessService.visibleEmployeeIds();
        Map<UUID, CareerAssessment> latest = new LinkedHashMap<>();

        for (CareerAssessment assessment : assessmentRepository.findLatestFresh()) {
            if (visible != null && !visible.contains(assessment.getEmployeeId())) {
                continue;
            }

            latest.putIfAbsent(assessment.getEmployeeId(), assessment);
        }

        return List.copyOf(latest.values());
    }

    @Cacheable(cacheNames = "dashboards", key = "'departments:' + T(java.util.Objects).hash(#root.target.scopeKey())")
    public DepartmentDashboardDto departments() {
        List<CareerAssessment> latest = visibleLatest();
        Map<UUID, UUID> departmentByEmployee = departmentByEmployee(
                latest.stream().map(CareerAssessment::getEmployeeId).distinct().toList()
        );
        Map<UUID, List<CareerAssessment>> byDepartment = new LinkedHashMap<>();
        for (CareerAssessment assessment : latest) {
            UUID departmentId = departmentByEmployee.get(assessment.getEmployeeId());
            byDepartment.computeIfAbsent(departmentId, key -> new ArrayList<>()).add(assessment);
        }
        List<DepartmentDashboardDto.Item> items = byDepartment.entrySet().stream()
                .sorted(Comparator.<Map.Entry<UUID, List<CareerAssessment>>>comparingLong(entry -> entry.getValue().size()).reversed())
                .map(entry -> new DepartmentDashboardDto.Item(
                        entry.getKey(),
                        entry.getValue().stream().map(CareerAssessment::getEmployeeId).distinct().count(),
                        average(entry.getValue())
                ))
                .toList();
        return new DepartmentDashboardDto(items);
    }

    private void addBucket(List<CareerReadinessDashboardDto.Bucket> buckets, String label, long count) {
        if (count > 0) {
            buckets.add(new CareerReadinessDashboardDto.Bucket(label, count));
        }
    }

    private Map<UUID, Long> deficitCounts(List<CareerAssessment> latest) {
        List<UUID> assessmentIds = latest.stream().map(CareerAssessment::getId).toList();
        Map<UUID, Long> counts = new LinkedHashMap<>();
        if (!assessmentIds.isEmpty()) {
            for (var gap : skillGapRepository.findAllByAssessmentIdInAndRequirementMetFalse(assessmentIds)) {
                counts.merge(gap.getSkillId(), 1L, Long::sum);
            }
        }
        return counts;
    }

    private List<SkillDeficitDashboardDto.Item> deficitItems(List<CareerAssessment> latest, int minEmployees, int limit) {
        List<Map.Entry<UUID, Long>> ranked = deficitCounts(latest).entrySet().stream()
                .filter(entry -> entry.getValue() >= minEmployees)
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .limit(limit)
                .toList();
        Map<UUID, String> names = skillNames(ranked.stream().map(Map.Entry::getKey).toList());
        return ranked.stream()
                .map(entry -> new SkillDeficitDashboardDto.Item(entry.getKey(), entry.getValue(), names.get(entry.getKey())))
                .toList();
    }

    private Map<UUID, String> skillNames(List<UUID> skillIds) {
        if (skillIds.isEmpty()) {
            return Map.of();
        }
        try {
            var lookup = employeeClient.skills(new com.zodiakk.analyticservice.integration.dto.IdsDto(skillIds));
            if (lookup == null || lookup.items() == null) {
                return Map.of();
            }
            Map<UUID, String> names = new LinkedHashMap<>();
            for (var item : lookup.items()) {
                if (item.id() != null) {
                    names.put(item.id(), item.name());
                }
            }
            return names;
        } catch (RuntimeException ex) {
            return Map.of();
        }
    }

    private Map<UUID, UUID> departmentByEmployee(List<UUID> employeeIds) {
        if (employeeIds.isEmpty()) {
            return Map.of();
        }
        try {
            EmployeeLookupResponseDto response = employeeClient.lookup(new IdListDto(employeeIds));
            if (response == null || response.employees() == null) {
                return Map.of();
            }
            Map<UUID, UUID> departments = new LinkedHashMap<>();
            for (EmployeeLookupItemDto employee : response.employees()) {
                if (employee != null && employee.id() != null) {
                    departments.put(employee.id(), employee.departmentId());
                }
            }
            return departments;
        } catch (RuntimeException ex) {
            return Map.of();
        }
    }

    private BigDecimal average(List<CareerAssessment> assessments) {
        if (assessments == null || assessments.isEmpty()) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }
        return assessments.stream()
                .map(item -> item.getTotalScore() == null ? BigDecimal.ZERO : item.getTotalScore())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(assessments.size()), 6, RoundingMode.HALF_UP);
    }
}
