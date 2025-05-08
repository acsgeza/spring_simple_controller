package com.reg.time_series.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class TimeSeriesViewDTO {
    private String powerStationName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String date;
    private List<VersionInfo> versions;
    private List<TimeSeriesRow> rows;
    private boolean hasSafetyWindow;

    @Data
    public static class VersionInfo {
        private int version;
        private LocalDateTime timestamp;
    }

    @Data
    public static class TimeSeriesRow {
        private String timeSlot;
        private Map<Integer, Integer> versionValues;
        private boolean inSafetyWindow;
    }
}
