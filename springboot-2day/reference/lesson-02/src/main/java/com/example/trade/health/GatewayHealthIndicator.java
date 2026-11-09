package com.example.trade.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

// A custom health contributor. Spring Boot finds every HealthIndicator bean and adds it to /actuator/health.
// The bean name minus "HealthIndicator" becomes the key in the JSON: here "gateway".
@Component
public class GatewayHealthIndicator implements HealthIndicator {
    private final GatewayStatus gateway;

    public GatewayHealthIndicator(GatewayStatus gateway) { this.gateway = gateway; }

    @Override
    public Health health() {
        return gateway.isUp()
                ? Health.up().withDetail("gateway", "SWIFT").build()
                : Health.down().withDetail("gateway", "SWIFT").withDetail("reason", "no heartbeat").build();
    }
}
