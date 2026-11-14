package com.zodiakk.analyticservice.assessment.mapper;

import com.zodiakk.analyticservice.assessment.dto.request.create.AssessmentComponentCreateRequestDto;
import com.zodiakk.analyticservice.assessment.dto.request.update.AssessmentComponentUpdateRequestDto;
import com.zodiakk.analyticservice.assessment.dto.response.AssessmentComponentResponseDto;
import com.zodiakk.analyticservice.assessment.entity.AssessmentComponent;
import com.zodiakk.analyticservice.config.MapStructConfig;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface AssessmentComponentMapper {

    AssessmentComponent toEntity(AssessmentComponentCreateRequestDto dto);

    AssessmentComponentResponseDto toResponseDto(AssessmentComponent entity);

    void updateEntity(AssessmentComponentUpdateRequestDto dto, @MappingTarget AssessmentComponent entity);
}
