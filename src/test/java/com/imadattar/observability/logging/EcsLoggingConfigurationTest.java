package com.imadattar.observability.logging;

import com.imadattar.observability.config.ObservabilityAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ECS logging configuration.
 */
class EcsLoggingConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ObservabilityAutoConfiguration.class));

    @Test
    void shouldEnableEcsLoggingWhenFormatIsEcs() {
        contextRunner
                .withPropertyValues(
                        "observability.logging.format=ecs",
                        "spring.application.name=test-app"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(EcsLoggingConfiguration.class);
                    assertThat(context).doesNotHaveBean(LoggingConfiguration.class);
                });
    }

    @Test
    void shouldNotEnableEcsLoggingWhenFormatIsJson() {
        contextRunner
                .withPropertyValues("observability.logging.format=json")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(EcsLoggingConfiguration.class);
                    assertThat(context).hasSingleBean(LoggingConfiguration.class);
                });
    }

    @Test
    void shouldEnableJsonLoggingByDefault() {
        contextRunner
                .run(context -> {
                    assertThat(context).hasSingleBean(LoggingConfiguration.class);
                    assertThat(context).doesNotHaveBean(EcsLoggingConfiguration.class);
                });
    }

    @Test
    void shouldConfigureEcsWithServiceMetadata() {
        contextRunner
                .withPropertyValues(
                        "observability.logging.format=ecs",
                        "spring.application.name=my-service",
                        "info.app.version=1.0.0",
                        "spring.profiles.active=production"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(EcsLoggingConfiguration.class);
                    assertThat(context.getEnvironment().getProperty("spring.application.name"))
                            .isEqualTo("my-service");
                });
    }

    @Test
    void shouldNotEnableEcsLoggingWhenLoggingIsDisabled() {
        contextRunner
                .withPropertyValues(
                        "observability.logging.json-enabled=false"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(EcsLoggingConfiguration.class);
                    assertThat(context).doesNotHaveBean(LoggingConfiguration.class);
                });
    }

    @Test
    void shouldEnableJsonLoggingWhenJsonEnabledIsTrue() {
        contextRunner
                .withPropertyValues("observability.logging.json-enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(LoggingConfiguration.class);
                });
    }

    @Test
    void shouldDisableLoggingWhenJsonEnabledIsFalse() {
        contextRunner
                .withPropertyValues("observability.logging.json-enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(LoggingConfiguration.class);
                    assertThat(context).doesNotHaveBean(EcsLoggingConfiguration.class);
                });
    }
}
