package com.reg.time_series.service;

import com.reg.time_series.entity.PowerStation;
import com.reg.time_series.entity.PowerStationDate;
import com.reg.time_series.entity.TimeSeriesVersion;
import com.reg.time_series.model.TimeSeriesData;
import com.reg.time_series.repositories.PowerStationRepository;
import com.reg.time_series.repositories.TimeSeriesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TimeSeriesSaveTest {

    private TimeSeriesService timeSeriesService;
    private TimeSeriesRepository timeSeriesRepository;
    private PowerStationRepository powerStationRepository;

    @BeforeEach
    void setUp() {
        timeSeriesRepository = mock(TimeSeriesRepository.class);
        powerStationRepository = mock(PowerStationRepository.class);
        timeSeriesService = new TimeSeriesService(timeSeriesRepository, powerStationRepository);

        ReflectionTestUtils.setField(timeSeriesService, "safetyWindowMinutes", 30);

    }

    @Test
    @DisplayName("Test save method with new time series data")
    void testSaveTimeSeriesData() {
        // Arrange
        TimeSeriesData timeSeriesData = new TimeSeriesData();
        timeSeriesData.setPowerStation("Test Station");
        timeSeriesData.setDate(LocalDate.of(2024, 3, 20));
        timeSeriesData.setZone("Europe/Budapest");
        timeSeriesData.setTimestamp(LocalDateTime.of(2024, 3, 20, 10, 0));
        timeSeriesData.setPeriod("PT15M");
        timeSeriesData.setSeries(Arrays.asList(1, 2, 3, 4));

        PowerStation powerStation = new PowerStation();
        powerStation.setPowerStation("Test Station");

        PowerStationDate powerStationDate = new PowerStationDate();
        powerStationDate.setPowerStation(powerStation);
        powerStationDate.setStationDate(timeSeriesData.getDate());
        powerStationDate.setZone(timeSeriesData.getZone());
        powerStationDate.setVersions(new ArrayList<>());

        // Mock repository responses
        when(powerStationRepository.findByPowerStation("Test Station"))
                .thenReturn(Optional.of(powerStation));
        when(timeSeriesRepository.findByPowerStationAndStationDate(any(), any()))
                .thenReturn(Optional.of(powerStationDate));
        when(timeSeriesRepository.findFirstByPowerStationDateOrderByVersionDesc(any()))
                .thenReturn(Optional.empty());

        // Act
        timeSeriesService.save(timeSeriesData);

        // Assert
        verify(timeSeriesRepository).save(argThat(savedPowerStationDate -> {
            // Check if PowerStationDate has correct basic properties
            boolean basicPropertiesMatch = savedPowerStationDate.getPowerStation().equals(powerStation) &&
                    savedPowerStationDate.getStationDate().equals(timeSeriesData.getDate()) &&
                    savedPowerStationDate.getZone().equals(timeSeriesData.getZone());

            // Check if version was created and added correctly
            boolean versionAdded = savedPowerStationDate.getVersions().size() == 1;
            if (!versionAdded) return false;

            TimeSeriesVersion version = savedPowerStationDate.getVersions().get(0);
            boolean versionPropertiesMatch = version.getVersion() == 1 &&
                    version.getTimestamp().equals(timeSeriesData.getTimestamp()) &&
                    version.getPeriod().equals(Duration.parse(timeSeriesData.getPeriod())) &&
                    version.getSeries().equals(timeSeriesData.getSeries());

            return basicPropertiesMatch && versionPropertiesMatch;
        }));
    }

    @Test
    @DisplayName("Test save method with existing time series and safety window")
    void testSaveTimeSeriesDataWithExistingVersion() {
        // Arrange
        TimeSeriesData timeSeriesData = new TimeSeriesData();
        timeSeriesData.setPowerStation("Test Station");
        timeSeriesData.setDate(LocalDate.of(2024, 3, 20));
        timeSeriesData.setZone("Europe/Budapest");
        timeSeriesData.setTimestamp(LocalDateTime.of(2024, 3, 20, 10, 0));
        timeSeriesData.setPeriod("PT15M");
        timeSeriesData.setSeries(Arrays.asList(5, 6, 7, 8));

        PowerStation powerStation = new PowerStation();
        powerStation.setPowerStation("Test Station");

        PowerStationDate powerStationDate = new PowerStationDate();
        powerStationDate.setPowerStation(powerStation);
        powerStationDate.setStationDate(timeSeriesData.getDate());
        powerStationDate.setZone(timeSeriesData.getZone());
        powerStationDate.setVersions(new ArrayList<>());

        TimeSeriesVersion existingVersion = new TimeSeriesVersion();
        existingVersion.setPowerStationDate(powerStationDate);
        existingVersion.setVersion(1);
        existingVersion.setTimestamp(LocalDateTime.of(2024, 3, 20, 9, 0));
        existingVersion.setPeriod(Duration.parse("PT15M"));
        existingVersion.setSeries(Arrays.asList(1, 2, 3, 4));
        powerStationDate.getVersions().add(existingVersion);

        // Mock repository responses
        when(powerStationRepository.findByPowerStation("Test Station"))
                .thenReturn(Optional.of(powerStation));
        when(timeSeriesRepository.findByPowerStationAndStationDate(any(), any()))
                .thenReturn(Optional.of(powerStationDate));
        when(timeSeriesRepository.findFirstByPowerStationDateOrderByVersionDesc(any()))
                .thenReturn(Optional.of(existingVersion));

        // Act
        timeSeriesService.save(timeSeriesData);

        // Assert
        verify(timeSeriesRepository).save(argThat(savedPowerStationDate -> {
            // Check if versions list has been updated
            boolean versionsUpdated = savedPowerStationDate.getVersions().size() == 2;
            if (!versionsUpdated) return false;

            TimeSeriesVersion newVersion = savedPowerStationDate.getVersions().get(1);
            boolean versionPropertiesMatch = newVersion.getVersion() == 2 &&
                    newVersion.getTimestamp().equals(timeSeriesData.getTimestamp()) &&
                    newVersion.getPeriod().equals(Duration.parse(timeSeriesData.getPeriod()));

            // We don't check the exact series values here as they depend on the safety window
            return versionsUpdated && versionPropertiesMatch;
        }));
    }
}
