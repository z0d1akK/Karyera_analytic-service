package com.zodiakk.analyticservice.job.mapper;

import com.zodiakk.analyticservice.config.MapStructConfig;
import com.zodiakk.analyticservice.job.dto.request.create.AnalyticsJobCreateRequestDto;
import com.zodiakk.analyticservice.job.dto.request.update.AnalyticsJobUpdateRequestDto;
import com.zodiakk.analyticservice.job.dto.response.AnalyticsJobResponseDto;
import com.zodiakk.analyticservice.job.entity.AnalyticsJob;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface AnalyticsJobMapper {

    AnalyticsJob toEntity(AnalyticsJobCreateRequestDto dto);

    AnalyticsJobResponseDto toResponseDto(AnalyticsJob entity);

    void updateEntity(AnalyticsJobUpdateRequestDto dto, @MappingTarget AnalyticsJob entity);
}
