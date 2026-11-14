package com.zodiakk.analyticservice.integration.support;

import com.zodiakk.analyticservice.common.exception.InsufficientDataException;
import com.zodiakk.analyticservice.common.exception.TransientIntegrationException;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IntegrationCallsTest {

    @Test
    void requiredValueIsReturned() {
        assertEquals("ok", IntegrationCalls.required(() -> "ok", "missing"));
    }

    @Test
    void missingValueAndNotFoundBecomeInsufficientData() {
        assertThrows(InsufficientDataException.class, () -> IntegrationCalls.required(() -> null, "missing"));
        assertThrows(InsufficientDataException.class, () -> IntegrationCalls.required(this::notFound, "missing"));
    }

    @Test
    void otherFailuresAreTransientAndOptionalCallsCollapse() {
        assertThrows(TransientIntegrationException.class,
                () -> IntegrationCalls.required(() -> { throw new IllegalStateException("down"); }, "missing"));
        assertNull(IntegrationCalls.optional(() -> { throw new IllegalStateException("down"); }));
    }

    private String notFound() {
        throw new FeignException.NotFound(
                "missing",
                Request.create(Request.HttpMethod.GET, "/missing", Map.of(), null, StandardCharsets.UTF_8, null),
                null,
                Map.of()
        );
    }
}
