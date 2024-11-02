package org.meteoapp.IT.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.meteoapp.model.TemperatureData;
import org.meteoapp.model.response.TemperatureResponse;
import org.meteoapp.repository.TemperatureRepository;
import org.meteoapp.service.impl.TemperatureServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
public class TemperatureServiceIntegrationTest {

    @Autowired
    private TemperatureServiceImpl temperatureService;

    @Autowired
    private TemperatureRepository repository;

    @MockBean
    private RestTemplate restTemplate;

    private static final double VALID_LATITUDE = 40.7128;
    private static final double VALID_LONGITUDE = -74.0060;
    private static final double TEMPERATURE = 25.0;
    @Autowired
    private Clock clock;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void givenValidCoordinatesWhenGetTemperatureThenReturnsTemperature() {
        TemperatureData temperatureData = new TemperatureData();
        temperatureData.setLatitude(VALID_LATITUDE);
        temperatureData.setLongitude(VALID_LONGITUDE);
        temperatureData.setTemperature(TEMPERATURE);
        temperatureData.setTimestamp(LocalDateTime.now(clock));
        repository.save(temperatureData);

        TemperatureResponse responseFromApi = new TemperatureResponse();
        responseFromApi.setLatitude(VALID_LATITUDE);
        responseFromApi.setLongitude(VALID_LONGITUDE);
        TemperatureResponse.CurrentWeather currentWeather = new TemperatureResponse.CurrentWeather();
        currentWeather.setTemperature(TEMPERATURE);
        responseFromApi.setCurrentWeather(currentWeather);

        when(restTemplate.getForObject(anyString(), eq(TemperatureResponse.class))).thenReturn(responseFromApi);

        Optional<TemperatureResponse> response = temperatureService.getTemperature(VALID_LATITUDE, VALID_LONGITUDE);

        assertTrue(response.isPresent());
        assertEquals(TEMPERATURE, response.get().getCurrentWeather().getTemperature());

        Optional<TemperatureData> savedData = repository.findByLatitudeAndLongitude(VALID_LATITUDE, VALID_LONGITUDE);
        assertTrue(savedData.isPresent());
        assertEquals(TEMPERATURE, savedData.get().getTemperature());

        assertEquals(1, repository.findAll().size());
    }


    @Test
    void givenStaleDataInDatabaseWhenGetTemperatureThenFetchesAndUpdatesData() {
        LocalDateTime staleTimestamp = LocalDateTime.now(clock).minusMinutes(2);

        TemperatureData staleData = new TemperatureData();
        staleData.setLatitude(VALID_LATITUDE);
        staleData.setLongitude(VALID_LONGITUDE);
        staleData.setTemperature(20.0);
        staleData.setTimestamp(staleTimestamp);
        repository.save(staleData);

        TemperatureResponse freshResponse = new TemperatureResponse(VALID_LATITUDE, VALID_LONGITUDE, TEMPERATURE);
        when(restTemplate.getForObject(anyString(), eq(TemperatureResponse.class))).thenReturn(freshResponse);

        Optional<TemperatureResponse> response = temperatureService.getTemperature(VALID_LATITUDE, VALID_LONGITUDE);

        assertTrue(response.isPresent());
        assertEquals(TEMPERATURE, response.get().getCurrentWeather().getTemperature());

        Optional<TemperatureData> updatedData = repository.findByLatitudeAndLongitude(VALID_LATITUDE, VALID_LONGITUDE);
        assertTrue(updatedData.isPresent());
        assertEquals(TEMPERATURE, updatedData.get().getTemperature());

        assertEquals(1, repository.findAll().size());
    }


    @Test
    void givenValidCoordinatesWhenDeleteTemperatureThenRemovesData() {
        TemperatureData data = new TemperatureData();
        data.setLatitude(VALID_LATITUDE);
        data.setLongitude(VALID_LONGITUDE);
        data.setTemperature(TEMPERATURE);
        data.setTimestamp(LocalDateTime.now(Clock.fixed(Instant.now(), ZoneOffset.UTC)));
        repository.save(data);

        assertTrue(repository.findByLatitudeAndLongitude(VALID_LATITUDE, VALID_LONGITUDE).isPresent());

        temperatureService.deleteTemperature(VALID_LATITUDE, VALID_LONGITUDE);
        assertFalse(repository.findByLatitudeAndLongitude(VALID_LATITUDE, VALID_LONGITUDE).isPresent());
    }

    @Test
    void givenInvalidCoordinatesWhenGetTemperatureThenThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> temperatureService.getTemperature(1000.0, -200.0));
    }
}
