# Embedded MCP OAuth2 — SP-A (Resource Server) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Status: COMPLETE (2026-07-07).** All six tasks done and committed; 12 unit + 3 integration tests green. Implementation notes: the MCP server secret is a `TenantKey`, so tenant resolves from the path with no DB query (Task 3). The platform `ApiKeyAuthenticationFilter` falls through on a null conversion and wraps the downstream in `TenantContext.runWithTenantId(token.getTenantId(), …)`, so coexistence and tenant propagation come for free (Task 5). The Task 6 IntTest is DB-free (JDBC auto-configs excluded) and must use `authorizeHttpRequests(... .authenticated())` + a 401 `HttpStatusEntryPoint` to actually enforce auth — a `permitAll` chain lets unauthenticated MCP requests through (200), making a "valid token" assertion a false positive. Embedded AS issuer wired as `null` for now (external IdPs only); SP-B supplies it.

**Goal:** Make the embedded MCP endpoint `/api/embedded/{secretKey}/mcp` accept an OAuth2 Bearer JWT (from ByteChef's embedded AS or a per-tenant external IdP) alongside the existing ByteChef-signed signing-key JWT, mapping the token to a `ConnectedUser`.

**Architecture:** The embedded chain is built from an `AuthenticationConverter` + `AuthenticationProvider` pair on `EmbeddedMcpServerSecurityConfigurer` (extends `AbstractApiKeyHttpConfigurer`). SP-A adds a **second** converter/provider pair for OAuth2, ordered **before** the signing-key one. Because both credentials are JWTs, the OAuth2 converter discriminates by the token's `iss`: it establishes the tenant from the path secret (`TenantKey.parse` — the MCP server secret *is* a `TenantKey`, so no DB query), resolves that tenant's trusted issuers, and only consumes the token if its `iss` is trusted; otherwise it returns `null` so the request falls through to the signing-key converter. Validation reuses `MultiIssuerJwtDecoder` from `platform-security-web-impl`. The provider resolves-or-creates a `ConnectedUser`, exactly like the existing signing-key provider.

**Tech Stack:** Java 25, Spring Boot 4.0.7, Spring Security 7, `spring-security-oauth2-jose` (Nimbus), `MultiIssuerJwtDecoder` (existing EE), `ConnectedUserService`, `IdentityProviderService`, JUnit 5, Testcontainers.

## Global Constraints

- **Edition: EE.** All code under `server/ee/libs/embedded/embedded-ai/embedded-ai-mcp-server`, package `com.bytechef.ee.embedded.ai.mcp.server.*`. EE license header + `@version ee` on every file (run `./gradlew spotlessApply`; the header is applied from the `@version ee` content).
- **Coexistence is a hard requirement:** the existing signing-key embedded JWT (`EmbeddedMcpServerApiKeyAuthenticationConverter`) must keep working unchanged.
- **Tenant comes from the path secret**, not the token: `TenantKey.parse(pathSecret).getTenantId()`, where `pathSecret` is the `{secretKey}` path variable of `/api/embedded/{secretKey}/mcp`. Environment stays on the `X-Environment` header.
- **Per-tenant issuer isolation:** a token whose `iss` is trusted for tenant A must never authenticate on tenant B's endpoint. Trusted issuers are resolved strictly within the path-secret tenant.
- Java style (CLAUDE.md): one blank line before control statements; blank line between a variable modification and its use; no blank line before class-closing `}`; no `TODO:` comments; test method names camelCase without underscores; descriptive variable names.
- Integration tests end in `IntTest` and run under `testIntegration`. Run `./gradlew spotlessApply` before every commit; commit only files the task touches.
- Reuse templates (read them first): `EmbeddedMcpServerApiKeyAuthenticationConverter`, `EmbeddedMcpServerApiKeyAuthenticationProvider`, `EmbeddedMcpServerApiKeyAuthenticationToken`, `EmbeddedMcpServerSecurityConfigurer` (all under `.../embedded/ai/mcp/server/security/web/`); and `MultiIssuerJwtDecoder`, `McpJwtDecoderFactory`, `IssuerLocationMcpJwtDecoderFactory`, `McpResourceServerProperties` (EE `platform-security-web-impl`).

---

### Task 1: Per-tenant trusted-issuer resolver

**Files:**
- Create: `.../security/web/authentication/EmbeddedMcpTrustedIssuerResolver.java`
- Test: `.../security/web/authentication/EmbeddedMcpTrustedIssuerResolverTest.java`

**Interfaces:**
- Consumes: `IdentityProviderService.getIdentityProviders()` (tenant-scoped; returns `List<IdentityProvider>`, each with `isEnabled()` + `getIssuerUri()`).
- Produces: `Set<String> resolveTrustedIssuerUris()` — the current tenant's enabled IdP issuer URIs, plus the embedded AS issuer (`bytechef.public-url`, passed in via constructor).

- [ ] **Step 1: Write the failing test** — `EmbeddedMcpTrustedIssuerResolverTest`: given a mock `IdentityProviderService` returning one enabled IdP (`issuerUri="https://idp.customer.test"`) and one disabled IdP (`issuerUri="https://disabled.test"`), and embedded AS issuer `"https://app.bytechef.test"`, assert `resolveTrustedIssuerUris()` == `{"https://idp.customer.test", "https://app.bytechef.test"}` (disabled excluded; blank/null issuer excluded).
- [ ] **Step 2:** Run it, verify it fails to compile (class missing).
- [ ] **Step 3: Implement** — constructor `(IdentityProviderService, String embeddedAuthorizationServerIssuerUri)`; `resolveTrustedIssuerUris()` streams `getIdentityProviders()`, filter `IdentityProvider::isEnabled` + non-blank `getIssuerUri()`, map `getIssuerUri()`, collect to a `HashSet`, add the embedded AS issuer if non-blank, return unmodifiable.
- [ ] **Step 4:** Run test, verify PASS.
- [ ] **Step 5: Commit** — `git add` both files; `git commit -m "Resolve per-tenant trusted MCP issuers for the embedded endpoint"`.

---

### Task 2: OAuth2 authentication token type

**Files:**
- Create: `.../security/web/authentication/EmbeddedMcpServerOAuth2AuthenticationToken.java`
- Test: covered via Task 3/4 (a data holder; no standalone test).

**Interfaces:**
- Produces: two states, mirroring `EmbeddedMcpServerApiKeyAuthenticationToken`:
  - unauthenticated: `EmbeddedMcpServerOAuth2AuthenticationToken(int environmentId, String externalUserId, String tenantId)` — `isAuthenticated()==false`, principal null.
  - authenticated: `EmbeddedMcpServerOAuth2AuthenticationToken(UserDetails principal)` — `isAuthenticated()==true`.
  - getters: `getEnvironmentId()`, `getExternalUserId()`, `getTenantId()`.

- [ ] **Step 1: Implement** — copy the structure of `EmbeddedMcpServerApiKeyAuthenticationToken` verbatim (same fields/constructors/getters), renaming the class. (Read that file; it is the exact template.)
- [ ] **Step 2:** Compile the module (`./gradlew :server:ee:libs:embedded:embedded-ai:embedded-ai-mcp-server:compileJava`), verify SUCCESS.
- [ ] **Step 3: Commit** — `git commit -m "Add embedded MCP OAuth2 authentication token type"`.

---

### Task 3: OAuth2 authentication converter (discriminate + validate)

**Files:**
- Create: `.../security/web/configurer/EmbeddedMcpServerOAuth2AuthenticationConverter.java`
- Test: `.../security/web/configurer/EmbeddedMcpServerOAuth2AuthenticationConverterTest.java`

**Interfaces:**
- Consumes: `EmbeddedMcpTrustedIssuerResolver` (Task 1); `MultiIssuerJwtDecoder` (existing) built from a `McpJwtDecoderFactory` restricted to the resolved per-tenant issuers; `EmbeddedMcpServerOAuth2AuthenticationToken` (Task 2).
- Produces: `Authentication convert(HttpServletRequest)` — returns an **unauthenticated** `EmbeddedMcpServerOAuth2AuthenticationToken` when the Bearer token is a JWT whose (unverified) `iss` is trusted for the path-secret tenant **and** validates; returns `null` otherwise (so the signing-key converter runs).

**Behavior (encode as tests):**
- No `Authorization` header → returns `null`.
- Bearer token that is not a JWT (no `.`) → returns `null`.
- Bearer JWT whose `iss` is not in the tenant's trusted set → returns `null` (fall-through; NOT an exception).
- Bearer JWT with a trusted `iss` but bad signature/expired → throws `BadCredentialsException` (it claimed to be ours).
- Bearer JWT with a trusted `iss` that validates → returns unauthenticated token with `externalUserId=sub`, `tenantId` from the path secret, `environmentId` from `X-Environment`.

- [ ] **Step 1: Write the failing test** — `EmbeddedMcpServerOAuth2AuthenticationConverterTest` using `MockHttpServletRequest` (servletPath `/api/embedded/{tenantKey}/mcp`, where `{tenantKey}=TenantKey.of("public")`), a static-key `MultiIssuerJwtDecoder`, and a stub trusted-issuer resolver. Cover the five behaviors above (`sub`, tenant from path, null-fall-through for untrusted iss, exception for bad-signature-trusted-iss, null for non-JWT).
- [ ] **Step 2:** Run it, verify it fails (class missing).
- [ ] **Step 3: Implement** —
  - `getAuthToken(request)`: read `Authorization`, strip `Bearer `; if absent → `null`.
  - if token has no `.` → `null`.
  - extract path secret: parse the servlet path with the regex `^/api/embedded/(.+)/mcp` → group(1); `tenantId = TenantKey.parse(pathSecret).getTenantId()`.
  - `TenantContext.callWithTenantId(tenantId, () -> issuerResolver.resolveTrustedIssuerUris())` → trusted set.
  - parse unverified `iss` via `com.nimbusds.jwt.JWTParser.parse(token).getJWTClaimsSet().getIssuer()`; if `iss` not in trusted set → return `null`.
  - `Jwt jwt = multiIssuerJwtDecoder.decode(token)` (decoder built with a factory restricted to the trusted set; on `JwtException` → throw `new BadCredentialsException("Invalid OAuth2 token", e)`).
  - return `new EmbeddedMcpServerOAuth2AuthenticationToken(environment.ordinal(), jwt.getSubject(), tenantId)`.
- [ ] **Step 4:** Run test, verify PASS.
- [ ] **Step 5: Commit** — `git commit -m "Add embedded MCP OAuth2 authentication converter"`.

---

### Task 4: OAuth2 authentication provider (ConnectedUser)

**Files:**
- Create: `.../security/web/authentication/EmbeddedMcpServerOAuth2AuthenticationProvider.java`
- Test: `.../security/web/authentication/EmbeddedMcpServerOAuth2AuthenticationProviderTest.java`

**Interfaces:**
- Consumes: `ConnectedUserService`; `EmbeddedMcpServerOAuth2AuthenticationToken` (Task 2).
- Produces: `AuthenticationProvider` — `authenticate()` fetch-or-creates the `ConnectedUser` and returns an authenticated `EmbeddedMcpServerOAuth2AuthenticationToken(User)`; `supports(EmbeddedMcpServerOAuth2AuthenticationToken.class)`.

- [ ] **Step 1: Write the failing test** — mirror `EmbeddedMcpServerApiKeyAuthenticationProviderTest`: given the unauthenticated token (externalUserId, environmentId), and a `ConnectedUserService` returning an enabled `ConnectedUser`, assert `authenticate()` returns an authenticated token whose principal username == the connected user's external id; a disabled connected user → `UserNotActivatedException`.
- [ ] **Step 2:** Run it, verify it fails (class missing).
- [ ] **Step 3: Implement** — copy `EmbeddedMcpServerApiKeyAuthenticationProvider` logic verbatim (fetch-or-create via `connectedUserService`, `createSpringSecurityUser`), typed to the OAuth2 token.
- [ ] **Step 4:** Run test, verify PASS.
- [ ] **Step 5: Commit** — `git commit -m "Add embedded MCP OAuth2 authentication provider"`.

---

### Task 5: Wire OAuth2 converter/provider onto the embedded chain

**Files:**
- Modify: `.../security/web/configurer/EmbeddedMcpServerSecurityConfigurer.java`
- Modify: the `SecurityConfigurerContributor` that constructs `EmbeddedMcpServerSecurityConfigurer` (find via `grep -rl EmbeddedMcpServerSecurityConfigurer .../config`), to inject `IdentityProviderService`, `ConnectedUserService`, `McpResourceServerProperties`/decoder factory, and the embedded AS issuer (`bytechef.public-url`).
- Modify: `.../embedded-ai-mcp-server/build.gradle.kts` — add `implementation(project(":server:ee:libs:platform:platform-security-web:platform-security-web-impl"))` (for `MultiIssuerJwtDecoder` et al.) and `implementation("org.springframework.security:spring-security-oauth2-jose")` if not already present; add `platform-user-api` (for `IdentityProviderService`) if absent.

**Interfaces:**
- Consumes: Tasks 1, 3, 4.
- Produces: the embedded chain now has the OAuth2 `ApiKeyAuthenticationFilter` (converter+provider) added **before** the signing-key filter, so OAuth2 tokens are consumed first and signing-key tokens fall through.

- [ ] **Step 1:** Read `AbstractApiKeyHttpConfigurer` and confirm each `AbstractApiKeyHttpConfigurer` instance adds one filter before `BasicAuthenticationFilter` and registers one provider. To add a second (OAuth2) pair, apply a **second** configurer to the chain (the contributor returns both, or `EmbeddedMcpServerSecurityConfigurer` registers the OAuth2 provider + adds the OAuth2 filter in its own `configure`). Add the OAuth2 filter with `http.addFilterBefore(oauth2Filter, <the signing-key filter's position>)`; since both use `addFilterBefore(BasicAuthenticationFilter.class)`, ensure the OAuth2 configurer is applied first so its filter runs first.
- [ ] **Step 2: Implement** the wiring: build the OAuth2 `MultiIssuerJwtDecoder` with an `IssuerLocationMcpJwtDecoderFactory` whose trusted set is resolved per-request (the converter passes the tenant's trusted issuers to the factory — construct the decoder inside the converter per request, or make the factory tenant-aware). Register `EmbeddedMcpServerOAuth2AuthenticationProvider`. Add the OAuth2 filter before the signing-key filter.
- [ ] **Step 3:** Compile the module, verify SUCCESS.
- [ ] **Step 4: Commit** — `git commit -m "Wire embedded MCP OAuth2 auth onto the embedded security chain"`.

---

### Task 6: End-to-end IntTest (JWT auth + coexistence + per-tenant isolation)

**Files:**
- Create: `.../security/web/EmbeddedMcpServerOAuth2SecurityIntTest.java`
- Create: `.../security/web/config/EmbeddedMcpServerOAuth2SecurityIntTestConfiguration.java`

**Interfaces:**
- Consumes: the whole chain (Tasks 1–5). Model the harness on `AutomationMcpOAuth2ResourceServerSecurityIntTest` + `AutomationMcpOAuth2ResourceServerSecurityIntTestConfiguration` (Testcontainers, random-port Tomcat, static-key `McpJwtDecoderFactory`, in-test JWT signing) and the existing embedded MCP server beans (`EmbeddedMcpServerConfiguration` transport + router + tool filter).

**Tests (each a `@Test`):**
- [ ] **Step 1: Write the failing tests:**
  - `testInitializeAndListToolsWithValidOAuth2Jwt`: seed an MCP server whose secret is `TenantKey.of("public")` at the path; stub `IdentityProviderService.getIdentityProviders()` (within tenant) to return an enabled IdP with the test issuer URI; sign a JWT (test issuer, `sub="ext-user-1"`); `initialize()` + `listTools()` succeed and a `ConnectedUser("ext-user-1")` is created (assert via `ConnectedUserService`/DB).
  - `testSigningKeyTokenStillAuthenticates`: a signing-key token (built via `SigningKeyService`, as the existing embedded IntTests do) still authenticates on the same endpoint (coexistence).
  - `testTokenFromIssuerTrustedForAnotherTenantIsRejected`: a JWT whose issuer is trusted for tenant B, presented on tenant A's endpoint (path secret = tenant A) → 401.
  - `testExpiredOrUntrustedIssuerJwtIsRejected`: expired token / issuer not configured → 401.
- [ ] **Step 2:** Run `:...:embedded-ai-mcp-server:testIntegration --tests EmbeddedMcpServerOAuth2SecurityIntTest --rerun-tasks`, verify they fail for the right reasons (feature incomplete), then iterate Tasks 3–5 until GREEN.
- [ ] **Step 3:** Run the module's full `test` + `testIntegration`, verify no regression in the existing embedded/signing-key tests.
- [ ] **Step 4: Commit** — `git commit -m "Prove embedded MCP OAuth2 end to end (auth, coexistence, per-tenant isolation)"`.

---

## Self-Review

- **Spec coverage:** SP-A requirements — resource server on embedded endpoint (Tasks 3–5), ConnectedUser mapping (Task 4), tenant from path secret (Task 3), environment from header (Task 3), per-tenant trusted issuers (Task 1, enforced Task 3), coexistence (Task 5 ordering, Task 6 test), reuse of `MultiIssuerJwtDecoder` (Tasks 3/5) — all mapped. SP-B and the CE relocation are out of scope (separate plans).
- **Verified-first item:** the plan's Task 5 Step 1 explicitly re-reads `AbstractApiKeyHttpConfigurer` to confirm the two-filter ordering before wiring — the one embedded-internal mechanic that must be confirmed against the code.
- **Open follow-up (not blocking SP-A):** whether all enabled IdPs are MCP-eligible or a dedicated flag is needed — SP-A trusts all enabled IdPs with an `issuerUri`; revisit if finer control is required.
