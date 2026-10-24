package com.axiomtx.fraud;

import com.axiomtx.shared.TransactionEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FraudRulesTest {

    private static TransactionEvent event(String sender, String receiver, String amount) {
        return new TransactionEvent("tx-1", sender, receiver, new BigDecimal(amount), "USD", null);
    }

    @Test
    void clearsOrdinaryTransaction() {
        assertThat(FraudRules.evaluate(event("acc_1000", "acc_2000", "250.00")))
                .isEqualTo(new FraudRules.Verdict(false, "Clear"));
    }

    @Test
    void amountAtThresholdIsClear() {
        assertThat(FraudRules.evaluate(event("acc_1000", "acc_2000", "10000.00")).fraud()).isFalse();
    }

    @Test
    void flagsAmountAboveThreshold() {
        assertThat(FraudRules.evaluate(event("acc_1000", "acc_2000", "10000.01")))
                .isEqualTo(new FraudRules.Verdict(true, "Amount exceeds 10k threshold"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"acc_9999", "acc_0000", "acc_6666"})
    void flagsBlocklistedSender(String sender) {
        assertThat(FraudRules.evaluate(event(sender, "acc_2000", "50.00")))
                .isEqualTo(new FraudRules.Verdict(true, "Sender is on high-risk blocklist"));
    }

    @Test
    void blocklistOnlyAppliesToSender() {
        assertThat(FraudRules.evaluate(event("acc_1000", "acc_9999", "50.00")).fraud()).isFalse();
    }
}
