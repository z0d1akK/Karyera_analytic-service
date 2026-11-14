package com.zodiakk.analyticservice.assessment.calculation;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class DecimalScale {

    public static final int SCALE = 6;

    public static final int INTERMEDIATE_SCALE = 12;

    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    public static final BigDecimal ONE = BigDecimal.ONE.setScale(SCALE, ROUNDING);

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, ROUNDING);

    private DecimalScale() {
    }

    public static BigDecimal scale(BigDecimal value) {
        return value.setScale(SCALE, ROUNDING);
    }

    public static BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
        return numerator.divide(denominator, INTERMEDIATE_SCALE, ROUNDING);
    }
}
