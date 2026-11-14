package com.zodiakk.analyticservice.config;

import com.zodiakk.analyticservice.common.constants.KafkaEventConstants;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.topics.analytics-events}")
    private String analyticsEventsTopic;

    @Value("${app.kafka.topics.employee-events}")
    private String employeeEventsTopic;

    @Value("${app.kafka.topics.career-events}")
    private String careerEventsTopic;

    @Value("${app.kafka.topics.project-events}")
    private String projectEventsTopic;

    @Value("${app.kafka.topics.time-events}")
    private String timeEventsTopic;

    @Bean
    public NewTopic analyticsEventsTopic() {
        return topic(analyticsEventsTopic);
    }

    @Bean
    public NewTopic analyticsEventsDltTopic() {
        return topic(analyticsEventsTopic + KafkaEventConstants.DLT_SUFFIX);
    }

    @Bean
    public NewTopic employeeEventsTopic() {
        return topic(employeeEventsTopic);
    }

    @Bean
    public NewTopic careerEventsTopic() {
        return topic(careerEventsTopic);
    }

    @Bean
    public NewTopic projectEventsTopic() {
        return topic(projectEventsTopic);
    }

    @Bean
    public NewTopic timeEventsTopic() {
        return topic(timeEventsTopic);
    }

    private NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }
}
