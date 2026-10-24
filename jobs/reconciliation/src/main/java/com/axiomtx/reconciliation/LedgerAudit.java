package com.axiomtx.reconciliation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Component
public class LedgerAudit {

    private static final Logger log = LoggerFactory.getLogger(LedgerAudit.class);

    private final JdbcTemplate jdbc;

    public LedgerAudit(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // returns the process exit code
    public int run() {
        log.info("starting end-of-day ledger audit for {}", LocalDate.now());
        try {
            Long unprocessed = jdbc.queryForObject(
                    "SELECT count(*) FROM transactions WHERE status != 'processed'", Long.class);
            BigDecimal totalVolume = Objects.requireNonNullElse(
                    jdbc.queryForObject("SELECT sum(amount) FROM transactions WHERE status = 'processed'",
                            BigDecimal.class),
                    BigDecimal.ZERO);

            log.info("audit complete. total volume settled: ${}. unprocessed txns: {}.", totalVolume, unprocessed);
            if (unprocessed > 0) {
                log.warn("CRITICAL: unprocessed transactions found in ledger");
            }
            return 0;
        } catch (DataAccessException e) {
            log.error("audit failed", e);
            return 1;
        }
    }
}
