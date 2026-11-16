# Apps: Lifecycle, Instances, End-User Access and the App Portal - Design

## Summary

Apps today (`.agents/apps.md`, `2026-10-06-apps-governed-ai-built-apps-design.md`,
`2026-10-16-apps-api-platform-design.md`) give an externally hosted web application one secret key, a set of bindings
and a governed path to ByteChef. The "ByteChef Applications" architecture proposal (November 2026) asks for more: a
logical application with several configured instances, deployment history, a hosting-provider abstraction with Vercel
first, sign-in for the people who use an app, per-instance access policies and a portal where employees find their
apps.

This spec maps every concept of that proposal onto what the codebase already has, and designs only the gaps. The
result is one refactor and five additions:

| # | Piece | Kind |
|---|-------|------|
| 1 | Split today's `app` row into `app` (logical application) and `app_instance` (environment, key, bindings, URL) | refactor of unreleased schema and code |
| 2 | Instance configuration, the `AppInstance` resource type, `APP_USE` and `APP_DEPLOY` scopes, sharing and grants | addition on the existing RBAC |
| 3 | End-user sign-in: OIDC authorization code with PKCE on the existing authorization server, and the user token on `/api/app/v1` | addition |
| 4 | SDK `auth` and `context` modules, template sign-in, CLI and skill updates | addition |
| 5 | Versions, deployment history, the deployment-provider SPI and a Vercel provider | addition |
| 6 | The App Portal as a Studio page, favorites and usage | addition |

The implementation plan is `docs/superpowers/plans/2026-11-16-apps-lifecycle-portal-and-access.md`.

## Decisions (made with the user, 2026-11-16)

These are fixed. The design does not reopen them. The answers to the nine questions an earlier draft of this spec
left open are recorded in "Decisions on the open questions" near the end and are folded into every section.

1. **Reuse everything that exists.** A new concept is first mapped to an existing ByteChef mechanism. Something new is
   built only when nothing fits, and the spec says why it does not fit.
2. **App end users are Studio users.** The people who use an app are ordinary ByteChef users and workspace members.
   There is no separate end-user identity store.
3. **Workspaces are the grouping and org-unit model** for end-user access policies. No group or org-unit model is
   added.
4. **The App Portal is part of Studio** (the existing React client under `client/`), not a separate product or
   deployment.
5. **App users need roles and permissions, built on the existing RBAC:** permission scopes, `WorkspaceRole`, custom
   roles, `hasResourceScope`, `ResourceVisibilityProvider`, `ResourceOwnershipResolver`, resource visibility and
   `resource_grant`.

## Terms

The proposal and the codebase use two words differently. This spec uses the right-hand column.

| Proposal term | Closest existing ByteChef term | Term in this spec |
|---|---|---|
| Application | `Project` (logical, workspace-owned, has visibility) | **App** (`app` table) |
| ApplicationInstance | `ProjectDeployment` (one per environment, holds connections and enabled state) | **App instance** (`app_instance`) |
| ApplicationDeployment | nothing: a project deployment is updated in place and keeps no history | **App deployment** (`app_deployment`), one immutable row per release of code to an instance |
| ApplicationVersion | `ProjectVersion` (but that versions content ByteChef owns) | **App version** (`app_version`), a label for code ByteChef does not hold |
| Organizational unit | `Workspace` | **Workspace** |

Note the trap: a ByteChef *project deployment* is the proposal's *instance*, not its *deployment*.

## Current state (verified in this checkout)

Paths are relative to the repository root. `A` is `server/ee/libs/automation/automation-app`.

- **Schema.** `A/automation-app-service/src/main/resources/config/liquibase/changelog/automation/app/20261006000001_automation_app_init.xml`
  creates `app`, `app_connection` and `app_connection_operation`. One `app` row holds `workspace_id`, `environment`,
  `name`, `owner_user_id`, `status`, `visibility`, `hosting_target`, `url`, `last_deployed_date` and the secret key
  hash, hint and last-used date. It is application, instance and environment in one. Unique key:
  `(workspace_id, environment, name)`. Sibling files `20261007000001_automation_app_workflow.xml` (`app_workflow`,
  `app_workflow_run`) and `20261016000001_automation_app_api_collection.xml` (`app_api_collection`).
- **Enums.** `A/automation-app-api/.../domain/AppStatus.java` is `ENABLED, DISABLED`.
  `AppHostingTarget.java` is `VERCEL, SELF_HOSTED` and is a label only: nothing calls Vercel.
- **Release status.** `git log v0.36.0 -- A` and `git log upstream/master -- A` are both empty; the newest release tag
  is `v0.36.0` (2026-10-26). The app tables are in no release and not on `master`. The `app_id` changelog on the
  invocation log (`.../platform-external-invocation-log-service/.../external/invocation/log/20261114000001_external_invocation_log_added_app_id.xml`)
  is also not on `upstream/master`.
- **Admin surface.** `A/automation-app-api/.../facade/AppFacade.java` (implementation `AppFacadeImpl`, every method
  `@PreAuthorize`d), `AppWorkflowFacade`, `AppApiCollectionFacade`; GraphQL
  `A/automation-app-graphql/src/main/resources/graphql/app.graphqls`; management REST
  `A/automation-app-public-rest/openapi-management.yaml` under `/api/automation/v1/apps`.
- **App-facing surface.** `A/automation-app-public-rest/openapi.yaml` under `/api/app/v1`: connections, actions, tools,
  proxy, workflows, workflow runs, API collections and `/api/{path}`. Facades `AppExecutionFacadeImpl`,
  `AppWorkflowExecutionFacadeImpl`, `AppApiExecutionFacadeImpl`.
- **App authentication.** `server/ee/libs/automation/automation-security-web/automation-security-web-impl/.../security/web/app/AppApiKeySecurityConfigurer.java`
  and `AppApiKeyAuthenticationProvider.java`; principal `A/automation-app-api/.../security/AppPrincipal.java`
  = `(tenantId, appId, workspaceId, environment)`. The app is not a user.
- **RBAC plumbing for apps.** `AppPermissionScope { APP_VIEW, APP_MANAGE }` and `AppPermissionScopeProvider`
  (`APP_VIEW` from VIEWER, `APP_MANAGE` ADMIN only) in
  `server/ee/libs/automation/automation-configuration/automation-configuration-service/.../security/scope/`.
  `AppVisibilityPolicy` (PRIVATE and WORKSPACE, default WORKSPACE), `AppVisibilityProvider`, `AppOwnershipResolver`
  and `AppEnvironmentResolver` in `A/automation-app-service/.../security/`, all for resource type `"App"`.
  `AppFacadeImpl.getApps` filters through `ResourceVisibilityResolver.filterVisibleIds`, and `deleteApp` calls
  `resourceGrantService.deleteGrants("App", id)`. **There is no way to create a grant on an app:** no sharing facade,
  no GraphQL mutation. Visibility can only be set through `updateApp`.
- **The RBAC it plugs into.** `PermissionServiceImpl.hasResourceScope`
  (`server/ee/libs/automation/automation-configuration/automation-configuration-service/.../service/PermissionServiceImpl.java`)
  resolves the owner, treats visibility as a precondition, resolves the resource's environment and ends in
  `hasWorkspaceScope(workspaceId, scope, environment)`. Roles are per environment: `WorkspaceUser` has `environment`,
  `workspace_role` and `custom_role_id`. Custom role scopes are stored by name (`CustomRoleScope(String scope)`), and
  scopes are contributed per module through `PermissionScopeProvider`. Grants: `ResourceGrantService` and the
  `resource_grant` table in `server/ee/libs/platform/platform-resource-grant` (polymorphic `resource_type`,
  `resource_id`, `user_id`). Sharing facades to copy: `ProjectSharingFacade` and `WorkspaceConnectionSharingFacade`
  (`.../automation-configuration-api/.../facade/`). `resource_grant` is on this branch stack but not yet on
  `upstream/master`.
- **Member removal.** `WorkspaceUserRemovedEvent`
  (`server/libs/automation/automation-configuration/automation-configuration-api/.../event/`) is handled by
  `WorkspaceUserRemovalListener` (EE `automation-configuration-service/.../listener/`), which revokes connection
  grants and marks owned connections. It does nothing for apps.
- **Audit.** `A/automation-app-service/.../audit/AppAuditEvent.java` and `AppAuditPublisher.java` publish Spring
  `AuditApplicationEvent`s (`APP_CREATED`, `APP_DELETED`, `APP_ENABLED`, `APP_DISABLED`, `APP_SECRET_KEY_ROTATED`,
  binding events).
- **Invocation log.** App calls are recorded through `ExternalInvocationRecorder` with
  `ExternalInvocationSource.APP_ACTION|APP_TOOL|APP_PROXY|APP_WORKFLOW|APP_API`
  (`server/libs/platform/platform-external-invocation/platform-external-invocation-api`); the row has `app_id`,
  `workspace_id`, `environment`, and no user column. `.agents/apps.md` still calls these `ToolExecutionSurface` and
  `ToolExecutionRecorder`; those names are stale on this line.
- **Authorization server.** `server/libs/platform/platform-oauth2-authorization-server` (CE module, off by default:
  `bytechef.oauth2.authorization-server.enabled: false` in
  `server/apps/server-app/src/main/resources/config/application-bytechef.yml`).
  `Oauth2AuthorizationServerConfiguration.java` builds on `McpAuthorizationServerConfigurer`
  (`org.springaicommunity:mcp-authorization-server` 0.1.14) and has: authorization code with PKCE for public clients
  including refresh (`PublicClientRefreshTokenAuthenticationProvider`), a persistent signing key
  (`jwk/PersistentJwkSource`), tenant routing (`tenant/SessionTenantFilter`, tenant-keyed codes and refresh tokens,
  `TenantRoutingOAuth2AuthorizationService`), a consent page, a login redirect
  (`web/LoginRedirectAuthenticationEntryPoint`), grant listing and revocation
  (`service/OAuth2AuthorizationGrantService`, `facade/RegisteredClientFacade`), a revocation checker and audit events.
  Access tokens carry the tenant id and the user's authorities. The MCP-specific parts are: dynamic client
  registration limited to MCP scopes (`registration/McpClientRegistrationValidator`), the RFC 8707 audience customizer
  (`token/McpResourceAudienceTokenCustomizer`, skipped when the `openid` scope is present), and the resource-server
  filter `TenantAwareJwtAuthenticationFilter`
  (`server/libs/platform/platform-security-web/platform-security-web-impl/.../mcp/oauth2/`), which rejects a JWT on any
  path that is not an `McpEndpoint`. `RegisteredClientService` can list, fetch and delete clients; **it cannot create
  one** (clients arrive only through dynamic registration). Registered clients are stored once for all tenants
  (`tenant/SharedRegisteredClientRepository`). The two prerequisites the first Apps spec listed for sign-in
  (persistent signing keys, tenant resolution on the token endpoint) are now in place.
- **SDK.** `sdks/frontend/automation/app/library` is `@bytechef/app`. `src/core/config.ts` reads `BYTECHEF_URL` and
  `BYTECHEF_APP_KEY` and `assertServerRuntime()` throws when `window` exists. There is no auth module.
- **Template and skill.** `sdks/frontend/automation/app/create/template/AGENTS.md` rule 8 says there is no sign-in
  yet. `claude-code-plugin/bytechef-dev/skills/bytechef-app-builder/SKILL.md`.
- **CLI.** `cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/AutomationAppCommand.java`,
  `AutomationAppBindingCommand.java`, `AppLinkFile.java` (`.bytechef/app.json`), all DEVELOPMENT only.
- **Client.** `client/src/pages/automation/apps/` (`Apps.tsx`, `App.tsx` with Overview, Connections, API collections,
  Workflows and Activity tabs), routes in `client/src/routes.tsx` wrapped in `EEVersion`, the nav row in
  `client/src/App.tsx` shown only for `EditionType.EE`. Sharing UI to reuse:
  `client/src/shared/components/visibility/ResourceVisibilityPicker.tsx`, `ResourceVisibilityBadge.tsx`,
  `client/src/shared/hooks/useCanManageResourceSharing.ts`,
  `client/src/pages/automation/project/components/ProjectShareDialog.tsx`.
- **Nothing exists for:** versions, deployment history, a provider SPI, a Vercel component or connection
  (`server/libs/modules/components` has no `vercel`), favorites, usage counters, a portal.

### Could not verify

- Whether `McpAuthorizationServerConfigurer` 0.1.14 turns on Spring Authorization Server's OIDC support (ID tokens,
  `/userinfo`). The class references `/.well-known/openid-configuration`, and the ByteChef configuration adds a matcher
  for that path by hand, but I found no call to an OIDC configurer. The plan's first sign-in task settles it with a
  test.
- That a Spring Authorization Server access token issued without a `resource` parameter carries `aud` = the client id.
  This is the framework default as I know it; the plan pins it with a test before anything depends on it.
- `PermissionScopeProviderEditionGatingTest`, which `.agents/apps.md` names, is not in this checkout.
  `PermissionScopeRegistryTest` is.
- Vercel's current REST API and OIDC federation were not checked against Vercel's documentation.

## Gap map

| Proposal concept | Exists today | Reused as | Genuinely new |
|---|---|---|---|
| **Application** | The `app` row, `App`, `AppFacade`, admin UI | Stays the `app` table and the `"App"` resource type with its visibility, ownership and scopes | `category_id` (the platform `category` table that `Project.categoryId` uses), `icon`, `portal_discoverable`; the environment, URL, key and status move out |
| **Instance** | The same `app` row; conceptually `ProjectDeployment` | Every binding table, `AppPrincipal`, the key, `/api/app/v1`, the executor lane, the invocation log | The `app_instance` table and the `"AppInstance"` resource type |
| **Environment** | `Environment { DEVELOPMENT, STAGING, PRODUCTION }` (`platform-configuration-api`), per-environment workspace roles, `X-Environment` scoping in `AppEnvironmentScope` | As is: `app_instance.environment` | Nothing. No `ApplicationEnvironment` entity |
| **Version** | Nothing. `ProjectVersion` versions ByteChef-owned content and does not fit code ByteChef never holds | The idea of a per-parent version list | `app_version` |
| **Deployment** | `app.last_deployed_date` only | Audit publisher, `@Scheduled` per-tenant sweep pattern (`AppWorkflowRunRetentionJob`) | `app_deployment` (immutable), `app_instance.current_app_deployment_id` |
| **Configuration** | Nothing on apps. `ProjectDeploymentWorkflow.inputs` is a `MapWrapper` JSON column | The `MapWrapper` column pattern | `app_instance.configuration`, served to the app on a new `/api/app/v1/context` |
| **AccessPolicy** | Visibility and scopes on `"App"`; `resource_grant`; sharing facades for projects and connections | `ResourceVisibility`, `ResourceVisibilityProvider`, `ResourceOwnershipResolver`, `ResourceEnvironmentResolver`, `resource_grant`, per-environment roles, custom roles | Providers for `"AppInstance"`, `APP_USE`, `AppSharingFacade`, a member-removal listener. No policy table |
| **Integration** | `app_connection` (+ operations), `app_workflow`, `app_api_collection` | Unchanged apart from pointing at the instance | Nothing |
| **Favorite** | Nothing in server or client | - | `app_favorite` |
| **Usage** | `external_invocation_log` rows per app; OAuth grant timestamps per client | Invocation log for call volume; the throttled last-used `UPDATE` pattern of `touchSecretKeyLastUsedDate` | `user_id` and `app_instance_id` on the log; `app_instance_usage` for launches |
| **Provider SPI and Vercel** | `AppHostingTarget` label; connections hold third-party credentials; `ActionDefinitionFacade.executePerform` runs component actions | A connection holds the Vercel token; the provider runs component actions; `ConnectionUsageChecker` guards the connection | `AppDeploymentProvider` SPI, a `vercel` component, `VercelAppDeploymentProvider` |
| **Three identities, OIDC/PKCE** | App key and `AppPrincipal` (application and service); Studio login and EE SSO (user); the authorization server | The whole authorization server, its grant store, revocation, consent and admin page | App client registration, an access-check SPI at the authorization endpoint, the user token on `/api/app/v1` |
| **SDK** | `@bytechef/app` with workflows, API calls, actions, tools, proxy, errors | Kept, name included | `auth` and `context` modules; a TanStack Start session adapter |
| **App Portal** | Studio shell, routing, `EEVersion`, list pages | A Studio route and page | Portal query, page, favorites and recents |
| **APIs** | GraphQL for the admin UI, `/api/automation/v1/apps` management REST, `/api/app/v1` runtime | The three surfaces map one-to-one onto the proposal's Administration, Portal and Runtime categories | New operations on each; no new base path |
| **Lifecycle** | `AppStatus { ENABLED, DISABLED }`, 401 on a disabled app | The enforcement points in `AppApiKeyAuthenticationProvider` and the execution facades | Two status enums (below) |
| **Monitoring and audit** | `AppAuditPublisher`, invocation log, Activity tab, Micrometer counters wired through `ObjectProvider<MeterRegistry>` | All of it | New audit event names, deployment counters |

### Answers to the proposal's section 17

| Question | Answer |
|---|---|
| Does existing SSO support external OIDC clients? | Partly. The authorization server issues authorization-code-with-PKCE tokens to public clients and survives restarts and multiple tenants. It is wired for MCP clients only: clients can be created only by dynamic registration with MCP scopes, and JWTs are accepted only on MCP endpoints. OIDC ID tokens are unverified. It is off by default. |
| How are organizational units represented? | They are not. By decision 3 a workspace is the unit. |
| Can existing permissions be scoped to individual instances? | Yes. `hasResourceScope(id, type, scope)` is per resource, with visibility and `resource_grant` rows per resource id. An instance becomes a resource type. |
| How is application metadata stored? | One `app` row per application-and-environment, described above. |
| Can workflows and APIs be scoped to an instance? | Yes. Bindings are rows keyed by the app id today and resolve against `(workspaceId, environment)`; re-keyed to the instance they are per instance. |
| How is Vercel deployment authentication handled? | It is not. No ByteChef code talks to Vercel. |

## Design 1: Domain model

### The split

Today's `app` row becomes two tables. Because the schema is in no release and not on `master`, the three app changelog
files are **edited in place** to describe the final tables. No expand/contract, no data migration. A developer
database that already ran the old changesets must be reset
(`docker compose -f server/docker-compose.dev.infra.yml down -v`) or have the app tables and their
`databasechangelog` rows dropped by hand. Changelog edits go in their own commits, subject
`XXXX Upgrade - <description>`.

**`app`** (logical application; resource type `"App"`)

| Column | Notes |
|---|---|
| `id`, `uuid`, `name`, `description` | as today |
| `workspace_id` | now `NOT NULL` (closes the open item in `.agents/apps.md` about the unique key not biting on null) |
| `owner_user_id` | as today |
| `category_id` | nullable, references `category` (the table `Project.categoryId` uses) |
| `icon` | nullable `TEXT`; a serialized `FileEntry` of an image uploaded into ByteChef file storage (below). Not a URL |
| `status` | `AppStatus`: `DRAFT, ACTIVE, SUSPENDED, ARCHIVED` |
| `visibility` | as today: who sees the app in the admin registry |
| `portal_discoverable` | `BOOLEAN`, default false |
| audit columns, `version` | as today |

Unique: `(workspace_id, name)`. `environment`, `hosting_target`, `url`, `last_deployed_date` and the three
`secret_key_*` columns leave this table.

**`app_instance`** (configured installation; resource type `"AppInstance"`)

| Column | Notes |
|---|---|
| `id`, `uuid` | |
| `app_id` | FK to `app`, cascade delete |
| `workspace_id` | `NOT NULL`. The workspace whose connections, deployments and members the instance uses. It may differ from the app's workspace ("Instances in other workspaces" below) |
| `owner_user_id` | `NOT NULL`. The user who created the instance, not the app's owner. For the first instance, created together with the app, the two are the same person |
| `environment` | `Environment` ordinal |
| `name` | display name, for example "Croatia" |
| `status` | `AppInstanceStatus`: `ACTIVE, SUSPENDED, TERMINATED` |
| `visibility` | `ResourceVisibility` ordinal, default WORKSPACE: who may **use** the instance |
| `hosting_target`, `url` | moved from `app` |
| `secret_key_hash`, `secret_key_hint`, `secret_key_last_used_date` | moved from `app`; one key per instance, so instance secrets stay isolated |
| `user_auth_mode` | `NONE, OPTIONAL, REQUIRED` (Design 3). Column default `NONE`; the facade writes `REQUIRED` on create |
| `registered_client_id` | nullable; the OAuth2 client of this instance |
| `configuration` | `TEXT`, JSON through `MapWrapper`, capped at 64 KB |
| `deployment_connection_id`, `deployment_settings` | nullable; the provider connection and provider settings (Design 5) |
| `current_app_deployment_id` | nullable |
| audit columns, `version` | |

Unique: `(app_id, environment, name)`, `uuid`, `secret_key_hash`. Several instances of one app in one environment are
allowed and are told apart by name.

**Binding tables** keep their names and shapes; `app_id` becomes `app_instance_id` in `app_connection`, `app_workflow`,
`app_workflow_run` and `app_api_collection`. Binding keys stay unique per instance, so the same code runs against a
development and a production instance with different connections behind the same keys. This is the environment
promotion the first Apps spec listed as a non-goal, obtained without a promotion feature.

**Why this split and not a new parent above today's `app`.** Keeping `app` as the instance and adding an
`app_group` above it would touch less code, but the table called `app` would then mean "instance" while the UI, the
portal and the proposal say "application". The rename is free now, while nothing is released, and never again.

### Instances in other workspaces

One app may have instances in workspaces other than its own. This is how "CRM Croatia" and "CRM Germany" are
modelled: one app, two instances, each in its unit's workspace, each with that workspace's connections, deployments,
members and grants.

- **Creating one** requires `APP_MANAGE` on the app **and** `APP_MANAGE` in the target workspace, in the instance's
  environment. Neither alone is enough: the first protects the app's identity, the second protects the target
  workspace's connections and members.
- **Managing it** (bindings, key, configuration, sharing, sign-in, suspend, terminate) belongs to the instance's
  workspace: `AppInstanceOwnershipResolver` reports the instance's `workspace_id` and the instance's own
  `owner_user_id` (the member of that workspace who created it, not the app's owner), so every
  `hasPermission(#id, 'AppInstance', ...)` check resolves there. A grantee must be a member of the instance's
  workspace.
- **What the target workspace sees of the parent app:** a read-only summary carried on the instance
  (`AppInstanceDTO.app`: id, name, description, category, icon, status). Holding a scope on an instance grants nothing
  on the `"App"` resource: the target workspace's admin cannot rename, re-categorise, publish, suspend or delete the
  app, and does not see its other instances. The Apps page of a workspace lists its own apps and, separately, the
  instances it hosts of apps owned elsewhere.
- **What the owning workspace sees of a foreign instance:** a read-only row (name, workspace, environment, status,
  current version), no bindings, no key hint, no configuration. It keeps one lever: suspending or archiving the app
  stops every instance, wherever it lives.
- Deleting an app cascades to its instances in every workspace; the delete confirmation names them.

### The icon

The icon is an image uploaded into ByteChef file storage, not a URL, so the portal never loads images from hosts
ByteChef does not control. A new `AppFileStorage` in `automation-app-service` wraps the core `FileStorageService`
(`server/libs/core/file-storage/file-storage-api/.../service/FileStorageService.java`, obtained through
`FileStorageServiceRegistry`) in the way `KnowledgeBaseFileStorageImpl` and `SharedTemplateFileStorageImpl` wrap it,
with its own directory. `app.icon` holds the stored `FileEntry`. Upload is `APP_MANAGE` on the app, limited to PNG,
JPEG and WebP (SVG is refused: it can carry script) and 256 KB; replacing or deleting the app deletes the old file.
The image is served to signed-in Studio users who can see the app in the registry or the portal. Which existing
multipart upload controller to model the endpoint on was not checked and is left to the task.

### What follows from the split

- `AppPrincipal` becomes `(tenantId, appId, appInstanceId, workspaceId, environment, @Nullable AppUser user)`.
  `workspaceId` and `environment` are the instance's. Decision 2 of the first Apps spec holds: every credential type
  produces this one principal.
- `AppApiKeyAuthenticationProvider` looks the **instance** up by key hash. The key authenticates when the instance is
  `ACTIVE` and its app is `DRAFT` or `ACTIVE`; otherwise 401, and the execution facades keep their 403 re-check
  (`APP_DISABLED`, same code, so the SDK does not change).
- `external_invocation_log` gains `app_instance_id` (and `user_id`, Design 3) in a new changelog file beside
  `20261114000001_external_invocation_log_added_app_id.xml`. `app_id` stays so the Activity tab can show a whole app.
- Creating an app creates its first instance in the chosen environment in the same transaction, named after the
  environment. `automation app create` and the admin dialog therefore keep working as one step.
- `.bytechef/app.json` gains `instanceId`.

### Lifecycle

| App status | Key authenticates | In portal | Notes |
|---|---|---|---|
| `DRAFT` | yes | no | default on create |
| `ACTIVE` | yes | if discoverable | |
| `SUSPENDED` | no | no | every instance stops at once; replaces today's `DISABLED` |
| `ARCHIVED` | no | no | read-only in the admin UI |

| Instance status | Key authenticates | Notes |
|---|---|---|
| `ACTIVE` | yes, if the app allows it | |
| `SUSPENDED` | no | this instance only; user tokens for it are revoked |
| `TERMINATED` | no | bindings are removed, the OAuth2 client is deleted; the row stays for deployment history |

**Reshaped from the proposal:** its instance lifecycle also has `PROVISIONING`, `UPDATING` and `FAILED`. Those are
states of a deployment, not of an instance: a registered, externally deployed instance is never provisioned, and an
instance must be able to be suspended while a deployment is failing. They live on `AppDeploymentStatus` (Design 5) and
the UI derives "Updating" and "Failed" badges from the latest deployment.

Portal discoverability is the separate `portal_discoverable` flag, as the proposal requires.

## Design 2: Access model

### Two resource types, one mechanism

| Resource type | Visibility answers | Supported | Default |
|---|---|---|---|
| `"App"` (exists) | Who sees the app in the admin registry | PRIVATE, WORKSPACE | WORKSPACE |
| `"AppInstance"` (new) | Who may sign in to and use the instance | PRIVATE, WORKSPACE | WORKSPACE |

`"AppInstance"` gets the same four beans `"App"` has, in `A/automation-app-service/.../security/`:
`AppInstanceVisibilityPolicy`, `AppInstanceVisibilityProvider` (visibility and `created_by` of the instance row),
`AppInstanceOwnershipResolver` (owner = the instance's own `owner_user_id`, workspace = the instance's `workspace_id`)
and `AppInstanceEnvironmentResolver`.

**Why the instance has its own owner.** `PermissionServiceImpl.isResourceOwner` is true only when the current user is
the resolved owner **and** that owner is a member of the resolved workspace
(`workspaceUserRepository.existsByUserIdAndWorkspaceId`). With the app's owner as the instance's owner, an instance in
another workspace would have an owner who is usually not a member there, so nobody but a tenant admin would count as
owner, and a PRIVATE instance would be visible to nobody in the workspace that runs it. The creator of the instance is
by construction a member of the instance's workspace (creating it requires `APP_MANAGE` there).
 With those registered,
`hasPermission(#instanceId, 'AppInstance', 'APP_USE')` runs through `PermissionServiceImpl.hasResourceScope` with no
change to it: tenant admins pass; the instance must be visible to the caller (WORKSPACE, or creator, or a
`resource_grant` row); and the caller's role in the instance's workspace **and environment** must carry the scope.

The proposal's access-policy list maps as follows.

| Proposal | Here |
|---|---|
| Individual users | `PRIVATE` plus `resource_grant` rows of type `"AppInstance"` ("Specific people"). As for connections, a grant conveys visibility only; the grantee still needs `APP_USE` from a role, and must be a member of the instance's workspace |
| Teams, groups, organizational units | The workspace: `WORKSPACE` visibility. "CRM Croatia" is an instance used by the Croatia workspace |
| Role-based permissions | `APP_USE` on built-in or custom roles, per environment |
| Organization-wide | Not in the first release (decided). `hasResourceScope` lets a non-member through only on `_VIEW` scopes of a workspace-less ORGANIZATION resource; `APP_USE` on a workspace-owned instance does not fit that branch |

No `ApplicationAccessPolicy` table is built. Hidden-but-authorized works by construction: discoverability is a column
on `app`, authorization is the check above.

### Scopes

Existing scope names follow `<RESOURCE>_<VERB>` (`CONNECTION_VIEW`, `DEPLOYMENT_CREATE`, `PROJECT_PUBLISH`,
`WORKSPACE_MANAGE`). The app scopes after this work, all in `AppPermissionScope`:

| Scope | Minimum built-in role | Guards |
|---|---|---|
| `APP_VIEW` (exists) | VIEWER | The admin registry: list and read apps, instances, bindings, deployments |
| `APP_USE` (new) | VIEWER | Sign in to an instance, see it in the portal, launch it, be the user on `/api/app/v1` calls |
| `APP_DEPLOY` (new) | EDITOR | Create a version, record or trigger a deployment, roll back. Hands out no credentials |
| `APP_MANAGE` (exists) | ADMIN | Create, update, suspend and delete apps and instances; bind; rotate keys; upload the icon; edit configuration, sign-in settings and the provider connection. Creating an instance in another workspace needs it on the app and in that workspace |

`APP_MANAGE` is not split into `APP_CREATE`/`APP_EDIT`/`APP_DELETE`: all three hand the app credentials or remove
them, and the existing ADMIN-only rule (`.agents/apps.md`, "Admin side") already depends on one scope.

Assignment needs no new machinery. `AppPermissionScopeProvider` declares the two new `ScopeDefinition`s, the
`PermissionScopeRegistry` gives them to the built-in roles by rank, and because custom roles store scope names they
become selectable in the custom role editor. A custom role holding only `WORKSPACE_VIEW` and `APP_USE` is an "app
user": a member who can open apps and nothing else. Because roles are per environment, a member can be allowed
production instances and refused development ones.

### Sharing

New `AppSharingFacade` (`automation-app-api`, implementation in `automation-app-service`), a copy of
`ProjectSharingFacade` for both types:

- `setAppVisibility`, `grantAppAccess`, `revokeAppAccess`, `getAppGrants`
- `setAppInstanceVisibility`, `grantAppInstanceAccess`, `revokeAppInstanceAccess`, `getAppInstanceGrants`

**Who may share.** The four methods on an app, and the four on an instance, are guarded by **owner, or holder of
`APP_MANAGE` on that resource**:
`@permissionService.isResourceOwner('AppInstance', #appInstanceId) || hasPermission(#appInstanceId, 'AppInstance', 'APP_MANAGE')`
(and the same with `'App'`). A tenant admin passes both halves. This is deliberately wider than the owner-or-tenant-admin
rule of `ProjectSharingFacadeImpl` and the connection facade, for two reasons:

- `APP_MANAGE` is ADMIN-only and already lets its holder rotate the key, rebind connections, suspend and delete the
  instance. Refusing that person the sharing dialog protects nothing.
- It removes the dead end when the owner leaves: `isResourceOwner` stops being true for a creator who is no longer a
  member, and without the second half only a tenant admin could ever change who uses the instance.

`getAppGrants` and `getAppInstanceGrants` use the same guard, so a plain user of an instance still cannot learn who
else it was shared with.

For an instance in another workspace this gives the intended result: the target workspace's admin who created it is
its owner and can share it; the app's owner, who is not a member of that workspace, is neither owner (the membership
half of `isResourceOwner` fails, and they are not the instance's `owner_user_id` anyway) nor holder of `APP_MANAGE`
there, and cannot.

The grantee must be a member of the **instance's** workspace; the membership check makes a non-member and a
non-existent user fail identically. `updateApp` stops accepting `visibility`. Deleting an app or an instance deletes
its grants (`resource_id` has no foreign key). Events go through `AppAuditPublisher`:
`APP_VISIBILITY_CHANGED`, `APP_ACCESS_GRANTED`, `APP_ACCESS_REVOKED` and the three `APP_INSTANCE_*` equivalents.

### Member removal

New `AppWorkspaceUserRemovalListener` in `automation-app-service` on `WorkspaceUserRemovedEvent`, in the removing
transaction: revoke the user's `"App"` and `"AppInstance"` grants in that workspace
(`ResourceGrantService.revokeUserGrants`), delete their favorites and usage rows for its instances, and after commit
revoke their OAuth2 authorizations on the workspace's instance clients
(`OAuth2AuthorizationGrantService.revokeGrant(registeredClientId, principalName)`).

**When the owner of an app or an instance leaves its workspace.** `owner_user_id` is not rewritten, and nothing moves
to a pending state as connections do. The departed member no longer counts as owner (the membership half of
`isResourceOwner`). What is left depends on visibility:

- **WORKSPACE-visible** (the default): every workspace ADMIN holds `APP_MANAGE` on it and, by the sharing rule above,
  can manage and share it. Nothing is orphaned.
- **PRIVATE:** visibility is a precondition of `hasResourceScope`, and `ResourceVisibilityResolverImpl` lets through
  only a tenant admin, the creator and grantees. A workspace ADMIN therefore does not pass `APP_MANAGE` on it.
  Grantees keep using it; only a tenant admin can manage it, and can hand it back to the workspace by setting it to
  WORKSPACE or granting an admin. The listener does not widen visibility by itself, because that would silently
  enlarge who may use the instance. It publishes `APP_OWNER_LEFT` or `APP_INSTANCE_OWNER_LEFT` for each PRIVATE app or
  instance the removed member owned in that workspace, so a tenant admin can find them.

The app's owner leaving the app's workspace does not affect instances in other workspaces: each has its own owner.
Reassigning an owner stays deferred.

### CE and EE

Apps are EE. Every new bean is `@ConditionalOnEEVersion`, every new class under `server/ee/` carries the ByteChef
Enterprise header and `@version ee`. CE gets nothing: no scopes (the provider is EE), no portal nav row (the existing
`edition === EditionType.EE` test in `client/src/App.tsx`), no routes (`EEVersion`). The one CE-located change is in
the authorization server module, which gains an SPI and a client-registration method that have no caller in CE.

## Design 3: End-user sign-in and the three identities

### The identities

| Proposal identity | In ByteChef | Credential |
|---|---|---|
| User | A Studio user who is a workspace member | Access token from the authorization server |
| Application | The `app` and `app_instance` rows | None of its own; it is what the other two are checked against |
| Service | The app's server-side code | The instance's secret key (`AppPrincipal`), unchanged |

### Flow

The app's **server** runs the flow (backend for frontend). The browser never holds a token or the key.

1. The browser opens the app. The app's server finds no session and redirects to ByteChef's
   `/oauth2/authorize` with `client_id`, `redirect_uri`, `scope=openid profile app:use`, `state` and a PKCE challenge.
2. ByteChef has no session: `LoginRedirectAuthenticationEntryPoint` sends the user to the Studio login, EE SSO
   included. A user who came from the portal already has a session and sees nothing.
3. The authorization endpoint asks the new access check (below) whether this user may use this instance. If not, the
   request fails with `access_denied`. App clients are registered with consent off, so there is no consent screen
   (`McpAuthorizationConsentRequired` already honours the client setting).
4. The app's server exchanges the code at `/oauth2/token` with the PKCE verifier and stores the tokens in an
   encrypted, HttpOnly, SameSite=Lax session cookie.
5. Each call the app's server makes to `/api/app/v1` carries two credentials:
   `Authorization: Bearer <instance key>` (as today) and `X-ByteChef-User-Token: <access token>`.

### Client registration

One OAuth2 client per instance, created by `AppInstanceFacade` when sign-in is turned on and updated when the
instance URL changes: public client (`ClientAuthenticationMethod.NONE`), `requireProofKey(true)`, grant types
`authorization_code` and `refresh_token`, scopes `openid`, `profile`, `app:use`, consent off, redirect URI
`<instance url>/auth/callback` plus `http://localhost:<port>/auth/callback` entries for DEVELOPMENT instances only.
The client id is `app-<instance uuid>`, unique across tenants as the shared client store requires.

This needs `RegisteredClientService.registerClient(...)` and `updateClient(...)`, which do not exist. The scope
`app:use` follows the existing `mcp:automation` naming. Dynamic registration cannot mint an app client:
`McpClientRegistrationValidator` rejects any scope outside the MCP ones.

A public client is chosen over a confidential one because the secret a confidential client would need is a second
long-lived credential beside the instance key, and the token is useless without the key anyway (next section).

### Access check at the authorization endpoint

New SPI in `platform-oauth2-authorization-server`:
`OAuth2ClientAccessChecker { boolean supports(RegisteredClient); boolean isAllowed(RegisteredClient, Authentication) }`,
collected through `ObjectProvider#orderedStream` and applied as the authentication validator of
`OAuth2AuthorizationCodeRequestAuthenticationProvider`, where the configuration already post-processes that provider.
The module stays unaware of apps (a lower layer never depends on an upper one). `AppInstanceClientAccessChecker` in
`automation-app-service` implements it: resolve the instance from the client id, require instance `ACTIVE`, app
`DRAFT` or `ACTIVE`, and `permissionService.hasResourceScope(instanceId, "AppInstance", "APP_USE")`. At that point
the session user is the current principal, so the ordinary check applies.

### The user token on `/api/app/v1`

`AppApiKeyAuthenticationProvider` gains the user step after the key step:

1. No `X-ByteChef-User-Token`: `user` is null. If the instance's `user_auth_mode` is `REQUIRED`, 401
   `APP_USER_REQUIRED`.
2. Header present: decode with the authorization server's `JwtDecoder` (through `ObjectProvider`; absent server means
   401 `APP_USER_TOKEN_INVALID`). Require: valid signature and expiry, issuer = the server's, tenant claim = the key's
   tenant, `aud` contains this instance's `registered_client_id`, scope contains `app:use`, not revoked
   (`AuthorizationServerTokenRevocationChecker`). Any failure is 401 `APP_USER_TOKEN_INVALID`, also in `OPTIONAL`
   mode: a bad token is never downgraded to "no user".
3. Load the user by login and re-check `APP_USE` on the instance **as that user**, by running
   `permissionService.hasResourceScope` under `SecurityUtils.runAs(userAuthentication, ...)` (the helper exists in
   `SecurityUtils`). Failure is 403 `APP_USER_FORBIDDEN`. This per-call check is what makes a role change, a revoked
   grant or a narrowed visibility take effect on the next request rather than at token expiry.

The audience rule keeps the token families apart: an MCP token has an MCP resource as audience and fails step 2, and
an app token has no MCP audience and fails `McpAudienceValidator` on MCP endpoints. The token alone opens nothing on
`/api/app/v1` because the key is still required; the key alone cannot impersonate a user because the token is signed.

**What the user identity changes in this cut.** It is recorded (`external_invocation_log.user_id`,
`app_instance_id`), it gates the call (step 3), and the app can read it. It does **not** change what runs: actions
still run under `runAsSystem` on `appExecutor`, workflow jobs still run as the project deployment, bindings are still
the allow-list. Per-user connections and per-binding user rules are deferred.

### New app-facing endpoints

In `A/automation-app-public-rest/openapi.yaml`:

- `GET /context`: the app and instance (ids, names, environment), `configuration`, and the sign-in settings the SDK
  needs (`userAuthMode`, `issuer`, `clientId`). Key only.
- `GET /me`: the signed-in user (`id`, `login`, `firstName`, `lastName`, `email`), their workspace role name in the
  instance's environment and which of the app scopes they hold. That role name is all an app gets to branch on in
  this release; roles defined per app are deferred (decided). 401 `APP_USER_REQUIRED` without a user token. This is
  what the proposal's `auth.getCurrentUser()` calls.

Organizational unit and tenant are never read from the request: the tenant comes from the key, the workspace and
environment from the instance row, the user from the signed token.

### Revocation

Suspending or terminating an instance, and suspending or archiving an app, each call
`OAuth2AuthorizationGrantService.revokeGrants(registeredClientId)` for the affected clients. Rotating the instance key
does not touch user tokens. A tenant admin can
already inspect and revoke per user on the existing Registered Clients settings page
(`client/src/ee/pages/settings/platform/registered-clients/`), where app clients appear with the rest.

### Default mode and preconditions

A new instance is created with `user_auth_mode = REQUIRED` (decided). The column default is `NONE`, so a row that
does not come through `AppInstanceFacade.createAppInstance` keeps today's behaviour; since the schema is unreleased
there are no released rows to migrate, and a developer database that is kept instead of reset gets `NONE` for its
existing instances when its tables are reshaped by hand. An admin can still choose `NONE` or `OPTIONAL` explicitly at
create time.

`REQUIRED` on an instance that has no URL yet is allowed: the OAuth2 client is registered when the URL is set, and
until then every `/api/app/v1` call without a user token answers 401 `APP_USER_REQUIRED`, which is the safe side.

Sign-in needs the authorization server and a `bytechef.public-url`. The server is switched by
`bytechef.oauth2.authorization-server.enabled`. Where its value comes from (all decided):

| Where | Value | Why |
|---|---|---|
| `server/apps/server-app/src/main/resources/config/application-bytechef.yml` | `false` (unchanged) | The shared default of every boot that overrides nothing |
| `server/apps/server-app/src/main/resources/config/application-dev.yml` | `true` (new) | Local development, below |
| `server/apps/server-app/src/main/resources/config/application-prod.yml` | not touched | |
| EE deployment configuration | `true` (new files, below) | On by default in EE deployments once sign-in ships |

Never a Java field initializer, never `matchIfMissing` on `ConditionalOnOauth2AuthorizationServerEnabled`.

**The edition default is EE, not CE.** `application-bytechef.yml` has `edition: EE` under the comment
"default: EE". A stock boot is therefore an EE boot with the authorization server off. CE is a boot that sets
`BYTECHEF_EDITION=CE`; it inherits the same `false`, and must keep the authorization server off.

**Why the dev profile turns it on.** New instances default to `REQUIRED`. Without the dev setting a stock local
server would refuse `automation app create` (and the admin dialog's default) with `APP_USER_AUTH_UNAVAILABLE`, and the
one-step loop of the template would break. `./gradlew -p server/apps/server-app bootRun` runs under the `dev` profile:
`buildSrc/src/main/kotlin/com.bytechef.java-spring-boot-conventions.gradle.kts` sets `profiles = "dev,local"` unless
`-Pprod` is given, passes `--spring.profiles.active=$profiles` to every `BootRun` task and substitutes the
`#spring.profiles.active#` token of `application.yml` at resource processing; `application.yml` then expands the `dev`
group to `dev` and `api-docs`. `application-dev.yml` already sets `bytechef.public-url: http://127.0.0.1:5173`, which
becomes the issuer.

Other configurations, checked:

- **`testint`.** Integration tests run with `spring.profiles.active=testint`
  (`buildSrc/src/main/kotlin/com.bytechef.java-common-conventions.gradle.kts`), not `dev`.
  `server/apps/server-app/src/test/resources/config/application-testint.yml` does not set the property, and tests that
  need the server turn it on per test (`ServerApplicationIntTest` passes
  `bytechef.oauth2.authorization-server.enabled=true`; the authorization server module's IntTests do the same). No
  `testint` yml changes. Consequence: once the create default is `REQUIRED`, every Apps test that creates an instance
  through the facade either passes `NONE` explicitly or enables the server for that test.
- **Playwright.** `client/playwright.config.ts` drives the client dev server at `http://127.0.0.1:5173` against
  whatever backend is running; it has no backend configuration of its own, so nothing changes there. A dev-profile
  backend now has the server on. How the existing MCP OAuth2 specs under `client/test/playwright/tests/mcp/` get it
  on today was not checked.
- **The app template's dev instructions.** They need no server setting: `T/AGENTS.md` and `T/README.md` only point the
  app at `BYTECHEF_URL`. They do need one new sentence: on a local dev server the issuer is the dev `public-url`
  (`http://127.0.0.1:5173`, the Studio client dev server), so that client must be running for the sign-in redirect.
  The client's Vite proxy (`client/vite.config.mts`) already forwards `/.well-known`, `/oauth2/authorize`,
  `/oauth2/token`, `/oauth2/jwks`, `/oauth2/introspect`, `/oauth2/register` and `/oauth2/revoke` to the backend on
  port 9555, which covers every endpoint this flow uses. It does not forward `/userinfo`; the design reads the user
  from `GET /me` instead, so nothing is missing.

**EE deployment files.** Verified in this checkout: there is no EE-specific Helm values file and no EE-specific
compose file. `kubernetes/helm/` holds one chart, `kubernetes/helm/bytechef/` (`values.yaml` with an `env.normal` map
rendered by `templates/configmap.yaml`), and it has no edition key. Because the application default is EE, **a plain
Helm install is already an EE install with the authorization server off**: Apps work, and creating an instance with
the default `REQUIRED` mode is refused until the setting is supplied. The root `docker-compose.cluster.yml` is the
only compose file that sets the edition, and it defaults to CE (`BYTECHEF_EDITION: ${BYTECHEF_EDITION:-CE}` in
`x-bytechef-environment`); `docker-compose.yml`, `docker-compose.src.yml`, `server/docker-compose.dev.server.yml` and
`server/docker-compose.dev.infra.yml` set none, and `server/ee/` has no compose file. The EE default is carried by two
new files:

- `kubernetes/helm/bytechef/values-ee.yaml`, an overlay with only
  `env.normal.BYTECHEF_OAUTH2_AUTHORIZATION_SERVER_ENABLED: "true"`, used with `-f values-ee.yaml`. It does not set
  `BYTECHEF_EDITION`: that would repeat the application default. `values.yaml` is not changed.
- root `docker-compose.cluster.ee.yml`, an override that sets `BYTECHEF_EDITION: EE` (needed here, because the base
  file defaults to CE) and `BYTECHEF_OAUTH2_AUTHORIZATION_SERVER_ENABLED: "true"`, used as
  `docker compose -f docker-compose.cluster.yml -f docker-compose.cluster.ee.yml`.

With the server off, creating an instance with the default mode, or setting any mode but `NONE`, is refused with
`APP_USER_AUTH_UNAVAILABLE` and a message that names the property; creating with an explicit `NONE` works, and
everything else about Apps works as today.

## Design 4: SDK, template, CLI and skill

The proposal's `@bytechef/sdk` and its browser-side `new ByteChef({baseUrl: import.meta.env.VITE_BYTECHEF_URL})` are
**not adopted**. The package exists as `@bytechef/app`, is server-only by construction (`assertServerRuntime`), and the
bundle checks fail a build that puts `/api/app/v1` or the key name in client code. A browser-side client would have to
call ByteChef without the key, which this design never allows, and contradicts the proposal's own rule that generated
apps never receive privileged credentials.

| Proposal module | `@bytechef/app` |
|---|---|
| Auth | New `auth` on the core client: `createAuthorizationRequest()` (URL, state, PKCE verifier), `exchangeCode()`, `refresh()`, `revoke()`, `getCurrentUser()`. Reads `issuer` and `clientId` from `GET /context`, so no new environment variable for them |
| Workflows | Exists: `workflows.list/execute/getRun` |
| APIs | Exists: `api.list/call` |
| Files | Does not exist and no app-facing file endpoint exists. Deferred |
| Context | New `context.get()` on `GET /context` |
| Errors | Exists: `BytechefAppError`; new codes `APP_USER_REQUIRED`, `APP_USER_TOKEN_INVALID`, `APP_USER_FORBIDDEN` |

`createBytechefClient` accepts `userToken`; when set, every request adds `X-ByteChef-User-Token`. The TanStack Start
entry point gains `bytechefAuthMiddleware()` (reads the session cookie, refreshes when needed, puts a per-request
client with the user token on `context.bytechef` and the user on `context.user`, redirects to sign-in when the
instance requires it) and the three route handlers `login`, `callback`, `logout`. The cookie is sealed with
`BYTECHEF_SESSION_SECRET`, a server-only variable the scaffolder generates. No handler here is a `createServerFn` in
`./tanstack-start` (the rule in `.agents/apps.md`, "SDK").

Template: the three `/auth/*` routes, the middleware on the root route, `AGENTS.md` rule 8 rewritten (pages are
visible to signed-in users allowed on the instance; `context.user` is who), and the bundle check extended to fail when
the client bundle contains `BYTECHEF_SESSION_SECRET` or its value. The `bytechef-app-builder` skill and
`mcp-instructions.md` follow.

CLI (`cli/commands/automation`): `automation app create` writes `instanceId`; new
`automation app instance create|list`, `automation app deployment record|deploy|list`,
`automation app version create|list`.

**Outside DEVELOPMENT the CLI is read-only plus deploy (decided).** It may list and inspect apps, instances,
bindings, versions and deployments, create a version, and record or trigger a deployment. It must not create an
instance, mint or rotate a key, or change bindings, configuration, sharing or status.

The rule is enforced **on the server**, on the surface the CLI uses, so a modified CLI or a plain `curl` with an
automation API key is refused too. `AppEnvironmentScope` (the class that already scopes every management REST route
to `X-Environment` before any permission check) gains `requireDevelopment(environment)`, called by every mutating
operation of `openapi-management.yaml` except version create and the deployment operations. Outside DEVELOPMENT those
answer 403 `APP_OPERATION_DEVELOPMENT_ONLY`. The CLI keeps its own early refusal for a clear message without a round
trip. Studio (GraphQL, session) is not restricted: a production instance is created, and its key shown once, in the
admin UI by a holder of `APP_MANAGE`.

## Design 5: Versions, deployments and the provider SPI

### Tables (new changelog file `20261116000001_automation_app_deployment.xml`)

**`app_version`**: `id`, `app_id` (FK, cascade), `name` (for example `1.1` or a short commit hash), `git_ref`,
`git_commit_sha`, `description`, created columns. Unique `(app_id, name)`.

**`app_deployment`**: `id`, `app_instance_id` (FK, cascade), `app_version_id` (FK), `provider` (`VERCEL`,
`EXTERNAL`), `provider_deployment_id`, `url`, `status` (`AppDeploymentStatus`: `QUEUED, BUILDING, READY, FAILED,
CANCELED`), `error_message`, `metadata` (JSON), `created_by`, `created_date`, `finished_date`. Rows are never updated
after they reach a terminal status and never deleted except with their instance. `app_instance.current_app_deployment_id`
points at the latest `READY` one. `app.last_deployed_date` is gone; the date is the current deployment's.

### Registration versus deployment

- **Register** (works today, stays the default): create the app and an instance with `hosting_target` and `url`.
  ByteChef calls nobody. Whoever deploys the code may **record** it: `AppDeploymentFacade.recordDeployment(instanceId,
  versionName, url, gitCommitSha)` writes a `READY` row with provider `EXTERNAL`. This is what the CLI's
  `deployment record` calls from CI, and what keeps history for self-hosted apps.
- **Deploy**: `AppDeploymentFacade.deploy(instanceId, versionId)` asks the instance's provider to build and release
  that version, writes a `QUEUED` row and returns. Needs `APP_DEPLOY` on the instance.
- **Roll back**: a deploy of an earlier version. It writes a new row; history is never rewritten.

### SPI

In `automation-app-api`, package `com.bytechef.ee.automation.app.deployment`:

```java
public interface AppDeploymentProvider {

    AppHostingTarget getHostingTarget();

    ProviderDeployment deploy(ProviderDeploymentRequest request);

    ProviderDeployment getDeployment(ProviderDeploymentRef ref);

    void cancel(ProviderDeploymentRef ref);
}
```

`ProviderDeploymentRequest` carries the instance's `deployment_connection_id` and `deployment_settings`, the version's
git reference and commit, and the environment. `ProviderDeployment` carries the provider's id, status and URL.

**Reshaped from the proposal's interface:** `getStatus` and `getDeployment` are one call (the status is a field of
the deployment); `delete` is replaced by `cancel`, because a deployment record is history and deleting a live release
at the provider is a different, destructive operation nobody has asked for; requests are typed records, not a bare
id, because a provider needs the connection to authenticate.

`AppDeploymentFacadeImpl` collects providers through `ObjectProvider#orderedStream` and picks by hosting target.
`SELF_HOSTED` has no provider: `deploy` answers 409 `APP_DEPLOYMENT_PROVIDER_UNAVAILABLE` and `recordDeployment` is
the only path.

Status is followed by `AppDeploymentStatusJob`, a `@Scheduled` per-tenant sweep over non-terminal rows modelled on
`AppWorkflowRunRetentionJob`, calling `getDeployment` and stopping after a configurable deadline
(`bytechef.app.deployment.*` in `ApplicationProperties.App`, defaults in `application-bytechef.yml`). Provider calls
do I/O, so the sweep hands each one to `appExecutor`, never the default executor. A Vercel webhook that replaces
polling is deferred.

### Layering and module placement

```
automation-app-api            AppDeploymentProvider SPI, records, AppDeploymentFacade
automation-app-service        AppDeploymentFacadeImpl, AppDeploymentService, AppVersionService, the job
automation-app-deployment/
  automation-app-deployment-vercel   VercelAppDeploymentProvider  -> depends on automation-app-api only
server/libs/modules/components/vercel   the vercel component (CE location, like every component)
```

The provider module depends on `automation-app-api` and platform APIs, never on `automation-app-service`. The facade
calls services and the SPI; services do not call each other; nothing in a facade touches a repository.

### Vercel

There is no Vercel code to reuse, so authentication is built on the mechanism ByteChef already has for third-party
credentials: **a connection**. A new `vercel` component defines the connection (access token; OAuth integration
later) and four actions: list projects, create a deployment from a git reference, get a deployment, cancel a
deployment. `VercelAppDeploymentProvider` runs those actions with `ActionDefinitionFacade.executePerform`, the call
`AppExecutionFacadeImpl` already makes, against `app_instance.deployment_connection_id`. This buys encryption at
rest, connection visibility, the `CONNECTION_EDIT` check when an admin attaches the connection (the same rule as
binding, since the connection is handed to code outside a workflow), token refresh, and the Vercel actions in ordinary
workflows. A new `ConnectionUsageChecker` reports instances that use a connection for deployment, so it cannot be
deleted from under them.

`deployment_settings` for Vercel: project id, team id, the git repository and the Vercel target (`production` or
`preview`, derived from the instance environment by default). Git-based deployment only.

In this cut the provider does **not** write `BYTECHEF_APP_KEY` into Vercel. The key is shown once at create or
rotate, and the admin sets it in Vercel, as today. Pushing environment variables and keyless authentication through
Vercel's OIDC issuer (Phase 2 of the first Apps spec) stay deferred.

### Addendum, 2026-11-16: a deploy hook provider for `SELF_HOSTED`

The text above says `SELF_HOSTED` has no provider. On 2026-11-16 a deploy hook provider was added for it at the product
owner's request. For an instance with hosting target `SELF_HOSTED`, ByteChef POSTs the version to an HTTP(S) endpoint the
customer runs (https is required outside the development environment) (through the HTTP Client component and the instance's deployment connection), polls its status URL and can
cancel it; a reference agent for one server ships in the app template. Its deployments carry the provider type
`DEPLOY_HOOK`, appended to the persisted enum. The protocol, settings keys, URL rules and the agent are described in
`.agents/apps.md`, section **Provider-driven deployments**.

Two additions to the SPI were made during implementation. `ProviderDeployment` carries a nullable `errorMessage`, the
provider's failure reason, which is sanitized and bounded before it is stored. `AppDeploymentProvider` gained
`validateDeploymentSettings(settings, environment)`, called when an instance's deployment settings are saved, and
`getDeploymentConnectionComponentName()`, the component a deployment connection must belong to.

## Design 6: The App Portal in Studio

### Server

`AppPortalFacade` (`automation-app-api`, impl in `-service`), GraphQL only (session-authenticated, like the rest of
Studio), in `app.graphqls`:

- `portalApps(environmentId: ID!): [PortalApp!]!`: for the current user, across every workspace they belong to
  (`WorkspaceFacade.getUserWorkspaces`), the apps that are `ACTIVE` and `portal_discoverable` and have at least one
  `ACTIVE` instance in that environment the user may use. "May use" is exactly the Design 2 check, computed in bulk:
  `ResourceVisibilityResolver.filterVisibleIds("AppInstance", workspaceId, ...)` per workspace, then
  `permissionService.hasWorkspaceScope(workspaceId, "APP_USE", environment)` once per workspace. The portal has no
  authorization logic of its own. Each `PortalApp` carries name, description, category, icon, `favorite`,
  `lastUsedDate` and its usable `instances` (id, name, workspace name, url).
- `setAppFavorite(appId: ID!, favorite: Boolean!)`, guarded by "the app is in the caller's `portalApps`".
- `recordAppLaunch(appInstanceId: ID!)`, guarded by `APP_USE` on the instance.

**`app_favorite`** (`user_id`, `app_id`, `created_date`; primary key both) and **`app_instance_usage`** (`user_id`,
`app_instance_id`, `last_used_date`, `use_count`; primary key both), in
`20261116000002_automation_app_portal.xml`. Both cascade with their app or instance and reference `user`.

Favorites are a table and not browser storage because they must follow the user across devices. Usage is a table and
not a query over OAuth grants because the grant store exists only when the authorization server is on, lives in
Spring's own tenant-routed tables, and knows nothing about instances that do not use sign-in. `app_instance_usage` is
written on `recordAppLaunch` and, throttled like `touchSecretKeyLastUsedDate`, on `GET /me`.

### Client

- Route `/automation/portal` in `client/src/routes.tsx`, wrapped as the Apps routes are (`PrivateRoute`, `EEVersion`,
  `LazyLoadWrapper`); nav row "App Portal" in `client/src/App.tsx` under the same EE test as Apps.
- `client/src/pages/automation/portal/Portal.tsx`: search box, category filter, a Favorites section, a Recently used
  section, then all apps. Cards reuse the list and empty-state patterns of `client/src/pages/automation/apps/`.
- One card per app. One usable instance: the card opens it. Several: the card opens a small instance selector (name
  and workspace). This is the proposal's preferred default.
- Launch calls `recordAppLaunch` and opens the instance URL in a new tab. The user's Studio session makes the app's
  sign-in redirect silent.
- The portal follows Studio's current environment, as the Apps page does (`apps(workspaceId, environmentId)`).
- Admin side: the app dialog gains category, an icon upload and a "Show in App Portal" switch; the app detail page gains
  Instances and Deployments tabs, and each instance a Sharing dialog built from `ResourceVisibilityPicker` and the
  pattern of `ProjectShareDialog`.

The portal is not on any app's execution path: an app talks to `/api/app/v1` and `/oauth2/*` only, so a broken portal
page stops no app. The proposal's "independently deployable" requirement is replaced by decision 4.

## APIs

The proposal's paths (`/api/applications`, `/api/application-instances`, `/api/portal/applications`) are **not
adopted**; its three categories already exist under established paths and authentication.

| Proposal category | ByteChef surface | Authentication | Additions |
|---|---|---|---|
| Administration | GraphQL `app.graphqls` (Studio) and REST `/api/automation/v1/apps` (`openapi-management.yaml`, CLI; mutations other than versions and deployments are DEVELOPMENT only) | Session; automation API key | Instances, sharing, versions, deployments. REST: `/apps/{id}/instances`, `/app-instances/{id}`, `/app-instances/{id}/deployments`, `/apps/{id}/versions`; bindings move under `/app-instances/{id}/...` |
| Portal | GraphQL `portalApps` and the two mutations | Session | All new |
| Runtime | REST `/api/app/v1` (`openapi.yaml`) | Instance key, plus user token | `GET /context`, `GET /me` |

Management endpoints are never reachable with an instance key: `AppApiKeyFilterChainTest` already pins that each key
type is 401 on the other's paths.

## Monitoring and audit

- **Audit** through `AppAuditPublisher`, new `AppAuditEvent` names: `APP_INSTANCE_CREATED`, `APP_INSTANCE_UPDATED`,
  `APP_INSTANCE_SUSPENDED`, `APP_INSTANCE_ACTIVATED`, `APP_INSTANCE_TERMINATED`, `APP_SUSPENDED`, `APP_ACTIVATED`,
  `APP_ARCHIVED`, `APP_PUBLISHED_TO_PORTAL`, `APP_UNPUBLISHED_FROM_PORTAL`, the six sharing events,
  `APP_DEPLOYMENT_INITIATED`, `APP_DEPLOYMENT_COMPLETED`, `APP_DEPLOYMENT_FAILED`, `APP_USER_AUTH_CHANGED`.
  `APP_ENABLED` and `APP_DISABLED` are removed with the statuses they described. Sign-in successes and failures are
  already audited by the authorization server's own events.
- **Metrics:** `bytechef_app_deployment` (counter; tags `provider`, `status`) and `bytechef_app_user_token_rejected`
  (counter; tag `reason`), wired through `ObjectProvider<MeterRegistry>` like `bytechef_connection_create`. No app or
  workspace id as a tag.
- **Admin UI:** the Deployments tab is the deployment history; the instance header shows current version, last
  deployment and its status; the Activity tab gains a user column.
- Availability probes, launch analytics beyond a counter and active-user charts are deferred.

## Error handling summary

New `AppErrorType` entries continue the existing numbering after 128; `AppErrorTypeTest` pins them.

| Code | Status | When |
|---|---|---|
| `APP_INSTANCE_NOT_FOUND` | 404 | unknown instance, or one outside the request's environment |
| `APP_INSTANCE_NAME_ALREADY_EXISTS` | 409 | `(app, environment, name)` taken |
| `APP_USER_REQUIRED` | 401 | no user token on an instance that requires one, or on `/me` |
| `APP_USER_TOKEN_INVALID` | 401 | any token validation failure |
| `APP_USER_FORBIDDEN` | 403 | valid token, user lacks `APP_USE` on the instance |
| `APP_USER_AUTH_UNAVAILABLE` | 409 | enabling sign-in while the authorization server is off or the instance has no URL |
| `APP_VERSION_NOT_FOUND`, `APP_DEPLOYMENT_NOT_FOUND` | 404 | |
| `APP_DEPLOYMENT_PROVIDER_UNAVAILABLE` | 409 | no provider for the hosting target, or no deployment connection |
| `APP_DEPLOYMENT_IN_PROGRESS` | 409 | a non-terminal deployment exists for the instance |
| `APP_OPERATION_DEVELOPMENT_ONLY` | 403 | a management REST mutation other than version create or a deployment operation, outside DEVELOPMENT |
| `APP_ICON_INVALID` | 400 | icon of an unsupported type or over the size limit |

`AbstractAppApiController.toAppStatus` stays the single mapping.

## Testing

- Unit tests end in `Test`, integration tests in `IntTest`; controller tests are WebMvc IntTests; one test class per
  source class; facade authorization tests follow `AppFacadeAuthorizationTest`; GraphQL controllers keep the
  no-`@PreAuthorize` reflection check of `AppWorkflowGraphQlControllerTest`.
- The split is covered by adapting the existing Apps suites, not by new ones: every existing test must pass against
  the instance model, and `AppApiKeyFilterChainTest` gains "suspended app stops every instance" and "suspending one
  instance leaves its sibling working".
- Ownership: the target workspace's admin who created a cross-workspace instance can share it and the app's owner,
  not a member there, cannot; a workspace ADMIN who is not the owner can share a WORKSPACE-visible instance; after
  the owner leaves, a WORKSPACE-visible instance stays manageable by a workspace ADMIN and a PRIVATE one only by a
  tenant admin.
- Cross-workspace: creating an instance in another workspace is refused with only one of the two `APP_MANAGE`s; the
  target workspace's admin can manage the instance and cannot change the app; the owning workspace sees the foreign
  instance without bindings or key hint.
- CLI rule: WebMvc IntTests prove the management REST refuses instance create, key rotation and binding changes in
  STAGING and PRODUCTION and allows version create and deployment record there, independent of the CLI.
- Access: a `PermissionServiceVisibilityTest`-style case for `"AppInstance"` proving that a workspace member with
  `APP_USE` is refused a PRIVATE instance without a grant; a per-environment role case.
- Sign-in: an IntTest that runs a full authorization-code-with-PKCE flow for an app client and for a user without
  `APP_USE`; token replay tests in both directions (MCP token on `/api/app/v1`, app token on an MCP endpoint); a test
  per token rejection reason; "grant revoked between two calls" answers 403 on the second.
- Provider: `AppDeploymentFacadeTest` with a fake provider; `VercelAppDeploymentProviderTest` against mocked action
  results; the component's generated definition snapshot.
- Portal: `AppPortalFacadeTest` for the filter matrix (discoverable, status, visibility, scope, environment); client
  tests for the page and the selector.
- SDK: `mockFetch` tests for `auth` and `context`; `check:template` extended with a mock issuer.

## Out of scope and deferred

- **Organization-wide instance access** (decided: deferred). The first release is workspace-scoped plus
  specific-people grants. `AppInstanceVisibilityPolicy` does not list `ORGANIZATION`.
- **Roles defined per app** (decided: not yet). "May use the instance" plus the workspace role name from `/me` is the
  whole model for now.
- **An API for customers' own employee portals** (decided: later, authenticated with the user's automation API key,
  which already authenticates as that user).
- Per-user connections, and per-binding rules keyed on the signed-in user's role.
- A `Files` SDK module and app-facing file endpoints.
- Creating instances, minting or rotating keys, and changing bindings from the CLI or management REST outside
  DEVELOPMENT.
- Providers other than Vercel; a Vercel webhook; pushing environment variables to Vercel; Vercel OIDC keyless
  authentication; OAuth-based Vercel connection.
- Availability probes of app URLs (ByteChef calling arbitrary URLs needs its own SSRF review).
- Analytics dashboards, recommendations, announcements, app templates beyond the existing one.
- Distributed-EE deployment: Apps stay `server-app` only.
- Rate limits and quotas per app or per user.
- Ownership reassignment for apps whose owner left the workspace.

## Decisions on the open questions

Answered by the user on 2026-11-16. Each is reflected in the section named.

1. **Instances in other workspaces: yes.** One app may have instances in other workspaces. Creating one requires
   `APP_MANAGE` on the app and `APP_MANAGE` in the target workspace. The target workspace's admin sees the parent app
   read-only through the instance and gains nothing on the app record. (Design 1, "Instances in other workspaces".)
2. **`DRAFT` restricts the portal only.** (Design 1, "Lifecycle".)
3. **Organization-wide use: deferred.** Workspace scope plus specific-people grants in the first release.
   (Design 2; deferred list.)
4. **In-app roles: not yet.** `/me` returns the workspace role name; per-app roles are deferred. (Design 3.)
5. **CLI outside DEVELOPMENT: read-only plus deploy,** enforced on the server. No instance creation, no key minting or
   rotation. (Design 4.)
6. **External employee-portal API: deferred,** later with the user's automation API key. (Deferred list.)
7. **`user_auth_mode` defaults to `REQUIRED` for new instances;** rows that already exist are `NONE`. (Design 3,
   "Default mode and preconditions".)
8. **The authorization server is on by default in EE deployments once sign-in ships.** The shared
   `application-bytechef.yml` keeps `bytechef.oauth2.authorization-server.enabled: false`; the EE Helm overlay
   `kubernetes/helm/bytechef/values-ee.yaml` and the compose override `docker-compose.cluster.ee.yml` set it to `true`.
   Not in code. (Design 3, "Default mode and preconditions".)
9. **The icon is uploaded into ByteChef file storage** through the existing `FileStorageService`. (Design 1, "The
   icon".)

10. **The dev profile turns the authorization server on.** `application-dev.yml` gains
    `bytechef.oauth2.authorization-server.enabled: true`, so `automation app create` stays one step on a local server
    now that new instances default to `REQUIRED`. `application-bytechef.yml` stays `false`; `application-prod.yml` is
    not touched. (Design 3, "Default mode and preconditions".)

### Remaining question

Since the application's default edition is EE and `kubernetes/helm/bytechef/values.yaml` sets no edition, a plain
Helm install is already EE and will have the authorization server off unless `values-ee.yaml` is added. **Should the
chart's own `values.yaml` carry `BYTECHEF_OAUTH2_AUTHORIZATION_SERVER_ENABLED: "true"` instead of (or as well as) the
`values-ee.yaml` overlay?**

## Proposal items reshaped or rejected

| Proposal | Here | Why |
|---|---|---|
| App Portal as a separately deployable product | A Studio page | Decision 4 |
| Organizational units, teams, groups in access policies | Workspaces | Decision 3 |
| `ApplicationAccessPolicy`, `ApplicationEnvironment`, `ApplicationIntegration`, `ApplicationConfiguration` as entities | Visibility plus grants; the `Environment` enum; the binding tables; a column | Each already exists in another form |
| `@bytechef/sdk` constructed in the browser with a `VITE_` URL | `@bytechef/app`, server-only, plus `auth` and `context` | Existing package; a browser client contradicts the proposal's own secret rule |
| `/api/applications...` and `/api/portal/...` | GraphQL, `/api/automation/v1/apps`, `/api/app/v1` | Existing conventions and security chains |
| Instance statuses `PROVISIONING`, `UPDATING`, `FAILED` | Deployment statuses | They describe a deployment |
| `ApplicationDeploymentProvider.delete` and separate `getStatus` | `cancel`; one `getDeployment` | History is immutable; status is a field |
| A new Vercel account-linking mechanism | A ByteChef connection of a new `vercel` component | Credentials already have a governed home |
| "Service" identity as something new | The existing instance key | It already is the service identity |
