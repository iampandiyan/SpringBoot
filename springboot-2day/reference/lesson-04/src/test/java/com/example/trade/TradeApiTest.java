package com.example.trade;

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

// Full application, real service and repository, HTTP simulated by MockMvc (no real port needed).
// One test walks the whole lifecycle, the way a client would.
@SpringBootTest
@AutoConfigureMockMvc
class TradeApiTest {

    @Autowired MockMvc mvc;

    @Test
    void createGetSettleAndConflict() throws Exception {
        String body = """
                {"isin":"US0378331005","quantity":100,"price":189.50}""";

        var created = mvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andReturn();
        String location = created.getResponse().getHeader("Location");

        mvc.perform(get(location)).andExpect(status().isOk()).andExpect(jsonPath("$.isin").value("US0378331005"));
        mvc.perform(post(location + "/settle")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SETTLED"));
        mvc.perform(post(location + "/settle")).andExpect(status().isConflict());
    }

    @Test
    void quantityAboveTheConfiguredLimitIs422() throws Exception {
        String body = """
                {"isin":"US0378331005","quantity":2000000,"price":1}""";

        mvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
    }
}
