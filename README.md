# 📊 Spring Boot Unified Observability Starter

[![CI](https://github.com/imadAttar/spring-boot-unified-observability-starter/actions/workflows/ci.yml/badge.svg)](https://github.com/imadAttar/spring-boot-unified-observability-starter/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-21+-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4+-brightgreen)](https://spring.io/projects/spring-boot)
[![OpenTelemetry](https://img.shields.io/badge/OpenTelemetry-enabled-blue)](https://opentelemetry.io/)
[![Prometheus](https://img.shields.io/badge/Prometheus-ready-red)](https://prometheus.io/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![JitPack](https://jitpack.io/v/imadAttar/spring-boot-unified-observability-starter.svg)](https://jitpack.io/#imadAttar/spring-boot-unified-observability-starter)

**Zero-configuration observability for Spring Boot 3.x**: Production-ready metrics, traces, and logs in a single dependency.

## 🎯 Why This Starter?

Built and battle-tested across multiple production projects to solve a recurring problem: **setting up observability takes too much time**.

Instead of spending hours configuring Prometheus, Grafana, OpenTelemetry, and creating dashboards for every new project, this starter provides everything pre-configured and ready to use.

**One dependency → Full observability stack**

## 🚨 Problem → Solution

### Before (Complex Setup)

```xml
<!-- 6+ dependencies + hours of configuration -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
<!-- + manual Grafana dashboards, Prometheus config, alerts... -->
```

**Result**: Hours of setup, version conflicts, manual dashboard creation

### After (Zero Config)

```xml
<dependency>
    <groupId>com.github.imadAttar</groupId>
    <artifactId>spring-boot-unified-observability-starter</artifactId>
    <version>1.3.0</version>
</dependency>
```

**Result**: Everything configured automatically in minutes

## ✨ What You Get

- 📈 **Auto-configured Metrics**: JVM, HTTP, Database, Custom
- 🔍 **Distributed Tracing**: OpenTelemetry with automatic correlation
- 📝 **Structured Logging**: JSON logs with trace IDs (Logstash or ECS format)
- 📊 **8 Grafana Dashboards**: Auto-exported and ready to import
- 🚨 **20 Prometheus Alerts**: Critical production alerts included
- 🐳 **Docker Compose Stack**: Complete monitoring setup
- ⚙️ **Flexible Export**: Monitoring stack exported automatically

## 🎯 Perfect For

This starter is ideal if you're looking for:
- **Spring Boot monitoring** without complex setup
- **Prometheus metrics** auto-configured for Spring Boot applications
- **Grafana dashboards** ready-to-use for Spring Boot microservices
- **OpenTelemetry tracing** integrated with Spring Boot 3.x
- **Production-ready observability** for Spring Boot in Kubernetes
- **Distributed tracing** with automatic trace-log correlation
- **Spring Boot application performance monitoring** (APM)
- **Zero-config observability** for rapid development

## Compatibility

| Component | Required | Notes |
|---|---|---|
| **Java** | 21+ | Compiled with `--release 21` |
| **Spring Boot** | 3.4+ | Structured logging requires 3.4+. Metrics/tracing may work on 3.2-3.3 but are untested. |
| **Servlet** | Supported | HTTP metrics via `DispatcherServlet` |
| **WebFlux** | Supported | HTTP metrics via `WebFluxConfigurer` |

> **Documentation**: [Getting Started](docs/getting-started.md) | [Configuration Reference](docs/configuration.md) | [Changelog](CHANGELOG.md)

## 🚀 Quick Start

### 1. Add Dependency

#### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.imadAttar</groupId>
    <artifactId>spring-boot-unified-observability-starter</artifactId>
    <version>1.3.0</version>
</dependency>
```

#### Gradle

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.imadAttar:spring-boot-unified-observability-starter:1.3.0'
}
```

### 2. Configure (Optional)

```yaml
# application.yml
observability:
  enabled: true
  service:
    name: ${spring.application.name}
    environment: production
  stack-export:
    enabled: true
    export-path: ./monitoring  # Relative, absolute, or ~/path
```

### 3. Run Your App

```bash
mvn spring-boot:run
```

**That's it!** Your monitoring stack is automatically exported to `./monitoring`

### 4. Start Monitoring

```bash
cd monitoring
docker-compose up -d

# Access services:
# Grafana:    http://localhost:3000 (admin/admin)
# Prometheus: http://localhost:9090
```

## 📦 Exported Monitoring Stack

```
monitoring/
├── docker-compose.yml                 # Complete stack (Grafana, Prometheus, Jaeger)
├── prometheus.yml                     # Prometheus configuration
├── start-monitoring.sh                # Quick start script
├── prometheus-alerts/
│   └── spring-boot-alerts.yml        # 20 production alerts
└── grafana-provisioning/
    └── dashboards/json/
        ├── jvm-metrics.json          # JVM monitoring
        ├── http-metrics.json         # HTTP performance
        ├── database-metrics.json     # Database & connection pool
        ├── cache-metrics.json        # Cache performance
        ├── business-metrics.json     # Custom business metrics
        ├── application-health.json   # Health & availability
        ├── distributed-tracing.json  # Trace analysis
        └── alerts-overview.json      # Active alerts dashboard
```

## 📊 Dashboards Included

| Dashboard | Key Metrics |
|-----------|-------------|
| **JVM** | Heap memory, GC, threads, CPU |
| **HTTP** | Latency (P50/P95/P99), error rate, throughput |
| **Database** | Query time, connection pool, slow queries |
| **Cache** | Hit rate, evictions (Redis, Caffeine) |
| **Business** | Custom business metrics |
| **Health** | Availability, error tracking |
| **Tracing** | Distributed traces, service latency |
| **Alerts** | Active alerts, history |

## 💻 Code Example

```java
@Service
@Slf4j
public class UserService {

    @Timed(value = "user.creation.time")
    @Counted(value = "user.creation.count")
    public User createUser(CreateUserRequest request) {
        log.info("Creating user", kv("email", request.getEmail()));

        User user = userRepository.save(new User(request));

        log.info("User created", kv("userId", user.getId()));
        return user;
    }
}
```

**Automatic Output**:
- ✅ Prometheus metrics: `user_creation_count`, `user_creation_time_seconds`
- ✅ OpenTelemetry traces with correlation
- ✅ JSON logs with automatic `trace_id` and `span_id`
- ✅ Real-time Grafana dashboards updated

## 🔧 Configuration Options

### Export Path Formats

```yaml
# Relative paths
export-path: ./monitoring
export-path: ../monitoring

# Absolute paths
export-path: /opt/monitoring           # Linux/Mac
export-path: C:\monitoring             # Windows

# Home directory
export-path: ~/monitoring

# Environment variables
export-path: ${MONITORING_PATH:./monitoring}
```

### Service Configuration

```yaml
observability:
  service:
    name: my-service
    version: 1.0.0
    environment: production
    team: platform-team

  metrics:
    enabled: true
    jvm-enabled: true
    http-enabled: true
    database-enabled: true

  tracing:
    enabled: true
    sampling-probability: 0.1  # 10% in production

  logging:
    json-enabled: true
    format: json              # Options: json (Logstash), ecs (Spring Boot 3.4+ native)
    include-trace-id: true
```

## 🔐 Security & Production Deployment

### ⚠️ Important: Endpoint Security

This starter follows **the same philosophy as all Spring Boot starters**:
- ❌ **No security enforced by default**
- ✅ **YOU are responsible for securing according to your needs**

**Just like**:
- `spring-boot-starter-actuator` exposes `/actuator` without security
- `spring-boot-starter-web` has no authentication by default
- `spring-boot-starter-data-rest` exposes APIs without protection

### 🔒 Securing Export Endpoints (Production)

Export endpoints `/actuator/observability/export` are **public by default** for development convenience.

**Option 1: Disable in production**
```yaml
observability:
  stack-export:
    enabled: false  # Disable export endpoints
```

**Option 2: Add Spring Security**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

```java
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/observability/**").hasRole("ADMIN")
            .anyRequest().permitAll()
        );
        return http.build();
    }
}
```

**Option 3: Spring Boot Actuator Security**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,prometheus  # Exclude observability
```

### 📋 Production Checklist

- [ ] Disable `stack-export.enabled` in production OR add authentication
- [ ] Change Grafana default credentials (admin/admin)
- [ ] Configure `tracing.sampling-probability` to 0.1 (10%) in production
- [ ] Secure `/actuator/prometheus` with Spring Security if needed
- [ ] Validate logs don't contain sensitive data

### 💡 Why No Security by Default?

1. **Flexibility**: Every organization has different requirements (OAuth2, LDAP, JWT, etc.)
2. **Compatibility**: Don't force unnecessary security dependencies
3. **Spring Boot Standard**: All official starters work this way
4. **Fast Development**: Zero friction in dev, security in production

> 🎯 **This starter is a tool, not a complete application**. Security is YOUR responsibility, as with any Spring Boot starter.

## ❓ FAQ

### How do I monitor a Spring Boot application?
Add this starter as a dependency. Metrics, traces, and logs are auto-configured. No manual Prometheus or Grafana setup needed.

### How to set up Prometheus with Spring Boot?
This starter includes Micrometer with Prometheus registry pre-configured. Metrics are exposed at `/actuator/prometheus` automatically.

### How to add distributed tracing to Spring Boot?
OpenTelemetry is auto-configured. Every HTTP request, database query, and log entry is automatically traced and correlated.

### How to monitor Spring Boot microservices in production?
This starter provides production-ready monitoring: 8 Grafana dashboards, 20 Prometheus alerts, distributed tracing, and structured logging.

### How to monitor database connection pools in Spring Boot?
HikariCP metrics are auto-configured: active connections, idle connections, pending threads, and connection wait time.

### How to export Grafana dashboards for Spring Boot?
Dashboards are auto-exported to `./monitoring` directory on startup. Import them into Grafana with one click.

### Does this work with Spring Boot 3.x?
Yes, specifically designed for Spring Boot 3.x with native OpenTelemetry support.

## ✅ Quality & Testing

### Production-Ready

This starter is thoroughly tested and validated:

- **124 tests** with 100% pass rate (Unit, Integration, Security, Performance, E2E)
- **Zero** critical security vulnerabilities
- **Comprehensive** path traversal protection (31 security tests)
- **Validated** configuration with JSR-303 annotations
- **No resource leaks** - All streams properly managed

### Test Coverage

```bash
mvn test  # Run all 124 tests
```

**Test Categories:**
- 🧪 Unit Tests (8) - Configuration validation
- 🔗 Integration Tests (10) - Export functionality
- 🔐 Security Tests (31) - Path traversal protection
- ⚡ Performance Tests (8) - Metrics performance
- 🎯 E2E Tests (10) - Complete workflows

### Code Quality

- ✅ JSR-303 validation on all configuration properties
- ✅ Null-safe operations with defensive checks
- ✅ Resource management with try-with-resources
- ✅ Specific exception handling (no generic catches)
- ✅ Clean, maintainable code following Spring Boot best practices

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👤 Author

**Imad ATTAR**

- 💼 [LinkedIn](https://linkedin.com/in/imad-attar-ba130389)
- 🐙 [GitHub](https://github.com/imadAttar)

## 🙏 Acknowledgments

- Spring Boot Team for the amazing framework
- OpenTelemetry Community for standardized observability
- Prometheus & Grafana Teams for monitoring excellence

---

⭐ **Star this project** if it helps you achieve better observability!

💬 **Questions?** Open an [issue](https://github.com/imadAttar/spring-boot-unified-observability-starter/issues)

🚀 **JitPack**: [https://jitpack.io/#imadAttar/spring-boot-unified-observability-starter](https://jitpack.io/#imadAttar/spring-boot-unified-observability-starter)
