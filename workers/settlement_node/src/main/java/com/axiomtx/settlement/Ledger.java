package com.axiomtx.settlement;

import com.axiomtx.shared.TransactionEvent;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.Set;

@Repository
public class Ledger {

    public enum Outcome { SETTLED, DUPLICATE, UNKNOWN_ACCOUNT }

    // 23503 is the standard state, h2 reports a missing parent row as 23506
    private static final Set<String> FOREIGN_KEY_VIOLATION = Set.of("23503", "23506");

    private final JdbcTemplate jdbc;

    public Ledger(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // the transaction_id primary key is what makes redelivery safe
    public Outcome settle(TransactionEvent event) {
        try {
            jdbc.update(
                    "INSERT INTO transactions (transaction_id, sender_id, receiver_id, amount) VALUES (?, ?, ?, ?)",
                    event.transactionId(), event.senderId(), event.receiverId(), event.amount());
            return Outcome.SETTLED;
        } catch (DuplicateKeyException e) {
            return Outcome.DUPLICATE;
        } catch (DataIntegrityViolationException e) {
            if (e.getMostSpecificCause() instanceof SQLException sql
                    && FOREIGN_KEY_VIOLATION.contains(sql.getSQLState())) {
                return Outcome.UNKNOWN_ACCOUNT;
            }
            throw e;
        }
    }
}
