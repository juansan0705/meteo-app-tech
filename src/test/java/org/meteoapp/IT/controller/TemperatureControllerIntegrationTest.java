package org.meteoapp.IT.controller;

import org.junit.jupiter.api.Test;
import org.meteoapp.model.response.TemperatureResponse;
import org.meteoapp.service.impl.TemperatureServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class TemperatureControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TemperatureServiceImpl temperatureService;

    private static final String VALID_LATITUDE = "40.7128";
    private static final String VALID_LONGITUDE = "-74.0060";
    private static final double VALID_LATITUDE_DOUBLE = 40.7128;
    private static final double VALID_LONGITUDE_DOUBLE = -74.0060;

    private static final String NON_EXISTENT_LATITUDE = "35.6895";
    private static final String NON_EXISTENT_LONGITUDE = "139.6917";

    private static final String INVALID_LATITUDE = "1000.0";
    private static final String INVALID_LONGITUDE = "-200.0";

    @Test
    void givenValidCoordinatesWhenGetTemperatureThenReturnsOk() throws Exception {
        TemperatureResponse response = new TemperatureResponse();
        TemperatureResponse.CurrentWeather weather = new TemperatureResponse.CurrentWeather();
        weather.setTemperature(25.0);
        response.setCurrentWeather(weather);

        when(temperatureService.getTemperature(VALID_LATITUDE_DOUBLE, VALID_LONGITUDE_DOUBLE)).thenReturn(Optional.of(response));

        mockMvc.perform(get("/temperature")
                        .param("latitude", VALID_LATITUDE)
                        .param("longitude", VALID_LONGITUDE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current_weather.temperature").value(25.0));
    }

    @Test
    void givenNonExistentCoordinatesWhenGetTemperatureThenReturnsNotFound() throws Exception {
        when(temperatureService.getTemperature(Double.parseDouble(NON_EXISTENT_LATITUDE), Double.parseDouble(NON_EXISTENT_LONGITUDE)))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/temperature")
                        .param("latitude", NON_EXISTENT_LATITUDE)
                        .param("longitude", NON_EXISTENT_LONGITUDE))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenInvalidCoordinatesWhenGetTemperatureThenReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/temperature")
                        .param("latitude", INVALID_LATITUDE)
                        .param("longitude", INVALID_LONGITUDE))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenValidCoordinatesWhenDeleteTemperatureThenReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/temperature")
                        .param("latitude", VALID_LATITUDE)
                        .param("longitude", VALID_LONGITUDE))
                .andExpect(status().isNoContent());
    }

    @Test
    void givenInvalidCoordinatesWhenDeleteTemperatureThenReturnsBadRequest() throws Exception {
        mockMvc.perform(delete("/temperature")
                        .param("latitude", INVALID_LATITUDE)
                        .param("longitude", INVALID_LONGITUDE))
                .andExpect(status().isBadRequest());
    }
}
