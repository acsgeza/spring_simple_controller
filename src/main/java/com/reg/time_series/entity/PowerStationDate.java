package com.reg.time_series.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDate;
import java.util.List;

@Data
@Entity
@Table(name = "power_station_date",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"power_station_id", "station_date"})
        },
        indexes = {
                @Index(columnList = "power_station_id,station_date")
        }
)

public class PowerStationDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @OneToMany(mappedBy = "powerStationDate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeSeriesVersion> versions;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "power_station_id", nullable = false)
    private PowerStation powerStation;

    @Column(name = "station_date", nullable = false)
    private LocalDate stationDate;

    @Column(name = "zone", nullable = false)
    private String zone;

}
