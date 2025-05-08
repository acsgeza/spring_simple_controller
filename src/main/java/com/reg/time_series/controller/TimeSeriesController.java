

package com.reg.time_series.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.reg.time_series.exceptions.TimeSeriesNotFoundException;
import com.reg.time_series.model.TimeSeriesData;
import com.reg.time_series.model.TimeSeriesViewDTO;
import com.reg.time_series.service.TimeSeriesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.log4j.Log4j2;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
        import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@RestController
@RequestMapping("/api/time-series")
@Log4j2
@Tag(name = "TimeSeries", description = "Endpoints for time-series data")
public class TimeSeriesController {
    private final TimeSeriesService timeSeriesService;
    private final ObjectMapper objectMapper;

    public TimeSeriesController(TimeSeriesService timeSeriesService) {
        this.timeSeriesService = timeSeriesService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    }

    @PostMapping
    @Operation(summary = "Receiving Time-series data",
              description = "Receiving and Saving timeseries data in JSON format")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = TimeSeriesData.class),
                    examples = @ExampleObject(
                            name = "timeSeriesExample",
                            summary = "Example for timeseries data",
                            value = """
            {
              "power-station": "Supercell on the Sun",
              "date": "2025-05-04",
              "zone": "Europe/Budapest",
              "timestamp": "2025-05-04 08:16:43",
              "period": "PT15M",
              "series": [
                0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
                4000,4000,8000,12000,16000,24000,40000,56000,88000,
                104000,76000,124000,140000,104000,160000,144000,324000,
                348000,360000,376000,308000,432000,444000,444000,452000
              ]
            }
            """
                    )
            )
    )
    public ResponseEntity<Void> receiveTimeSeries(@RequestBody @Validated TimeSeriesData timeSeriesData) {
        if (timeSeriesData.getTimestamp() == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
        timeSeriesService.save(timeSeriesData);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/file")
    public ResponseEntity<Void> receiveTimeSeriesFromFile(@RequestParam("file") MultipartFile file) {
        try {
            String jsonContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            TimeSeriesData timeSeriesData = objectMapper.readValue(jsonContent, TimeSeriesData.class);
            timeSeriesService.save(timeSeriesData);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IOException e) {
            log.error("Error processing JSON file: ", e);
            return ResponseEntity.badRequest().build();
        }
    }


    @PostMapping("/folder")
    public ResponseEntity<String> receiveTimeSeriesFolder(@RequestParam("file") MultipartFile zipFile) {
        if (!zipFile.getOriginalFilename().toLowerCase().endsWith(".zip")) {
            return ResponseEntity.badRequest().body("Only ZIP files are accepted!");
        }

        StringBuilder result = new StringBuilder();
        int successCount = 0;
        int failureCount = 0;

        try (ZipInputStream zipInputStream = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".json")) {
                    try {
                        // Reading JSON content
                        String jsonContent = new String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8);

                        // Converting JSON to TimeSeriesData object
                        TimeSeriesData timeSeriesData = objectMapper.readValue(jsonContent, TimeSeriesData.class);

                        // Saving data
                        timeSeriesService.save(timeSeriesData);

                        successCount++;
                        result.append(String.format("Successful processing: %s%n", entry.getName()));

                    } catch (Exception e) {
                        failureCount++;
                        result.append(String.format("Error during processing %s: %s%n",
                                entry.getName(), e.getMessage()));
                        log.error("Error processing file {}: ", entry.getName(), e);
                    }
                }
                zipInputStream.closeEntry();
            }

            result.append(String.format("%nSummary: %d files successfully processed, %d files failed",
                    successCount, failureCount));

            if (successCount > 0) {
                return ResponseEntity.status(HttpStatus.CREATED).build();
            } else {
                return ResponseEntity.badRequest().body(result.toString());
            }

        } catch (IOException e) {
            log.error("Error processing ZIP file: ", e);
            return ResponseEntity.badRequest().body("Error processing ZIP file: " + e.getMessage());
        }
    }

    // At development helps to upload local files
//    @PostMapping("/process-local-folder")
    public ResponseEntity<String> processLocalFolder(@RequestParam String folderPath) {
        File folder = new File(folderPath);
        if (!folder.isDirectory()) {
            return ResponseEntity.badRequest().body("The path is not a directory!");
        }

        File[] jsonFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        if (jsonFiles == null || jsonFiles.length == 0) {
            return ResponseEntity.badRequest().body("No JSON files found in the directory!");
        }

        StringBuilder result = new StringBuilder();
        int successCount = 0;
        int failureCount = 0;

        for (File jsonFile : jsonFiles) {
            try {
                String jsonContent = Files.readString(jsonFile.toPath());
                TimeSeriesData timeSeriesData = objectMapper.readValue(jsonContent, TimeSeriesData.class);
                timeSeriesService.save(timeSeriesData);

                successCount++;
                result.append(String.format("Successful processing: %s%n", jsonFile.getName()));
            } catch (Exception e) {
                failureCount++;
                result.append(String.format("Error during processing %s: %s%n",
                        jsonFile.getName(), e.getMessage()));
                log.error("Error processing file {}: ", jsonFile.getName(), e);
            }
        }

        result.append(String.format("%nSummary: %d files successfully processed, %d files failed",
                successCount, failureCount));

        return ResponseEntity.ok(result.toString());
    }


    @GetMapping("/power-stations")
    public ResponseEntity<List<String>> getPowerStations() {
        return ResponseEntity.ok(timeSeriesService.getAllPowerStations());
    }

    @GetMapping("/power-stations/{powerStationName}/dates")
    public ResponseEntity<List<String>> getAvailableDates(
            @PathVariable String powerStationName) {
        List<String> formattedDates = timeSeriesService.getAvailableDates(powerStationName)
                .stream()
                .map(date -> date.format(DateTimeFormatter.ISO_DATE))
                .collect(Collectors.toList());
        return ResponseEntity.ok(formattedDates);
    }


    @GetMapping("/power-stations/{powerStationName}/dates/{date}")
    public ResponseEntity<TimeSeriesViewDTO> getTimeSeriesView(
            @PathVariable String powerStationName,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        try {
            return ResponseEntity.ok(timeSeriesService.getTimeSeriesView(powerStationName, date));
        } catch (Exception e) {
            throw new TimeSeriesNotFoundException("Time series not found for station: " + powerStationName);
        }

    }


}