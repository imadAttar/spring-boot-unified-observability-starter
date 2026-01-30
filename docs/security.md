# Security Best Practices

This guide covers security recommendations for deploying the Observability Starter in production environments.

## Overview

The Observability Starter exposes several actuator endpoints that should be properly secured in production:

- `/actuator/observability/export` - Download monitoring stack as ZIP
- `/actuator/observability/export/directory` - Export stack to filesystem
- `/actuator/observability/info` - Get dashboard information
- `/actuator/prometheus` - Prometheus metrics scraping endpoint

## Security Recommendations

### 1. Disable Export Endpoints in Production

The export endpoints are disabled by default for security. Only enable them when needed:

```yaml
observability:
  stack-export:
    enabled: false  # Default - keep disabled in production
```

### 2. Secure Actuator Endpoints with Spring Security

Add Spring Security to protect all actuator endpoints with authentication:

#### Maven Dependency

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

#### Security Configuration

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                // Public endpoints
                .requestMatchers("/actuator/health").permitAll()

                // Prometheus scraping - allow from monitoring network
                .requestMatchers("/actuator/prometheus").hasIpAddress("10.0.0.0/8")

                // Observability export - require authentication
                .requestMatchers("/actuator/observability/**").hasRole("ADMIN")

                // All other actuator endpoints require authentication
                .requestMatchers("/actuator/**").authenticated()

                .anyRequest().permitAll()
            )
            .httpBasic(withDefaults())
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/actuator/**")
            );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.builder()
            .username("admin")
            .password("{bcrypt}$2a$10$...")  // Use encrypted password
            .roles("ADMIN")
            .build();

        return new InMemoryUserDetailsManager(admin);
    }
}
```

### 3. Rate Limiting

Implement rate limiting to prevent abuse of export endpoints:

#### Using Bucket4j

```xml
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.7.0</version>
</dependency>
```

```java
@Component
public class RateLimitingFilter implements Filter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // Rate limit export endpoints
        if (httpRequest.getRequestURI().startsWith("/actuator/observability/export")) {
            String clientIp = httpRequest.getRemoteAddr();
            Bucket bucket = getBucket(clientIp);

            if (bucket.tryConsume(1)) {
                chain.doFilter(request, response);
            } else {
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.setStatus(429); // Too Many Requests
                httpResponse.getWriter().write("Rate limit exceeded");
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    private Bucket getBucket(String clientIp) {
        return buckets.computeIfAbsent(clientIp, k -> {
            Bandwidth limit = Bandwidth.builder()
                .capacity(10)  // 10 requests
                .refillGreedy(10, Duration.ofHours(1))  // per hour
                .build();
            return Bucket.builder()
                .addLimit(limit)
                .build();
        });
    }
}
```

#### Using Spring Cloud Gateway Rate Limiter

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: observability-export
          uri: http://localhost:8080
          predicates:
            - Path=/actuator/observability/export/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
```

### 4. Network-Level Security

#### Restrict Access by IP

Use firewall rules or Spring Security IP matching:

```java
.requestMatchers("/actuator/observability/**")
    .access(new IpAddressMatcher("10.0.0.0/8"))
```

#### Use VPN or Private Networks

Deploy monitoring infrastructure in a private network accessible only via VPN.

### 5. Audit Logging

Log all access to sensitive endpoints:

```java
@Aspect
@Component
@Slf4j
public class AuditLoggingAspect {

    @AfterReturning("execution(* com.imadattar.observability.export.ObservabilityStackExportController.*(..))")
    public void logExportAccess(JoinPoint joinPoint) {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            log.warn("AUDIT: Observability export accessed by {} from {}",
                request.getUserPrincipal().getName(),
                request.getRemoteAddr());
        }
    }
}
```

### 6. HTTPS Only

Always use HTTPS in production:

```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
```

Force HTTPS redirect:

```java
http.requiresChannel(channel ->
    channel.anyRequest().requiresSecure()
);
```

### 7. Path Traversal Protection

The starter includes comprehensive path traversal protection. Ensure it remains enabled:

- Never disable path validation
- Use only whitelisted export paths
- Avoid user-provided paths in production

### 8. Secrets Management

Never hardcode credentials in configuration files:

```yaml
# ❌ Bad
spring:
  security:
    user:
      password: admin123

# ✅ Good
spring:
  security:
    user:
      password: ${ADMIN_PASSWORD}
```

Use secrets management tools:
- Spring Cloud Config Server
- HashiCorp Vault
- AWS Secrets Manager
- Azure Key Vault

## Production Checklist

- [ ] Stack export endpoints disabled (`observability.stack-export.enabled=false`)
- [ ] Spring Security configured with authentication
- [ ] Rate limiting implemented for export endpoints
- [ ] HTTPS enabled with valid certificates
- [ ] IP restrictions configured for Prometheus scraping
- [ ] Audit logging enabled for sensitive endpoints
- [ ] Secrets externalized from configuration files
- [ ] Network firewall rules configured
- [ ] Regular security updates applied

## Monitoring Security Events

Monitor security-related metrics:

```java
@Component
public class SecurityMetrics {

    private final Counter authFailures;
    private final Counter rateLimitExceeded;

    public SecurityMetrics(MeterRegistry registry) {
        this.authFailures = Counter.builder("security.auth.failures")
            .description("Authentication failures")
            .register(registry);

        this.rateLimitExceeded = Counter.builder("security.ratelimit.exceeded")
            .description("Rate limit exceeded events")
            .register(registry);
    }
}
```

## Additional Resources

- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/)
- [OWASP Security Guidelines](https://owasp.org/www-project-top-ten/)
- [Spring Boot Actuator Security](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.endpoints.security)
- [Bucket4j Rate Limiting](https://github.com/bucket4j/bucket4j)

## Support

For security issues, please report privately to: security@example.com

Do not disclose security vulnerabilities publicly until they have been addressed.
