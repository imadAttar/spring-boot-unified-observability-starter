package com.imadattar.observability.metrics;

import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for DatabaseMetricsConfiguration.
 */
class DatabaseMetricsConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(DatabaseMetricsConfiguration.class));

    /**
     * Simple test DataSource implementation.
     */
    private static class TestDataSource implements DataSource {
        @Override
        public Connection getConnection() throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public PrintWriter getLogWriter() throws SQLException {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) throws SQLException {
        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {
        }

        @Override
        public int getLoginTimeout() throws SQLException {
            return 0;
        }

        @Override
        public Logger getParentLogger() {
            return Logger.getLogger("test");
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return false;
        }
    }

    @Test
    void testMeterBinderNotCreatedWhenDisabled() {
        contextRunner
            .withPropertyValues("observability.metrics.database-enabled=false")
            .withBean(DataSource.class, TestDataSource::new)
            .run(context -> {
                assertThat(context).doesNotHaveBean(MeterBinder.class);
            });
    }

    @Test
    void testMeterBinderNotCreatedWhenNoDataSource() {
        contextRunner
            .withPropertyValues("observability.metrics.database-enabled=true")
            .run(context -> {
                // Should not create bean without DataSource
                assertThat(context).doesNotHaveBean(MeterBinder.class);
            });
    }

    @Test
    void testMeterBinderCreatedWithDataSource() {
        contextRunner
            .withPropertyValues("observability.metrics.database-enabled=true")
            .withBean(DataSource.class, TestDataSource::new)
            .run(context -> {
                assertThat(context).hasSingleBean(MeterBinder.class);
            });
    }

    @Test
    void testMeterBinderCreatedByDefaultWhenPropertyNotSet() {
        contextRunner
            .withBean(DataSource.class, TestDataSource::new)
            .run(context -> {
                // matchIfMissing = true, so bean should be created
                assertThat(context).hasSingleBean(MeterBinder.class);
            });
    }

    @Test
    void testMeterBinderHandlesNonHikariDataSource() {
        contextRunner
            .withPropertyValues("observability.metrics.database-enabled=true")
            .withBean(DataSource.class, TestDataSource::new)
            .run(context -> {
                assertThat(context).hasSingleBean(MeterBinder.class);

                MeterBinder binder = context.getBean(MeterBinder.class);
                SimpleMeterRegistry registry = new SimpleMeterRegistry();

                // Should not throw exception even with non-HikariCP DataSource
                binder.bindTo(registry);

                // No HikariCP metrics should be registered (not HikariCP)
                assertThat(registry.find("hikaricp.connections.active").gauge()).isNull();
            });
    }

    @Test
    void testMeterBinderWorks() {
        contextRunner
            .withPropertyValues("observability.metrics.database-enabled=true")
            .withBean(DataSource.class, TestDataSource::new)
            .run(context -> {
                assertThat(context).hasSingleBean(MeterBinder.class);
                MeterBinder binder = context.getBean(MeterBinder.class);
                assertThat(binder).isNotNull();
            });
    }
}
