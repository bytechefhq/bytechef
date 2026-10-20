# Apps — Governed AI-Built Apps — Design

## Summary

An **App** is a web application someone built with an AI coding tool (Claude Code, Cursor, Codex, Copilot, Windsurf)
that runs on a host ByteChef does not own — Vercel first, or the customer's own infrastructure — and reaches every
third-party system it needs **through ByteChef**. ByteChef holds the credentials, decides what the app may do, records
everything it does, and can switch it off. The app holds one secret key that is useless anywhere but ByteChef.

The reference point is Tray.ai's Helix ("governed runtime for AI-built apps"). Helix governs by being the runtime.
ByteChef deliberately does not host apps; it governs by being the app's **only path to data**: component actions,
component tools, and a raw HTTP proxy over a bound connection. Disabling the app or rotating its key stops all three
at once, wherever the app runs.

This spec covers four sub-projects, each with its own implementation plan, built in order:

| # | Sub-project | Delivers |
|---|-------------|----------|
| A | Server | `App` entity, bindings and allow-lists, app secret key, public action / tool / proxy endpoints, invocation logging, admin UI |
| A4 | Workflow bindings | Deployed Call-workflow workflows bound to an app and run through `/api/app/v1`; see `2026-10-06-apps-workflows-and-sdk-design.md` |
| B | SDK | `@bytechef/app`, a framework-agnostic, server-only client with `/tanstack-start` and `/ai-sdk` subpath adapters; see `2026-10-06-apps-workflows-and-sdk-design.md` |
| C | Template | `npm create bytechef-app` — a TanStack Start starter with `AGENTS.md` |
| D | Coding-agent loop | The management MCP builds and deploys workflows; the `bytechef automation app …` CLI manages apps; see `2026-10-08-apps-agent-loop-design.md` |

## Problem

AI coding tools let anyone in a company build a working internal tool in an afternoon. What they produce reaches
Salesforce, HubSpot or Postgres by pasting an API key into an environment variable, often an admin key, often the
builder's personal one. IT cannot see the app, cannot see what it calls, and cannot revoke its access without
rotating a credential every other system shares.

ByteChef already holds governed connections, knows how to execute any of its components' actions and tools against
them, and records tool invocations — but only for workflows, agents, MCP servers and embedded connected users. There
is no way for a standalone application to use that machinery:

- The automation public API (`/api/automation/v1`, `automation-configuration-public-rest`) exposes only
  `/workflow-executions`, `/projects/deploy` and `/projects/{id}/git/pull`. It has no action or tool endpoints.
- The embedded public API has them (`executeAction`, `getTools`, `executeTool` in
  `embedded-execution-public-rest`), but resolves the connection from a connected user's integration instance — a
  model built for a SaaS vendor's end users, not for a company's own internal tools.
- Automation API keys authenticate **as the user who created them** (`AutomationApiKeyAuthenticationProvider` builds
  the principal from `apiKey.getUserId()`). An app given one inherits that user's full permissions and dies when the
  user is deactivated.

## Goals

- An admin can register an App in a workspace, bind connections to it, and list exactly which actions and tools it may
  call per binding, and whether it may use the raw HTTP proxy.
- An app authenticates with its own secret key and acts as **itself**, never as a user.
- An app can execute an allowed action, list and execute allowed tools, and send a raw HTTP request through a bound
  connection — without ever seeing that connection's credentials.
- Every call is recorded in the existing tool invocation log, attributed to the app.
- Disabling or deleting an app, or rotating its key, takes effect on the next request.
- A TanStack Start developer — or an AI coding tool following `AGENTS.md` — goes from the starter template to a
  running app bound to its connections with `bytechef automation app create` and `bind`; hosting deploy stays the
  template's `vercel deploy` / Docker.

## Non-goals

- **Hosting.** ByteChef never runs app code. Vercel is reached through the developer's own Vercel CLI; self-hosting is
  a build plus instructions.
- **AI gateway.** Model calls from apps are out of scope for v1. The gateway's spend summaries would need an app
  dimension; that is a later spec.
- **Sign-in for the app's users.** Phase 2. v1 leaves "who may open the app" to the host (e.g. Vercel Deployment
  Protection). Sign-in will register its own OAuth2 client for the app; nothing in v1 depends on it or precludes it.
- **Embedded edition.** Apps are an automation (EE) feature. Embedded's "SaaS vendor lets its customers ship
  extensions" case is a separate product question. Request and response bodies follow embedded's where the
  semantics match, but the paths differ: embedded addresses an action by component, because a connected user has at
  most one connection per component, while an app can hold several connections of one component and so addresses
  the connection by binding key.
- **Per-user connections.** In v1 an app uses workspace connections bound to it, not connections owned by whoever is
  using the app.
- **Running workflows from an app.** Apps call actions, tools and the proxy. Triggering published workflows stays with
  the API Platform and `/workflow-executions`.
- **Component Rules on app calls.** Rules govern AI-agent tool calls and resolve their workspace from a project
  deployment's `jobPrincipalId`; an app has none. The per-binding allow-list is v1's governance. See Decisions.
- **Environment promotion of apps**, **detecting unregistered apps on Vercel**, and **distributed-EE deployment**
  (v1 is `server-app` only, like environment promotion).
- **A Claude Code plugin.** Nothing depends on one; D adds the optional `bytechef-app-builder` skill to `bytechef-dev`.
- **Gating `ToolInvocationLogGraphQlController`.** Done separately: the tenant-wide `toolInvocationLogs` query is
  tenant-admin only, and the automation views use `workspaceToolInvocationLogs` (`EXECUTION_VIEW`), per the
  hardening-and-polish spec (item 9).
- **Rate limiting.** No API-key path has one today. v1 bounds app load only through the executor lane it runs on (see
  App-facing endpoints) and the proxy's size and time limits; a per-app quota is a later addition.

## Decisions

1. **Automation, not embedded.** The buyer is a company's IT or workspace admin, and the controls they expect —
   workspaces, roles, scopes, audit, invocation logs — live in automation. Embedded's endpoints exist but are keyed on
   `externalUserId` and integration instances. The embedded execution layer is thin (`ActionFacadeImpl` resolves one
   connection id and calls the platform `ActionDefinitionFacade.executePerform`), so the automation version reuses
   the platform facades and replaces only the connection lookup.

2. **The app is the principal.** Not a user, not an API key owner. Every request is authenticated as an app.
   Authorization is "is this call on this app's allow-list", never "does the creating user have scope X". Every
   credential type — the v1 secret key, and any later one (see Phase 2) — produces the same
   `AppPrincipal(tenantId, appId, workspaceId, environment)`, so adding a credential type touches authentication only,
   never the action, tool or proxy code.

3. **A secret key stored on the `app` row, not OAuth2 client credentials and not the `api_key` table.**
   - *Not OAuth2.* A JWT's advantage is that a request needs no credential lookup, but v1 looks the app up on every
     request anyway, because that is what makes disable immediate. Client credentials would then add a token hop, an
     SDK token cache, and three prerequisite fixes to the authorization server (persistent signing keys, tenant
     resolution on the token endpoint, mandatory enablement) for no gain. The secret lives in the host's environment
     either way, so a leak is equally serious in both designs.
   - *Not `api_key`.* `api_key.user_id` is `NOT NULL` with a foreign key to `user`, and every key provider builds its
     principal from that user. Its secret is stored in plain text and matched by equality.
   - *Not a separate key table.* One key per app is enough: only an explicit rotation replaces it (D has no hosting
     deploy), so no overlap window is needed. Rotation is a deliberate admin action, usually after a leak, where the old
     key should die at once.
   - *Hashed, unsalted.* This is the first hashed secret in the codebase (`api_key` and `mcp_server.secret_key` are
     plain). Unsalted SHA-256 is acceptable because the key carries 24 bytes of CSPRNG entropy, and a salt would
     defeat the equality lookup.

4. **Allow-list per binding, deny by default.** Binding a connection to an app grants nothing by itself. Each binding
   lists the actions and tools the app may call, and separately whether it may use the proxy. Binding a Salesforce
   admin connection must not turn the app into a general-purpose Salesforce admin.

5. **The connection proxy is in v1.** It is the strongest control: the app can reach any endpoint of a connected API
   without the credential ever leaving ByteChef. It is also the most dangerous endpoint to get wrong, so it carries
   its own constraints (see Proxy).

6. **Tool execution addresses tools by binding key and tool name**, not by a flattened name. Embedded's
   `ToolFacadeImpl` parses `SLACK_SEND_MESSAGE` by splitting on the first `_` and re-camel-casing, which cannot
   round-trip a camelCase component name (`googleDrive` → `GOOGLEDRIVE_…` → `googledrive`), and a component-based
   name collides as soon as two bindings use the same component (`sales_slack` and `support_slack`). Listings still
   return a flat `name` for LLM function calling, built as `<BINDING_KEY>__<TOOL_NAME>` in upper snake case
   (`SALES_SLACK__SEND_MESSAGE`); binding keys are unique per app, and unique after flattening (bind enforces it),
   so the name is too. The SDK maps a flat name back to `(bindingKey, toolName)` from the listing and never parses
   it.

7. **TanStack Start first, framework-agnostic core.** The adapter is thin; the client that talks to ByteChef has no
   framework dependency so a Next.js or Hono adapter is a small addition later.

8. **No Claude Code plugin dependency.** The agent loop that works in every coding agent is the CLI (all agents run
   shell commands), the management MCP tools (all of them speak MCP), and `AGENTS.md` inside the template (all of them
   read it).

## Current state

### Authentication

- `ApiKeyAuthenticationFilter` + `AbstractApiKeyAuthenticationConverter` (platform-security-web-api) read
  `Authorization: Bearer <key>` and `X-ENVIRONMENT`, resolve the tenant with `TenantKey.parse`, and delegate to a
  per-platform provider. `AutomationApiKeySecurityConfigurer` matches `^/api/automation/v[0-9]+/.+`.
- `api_key` has `environment`, `type` (`PlatformType`) and `user_id` (`NOT NULL`, FK `fk_api_key_user`); the
  workspace link is the `workspace_api_key` relation table and is never consulted during authentication.
  `ApiKeyServiceImpl` generates the secret as `TenantKey.of()`, stores it in plain text and finds it with
  `findBySecretKey`.
- Relevant to Phase 2 only: `Oauth2AuthorizationServerConfiguration` is enabled by `bytechef.oauth2.authorization-server.enabled=true`, was
  built for MCP (`McpAuthorizationServerConfigurer`, dynamic client registration), stores clients, authorizations and
  consents through the JDBC repositories, mints `tenant_id` and `authorities` claims (tenant from the RFC 8707
  `resource` parameter or the current tenant), and **generates its RSA signing key in memory at startup**
  (`jwkSource()` → `generateRsaKey()`).

### Execution

- `ActionDefinitionFacade.executePerform(componentName, int componentVersion, actionName, …, inputParameters,
  Map<String, Long> connectionIds, extensions, environmentId, PlatformType, editorEnvironment, …)` executes any
  action. `ActionDefinitionServiceImpl` enforces tenant-wide component policy through `ComponentVisibilityProvider`
  (`COMPONENT_DISABLED` / `ACTION_DISABLED`).
- `ActionDefinitionFacadeImpl.getComponentConnection` only loads the connection and checks it is `ACTIVE`. Nothing
  on the execution path checks that the caller may use that connection, or that it is in any workspace or
  environment — connection access is entirely the caller's responsibility.
- `Connection` has **no `workspace_id`**; workspace membership is the `workspace_connection` relation
  (`WorkspaceConnectionRepository.findByConnectionId`). It does carry `environment`, `componentName`,
  `connectionVersion`, `type` (`PlatformType`), `status` and `authorizationType` (nullable).
  `ConnectionServiceImpl.getConnection` also serves virtual AI-provider connections (`AiProviderConnectionId`).
- `ClusterElementDefinitionFacade.executeTool(componentName, clusterElementName, inputParameters, connectionId)`
  executes a TOOLS cluster element; `ClusterElementDefinitionServiceImpl` applies the same visibility checks.
- `ContextFactory.createContext(componentName, componentConnection)` builds the `Context` the HTTP client needs;
  `ActionDefinitionFacadeImpl.getComponentConnection` shows how a `ComponentConnection(componentName,
  connectionVersion, id, parameters, authorizationType)` is built from a `Connection`.
- `HttpClientExecutor.execute(urlString, headers, queryParameters, body, configuration, requestMethod,
  componentName, componentVersion, operationName, componentConnection, context)` applies a connection's
  authorization (`ConnectionDefinitionService.executeAuthorizationApply`), joins a relative URL onto the
  connection's base URI (`executeBaseUri`) by plain string concatenation, and refreshes OAuth2 tokens.
  **`getConnectionUrl` returns any URL containing `://` unchanged, and `createHttpClient` attaches the connection's
  credentials whenever `componentConnection != null && authorizationType != null`** — so an absolute URL gets the
  credentials. `applyAuthorization` merges headers with `putAll`, so a header differing only in case is not replaced.
- Security: one `apiFilterChain` (`securityMatcher("/api/**", "/graphql")`, `.authenticated()`) applies every
  `SecurityConfigurerContributor` bean; each adds its own `ApiKeyAuthenticationFilter` scoped by `shouldNotFilter`,
  so several key types coexist on `/api/**` (API Platform `^/api/o/.+` and the AI gateway already do).
  `AbstractApiKeyHttpConfigurer.registerCsrfOverride` is a no-op; `AutomationApiKeySecurityConfigurer` overrides it
  to ignore CSRF on its pattern. `ApiKeyAuthenticationFilter` casts the converter's result to
  `AbstractApiKeyAuthenticationToken`, whose authenticated constructors require a Spring `User`, and catches only
  `AuthenticationException` — `TenantKey.parse` on a garbage token throws `IllegalArgumentException`, which is a 500
  today for every key type. `SecurityUtils.extractPrincipal` returns null for a non-`UserDetails` principal, so
  `AuditorAware` writes `SYSTEM`. `SpaWebFilter` already excludes `/api`.

### Logging

- `ToolExecutionRecorder.record(builder, supplier)` (CE `platform-tool-execution`) times a call, sets the outcome,
  publishes a `ToolExecutionEvent` and increments `bytechef_tool_invocation`. The event never carries payloads.
- EE `platform-tool-invocation-log` persists events to `tool_invocation_log`; `surface`, `kind` and `outcome` are
  stored **by ordinal**. `ToolExecutionSurface` has `MCP_AUTOMATION, MCP_EMBEDDED, MCP_MANAGEMENT,
  EMBEDDED_API_ACTION, EMBEDDED_API_TOOL, AI_AGENT`; `ToolExecutionKind` has `COMPONENT, WORKFLOW, CONTRIBUTED,
  MANAGEMENT_TOOL`.
- The automation client already has `/automation/executions/tool-invocations`. Its original `toolInvocationLogs`
  GraphQL query takes no `appId` or `workspaceId` argument; it is now tenant-admin only, and the automation views
  (this page and the App Activity tab) use `workspaceToolInvocationLogs`, guarded by `EXECUTION_VIEW` in the
  workspace and environment (hardening-and-polish spec, item 9).

## Design — A. Server

### Module

`server/ee/libs/automation/automation-app/` with `automation-app-api`, `automation-app-service`,
`automation-app-graphql` (admin UI) and `automation-app-public-rest` (app-facing and management REST). All classes
carry the ByteChef Enterprise header and `@version ee`. Wired into `server-app` only.

### Model

**`app`**

| Column | Type | Notes |
|--------|------|-------|
| `id` | BIGINT | |
| `uuid` | VARCHAR(36) | stable identity for CLI `.bytechef/app.json` |
| `name` | VARCHAR(256) | unique per (workspace, environment) |
| `description` | TEXT | nullable |
| `workspace_id` | BIGINT | nullable column per convention; always set for apps |
| `environment` | INT | `Environment` ordinal; one app row per environment |
| `owner_user_id` | BIGINT | the named owner shown in the registry; not the principal |
| `status` | INT | `AppStatus { ENABLED, DISABLED }`, append-only ordinals |
| `visibility` | INT | `ResourceVisibility`, created `WORKSPACE` |
| `hosting_target` | INT | `AppHostingTarget { VERCEL, SELF_HOSTED }` |
| `url` | VARCHAR(2048) | nullable; set by deploy |
| `last_deployed_date` | TIMESTAMP | nullable; set by deploy |
| `secret_key_hash` | VARCHAR(64) | hex SHA-256 of the key; unique index; requests are matched on it |
| `secret_key_hint` | VARCHAR(16) | last 8 characters of the key, `=` padding stripped, shown in the UI to identify the current key. Not the first characters: a `TenantKey` is `base64(tenantId + ":" + base64(random))`, so its leading characters encode the tenant id and are identical for every app in a tenant |
| `secret_key_last_used_date` | TIMESTAMP | nullable; written at most once a minute per app, by a targeted `UPDATE` that touches neither `version` nor the audit columns (see App secret key) |
| audit columns | | `created_by`, `created_date`, `last_modified_by`, `last_modified_date`, `version` |

The `app_` prefix is shared with embedded's `app_event` / `app_event_workflow` tables and `AppEvent*` classes; Java
classes live in `com.bytechef.ee.automation.app` and clash with nothing.

**`app_connection`** — one row per binding.

| Column | Type | Notes |
|--------|------|-------|
| `id` | BIGINT | |
| `app_id` | BIGINT | FK `app` |
| `connection_id` | BIGINT | FK `connection`, no cascade; at bind time the connection must be `type = AUTOMATION`, a real (not AI-provider virtual) connection, in the app's workspace via `workspace_connection`, and in the app's environment |
| `binding_key` | VARCHAR(64) | app-chosen handle, e.g. `crm`; unique per app; `^[a-z][a-z0-9_-]{0,63}$` because it is a URL path segment and the stem of flat tool names. Two keys whose flat form is equal (`support-crm`, `support_crm` -> `SUPPORT_CRM`) cannot coexist on one app: bind rejects the second with `APP_BINDING_KEY_ALREADY_EXISTS` |
| `proxy_enabled` | BOOLEAN | default false |
| `proxy_allowed_methods` | VARCHAR(64) | comma list, e.g. `GET,POST`; empty = none |

**`app_connection_operation`** — the allow-list.

| Column | Type | Notes |
|--------|------|-------|
| `app_connection_id` | BIGINT | FK |
| `operation_type` | INT | `AppOperationType { ACTION, TOOL }` |
| `operation_name` | VARCHAR(256) | action name, or TOOLS cluster element name |

Component name and version are not stored on the operation; they come from the bound connection
(`connection.componentName`, `connection.connectionVersion`). An action or tool call uses the component's latest
version unless the request names `componentVersion`. Bindings do not pin a version in v1. Because execution runs
the latest version, every binding listing (the app-facing `GET /connections`, the admin `appBindings` and
`appBindableConnections` queries) carries `componentVersion`, the latest version resolved server-side, next to
`connectionVersion`; the admin binding picker offers the operations of `componentVersion`, never of
`connectionVersion`. The listings resolve that version with `ComponentDefinitionService.fetchComponentDefinition`, so
a component that cannot be resolved (a deleted custom component, one dropped in an upgrade, or one missing from the
component index) never fails a listing: `appBindableConnections` leaves such connections out, the app's tool listing
leaves out their tools, and a binding to one carries a null `componentVersion` (GraphQL `Int`, optional in the
app-facing OpenAPI), which the admin UI marks unavailable without loading its operations. An action or tool call
through such a binding still fails.

**Deleting a bound connection is refused.** Because the FK has no cascade, a connection delete fails while any app
binds it. `ConnectionFacade`'s delete gains an "in use" pre-check through a `ConnectionUsageChecker` SPI
(`platform-connection-api`; `AppConnectionUsageChecker` implements it and reports every app that binds the connection,
whatever its status) so the user sees a clear error naming the apps, not an FK violation. Unbinding first is the
way out. A connection can therefore not be "deleted" under a live binding; the 409 `APP_CONNECTION_UNAVAILABLE`
case covers a connection that was moved to another workspace or environment or set inactive.

Liquibase: a new folder `changelog/automation/app/` under
`automation-app-service/src/main/resources/config/liquibase/` (changelogs live in the owning `-service` module, as
`workflow_alert` and `workflow_execution_cost` do) with `20261006000001_automation_app_init.xml`, and a new
`includeAll` line in `liquibase-config`'s `master.xml` (`contextFilter="mono or configuration or multitenant"`).

### Visibility, ownership, scopes

Follows `.agents/resource-visibility.md`:

- Five classes, in one commit, each modelled on its `Connection`/`Mcp` sibling: `AppVisibilityPolicy implements
  ResourceVisibilityPolicy` (`resourceType "App"`, default `WORKSPACE`, supported values `PRIVATE` and `WORKSPACE`
  only: an app belongs to one workspace, so `ORGANIZATION` is not offered), `AppVisibilityProvider implements
  ResourceVisibilityProvider`, `AppOwnershipResolver implements ResourceOwnershipResolver`,
  `AppEnvironmentResolver implements ResourceEnvironmentResolver` (the app row carries its environment), and
  `AppPermissionScopeProvider implements PermissionScopeProvider` (`@Component @ConditionalOnEEVersion`).
- `AppPermissionScope { APP_VIEW, APP_MANAGE }`: `APP_VIEW` → `VIEWER`, `APP_MANAGE` → `ADMIN`. **Creating, binding
  and rotating are admin actions by design**: managing an app hands it credentials, so EDITOR is not enough. A builder
  who is not a workspace admin asks an admin to create the app and receives the key from them. `APP_VIEW` lets any
  workspace member see an app's registry entry, key hint, last-used date and activity.
- Admin facades gate with `hasWorkspaceScopeInEnvironmentId(#workspaceId, 'APP_MANAGE', #environmentId)` or
  `hasResourceScope(#id, 'App', 'APP_MANAGE')`. `EnvironmentAwareGateCoverageTest.RESOLVED_RESOURCE_TYPES` and
  `ResourceEnvironmentResolverCoverageTest`'s discovery assertion hard-code the resolved types and gain `"App"`; the
  new facade methods are added to the PermissionService classification table in
  `docs/superpowers/specs/2026-08-17-project-visibility-design.md` §17.
- The app list (`getApps`) shows only apps the caller can see (`ResourceVisibilityResolver.filterVisibleIds` on the
  app's visibility and creator), so a `PRIVATE` app is hidden from workspace members who are not its creator and
  hold no grant on it.
- The bindable-connections lookup lists only connections the caller can see
  (`ResourceVisibilityResolver.filterVisibleIds` on the connection's visibility and creator), so a `PRIVATE` connection
  cannot be bound by someone who cannot see it.
- Binding a connection additionally requires the caller to hold the connection-use scope on that connection
  (`hasResourceScope(#connectionId, 'Connection', <the scope ConnectionFacade uses for reading a connection's
  details>)`), so an app cannot launder access to a connection its admin cannot see.
- `toolInvocationLogs` gains an `appId` argument (repository `@Query` change) for the Activity tab. Reads are
  authorized on `ToolInvocationLogFacade`: the Activity tab and the automation Tool Invocations page use
  `workspaceToolInvocationLogs` (`EXECUTION_VIEW` in the workspace and environment, rows filtered by `workspace_id`
  and `environment`); `toolInvocationLogs` is tenant-admin only.
- Audit events: `APP_CREATED`, `APP_DELETED`, `APP_ENABLED`, `APP_DISABLED`, `APP_SECRET_KEY_ROTATED`,
  `APP_CONNECTION_BOUND`, `APP_CONNECTION_UNBOUND`, `APP_OPERATIONS_CHANGED`, `APP_UPDATED` (payload `changedFields`
  plus the new `name`, `visibility` and `hostingTarget` when changed; only when something changed).
  `APP_CONNECTION_UNBOUND` is published only when a binding was actually deleted; unbinding an unknown key is a
  silent no-op.
- An app's name is at most 256 characters and its url at most 2048 (the column widths), checked before any write
  (400 `APP_FIELD_TOO_LONG`; the management OpenAPI declares the same `maxLength`). A unique-constraint violation
  on save becomes `APP_NAME_ALREADY_EXISTS` only when it names `uk_app_workspace_environment_name`; any other
  integrity violation is rethrown unchanged.

### App secret key

The key is the app's credential to ByteChef. It is never a third-party credential: those stay inside the bound
connections and never reach the app.

- **Create** generates the key in the `TenantKey` format (so the tenant can be read from it before any lookup, as
  `AbstractApiKeyAuthenticationConverter` does today), stores `secret_key_hash` and `secret_key_hint`, and returns
  the key once. ByteChef cannot show it again.
- **Rotate** generates a new key and replaces the hash and hint. The old key fails on its next request. There is no
  overlap window: rotation is a deliberate admin action, and deploy never rotates.
- **Disable** sets `status = DISABLED`; the key fails on its next request. Enabling restores it unchanged.
- **Delete** removes the app row, and with it the key, the bindings, the operations and the resource grants.
- `secret_key_last_used_date` lets an admin see whether a deployed app is still calling. It is written by a targeted
  `@Modifying @Query("UPDATE app SET secret_key_last_used_date = :now WHERE id = :id AND
  (secret_key_last_used_date IS NULL OR secret_key_last_used_date < :cutoff)")`, best-effort and outside the
  request's outcome. Saving the aggregate from request threads would race admin edits on `version` and turn an
  `OptimisticLockingFailureException` into a 500 for the app; the targeted update bumps neither `version` nor the
  audit columns. The throttle is the `secret_key_last_used_date < :cutoff` predicate in the database, not replica
  state, so the whole cluster writes at most once per app per minute.

### App-facing authentication

`AppSecurityConfigurer extends AbstractApiKeyHttpConfigurer`, registered through a `@Configuration
@ConditionalOnEEVersion AppSecurityConfigurerContributor` like its automation sibling, matches
`^/api/app/v[0-9]+/.+`, overrides `registerCsrfOverride` to ignore CSRF on that pattern (without it every POST is
rejected), and installs an `ApiKeyAuthenticationFilter` with an `AppApiKeyAuthenticationConverter` and an
`AppApiKeyAuthenticationProvider`:

1. The converter reads `Authorization: Bearer <key>` and resolves the tenant with `TenantKey.parse`, wrapping any
   `RuntimeException` it throws in `BadCredentialsException` and validating the tenant id itself (the filter switches
   to the key's tenant before the provider runs, outside its failure handling, so a tenant `TenantContext` would
   reject must be refused here), so a malformed key is 401, not 500. It overrides
   `getEnvironment` so `X-ENVIRONMENT` is ignored; the environment belongs to the app.
2. Inside `TenantContext.runWithTenantId`, the provider hashes the key and loads the app by `secret_key_hash`. No
   match is 401. A `DISABLED` app is rejected here with `DisabledException`, which the API key filter answers as 401
   like any other authentication failure; the execution facade re-checks the status and answers 403 `APP_DISABLED` for
   an app disabled between authentication and execution.
3. The provider returns an `AppAuthenticationToken` whose principal is
   `AppPrincipal(tenantId, appId, workspaceId, environment)`, read from the app row, and whose
   `fetchEnvironmentId()` returns the app's environment, because `PrincipalEnvironment` reads the environment from
   the token. `AbstractApiKeyAuthenticationToken`'s authenticated constructors require a Spring `User`, so the base
   class gains one protected authenticated constructor taking an arbitrary principal and authorities;
   `AppAuthenticationToken` uses it and overrides `getPrincipal()`. Its single authority is `ROLE_APP`.

**The lookup is not cached.** It is one indexed `SELECT` per request. Spring Cache here is Caffeine per replica by
default and nothing broadcasts evictions, so a cache would make disable and rotate take effect on one node and not
the others.

**The app is not a user.** `SecurityUtils.extractPrincipal` returns null for `AppPrincipal`, so writes made during an
app call are audited as `SYSTEM`, and `SecurityUtils.getCurrentUserLogin()` throws — code on the app path must not
call it. The invocation log, not the audit columns, is where an app's activity is attributed.

### App-facing endpoints

Base `/api/app/v1` — a root of its own, not under `/api/automation/v1`, because
`AutomationApiKeySecurityConfigurer` claims every `^/api/automation/v[0-9]+/.+` path for user API keys and the two
credential types must never be accepted on each other's endpoints. The controllers use
`@RequestMapping("${openapi.openAPIDefinition.base-path.app:/api/app}/v1")`; the property is added next to the
`automation`/`embedded`/`platform` entries in `server-app`'s `application.yml` and
`config-server-app`'s `config/apps/application.yml`. The default is deliberately non-empty — the sibling `${…:}`
pattern would silently mount the endpoints at `/v1/**`, outside the `/api/**` security chain, if the property were
missing. Request and response bodies follow embedded's `executeAction` / `getTools` / `executeTool` where the
semantics match; the paths do not (see Non-goals, Embedded edition).

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/connections` | The app's bindings: `bindingKey`, `componentName`, `componentVersion`, allowed actions, allowed tools, proxy flag and methods. Never credentials. |
| POST | `/connections/{bindingKey}/actions/{actionName}` | Execute an allowed action. Body `{input}`. Optional `componentVersion`. |
| GET | `/tools` | Allowed tools across all bindings: `name` (`<BINDING_KEY>__<TOOL_NAME>`, for LLMs), `bindingKey`, `componentName`, `toolName`, `description`, `parameters` (JSON schema built as embedded's `ToolFacadeImpl` does: `JsonSchemaGeneratorUtils.generateInputSchema(ToolPropertyUtils.withoutToolOverrideProperties(props))`). |
| POST | `/connections/{bindingKey}/tools/{toolName}` | Execute an allowed tool. Body `{input}`. |
| POST | `/connections/{bindingKey}/proxy` | Raw HTTP through the connection. See Proxy. |

`AppExecutionFacade` (in `-service`) does, for every call:

1. Resolve the binding by `(appId, bindingKey)`; 404 if absent.
2. Check the operation against the allow-list; 403 `APP_OPERATION_NOT_ALLOWED` if absent. The proxy checks
   `proxy_enabled` and the method instead.
3. Verify the bound connection is still in the app's workspace (through `workspace_connection`) and environment and
   is `ACTIVE`; 409 `APP_CONNECTION_UNAVAILABLE` otherwise. This is the only place the connection is checked —
   `executePerform` itself trusts whatever connection id it is handed.
4. Execute **on `appExecutor`**, a bulkhead of its own (`bytechef.app.execution.concurrency-limit`, default 10) so
   apps cannot starve the API Platform sync path or the editor's Test button, with a per-call timeout (`bytechef.app.execution.timeout`, default 60 s, 504 `APP_EXECUTION_TIMEOUT`).
   Component code must never run on the request thread, and a burst from one app must not take a permit from
   scheduled workflow runs on `workerExecutor`. The timeout is validated before the call is submitted, and the call
   runs under `SecurityUtils.runAsSystem`, as components do in workflow workers: the app principal carried onto the
   lane is not a user, and OAuth2 token refresh updates the connection under an owner-or-admin check that it would
   fail. Actions go through `ActionDefinitionFacade.executePerform` with
   `connectionIds = Map.of(componentName, connectionId)`, `type = PlatformType.AUTOMATION`, the app's environment
   id, `editorEnvironment = false`, and all job/principal ids null (as embedded does); tools through
   `ClusterElementDefinitionFacade.executeTool`; proxy through `AppProxyExecutor`. Tenant-wide component policy
   applies automatically on the first two; the proxy checks `ComponentVisibilityProvider.isVisible(componentName)`
   itself.
5. Record through `ToolExecutionRecorder.record` (see Logging).

Errors from REST controllers are mapped by handlers declared on the controllers' abstract base class
(`AbstractAppApiController`), not by an advice: `GlobalResponseEntityExceptionHandler` is `HIGHEST_PRECEDENCE`, so an
advice of the module's own would be shadowed (a facade-thrown `AccessDeniedException` would surface as 500 and every
App error as a generic 400), while Spring consults a controller's own `@ExceptionHandler`s first.

An `APP_OPERATION_NOT_ALLOWED` message lists the binding's allowed operations of that type
(`'deleteRecord' is not allowed on binding 'crm'; allowed actions: createRecord, updateRecord`; the proxy lists its
allowed methods), so a coding agent can correct itself. Typed bindings and an instructions endpoint are out of scope.

A connection is bound to an app only by an admin action. The app-facing API has no way to bind, list unbound
connections, or change its own allow-list.

### Proxy

Request body:

```json
{
  "method": "GET",
  "path": "/services/data/v60.0/query",
  "query": {"q": ["SELECT Id FROM Account LIMIT 5"]},
  "headers": {"Accept": ["application/json"]},
  "body": null
}
```

Response: `{status, headers, body}`. The proxy endpoint itself returns 200 whenever the upstream call completed, so
the app can distinguish "ByteChef refused" (4xx/5xx from ByteChef with an error body) from "Salesforce said 404"
(200 with `status: 404`).

Constraints, all enforced in `AppProxyExecutor` before `HttpClientExecutor` is reached:

- **Relative paths only.** `path` must start with a single `/`. Rejected: anything containing `://`, a leading `//`,
  an empty inner segment (`//`), a backslash, `@`, `?`, `#`, whitespace and control characters, percent-encoded
  `/ \ ? # @ %`, and any segment that reduces to `.` or `..` in a form an upstream might reduce it to. A decoded segment
  is cut at the first `;` (a decoded `%3b` included, because upstreams drop path parameters before resolving dot
  segments), trimmed, NFKC-normalised and re-checked; beyond ASCII only letters, digits and combining marks are allowed,
  since non-ASCII punctuation can be a look-alike for `.`, `/`, `\`, `@` or `;` that NFKC does not fold. This closes
  the `getConnectionUrl` pass-through: an absolute URL would otherwise receive the connection's credentials. The cost is
  that non-ASCII punctuation in an upstream path is unreachable through the proxy.
- **Host pinning.** `executeBaseUri` + path is plain string concatenation, so the executor joins them, requires the
  result's scheme, host and port to equal the base URI's and its path to stay under the base URI's path (a base URI
  with userinfo, a query or a fragment is rejected), and **sends that pinned absolute URI**, not the relative path:
  `HttpClientExecutor` passes an absolute URL through unchanged while still applying the connection's credentials, so
  the string that was validated is exactly the string sent. Redirects are never followed; a 3xx is returned to the app.
  A connection with no base URI cannot be proxied (409 `APP_PROXY_UNSUPPORTED`).
- **Credentials required.** A connection whose `authorizationType` is null would proxy with no credentials attached;
  the proxy exists to inject credentials, and an app can call an unauthenticated API itself, so this is also 409
  `APP_PROXY_UNSUPPORTED`.
- **Header hygiene.** Client-supplied `Authorization`, `Proxy-Authorization`, `Cookie`, `Host`, `Expect`, hop-by-hop
  headers and proxy metadata (`Forwarded`, `X-Real-IP`, `X-Original-URL`, anything starting `X-Forwarded-`) are dropped
  **case-insensitively**. Header names must be RFC 9110 tokens and values free of control characters, and the content
  type must parse, else 400 `APP_PROXY_REQUEST_INVALID`. Response `Set-Cookie`, `Set-Cookie2` and hop-by-hop headers are
  dropped.
- **Credentials are applied by the proxy itself.** `HttpClientExecutor` is called with `disableAuthorization(true)`
  and the proxy applies the connection's authorization (`ConnectionDefinitionService.executeAuthorizationApply`),
  after removing any app header or query key that collides with an applied one case-insensitively, so the connection's
  credentials always win. The client's own authorization path installs an error interceptor that turns every 4xx and
  5xx into an exception before the body is read, which would hide upstream errors from the app.
- **Method allow-list** from `proxy_allowed_methods`.
- **Limits.** Request body 10 MB (checked before the call, 413 `APP_PROXY_REQUEST_TOO_LARGE`), timeout 30 s via
  `Context.Http.Configuration` → `resolveRequestTimeout`, all under `bytechef.app.proxy.*` with defaults in yml. The
  response is requested as BINARY, so `HttpClientExecutor` spools the body to temporary file storage, and the client
  aborts the exchange as soon as the body passes 10 MB (`Http.Configuration` `maxResponseBytes`, 502
  `APP_PROXY_RESPONSE_TOO_LARGE`); the read-back keeps the same bound as a backstop. Spool files get unique names, `proxy-request-<uuid>` and
  `proxy-response-<uuid>`, since some storage providers delete by name alone, and are deleted once read
  (`TempFileStorage.deleteFile`). Binary bodies are base64 in both directions.
- **Token refresh** is done by the proxy, once. `HttpClientExecutor`'s refresh lives in an aspect around
  `@WithTokenRefresh` beans that the proxy does not pass through, so a connection that
  `canCredentialsBeRefreshed()` and whose upstream response matches the authorization's refresh-on rules is refreshed
  by the proxy, the context rebuilt, the base URI re-resolved and re-pinned, and the request sent again. A refresh that
  fails answers 409 `APP_CONNECTION_UNAVAILABLE` with a fixed message. The `Context` comes from
  `ContextFactory.createContext(componentName, componentConnection)` and the `ComponentConnection` is built from the
  `Connection` exactly as `ActionDefinitionFacadeImpl.getComponentConnection` builds it.

### Logging

Append to the enums (ordinals are persisted — append only):

- `ToolExecutionSurface`: `AUTOMATION_APP_ACTION`, `AUTOMATION_APP_TOOL`, `AUTOMATION_APP_PROXY`.
- `ToolExecutionKind`: `HTTP_PROXY`.

Add a nullable `Long appId` to the `ToolExecutionEvent` record and its builder, map it in
`ToolInvocationLogEventListener`, add it to the `ToolInvocationLog` domain, and add an `app_id BIGINT` column plus an
`(app_id, created_date)` index to `tool_invocation_log` (new changeset in
`platform-tool-invocation-log-service`'s changelog folder). `ToolInvocationLog` is a Spring Data JDBC aggregate, not
a Jackson-persisted record, and the event never crosses the broker, so a nullable field is safe for existing rows.
For the proxy, `operationName` is `"<METHOD> <path without query>"`; query strings are not recorded, since they often
carry data.

The existing Tool Invocations page gains an **App** filter (`tool-invocation-log.graphqls` + client
`toolInvocationLogs.graphql` + codegen); the app detail page links to it pre-filtered.

### Management endpoints

For the CLI (the MCP has no app tools), under the existing automation API-key auth (the caller is a user, gated by
`APP_MANAGE`), base `/api/automation/v1/apps`, workspace as a query parameter like the existing endpoints:

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/apps` | List |
| POST | `/apps` | Create; returns the secret key once |
| GET | `/apps/{id}` | Get |
| PATCH | `/apps/{id}` | Update name, description, status, `url`, `lastDeployedDate`, hosting target. Every field is optional and an omitted (null) field is kept; an empty string clears `description` and `url` |
| DELETE | `/apps/{id}` | Delete |
| POST | `/apps/{id}/secret-key` | Rotate; returns the new key once |
| PUT | `/apps/{id}/connections/{bindingKey}` | Create or replace a binding with its allow-list and proxy settings |
| DELETE | `/apps/{id}/connections/{bindingKey}` | Unbind |

The GraphQL admin surface (`automation-app-graphql`, `app.graphqls`) exposes the same operations for the UI, plus
options lookups (connections the caller can bind; the actions and tools a component offers).

### Admin UI

- Sidebar: **Apps** row in the automation **Deploy** group (`navigationItems.ts`), route `/automation/apps`.
- **Apps page:** list rows following the shared `*ListItem` rhythm — name, owner, status switch, hosting target,
  URL, last deployed.
- **App detail:** overview (owner, URL, status, key hint and last used, Rotate key), **Connections** tab (bind a connection,
  pick a binding key, tick allowed actions and tools, toggle proxy and methods), **Activity** tab (the tool
  invocation log filtered to the app).
- Create and Rotate show the key once with a copy button and the exact environment variables to set.

## Design — B. SDK

Superseded by `2026-10-06-apps-workflows-and-sdk-design.md`, which also adds workflow bindings (A4). In short: one
package, `@bytechef/app`, with `@bytechef/app/tanstack-start` and `@bytechef/app/ai-sdk` subpath exports instead of
two packages; a workflows API; and a Vercel AI SDK adapter.

## Design — C. Template

`npm create bytechef-app` (package `create-bytechef-app` in the same workspace) scaffolds a TanStack Start app that is
safe by default and built to be extended by an AI coding agent. Specified in `2026-10-07-apps-template-design.md`; where
that spec and this document disagree about the template, that spec wins.

## Design — D. Coding-agent loop

Specified in `2026-10-08-apps-agent-loop-design.md`, which replaces the deploy design that stood here. App management
moved from MCP tools to the CLI (`bytechef automation app …`, so the key never passes through the model and is
written straight into `.env.local`); `app deploy` and `key rotate --vercel` were dropped (the template deploys itself
with `vercel deploy` / Docker); workflow building and deployment were added (the management MCP's `buildWorkflow`
app-callable mode and `deployProject`).

## Error handling summary

| Situation | Status | Code |
|-----------|--------|------|
| Missing or malformed key, or no app with that key (including a deleted app or a rotated-out key) | 401 | — |
| App disabled, at authentication (the filter) | 401 | — |
| App disabled between authentication and execution (the facade) | 403 | `APP_DISABLED` |
| Unknown binding key | 404 | `APP_CONNECTION_NOT_FOUND` |
| Operation not on the allow-list / proxy off / method not allowed | 403 | `APP_OPERATION_NOT_ALLOWED` |
| Bound connection moved to another workspace or environment, inactive, or its token refresh failed | 409 | `APP_CONNECTION_UNAVAILABLE` |
| Binding key whose flat form collides with another binding | 409 | `APP_BINDING_KEY_ALREADY_EXISTS` |
| Proxy path rejected | 400 | `APP_PROXY_PATH_REJECTED` |
| Proxy request malformed (header name or value, content type) | 400 | `APP_PROXY_REQUEST_INVALID` |
| Proxy request body over `max-request-bytes` | 413 | `APP_PROXY_REQUEST_TOO_LARGE` |
| Proxy response body over `max-response-bytes` | 502 | `APP_PROXY_RESPONSE_TOO_LARGE` |
| Upstream unreachable (connect, DNS, I/O, timeout) | 502 | `APP_PROXY_UPSTREAM_UNAVAILABLE` |
| Proxy could not store or read its own temporary file | 500 | `APP_PROXY_STORAGE_FAILED` |
| Connection has no base URI or no authorization | 409 | `APP_PROXY_UNSUPPORTED` |
| Component or action disabled by policy (actions and tools) | 403 | `COMPONENT_DISABLED` / `ACTION_DISABLED` |
| App name or url longer than its column | 400 | `APP_FIELD_TOO_LONG` |
| Action or tool threw (`ExecutionException`, or any other non-App error from the component layer) | 422 | `COMPONENT_ERROR`, message only, never the input parameters |
| Same, with a `ProviderException` anywhere in the cause chain (the perform path wraps provider errors in `ExecutionException`) | 422 | `PROVIDER_ERROR`, the provider exception's message only |
| Call exceeded `bytechef.app.execution.timeout` | 504 | `APP_EXECUTION_TIMEOUT` |
| Upstream call completed (any status) | 200 | proxied `status` in the body |

## Testing

**Server**

- `AppExecutionFacadeTest`: allow-list hit and miss for each operation type, with the allowed operations listed in
  the refusal; connection moved to another workspace or environment, deleted or inactive; disabled app; two bindings
  on one component yield distinct flat tool names; execution runs on `appExecutor` as the system principal;
  a slow action is cut off with `APP_EXECUTION_TIMEOUT`; a missing timeout fails before the component runs;
  bindings carry the latest `componentVersion`, or null for an unresolvable component, whose tools the tool listing
  leaves out; proxy recording, transport-failure classification, and the lane
  timeout interrupting an upstream that drips its body (stub HTTP server).
- `AppExecutionFacadeIntTest`: an allowed action reaches the platform facade with the bound connection and the
  tenant; an action outside the allow-list is refused; a proxy on a binding without proxy enabled is refused and
  recorded as an error; disabling the app stops an already authenticated principal; rotating the key invalidates
  the old one.
- `AppProxyExecutorTest` and `ProxyPathValidatorTest`: a table of rejected paths (`https://evil.com`,
  `//evil.com/x`, `/\evil.com`, `/a/../../b`, `/%2e%2e/b`, `/..;/x`, `/a/..%3b/b`, `@evil.com/x`, control
  characters) and accepted ones; host pinning after base-URI join; method allow-list; a connection without
  `authorizationType` or base URI is refused; applied credentials win over colliding app headers and query keys;
  upstream error bodies, redirects and 204 pass through; refresh exactly once; spool files are uniquely named and
  deleted. `ProxyHeadersTest`: headers stripped regardless of case.
- `AppApiKeyFilterChainTest`: one real security filter chain with both `AutomationApiKeySecurityConfigurer` and
  `AppApiKeySecurityConfigurer` and CSRF enabled: an app key is accepted on `/api/app/v1` for a POST without a CSRF
  token and yields an `AppPrincipal`; a user API key is 401 on `/api/app/v1` and an app key is 401 on
  `/api/automation/v1`; a disabled app, an unknown or rotated-out key, and a malformed or missing key are 401.
- `AppApiKeyAuthenticationConverterTest`: the tenant is read from the key, `X-ENVIRONMENT` is ignored, a malformed
  key is `BadCredentialsException` (401, not 500). `AppApiKeyAuthenticationProviderTest`: an unknown key is
  rejected, a disabled app raises `DisabledException`, the principal's environment comes from the app row.
  `AppApiKeySecurityConfigurerTest`: the path pattern and the CSRF override.
- `AppServiceIntTest`: create returns the key once and stores only its hash; duplicate names are rejected; rotation
  replaces the hash; `secret_key_last_used_date` is written at most once a minute. `AppServiceTest`: the last-used
  write is best-effort; name and url lengths are checked up front; only the name constraint maps to
  `APP_NAME_ALREADY_EXISTS`.
- `AppFacadeTest`: the `@PreAuthorize` expressions are pinned; the app list and the bindable connections are
  filtered by visibility; bind and unbind audit as specified; bindings and bindable connections carry the latest
  `componentVersion`; a bindable connection with an unresolvable component is left out and a binding to one maps to
  a null `componentVersion`. `AppConnectionServiceTest`: binding key format, flat-key collisions, bindability (workspace,
  environment, automation-type connection), unbind reports whether a row was deleted.
- `AppApiControllerTest`, `AppConnectionApiControllerTest`, `AppToolApiControllerTest`, `AppProxyApiControllerTest`,
  `AppApiControllerGlobalHandlerTest` and `AppExecutionControllersGlobalHandlerTest`: status and code mapping,
  including 422 `COMPONENT_ERROR` for a real `ExecutionException` with the global exception handler installed and
  no input parameters in the body, 422 `PROVIDER_ERROR` for one wrapping a `ProviderException`, and a null
  `componentVersion` serialised on `GET /connections`. `AppGraphQlControllerTest`: delegation only.
- `ConnectionFacadeIntTest`: deleting a connection is refused while a `ConnectionUsageChecker` reports usages;
  `AppConnectionUsageCheckerTest` names the apps. `WorkspaceConnectionFacadeTest`: the refusable delete runs before
  the OAuth refresh job is cancelled.
- `AppSecretKeyRedactionTest` and `AppGraphQlControllerTest`: no record carrying the plaintext key prints it.
- Coverage tests that already exist must pass with the new gates: `EnvironmentAwareGateCoverageTest`,
  `ResourceEnvironmentResolverCoverageTest`.
- `ToolInvocationLogServiceIntTest`: persistence with the new surfaces and `app_id`, filtered by app.

**SDK** — vitest with a mocked `fetch`: the key is sent on every request, construction in a browser-like environment
throws, a missing `BYTECHEF_APP_KEY` fails with a clear message, `executeByName` mapping, error typing.

**Template** — `npm run check:template` in the `sdk-app` CI job packs `@bytechef/app` and `create-bytechef-app`,
scaffolds an app from the tarballs, runs the app's own `npm run check` (typecheck, build, bundle leak check) and proves
a development-mode build fails it. It then drives `bytechef:status`, the production server and the development server
against a mock ByteChef: the production page is a placeholder that contains nothing from ByteChef and the production
server build holds no development-only server function.

**End to end** — one manual run per release: template → `bytechef automation app create` →
`bytechef automation app bind connection …` → run the app in development: the overview renders the bound actions and
"Try it" executes one → `vercel deploy --prod` (the template's own deploy):
the deployed page is a placeholder → disable the app in the UI → `bytechef:status` fails within one request.

## Sub-project order

1. **A1** App model with its secret key, visibility, scopes, management REST and GraphQL, admin UI.
2. **A2** App-facing authentication, action and tool endpoints, logging.
3. **A3** Proxy.
4. **A4** Workflow bindings.
5. **B** SDK package.
6. **C** Template.
7. **D** Coding-agent loop (D1 server; D2 CLI, template, plugin).

A1–A3 are one implementation plan; A4, B and C one each; D two (D1, D2).

## Phase 2 (designed for, not built)

- **Sign-in for the app's users**: the app registers an OAuth2 client on ByteChef's authorization server for
  authorization code + PKCE, backed by the EE identity provider for company SSO; an `app_access` grant of who may use
  the app. This is where the authorization server's prerequisites land: signing keys that survive restarts and are
  shared across replicas (today `jwkSource()` generates one in memory at startup), tenant resolution on the
  back-channel token endpoint, and a startup check that the server is enabled.
- **Calls on behalf of the signed-in user**, carrying the user so the invocation log records who.
- **Keyless authentication on Vercel**: trust Vercel's OIDC issuer for a given team and project and accept the
  deployment's short-lived OIDC token instead of `BYTECHEF_APP_KEY`, so no ByteChef credential is stored in Vercel.
  Self-hosted apps keep the key. (Not yet checked against Vercel's current OIDC Federation documentation.)
- **OAuth2 client credentials** as an additional credential type, only if customers' security policies require
  short-lived tokens.
- **Per-app rate limits or quotas.**
- **AI gateway** spend per app.
- **Embedded** variant reusing the same endpoint shapes.

All three new credential types produce the same `AppPrincipal` as the v1 key (Decision 2).
