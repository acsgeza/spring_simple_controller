package com.reg.time_series.repository;

import com.reg.time_series.entity.PowerStation;
import com.reg.time_series.repositories.PowerStationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
@DataJpaTest
class PowerStationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PowerStationRepository powerStationRepository;

    @Test
    void findByPowerStation_WhenExists_ShouldReturnPowerStation() {
        // Arrange
        PowerStation powerStation = new PowerStation();
        powerStation.setPowerStation("Test Station");
        entityManager.persist(powerStation);
        entityManager.flush();

        // Act
        Optional<PowerStation> found = powerStationRepository.findByPowerStation("Test Station");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getPowerStation()).isEqualTo("Test Station");
    }

    @Test
    void findByPowerStation_WhenNotExists_ShouldReturnEmpty() {
        // Act
        Optional<PowerStation> found = powerStationRepository.findByPowerStation("Nonexistent Station");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    void findDistinctPowerStationNames_ShouldReturnAllUniqueNames() {
        // Arrange
        PowerStation station1 = new PowerStation();
        station1.setPowerStation("Station 1");

        PowerStation station2 = new PowerStation();
        station2.setPowerStation("Station 2");

        PowerStation station3 = new PowerStation();
        station3.setPowerStation("Station 3");

        entityManager.persist(station1);
        entityManager.persist(station2);
        entityManager.persist(station3);
        entityManager.flush();

        // Act
        List<String> stationNames = powerStationRepository.findDistinctPowerStationNames();

        // Assert
        assertThat(stationNames)
                .hasSize(3)
                .containsExactlyInAnyOrder("Station 1", "Station 2", "Station 3");
    }

    @Test
    void findDistinctPowerStationNames_WhenEmpty_ShouldReturnEmptyList() {
        // Act
        List<String> stationNames = powerStationRepository.findDistinctPowerStationNames();

        // Assert
        assertThat(stationNames).isEmpty();
    }
}
