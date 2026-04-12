# Configuration Reference

All properties are under the `observability` prefix and support IDE autocompletion.

## Global

| Property | Type | Default | Description |
|---|---|---|---|
| `observability.enabled` | boolean | `true` | Enable/disable all observability features |

## Metrics

| Property | Type | Default | Description |
|---|---|---|---|
| `observability.metrics.enabled` | boolean | `true` | Enable Prometheus metrics |
| `observability.metrics.jvm-enabled` | boolean | `true` | JVM metrics (memory, GC, threads) |
| `observability.metrics.http-enabled` | boolean | `true` | HTTP request metrics with percentiles |
| `observability.metrics.database-enabled` | boolean | `true` | HikariCP connection pool metrics |
| `observability.metrics.http-slo-millis` | double[] | `[10,50,100,200,500,1000,2000,5000]` | SLO bucket boundaries in ms |

### HTTP Metrics Detail

When `http-enabled=true`, the starter automatically configures:
- **Percentiles**: p50, p95, p99 for latency analysis
- **SLO buckets**: Configurable histogram boundaries for SLO compliance
- **Servlet + WebFlux**: Both servlet and reactive stacks are supported

## Tracing

| Property | Type | Default | Description |
|---|---|---|---|
| `observability.tracing.enabled` | boolean | `true` | Enable OpenTelemetry tracing |
| `observability.tracing.otlp-endpoint` | string | `http://localhost:4318/v1/traces` | OTLP gRPC exporter endpoint |
| `observability.tracing.sampling-probability` | double | `1.0` | Sampling rate (0.0-1.0) |
| `observability.tracing.service-name` | string | `spring-boot-app` | Service name in traces |
| `observability.tracing.propagation-enabled` | boolean | `true` | W3C + B3 header propagation |
| `observability.tracing.timeout-seconds` | long | `10` | OTLP request timeout |
| `observability.tracing.connect-timeout-seconds` | long | `10` | OTLP connection timeout |

### Sampling Recommendations

| Environment | Probability | Rationale |
|---|---|---|
| Development | `1.0` | Trace everything for debugging |
| Staging | `0.5` | Balance visibility and overhead |
| Production | `0.1` | 10% sampling for most services |
| High-traffic | `0.01` | 1% to limit storage costs |

### TracingHelper Usage

For manual span creation:

```java
@Autowired
private TracingHelper tracingHelper;

public Order processOrder(OrderRequest request) {
    return tracingHelper.trace("order.process", () -> {
        tracingHelper.addAttribute("order.id", request.getId());
        // business logic
        return orderRepository.save(order);
    });
}
```

## Logging

| Property | Type | Default | Description |
|---|---|---|---|
| `observability.logging.json-enabled` | boolean | `true` | Enable structured logging |
| `observability.logging.format` | string | `json` | Format: `json` or `ecs` |
| `observability.logging.include-trace-id` | boolean | `true` | Add trace ID to logs |
| `observability.logging.include-span-id` | boolean | `true` | Add span ID to logs |
| `observability.logging.include-mdc` | boolean | `true` | Include MDC entries |
| `observability.logging.level` | string | `INFO` | Default log level |

### Format Comparison

| Feature | `json` (Logstash) | `ecs` (Elastic) |
|---|---|---|
| Dependency | `logstash-logback-encoder` | None (Spring Boot 3.4+) |
| Best for | ELK stack, general use | Elastic Stack |
| Spring Boot | 3.x+ | 3.4+ only |
| MDC support | Full | Standard |

## Stack Export

| Property | Type | Default | Description |
|---|---|---|---|
| `observability.stack-export.enabled` | boolean | `false` | Enable export feature |
| `observability.stack-export.export-path` | string | `./monitoring` | Export destination path |
| `observability.stack-export.personalize-config` | boolean | `true` | Customize with service name |
| `observability.stack-export.export-on-startup` | boolean | `false` | Auto-export on app start |

### Path Formats

```yaml
# Relative to working directory
export-path: ./monitoring

# Parent directory
export-path: ../monitoring

# Absolute path
export-path: /opt/monitoring

# Home directory
export-path: ~/monitoring

# Environment variable with fallback
export-path: ${MONITORING_PATH:./monitoring}
```

### REST API Endpoints

When `stack-export.enabled=true` and `spring-boot-starter-web` is on the classpath:

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/actuator/observability/export` | Download monitoring stack as ZIP |
| `POST` | `/actuator/observability/export/directory?path=...` | Export to filesystem |
| `GET` | `/actuator/observability/info` | Dashboard and configuration info |

## Profile Examples

### Development

```yaml
# application-dev.yml
observability:
  tracing:
    sampling-probability: 1.0
  logging:
    format: json
  stack-export:
    enabled: true
    export-on-startup: true
```

### Production

```yaml
# application-prod.yml
observability:
  tracing:
    sampling-probability: 0.1
    otlp-endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://otel-collector:4318/v1/traces}
  logging:
    format: ecs
  stack-export:
    enabled: false
```
