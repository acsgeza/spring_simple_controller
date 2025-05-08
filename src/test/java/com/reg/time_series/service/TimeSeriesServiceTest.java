package com.reg.time_series.service;

import com.reg.time_series.entity.PowerStation;
import com.reg.time_series.entity.PowerStationDate;
import com.reg.time_series.model.TimeSeriesData;
import com.reg.time_series.repositories.PowerStationRepository;
import com.reg.time_series.repositories.TimeSeriesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


class TimeSeriesServiceTest {

    private TimeSeriesService timeSeriesService;
    private TimeSeriesRepository timeSeriesRepository;
    private PowerStationRepository powerStationRepository;

    @BeforeEach
    void setUp() {
        timeSeriesRepository = mock(TimeSeriesRepository.class);
        powerStationRepository = mock(PowerStationRepository.class);
        timeSeriesService = new TimeSeriesService(timeSeriesRepository, powerStationRepository);
    }


    @Test
    void testMergeDifferentPeriodSeriesWithOverlap() {
        LocalDate testDate = LocalDate.of(2024, 3, 20);
        LocalDateTime dayStart = testDate.atStartOfDay();

        // Previous series: 15 min period
        List<Integer> previousSeries = Arrays.asList(
                10, 20, 30, 40  // 00:00 - 01:00
        );

        // New series: 5 min period
        List<Integer> newSeries = Arrays.asList(
                101, 102, 103,   // 00:00 - 00:15
                104, 105, 106,   // 00:15 - 00:30
                107, 108, 109,   // 00:30 - 00:45
                110, 111, 112    // 00:45 - 01:00
        );

        Duration previousPeriod = Duration.ofMinutes(15);
        Duration newPeriod = Duration.ofMinutes(5);

        LocalDateTime timestamp = dayStart;  // 00:00
        LocalDateTime safetyWindowEnd = dayStart.plusMinutes(30);  // 00:30

        // Call implementation
        List<Integer> result = timeSeriesService.mergeDifferentPeriodSeries(
                previousSeries,
                newSeries,
                previousPeriod,
                newPeriod,
                safetyWindowEnd,
                testDate
        );

        // Check first 2 items
        assertEquals(previousSeries.get(0), result.get(0), "Az első elem nem egyezik (00:00-00:15)");
        assertEquals(previousSeries.get(1), result.get(1), "A második elem nem egyezik (00:15-00:30)");

        // After Safety window
        assertEquals(newSeries.get(6), result.get(2), "A harmadik elem nem egyezik (00:30-00:45)");
        assertEquals(newSeries.get(9), result.get(3), "A negyedik elem nem egyezik (00:45-01:00)");
    }


}