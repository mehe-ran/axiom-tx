package com.axiomtx.fraud;

import com.axiomtx.shared.TransactionEvent;

import java.math.BigDecimal;
import java.util.Set;

public final class FraudRules {

    static final BigDecimal AMOUNT_LIMIT = new BigDecimal("10000.00");
    static final Set<String> BLOCKLIST = Set.of("acc_9999", "acc_0000", "acc_6666");

    public record Verdict(boolean fraud, String reason) {
    }

    private FraudRules() {
    }

    public static Verdict evaluate(TransactionEvent event) {
        if (event.amount().compareTo(AMOUNT_LIMIT) > 0) {
            return new Verdict(true, "Amount exceeds 10k threshold");
        }
        if (BLOCKLIST.contains(event.senderId())) {
            return new Verdict(true, "Sender is on high-risk blocklist");
        }
        return new Verdict(false, "Clear");
    }
}
