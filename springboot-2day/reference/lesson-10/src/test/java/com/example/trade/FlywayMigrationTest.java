package com.example.trade;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// Flyway ran at startup and recorded every migration in its own table (flyway_schema_history).
// If a migration did not match the entities, Hibernate's "validate" would already have stopped the context from starting.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "trade.outbox.poll-interval-ms=3600000")
class FlywayMigrationTest {

    @Autowired Flyway flyway;

    @Test
    void everyMigrationWasAppliedInOrder() {
        assertThat(flyway.info().applied()).extracting(m -> m.getVersion().getVersion())
                .containsExactly("1", "2", "3", "4", "5");
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.info().applied()).extracting(MigrationInfo::getDescription)
                .containsExactly("trades", "outbox", "saga", "settled trade report", "processed event");
    }
}
