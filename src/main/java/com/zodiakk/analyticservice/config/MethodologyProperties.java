package com.zodiakk.analyticservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "app.methodology")
public record MethodologyProperties(
        String version,
        BigDecimal skillWeight,
        BigDecimal experienceWeight,
        BigDecimal projectWeight
) {
}
