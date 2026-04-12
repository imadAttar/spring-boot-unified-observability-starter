package com.imadattar.observability.logging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Environment post-processor that configures structured JSON logging properties early in the Spring Boot lifecycle.
 * <p>
 * This processor runs before the application context is created, allowing it to configure
 * logging properties before the logging system is initialized. It checks if JSON logging format
 * is enabled (default) and adds the necessary Spring Boot 3.4+ structured logging properties
 * to enable Logstash JSON format.
 * <p>
 * When {@code observability.logging.format=json} (default), it configures:
 * <ul>
 *   <li>{@code logging.structured.format.console=logstash}</li>
 *   <li>{@code logging.structured.logstash.service.name} (from spring.application.name)</li>
 *   <li>{@code logging.structured.logstash.service.version} (from info.app.version)</li>
 * </ul>
 *
 * @see EcsLoggingEnvironmentPostProcessor
 * @since 1.3.0
 */
public class JsonLoggingEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String OBSERVABILITY_LOGGING_FORMAT = "observability.logging.format";
    private static final String OBSERVABILITY_LOGGING_ENABLED = "observability.logging.json-enabled";
    private static final String JSON_FORMAT = "json";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String loggingFormat = environment.getProperty(OBSERVABILITY_LOGGING_FORMAT, "json");
        boolean loggingEnabled = environment.getProperty(OBSERVABILITY_LOGGING_ENABLED, Boolean.class, true);

        // Only configure JSON if format is set to "json" (default) and logging is enabled
        if (!JSON_FORMAT.equalsIgnoreCase(loggingFormat) || !loggingEnabled) {
            return;
        }

        Map<String, Object> jsonProperties = new HashMap<>();

        // Enable Logstash JSON format for console logging (Spring Boot 3.4+)
        jsonProperties.put("logging.structured.format.console", "logstash");

        // Configure service metadata from environment
        String serviceName = environment.getProperty("spring.application.name");
        if (serviceName != null) {
            jsonProperties.put("logging.structured.logstash.service.name", serviceName);
        }

        String serviceVersion = environment.getProperty("info.app.version");
        if (serviceVersion != null) {
            jsonProperties.put("logging.structured.logstash.service.version", serviceVersion);
        }

        // Add JSON properties to environment with high precedence
        environment.getPropertySources().addFirst(
                new MapPropertySource("jsonLoggingConfiguration", jsonProperties)
        );
    }
}
