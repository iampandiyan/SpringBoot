package com.example.trade;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.trade.domain.TradeStatus;
import com.example.trade.service.TradeService;
import java.math.BigDecimal;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// The same migrations and the same application, but on a REAL PostgreSQL started in Docker for the length of this test.
// H2 accepted our SQL; this proves PostgreSQL does too, and that Hibernate's "validate" agrees with the schema.
// disabledWithoutDocker: on a machine without Docker the test is SKIPPED instead of failing.
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "trade.outbox.poll-interval-ms=3600000")
class PostgresMigrationTest {

    // @ServiceConnection: Spring Boot reads the container's JDBC URL, user and password and uses them as the datasource.
    // No spring.datasource.* property is needed, and the embedded H2 is not used.
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired DataSource dataSource;
    @Autowired Flyway flyway;
    @Autowired TradeService trades;
    @Autowired JdbcTemplate jdbc;

    @Test
    void theApplicationStartsOnPostgresAndAllMigrationsApplied() {
        String product = jdbc.queryForObject("select version()", String.class);
        assertThat(product).startsWith("PostgreSQL 16");
        assertThat(flyway.info().applied()).hasSize(5);
        assertThat(flyway.info().pending()).isEmpty();
    }

    @Test
    void aTradeCanBeCreatedAndSettledInPostgres() {
        var t = trades.create("US0378331005", new BigDecimal("100"), new BigDecimal("189.50"));

        assertThat(trades.settle(t.id()).status()).isEqualTo(TradeStatus.SETTLED);
        // the settlement wrote its event into the outbox table, in the same Postgres transaction
        Integer events = jdbc.queryForObject(
                "select count(*) from outbox_event where aggregate_id = ? and event_type = 'TradeSettled'",
                Integer.class, String.valueOf(t.id()));
        assertThat(events).isEqualTo(1);
    }
}
