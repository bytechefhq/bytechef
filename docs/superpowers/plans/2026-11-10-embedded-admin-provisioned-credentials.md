# Embedded Admin-Managed Credentials Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tenant admins stop seeing connected users' own credentials, can replace credentials of the tenant's own embedded connections, and can view (obfuscated) and replace the credentials of integration instances an admin provisioned.

**Architecture:** A `provisioned_by` marker on `integration_instance` and `connected_user_connection` records which credentials an admin entered. Admin endpoints filter by connected-user ownership (`ConnectedUserConnectionMembership`). One platform `ConnectionFacade.updateCredentials` carries the replace checks for automation, the embedded Connections page and provisioned instances.

**Tech Stack:** Java 25 / Spring Boot 4 / Spring Data JDBC / Liquibase / OpenAPI generator; React 19 / TanStack Query / Vitest.

**Spec:** `docs/superpowers/specs/2026-11-10-embedded-admin-provisioned-credentials-design.md`

## Global Constraints

- Branch `7398`, one PR. Commit subjects: server `7398 <Description>`, client `7398 client - <Description>`, docs `7398 docs - <Description>`, Liquibase `7398 Upgrade - <Description>`. Subject only: no body, no `Co-Authored-By`, no code comments in commits.
- `openapi.yaml` edits in their own commit; generated server and client output in its own commit; Liquibase changelog in its own commit.
- After any `openapi.yaml` edit: `./gradlew -p <module dir> generateOpenAPI` before anything else. Never hand-edit generated files.
- Files under `server/ee/` use the ByteChef Enterprise license header and the `@version ee` Javadoc tag.
- No credential value in any audit event. Views are never audited.
- Credentials returned to the browser are always obfuscated with `ObfuscateUtils.obfuscate(value, 28, 8)`; `parameters` is never returned.
- Version or status conflict on a credentials replace raises `ConfigurationException(..., ConnectionErrorType.INVALID_CONNECTION)` (HTTP 400), matching automation.
- Integration tests end in `IntTest`; controller tests are WebMvc IntTests; unit tests end in `Test`.
- Java style: blank line before control statements and after variable modification, no trailing blank line in a class body, descriptive names.
- Client: sorted object keys, sorted import destructures, `Icon`-suffixed lucide imports, `twMerge`, interfaces end in `I` or `Props`.

## Review Focus

1. Shared admin connections (`connected_user_shared_connection`) must stay visible and editable on the admin Connections page after ownership filtering: Task 2 test `testGetConnectionsKeepsSharedAdminConnections`.
2. Replacing credentials of a provisioned OAuth2 instance must keep the configuration's client id and secret: Task 7 test `testUpdateIntegrationInstanceCredentialsKeepsConfigurationParameters`.
3. A tenant admin must not be able to replace a connected user's credentials through automation's GraphQL mutation: Task 3 test `testUpdateConnectionCredentialsRejectsEmbeddedConnection`.
4. An instance a connected user activated themselves returns 404 for both credential endpoints, not 403 (no existence leak): Task 7 tests.
5. The admin workflow builder must still list a connected user's own connections (name only) so the admin can pick one: Task 6 test `testGetConnectedUserConnectionsForAdminKeepsUserConnectionsWithoutCredentials`.

---

### Task 1: `provisioned_by` column and domain fields

**Files:**
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/resources/config/liquibase/changelog/embedded/configuration/20261110130000_embedded_configuration_added_provisioned_by.xml`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/domain/IntegrationInstance.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/domain/ConnectedUserConnection.java`

**Interfaces:**
- Produces: `IntegrationInstance.getProvisionedBy(): @Nullable String`, `setProvisionedBy(@Nullable String)`; `ConnectedUserConnection.getProvisionedBy()`, `setProvisionedBy(...)`. Column `provisioned_by` (`VARCHAR(256)`, nullable; match the length of `created_by` in the existing `integration_instance` changelog) on both tables.

- [ ] **Step 1:** Write the changelog: two `addColumn` changesets (author `ivicac`), one per table, nullable, no default. Follow the format of `20261110120000_embedded_configuration_renamed_automation_workflow_to_automation.xml`. `master.xml` already `includeAll`s this directory.
- [ ] **Step 2:** Add `@Column("provisioned_by") private String provisionedBy;` with getter/setter to both domain classes; include it in `equals`/`toString` only if those already list all columns.
- [ ] **Step 3:** Run `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:testIntegration --tests '*IntegrationInstance*IntTest'`. Expected: PASS (changelog applies on Testcontainers Postgres). Delete stale copies under `build/resources/` first if the file was renamed.
- [ ] **Step 4:** Commit the changelog alone: `7398 Upgrade - Add provisioned_by to integration_instance and connected_user_connection`. Then commit the domain fields: `7398 Add provisionedBy to IntegrationInstance and ConnectedUserConnection`.

### Task 2: Admin connection endpoints exclude connected-user connections

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/ConnectionAdminFacadeImpl.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/facade/ConnectionAdminFacade.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-rest/embedded-configuration-rest-impl/src/main/java/com/bytechef/ee/embedded/configuration/web/rest/ConnectionTagApiController.java`
- Test: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/facade/ConnectionAdminFacadeTest.java` (create, or extend if a test class for this facade exists)

**Interfaces:**
- Consumes: `ConnectedUserConnectionMembership.getConnectedUserOwnedConnectionIds(Set<Long>)`, `isOwnedByConnectedUser(long)`.
- Produces: `ConnectionAdminFacade.getConnectionTags(): List<Tag>` and `ConnectionAdminFacade.updateConnectionTags(long id, List<Tag> tags)`, both admin-connections only; `requireAdminConnection(long id)` (private) throws `NoSuchElementException("Connection id=%s not found")` for non-embedded or connected-user-owned ids.

- [ ] **Step 1: Write failing tests** (Mockito, `ConnectionAdminFacadeImpl` with mocked `ConnectionFacade`, `ConnectionService`, `ConnectedUserConnectionService`, `ConnectedUserConnectionMembership`):
  - `testGetConnectionsExcludesConnectedUserConnections`: facade returns DTOs 1, 2, 3; membership says {2, 3} are owned → result ids `[1]`.
  - `testGetConnectionsKeepsSharedAdminConnections`: shared id 4 not owned → present.
  - `testGetConnectionThrowsForConnectedUserConnection`, `testUpdateConnectionThrowsForConnectedUserConnection`, `testDeleteConnectionThrowsForConnectedUserConnection`: `assertThatThrownBy(...).isInstanceOf(NoSuchElementException.class)` and no delegate call (`verify(connectionFacade, never())...`).
  - `testUpdateConnectionTagsThrowsForConnectedUserConnection`, `testGetConnectionTagsReturnsOnlyAdminConnectionTags`.
- [ ] **Step 2:** Run `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests '*ConnectionAdminFacadeTest'`. Expected: FAIL.
- [ ] **Step 3:** Implement: inject `ConnectedUserConnectionMembership`; `getConnections` filters out owned ids in one membership call; `getConnection`/`updateConnection`/`deleteConnection` call `requireAdminConnection`; add the two tag methods (tags = tags of the admin connections from `getConnections(null, null, null, null)`; update = `requireAdminConnection` then `connectionFacade.update(id, tags)`). Switch `ConnectionTagApiController` from `connectionFacade` to these `connectionAdminFacade` methods.
- [ ] **Step 4:** Run the test command. Expected: PASS. Run the module's `ConnectionApiController*IntTest`/`ConnectionTagApiController*IntTest` if they exist. Expected: PASS.
- [ ] **Step 5:** Commit: `7398 Hide connected users' connections from the embedded admin connection endpoints`.

### Task 3: Shared credentials replace and automation type check

**Files:**
- Modify: `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/facade/ConnectionFacade.java`
- Modify: `server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/facade/ConnectionFacadeImpl.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/WorkspaceConnectionFacadeImpl.java`
- Test: `ConnectionFacadeTest` in platform-connection-service (extend), `WorkspaceConnectionFacadeTest` in automation-configuration-service (extend or create)
- Modify: EE remote-client / runtime-job-app stubs of `ConnectionFacade` if they exist (`grep -rl "implements ConnectionFacade" server`), throwing `UnsupportedOperationException`.

**Interfaces:**
- Produces: `ConnectionFacade.updateCredentials(long id, Map<String, ?> parameters, int version): void`: `ConfigurationException` "Credentials cannot be replaced on a connection with status %s" unless `ACTIVE`; `ConfigurationException` "Connection id=%s was modified by someone else; reload and try again" on version mismatch; then `replaceAuthorizationParameters(id, parameters, version)`.

- [ ] **Step 1: Failing tests:** `testUpdateCredentialsRejectsInactiveConnection`, `testUpdateCredentialsRejectsStaleVersion`, `testUpdateCredentialsReplacesAuthorizationParameters` (platform); `testUpdateConnectionCredentialsRejectsEmbeddedConnection` (automation: connection type `EMBEDDED` → `ConfigurationException`, `connectionFacade.updateCredentials` never called), `testUpdateConnectionCredentialsDelegatesForAutomationConnection`.
- [ ] **Step 2:** Run both modules' tests for these classes. Expected: FAIL.
- [ ] **Step 3:** Move the two checks from `WorkspaceConnectionFacadeImpl.updateConnectionCredentials` into `ConnectionFacadeImpl.updateCredentials`; the automation method keeps `@Audited` and `@PreAuthorize`, adds the type check (same message style, `INVALID_CONNECTION`), then calls `connectionFacade.updateCredentials`.
- [ ] **Step 4:** Run tests. Expected: PASS. Run `./gradlew :server:libs:automation:automation-configuration:automation-configuration-graphql:test` (GraphQL controller tests). Expected: PASS.
- [ ] **Step 5:** Commit: `7398 Move the credentials replace checks to ConnectionFacade and reject non-automation connections in the automation mutation`.

### Task 4: Embedded admin Connections page replace credentials (server)

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-rest/embedded-configuration-rest-impl/openapi.yaml`
- Modify: `ConnectionAdminFacade.java`, `ConnectionAdminFacadeImpl.java`, `embedded-configuration-service/.../audit/ConnectionAuditEvents.java`, `embedded-configuration-rest-impl/.../web/rest/ConnectionApiController.java`
- Test: `ConnectionAdminFacadeTest` (Task 2), `ConnectionApiControllerIntTest` in embedded-configuration-rest-impl (extend or create, WebMvc)

**Interfaces:**
- Consumes: `ConnectionFacade.updateCredentials` (Task 3), `requireAdminConnection` (Task 2).
- Produces: `ConnectionAuditEvents.CONNECTION_CREDENTIALS_UPDATED = "CONNECTION_CREDENTIALS_UPDATED"`; `ConnectionAdminFacade.updateConnectionCredentials(long id, Map<String, ?> parameters, int version)` with `@Audited(event = CONNECTION_CREDENTIALS_UPDATED, data = {@AuditData(key = "connectionId", value = "#id")})`; REST `PUT /connections/{id}/credentials`, operationId `updateConnectionCredentials`, body schema `UpdateConnectionCredentialsRequest {parameters: object (additionalProperties: true), version: integer}` (both required), 204.

- [ ] **Step 1:** Edit `openapi.yaml`, run `./gradlew -p server/ee/libs/embedded/embedded-configuration/embedded-configuration-rest/embedded-configuration-rest-impl generateOpenAPI`. Commit `openapi.yaml` alone: `7398 Add the embedded admin update connection credentials endpoint to the OpenAPI spec`. Commit generated server + client output: `7398 Generated by generateOpenAPI` (server) and `7398 client - Generated by generateOpenAPI` (client).
- [ ] **Step 2: Failing tests:** facade `testUpdateConnectionCredentialsDelegatesForAdminConnection`, `testUpdateConnectionCredentialsThrowsForConnectedUserConnection`; controller IntTest `testUpdateConnectionCredentials` (204, facade called with parameters and version) and `testUpdateConnectionCredentialsForbiddenForNonAdmin` if the module's IntTests exercise security.
- [ ] **Step 3:** Run. Expected: FAIL.
- [ ] **Step 4:** Implement facade method (`requireAdminConnection` then `connectionFacade.updateCredentials`) and controller method.
- [ ] **Step 5:** Run. Expected: PASS.
- [ ] **Step 6:** Commit: `7398 Let tenant admins replace the credentials of embedded admin connections`.

### Task 5: Record `provisioned_by` and expose `provisioned`

**Files:**
- Modify: `embedded-configuration-service/.../facade/ConnectedUserIntegrationFacadeImpl.java` (`provisionIntegrationInstance`), `IntegrationInstanceAdminFacadeImpl.java`, the `ConnectedUserIntegrationFacade` interface, `IntegrationInstanceService`(+Impl) `create`
- Modify: `ConnectedUserConnectionFacade` (+Impl), `ConnectedUserConnectionService`(+Impl) `create`, admin `ConnectionApiController.createConnectedUserConnection`
- Modify: `server/ee/libs/embedded/embedded-connected-user/embedded-connected-user-rest/openapi.yaml` (`ConnectedUserIntegrationInstance.provisioned`, boolean, readOnly) and the MapStruct mapper that produces `ConnectedUserIntegrationInstanceModel` (`grep -rl ConnectedUserIntegrationInstanceModel server/ee --include=*Mapper*.java`)
- Test: `ConnectedUserIntegrationFacadeTest` / existing provisioning IntTest, `ConnectedUserConnectionFacadeTest`, connected user controller IntTest

**Interfaces:**
- Produces: `IntegrationInstanceService.create(long connectedUserId, long connectionId, long integrationInstanceConfigurationId, @Nullable String provisionedBy)` (old 3-arg overload delegates with `null`); `ConnectedUserIntegrationFacade.provisionIntegrationInstance(..., @Nullable String provisionedBy)` as the new last parameter (update every caller found by grep); `ConnectedUserConnectionService.create(long connectedUserId, long connectionId, @Nullable String provisionedBy)`; `ConnectedUserConnectionFacade.createConnectedUserConnectionAsAdmin(long connectedUserId, ConnectionDTO connectionDTO): long` with `@PreAuthorize("isTenantAdmin()")` and the same `@Audited(CONNECTION_CREATED, ...)` as `createConnectedUserConnection`, recording `SecurityUtils.getCurrentUserLogin()`; `ConnectedUserIntegrationInstanceModel.provisioned: Boolean` = `provisionedBy != null`.

- [ ] **Step 1:** connected-user `openapi.yaml` edit → `generateOpenAPI` for that module → commit yaml alone, then generated server/client output (same subjects pattern as Task 4).
- [ ] **Step 2: Failing tests:** `testProvisionIntegrationInstanceRecordsProvisioningAdmin` (instance created with the admin's login), `testSelfActivatedIntegrationInstanceHasNoProvisionedBy`, `testCreateConnectedUserConnectionAsAdminRecordsAdmin`, `testCreateConnectedUserConnectionLeavesProvisionedByEmpty`, mapper/controller `provisioned` true/false.
- [ ] **Step 3:** Run. Expected: FAIL.
- [ ] **Step 4:** Implement. `IntegrationInstanceAdminFacadeImpl.provisionIntegrationInstance` passes `SecurityUtils.getCurrentUserLogin()`; the admin `ConnectionApiController.createConnectedUserConnection` calls `createConnectedUserConnectionAsAdmin`; the public controller keeps `createConnectedUserConnection`.
- [ ] **Step 5:** Run. Expected: PASS.
- [ ] **Step 6:** Commit: `7398 Record which admin provisioned an integration instance or created a connected user connection`.

### Task 6: Admin workflow builder lists user-entered connections without credentials

**Files:**
- Modify: `ConnectedUserConnectionFacade` (+Impl), admin `ConnectionApiController.getConnectedUserConnections`; `ConnectionDTO` (platform-connection-api) only if a copy helper is needed
- Test: `ConnectedUserConnectionFacadeTest`

**Interfaces:**
- Consumes: `provisionedBy` on both domains (Task 1/5).
- Produces: `ConnectedUserConnectionFacade.getConnectedUserConnectionsForAdmin(long connectedUserId, @Nullable String componentName, List<Long> connectionIds): List<ConnectionDTO>`, `@PreAuthorize("isTenantAdmin()")`: same list as `getConnectedUserConnections`, but a connection not entered by an admin has `authorizationParameters` and `parameters` null. Admin-entered = its `connected_user_connection.provisioned_by` is set, or it is the connection of an integration instance whose `provisioned_by` is set.

- [ ] **Step 1: Failing tests:** `testGetConnectedUserConnectionsForAdminKeepsUserConnectionsWithoutCredentials` (user-created connection present, `authorizationParameters()` null), `testGetConnectedUserConnectionsForAdminKeepsAdminEnteredCredentials` (both admin-created and provisioned-instance cases keep parameters).
- [ ] **Step 2:** Run. Expected: FAIL.
- [ ] **Step 3:** Implement; switch the admin controller to the new method; confirm `ConnectionApiController`'s model mapping tolerates null `authorizationParameters` (obfuscation step must skip null).
- [ ] **Step 4:** Run. Expected: PASS.
- [ ] **Step 5:** Commit: `7398 Leave connected users' own credentials out of the admin workflow builder connection list`.

### Task 7: Provisioned instance credentials endpoints (server)

**Files:**
- Modify: `embedded-configuration-rest-impl/openapi.yaml`; `IntegrationInstanceAdminFacade` (+Impl); `ConnectedUserConnectionFacade` (+Impl); `IntegrationAuditEvents.java`; admin `IntegrationInstanceApiController.java` (`embedded-configuration-rest-impl/.../web/rest/`)
- Test: `IntegrationInstanceAdminFacadeTest`, `ConnectedUserConnectionFacadeTest`, `IntegrationInstanceApiControllerIntTest`

**Interfaces:**
- Consumes: `ConnectionFacade.updateCredentials` (Task 3); `withIntegrationCredentials` (existing private method in `ConnectedUserConnectionFacadeImpl`).
- Produces:
  - `IntegrationAuditEvents.INTEGRATION_INSTANCE_CREDENTIALS_UPDATED = "INTEGRATION_INSTANCE_CREDENTIALS_UPDATED"`.
  - `ConnectedUserConnectionFacade.updateIntegrationInstanceConnectionCredentials(long connectedUserId, long connectionId, Map<String, ?> parameters, int version)`, `@PreAuthorize("isTenantAdmin()")`: `requireOwned`, merge with `withIntegrationCredentials`, then `connectionFacade.updateCredentials(connectionId, merged, version)`.
  - `IntegrationInstanceAdminFacade.getIntegrationInstanceConnection(long integrationInstanceId): ConnectionDTO` and `updateIntegrationInstanceCredentials(long integrationInstanceId, Map<String, ?> parameters, int version)` with `@Audited(event = INTEGRATION_INSTANCE_CREDENTIALS_UPDATED, data = {integrationInstanceId = #integrationInstanceId})` plus `connectionId` and `connectedUserId` (resolve them before the call so the audit data can reference them; if `@AuditData` cannot reference locals, record them with the existing audit API used elsewhere in the module). Both throw `NoSuchElementException("Integration instance id=%s not found")` when the instance does not exist or `provisionedBy` is null.
  - REST: `GET /integration-instances/{id}/connection` (operationId `getIntegrationInstanceConnection`, response `Connection`, obfuscated exactly as `ConnectionApiController.getConnection`), `PUT /integration-instances/{id}/connection/credentials` (operationId `updateIntegrationInstanceCredentials`, body `UpdateConnectionCredentialsRequest` from Task 4, 204).

- [ ] **Step 1:** `openapi.yaml` edit → `generateOpenAPI` → commits (yaml alone; generated output).
- [ ] **Step 2: Failing tests:** `testGetIntegrationInstanceConnectionReturnsProvisionedConnection`, `testGetIntegrationInstanceConnectionThrowsForSelfActivatedInstance`, `testUpdateIntegrationInstanceCredentialsThrowsForSelfActivatedInstance`, `testUpdateIntegrationInstanceCredentialsKeepsConfigurationParameters` (configuration `connectionParameters` `{clientId: "app-id", clientSecret: "app-secret"}`, authorization properties `clientId, clientSecret, token`, submitted `{token: "new"}` → `updateCredentials` receives all three), controller IntTests: GET obfuscates (`authorizationParameters.token` ≠ raw value, `parameters` absent), PUT 204, unknown/self-activated id → 404.
- [ ] **Step 3:** Run. Expected: FAIL.
- [ ] **Step 4:** Implement.
- [ ] **Step 5:** Run. Expected: PASS.
- [ ] **Step 6:** Commit: `7398 Let tenant admins view and replace the credentials of integration instances they provisioned`.

### Task 8: Embedded Connections page Update credentials (client)

**Files:**
- Modify: `client/src/ee/shared/mutations/embedded/connections.mutations.ts`, `client/src/ee/pages/embedded/connections/components/connection-list/ConnectionListItem.tsx`
- Test: `client/src/ee/pages/embedded/connections/components/connection-list/tests/ConnectionListItem.test.tsx`

**Interfaces:**
- Consumes: generated `ConnectionApi.updateConnectionCredentials({id, updateConnectionCredentialsRequest: {parameters, version}})` (Task 4).
- Produces: `useUpdateConnectionCredentialsMutation(mutationProps)` with the `ConnectionDialog` contract `(props: {onSuccess?, onError?}) => UseMutationResult<void, Error, ConnectionI>`; maps `ConnectionI.authorizationParameters`/`version` to the request.

- [ ] **Step 1: Failing test:** `passes the credentials mutation to the edit dialog` (mock `ConnectionDialog`, assert the `useUpdateConnectionCredentialsMutation` prop is defined). Admins are the only users of this page, so no ownership condition (unlike automation's `canReplaceCredentials`).
- [ ] **Step 2:** `npx vitest run <test>`. Expected: FAIL.
- [ ] **Step 3:** Implement; invalidate `ConnectionKeys.connections` on success.
- [ ] **Step 4:** Run. Expected: PASS.
- [ ] **Step 5:** Commit: `7398 client - Add Update credentials to the embedded Connections page`.

### Task 9: Credentials dialog for provisioned integrations (client)

**Files:**
- Create: `client/src/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserIntegrationCredentialsDialog.tsx`
- Modify: `ConnectedUserSheetPanelIntegrationListItem.tsx` (same directory), `client/src/ee/shared/queries/embedded/integrationInstances.queries.ts`, `client/src/ee/shared/mutations/embedded/integrationInstances.mutations.ts`
- Test: `.../connected-user-sheet/tests/ConnectedUserIntegrationCredentialsDialog.test.tsx`, `.../tests/ConnectedUserSheetPanelIntegrationListItem.test.tsx`

**Interfaces:**
- Consumes: generated `IntegrationInstanceApi.getIntegrationInstanceConnection` / `updateIntegrationInstanceCredentials` (Task 7); `ConnectedUserIntegrationInstance.provisioned` (Task 5); `ProvisionIntegrationDialogConnectionStep` props `{componentName, componentVersion, configuration, control, formState, onCodeSuccess}`; `useGetProvisionableIntegrationsQuery(connectedUserId)`; `ConnectionParameters`.
- Produces: `useGetIntegrationInstanceConnectionQuery(integrationInstanceId, enabled)`, `IntegrationInstanceKeys.integrationInstanceConnection(id)`, `useUpdateIntegrationInstanceCredentialsMutation({onSuccess})`; `ConnectedUserIntegrationCredentialsDialogProps {connectedUserId: number; integrationInstance: ConnectedUserIntegrationInstance; onClose: () => void}`.

- [ ] **Step 1: Failing tests:** list item: `shows the Credentials action only for provisioned integrations`; dialog: `shows the provisioned connection parameters read-only`, `replaces the credentials with the entered values and the connection version`, `is view-only when the integration configuration is no longer provisionable` (explanation text "This integration's configuration is disabled, so its credentials cannot be replaced."), `uses the configuration's OAuth2 app for OAuth2 integrations` (configuration from the provisionable query passed to `ProvisionIntegrationDialogConnectionStep`).
- [ ] **Step 2:** Run. Expected: FAIL.
- [ ] **Step 3:** Implement. Dialog: title "Credentials", `ConnectionParameters` from the GET, **Update credentials** button switches to `ProvisionIntegrationDialogConnectionStep`, **Save** submits `{parameters: form values (connection step's authorization fields), version: connection.version}`; OAuth2 code success fills parameters as the provisioning dialog does. On success invalidate `ConnectedUserKeys.connectedUser(connectedUserId)`, `IntegrationInstanceKeys.integrationInstance(id)`, `IntegrationInstanceKeys.integrationInstanceConnection(id)`. Row action: a ghost icon button with a `KeyRoundIcon`, tooltip "Credentials", before the existing menu.
- [ ] **Step 4:** Run. Expected: PASS.
- [ ] **Step 5:** Commit: `7398 client - Let admins view and replace the credentials of provisioned integrations`.

### Task 10: Docs

**Files:**
- Modify: `docs/content/docs/platform/embedded/build/connections.md`, `docs/content/docs/platform/embedded/monitor/connected-users.md`, `docs/content/docs/platform/settings/connections.mdx`

- [ ] **Step 1:** Write the content listed in spec decision 9, inside "Coming soon ... not yet available in the latest released version" callouts. Name the audit event `INTEGRATION_INSTANCE_CREDENTIALS_UPDATED`. No em dashes.
- [ ] **Step 2:** Commit: `7398 docs - Describe admin-managed credentials for embedded connections and provisioned integrations`.

### Task 11: Verification and PR

- [ ] **Step 1:** `./gradlew spotlessApply`, then `./gradlew :<each touched module>:check` (embedded-configuration-api/-service/-rest-impl, embedded-connected-user-*, platform-connection-*, automation-configuration-*). Expected: BUILD SUCCESSFUL. Run `./gradlew --stop` and retry once on an unexplained daemon failure.
- [ ] **Step 2:** `./gradlew :<embedded-configuration-service>:testIntegration` and `./gradlew -p server/apps/server-app testIntegration` (boot with the new changelog). Expected: PASS. Testcontainers needs `DOCKER_HOST=unix:///Volumes/Data/Users/ivicac2/.orbstack/run/docker.sock`.
- [ ] **Step 3:** `cd client && npm run check`. Expected: lint, typecheck, tests pass.
- [ ] **Step 4:** Live check on a separate server port and fresh database: a connected user's self-created connection is absent from `/embedded/connections`; an admin provisions an API-key integration, opens Credentials, sees obfuscated values, replaces the key, the instance still runs.
- [ ] **Step 5:** Push `7398` to `upstream`, open the PR against `master` on `bytechefhq/bytechef` (`Fixes #7398`), covering the connection dialog fixes, the design, and this feature.
