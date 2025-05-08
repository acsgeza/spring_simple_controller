
package com.reg.time_series.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.reg.time_series.model.TimeSeriesData;
import com.reg.time_series.model.TimeSeriesViewDTO;
import com.reg.time_series.service.TimeSeriesService;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.ByteArrayOutputStream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TimeSeriesControllerTest {

    @Mock
    private TimeSeriesService timeSeriesService;

    @InjectMocks
    private TimeSeriesController timeSeriesController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(timeSeriesController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    }


    @Test
    void receiveTimeSeries_ValidData_ReturnsCreated() throws Exception {
        // Arrange
        TimeSeriesData data = createSampleTimeSeriesData();
        String jsonContent = objectMapper.writeValueAsString(data);

        // Act & Assert
        mockMvc.perform(post("/api/time-series")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isCreated());

        verify(timeSeriesService).save(any(TimeSeriesData.class));
    }

    @Test
    void receiveTimeSeriesFromFile_ValidFile_ReturnsCreated() throws Exception {
        // Arrange
        TimeSeriesData data = createSampleTimeSeriesData();
        String jsonContent = objectMapper.writeValueAsString(data);
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.json",
                MediaType.APPLICATION_JSON_VALUE,
                jsonContent.getBytes()
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/time-series/file")
                        .file(file))
                .andExpect(status().isCreated());

        verify(timeSeriesService).save(any(TimeSeriesData.class));
    }

    @Test
    void receiveTimeSeriesFolder_ValidZipFile_ReturnsCreated() throws Exception {
        // Arrange
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry("test.json");
            zos.putNextEntry(entry);
            zos.write(objectMapper.writeValueAsString(createSampleTimeSeriesData()).getBytes());
            zos.closeEntry();
        }

        MockMultipartFile zipFile = new MockMultipartFile(
                "file", "test.zip",
                "application/zip",
                baos.toByteArray()
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/time-series/folder")
                        .file(zipFile))
                .andExpect(status().isCreated());

        verify(timeSeriesService).save(any(TimeSeriesData.class));
    }

    @Test
    void getPowerStations_ReturnsListOfStations() throws Exception {
        // Arrange
        List<String> stations = Arrays.asList("Station1", "Station2");
        when(timeSeriesService.getAllPowerStations()).thenReturn(stations);

        // Act & Assert
        mockMvc.perform(get("/api/time-series/power-stations"))
                .andExpect(status().isOk())
                .andExpect(content().json("[\"Station1\",\"Station2\"]"));
    }

    @Test
    void getAvailableDates_ReturnsFormattedDates() throws Exception {
        // Arrange
        String stationName = "TestStation";
        List<LocalDate> dates = Arrays.asList(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 2)
        );
        when(timeSeriesService.getAvailableDates(stationName)).thenReturn(dates);

        // Act & Assert
        mockMvc.perform(get("/api/time-series/power-stations/{powerStationName}/dates", stationName))
                .andExpect(status().isOk())
                .andExpect(content().json("[\"2024-01-01\",\"2024-01-02\"]"));
    }

    @Test
    void getTimeSeriesView_ReturnsTimeSeriesViewDTO() throws Exception {
        // Arrange
        String stationName = "TestStation";
        LocalDate date = LocalDate.of(2024, 1, 1);
        TimeSeriesViewDTO dto = new TimeSeriesViewDTO();
        when(timeSeriesService.getTimeSeriesView(stationName, date)).thenReturn(dto);

        // Act & Assert
        mockMvc.perform(get("/api/time-series/power-stations/{powerStationName}/dates/{date}",
                        stationName, "2024-01-01"))
                .andExpect(status().isOk());
    }

    private TimeSeriesData createSampleTimeSeriesData() {
        TimeSeriesData data = new TimeSeriesData();
        data.setPowerStation("TestStation");
        data.setDate(LocalDate.now());
        data.setZone("Europe/Budapest");
        data.setTimestamp(LocalDateTime.now());
        data.setPeriod("PT15M");
        data.setSeries(Arrays.asList(1, 2, 3, 4));
        return data;
    }
}
