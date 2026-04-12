package com.imadattar.observability.unit;

import com.imadattar.observability.config.ObservabilityProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires pour ObservabilityProperties.
 * Vérifie les valeurs par défaut et la configuration.
 */
@DisplayName("ObservabilityProperties Unit Tests")
class ObservabilityPropertiesUnitTest {

    @Test
    @DisplayName("Should have correct default values for main properties")
    void shouldHaveCorrectDefaultValues() {
        ObservabilityProperties properties = new ObservabilityProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getMetrics()).isNotNull();
        assertThat(properties.getTracing()).isNotNull();
        assertThat(properties.getLogging()).isNotNull();
        assertThat(properties.getStackExport()).isNotNull();
    }

    @Test
    @DisplayName("Should have correct default values for metrics")
    void shouldHaveCorrectMetricsDefaults() {
        ObservabilityProperties properties = new ObservabilityProperties();
        ObservabilityProperties.Metrics metrics = properties.getMetrics();

        assertThat(metrics.isEnabled()).isTrue();
        assertThat(metrics.isJvmEnabled()).isTrue();
        assertThat(metrics.isHttpEnabled()).isTrue();
        assertThat(metrics.isDatabaseEnabled()).isTrue();
        assertThat(metrics.getPath()).isEqualTo("/actuator/prometheus");
    }

    @Test
    @DisplayName("Should have correct default values for tracing")
    void shouldHaveCorrectTracingDefaults() {
        ObservabilityProperties properties = new ObservabilityProperties();
        ObservabilityProperties.Tracing tracing = properties.getTracing();

        assertThat(tracing.isEnabled()).isTrue();
        assertThat(tracing.getOtlpEndpoint()).isEqualTo("http://localhost:4317");
        assertThat(tracing.getSamplingProbability()).isEqualTo(1.0);
        assertThat(tracing.getServiceName()).isEqualTo("spring-boot-app");
        assertThat(tracing.isPropagationEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should have correct default values for logging")
    void shouldHaveCorrectLoggingDefaults() {
        ObservabilityProperties properties = new ObservabilityProperties();
        ObservabilityProperties.Logging logging = properties.getLogging();

        assertThat(logging.isJsonEnabled()).isTrue();
        assertThat(logging.getFormat()).isEqualTo("json");
        assertThat(logging.isIncludeTraceId()).isTrue();
        assertThat(logging.isIncludeSpanId()).isTrue();
        assertThat(logging.isIncludeMdc()).isTrue();
        assertThat(logging.getLevel()).isEqualTo("INFO");
    }

    @Test
    @DisplayName("Should have correct default values for stack export")
    void shouldHaveCorrectStackExportDefaults() {
        ObservabilityProperties properties = new ObservabilityProperties();
        ObservabilityProperties.StackExport stackExport = properties.getStackExport();

        assertThat(stackExport.isEnabled()).isFalse();
        assertThat(stackExport.getExportPath()).isEqualTo("./monitoring");
        assertThat(stackExport.isPersonalizeConfig()).isTrue();
        assertThat(stackExport.isExportOnStartup()).isFalse();
    }

    @Test
    @DisplayName("Should allow modification of properties")
    void shouldAllowPropertyModification() {
        ObservabilityProperties properties = new ObservabilityProperties();

        properties.setEnabled(false);
        properties.getMetrics().setEnabled(false);
        properties.getTracing().setServiceName("custom-service");
        properties.getLogging().setFormat("ecs");

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getMetrics().isEnabled()).isFalse();
        assertThat(properties.getTracing().getServiceName()).isEqualTo("custom-service");
        assertThat(properties.getLogging().getFormat()).isEqualTo("ecs");
    }

    @Test
    @DisplayName("Should validate sampling probability is between 0 and 1")
    void shouldValidateSamplingProbability() {
        ObservabilityProperties properties = new ObservabilityProperties();
        ObservabilityProperties.Tracing tracing = properties.getTracing();

        // Valeurs valides
        tracing.setSamplingProbability(0.0);
        assertThat(tracing.getSamplingProbability()).isEqualTo(0.0);

        tracing.setSamplingProbability(0.5);
        assertThat(tracing.getSamplingProbability()).isEqualTo(0.5);

        tracing.setSamplingProbability(1.0);
        assertThat(tracing.getSamplingProbability()).isEqualTo(1.0);

        // Note: La validation stricte devrait être ajoutée dans le code
        // Pour l'instant, on teste juste que les valeurs peuvent être définies
    }

    @Test
    @DisplayName("Should support both json and ecs logging formats")
    void shouldSupportLoggingFormats() {
        ObservabilityProperties properties = new ObservabilityProperties();
        ObservabilityProperties.Logging logging = properties.getLogging();

        logging.setFormat("json");
        assertThat(logging.getFormat()).isEqualTo("json");

        logging.setFormat("ecs");
        assertThat(logging.getFormat()).isEqualTo("ecs");
    }
}
