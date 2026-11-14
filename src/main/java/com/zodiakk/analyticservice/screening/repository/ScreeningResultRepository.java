package com.zodiakk.analyticservice.screening.repository;

import com.zodiakk.analyticservice.screening.entity.ScreeningResult;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface ScreeningResultRepository extends JpaRepository<ScreeningResult, UUID> {

    List<ScreeningResult> findAllByScreeningIdOrderByRankAsc(UUID screeningId);

    Optional<ScreeningResult> findByScreeningIdAndEmployeeId(UUID screeningId, UUID employeeId);

    boolean existsByScreeningIdAndEmployeeId(UUID screeningId, UUID employeeId);

    long countByScreeningId(UUID screeningId);

}
