package com.imadattar.observability.export;

import com.imadattar.observability.config.ObservabilityProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ObservabilityStackAutoExporter.
 * Verifies that auto-export works correctly on application startup.
 */
@SpringBootTest(classes = com.imadattar.observability.TestApplication.class)
@TestPropertySource(properties = {
    "observability.stack-export.enabled=true",
    "observability.stack-export.export-on-startup=true",
    "observability.stack-export.export-path=${java.io.tmpdir}/obs-auto-export-test"
})
class ObservabilityStackAutoExporterTest {

    @Autowired
    private ObservabilityProperties properties;

    @Test
    void shouldAutoExportOnStartup() {
        Path exportPath = Path.of(properties.getStackExport().getExportPath());
        // The auto-exporter runs on ApplicationReadyEvent, so files should exist
        assertThat(exportPath).exists();
        assertThat(exportPath.resolve("docker-compose.yml")).exists();
        assertThat(exportPath.resolve("prometheus.yml")).exists();
    }

    @Test
    void shouldExportAllDashboards() {
        Path exportPath = Path.of(properties.getStackExport().getExportPath());
        Path dashboardsDir = exportPath.resolve("grafana-provisioning/dashboards/json");
        assertThat(dashboardsDir).exists();
        assertThat(dashboardsDir.toFile().listFiles()).hasSizeGreaterThanOrEqualTo(9);
    }

    @Test
    void shouldExportGrafanaProvisioning() {
        Path exportPath = Path.of(properties.getStackExport().getExportPath());
        assertThat(exportPath.resolve("grafana-provisioning/datasources/prometheus.yml")).exists();
        assertThat(exportPath.resolve("grafana-provisioning/dashboards/dashboards.yml")).exists();
    }
}
