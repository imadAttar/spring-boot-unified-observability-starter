# Prometheus Alert Rules for Spring Boot Unified Observability

This directory contains production-ready Prometheus alert rules for comprehensive monitoring of Spring Boot applications.

## Overview

**File**: `spring-boot-alerts.yml`
**Total Rules**: 20 alert rules
**Categories**: 7 categories covering all critical aspects of application health

## Alert Categories

### 1. Latency (5 rules)
- **HighP95Latency**: P95 latency > 1s for 5m (WARNING)
- **CriticalP95Latency**: P95 latency > 2s for 3m (CRITICAL)
- **HighP99Latency**: P99 latency > 3s for 5m (WARNING)
- **SlowDatabaseQueries**: DB connection acquisition > 0.5s for 5m (WARNING)
- **HighAverageResponseTime**: Average response time > 0.5s for 10m (WARNING)

### 2. Error Rates (4 rules)
- **ElevatedErrorRate**: 5xx errors > 1% for 5m (WARNING)
- **HighErrorRate**: 5xx errors > 5% for 2m (CRITICAL)
- **Increased4xxErrors**: 4xx errors > 10% for 10m (INFO)
- **HighExceptionRate**: Exceptions > 1/s for 5m (WARNING)

### 3. Memory Pressure (3 rules)
- **HighHeapMemoryUsage**: Heap > 80% for 5m (WARNING)
- **CriticalHeapMemoryUsage**: Heap > 90% for 2m (CRITICAL)
- **PossibleMemoryLeak**: Heap growth > 10% per hour for 30m (WARNING)

### 4. GC Pressure (2 rules)
- **HighGCActivity**: GC > 5 times/s for 5m (WARNING)
- **LongGCPauses**: Average GC pause > 0.1s for 5m (WARNING)

### 5. Thread Pool (2 rules)
- **HighThreadCount**: Threads > 500 for 10m (WARNING)
- **ThreadPoolExhaustion**: Thread usage > 90% of peak for 5m (CRITICAL)

### 6. Database Pool (2 rules)
- **ConnectionPoolSaturation**: Pool usage > 80% for 5m (WARNING)
- **PendingDatabaseConnections**: Pending connections > 0 for 5m (WARNING)

### 7. Availability (2 rules)
- **ServiceDown**: Service up == 0 for 1m (CRITICAL)
- **LowRequestRate**: Requests < 20% of daily average for 10m (WARNING)

## Installation

### Step 1: Copy Alert Rules to Prometheus

```bash
# Copy the alert rules file to your Prometheus rules directory
sudo cp spring-boot-alerts.yml /etc/prometheus/rules/

# Verify file permissions
sudo chmod 644 /etc/prometheus/rules/spring-boot-alerts.yml
```

### Step 2: Update Prometheus Configuration

Edit `/etc/prometheus/prometheus.yml` and add the rule file:

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - /etc/prometheus/rules/spring-boot-alerts.yml

scrape_configs:
  - job_name: 'spring-boot'
    static_configs:
      - targets: ['localhost:8080']
        labels:
          service: 'my-service'
          environment: 'production'
```

### Step 3: Validate Alert Rules

```bash
# Validate syntax
promtool check rules /etc/prometheus/rules/spring-boot-alerts.yml

# Expected output: SUCCESS: X rules found
```

### Step 4: Reload Prometheus

```bash
# Option 1: Use reload endpoint (if --web.enable-lifecycle is enabled)
curl -X POST http://localhost:9090/-/reload

# Option 2: Send HUP signal
kill -HUP $(pidof prometheus)

# Option 3: Restart Prometheus
sudo systemctl restart prometheus
```

### Step 5: Verify Rules are Loaded

Open Prometheus UI and check:

```bash
# Open in browser
open http://localhost:9090/rules

# Or check via API
curl http://localhost:9090/api/v1/rules | jq '.data.groups[].rules[].name'
```

## Alertmanager Integration

### Configure Prometheus to Send Alerts

In `prometheus.yml`:

```yaml
alerting:
  alertmanagers:
    - static_configs:
        - targets: ['localhost:9093']
```

### Example Alertmanager Configuration

Create `/etc/alertmanager/alertmanager.yml`:

```yaml
global:
  resolve_timeout: 5m

route:
  group_by: ['alertname', 'service', 'environment']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 12h
  receiver: 'default'
  routes:
    - match:
        severity: critical
      receiver: 'pagerduty'
      continue: true
    - match:
        severity: warning
      receiver: 'slack'
    - match:
        severity: info
      receiver: 'email'

receivers:
  - name: 'default'
    slack_configs:
      - api_url: 'https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK'
        channel: '#alerts'
        title: '{{ template "slack.default.title" . }}'
        text: '{{ template "slack.default.text" . }}'

  - name: 'pagerduty'
    pagerduty_configs:
      - service_key: 'YOUR_PAGERDUTY_SERVICE_KEY'
        description: '{{ template "pagerduty.default.description" . }}'

  - name: 'slack'
    slack_configs:
      - api_url: 'https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK'
        channel: '#monitoring'

  - name: 'email'
    email_configs:
      - to: 'team@example.com'
        from: 'alerts@example.com'
        smarthost: 'smtp.example.com:587'
```

## Testing Alerts

### Method 1: Generate Load

```bash
# High latency (add Thread.sleep to your code)
# Or use external load testing tool

# High error rate
for i in {1..1000}; do curl http://localhost:8080/error; done

# High memory usage
curl -X POST http://localhost:8080/actuator/heapdump
```

### Method 2: Temporarily Lower Thresholds

Edit alert rules temporarily:

```yaml
# Change from
expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 1

# To (easier to trigger)
expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 0.001
```

Then reload Prometheus and check alerts:

```bash
curl -X POST http://localhost:9090/-/reload
open http://localhost:9090/alerts
```

### Method 3: Use amtool (Alertmanager CLI)

```bash
# Check alert status
amtool alert

# Silence an alert
amtool silence add alertname="HighP95Latency" --duration=1h --comment="Planned maintenance"

# List silences
amtool silence query
```

## Customization

### Adjusting Thresholds

Edit thresholds in `spring-boot-alerts.yml`:

```yaml
# Example: Make error rate alert less sensitive
- alert: ElevatedErrorRate
  expr: |
    (
      sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (service, environment)
      /
      sum(rate(http_server_requests_seconds_count[5m])) by (service, environment)
    ) * 100 > 5  # Changed from 1 to 5
  for: 10m  # Changed from 5m to 10m
```

### Adding Custom Labels

Add labels for routing or grouping:

```yaml
- alert: HighP95Latency
  labels:
    severity: warning
    component: http
    team: backend  # Add custom label
    slack_channel: backend-alerts  # For Alertmanager routing
```

### Modifying Annotations

Customize alert messages:

```yaml
annotations:
  summary: "[{{ $labels.environment }}] High latency on {{ $labels.service }}"
  description: "P95 is {{ $value | humanizeDuration }}. Check logs: https://logs.example.com?service={{ $labels.service }}"
  dashboard: "https://grafana.example.com/d/http-dashboard?var-service={{ $labels.service }}"
  runbook_url: "https://wiki.example.com/runbooks/HighP95Latency"
```

## Alert Response Runbooks

### HighP95Latency / CriticalP95Latency

1. **Check recent deployments**: Recent code changes?
2. **Check database**: Slow queries, connection pool saturation?
3. **Check external services**: Downstream API latency?
4. **Check logs**: Error patterns, stack traces?
5. **Scale if needed**: Add more instances
6. **Optimize hot paths**: Profile and optimize slow code

### HighErrorRate / ElevatedErrorRate

1. **Check error logs**: What exceptions are being thrown?
2. **Check recent deployments**: Rollback if needed
3. **Check dependencies**: Database, cache, external APIs up?
4. **Check load**: Under unusual traffic spike?
5. **Check configuration**: Recent config changes?

### CriticalHeapMemoryUsage

1. **Immediate**: Restart service if critical
2. **Heap dump**: `jmap -dump:live,format=b,file=heap.bin <pid>`
3. **Analyze**: Use Eclipse MAT or VisualVM
4. **Check for leaks**: Growing collections, static references
5. **Tune JVM**: Increase heap size if needed: `-Xmx4g`
6. **Fix code**: Remove memory leaks

### ServiceDown

1. **Verify**: Is service actually down or monitoring issue?
2. **Check logs**: `journalctl -u myservice -n 100`
3. **Check health endpoint**: `curl http://localhost:8080/actuator/health`
4. **Check process**: `ps aux | grep java`
5. **Check resources**: CPU, memory, disk full?
6. **Restart if needed**: `systemctl restart myservice`
7. **Escalate**: If can't resolve quickly

### ConnectionPoolSaturation

1. **Check active queries**: Long-running queries?
2. **Check pool size**: Increase if needed (HikariCP maxPoolSize)
3. **Check for leaks**: Connections not being closed?
4. **Optimize queries**: Add indexes, optimize N+1 queries
5. **Connection timeout**: Reduce acquisition timeout if too high

## Monitoring Best Practices

1. **Start conservative**: Begin with higher thresholds, lower as needed
2. **Tune for your workload**: Different apps have different normal ranges
3. **Avoid alert fatigue**: Too many alerts = ignored alerts
4. **Group related alerts**: Use Alertmanager routing
5. **Document runbooks**: Every alert should have a runbook
6. **Test regularly**: Verify alerts fire as expected
7. **Review periodically**: Remove noisy alerts, add missing ones

## Severity Guidelines

- **CRITICAL**: Wakes someone up, immediate action required
  - Service down
  - Data loss risk
  - User-facing errors >5%
  - System near failure

- **WARNING**: Requires attention during business hours
  - Degraded performance
  - Resource pressure (80% threshold)
  - Elevated error rates (1-5%)
  - Potential issues forming

- **INFO**: FYI, investigate when convenient
  - Trends worth noting
  - Client errors (4xx)
  - Non-urgent anomalies

## Required Metrics

These alert rules require the following metrics to be exposed:

✅ **HTTP Metrics** (from Spring Boot Actuator):
- `http_server_requests_seconds_count`
- `http_server_requests_seconds_sum`
- `http_server_requests_seconds_bucket`

✅ **JVM Metrics** (from Micrometer JVM binders):
- `jvm_memory_used_bytes`
- `jvm_memory_max_bytes`
- `jvm_gc_pause_seconds_count`
- `jvm_gc_pause_seconds_sum`
- `jvm_threads_live_threads`
- `jvm_threads_peak_threads`

✅ **Database Metrics** (from HikariCP):
- `hikaricp_connections_active`
- `hikaricp_connections_max`
- `hikaricp_connections_pending`
- `hikaricp_connections_acquire_seconds_bucket`

✅ **Availability Metrics** (from Prometheus):
- `up`

✅ **Application Metrics** (optional, for exception tracking):
- `application_exceptions_total`

All required metrics are automatically exposed by the Spring Boot Unified Observability Starter when properly configured.

## Troubleshooting

### Alerts not appearing in Prometheus UI

```bash
# Check Prometheus logs
journalctl -u prometheus -f

# Verify rules file syntax
promtool check rules spring-boot-alerts.yml

# Check file permissions
ls -la /etc/prometheus/rules/spring-boot-alerts.yml
```

### Alerts not firing when they should

```bash
# Check if metrics exist
curl 'http://localhost:9090/api/v1/query?query=up'

# Check alert expression manually
curl 'http://localhost:9090/api/v1/query?query=histogram_quantile(0.95,rate(http_server_requests_seconds_bucket[5m]))'

# Verify labels match
curl 'http://localhost:9090/api/v1/series?match[]=http_server_requests_seconds_count'
```

### Alerts firing too frequently

1. Increase `for` duration
2. Raise thresholds
3. Add label matchers to be more specific
4. Use Alertmanager grouping/throttling

## Support

For questions or issues:
- Check Prometheus documentation: https://prometheus.io/docs/
- Check Alertmanager documentation: https://prometheus.io/docs/alerting/alertmanager/
- Review Spring Boot Actuator metrics: https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html

---

**Version**: 1.0.0
**Last Updated**: 2026-01-28
**Maintainer**: Spring Boot Unified Observability Team
