package com.zodiakk.analyticservice.common.exception;

import java.util.UUID;

public class ScreeningNotFoundException extends ResourceNotFoundException {

    public ScreeningNotFoundException(UUID id) {
        super("Скрининг не найден: " + id);
    }
}
