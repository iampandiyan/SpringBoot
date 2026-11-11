package com.example.trade.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component 
public class GatewayHealthIndicator implements HealthIndicator {
    private final GatewayStatus gateway;
    public GatewayHealthIndicator(GatewayStatus status){
        this.gateway=status;
    }

    @Override 
    public Health health(){
          return gateway.isUp()
                ? Health.up().withDetail("gateway", "SWIFT").build()
                : Health.down().withDetail("gateway", "SWIFT").withDetail("reason", "no heartbeat").build();
    }
}
