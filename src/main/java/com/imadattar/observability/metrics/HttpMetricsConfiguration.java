package com.imadattar.observability.metrics;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;

import java.time.Duration;

/**
 * Configuration for HTTP metrics with enhanced histogram and percentile support.
 *
 * Configures:
 * - Percentiles: p50, p95, p99 for latency analysis
 * - SLO buckets: 50ms, 100ms, 200ms, 500ms, 1s, 2s, 5s
 * - Applies to all http.server.requests metrics
 */
@Configuration
@ConditionalOnProperty(prefix = "observability.metrics", name = "http-enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(DispatcherServlet.class)
@Slf4j
public class HttpMetricsConfiguration {

    /**
     * Customize HTTP metrics with percentiles and histogram buckets.
     * This enables detailed latency analysis in Prometheus/Grafana.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> httpMetricsCustomizer() {
        log.info("✅ Configuring HTTP metrics with percentiles (p50, p95, p99) and SLO buckets");

        return registry -> {
            registry.config().meterFilter(
                new MeterFilter() {
                    @Override
                    public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                        if (id.getName().startsWith("http.server.requests")) {
                            return DistributionStatisticConfig.builder()
                                .percentiles(0.5, 0.95, 0.99)
                                .serviceLevelObjectives(
                                    Duration.ofMillis(50).toNanos(),
                                    Duration.ofMillis(100).toNanos(),
                                    Duration.ofMillis(200).toNanos(),
                                    Duration.ofMillis(500).toNanos(),
                                    Duration.ofSeconds(1).toNanos(),
                                    Duration.ofSeconds(2).toNanos(),
                                    Duration.ofSeconds(5).toNanos()
                                )
                                .build()
                                .merge(config);
                        }
                        return config;
                    }
                }
            );

            log.info("✅ HTTP metrics configured: latency percentiles, status codes, SLO buckets");
        };
    }
}
