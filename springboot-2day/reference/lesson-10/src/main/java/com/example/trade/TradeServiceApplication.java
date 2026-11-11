package com.example.trade;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

// @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan.
// Scanning starts at THIS package, so all our classes must live under com.example.trade.
// @ConfigurationPropertiesScan finds @ConfigurationProperties classes (TradeProperties) and registers them as beans.
// @EnableScheduling (NEW in lesson 08) switches on @Scheduled methods: the outbox relay polls on a timer.
@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
public class TradeServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TradeServiceApplication.class, args);
    }
}
