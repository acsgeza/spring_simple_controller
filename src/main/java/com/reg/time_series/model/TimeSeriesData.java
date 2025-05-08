package com.reg.time_series.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TimeSeriesData {
    @JsonProperty("power-station")
    private String powerStation;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    private String zone;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    private String period;
    private List<Integer> series;
}

