package com.axiomtx.settlement;

import com.axiomtx.shared.TransactionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SettlementListener {

    private static final Logger log = LoggerFactory.getLogger(SettlementListener.class);

    private final ObjectMapper mapper;
    private final Ledger ledger;
    private final DeadLetterPublisher deadLetters;

    public SettlementListener(ObjectMapper mapper, Ledger ledger, DeadLetterPublisher deadLetters) {
        this.mapper = mapper;
        this.ledger = ledger;
        this.deadLetters = deadLetters;
    }

    @KafkaListener(topics = TransactionEvent.TOPIC)
    public void onMessage(String raw) {
        try {
            TransactionEvent event = mapper.readValue(raw, TransactionEvent.class);
            switch (ledger.settle(event)) {
                case SETTLED -> log.info("transaction {} settled", event.transactionId());
                case DUPLICATE -> log.warn("duplicate transaction {} skipped", event.transactionId());
                case UNKNOWN_ACCOUNT ->
                        log.warn("foreign key missing for {} - check accounts table", event.transactionId());
            }
        } catch (Exception e) {
            log.error("failed to process message: {}", e.getMessage());
            deadLetters.publish(raw, e);
        }
    }
}
