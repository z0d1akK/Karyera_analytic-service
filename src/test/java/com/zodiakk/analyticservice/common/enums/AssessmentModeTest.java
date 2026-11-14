package com.zodiakk.analyticservice.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssessmentModeTest {

    @Test
    void parsesKnownModesAndRejectsUnknown() {
        assertThat(AssessmentMode.from(null)).isEqualTo(AssessmentMode.FULL);
        assertThat(AssessmentMode.from("  ")).isEqualTo(AssessmentMode.FULL);
        assertThat(AssessmentMode.from("skills")).isEqualTo(AssessmentMode.SKILLS);
        assertThat(AssessmentMode.from("SKILLS_AND_EXPERIENCE")).isEqualTo(AssessmentMode.SKILLS_AND_EXPERIENCE);
        assertThat(AssessmentMode.from("FULL")).isEqualTo(AssessmentMode.FULL);
        assertThatThrownBy(() -> AssessmentMode.from("NOPE"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
