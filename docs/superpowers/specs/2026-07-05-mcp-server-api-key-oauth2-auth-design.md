# MCP Server Authentication: API Keys (Phase 1) and OAuth2 (Phase 2)

- **Date**: 2026-07-05
- **Status**: Approved design, pending implementation plan
- **Scope**: Automation and Management MCP servers. Embedded MCP server is explicitly out of scope (see "Out of scope").

## Problem

ByteChef exposes three MCP servers:

| Server | Path | Current authentication |
|---|---|---|
| Automation (CE) | `/api/automation/{secretKey}/mcp` | URL path secret only, validated against `McpServer.secretKey`; Bearer token optional and unused |
| Management (CE) | `/api/management/{secretKey}/mcp` | URL path secret only, validated against the `mcp.server.secretKey` platform property; Bearer token optional (validated as `ApiKey` when present, anonymous access when absent) |
| Embedded (EE) | `/api/embedded/{secretKey}/mcp` | Per-tenant signed JWT (`kid` = TenantKey, subject = `externalUserId`) — already strong |

For automation and management, a single URL-embedded secret is both the server identifier and the sole credential. URL secrets leak easily (server logs, proxies, pasted client configs), are not bound to a user, carry no permissions, and cannot be revoked per consumer. Anyone holding the URL has full access to the MCP server's tools.

## Decisions

These were settled during design review:

1. **Reuse `ApiKey`, do not resurrect `ApiClient`.** The `api_key` table already has the right shape: user binding (`user_id`), `type` discriminator (`AUTOMATION`, `EMBEDDED`, `null` = admin), `environment`, `last_used_date`. "Admin API keys" are not a separate entity — they are `api_key` rows with `type = null`, surfaced via the `adminApiKeys` GraphQL query. `ApiClient` (EE `automation-api-platform`, dormant) models an application credential with no user binding; MCP tools call `@PreAuthorize`-guarded facades that need a user-shaped principal, and the automation/management MCP servers are CE while `ApiClient` is EE. `ApiClient` stays dormant for its original API Platform purpose.
2. **Embedded MCP keeps its JWT flow.** A static API key cannot carry `externalUserId`, which is what scopes connections and integration instances for embedded. The existing SigningKey JWT flow is already stronger than an API key. No changes in either phase.
3. **URL paths stay; the path secret is demoted to an identifier.** `/api/automation/{secretKey}/mcp` and `/api/management/{secretKey}/mcp` keep working, and the path secret keeps selecting the `McpServer` entity (automation) or being checked against the platform property (management). A valid Bearer API key becomes mandatory: a request without one gets 401 regardless of the path secret. Hard cutover, no legacy grace mode — release-noted as a breaking change.
4. **Phase 2 embeds the authorization server in ByteChef** (no external IdP requirement). OSS deployments get out-of-box remote MCP with OAuth2.
5. **Build on `org.springaicommunity:mcp-security` now** (Approach B), not on the homegrown `AbstractApiKeyHttpConfigurer` chain. Verified compatibility: v0.1.13 (2026-06-12) is tested against Spring Boot 4.1.0, Spring AI 2.0.0, MCP SDK 2.0.0; ByteChef is on Boot 4.0.7 / Spring AI 2.0.0 / MCP SDK 2.0.0, and all three MCP servers use the WebMvc streamable-HTTP transport, which is the only transport the library supports. The Boot 4.0.7-vs-4.1.0 gap is de-risked with an early spike (below); fallback is bumping Boot to 4.1.x, not forking the library.

## Phase 1: Mandatory API keys

### Dependency

Add `org.springaicommunity:mcp-server-security:0.1.13` to `gradle/libs.versions.toml` and to the automation and management MCP server modules.

### Credential model

No schema change. Reuse `api_key`:

- **Automation MCP** requires an `ApiKey` with `type = AUTOMATION`; the key's `environment` must equal the target `McpServer`'s environment.
- **Management MCP** requires an `ApiKey` with `type = null` (admin key); the key's `environment` must match the `X-ENVIRONMENT` request header (default `PRODUCTION`, per the existing `AbstractApiKeyAuthenticationConverter` convention).

### Enforcement

The `SecurityConfigurerContributor`s for `^/api/automation/.+/mcp` and `^/api/management/.+/mcp` are rebuilt on the library's `mcpServerApiKey()` `HttpSecurity` configurer:

- **Custom `authenticationConverter`** reads `Authorization: Bearer <secretKey>` (ByteChef public-API convention, not the library's `X-API-Key` default) and parses the `TenantKey` out of the presented secret for tenant routing.
- **Custom `ApiKeyEntityRepository`** implementation delegates to `ApiKeyService`, executing inside `TenantContext.runWithTenantId(...)`. This is the adapter seam that preserves ByteChef's invariant — tenant context resolved before any repository call — without forking the library. (The library's `InMemoryApiKeyEntityRepository` and its bcrypt storage are demo-only and unused; `api_key.secret_key` storage is unchanged in this design.)
- **Principal construction** follows the existing `ManagementMcpServerApiKeyAuthenticationProvider` pattern: `ApiKey → userId → User → authorities`, so facade-level `@PreAuthorize` guards enforce real permissions on every tool invocation. `last_used_date` is updated on successful authentication only.

### Per-server flow

**Automation** (`/api/automation/{secretKey}/mcp`):

1. API-key filter: extract Bearer secret → parse `TenantKey` → tenant-scoped lookup → check exists and `type = AUTOMATION`. Failure → 401.
2. Transport handler extracts the path `secretKey` into `McpTransportContext` as today; the tool filter resolves the `McpServer` entity from it. New check at this layer: `McpServer.environment == ApiKey.environment` — the filter cannot do this check because it does not know which `McpServer` the path targets; the tool filter does.
3. Principal = the key's owning user with authorities.

**Management** (`/api/management/{secretKey}/mcp`):

1. API-key filter: same shape; key must have `type = null`. Environment check happens in the filter (key `environment` vs `X-ENVIRONMENT` header) because management tools are static and there is no per-server entity.
2. Path secret still checked against the `mcp.server.secretKey` platform property, as today.
3. The current "no Bearer → null authentication → anonymous access" branch in `ManagementMcpServerApiKeyAuthenticationProvider` is deleted. This deletion is the core security fix of phase 1.

### Error handling

- Missing, invalid, wrong-type, or wrong-environment key → **401**, matching what `ApiKeyAuthenticationFilter` returns today for the public API. **Correction (verified 2026-07-05 during implementation):** the phrase "with a JSON problem body" in the original draft was inaccurate. The public `ApiKeyAuthenticationFilter` and the global `/api/**` chain in `SecurityConfiguration` both use `HttpStatusEntryPoint(UNAUTHORIZED)` — a **bare 401 with an empty body**, not a JSON problem body. The MCP endpoints inherit exactly this via the library's default entry point, so they already match the public API's error shape. Accordingly, **no JSON body is added** and the integration tests assert the empty body to lock the contract. No `WWW-Authenticate` challenge in phase 1 — that header is the OAuth discovery entry point and must not appear until phase 2 provides the metadata it points at.
- Unparseable `TenantKey` in the presented secret → same 401 path. Responses do not reveal which check failed.
- Failures logged at `WARN` with tenant and path, never the key material.

## Phase 2: OAuth2 with embedded authorization server

### Topology

- ByteChef server-app embeds the authorization server via `org.springaicommunity:mcp-authorization-server` (Spring Authorization Server pre-configured with MCP-specific features, notably Dynamic Client Registration).
- The automation and management MCP endpoints add OAuth2 **resource-server** support via `mcp-server-security`'s OAuth configurer.
- **API keys remain accepted.** Phase 2 adds a second credential type on the same endpoints; it does not replace the first.

### Client journey

1. Unauthenticated request → **401 + `WWW-Authenticate`** carrying the protected-resource-metadata URL (RFC 9728). This replaces phase 1's bare 401.
2. Client fetches the protected-resource metadata → discovers the authorization server → fetches AS metadata → performs Dynamic Client Registration.
3. Authorization-code + PKCE flow. The AS delegates end-user authentication to ByteChef's existing Spring Security login — no second user store — followed by a consent screen.
4. Client presents the issued JWT as Bearer; the resource-server side validates signature, issuer, and audience.

### Identity, tenancy, scopes

- Issued JWTs carry the ByteChef user as subject plus a tenant claim; the resource-server filter establishes `TenantContext` from that claim before any repository access — the same route-by-credential invariant as phase 1.
- The principal maps to the same user + authorities shape as phase 1, keeping facade `@PreAuthorize` credential-agnostic.
- Scopes `mcp:automation` and `mcp:management` distinguish the two surfaces, requested at DCR/consent time and enforced per endpoint.
- Environment selection stays with the `X-ENVIRONMENT` header; it is not baked into tokens.

### Persistence and administration

- Spring Authorization Server's standard tables (`oauth2_registered_client`, `oauth2_authorization`, `oauth2_authorization_consent`) arrive as new Liquibase changelogs in a platform module, tenant-scoped consistently with the rest of the schema.
- DCR-created clients are listable and revocable in the admin UI (minimal: list + delete).
- The AS is gated behind `bytechef.oauth2.authorization-server.enabled` (registered as a field in the central `ApplicationProperties` — strict binding), so its endpoints do not appear until enabled.

### Edition placement

~~The authorization server and the OAuth support for automation/management MCP go in **CE**: both servers are CE, and out-of-box remote MCP with OAuth is a headline OSS capability. If embedded MCP ever gets OAuth, federation with the customer's own IdP is the natural shape — out of scope here.~~

**Correction (2026-07-06): edition placement changed to EE.** MCP strong authentication — the API-key auth (Phase 1) *and* the OAuth2 authorization server / resource server (Phase 2) — is now an **EE-only** feature, gated by `@ConditionalOnEEVersion` (`bytechef.edition=ee`). Consequences: in a **CE** deployment the automation & management MCP servers accept the **URL path secret only** (Phase 1's mandatory-API-key hardening does not apply to CE — release-note this). The Phase 1 shared classes moved to EE `platform-security-web-impl`, the per-server configurers to EE `automation-security-web-impl` / `platform-security-web-impl`, and the authorization server to EE `platform-oauth2-authorization-server` (all `com.bytechef.ee.*`). **Also reversed:** external-IdP federation is no longer out of scope — the resource server (Phase B) must validate JWTs from an external OAuth2 authorization server (customer IdP) as an alternative/addition to the embedded AS; the external-user → ByteChef-user mapping is the key open design question for Phase B.

## Testing

### Phase 1

- **Unit tests** (`*Test`): the Bearer/`TenantKey` authentication converter (extraction, malformed tokens), the `ApiKeyService`-backed repository adapter (tenant context established before lookup), and the type/environment check logic at both layers.
- **Integration tests** (`*IntTest`, Testcontainers PostgreSQL, `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `testint` profile), one per server module:
  - valid path secret + valid key → MCP `initialize` + `tools/list` succeed over real streamable HTTP;
  - valid path secret + **no Bearer** → 401 — the regression test for the vulnerability being closed;
  - wrong key type (e.g. `EMBEDDED` key on the automation endpoint) → 401;
  - environment mismatch → rejected;
  - valid key + wrong path secret → rejected (path identity still enforced).
- **Day-one spike**: compile and one green integration test against Boot 4.0.7 with `mcp-server-security:0.1.13` before building anything else, to close the 4.0.7-vs-4.1.0 validation gap early.

### Phase 2

Integration tests for: the discovery chain (401 → `WWW-Authenticate` → protected-resource metadata → AS metadata); DCR followed by a full authorization-code + PKCE flow with a test client; token validation on MCP endpoints (expired / wrong audience / wrong issuer → 401); and coexistence (API key still authenticates after OAuth is enabled).

## Rollout

- **Phase 1** ships as one server-side change-set (automation MCP module, management MCP module, shared adapter code in `platform-security-web`) plus a small client change: the MCP server settings UI's connection snippet gains the `Authorization: Bearer <api-key>` header and points at the existing API Keys / Admin API Keys settings pages. Release notes flag the breaking change: existing MCP URLs keep working only after adding the header.
- **Phase 2** is an independent change-set behind the AS enable property; API-key auth is the always-on baseline.

## Out of scope

- Embedded MCP server changes (both phases).
- Hashing `api_key.secret_key` at rest (future hardening, orthogonal to this work).
- OAuth2 for the public REST APIs (`/api/automation/v1`, etc.) — this design covers MCP endpoints only.
- Reviving `ApiClient` for any purpose.
