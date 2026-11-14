package com.zodiakk.analyticservice.common.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KafkaEventConstants {

    public static final int EVENT_VERSION_V1 = 1;

    public static final String DLT_SUFFIX = ".DLT";

    public static final String AGGREGATE_ASSESSMENT = "CAREER_ASSESSMENT";

    public static final String AGGREGATE_SCREENING = "TALENT_SCREENING";

    public static final String CAREER_ASSESSMENT_COMPLETED = "CareerAssessmentCompleted";

    public static final String CAREER_ASSESSMENT_FAILED = "CareerAssessmentFailed";

    public static final String TALENT_RESERVE_SCREENING_COMPLETED = "TalentReserveScreeningCompleted";

    public static final String TALENT_RESERVE_SCREENING_FAILED = "TalentReserveScreeningFailed";

    public static final String EMPLOYEE_SKILL_UPDATED = "EmployeeSkillUpdated";

    public static final String EMPLOYEE_POSITION_CHANGED = "EmployeePositionChanged";

    public static final String POSITION_REQUIREMENT_PUBLISHED = "PositionRequirementPublished";

    public static final String PROJECT_TASK_COMPLETED = "ProjectTaskCompleted";

    public static final String PROJECT_TASK_REOPENED = "ProjectTaskReopened";

    public static final String PROJECT_TASK_COMPLEXITY_CHANGED = "ProjectTaskComplexityChanged";

    public static final String TIMESHEET_APPROVED = "TimesheetApproved";

    public static final String ERROR_INSUFFICIENT_DATA = "INSUFFICIENT_DATA";

    public static final String ERROR_INTELLIGENCE_UNAVAILABLE = "INTELLIGENCE_UNAVAILABLE";

    public static final String ERROR_INVALID_EXPLANATION = "INVALID_EXPLANATION";
}
