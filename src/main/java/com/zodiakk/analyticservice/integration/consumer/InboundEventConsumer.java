package com.zodiakk.analyticservice.integration.consumer;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.zodiakk.analyticservice.common.constants.KafkaEventConstants;
import com.zodiakk.analyticservice.integration.repository.ProcessedEventRepository;
import com.zodiakk.analyticservice.projection.service.ActivityAggregateService;
import com.zodiakk.analyticservice.projection.service.ProfileProjectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class InboundEventConsumer {

    private final ObjectMapper objectMapper;

    private final ProcessedEventRepository processedEventRepository;

    private final ProfileProjectionService profileProjectionService;

    private final ActivityAggregateService activityAggregateService;

    @KafkaListener(topics = "${app.kafka.topics.employee-events}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consumeEmployee(String message) {
        consume(message);
    }

    @KafkaListener(topics = "${app.kafka.topics.career-events}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consumeCareer(String message) {
        consume(message);
    }

    @KafkaListener(topics = "${app.kafka.topics.project-events}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consumeProject(String message) {
        consume(message);
    }

    @KafkaListener(topics = "${app.kafka.topics.time-events}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consumeTime(String message) {
        consume(message);
    }

    private void consume(String message) {
        Envelope envelope = objectMapper.readValue(message, Envelope.class);

        if (processedEventRepository.registerIfAbsent(envelope.eventId()) == 0) {
            return;
        }

        JsonNode payload = envelope.payload();

        try {
            switch (envelope.eventType()) {
                case KafkaEventConstants.EMPLOYEE_SKILL_UPDATED -> profileProjectionService.markEmployeeStale(
                        UUID.fromString(payload.path("employeeId").asString())
                );
                case KafkaEventConstants.EMPLOYEE_POSITION_CHANGED -> profileProjectionService.applyPositionChange(
                        payload,
                        envelope.eventId()
                );
                case KafkaEventConstants.POSITION_REQUIREMENT_PUBLISHED -> profileProjectionService.markTargetStale(
                        UUID.fromString(payload.path("positionId").asString()),
                        UUID.fromString(payload.path("gradeId").asString())
                );
                case KafkaEventConstants.PROJECT_TASK_COMPLETED, KafkaEventConstants.PROJECT_TASK_REOPENED ->
                        activityAggregateService.rebuildProject(
                                UUID.fromString(payload.path("projectId").asString()),
                                date(payload.path("completedAt").asString(null)),
                                uuid(payload, "employeeId")
                        );
                case KafkaEventConstants.PROJECT_TASK_COMPLEXITY_CHANGED -> activityAggregateService.rebuildProject(
                        UUID.fromString(payload.path("projectId").asString()),
                        null,
                        uuid(payload, "employeeId")
                );
                case KafkaEventConstants.TIMESHEET_APPROVED -> activityAggregateService.rebuildEmployeePeriod(
                        UUID.fromString(payload.path("employeeId").asString()),
                        LocalDate.parse(payload.path("periodStart").asString()),
                        LocalDate.parse(payload.path("periodEnd").asString())
                );
                default -> log.debug("Ignored event type {}", envelope.eventType());
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to apply inbound event {}: {}", envelope.eventType(), ex.getMessage());
        }
    }

    private UUID uuid(JsonNode payload, String field) {
        String value = payload.path(field).asString(null);
        if (value == null || value.isBlank()) {
            return null;
        }
        return UUID.fromString(value);
    }

    private LocalDate date(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return OffsetDateTime.parse(value).toLocalDate();
    }

    public record Envelope(
            UUID eventId,
            UUID aggregateId,
            String eventType,
            String aggregateType,
            @JsonAlias("schemaVersion") int eventVersion,
            OffsetDateTime occurredAt,
            JsonNode payload
    ) {
    }
}
