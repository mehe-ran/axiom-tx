package com.axiomtx.settlement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class DeadLetterPublisher {

    public static final String TOPIC = "tx_dead_letter";

    private static final Logger log = LoggerFactory.getLogger(DeadLetterPublisher.class);

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;

    public DeadLetterPublisher(KafkaTemplate<String, String> kafka, ObjectMapper mapper) {
        this.kafka = kafka;
        this.mapper = mapper;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record DeadLetter(String originalData, String error) {
    }

    public void publish(String originalData, Exception error) {
        try {
            String json = mapper.writeValueAsString(new DeadLetter(originalData, String.valueOf(error)));
            kafka.send(TOPIC, json).get(10, TimeUnit.SECONDS);
            log.warn("routed failed transaction to dlq");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("dlq routing interrupted", e);
        } catch (Exception e) {
            log.error("dlq routing failed", e);
        }
    }
}
