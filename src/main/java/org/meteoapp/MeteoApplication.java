package org.meteoapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"org.meteoapp"})
public class MeteoApplication {
    public static void main(String[] args) {
        SpringApplication.run(MeteoApplication.class, args);
    }
}
