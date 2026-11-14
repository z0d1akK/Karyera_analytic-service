package com.zodiakk.analyticservice.recommendation.mapper;

import com.zodiakk.analyticservice.config.MapStructConfig;
import com.zodiakk.analyticservice.recommendation.dto.request.create.AiRecommendationCreateRequestDto;
import com.zodiakk.analyticservice.recommendation.dto.request.update.AiRecommendationUpdateRequestDto;
import com.zodiakk.analyticservice.recommendation.dto.response.AiRecommendationResponseDto;
import com.zodiakk.analyticservice.recommendation.entity.AiRecommendation;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface AiRecommendationMapper {

    AiRecommendation toEntity(AiRecommendationCreateRequestDto dto);

    AiRecommendationResponseDto toResponseDto(AiRecommendation entity);

    void updateEntity(AiRecommendationUpdateRequestDto dto, @MappingTarget AiRecommendation entity);
}
