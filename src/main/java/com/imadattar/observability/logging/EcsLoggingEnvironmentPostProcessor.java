package com.imadattar.observability.logging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Environment post-processor that configures ECS logging properties early in the Spring Boot lifecycle.
 * <p>
 * This processor runs before the application context is created, allowing it to configure
 * logging properties before the logging system is initialized. It checks if ECS logging format
 * is enabled and adds the necessary Spring Boot 3.4+ structured logging properties.
 * <p>
 * The processor is automatically discovered by Spring Boot via {@code META-INF/spring.factories}.
 * <p>
 * When {@code observability.logging.format=ecs}, it configures:
 * <ul>
 *   <li>{@code logging.structured.format.console=ecs}</li>
 *   <li>{@code logging.structured.ecs.service.name} (from spring.application.name)</li>
 *   <li>{@code logging.structured.ecs.service.version} (from info.app.version)</li>
 *   <li>{@code logging.structured.ecs.service.environment} (from spring.profiles.active)</li>
 * </ul>
 *
 * @see <a href="https://docs.spring.io/spring-boot/reference/features/logging.html#features.logging.structured">Spring Boot Structured Logging</a>
 */
public class EcsLoggingEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String OBSERVABILITY_LOGGING_FORMAT = "observability.logging.format";
    private static final String OBSERVABILITY_LOGGING_ENABLED = "observability.logging.json-enabled";
    private static final String ECS_FORMAT = "ecs";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String loggingFormat = environment.getProperty(OBSERVABILITY_LOGGING_FORMAT, "json");
        boolean loggingEnabled = environment.getProperty(OBSERVABILITY_LOGGING_ENABLED, Boolean.class, true);

        // Only configure ECS if format is set to "ecs" and logging is enabled
        if (!ECS_FORMAT.equalsIgnoreCase(loggingFormat) || !loggingEnabled) {
            return;
        }

        Map<String, Object> ecsProperties = new HashMap<>();

        // Enable ECS format for console logging
        ecsProperties.put("logging.structured.format.console", "ecs");

        // Configure service metadata from environment
        String serviceName = environment.getProperty("spring.application.name");
        if (serviceName != null) {
            ecsProperties.put("logging.structured.ecs.service.name", serviceName);
        }

        String serviceVersion = environment.getProperty("info.app.version");
        if (serviceVersion != null) {
            ecsProperties.put("logging.structured.ecs.service.version", serviceVersion);
        }

        String serviceEnvironment = environment.getProperty("spring.profiles.active");
        if (serviceEnvironment != null) {
            ecsProperties.put("logging.structured.ecs.service.environment", serviceEnvironment);
        }

        // Add ECS properties to environment with high precedence
        environment.getPropertySources().addFirst(
                new MapPropertySource("ecsLoggingConfiguration", ecsProperties)
        );
    }
}
