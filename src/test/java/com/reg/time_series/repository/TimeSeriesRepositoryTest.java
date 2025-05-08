package com.reg.time_series.repository;

import com.reg.time_series.entity.PowerStation;
import com.reg.time_series.entity.PowerStationDate;
import com.reg.time_series.entity.TimeSeriesVersion;
import com.reg.time_series.repositories.TimeSeriesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TimeSeriesRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TimeSeriesRepository repository;

    private PowerStation powerStation;
    private PowerStationDate powerStationDate;
    private TimeSeriesVersion version;

    @BeforeEach
    void setUp() {
        // PowerStation létrehozása
        powerStation = new PowerStation();
        powerStation.setPowerStation("Test Station");
        powerStation = entityManager.persist(powerStation);

        // PowerStationDate létrehozása
        powerStationDate = new PowerStationDate();
        powerStationDate.setPowerStation(powerStation);
        powerStationDate.setStationDate(LocalDate.of(2024, 3, 20));
        powerStationDate.setZone("Europe/Budapest");
        powerStationDate.setVersions(new ArrayList<>());
        powerStationDate = entityManager.persist(powerStationDate);

        // TimeSeriesVersion létrehozása
        version = new TimeSeriesVersion();
        version.setPowerStationDate(powerStationDate);
        version.setVersion(1);
        version.setTimestamp(LocalDateTime.now());
        version.setPeriod(Duration.ofMinutes(15));
        version.setSeries(List.of(1, 2, 3, 4));
        version = entityManager.persist(version);

        powerStationDate.getVersions().add(version);
        entityManager.flush();
    }

    @Test
    void findByPowerStationAndStationDate_WhenExists_ShouldReturnPowerStationDate() {
        // Act
        Optional<PowerStationDate> found = repository.findByPowerStationAndStationDate(
                powerStation,
                LocalDate.of(2024, 3, 20)
        );

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getPowerStation()).isEqualTo(powerStation);
        assertThat(found.get().getStationDate()).isEqualTo(LocalDate.of(2024, 3, 20));
    }

    @Test
    void findByPowerStationAndStationDate_WhenNotExists_ShouldReturnEmpty() {
        // Act
        Optional<PowerStationDate> found = repository.findByPowerStationAndStationDate(
                powerStation,
                LocalDate.of(2024, 3, 21) // Más dátum
        );

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    void findFirstByPowerStationDateOrderByVersionDesc_ShouldReturnLatestVersion() {
        // Arrange
        TimeSeriesVersion version2 = new TimeSeriesVersion();
        version2.setPowerStationDate(powerStationDate);
        version2.setVersion(2);
        version2.setTimestamp(LocalDateTime.now().plusHours(1));
        version2.setPeriod(Duration.ofMinutes(15));
        version2.setSeries(List.of(5, 6, 7, 8));
        entityManager.persist(version2);
        powerStationDate.getVersions().add(version2);
        entityManager.flush();

        // Act
        Optional<TimeSeriesVersion> found = repository
                .findFirstByPowerStationDateOrderByVersionDesc(powerStationDate);

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getVersion()).isEqualTo(2);
    }

    @Test
    void findDatesByPowerStation_ShouldReturnAllDatesForStation() {
        // Arrange
        PowerStationDate secondDate = new PowerStationDate();
        secondDate.setPowerStation(powerStation);
        secondDate.setStationDate(LocalDate.of(2024, 3, 21));
        secondDate.setZone("Europe/Budapest");
        secondDate.setVersions(new ArrayList<>());
        entityManager.persist(secondDate);
        entityManager.flush();

        // Act
        List<LocalDate> dates = repository.findDatesByPowerStation(powerStation.getPowerStation());

        // Assert
        assertThat(dates)
                .hasSize(2)
                .contains(LocalDate.of(2024, 3, 20), LocalDate.of(2024, 3, 21));
    }

    @Test
    void findByPowerStationNameAndDate_ShouldReturnPowerStationDateWithVersions() {
        // Act
        Optional<PowerStationDate> found = repository.findByPowerStationNameAndDate(
                "Test Station",
                LocalDate.of(2024, 3, 20)
        );

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getVersions()).hasSize(1);
        assertThat(found.get().getVersions().get(0).getVersion()).isEqualTo(1);
    }
}