package com.imadattar.observability.tracing;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * Utility class for manual span creation and management.
 *
 * Example usage:
 * <pre>
 * &#64;Autowired
 * private TracingHelper tracingHelper;
 *
 * public void processOrder(Order order) {
 *     tracingHelper.trace("process-order", () -> {
 *         // Your business logic here
 *         return orderRepository.save(order);
 *     });
 * }
 * </pre>
 */
@RequiredArgsConstructor
@Slf4j
public class TracingHelper {

    private final Tracer tracer;

    /**
     * Execute code within a new span.
     *
     * @param spanName Name of the span
     * @param operation Operation to execute
     * @param <T> Return type
     * @return Result of the operation
     */
    public <T> T trace(String spanName, Supplier<T> operation) {
        Span span = tracer.spanBuilder(spanName).startSpan();

        try (Scope scope = span.makeCurrent()) {
            T result = operation.get();
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Execute code within a new span (void operation).
     *
     * @param spanName Name of the span
     * @param operation Operation to execute
     */
    public void trace(String spanName, Runnable operation) {
        Span span = tracer.spanBuilder(spanName).startSpan();

        try (Scope scope = span.makeCurrent()) {
            operation.run();
            span.setStatus(StatusCode.OK);
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Add attribute to current span.
     *
     * @param key Attribute key
     * @param value Attribute value
     */
    public void addAttribute(String key, String value) {
        Span.current().setAttribute(key, value);
    }

    /**
     * Add event to current span.
     *
     * @param eventName Event name
     */
    public void addEvent(String eventName) {
        Span.current().addEvent(eventName);
    }
}
