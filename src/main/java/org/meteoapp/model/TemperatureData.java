package org.meteoapp.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.time.LocalDateTime;

@Data
@Document(collection = "temperature_data")
public class TemperatureData {

    @Id
    private String id;

    @Min(-90) @Max(90)
    private double latitude;

    @Min(-180) @Max(180)
    private double longitude;

    private double temperature;

    private LocalDateTime timestamp;

    public TemperatureData() {
        this.timestamp = LocalDateTime.now();
    }
}
