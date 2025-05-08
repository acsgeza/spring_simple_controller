package com.reg.time_series.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
public class PowerStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "power_station", nullable = false)
    private String powerStation;

    @OneToMany(mappedBy = "powerStation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PowerStationDate> powerStationDates;
}

