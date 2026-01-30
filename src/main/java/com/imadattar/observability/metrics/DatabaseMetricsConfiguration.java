package com.imadattar.observability.metrics;

import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Configuration for database connection pool metrics.
 *
 * Provides metrics for HikariCP connection pool when available:
 * - Active connections
 * - Idle connections
 * - Pending connection requests
 * - Maximum pool size
 * - Connection acquisition time
 * - Connection timeout count
 *
 * Note: Requires HikariCP to be on the classpath and used as the DataSource implementation.
 *
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = "observability.metrics", name = "database-enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(DataSource.class)
@Slf4j
public class DatabaseMetricsConfiguration {

    /**
     * Register HikariCP connection pool metrics if HikariCP is available.
     * This uses reflection to avoid compile-time dependency on HikariCP.
     *
     * HikariCP metrics are automatically registered when HikariDataSource.setMetricRegistry() is called.
     *
     * Exposes metrics:
     * - hikaricp_connections_active
     * - hikaricp_connections_idle
     * - hikaricp_connections_pending
     * - hikaricp_connections_max
     * - hikaricp_connections_min
     * - hikaricp_connections_usage
     * - hikaricp_connections_timeout_total
     * - hikaricp_connections_acquire_seconds
     */
    @Bean
    @ConditionalOnBean(DataSource.class)
    public MeterBinder hikariMetrics(DataSource dataSource) {
        // Check if HikariCP is available and the DataSource is HikariDataSource
        String dataSourceClassName = dataSource.getClass().getName();

        if (dataSourceClassName.contains("HikariDataSource")) {
            log.info("✅ Registering HikariCP connection pool metrics");
            return registry -> {
                try {
                    // Use reflection to call setMetricRegistry on HikariDataSource
                    dataSource.getClass()
                        .getMethod("setMetricRegistry", Object.class)
                        .invoke(dataSource, registry);

                    // Get pool name if available
                    String poolName = "default";
                    try {
                        Object name = dataSource.getClass().getMethod("getPoolName").invoke(dataSource);
                        if (name != null) {
                            poolName = name.toString();
                        }
                    } catch (Exception e) {
                        // Ignore if getPoolName fails
                    }

                    log.info("✅ HikariCP metrics registered for pool: {}", poolName);
                } catch (Exception e) {
                    log.warn("⚠️ Failed to register HikariCP metrics: {}", e.getMessage());
                }
            };
        }

        log.info("ℹ️ DataSource is not HikariCP ({}) - connection pool metrics not available", dataSourceClassName);
        return registry -> {}; // no-op if not HikariCP
    }
}
