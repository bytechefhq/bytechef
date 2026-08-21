# Relocate `identity_provider` CE→EE + rename `mcp`→`mcp_embedded` — Implementation Plan

> **STATUS: ✅ IMPLEMENTED (2026-07-09).** E1 rename landed earlier (in the squashed history). E2–E7 executed: EE `platform-user-api` (domain + event + service iface) and `platform-user-service` (impl + repository + JDBC-repo autoconfig + 6 changelogs on the same classpath path) created; all EE consumers repointed; `SsoDiscoveryController` → EE `security-sso-config`; `CustomOidcUserService` split into CE `SocialOidcUserService` (social login) + EE `CustomOidcUserService` (SSO); `server-app` wired for the EE service. Full-repo compile (main+test) + affected tests + static analysis + edition greps all green.

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:executing-plans (or subagent-driven-development). Steps use checkbox syntax.

**Goal:** Move the entire `identity_provider` capability (SSO login config + MCP federation config) from CE to EE, and rename the misnamed `mcp` embedded-surface field to `mcp_embedded`, per `docs/superpowers/specs/2026-07-08-identity-provider-to-ee-design.md`.

**Architecture:** Rename first (isolated, still CE). Then stand up EE `platform-user-api` + `platform-user-service`, relocate the domain/service/repository/event/changelogs into them (keeping the Liquibase classpath path identical so upgrades don't break), repoint the already-EE consumers, and move the two CE SSO-login blockers to EE. Prove a CE-only build boots with no SSO beans and no `identity_provider` table, and an EE build behaves identically.

## Global Constraints

- CE files: Apache 2.0 header, `com.bytechef.*` packages, no `@ConditionalOnEEVersion`.
- EE files: ByteChef Enterprise header, `@version ee` Javadoc tag, `com.bytechef.ee.*` packages, `@ConditionalOnEEVersion` on Spring `@Configuration`/`@Component` beans.
- Liquibase: EE changelogs keep the **identical classpath path** `config/liquibase/changelog/platform/user/` and **unchanged `id`/`author`** so `DATABASECHANGELOG.filepath` matches — no re-run, no checksum break.
- Behavior must not change on an EE build; a CE-only build must boot with local login + API-key + self-AS OAuth2 and no `identity_provider` table.
- Commit only files each task touches. Never amend (user commits in parallel; fresh commits). Run `./gradlew spotlessApply` + affected-module `check`/`testIntegration` per task.
- Do NOT open a PR unless asked.

---

## Phase E0 — Prep / confirm (no code change)

### Task E0.1: Confirm changelog table scope
**Why:** a changelog that also alters a CE-retained table (`user`, etc.) can't move wholesale.
- [ ] Open each of the 5 changelogs under `server/libs/platform/platform-user/platform-user-service/src/main/resources/config/liquibase/changelog/platform/user/`: `20250208000000_*_identity_provider_tables.xml`, `20260209000000_*_saml_scim_mfa_columns.xml`, `20260707000000_*_identity_provider_mcp_column.xml`, `20260708000000_*_validate_mcp_audience_column.xml`, `20260708000001_*_mcp_surface_and_authority_mapping.xml`.
- [ ] Confirm every changeset targets only `identity_provider`, `identity_provider_domain`, `identity_provider_authority_mapping`. If any also alters a CE table, note it — that changeset stays in CE (split required); the rest move.
- [ ] No commit (investigation only). Record findings inline in the plan.

### Task E0.2: Confirm `CustomOidcUserService` bean wiring — ✅ DONE (finding below)
**Finding (verified 2026-07-08):** `CustomOidcUserService` is a CE `@Service` `@ConditionalOnExpression("bytechef.security.social-login.enabled or bytechef.security.sso.enabled")`, injected by **two** chains: CE social login (`SingleTenantOAuth2LoginCustomizer`, `@ConditionalOnSingleTenant` + `social-login.enabled`) and EE SSO (`SsoSecurityConfiguration`, `@ConditionalOnEEVersion` + `sso.enabled`). Its only `IdentityProvider` coupling is the `registrationId.startsWith("sso-")` branch (autoProvision/defaultAuthority). **⇒ It cannot move to EE wholesale (breaks CE social login).** E6 becomes an SPI extraction, not a move. (`CustomOAuth2UserService`, the non-OIDC sibling, does NOT reference `IdentityProvider` — it stays CE untouched.)

---

## Phase E1 — Rename `mcp` → `mcp_embedded` (still CE, isolated)

### Task E1.1: Rename the entity field + accessors
**Files:** `server/libs/platform/platform-user/platform-user-api/src/main/java/com/bytechef/platform/user/domain/IdentityProvider.java`
- [ ] Rename the `mcp` field, `@Column`/mapping if explicit, `isMcp()` → `isMcpEmbedded()`, `setMcp(boolean)` → `setMcpEmbedded(boolean)`. Update the field Javadoc to "embedded MCP surface flag".
- [ ] `./gradlew :server:libs:platform:platform-user:platform-user-api:compileJava` — expect downstream break (fixed next steps), so this step just confirms the entity compiles in isolation.

### Task E1.2: Update all `isMcp()`/`setMcp()` call sites
**Files (verified call sites):**
- `server/ee/libs/platform/platform-security-web/platform-security-web-impl/src/main/java/com/bytechef/ee/platform/security/web/mcp/oauth2/McpTenantIssuerResolver.java` (`Surface.EMBEDDED(IdentityProvider::isMcp)` → `::isMcpEmbedded`)
- `server/ee/libs/platform/platform-user/platform-user-graphql/src/main/java/com/bytechef/ee/platform/user/web/graphql/IdentityProviderGraphQlController.java` (`identityProvider.isMcp()` read; `identityProvider.setMcp(input.mcp())` write — see E1.3 for the GraphQL field)
- Tests: `McpTenantIssuerResolverTest`, `IdentityProviderServiceIntTest`, embedded tests (`EmbeddedMcpServerOAuth2AuthenticationConverterTest`, `EmbeddedMcpServerOAuth2SecurityIntTest`, `EmbeddedMcpTrustedIssuerResolverTest`, `EmbeddedMcpProtectedResourceMetadataResolverTest`, `EmbeddedMcpDiscoveryIntTest`)
- [ ] Replace `isMcp(`→`isMcpEmbedded(` and `setMcp(`→`setMcpEmbedded(` in these files only (do NOT touch `ApplicationProperties.setMcp(Mcp)` — unrelated type).
- [ ] `./gradlew compileJava` (affected modules) green.

### Task E1.3: Rename the GraphQL field + regen client
**Files:** `server/ee/libs/platform/platform-user/platform-user-graphql/src/main/resources/graphql/identity-provider.graphqls` (type field line 27 `mcp: Boolean!`; input field line 57 `mcp: Boolean`)
- [ ] Rename both GraphQL fields `mcp` → `mcpEmbedded`; update the controller mapping (`input.mcp()` → `input.mcpEmbedded()`; response `.isMcpEmbedded()`).
- [ ] Client: rename in `client/src/ee/pages/settings/platform/identity-providers/components/{IdentityProviderDialog.tsx,IdentityProvidersTable.tsx,hooks/useIdentityProviderDialog.ts}` and the `.graphql` operation(s), then `cd client && npx graphql-codegen` to regenerate `src/shared/middleware/graphql.ts` + `graphql-types.ts`.
- [ ] `cd client && npm run check` green.

### Task E1.4: Add the column-rename changelog
**Files:** new `server/libs/platform/platform-user/platform-user-service/src/main/resources/config/liquibase/changelog/platform/user/20260709000000_platform_user_rename_identity_provider_mcp_column.xml`
- [ ] `renameColumn` `identity_provider.mcp` → `mcp_embedded` (keep boolean type/default). Delete stale copies from `build/resources/` if present.
- [ ] `IdentityProviderServiceIntTest` green (proves the entity↔column mapping after rename).
- [ ] Commit E1 (rename) as one commit: `732 Rename identity_provider.mcp -> mcp_embedded (embedded MCP surface flag)`.

---

## Phase E2 — Stand up EE `platform-user-api`; move the domain + event

### Task E2.1: Create EE `platform-user-api` module
**Files:** new `server/ee/libs/platform/platform-user/platform-user-api/build.gradle.kts`; register in `settings.gradle.kts` (near line 621, the EE platform-user block).
- [ ] `build.gradle.kts` deps: mirror what the moved domain needs (spring-data-commons/relational for `@Table`/`@Column`, jspecify, findbugs annotations, `commons-util`, and `platform-user-api` CE if the domain references CE `User`/`Authority` types — check imports).
- [ ] Register `include("server:ee:libs:platform:platform-user:platform-user-api")`.

### Task E2.2: Move the domain classes + event to EE
**Files:** `git mv` CE→EE, rewrite package `com.bytechef.platform.user.{domain,event}` → `com.bytechef.ee.platform.user.{domain,event}`, EE header, add `@version ee`:
- `.../domain/IdentityProvider.java`, `.../domain/IdentityProviderDomain.java`, `.../domain/IdentityProviderAuthorityMapping.java`
- `.../event/IdentityProviderChangedEvent.java`
- [ ] Fix any intra-move references (same package → no import; cross-package CE types like `User` keep CE imports).
- [ ] `./gradlew :server:ee:libs:platform:platform-user:platform-user-api:compileJava` green.
- [ ] Commit: `732 Move IdentityProvider domain + change event to EE platform-user-api`.

---

## Phase E3 — Stand up EE `platform-user-service`; move service + repo + changelogs

### Task E3.1: Create EE `platform-user-service` module
**Files:** new `server/ee/libs/platform/platform-user/platform-user-service/build.gradle.kts`; register in `settings.gradle.kts`.
- [ ] Deps: EE `platform-user-api` (E2), CE `platform-user-api` (for `User`/`Authority` if referenced), spring-data-jdbc, spring-context, `commons-util`, `tenant-api`; `@AutoConfiguration` + `@EnableJdbcRepositories(basePackages="com.bytechef.ee.platform.user.repository")` + `@ConditionalOnBean(AbstractJdbcConfiguration.class)` per the repo's "New Spring Data JDBC Modules" convention, registered in `META-INF/spring/...AutoConfiguration.imports`.
- [ ] Test deps mirror CE `platform-user-service` test set (testcontainers-postgres, liquibase-config, test-int-support).

### Task E3.2: Move service interface + impl + repository to EE
**Files:** `git mv` + package rewrite `com.bytechef.platform.user.{service,repository}` → `com.bytechef.ee.platform.user.{service,repository}`, EE header, `@version ee`, add `@ConditionalOnEEVersion` to `IdentityProviderServiceImpl`:
- `platform-user-api`: `.../service/IdentityProviderService.java`
- `platform-user-service`: `.../service/IdentityProviderServiceImpl.java`, `.../repository/IdentityProviderRepository.java`, test `.../service/IdentityProviderServiceIntTest.java`
- [ ] `IdentityProviderServiceImpl` publishes `IdentityProviderChangedEvent` (now EE) — keep the `ApplicationEventPublisher` wiring.
- [ ] `./gradlew :server:ee:libs:platform:platform-user:platform-user-service:compileJava` green.

### Task E3.3: Move the 5 (now 6, incl. rename) changelogs to EE — identical classpath path
**Files:** `git mv` each changelog from CE `platform-user-service/src/main/resources/config/liquibase/changelog/platform/user/` to EE `platform-user-service/src/main/resources/config/liquibase/changelog/platform/user/` (SAME relative path). Keep `id`/`author` unchanged.
- [ ] Delete stale copies from CE `build/resources/`.
- [ ] Confirm `master.xml` `includeAll` still resolves them on the classpath (EE module present) — no master.xml edit needed (path unchanged).
- [ ] `IdentityProviderServiceIntTest` (now EE) green — proves schema + mapping intact.
- [ ] Commit: `732 Move IdentityProvider service, repository, and changelogs to EE platform-user-service`.

---

## Phase E4 — Repoint EE consumers to the EE types

### Task E4.1: Update EE import references
**Files (verified consumers):** `IdentityProviderGraphQlController`; SCIM `ScimBearerTokenAuthenticationFilter` + `ScimSecurityConfiguration`; `McpTenantIssuerResolver` + `McpTenantIssuerResolverConfiguration` + `McpTenantIssuerCacheEvictionListener`; SSO-config `DynamicClientRegistrationRepository`, `DynamicRelyingPartyRegistrationRepository`, `SsoSaml2AuthenticationSuccessHandler`, `SsoSecurityConfiguration`, `SsoEnforcementFilter`, `SsoOAuth2AuthenticationSuccessHandler`; the EE MCP test configs (`McpOAuth2ResourceServerSecurityIntTestConfiguration`, `AutomationMcpOAuth2ResourceServerSecurityIntTestConfiguration`) and federation tests.
- [ ] Rewrite `import com.bytechef.platform.user.domain.IdentityProvider*` → `com.bytechef.ee.platform.user.domain.*`; `import com.bytechef.platform.user.service.IdentityProviderService` → `com.bytechef.ee.platform.user.service.IdentityProviderService`; `...event.IdentityProviderChangedEvent` → `com.bytechef.ee.platform.user.event.*`.
- [ ] Add the EE `platform-user-api`/`platform-user-service` gradle deps to each consuming module that doesn't already get them transitively.
- [ ] `./gradlew compileJava` (affected EE modules) green.
- [ ] Commit: `732 Repoint EE IdentityProvider consumers at the EE package`.

---

## Phase E5 — Relocate CE blocker #1: `SsoDiscoveryController`

### Task E5.1: Move `SsoDiscoveryController` to EE
**Files:** create EE REST home (either a new `server:ee:libs:platform:platform-user:platform-user-rest` module, or fold into an existing EE web module — pick per what already exposes EE REST); `git mv` `server/libs/platform/platform-user/platform-user-rest/.../SsoDiscoveryController.java` → EE, package `com.bytechef.ee.platform.user.web.rest`, EE header, `@version ee`, `@ConditionalOnEEVersion`; import the EE domain/service.
- [ ] Ensure the EE security chain permits this unauthenticated endpoint (it is a pre-login discovery call). Verify the path is in the permit-all matcher.
- [ ] Remove the now-dead CE dep on `IdentityProviderService` from `platform-user-rest` if nothing else uses it.
- [ ] `platform-user-rest` (CE) + the new EE module compile; any existing SsoDiscovery test moves with it and is green.
- [ ] Commit: `732 Move SsoDiscoveryController to EE`.

---

## Phase E6 — CE blocker #2: split `CustomOidcUserService` into CE-base (social) + EE (SSO/IdP)

Per E0.2 + user decision (2026-07-08): keep CE social login; the IdP-aware `CustomOidcUserService` lands in EE. Split into two sibling `OidcUserService`s.

### Task E6.1: CE base `SocialOidcUserService` for social login
**Files:** new CE `server/libs/config/security-config/.../oauth2/SocialOidcUserService.java`; edit `SingleTenantOAuth2LoginCustomizer.java`; optional CE helper for shared claim→user logic.
- [ ] `SocialOidcUserService extends OidcUserService` = current `CustomOidcUserService.loadUser` **minus** the `sso-` branch (always `autoProvision=true`, `defaultAuthority=ADMIN`); no `IdentityProvider`/`IdentityProviderService` import. Returns the (CE, unchanged) `CustomOidcUser`.
- [ ] Rewire `SingleTenantOAuth2LoginCustomizer` (CE social login) to inject `SocialOidcUserService` instead of `CustomOidcUserService`.
- [ ] Extract shared claim-extraction/`findOrCreateSocialUser`/authorities into a small CE helper if it reduces duplication with the EE class.
- [ ] `security-config` compiles with no `IdentityProvider*` reference; CE social-login behavior unchanged (unit test).

### Task E6.2: EE `CustomOidcUserService` (IdP-aware) for SSO
**Files:** new `server/ee/libs/config/security-sso-config/.../oauth2/CustomOidcUserService.java` (`@ConditionalOnEEVersion`, `@version ee`); edit EE `SsoSecurityConfiguration`; delete the old CE `CustomOidcUserService`.
- [ ] EE `CustomOidcUserService extends OidcUserService` = base provisioning + the `sso-` branch reading the EE `IdentityProviderService` (`autoProvision`/`defaultAuthority` from the `IdentityProvider`). Sibling of the CE base (not a subclass) so each chain injects its concrete type unambiguously.
- [ ] Rewire `SsoSecurityConfiguration` to inject the EE `CustomOidcUserService`.
- [ ] `git rm` the old CE `server/libs/config/security-config/.../oauth2/CustomOidcUserService.java`.
- [ ] `security-config` (CE) + `security-sso-config` (EE) compile; EE SSO login IntTests (if any) green.
- [ ] Commit: `732 Split OIDC user service: CE base for social login, EE CustomOidcUserService for SSO`.

---

## Phase E7 — Verification + edition reasoning

### Task E7.1: Whole-repo compile + affected tests
- [ ] `./gradlew compileJava` (whole repo) green.
- [ ] `./gradlew` `test`/`testIntegration` for: EE `platform-user-api`/`platform-user-service`, `platform-user-graphql`, `platform-user-scim`, `security-sso-config`, EE `platform-security-web-impl`, embedded MCP server, automation-security-web, and the CE-only smoke (`McpOAuth2ResourceServerCeOnlyIntTest`).
- [ ] `spotlessCheck`/`checkstyle`/`pmd`/`spotbugs` on every new/changed module.

### Task E7.2: Edition-reasoning greps
- [ ] No `com.bytechef.ee.platform.user` import in any CE module (`grep -rn "com.bytechef.ee.platform.user" server/libs --include=*.java | grep -v /build/` → empty).
- [ ] No CE main class references `IdentityProvider*` (the two blockers are gone; only javadoc prose may remain in `platform-security-web-impl`).
- [ ] Every moved EE file: EE header + `@version ee`; `@ConditionalOnEEVersion` on beans.

### Task E7.3: CE-only boot + EE upgrade checks
- [ ] Add/confirm a CE-only boot check: an app context with `bytechef.edition` not `ee` (or the EE modules absent) boots with no `IdentityProvider*` bean and no `identity_provider` table required. Prefer extending an existing CE server-app boot IntTest over a new one.
- [ ] EE upgrade check: on an EE profile, `DATABASECHANGELOG` shows the moved changesets as already-run (same `id`/`author`/`filepath`) — i.e. Liquibase runs only the new rename changeset, not the moved ones. Verify against a DB that already has the CE-era rows if available; otherwise assert the changelog `filepath` string is unchanged.
- [ ] Commit: `732 Verify identity_provider CE->EE move (compile, edition greps, boot)`.

### Task E7.4: Update specs + memory
- [ ] Set `docs/superpowers/specs/2026-07-08-identity-provider-to-ee-design.md` status → Implemented.
- [ ] Update the OAuth→CE spec's line about "`IdentityProvider` data model stays CE" to point here (superseded).
- [ ] Update memory (`project_mcp_oauth2_phase2_ee.md` / a new `identity_provider` memory).
- [ ] Commit.

## Self-review checklist
- CE-only build boots with local login + API-key + self-AS OAuth2, no `identity_provider` table, no `com.bytechef.ee.*` reference.
- EE build behavior byte-for-byte identical; SSO login + MCP federation unchanged.
- Liquibase: EE changelogs keep the same classpath path + `id`/`author`; EE upgrades don't re-run them; only the `mcp`→`mcp_embedded` rename changeset is new.
- No CE file references any EE `platform.user` type; `User`/`Authority`/local login stay CE.
- The `mcp`→`mcp_embedded` rename is complete across entity, resolver `Surface`, GraphQL, client, changelog, and tests.
