package com.zodiakk.analyticservice.screening.mapper;

import com.zodiakk.analyticservice.config.MapStructConfig;
import com.zodiakk.analyticservice.screening.dto.request.create.TalentScreeningCreateRequestDto;
import com.zodiakk.analyticservice.screening.dto.request.update.TalentScreeningUpdateRequestDto;
import com.zodiakk.analyticservice.screening.dto.response.TalentScreeningResponseDto;
import com.zodiakk.analyticservice.screening.entity.TalentScreening;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface TalentScreeningMapper {

    TalentScreening toEntity(TalentScreeningCreateRequestDto dto);

    TalentScreeningResponseDto toResponseDto(TalentScreening entity);

    void updateEntity(TalentScreeningUpdateRequestDto dto, @MappingTarget TalentScreening entity);
}
