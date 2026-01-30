package com.imadattar.observability.export;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.text.StringSubstitutor;

/**
 * Service for exporting the complete observability stack (Grafana dashboards, Prometheus config, Docker Compose).
 * This allows users to easily get all monitoring infrastructure files.
 *
 * @since 1.0.0
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "observability.stack-export", name = "enabled", havingValue = "true", matchIfMissing = false)
public class ObservabilityStackExportService {

    @Value("${spring.application.name:spring-boot-app}")
    private String applicationName;

    @Value("${observability.service.name:spring-boot-app}")
    private String serviceName;

    @Value("${observability.service.environment:development}")
    private String environment;

    @Value("${observability.service.team:platform-team}")
    private String team;

    private final PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

    @PostConstruct
    public void init() {
        // Use applicationName as fallback for serviceName if it's still the default
        if ("spring-boot-app".equals(serviceName) && !applicationName.equals(serviceName)) {
            serviceName = applicationName;
        }

        // Validate configuration values to prevent YAML/JSON injection
        validateConfigValue(serviceName, "observability.service.name");
        validateConfigValue(applicationName, "spring.application.name");
        validateConfigValue(environment, "observability.service.environment");
        validateConfigValue(team, "observability.service.team");

        log.info("✅ ObservabilityStackExportService bean created");
        log.info("   Application: {}", applicationName);
        log.info("   Service: {}", serviceName);
        log.info("   Environment: {}", environment);
        log.info("   Team: {}", team);
    }

    /**
     * Validate configuration value to prevent YAML/JSON injection.
     * Only allows alphanumeric characters, hyphens, underscores, and dots.
     *
     * @param value the value to validate
     * @param propertyName the property name for error messages
     * @throws IllegalArgumentException if value contains invalid characters
     */
    private void validateConfigValue(String value, String propertyName) {
        if (value == null || value.trim().isEmpty()) {
            log.warn("Configuration property '{}' is empty, using default", propertyName);
            return;
        }

        // Allow alphanumeric, hyphens, underscores, and dots only
        if (!value.matches("^[a-zA-Z0-9._-]+$")) {
            throw new IllegalArgumentException(
                String.format("Invalid characters in configuration property '%s': '%s'. " +
                    "Only alphanumeric characters, hyphens, underscores, and dots are allowed.",
                    propertyName, value)
            );
        }
    }

    /**
     * Export the complete monitoring stack as a ZIP file.
     *
     * @return ZIP file content as byte array
     * @throws IOException if file operations fail
     */
    public byte[] exportMonitoringStack() throws IOException {
        log.info("Exporting observability monitoring stack for application: {}", applicationName);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zipOut = new ZipOutputStream(baos)) {

            // Export Grafana dashboards
            exportGrafanaDashboards(zipOut);

            // Export Prometheus configuration
            exportPrometheusConfig(zipOut);

            // Export Docker Compose
            exportDockerCompose(zipOut);

            // Export provisioning files
            exportGrafanaProvisioning(zipOut);

            // Export startup script
            exportStartupScript(zipOut);

            // Export README
            exportReadme(zipOut);
        }

        log.info("✅ Monitoring stack exported successfully ({} bytes)", baos.size());
        return baos.toByteArray();
    }

    /**
     * Export monitoring stack to a directory on the filesystem.
     *
     * @param targetDirectory target directory path
     * @throws IOException if file operations fail
     */
    public void exportToDirectory(String targetDirectory) throws IOException {
        Path targetPath = Paths.get(targetDirectory);
        if (!Files.exists(targetPath)) {
            Files.createDirectories(targetPath);
        }

        log.info("Exporting observability stack to directory: {}", targetDirectory);

        // Export Grafana dashboards
        exportGrafanaDashboardsToDirectory(targetPath);

        // Export Prometheus configuration
        exportPrometheusConfigToDirectory(targetPath);

        // Export Docker Compose
        exportDockerComposeToDirectory(targetPath);

        // Export provisioning files
        exportGrafanaProvisioningToDirectory(targetPath);

        // Export startup script
        exportStartupScriptToDirectory(targetPath);

        // Export README
        exportReadmeToDirectory(targetPath);

        log.info("✅ Monitoring stack exported to: {}", targetDirectory);
    }

    private void exportGrafanaDashboards(ZipOutputStream zipOut) throws IOException {
        Resource[] dashboards = resourceResolver.getResources("classpath:observability-stack/grafana-dashboards/*.json");

        for (Resource dashboard : dashboards) {
            String fileName = "grafana-provisioning/dashboards/json/" + dashboard.getFilename();
            try (InputStream is = dashboard.getInputStream()) {
                addFileToZip(zipOut, fileName, is);
            }
        }

        log.debug("Exported {} Grafana dashboards", dashboards.length);
    }

    private void exportPrometheusConfig(ZipOutputStream zipOut) throws IOException {
        Resource prometheusConfig = resourceResolver.getResource("classpath:observability-stack/prometheus-config/prometheus.yml");

        // Read and customize Prometheus config with application details
        String configContent = readResourceAsString(prometheusConfig);
        String customizedConfig = customizePrometheusConfig(configContent);

        addStringToZip(zipOut, "prometheus.yml", customizedConfig);
        log.debug("Exported Prometheus configuration");
    }

    private void exportDockerCompose(ZipOutputStream zipOut) throws IOException {
        Resource dockerCompose = resourceResolver.getResource("classpath:observability-stack/docker-compose/docker-compose.yml");

        String composeContent = readResourceAsString(dockerCompose);
        addStringToZip(zipOut, "docker-compose.yml", composeContent);
        log.debug("Exported Docker Compose configuration");
    }

    private void exportGrafanaProvisioning(ZipOutputStream zipOut) throws IOException {
        // Datasource provisioning
        String datasourceConfig = """
                apiVersion: 1

                datasources:
                  - name: Prometheus
                    type: prometheus
                    access: proxy
                    url: http://prometheus:9090
                    isDefault: true
                    editable: true
                    jsonData:
                      httpMethod: POST
                      timeInterval: 15s
                """;
        addStringToZip(zipOut, "grafana-provisioning/datasources/prometheus.yml", datasourceConfig);

        // Dashboard provisioning
        String dashboardConfig = """
                apiVersion: 1

                providers:
                  - name: 'Spring Boot Observability'
                    orgId: 1
                    folder: 'Spring Boot'
                    type: file
                    disableDeletion: false
                    updateIntervalSeconds: 10
                    allowUiUpdates: true
                    options:
                      path: /etc/grafana/provisioning/dashboards/json
                      foldersFromFilesStructure: false
                """;
        addStringToZip(zipOut, "grafana-provisioning/dashboards/dashboards.yml", dashboardConfig);

        log.debug("Exported Grafana provisioning files");
    }

    private void exportStartupScript(ZipOutputStream zipOut) throws IOException {
        String script = generateStartupScript();
        addStringToZip(zipOut, "start-monitoring.sh", script);
        log.debug("Exported startup script");
    }

    private void exportReadme(ZipOutputStream zipOut) throws IOException {
        String readme = generateReadme();
        addStringToZip(zipOut, "README.md", readme);
        log.debug("Exported README");
    }

    // Export to directory methods
    private void exportGrafanaDashboardsToDirectory(Path targetPath) throws IOException {
        Path dashboardsPath = targetPath.resolve("grafana-provisioning/dashboards/json");
        Files.createDirectories(dashboardsPath);

        Resource[] dashboards = resourceResolver.getResources("classpath:observability-stack/grafana-dashboards/*.json");
        for (Resource dashboard : dashboards) {
            Path targetFile = dashboardsPath.resolve(dashboard.getFilename());
            try (InputStream is = dashboard.getInputStream()) {
                Files.copy(is, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void exportPrometheusConfigToDirectory(Path targetPath) throws IOException {
        Resource prometheusConfig = resourceResolver.getResource("classpath:observability-stack/prometheus-config/prometheus.yml");
        String configContent = readResourceAsString(prometheusConfig);
        String customizedConfig = customizePrometheusConfig(configContent);
        Files.writeString(targetPath.resolve("prometheus.yml"), customizedConfig);
    }

    private void exportDockerComposeToDirectory(Path targetPath) throws IOException {
        Resource dockerCompose = resourceResolver.getResource("classpath:observability-stack/docker-compose/docker-compose.yml");
        String composeContent = readResourceAsString(dockerCompose);
        Files.writeString(targetPath.resolve("docker-compose.yml"), composeContent);
    }

    private void exportGrafanaProvisioningToDirectory(Path targetPath) throws IOException {
        Path datasourcePath = targetPath.resolve("grafana-provisioning/datasources");
        Path dashboardProvPath = targetPath.resolve("grafana-provisioning/dashboards");
        Files.createDirectories(datasourcePath);
        Files.createDirectories(dashboardProvPath);

        String datasourceConfig = """
                apiVersion: 1

                datasources:
                  - name: Prometheus
                    type: prometheus
                    access: proxy
                    url: http://prometheus:9090
                    isDefault: true
                    editable: true
                    jsonData:
                      httpMethod: POST
                      timeInterval: 15s
                """;
        Files.writeString(datasourcePath.resolve("prometheus.yml"), datasourceConfig);

        String dashboardConfig = """
                apiVersion: 1

                providers:
                  - name: 'Spring Boot Observability'
                    orgId: 1
                    folder: 'Spring Boot'
                    type: file
                    disableDeletion: false
                    updateIntervalSeconds: 10
                    allowUiUpdates: true
                    options:
                      path: /etc/grafana/provisioning/dashboards/json
                      foldersFromFilesStructure: false
                """;
        Files.writeString(dashboardProvPath.resolve("dashboards.yml"), dashboardConfig);
    }

    private void exportStartupScriptToDirectory(Path targetPath) throws IOException {
        String script = generateStartupScript();
        Path scriptPath = targetPath.resolve("start-monitoring.sh");
        Files.writeString(scriptPath, script);
        scriptPath.toFile().setExecutable(true);
    }

    private void exportReadmeToDirectory(Path targetPath) throws IOException {
        String readme = generateReadme();
        Files.writeString(targetPath.resolve("README.md"), readme);
    }

    // Utility methods
    private void addFileToZip(ZipOutputStream zipOut, String fileName, InputStream inputStream) throws IOException {
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        inputStream.transferTo(zipOut);
        zipOut.closeEntry();
    }

    private void addStringToZip(ZipOutputStream zipOut, String fileName, String content) throws IOException {
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        zipOut.write(content.getBytes(StandardCharsets.UTF_8));
        zipOut.closeEntry();
    }

    private String readResourceAsString(Resource resource) throws IOException {
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String customizePrometheusConfig(String template) {
        Map<String, String> values = new HashMap<>();
        values.put("SERVICE_NAME", serviceName);
        values.put("APPLICATION_NAME", applicationName);
        values.put("ENVIRONMENT", environment);
        values.put("TEAM", team);

        // Use StringSubstitutor for efficient template replacement
        StringSubstitutor substitutor = new StringSubstitutor(values);

        // First apply variable substitution, then legacy string replacements for backwards compatibility
        String result = substitutor.replace(template);
        return result
                .replace("spring-boot-demo", serviceName)
                .replace("demo-app", serviceName)
                .replace("demo-observability-app", applicationName)
                .replace("service: 'demo-app'", "service: '" + serviceName + "'")
                .replace("application: 'demo-observability-app'", "application: '" + applicationName + "'")
                .replace("environment: 'development'", "environment: '" + environment + "'")
                .replace("team: 'platform'", "team: '" + team + "'");
    }

    private String generateStartupScript() {
        return """
                #!/bin/bash

                # Monitoring Stack Startup Script
                # Generated by Spring Boot Unified Observability Starter

                set -e

                echo "╔════════════════════════════════════════════════════════════════════╗"
                echo "║     🚀 Starting Observability Monitoring Stack                    ║"
                echo "╚════════════════════════════════════════════════════════════════════╝"
                echo ""

                # Check Docker
                if ! command -v docker &> /dev/null; then
                    echo "❌ Docker is not installed. Please install Docker Desktop."
                    exit 1
                fi

                if ! docker info &> /dev/null; then
                    echo "❌ Docker is not running. Please start Docker Desktop."
                    exit 1
                fi

                echo "✅ Docker is running"
                echo ""

                # Start Prometheus and Grafana
                echo "🚀 Starting Prometheus and Grafana..."
                docker-compose up -d

                echo ""
                echo "⏳ Waiting for services to be ready (15 seconds)..."
                sleep 15

                # Check Prometheus
                if curl -s http://localhost:9090/-/healthy > /dev/null 2>&1; then
                    echo "✅ Prometheus is ready: http://localhost:9090"
                else
                    echo "⏳ Prometheus is starting..."
                fi

                # Check Grafana
                if curl -s http://localhost:3000/api/health > /dev/null 2>&1; then
                    echo "✅ Grafana is ready: http://localhost:3000"
                else
                    echo "⏳ Grafana is starting..."
                fi

                echo ""
                echo "╔════════════════════════════════════════════════════════════════════╗"
                echo "║                     🎉 STACK IS READY! 🎉                          ║"
                echo "╚════════════════════════════════════════════════════════════════════╝"
                echo ""
                echo "📊 Access URLs:"
                echo "   • Prometheus:  http://localhost:9090"
                echo "   • Grafana:     http://localhost:3000 (admin/admin)"
                echo "   • Application: http://localhost:8080"
                echo "   • Metrics:     http://localhost:8080/actuator/prometheus"
                echo ""
                echo "📈 8 Grafana Dashboards available in the 'Spring Boot' folder"
                echo ""
                echo "🛑 To stop: docker-compose down"
                echo ""
                """;
    }

    private String generateReadme() {
        return String.format("""
                # %s - Observability Monitoring Stack

                This monitoring stack was automatically generated by the **Spring Boot Unified Observability Starter**.

                ## 📊 What's Included

                - **Prometheus** - Metrics collection and storage
                - **Grafana** - 8 pre-configured dashboards for visualization
                - **Docker Compose** - Easy one-command deployment
                - **Auto-provisioning** - Dashboards and datasources configured automatically

                ## 🚀 Quick Start

                ### 1. Start the Monitoring Stack

                ```bash
                ./start-monitoring.sh
                ```

                Or manually:

                ```bash
                docker-compose up -d
                ```

                ### 2. Access Grafana

                - **URL**: http://localhost:3000
                - **Login**: admin
                - **Password**: admin

                Navigate to **Dashboards → Browse → Spring Boot** to see the 8 dashboards.

                ### 3. Access Prometheus

                - **URL**: http://localhost:9090
                - **Targets**: http://localhost:9090/targets

                ## 📈 Available Dashboards

                1. **JVM Metrics** - Memory, GC, Threads, CPU
                2. **HTTP Metrics** - Request rate, Latency, Status codes
                3. **Database Metrics** - HikariCP connection pool
                4. **Application Health** - Overall health and availability
                5. **Business Metrics** - Custom business KPIs
                6. **Cache Metrics** - Cache performance
                7. **Distributed Tracing** - Trace analysis
                8. **Alerts Overview** - Active alerts monitoring

                ## 🔧 Configuration

                ### Application Configuration

                Your Spring Boot application should expose metrics on:
                - **Endpoint**: `/actuator/prometheus`
                - **Port**: `8080` (or update `prometheus.yml` if different)

                ### Prometheus Configuration

                The `prometheus.yml` file is already configured to scrape your application:

                ```yaml
                scrape_configs:
                  - job_name: '%s'
                    metrics_path: '/actuator/prometheus'
                    scrape_interval: 10s
                    static_configs:
                      - targets: ['host.docker.internal:8080']
                        labels:
                          service: '%s'
                          environment: '%s'
                          team: '%s'
                ```

                ## 📊 Accessing Metrics

                Once everything is running:

                1. Check your application health:
                   ```bash
                   curl http://localhost:8080/actuator/health
                   ```

                2. View raw Prometheus metrics:
                   ```bash
                   curl http://localhost:8080/actuator/prometheus
                   ```

                3. Open Grafana and explore the dashboards

                ## 🛑 Stopping the Stack

                ```bash
                docker-compose down
                ```

                To remove all data:

                ```bash
                docker-compose down -v
                ```

                ## 📚 Documentation

                For more information, see:
                - [Spring Boot Unified Observability Starter](https://github.com/your-repo/spring-boot-unified-observability-starter)
                - [Prometheus Documentation](https://prometheus.io/docs/)
                - [Grafana Documentation](https://grafana.com/docs/)

                ## 🎯 Next Steps

                1. Customize the dashboards in Grafana
                2. Set up alerting with Alertmanager (optional)
                3. Add custom business metrics to your application
                4. Configure trace export to Jaeger/Tempo for distributed tracing

                ---

                Generated by Spring Boot Unified Observability Starter
                Application: %s
                Environment: %s
                """,
                applicationName,
                serviceName,
                serviceName,
                environment,
                team,
                applicationName,
                environment
        );
    }

    /**
     * Get list of available dashboard names.
     *
     * @return list of dashboard descriptive names
     * @throws IOException if dashboard resources cannot be read
     */
    public java.util.List<String> getAvailableDashboards() throws IOException {
        Resource[] dashboardResources = resourceResolver.getResources("classpath:observability-stack/grafana-dashboards/*.json");

        java.util.List<String> dashboards = new java.util.ArrayList<>();
        for (Resource resource : dashboardResources) {
            String filename = resource.getFilename();
            if (filename != null) {
                // Convert filename to readable name: "jvm-metrics.json" -> "JVM Metrics"
                String readableName = filename.replace(".json", "").replace("-", " ");

                // Capitalize each word
                String[] words = readableName.split(" ");
                StringBuilder capitalized = new StringBuilder();
                for (String word : words) {
                    if (!word.isEmpty()) {
                        if (capitalized.length() > 0) {
                            capitalized.append(" ");
                        }
                        capitalized.append(Character.toUpperCase(word.charAt(0)))
                                   .append(word.substring(1));
                    }
                }
                dashboards.add(capitalized.toString());
            }
        }

        // Return sorted list
        java.util.Collections.sort(dashboards);
        return dashboards;
    }
}
