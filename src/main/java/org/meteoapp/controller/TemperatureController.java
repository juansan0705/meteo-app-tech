package org.meteoapp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.meteoapp.kafka.producer.KafkaProducer;
import org.meteoapp.model.response.TemperatureResponse;
import org.meteoapp.service.TemperatureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/temperature")
public class TemperatureController {

    private final TemperatureService temperatureService;
    private final KafkaProducer kafkaProducer;

    @Autowired
    public TemperatureController(TemperatureService temperatureService, KafkaProducer kafkaProducer) {
        this.temperatureService = temperatureService;
        this.kafkaProducer = kafkaProducer;
    }

    @Operation(summary = "Get temperature by coordinates", description = "Fetches temperature data for a given latitude and longitude.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Temperature data retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Temperature data not found"),
            @ApiResponse(responseCode = "400", description = "Invalid latitude or longitude values"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<?> getTemperature(
            @Parameter(description = "Latitude of the location", required = true) @RequestParam double latitude,
            @Parameter(description = "Longitude of the location", required = true) @RequestParam double longitude) {
        validateCoordinates(latitude, longitude);
        Optional<TemperatureResponse> optionalData = temperatureService.getTemperature(latitude, longitude);

        if (optionalData.isPresent()) {
            TemperatureResponse data = optionalData.get();
            kafkaProducer.sendMessage(String.format("Lat: %s, Lon: %s, Temp: %s", latitude, longitude, data.getCurrentWeather().getTemperature()));
            return ResponseEntity.ok(data);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Temperature data not found for the given coordinates.");
        }
    }

    @Operation(summary = "Delete temperature data by coordinates", description = "Deletes cached temperature data for a given latitude and longitude.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Temperature data deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid latitude or longitude values"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping
    public ResponseEntity<Void> deleteTemperature(
            @Parameter(description = "Latitude of the location", required = true) @RequestParam double latitude,
            @Parameter(description = "Longitude of the location", required = true) @RequestParam double longitude) {
        validateCoordinates(latitude, longitude);
        temperatureService.deleteTemperature(latitude, longitude);
        return ResponseEntity.noContent().build();
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Invalid latitude or longitude values.");
        }
    }
}
