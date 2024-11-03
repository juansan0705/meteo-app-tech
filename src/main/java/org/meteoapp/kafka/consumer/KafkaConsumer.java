package org.meteoapp.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumer {
    @KafkaListener(topics = "temperature-update", groupId = "temperature-data-group")
    public void consumeMessage(String message) {
        System.out.println("Received message: " + message);
    }
}