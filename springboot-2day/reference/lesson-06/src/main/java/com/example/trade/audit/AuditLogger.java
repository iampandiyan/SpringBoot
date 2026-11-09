package com.example.trade.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// A conditional bean: it exists ONLY when audit.enabled=true. This is exactly how auto-configuration
// decides what to create, just with our own switch instead of "is the H2 jar on the classpath?".
@Component
@ConditionalOnProperty(name = "audit.enabled", havingValue = "true")
public class AuditLogger {
    private static final Logger log = LoggerFactory.getLogger(AuditLogger.class);

    public AuditLogger() { log.info("audit logging is ON"); }
}
