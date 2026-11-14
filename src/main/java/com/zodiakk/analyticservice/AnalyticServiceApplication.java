package com.zodiakk.analyticservice;

import com.zodiakk.analyticservice.config.JobProperties;
import com.zodiakk.analyticservice.config.MethodologyProperties;
import com.zodiakk.analyticservice.outbox.config.OutboxProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
@EnableConfigurationProperties({MethodologyProperties.class, JobProperties.class, OutboxProperties.class})
public class AnalyticServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyticServiceApplication.class, args);
    }

}
