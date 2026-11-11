package com.example.trade;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.trade.domain.Trade;
import com.example.trade.domain.TradeStatus;
import com.example.trade.service.BusinessRuleException;
import com.example.trade.service.TradeNotFoundException;
import com.example.trade.service.TradeService;
import com.example.trade.service.TradeStateException;
import com.example.trade.security.SecurityConfig;
import com.example.trade.security.SecurityProperties;
import com.example.trade.web.TradeController;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// A "slice" test: only the web layer is started (controllers, the exception handler, JSON conversion).
// The service is replaced by a Mockito mock, so we test HTTP behaviour alone and fast.
// Since lesson 05 the slice also loads our real SecurityConfig, so the real URL rules and @PreAuthorize apply.
@WebMvcTest(TradeController.class)
@Import(SecurityConfig.class)
@EnableConfigurationProperties(SecurityProperties.class)   // a slice does not scan properties classes, so we add it
class TradeControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean TradeService service;

    private static final String VALID = """
            {"isin":"US0378331005","quantity":100,"price":189.50}""";

    // jwt() puts an already-validated token into the request, so these tests need no real signing.
    private static JwtRequestPostProcessor as(String role) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private static Trade trade(long id, TradeStatus status) {
        return new Trade(id, "US0378331005", new BigDecimal("100"), new BigDecimal("189.50"), status);
    }

    @Test
    void createReturns201WithLocationHeader() throws Exception {
        given(service.create(eq("US0378331005"), any(), any())).willReturn(trade(7, TradeStatus.NEW));

        mvc.perform(post("/trades").with(as("TRADER")).contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/trades/7"))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void invalidBodyReturns400WithFieldErrors() throws Exception {
        String bad = """
                {"isin":"not-an-isin","quantity":-5}""";

        mvc.perform(post("/trades").with(as("TRADER")).contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.isin").value("must be a 12-character ISIN"))
                .andExpect(jsonPath("$.errors.quantity").exists())
                .andExpect(jsonPath("$.errors.price").exists());
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mvc.perform(post("/trades").with(as("TRADER")).contentType(MediaType.APPLICATION_JSON).content("{oops"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownTradeReturns404ProblemDetail() throws Exception {
        given(service.get(99L)).willThrow(new TradeNotFoundException(99L));

        mvc.perform(get("/trades/99").with(as("VIEWER")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Trade not found: 99"));
    }

    @Test
    void settlingTwiceReturns409() throws Exception {
        given(service.settle(1L)).willThrow(new TradeStateException("Trade 1 is already settled"));

        mvc.perform(post("/trades/1/settle").with(as("OPS"))).andExpect(status().isConflict());
    }

    @Test
    void businessRuleReturns422() throws Exception {
        given(service.create(any(), any(), any())).willThrow(new BusinessRuleException("too big"));

        mvc.perform(post("/trades").with(as("TRADER")).contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("too big"));
    }

    @Test
    void unexpectedErrorReturnsGeneric500WithoutLeakingDetails() throws Exception {
        given(service.get(1L)).willThrow(new IllegalStateException("jdbc:oracle://secret-host failed"));

        mvc.perform(get("/trades/1").with(as("VIEWER")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Unexpected error"));
    }

    @Test
    void listPassesTheStatusFilterAndRejectsBadValues() throws Exception {
        given(service.list(TradeStatus.NEW)).willReturn(List.of(trade(1, TradeStatus.NEW)));

        mvc.perform(get("/trades").with(as("VIEWER")).param("status", "NEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
        mvc.perform(get("/trades").with(as("VIEWER")).param("status", "BOGUS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void noTokenReturns401() throws Exception {
        mvc.perform(get("/trades/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void viewerCannotCreateAndGets403ProblemDetail() throws Exception {
        mvc.perform(post("/trades").with(as("VIEWER")).contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void onlyOpsMaySettle() throws Exception {
        mvc.perform(post("/trades/1/settle").with(as("TRADER"))).andExpect(status().isForbidden());
    }
}
