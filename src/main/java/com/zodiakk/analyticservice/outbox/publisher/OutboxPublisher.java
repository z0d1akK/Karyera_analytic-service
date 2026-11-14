package com.zodiakk.analyticservice.outbox.publisher;

import com.zodiakk.analyticservice.outbox.config.OutboxProperties;
import com.zodiakk.analyticservice.outbox.entity.OutboxEvent;
import com.zodiakk.analyticservice.outbox.service.OutboxClaimService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxClaimService claimService;

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final OutboxProperties properties;

    @Value("${app.kafka.topics.analytics-events}")
    private String analyticsEventsTopic;

    @Scheduled(fixedDelayString = "${app.outbox.poll-delay-ms}")
    public void publishPendingEvents() {
        for (OutboxEvent event : claimService.claimBatch()) {
            publish(event);
        }
    }

    private void publish(OutboxEvent event) {
        try {
            kafkaTemplate.send(analyticsEventsTopic, event.getAggregateId().toString(), event.getPayload())
                    .orTimeout(properties.sendTimeoutSeconds(), TimeUnit.SECONDS)
                    .whenComplete((result, ex) -> complete(event, ex));
        } catch (RuntimeException ex) {
            complete(event, ex);
        }
    }

    private void complete(OutboxEvent event, Throwable ex) {
        if (ex != null) {
            log.error("Failed to publish outbox event: id={}", event.getId(), ex);

            try {
                claimService.markFailed(event.getId(), event.getAttempts());
            } catch (RuntimeException markException) {
                log.error("Failed to mark outbox event as failed: id={}", event.getId(), markException);
            }

            return;
        }

        try {
            claimService.markPublished(event.getId(), event.getAttempts());
        } catch (RuntimeException markException) {
            log.error("Failed to mark outbox event as published: id={}", event.getId(), markException);
        }
    }
}
