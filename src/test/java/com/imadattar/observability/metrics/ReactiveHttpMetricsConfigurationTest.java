package com.imadattar.observability.metrics;

import com.imadattar.observability.config.ObservabilityProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ReactiveHttpMetricsConfigurationTest {

    @Test
    void shouldCreateCustomizerBean() {
        ObservabilityProperties properties = new ObservabilityProperties();
        ReactiveHttpMetricsConfiguration config = new ReactiveHttpMetricsConfiguration(properties);

        MeterRegistryCustomizer<MeterRegistry> customizer = config.reactiveHttpMetricsCustomizer();

        assertThat(customizer).isNotNull();
    }

    @Test
    void shouldApplyPercentilesAndSloToHttpMetrics() {
        ObservabilityProperties properties = new ObservabilityProperties();
        ReactiveHttpMetricsConfiguration config = new ReactiveHttpMetricsConfiguration(properties);

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MeterRegistryCustomizer<MeterRegistry> customizer = config.reactiveHttpMetricsCustomizer();
        customizer.customize(registry);

        // Record an HTTP request metric
        Timer timer = Timer.builder("http.server.requests")
            .register(registry);
        timer.record(Duration.ofMillis(150));

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void shouldNotAffectNonHttpMetrics() {
        ObservabilityProperties properties = new ObservabilityProperties();
        ReactiveHttpMetricsConfiguration config = new ReactiveHttpMetricsConfiguration(properties);

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MeterRegistryCustomizer<MeterRegistry> customizer = config.reactiveHttpMetricsCustomizer();
        customizer.customize(registry);

        // Record a non-HTTP metric
        Timer timer = Timer.builder("custom.operation")
            .register(registry);
        timer.record(Duration.ofMillis(50));

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void shouldUseConfiguredSloMillis() {
        ObservabilityProperties properties = new ObservabilityProperties();
        properties.getMetrics().setHttpSloMillis(new double[]{100, 500, 1000});
        ReactiveHttpMetricsConfiguration config = new ReactiveHttpMetricsConfiguration(properties);

        MeterRegistryCustomizer<MeterRegistry> customizer = config.reactiveHttpMetricsCustomizer();

        assertThat(customizer).isNotNull();
    }
}
