package com.imadattar.observability.config;

import com.imadattar.observability.metrics.MetricsConfiguration;
import com.imadattar.observability.tracing.TracingConfiguration;
import com.imadattar.observability.logging.LoggingConfiguration;
import com.imadattar.observability.logging.EcsLoggingConfiguration;
import com.imadattar.observability.export.ObservabilityStackExportService;
import com.imadattar.observability.export.ObservabilityStackAutoExporter;
import com.imadattar.observability.export.ObservabilityStackExportController;
import com.imadattar.observability.health.ObservabilityHealthIndicator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
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
import org.springframework.core.env.Environment;

import java.time.Duration;

/**
 * Auto-configuration for Unified Observability Starter.
 *
 * Configures:
 * - Prometheus metrics (Micrometer)
 * - Distributed tracing (OpenTelemetry)
 * - Structured logging (JSON/ECS)
 * - Monitoring stack export
 *
 * All OTel components are exposed as proper Spring beans with managed lifecycle.
 *
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(ObservabilityProperties.class)
@ConditionalOnProperty(prefix = "observability", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({
    MetricsConfiguration.class,
    TracingConfiguration.class,
    LoggingConfiguration.class,
    EcsLoggingConfiguration.class,
    ObservabilityStackExportService.class,
    ObservabilityStackAutoExporter.class,
    ObservabilityStackExportController.class,
    ObservabilityHealthIndicator.class
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
        log.info("Configuring Prometheus metrics registry");
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }

    /**
     * Customize meter registry with common tags.
     */
    @Bean
    @ConditionalOnProperty(prefix = "observability.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(
            ObservabilityProperties properties,
            Environment environment) {
        String[] activeProfiles = environment.getActiveProfiles();
        String activeEnvironment = activeProfiles.length > 0 ? activeProfiles[0] : "default";

        return registry -> {
            registry.config().commonTags(
                "service", properties.getTracing().getServiceName(),
                "environment", activeEnvironment
            );
            log.info("Metrics common tags configured: service={}, environment={}",
                properties.getTracing().getServiceName(),
                activeEnvironment);
        };
    }

    /**
     * Configure OTLP span exporter as a standalone bean.
     * Spring manages its lifecycle (closed on context shutdown).
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(SpanExporter.class)
    @ConditionalOnClass(OtlpGrpcSpanExporter.class)
    @ConditionalOnProperty(prefix = "observability.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
    public OtlpGrpcSpanExporter otlpGrpcSpanExporter(ObservabilityProperties properties) {
        ObservabilityProperties.Tracing tracingConfig = properties.getTracing();
        log.info("Configuring OTLP span exporter: endpoint={}", tracingConfig.getOtlpEndpoint());

        return OtlpGrpcSpanExporter.builder()
            .setEndpoint(tracingConfig.getOtlpEndpoint())
            .setTimeout(Duration.ofSeconds(tracingConfig.getTimeoutSeconds()))
            .setConnectTimeout(Duration.ofSeconds(tracingConfig.getConnectTimeoutSeconds()))
            .build();
    }

    /**
     * Configure tracer provider as a standalone bean.
     * Spring manages its lifecycle (closed on context shutdown).
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    @ConditionalOnClass(SdkTracerProvider.class)
    @ConditionalOnProperty(prefix = "observability.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
    public SdkTracerProvider sdkTracerProvider(
            SpanExporter spanExporter,
            ObservabilityProperties properties) {
        ObservabilityProperties.Tracing tracingConfig = properties.getTracing();
        log.info("Configuring tracer provider: sampling={}", tracingConfig.getSamplingProbability());

        return SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
            .setSampler(Sampler.traceIdRatioBased(tracingConfig.getSamplingProbability()))
            .build();
    }

    /**
     * Configure OpenTelemetry SDK with the tracer provider and context propagators.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(OpenTelemetry.class)
    @ConditionalOnProperty(prefix = "observability.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
    public OpenTelemetry openTelemetry(
            SdkTracerProvider tracerProvider,
            ContextPropagators contextPropagators) {
        log.info("Configuring OpenTelemetry SDK");

        return OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setPropagators(contextPropagators)
            .build();
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
