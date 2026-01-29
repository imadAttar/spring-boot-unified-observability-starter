package com.imadattar.observability;

import com.imadattar.observability.config.ObservabilityAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * Test application for integration tests.
 */
@SpringBootApplication
@Import(ObservabilityAutoConfiguration.class)
public class TestApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
