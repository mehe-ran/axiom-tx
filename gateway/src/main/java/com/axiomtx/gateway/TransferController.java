package com.axiomtx.gateway;

import com.axiomtx.shared.TransactionEvent;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransferController {

    private static final Logger log = LoggerFactory.getLogger(TransferController.class);

    private final TransactionPublisher publisher;

    public TransferController(TransactionPublisher publisher) {
        this.publisher = publisher;
    }

    @PostMapping("/transfer")
    public Accepted transfer(@Valid @RequestBody TransferRequest request) {
        var event = new TransactionEvent(
                request.transactionId(),
                request.senderId(),
                request.receiverId(),
                request.amount(),
                request.currency(),
                null);
        publisher.publish(event);
        log.info("ingested and routed transaction {}", event.transactionId());
        return new Accepted("accepted", event.transactionId());
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Accepted(String status, String transactionId) {
    }
}
