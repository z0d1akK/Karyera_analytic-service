package com.zodiakk.analyticservice.common.exception;

import java.util.UUID;

public class AssessmentNotFoundException extends ResourceNotFoundException {

    public AssessmentNotFoundException(UUID id) {
        super("Оценка не найдена: " + id);
    }
}
