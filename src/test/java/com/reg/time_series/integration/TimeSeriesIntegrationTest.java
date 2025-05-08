package com.reg.time_series.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.reg.time_series.model.TimeSeriesData;
import com.reg.time_series.repositories.PowerStationRepository;
import com.reg.time_series.repositories.TimeSeriesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TimeSeriesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TimeSeriesRepository timeSeriesRepository;

    @Autowired
    private PowerStationRepository powerStationRepository;

    private TimeSeriesData createSampleTimeSeriesData() {
        TimeSeriesData data = new TimeSeriesData();
        data.setPowerStation("Test Station");
        data.setDate(LocalDate.now());
        data.setZone("Europe/Budapest");
        data.setTimestamp(LocalDateTime.now());
        data.setPeriod(String.valueOf(Duration.ofMinutes(15)));
        data.setSeries(Arrays.asList(100, 200, 300, 400));
        return data;
    }

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        powerStationRepository.deleteAll();
        timeSeriesRepository.deleteAll();
    }

    @Test
    void fullIntegrationTest() throws Exception {
        // 1. Upload single time series
        TimeSeriesData testData = createSampleTimeSeriesData();
        mockMvc.perform(post("/api/time-series")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testData)))
                .andExpect(status().isCreated());

        // 2. Upload ZIP file
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry("test.json");
            zos.putNextEntry(entry);
            zos.write(objectMapper.writeValueAsString(testData).getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        MockMultipartFile zipFile = new MockMultipartFile(
                "file",
                "test.zip",
                "application/zip",
                baos.toByteArray()
        );

        mockMvc.perform(multipart("/api/time-series/folder")
                        .file(zipFile))
                .andExpect(status().isCreated());

        // 3. Query and verify data
        mockMvc.perform(get("/api/time-series/power-stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Test Station"));

        mockMvc.perform(get("/api/time-series/power-stations/Test Station/dates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(LocalDate.now().toString()));

        mockMvc.perform(get("/api/time-series/power-stations/Test Station/dates/" + LocalDate.now()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.powerStationName").value("Test Station"));

        // 5. Verify database
        assertThat(powerStationRepository.findByPowerStation("Test Station")).isPresent();
        assertThat(timeSeriesRepository.findDatesByPowerStation("Test Station"))
                .contains(LocalDate.now());
    }
    @SuppressWarnings("JsonStandardCompliance")
    @Test
    void errorHandlingTest() throws Exception {
        // 1. Hibás JSON formátum
        mockMvc.perform(post("/api/time-series")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid: json}"))
                .andExpect(status().isBadRequest());

        // 2. Hiányzó kötelező mezők
        TimeSeriesData invalidData = new TimeSeriesData();
        mockMvc.perform(post("/api/time-series")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidData)))
                .andExpect(status().isBadRequest());

        // 3. Nem létező erőmű lekérdezése
        mockMvc.perform(get("/api/time-series")
                        .param("powerStation", "NonexistentStation")
                        .param("date", LocalDate.now().toString()))
                .andExpect(status().isNotFound());

        // 4. Hibás ZIP fájl feltöltése
        MockMultipartFile invalidZipFile = new MockMultipartFile(
                "file",
                "test.zip",
                "application/zip",
                "invalid zip content".getBytes()
        );

        mockMvc.perform(multipart("/api/time-series/folder")
                        .file(invalidZipFile))
                .andExpect(status().isBadRequest());

    }
}