# Auto-Memory Owner Discriminator Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an `AiAutoMemoryPrincipalType {USER, DEPLOYMENT}` discriminator to the CE `ai_auto_memory` stack and rename `user_id` → `principal_id`, so memory rows can be owned by a user *or* a project deployment without colliding in the shared table.

**Architecture:** `ai_auto_memory` becomes a polymorphic-owner table. A new INT-ordinal enum discriminates owner kind; it joins `(workspaceId, principalId, environment, name)` as part of the service-layer ownership/uniqueness key. All existing callers pass `USER` (behavior unchanged); the future workflow tool (Plan B) will pass `DEPLOYMENT`. This is a refactor of an existing CE module — it lands green with no externally visible behavior change.

**Tech Stack:** Java 25, Spring Boot 4, Spring Data JDBC, Liquibase, JUnit 5 + Testcontainers (integration), Gradle.

**Scope note:** This is Plan A (foundation). Plan B (CE SPI adapters + `AiAgentUtilsAutoMemoryTool` exposure) is a separate plan written after A lands. Spec: `docs/superpowers/specs/2026-06-05-workflow-agent-auto-memory-design.md`.

**Module path shorthand used below:**
- `:api` = `:server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-api`
- `:service` = `:server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-service`
- `:repo-api` = `:server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-repository:platform-ai-auto-memory-repository-api`
- `:repo-jdbc` = `:server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-repository:platform-ai-auto-memory-repository-jdbc`
- `:graphql` = `:server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-graphql`
- `:hub-api` = `:server:ee:libs:platform:platform-ai-hub:platform-ai-hub-api`
- `:hub-service` = `:server:ee:libs:platform:platform-ai-hub:platform-ai-hub-service`

**Global guard rails (apply to every task):**
- Run `./gradlew spotlessApply` on touched modules before each commit.
- Stage only files for the current task.
- Commit messages: server-side `<ticket> <description>`; use `-` as the ticket placeholder for this chore unless a ticket is assigned.

---

### Task 1: Add `AiAutoMemoryPrincipalType` enum + ordinal-stability pin

**Files:**
- Create: `server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-api/src/main/java/com/bytechef/platform/ai/auto/memory/AiAutoMemoryPrincipalType.java`
- Modify: `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/test/java/com/bytechef/ee/platform/aihub/util/EnumOrdinalStabilityTest.java`

- [ ] **Step 1: Write the failing pin test**

Add this method inside `EnumOrdinalStabilityTest` (next to `testAiAutoMemoryTypeOrdinalsAreStable`), and add the import `import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;`:

```java
@Test
void testAiAutoMemoryPrincipalTypeOrdinalsAreStable() {
    Map<String, Integer> expected = new LinkedHashMap<>();

    expected.put("USER", 0);
    expected.put("DEPLOYMENT", 1);

    OrdinalStabilityAssertions.assertOrdinalsMatch(
        AiAutoMemoryPrincipalType.values(), expected, AiAutoMemoryPrincipalType.class.getSimpleName());
}
```

- [ ] **Step 2: Run test to verify it fails (compile error — enum missing)**

Run: `./gradlew :hub-api:compileTestJava`
Expected: FAIL — `cannot find symbol ... AiAutoMemoryPrincipalType`.

- [ ] **Step 3: Create the enum**

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

package com.bytechef.platform.ai.auto.memory;

/**
 * Discriminates the owner kind of an {@link AiAutoMemory} row so user-owned (AI Hub) and deployment-owned
 * (workflow agent) memory can share the {@code ai_auto_memory} table without colliding.
 *
 * <p>
 * <b>Append-only.</b> Persisted as an INT ordinal in {@code ai_auto_memory.principal_type}; reordering or removing a
 * value would silently re-map historical rows. New values MUST be appended at the end. Pinned by
 * {@code EnumOrdinalStabilityTest#testAiAutoMemoryPrincipalTypeOrdinalsAreStable}.
 * </p>
 *
 * @author Ivica Cardic
 */
public enum AiAutoMemoryPrincipalType {

    // append-only
    USER,
    DEPLOYMENT
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :hub-api:test --tests "*EnumOrdinalStabilityTest*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
./gradlew :api:spotlessApply :hub-api:spotlessApply
git add server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-api/src/main/java/com/bytechef/platform/ai/auto/memory/AiAutoMemoryPrincipalType.java \
        server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/test/java/com/bytechef/ee/platform/aihub/util/EnumOrdinalStabilityTest.java
git commit -m "- Add AiAutoMemoryPrincipalType discriminator enum"
```

---

### Task 2: Liquibase migration — rename `user_id` → `principal_id`, add `principal_type`, swap index

**Files:**
- Create: `server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-repository/platform-ai-auto-memory-repository-jdbc/src/main/resources/config/liquibase/changelog/platform/ai/auto/memory/20260605000001_ai_auto_memory_principal_type.xml`
- Modify: the module's changelog aggregator that `<include>`s the init file (same directory or a `db.changelog-master`-style file — find it in Step 1).

- [ ] **Step 1: Find the exact existing names and the changelog include site**

Run:
```bash
cd server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-repository/platform-ai-auto-memory-repository-jdbc/src/main/resources
grep -rn "ai_auto_memory" --include=*.xml .
grep -rn "createIndex\|addUniqueConstraint\|user_id" config/liquibase/changelog/platform/ai/auto/memory/20260424000001_ai_auto_memory_init.xml
grep -rn "20260424000001" --include=*.xml .
```
Expected: shows the init changeSet, the real index name (e.g. `idx_ai_auto_memory_user_env`) and columns, and the file that `<include>`s the init changelog. Record the exact index name — the next step drops it by name.

- [ ] **Step 2: Create the follow-up migration**

Use the exact `oldIndexName` recorded in Step 1 (shown below as `idx_ai_auto_memory_user_env`; correct if different).

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <changeSet author="bytechef" id="20260605000001-1">
        <renameColumn tableName="ai_auto_memory" oldColumnName="user_id" newColumnName="principal_id"
                      columnDataType="BIGINT"/>
    </changeSet>

    <changeSet author="bytechef" id="20260605000001-2">
        <addColumn tableName="ai_auto_memory">
            <column name="principal_type" type="INT" defaultValueNumeric="0">
                <constraints nullable="false"/>
            </column>
        </addColumn>
    </changeSet>

    <changeSet author="bytechef" id="20260605000001-3">
        <dropIndex tableName="ai_auto_memory" indexName="idx_ai_auto_memory_user_env"/>
        <createIndex tableName="ai_auto_memory" indexName="idx_ai_auto_memory_principal_env">
            <column name="principal_type"/>
            <column name="principal_id"/>
            <column name="environment"/>
            <column name="memory_type"/>
        </createIndex>
    </changeSet>
</databaseChangeLog>
```

- [ ] **Step 3: Register the migration in the changelog aggregator**

Add an `<include file="config/liquibase/changelog/platform/ai/auto/memory/20260605000001_ai_auto_memory_principal_type.xml" relativeToChangelogFile="false"/>` line immediately AFTER the existing init `<include>` found in Step 1 (match the surrounding file's path/attribute style exactly).

- [ ] **Step 4: Delete stale build copies (migration-rename lesson)**

Run:
```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/quizzical-driscoll-8d47a4
rm -rf server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-repository/platform-ai-auto-memory-repository-jdbc/build/resources
```
Expected: no output. (Prevents Liquibase from seeing both old and new copies on the classpath.)

- [ ] **Step 5: Commit (verification happens in Task 4's integration test)**

```bash
git add server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-repository/platform-ai-auto-memory-repository-jdbc/src/main/resources/config/liquibase/
git commit -m "- Migrate ai_auto_memory: rename user_id to principal_id, add principal_type"
```

---

### Task 3: Update the `AiAutoMemory` entity (field rename + `principalType`)

**Files:**
- Modify: `server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-api/src/main/java/com/bytechef/platform/ai/auto/memory/AiAutoMemory.java`

- [ ] **Step 1: Rename the `userId` field and accessors to `principalId`**

Replace the field declaration:
```java
    @Column("user_id")
    private long userId;
```
with:
```java
    @Column("principal_id")
    private long principalId;

    @Column("principal_type")
    private int principalType;
```

Replace the bind-once constructor:
```java
    public AiAutoMemory(long userId) {
        this.userId = userId;
    }
```
with:
```java
    public AiAutoMemory(AiAutoMemoryPrincipalType principalType, long principalId) {
        this.principalType = Objects.requireNonNull(principalType, "principalType")
            .ordinal();
        this.principalId = principalId;
    }
```

Replace the `getUserId()`/`setUserId(...)` accessors (find them just below the read region) with:
```java
    public long getPrincipalId() {
        return principalId;
    }

    void setPrincipalId(long principalId) {
        this.principalId = principalId;
    }

    public AiAutoMemoryPrincipalType getPrincipalType() {
        AiAutoMemoryPrincipalType[] values = AiAutoMemoryPrincipalType.values();

        if (principalType < 0 || principalType >= values.length) {
            throw new IllegalStateException("Unknown AiAutoMemoryPrincipalType ordinal: " + principalType);
        }

        return values[principalType];
    }

    void setPrincipalType(AiAutoMemoryPrincipalType principalType) {
        this.principalType = Objects.requireNonNull(principalType, "principalType")
            .ordinal();
    }
```

- [ ] **Step 2: Update equals/hashCode/toString if they reference `userId`**

Run: `grep -n "userId" server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-api/src/main/java/com/bytechef/platform/ai/auto/memory/AiAutoMemory.java`
For each remaining hit (e.g. in `equals`, `hashCode`, `toString`), replace `userId` with `principalId` and add `principalType` alongside it where the others are listed.

- [ ] **Step 3: Compile**

Run: `./gradlew :api:compileJava`
Expected: FAIL only in dependent modules later; `:api` itself compiles. (The service/repo still reference old names — fixed in Tasks 4–5. If `:api` has no internal callers of `getUserId`, it compiles clean.)

- [ ] **Step 4: Spotless + commit**

```bash
./gradlew :api:spotlessApply
git add server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-api/src/main/java/com/bytechef/platform/ai/auto/memory/AiAutoMemory.java
git commit -m "- Rename AiAutoMemory.userId to principalId; add principalType"
```

---

### Task 4: Update repository (interface + JDBC) and prove the migration

**Files:**
- Modify: `server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-repository/platform-ai-auto-memory-repository-api/src/main/java/com/bytechef/platform/ai/auto/memory/repository/AiAutoMemoryRepository.java`
- Modify: `server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-repository/platform-ai-auto-memory-repository-jdbc/src/main/java/com/bytechef/platform/ai/auto/memory/repository/jdbc/JdbcAiAutoMemoryRepository.java`
- Test (integration): the existing `*IntTest` under `:repo-jdbc/src/test` (find it in Step 1).

- [ ] **Step 1: Locate the repository integration test**

Run: `find server/libs/platform/platform-ai/platform-ai-auto-memory -name "*IntTest.java"`
Expected: a JDBC repo `IntTest`. If none exists, create `JdbcAiAutoMemoryRepositoryIntTest` mirroring a sibling repo IntTest (e.g. under `platform-ai-hub` or another auto-memory module). Open it; you'll update its query calls in Step 4.

- [ ] **Step 2: Rewrite the three derived query signatures in the interface**

Replace:
```java
    List<AiAutoMemory> findByWorkspaceIdAndUserIdAndEnvironmentOrderByUpdatedAtDesc(
        long workspaceId, long userId, int environment);

    List<AiAutoMemory> findByWorkspaceIdAndUserIdAndEnvironmentAndMemoryTypeOrderByUpdatedAtDesc(
        long workspaceId, long userId, int environment, int memoryType);

    List<AiAutoMemory> findAllByWorkspaceIdAndUserIdAndEnvironmentAndName(
        long workspaceId, long userId, int environment, String name);
```
with:
```java
    List<AiAutoMemory> findByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentOrderByUpdatedAtDesc(
        long workspaceId, int principalType, long principalId, int environment);

    List<AiAutoMemory> findByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentAndMemoryTypeOrderByUpdatedAtDesc(
        long workspaceId, int principalType, long principalId, int environment, int memoryType);

    List<AiAutoMemory> findAllByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentAndName(
        long workspaceId, int principalType, long principalId, int environment, String name);
```

- [ ] **Step 3: Update the JDBC `@Query` SQL**

In `JdbcAiAutoMemoryRepository`, for each of the three methods: rename the method to match Step 2, change `m.user_id = :userId` → `m.principal_id = :principalId`, add `AND m.principal_type = :principalType`, and update the `@Param` names accordingly. Example for the first:

```java
@Query("""
    SELECT m.* FROM ai_auto_memory m
    JOIN workspace_ai_auto_memory wam ON wam.ai_auto_memory_id = m.id
    WHERE wam.workspace_id = :workspaceId
      AND m.principal_type = :principalType
      AND m.principal_id = :principalId
      AND m.environment = :environment
    ORDER BY m.updated_at DESC
    """)
List<AiAutoMemory> findByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentOrderByUpdatedAtDesc(
    @Param("workspaceId") long workspaceId, @Param("principalType") int principalType,
    @Param("principalId") long principalId, @Param("environment") int environment);
```
Apply the equivalent edit (add `principal_type` predicate, `user_id`→`principal_id`) to the memory-type-filtered and the by-name methods.

- [ ] **Step 4: Update the integration test to exercise the new signatures + isolation**

In the repo IntTest, save two rows that differ ONLY by principal type, then assert the query is owner-scoped. Add (adapt entity construction to the test's existing helpers):

```java
@Test
void testPrincipalTypeIsolatesRows() {
    AiAutoMemory userRow = new AiAutoMemory(AiAutoMemoryPrincipalType.USER, 100L);
    userRow.setName("shared-name");
    userRow.setTitle("u");
    userRow.setContent("user-content");
    userRow.setMemoryType(AiAutoMemoryType.USER);
    userRow.setEnvironment(0);
    userRow = aiAutoMemoryRepository.save(userRow);
    linkWorkspace(1L, userRow.getId());   // helper that inserts workspace_ai_auto_memory

    AiAutoMemory deploymentRow = new AiAutoMemory(AiAutoMemoryPrincipalType.DEPLOYMENT, 100L);
    deploymentRow.setName("shared-name");
    deploymentRow.setTitle("d");
    deploymentRow.setContent("deployment-content");
    deploymentRow.setMemoryType(AiAutoMemoryType.USER);
    deploymentRow.setEnvironment(0);
    deploymentRow = aiAutoMemoryRepository.save(deploymentRow);
    linkWorkspace(1L, deploymentRow.getId());

    List<AiAutoMemory> userHits = aiAutoMemoryRepository
        .findAllByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentAndName(
            1L, AiAutoMemoryPrincipalType.USER.ordinal(), 100L, 0, "shared-name");

    assertThat(userHits).hasSize(1);
    assertThat(userHits.get(0).getContent()).isEqualTo("user-content");
}
```
If `linkWorkspace` / a workspace-link helper doesn't exist in the test, add a minimal one that inserts a `workspace_ai_auto_memory(workspace_id, ai_auto_memory_id)` row via the test's `JdbcTemplate`/repository.

- [ ] **Step 5: Run the integration test (validates migration + queries together)**

Run: `./gradlew :repo-jdbc:testIntegration --tests "*AiAutoMemory*"`
Expected: PASS (Testcontainers applies the new migration; isolation holds).

- [ ] **Step 6: Spotless + commit**

```bash
./gradlew :repo-api:spotlessApply :repo-jdbc:spotlessApply
git add server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-repository/
git commit -m "- Add principal_type to ai_auto_memory repository queries"
```

---

### Task 5: Thread `principalType` through `AiAutoMemoryService` (+ impl + unit test)

**Files:**
- Modify: `server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-api/src/main/java/com/bytechef/platform/ai/auto/memory/AiAutoMemoryService.java`
- Modify: `server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-service/src/main/java/com/bytechef/platform/ai/auto/memory/AiAutoMemoryServiceImpl.java`
- Test: `server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-service/src/test/java/com/bytechef/platform/ai/auto/memory/AiAutoMemoryServiceTest.java`

- [ ] **Step 1: Write the failing isolation test**

Replace the `USER_ID` constant usage by adding a principal type. Add this test (adapt to the test's existing mock/stub setup for the repository):

```java
@Test
void testUserAndDeploymentMemoriesDoNotCollide() {
    aiAutoMemoryService.create(
        WORKSPACE_ID, AiAutoMemoryPrincipalType.USER, PRINCIPAL_ID, ENVIRONMENT,
        "notes", "t", null, AiAutoMemoryType.USER, "user-body");
    aiAutoMemoryService.create(
        WORKSPACE_ID, AiAutoMemoryPrincipalType.DEPLOYMENT, PRINCIPAL_ID, ENVIRONMENT,
        "notes", "t", null, AiAutoMemoryType.USER, "deployment-body");

    Optional<AiAutoMemory> userRead = aiAutoMemoryService.read(
        WORKSPACE_ID, AiAutoMemoryPrincipalType.USER, PRINCIPAL_ID, ENVIRONMENT, "notes");

    assertThat(userRead).isPresent();
    assertThat(userRead.get().getContent()).isEqualTo("user-body");
}
```
Add constants `PRINCIPAL_ID`/`ENVIRONMENT`/`WORKSPACE_ID` if not already present (reuse the existing `USER_ID` value for `PRINCIPAL_ID`). For a mock-repository test, stub `findAllBy...AndName(...)` to be principal-type-aware so the create() duplicate check passes for each type.

- [ ] **Step 2: Run test to verify it fails (compile error — old signatures)**

Run: `./gradlew :service:compileTestJava`
Expected: FAIL — `create(...)`/`read(...)` signatures don't match.

- [ ] **Step 3: Update the service interface**

For every method, replace the `long userId` param with `AiAutoMemoryPrincipalType principalType, long principalId` (principalType first), and rename `listByUserAndWorkspace` → `listByPrincipalAndWorkspace`. Final interface:

```java
AiAutoMemory create(long workspaceId, AiAutoMemoryPrincipalType principalType, long principalId, int environment,
    String name, String title, @Nullable String description, AiAutoMemoryType memoryType, String content);
Optional<AiAutoMemory> read(long workspaceId, AiAutoMemoryPrincipalType principalType, long principalId,
    int environment, String name);
AiAutoMemory update(long workspaceId, AiAutoMemoryPrincipalType principalType, long principalId, int environment,
    String name, @Nullable String title, @Nullable String description, @Nullable AiAutoMemoryType memoryType,
    @Nullable String content);
AiAutoMemory updateById(long workspaceId, AiAutoMemoryPrincipalType principalType, long principalId, long memoryId,
    @Nullable String title, @Nullable String description, @Nullable AiAutoMemoryType memoryType,
    @Nullable String content);
AiAutoMemory delete(long workspaceId, AiAutoMemoryPrincipalType principalType, long principalId, int environment,
    String name);
AiAutoMemory deleteById(long workspaceId, AiAutoMemoryPrincipalType principalType, long principalId, long memoryId);
AiAutoMemory rename(long workspaceId, AiAutoMemoryPrincipalType principalType, long principalId, int environment,
    String oldName, String newName);
List<AiAutoMemory> list(long workspaceId, AiAutoMemoryPrincipalType principalType, long principalId, int environment,
    @Nullable AiAutoMemoryType memoryType);
Optional<AiAutoMemory> findById(long workspaceId, AiAutoMemoryPrincipalType principalType, long principalId,
    long memoryId);
List<AiAutoMemory> listByPrincipalAndWorkspace(long workspaceId, AiAutoMemoryPrincipalType principalType,
    long principalId, int environment);
```

- [ ] **Step 4: Update `AiAutoMemoryServiceImpl`**

Mechanical propagation: in every method body, replace calls to the renamed repository methods (Task 4), pass `principalType.ordinal()` and `principalId` where the repo expects them, construct new rows via `new AiAutoMemory(principalType, principalId)` and call `setPrincipalType(principalType)` is already handled by the constructor. In `create()`, the duplicate-name check must call `findAllByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentAndName(workspaceId, principalType.ordinal(), principalId, environment, name)`. In ownership checks for `*ById`, verify the loaded row's `getPrincipalType() == principalType` AND `getPrincipalId() == principalId` before returning (else throw `AiAutoMemoryNotFoundException`, preserving the existing not-found shape).

Run `grep -n "userId\|listByUserAndWorkspace\|new AiAutoMemory(" AiAutoMemoryServiceImpl.java` and resolve every hit per the above.

- [ ] **Step 5: Run the unit test**

Run: `./gradlew :service:test --tests "*AiAutoMemoryServiceTest*"`
Expected: PASS.

- [ ] **Step 6: Spotless + commit**

```bash
./gradlew :api:spotlessApply :service:spotlessApply
git add server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-api/src/main/java/com/bytechef/platform/ai/auto/memory/AiAutoMemoryService.java \
        server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-service/
git commit -m "- Thread AiAutoMemoryPrincipalType through AiAutoMemoryService"
```

---

### Task 6: Update the CE GraphQL caller to pass `USER`

**Files:**
- Modify: `server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-graphql/src/main/java/com/bytechef/platform/ai/auto/memory/web/graphql/AiAutoMemoryGraphQlController.java`

- [ ] **Step 1: Update each service call**

Run: `grep -n "aiAutoMemoryService\.\|getCurrentUser" AiAutoMemoryGraphQlController.java`
For each `aiAutoMemoryService.<method>(workspaceId, userId, ...)` call, insert `AiAutoMemoryPrincipalType.USER` immediately before the user id arg (which is `principalId` now). Example:
```java
// before
aiAutoMemoryService.list(workspaceId, userId, environment, memoryType);
// after
aiAutoMemoryService.list(workspaceId, AiAutoMemoryPrincipalType.USER, userId, environment, memoryType);
```
Add `import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;`.

- [ ] **Step 2: Compile**

Run: `./gradlew :graphql:compileJava`
Expected: PASS.

- [ ] **Step 3: Run the module's tests**

Run: `./gradlew :graphql:test`
Expected: PASS (update any test that calls the service to also pass `AiAutoMemoryPrincipalType.USER`).

- [ ] **Step 4: Spotless + commit**

```bash
./gradlew :graphql:spotlessApply
git add server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-graphql/
git commit -m "- Pass USER principal type from AiAutoMemory GraphQL controller"
```

---

### Task 7: Update the EE AI Hub adapters + seam test to pass `USER`

**Files:**
- Modify: `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/tool/memory/DbMemoryResource.java`
- Modify: `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/tool/memory/DbAutoMemoryDirectoryOps.java`
- Modify: `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/test/java/com/bytechef/ee/platform/aihub/tool/memory/DbAutoMemorySeamTest.java`

- [ ] **Step 1: Update both adapters**

In `DbMemoryResource` and `DbAutoMemoryDirectoryOps`, for every `aiAutoMemoryService.<method>(...)` call, insert `AiAutoMemoryPrincipalType.USER` before the user-id arg, and rename `listByUserAndWorkspace(...)` → `listByPrincipalAndWorkspace(workspaceId, AiAutoMemoryPrincipalType.USER, userId, environment)`. Add the import to each. These are AI Hub (user-owned) so `USER` is correct.

Run per file: `grep -n "aiAutoMemoryService\.\|listByUserAndWorkspace" <file>` and resolve each hit.

- [ ] **Step 2: Update the seam test**

In `DbAutoMemorySeamTest`, update the mock verifications/stubs to the new signatures (insert `AiAutoMemoryPrincipalType.USER`, rename the list method). Run `grep -n "aiAutoMemoryService\|listByUserAndWorkspace" DbAutoMemorySeamTest.java` and fix each.

- [ ] **Step 3: Run the seam test**

Run: `./gradlew :hub-service:test --tests "*DbAutoMemorySeamTest*"`
Expected: PASS.

- [ ] **Step 4: Spotless + commit**

```bash
./gradlew :hub-service:spotlessApply
git add server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/tool/memory/DbMemoryResource.java \
        server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/tool/memory/DbAutoMemoryDirectoryOps.java \
        server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/test/java/com/bytechef/ee/platform/aihub/tool/memory/DbAutoMemorySeamTest.java
git commit -m "- Pass USER principal type from AI Hub auto-memory adapters"
```

---

### Task 8: Full verification across all touched modules

**Files:** none (verification only).

- [ ] **Step 1: Run `check` on every touched module**

Run:
```bash
./gradlew :api:check :service:check :repo-api:check :repo-jdbc:check :graphql:check \
          :hub-api:check :hub-service:check
```
Expected: BUILD SUCCESSFUL. Fix any straggler caller the compiler surfaces (grep the whole repo again: `grep -rn "listByUserAndWorkspace\|getUserId()" --include="*.java" server/ | grep -i memory`).

- [ ] **Step 2: Run repo integration tests**

Run: `./gradlew :repo-jdbc:testIntegration`
Expected: BUILD SUCCESSFUL (Docker running for Testcontainers).

- [ ] **Step 3: Confirm no stray references remain**

Run: `grep -rn "AiAutoMemory" --include="*.java" server/ | grep -E "userId|listByUserAndWorkspace" || echo CLEAN`
Expected: `CLEAN`.

---

## Self-Review

- **Spec coverage:** §1 data model → Tasks 1–3; §2 service → Task 5; §3 repository → Task 4; §6 caller migration → Tasks 6–7; testing → Tasks 1/4/5 + Task 8. CE adapters (§4) and CE tool (§5) are intentionally **Plan B**, noted in the header. ✅
- **Placeholder scan:** No TBD/"add error handling"; every code step shows code; migration uses concrete change types. The two "find the exact name/test" steps (Task 2 Step 1, Task 4 Step 1) are deliberate verifications for names that differ between the entity Javadoc and the on-disk file. ✅
- **Type consistency:** `AiAutoMemoryPrincipalType` (USER=0, DEPLOYMENT=1); `principalId`/`principalType`; renamed methods `find...PrincipalTypeAndPrincipalId...`, `listByPrincipalAndWorkspace`; service signatures put `principalType` before `principalId` everywhere. ✅
