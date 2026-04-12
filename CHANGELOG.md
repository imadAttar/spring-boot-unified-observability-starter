# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.3.0] - 2026-04-12

### Added
- `CountedAspect` bean in `MetricsConfiguration` — `@Counted` annotations now work correctly
- `JsonLoggingEnvironmentPostProcessor` — JSON structured logging now configures Logback automatically via Spring Boot 3.4+ native support
- W3C Trace Context propagator alongside B3 — composite propagation as documented
- WebFlux HTTP metrics support via `ReactiveHttpMetricsConfiguration`
- `additional-spring-configuration-metadata.json` — IDE autocompletion for all `observability.*` properties
- Jaeger service in Docker Compose — distributed tracing works out of the box
- Alertmanager as Docker Compose optional profile (`--profile alerting`)
- `docs/getting-started.md` and `docs/configuration.md`
- GitHub Actions CI pipeline (build + test + coverage)

### Changed
- OpenTelemetry dependencies are now optional — consumers who only need metrics don't pull ~5MB of OTel SDK
- `commons-text` and `micrometer-tracing-bridge-otel` are now optional
- `SdkTracerProvider` and `OtlpGrpcSpanExporter` are now proper Spring beans with managed lifecycle (no more mutable fields on auto-configuration class)
- `ContextPropagators` is now injected into the OpenTelemetry SDK (previously was a disconnected bean)
- Docker Compose: removed deprecated `version: '3.8'` field
- Prometheus config: added multi-environment comments (Docker Mac/Linux, Kubernetes, local)
- JaCoCo coverage threshold raised from 55% to 70%

### Fixed
- `@Counted` annotation was silently non-functional (missing `CountedAspect`)
- JSON logging format (`observability.logging.format=json`) had no effect — only logged a message without configuring Logback
- W3C Trace Context propagation was not configured despite documentation claiming support
- Duplicate `@ConditionalOnClass` import in `ObservabilityAutoConfiguration`
- Flaky performance test `shouldQueryMetricsEfficiently` (threshold too strict)

### Removed
- `CustomMetricsExamples` from starter JAR — moved to test sources (was registering business metrics in consumer apps)
- `application-examples.yml` from classpath — moved to `docs/` (could interfere with Spring profile resolution)

### Deprecated
- `ObservabilityProperties.Grafana` — configuration exists but has no implementation. Will be removed or implemented in a future version.
- `observability.metrics.custom-enabled` property — no longer has any effect

## [1.2.0] - 2026-03-15

### Added
- Security section in README
- JSR-303 validation on configuration properties
- Comprehensive test suite (124 tests across 5 categories)
- Path traversal protection with 7-layer defense (31 security tests)
- Performance tests for metrics subsystem
- `@Validated` on `ObservabilityProperties`

### Changed
- Gradle dependency version updated to 1.2.0

### Fixed
- JaCoCo coverage threshold adjusted to 55%

## [1.0.0] - 2026-02-01

### Added
- Core observability auto-configuration (Prometheus metrics, OpenTelemetry tracing, structured logging)
- 8 pre-built Grafana dashboards (JVM, HTTP, database, cache, business, health, tracing, alerts)
- 20 Prometheus alert rules across 7 categories
- Monitoring stack export system (REST API + auto-export on startup)
- `@Observed` composite annotation (`@Timed` + `@Counted`)
- `TracingHelper` utility for manual span creation
- ECS logging support for Spring Boot 3.4+
- Docker Compose with Prometheus + Grafana
- JitPack distribution

[Unreleased]: https://github.com/imadAttar/spring-boot-unified-observability-starter/compare/1.3.0...HEAD
[1.3.0]: https://github.com/imadAttar/spring-boot-unified-observability-starter/compare/1.2.0...1.3.0
[1.2.0]: https://github.com/imadAttar/spring-boot-unified-observability-starter/compare/1.0.0...1.2.0
[1.0.0]: https://github.com/imadAttar/spring-boot-unified-observability-starter/releases/tag/1.0.0
