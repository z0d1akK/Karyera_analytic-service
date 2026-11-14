package com.zodiakk.analyticservice.projection.mapper;

import com.zodiakk.analyticservice.config.MapStructConfig;
import com.zodiakk.analyticservice.projection.dto.request.create.EmployeeActivityAggregateCreateRequestDto;
import com.zodiakk.analyticservice.projection.dto.request.update.EmployeeActivityAggregateUpdateRequestDto;
import com.zodiakk.analyticservice.projection.dto.response.EmployeeActivityAggregateResponseDto;
import com.zodiakk.analyticservice.projection.entity.EmployeeActivityAggregate;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface EmployeeActivityAggregateMapper {

    EmployeeActivityAggregate toEntity(EmployeeActivityAggregateCreateRequestDto dto);

    EmployeeActivityAggregateResponseDto toResponseDto(EmployeeActivityAggregate entity);

    void updateEntity(EmployeeActivityAggregateUpdateRequestDto dto, @MappingTarget EmployeeActivityAggregate entity);
}
