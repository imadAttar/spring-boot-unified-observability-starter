package com.imadattar.observability.export;

import com.imadattar.observability.config.ObservabilityProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Automatically exports the monitoring stack to a directory when the application starts.
 * <p>
 * Enable with:
 * <pre>{@code
 * observability:
 *   stack-export:
 *     enabled: true
 *     export-on-startup: true
 *     export-path: ./monitoring
 *     personalize-config: true
 * }</pre>
 *
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "observability.stack-export", name = "export-on-startup", havingValue = "true")
public class ObservabilityStackAutoExporter {

    private final ObservabilityStackExportService exportService;
    private final ObservabilityProperties properties;

    @PostConstruct
    public void init() {
        log.info("✅ ObservabilityStackAutoExporter bean created - auto-export will trigger on ApplicationReadyEvent");
        log.info("   Export path: {}", properties.getStackExport().getExportPath());
        log.info("   Export on startup: {}", properties.getStackExport().isExportOnStartup());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void autoExportOnStartup() {
        try {
            String exportPath = properties.getStackExport().getExportPath();
            Path absolutePath = resolveExportPath(exportPath);

            log.info("📁 Working directory: {}", System.getProperty("user.dir"));
            log.info("🚀 Auto-exporting observability monitoring stack to: {}", absolutePath);

            exportService.exportToDirectory(absolutePath.toString());

            log.info("""

                    ╔════════════════════════════════════════════════════════════════════╗
                    ║     ✅ Observability Monitoring Stack Exported Successfully       ║
                    ╚════════════════════════════════════════════════════════════════════╝

                    📂 Location: {}

                    📊 What's inside:
                       • 8 Grafana Dashboards
                       • Prometheus Configuration
                       • Docker Compose Setup
                       • 20 Prometheus Alert Rules
                       • Complete Documentation

                    🚀 Quick Start:
                       1. cd {}
                       2. docker-compose up -d
                       3. Open Grafana: http://localhost:3000 (admin/admin)

                    📚 See README.md for detailed instructions
                    ╚════════════════════════════════════════════════════════════════════╝
                    """, absolutePath, absolutePath);

        } catch (IOException e) {
            String exportPath = properties.getStackExport().getExportPath();
            log.error("❌ Failed to auto-export monitoring stack to: {}", exportPath, e);
            log.warn("💡 You can manually export via REST API:");
            log.warn("   curl http://localhost:8080/observability/export/all?path={}", exportPath);
        }
    }

    /**
     * Resolves export path supporting multiple formats:
     * - Relative paths: ./monitoring, monitoring
     * - Absolute paths: /opt/monitoring, C:\monitoring
     * - Home directory: ~/monitoring
     *
     * @param exportPath the configured export path
     * @return resolved absolute path
     */
    private Path resolveExportPath(String exportPath) {
        if (exportPath == null || exportPath.trim().isEmpty()) {
            exportPath = "./monitoring";
        }

        // Handle home directory expansion (~)
        if (exportPath.startsWith("~")) {
            String userHome = System.getProperty("user.home");
            exportPath = exportPath.replaceFirst("^~", userHome);
        }

        return Paths.get(exportPath).toAbsolutePath();
    }
}
