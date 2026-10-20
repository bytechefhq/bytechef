# Apps (EE, server side)

An **App** is a web application built outside ByteChef (with an AI coding tool, hosted on Vercel or the customer's
own infrastructure) that reaches third-party systems only through ByteChef. ByteChef holds the credentials, decides
what the app may call, records every call, and can switch the app off. Each app instance holds one secret key that is
useless anywhere but ByteChef. ByteChef does not host app code.

Read this before touching anything named `App*`, `/api/app/v1`, or the connection-delete guard.

Design rationale: `docs/superpowers/specs/2026-10-06-apps-governed-ai-built-apps-design.md`. Plan:
`docs/superpowers/plans/2026-10-06-apps-server.md`. This file covers sub-project A (server and admin UI), the SDK (its
**SDK** section) and the app template (its **Template** section; design
`docs/superpowers/specs/2026-10-07-apps-template-design.md`). Sub-project D, the coding-agent loop (design
`docs/superpowers/specs/2026-10-08-apps-agent-loop-design.md`), has its server half in the **Agent loop** section; the
CLI, template and plugin half is described in **CLI, template and plugin (Plan D2)**. The CLI and the plugin skills
live on branches stacked on this one, so a statement about them holds on the branches that include them.

## Terms

An **App** (`app`) is the logical application: name, description, category, status, visibility, owner, owning
workspace. An **app instance** (`app_instance`) is one configured installation of it: environment, hosting target,
URL, status, visibility, its own workspace and owner, its own secret key, and every binding. One app has one or more
instances; an instance may live in another workspace than its app. Design:
`docs/superpowers/specs/2026-11-16-apps-lifecycle-portal-and-access-design.md` (Designs 1 and 2). Phases 1 and 2 of
that design exist in the code: the split, the instance-keyed key authentication, facades, GraphQL, management REST, CLI
and the admin pages (Phase 1), and the access model (scopes, sharing, member removal), per-instance configuration and
the app icon (Phase 2; sections **Access scopes**, **Sharing**, **Member removal**, **Instance configuration** and
**The icon**). Phase 3, end-user sign-in, exists too (section **End-user sign-in**), and so does Phase 4, its use
from the SDK, the template and the CLI (sections **SDK**, **Template** and **CLI, template and plugin (Plan D2)**).
Phase 5, versions and recorded deployments, exists as well (section **Versions and deployments**), and so does
Phase 6, provider-driven deployments: deploy, rollback and cancel through Vercel or a self-hosted deploy hook (section
**Provider-driven deployments**). Phase 7, the App Portal, exists too (section **App Portal**), and so does Phase 8,
audit events, metrics and the Activity tab (section **Audit, metrics and the Activity tab**). Every limit still true is
gathered in the last section, **Known limits and open decisions**.

- `AppStatus`: `DRAFT, ACTIVE, SUSPENDED, ARCHIVED`. `AppInstanceStatus`: `ACTIVE, SUSPENDED, TERMINATED`. The key
  authenticates only when `AppKeyAccess.isAllowed(appStatus, instanceStatus)`: app `DRAFT` or `ACTIVE` and instance
  `ACTIVE`. Suspending an app therefore stops every instance at once; suspending an instance stops only it.
- Bindings (`app_connection`, `app_workflow`, `app_workflow_run`, `app_api_collection`) hang off `app_instance_id`.
  Binding keys are unique per instance, so the same code runs against a development and a production instance with
  different connections behind the same keys.
- Unique constraints: `uk_app_workspace_name` on `app`, `uk_app_instance_app_environment_name` on `app_instance`
  (several instances of one app may share an environment, told apart by name).
- The three changelogs under `changelog/automation/app/` were edited in place (nothing was released); a local database
  that ran the old changesets must be reset. `20261116000001_automation_app_deployment.xml` (`app_version`,
  `app_deployment` and the `app_instance.current_app_deployment_id` foreign key) is a separate changelog; see
  **Versions and deployments**.

## Module map

`server/ee/libs/automation/automation-app/`, wired into `server-app` only (not a distributed-EE app):

| Module | Holds |
|--------|-------|
| `automation-app-api` | `App`, `AppInstance`, `AppConnection`, `AppConnectionOperation` entities, enums (`AppStatus`, `AppInstanceStatus`, `AppUserAuthMode`, `AppHostingTarget`, `AppOperationType`), `AppKeyAccess`, `AppErrorType`, DTOs (`AppDTO`, `AppInstanceDTO`, `ForeignAppInstanceDTO`, `AppSecretKeyDTO`), `AppFacade` (app level, including the icon), `AppInstanceFacade` (instances, key, configuration, connection bindings), `AppSharingFacade` (visibility and grants of apps and instances), `AppPortalFacade` (the App Portal) and `AppExecutionFacade` (app-facing) interfaces, `AppFavorite`/`AppInstanceUsage` records, `AppFavoriteService`/`AppInstanceUsageService`, `PortalAppDTO`/`PortalAppInstanceDTO`/`AppCategoryDTO`, `AppDeploymentFacade`, the `AppVersion`/`AppDeployment` entities, `AppDeploymentStatus`, `AppDeploymentProviderType`, the provider SPI in package `...app.deployment` (`AppDeploymentProvider`, `ProviderDeploymentRequest`, `ProviderDeployment`, `ProviderDeploymentRef`, `AppDeploymentSettingsException`), `AppVersionDTO`, `AppDeploymentDTO`, `AppVersionService`/`AppDeploymentService`, `AppFileStorage`, `AppIconDTO`, `AppService`/`AppInstanceService`/`AppConnectionService`, `AppSecretKeys`, `AppBindingKeys`, `AppPrincipal`, `ProxyRequest`/`ProxyResponse` |
| `automation-app-service` | the service and facade impls, repositories, the visibility/ownership/environment resolvers (`AppVisibilityProvider`, `AppOwnershipResolver` for `"App"`; `AppInstanceVisibilityProvider`, `AppInstanceOwnershipResolver`, `AppInstanceEnvironmentResolver` for `"AppInstance"`), `AppConnectionUsageChecker`, `AppSharingFacadeImpl`, `AppFileStorageImpl` (wired by `AppFileStorageConfiguration`), `AppWorkspaceUserRemovalListener`, the audit events (`audit/AppAuditEvent`, `audit/AppAuditPublisher`), and the proxy (`AppProxyExecutor`, `ProxyPathValidator`, `ProxyHeaders`). Liquibase: `changelog/automation/app/20261006000001_automation_app_init.xml` (`app`, `app_instance`, `app_connection`), `..._automation_app_workflow.xml`, `..._automation_app_api_collection.xml`, `..._automation_app_deployment.xml` (`app_version`, `app_deployment`); `AppDeploymentFacadeImpl`; `deployment/AppDeploymentSynchronizer` and `deployment/AppDeploymentStatusJob`; `security/AppDeploymentConnectionUsageChecker`; `AppPortalFacadeImpl`; the `JdbcClient` repositories `AppFavoriteRepository` and `AppInstanceUsageRepository`; `..._automation_app_portal.xml` (`app_favorite`, `app_instance_usage`) |
| `automation-app-deployment/automation-app-deployment-vercel` | `VercelAppDeploymentProvider` (hosting target `VERCEL`); calls the `vercel` component's actions only |
| `automation-app-deployment/automation-app-deployment-hook` | `DeployHookAppDeploymentProvider` (hosting target `SELF_HOSTED`); calls the HTTP Client component's `post` and `get` actions only |
| `server/libs/modules/components/vercel` (component, not under `automation-app`) | the `vercel` component the Vercel provider calls: connection `BEARER_TOKEN`, actions `listProjects`, `createDeployment`, `getDeployment`, `cancelDeployment` |
| `automation-app-rest` | the hand-written, session-authenticated icon endpoints `AppIconApiController` (see **The icon**) and `AppPortalApiController` (see **App Portal**) |
| `automation-app-graphql` | admin UI surface (`app.graphqls`; `AppSharingGraphQlController` for visibility and grants, `AppGraphQlController` for apps, `AppInstanceGraphQlController` for instances, key and connection bindings, `AppWorkflowGraphQlController`, `AppApiCollectionGraphQlController`, `AppDeploymentGraphQlController` for versions and deployments; they only delegate to the facades) |
| `automation-app-public-rest` | two OpenAPI specs and generators: the app-facing `/api/app/v1` (`AppConnectionApiController`, `AppToolApiController`, `AppProxyApiController`) and the management `/api/automation/v1` (`AppApiController` under `/apps`; `AppInstanceApiController`, `AppWorkflowBindingApiController` and `AppApiCollectionBindingApiController` under `/app-instances/{id}/...`; `AppDeploymentApiController` for `/apps/{id}/versions` and `/app-instances/{id}/deployments`; `AppEnvironmentScope`); both extend `AbstractAppApiController`. Both generator tasks set `supportingFilesConstrainedTo=[ApiUtil.java]` so they do not collide on `org.openapitools.configuration.*` |

Touched outside the module:

- `automation-security-web-impl`, package `...security.web.app`: `AppApiKeySecurityConfigurer` (+ its
  `Contributor`), `AppApiKeyAuthenticationProvider` (looks the instance up by key hash), `AppAuthenticationToken`.
- `platform-security-web-api`: `AbstractApiKeyAuthenticationToken` gained one protected constructor taking an
  arbitrary principal and authorities (the existing authenticated constructors require a Spring `User`).
- `automation-configuration-service` (EE): `AppPermissionScope { APP_VIEW -> VIEWER, APP_USE -> VIEWER, APP_DEPLOY -> EDITOR, APP_MANAGE -> ADMIN }` and its
  provider (`@ConditionalOnEEVersion`, checked by `PermissionScopeProviderEditionGatingTest`).
- `automation-configuration-service` (CE): `WorkspaceConnectionFacadeImpl.delete` now runs the connection delete
  (which the guard can refuse) before cancelling the scheduled OAuth refresh, since the cancel reaches an external
  scheduler that no rollback undoes; `platform-connection-api`/`-service`: `ConnectionUsageChecker` SPI and
  `ConnectionFacadeImpl` wiring.
- `platform-external-invocation-api` (`server/libs/platform/platform-external-invocation`):
  `ExternalInvocationSource.APP_ACTION|APP_TOOL|APP_PROXY|APP_WORKFLOW|APP_API`, `ExternalInvocationKind.HTTP_PROXY`,
  `ExternalInvocationEvent.appId`, `appInstanceId` and `userId` (written through `ExternalInvocationRecorder`).
  `platform-external-invocation-log-*` (EE): `app_id` column (`20261114000001_external_invocation_log_added_app_id.xml`),
  `app_instance_id` and `user_id` columns (`20261116000001_external_invocation_log_added_app_instance_id_and_user_id.xml`),
  an `appId` filter on `externalInvocationLogs`. Reads go through `ExternalInvocationLogFacade`:
  `workspaceExternalInvocationLogs(workspaceId, environmentId, ...)` needs a platform user holding `WORKFLOW_VIEW` on that
  workspace (`@PreAuthorize` on `ExternalInvocationLogFacadeImpl`; the query always adds `workspace_id` and `environment`,
  and shows a row of an app instance only to a caller who may view that instance)
  and is what the automation External Invocations page (`client/src/pages/automation/external-invocations`) and the app
  Activity tab call; the tenant-wide `externalInvocationLogs` is `isTenantAdmin()` and serves the embedded
  External Invocations page (`EmbeddedExternalInvocations`) and Connected User sheet. Its optional `environmentId`
  narrows it to one environment (null means all): the embedded page passes the current embedded environment and
  remounts on a switch, the Connected User sheet filters by `connectedUserId` alone (a connected user lives in one
  environment), and the embedded public REST `/{externalUserId}/external-invocations` passes the environment it
  resolves the connected user in. A row with a null environment matches no environment, so it never appears on the
  environment-filtered embedded page. Rows without a workspace (the embedded surfaces) are visible only through the
  tenant-admin `externalInvocationLogs` query. The embedded writers (`EmbeddedMcpToolFacade`, embedded
  `ActionFacadeImpl`, `ToolFacadeImpl`) record the row's `connectedUserId` (the MCP one leaves it null when no
  connected user exists yet for the external user), which is what the Connected User sheet and the public REST
  endpoint filter on. The Activity tab filters by `appId` and by the selected `appInstanceId` (see **Audit, metrics and
  the Activity tab**).
  Enum ordinals are persisted: append only.
- `platform-user-api`/`-service` (CE): `UserService.getUsers(Collection<Long> ids)`, the batch lookup behind the Activity
  tab's User column; the EE `platform-user-remote-client` stub implements it and throws `UnsupportedOperationException`.
- `platform-file-storage-*`: `TempFileStorage.deleteFile(FileEntry)`.
- `app-config`: `ApplicationProperties.App` (below). `server-app` and `config-server-app` yml: the
  `openapi.openAPIDefinition.base-path.app` entry and `bytechef.app.*` defaults.
- Client: `client/src/pages/automation/apps`, the Apps row in the automation Deploy group, an App filter on the External
  Invocations page (`?appId=`).

## The key

The key belongs to an **app instance**, not to the app: one key per instance, so instance secrets stay isolated and
rotating or leaking one touches only that instance. `AppSecretKeys.generate()` returns a `TenantKey.of()` (so the
tenant is readable from the key before any lookup), its hex SHA-256 (`app_instance.secret_key_hash`, 64 chars, unique
index `uk_app_instance_secret_key_hash`) and a hint (last 8 characters, `=` padding stripped; not the first
characters, which encode the tenant id and are identical for every key of a tenant). The hash is **unsalted on
purpose**: the key has 24 bytes of CSPRNG entropy and the request-path lookup is an equality `SELECT` on the hash,
which a salt would defeat. This is the first hashed secret in the codebase; `api_key` and `mcp_server.secret_key` are
plain. The key is returned only by `createApp` (the first instance's key), `createAppInstance` and
`rotateSecretKey` (`AppSecretKeyDTO`, GraphQL `AppInstanceSecretKey`, REST `AppInstanceSecretKey`). Rotation replaces
hash and hint; the old key is dead on its next request (no overlap window). `secret_key_last_used_date` is written by
`AppInstanceService.touchSecretKeyLastUsedDate`, a targeted `UPDATE` throttled by its own
`secret_key_last_used_date < :cutoff` predicate, so the whole cluster writes at most once per instance per minute,
best-effort (a failing `UPDATE` is logged at WARN and does not change the request outcome; the method is
`REQUIRES_NEW`, so a failure to begin or commit that transaction escapes the catch, see **Known limits and open decisions**); it bumps
neither `version` nor the audit columns.

**The lookup is not cached.** Caches here are per-replica Caffeine with no eviction broadcast, so a cache would make
disable and rotate take effect on one node only. Do not add one.

## App-facing authentication

`/api/app/v1/**` is a root of its own, not under `/api/automation/v1`: `AutomationApiKeySecurityConfigurer` claims
every `^/api/automation/v[0-9]+/.+` path for user API keys, and the two credential types must never be accepted on
each other's endpoints. The base path is `${openapi.openAPIDefinition.base-path.app:/api/app}/v1`; the default is
deliberately non-empty (the sibling `${...:}` pattern would mount at `/v1/**`, outside the `/api/**` chain, if the
property went missing).

- `AppApiKeySecurityConfigurer` matches `^/api/app/v[0-9]+/.+`, ignores CSRF on that pattern (without it every POST
  is rejected), and ignores `X-ENVIRONMENT`: an instance's environment belongs to its row.
- The converter turns every malformed-key failure (a `TenantKey.parse` `RuntimeException`, an invalid tenant id) into
  `BadCredentialsException`. The filter switches to the key's tenant before the provider runs, outside its
  authentication-failure handling, so a tenant `TenantContext` would reject must be refused in the converter or it
  surfaces as a 500.
- The provider hashes the key and looks the **app instance** up (`AppInstanceService.fetchAppInstanceBySecretKey`),
  then its app, and answers an unknown key as 401. A key that `AppKeyAccess` does not allow (app `SUSPENDED` or
  `ARCHIVED`, or instance `SUSPENDED` or `TERMINATED`) is a `DisabledException`, which the API key filter maps to
  **401**. `AppExecutionFacadeImpl` re-checks the status and answers **403 `APP_DISABLED`** for an app or instance
  disabled between authentication and execution. Both are intentional; the 401 is the common case.
- The principal is `AppPrincipal(tenantId, appId, appInstanceId, workspaceId, environment, user)`; `workspaceId` and
  `environment` are the **instance's**, which may differ from the app's workspace. `AppAuthenticationToken` carries
  `ROLE_APP`. Every app-facing facade reads the binding of `appInstanceId`, so two keys of one app see only their own
  instance's bindings. The principal also carries an optional `AppUser(id, login)`, set only when the call carries a
  valid user token (see **End-user sign-in**). The app is not a user: `SecurityUtils.extractPrincipal` returns null for
  it, so writes made during an app call are audited as `SYSTEM`, and `SecurityUtils.getCurrentUserLogin()` throws,
  signed-in user or not. Attribution is the invocation log's job (`app_id`, `app_instance_id`, `user_id`), not the
  audit columns.
- `AppApiKeyFilterChainTest` runs both key configurers in one real filter chain with CSRF on: each key type is 401 on
  the other's endpoints, an app POST needs no CSRF token, and disabled, unknown, rotated-out and malformed keys are
  401.

## End-user sign-in

Design: `docs/superpowers/specs/2026-11-16-apps-lifecycle-portal-and-access-design.md`, Design 3. The server half is
described here; the SDK (`auth`, `context`), the TanStack Start adapter and the template use it, and the CLI sends
the sign-in mode (see **SDK**, **Template** and the CLI part of **CLI, template and plugin (Plan D2)**).

### The three identities

| Identity | In ByteChef | Credential |
|----------|-------------|------------|
| User | A Studio user who is a workspace member | An access token from the authorization server |
| Application | The `app` and `app_instance` rows | None of its own: it is what the other two are checked against |
| Service | The app's server-side code | The instance's secret key (`AppPrincipal`) |

The app's server runs the flow (backend for frontend): it redirects the browser to `/oauth2/authorize`, exchanges the
code at `/oauth2/token` with the PKCE verifier and keeps the tokens in its own session. The browser never holds a
token or the key.

### The per-instance OAuth2 client

`AppInstanceFacade` registers one OAuth2 client per instance through `RegisteredClientService.registerClient`,
updates it through `updateClient` and deletes it through `deleteClient`; `AppInstanceClients` (`automation-app-service`)
is the helper that builds the request.

- Client id `app-<instance uuid>`, stored in `app_instance.registered_client_id` (the client id, not the row id of the
  client table). `AppInstanceClients.parseInstanceUuid` reads the uuid back.
- A public client (no client secret) that must use PKCE, with grant types `authorization_code` and `refresh_token`,
  scopes `openid profile app:use`, consent off.
- Redirect URIs: `<instance url>/auth/callback`, plus `http://localhost:3000/auth/callback` for a DEVELOPMENT instance
  only (`AppInstanceClients.DEVELOPMENT_REDIRECT_URIS`).
- The client name is the app name, with the instance name in parentheses when it has one.
- It is registered when sign-in is on and the instance has a URL, updated when `url` or `name` change, and deleted
  when the URL is cleared, when the instance or its app is deleted, when the instance is terminated and when the mode is
  switched to `NONE`. An instance without a URL has no client. Only a client made by `registerClient` can be updated
  (it carries a registration marker), and `updateClient` refuses to change scopes (`SCOPES_NOT_UPDATABLE`).
- Token lifetimes are Spring's defaults (5 minutes for the access token, 60 minutes for the refresh token), so an app's
  session lasts at most an hour without a new sign-in.
- Refresh tokens rotate (`reuseRefreshTokens(false)`): every refresh returns a new refresh token and the one sent is
  spent. **Reuse grace** (`AppRefreshTokenReuseAuthenticationProvider`, `platform-oauth2-authorization-server`, wrapping
  Spring's `OAuth2RefreshTokenAuthenticationProvider` on the token endpoint): for an app client (scope `app:use`,
  public, rotating) a refresh runs under a `SELECT ... FOR UPDATE` lock on the `oauth2_authorization` row, and a
  rotation records the SHA-256 of the spent refresh token and the rotation time in that row's attributes. The spent
  token presented again within `APP_REFRESH_TOKEN_REUSE_GRACE` (10 seconds) answers the authorization's current access,
  refresh and ID tokens (the pair the first request received), with `expires_in` the access token's remaining
  lifetime; nothing is issued and no lifetime grows. Refused with `invalid_grant`: after the grace, a token from two
  rotations back, another client's id, a DPoP proof, other scopes, or an inactive current token. A refused replay
  only fails that request; Spring has no replay detection, so the grant and its current refresh token stay valid.
  App refresh tokens carry the authorization id (`TenantKeyedTokenValues.generate(tenantId, authorizationId)`) so the
  row is found by primary key; the hash check, not the id, authorizes the reuse. MCP and other clients keep Spring's
  behaviour: a replay is `invalid_grant`, and concurrent refreshes are not serialised.

### `user_auth_mode`

`AppUserAuthMode`: `NONE`, `OPTIONAL`, `REQUIRED`. An instance created through `AppFacade.createApp` or
`AppInstanceFacade.createAppInstance` with no mode gets `REQUIRED` when the authorization server is available and
`NONE` when it is not (`AppInstanceClients.getDefaultUserAuthMode`, which uses the same `isAvailable` test as
`checkAvailable`); the column default is `NONE`, so a row that does not come through those facades keeps today's
behaviour. `AppFacade.isUserAuthAvailable(workspaceId)` (GraphQL `appUserAuthAvailable(workspaceId): Boolean!`,
workspace `APP_VIEW`) answers the same test; the app and instance create dialogs preselect `None` from it, disable
`Optional` and `Required`, and say that sign-in is unavailable because the authorization server is off. It is not the
`oauth2.authorizationServer.enabled` application-info flag, which also requires a self issuer. The mode can be changed with `updateAppInstanceUserAuth`
(GraphQL mutation of the same name, and REST `PUT /app-instances/{id}/user-auth`; `APP_MANAGE`; DEVELOPMENT instances only over REST, 403
`APP_OPERATION_DEVELOPMENT_ONLY` otherwise; audited as `APP_USER_AUTH_CHANGED`). A terminated instance cannot change
mode. Any mode but `NONE` needs the authorization server: with it off, create and update answer 409
`APP_USER_AUTH_UNAVAILABLE` (`AppInstanceClients.checkAvailable`); only `NONE` works. The `automation app create` and
`automation app instance create` CLI commands take `--user-auth-mode`; without it they send no mode and the server
applies its default (`REQUIRED`, or `NONE` with the server off), which the output's `userAuthMode` shows.

### Where the authorization server is switched on

`bytechef.oauth2.authorization-server.enabled`. It is `false` in the shared
`server/apps/server-app/src/main/resources/config/application-bytechef.yml` (so a stock boot, which is EE, and a CE boot
have it off), `true` in `application-dev.yml`, and `true` in the EE Helm overlay `kubernetes/helm/bytechef/values-ee.yaml`
and the compose override `docker-compose.cluster.ee.yml`. `application-prod.yml` and the `testint` profile do not set
it; a test that needs the server turns it on itself. **`bytechef.public-url` must be set**: the token issuer is
compared with it (`AppUserTokenVerifier.getIssuer`), and when it is blank every user token is invalid. The dev profile
sets it to `http://127.0.0.1:5173`, which is therefore the issuer on a local server. `docker-compose.cluster.yml`
defaults `BYTECHEF_PUBLIC_URL` to `http://localhost:8080`. The Helm overlay carries a commented
`BYTECHEF_PUBLIC_URL` placeholder next to the authorization server setting; it is set there or in `values.yaml`.

### The check at sign-in

`AppInstanceClientAccessChecker` implements the `OAuth2ClientAccessChecker` extension point of
`platform-oauth2-authorization-server`, which the authorization endpoint applies once the user has a session and before
any code is issued (the module itself knows nothing of apps). It supports every client that holds the `app:use` scope (`AppClients.isAppClient`).
It refuses (the user is sent back to the app's redirect URI with `access_denied`) when: the client id does not resolve
to an instance, the instance's mode is `NONE` or its `registered_client_id` is not this client, the app is not `DRAFT`
or `ACTIVE` or the instance is not `ACTIVE` (`AppKeyAccess`), the user is unknown or not activated, or the user does
not hold `APP_USE` on the instance. A checker that throws counts as a refusal.

### The two credentials on `/api/app/v1`

Every call carries `Authorization: Bearer <instance key>` and, for a signed-in user, `X-ByteChef-User-Token: <access
token>`. `AppApiKeyAuthenticationProvider` runs the key step (the key section above) and then the user step, in this
order:

1. No user token: the call has no user. If the instance is `REQUIRED`, 401 `APP_USER_REQUIRED`. The exceptions are
   the read-only requests that describe the instance and carry nothing of any user: `GET /context`, so an app can read
   its sign-in settings, and the discovery lists `GET /connections`, `/actions`, `/tools`, `/workflows` and
   `/api-collections`, so `bytechef:status` and a coding agent can see what the app may call (one anchored regex in
   `AppApiKeySecurityConfigurer`). Every call that runs something, `/me`, `/workflow-runs/{id}` and the app API path stay
   refused without a user.
2. A user token: `AppUserTokenVerifier` decodes it with the authorization server's `JwtDecoder` and requires a valid
   signature and expiry, issuer equal to `bytechef.public-url` (trailing slash removed), the tenant claim equal to the
   key's tenant, an audience that contains the instance's `registered_client_id`, the scope `app:use`, a subject, and a
   token that is not revoked. Any failure, and an absent authorization server or revocation checker, is 401
   `APP_USER_TOKEN_INVALID`.
3. It loads the user by login (an unknown or deactivated user is 403) and checks `APP_USE` on the instance **as that
   user** (`SecurityUtils.runAs`). Failure is 403 `APP_USER_FORBIDDEN`.

`AppApiSecurityErrorHandler` writes the three codes: `APP_USER_REQUIRED` 401, `APP_USER_TOKEN_INVALID` 401,
`APP_USER_FORBIDDEN` 403. **An invalid token is refused in every mode, `OPTIONAL` and `NONE` included; it is never
treated as "no user".** The per-call check in step 3 is the only enforcement after sign-in: refreshing a token
re-checks nothing, so a role change, a revoked grant or a narrowed visibility takes effect on the next call, not at token
expiry.

### `GET /context` and `GET /me`

Both are in the app-facing OpenAPI (`AppContextApiController`, `AppExecutionFacade.getContext` and `getCurrentUser`).

- `GET /context` needs only the key. It returns app id and name, instance id and name, environment,
  `configuration`, `userAuthMode`, and the sign-in settings `issuer`, `clientId`, `authorizationEndpoint` and
  `tokenEndpoint`. The four settings are null while the instance has no client; the endpoints are also null when the
  authorization server is off.
- `GET /me` needs a user token and answers 401 `APP_USER_REQUIRED` without one, whatever the mode. It returns the
  user's `id`, `login`, `firstName`, `lastName`, `email`, `workspaceRole` (the built-in role name in the instance's
  environment, null for a custom role or none) and `scopes` (the ones of `APP_VIEW`, `APP_USE`, `APP_DEPLOY`,
  `APP_MANAGE` that the user holds on the instance).

### What the user identity changes

It is recorded (`external_invocation_log.user_id`), it gates the call (step 3) and the app can read it. It does not
change what runs: actions and tools still run as the system on `appExecutor`, workflow jobs still run as the project
deployment, and bindings are still the allow-list. Per-user connections and per-binding user rules are not built.

### What the authorization server issues to an app client

`AppClients` (in the authorization server module) recognises an app client by its `app:use` scope. An ID token is issued (the
`openid` scope) with the user's name and email, added by `AppUserIdTokenCustomizer`. The access token's audience is the
client id (`McpResourceAudienceTokenCustomizer` leaves app clients alone), and it carries the tenant claim and the scope
`app:use`. There is no OIDC discovery document (`/.well-known/openid-configuration` answers 404); use the RFC 8414
document `/.well-known/oauth-authorization-server` or the endpoints from `GET /context`, and validate the ID token
against `/oauth2/jwks`. `/userinfo` is off. An app token is refused on the MCP endpoints (a 403 from the scope check).

### Revocation

`AppInstanceClients.revokeGrants` revokes every grant of a client when an instance is suspended or terminated and when
its app is suspended or archived. `AppWorkspaceUserRemovalListener` revokes the removed member's grant on each of the
workspace's instance clients after the removal commits. Rotating the instance key does not touch user tokens. Revocation
makes the token fail step 2 (401); withdrawing access without a removal (role, visibility, grant) leaves the token valid
and is cut by step 3 (403). A tenant admin can also inspect and revoke per user on the Registered Clients settings page. That page cannot delete
an app instance's client: `RegisteredClientService.deleteRegisteredClient` refuses a programmatically registered client
with `REGISTERED_CLIENT_MANAGED_BY_SERVER`, because the instance would keep `registered_client_id` and sign-in would
break; the client goes away with the instance, its URL or `NONE`.

## Execution lane

`AppExecutionFacadeImpl` does the identity, allow-list and connection checks on the request thread and runs only the
component call on **`appExecutor`** (a bulkhead of its own, limit `bytechef.app.execution.concurrency-limit`, default
10, so a burst from apps takes no permit from scheduled runs on `workerExecutor` nor from the API Platform sync path
and the editor's Test button on `syncWorkerExecutor`), bounded by `bytechef.app.execution.timeout` (read and validated
before submit: once a component has started, its side effects cannot be undone, so a missing timeout must fail the
request, not the component). The facade is deliberately not transactional. Waiting for a lane permit counts against
that timeout: a fair `Semaphore` sized to the same concurrency limit sits in front of `appExecutor` (taken with
`tryAcquire(timeout)`, released when the lane task ends, including a task cancelled before it ran), so a saturated lane
answers 504 `APP_EXECUTION_TIMEOUT` ("No app execution slot became free within …") instead of parking the request
thread in `SimpleAsyncTaskExecutor`'s throttle. The component gets whatever remains of the timeout. A timeout cancels
the future with interrupt and answers 504 `APP_EXECUTION_TIMEOUT`.

The call runs under `SecurityUtils.runAsSystem`, like components in workflow workers. Why: `ContextPropagatingTaskDecorator`
carries the `AppPrincipal` onto the lane, and OAuth2 token refresh (`TokenRefreshHandler` ->
`ConnectionServiceImpl.validateOwnerOrAdmin`) throws for a non-user principal. Consequence: component code on the lane
sees the system principal with admin authority; none branch on caller identity for automation execution today.

The only connection check on the path is `checkAvailable`: the connection exists, is in the instance's workspace
(`workspace_connection`), is in the instance's environment, and is `ACTIVE`; otherwise 409 `APP_CONNECTION_UNAVAILABLE`.
`executePerform` and `executeTool` trust whatever connection id they are given.

An unknown operation answers 403 `APP_OPERATION_NOT_ALLOWED` with a message that lists the binding's allowed
operations of that type (`'x' is not allowed on binding 'crm'; allowed actions: a, b`) so a coding agent can
self-correct. Typed bindings and an instructions endpoint were deliberately not built.

## Tool names

Tools are addressed by `(bindingKey, toolName)` in the URL. The listing returns a flat `name` for LLM function
calling, `<BINDING_KEY>__<TOOL_NAME>`: the binding key through `AppBindingKeys.toFlatKey` (non-alphanumeric runs ->
`_`, upper case) and the tool name camel-to-UPPER_SNAKE (`SALES_SLACK__SEND_MESSAGE`). Clients never parse a flat
name; they map it back through the listing.

**Collision rule:** the binding key regex (`^[a-z][a-z0-9_-]{0,63}$`) lets `support-crm` and `support_crm` both
exist, and both flatten to `SUPPORT_CRM`. `AppConnectionServiceImpl.bind` therefore rejects a key whose flat form
collides with another binding on the same app (`APP_BINDING_KEY_ALREADY_EXISTS`). The check is check-then-save
with no DB constraint, so two concurrent first binds can still race.

## Action listing

`GET /api/app/v1/actions` (tag `app-connection`, so it lands on `AppConnectionApiController` beside `executeAction`;
model `AppAction`: `bindingKey`, `componentName`, `actionName`, nullable `description`, `inputSchema`) returns every
allowed action of the app's bindings with its input JSON Schema. `AppExecutionFacadeImpl.getActions()` resolves each
binding's component at its **latest version** (what `actions.execute` uses without a `componentVersion`) and keeps the
allowed names; a component that cannot be resolved, or an allowed name missing from the latest version, is skipped,
as `getTools()` skips an unresolvable component. The listing is not written to the invocation log, and it is
authenticated as the app, like `GET /tools`.

- **`ActionInputSchemaGenerator`** (`platform-component-api`, `component/util`) builds the schema recursively from the
  platform `Property` tree: nested objects and array items, static options as `enum` (DATE/DATE_TIME/TIME options as
  ISO strings), `default`, `format` for the date types, `DYNAMIC_PROPERTIES` as an open object, and a property with a
  display condition never in `required`. **Hidden properties are excluded everywhere in the tree**, as are options that
  come from a data source. Tools keep the flat `JsonSchemaGeneratorUtils.generateInputSchema`; it flattens objects and
  arrays and must not be used for actions. A follow-up may move tools over.
- **No component visibility filtering on this line.** There is no `ComponentVisibilityProvider` SPI here, so the
  listings and execution offer every allowed operation of a resolvable component, and no component- or
  action-disabled 403 exists (see **Known limits and open decisions**).
- **Listings do not gate on connection state.** `getBindings`, `getTools` and `getActions` list a binding's allowed
  operations whatever the connection's status; only execution answers 409 `APP_CONNECTION_UNAVAILABLE`.
- **One bad definition does not fail a listing.** `getActions()` and `getTools()` catch a `RuntimeException` from
  schema generation per definition, list that action or tool with `{"type": "object"}`, and log a WARN naming the
  component and the action or tool.

## The proxy

`POST /connections/{bindingKey}/proxy` sends a raw HTTP request through a bound connection with the connection's
credentials attached. The credentials must only ever reach the connection's own base URI.

- **Pinning.** `ProxyPathValidator.validate` accepts only a plain absolute path; `pin(baseUri, path)` joins it to the
  connection's `executeBaseUri` and proves scheme, host and port equal the base's and the path stays under the base
  path (a base with userinfo, query or fragment is rejected). The executor sends the **pinned absolute URI**, not the
  relative path: `HttpClientExecutor` passes any URL containing `://` through unchanged while still attaching
  credentials, so the string validated is exactly the string sent. Redirects are never followed; a 3xx goes back to
  the app.
- **Path rules.** ASCII punctuation allow-list; `? # @ \` whitespace and control characters rejected; percent-encoded
  `/ \ ? # @ %` rejected (no second decoding layer); empty inner segments (`//`) rejected. Each decoded segment is cut
  at the first `;` (decoded `%3b` included: upstreams drop path parameters before resolving dot segments), trimmed,
  NFKC-normalised, and re-checked against `.`, `..`, `/`, `\`, `@`, `;`. Beyond ASCII only letters, digits and
  combining marks are allowed (U+3002 and similar look-alikes are not folded by NFKC). Cost: legitimate non-ASCII
  punctuation or symbols (euro sign, em dash) in upstream paths are unreachable through the proxy.
- **Not covered:** combining marks on bare dots, and IIS-style trailing dot/space trimming. Decide whether IIS
  upstreams are in scope before relying on the proxy for them.
- **Headers.** Request side drops `authorization`, `proxy-authorization`, `cookie`, `host`, `content-length`,
  `expect`, hop-by-hop headers, `forwarded`, `x-real-ip`, `x-original-url` and anything starting `x-forwarded-`,
  case-insensitively. Response side drops `set-cookie`, `set-cookie2` and hop-by-hop headers. Names must be RFC 9110
  tokens and values free of control characters, else 400 `APP_PROXY_REQUEST_INVALID`; the content type must parse as
  `type/subtype`.
- **Credentials.** The proxy sets `disableAuthorization(true)` and applies credentials itself
  (`ConnectionDefinitionService.executeAuthorizationApply`), removing app headers and query keys that collide
  case-insensitively with the applied ones. The client's own authorization path installs an error interceptor that
  turns every 4xx/5xx into an exception before the body is read, which would hide upstream errors from the app. A
  connection with no base URI or no `authorizationType` answers 409 `APP_PROXY_UNSUPPORTED`.
- **Token refresh.** Refresh lives in `TokenRefreshAspect` around `@WithTokenRefresh` beans, which the proxy does not
  pass through, so the proxy refreshes itself: exactly once, when the connection `canCredentialsBeRefreshed()` and
  the upstream status matches the authorization's refresh-on rules; it then rebuilds the context, re-resolves and
  re-pins the base URI and resends. A failed refresh answers 409 `APP_CONNECTION_UNAVAILABLE` with a fixed message.
  The refresh itself still has a pre-existing race (the identity-provider call is unlocked).
- **Spool files.** The response is requested BINARY so the client spools it to temp file storage; the client
  aborts the exchange as soon as the body passes `bytechef.app.proxy.max-response-bytes` (the SDK's
  `Http.Configuration` `maxResponseBytes`, counted while spooling; `HttpResponseTooLargeException` in the cause chain
  becomes 502 `APP_PROXY_RESPONSE_TOO_LARGE`), the read-back keeps the same cap as a backstop, and the request body
  cap is checked up front (413 `APP_PROXY_REQUEST_TOO_LARGE`). Request spool `proxy-request-<uuid>`, response spool
  `proxy-response-<uuid>[.<ext>]` (the upstream's Content-Disposition name is never used). Unique names matter
  because some storage providers (AWS) delete temp files by name alone. Both spools are deleted in `finally`.
- **In-flight cap.** `HttpClientExecutor` wraps a BINARY body in a counting stream when the configuration carries
  `maxResponseBytes`; past the limit the stream closes the upstream (cancelling the exchange) and reports end of
  stream, the client deletes the partial spool (a delete failure is logged and never masks the size error) and throws
  `HttpResponseTooLargeException`. No spool survives an oversized response on any storage provider.
- **Errors.** An `IOException` or `HttpTimeoutException` anywhere in the cause chain is an upstream transport
  failure: 502 `APP_PROXY_UPSTREAM_UNAVAILABLE` with a fixed message (the original can carry upstream body text).
  Message-text matching is not used. A local spool
  read/store failure is `ProxyStorageException`, rethrown by the facade as 500 `APP_PROXY_STORAGE_FAILED` (123). Upstream
  status codes, including 4xx/5xx, come back as a 200 proxy response with `status` set.
- The invocation log records `"<METHOD> <path without query>"` only; never the query, headers or body.

## Error mapping

Controller-local `@ExceptionHandler`s in `AbstractAppApiController`, not an advice: `GlobalResponseEntityExceptionHandler`
is `HIGHEST_PRECEDENCE` and would answer `AccessDeniedException` with 500 and every `AbstractException` with 400, and
a standalone MockMvc test hides that. Spring consults the controller's own handlers first. Every new app-facing
controller must extend it. Body is `{"code", "message"}`. A component failure is an `ExecutionException` (not a
`ConfigurationException`), so the controller handles `AbstractException` too: any non-App `AbstractException` is 422
`COMPONENT_ERROR` with the message only, never the input parameters the global handler would echo.
`ActionDefinitionServiceImpl` wraps every perform failure, `ProviderException` included, in `ExecutionException`, so
`translate` walks the cause chain: a `ProviderException` anywhere in it answers 422 `PROVIDER_ERROR` with that
exception's message only. App errors are decided before that walk. A
bean-validation failure on a management body (`maxLength` on name and url) is 400 `INVALID_REQUEST`.

The keys of `AppErrorType` (pinned by `AppErrorTypeTest` and the `AppApiControllerIntTest` mapping table; the status
column is `AbstractAppApiController.toAppStatus`, which is the source: any key it does not list is 400). The two
sign-in 401s and the 403 are described under **End-user sign-in**.

| Key | Name | Status |
|-----|------|--------|
| 100 | `APP_NAME_ALREADY_EXISTS` | 409 |
| 101 | `APP_NOT_FOUND` | 404 |
| 102 | `APP_DISABLED` | 403 |
| 103 | `APP_CONNECTION_NOT_FOUND` | 404 |
| 104 | `APP_OPERATION_NOT_ALLOWED` | 403 |
| 105 | `APP_CONNECTION_UNAVAILABLE` | 409 |
| 106 | `APP_PROXY_PATH_REJECTED` | 400 |
| 107 | `APP_PROXY_UNSUPPORTED` | 409 |
| 108 | `APP_EXECUTION_TIMEOUT` | 504 |
| 109 | `APP_BINDING_KEY_INVALID` | 400 |
| 110 | `APP_BINDING_KEY_ALREADY_EXISTS` | 409 |
| 111 | `APP_CONNECTION_NOT_BINDABLE` | 400 |
| 112 | `APP_PROXY_REQUEST_TOO_LARGE` | 413 |
| 113 | `APP_PROXY_RESPONSE_TOO_LARGE` | 502 |
| 114 | `APP_VISIBILITY_UNSUPPORTED` | 400 |
| 115 | `APP_PROXY_REQUEST_INVALID` | 400 |
| 116 | `APP_PROXY_UPSTREAM_UNAVAILABLE` | 502 |
| 117 | `APP_FIELD_TOO_LONG` | 400 |
| 118 | `APP_WORKFLOW_NOT_FOUND` | 404 |
| 119 | `APP_WORKFLOW_UNAVAILABLE` | 409 |
| 120 | `APP_WORKFLOW_INPUT_INVALID` | 400 |
| 121 | `APP_WORKFLOW_RUN_NOT_FOUND` | 404 |
| 122 | `APP_WORKFLOW_NOT_BINDABLE` | 400 |
| 123 | `APP_PROXY_STORAGE_FAILED` | 500 |
| 124 | `APP_API_COLLECTION_NOT_FOUND` | 404 |
| 125 | `APP_API_COLLECTION_NOT_BINDABLE` | 400 |
| 126 | `APP_API_COLLECTION_UNAVAILABLE` | 409 |
| 127 | `APP_API_ENDPOINT_NOT_FOUND` | 404 |
| 128 | `APP_EXECUTION_FAILED` | 500 |
| 129 | `APP_INSTANCE_NOT_FOUND` | 404 |
| 130 | `APP_INSTANCE_NAME_ALREADY_EXISTS` | 409 |
| 131 | `APP_INSTANCE_TERMINATED` | 409 |
| 132 | `APP_OPERATION_DEVELOPMENT_ONLY` | 403 |
| 133 | `APP_INSTANCE_CONFIGURATION_INVALID` | 400 |
| 134 | `APP_INSTANCE_CONFIGURATION_TOO_LARGE` | 413 |
| 135 | `APP_ICON_INVALID` | 400 |
| 136 | `APP_USER_AUTH_UNAVAILABLE` | 409 |
| 137 | `APP_USER_REQUIRED` | 401 |
| 138 | `APP_USER_TOKEN_INVALID` | 401 |
| 139 | `APP_USER_FORBIDDEN` | 403 |
| 140 | `APP_VERSION_NOT_FOUND` | 404 |
| 141 | `APP_DEPLOYMENT_NOT_FOUND` | 404 |
| 142 | `APP_VERSION_NAME_ALREADY_EXISTS` | 409 |
| 143 | `APP_VERSION_COMMIT_MISMATCH` | 409 |
| 144 | `APP_DEPLOYMENT_TERMINAL` | 409 |
| 145 | `APP_DEPLOYMENT_URL_INVALID` | 400 |
| 146 | `APP_LIST_LIMIT_INVALID` | 400 |
| 147 | `APP_DEPLOYMENT_PROVIDER_UNAVAILABLE` | 409 |
| 148 | `APP_DEPLOYMENT_IN_PROGRESS` | 409 |
| 149 | `APP_DEPLOYMENT_PROVIDER_FAILED` | 502 |
| 150 | `APP_DEPLOYMENT_SETTINGS_INVALID` | 400 |
| 151 | `APP_CATEGORY_INVALID` | 400 |
| 152 | `APP_INSTANCE_URL_INVALID` | 400 |

Outside the table: 401 for an unknown, malformed or disabled key (the filter, not a controller); 422 `COMPONENT_ERROR`
(a component's own error, message only) and `PROVIDER_ERROR` (a `ProviderException` itself or in the cause chain,
message only); 400 `INVALID_REQUEST` (bean validation) and 400 for an `IllegalArgumentException` such as a blank version
name. `APP_API_COLLECTION_NOT_FOUND` is mapped but not thrown today.

## Admin side

- Gates: `APP_VIEW` (any workspace member) and `APP_MANAGE` (workspace admin; creating, binding and rotating hand the
  app credentials, so EDITOR is not enough); `APP_USE` (checked at sign-in and per call) and `APP_DEPLOY` (versions and deployments; see **Versions and deployments**) are separate scopes (see **Access
  scopes**). Visibility is not changed here: see **Sharing**. The facades own authorization and the GraphQL and REST controllers only
  delegate. `AppFacade` works on the app (`createApp`, `getApp`, `getApps`, `updateApp(id, name, description, category, portalDiscoverable)`,
  `updateAppStatus`, `deleteApp`, `updateAppIcon`, `deleteAppIcon`, `getAppIcon`, `getAppCategories`, the bindable-connections and
  component-operations lookups), guarded on the `"App"` resource.
  `AppInstanceFacade` works on instances (`createAppInstance`, `getAppInstance`, `getHostedAppInstances`,
  `updateAppInstance`, `updateAppInstanceConfiguration`, `updateAppInstanceStatus`, `deleteAppInstance`, `rotateSecretKey`,
  and the connection bindings
  `getBindings`, `getBindableConnections`, `bindConnection`, `unbindConnection`), guarded on the `"AppInstance"`
  resource, so `hasPermission(#id, 'AppInstance', ...)` resolves in the instance's own workspace. `AppWorkflowFacade`
  and `AppApiCollectionFacade` are keyed by instance id too.
- **Creating.** `createApp(workspaceId, environment, ...)` needs `APP_MANAGE` in that workspace and environment and
  creates the app and its first instance in one transaction, the instance named after the environment (`Development`)
  and owned by the same user, with the app's visibility (a PRIVATE app gets a PRIVATE first instance); the returned
  `AppSecretKeyDTO` holds that instance's key. `createAppInstance(appId,
  workspaceId, environment, ...)` needs `APP_MANAGE` on the app **and** `APP_MANAGE` in the target workspace and
  environment, so an instance may live in another workspace than its app (`APP_INSTANCE_NAME_ALREADY_EXISTS` when the
  `(app, environment, name)` triple is taken).
- **Delete and suspend an app** need `APP_MANAGE` on the app and in every environment of its workspace
  (`checkManageInEveryEnvironment`), because they stop or remove instances of every environment. Deleting cascades to
  the instances in every workspace and deletes the grants of the app and its instances. A suspended app stops every
  instance's key; an app cannot return to `DRAFT`.
- **Instance status.** `updateAppInstanceStatus` moves an instance between `ACTIVE` and `SUSPENDED`, or to
  `TERMINATED`, which deletes its connection, workflow and API collection bindings and keeps the row; a terminated
  instance cannot return to another status (409 `APP_INSTANCE_TERMINATED`). Terminating first cancels each of the
  latest 10 deployments that is not terminal (at the provider when it has a provider deployment id, best effort, then
  locally), then clears the deployment connection and deployment settings, so the deployment connection no longer
  counts as in use. A terminated instance also refuses connection, workflow and API collection binds, key rotation,
  configuration updates and deployment-settings updates with 409 `APP_INSTANCE_TERMINATED`.
- **Instance URL.** `AppInstanceService.create` and `update` (reached through `updateAppInstance`) accept only an
  absolute `http` or `https` URL with a host and no user info; anything else is 400 `APP_INSTANCE_URL_INVALID` (an empty string on update clears the URL).
  With sign-in on, an instance outside DEVELOPMENT also needs an `https` URL, and a redirect URI the authorization
  server refuses (`INVALID_REDIRECT_URI`, for example plain `http` on a host that is not loopback) is translated by
  `AppInstanceClients` into `APP_INSTANCE_URL_INVALID` too, never a 422 `COMPONENT_ERROR`. A suspended or terminated instance's key is
  401 at once, with no cache to expire.
- **Reads embed only what the caller may see.** `AppDTO.instances` holds the instances of the app's own workspace that
  pass the instance visibility filter and `APP_VIEW` in the instance's environment (one bulk
  `filterVisibleIds("AppInstance", ...)` per read, one scope check per environment). An instance of another workspace
  never appears there: the owning workspace gets a reduced `ForeignAppInstanceDTO` (id, name, workspace, environment,
  status, current deployment id; no bindings, key hint or configuration) in `AppDTO.foreignInstances`. The workspace
  that hosts a foreign instance lists it through `getHostedAppInstances(workspaceId, environment)` (`hostedAppInstances`
  in GraphQL), which carries a read-only summary of the parent app and grants nothing on the `"App"` resource.
- Visibility: an app and an instance each belong to one workspace, so `AppVisibilityPolicy` and
  `AppInstanceVisibilityPolicy` support `PRIVATE` and `WORKSPACE` only (no `ORGANIZATION`) and both default to
  `WORKSPACE`; the instance's visibility says who may use it. Neither `updateApp` nor `updateAppInstance` takes a
  visibility any more (creating an app or an instance still does); it changes only through `AppSharingFacade`. `AppFacade.getApps` filters the list through
  `ResourceVisibilityResolver.filterVisibleIds("App", ...)`, so a PRIVATE app is listed only for those who can see it.
  The app and instance policies, providers and ownership resolvers, and `AppInstanceEnvironmentResolver`, are
  `@ConditionalOnEEVersion`, like every bean that needs `AppService`. `AppInstanceFacade.getBindableConnections` filters through
  `ResourceVisibilityResolver.filterVisibleIds`, so a PRIVATE connection the caller cannot see is not offered; binding
  additionally requires `CONNECTION_EDIT` on the connection (this line has no `CONNECTION_USE` scope), and a bindable
  connection's `usable` flag means valid credentials plus `CONNECTION_EDIT`. The picker does not apply the per-environment
  `environmentScopeFilter` that `WorkspaceConnectionFacadeImpl` does (APP_MANAGE on an env-resolved AppInstance already implies
  workspace ADMIN in that environment).
- **Update semantics are keep-when-null** for every field except `id` (`AppService.update`, `AppInstanceService.update`);
  an empty string clears `description`/`url`. The invariant lives in the service so each transport does not
  re-implement it. Consequences: GraphQL cannot tell absent from explicit null, and the management PATCH with fields
  plus a status change runs two facade calls (non-atomic); status changes are their own `PUT .../status` operations.
- **Component version.** Execution always runs the component's latest version, so `AppBindingDTO`,
  `AppBindableConnectionDTO`, the GraphQL `AppBinding`/`AppBindableConnection` types and the app-facing
  `GET /connections` carry `componentVersion` (latest, resolved server-side through `ComponentDefinitionService`) next
  to `connectionVersion`. The binding dialog loads operations for `componentVersion`; `connectionVersion` is
  informational only. Listings resolve it with `fetchComponentDefinition`, never `getComponentDefinition` (which
  throws), so one unresolvable component (a deleted custom component, one dropped in an upgrade, one missing from the
  component index) cannot fail a whole listing: `getBindableConnections` leaves such connections out (so
  `AppBindableConnectionDTO.componentVersion` stays `int`), `AppExecutionFacade.getTools` leaves out their tools, and
  `AppBindingDTO.componentVersion` is a nullable `Integer`, null for them (GraphQL `AppBinding.componentVersion: Int`,
  not required in the app-facing OpenAPI). The Bindings tab marks such a binding "Unavailable" and never loads its
  operations. `executeAction`/`executeTool` still resolve with `getComponentDefinition` and fail that one call.
- Rebinding a key to a different connection publishes `APP_CONNECTION_BOUND` with `previousConnectionId`; the same
  connection publishes `APP_OPERATIONS_CHANGED`. `AppConnectionService.unbind` returns whether a row was deleted and
  `APP_CONNECTION_UNBOUND` is published only then. `updateApp` publishes `APP_UPDATED` and `updateAppInstance`
  publishes `APP_INSTANCE_UPDATED` when at least one editable field changed: `changedFields` lists them. Instance
  events (`APP_INSTANCE_CREATED|UPDATED|ACTIVATED|SUSPENDED|TERMINATED|DELETED`, `APP_SECRET_KEY_ROTATED`, the binding
  events) carry the app id with `appInstanceId` in the data; every guarded write of `AppFacadeImpl` and
  `AppInstanceFacadeImpl` is listed in `AuditDataContractTest` as recorded by `AppAuditPublisher`.
- Name (256) and url (2048) lengths are checked in `AppServiceImpl` and `AppInstanceServiceImpl` before any write (400
  `APP_FIELD_TOO_LONG`). A `DataIntegrityViolationException` on save becomes `APP_NAME_ALREADY_EXISTS` only when its
  cause chain names `uk_app_workspace_name`, and `APP_INSTANCE_NAME_ALREADY_EXISTS` only for
  `uk_app_instance_app_environment_name`; any other violation is rethrown.
- **Connection-delete guard SPI:** `ConnectionUsageChecker` (`platform-connection-api`) lets a module outside
  platform-connection say a connection is in use; `ConnectionFacadeImpl` collects every checker through
  `ObjectProvider#orderedStream` and refuses the delete naming the consumers. `AppConnectionUsageChecker` reports
  every app instance binding the connection, whatever the app's or instance's status. `app_connection.connection_id` has no cascade, so
  without the guard the user would get a bare FK violation.
- Client-side owner name is resolved with `useWorkspaceUsersQuery`, falling back to `User <id>`; there is no
  server-side `ownerName`.
- **Admin pages** (`client/src/pages/automation/apps`): the Apps list and `HostedAppInstances` (instances this
  workspace hosts of apps owned elsewhere, each row linking to `apps/instances/:id`); the app page (`App.tsx`) with
  Overview, Instances, Connections, API collections, Workflows and Activity tabs, an instance selector
  (`AppInstanceSelect`) driving the binding tabs; the instance page (`AppInstance.tsx`), which shows the parent app
  read-only and only the three binding tabs. Bind and unbind controls check `APP_MANAGE` in the instance's own workspace
  and environment (`useAppManageScopes`), and a `TERMINATED` instance is never manageable. The Activity tab shows the
  invocations of the selected instance. A Deployments tab (RocketIcon, next to the instance tabs and
  following the instance selector) and a deployment header are described under **Versions and deployments**.

## Versions and deployments

Phase 5. A **version** is a label for code ByteChef does not hold; a **deployment** is the recorded fact that a version
was deployed to one instance. A deployment is either recorded (CI says what it deployed: provider `EXTERNAL`, status
`READY`, described here) or made by ByteChef through a hosting provider (see **Provider-driven deployments**).

- **Tables** (changelog `20261116000001_automation_app_deployment.xml`). `app_version`: `app_id` (FK, cascade),
  `name` (256), `git_ref` (256), `git_commit_sha` (64), `description` (TEXT), audit columns and `version`; unique
  `uk_app_version_app_name (app_id, name)`, so versions belong to the app, not to an instance. `app_deployment`:
  `app_instance_id` (FK, cascade), `app_version_id` (FK, no cascade: a referenced version cannot be deleted alone),
  `provider`, `provider_deployment_id`, `url` (2048), `status`, `error_message`, `metadata` (JSON through `MapWrapper`),
  `created_by`, `created_date`, `finished_date`, `version`. `app_instance.current_app_deployment_id` points at the
  current deployment with `fk_app_instance_current_app_deployment` (`ON DELETE SET NULL`); deleting an instance or an app
  cascades to its deployments and versions.
- **Enums** are stored as ordinals, so append only: `AppDeploymentStatus { QUEUED, BUILDING, READY, FAILED, CANCELED }`
  (`isTerminal()` is true for the last three) and `AppDeploymentProviderType { VERCEL, EXTERNAL, DEPLOY_HOOK }`. The
  provider type is persisted by ordinal (`app_deployment.provider INT`, no check constraint), so values are only ever
  appended; `DEPLOY_HOOK` is ordinal 2 and needed no changelog. GraphQL `AppDeploymentProviderType`, the REST and CLI
  `AppDeploymentProvider` and the client's labels mirror it.
- **A deployment is never updated after a terminal status.** `AppDeploymentService.create` sets `finished_date` for a
  terminal status; `updateStatus` is the only mutation and refuses a terminal row with 409 `APP_DEPLOYMENT_TERMINAL`. The status sweep
  and the provider-driven paths write through it (and through `AppDeploymentService.cancel`, which decides on the row it
  writes).
- **`AppDeploymentFacade`** (`AppDeploymentFacadeImpl`, `@Transactional`, `@ConditionalOnEEVersion`). It composes
  `AppVersionService`, `AppDeploymentService`, `AppInstanceService` and `AppService` and uses no repository.
  - `getDeployments(appInstanceId, limit)`: `hasPermission(#appInstanceId, 'AppInstance', 'APP_VIEW')`, newest first,
    each row marked `current` when it is the instance's pointer. Works on a `TERMINATED` instance: its history is kept.
  - `getCurrentDeployment(appInstanceId)`: `APP_VIEW` on the instance; reads only the pointed row and its version. A
    deployment that belongs to another instance is treated as no current deployment and logged at WARN.
  - `getVersions(appId, limit)`: `hasPermission(#appId, 'App', 'APP_VIEW')`, newest first.
  - `createVersion(appId, name, gitRef, gitCommitSha, description)`: `hasPermission(#appId, 'App', 'APP_DEPLOY')`. The
    guard runs first, so an unknown app answers 403 to a caller who is not a tenant admin (the guard cannot place an
    unknown app in a workspace) and 404 `APP_NOT_FOUND` only to a tenant admin. It publishes no audit event (`AuditDataContractTest` skips it as `VERSION_LABEL`).
  - `recordDeployment(appInstanceId, versionName, url, gitCommitSha)`:
    `hasPermission(#appInstanceId, 'AppInstance', 'APP_DEPLOY')`. In one transaction it normalises the input, refuses a
    `TERMINATED` instance (409 `APP_INSTANCE_TERMINATED`; `SUSPENDED` is allowed), finds the version by `(app, name)` or
    creates it with the given commit, writes a `READY` `EXTERNAL` deployment, sets `current_app_deployment_id` and
    publishes `APP_DEPLOYMENT_COMPLETED` (`appInstanceId`, `appDeploymentId`, `appVersionId`, `versionName`, `provider`).
- **Normalisation, in the facade so GraphQL and REST agree.** Blank `gitRef`, `gitCommitSha`, `url` and `description`
  mean absent, and ref, commit and url are trimmed (a trailing newline from `git rev-parse` is dropped). The version name
  is trimmed, and a blank name or one with a control character or line break is refused with 400, in both
  `createVersion` and `recordDeployment`. The description is capped at 4096 characters in `AppVersionServiceImpl`
  (400 `APP_FIELD_TOO_LONG`, also `maxLength` in the REST schema), like the name, ref and commit limits.
- **Commit mismatch.** Recording an existing version name with a commit that differs from the stored one (case
  ignored, a stored null counts as different) is refused with 409 `APP_VERSION_COMMIT_MISMATCH` and the stored version
  is never changed; with no commit given, the existing version is reused. Choose a new name.
- **URL rule.** A non-null url must parse as a non-opaque `java.net.URI` with a host and the scheme `http` or `https`,
  otherwise 400 `APP_DEPLOYMENT_URL_INVALID`; the message does not echo the input. The check runs before any service call.
- **List limit.** `DEFAULT_LIST_LIMIT = 50`, `MAX_LIST_LIMIT = 200`; a limit outside 1..200 is 400
  `APP_LIST_LIMIT_INVALID`, checked in the facade before any read and passed to the repositories as a `Limit`.
- **Scope.** `APP_DEPLOY` on the `'App'` resource passes on a role held in any one environment (pinned by
  `testCreateVersionPassesOnDeployHeldInOneEnvironmentOnly`; a version belongs to the app), while recording is per instance and so per
  environment: a member with `APP_DEPLOY` in DEVELOPMENT only can create a version name but not record in PRODUCTION.
- **GraphQL** (`AppDeploymentGraphQlController`, no `@PreAuthorize` of its own, at most one facade call per field):
  queries `appVersions(appId, limit: Int = 50)` and `appDeployments(appInstanceId, limit: Int = 50)`; mutations
  `createAppVersion(appId, input: AppVersionInput)` and `recordAppDeployment(appInstanceId, input:
  AppDeploymentRecordInput)`; fields `App.versions(limit)`, `AppInstance.deployments(limit)` and
  `AppInstance.currentDeployment` (null without a facade call when the instance has no pointer). `metadata` is not exposed.
- **Instance-keyed version reads.** `getVersionsForInstance(appInstanceId, limit)` and
  `fetchVersionForInstance(appInstanceId, name)` are guarded by `APP_VIEW` on the instance, so the workspace that hosts
  an instance of another workspace's app can list and resolve the app's versions (GraphQL
  `appInstanceVersions(appInstanceId, limit)`, REST `GET /app-instances/{id}/versions[?name=]`; the Deployments tab and
  the CLI's `deployment deploy|rollback|list` use them). The app-keyed reads stay for the app's own workspace.
  `recordDeployment` with instance-level `APP_DEPLOY` may create a version name on the app: that is the documented rule
  (CI records what it deployed and the version name follows), not a leak of app-level write.
- **Management REST** (`AppDeploymentApiController`, tag `app-deployment`): `GET|POST /apps/{id}/versions`
  (`getAppVersions`, `createAppVersion`) and `GET|POST /app-instances/{id}/deployments` (`getAppInstanceDeployments`,
  `recordAppInstanceDeployment`); lists take an optional `limit`. Version create and deployment record are two of the
  five management writes allowed outside DEVELOPMENT (deploy, roll back and cancel are the others): they never call
  `requireDevelopment`, because CI records what it
  deployed to STAGING and PRODUCTION with a key of that environment, and recording is a fact about a deployment, not a
  change to what an instance may call. The deployment routes still resolve `X-Environment` and call
  `AppEnvironmentScope.requireInEnvironment`, so an instance of another environment is 404. `/apps/{id}/versions` takes
  the header like the other `/apps/{id}` routes but ignores the value.
- **CLI.** See **CLI, template and plugin (Plan D2)**. `AppDeploymentFacade` is the only server entry for it.
- **UI** (`client/src/pages/automation/apps`): a Deployments tab (`AppDeploymentsTab`) on the app page, which follows the
  instance selector, and on the instance page. Both pages pass `canDeploy` (from `useAppDeployScopes`), the deployment
  connection and provider availability, so Deploy, Roll back and Cancel are offered on the app page too, and an owned
  instance's name in the Instances tab links to its instance page. It lists version, status, provider, who recorded it, created and finished
  time and the URL (a link only for `http` and `https`), shows a failed deployment's error, starts at 50 rows and offers
  "Show more" (100, 200). The instance header (`AppDeploymentSummary`) shows the current version from
  `currentDeployment` and the latest deployment's status and time from `deployments(limit: 1)`, or "No deployments yet".
  An empty tab names `bytechef automation app deployment record`. Phase 6 adds the deploy, rollback and cancel actions
  and the deployment settings dialog (see **Provider-driven deployments**).

## Provider-driven deployments

Phase 6. ByteChef can run a deployment itself: it asks a hosting provider to deploy a version to an instance, follows
the provider until the deployment ends, and can roll back or cancel it. `recordDeployment` still exists and remains the
way to report a deployment made outside ByteChef (provider `EXTERNAL`); it never calls a provider and works on any
hosting target. Nothing here was run against a real Vercel account: the Vercel component and provider are built from the
documented API and tested with mocks, and the deploy hook provider is tested with mocks while the reference agent is
tested on its own in the SDK's tests.

- **The SPI** (`automation-app-api`, package `com.bytechef.ee.automation.app.deployment`). `AppDeploymentProvider`:
  `getHostingTarget()`, `getDeploymentConnectionComponentName()`, `deploy(ProviderDeploymentRequest)`,
  `getDeployment(ProviderDeploymentRef)`, `cancel(ProviderDeploymentRef)` and `validateDeploymentSettings(settings,
  environment)` (default no-op; throws `AppDeploymentSettingsException`). Records: `ProviderDeploymentRequest`
  (`connectionId`, `deploymentSettings`, `gitRef`, `gitCommitSha`, `environment`, nullable `versionName`),
  `ProviderDeploymentRef` (`connectionId`, `deploymentSettings`, `providerDeploymentId`; no environment) and
  `ProviderDeployment` (`id`, `status`, `url`, nullable `errorMessage`, the failure reason). Providers are `@Component
  @ConditionalOnEEVersion` beans, resolved through `ObjectProvider<AppDeploymentProvider>#orderedStream` by hosting
  target in `AppDeploymentSynchronizer`; nothing injects a provider directly, so the app starts without one. Both
  providers run their component actions through `ActionDefinitionFacade.executePerform` inside
  `SecurityUtils.runAsSystem` (the facade already authorized `APP_DEPLOY`, and the sweep has no user), with the
  instance's deployment connection as the component's connection. Deploy passes the environment ordinal, status and
  cancel pass null.
- **Selection.** By the instance's hosting target: `VERCEL` uses the Vercel provider (deployment provider `VERCEL`),
  `SELF_HOSTED` the deploy hook provider (`DEPLOY_HOOK`). Without a provider for the target, or without a deployment
  connection on the instance, deploy and rollback answer 409 `APP_DEPLOYMENT_PROVIDER_UNAVAILABLE` before anything is
  written. The connection component each provider needs is `vercel` and `httpClient`.
- **`AppDeploymentFacade.deploy|rollback|cancel`** (`hasPermission(#appInstanceId, 'AppInstance', 'APP_DEPLOY')`, like
  `recordDeployment`; no environment restriction; all three are `@Transactional(propagation = NOT_SUPPORTED)`, pinned by
  a test, so no transaction is held across a provider call).
  - `deploy(appInstanceId, appVersionId)` first refuses a `TERMINATED` instance (409 `APP_INSTANCE_TERMINATED`) and a
    version of another app (404 `APP_VERSION_NOT_FOUND`). Then `AppDeploymentService.create` takes `SELECT ... FOR UPDATE` on the
    `app_instance` row, then refuses (409 `APP_DEPLOYMENT_IN_PROGRESS`) when a `QUEUED` or `BUILDING` row exists, and
    inserts the `QUEUED` row in the same transaction: one non-terminal deployment per instance, enforced under the lock.
    Recording (a terminal `READY` row) is still allowed while one runs.
  - The `QUEUED` row commits before the provider is called. The provider request is built from a re-read of the
    instance after the insert (the settings cannot change in between: see the settings lock below). The provider's
    answer is applied by `AppDeploymentSynchronizer` in a separate transaction: `QUEUED`, `BUILDING`, `READY` (which
    marks the deployment current and publishes `APP_DEPLOYMENT_COMPLETED`, the same path `recordDeployment` uses) or
    `FAILED`. A provider URL that is not absolute http(s) is ignored. The synchronizer never writes over an already-terminal row (it does write `READY`, `FAILED` and `CANCELED`). Audit
    events: `APP_DEPLOYMENT_INITIATED`, `APP_DEPLOYMENT_FAILED`, `APP_DEPLOYMENT_CANCELED`.
  - **A provider exception** on deploy leaves the row `FAILED` with the fixed message "The hosting provider could not
    start the deployment", `finished_date` set, no current pointer, `APP_DEPLOYMENT_FAILED` audited; the FAILED
    deployment is returned, not thrown. Only the exception's class name is logged. Anything between the `QUEUED` commit
    and the provider call that throws (no connection, wrong component, no provider, the audit) fails the row the same
    way with its own fixed message, so no row is stranded; if marking it `FAILED` itself fails, the error is rethrown.
    An answer the row cannot store (for example an id over 256 characters) fails it with "The hosting provider returned
    a deployment that could not be stored".
  - **Rollback** is the deploy path for a version that was `READY` on that instance at least once, otherwise 404
    `APP_VERSION_NOT_FOUND`. It writes a new row with metadata `rollback: true`; older rows are never touched.
  - **Cancel** (`cancel(appInstanceId, appDeploymentId)`): a deployment of another instance is 404
    `APP_DEPLOYMENT_NOT_FOUND`; a terminal one is 409 `APP_DEPLOYMENT_TERMINAL`. With a provider id the facade asks the
    provider first (a provider failure is 502 `APP_DEPLOYMENT_PROVIDER_FAILED` naming only the deployment id, and the row
    stays as it was), then writes `CANCELED`. A `QUEUED` row with no provider id is canceled with no provider call. The
    decision is taken on the row that is written: `AppDeploymentService.cancel(id, handledProviderDeploymentId)` loads
    the row once and writes `CANCELED` on that same entity, so its `@Version` guards the decision; a row that gained a
    provider id the caller did not cancel writes nothing and the facade tries again. The facade repeats the sequence
    (read, provider cancel, write) up to 3 times, also on an `OptimisticLockingFailureException`, and then answers 409
    `APP_DEPLOYMENT_IN_PROGRESS`. When the instance's connection is missing or of another component, or no provider bean
    exists for the row's provider type, the deployment is canceled locally with no provider call (logged at WARN)
    rather than left running.
  - **An answer arriving after a cancel.** If the provider's answer to `deploy` arrives on a row that is already
    terminal without that id (canceled, or failed by ByteChef), the facade calls the provider's `cancel` for the id just
    returned (`AppDeploymentSynchronizer.cancelAtProvider`, `NOT_SUPPORTED`, best effort, class name only logged). The
    same compensating cancel runs when the sweep fails a row at its deadline and the row has a provider id.
- **The status sweep** (`AppDeploymentStatusJob`, `@Scheduled(cron = "${bytechef.app.deployment.poll-cron:...}")`). For
  every `QUEUED` or `BUILDING` deployment with a provider id it asks the provider (`getDeployment`) on `appExecutor`
  and applies the answer. It is registered everywhere but **needs `appExecutor` and does nothing without it**
  (`appExecutor` exists only when `bytechef.app.execution.concurrency-limit` is set, which only `server-app`'s yml does),
  so in any other app a provider deployment is never advanced: it stays `BUILDING` and blocks the instance until it is
  canceled. Each tenant is swept under its own `TenantContext`; a failing tenant is logged and skipped, one failing
  deployment is logged by class name without stopping the others, and a sweep waits for its calls at most 5 minutes so
  sweeps do not overlap. A row older than `bytechef.app.deployment.timeout` (`30m` in the yml; 30 minutes also when it
  is unset or not positive, so a deployment cannot wait forever) is failed with "The deployment did not finish within
  30 minutes" without asking the provider for its status, and the provider is then asked to cancel it. A row without a
  provider id waits for that deadline. A connection of the wrong component fails the row without a provider call.
  `bytechef.app.deployment.poll-cron` defaults to `*/30 * * * * *`.
- **Deployment connection and settings on the instance** (`app_instance.deployment_connection_id` and
  `deployment_settings`, both from the Phase 1 changelog; no foreign key on the connection).
  `AppInstanceFacade.updateAppInstanceDeploymentSettings(id, deploymentConnectionId, deploymentSettings)` needs
  `APP_MANAGE` on the instance and, when a connection is given, `CONNECTION_EDIT` on it and its ownership (the caller
  owns the connection or is a tenant admin), the rule of renaming or replacing a connection's credentials. It applies to
  every submitted connection id, the current one included; clearing needs no ownership.
  Setting needs an automation connection of the instance's workspace and environment (400
  `APP_CONNECTION_NOT_BINDABLE` otherwise); then, **only when a provider exists for the instance's hosting target**
  (without one the save succeeds after the bindable check), the connection must be **of the provider's component** (400 `APP_DEPLOYMENT_SETTINGS_INVALID`, the
  message naming only the component), and the provider's `validateDeploymentSettings` must accept the settings (the same
  400 with the provider's message, which carries key names and fixed text only). A null connection with null or empty
  settings **clears** both (no bindable or provider checks, audited without a connection id); a null connection with
  settings is 400. Settings are a JSON object of at most 64 KB, like the configuration. While a deployment is in
  progress the update is refused with 409 `APP_DEPLOYMENT_IN_PROGRESS`: it takes the same `FOR UPDATE` lock on the
  instance row as `deploy` and holds it until the settings commit. **The settings are not secrets** and are readable by
  anyone who can view the instance, like the configuration; the credential lives in the connection, which deploy only
  uses. The deployment connection is a connection usage: `AppDeploymentConnectionUsageChecker` names `app '<app>'
  instance '<instance>' (deployment)`, so the connection cannot be deleted while an instance uses it.
- **The required component is enforced in five places**, because an instance's hosting target can change without the
  connection being revalidated: on save (400), at deploy start (the row fails with "The deployment connection does not
  belong to the component the hosting provider uses", no provider call), in the facade `cancel` (no provider call;
  the row is canceled locally), in `AppDeploymentSynchronizer.cancelAtProvider`, the compensating call on an already
  terminal row (it skips the provider and logs), and in the sweep (the row is failed, no
  `getDeployment`). A connection of the wrong component is never handed to a provider.
- **The failure reason.** A provider reports a failure as `ProviderDeployment.errorMessage` with status `FAILED`.
  `AppDeploymentSynchronizer` stores "The hosting provider reported that the deployment failed: <reason>" in
  `error_message`, with the reason's control, format and line or paragraph separator characters (`Cc`, `Cf`, `Zl`,
  `Zp`) replaced by a space, trimmed, and cut to 500 characters without splitting a surrogate pair; a blank or null
  reason stores only the fixed text. The reason is provider-supplied text, sanitized and bounded but not redacted.
  **Exception messages never reach stored text**, logs or responses: a provider exception becomes one of the fixed
  messages above, and logs carry its class name only. The UI renders the text as plain text.
- **Vercel.** The `vercel` component (`server/libs/modules/components/vercel`, connection `BEARER_TOKEN`, no connection
  test) on `https://api.vercel.com`, with an optional `teamId` query: `listProjects` (`GET /v10/projects`, follows the
  pagination token up to 50 pages), `createDeployment` (`POST /v13/deployments`, GitHub git source only),
  `getDeployment` (`GET /v13/deployments/{id}`) and `cancelDeployment` (`PATCH /v12/deployments/{id}/cancel`).
  Deployment actions return `{id, url (https:// prepended), state, errorCode, errorMessage}`. Settings keys:
  `projectId` (required), `repository` (required, exactly `owner/name`, one trailing `.git` stripped), `teamId` and
  `target` (`production` or `preview`). The version needs a git ref. Target rule: a PRODUCTION instance deploys to
  `production` by default and may choose `preview`; DEVELOPMENT and STAGING deploy `preview`, and `target=production`
  on them is rejected ("Only a PRODUCTION app instance may deploy to the Vercel production target"). State mapping:
  `QUEUED` to QUEUED; `INITIALIZING` and `BUILDING` to BUILDING; `READY` to READY; `ERROR` to FAILED (reason
  `errorCode: errorMessage`); `CANCELED` and `DELETED` to CANCELED; absent or unknown to BUILDING (unknown values are
  logged, cut to 40 characters). Provider failures are `VercelDeploymentException` with fixed text and no cause.
- **The deploy hook** (`SELF_HOSTED`). ByteChef calls an HTTP endpoint the customer runs, through the HTTP Client
  component (`post` and `get`, `fullResponse`, JSON, redirects off, 10 s timeout) with the deployment connection for
  authorization (typically Bearer Token). Protocol: `POST {deployUrl}` with `{environment, gitRef?, gitCommitSha?,
  version?}` (a git ref or a commit is required) answers 2xx with a JSON object whose `id` is a string or any integer
  (at most 256 characters, not only dots); `GET statusUrl` answers `{status, url?, errorMessage?}` with `status` one of
  `QUEUED`, `BUILDING`, `READY`, `FAILED`, `CANCELED` or `CANCELLED` (case-insensitive; missing or unknown means
  BUILDING); `POST cancelUrl` with `{}` answers any 2xx. `url` is kept only when absolute http(s) with a host, no
  user-info and at most 2048 characters; `errorMessage` only for `FAILED`. A 404 on **status** fails the deployment
  with "The deploy endpoint no longer knows this deployment" (the agent forgot it after a restart); a 404 on deploy or
  cancel is an ordinary failure. Settings keys: `deployUrl` (required), `statusUrl` and `cancelUrl` (optional, each
  must contain `{id}`; defaults `{deployUrl}/{id}` and `{deployUrl}/{id}/cancel`). URL rules, checked on save and again
  at deploy: a string, absolute http(s) with a host, no user-info or fragment, at most 2048 characters; **https outside
  DEVELOPMENT**; `statusUrl` and `cancelUrl` on the same scheme, host and effective port as `deployUrl` (so the
  credential cannot be sent to another host); `{id}` may not appear in the authority; the provider id is
  percent-encoded as a path segment (everything but `A-Za-z0-9-_~`). Status and cancel check structure and origin only,
  because `ProviderDeploymentRef` has no environment; https is enforced at save and deploy. Failures are
  `DeployHookDeploymentException` with fixed text, no cause, nothing from the response.
- **The reference agent** (`sdks/frontend/automation/app/create/template/scripts/deploy-agent.mjs`, `npm run
  deploy-agent`, documented in the template's `AGENTS.md`). Node built-ins only, for one app on one machine. Configured
  by environment variables: `DEPLOY_AGENT_TOKEN` (required, at least 32 characters, never passed to the command),
  `DEPLOY_COMMAND` (required), `DEPLOY_AGENT_HOST` (default `127.0.0.1`), `DEPLOY_AGENT_PORT` (default 8787),
  `DEPLOY_AGENT_PUBLIC_URL` and `DEPLOY_AGENT_KILL_GRACE_MS` (default 10000). Every request needs `Authorization: Bearer
  <token>` (compared with `timingSafeEqual`; 401 with an empty body). `POST /` (16 KB limit, 413) validates
  `environment`, `gitRef`, `gitCommitSha` (7 to 64 hex characters) and `version` (strings of at most 255 characters
  without control characters; every field except the commit sha also must not start with `-`), answers 409 while a command runs, otherwise
  spawns `DEPLOY_COMMAND` through the shell and answers 202 `{id, status: BUILDING}`. **The request values reach the
  command only as the environment variables `BYTECHEF_ENVIRONMENT`, `BYTECHEF_GIT_REF`, `BYTECHEF_GIT_COMMIT_SHA` and
  `BYTECHEF_VERSION`**, never in its command string. `GET /{id}` answers `{id, status, url?, errorMessage?}` or 404;
  `POST /{id}/cancel` sends SIGTERM to the process group and SIGKILL after the grace period and answers 200 (also 200
  with the current state for a deployment that already ended). Exit code 0 is READY (with the public URL), anything
  else FAILED with `Exit code <n>`; the command's output is discarded. State is in memory (the last 100 deployments).
- **GraphQL** (`app.graphqls`; resolvers only delegate, the guards sit on the facades): query `appDeployment(appInstanceId,
  appDeploymentId)` (`APP_VIEW`); mutations `deployAppInstance(appInstanceId, appVersionId)`,
  `rollbackAppInstance(appInstanceId, appVersionId)` and `cancelAppDeployment(appInstanceId, appDeploymentId)` (all
  `APP_DEPLOY`) and `updateAppInstanceDeploymentSettings(id, input: AppInstanceDeploymentSettingsInput)` with
  `deploymentConnectionId: ID` and `deploymentSettings: Map`; `AppInstance` gains `deploymentConnectionId`,
  `deploymentSettings: Map!` and `deploymentProviderAvailable: Boolean!` (whether a provider bean exists for the hosting
  target; it ignores the connection, so the UI combines the two). GraphQL has no environment rule. The facade also gained
  `getDeployment`, `fetchVersion(appId, name)` and `isDeploymentProviderAvailable(hostingTarget)`.
- **Management REST** (`openapi-management.yaml`). Deploy, rollback, cancel and the reads work in every environment
  (`requireInEnvironment`); the settings write is DEVELOPMENT only. `POST /app-instances/{id}/deploy` and `/rollback`
  (body `{appVersionId}`, 201), `POST /app-instances/{id}/deployments/{deploymentId}/cancel` (200), `GET
  /app-instances/{id}/deployments/{deploymentId}`, `GET /apps/{id}/versions?name=` (the CLI resolves a version by name
  through it) and `PUT /app-instances/{id}/deployment-settings` (body `{connectionId?, settings: map<string,string>}`,
  `requireDevelopmentAndInEnvironment`, so 403 `APP_OPERATION_DEVELOPMENT_ONLY` elsewhere; no connection and no
  settings clears). `AppInstance` carries `deploymentConnectionId` and `deploymentSettings`. REST and CLI settings are
  string maps.
- **CLI** (`AutomationAppDeploymentCommand`, `AutomationAppInstanceCommand`): `automation app deployment deploy --version
  <name> [--commit] [--git-ref] [--wait] [--timeout <seconds>]`, `deployment rollback --version <name> [--wait]
  [--timeout]`, `deployment cancel [--deployment-id]` and `automation app instance deployment-settings set --connection-id
  <id> [--setting key=value]...`, `clear` and `get`. Deploy, rollback, cancel and `get` work in every environment; `set`
  and `clear` are DEVELOPMENT only. Deploy creates a missing version only with `--commit` or `--git-ref`; rollback never
  creates one. `--wait` polls every 5 s (the first poll at once) up to `--timeout` (default 600), prints `Deployment
  <id> is <status>` on stderr first, retries a poll that fails with a transport error, 408, 429 or 5xx, and exits 1 for
  FAILED, CANCELED, a deployment the provider could not start and a timeout (the deployment keeps running; the message
  names `deployment cancel --deployment-id`). `cancel` without an id cancels the single in-progress deployment (exit 3
  when there is none, 1 when several). Repeated `--setting` options are read from the parsed input, because Spring Shell
  returns only the first match.
- **UI** (`client/src/pages/automation/apps`). `AppInstanceDeploymentSettingsDialog`, opened by a "Deployment" button on
  the instance page header and in the instance actions (`APP_MANAGE` holders, instance not terminated): a connection
  picker filtered to the provider's component, the Vercel fields (project id, team id, repository, target) or the deploy
  hook fields (deploy URL, status URL, cancel URL), a notice with every field disabled when `deploymentProviderAvailable`
  is false, and "Remove deployment configuration". Save keeps stored keys the form does not edit. The Deployments tab has
  a Deploy button (version picker, newest first, at most 50 versions; it shows why it is disabled) and Roll back and
  Cancel per row behind confirmations, for `APP_DEPLOY` holders only. The tab and the instance summary poll every 5 s
  while a row is `QUEUED` or `BUILDING` and stop otherwise; when an in-progress row ends they refresh the instance. Server
  400s reach the user through the global toast.

## Access scopes

`AppPermissionScope` and its provider (`automation-configuration-service`, EE) declare four scopes. The registry hands
them to the built-in roles by rank, and custom roles store scope names, so all four are selectable in the custom role
editor. Roles are per environment.

| Scope | Minimum built-in role | Gated today |
|-------|-----------------------|-------------|
| `APP_VIEW` | VIEWER | yes: reads of apps, instances, bindings, the icon |
| `APP_USE` | VIEWER | yes: at sign-in and on every `/api/app/v1` call that carries a user token |
| `APP_DEPLOY` | EDITOR | yes: creating a version (on the app) and recording a deployment (on the instance); see **Versions and deployments** |
| `APP_MANAGE` | ADMIN | yes: every write, binding, key rotation, configuration, icon, and sharing (see below) |

`APP_USE` (sign in to and use an instance) is checked by the sign-in access check and by the per-call user check (see
**End-user sign-in**) rather than by a `@PreAuthorize` gate. `APP_DEPLOY` (versions and deployments, which hand out no
credentials) is gated by `AppDeploymentFacadeImpl`. No scope is exempt in `PermissionScopeRegistryTest.ShippedCatalog`
any more: `SCOPES_AWAITING_THEIR_GATES` is empty, and the test fails if a scope is added to it while it is gated. That
test scans source files that are not Gradle inputs, so run it with `--rerun`; a cached green result can be stale.

## Sharing

`AppSharingFacade` (`automation-app-api`; `AppSharingFacadeImpl` in `automation-app-service`) is the only way to change
who sees an app or who may use an instance: `setAppVisibility`, `grantAppAccess`, `revokeAppAccess`, `getAppGrants`
and the four `...AppInstance...` equivalents. GraphQL exposes them through `AppSharingGraphQlController`; there is no
management REST for sharing.

- **Who may share.** The resource's owner, or a holder of `APP_MANAGE` on that resource (a tenant admin passes both):
  `@permissionService.isResourceOwner('App'|'AppInstance', #id) || hasPermission(#id, 'App'|'AppInstance', 'APP_MANAGE')`.
  This is wider than projects and connections (owner or tenant admin) so that an app whose owner has left can still
  be managed by a workspace admin. The grant listings use the same guard, so a plain user cannot learn who else was
  given access.
- Every call passes the resource's own `workspaceId`; a resource that does not belong to it answers
  `APP_NOT_FOUND`/`APP_INSTANCE_NOT_FOUND`. For an instance that is the instance's workspace, which may differ from
  its app's.
- **A grantee must be a member of the resource's own workspace.** A non-member and a non-existent user fail with the
  same message, so user ids cannot be enumerated. Revoking skips the membership check so a grant held by someone who
  has left can still be removed. Granting is idempotent.
- Only `PRIVATE` and `WORKSPACE` are accepted (`APP_VISIBILITY_UNSUPPORTED` otherwise).
- **Narrowing to PRIVATE deletes the resource's grants** (the project behavior; connections keep theirs so that
  demoting restores the audience). Deleting an app or an instance deletes its grants too.
- Audit events through `AppAuditPublisher`: `APP_VISIBILITY_CHANGED`, `APP_ACCESS_GRANTED`, `APP_ACCESS_REVOKED` and
  `APP_INSTANCE_VISIBILITY_CHANGED`, `APP_INSTANCE_ACCESS_GRANTED`, `APP_INSTANCE_ACCESS_REVOKED` (data: the user id,
  the visibility and `previousVisibility`; instance events add `appInstanceId`). All eight facade methods are listed in
  `AuditDataContractTest`.
- Client: `AppInstanceShareDialog` (instance Share button, shown where the existing `APP_MANAGE` check passes, so an
  owner without `APP_MANAGE` is allowed by the server but sees no button) and the visibility picker in `AppDialog`
  (edit mode, `canShare`, no "Specific people" for an app).

## Member removal

`AppWorkspaceUserRemovalListener` handles `WorkspaceUserRemovedEvent` in the removing transaction. For the removed user
it revokes their `"App"` grants on the apps of that workspace and their `"AppInstance"` grants on the instances that
live in it (`ResourceGrantService.revokeUserGrants`; instances of the workspace's apps that live in other workspaces
are not touched). It changes neither owner nor visibility. For each PRIVATE app or instance the removed member owned
there it publishes `APP_OWNER_LEFT` or `APP_INSTANCE_OWNER_LEFT`: once the owner has left, a workspace admin no longer
passes `APP_MANAGE` on a PRIVATE resource (visibility is a precondition), so only a tenant admin can manage it, and
the event lets them find it. A WORKSPACE-visible resource needs nothing: every workspace admin holds `APP_MANAGE`.
Reassigning an owner is not built. There is no test of the listener's Spring wiring, only a unit test with mocks.

## Instance configuration

`app_instance.configuration` holds a JSON object per instance, exposed as `AppInstanceDTO.configuration`, GraphQL
`AppInstance.configuration: Map!` and management REST `AppInstance.configuration`. It is never on
`ForeignAppInstanceDTO`.

- Set through `AppInstanceFacade.updateAppInstanceConfiguration(id, configuration)` (GraphQL
  `updateAppInstanceConfiguration`) or the five-argument `updateAppInstance` (management `PATCH`, one transaction with
  the other fields; absent or null keeps the stored value, an empty object clears it). Both need `APP_MANAGE` on the
  instance.
- The parameter is `Object` so the facade itself refuses anything that is not a JSON object
  (`APP_INSTANCE_CONFIGURATION_INVALID`, 400) and anything over 64 KB of serialized UTF-8 JSON
  (`APP_INSTANCE_CONFIGURATION_TOO_LARGE`, 413), before any write.
- **Readable by anyone who can view the instance** (`APP_VIEW` plus the instance visibility), not only by managers.
  Audit data never carries the values: `APP_INSTANCE_UPDATED` lists `configuration` in `changedFields` and nothing
  else about it.
- **It must not hold secrets.** Credentials belong in connections, bound to the instance. The management OpenAPI
  description and `AppInstanceConfigurationDialog` say the same.

## The icon

An app's icon is an image in ByteChef file storage, never a URL, so nothing loads an image from a host ByteChef does
not control. `AppFileStorage` (`automation-app-api`; `AppFileStorageImpl`, built by `AppFileStorageConfiguration` from
`FileStorageServiceRegistry` and `bytechef.file-storage.provider`) wraps the core `FileStorageService` in the directory
`app_icons`; like `KnowledgeBaseFileStorage` it is called from facades, not services. `app.icon` is a `FileEntry`.

- `AppFacade.updateAppIcon` (`APP_MANAGE`) reads at most 256 KB plus one byte, then decides the type from the
  content's magic bytes: PNG, JPEG or WebP. The user's file name and declared content type are never used; SVG, GIF
  and empty input fail with `APP_ICON_INVALID` (400), as does anything over 256 KB, before storage is touched. The file
  is stored under a name the storage provider makes unique (`storeFileContent(dir, name, content, true)`, so two apps
  never share a key on the AWS provider), then the previous file is deleted. `deleteAppIcon` and `deleteApp` delete
  the file too; a failed file delete is logged at WARN and does not fail the operation.
- `getAppIcon` (`APP_VIEW`) detects the type again from the stored bytes and refuses non-image bytes.
- Endpoint (`automation-app-rest`, `AppIconApiController`, session security, CSRF cookie token, not the API-key
  surface): `POST|GET|DELETE /api/automation/internal/apps/{id}/icon`; the POST takes a multipart field `file` and
  answers 204; the GET answers the image with the detected `Content-Type` and `X-Content-Type-Options: nosniff`, or
  404 when there is no icon.
- Read models expose only `hasIcon` (`AppDTO`, `AppInstanceDTO.AppSummary`, GraphQL `App`/`AppSummary`, management
  REST `App.hasIcon` and `AppInstance.appHasIcon`), never the `FileEntry`; a client builds the endpoint path from the
  app id. Changing the icon publishes `APP_UPDATED` with `changedFields: ["icon"]`.

## App Portal

Phase 7 (design Design 2, "portal"). The portal lists the apps a signed-in member may open, per environment, and lets
them launch an instance. It exists in EE only; CE has no `automation-app` modules.

- **Schema.** `20261116000002_automation_app_portal.xml` (changeset `20261116000002-1`, its own commit): `app_favorite`
  (primary key `user_id, app_id`; foreign keys to `app` and `user`, both `ON DELETE CASCADE`; `idx_app_favorite_app`) and
  `app_instance_usage` (primary key `user_id, app_instance_id`; foreign keys to `app_instance` and `user`, both
  `ON DELETE CASCADE`; `use_count` defaults to 0; `last_used_date`; `idx_app_instance_usage_app_instance`). `app.category_id`
  (foreign key `fk_app_category` to the global `category` table) and `app.portal_discoverable` came with Phase 1; no column
  was added. The two repositories are `JdbcClient` classes (the composite key has no Spring Data JDBC precedent) with the
  `ResourceGrantServiceImpl` dialect split: `ON CONFLICT` on PostgreSQL, UPDATE then `INSERT ... WHERE NOT EXISTS`
  elsewhere. The non-PostgreSQL path has no test.
- **The rule.** `AppPortalFacade.getPortalApps(environment)` (`isAuthenticated()`; GraphQL `portalApps(environmentId)`)
  lists an instance only when all of these hold: its app is `ACTIVE` and `portalDiscoverable`; the instance is `ACTIVE` and
  has a non-blank URL; the instance lives in a workspace the caller belongs to (checked against the instance's workspace,
  not the app's; a tenant admin counts as a member of every workspace); it is visible to the caller
  (`ResourceVisibilityResolver.filterVisibleIds("AppInstance", ...)`, so a PRIVATE instance needs a grant or the
  creator); and the caller holds `APP_USE` in the instance's own workspace and environment (memoized per workspace and
  environment). `AppKeyAccess.isAllowed` is applied too. This is the set-wise form of the sign-in check
  `hasResourceScope(id, "AppInstance", "APP_USE")` run by `AppInstanceClientAccessChecker`, built from the same
  primitives, and `AppPortalFacadeIntTest` (automation-app-graphql, the real EE `PermissionServiceImpl`) compares the two
  for every user (a tenant admin and an anonymous caller included), both environments and every instance: listed must
  equal "sign-in allows it" plus the portal-only conditions (discoverable, ACTIVE instance, URL, environment). If the two
  ever diverge that test fails. Instances with `user_auth_mode` NONE are listed under the same rule even though sign-in
  does not apply to them (`APP_USE` and visibility still decide).
- **What an entry carries.** A card (`PortalAppDTO`, GraphQL `PortalApp`) is one app: id, name, description, `categoryId`,
  `categoryName`, `hasIcon`, `favorite`, `lastUsedDate` (latest over its instances) and its instances; there is no owner
  and no owning workspace. An instance entry (`PortalAppInstanceDTO`, `PortalAppInstance`) carries id, name,
  `workspaceId`, `workspaceName`, `url`, the caller's `lastUsedDate` and `useCount`. A foreign app (owned by a workspace
  the caller is not in) appears when the caller's workspace hosts a usable instance of it. Apps sort by name
  (case-insensitive) then id; instances by workspace name, name, id. Favorites and usages are two bulk queries;
  category names are one `getCategories` call for the cards in the portal only.
- **Favorites.** `setAppFavorite(appId, favorite)` (`isAuthenticated()`) is per user and app. Adding runs the portal rule
  over the app's instances; an app with no usable instance, or an unknown one, answers the same 404 `APP_NOT_FOUND` "App
  <id> not found". Removing needs no lookup and is idempotent. A favorite of an app that left the portal stays stored but
  its card does not come back (favorites are read only for the apps in the listing).
- **Launches.** `recordAppLaunch(appInstanceId)` is guarded by `hasPermission(#appInstanceId, 'AppInstance', 'APP_USE')`
  and then requires the instance to pass the portal rule; an instance outside the portal throws
  `AuthorizationDeniedException("Access Denied")`, exactly what the guard answers for an unknown or unusable id, so a
  caller cannot tell a hidden instance from a missing one (the drift test compares the two answers through the real
  guard). The count is one atomic upsert (`use_count = use_count + 1`).
- **Last-used touch.** `AppExecutionFacadeImpl.getCurrentUser` (the `/me` call) calls
  `AppInstanceUsageService.touchLastUsedDate` for the signed-in user. The throttle is in the database:
  `AppInstanceUsageServiceImpl.TOUCH_THROTTLE` (15 minutes), an upsert that writes only `WHERE last_used_date < cutoff`
  and inserts a count-0 row when none exists, so it holds across a cluster. It runs `REQUIRES_NEW` and the facade catches
  any `RuntimeException` and logs the class name at WARN, so it cannot fail `/me`. It never changes `use_count`.
- **Member removal.** `AppWorkspaceUserRemovalListener` also deletes the removed user's usage rows for the instances
  living in that workspace and their favorites of the apps those instances belong to, in the removing transaction. A
  favorite is kept when the app has an `ACTIVE` instance in another workspace the user is still a member of.
- **The icon.** `AppPortalFacade.getPortalAppIcon(appId)` (`isAuthenticated()`) serves the icon when the app has a usable
  instance in any environment of the caller's reach (the same computation as the listing). An unknown app, an app outside
  the portal and an app without an icon all answer an empty `Optional`, so the endpoint
  `GET /api/automation/internal/portal/apps/{id}/icon` (`AppPortalApiController`, session security, no guard of its
  own) answers a bodiless 404 for all three. Headers: the detected `Content-Type` (PNG, JPEG or WebP, decided from the
  stored bytes by the shared `AppIconType`), `X-Content-Type-Options: nosniff` and `Cache-Control: max-age=300, private`.
  The admin endpoints `POST|GET|DELETE /api/automation/internal/apps/{id}/icon` are in **The icon**.
- **Writing the category and the flag.** `createApp` (`APP_MANAGE` in the workspace and environment) and `updateApp`
  (`APP_MANAGE` on the app) take `category` and `portalDiscoverable`; no separate publishing scope exists, and the flag may
  be set in any app status (the listing still requires `ACTIVE`). `category` is `{id?, name?}`: an id attaches that
  existing global category and ignores the name; a name without an id attaches the category of that trimmed name,
  creating it when missing (over 256 characters is 400 `APP_FIELD_TOO_LONG`); an unknown id is 400
  `APP_CATEGORY_INVALID`, checked before anything is stored. On create an absent category means none. On update null
  keeps it, and a blank name without an id clears it; `portalDiscoverable` null keeps the flag. Each is written only
  when it changed. `getAppCategories(workspaceId)` (`hasPermission(#workspaceId, 'Workspace', 'APP_MANAGE')`, GraphQL
  `appCategories`) returns `{id, name}` for the categories of the apps in that workspace the caller can see; a portal
  member needs no such query because cards carry `categoryName`.
- **Surfaces.** GraphQL (`app.graphqls`): query `portalApps(environmentId: ID!): [PortalApp!]!` and `appCategories`;
  mutations `setAppFavorite(appId, favorite): Boolean!` and `recordAppLaunch(appInstanceId): Boolean!`
  (`AppPortalGraphQlController`, no guard of its own); types `PortalApp` and `PortalAppInstance`; `AppCategoryInput`, and
  `category` and `portalDiscoverable` on `AppInput` (absent flag means false) and `AppUpdateInput` (absent means
  unchanged); `App` exposes `categoryId` and `portalDiscoverable`. Management REST: `AppInput` and `AppUpdate` gained
  `category` (`AppCategory {id, name}`) and `portalDiscoverable`; there is no REST category list and no REST portal
  listing. The CLI has no app update command (only `create`, `link`, `list`, `status` and `key rotate`), so an app is
  published to the portal from Studio only; its generated client was refreshed for the new fields.
- **Client** (`client/src/pages/automation/portal`): route `automation/portal` (`PrivateRoute` ADMIN and USER, `EEVersion`)
  and an "App Portal" row after Apps in the nav, shown in EE only and to any member (no scope gate). The page has a search
  box and a category filter (built from the returned cards: "All categories", names A to Z, "Uncategorized" only when a
  card has none, hidden when no card has a category; it combines with search and "Clear filters" resets both) and the
  sections Favorites, Recently used (newest first, `RECENTLY_USED_LIMIT` 6) and All apps, each omitted when empty; with no
  cards it shows "No Apps Yet". The environment is the current one from `useEnvironmentStore`. Launching ("Open", or an
  instance row in `PortalInstanceSelect` when the app has several) opens the tab synchronously in the click handler with
  `window.open(url, '_blank', 'noopener,noreferrer')` for an `http` or `https` URL only (the normalised `new URL(url).href`;
  other schemes are a disabled row and nothing renders as a link), then calls `recordAppLaunch` in the background; its
  failure changes nothing beyond the global toast. Favorites are optimistic with rollback on error. Icons load from the
  portal icon endpoint when `hasIcon`, with a glyph fallback; every server string renders as plain text. The app dialog
  (`AppDialog`) has the category (`CategoryFormField`, creatable, fed by `appCategories`; an existing one is sent as
  `{id}`, a typed one as `{name}`, and an untouched one is omitted on update), the "Show in App Portal" switch and the icon
  upload (`AppIconField`: type and size checked first, applied after the save; a failed upload after create keeps the key on
  screen and says so; a per-app version query string refreshes the preview after a change).
- **Audit.** Publishing and unpublishing are audited; favorites and launches are deliberately not. See **Audit,
  metrics and the Activity tab**.

## Audit, metrics and the Activity tab

Phase 8. What an operator can see about an app after the fact.

### Audit events

`AppAuditEvent` (35 names) through `AppAuditPublisher`; every payload also carries `appId`, added by the publisher.
Every guarded write of `AppFacadeImpl`, `AppInstanceFacadeImpl`, `AppDeploymentFacadeImpl`, `AppSharingFacadeImpl`,
`AppWorkflowFacadeImpl` and `AppApiCollectionFacadeImpl` is listed in `AuditDataContractTest` (server-app).

| Area | Events |
|------|--------|
| App | `APP_CREATED`, `APP_UPDATED`, `APP_DELETED`, `APP_ACTIVATED`, `APP_SUSPENDED`, `APP_ARCHIVED` |
| Portal | `APP_PUBLISHED_TO_PORTAL`, `APP_UNPUBLISHED_FROM_PORTAL` |
| Instance | `APP_INSTANCE_CREATED`, `APP_INSTANCE_UPDATED`, `APP_INSTANCE_ACTIVATED`, `APP_INSTANCE_SUSPENDED`, `APP_INSTANCE_TERMINATED`, `APP_INSTANCE_DELETED`, `APP_SECRET_KEY_ROTATED`, `APP_USER_AUTH_CHANGED` |
| Deployment | `APP_DEPLOYMENT_INITIATED`, `APP_DEPLOYMENT_COMPLETED`, `APP_DEPLOYMENT_FAILED`, `APP_DEPLOYMENT_CANCELED` |
| Bindings | `APP_CONNECTION_BOUND`, `APP_CONNECTION_UNBOUND`, `APP_OPERATIONS_CHANGED`, `APP_WORKFLOW_BOUND`, `APP_WORKFLOW_UNBOUND`, `APP_API_COLLECTION_BOUND`, `APP_API_COLLECTION_UNBOUND` |
| Sharing | `APP_VISIBILITY_CHANGED`, `APP_ACCESS_GRANTED`, `APP_ACCESS_REVOKED`, `APP_INSTANCE_VISIBILITY_CHANGED`, `APP_INSTANCE_ACCESS_GRANTED`, `APP_INSTANCE_ACCESS_REVOKED` |
| Member removal | `APP_OWNER_LEFT`, `APP_INSTANCE_OWNER_LEFT` |

- **Portal events.** `APP_PUBLISHED_TO_PORTAL` is published by `createApp` when the app is created discoverable and by
  `updateApp` when `portalDiscoverable` turns on; `APP_UNPUBLISHED_FROM_PORTAL` by `updateApp` when it turns off. They
  come in addition to the `APP_CREATED` and `APP_UPDATED` payloads, not instead of them.
- **Payloads.** `APP_CREATED` carries `name`, `workspaceId`, `portalDiscoverable` and `categoryId` when a category is
  attached. `APP_UPDATED` lists `changedFields` (`name`, `description`, `icon`, `categoryId`, `portalDiscoverable`) and
  carries the new `name`, `portalDiscoverable` and `categoryId` for the fields that changed; **`categoryId` is absent
  when the category was cleared** (the field is in `changedFields`, the key is not in the payload). Instance events carry
  `appInstanceId`; `APP_INSTANCE_UPDATED` lists field names only. Deployment events carry `appInstanceId`,
  `appDeploymentId`, `appVersionId` and `provider` (`APP_DEPLOYMENT_COMPLETED` and `APP_DEPLOYMENT_INITIATED` add
  `versionName`; initiated adds `rollback`). Rebinding publishes `APP_CONNECTION_BOUND` with `previousConnectionId`.
  Creating a version publishes nothing (`AuditDataContractTest` skips it as `VERSION_LABEL`).
- **No secrets.** A payload holds ids, names and field names, never a key, key hash or hint, token, connection
  parameter, configuration value or deployment setting. `AppAuditDataAssertions` (test helper in the `audit` package)
  walks every `publish(event, id, data)` call recorded on the mocked publisher, recursively, and fails on a key whose
  name contains `secret`, `hash`, `hint`, `token`, `password`, `credential`, `authorization`, `parameters`, `settings`
  or `configuration`, on a value containing a forbidden string the test supplied, and on any string shaped like an app
  key, a SHA-256 hash or a JWT. It runs in `@AfterEach` of `AppFacadeTest`, `AppInstanceFacadeTest`,
  `AppDeploymentFacadeTest` and `AppDeploymentStatusJobTest`, so every operation those classes drive is checked;
  `AppAuditDataSecretsTest` proves the helper itself rejects each case. A new event or payload key needs no change to
  pass it, but a new secret-bearing value does.
- **Deliberately not audited:** portal favorites and launches (`setAppFavorite`, `recordAppLaunch`), which are per-user
  bookkeeping rather than changes to a governed resource, listed in
  `AuditDataContractTest.UNAUDITED_GUARDED_METHODS`; version creation; reads. The user-facing event list is
  `docs/content/docs/platform/settings/audit-events.mdx`.

### Metrics

Both counters are built from an `ObjectProvider<MeterRegistry>` read in the constructor (so an app variant without a
registry starts cleanly), as `bytechef_connection_create` is, and are guarded: a failure to count is logged by exception
class name at WARN and never fails the deployment write or the token rejection.

- `bytechef_app_deployment`, tags `provider` (`AppDeploymentProviderType`) and `status` (`READY`, `FAILED` or
  `CANCELED`). Counted once per terminal deployment, in `AppDeploymentSynchronizer.countTerminal`, right after the
  terminal write (`markCurrent` for `READY`, which a recorded deployment and a provider `READY` both reach, `fail`,
  `cancel` and the provider-answer path). A terminal row is never written again, so it counts once; inside a transaction
  the increment is registered `afterCommit`, so a rollback counts nothing.
- `bytechef_app_user_token_rejected`, tag `reason`, counted in `AppUserTokenVerifier` where a rejection is created
  (`invalidToken` and `forbidden`). The reasons are fixed lower-case strings: `decoder_unavailable`,
  `validation_failed` (the decoder rejected the signature or a claim it validates), `undecodable`, `expired`, `issuer`,
  `tenant`, `client_unregistered` (the instance has no OAuth2 client), `audience`, `scope`, `subject`,
  `revocation_unavailable`, `revoked`, `user_inactive` (unknown or deactivated user) and `forbidden` (no `APP_USE`). A
  missing token is answered before the verifier and counts nothing. No id or URL is ever a tag.

### The Activity tab and the invocation log

The app page's Activity tab (`AppActivityTab`) shows the external invocation log of the **instance selected** in the
instance selector, filtered by `appId` and `appInstanceId`, in that instance's workspace and environment, with a User
column.

- **Guard.** `ExternalInvocationLogFacade.getWorkspaceExternalInvocationLogs` needs a platform user holding
  `WORKFLOW_VIEW` on the workspace (not `EXECUTION_VIEW`). The tenant-wide `externalInvocationLogs` is `isTenantAdmin()`
  and passes no instance.
- **Instance filter.** `app_instance_id` is an extra `AND` next to the always-present `workspace_id` and
  `environment`, so it can only narrow. An unknown instance id, or one of another workspace (a foreign instance's rows
  carry the hosting workspace's id, so the owning workspace never sees them here), returns an empty page, not a refusal;
  the `appId` and `mcpServerId` arguments behave the same. Changing the instance remounts the list and starts at page 0.
- **User column.** `user_id` is written by every app writer from `AppPrincipal.userId()`. The facade resolves the
  distinct ids of a page with one `UserService.getUsers` call and shows first and last name, else the email, else the
  login; null for a key-only call and for a user that no longer resolves; the table shows `-` then. `UserService` is an
  `ObjectProvider` there because the same facade bean runs in the EE `execution-app`, which has no `UserService`, where
  `userName` is always null. Only the display value is exposed, never the login separately or the role.
- **Instance visibility.** A row of an app instance follows that instance's visibility: the workspace query shows it
  only when the caller may view the instance (workspace `APP_VIEW` in that environment and `AppInstance` visibility,
  so a PRIVATE instance's rows reach its owner and its grantees). A tenant admin is not restricted: they see every
  row of the workspace and environment, those of deleted instances and of PRIVATE instances without a grant included.
  Rows without an instance are unaffected. The facade asks the optional SPI `ExternalInvocationLogAppInstanceAccess`
  (log api; `AppInstanceInvocationLogAccess` in `automation-app-service`) for a `ViewableAppInstances`: `all()` for a
  tenant admin, otherwise `only(ids)` of the existing instances the caller may view. The repository filters in SQL
  before paging, so totals and pages count only visible rows. Without the SPI bean, instance rows are hidden; no
  shipped app serves the workspace query without it, since only `server-app` has the log GraphQL module and it always
  has Apps. The tenant-wide `externalInvocationLogs` is not filtered.

## Workflow bindings

An app can run workflows through `/api/app/v1/workflows`. Design: `docs/superpowers/specs/2026-10-06-apps-workflows-and-sdk-design.md`.
Plan: `docs/superpowers/plans/2026-10-07-apps-workflow-bindings.md`. Code: `AppWorkflowExecutionFacade` (app-facing),
`AppWorkflowFacade` (admin, beside `AppFacade`), `AppWorkflowService`, `AppCallableWorkflowResolver`, tables
`app_workflow` and `app_workflow_run` (`20261007000001_automation_app_workflow.xml`).

- **What can be bound.** A **Call workflow** workflow (`workflow/newWorkflowCall` trigger) of an **enabled** project
  deployment in the instance's own workspace and environment, whose project deployment workflow is enabled. Webhook-triggered
  workflows are never bindable; they stay public endpoints. `AppCallableWorkflowResolver` is the single definition of
  "callable" for binding, listing and execution: the three paths cannot disagree about what a binding means.
- **Identity of a binding.** `(project_deployment_id, workflow_uuid)`, never a workflow id. Every call resolves the id
  again through `ProjectWorkflowService.fetchProjectWorkflowWorkflowId`, so a redeploy changes what runs with no
  rebinding. Do not cache the id or store it in the row: MCP pins a version, apps deliberately do not. The key follows
  `AppBindingKeys` and is unique per instance also after `toFlatKey` normalisation (same check-then-save race as connection
  bindings).
- **Availability causes.** Deployment or workflow gone, deployment disabled, deployment workflow disabled, deployment in
  another workspace or environment, trigger no longer Call workflow, trigger `inputSchema` not readable JSON or not a
  JSON Schema networknt can compile (`AppWorkflowSchemas.checkReadable`, references resolved eagerly), malformed uuid: all make `AppCallableWorkflowResolver.resolve` return empty, so bind (400 `APP_WORKFLOW_NOT_BINDABLE`), the
  bindable list, the admin list (`available: false`), the app-facing list and execute (409 `APP_WORKFLOW_UNAVAILABLE`)
  agree. Execute's `checkInput` and `getWorkflows`' unreadable-schema branch (`available: false`, label and description
  kept, schemas null) remain as defence in depth; do not remove them. Deleting a deployment is not blocked by bindings
  (no FK, no `ConnectionUsageChecker`-style guard); the binding just becomes unavailable.
- **200/202 contract.** `POST /workflows/{key}` waits up to `waitSeconds` (omitted: `bytechef.app.workflow.default-wait`,
  30 s; allowed `0..max-wait`, 60 s, else 400 `APP_WORKFLOW_INPUT_INVALID`; `0` reads the job's state without waiting).
  200 when the run is `COMPLETED`/`FAILED`/`CANCELLED`, 202 for `RUNNING`/`APPROVAL_REQUIRED`. A failed workflow is run
  state, not an HTTP error: HTTP errors mean no run started. A wait that times out is not an error. Any other awaiter
  failure, or a failure recording the run, propagates without a run id while the job keeps running. The request thread
  waits (not on `appExecutor`, no cap on concurrent waits).
- **Run ownership.** `app_workflow_run` (`run_id` UUID string, `app_instance_id`, `job_id`) is the authorisation record for
  polling, written synchronously because the invocation log is asynchronous. `GET /workflow-runs/{runId}` looks up by
  `(appInstanceId, runId)`: another instance's run, an unknown run and a run whose job is gone all answer 404
  `APP_WORKFLOW_RUN_NOT_FOUND`, never 403. Rows cascade with the instance. `AppWorkflowRunRetentionJob` purges rows older than
  `bytechef.app.workflow.run-retention-days` (90) daily (`run-retention-cron`, default 02:40); a purged run polls 404.
  A retention of zero or fewer days disables the sweep rather than deleting every row. The sweep runs once per
  tenant under that tenant's context (a failing tenant is logged and skipped), as does
  `ExternalInvocationLogRetentionJob`'s.
  Unbinding does not cancel runs and does not remove their rows. `AppWorkflowExecutionFacadeIntTest` pins ownership and
  the cascade against Postgres.
- **Who the job runs as.** `principalJobFacade.createJob(..., projectDeploymentId, AUTOMATION)`, as MCP and webhooks do:
  the principal is the project deployment, so licence usage, variables and the Executions list behave as for any
  deployment run. No `runAsSystem` (unlike the action lane): `createJob` has no authorization check and `createdBy`
  stays honest. Inputs are the deployment workflow's inputs plus `{<triggerName>: input, __triggerName: <triggerName>}`.
- **Shared result mapping.** `WorkflowCallResults` (`platform-workflow-execution-api`) holds the callable-response
  output, pending-approval and approval-form-URL helpers used by both `AutomationMcpToolFacade` and
  `AppWorkflowExecutionFacadeImpl`. Change them for both or neither. Status mapping: `CREATED`/`STARTED` -> `RUNNING`,
  `STOPPED` with a resume id -> `APPROVAL_REQUIRED`, any other `STOPPED` -> `CANCELLED` (`Job.Status` has no
  `CANCELLED` of its own on this line).
- **Recording.** Each execute records one event through `ExternalInvocationRecorder` under
  `ExternalInvocationSource.APP_WORKFLOW` (kind `WORKFLOW`, key as `operationName`, `jobId`, the app and instance ids): outcome `ERROR` for
  `FAILED`, `APPROVAL_REQUIRED` for a waiting run, `SUCCESS` otherwise (a still-`RUNNING` run included). Polls and
  refused attempts are not recorded.
- **The approval link is a capability.** `approvalFormUrl` is the hosted form link (`/job/resume/**` is permit-all), so
  whoever holds it, including the app, may decide the approval. The API has no approve call; the app is expected to show
  the link to a person. Withholding it per binding is an open follow-up.
- **Errors.** `AppErrorType` 118 `APP_WORKFLOW_NOT_FOUND` (404), 119 `APP_WORKFLOW_UNAVAILABLE` (409), 120
  `APP_WORKFLOW_INPUT_INVALID` (400), 121 `APP_WORKFLOW_RUN_NOT_FOUND` (404), 122 `APP_WORKFLOW_NOT_BINDABLE` (400, bind
  time, including a project the caller cannot see). `AppWorkflowFacade` gates `APP_VIEW`/`APP_MANAGE`; GraphQL and REST
  only delegate. `AppWorkflowSchemas` validates input with networknt `json-schema-validator`.
- **`resolve` refuses system deployments.** A deployment named with an API collection, MCP server or A2A server marker,
  or one whose project is a system project (`SystemProjects.isSystemProjectName` or the `__EMBEDDED__` marker), never
  resolves — the same set `getBindableWorkflows` hides. Bind validates through `resolve`, so such a deployment cannot
  be bound through the REST `PUT /app-instances/{id}/workflows/{workflowKey}` either, and an existing binding to one stops
  executing.

## API collection bindings and the app API path

An app calls API Platform endpoints through `/api/app/v1/api/v{collectionVersion}/{contextPath}/{endpointPath}`; this
is the default path for the app's code (design `docs/superpowers/specs/2026-10-16-apps-api-platform-design.md`, plan
`docs/superpowers/plans/2026-10-16-apps-api-platform-bindings.md`). Code: `AppApiCollectionFacade` (admin),
`AppApiExecutionFacade` (app-facing), `AppApiCollectionService`, `AppApiCollectionResolver`, `AppApiEndpointController`.

- **Table.** `app_api_collection` (`app_instance_id`, `api_collection_id`, unique together;
  `20261016000001_automation_app_api_collection.xml`). Rows cascade with the instance; there is no FK to `api_collection`, so deleting a collection leaves a binding that
  resolves to nothing. A whole collection is bound: the instance's key may call every endpoint of it, no per-endpoint list.
- **Resolver.** `AppApiCollectionResolver` is the single definition of "callable": a binding resolves only while the
  collection exists and its hidden project deployment (`"__API_COLLECTION__" + projectId`) exists, is enabled, is in
  the instance's environment and belongs to a project of the instance's workspace. Bindable collections are those of the
  workspace and environment whose deployment exists, disabled ones included, so an admin can bind before enabling.
- **Facade gates.** `AppApiCollectionFacade`: `getApiCollectionBindings` `APP_VIEW`; `getBindableApiCollections`,
  `bindApiCollection`, `unbindApiCollection` `APP_MANAGE`. The bindable list keeps only collections whose
  project the caller can see (`ResourceVisibilityResolver.filterVisibleIds`). Bind publishes `APP_API_COLLECTION_BOUND` only when a row was created, unbind
  `APP_API_COLLECTION_UNBOUND` only when one was deleted. GraphQL (`appApiCollections`, `appBindableApiCollections`,
  `bindAppApiCollection`, `unbindAppApiCollection`), management REST (`/app-instances/{id}/bindable-api-collections`,
  `/app-instances/{id}/api-collections[/{apiCollectionId}]`) and the CLI (`automation app bind|unbind api-collection`) only
  delegate. The client's Apps page has an API collections tab.
- **Errors.** `AppErrorType` 124 `APP_API_COLLECTION_NOT_FOUND` (404; reserved: mapped, not thrown today), 125 `APP_API_COLLECTION_NOT_BINDABLE` (400, bind
  time), 126 `APP_API_COLLECTION_UNAVAILABLE` (409: a path under a bound collection that does not resolve, or a
  matched endpoint whose workflow or deployment is disabled), 127 `APP_API_ENDPOINT_NOT_FOUND` (404: no endpoint of
  an available bound collection matches; never says whether an unbound collection would have). 128
  `APP_EXECUTION_FAILED` (500) is a failed endpoint workflow; a timeout is 504 `APP_EXECUTION_TIMEOUT`.
- **Shared endpoint resolution.** `AbstractApiPlatformEndpointController` (`automation-api-platform-handler-rest`) does
  matching (`/v{collectionVersion}/{contextPath}/{endpointPath}`, collection order then endpoint order, first match
  wins) and runs the endpoint's workflow through its `apiPlatform/newApiRequest` trigger. Subclasses choose the
  searchable collections, the environment, and what an unmatched or unavailable path answers:
  `ApiPlatformHandlerController` (`/api/o/**`, platform API keys) and `AppApiEndpointController` (the app's bound,
  resolvable collections, the instance's own environment; `X-ENVIRONMENT` is ignored). Change matching there, for both.
- **Pass-through and the error marker.** The status, headers and body the workflow chooses pass through unchanged.
  ByteChef's own errors carry `X-ByteChef-App-Error: true`; the controller strips that header from anything a workflow
  writes, so the SDK can trust it (`api.call` throws only on marked responses).
- **Recording.** Each matched call records one event under `ExternalInvocationSource.APP_API` (kind
  `WORKFLOW`, endpoint name as tool, collection name as `operationName`, the app and instance ids); a status of 400 or above is
  recorded as `HTTP <status>`. Unmatched and unavailable paths are not recorded.
- **App-facing listing.** `GET /api-collections` lists every binding with `available`; an unavailable one has empty
  `endpoints`. Endpoints carry method, path and the trigger's request and response JSON Schemas.

## Agent loop (server half)

A coding agent builds API endpoint, scheduled and app-callable workflows, creates API collections, endpoints and data
tables, and deploys through the management MCP; it manages apps through the CLI (Plan D2), which reads the four routes
below. Design: `docs/superpowers/specs/2026-10-08-apps-agent-loop-design.md`, and for API endpoints and scheduled work
`docs/superpowers/specs/2026-10-16-apps-api-platform-design.md`.
Plan: `docs/superpowers/plans/2026-10-08-apps-agent-loop-server.md`. DEVELOPMENT only: every tool refuses another
`environment` with "The app loop runs in DEVELOPMENT only".

- **One definition of callable.** `AppCallableWorkflowResolver` implements the CE SPI `CallableWorkflowChecker`
  (`automation-configuration-api`, `…configuration.workflow`): `check(workflowId)` returns the problems (no
  `workflow/v1/newWorkflowCall` trigger, unreadable `inputSchema`) and the schema. The resolver is keyed by
  `(workspaceId, environment)`, not an `App`, and its listing drops system deployments
  (`SystemProjects.isSystemDeploymentName`) and system projects, as `resolve` always did.
- **`buildWorkflow` kinds** live on the management MCP wrapper, not the delegate: `WorkspaceScopedSubAgentToolCallback`
  takes every `WorkflowKindChecker` bean (`automation-configuration-api`) for `buildWorkflow` only
  (`ToolCallbackContributorConfiguration`), and offers `kind` only when at least one exists: `APP_CALLABLE`
  (`AppCallableWorkflowResolver`, EE), `API_ENDPOINT` (`ApiEndpointWorkflowChecker`, EE) and `SCHEDULED`
  (`ScheduledWorkflowChecker`, CE). An unknown kind is refused with the supported ones. The result names, per saved
  workflow, its uuid and the next step: `deployProject` for `APP_CALLABLE`, `createAppApiCollection` /
  `createAppApiEndpoint` for `API_ENDPOINT`, and for `SCHEDULED` `deployProject` with the uuid in `enabledWorkflows`
  when the app loop is registered (decided by an `APP_CALLABLE` checker being present: the EE app module that
  registers `deployProject` also supplies it), otherwise a deploy in the ByteChef UI with the workflows enabled.
  With a kind the wrapper appends that kind's requirements to the request, installs the capture holder and the
  `AppCallableDraftGuard.APP_CALLABLE_KEY` flag in the forwarded context (`WorkflowPersistCaptureUtils.withCaptureHolder`
  keeps a holder already present), and checks every distinct captured `workflowId` afterwards: nothing captured
  passes the reply through; a failure is an error naming each id and leaves the workflow saved. A check that throws
  reports `workflowId X: could not be checked` (never the exception text), and a workflow deleted in the same run is
  dropped from the capture holder. Under the flag, `ProjectWorkflowTools.updateWorkflow`/`deleteWorkflow` and
  `ScriptTools.updateScriptComponentCode` refuse a workflow outside the project's DRAFT version and name the DRAFT
  id, checked after their facade read.
- **`ProjectDeploymentFacade.deployProject`** owns the deploy rules behind one guard (`DEPLOYMENT_CREATE` on the
  project in DEVELOPMENT, plus `DEPLOYMENT_EDIT` on the deployment when updating one): one DEVELOPMENT deployment per
  project, updated in place so bindings `(deployment id, workflow uuid)` stay valid; refuses unpublished changes (the Data Sync rule over every
  workflow); a row for every workflow of the version (`checkProjectDeploymentWorkflows` deletes rows missing from its
  list); slots from `ComponentConnectionFacade.getWorkflowComponentConnections`; candidates from
  `WorkspaceConnectionFacade.getConnections` filtered to `VALID` + `CONNECTION_VIEW`, AI-provider connections never;
  all or nothing. It writes the deployment disabled; arming is the tool's separate `enableProjectDeployment`, whose
  failure leaves it disabled and is reported as `arming failed`. Redeploying an enabled deployment reads the
  registrations of its armed triggers before the write and disarms them only after the transaction commits (a
  failure is logged at WARN); a write that fails or rolls back leaves the deployment enabled and its triggers armed.
  Its three new collaborators are `@Lazy` so the hand-assembled IntTest contexts that build the facade without them
  still boot.
- **Why deploy and bind ask for different connection scopes.** `deployProject` offers any valid connection the caller
  can see (`CONNECTION_VIEW`): a deployment only runs the connection inside the workspace's own workflows. Binding a
  connection to an app instance (`AppInstanceFacade.bindConnection`) hands it to code ByteChef does not run, through the app's actions,
  tools and the credentialed proxy, so it needs `CONNECTION_EDIT`.
- **API Platform and data-table tools** `createAppApiCollection`, `createAppApiEndpoint`,
  `updateAppApiCollectionVersion`, `listAppApiCollections`, `createAppDataTable`: `AppApiPlatformLoopTools` (EE
  `automation-ai-tool`), registered beside `AppLoopTools`, DEVELOPMENT only and Copilot-independent, named apart from
  the Copilot `createApiCollection`/`listApiCollections`/`createDataTable` to avoid a registry collision. The two
  create tools enable what they create (the collection's hidden deployment, the endpoint's workflow); the admin UI and
  REST keep creating disabled. `createAppApiCollection` returns `bindCommand` (the CLI bind command), also on
  `arming failed`: a disabled collection is bindable, and its calls answer 409 until a `DEPLOYMENT_EDIT` holder enables
  it. `createAppApiEndpoint` resolves each connection slot like `deployProject`, auto-filling only required slots (an
  optional slot is filled only when chosen): `unresolved` creates nothing and lists candidates, `arming failed` leaves
  the endpoint saved but disabled (enabling needs `API_PLATFORM_EDIT` on the endpoint and `DEPLOYMENT_EDIT` on the
  collection's deployment). `updateAppApiCollectionVersion` moves the
  collection to a newer published version in place and reports removed, disabled endpoints and the missing
  connections and inputs that disabled them.
- **MCP tools** `deployProject`, `listConnections`, `listAppCallableWorkflows`: `AppLoopTools` (EE
  `automation-ai-tool`), registered by `AppLoopMcpContributorConfiguration` (`appLoopMcpContributor`) through
  `WorkspaceScopedFlatToolCallback`, contributed only when the app module is present. Refusals are returned as values
  (`status: refused`, `error`): the wrapper reduces a thrown exception to its class name.
- **REST for the CLI** in `openapi-management.yaml`: `GET /app-instances/{id}/bindable-connections?componentName=`,
  `GET /app-instances/{id}/bindable-workflows`, `GET /apps/component-operations`, `GET /connections` (new
  `WorkspaceConnectionApiController`, tag `workspace-connection`). Facades own the guards
  (`AppInstanceFacade.getBindableConnections`: `CONNECTION_VIEW` in the environment; `getCallableWorkflows`:
  `DEPLOYMENT_VIEW` in DEVELOPMENT); every controller extends `AbstractAppApiController` for 403 on a denial.
  `AppBindableConnectionDTO` gained `usable`. Management REST outside DEVELOPMENT is read-only: every mutating
  operation calls `AppEnvironmentScope.requireDevelopment` first and answers 403 `APP_OPERATION_DEVELOPMENT_ONLY` in
  STAGING and PRODUCTION, before any facade call (GraphQL, the Studio surface, is not restricted). The two exceptions
  are the version create and the deployment record (see **Versions and deployments**). Every
  `/app-instances/{id}/...` route is scoped to the request's environment (the `environment` parameter, absent
  meaning PRODUCTION, the one the API key authenticated in): `AppEnvironmentScope.requireInEnvironment` first calls
  `AppInstanceFacade.requireAppInstanceInEnvironment`, which carries no `@PreAuthorize` of its own, so an instance of
  another environment is `APP_INSTANCE_NOT_FOUND` (404), indistinguishable from a missing one. It also answers
  `APP_INSTANCE_NOT_FOUND` when the caller lacks `APP_VIEW` on the instance, so an instance of a workspace the caller
  cannot see answers exactly like an unknown id; a caller who may view but not manage still gets 403 from the guard. A caller who may view
  none of an app's instances gets the app with an empty `instances` list.
- **End to end:** `AppLoopToolsIntTest` (`server-app`) runs build → publish → deploy → bind → call, then a rebuild
  through `draftWorkflowId`, and the same binding runs the new definition.

### CLI, template and plugin (Plan D2)

- **Commands** (`cli/commands/automation`): `AutomationAppCommand` (`automation app create|link|list|status|key
  rotate`), `AutomationAppInstanceCommand` (`automation app instance create|list|update`), `AutomationAppBindingCommand`
  (`automation app bind connection|bind workflow|bind api-collection|unbind ...`), `AutomationConnectionCommand`
  (`automation connection list`). They share `AppCommandSupport`, `AppLinkFile` and
  `EnvFile`. Every command addresses an **instance** (`/app-instances/{id}/...`). `create`, `link`, `key rotate`,
  `instance create`, `instance update`, `bind` and `unbind` are DEVELOPMENT only and are refused locally before any request elsewhere
  (the server's `APP_OPERATION_DEVELOPMENT_ONLY` is the rule that counts); `list`, `status` and `instance list` work
  in every environment, and
  `AuthInterceptor` sends the profile's environment on every request. `create` makes the app and its first
  instance and writes the instance id into `.bytechef/app.json`; `key rotate` calls
  `POST /app-instances/{id}/secret-key`. Instance resolution (`AppCommandSupport.resolveInstanceId`): `--instance-id`
  (on `link`, `status`, `key rotate`, every `bind` and `unbind`) names an instance of the app in the profile's
  environment, otherwise an error lists them; without it, a link file gives the instance from the file with no
  request; with `--app-id` and no link, `getApp` and exactly one instance in the profile's environment and workspace,
  otherwise an error listing the instances (DEVELOPMENT: pass `--instance-id` or `link --app-id N --instance-id M`;
  other environments: pass `--instance-id`, linking is DEVELOPMENT only). `--app-id` naming the linked app overrides
  a link file written for another host, environment or workspace.
- **Sign-in mode and instances.** `--user-auth-mode NONE|OPTIONAL|REQUIRED` (case-insensitive, checked before any
  request) is on `app create` and `instance create`; absent means the field is not sent and the server applies
  `REQUIRED`. A 409 `APP_USER_AUTH_UNAVAILABLE` (authorization server off) keeps the server's message and adds
  `To create it without sign-in, pass --user-auth-mode NONE.` `instance create --app-id N --name ... --env-file F`
  adds an instance to an app in DEVELOPMENT (`--workspace-id` is the new instance's workspace, defaulting to the
  profile's), writes its key to `--env-file`, which is **required** (a default of `.env.local` would overwrite the
  linked instance's key; a relative path resolves against the linked app directory, as for `key rotate`), and neither
  writes nor updates `.bytechef/app.json`. `instance update [--instance-id N] [--url U] [--name X]` sends
  `PATCH /app-instances/{id}` with only the given fields and refuses locally when neither is given; it is how a builder
  sets the URL (`http://localhost:3000` for local development) that sign-in needs, since an instance without a URL
  has no OAuth2 client. `app create` and `instance create` take no `--url` (the create inputs carry none). `instance list` shows each instance
  with its workspace, status, sign-in mode and URL. `status` lists the instances and the sign-in mode. A key is never
  written for an instance other than the linked one without an explicit `--env-file`
  (`AppCommandSupport.requireEnvFileForAnotherInstance`, a local refusal before any request); this includes a link
  file that is ignored because its host or workspace differs from the profile's. `--workspace-id` is also an option
  of the commands that resolve a profile. **The CLI sets an instance's URL and name with `instance update` but cannot set its
  configuration**, which is set in the admin UI or over GraphQL. An instance with sign-in on and no URL has no OAuth2 client, so sign-in works for it
  only after a URL is set (DEVELOPMENT instances also accept the `localhost:3000` callback).
- **The client** `cli/clients/automation-app` is generated from a committed copy of `openapi-management.yaml` with
  `Environment` inlined (the server file's relative `$ref` does not resolve from there). Re-copy, re-inline and run
  `generateClient` whenever the server spec changes; nothing fails if you forget.
- **`.bytechef/app.json`** (`id`, `uuid` (the instance's), `instanceId`, `workspaceId`, `environment`, `host`; no
  secret) is found by walking up like git, keyed on the file, never on a `.bytechef` directory alone: `~/.bytechef/`
  holds the CLI's `config`. A file written for another host, environment or workspace is an error, and a file without
  `instanceId` is rejected with a relink hint. `create` and `link` refuse where one exists, so a directory never gets a
  second app.
- **The key** goes only into the env file: only the `BYTECHEF_URL`/`BYTECHEF_APP_KEY` lines change, the write goes
  through an owner-only temporary file renamed over the target (a symlinked env file is refused), and
  `git check-ignore` decides the not-ignored warning (silent outside a repository). A failed write after create names
  the app id and the two recovery commands, never the key.
- **`bind connection` is a full replace** (empty lists, `proxyEnabled: false`), picks the only usable bindable
  connection when `--connection-id` is absent, and checks names against `component-operations` before its single PUT.
- **`status`** takes each binding's component version from `bindable-connections` (the management `AppBinding` has
  none); a 403 there (an `APP_VIEW`-only caller) or a connection that can no longer be bound shows `none` in table output and `null` in
  JSON.
- **Deployment commands** (`AutomationAppDeploymentCommand`): `automation app deployment record --version <name> [--url]
  [--commit] [--app-id] [--instance-id]`, `automation app deployment list [--app-id] [--instance-id] [--limit]`,
  `automation app version create --name <name> [--commit] [--git-ref] [--description] [--app-id]` and
  `automation app version list [--app-id] [--limit]`, each with `--profile --host --token --environment --output`. They
  use `AppCommandSupport.resolve`, not `resolveDevelopment`, so they run in every environment, write no file and are
  the CLI's two writes outside DEVELOPMENT. `record` retries exactly once on a 409 whose body names
  `APP_VERSION_NAME_ALREADY_EXISTS` (a lost race on a new version name, so the second attempt finds the version) and
  never retries another 409. 401 and 403 hints name the profile's environment and the needed scope (`APP_VIEW` for the
  lists, `APP_DEPLOY` for the writes; `AutomationClientFactory.toCliException`). Phase 6 adds `deployment deploy`,
  `deployment rollback`, `deployment cancel` and `instance deployment-settings set|clear|get`; see **Provider-driven
  deployments**.
- **Template:** `AGENTS.md`'s "Setting up the app with ByteChef" section carries the loop (it also covers
  `--user-auth-mode`, `instance list`, `--instance-id` and the DEVELOPMENT-only writes); `AppTemplateCliCommandDriftTest`
  (server-app, next to `PluginSkillTagDriftIntTest`) fails when it names a `bytechef …` command with no
  `@Command(name = …)` under `cli/commands`. `check:template` cannot do this: its job has no CLI binary.
- **Plugin:** the `bytechef-app-builder` skill. It states the order of preference (API Platform endpoint → Call-workflow
  binding → direct action or tool → proxy, saying why past the first) and the decision table. Its `mcp` fences are
  `edition: ee` and keyed only on the EE app-loop tools (`createAppApiCollection`, `createAppApiEndpoint`,
  `updateAppApiCollectionVersion`, `listAppApiCollections`, `createAppDataTable`, `deployProject`, `listConnections`,
  `listAppCallableWorkflows`), so a CE server never receives the app-loop instructions; its prose names the CE tools
  (`buildWorkflow`, `publishProject`, `createProject`), the one documented exception to the "`uses:` lists every
  tool" rule. The template's `AGENTS.md` carries the same order and loop.

## SDK

`@bytechef/app` lives in `sdks/frontend/automation/app/` (npm workspace: `library/` is the package, `test-app/` a
minimal TanStack Start app). Design: `docs/superpowers/specs/2026-10-06-apps-workflows-and-sdk-design.md`, "Design —
B. SDK". Four entry points: `.` (core client, no runtime dependencies), `./tanstack-start` (middleware, sign-in handlers,
handlers, validators, types), `./tanstack-start/server-fns` (the eleven ready-made server functions, nothing else) and `./ai-sdk`.

- **`./tanstack-start` must never reach a `createServerFn` call.** Start registers every server function declared in
  a module the app imports, used or not: when the nine lived in `./tanstack-start`, importing only
  `bytechefMiddleware` published all nine, `proxy` included, as RPC endpoints at predictable ids. Importing any one
  ready-made function registers all eleven; importing only `./tanstack-start` registers none.
  `test-app/scripts/check-bundle.mjs` follows the built `tanstack-start.js` and its `./*.js` chunks and fails on any
  `createServerFn(`, and fails unless `server-fns.js` declares all eleven at module level.

- **Types come from the server contract.** `library/src/generated/openapi.ts` is generated from
  `automation-app-public-rest/openapi.yaml` (`npm run generate:api`) and committed; `npm run check:api` regenerates and
  fails on drift, so change the spec and the generated file together. The package never declares its own wire types.
- **The `@tanstack/react-start` peer entry is load-bearing.** Start's Vite plugin treats a dependency that names it as
  a peer as a framework package and compiles it, which is what makes server functions shipped in the package work;
  `library/src/package.test.ts` pins it. Its range is `^1.168.10` (`MINIMUM_TANSTACK_START_VERSION`): 1.168.10 is the
  first `@tanstack/react-start` whose entry exports `createCsrfMiddleware` (1.168.0 to 1.168.9 ship it in
  `@tanstack/start-client-core` without re-exporting it), established by bisecting the published files.
  `bytechefRequestMiddleware` throws a message naming that version when the export is missing, so it never runs
  server functions without the CSRF check; `package.test.ts` also checks that the tested and the template's pinned
  versions are inside the range. Start compiles only module-level `x = createServerFn(…)` declarations, so
  there is no factory: eleven ready-made server functions configured from the environment, and an app that wants
  explicit options writes its own on `bytechefMiddleware(options)`. The library build stays unminified (`build.minify:
  false`) so `dist` keeps those declarations. The server functions ship in the package, opt-in. The template does **not** import them: each is a public endpoint, so it writes its own server functions on
  `bytechefMiddleware()`; only the middleware, validators and handlers come from the package.
- **Core surface.** `api.list()` and `api.call({method, path, body?, query?, headers?})` (`path`
  `v{n}/{contextPath}/{endpointPath}`, `.`/`..` segments refused) → `{status, headers, body}`: a workflow-chosen 4xx or
  5xx is data; only a response marked `X-ByteChef-App-Error: true` throws `BytechefAppError`. TanStack: `callApi`,
  `listApiCollections` ready-made server functions with `callApiHandler`/`callApiInput`. `connections.list()`, `actions.list()` (`AppAction[]`, `inputSchema` typed as `JsonSchema`) and
  `actions.execute`, `tools.list/execute/executeByName`, `proxy`, `workflows.list/execute/getRun`. `AppAction` is the
  generated schema with `inputSchema` narrowed to the SDK's `JsonSchema`, exported from `@bytechef/app`. A server that
  predates `/actions` answers 404 without a `{code, message}` body, which surfaces as `BytechefAppError` code
  `HTTP_ERROR` with status 404; callers treat that as "no action schemas on this server".
- **Environment variables.** `BYTECHEF_URL` and `BYTECHEF_APP_KEY`, read lazily on the first request, never under a
  `VITE_` prefix (Vite inlines those into the client bundle); client construction throws in a browser. Explicit
  `createBytechefClient` options win. `test-app/scripts/check-bundle.mjs` (`npm run check:test-app`) fails the build if
  the client bundle contains the variable name, `/api/app/v1`, the key's value, `BYTECHEF_SESSION_SECRET`,
  `bytechef_session`, `bytechef_login` or the session secret's value.
- **Sign-in in the core client** (`createBytechefClient`; `client.auth`, `client.context`; server runtimes only):
  - `context.get()` is `GET /context`, kept per client (closure state, nothing at module level) for `contextTtlMs`
    (default `DEFAULT_CONTEXT_TTL_MS`, 60 seconds; `0` fetches on every call after an answer; a negative or non-finite
    value throws). Concurrent callers share one fetch and a failed fetch is not kept. `context.invalidate()` drops the
    entry; `auth` does it itself after `SIGN_IN_UNAVAILABLE` and after an `invalid_client` answer.
  - `auth.createAuthorizationRequest({redirectUri})` returns `{url, state, codeVerifier}` (PKCE S256, scope
    `openid profile app:use`); `auth.exchangeCode({code, codeVerifier, redirectUri})`,
    `auth.refresh({refreshToken})` (refresh tokens rotate, so an answer without a new one is `INVALID_RESPONSE`) and
    `auth.getCurrentUser()` (`GET /me`; `APP_USER_REQUIRED` locally, without a request, when the client has no
    `userToken`). The token POST is form-encoded with `client_id` and no `Authorization` header, and redirects are not
    followed. A `userToken` client option adds `X-ByteChef-User-Token` to every request; a caller-supplied header of
    that name is dropped.
  - **The endpoints come from `GET /context`** (`authorizationEndpoint`, `tokenEndpoint`, `clientId`); no path is
    hardcoded and no discovery document is fetched, because ByteChef serves no OIDC document. The ID token is neither
    decoded nor returned: identity comes from `/me`. **The instance key is never sent to the authorization server.**
  - **There is no `revoke`.** The authorization server has `/oauth2/revoke`, but a public client has no way to
    authenticate there as configured, and `/context` offers no revocation endpoint. Sign-out clears the app's session
    only; the tokens live until they expire or an administrator revokes the grant.
  - Error codes: server-side `APP_USER_REQUIRED` (401), `APP_USER_TOKEN_INVALID` (401), `APP_USER_FORBIDDEN` (403);
    client-side `SESSION_ENDED` (the token endpoint's `invalid_grant`: the user must sign in again),
    `SIGN_IN_MISCONFIGURED` (any other 4xx of the token endpoint, a 3xx, for example a redirect URI mismatch or
    `invalid_client`), `SIGN_IN_UNAVAILABLE` (`/context` has no `clientId` or endpoints: the instance has no OAuth2
    client), `RATE_LIMITED` (429), plus `HTTP_ERROR`, `NETWORK_ERROR`, `TIMEOUT`, `ABORTED`, `INVALID_RESPONSE`. The
    message names only the OAuth error code, never the description, token, code or verifier. `isSignInRequired(error)`
    is true for `APP_USER_REQUIRED`, `APP_USER_TOKEN_INVALID` and `SESSION_ENDED`, not for `APP_USER_FORBIDDEN`.
    `isSameState` compares the `state` in constant time.
- **TanStack Start adapter** (`./tanstack-start`). Sealed cookies, both `HttpOnly; SameSite=Lax`, `Secure` unless the
  app URL's host (the request's when `appUrl` is unset) is localhost, `*.localhost`, `127.0.0.1` or `[::1]`:

  | Cookie | Holds | Path | Lifetime |
  |--------|-------|------|----------|
  | `bytechef_session` | access token, its expiry, refresh token, session expiry | `/` | `sessionMaxAgeSeconds` (3600) |
  | `bytechef_login` | PKCE verifier, `state`, redirect URI, return path | `/auth` (the `basePath`) | 10 minutes |

  - They are sealed (AES-256-GCM, a key derived per cookie name with HKDF-SHA-256) with `BYTECHEF_SESSION_SECRET`, at
    least 32 characters, read lazily, **with no fallback**: a missing or short secret throws, and only in a mode other
    than `NONE`. An unreadable or tampered cookie counts as no session and is cleared. The pending-login cookie is
    cleared on every callback outcome.
  - **Registration:** `createStart(() => ({requestMiddleware: bytechefRequestMiddleware(options)}))`. It returns two
    middlewares: a CSRF check for server function calls (`Sec-Fetch-Site`, else `Origin`, else `Referer` must be the
    app's own; a request with none is refused), then `bytechefAuthMiddleware(options)`. TanStack Start applies its
    default CSRF check only while no `requestMiddleware` is set, so **registering the bare auth middleware alone drops
    the CSRF protection**. The auth middleware throws when it runs inside a server function's `.middleware([...])`
    chain (it could not set cookies there); it is listed there only for `context` types, and Start runs it once.
  - Per request: `<basePath>` routes pass through without a user. `userAuthMode` comes from the shared client's
    `context.get()`. `NONE` never reads a cookie and adds no user token. Otherwise it reads the cookie, refreshes the
    tokens when the access token expires within `refreshMarginMs` (30 s), calls `GET /me` with a per-request client
    carrying the user token, and sets `context.user` and `context.bytechef`. `REQUIRED` without a user: a page gets a
    302 to `<basePath>/login?returnTo=...`, a server function call 401 `APP_USER_REQUIRED`; a forbidden user gets 403.
    `OPTIONAL` goes on with no user. A session the authorization server (`invalid_grant`) or ByteChef
    (`APP_USER_TOKEN_INVALID`) refuses is cleared and counts as none; any other error (for example
    `SIGN_IN_MISCONFIGURED`) is thrown, so a misconfigured client gives an error, not a redirect loop. Every response
    is `Cache-Control: no-store`.
  - **Handlers**, wired as routes: `loginHandler` (GET `<basePath>/login`: PKCE, state, the pending-login cookie, a 302
    to the authorization endpoint; with `NONE` it only redirects back; when sign-in is not set up, `SIGN_IN_UNAVAILABLE`
    or `SIGN_IN_MISCONFIGURED`, it answers 503 with fixed text that says to set the instance URL, never a 500;
    `basePath` must stay `/auth`, the only callback path the server registers), `callbackHandler` (GET: constant-time `state`
    check, code exchange, session cookie, redirect to the return path; `access_denied` answers 403, the rest 400 with
    fixed text) and `logoutHandler` (POST only: a GET is 405, a cross-site POST 403; it clears the cookie and answers
    303 to `/`).
  - **`appUrl` / `APP_URL`:** the public origin used for the redirect URI `<appUrl><basePath>/callback`, the cookies'
    `Secure` flag and the CSRF origin. It must be an absolute URL with a scheme (an invalid one throws); without it
    the request's own origin is used, which behind a proxy may be an internal address that does not match the
    registered callback. The template reads `APP_URL`; the SDK reads no variable for it.
  - **Return path:** `safeReturnPath` accepts only a path on the app's own origin (one leading `/`, no `//`, no
    backslash, no control character, at most 2048 characters), checked on the raw value and again after URL
    normalisation, so `/.//evil.com` and `/a/..//evil.com` give `/`. `basePath` goes through the same check.
  - **Refresh:** one refresh per session per process: concurrent requests with the same refresh token share one
    promise, and its result is reused for 30 seconds by a request still holding the old cookie. The map is per
    process; across processes (serverless, replicas) the server's reuse grace covers it: a second process that
    presents the same refresh token within 10 seconds of the rotation gets the same new tokens, so both responses set
    a cookie with the same contents. A request that reaches the token endpoint with the spent token later than that gets
    `invalid_grant` and clears its session (see **Known limits**).
  - **Per sign-in mode:** `REQUIRED` sends visitors to sign in and every page and server function sees a user;
    `OPTIONAL` renders for everyone and `context.user` is undefined without a sign-in; `NONE` reads no cookie, needs no
    session secret and calls only `GET /context`. Each signed-in request calls `GET /me`.
- **No retries anywhere.** Actions are not idempotent; a silent retry can create a record twice. Failures surface as
  `BytechefAppError` with the server's `code`, or `NETWORK_ERROR`, `TIMEOUT`, `ABORTED`, `HTTP_ERROR`,
  `INVALID_RESPONSE` client-side. A failed workflow run is data, not an exception.
- **Adapter seams.** TanStack: `handlers.ts` (plain `({data, context}) => …` functions returning JSON-serialisable
  results, since Start rejects `unknown`), `validators.ts` (plain functions that throw `TypeError`; plain objects only),
  `middleware.ts` (puts the client on `context.bytechef`) and `serverFns.ts` (the only file that calls
  `createServerFn`, re-exported only by `src/server-fns.ts`). The ready-made `executeWorkflow` does not wait (Start gives handlers no abort signal): it returns the
  run after `waitSeconds` and the browser polls `getWorkflowRun`; the core's `wait` is unbounded except by `signal`.
  `workflows.execute` rejects `waitSeconds * 1000 >= timeoutMs` with a `RangeError` before sending (a starting POST
  cut off by the client timeout leaves a started run with no `runId`). With `waitSeconds` omitted and a `timeoutMs`
  under 35 s, it sends `floor(timeoutMs / 1000) - 5` (minimum 0) so the server's 30 s default wait cannot outlast
  the client.
  AI SDK: `ai/tools.ts` builds every tool with `dynamicTool` + `jsonSchema` (validates nothing client-side; `ai` needs
  `zod` in the app), returns `BytechefAppError`s to the model as text except `ABORTED`, a 401, and a 403 that is
  `APP_DISABLED` or carries no code (rethrown to the developer; `APP_OPERATION_NOT_ALLOWED` stays model-visible since
  it lists the allowed operations), logs unavailable codes with `console.error`, tells the model a workflow `TIMEOUT`
  may have started a run, and flat names come from the
  server listing (`AppBindingKeys.toFlatKey` of the binding key, `__`, UPPER_SNAKE tool name; see Tool names) or the
  SDK's `toFlatKey` for workflows.
  Binding and workflow keys are lower-case (`^[a-z][a-z0-9_-]{0,63}$`).

## Template

`create-bytechef-app` (`sdks/frontend/automation/app/create/`, npm workspace member, same version as `@bytechef/app`)
scaffolds a TanStack Start app from the vendored `create/template/`. Design:
`docs/superpowers/specs/2026-10-07-apps-template-design.md`. The point of the template is that it is safe by default
and extensible by an AI coding agent: `AGENTS.md` says to discover with `npm run -s bytechef:status` and to reach
third-party systems only through `@bytechef/app`.

- **Two server modules, development only.** `src/server/bytechef.ts` holds only the module-level request middleware
  (`bytechefRequestMiddleware(authOptions)`, whose second element, the auth middleware, is exported as `bytechef`) and
  no `createServerFn`. `src/server/dev.ts` holds the ByteChef-calling server functions
  (`getAppOverview`, `tryAction`); `src/server/overview.ts` is the pure `buildOverview` mapping. Every client path to
  `dev.ts` is gated on `import.meta.env.DEV`: the route's loader imports it dynamically behind that check and the route
  picks the overview component on DEV, so neither the server functions nor the overview UI is in a production build,
  which renders `#placeholder`. **Why two modules and no inline gate:** Start's compiler registers only module-level
  `x = createServerFn(…)` declarations, and it registers every server function of any module the app imports, so a
  `createServerFn` cannot be declared conditionally in place. A dynamic import whose condition is
  `import.meta.env.DEV` is replaced by `false` and eliminated at build time. `NODE_ENV=development vite build` is the
  one way to ship the dev code, and `npm run check` catches it.
- **The leak check** (`scripts/check-bundle.mjs`, run by `npm run check` after `tsc` and `vite build`, and by the
  Dockerfile build) derives the dev-only server function names from `export const x = createServerFn` in `dev.ts`
  (it fails if that file is missing or declares none, so keep it) and fails when any server build file
  contains `<name>_createServerFn_handler` or imports a chunk whose base name is exactly `dev` (`./dev-<8-char
  hash>.mjs`, so a user `dev-utils.ts` does not match), or when no server build exists. It scans every build output
  that exists — client `.output/public`, `.vercel/output/static`, `dist/client`; server `.output/server`,
  `.vercel/output/functions` (Nitro writes the `.vercel/output` pair when `VERCEL=1`) — so a stale directory can only
  add failures, and the success line names the directories it scanned. It also fails when the server
  build registers any of the eleven public ready-made server functions from `@bytechef/app/tanstack-start/server-fns`
  (`<name>_createServerFn_handler`; importing one registers all eleven) unless `package.json` has
  `"bytechef": {"allowReadyMadeServerFns": true}`, which turns the failure into a warning; the template does not set
  it. The key values are those of every variable whose name ends with `BYTECHEF_APP_KEY` (so `VITE_BYTECHEF_APP_KEY`
  too), taken from `process.env` **and** from `.env`, `.env.local`, `.env.production`, `.env.production.local` parsed
  with `util.parseEnv`, and must appear in no client file; the client must also contain no `BYTECHEF_APP_KEY` and no
  `/api/app/v1`, and must contain `/_serverFn/` (a sanity check that it is a Start client build). Client code therefore
  never names the variable; setup help lives in the README and in server-side errors. The same check forbids
  `BYTECHEF_SESSION_SECRET`, `bytechef_session` and `bytechef_login` in the client, fails on the session secret's value
  in any client file (values from the environment and the same env files, never printed) and on
  `BYTECHEF_SESSION_SECRET=<value>` in a server file. `test-app/scripts/check-bundle.mjs` keeps its own copy of the
  forbidden list; `check:template` has leak cases for the secret value, its name, `bytechef_session` and
  `bytechef_login`.
- **Sign-in in the template.** The scaffolded app signs users in with no code of the author's:
  - `src/start.ts` is `createStart(() => ({requestMiddleware}))` with the composed pair from `src/server/bytechef.ts`,
    built from `authOptions = {appUrl}` where `appUrl` is the `APP_URL` server variable, read through
    `createIsomorphicFn().server(...).client(() => undefined)` so its name never reaches the client. Without
    `APP_URL` the SDK uses the request's origin. Server functions keep `.middleware([bytechef])` for the context types;
    Start runs the auth middleware once per request. `AGENTS.md` rule 9 says to keep `src/start.ts` as it is and not to
    register the sign-in middleware alone (CSRF, see **SDK**).
  - `src/routes/auth/login.tsx`, `callback.tsx` and `logout.tsx` mount the three handlers (GET, GET, POST) with the
    same options. Routes under `/auth` run without a user.
  - **What reaches the browser is a display name only.** `src/server/user.ts` (`getSignIn`, a GET server function)
    returns `{enabled, user: {name} | null}`, the name being first and last name or the login; `enabled` is
    `userAuthMode !== 'NONE'`. The root route's loader calls it and its layout renders the name with a sign-out form
    (a POST to `/auth/logout`, never a link), or a Sign in link when signed out and sign-in is on, or nothing with
    `NONE`. No token, cookie value, secret or full `context.user` is sent.
  - `.env.local` gets `BYTECHEF_SESSION_SECRET` from `create/src/env.mjs`: 48 random bytes, base64url, generated on
    every scaffold (a variable in the shell is deliberately not used), written with mode `0600`, never printed;
    `writeEnvLocal` keeps the secret of an existing file. `.env.example` lists it and `APP_URL`, both empty.
    `bytechef:status` also reports `userAuthMode` (`null` plus one stderr line when `/context` fails).
  - **The app needs ByteChef reachable to serve pages.** The middleware reads `GET /context` on every request through
    the SDK's 60-second cache (the first request, and any request after the cache expires, waits for it); when
    ByteChef is unreachable and the cache is cold, pages answer 500, in every mode including `NONE`.
  - `scripts/mock-bytechef.mjs` takes `--user-auth-mode=NONE|OPTIONAL|REQUIRED` (default `NONE`). With sign-in on it
    serves `/oauth2/authorize` (validating the client id, PKCE S256, `state` of at least 16 characters, `app:use`) and a
    form-encoded `/oauth2/token` with single-use codes, PKCE verification and rotating refresh tokens, plus `/me`; with
    `NONE` the endpoints in `/context` are null, as with the authorization server off. `check:template` drives
    `OPTIONAL` and a `REQUIRED` round trip (login, callback, signed-in page, sign-out) against it.
- **`bytechef:status` contract** (`node scripts/bytechef-status.mjs`; the script loads `.env.local` itself with
  `util.parseEnv`, as the bundle check reads env files, and when the file is absent writes one stderr line instead of
  Node's `--env-file-if-exists` notice): JSON on stdout,
  diagnostics on stderr, exit 1 on a missing URL or key (the SDK's configuration message) or a ByteChef error
  (`BytechefAppError` code and message), with stdout empty. It is built from `/connections` first, so every allowed
  action and tool is listed even when the component is unavailable (`componentVersion: null`, `inputSchema`/
  `parameters` null; tools carry the server's flat name). `actionSchemas` is false when `/actions` failed in any way (a
  404 from an older server, a 500, a network error), and actions are then listed by name with `inputSchema: null`;
  the script adds one stderr line `Action schemas unavailable: <code or message>` and still exits 0. The development
  overview degrades the same way. A variable already in the
  environment, even an empty one, wins over `.env.local`, as it did under Node's `--env-file`.
- **Dependencies.** Every dependency is an exact version (an upstream minor release must not break every new app); the
  scaffolder rewrites only `@bytechef/app`, to its own version. `create/template/package-lock.json` locks the
  transitive dependencies; `npm run template:lock` (workspace root, `scripts/template-lock.mjs`) regenerates it from
  the template's `package.json` **without** `@bytechef/app` (published in lockstep with the scaffolder, so it cannot
  be locked ahead), and the app's first `npm install` adds that one entry and changes no locked version. The
  scaffolder keeps the lockfile for npm and deletes it for pnpm, yarn and bun with a one-line notice; with
  `--no-install` the next steps say to install before deploying, because the Dockerfile's `npm ci` needs the lockfile
  to list `@bytechef/app`. `template:lock` runs a real `npm install` plus `npm prune` (a `--package-lock-only` run left
  an `extraneous` entry) against `https://registry.npmjs.org/` with `npm_config_registry` stripped, and the
  `check:template` lock step fails on any `resolved` host other than `registry.npmjs.org`. The shadcn components import `cn` from the `cn` package
  and `Slot` from the unified `radix-ui` package, not `@/lib/utils` or `@radix-ui/react-slot`, so both stay
  dependencies; `components.json` still lists the `@/lib/utils` alias, which does not exist.
- **The generated app needs Node.js >= 22.18.0** (template `engines`; the scaffolder warns on older Node). Nitro's
  development server fails on Node 22.17 and earlier with `Vite environment "ssr" is unavailable`; 22.18.0 onward and
  Node 24 pass `check:template`. CI's `node-version: 22` resolves to the newest 22.
- **Hosting is Nitro v3 (beta).** `nitro/vite` produces `.output`; `start` is `node .output/server/index.mjs` (honours
  `PORT`/`HOST`); `vercel.json` sets `"framework": "tanstack-start"` and `"buildCommand": "npm run check"`, so every
  Vercel deploy typechecks, builds and runs the leak check against `.vercel/output`, and a failing check fails the
  deploy. The Dockerfile uses `node:24-alpine` in both stages; with a lockfile it runs `npm ci` and, if that fails,
  prints `package-lock.json does not match this npm version; falling back to npm install (locked versions are kept)`
  and runs `npm install`; without one it runs `npm install`. It runs the bundle check in its build stage, and the
  runtime stage runs as `USER node`. There are no Google Fonts. `AGENTS.md` rule 9 still has
  the agent run `npm run check` for local work.
- **The scaffolder.** No secret on the command line: there is no key flag, and `BYTECHEF_APP_KEY` is copied only from
  the environment, with a notice. `.env.local` is written with mode `0600`. `_gitignore` and `_dockerignore` are renamed
  on copy because npm strips such dotfiles from published packages; for the same reason the package's `files` list
  excludes local build output and `.env*` secrets from `template/`, and the `cpSync` filter skips the same top-level
  names. Under `npm create` the flags must follow `--`; the scaffolder refuses a second positional argument and says
  so, and it blames npm for a swallowed flag only when that flag did not reach it: `npm_config_url` non-empty, or `npm_config_install` set at all (npm writes a swallowed `--no-install` as the empty string). `--url` must be http(s); a line break in the URL or the key is refused before anything is written; `@scope/name` creates `name/` with the scoped package name. `--help`/`-h` prints the usage. Next
  steps print `npm run -s bytechef:status` for npm only (`-s` is npm's) and `<manager> run bytechef:status`
  otherwise. The workspace `.gitignore` (`sdks/frontend/automation/app/.gitignore`) carries `!create/bin/` because
  the repo-root `.gitignore` ignores `bin/`.
- **Proof in CI.** `npm run check:template` (`scripts/check-template.mjs`, mock server `scripts/mock-bytechef.mjs`,
  fixture `scripts/fixtures/app.json`) in the `sdk-app` job: it checks the formatting of the template's own files (the workspace `.prettierignore` lists the generated and starter-only ones; `npm run format` checks the same set), packs both packages, scaffolds with
  `npm exec --package=<tgz>`, checks the scaffolded lockfile against the pins and the public registry
  (`scripts/template-lockfile.mjs`; a pin bumped without `template:lock` fails), installs from the tarball and checks
  the install changed no locked version and added only `@bytechef/app` and optional peer entries (nested removals are
  reported, not failed), checks `npm ci --dry-run` passes, runs the app's `npm run check`,
  compiles every ```ts/```tsx
  block of the scaffolded `AGENTS.md` (written to `src/agents-recipes/`, `@ai-sdk/anthropic` stubbed, `ai`/`zod`
  installed `--no-save`, `tsc --noEmit`, then deleted), proves a route importing a ready-made server function and a
  `NODE_ENV=development` build each fail that check, and drives `bytechef:status` (including `/actions` answering
  404 and 500), the production server and the dev server against the mock (which fails the run on any POST). Linux and macOS only; needs registry access.

## Configuration

`bytechef.app.*` binds through **`ApplicationProperties`** (prefix `bytechef`, `ignoreUnknownFields=false`), as a
nested `App` class: `Execution{concurrencyLimit, timeout}`, `Proxy{maxRequestBytes, maxResponseBytes, timeout}` and
`WorkflowRun workflow{defaultWait, maxWait, runRetentionDays, runRetentionCron}` and `Deployment{pollCron, timeout}`. A standalone
`@ConfigurationProperties` for `bytechef.app` would fail startup. No Java field initializers; defaults live in
server-app's `application-bytechef.yml` (`ApplicationPropertiesTest` covers them): execution concurrency limit 10 and timeout 60 s; proxy
10485760 request bytes, 10485760 response bytes, 30 s timeout; workflow default wait 30 s, max wait 60 s, run retention
90 days, retention sweep cron `0 40 2 * * *` (the `@Scheduled` placeholder repeats
that cron as its default so contexts without the yml still boot); deployment poll cron `*/30 * * * * *` and timeout
`30m` (`AppDeploymentStatusJob` falls back to 30 minutes when the timeout is unset or not positive).

## Known limits and open decisions

Everything below is still true. Add a limit here, not in the section it came from.

### Sign-in and the template

- With the authorization server off, switching an instance to `NONE` or terminating it only logs a warning
  (`deleteClient`) but still clears `registered_client_id`, so a client registered while the server was on stays in
  `oauth2_registered_client`, and turning sign-in on again once the server is back fails because the id is taken.
- With the server off, an existing `REQUIRED` or `OPTIONAL` instance cannot be renamed or have its URL changed
  (`updateClient` needs the server: 409 `APP_USER_AUTH_UNAVAILABLE`).
- `workspaceRole` of `/me` is null for a custom role; the custom role's name is not exposed. Renaming the app does not
  refresh its instances' client names; only renaming an instance does. Without `bytechef.public-url`, every user token
  is invalid even though the authorization server derives its own issuer from the request.
- Sign-out does not revoke tokens: it clears the app's session cookie only (the SDK has no `revoke`), and the tokens
  stay valid until they expire (5 minutes for the access token, 60 for the refresh token) or an administrator revokes
  the grant. Per-user connections and per-binding user rules are not built.
- Every page of an app needs ByteChef reachable: the SDK reads `GET /context` on every request through a 60-second
  cache, so with ByteChef down and a cold cache pages answer 500 in every mode, `NONE` included. Each signed-in request
  calls `GET /me`.
- Refresh across processes: a refresh token presented again more than 10 seconds after its rotation (for example a
  request with the old cookie that waited on a cold start that long) is refused and that response clears the session
  cookie, which signs the user out if it reaches the browser after the rotated cookie. Separately, a refresh makes the
  previous access token count as revoked at once (`AuthorizationServerTokenRevocationChecker` accepts only the
  authorization's current access token), so a request that passed the middleware with the old token just before
  another request of the same session refreshed gets `APP_USER_TOKEN_INVALID` on its next ByteChef call; in the
  middleware's `GET /me` that clears the session. Both need requests that straddle the moment of a refresh, once per
  access token lifetime (5 minutes).
- The CLI cannot set an instance's configuration; that is set in the admin UI or over GraphQL. An instance with sign-in
  on and no URL has no OAuth2 client until `instance update --url` or the admin UI sets one.
- Template lockfiles written by npm 11 drop an optional nested peer entry (`nitro/node_modules/lru-cache`, an optional
  peer of `unstorage`) that npm 10's `npm ci` requires, and npm 10's `npm install` adds it back; no single lockfile
  satisfies both. The Dockerfile (Node 24) falls back to `npm install` when `npm ci` refuses the lockfile, and CI's
  `sdk-app` job (Node 22) tolerates the re-added entry. pnpm, yarn and bun scaffolds resolve their transitive
  dependencies fresh. The devtools plugin's event bus listens on `localhost:4206` in development only and falls back to
  an OS-assigned port when that is taken.

### Versions and deployments

- Nothing was run against a real Vercel account; the deploy hook provider is tested with mocks and the reference agent
  on its own.
- Versions are app-level, so a member with `APP_DEPLOY` in any one environment can create a version name that every
  environment then sees. `ForeignAppInstanceDTO` carries only `currentAppDeploymentId`, not a version name.
- The status sweep runs only where `appExecutor` exists (only `server-app` sets the property); elsewhere provider
  deployments are never advanced.
- A crash between the `QUEUED` commit and the provider call leaves a row that blocks deploys of that instance until the
  sweep's deadline (30 minutes by default) fails it.
- The hook inherits the HTTP Client's request timeout and its response size is unbounded; the configured URLs are not
  blocked from reaching private networks, as with the HTTP Client itself.
- The sweep polls with the instance's current deployment connection, not the one the deployment started with. The
  connection picker lists connections the caller does not own; choosing one is refused by the server (toast).
- REST and CLI settings are string maps: a non-string value set over GraphQL reads back but cannot be written by the
  CLI.

### Portal

- The by-hand scenario (an administrator publishes an app, a viewer launches it signed in) was not run; the behavior is
  covered by unit, IntTest and client tests only. The non-PostgreSQL path of the two portal repositories has no test.
- Member removal keeps a favorite only for an app with an `ACTIVE` instance in another workspace the user still
  belongs to; reachability is judged by workspace membership only, not by the portal's own computation.
- The app dialog cannot clear a category (as the project dialog cannot), although the server clears on a blank name.
- Categories are global and unscoped: any existing category can be attached by id, and a name typed in the dialog can
  reuse one a project created; its name is then visible to the app's portal users. Two concurrent creates of the same
  new name can hit `uk_category_name` and answer 500, as for projects.
- A browser may keep an icon for five minutes after the caller's access is revoked (`max-age=300, private`). An app
  with a usable instance but a stored icon that is not an image answers `APP_ICON_INVALID`, as the admin read does.
- `recordAppLaunch` throws a Spring Security exception directly; its message `Access Denied` must match the guard's,
  which the drift test checks.
- Reassigning the owner of an app or instance after the owner left is not built (see **Member removal**), and the
  listener's Spring wiring has only a unit test with mocks.

### Audit and the Activity tab

- Rows of a deleted app instance appear in the workspace query only for a tenant admin; every other member, a
  workspace admin included, no longer sees them.
- In the EE `execution-app` the Activity tab's `userName` is always null (no `UserService` bean there).
- The Activity tab accumulates pages through an effect (a focus refetch can misorder rows; `useInfiniteQuery` is the
  follow-up), and its "View in External Invocations" link carries `appId` only; `?appId=` is read on mount only.

### Server and platform

- **Component visibility** (`ComponentVisibilityProvider` filtering of the listings, and the `COMPONENT_DISABLED` /
  `ACTION_DISABLED` 403s on execution) is not on this line: it arrives with `0_p_component_policies`.
- Typed bindings and an instructions endpoint were deliberately not built.
- The code-artifact plugin skills (`bytechef-code-workflow`, `bytechef-custom-component`) belong to the code_workflows
  and custom_components ports and are present only on branches that include them.
- The approval link of a workflow binding (`approvalFormUrl`) cannot be withheld per binding; any holder of it may
  decide the approval. Withholding it is an open follow-up.
- Action input schemas come from `ActionInputSchemaGenerator`, while tools keep the flat
  `JsonSchemaGeneratorUtils.generateInputSchema`; moving tools over is an open follow-up.
- Push to the app's `url` when scheduled results are ready (spec Decision 5) is a later follow-up: today the app reads
  them by calling the read endpoint.

### Execution and proxy

- Response-spool write failures inside `http.execute` still classify as 502, and header values with chars above
  0xFF pass validation and the JDK rejects them (500, not 400). Both need a platform change.
- A non-App `ConfigurationException` from `executeAuthorizationApply` reaches the app as 422 `COMPONENT_ERROR` with
  the cause's `toString()` (same channel as the action path).
- Lane permits: the permit wait and the run share one deadline, so the timeout bounds end-to-end latency; a call that
  ignores interrupts keeps its permit until it returns. A per-app cap is a follow-up. `InterruptedException` is reported as a timeout. Refused attempts (allow-list or availability) are not
  recorded in the invocation log.
- `getTools`/`getBindings`/`getActions` read credential-store state per binding and list the operations of
  unavailable connections (see Action listing).
- `AppConnection.getAppInstanceId()` and `setProxyAllowedMethodList(null)` are NPE hazards (brief-mandated shape); `bind()`
  with null proxy methods NPEs.
- A bearer whose tenant id contains characters outside `[A-Za-z0-9_]` (for example `base64("!!!:abc")`) is answered
  **403**, not 401, on every `/api/**` path of every key type: `TenantAwareApiKeyAuthenticationFilter`
  (`platform-security-web-api`, the MCP filter, untouched by this work) wraps the request in
  `TenantContext.runWithTenantId` before any key-type filter runs, and its `IllegalArgumentException` surfaces as an
  error dispatch. `AppApiKeyAuthenticationConverter`'s own tenant validation is correct but is not reached for that
  input on the monolith. The fix belongs in that filter (fall through on an invalid tenant id).
- The touch write's `REQUIRES_NEW` begin/commit failures escape `AppInstanceServiceImpl`'s catch (500); `AppPrincipal.current()`
  ignores `isAuthenticated()`; `AppAuthenticationToken.getName()` returns the principal's record `toString`, which
  matters if auditing ever reads it.
- `AppSecretKeyDTO`, `AppInstanceService.CreatedAppInstance`/`RotatedKey`, `AppSecretKeys.GeneratedKey` and
  `AppGraphQlMapper.AppInstanceSecretKeyResponse` redact the key in `toString`. The generated `AppInstanceSecretKeyModel` (REST)
  cannot be edited and still prints it; do not log it.
- The connection-delete DTO's used-flag builds description strings and fetches the app per binding on list pages.
- Pre-existing and out of scope: `TrialFilter` (`billing.enabled`) re-runs the chain under a session tenant when a
  session cookie accompanies an API key; AWS temp storage keys by name without a UUID; the OAuth2 refresh race.
