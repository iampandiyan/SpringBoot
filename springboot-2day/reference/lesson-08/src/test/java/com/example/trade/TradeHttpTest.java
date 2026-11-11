package com.example.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.example.trade.domain.Trade;
import com.example.trade.domain.TradeStatus;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

// The top of the pyramid: a REAL embedded Tomcat on a random port and REAL HTTP calls, like a client in production.
// Slower than MockMvc, so keep only a few of these: the main journeys, not every edge case.
@SpringBootTest(webEnvironment = RANDOM_PORT)
class TradeHttpTest {

    @Autowired TestRestTemplate rest;

    private static final String BODY = """
            {"isin":"US0378331005","quantity":100,"price":189.50}""";

    @SuppressWarnings("unchecked")
    private String login(String user) {
        ResponseEntity<Map> response = rest.withBasicAuth(user, "password").postForEntity("/auth/token", null, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) response.getBody().get("access_token");
    }

    private HttpEntity<String> json(String body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }

    @Test
    void aTraderBooksAndOpsSettlesOverRealHttp() {
        String trader = login("alice"), ops = login("bob");

        ResponseEntity<Trade> created = rest.exchange("/trades", HttpMethod.POST, json(BODY, trader), Trade.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getHeaders().getLocation()).isNotNull();

        String location = created.getHeaders().getLocation().toString();
        ResponseEntity<Trade> settled = rest.exchange(location + "/settle", HttpMethod.POST, json(null, ops), Trade.class);
        assertThat(settled.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(settled.getBody().status()).isEqualTo(TradeStatus.SETTLED);
    }

    @Test
    void withoutATokenTheApiIs401() {
        ResponseEntity<String> response = rest.getForEntity("/trades", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @SuppressWarnings("unchecked")
    void aBadRequestIs400WithFieldErrorsAsProblemJson() {
        String bad = """
                {"isin":"nope","quantity":1,"price":1}""";

        ResponseEntity<Map> response = rest.exchange("/trades", HttpMethod.POST, json(bad, login("alice")), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType().toString()).startsWith("application/problem+json");
        assertThat((Map<String, String>) response.getBody().get("errors")).containsKey("isin");
    }
}
