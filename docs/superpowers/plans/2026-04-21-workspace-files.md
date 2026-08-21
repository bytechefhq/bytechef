# Workspace Files Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a workspace-scoped Files capability in ByteChef with upload/view/edit/download, plus conversational AI generation via the existing Copilot — parallel to Knowledge Bases and Data Tables.

**Architecture:** CE modules under `server/libs/automation/automation-workspace-file/*` provide the entity, storage, CRUD, REST, and GraphQL surface. EE module under `server/ee/libs/automation/automation-workspace-file/automation-workspace-file-ai/*` adds a `CreateWorkspaceFileToolCallback` registered globally across Copilot agents via `@ConditionalOnEEVersion`. A new `Source.FILES` with `FilesSpringAIAgent` lives in `ai-copilot-app`. Client side: Files panel at `/automation/workspace-files` with Monaco-based editor and Copilot tool-result interception to prepend newly-generated files.

**Tech Stack:** Java 25, Spring Boot 4, Spring Data JDBC, Spring AI, Liquibase, PostgreSQL, Apache Tika, Micrometer, React 19, TypeScript 5.9, Apollo Client, Zustand, Monaco, Vitest, Playwright, Testcontainers.

**Spec:** `docs/superpowers/specs/2026-04-21-workspace-files-design.md`

**GitHub issue:** https://github.com/bytechefhq/bytechef/issues/4815

---

## Execution guidance

- **TDD** throughout. Write the failing test first, run it to see it fail, write the minimal implementation, run to see it pass, commit.
- **Frequent commits.** Every task ends with a commit. Commit message format: `4815 <description>` for server; `4815 client - <description>` for client (per `CLAUDE.md`).
- **Before each server commit:** `./gradlew spotlessApply`. Before each client commit: `npm run format`.
- **After each task:** run the task's test subset. After each phase: run `./gradlew check` (server) or `cd client && npm run check` (client).
- **Pattern references.** When a task says "mirror `X.java`", open that file, copy it, and change what's called out. Don't skip this — the mirrored files carry Spotless formatting and author headers that must be preserved.
- **Branch.** Create branch `4815-workspace-files` off `master`. Do not build on the `workspace-files-design-spec` branch that hosts the spec.

---

## File Structure

### New server modules (CE)

```
server/libs/automation/automation-workspace-file/
├── automation-workspace-file-api/
│   ├── build.gradle.kts
│   └── src/main/java/com/bytechef/automation/workspacefile/
│       ├── domain/
│       │   ├── WorkspaceFile.java
│       │   ├── WorkspaceFileSource.java
│       │   ├── WorkspaceFileTag.java
│       │   └── WorkspaceWorkspaceFile.java
│       └── service/
│           ├── WorkspaceFileFacade.java
│           ├── WorkspaceFileService.java
│           └── WorkspaceFileTagService.java
├── automation-workspace-file-file-storage/
│   ├── build.gradle.kts
│   └── src/main/java/com/bytechef/automation/workspacefile/file/storage/
│       ├── WorkspaceFileFileStorage.java
│       └── WorkspaceFileFileStorageImpl.java
├── automation-workspace-file-service/
│   ├── build.gradle.kts
│   └── src/main/java/com/bytechef/automation/workspacefile/
│       ├── config/
│       │   ├── AutomationWorkspaceFileJdbcRepositoryConfiguration.java
│       │   ├── AutomationWorkspaceFileQuotaProperties.java
│       │   └── AutomationWorkspaceFileOrphanCleanupProperties.java
│       ├── exception/
│       │   └── WorkspaceFileQuotaExceededException.java
│       ├── metric/
│       │   └── WorkspaceFileMetrics.java
│       ├── repository/
│       │   ├── WorkspaceFileRepository.java
│       │   ├── WorkspaceFileTagRepository.java
│       │   └── WorkspaceWorkspaceFileRepository.java
│       ├── scheduler/
│       │   └── WorkspaceFileOrphanBlobCleaner.java
│       ├── search/
│       │   └── WorkspaceFileSearchAssetProvider.java
│       ├── service/
│       │   ├── WorkspaceFileFacadeImpl.java
│       │   ├── WorkspaceFileServiceImpl.java
│       │   └── WorkspaceFileTagServiceImpl.java
│       └── util/
│           └── WorkspaceFileNameSanitizer.java
│   └── src/main/resources/
│       ├── META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
│       └── config/liquibase/changelog/automation/workspace_file/
│           └── 00000000000001_automation_workspace_file_init.xml
├── automation-workspace-file-rest/
│   ├── build.gradle.kts
│   └── src/main/java/com/bytechef/automation/workspacefile/web/rest/
│       └── WorkspaceFileRestController.java
└── automation-workspace-file-graphql/
    ├── build.gradle.kts
    └── src/main/java/com/bytechef/automation/workspacefile/web/graphql/
        └── WorkspaceFileGraphQlController.java
    └── src/main/resources/graphql/workspacefile.graphqls
```

### New server modules (EE)

```
server/ee/libs/automation/automation-workspace-file/
└── automation-workspace-file-ai/
    └── automation-workspace-file-ai-service/
        ├── build.gradle.kts
        └── src/main/java/com/bytechef/automation/workspacefile/ai/tool/
            ├── CreateWorkspaceFileToolCallback.java
            ├── GetWorkspaceFileContentToolCallback.java
            └── ListWorkspaceFilesToolCallback.java
```

### Modifications to existing files

- `settings.gradle.kts` — register new modules
- `server/ee/apps/ai-copilot-app/.../Source.java` — add `FILES`
- `server/ee/apps/ai-copilot-app/.../CopilotApiController.java` — dispatch `FILES → filesSpringAIAgent`
- `server/ee/apps/ai-copilot-app/.../agent/FilesSpringAIAgent.java` — new agent class
- `server/ee/apps/ai-copilot-app/.../agent/WorkflowEditorSpringAIAgent.java` (and siblings) — accept `List<ToolCallback>` from `ObjectProvider`
- `server/apps/server-app/build.gradle.kts` — depend on new CE modules

### New client files

```
client/src/
├── pages/automation/workspace-files/
│   ├── WorkspaceFilesPage.tsx
│   ├── WorkspaceFilesPage.test.tsx
│   ├── components/
│   │   ├── WorkspaceFileDetailSheet.tsx
│   │   ├── WorkspaceFileDetailSheet.test.tsx
│   │   ├── WorkspaceFileRow.tsx
│   │   └── WorkspaceFileUploadZone.tsx
│   ├── hooks/
│   │   ├── useWorkspaceFileUpload.ts
│   │   └── useWorkspaceFileUpload.test.ts
│   └── stores/
│       ├── useWorkspaceFilesStore.ts
│       └── useWorkspaceFilesStore.test.ts
├── graphql/workspace-files/
│   ├── getWorkspaceFile.graphql
│   ├── getWorkspaceFileTextContent.graphql
│   ├── getWorkspaceFileTags.graphql
│   ├── getWorkspaceFiles.graphql
│   ├── updateWorkspaceFile.graphql
│   ├── updateWorkspaceFileTags.graphql
│   ├── updateWorkspaceFileTextContent.graphql
│   └── deleteWorkspaceFile.graphql
└── (modifications)
    ├── shared/middleware/graphql.ts (regenerated)
    ├── codegen.ts (add schema source)
    ├── App.tsx (route)
    ├── layouts/automation/AutomationSidebar.tsx (Files entry)
    └── shared/copilot/CopilotRuntimeProvider.tsx (onToolResult)
```

Playwright E2E:
```
client/playwright/e2e/workspace-files.spec.ts
```

---

## Phase 1 — CE API module & domain

### Task 1.1: Register new module in `settings.gradle.kts` & scaffold `-api`

**Files:**
- Modify: `settings.gradle.kts:~85` (alphabetical block after `automation-data-table-*`)
- Create: `server/libs/automation/automation-workspace-file/automation-workspace-file-api/build.gradle.kts`

- [ ] **Step 1: Add module include** — edit `settings.gradle.kts`, after line 86, add:
  ```kotlin
  include("server:libs:automation:automation-workspace-file:automation-workspace-file-api")
  include("server:libs:automation:automation-workspace-file:automation-workspace-file-file-storage")
  include("server:libs:automation:automation-workspace-file:automation-workspace-file-graphql")
  include("server:libs:automation:automation-workspace-file:automation-workspace-file-rest")
  include("server:libs:automation:automation-workspace-file:automation-workspace-file-service")
  ```

- [ ] **Step 2: Create `-api` build.gradle.kts** with contents:
  ```kotlin
  dependencies {
      api("org.springframework.data:spring-data-commons")
      api(project(":server:libs:core:file-storage:file-storage-api"))
      api(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
      api(project(":server:libs:platform:platform-tag:platform-tag-api"))

      implementation("org.springframework.data:spring-data-jdbc")
      implementation(project(":server:libs:core:commons:commons-util"))
  }
  ```

- [ ] **Step 3: Verify gradle sync** — run `./gradlew :server:libs:automation:automation-workspace-file:automation-workspace-file-api:tasks` — expected: non-empty task list, no errors.

- [ ] **Step 4: Commit.**
  ```bash
  ./gradlew spotlessApply
  git add settings.gradle.kts server/libs/automation/automation-workspace-file/automation-workspace-file-api/build.gradle.kts
  git commit -m "4815 Scaffold automation-workspace-file-api module"
  ```

### Task 1.2: `WorkspaceFileSource` enum

**Files:**
- Create: `server/libs/automation/automation-workspace-file/automation-workspace-file-api/src/main/java/com/bytechef/automation/workspacefile/domain/WorkspaceFileSource.java`

- [ ] **Step 1: Write the file.**
  ```java
  /*
   * Copyright 2026 ByteChef
   *
   * Licensed under the Apache License, Version 2.0 ...
   */
  package com.bytechef.automation.workspacefile.domain;

  /**
   * @author Ivica Cardic
   */
  public enum WorkspaceFileSource {
      USER_UPLOAD,
      AI_GENERATED;

      public static WorkspaceFileSource valueOf(short ordinal) {
          WorkspaceFileSource[] values = values();

          if (ordinal < 0 || ordinal >= values.length) {
              throw new IllegalArgumentException("Invalid WorkspaceFileSource ordinal: " + ordinal);
          }

          return values[ordinal];
      }
  }
  ```
  Use the Apache 2.0 header from `KnowledgeBase.java` verbatim (lines 1–15).

- [ ] **Step 2: Commit.**
  ```bash
  ./gradlew spotlessApply
  git add server/libs/automation/automation-workspace-file/automation-workspace-file-api/src/main/java/com/bytechef/automation/workspacefile/domain/WorkspaceFileSource.java
  git commit -m "4815 Add WorkspaceFileSource enum"
  ```

### Task 1.3: `WorkspaceFileTag` entity

**Files:**
- Create: `.../domain/WorkspaceFileTag.java`

- [ ] **Step 1: Mirror `KnowledgeBaseDocumentTag.java`** (server/libs/automation/automation-knowledge-base/automation-knowledge-base-api/src/main/java/com/bytechef/automation/knowledgebase/domain/KnowledgeBaseDocumentTag.java). Change package to `com.bytechef.automation.workspacefile.domain`, class name to `WorkspaceFileTag`, `@Table("workspace_file_tag")`. Keep the `tagId` field and single-arg constructor exactly.

- [ ] **Step 2: Commit.**
  ```bash
  ./gradlew spotlessApply
  git add <path>
  git commit -m "4815 Add WorkspaceFileTag entity"
  ```

### Task 1.4: `WorkspaceFile` entity

**Files:**
- Create: `.../domain/WorkspaceFile.java`

- [ ] **Step 1: Write the entity.** Mirror `KnowledgeBaseDocument.java` structure but:
  - `@Table("workspace_file")`
  - Fields (in this order — SpotBugs prefers alphabetical but Spotless will reorder if needed):
    ```java
    @Id
    private Long id;
    private String name;
    private String description;
    private String mimeType;
    private long sizeBytes;
    private FileEntry file;
    private short source;
    private Short generatedByAgentSource;   // nullable
    private String generatedFromPrompt;
    @MappedCollection(idColumn = "workspace_file_id")
    private Set<WorkspaceFileTag> workspaceFileTags = new HashSet<>();
    @CreatedDate private Instant createdDate;
    @CreatedBy private String createdBy;
    @LastModifiedDate private Instant lastModifiedDate;
    @LastModifiedBy private String lastModifiedBy;
    @Version private int version;
    ```
  - Getters/setters for each field.
  - `getSource(): WorkspaceFileSource` delegating to `WorkspaceFileSource.valueOf(source)`; `setSource(WorkspaceFileSource)` sets `this.source = (short) value.ordinal()`.
  - Same for `getGeneratedByAgentSource` — returns nullable `Short` (raw ordinal); add a helper `getGeneratedByAgentSourceOrdinal(): Short` — agent resolution happens in the Copilot layer, not here.
  - `getTagIds()/setTagIds(List<Long>)/setTags(List<Tag>)` exactly as in `KnowledgeBaseDocument.java` lines 159–182.
  - `equals`/`hashCode`/`toString` on `id` as in `KnowledgeBaseDocument.java` lines 184–217.

- [ ] **Step 2: Commit.**
  ```bash
  ./gradlew spotlessApply
  git add <path>
  git commit -m "4815 Add WorkspaceFile entity"
  ```

### Task 1.5: `WorkspaceWorkspaceFile` join entity

**Files:**
- Create: `.../domain/WorkspaceWorkspaceFile.java`

- [ ] **Step 1: Mirror `WorkspaceKnowledgeBase.java` verbatim.** Change package, class name, `@Table("workspace_workspace_file")`, and rename the foreign-key field `knowledgeBaseId` → `workspaceFileId` with `@Column("workspace_file_id")`. Two-arg constructor `(Long workspaceFileId, Long workspaceId)`.

- [ ] **Step 2: Commit.**
  ```bash
  ./gradlew spotlessApply
  git add <path>
  git commit -m "4815 Add WorkspaceWorkspaceFile join entity"
  ```

### Task 1.6: Service interfaces

**Files:**
- Create: `.../service/WorkspaceFileService.java`
- Create: `.../service/WorkspaceFileTagService.java`
- Create: `.../service/WorkspaceFileFacade.java`

- [ ] **Step 1: `WorkspaceFileService.java`**
  ```java
  public interface WorkspaceFileService {
      WorkspaceFile create(WorkspaceFile workspaceFile, Long workspaceId);
      void delete(Long id);
      Optional<WorkspaceFile> fetchByWorkspaceIdAndName(Long workspaceId, String name);
      List<WorkspaceFile> findAllByWorkspaceId(Long workspaceId, List<Long> tagIds);
      WorkspaceFile findById(Long id);
      long sumSizeBytesByWorkspaceId(Long workspaceId);
      WorkspaceFile update(WorkspaceFile workspaceFile);
  }
  ```

- [ ] **Step 2: `WorkspaceFileTagService.java`** — mirror `DataTableTagService.java` signature (method set: `fetchAll`, `create`, `delete`).

- [ ] **Step 3: `WorkspaceFileFacade.java`**
  ```java
  public interface WorkspaceFileFacade {
      WorkspaceFile createFromUpload(Long workspaceId, String filename, String contentType, InputStream data);
      WorkspaceFile createFromAi(
          Long workspaceId, String filename, String contentType, String content,
          Short generatedByAgentSource, String generatedFromPrompt);
      void delete(Long id);
      InputStream downloadContent(Long id);
      List<WorkspaceFile> findAllByWorkspaceId(Long workspaceId, List<Long> tagIds);
      WorkspaceFile findById(Long id);
      WorkspaceFile rename(Long id, String newName);
      WorkspaceFile updateContent(Long id, String contentType, InputStream data);
      WorkspaceFile updateTags(Long id, List<Long> tagIds);
  }
  ```

- [ ] **Step 4: Commit.**
  ```bash
  ./gradlew spotlessApply
  git add server/libs/automation/automation-workspace-file/automation-workspace-file-api/src/main/java/com/bytechef/automation/workspacefile/service/
  git commit -m "4815 Add WorkspaceFile service interfaces"
  ```

### Task 1.7: Compile `-api` module

- [ ] **Step 1: Build.** `./gradlew :server:libs:automation:automation-workspace-file:automation-workspace-file-api:compileJava`. Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: If failures, fix imports / type names** until clean.

---

## Phase 2 — CE file storage wrapper

### Task 2.1: Scaffold `-file-storage` module

**Files:**
- Create: `.../automation-workspace-file-file-storage/build.gradle.kts`

- [ ] **Step 1: Write build.gradle.kts** — mirror `server/libs/automation/automation-knowledge-base/automation-knowledge-base-file-storage/build.gradle.kts`. Dependencies on `automation-workspace-file-api` and `core/file-storage/file-storage-api`.

- [ ] **Step 2: Commit.** `4815 Scaffold automation-workspace-file-file-storage module`.

### Task 2.2: `WorkspaceFileFileStorage` interface

**Files:**
- Create: `.../file/storage/WorkspaceFileFileStorage.java`

- [ ] **Step 1: Write interface.**
  ```java
  public interface WorkspaceFileFileStorage {
      void deleteFile(FileEntry fileEntry);
      InputStream getInputStream(FileEntry fileEntry);
      FileEntry storeFile(String filename, InputStream data);
      FileEntry storeFile(String filename, String content);
  }
  ```

- [ ] **Step 2: Commit.** `4815 Add WorkspaceFileFileStorage interface`.

### Task 2.3: `WorkspaceFileFileStorageImpl` (TDD)

**Files:**
- Create: `.../file/storage/WorkspaceFileFileStorageImpl.java`
- Create: `.../src/test/java/.../WorkspaceFileFileStorageImplTest.java`

- [ ] **Step 1: Write failing test** `WorkspaceFileFileStorageTest.java`:
  ```java
  @ExtendWith(MockitoExtension.class)
  class WorkspaceFileFileStorageTest {
      @Mock FileStorageService fileStorageService;
      WorkspaceFileFileStorage fileStorage;

      @BeforeEach
      void setUp() {
          fileStorage = new WorkspaceFileFileStorageImpl(fileStorageService);
      }

      @Test
      void testStoreFileString() {
          FileEntry expected = new FileEntry("spec.md", "url");
          when(fileStorageService.storeFileContent("workspace-files", "spec.md", "hello", true))
              .thenReturn(expected);

          FileEntry actual = fileStorage.storeFile("spec.md", "hello");

          assertThat(actual).isEqualTo(expected);
      }
  }
  ```

- [ ] **Step 2: Run test, expect FAIL** (class doesn't exist).

- [ ] **Step 3: Implement.**
  ```java
  public class WorkspaceFileFileStorageImpl implements WorkspaceFileFileStorage {
      static final String DIRECTORY = "workspace-files";
      private final FileStorageService fileStorageService;

      public WorkspaceFileFileStorageImpl(FileStorageService fileStorageService) {
          this.fileStorageService = fileStorageService;
      }

      @Override public void deleteFile(FileEntry fileEntry) { fileStorageService.deleteFile(DIRECTORY, fileEntry); }
      @Override public InputStream getInputStream(FileEntry fileEntry) { return fileStorageService.getInputStream(DIRECTORY, fileEntry); }
      @Override public FileEntry storeFile(String filename, InputStream data) { return fileStorageService.storeFileContent(DIRECTORY, filename, data, true); }
      @Override public FileEntry storeFile(String filename, String content) { return fileStorageService.storeFileContent(DIRECTORY, filename, content, true); }
  }
  ```

- [ ] **Step 4: Run test, expect PASS.**

- [ ] **Step 5: Commit.** `4815 Add WorkspaceFileFileStorage implementation`.

---

## Phase 3 — CE service layer

### Task 3.1: Scaffold `-service` module & Liquibase migration

**Files:**
- Create: `.../automation-workspace-file-service/build.gradle.kts`
- Create: `.../src/main/resources/config/liquibase/changelog/automation/workspace_file/00000000000001_automation_workspace_file_init.xml`
- Modify: a central `master.xml` or aggregator that includes this changelog (locate via `grep -r "knowledge_base_init" server/libs/config`)

- [ ] **Step 1: Write build.gradle.kts** mirroring `automation-data-table-service/build.gradle.kts`. Key dependencies:
  ```kotlin
  dependencies {
      annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

      api(project(":server:libs:automation:automation-workspace-file:automation-workspace-file-api"))
      api(project(":server:libs:automation:automation-workspace-file:automation-workspace-file-file-storage"))
      api(project(":server:libs:platform:platform-search:platform-search-api"))

      implementation("io.micrometer:micrometer-core")
      implementation("org.apache.tika:tika-core")
      implementation("org.springframework:spring-tx")
      implementation("org.springframework.boot:spring-boot")
      implementation("org.springframework.boot:spring-boot-autoconfigure")
      implementation("org.springframework.data:spring-data-jdbc")
      implementation(project(":server:libs:core:commons:commons-util"))
      implementation(project(":server:libs:core:file-storage:file-storage-api"))

      testImplementation(project(":server:libs:test:test-int-support"))
      testImplementation("org.testcontainers:postgresql")
  }
  ```

- [ ] **Step 2: Write the Liquibase changelog.** Single changeSet with three tables:
  ```xml
  <changeSet id="00000000000001" author="Ivica Cardic">
      <createTable tableName="workspace_file">
          <column name="id" type="BIGINT" autoIncrement="true" startWith="1050">
              <constraints primaryKey="true" nullable="false"/>
          </column>
          <column name="name" type="VARCHAR(256)"><constraints nullable="false"/></column>
          <column name="description" type="TEXT"/>
          <column name="mime_type" type="VARCHAR(128)"><constraints nullable="false"/></column>
          <column name="size_bytes" type="BIGINT"><constraints nullable="false"/></column>
          <column name="file" type="TEXT"><constraints nullable="false"/></column>
          <column name="source" type="SMALLINT"><constraints nullable="false"/></column>
          <column name="generated_by_agent_source" type="SMALLINT"/>
          <column name="generated_from_prompt" type="TEXT"/>
          <column name="created_date" type="TIMESTAMP"><constraints nullable="false"/></column>
          <column name="created_by" type="VARCHAR(50)"><constraints nullable="false"/></column>
          <column name="last_modified_date" type="TIMESTAMP"><constraints nullable="false"/></column>
          <column name="last_modified_by" type="VARCHAR(50)"><constraints nullable="false"/></column>
          <column name="version" type="BIGINT"><constraints nullable="false"/></column>
      </createTable>

      <createTable tableName="workspace_file_tag">
          <column name="workspace_file_id" type="BIGINT"><constraints nullable="false"/></column>
          <column name="tag_id" type="BIGINT"><constraints nullable="false"/></column>
      </createTable>
      <addPrimaryKey tableName="workspace_file_tag" columnNames="workspace_file_id,tag_id"/>
      <addForeignKeyConstraint baseTableName="workspace_file_tag" baseColumnNames="workspace_file_id"
          referencedTableName="workspace_file" referencedColumnNames="id"
          constraintName="fk_workspace_file_tag_file" onDelete="CASCADE"/>
      <addForeignKeyConstraint baseTableName="workspace_file_tag" baseColumnNames="tag_id"
          referencedTableName="tag" referencedColumnNames="id"
          constraintName="fk_workspace_file_tag_tag" onDelete="CASCADE"/>

      <createTable tableName="workspace_workspace_file">
          <column name="id" type="BIGINT" autoIncrement="true" startWith="1050">
              <constraints primaryKey="true" nullable="false"/>
          </column>
          <column name="workspace_id" type="BIGINT"><constraints nullable="false"/></column>
          <column name="workspace_file_id" type="BIGINT"><constraints nullable="false"/></column>
          <column name="created_date" type="TIMESTAMP"><constraints nullable="false"/></column>
          <column name="created_by" type="VARCHAR(50)"><constraints nullable="false"/></column>
          <column name="last_modified_date" type="TIMESTAMP"><constraints nullable="false"/></column>
          <column name="last_modified_by" type="VARCHAR(50)"><constraints nullable="false"/></column>
          <column name="version" type="BIGINT"><constraints nullable="false"/></column>
      </createTable>
      <addUniqueConstraint tableName="workspace_workspace_file"
          columnNames="workspace_id,workspace_file_id"
          constraintName="uk_workspace_workspace_file"/>
      <createIndex tableName="workspace_workspace_file" indexName="idx_workspace_workspace_file_workspace">
          <column name="workspace_id"/>
      </createIndex>
      <addForeignKeyConstraint baseTableName="workspace_workspace_file" baseColumnNames="workspace_file_id"
          referencedTableName="workspace_file" referencedColumnNames="id"
          constraintName="fk_workspace_workspace_file_file" onDelete="CASCADE"/>
      <addForeignKeyConstraint baseTableName="workspace_workspace_file" baseColumnNames="workspace_id"
          referencedTableName="workspace" referencedColumnNames="id"
          constraintName="fk_workspace_workspace_file_workspace" onDelete="CASCADE"/>

      <createIndex tableName="workspace_file" indexName="idx_workspace_file_name">
          <column name="name"/>
      </createIndex>
  </changeSet>
  ```
  XML header copied from `knowledge_base_init.xml`.

- [ ] **Step 3: Register in master changelog.** Run `grep -rn "knowledge_base_init" server/libs/config/liquibase-config/ 2>/dev/null` to find the aggregator file, then insert a parallel `<include file="config/liquibase/changelog/automation/workspace_file/00000000000001_automation_workspace_file_init.xml"/>` line after the knowledge_base include.

- [ ] **Step 4: Commit.** `4815 Scaffold automation-workspace-file-service module with Liquibase schema`.

### Task 3.2: Repositories

**Files:**
- Create: `.../repository/WorkspaceFileRepository.java`
- Create: `.../repository/WorkspaceFileTagRepository.java`
- Create: `.../repository/WorkspaceWorkspaceFileRepository.java`

- [ ] **Step 1: `WorkspaceFileRepository`.**
  ```java
  public interface WorkspaceFileRepository extends ListCrudRepository<WorkspaceFile, Long> {
      @Query("""
          SELECT wf.* FROM workspace_file wf
          JOIN workspace_workspace_file wwf ON wwf.workspace_file_id = wf.id
          WHERE wwf.workspace_id = :workspaceId
          ORDER BY wf.last_modified_date DESC
          """)
      List<WorkspaceFile> findAllByWorkspaceId(@Param("workspaceId") Long workspaceId);

      @Query("""
          SELECT wf.* FROM workspace_file wf
          JOIN workspace_workspace_file wwf ON wwf.workspace_file_id = wf.id
          JOIN workspace_file_tag wft ON wft.workspace_file_id = wf.id
          WHERE wwf.workspace_id = :workspaceId AND wft.tag_id IN (:tagIds)
          ORDER BY wf.last_modified_date DESC
          """)
      List<WorkspaceFile> findAllByWorkspaceIdAndTagIdsIn(
          @Param("workspaceId") Long workspaceId, @Param("tagIds") List<Long> tagIds);

      Optional<WorkspaceFile> findFirstByName(String name);

      @Query("""
          SELECT COALESCE(SUM(wf.size_bytes), 0) FROM workspace_file wf
          JOIN workspace_workspace_file wwf ON wwf.workspace_file_id = wf.id
          WHERE wwf.workspace_id = :workspaceId
          """)
      long sumSizeBytesByWorkspaceId(@Param("workspaceId") Long workspaceId);
  }
  ```

- [ ] **Step 2: `WorkspaceFileTagRepository`** — mirror `DataTableTagRepository.java`.

- [ ] **Step 3: `WorkspaceWorkspaceFileRepository`.**
  ```java
  public interface WorkspaceWorkspaceFileRepository extends ListCrudRepository<WorkspaceWorkspaceFile, Long> {
      Optional<WorkspaceWorkspaceFile> findByWorkspaceFileId(Long workspaceFileId);
  }
  ```

- [ ] **Step 4: Commit.** `4815 Add WorkspaceFile repositories`.

### Task 3.3: JDBC autoconfiguration

**Files:**
- Create: `.../config/AutomationWorkspaceFileJdbcRepositoryConfiguration.java`
- Create: `.../src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- [ ] **Step 1: Write config class.**
  ```java
  @AutoConfiguration
  @EnableJdbcRepositories(basePackages = "com.bytechef.automation.workspacefile.repository")
  @ConditionalOnBean(AbstractJdbcConfiguration.class)
  public class AutomationWorkspaceFileJdbcRepositoryConfiguration { }
  ```

- [ ] **Step 2: Register.** `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` with a single line:
  ```
  com.bytechef.automation.workspacefile.config.AutomationWorkspaceFileJdbcRepositoryConfiguration
  ```

- [ ] **Step 3: Commit.** `4815 Wire WorkspaceFile JDBC auto-configuration`.

### Task 3.4: `WorkspaceFileServiceImpl` (TDD)

**Files:**
- Create: `.../service/WorkspaceFileServiceImpl.java`
- Create: `.../src/test/java/.../WorkspaceFileServiceTest.java`

- [ ] **Step 1: Write failing tests.**
  ```java
  @ExtendWith(MockitoExtension.class)
  class WorkspaceFileServiceTest {
      @Mock WorkspaceFileRepository repository;
      @Mock WorkspaceWorkspaceFileRepository workspaceRepository;
      WorkspaceFileService service;

      @BeforeEach void setUp() {
          service = new WorkspaceFileServiceImpl(repository, workspaceRepository);
      }

      @Test
      void testCreate_persistsAndLinksToWorkspace() {
          WorkspaceFile input = new WorkspaceFile();
          input.setName("spec.md");
          WorkspaceFile saved = new WorkspaceFile();
          saved.setId(42L);
          saved.setName("spec.md");

          when(repository.save(input)).thenReturn(saved);

          WorkspaceFile result = service.create(input, 7L);

          assertThat(result.getId()).isEqualTo(42L);
          verify(workspaceRepository).save(argThat(link ->
              link.getWorkspaceId().equals(7L) && link.getWorkspaceFileId().equals(42L)));
      }

      @Test
      void testFindById_throwsWhenMissing() {
          when(repository.findById(1L)).thenReturn(Optional.empty());
          assertThatThrownBy(() -> service.findById(1L)).isInstanceOf(IllegalArgumentException.class);
      }
  }
  ```

- [ ] **Step 2: Run test, expect FAIL** (no impl yet).

- [ ] **Step 3: Implement.**
  ```java
  @Service
  @Transactional
  public class WorkspaceFileServiceImpl implements WorkspaceFileService {
      private final WorkspaceFileRepository repository;
      private final WorkspaceWorkspaceFileRepository workspaceRepository;

      public WorkspaceFileServiceImpl(
          WorkspaceFileRepository repository,
          WorkspaceWorkspaceFileRepository workspaceRepository) {
          this.repository = repository;
          this.workspaceRepository = workspaceRepository;
      }

      @Override
      public WorkspaceFile create(WorkspaceFile workspaceFile, Long workspaceId) {
          Assert.notNull(workspaceId, "workspaceId is required");

          WorkspaceFile saved = repository.save(workspaceFile);

          workspaceRepository.save(new WorkspaceWorkspaceFile(saved.getId(), workspaceId));

          return saved;
      }

      @Override public void delete(Long id) { repository.deleteById(id); }

      @Override
      public Optional<WorkspaceFile> fetchByWorkspaceIdAndName(Long workspaceId, String name) {
          return repository.findAllByWorkspaceId(workspaceId)
              .stream()
              .filter(wf -> Objects.equals(wf.getName(), name))
              .findFirst();
      }

      @Override
      @Transactional(readOnly = true)
      public List<WorkspaceFile> findAllByWorkspaceId(Long workspaceId, List<Long> tagIds) {
          if (tagIds == null || tagIds.isEmpty()) {
              return repository.findAllByWorkspaceId(workspaceId);
          }

          return repository.findAllByWorkspaceIdAndTagIdsIn(workspaceId, tagIds);
      }

      @Override
      @Transactional(readOnly = true)
      public WorkspaceFile findById(Long id) {
          return repository.findById(id)
              .orElseThrow(() -> new IllegalArgumentException("WorkspaceFile %d not found".formatted(id)));
      }

      @Override
      @Transactional(readOnly = true)
      public long sumSizeBytesByWorkspaceId(Long workspaceId) {
          return repository.sumSizeBytesByWorkspaceId(workspaceId);
      }

      @Override public WorkspaceFile update(WorkspaceFile workspaceFile) { return repository.save(workspaceFile); }
  }
  ```

- [ ] **Step 4: Run tests, expect PASS.**

- [ ] **Step 5: Commit.** `4815 Implement WorkspaceFileService`.

### Task 3.5: `WorkspaceFileTagServiceImpl`

- [ ] **Step 1: Mirror `DataTableTagServiceImpl.java`** (server/libs/automation/automation-data-table/automation-data-table-service/src/main/java/com/bytechef/automation/data/table/configuration/service/DataTableTagServiceImpl.java). Rename everything DataTable→WorkspaceFile.

- [ ] **Step 2: Add unit test** `WorkspaceFileTagServiceTest.java` covering `fetchAll` and `create`, mirroring the existing DataTable tag tests if present.

- [ ] **Step 3: Run tests.**

- [ ] **Step 4: Commit.** `4815 Implement WorkspaceFileTagService`.

### Task 3.6: Config properties & exception

**Files:**
- Create: `.../config/AutomationWorkspaceFileQuotaProperties.java`
- Create: `.../config/AutomationWorkspaceFileOrphanCleanupProperties.java`
- Create: `.../exception/WorkspaceFileQuotaExceededException.java`

- [ ] **Step 1: Quota properties.**
  ```java
  @ConfigurationProperties(prefix = "bytechef.workspace-file")
  public record AutomationWorkspaceFileQuotaProperties(
      @DefaultValue("26214400") long maxFileSizeBytes,            // 25 MB
      @DefaultValue("1073741824") long perWorkspaceTotalBytes,    // 1 GB
      @DefaultValue("1048576") long maxTextEditBytes               // 1 MB
  ) {}
  ```

- [ ] **Step 2: Orphan cleanup properties.**
  ```java
  @ConfigurationProperties(prefix = "bytechef.workspace-file.orphan-cleanup")
  public record AutomationWorkspaceFileOrphanCleanupProperties(
      @DefaultValue("true") boolean enabled
  ) {}
  ```

- [ ] **Step 3: Exception.**
  ```java
  public class WorkspaceFileQuotaExceededException extends RuntimeException {
      private final long attempted;
      private final long limit;
      public WorkspaceFileQuotaExceededException(String message, long attempted, long limit) {
          super(message);
          this.attempted = attempted;
          this.limit = limit;
      }
      public long getAttempted() { return attempted; }
      public long getLimit() { return limit; }
  }
  ```

- [ ] **Step 4: Add `@EnableConfigurationProperties({...})` to the JDBC config class** from Task 3.3.

- [ ] **Step 5: Commit.** `4815 Add WorkspaceFile quota properties and exception`.

### Task 3.7: `WorkspaceFileNameSanitizer`

**Files:**
- Create: `.../util/WorkspaceFileNameSanitizer.java`
- Create: `.../src/test/java/.../WorkspaceFileNameSanitizerTest.java`

- [ ] **Step 1: Write failing tests.**
  ```java
  class WorkspaceFileNameSanitizerTest {
      @Test void stripsPathSeparators() {
          assertThat(WorkspaceFileNameSanitizer.sanitize("../etc/passwd"))
              .isEqualTo("etcpasswd");
      }
      @Test void stripsControlChars() {
          assertThat(WorkspaceFileNameSanitizer.sanitize("ok name.md"))
              .isEqualTo("okname.md");
      }
      @Test void clampsToTwoHundredFiftyFive() {
          String input = "a".repeat(300) + ".md";
          assertThat(WorkspaceFileNameSanitizer.sanitize(input)).hasSize(255);
      }
      @Test void normalizesNfc() {
          String decomposed = "café.md";  // e + combining acute
          assertThat(WorkspaceFileNameSanitizer.sanitize(decomposed))
              .isEqualTo("café.md");
      }
      @Test void returnsUntitledWhenEmptyAfterSanitize() {
          assertThat(WorkspaceFileNameSanitizer.sanitize("/")).isEqualTo("untitled");
      }
  }
  ```

- [ ] **Step 2: Run, expect FAIL.**

- [ ] **Step 3: Implement.**
  ```java
  public final class WorkspaceFileNameSanitizer {
      private static final Pattern PATH_SEP = Pattern.compile("[/\\\\]");
      private static final Pattern CONTROL = Pattern.compile("\\p{Cntrl}");

      private WorkspaceFileNameSanitizer() {}

      public static String sanitize(String raw) {
          if (raw == null) return "untitled";

          String cleaned = PATH_SEP.matcher(raw).replaceAll("");
          cleaned = CONTROL.matcher(cleaned).replaceAll("");
          cleaned = Normalizer.normalize(cleaned, Normalizer.Form.NFC).trim();

          if (cleaned.length() > 255) cleaned = cleaned.substring(0, 255);

          return cleaned.isEmpty() ? "untitled" : cleaned;
      }
  }
  ```

- [ ] **Step 4: Run, expect PASS.**

- [ ] **Step 5: Commit.** `4815 Add WorkspaceFileNameSanitizer`.

### Task 3.8: `WorkspaceFileMetrics`

**Files:**
- Create: `.../metric/WorkspaceFileMetrics.java`

- [ ] **Step 1: Write.**
  ```java
  @Component
  public class WorkspaceFileMetrics {
      private static final String COUNTER_NAME = "bytechef_workspace_file_create";
      private final ObjectProvider<MeterRegistry> meterRegistryProvider;

      public WorkspaceFileMetrics(ObjectProvider<MeterRegistry> meterRegistryProvider) {
          this.meterRegistryProvider = meterRegistryProvider;
      }

      public void recordCreate(WorkspaceFileSource source, String mimeType) {
          MeterRegistry registry = meterRegistryProvider.getIfAvailable();

          if (registry == null) return;

          Counter.builder(COUNTER_NAME)
              .tag("source", source.name())
              .tag("mime_type_category", categorize(mimeType))
              .register(registry)
              .increment();
      }

      private String categorize(String mimeType) {
          if (mimeType == null) return "other";
          if (mimeType.startsWith("text/") || "application/json".equals(mimeType)) return "text";
          if (mimeType.startsWith("image/")) return "image";
          if ("application/pdf".equals(mimeType)) return "pdf";
          return "other";
      }
  }
  ```

- [ ] **Step 2: Commit.** `4815 Add WorkspaceFile metrics counter`.

### Task 3.9: `WorkspaceFileFacadeImpl` (TDD — largest task; split into sub-steps)

**Files:**
- Create: `.../service/WorkspaceFileFacadeImpl.java`
- Create: `.../src/test/java/.../WorkspaceFileFacadeTest.java`

- [ ] **Step 1: Write tests for `createFromUpload` — happy path.**
  ```java
  @ExtendWith(MockitoExtension.class)
  class WorkspaceFileFacadeTest {
      @Mock WorkspaceFileService service;
      @Mock WorkspaceFileFileStorage fileStorage;
      @Mock WorkspaceFileMetrics metrics;
      AutomationWorkspaceFileQuotaProperties quota =
          new AutomationWorkspaceFileQuotaProperties(1024, 10_000, 512);
      Tika tika = new Tika();
      WorkspaceFileFacade facade;

      @BeforeEach void setUp() {
          facade = new WorkspaceFileFacadeImpl(service, fileStorage, metrics, quota, tika);
      }

      @Test void testCreateFromUpload_happyPath() throws Exception {
          byte[] bytes = "# hello".getBytes();
          InputStream input = new ByteArrayInputStream(bytes);
          FileEntry stored = new FileEntry("spec.md", "url");

          when(service.sumSizeBytesByWorkspaceId(1L)).thenReturn(0L);
          when(fileStorage.storeFile(eq("spec.md"), any(InputStream.class))).thenReturn(stored);
          when(service.create(any(), eq(1L))).thenAnswer(inv -> {
              WorkspaceFile wf = inv.getArgument(0);
              wf.setId(99L);
              return wf;
          });

          WorkspaceFile result = facade.createFromUpload(1L, "spec.md", "text/markdown", input);

          assertThat(result.getId()).isEqualTo(99L);
          assertThat(result.getName()).isEqualTo("spec.md");
          assertThat(result.getSource()).isEqualTo(WorkspaceFileSource.USER_UPLOAD);
          verify(metrics).recordCreate(WorkspaceFileSource.USER_UPLOAD, result.getMimeType());
      }
  }
  ```

- [ ] **Step 2: Run, expect FAIL.**

- [ ] **Step 3: Implement `createFromUpload`.**
  ```java
  @Service
  @Transactional
  public class WorkspaceFileFacadeImpl implements WorkspaceFileFacade {
      private final WorkspaceFileService service;
      private final WorkspaceFileFileStorage fileStorage;
      private final WorkspaceFileMetrics metrics;
      private final AutomationWorkspaceFileQuotaProperties quota;
      private final Tika tika;

      public WorkspaceFileFacadeImpl(
          WorkspaceFileService service, WorkspaceFileFileStorage fileStorage,
          WorkspaceFileMetrics metrics, AutomationWorkspaceFileQuotaProperties quota, Tika tika) {
          this.service = service;
          this.fileStorage = fileStorage;
          this.metrics = metrics;
          this.quota = quota;
          this.tika = tika;
      }

      @Override
      public WorkspaceFile createFromUpload(Long workspaceId, String filename, String contentType, InputStream data) {
          String sanitized = resolveUniqueName(workspaceId, WorkspaceFileNameSanitizer.sanitize(filename));
          byte[] bytes = readAll(data);

          enforceSingleFileQuota(bytes.length);
          enforceWorkspaceQuota(workspaceId, bytes.length);

          String sniffedMime = tika.detect(bytes, sanitized);
          FileEntry stored = fileStorage.storeFile(sanitized, new ByteArrayInputStream(bytes));

          WorkspaceFile wf = new WorkspaceFile();
          wf.setName(sanitized);
          wf.setMimeType(sniffedMime);
          wf.setSizeBytes(bytes.length);
          wf.setFile(stored);
          wf.setSource(WorkspaceFileSource.USER_UPLOAD);

          WorkspaceFile saved;
          try {
              saved = service.create(wf, workspaceId);
          } catch (RuntimeException e) {
              fileStorage.deleteFile(stored);
              throw e;
          }

          metrics.recordCreate(WorkspaceFileSource.USER_UPLOAD, sniffedMime);

          return saved;
      }

      // ... (other methods implemented in subsequent sub-tasks)
  }
  ```
  Private helpers:
  ```java
  private String resolveUniqueName(Long workspaceId, String candidate) {
      String base = candidate;
      int suffix = 1;

      while (service.fetchByWorkspaceIdAndName(workspaceId, base).isPresent()) {
          suffix++;
          base = appendSuffix(candidate, suffix);
      }

      return base;
  }

  private String appendSuffix(String name, int suffix) {
      int dot = name.lastIndexOf('.');

      if (dot <= 0) return name + "-" + suffix;

      return name.substring(0, dot) + "-" + suffix + name.substring(dot);
  }

  private byte[] readAll(InputStream in) {
      try (in) {
          return in.readAllBytes();
      } catch (IOException e) {
          throw new UncheckedIOException(e);
      }
  }

  private void enforceSingleFileQuota(long bytes) {
      if (bytes > quota.maxFileSizeBytes()) {
          throw new WorkspaceFileQuotaExceededException(
              "File size %d exceeds single-file limit %d".formatted(bytes, quota.maxFileSizeBytes()),
              bytes, quota.maxFileSizeBytes());
      }
  }

  private void enforceWorkspaceQuota(Long workspaceId, long additionalBytes) {
      if (quota.perWorkspaceTotalBytes() < 0) return;
      long current = service.sumSizeBytesByWorkspaceId(workspaceId);

      if (current + additionalBytes > quota.perWorkspaceTotalBytes()) {
          throw new WorkspaceFileQuotaExceededException(
              "Workspace %d total %d + %d would exceed %d"
                  .formatted(workspaceId, current, additionalBytes, quota.perWorkspaceTotalBytes()),
              current + additionalBytes, quota.perWorkspaceTotalBytes());
      }
  }
  ```

- [ ] **Step 4: Run, expect PASS.**

- [ ] **Step 5: Add tests for quota rejection.**
  ```java
  @Test void testCreateFromUpload_rejectsWhenSingleFileOverLimit() {
      byte[] bytes = new byte[2048]; // quota.maxFileSizeBytes == 1024
      assertThatThrownBy(() -> facade.createFromUpload(
          1L, "big.txt", "text/plain", new ByteArrayInputStream(bytes)))
          .isInstanceOf(WorkspaceFileQuotaExceededException.class);
  }

  @Test void testCreateFromUpload_rejectsWhenWorkspaceTotalOver() {
      when(service.sumSizeBytesByWorkspaceId(1L)).thenReturn(9_999L);
      byte[] bytes = new byte[2];
      assertThatThrownBy(() -> facade.createFromUpload(
          1L, "tiny.txt", "text/plain", new ByteArrayInputStream(bytes)))
          .isInstanceOf(WorkspaceFileQuotaExceededException.class);
  }
  ```
  Run, expect PASS (already covered by the impl).

- [ ] **Step 6: Add test + impl for `createFromAi`.**
  ```java
  @Test void testCreateFromAi_usesStringStorageAndSetsProvenance() {
      when(service.sumSizeBytesByWorkspaceId(1L)).thenReturn(0L);
      FileEntry stored = new FileEntry("runbook.md", "url");
      when(fileStorage.storeFile(eq("runbook.md"), eq("# runbook"))).thenReturn(stored);
      when(service.create(any(), eq(1L))).thenAnswer(inv -> {
          WorkspaceFile wf = inv.getArgument(0);
          wf.setId(123L);
          return wf;
      });

      WorkspaceFile result = facade.createFromAi(
          1L, "runbook.md", "text/markdown", "# runbook", (short) 0, "write a runbook");

      assertThat(result.getSource()).isEqualTo(WorkspaceFileSource.AI_GENERATED);
      assertThat(result.getGeneratedFromPrompt()).isEqualTo("write a runbook");
  }
  ```
  Impl:
  ```java
  @Override
  public WorkspaceFile createFromAi(
      Long workspaceId, String filename, String contentType, String content,
      Short generatedByAgentSource, String generatedFromPrompt) {
      String sanitized = resolveUniqueName(workspaceId, WorkspaceFileNameSanitizer.sanitize(filename));
      byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

      enforceSingleFileQuota(bytes.length);
      enforceWorkspaceQuota(workspaceId, bytes.length);

      FileEntry stored = fileStorage.storeFile(sanitized, content);

      WorkspaceFile wf = new WorkspaceFile();
      wf.setName(sanitized);
      wf.setMimeType(contentType);
      wf.setSizeBytes(bytes.length);
      wf.setFile(stored);
      wf.setSource(WorkspaceFileSource.AI_GENERATED);
      wf.setGeneratedByAgentSource(generatedByAgentSource);
      wf.setGeneratedFromPrompt(generatedFromPrompt);

      WorkspaceFile saved;
      try {
          saved = service.create(wf, workspaceId);
      } catch (RuntimeException e) {
          fileStorage.deleteFile(stored);
          throw e;
      }

      metrics.recordCreate(WorkspaceFileSource.AI_GENERATED, contentType);

      return saved;
  }
  ```

- [ ] **Step 7: Implement remaining methods.**
  - `updateContent(id, contentType, data)`: read bytes, enforce quotas against *delta*, Tika-sniff, store new `FileEntry`, delete old one, update entity, save.
  - `downloadContent(id)`: `fileStorage.getInputStream(service.findById(id).getFile())`.
  - `rename(id, newName)`: sanitize, resolve unique name, set, save.
  - `updateTags(id, tagIds)`: load, `setTagIds(tagIds)`, save.
  - `delete(id)`: load, delete blob, delete row.
  - `findById`/`findAllByWorkspaceId`: delegate to service.

- [ ] **Step 8: Tests for each remaining method.** One happy-path test per method, plus these specific edge cases:
  - `updateContent`: write a test where existing file size + new size – old size would exceed the workspace quota, assert `WorkspaceFileQuotaExceededException`. Example:
    ```java
    @Test void testUpdateContent_enforcesDeltaQuota() {
        WorkspaceFile existing = new WorkspaceFile();
        existing.setId(5L);
        existing.setSizeBytes(900);
        existing.setFile(new FileEntry("a.md", "url"));
        when(service.findById(5L)).thenReturn(existing);
        when(service.sumSizeBytesByWorkspaceId(any())).thenReturn(9_000L);   // total=9000; delta=+5000 over 10000 cap
        byte[] bigger = new byte[5_900];
        assertThatThrownBy(() -> facade.updateContent(5L, "text/plain", new ByteArrayInputStream(bigger)))
            .isInstanceOf(WorkspaceFileQuotaExceededException.class);
    }
    ```
  - `rename`: write a test with a colliding name; assert the resulting entity's name is suffixed (`foo-2.md`).
  - `delete`: verify `fileStorage.deleteFile(fileEntry)` is called AND `service.delete(id)` is called; verify they occur in that order (`InOrder` from Mockito).
  - `downloadContent`: verify the returned `InputStream` yields the bytes that were stored (round-trip via mocked `fileStorage.getInputStream`).
  - `updateTags`: verify `setTagIds(tagIds)` and `service.update(...)` called with the updated entity.
  - `findById` / `findAllByWorkspaceId`: delegation tests — verify the call passes through to `service`.

- [ ] **Step 9: Run full `WorkspaceFileFacadeTest` — expect green.**

- [ ] **Step 10: Commit.** `4815 Implement WorkspaceFileFacade with quota, sanitization, and Tika sniffing`.

### Task 3.10: Integration test — `WorkspaceFileFacadeIntTest`

**Files:**
- Create: `.../src/test/java/.../WorkspaceFileFacadeIntTest.java`
- Create: `.../src/test/resources/config/application-testint.yml` (if not already present for this module)

- [ ] **Step 1: Test class scaffold.**
  ```java
  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
  @ActiveProfiles("testint")
  @Testcontainers
  class WorkspaceFileFacadeIntTest {
      @Container @ServiceConnection
      static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15");

      @Autowired WorkspaceFileFacade facade;
      @Autowired WorkspaceFileService service;

      @Test void testUploadDownloadRoundTrip() throws Exception {
          WorkspaceFile created = facade.createFromUpload(
              1L, "spec.md", "text/markdown",
              new ByteArrayInputStream("# hi".getBytes()));

          assertThat(created.getId()).isNotNull();
          assertThat(new String(facade.downloadContent(created.getId()).readAllBytes()))
              .isEqualTo("# hi");
      }

      @Test void testQuotaEnforcedAtWorkspaceLevel() { /* ... */ }
      @Test void testDeleteRemovesBlobAndRow() { /* ... */ }
      @Test void testRenameCollisionSuffixes() { /* ... */ }
      @Test void testFilenameSanitized() { /* ... */ }
  }
  ```

- [ ] **Step 2: Configure minimal testint application YAML** pointing at the Testcontainers Postgres; enable file-storage provider `base64` or `jdbc`.

- [ ] **Step 3: Run.** `./gradlew :server:libs:automation:automation-workspace-file:automation-workspace-file-service:testIntegration`. Expect PASS.

- [ ] **Step 4: Commit.** `4815 Add WorkspaceFileFacade integration tests`.

### Task 3.11: Search asset provider

- [ ] **Step 1: Mirror `DataTableSearchAssetProvider.java`** with entity `WorkspaceFile` and path `/automation/workspace-files/{id}`.

- [ ] **Step 2: Integration smoke test** via existing search asset test pattern (if present).

- [ ] **Step 3: Commit.** `4815 Add WorkspaceFileSearchAssetProvider`.

### Task 3.12: Orphan blob cleaner

**Files:**
- Create: `.../scheduler/WorkspaceFileOrphanBlobCleaner.java`

- [ ] **Step 1: Write.**
  ```java
  @Component
  @ConditionalOnProperty(prefix = "bytechef.workspace-file.orphan-cleanup", name = "enabled", havingValue = "true", matchIfMissing = true)
  public class WorkspaceFileOrphanBlobCleaner {
      private static final Logger log = LoggerFactory.getLogger(WorkspaceFileOrphanBlobCleaner.class);
      private final WorkspaceFileService service;
      private final FileStorageService fileStorageService;

      public WorkspaceFileOrphanBlobCleaner(WorkspaceFileService service, FileStorageService fileStorageService) {
          this.service = service;
          this.fileStorageService = fileStorageService;
      }

      @Scheduled(fixedDelay = 60 * 60 * 1000L)   // 1 hour
      public void cleanup() {
          log.atDebug().setMessage("workspace-files orphan cleanup started").log();
          // Implementation: list blob URLs in directory, cross-check against repository, delete orphans.
      }
  }
  ```
  Note: exact listing API on `FileStorageService` may need a small extension; if `FileStorageService` has no list method, log a TODO-free WARN and skip implementation until that API is added — do NOT add a TODO comment (forbidden by Checkstyle). Instead, file a follow-up ticket now via `gh issue create`.

- [ ] **Step 2: Commit.** `4815 Add WorkspaceFileOrphanBlobCleaner scaffold`.

### Task 3.13: Phase 3 gate

- [ ] **Step 1: `./gradlew check`.** Expected: green.

- [ ] **Step 2: If red, fix and commit.** No new work; only fix regressions.

---

## Phase 4 — CE REST API

### Task 4.1: Scaffold `-rest` module

- [ ] **Step 1:** Mirror `automation-knowledge-base-rest/build.gradle.kts`. Deps: `-api`, `-service`, `org.springframework:spring-web`.

- [ ] **Step 2:** Commit `4815 Scaffold automation-workspace-file-rest module`.

### Task 4.2: `WorkspaceFileRestController` — upload

**Files:**
- Create: `.../web/rest/WorkspaceFileRestController.java`

- [ ] **Step 1: Write controller with upload endpoint.**
  ```java
  @RestController
  @RequestMapping("/api/automation/internal/workspace-files")
  public class WorkspaceFileRestController {
      private final WorkspaceFileFacade facade;

      public WorkspaceFileRestController(WorkspaceFileFacade facade) {
          this.facade = facade;
      }

      @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
      public ResponseEntity<WorkspaceFileDTO> upload(
          @RequestParam Long workspaceId,
          @RequestParam MultipartFile file) throws IOException {

          WorkspaceFile created = facade.createFromUpload(
              workspaceId, file.getOriginalFilename(), file.getContentType(), file.getInputStream());

          return ResponseEntity.status(HttpStatus.CREATED).body(WorkspaceFileDTO.from(created));
      }
  }
  ```
  `WorkspaceFileDTO` is a small record co-located in the same package.

- [ ] **Step 2: Download endpoint.**
  ```java
  @GetMapping(value = "/{id}/content")
  public ResponseEntity<StreamingResponseBody> download(@PathVariable Long id) {
      WorkspaceFile wf = facade.findById(id);

      StreamingResponseBody body = out -> facade.downloadContent(id).transferTo(out);

      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\"".formatted(wf.getName()))
          .header(HttpHeaders.CONTENT_TYPE, wf.getMimeType())
          .body(body);
  }
  ```

- [ ] **Step 3: PUT content endpoint.**
  ```java
  @PutMapping(value = "/{id}/content", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<WorkspaceFileDTO> replaceContent(
      @PathVariable Long id,
      @RequestParam MultipartFile file) throws IOException {
      WorkspaceFile updated = facade.updateContent(id, file.getContentType(), file.getInputStream());

      return ResponseEntity.ok(WorkspaceFileDTO.from(updated));
  }
  ```

- [ ] **Step 4: Exception handler** (`@ExceptionHandler(WorkspaceFileQuotaExceededException.class)`) returning 413.

- [ ] **Step 5: Commit.** `4815 Add WorkspaceFile REST controller`.

### Task 4.3: REST integration test

**Files:**
- Create: `.../src/test/java/.../WorkspaceFileRestControllerIntTest.java`

- [ ] **Step 1: Test `POST /upload` with a multipart request using `MockMvc` + Testcontainers.**
  ```java
  @Test void testUploadReturns201() throws Exception {
      MockMultipartFile file = new MockMultipartFile("file", "spec.md",
          "text/markdown", "# hi".getBytes());

      mockMvc.perform(multipart("/api/automation/internal/workspace-files/upload")
              .file(file)
              .param("workspaceId", "1"))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.name").value("spec.md"));
  }
  ```

- [ ] **Step 2: Test download (`GET /{id}/content`).**

- [ ] **Step 3: Test quota 413.**

- [ ] **Step 4: Run `./gradlew :...-rest:testIntegration`. Expect PASS.**

- [ ] **Step 5: Commit.** `4815 Add WorkspaceFile REST integration tests`.

---

## Phase 5 — CE GraphQL

### Task 5.1: Scaffold `-graphql` module

- [ ] **Step 1:** Mirror `automation-data-table-graphql/build.gradle.kts`. Deps: `-api`, `-service`, `spring-graphql`.

- [ ] **Step 2:** Commit `4815 Scaffold automation-workspace-file-graphql module`.

### Task 5.2: GraphQL schema

**Files:**
- Create: `.../src/main/resources/graphql/workspacefile.graphqls`

- [ ] **Step 1: Write schema** (copy from §4.2 of the design spec verbatim — open `docs/superpowers/specs/2026-04-21-workspace-files-design.md` lines 130–175).

- [ ] **Step 2: Add schema path to `client/codegen.ts`** `schema` array so client can typecheck against it.

- [ ] **Step 3: Commit.** `4815 Add WorkspaceFile GraphQL schema`.

### Task 5.3: `WorkspaceFileGraphQlController`

**Files:**
- Create: `.../web/graphql/WorkspaceFileGraphQlController.java`

- [ ] **Step 1: Write controller.**
  ```java
  @Controller
  public class WorkspaceFileGraphQlController {
      private final WorkspaceFileFacade facade;
      private final WorkspaceFileTagService tagService;
      private final AutomationWorkspaceFileQuotaProperties quota;
      private final WorkspaceFileFileStorage fileStorage;

      // constructor ...

      @QueryMapping
      public WorkspaceFile workspaceFile(@Argument Long id) { return facade.findById(id); }

      @QueryMapping
      public List<WorkspaceFile> workspaceFiles(
          @Argument Long workspaceId, @Argument List<Long> tagIds, @Argument String mimeTypePrefix) {
          List<WorkspaceFile> all = facade.findAllByWorkspaceId(workspaceId, tagIds);

          if (mimeTypePrefix == null) return all;

          return all.stream()
              .filter(wf -> wf.getMimeType() != null && wf.getMimeType().startsWith(mimeTypePrefix))
              .toList();
      }

      @QueryMapping
      public String workspaceFileTextContent(@Argument Long id) {
          WorkspaceFile wf = facade.findById(id);

          if (wf.getSizeBytes() > quota.maxTextEditBytes()) {
              throw new IllegalArgumentException("File too large for text editing; use download.");
          }

          try (InputStream in = facade.downloadContent(id)) {
              return new String(in.readAllBytes(), StandardCharsets.UTF_8);
          } catch (IOException e) {
              throw new UncheckedIOException(e);
          }
      }

      @MutationMapping
      public WorkspaceFile updateWorkspaceFile(@Argument UpdateWorkspaceFileInput input) {
          if (input.name() != null) facade.rename(input.id(), input.name());
          // description updates: add `setDescription` path to facade if needed
          return facade.findById(input.id());
      }

      @MutationMapping
      public WorkspaceFile updateWorkspaceFileTextContent(@Argument Long id, @Argument String content) {
          return facade.updateContent(id, "text/plain",
              new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
      }

      @MutationMapping
      public boolean deleteWorkspaceFile(@Argument Long id) {
          facade.delete(id);
          return true;
      }

      @MutationMapping
      public WorkspaceFile updateWorkspaceFileTags(@Argument Long id, @Argument List<Long> tagIds) {
          return facade.updateTags(id, tagIds);
      }

      @SchemaMapping
      public String downloadUrl(WorkspaceFile wf) {
          return "/api/automation/internal/workspace-files/%d/content".formatted(wf.getId());
      }

      @SchemaMapping
      public String source(WorkspaceFile wf) {
          return wf.getSource().name();
      }
  }
  ```

- [ ] **Step 2: Integration test** `WorkspaceFileGraphQlControllerIntTest` covering every op plus workspace-membership permission matrix (member / non-member / admin). Use `HttpGraphQlTester`. Mirror pattern from `KnowledgeBaseDocumentGraphQlControllerIntTest` if present (locate via `grep -rn "GraphQlTester" server/libs/automation/automation-knowledge-base`).

- [ ] **Step 3: Commit.** `4815 Add WorkspaceFile GraphQL controller and integration tests`.

### Task 5.4: Wire the new modules into the server app

**Files:**
- Modify: `server/apps/server-app/build.gradle.kts`

- [ ] **Step 1: Add runtime dependencies** on `-service`, `-rest`, `-graphql`.

- [ ] **Step 2: Boot the app** — `./gradlew -p server/apps/server-app bootRun` (background). Verify the app starts and `GET /actuator/health` returns 200. Kill the background process afterwards.

- [ ] **Step 3: Run smoke HTTP check** — `curl -fsS http://localhost:8080/actuator/health`.

- [ ] **Step 4: Commit.** `4815 Wire WorkspaceFile modules into server-app`.

### Task 5.5: Phase 5 gate

- [ ] **Step 1: `./gradlew check`.** Green.

---

## Phase 6 — Client groundwork

### Task 6.1: GraphQL operations

**Files:**
- Create: `client/src/graphql/workspace-files/getWorkspaceFiles.graphql`
- Create: `client/src/graphql/workspace-files/getWorkspaceFile.graphql`
- Create: `client/src/graphql/workspace-files/getWorkspaceFileTextContent.graphql`
- Create: `client/src/graphql/workspace-files/getWorkspaceFileTags.graphql`
- Create: `client/src/graphql/workspace-files/updateWorkspaceFile.graphql`
- Create: `client/src/graphql/workspace-files/updateWorkspaceFileTextContent.graphql`
- Create: `client/src/graphql/workspace-files/updateWorkspaceFileTags.graphql`
- Create: `client/src/graphql/workspace-files/deleteWorkspaceFile.graphql`

- [ ] **Step 1: `getWorkspaceFiles.graphql`.**
  ```graphql
  query GetWorkspaceFiles($workspaceId: ID!, $tagIds: [ID!], $mimeTypePrefix: String) {
      workspaceFiles(workspaceId: $workspaceId, tagIds: $tagIds, mimeTypePrefix: $mimeTypePrefix) {
          id
          name
          description
          mimeType
          sizeBytes
          source
          generatedByAgentSource
          generatedFromPrompt
          downloadUrl
          createdBy
          createdDate
          lastModifiedBy
          lastModifiedDate
          tags { id name }
      }
  }
  ```

- [ ] **Step 2: Write remaining operations** with field selections appropriate to each (use the same `WorkspaceFile` selection set for single-item queries).

- [ ] **Step 3: Regenerate types.**
  ```bash
  cd client
  npx graphql-codegen
  ```

- [ ] **Step 4: Commit.**
  ```bash
  git add client/src/graphql/workspace-files/ client/src/shared/middleware/graphql.ts client/codegen.ts
  git commit -m "4815 client - Add WorkspaceFile GraphQL operations"
  ```

### Task 6.2: `useWorkspaceFilesStore`

**Files:**
- Create: `client/src/pages/automation/workspace-files/stores/useWorkspaceFilesStore.ts`
- Create: `client/src/pages/automation/workspace-files/stores/useWorkspaceFilesStore.test.ts`

- [ ] **Step 1: Write failing test.**
  ```ts
  import {act, renderHook} from '@testing-library/react';
  import {beforeEach, describe, expect, test} from 'vitest';
  import {useWorkspaceFilesStore, workspaceFilesStore} from './useWorkspaceFilesStore';

  describe('useWorkspaceFilesStore', () => {
      beforeEach(() => {
          workspaceFilesStore.setState({searchQuery: '', selectedTagIds: [], selectedFileId: null});
      });

      test('sets search query', () => {
          const {result} = renderHook(() => useWorkspaceFilesStore());
          act(() => result.current.setSearchQuery('spec'));
          expect(workspaceFilesStore.getState().searchQuery).toBe('spec');
      });
  });
  ```

- [ ] **Step 2: Run, expect FAIL (no store).**

- [ ] **Step 3: Implement.**
  ```ts
  import {create} from 'zustand';

  interface WorkspaceFilesStateI {
      searchQuery: string;
      selectedFileId: number | null;
      selectedTagIds: number[];
      setSearchQuery: (searchQuery: string) => void;
      setSelectedFileId: (selectedFileId: number | null) => void;
      setSelectedTagIds: (selectedTagIds: number[]) => void;
  }

  export const workspaceFilesStore = create<WorkspaceFilesStateI>()((set) => ({
      searchQuery: '',
      selectedFileId: null,
      selectedTagIds: [],
      setSearchQuery: (searchQuery) => set({searchQuery}),
      setSelectedFileId: (selectedFileId) => set({selectedFileId}),
      setSelectedTagIds: (selectedTagIds) => set({selectedTagIds}),
  }));

  export const useWorkspaceFilesStore = workspaceFilesStore;
  ```

- [ ] **Step 4: Run, expect PASS.**

- [ ] **Step 5: Commit.** `4815 client - Add useWorkspaceFilesStore`.

### Task 6.3: `useWorkspaceFileUpload` hook

**Files:**
- Create: `client/src/pages/automation/workspace-files/hooks/useWorkspaceFileUpload.ts`
- Create: `client/src/pages/automation/workspace-files/hooks/useWorkspaceFileUpload.test.ts`

- [ ] **Step 1: Write test.**
  ```ts
  test('upload posts multipart with workspaceId and file', async () => {
      global.fetch = vi.fn().mockResolvedValue({
          ok: true,
          status: 201,
          json: async () => ({id: 1, name: 'spec.md'}),
      });
      const {result} = renderHook(() => useWorkspaceFileUpload());

      const file = new File(['# hi'], 'spec.md', {type: 'text/markdown'});
      const resp = await result.current.upload(42, file);

      expect(resp.id).toBe(1);
      expect(fetch).toHaveBeenCalledWith(
          '/api/automation/internal/workspace-files/upload',
          expect.objectContaining({method: 'POST', body: expect.any(FormData)})
      );
  });
  ```

- [ ] **Step 2: Run, expect FAIL.**

- [ ] **Step 3: Implement.**
  ```ts
  import {useState} from 'react';

  interface UploadResultI {
      id: number;
      mimeType: string;
      name: string;
      sizeBytes: number;
  }

  export const useWorkspaceFileUpload = () => {
      const [progress, setProgress] = useState<number>(0);

      const upload = async (workspaceId: number, file: File): Promise<UploadResultI> => {
          const formData = new FormData();
          formData.append('workspaceId', String(workspaceId));
          formData.append('file', file);

          const resp = await fetch('/api/automation/internal/workspace-files/upload', {
              body: formData,
              method: 'POST',
          });

          if (!resp.ok) throw new Error(`Upload failed: ${resp.status}`);

          setProgress(100);

          return resp.json();
      };

      return {progress, upload};
  };
  ```

- [ ] **Step 4: Commit.** `4815 client - Add useWorkspaceFileUpload hook`.

---

## Phase 7 — Client UI

### Task 7.1: `WorkspaceFilesPage` list

**Files:**
- Create: `client/src/pages/automation/workspace-files/WorkspaceFilesPage.tsx`

- [ ] **Step 1: Write tests first.**
  ```tsx
  describe('WorkspaceFilesPage', () => {
      test('renders loaded files in rows', async () => {
          // mock GraphQL → [{id:1, name:'spec.md', ...}]
          render(<MockedProvider mocks={...}><WorkspaceFilesPage /></MockedProvider>);
          expect(await screen.findByText('spec.md')).toBeInTheDocument();
      });
      test('drop opens upload flow', async () => {
          // simulate drag-drop and verify useWorkspaceFileUpload called
      });
  });
  ```

- [ ] **Step 2: Implement.** Components:
  - Top bar: title "Files", tag filter `Select`, search `Input`, `Upload` button (native file input, hidden), "Create with AI" button (opens Copilot via `openCopilotForFiles()` — defined in Task 9.x).
  - Drop zone wraps the list body; `onDrop` calls `useWorkspaceFileUpload.upload()` in parallel.
  - `TanStack Table` (or existing table component — locate via `grep -rn "DataTable" client/src/pages/automation/data-tables | head`).
  - Row actions menu: Edit (opens side sheet), Download (anchor `href={downloadUrl}`), Rename (inline dialog), Delete (confirm dialog + mutation).
  - Apollo `useGetWorkspaceFilesQuery` drives the list; `refetch` after mutations.

Mandatory client-convention checks before committing: `twMerge`, `FileTextIcon` etc. with `Icon` suffix, interfaces ending in `I`/`Props`, alphabetized imports, object keys sorted.

- [ ] **Step 3: Commit.** `4815 client - Add WorkspaceFilesPage`.

### Task 7.2: `WorkspaceFileDetailSheet`

**Files:**
- Create: `.../components/WorkspaceFileDetailSheet.tsx`

- [ ] **Step 1: Write test** — asserts Monaco renders for text, `<img>` for image, download button for other.

- [ ] **Step 2: Implement.**
  ```tsx
  const TEXT_MIME_PREFIXES = ['text/', 'application/json'];
  const isTextMime = (mime: string) => TEXT_MIME_PREFIXES.some((p) => mime.startsWith(p));

  const WorkspaceFileDetailSheet = ({fileId, open, onClose}: WorkspaceFileDetailSheetPropsType) => {
      const {data} = useGetWorkspaceFileQuery({skip: !fileId, variables: {id: fileId!}});
      const file = data?.workspaceFile;
      const {data: textContent} = useGetWorkspaceFileTextContentQuery({
          skip: !file || !isTextMime(file.mimeType),
          variables: {id: fileId!},
      });

      const [draft, setDraft] = useState<string | undefined>();
      const [updateText] = useUpdateWorkspaceFileTextContentMutation();
      const dirty = draft !== undefined && draft !== textContent?.workspaceFileTextContent;

      // Render Sheet from shadcn/ui-like library, with Monaco inside when text, <img> when image, <iframe> when PDF, else "no preview"
      // Right rail: metadata, tags, provenance (if AI_GENERATED), "Continue in Copilot" button
      // Save → updateText({variables:{id, content: draft}})
      // Dirty warning on close
  };
  ```
  Use existing Monaco import pattern — `grep -rn "from '@monaco-editor" client/src` to find the wrapper.

- [ ] **Step 3: Commit.** `4815 client - Add WorkspaceFileDetailSheet`.

### Task 7.3: Sidebar entry + route

**Files:**
- Modify: `client/src/layouts/automation/AutomationSidebar.tsx` (locate via `grep -rn "Knowledge Bases" client/src/layouts`)
- Modify: `client/src/App.tsx`

- [ ] **Step 1: Insert "Files" sidebar item** between "Knowledge Bases" and "Data Tables", with `FileTextIcon` and link to `/automation/workspace-files`.

- [ ] **Step 2: Add route** in `App.tsx`.
  ```tsx
  <Route path="/automation/workspace-files" element={<WorkspaceFilesPage />} />
  ```

- [ ] **Step 3: `npm run check`.** Green.

- [ ] **Step 4: Commit.** `4815 client - Add Files sidebar entry and route`.

### Task 7.4: Phase 7 gate

- [ ] **Step 1: `cd client && npm run check`.** Green.

- [ ] **Step 2: Start dev server, manually drag-drop a markdown file, verify it appears in the list and the side sheet Monaco shows the content.**

---

## Phase 8 — EE AI tool callback module

### Task 8.1: Scaffold EE module

**Files:**
- Modify: `settings.gradle.kts` (EE block around line 544)
- Create: `server/ee/libs/automation/automation-workspace-file/automation-workspace-file-ai/automation-workspace-file-ai-service/build.gradle.kts`

- [ ] **Step 1: Add includes.**
  ```kotlin
  include("server:ee:libs:automation:automation-workspace-file:automation-workspace-file-ai:automation-workspace-file-ai-service")
  ```

- [ ] **Step 2: Write build.gradle.kts** — mirror `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/build.gradle.kts`. Deps: CE `-api`, Spring AI (`org.springframework.ai:spring-ai-model`).

  Apply EE license header to all Java files (per `CLAUDE.md`: ByteChef Enterprise license). Add `@version ee` javadoc tag on each class.

- [ ] **Step 3: Commit.** `4815 Scaffold automation-workspace-file-ai-service EE module`.

### Task 8.2: `CreateWorkspaceFileToolCallback` (TDD)

**Files:**
- Create: `.../ai/tool/CreateWorkspaceFileToolCallback.java`
- Create: `.../src/test/java/.../CreateWorkspaceFileToolCallbackTest.java`

- [ ] **Step 1: Failing test.**
  ```java
  @ExtendWith(MockitoExtension.class)
  class CreateWorkspaceFileToolCallbackTest {
      @Mock WorkspaceFileFacade facade;
      @Mock WorkspaceContextProvider ctx;
      CreateWorkspaceFileToolCallback callback;

      @BeforeEach void setUp() {
          callback = new CreateWorkspaceFileToolCallback(facade, ctx);
      }

      @Test void testCall_happyPath() {
          when(ctx.currentWorkspaceId()).thenReturn(7L);
          when(ctx.currentSourceOrdinal()).thenReturn((short) 0);
          when(ctx.lastUserPrompt()).thenReturn("write a runbook");

          WorkspaceFile saved = new WorkspaceFile();
          saved.setId(42L);
          saved.setName("runbook.md");
          saved.setSizeBytes(10);
          when(facade.createFromAi(7L, "runbook.md", "text/markdown", "# runbook",
              (short) 0, "write a runbook")).thenReturn(saved);

          String json = """
              {"filename":"runbook.md","mimeType":"text/markdown","content":"# runbook"}
              """;
          String result = callback.call(json);

          assertThat(result).contains("\"id\":42").contains("\"name\":\"runbook.md\"");
      }

      @Test void testCall_rejectsDisallowedMime() {
          when(ctx.currentWorkspaceId()).thenReturn(7L);
          String json = """
              {"filename":"x.png","mimeType":"image/png","content":"..."}
              """;

          String result = callback.call(json);
          assertThat(result).contains("error").contains("mime type");
      }
  }
  ```

- [ ] **Step 2: Run, expect FAIL.**

- [ ] **Step 3: Implement.**
  ```java
  @Component("createWorkspaceFileToolCallback")
  @ConditionalOnEEVersion
  public class CreateWorkspaceFileToolCallback implements ToolCallback {
      private static final Set<String> ALLOWED = Set.of(
          "text/markdown", "text/csv", "text/plain", "application/json",
          "text/javascript", "text/x-python", "text/x-java",
          "text/html", "text/css", "text/yaml");

      private final WorkspaceFileFacade facade;
      private final WorkspaceContextProvider ctx;
      private final ObjectMapper objectMapper = new ObjectMapper();

      public CreateWorkspaceFileToolCallback(WorkspaceFileFacade facade, WorkspaceContextProvider ctx) {
          this.facade = facade;
          this.ctx = ctx;
      }

      @Override
      public ToolDefinition getToolDefinition() {
          return ToolDefinition.builder()
              .name("createWorkspaceFile")
              .description("""
                  Create a new text file in the user's workspace. Use this when the user asks you to
                  write, draft, or generate a document, spec, CSV, JSON, markdown note, or code file.
                  The file will appear in their Files panel. Choose a filename with an appropriate
                  extension. Supported mime types: text/markdown, text/csv, text/plain, application/json,
                  text/javascript, text/x-python, text/x-java, text/html, text/css, text/yaml.
                  """)
              .inputSchema(JsonSchemaGenerator.generate(CreateWorkspaceFileInput.class))
              .build();
      }

      @Override
      public String call(String toolInput) {
          try {
              CreateWorkspaceFileInput input = objectMapper.readValue(toolInput, CreateWorkspaceFileInput.class);

              if (!ALLOWED.contains(input.mimeType())) {
                  return toolError("Unsupported mime type: %s".formatted(input.mimeType()));
              }

              WorkspaceFile wf = facade.createFromAi(
                  ctx.currentWorkspaceId(), input.filename(), input.mimeType(), input.content(),
                  ctx.currentSourceOrdinal(), ctx.lastUserPrompt());

              return objectMapper.writeValueAsString(new CreateWorkspaceFileOutput(
                  wf.getId(), wf.getName(),
                  "/api/automation/internal/workspace-files/%d/content".formatted(wf.getId()),
                  wf.getSizeBytes()));
          } catch (WorkspaceFileQuotaExceededException e) {
              return toolError(e.getMessage());
          } catch (JsonProcessingException e) {
              return toolError("Invalid tool input: " + e.getMessage());
          }
      }

      private String toolError(String msg) {
          try { return objectMapper.writeValueAsString(Map.of("error", msg)); }
          catch (JsonProcessingException e) { return "{\"error\":\"serialization failure\"}"; }
      }

      public record CreateWorkspaceFileInput(String filename, String mimeType, String content, String description) {}
      public record CreateWorkspaceFileOutput(long id, String name, String downloadUrl, long sizeBytes) {}
  }
  ```

- [ ] **Step 4: Run, expect PASS.**

- [ ] **Step 5: Commit.** `4815 Add CreateWorkspaceFileToolCallback`.

### Task 8.3: `WorkspaceContextProvider` interface + default impl

The tool callback depends on a `WorkspaceContextProvider` that resolves current-request context.

**Files:**
- Create: `.../ai/tool/WorkspaceContextProvider.java`

- [ ] **Step 1: Interface.**
  ```java
  public interface WorkspaceContextProvider {
      Long currentWorkspaceId();
      Short currentSourceOrdinal();
      String lastUserPrompt();
  }
  ```

- [ ] **Step 2: Default Spring-AI-bridging impl** — read from `ToolContext` (Spring AI passes a per-call map). Name impl `AgUiToolContextWorkspaceContextProvider`, register `@ConditionalOnEEVersion`.

- [ ] **Step 3: Commit.** `4815 Add WorkspaceContextProvider SPI`.

### Task 8.4: `ListWorkspaceFilesToolCallback` & `GetWorkspaceFileContentToolCallback`

**Files:**
- Create: `.../ai/tool/ListWorkspaceFilesToolCallback.java`
- Create: `.../ai/tool/GetWorkspaceFileContentToolCallback.java`

- [ ] **Step 1: `List` tool** — returns the current workspace's files list (max 50, summary fields only). Input schema: empty. Output: JSON array. Only the Files agent registers this.

- [ ] **Step 2: `Get` tool** — input `{id}`. Returns text content if mime is text and size ≤ `maxTextEditBytes`, else returns an error.

- [ ] **Step 3: Unit tests for each.**

- [ ] **Step 4: Commit.** `4815 Add List and Get workspace file tool callbacks`.

### Task 8.5: Phase 8 gate

- [ ] **Step 1:** `./gradlew :server:ee:libs:automation:automation-workspace-file:automation-workspace-file-ai:automation-workspace-file-ai-service:check`. Green.

---

## Phase 9 — Copilot wiring (ai-copilot-app)

### Task 9.1: Refactor existing agents to accept `List<ToolCallback>` via `ObjectProvider`

**Files (locate with `grep -rn "SpringAIAgent" server/ee/apps/ai-copilot-app/src | head -20`):**
- Modify: `.../WorkflowEditorSpringAIAgent.java`
- Modify: `.../CodeEditorSpringAIAgent.java`
- Modify: `.../ClusterElementSpringAIAgent.java` (if exists)

- [ ] **Step 1: In each agent class**, change the field `private final ToolCallback[] tools;` (or `List<ToolCallback>`) to be constructed from:
  ```java
  public WorkflowEditorSpringAIAgent(..., ObjectProvider<ToolCallback> toolProvider) {
      // keep existing args
      this.tools = Stream.concat(
          Stream.of(existingLocalTools),
          toolProvider.orderedStream()
      ).toList();
  }
  ```
  This is additive — existing agent tools still work, and any `@Component` `ToolCallback` bean (like `CreateWorkspaceFileToolCallback`) gets appended.

- [ ] **Step 2: Run the existing agent unit tests.** Expect green — no behavioral change for existing tools.

- [ ] **Step 3: Commit.** `4815 Refactor Copilot agents to accept ObjectProvider<ToolCallback>`.

### Task 9.2: Add `Source.FILES`

**Files:**
- Modify: `server/ee/apps/ai-copilot-app/.../Source.java` (locate with `grep -rn "enum Source" server/ee/apps/ai-copilot-app`)

- [ ] **Step 1: Append `FILES` after the last existing value.**

- [ ] **Step 2: Commit.** `4815 Add Source.FILES enum value`.

### Task 9.3: `FilesSpringAIAgent`

**Files:**
- Create: `.../agent/FilesSpringAIAgent.java`

- [ ] **Step 1: Failing unit test** — verify the agent is configured with `createWorkspaceFile` + `listWorkspaceFiles` + `getWorkspaceFileContent` tools.

- [ ] **Step 2: Implement.** Extends `SpringAIAgent` (or the class that other agents extend). System prompt:
  > "You are a document authoring assistant inside ByteChef. When the user asks for a file (spec, runbook, CSV, JSON, markdown note, code file), produce the content and save it by calling `createWorkspaceFile`. Keep files concise and useful. When referring to existing files, call `listWorkspaceFiles` first; when editing, call `getWorkspaceFileContent` before `createWorkspaceFile` with the updated content under a new filename."

  Tool set: `CreateWorkspaceFileToolCallback` + `ListWorkspaceFilesToolCallback` + `GetWorkspaceFileContentToolCallback`.

  State injection into `AgUiParameters.state`: current `workspaceId` plus recent files list (max 50) produced by calling `WorkspaceFileFacade.findAllByWorkspaceId(...)`.

- [ ] **Step 3: Commit.** `4815 Add FilesSpringAIAgent`.

### Task 9.4: `CopilotApiController` dispatcher case

**Files:**
- Modify: `.../CopilotApiController.java`

- [ ] **Step 1: Add case** in the agent dispatch switch:
  ```java
  case FILES -> filesSpringAIAgent;
  ```

- [ ] **Step 2: Wire the new agent bean into the controller's constructor.**

- [ ] **Step 3: Integration smoke test** — POST `/api/platform/internal/ai/chat/files` with a small prompt, verify the SSE stream starts and ends without error.

- [ ] **Step 4: Commit.** `4815 Dispatch Source.FILES to FilesSpringAIAgent`.

### Task 9.5: Phase 9 gate

- [ ] **Step 1:** `./gradlew :server:ee:apps:ai-copilot-app:check`. Green.

---

## Phase 10 — Client Copilot integration

### Task 10.1: `CopilotRuntimeProvider.onToolResult` interceptor

**Files:**
- Modify: `client/src/shared/copilot/CopilotRuntimeProvider.tsx` (locate exact path with `grep -rn "HttpAgent" client/src | head -5`)
- Modify: test file

- [ ] **Step 1: Write failing test.**
  ```tsx
  test('createWorkspaceFile tool result prepends file to Apollo cache', async () => {
      const cache = new InMemoryCache();
      // pre-seed cache with an empty workspaceFiles list
      const client = new ApolloClient({cache});

      const subscriber = buildCopilotSubscriber(client, 42);
      subscriber.onToolResult({
          toolName: 'createWorkspaceFile',
          result: {id: 99, name: 'spec.md', mimeType: 'text/markdown', sizeBytes: 5},
      });

      const result = client.readQuery({
          query: GetWorkspaceFilesDocument,
          variables: {workspaceId: '42'},
      });
      expect(result?.workspaceFiles[0].id).toBe('99');
  });
  ```

- [ ] **Step 2: Run, expect FAIL.**

- [ ] **Step 3: Implement.** Extract the existing `AgentSubscriber` construction into `buildCopilotSubscriber(apolloClient, workspaceId)` and add:
  ```ts
  onToolResult: (event: ToolResultEventI) => {
      if (event.toolName !== 'createWorkspaceFile') return;
      const {id, name, mimeType, sizeBytes} = event.result as CreateWorkspaceFileOutputI;

      apolloClient.cache.modify({
          fields: {
              workspaceFiles(existing = [], {readField}) {
                  const newRef = apolloClient.cache.writeFragment({
                      data: {__typename: 'WorkspaceFile', id, mimeType, name, sizeBytes,
                          source: 'AI_GENERATED', tags: [], downloadUrl: `/api/.../${id}/content`},
                      fragment: gql`fragment NewWorkspaceFile on WorkspaceFile {
                          id name mimeType sizeBytes source tags { id name } downloadUrl
                      }`,
                  });
                  return [newRef, ...existing];
              },
          },
      });
      toast.success(`Created "${name}"`);
  }
  ```

- [ ] **Step 4: Run, expect PASS.**

- [ ] **Step 5: Commit.** `4815 client - Intercept createWorkspaceFile tool result`.

### Task 10.2: "Create with AI" button + `openCopilotForFiles()`

**Files:**
- Modify: `client/src/shared/copilot/stores/useCopilotPanelStore.ts` (or wherever `useCopilotPanelStore` lives)
- Modify: `WorkspaceFilesPage.tsx` (wire the button)

- [ ] **Step 1: Export `openCopilotForFiles()`** helper that sets the source to `'files'` and opens the panel.

- [ ] **Step 2: Wire button** in `WorkspaceFilesPage`:
  ```tsx
  <Button onClick={openCopilotForFiles}>
      <SparklesIcon className="mr-2 size-4" /> Create with AI
  </Button>
  ```

- [ ] **Step 3: Manual smoke** — open the Files page, click "Create with AI", Copilot panel opens with Files source. Type "write a CSV of test accounts with name,email,plan". File should appear.

- [ ] **Step 4: Commit.** `4815 client - Add "Create with AI" entry point to Files page`.

### Task 10.3: Phase 10 gate

- [ ] **Step 1:** `cd client && npm run check`. Green.

---

## Phase 11 — E2E & final wrap-up

### Task 11.1: Playwright E2E happy path

**Files:**
- Create: `client/playwright/e2e/workspace-files.spec.ts`

- [ ] **Step 1: Write test.**
  ```ts
  import {expect, test} from '@playwright/test';

  test('upload markdown, edit, persist', async ({page}) => {
      await page.goto('/');
      await page.getByLabel('Email').fill('admin@localhost.com');
      await page.getByLabel('Password').fill('admin');
      await page.getByRole('button', {name: 'Sign in'}).click();

      await page.getByRole('link', {name: 'Files'}).click();
      await expect(page).toHaveURL(/\/automation\/workspace-files/);

      const fileChooser = page.waitForEvent('filechooser');
      await page.getByRole('button', {name: 'Upload'}).click();
      (await fileChooser).setFiles({
          name: 'e2e.md', mimeType: 'text/markdown', buffer: Buffer.from('# e2e'),
      });
      await expect(page.getByRole('cell', {name: 'e2e.md'})).toBeVisible();

      await page.getByRole('cell', {name: 'e2e.md'}).click();
      const editor = page.getByRole('textbox');
      await editor.fill('# e2e edited');
      await page.getByRole('button', {name: 'Save'}).click();

      await page.reload();
      await page.getByRole('cell', {name: 'e2e.md'}).click();
      await expect(page.getByRole('textbox')).toHaveText('# e2e edited');
  });
  ```

- [ ] **Step 2: Run E2E** — `cd client && npx playwright test workspace-files`. Fix selectors / timing as needed.

- [ ] **Step 3: Commit.** `4815 client - Add workspace-files E2E happy path`.

### Task 11.2: Final check gate

- [ ] **Step 1:** `./gradlew spotlessApply check` — green.

- [ ] **Step 2:** `cd client && npm run check` — green.

- [ ] **Step 3:** `./gradlew testIntegration` — green.

- [ ] **Step 4:** Manual smoke:
  1. Upload a markdown file via drag-drop.
  2. Open Copilot from the workflow editor, ask "write a brief runbook for a failing job as markdown", verify it lands in `/automation/workspace-files`.
  3. Open the generated file, verify provenance panel shows the prompt.
  4. Edit and save, reload, verify edit persists.

- [ ] **Step 5:** Push branch, open PR titled `4815 Workspace Files` linking to issue #4815. Include the acceptance-criteria checklist from spec §9 in the PR body.

---

## Acceptance Criteria (mirrors spec §9)

- [ ] CE module tree (`automation-workspace-file/*`) compiles; unit and integration tests pass.
- [ ] EE module tree (`automation-workspace-file/automation-workspace-file-ai/*`) compiles with `@ConditionalOnEEVersion`.
- [ ] Liquibase migration applies cleanly on an empty DB.
- [ ] Upload/download/rename/delete round-trip works over REST and GraphQL with workspace membership enforced.
- [ ] `CreateWorkspaceFileToolCallback` is invokable from the workflow-editor Copilot and from the Files agent; result prepends to the Files panel list without a page refresh.
- [ ] Monaco editor opens text-like files ≤ 1 MB; images/PDFs preview; other binary falls back to download.
- [ ] `./gradlew check` and `cd client && npm run check` are green.
