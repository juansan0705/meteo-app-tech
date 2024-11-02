package org.meteoapp.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import java.time.*;


@Getter
@Setter
@AllArgsConstructor
public class TemperatureData {
    @Id
    private String id;
    private double latitude;
    private double longitude;
    private double temperature;
    private LocalDateTime timestamp;

}


