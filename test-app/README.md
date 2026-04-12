# Demo Application

Minimal Spring Boot app demonstrating the Unified Observability Starter.

## Setup

```bash
# 1. Install the starter to local Maven repo (from project root)
cd ..
mvn clean install -DskipTests

# 2. Run the demo app
cd test-app
mvn spring-boot:run
```

## Try It

```bash
# Generate some traffic
curl http://localhost:8080/orders
curl http://localhost:8080/orders/123
curl http://localhost:8080/orders/slow

# View Prometheus metrics
curl http://localhost:8080/actuator/prometheus | grep orders

# View health with observability details
curl http://localhost:8080/actuator/health | jq

# Download monitoring stack
curl http://localhost:8080/actuator/observability/export -o monitoring-stack.zip
```

## Start Monitoring Stack

```bash
cd monitoring
docker compose up -d
```

- Grafana: http://localhost:3000 (admin/admin)
- Prometheus: http://localhost:9090
- Jaeger: http://localhost:16686

## What to Look For

1. **Grafana** - Open the "HTTP Metrics" dashboard to see request rates and latencies
2. **Prometheus** - Query `orders_list_seconds_count` to see @Observed metrics
3. **Jaeger** - Search for `demo-observability-app` traces
4. **Health** - `/actuator/health` shows observability component status
