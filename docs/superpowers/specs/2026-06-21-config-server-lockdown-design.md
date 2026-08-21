# T2 (follow-up) — Config Server Lockdown — Design

- **Date:** 2026-06-21
- **Scope:** gecko remediation task T2 residual — lock down the open Spring Cloud Config server (`config-server-app`, `@EnableConfigServer`)
- **Source findings:** `gecko-security-report.md`, tracked in `gecko-remediation-tasks.md`
- **Branch:** `0_732` (continuation of the `gecko`-prefixed remediation stream)
- **Predecessor:** `2026-06-21-remote-service-auth-design.md` (the `/remote` half of T2)

## Overview

The Spring Cloud Config server (`config-server-app`) has **no Spring Security**
on its classpath — only `spring-cloud-config-server` — so its endpoints (which
serve every app's configuration) are unauthenticated. EE apps fetch config via
`spring.config.import: optional:configserver:http://localhost:6111` with **no
credentials**. This spec adds HTTP Basic auth to the config server and the
matching credentials to each client, with dedicated credentials and the existing
fail-open (`optional:`) client posture preserved.

## Components

### 1. Server — `config-server-app`

- Add a Spring Security dependency (`spring-boot-starter-security`).
- Add a `SecurityFilterChain` bean that:
  - permits `/actuator/health/**` (Kubernetes liveness/readiness probes),
  - requires authentication for every other request,
  - enables HTTP Basic,
  - disables CSRF (non-browser clients),
  - uses stateless sessions.
- The credential comes from Spring Boot's `spring.security.user.{name,password}`
  (auto-configured `InMemoryUserDetailsManager`), sourced from env:
  - `spring.security.user.name: ${BYTECHEF_CONFIG_SERVER_USERNAME:configserver}`
  - `spring.security.user.password: ${BYTECHEF_CONFIG_SERVER_PASSWORD:}`
  - A dev default password is set in `config-server-app`'s `application-dev.yml`
    so local distributed runs work; production sets the env var.

### 2. Clients — the 9 EE apps

Each app's **local** `application.yml` (the file carrying
`spring.config.import: optional:configserver:…`) gains:

```yaml
spring:
  cloud:
    config:
      username: ${BYTECHEF_CONFIG_SERVER_USERNAME:configserver}
      password: ${BYTECHEF_CONFIG_SERVER_PASSWORD:dev-config-server-secret}
```

These credentials must live in each client's local config (not in the
config-server-served config) because they are needed *before* the client can
fetch remote config — the bootstrap chicken-and-egg. The `optional:` prefix is
kept (fail-open). Apps: `scheduler-app`, `worker-app`, `webhook-app`,
`api-gateway-app`, `coordinator-app`, `ai-gateway-app`, `connection-app`,
`configuration-app`, `execution-app`.

### 3. Credentials

- Dedicated `BYTECHEF_CONFIG_SERVER_USERNAME` (default `configserver`) and
  `BYTECHEF_CONFIG_SERVER_PASSWORD`, separate from the `/remote` service token
  (independent rotation).
- Dev default password `dev-config-server-secret` on both sides so local
  `dev`-profile distributed runs authenticate out of the box; production sets a
  strong value via env on the config server **and** every client.

## Error handling / behavior

- Config server: unauthenticated request to a non-probe endpoint → `401`;
  health probes → permitted.
- Client (fail-open, `optional:`): if the client's credentials are missing or
  wrong (or the config server is down), the import is skipped and the app boots
  on its local/baked config — degraded, not failed. This is the accepted
  tradeoff of keeping `optional:`; deployments should verify clients actually
  loaded remote config.

## Testing

- A sliced security integration test for the config-server `SecurityFilterChain`
  (a `@SpringBootTest` loading the security configuration plus a stub endpoint,
  driven by MockMvc, with `spring.security.user.name/password` set via test
  properties): unauthenticated request → 401; correct Basic credentials → 200;
  `/actuator/health` → 200 without credentials. This avoids booting the full
  config-server context (which needs redis discovery).

## Rollout (operational note)

- Production must set `BYTECHEF_CONFIG_SERVER_USERNAME`/`PASSWORD` on the config
  server and on all 9 client apps. Because clients are fail-open, a mismatch
  degrades a client to local config silently rather than crashing — add a
  deploy-time check that remote config loaded.

## Out of scope

- Changing the fail-open (`optional:`) posture to fail-closed — deliberately kept.
- Encrypting config values at rest / Vault backend — separate concern.
- The `/remote` service-token auth — already shipped (predecessor spec).

## Defaults chosen (flag if these should change)

- HTTP Basic auth; dedicated `BYTECHEF_CONFIG_SERVER_*` credentials.
- `/actuator/health/**` permitted; everything else authenticated.
- Dev default `configserver` / `dev-config-server-secret`; prod via env.
- `optional:` client import preserved (fail-open).
