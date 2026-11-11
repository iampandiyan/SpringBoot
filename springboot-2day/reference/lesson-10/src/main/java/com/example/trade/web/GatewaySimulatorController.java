package com.example.trade.web;

import com.example.trade.gateway.FlakySettlementGateway;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// A switch to break and repair the SIMULATED gateway while you experiment with curl. It exists only in the "dev"
// profile (the default on your laptop); in "prod" this class is not even created.
@RestController
@Profile("dev")
@RequestMapping("/sim/gateway")
public class GatewaySimulatorController {

    private final FlakySettlementGateway gateway;

    public GatewaySimulatorController(FlakySettlementGateway gateway) {
        this.gateway = gateway;
    }

    // POST /sim/gateway/fail-next?calls=100  : the next 100 calls fail.   calls=0 : repair it.
    @PostMapping("/fail-next")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> failNext(@RequestParam int calls) {
        gateway.failNext(calls, false);
        return ResponseEntity.noContent().build();
    }
}
