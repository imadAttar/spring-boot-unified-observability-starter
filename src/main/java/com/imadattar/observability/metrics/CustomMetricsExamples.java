package com.imadattar.observability.metrics;

import io.micrometer.core.instrument.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Examples of custom business metrics using Micrometer.
 *
 * This class demonstrates how to create and use:
 * - Counter: For counting events (orders, logins, errors)
 * - Timer: For measuring durations (payment processing, API calls)
 * - Gauge: For tracking current values (active users, queue size)
 * - DistributionSummary: For measuring distributions (order values, file sizes)
 *
 * These examples can be used as templates for implementing domain-specific metrics.
 */
@Service
@ConditionalOnProperty(prefix = "observability.metrics", name = "custom-enabled", havingValue = "true")
@Slf4j
public class CustomMetricsExamples {

    private final Counter orderCounter;
    private final Timer paymentTimer;
    private final AtomicInteger activeUsers;
    private final DistributionSummary orderValue;

    public CustomMetricsExamples(MeterRegistry registry) {
        log.info("✅ Initializing custom business metrics examples");

        // Counter: Counts total number of orders
        this.orderCounter = Counter.builder("business.orders.total")
            .description("Total number of orders placed")
            .tag("type", "example")
            .register(registry);

        // Timer: Measures payment processing duration
        this.paymentTimer = Timer.builder("business.payment.duration")
            .description("Payment processing time in seconds")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);

        // Gauge: Tracks number of active users (current value)
        this.activeUsers = new AtomicInteger(0);
        Gauge.builder("business.users.active", activeUsers, AtomicInteger::get)
            .description("Number of currently active users")
            .register(registry);

        // DistributionSummary: Measures distribution of order values
        this.orderValue = DistributionSummary.builder("business.order.value")
            .description("Order value distribution in USD")
            .baseUnit("dollars")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);

        log.info("✅ Custom metrics registered: orders counter, payment timer, active users gauge, order value summary");
    }

    /**
     * Record a new order with its value.
     * Usage: customMetricsExamples.recordOrder(99.99);
     *
     * @param value Order value in dollars
     */
    public void recordOrder(double value) {
        orderCounter.increment();
        orderValue.record(value);
        log.debug("Order recorded: ${}", value);
    }

    /**
     * Record payment processing time.
     * Usage: customMetricsExamples.processPayment(() -> { ... payment logic ... });
     *
     * @param paymentLogic Runnable containing payment processing logic
     */
    public void processPayment(Runnable paymentLogic) {
        paymentTimer.record(paymentLogic);
    }

    /**
     * Update the count of active users.
     * Usage: customMetricsExamples.setActiveUsers(42);
     *
     * @param count Current number of active users
     */
    public void setActiveUsers(int count) {
        activeUsers.set(count);
        log.debug("Active users updated: {}", count);
    }

    /**
     * Get the current order count (for testing/monitoring).
     *
     * @return Current value of order counter
     */
    public double getOrderCount() {
        return orderCounter.count();
    }

    /**
     * Get the current active users count (for testing/monitoring).
     *
     * @return Current number of active users
     */
    public int getActiveUsers() {
        return activeUsers.get();
    }
}
