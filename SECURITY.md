# Security Policy

## Supported Versions

| Version | Supported |
|---|---|
| 1.3.x | Yes |
| < 1.3 | No |

## Reporting a Vulnerability

If you discover a security vulnerability, please report it responsibly:

1. **Do NOT open a public GitHub issue**
2. Email: attar.imadeddine@gmail.com
3. Include:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if any)

You will receive a response within 48 hours. Critical vulnerabilities will be patched and released within 7 days.

## Security Considerations

This starter exposes actuator endpoints and a monitoring stack export API. In production:

- Disable `observability.stack-export.enabled` or secure it with Spring Security
- Restrict actuator endpoint exposure via `management.endpoints.web.exposure.include`
- Change default Grafana credentials (admin/admin)
- Configure appropriate `tracing.sampling-probability` to limit data volume

See the [Security section in the README](README.md#-security--production-deployment) for detailed guidance.
