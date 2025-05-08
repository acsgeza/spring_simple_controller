package com.reg.time_series.service;


import com.reg.time_series.model.TimeSeriesData;
import com.reg.time_series.repositories.PowerStationRepository;
import com.reg.time_series.repositories.TimeSeriesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class SafetyWindowCalculationTest {
    private TimeSeriesService timeSeriesService;

    @BeforeEach
    void setUp() {
        TimeSeriesRepository timeSeriesRepository = mock(TimeSeriesRepository.class);
        PowerStationRepository powerStationRepository = mock(PowerStationRepository.class);
        timeSeriesService = new TimeSeriesService(timeSeriesRepository, powerStationRepository);
        // Set safety window minutes using reflection since it's a private field
        ReflectionTestUtils.setField(timeSeriesService, "safetyWindowMinutes", 30);
    }

    @Test
    @DisplayName("Calculate safety window end with valid input")
    void calculateSafetyWindowEnd_WithValidInput() throws Exception {
        // Arrange
        TimeSeriesData data = new TimeSeriesData();
        data.setTimestamp(LocalDateTime.of(2024, 3, 20, 14, 7)); // 14:07
        data.setZone("Europe/Budapest");
        Duration period = Duration.ofMinutes(15);

        // Act
        LocalDateTime result = invokeCalculateSafetyWindowEnd(data, period);

        // Assert
        // Next 15-min interval after 14:07 is 14:15
        // Safety window end should be 14:15 + 30 minutes = 14:45
        LocalDateTime expected = LocalDateTime.of(2024, 3, 20, 14, 45);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Calculate safety window end near midnight")
    void calculateSafetyWindowEnd_NearMidnight() throws Exception {
        // Arrange
        TimeSeriesData data = new TimeSeriesData();
        data.setTimestamp(LocalDateTime.of(2024, 3, 20, 23, 50));
        data.setZone("Europe/Budapest");
        Duration period = Duration.ofMinutes(15);

        // Act
        LocalDateTime result = invokeCalculateSafetyWindowEnd(data, period);

        // Assert
        // Should be capped at 23:59:59 of the same day
        LocalDateTime expected = LocalDateTime.of(2024, 3, 20, 23, 59, 59);
        assertEquals(expected, result);
    }


    @Test
    @DisplayName("Calculate safety window end with null timestamp")
    void calculateSafetyWindowEnd_WithNullTimestamp() {
        // Arrange
        TimeSeriesData data = new TimeSeriesData();
        data.setZone("Europe/Budapest");
        Duration period = Duration.ofMinutes(15);

        // Act & Assert
        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(
                        timeSeriesService,
                        "calculateSafetyWindowEnd",
                        data,
                        period
                )
        );

        assertEquals("Timestamp cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Calculate safety window with invalid safety window minutes")
    void calculateSafetyWindowEnd_WithInvalidSafetyWindowMinutes() {
        // Arrange
        TimeSeriesData data = new TimeSeriesData();
        data.setTimestamp(LocalDateTime.of(2024, 3, 20, 14, 0));
        data.setZone("Europe/Budapest");
        Duration period = Duration.ofMinutes(15);

        // Set invalid safety window minutes
        ReflectionTestUtils.setField(timeSeriesService, "safetyWindowMinutes", 0);

        // Act & Assert
        Exception exception = assertThrows(
                IllegalStateException.class,
                () -> ReflectionTestUtils.invokeMethod(
                        timeSeriesService,
                        "calculateSafetyWindowEnd",
                        data,
                        period
                )
        );

        assertEquals("Safety window minutes must be between 1 and 1440", exception.getMessage());
    }

    @Test
    @DisplayName("Calculate safety window end during DST change")
    void calculateSafetyWindowEnd_DuringDSTChange() throws Exception {
        // Arrange
        TimeSeriesData data = new TimeSeriesData();
        // 2024 March 31 02:00 -> 03:00 (DST start in Europe)
        data.setTimestamp(LocalDateTime.of(2024, 3, 31, 1, 45));
        data.setZone("Europe/Budapest");
        Duration period = Duration.ofMinutes(15);

        // Act
        LocalDateTime result = invokeCalculateSafetyWindowEnd(data, period);

        // Assert
        // Next 15-min interval would be 02:00, which becomes 03:00 due to DST
        // Safety window end should be 03:00 + 30 minutes = 03:30
        LocalDateTime expected = LocalDateTime.of(2024, 3, 31, 3, 30);
        assertEquals(expected, result);
    }

    // Helper method to invoke private calculateSafetyWindowEnd method
    private LocalDateTime invokeCalculateSafetyWindowEnd(TimeSeriesData data, Duration period) throws Exception {
        Method method = TimeSeriesService.class.getDeclaredMethod("calculateSafetyWindowEnd",
                TimeSeriesData.class, Duration.class);
        method.setAccessible(true);
        return (LocalDateTime) method.invoke(timeSeriesService, data, period);
    }

    // Helper method to set private field value
    private void setPrivateField(Object object, String fieldName, Object value) {
        try {
            Field field = TimeSeriesService.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(object, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

