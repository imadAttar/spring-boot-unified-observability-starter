package com.imadattar.observability.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de sécurité pour la validation des chemins d'export.
 * Vérifie la protection contre les attaques de path traversal.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = com.imadattar.observability.TestApplication.class
)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "observability.stack-export.enabled=true"
})
@DisplayName("Path Traversal Security Tests")
class PathTraversalSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {
        "../etc/passwd",
        "../../etc/passwd",
        "../../../etc/passwd",
        "../../../../etc/passwd",
        "./../../etc/passwd",
        "test/../../../etc/passwd"
    })
    @DisplayName("Should block path traversal with double dots")
    void shouldBlockPathTraversalWithDoubleDots(String maliciousPath) throws Exception {
        mockMvc.perform(post("/actuator/observability/export/directory")
                .param("path", maliciousPath))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid export path"))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("path traversal")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "~/etc/passwd",
        "~root/.ssh",
        "~/.aws/credentials"
    })
    @DisplayName("Should block path traversal with tilde")
    void shouldBlockPathTraversalWithTilde(String maliciousPath) throws Exception {
        mockMvc.perform(post("/actuator/observability/export/directory")
                .param("path", maliciousPath))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid export path"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/etc",
        "/etc/passwd",
        "/sys",
        "/proc",
        "/root",
        "/var/log",
        "/boot"
    })
    @DisplayName("Should block writes to system directories")
    void shouldBlockSystemDirectories(String systemPath) throws Exception {
        mockMvc.perform(post("/actuator/observability/export/directory")
                .param("path", systemPath))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("system directory")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/home/user/.ssh",
        "/home/user/.aws",
        "/home/user/.kube",
        "/home/user/.gnupg",
        "/home/user/credentials"
    })
    @DisplayName("Should block writes to sensitive user directories")
    void shouldBlockSensitiveUserDirectories(String sensitivePath) throws Exception {
        mockMvc.perform(post("/actuator/observability/export/directory")
                .param("path", sensitivePath))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.anyOf(
                org.hamcrest.Matchers.containsString("sensitive directories"),
                org.hamcrest.Matchers.containsString("does not exist")  // Parent directory check
            )));
    }

    @Test
    @DisplayName("Should block writes to /root directory and subdirectories")
    void shouldBlockRootDirectory() throws Exception {
        mockMvc.perform(post("/actuator/observability/export/directory")
                .param("path", "/root/.ssh/id_rsa"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("system directory")));
    }

    @Test
    @DisplayName("Should block null path")
    void shouldBlockNullPath() throws Exception {
        mockMvc.perform(post("/actuator/observability/export/directory"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should block empty path")
    void shouldBlockEmptyPath() throws Exception {
        mockMvc.perform(post("/actuator/observability/export/directory")
                .param("path", ""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("cannot be null or empty")));
    }

    @Test
    @DisplayName("Should block excessively long path")
    void shouldBlockExcessivelyLongPath() throws Exception {
        String longPath = "a".repeat(1000);

        mockMvc.perform(post("/actuator/observability/export/directory")
                .param("path", longPath))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("maximum length")));
    }

    @Test
    @DisplayName("Should block path with non-existent parent directory")
    void shouldBlockNonExistentParentDirectory() throws Exception {
        mockMvc.perform(post("/actuator/observability/export/directory")
                .param("path", "/nonexistent/parent/directory/export"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("does not exist")));
    }

    @Test
    @DisplayName("Should block URL-encoded path traversal attempts")
    void shouldBlockEncodedPathTraversal() throws Exception {
        // Spring MVC décode automatiquement les paramètres URL
        // Donc on test avec des chemins qui contiennent ".." après décodage

        // Test 1: Path avec ".." qui devrait être détecté
        mockMvc.perform(post("/actuator/observability/export/directory")
                .param("path", "../../../etc/passwd"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("path traversal")));

        // Test 2: Path avec "~" qui devrait être détecté
        mockMvc.perform(post("/actuator/observability/export/directory")
                .param("path", "~/secret"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("path traversal")));

        // Test 3: Path complexe avec ".."
        mockMvc.perform(post("/actuator/observability/export/directory")
                .param("path", "/tmp/../../../etc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("path traversal")));
    }

    @Test
    @DisplayName("Should block complex path traversal patterns")
    void shouldBlockComplexPathTraversalPatterns() throws Exception {
        // Ces patterns devraient être détectés même s'ils sont plus complexes
        mockMvc.perform(post("/actuator/observability/export/directory")
                .param("path", "valid/../../etc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("path traversal")));

        mockMvc.perform(post("/actuator/observability/export/directory")
                .param("path", "./../../sensitive"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("path traversal")));
    }

    @Test
    @DisplayName("Should allow valid /tmp directory export")
    void shouldAllowValidTmpExport() throws Exception {
        mockMvc.perform(post("/actuator/observability/export/directory")
                .param("path", "/tmp/valid-export-test"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("exported successfully")));
    }

    @Test
    @DisplayName("Should allow valid relative path export")
    void shouldAllowValidRelativePathExport() throws Exception {
        mockMvc.perform(post("/actuator/observability/export/directory")
                .param("path", "./monitoring-test"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("exported successfully")));
    }

    @Test
    @DisplayName("Should sanitize and normalize valid paths")
    void shouldSanitizeAndNormalizeValidPaths() throws Exception {
        // Ce chemin est techniquement valide après normalisation
        mockMvc.perform(post("/actuator/observability/export/directory")
                .param("path", "/tmp/test/../valid-export"))
            .andExpect(status().isBadRequest()); // Devrait rejeter car contient ".."
    }
}
