# Connected-User-Scoped Connections — Design Spec

- **Date:** 2026-06-24
- **Branch:** 0_732
- **Author:** Ivica Cardic
- **Status:** Implemented
- **Module:** `server/ee/libs/embedded/embedded-configuration` (EE)

## Problem

Embedded connected-user connections are currently stored per **workflow**:

```
ConnectedUser → ConnectedUserProject → ConnectedUserProjectWorkflow → ConnectedUserProjectWorkflowConnection
```

`ConnectedUserProjectWorkflowConnection` (table `connected_user_project_workflow_connection`) is a
`@MappedCollection` child of `ConnectedUserProjectWorkflow`, keyed by
`connected_user_project_workflow_id`.

This scoping is **already vestigial**:

- The sole read path, `ConnectedUserConnectionFacadeImpl.getConnections(...)`, loads the connected
  user's single project, loads **all** its workflows, flattens **every** workflow's connections into
  one list, and filters by `componentName`. The per-workflow grouping is collected and immediately
  discarded.
- `ConnectedUserProjectWorkflowDTO` never exposes connections, so no client ever sees the
  workflow→connection association.
- `ConnectedUserProjectWorkflow.getConnectionsCount()` has zero callers.
- `ConnectedUserProjectWorkflowService.isConnectionUsed(connectionId)` has no production caller (the
  connection-delete guard in `ConnectionFacadeImpl` routes through
  `IntegrationInstanceConfigurationWorkflowService`, not the connected-user-workflow service).

Conceptually, an embedded connected user's connection is **their credential** (their Google/Slack
auth), not a property of a particular workflow. The workflow grouping is incidental and the read side
already treats connections as user-scoped.

### Existing footgun

`ConnectedUserProjectFacadeImpl.deleteProjectWorkflow(...)` hard-deletes a workflow's connections
(`connectionService.delete`) **without** checking whether sibling workflows reference the same
connection. In the current shared-read model this can delete a connection another workflow still
relies on.

## Goal

Re-anchor connections on the **connected user** so storage matches how they are already read, and
remove the now-redundant per-workflow table.

```
ConnectedUser → ConnectedUserConnection   (table: connected_user_connection)
```

## Decisions (locked during brainstorming)

1. **Anchor on `ConnectedUser`**, not `ConnectedUserProject`. The facade already has
   `connectedUserId` in hand, and a user's connections should not fragment if a connected user ever
   spans more than one project. Modeled as a **standalone aggregate** with its own surrogate `id` +
   repository in `embedded-configuration` (a cross-module `@MappedCollection` onto `ConnectedUser`,
   which lives in `embedded-connected-user`, is not appropriate). This mirrors how
   `ConnectedUserProjectWorkflow` already exists as its own aggregate.
2. **Remove `connected_user_project_workflow_connection`** and migrate its rows into
   `connected_user_connection`.
3. **Connection lifecycle:** connections **persist on workflow delete**. There is no per-workflow
   reference left to compute "is this connection needed elsewhere," and the workflow no longer owns
   the connection. This is consistent with the rest of the chain, which already has no FK
   `ON DELETE CASCADE` and no cascade in `deleteConnectedUser`. Individual connections remain
   deletable via the generic `DELETE /connections/{id}`.
4. **Clean the writer API surface:** rename
   `POST /connected-users/{connectedUserId}/workflows/{workflowUuid}/connections` →
   `POST /connected-users/{connectedUserId}/connections`, dropping the now-meaningless
   `workflowUuid`. Facade/operation renamed `createConnectedUserProjectWorkflowConnection` →
   `createConnectedUserConnection`.

## Design

### New domain entity

`com.bytechef.ee.embedded.configuration.domain.ConnectedUserConnection` (in
`embedded-configuration-api`):

- `@Table("connected_user_connection")`
- `@Id Long id` (surrogate PK — required for Spring Data JDBC CRUD on a standalone aggregate)
- `@Column("connected_user_id") AggregateReference<ConnectedUser, Long> connectedUserId`
- `@Column("connection_id") AggregateReference<Connection, Long> connectionId`
- `@version ee` Javadoc + ByteChef Enterprise license header.
- MapStruct `@Default` constructor pattern as in the current code if needed.

### Removed / changed entities

- **Delete** `ConnectedUserProjectWorkflowConnection.java`.
- **`ConnectedUserProjectWorkflow`:** remove the `connections` `@MappedCollection` field plus
  `addConnection`, `getConnections`, `getConnectionsCount`, `setConnections`, and the `connections`
  entry in `toString`. The entity remains (it still links project ↔ workflow with `workflowVersion`).

### Repository

New `ConnectedUserConnectionRepository extends ListCrudRepository<ConnectedUserConnection, Long>` in
`embedded-configuration-service`:

- `List<ConnectedUserConnection> findAllByConnectedUserId(long connectedUserId)`
- (optional) `boolean existsByConnectionId(long connectionId)` — only if a future
  connection-used guard wants it; otherwise omit (current `isConnectionUsed` is dead).

Remove `findConnectedUserProjectWorkflowConnectionIdsByConnectionId` from
`ConnectedUserProjectWorkflowRepository`.

### Service

New `ConnectedUserConnectionService` (interface in `-api`, impl in `-service`):

- `void create(long connectedUserId, long connectionId)`
- `List<Long> getConnectionIds(long connectedUserId)`

From `ConnectedUserProjectWorkflowService` remove `addConnection(...)` and the dead
`isConnectionUsed(...)` (and its impl). Keep `getConnectedUserProjectWorkflows`,
`fetchConnectedUserProjectWorkflow`, CRUD, etc.

### Facade

`ConnectedUserConnectionFacade` / `ConnectedUserConnectionFacadeImpl`:

- `createConnectedUserConnection(long connectedUserId, ConnectionDTO connectionDTO)`:
  ```java
  long connectionId = connectionFacade.create(connectionDTO, PlatformType.EMBEDDED);
  connectedUserConnectionService.create(connectedUserId, connectionId);
  return connectionId;
  ```
  Drops the `ConnectedUserProjectService`, `ConnectedUserProjectWorkflowService`,
  `ProjectWorkflowService` dependencies from the write path.
- `getConnections(Long connectedUserId, String componentName, List<Long> connectionIds)`:
  ```java
  ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserId);
  allConnectionIds.addAll(integrationInstanceService
      .getIntegrationInstances(connectedUser.getId(), componentName, connectedUser.getEnvironment())
      .stream().map(IntegrationInstance::getConnectionId).toList());
  allConnectionIds.addAll(connectedUserConnectionService.getConnectionIds(connectedUser.getId()));
  // unchanged: connectionIds param, connectionFacade.getConnections, componentName filter
  ```
  Drops the project/workflow traversal entirely.

Update the remote-client mirror `RemoteConnectedUserConnectionFacadeClient` to the renamed method.

### Workflow delete

`ConnectedUserProjectFacadeImpl.deleteProjectWorkflow(externalUserId, workflowUuid, environment)`:
remove the connection-collection gathering and the trailing `connectionService.delete(connectionId)`
loop. Continue deleting the `ConnectedUserProjectWorkflow` rows and the project workflows.

### REST

`embedded-configuration-rest-impl/openapi.yaml`:

- Replace operation `createConnectedUserProjectWorkflowConnection`
  (`POST /connected-users/{connectedUserId}/workflows/{workflowUuid}/connections`) with
  `createConnectedUserConnection` (`POST /connected-users/{connectedUserId}/connections`), body
  `Connection`, returns connection id.
- `getConnectedUserConnections`
  (`GET /connected-users/{connectedUserId}/components/{componentName}/connections`) is unchanged —
  already user-scoped.

Regenerate the REST API module and update `ConnectionApiController` accordingly.

### Client

- `client/src/ee/shared/mutations/embedded/connections.mutations.ts`:
  `getCreateConnectedUserProjectWorkflowConnection(connectedUserId, workflowUuid)` →
  `getCreateConnectedUserConnection(connectedUserId)`; call `createConnectedUserConnection({ connectedUserId, connection })`.
- `client/src/ee/pages/embedded/workflow-builder/WorkflowBuilder.tsx`: drop the `workflowUuid`
  argument at the call site.
- Regenerate the embedded configuration middleware (`ConnectionApi.ts`) via the GraphQL/OpenAPI
  codegen step.
- Run `npm run check` before committing client changes.

### Liquibase

New changelog under
`embedded-configuration-service/.../changelog/embedded/configuration/` (timestamped today):

1. `createTable connected_user_connection` — `id` (bigint, autoIncrement, `startWith` matching the
   module convention, PK), `connected_user_id` (bigint, not null), `connection_id` (bigint, not
   null).
2. Unique index on `(connected_user_id, connection_id)`.
3. FK `connected_user_id → connected_user`. FK `connection_id → connection` inside a
   `contextFilter="mono"` changeSet, matching the existing pattern in
   `20240604183140_embedded_configuration_added_connected_user_project.xml` (the `connection` table
   FK is mono-only).
4. **Backfill** (dedup across workflows):
   ```sql
   INSERT INTO connected_user_connection (connected_user_id, connection_id)
   SELECT DISTINCT cup.connected_user_id, cupwc.connection_id
   FROM connected_user_project_workflow_connection cupwc
   JOIN connected_user_project_workflow cupw ON cupw.id = cupwc.connected_user_project_workflow_id
   JOIN connected_user_project cup ON cup.id = cupw.connected_user_project_id;
   ```
   (`id` filled by autoincrement.)
5. `dropTable connected_user_project_workflow_connection`.

Existing dev DBs may need `docker compose -f server/docker-compose.dev.infra.yml down -v` if Liquibase
checksums drift; note this in the PR.

## Testing

- **Unit:** `ConnectedUserConnectionFacadeImplTest` — `createConnectedUserConnection` (creates
  connection, records the user link) and `getConnections` (merges integration-instance + connected-
  user connection ids, filters by component, honors explicit `connectionIds`). Mock the new service.
- **Integration:** `ConnectedUserConnectionServiceIntTest` (or repository IntTest) — create + read
  by `connectedUserId`, unique-constraint behavior on duplicate `(connected_user_id, connection_id)`.
- Adjust/remove tests that referenced `ConnectedUserProjectWorkflow` connections,
  `addConnection`, or `isConnectionUsed` on the connected-user-workflow service.
- Server gate: `./gradlew spotlessApply && ./gradlew check` (full `check` catches app-context
  load regressions in EE apps using the remote-client).

## Out of scope

- The pre-existing absence of cascade in `deleteConnectedUser` (orphaned `connected_user_project*`
  rows). The new table follows the same lifecycle as the surrounding tables; a broader cascade
  cleanup is a separate concern.
- No change to integration-instance connections or the generic `/connections` CRUD endpoints.

## Affected files (inventory)

**Server (EE):**
- `embedded-configuration-api/.../domain/ConnectedUserConnection.java` (new)
- `embedded-configuration-api/.../domain/ConnectedUserProjectWorkflowConnection.java` (delete)
- `embedded-configuration-api/.../domain/ConnectedUserProjectWorkflow.java` (remove connections)
- `embedded-configuration-api/.../service/ConnectedUserConnectionService.java` (new)
- `embedded-configuration-api/.../service/ConnectedUserProjectWorkflowService.java` (remove addConnection/isConnectionUsed)
- `embedded-configuration-api/.../facade/ConnectedUserConnectionFacade.java` (rename method)
- `embedded-configuration-service/.../service/ConnectedUserConnectionServiceImpl.java` (new)
- `embedded-configuration-service/.../service/ConnectedUserProjectWorkflowServiceImpl.java` (remove methods)
- `embedded-configuration-service/.../repository/ConnectedUserConnectionRepository.java` (new)
- `embedded-configuration-service/.../repository/ConnectedUserProjectWorkflowRepository.java` (remove query)
- `embedded-configuration-service/.../facade/ConnectedUserConnectionFacadeImpl.java` (rewrite create + read)
- `embedded-configuration-service/.../facade/ConnectedUserProjectFacadeImpl.java` (deleteProjectWorkflow cleanup removal)
- `embedded-configuration-remote-client/.../facade/RemoteConnectedUserConnectionFacadeClient.java` (rename)
- `embedded-configuration-service/.../resources/config/liquibase/changelog/embedded/configuration/<ts>_*.xml` (new)
- `embedded-configuration-rest-impl/openapi.yaml` (rename operation/path) + regenerated REST API
- `embedded-configuration-rest-impl/.../web/rest/ConnectionApiController.java` (update)

**Client:**
- `client/src/ee/shared/mutations/embedded/connections.mutations.ts`
- `client/src/ee/pages/embedded/workflow-builder/WorkflowBuilder.tsx`
- regenerated `client/src/ee/shared/middleware/embedded/configuration/apis/ConnectionApi.ts`
