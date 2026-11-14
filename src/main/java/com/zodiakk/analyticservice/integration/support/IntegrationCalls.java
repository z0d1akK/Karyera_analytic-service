package com.zodiakk.analyticservice.integration.support;

import com.zodiakk.analyticservice.common.exception.InsufficientDataException;
import com.zodiakk.analyticservice.common.exception.TransientIntegrationException;
import feign.FeignException;

import java.util.function.Supplier;

public final class IntegrationCalls {

    private IntegrationCalls() {
    }

    public static <T> T required(Supplier<T> call, String missingMessage) {
        try {
            T value = call.get();

            if (value == null) {
                throw new InsufficientDataException(missingMessage);
            }

            return value;
        } catch (InsufficientDataException ex) {
            throw ex;
        } catch (FeignException.NotFound ex) {
            throw new InsufficientDataException(missingMessage);
        } catch (RuntimeException ex) {
            throw new TransientIntegrationException("Временная ошибка интеграции", ex);
        }
    }

    public static <T> T optional(Supplier<T> call) {
        try {
            return call.get();
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
