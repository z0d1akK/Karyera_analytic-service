package com.zodiakk.analyticservice.projection.mapper;

import com.zodiakk.analyticservice.config.MapStructConfig;
import com.zodiakk.analyticservice.projection.dto.request.create.EmployeeAnalyticsProfileCreateRequestDto;
import com.zodiakk.analyticservice.projection.dto.request.update.EmployeeAnalyticsProfileUpdateRequestDto;
import com.zodiakk.analyticservice.projection.dto.response.EmployeeAnalyticsProfileResponseDto;
import com.zodiakk.analyticservice.projection.entity.EmployeeAnalyticsProfile;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface EmployeeAnalyticsProfileMapper {

    EmployeeAnalyticsProfile toEntity(EmployeeAnalyticsProfileCreateRequestDto dto);

    EmployeeAnalyticsProfileResponseDto toResponseDto(EmployeeAnalyticsProfile entity);

    void updateEntity(EmployeeAnalyticsProfileUpdateRequestDto dto, @MappingTarget EmployeeAnalyticsProfile entity);
}
