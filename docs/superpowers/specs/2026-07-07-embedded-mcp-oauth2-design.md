# Embedded MCP Server OAuth2 (Federated) — Design

**Status:** DRAFT — design approved in brainstorming (2026-07-07); SP-A ready to plan, SP-B gated on a spike.

## Goal

Let generic MCP clients (Claude Desktop, Cursor, ChatGPT, end-user agents) connect to a ByteChef **embedded** MCP server (`/api/embedded/{secretKey}/mcp`) using standard OAuth2 — discovering the authorization server, registering, logging in, and presenting a Bearer JWT — where the authenticated end user is materialized as a ByteChef **ConnectedUser**, not a ByteChef platform user. Federate that login to the OEM customer's own IdP, and also accept the OEM IdP's tokens directly.

This extends the automation/management MCP OAuth2 work (spec `2026-07-05-mcp-server-api-key-oauth2-auth-design.md`) to the embedded/OEM surface, whose identity model is fundamentally different.

## Edition — all EE

The whole embedded subsystem lives under `server/ee/libs/embedded/`, and this feature is external-IdP **federation** — the same class of capability as SSO/SAML (`server/ee/libs/config/security-sso-config`), which is EE. So embedded MCP OAuth2 is entirely EE.

This is independent of the separate follow-up to move the *automation/management* API-key + base OAuth2 back to CE (see Roadmap). That CE relocation keeps only **external-issuer acceptance** EE-gated; it does not touch embedded, which is EE regardless.

## Current embedded auth model (what exists today)

The embedded endpoint already authenticates with a **JWT** — but a bespoke, ByteChef-signed one, not OAuth2:

- `EmbeddedMcpServerSecurityConfigurer` (extends `AbstractApiKeyHttpConfigurer`, path `^/api/embedded/.+/mcp`) →
- `EmbeddedMcpServerApiKeyAuthenticationConverter`: parses the Bearer token as a JWS signed by ByteChef's per-tenant `SigningKeyService`; the JWT `kid` header is a `TenantKey` (→ tenant + signing public key); the `sub` claim is the OEM's `externalUserId`; environment from the `X-Environment` header.
- `EmbeddedMcpServerApiKeyAuthenticationProvider`: `connectedUserService.fetchConnectedUser(externalUserId, environmentId)` **or auto-creates** one; principal = a Spring `User` whose username is the connected user's external id.
- The transport `contextExtractor` (`EmbeddedMcpServerConfiguration`) reads `externalUserId = SecurityUtils.getCurrentUserLogin()`, `secretKey` (path), and `X-Environment`; downstream, tenant is `TenantKey.parse(mcpServer.getSecretKey())`.

**Identity invariant to preserve:** whatever authenticates a request must (a) establish the tenant, (b) resolve/create a `ConnectedUser`, and (c) leave `SecurityUtils.getCurrentUserLogin()` = the external user id so the existing tool facade keeps working. The existing signing-key path must keep working (coexistence — do not break current OEMs).

---

## Phase 1 (SP-A) — Resource server on the embedded endpoint (accept tokens)

Make `/api/embedded/{secretKey}/mcp` accept an OAuth2 Bearer JWT alongside the existing signing-key JWT.

### Reused from the automation/management resource server (EE `platform-security-web-impl`)
- `MultiIssuerJwtDecoder` — per-`iss` decoder selection.
- `McpJwtDecoderFactory` / `IssuerLocationMcpJwtDecoderFactory` — per-issuer JWKS via `/.well-known`.
- `McpBearerTokenResolver` — resolve a Bearer token only when it is a JWT (not a signing-key token / API key). Its MCP-path regex extends to include `/api/embedded/...`, or the embedded chain gets its own resolver instance scoped to the embedded path.

### New for embedded
- **`EmbeddedMcpJwtIdentityMapper`** (embedded analogue of `McpJwtIdentityMapper`): a validated `Jwt` →
  - **user:** `sub` (or a per-issuer configured claim) → `ConnectedUserService.fetchConnectedUser(externalId, environmentId)` **or create** — identical to `EmbeddedMcpServerApiKeyAuthenticationProvider`.
  - **tenant:** from the URL path secret, **not** the token — mirroring the current model (`TenantKey.parse` of the endpoint's tenant-resolvable path secret). A raw OEM-IdP token therefore needs no tenant claim.
  - **environment:** from the `X-Environment` header (unchanged).
  - **principal:** a Spring `User` whose username = external user id, so `SecurityUtils.getCurrentUserLogin()` is unchanged for the transport `contextExtractor`.
- **Tenant-aware JWT filter (embedded variant):** establish `TenantContext` from the path-secret tenant before the ConnectedUser lookup / repo access, then run the chain in that tenant — the embedded analogue of `TenantAwareJwtAuthenticationFilter`.
- **Per-tenant trusted issuers (the one genuinely new requirement):** a token from tenant A's IdP must not authenticate on tenant B's endpoint. Since tenant is fixed by the path secret, the resource server must verify the token's `iss` is trusted **for that tenant**. Source the per-tenant trusted issuers from `IdentityProviderService` (existing per-tenant IdP config), not a single global list. The ByteChef embedded AS issuer is always trusted (needed to validate SP-B's minted tokens).

### Tenant resolution (design note to confirm in implementation)
The embedded path secret must be tenant-resolvable *before* any DB access (the API-key path solves the same problem by parsing the credential as a `TenantKey`). Confirm the embedded endpoint's `{secretKey}` is either itself a `TenantKey` or otherwise yields the tenant without a pre-tenant DB query; if not, add a resolution step. This is the first implementation task.

### Coexistence
Signing-key token vs OAuth2 JWT are distinguished the same way API keys vs JWTs are: the OAuth2 path claims a token only if it validates against a trusted issuer; the signing-key converter continues to own its tokens. Both filters partition the Bearer space; a token neither claims is rejected by the chain.

### Tests (SP-A)
Embedded `*IntTest` (Testcontainers, real streamable HTTP), mirroring `AutomationMcpOAuth2ResourceServerSecurityIntTest`:
- an OAuth2 JWT (test static-JWKS issuer, `sub`) authenticates → `tools/list`, and resolves/creates a ConnectedUser in the path-secret tenant;
- the existing **signing-key** token still authenticates (coexistence);
- a JWT whose issuer is trusted for a *different* tenant → 401 (per-tenant issuer isolation);
- expired / untrusted issuer → 401.

---

## Two federation models (reprioritized 2026-07-07)

Prompted by the "your IdP runs the show" MCP-gateway pattern (Composio), the primary enterprise path is **external-direct**, not the ByteChef broker. Both share SP-A's resource server; they differ only in **which authorization server the endpoint's discovery advertises**.

### Primary — External-direct ("the tenant's IdP runs the show")
ByteChef is a **pure resource server**; the tenant's IdP is the authorization server. Discovery for `/api/embedded/{secretKey}/mcp` advertises `authorization_servers = [the tenant's flagged IdP issuer]`. A generic MCP client discovers the tenant's IdP, does DCR + auth-code **directly against it**, gets an IdP token, and presents it to ByteChef — which **already validates it** (SP-A, per-tenant trusted issuers) and maps `sub` → `ConnectedUser`. No ByteChef AS in the loop, no broker.

- **Only missing piece: discovery** (B-2) advertising the tenant's IdP as the AS. SP-A does the rest.
- **Caveats:** (a) the IdP must support **DCR (RFC 7591)** so a generic MCP client can register itself — when it can't, fall back to the broker (SP-B). (b) **Audience validation matters here** (the deferred `validateAudienceClaim`): an IdP token minted for another relying party must not be replayable at ByteChef — the token's `aud`/resource must include ByteChef's MCP resource. Reconsider the SP-A audience deferral for this path.

### Fallback — ByteChef-brokered (Phase 2 / SP-B, below)
For tenants whose IdP can't run the show directly (no DCR, needs uniform tokens/consent, or the direct-prosumer "ByteChef is its own OEM" case): ByteChef's embedded AS is the authorization server and brokers login to the tenant's IdP (or ByteChef's own login → ConnectedUser, or a social IdP). Larger; the "which IdP / which login source" knob (external IdP · ByteChef-login→ConnectedUser · social) is a first-class part of B-3.

### IdP-driven tool *authorization* — DECIDED (2026-07-07): both, configurable per tenant
Default: tool access comes from the connected user's **provisioned connections** (what SP-A does today). **Optional, per-tenant opt-in:** the tenant's IdP `groups`/`roles` claim also gates *which tools* a user may call — layering the same claim→authority mapping the automation/management resource server already has (`McpJwtIdentityMapper.authoritiesClaim`) onto the embedded `ConnectedUser` path. Requires: (a) a per-tenant config for the claim name + on/off (natural home: the `IdentityProvider` record, alongside the `mcp` flag); (b) the embedded identity mapper reads those claim authorities when enabled; (c) tool filtering honors both connection-provisioning **and** the mapped authorities (intersection). When off, behavior is unchanged. This is additive to SP-A and orthogonal to external-direct vs broker — both token sources carry the claims.

---

## Phase 2 (SP-B) — Brokered authorization-code flow (issue tokens) — FALLBACK

> Fallback path for IdPs that can't be the direct AS (see "Two federation models"). Build **external-direct discovery first**; SP-B second.

Give generic MCP clients the standard handshake. Depends on SP-A (the token SP-B mints is validated by SP-A's resource server trusting the embedded AS issuer).

### Not a separate authorization server — one AS, two login sources

SP-B does **not** stand up a new authorization server. It reuses the **same** ByteChef embedded AS (`platform-oauth2-authorization-server`, Phase A) that **automation/management already use**: DCR + authorization-code + PKCE + token minting are shared. The `resource`→`tenant_id` customizer lives in that shared AS config and is path-specific (it parses only `/api/embedded/...` resources; automation/management fall back to `TenantContext`).

The **only** thing SP-B adds is the embedded **login source + identity type** on that shared AS:

| | Automation/management (Phase A/B, exists) | Embedded (SP-B) |
|---|---|---|
| Authorization server | ByteChef embedded AS | **same** AS |
| DCR + auth-code + PKCE + token minting | ✅ | **same** |
| Login step | ByteChef form-login | **federate to the tenant's IdP** |
| Resulting principal | ByteChef user | **ConnectedUser** |
| Token `sub` | ByteChef user login | external end-user id |

So the first-party ↔ embedded split is **two login sources on one AS**, not two servers. SP-B is scoped to embedded only because "federate → ConnectedUser" is the one login path automation/management never need. (And after the reprioritization, embedded's *primary* path is external-direct — no ByteChef AS at all — so SP-B's broker narrows to the fallback + the direct-prosumer "ByteChef is its own OEM" case; automation/management keep the ByteChef AS as their normal brokered path.)

### Flow
1. Client hits `/api/embedded/{secretKey}/mcp` with no token → `401 + WWW-Authenticate` → RFC 9728 protected-resource metadata (the discovery filter + metadata chain built in B.3) → points at the ByteChef embedded AS.
2. Client does DCR + authorization-code + PKCE against the embedded AS.
3. At the authorize endpoint the end user is unauthenticated → **federate login to the OEM's per-tenant IdP** (reuse `security-sso-config`'s `DynamicClientRegistrationRepository` — it already resolves per-tenant OAuth2 client registrations for SSO).
4. On successful federation, **materialize a `ConnectedUser`** (fetch-or-create) from the federated identity, and make it the authenticated principal the AS issues a code for.
5. The AS mints an access token with the connected-user `sub` + tenant + environment + `scope` claims — via an `OAuth2TokenCustomizer` (like the `tenant_id` customizer already built for the platform AS).
6. Client presents the token → SP-A validates it and maps to the ConnectedUser.

### Reused
- RFC 9728 discovery + protected-resource metadata (B.3 — already built).
- `DynamicClientRegistrationRepository` + SSO federation (`security-sso-config`).
- The platform authorization server module (`platform-oauth2-authorization-server`) + its token-customizer pattern.
- `ConnectedUserService` fetch-or-create.

### Spike outcome (resolved 2026-07-07) — Option A: RFC 8707 `resource` parameter

**The tenant rides into the AS on the standard MCP `resource` parameter**, not a tenant-scoped issuer. The client sends `resource=<baseUrl>/api/embedded/{secretKey}/mcp` on the authorize (and token) request; a custom `authorizationRequestConverter` on the embedded-AS chain parses the `{secretKey}` (a `TenantKey`) → tenant → `TenantContext`. Confirmed feasible: Spring Authorization Server (via `mcp-authorization-server:0.1.13`) supports the `resource` parameter and `ResourceIdentifierAudienceTokenCustomizer`; `OAuth2Authorization` carries attributes from authorize→token; and the `jwtTokenCustomizer` already built mints `tenant_id` from `TenantContext`. **Environment stays on the MCP request's `X-Environment` header** (SP-A reads it there), so the token needn't carry it, and SP-A's provider already materializes the `ConnectedUser` from the token `sub` + header environment — so the AS flow is just "federate → mint token with `sub` + `tenant_id`", and step 4 above (materialize connected user at the AS) is **not** needed.

**IdP selection — DECIDED (2026-07-07): a dedicated "embedded MCP" flag on `IdentityProvider`.** The AS federates the end-user login to the tenant's IdP that is flagged for embedded MCP (a tenant may separately configure IdPs for its ByteChef admins vs. its embedded end users). Adds a boolean column/field + service/GraphQL/UI to set it.

**The real work (the spike undersold it):** the existing SSO federation (`SsoOAuth2AuthenticationSuccessHandler`) is built for **ByteChef users** — it maps the IdP email → a ByteChef user → tenant-by-email and redirects to `/oauth2/redirect`. Embedded needs a **connected-user federation path**: tenant from the `resource` param (not email), auto-federate to the tenant's flagged IdP (skip ByteChef's `/` login page — redirect straight to `/oauth2/authorization/sso-{idpId}`), and leave the **federated OAuth2 principal** (external `sub`) authenticated so the AS issues a code for it (no ByteChef user). SP-A then turns `sub` into a `ConnectedUser` at MCP-request time.

**Residual proof-of-concept gate (do first in SP-B):** confirm Spring Authorization Server issues an authorization code for the **federated OAuth2/OIDC principal** (a non-ByteChef user) returning to the pending authorize request, with a connected-user-aware success handler that does NOT remap to a ByteChef user. This is the one mechanic the spike did not execute.

### Decomposition (SP-B sub-phases)
- **B-1** — `IdentityProvider` "embedded MCP" flag: Liquibase column + domain field + service + GraphQL/UI to set it, and a lookup "the tenant's embedded-MCP IdP". Self-contained.
- **B-2** — Discovery on the embedded endpoint: `401 + WWW-Authenticate` + RFC 9728 protected-resource metadata for `/api/embedded/*/mcp` pointing at the embedded AS (reuse the B.3 filter/metadata pattern, tenant-aware).
- **B-3** — AS brokered flow: `authorizationRequestConverter` (resource→tenant→`TenantContext`) + auto-federate entry point to the tenant's flagged IdP + connected-user federation success handling + token minting (`sub`+`tenant_id`+`scope`) + add the embedded AS issuer to SP-A's trusted-issuer set so SP-A validates SP-B's tokens. Gated on the residual POC above.

### Tests (SP-B)
- Full generic-client handshake against a test OEM IdP (static issuer): discovery → DCR → auth-code+PKCE with federated login → minted token → `tools/list` as the resolved connected user in the correct tenant (environment from the header).
- Wrong-tenant isolation; a tenant with no flagged embedded IdP → clear authorize error.

---

## Coexistence & non-goals

- The existing signing-key embedded JWT keeps working throughout (hard requirement).
- No change to the automation/management endpoints in this feature.
- The CE relocation of base auth is a separate track (Roadmap), not part of this spec.

## Roadmap / sequencing (reprioritized 2026-07-07)

1. **SP-A** — embedded resource server. **DONE** (validates external-IdP tokens → ConnectedUser; 15 tests).
2. **External-direct discovery (B-2) — PRIMARY, next.** Tenant-aware RFC 9728 discovery on `/api/embedded/*/mcp` advertising the tenant's flagged IdP issuer as the authorization server (reuse the B.3 filter/metadata pattern). This alone delivers the "your IdP runs the show" path on top of SP-A — smaller than SP-B, and the more gateway-native posture. Reconsider the deferred **audience validation** here (token replay).
3. **SP-B — brokered AS flow (FALLBACK).** For IdPs that can't be the direct AS. B-1 flag + B-3 brokered federation. Partially done: B-3 POC (resource→`tenant_id`) ✅, B-1 server core ✅; remaining B-1 UI + full B-3 (federation) + B-2-variant discovery pointing at ByteChef's AS.
4. **IdP-claims → tool authorization** — decide + (if yes) layer claim→authority mapping onto the embedded ConnectedUser path.
5. **CE relocation (separate spec/plan)** — move automation/management base auth to CE; external-issuer acceptance stays EE-gated. Does not touch embedded.

## Open questions / gates

- **SP-A:** confirm the embedded path secret is tenant-resolvable without a pre-tenant DB query (first implementation task).
- **SP-A:** exact shape of per-tenant trusted-issuer config sourced from `IdentityProviderService` (which IdP records are MCP-eligible; whether a dedicated flag/type is needed).
- **SP-B (blocking spike):** tenant/environment propagation from endpoint discovery to the AS authorize step for per-tenant IdP selection.

## Risks

- **[MED] Per-tenant issuer isolation** — the core new security property; a token trusted for tenant A must never authenticate on tenant B's endpoint. Guarded by resolving trusted issuers strictly within the path-secret tenant, with an explicit cross-tenant-rejection test.
- **[MED] ConnectedUser auto-provisioning from federated identity** — a valid federated login auto-creates a ConnectedUser; ensure this matches the existing signing-key path's provisioning semantics and the OEM's expectations (the OEM's IdP is the gate).
- **[MED] SP-B tenant-through-discovery** — unresolved until the spike; blocks SP-B step-level detail (honest gate, not a placeholder).
- **[LOW] Coexistence** — signing-key and OAuth2 tokens on one chain; the trusted-issuer check partitions them, guarded by a coexistence test.
