package com.axiomtx.settlement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// runs the real init.sql against h2 in postgres mode
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:ledger;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=file:../../infra/postgres/init.sql",
        "spring.kafka.listener.auto-startup=false"
})
class SettlementListenerTest {

    @Autowired
    private SettlementListener listener;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private KafkaTemplate<String, String> kafka;

    @BeforeEach
    void resetLedger() {
        jdbc.update("DELETE FROM transactions");
        jdbc.update("DELETE FROM accounts");
        jdbc.update("INSERT INTO accounts (account_id, balance) VALUES ('acc_1000', 10000), ('acc_2000', 10000)");
        when(kafka.send(anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));
    }

    private static String message(String transactionId, String sender, String amount) {
        return """
                {"transaction_id": "%s", "sender_id": "%s", "receiver_id": "acc_2000",
                 "amount": %s, "currency": "USD", "status": "pending"}
                """.formatted(transactionId, sender, amount);
    }

    private int rowCount(String transactionId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM transactions WHERE transaction_id = ?", Integer.class, transactionId);
    }

    private JsonNode deadLetter() throws Exception {
        var captor = ArgumentCaptor.forClass(String.class);
        verify(kafka).send(eq(DeadLetterPublisher.TOPIC), captor.capture());
        return mapper.readTree(captor.getValue());
    }

    @Test
    void settlesNewTransaction() {
        listener.onMessage(message("tx-1", "acc_1000", "123.45"));

        var row = jdbc.queryForMap("SELECT amount, status FROM transactions WHERE transaction_id = 'tx-1'");
        assertThat(row.get("amount")).isEqualTo(new BigDecimal("123.45"));
        assertThat(row.get("status")).isEqualTo("processed");
        verify(kafka, never()).send(anyString(), anyString());
    }

    @Test
    void redeliveredTransactionSettlesExactlyOnce() {
        String raw = message("tx-1", "acc_1000", "123.45");

        listener.onMessage(raw);
        listener.onMessage(raw);

        assertThat(rowCount("tx-1")).isEqualTo(1);
        verify(kafka, never()).send(anyString(), anyString());
    }

    @Test
    void duplicateIdNeverOverwritesSettledAmount() {
        listener.onMessage(message("tx-1", "acc_1000", "123.45"));
        listener.onMessage(message("tx-1", "acc_1000", "999.99"));

        assertThat(jdbc.queryForObject("SELECT amount FROM transactions WHERE transaction_id = 'tx-1'",
                BigDecimal.class)).isEqualTo(new BigDecimal("123.45"));
    }

    @Test
    void unknownAccountIsSkippedWithoutDeadLetter() {
        listener.onMessage(message("tx-1", "acc_missing", "10.00"));

        assertThat(rowCount("tx-1")).isZero();
        verify(kafka, never()).send(anyString(), anyString());
    }

    @Test
    void malformedMessageGoesToDeadLetter() throws Exception {
        listener.onMessage("not json");

        JsonNode letter = deadLetter();
        assertThat(letter.get("original_data").asText()).isEqualTo("not json");
        assertThat(letter.get("error").asText()).isNotBlank();
    }

    @Test
    void messageMissingAmountGoesToDeadLetter() throws Exception {
        String raw = """
                {"transaction_id": "tx-1", "sender_id": "acc_1000", "receiver_id": "acc_2000", "currency": "USD"}
                """;

        listener.onMessage(raw);

        assertThat(deadLetter().get("original_data").asText()).isEqualTo(raw);
        assertThat(rowCount("tx-1")).isZero();
    }

    @Test
    void otherDatabaseFailureGoesToDeadLetter() throws Exception {
        String tooLongForColumn = "acc_" + "1".repeat(60);
        String raw = message("tx-1", tooLongForColumn, "10.00");

        listener.onMessage(raw);

        assertThat(deadLetter().get("original_data").asText()).isEqualTo(raw);
        assertThat(rowCount("tx-1")).isZero();
    }
}
