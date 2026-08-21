# MCP Server OAuth2 (Phase 2) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add OAuth2 to the automation (`/api/automation/{secretKey}/mcp`) and management (`/api/management/{secretKey}/mcp`) MCP endpoints via an authorization server embedded in ByteChef, so OSS deployments get out-of-box remote MCP with OAuth2 — while API-key auth (Phase 1) stays the always-on baseline.

**Architecture:** ByteChef server-app embeds a Spring Authorization Server (pre-configured for MCP, notably Dynamic Client Registration) behind a `bytechef.oauth2.authorization-server.enabled` flag. The two MCP endpoints gain OAuth2 **resource-server** support (accept a Bearer JWT *in addition to* an API key) on the same filter chain built in Phase 1. An unauthenticated request now returns `401 + WWW-Authenticate` pointing at RFC 9728 protected-resource metadata; the client discovers the AS, performs DCR, runs authorization-code + PKCE (the AS delegates end-user login to ByteChef's existing Spring Security form-login + a consent screen), and presents the issued JWT. Both credential types resolve to the same user + authorities principal, keeping facade `@PreAuthorize` guards credential-agnostic. Spec: `docs/superpowers/specs/2026-07-05-mcp-server-api-key-oauth2-auth-design.md` (Phase 2 section).

**Tech Stack:** Java 25, Spring Boot 4.0.7, Spring Security 7, Spring Authorization Server (version TBD — Phase 0), `org.springaicommunity:mcp-authorization-server` + `mcp-server-security` OAuth configurer (versions TBD — Phase 0), Spring AI 2.0.0, MCP SDK 2.0.0, Liquibase, JUnit 5, Testcontainers.

## Status: DRAFT — gated on the Phase 0 spike

Unlike the Phase 1 plan (whose "Key library facts" were verified against `mcp-server-security` 0.1.13 source before planning), Phase 2 depends on two libraries that are **not yet on any ByteChef classpath and not yet version-pinned** (`mcp-authorization-server`, Spring Authorization Server). Per the honesty rule for plans, the task detail below deliberately stops at the interface/file/test level for the AS internals; **step-level code for Phase A/B is finalized only after Phase 0 confirms the library versions and their APIs.** Do not treat the later phases as fully specified until the spike closes the unknowns listed in "Open questions."

**Update (2026-07-06):** The gating compatibility question is answered — **Spring Boot 4.0.7 CAN host the embedded authorization server** (confirmed). Risk #1 is therefore downgraded from HIGH to resolved: the Boot-4.1.x-bump fallback is no longer expected to be needed. Phase 0 narrows from a go/no-go compatibility gate to a scoping spike — pin the exact `mcp-authorization-server` + Spring Authorization Server versions, resolve their configurer APIs, and capture the verified library facts — after which Phase A can start.

## Key library facts (verified 2026-07-06 — Phase 0)

Confirmed against the published `0.1.13` POM + jar (`javap`) and ByteChef's resolved dependency graph:

- **Artifacts (same 0.1.13 release train as Phase 1's `mcp-server-security`, published 2026-06-12):**
  - AS: `org.springaicommunity:mcp-authorization-server:0.1.13` → transitively pulls `org.springframework.security:spring-security-oauth2-authorization-server:7.1.0`, `spring-security-config:7.1.0`, `org.springaicommunity:mcp-security-common:0.1.13`.
  - Resource server (Phase B): **reuse the already-present** `org.springaicommunity:mcp-server-security:0.1.13` — no new dependency.
- **Version alignment — no conflict:** ByteChef on Boot 4.0.7 already resolves `spring-security-core` to **7.1.0** (Boot upgrades 7.0.6 → 7.1.0), the exact version Spring Authorization Server 7.1.0 requires. The AS slots into the existing Security 7.1.0 stack with no bump. Combined with the confirmation that Boot 4.0.7 hosts the AS, Risk #1 is closed.
- **AS configurer** — `org.springaicommunity.mcp.security.authorizationserver.config.McpAuthorizationServerConfigurer`, an `AbstractHttpConfigurer<…, HttpSecurity>` applied via `http.with(McpAuthorizationServerConfigurer.mcpAuthorizationServer(), …)` (identical pattern to Phase 1's configurers):
  - `mcpAuthorizationServer()` — static factory.
  - `.authorizationServer(Customizer<OAuth2AuthorizationServerConfigurer>)` — customize the underlying Spring Authorization Server.
  - `.dynamicClientRegistration(boolean)` — enable DCR (RFC 7591).
  - `.cimd(boolean)` — client-id metadata document support.
  - `.dynamicClientRegistrationValidator(Consumer<OAuth2ClientRegistrationAuthenticationContext>)` — custom DCR validation hook.
- **Token customizers:** `McpDefaultJwtCustomizer.DEFAULT_JWT_CUSTOMIZER` and `ResourceIdentifierAudienceTokenCustomizer` (sets the RFC 8707 resource identifier as the JWT audience, so the resource server can validate `aud`).
- **Resource-server API** (`mcp-server-security:0.1.13`, for Phase B): `org.springaicommunity.mcp.security.server.config.McpServerOAuth2Configurer` (OAuth2 configurer, companion to Phase 1's API-key path); `…server.oauth2.authentication.BearerResourceMetadataTokenAuthenticationEntryPoint` (returns `401 + WWW-Authenticate` with RFC 9728 protected-resource metadata — exactly Task B.3); `…server.oauth2.jwt.AudienceValidationJwtDecoder` / `JwtResourceValidator` (audience/issuer JWT validation); `…server.oauth2.metadata.ResourceIdentifier`.
- **NOT yet proven (honest gap):** I have not booted the AS in ByteChef's context, and **Open question 5 (login/consent delegation to the existing form-login without a second user store) is unverified.** Compatibility is confirmed (user + clean version graph + resolvable artifacts + known APIs), so the green boot + metadata test becomes **Phase A Task A.3's first deliverable** (a real test in the new module) rather than a throwaway spike; the login-delegation mechanism is confirmed in **Task A.4**.

## Scope check — DECIDED: three PRs (confirmed 2026-07-06)

This plan covers three separable subsystems, shipped as **three sequential change-sets / PRs**, each independently testable, all behind the enable flag:

- **Phase A — Embedded authorization server** (dependency + enable flag + AS config + Liquibase + login/consent delegation).
- **Phase B — Resource-server on the MCP endpoints** (JWT acceptance alongside API keys + token→tenant + scopes + discovery `WWW-Authenticate`).
- **Phase C — Admin UI** for DCR-created clients (list + delete).

Phase B depends on Phase A (needs an issuer to validate against). Phase C depends on Phase A (needs the client store). Get a decision on the scope split before starting (see Open questions).

## Global Constraints

- Copy exact values from the spec; every task's requirements implicitly include this section.
- **Edition: EE (changed 2026-07-06; was CE).** MCP strong auth (API keys + OAuth2) is EE-only, gated by `@ConditionalOnEEVersion`. Phase 1 + the embedded AS were **relocated CE→EE** (commits `cd28b26335e`, `aec5d6d96e2`): shared classes → EE `platform-security-web-impl`, per-server configurers → EE `automation-security-web-impl`/`platform-security-web-impl`, AS module → EE `platform-oauth2-authorization-server` (all `com.bytechef.ee.*`). In CE, the MCP servers accept the URL path secret only. External-IdP support is now IN scope (see the Phase B scope-addition note). Federation with a customer IdP is the resource server accepting external issuers.
- **API keys remain accepted.** Phase 2 adds a *second* credential type on the same endpoints; it does not replace Phase 1. Coexistence is a hard requirement with its own regression test.
- **Enable flag:** `bytechef.oauth2.authorization-server.enabled` MUST be a field in the central `ApplicationProperties` (`server/libs/config/app-config/.../ApplicationProperties.java`, nested under the existing `Oauth2` type) — strict binding means an unregistered `bytechef.*` property fails boot. Default **false**: AS endpoints do not appear until enabled.
- **Embedded MCP server is out of scope** (both phases) — do not touch `server/ee/libs/embedded/...`.
- **Phase 1 behavior is preserved.** The bare `401` (no body) stays the response *only while OAuth is disabled*. When the AS is enabled, the MCP endpoints' unauthenticated response becomes `401 + WWW-Authenticate` (RFC 9728) — that header must NOT appear unless the AS is enabled and serving the metadata it points at (this was the explicit Phase 1 deferral).
- **Tenancy invariant:** the resource-server filter establishes `TenantContext` from the token's tenant claim before any repository access — the same route-by-credential invariant as Phase 1's `TenantAwareApiKeyAuthenticationFilter`.
- Java style (from CLAUDE.md): one blank line before control statements; blank line between a variable modification and a statement using it; no blank line before class-closing `}`; no `TODO:` comments; test method names camelCase without underscores; descriptive variable names.
- New CE files under `server/libs/` get the Apache 2.0 license header + `@author Ivica Cardic`.
- Integration test classes end in `IntTest` and run under the `testIntegration` task (the `java-common-conventions` plugin excludes `*IntTest*` from `test`).
- Run `./gradlew spotlessApply` before every commit; commit only files the task touches; no ticket number in commit messages.

## Open questions (resolve before/at Phase 0)

1. **Library selection + versions. — RESOLVED (Phase 0, 2026-07-06):** `org.springaicommunity:mcp-authorization-server:0.1.13` (pulls Spring Authorization Server 7.1.0), which aligns cleanly with ByteChef's already-resolved Spring Security 7.1.0 on Boot 4.0.7. Configurer APIs verified — see "Key library facts". No longer a risk.
2. **Scope split / sequencing. — DECIDED (2026-07-06):** three sequential PRs (Phase A → B → C).
3. **Module placement.** New module `platform-oauth2-authorization-server` (`{api,service}`) vs. folding AS config into an existing platform module. The AS's Liquibase changelogs (`oauth2_registered_client`, `oauth2_authorization`, `oauth2_authorization_consent`) need a home; a dedicated module mirrors the codebase's one-module-per-concern convention.
4. **Token model.** Access-token lifetime, whether refresh tokens are issued, and the exact custom claims (subject = ByteChef user id; a tenant claim — name it, e.g. `tenant_id`). Scopes are fixed by the spec: `mcp:automation`, `mcp:management`.
5. **Consent + login reuse.** Confirm the AS can delegate `authenticate` to the existing form-login `SecurityConfiguration.apiFilterChain` session without a second user store, and where the consent screen renders (server-rendered page vs. SPA route). The spec asserts "no second user store" — verify the mechanism.
6. **Admin UI surface.** Spec says "minimal: list + delete." Confirm that's the whole Phase C scope (no create/edit — clients arrive via DCR).
7. **Environment selection.** Stays with the `X-ENVIRONMENT` header (not baked into tokens) — confirm the resource-server path reads it the same way Phase 1's converter does.

---

## Phase 0 — Feasibility spike (the gate) — DONE (except the deferred boot proof)

Mirrors Phase 1's Task 1. Outcome: the architecture is de-risked — versions pinned, artifacts resolve, Security version aligns at 7.1.0, APIs verified (see "Key library facts"). The one remaining piece, an actual green AS-metadata boot, is intentionally moved into Phase A Task A.3 (built for real in the new module, not thrown away).

### Task 0.1: Pin libraries and verify Boot 4.0.7 compatibility

- [x] **Step 1 — DONE:** `mcp-authorization-server:0.1.13` selected (latest; same release train as `mcp-server-security` we already use). Transitive graph resolved: pulls Spring Authorization Server 7.1.0; ByteChef already resolves Spring Security to 7.1.0, so no version conflict. Verified facts recorded above. (The `gradle/libs.versions.toml` entry is added in Phase A Task A.2/A.3 where it is first referenced, to avoid a dangling unused catalog entry.)
- [x] **Step 2/3 — compatibility CLOSED without a throwaway boot:** Boot 4.0.7 hosting the AS is confirmed (user) and corroborated by the clean 7.1.0 version graph and resolvable artifacts + verified configurer APIs. The green `/.well-known/oauth-authorization-server` boot test is promoted to **Phase A Task A.3** as a real test in the `platform-oauth2-authorization-server` module.
- [ ] **Deferred to Task A.4:** verify the AS delegates end-user login to the existing form-login without a second user store (Open question 5 — still open).

**Exit status:** Open question 1 resolved; Open question 5 remains, to be closed in Task A.4. Gate is open for Phase A.

---

## Phase A — Embedded authorization server

*(Task detail below is at interface granularity; expand to bite-sized steps with real code after Phase 0 verifies the library APIs.)*

### Task A.1: Register the enable flag in `ApplicationProperties` — DONE (commit c99bcc79337)
- `AuthorizationServer` nested type added under `Oauth2` (`enabled`, default false); `ApplicationPropertiesTest` binding test (2/2 green). The AS config gates on the raw property via `@ConditionalOnProperty`; this field is what keeps strict binding (`ignoreUnknownFields=false`) from failing boot when the property is set.

### Task A.2: New module + Liquibase for the AS tables — DONE
- Module `server/libs/platform/platform-oauth2-authorization-server/` created + registered in `settings.gradle.kts`. Liquibase changelog creates `oauth2_registered_client`, `oauth2_authorization`, `oauth2_authorization_consent` (exact Spring Authorization Server 7.1.0 schema, Postgres types: `blob`→`TEXT`, `timestamp`→`TIMESTAMP WITH TIME ZONE`), registered in `master.xml` (`includeAll`, `contextFilter="mono or configuration or multitenant"`). `*IntTest` asserts the three tables exist.

### Task A.3: Authorization-server configuration, gated on the flag — DONE (isolated milestone)
- `Oauth2AuthorizationServerConfiguration` (`@ConditionalOnProperty(...enabled=true)`) wires `McpAuthorizationServerConfigurer.mcpAuthorizationServer().dynamicClientRegistration(true)` + SAS JDBC beans (`JdbcRegisteredClientRepository` / `JdbcOAuth2AuthorizationService` / `JdbcOAuth2AuthorizationConsentService`) + `JWKSource` (RSA) + `JwtDecoder` + `AuthorizationServerSettings`.
- **Verified green (4/4 `testIntegration`):** flag ON → `GET /.well-known/oauth-authorization-server` = 200 with real issuer/`registration_endpoint`/`jwks_uri`, DCR POST persists a `oauth2_registered_client` row; flag OFF → AS endpoints absent. **This confirms Boot 4.0.7 hosts Spring Authorization Server 7.1.0 — the Phase 0 deferred proof.**
- **⚠️ KNOWN INTEGRATION BLOCKER (found during A.3, must be fixed in A.4 before wiring into server-app):** the AS `SecurityFilterChain` is `@Order(HIGHEST_PRECEDENCE)` with `anyRequest().permitAll()` and **no `http.securityMatcher(...)`**. The `mcp-authorization-server` README's sample is written for a *dedicated* AS application (unscoped chain + `anyRequest().authenticated()` + form login), where there are no other chains to shadow. ByteChef **co-hosts** the AS with `SecurityConfiguration`'s `/api/**` and MCP chains, so an unscoped highest-precedence permit-all chain would shadow them all → auth bypass. Not a live vuln today (the module is standalone; server-app does not depend on it yet), but A.4 MUST scope this chain to the AS endpoints (metadata, `/oauth2/**`, `/oauth2/register` DCR, jwks, CIMD) via `securityMatcher`, and must include a multi-chain test proving a non-AS request falls through to the app's chain rather than being permit-all'd.

### Task A.4: Delegate end-user auth to existing login + SCOPE THE CHAIN — DONE
- **Chain scoping (commit effabaa0b6a):** the AS `SecurityFilterChain` is scoped via `http.securityMatcher(...)` using the library's own idiom — `authorizationServer.getEndpointsMatcher()` obtained *inside* the `mcp.authorizationServer(...)` customizer (so it includes the DCR endpoint) OR-ed with `/.well-known/openid-configuration`, `anyRequest().authenticated()`. A chain-scoping `*IntTest` proves a non-AS request (`/protected`) is no longer shadowed (401, not permit-all 200) while metadata still serves. (Spring Security 7 hard-fails context startup on an unscoped `anyRequest` chain co-hosted with another — so this was mandatory, not cosmetic.)
- **Login delegation (commit 7d00d2a8aa4):** unauthenticated browser requests to the authorize endpoint are redirected to `/` via `LoginUrlAuthenticationEntryPoint`. An end-to-end `*IntTest` logs a user in against a *separate* co-hosted form-login chain (standing in for `SecurityConfiguration`), then the authorize endpoint reuses that session to issue a code (PKCE S256), exchanged for a JWT with `sub` = the logged-in user. **Open question 5 (no second user store) is confirmed.**
- **Verified:** 8/8 `testIntegration` green (independent `--rerun-tasks`).
- **Follow-ups:**
  1. **Wire the module into `server-app` — DONE (commit ec09d312ffe).** Added as a `server-app` dependency; component-scanned, dormant while the flag is off. Full-app boot tests (5/5): beans absent when disabled, and the whole context loads with the flag enabled — proving the scoped AS chain co-hosts with `SecurityConfiguration`'s chains (an unscoped chain fails Spring Security context startup).
  2. **Consent UI — DONE (commits 52ae5926e80 server, 2ebdcd2697c client).** Server: `authorizationEndpoint().consentPage("/oauth2/consent")` so a consent-requiring client redirects to ByteChef's page (IntTest asserts the 302 carries `client_id`/`scope`; RED-verified against SAS's default form). Client: `/oauth2/consent` public SPA route rendering the client + requested scopes as checkboxes, submitting Allow/Deny as a native form POST to `/oauth2/authorize`. `npm run check` green (3695 tests).
  3. **Tenant claim** on issued JWTs — deferred to Phase B (identity/tenancy), where the resource server also validates it.

---

## Phase A / PR 1 status: COMPLETE

The embedded authorization server is built, gated, wired into `server-app`, and proven end-to-end: it boots on Boot 4.0.7, serves metadata + DCR, its filter chain is safely scoped so it co-hosts with the app's other security chains, the authorization-code + PKCE flow works with login delegated to the existing form-login session (no second user store), and consent redirects to a ByteChef SPA page. Commits: `c99bcc79337` (A.1 flag), `4663148ddd7` (A.2/A.3 module), `effabaa0b6a` (A.4 scoping), `7d00d2a8aa4` (A.4 flow), `ec09d312ffe` (server-app wiring), `52ae5926e80` (consent server), `2ebdcd2697c` (consent client). Only the tenant claim remains, and it belongs to Phase B. **Phase B (resource server on the MCP endpoints)** and **Phase C (admin UI)** follow.

---

## Phase B — Resource-server on the MCP endpoints

**Scope addition (2026-07-06): support an EXTERNAL OAuth2 authorization server, not only ByteChef's embedded one.** The spec had put customer-IdP federation out of scope; that is reversed. The MCP resource server must validate JWTs from an external IdP (Okta / Auth0 / Keycloak / Entra) as an alternative or addition to the embedded AS. `McpServerOAuth2Configurer.authorizationServer(issuerUri)` takes an issuer URI, so validation is largely configuration; the hard parts are:
- **Issuer config:** a new EE property (e.g. `bytechef.oauth2.resource-server.issuer-uri`); decide embedded-vs-external mutually exclusive, or multi-issuer.
- **External identity mapping — DECIDED (2026-07-06): configured claim → tenant/authorities mapping.** For an external IdP, the resource server does NOT require a 1:1 ByteChef user row. A configurable mapping translates external token claims (e.g. a groups/roles claim, an org/tenant claim) into a ByteChef tenant + granted authorities, and the principal is built from the token (subject + mapped authorities) rather than a DB user lookup. (The embedded AS path is unaffected: its `sub` is already a ByteChef user with a minted tenant claim.) The mapping configuration shape (which claims → tenant, which claim → authorities) is an EE property/config to design in the implementation.
- **Tenant + scopes from external tokens:** derive tenant from the mapped user (EE `TenantService`) or a claim; the external client must be granted `mcp:automation`/`mcp:management` (or a claim mapping).
- **Audience:** RFC 8707 resource-indicator/audience validation must accept the MCP endpoint's resource id from an external issuer.
- **Tests:** an externally-issued JWT (against a test issuer/static JWKS) authenticates and maps to a ByteChef principal; wrong issuer/audience/expired → 401; API key + embedded JWT + external JWT coexist.

*(This threads through Tasks B.1–B.3 — each must work for both the embedded and external issuer.)*

### Task B.1: Add OAuth2 resource-server to the MCP filter chains
- **Files:** extend the Phase 1 configurers (`AutomationMcpServerSecurityConfigurer`, `ManagementMcpServerSecurityConfigurer` / the shared `McpApiKeyHttpConfigurer`) to also accept a Bearer **JWT** via `mcp-server-security`'s OAuth configurer, validating signature, issuer, and audience. API-key auth stays first-class on the same chain (try one, then the other).
- **Interface consumed:** the Phase 1 principal shape (user + authorities) — the JWT path must map to the *same* `McpApiKeyEntity`-equivalent principal so facade `@PreAuthorize` stays credential-agnostic.
- **Test:** `*IntTest` — a valid JWT authenticates and `tools/list` succeeds; **coexistence:** a valid API key still authenticates on the same endpoint after OAuth is enabled (the explicit spec regression).

### Task B.2: Token → tenant + scopes per endpoint
- **Files:** a resource-server filter (analogue of `TenantAwareApiKeyAuthenticationFilter`) that establishes `TenantContext` from the token's tenant claim before repository access; enforce scope `mcp:automation` on the automation endpoint and `mcp:management` on the management endpoint; keep environment selection on the `X-ENVIRONMENT` header (Open question 7).
- **Test:** `*IntTest` — wrong/expired/wrong-audience/wrong-issuer JWT → 401; automation-scope token rejected on the management endpoint and vice-versa.

### Task B.3: Discovery `401 + WWW-Authenticate` (replaces Phase 1's bare 401 *when AS enabled*)
- **Files:** when the AS is enabled, unauthenticated MCP requests return `401` with `WWW-Authenticate` carrying the RFC 9728 protected-resource-metadata URL; serve that metadata. When the AS is disabled, Phase 1's bare 401 (no header) is unchanged.
- **Test:** `*IntTest` — AS enabled: unauthenticated → 401 + `WWW-Authenticate` → metadata resolves → points at the AS; AS disabled: unauthenticated → bare 401, no `WWW-Authenticate` (guards the Phase 1 contract).

---

## Phase B — resolved execution design (expanded 2026-07-06)

Decisions confirmed this session (issuer topology = **multi-issuer**; implement all of Phase B) and library behavior verified against `mcp-server-security:0.1.13` bytecode (`javap`). Records the concrete mechanics so implementation is not re-derived.

### Verified library behavior (`McpServerOAuth2Configurer`, 0.1.13)
- `AbstractHttpConfigurer<…, HttpSecurity>`; static factory `mcpServerOAuth2()`; applied via `http.with(configurer, …)`.
- `.init(http)` **requires** `authorizationServer` (issuer URI, non-null) and a `resourceIdentifier` (from `.resourcePath(path)` / `.resourceName(name)`), then calls `http.oauth2ResourceServer(rs -> rs.jwt(decoder).authenticationEntryPoint(BearerResourceMetadataTokenAuthenticationEntryPoint).protectedResourceMetadata(…))` and `SessionBindingConfigurer.init(http)`.
- Decoder: if `.jwtDecoder(custom)` is set it is used **as-is** (we then own audience validation); otherwise it builds `NimbusJwtDecoder.withIssuerLocation(issuer)` and, when `.validateAudienceClaim(true)`, wraps it in `AudienceValidationJwtDecoder`.
- `.oauth2ResourceServer(Customizer<OAuth2ResourceServerConfigurer>)` lets us further customize the same resource server (e.g. set a custom `BearerTokenResolver`, `jwtAuthenticationConverter`). `jwt()` and `authenticationManagerResolver()` are mutually exclusive, so multi-issuer is done at the **decoder** level, not via `JwtIssuerAuthenticationManagerResolver`.
- `BearerResourceMetadataTokenAuthenticationEntryPoint(ResourceIdentifier)` → 401 + `WWW-Authenticate` (RFC 9728). `ResourceIdentifier(String path)`.

### Coexistence with the API-key filter (the hard part) — SOLVED
The configurer adds a `BearerTokenAuthenticationFilter` on the **same** MCP chain as `TenantAwareApiKeyAuthenticationFilter`. By default that JWT filter would try to JWT-decode an API-key Bearer and 401 it. Fix: a custom `BearerTokenResolver` that returns `null` when the token parses as a `TenantKey` (API key) and returns it only for JWTs — mirroring the API-key filter's existing fall-through (commit `60098349d61`), so the two filters partition the Bearer space with zero overlap. Regression test: an API key still authenticates after the resource server is enabled.

### Multi-issuer decoder
`bytechef.oauth2.resource-server.issuer-uris` = ordered list of trusted issuers (embedded AS issuer = app public URL, plus external IdPs). A composite `JwtDecoder` reads the token's `iss` (unverified header/claim parse), selects the matching per-issuer `NimbusJwtDecoder.withIssuerLocation(iss)` (lazily built, cached), each wrapped so it validates issuer + audience (RFC 8707 resource id) + expiry/signature. Unknown `iss` → `JwtException` → 401. `.authorizationServer(primaryIssuer)` (first configured) feeds discovery metadata; `.protectedResourceMetadataCustomizer(…)` lists all trusted issuers as `authorization_servers`.

### Token → principal + tenant (per-issuer identity mapping)
- **Embedded issuer:** `sub` = ByteChef user login/id; build authorities from the ByteChef user (same as the API-key `McpApiKeyEntityRepository`), inside the tenant from the minted `tenant_id` claim.
- **External issuer:** configured claim → tenant + authorities mapping (no ByteChef user row). EE config: per-issuer `tenant-claim` (which claim yields the tenant id) + `authorities-claim` (groups/roles → authorities) + optional static authorities.
- A tenant-aware JWT filter placed **after** the bearer filter reads the now-*validated* tenant claim and wraps the remainder of the chain in `TenantContext.runWithTenantId(…)` (JWT analogue of `TenantAwareApiKeyAuthenticationFilter`); for the embedded path the ByteChef-user principal is resolved inside that context so facade `@PreAuthorize` stays credential-agnostic.
- **Scopes:** resource server maps `scope`/`scp` → `SCOPE_*` authorities; enforce `SCOPE_mcp:automation` on the automation chain and `SCOPE_mcp:management` on the management chain (`authorizeHttpRequests` on the MCP path, or in the configurer).

### Mint the tenant claim on the embedded AS (Task B.2 dependency)
The token endpoint is back-channel (no session), so capture the tenant at **authorize** time (front-channel, session present via delegated login), carry it onto the `OAuth2Authorization`, and emit it in the JWT via an `OAuth2TokenCustomizer<JwtEncodingContext>` in `Oauth2AuthorizationServerConfiguration`. Tenant source = `TenantContext.getCurrentTenantId()` / `CURRENT_TENANT_ID` session attr (EE `MultiTenantInternalFilter`).

### Task checklist
- [x] **B.0** `bytechef.oauth2.resource-server.issuers` (list of `{uri, tenantClaim, authoritiesClaim, authorities}`) in `ApplicationProperties` (nested under `Oauth2`, for strict binding) + binding test. Functional reader is an EE-local `McpResourceServerProperties` (`@ConfigurationProperties`), mirroring the Task A.1 precedent. Default empty → resource server dormant. **DONE.**
- [x] **B.1** `MultiIssuerJwtDecoder` (per-`iss` decoder selection) + `McpBearerTokenResolver` (MCP-path + JWT-only, so API keys fall through) + `McpJwtDecoderFactory` (prod fetches each issuer's JWKS via `/.well-known`) + `McpOAuth2ResourceServerSecurityConfigurerContributor` applying `McpServerOAuth2Configurer` on the shared `/api` chain, gated on issuers configured. IntTests: minted JWT authenticates + `tools/list`; API key still works (coexistence); untrusted issuer → 401. **DONE.**
- [x] **B.2** `TenantAwareJwtAuthenticationFilter` (establishes `TenantContext` from the validated tenant claim, enforces `mcp:automation`/`mcp:management` per endpoint, installs claim-derived identity) + `McpJwtIdentityMapper` (uniform embedded/external claim→tenant/authorities) + `OAuth2TokenCustomizer` on the embedded AS minting `tenant_id` + `authorities`. Unit tests (mapper, filter) + IntTests: wrong-scope/expired → 401; external-issuer federation succeeds; AS flow asserts minted `tenant_id`. **DONE.**
- [x] **B.3** `McpDiscoveryAuthenticationFilter` (runs ahead of the API-key filter; unauthenticated MCP request → 401 + `WWW-Authenticate` with the RFC 9728 pointer when issuers configured; no-op otherwise, so Phase 1 bare 401 is preserved) + `McpProtectedResourceMetadataSecurityConfiguration` (dedicated EE well-known chain serving the metadata in production, mirroring the AS's own metadata chain). IntTests: discovery challenge present when active; metadata resolves to the configured issuer; dedicated chain serves it standalone. **DONE.**

### Deferred (honest scope)
- **Strict per-endpoint audience (RFC 8707 resource-indicator) validation** is not enforced: `validateAudienceClaim(false)`, and the multi-issuer decoder does not check `aud`. Per-endpoint isolation is provided by **scope** (`mcp:automation` vs `mcp:management`), which is the primary guard. A single shared resource server across two MCP endpoints cannot do per-endpoint audience validation without either the dedicated-chain-per-endpoint architecture or a request-aware decoder; deferred as a hardening follow-up.
- **The resource server rides the shared `/api` chain** (scoped by `McpBearerTokenResolver`) rather than a dedicated MCP chain, to avoid refactoring Phase 1's shared-chain API-key wiring. The discovery metadata is served by a separate dedicated EE chain because the `/api` chain does not match the well-known path.

---

## Phase C — Admin UI for DCR-created clients — COMPLETE (2026-07-07)

### Task C.1: Server endpoint (list + delete registered clients) — DONE
- **Built:** `RegisteredClientFacade` + `RegisteredClientGraphQlController` (`registeredClients` query + `deleteRegisteredClient` mutation) in the EE `platform-oauth2-authorization-server` module, `hasAuthority(ROLE_ADMIN)` guarded, EE + coordinator gated. `JdbcRegisteredClientRepository` exposes no list/delete, so the facade queries `oauth2_registered_client` directly and, on delete, first removes the client's `oauth2_authorization` rows (revoking tokens) then the client.
- **Test:** facade `*IntTest` (Testcontainers) — list returns registered clients; delete removes the client and revokes its authorizations. (2 tests; module suite 11 green.)

### Task C.2: Client UI (list + delete) — DONE
- **Built:** admin-only, EE-gated settings page `client/src/ee/pages/settings/platform/registered-clients/RegisteredClients.tsx` (list + delete-with-confirmation), route `/settings/platform/registered-clients` + nav "OAuth2 Clients". A dedicated page (not the create/edit `ApiKeysContent`) since registered clients are list+delete only.
- **Codegen repair (prerequisite):** client GraphQL codegen was pre-existing broken (43 doc-validation errors) — fixed by adding three missing server schema modules (automation-ai-mcp, ai-mcp-server-configuration, embedded-ai-mcp) to `codegen.ts`, removing the dead `integrationById` op, and dropping the manual `TypedDocumentString` `add` that `typescript-react-query` already emits.
- **Test:** component test (list + delete-dialog flow). `npm run check` green (3697 tests).

---

## Testing (from the spec's Phase 2 section)

Integration tests (`*IntTest`, Testcontainers Postgres, real streamable HTTP, per module) for:
- **Discovery chain:** unauthenticated → `401 + WWW-Authenticate` → protected-resource metadata → AS metadata.
- **DCR + full authorization-code + PKCE** flow with a test client.
- **Token validation** on the MCP endpoints: expired / wrong audience / wrong issuer → 401.
- **Coexistence:** an API key still authenticates after OAuth is enabled.
- **Flag off:** AS endpoints absent; MCP endpoints keep Phase 1's bare-401 behavior (no `WWW-Authenticate`).

## Risks

- **[RESOLVED] Spring Authorization Server ↔ Boot 4.0.7 compatibility** — confirmed 2026-07-06 that Boot 4.0.7 can host the embedded authorization server, so the Boot-4.1.x-bump fallback is not expected to be needed. Phase 0 still pins the exact library versions and captures verified configurer APIs before Phase A.
- **[MED] Login/consent delegation without a second user store** — the spec asserts it; verify the mechanism in Phase 0/Task A.4 before committing to the AS approach.
- **[MED] Two credential types on one chain** — filter ordering between the API-key filter and the resource-server filter must not let one mask the other; the coexistence regression test is the guard.
- **[MED] Tenant claim trust** — the token's tenant claim drives `TenantContext`; it must be minted by the AS (trusted) and validated (issuer/audience) before use, exactly as the API-key path parses tenant only from a validated secret.
- **[LOW] `WWW-Authenticate` leaking while AS disabled** — would break Phase 1's deliberate deferral; covered by the flag-off test.

## Self-review notes

- Spec coverage: topology (Phase A), client journey/discovery (B.3 + A.3/A.4), identity/tenancy/scopes (B.2), persistence/administration (A.2 + C), edition placement CE (global constraints), enable flag + strict binding (A.1) — all mapped.
- Deliberate omission: this plan does **not** provide bite-sized step-level code for the AS internals, because the library APIs are unverified (see "Status"). That is honest scoping, not a placeholder gap — Phase 0 is the task that unblocks writing that detail.
