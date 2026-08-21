# Connection Credential Store PR 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Introduce the `ConnectionCredentialStore` SPI with `DatabaseConnectionCredentialStore` as the always-active default, refactor `ConnectionServiceImpl` to dispatch per-connection via a new `credential_store_type` discriminator, and add the read-only / `registerExisting` plumbing. PR-1 is the foundation that subsequent PRs (GraphQL info query, AWS Secrets Manager adapter, HashiCorp Vault adapter) build on.

**Architecture:** Database store is always registered as a Spring bean (no `@ConditionalOnProperty`). The new schema columns are `credential_store_type INT NOT NULL DEFAULT 0` (ordinal of `ConnectionCredentialStoreType`) and `credential_ref VARCHAR(64)` (nullable; UUID written by future external adapters). `ConnectionServiceImpl` injects `List<ConnectionCredentialStore>` and routes per-row by `connection.credentialStoreType`. All reads run through `populateParameters` so existing callers' `connection.getParameters()` keeps working.

**Tech Stack:** Java 25, Spring Boot 4.0.6, Spring Data JDBC, Liquibase, JUnit 5, Mockito, Testcontainers PostgreSQL 15, AssertJ.

**Spec:** [docs/superpowers/specs/2026-05-19-connection-credential-store-design.md](../specs/2026-05-19-connection-credential-store-design.md)

**Issue:** [bytechefhq/bytechef#547](https://github.com/bytechefhq/bytechef/issues/547)

---

## File Structure

**New files:**
| File | Responsibility |
|---|---|
| `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/service/ConnectionCredentialStore.java` | SPI interface (5 methods + getType) |
| `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/service/ConnectionCredentialStoreType.java` | Enum: DATABASE / AWS_SECRETS_MANAGER / HASHICORP_VAULT |
| `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/exception/ReadOnlyCredentialStoreException.java` | Typed exception thrown by service when store is read-only |
| `server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/service/DatabaseConnectionCredentialStore.java` | Default impl mutating `connection.parameters`; always-registered bean |
| `server/libs/config/liquibase-config/src/main/resources/config/liquibase/changelog/platform/connection/20260519000001_platform_connection_credential_store_columns.xml` | Adds `credential_store_type` + `credential_ref` columns |
| `server/libs/platform/platform-connection/platform-connection-service/src/test/java/com/bytechef/platform/connection/service/TestExternalConnectionCredentialStore.java` | Test helper — in-memory external store stub for dispatch tests |
| `server/libs/platform/platform-connection/platform-connection-service/src/test/java/com/bytechef/platform/connection/service/DatabaseConnectionCredentialStoreTest.java` | Unit test for the default impl |

**Modified files:**
| File | Change |
|---|---|
| `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/domain/Connection.java` | Add `credentialStoreType` (int) + `credentialRef` (String) fields + getters/setters |
| `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/exception/ConnectionErrorType.java` | Add `READ_ONLY_CREDENTIAL_STORE` enum value |
| `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/service/ConnectionService.java` | Add `registerExisting` method |
| `server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/service/ConnectionServiceImpl.java` | Inject `List<ConnectionCredentialStore>`, add `getStore` + `populateParameters` helpers, route writes through store, eager-populate on reads |
| `server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/aspect/TokenRefreshHandler.java` | Check `store.isReadOnly()` before calling `updateConnectionParameters` |
| `server/ee/libs/platform/platform-connection/platform-connection-remote-client/src/main/java/com/bytechef/ee/platform/connection/remote/client/service/RemoteConnectionServiceClient.java` | Stub new `registerExisting` method |
| `server/ee/apps/runtime-job-app/src/main/java/com/bytechef/runtime/job/platform/connection/service/ConnectionServiceImpl.java` | Stub new `registerExisting` method |
| `server/libs/platform/platform-connection/platform-connection-service/src/test/java/com/bytechef/platform/connection/service/ConnectionServiceIntTest.java` | Add dispatch coverage with `TestExternalConnectionCredentialStore` registered alongside Database |
| `server/libs/platform/platform-connection/platform-connection-service/src/test/java/com/bytechef/platform/connection/config/ConnectionIntTestConfiguration.java` | Register `TestExternalConnectionCredentialStore` bean for tests that need it |
| `server/libs/config/liquibase-config/src/main/resources/config/liquibase/master.xml` (or equivalent platform-connection includeAll location) | Include new migration |

**Files to discard:**
The stale exploratory changes in the working tree (modified `ConnectionServiceImpl.java` + new `ConnectionCredentialStore.java` + new `DatabaseConnectionCredentialStore.java`) match the abandoned *single-active-store* design. Task 1 discards them. The new implementation in this plan diverges substantially (the SPI gains `getType()` + `isReadOnly()`, the impl gets dispatched via List, etc.), so reusing the stale code would mislead more than help.

---

### Task 1: Discard stale working-tree changes

**Why:** Three files in the working tree from before the design pivot don't match the agreed design. Starting from a clean slate avoids accidentally carrying over the old single-store assumptions.

**Files:**
- Discard: `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/service/ConnectionCredentialStore.java`
- Discard: `server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/service/DatabaseConnectionCredentialStore.java`
- Restore: `server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/service/ConnectionServiceImpl.java`

- [ ] **Step 1: Inspect current working-tree state**

Run: `git status --short`
Expected output includes:
```
 M server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/service/ConnectionServiceImpl.java
?? server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/service/ConnectionCredentialStore.java
?? server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/service/DatabaseConnectionCredentialStore.java
```

- [ ] **Step 2: Remove the two untracked stale files**

Run:
```bash
rm server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/service/ConnectionCredentialStore.java
rm server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/service/DatabaseConnectionCredentialStore.java
```

- [ ] **Step 3: Restore the modified ConnectionServiceImpl**

Run: `git restore server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/service/ConnectionServiceImpl.java`

- [ ] **Step 4: Verify clean state**

Run: `git status --short`
Expected output: empty (only the design spec commit is on the branch).

- [ ] **Step 5: Verify the existing tests still pass with the original code**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-service:test :server:libs:platform:platform-connection:platform-connection-service:testIntegration`
Expected: BUILD SUCCESSFUL, 18 tests pass.

(No commit — this task just resets the state for fresh implementation.)

---

### Task 2: Add Liquibase migration for the new columns

**Files:**
- Create: `server/libs/config/liquibase-config/src/main/resources/config/liquibase/changelog/platform/connection/20260519000001_platform_connection_credential_store_columns.xml`
- Modify: the platform-connection changelog includer (search for the file that currently `includeAll`s or explicitly lists files under `config/liquibase/changelog/platform/connection/`)

- [ ] **Step 1: Locate the platform-connection changelog inclusion point**

Run:
```bash
find server/libs/config/liquibase-config -name "*.xml" -exec grep -l "platform/connection" {} \;
```

Expected: identifies the master / aggregator file (likely `server/libs/config/liquibase-config/src/main/resources/config/liquibase/master.xml` or a platform-connection-specific aggregator). Open it to confirm how connection migrations are wired (often via `<includeAll path="..."/>`).

- [ ] **Step 2: Inspect existing connection migrations to match formatting conventions**

Run:
```bash
ls server/libs/config/liquibase-config/src/main/resources/config/liquibase/changelog/platform/connection/
```

Open the most recent one (e.g., `20250522081740_platform_connection_connection_updated_column.xml`) to confirm the XML preamble, `<databaseChangeLog>` namespace declarations, and the `id`/`author` format used in this codebase.

- [ ] **Step 3: Write the migration file**

Create `server/libs/config/liquibase-config/src/main/resources/config/liquibase/changelog/platform/connection/20260519000001_platform_connection_credential_store_columns.xml` with content matching the local convention (adapt the namespace/preamble lines from the file inspected in Step 2):

```xml
<?xml version="1.0" encoding="UTF-8"?>

<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.27.xsd">

    <changeSet id="20260519000001" author="Ivica Cardic">
        <addColumn tableName="connection">
            <column name="credential_store_type" type="INT" defaultValueNumeric="0">
                <constraints nullable="false"/>
            </column>
            <column name="credential_ref" type="VARCHAR(64)">
                <constraints nullable="true"/>
            </column>
        </addColumn>
    </changeSet>

</databaseChangeLog>
```

- [ ] **Step 4: Wire the migration into the master changelog if not auto-included**

If the file from Step 1 uses `<includeAll>` over the connection directory, no change is needed — the new file will be picked up automatically. If it uses explicit `<include>` entries, append:

```xml
<include file="config/liquibase/changelog/platform/connection/20260519000001_platform_connection_credential_store_columns.xml"/>
```

after the most recent connection migration include.

- [ ] **Step 5: Verify migration applies cleanly via the existing integration test**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-service:testIntegration --tests "ConnectionRepositoryIntTest"`
Expected: BUILD SUCCESSFUL. The test runs Liquibase against a fresh Testcontainers PostgreSQL — applying the migration successfully (and adding the columns) confirms the XML is valid. **If you see `Filtered out: 0` for the new changeset, that is correct on first run — the changelog test verifies the migration applies cleanly.**

- [ ] **Step 6: Commit**

```bash
git add server/libs/config/liquibase-config/src/main/resources/config/liquibase/changelog/platform/connection/20260519000001_platform_connection_credential_store_columns.xml
# Also stage any modified master changelog if Step 4 required edits
git commit -m "$(cat <<'EOF'
547 Add credential_store_type and credential_ref columns to connection

Foundation for issue #547 — external credential store support. The new
columns let the row carry a discriminator (which credential store owns
the secret) and an opaque reference (UUID into the external vault).
Existing rows default to credential_store_type = 0 (DATABASE).

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: Extend the Connection entity with the new fields

**Files:**
- Modify: `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/domain/Connection.java`
- Test: `server/libs/platform/platform-connection/platform-connection-service/src/test/java/com/bytechef/platform/connection/repository/ConnectionRepositoryIntTest.java` (extend)

- [ ] **Step 1: Open the entity and add the fields**

Add (inserted before the existing `@Column private int type;` field, matching alphabetical-ish ordering with the surrounding fields):

```java
@Column("credential_ref")
@Nullable
private String credentialRef;

@Column("credential_store_type")
private int credentialStoreType;
```

(The existing import `org.jspecify.annotations.Nullable` already exists in this file — verify and reuse.)

- [ ] **Step 2: Add getters and setters following the existing style**

Place near the other getter/setter pairs, matching their style (Javadoc only where the existing methods have it):

```java
@Nullable
public String getCredentialRef() {
    return credentialRef;
}

public void setCredentialRef(@Nullable String credentialRef) {
    this.credentialRef = credentialRef;
}

public ConnectionCredentialStoreType getCredentialStoreType() {
    return ConnectionCredentialStoreType.values()[credentialStoreType];
}

public void setCredentialStoreType(ConnectionCredentialStoreType credentialStoreType) {
    this.credentialStoreType = credentialStoreType.ordinal();
}
```

Add the import:
```java
import com.bytechef.platform.connection.service.ConnectionCredentialStoreType;
```

(`ConnectionCredentialStoreType` doesn't exist yet — IDE will flag it red. Task 4 creates it. The build won't pass until then; that's expected — proceed.)

- [ ] **Step 3: Update the `toString()` method to include the new fields**

Find the existing `toString()` and add (placed after `", type=" + type +`):

```java
", credentialStoreType=" + credentialStoreType +
", credentialRef='" + credentialRef + '\'' +
```

- [ ] **Step 4: Defer testing**

The entity changes can't be tested until Task 4 (enum) and Task 5 (SPI) exist. Move on; the integration test in Task 13 covers the full round-trip.

- [ ] **Step 5: No commit yet**

This task is incomplete on its own (compile error from missing enum). Continue to Task 4; commit when the entity compiles cleanly after Task 4.

---

### Task 4: Define the ConnectionCredentialStoreType enum

**Files:**
- Create: `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/service/ConnectionCredentialStoreType.java`

- [ ] **Step 1: Write the enum file**

Create `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/service/ConnectionCredentialStoreType.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.connection.service;

/**
 * Identifies the backend that holds the credential payload for a given {@link
 * com.bytechef.platform.connection.domain.Connection}.
 *
 * <p>Persisted as INT ordinal on the {@code connection.credential_store_type} column. New values must always be
 * appended (never inserted) to preserve ordinal stability.
 *
 * @author Ivica Cardic
 */
public enum ConnectionCredentialStoreType {

    /** Default — credentials stored encrypted in the {@code connection.parameters} column. */
    DATABASE,

    /** AWS Secrets Manager. Implemented in a follow-up PR. */
    AWS_SECRETS_MANAGER,

    /** HashiCorp Vault (KV v2). Implemented in a follow-up PR. */
    HASHICORP_VAULT
}
```

- [ ] **Step 2: Verify the entity compiles now**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-api:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: No commit yet**

Task 5 (SPI interface) is the natural unit to commit alongside this enum.

---

### Task 5: Define the ConnectionCredentialStore SPI interface

**Files:**
- Create: `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/service/ConnectionCredentialStore.java`

- [ ] **Step 1: Write the interface file**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.connection.service;

import com.bytechef.platform.connection.domain.Connection;
import java.util.Map;

/**
 * Strategy for persisting and resolving the credential payload of a {@link Connection}.
 *
 * <p>{@link DatabaseConnectionCredentialStore} is the always-registered default. Operators may additionally register
 * one external store (AWS Secrets Manager / HashiCorp Vault) via configuration; each connection row carries a
 * {@link ConnectionCredentialStoreType} discriminator so the service can dispatch per-row.
 *
 * <p>Read-only implementations (operator policy via {@code bytechef.connection.credential-store.<provider>.read-only})
 * throw {@link UnsupportedOperationException} from {@link #storeParameters} and {@link #deleteParameters}; callers
 * gate on {@link #isReadOnly()} first.
 *
 * @author Ivica Cardic
 */
public interface ConnectionCredentialStore {

    /** Identifies which connection rows this store handles. */
    ConnectionCredentialStoreType getType();

    /** Whether this store refuses writes in the current deployment. */
    boolean isReadOnly();

    /** Resolve the credential parameters for the given connection. */
    Map<String, ?> getParameters(Connection connection);

    /**
     * Persist the credential payload. Called BEFORE the connection row is saved, so the implementation may mutate
     * the entity (e.g., setting {@code credentialRef}, clearing {@code parameters}). Throws
     * {@link UnsupportedOperationException} on read-only stores.
     */
    void storeParameters(Connection connection, Map<String, ?> parameters);

    /**
     * Remove the credential payload. Called BEFORE the row is deleted. Throws {@link UnsupportedOperationException}
     * on read-only stores.
     */
    void deleteParameters(Connection connection);
}
```

- [ ] **Step 2: Verify the api module compiles**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-api:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit Tasks 3 + 4 + 5 together**

```bash
git add \
  server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/domain/Connection.java \
  server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/service/ConnectionCredentialStore.java \
  server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/service/ConnectionCredentialStoreType.java
git commit -m "$(cat <<'EOF'
547 Add ConnectionCredentialStore SPI and entity fields

Defines the per-connection credential storage abstraction:
- ConnectionCredentialStore interface (getType / isReadOnly / get / store /
  delete) — implementations dispatched by ConnectionServiceImpl per row
- ConnectionCredentialStoreType enum (DATABASE / AWS_SECRETS_MANAGER /
  HASHICORP_VAULT), persisted as INT ordinal
- Connection.credentialStoreType + credentialRef fields with getters/setters

Companion to spec docs/superpowers/specs/2026-05-19-connection-credential-store-design.md.
The DatabaseConnectionCredentialStore impl and ConnectionServiceImpl
refactor follow in subsequent commits.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 6: Add the ReadOnlyCredentialStoreException + ConnectionErrorType case

**Files:**
- Modify: `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/exception/ConnectionErrorType.java`
- Create: `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/exception/ReadOnlyCredentialStoreException.java`

- [ ] **Step 1: Inspect existing ConnectionErrorType**

Run: `cat server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/exception/ConnectionErrorType.java`

Expected: enum implementing `ErrorType` (from the project's common exception infrastructure). Note the existing values (e.g., `CONNECTION_IS_USED`) and their numeric codes.

- [ ] **Step 2: Add the new enum value**

Append (after the last existing constant — appending preserves ordinal stability):

```java
READ_ONLY_CREDENTIAL_STORE(<next-available-code>)
```

Replace `<next-available-code>` with the next integer after the last existing code (e.g., if the last is `103`, use `104`).

- [ ] **Step 3: Inspect an existing typed exception in the codebase for the convention**

Run:
```bash
find server/libs -name "ConfigurationException.java" -path "*/exception/*" | head -3
```

Open the closest existing typed exception that wraps `ConnectionErrorType` (or `ConfigurationException` if `ConnectionErrorType` isn't directly thrown anywhere) to match the constructor signatures.

- [ ] **Step 4: Write ReadOnlyCredentialStoreException**

Create `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/exception/ReadOnlyCredentialStoreException.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.connection.exception;

import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.connection.service.ConnectionCredentialStoreType;

/**
 * Thrown when a write operation is attempted against a {@link ConnectionCredentialStoreType} whose deployment is
 * configured read-only. Caller should refuse the operation and surface the constraint to the user.
 *
 * @author Ivica Cardic
 */
public class ReadOnlyCredentialStoreException extends ConfigurationException {

    public ReadOnlyCredentialStoreException(ConnectionCredentialStoreType storeType) {
        super(
            "Credential store %s is configured read-only — writes are not permitted".formatted(storeType),
            ConnectionErrorType.READ_ONLY_CREDENTIAL_STORE);
    }
}
```

If `ConfigurationException`'s constructor signature differs from `(String, ErrorType)`, adapt to match what you observed in Step 3.

- [ ] **Step 5: Verify compilation**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-api:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add \
  server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/exception/ConnectionErrorType.java \
  server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/exception/ReadOnlyCredentialStoreException.java
git commit -m "$(cat <<'EOF'
547 Add ReadOnlyCredentialStoreException

Typed error surfaced when ConnectionServiceImpl detects a write attempt
against a credential store the operator has marked read-only. Subclass of
ConfigurationException so the existing facade-level error mapping catches it.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 7: Implement DatabaseConnectionCredentialStore + unit test

**Files:**
- Create: `server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/service/DatabaseConnectionCredentialStore.java`
- Create: `server/libs/platform/platform-connection/platform-connection-service/src/test/java/com/bytechef/platform/connection/service/DatabaseConnectionCredentialStoreTest.java`

- [ ] **Step 1: Write the failing unit test**

Create `DatabaseConnectionCredentialStoreTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.connection.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.connection.domain.Connection;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DatabaseConnectionCredentialStoreTest {

    private final DatabaseConnectionCredentialStore store = new DatabaseConnectionCredentialStore();

    @Test
    void testGetTypeIsDatabase() {
        assertThat(store.getType()).isEqualTo(ConnectionCredentialStoreType.DATABASE);
    }

    @Test
    void testIsReadOnlyIsFalse() {
        assertThat(store.isReadOnly()).isFalse();
    }

    @Test
    void testStoreParametersMutatesEntity() {
        Connection connection = new Connection();
        Map<String, Object> params = Map.of("token", "abc123");

        store.storeParameters(connection, params);

        assertThat(connection.getParameters()).containsEntry("token", "abc123");
    }

    @Test
    void testGetParametersReadsEntityField() {
        Connection connection = new Connection();
        Map<String, Object> params = Map.of("apiKey", "xyz");

        connection.setParameters(params);

        assertThat(store.getParameters(connection)).containsEntry("apiKey", "xyz");
    }

    @Test
    void testDeleteParametersIsNoOp() {
        Connection connection = new Connection();
        Map<String, Object> params = Map.of("apiKey", "xyz");

        connection.setParameters(params);

        store.deleteParameters(connection);

        // No-op: parameters remain on the entity (row delete cascades them).
        assertThat(connection.getParameters()).containsEntry("apiKey", "xyz");
    }
}
```

- [ ] **Step 2: Run the test to verify compile failure**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-service:test --tests "DatabaseConnectionCredentialStoreTest"`
Expected: COMPILE FAILURE — `DatabaseConnectionCredentialStore` not found.

- [ ] **Step 3: Write the implementation**

Create `DatabaseConnectionCredentialStore.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.connection.service;

import com.bytechef.platform.connection.domain.Connection;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Default {@link ConnectionCredentialStore} backed by the {@code connection.parameters} column. Parameters are
 * encrypted on disk by the existing {@code EncryptedMapWrapper} converters; this store only mutates the in-memory
 * entity — actual persistence happens when the surrounding service calls {@code ConnectionRepository.save(connection)}.
 *
 * <p>Always registered — no {@code @ConditionalOnProperty}. Connections with
 * {@code credentialStoreType = DATABASE} (the default for every existing and new row) dispatch here.
 *
 * @author Ivica Cardic
 */
@Component
public class DatabaseConnectionCredentialStore implements ConnectionCredentialStore {

    @Override
    public ConnectionCredentialStoreType getType() {
        return ConnectionCredentialStoreType.DATABASE;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public Map<String, ?> getParameters(Connection connection) {
        return connection.getParameters();
    }

    @Override
    public void storeParameters(Connection connection, Map<String, ?> parameters) {
        connection.setParameters(parameters);
    }

    @Override
    public void deleteParameters(Connection connection) {
        // No-op: parameters are cleared when the connection row is deleted.
    }
}
```

- [ ] **Step 4: Run the test to verify pass**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-service:test --tests "DatabaseConnectionCredentialStoreTest"`
Expected: BUILD SUCCESSFUL, 5 tests pass.

- [ ] **Step 5: Commit**

```bash
git add \
  server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/service/DatabaseConnectionCredentialStore.java \
  server/libs/platform/platform-connection/platform-connection-service/src/test/java/com/bytechef/platform/connection/service/DatabaseConnectionCredentialStoreTest.java
git commit -m "$(cat <<'EOF'
547 Add DatabaseConnectionCredentialStore (default impl)

Always-registered Spring bean (no @ConditionalOnProperty). Acts as a thin
shim over the existing Connection.parameters column — the surrounding
ConnectionRepository.save() persists via the existing EncryptedMapWrapper
converters. Behavior is identical to today's direct-field access; the
indirection exists so external stores can slot in without changing
ConnectionServiceImpl.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 8: Add registerExisting to the ConnectionService interface

**Files:**
- Modify: `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/service/ConnectionService.java`

- [ ] **Step 1: Add the method declaration**

Open the interface and add (immediately after the existing `Connection update(...)` declarations and before `updateConnectionCredentialStatus`):

```java
/**
 * Register a connection whose credential payload already exists in an external store. Used in read-only
 * deployments where the operator provisioned the secret out-of-band. The credential payload is NOT written
 * via the credential store — the caller asserts a secret already exists at the path derivable from
 * {@code credentialRef}. Throws if {@code storeType} is {@link ConnectionCredentialStoreType#DATABASE}.
 */
Connection registerExisting(
    Connection connection, ConnectionCredentialStoreType storeType, String credentialRef);
```

Add the import:
```java
import com.bytechef.platform.connection.service.ConnectionCredentialStoreType;
```

(Same package, so technically no import needed — but include it for IDE clarity if `ConnectionService` is in a sub-package. Verify whether the import is required by reading the existing imports.)

- [ ] **Step 2: Verify compile error in implementations**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-api:compileJava`
Expected: BUILD SUCCESSFUL (interface alone compiles).

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-service:compileJava`
Expected: COMPILE FAILURE — `ConnectionServiceImpl` doesn't implement `registerExisting`.

Run: `./gradlew :server:ee:libs:platform:platform-connection:platform-connection-remote-client:compileJava`
Expected: COMPILE FAILURE — `RemoteConnectionServiceClient` doesn't implement `registerExisting`.

Run: `./gradlew :server:ee:apps:runtime-job-app:compileJava`
Expected: COMPILE FAILURE — `runtime-job-app/ConnectionServiceImpl` doesn't implement `registerExisting`.

These failures are expected — Tasks 9, 10, 11 fix them. Do not commit yet.

---

### Task 9: Refactor ConnectionServiceImpl to multi-store dispatch

**Files:**
- Modify: `server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/service/ConnectionServiceImpl.java`

This task is the largest single change. The existing file has 228 lines; the rewrite is roughly the same size. Do it as one focused edit, then run the existing integration tests as the verification.

- [ ] **Step 1: Replace the class body**

Open `ConnectionServiceImpl.java` and replace the entire content (preserving package + license header) with:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.connection.service;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.FormatUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.Connection.CredentialStatus;
import com.bytechef.platform.connection.exception.ReadOnlyCredentialStoreException;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Service("connectionService")
@Transactional
public class ConnectionServiceImpl implements ConnectionService {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionServiceImpl.class);

    private final List<ConnectionCredentialStore> connectionCredentialStores;
    private final ConnectionRepository connectionRepository;

    @SuppressFBWarnings("EI2")
    public ConnectionServiceImpl(
        List<ConnectionCredentialStore> connectionCredentialStores, ConnectionRepository connectionRepository) {

        this.connectionCredentialStores = connectionCredentialStores;
        this.connectionRepository = connectionRepository;
    }

    @Override
    public Connection create(Connection connection) {
        Assert.notNull(connection, "'connection' must not be null");
        Assert.hasText(connection.getComponentName(), "'componentName' must not be empty");
        Assert.hasText(connection.getName(), "'name' must not be empty");
        Assert.isTrue(connection.getId() == null, "'id' must be null");

        ConnectionCredentialStore store = getStore(connection.getCredentialStoreType());

        if (store.isReadOnly()) {
            throw new ReadOnlyCredentialStoreException(store.getType());
        }

        Map<String, ?> parameters = connection.getParameters();

        store.storeParameters(connection, parameters);

        Connection saved = connectionRepository.save(connection);

        return populateParameters(saved);
    }

    @Override
    public Connection create(
        @Nullable AuthorizationType authorizationType, String componentName, int connectionVersion,
        int environmentId, String name, Map<String, Object> parameters, PlatformType platformType) {

        Assert.hasText(componentName, "'componentName' must not be empty");
        Assert.hasText(name, "'name' must not be empty");
        Assert.notNull(parameters, "'parameters' must not be null");
        Assert.notNull(platformType, "'platformType' must not be null");

        Connection connection = new Connection();

        connection.setAuthorizationType(authorizationType);
        connection.setComponentName(componentName);
        connection.setConnectionVersion(connectionVersion);
        connection.setEnvironmentId(environmentId);
        connection.setName(name);
        connection.setParameters(parameters);
        connection.setType(platformType);

        if (logger.isTraceEnabled()) {
            logger.trace("Saved..: {}", FormatUtils.toString(parameters));
        }

        return create(connection);
    }

    @Override
    public void delete(long id) {
        Connection connection = connectionRepository.findById(id)
            .orElseThrow(() -> new IllegalStateException("Connection does not exist for id=" + id));

        ConnectionCredentialStore store = getStore(connection.getCredentialStoreType());

        if (!store.isReadOnly()) {
            store.deleteParameters(connection);
        }
        // When the active store is read-only, the connection row is deleted but the external secret is left intact.
        // The operator owns the vault lifecycle; documented behavior.

        connectionRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Connection getConnection(long id) {
        return populateParameters(
            OptionalUtils.get(connectionRepository.findById(id), "Connection does not exist for id=" + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Connection> getConnections(PlatformType type) {
        return populateAll(
            CollectionUtils.filter(
                connectionRepository.findAll(Sort.by("name", "id")), connection -> connection.getType() == type));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Connection> getConnections(String componentName, int version, PlatformType type) {
        return populateAll(
            connectionRepository.findAllByComponentNameAndConnectionVersionAndTypeOrderByName(
                componentName, version, type.ordinal()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Connection> getConnections(
        String componentName, Integer connectionVersion, Long tagId, Long environmentId, PlatformType type) {

        List<Connection> connections;

        if (StringUtils.isBlank(componentName) && tagId == null) {
            connections = connectionRepository.findAllByTypeOrderByName(type.ordinal());
        } else if (StringUtils.isNotBlank(componentName) && tagId == null) {
            if (connectionVersion == null) {
                connections = connectionRepository.findAllByComponentNameAndTypeOrderByName(
                    componentName, type.ordinal());
            } else {
                connections = connectionRepository.findAllByComponentNameAndConnectionVersionAndTypeOrderByName(
                    componentName, connectionVersion, type.ordinal());
            }
        } else if (StringUtils.isBlank(componentName)) {
            connections = connectionRepository.findAllByTagIdAndTypeOrderByName(tagId, type.ordinal());
        } else {
            if (connectionVersion == null) {
                connections = connectionRepository.findAllByComponentNameAndTagIdAndTypeOrderByName(
                    componentName, tagId, type.ordinal());
            } else {
                connections = connectionRepository.findAllByCNCVTITOrderByName(
                    componentName, connectionVersion, tagId, type.ordinal());
            }
        }

        if (environmentId != null) {
            connections = connections.stream()
                .filter(connection -> connection.getEnvironmentId() == environmentId)
                .toList();
        }

        return populateAll(CollectionUtils.toList(connections));
    }

    @Override
    public List<Connection> getConnections(List<Long> connectionIds) {
        return populateAll(connectionRepository.findAllByIdIn(connectionIds));
    }

    @Override
    public Connection registerExisting(
        Connection connection, ConnectionCredentialStoreType storeType, String credentialRef) {

        Assert.notNull(connection, "'connection' must not be null");
        Assert.notNull(storeType, "'storeType' must not be null");
        Assert.isTrue(storeType != ConnectionCredentialStoreType.DATABASE,
            "registerExisting requires an external store");
        Assert.hasText(credentialRef, "'credentialRef' must not be empty");

        ConnectionCredentialStore store = getStore(storeType);

        connection.setCredentialStoreType(storeType);
        connection.setCredentialRef(credentialRef);
        connection.setParameters(Map.of());

        // Probe existence — fail-fast if the secret doesn't actually exist in the external store.
        store.getParameters(connection);

        return connectionRepository.save(connection);
    }

    @Override
    public Connection update(long id, List<Long> tagIds) {
        Connection connection = getConnection(id);

        connection.setTagIds(tagIds);

        return populateParameters(connectionRepository.save(connection));
    }

    @Override
    public Connection update(long id, String name, List<Long> tagIds, int version) {
        Connection curConnection = getConnection(id);

        if (name != null) {
            curConnection.setName(name);
        }

        if (tagIds != null) {
            curConnection.setTagIds(tagIds);
        }

        curConnection.setVersion(version);

        return populateParameters(connectionRepository.save(curConnection));
    }

    @Override
    public Connection updateConnectionCredentialStatus(long connectionId, CredentialStatus status) {
        Assert.notNull(status, "'status' must not be null");

        Connection connection = getConnection(connectionId);

        connection.setCredentialStatus(status);

        Connection updatedConnection = connectionRepository.save(connection);

        updatedConnection.setCredentialsStatusUpdated();

        return populateParameters(updatedConnection);
    }

    @Override
    public Connection updateConnectionParameters(long connectionId, Map<String, ?> parameters) {
        Assert.notNull(parameters, "'parameters' must not be null");

        Connection connection = getConnection(connectionId);
        ConnectionCredentialStore store = getStore(connection.getCredentialStoreType());

        if (store.isReadOnly()) {
            throw new ReadOnlyCredentialStoreException(store.getType());
        }

        if (logger.isTraceEnabled()) {
            logger.trace("New....: {}", FormatUtils.toString(parameters));
        }

        Map<String, Object> curParameters = new HashMap<>(store.getParameters(connection));

        if (logger.isTraceEnabled()) {
            logger.trace("Current: {}", FormatUtils.toString(curParameters));
        }

        curParameters.putAll(parameters);

        store.storeParameters(connection, curParameters);

        if (logger.isTraceEnabled()) {
            logger.trace("Saved..: {}", FormatUtils.toString(curParameters));
        }

        return populateParameters(connectionRepository.save(connection));
    }

    private ConnectionCredentialStore getStore(ConnectionCredentialStoreType type) {
        return connectionCredentialStores.stream()
            .filter(store -> store.getType() == type)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "No ConnectionCredentialStore registered for type %s. ".formatted(type) +
                    "Configure bytechef.connection.credential-store.external.provider or migrate this connection to DATABASE."));
    }

    private Connection populateParameters(Connection connection) {
        ConnectionCredentialStore store = getStore(connection.getCredentialStoreType());

        connection.setParameters(store.getParameters(connection));

        return connection;
    }

    private List<Connection> populateAll(List<Connection> connections) {
        connections.forEach(this::populateParameters);

        return connections;
    }
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-service:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run the existing integration tests**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-service:testIntegration`
Expected: BUILD SUCCESSFUL, all 18 existing tests pass. The Spring context picks up `DatabaseConnectionCredentialStore` via the test config's existing `@ComponentScan("com.bytechef.platform.connection")`, satisfying the `List<ConnectionCredentialStore>` dependency.

If a test fails with "No ConnectionCredentialStore registered for type DATABASE", it means the test config isn't scanning the new class — see Task 12.

- [ ] **Step 4: Commit**

```bash
git add server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/service/ConnectionServiceImpl.java
git commit -m "$(cat <<'EOF'
547 Refactor ConnectionServiceImpl to multi-store dispatch

Inject List<ConnectionCredentialStore> instead of a single ref. Each
read method runs results through populateParameters so the entity's
parameters field always reflects the resolved credentials (no caller
churn). create / delete / updateConnectionParameters dispatch per
connection.credentialStoreType. registerExisting added for read-only
deployments where the operator provisioned the secret out-of-band.

ConnectionServiceImpl is the only place that knows about multiple
backends; downstream callers stay unchanged.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 10: Stub registerExisting in EE RemoteConnectionServiceClient

**Files:**
- Modify: `server/ee/libs/platform/platform-connection/platform-connection-remote-client/src/main/java/com/bytechef/ee/platform/connection/remote/client/service/RemoteConnectionServiceClient.java`

- [ ] **Step 1: Add the stub method**

Open the file. Add the import:
```java
import com.bytechef.platform.connection.service.ConnectionCredentialStoreType;
```

Append the method (placement: keep the existing alphabetical-ish order of methods — `registerExisting` goes between `getConnections(List<Long>)` and `update(long, List<Long>)`):

```java
@Override
public Connection registerExisting(
    Connection connection, ConnectionCredentialStoreType storeType, String credentialRef) {

    throw new UnsupportedOperationException();
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew :server:ee:libs:platform:platform-connection:platform-connection-remote-client:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run module checks**

Run: `./gradlew :server:ee:libs:platform:platform-connection:platform-connection-remote-client:check`
Expected: BUILD SUCCESSFUL.

(No commit yet — Task 11 stubs the runtime-job-app variant in the same commit unit.)

---

### Task 11: Stub registerExisting in runtime-job-app ConnectionServiceImpl

**Files:**
- Modify: `server/ee/apps/runtime-job-app/src/main/java/com/bytechef/runtime/job/platform/connection/service/ConnectionServiceImpl.java`

- [ ] **Step 1: Add the stub method**

Add the import:
```java
import com.bytechef.platform.connection.service.ConnectionCredentialStoreType;
```

Append the method (same placement as Task 10):

```java
@Override
public Connection registerExisting(
    Connection connection, ConnectionCredentialStoreType storeType, String credentialRef) {

    throw new UnsupportedOperationException();
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew :server:ee:apps:runtime-job-app:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run module checks**

Run: `./gradlew :server:ee:apps:runtime-job-app:check`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit Tasks 8 + 10 + 11 together (registerExisting interface + EE stubs)**

```bash
git add \
  server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/service/ConnectionService.java \
  server/ee/libs/platform/platform-connection/platform-connection-remote-client/src/main/java/com/bytechef/ee/platform/connection/remote/client/service/RemoteConnectionServiceClient.java \
  server/ee/apps/runtime-job-app/src/main/java/com/bytechef/runtime/job/platform/connection/service/ConnectionServiceImpl.java
git commit -m "$(cat <<'EOF'
547 Add ConnectionService.registerExisting with EE stubs

Adds the public service method for registering a connection whose
credentials already live in an external store (used by read-only
deployments). EE remote clients throw UnsupportedOperationException
— they're RPC proxies that don't own connection lifecycle. Real
implementation will land in the configuration-app (server-side
canonical impl) once external stores ship.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 12: Add TestExternalConnectionCredentialStore test helper

**Files:**
- Create: `server/libs/platform/platform-connection/platform-connection-service/src/test/java/com/bytechef/platform/connection/service/TestExternalConnectionCredentialStore.java`

This helper lets the integration test verify dispatch with two stores registered. It's a deliberately minimal in-memory implementation.

- [ ] **Step 1: Write the helper**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.connection.service;

import com.bytechef.platform.connection.domain.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test-only {@link ConnectionCredentialStore} that imitates an external secret store entirely in memory. Used by
 * {@code ConnectionServiceIntTest} to verify multi-store dispatch without standing up a vault. Maps the entity's
 * {@code credentialRef} to a parameters payload; generates a fresh UUID on first store.
 *
 * <p>Registered as type {@link ConnectionCredentialStoreType#HASHICORP_VAULT} purely so it has a non-DATABASE value
 * — the test doesn't care which non-default discriminator the store reports.
 *
 * @author Ivica Cardic
 */
public class TestExternalConnectionCredentialStore implements ConnectionCredentialStore {

    private final Map<String, Map<String, Object>> secrets = new ConcurrentHashMap<>();
    private boolean readOnly;

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    @Override
    public ConnectionCredentialStoreType getType() {
        return ConnectionCredentialStoreType.HASHICORP_VAULT;
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public Map<String, ?> getParameters(Connection connection) {
        String ref = connection.getCredentialRef();

        if (ref == null) {
            return Map.of();
        }

        return secrets.getOrDefault(ref, Map.of());
    }

    @Override
    public void storeParameters(Connection connection, Map<String, ?> parameters) {
        String ref = connection.getCredentialRef();

        if (ref == null) {
            ref = UUID.randomUUID()
                .toString();

            connection.setCredentialRef(ref);
        }

        secrets.put(ref, new HashMap<>(parameters));

        connection.setParameters(Map.of());
    }

    @Override
    public void deleteParameters(Connection connection) {
        String ref = connection.getCredentialRef();

        if (ref != null) {
            secrets.remove(ref);
        }
    }
}
```

- [ ] **Step 2: Verify compile**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-service:compileTestJava`
Expected: BUILD SUCCESSFUL.

(No commit yet — Task 13 ties this helper to actual tests in the same commit.)

---

### Task 13: Extend ConnectionServiceIntTest with dispatch coverage

**Files:**
- Modify: `server/libs/platform/platform-connection/platform-connection-service/src/test/java/com/bytechef/platform/connection/config/ConnectionIntTestConfiguration.java`
- Modify: `server/libs/platform/platform-connection/platform-connection-service/src/test/java/com/bytechef/platform/connection/service/ConnectionServiceIntTest.java`

- [ ] **Step 1: Register the test helper bean in the int test config**

Open `ConnectionIntTestConfiguration.java`. Find the existing `@Bean ComponentHandler componentHandler()` method (around line 67). After the closing brace of `componentHandler()`, add:

```java
@Bean
TestExternalConnectionCredentialStore testExternalConnectionCredentialStore() {
    return new TestExternalConnectionCredentialStore();
}
```

Add the import:
```java
import com.bytechef.platform.connection.service.TestExternalConnectionCredentialStore;
```

- [ ] **Step 2: Write the failing dispatch test cases**

Open `ConnectionServiceIntTest.java`. Add the following imports at the top:

```java
import com.bytechef.platform.connection.exception.ReadOnlyCredentialStoreException;
import com.bytechef.platform.connection.service.ConnectionCredentialStoreType;
import com.bytechef.platform.connection.service.TestExternalConnectionCredentialStore;
```

After the existing `private TagRepository tagRepository;` field, add:

```java
@Autowired
private TestExternalConnectionCredentialStore testExternalConnectionCredentialStore;
```

Update the existing `@AfterEach afterEach()` method to also reset the external store:

```java
@AfterEach
public void afterEach() {
    connectionRepository.deleteAll();
    tagRepository.deleteAll();
    testExternalConnectionCredentialStore.setReadOnly(false);
}
```

Add the following new test methods to the class (place after the last existing `@Test` method):

```java
@Test
public void testCreateExternalBackedConnection() {
    Connection connection = getConnection();

    connection.setCredentialStoreType(ConnectionCredentialStoreType.HASHICORP_VAULT);

    Connection saved = connectionService.create(connection);

    Long id = Validate.notNull(saved.getId(), "id");

    assertThat(saved.getCredentialStoreType())
        .isEqualTo(ConnectionCredentialStoreType.HASHICORP_VAULT);
    assertThat(saved.getCredentialRef()).isNotBlank();
    assertThat(saved.getParameters()).containsEntry("key1", "value1");

    // Reload from DB and verify the row carries only the ref, while the service re-populates parameters on read.
    Connection persistedRow = Validate.notNull(
        connectionRepository.findById(id).orElse(null), "persisted row");

    assertThat(persistedRow.getParameters()).isEmpty();
    assertThat(persistedRow.getCredentialRef()).isNotBlank();

    Connection viaService = connectionService.getConnection(id);

    assertThat(viaService.getParameters()).containsEntry("key1", "value1");
}

@Test
public void testUpdateParametersOnExternalStore() {
    Connection connection = getConnection();

    connection.setCredentialStoreType(ConnectionCredentialStoreType.HASHICORP_VAULT);

    Connection saved = connectionService.create(connection);
    Long id = Validate.notNull(saved.getId(), "id");

    Connection updated = connectionService.updateConnectionParameters(
        id, Map.of("key2", "value2"));

    assertThat(updated.getParameters())
        .containsEntry("key1", "value1")
        .containsEntry("key2", "value2");
}

@Test
public void testDeleteExternalBackedConnection() {
    Connection connection = getConnection();

    connection.setCredentialStoreType(ConnectionCredentialStoreType.HASHICORP_VAULT);

    Connection saved = connectionService.create(connection);
    Long id = Validate.notNull(saved.getId(), "id");

    connectionService.delete(id);

    assertThat(connectionRepository.findById(id)).isNotPresent();
    assertThat(testExternalConnectionCredentialStore.getParameters(saved)).isEmpty();
}

@Test
public void testCreateRefusedWhenStoreReadOnly() {
    testExternalConnectionCredentialStore.setReadOnly(true);

    Connection connection = getConnection();

    connection.setCredentialStoreType(ConnectionCredentialStoreType.HASHICORP_VAULT);

    assertThatThrownBy(() -> connectionService.create(connection))
        .isInstanceOf(ReadOnlyCredentialStoreException.class);
}

@Test
public void testRegisterExistingForReadOnlyStore() {
    testExternalConnectionCredentialStore.setReadOnly(true);

    // Pre-seed a "secret" in the external store keyed by a known ref.
    Connection seed = new Connection();
    seed.setCredentialRef("preprovisioned-ref-123");

    testExternalConnectionCredentialStore.setReadOnly(false);
    testExternalConnectionCredentialStore.storeParameters(seed, Map.of("apiKey", "secret-from-vault"));
    testExternalConnectionCredentialStore.setReadOnly(true);

    Connection connection = new Connection();

    connection.setComponentName("componentName");
    connection.setName("registered");
    connection.setType(PlatformType.AUTOMATION);

    Connection registered = connectionService.registerExisting(
        connection, ConnectionCredentialStoreType.HASHICORP_VAULT, "preprovisioned-ref-123");

    Connection viaService = connectionService.getConnection(Validate.notNull(registered.getId(), "id"));

    assertThat(viaService.getParameters()).containsEntry("apiKey", "secret-from-vault");
    assertThat(viaService.getCredentialRef()).isEqualTo("preprovisioned-ref-123");
}
```

Add the import for `assertThatThrownBy`:

```java
import static org.assertj.core.api.Assertions.assertThatThrownBy;
```

- [ ] **Step 3: Run the new tests**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-service:testIntegration --tests "ConnectionServiceIntTest"`
Expected: BUILD SUCCESSFUL, all tests pass (existing 18 + 5 new = 23).

If `testCreateExternalBackedConnection` fails on `assertThat(persistedRow.getParameters()).isEmpty()`, double-check `TestExternalConnectionCredentialStore.storeParameters` actually calls `connection.setParameters(Map.of())` at the end.

- [ ] **Step 4: Commit Tasks 12 + 13 together**

```bash
git add \
  server/libs/platform/platform-connection/platform-connection-service/src/test/java/com/bytechef/platform/connection/service/TestExternalConnectionCredentialStore.java \
  server/libs/platform/platform-connection/platform-connection-service/src/test/java/com/bytechef/platform/connection/config/ConnectionIntTestConfiguration.java \
  server/libs/platform/platform-connection/platform-connection-service/src/test/java/com/bytechef/platform/connection/service/ConnectionServiceIntTest.java
git commit -m "$(cat <<'EOF'
547 Test multi-store dispatch in ConnectionServiceIntTest

Adds an in-memory TestExternalConnectionCredentialStore registered
alongside DatabaseConnectionCredentialStore so the int test can verify:
- create dispatches to the right store based on credentialStoreType
- read path populates parameters from the external store on each getConnection
- updateConnectionParameters writes back through the external store
- delete removes the external secret
- create against a read-only store throws ReadOnlyCredentialStoreException
- registerExisting works when the secret already exists in a read-only store

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 14: Verify ConnectionFacadeIntTest passes unchanged

**Files:**
- Test only: `server/libs/platform/platform-connection/platform-connection-service/src/test/java/com/bytechef/platform/connection/facade/ConnectionFacadeIntTest.java`

This task is a verification gate. The facade should be unaffected — it uses `ConnectionService` via DI, and the new behavior is invisible from above the service.

- [ ] **Step 1: Run the facade integration test**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-service:testIntegration --tests "ConnectionFacadeIntTest"`
Expected: BUILD SUCCESSFUL.

If a test fails, the most likely cause is `Connection.equals()` / `hashCode()` semantics shifting because of the new field — open the failing assertion and look for "got null, expected null" mismatches around `credentialRef` or `credentialStoreType`. Fix by ensuring the entity's `equals` continues to compare on `id` only (it already does — see `Connection.java`).

- [ ] **Step 2: No commit**

This task is verification only.

---

### Task 15: Wire TokenRefreshHandler read-only check

**Files:**
- Modify: `server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/aspect/TokenRefreshHandler.java`

- [ ] **Step 1: Read the current method around line 165**

Open the file. Find `connectionService.updateConnectionParameters(...)` — this is the call site that writes refreshed tokens back. Today it always proceeds.

- [ ] **Step 2: Add the credential store dependency**

Add the import:
```java
import com.bytechef.platform.connection.service.ConnectionCredentialStore;
import com.bytechef.platform.connection.service.ConnectionCredentialStoreType;
import java.util.List;
import java.util.Optional;
```

Add the field:
```java
private final List<ConnectionCredentialStore> connectionCredentialStores;
```

Update the constructor to accept the new dependency. Find the existing constructor; add `List<ConnectionCredentialStore> connectionCredentialStores` as a parameter and assign it. If the constructor is annotated `@SuppressFBWarnings("EI2")`, keep that annotation — the new mutable ref triggers the same check.

- [ ] **Step 3: Guard the updateConnectionParameters call**

Find the existing call (~line 165):
```java
connection = connectionService.updateConnectionParameters(
    connection.getId(), ...);
```

Wrap it with the read-only check. The block becomes:

```java
ConnectionCredentialStoreType storeType = connection.getCredentialStoreType();
Optional<ConnectionCredentialStore> store = connectionCredentialStores.stream()
    .filter(curStore -> curStore.getType() == storeType)
    .findFirst();

if (store.map(ConnectionCredentialStore::isReadOnly).orElse(false)) {
    log.warn(
        "Cannot refresh token for connection {} — credential store {} is read-only",
        connection.getId(), storeType);

    connectionService.updateConnectionCredentialStatus(
        connection.getId(), Connection.CredentialStatus.INVALID);

    return;
}

connection = connectionService.updateConnectionParameters(
    connection.getId(), ...);
```

(Match the existing `...` arguments — don't change them.)

- [ ] **Step 4: Compile and run module check**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-service:compileJava`
Expected: BUILD SUCCESSFUL.

Run: `./gradlew :server:libs:platform:platform-component:platform-component-service:test --tests "TokenRefreshAspectIntTest"`
Expected: BUILD SUCCESSFUL. The existing test uses `@MockitoBean ConnectionService`, so the read-only check path is exercised only if the test happens to mock `connectionCredentialStores`. For PR-1 (where Database is the only store and is never read-only), the existing tests should pass without modification — the early-return branch is unreachable.

If the test fails because `connectionCredentialStores` isn't auto-wired, add `@MockitoBean private List<ConnectionCredentialStore> connectionCredentialStores;` to the test class fields and a `when(connectionCredentialStores.stream())...` stub returning the database store with `isReadOnly() = false`.

- [ ] **Step 5: Commit**

```bash
git add server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/aspect/TokenRefreshHandler.java
# If test wiring required updates, also stage that file
git commit -m "$(cat <<'EOF'
547 Guard TokenRefreshHandler against read-only credential stores

When the active store reports isReadOnly(), refreshed OAuth tokens can't
be written back. Instead of throwing at the persistence layer, log a
warning and mark the connection's credential status INVALID so the
existing UI flag surfaces it. Database-backed connections (the default)
are never read-only, so this branch is dead code until external stores
ship in subsequent PRs — wiring it now keeps the PRs that introduce
external stores small.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 16: Final verification — spotless, check, full module sweep

**Files:** none new

- [ ] **Step 1: Apply spotless across all touched modules**

Run:
```bash
./gradlew \
  :server:libs:platform:platform-connection:platform-connection-api:spotlessApply \
  :server:libs:platform:platform-connection:platform-connection-service:spotlessApply \
  :server:libs:platform:platform-component:platform-component-service:spotlessApply \
  :server:ee:libs:platform:platform-connection:platform-connection-remote-client:spotlessApply \
  :server:ee:apps:runtime-job-app:spotlessApply
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Run check across all touched modules**

Run:
```bash
./gradlew \
  :server:libs:platform:platform-connection:platform-connection-api:check \
  :server:libs:platform:platform-connection:platform-connection-service:check \
  :server:libs:platform:platform-component:platform-component-service:check \
  :server:ee:libs:platform:platform-connection:platform-connection-remote-client:check \
  :server:ee:apps:runtime-job-app:check
```

Expected: BUILD SUCCESSFUL. Common failure modes and fixes:
- **SpotBugs `EI_EXPOSE_REP2`** on the new constructors → add `@SuppressFBWarnings("EI2")`. Pattern matches existing constructors in the codebase.
- **Checkstyle `MethodLength`** if a method grew too long → split a helper method.
- **PMD `UnusedFormalParameter`** on EE stubs → annotate the class or method with `@SuppressWarnings("PMD.UnusedFormalParameter")`.

- [ ] **Step 3: If spotless made formatting changes, commit them**

Run: `git status --short`
If files are modified, commit:
```bash
git add -u
git commit -m "$(cat <<'EOF'
547 Apply spotless formatting

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

If nothing changed, skip this step.

- [ ] **Step 4: Final smoke — verify the whole branch builds and tests cleanly**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-service:check`
Expected: BUILD SUCCESSFUL, all tests pass (existing 18 + 5 new from Task 13 + 5 new from Task 7 = 28 tests in this module).

- [ ] **Step 5: Inspect the commit graph for the PR**

Run: `git log --oneline master..HEAD`
Expected output (order may vary if Tasks 8+10+11 were combined):

```
<hash> 547 Apply spotless formatting              (only if Step 3 was needed)
<hash> 547 Guard TokenRefreshHandler against read-only credential stores
<hash> 547 Test multi-store dispatch in ConnectionServiceIntTest
<hash> 547 Add ConnectionService.registerExisting with EE stubs
<hash> 547 Refactor ConnectionServiceImpl to multi-store dispatch
<hash> 547 Add DatabaseConnectionCredentialStore (default impl)
<hash> 547 Add ReadOnlyCredentialStoreException
<hash> 547 Add ConnectionCredentialStore SPI and entity fields
<hash> 547 Add credential_store_type and credential_ref columns to connection
<hash> 547 Add connection credential store design spec
```

- [ ] **Step 6: PR-ready check**

The PR contains 9 commits (10 if Step 3 was needed). Each commit is self-contained; the test suite passes after each one (except Tasks 3-5 which build incrementally toward a single commit, and Tasks 8/10/11 which build incrementally toward a single commit — both intentional). PR-1 is complete.

---

## Self-Review Notes

**Spec coverage:** Every requirement in the spec's PR-1 scope has a corresponding task:
- ✓ `ConnectionCredentialStore` SPI → Task 5
- ✓ `ConnectionCredentialStoreType` enum → Task 4
- ✓ `DatabaseConnectionCredentialStore` always-registered bean → Task 7
- ✓ Liquibase migration → Task 2
- ✓ Connection entity fields → Task 3
- ✓ `ConnectionServiceImpl` multi-store dispatch + `populateParameters` + `registerExisting` → Tasks 8, 9
- ✓ `ReadOnlyCredentialStoreException` → Task 6
- ✓ `TokenRefreshHandler` read-only check → Task 15
- ✓ `ConnectionServiceIntTest` dispatch coverage → Task 13
- ✓ `spotlessApply` + `check` on all touched modules → Task 16

**Out-of-scope items confirmed not in plan:**
- ✓ GraphQL query — defers to PR 2
- ✓ AWS Secrets Manager adapter — defers to PR 3
- ✓ HashiCorp Vault adapter — defers to PR 4
- ✓ Frontend UI — sibling spec
- ✓ AWS KMS EncryptionKey provider — sibling track

**Type consistency:** All type/method names cross-reference correctly between tasks (`ConnectionCredentialStore`, `ConnectionCredentialStoreType`, `DatabaseConnectionCredentialStore`, `ReadOnlyCredentialStoreException`, `TestExternalConnectionCredentialStore`, `registerExisting`, `populateParameters`, `getStore`).

**Identified risks:**
- Task 6's exception class is written based on the spec's expectation that `ConfigurationException` exists with a `(String, ErrorType)` constructor. Task 6 Step 3 explicitly tells the engineer to verify the signature before writing the file. If the signature differs, adapt.
- Task 15's test wiring (`@MockitoBean private List<ConnectionCredentialStore>`) may need adjustment depending on how the test currently mocks `ConnectionService` — guidance is in the task.
- Task 2's Liquibase changelog inclusion mechanism varies by project — the task explicitly inspects the existing aggregator first.
