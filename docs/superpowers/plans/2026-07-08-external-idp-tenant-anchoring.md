# External-IdP Tenant Anchoring (Option A) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** For MCP endpoints whose URL path secret is a per-tenant `TenantKey` — **automation** and **embedded** — anchor the tenant to that ByteChef-issued URL secret and treat an external IdP OAuth2 token as **identity-only**, with **audience** binding the token to its endpoint. This removes external IdPs from the tenancy trust boundary and closes both the automation claim-injection gap and the embedded external-direct replay gap under one model. **Management is out of scope** (its URL secret is a platform-scoped gate, not a tenant — see spec).

**Spec:** `docs/superpowers/specs/2026-07-08-external-idp-tenant-anchoring-design.md` (Option A, confirmed 2026-07-08).

**Architecture:** Java 25, Spring Boot 4.0.7, Spring Security 7.1.0, `mcp-server-security` 0.1.13, Liquibase, JUnit 5 + Testcontainers. MCP OAuth2 is EE.

## Decisions (from spec review, 2026-07-08)

- **Option A** — URL-`TenantKey` anchor + audience, IdP token identity-only. Applies to **automation + embedded**; **management excluded**.
- **Q2 resolved — uniform URL anchor.** On the automation OAuth2 path the tenant comes from the URL `TenantKey` for **all** issuers (including `self`), not from `tenantClaim`. The AS-minted `tenant_id` becomes advisory; if present it must agree with the URL tenant (consistency assertion), else reject. Authorities still come from claims.
- **Q3 resolved — audience required for external issuers.** Under the URL anchor, a token from a *shared* external issuer with no `aud` is replayable, so audience enforcement is **required** for non-`self` issuers. The per-issuer opt-out (`Issuer.audience` unset) is retained only for an issuer the operator asserts is **single-tenant/dedicated**; the plan documents this as a reduced-assurance config and logs a warning when an external issuer is trusted without audience.

## Global Constraints

- MCP OAuth2 code is EE (`com.bytechef.ee.*`, EE header, `@version ee`, `@ConditionalOnEEVersion`). New embedded config fields on `IdentityProvider` follow the existing CE/EE split of that entity.
- No behavior change for the API-key / signing-key paths (already tenant-bound by construction) or for management.
- Backward-compat: automation tokens that today carry a valid `tenant_id` matching their endpoint keep working (the consistency assertion passes). Tokens whose claim disagrees with the URL are now rejected (previously the claim silently won) — this is the intended hardening; call it out in the commit.
- One concern per task; `test` + `testIntegration` after each; `spotlessApply` before every commit; never amend (parallel commits).

## Open questions (resolve in Task 0)

1. **Where to parse the URL `TenantKey` for automation OAuth2.** `TenantAwareJwtAuthenticationFilter` has the request; `McpJwtIdentityMapper.map(jwt, issuer, scopeAuthorities)` does not. Decide: parse in the filter and pass the tenant into a new mapper signature, or give the mapper the request URL. Confirm the automation path regex (`^/api/automation/(.+)/mcp`) and that the secret always parses as a `TenantKey`.
2. **`IdentityProvider` audience field shape** — a boolean `validateMcpAudience` (endpoint-URL check) vs an optional `mcpAudience` string. Recommend the boolean: embedded external-direct wants the token's `aud` to contain *this* `/api/embedded/{secret}/mcp` URL, which is per-request, not a fixed string. Confirm against how tenant IdPs actually emit `aud`.
3. **CE/EE home for the new `IdentityProvider` field + migration** — `IdentityProvider` is CE (`platform-user-api`), managed by the B-1 GraphQL + client. The new column/field/UI follow B-1's pattern exactly.

## Task 1: Automation — resolve tenant from the URL `TenantKey` (uniform anchor)

**Files:** `TenantAwareJwtAuthenticationFilter`, `McpJwtIdentityMapper` (+ `McpJwtIdentity`), their tests, `McpOAuth2ResourceServerSecurityIntTest`(+config), all EE `platform-security-web-impl`.

- [ ] **Step 1 (test):** unit test — a token whose `tenant_id` claim is **absent** but whose request URL secret is `TenantKey.of("acme")` resolves tenant `acme` (previously threw "missing tenant claim").
- [ ] **Step 2 (test):** unit test — a token whose `tenant_id` claim **disagrees** with the URL tenant is rejected (401).
- [ ] **Step 3:** run — both fail (mapper requires + trusts the claim).
- [ ] **Step 4 (impl):** in `TenantAwareJwtAuthenticationFilter`, parse the URL `TenantKey` (regex `^/api/automation/(.+)/mcp` → `TenantKey.parse(secret).getTenantId()`) and make it the authoritative tenant. Change `McpJwtIdentityMapper` to take the URL tenant: authorities still from `scopeAuthorities` + `authoritiesClaim` + issuer static; tenant = URL tenant; if the token carries `tenantClaim` and it disagrees → `OAuth2AuthenticationException`.
- [ ] **Step 5 (impl):** ensure audience is enforced for non-`self` issuers — if a non-`self` issuer has no configured audience, log a startup/first-use warning ("external issuer X trusted without audience — reduced assurance"). (Reuse `McpAudienceValidator`; the endpoint-URL check already exists.)
- [ ] **Step 6:** run unit + `McpOAuth2ResourceServerSecurityIntTest` (the management IntTest config uses `/api/management` — automation coverage lives where the automation OAuth2 IntTest is; extend that one) — a claim-less token authenticates via URL; a mismatched-claim token is rejected; a cross-endpoint (mismatched-`aud`) token is rejected.
- [ ] **Step 7:** `spotlessApply`; commit `Anchor automation MCP OAuth2 tenant to the URL TenantKey`.

## Task 2: Embedded — per-IdP audience (identity provider field + migration)

**Files:** `IdentityProvider` (+ domain/service), Liquibase migration, `platform-user-graphql` controller + `.graphqls`, client identity-provider GraphQL ops + dialog + hook (mirrors B-1).

- [ ] **Step 1 (test):** `IdentityProviderService` int test — persist/read the new `validateMcpAudience` flag.
- [ ] **Step 2:** run — fails (no column/field).
- [ ] **Step 3 (impl):** add `identity_provider.validate_mcp_audience BOOLEAN NOT NULL DEFAULT false` (migration) + `IdentityProvider.validateMcpAudience` accessor.
- [ ] **Step 4:** GraphQL — thread `validateMcpAudience` through `IdentityProviderType`/`Input` + controller records/mappers (mirror the B-1 `mcp` flag). Regenerate client types; add the checkbox to the dialog (OIDC-only, near the MCP flag) + hook. `npm run check`.
- [ ] **Step 5:** `spotlessApply`; commit `Add per-IdP embedded MCP audience flag`.

## Task 3: Embedded — enforce audience in the OAuth2 converter

**Files:** `EmbeddedMcpServerOAuth2AuthenticationConverter` (+ `EmbeddedMcpTrustedIssuerResolver`/`IdentityProviderService` access), tests, `EmbeddedMcpServerOAuth2SecurityIntTest`(+config), all EE `embedded-ai-mcp-server`.

- [ ] **Step 1 (test):** converter unit test — with the flagged IdP's `validateMcpAudience = true`, a token whose `aud` lacks the current `/api/embedded/{secret}/mcp` URL is rejected; one whose `aud` contains it is accepted; with the flag `false`, `aud` is not checked (today's behavior).
- [ ] **Step 2:** run — fails (no audience check).
- [ ] **Step 3 (impl):** after JWT validation, when the matched IdP has `validateMcpAudience`, require `jwt.getAudience()` to contain the current endpoint URL (reuse the same per-request-URL logic as `McpAudienceValidator`; extract a shared helper if clean, else mirror it). Build the endpoint URL from the request as the discovery entry point does.
- [ ] **Step 4:** run unit + `EmbeddedMcpServerOAuth2SecurityIntTest` — mismatched-`aud` rejected, matching accepted, flag-off unchanged.
- [ ] **Step 5:** `spotlessApply`; commit `Enforce embedded external-direct MCP audience`.

## Task 4: Verification + docs

- [ ] **Step 1:** `spotlessApply check` for touched modules + their `testIntegration`.
- [ ] **Step 2:** Reason through (record in commit): automation OAuth2 tenant now = URL, claim advisory, audience required for external → external IdP removed from tenancy trust. Embedded external-direct now audience-bound per flagged IdP. Management unchanged. API-key paths unchanged.
- [ ] **Step 3:** Update the spec status to Implemented; commit `Verify external-IdP tenant anchoring`.

## Self-review checklist

- Automation OAuth2 tenant comes from the URL `TenantKey`, never the claim; mismatched claim rejected.
- Non-`self` automation issuers without audience log a reduced-assurance warning.
- Embedded external-direct honors `validateMcpAudience` per IdP.
- Management, API-key, and signing-key paths untouched.
- New `IdentityProvider` field mirrors the B-1 `mcp`-flag plumbing (GraphQL + client + migration).
