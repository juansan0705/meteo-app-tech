package org.meteoapp.controller;

import org.meteoapp.kafka.producer.KafkaProducer;
import org.meteoapp.model.TemperatureData;
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

    @GetMapping
    public ResponseEntity<?> getTemperature(@RequestParam double latitude, @RequestParam double longitude) {
        try {
            validateCoordinates(latitude, longitude);
            Optional<TemperatureData> optionalData = temperatureService.getTemperature(latitude, longitude);

            if (optionalData.isPresent()) {
                TemperatureData data = optionalData.get();
                kafkaProducer.sendMessage("temperature-update", String.format("Lat: %s, Lon: %s, Temp: %s", latitude, longitude, data.getTemperature()));
                return ResponseEntity.ok(data);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Temperature data not found for the given coordinates.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching temperature data: " + e.getMessage());
        }
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteTemperature(@RequestParam double latitude, @RequestParam double longitude) {
        try {
            validateCoordinates(latitude, longitude);
            temperatureService.deleteTemperature(latitude, longitude);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Invalid latitude or longitude values.");
        }
    }
}
