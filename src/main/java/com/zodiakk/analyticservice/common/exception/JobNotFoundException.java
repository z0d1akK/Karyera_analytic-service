package com.zodiakk.analyticservice.common.exception;

import java.util.UUID;

public class JobNotFoundException extends ResourceNotFoundException {

    public JobNotFoundException(UUID id) {
        super("Задание не найдено: " + id);
    }
}
