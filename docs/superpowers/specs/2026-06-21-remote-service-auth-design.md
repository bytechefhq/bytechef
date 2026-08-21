# T2 — Internal `/remote/**` Service Authentication — Design

- **Date:** 2026-06-21
- **Scope:** gecko remediation task T2 (Phase 0 Critical — unauthenticated internal microservice endpoints, 7× Critical)
- **Source findings:** `gecko-security-report.md`, tracked in `gecko-remediation-tasks.md`
- **Branch:** `0_732` (continuation of the `gecko`-prefixed remediation stream)

## Overview

EE microservices call each other over HTTP at `/remote/**`. These endpoints are
**unauthenticated**: any client that can reach a node's port can invoke them.
This spec adds **service-to-service authentication** — a shared secret token sent
on every outgoing `/remote` call and validated, fail-closed, by a servlet filter
on the receiving side.

### Why this is needed (and is not redundant with facade authorization)

There are two distinct controller→service paths, and only one is guarded:

- `/api/**` → user-facing facade (guarded by `@PreAuthorize` on the **acting
  user's** authorities) → service.
- `/remote/**` → the **service tier** directly, with **no `@PreAuthorize` and no
  user principal** — it trusts that the caller is a sibling node.

Verified: every remote-reached impl sampled (`ContextServiceImpl`,
`CounterServiceImpl`, `TaskExecutionServiceImpl`, `JobServiceImpl`,
`TriggerStateServiceImpl`, `TriggerExecutionServiceImpl`, `PrincipalJobFacadeImpl`,
`TriggerLifecycleFacadeImpl`, `ComponentDefinitionFacadeImpl`,
`ActionDefinitionFacadeImpl`) has **zero `@PreAuthorize`**, and **no user
authentication is propagated** over `/remote` (only `CURRENT_TENANT_ID`). The
guards cannot move here: with no user principal on the call, a `hasAuthority(...)`
check would reject every legitimate inter-service request. `RemoteTaskHandlerController`
even executes an arbitrary task from the request body
(`taskHandlerRegistry.getTaskHandler(type).handle(taskExecution)`) with no auth.

So service authentication is a **separate axis** from user authorization: the
facade `@PreAuthorize` answers "which user may do this" (on `/api`); the service
token answers "is the caller a real ByteChef node" (on `/remote`). This spec adds
the second axis and leaves the facade-tier user-authorization untouched.

## Why a servlet filter (not a Spring Security chain)

Lightweight EE apps (e.g. `worker-app`) have **no Spring Security** on the
classpath — no `SecurityFilterChain`, no `security-config` dependency. The only
thing intercepting `/remote/**` on those apps is the existing `@Component`
servlet filter `RemoteMultiTenantFilter` (from `remote-rest`, which every EE app
exposing `/remote` depends on). The new auth must therefore be a plain servlet
`@Component` `OncePerRequestFilter` in `remote-rest`, so it applies on every app
regardless of its Spring Security setup.

## Components

### 1. Shared token + header

- **Property:** `bytechef.internal.service-token` — a strong shared secret,
  distributed to every EE app (config server / env var). Read via
  `@Value("${bytechef.internal.service-token:}")` on both the server filter and
  the client (no `ApplicationProperties` dependency, avoiding module cycles —
  matching the pattern used for the TOTP/SSRF settings).
- **Header name constant:** a single shared constant (e.g.
  `INTERNAL_SERVICE_TOKEN` = `"X-Bytechef-Internal-Token"`) placed where both
  `remote-rest` (server) and `remote-client` (client) can see it — both already
  depend on `tenant-api`, which is where the existing transport-header constant
  (`TenantConstants.CURRENT_TENANT_ID`) lives, so a sibling constants class there
  is the natural home. (Exact class name finalized in the plan.)

### 2. Server side — `RemoteServiceAuthenticationFilter`

New `@Component OncePerRequestFilter` in `remote-rest`, mirroring
`RemoteMultiTenantFilter`:

- `shouldNotFilter` returns true for anything **not** under `/remote/**` (same
  `NegatedRequestMatcher("/remote/**")` pattern).
- Ordered **before** `RemoteMultiTenantFilter` — add explicit `@Order` to both so
  an unauthenticated caller is rejected before any tenant context is established
  from its (untrusted) header.
- **Fail-closed:**
  - if the configured server token is null/blank → `401` (do not chain);
  - if the request's `X-Bytechef-Internal-Token` header is null/blank or does not
    match (constant-time `MessageDigest.isEqual`) → `401`;
  - otherwise continue the chain.

### 3. Client side — `AbstractRestClient`

All remote clients funnel through `AbstractRestClient` (both `DefaultRestClient`
and `LoadBalancedRestClient` extend it). The ~10 repeated
`.header(CURRENT_TENANT_ID, …)` sites are refactored into one shared helper that
sets **both** the tenant header and `X-Bytechef-Internal-Token` (from the
injected token). The token is injected with `@Value`. This single change covers
every outgoing `/remote` call.

### 4. Editions

Monolith (`mono`) makes no `/remote` calls (everything is in-process) and
`remote-client` is EE-only, so the monolith is unaffected. EE distributed mode
must configure the token (fail-closed).

## Error handling

- Missing/blank/mismatched token, or unconfigured server token → HTTP `401`, chain
  not invoked, no tenant context set.

## Testing

- **`RemoteServiceAuthenticationFilter`** (servlet unit test, mock
  request/response/chain, mirroring `RemoteMultiTenantFilterTest`): valid token →
  chain proceeds; missing header → 401; wrong token → 401; blank server token →
  401 (fail-closed).
- **`AbstractRestClient`** outgoing-header helper: sets both the tenant header and
  the internal-token header from the configured values (test the helper directly).

## Rollout (operational note)

- **Fail-closed means existing EE deployments must set
  `bytechef.internal.service-token` before/at upgrade**, or `/remote` calls will
  be rejected (services can't talk to each other). Document the env var.
- Provide a **dev/integration default token** via the EE config (the apps'
  config-server `application.yml`) so distributed dev and integration tests keep
  working; production sets a strong value via env var.

## Out of scope (recorded for the tracker)

- **Spring Cloud Config server lockdown** (`@EnableConfigServer`, open endpoints)
  — split into a separate follow-up spec, as agreed.
- mTLS between services — heavier alternative; a possible future hardening. The
  shared token is the chosen mechanism.
- Propagating user identity over `/remote` — these services are intentionally
  internal infra without per-user authorization; out of scope.

## Defaults chosen (flag if these should change)

- Header: `X-Bytechef-Internal-Token`; property `bytechef.internal.service-token`.
- Fail-closed (unconfigured/blank token rejects all `/remote`).
- Constant-time comparison via `MessageDigest.isEqual`.
- Token injected via `@Value`; field injection on `AbstractRestClient` (localized
  exception to constructor-injection, to avoid threading it through the ~8
  per-app `RestClientConfiguration` beans).
