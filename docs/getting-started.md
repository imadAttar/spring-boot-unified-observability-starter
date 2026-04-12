# Getting Started

## Prerequisites

- Java 21+
- Spring Boot 3.4+
- Maven or Gradle

## Installation

### Maven (via JitPack)

Add the JitPack repository:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Add the dependency:

```xml
<dependency>
    <groupId>com.github.imadAttar</groupId>
    <artifactId>spring-boot-unified-observability-starter</artifactId>
    <version>1.3.0</version>
</dependency>
```

### Gradle (via JitPack)

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.imadAttar:spring-boot-unified-observability-starter:1.3.0'
}
```

## Quick Start

### 1. Add the dependency (above)

### 2. Configure your application

Add to your `application.yml`:

```yaml
spring:
  application:
    name: my-service

observability:
  enabled: true
  tracing:
    service-name: my-service
  stack-export:
    enabled: true
    export-on-startup: true
```

### 3. Start your application

```bash
mvn spring-boot:run
```

On startup you will see:
- Prometheus metrics at `/actuator/prometheus`
- Health check at `/actuator/health`
- Monitoring stack exported to `./monitoring/`

### 4. Start the monitoring stack

```bash
cd monitoring
docker compose up -d
```

Access:
- **Grafana**: http://localhost:3000 (admin/admin) - 8 pre-built dashboards
- **Prometheus**: http://localhost:9090 - metrics query
- **Jaeger**: http://localhost:16686 - distributed traces

## What You Get

### Metrics (automatic)
- JVM: memory, GC, threads, class loader, processor
- HTTP: request count, latency percentiles (p50, p95, p99), SLO buckets
- Database: HikariCP connection pool (if present)

### Tracing (automatic)
- OpenTelemetry distributed tracing
- W3C Trace Context + B3 propagation
- OTLP export to Jaeger/Tempo
- `TracingHelper` for manual span creation

### Logging (automatic)
- Structured JSON (Logstash format) or ECS (Elastic Common Schema)
- Trace/span ID correlation in logs
- MDC support

### Monitoring Stack (exported)
- 8 Grafana dashboards (JVM, HTTP, database, cache, business, health, tracing, alerts)
- 20 Prometheus alert rules
- Docker Compose with Prometheus + Grafana + Jaeger
- Ready-to-use configuration

## Optional Dependencies

The starter uses optional dependencies to keep your footprint minimal:

| Feature | Required Dependency |
|---|---|
| Metrics only | None (included via actuator) |
| Tracing | `io.opentelemetry:opentelemetry-sdk` |
| JSON logging | `net.logstash.logback:logstash-logback-encoder` |
| ECS logging | None (Spring Boot 3.4+ native) |
| Stack export | `org.apache.commons:commons-text` |
| Export REST API | `spring-boot-starter-web` |

If you only need metrics, you don't need to add any extra dependencies.

## Next Steps

- [Configuration Reference](configuration.md) - All available properties
- [Application Examples](application-examples.yml) - YAML configuration examples
