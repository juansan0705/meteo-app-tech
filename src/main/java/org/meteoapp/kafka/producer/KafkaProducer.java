package org.meteoapp.kafka.producer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.util.logging.Logger;

@Service
public class KafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final Logger logger = Logger.getLogger(KafkaProducer.class.getName());

    @Value("${kafka.topic}")
    private String topic;

    public KafkaProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String message) {
        kafkaTemplate.send(topic, message);
        logger.info("Message sent to Kafka: " + message);
    }
}

