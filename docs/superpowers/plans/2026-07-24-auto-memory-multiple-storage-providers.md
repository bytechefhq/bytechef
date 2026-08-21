# Auto-memory Multiple Storage Providers Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let auto-memory persist to `JDBC` (today, default), `FILESYSTEM`, or `AWS`, selected by `bytechef.ai.auto-memory.provider`, with identical observable query behavior across backends.

**Architecture:** Auto-memory's repository seam (`AiAutoMemoryRepository`, `WorkspaceAiAutoMemoryRepository`) is already backend-agnostic plain-Java. Add ONE CE implementation over the existing `FileStorageService` abstraction; `FILESYSTEM` and `AWS` are the same code path differing only in which `FileStorageService` the shared `FileStorageServiceRegistry` returns. No SPI change, no new EE module.

**Tech Stack:** Java 25, Spring Boot 4.0.7, Gradle 9.4 (Kotlin DSL), Jackson (`tools.jackson`), Testcontainers (PostgreSQL + LocalStack).

**Spec:** `docs/superpowers/specs/2026-07-24-auto-memory-multiple-storage-providers-design.md`

## Global Constraints

- CE code (`server/libs/`): Apache 2.0 license header, NO `@version ee`. EE code (`server/ee/`): ByteChef Enterprise header AND a `@version ee` Javadoc tag (Spotless selects the header by `@version ee` CONTENT — an EE file without it gets rewritten to the Apache header and fails the build).
- `bytechef.ai.auto-memory.provider` MUST be declared as a typed field in `ApplicationProperties` (`@ConfigurationProperties(prefix = "bytechef", ignoreUnknownFields = false)`) or setting it in yml fails startup binding.
- **Default is `JDBC`** — existing deployments must be byte-for-byte unaffected.
- Never generate ids with `hashCode()` (collision risk); use a UUID-derived positive `long`.
- File-backed semantics are **best-effort, single-writer**: whole-object writes, last-write-wins, no cross-object transaction. Document; do not silently pretend otherwise.
- An unresolvable provider MUST fail fast at startup with an actionable message — never silently fall back to another backend.
- Ordering comes from the JSON `updatedAt` field, never from file metadata (`FileEntry` has none).
- Repo style: descriptive names, blank line before control statements, no trailing blank line before class close. Run `./gradlew spotlessApply` before each commit.

---

### Task 1: Harmonize `getFileEntries` across providers

Pre-existing divergence that must be fixed before one repository can rely on it.

**Files:**
- Modify: `server/libs/core/file-storage/file-storage-api/src/main/java/com/bytechef/file/storage/service/FileStorageService.java` (javadoc on `getFileEntries`, line ~41)
- Modify: `server/ee/libs/core/file-storage/file-storage-aws/file-storage-aws-impl/src/main/java/com/bytechef/ee/file/storage/aws/service/AwsFileStorageServiceImpl.java` (~line 89)
- Modify: `server/libs/core/file-storage/file-storage-filesystem-service/src/main/java/com/bytechef/file/storage/filesystem/service/FilesystemFileStorageService.java` (~line 123)
- Test: `.../file-storage-filesystem-service/src/test/java/.../FilesystemFileStorageServiceTest.java` (create if absent)

**Interfaces:**
- Produces: a pinned `getFileEntries` contract — recursive/prefix traversal; `FileEntry.name` is the bare filename with NO leading separator; empty set for a missing directory; never throws for a missing directory; ordering unspecified.

- [ ] **Step 1: Write failing tests** for `FilesystemFileStorageService.getFileEntries`:
  - stores `a.json` under `dir/sub/` and `b.json` under `dir/`, asserts `getFileEntries("dir")` returns names exactly `{"a.json", "b.json"}` (no leading `/`, recursive).
  - asserts `getFileEntries("does-not-exist")` returns an EMPTY set and does not throw.
- [ ] **Step 2: Run them — the missing-directory test must FAIL** (`Files.walk` throws `NoSuchFileException`). Run: `./gradlew :server:libs:core:file-storage:file-storage-filesystem-service:test --console=plain` then `echo "EXIT=$?"` on its own line.
- [ ] **Step 3: Fix `FilesystemFileStorageService.getFileEntries`** — return `Set.of()` when `Files.notExists(directoryPath)` before walking.
- [ ] **Step 4: Fix the AWS off-by-one.** In `AwsFileStorageServiceImpl.getFileEntries` change
  `filename.substring(filename.lastIndexOf('/'))` to `filename.substring(filename.lastIndexOf('/') + 1)`
  so AWS returns `memo.json`, matching filesystem, instead of `/memo.json`.
- [ ] **Step 5: Verify S3 pagination.** Read Spring Cloud AWS `S3Template.listObjects` and determine whether it returns ALL keys or a single (1000-key) page. If it truncates, page with a continuation token. Record the finding in the report either way — do not assume.
- [ ] **Step 6: Document the contract** in the `FileStorageService.getFileEntries` javadoc (the four guarantees above).
- [ ] **Step 7: Run + commit.** `./gradlew :server:libs:core:file-storage:file-storage-filesystem-service:test :server:libs:core:file-storage:file-storage-api:compileJava :server:ee:libs:core:file-storage:file-storage-aws:file-storage-aws-impl:compileJava --console=plain`, then `spotlessApply`, then:
  `git commit -m "Harmonize FileStorageService.getFileEntries semantics across providers"`

---

### Task 2: Declare the provider property

**Files:**
- Modify: `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java`

**Interfaces:**
- Produces: `applicationProperties.getAi().getAutoMemory().getProvider()` returning `Ai.AutoMemory.Provider` (`JDBC` default).

- [ ] **Step 1: Add the nested class** inside `Ai` (mirror the existing `Ai.Memory` at ~line 785). Insert the field alphabetically among `Ai`'s fields (before `copilot`, ~line 690), with matching getter/setter placement:

```java
        /**
         * Auto-memory storage configuration
         */
        private AutoMemory autoMemory = new AutoMemory();
```

```java
        public static class AutoMemory {

            /**
             * Auto-memory storage provider type.
             */
            public enum Provider {
                /**
                 * AWS S3 object storage (requires the EE AWS file-storage module)
                 */
                AWS,
                /**
                 * Local filesystem storage
                 */
                FILESYSTEM,
                /**
                 * Relational JDBC storage
                 */
                JDBC
            }

            /**
             * Auto-memory storage provider
             */
            private Provider provider = Provider.JDBC;

            public Provider getProvider() {
                return provider;
            }

            public void setProvider(Provider provider) {
                this.provider = provider;
            }
        }
```

- [ ] **Step 2: Add the `Ai`-level getter/setter** (`getAutoMemory()` / `setAutoMemory(...)`) in the same alphabetical position as the field, matching the surrounding style.
- [ ] **Step 3: Compile + commit.** `./gradlew :server:libs:config:app-config:compileJava --console=plain` → `echo "EXIT=$?"`; `spotlessApply`;
  `git commit -m "Declare bytechef.ai.auto-memory.provider in ApplicationProperties"`

---

### Task 3: The file-storage repository module (core of the feature)

**Files:**
- Create: `server/libs/platform/platform-ai/platform-ai-auto-memory/platform-ai-auto-memory-repository/platform-ai-auto-memory-repository-file-storage/build.gradle.kts`
- Create: `.../src/main/java/com/bytechef/platform/ai/auto/memory/repository/filestorage/AiAutoMemoryDocument.java`
- Create: `.../filestorage/FileStorageAiAutoMemoryRepository.java`
- Create: `.../filestorage/FileStorageWorkspaceAiAutoMemoryRepository.java`
- Modify: `settings.gradle.kts` (include the new module after the `-jdbc` line, ~line 64)

**Interfaces:**
- Consumes: `AiAutoMemoryRepository`, `WorkspaceAiAutoMemoryRepository` (repository-api); `FileStorageService` (`file-storage-api`); `AiAutoMemory`, `WorkspaceAiAutoMemory`, `AiAutoMemoryPrincipalType`, `AiAutoMemoryType` (auto-memory-api).
- Produces: `FileStorageAiAutoMemoryRepository(FileStorageService fileStorageService)` and `FileStorageWorkspaceAiAutoMemoryRepository(FileStorageService fileStorageService)`.

`AiAutoMemory` accessors available (verified): `getId/setId(Long)`, `getPrincipalId()`, `getPrincipalType(): AiAutoMemoryPrincipalType`, `getName/setName`, `getTitle/setTitle`, `getDescription/setDescription`, `getMemoryType/setMemoryType(AiAutoMemoryType)`, `getEnvironment(): Environment`, `getEnvironmentId(): long`, `setEnvironment(Environment)`, `getContent/setContent`, `getCreatedAt/setCreatedAt(LocalDateTime)`, `getUpdatedAt/setUpdatedAt(LocalDateTime)`. Constructors: `AiAutoMemory()` and `AiAutoMemory(AiAutoMemoryPrincipalType, long principalId)`.

`WorkspaceAiAutoMemory` has `WorkspaceAiAutoMemory(Long workspaceId, Long aiAutoMemoryId)`, `getId()`, `getWorkspaceId()`, `getAiAutoMemoryId()`.

**Directory layout** (encodes the filter tuple so a query's prefix narrows to its rows):
`ai-auto-memory/{tenantId}/{workspaceId}/{principalType}-{principalId}/{environment}/{id}.json`

- [ ] **Step 1: `build.gradle.kts`** (mirror the `-jdbc` module):

```kotlin
dependencies {
    api(project(":server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-repository:platform-ai-auto-memory-repository-api"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:file-storage:file-storage-api"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-api"))

    testImplementation(project(":server:libs:core:file-storage:file-storage-filesystem-service"))
}
```
Add to `settings.gradle.kts`:
`include("server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-repository:platform-ai-auto-memory-repository-file-storage")`

- [ ] **Step 2: Write the persisted document type** `AiAutoMemoryDocument` — a record carrying every `AiAutoMemory` field PLUS `workspaceId` (membership folded in, so each write is a single object). Include an `AiAutoMemory toDomain()` and `static AiAutoMemoryDocument fromDomain(AiAutoMemory, long workspaceId)`. Serialize `principalType`/`memoryType`/`environment` as their persisted INT ordinals (matching the JDBC columns) and `createdAt`/`updatedAt` as ISO-8601 strings.

- [ ] **Step 3: Write failing tests first** — `FileStorageAiAutoMemoryRepositoryTest` backed by a real `FilesystemFileStorageService` over a JUnit `@TempDir`:
  - `save()` then `findById()` round-trips every field.
  - `findByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentOrderByUpdatedAtDesc` returns ONLY matching rows and is ordered newest-`updatedAt` first (insert out of order to prove sorting).
  - the `...AndMemoryType...` variant narrows further.
  - `findAllBy...AndName` returns all matches (duplicates permitted — the DB no longer enforces uniqueness).
  - `delete`/`deleteById` removes only the target; `findById` then returns `Optional.empty()`.
  - a query for a principal with NO memories returns an empty list (exercises the missing-directory guarantee from Task 1).
- [ ] **Step 4: Run — expect FAIL** (classes absent). `./gradlew :server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-repository:platform-ai-auto-memory-repository-file-storage:test --console=plain` → `echo "EXIT=$?"`.
- [ ] **Step 5: Implement both repositories.** Queries: build the directory prefix from the tuple → `fileStorageService.getFileEntries(dir)` → read each via `readFileToString` → deserialize → filter remaining predicates → `sort(Comparator.comparing(AiAutoMemory::getUpdatedAt).reversed())`. `save()` assigns a UUID-derived positive `long` id when `getId() == null` (never `hashCode()`), sets `createdAt`/`updatedAt`, and writes the whole object. `findById`/`deleteById` resolve by listing the tenant subtree and matching id (document the cost in javadoc). `FileStorageWorkspaceAiAutoMemoryRepository` derives membership from the documents rather than a second collection.
- [ ] **Step 6: Run — expect PASS.** Same command as Step 4.
- [ ] **Step 7: Commit.** `spotlessApply`, then
  `git commit -m "Add file-storage backed auto-memory repository"`

---

### Task 4: Provider wiring with fail-fast

**Files:**
- Create: `.../platform-ai-auto-memory-repository-file-storage/src/main/java/com/bytechef/platform/ai/auto/memory/repository/filestorage/config/AiAutoMemoryFileStorageConfiguration.java`
- Modify: `.../platform-ai-auto-memory-repository-jdbc/src/main/java/com/bytechef/platform/ai/auto/memory/repository/jdbc/config/AiAutoMemoryJdbcRepositoryConfiguration.java`
- Test: `.../filestorage/config/AiAutoMemoryFileStorageConfigurationTest.java`

**Interfaces:**
- Consumes: `ApplicationProperties`, `FileStorageServiceRegistry` (`getFileStorageService(String type)` returns **null** when absent — it does not throw).

- [ ] **Step 1: Gate the JDBC config** so exactly one implementation is active. Add to `AiAutoMemoryJdbcRepositoryConfiguration` (keeping its existing `@ConditionalOnBean(AbstractJdbcConfiguration.class)`):

```java
@ConditionalOnProperty(prefix = "bytechef.ai.auto-memory", name = "provider", havingValue = "JDBC", matchIfMissing = true)
```
`matchIfMissing = true` preserves today's default behavior for deployments that set nothing.

- [ ] **Step 2: Write the file-storage configuration**, active only for `FILESYSTEM`/`AWS`. Because `@ConditionalOnProperty` takes a single `havingValue`, use TWO `@Bean` methods (or two nested `@Configuration` classes) — one per provider value — both delegating to a shared private factory that resolves the service and fails fast:

```java
    private static AiAutoMemoryRepository createRepository(
        FileStorageServiceRegistry fileStorageServiceRegistry, Ai.AutoMemory.Provider provider) {

        FileStorageService fileStorageService = fileStorageServiceRegistry.getFileStorageService(provider.name());

        if (fileStorageService == null) {
            throw new IllegalStateException(
                "bytechef.ai.auto-memory.provider=" + provider +
                    " but no FileStorageService of type " + provider.name() +
                    " is registered. For AWS, ensure the EE file-storage-aws module is on the classpath.");
        }

        return new FileStorageAiAutoMemoryRepository(fileStorageService);
    }
```

- [ ] **Step 3: Write the config test** using `ApplicationContextRunner`: (a) default/unset → the JDBC binding is selected and no file-storage repository bean exists; (b) `FILESYSTEM` with a registered filesystem service → the file-storage repository bean exists; (c) `AWS` with NO AWS service registered → context startup FAILS with a message naming the provider (assert the failure, not a fallback).
- [ ] **Step 4: Run — expect PASS.** `./gradlew :server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-repository:platform-ai-auto-memory-repository-file-storage:test --console=plain` → `echo "EXIT=$?"`.
- [ ] **Step 5: Register the module** where the JDBC repository module is consumed so both are available for selection (grep for `platform-ai-auto-memory-repository-jdbc` in `server/apps/server-app/build.gradle.kts` and any EE app, and add the file-storage module alongside).
- [ ] **Step 6: Commit.** `spotlessApply`, then
  `git commit -m "Wire auto-memory storage provider selection with fail-fast resolution"`

---

### Task 5: Shared cross-provider contract tests

The mechanism that stops backends drifting apart.

**Files:**
- Create: `.../platform-ai-auto-memory-repository-api/src/testFixtures/java/com/bytechef/platform/ai/auto/memory/repository/AiAutoMemoryRepositoryContractTests.java` (abstract JUnit 5 base; add `java-test-fixtures` to that module's plugins if not present — otherwise place it in a shared test-support module and depend on it from both binding modules)
- Modify: `.../platform-ai-auto-memory-repository-jdbc/src/test/java/.../AiAutoMemoryRepositoryIntTest.java` to extend it
- Create: `.../platform-ai-auto-memory-repository-file-storage/src/test/java/.../FileStorageAiAutoMemoryRepositoryContractTest.java` (filesystem backing)
- Create: `.../platform-ai-auto-memory-repository-file-storage/src/test/java/.../AwsFileStorageAiAutoMemoryRepositoryContractIntTest.java` (LocalStack via Testcontainers, mirroring the existing `AwsFileStorageIntTest` setup in `file-storage-aws-impl`)

**Interfaces:**
- Produces: `abstract class AiAutoMemoryRepositoryContractTests` with one abstract hook `protected abstract AiAutoMemoryRepository getRepository();` (plus a workspace-repository hook if needed) and concrete `@Test` methods covering the documented contract.

- [ ] **Step 1: Write the abstract contract tests** covering exactly the documented behavior: the 4-field filter shape; `updatedAt DESC` ordering; the `memoryType` narrowing; `findAllBy...AndName` returning multiple matches; membership scoping (a memory in workspace A never appears in a workspace B query); delete/`findById` semantics; empty result for an unknown principal.
- [ ] **Step 2: Subclass for JDBC** — have the existing `AiAutoMemoryRepositoryIntTest` extend the base, keeping its current Testcontainers PG setup and any JDBC-specific tests it already has.
- [ ] **Step 3: Subclass for filesystem** — `@TempDir` + `FilesystemFileStorageService`.
- [ ] **Step 4: Subclass for AWS** — LocalStack S3 container; name the class `*IntTest` so it runs under `testIntegration` per repo convention.
- [ ] **Step 5: Run all three.** `./gradlew :server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-repository:platform-ai-auto-memory-repository-jdbc:testIntegration :server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-repository:platform-ai-auto-memory-repository-file-storage:test :server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-repository:platform-ai-auto-memory-repository-file-storage:testIntegration --console=plain` → `echo "EXIT=$?"`. All must pass. **If a backend genuinely cannot satisfy a documented behavior, STOP and report it** — do not weaken the contract test to accommodate a backend.
- [ ] **Step 6: Commit.** `spotlessApply`, then
  `git commit -m "Add cross-provider auto-memory repository contract tests"`

---

### Task 6: Documentation + full verification

**Files:**
- Modify: `CLAUDE.md` (a short subsection under the auto-memory/AI area describing the provider switch, the three values, the default, and the best-effort single-writer limitation)
- Modify: the spec's Rollout section if anything changed during implementation

- [ ] **Step 1: Document** the provider switch: `bytechef.ai.auto-memory.provider = JDBC (default) | FILESYSTEM | AWS`; AWS requires the EE `file-storage-aws` module; file backends are best-effort single-writer (last-write-wins) and there is NO migration path between providers.
- [ ] **Step 2: Full verification** across every touched module, capturing the REAL exit code (run gradle directly; `echo "EXIT=$?"` on its own line; do NOT pipe to tail):
  `./gradlew :server:libs:core:file-storage:file-storage-api:check :server:libs:core:file-storage:file-storage-filesystem-service:check :server:ee:libs:core:file-storage:file-storage-aws:file-storage-aws-impl:check :server:libs:config:app-config:check :server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-repository:platform-ai-auto-memory-repository-api:check :server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-repository:platform-ai-auto-memory-repository-jdbc:check :server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-repository:platform-ai-auto-memory-repository-file-storage:check :server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-service:check --console=plain`
- [ ] **Step 3: Assemble the monolith** to prove wiring: `./gradlew :server:apps:server-app:compileJava --console=plain` → `echo "EXIT=$?"` → BUILD SUCCESSFUL.
- [ ] **Step 4: Commit.** `git commit -m "Document auto-memory storage provider selection"`

---

## Self-Review

**Spec coverage:** harmonized `getFileEntries` + AWS off-by-one + S3 pagination → Task 1; provider property → Task 2; single CE repository, directory layout, folded membership, UUID ids, best-effort semantics → Task 3; fail-fast provider resolution + JDBC default preserved → Task 4; cross-provider contract suite (JDBC/filesystem/AWS) → Task 5; docs + verification → Task 6. Deferred items from the spec (no inter-provider migration; base64 not offered as a provider) are documented in Task 6 Step 1, not silently dropped.

**Placeholder scan:** no TBDs. The two genuine unknowns are resolved by investigation inside a task with a recorded finding: S3 pagination behavior (Task 1 Step 5) and where the test-fixtures base class can live (Task 5, with a stated fallback).

**Type consistency:** `Ai.AutoMemory.Provider {AWS, FILESYSTEM, JDBC}`, `getFileEntries(String): Set<FileEntry>`, `FileStorageServiceRegistry.getFileStorageService(String): FileStorageService` (nullable), `AiAutoMemoryRepository`/`WorkspaceAiAutoMemoryRepository`, `FileStorageAiAutoMemoryRepository`, `AiAutoMemoryDocument` are used consistently across tasks. Domain accessor names were verified against the source, including the `Environment`-typed `getEnvironment()` vs `long getEnvironmentId()` distinction that the document type must respect.
