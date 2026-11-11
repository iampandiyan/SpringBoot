package com.example.trade;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// Full application, real service, repository, security and database; HTTP simulated by MockMvc (no real port).
// One test walks the whole lifecycle, the way real clients would: a trader books, ops settles.
@SpringBootTest
@AutoConfigureMockMvc
class TradeApiTest {

    @Autowired MockMvc mvc;

    @Test
    void createGetSettleAndConflict() throws Exception {
        String trader = TestTokens.bearerFor(mvc, "alice");
        String ops = TestTokens.bearerFor(mvc, "bob");
        String body = """
                {"isin":"US0378331005","quantity":100,"price":189.50}""";

        var created = mvc.perform(post("/trades").header(AUTHORIZATION, trader)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andReturn();
        String location = created.getResponse().getHeader("Location");

        mvc.perform(get(location).header(AUTHORIZATION, trader))
                .andExpect(status().isOk()).andExpect(jsonPath("$.isin").value("US0378331005"));
        mvc.perform(post(location + "/settle").header(AUTHORIZATION, ops))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SETTLED"));
        mvc.perform(post(location + "/settle").header(AUTHORIZATION, ops))
                .andExpect(status().isConflict());
    }

    @Test
    void quantityAboveTheConfiguredLimitIs422() throws Exception {
        String body = """
                {"isin":"US0378331005","quantity":2000000,"price":1}""";

        mvc.perform(post("/trades").header(AUTHORIZATION, TestTokens.bearerFor(mvc, "alice"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
    }
}
