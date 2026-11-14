package com.zodiakk.analyticservice.recommendation.service;

import com.zodiakk.analyticservice.integration.dto.CareerExplanationRequest;
import com.zodiakk.analyticservice.integration.dto.CareerExplanationResponse;
import com.zodiakk.analyticservice.integration.dto.DevelopmentRecommendationRequest;
import com.zodiakk.analyticservice.integration.dto.DevelopmentRecommendationResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class RecommendationFallback {

    public static final String MODEL_IDENTIFIER = "template-fallback";

    public CareerExplanationResponse explain(CareerExplanationRequest request) {
        List<String> strengths = new ArrayList<>();
        if (request.strengths() != null) {
            for (CareerExplanationRequest.SkillStrength strength : request.strengths()) {
                strengths.add("Уровень " + name(strength.skillName()) + " соответствует требованиям: "
                        + strength.actualLevel() + " из " + strength.requiredLevel() + ".");
            }
        }

        List<CareerExplanationResponse.Area> areas = new ArrayList<>();
        if (request.skillGaps() != null) {
            for (CareerExplanationRequest.SkillGap gap : request.skillGaps()) {
                areas.add(new CareerExplanationResponse.Area(
                        gap.skillId(),
                        "Текущий уровень " + name(gap.skillName()) + " равен " + gap.actualLevel()
                                + ", требуется " + gap.requiredLevel() + "."
                ));
            }
        }

        List<String> limitations = new ArrayList<>();
        limitations.add("Результат не является автоматическим решением о повышении.");
        if (!"COMPLETE".equals(request.dataQualityStatus())) {
            limitations.add("Часть исходных данных была неполной, поэтому объяснение ограничено доступными баллами.");
        }

        String position = request.target() == null ? "цели" : name(request.target().positionName());
        CareerExplanationRequest.Scores scores = request.scores();
        String summary = "Итоговое соответствие " + position + " составляет " + percent(scores == null ? null : scores.total())
                + ". Соответствие навыков " + percent(scores == null ? null : scores.skill())
                + ". Обязательные требования "
                + (Boolean.TRUE.equals(request.mandatoryRequirementsMet()) ? "выполнены." : "выполнены не все.");

        return new CareerExplanationResponse(
                UUID.randomUUID(),
                request.assessmentId(),
                summary,
                strengths,
                areas,
                limitations,
                MODEL_IDENTIFIER,
                null,
                "FALLBACK"
        );
    }

    public DevelopmentRecommendationResponse recommend(DevelopmentRecommendationRequest request) {
        if (request == null || request.developmentAreas() == null || request.developmentAreas().isEmpty()) {
            return new DevelopmentRecommendationResponse(UUID.randomUUID(), List.of());
        }
        int limit = request.preferences() == null || request.preferences().maxRecommendations() == null
                ? request.developmentAreas().size()
                : request.preferences().maxRecommendations();
        boolean mentoring = request.preferences() != null && Boolean.TRUE.equals(request.preferences().includeMentoring());
        boolean project = request.preferences() == null || !Boolean.FALSE.equals(request.preferences().includeProjectActivities());
        List<DevelopmentRecommendationResponse.Item> items = new ArrayList<>();
        for (DevelopmentRecommendationRequest.Area area : request.developmentAreas()) {
            if (items.size() >= limit) {
                break;
            }
            String skill = name(area.skillName());
            String type = mentoring && items.size() % 2 == 1
                    ? "MENTORING"
                    : project ? "PROJECT_EXPERIENCE" : "SELF_STUDY";
            items.add(new DevelopmentRecommendationResponse.Item(
                    area.skillId(),
                    type,
                    "Развитие навыка " + skill,
                    "Рекомендация относится к навыку " + skill + ": текущий уровень "
                            + area.currentLevel() + ", целевой " + area.targetLevel()
                            + ". Конкретный курс или свободный проект не указаны, потому что таких данных нет.",
                    List.of(
                            "Разобрать практический пример по навыку " + skill + ".",
                            "Закрепить результат на задаче, связанной с этим навыком."
                    ),
                    "Практическое понимание навыка " + skill + " без обещания срока повышения."
            ));
        }
        return new DevelopmentRecommendationResponse(UUID.randomUUID(), items);
    }

    private String name(String value) {
        return value == null || value.isBlank() ? "навыка" : value;
    }

    private String percent(BigDecimal value) {
        if (value == null) {
            return "0%";
        }
        BigDecimal scaled = value.compareTo(BigDecimal.ONE) <= 0
                ? value.multiply(BigDecimal.valueOf(100))
                : value;
        return scaled.setScale(0, RoundingMode.HALF_UP) + "%";
    }
}
