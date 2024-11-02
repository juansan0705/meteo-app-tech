package org.meteoapp.service;

import org.meteoapp.model.TemperatureData;
import org.meteoapp.repository.TemperatureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.logging.Logger;

@Service
public class TemperatureService {

    private static final Logger logger = Logger.getLogger(TemperatureService.class.getName());

    private final TemperatureRepository repository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final Clock clock;

    @Autowired
    public TemperatureService(TemperatureRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Optional<TemperatureData> getTemperature(double latitude, double longitude) {
        validateCoordinates(latitude, longitude);

        Optional<TemperatureData> optionalData = repository.findByLatitudeAndLongitude(latitude, longitude);

        if (optionalData.isEmpty() || isDataStale(optionalData.get())) {
            return fetchAndSaveTemperatureData(latitude, longitude);
        }

        return optionalData;
    }

    protected Optional<TemperatureData> fetchAndSaveTemperatureData(double latitude, double longitude) {
        String url = String.format("https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&current_weather=true", latitude, longitude);
        try {
            TemperatureData response = restTemplate.getForObject(url, TemperatureData.class);
            if (response != null) {
                response.setLatitude(latitude);
                response.setLongitude(longitude);
                response.setTimestamp(LocalDateTime.now(clock));
                repository.save(response);
            }
            return Optional.ofNullable(response);
        } catch (Exception e) {
            logger.severe("Error fetching data from API: " + e.getMessage());
            return Optional.empty();
        }
    }

    public void deleteTemperature(double latitude, double longitude) {
        validateCoordinates(latitude, longitude);
        repository.deleteByLatitudeAndLongitude(latitude, longitude);
    }

    protected boolean isDataStale(TemperatureData data) {
        return ChronoUnit.MINUTES.between(data.getTimestamp(), LocalDateTime.now(clock)) > 1;
    }

    protected void validateCoordinates(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Latitude must be in range of -90 to 90° and longitude from -180 to 180°.");
        }
    }
}
