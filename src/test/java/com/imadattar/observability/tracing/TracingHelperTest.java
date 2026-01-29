package com.imadattar.observability.tracing;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Comprehensive tests for TracingHelper.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TracingHelperTest {

    @Mock
    private Tracer tracer;

    @Mock
    private Span span;

    @Mock
    private Scope scope;

    @Mock
    private SpanBuilder spanBuilder;

    private TracingHelper tracingHelper;

    @BeforeEach
    void setUp() {
        when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.startSpan()).thenReturn(span);
        when(span.makeCurrent()).thenReturn(scope);

        tracingHelper = new TracingHelper(tracer);
    }

    @Test
    void testTraceWithSupplier_Success() {
        // Given
        String expectedResult = "success";
        String spanName = "test-operation";

        // When
        String actualResult = tracingHelper.trace(spanName, () -> expectedResult);

        // Then
        assertThat(actualResult).isEqualTo(expectedResult);
        verify(tracer).spanBuilder(spanName);
        verify(spanBuilder).startSpan();
        verify(span).makeCurrent();
        verify(span).setStatus(StatusCode.OK);
        verify(span).end();
        verify(scope).close();
    }

    @Test
    void testTraceWithSupplier_Exception() {
        // Given
        String spanName = "failing-operation";
        RuntimeException expectedException = new RuntimeException("Test exception");

        // When / Then
        assertThatThrownBy(() ->
            tracingHelper.trace(spanName, () -> {
                throw expectedException;
            })
        ).isSameAs(expectedException);

        verify(tracer).spanBuilder(spanName);
        verify(spanBuilder).startSpan();
        verify(span).makeCurrent();
        verify(span).setStatus(StatusCode.ERROR, "Test exception");
        verify(span).recordException(expectedException);
        verify(span).end();
        verify(scope).close();
    }

    @Test
    void testTraceWithRunnable_Success() {
        // Given
        String spanName = "void-operation";
        boolean[] executed = {false};

        // When
        tracingHelper.trace(spanName, () -> executed[0] = true);

        // Then
        assertThat(executed[0]).isTrue();
        verify(tracer).spanBuilder(spanName);
        verify(spanBuilder).startSpan();
        verify(span).makeCurrent();
        verify(span).setStatus(StatusCode.OK);
        verify(span).end();
        verify(scope).close();
    }

    @Test
    void testTraceWithRunnable_Exception() {
        // Given
        String spanName = "failing-void-operation";
        RuntimeException expectedException = new RuntimeException("Void test exception");

        // When / Then
        assertThatThrownBy(() ->
            tracingHelper.trace(spanName, (Runnable) () -> {
                throw expectedException;
            })
        ).isSameAs(expectedException);

        verify(tracer).spanBuilder(spanName);
        verify(spanBuilder).startSpan();
        verify(span).makeCurrent();
        verify(span).setStatus(StatusCode.ERROR, "Void test exception");
        verify(span).recordException(expectedException);
        verify(span).end();
        verify(scope).close();
    }

    @Test
    void testAddAttribute() {
        // Given
        String key = "user.id";
        String value = "12345";
        Span currentSpan = mock(Span.class);

        // Mock Span.current() - this is a static method, so we need to set up the mock differently
        // For this test, we'll verify the method exists and can be called
        // In a real scenario, you'd use a spy or integration test

        // When
        tracingHelper.addAttribute(key, value);

        // Then - this is a best-effort test since Span.current() is static
        // The method should execute without throwing exceptions
        // In integration tests, we'd verify the actual attribute was added
    }

    @Test
    void testAddEvent() {
        // Given
        String eventName = "user.logged.in";

        // When
        tracingHelper.addEvent(eventName);

        // Then - this is a best-effort test since Span.current() is static
        // The method should execute without throwing exceptions
        // In integration tests, we'd verify the actual event was added
    }

    @Test
    void testSpanNaming() {
        // Given
        String spanName1 = "database.query";
        String spanName2 = "http.request";

        // When
        tracingHelper.trace(spanName1, () -> "result1");
        tracingHelper.trace(spanName2, () -> "result2");

        // Then
        verify(tracer).spanBuilder(spanName1);
        verify(tracer).spanBuilder(spanName2);
    }

    @Test
    void testNestedSpans() {
        // Given
        String outerSpanName = "outer-operation";
        String innerSpanName = "inner-operation";

        Span outerSpan = mock(Span.class);
        Span innerSpan = mock(Span.class);
        Scope outerScope = mock(Scope.class);
        Scope innerScope = mock(Scope.class);

        SpanBuilder outerBuilder = mock(SpanBuilder.class);
        SpanBuilder innerBuilder = mock(SpanBuilder.class);

        when(tracer.spanBuilder(outerSpanName)).thenReturn(outerBuilder);
        when(tracer.spanBuilder(innerSpanName)).thenReturn(innerBuilder);
        when(outerBuilder.startSpan()).thenReturn(outerSpan);
        when(innerBuilder.startSpan()).thenReturn(innerSpan);
        when(outerSpan.makeCurrent()).thenReturn(outerScope);
        when(innerSpan.makeCurrent()).thenReturn(innerScope);

        // When
        String result = tracingHelper.trace(outerSpanName, () ->
            tracingHelper.trace(innerSpanName, () -> "nested-result")
        );

        // Then
        assertThat(result).isEqualTo("nested-result");

        // Verify outer span
        verify(tracer).spanBuilder(outerSpanName);
        verify(outerBuilder).startSpan();
        verify(outerSpan).makeCurrent();
        verify(outerSpan).setStatus(StatusCode.OK);
        verify(outerSpan).end();
        verify(outerScope).close();

        // Verify inner span
        verify(tracer).spanBuilder(innerSpanName);
        verify(innerBuilder).startSpan();
        verify(innerSpan).makeCurrent();
        verify(innerSpan).setStatus(StatusCode.OK);
        verify(innerSpan).end();
        verify(innerScope).close();
    }

    @Test
    void testSpanEndCalledEvenOnException() {
        // Given
        String spanName = "operation-with-exception";
        RuntimeException exception = new RuntimeException("Test");

        // When
        try {
            tracingHelper.trace(spanName, () -> {
                throw exception;
            });
        } catch (RuntimeException e) {
            // Expected
        }

        // Then - span.end() should be called even when exception occurs
        verify(span).end();
    }

    @Test
    void testMultipleSequentialTraces() {
        // Given
        int numberOfTraces = 5;

        // When
        for (int i = 0; i < numberOfTraces; i++) {
            final int index = i; // Make effectively final for lambda
            tracingHelper.trace("operation-" + index, () -> "result-" + index);
        }

        // Then
        verify(tracer, times(numberOfTraces)).spanBuilder(anyString());
        verify(spanBuilder, times(numberOfTraces)).startSpan();
        verify(span, times(numberOfTraces)).setStatus(StatusCode.OK);
        verify(span, times(numberOfTraces)).end();
    }
}
