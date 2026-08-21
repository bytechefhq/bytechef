# Connection Credential Update Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the owner (or an admin) of an existing ByteChef connection submit new credentials from `ConnectionDialog` — write-only, never prefilled — so a connection whose secret stopped working is reused rather than recreated.

**Architecture:** A new platform-level `replaceConnectionParameters` service method (wholesale write, unlike the existing merge) is wrapped by a new `ConnectionFacade.replaceAuthorizationParameters` that subtracts the connection definition's authorization property names from the stored map, applies the submitted ones, re-runs the OAuth2 code exchange when present, and resets `credentialStatus` to `VALID`. A new owner-or-admin `WorkspaceConnectionFacade.updateConnectionCredentials` exposes it, reached by a new GraphQL mutation. On the client, `ConnectionDialog` gains an `isUpdatingCredentials` state that re-opens the create path's credential fields for an existing connection and routes submission to the new mutation.

**Tech Stack:** Java 25 / Spring Boot 4, Spring Data JDBC, Spring GraphQL, JUnit 5 + Mockito + AssertJ; React 19 + TypeScript, react-hook-form, TanStack Query, GraphQL Codegen, Vitest.

**Spec:** `docs/superpowers/specs/2026-08-20-connection-credential-update-design.md`

## Global Constraints

- **Server commit messages:** `<ticket_number> <description>`. **Client commit messages:** `<ticket_number> client - <description>`. This work has no ticket number assigned; use `4750` (the connection/visibility ticket this branch already uses) unless told otherwise.
- **Branch:** `0_732`. Never `git commit --amend` — the user commits to this branch in parallel. Always fresh commits.
- **Stage only files this task modified.** The working tree has unrelated in-flight changes; never `git add -A` or `git add .`.
- **Before any server commit:** `./gradlew spotlessApply`.
- **Before any client commit:** `cd client && npm run check`.
- **Never judge a Gradle run piped into `tail`/`grep`.** Redirect to a file, check `$?` on its own line, then grep the file for `^> Task .* FAILED`.
- **Java style (enforced by review, some by Checkstyle):** one blank line before control statements (`if`, `for`, `try`, …) except at the start of a block; one blank line between a variable modification and the next statement that uses it; no blank line before a class's closing brace; no `_` prefix on private methods; descriptive variable names (`connection`, not `c`); test method names are camelCase with no underscores (`testUpdateCredentialsReplaces`, never `testUpdateCredentials_Replaces`).
- **Client style (enforced by ESLint):** object keys in ascending alphabetical order (`sort-keys` — `--fix` does NOT fix these, do it by hand); named imports sorted alphabetically inside `{}`; interface names end in `I` or `Props`; `useRef` variables end in `Ref`; use `twMerge`, never `cn()`; lucide icons imported with the `Icon` suffix (`KeyRoundIcon`).
- **Enum values are persisted as INT ordinals.** Append new enum constants at the END, never in the middle.

---

## File Structure

**Server — created**

| File | Responsibility |
|---|---|
| `.../platform/connection/facade/ConnectionCredentialReplacementTest.java` | Unit tests for the replace semantics + `VALID` reset |

**Server — modified**

| File | Responsibility |
|---|---|
| `platform-connection-api/.../service/ConnectionService.java` | Declare `replaceConnectionParameters` |
| `platform-connection-service/.../service/ConnectionServiceImpl.java` | Wholesale secret write (no merge) |
| `platform-connection-api/.../facade/ConnectionFacade.java` | Declare `replaceAuthorizationParameters` |
| `platform-connection-service/.../facade/ConnectionFacadeImpl.java` | Key subtraction, OAuth2 exchange reuse, `VALID` reset |
| `platform-connection-remote-client/.../RemoteConnectionFacadeClient.java` | EE distributed stub |
| `automation-configuration-api/.../facade/WorkspaceConnectionFacade.java` | Declare `updateConnectionCredentials` |
| `automation-configuration-service/.../facade/WorkspaceConnectionFacadeImpl.java` | Owner-or-admin guard, version check, status rejection |
| `automation-configuration-remote-client/.../RemoteWorkspaceConnectionFacadeClient.java` | EE distributed stub |
| `ee/.../automation/configuration/facade/WorkspaceConnectionFacadeImpl.java` | `@AuditConnection` override |
| `platform-connection-audit/.../ConnectionAuditEvent.java` | New `CONNECTION_CREDENTIALS_UPDATED` constant |
| `automation-configuration-graphql/.../graphql/connection.graphqls` | Mutation + input |
| `automation-configuration-graphql/.../ConnectionGraphQlController.java` | Mutation mapping |

**Client — created**

| File | Responsibility |
|---|---|
| `client/src/graphql/automation/configuration/updateConnectionCredentials.graphql` | The GraphQL operation |
| `client/src/shared/mutations/automation/…` (existing file, new export) | TanStack wrapper |

**Client — modified**

| File | Responsibility |
|---|---|
| `client/src/pages/platform/workflow-editor/providers/workflowEditorProvider.tsx` | Two new `ConnectionI` fields |
| `client/src/shared/components/connection/ConnectionDialog.tsx` | The credentials mode |
| `client/src/shared/components/connection/ConnectionDialog.test.tsx` | Coverage |
| `client/src/pages/automation/connections/components/connection-list/ConnectionListItem.tsx` | Pass the new mutation |
| `client/src/shared/middleware/graphql.ts` | **Generated — never hand-edit** |

---

## Task 1: Wholesale parameter write in `ConnectionService`

The existing `updateConnectionParameters` merges (`curParameters.putAll(parameters)`). Replace semantics need a sibling that writes the supplied map verbatim, keeping every other guard identical.

**Files:**
- Modify: `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/service/ConnectionService.java`
- Modify: `server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/service/ConnectionServiceImpl.java`
- Test: `server/libs/platform/platform-connection/platform-connection-service/src/test/java/com/bytechef/platform/connection/service/ConnectionServiceTest.java` (create if absent)

**Interfaces:**
- Consumes: nothing.
- Produces: `Connection ConnectionService.replaceConnectionParameters(long connectionId, Map<String, ?> parameters)`.

- [ ] **Step 1: Write the failing test**

Create or append to `ConnectionServiceTest.java`. If creating, use the Apache 2.0 header copied verbatim from `ConnectionServiceImpl.java` (CE file — **not** the EE header).

```java
@Test
void testReplaceConnectionParametersDoesNotMergeStoredValues() {
    Connection connection = new Connection();

    connection.setId(5L);
    connection.setComponentName("mailchimp");
    connection.setCredentialStoreType(CredentialStoreType.DATABASE);
    connection.setCreatedBy("me@localhost.com");

    when(connectionRepository.findById(5L)).thenReturn(Optional.of(connection));
    when(connectionRepository.save(any(Connection.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(credentialStore.getType()).thenReturn(CredentialStoreType.DATABASE);
    when(credentialStore.isReadOnly()).thenReturn(false);
    when(credentialStore.getSecret(connection)).thenReturn(Map.of("apiKey", "old", "region", "us1"));

    connectionService.replaceConnectionParameters(5L, Map.of("apiKey", "new"));

    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.captor();

    verify(credentialStore).storeSecret(eq(connection), captor.capture());

    assertThat(captor.getValue()).isEqualTo(Map.of("apiKey", "new"));
}
```

Authenticate the test principal as `me@localhost.com` the same way the existing tests in this module do — grep for `SecurityUtils` or `@WithMockUser` in `server/libs/platform/platform-connection/platform-connection-service/src/test/` and copy the established pattern. `validateOwnerOrAdmin` compares against `connection.getCreatedBy()`.

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :server:libs:platform:platform-connection:platform-connection-service:test --tests '*ConnectionServiceTest*' > /tmp/t1.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED|cannot find symbol' /tmp/t1.log
```

Expected: FAIL — `cannot find symbol: method replaceConnectionParameters`.

- [ ] **Step 3: Declare the method on the interface**

In `ConnectionService.java`, directly above the existing `updateConnectionParameters` declaration:

```java
    /**
     * Writes {@code parameters} to the connection's credential store <strong>wholesale</strong>, discarding whatever
     * was stored before. This is the deliberate counterpart to {@link #updateConnectionParameters}, which merges: a
     * caller that has already computed the connection's complete final parameter map must not have the previous map
     * merged back underneath it, or a cleared value would silently survive.
     *
     * <p>
     * Every other guard is identical to {@link #updateConnectionParameters} — AI provider connections are rejected,
     * a read-only credential store throws, and only the connection's creator or an admin may call it.
     */
    Connection replaceConnectionParameters(long connectionId, Map<String, ?> parameters);
```

- [ ] **Step 4: Implement it**

In `ConnectionServiceImpl.java`, directly below `updateConnectionParameters`:

```java
    @Override
    public Connection replaceConnectionParameters(long connectionId, Map<String, ?> parameters) {
        rejectIfAiProviderConnection(connectionId);

        Assert.notNull(parameters, "'parameters' must not be null");

        Connection connection = getConnection(connectionId);
        CredentialStore store = getStore(connection.getCredentialStoreType());

        if (store.isReadOnly()) {
            throw new ReadOnlyCredentialStoreException(store.getType());
        }

        validateOwnerOrAdmin(connection);

        store.storeSecret(connection, new HashMap<>(parameters));

        return populateParameters(connectionRepository.save(connection));
    }
```

- [ ] **Step 5: Run the test to verify it passes**

```bash
./gradlew :server:libs:platform:platform-connection:platform-connection-service:test --tests '*ConnectionServiceTest*' > /tmp/t1.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/t1.log
```

Expected: exit=0, no FAILED lines.

- [ ] **Step 6: Format and commit**

```bash
./gradlew spotlessApply > /tmp/spotless.log 2>&1; echo "exit=$?"
git add server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/service/ConnectionService.java \
        server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/service/ConnectionServiceImpl.java \
        server/libs/platform/platform-connection/platform-connection-service/src/test/java/com/bytechef/platform/connection/service/ConnectionServiceTest.java
git commit -m "4750 Add wholesale connection parameter replacement to ConnectionService"
```

---

## Task 2: `replaceAuthorizationParameters` on `ConnectionFacade`

Subtract the authorization property names from the stored map, apply the submitted ones, re-run the OAuth2 code exchange when a `code` is present, and reset `credentialStatus` to `VALID`.

**Files:**
- Modify: `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/facade/ConnectionFacade.java`
- Modify: `server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/facade/ConnectionFacadeImpl.java`
- Modify: `server/ee/libs/platform/platform-connection/platform-connection-remote-client/src/main/java/com/bytechef/ee/platform/connection/remote/client/fasade/RemoteConnectionFacadeClient.java`
- Test: `server/libs/platform/platform-connection/platform-connection-service/src/test/java/com/bytechef/platform/connection/facade/ConnectionFacadeTest.java`

**Interfaces:**
- Consumes: `ConnectionService.replaceConnectionParameters(long, Map<String, ?>)` (Task 1); `ConnectionService.updateConnectionCredentialStatus(long, Connection.CredentialStatus)` (already exists).
- Produces: `void ConnectionFacade.replaceAuthorizationParameters(long id, Map<String, ?> parameters)`.

- [ ] **Step 1: Write the failing tests**

Append to the existing `ConnectionFacadeTest.java`. Match its existing mock field names and `@BeforeEach` setup — read the file first; the tests below assume `connectionService`, `connectionDefinitionService`, `oAuth2Service` and a `facade` under test, which is what the existing `updateAuthorization` tests around line 191 use.

```java
    @Test
    void testReplaceAuthorizationParametersDropsUnsubmittedAuthorizationKeys() {
        Connection connection = new Connection();

        connection.setId(5L);
        connection.setComponentName("mailchimp");
        connection.setConnectionVersion(1);

        connection.setParameters(Map.of("apiKey", "old", "clientSecret", "old-secret", "region", "us1"));

        when(connectionService.getConnection(5L)).thenReturn(connection);
        when(connectionDefinitionService.getConnectionConnectionDefinition("mailchimp", 1))
            .thenReturn(connectionDefinitionWithAuthorizationProperties("apiKey", "clientSecret"));

        facade.replaceAuthorizationParameters(5L, Map.of("apiKey", "new"));

        // region is a connection-level property, so it survives; clientSecret was not resubmitted, so it is gone.
        verify(connectionService).replaceConnectionParameters(5L, Map.of("apiKey", "new", "region", "us1"));
    }

    @Test
    void testReplaceAuthorizationParametersResetsCredentialStatusToValid() {
        Connection connection = new Connection();

        connection.setId(5L);
        connection.setComponentName("mailchimp");
        connection.setConnectionVersion(1);
        connection.setParameters(Map.of("apiKey", "old"));

        when(connectionService.getConnection(5L)).thenReturn(connection);
        when(connectionDefinitionService.getConnectionConnectionDefinition("mailchimp", 1))
            .thenReturn(connectionDefinitionWithAuthorizationProperties("apiKey"));

        facade.replaceAuthorizationParameters(5L, Map.of("apiKey", "new"));

        verify(connectionService).updateConnectionCredentialStatus(5L, Connection.CredentialStatus.VALID);
    }
```

Write the `connectionDefinitionWithAuthorizationProperties(String... names)` helper as a private static method at the bottom of the test class, building a `ConnectionDefinition` whose single authorization carries properties with those names and whose top-level `properties` list is empty apart from `region`. Copy the construction idiom from whatever the file already uses to build definitions; if it builds none yet, mock `ConnectionDefinition` with Mockito and stub `getAuthorizations()` / `getProperties()`.

- [ ] **Step 2: Run to verify failure**

```bash
./gradlew :server:libs:platform:platform-connection:platform-connection-service:test --tests '*ConnectionFacadeTest*' > /tmp/t2.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED|cannot find symbol' /tmp/t2.log
```

Expected: FAIL — `cannot find symbol: method replaceAuthorizationParameters`.

- [ ] **Step 3: Declare it on the interface**

In `ConnectionFacade.java`, immediately after the existing `updateAuthorization` declaration:

```java
    /**
     * Replaces the connection's authorization parameters wholesale and marks its credentials
     * {@link Connection.CredentialStatus#VALID} again.
     *
     * <p>
     * Added as a sibling of the merge-based {@code updateAuthorization} rather than as a flag on it, so that the
     * embedded reconnect keeps compiling and behaving unchanged until its own migration moves it across. That
     * migration then deletes {@code updateAuthorization}: two near-identical methods differing only in
     * merge-versus-replace is a trap, because picking the wrong one leaves a stale credential in place with no
     * failing test to show for it.
     *
     * <p>
     * Only the keys the connection definition declares as authorization properties are replaced — connection-level
     * properties (base URI inputs, region, subdomain) survive untouched, so the caller never has to enumerate them.
     */
    void replaceAuthorizationParameters(long id, Map<String, ?> parameters);
```

- [ ] **Step 4: Implement it**

In `ConnectionFacadeImpl.java`, directly below `updateAuthorization`:

```java
    @Override
    public void replaceAuthorizationParameters(long id, Map<String, ?> parameters) {
        Connection connection = getConnectionWithReplacedAuthorizationParameters(id, parameters);

        resolveOAuth2AuthorizationCode(connection);

        Map<String, ?> updatedParameters = new HashMap<>(connection.getParameters());

        updatedParameters.remove("state");

        connectionService.replaceConnectionParameters(id, updatedParameters);

        // Nothing else in production sets this back to VALID. Without it ComponentDefinitionFacadeImpl keeps
        // blocking execution, and ConnectionAfterSaveEventListener never re-arms the token refresh routine.
        connectionService.updateConnectionCredentialStatus(id, Connection.CredentialStatus.VALID);
    }

    /**
     * Builds the connection's complete post-replacement parameter map: every key the connection definition declares as
     * an authorization property is dropped, then {@code parameters} is applied. Subtracting by declared name is what
     * lets an unsubmitted optional credential actually clear while a connection-level property survives.
     */
    private Connection getConnectionWithReplacedAuthorizationParameters(long id, Map<String, ?> parameters) {
        Connection connection = connectionService.getConnection(id);

        ConnectionDefinition connectionDefinition = connectionDefinitionService.getConnectionConnectionDefinition(
            connection.getComponentName(), connection.getConnectionVersion());

        List<String> authorizationPropertyNames = connectionDefinition.getAuthorizations()
            .stream()
            .flatMap(authorization -> CollectionUtils.stream(authorization.getProperties()))
            .map(BaseProperty::getName)
            .toList();

        Map<String, Object> replacedParameters = new HashMap<>(connection.getParameters());

        authorizationPropertyNames.forEach(replacedParameters::remove);
        replacedParameters.putAll(parameters);

        connection.setParameters(replacedParameters);

        return connection;
    }
```

`CollectionUtils`, `BaseProperty` and `ConnectionDefinition` are already imported in this file — `toConnectionDTO` uses the identical derivation. Add `java.util.List` if not already present.

- [ ] **Step 5: Add the EE remote stub**

In `RemoteConnectionFacadeClient.java`, directly below the existing `updateAuthorization` override:

```java
    @Override
    public void replaceAuthorizationParameters(long id, Map<String, ?> parameters) {
        throw new UnsupportedOperationException();
    }
```

Match the body style of the sibling stubs in that file exactly — if they throw with a message, do the same.

- [ ] **Step 6: Run to verify pass**

```bash
./gradlew :server:libs:platform:platform-connection:platform-connection-service:test --tests '*ConnectionFacadeTest*' > /tmp/t2.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/t2.log
```

Expected: exit=0, no FAILED lines.

- [ ] **Step 7: Compile everything that depends on the interface**

```bash
./gradlew compileJava compileTestJava --continue > /tmp/compile.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/compile.log
```

Expected: exit=0. A failure here means another `ConnectionFacade` implementor needs the new method — add the same stub there.

- [ ] **Step 8: Format and commit**

```bash
./gradlew spotlessApply > /tmp/spotless.log 2>&1; echo "exit=$?"
git add server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/facade/ConnectionFacade.java \
        server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/facade/ConnectionFacadeImpl.java \
        server/libs/platform/platform-connection/platform-connection-service/src/test/java/com/bytechef/platform/connection/facade/ConnectionFacadeTest.java \
        server/ee/libs/platform/platform-connection/platform-connection-remote-client/src/main/java/com/bytechef/ee/platform/connection/remote/client/fasade/RemoteConnectionFacadeClient.java
git commit -m "4750 Replace connection authorization parameters and reset credential status"
```

---

## Task 3: The audit event

**Files:**
- Modify: `server/ee/libs/platform/platform-connection/platform-connection-audit/src/main/java/com/bytechef/ee/platform/connection/audit/ConnectionAuditEvent.java`

**Interfaces:**
- Produces: `ConnectionAuditEvent.CONNECTION_CREDENTIALS_UPDATED`.

- [ ] **Step 1: Add the constant**

Append it **after** the last existing constant in the enum (append-only; see Global Constraints), matching the file's javadoc style:

```java
    /**
     * A connection's authorization parameters were replaced. Payload: no additional keys — {@code connectionId}
     * identifies the row, and no credential material is ever recorded.
     *
     * <p>
     * Marked {@code strictAudit} — replacing a credential changes which external account every workflow using this
     * connection now acts as, which is the same class of event as a reassignment. A missing trail would hide who
     * repointed a shared credential and when.
     */
    CONNECTION_CREDENTIALS_UPDATED(true),
```

Place it before whatever terminal constant/`;` the enum ends with, keeping the file's existing ordering convention. Read the tail of the file first — the last constant seen during design was a workflow-paused event; append after it.

- [ ] **Step 2: Compile**

```bash
./gradlew :server:ee:libs:platform:platform-connection:platform-connection-audit:compileJava > /tmp/t3.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/t3.log
```

Expected: exit=0.

- [ ] **Step 3: Commit**

```bash
./gradlew spotlessApply > /tmp/spotless.log 2>&1; echo "exit=$?"
git add server/ee/libs/platform/platform-connection/platform-connection-audit/src/main/java/com/bytechef/ee/platform/connection/audit/ConnectionAuditEvent.java
git commit -m "4750 Add CONNECTION_CREDENTIALS_UPDATED audit event"
```

---

## Task 4: The guarded workspace facade method

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/facade/WorkspaceConnectionFacade.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/WorkspaceConnectionFacadeImpl.java`
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-remote-client/src/main/java/com/bytechef/ee/automation/configuration/remote/client/facade/RemoteWorkspaceConnectionFacadeClient.java`
- Test: `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/facade/WorkspaceConnectionFacadeAuthorizationTest.java`
- Test: `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/facade/WorkspaceConnectionFacadeTest.java`

**Interfaces:**
- Consumes: `ConnectionFacade.replaceAuthorizationParameters(long, Map<String, ?>)` (Task 2).
- Produces: `void WorkspaceConnectionFacade.updateConnectionCredentials(long connectionId, Map<String, ?> parameters, int version)`.

- [ ] **Step 1: Write the failing authorization test**

`WorkspaceConnectionFacadeAuthorizationTest` currently only asserts the admin-only expression. Add the owner-or-admin assertion alongside it, reusing its `findMethod` helper:

```java
    private static final String OWNER_OR_ADMIN_EXPRESSION =
        "@permissionService.isResourceOwner('Connection', #connectionId) || " +
            "@permissionService.hasResourceRole(#connectionId, 'Connection', 'ADMIN')";

    @Test
    void testUpdateConnectionCredentialsRequiresOwnerOrAdmin() {
        PreAuthorize preAuthorize = findMethod("updateConnectionCredentials").getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as(
                "updateConnectionCredentials must be owner-or-admin. Without it, any member holding CONNECTION_EDIT "
                    + "on a workspace-shared connection could repoint it at an account they control, and every "
                    + "workflow using that connection would silently follow.")
            .isNotNull();

        assertThat(preAuthorize.value())
            .as("updateConnectionCredentials @PreAuthorize expression must be owner-or-admin")
            .isEqualTo(OWNER_OR_ADMIN_EXPRESSION);
    }
```

- [ ] **Step 2: Write the failing behaviour tests**

Append to `WorkspaceConnectionFacadeTest.java`, matching its existing mock setup:

```java
    @Test
    void testUpdateConnectionCredentialsDelegatesToConnectionFacade() {
        Connection connection = new Connection();

        connection.setId(5L);
        connection.setVersion(3);
        connection.setStatus(ConnectionStatus.ACTIVE);

        when(connectionService.getConnection(5L)).thenReturn(connection);

        workspaceConnectionFacade.updateConnectionCredentials(5L, Map.of("apiKey", "new"), 3);

        verify(connectionFacade).replaceAuthorizationParameters(5L, Map.of("apiKey", "new"));
    }

    @Test
    void testUpdateConnectionCredentialsRejectsStaleVersion() {
        Connection connection = new Connection();

        connection.setId(5L);
        connection.setVersion(4);
        connection.setStatus(ConnectionStatus.ACTIVE);

        when(connectionService.getConnection(5L)).thenReturn(connection);

        assertThatThrownBy(() -> workspaceConnectionFacade.updateConnectionCredentials(5L, Map.of("apiKey", "new"), 3))
            .isInstanceOf(ConfigurationException.class);

        verify(connectionFacade, never()).replaceAuthorizationParameters(anyLong(), any());
    }

    @Test
    void testUpdateConnectionCredentialsRejectsConnectionPendingReassignment() {
        Connection connection = new Connection();

        connection.setId(5L);
        connection.setVersion(3);
        connection.setStatus(ConnectionStatus.PENDING_REASSIGNMENT);

        when(connectionService.getConnection(5L)).thenReturn(connection);

        assertThatThrownBy(() -> workspaceConnectionFacade.updateConnectionCredentials(5L, Map.of("apiKey", "new"), 3))
            .isInstanceOf(ConfigurationException.class);

        verify(connectionFacade, never()).replaceAuthorizationParameters(anyLong(), any());
    }
```

- [ ] **Step 3: Run both to verify failure**

```bash
./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests '*WorkspaceConnectionFacade*Test*' > /tmp/t4.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED|cannot find symbol' /tmp/t4.log
```

Expected: FAIL — `cannot find symbol: method updateConnectionCredentials`.

- [ ] **Step 4: Declare it on the interface**

In the CE `WorkspaceConnectionFacade.java`, after `update`:

```java
    void updateConnectionCredentials(long connectionId, Map<String, ?> parameters, int version);
```

Add `import java.util.Map;`.

- [ ] **Step 5: Implement it**

In the CE `WorkspaceConnectionFacadeImpl.java`, directly below `update`:

```java
    @Override
    @PreAuthorize("@permissionService.isResourceOwner('Connection', #connectionId) || " +
        "@permissionService.hasResourceRole(#connectionId, 'Connection', 'ADMIN')")
    public void updateConnectionCredentials(long connectionId, Map<String, ?> parameters, int version) {
        Connection connection = connectionService.getConnection(connectionId);

        if (connection.getStatus() != ConnectionStatus.ACTIVE) {
            throw new ConfigurationException(
                "Credentials cannot be replaced on a connection with status %s".formatted(connection.getStatus()),
                ConnectionErrorType.INVALID_CONNECTION);
        }

        if (connection.getVersion() != version) {
            throw new ConfigurationException(
                "Connection id=%s was modified by someone else; reload and try again".formatted(connectionId),
                ConnectionErrorType.INVALID_CONNECTION);
        }

        connectionFacade.replaceAuthorizationParameters(connectionId, parameters);
    }
```

Add imports for `Connection`, `ConnectionStatus`, `ConfigurationException`, `ConnectionErrorType` and `java.util.Map` as needed — several are already present in this file.

- [ ] **Step 6: Add the EE remote stub**

In `RemoteWorkspaceConnectionFacadeClient.java`, matching the sibling stubs:

```java
    @Override
    public void updateConnectionCredentials(long connectionId, Map<String, ?> parameters, int version) {
        throw new UnsupportedOperationException();
    }
```

- [ ] **Step 7: Run to verify pass**

```bash
./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests '*WorkspaceConnectionFacade*Test*' > /tmp/t4.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/t4.log
```

Expected: exit=0.

- [ ] **Step 8: Format and commit**

```bash
./gradlew spotlessApply > /tmp/spotless.log 2>&1; echo "exit=$?"
git add server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/facade/WorkspaceConnectionFacade.java \
        server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/WorkspaceConnectionFacadeImpl.java \
        server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/facade/WorkspaceConnectionFacadeAuthorizationTest.java \
        server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/facade/WorkspaceConnectionFacadeTest.java \
        server/ee/libs/automation/automation-configuration/automation-configuration-remote-client/src/main/java/com/bytechef/ee/automation/configuration/remote/client/facade/RemoteWorkspaceConnectionFacadeClient.java
git commit -m "4750 Guard credential replacement behind the workspace connection facade"
```

---

## Task 5: EE audit override

**Files:**
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/facade/WorkspaceConnectionFacadeImpl.java`

**Interfaces:**
- Consumes: `WorkspaceConnectionFacade.updateConnectionCredentials(long, Map<String, ?>, int)` (Task 4); `ConnectionAuditEvent.CONNECTION_CREDENTIALS_UPDATED` (Task 3).
- Produces: nothing new.

- [ ] **Step 1: Add the override**

The EE impl extends the CE one and adds audit annotations by overriding and calling `super` — the existing `delete` override is the template. Add, next to the other `@AuditConnection` methods:

```java
    @Override
    @AuditConnection(event = CONNECTION_CREDENTIALS_UPDATED, connectionId = "#connectionId")
    public void updateConnectionCredentials(long connectionId, Map<String, ?> parameters, int version) {
        super.updateConnectionCredentials(connectionId, parameters, version);
    }
```

Add `import static com.bytechef.ee.platform.connection.audit.ConnectionAuditEvent.CONNECTION_CREDENTIALS_UPDATED;` alongside the existing static audit-event imports, and `java.util.Map` if absent.

**Do not re-declare `@PreAuthorize` here.** It is inherited from the CE implementation, exactly as the CE `delete`'s guard is inherited by the EE `delete` override.

- [ ] **Step 2: Compile**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:compileJava > /tmp/t5.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/t5.log
```

Expected: exit=0.

- [ ] **Step 3: Commit**

```bash
./gradlew spotlessApply > /tmp/spotless.log 2>&1; echo "exit=$?"
git add server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/facade/WorkspaceConnectionFacadeImpl.java
git commit -m "4750 Audit connection credential replacement"
```

---

## Task 6: The GraphQL mutation

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-graphql/src/main/resources/graphql/connection.graphqls`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-graphql/src/main/java/com/bytechef/automation/configuration/web/graphql/ConnectionGraphQlController.java`
- Test: `server/libs/automation/automation-configuration/automation-configuration-graphql/src/test/java/com/bytechef/automation/configuration/web/graphql/ConnectionGraphQlControllerIntTest.java`

**Interfaces:**
- Consumes: `WorkspaceConnectionFacade.updateConnectionCredentials(long, Map<String, ?>, int)` (Task 4).
- Produces: GraphQL `updateConnectionCredentials(input: UpdateConnectionCredentialsInput!): Boolean!`.

- [ ] **Step 1: Write the failing test**

Append to `ConnectionGraphQlControllerIntTest.java`, copying the `disconnectConnection` test's structure (it is the nearest sibling and already mocks `workspaceConnectionFacade`):

```java
    @Test
    void testUpdateConnectionCredentials() {
        graphQlTester
            .document(
                """
                    mutation {
                        updateConnectionCredentials(
                            input: {connectionId: 123, parameters: {apiKey: "new"}, version: 3})
                    }
                    """)
            .execute()
            .path("updateConnectionCredentials")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(workspaceConnectionFacade).updateConnectionCredentials(123L, Map.of("apiKey", "new"), 3);
    }
```

- [ ] **Step 2: Run to verify failure**

```bash
./gradlew :server:libs:automation:automation-configuration:automation-configuration-graphql:test --tests '*ConnectionGraphQlControllerIntTest*' > /tmp/t6.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/t6.log
```

Expected: FAIL — unknown field `updateConnectionCredentials`.

- [ ] **Step 3: Extend the schema**

In `connection.graphqls`, inside the existing `extend type Mutation` block, after `registerExistingConnection`:

```graphql
    """Replace an existing connection's authorization parameters. Owner-or-admin only.

    The submitted parameters replace the connection's authorization parameters wholesale — a
    credential the caller does not resubmit is cleared, not kept. Connection-level properties
    (base URI inputs, region, subdomain) are untouched. Succeeding means the values were stored,
    not that they work: there is no test-connection step, so a wrong credential is re-flagged
    INVALID by the token refresh handler on the next execution."""
    updateConnectionCredentials(input: UpdateConnectionCredentialsInput!): Boolean!
```

And at the end of the file, alongside `RegisterExistingConnectionInput`:

```graphql
input UpdateConnectionCredentialsInput {
    connectionId: ID!
    parameters: Map!
    version: Int!
}
```

`Map` is the codebase's registered scalar for free-form key/value payloads. There is no `JSON` scalar — `CreateOrganizationConnectionInput` in `organization-connection.graphqls` already uses `parameters: Map!` for exactly this payload.

- [ ] **Step 4: Add the controller mapping**

In `ConnectionGraphQlController.java`, after `registerExistingConnection`:

```java
    @MutationMapping(name = "updateConnectionCredentials")
    public Boolean updateConnectionCredentials(@Argument UpdateConnectionCredentialsInput input) {
        workspaceConnectionFacade.updateConnectionCredentials(
            input.connectionId(), input.parameters(), input.version());

        return true;
    }
```

And the input record, next to `RegisterExistingConnectionInput`:

```java
    public record UpdateConnectionCredentialsInput(long connectionId, Map<String, Object> parameters, int version) {
    }
```

Add `import java.util.Map;`.

- [ ] **Step 5: Run to verify pass**

```bash
./gradlew :server:libs:automation:automation-configuration:automation-configuration-graphql:test --tests '*ConnectionGraphQlControllerIntTest*' > /tmp/t6.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/t6.log
```

Expected: exit=0.

- [ ] **Step 6: Format and commit**

```bash
./gradlew spotlessApply > /tmp/spotless.log 2>&1; echo "exit=$?"
git add server/libs/automation/automation-configuration/automation-configuration-graphql/src/main/resources/graphql/connection.graphqls \
        server/libs/automation/automation-configuration/automation-configuration-graphql/src/main/java/com/bytechef/automation/configuration/web/graphql/ConnectionGraphQlController.java \
        server/libs/automation/automation-configuration/automation-configuration-graphql/src/test/java/com/bytechef/automation/configuration/web/graphql/ConnectionGraphQlControllerIntTest.java
git commit -m "4750 Expose updateConnectionCredentials GraphQL mutation"
```

---

## Task 7: Client GraphQL operation and mutation hook

**Files:**
- Create: `client/src/graphql/automation/configuration/updateConnectionCredentials.graphql`
- Modify: `client/src/shared/middleware/graphql.ts` (**generated — do not hand-edit**)
- Modify: `client/src/shared/mutations/automation/connections.mutations.ts`

**Interfaces:**
- Consumes: the GraphQL mutation from Task 6.
- Produces: `useUpdateConnectionCredentialsMutation(mutationProps?)` returning `UseMutationResult<void, Error, ConnectionI>`, matching the shape `ConnectionDialog` already expects from `useUpdateConnectionMutation`.

- [ ] **Step 1: Write the operation file**

`client/src/graphql/automation/configuration/updateConnectionCredentials.graphql`:

```graphql
mutation UpdateConnectionCredentials($input: UpdateConnectionCredentialsInput!) {
    updateConnectionCredentials(input: $input)
}
```

- [ ] **Step 2: Regenerate the typed hooks**

```bash
cd client && npx graphql-codegen
```

Expected: `src/shared/middleware/graphql.ts` gains `useUpdateConnectionCredentialsMutation`. If codegen errors with an unknown type, the server schema file from Task 6 is not on `codegen.ts`'s `schema` glob — it should be, since that glob already covers `automation-configuration-graphql/src/main/resources/graphql/*.graphqls`.

- [ ] **Step 3: Add the TanStack wrapper**

Append to `connections.mutations.ts`, following the file's existing `useDisconnectConnectionMutation` shape:

```typescript
interface UpdateConnectionCredentialsMutationPropsI {
    onError?: (error: Error, variables: ConnectionI) => void;
    onSuccess?: (result: void, variables: ConnectionI) => void;
}

/**
 * Submits replacement credentials for an existing connection. Deliberately shaped like
 * `useUpdateConnectionMutation` (same `ConnectionI` variables, same `void` result) so `ConnectionDialog` can
 * accept it through a prop of the same type. Only `id`, `parameters` and `version` are read — the server
 * replaces the connection's authorization parameters wholesale and ignores everything else on the object.
 */
export const useUpdateConnectionCredentialsMutation = (
    mutationProps?: UpdateConnectionCredentialsMutationPropsI
) => {
    const graphqlMutation = useUpdateConnectionCredentialsGraphQL();

    return useMutation<void, Error, ConnectionI>({
        mutationFn: async (connection) => {
            await graphqlMutation.mutateAsync({
                input: {
                    connectionId: String(connection.id!),
                    parameters: connection.parameters,
                    version: connection.version!,
                },
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
};
```

Add `useUpdateConnectionCredentialsMutation as useUpdateConnectionCredentialsGraphQL` to the existing `@/shared/middleware/graphql` import — **keep the named imports alphabetically sorted**, so it goes after `useRegisterExistingConnectionMutation as useRegisterExistingConnectionGraphQL`. Import `ConnectionI` from `@/pages/platform/workflow-editor/providers/workflowEditorProvider`.

- [ ] **Step 4: Typecheck**

```bash
cd client && npm run check
```

Expected: passes.

- [ ] **Step 5: Commit (operations and generated file separately, per the repo's GraphQL workflow)**

```bash
git add client/src/graphql/automation/configuration/updateConnectionCredentials.graphql \
        client/src/shared/mutations/automation/connections.mutations.ts
git commit -m "4750 client - Add updateConnectionCredentials mutation hook"
git add client/src/shared/middleware/graphql.ts
git commit -m "4750 client - Regenerate GraphQL types for updateConnectionCredentials"
```

---

## Task 8: `ConnectionI` gains the two gating fields

`ConnectionDialog` must hide the credentials affordance for connections whose secret is not the platform's to write. `ConnectionI` carries neither field today.

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/providers/workflowEditorProvider.tsx`

**Interfaces:**
- Produces: `ConnectionI.credentialStoreType?: 'AWS_SECRETS_MANAGER' | 'DATABASE' | 'HASHICORP_VAULT'` and `ConnectionI.managed?: boolean`.

- [ ] **Step 1: Add the fields**

In the `ConnectionI` interface. Note this interface is **not** strictly alphabetical — it already has `authorizationType` before `authorizationParameters` — so `sort-keys` is not enforcing member order here. Place `credentialStoreType` immediately after `credentialStatus`, and `managed` after `lastModifiedDate`, matching the file's own grouping:

```typescript
    credentialStoreType?: 'AWS_SECRETS_MANAGER' | 'DATABASE' | 'HASHICORP_VAULT';
```

```typescript
    readonly managed?: boolean;
```

Both are optional so every existing construction site compiles unchanged.

- [ ] **Step 2: Typecheck**

```bash
cd client && npm run check
```

Expected: passes.

- [ ] **Step 3: Commit**

```bash
git add client/src/pages/platform/workflow-editor/providers/workflowEditorProvider.tsx
git commit -m "4750 client - Carry credentialStoreType and managed on ConnectionI"
```

---

## Task 9: The credentials mode in `ConnectionDialog`

The dialog gates its credential fields on `!connection?.id`. Introduce an `isUpdatingCredentials` state that re-opens exactly those fields for an existing connection, leaves the authorization-type selector and connection-property fields closed (both are non-goals), and routes submission to the new mutation.

**Files:**
- Modify: `client/src/shared/components/connection/ConnectionDialog.tsx`
- Modify: `client/src/pages/automation/connections/components/connection-list/ConnectionListItem.tsx`

**Interfaces:**
- Consumes: `useUpdateConnectionCredentialsMutation` (Task 7); `ConnectionI.credentialStoreType` / `.managed` (Task 8).
- Produces: `ConnectionDialogProps.useUpdateConnectionCredentialsMutation?: (mutationProps?: {onError?: (error: Error, variables: ConnectionI) => void; onSuccess?: (result: void, variables: ConnectionI) => void}) => UseMutationResult<void, Error, ConnectionI, unknown>`.

- [ ] **Step 1: Add the prop and the state**

In `ConnectionDialogProps`. Its members are alphabetically ordered, and `useUpdateConnectionC…` sorts before `useUpdateConnectionM…`, so this goes **immediately before** `useUpdateConnectionMutation`:

```typescript
    /**
     * Supplied only by surfaces that allow an existing connection's credentials to be replaced. Absent means the
     * "Update credentials" affordance is not rendered at all, which is how every caller that has not opted in keeps
     * today's rename-only edit dialog.
     */
    useUpdateConnectionCredentialsMutation?: (mutationProps: {
        onSuccess?: (result: void, variables: ConnectionI) => void;
        onError?: (error: Error, variables: ConnectionI) => void;
    }) => UseMutationResult<void, Error, ConnectionI, unknown>;
```

And, sorted before `title`:

```typescript
    /**
     * Opens the dialog directly in credential-replacement mode instead of the rename-only edit body. For surfaces
     * whose entire purpose is reconnecting an account — the embedded hub's Reconnect action — where making the user
     * find an "Update credentials" button first would be pointless.
     */
    startInCredentialsMode?: boolean;
```

Alongside the existing `useState` declarations (keep them grouped — see the hook-ordering rule):

```typescript
    const [isUpdatingCredentials, setIsUpdatingCredentials] = useState(false);
```

- [ ] **Step 2: Derive whether the affordance is offered**

Next to the other derived booleans (near `isEdit`):

```typescript
    // A connection whose secret lives in an external store, or that the platform manages on the user's behalf, is
    // not ours to rewrite: ConnectionServiceImpl throws ReadOnlyCredentialStoreException for the former, and the
    // user would have typed a secret before finding out. Hide rather than fail.
    const credentialsAreExternallyStored =
        (!!connection?.credentialStoreType && connection.credentialStoreType !== 'DATABASE') || !!connection?.managed;

    const canUpdateCredentials = isEdit && !!useUpdateConnectionCredentialsMutation && !credentialsAreExternallyStored;
```

- [ ] **Step 3: Wire the credentials mutation**

Below the existing `connectionMutation` declaration:

```typescript
    const credentialsMutation = useUpdateConnectionCredentialsMutation?.({
        onSuccess: () => {
            toast('Credentials updated', {
                description: 'The new credentials were saved. They are verified the next time the connection runs.',
            });

            handleConnectionSuccess();
        },
    });
```

The description is deliberate: there is no test-connection step, so the toast must not claim the credentials work. See the spec's §8.

- [ ] **Step 4: Seed the authorization type and auto-open for invalid credentials**

The authorization-type selector stays hidden in this mode, so `authorizationType` must be seeded from the stored connection or the credential form renders no fields. Add a `useEffect` with the other effects (they go last, immediately before the `return`):

```typescript
    useEffect(() => {
        if (!isEdit) {
            return;
        }

        if (connection?.authorizationType) {
            setAuthorizationType(connection.authorizationType);
        }

        if (startInCredentialsMode || connection?.credentialStatus === 'INVALID') {
            setIsUpdatingCredentials(true);
        }
    }, [connection?.authorizationType, connection?.credentialStatus, isEdit, startInCredentialsMode]);
```

- [ ] **Step 5: Open the credential fields in this mode**

Change the three gates that currently read `!connection?.id` **only where the spec says the field belongs in this mode**:

- `showOAuth2Step` — from `&& !connection?.id` to `&& (!connection?.id || isUpdatingCredentials)`
- the authorization-properties `Properties` block — from `{!connection?.id && showAuthorizationProperties && …}` to `{(!connection?.id || isUpdatingCredentials) && showAuthorizationProperties && …}`

**Leave these two exactly as they are** — both are explicit non-goals in the spec:
- the connection-properties `Properties` block (non-secret fields stay read-only)
- the authorization-type `Select` (the type is locked to the stored one)

Additionally hide the name, tags and visibility fields while `isUpdatingCredentials` is true, so the mode reads as one job.

- [ ] **Step 6: Route submission**

In `saveConnection`, make the credentials branch the first test — before the existing `connection?.id` branch, since in this mode an id IS present:

```typescript
        if (isUpdatingCredentials && credentialsMutation) {
            const {parameters} = getValues();

            return credentialsMutation.mutateAsync({
                id: connection!.id,
                parameters: {...parameters, ...additionalParameters},
                version: connection!.version,
            } as ConnectionI);
        }
```

- [ ] **Step 7: Add the entry point and reset**

In the edit-mode footer, render a button when `canUpdateCredentials && !isUpdatingCredentials`:

```typescript
                            {canUpdateCredentials && !isUpdatingCredentials && (
                                <Button onClick={() => setIsUpdatingCredentials(true)} size="sm" variant="outline">
                                    <KeyRoundIcon /> Update credentials
                                </Button>
                            )}
```

Import `KeyRoundIcon` from `lucide-react`, keeping that import's names alphabetically sorted. When `isUpdatingCredentials` is true the footer shows a submit button labelled `Update credentials`, and a Back button (`setIsUpdatingCredentials(false)`) — but **render Back only when `!startInCredentialsMode`**. A surface that opens directly in this mode has no rename-only body behind it, so Back would strand the user in a state they never chose to enter.

In `closeDialog`'s `setTimeout` body, alongside the other resets:

```typescript
            setIsUpdatingCredentials(false);
```

- [ ] **Step 8: Show why, when the credentials are known bad**

Above the credential fields while `isUpdatingCredentials && connection?.credentialStatus === 'INVALID'`:

```typescript
                            <Alert variant="destructive">
                                <AlertTitle>These credentials were rejected</AlertTitle>

                                <AlertDescription>
                                    Workflows using this connection are blocked until new credentials are saved.
                                </AlertDescription>
                            </Alert>
```

`Alert`, `AlertTitle` and `AlertDescription` are already imported in this file.

- [ ] **Step 9: Opt the automation connections list in**

In `ConnectionListItem.tsx`, add to the import from `@/shared/mutations/automation/connections.mutations` (alphabetically sorted) and pass it to the dialog:

```typescript
                        useUpdateConnectionCredentialsMutation={useUpdateConnectionCredentialsMutation}
```

Place the prop so the JSX attributes stay alphabetically ordered.

- [ ] **Step 10: Check**

```bash
cd client && npm run check
```

Expected: passes. `sort-keys` and `sort-import-destructures` are not auto-fixable — fix by hand.

- [ ] **Step 11: Commit**

```bash
git add client/src/shared/components/connection/ConnectionDialog.tsx \
        client/src/pages/automation/connections/components/connection-list/ConnectionListItem.tsx
git commit -m "4750 client - Allow replacing credentials on an existing connection"
```

---

## Task 10: Client tests

**Files:**
- Modify: `client/src/shared/components/connection/ConnectionDialog.test.tsx`

**Interfaces:**
- Consumes: everything from Task 9.
- Produces: nothing.

- [ ] **Step 1: Write the tests**

Read the existing file first and reuse its render helper, mock setup and query-client wrapper rather than inventing new ones. Add:

```typescript
    it('never prefills stored credentials when updating them', async () => {
        renderConnectionDialog({
            connection: {
                ...baseConnection,
                authorizationParameters: {apiKey: '••••••••1234'},
                authorizationType: 'API_KEY',
                credentialStoreType: 'DATABASE',
                id: 5,
            },
            useUpdateConnectionCredentialsMutation: mockCredentialsMutationHook,
        });

        await userEvent.click(screen.getByRole('button', {name: /update credentials/i}));

        expect(screen.getByLabelText(/api key/i)).toHaveValue('');
    });

    it('opens straight into the credentials fields when the credentials are invalid', () => {
        renderConnectionDialog({
            connection: {...baseConnection, credentialStatus: 'INVALID', id: 5},
            useUpdateConnectionCredentialsMutation: mockCredentialsMutationHook,
        });

        expect(screen.getByText(/these credentials were rejected/i)).toBeInTheDocument();
    });

    it('does not offer credential updates for externally stored credentials', () => {
        renderConnectionDialog({
            connection: {...baseConnection, credentialStoreType: 'AWS_SECRETS_MANAGER', id: 5},
            useUpdateConnectionCredentialsMutation: mockCredentialsMutationHook,
        });

        expect(screen.queryByRole('button', {name: /update credentials/i})).not.toBeInTheDocument();
    });

    it('does not offer credential updates when the caller has not opted in', () => {
        renderConnectionDialog({connection: {...baseConnection, id: 5}});

        expect(screen.queryByRole('button', {name: /update credentials/i})).not.toBeInTheDocument();
    });
```

Declare `mockCredentialsMutationHook` with `vi.hoisted` if it is referenced from any `vi.mock` factory — module-scope `const`s are not initialised when those factories run (see the repo's Vitest hoisting note).

- [ ] **Step 2: Run**

```bash
cd client && npm run test -- ConnectionDialog
```

Expected: all pass.

- [ ] **Step 3: Full check and commit**

```bash
cd client && npm run check
git add client/src/shared/components/connection/ConnectionDialog.test.tsx
git commit -m "4750 client - Cover credential replacement in ConnectionDialog"
```

---

## Task 11: Amend `CLAUDE.md`

The change makes a documented statement false. Correcting it is part of shipping, not a follow-up.

**Files:**
- Modify: `CLAUDE.md`

**Interfaces:**
- Consumes: nothing. Produces: nothing.

- [ ] **Step 1: Rewrite the sentence**

Find, under **Resource Visibility & Sharing → Connections → "What sharing exposes"**:

> `WORKSPACE` grants *use plus existence*, not *read plus write*: both REST controllers obfuscate `authorizationParameters` and null `parameters`, and no `ConnectionFacade` method mutates authorization parameters after creation. A member can run a workflow against a colleague's account; they cannot extract or repoint the credential.

Replace the second half:

> `WORKSPACE` grants *use plus existence*, not *read plus write*: both REST controllers obfuscate `authorizationParameters` and null `parameters`, so a member can never extract a credential. Credentials *can* be replaced after creation — `ConnectionFacade.replaceAuthorizationParameters`, reached through `WorkspaceConnectionFacade.updateConnectionCredentials` — but only by the connection's **owner or an admin** (`isResourceOwner || hasResourceRole(…, 'ADMIN')`, the sharing-management posture). `CONNECTION_EDIT` alone still cannot repoint a shared connection. A member can run a workflow against a colleague's account; they can neither extract nor repoint the credential.

- [ ] **Step 2: Verify nothing else in the file contradicts it**

```bash
grep -n "mutates authorization parameters\|repoint" CLAUDE.md
```

Expected: only the amended passage.

- [ ] **Step 3: Commit**

```bash
git add CLAUDE.md
git commit -m "4750 Document that connection credentials are replaceable by owner or admin"
```

---

## Task 12: Switch the embedded reconnect to replace semantics

`ConnectedUserConnectionFacadeImpl.reauthorizeConnectedUserConnection` is the **only** production caller of `updateAuthorization`. Moving it to `replaceAuthorizationParameters` gives both surfaces one semantics and makes the hub inherit the `credentialStatus = VALID` reset — which is what fixes the hub's currently-broken reconnect.

This changes the behaviour of a **published** endpoint (`POST /connections/{id}/reauthorize`): a caller posting a partial parameter set will now have the omitted authorization parameters cleared rather than preserved. That is spec decision ⚑7, and the OpenAPI description must say so.

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserConnectionFacadeImpl.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/openapi.yaml`
- Test: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserConnectionFacadeTest.java`

**Interfaces:**
- Consumes: `ConnectionFacade.replaceAuthorizationParameters(long, Map<String, ?>)` (Task 2).
- Produces: nothing new — the facade method signature is unchanged.

- [ ] **Step 1: Update the existing tests**

`ConnectedUserConnectionFacadeTest` currently has two assertions on this path — around lines 165 and 178 — one verifying `updateAuthorization(1L, parameters)` and one verifying it is never called for an unowned connection. Change both to `replaceAuthorizationParameters`:

```java
        verify(connectionFacade).replaceAuthorizationParameters(1L, parameters);
```

```java
        verify(connectionFacade, never()).replaceAuthorizationParameters(anyLong(), any());
```

Change nothing else about those tests — the ownership contract they pin is unaffected.

- [ ] **Step 2: Run to verify failure**

```bash
./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests '*ConnectedUserConnectionFacadeTest*' > /tmp/t12.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/t12.log
```

Expected: FAIL — the facade still calls `updateAuthorization`, so the `replaceAuthorizationParameters` verification finds no invocation.

- [ ] **Step 3: Switch the call**

In `ConnectedUserConnectionFacadeImpl.reauthorizeConnectedUserConnection`, replace the single call:

```java
        connectionFacade.replaceAuthorizationParameters(connectionId, parameters);
```

Update that method's javadoc, if it has one, to state replace semantics.

- [ ] **Step 4: Run to verify pass**

```bash
./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests '*ConnectedUserConnectionFacadeTest*' > /tmp/t12.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/t12.log
```

Expected: exit=0.

- [ ] **Step 5: Document the behaviour change on the public endpoint**

In `openapi.yaml`, find `/connections/{id}/reauthorize` (around line 368) and give the `reauthorizeFrontendConnection` operation a description stating replace semantics. Match the surrounding operations' YAML style:

```yaml
      description: "Replaces the connection's authorization parameters with the supplied\
        \ ones. Parameters that are not supplied are cleared, not preserved: submit\
        \ the connection's complete authorization parameter set. Connection-level\
        \ properties are untouched. A successful call also marks the connection's\
        \ credentials valid again."
```

- [ ] **Step 6: Regenerate — and check what moved**

```bash
./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:generateOpenAPI > /tmp/t12gen.log 2>&1; echo "exit=$?"
git status --short
```

Regeneration writes to BOTH `generated/` (committed Spring interfaces) and `client/src/ee/shared/middleware/embedded/public/` (committed TypeScript client).

**Ruling, decide by inspection:** if the regenerated diff touches only description/comment strings, commit it. If it touches anything else — a generator-version drift producing a wide unrelated diff — run `git checkout --` on the regenerated paths, keep only the `openapi.yaml` change, and say so in your report. Do not commit an unrelated generator churn diff inside this change.

- [ ] **Step 7: Format and commit**

```bash
./gradlew spotlessApply > /tmp/spotless.log 2>&1; echo "exit=$?"
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserConnectionFacadeImpl.java \
        server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserConnectionFacadeTest.java \
        server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/openapi.yaml
# plus the regenerated paths, only if Step 6 ruled them clean
git commit -m "4750 Replace embedded reconnect authorization parameters wholesale"
```

---

## Task 13: Delete `ConnectionFacade.updateAuthorization`

With Task 12 done it has no production callers. Leaving a merge-shaped method beside a replace-shaped one is the trap spec §5 names.

**Files:**
- Modify: `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/facade/ConnectionFacade.java`
- Modify: `server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/facade/ConnectionFacadeImpl.java`
- Modify: `server/ee/libs/platform/platform-connection/platform-connection-remote-client/src/main/java/com/bytechef/ee/platform/connection/remote/client/fasade/RemoteConnectionFacadeClient.java`
- Test: `server/libs/platform/platform-connection/platform-connection-service/src/test/java/com/bytechef/platform/connection/facade/ConnectionFacadeTest.java`

**Interfaces:**
- Consumes: nothing. Produces: nothing — this is a removal.

- [ ] **Step 1: Confirm it is genuinely dead**

```bash
grep -rn "updateAuthorization" --include="*.java" server | grep -v "/bin/\|/build/"
```

Expected: only the interface declaration, the impl, the EE remote-client stub, and tests in `ConnectionFacadeTest`. **If any other production caller appears, stop and report it** — the deletion premise is wrong and the task needs a ruling.

- [ ] **Step 2: Delete the three production sites**

Remove the declaration from `ConnectionFacade.java`, the `updateAuthorization` method from `ConnectionFacadeImpl.java`, and the override from `RemoteConnectionFacadeClient.java`.

**Keep `resolveOAuth2AuthorizationCode`** — `replaceAuthorizationParameters` calls it. Update its javadoc, which currently says it is "Shared by `create` and `updateAuthorization`", to name `replaceAuthorizationParameters` instead.

- [ ] **Step 3: Retire the superseded tests**

`ConnectionFacadeTest` has three `updateAuthorization` tests (around lines 191, 218, 241) covering merge, the OAuth2 code exchange, and `state` stripping. Delete the merge test — that behaviour is gone. **Port the other two to `replaceAuthorizationParameters`**, since the code exchange and `state` stripping both still apply:

```java
    @Test
    void testReplaceAuthorizationParametersRunsOAuth2CodeExchange() {
        // Body ported verbatim from the deleted testUpdateAuthorizationWithCode, with the call changed to
        // facade.replaceAuthorizationParameters(5L, Map.of(Authorization.CODE, "auth-code"));
    }

    @Test
    void testReplaceAuthorizationParametersStripsState() {
        // Body ported verbatim from the deleted state-stripping test, with the call changed to
        // facade.replaceAuthorizationParameters(5L, Map.of("apiKey", "new", "state", "csrf-token"));
    }
```

Read the originals and port their real bodies — adjust each stubbed `connectionService` verification from `updateConnectionParameters` to `replaceConnectionParameters`, and add the connection-definition stub that `replaceAuthorizationParameters` needs (the same helper Task 2 added to this file).

- [ ] **Step 4: Compile and test everything**

```bash
./gradlew compileJava compileTestJava --continue > /tmp/t13c.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/t13c.log
./gradlew :server:libs:platform:platform-connection:platform-connection-service:test > /tmp/t13.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/t13.log
```

Expected: exit=0 for both. A compile failure names a caller Step 1 missed.

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply > /tmp/spotless.log 2>&1; echo "exit=$?"
git add server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/facade/ConnectionFacade.java \
        server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/facade/ConnectionFacadeImpl.java \
        server/libs/platform/platform-connection/platform-connection-service/src/test/java/com/bytechef/platform/connection/facade/ConnectionFacadeTest.java \
        server/ee/libs/platform/platform-connection/platform-connection-remote-client/src/main/java/com/bytechef/ee/platform/connection/remote/client/fasade/RemoteConnectionFacadeClient.java
git commit -m "4750 Remove superseded merge-based updateAuthorization"
```

---

## Task 14: Migrate `HubConnectionDialog` onto the credentials mode

The hub reconnects by passing a `connection` **without** an `id`, because that is the only way the pre-mode dialog would render credential fields. Task 9 made the mode explicit, so the hack can go — and with it two user-visible defects it causes.

**Files:**
- Modify: `client/src/ee/pages/embedded/automation-hub/views/components/HubConnectionDialog.tsx`
- Test: `client/src/ee/pages/embedded/automation-hub/tests/HubConnectionDialog.test.tsx`

**Interfaces:**
- Consumes: `ConnectionDialogProps.useUpdateConnectionCredentialsMutation` and `ConnectionDialogProps.startInCredentialsMode` (Task 9).
- Produces: nothing.

- [ ] **Step 1: Write the failing tests**

Read the existing `HubConnectionDialog.test.tsx` first and reuse its render helper and mocks. Add:

```typescript
    it('does not announce a reconnect as a newly created connection', async () => {
        renderHubConnectionDialog({componentName: 'mailchimp', existingConnectionId: 5});

        await submitReconnect();

        expect(screen.queryByText(/connection created/i)).not.toBeInTheDocument();
    });

    it('opens a reconnect directly in credential entry', () => {
        renderHubConnectionDialog({componentName: 'mailchimp', existingConnectionId: 5});

        expect(screen.queryByRole('button', {name: /update credentials/i})).not.toBeInTheDocument();
    });
```

The second test asserts the dialog is *already* in credential entry, so no "Update credentials" button is offered. Write `submitReconnect()` against whatever the dialog actually renders — as in Task 10, the queries are illustrative and the INTENT is what must hold.

- [ ] **Step 2: Run to verify failure**

```bash
cd client && npm run test -- HubConnectionDialog
```

Expected: the first test fails — the reconnect currently toasts "Connection created".

- [ ] **Step 3: Pass a real connection and the mode**

Replace the id-less prefill with a real one and drop the overrides that only existed to disguise a fake create:

```typescript
            connection={
                existingConnectionId
                    ? {
                          componentName,
                          connectionVersion,
                          id: existingConnectionId,
                          name: componentTitle,
                          parameters: {},
                      }
                    : undefined
            }
```

Delete the `description` and `title` props entirely, delete `useUpdateConnectionMutation`, and add:

```typescript
            startInCredentialsMode={!!existingConnectionId}
            useUpdateConnectionCredentialsMutation={
                existingConnectionId ? useReauthorizeHubConnectionMutation : undefined
            }
```

- [ ] **Step 4: Delete the closure**

`getReauthorizeHubConnectionMutation(existingConnectionId)` exists only because the old payload carried no `id`. It does now, so the hook reads it off its own variables — the same shape Task 7's automation hook uses. Replace the closure factory with a plain hook:

```typescript
const useReauthorizeHubConnectionMutation = (mutationProps?: ReauthorizeConnectionMutationPropsI) =>
    useMutation<void, Error, ConnectionI>({
        mutationFn: (connection) =>
            new ConnectionApi().reauthorizeFrontendConnection({
                id: connection.id!,
                reauthorizeConnectionRequest: {parameters: connection.parameters},
            }),
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
```

Delete the long javadoc above the old factory — it documents a workaround that no longer exists. Replace it with a one-line comment noting the endpoint replaces parameters wholesale.

- [ ] **Step 5: Update the component javadoc**

The `HubConnectionDialog` javadoc describes the id-less hack at length ("deliberately prefills `connection` WITHOUT an `id`…"). Rewrite that paragraph to describe what the code now does: a reconnect passes the real connection and opens in credential-replacement mode. Keep the `existingConnectionVersion` paragraph — that reasoning is still accurate and still load-bearing.

- [ ] **Step 6: Run to verify pass**

```bash
cd client && npm run test -- HubConnectionDialog
cd client && npm run check
```

Expected: both pass.

- [ ] **Step 7: Commit**

```bash
git add client/src/ee/pages/embedded/automation-hub/views/components/HubConnectionDialog.tsx \
        client/src/ee/pages/embedded/automation-hub/tests/HubConnectionDialog.test.tsx
git commit -m "4750 client - Reconnect through the shared credential replacement mode"
```

---

## Task 15: Full verification

**Files:** none modified.

- [ ] **Step 1: Server**

```bash
./gradlew compileJava compileTestJava --continue > /tmp/final-compile.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/final-compile.log
./gradlew check --continue > /tmp/final-check.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/final-check.log
```

Expected: exit=0 for both, no FAILED lines. **Do not read a piped exit code** — the redirect-then-check form above is required. For SpotBugs failures read `build/reports/spotbugs/*test.html`, not the XML (it is disabled and stale).

- [ ] **Step 2: Client**

```bash
cd client && npm run check
```

Expected: passes.

- [ ] **Step 3: Confirm the working tree carries only this work**

```bash
git status --short
git log --oneline -12
```

Expected: the unrelated in-flight `0_732` changes are still unstaged and untouched; the commits above are present.

---

## Self-Review

**Spec coverage**

| Spec section | Task |
|---|---|
| §4 authorization gate | 4 (impl) + 4 Step 1 (annotation pinned) |
| §5 replace semantics | 1 (service), 2 (facade) |
| §5 `VALID` reset | 2 |
| §5 audit event | 3 (constant), 5 (emission) |
| §6 GraphQL surface | 6 |
| §7 third mode, blank fields, OAuth2 leg | 9 |
| §7 hidden for external stores / managed | 8 (fields), 9 Step 2 |
| §7 auto-open on `INVALID` | 9 Step 4 |
| §8 no write-time validation (toast wording) | 9 Step 3 |
| §9 testing | 1, 2, 4, 6, 10, 12, 13, 14 |
| §10 embedded endpoint → replace-all (⚑7) | 12 |
| §10 `updateAuthorization` deleted | 13 |
| §10 hub client migration, hack + both defects removed | 14 |
| §10 hub inherits the `VALID` reset | 12 (falls out of the call switch; no separate change) |
| §11 `CLAUDE.md` amendment | 11 |
| §11 release note for the breaking API change | 12 Step 5 documents it on the endpoint. **The release note itself is not a task** — this repo has no release-notes file in scope; it is called out here so it reaches whoever cuts the release. |

**Not covered by any task, deliberately:** filing the upstream GitHub issue (spec §11 — a public action needing explicit approval).

**Ordering constraint:** Task 13 deletes `updateAuthorization` and is only safe **after** Task 12 moves its sole caller. Running 13 before 12 breaks the build. Tasks 1–11 are unaffected by either.

**Type consistency:** `replaceConnectionParameters(long, Map<String, ?>)` is declared in Task 1 and called in Task 2. `replaceAuthorizationParameters(long, Map<String, ?>)` is declared in Task 2 and called in Task 4. `updateConnectionCredentials(long, Map<String, ?>, int)` is declared in Task 4 and called in Tasks 5 and 6. `useUpdateConnectionCredentialsMutation` is produced in Task 7 and consumed in Task 9. `credentialStoreType` / `managed` are added in Task 8 and read in Task 9.

**Known imprecision, by design:** Tasks 9 and 10 give anchors and code rather than line numbers. `ConnectionDialog.tsx` is 1132 lines and every earlier task shifts nothing in it, but the gate expressions named in Task 9 Step 5 are unique strings — locate them by search, not by line.
