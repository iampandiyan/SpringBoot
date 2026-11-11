package com.example.trade;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.trade.audit.AuditLogger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

// Auto-configuration in miniature: the same class is a bean or not depending on a property.
class ConditionalBeanTest {

    @Nested
    @SpringBootTest(properties = "spring.main.web-application-type=none")
    class SwitchOff {
        @Autowired ApplicationContext ctx;

        @Test
        void auditLoggerIsAbsentByDefault() {
            assertThat(ctx.getBeanNamesForType(AuditLogger.class)).isEmpty();
        }
    }

    @Nested
    @SpringBootTest(properties = {"spring.main.web-application-type=none", "audit.enabled=true"})
    class SwitchOn {
        @Autowired ApplicationContext ctx;

        @Test
        void auditLoggerExistsWhenEnabled() {
            assertThat(ctx.getBeanNamesForType(AuditLogger.class)).hasSize(1);
        }
    }
}