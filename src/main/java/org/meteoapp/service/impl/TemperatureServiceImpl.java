package org.meteoapp.service.impl;

import org.meteoapp.model.TemperatureData;
import org.meteoapp.model.response.TemperatureResponse;
import org.meteoapp.repository.TemperatureRepository;
import org.meteoapp.service.TemperatureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.logging.Logger;

@Service
public class TemperatureServiceImpl implements TemperatureService {

    private static final Logger logger = Logger.getLogger(TemperatureServiceImpl.class.getName());

    private final TemperatureRepository repository;
    private final RestTemplate restTemplate;
    private final Clock clock;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public TemperatureServiceImpl(TemperatureRepository repository, Clock clock, RestTemplate restTemplate, KafkaTemplate<String, String> kafkaTemplate) {
        this.repository = repository;
        this.clock = clock;
        this.restTemplate = restTemplate;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public Optional<TemperatureResponse> getTemperature(double latitude, double longitude) {
        validateCoordinates(latitude, longitude);

        Optional<TemperatureData> optionalData = repository.findByLatitudeAndLongitude(latitude, longitude);

        if (optionalData.isPresent() && !isDataStale(optionalData.get())) {
            sendToKafka(latitude, longitude, optionalData.get().getTemperature());
            return optionalData.map(this::mapToResponse);
        }

        Optional<TemperatureData> freshData = fetchAndSaveTemperatureData(latitude, longitude);
        freshData.ifPresent(data -> sendToKafka(latitude, longitude, data.getTemperature()));
        return freshData.map(this::mapToResponse);
    }

    @Override
    public Optional<TemperatureData> fetchAndSaveTemperatureData(double latitude, double longitude) {
        String url = String.format("https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current_weather=true", latitude, longitude);
        try {
            TemperatureData response = restTemplate.getForObject(url, TemperatureData.class);
            if (response != null) {
                response.setLatitude(latitude);
                response.setLongitude(longitude);
                response.setTimestamp(LocalDateTime.now(clock));

                repository.findByLatitudeAndLongitude(latitude, longitude).ifPresent(existingData -> response.setId(existingData.getId()));

                repository.save(response);
            }
            return Optional.ofNullable(response);
        } catch (Exception e) {
            logger.severe("Error fetching data from API: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void deleteTemperature(double latitude, double longitude) {
        validateCoordinates(latitude, longitude);
        repository.deleteByLatitudeAndLongitude(latitude, longitude);
    }

    @Override
    public void validateCoordinates(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Latitude must be in range of -90 to 90° and longitude from -180 to 180°.");
        }
    }

    private TemperatureResponse mapToResponse(TemperatureData data) {
        TemperatureResponse response = new TemperatureResponse();
        TemperatureResponse.CurrentWeather currentWeather = new TemperatureResponse.CurrentWeather();
        currentWeather.setTemperature(data.getTemperature());
        response.setCurrentWeather(currentWeather);
        return response;
    }

    private boolean isDataStale(TemperatureData data) {
        return ChronoUnit.MINUTES.between(data.getTimestamp(), LocalDateTime.now(clock)) > 1;
    }

    private void sendToKafka(double latitude, double longitude, double temperature) {
        String message = String.format("Lat: %.4f, Lon: %.4f, Temp: %.2f", latitude, longitude, temperature);
        kafkaTemplate.send("my-Topic", message);
    }
}
