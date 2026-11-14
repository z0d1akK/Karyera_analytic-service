package com.zodiakk.analyticservice.common.enums;

public enum AssessmentMode {

    SKILLS,
    SKILLS_AND_EXPERIENCE,
    FULL;

    public static AssessmentMode from(String raw) {
        if (raw == null || raw.isBlank()) {
            return FULL;
        }
        return switch (raw.trim().toUpperCase()) {
            case "SKILLS" -> SKILLS;
            case "SKILLS_AND_EXPERIENCE" -> SKILLS_AND_EXPERIENCE;
            case "FULL" -> FULL;
            default -> throw new IllegalArgumentException("Неизвестный режим оценки: " + raw);
        };
    }
}
