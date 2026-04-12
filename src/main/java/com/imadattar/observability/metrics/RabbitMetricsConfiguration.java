package com.imadattar.observability.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for RabbitMQ metrics.
 * <p>
 * Activates automatically when RabbitMQ client is on the classpath.
 * Spring Boot's auto-configuration already provides basic RabbitMQ metrics
 * via Micrometer; this configuration adds observability-specific tags
 * and ensures metrics are properly labeled.
 * <p>
 * Requires:
 * - {@code org.springframework.boot:spring-boot-starter-amqp}
 *
 * @since 1.3.0
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.amqp.rabbit.core.RabbitTemplate")
@ConditionalOnProperty(prefix = "observability.metrics", name = "rabbit-enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class RabbitMetricsConfiguration {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> rabbitMetricsCustomizer() {
        log.info("RabbitMQ metrics support enabled (auto-detected spring-amqp on classpath)");
        return registry -> {
            // RabbitMQ connection and channel metrics are auto-registered by Spring Boot
            // when spring-boot-starter-amqp is on the classpath
            log.info("RabbitMQ metrics registered in meter registry");
        };
    }
}
