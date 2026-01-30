package com.imadattar.observability.export;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

/**
 * REST API controller for exporting the complete observability monitoring stack.
 * <p>
 * This controller provides HTTP endpoints to retrieve all monitoring infrastructure files embedded
 * in the starter JAR, including Grafana dashboards, Prometheus configuration, Docker Compose setup,
 * and startup scripts. All exported files are automatically customized with your application's
 * configuration parameters.
 * </p>
 *
 * <h2>Available Endpoints</h2>
 * <ul>
 *   <li><b>GET /actuator/observability/export</b> - Download monitoring stack as ZIP file</li>
 *   <li><b>POST /actuator/observability/export/directory</b> - Export stack to filesystem directory</li>
 *   <li><b>GET /actuator/observability/info</b> - Get information about available dashboards</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Download as ZIP:</h3>
 * <pre>{@code
 * curl -o monitoring-stack.zip http://localhost:8080/actuator/observability/export
 * unzip monitoring-stack.zip -d monitoring/
 * cd monitoring && ./start-monitoring.sh
 * }</pre>
 *
 * <h3>Export to Directory:</h3>
 * <pre>{@code
 * curl -X POST "http://localhost:8080/actuator/observability/export/directory?path=./monitoring"
 * cd monitoring && ./start-monitoring.sh
 * }</pre>
 *
 * <h3>Get Info:</h3>
 * <pre>{@code
 * curl http://localhost:8080/actuator/observability/info | jq
 * }</pre>
 *
 * <h2>Exported Content</h2>
 * <p>The exported monitoring stack includes:</p>
 * <ul>
 *   <li>8 Grafana dashboards (JVM, HTTP, Database, Health, Business, Cache, Tracing, Alerts)</li>
 *   <li>Prometheus configuration (customized with your app name and labels)</li>
 *   <li>Docker Compose file (Prometheus + Grafana with auto-provisioning)</li>
 *   <li>Grafana provisioning files (datasources and dashboards)</li>
 *   <li>Startup script (one-command launch)</li>
 *   <li>Complete documentation (README.md)</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * <p>Control endpoint availability:</p>
 * <pre>
 * observability:
 *   export:
 *     endpoint-enabled: true  # Enable REST endpoints (default: true)
 *
 * management:
 *   endpoints:
 *     web:
 *       exposure:
 *         include: observability  # Expose observability endpoints
 * </pre>
 *
 * @see ObservabilityStackExportService
 * @see ObservabilityStackAutoExporter
 * @since 1.0.0
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "observability.stack-export", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ObservabilityStackExportController {

    private static final String EXPORT_BASE_PATH = "/actuator/observability";
    private static final String EXPORT_ZIP_PATH = EXPORT_BASE_PATH + "/export";
    private static final String EXPORT_DIR_PATH = EXPORT_BASE_PATH + "/export/directory";
    private static final String INFO_PATH = EXPORT_BASE_PATH + "/info";

    // Security: Blocked system directories
    private static final Set<String> BLOCKED_PATHS = Set.of(
        "/etc", "/sys", "/proc", "/boot", "/root", "/var/log",
        "C:\\Windows", "C:\\Program Files"
    );

    private static final long MAX_PATH_LENGTH = 500;

    private final ObservabilityStackExportService exportService;

    /**
     * Download the complete monitoring stack as a ZIP file.
     *
     * <p>This endpoint returns a ZIP file containing:</p>
     * <ul>
     *   <li>8 Grafana dashboards (JSON)</li>
     *   <li>Prometheus configuration (prometheus.yml)</li>
     *   <li>Docker Compose file (docker-compose.yml)</li>
     *   <li>Grafana provisioning files</li>
     *   <li>Startup script (start-monitoring.sh)</li>
     *   <li>README with instructions</li>
     * </ul>
     *
     * <p>Usage:</p>
     * <pre>{@code
     * curl -o monitoring-stack.zip http://localhost:8080/actuator/observability/export
     * unzip monitoring-stack.zip -d monitoring/
     * cd monitoring && ./start-monitoring.sh
     * }</pre>
     *
     * <p><b>Security Note:</b> This endpoint should be protected with authentication in production environments.</p>
     *
     * @return ZIP file containing the complete monitoring stack
     */
    @GetMapping(value = EXPORT_ZIP_PATH, produces = "application/zip")
    public ResponseEntity<StreamingResponseBody> exportMonitoringStack() {
        log.info("📦 Exporting monitoring stack via REST endpoint");

        try {
            byte[] zipContent = exportService.exportMonitoringStack();

            StreamingResponseBody stream = outputStream -> {
                outputStream.write(zipContent);
                outputStream.flush();
            };

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=monitoring-stack.zip")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(zipContent.length)
                    .body(stream);
        } catch (IOException e) {
            log.error("Failed to export monitoring stack", e);
            throw new ExportException("Failed to export monitoring stack: " + e.getMessage(), e);
        }
    }

    /**
     * Export the monitoring stack to a directory on the server filesystem.
     *
     * <p>This endpoint is useful for automated deployment scripts.</p>
     *
     * <p><b>Security Warnings:</b></p>
     * <ul>
     *   <li>This endpoint should be protected with authentication in production</li>
     *   <li>Path validation prevents writes to system directories</li>
     *   <li>Consider disabling this endpoint in production: observability.export.endpoint-enabled=false</li>
     * </ul>
     *
     * <p>Usage:</p>
     * <pre>{@code
     * curl -X POST "http://localhost:8080/actuator/observability/export/directory?path=/opt/monitoring"
     * }</pre>
     *
     * @param path target directory path (will be validated for security)
     * @return success message with export location
     */
    @PostMapping(EXPORT_DIR_PATH)
    public ResponseEntity<String> exportToDirectory(@RequestParam(required = false) String path) {
        log.info("📂 Export request received for directory: {}", path);

        try {
            // Security: Validate path before processing
            validateExportPath(path);

            Path normalizedPath = Paths.get(path).toAbsolutePath().normalize();
            String absolutePath = normalizedPath.toString();

            log.info("📂 Exporting monitoring stack to validated directory: {}", absolutePath);
            exportService.exportToDirectory(absolutePath);

            String message = String.format("""
                    ✅ Monitoring stack exported successfully!

                    Location: %s

                    Next steps:
                    1. cd %s
                    2. ./start-monitoring.sh
                    3. Open Grafana: http://localhost:3000

                    ⚠️  IMPORTANT: Change Grafana default credentials after first login!
                    """, absolutePath, absolutePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(message);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid export path provided: {}", path, e);
            throw new InvalidPathException(e.getMessage());
        } catch (IOException e) {
            log.error("Failed to export monitoring stack to directory: {}", path, e);
            throw new ExportException("Failed to export monitoring stack: " + e.getMessage(), e);
        }
    }

    /**
     * Get information about the observability stack.
     *
     * <p>This endpoint provides metadata about available dashboards, export endpoints, and quick start instructions.</p>
     *
     * @return information about available dashboards and configuration
     */
    @GetMapping(value = INFO_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ObservabilityInfo> getInfo() {
        try {
            List<String> availableDashboards = exportService.getAvailableDashboards();

            ObservabilityInfo info = ObservabilityInfo.builder()
                    .dashboardCount(availableDashboards.size())
                    .dashboards(availableDashboards.toArray(new String[0]))
                    .exportEndpoint(EXPORT_ZIP_PATH)
                    .exportDirectoryEndpoint(EXPORT_DIR_PATH)
                    .quickStart("""
                            1. Download: curl -o monitoring-stack.zip http://localhost:8080/actuator/observability/export
                            2. Extract: unzip monitoring-stack.zip -d monitoring/
                            3. Start: cd monitoring && ./start-monitoring.sh
                            4. Open Grafana: http://localhost:3000
                            5. Login with default credentials and CHANGE THEM immediately!
                            """)
                    .securityWarning("These endpoints should be protected with authentication in production. " +
                            "Consider setting observability.export.endpoint-enabled=false in production environments.")
                    .build();

            return ResponseEntity.ok(info);
        } catch (IOException e) {
            log.error("Failed to load dashboard resources: {}", e.getMessage(), e);
            // Return minimal info on error
            ObservabilityInfo fallbackInfo = ObservabilityInfo.builder()
                    .dashboardCount(8)
                    .dashboards(new String[]{"JVM Metrics", "HTTP Metrics", "Database Metrics",
                            "Application Health", "Business Metrics", "Cache Metrics",
                            "Distributed Tracing", "Alerts Overview"})
                    .exportEndpoint(EXPORT_ZIP_PATH)
                    .exportDirectoryEndpoint(EXPORT_DIR_PATH)
                    .quickStart("See documentation for usage instructions")
                    .securityWarning("Dashboard resources could not be loaded. Using default list.")
                    .build();
            return ResponseEntity.ok(fallbackInfo);
        }
    }

    /**
     * Validates the export path for security vulnerabilities.
     *
     * <p>Prevents:</p>
     * <ul>
     *   <li>Path traversal attacks (../, ~)</li>
     *   <li>Writes to system directories (/etc, /sys, /proc, etc.)</li>
     *   <li>Writes to sensitive user directories (.ssh, .aws, etc.)</li>
     *   <li>Excessively long paths (>500 characters)</li>
     * </ul>
     *
     * @param path the path to validate
     * @throws IllegalArgumentException if path validation fails
     */
    private void validateExportPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Export path cannot be null or empty");
        }

        if (path.length() > MAX_PATH_LENGTH) {
            throw new IllegalArgumentException("Export path exceeds maximum length of " + MAX_PATH_LENGTH + " characters");
        }

        // Normalize path FIRST to handle encoded characters and relative paths
        Path normalizedPath;
        try {
            normalizedPath = Paths.get(path).toAbsolutePath().normalize();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid path format: " + path, e);
        }

        String absolutePath = normalizedPath.toString();
        String originalPath = path;

        // Check if normalization changed the path significantly (path traversal attempt)
        // After normalization, ".." and "~" patterns should be resolved
        // If the original path contained these, it's likely a traversal attempt
        if (originalPath.contains("..") || originalPath.contains("~")) {
            throw new IllegalArgumentException("Invalid path: path traversal patterns detected (.., ~)");
        }

        // Additional check: if normalized path is very different from original, suspect traversal
        Path originalAbsolute = Paths.get(originalPath).toAbsolutePath();
        if (!absolutePath.equals(originalAbsolute.toString()) && originalPath.contains("..")) {
            throw new IllegalArgumentException("Invalid path: path traversal detected after normalization");
        }

        // Prevent writing to blocked system directories
        for (String blockedPath : BLOCKED_PATHS) {
            if (absolutePath.startsWith(blockedPath)) {
                throw new IllegalArgumentException("Cannot export to system directory: " + blockedPath);
            }
        }

        // Prevent writing to sensitive user directories
        if (absolutePath.contains("/.ssh") ||
            absolutePath.contains("/.aws") ||
            absolutePath.contains("/.kube") ||
            absolutePath.contains("/.gnupg") ||
            absolutePath.endsWith("/.ssh") ||
            absolutePath.endsWith("/.aws") ||
            absolutePath.endsWith("/.kube") ||
            absolutePath.endsWith("/.gnupg") ||
            absolutePath.contains("/credentials")) {
            throw new IllegalArgumentException("Cannot export to sensitive directories (.ssh, .aws, .kube, .gnupg, credentials)");
        }

        // Validate parent directory exists (if not root)
        Path parentPath = normalizedPath.getParent();
        if (parentPath != null && !Files.exists(parentPath)) {
            throw new IllegalArgumentException("Parent directory does not exist: " + parentPath);
        }

        log.debug("Path validation successful: {}", absolutePath);
    }

    /**
     * Exception handler for invalid path errors.
     *
     * @param e the exception
     * @return error response with HTTP 400
     */
    @ExceptionHandler(InvalidPathException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPath(InvalidPathException e) {
        log.warn("Invalid path provided: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Invalid export path", e.getMessage()));
    }

    /**
     * Exception handler for export failures.
     *
     * @param e the exception
     * @return error response with HTTP 500
     */
    @ExceptionHandler(ExportException.class)
    public ResponseEntity<ErrorResponse> handleExportException(ExportException e) {
        log.error("Export failed: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Export failed", e.getMessage()));
    }

    /**
     * Generic exception handler for unexpected errors.
     *
     * @param e the exception
     * @return error response with HTTP 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Unexpected error in ObservabilityStackExportController", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Internal server error", "An unexpected error occurred. Check server logs."));
    }

    /**
     * Error response for API errors.
     */
    @Getter
    @ToString
    @RequiredArgsConstructor
    public static class ErrorResponse {
        private final String error;
        private final String message;
    }

    /**
     * Information about the observability stack.
     */
    @lombok.Builder
    @Getter
    @ToString
    public static class ObservabilityInfo {
        private final int dashboardCount;
        private final String[] dashboards;
        private final String exportEndpoint;
        private final String exportDirectoryEndpoint;
        private final String quickStart;
        private final String securityWarning;
    }

    /**
     * Exception thrown when export path validation fails.
     */
    public static class InvalidPathException extends RuntimeException {
        public InvalidPathException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when export operation fails.
     */
    public static class ExportException extends RuntimeException {
        public ExportException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
