package com.reg.time_series.exceptions;


public class TimeSeriesNotFoundException extends RuntimeException {
    public TimeSeriesNotFoundException(String message) {
        super(message);
    }
}