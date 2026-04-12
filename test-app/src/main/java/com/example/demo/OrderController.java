package com.example.demo;

import com.imadattar.observability.annotation.Observed;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Demo controller showing observability starter features.
 */
@RestController
public class OrderController {

    @GetMapping("/orders")
    @Observed(name = "orders.list")
    public Map<String, Object> listOrders() {
        simulateWork(50, 200);
        return Map.of(
            "orders", 42,
            "status", "ok"
        );
    }

    @GetMapping("/orders/{id}")
    @Timed(value = "orders.get", description = "Get order by ID")
    @Counted(value = "orders.get.count")
    public Map<String, Object> getOrder(@PathVariable String id) {
        simulateWork(10, 100);
        return Map.of(
            "id", id,
            "product", "Widget",
            "amount", 29.99
        );
    }

    @GetMapping("/orders/slow")
    @Timed(value = "orders.slow", description = "Slow endpoint for testing alerts")
    public Map<String, Object> slowEndpoint() {
        simulateWork(1000, 3000);
        return Map.of("status", "finally done");
    }

    private void simulateWork(int minMs, int maxMs) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(minMs, maxMs));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
