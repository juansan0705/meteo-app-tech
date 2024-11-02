package org.meteoapp.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TemperatureResponse {

    private double latitude;
    private double longitude;

    @JsonProperty("current_weather")
    private CurrentWeather currentWeather;

    @Data
    public static class CurrentWeather {
        private double temperature;

        public CurrentWeather(double temperature) {
            this.temperature = temperature;
        }

        public CurrentWeather() {}
    }

    public TemperatureResponse(double latitude, double longitude, double temperature) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.currentWeather = new CurrentWeather(temperature);
    }

    public TemperatureResponse() {}
}
