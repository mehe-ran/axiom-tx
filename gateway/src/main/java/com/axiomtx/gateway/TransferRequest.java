package com.axiomtx.gateway;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

@DistinctAccounts
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TransferRequest(
        String transactionId,
        @NotNull @Size(min = 4) String senderId,
        @NotNull @Size(min = 4) String receiverId,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
        @Size(max = 3) String currency) {

    public TransferRequest {
        if (transactionId == null) {
            transactionId = UUID.randomUUID().toString();
        }
        if (currency == null) {
            currency = "USD";
        }
    }
}
