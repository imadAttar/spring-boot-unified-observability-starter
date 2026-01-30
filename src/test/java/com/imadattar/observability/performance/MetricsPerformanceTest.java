package com.imadattar.observability.performance;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de performance pour les métriques.
 * Vérifie que la collecte de métriques est performante.
 */
@DisplayName("Metrics Performance Tests")
class MetricsPerformanceTest {

    @Test
    @DisplayName("Should create 10000 counters quickly")
    void shouldCreate10000CountersQuickly() {
        MeterRegistry registry = new SimpleMeterRegistry();

        long start = System.currentTimeMillis();

        for (int i = 0; i < 10000; i++) {
            Counter.builder("test.counter." + i)
                .description("Test counter " + i)
                .tag("index", String.valueOf(i))
                .register(registry);
        }

        long duration = System.currentTimeMillis() - start;

        assertThat(registry.getMeters()).hasSize(10000);
        assertThat(duration).isLessThan(5000); // Moins de 5 secondes

        System.out.println("Created 10000 counters in " + duration + "ms");
    }

    @Test
    @DisplayName("Should increment counter 1 million times quickly")
    void shouldIncrementCounterQuickly() {
        MeterRegistry registry = new SimpleMeterRegistry();
        Counter counter = Counter.builder("test.counter")
            .register(registry);

        long start = System.currentTimeMillis();

        for (int i = 0; i < 1_000_000; i++) {
            counter.increment();
        }

        long duration = System.currentTimeMillis() - start;

        assertThat(counter.count()).isEqualTo(1_000_000);
        assertThat(duration).isLessThan(1000); // Moins de 1 seconde

        System.out.println("Incremented counter 1M times in " + duration + "ms");
    }

    @Test
    @DisplayName("Should record timer measurements quickly")
    void shouldRecordTimerQuickly() {
        MeterRegistry registry = new SimpleMeterRegistry();
        Timer timer = Timer.builder("test.timer")
            .register(registry);

        long start = System.currentTimeMillis();

        for (int i = 0; i < 100_000; i++) {
            timer.record(100, TimeUnit.MILLISECONDS);
        }

        long duration = System.currentTimeMillis() - start;

        assertThat(timer.count()).isEqualTo(100_000);
        assertThat(duration).isLessThan(2000); // Moins de 2 secondes

        System.out.println("Recorded 100k timer measurements in " + duration + "ms");
    }

    @Test
    @DisplayName("Should handle concurrent metric updates")
    void shouldHandleConcurrentUpdates() throws InterruptedException {
        MeterRegistry registry = new SimpleMeterRegistry();
        Counter counter = Counter.builder("concurrent.counter")
            .register(registry);

        int threadCount = 10;
        int incrementsPerThread = 10000;
        Thread[] threads = new Thread[threadCount];

        long start = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    counter.increment();
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        long duration = System.currentTimeMillis() - start;

        assertThat(counter.count()).isEqualTo(threadCount * incrementsPerThread);
        assertThat(duration).isLessThan(3000); // Moins de 3 secondes

        System.out.println("Handled " + (threadCount * incrementsPerThread) +
                         " concurrent increments in " + duration + "ms");
    }

    @Test
    @DisplayName("Should handle many tags efficiently")
    void shouldHandleManyTagsEfficiently() {
        MeterRegistry registry = new SimpleMeterRegistry();

        long start = System.currentTimeMillis();

        for (int i = 0; i < 1000; i++) {
            Counter.builder("tagged.counter")
                .tag("tag1", "value" + i)
                .tag("tag2", "value" + (i % 10))
                .tag("tag3", "value" + (i % 5))
                .tag("tag4", "value" + (i % 2))
                .register(registry);
        }

        long duration = System.currentTimeMillis() - start;

        assertThat(registry.getMeters()).hasSize(1000);
        assertThat(duration).isLessThan(2000); // Moins de 2 secondes

        System.out.println("Created 1000 meters with multiple tags in " + duration + "ms");
    }

    @Test
    @DisplayName("Should query metrics efficiently")
    void shouldQueryMetricsEfficiently() {
        MeterRegistry registry = new SimpleMeterRegistry();

        // Créer beaucoup de métriques
        for (int i = 0; i < 10000; i++) {
            Counter.builder("query.test.counter." + i)
                .tag("category", "test")
                .register(registry);
        }

        long start = System.currentTimeMillis();

        // Faire plusieurs recherches
        for (int i = 0; i < 1000; i++) {
            registry.find("query.test.counter." + (i % 100)).counter();
        }

        long duration = System.currentTimeMillis() - start;

        assertThat(duration).isLessThan(1000); // Moins de 1 seconde

        System.out.println("Performed 1000 metric queries in " + duration + "ms");
    }

    @Test
    @DisplayName("Should calculate percentiles efficiently")
    void shouldCalculatePercentilesEfficiently() {
        MeterRegistry registry = new SimpleMeterRegistry();
        Timer timer = Timer.builder("percentile.timer")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);

        long start = System.currentTimeMillis();

        // Enregistrer beaucoup de mesures
        for (int i = 0; i < 10000; i++) {
            timer.record(i, TimeUnit.MILLISECONDS);
        }

        long duration = System.currentTimeMillis() - start;

        assertThat(timer.count()).isEqualTo(10000);
        assertThat(duration).isLessThan(2000); // Moins de 2 secondes

        System.out.println("Recorded 10k timer samples with percentiles in " + duration + "ms");
    }

    @Test
    @DisplayName("Should have minimal memory overhead per metric")
    void shouldHaveMinimalMemoryOverhead() {
        MeterRegistry registry = new SimpleMeterRegistry();

        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long memBefore = runtime.totalMemory() - runtime.freeMemory();

        // Créer 1000 métriques
        for (int i = 0; i < 1000; i++) {
            Counter.builder("memory.test.counter." + i)
                .description("Memory test counter " + i)
                .tag("index", String.valueOf(i))
                .register(registry);
        }

        runtime.gc();
        long memAfter = runtime.totalMemory() - runtime.freeMemory();

        long memUsed = memAfter - memBefore;
        long memPerMetric = memUsed / 1000;

        System.out.println("Memory used for 1000 metrics: " + (memUsed / 1024) + "KB");
        System.out.println("Memory per metric: " + memPerMetric + " bytes");

        // Chaque métrique devrait utiliser moins de 10KB
        assertThat(memPerMetric).isLessThan(10 * 1024);
    }
}
