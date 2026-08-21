# Connection Credential Store PR 2 — GraphQL info query

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expose the list of currently-registered `ConnectionCredentialStore` beans via GraphQL so the UI (sibling frontend spec) can render a store-picker on connection creation and show a read-only badge when applicable.

**Architecture:** New `platform-connection-graphql` module (mirroring `platform-mcp-graphql`). Single `@QueryMapping connectionCredentialStores` that iterates the injected `List<ConnectionCredentialStore>` and returns `(type, readOnly)` tuples. Pure read-only — no mutations.

**Tech Stack:** Spring GraphQL, Java 25. New module: `server/libs/platform/platform-connection/platform-connection-graphql/`.

**Spec:** [docs/superpowers/specs/2026-05-19-connection-credential-store-design.md](../specs/2026-05-19-connection-credential-store-design.md) (Section "GraphQL info query")

**Issue:** [bytechefhq/bytechef#547](https://github.com/bytechefhq/bytechef/issues/547)

**Branch state at PR-2 start:** Feature branch `claude/amazing-brahmagupta-12e13d` at commit `089f60ed024` (PR-1 spotless commit), with PR-1 already merged into the same branch. PR-2 lands additional commits on the same branch.

---

## Branch hygiene — applies to every task

Every subagent dispatch MUST:
1. Run `git branch --show-current` and confirm `claude/amazing-brahmagupta-12e13d` before any change.
2. NEVER run `git checkout`, `git pull`, `git rebase`, or `git fetch`.
3. If anything seems wrong with the branch state, STOP and report — don't try to fix with git operations.

---

## File Structure

**New module:** `server/libs/platform/platform-connection/platform-connection-graphql/`

**New files:**
| File | Responsibility |
|---|---|
| `server/libs/platform/platform-connection/platform-connection-graphql/build.gradle.kts` | Module deps (spring-graphql + platform-connection-api) |
| `server/libs/platform/platform-connection/platform-connection-graphql/src/main/resources/graphql/connection-credential-store.graphqls` | Schema fragment: `extend type Query`, `ConnectionCredentialStoreInfo`, `ConnectionCredentialStoreType` |
| `server/libs/platform/platform-connection/platform-connection-graphql/src/main/java/com/bytechef/platform/connection/web/graphql/ConnectionCredentialStoreGraphQlController.java` | `@Controller` with one `@QueryMapping` method |
| `server/libs/platform/platform-connection/platform-connection-graphql/src/main/java/com/bytechef/platform/connection/web/graphql/dto/ConnectionCredentialStoreInfo.java` | DTO record returned by the resolver |
| `client/src/graphql/platform/connection/connectionCredentialStores.graphql` | Client operation file |

**Modified files:**
| File | Change |
|---|---|
| `settings.gradle.kts` | Include `:server:libs:platform:platform-connection:platform-connection-graphql` |
| `client/codegen.ts` | Add the new schema path to the `schema` array |
| `client/src/shared/middleware/graphql.ts` | **Regenerated** by `npx graphql-codegen` — committed separately per project convention |

---

### Task 1: Scaffold platform-connection-graphql module

- [ ] **Step 1: Verify branch state**

```bash
git branch --show-current  # → claude/amazing-brahmagupta-12e13d
git log --oneline -1       # top is 089f60ed024 547 Apply spotless formatting
```

- [ ] **Step 2: Create the module directory and build.gradle.kts**

```bash
mkdir -p server/libs/platform/platform-connection/platform-connection-graphql/src/main/java/com/bytechef/platform/connection/web/graphql/dto
mkdir -p server/libs/platform/platform-connection/platform-connection-graphql/src/main/resources/graphql
mkdir -p server/libs/platform/platform-connection/platform-connection-graphql/src/test/java/com/bytechef/platform/connection/web/graphql
```

Create `server/libs/platform/platform-connection/platform-connection-graphql/build.gradle.kts`:

```kotlin
dependencies {
    implementation("org.springframework:spring-web")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.graphql:spring-graphql")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-connection:platform-connection-api"))

    testImplementation("org.springframework:spring-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-graphql-test")
    testImplementation(project(":server:libs:test:test-int-support"))
}
```

- [ ] **Step 3: Register the module in settings.gradle.kts**

Open `settings.gradle.kts` and find the existing `include("server:libs:platform:platform-connection:platform-connection-api")` line (around line 162). Insert a new line directly below it:

```kotlin
include("server:libs:platform:platform-connection:platform-connection-graphql")
```

Maintain alphabetical-within-platform-connection ordering: api, graphql, service.

- [ ] **Step 4: Verify the new module is recognized by Gradle**

```bash
./gradlew :server:libs:platform:platform-connection:platform-connection-graphql:tasks 2>&1 | tail -5
```

Expected: lists Gradle tasks (the module is recognized). If the module isn't found, the include line is wrong.

(No commit yet — Tasks 2–3 add content; commit at end of Task 3.)

---

### Task 2: Add the GraphQL schema fragment

**File (create):** `server/libs/platform/platform-connection/platform-connection-graphql/src/main/resources/graphql/connection-credential-store.graphqls`

- [ ] **Step 1: Write the schema fragment**

```graphql
extend type Query {
    connectionCredentialStores: [ConnectionCredentialStoreInfo!]!
}

type ConnectionCredentialStoreInfo {
    type: ConnectionCredentialStoreType!
    readOnly: Boolean!
}

enum ConnectionCredentialStoreType {
    DATABASE
    AWS_SECRETS_MANAGER
    HASHICORP_VAULT
}
```

This uses `extend type Query` — Spring GraphQL composes this with the base placeholder declared in `platform-configuration-graphql/src/main/resources/graphql/base.graphqls`. No other schema changes needed.

- [ ] **Step 2: No commit yet**

Commit at end of Task 3 once the resolver compiles against the schema.

---

### Task 3: Add the resolver + DTO

**Files (create):**
- `server/libs/platform/platform-connection/platform-connection-graphql/src/main/java/com/bytechef/platform/connection/web/graphql/dto/ConnectionCredentialStoreInfo.java`
- `server/libs/platform/platform-connection/platform-connection-graphql/src/main/java/com/bytechef/platform/connection/web/graphql/ConnectionCredentialStoreGraphQlController.java`

- [ ] **Step 1: Create the DTO record**

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

package com.bytechef.platform.connection.web.graphql.dto;

import com.bytechef.platform.connection.service.ConnectionCredentialStoreType;

/**
 * DTO returned by the {@code connectionCredentialStores} GraphQL query.
 *
 * @author Ivica Cardic
 */
public record ConnectionCredentialStoreInfo(ConnectionCredentialStoreType type, boolean readOnly) {
}
```

- [ ] **Step 2: Create the resolver**

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

package com.bytechef.platform.connection.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.connection.service.ConnectionCredentialStore;
import com.bytechef.platform.connection.web.graphql.dto.ConnectionCredentialStoreInfo;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller exposing which {@link ConnectionCredentialStore} backends are registered in the current
 * deployment. Consumed by the UI to render the store picker on connection creation forms.
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class ConnectionCredentialStoreGraphQlController {

    private final List<ConnectionCredentialStore> connectionCredentialStores;

    @SuppressFBWarnings("EI2")
    public ConnectionCredentialStoreGraphQlController(List<ConnectionCredentialStore> connectionCredentialStores) {
        this.connectionCredentialStores = connectionCredentialStores;
    }

    @QueryMapping
    public List<ConnectionCredentialStoreInfo> connectionCredentialStores() {
        return connectionCredentialStores.stream()
            .map(store -> new ConnectionCredentialStoreInfo(store.getType(), store.isReadOnly()))
            .toList();
    }
}
```

- [ ] **Step 3: Compile**

```bash
./gradlew :server:libs:platform:platform-connection:platform-connection-graphql:compileJava
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run module check (no tests yet — they're added in Task 5)**

```bash
./gradlew :server:libs:platform:platform-connection:platform-connection-graphql:check
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit Tasks 1–3 together**

```bash
git add \
  server/libs/platform/platform-connection/platform-connection-graphql/ \
  settings.gradle.kts
git commit -m "$(cat <<'EOF'
547 Add platform-connection-graphql module with credential-store query

Spring GraphQL @QueryMapping for connectionCredentialStores returning
the list of currently-registered ConnectionCredentialStore beans. UI
will consume this to render the per-connection store picker and the
read-only badge.

Module mirrors the platform-mcp-graphql layout. The resolver is gated
by @ConditionalOnCoordinator, consistent with other GraphQL controllers
in the codebase.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 4: Wire codegen.ts and create the client operation

**Files:**
- Modify: `client/codegen.ts`
- Create: `client/src/graphql/platform/connection/connectionCredentialStores.graphql`

- [ ] **Step 1: Read the existing codegen.ts schema array**

```bash
grep -nE "platform-(mcp|configuration|connection)-graphql|schema" client/codegen.ts | head -20
```

Identify the schema array. The existing pattern includes paths like:
```
'../server/libs/platform/platform-configuration/platform-configuration-graphql/src/main/resources/graphql/*.graphqls',
'../server/libs/platform/platform-mcp/platform-mcp-graphql/src/main/resources/graphql/*.graphqls',
```

- [ ] **Step 2: Add the new path**

Append (preserving the alphabetical-by-domain order):
```
'../server/libs/platform/platform-connection/platform-connection-graphql/src/main/resources/graphql/*.graphqls',
```

The exact placement depends on the existing ordering — insert so that the array stays sorted by domain (`configuration` < `connection` < `mcp`). If existing entries aren't strictly sorted, place near other platform-connection-related entries or at the natural insertion point.

- [ ] **Step 3: Create the client operation file**

```bash
mkdir -p client/src/graphql/platform/connection
```

Create `client/src/graphql/platform/connection/connectionCredentialStores.graphql`:

```graphql
query ConnectionCredentialStores {
    connectionCredentialStores {
        type
        readOnly
    }
}
```

- [ ] **Step 4: Run graphql-codegen**

```bash
cd client && npx graphql-codegen
cd ..
```

Expected: regenerates `client/src/shared/middleware/graphql.ts` to include the new `useConnectionCredentialStoresQuery` hook + `ConnectionCredentialStoreType` enum + `ConnectionCredentialStoreInfo` type. No errors.

If codegen fails with "schema not found" or similar, check that the path in Step 2 is correct (relative to the `client/` directory).

- [ ] **Step 5: Run client check**

```bash
cd client && npm run check
cd ..
```

Expected: lint + typecheck + tests all pass. The `npm run check` runs `lint && typecheck && test`. Per CLAUDE.md convention, this MUST pass before committing client-side changes.

- [ ] **Step 6: Commit the operation + codegen.ts as one logical change; commit regenerated middleware separately**

```bash
git add \
  client/codegen.ts \
  client/src/graphql/platform/connection/connectionCredentialStores.graphql
git commit -m "$(cat <<'EOF'
547 client - Add connectionCredentialStores GraphQL operation

Wires the credential-store info query into the client codegen pipeline.
Subsequent commit regenerates src/shared/middleware/graphql.ts.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

Then commit the regenerated middleware:

```bash
git add client/src/shared/middleware/graphql.ts
git commit -m "$(cat <<'EOF'
547 client - Regenerate graphql.ts middleware

Includes useConnectionCredentialStoresQuery hook and
ConnectionCredentialStoreInfo / ConnectionCredentialStoreType types.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

If `npx graphql-codegen` produced changes to other files (e.g., it regenerated `graphql.ts` AND something else), include them in this second commit and note in the report.

---

### Task 5: Integration test for the resolver

**File (create):** `server/libs/platform/platform-connection/platform-connection-graphql/src/test/java/com/bytechef/platform/connection/web/graphql/ConnectionCredentialStoreGraphQlControllerIntTest.java`

- [ ] **Step 1: Inspect an existing GraphQL integration test for the project's convention**

```bash
find server/libs -name "*GraphQlControllerIntTest.java" -type f | head -5
```

Open one (e.g., `McpServerGraphQlControllerIntTest.java` if it exists, or any other `*GraphQlControllerIntTest.java`) and note:
- Whether it uses `@GraphQlTest` or `@SpringBootTest` + `HttpGraphQlTester`
- How the test config is wired
- How dependencies are mocked

If no such test exists in the codebase, fall back to the standard Spring GraphQL test pattern.

- [ ] **Step 2: Write the integration test**

Adapt the following to match the convention you observed in Step 1. If the convention uses `@GraphQlTest`, use that; otherwise use `@SpringBootTest` with `HttpGraphQlTester`.

A minimal `@GraphQlTest` example:

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

package com.bytechef.platform.connection.web.graphql;

import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionCredentialStore;
import com.bytechef.platform.connection.service.ConnectionCredentialStoreType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;

/**
 * @author Ivica Cardic
 */
@GraphQlTest(ConnectionCredentialStoreGraphQlController.class)
@Import(ConnectionCredentialStoreGraphQlControllerIntTest.TestStoresConfiguration.class)
class ConnectionCredentialStoreGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    void testConnectionCredentialStoresReturnsRegisteredStores() {
        graphQlTester.document("""
                query {
                    connectionCredentialStores {
                        type
                        readOnly
                    }
                }
                """)
            .execute()
            .path("connectionCredentialStores")
            .entityList(Object.class)
            .hasSize(2);

        graphQlTester.document("""
                query {
                    connectionCredentialStores {
                        type
                        readOnly
                    }
                }
                """)
            .execute()
            .path("connectionCredentialStores[0].type").entity(String.class).isEqualTo("DATABASE")
            .path("connectionCredentialStores[0].readOnly").entity(Boolean.class).isEqualTo(false)
            .path("connectionCredentialStores[1].type").entity(String.class).isEqualTo("HASHICORP_VAULT")
            .path("connectionCredentialStores[1].readOnly").entity(Boolean.class).isEqualTo(true);
    }

    @TestConfiguration
    static class TestStoresConfiguration {

        @Bean
        ConnectionCredentialStore databaseStub() {
            return new StubStore(ConnectionCredentialStoreType.DATABASE, false);
        }

        @Bean
        ConnectionCredentialStore vaultStub() {
            return new StubStore(ConnectionCredentialStoreType.HASHICORP_VAULT, true);
        }
    }

    private record StubStore(ConnectionCredentialStoreType type, boolean readOnly)
        implements ConnectionCredentialStore {

        @Override
        public ConnectionCredentialStoreType getType() {
            return type;
        }

        @Override
        public boolean isReadOnly() {
            return readOnly;
        }

        @Override
        public Map<String, ?> getParameters(Connection connection) {
            return Map.of();
        }

        @Override
        public void storeParameters(Connection connection, Map<String, ?> parameters) {
            // no-op for test
        }

        @Override
        public void deleteParameters(Connection connection) {
            // no-op for test
        }
    }
}
```

**Adapt** to the convention from Step 1 if it differs. The test class name MUST end with `IntTest` (project convention — integration tests).

- [ ] **Step 3: Run the test**

```bash
./gradlew :server:libs:platform:platform-connection:platform-connection-graphql:testIntegration
```
Expected: BUILD SUCCESSFUL, 1 test passes.

If `@ConditionalOnCoordinator` blocks the controller from being instantiated in the test context, the simplest fix is to add a stub `@ConditionalOnCoordinator`-satisfying bean in the test config. Look at how other `*GraphQlControllerIntTest` classes in the project handle this — they likely already have the pattern.

- [ ] **Step 4: Run check**

```bash
./gradlew :server:libs:platform:platform-connection:platform-connection-graphql:check
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add server/libs/platform/platform-connection/platform-connection-graphql/src/test/java/com/bytechef/platform/connection/web/graphql/ConnectionCredentialStoreGraphQlControllerIntTest.java
git commit -m "$(cat <<'EOF'
547 Test connectionCredentialStores GraphQL query

GraphQlTester-based integration test verifying the resolver iterates the
injected ConnectionCredentialStore list and returns (type, readOnly)
tuples in registration order. Uses lightweight stub stores so the test
doesn't require Spring Data JDBC + Testcontainers.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 6: Final spotless + check sweep

- [ ] **Step 1: Spotless on all touched modules**

```bash
./gradlew \
  :server:libs:platform:platform-connection:platform-connection-graphql:spotlessApply
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Full check on the new module + dependents**

```bash
./gradlew :server:libs:platform:platform-connection:platform-connection-graphql:check
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: If spotless changed anything, commit**

```bash
git status --short
# If files changed:
git add -u
git commit -m "$(cat <<'EOF'
547 Apply spotless formatting

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

- [ ] **Step 4: Verify commit graph**

```bash
git log --oneline 089f60ed024..HEAD
```
Expected output (~4–5 new commits):
```
<hash> 547 Apply spotless formatting           (only if Step 3 was needed)
<hash> 547 Test connectionCredentialStores GraphQL query
<hash> 547 client - Regenerate graphql.ts middleware
<hash> 547 client - Add connectionCredentialStores GraphQL operation
<hash> 547 Add platform-connection-graphql module with credential-store query
```

PR-2 complete. PR-3 (AWS Secrets Manager adapter) is the next plan.

---

## Self-Review Notes

**Spec coverage:**
- ✓ `connectionCredentialStores` GraphQL query → Task 3
- ✓ `ConnectionCredentialStoreInfo` type → Task 3
- ✓ `ConnectionCredentialStoreType` enum in GraphQL → Task 2 (schema)
- ✓ Client operation file → Task 4
- ✓ codegen.ts integration → Task 4
- ✓ Resolver-level test → Task 5

**Open items from spec resolved here:**
- ✓ "identify existing platform-connection GraphQL module or create new one" → Task 1 creates `platform-connection-graphql` (none existed)
- ✓ Resolver located at `com.bytechef.platform.connection.web.graphql` matching the project's naming convention

**Out-of-scope for PR-2:**
- AWS Secrets Manager adapter (PR-3)
- HashiCorp Vault adapter (PR-4)
- Frontend UI consuming the query (sibling spec)

**Risks:**
- Task 5's exact `@GraphQlTest` invocation may need adjustment depending on what the project's other GraphQL int tests do. Step 1 explicitly inspects an existing test first to match the convention.
- If `@ConditionalOnCoordinator` requires a property to be set in the test context, the test may need additional config. Existing tests should show the pattern.
