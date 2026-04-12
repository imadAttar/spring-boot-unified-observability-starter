package com.imadattar.observability.metrics;

import com.imadattar.observability.config.ObservabilityProperties;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import java.time.Duration;
import java.util.Arrays;

/**
 * Configuration for HTTP metrics in reactive (WebFlux) applications.
 *
 * Provides the same percentile and SLO bucket configuration as
 * {@link HttpMetricsConfiguration} but activates for WebFlux applications
 * instead of Servlet-based ones.
 *
 * @since 1.3.0
 * @see HttpMetricsConfiguration
 */
@Configuration
@ConditionalOnProperty(prefix = "observability.metrics", name = "http-enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(WebFluxConfigurer.class)
@RequiredArgsConstructor
@Slf4j
public class ReactiveHttpMetricsConfiguration {

    private final ObservabilityProperties properties;

    /**
     * Customize HTTP metrics with percentiles and histogram buckets for WebFlux.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> reactiveHttpMetricsCustomizer() {
        double[] sloMillis = properties.getMetrics().getHttpSloMillis();
        log.info("Configuring reactive HTTP metrics with percentiles (p50, p95, p99) and {} SLO buckets", sloMillis.length);

        double[] sloNanos = Arrays.stream(sloMillis)
            .map(millis -> Duration.ofMillis((long) millis).toNanos())
            .toArray();

        return registry -> {
            registry.config().meterFilter(
                new MeterFilter() {
                    @Override
                    public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                        if (id.getName().startsWith("http.server.requests")) {
                            return DistributionStatisticConfig.builder()
                                .percentiles(0.5, 0.95, 0.99)
                                .serviceLevelObjectives(sloNanos)
                                .build()
                                .merge(config);
                        }
                        return config;
                    }
                }
            );
        };
    }
}
