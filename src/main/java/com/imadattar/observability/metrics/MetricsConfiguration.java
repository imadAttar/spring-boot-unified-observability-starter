package com.imadattar.observability.metrics;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration for Micrometer metrics.
 *
 * Enables:
 * - @Timed annotation support (method-level timing metrics)
 * - @Counted annotation support (method-level counters)
 * - JVM metrics (memory, GC, threads) via JvmMetricsConfiguration
 * - HTTP metrics (requests, responses, latency) via HttpMetricsConfiguration
 * - Database metrics (connection pool, query performance) via DatabaseMetricsConfiguration
 * - Custom business metrics examples via CustomMetricsExamples
 *
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = "observability.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({
    JvmMetricsConfiguration.class,
    HttpMetricsConfiguration.class,
    DatabaseMetricsConfiguration.class
})
@Slf4j
public class MetricsConfiguration {

    /**
     * Enable @Timed annotation support for method-level metrics.
     */
    @Bean
    @ConditionalOnClass(TimedAspect.class)
    public TimedAspect timedAspect(MeterRegistry registry) {
        log.info("✅ @Timed annotation support enabled");
        return new TimedAspect(registry);
    }
}
