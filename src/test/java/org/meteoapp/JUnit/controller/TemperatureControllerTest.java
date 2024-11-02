package org.meteoapp.JUnit.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.meteoapp.controller.TemperatureController;
import org.meteoapp.kafka.producer.KafkaProducer;
import org.meteoapp.model.response.TemperatureResponse;
import org.meteoapp.service.TemperatureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TemperatureController.class)
class TemperatureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TemperatureService temperatureService;

    @MockBean
    private KafkaProducer kafkaProducer;

    private static final double LATITUDE = 40.7128;
    private static final double LONGITUDE = -74.0060;

    private TemperatureResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = new TemperatureResponse();
        TemperatureResponse.CurrentWeather weather = new TemperatureResponse.CurrentWeather();
        weather.setTemperature(25.0);
        sampleResponse.setCurrentWeather(weather);
    }

    @Test
    void givenValidCoordinatesWhenGetTemperatureThenReturnsOk() throws Exception {
        when(temperatureService.getTemperature(LATITUDE, LONGITUDE)).thenReturn(Optional.of(sampleResponse));

        mockMvc.perform(get("/temperature")
                        .param("latitude", String.valueOf(LATITUDE))
                        .param("longitude", String.valueOf(LONGITUDE)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.current_weather.temperature").value(25.0));

        verify(temperatureService, times(1)).getTemperature(LATITUDE, LONGITUDE);
        verify(kafkaProducer, times(1)).sendMessage(anyString());
    }

    @Test
    void givenNonExistentCoordinatesWhenGetTemperatureThenReturnsNotFound() throws Exception {
        when(temperatureService.getTemperature(LATITUDE, LONGITUDE)).thenReturn(Optional.empty());

        mockMvc.perform(get("/temperature")
                        .param("latitude", String.valueOf(LATITUDE))
                        .param("longitude", String.valueOf(LONGITUDE)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Temperature data not found for the given coordinates."));

        verify(temperatureService, times(1)).getTemperature(LATITUDE, LONGITUDE);
        verifyNoInteractions(kafkaProducer);
    }

    @Test
    void givenInvalidCoordinatesWhenGetTemperatureThenReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/temperature")
                        .param("latitude", "1000")
                        .param("longitude", "-200"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(temperatureService, kafkaProducer);
    }

    @Test
    void givenValidCoordinatesWhenDeleteTemperatureThenReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/temperature")
                        .param("latitude", String.valueOf(LATITUDE))
                        .param("longitude", String.valueOf(LONGITUDE)))
                .andExpect(status().isNoContent());

        verify(temperatureService, times(1)).deleteTemperature(LATITUDE, LONGITUDE);
    }

    @Test
    void givenInvalidCoordinatesWhenDeleteTemperatureThenReturnsBadRequest() throws Exception {
        mockMvc.perform(delete("/temperature")
                        .param("latitude", "1000")
                        .param("longitude", "-200"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(temperatureService);
    }
}
