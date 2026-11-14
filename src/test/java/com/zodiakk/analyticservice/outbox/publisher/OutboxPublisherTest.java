package com.zodiakk.analyticservice.outbox.publisher;

import com.zodiakk.analyticservice.outbox.config.OutboxProperties;
import com.zodiakk.analyticservice.outbox.entity.OutboxEvent;
import com.zodiakk.analyticservice.outbox.entity.OutboxEventStatus;
import com.zodiakk.analyticservice.outbox.service.OutboxClaimService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxClaimService claimService;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxPublisher(claimService, kafkaTemplate, new OutboxProperties(10, 1000, 30, 5));
        ReflectionTestUtils.setField(publisher, "analyticsEventsTopic", "analytics.events.v1");
    }

    @Test
    void publishesAndMarksFailedWhenKafkaRejects() {
        OutboxEvent event = event();
        when(claimService.claimBatch()).thenReturn(List.of(event));
        CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("kafka"));
        when(kafkaTemplate.send("analytics.events.v1", event.getAggregateId().toString(), "{}")).thenReturn(failed);

        publisher.publishPendingEvents();

        verify(claimService).markFailed(event.getId(), 1);
    }

    @Test
    void marksFailedWhenSendThrowsImmediately() {
        OutboxEvent event = event();
        when(claimService.claimBatch()).thenReturn(List.of(event));
        when(kafkaTemplate.send("analytics.events.v1", event.getAggregateId().toString(), "{}"))
                .thenThrow(new IllegalStateException("kafka"));
        doThrow(new IllegalStateException("db")).when(claimService).markFailed(event.getId(), 1);

        publisher.publishPendingEvents();

        verify(claimService).markFailed(event.getId(), 1);
    }

    @Test
    void survivesAFailedPublishedMark() {
        OutboxEvent event = event();
        when(claimService.claimBatch()).thenReturn(List.of(event));
        when(kafkaTemplate.send("analytics.events.v1", event.getAggregateId().toString(), "{}"))
                .thenReturn(CompletableFuture.completedFuture(null));
        doThrow(new IllegalStateException("db")).when(claimService).markPublished(event.getId(), 1);

        publisher.publishPendingEvents();

        verify(claimService).markPublished(event.getId(), 1);
    }

    private OutboxEvent event() {
        return OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateId(UUID.randomUUID())
                .attempts(1)
                .status(OutboxEventStatus.PROCESSING)
                .payload("{}")
                .eventType("CareerAssessmentCompleted")
                .build();
    }
}
