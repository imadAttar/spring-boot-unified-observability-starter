package com.imadattar.observability.logging;

import com.imadattar.observability.config.ObservabilityProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for structured JSON logging with Logstash encoder.
 *
 * Enables:
 * - JSON log format (Logstash/ELK compatible)
 * - Trace ID and Span ID injection in logs (correlation)
 * - MDC (Mapped Diagnostic Context) support
 * - Contextual logging (user, request ID, etc.)
 *
 * Logback configuration is in src/main/resources/logback-spring.xml
 */
@Configuration
@ConditionalOnProperty(prefix = "observability.logging", name = "json-enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class LoggingConfiguration {

    public LoggingConfiguration(ObservabilityProperties properties) {
        ObservabilityProperties.Logging loggingConfig = properties.getLogging();

        log.info("✅ Structured JSON logging enabled");
        log.info("   - Trace ID in logs: {}", loggingConfig.isIncludeTraceId());
        log.info("   - Span ID in logs: {}", loggingConfig.isIncludeSpanId());
        log.info("   - MDC in logs: {}", loggingConfig.isIncludeMdc());
        log.info("   - Log level: {}", loggingConfig.getLevel());
    }
}
