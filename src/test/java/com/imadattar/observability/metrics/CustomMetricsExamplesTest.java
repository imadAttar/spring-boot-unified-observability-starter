package com.imadattar.observability.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for CustomMetricsExamples.
 */
class CustomMetricsExamplesTest {

    private SimpleMeterRegistry registry;
    private CustomMetricsExamples customMetrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        customMetrics = new CustomMetricsExamples(registry);
    }

    @Test
    void testRecordOrder() {
        // Given
        double orderValue = 99.99;

        // When
        customMetrics.recordOrder(orderValue);

        // Then
        assertThat(customMetrics.getOrderCount()).isEqualTo(1.0);
        assertThat(registry.find("business.orders.total").counter()).isNotNull();
        assertThat(registry.find("business.orders.total").counter().count()).isEqualTo(1.0);

        assertThat(registry.find("business.order.value").summary()).isNotNull();
        assertThat(registry.find("business.order.value").summary().count()).isEqualTo(1);
        assertThat(registry.find("business.order.value").summary().totalAmount()).isEqualTo(orderValue);
    }

    @Test
    void testRecordMultipleOrders() {
        // Given
        double order1 = 50.0;
        double order2 = 75.5;
        double order3 = 100.0;

        // When
        customMetrics.recordOrder(order1);
        customMetrics.recordOrder(order2);
        customMetrics.recordOrder(order3);

        // Then
        assertThat(customMetrics.getOrderCount()).isEqualTo(3.0);
        assertThat(registry.find("business.order.value").summary().totalAmount())
            .isEqualTo(order1 + order2 + order3);
    }

    @Test
    void testProcessPayment() {
        // Given
        boolean[] executed = {false};

        // When
        customMetrics.processPayment(() -> executed[0] = true);

        // Then
        assertThat(executed[0]).isTrue();
        assertThat(registry.find("business.payment.duration").timer()).isNotNull();
        assertThat(registry.find("business.payment.duration").timer().count()).isEqualTo(1);
    }

    @Test
    void testPaymentTimerRecordsTime() {
        // Given / When
        customMetrics.processPayment(() -> {
            try {
                Thread.sleep(10); // Simulate payment processing
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Then
        assertThat(registry.find("business.payment.duration").timer()).isNotNull();
        assertThat(registry.find("business.payment.duration").timer().totalTime(java.util.concurrent.TimeUnit.MILLISECONDS))
            .isGreaterThan(0);
    }

    @Test
    void testSetActiveUsers() {
        // Given
        int userCount = 42;

        // When
        customMetrics.setActiveUsers(userCount);

        // Then
        assertThat(customMetrics.getActiveUsers()).isEqualTo(userCount);
        assertThat(registry.find("business.users.active").gauge()).isNotNull();
        assertThat(registry.find("business.users.active").gauge().value()).isEqualTo(userCount);
    }

    @Test
    void testUpdateActiveUsers() {
        // Given
        customMetrics.setActiveUsers(10);

        // When
        customMetrics.setActiveUsers(25);

        // Then
        assertThat(customMetrics.getActiveUsers()).isEqualTo(25);
        assertThat(registry.find("business.users.active").gauge().value()).isEqualTo(25);
    }

    @Test
    void testMetricsHaveCorrectTags() {
        // Given / When
        customMetrics.recordOrder(100.0);

        // Then
        assertThat(registry.find("business.orders.total")
            .tags("type", "example")
            .counter())
            .isNotNull();
    }

    @Test
    void testAllMetricsRegistered() {
        // When
        customMetrics.recordOrder(50.0);
        customMetrics.processPayment(() -> {});
        customMetrics.setActiveUsers(10);

        // Then
        assertThat(registry.find("business.orders.total").counter()).isNotNull();
        assertThat(registry.find("business.payment.duration").timer()).isNotNull();
        assertThat(registry.find("business.users.active").gauge()).isNotNull();
        assertThat(registry.find("business.order.value").summary()).isNotNull();
    }

    @Test
    void testOrderValueBaseUnit() {
        // Given / When
        customMetrics.recordOrder(100.0);

        // Then
        assertThat(registry.find("business.order.value").summary().getId().getBaseUnit())
            .isEqualTo("dollars");
    }
}
