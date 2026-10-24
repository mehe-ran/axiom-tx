package com.axiomtx.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionEventTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void readsPayloadProducedByPydantic() throws Exception {
        String json = """
                {"transaction_id": "tx-1", "sender_id": "acc_1000", "receiver_id": "acc_2000",
                 "amount": 123.45, "currency": "USD", "status": "pending"}
                """;

        TransactionEvent event = mapper.readValue(json, TransactionEvent.class);

        assertThat(event).isEqualTo(new TransactionEvent(
                "tx-1", "acc_1000", "acc_2000", new BigDecimal("123.45"), "USD", "pending"));
    }

    @Test
    void writesSnakeCaseKeys() throws Exception {
        var event = new TransactionEvent("tx-1", "acc_1000", "acc_2000", new BigDecimal("10.50"), "USD", null);

        assertThat(mapper.readTree(mapper.writeValueAsString(event)).fieldNames())
                .toIterable()
                .containsExactly("transaction_id", "sender_id", "receiver_id", "amount", "currency", "status");
    }

    @Test
    void statusDefaultsToPending() {
        var event = new TransactionEvent("tx-1", "acc_1000", "acc_2000", BigDecimal.ONE, "USD", null);

        assertThat(event.status()).isEqualTo("pending");
    }

    @Test
    void rejectsMissingRequiredField() {
        String json = """
                {"transaction_id": "tx-1", "sender_id": "acc_1000", "receiver_id": "acc_2000", "currency": "USD"}
                """;

        assertThatThrownBy(() -> mapper.readValue(json, TransactionEvent.class))
                .isInstanceOf(ValueInstantiationException.class)
                .hasMessageContaining("amount is required");
    }
}
