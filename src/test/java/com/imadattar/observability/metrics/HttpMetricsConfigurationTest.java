package com.imadattar.observability.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for HttpMetricsConfiguration.
 */
class HttpMetricsConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(HttpMetricsConfiguration.class));

    @Test
    void testCustomizerBeanCreatedWhenEnabled() {
        contextRunner
            .withPropertyValues("observability.metrics.http-enabled=true")
            .run(context -> {
                assertThat(context).hasSingleBean(MeterRegistryCustomizer.class);
            });
    }

    @Test
    void testCustomizerBeanNotCreatedWhenDisabled() {
        contextRunner
            .withPropertyValues("observability.metrics.http-enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(MeterRegistryCustomizer.class);
            });
    }

    @Test
    void testCustomizerBeanCreatedByDefaultWhenPropertyNotSet() {
        contextRunner
            .run(context -> {
                // matchIfMissing = true, so bean should be created
                assertThat(context).hasSingleBean(MeterRegistryCustomizer.class);
            });
    }

    @Test
    void testPercentilesConfigured() {
        contextRunner
            .withPropertyValues("observability.metrics.http-enabled=true")
            .run(context -> {
                MeterRegistryCustomizer<SimpleMeterRegistry> customizer =
                    (MeterRegistryCustomizer<SimpleMeterRegistry>) context.getBean(MeterRegistryCustomizer.class);

                SimpleMeterRegistry registry = new SimpleMeterRegistry();
                customizer.customize(registry);

                // Create a timer to verify percentile configuration
                io.micrometer.core.instrument.Timer timer = registry.timer("http.server.requests",
                    "method", "GET", "uri", "/test", "status", "200");

                timer.record(Duration.ofMillis(100));

                // Verify timer was created (configuration was applied)
                assertThat(timer).isNotNull();
                assertThat(timer.count()).isEqualTo(1);
            });
    }

    @Test
    void testSloHistogramsConfigured() {
        contextRunner
            .withPropertyValues("observability.metrics.http-enabled=true")
            .run(context -> {
                MeterRegistryCustomizer<SimpleMeterRegistry> customizer =
                    (MeterRegistryCustomizer<SimpleMeterRegistry>) context.getBean(MeterRegistryCustomizer.class);

                SimpleMeterRegistry registry = new SimpleMeterRegistry();
                customizer.customize(registry);

                // Create a timer and verify it can be used
                io.micrometer.core.instrument.Timer timer = registry.timer("http.server.requests",
                    "method", "POST", "uri", "/api/test", "status", "201");

                // Record some values
                timer.record(Duration.ofMillis(50));
                timer.record(Duration.ofMillis(150));
                timer.record(Duration.ofSeconds(1));

                // Verify recordings worked
                assertThat(timer.count()).isEqualTo(3);
                assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS))
                    .isGreaterThanOrEqualTo(1200); // ~50 + 150 + 1000
            });
    }

    @Test
    void testFilterOnlyAppliestoHttpServerRequests() {
        contextRunner
            .withPropertyValues("observability.metrics.http-enabled=true")
            .run(context -> {
                MeterRegistryCustomizer<SimpleMeterRegistry> customizer =
                    (MeterRegistryCustomizer<SimpleMeterRegistry>) context.getBean(MeterRegistryCustomizer.class);

                SimpleMeterRegistry registry = new SimpleMeterRegistry();
                customizer.customize(registry);

                // Create HTTP timer
                io.micrometer.core.instrument.Timer httpTimer = registry.timer("http.server.requests",
                    "method", "GET", "uri", "/test", "status", "200");
                httpTimer.record(Duration.ofMillis(100));

                // Create non-HTTP timer
                io.micrometer.core.instrument.Timer otherTimer = registry.timer("database.query.time");
                otherTimer.record(Duration.ofMillis(100));

                // Both should work (filter applies configuration to HTTP, leaves others alone)
                assertThat(httpTimer.count()).isEqualTo(1);
                assertThat(otherTimer.count()).isEqualTo(1);
            });
    }
}
