---
description: "ByteChef Configure Instance. Learn to configure your ByteChef deployment with environment variables and settings."
title: Configure Instance
---

<<<<<<< HEAD
ByteChef configuration controls how your self-hosted instance behaves, connects to external services, and runs in different environments. All configuration is applied through environment variables and is evaluated when the instance starts.

This page explains how configuration works, where it's applied, and how it relates to other deployment docs.


## How Configuration Works

ByteChef is configured entirely using environment variables.

These variables define:
- Database connections
- Security and authentication behavior
- Public URLs and networking
- Email delivery
- Feature flags
- Observability settings

Configuration is read when ByteChef starts. If you change a configuration value, you need to restart the instance for changes to take effect.

## Where Configuration Is Applied

Where you define configuration depends on how ByteChef is deployed.

### Docker / Docker Compose

Environment variables are defined in `docker-compose.yml` or passed at runtime using the `-e` flag. Secrets should be injected using Docker secrets or external secret managers.

**Example:**
```yaml
services:
  bytechef:
    environment:
      - BYTECHEF_DATASOURCE_URL=jdbc:postgresql://postgres:5432/bytechef
      - BYTECHEF_PUBLIC_URL=https://bytechef.example.com
```

### Kubernetes

Environment variables are defined using ConfigMaps for non-sensitive values and Secrets for credentials, tokens, and keys. Changes require a rollout restart of the deployment.

**Example:**
```yaml
envFrom:
  - secretRef:
      name: bytechef-secrets
  - configMapRef:
      name: bytechef-envs
```

Other container-based platforms follow the same pattern: configuration is injected at runtime using environment variables.

## Configuration Scope

Configuration affects multiple parts of the system:
- Application behavior and limits
- Authentication and session handling
- Encryption and credential storage
- Network and URL resolution
- Email notifications
- Feature availability
- Metrics, logs, and tracing

> **Note**
> 
> This page does not list individual configuration options. For a complete reference of all available variables, see the [Environment Variables](/self-hosting/configuration/environment-variables) page.

## Security Best Practices

- Don't hardcode secrets directly in configuration files
- Use secret management tools provided by your platform (Docker secrets, Kubernetes Secrets, Vault)
- Treat all environment variables as sensitive input
- Store encryption keys securely and back them up
- Changing encryption-related configuration can make existing data unreadable

Detailed configuration options are documented in [Environment Variables](/self-hosting/configuration/environment-variables)
=======

Documentation for configuring your ByteChef instance is coming soon. Please check back later!
>>>>>>> 002576c3f5 (docs: Update Docker deployment documentation and clean up files)
