package com.imadattar.observability.logging;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class EcsLoggingEnvironmentPostProcessorTest {

    private final EcsLoggingEnvironmentPostProcessor processor = new EcsLoggingEnvironmentPostProcessor();
    private final SpringApplication application = new SpringApplication();

    @Test
    void shouldConfigureEcsFormatWhenEcsIsSet() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("observability.logging.format", "ecs");

        processor.postProcessEnvironment(env, application);

        assertThat(env.getProperty("logging.structured.format.console")).isEqualTo("ecs");
    }

    @Test
    void shouldNotConfigureWhenFormatIsJson() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("observability.logging.format", "json");

        processor.postProcessEnvironment(env, application);

        assertThat(env.getProperty("logging.structured.format.console")).isNull();
    }

    @Test
    void shouldNotConfigureWhenFormatIsDefault() {
        MockEnvironment env = new MockEnvironment();
        // Default is "json", so ECS should not activate

        processor.postProcessEnvironment(env, application);

        assertThat(env.getProperty("logging.structured.format.console")).isNull();
    }

    @Test
    void shouldNotConfigureWhenLoggingIsDisabled() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("observability.logging.format", "ecs");
        env.setProperty("observability.logging.json-enabled", "false");

        processor.postProcessEnvironment(env, application);

        assertThat(env.getProperty("logging.structured.format.console")).isNull();
    }

    @Test
    void shouldSetServiceNameFromSpringApplicationName() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("observability.logging.format", "ecs");
        env.setProperty("spring.application.name", "my-ecs-service");

        processor.postProcessEnvironment(env, application);

        assertThat(env.getProperty("logging.structured.ecs.service.name")).isEqualTo("my-ecs-service");
    }

    @Test
    void shouldSetServiceVersionFromInfoAppVersion() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("observability.logging.format", "ecs");
        env.setProperty("info.app.version", "3.0.0");

        processor.postProcessEnvironment(env, application);

        assertThat(env.getProperty("logging.structured.ecs.service.version")).isEqualTo("3.0.0");
    }

    @Test
    void shouldSetServiceEnvironmentFromActiveProfile() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("observability.logging.format", "ecs");
        env.setProperty("spring.profiles.active", "production");

        processor.postProcessEnvironment(env, application);

        assertThat(env.getProperty("logging.structured.ecs.service.environment")).isEqualTo("production");
    }

    @Test
    void shouldNotSetOptionalPropertiesWhenAbsent() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("observability.logging.format", "ecs");

        processor.postProcessEnvironment(env, application);

        assertThat(env.getProperty("logging.structured.format.console")).isEqualTo("ecs");
        assertThat(env.getProperty("logging.structured.ecs.service.name")).isNull();
        assertThat(env.getProperty("logging.structured.ecs.service.version")).isNull();
        assertThat(env.getProperty("logging.structured.ecs.service.environment")).isNull();
    }
}
