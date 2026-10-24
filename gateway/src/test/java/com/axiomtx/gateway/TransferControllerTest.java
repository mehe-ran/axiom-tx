package com.axiomtx.gateway;

import com.axiomtx.shared.TransactionEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransferController.class)
class TransferControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TransactionPublisher publisher;

    private ResultActions postTransfer(String body) throws Exception {
        return mvc.perform(post("/transfer").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void acceptsValidTransferAndPublishesIt() throws Exception {
        postTransfer("""
                {"transaction_id": "tx-1", "sender_id": "acc_1000", "receiver_id": "acc_2000",
                 "amount": 250.75, "currency": "EUR"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("accepted"))
                .andExpect(jsonPath("$.transaction_id").value("tx-1"));

        verify(publisher).publish(new TransactionEvent(
                "tx-1", "acc_1000", "acc_2000", new BigDecimal("250.75"), "EUR", "pending"));
    }

    @Test
    void defaultsTransactionIdAndCurrency() throws Exception {
        postTransfer("""
                {"sender_id": "acc_1000", "receiver_id": "acc_2000", "amount": 10}
                """)
                .andExpect(status().isOk());

        var captor = ArgumentCaptor.forClass(TransactionEvent.class);
        verify(publisher).publish(captor.capture());
        TransactionEvent event = captor.getValue();
        assertThat(UUID.fromString(event.transactionId())).isNotNull();
        assertThat(event.currency()).isEqualTo("USD");
    }

    @Test
    void rejectsSameSenderAndReceiver() throws Exception {
        postTransfer("""
                {"sender_id": "acc_1000", "receiver_id": "acc_1000", "amount": 500}
                """)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail[0].loc[1]").value("receiver_id"));

        verify(publisher, never()).publish(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-150.00"})
    void rejectsNonPositiveAmount(String amount) throws Exception {
        postTransfer("""
                {"sender_id": "acc_1000", "receiver_id": "acc_2000", "amount": %s}
                """.formatted(amount))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail[0].loc[0]").value("body"))
                .andExpect(jsonPath("$.detail[0].loc[1]").value("amount"));

        verify(publisher, never()).publish(any());
    }

    @Test
    void rejectsShortAccountIds() throws Exception {
        postTransfer("""
                {"sender_id": "acc", "receiver_id": "ac", "amount": 10}
                """)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail.length()").value(2))
                .andExpect(jsonPath("$.detail[*].loc[1]").value(
                        containsInAnyOrder("sender_id", "receiver_id")));
    }

    @Test
    void rejectsMissingAmount() throws Exception {
        postTransfer("""
                {"sender_id": "acc_1000", "receiver_id": "acc_2000"}
                """)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail[0].loc[1]").value("amount"));
    }

    @Test
    void rejectsCurrencyLongerThanThreeChars() throws Exception {
        postTransfer("""
                {"sender_id": "acc_1000", "receiver_id": "acc_2000", "amount": 10, "currency": "USDT"}
                """)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail[0].loc[1]").value("currency"));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        postTransfer("{\"sender_id\": ")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail[0].type").value("json_invalid"));
    }

    @Test
    void returns500WhenKafkaSendFails() throws Exception {
        doThrow(new TransactionPublisher.RoutingException(new RuntimeException("broker down")))
                .when(publisher).publish(any());

        postTransfer("""
                {"sender_id": "acc_1000", "receiver_id": "acc_2000", "amount": 10}
                """)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Internal routing failure"));
    }
}
