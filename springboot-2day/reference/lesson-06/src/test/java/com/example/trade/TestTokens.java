package com.example.trade;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.springframework.test.web.servlet.MockMvc;

// Test helper: logs in through the REAL /auth/token endpoint and returns the Authorization header value.
final class TestTokens {
    private TestTokens() {}

    static String bearerFor(MockMvc mvc, String user) throws Exception {
        String json = mvc.perform(post("/auth/token").with(httpBasic(user, "password")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.<String>read(json, "$.access_token");
    }
}
