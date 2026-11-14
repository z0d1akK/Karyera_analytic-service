package com.zodiakk.analyticservice.screening.repository;

import com.zodiakk.analyticservice.common.enums.*;
import com.zodiakk.analyticservice.screening.entity.TalentScreening;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface TalentScreeningRepository extends JpaRepository<TalentScreening, UUID> {

    List<TalentScreening> findAllByReserveIdOrderByCreatedAtDesc(UUID reserveId);

    List<TalentScreening> findAllByJobId(UUID jobId);

    List<TalentScreening> findAllByStatus(ScreeningStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM TalentScreening s WHERE s.id = :screeningId")
    Optional<TalentScreening> findByIdForUpdate(@Param("screeningId") UUID screeningId);

}
