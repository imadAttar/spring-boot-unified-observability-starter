package com.imadattar.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the complete observability stack.
 * Tests end-to-end functionality of metrics, tracing, and logging.
 */
@SpringBootTest(
    classes = TestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "observability.enabled=true",
        "observability.metrics.enabled=true",
        "observability.tracing.enabled=true",
        "observability.logging.enabled=true",
        "management.endpoints.web.exposure.include=health,prometheus,metrics",
        "management.endpoint.health.show-details=always"
    }
)
@AutoConfigureObservability
class ObservabilityIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void testMetricsRegistryContainsJvmMetrics() {
        // Then
        assertThat(meterRegistry.find("jvm.memory.used").gauges()).isNotEmpty();
        assertThat(meterRegistry.find("jvm.threads.live").gauge()).isNotNull();
        assertThat(meterRegistry.find("system.cpu.count").gauge()).isNotNull();
    }

    @Test
    void testHealthEndpointReturnsUp() {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/actuator/health",
            String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void testMetricsEndpointAvailable() {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/actuator/metrics",
            String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("names");
    }

    @Test
    void testSpecificMetricEndpoint() {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/actuator/metrics/jvm.memory.used",
            String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"name\":\"jvm.memory.used\"");
        assertThat(response.getBody()).contains("measurements");
    }

    @Test
    void testCommonTagsApplied() {
        // Then - verify common tags are present in metrics
        assertThat(meterRegistry.find("jvm.memory.used").gauges())
            .isNotEmpty()
            .allMatch(gauge -> gauge.getId().getTags().stream()
                .anyMatch(tag -> tag.getKey().equals("service") || tag.getKey().equals("environment")));
    }
}
