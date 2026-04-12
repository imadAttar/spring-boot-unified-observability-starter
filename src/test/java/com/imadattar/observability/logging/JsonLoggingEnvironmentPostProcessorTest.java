package com.imadattar.observability.logging;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class JsonLoggingEnvironmentPostProcessorTest {

    private final JsonLoggingEnvironmentPostProcessor processor = new JsonLoggingEnvironmentPostProcessor();
    private final SpringApplication application = new SpringApplication();

    @Test
    void shouldConfigureLogstashFormatWhenJsonIsDefault() {
        MockEnvironment env = new MockEnvironment();
        // Default: format=json, json-enabled=true

        processor.postProcessEnvironment(env, application);

        assertThat(env.getProperty("logging.structured.format.console")).isEqualTo("logstash");
    }

    @Test
    void shouldConfigureLogstashFormatWhenJsonIsExplicit() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("observability.logging.format", "json");

        processor.postProcessEnvironment(env, application);

        assertThat(env.getProperty("logging.structured.format.console")).isEqualTo("logstash");
    }

    @Test
    void shouldNotConfigureWhenFormatIsEcs() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("observability.logging.format", "ecs");

        processor.postProcessEnvironment(env, application);

        assertThat(env.getProperty("logging.structured.format.console")).isNull();
    }

    @Test
    void shouldNotConfigureWhenLoggingIsDisabled() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("observability.logging.json-enabled", "false");

        processor.postProcessEnvironment(env, application);

        assertThat(env.getProperty("logging.structured.format.console")).isNull();
    }

    @Test
    void shouldSetServiceNameFromSpringApplicationName() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("spring.application.name", "my-service");

        processor.postProcessEnvironment(env, application);

        assertThat(env.getProperty("logging.structured.logstash.service.name")).isEqualTo("my-service");
    }

    @Test
    void shouldSetServiceVersionFromInfoAppVersion() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("info.app.version", "2.0.0");

        processor.postProcessEnvironment(env, application);

        assertThat(env.getProperty("logging.structured.logstash.service.version")).isEqualTo("2.0.0");
    }

    @Test
    void shouldNotSetServiceNameWhenAbsent() {
        MockEnvironment env = new MockEnvironment();
        // No spring.application.name set

        processor.postProcessEnvironment(env, application);

        assertThat(env.getProperty("logging.structured.logstash.service.name")).isNull();
    }
}
