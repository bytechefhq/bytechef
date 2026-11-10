# Embedded: admin-managed credentials for provisioned integration instances

Issue: [#7398](https://github.com/bytechefhq/bytechef/issues/7398) (part 2; part 1, the connection parameter headings, is already committed on branch `7398`).

## Intent

A tenant admin can provision an integration instance for a connected user (Connected Users → user sheet → Add integration). The admin types the credentials (or runs the OAuth2 popup) during provisioning. After that the admin loses the credentials completely: the connection is reassigned to the connected user (`ConnectionService.updateCreatedBy(connectionId, connectedUser.getExternalId())` in `ConnectedUserIntegrationFacadeImpl.provisionIntegrationInstance`), and the connected user sheet shows only a valid/invalid status (`ConnectedUserCredentialStatus`). When a token the admin provisioned expires or must be rotated, the admin has no way to see or replace it.

At the same time, the embedded admin Connections page lists every embedded connection in the environment, including the ones connected users created for themselves (`ConnectionAdminFacadeImpl.getConnections` → `ConnectionServiceImpl.getConnections` filters by type only, never by owner). Those credentials belong to the tenant's end customers and must not be visible to, or editable by, the tenant admin.

The principle (user's decision): **an admin manages only the credentials an admin entered. Credentials a connected user entered stay theirs.**

## Goals

1. The embedded admin Connections page and the admin connection endpoints cover only the tenant's own connections. Connections owned by a connected user are excluded.
2. For an integration instance an admin provisioned, an admin can **view** its credentials (obfuscated) and **replace** them, from the connected user sheet.
5. The embedded admin Connections page gets Update credentials for the tenant's own connections, like automation.
6. The admin embedded workflow builder no longer exposes a connected user's self-entered credentials.
3. Replacing credentials is audited.
4. Docs describe both.

## Non-goals

- Admins viewing or editing credentials of instances a connected user activated themselves, or connections a connected user created through the SDK / Connect dialog.
- Auditing views (user's decision: no audit event for viewing).
- Notifying the connected user when an admin replaces credentials.
- A replace-credentials action inside the admin embedded workflow builder.
- Changing who may delete a connected user's connection.
- Automation (non-embedded) connections: unchanged.
- Making instances provisioned before this change manageable (see "Existing data").

## Anti-goals

- An admin can read a connected user's self-entered credential through any admin endpoint, even obfuscated.
- Raw (non-obfuscated) credential values reach the browser.
- A credential value ends up in an audit event.

## Decisions

### 1. Admin connection endpoints exclude connected-user connections (user's decision)

A connection is **owned by a connected user** when `ConnectedUserConnectionMembership.getConnectedUserOwnedConnectionIds` / `isOwnedByConnectedUser` says so (integration instance connections and `connected_user_connection` rows). Shared connections live in `connected_user_shared_connection`, not `connected_user_connection` (`ConnectedUserConnectionServiceImpl.updateShared`), so they stay admin connections.

For the tenant admin endpoints these connections do not exist:

- `ConnectionAdminFacadeImpl.getConnections` leaves them out; `getConnection`, `updateConnection`, `deleteConnection` throw the same `NoSuchElementException` (`Connection id=%s not found`) as `requireEmbedded`.
- `ConnectionTagApiController`: `getConnectionTags` returns only tags of admin connections; `updateConnectionTags` (`PUT /connections/{id}/tags`) rejects connected-user connections the same way (today it has no embedded or ownership check at all).
- This also removes them from admin connection pickers that use the same list, for example the integration instance configuration dialog. Intended.
- Ruling: shared connections count as admin-entered, so the admin sees them (obfuscated) everywhere, including the workflow builder list.
- Ruling: the tag endpoints are dual-caller. The controller routes a connected-user caller (embed JWT) to `ConnectedUserConnectionFacade` (own connections only, gated by `isCurrentConnectedUser`) and everyone else to `ConnectionAdminFacade`.

### 2. Provenance marker: `provisioned_by` (my call, approved)

Add a nullable `provisioned_by` varchar column (same length as `created_by`) to:

- `integration_instance`: set by admin provisioning (`IntegrationInstanceAdminFacadeImpl.provisionIntegrationInstance` -> `ConnectedUserIntegrationFacadeImpl.provisionIntegrationInstance`) to the admin's login (`SecurityUtils.fetchCurrentUserLogin()`).
- `connected_user_connection`: set when a tenant admin creates a connection for a connected user (`ConnectedUserConnectionFacadeImpl.createConnectedUserConnection` called by an admin, which is what the admin embedded workflow builder does). Left null when the connected user creates it.
- Ruling: the create and list endpoints are dual-caller too. The controller routes a connected-user caller to `createConnectedUserConnection` / `getConnectedUserConnections` and an admin to `createConnectedUserConnectionAsAdmin` / `getConnectedUserConnectionsForAdmin`. The admin create also reassigns `created_by` to the connected user's external id, as provisioning does, so the user can reauthorize, retag and delete it.

Never changed afterwards, including when the connected user reauthorizes (decision 7). One Liquibase changelog in its own commit, subject `7398 Upgrade - Add provisioned_by to integration_instance and connected_user_connection`. Additive only (both tables are released).

### 3. Admin access to connected users' connections (user's decision)

Rule: an admin sees and manages only credentials an admin entered.

- **Connected user sheet, provisioned integration instance** (`provisioned_by` set): Credentials action, view obfuscated, replace (decisions 5 and 6).
- **Admin embedded workflow builder** (`getConnectedUserConnections` with `isTenantAdmin()`, `ConnectedUserConnectionFacadeImpl`): still lists all of the connected user's connections so the admin can pick one for the user's workflow, but a connection that no admin entered (no `provisioned_by` on its `connected_user_connection` row, or on its integration instance) is returned **without** `authorizationParameters`, `parameters`, `connectionParameters` and `baseUri` (name, component, version, status, tags only). Admin-entered ones keep their obfuscated parameters. Called by the connected user themselves: unchanged. The builder has no replace-credentials UI today; none is added (non-goal).
- Admin delete of a connected user's connection through `deleteConnectedUserConnection`: unchanged (out of scope).

### 4. Shared credentials-replace logic (my call)

Move the checks of automation's `WorkspaceConnectionFacadeImpl.updateConnectionCredentials` (status must be `ACTIVE`, version must match; both raise `ConfigurationException` with `ConnectionErrorType.INVALID_CONNECTION`, which the global handler maps to **400**) into one platform `ConnectionFacade` method that ends in `replaceAuthorizationParameters`. Callers:

- **Automation** `updateConnectionCredentials`: additionally rejects a connection whose type is not `AUTOMATION` (today a tenant admin can call it with any connection id, including a connected user's embedded connection). Existing audit `CONNECTION_CREDENTIALS_UPDATED` unchanged.
- Ruling: every automation connection operation (get, update, update tags, delete, disconnect, update credentials) answers the same not-found for a non-automation or unknown id.
- **Embedded admin Connections page** (user's decision, include): new `ConnectionAdminFacade.updateConnectionCredentials(id, parameters, version)`, tenant admin, admin connections only (decision 1), REST `PUT /connections/{id}/credentials` in `embedded-configuration-rest` with `{parameters, version}`, audited with a new `ConnectionAuditEvents.CONNECTION_CREDENTIALS_UPDATED` (data `connectionId`). Client: pass `useUpdateConnectionCredentialsMutation` to the embedded Connections list's `ConnectionDialog`, the same way `client/src/pages/automation/connections/components/connection-list/ConnectionListItem.tsx` does.
- **Provisioned integration instance**: decision 5.

### 5. Provisioned integration instance: view and replace (my call)

Tenant admin endpoints in `embedded-configuration-rest`, facade `IntegrationInstanceAdminFacade` (`@PreAuthorize("isTenantAdmin()")`):

- `GET /integration-instances/{id}/connection`: the instance's connection, same shape and obfuscation as the admin `ConnectionApiController.getConnection` (`ObfuscateUtils.obfuscate(value, 28, 8)` on `authorizationParameters`, `parameters` not returned). 404 when the instance does not exist or `provisioned_by` is null. No environment parameter: tenant isolation is enough, as for the other admin endpoints.
- Ruling: `ConnectedUserConnectionFacade.updateIntegrationInstanceConnectionCredentials` enforces the rule itself, not only its caller: the connection must back an integration instance of that connected user with `provisioned_by` set, otherwise not-found.
- `PUT /integration-instances/{id}/connection/credentials` with `{parameters, version}`: replaces the credentials **the way the connected user's reauthorize does** (`ConnectedUserConnectionFacadeImpl` reauthorize path, `withIntegrationCredentials`): the integration instance configuration's connection parameters (for example the tenant's OAuth2 client id and secret) are merged back in, then the shared logic of decision 4 runs (status, version, replace). Same 404 rules. Audited with `IntegrationAuditEvents.INTEGRATION_INSTANCE_CREDENTIALS_UPDATED`, data `integrationInstanceId`, `connectionId`, `connectedUserId`, never values.
- `ConnectedUserIntegrationInstance` (connected user admin API) gets a read-only `provisioned` boolean. Not the admin's login.

After any `openapi.yaml` change: `./gradlew -p <module> generateOpenAPI`, then regenerate the client middleware.

### 6. UI (my call)

- **Connected user sheet** (`ConnectedUserSheetPanelIntegrationListItem`): for rows with `provisioned`, a **Credentials** action opens a dialog that shows the connection read-only with `ConnectionParameters` (Connection Parameters / Authorization Parameters, from the GET) and an **Update credentials** button. Updating reuses `ProvisionIntegrationDialogConnectionStep` with the instance's configuration as a `ProvisionableIntegrationInstanceConfiguration`: it hides the configuration-supplied fields via `sharedConnectionParameterNames`, and OAuth2 uses the configuration's `oAuth2AuthorizationParameters` and `redirectUri`, so the admin never retypes the app's client id and secret. The configuration comes from the provisionable integrations query for the connected user; if the instance's configuration is no longer provisionable (disabled), the dialog is view-only with a short explanation. Save calls the PUT with the connection's `version`, then invalidates the connected user and instance queries as the row's other mutations do.
- **Embedded admin Connections page**: Update credentials in the existing `ConnectionDialog` (decision 4).
- Rows without `provisioned` show only the status, as today.

### 7. Connected user reauthorizes an admin-provisioned connection

The user still owns the connection and can reauthorize it (`CONNECTION_REAUTHORIZED`). `provisioned_by` stays set, so the admin can still view (obfuscated) and replace it. Accepted (my call): the admin provisioned it and remains responsible.

### 8. Auditing (user's decision: create, update, delete; never view)

- Create: `INTEGRATION_INSTANCE_PROVISIONED` (exists); `CONNECTION_CREATED` for admin-created connected-user connections (exists).
- Update: `INTEGRATION_INSTANCE_CREDENTIALS_UPDATED` (new); embedded `ConnectionAuditEvents.CONNECTION_CREDENTIALS_UPDATED` (new); automation `CONNECTION_CREDENTIALS_UPDATED` (exists).
- Delete: `INTEGRATION_INSTANCE_DELETED`, `CONNECTION_DELETED` (exist).
- No values in any event.

### 9. Docs

Ruling: the docs ship with the feature in the next release and carry no "Coming soon" note.

- `docs/content/docs/platform/embedded/build/connections.md`: the admin list covers only tenant connections; Update credentials for them; connected users' connections are not shown to admins.
- `docs/content/docs/platform/embedded/monitor/connected-users.md`: "Credentials of provisioned integrations" (which rows, view, replace, configuration values kept, after reauthorize, older instances not covered, audit event); the workflow builder shows a user's own connections by name only.
- `docs/content/docs/platform/settings/connections.mdx`: replacing credentials applies to automation connections only (one sentence).

## Existing data

Instances provisioned before the `provisioned_by` column exists have it null and are treated as self-activated: no Credentials action. There is no reliable way to backfill (audit is disabled by default, and the connection's `created_by` was overwritten). Accepted, pending review.

## Testing

Ruling: tests live in the existing `<Class>IntTest` classes; there are no mocked facade unit tests.

- Facade IntTests: ownership filtering for admin list/get/update/delete and tags get/update (instance connection, connected-user connection, shared admin connection, plain admin connection); `provisioned_by` set by admin provisioning and by admin-created connected-user connections, not by self-activation or self-creation; the builder list strips parameters of user-entered connections only; instance credentials GET/PUT 404 without `provisioned_by`; PUT merges configuration parameters; status and version checks (400); automation mutation rejects non-automation connections.
- Controller WebMvc IntTests for the new endpoints and for caller routing on the dual-caller endpoints; non-admin denial is asserted in the facade IntTests, where the `@PreAuthorize` guards live.
- Audit data keys present, no values.
- Liquibase changelog applies (module `testIntegration`).
- Client: Credentials action only on provisioned rows; view and update flow; Connections page update credentials; builder picker shows no parameters for user-entered connections.
- Live check: a connected user's self-created connection disappears from the admin Connections page; an admin replaces a provisioned API-key credential and the instance keeps working.

## Left to the builder

- Exact repository queries for the ownership check (a single "connection ids owned by connected users" query is fine).
- Placement and icon of the Credentials action in the row.
- Query keys and cache invalidation after a replace (invalidate the connected user and the instance queries, as the row's other mutations do).
- Whether the remote-client / runtime-job-app stubs need the new facade methods (follow the EE remote client stub convention).
- Name of the shared platform facade method.
