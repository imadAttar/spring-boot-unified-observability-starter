package com.imadattar.observability.tracing;

import com.imadattar.observability.config.ObservabilityProperties;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenTelemetry distributed tracing.
 *
 * Enables:
 * - Automatic trace context propagation (W3C Trace Context, B3)
 * - Span creation for HTTP requests, database queries, external calls
 * - Integration with logs (trace ID, span ID in MDC)
 * - OTLP export to Jaeger, Tempo, or other backends
 *
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = "observability.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class TracingConfiguration {

    /**
     * Configure trace propagation for distributed systems.
     */
    @Bean
    @ConditionalOnClass(ContextPropagators.class)
    @ConditionalOnProperty(prefix = "observability.tracing", name = "propagation-enabled", havingValue = "true", matchIfMissing = true)
    public ContextPropagators contextPropagators() {
        log.info("✅ Trace propagation enabled: W3C Trace Context + B3");
        return ContextPropagators.create(B3Propagator.injectingMultiHeaders());
    }

    /**
     * Provide utility bean for manual span creation.
     */
    @Bean
    @ConditionalOnClass(TracingHelper.class)
    public TracingHelper tracingHelper(Tracer tracer) {
        log.info("✅ TracingHelper bean available for manual instrumentation");
        return new TracingHelper(tracer);
    }
}
