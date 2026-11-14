package com.zodiakk.analyticservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jobs")
public record JobProperties(
        int batchSize,
        long pollDelayMs,
        long leaseSeconds
) {
}
