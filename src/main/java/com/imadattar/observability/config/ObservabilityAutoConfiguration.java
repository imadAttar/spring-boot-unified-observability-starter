package com.imadattar.observability.config;

import com.imadattar.observability.metrics.MetricsConfiguration;
import com.imadattar.observability.tracing.TracingConfiguration;
import com.imadattar.observability.logging.LoggingConfiguration;
import com.imadattar.observability.export.ObservabilityStackExportService;
import com.imadattar.observability.export.ObservabilityStackAutoExporter;
import com.imadattar.observability.export.ObservabilityStackExportController;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for Unified Observability Starter.
 *
 * Configures:
 * - Prometheus metrics (Micrometer)
 * - Distributed tracing (OpenTelemetry)
 * - Structured JSON logging (Logstash encoder)
 * - Grafana dashboard integration
 */
@AutoConfiguration
@EnableConfigurationProperties(ObservabilityProperties.class)
@ConditionalOnProperty(prefix = "observability", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({
    MetricsConfiguration.class,
    TracingConfiguration.class,
    LoggingConfiguration.class,
    ObservabilityStackExportService.class,
    ObservabilityStackAutoExporter.class,
    ObservabilityStackExportController.class
})
@Slf4j
public class ObservabilityAutoConfiguration {

    /**
     * Configure Prometheus metrics registry.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(PrometheusMeterRegistry.class)
    @ConditionalOnProperty(prefix = "observability.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
    public PrometheusMeterRegistry prometheusMeterRegistry() {
        log.info("🔧 Configuring Prometheus metrics registry");
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }

    /**
     * Customize meter registry with common tags.
     */
    @Bean
    @ConditionalOnProperty(prefix = "observability.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(ObservabilityProperties properties) {
        return registry -> {
            registry.config().commonTags(
                "service", properties.getTracing().getServiceName(),
                "environment", System.getProperty("spring.profiles.active", "default")
            );
            log.info("✅ Metrics common tags configured: service={}, environment={}",
                properties.getTracing().getServiceName(),
                System.getProperty("spring.profiles.active", "default"));
        };
    }

    /**
     * Configure OpenTelemetry SDK for distributed tracing.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(OpenTelemetry.class)
    @ConditionalOnProperty(prefix = "observability.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
    public OpenTelemetry openTelemetry(ObservabilityProperties properties) {
        log.info("🔧 Configuring OpenTelemetry distributed tracing");

        ObservabilityProperties.Tracing tracingConfig = properties.getTracing();

        // Configure OTLP exporter
        OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
            .setEndpoint(tracingConfig.getOtlpEndpoint())
            .build();

        // Configure tracer provider with sampling
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
            .setSampler(Sampler.traceIdRatioBased(tracingConfig.getSamplingProbability()))
            .build();

        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .build();

        log.info("✅ OpenTelemetry configured: endpoint={}, sampling={}",
            tracingConfig.getOtlpEndpoint(),
            tracingConfig.getSamplingProbability());

        return openTelemetry;
    }

    /**
     * Provide Tracer bean for manual instrumentation.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(Tracer.class)
    @ConditionalOnProperty(prefix = "observability.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
    public Tracer tracer(OpenTelemetry openTelemetry, ObservabilityProperties properties) {
        return openTelemetry.getTracer(properties.getTracing().getServiceName());
    }
}
