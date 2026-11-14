package com.zodiakk.analyticservice.assessment.calculation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ExperienceCalculator {

    public static final BigDecimal DAYS_PER_MONTH = new BigDecimal("30.4375");

    public BigDecimal coveredMonths(List<DateInterval> periods, LocalDate today) {
        if (today == null) {
            throw new InvalidAssessmentInputException("Дата расчёта опыта не задана");
        }

        List<DateInterval> merged = merge(periods, today);
        long days = 0;

        for (DateInterval interval : merged) {
            days += ChronoUnit.DAYS.between(interval.start(), interval.endInclusive()) + 1;
        }

        if (days == 0) {
            return DecimalScale.ZERO;
        }

        return DecimalScale.scale(DecimalScale.divide(BigDecimal.valueOf(days), DAYS_PER_MONTH));
    }

    public BigDecimal score(BigDecimal coveredMonths, int minExperienceMonths) {
        if (minExperienceMonths < 0) {
            throw new InvalidAssessmentInputException("Минимальный опыт не может быть отрицательным");
        }

        if (minExperienceMonths == 0) {
            return DecimalScale.ONE;
        }

        BigDecimal months = coveredMonths == null ? BigDecimal.ZERO : coveredMonths;
        BigDecimal ratio = DecimalScale.divide(months, BigDecimal.valueOf(minExperienceMonths));

        if (ratio.compareTo(BigDecimal.ONE) > 0) {
            ratio = BigDecimal.ONE;
        }

        if (ratio.signum() < 0) {
            ratio = BigDecimal.ZERO;
        }

        return DecimalScale.scale(ratio);
    }

    public List<DateInterval> merge(List<DateInterval> periods, LocalDate today) {
        if (periods == null || periods.isEmpty()) {
            return List.of();
        }

        List<DateInterval> normalized = new ArrayList<>();

        for (DateInterval period : periods) {
            if (period == null || period.start() == null) {
                continue;
            }

            LocalDate end = period.endInclusive() == null ? today : period.endInclusive();

            if (end.isAfter(today)) {
                end = today;
            }

            if (end.isBefore(period.start())) {
                continue;
            }

            normalized.add(new DateInterval(period.start(), end));
        }

        normalized.sort(Comparator.comparing(DateInterval::start).thenComparing(DateInterval::endInclusive));

        List<DateInterval> merged = new ArrayList<>();

        for (DateInterval interval : normalized) {
            if (merged.isEmpty()) {
                merged.add(interval);
                continue;
            }

            DateInterval current = merged.get(merged.size() - 1);
            LocalDate adjacent = current.endInclusive().plusDays(1);

            if (!interval.start().isAfter(adjacent)) {
                LocalDate end = current.endInclusive().isAfter(interval.endInclusive())
                        ? current.endInclusive()
                        : interval.endInclusive();
                merged.set(merged.size() - 1, new DateInterval(current.start(), end));
            } else {
                merged.add(interval);
            }
        }

        return List.copyOf(merged);
    }
}
