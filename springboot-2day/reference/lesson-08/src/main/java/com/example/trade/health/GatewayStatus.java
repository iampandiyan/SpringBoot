package com.example.trade.health;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

// Stand-in for the state of a real downstream (a SWIFT gateway, a database, a broker).
// In production this would be a ping or a connection check. Here a flag lets a test flip it.
@Component
public class GatewayStatus {
    private final AtomicBoolean up = new AtomicBoolean(true);

    public boolean isUp() { return up.get(); }

    public void setUp(boolean value) { up.set(value); }
}
