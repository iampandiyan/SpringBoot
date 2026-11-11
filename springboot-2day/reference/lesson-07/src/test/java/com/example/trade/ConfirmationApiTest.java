package com.example.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.trade.gateway.FlakySettlementGateway;
import com.jayway.jsonpath.JsonPath;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// The whole stack over MockMvc: real security, real controller, real resilience proxies, simulated gateway.
@SpringBootTest(properties = {
        "resilience4j.retry.instances.gateway.wait-duration=10ms",
        "management.endpoints.web.exposure.include=*"})
@AutoConfigureMockMvc
class ConfirmationApiTest {

    @Autowired MockMvc mvc;
    @Autowired FlakySettlementGateway gateway;
    @Autowired CircuitBreakerRegistry breakers;

    @BeforeEach
    @AfterEach
    void cleanState() {
        gateway.reset();
        breakers.circuitBreaker("gateway").reset();
    }

    private long createTrade() throws Exception {
        String alice = TestTokens.bearerFor(mvc, "alice");
        String body = mvc.perform(post("/trades").header("Authorization", alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isin\":\"US0378331005\",\"quantity\":100,\"price\":189.50}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void confirmReturns200WhenTheGatewayAnswers() throws Exception {
        long id = createTrade();
        String bob = TestTokens.bearerFor(mvc, "bob");

        mvc.perform(post("/trades/" + id + "/confirm").header("Authorization", bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmed").value(true))
                .andExpect(jsonPath("$.reference").value("SWIFT-T" + id));
    }

    @Test
    void confirmReturns202WhenTheGatewayIsDown() throws Exception {
        long id = createTrade();
        String bob = TestTokens.bearerFor(mvc, "bob");
        gateway.failNext(100, false);

        mvc.perform(post("/trades/" + id + "/confirm").header("Authorization", bob))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.confirmed").value(false));
    }

    @Test
    void theDevSwitchBreaksTheSimulatedGatewayForAdminsOnly() throws Exception {
        long id = createTrade();
        mvc.perform(post("/sim/gateway/fail-next?calls=100").header("Authorization", TestTokens.bearerFor(mvc, "bob")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/sim/gateway/fail-next?calls=100").header("Authorization", TestTokens.bearerFor(mvc, "admin")))
                .andExpect(status().isNoContent());

        mvc.perform(post("/trades/" + id + "/confirm").header("Authorization", TestTokens.bearerFor(mvc, "bob")))
                .andExpect(status().isAccepted());
    }

    @Test
    void aTraderMayNotConfirmAndAnUnknownTradeIs404() throws Exception {
        long id = createTrade();
        mvc.perform(post("/trades/" + id + "/confirm").header("Authorization", TestTokens.bearerFor(mvc, "alice")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/trades/9999/confirm").header("Authorization", TestTokens.bearerFor(mvc, "bob")))
                .andExpect(status().isNotFound());
    }

    @Test
    void theBreakerIsVisibleInActuatorForAdmins() throws Exception {
        String admin = TestTokens.bearerFor(mvc, "admin");
        String json = mvc.perform(get("/actuator/circuitbreakers").header("Authorization", admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(json).contains("gateway").contains("CLOSED");
    }
}
