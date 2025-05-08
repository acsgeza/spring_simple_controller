package com.reg.time_series.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimeSeriesMergeTest {

    private TimeSeriesService timeSeriesService;

    @BeforeEach
    void setUp() {
        timeSeriesService = new TimeSeriesService(null, null);
    }

    @Test
    @DisplayName("Azonos periódusú sorozatok egyesítése safety window-val")
    void mergeSamePeriodSeries() {
        // Arrange
        LocalDate testDate = LocalDate.of(2024, 3, 20);
        List<Integer> previousSeries = Arrays.asList(1, 1, 1, 1);
        List<Integer> newSeries = Arrays.asList(2, 2, 2, 2);
        Duration period = Duration.ofMinutes(15);
        LocalDateTime safetyWindowEnd = testDate.atStartOfDay().plusMinutes(30);

        // Act
        List<Integer> result = timeSeriesService.mergeDifferentPeriodSeries(
                previousSeries,
                newSeries,
                period,
                period,
                safetyWindowEnd,
                testDate
        );

        // Assert
        assertThat(result)
                .hasSize(4)
                .containsExactly(1, 1, 2, 2);
    }

    @Test
    @DisplayName("Különböző periódusú sorozatok egyesítése")
    void mergeDifferentPeriodSeries() {
        // Arrange
        LocalDate testDate = LocalDate.of(2024, 3, 20);
        List<Integer> previousSeries = Arrays.asList(10, 20, 30, 40);
        List<Integer> newSeries = Arrays.asList(
                100, 101, 102,
                200, 201, 202,
                300, 301, 302,
                400, 401, 402
        );
        Duration previousPeriod = Duration.ofMinutes(15);
        Duration newPeriod = Duration.ofMinutes(5);
        LocalDateTime safetyWindowEnd = testDate.atStartOfDay().plusMinutes(30);

        // Act
        List<Integer> result = timeSeriesService.mergeDifferentPeriodSeries(
                previousSeries,
                newSeries,
                previousPeriod,
                newPeriod,
                safetyWindowEnd,
                testDate
        );

        // Assert
        assertThat(result)
                .hasSize(4)
                .containsExactly(10, 20, 300, 400);
    }

    @Test
    @DisplayName("Üres safety window utáni egyesítés")
    void mergeWithEmptySafetyWindow() {
        // Arrange
        LocalDate testDate = LocalDate.of(2024, 3, 20);
        List<Integer> previousSeries = Arrays.asList(1, 1, 1, 1);
        List<Integer> newSeries = Arrays.asList(2, 2, 2, 2);
        Duration period = Duration.ofMinutes(15);
        LocalDateTime safetyWindowEnd = testDate.atStartOfDay(); // Nincs safety window

        // Act
        List<Integer> result = timeSeriesService.mergeDifferentPeriodSeries(
                previousSeries,
                newSeries,
                period,
                period,
                safetyWindowEnd,
                testDate
        );

        // Assert
        assertThat(result)
                .hasSize(4)
                .containsExactly(2, 2, 2, 2);
    }

    @Test
    @DisplayName("Teljes safety window-val történő egyesítés")
    void mergeWithFullSafetyWindow() {
        // Arrange
        LocalDate testDate = LocalDate.of(2024, 3, 20);
        List<Integer> previousSeries = Arrays.asList(1, 1, 1, 1);
        List<Integer> newSeries = Arrays.asList(2, 2, 2, 2);
        Duration period = Duration.ofMinutes(15);
        LocalDateTime safetyWindowEnd = testDate.atStartOfDay().plusHours(1); // Teljes időszak védett

        // Act
        List<Integer> result = timeSeriesService.mergeDifferentPeriodSeries(
                previousSeries,
                newSeries,
                period,
                period,
                safetyWindowEnd,
                testDate
        );

        // Assert
        assertThat(result)
                .hasSize(4)
                .containsExactly(1, 1, 1, 1);
    }

    @Test
    @DisplayName("Null értékek kezelése")
    void mergeWithNullValues() {
        // Arrange
        LocalDate testDate = LocalDate.of(2024, 3, 20);
        Duration period = Duration.ofMinutes(15);
        LocalDateTime safetyWindowEnd = testDate.atStartOfDay().plusMinutes(30);

        // Assert
        assertThrows(IllegalArgumentException.class, () ->
                timeSeriesService.mergeDifferentPeriodSeries(
                        null,
                        Arrays.asList(1, 2, 3),
                        period,
                        period,
                        safetyWindowEnd,
                        testDate
                ));

        assertThrows(IllegalArgumentException.class, () ->
                timeSeriesService.mergeDifferentPeriodSeries(
                        Arrays.asList(1, 2, 3),
                        null,
                        period,
                        period,
                        safetyWindowEnd,
                        testDate
                ));
    }
}
