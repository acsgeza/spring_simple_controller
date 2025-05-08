package com.reg.time_series.service;

import com.reg.time_series.entity.PowerStation;
import com.reg.time_series.entity.PowerStationDate;
import com.reg.time_series.entity.TimeSeriesVersion;
import com.reg.time_series.model.TimeSeriesData;
import com.reg.time_series.model.TimeSeriesViewDTO;
import com.reg.time_series.repositories.PowerStationRepository;
import com.reg.time_series.repositories.TimeSeriesRepository;
import jakarta.transaction.Transactional;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.*;


import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service providing operations for time series data management.
 * <p>
 * The TimeSeriesService handles the storage and merging of new time series data,
 * as well as managing the entities associated with the power station and time
 * series versions.
 * <p>
 * This service interacts with repositories to persist and retrieve data entities
 * such as PowerStation, PowerStationDate, and TimeSeriesVersion.
 */
@Log4j2
@Service
public class TimeSeriesService {
    private final TimeSeriesRepository repository;
    private final PowerStationRepository powerStationRepository;

    @Value("${time-series.safety-window-minutes}")
    private int safetyWindowMinutes;

    public TimeSeriesService(TimeSeriesRepository repository, PowerStationRepository powerStationRepository) {
        this.repository = repository;
        this.powerStationRepository = powerStationRepository;
    }

    @Transactional
    public void save(TimeSeriesData timeSeriesData) {
        PowerStation powerStation = getOrCreatePowerStation(timeSeriesData);
        PowerStationDate powerStationDate = getOrCreatePowerStationDate(powerStation, timeSeriesData);
        TimeSeriesVersion newVersion = createTimeSeriesVersion(powerStationDate, timeSeriesData);

        List<Integer> mergedSeries = mergeSeries(powerStationDate, timeSeriesData);
        newVersion.setSeries(mergedSeries);

        int nextVersion = calculateNextVersion(powerStationDate.getVersions());
        newVersion.setVersion(nextVersion);

        ensureVersionsList(powerStationDate);
        powerStationDate.getVersions().add(newVersion);
        repository.save(powerStationDate);
    }




    private List<Integer> mergeSeries(PowerStationDate powerStationDate, TimeSeriesData newData) {
        if (powerStationDate == null || newData == null) {
            throw new IllegalArgumentException("Input parameters cannot be null");
        }

        Optional<TimeSeriesVersion> latestVersionOpt = repository.findFirstByPowerStationDateOrderByVersionDesc(powerStationDate);
        if (latestVersionOpt.isEmpty()) {
            return Optional.ofNullable(newData.getSeries())
                    .orElseThrow(() -> new IllegalArgumentException("New series data cannot be null"));
        }

        TimeSeriesVersion latestVersion = latestVersionOpt.get();
        List<Integer> previousSeries = latestVersion.getSeries();
        List<Integer> newSeries = Optional.ofNullable(newData.getSeries())
                .orElseThrow(() -> new IllegalArgumentException("New series data cannot be null"));

        Duration previousPeriod = latestVersion.getPeriod();
        Duration newPeriod = tryParsePeriod(newData.getPeriod());

        // A safety window végének kiszámítása
        LocalDateTime timestamp = newData.getTimestamp();
        LocalDateTime safetyWindowEnd = calculateSafetyWindowEnd(newData, newPeriod);

        return mergeDifferentPeriodSeries(
                previousSeries,
                newSeries,
                previousPeriod,
                newPeriod,
                safetyWindowEnd,
                newData.getDate()
        );
    }

    private Duration tryParsePeriod(String period) {
        try {
            return Duration.parse(period);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid period format: " + period);
        }
    }

    private Duration parsePeriod(String periodStr) {
        return Duration.parse(periodStr);
    }

    private LocalDateTime calculateSafetyWindowEnd(TimeSeriesData data, Duration period) {
        LocalDateTime timestamp = data.getTimestamp();
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
        if (safetyWindowMinutes <= 0 || safetyWindowMinutes > 24 * 60) {
            throw new IllegalStateException("Safety window minutes must be between 1 and 1440");
        }

        // ZonedTime conversion
        ZoneId zone = ZoneId.of(data.getZone());
        ZonedDateTime zonedTime = timestamp.atZone(zone);

        // Calculate the next 15 minutes interval
        ZonedDateTime nextPeriodStart = calculateNextPeriodStart(zonedTime, (int) period.toMinutes());

        // Safety window depends on DST
        ZonedDateTime safetyWindowEnd = addSafetyWindow(nextPeriodStart);

        // Check midnight because the timeseries data handles by date
        if (!safetyWindowEnd.toLocalDate().equals(zonedTime.toLocalDate())) {
            safetyWindowEnd = zonedTime.toLocalDate()
                    .atTime(23, 59, 59)
                    .atZone(zone);
        }
        return safetyWindowEnd.toLocalDateTime();
    }

    private ZonedDateTime calculateNextPeriodStart(ZonedDateTime zonedTime, int actualPeriodLength
    ) {

        if (isDSTTransition(zonedTime)) { // ZonedTime.getOffset() UTC diff
            log.info("DST transition detected at {}", zonedTime);
        }

        ZonedDateTime truncatedHour = zonedTime.truncatedTo(ChronoUnit.HOURS);

        // minutes depend on DST
        long currentMinute = ChronoUnit.MINUTES.between(truncatedHour, zonedTime);

        int periodsElapsed = (int) (currentMinute / actualPeriodLength);
        int nextPeriodMinutes = (periodsElapsed + 1) * actualPeriodLength;

        return truncatedHour.plus(Duration.ofMinutes(nextPeriodMinutes));
    }

    private ZonedDateTime addSafetyWindow(ZonedDateTime startTime) {
        // A plusMinutes helyett Duration-t használunk, hogy helyesen kezelje a DST-t
        return startTime.plus(Duration.ofMinutes(safetyWindowMinutes));
    }

    private boolean isDSTTransition(ZonedDateTime time) {
        ZonedDateTime oneHourBefore = time.minusHours(1);
        return oneHourBefore.getOffset().getTotalSeconds() != time.getOffset().getTotalSeconds();
    }




    protected List<Integer> mergeDifferentPeriodSeries(
            List<Integer> previousSeries,
            List<Integer> newSeries,
            Duration previousPeriod,
            Duration newPeriod,
            LocalDateTime safetyWindowEnd,
            LocalDate date) {

        if (previousSeries == null || newSeries == null) {
            throw new IllegalArgumentException("Input series cannot be null");
        }
        if (previousPeriod == null || newPeriod == null) {
            throw new IllegalArgumentException("Period cannot be null");
        }
        if (safetyWindowEnd == null || date == null) {
            throw new IllegalArgumentException("DateTime parameters cannot be null");
        }

        List<Integer> mergedSeries = new ArrayList<>();
        LocalDateTime dayStart = date.atStartOfDay();

        // Safety window vége index számítás
        long safetyWindowEndMinutes = ChronoUnit.MINUTES.between(dayStart, safetyWindowEnd);
        long safetyWindowEndIndex = safetyWindowEndMinutes / previousPeriod.toMinutes();

        // Egy nap hossza 15 perces időszakokban
        int expectedSize = (int) (24 * 60 / previousPeriod.toMinutes());

        log.debug("Safety window end minutes: {}, index: {}", safetyWindowEndMinutes, safetyWindowEndIndex);

        for (int i = 0; i < expectedSize; i++) {
            int currentMinutes = i * (int)previousPeriod.toMinutes();

            if (currentMinutes < safetyWindowEndMinutes) {
                // Safety window-n belül: régi értékek
                if (i < previousSeries.size()) {
                    mergedSeries.add(previousSeries.get(i));
                }
            } else {
                // Safety window után: új értékek
                int newSeriesIndex = currentMinutes / (int)newPeriod.toMinutes();

                if (newSeriesIndex < newSeries.size()) {
                    mergedSeries.add(newSeries.get(newSeriesIndex));
                } else if (i < previousSeries.size()) {
                    mergedSeries.add(previousSeries.get(i));
                }
            }
        }

        log.info("Merged series size: {}, Expected size: {}",
                mergedSeries.size(), expectedSize);
        return mergedSeries;
    }

    private PowerStation getOrCreatePowerStation(TimeSeriesData data) {
        return powerStationRepository
                .findByPowerStation(data.getPowerStation())
                .orElseGet(() -> createPowerStation(data));
    }

    private PowerStation createPowerStation(TimeSeriesData data) {
        PowerStation newPowerStation = new PowerStation();
        newPowerStation.setPowerStation(data.getPowerStation());
        return powerStationRepository.save(newPowerStation);
    }

    private PowerStationDate getOrCreatePowerStationDate(PowerStation powerStation, TimeSeriesData data) {
        return repository
                .findByPowerStationAndStationDate(powerStation, data.getDate())
                .orElseGet(() -> createPowerStationDate(powerStation, data));
    }

    private PowerStationDate createPowerStationDate(PowerStation powerStation, TimeSeriesData data) {
        PowerStationDate newPowerStationDate = new PowerStationDate();
        newPowerStationDate.setPowerStation(powerStation);
        newPowerStationDate.setStationDate(data.getDate());
        newPowerStationDate.setZone(data.getZone());
        newPowerStationDate.setVersions(new ArrayList<>());
        return repository.save(newPowerStationDate);
    }

    private TimeSeriesVersion createTimeSeriesVersion(PowerStationDate powerStationDate, TimeSeriesData data) {
        TimeSeriesVersion newVersion = new TimeSeriesVersion();
        newVersion.setPowerStationDate(powerStationDate);
        newVersion.setTimestamp(data.getTimestamp());
        newVersion.setPeriod(parsePeriod(data.getPeriod()));
        newVersion.setSeries(data.getSeries());
        return newVersion;
    }


    private int calculateNextVersion(List<TimeSeriesVersion> versions) {
        if (versions.isEmpty()) {
            return 1;
        }
        return versions.stream()
                .mapToInt(TimeSeriesVersion::getVersion)
                .max()
                .orElse(0) + 1;
    }

    private void ensureVersionsList(PowerStationDate powerStationDate) {
        if (powerStationDate.getVersions() == null) {
            powerStationDate.setVersions(new ArrayList<>());
        }
    }

    public List<String> getAllPowerStations() {
        return powerStationRepository.findDistinctPowerStationNames();
    }

    public List<LocalDate> getAvailableDates(String powerStationName) {
        return repository.findDatesByPowerStation(powerStationName);
    }

    public TimeSeriesViewDTO getTimeSeriesView(String powerStationName, LocalDate date) {
        PowerStationDate powerStationDate = repository.findByPowerStationNameAndDate(powerStationName, date)
                .orElseThrow(() -> new IllegalArgumentException("No data found for given power station and date"));

        TimeSeriesViewDTO dto = new TimeSeriesViewDTO();
        dto.setPowerStationName(powerStationName);
        dto.setDate(date.toString());

        dto.setVersions(powerStationDate.getVersions().stream()
                .map(v -> {
                    TimeSeriesViewDTO.VersionInfo versionInfo = new TimeSeriesViewDTO.VersionInfo();
                    versionInfo.setVersion(v.getVersion());
                    versionInfo.setTimestamp(v.getTimestamp());
                    return versionInfo;
                }).toList());

        Duration periodLength = powerStationDate.getVersions().get(0).getPeriod();
        int periodsPerDay = (int) (24 * 60 / periodLength.toMinutes());

        List<TimeSeriesViewDTO.TimeSeriesRow> rows = new ArrayList<>();
        for (int i = 0; i < periodsPerDay; i++) {
            TimeSeriesViewDTO.TimeSeriesRow row = new TimeSeriesViewDTO.TimeSeriesRow();

            int minutes = (int) (i * periodLength.toMinutes());
            int hours = minutes / 60;
            minutes = minutes % 60;
            row.setTimeSlot(String.format("%02d:%02d", hours, minutes));

            Map<Integer, Integer> versionValues = new HashMap<>();
            for (TimeSeriesVersion version : powerStationDate.getVersions()) {
                if (i < version.getSeries().size()) {
                    versionValues.put(version.getVersion(), version.getSeries().get(i));
                }
            }
            row.setVersionValues(versionValues);

            LocalDateTime timeSlotDateTime = date.atStartOfDay().plusMinutes(i * periodLength.toMinutes());
            row.setInSafetyWindow(isInSafetyWindow(timeSlotDateTime, powerStationDate.getZone(), powerStationDate));

            rows.add(row);
        }
        dto.setRows(rows);

        return dto;
    }

    private boolean isInSafetyWindow(LocalDateTime timeSlot, String zoneId, PowerStationDate powerStationDate) {
        ZoneId zone = ZoneId.of(zoneId);
        ZonedDateTime zonedTimeSlot = timeSlot.atZone(zone);

        ZonedDateTime now = ZonedDateTime.now(zone);
        Duration actualPeriod = powerStationDate.getVersions().get(0).getPeriod();
        ZonedDateTime nextPeriodStart = calculateNextPeriodStart(now, (int) actualPeriod.toMinutes());

        ZonedDateTime safetyWindowEnd = nextPeriodStart.plusMinutes(safetyWindowMinutes);

        return zonedTimeSlot.toLocalDate().equals(now.toLocalDate()) &&
                !zonedTimeSlot.isAfter(safetyWindowEnd);
    }
}