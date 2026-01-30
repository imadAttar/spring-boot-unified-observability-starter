package com.imadattar.observability.config;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Unified Observability Starter.
 */
@Data
@Validated
@ConfigurationProperties(prefix = "observability")
public class ObservabilityProperties {

    /**
     * Enable/disable observability features globally.
     */
    private boolean enabled = true;

    /**
     * Metrics configuration.
     */
    private Metrics metrics = new Metrics();

    /**
     * Tracing configuration.
     */
    private Tracing tracing = new Tracing();

    /**
     * Logging configuration.
     */
    private Logging logging = new Logging();

    /**
     * Grafana configuration.
     */
    private Grafana grafana = new Grafana();

    /**
     * Stack export configuration.
     */
    private StackExport stackExport = new StackExport();

    @Data
    public static class Metrics {
        /**
         * Enable Prometheus metrics.
         */
        private boolean enabled = true;

        /**
         * Metrics export path.
         */
        private String path = "/actuator/prometheus";

        /**
         * Enable JVM metrics.
         */
        private boolean jvmEnabled = true;

        /**
         * Enable HTTP metrics.
         */
        private boolean httpEnabled = true;

        /**
         * Enable database metrics.
         */
        private boolean databaseEnabled = true;

        /**
         * Enable custom business metrics.
         */
        private boolean customEnabled = true;
    }

    @Data
    public static class Tracing {
        /**
         * Enable distributed tracing.
         */
        private boolean enabled = true;

        /**
         * OpenTelemetry exporter endpoint.
         */
        @NotBlank(message = "OTLP endpoint cannot be blank")
        private String otlpEndpoint = "http://localhost:4318/v1/traces";

        /**
         * Sampling probability (0.0 to 1.0).
         */
        @DecimalMin(value = "0.0", message = "Sampling probability must be between 0.0 and 1.0")
        @DecimalMax(value = "1.0", message = "Sampling probability must be between 0.0 and 1.0")
        private double samplingProbability = 1.0;

        /**
         * Service name for tracing.
         */
        @NotBlank(message = "Service name cannot be blank")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Service name must contain only alphanumeric characters, hyphens, and underscores")
        private String serviceName = "spring-boot-app";

        /**
         * Enable trace propagation in HTTP headers.
         */
        private boolean propagationEnabled = true;
    }

    @Data
    public static class Logging {
        /**
         * Enable structured JSON logging.
         */
        private boolean jsonEnabled = true;

        /**
         * Logging format.
         * <p>
         * Supported formats:
         * <ul>
         *   <li>{@code json} - Logstash JSON format (default, backward compatible)</li>
         *   <li>{@code ecs} - Elastic Common Schema (requires Spring Boot 3.4+)</li>
         * </ul>
         * <p>
         * The ECS format uses Spring Boot 3.4+ native structured logging support,
         * which provides better integration with Elastic Stack and reduces external dependencies.
         * <p>
         * Default: {@code json}
         */
        private String format = "json";

        /**
         * Include trace ID in logs.
         */
        private boolean includeTraceId = true;

        /**
         * Include span ID in logs.
         */
        private boolean includeSpanId = true;

        /**
         * Include MDC (Mapped Diagnostic Context) in logs.
         */
        private boolean includeMdc = true;

        /**
         * Log level.
         */
        private String level = "INFO";
    }

    @Data
    public static class Grafana {
        /**
         * Enable Grafana dashboard auto-import.
         */
        private boolean enabled = true;

        /**
         * Grafana server URL.
         */
        private String url = "http://localhost:3000";

        /**
         * Grafana API key.
         */
        private String apiKey;

        /**
         * Auto-import dashboards on startup.
         */
        private boolean autoImport = false;

        /**
         * Dashboard folder name.
         */
        private String folderName = "Spring Boot Observability";
    }

    @Data
    public static class StackExport {
        /**
         * Enable monitoring stack export.
         */
        private boolean enabled = false;

        /**
         * Path where to export monitoring files.
         * <p>
         * Supports multiple formats:
         * <ul>
         *   <li>Relative path: {@code ./monitoring} or {@code monitoring} (resolved from working directory)</li>
         *   <li>Parent relative: {@code ../monitoring} (one level up from working directory)</li>
         *   <li>Absolute path: {@code /opt/monitoring} or {@code C:\monitoring}</li>
         *   <li>Environment variable: {@code ${MONITORING_PATH:./monitoring}}</li>
         *   <li>Home directory: {@code ~/monitoring} (will be resolved to user home)</li>
         * </ul>
         * <p>
         * Default: {@code ./monitoring} (relative to working directory)
         * <p>
         * Example configurations:
         * <pre>{@code
         * # Relative to working directory
         * observability.stack-export.export-path=./monitoring
         *
         * # Absolute path
         * observability.stack-export.export-path=/opt/myapp/monitoring
         *
         * # Environment variable with fallback
         * observability.stack-export.export-path=${MONITORING_PATH:./monitoring}
         *
         * # User home directory
         * observability.stack-export.export-path=~/monitoring
         * }</pre>
         */
        private String exportPath = "./monitoring";

        /**
         * Personalize configuration with service name.
         */
        private boolean personalizeConfig = true;

        /**
         * Auto-export on application startup.
         */
        private boolean exportOnStartup = false;
    }
}
