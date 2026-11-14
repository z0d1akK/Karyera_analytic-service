package com.zodiakk.analyticservice.screening.mapper;

import com.zodiakk.analyticservice.config.MapStructConfig;
import com.zodiakk.analyticservice.screening.dto.request.create.ScreeningResultCreateRequestDto;
import com.zodiakk.analyticservice.screening.dto.request.update.ScreeningResultUpdateRequestDto;
import com.zodiakk.analyticservice.screening.dto.response.ScreeningResultResponseDto;
import com.zodiakk.analyticservice.screening.entity.ScreeningResult;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface ScreeningResultMapper {

    ScreeningResult toEntity(ScreeningResultCreateRequestDto dto);

    ScreeningResultResponseDto toResponseDto(ScreeningResult entity);

    void updateEntity(ScreeningResultUpdateRequestDto dto, @MappingTarget ScreeningResult entity);
}
