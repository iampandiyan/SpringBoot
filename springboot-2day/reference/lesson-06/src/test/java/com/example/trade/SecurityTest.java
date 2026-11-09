package com.example.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;

// End-to-end security with REAL tokens: log in, call the API, see 401 and 403 for the right reasons.
@SpringBootTest
@AutoConfigureMockMvc
class SecurityTest {

    @Autowired MockMvc mvc;
    @Autowired JwtEncoder encoder;

    private static final String BODY = """
            {"isin":"US0378331005","quantity":100,"price":189.50}""";

    // ---- authentication: who are you? (401 when the answer is "unknown") ----

    @Test
    void apiWithoutATokenIs401WithABearerChallenge() throws Exception {
        mvc.perform(get("/trades"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.startsWith("Bearer")));
    }

    @Test
    void loginWithAWrongPasswordIs401() throws Exception {
        mvc.perform(post("/auth/token").with(httpBasic("alice", "wrong"))).andExpect(status().isUnauthorized());
    }

    @Test
    void loginReturnsABearerTokenWithTheRolesInIt() throws Exception {
        mvc.perform(post("/auth/token").with(httpBasic("alice", "password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(900))
                .andExpect(jsonPath("$.access_token").isNotEmpty());
    }

    @Test
    void aGarbageTokenIs401() throws Exception {
        mvc.perform(get("/trades").header(AUTHORIZATION, "Bearer not.a.jwt")).andExpect(status().isUnauthorized());
    }

    @Test
    void aTamperedTokenIs401() throws Exception {
        String good = TestTokens.bearerFor(mvc, "alice");
        String tampered = good.substring(0, good.length() - 3) + (good.endsWith("AAA") ? "BBB" : "AAA");   // break the signature

        mvc.perform(get("/trades").header(AUTHORIZATION, tampered)).andExpect(status().isUnauthorized());
    }

    @Test
    void anExpiredTokenIs401() throws Exception {
        Instant past = Instant.now().minusSeconds(3600);
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("alice").issuedAt(past.minusSeconds(60))
                .expiresAt(past).claim("roles", List.of("TRADER")).build();
        String expired = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        mvc.perform(get("/trades").header(AUTHORIZATION, "Bearer " + expired)).andExpect(status().isUnauthorized());
    }

    @Test
    void aTokenIsNotAcceptedAtTheLoginEndpoint() throws Exception {
        mvc.perform(post("/auth/token").header(AUTHORIZATION, TestTokens.bearerFor(mvc, "alice")))
                .andExpect(status().isUnauthorized());     // /auth/** wants Basic credentials, not a token
    }

    // ---- authorization: are you allowed? (403 when the answer is "known, but no") ----

    @Test
    void viewerCanReadButNotCreate() throws Exception {
        String viewer = TestTokens.bearerFor(mvc, "carol");

        mvc.perform(get("/trades").header(AUTHORIZATION, viewer)).andExpect(status().isOk());
        mvc.perform(post("/trades").header(AUTHORIZATION, viewer).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void traderCanCreateButNotSettle() throws Exception {
        String trader = TestTokens.bearerFor(mvc, "alice");
        String location = mvc.perform(post("/trades").header(AUTHORIZATION, trader)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");

        mvc.perform(post(location + "/settle").header(AUTHORIZATION, trader)).andExpect(status().isForbidden());
        mvc.perform(post(location + "/settle").header(AUTHORIZATION, TestTokens.bearerFor(mvc, "bob")))
                .andExpect(status().isOk());
    }

    // ---- actuator: health is public, the rest is for admins ----

    @Test
    void healthIsPublicButBeansNeedAdmin() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/actuator/beans")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/beans").header(AUTHORIZATION, TestTokens.bearerFor(mvc, "alice")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/actuator/beans").header(AUTHORIZATION, TestTokens.bearerFor(mvc, "admin")))
                .andExpect(status().isOk());
    }

    // ---- configuration safety ----

    @Test
    void prodRefusesToStartWithoutAJwtSecret() {
        assertThatThrownBy(() -> new SpringApplicationBuilder(TradeServiceApplication.class)
                .profiles("prod")
                .web(WebApplicationType.NONE)
                .run())
                .hasStackTraceContaining("jwtSecret");
        assertThat(System.getenv("JWT_SECRET")).as("this test assumes JWT_SECRET is not set").isNull();
    }
}
