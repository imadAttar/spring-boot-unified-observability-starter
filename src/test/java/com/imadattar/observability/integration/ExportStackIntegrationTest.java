package com.imadattar.observability.integration;

import com.imadattar.observability.export.ObservabilityStackExportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration pour l'export du stack de monitoring.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "observability.stack-export.enabled=true",
    "spring.application.name=test-export-app"
})
@DisplayName("Export Stack Integration Tests")
class ExportStackIntegrationTest {

    @Autowired
    private ObservabilityStackExportService exportService;

    @Test
    @DisplayName("Should export monitoring stack as ZIP successfully")
    void shouldExportMonitoringStackAsZip() throws IOException {
        byte[] zipContent = exportService.exportMonitoringStack();

        assertThat(zipContent).isNotEmpty();
        assertThat(zipContent.length).isGreaterThan(1000);
    }

    @Test
    @DisplayName("Should export ZIP containing all required files")
    void shouldExportZipWithRequiredFiles() throws IOException {
        byte[] zipContent = exportService.exportMonitoringStack();

        // Vérifier le contenu du ZIP
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipContent))) {
            boolean hasDockerCompose = false;
            boolean hasPrometheus = false;
            boolean hasStartScript = false;
            boolean hasReadme = false;
            int dashboardCount = 0;

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.equals("docker-compose.yml")) hasDockerCompose = true;
                if (name.equals("prometheus.yml")) hasPrometheus = true;
                if (name.equals("start-monitoring.sh")) hasStartScript = true;
                if (name.equals("README.md")) hasReadme = true;
                if (name.contains("grafana-provisioning/dashboards/json/") && name.endsWith(".json")) {
                    dashboardCount++;
                }
            }

            assertThat(hasDockerCompose).isTrue();
            assertThat(hasPrometheus).isTrue();
            assertThat(hasStartScript).isTrue();
            assertThat(hasReadme).isTrue();
            assertThat(dashboardCount).isEqualTo(9);
        }
    }

    @Test
    @DisplayName("Should list all available dashboards")
    void shouldListAllDashboards() throws IOException {
        List<String> dashboards = exportService.getAvailableDashboards();

        assertThat(dashboards).hasSize(9);
        assertThat(dashboards).contains(
            "Alerts Overview",
            "Application Health",
            "Business Metrics",
            "Cache Metrics",
            "Database Metrics",
            "Distributed Tracing",
            "Http Metrics",
            "Jvm Metrics"
        );
    }

    @Test
    @DisplayName("Should export to directory successfully")
    void shouldExportToDirectory(@TempDir Path tempDir) throws IOException {
        String exportPath = tempDir.toString();

        exportService.exportToDirectory(exportPath);

        // Vérifier que tous les fichiers sont créés
        assertThat(tempDir.resolve("docker-compose.yml")).exists();
        assertThat(tempDir.resolve("prometheus.yml")).exists();
        assertThat(tempDir.resolve("start-monitoring.sh")).exists();
        assertThat(tempDir.resolve("README.md")).exists();

        // Vérifier les répertoires
        assertThat(tempDir.resolve("grafana-provisioning")).exists();
        assertThat(tempDir.resolve("grafana-provisioning/dashboards")).exists();
        assertThat(tempDir.resolve("grafana-provisioning/dashboards/json")).exists();
        assertThat(tempDir.resolve("grafana-provisioning/datasources")).exists();
    }

    @Test
    @DisplayName("Should export 8 Grafana dashboards to directory")
    void shouldExport8DashboardsToDirectory(@TempDir Path tempDir) throws IOException {
        exportService.exportToDirectory(tempDir.toString());

        Path dashboardsDir = tempDir.resolve("grafana-provisioning/dashboards/json");
        assertThat(dashboardsDir).exists();

        long dashboardCount = Files.list(dashboardsDir)
            .filter(path -> path.toString().endsWith(".json"))
            .count();

        assertThat(dashboardCount).isEqualTo(9);
    }

    @Test
    @DisplayName("Should export executable start script")
    void shouldExportExecutableStartScript(@TempDir Path tempDir) throws IOException {
        exportService.exportToDirectory(tempDir.toString());

        Path startScript = tempDir.resolve("start-monitoring.sh");
        assertThat(startScript).exists();
        assertThat(Files.isExecutable(startScript)).isTrue();
    }

    @Test
    @DisplayName("Should customize Prometheus config with service name")
    void shouldCustomizePrometheusConfig(@TempDir Path tempDir) throws IOException {
        exportService.exportToDirectory(tempDir.toString());

        Path prometheusConfig = tempDir.resolve("prometheus.yml");
        String content = Files.readString(prometheusConfig);

        assertThat(content).contains("test-export-app");
        assertThat(content).contains("job_name:");
        assertThat(content).contains("metrics_path: '/actuator/prometheus'");
    }

    @Test
    @DisplayName("Should create README with service information")
    void shouldCreateReadmeWithServiceInfo(@TempDir Path tempDir) throws IOException {
        exportService.exportToDirectory(tempDir.toString());

        Path readme = tempDir.resolve("README.md");
        String content = Files.readString(readme);

        assertThat(content).contains("test-export-app");
        assertThat(content).contains("Quick Start");
        assertThat(content).contains("Grafana");
        assertThat(content).contains("Prometheus");
    }

    @Test
    @DisplayName("Should create Grafana provisioning files")
    void shouldCreateGrafanaProvisioningFiles(@TempDir Path tempDir) throws IOException {
        exportService.exportToDirectory(tempDir.toString());

        // Datasource provisioning
        Path datasourceConfig = tempDir.resolve("grafana-provisioning/datasources/prometheus.yml");
        assertThat(datasourceConfig).exists();
        String datasourceContent = Files.readString(datasourceConfig);
        assertThat(datasourceContent).contains("Prometheus");
        assertThat(datasourceContent).contains("prometheus:9090");

        // Dashboard provisioning
        Path dashboardConfig = tempDir.resolve("grafana-provisioning/dashboards/dashboards.yml");
        assertThat(dashboardConfig).exists();
        String dashboardContent = Files.readString(dashboardConfig);
        assertThat(dashboardContent).contains("Spring Boot Observability");
    }

    @Test
    @DisplayName("Should handle multiple exports to same directory")
    void shouldHandleMultipleExportsToSameDirectory(@TempDir Path tempDir) throws IOException {
        // Premier export
        exportService.exportToDirectory(tempDir.toString());
        assertThat(tempDir.resolve("docker-compose.yml")).exists();

        // Deuxième export (devrait écraser les fichiers)
        exportService.exportToDirectory(tempDir.toString());
        assertThat(tempDir.resolve("docker-compose.yml")).exists();

        // Vérifier que les fichiers sont toujours valides
        Path prometheusConfig = tempDir.resolve("prometheus.yml");
        assertThat(Files.size(prometheusConfig)).isGreaterThan(0);
    }
}
