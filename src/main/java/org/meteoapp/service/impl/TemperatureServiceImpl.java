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
        String url = String.format("https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s&current_weather=true", latitude, longitude);
        try {
            TemperatureResponse response = restTemplate.getForObject(url, TemperatureResponse.class);
            if (response != null && response.getCurrentWeather() != null) {
                TemperatureData data = new TemperatureData();
                data.setLatitude(latitude);
                data.setLongitude(longitude);
                data.setTemperature(response.getCurrentWeather().getTemperature());
                data.setTimestamp(LocalDateTime.now(clock));

                repository.findByLatitudeAndLongitude(latitude, longitude).ifPresent(existingData -> {
                    data.setId(existingData.getId());
                    repository.save(data);
                });

                if (!repository.findByLatitudeAndLongitude(latitude, longitude).isPresent()) {
                    repository.save(data);
                }

                return Optional.of(data);
            }
        } catch (Exception e) {
            logger.severe("Error fetching data from API: " + e.getMessage());
        }
        return Optional.empty();
    }


    private TemperatureResponse mapToResponse(TemperatureData data) {
        TemperatureResponse response = new TemperatureResponse();
        response.setLatitude(data.getLatitude());
        response.setLongitude(data.getLongitude());

        TemperatureResponse.CurrentWeather currentWeather = new TemperatureResponse.CurrentWeather();
        currentWeather.setTemperature(data.getTemperature());
        response.setCurrentWeather(currentWeather);

        return response;
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

    public boolean isDataStale(TemperatureData data) {
        return ChronoUnit.MINUTES.between(data.getTimestamp(), LocalDateTime.now(clock)) > 1;
    }

    public void sendToKafka(double latitude, double longitude, double temperature) {
        String message = String.format(java.util.Locale.US, "Lat: %.4f, Lon: %.4f, Temp: %.2f", latitude, longitude, temperature);
        try {
            kafkaTemplate.send("my-Topic", message);
        } catch (Exception e) {
            logger.severe("Error sending message to Kafka: " + e.getMessage());
        }
    }

}
