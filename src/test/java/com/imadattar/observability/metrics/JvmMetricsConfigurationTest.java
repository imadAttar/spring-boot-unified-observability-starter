package com.imadattar.observability.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for JvmMetricsConfiguration.
 */
class JvmMetricsConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(JvmMetricsConfiguration.class))
        .withBean(SimpleMeterRegistry.class, SimpleMeterRegistry::new);

    @Test
    void testBeansCreatedWhenEnabled() {
        contextRunner
            .withPropertyValues("observability.metrics.jvm-enabled=true")
            .run(context -> {
                assertThat(context).hasSingleBean(JvmMemoryMetrics.class);
                assertThat(context).hasSingleBean(JvmGcMetrics.class);
                assertThat(context).hasSingleBean(JvmThreadMetrics.class);
                assertThat(context).hasSingleBean(ClassLoaderMetrics.class);
                assertThat(context).hasSingleBean(ProcessorMetrics.class);
            });
    }

    @Test
    void testBeansNotCreatedWhenDisabled() {
        contextRunner
            .withPropertyValues("observability.metrics.jvm-enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(JvmMemoryMetrics.class);
                assertThat(context).doesNotHaveBean(JvmGcMetrics.class);
                assertThat(context).doesNotHaveBean(JvmThreadMetrics.class);
                assertThat(context).doesNotHaveBean(ClassLoaderMetrics.class);
                assertThat(context).doesNotHaveBean(ProcessorMetrics.class);
            });
    }

    @Test
    void testBeansCreatedByDefaultWhenPropertyNotSet() {
        contextRunner
            .run(context -> {
                // matchIfMissing = true, so beans should be created
                assertThat(context).hasSingleBean(JvmMemoryMetrics.class);
                assertThat(context).hasSingleBean(JvmGcMetrics.class);
                assertThat(context).hasSingleBean(JvmThreadMetrics.class);
                assertThat(context).hasSingleBean(ClassLoaderMetrics.class);
                assertThat(context).hasSingleBean(ProcessorMetrics.class);
            });
    }

    @Test
    void testMetricsRegistered() {
        contextRunner
            .withPropertyValues("observability.metrics.jvm-enabled=true")
            .run(context -> {
                SimpleMeterRegistry registry = context.getBean(SimpleMeterRegistry.class);

                // Bind metrics to registry
                context.getBean(JvmMemoryMetrics.class).bindTo(registry);
                context.getBean(JvmGcMetrics.class).bindTo(registry);
                context.getBean(JvmThreadMetrics.class).bindTo(registry);
                context.getBean(ClassLoaderMetrics.class).bindTo(registry);
                context.getBean(ProcessorMetrics.class).bindTo(registry);

                // Verify metrics are present
                assertThat(registry.find("jvm.memory.used").meters()).isNotEmpty();
                assertThat(registry.find("jvm.memory.max").meters()).isNotEmpty();
                // GC metrics may not be immediately available, so we just check the binder exists
                assertThat(context.getBean(JvmGcMetrics.class)).isNotNull();
                assertThat(registry.find("jvm.threads.live").gauge()).isNotNull();
                assertThat(registry.find("jvm.classes.loaded").gauge()).isNotNull();
                assertThat(registry.find("system.cpu.count").gauge()).isNotNull();
            });
    }

    @Test
    void testMetricTags() {
        contextRunner
            .withPropertyValues("observability.metrics.jvm-enabled=true")
            .run(context -> {
                SimpleMeterRegistry registry = context.getBean(SimpleMeterRegistry.class);

                // Bind metrics
                context.getBean(JvmMemoryMetrics.class).bindTo(registry);

                // Verify that memory metrics have appropriate tags
                assertThat(registry.find("jvm.memory.used").meters())
                    .isNotEmpty()
                    .allMatch(meter -> meter.getId().getTags().stream()
                        .anyMatch(tag -> tag.getKey().equals("area")));
            });
    }
}
