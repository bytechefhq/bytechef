# Relocate MCP Authentication (API Key + OAuth2) to EE — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make MCP authentication (Phase 1 API keys + Phase 2 OAuth2 authorization server + the upcoming Phase B resource server) an **EE-only** capability. In a CE deployment the automation & management MCP servers revert to URL-path-secret-only auth; in an EE deployment (`bytechef.edition=ee`) the API-key + OAuth2 auth activates.

**Architecture:** ByteChef ships one `server-app` that bundles CE and EE modules; EE features activate via `@ConditionalOnEEVersion` (which requires `bytechef.edition=ee`). Relocation therefore means: move the MCP-auth classes into `com.bytechef.ee.*` EE modules with the ByteChef Enterprise license header + `@version ee`, and gate the `SecurityConfigurerContributor` / authorization-server `@Configuration` beans with `@ConditionalOnEEVersion`. The CE MCP server modules stop creating those beans. Model on the existing EE API-key auth: `server/ee/libs/platform/platform-security-web/platform-security-web-impl` (`PlatformApiKeySecurityConfigurerContributor` + `PlatformApiKeySecurityConfigurer` + `PlatformApiKeyAuthenticationProvider`) and `server/ee/libs/automation/automation-security-web/automation-security-web-impl` (`AutomationApiKeySecurityConfigurerContributor`).

**Tech Stack:** Java 25, Spring Boot 4.0.7, Spring Security 7.1.0, Spring Authorization Server 7.1.0, `org.springaicommunity:mcp-server-security` / `mcp-authorization-server` 0.1.13, Gradle, Liquibase, JUnit 5 + Testcontainers.

## Decision record (2026-07-06)

- **MCP OAuth2 + API-key auth are EE-only** (overrides the design spec's "Edition placement: CE"). Confirmed: in CE the MCP servers revert to URL-path-secret-only — the Phase 1 mandatory-API-key fix no longer applies to CE. This is intended; strong MCP auth is an EE feature.
- **Tenant claim: full multi-tenant.** Because the feature is now EE-only, the resource server can rely on the EE multi-tenant tenant services being present; no CE single-tenant fallback logic is required beyond the `"public"` default.

## Global Constraints

- **EE code conventions** (from CLAUDE.md): ByteChef Enterprise license header (NOT Apache 2.0) on every file under `server/ee/`; `@version ee` Javadoc tag on every class under `server/ee/`. Spotless picks the EE header from the `@version ee` content, so it must be present.
- **Gating:** every relocated auth bean that attaches to a security chain is `@ConditionalOnEEVersion` (requires `bytechef.edition=ee`). The `SecurityConfiguration` already collects `List<SecurityConfigurerContributor>`, so an EE-gated contributor bean is picked up automatically when the edition is EE and absent otherwise.
- **No behavior change when EE:** the relocated auth must behave identically to today's CE implementation under `bytechef.edition=ee` — same 401s, same principal, same tenant routing. All existing tests move with their code (adjusted for `@ActiveProfiles`/edition where they boot a chain).
- **Reversibility:** relocate one concern per task, re-running the relevant `test` + `testIntegration` after each, so any task can be reverted independently. Keep git history clean (one logical move per commit).
- Integration test classes end in `IntTest` and run under `testIntegration`. Run `./gradlew spotlessApply` before every commit.
- Commit messages: plain imperative, no ticket. EE relocation commits should say so, e.g. `Relocate MCP API key security to EE`.

## Open questions (resolve in Task 0)

1. **Management MCP configurer EE home.** The automation MCP configurer has a natural EE home (`automation-security-web-impl`). The management MCP server is `server/libs/ai/ai-mcp/ai-mcp-server` (CE) — is there an EE `*-security-web-impl` peer, or do we create `server/ee/libs/ai/ai-mcp/ai-mcp-security-web-impl` (or place it in `platform-security-web-impl`)? Decide in Task 0.
2. **Shared MCP classes home.** The shared classes (`McpApiKeyAuthenticationConverter`, `McpApiKeyEntity`, `McpApiKeyEntityRepository`, `McpApiKeyHttpConfigurer`, `TenantAwareApiKeyAuthenticationFilter`, `McpApiKeyCredentials`) go in EE `platform-security-web-impl` (package `com.bytechef.ee.platform.security.web.mcp`). Confirm nothing in CE still references them (only the per-server configurers do, which also move).
3. **`ApiKeyService.fetchApiKey` / `updateLastUsedDate`** (added to CE `platform-security-service` in Phase 1) — these are generic service methods, not auth mechanism. **Leave in CE** (harmless, and the EE code depends on the CE `ApiKeyService` interface). Confirm.
4. **AS module home + Liquibase.** New EE module `server/ee/libs/platform/platform-oauth2-authorization-server` (package `com.bytechef.ee.platform.oauth2.authorizationserver`). Its Liquibase changelog moves too; EE modules already contribute changelogs via the classpath `config/liquibase/changelog/...` + the CE `master.xml` `includeAll` (which scans the classpath), so the existing `includeAll` line stays and the changelog moves with the module. Verify the `includeAll` path still resolves from the EE module.
5. **AS enable flag.** Gate the AS `@Configuration` with BOTH `@ConditionalOnEEVersion` AND `@ConditionalOnProperty(bytechef.oauth2.authorization-server.enabled)`. Keep the property field in CE `ApplicationProperties` (harmless; strict binding needs it to exist). Confirm.
6. **`server-app` wiring + tests.** `server-app` currently depends on the CE AS module and has `ServerApplicationAuthorizationServerEnabledIntTest` (flag on). After relocation, `server-app` depends on the EE AS module; the enabled test must also set `bytechef.edition=ee` (else `@ConditionalOnEEVersion` keeps the AS off). Adjust.

---

## Task 0: Confirm EE homes + inventory the move

**Files:** none (investigation + this plan's open questions).

- [ ] **Step 1:** Resolve Open questions 1–6 by inspecting the EE module tree (`server/ee/libs/platform/platform-security-web/platform-security-web-impl`, `server/ee/libs/automation/automation-security-web/automation-security-web-impl`, `settings.gradle.kts`). Decide the management-MCP EE home and whether to create a new EE module for it.
- [ ] **Step 2:** Produce the exact old-path → new-path + old-package → new-package (`com.bytechef.*` → `com.bytechef.ee.*`) mapping for every file relocated in Tasks 1–4. Record it here.
- [ ] **Step 3:** Grep for every reference to the moving classes/packages across the repo (CE + tests) so no dangling import survives the move.

**Exit:** a concrete file/package move-map and confirmed EE module homes.

---

## Task 1: Relocate the shared MCP API-key classes (CE → EE `platform-security-web-impl`)

**Files:**
- Move (CE→EE): `server/libs/platform/platform-security-web/platform-security-web-api/src/main/java/com/bytechef/platform/security/web/mcp/{McpApiKeyAuthenticationConverter,McpApiKeyCredentials,McpApiKeyEntity,McpApiKeyEntityRepository,McpApiKeyHttpConfigurer,TenantAwareApiKeyAuthenticationFilter}.java` → `server/ee/libs/platform/platform-security-web/platform-security-web-impl/src/main/java/com/bytechef/ee/platform/security/web/mcp/…`
- Move the matching unit tests (`McpApiKeyAuthenticationConverterTest`, `McpApiKeyEntityRepositoryTest`, `TenantAwareApiKeyAuthenticationFilterTest`, `McpServerSecurityLibrarySmokeTest`) into the EE module's test source.
- Modify: EE module `build.gradle.kts` (add the `mcp-server-security` dependency + whatever the moved classes need — it already `api`-exports library types today from `platform-security-web-api`; move that dependency declaration).
- Modify: `platform-security-web-api/build.gradle.kts` — drop the `mcp-server-security` `api` dependency if nothing CE uses it anymore.

- [ ] **Step 1:** Move files; rewrite `package com.bytechef.platform.security.web.mcp` → `package com.bytechef.ee.platform.security.web.mcp`; swap the Apache header for the ByteChef Enterprise header; add `@version ee` Javadoc.
- [ ] **Step 2:** Update the EE module `build.gradle.kts` dependencies (library + `platform-security-api`, `platform-user-api`, `tenant-api`, etc.).
- [ ] **Step 3:** Run the moved unit tests: `./gradlew :server:ee:libs:platform:platform-security-web:platform-security-web-impl:test`. Expected: green (same assertions as before).
- [ ] **Step 4:** `spotlessApply`; commit `Relocate shared MCP API key security classes to EE`.

**Interfaces produced:** `com.bytechef.ee.platform.security.web.mcp.*` — consumed by Tasks 3 & 4.

---

## Task 2: Relocate `ApiKeyService` additions decision

**Files:** none to move (per Open question 3, the `fetchApiKey` / `updateLastUsedDate` methods stay in CE `platform-security-service`).

- [ ] **Step 1:** Confirm no CE production code other than the (now-moved) MCP auth referenced these in a way that breaks. They are generic; leave in place. No commit.

---

## Task 3: Relocate the automation MCP configurer/provider (CE → EE `automation-security-web-impl`)

**Files:**
- Move (CE→EE): `server/libs/automation/automation-ai/automation-ai-mcp-server/src/main/java/com/bytechef/automation/ai/mcp/server/security/web/{configurer/AutomationMcpServerSecurityConfigurer,authentication/AutomationMcpServerApiKeyAuthenticationProvider}.java` → `server/ee/libs/automation/automation-security-web/automation-security-web-impl/src/main/java/com/bytechef/ee/automation/…/mcp/…` (package `com.bytechef.ee.automation.security.web.mcp`).
- Create (EE): `AutomationMcpServerApiKeySecurityConfigurerContributor` (`@Configuration @ConditionalOnEEVersion implements SecurityConfigurerContributor`) — mirrors `AutomationApiKeySecurityConfigurerContributor`; constructs the moved configurer with `ApiKeyService`/`AuthorityService`/`McpServerService`/`UserService`.
- Modify (CE): `automation-ai-mcp-server` `AutomationMcpServerConfiguration` — **delete** the `automationMcpServerSecurityConfigurerContributor` `@Bean` (now provided by EE).
- Move the tests: `AutomationMcpServerApiKeyAuthenticationProviderTest`, `AutomationMcpServerSecurityFilterChainTest`, and the Testcontainers `AutomationMcpServerSecurityIntTest` (+ its IntTest config) into the EE module. The filter-chain/int tests boot a chain applying the configurer, so they run fine in EE; add `bytechef.edition=ee` context if any bean is `@ConditionalOnEEVersion` in the boot.
- Modify build files: EE module `build.gradle.kts` gains deps on the CE `automation-ai-mcp-server-api` (for `McpServerService` etc.), `platform-security-web-impl` (moved shared classes), library. CE `automation-ai-mcp-server` drops the now-unused security deps if any.

- [ ] Steps: move + repackage + EE header/`@version ee`; create the `@ConditionalOnEEVersion` contributor; delete the CE bean; move tests; `./gradlew :…automation-security-web-impl:test :…:testIntegration` green; the CE `automation-ai-mcp-server:test` still green (now with no auth contributor). `spotlessApply`; commit `Relocate automation MCP API key security to EE`.

---

## Task 4: Relocate the management MCP configurer/provider (CE → EE)

**Files:** same shape as Task 3 for `server/libs/ai/ai-mcp/ai-mcp-server` → the EE home chosen in Task 0 (`ai-mcp-security-web-impl` new module, or `platform-security-web-impl`). Move `ManagementMcpServerSecurityConfigurer` + `ManagementMcpServerApiKeyAuthenticationProvider`; create `ManagementMcpServerApiKeySecurityConfigurerContributor` (`@ConditionalOnEEVersion`); delete the CE `mcpServerSecurityConfigurerContributor` `@Bean` from `ManagementMcpServerConfiguration`; move the tests.
- [ ] Steps mirror Task 3. Commit `Relocate management MCP API key security to EE`.

---

## Task 5: Relocate the authorization server module (CE → EE)

**Files:**
- Move the whole module `server/libs/platform/platform-oauth2-authorization-server` → `server/ee/libs/platform/platform-oauth2-authorization-server`; repackage `com.bytechef.platform.oauth2.authorizationserver` → `com.bytechef.ee.platform.oauth2.authorizationserver`; EE headers + `@version ee`; move the Liquibase changelog with it (path under `config/liquibase/changelog/platform/oauth2-authorization-server/` unchanged so the `master.xml` `includeAll` still resolves from the EE jar).
- Modify: `Oauth2AuthorizationServerConfiguration` — add `@ConditionalOnEEVersion` alongside the existing `@ConditionalOnProperty`.
- Modify `settings.gradle.kts` (module path change) and `master.xml` only if the changelog path changes (it should not).
- Modify `server-app/build.gradle.kts`: replace the CE module dependency with the EE module dependency.
- Move the module's `*IntTest`s (schema, enabled, disabled, chain-scoping, auth-code-flow, consent) into the EE module; they set `bytechef.oauth2.authorization-server.enabled=true` and now must also run as edition EE — add `bytechef.edition=ee` to their `@SpringBootTest(properties=...)` so `@ConditionalOnEEVersion` passes.
- Modify `ServerApplicationAuthorizationServerEnabledIntTest`: add `bytechef.edition=ee` so the AS actually activates; `ServerApplicationIntTest.testAuthorizationServerBeansNotPresentWhenDisabled` still valid (flag off).

- [ ] Steps: move module; repackage; EE headers; `@ConditionalOnEEVersion`; fix `settings.gradle.kts` + `server-app` dep; adjust tests for edition EE; run `:server:ee:libs:platform:platform-oauth2-authorization-server:testIntegration` (8 tests) + the two `server-app` boot tests green. `spotlessApply`; commit `Relocate embedded OAuth2 authorization server to EE`.

---

## Task 6: Update the design spec + Phase 2 plan (honest record)

**Files:** `docs/superpowers/specs/2026-07-05-mcp-server-api-key-oauth2-auth-design.md`, `docs/superpowers/plans/2026-07-05-mcp-server-oauth2-phase2.md`.
- [ ] Record that the edition placement changed from CE to **EE** (2026-07-06 decision), with the rationale (MCP strong auth is an EE feature; CE reverts to URL-secret-only). Do not silently rewrite history — add a dated correction, matching the existing correction-note style. Commit `Record MCP auth relocation to EE in the spec and plan`.

---

## Task 7: Full verification

- [ ] Run the full `check` + `testIntegration` for every touched module (CE MCP servers now sans-auth; the EE security modules; the EE AS module; `server-app`). Confirm: CE MCP server modules build green with no auth contributor; EE modules green; `server-app` boots with edition default (AS off) and with `edition=ee`+flag (AS on). `spotlessApply`; final commit if needed.

---

## After relocation: Phase B (resource server) is built EE-native

Once relocated, Phase B (resource-server support on the MCP endpoints — JWT alongside API key, `mcp:automation`/`mcp:management` scopes, tenant claim, `401 + WWW-Authenticate` discovery) is implemented directly in the EE modules, using the EE multi-tenant tenant services for the tenant claim. Phase B is a separate plan/PR; this plan only relocates the existing Phase 1 + Phase A work so Phase B has an EE home.

## Risks

- **[HIGH] CE security regression is intended but must be explicit.** After this, CE MCP servers accept URL-secret only. The release notes must state that mandatory MCP API-key/OAuth auth is EE-only.
- **[MED] `@ConditionalOnEEVersion` + filter-chain timing.** The AS `SecurityFilterChain` and the MCP `SecurityConfigurerContributor`s must only materialize under EE. Verify with a CE-edition boot test (chains absent) and an EE-edition boot test (chains present) — this is the coexistence guard, re-run from Phase A.
- **[MED] Test relocation drift.** Moving Testcontainers `*IntTest`s across modules can silently drop coverage if a module lacks the test deps. Re-run each moved test and confirm the count matches pre-move.
- **[LOW] Liquibase path.** The `oauth2_*` changelog must still be discovered by `master.xml`'s `includeAll` from the EE jar; verify the table is created in an EE integration test.

## Self-review

- Every currently-committed MCP-auth artifact has a relocation task: shared classes (T1), automation configurer (T3), management configurer (T4), AS module + Liquibase + server-app wiring + tests (T5), spec/plan (T6). `ApiKeyService` additions consciously stay in CE (T2). Phase B is explicitly out of this plan's scope (built EE-native afterward).
- No new production behavior is introduced — this is a move + gate. The only behavior *change* is that CE no longer enforces MCP auth, which is the intended decision recorded above.
