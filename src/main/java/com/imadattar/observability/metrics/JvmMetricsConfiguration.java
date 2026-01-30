package com.imadattar.observability.metrics;

import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for JVM metrics using Micrometer binders.
 *
 * Provides metrics for:
 * - Memory usage (heap, non-heap)
 * - Garbage collection (pause time, count)
 * - Thread metrics (live, daemon, peak)
 * - Class loader metrics (loaded, unloaded)
 * - CPU metrics (system, process)
 *
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = "observability.metrics", name = "jvm-enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class JvmMetricsConfiguration {

    /**
     * Register JVM memory metrics.
     * Exposes: jvm_memory_used_bytes, jvm_memory_max_bytes, jvm_memory_committed_bytes
     */
    @Bean
    public JvmMemoryMetrics jvmMemoryMetrics() {
        log.info("✅ Registering JVM memory metrics");
        return new JvmMemoryMetrics();
    }

    /**
     * Register JVM garbage collection metrics.
     * Exposes: jvm_gc_pause_seconds_count, jvm_gc_pause_seconds_sum, jvm_gc_pause_seconds_max
     */
    @Bean
    public JvmGcMetrics jvmGcMetrics() {
        log.info("✅ Registering JVM GC metrics");
        return new JvmGcMetrics();
    }

    /**
     * Register JVM thread metrics.
     * Exposes: jvm_threads_live_threads, jvm_threads_daemon_threads, jvm_threads_peak_threads, jvm_threads_states_threads
     */
    @Bean
    public JvmThreadMetrics jvmThreadMetrics() {
        log.info("✅ Registering JVM thread metrics");
        return new JvmThreadMetrics();
    }

    /**
     * Register class loader metrics.
     * Exposes: jvm_classes_loaded_classes, jvm_classes_unloaded_classes_total
     */
    @Bean
    public ClassLoaderMetrics classLoaderMetrics() {
        log.info("✅ Registering class loader metrics");
        return new ClassLoaderMetrics();
    }

    /**
     * Register processor (CPU) metrics.
     * Exposes: system_cpu_usage, system_cpu_count, process_cpu_usage
     */
    @Bean
    public ProcessorMetrics processorMetrics() {
        log.info("✅ Registering processor metrics");
        return new ProcessorMetrics();
    }
}
