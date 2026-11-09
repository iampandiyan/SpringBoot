package com.example.trade;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.trade.service.TradeService;

@Component 
public class DemoRunner implements  CommandLineRunner {
    private static final Logger log=LoggerFactory.getLogger(DemoRunner.class);
    private final TradeService service;

    public DemoRunner(TradeService service){
        this.service=service;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("service class ={}",service.getClass().getSimpleName());
        var t=service.create("US0378331005", new BigDecimal("100"), new BigDecimal("189.50"));
        log.info("Created at {}",t);
        log.info("Settled at {}",service.settle(t.id()));        
    }
    
}
