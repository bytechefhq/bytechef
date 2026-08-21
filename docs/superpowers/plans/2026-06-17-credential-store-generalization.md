# Credential Store Generalization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Lift the connection-only credential-store SPI into a neutral `platform-credential-store` module so one adapter per provider (DATABASE / AWS Secrets Manager / HashiCorp Vault) serves both `Connection` and `Property` via a shared `CredentialSecret` seam.

**Architecture:** A new neutral CE module exposes `CredentialSecret` (entity seam), `CredentialStore` (SPI), `CredentialStoreType` (shared enum), `CredentialPathResolver`, and the read-only exception. `Connection` and `Property` implement `CredentialSecret`; their services dispatch through an injected `List<CredentialStore>`. The two EE adapters relocate out of `platform-connection`, retype from `Connection` to `CredentialSecret`, and read a unified `bytechef.credential-store.*` config tree.

**Tech Stack:** Java 25, Spring Boot 4, Spring Data JDBC, Gradle (Kotlin DSL), JUnit 5 + Mockito, Liquibase, Caffeine, Spring Cloud AWS, Spring Vault, Testcontainers (LocalStack / Vault dev mode).

**Spec:** [docs/superpowers/specs/2026-06-17-credential-store-generalization-design.md](../specs/2026-06-17-credential-store-generalization-design.md)

## Global Constraints

- Enum ordinals are pinned: `DATABASE=0, AWS_SECRETS_MANAGER=1, HASHICORP_VAULT=2`. New values append only — never insert.
- CE files (everything under `server/libs/`) use the Apache 2.0 license header. EE files (under `server/ee/`) use the ByteChef Enterprise header AND carry a `@version ee` Javadoc tag (Spotless selects the header by file content, so the tag must be present).
- Run `./gradlew spotlessApply` before every commit; code must pass `checkstyleMain`, `pmdMain`, `spotbugsMain`.
- Do not chain method calls except idiomatic builders/streams/Optional. One blank line before control statements and after a variable modification that precedes its use.
- Descriptive variable names (no single letters in lambdas/loops): `store` not `s`, `connection` not `c`.
- Never `git commit --amend` on branch `0_732`; always fresh commits. Stage only files touched by the current task.
- The breaking config rename `bytechef.connection.credential-store.*` → `bytechef.credential-store.*` is intentional; no compatibility shim.

---

## File Structure

**New module `platform-credential-store-api`** (`server/libs/platform/platform-credential-store/platform-credential-store-api/`):
- `…/platform/credential/store/CredentialStoreType.java` — shared discriminator enum
- `…/platform/credential/store/CredentialSecret.java` — entity-facing seam
- `…/platform/credential/store/CredentialStore.java` — SPI
- `…/platform/credential/store/CredentialPathResolver.java` — moved from `platform-connection-api`
- `…/platform/credential/store/exception/CredentialStoreErrorType.java`
- `…/platform/credential/store/exception/ReadOnlyCredentialStoreException.java`

**New module `platform-credential-store-service`** (`server/libs/platform/platform-credential-store/platform-credential-store-service/`):
- `…/platform/credential/store/service/DatabaseCredentialStore.java` — always-registered default

**Modified (connection):** `Connection.java`, `ConnectionService.java`, `ConnectionServiceImpl.java`, `ConnectionCredentialStoreGraphQlController.java`, `ConnectionCredentialStoreInfo.java`, `connection-credential-store.graphqls`, both `build.gradle.kts`. **Deleted:** `ConnectionCredentialStore.java`, `ConnectionCredentialStoreType.java`, `DatabaseConnectionCredentialStore.java`, connection `ReadOnlyCredentialStoreException.java`, `ConnectionErrorType.READ_ONLY_CREDENTIAL_STORE`.

**Modified (configuration):** `Property.java`, `PropertyServiceImpl.java`, both `build.gradle.kts`. **Deleted (first-pass):** `PropertyCredentialStore.java`, `PropertyCredentialStoreType.java`, `DatabasePropertyCredentialStore.java`, `PropertyErrorType.java`, configuration `ReadOnlyCredentialStoreException.java`. **Kept:** the `property` Liquibase migration + the entity columns.

**Modified (config + REST):** `ApplicationProperties.java`, both `CredentialStoreTypeMapper.java`.

**Relocated (EE):** `platform-connection-credential-store-aws-secrets-manager` → `platform-credential-store/credential-store-aws-secrets-manager`; `…-hashicorp-vault` likewise; `settings.gradle.kts` includes updated.

---

## Task 1: Neutral API module — seam, SPI, enum, resolver, exception

**Files:**
- Create: `server/libs/platform/platform-credential-store/platform-credential-store-api/build.gradle.kts`
- Create: `.../platform/credential/store/CredentialStoreType.java`
- Create: `.../platform/credential/store/CredentialSecret.java`
- Create: `.../platform/credential/store/CredentialStore.java`
- Create: `.../platform/credential/store/CredentialPathResolver.java` (content moved from connection-api)
- Create: `.../platform/credential/store/exception/CredentialStoreErrorType.java`
- Create: `.../platform/credential/store/exception/ReadOnlyCredentialStoreException.java`
- Test: `.../src/test/java/com/bytechef/platform/credential/store/CredentialPathResolverTest.java` (moved)
- Modify: `settings.gradle.kts` (add include after line 205)

**Interfaces:**
- Produces: `CredentialSecret { @Nullable String getCredentialRef(); void setCredentialRef(@Nullable String); Map<String,?> getPayload(); void setPayload(Map<String,?>); CredentialStoreType getCredentialStoreType(); void setCredentialStoreType(CredentialStoreType); }`
- Produces: `CredentialStore { CredentialStoreType getType(); boolean isReadOnly(); Map<String,?> getSecret(CredentialSecret); void storeSecret(CredentialSecret, Map<String,?>); void deleteSecret(CredentialSecret); }`
- Produces: `enum CredentialStoreType { DATABASE, AWS_SECRETS_MANAGER, HASHICORP_VAULT }`
- Produces: `CredentialPathResolver.resolve(String template, @Nullable String tenant, @Nullable String environment, @Nullable String ref): String`
- Produces: `ReadOnlyCredentialStoreException(CredentialStoreType)`

- [ ] **Step 1: Register the module in settings.gradle.kts**

Add after `include("server:libs:platform:platform-connection:platform-connection-api")` (line 203 region):

```kotlin
include("server:libs:platform:platform-credential-store:platform-credential-store-api")
include("server:libs:platform:platform-credential-store:platform-credential-store-service")
```

- [ ] **Step 2: Create the api `build.gradle.kts`**

```kotlin
dependencies {
    api(project(":server:libs:core:exception:exception-api"))

    implementation("org.apache.commons:commons-lang3")

    testImplementation(project(":server:libs:test:test-support"))
}
```

- [ ] **Step 3: Create `CredentialStoreType`**

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

package com.bytechef.platform.credential.store;

/**
 * Identifies the backend that holds the credential payload for a {@link CredentialSecret}. Persisted as INT ordinal on
 * each entity's {@code credential_store_type} column. New values must be appended (never inserted) to preserve ordinal
 * stability.
 *
 * @author Ivica Cardic
 */
public enum CredentialStoreType {

    DATABASE,
    AWS_SECRETS_MANAGER,
    HASHICORP_VAULT
}
```

- [ ] **Step 4: Create `CredentialSecret`**

```java
/* Apache header */
package com.bytechef.platform.credential.store;

import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Entity-facing seam implemented by every credential-bearing entity ({@code Connection}, {@code Property}). It is
 * exactly the surface a {@link CredentialStore} touches — the inline payload accessors plus the external-store
 * discriminator and reference.
 *
 * @author Ivica Cardic
 */
public interface CredentialSecret {

    @Nullable
    String getCredentialRef();

    void setCredentialRef(@Nullable String credentialRef);

    /** The inline (in-entity) decrypted payload. Read by the DATABASE-backed store. */
    Map<String, ?> getPayload();

    /** Set or clear the inline payload. External stores call this with {@code Map.of()} to clear. */
    void setPayload(Map<String, ?> payload);

    CredentialStoreType getCredentialStoreType();

    void setCredentialStoreType(CredentialStoreType credentialStoreType);
}
```

- [ ] **Step 5: Create `CredentialStore`**

```java
/* Apache header */
package com.bytechef.platform.credential.store;

import java.util.Map;

/**
 * Strategy for persisting and resolving the credential payload of a {@link CredentialSecret}. The DATABASE-backed store
 * is always registered; operators may additionally register one external store. Read-only implementations throw
 * {@link UnsupportedOperationException} from {@link #storeSecret} / {@link #deleteSecret}; callers gate on
 * {@link #isReadOnly()} first.
 *
 * @author Ivica Cardic
 */
public interface CredentialStore {

    CredentialStoreType getType();

    boolean isReadOnly();

    Map<String, ?> getSecret(CredentialSecret secret);

    /**
     * Persist the payload. Called BEFORE the row is saved, so the implementation may mutate the entity (set
     * {@code credentialRef}, clear the inline payload).
     */
    void storeSecret(CredentialSecret secret, Map<String, ?> payload);

    /** Remove the payload. Called BEFORE the row is deleted. */
    void deleteSecret(CredentialSecret secret);
}
```

- [ ] **Step 6: Move `CredentialPathResolver` into this module**

Copy the existing body of `server/libs/platform/platform-connection/platform-connection-api/.../connection/util/CredentialPathResolver.java` verbatim, changing only the package line to:

```java
package com.bytechef.platform.credential.store;
```

Delete the original `platform-connection-api/.../connection/util/CredentialPathResolver.java`.

- [ ] **Step 7: Move `CredentialPathResolverTest` into this module**

Copy the existing `platform-connection-api/.../connection/util/CredentialPathResolverTest.java` to `platform-credential-store-api/src/test/java/com/bytechef/platform/credential/store/CredentialPathResolverTest.java`, changing the package line to `com.bytechef.platform.credential.store` and updating the import of `CredentialPathResolver` accordingly. Delete the original test.

- [ ] **Step 8: Create `CredentialStoreErrorType`**

```java
/* Apache header */
package com.bytechef.platform.credential.store.exception;

import com.bytechef.exception.AbstractErrorType;
import com.bytechef.platform.credential.store.CredentialStore;

/**
 * Domain error codes for credential-store operations. Keys are numeric and MUST remain stable.
 *
 * @author Ivica Cardic
 */
public class CredentialStoreErrorType extends AbstractErrorType {

    public static final CredentialStoreErrorType READ_ONLY_CREDENTIAL_STORE = new CredentialStoreErrorType(100);

    private CredentialStoreErrorType(int errorKey) {
        super(CredentialStore.class, errorKey);
    }
}
```

- [ ] **Step 9: Create `ReadOnlyCredentialStoreException`**

```java
/* Apache header */
package com.bytechef.platform.credential.store.exception;

import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.credential.store.CredentialStoreType;

/**
 * Thrown when a write is attempted against a {@link CredentialStoreType} configured read-only.
 *
 * @author Ivica Cardic
 */
public class ReadOnlyCredentialStoreException extends ConfigurationException {

    public ReadOnlyCredentialStoreException(CredentialStoreType storeType) {
        super(
            "Credential store %s is configured read-only — writes are not permitted".formatted(storeType),
            CredentialStoreErrorType.READ_ONLY_CREDENTIAL_STORE);
    }
}
```

- [ ] **Step 10: Compile + run the moved test**

Run: `./gradlew :server:libs:platform:platform-credential-store:platform-credential-store-api:compileJava :server:libs:platform:platform-credential-store:platform-credential-store-api:test`
Expected: BUILD SUCCESSFUL; `CredentialPathResolverTest` passes.

- [ ] **Step 11: Spotless + commit**

```bash
./gradlew :server:libs:platform:platform-credential-store:platform-credential-store-api:spotlessApply
git add settings.gradle.kts \
  server/libs/platform/platform-credential-store/platform-credential-store-api \
  server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/util/CredentialPathResolver.java \
  server/libs/platform/platform-connection/platform-connection-api/src/test/java/com/bytechef/platform/connection/util/CredentialPathResolverTest.java
git commit -m "547 Add platform-credential-store-api shared SPI and seam

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

> Note: connection-api will not compile standalone until Task 3 (it still imports the moved `CredentialPathResolver` from its old package via the EE adapters; nothing in connection-api itself references it). The repo as a whole is rebuilt at the end of Task 3. If you prefer green-at-every-task, fold Task 1 Steps 6–7 into Task 3.

---

## Task 2: Neutral service module — `DatabaseCredentialStore`

**Files:**
- Create: `server/libs/platform/platform-credential-store/platform-credential-store-service/build.gradle.kts`
- Create: `.../platform/credential/store/service/DatabaseCredentialStore.java`
- Test: `.../src/test/java/com/bytechef/platform/credential/store/service/DatabaseCredentialStoreTest.java`

**Interfaces:**
- Consumes: `CredentialStore`, `CredentialSecret`, `CredentialStoreType` (Task 1)
- Produces: `DatabaseCredentialStore` — `@Component`, `getType()==DATABASE`, `isReadOnly()==false`, `getSecret`=`secret.getPayload()`, `storeSecret`=`secret.setPayload(payload)`, `deleteSecret`=no-op

- [ ] **Step 1: Create the service `build.gradle.kts` and register the module**

First create the file `server/libs/platform/platform-credential-store/platform-credential-store-service/build.gradle.kts`:

```kotlin
dependencies {
    api(project(":server:libs:platform:platform-credential-store:platform-credential-store-api"))

    implementation("org.springframework:spring-context")

    testImplementation(project(":server:libs:test:test-support"))
}
```

Then add the `settings.gradle.kts` include (deferred from Task 1 because Gradle 9 rejects an include for a not-yet-existing directory). Place it immediately after the existing `platform-credential-store-api` include line:

```kotlin
include("server:libs:platform:platform-credential-store:platform-credential-store-service")
```

- [ ] **Step 2: Write the failing test**

```java
/* Apache header */
package com.bytechef.platform.credential.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.credential.store.CredentialSecret;
import com.bytechef.platform.credential.store.CredentialStoreType;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DatabaseCredentialStoreTest {

    private final DatabaseCredentialStore databaseCredentialStore = new DatabaseCredentialStore();

    @Test
    void testGetTypeIsDatabase() {
        assertThat(databaseCredentialStore.getType()).isEqualTo(CredentialStoreType.DATABASE);
    }

    @Test
    void testIsReadOnlyFalse() {
        assertThat(databaseCredentialStore.isReadOnly()).isFalse();
    }

    @Test
    void testGetSecretReadsInlinePayload() {
        CredentialSecret secret = mock(CredentialSecret.class);

        when(secret.getPayload()).thenReturn(Map.of("apiKey", "abc"));

        assertThat(databaseCredentialStore.getSecret(secret)).containsEntry("apiKey", "abc");
    }

    @Test
    void testStoreSecretWritesInlinePayload() {
        CredentialSecret secret = mock(CredentialSecret.class);
        Map<String, Object> payload = Map.of("apiKey", "abc");

        databaseCredentialStore.storeSecret(secret, payload);

        verify(secret).setPayload(payload);
    }
}
```

- [ ] **Step 3: Run test, verify it fails**

Run: `./gradlew :server:libs:platform:platform-credential-store:platform-credential-store-service:test`
Expected: FAIL — `DatabaseCredentialStore` does not exist.

- [ ] **Step 4: Implement `DatabaseCredentialStore`**

```java
/* Apache header */
package com.bytechef.platform.credential.store.service;

import com.bytechef.platform.credential.store.CredentialSecret;
import com.bytechef.platform.credential.store.CredentialStore;
import com.bytechef.platform.credential.store.CredentialStoreType;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Default {@link CredentialStore} backed by the entity's inline encrypted payload column. Always registered.
 *
 * @author Ivica Cardic
 */
@Component
public class DatabaseCredentialStore implements CredentialStore {

    @Override
    public CredentialStoreType getType() {
        return CredentialStoreType.DATABASE;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public Map<String, ?> getSecret(CredentialSecret secret) {
        return secret.getPayload();
    }

    @Override
    public void storeSecret(CredentialSecret secret, Map<String, ?> payload) {
        secret.setPayload(payload);
    }

    @Override
    public void deleteSecret(CredentialSecret secret) {
        // No-op: the inline payload is cleared when the row is deleted.
    }
}
```

- [ ] **Step 5: Run test, verify pass**

Run: `./gradlew :server:libs:platform:platform-credential-store:platform-credential-store-service:test`
Expected: PASS.

- [ ] **Step 6: Spotless + commit**

```bash
./gradlew :server:libs:platform:platform-credential-store:platform-credential-store-service:spotlessApply
git add settings.gradle.kts server/libs/platform/platform-credential-store/platform-credential-store-service
git commit -m "547 Add DatabaseCredentialStore default backend

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: Retype connection onto the shared SPI

This task removes the connection-namespaced credential-store types and points connection at the neutral module. It is the largest task; keep it as one commit because the codebase does not compile in the middle.

**Files:**
- Modify: `Connection.java` (implement `CredentialSecret`, retype enum accessors)
- Modify: `ConnectionService.java` (`registerExisting` signature → `CredentialStoreType`)
- Modify: `ConnectionServiceImpl.java` (`List<CredentialStore>`, retyped helpers, `ReadOnlyCredentialStoreException` import)
- Modify: `ConnectionCredentialStoreGraphQlController.java`, `ConnectionCredentialStoreInfo.java` (retype to shared)
- Modify: `connection-credential-store.graphqls` (no schema change; enum maps to shared Java type)
- Modify: `platform-connection-api/build.gradle.kts` (+ `platform-credential-store-api`), `platform-connection-service/build.gradle.kts` (+ `platform-credential-store-service`)
- Delete: `ConnectionCredentialStore.java`, `ConnectionCredentialStoreType.java`, `DatabaseConnectionCredentialStore.java`, `platform-connection-api/.../exception/ReadOnlyCredentialStoreException.java`
- Modify: `ConnectionErrorType.java` (remove `READ_ONLY_CREDENTIAL_STORE`)
- Modify: `ConnectionServiceTest.java`, `ConnectionServiceIntTest.java`, `DatabaseConnectionCredentialStoreTest.java` (delete), `TestExternalConnectionCredentialStore.java`, `ConnectionIntTestConfiguration.java`
- Modify (cross-module consumers of `ConnectionCredentialStoreType` / `registerExisting`, folded in per pre-flight decision):
  - `automation-configuration`: `WorkspaceConnectionFacade.java` (+ `Impl`), `ConnectionGraphQlController.java`, REST `CredentialStoreTypeMapper.java` + `ConnectionApiController.java`, and tests `ConnectionGraphQlControllerIntTest.java`, `ConnectionApiControllerIntTest.java`, `WorkspaceConnectionFacadeAuthorizationTest.java`
  - `embedded-configuration`: REST `CredentialStoreTypeMapper.java`
  - EE remote clients / app stub: `RemoteWorkspaceConnectionFacadeClient.java`, `RemoteConnectionServiceClient.java`, `runtime-job-app/.../connection/service/ConnectionServiceImpl.java`

**Interfaces:**
- Consumes: Task 1 + Task 2 types
- Produces: `Connection implements CredentialSecret`; `ConnectionService.registerExisting(Connection, CredentialStoreType, String)`

- [ ] **Step 1: Add the api dependency**

In `platform-connection-api/build.gradle.kts`, add under the `api(...)` block:

```kotlin
    api(project(":server:libs:platform:platform-credential-store:platform-credential-store-api"))
```

- [ ] **Step 2: Make `Connection` implement `CredentialSecret`**

In `Connection.java`: add `import com.bytechef.platform.credential.store.CredentialSecret;` and `import com.bytechef.platform.credential.store.CredentialStoreType;`, remove `import com.bytechef.platform.connection.service.ConnectionCredentialStoreType;`, change the class declaration to `public final class Connection implements CredentialSecret {`, retype the two accessors, and add the payload bridge:

```java
    @Override
    public CredentialStoreType getCredentialStoreType() {
        return CredentialStoreType.values()[credentialStoreType];
    }

    @Override
    public void setCredentialStoreType(CredentialStoreType credentialStoreType) {
        this.credentialStoreType = credentialStoreType.ordinal();
    }

    @Override
    public Map<String, ?> getPayload() {
        return getParameters();
    }

    @Override
    public void setPayload(Map<String, ?> payload) {
        setParameters(payload);
    }

    @Override
    public @Nullable String getCredentialRef() {
        return credentialRef;
    }

    @Override
    public void setCredentialRef(@Nullable String credentialRef) {
        this.credentialRef = credentialRef;
    }
```

- [ ] **Step 3: Delete the connection-namespaced credential-store types**

```bash
rm server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/service/ConnectionCredentialStore.java
rm server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/service/ConnectionCredentialStoreType.java
rm server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/exception/ReadOnlyCredentialStoreException.java
rm server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/service/DatabaseConnectionCredentialStore.java
rm server/libs/platform/platform-connection/platform-connection-service/src/test/java/com/bytechef/platform/connection/service/DatabaseConnectionCredentialStoreTest.java
```

- [ ] **Step 4: Remove the obsolete error key**

In `ConnectionErrorType.java`, delete the line:

```java
    public static final ConnectionErrorType READ_ONLY_CREDENTIAL_STORE = new ConnectionErrorType(103);
```

- [ ] **Step 5: Retype `ConnectionService.registerExisting`**

In `ConnectionService.java`, replace `import com.bytechef.platform.connection.service.ConnectionCredentialStoreType;`-style references and the method signature with:

```java
    Connection registerExisting(
        Connection connection, com.bytechef.platform.credential.store.CredentialStoreType storeType, String credentialRef);
```

(Use a normal top-of-file import rather than the fully-qualified name; inlined here for clarity.)

- [ ] **Step 6: Add the service dependency**

In `platform-connection-service/build.gradle.kts`, add:

```kotlin
    implementation(project(":server:libs:platform:platform-credential-store:platform-credential-store-service"))
```

- [ ] **Step 7: Retype `ConnectionServiceImpl`**

Replace every `ConnectionCredentialStore` with `CredentialStore`, `ConnectionCredentialStoreType` with `CredentialStoreType`, and the import of `com.bytechef.platform.connection.exception.ReadOnlyCredentialStoreException` with `com.bytechef.platform.credential.store.exception.ReadOnlyCredentialStoreException`. The store method calls rename: `getParameters(connection)`→`getSecret(connection)`, `storeParameters(connection, params)`→`storeSecret(connection, params)`, `deleteParameters(connection)`→`deleteSecret(connection)`. Field, constructor, and helpers become:

```java
    private final List<CredentialStore> credentialStores;
    private final ConnectionRepository connectionRepository;

    @SuppressFBWarnings("EI2")
    public ConnectionServiceImpl(List<CredentialStore> credentialStores, ConnectionRepository connectionRepository) {
        this.credentialStores = credentialStores;
        this.connectionRepository = connectionRepository;
    }

    private CredentialStore getStore(CredentialStoreType type) {
        return credentialStores.stream()
            .filter(store -> store.getType() == type)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "No CredentialStore registered for type %s.".formatted(type)));
    }

    private Connection populateParameters(Connection connection) {
        CredentialStore store = getStore(connection.getCredentialStoreType());

        connection.setParameters(store.getSecret(connection));

        return connection;
    }
```

Update `create`, `delete`, `updateConnectionParameters`, and `registerExisting` to call `getSecret`/`storeSecret`/`deleteSecret` and to use the shared `CredentialStoreType`.

- [ ] **Step 8: Retype the GraphQL controller + DTO**

`ConnectionCredentialStoreInfo.java`:

```java
package com.bytechef.platform.connection.web.graphql.dto;

import com.bytechef.platform.credential.store.CredentialStoreType;

public record ConnectionCredentialStoreInfo(CredentialStoreType type, boolean readOnly) {
}
```

`ConnectionCredentialStoreGraphQlController.java`: change the injected field/constructor to `List<CredentialStore> credentialStores` (import `com.bytechef.platform.credential.store.CredentialStore`); the `connectionCredentialStores()` body is otherwise unchanged. Add `platform-credential-store-api` to `platform-connection-graphql/build.gradle.kts` if not transitively present.

> The `.graphqls` keeps `enum ConnectionCredentialStoreType { DATABASE, AWS_SECRETS_MANAGER, HASHICORP_VAULT }` — GraphQL maps it positionally/by-name onto the shared Java enum. No client codegen change.

- [ ] **Step 9: Retype connection tests + test doubles**

- Delete `DatabaseConnectionCredentialStoreTest.java` (covered by `DatabaseCredentialStoreTest`).
- `TestExternalConnectionCredentialStore.java`: implement `CredentialStore` over `CredentialSecret` instead of `ConnectionCredentialStore`/`Connection`.
- `ConnectionIntTestConfiguration.java` / `ConnectionServiceIntTest.java` / `ConnectionServiceTest.java`: retype references to the shared types; the DATABASE store bean is now `DatabaseCredentialStore`.

- [ ] **Step 9b: Retype every cross-module consumer of the enum / `registerExisting`**

The shared `CredentialStoreType` replaces `ConnectionCredentialStoreType` everywhere. Find them with:

```bash
git grep -nl 'ConnectionCredentialStoreType' -- ':!*/build/*' ':!*/generated/*' ':!docs/*'
```

In each hit, replace the import `com.bytechef.platform.connection.service.ConnectionCredentialStoreType` with `com.bytechef.platform.credential.store.CredentialStoreType` and substitute the type name. Specifically:

- `WorkspaceConnectionFacade.java` / `WorkspaceConnectionFacadeImpl.java`: `registerExisting(..., ConnectionCredentialStoreType storeType, ...)` → `CredentialStoreType storeType`.
- `RemoteWorkspaceConnectionFacadeClient.java`, `RemoteConnectionServiceClient.java`, `runtime-job-app/.../connection/service/ConnectionServiceImpl.java`: same parameter retype on their `registerExisting` overrides.
- `ConnectionGraphQlController.java` and `ConnectionApiController.java`: wherever they build the `CredentialStoreType` argument (via the mapper), retype.
- Both REST `CredentialStoreTypeMapper.java` (automation + embedded): swap the import, rename method `mapToConnectionCredentialStoreType` → `mapToCredentialStoreType`, and update the corresponding `*MapperSpringConfig` `uses = {...}` reference if the method rename requires it (it does not change the class name, so the `uses` entry is unchanged).
- Tests `ConnectionGraphQlControllerIntTest.java`, `ConnectionApiControllerIntTest.java`, `WorkspaceConnectionFacadeAuthorizationTest.java`, `ConnectionServiceIntTest.java`: retype references.

Generated OpenAPI models (`RegisterExistingConnectionRequestModel`, `CredentialStoreTypeEnum`) are NOT touched — they are a separate generated enum the mapper bridges.

- [ ] **Step 10: Build connection + dependent modules + tests**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-service:test :server:libs:automation:automation-configuration:automation-configuration-service:compileJava :server:libs:automation:automation-configuration:automation-configuration-graphql:compileJava :server:libs:automation:automation-configuration:automation-configuration-rest:automation-configuration-rest-api:compileJava :server:ee:libs:embedded:embedded-configuration:embedded-configuration-rest:embedded-configuration-rest-api:compileJava :server:ee:apps:runtime-job-app:compileJava`
Expected: BUILD SUCCESSFUL; connection service unit tests pass.

- [ ] **Step 11: Static analysis + Spotless**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-api:spotlessApply :server:libs:platform:platform-connection:platform-connection-service:spotlessApply :server:libs:platform:platform-connection:platform-connection-api:checkstyleMain :server:libs:platform:platform-connection:platform-connection-service:checkstyleMain`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 12: Commit**

```bash
git add server/libs/platform/platform-connection \
  server/libs/automation/automation-configuration \
  server/ee/libs/embedded/embedded-configuration \
  server/ee/libs/automation/automation-configuration/automation-configuration-remote-client \
  server/ee/libs/platform/platform-connection/platform-connection-remote-client \
  server/ee/apps/runtime-job-app
git commit -m "547 Retype connection credential store onto shared CredentialStore SPI

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: Retype property onto the shared SPI + route-to-external

**Files:**
- Modify: `Property.java` (implement `CredentialSecret`, retype enum accessors, add payload bridge)
- Modify: `PropertyServiceImpl.java` (`List<CredentialStore>`, `resolveTargetStore()`, re-save migration)
- Modify: `platform-configuration-api/build.gradle.kts` (+ `platform-credential-store-api`), `platform-configuration-service/build.gradle.kts` (+ `platform-credential-store-service`)
- Delete (first pass): `PropertyCredentialStore.java`, `PropertyCredentialStoreType.java`, `DatabasePropertyCredentialStore.java`, `PropertyErrorType.java`, configuration `ReadOnlyCredentialStoreException.java`
- Test: `.../platform-configuration-service/src/test/java/com/bytechef/platform/configuration/service/PropertyServiceImplTest.java`

**Interfaces:**
- Consumes: Task 1 + Task 2 types
- Produces: `Property implements CredentialSecret`; `PropertyServiceImpl.resolveTargetStore(): CredentialStore`

- [ ] **Step 1: Delete the first-pass property credential-store types**

```bash
rm server/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/platform/configuration/service/PropertyCredentialStore.java
rm server/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/platform/configuration/service/PropertyCredentialStoreType.java
rm server/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/platform/configuration/exception/PropertyErrorType.java
rm server/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/platform/configuration/exception/ReadOnlyCredentialStoreException.java
rm server/libs/platform/platform-configuration/platform-configuration-service/src/main/java/com/bytechef/platform/configuration/service/DatabasePropertyCredentialStore.java
```

- [ ] **Step 2: Add the api dependency + make `Property` implement `CredentialSecret`**

In `platform-configuration-api/build.gradle.kts` add `implementation(project(":server:libs:platform:platform-credential-store:platform-credential-store-api"))`. In `Property.java`: replace `import com.bytechef.platform.configuration.service.PropertyCredentialStoreType;` with `import com.bytechef.platform.credential.store.CredentialSecret;` and `import com.bytechef.platform.credential.store.CredentialStoreType;`; change declaration to `public class Property implements CredentialSecret {`; retype the two accessors and add the bridge:

```java
    @Override
    public CredentialStoreType getCredentialStoreType() {
        return CredentialStoreType.values()[credentialStoreType];
    }

    @Override
    public void setCredentialStoreType(CredentialStoreType credentialStoreType) {
        this.credentialStoreType = credentialStoreType.ordinal();
    }

    @Override
    public @Nullable String getCredentialRef() {
        return credentialRef;
    }

    @Override
    public void setCredentialRef(@Nullable String credentialRef) {
        this.credentialRef = credentialRef;
    }

    @Override
    public Map<String, ?> getPayload() {
        return getValue();
    }

    @Override
    public void setPayload(Map<String, ?> payload) {
        setValue(payload);
    }
```

- [ ] **Step 3: Write the failing `PropertyServiceImplTest`**

```java
/* Apache header */
package com.bytechef.platform.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.repository.PropertyRepository;
import com.bytechef.platform.credential.store.CredentialSecret;
import com.bytechef.platform.credential.store.CredentialStore;
import com.bytechef.platform.credential.store.CredentialStoreType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PropertyServiceImplTest {

    private final CredentialStore databaseStore = stubStore(CredentialStoreType.DATABASE, false);
    private final PropertyRepository propertyRepository = Mockito.mock(PropertyRepository.class);

    @Test
    void testSaveRoutesToExternalStoreWhenRegistered() {
        CredentialStore externalStore = stubStore(CredentialStoreType.AWS_SECRETS_MANAGER, false);
        PropertyServiceImpl propertyService = new PropertyServiceImpl(
            List.of(databaseStore, externalStore), propertyRepository);

        when(propertyRepository.findByKeyAndScope("k", Property.Scope.PLATFORM.ordinal()))
            .thenReturn(Optional.empty());
        when(propertyRepository.save(any(Property.class))).thenAnswer(invocation -> invocation.getArgument(0));

        propertyService.save("k", Map.of("apiKey", "v"), Property.Scope.PLATFORM, null);

        verify(externalStore).storeSecret(any(CredentialSecret.class), any());
    }

    @Test
    void testSaveFallsBackToDatabaseWhenNoExternal() {
        PropertyServiceImpl propertyService = new PropertyServiceImpl(List.of(databaseStore), propertyRepository);

        when(propertyRepository.findByKeyAndScope("k", Property.Scope.PLATFORM.ordinal()))
            .thenReturn(Optional.empty());
        when(propertyRepository.save(any(Property.class))).thenAnswer(invocation -> invocation.getArgument(0));

        propertyService.save("k", Map.of("apiKey", "v"), Property.Scope.PLATFORM, null);

        verify(databaseStore).storeSecret(any(CredentialSecret.class), any());
    }

    private static CredentialStore stubStore(CredentialStoreType type, boolean readOnly) {
        CredentialStore store = Mockito.mock(CredentialStore.class);

        when(store.getType()).thenReturn(type);
        when(store.isReadOnly()).thenReturn(readOnly);
        when(store.getSecret(any())).thenReturn(Map.of());

        return store;
    }
}
```

- [ ] **Step 4: Run test, verify it fails**

Run: `./gradlew :server:libs:platform:platform-configuration:platform-configuration-service:test --tests '*PropertyServiceImplTest'`
Expected: FAIL — constructor still takes `List<PropertyCredentialStore>`.

- [ ] **Step 5: Rewrite `PropertyServiceImpl` onto the shared SPI**

Constructor → `List<CredentialStore> credentialStores`; import `com.bytechef.platform.credential.store.*` and `com.bytechef.platform.credential.store.exception.ReadOnlyCredentialStoreException`. Replace `getStore`/`populateValue`/`storeValue` to use the shared types, add `resolveTargetStore`, and route `save` through the target with re-save migration:

```java
    private CredentialStore resolveTargetStore() {
        return credentialStores.stream()
            .filter(store -> store.getType() != CredentialStoreType.DATABASE)
            .findFirst()
            .orElseGet(() -> getStore(CredentialStoreType.DATABASE));
    }

    private void storeValue(Property property, Map<String, ?> value) {
        CredentialStore target = resolveTargetStore();

        if (target.isReadOnly()) {
            throw new ReadOnlyCredentialStoreException(target.getType());
        }

        CredentialStore current = getStore(property.getCredentialStoreType());

        if (current.getType() != target.getType() && !current.isReadOnly()) {
            current.deleteSecret(property);
        }

        property.setCredentialStoreType(target.getType());
        property.setCredentialRef(null);

        target.storeSecret(property, value);
    }

    private CredentialStore getStore(CredentialStoreType type) {
        return credentialStores.stream()
            .filter(store -> store.getType() == type)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "No CredentialStore registered for type %s.".formatted(type)));
    }

    private Property populateValue(Property property) {
        CredentialStore store = getStore(property.getCredentialStoreType());

        property.setValue(store.getSecret(property));

        return property;
    }
```

`save(...)` present-and-absent branches both call `storeValue(property, value)` then `propertyRepository.save(property)` (the absent branch first sets enabled/key/scope/scopeId/environment as today). `delete(...)` resolves `getStore(property.getCredentialStoreType())` and calls `deleteSecret` when not read-only. Add `platform-credential-store-service` to `platform-configuration-service/build.gradle.kts`.

- [ ] **Step 6: Run test, verify pass**

Run: `./gradlew :server:libs:platform:platform-configuration:platform-configuration-service:test --tests '*PropertyServiceImplTest'`
Expected: PASS (both tests).

- [ ] **Step 7: Build + static analysis**

Run: `./gradlew :server:libs:platform:platform-configuration:platform-configuration-api:compileJava :server:libs:platform:platform-configuration:platform-configuration-service:compileTestJava :server:libs:platform:platform-configuration:platform-configuration-service:checkstyleMain :server:libs:platform:platform-configuration:platform-configuration-service:spotbugsMain`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Spotless + commit**

```bash
./gradlew :server:libs:platform:platform-configuration:platform-configuration-api:spotlessApply :server:libs:platform:platform-configuration:platform-configuration-service:spotlessApply
git add server/libs/platform/platform-configuration
git commit -m "547 Route property credentials through shared CredentialStore SPI

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 5: Unify the config namespace to `bytechef.credential-store.*`

**Files:**
- Modify: `server/libs/config/app-config/.../config/ApplicationProperties.java` (move `Connection.CredentialStore` → top-level `CredentialStore`)
- Modify: `server/apps/server-app/src/main/resources/config/application-bytechef.yml` (any `bytechef.connection.credential-store.*` keys → `bytechef.credential-store.*`)

> The REST `CredentialStoreTypeMapper` retype was folded into Task 3 (pre-flight decision) — Task 5 is config-only.

**Interfaces:**
- Produces: `ApplicationProperties.getCredentialStore(): CredentialStore` with nested `External`, `Cache`, `AwsSecretsManager`, `HashiCorpVault`, `AppRole` (fields verbatim from predecessor)

- [ ] **Step 1: Move the config tree to top level**

In `ApplicationProperties.java`, remove the `Connection` wrapper class's `credentialStore` field and getter; introduce a top-level `private CredentialStore credentialStore = new CredentialStore();` with `getCredentialStore()`/`setCredentialStore(...)`, and lift the nested `CredentialStore` (with `External`, `Cache`, `AwsSecretsManager`, `HashiCorpVault`, `AppRole`) to a top-level static class. Keep every field name identical so relaxed binding is unchanged.

- [ ] **Step 2: Update YAML keys**

In `application-bytechef.yml`, rename any `bytechef.connection.credential-store.*` block to `bytechef.credential-store.*` (verify with `grep -n 'credential-store' server/apps/server-app/src/main/resources/config/application-bytechef.yml`).

- [ ] **Step 3: Build app-config**

Run: `./gradlew :server:libs:config:app-config:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Spotless + commit**

```bash
./gradlew :server:libs:config:app-config:spotlessApply
git add server/libs/config/app-config \
  server/apps/server-app/src/main/resources/config/application-bytechef.yml
git commit -m "547 Unify credential-store config under bytechef.credential-store

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 6: Relocate + retype the EE adapters

**Files:**
- Move dir: `server/ee/libs/platform/platform-connection/platform-connection-credential-store-aws-secrets-manager` → `server/ee/libs/platform/platform-credential-store/credential-store-aws-secrets-manager`
- Move dir: `…-hashicorp-vault` → `server/ee/libs/platform/platform-credential-store/credential-store-hashicorp-vault`
- Modify: `settings.gradle.kts` (update the two `include(...)` paths)
- Modify: each adapter `.java` (retype `Connection`→`CredentialSecret`, config getter, prefixes), `build.gradle.kts` (dep `platform-connection-api`→`platform-credential-store-api`), int tests + test configs

**Interfaces:**
- Consumes: Task 1 types + Task 5 config
- Produces: `AwsSecretsManagerCredentialStore`, `HashiCorpVaultCredentialStore` implementing `CredentialStore`

> **Carryover from Task 3 (verified):** Task 3 already retyped both adapters' class declarations to `implements CredentialStore` and their method signatures to `CredentialSecret` (forced by deleting `ConnectionCredentialStore`). BUT the two adapter modules currently **do not compile** because they still `import com.bytechef.platform.connection.util.CredentialPathResolver` (Task 1 moved it to `com.bytechef.platform.credential.store.CredentialPathResolver`) and still depend on `platform-connection-api` for it. As part of this task you MUST: fix that import in both adapters, and switch each `build.gradle.kts` dependency from `platform-connection-api` to `platform-credential-store-api` (the adapters no longer need anything from connection). Verify the adapters reference `secret.getPayload()/setPayload()` correctly. The relocation (`git mv`), class rename (`…ConnectionCredentialStore`→`…CredentialStore`), config getter (`getConnection().getCredentialStore()`→`getCredentialStore()`, available after Task 5), and `@ConditionalOnProperty`/EnvironmentPostProcessor prefix repoint are all still TODO here.

- [ ] **Step 1: Move the module directories with git**

```bash
mkdir -p server/ee/libs/platform/platform-credential-store
git mv server/ee/libs/platform/platform-connection/platform-connection-credential-store-aws-secrets-manager \
  server/ee/libs/platform/platform-credential-store/credential-store-aws-secrets-manager
git mv server/ee/libs/platform/platform-connection/platform-connection-credential-store-hashicorp-vault \
  server/ee/libs/platform/platform-credential-store/credential-store-hashicorp-vault
```

- [ ] **Step 2: Update settings.gradle.kts includes**

Replace lines 737–738:

```kotlin
include("server:ee:libs:platform:platform-credential-store:credential-store-aws-secrets-manager")
include("server:ee:libs:platform:platform-credential-store:credential-store-hashicorp-vault")
```

- [ ] **Step 3: Retype the AWS adapter**

In `AwsSecretsManagerConnectionCredentialStore.java` (rename file + class to `AwsSecretsManagerCredentialStore`): change `implements ConnectionCredentialStore`→`implements CredentialStore`; imports `com.bytechef.platform.connection.*`→`com.bytechef.platform.credential.store.*`; method signatures `getParameters(Connection)`→`getSecret(CredentialSecret)`, `storeParameters(Connection, Map)`→`storeSecret(CredentialSecret, Map)`, `deleteParameters(Connection)`→`deleteSecret(CredentialSecret)`; entity calls `connection.getCredentialRef()`→`secret.getCredentialRef()`, `connection.setCredentialRef(ref)`→`secret.setCredentialRef(ref)`, `connection.setParameters(Map.of())`→`secret.setPayload(Map.of())`; config read `applicationProperties.getConnection().getCredentialStore()`→`applicationProperties.getCredentialStore()`; `getType()` returns `CredentialStoreType.AWS_SECRETS_MANAGER`. Keep the `@version ee` Javadoc + Enterprise header.

- [ ] **Step 4: Retype the AWS configuration + post-processor**

In `AwsSecretsManagerCredentialStoreConfiguration.java`: `@ConditionalOnProperty(prefix = "bytechef.credential-store.external", name = "provider", havingValue = "aws-secrets-manager")`; bean type/name → `AwsSecretsManagerCredentialStore`. In `AwsSecretsManagerCredentialStoreEnvironmentPostProcessor.java`: read `bytechef.credential-store.external.provider`.

- [ ] **Step 5: Retype the AWS build + tests**

`build.gradle.kts`: replace `project(":server:libs:platform:platform-connection:platform-connection-api")` with `project(":server:libs:platform:platform-credential-store:platform-credential-store-api")`. Int test + test config: retype to `CredentialStore` / `CredentialSecret`, using a `Connection` (or a minimal test `CredentialSecret`) as the fixture; update the package-scan in the test config.

- [ ] **Step 6: Repeat Steps 3–5 for the Vault adapter**

Same substitutions in `HashiCorpVaultConnectionCredentialStore.java` → `HashiCorpVaultCredentialStore` (config getter `getHashicorpVault()` now off `applicationProperties.getCredentialStore()`; `getType()` → `HASHICORP_VAULT`; default template unchanged), its configuration, post-processor (`bytechef.credential-store.external.provider`), `build.gradle.kts`, and int test/config.

- [ ] **Step 7: Build both EE modules**

Run: `./gradlew :server:ee:libs:platform:platform-credential-store:credential-store-aws-secrets-manager:compileJava :server:ee:libs:platform:platform-credential-store:credential-store-hashicorp-vault:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Spotless (EE header check) + commit**

```bash
./gradlew :server:ee:libs:platform:platform-credential-store:credential-store-aws-secrets-manager:spotlessApply :server:ee:libs:platform:platform-credential-store:credential-store-hashicorp-vault:spotlessApply
git add settings.gradle.kts server/ee/libs/platform/platform-credential-store \
  server/ee/libs/platform/platform-connection
git commit -m "547 Relocate and generalize EE credential-store adapters

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 7: Full-tree verification

**Files:** none (verification only)

- [ ] **Step 1: Compile the whole server tree**

Run: `./gradlew compileJava compileTestJava`
Expected: BUILD SUCCESSFUL. Fix any stragglers that still import the deleted connection/property credential-store types (grep below).

- [ ] **Step 2: Grep for stale references**

Run:
```bash
git grep -n 'ConnectionCredentialStore\|PropertyCredentialStore\|connection.credential-store\|PropertyCredentialStoreType' -- ':!docs/*'
```
Expected: no matches outside docs. Fix any found.

- [ ] **Step 3: Run the affected module tests + integration tests**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-service:test :server:libs:platform:platform-configuration:platform-configuration-service:test :server:libs:platform:platform-credential-store:platform-credential-store-service:test`
Then (Docker required): `./gradlew :server:libs:platform:platform-connection:platform-connection-service:testIntegration`
Expected: all green.

- [ ] **Step 4: Final commit (if Step 1–2 required fixes)**

```bash
git add -A
git commit -m "547 Fix residual references after credential-store generalization

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Self-Review

**Spec coverage:**
- §1 neutral module → Tasks 1–2. §2 `CredentialSecret` → Tasks 1, 3, 4. §3 SPI + stores → Tasks 1, 2, 6. §4 service wiring → Tasks 3, 4. §5 config → Task 5. §6 client surface → Tasks 3 (GraphQL), 5 (REST). §7 migration/ordinals/re-save → Tasks 1 (ordinals), 4 (re-save). §8 testing → Tasks 2, 4, 6, 7. All sections covered.

**Placeholder scan:** No "TBD/handle edge cases/similar to Task N". Mechanical retypes spell out the exact substitutions and show the changed code.

**Type consistency:** `CredentialStore` / `CredentialSecret` / `CredentialStoreType` used identically across tasks; method names `getSecret`/`storeSecret`/`deleteSecret` consistent; `resolveTargetStore()` defined once (Task 4) and not referenced elsewhere; `getPayload`/`setPayload` consistent between seam (Task 1), `Connection` (Task 3), `Property` (Task 4), and `DatabaseCredentialStore` (Task 2).

**Known compile-ordering note:** Task 1 moves `CredentialPathResolver` out of `platform-connection-api`; the whole-tree compile is only asserted at Task 7. Per-task builds target the modules each task changes. If strict green-at-every-task is desired, run `./gradlew compileJava` only at Task 7 (as written) and treat intermediate per-module builds as the gate.
