package com.imadattar.observability.logging;

import com.imadattar.observability.config.ObservabilityProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for structured JSON logging with Logstash encoder.
 * <p>
 * This configuration activates when {@code observability.logging.format=json} (default).
 * It uses the Logstash Logback encoder to provide structured JSON logging compatible
 * with ELK stack and other log aggregation systems.
 * <p>
 * Enables:
 * <ul>
 *   <li>JSON log format (Logstash/ELK compatible)</li>
 *   <li>Trace ID and Span ID injection in logs (correlation)</li>
 *   <li>MDC (Mapped Diagnostic Context) support</li>
 *   <li>Contextual logging (user, request ID, etc.)</li>
 * </ul>
 * <p>
 * For Spring Boot 3.4+ users who prefer native ECS logging, use {@code observability.logging.format=ecs}.
 *
 * @see EcsLoggingConfiguration
 */
@Configuration
@ConditionalOnExpression(
        "${observability.logging.json-enabled:true} && !'ecs'.equals('${observability.logging.format:json}')"
)
@Slf4j
public class LoggingConfiguration {

    public LoggingConfiguration(ObservabilityProperties properties) {
        ObservabilityProperties.Logging loggingConfig = properties.getLogging();

        log.info("✅ Structured JSON logging enabled (Logstash format)");
        log.info("   - Format: Logstash JSON");
        log.info("   - Trace ID in logs: {}", loggingConfig.isIncludeTraceId());
        log.info("   - Span ID in logs: {}", loggingConfig.isIncludeSpanId());
        log.info("   - MDC in logs: {}", loggingConfig.isIncludeMdc());
        log.info("   - Log level: {}", loggingConfig.getLevel());
    }
}
