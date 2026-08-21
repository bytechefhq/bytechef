# MCP Server Optional Authentication — Design

**Date:** 2026-07-15
**Status:** Approved (design)
**Related:** [MCP tool authorization design](2026-07-07-mcp-tool-authorization-design.md), [MCP dual-transport SSE + Streamable](2026-07-14-mcp-dual-transport-sse-streamable-design.md)

## Problem

Recent work added API-key and OAuth2 authentication to all three MCP servers
(automation, embedded, management). Authentication is currently **mandatory** for every
request that matches an MCP path — a request without a valid `Bearer` credential is
rejected with 401 before the target server is even resolved.

We want authentication to be **optional per MCP server**:

- **Existing servers:** authentication **not** required — so enabling this feature does not
  break any server that is already in use.
- **New servers:** authentication **required** by default.
- **Management server** (one per tenant): the same simple `true`/`false` toggle as the
  others.

The URL path secret embedded in every MCP endpoint (`/api/automation/<secret>/mcp`) is
itself a per-server secret and continues to protect a server whose `authenticationRequired`
is `false`; the toggle governs only the *additional* API-key / OAuth credential layer.

## Current architecture (as-is)

- **Filter-chain gate.** Automation and management install `McpApiKeyHttpConfigurer`
  (`platform-security-web`), which registers `TenantAwareApiKeyAuthenticationFilter` on a
  path regex (e.g. `^/api/automation/.+/(mcp|sse|message)(\?.*)?`). The shared
  `McpApiKeyAuthenticationConverter` **throws `BadCredentialsException` when the `Bearer`
  header is missing** — before any server lookup. Each `*ApiKeyAuthenticationProvider` then
  validates the key and cross-checks it against the resolved server (key type, environment,
  secret match).
- **Global authorization rule.** `SecurityConfiguration` (`config/security-config`) applies
  `.requestMatchers(mvc.matcher("/api/**")).authenticated()`. Every MCP endpoint lives under
  `/api/**`, so an anonymous request is rejected (403) unless a principal is present —
  skipping the auth filter alone is **not** sufficient.
- **Tool filter is already anonymous-tolerant.** The transport `contextExtractor` reads
  `secretKey` from the **URL path variable** and authorities from the **SecurityContext**
  (`SecurityUtils.fetchCurrentUserAuthorities()`, empty for an anonymous caller). Tool
  listing therefore works for an unauthenticated caller *provided the request reaches the
  handler*.
- **Two storage shapes.**
  - Automation + embedded servers are `mcp_server` rows mapped by the `McpServer` domain
    object (which already carries the sibling flag `enforceToolAuthorization`).
  - The **management** server is **not** a row. It is a tenant `Property` (`mcp.server`,
    scope `PLATFORM`) holding `{secretKey}`, read/written by `ManagementMcpServerServiceImpl`.
- **Embedded is bespoke.** `EmbeddedMcpServerSecurityConfigurer` extends
  `AbstractApiKeyHttpConfigurer` (not the shared `McpApiKeyHttpConfigurer`) and wires **three**
  interception points:
  1. a signing-key converter/provider (`EmbeddedMcpServerApiKey*`),
  2. an OAuth2 JWT converter/provider (`EmbeddedMcpServerOAuth2*`),
  3. `McpDiscoveryAuthenticationFilter`, which answers a token-less request with an RFC 9728
     discovery challenge (401 + `WWW-Authenticate`) instead of a bare 401.

## Design

### 1. Data model & defaults

**Automation + embedded (`mcp_server` rows).**

- Add `McpServer.authenticationRequired`, `@Column("authentication_required")`, beside the
  existing `enforceToolAuthorization`.
- Liquibase migration adds the column
  `authentication_required BOOLEAN NOT NULL DEFAULT false`. This backfills **all existing
  rows to `false`**, so no server currently in use starts demanding a credential.
- The Java field initializes to **`true`** (`private boolean authenticationRequired = true;`).
  Spring Data JDBC hydrates a loaded row by setting the property from the column value, so a
  legacy row loads `false` while a freshly-constructed `McpServer` defaults `true`. This
  yields "existing = false, new = true" with no create-path special-casing.
  - **Risk to pin with a test:** the default depends on hydration overwriting the field
    initializer. A mapping/integration test must assert (a) a newly created server persists
    `authenticationRequired = true`, and (b) a row inserted with `authentication_required =
    false` loads as `false`.

**Management (`mcp.server` Property).**

- Store the flag inside the property map alongside the secret:
  `{secretKey, authenticationRequired}`.
- A property **missing** the `authenticationRequired` key reads as `false` (existing tenants
  are unaffected).
- When the secret is first minted (the `else` branch of `getManagementMcpServerUrl`, and
  `updateManagementMcpServerUrl`), persist `authenticationRequired = true` for a brand-new
  configuration. Rotating the URL of an already-configured server preserves the existing
  flag value.

### 2. Enforcement mechanism

The auth decision must consult the resolved server's flag before treating a missing or
absent credential as fatal, and must still satisfy the global `/api/** → authenticated()`
rule. We therefore **produce a successful anonymous authentication** when a server opts out,
rather than skipping the filter.

**Anonymous MCP authentication token.** A small dedicated `Authentication` type (e.g.
`McpAnonymousAuthenticationToken`) that is `authenticated == true`, carries the path secret
as principal, and holds **no granted authorities**. It must **not** subclass
`AnonymousAuthenticationToken`, because Spring's `.authenticated()` authorization manager
explicitly rejects that class.

**Shared converter (`McpApiKeyAuthenticationConverter`, automation + management).**

- Stop throwing on a missing/malformed `Bearer` header. Always emit an unauthenticated
  `ApiKeyAuthenticationToken` whose `McpApiKeyCredentials` carry
  `(environment, mcpServerSecretKey, apiKeySecret-or-null)`. `apiKeySecret == null` is the
  marker for "no token presented"; the path secret is always extracted from the servlet path.

**Providers (`AutomationMcpServerApiKeyAuthenticationProvider`,
`ManagementMcpServerApiKeyAuthenticationProvider`).**

- Resolve the target server from the path secret (already done: automation via
  `McpServerService.getMcpServer(secretKey)`, management via `PropertyService`).
- Read `authenticationRequired`:
  - **`false`** → return an **anonymous MCP authentication** (no authorities). Per the
    "ignore tokens entirely" decision, any presented credential is disregarded — a valid,
    invalid, or absent token all resolve to the same anonymous principal. This satisfies
    `authenticated()`; the tool filter sees empty authorities.
  - **`true`** → require a real credential: a null `apiKeySecret` → `BadCredentialsException`
    (401); otherwise validate exactly as today (key type, environment, secret match).

**Why anonymous-authentication rather than filter-skip.** `/api/** → authenticated()`
requires a principal in the SecurityContext. Producing an authenticated (but authority-less)
principal is the minimal change that both lets the request through and leaves the existing
authorization rule untouched.

**Embedded.** All three embedded interception points must honor the flag:

1. the signing-key converter/provider,
2. the OAuth2 converter/provider,
3. `McpDiscoveryAuthenticationFilter` must **not** issue the discovery challenge when the
   resolved server has `authenticationRequired == false`; the request falls through as
   anonymous.

**Open semantic risk (embedded).** Embedded MCP tools are normally scoped to a *connected
user*, established from the credential. An anonymous embedded caller has no connected-user
identity, so "authentication not required" on embedded may yield an empty or degraded tool
set unless the URL secret alone can resolve the instance/connected-user context.

**Decision:** ship automation + management fully in this pass. For embedded, implement the
plumbing but treat the connected-user-context question as a risk to resolve during
implementation. Acceptable fallback if it cannot be resolved cleanly: embedded keeps
authentication mandatory and its toggle is disabled, documented as a known limitation.

### 3. Flag interaction: `authenticationRequired` vs `enforceToolAuthorization`

An anonymous caller has no authorities, and `enforceToolAuthorization` is deny-by-default, so
`authenticationRequired = false` combined with `enforceToolAuthorization = true` would hide
**all** tools. We prevent the combination.

**Invariant:** `authenticationRequired == false ⇒ enforceToolAuthorization == false`.

- **Server-side (source of truth).** `McpServerServiceImpl.update(...)` throws a validation
  error when an update would leave both flags in the forbidden state. The management update
  path enforces the same invariant.
- **Client.** When the authentication toggle is off, the enforce-tool-authorization toggle is
  disabled and forced off, with a tooltip explaining the coupling.

### 4. GraphQL + client surface

Mirror the existing `enforceToolAuthorization` wiring.

- `mcp-server.graphqls`: add `authenticationRequired: Boolean!` to the `McpServer` type and
  `authenticationRequired: Boolean` to `McpServerUpdateInput`. Wire it in
  `McpServerGraphQlController.updateMcpServer` identically to `enforceToolAuthorization`,
  applying the invariant check in the service.
- `management-mcp-server.graphqls`: add a query field for the current value and an update
  mutation (e.g. `updateManagementMcpServerAuthenticationRequired(authenticationRequired:
  Boolean!): Boolean!`). Wire controller → `ManagementMcpServerService` → Property storage,
  keeping the existing `isTenantAdmin()` gate.
- **Client.** Add the toggle to both `McpServerDialog.tsx` (automation +
  `ee/.../embedded/.../McpServerDialog.tsx`) and the management `McpServer.tsx`. Regenerate
  GraphQL types (`npx graphql-codegen`). Apply the toggle-coupling from §3.

### 5. Testing

- **Converter (unit):** a request with no `Bearer` header no longer throws; it yields an
  unauthenticated token with a null api-key secret and the correct path secret.
- **Provider (unit):**
  - `authenticationRequired = false` → returns anonymous MCP authentication, both with and
    without a presented token (token ignored).
  - `authenticationRequired = true`, no token → `BadCredentialsException`.
  - `authenticationRequired = true`, valid token → authenticates as today.
- **Domain default (mapping/int):** new `McpServer` → `authenticationRequired = true`; a row
  persisted with `authentication_required = false` loads as `false`.
- **Service validation:** `update` rejects `authenticationRequired = false` +
  `enforceToolAuthorization = true`.
- **Automation integration** (mirroring the existing enforce-tool-authorization int test):
  - server with `authenticationRequired = false` → `tools/list` succeeds with **no**
    `Authorization` header;
  - server with `authenticationRequired = true` → 401 without a token, succeeds with a valid
    key.
- **Management:** property missing the key → `false`; first URL creation → `true`; rotating
  an existing URL preserves the flag.
- **Embedded:** discovery challenge suppressed when `authenticationRequired = false`;
  connected-user-context behavior asserted against whatever resolution the implementation
  settles on.

## Out of scope

- No change to the URL path-secret mechanism; it remains the baseline protection for every
  server regardless of the toggle.
- No per-tool or per-component authentication granularity — this is a whole-server switch.
- No migration of the management server into the `mcp_server` table.
