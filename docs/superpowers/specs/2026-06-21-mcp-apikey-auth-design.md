# T3 — MCP API-Key Authentication Hardening — Design

- **Date:** 2026-06-21
- **Scope:** gecko remediation task T3 (Phase 0 Critical — MCP API-key auth bypass, CVSS 9.1–9.4)
- **Source findings:** `gecko-security-report.md`, tracked in `gecko-remediation-tasks.md`
- **Branch:** `0_732` (continuation of the `gecko`-prefixed remediation stream)

## Overview

The two MCP server authentication providers (Management and Automation) accept a
server secret but treat the per-user API key inconsistently and never use it for
access control. This spec commits both servers to an explicit **secret-only**
authentication model for this phase, hardens the secret check, and returns a
well-defined synthetic system principal. Real per-user identity (API key /
OAuth) is deferred to a follow-up phase for **both** servers.

### Reframing (honesty note)

The report framed T3 as "require and validate **both** credentials (server
secret + user API key)." Investigation showed the per-user API key is **dead
code for access control**:

- Nothing reads `getAuthSecretKey()` anywhere.
- No MCP tool or module reads the authenticated principal, authorities, or
  `SecurityContext`; tools that touch data derive scope elsewhere (below).
- Tenant scoping is set from the converter token's tenantId in
  `ApiKeyAuthenticationFilter` (`runWithTenantId(authentication.getTenantId(),
  ...)`), **before** the provider runs — independent of the result principal.
- Automation tool/workspace scoping is derived from the `McpServer` record
  (looked up by the secret) and its workspace mapping
  (`fetchWorkspaceIdByMcpServerId`), not from a user.

So the correct remediation is **not** "validate the second credential" — it is
"commit to secret-only, delete the misleading unvalidated user-API-key paths,
harden the secret comparison, and return a defined system principal." This is
recorded so the change is not mistaken for a two-factor implementation.

## Identity model (this phase)

- **Management MCP** = platform operator server (single platform secret in the
  `mcp.server` property). Synthetic system principal with `ROLE_ADMIN`.
- **Automation MCP** = per-`McpServer` server (per-record secret; scope bound to
  the server's workspace/components). Synthetic system principal with
  `ROLE_USER`.
- **Both:** the server secret stays in the URL path (deliberate phase decision).
  Real per-user identity is added next phase and will override this default
  principal — same token shape, so nothing downstream changes.

## Components

### Shared: synthetic system principal

After the secret check passes, build a principal on the fly (no DB lookup):

```java
new org.springframework.security.core.userdetails.User(
    "system", "", List.of(new SimpleGrantedAuthority(<role>)))
```

returned through the token's existing `(User)` constructor
(`AbstractApiKeyAuthenticationToken(User)` → `super(user.getAuthorities())` +
`setAuthenticated(true)`). `<role>` is `AuthorityConstants.ADMIN` for management,
`AuthorityConstants.USER` for automation. The result token does **not** need to
carry the tenantId (the filter already set `TenantContext` from the converter
token). `createdBy` will stamp `"system"` for any writes until per-user identity
lands.

### Management provider

`ManagementMcpServerApiKeyAuthenticationProvider.authenticate`:

- Read the `mcp.server` platform property `secretKey`.
- Reject (`BadCredentialsException`) when the configured secret **or** the
  request's path secret is null/blank.
- Compare with `MessageDigest.isEqual(...)` (constant-time) instead of
  `Objects.equals`.
- On success, return a token built from the system principal
  (`AuthorityConstants.ADMIN`).
- **Delete** the `apiKeyService` / `userService` / `authorityService`
  dependencies, the `authSecretKey != null` API-key branch, the
  `createSpringSecurityUser` helper, and the now-unused imports (`ApiKey`,
  `UserNotActivatedException`, etc.). Remove the dead token constructors that are
  no longer used.

### Automation provider

`AutomationMcpServerApiKeyAuthenticationProvider.authenticate`:

- Reject (`BadCredentialsException`) a null/blank path secret before lookup.
- Keep `mcpServerService.getMcpServer(secretKey)` — it throws
  `IllegalArgumentException` for an unknown secret (the real validation);
  translate that to `BadCredentialsException`.
- Add a `mcpServer.isEnabled()` check — a disabled server must not authenticate.
- Drop the redundant tautological `Objects.equals(mcpServer.getSecretKey(),
  secretKey)`.
- On success, return a token built from the system principal
  (`AuthorityConstants.USER`).
- Stop threading the unused `authSecretKey`; fix the token so `getPrincipal()`
  no longer returns the raw key string.

### Token classes

- Reuse `AbstractApiKeyAuthenticationToken(User)` for the authenticated result in
  both tokens.
- `AutomationMcpServerApiKeyAuthenticationToken`: drop the `authSecretKey` field
  and its `getPrincipal()` override (the `User`-based principal comes from the
  superclass). Keep the converter constructor that carries `tenantId` for the
  unauthenticated request token.
- `ManagementMcpServerApiKeyAuthenticationToken`: keep the `(User)` constructor;
  remove the no-arg "authenticated-with-nothing" constructor and the
  `authSecretKey` plumbing once the API-key branch is gone. Keep the converter
  constructor that carries `tenantId`.

## Error handling

- Missing/blank/wrong secret, unknown automation server, disabled automation
  server → `BadCredentialsException` (401).
- No behavioral dependence on the `Authorization` header this phase (secret-only).

## Testing

Per provider (unit tests on `authenticate`, mocking `PropertyService` /
`McpServerService`):

- valid secret → returns an authenticated token carrying the expected role
  (`ROLE_ADMIN` management / `ROLE_USER` automation);
- blank/empty configured secret or blank request secret → `BadCredentialsException`;
- wrong secret → `BadCredentialsException`;
- automation: disabled server (`isEnabled() == false`) → `BadCredentialsException`;
- secret-only: a request with no `Authorization` header still authenticates
  (confirming the per-user key is not required this phase).

## Out of scope (recorded for the tracker)

- Per-user identity (API key / OAuth) for both MCP servers — **next phase**; it
  will resolve a real user and override the system principal.
- Moving the server secret out of the URL path — deliberately kept this phase.

## Defaults chosen (flag if these should change)

- Management system principal authority: `AuthorityConstants.ADMIN`.
- Automation system principal authority: `AuthorityConstants.USER`.
- Synthetic principal username: `"system"`.
- Constant-time secret comparison via `MessageDigest.isEqual`.
