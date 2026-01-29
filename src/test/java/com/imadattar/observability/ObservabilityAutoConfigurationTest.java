package com.imadattar.observability;

import com.imadattar.observability.config.ObservabilityAutoConfiguration;
import com.imadattar.observability.config.ObservabilityProperties;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for ObservabilityAutoConfiguration.
 */
class ObservabilityAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ObservabilityAutoConfiguration.class));

    @Test
    void shouldLoadAutoConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ObservabilityProperties.class);
        });
    }

    @Test
    void shouldConfigurePrometheusMetrics() {
        contextRunner
            .withPropertyValues("observability.metrics.enabled=true")
            .run(context -> {
                assertThat(context).hasSingleBean(PrometheusMeterRegistry.class);
            });
    }

    @Test
    void shouldConfigureOpenTelemetry() {
        contextRunner
            .withPropertyValues("observability.tracing.enabled=true")
            .run(context -> {
                assertThat(context).hasSingleBean(OpenTelemetry.class);
                assertThat(context).hasSingleBean(Tracer.class);
            });
    }

    @Test
    void shouldDisableMetricsWhenPropertyIsFalse() {
        contextRunner
            .withPropertyValues("observability.metrics.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(PrometheusMeterRegistry.class);
            });
    }

    @Test
    void shouldDisableTracingWhenPropertyIsFalse() {
        contextRunner
            .withPropertyValues("observability.tracing.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(OpenTelemetry.class);
            });
    }
}
