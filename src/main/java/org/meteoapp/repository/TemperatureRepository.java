package org.meteoapp.repository;

import org.meteoapp.model.TemperatureData;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface TemperatureRepository extends MongoRepository<TemperatureData, String> {
    Optional<TemperatureData> findByLatitudeAndLongitude(double latitude, double longitude);
    void deleteByLatitudeAndLongitude(double latitude, double longitude);
}
