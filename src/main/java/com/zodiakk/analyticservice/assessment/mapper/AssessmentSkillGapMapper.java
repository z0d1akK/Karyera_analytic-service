package com.zodiakk.analyticservice.assessment.mapper;

import com.zodiakk.analyticservice.assessment.dto.request.create.AssessmentSkillGapCreateRequestDto;
import com.zodiakk.analyticservice.assessment.dto.request.update.AssessmentSkillGapUpdateRequestDto;
import com.zodiakk.analyticservice.assessment.dto.response.AssessmentSkillGapResponseDto;
import com.zodiakk.analyticservice.assessment.entity.AssessmentSkillGap;
import com.zodiakk.analyticservice.config.MapStructConfig;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface AssessmentSkillGapMapper {

    AssessmentSkillGap toEntity(AssessmentSkillGapCreateRequestDto dto);

    AssessmentSkillGapResponseDto toResponseDto(AssessmentSkillGap entity);

    void updateEntity(AssessmentSkillGapUpdateRequestDto dto, @MappingTarget AssessmentSkillGap entity);
}
