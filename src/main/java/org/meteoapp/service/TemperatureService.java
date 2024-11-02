package org.meteoapp.service;

import org.meteoapp.model.TemperatureData;
import org.meteoapp.model.response.TemperatureResponse;

import java.util.Optional;

public interface TemperatureService {

    Optional<TemperatureResponse> getTemperature(double latitude, double longitude);

    Optional<TemperatureData> fetchAndSaveTemperatureData(double latitude, double longitude);

    void deleteTemperature(double latitude, double longitude);

    void validateCoordinates(double latitude, double longitude);
}
