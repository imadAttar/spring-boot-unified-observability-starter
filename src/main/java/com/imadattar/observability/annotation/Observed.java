package com.imadattar.observability.annotation;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;

import java.lang.annotation.*;

/**
 * Composite annotation for full observability on a method.
 *
 * Combines:
 * - @Timed: Records execution time as metrics
 * - @Counted: Counts method invocations
 * - Automatic span creation for tracing
 *
 * Example usage:
 * <pre>
 * &#64;Service
 * public class UserService {
 *
 *     &#64;Observed(name = "user.service.create")
 *     public User createUser(CreateUserRequest request) {
 *         // Your business logic
 *         return userRepository.save(user);
 *     }
 * }
 * </pre>
 *
 * This will automatically:
 * - Create Prometheus metrics: user_service_create_seconds_count, user_service_create_seconds_sum
 * - Create distributed trace span
 * - Log execution with trace ID
 *
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Timed
@Counted
public @interface Observed {

    /**
     * Name of the metric and span.
     * If not specified, defaults to class.method format.
     */
    String name() default "";

    /**
     * Description of what this method does.
     */
    String description() default "";

    /**
     * Additional tags to add to metrics.
     */
    String[] tags() default {};

    /**
     * Whether to record exceptions as separate metrics.
     */
    boolean recordExceptions() default true;
}
