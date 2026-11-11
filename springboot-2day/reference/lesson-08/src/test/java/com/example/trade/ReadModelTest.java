package com.example.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.trade.gateway.FlakySettlementGateway;
import com.example.trade.outbox.OutboxEventEntity;
import com.example.trade.outbox.OutboxEventRepository;
import com.example.trade.outbox.OutboxMessage;
import com.example.trade.outbox.OutboxRelay;
import com.example.trade.report.SettledTradeReportRepository;
import com.example.trade.report.TradeReportProjector;
import com.example.trade.saga.CashLedger;
import com.example.trade.saga.SagaJournal;
import com.example.trade.saga.SecuritiesDepot;
import com.example.trade.service.TradeService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

// The whole write side to read side journey over HTTP: settle through the saga, publish through the relay,
// read the result from the read model.
@SpringBootTest(properties = {
        "trade.outbox.poll-interval-ms=3600000",
        "resilience4j.retry.instances.gateway.wait-duration=10ms"})
@AutoConfigureMockMvc
class ReadModelTest {

    private static final String ISIN = "US5949181045";

    @Autowired MockMvc mvc;
    @Autowired TradeService trades;
    @Autowired OutboxRelay relay;
    @Autowired OutboxEventRepository outbox;
    @Autowired SettledTradeReportRepository reports;
    @Autowired TradeReportProjector projector;
    @Autowired SecuritiesDepot depot;
    @Autowired CashLedger ledger;
    @Autowired SagaJournal journal;
    @Autowired FlakySettlementGateway gateway;
    @Autowired CircuitBreakerRegistry breakers;

    @BeforeEach
    @AfterEach
    void clean() {
        outbox.deleteAll();
        reports.deleteAll();
        depot.reset();
        ledger.reset();
        journal.clear();
        gateway.reset();
        breakers.circuitBreaker("gateway").reset();
    }

    private long newTradeId() {
        return trades.create(ISIN, new BigDecimal("200"), new BigDecimal("10.5")).id();
    }

    @Test
    void aSettledTradeAppearsInTheReportOnlyAfterTheRelayHasPublishedItsEvent() throws Exception {
        long id = newTradeId();
        String bob = TestTokens.bearerFor(mvc, "bob");

        mvc.perform(post("/trades/" + id + "/settlement").header("Authorization", bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("COMPLETED"));

        // The write side is done, but the read side has not heard yet: EVENTUAL consistency.
        mvc.perform(get("/reports/settled?isin=" + ISIN).header("Authorization", TestTokens.bearerFor(mvc, "carol")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        relay.publishPending();

        mvc.perform(get("/reports/settled?isin=" + ISIN).header("Authorization", TestTokens.bearerFor(mvc, "carol")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tradeId").value(id))
                .andExpect(jsonPath("$[0].notional").value(2100.0));    // 200 x 10.5
    }

    @Test
    void handlingTheSameEventTwiceLeavesOneRow() {
        long id = newTradeId();
        trades.settle(id);
        OutboxEventEntity e = outbox.findAll().stream()
                .filter(x -> x.getAggregateId().equals(String.valueOf(id))).findFirst().orElseThrow();
        OutboxMessage message = new OutboxMessage(e.getId(), e.getEventType(), e.getAggregateId(), e.getPayload(), e.getCreatedAt());

        projector.handle(message);
        projector.handle(message);                          // a duplicate delivery, as at-least-once allows

        assertThat(reports.findByIsinOrderBySettledAtDesc(ISIN)).hasSize(1);
    }

    @Test
    void aFailedSettlementAnswers422AndNeverReachesTheReport() throws Exception {
        long id = newTradeId();
        depot.refuseDeliveries(true);

        mvc.perform(post("/trades/" + id + "/settlement").header("Authorization", TestTokens.bearerFor(mvc, "bob")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.state").value("COMPENSATED"));

        relay.publishPending();
        assertThat(reports.findByIsinOrderBySettledAtDesc(ISIN)).isEmpty();
    }

    @Test
    void onlyOperationsMaySettle() throws Exception {
        long id = newTradeId();
        mvc.perform(post("/trades/" + id + "/settlement").header("Authorization", TestTokens.bearerFor(mvc, "alice")))
                .andExpect(status().isForbidden());
    }
}
