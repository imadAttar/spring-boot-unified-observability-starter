package com.imadattar.observability.health;

import com.imadattar.observability.config.ObservabilityProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Health indicator that reports the status of observability components.
 * <p>
 * Reports:
 * - Whether metrics collection is active and meter count
 * - Tracing configuration (service name, sampling, endpoint)
 * - Logging format and trace correlation status
 * - Stack export status
 *
 * @since 1.3.0
 */
@Component
@ConditionalOnClass(HealthIndicator.class)
@ConditionalOnBean(ObservabilityProperties.class)
@ConditionalOnProperty(prefix = "observability", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ObservabilityHealthIndicator implements HealthIndicator {

    private final MeterRegistry meterRegistry;
    private final ObservabilityProperties properties;

    @Autowired
    public ObservabilityHealthIndicator(
            @Autowired(required = false) MeterRegistry meterRegistry,
            ObservabilityProperties properties) {
        this.meterRegistry = meterRegistry;
        this.properties = properties;
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.up();

        // Metrics status
        boolean metricsEnabled = properties.getMetrics().isEnabled();
        builder.withDetail("metrics.enabled", metricsEnabled);
        if (metricsEnabled && meterRegistry != null) {
            builder.withDetail("metrics.registeredMeters", meterRegistry.getMeters().size());
        }

        // Tracing status
        boolean tracingEnabled = properties.getTracing().isEnabled();
        builder.withDetail("tracing.enabled", tracingEnabled);
        if (tracingEnabled) {
            builder.withDetail("tracing.serviceName", properties.getTracing().getServiceName());
            builder.withDetail("tracing.samplingProbability", properties.getTracing().getSamplingProbability());
            builder.withDetail("tracing.otlpEndpoint", properties.getTracing().getOtlpEndpoint());
        }

        // Logging status
        builder.withDetail("logging.format", properties.getLogging().getFormat());
        builder.withDetail("logging.traceCorrelation", properties.getLogging().isIncludeTraceId());

        // Stack export status
        builder.withDetail("stackExport.enabled", properties.getStackExport().isEnabled());

        return builder.build();
    }
}
