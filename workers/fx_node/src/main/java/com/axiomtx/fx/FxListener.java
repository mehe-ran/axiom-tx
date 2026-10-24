package com.axiomtx.fx;

import com.axiomtx.shared.TransactionEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// stub: observes transactions, no conversion yet
@Component
public class FxListener {

    private static final Logger log = LoggerFactory.getLogger(FxListener.class);

    private final ObjectMapper mapper;

    public FxListener(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @KafkaListener(topics = TransactionEvent.TOPIC)
    public void onMessage(String raw) {
        try {
            TransactionEvent event = mapper.readValue(raw, TransactionEvent.class);
            log.info("processing transaction {}: {} {}", event.transactionId(), event.amount(), event.currency());
        } catch (JsonProcessingException e) {
            log.error("failed to parse message as TransactionEvent: {}", e.getOriginalMessage());
        }
    }
}
