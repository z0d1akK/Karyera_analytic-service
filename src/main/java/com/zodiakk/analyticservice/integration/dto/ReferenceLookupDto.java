package com.zodiakk.analyticservice.integration.dto;

import java.util.List;
import java.util.UUID;

public record ReferenceLookupDto(List<Item> items) {

    public record Item(UUID id, String name, Boolean active) {
    }
}
