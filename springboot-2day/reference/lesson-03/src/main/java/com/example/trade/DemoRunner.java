package com.example.trade;

import com.example.trade.service.TradeService;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Runs once at startup, so we can SEE the container wiring work without a web layer yet.
@Component
public class DemoRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoRunner.class);
    private final TradeService service;

    public DemoRunner(TradeService service) { this.service = service; }

    @Override
    public void run(String... args) {
        log.info("service class = {}", service.getClass().getSimpleName()); // shows the CGLIB proxy
        var t = service.create("US0378331005", new BigDecimal("100"), new BigDecimal("189.50"));
        log.info("created {}", t);
        log.info("settled {}", service.settle(t.id()));
    }
}
