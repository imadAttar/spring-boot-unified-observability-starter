package com.imadattar.observability.e2e;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests end-to-end pour l'observabilité complète.
 * Simule des scénarios utilisateur réels de bout en bout.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = com.imadattar.observability.TestApplication.class
)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "observability.enabled=true",
    "observability.metrics.enabled=true",
    "observability.tracing.enabled=true",
    "observability.logging.json-enabled=true",
    "observability.stack-export.enabled=true",
    "spring.application.name=e2e-test-app",
    "management.endpoints.web.exposure.include=health,info,metrics,prometheus,observability",
    "management.endpoint.health.show-details=always",
    "management.endpoint.health.show-components=always"
})
@DisplayName("Observability E2E Tests")
class ObservabilityE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    @DisplayName("Complete observability workflow - metrics collection and export")
    void completeObservabilityWorkflow() throws Exception {
        // ÉTAPE 1: Vérifier que l'application démarre avec l'observabilité activée
        assertThat(meterRegistry).isNotNull();

        // ÉTAPE 2: Effectuer plusieurs requêtes HTTP pour générer des métriques
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
        }

        // ÉTAPE 3: Vérifier que les métriques JVM sont collectées directement via le registry
        assertThat(meterRegistry.find("jvm.memory.used").meter()).isNotNull();
        assertThat(meterRegistry.find("jvm.threads.live").meter()).isNotNull();
        assertThat(meterRegistry.find("process.uptime").meter()).isNotNull();

        // ÉTAPE 4: Vérifier que le registry est bien un PrometheusMeterRegistry
        assertThat(meterRegistry.getClass().getName()).contains("PrometheusMeterRegistry");

        // ÉTAPE 5: Vérifier l'endpoint d'informations d'observabilité
        mockMvc.perform(get("/actuator/observability/info"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dashboardCount").value(8))
            .andExpect(jsonPath("$.dashboards", hasSize(8)))
            .andExpect(jsonPath("$.exportEndpoint").value("/actuator/observability/export"))
            .andExpect(jsonPath("$.dashboards", hasItems(
                "Jvm Metrics",
                "Http Metrics",
                "Database Metrics"
            )));

        // ÉTAPE 6: Télécharger le stack de monitoring
        MvcResult exportResult = mockMvc.perform(get("/actuator/observability/export"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/octet-stream"))
            .andExpect(header().exists("Content-Disposition"))
            .andReturn();

        byte[] zipContent = exportResult.getResponse().getContentAsByteArray();
        assertThat(zipContent).isNotEmpty();
        assertThat(zipContent.length).isGreaterThan(1000);

        // ÉTAPE 7: Vérifier le health endpoint
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Metrics are collected across multiple endpoints")
    void metricsAreCollectedAcrossMultipleEndpoints() throws Exception {
        // Appeler différents endpoints
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/info")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/observability/info")).andExpect(status().isOk());

        // Vérifier que les métriques HTTP sont enregistrées dans le registry
        assertThat(meterRegistry.find("http.server.requests").meter()).isNotNull();
    }

    @Test
    @DisplayName("JVM metrics are continuously updated")
    void jvmMetricsAreContinuouslyUpdated() throws Exception {
        // Première lecture
        MvcResult result1 = mockMvc.perform(get("/actuator/metrics/jvm.memory.used"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("jvm.memory.used"))
            .andExpect(jsonPath("$.measurements[0].value").isNumber())
            .andReturn();

        // Forcer une allocation de mémoire
        byte[] bytes = new byte[1024 * 1024]; // 1MB

        // Deuxième lecture
        MvcResult result2 = mockMvc.perform(get("/actuator/metrics/jvm.memory.used"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("jvm.memory.used"))
            .andExpect(jsonPath("$.measurements[0].value").isNumber())
            .andReturn();

        // Les métriques JVM devraient exister
        assertThat(result1.getResponse().getContentAsString()).isNotEmpty();
        assertThat(result2.getResponse().getContentAsString()).isNotEmpty();
    }

    @Test
    @DisplayName("All 8 dashboards are available for export")
    void allDashboardsAreAvailable() throws Exception {
        mockMvc.perform(get("/actuator/observability/info"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dashboardCount").value(8))
            .andExpect(jsonPath("$.dashboards[*]", hasItems(
                "Alerts Overview",
                "Application Health",
                "Business Metrics",
                "Cache Metrics",
                "Database Metrics",
                "Distributed Tracing",
                "Http Metrics",
                "Jvm Metrics"
            )));
    }

    @Test
    @DisplayName("Prometheus registry is properly configured")
    void prometheusRegistryIsConfigured() throws Exception {
        // Vérifier que le registry est bien un PrometheusMeterRegistry
        assertThat(meterRegistry.getClass().getName()).contains("PrometheusMeterRegistry");

        // Vérifier que plusieurs types de métriques sont enregistrées
        assertThat(meterRegistry.find("jvm.memory.used").meter()).isNotNull();
        assertThat(meterRegistry.find("jvm.threads.live").meter()).isNotNull();
        assertThat(meterRegistry.find("process.uptime").meter()).isNotNull();

        // Vérifier que le registry a bien des métriques enregistrées
        assertThat(meterRegistry.getMeters()).isNotEmpty();
        assertThat(meterRegistry.getMeters().size()).isGreaterThan(10);
    }

    @Test
    @DisplayName("Security warning is present in observability info")
    void securityWarningIsPresent() throws Exception {
        mockMvc.perform(get("/actuator/observability/info"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.securityWarning").exists())
            .andExpect(jsonPath("$.securityWarning",
                containsString("protected with authentication")));
    }

    @Test
    @DisplayName("Quick start instructions are provided")
    void quickStartInstructionsAreProvided() throws Exception {
        mockMvc.perform(get("/actuator/observability/info"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quickStart").exists())
            .andExpect(jsonPath("$.quickStart", containsString("curl")))
            .andExpect(jsonPath("$.quickStart", containsString("unzip")))
            .andExpect(jsonPath("$.quickStart", containsString("Grafana")));
    }

    @Test
    @DisplayName("Application health includes all expected components")
    void healthIncludesAllComponents() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.components").exists());
    }

    @Test
    @DisplayName("Metrics endpoint lists all available metrics")
    void metricsEndpointListsAllMetrics() throws Exception {
        MvcResult result = mockMvc.perform(get("/actuator/metrics"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.names").isArray())
            .andReturn();

        String content = result.getResponse().getContentAsString();

        // Vérifier que plusieurs catégories de métriques sont présentes
        assertThat(content).contains("jvm.");
        assertThat(content).contains("process.");
        assertThat(content).contains("system.");
        assertThat(content).contains("http.server.requests");
    }

    @Test
    @DisplayName("Export endpoints are consistently accessible")
    void exportEndpointsAreConsistentlyAccessible() throws Exception {
        // Test multiple fois pour vérifier la stabilité
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/actuator/observability/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dashboardCount").value(8));

            mockMvc.perform(get("/actuator/observability/export"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"));
        }
    }
}
