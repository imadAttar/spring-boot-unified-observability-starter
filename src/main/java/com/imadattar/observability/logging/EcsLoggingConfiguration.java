package com.imadattar.observability.logging;

import com.imadattar.observability.config.ObservabilityProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Configuration for Elastic Common Schema (ECS) structured logging.
 * <p>
 * This configuration activates when {@code observability.logging.format=ecs}.
 * It leverages Spring Boot 3.4+ native structured logging support to provide
 * ECS-formatted JSON logs without requiring external dependencies.
 * <p>
 * ECS format provides:
 * <ul>
 *   <li>Standardized log structure for Elastic Stack</li>
 *   <li>Automatic trace/span ID correlation</li>
 *   <li>Service metadata (name, version, environment)</li>
 *   <li>Better integration with Elasticsearch and Kibana</li>
 * </ul>
 * <p>
 * The actual configuration is performed by {@link EcsLoggingEnvironmentPostProcessor}
 * which runs early in the Spring Boot lifecycle to configure logging properties before
 * the logging system is initialized.
 * <p>
 * This configuration bean primarily serves to log the ECS configuration status
 * and provide a Spring bean for potential customization.
 *
 * @see EcsLoggingEnvironmentPostProcessor
 * @see <a href="https://docs.spring.io/spring-boot/reference/features/logging.html#features.logging.structured">Spring Boot Structured Logging</a>
 * @see <a href="https://www.elastic.co/guide/en/ecs/current/index.html">Elastic Common Schema</a>
 */
@Configuration
@ConditionalOnExpression(
        "${observability.logging.json-enabled:true} && 'ecs'.equals('${observability.logging.format:json}')"
)
@Slf4j
public class EcsLoggingConfiguration {

    public EcsLoggingConfiguration(ObservabilityProperties properties, Environment environment) {
        ObservabilityProperties.Logging loggingConfig = properties.getLogging();

        String serviceName = environment.getProperty("spring.application.name", "spring-boot-app");
        String serviceEnvironment = environment.getProperty("spring.profiles.active", "default");

        log.info("✅ ECS structured logging enabled (Spring Boot 3.4+ native)");
        log.info("   - Format: Elastic Common Schema (ECS)");
        log.info("   - Service: {}", serviceName);
        log.info("   - Environment: {}", serviceEnvironment);
        log.info("   - Trace ID in logs: {}", loggingConfig.isIncludeTraceId());
        log.info("   - Span ID in logs: {}", loggingConfig.isIncludeSpanId());
    }
}
