# Relocate MCP API-Key Auth to CE — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Make **MCP API-key authentication** a CE capability — a plain CE deployment's automation & management MCP servers enforce API-key auth (not just URL-path-secret). **All OAuth2 stays EE** — the embedded authorization server, the OAuth2 resource server (JWT validation, tenant mapping, discovery, audience binding), and external-IdP federation are unchanged and remain EE-gated.

**Architecture:** One `server-app` bundles CE + EE; EE features activate via `@ConditionalOnEEVersion`. This relocation moves only the **MCP API-key** classes from `com.bytechef.ee.*` EE modules back to `com.bytechef.*` CE modules: swap the ByteChef Enterprise license header for Apache 2.0, drop `@version ee`, and remove `@ConditionalOnEEVersion` from the MCP API-key contributor beans so they activate in CE. The `SecurityConfiguration` collects `List<SecurityConfigurerContributor>`, so a CE-unconditional API-key contributor is picked up in both editions, and the EE OAuth2 contributors continue to compose on the same chain when the edition is EE.

**Tech Stack:** Java 25, Spring Boot 4.0.7, Spring Security 7.1.0, `org.springaicommunity:mcp-server-security` 0.1.13, Gradle, Liquibase, JUnit 5 + Testcontainers.

## Decision record (2026-07-08)

- **Only MCP API-key auth → CE. All OAuth2 → EE** (confirmed 2026-07-08). CE deployment: automation & management MCP servers enforce API-key (coexisting with URL-secret as today). OAuth2 (issue + validate + external federation) is entirely an EE feature. The embedded MCP server is untouched (fully EE).
- **General (non-MCP) API-key auth stays EE.** `PlatformApiKey*` and `AutomationApiKey*` (the pre-existing EE API-key auth for the public/automation APIs, which the MCP API-key auth was modeled on) are NOT part of this move — only the MCP-scoped API-key classes relocate. Task 0 confirms the exact MCP-vs-general split.
- **No external-issuer gate needed** (that was only relevant if the resource server moved to CE; it does not).

## Global Constraints

- **CE code conventions:** Apache 2.0 header (NOT the EE header) on every relocated file now under `server/libs/`; **remove** `@version ee` (Spotless flips the header based on that tag's presence).
- **No behavior change under EE:** with `bytechef.edition=ee`, every MCP endpoint behaves exactly as today — API-key + OAuth2 coexist, same 401s, same principal, same tenant routing.
- **New CE behavior:** under CE, the automation/management MCP endpoints now enforce API-key auth (the mandatory-API-key behavior that was EE-only becomes CE). Document this in the final commit.
- **Reversibility:** one concern per task; re-run `test` + `testIntegration` after each; one logical move per commit.
- **Parallel-commit safety:** the user commits in parallel on this branch — never amend; fresh commits; stage only files this task touches.
- Integration test classes end in `IntTest` (run under `testIntegration`). Run `./gradlew spotlessApply` before every commit.
- Commit messages: plain imperative, e.g. `Relocate shared MCP API key classes to CE`.

## Open questions (resolve in Task 0)

1. **CE home for the shared MCP API-key classes** (`McpApiKeyAuthenticationConverter, McpApiKeyCredentials, McpApiKeyEntity, McpApiKeyEntityRepository, McpApiKeyHttpConfigurer, TenantAwareApiKeyAuthenticationFilter`). The CE peer holding `AbstractApiKeyHttpConfigurer` + `ApiKeyAuthenticationFilter` is `server/libs/platform/platform-security-web/platform-security-web-api`. Decide: reuse `-api`, or create a CE `platform-security-web-impl`. A repository + `@ConfigurationProperties`-free set of classes can live in `-api`; a contributor bean may prefer an `-impl`. Confirm.
2. **MCP-vs-general API-key split.** Confirm which classes in EE `platform-security-web-impl` and `automation-security-web-impl` are **MCP-scoped** (move) vs **general API-key** (stay EE). Expected to move: shared `mcp/McpApiKey*`, management `ManagementMcpServerApiKey*` + `ManagementMcpServerSecurityConfigurer` + its contributor, automation `AutomationMcpServer*` + its contributor. Expected to stay EE: `PlatformApiKey*`, `AutomationApiKey*` (general). Verify by reading each class's path pattern / usage.
3. **CE home for the management MCP API-key configurer.** Management server is `server/libs/ai/ai-mcp/ai-mcp-server` (CE). Place its configurer in a CE `ai-mcp` security module or the platform CE security home.
4. **CE home for the automation MCP API-key configurer.** Is there a CE `automation-security-web` peer, or does it go to the platform CE home? Decide.
5. **`mcp_api_key` persistence.** If `McpApiKeyEntity`/`McpApiKeyEntityRepository` back a table/changelog that moved to EE, the changelog moves back to CE with them (CE `master.xml` `includeAll` scans the classpath). Verify whether MCP API keys use a dedicated table or the CE `api_key` table; move the changelog only if dedicated.
6. **`@ConditionalOnEEVersion` removal.** The MCP API-key contributors (`ManagementMcpServerApiKeySecurityConfigurerContributor`, `AutomationMcpServerApiKeySecurityConfigurerContributor`, and any platform MCP API-key contributor) drop `@ConditionalOnEEVersion` so they activate in CE. Confirm none of them also wires OAuth2 (which must stay EE) — if a contributor mixes API-key + OAuth2, split it.

**Task 0 exit:** a concrete old-path→new-path + old-package→new-package move-map for the MCP API-key classes only, confirmed CE homes, the MCP-vs-general split, and a repo-wide grep of references so no dangling import survives.

### Task 0 result (resolved 2026-07-08)

Authoritative reverse of commit `cd28b26335e` ("Relocate MCP API key authentication to EE"). Confirmed via grep: no external consumer imports these; the EE OAuth2 stack references `TenantAwareApiKeyAuthenticationFilter` only in Javadoc (safe). `PlatformApiKeySecurityConfigurer` scopes to `^/api/platform/v[0-9]+/.+` (general public API) → **stays EE**. No dedicated `mcp_api_key` changelog moved (entity is a `UserDetails`/`ApiKeyEntity` adapter, not a `@Table`).

- **T1 shared MCP (EE `com.bytechef.ee.platform.security.web.mcp` → CE `platform-security-web-api` `com.bytechef.platform.security.web.mcp`):** `McpApiKeyAuthenticationConverter`, `McpApiKeyCredentials`, `McpApiKeyEntity`, `McpApiKeyEntityRepository`, `McpApiKeyHttpConfigurer`, `TenantAwareApiKeyAuthenticationFilter` + tests `McpApiKeyAuthenticationConverterTest`, `McpApiKeyEntityRepositoryTest`, `McpServerSecurityLibrarySmokeTest`, `TenantAwareApiKeyAuthenticationFilterTest`.
- **T2 management MCP (EE → CE `ai-mcp-server` `com.bytechef.ai.mcp.server.security.web.*`):** `ManagementMcpServerApiKeyAuthenticationProvider` (authentication/), `ManagementMcpServerSecurityConfigurer` (configurer/), `ManagementMcpServerApiKeySecurityConfigurerContributor` (config/, drop `@ConditionalOnEEVersion`) + 4 tests.
- **T3 automation MCP (EE `automation-security-web-impl` → CE `automation-ai-mcp-server` `com.bytechef.automation.ai.mcp.server.security.web.*`):** `AutomationMcpServerApiKeyAuthenticationProvider` (authentication/), `AutomationMcpServerSecurityConfigurer` (configurer/), `AutomationMcpServerApiKeySecurityConfigurerContributor` (config/, drop `@ConditionalOnEEVersion`) + 4 tests.
- **Headers:** Spotless swaps EE→Apache header automatically once `@version ee` is removed and the file sits under a CE path — so per file: `git mv`, rewrite package, delete the `@version ee` Javadoc line, then `spotlessApply`.
- **build.gradle:** CE homes regain `mcp-server-security` + the deps commit `cd28b26335e` added to the EE modules (see that commit's diff); leave the EE `platform-security-web-impl` deps (the OAuth2 code that stays still needs them).

---

## Task 1: Relocate shared MCP API-key classes (EE → CE)

**Move (EE→CE):** `com.bytechef.ee.platform.security.web.mcp.{McpApiKeyAuthenticationConverter, McpApiKeyCredentials, McpApiKeyEntity, McpApiKeyEntityRepository, McpApiKeyHttpConfigurer, TenantAwareApiKeyAuthenticationFilter}` → `com.bytechef.platform.security.web.mcp.*` in the CE home (Task 0 Q1). Move their unit tests (`McpApiKeyAuthenticationConverterTest`, `McpApiKeyEntityRepositoryTest`, `TenantAwareApiKeyAuthenticationFilterTest`, any smoke test).

- [ ] **Step 1:** Move files; repackage `…ee.platform.security.web.mcp` → `…platform.security.web.mcp`; swap EE header for Apache 2.0; remove `@version ee`.
- [ ] **Step 2:** Update CE + EE `build.gradle.kts` (CE home gains `mcp-server-security` + `platform-user-api`/`tenant-api`/etc. as needed; EE `-impl` drops what it no longer uses).
- [ ] **Step 3:** If a dedicated `mcp_api_key` changelog exists (Task 0 Q5), move it to the CE module's `config/liquibase/changelog/…`.
- [ ] **Step 4:** `./gradlew :<ce-module>:test :<ce-module>:testIntegration` — green (same assertions).
- [ ] **Step 5:** `spotlessApply`; commit `Relocate shared MCP API key classes to CE`.

**Produces:** `com.bytechef.platform.security.web.mcp.*` — consumed by Tasks 2, 3.

## Task 2: Relocate the management MCP API-key configurer (EE → CE, un-gate)

**Move:** `ManagementMcpServerApiKeyAuthenticationProvider`, `ManagementMcpServerSecurityConfigurer`, `ManagementMcpServerApiKeySecurityConfigurerContributor` → CE home (Task 0 Q3). If a platform-level MCP API-key contributor exists (`PlatformApiKeySecurityConfigurerContributor` scoped to MCP), split its MCP part out and move only that.

- [ ] **Step 1:** Move + repackage + re-header + drop `@version ee`.
- [ ] **Step 2:** Remove `@ConditionalOnEEVersion` from the contributor so it activates in CE.
- [ ] **Step 3:** Move `ManagementMcpServerSecurityIntTest` + its config; drop `bytechef.edition=ee` from the boot properties (so it boots the CE contributor).
- [ ] **Step 4:** `testIntegration` green (management MCP endpoint enforces API-key without edition=ee).
- [ ] **Step 5:** `spotlessApply`; commit `Relocate management MCP API key security to CE`.

## Task 3: Relocate the automation MCP API-key configurer (EE → CE, un-gate)

**Move:** `AutomationMcpServerApiKeyAuthenticationProvider`, `AutomationMcpServerSecurityConfigurer`, `AutomationMcpServerApiKeySecurityConfigurerContributor` (from `automation-security-web-impl` EE) → CE home (Task 0 Q4). Leave the general `AutomationApiKey*` in EE.

- [ ] **Step 1:** Move + repackage `…ee.automation.security.web…` → `…automation.security.web…` + re-header + drop `@version ee`.
- [ ] **Step 2:** Remove `@ConditionalOnEEVersion` from `AutomationMcpServerApiKeySecurityConfigurerContributor`.
- [ ] **Step 3:** Move any automation-MCP API-key IntTest + config; drop `bytechef.edition=ee`.
- [ ] **Step 4:** `testIntegration` green.
- [ ] **Step 5:** `spotlessApply`; commit `Relocate automation MCP API key security to CE`.

## Task 4: Full verification + EE-parity + CE-behavior check

- [ ] **Step 1:** `./gradlew spotlessApply check` + `testIntegration` for every touched module.
- [ ] **Step 2:** Grep for surviving `com.bytechef.ee.…mcp.McpApiKey*` / management / automation MCP API-key imports outside their new CE homes; fix dangling refs.
- [ ] **Step 3:** Confirm the OAuth2 stack is untouched and still EE-gated: resource server, embedded AS, external federation, discovery, audience — all still `com.bytechef.ee.*` with `@version ee` + `@ConditionalOnEEVersion`. The `McpBearerTokenResolver` still falls through for non-JWT, so CE API-key + EE OAuth2 coexist on the same chain in EE.
- [ ] **Step 4:** Reason through both editions (record in the commit): **CE** → automation/management MCP endpoints enforce API-key; no OAuth2. **EE** → identical to pre-refactor (API-key + OAuth2 coexist). Embedded MCP server unchanged.
- [ ] **Step 5:** Commit `Verify MCP API-key CE relocation`.

## Execution complete (2026-07-08)

- **T1** (`22f8827f735`) shared MCP API-key classes → CE `platform-security-web-api`; 12 unit tests green.
- **T2** (`3bd66004f9a`) management MCP configurer → CE `ai-mcp-server`; management tests + EE OAuth2 coexistence IntTest green. EE `platform-security-web-impl` gained an `ai-mcp-server` test dep.
- **T3** (`6bdfeb06169`) automation MCP configurer → CE `automation-ai-mcp-server`; automation API-key tests + EE OAuth2 IntTest green. EE `automation-security-web-impl` gained an `automation-ai-mcp-server` test dep.
- **T4** verification: whole-repo `compileJava compileTestJava` clean (all apps/modules, main + test); no surviving `com.bytechef.ee.*` MCP API-key references; moved classes carry the Apache header with no `@version ee`/`@ConditionalOnEEVersion`; the OAuth2 stack (9 `mcp/oauth2` files) is untouched and still EE-gated.

**Edition behavior (reasoned):** CE → the MCP API-key contributors are now CE-unconditional, so CE automation/management MCP endpoints enforce API-key auth; the OAuth2 contributors remain `@ConditionalOnEEVersion`, so CE has no OAuth2. EE → API-key (CE) + OAuth2 (EE) coexist exactly as before (`McpBearerTokenResolver` falls through for non-JWT), proven by the management + automation OAuth2 coexistence IntTests. Embedded MCP server untouched.

## Self-review checklist

- Every relocated file: Apache header, no `@version ee`, package `com.bytechef.*`.
- Only the **MCP API-key** classes moved; general `PlatformApiKey*` / `AutomationApiKey*` and the whole OAuth2 stack stayed EE.
- No MCP API-key contributor still carries `@ConditionalOnEEVersion`.
- Moved tests pass in their CE home; boot-a-chain tests no longer force `bytechef.edition=ee` for API-key.
- Embedded MCP server untouched.
