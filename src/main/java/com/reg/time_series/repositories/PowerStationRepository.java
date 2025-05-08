package com.reg.time_series.repositories;

import com.reg.time_series.entity.PowerStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PowerStationRepository extends JpaRepository<PowerStation, Long> {
    Optional<PowerStation> findByPowerStation(String powerStation);

    @Query("SELECT DISTINCT p.powerStation FROM PowerStation p")
    List<String> findDistinctPowerStationNames();

}

