# Relocate `identity_provider` (SSO + MCP federation) CE→EE — Design

**Date:** 2026-07-08
**Status:** Implemented (2026-07-09). Entity (`IdentityProvider` + `IdentityProviderDomain` + `IdentityProviderAuthorityMapping`), `IdentityProviderChangedEvent`, `IdentityProviderService` (+ impl, repository, JDBC-repo autoconfig) and the 6 Liquibase changelogs relocated to new EE `platform-user-api` + `platform-user-service` modules (same classpath changelog path). All EE consumers repointed. `SsoDiscoveryController` moved to EE `security-sso-config`. `CustomOidcUserService` split: CE `SocialOidcUserService` (social login) + EE `CustomOidcUserService` (SSO/IdP). `server-app` wired for the EE service. Verified: full-repo compile (main+test); EE `IdentityProviderServiceIntTest` 4, EE security-web 10 unit + 15 int, embedded 18 + 7 int, automation 4 int, server-app boot 5; static analysis green on the new modules; no CE module references `com.bytechef.ee.platform.user`.
**Relates to:** `2026-07-08-mcp-oauth2-base-to-ce-design.md`. That work drew the line "the `IdentityProvider` *data model* is CE, only the *federation behavior* is EE." This reverses the data-model half: the whole entity becomes EE, collapsing the compromise into one rule.

## Decisions (locked)

1. **Relocate the entire `identity_provider` capability CE→EE.** All external-IdP configuration — SSO login *and* MCP federation — becomes enterprise-only. CE keeps local login + API-key + the ByteChef self-AS OAuth2.
2. **Rename the `mcp` field → `mcp_embedded`** (column `mcp` → `mcp_embedded`, `isMcp()`/`setMcp()` → `isMcpEmbedded()`/`setMcpEmbedded()`). Next to `mcpAutomation`/`mcpManagement`, a bare `mcp` misreads as a master switch; it is only the embedded MCP surface flag (`/api/embedded/{secret}/mcp`). Done in the same pass since the changelogs are moving anyway.

The resulting free/paid line: **CE = local login + API-key + ByteChef-AS OAuth2; EE = every external identity provider (bring-your-own SSO + bring-your-own MCP federation).**

## Why this tightens the CE/EE line

- The MCP-federation `McpTenantIssuerResolver` (EE) currently depends on the CE `IdentityProviderService` — an EE→CE edge. Once the entity + service are EE, that becomes EE→EE: one fewer cross-edition seam.
- The base CE MCP resource server (relocated in the prior plan) was deliberately built to never reference `IdentityProvider` (SPI + neutral `McpTenantTrustContext` seams). **Verified:** no CE base RS class imports `IdentityProvider`. So this move does not disturb that work.

## How MCP IdP support relates to existing SSO (the load-bearing finding)

Not a separate concept — **the MCP per-tenant IdP support is the existing SSO `IdentityProvider` entity, extended.** One record, two eras of fields:

- **SSO login (pre-existing):** `type` (OIDC/SAML), `clientId`, `clientSecret`, `scopes`, `issuerUri`, `metadataUri`, `signingCertificate`, `nameIdFormat`, `domains` (email-domain routing), `autoProvision`, `defaultAuthority`, `enforced`, `mfaMethod`/`mfaRequired`, `scimApiKey`.
- **MCP federation (added by T1.1):** `mcp`(→`mcp_embedded`), `mcpAutomation`, `mcpManagement` surface flags; `authoritiesClaim`; `authorityMappings` (child table); `validateMcpAudience`.

SSO login is an interactive authorization-code/SAML flow that JIT-creates a ByteChef user; MCP federation validates an IdP-minted Bearer JWT at the MCP API, is identity-only (no user, no session; tenant from the URL secret). They share the **trust anchor** (the vetted issuer) but use disjoint field sets. Enabling MCP for an org that already has SSO = ticking the `mcp*` boxes on the same provider record.

**Crucially, the interactive SSO login machinery is already EE** (`server/ee/libs/config/security-sso-config/`, `@ConditionalOnEEVersion`): `DynamicClientRegistrationRepository` (OIDC), `DynamicRelyingPartyRegistrationRepository` (SAML), `SsoSaml2AuthenticationSuccessHandler`, `SsoSecurityConfiguration`, `SsoEnforcementFilter`; plus the admin `IdentityProviderGraphQlController` and SCIM. So this move is a **data-model relocation**, not a login rework.

## Move set (travels to EE with the table)

- **Domain** (CE `platform-user-api` → EE `platform-user-api`): `IdentityProvider`, `IdentityProviderDomain`, `IdentityProviderAuthorityMapping`; the `IdentityProviderChangedEvent`.
- **Service** (CE `platform-user-service` → EE `platform-user-service`): `IdentityProviderService` (interface), `IdentityProviderServiceImpl`, `IdentityProviderRepository`, `IdentityProviderServiceIntTest`.
- **Schema** (5 Liquibase changelogs, CE `platform-user-service` resources → EE `platform-user-service` resources, **same classpath path** — see below).
- **EE consumers just update imports** (`com.bytechef.platform.user.*` → `com.bytechef.ee.platform.user.*`): `IdentityProviderGraphQlController`, SCIM filter/config, `McpTenantIssuerResolver`(+config), the SSO-config classes, and the EE MCP test configs.

## The two CE blockers (only real CE code dependencies)

Verified: the *only* CE main classes that import `IdentityProvider`/`IdentityProviderService` are these two (the two `platform-security-web-impl` matches are javadoc-only). Both are SSO-login features that belong in EE:

1. **`SsoDiscoveryController`** (CE `platform-user-rest`) — unauthenticated "which IdP handles this email domain?" endpoint for the login page. Genuinely SSO-only. **→ Relocate to an EE REST module** (user-confirmed 2026-07-08: SSO is an EE feature). It is a public endpoint, so the EE security chain must still permit it unauthenticated.
2. **`CustomOidcUserService`** (CE `security-config`) — OIDC JIT provisioning. **Correction (verified 2026-07-08):** it is NOT SSO-only. It is a CE `@Service` (`@ConditionalOnExpression("social-login.enabled or sso.enabled")`) injected by *two* chains: CE **social login** (`SingleTenantOAuth2LoginCustomizer`, `@ConditionalOnSingleTenant` + `social-login.enabled`) **and** EE SSO (`SsoSecurityConfiguration`). So it **cannot move to EE wholesale** — that would break CE social login (Google/GitHub), a CE feature. Its *only* `IdentityProvider` coupling is the `registrationId.startsWith("sso-")` branch (reads `autoProvision`/`defaultAuthority`); social login never enters it (it uses `autoProvision=true`, `defaultAuthority=ADMIN`).
   **Resolution (locked, user-chosen 2026-07-08 — "keep CE social login; CustomOidcUserService in EE"): split into two sibling `OidcUserService`s.**
   - **CE** gets a base `SocialOidcUserService extends OidcUserService` — the current provisioning logic *minus* the `sso-` branch (always `autoProvision=true`, `defaultAuthority=ADMIN`), no `IdentityProvider` import. Wired to CE social login (`SingleTenantOAuth2LoginCustomizer`). Google/OIDC social login keeps working on CE.
   - **EE** gets the IdP-aware `CustomOidcUserService extends OidcUserService` (`@ConditionalOnEEVersion`, in `security-sso-config`) — full provisioning + the `sso-` branch reading the EE `IdentityProviderService`. Wired to EE SSO (`SsoSecurityConfiguration`).
   - They are *siblings* (not sub/superclass) so each chain injects its own concrete type with no bean ambiguity on an EE build; shared claim-extraction goes in a small CE helper to avoid duplication.
   - `CustomOidcUser` (the returned `OidcUser`) has no `IdentityProvider` dependency and is returned by both paths → **stays CE**. `CustomOAuth2UserService` (GitHub/OAuth2 social login, no IdP dep) → **stays CE untouched**.
   This puts the IdP-aware OIDC service in EE (honoring "SSO is EE") while preserving CE Google/OIDC social login.

## Changelog relocation without breaking upgrades

`master.xml` includes the schema via `<includeAll path="classpath:config/liquibase/changelog/platform/user/" relativeToChangelogFile="false" errorIfMissingOrEmpty="false" .../>`. Liquibase keys a changeset by `id + author + filepath`.

- **Keep the identical classpath resource path** `config/liquibase/changelog/platform/user/` in the EE module and keep every `id`/`author` unchanged → the stored `filepath` matches → EE upgrades see the changesets as already-run (no re-run, no checksum break).
- **CE-only build:** the EE module is absent, so `includeAll` finds nothing there (`errorIfMissingOrEmpty="false"` → OK) and `identity_provider*` tables are simply not created — the intended end-state. Existing CE installs that already ran these changesets keep the (now-orphaned) tables; Liquibase does not drop or error on absent changesets.

The 5 changelogs: `20250208000000_*_identity_provider_tables`, `20260209000000_*_saml_scim_mfa_columns`, `20260707000000_*_identity_provider_mcp_column`, `20260708000000_*_validate_mcp_audience_column`, `20260708000001_*_mcp_surface_and_authority_mapping`. The rename adds a 6th changeset renaming the column `mcp` → `mcp_embedded`.

> **Caveat:** `20260209000000_*_saml_scim_mfa_columns` also adds SSO columns to the `user`/other tables? — a plan task must confirm each changelog touches *only* `identity_provider*` tables. Any changeset that also alters a CE-retained table (e.g. `user`) must be split so the CE part stays in CE.

## Edition reasoning to preserve

- **CE-only build:** no `identity_provider` table, no SSO login, no MCP federation; local login + API-key + self-AS OAuth2 all work. No `com.bytechef.ee.*` referenced.
- **EE build:** identical behavior to today — same entity, same SSO flows, same MCP federation; only the packages/module homes changed.

## Non-goals

- No functional change to SSO login or MCP federation — relocation + one column rename only.
- The MCP OAuth2→CE work (base RS + AS + API-key, already relocated) is untouched.
- No change to `User`/`Authority`/local-login, which stay in CE `platform-user-*`.

## Effort

Larger than a single-class move because it stands up EE `platform-user-api` + `platform-user-service` modules (mirroring the CE split) and relocates two login-adjacent CE classes — but the delicate part (SSO login wiring) is already EE, and the base MCP RS is already IdP-free. Task-per-concern with an explicit CE-only boot check (no SSO beans, no `identity_provider` table) and an EE upgrade check (changelog `filepath` unchanged).
