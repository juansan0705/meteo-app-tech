package org.meteoapp.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic temperatureUpdate() {
        return new NewTopic("temperature-update", 1, (short) 1);
    }

}
