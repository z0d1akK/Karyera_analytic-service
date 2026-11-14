package com.zodiakk.analyticservice.common.exception;

import java.util.List;

public class InsufficientDataException extends BusinessException {

    private final List<String> missing;

    private final List<String> suggestedModes;

    public InsufficientDataException(String message) {
        this(message, List.of(), List.of());
    }

    public InsufficientDataException(String message, List<String> missing, List<String> suggestedModes) {
        super(message);
        this.missing = missing == null ? List.of() : List.copyOf(missing);
        this.suggestedModes = suggestedModes == null ? List.of() : List.copyOf(suggestedModes);
    }

    public List<String> getMissing() {
        return missing;
    }

    public List<String> getSuggestedModes() {
        return suggestedModes;
    }
}
