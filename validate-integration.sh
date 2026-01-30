#!/bin/bash

# Script de validation automatique des scénarios d'intégration critiques
# Usage: ./validate-integration.sh

set -e

echo "🧪 Validation d'Intégration - Spring Boot Unified Observability Starter"
echo "========================================================================"
echo ""

# Couleurs pour output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Compteurs
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Fonction de test
run_test() {
    local test_name=$1
    local test_command=$2

    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo -n "Testing: $test_name ... "

    if eval "$test_command" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ PASSED${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        return 0
    else
        echo -e "${RED}✗ FAILED${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        return 1
    fi
}

# Vérifier que l'application test existe
if [ ! -d "test-app" ]; then
    echo -e "${YELLOW}⚠️  test-app directory not found. Creating minimal test application...${NC}"
    mkdir -p test-app
fi

echo "📦 Étape 1: Build du Starter"
echo "----------------------------"
run_test "Maven Clean Install" "mvn clean install -DskipTests"
echo ""

echo "🧪 Étape 2: Tests Unitaires"
echo "----------------------------"
run_test "Unit Tests Pass" "mvn test -Dtest=*UnitTest"
run_test "Integration Tests Pass" "mvn test -Dtest=*IntegrationTest"
run_test "Security Tests Pass" "mvn test -Dtest=PathTraversalSecurityTest"
echo ""

echo "🔍 Étape 3: Validation Auto-Configuration"
echo "------------------------------------------"

# Test 1: Vérifier que META-INF/spring existe
run_test "Auto-configuration file exists" "[ -f target/classes/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports ]"

# Test 2: Vérifier le contenu
run_test "Auto-configuration contains ObservabilityAutoConfiguration" "grep -q 'ObservabilityAutoConfiguration' target/classes/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"

echo ""

echo "📋 Étape 4: Validation des Resources"
echo "-------------------------------------"

# Test 3: Vérifier que les dashboards sont présents
run_test "Grafana dashboards exist (8 files)" "[ \$(find target/classes/observability-stack/grafana-dashboards -name '*.json' 2>/dev/null | wc -l) -eq 8 ]"

# Test 4: Vérifier Prometheus config
run_test "Prometheus config exists" "[ -f target/classes/observability-stack/prometheus-config/prometheus.yml ]"

# Test 5: Vérifier Docker Compose
run_test "Docker Compose file exists" "[ -f target/classes/observability-stack/docker-compose/docker-compose.yml ]"

echo ""

echo "🔧 Étape 5: Validation des Dépendances"
echo "---------------------------------------"

# Test 6: Vérifier les dépendances critiques
run_test "Micrometer Core present" "mvn dependency:tree | grep -q 'micrometer-core'"
run_test "Micrometer Prometheus present" "mvn dependency:tree | grep -q 'micrometer-registry-prometheus'"
run_test "OpenTelemetry SDK present" "mvn dependency:tree | grep -q 'opentelemetry-sdk'"
run_test "Logback present" "mvn dependency:tree | grep -q 'logback'"

echo ""

echo "🎯 Étape 6: Test de Compilation"
echo "--------------------------------"

# Test 7: Vérifier qu'il n'y a pas d'erreurs de compilation
run_test "No compilation errors" "mvn compile -q"

echo ""

echo "🔐 Étape 7: Validation Sécurité"
echo "--------------------------------"

# Test 8: Vérifier que les tests de sécurité sont présents
run_test "Security tests exist" "[ -f src/test/java/com/imadattar/observability/security/PathTraversalSecurityTest.java ]"

# Test 9: Vérifier validation path dans le code
run_test "Path validation implemented" "grep -q 'validateExportPath' src/main/java/com/imadattar/observability/export/ObservabilityStackExportController.java"

echo ""

echo "📊 Étape 8: Code Quality Checks"
echo "--------------------------------"

# Test 10: Pas de System.out.println dans le code
if grep -r "System.out.println" src/main/java/ > /dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  WARNING: System.out.println found in main code${NC}"
else
    run_test "No System.out.println in main code" "true"
fi

# Test 11: @Slf4j présent
run_test "Lombok @Slf4j used" "grep -r '@Slf4j' src/main/java/ | wc -l | grep -qv '^0$'"

echo ""

echo "🚀 Étape 9: Artifact Validation"
echo "--------------------------------"

# Test 12: Vérifier que le JAR est créé
run_test "JAR artifact created" "[ -f target/spring-boot-unified-observability-starter-1.0.0.jar ]"

# Test 13: Vérifier la taille du JAR (doit être < 10MB)
if [ -f target/spring-boot-unified-observability-starter-1.0.0.jar ]; then
    JAR_SIZE=$(du -k target/spring-boot-unified-observability-starter-1.0.0.jar | cut -f1)
    if [ $JAR_SIZE -lt 10240 ]; then
        run_test "JAR size reasonable (<10MB)" "true"
    else
        echo -e "${YELLOW}⚠️  WARNING: JAR size is ${JAR_SIZE}KB (>10MB)${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
fi

echo ""

echo "📝 Étape 10: Documentation Validation"
echo "--------------------------------------"

# Test 14: README existe et n'est pas vide
run_test "README.md exists and not empty" "[ -s README.md ]"

# Test 15: Vérifier que le README mentionne Maven dependency
run_test "README contains Maven dependency" "grep -q '<dependency>' README.md"

echo ""

echo "========================================================================"
echo -e "📊 ${GREEN}RÉSULTATS DE VALIDATION${NC}"
echo "========================================================================"
echo ""
echo "Tests exécutés:  $TOTAL_TESTS"
echo -e "Tests réussis:   ${GREEN}$PASSED_TESTS${NC}"
echo -e "Tests échoués:   ${RED}$FAILED_TESTS${NC}"
echo ""

SUCCESS_RATE=$((PASSED_TESTS * 100 / TOTAL_TESTS))
echo "Taux de réussite: $SUCCESS_RATE%"
echo ""

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}✅ TOUS LES TESTS SONT PASSÉS!${NC}"
    echo ""
    echo "Le starter est prêt pour l'intégration!"
    echo ""
    echo "Prochaines étapes:"
    echo "  1. Tester avec une vraie application (voir test-app/)"
    echo "  2. Valider tous les scénarios du INTEGRATION_VALIDATION.md"
    echo "  3. Release sur Maven Central ou JitPack"
    exit 0
else
    echo -e "${RED}❌ CERTAINS TESTS ONT ÉCHOUÉ${NC}"
    echo ""
    echo "Veuillez corriger les problèmes avant d'intégrer."
    exit 1
fi
