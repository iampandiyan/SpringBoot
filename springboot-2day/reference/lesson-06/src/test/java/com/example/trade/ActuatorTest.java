package com.example.trade;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.trade.health.GatewayStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

class ActuatorTest {

    // Since lesson 05 only /actuator/health and /actuator/info are public; everything else needs the ADMIN role.
    private static final org.springframework.test.web.servlet.request.RequestPostProcessor ADMIN =
            jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    class Dev {
        @Autowired MockMvc mvc;
        @Autowired GatewayStatus gateway;

        @Test
        void healthIsUpAndShowsOurCustomIndicator() throws Exception {
            mvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.components.gateway.status").value("UP"));
        }

        @Test
        void healthGoesDownWith503WhenTheGatewayIsDown() throws Exception {
            gateway.setUp(false);
            try {
                mvc.perform(get("/actuator/health"))
                        .andExpect(status().isServiceUnavailable())
                        .andExpect(jsonPath("$.status").value("DOWN"))
                        .andExpect(jsonPath("$.components.gateway.details.reason").value("no heartbeat"));
            } finally {
                gateway.setUp(true);    // the context is shared by other tests: always restore
            }
        }

        @Test
        void infoShowsKeysFromTheYaml() throws Exception {
            mvc.perform(get("/actuator/info"))
                    .andExpect(jsonPath("$.app.name").value("trade-service"));
        }

        @Test
        void devExposesTheBeansEndpoint() throws Exception {
            mvc.perform(get("/actuator/beans").with(ADMIN)).andExpect(status().isOk());
        }
    }

    @Nested
    @SpringBootTest(properties = "trade.security.jwt-secret=test-secret-test-secret-test-secret-123")   // prod has no default secret
    @AutoConfigureMockMvc
    @ActiveProfiles("prod")
    // the prod profile has no default secret (on purpose), so the test supplies one
    class Prod {
        @Autowired MockMvc mvc;

        @Test
        void prodDoesNotExposeBeans() throws Exception {
            mvc.perform(get("/actuator/beans").with(ADMIN)).andExpect(status().isNotFound());
        }

        @Test
        void prodHidesHealthDetailsButKeepsProbes() throws Exception {
            mvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.components").doesNotExist());
            mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
            mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
        }
    }
}
