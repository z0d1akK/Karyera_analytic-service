package com.zodiakk.analyticservice.assessment.mapper;

import com.zodiakk.analyticservice.assessment.dto.request.create.CareerAssessmentCreateRequestDto;
import com.zodiakk.analyticservice.assessment.dto.request.update.CareerAssessmentUpdateRequestDto;
import com.zodiakk.analyticservice.assessment.dto.response.CareerAssessmentResponseDto;
import com.zodiakk.analyticservice.assessment.entity.CareerAssessment;
import com.zodiakk.analyticservice.config.MapStructConfig;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface CareerAssessmentMapper {

    CareerAssessment toEntity(CareerAssessmentCreateRequestDto dto);

    CareerAssessmentResponseDto toResponseDto(CareerAssessment entity);

    void updateEntity(CareerAssessmentUpdateRequestDto dto, @MappingTarget CareerAssessment entity);
}
