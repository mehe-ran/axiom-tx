package com.axiomtx.shared;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.util.Objects;

// message contract on tx_pending, snake_case so python and java services can share it
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TransactionEvent(
        String transactionId,
        String senderId,
        String receiverId,
        BigDecimal amount,
        String currency,
        String status) {

    public static final String TOPIC = "tx_pending";

    public TransactionEvent {
        Objects.requireNonNull(transactionId, "transaction_id is required");
        Objects.requireNonNull(senderId, "sender_id is required");
        Objects.requireNonNull(receiverId, "receiver_id is required");
        Objects.requireNonNull(amount, "amount is required");
        Objects.requireNonNull(currency, "currency is required");
        if (status == null) {
            status = "pending";
        }
    }
}
