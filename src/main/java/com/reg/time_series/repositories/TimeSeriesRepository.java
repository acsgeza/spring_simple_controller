package com.reg.time_series.repositories;

import com.reg.time_series.entity.PowerStation;
import com.reg.time_series.entity.PowerStationDate;
import com.reg.time_series.entity.TimeSeriesVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import lombok.NonNull;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TimeSeriesRepository extends JpaRepository<PowerStationDate, Long> {

    Optional<PowerStationDate> findByPowerStationAndStationDate(
            @NonNull PowerStation powerStation,
            @NonNull LocalDate stationDate);

    @Query(value = "SELECT v FROM TimeSeriesVersion v " +
            "WHERE v.powerStationDate = :psd " +
            "ORDER BY v.version DESC " +
            "FETCH FIRST 1 ROWS ONLY")
    Optional<TimeSeriesVersion> findFirstByPowerStationDateOrderByVersionDesc(
            @Param("psd") PowerStationDate powerStationDate);

    @Query("SELECT DISTINCT psd.stationDate FROM PowerStationDate psd " +
            "WHERE psd.powerStation.powerStation = :powerStationName ORDER BY psd.stationDate")
    List<LocalDate> findDatesByPowerStation(@Param("powerStationName") String powerStationName);

    @Query("SELECT psd FROM PowerStationDate psd " +
            "LEFT JOIN FETCH psd.versions v " +
            "WHERE psd.powerStation.powerStation = :powerStationName " +
            "AND psd.stationDate = :date")
    Optional<PowerStationDate> findByPowerStationNameAndDate(
            @Param("powerStationName") String powerStationName,
            @Param("date") LocalDate date);


}