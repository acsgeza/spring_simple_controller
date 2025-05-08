package com.reg.time_series.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "time_series_versions")
public class TimeSeriesVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "time_series_id", nullable = false)
    private PowerStationDate powerStationDate;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "period", nullable = false)
    private Duration period;

    @ElementCollection
    @CollectionTable(name = "time_series_values", joinColumns = @JoinColumn(name = "time_series_version_id"))
    @Column(name = "series_value")
    private List<Integer> series;
}

