package com.reg.time_series.exceptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        webRequest = mock(WebRequest.class);
        when(webRequest.getDescription(false)).thenReturn("uri=/test");
    }

    @Test
    void handleIllegalArgumentException_ShouldReturnBadRequest() {
        // Arrange
        String errorMessage = "Invalid value";
        IllegalArgumentException ex = new IllegalArgumentException(errorMessage);

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgumentException(ex, webRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .satisfies(errorResponse -> {
                    assertThat(errorResponse.getStatus()).isEqualTo(400);
                    assertThat(errorResponse.getError()).isEqualTo("Invalid input");
                    assertThat(errorResponse.getMessage()).isEqualTo(errorMessage);
                    assertThat(errorResponse.getPath()).isEqualTo("uri=/test");
                    assertThat(errorResponse.getTimestamp()).isBefore(LocalDateTime.now());
                });
    }

    @Test
    void handleDateTimeParseException_ShouldReturnBadRequest() {
        // Arrange
        String invalidDate = "2024-13-45";
        DateTimeParseException ex = new DateTimeParseException("Invalid date format", invalidDate, 0);

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleDateTimeParseException(ex, webRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .satisfies(errorResponse -> {
                    assertThat(errorResponse.getStatus()).isEqualTo(400);
                    assertThat(errorResponse.getError()).isEqualTo("Dateformat error");
                    assertThat(errorResponse.getMessage()).contains(invalidDate);
                    assertThat(errorResponse.getPath()).isEqualTo("uri=/test");
                });
    }

    @Test
    void handleIllegalStateException_ShouldReturnInternalServerError() {
        // Arrange
        String errorMessage = "Internal system error";
        IllegalStateException ex = new IllegalStateException(errorMessage);

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleIllegalStateException(ex, webRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
                .satisfies(errorResponse -> {
                    assertThat(errorResponse.getStatus()).isEqualTo(500);
                    assertThat(errorResponse.getError()).isEqualTo("Internal server error");
                    assertThat(errorResponse.getMessage()).isEqualTo(errorMessage);
                });
    }

    @Test
    void handleAllUncaughtException_ShouldReturnInternalServerError() {
        // Arrange
        String errorMessage = "Unexpected error";
        Exception ex = new Exception(errorMessage);

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleAllUncaughtException(ex, webRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
                .satisfies(errorResponse -> {
                    assertThat(errorResponse.getStatus()).isEqualTo(500);
                    assertThat(errorResponse.getError()).isEqualTo("Unexpected error occured. Please contact the administrator.");
                    assertThat(errorResponse.getMessage()).isEqualTo(errorMessage);
                });
    }

    @Test
    void handleTimeSeriesNotFoundException_ShouldReturnNotFound() {
        // Arrange
        String errorMessage = "Timeseries not found";
        TimeSeriesNotFoundException ex = new TimeSeriesNotFoundException(errorMessage);

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleTimeSeriesNotFoundException(ex, webRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody())
                .satisfies(errorResponse -> {
                    assertThat(errorResponse.getStatus()).isEqualTo(404);
                    assertThat(errorResponse.getError()).isEqualTo("Timeseries not found");
                    assertThat(errorResponse.getMessage()).isEqualTo(errorMessage);
                    assertThat(errorResponse.getPath()).isEqualTo("uri=/test");
                });
    }
}