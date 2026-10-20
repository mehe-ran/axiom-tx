package com.axiomtx.gateway;

import com.axiomtx.shared.TransactionEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class TransactionPublisher {

    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;

    public TransactionPublisher(KafkaTemplate<String, String> kafka, ObjectMapper mapper) {
        this.kafka = kafka;
        this.mapper = mapper;
    }

    // blocks until the broker acks, so a 200 means the event is on tx_pending
    public void publish(TransactionEvent event) {
        try {
            String json = mapper.writeValueAsString(event);
            kafka.send(TransactionEvent.TOPIC, event.transactionId(), json)
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RoutingException(e);
        } catch (JsonProcessingException | ExecutionException | TimeoutException e) {
            throw new RoutingException(e);
        }
    }

    public static class RoutingException extends RuntimeException {
        RoutingException(Throwable cause) {
            super(cause);
        }
    }
}
