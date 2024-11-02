package org.meteoapp.JUnit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.meteoapp.model.response.TemperatureResponse;
import org.meteoapp.service.impl.TemperatureServiceImpl;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.meteoapp.model.TemperatureData;
import org.meteoapp.repository.TemperatureRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TemperatureServiceTest {

    @Mock
    private TemperatureRepository repository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private TemperatureServiceImpl temperatureService;

    private Clock clock;

    private static final double LATITUDE = 40.7128;
    private static final double LONGITUDE = -74.0060;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        clock = Clock.fixed(Instant.now(), ZoneOffset.UTC);

        temperatureService = new TemperatureServiceImpl(repository, clock, restTemplate, kafkaTemplate);
    }

    @Test
    void givenDataInRepositoryNotStaleWhenGetTemperatureThenReturnsData() {
        TemperatureData data = new TemperatureData();
        data.setLatitude(LATITUDE);
        data.setLongitude(LONGITUDE);
        data.setTemperature(25.0);
        data.setTimestamp(LocalDateTime.now(clock));

        when(repository.findByLatitudeAndLongitude(LATITUDE, LONGITUDE)).thenReturn(Optional.of(data));

        Optional<TemperatureResponse> result = temperatureService.getTemperature(LATITUDE, LONGITUDE);

        assertTrue(result.isPresent());
        assertEquals(25.0, result.get().getCurrentWeather().getTemperature());
        verify(repository, times(1)).findByLatitudeAndLongitude(LATITUDE, LONGITUDE);
        verifyNoMoreInteractions(repository, restTemplate);
    }

    @Test
    void givenDataInRepositoryStaleWhenGetTemperatureThenFetchesAndSavesNewData() {
        TemperatureData staleData = new TemperatureData();
        staleData.setLatitude(LATITUDE);
        staleData.setLongitude(LONGITUDE);
        staleData.setTemperature(20.0);
        staleData.setTimestamp(LocalDateTime.now(clock).minusMinutes(10));

        TemperatureData freshData = new TemperatureData();
        freshData.setLatitude(LATITUDE);
        freshData.setLongitude(LONGITUDE);
        freshData.setTemperature(25.0);
        freshData.setTimestamp(LocalDateTime.now(clock));

        when(repository.findByLatitudeAndLongitude(LATITUDE, LONGITUDE))
                .thenReturn(Optional.of(staleData))
                .thenReturn(Optional.of(freshData));

        when(restTemplate.getForObject(anyString(), eq(TemperatureData.class)))
                .thenReturn(freshData);

        Optional<TemperatureResponse> result = temperatureService.getTemperature(LATITUDE, LONGITUDE);

        assertTrue(result.isPresent());
        assertEquals(25.0, result.get().getCurrentWeather().getTemperature());

        verify(repository, times(1)).save(freshData);
        verify(repository, atLeastOnce()).findByLatitudeAndLongitude(LATITUDE, LONGITUDE);
    }

    @Test
    void givenApiCallSuccessfulWhenFetchAndSaveTemperatureDataThenSavesData() {
        TemperatureData apiData = new TemperatureData();
        apiData.setLatitude(LATITUDE);
        apiData.setLongitude(LONGITUDE);
        apiData.setTemperature(30.0);
        apiData.setTimestamp(LocalDateTime.now(clock));

        String expectedUrl = String.format("https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current_weather=true", LATITUDE, LONGITUDE);

        when(restTemplate.getForObject(eq(expectedUrl), eq(TemperatureData.class))).thenReturn(apiData);

        Optional<TemperatureData> result = temperatureService.fetchAndSaveTemperatureData(LATITUDE, LONGITUDE);

        assertTrue(result.isPresent());
        assertEquals(30.0, result.get().getTemperature());
        verify(repository, times(1)).save(apiData);
    }

    @Test
    void givenApiCallFailsWhenFetchAndSaveTemperatureDataThenReturnsEmptyOptional() {
        when(restTemplate.getForObject(anyString(), eq(TemperatureData.class))).thenThrow(new RuntimeException("API error"));

        Optional<TemperatureData> result = temperatureService.fetchAndSaveTemperatureData(LATITUDE, LONGITUDE);

        assertFalse(result.isPresent());
        verify(repository, never()).save(any(TemperatureData.class));
    }

    @Test
    void givenValidCoordinatesWhenDeleteTemperatureThenCallsDeleteOnRepository() {
        temperatureService.deleteTemperature(LATITUDE, LONGITUDE);
        verify(repository, times(1)).deleteByLatitudeAndLongitude(LATITUDE, LONGITUDE);
    }

    @Test
    void givenValidCoordinatesWhenValidateCoordinatesThenNoExceptionThrown() {
        assertDoesNotThrow(() -> temperatureService.validateCoordinates(45.0, 90.0));
    }

    @Test
    void givenInvalidCoordinatesWhenValidateCoordinatesThenThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> temperatureService.validateCoordinates(100.0, 190.0));
        assertEquals("Latitude must be in range of -90 to 90° and longitude from -180 to 180°.", exception.getMessage());
    }
}
