package com.reg.time_series.service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DSTTransitionTest {
    private TimeSeriesService timeSeriesService;

    @BeforeEach
    void setUp() {
        timeSeriesService = new TimeSeriesService(null, null);
    }

    @Test
    @DisplayName("Should detect DST start transition")
    void isDSTTransition_DSTStart() {
        // Arrange
        // 2024 March 31 02:00 -> 03:00 (DST starts in Europe)
        ZonedDateTime dstTransitionTime = ZonedDateTime.of(
                2024, 3, 31, 3, 0, 0, 0,
                ZoneId.of("Europe/Budapest")
        );

        // Act
        boolean result = ReflectionTestUtils.invokeMethod(
                timeSeriesService,
                "isDSTTransition",
                dstTransitionTime
        );

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should detect DST end transition")
    void isDSTTransition_DSTEnd() {
        // Arrange
        // 2024 October 27 03:00 -> 02:00 (DST ends in Europe)
        ZonedDateTime dstTransitionTime = ZonedDateTime.of(
                2024, 10, 27, 3, 0, 0, 0,
                ZoneId.of("Europe/Budapest")
        ).minusHours(1);  // Ez adja meg a valódi átállási időpontot
        System.out.println(dstTransitionTime);
        // Act
        boolean result = ReflectionTestUtils.invokeMethod(
                timeSeriesService,
                "isDSTTransition",
                dstTransitionTime
        );

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false for non-DST transition time")
    void isDSTTransition_NonDSTTransition() {
        // Arrange
        ZonedDateTime normalTime = ZonedDateTime.of(
                2024, 6, 15, 14, 0, 0, 0,
                ZoneId.of("Europe/Budapest")
        );

        // Act
        boolean result = ReflectionTestUtils.invokeMethod(
                timeSeriesService,
                "isDSTTransition",
                normalTime
        );

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should handle midnight transition correctly")
    void isDSTTransition_AtMidnight() {
        // Arrange
        ZonedDateTime midnightTime = ZonedDateTime.of(
                2024, 6, 15, 0, 0, 0, 0,
                ZoneId.of("Europe/Budapest")
        );

        // Act
        boolean result = ReflectionTestUtils.invokeMethod(
                timeSeriesService,
                "isDSTTransition",
                midnightTime
        );

        // Assert
        assertFalse(result);
    }
}
