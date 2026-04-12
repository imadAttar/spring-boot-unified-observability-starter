package com.imadattar.observability.health;

import com.imadattar.observability.config.ObservabilityProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilityHealthIndicatorTest {

    @Test
    void shouldReturnUpWithAllDetails() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ObservabilityProperties properties = new ObservabilityProperties();

        ObservabilityHealthIndicator indicator = new ObservabilityHealthIndicator(registry, properties);
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("metrics.enabled");
        assertThat(health.getDetails()).containsKey("tracing.enabled");
        assertThat(health.getDetails()).containsKey("logging.format");
        assertThat(health.getDetails()).containsKey("stackExport.enabled");
    }

    @Test
    void shouldIncludeMetricsCountWhenEnabled() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        registry.counter("test.counter").increment();
        ObservabilityProperties properties = new ObservabilityProperties();
        properties.getMetrics().setEnabled(true);

        ObservabilityHealthIndicator indicator = new ObservabilityHealthIndicator(registry, properties);
        Health health = indicator.health();

        assertThat(health.getDetails().get("metrics.enabled")).isEqualTo(true);
        assertThat(health.getDetails().get("metrics.registeredMeters")).isNotNull();
        assertThat((int) health.getDetails().get("metrics.registeredMeters")).isGreaterThan(0);
    }

    @Test
    void shouldIncludeTracingDetailsWhenEnabled() {
        ObservabilityProperties properties = new ObservabilityProperties();
        properties.getTracing().setEnabled(true);
        properties.getTracing().setServiceName("test-service");
        properties.getTracing().setSamplingProbability(0.5);

        ObservabilityHealthIndicator indicator = new ObservabilityHealthIndicator(new SimpleMeterRegistry(), properties);
        Health health = indicator.health();

        assertThat(health.getDetails().get("tracing.enabled")).isEqualTo(true);
        assertThat(health.getDetails().get("tracing.serviceName")).isEqualTo("test-service");
        assertThat(health.getDetails().get("tracing.samplingProbability")).isEqualTo(0.5);
    }

    @Test
    void shouldOmitTracingDetailsWhenDisabled() {
        ObservabilityProperties properties = new ObservabilityProperties();
        properties.getTracing().setEnabled(false);

        ObservabilityHealthIndicator indicator = new ObservabilityHealthIndicator(new SimpleMeterRegistry(), properties);
        Health health = indicator.health();

        assertThat(health.getDetails().get("tracing.enabled")).isEqualTo(false);
        assertThat(health.getDetails()).doesNotContainKey("tracing.serviceName");
    }

    @Test
    void shouldWorkWithNullMeterRegistry() {
        ObservabilityProperties properties = new ObservabilityProperties();
        properties.getMetrics().setEnabled(true);

        ObservabilityHealthIndicator indicator = new ObservabilityHealthIndicator(null, properties);
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails().get("metrics.enabled")).isEqualTo(true);
        assertThat(health.getDetails()).doesNotContainKey("metrics.registeredMeters");
    }

    @Test
    void shouldIncludeLoggingFormat() {
        ObservabilityProperties properties = new ObservabilityProperties();
        properties.getLogging().setFormat("ecs");

        ObservabilityHealthIndicator indicator = new ObservabilityHealthIndicator(new SimpleMeterRegistry(), properties);
        Health health = indicator.health();

        assertThat(health.getDetails().get("logging.format")).isEqualTo("ecs");
    }
}
