# Contributing

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker (for running the monitoring stack)

## Development Setup

```bash
git clone https://github.com/imadAttar/spring-boot-unified-observability-starter.git
cd spring-boot-unified-observability-starter
mvn clean verify
```

## Running Tests

```bash
# All tests
mvn test

# With coverage report
mvn clean test jacoco:report
# Report at: target/site/jacoco/index.html

# Specific test class
mvn test -Dtest=ObservabilityAutoConfigurationTest
```

## Code Standards

- Follow existing code patterns and naming conventions
- All new classes must have corresponding test classes
- JaCoCo coverage must stay above 70%
- Use `@ConditionalOnProperty` and `@ConditionalOnClass` for optional features
- No breaking changes to the public API without a major version bump

## Commit Messages

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: Add new feature
fix: Fix a bug
refactor: Code change that neither fixes a bug nor adds a feature
docs: Documentation only
test: Adding or updating tests
chore: Build process, dependencies, CI
```

## Pull Request Process

1. Fork the repository
2. Create a feature branch from `dev`
3. Make your changes with tests
4. Run `mvn clean verify` to ensure everything passes
5. Submit a PR to `dev` branch
6. Fill in the PR template

## Adding New Features

### New metrics source
1. Create a `@Configuration` class in `metrics/`
2. Guard with `@ConditionalOnClass` for the relevant library
3. Add `@ConditionalOnProperty` for user control
4. Import in `MetricsConfiguration`
5. Add property in `ObservabilityProperties.Metrics`
6. Write tests

### New Grafana dashboard
1. Add JSON file to `src/main/resources/observability-stack/grafana-dashboards/`
2. Use placeholders: service names will be auto-substituted on export
3. Update test assertions for dashboard count
