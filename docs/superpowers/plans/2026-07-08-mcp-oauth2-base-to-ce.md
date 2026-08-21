# Relocate base MCP OAuth2 to CE — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:executing-plans (or subagent-driven-development). Steps use checkbox syntax.

**Goal:** Move base MCP OAuth2 (embedded AS + self-issuer resource server) from EE to CE, leaving external-IdP federation in EE, per `docs/superpowers/specs/2026-07-08-mcp-oauth2-base-to-ce-design.md`.

**Architecture:** Extract 3 federation seams behind CE SPIs **first** (while everything is still EE, so each is independently testable), **then** do the mechanical EE→CE package relocation, **then** prove a CE-only build works.

## Global Constraints

- CE files: Apache 2.0 header, `com.bytechef.*` packages, no `@ConditionalOnEEVersion`, no reference to any `com.bytechef.ee.*` type.
- EE files: Enterprise header, `@version ee`, `com.bytechef.ee.*`, `@ConditionalOnEEVersion`.
- Behavior must not change on an EE build; a CE-only build must validate self-issuer JWTs and reject non-static issuers.
- Commit only files each task touches. Never amend. Run `./gradlew spotlessApply` + module `check`/`testIntegration` per task.
- Do NOT open a PR unless asked.

---

## Phase A — Extract federation seams (still all EE, behavior-preserving) — ✅ DONE

### Task A1: `McpFederatedIssuerAuthenticator` SPI + refactor `TenantAwareJwtAuthenticationFilter` — ✅ (commit `6625abf9bc9`)
**Files:** create `McpFederatedIssuerAuthenticator` (interface, will become CE); refactor `TenantAwareJwtAuthenticationFilter` so the static-issuer branch is inline and the per-tenant branch is moved behind the SPI; create an EE impl `TenantIdpFederatedIssuerAuthenticator` holding the current federation branch (`McpTenantTrustContext` lookup + `McpAudienceValidator.isTenantAudienceValid` + `mapTenantIssuer`).
- [x] Define `interface McpFederatedIssuerAuthenticator { Optional<McpJwtIdentity> authenticate(Jwt jwt, HttpServletRequest request, String urlTenantId); }`.
- [x] `TenantAwareJwtAuthenticationFilter` takes an optional `@Nullable McpFederatedIssuerAuthenticator`. Static issuer → existing `authenticateStaticIssuer`. Else → `authenticator != null ? authenticator.authenticate(...) : empty` → 401 on empty.
- [x] Move the per-tenant branch verbatim into the EE `TenantIdpFederatedIssuerAuthenticator`.
- [x] Wire the EE authenticator into `McpJwtTenantSecurityConfigurerContributor`.
- [x] Run `platform-security-web-impl` unit + IntTests — all green, no behavior change.
- [x] Commit.

### Task A2: Discovery entry point seam — ✅ (commit `a897d91045a`) — SIMPLER THAN PLANNED
**Finding during execution:** the discovery entry point (`McpTenantProtectedResourceMetadataAuthenticationEntryPoint`) carries **no** federation logic — `commence()` is a generic RFC 9728 per-request pointer (pure path insertion). Tenant-awareness lives entirely in the metadata *document* served at that URL (the EE controller/resolver/chain), which are **already** separate EE classes. So the plan's optional-injected-entry-point indirection is unnecessary (YAGNI): the entry point is base behavior and moves to CE directly with the discovery filter. A2 therefore became a *confirm-and-document* task, parallel to A3.
- [x] ~~Optional `@Nullable AuthenticationEntryPoint` + EE bean~~ → not needed; entry point is federation-neutral base.
- [x] Marker comments on the entry point + discovery contributor drawing the CE/EE line; unit test pinning the base challenge contract.
- [x] IntTests green (challenge still points at tenant metadata on the EE build).
- [x] Commit.
- **Phase B note:** relocate `McpTenantProtectedResourceMetadataAuthenticationEntryPoint` to CE **as-is** (consider renaming off the "Tenant" prefix, e.g. `McpProtectedResourceMetadataAuthenticationEntryPoint`); keep the tenant metadata controller/resolver/chain EE.

### Task A3: Confirm decoder-factory seam is federation-neutral — ✅ (commit `c5226fda145`)
**Files:** `IssuerLocationMcpJwtDecoderFactory`, `McpTenantTrustContext`.
- [x] Verify the factory only reads `McpTenantTrustContext` (a neutral thread-local) — no other `ee.*` reference. It already does; add a comment marking `McpTenantTrustContext` as CE-bound and the populating filter as EE.
- [x] No code change beyond the marker; unit test the empty-context (no per-tenant trust) path.
- [x] Commit.

---

## Phase B — Relocate base resource server EE→CE — ✅ DONE (+ API-key, per user scope choice; commit `444c874b012`)

> **Scope note:** the user chose to also move MCP **API-key** auth to CE in this pass (spec's "CE = API-key + OAuth2" end-state), reversing part of the earlier "Relocate MCP API key authentication to EE." Target CE module `platform-security-web-impl` was an unregistered orphan; Phase B registered it in `settings.gradle.kts` and gave it real resource-server deps.

### Task B1: Move base RS + API-key classes to CE `platform-security-web-impl` — ✅
**No class splitting was needed** (plan assumed splitting `McpJwtIdentityMapper`/`McpAudienceValidator`): because CE's `McpTenantTrustContext` exposes `McpTenantIssuer` in its API, that record moves to CE too, so the tenant methods reference only CE-visible types and stay on the whole (CE) classes. 24 classes moved (13 RS + discovery/metadata config + `McpTenantIssuer` + SPI + 5 API-key).
- [x] `git mv` per file; Apache header, drop `@version ee`/`@ConditionalOnEEVersion`, rewrite package + FQN refs; `{@link}` → `{@code}` for EE-staying refs in moved files.
- [x] Update EE federation classes to import the now-CE base types (FQN rewrite + added same-package imports).
- [x] CE module `build.gradle.kts` (mirrors EE minus caffeine) + registered in `settings.gradle.kts`; added CE dep to EE security-web, embedded MCP server, automation security-web (test).
- [x] `check` (spotless/checkstyle/pmd/spotbugs) green on both modules; full-repo `compileJava` green; `DisabledMcpOAuth2ResourceServerConfigurer` made public (cross-package access).
- [x] Committed as one cohesive relocation (all changes interdependent → single commit, not per-concern).

### Task B2: Test split — ✅ (base unit tests → CE; integrated IntTests kept EE)
- [x] Base unit tests → CE (44 tests: audience, bearer resolver, identity mapper, decoders, trust context, filter, entry point).
- [x] Federation unit tests stay EE (10 tests); the **integrated** base+federation IntTests stay EE (15 int) — they now exercise CE base beans + EE federation beans, the real EE deployment.
- **Deferred to Phase D:** rather than split the live-Tomcat IntTest into base-only vs federation, the CE-only proof is delivered by the purpose-built D1 smoke IntTest (base works with no EE on the classpath). Cleaner than bisecting an intertwined IntTest.

---

## Phase C — Relocate embedded authorization server EE→CE — ✅ DONE (commits `d45e803f4ea`, `2129d88a70b`)

### Task C1: Move base AS to CE — ✅
**Finding:** the AS config has **no** external-IdP federation entry (the SP-B broker / `authorizationRequestConverter` auto-federate was never built), so the whole `platform-oauth2-authorization-server` module is base → the entire module relocates to CE (`server/ee/libs` → `server/libs`, `com.bytechef.ee.platform.oauth2.authorizationserver` → `com.bytechef.platform.oauth2.authorizationserver`, Apache header, drop `@ConditionalOnEEVersion`).
- [x] Relocated whole module; updated `settings.gradle.kts`, the `server-app` dependency + its two AS boot IntTests, and `client/codegen.ts` (GraphQL schema path). Liquibase changelog + GraphQL schema stay on the same classpath resource paths. `@SuppressFBWarnings` added to `RegisteredClientInfo` (latent EE spotbugs debt).
- [x] AS module 26 tests + spotless/checkstyle/pmd/spotbugs green; server-app boots with the relocated module (fixed a latent mismatch: gated `McpTenantProtectedResourceMetadataController` on the issuers property — commit `d45e803f4ea`).
- [x] Committed (controller fix + relocation).

**Also (completing Phase B's JWT-chain split, commit `2ac4148945a`):** Phase B moved `TenantAwareJwtAuthenticationFilter` to CE but left its wiring configurer in EE, so a CE-only build wouldn't enforce scope/audience/revocation. Split `McpJwtTenantSecurityConfigurer(Contributor)` into a **CE** `McpJwtSecurityConfigurer(Contributor)` (wires the base filter; federation SPI via `ObjectProvider`) + an **EE** `McpTenantTrustResolutionConfigurer(Contributor)` (wires the trust filter) + an **EE** `McpFederatedIssuerAuthenticator` `@Bean`. EE behavior identical; CE-only now enforces the full base policy.

---

## Phase D — CE-only proof + verification — ✅ DONE (commits `47259fe74f9`, `<this>`)

### Task D1: CE-only smoke IntTest — ✅ (commit `47259fe74f9`)
- [x] `McpOAuth2ResourceServerCeOnlyIntTest` (+ config) in the CE `platform-security-web-impl` module (no `com.bytechef.ee.*` on its classpath), wiring only CE contributors and no federation. Proves (7 tests): self-issuer JWT authenticates + lists tools; a non-static issuer is rejected (no federation SPI); wrong scope / mismatched audience / deactivated user rejected; unauthenticated → RFC 9728 discovery challenge; static metadata advertises the self issuer.
- [x] Commit.

### Task D2: Whole-repo verification + edition reasoning — ✅
- [x] `./gradlew compileJava` (whole repo) green; affected modules' `test`/`testIntegration` green (CE 44 unit + 7 int; EE 10 unit + 15 int; AS 26; embedded 18 + int; automation + 4 int; server-app boot).
- [x] Grep: no `com.bytechef.ee.*` code ref in any moved CE file (only javadoc prose); every moved CE file has the Apache header and no `@ConditionalOnEEVersion`; every remaining federation file keeps `@version ee`.
- [x] Specs' status → Implemented; memory updated.
- [x] Commit.

## Self-review checklist — ✅ all satisfied
- CE-only build validates self-issuer JWTs and rejects non-static issuers (SPI absent) — proven by D1.
- EE build behavior is byte-for-byte the same as before the move (federation SPI present) — EE unit + int tests green.
- No CE file references any `ee.*` type; no federation file leaked to CE.
- The `IdentityProvider` data model stays CE; only federation behavior is EE.
