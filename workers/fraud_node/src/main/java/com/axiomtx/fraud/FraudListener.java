package com.axiomtx.fraud;

import com.axiomtx.shared.TransactionEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// logs verdicts only, settlement does not wait on this node
@Component
public class FraudListener {

    private static final Logger log = LoggerFactory.getLogger(FraudListener.class);

    private final ObjectMapper mapper;

    public FraudListener(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @KafkaListener(topics = TransactionEvent.TOPIC)
    public void onMessage(String raw) {
        try {
            TransactionEvent event = mapper.readValue(raw, TransactionEvent.class);
            FraudRules.Verdict verdict = FraudRules.evaluate(event);
            if (verdict.fraud()) {
                log.error("FRAUD FLAGGED: {} | reason: {}", event.transactionId(), verdict.reason());
            } else {
                log.info("transaction cleared: {}", event.transactionId());
            }
        } catch (JsonProcessingException e) {
            log.error("failed to process message: {}", e.getOriginalMessage());
        }
    }
}
