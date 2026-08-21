# Context Store MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the MVP of Context Store — a workspace-scoped, periodically-refreshed structured replica of source-system data, with workflow-step access (CE) and AI-agent tool surfaces (EE: AI Agent component, MCP server, AiHub).

**Architecture:** Option B from spec (everything EE) + 2026-05-08 DataStream pivot for sync orchestration. Sync mechanism is the existing `data-stream.stream` action wrapped in an auto-generated workflow per Context Source: `[schedule.cronTrigger] → [data-stream.stream(SOURCE=<component>.ItemReader, DESTINATION=contextStore.writeToReplica)]`. The only new sync code is one DESTINATION cluster element + one JobExecutionListener. Atlas's workflow engine handles cron, retry, observability, manual runs, and Worker dispatch. PgVector and the AI gateway are NOT used in MVP; semantic search and public REST API are documented in spec §10/§11 but deferred to subsequent plans.

**Tech Stack:**
- Java 25, Spring Boot 4.0.5, Spring Data JDBC
- DataStream (existing `data-stream.stream` action; Spring Batch underneath, but used via DataStream's component layer)
- Atlas workflow engine (cron triggers, manual runs, Worker dispatch, JobExecution history)
- PostgreSQL 15+, Liquibase
- Spring AI (`FunctionToolCallback` for the EE tool surface)
- ByteChef component DSL (`@AutoService(ComponentHandler.class)`, `BaseToolFunction.TOOLS` cluster elements, DataStream `DESTINATION` cluster elements, `AbstractComponentDefinitionWrapper`)
- GraphQL via Spring GraphQL
- Testcontainers PostgreSQL for `*IntTest`s

**Source spec:** [docs/superpowers/specs/2026-05-08-context-store-design.md](../specs/2026-05-08-context-store-design.md)

**In scope (added 2026-05-08 via owner pivot, redesigned 2026-05-08 mid-Phase-13):**
- **Knowledge Base Source** — periodic ingestion of document-shaped content from connected sources directly into the existing Knowledge Base. Per the post-2026-05-09 KB-to-platform move (commit `5cee82ab933`), KB-Source rides on the same platform-vs-automation split as KB itself: the `KnowledgeBaseSource` entity + repo + service + sync listener + sync helpers on `KnowledgeBaseDocumentService` live on `platform-knowledge-base-{api,service}`; the `WorkspaceKnowledgeBaseSource` relation entity + workspace-aware `WorkspaceKnowledgeBaseSourceFacade` (workflow auto-gen, ProjectDeploymentWorkflow lifecycle, manual job dispatch) live on `automation-knowledge-base-{api,service}`; the GraphQL controller stays on `automation-knowledge-base-graphql`. Five nullable sync columns are added to the existing `knowledge_base_document` table (now a platform table). See spec §12. Implemented in **Phase 13** (Tasks 29-33 + 32a) after the Context Store MVP completes. Reuses the CS DataStream-driven sync mechanism with a new `knowledgeBase.writeAsDocument` DESTINATION cluster element on the existing `knowledgeBase` component (`server/libs/modules/components/ai/vectorstore/knowledgebase/`). **Supersedes the earlier "Knowledge Sync as parallel primitive" design** (separate `automation-knowledge-sync` module tree + `KnowledgeSyncSource`/`KnowledgeSyncEntity` entities + per-entity templates) — see spec §16 decision log.
- **Semantic search add-on for Context Store records** — Phase 14 (Tasks 34-35). PgVector embeddings + cosine-similarity search over `context_store_record`, gated by `@ConditionalOnBean(EmbeddingModel.class)`. Spring AI primitives only; no gateway-specific imports. Hybrid prefilter via direct subquery against `context_store_record_index`.
- **Client-side UI** — Phase 15 (Tasks 36-39). Workspace-scoped sources list + detail + Add dialogs for both Context Store and Knowledge Base Source; KB document list gains a "Sync source" badge column.

**Out of scope for this plan:**
- public REST API (spec §11) — separate plan when scheduled
- ClickHouse alternative store (spec §12a) — post-MVP swap; tracked as **Phase 16** (Tasks 40-45)
- Incremental sync via `since: Instant` parameter on `ItemReader.open()` — separate post-MVP DataStream SPI extension

---

## File structure

> **2026-05-09 platform pivot (commit `baa1f1fe311`)**: CS core moved from `server/ee/libs/automation/automation-context-store/{api,service}/` to `server/ee/libs/platform/platform-context-store/{api,service}/` per the Connection/WorkspaceConnection precedent (spec §4a). Java packages renamed `com.bytechef.ee.automation.contextstore.*` → `com.bytechef.ee.platform.contextstore.*`. The slim `automation-context-store-{api,service}` module now hosts only the `WorkspaceContextStoreSource` relation entity + repo + service. `automation-context-store-graphql` stays in automation per "in automation only leave graphql". `workspace_id` columns dropped from `context_store_source` and `context_store_record`; new `workspace_context_store_source` relation table. Most task descriptions below were authored pre-pivot — paths and packages have been updated to match the post-pivot layout. See spec §16 decision log for the full migration narrative.

### EE platform — core entity + sync logic — `server/ee/libs/platform/platform-context-store/`

```
platform-context-store-api/
  build.gradle.kts
  src/main/java/com/bytechef/ee/platform/contextstore/
    domain/
      ContextStoreSource.java                  # @Table, NO workspace_id; has reader_strategy, cadence, status, workflow_id
      ContextStoreEntity.java                  # @Table, FK to source via AggregateReference
      ContextStoreRecord.java                  # @Table, payload JSONB via MapWrapper, payload_hash, deleted_at; NO workspace_id
      ContextStoreRecordIndex.java             # @Table, sidecar typed-column index rows
      ReaderStrategy.java                      # enum: CLUSTER_ELEMENT(0), LIST_ACTION(1)
      ContextStoreSourceStatus.java            # enum: BUILDING_PREVIEW(0), PREVIEW(1), READY(2), FAILED(3), DISABLED(4)
    dto/
      ContextStoreQuery.java                   # search query DTO
      ContextStoreQueryFilter.java             # {field, op, value}
      ContextStoreQuerySort.java               # {field, dir}
      ContextStoreSearchResult.java            # {items, nextCursor}
      CreateContextStoreSourceInput.java       # facade input record
      UpdateContextStoreSourceInput.java       # facade input record
    repository/
      ContextStoreSourceRepository.java
      ContextStoreEntityRepository.java
      ContextStoreRecordRepository.java
      ContextStoreRecordIndexRepository.java
    service/
      ContextStoreSourceService.java           # interface
      ContextStoreEntityService.java           # interface
      ContextStoreRecordService.java           # interface
      ContextStoreQueryService.java            # interface — structured search/get
    facade/
      # (the WorkspaceContextStoreSourceFacade interface lived here briefly between
      #  commits baa1f1fe311 and 64bf8e1fc5d. The second 2026-05-09 pivot moved it to
      #  automation-context-store-api per the "workspace logic stays in automation" rule.)
    tool/
      ContextStoreToolFacade.java              # interface — mints per-(source, entity) FunctionToolCallbacks

platform-context-store-service/
  build.gradle.kts
  src/main/java/com/bytechef/ee/platform/contextstore/
    config/
      ContextStoreJdbcRepositoryConfiguration.java   # @AutoConfiguration + @EnableJdbcRepositories
    service/
      ContextStoreSourceServiceImpl.java
      ContextStoreEntityServiceImpl.java
      ContextStoreRecordServiceImpl.java
      ContextStoreQueryServiceImpl.java        # filter→SQL translation, cursor pagination
    facade/
      WorkspaceContextStoreSourceFacadeImpl.java        # auto-generates workflow on create (in automation; calls platform service for entity CRUD)
                                               # to resolve workspaceId → project (no automation import)
    listener/
      ContextStoreSyncJobListener.java         # JobExecutionListener; detects writeToReplica destination + tombstone-on-completion;
                                               # mode-aware (FULL_REPLACE | PARTIAL) per spec §6
    tool/
      ContextStoreToolFacadeImpl.java          # extends AbstractToolFacade; mints per-(source, entity) FunctionToolCallbacks
                                               # for McpServer enumeration
    util/
      ContextStoreWorkflowGenerator.java       # builds the auto-generated sync workflow definition
  src/main/resources/
    config/liquibase/changelog/platform/context_store/
      00000000000001_platform_context_store_init.xml          # 5 tables: 4 CS core (no workspace_id on source/record)
                                                              # + workspace_context_store_source relation table
                                                              # + the deferred semantic embedding table (empty in MVP)
      master.xml                                              # includeAll path: platform/context_store/
  src/test/java/com/bytechef/ee/platform/contextstore/
    config/
      ContextStoreIntTestConfiguration.java
    util/
      EnumOrdinalStabilityTest.java
      ContextStoreWorkflowGeneratorTest.java
    service/
      ContextStoreSourceServiceIntTest.java
      ContextStoreEntityServiceIntTest.java
      ContextStoreQueryServiceIntTest.java
    facade/
      WorkspaceContextStoreSourceFacadeImplTest.java    # workflow auto-gen, cadence update, deletion cascade
    listener/
      ContextStoreSyncJobListenerTest.java     # destination detection + tombstone behavior + mode branching
    tool/
      ContextStoreToolFacadeImplTest.java
    ContextStoreSyncE2EIntTest.java            # full DataStream + workflow sync flow with fake ItemReader
```

### EE automation — workspace-relation + GraphQL only — `server/ee/libs/automation/automation-context-store/`

```
automation-context-store-api/                  # SLIM — workspace-relation entity only
  build.gradle.kts
  src/main/java/com/bytechef/ee/automation/contextstore/
    domain/
      WorkspaceContextStoreSource.java         # @Table workspace_context_store_source, mirrors WorkspaceConnection shape
    repository/
      WorkspaceContextStoreSourceRepository.java
    service/
      WorkspaceContextStoreSourceService.java  # interface

automation-context-store-service/              # SLIM — workspace-relation impl + SPI bridge
  build.gradle.kts
  src/main/java/com/bytechef/ee/automation/contextstore/
    config/
      WorkspaceContextStoreSourceJdbcRepositoryConfiguration.java
    service/
      WorkspaceContextStoreSourceServiceImpl.java   # (post-2026-05-09 SPI deletion: just the workspace-relation service now)
                                                    # from platform-CS-api → resolves workspaceId-derived data
                                                    # (workspace's "private" project for auto-generated workflow ownership)

automation-context-store-graphql/               # UNCHANGED LOCATION — stays in automation per "graphql only" rule
  build.gradle.kts
  src/main/java/com/bytechef/ee/automation/contextstore/web/graphql/
    ContextStoreSourceGraphQlController.java   # imports updated to platform-CS-api types;
                                               # uses WorkspaceContextStoreSourceService for workspace-scoped reads;
                                               # mutations call WorkspaceContextStoreSourceFacade (in automation) with workspaceId
                                               # (facade is in automation; calls platform service for entity CRUD)
    dto/
      CreateContextStoreSourceGraphQlInput.java
      UpdateContextStoreSourceGraphQlInput.java
      ContextStoreSourceFilter.java
  src/main/resources/graphql/
    context-store.graphqls
  src/test/java/com/bytechef/ee/automation/contextstore/web/graphql/
    config/
      ContextStoreGraphQlTestConfiguration.java
      ContextStoreGraphQlConfigurationSharedMocks.java
    ContextStoreSourceGraphQlControllerIntTest.java
```

### EE — synthetic component (workflow Actions + DataStream DESTINATION) — `server/ee/libs/modules/components/context-store/`

```
build.gradle.kts
src/main/java/com/bytechef/ee/component/contextstore/
  ContextStoreComponentHandler.java            # @AutoService(ComponentHandler.class), name "contextStore", version 1
                                               # imports updated to platform-CS-api package
  action/
    ContextStoreSearchAction.java              # search action with dynamic options (sourceId, entity);
                                               # takes sourceId via inputParameters; calls platform query service directly
    ContextStoreGetAction.java                 # get action
  destination/
    ContextStoreItemWriter.java                # DataStream DESTINATION cluster element; mode parameter (FULL_REPLACE | PARTIAL)
  util/
    ContextStoreOptionsUtils.java              # OptionsFunction implementations for sourceId/entity dropdowns
src/main/resources/
  assets/context-store.svg
  README.md                                    # auto-generated by ./gradlew generateDocumentation
src/test/java/com/bytechef/ee/component/contextstore/
  ContextStoreComponentHandlerIntTest.java     # auto-generates definition JSON
src/test/resources/
  definition/
    context-store_v1.json                      # auto-generated, regenerated each test run
```

### EE — modify existing `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/`

```
src/main/java/com/bytechef/ee/automation/aihub/tool/
  SearchContextStoreToolCallback.java          # NEW
  ListContextSourcesToolCallback.java          # NEW
  GetContextStoreRecordToolCallback.java       # NEW
src/main/java/com/bytechef/ee/automation/aihub/config/
  AiHubConfiguration.java                # MODIFY — add 3 new callbacks to defaultToolCallbacks()
src/test/java/com/bytechef/ee/automation/aihub/tool/
  SearchContextStoreToolCallbackTest.java      # NEW
  ListContextSourcesToolCallbackTest.java      # NEW
  GetContextStoreRecordToolCallbackTest.java   # NEW
```

### Platform — modify existing

```
server/libs/automation/automation-ai/automation-ai-mcp-server/.../config/AutomationMcpServerConfiguration.java
  # MODIFY — aggregate ContextStoreToolFacade.getFunctionToolCallbacks(workspaceId) into the McpServer's tool list
  # alongside AutomationMcpToolFacade outputs
```

### Phase 13 — Knowledge Base + KB-Source platform/automation split

> **2026-05-09 KB-to-platform move (commit `5cee82ab933`)**: KB itself moved from `server/libs/automation/automation-knowledge-base/{api,service,...}/` to `server/libs/platform/platform-knowledge-base/{api,service,...}/`. Java package renamed `com.bytechef.automation.knowledgebase.*` → `com.bytechef.platform.knowledgebase.*`. The slim `automation-knowledge-base-{api,service,graphql}` modules now host only the workspace-relation entity + relation service + workspace-aware facade + GraphQL controllers + auxiliary classes. KB-Source (Phase 13) follows the same pattern: it rides on the same module tree as KB, no separate `platform-knowledge-base-source` module. See spec §16 decision log entry dated 2026-05-09 (later same day).

```
# Platform side — pure data plane (no workspace concerns):
server/libs/platform/platform-knowledge-base/
  platform-knowledge-base-api/
    src/main/java/com/bytechef/platform/knowledgebase/
      domain/
        KnowledgeBaseSource.java                  # NEW — entity, NO workspace_id
        KnowledgeBaseSourceStatus.java            # NEW — enum, ordinal-stable
        ReaderStrategy.java                       # NEW — enum
        KnowledgeBaseDocument.java                # MODIFY — add 5 nullable sync columns
      repository/
        KnowledgeBaseSourceRepository.java        # NEW — CRUD by id only
        KnowledgeBaseDocumentRepository.java      # MODIFY — add findBySourceIdAndSourceRecordId, tombstoneUnseen
      service/
        KnowledgeBaseSourceService.java           # NEW — interface; CRUD by id only (no workspaceId param)
  platform-knowledge-base-service/
    src/main/java/com/bytechef/platform/knowledgebase/
      service/
        KnowledgeBaseSourceServiceImpl.java       # NEW
        KnowledgeBaseDocumentServiceImpl.java     # MODIFY — add package-private createSyncedDocument
                                                  # + replaceSyncedDocument helpers
      listener/
        KnowledgeBaseSourceSyncJobListener.java   # NEW — Spring Batch JobExecutionListener;
                                                  # tombstone + status updates by source_id only
                                                  # (mirrors ContextStoreSyncJobListener placement)
    src/main/resources/config/liquibase/changelog/platform/knowledge_base/
      <next-id>_platform_knowledge_base_source_init.xml   # NEW — creates knowledge_base_source table
                                                          # (NO workspace_id), adds 5 nullable sync
                                                          # columns to platform's knowledge_base_document,
                                                          # plus partial UNIQUE + indexes

# Automation side — workspace-relation + workspace-aware orchestration + GraphQL:
server/libs/automation/automation-knowledge-base/
  automation-knowledge-base-api/
    src/main/java/com/bytechef/automation/knowledgebase/
      domain/
        WorkspaceKnowledgeBaseSource.java         # NEW — relation entity (mirrors WorkspaceKnowledgeBase)
      repository/
        WorkspaceKnowledgeBaseSourceRepository.java   # NEW
      service/
        WorkspaceKnowledgeBaseSourceService.java  # NEW — interface; getAllByWorkspaceId,
                                                  # fetchWorkspaceIdByKnowledgeBaseSourceId, etc.
      facade/
        WorkspaceKnowledgeBaseSourceFacade.java   # NEW — interface; create/update/delete/refreshNow/
                                                  # setEnabled, all signatures take workspaceId first
  automation-knowledge-base-service/
    src/main/java/com/bytechef/automation/knowledgebase/
      service/
        WorkspaceKnowledgeBaseSourceServiceImpl.java  # NEW
      facade/
        WorkspaceKnowledgeBaseSourceFacadeImpl.java   # NEW — uses platform KnowledgeBaseSourceService
                                                      # for entity CRUD + WorkspaceKnowledgeBaseSourceRepository
                                                      # for relation insert/delete + atlas-coordinator
                                                      # for workflow auto-gen / ProjectDeploymentWorkflow
                                                      # / manual job dispatch
    src/main/resources/config/liquibase/changelog/automation/knowledge_base/
      <next-id>_automation_workspace_knowledge_base_source_init.xml   # NEW — creates
                                                                       # workspace_knowledge_base_source table
  automation-knowledge-base-graphql/
    src/main/java/com/bytechef/automation/knowledgebase/web/graphql/
      KnowledgeBaseSourceGraphQlController.java   # NEW — imports both com.bytechef.platform.knowledgebase.*
                                                  # AND com.bytechef.automation.knowledgebase.*
    src/main/resources/graphql/
      <existing or new schema file>               # MODIFY — KnowledgeBaseSource type, CRUD mutations,
                                                  # extend KnowledgeBaseDocument with sync fields

# CE — modify existing knowledgeBase component:
server/libs/modules/components/ai/vectorstore/knowledgebase/
  src/main/java/com/bytechef/component/ai/vectorstore/knowledgebase/
    destination/
      KnowledgeBaseItemWriter.java                # NEW — DESTINATION cluster element;
                                                  # ItemWriter<Map<String,Object>>; takes sourceId + mode
                                                  # via inputParameters; calls platform services
    KnowledgeBaseComponentHandler.java            # MODIFY — register writeAsDocument cluster element
```

### Root settings

```
settings.gradle.kts
  # MODIFY — register all new module paths
```

---

## Phase 0: Module scaffolding

### Task 1: Create EE platform-CS + GraphQL module skeletons

> **Post-2026-05-09 platform pivot**: this task scaffolds three modules across two namespaces. The platform-CS modules host the core CS surface; `automation-context-store-graphql` stays under automation per "in automation only leave graphql".

**Files:**
- Create: `server/ee/libs/platform/platform-context-store/platform-context-store-api/build.gradle.kts`
- Create: `server/ee/libs/platform/platform-context-store/platform-context-store-service/build.gradle.kts`
- Create: `server/ee/libs/automation/automation-context-store/automation-context-store-graphql/build.gradle.kts`
- Modify: `settings.gradle.kts`

- [ ] **Step 1: Read sibling module's build.gradle.kts as template**

```bash
cat server/libs/automation/automation-knowledge-base/automation-knowledge-base-api/build.gradle.kts
cat server/libs/automation/automation-knowledge-base/automation-knowledge-base-service/build.gradle.kts
cat server/libs/automation/automation-knowledge-base/automation-knowledge-base-graphql/build.gradle.kts
```

These are the closest siblings — Context Store has a near-identical dependency footprint.

- [ ] **Step 2: Create `platform-context-store-api/build.gradle.kts`**

Copy `automation-knowledge-base-api/build.gradle.kts` and adjust:
- Replace `:server:libs:automation:automation-knowledge-base:automation-knowledge-base-api` references with `:server:ee:libs:platform:platform-context-store:platform-context-store-api` where the module is consumed.
- Remove KB-specific dependencies (vector store, embedding) — Context Store API only needs Spring Data JDBC, JSpecify, jakarta.validation, and `:server:libs:platform:platform-api` (or whatever KB-api depends on for `Workspace`/`Tag`/`AggregateReference` types).
- The api module has no `@Service` impls; just interfaces, domain, DTOs, repositories. (The `ContextStoreToolFacade` interface and the workspace-aware facade live in automation per the second 2026-05-09 pivot.).
- ByteChef Enterprise license header (NOT Apache 2.0) and `@version ee` on all classes — this is an EE module.

- [ ] **Step 3: Create `platform-context-store-service/build.gradle.kts`**

Add dependencies:
- `project(":server:ee:libs:platform:platform-context-store:platform-context-store-api")`
- `project(":server:libs:platform:platform-component:platform-component-api")` (for `ClusterElementDefinitionService`, `ActionDefinitionService`)
- `project(":server:libs:platform:platform-scheduler:platform-scheduler-api")` (for the scheduler integration)
- `project(":server:libs:platform:platform-ai:platform-ai-tool-api")` (for `AbstractToolFacade` — the tool facade impl lives here now, post-pivot collapse)
- Spring Boot Batch starter: `implementation("org.springframework.boot:spring-boot-starter-batch")`
- `implementation("org.springframework.boot:spring-boot-starter-data-jdbc")`
- `implementation("com.bytechef.commons:commons-data-jdbc")` (for `MapWrapper`)
- `implementation("org.springframework.ai:spring-ai-core")` (for `FunctionToolCallback`)
- testImplementation: Testcontainers, AssertJ, Mockito (mirror `automation-knowledge-base-service`'s test deps)

- [ ] **Step 4: Create `automation-context-store-graphql/build.gradle.kts`**

Copy from `automation-knowledge-base-graphql/build.gradle.kts`. Replace project references. Add:
- `implementation("org.springframework.boot:spring-boot-starter-graphql")`
- `project(":server:ee:libs:platform:platform-context-store:platform-context-store-api")` (for facade + service interfaces, domain, DTOs)
- `project(":server:ee:libs:automation:automation-context-store:automation-context-store-api")` (for `WorkspaceContextStoreSourceService` — workspace-scoped reads)
- ByteChef Enterprise license header on the controller (`server/ee/` convention).

- [ ] **Step 5: Register all three modules in `settings.gradle.kts`**

Find the block where automation-knowledge-base modules are registered and add:

```kotlin
include(":server:ee:libs:platform:platform-context-store:platform-context-store-api")
include(":server:ee:libs:platform:platform-context-store:platform-context-store-service")
include(":server:ee:libs:automation:automation-context-store:automation-context-store-graphql")
```

- [ ] **Step 6: Verify gradle sees the new modules**

Run: `./gradlew :server:ee:libs:platform:platform-context-store:platform-context-store-api:dependencies --configuration compileClasspath | head -20`
Expected: gradle prints the dependency tree without errors. (No source files yet, so no compilation; just dependency resolution.)

- [ ] **Step 7: Commit**

```bash
git add server/ee/libs/platform/platform-context-store/ \
        server/ee/libs/automation/automation-context-store/automation-context-store-graphql/ \
        settings.gradle.kts
git commit -m "$(cat <<'EOF'
4854 Scaffold platform-context-store + automation-context-store-graphql modules

Empty module skeletons. No source files yet; dependencies resolved successfully.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

(Replace `4854` with the actual ticket number for the user's tracking; if no ticket, use a placeholder like `CS-001` and let the user adjust.)

---

### Task 2: Create the slim `automation-context-store-{api,service}` modules (workspace-relation only)

> **Post-2026-05-09 platform pivot**: under the original (pre-pivot) plan, this task scaffolded EE tool modules `automation-context-store-tool-{api,service}`. Per spec §4 line 207 (Option B EE-only collapse), those tool modules collapsed into the main api/service. After the 2026-05-09 platform pivot (commit `baa1f1fe311`), the main api/service modules moved to `platform-context-store-{api,service}` (scaffolded in Task 1), and the tool surface lives there. The original `automation-context-store-{api,service}` modules now hold ONLY the `WorkspaceContextStoreSource` relation entity + repo + service. This task scaffolds those slim modules.

**Files:**
- Create: `server/ee/libs/automation/automation-context-store/automation-context-store-api/build.gradle.kts`
- Create: `server/ee/libs/automation/automation-context-store/automation-context-store-service/build.gradle.kts`
- Modify: `settings.gradle.kts`

- [ ] **Step 1: Read existing relation-table sibling for reference**

```bash
cat server/libs/automation/automation-configuration/automation-configuration-api/build.gradle.kts
```

`WorkspaceConnection` is the closest precedent for the workspace-relation pattern; mirror its build dependencies for `automation-context-store-api`.

- [ ] **Step 2: Create `automation-context-store-api/build.gradle.kts`**

```kotlin
plugins {
    `java-library`
}

dependencies {
    api(project(":server:libs:platform:platform-api"))                                 // for Workspace types
    api(project(":server:ee:libs:platform:platform-context-store:platform-context-store-api"))  // for ContextStoreSource AggregateReference (entity)
    implementation("org.springframework.data:spring-data-jdbc")
}
```

ByteChef Enterprise license header (NOT Apache 2.0) and `@version ee` on all classes.

- [ ] **Step 3: Create `automation-context-store-service/build.gradle.kts`**

```kotlin
plugins {
    `java-library`
}

dependencies {
    implementation(project(":server:ee:libs:automation:automation-context-store:automation-context-store-api"))
    implementation(project(":server:ee:libs:platform:platform-context-store:platform-context-store-api"))
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))   // for Project ownership lookup
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    testImplementation(project(":server:libs:test:test-int-support"))
}
```

- [ ] **Step 4: Register in `settings.gradle.kts`**

```kotlin
include(":server:ee:libs:automation:automation-context-store:automation-context-store-api")
include(":server:ee:libs:automation:automation-context-store:automation-context-store-service")
```

- [ ] **Step 5: Verify gradle resolution**

Run: `./gradlew :server:ee:libs:automation:automation-context-store:automation-context-store-service:dependencies --configuration compileClasspath | head -20`
Expected: no errors.

- [ ] **Step 6: Commit**

```bash
git add server/ee/libs/automation/automation-context-store/ settings.gradle.kts
git commit -m "Scaffold slim automation-context-store-{api,service} modules

Empty workspace-relation module skeletons under server/ee/libs/automation/.
Hosts WorkspaceContextStoreSource relation entity (post-2026-05-09 SPI deletion: the workspace-aware facade now also lives here)
SPI implementation only; CS core lives in platform-context-store-{api,service}.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 3: Create the `context-store` component module

**Files:**
- Create: `server/ee/libs/modules/components/context-store/build.gradle.kts`
- Create: `server/ee/libs/modules/components/context-store/src/main/resources/assets/context-store.svg`
- Modify: `settings.gradle.kts`

- [ ] **Step 1: Read a small sibling component**

```bash
cat server/libs/modules/components/crypto-helper/build.gradle.kts
```

`crypto-helper` is small and has a similar dependency profile (no external API, just internal services).

- [ ] **Step 2: Create `build.gradle.kts`**

```kotlin
description = "Context Store component — search and get actions over the workspace's replicated source data"

dependencies {
    api(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:ee:libs:platform:platform-context-store:platform-context-store-api"))
    implementation(project(":server:ee:libs:platform:platform-context-store:platform-context-store-service"))
    annotationProcessor("com.google.auto.service:auto-service:${rootProject.libs.versions.auto.service.get()}")
    compileOnly("com.google.auto.service:auto-service-annotations:${rootProject.libs.versions.auto.service.get()}")
}
```

(Verify exact `auto.service` reference idiom by looking at any existing component's `build.gradle.kts`.)

- [ ] **Step 3: Drop a placeholder SVG icon**

```bash
cp server/libs/modules/components/crypto-helper/src/main/resources/assets/crypto-helper.svg \
   server/ee/libs/modules/components/context-store/src/main/resources/assets/context-store.svg
```

(User can replace with a proper icon later.)

- [ ] **Step 4: Register in `settings.gradle.kts`**

```kotlin
include(":server:ee:libs:modules:components:context-store")
```

- [ ] **Step 5: Verify**

Run: `./gradlew :server:ee:libs:modules:components:context-store:compileJava`
Expected: BUILD SUCCESSFUL (no Java sources yet, but the module compiles).

- [ ] **Step 6: Commit**

```bash
git add server/ee/libs/modules/components/context-store/ settings.gradle.kts
git commit -m "Scaffold context-store component module

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Phase 1: Domain entities (EE platform-CS)

Each entity follows the established Spring Data JDBC pattern from `KnowledgeBase.java:40–79`. **All entities** use:
- `@Table` (table name implicit from class)
- `@Id Long id;`
- `@Version int version;`
- `@CreatedBy String createdBy; @CreatedDate Instant createdDate;`
- `@LastModifiedBy String lastModifiedBy; @LastModifiedDate Instant lastModifiedDate;`
- `@Column("workspace_id")` (only on tables that inline it)
- INT ordinal columns for enums; getters return the enum, setters accept the enum

### Task 4: ContextStoreSource entity + ReaderStrategy + ContextStoreSourceStatus enums

> **Post-2026-05-09 platform pivot delta**: the code block below shows the pre-pivot entity with `workspaceId` inlined. After commit `baa1f1fe311`, `workspace_id` was DROPPED from `ContextStoreSource` (and from `ContextStoreRecord`); workspace scoping flows through the new `WorkspaceContextStoreSource` relation entity in the slim automation-CS module. Skip the `workspaceId` field, getter, and setter when implementing — the entity now has no workspace coupling. The `WorkspaceContextStoreSource` relation entity (Task 1's slim automation-CS module) carries `(workspace_id, context_store_source_id)` instead.

**Files:**
- Create: `platform-context-store-api/.../domain/ReaderStrategy.java`
- Create: `platform-context-store-api/.../domain/ContextStoreSourceStatus.java`
- Create: `platform-context-store-api/.../domain/ContextStoreSource.java`

- [ ] **Step 1: Write the failing enum tests for ordinal stability**

Create `EnumOrdinalStabilityTest.java` in `platform-context-store-service/src/test/java/com/bytechef/ee/platform/contextstore/util/EnumOrdinalStabilityTest.java` with two test methods. Pattern from `EnumOrdinalStabilityTest.java:40–50`:

```java
package com.bytechef.ee.platform.contextstore.util;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreSourceStatus;
import com.bytechef.ee.platform.contextstore.domain.ReaderStrategy;
import com.bytechef.platform.tenant.test.OrdinalStabilityAssertions;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EnumOrdinalStabilityTest {

    @Test
    void testReaderStrategyOrdinalsAreStable() {
        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("CLUSTER_ELEMENT", 0);
        expected.put("LIST_ACTION", 1);

        OrdinalStabilityAssertions.assertOrdinalsMatch(
            ReaderStrategy.values(), expected, ReaderStrategy.class.getSimpleName());
    }

    @Test
    void testContextStoreSourceStatusOrdinalsAreStable() {
        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("BUILDING_PREVIEW", 0);
        expected.put("PREVIEW", 1);
        expected.put("READY", 2);
        expected.put("FAILED", 3);
        expected.put("DISABLED", 4);

        OrdinalStabilityAssertions.assertOrdinalsMatch(
            ContextStoreSourceStatus.values(), expected, ContextStoreSourceStatus.class.getSimpleName());
    }
}
```

- [ ] **Step 2: Run test — expect compilation failure (enums don't exist yet)**

Run: `./gradlew :server:ee:libs:platform:platform-context-store:platform-context-store-service:test --tests EnumOrdinalStabilityTest`
Expected: FAIL — `cannot find symbol class ReaderStrategy`.

- [ ] **Step 3: Create the two enums**

`ReaderStrategy.java`:

```java
package com.bytechef.ee.platform.contextstore.domain;

/**
 * Strategy for reading records from a source component during sync.
 * <p>Ordinals are pinned by EnumOrdinalStabilityTest — append new values at the end.
 */
public enum ReaderStrategy {
    CLUSTER_ELEMENT,
    LIST_ACTION
}
```

`ContextStoreSourceStatus.java`:

```java
package com.bytechef.ee.platform.contextstore.domain;

/**
 * Lifecycle state of a Context Store source. Maps to Airbyte's progressive-availability model.
 * <p>Ordinals are pinned by EnumOrdinalStabilityTest — append new values at the end.
 */
public enum ContextStoreSourceStatus {
    BUILDING_PREVIEW,
    PREVIEW,
    READY,
    FAILED,
    DISABLED
}
```

- [ ] **Step 4: Run enum tests — expect them to pass; ContextStoreSource compilation still fails**

Run: `./gradlew :server:ee:libs:platform:platform-context-store:platform-context-store-service:test --tests EnumOrdinalStabilityTest`
Expected: PASS for both methods.

- [ ] **Step 5: Create `ContextStoreSource` entity**

```java
package com.bytechef.ee.platform.contextstore.domain;

import com.bytechef.platform.connection.domain.Connection;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("context_store_source")
public class ContextStoreSource {

    @Id
    private Long id;

    @Column("workspace_id")
    private Long workspaceId;

    private String name;

    @Column("source_component_name")
    private String sourceComponentName;

    @Column("source_component_version")
    private int sourceComponentVersion;

    @Column("reader_strategy")
    private int readerStrategy;

    @Column("source_cluster_element_name")
    @Nullable
    private String sourceClusterElementName;

    @Column("source_list_action_name")
    @Nullable
    private String sourceListActionName;

    @Column("connection_id")
    @Nullable
    private AggregateReference<Connection, Long> connectionId;

    private String cadence;

    private int status;

    private boolean enabled = true;

    @Column("last_sync_run_at")
    @Nullable
    private Instant lastSyncRunAt;

    @Column("last_sync_job_execution_id")
    @Nullable
    private Long lastSyncJobExecutionId;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @CreatedDate
    @Column("created_date")
    private Instant createdDate;

    @LastModifiedBy
    @Column("last_modified_by")
    private String lastModifiedBy;

    @LastModifiedDate
    @Column("last_modified_date")
    private Instant lastModifiedDate;

    @Version
    private int version;

    public ContextStoreSource() {}

    public Long getId() { return id; }
    public Long getWorkspaceId() { return workspaceId; }
    public String getName() { return name; }
    public String getSourceComponentName() { return sourceComponentName; }
    public int getSourceComponentVersion() { return sourceComponentVersion; }
    public ReaderStrategy getReaderStrategy() { return ReaderStrategy.values()[readerStrategy]; }
    public @Nullable String getSourceClusterElementName() { return sourceClusterElementName; }
    public @Nullable String getSourceListActionName() { return sourceListActionName; }
    public @Nullable Long getConnectionId() { return connectionId == null ? null : connectionId.getId(); }
    public String getCadence() { return cadence; }
    public ContextStoreSourceStatus getStatus() { return ContextStoreSourceStatus.values()[status]; }
    public boolean isEnabled() { return enabled; }
    public @Nullable Instant getLastSyncRunAt() { return lastSyncRunAt; }
    public @Nullable Long getLastSyncJobExecutionId() { return lastSyncJobExecutionId; }
    public Instant getCreatedDate() { return createdDate; }
    public String getCreatedBy() { return createdBy; }
    public Instant getLastModifiedDate() { return lastModifiedDate; }
    public String getLastModifiedBy() { return lastModifiedBy; }
    public int getVersion() { return version; }

    public void setId(Long id) { this.id = id; }
    public void setWorkspaceId(Long workspaceId) { this.workspaceId = workspaceId; }
    public void setName(String name) { this.name = name; }
    public void setSourceComponentName(String name) { this.sourceComponentName = name; }
    public void setSourceComponentVersion(int v) { this.sourceComponentVersion = v; }
    public void setReaderStrategy(ReaderStrategy s) { this.readerStrategy = s.ordinal(); }
    public void setSourceClusterElementName(@Nullable String n) { this.sourceClusterElementName = n; }
    public void setSourceListActionName(@Nullable String n) { this.sourceListActionName = n; }
    public void setConnectionId(@Nullable Long id) { this.connectionId = id == null ? null : AggregateReference.to(id); }
    public void setCadence(String c) { this.cadence = c; }
    public void setStatus(ContextStoreSourceStatus s) { this.status = s.ordinal(); }
    public void setEnabled(boolean e) { this.enabled = e; }
    public void setLastSyncRunAt(@Nullable Instant t) { this.lastSyncRunAt = t; }
    public void setLastSyncJobExecutionId(@Nullable Long jid) { this.lastSyncJobExecutionId = jid; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContextStoreSource other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
```

- [ ] **Step 6: Run all enum tests**

Run: `./gradlew :server:ee:libs:platform:platform-context-store:platform-context-store-service:test --tests EnumOrdinalStabilityTest`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add server/ee/libs/platform/platform-context-store/platform-context-store-api/src/main/java/com/bytechef/automation/contextstore/domain/ \
        server/ee/libs/platform/platform-context-store/platform-context-store-service/src/test/java/com/bytechef/automation/contextstore/util/EnumOrdinalStabilityTest.java
git commit -m "Add ContextStoreSource entity and enums

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 5: ContextStoreEntity, ContextStoreRecord, ContextStoreRecordIndex, ContextStoreSyncRun + ContextStoreSyncRunStatus

> **2026-05-08 pivot note**: this task as historically described created `ContextStoreSyncRun` + `ContextStoreSyncRunStatus`. After the DataStream pivot, those are dropped (sync history is captured by Atlas's JobExecution rows). The entity, enum, repository, and service all need to be **deleted** in Task 14 (alongside the Liquibase column drop). The other entities created in this task (`ContextStoreEntity`, `ContextStoreRecord`, `ContextStoreRecordIndex`) stay.

**Files:**
- Create: `platform-context-store-api/.../domain/ContextStoreSyncRunStatus.java`  *(later deleted in Task 14)*
- Create: `platform-context-store-api/.../domain/ContextStoreEntity.java`
- Create: `platform-context-store-api/.../domain/ContextStoreRecord.java`
- Create: `platform-context-store-api/.../domain/ContextStoreRecordIndex.java`
- Create: `platform-context-store-api/.../domain/ContextStoreSyncRun.java`  *(later deleted in Task 14)*
- Modify: `EnumOrdinalStabilityTest.java`

- [ ] **Step 1: Extend the failing enum test for `ContextStoreSyncRunStatus`**

Add to `EnumOrdinalStabilityTest.java`:

```java
@Test
void testContextStoreSyncRunStatusOrdinalsAreStable() {
    Map<String, Integer> expected = new LinkedHashMap<>();
    expected.put("STARTED", 0);
    expected.put("COMPLETED", 1);
    expected.put("FAILED", 2);

    OrdinalStabilityAssertions.assertOrdinalsMatch(
        ContextStoreSyncRunStatus.values(), expected, ContextStoreSyncRunStatus.class.getSimpleName());
}
```

- [ ] **Step 2: Run — expect failure (enum doesn't exist)**

Run: `./gradlew ...:test --tests EnumOrdinalStabilityTest.testContextStoreSyncRunStatusOrdinalsAreStable`
Expected: FAIL — `cannot find symbol class ContextStoreSyncRunStatus`.

- [ ] **Step 3: Create `ContextStoreSyncRunStatus`**

```java
package com.bytechef.ee.platform.contextstore.domain;

public enum ContextStoreSyncRunStatus {
    STARTED,
    COMPLETED,
    FAILED
}
```

- [ ] **Step 4: Create the four remaining entity classes**

Each follows the Spring Data JDBC pattern. Use `MapWrapper` from `com.bytechef.commons.data.jdbc.wrapper.MapWrapper` for JSONB columns (precedent: `McpTool.java:48`).

`ContextStoreEntity.java`:

```java
package com.bytechef.ee.platform.contextstore.domain;

import com.bytechef.commons.data.jdbc.wrapper.MapWrapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("context_store_entity")
public class ContextStoreEntity {

    @Id
    private Long id;

    @Column("source_id")
    private AggregateReference<ContextStoreSource, Long> sourceId;

    @Column("entity_name")
    private String entityName;

    @Nullable
    private String description;

    @Column("id_field")
    private String idField;

    @Column("indexed_fields")
    private MapWrapper indexedFields;

    @Column("semantic_index_fields")
    @Nullable
    private MapWrapper semanticIndexFields;

    @Column
    @Nullable
    private MapWrapper parameters;

    @CreatedBy @Column("created_by") private String createdBy;
    @CreatedDate @Column("created_date") private Instant createdDate;
    @LastModifiedBy @Column("last_modified_by") private String lastModifiedBy;
    @LastModifiedDate @Column("last_modified_date") private Instant lastModifiedDate;
    @Version private int version;

    // getters/setters; sourceId via AggregateReference like McpComponent
    // indexedFields exposed as List<Map<String,Object>> via MapWrapper.getList() helper if available,
    //   else as Map<String,Object> — see what other JSONB-storing entities do
    // (parameters and indexedFields both stored as JSONB; semanticIndexFields nullable)
}
```

(Show the full getters/setters in the implementation; abbreviated above for plan length.)

`ContextStoreRecord.java` (post-pivot: NO `workspace_id` field — see Task 4 delta note):

```java
@Table("context_store_record")
public class ContextStoreRecord {
    @Id private Long id;
    @Column("source_id") private Long sourceId;
    @Column("entity_name") private String entityName;
    @Column("source_record_id") private String sourceRecordId;
    @Column("payload") private MapWrapper payload;
    @Column("payload_hash") private String payloadHash;
    @Column("last_seen_at") private Instant lastSeenAt;
    @Column("deleted_at") @Nullable private Instant deletedAt;
    @CreatedDate @Column("created_date") private Instant createdDate;
    @LastModifiedDate @Column("last_modified_date") private Instant lastModifiedDate;
    // No @Version on records — they're touched on every sync; optimistic-lock contention would be bad.
    // Concurrency is constrained by the per-source job lock, not row-level versioning.
    // standard getters/setters
}
```

`ContextStoreRecordIndex.java`:

```java
@Table("context_store_record_index")
public class ContextStoreRecordIndex {
    @Id private Long id;
    @Column("record_id") private Long recordId;
    @Column("field_name") private String fieldName;
    @Column("value_text") @Nullable private String valueText;
    @Column("value_numeric") @Nullable private java.math.BigDecimal valueNumeric;
    @Column("value_timestamp") @Nullable private Instant valueTimestamp;
}
```

`ContextStoreSyncRun.java` *(deleted in Task 14 after the DataStream pivot — listed here for historical reference)*:

```java
@Table("context_store_sync_run")
public class ContextStoreSyncRun {
    @Id private Long id;
    @Column("source_id") private Long sourceId;
    @Column("job_execution_id") private Long jobExecutionId;
    @Column("started_at") @Nullable private Instant startedAt;
    @Column("finished_at") @Nullable private Instant finishedAt;
    private int status;  // ContextStoreSyncRunStatus ordinal
    @Column("records_read") @Nullable private Integer recordsRead;
    @Column("records_upserted") @Nullable private Integer recordsUpserted;
    @Column("records_tombstoned") @Nullable private Integer recordsTombstoned;
    @Nullable private String error;
    @CreatedDate @Column("created_date") private Instant createdDate;
    @LastModifiedDate @Column("last_modified_date") private Instant lastModifiedDate;
    public ContextStoreSyncRunStatus getStatus() { return ContextStoreSyncRunStatus.values()[status]; }
    public void setStatus(ContextStoreSyncRunStatus s) { this.status = s.ordinal(); }
    // standard getters/setters
}
```

- [ ] **Step 5: Run all enum tests pass**

Run: `./gradlew ...:test --tests EnumOrdinalStabilityTest`
Expected: 3 tests PASS.

- [ ] **Step 6: Commit**

```bash
git add server/ee/libs/platform/platform-context-store/
git commit -m "Add remaining ContextStore domain entities and SyncRunStatus enum

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Phase 2: Liquibase migrations (EE platform-CS)

### Task 6: Create the init Liquibase changelog

> **Post-2026-05-09 platform pivot delta**: the snippet below shows the pre-pivot init with `workspace_id` columns on `context_store_source` and `context_store_record`. After commit `baa1f1fe311`, the actual init file is at the platform path and reflects three deltas:
> 1. **DROP `workspace_id` from `context_store_source`** — and drop the `(workspace_id, name)` unique constraint; the unique constraint becomes `(name)` only (workspace scoping enforced via the relation table).
> 2. **DROP `workspace_id` from `context_store_record`** — and drop it from the unique constraint; constraint becomes `(source_id, entity_name, source_record_id)` only.
> 3. **ADD `workspace_context_store_source` table** — `id` PK + `workspace_id` BIGINT NOT NULL + `context_store_source_id` BIGINT NOT NULL + audit + `version` BIGINT NOT NULL + UNIQUE `(workspace_id, context_store_source_id)` + FK `context_store_source_id → context_store_source.id ON DELETE CASCADE`. Mirror of `workspace_connection`. Lives in the same init file (single changeset id `00000000000001-1`); the relation table doesn't get its own migration since the branch is unmerged.
>
> Same changeset id since the branch is unmerged. master.xml's includeAll path also became `platform/context_store/`.

**Files:**
- Create: `platform-context-store-service/src/main/resources/config/liquibase/changelog/platform/context_store/00000000000001_platform_context_store_init.xml`
- Create: `platform-context-store-service/src/main/resources/config/liquibase/changelog/platform/context_store/master.xml`

- [ ] **Step 1: Read sibling init for pattern reference**

```bash
cat server/libs/automation/automation-data-table/automation-data-table-service/src/main/resources/config/liquibase/changelog/automation/data_table/00000000000001_automation_data_table_init.xml
```

This is the most recent automation-package Liquibase changeset. Mirror its structure exactly.

- [ ] **Step 2: Create `00000000000001_platform_context_store_init.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="00000000000001-1" author="Ivica Cardic">
        <createTable tableName="context_store_source">
            <column name="id" type="BIGINT" autoIncrement="true" startWith="1050">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="workspace_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="name" type="VARCHAR(256)">
                <constraints nullable="false"/>
            </column>
            <column name="source_component_name" type="VARCHAR(256)">
                <constraints nullable="false"/>
            </column>
            <column name="source_component_version" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="reader_strategy" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="source_cluster_element_name" type="VARCHAR(256)"/>
            <column name="source_list_action_name" type="VARCHAR(256)"/>
            <column name="connection_id" type="BIGINT"/>
            <column name="cadence" type="VARCHAR(64)">
                <constraints nullable="false"/>
            </column>
            <column name="status" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="enabled" type="BOOLEAN" defaultValueBoolean="true">
                <constraints nullable="false"/>
            </column>
            <column name="last_sync_run_at" type="TIMESTAMP WITH TIME ZONE"/>
            <column name="last_sync_job_execution_id" type="BIGINT"/>
            <column name="created_by" type="VARCHAR(256)"/>
            <column name="created_date" type="TIMESTAMP WITH TIME ZONE"/>
            <column name="last_modified_by" type="VARCHAR(256)"/>
            <column name="last_modified_date" type="TIMESTAMP WITH TIME ZONE"/>
            <column name="version" type="BIGINT">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <addUniqueConstraint
            tableName="context_store_source"
            columnNames="workspace_id, name"
            constraintName="uk_context_store_source_workspace_name"/>

        <createTable tableName="context_store_entity">
            <column name="id" type="BIGINT" autoIncrement="true" startWith="1050">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="source_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="entity_name" type="VARCHAR(256)">
                <constraints nullable="false"/>
            </column>
            <column name="description" type="TEXT"/>
            <column name="id_field" type="VARCHAR(256)">
                <constraints nullable="false"/>
            </column>
            <column name="stored_fields" type="JSONB"/>
            <column name="indexed_fields" type="JSONB">
                <constraints nullable="false"/>
            </column>
            <column name="semantic_index_fields" type="JSONB"/>
            <column name="parameters" type="JSONB"/>
            <column name="created_by" type="VARCHAR(256)"/>
            <column name="created_date" type="TIMESTAMP WITH TIME ZONE"/>
            <column name="last_modified_by" type="VARCHAR(256)"/>
            <column name="last_modified_date" type="TIMESTAMP WITH TIME ZONE"/>
            <column name="version" type="BIGINT">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <addForeignKeyConstraint
            baseTableName="context_store_entity"
            baseColumnNames="source_id"
            constraintName="fk_context_store_entity_source"
            referencedTableName="context_store_source"
            referencedColumnNames="id"
            onDelete="CASCADE"/>

        <addUniqueConstraint
            tableName="context_store_entity"
            columnNames="source_id, entity_name"
            constraintName="uk_context_store_entity_source_name"/>

        <createTable tableName="context_store_record">
            <column name="id" type="BIGINT" autoIncrement="true" startWith="1050">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="workspace_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="source_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="entity_name" type="VARCHAR(256)">
                <constraints nullable="false"/>
            </column>
            <column name="source_record_id" type="VARCHAR(512)">
                <constraints nullable="false"/>
            </column>
            <column name="payload" type="JSONB">
                <constraints nullable="false"/>
            </column>
            <column name="payload_hash" type="VARCHAR(16)">
                <constraints nullable="false"/>
            </column>
            <column name="last_seen_at" type="TIMESTAMP WITH TIME ZONE">
                <constraints nullable="false"/>
            </column>
            <column name="deleted_at" type="TIMESTAMP WITH TIME ZONE"/>
            <column name="created_date" type="TIMESTAMP WITH TIME ZONE"/>
            <column name="last_modified_date" type="TIMESTAMP WITH TIME ZONE"/>
        </createTable>

        <addForeignKeyConstraint
            baseTableName="context_store_record"
            baseColumnNames="source_id"
            constraintName="fk_context_store_record_source"
            referencedTableName="context_store_source"
            referencedColumnNames="id"
            onDelete="CASCADE"/>

        <addUniqueConstraint
            tableName="context_store_record"
            columnNames="workspace_id, source_id, entity_name, source_record_id"
            constraintName="uk_context_store_record_workspace_source_entity_record"/>

        <sql>CREATE INDEX idx_context_store_record_payload_gin ON context_store_record USING GIN (payload);</sql>
        <sql>CREATE INDEX idx_context_store_record_deleted_at ON context_store_record (deleted_at) WHERE deleted_at IS NOT NULL;</sql>

        <createTable tableName="context_store_record_index">
            <column name="id" type="BIGINT" autoIncrement="true" startWith="1050">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="record_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="field_name" type="VARCHAR(256)">
                <constraints nullable="false"/>
            </column>
            <column name="value_text" type="TEXT"/>
            <column name="value_numeric" type="NUMERIC"/>
            <column name="value_timestamp" type="TIMESTAMP WITH TIME ZONE"/>
        </createTable>

        <addForeignKeyConstraint
            baseTableName="context_store_record_index"
            baseColumnNames="record_id"
            constraintName="fk_context_store_record_index_record"
            referencedTableName="context_store_record"
            referencedColumnNames="id"
            onDelete="CASCADE"/>

        <createIndex tableName="context_store_record_index" indexName="idx_context_store_record_index_record_field">
            <column name="record_id"/>
            <column name="field_name"/>
        </createIndex>

        <sql>CREATE INDEX idx_context_store_record_index_field_text ON context_store_record_index (field_name, value_text) WHERE value_text IS NOT NULL;</sql>
        <sql>CREATE INDEX idx_context_store_record_index_field_numeric ON context_store_record_index (field_name, value_numeric) WHERE value_numeric IS NOT NULL;</sql>
        <sql>CREATE INDEX idx_context_store_record_index_field_timestamp ON context_store_record_index (field_name, value_timestamp) WHERE value_timestamp IS NOT NULL;</sql>

        <createTable tableName="context_store_sync_run">
            <column name="id" type="BIGINT" autoIncrement="true" startWith="1050">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="source_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="job_execution_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="started_at" type="TIMESTAMP WITH TIME ZONE"/>
            <column name="finished_at" type="TIMESTAMP WITH TIME ZONE"/>
            <column name="status" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="records_read" type="INT"/>
            <column name="records_upserted" type="INT"/>
            <column name="records_tombstoned" type="INT"/>
            <column name="error" type="TEXT"/>
            <column name="created_date" type="TIMESTAMP WITH TIME ZONE"/>
            <column name="last_modified_date" type="TIMESTAMP WITH TIME ZONE"/>
        </createTable>

        <addForeignKeyConstraint
            baseTableName="context_store_sync_run"
            baseColumnNames="source_id"
            constraintName="fk_context_store_sync_run_source"
            referencedTableName="context_store_source"
            referencedColumnNames="id"
            onDelete="CASCADE"/>

        <createIndex tableName="context_store_sync_run" indexName="idx_context_store_sync_run_source_started">
            <column name="source_id"/>
            <column name="started_at"/>
        </createIndex>
    </changeSet>
</databaseChangeLog>
```

- [ ] **Step 3: Create `master.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">
    <include file="config/liquibase/changelog/platform/context_store/00000000000001_platform_context_store_init.xml"/>
</databaseChangeLog>
```

- [ ] **Step 4: Wire master.xml into the application's main Liquibase aggregator**

Find the file in `server/libs/config/liquibase-config/` or wherever the project aggregates module-specific changelog masters. Add:

```xml
<include file="config/liquibase/changelog/platform/context_store/master.xml"/>
```

(Pattern: see how `automation-data-table` was wired — search for `automation/data_table/master.xml` references.)

- [ ] **Step 5: Run a smoke integration test that verifies migrations apply**

Use any existing simple `*IntTest` (e.g., `KnowledgeBaseServiceIntTest`) — just running it confirms migrations apply across all modules without errors.

Run: `./gradlew :server:libs:platform:platform-knowledge-base:platform-knowledge-base-service:integrationTest --tests 'KnowledgeBaseServiceIntTest.testCreateKnowledgeBase'`
Expected: PASS — proves the new Liquibase changeset doesn't break startup.

- [ ] **Step 6: Commit**

```bash
git add server/ee/libs/platform/platform-context-store/platform-context-store-service/src/main/resources/config/liquibase/ \
        server/libs/config/liquibase-config/   # adjust path to wherever the master aggregator lives
git commit -m "Add Liquibase migration for context_store tables

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Phase 3: Repositories + basic services (EE platform-CS)

### Task 7: Five repositories

**Files:**
- Create all 5 under `platform-context-store-api/.../repository/`

- [ ] **Step 1: Read sibling for pattern**

```bash
cat server/libs/platform/platform-knowledge-base/platform-knowledge-base-service/src/main/java/com/bytechef/platform/knowledgebase/repository/KnowledgeBaseDocumentRepository.java
```

Pattern: extends both `PagingAndSortingRepository<T, Long>` and `ListCrudRepository<T, Long>`. Derived methods like `findAllByXxx` need no `@Query`.

- [ ] **Step 2: Create `ContextStoreSourceRepository`**

```java
package com.bytechef.ee.platform.contextstore.repository;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContextStoreSourceRepository
    extends PagingAndSortingRepository<ContextStoreSource, Long>, ListCrudRepository<ContextStoreSource, Long> {

    List<ContextStoreSource> findAllByWorkspaceId(Long workspaceId);

    List<ContextStoreSource> findAllByWorkspaceIdAndEnabled(Long workspaceId, boolean enabled);
}
```

- [ ] **Step 3: Create `ContextStoreEntityRepository`**

```java
@Repository
public interface ContextStoreEntityRepository
    extends ListCrudRepository<ContextStoreEntity, Long> {
    List<ContextStoreEntity> findAllBySourceId(Long sourceId);
    Optional<ContextStoreEntity> findBySourceIdAndEntityName(Long sourceId, String entityName);
}
```

- [ ] **Step 4: Create `ContextStoreRecordRepository`**

```java
@Repository
public interface ContextStoreRecordRepository
    extends PagingAndSortingRepository<ContextStoreRecord, Long>, ListCrudRepository<ContextStoreRecord, Long> {

    Optional<ContextStoreRecord> findByWorkspaceIdAndSourceIdAndEntityNameAndSourceRecordId(
        Long workspaceId, Long sourceId, String entityName, String sourceRecordId);

    @Modifying
    @Query("""
        UPDATE context_store_record
        SET deleted_at = :deletedAt, last_modified_date = :deletedAt
        WHERE source_id = :sourceId
          AND entity_name = :entityName
          AND source_record_id NOT IN (:seenIds)
          AND deleted_at IS NULL
        """)
    int tombstoneUnseen(
        @Param("sourceId") Long sourceId,
        @Param("entityName") String entityName,
        @Param("seenIds") Collection<String> seenIds,
        @Param("deletedAt") Instant deletedAt);
}
```

(Note: `@Modifying` is REQUIRED for string `@Query` UPDATE per project memory — without it Spring Data JDBC tries `executeQuery()` and fails with a misleading `DataIntegrityViolationException`.)

- [ ] **Step 5: Create `ContextStoreRecordIndexRepository`**

```java
@Repository
public interface ContextStoreRecordIndexRepository extends ListCrudRepository<ContextStoreRecordIndex, Long> {

    List<ContextStoreRecordIndex> findAllByRecordId(Long recordId);

    @Modifying
    @Query("DELETE FROM context_store_record_index WHERE record_id = :recordId")
    void deleteAllByRecordId(@Param("recordId") Long recordId);
}
```

- [ ] **Step 6: Create `ContextStoreSyncRunRepository`**

```java
@Repository
public interface ContextStoreSyncRunRepository
    extends PagingAndSortingRepository<ContextStoreSyncRun, Long>, ListCrudRepository<ContextStoreSyncRun, Long> {

    List<ContextStoreSyncRun> findAllBySourceIdOrderByStartedAtDesc(Long sourceId, Pageable pageable);

    Optional<ContextStoreSyncRun> findFirstBySourceIdOrderByStartedAtDesc(Long sourceId);
}
```

- [ ] **Step 7: Compile**

Run: `./gradlew :server:ee:libs:platform:platform-context-store:platform-context-store-service:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Commit**

```bash
git add server/ee/libs/platform/platform-context-store/platform-context-store-service/src/main/java/com/bytechef/automation/contextstore/repository/
git commit -m "Add ContextStore Spring Data JDBC repositories

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 8: Basic services (Source, Entity, Record, SyncRun)

**Files:**
- Create: `platform-context-store-api/.../service/ContextStoreSourceService.java` (interface)
- Create: `platform-context-store-api/.../service/ContextStoreEntityService.java` (interface)
- Create: `platform-context-store-api/.../service/ContextStoreRecordService.java` (interface)
- Create: `platform-context-store-api/.../service/ContextStoreSyncRunService.java` (interface)
- Create: `platform-context-store-service/.../service/ContextStoreSourceServiceImpl.java`
- Create: `platform-context-store-service/.../service/ContextStoreEntityServiceImpl.java`
- Create: `platform-context-store-service/.../service/ContextStoreRecordServiceImpl.java`
- Create: `platform-context-store-service/.../service/ContextStoreSyncRunServiceImpl.java`
- Create: `platform-context-store-service/src/test/java/com/bytechef/ee/platform/contextstore/service/ContextStoreSourceServiceIntTest.java`

This task aggregates four parallel services because each follows the identical CRUD-wrapper pattern that's well-established (`KnowledgeBaseServiceImpl.java` is the model).

- [ ] **Step 1: Read `KnowledgeBaseServiceImpl.java` and its `KnowledgeBaseService.java` interface**

```bash
cat server/libs/platform/platform-knowledge-base/platform-knowledge-base-api/src/main/java/com/bytechef/platform/knowledgebase/service/KnowledgeBaseService.java
cat server/libs/platform/platform-knowledge-base/platform-knowledge-base-service/src/main/java/com/bytechef/platform/knowledgebase/service/KnowledgeBaseServiceImpl.java
```

Pattern: interface in api module declares `getX(id)`, `fetchX(id)` (Optional), `createX(...)`, `updateX(...)`, `deleteX(id)`, `getAllByYyy(...)`. Impl in service module uses the repository directly with `@Service` and `@Transactional`.

- [ ] **Step 2: Write a failing service IntTest**

`ContextStoreSourceServiceIntTest.java`:

```java
package com.bytechef.ee.platform.contextstore.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.contextstore.config.ContextStoreIntTestConfiguration;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSourceStatus;
import com.bytechef.ee.platform.contextstore.domain.ReaderStrategy;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("testint")
@SpringBootTest(classes = ContextStoreIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class ContextStoreSourceServiceIntTest {

    @Autowired private ContextStoreSourceService contextStoreSourceService;

    @AfterEach
    void cleanup() {
        contextStoreSourceService.getAllByWorkspaceId(1L).forEach(s -> contextStoreSourceService.delete(s.getId()));
    }

    @Test
    void testCreateAndFetch() {
        ContextStoreSource src = new ContextStoreSource();
        src.setWorkspaceId(1L);
        src.setName("HubSpot Production");
        src.setSourceComponentName("hubspot");
        src.setSourceComponentVersion(1);
        src.setReaderStrategy(ReaderStrategy.LIST_ACTION);
        src.setSourceListActionName("searchContacts");
        src.setCadence("@hourly");
        src.setStatus(ContextStoreSourceStatus.BUILDING_PREVIEW);

        ContextStoreSource created = contextStoreSourceService.create(src);

        assertThat(created.getId()).isNotNull();
        assertThat(contextStoreSourceService.fetch(created.getId())).isPresent();
        assertThat(contextStoreSourceService.getAllByWorkspaceId(1L)).hasSize(1);
    }
}
```

- [ ] **Step 3: Create `ContextStoreIntTestConfiguration`**

```java
package com.bytechef.ee.platform.contextstore.config;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.bytechef.ee.platform.contextstore")
public class ContextStoreIntTestConfiguration {
}
```

(Pattern from `KnowledgeBaseIntTestConfiguration.java`. Per memory, may need additional `basePackages` for transitive `@Service` deps — add as needed when test fails.)

- [ ] **Step 4: Run test — expect failure (no service yet)**

Run: `./gradlew :server:ee:libs:platform:platform-context-store:platform-context-store-service:integrationTest --tests ContextStoreSourceServiceIntTest`
Expected: FAIL — `ContextStoreSourceService` cannot be resolved.

- [ ] **Step 5: Create `ContextStoreSourceService` interface**

```java
package com.bytechef.ee.platform.contextstore.service;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSourceStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public interface ContextStoreSourceService {
    ContextStoreSource create(ContextStoreSource source);
    ContextStoreSource update(ContextStoreSource source);
    void delete(Long id);
    ContextStoreSource get(Long id);
    Optional<ContextStoreSource> fetch(Long id);
    List<ContextStoreSource> getAllByWorkspaceId(Long workspaceId);
    List<ContextStoreSource> getAllEnabledByWorkspaceId(Long workspaceId);
    void updateStatus(Long id, ContextStoreSourceStatus status, @Nullable Instant lastSyncRunAt, @Nullable Long jobExecutionId);
    void setEnabled(Long id, boolean enabled);
}
```

- [ ] **Step 6: Create `ContextStoreSourceServiceImpl`**

```java
package com.bytechef.ee.platform.contextstore.service;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSourceStatus;
import com.bytechef.ee.platform.contextstore.repository.ContextStoreSourceRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ContextStoreSourceServiceImpl implements ContextStoreSourceService {

    private final ContextStoreSourceRepository repository;

    public ContextStoreSourceServiceImpl(ContextStoreSourceRepository repository) {
        this.repository = repository;
    }

    @Override
    public ContextStoreSource create(ContextStoreSource source) { return repository.save(source); }

    @Override
    public ContextStoreSource update(ContextStoreSource source) { return repository.save(source); }

    @Override
    public void delete(Long id) { repository.deleteById(id); }

    @Override
    @Transactional(readOnly = true)
    public ContextStoreSource get(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("ContextStoreSource " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ContextStoreSource> fetch(Long id) { return repository.findById(id); }

    @Override
    @Transactional(readOnly = true)
    public List<ContextStoreSource> getAllByWorkspaceId(Long workspaceId) {
        return repository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContextStoreSource> getAllEnabledByWorkspaceId(Long workspaceId) {
        return repository.findAllByWorkspaceIdAndEnabled(workspaceId, true);
    }

    @Override
    public void updateStatus(Long id, ContextStoreSourceStatus status, @Nullable Instant t, @Nullable Long jobId) {
        ContextStoreSource src = get(id);
        src.setStatus(status);

        if (t != null) {
            src.setLastSyncRunAt(t);
        }

        if (jobId != null) {
            src.setLastSyncJobExecutionId(jobId);
        }

        repository.save(src);
    }

    @Override
    public void setEnabled(Long id, boolean enabled) {
        ContextStoreSource src = get(id);
        src.setEnabled(enabled);

        repository.save(src);
    }
}
```

- [ ] **Step 7: Run test — expect PASS**

Run: same command from step 4.
Expected: PASS.

- [ ] **Step 8: Add `findAllActiveAcrossWorkspaces` to `ContextStoreSourceService`** (post-pivot: no longer needed by a scheduler — Atlas's workflow scheduling drives sync. Method may still be useful for admin tooling; remove if unused.)

```java
// in ContextStoreSourceService:
List<ContextStoreSource> findAllActiveAcrossWorkspaces();   // workspace-agnostic; admin tooling only post-DataStream pivot
```

```java
// in ContextStoreSourceServiceImpl:
@Override
@Transactional(readOnly = true)
public List<ContextStoreSource> findAllActiveAcrossWorkspaces() {
    return repository.findAllByEnabled(true);
}
```

Add to `ContextStoreSourceRepository`:

```java
List<ContextStoreSource> findAllByEnabled(boolean enabled);
```

- [ ] **Step 9: Create `ContextStoreEntityService` interface + impl + IntTest**

Interface:

```java
package com.bytechef.ee.platform.contextstore.service;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreEntity;
import java.util.List;
import java.util.Optional;

public interface ContextStoreEntityService {
    ContextStoreEntity create(ContextStoreEntity entity);
    ContextStoreEntity update(ContextStoreEntity entity);
    void delete(Long id);
    ContextStoreEntity get(Long id);
    Optional<ContextStoreEntity> fetch(Long id);
    List<ContextStoreEntity> getAllBySourceId(Long sourceId);
    Optional<ContextStoreEntity> fetchBySourceIdAndEntityName(Long sourceId, String entityName);
}
```

Impl: structurally identical to `ContextStoreSourceServiceImpl` (delegate to repository, `@Service @Transactional`, readOnly on get/fetch/getAll). Add a one-test IntTest for create/fetch round-trip.

- [ ] **Step 10: Create `ContextStoreRecordService` interface + impl + IntTest**

Interface:

```java
package com.bytechef.ee.platform.contextstore.service;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreRecord;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

public interface ContextStoreRecordService {
    ContextStoreRecord save(ContextStoreRecord record);
    Optional<ContextStoreRecord> fetchByKey(Long workspaceId, Long sourceId, String entityName, String sourceRecordId);
    int tombstoneUnseen(Long sourceId, String entityName, Collection<String> seenIds, Instant deletedAt);
    void delete(Long id);
}
```

Impl wraps `ContextStoreRecordRepository`. The `tombstoneUnseen` method delegates to the repository's `@Modifying @Query` method declared in Task 7.

- [ ] **Step 11: Create `ContextStoreSyncRunService` interface + impl + IntTest**

Interface:

```java
package com.bytechef.ee.platform.contextstore.service;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreSyncRun;
import java.util.List;
import java.util.Optional;

public interface ContextStoreSyncRunService {
    ContextStoreSyncRun create(ContextStoreSyncRun syncRun);
    ContextStoreSyncRun update(ContextStoreSyncRun syncRun);
    ContextStoreSyncRun get(Long id);
    Optional<ContextStoreSyncRun> fetch(Long id);
    Optional<ContextStoreSyncRun> fetchByJobExecutionId(Long jobExecutionId);
    Optional<ContextStoreSyncRun> getLatestForSource(Long sourceId);
    List<ContextStoreSyncRun> getRecentForSource(Long sourceId, int limit);
}
```

Add a corresponding repository method:

```java
// in ContextStoreSyncRunRepository:
Optional<ContextStoreSyncRun> findByJobExecutionId(Long jobExecutionId);
```

Impl wraps the repository. `getRecentForSource` uses the existing `findAllBySourceIdOrderByStartedAtDesc(Long, Pageable)` with `PageRequest.of(0, limit)`.

- [ ] **Step 12: Run all 4 service IntTests**

Run: `./gradlew :server:ee:libs:platform:platform-context-store:platform-context-store-service:integrationTest`
Expected: 4 PASS.

- [ ] **Step 13: Commit**

```bash
git add server/ee/libs/platform/platform-context-store/
git commit -m "Add ContextStore CRUD services (Source/Entity/Record/SyncRun)

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Phase 4: Query service (EE platform-CS)

### Task 9: Query DTO + Filter/Sort + SearchResult

**Files:**
- Create: `platform-context-store-api/.../dto/ContextStoreQuery.java`
- Create: `platform-context-store-api/.../dto/ContextStoreQueryFilter.java`
- Create: `platform-context-store-api/.../dto/ContextStoreQuerySort.java`
- Create: `platform-context-store-api/.../dto/ContextStoreSearchResult.java`

- [ ] **Step 1: Create the DTO records**

```java
// ContextStoreQueryFilter.java
package com.bytechef.ee.platform.contextstore.dto;

public record ContextStoreQueryFilter(String field, FilterOp op, Object value) {

    public enum FilterOp {
        EQ, NEQ, IN, CONTAINS, STARTS_WITH, GT, GTE, LT, LTE, BETWEEN
    }
}
```

```java
// ContextStoreQuerySort.java
package com.bytechef.ee.platform.contextstore.dto;

public record ContextStoreQuerySort(String field, SortDirection dir) {

    public enum SortDirection { ASC, DESC }
}
```

```java
// ContextStoreQuery.java
package com.bytechef.ee.platform.contextstore.dto;

import java.util.List;
import org.jspecify.annotations.Nullable;

public record ContextStoreQuery(
    Long workspaceId,
    Long sourceId,
    String entityName,
    List<ContextStoreQueryFilter> filters,
    List<ContextStoreQuerySort> sort,
    int limit,
    @Nullable String cursor,
    boolean includeDeleted,
    @Nullable List<String> fields) {

    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 500;

    public ContextStoreQuery {
        if (limit <= 0 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be in (0, " + MAX_LIMIT + "]");
        }

        filters = filters == null ? List.of() : List.copyOf(filters);
        sort = sort == null ? List.of() : List.copyOf(sort);
    }
}
```

```java
// ContextStoreSearchResult.java
package com.bytechef.ee.platform.contextstore.dto;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreRecord;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record ContextStoreSearchResult(List<ContextStoreRecord> items, @Nullable String nextCursor) {}
```

- [ ] **Step 2: Compile**

Run: `./gradlew :server:ee:libs:platform:platform-context-store:platform-context-store-api:compileJava`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/platform/platform-context-store/platform-context-store-api/src/main/java/com/bytechef/automation/contextstore/dto/
git commit -m "Add ContextStore query DTOs (Query, Filter, Sort, SearchResult)

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 10: ContextStoreQueryService — search() with filter→SQL translation + cursor pagination

**Files:**
- Create: `platform-context-store-api/.../service/ContextStoreQueryService.java`
- Create: `platform-context-store-service/.../service/ContextStoreQueryServiceImpl.java`
- Create: `platform-context-store-service/src/test/.../service/ContextStoreQueryServiceIntTest.java`

This is the most non-trivial service. Filter ops translate to a JOIN against `context_store_record_index`; cursor is `(lastSortValue, lastRecordId)` base64'd.

- [ ] **Step 1: Write a failing test for `search` with EQ filter**

```java
@ActiveProfiles("testint")
@SpringBootTest(classes = ContextStoreIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class ContextStoreQueryServiceIntTest {

    @Autowired private ContextStoreQueryService queryService;
    @Autowired private ContextStoreSourceService sourceService;
    @Autowired private ContextStoreEntityService entityService;
    @Autowired private ContextStoreRecordService recordService;

    @Test
    void testSearchWithEqFilter() {
        Long workspaceId = 1L;
        Long sourceId = givenSource(workspaceId, "hubspot", "contacts").getId();
        givenEntity(sourceId, "contacts", "id", List.of(Map.of("name", "company.name", "type", "TEXT")));

        givenRecord(workspaceId, sourceId, "contacts", "c1",
            Map.of("id", "c1", "company", Map.of("name", "Acme")));
        givenRecord(workspaceId, sourceId, "contacts", "c2",
            Map.of("id", "c2", "company", Map.of("name", "Globex")));

        ContextStoreSearchResult result = queryService.search(new ContextStoreQuery(
            workspaceId, sourceId, "contacts",
            List.of(new ContextStoreQueryFilter("company.name", ContextStoreQueryFilter.FilterOp.EQ, "Acme")),
            List.of(),
            10, null, false, null));

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).getSourceRecordId()).isEqualTo("c1");
        assertThat(result.nextCursor()).isNull();
    }

    // Helper methods: givenSource, givenEntity, givenRecord — wrap calls to the corresponding services
}
```

- [ ] **Step 2: Run — expect compilation failure (`ContextStoreQueryService` doesn't exist)**

Run: `./gradlew ...:integrationTest --tests ContextStoreQueryServiceIntTest`
Expected: FAIL.

- [ ] **Step 3: Create the interface**

```java
package com.bytechef.ee.platform.contextstore.service;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreRecord;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreQuery;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreSearchResult;
import java.util.Optional;

public interface ContextStoreQueryService {
    ContextStoreSearchResult search(ContextStoreQuery query);
    Optional<ContextStoreRecord> get(Long workspaceId, Long sourceId, String entityName, String sourceRecordId);
}
```

- [ ] **Step 4: Implement `ContextStoreQueryServiceImpl` — minimal viable: handle EQ filter only first**

The full implementation is non-trivial; build it incrementally. Start with single-filter EQ on a text-indexed field:

```java
@Service
@Transactional(readOnly = true)
public class ContextStoreQueryServiceImpl implements ContextStoreQueryService {

    private final NamedParameterJdbcTemplate jdbc;
    private final ContextStoreRecordRepository recordRepository;
    private final ObjectMapper objectMapper;

    public ContextStoreQueryServiceImpl(NamedParameterJdbcTemplate jdbc,
                                        ContextStoreRecordRepository recordRepository,
                                        ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.recordRepository = recordRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public ContextStoreSearchResult search(ContextStoreQuery query) {
        StringBuilder sql = new StringBuilder("""
            SELECT r.id, r.workspace_id, r.source_id, r.entity_name, r.source_record_id,
                   r.payload, r.payload_hash, r.last_seen_at, r.deleted_at,
                   r.created_date, r.last_modified_date
              FROM context_store_record r
            """);

        Map<String, Object> params = new HashMap<>();
        List<String> wheres = new ArrayList<>();
        wheres.add("r.workspace_id = :workspaceId");
        params.put("workspaceId", query.workspaceId());
        wheres.add("r.source_id = :sourceId");
        params.put("sourceId", query.sourceId());
        wheres.add("r.entity_name = :entityName");
        params.put("entityName", query.entityName());

        if (!query.includeDeleted()) {
            wheres.add("r.deleted_at IS NULL");
        }

        int filterIndex = 0;

        for (ContextStoreQueryFilter filter : query.filters()) {
            String alias = "i" + filterIndex++;

            sql.append(" JOIN context_store_record_index ").append(alias)
                .append(" ON ").append(alias).append(".record_id = r.id");

            wheres.add(alias + ".field_name = :" + alias + "_field");
            params.put(alias + "_field", filter.field());

            switch (filter.op()) {
                case EQ -> {
                    wheres.add(alias + ".value_text = :" + alias + "_val");
                    params.put(alias + "_val", filter.value().toString());
                }
                case CONTAINS -> {
                    wheres.add(alias + ".value_text ILIKE :" + alias + "_val");
                    params.put(alias + "_val", "%" + filter.value() + "%");
                }
                // ... NEQ, IN, STARTS_WITH, GT, GTE, LT, LTE, BETWEEN — fill in incrementally
                default -> throw new UnsupportedOperationException("Filter op " + filter.op() + " not yet implemented");
            }
        }

        sql.append(" WHERE ").append(String.join(" AND ", wheres));

        // Cursor: simple offset for MVP first; replace with (lastSortValue, lastRecordId) tuple later in this task
        sql.append(" ORDER BY r.id ASC");
        sql.append(" LIMIT :limit");
        params.put("limit", query.limit() + 1);    // fetch one extra to detect nextCursor

        if (query.cursor() != null) {
            wheres.add("r.id > :cursor");
            params.put("cursor", Long.parseLong(new String(Base64.getDecoder().decode(query.cursor()))));
        }

        List<ContextStoreRecord> items = jdbc.query(sql.toString(), params, new ContextStoreRecordRowMapper());

        String nextCursor = null;

        if (items.size() > query.limit()) {
            ContextStoreRecord last = items.get(query.limit() - 1);
            nextCursor = Base64.getEncoder().encodeToString(last.getId().toString().getBytes());
            items = items.subList(0, query.limit());
        }

        return new ContextStoreSearchResult(items, nextCursor);
    }

    @Override
    public Optional<ContextStoreRecord> get(Long workspaceId, Long sourceId, String entityName, String sourceRecordId) {
        return recordRepository.findByWorkspaceIdAndSourceIdAndEntityNameAndSourceRecordId(
            workspaceId, sourceId, entityName, sourceRecordId);
    }

    // ContextStoreRecordRowMapper inner class, maps result rows to ContextStoreRecord
}
```

- [ ] **Step 5: Run test — expect PASS for the EQ case**

Run: same command from step 2.
Expected: PASS.

- [ ] **Step 6: Add tests for each remaining filter op**

One test per op: NEQ, IN, CONTAINS, STARTS_WITH, GT, GTE, LT, LTE, BETWEEN. Each test creates a few records, runs the query, asserts the right subset.

- [ ] **Step 7: Implement remaining ops in the `switch` block**

Full op set:
- `NEQ`: `value_text <> :val`
- `IN`: `value_text IN (:vals)` — value is a `List<Object>`; bind as `:vals`
- `STARTS_WITH`: `value_text ILIKE :val || '%'`
- `GT/GTE/LT/LTE`: numeric column or timestamp column based on inferred type — examine `value` type at runtime
- `BETWEEN`: `value_x BETWEEN :lo AND :hi` — value is `[lo, hi]`

For numeric/timestamp filters, look up the field's typed column from `context_store_record_index` (`value_numeric` or `value_timestamp`) — implementer must consult `ContextStoreEntity.indexedFields` JSON to know the field's declared type.

- [ ] **Step 8: Add cursor stability test**

```java
@Test
void testCursorPaginationIsStable() {
    // Insert 7 records, page size 3 → expect 3 + 3 + 1 with two cursor hops
    // ...
}
```

- [ ] **Step 9: Add includeDeleted test**

Tombstone one record (set deleted_at), then search with includeDeleted=false (expect not in result) and includeDeleted=true (expect in result).

- [ ] **Step 10: Add sort test**

Sort by an indexed text field ASC/DESC, assert result order.

- [ ] **Step 11: Run all query tests**

Run: `./gradlew ...:integrationTest --tests ContextStoreQueryServiceIntTest`
Expected: all PASS.

- [ ] **Step 12: Commit**

```bash
git add server/ee/libs/platform/platform-context-store/
git commit -m "Add ContextStoreQueryService with structured-filter SQL translation

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Phase 5: Sync engine — DataStream + auto-generated workflow (EE)

> **2026-05-08 pivot**: this phase was redesigned. Previously it built a parallel Spring Batch sync engine with custom Launcher/Scheduler/SyncRun classes (Tasks 12-19 in the original plan). After the pivot, sync is driven by the existing `data-stream.stream` action wrapped in an auto-generated workflow per Context Source. Only **one** new piece of sync code is needed: a DESTINATION cluster element (`contextStore.writeToReplica`) plus a `JobExecutionListener` for tombstone-on-completion. Atlas's workflow engine handles cron, retry, observability, manual runs, and Worker dispatch.
>
> The plan reads "Tasks 12-16 cover the new Phase 5", then jumps to Task 20 — Tasks 17-19 (end-to-end IntTest + Launcher + Scheduler) are **dropped** because the new Tasks 14-15 cover what's still needed.

### Task 11: PayloadHashUtil

**Files:**
- Create: `platform-context-store-service/.../util/PayloadHashUtil.java`
- Create: corresponding unit test

- [ ] **Step 1: Write failing test**

```java
class PayloadHashUtilTest {
    @Test
    void testHashIsStableAcrossKeyOrder() {
        String h1 = PayloadHashUtil.hash(Map.of("a", 1, "b", 2));
        String h2 = PayloadHashUtil.hash(Map.of("b", 2, "a", 1));
        assertThat(h1).isEqualTo(h2);
        assertThat(h1).hasSize(16);
    }

    @Test
    void testHashChangesWhenValueChanges() {
        String h1 = PayloadHashUtil.hash(Map.of("name", "Alice"));
        String h2 = PayloadHashUtil.hash(Map.of("name", "Bob"));
        assertThat(h1).isNotEqualTo(h2);
    }
}
```

- [ ] **Step 2: Run — expect failure**

- [ ] **Step 3: Implement**

```java
package com.bytechef.ee.platform.contextstore.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;
import tools.jackson.databind.ObjectMapper;

public final class PayloadHashUtil {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private PayloadHashUtil() {}

    /**
     * Returns the first 8 bytes of SHA-256 of the canonical (key-sorted) JSON serialization, hex-encoded (16 chars).
     * Per project memory: prefer SHA-256 first 8 bytes for deterministic long IDs.
     */
    public static String hash(Map<String, ?> payload) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(canonicalize(payload));
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] full = md.digest(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(16);

            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", full[i]));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object canonicalize(Object value) {
        if (value instanceof Map<?, ?> map) {
            TreeMap<String, Object> sorted = new TreeMap<>();
            map.forEach((k, v) -> sorted.put(k.toString(), canonicalize(v)));

            return sorted;
        }

        if (value instanceof java.util.List<?> list) {
            return list.stream().map(PayloadHashUtil::canonicalize).toList();
        }

        return value;
    }
}
```

- [ ] **Step 4: Run — expect PASS**

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/platform/platform-context-store/platform-context-store-service/src/main/java/com/bytechef/automation/contextstore/util/ \
        server/ee/libs/platform/platform-context-store/platform-context-store-service/src/test/java/com/bytechef/automation/contextstore/util/PayloadHashUtilTest.java
git commit -m "Add PayloadHashUtil for stable change detection

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 12: ContextStoreItemWriter (DataStream DESTINATION cluster element)

**Files:**
- Create: `server/ee/libs/modules/components/context-store/src/main/java/com/bytechef/ee/component/contextstore/destination/ContextStoreItemWriter.java`
- Create: matching unit test
- Modify: `ContextStoreComponentHandler` to register the new DESTINATION cluster element

`ContextStoreItemWriter` implements ByteChef's existing `ItemWriter` SPI (the same interface Airtable/CSV/JSON DESTINATIONs use). It's a DataStream cluster element of type `DESTINATION` on the `contextStore` component.

Lifecycle (from `ItemWriter` interface, lifecycle methods inherited from `ItemStream`):
- `open(inputParameters, connectionParameters, context, executionContext)` — reads `sourceId`, `entityName`, `idField`, `indexedFields`, `storedFields` from `inputParameters`. Initializes `seenIds` set in `executionContext`.
- `write(List<Map<String, Object>> records)` — for each record:
  1. Resolve `sourceRecordId = String.valueOf(record.get(idField))`.
  2. If `storedFields` is non-null, apply field whitelist (filter to listed dotted paths via a small `FieldFilterUtil`).
  3. Compute `payload_hash = PayloadHashUtil.hash(filteredRecord)`.
  4. Find or upsert `ContextStoreRecord` by `(workspaceId, sourceId, entityName, sourceRecordId)`. workspaceId resolved at `open()` from the source's row.
  5. If unchanged hash + existing record → cheap path (UPDATE last_seen_at only).
  6. Else → upsert payload + payload_hash, clear `deleted_at`, rebuild index rows.
  7. `executionContext.<seenIds>.add(sourceRecordId)`.
- `update(executionContext)` — persists incremental progress (same as existing DataStream sources).
- `close()` — no-op; tombstone is handled by `JobExecutionListener` at job-end.

Cluster-element registration in `ContextStoreComponentHandler`: add to `.clusterElements(...)` (or wherever the existing TOOLS cluster elements are registered) a `clusterElement("writeToReplica").type(DESTINATION).object(ContextStoreItemWriter.class).properties(...)` declaration.

Properties on the cluster element: `sourceId` (long), `entityName` (string), `idField` (string), `indexedFields` (array — JSON), `storedFields` (array — JSON, nullable).

Unit test: mock the `ContextStoreRecordService`/repository, exercise: new-record insert, unchanged-hash cheap path, changed-hash with index rebuild, `storedFields` filter application.

### Task 13: ContextStoreSyncJobListener (tombstone + status updates)

**Files:**
- Create: `platform-context-store-service/src/main/java/com/bytechef/ee/platform/contextstore/listener/ContextStoreSyncJobListener.java`

`ContextStoreSyncJobListener` is a `@Component`-annotated `JobExecutionListener` registered via Spring DI. It runs on every Spring Batch job (every DataStream `stream` action invokes Spring Batch internally), but only acts when the job's destination is `contextStore.writeToReplica`.

```java
@Component
public class ContextStoreSyncJobListener implements JobExecutionListener {
    public void beforeJob(JobExecution jobExecution) {
        if (!isContextStoreDestination(jobExecution)) return;
        Long sourceId = extractSourceId(jobExecution);
        sourceService.updateStatus(sourceId, BUILDING_PREVIEW, null, null);
    }

    public void afterJob(JobExecution jobExecution) {
        if (!isContextStoreDestination(jobExecution)) return;
        Long sourceId = extractSourceId(jobExecution);
        String entityName = extractEntityName(jobExecution);
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            Set<String> seenIds = aggregateSeenIds(jobExecution);
            int tombstoned = recordService.tombstoneUnseen(sourceId, entityName, seenIds, now());
            sourceService.updateStatus(sourceId, READY, now(), jobExecution.getId());
        } else {
            sourceService.updateStatus(sourceId, FAILED, null, jobExecution.getId());
        }
    }
}
```

Detection helpers (`isContextStoreDestination`, `extractSourceId`, `extractEntityName`, `aggregateSeenIds`) read from `JobExecution.getJobParameters()` (which DataStream populates with the cluster-element identification — including the destination component name + cluster-element name + parameters).

Unit test the detection logic + the COMPLETED/FAILED state transitions with mocked services.

### Task 14: WorkspaceContextStoreSourceFacade — auto-generate workflow on create

> **Post-2026-05-09 platform pivot delta** (after the second 2026-05-09 pivot, commit `64bf8e1fc5d`): the facade lives in `automation-context-store-{api,service}` (NOT in platform). The Impl talks directly to `WorkspaceContextStoreSourceRepository` (same module) to insert/delete the `workspace_context_store_source` relation row + to platform `ContextStoreSourceService` for entity CRUD + to atlas-coordinator/workflow-execution APIs for workflow auto-gen + ProjectDeploymentWorkflow lifecycle. Method signatures all take `Long workspaceId` as the first parameter (e.g., `create(workspaceId, input)`, `delete(workspaceId, id)`). No SPI seam — automation imports platform-api directly, which is the legal direction. The intermediate `ContextStoreWorkspaceResolver` SPI introduced in commit `baa1f1fe311` was deleted in `64bf8e1fc5d`.

**Files:**
- Create: `automation-context-store-api/src/main/java/com/bytechef/ee/automation/contextstore/facade/WorkspaceContextStoreSourceFacade.java` (interface)
- (No SPI seam — the workspace-aware facade lives in automation alongside the relation repository per the second 2026-05-09 pivot.)
- Create: `automation-context-store-service/src/main/java/com/bytechef/ee/automation/contextstore/facade/WorkspaceContextStoreSourceFacadeImpl.java`
- Modify: `ContextStoreSource` entity — add `workflow_id` field as `AggregateReference<Workflow, Long>`, with column `workflow_id BIGINT`
- Modify: existing Liquibase changeset — add `workflow_id` column to `context_store_source` (the changeset hasn't been applied to any production DB yet on this branch; edit-in-place is acceptable per CLAUDE.md "After renaming migration files, delete stale copies from build/resources/" pattern)
- Drop: `context_store_sync_run` table from the same Liquibase changeset (delete the entire `<createTable tableName="context_store_sync_run">` block + its FK + index — not used anymore)

`WorkspaceContextStoreSourceFacade.create(workspaceId, CreateContextStoreSourceInput input)`:
1. INSERT `context_store_source` row (status=BUILDING_PREVIEW, workflow_id=null initially). NO `workspace_id` on the row anymore.
2. INSERT `workspace_context_store_source` relation row `(workspaceId, sourceId)` via the SPI's `recordWorkspaceOwnership(workspaceId, sourceId)` hook.
3. INSERT `context_store_entity` rows.
4. **Build a workflow definition** matching the spec §6 template:
   - Trigger: `schedule/v1/cronTrigger` with `cron` translated from `input.cadence` (`@hourly` → `0 0 * * * *`, `@daily` → `0 0 0 * * *`, otherwise pass-through cron).
   - Single task: `data-stream/v1/stream` with cluster elements: SOURCE = `<input.sourceComponentName>.<input.sourceClusterElementName>`, DESTINATION = `contextStore.writeToReplica` parameterized with `{sourceId, entityName, idField, indexedFields, storedFields}`.
   - Workflow `metadata.contextStoreSourceId = sourceId` for ownership marking.
   - Project ownership: ask the resolver SPI for `resolveWorkspaceProjectId(workspaceId)` to know which project to wire the workflow into.
5. Persist the workflow via `WorkflowService.createWorkflow(...)`. Persist a `ProjectDeploymentWorkflow` row to wire it into the resolved project.
6. UPDATE `context_store_source.workflow_id = persistedWorkflow.id`.
7. Trigger initial sync immediately by creating a Job against the workflow.

`WorkspaceContextStoreSourceFacade.update(workspaceId, ...)`:
- If `cadence` changed: load the workflow, mutate its trigger's `cron` parameter, save. Don't regenerate the whole workflow.
- If `enabled` changed: enable/disable the workflow's `ProjectDeploymentWorkflow` accordingly.

`WorkspaceContextStoreSourceFacade.delete(workspaceId, id)`:
- Delete the workflow + its `ProjectDeploymentWorkflow` (cascades from source FK delete? — verify; if not, delete explicitly).
- Delete the `context_store_source` row. Cascades to entities + records + index. The `workspace_context_store_source` relation row also cascades via its FK ON DELETE CASCADE.

`WorkspaceContextStoreSourceFacade.refreshNow(id)`:
- Create a Job against the source's workflow with manual-run parameters.

IntTest: create a source via the facade, assert workflow was generated with right structure (read it back via WorkflowService), assert initial Job was triggered, assert workflow_id is populated on the source row.

### Task 15: End-to-end sync IntTest

**Files:**
- Create: `platform-context-store-service/src/test/.../ContextStoreSyncIntTest.java`

Test scenario:
1. Register a fake `ItemReader` cluster element via the test `ComponentDefinitionRegistry` (or use the existing CSV/JSON sources with a fixture file).
2. Create a `ContextStoreSource` via `WorkspaceContextStoreSourceFacade.create(...)` pointing at the fake source. Assert the auto-generated workflow exists.
3. Trigger a sync via `WorkspaceContextStoreSourceFacade.refreshNow(sourceId)`. Wait for the JobExecution to complete.
4. Assert: `context_store_record` rows match the fake source's emit; `context_store_record_index` rows match the indexed fields; source status flipped to READY.
5. Modify a record in the fake source, trigger another sync. Assert: `payload` updated, `payload_hash` changed, `last_modified_date` advanced, index rows rebuilt.
6. Remove a record from the fake source, trigger sync. Assert: `deleted_at` set on the missing record (tombstoned).
7. Resync — assert tombstoned record stays tombstoned (not seen → not un-tombstoned).

This single IntTest replaces the previous Tasks 17 (sync IntTest) — it exercises the full DataStream + workflow path end-to-end.

### Task 16: GraphQL surface (CS) — mostly unchanged, with workflow lifecycle hooks

**Files:**
- Create: `automation-context-store-graphql/.../web/graphql/ContextStoreSourceGraphQlController.java`
- Create: GraphQL schema + DTOs

Schema is unchanged from the earlier plan. Mutations route through `WorkspaceContextStoreSourceFacade` (not the underlying services directly), so workflow auto-generation happens transparently:
- `createContextStoreSource(input)` → `WorkspaceContextStoreSourceFacade.create(input)` → workflow auto-gen.
- `updateContextStoreSource(id, input)` → `WorkspaceContextStoreSourceFacade.update(input)` → workflow trigger updated if cadence changed.
- `deleteContextStoreSource(id)` → `WorkspaceContextStoreSourceFacade.delete(id)` → workflow + source deleted.
- `refreshContextStoreSource(id)` → `WorkspaceContextStoreSourceFacade.refreshNow(id)` → Job created against workflow.

GraphQL IntTest verifies the round-trip: create source through GraphQL, fetch source, see populated `workflow.id` field on the response.

> **Tasks 17-19 dropped via DataStream pivot** — the original plan had:
> - Task 17: End-to-end sync IntTest (now Task 15)
> - Task 18: ContextStoreSyncLauncher (replaced by Atlas's standard JobService.create against the auto-generated workflow)
> - Task 19: ContextStoreSyncScheduler (replaced by the workflow's own schedule.cronTrigger)

> **Phases 6 and 7 dropped via DataStream pivot** — the original plan had Phase 6 ("Spring Batch sync orchestration: ContextStoreSyncJob + ContextStoreBatchConfiguration + Launcher + Scheduler") and Phase 7 ("Atlas wiring for distributed dispatch"). Both collapsed into Phase 5 once the auto-generated workflow + DataStream destination model replaced the Spring-Batch-direct design. The phase numbering jumps 5 → 8 to preserve task-id references inside the plan rather than renumbering 30+ already-completed tasks. See spec §6 and §16 decision log "DataStream pivot for sync orchestration".

---

## Phase 8: Synthetic component (EE)

> **Post-2026-05-09 platform pivot**: this phase was originally labeled CE; the component lives at `server/ee/libs/modules/components/context-store/` per spec §4 (Option B EE-only). Imports reference platform-CS (`com.bytechef.ee.platform.contextstore.*`) for domain types and services. Actions take `sourceId` via inputParameters; the workspace-scope check is upstream (post-2026-05-09 SPI deletion).

### Task 20: ContextStoreSearchAction + ContextStoreGetAction + ContextStoreComponentHandler

**Files:**
- Create: `components/context-store/.../ContextStoreComponentHandler.java`
- Create: `components/context-store/.../action/ContextStoreSearchAction.java`
- Create: `components/context-store/.../action/ContextStoreGetAction.java`
- Create: `components/context-store/.../util/ContextStoreOptionsUtils.java`
- Create: `components/context-store/src/test/.../ContextStoreComponentHandlerIntTest.java`

- [ ] **Step 1: Implement `ContextStoreSearchAction`**

```java
package com.bytechef.ee.component.contextstore.action;

import static com.bytechef.component.definition.ComponentDsl.*;
import com.bytechef.ee.component.contextstore.util.ContextStoreOptionsUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;

public class ContextStoreSearchAction {
    public static final ModifiableActionDefinition ACTION_DEFINITION = action("search")
        .title("Search Context Store")
        .description("Query the workspace's replicated source data with structured filters.")
        .properties(
            integer("sourceId").label("Source").required(true)
                .options((ActionDefinition.OptionsFunction<Long>) ContextStoreOptionsUtils::getSourceOptions),
            string("entity").label("Entity").required(true)
                .options((ActionDefinition.OptionsFunction<String>) ContextStoreOptionsUtils::getEntityOptions)
                .optionsLookupDependsOn("sourceId"),
            array("filters").label("Filters").required(false)
                .description("List of {field, op, value} filter objects."),
            array("sort").label("Sort").required(false),
            integer("limit").label("Limit").defaultValue(50),
            string("cursor").label("Cursor").required(false),
            bool("includeDeleted").label("Include Deleted").defaultValue(false))
        .perform((inputParameters, connectionParameters, context) -> {
            // Resolve ContextStoreQueryService bean and call search()
            // Return Map<String, Object> with items + nextCursor
        })
        .output();      // dynamic output schema based on entity's indexed fields
}
```

(Pattern from `AirtableGetRecordAction.java:44–66` for `optionsLookupDependsOn`.)

- [ ] **Step 2: Implement `ContextStoreGetAction`**

Similar shape — `sourceId`, `entity` (cascading), `sourceRecordId`, returns single record or null.

- [ ] **Step 3: Implement `ContextStoreComponentHandler`**

```java
package com.bytechef.ee.component.contextstore;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.ee.component.contextstore.action.ContextStoreGetAction;
import com.bytechef.ee.component.contextstore.action.ContextStoreSearchAction;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

@AutoService(ComponentHandler.class)
public class ContextStoreComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("contextStore")
        .title("Context Store")
        .description("Search and fetch records from the workspace's replicated source data.")
        .icon("path:assets/context-store.svg")
        .categories(ComponentCategory.HELPERS)
        .actions(
            ContextStoreSearchAction.ACTION_DEFINITION,
            ContextStoreGetAction.ACTION_DEFINITION)
        .version(1);

    @Override
    public ComponentDefinition getDefinition() { return COMPONENT_DEFINITION; }
}
```

(Pattern from `CryptoHelperComponentHandler.java:38–70`.)

- [ ] **Step 4: Implement `ContextStoreOptionsUtils` for dynamic dropdowns**

Inject `ContextStoreSourceService` and `ContextStoreEntityService` via Spring (component context) and return `List<Option>` of sources / entities for the workspace.

- [ ] **Step 5: Add `ContextStoreComponentHandlerIntTest`**

```java
class ContextStoreComponentHandlerIntTest {
    @Test
    void testDefinitionGenerates() {
        // Triggers JSON definition file auto-generation under src/test/resources/definition/
        // Per project memory: delete existing JSON + build/resources/test/definition/ before running to regenerate
    }
}
```

- [ ] **Step 6: Run, regenerate definition file, commit**

```bash
rm -rf server/ee/libs/modules/components/context-store/src/test/resources/definition/
rm -rf server/ee/libs/modules/components/context-store/build/resources/test/definition/
./gradlew :server:ee:libs:modules:components:context-store:test
git add server/ee/libs/modules/components/context-store/
git commit -m "Add contextStore component (search + get actions)

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Phase 9: GraphQL (EE — automation-side per "graphql only" rule)

### Task 21: GraphQL schema + ContextStoreSourceGraphQlController

> **Post-2026-05-09 platform pivot** (after both 2026-05-09 pivots): the controller stays under `server/ee/libs/automation/automation-context-store/automation-context-store-graphql/` and keeps its package `com.bytechef.ee.automation.contextstore.web.graphql.*` per the "in automation only leave graphql" rule. Imports update to `com.bytechef.ee.platform.contextstore.*` (domain types, DTOs, the platform `ContextStoreSourceService` / `ContextStoreEntityService` / `ContextStoreQueryService`) AND to `com.bytechef.ee.automation.contextstore.*` (the new `WorkspaceContextStoreSourceFacade` + `WorkspaceContextStoreSourceService`). The controller calls `WorkspaceContextStoreSourceService` for workspace-scoped reads and `WorkspaceContextStoreSourceFacade` for mutations (passing `workspaceId` from the input). The facade itself inserts the `workspace_context_store_source` relation row when a source is created — no SPI involved. The `ContextStoreSource.workspaceId` GraphQL field becomes a `@SchemaMapping` that calls `WorkspaceContextStoreSourceService` (since the entity itself no longer carries `workspace_id`); the schema field is nullable because the relation row may not exist for orphaned sources.

**Files:**
- Create: `automation-context-store-graphql/src/main/resources/graphql/context-store.graphqls`
- Create: `automation-context-store-graphql/.../web/graphql/ContextStoreSourceGraphQlController.java` (package `com.bytechef.ee.automation.contextstore.web.graphql`)
- Create: `automation-context-store-graphql/.../web/graphql/dto/CreateContextStoreSourceInput.java`
- Create: `automation-context-store-graphql/.../web/graphql/dto/UpdateContextStoreSourceInput.java`

- [ ] **Step 1: Define schema**

```graphql
# context-store.graphqls
extend type Workspace {
    contextStoreSources(filter: ContextStoreSourceFilter): [ContextStoreSource!]!
    contextStoreSource(id: ID!): ContextStoreSource
}

type ContextStoreSource {
    id: ID!
    workspaceId: ID!
    name: String!
    sourceComponentName: String!
    sourceComponentVersion: Int!
    readerStrategy: ContextStoreReaderStrategy!
    sourceClusterElementName: String
    sourceListActionName: String
    connection: Connection
    cadence: String!
    status: ContextStoreSourceStatus!
    enabled: Boolean!
    lastSyncRunAt: DateTime
    workflow: Workflow                              # the auto-generated sync workflow
    entities: [ContextStoreEntity!]!
}

type ContextStoreEntity {
    id: ID!
    sourceId: ID!
    entityName: String!
    description: String
    idField: String!
    indexedFields: JSON!
    parameters: JSON
}

# (No ContextStoreSyncRun type — sync history surfaced via Atlas's standard JobExecution rows for the source's workflow.)

enum ContextStoreReaderStrategy {
    CLUSTER_ELEMENT
    LIST_ACTION
}

enum ContextStoreSourceStatus {
    BUILDING_PREVIEW
    PREVIEW
    READY
    FAILED
    DISABLED
}

input CreateContextStoreSourceInput {
    workspaceId: ID!
    name: String!
    sourceComponentName: String!
    sourceComponentVersion: Int!
    readerStrategy: ContextStoreReaderStrategy!
    sourceClusterElementName: String
    sourceListActionName: String
    connectionId: ID
    cadence: String!
    entities: [CreateContextStoreEntityInput!]!
}

input CreateContextStoreEntityInput {
    entityName: String!
    description: String
    idField: String!
    indexedFields: JSON!
    parameters: JSON
}

input UpdateContextStoreSourceInput {
    name: String
    cadence: String
    enabled: Boolean
}

input ContextStoreSourceFilter {
    enabled: Boolean
}

extend type Mutation {
    createContextStoreSource(input: CreateContextStoreSourceInput!): ContextStoreSource!
    updateContextStoreSource(id: ID!, input: UpdateContextStoreSourceInput!): ContextStoreSource!
    deleteContextStoreSource(id: ID!): Boolean!
    refreshContextStoreSource(id: ID!): JobExecution!     # admin-only; returns Atlas JobExecution row
    setContextStoreSourceEnabled(id: ID!, enabled: Boolean!): ContextStoreSource!
}
```

- [ ] **Step 2: Add the schema to client codegen config**

Edit `client/codegen.ts` and add `'server/ee/libs/automation/automation-context-store/automation-context-store-graphql/src/main/resources/graphql/context-store.graphqls'` to the schema array (per CLAUDE.md GraphQL workflow).

- [ ] **Step 3: Implement controller**

```java
package com.bytechef.ee.automation.contextstore.web.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

@Controller
public class ContextStoreSourceGraphQlController {

    // Post-DataStream-pivot: routes through the facade for workflow auto-gen on create
    // and JobService-create on refresh.
    private final WorkspaceContextStoreSourceFacade sourceFacade;
    private final ContextStoreSourceService sourceService;        // still used for read paths
    private final ContextStoreEntityService entityService;
    // private final JobService jobService; // for refresh — see Phase 5 Task 14

    public ContextStoreSourceGraphQlController(/* ... */) { /* ... */ }

    @QueryMapping
    public ContextStoreSource contextStoreSource(@Argument Long id) {
        return sourceService.get(id);
    }

    @MutationMapping
    public ContextStoreSource createContextStoreSource(@Argument CreateContextStoreSourceInput input) {
        // map input → ContextStoreSource, .create(), then create entities
    }

    @MutationMapping
    public ContextStoreSource updateContextStoreSource(@Argument Long id, @Argument UpdateContextStoreSourceInput input) {
        // ...
    }

    @MutationMapping
    public Boolean deleteContextStoreSource(@Argument Long id) {
        sourceService.delete(id);

        return true;
    }

    @MutationMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public JobExecution refreshContextStoreSource(@Argument Long id) {
        // Post-DataStream-pivot: facade triggers a manual run of the source's auto-generated workflow,
        // returning the Atlas JobExecution row for status surfacing.
        return sourceFacade.refreshNow(id);
    }

    @MutationMapping
    public ContextStoreSource setContextStoreSourceEnabled(@Argument Long id, @Argument Boolean enabled) {
        sourceService.setEnabled(id, enabled);

        return sourceService.get(id);
    }

    @SchemaMapping(typeName = "Workspace")
    public List<ContextStoreSource> contextStoreSources(Workspace workspace, @Argument ContextStoreSourceFilter filter) {
        return sourceService.getAllByWorkspaceId(workspace.getId());
    }

    @SchemaMapping
    public List<ContextStoreEntity> entities(ContextStoreSource source) {
        return entityService.getAllBySourceId(source.getId());
    }

    @SchemaMapping
    public List<JobExecution> recentSyncRuns(ContextStoreSource source, @Argument int limit) {
        // Post-DataStream-pivot: query Atlas's JobExecution rows for source.workflowId
        return sourceFacade.getRecentSyncRuns(source.getId(), limit);
    }
}
```

(Pattern from `ProjectGraphQlController.java:44–120`.)

- [ ] **Step 4: Add GraphQL IntTest**

Pattern from `McpProjectGraphQlControllerIntTest.java:45–99`:

```java
@ContextConfiguration(classes = {
    ContextStoreGraphQlTestConfiguration.class,
    ContextStoreSourceGraphQlController.class
})
@GraphQlTest(controllers = ContextStoreSourceGraphQlController.class, /* ... */)
class ContextStoreSourceGraphQlControllerIntTest {

    @Autowired private GraphQlTester graphQlTester;

    @Test void testGetContextStoreSourceById() { ... }
    @Test void testCreateContextStoreSourceMutation() { ... }
    @Test void testRefreshRequiresAdmin() { ... }
}
```

- [ ] **Step 5: Run client codegen + commit**

```bash
cd client && npx graphql-codegen
cd ..
git add server/ee/libs/automation/automation-context-store/automation-context-store-graphql/ \
        client/codegen.ts \
        client/src/shared/middleware/graphql.ts
git commit -m "Add Context Store GraphQL surface (queries, mutations, refresh auth)

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Phase 10: EE tool surface

### Task 22: ContextStoreToolFacade interface + impl

**Files:**
- Create: `server/ee/libs/platform/platform-context-store/platform-context-store-api/.../tool/ContextStoreToolFacade.java`
- Create: `server/ee/libs/platform/platform-context-store/platform-context-store-service/.../tool/ContextStoreToolFacadeImpl.java`
- Create: corresponding test

- [ ] **Step 1: Interface**

```java
package com.bytechef.ee.automation.contextstore.tool;

import java.util.List;
import org.springframework.ai.tool.ToolCallback;

/**
 * @version ee
 */
public interface ContextStoreToolFacade {

    /**
     * Mints one FunctionToolCallback per (ContextStoreSource × ContextStoreEntity) for the given workspace.
     * Names follow CONTEXT_STORE_<source>_<entity>_SEARCH convention.
     */
    List<ToolCallback> getFunctionToolCallbacks(Long workspaceId);

    /**
     * Mints a single per-(source, entity) callback. Used by McpServer enumeration when only one entity is bound.
     */
    ToolCallback getFunctionToolCallback(Long sourceId, String entityName);
}
```

- [ ] **Step 2: Impl extends `AbstractToolFacade`**

```java
package com.bytechef.ee.automation.contextstore.tool;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreEntity;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreQuery;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreQueryFilter;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreQuerySort;
import com.bytechef.ee.platform.contextstore.service.ContextStoreEntityService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreQueryService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSourceService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.ai.tool.facade.AbstractToolFacade;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;

/**
 * @version ee
 */
@Component
public class ContextStoreToolFacadeImpl extends AbstractToolFacade implements ContextStoreToolFacade {

    private final ContextStoreSourceService sourceService;
    private final ContextStoreEntityService entityService;
    private final ContextStoreQueryService queryService;

    public ContextStoreToolFacadeImpl(ContextStoreSourceService sourceService,
                                      ContextStoreEntityService entityService,
                                      ContextStoreQueryService queryService,
                                      Evaluator evaluator) {
        super(evaluator);
        this.sourceService = sourceService;
        this.entityService = entityService;
        this.queryService = queryService;
    }

    @Override
    public List<ToolCallback> getFunctionToolCallbacks(Long workspaceId) {
        List<ToolCallback> callbacks = new ArrayList<>();

        for (ContextStoreSource source : sourceService.getAllEnabledByWorkspaceId(workspaceId)) {
            for (ContextStoreEntity entity : entityService.getAllBySourceId(source.getId())) {
                callbacks.add(buildCallback(source, entity));
            }
        }

        return callbacks;
    }

    @Override
    public ToolCallback getFunctionToolCallback(Long sourceId, String entityName) {
        ContextStoreSource src = sourceService.get(sourceId);
        ContextStoreEntity entity = entityService.fetchBySourceIdAndEntityName(sourceId, entityName)
            .orElseThrow();

        return buildCallback(src, entity);
    }

    private ToolCallback buildCallback(ContextStoreSource source, ContextStoreEntity entity) {
        String name = String.format("CONTEXT_STORE_%s_%s_SEARCH",
            source.getSourceComponentName().toUpperCase(),
            entity.getEntityName().toUpperCase());

        String description = "Search %s entities from %s by structured filters."
            .formatted(entity.getEntityName(), source.getName());

        String inputSchema = generateSchemaFromIndexedFields(entity);   // typed JSON Schema

        return FunctionToolCallback
            .builder(name, (Map<String, Object> args) -> executeSearch(source, entity, args))
            .inputType(Map.class)
            .description(description)
            .inputSchema(inputSchema)
            .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> executeSearch(ContextStoreSource source, ContextStoreEntity entity, Map<String, Object> args) {
        List<ContextStoreQueryFilter> filters = ((List<Map<String, Object>>) args.getOrDefault("filters", List.of()))
            .stream()
            .map(m -> new ContextStoreQueryFilter(
                (String) m.get("field"),
                ContextStoreQueryFilter.FilterOp.valueOf((String) m.get("op")),
                m.get("value")))
            .toList();

        List<ContextStoreQuerySort> sort = ((List<Map<String, Object>>) args.getOrDefault("sort", List.of()))
            .stream()
            .map(m -> new ContextStoreQuerySort(
                (String) m.get("field"),
                ContextStoreQuerySort.SortDirection.valueOf((String) m.getOrDefault("dir", "ASC"))))
            .toList();

        int limit = ((Number) args.getOrDefault("limit", 50)).intValue();
        String cursor = (String) args.get("cursor");

        var result = queryService.search(new ContextStoreQuery(
            source.getWorkspaceId(), source.getId(), entity.getEntityName(),
            filters, sort, limit, cursor, false, null));

        return Map.of("items", result.items(), "nextCursor", result.nextCursor());
    }

    private String generateSchemaFromIndexedFields(ContextStoreEntity entity) {
        // Build a JSON Schema string from entity.indexedFields ([{name,type},...])
        // Spring AI consumes this as the FunctionToolCallback inputSchema
        // Return a minimal schema with `filters` as array, `limit`/`cursor`/`sort` as expected
        return /* ... */;
    }
}
```

- [ ] **Step 3: Unit test**

```java
class ContextStoreToolFacadeImplTest {
    @Test void testGetFunctionToolCallbacksMintsOnePerEntity() { /* mock services, verify count */ }
    @Test void testCallbackSearchDelegates() { /* invoke .call(...), verify ContextStoreQueryService called with correct args */ }
}
```

- [ ] **Step 4: Commit**

---

### Task 23: ContextStoreToolsComponentHandler — TOOLS cluster element extension

**Files:**
- Create: `server/ee/libs/modules/components/context-store/.../ContextStoreToolsComponentHandler.java`

> **Post-2026-05-09 platform pivot**: pre-pivot, this handler lived in the now-collapsed `automation-context-store-tool-service` module. Per spec §4 line 207 (Option B EE-only collapse) the tool modules folded into the main api/service. Since the synthetic component module is the natural home for `@AutoService(ComponentHandler.class)` registrations targeting the `contextStore` component, the wrapper handler now lives in `server/ee/libs/modules/components/context-store/` alongside the base `ContextStoreComponentHandler`.

- [ ] **Step 1: Implement using `AbstractComponentDefinitionWrapper`**

```java
package com.bytechef.ee.automation.contextstore.tool;

import static com.bytechef.component.definition.ComponentDsl.clusterElement;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.ee.component.contextstore.action.ContextStoreGetAction;
import com.bytechef.ee.component.contextstore.action.ContextStoreSearchAction;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.google.auto.service.AutoService;

/**
 * @version ee
 */
@AutoService(ComponentHandler.class)
public class ContextStoreToolsComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = new ContextStoreToolsDefinition(
        component("contextStore")
            .title("Context Store")
            .description("Search and fetch records from the workspace's replicated source data.")
            .icon("path:assets/context-store.svg")
            .actions(
                ContextStoreSearchAction.ACTION_DEFINITION,
                ContextStoreGetAction.ACTION_DEFINITION)
            .clusterElements(
                clusterElement("search")
                    .type(BaseToolFunction.TOOLS)
                    .properties(/* same as Action: search, but expecting fromAi() placeholders */),
                clusterElement("get")
                    .type(BaseToolFunction.TOOLS)
                    .properties(/* ... */))
            .version(1));

    @Override
    public ComponentDefinition getDefinition() { return COMPONENT_DEFINITION; }

    private static class ContextStoreToolsDefinition extends AbstractComponentDefinitionWrapper {
        ContextStoreToolsDefinition(ComponentDefinition d) { super(d); }
    }
}
```

(Pattern from `DataStreamComponentHandler.java:59–75` — `AbstractComponentDefinitionWrapper`.)

(**Important caveat**: ByteChef's `@AutoService` discovery may register both the CE and EE handlers; verify with implementer that the ServiceLoader chain picks the EE handler when present. If not, use a different mechanism — e.g., the EE handler annotated with `@ConditionalOnEEVersion` and the CE handler with `@ConditionalOnMissingBean`. Confirm by reading `ComponentRegistry`/`ComponentDefinitionRegistry` to understand how multiple handlers for the same component name are merged.)

- [ ] **Step 2: IntTest**

```java
class ContextStoreToolsComponentHandlerIntTest {
    @Test
    void testTOOLSClusterElementsRegistered() {
        // Resolve the contextStore component definition; assert it has TOOLS cluster elements named "search" and "get"
    }
}
```

- [ ] **Step 3: Commit**

---

### Task 24: McpServer enumeration — aggregate Context Store callbacks

**Files:**
- Modify: `server/libs/automation/automation-ai/automation-ai-mcp-server/.../config/AutomationMcpServerConfiguration.java`

- [ ] **Step 1: Read the file to find where tool callbacks are aggregated**

```bash
cat server/libs/automation/automation-ai/automation-ai-mcp-server/src/main/java/com/bytechef/automation/ai/mcp/server/config/AutomationMcpServerConfiguration.java
```

- [ ] **Step 2: Add a conditional-on-bean injection of `ContextStoreToolFacade`**

When the EE bean is present, append its callbacks to the MCP server's tool list. When absent (CE-only), skip.

```java
// In the bean that aggregates tool callbacks per McpServer:
ObjectProvider<ContextStoreToolFacade> contextStoreToolFacadeProvider;

// inside the per-McpServer aggregation:
List<ToolCallback> all = new ArrayList<>();
all.addAll(/* existing AutomationMcpToolFacade outputs */);
contextStoreToolFacadeProvider.ifAvailable(facade -> all.addAll(facade.getFunctionToolCallbacks(workspaceId)));
```

- [ ] **Step 3: IntTest with both EE-on and EE-off**

- [ ] **Step 4: Commit**

---

## Phase 11: AiHub EE callbacks

> **Implementation note (2026-05-08)**: tasks 25 + 26 below describe the original 3-callback scope (1 search + 2 discovery: Search/ListContextSources/GetContextStoreRecord). The owner's spec §3 + §16 mid-implementation pivot expanded the surface to **11 callbacks total** — the 3 read-side ones documented in tasks 25-26 PLUS 5 define-side callbacks (`Create/Update/Delete/Refresh/SetEnabledContextStoreSource…`) that delegate through `WorkspaceContextStoreSourceFacade` with admin-role authorization + chat-level user confirmation, AND 2 source-discovery callbacks (`ListAvailableSourceComponents…`, `DescribeSourceComponentEntities…`). All 11 shipped in commit `0c67bdf3b48` registered across both ASK (read-only) and BUILD (mutations) agents in `AiHubConfiguration`. The expanded surface is documented in spec §3 and §16's "CC chat surface gets full Context Store define tools (not just consume)" decision-log entry — not as separate task entries here, since they landed in a single commit alongside the read-side ones. Future plan revisions can either backfill task entries 25a-26b for the define+discovery callbacks or leave the implementation note as the single source of truth (preferred — the plan's role is forward-looking).

### Task 25: SearchContextStoreToolCallback

**Files:**
- Create: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/.../tool/SearchContextStoreToolCallback.java`
- Create: corresponding `*Test.java`
- Modify: `AiHubConfiguration.java` (or wherever `defaultToolCallbacks()` lives — see `AiHubConfiguration.java:68–71` per the patterns survey)

- [ ] **Step 1: Read the existing pattern**

```bash
cat server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/tool/DeleteKnowledgeBaseDocumentToolCallback.java
cat server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/config/AiHubConfiguration.java
```

- [ ] **Step 2: Implement**

```java
package com.bytechef.ee.automation.aihub.tool;

import com.bytechef.ee.platform.contextstore.dto.ContextStoreQuery;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreQueryFilter;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreSearchResult;
import com.bytechef.ee.platform.contextstore.service.ContextStoreQueryService;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolContext;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 */
public class SearchContextStoreToolCallback implements ToolCallback {

    private static final String TOOL_NAME = "search_context_store";
    private static final String DESCRIPTION = """
        Search the user's Context Store — the workspace's replicated source-system data —
        with structured filters. Use this instead of calling live source APIs (HubSpot, Salesforce, etc.)
        for faster, deterministic reads.""";

    private static final String INPUT_SCHEMA = """
        {
          "type": "object",
          "properties": {
            "sourceId": {"type": "integer", "description": "Context Source ID (use list_context_sources to discover)"},
            "entity": {"type": "string"},
            "filters": {"type": "array", "items": {"type": "object"}},
            "sort": {"type": "array", "items": {"type": "object"}},
            "limit": {"type": "integer", "default": 50},
            "cursor": {"type": "string"}
          },
          "required": ["sourceId", "entity"]
        }
        """;

    private final ContextStoreQueryService queryService;
    private final ObjectMapper objectMapper;

    public SearchContextStoreToolCallback(ContextStoreQueryService queryService, ObjectMapper objectMapper) {
        this.queryService = queryService;
        this.objectMapper = objectMapper;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name(TOOL_NAME)
            .description(DESCRIPTION)
            .inputSchema(INPUT_SCHEMA)
            .build();
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        try {
            SearchInput input = objectMapper.readValue(toolInput, SearchInput.class);
            Long workspaceId = (Long) toolContext.getContext().get("workspaceId");

            // build ContextStoreQuery from input...
            ContextStoreSearchResult result = queryService.search(/* ... */);

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    record SearchInput(Long sourceId, String entity, java.util.List<ContextStoreQueryFilter> filters,
                       Integer limit, String cursor) {}
}
```

- [ ] **Step 3: Test**

```java
class SearchContextStoreToolCallbackTest {
    @Test void testCallReturnsSearchResultJson() { /* mock query service */ }
    @Test void testCallHandlesInvalidInput() { /* malformed JSON → error response */ }
}
```

- [ ] **Step 4: Register in `AiHubConfiguration` (or whichever config aggregates default tool callbacks)**

Add `new SearchContextStoreToolCallback(queryService, objectMapper)` to the list returned by `defaultToolCallbacks()`.

- [ ] **Step 5: Commit**

---

### Task 26: ListContextSourcesToolCallback + GetContextStoreRecordToolCallback

**Files:**
- Create: `tool/ListContextSourcesToolCallback.java`, `tool/GetContextStoreRecordToolCallback.java` + tests
- Modify: `AiHubConfiguration.java` (add both)

- [ ] **Step 1: Implement `ListContextSourcesToolCallback`**

```java
public class ListContextSourcesToolCallback implements ToolCallback {

    private static final String TOOL_NAME = "list_context_sources";
    private static final String DESCRIPTION = """
        List the user's available Context Sources and their entities.
        Returns id, name, component, status, and indexedFields per entity.
        Call this first to discover what's available before search_context_store.""";

    private static final String INPUT_SCHEMA = "{\"type\": \"object\", \"properties\": {}}";

    private final ContextStoreSourceService sourceService;
    private final ContextStoreEntityService entityService;
    private final ObjectMapper objectMapper;

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        Long workspaceId = (Long) toolContext.getContext().get("workspaceId");
        List<ContextStoreSource> sources = sourceService.getAllEnabledByWorkspaceId(workspaceId);

        // Build a List<Map<String, Object>> with source + entities, then serialize
        return objectMapper.writeValueAsString(payload);
    }
}
```

- [ ] **Step 2: Implement `GetContextStoreRecordToolCallback`**

```java
public class GetContextStoreRecordToolCallback implements ToolCallback {
    // input: {sourceId, entity, sourceRecordId}
    // output: full payload or null
}
```

- [ ] **Step 3: Tests for both**

- [ ] **Step 4: Register both in `AiHubConfiguration`**

- [ ] **Step 5: Commit**

---

## Phase 12: End-to-end & polish

### Task 27: Full E2E `ContextStoreE2EIntTest`

**Files:**
- Create: `platform-context-store-service/src/test/.../ContextStoreE2EIntTest.java`

- [ ] **Step 1: Test scenario**

1. Create a workspace
2. Create a Context Source pointing at a fake test-only `ItemReader` cluster element
3. Add a `contacts` entity with `idField=id`, `indexedFields=[{name:"company.name",type:"TEXT"}]`
4. Trigger `runSync` → wait → assert `status=READY`
5. Query via `ContextStoreQueryService.search` with filter `company.name = "Acme"` → assert correct subset
6. Modify a record in the fake reader → re-sync → assert payload updated
7. Remove a record from the fake reader → re-sync → assert tombstoned (`deleted_at` set)

- [ ] **Step 2: Run, debug, commit**

---

### Task 28: `./gradlew check` + `npm run check` final pass

- [ ] **Step 1: Server-side**

```bash
./gradlew spotlessApply
./gradlew :server:ee:libs:platform:platform-context-store:check
./gradlew :server:ee:libs:modules:components:context-store:check
./gradlew :server:ee:libs:automation:automation-context-store:check
```

- [ ] **Step 2: Client-side (after GraphQL codegen)**

```bash
cd client
npm run check
```

- [ ] **Step 3: Commit any spotless / formatting / generated-file changes**

```bash
git add -p   # interactively review and stage only relevant changes
git commit -m "Fix formatting and codegen drift

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Phase 13: Knowledge Base Source — sync into existing KB documents (in MVP)

Per spec §12. Adds workspace-configurable periodic sync of document-shaped content (Notion / Confluence / Google Docs / etc.) into the existing Knowledge Base. Per the post-2026-05-09 KB-to-platform move (commit `5cee82ab933`), KB-Source rides on the same platform-vs-automation split as KB itself: the `KnowledgeBaseSource` entity + repo + service + sync listener + sync helpers on `KnowledgeBaseDocumentService` live on `platform-knowledge-base-{api,service}`; the `WorkspaceKnowledgeBaseSource` relation entity + workspace-aware `WorkspaceKnowledgeBaseSourceFacade` live on `automation-knowledge-base-{api,service}`; the GraphQL controller stays in `automation-knowledge-base-graphql`. The relationship between "synced KB doc" and "source record" is 1:1 and expressed inline as five nullable columns on the existing `knowledge_base_document` table (now a platform table) plus one new `knowledge_base_source` table (also platform-side, no `workspace_id`) plus the relation table `workspace_knowledge_base_source` (automation-side).

> **2026-05-08 redesign pivot** (after Tasks 29-32 of the prior 8-task plan landed): the original "Knowledge Sync as parallel primitive" framing — separate `automation-knowledge-sync-{api,service,graphql}` modules, separate `KnowledgeSyncSource`/`KnowledgeSyncEntity` entities, separate Liquibase tables, separate listener — is replaced by an inline-columns approach. Phase 13 shrinks from 8 tasks to 5. Existing commits `3c8b1dce19d` (scaffold KS modules), `f78e47cc297` (KS domain entities), `52d8f0b93a0` (KS Liquibase + KB external_key column), `40b6fb2e14f` (KS repos + services), and `9b46349ef68` (KB externalKey/upsertBySourceKey) are reverted before the new tasks run. The redo is a clean slate: `KnowledgeBaseSource` plus five new nullable columns (`source_id`, `source_record_id`, `synced_payload_hash`, `last_seen_at`, `deleted_at`) on the existing `knowledge_base_document`.

> **2026-05-09 KB-to-platform move (commit `5cee82ab933`)**: KB itself relocated from automation to platform. KB-Source paths below reflect the post-move layout — `platform-knowledge-base-{api,service}` for entity/repos/services/listener/sync helpers, `automation-knowledge-base-{api,service}` for the workspace-relation + facade. Both sides use Apache 2.0 license headers (KB is CE on both sides). Java packages: `com.bytechef.platform.knowledgebase.*` for platform-side classes, `com.bytechef.automation.knowledgebase.*` for the workspace-relation + facade classes (and `com.bytechef.automation.knowledgebase.web.graphql.*` for the controller).

### Task 29: KnowledgeBaseSource + WorkspaceKnowledgeBaseSource entities + enums + Liquibase migrations

**Files (in `platform-knowledge-base-api`):**
- `server/libs/platform/platform-knowledge-base/platform-knowledge-base-api/src/main/java/com/bytechef/platform/knowledgebase/domain/`
  - Create `KnowledgeBaseSource.java` — mirrors `ContextStoreSource` shape post-platform-pivot: `id`, **NO `workspace_id` column** (workspace flows through the relation table in automation), `name`, `sourceComponentName`, `sourceComponentVersion`, `readerStrategy` int ordinal, `sourceClusterElementName?`, `sourceListActionName?`, `connectionId AggregateReference<Connection,Long>?`, `cadence`, `status` int ordinal, `enabled` default true, `lastSyncRunAt?`, `lastSyncJobExecutionId?`, `workflowId String?`, audit, `@Version`, plus `knowledgeBaseId AggregateReference<KnowledgeBase,Long>` non-null. Apache 2.0 license header.
  - Create `KnowledgeBaseSourceStatus.java` — enum with `BUILDING_PREVIEW(0), PREVIEW(1), READY(2), FAILED(3), DISABLED(4)`. Same shape as `ContextStoreSourceStatus`, separate enum class to keep KB and CS independently evolvable.
  - Create `ReaderStrategy.java` — enum with `CLUSTER_ELEMENT(0), LIST_ACTION(1)`. Mirrors CS `ReaderStrategy` exactly. Separate class so platform-KB module doesn't import EE-side CS code.
  - Modify `KnowledgeBaseDocument.java` — add five nullable fields + getters/setters: `@Column("source_id") @Nullable AggregateReference<KnowledgeBaseSource,Long> sourceId`, `@Column("source_record_id") @Nullable String sourceRecordId`, `@Column("synced_payload_hash") @Nullable String syncedPayloadHash`, `@Column("last_seen_at") @Nullable Instant lastSeenAt`, `@Column("deleted_at") @Nullable Instant deletedAt`. Update `toString()` to include these.

**Files (in `automation-knowledge-base-api`):**
- `server/libs/automation/automation-knowledge-base/automation-knowledge-base-api/src/main/java/com/bytechef/automation/knowledgebase/domain/`
  - Create `WorkspaceKnowledgeBaseSource.java` — relation entity mirroring the existing `WorkspaceKnowledgeBase` shape: `id` BIGSERIAL, `workspaceId` BIGINT not null, `knowledgeBaseSourceId` BIGINT not null (use `AggregateReference<KnowledgeBaseSource, Long>` if convenient — the platform `KnowledgeBaseSource` class is on the api module's classpath via the platform-KB-api dep), audit fields, `@Version`. Apache 2.0 license header.

**Files (Liquibase — platform side):**
- `server/libs/platform/platform-knowledge-base/platform-knowledge-base-service/src/main/resources/config/liquibase/changelog/platform/knowledge_base/`
  - Create `2026XXXXXXXXXX_platform_knowledge_base_source_init.xml` — pick the next-available ID after `20260429000001_platform_knowledge_base_document_tag_name.xml`. Create:
    - `knowledge_base_source` table — column list mirrors `context_store_source` post-platform-pivot (BIGINT autoIncrement startWith 1050; **NO `workspace_id` column**; `name VARCHAR(256) NOT NULL`; `source_component_name VARCHAR(256) NOT NULL`; `source_component_version INT NOT NULL`; `reader_strategy INT NOT NULL`; `source_cluster_element_name VARCHAR(256)`; `source_list_action_name VARCHAR(256)`; `connection_id BIGINT`; `cadence VARCHAR(64) NOT NULL`; `status INT NOT NULL`; `enabled BOOLEAN NOT NULL DEFAULT TRUE`; `last_sync_run_at TIMESTAMP WITH TIME ZONE`; `last_sync_job_execution_id BIGINT`; `workflow_id VARCHAR(256)`; audit columns `TIMESTAMP WITH TIME ZONE`; `version BIGINT NOT NULL`) PLUS `knowledge_base_id BIGINT NOT NULL`.
    - UNIQUE `(name)` constraint (no `workspace_id` to compose with — workspace scoping flows through the relation table).
    - FK `knowledge_base_source.knowledge_base_id → knowledge_base.id ON DELETE CASCADE`.
    - Index `idx_kb_source_knowledge_base (knowledge_base_id)` for "list sources targeting this KB".
    - `addColumn` to existing `knowledge_base_document` table (now a platform table) for the five new nullable columns: `source_id BIGINT`, `source_record_id VARCHAR(512)`, `synced_payload_hash VARCHAR(16)`, `last_seen_at TIMESTAMP WITH TIME ZONE`, `deleted_at TIMESTAMP WITH TIME ZONE`.
    - FK `knowledge_base_document.source_id → knowledge_base_source.id ON DELETE SET NULL` (deleting a source orphans its docs but doesn't lose them).
    - Partial UNIQUE index `uk_kb_doc_source_record (source_id, source_record_id) WHERE source_id IS NOT NULL` — manual uploads keep `source_id = NULL` and don't participate.
    - Index `idx_kb_doc_source (source_id)` for "list synced docs for this source" queries.
    - Index `idx_kb_doc_deleted_at (deleted_at) WHERE deleted_at IS NOT NULL` — filters tombstoned docs cheaply (mirrors CS `context_store_record.deleted_at`).

**Files (Liquibase — automation side):**
- `server/libs/automation/automation-knowledge-base/automation-knowledge-base-service/src/main/resources/config/liquibase/changelog/automation/knowledge_base/`
  - Create `2026XXXXXXXXXX_automation_workspace_knowledge_base_source_init.xml` — pick the next-available ID. Create the `workspace_knowledge_base_source` table mirroring the existing `workspace_knowledge_base` shape (id BIGSERIAL startWith 1050, workspace_id BIGINT NOT NULL, knowledge_base_source_id BIGINT NOT NULL, audit, version), UNIQUE `(workspace_id, knowledge_base_source_id)`, FK `knowledge_base_source_id → knowledge_base_source.id ON DELETE CASCADE`. No FK on `workspace_id` (mirroring `workspace_knowledge_base` precedent).

**Files (tests):**
- `server/libs/platform/platform-knowledge-base/platform-knowledge-base-service/src/test/java/com/bytechef/platform/knowledgebase/util/EnumOrdinalStabilityTest.java` — create or extend the existing one to pin `KnowledgeBaseSourceStatus` and `ReaderStrategy` ordinals.

**No master.xml change needed** — the platform-KB module's `platform/knowledge_base/` includeAll and the automation-KB module's `automation/knowledge_base/` includeAll pick the new changesets up automatically.

Run spotless + check + the new ordinal tests + a smoke KB IntTest on both modules to verify the migrations apply cleanly on Testcontainers Postgres:

```bash
./gradlew :server:libs:platform:platform-knowledge-base:platform-knowledge-base-api:check
./gradlew :server:libs:platform:platform-knowledge-base:platform-knowledge-base-service:check
./gradlew :server:libs:automation:automation-knowledge-base:automation-knowledge-base-api:check
./gradlew :server:libs:platform:platform-knowledge-base:platform-knowledge-base-service:integrationTest --tests 'KnowledgeBaseServiceIntTest'
```

Single commit:

```
4855 KB - Add KnowledgeBaseSource entity + sync metadata columns on knowledge_base_document

KnowledgeBaseSource is a new platform-knowledge-base entity mirroring
ContextStoreSource post-2026-05-09 platform pivot (no workspace_id column;
workspace flows through workspace_knowledge_base_source relation), plus a
non-null knowledge_base_id targeting the existing knowledge_base table.
KnowledgeBaseSourceStatus + ReaderStrategy are new parallel enums duplicated
from CS to keep the modules independently evolvable. WorkspaceKnowledgeBaseSource
relation entity lives in automation-knowledge-base-api alongside the existing
WorkspaceKnowledgeBase, mirroring its shape.

knowledge_base_document (now a platform table per commit 5cee82ab933) gains
five nullable columns: source_id (FK to knowledge_base_source ON DELETE SET NULL),
source_record_id, synced_payload_hash, last_seen_at, deleted_at. Manual uploads
keep all five NULL; synced docs have them populated. Partial UNIQUE INDEX on
(source_id, source_record_id) WHERE source_id IS NOT NULL enforces idempotent
upsert without affecting manual docs.

Liquibase: knowledge_base_source table + 5 sync columns added by a new platform
changeset; workspace_knowledge_base_source relation table added by a companion
automation changeset.

EnumOrdinalStabilityTest pins both new enums.

Plan reference: Phase 13 Task 29 (post-2026-05-09 KB-to-platform layout).
```

### Task 30: Repositories + KnowledgeBaseSourceService + WorkspaceKnowledgeBaseSourceService + KB document sync helpers

**Files (in `platform-knowledge-base-api`):**
- Create `repository/KnowledgeBaseSourceRepository.java` (package `com.bytechef.platform.knowledgebase.repository`) — `extends PagingAndSortingRepository<KnowledgeBaseSource, Long>, ListCrudRepository<KnowledgeBaseSource, Long>`. Methods: `findAllByEnabled(boolean)`, `findAllByKnowledgeBaseId(Long)`. **NO `findAllByWorkspaceId`** — that's the workspace-relation service's responsibility (in automation).
- Modify `repository/KnowledgeBaseDocumentRepository.java` — add `Optional<KnowledgeBaseDocument> findBySourceIdAndSourceRecordId(Long sourceId, String sourceRecordId);` and a `@Modifying @Query` derived method for `tombstoneUnseen(Long sourceId, Set<String> seenSourceRecordIds, Instant now)` returning `int` (count tombstoned). The `@Modifying` is required for the JDBC string `@Query DELETE/UPDATE` (per the lesson in MEMORY.md from the workspace-relation refactor).
- Create `service/KnowledgeBaseSourceService.java` (package `com.bytechef.platform.knowledgebase.service`) — interface; CRUD by id only (no `workspaceId` parameter on any method): `create`, `update`, `delete`, `get(Long id)`, `fetch(Long id)`, `findAllActive()` (called by the listener and not workspace-scoped), `findAllByKnowledgeBaseId(Long)`, `updateStatus(Long id, KnowledgeBaseSourceStatus status, @Nullable Instant lastSyncRunAt, @Nullable Long jobExecutionId)`, `updateLastSyncMetadata(Long id, @Nullable Instant lastSyncRunAt, @Nullable Long jobExecutionId)` (used by the PARTIAL-mode listener branch — same shape as the new CS helper added in Task 32a), `setEnabled(Long id, boolean enabled)`, `setWorkflowId(Long id, String workflowId)`. Mirrors `ContextStoreSourceService` post-2026-05-09 platform pivot.

**Files (in `platform-knowledge-base-service`):**
- Create `service/KnowledgeBaseSourceServiceImpl.java` (package `com.bytechef.platform.knowledgebase.service`) — `@Service`, `@SuppressFBWarnings("EI")`, `@Transactional` on mutating methods, `Validate.notNull` for required params. Mirror `ContextStoreSourceServiceImpl` patterns exactly.
- Modify `service/KnowledgeBaseDocumentServiceImpl.java` (in package `com.bytechef.platform.knowledgebase.service`) — add two **package-private** helper methods used by the writer in Task 31:
  - `KnowledgeBaseDocument createSyncedDocument(long kbId, long sourceId, String sourceRecordId, String name, String text, Map<String, ?> metadata, String payloadHash, Instant now)` — persists `text` to platform `KnowledgeBaseFileStorage` as `<sourceRecordId>.md`, builds a new `KnowledgeBaseDocument` with kbId + sourceId + sourceRecordId + name + FileEntry + metadata-as-tags + syncedPayloadHash + lastSeenAt = now + status STATUS_UPLOADED, saves, publishes platform `KnowledgeBaseDocumentEvent(documentId)` to kick the existing chunker/embedder.
  - `KnowledgeBaseDocument replaceSyncedDocument(long documentId, String name, String text, Map<String, ?> metadata, String payloadHash, Instant now)` — replaces document content. **This is NOT a thin wrapper** — the existing KB service has no public replace path (only `upload(...)` and `delete(...)`). Real surface area to handle, with the post-platform-pivot KB layout in mind (file-storage swap now happens via the platform `KnowledgeBaseFileStorage` interface):
    - **File-storage swap**: write the new text to platform file storage as a new `FileEntry`, then swap the document's `document` field to point at the new entry. The old `FileEntry` should be cleaned up — either delete it eagerly (preferred; matches the manual-upload replace flow if one exists in `KnowledgeBaseFileStorageImpl`, or borrow that pattern) OR leave a follow-up task for orphan cleanup if eager delete creates ordering issues with the chunker.
    - **Chunk ordering**: existing chunks in `knowledge_base_document_chunk` (FK to this document; both tables now in platform-KB) must be cleared before the chunker re-runs against the new content. If platform `KnowledgeBaseDocumentEvent` already triggers chunk-replacement (rather than chunk-append) in `platform-knowledge-base-worker`, use that. Otherwise add a step to delete old chunks before publishing the event. Verify by reading the existing chunker's event handler in `platform-knowledge-base-worker`.
    - **Transactional ordering**: file-storage write happens outside the DB transaction (it's an external system — the platform `KnowledgeBaseFileStorage` interface dispatches to JDBC blob / S3 / FS depending on config). If the DB save fails after the file write succeeds, the new file is orphaned. Make `replaceSyncedDocument` resilient: write file → save DB row inside transaction → if save fails, delete file. OR: save the new FileEntry pointer first via a transactional outbox pattern. MVP can use the simpler "write then save then cleanup-on-failure" pattern.
    - **Idempotency on retry**: if the same `(sourceId, sourceRecordId, payloadHash)` arrives twice (Spring Batch retry), the second call should be a no-op fast path. The writer-level fast path (Task 31 — unchanged hash → bump `last_seen_at` only) catches most retries upstream, but `replaceSyncedDocument` should also handle "this hash already matches — skip" defensively.
    - **Status transitions**: setting `STATUS_UPLOADED` re-triggers the chunker. Verify that the existing chunker-driven status flow (`STATUS_UPLOADED` → `STATUS_PROCESSING` → `STATUS_READY`) tolerates a doc bouncing back to `STATUS_UPLOADED` while it was already in `STATUS_PROCESSING` — particularly whether a partial chunk-set is safely cleared. The chunker now runs in `platform-knowledge-base-worker`'s `KnowledgeBaseDocumentProcessWorker` — verify behavior there.
    - Test cases: the four scenarios above each get a focused IntTest. If any of them surface non-trivial behavior, factor out the file-swap/chunk-replacement logic into a private helper that the test can target directly.
  - Both methods stay package-private to the platform-KB-service module (no need to expose on the public interface unless Phase 14 semantic add-on needs them; defer until then).
- The existing `delete(long id)` on `KnowledgeBaseDocumentService` already covers the "purge a tombstoned doc" path; no change needed.
- Create `listener/KnowledgeBaseSourceSyncJobListener.java` — see Task 32 (listener placement is platform-side).

**Files (in `automation-knowledge-base-api`):**
- Create `repository/WorkspaceKnowledgeBaseSourceRepository.java` (package `com.bytechef.automation.knowledgebase.repository`) — `extends PagingAndSortingRepository<WorkspaceKnowledgeBaseSource, Long>, ListCrudRepository<WorkspaceKnowledgeBaseSource, Long>`. Methods: `findAllByWorkspaceId(Long)`, `findByKnowledgeBaseSourceId(Long)`, `existsByWorkspaceIdAndKnowledgeBaseSourceId(Long, Long)`, `deleteByKnowledgeBaseSourceId(Long)`. Mirror `WorkspaceKnowledgeBaseRepository` shape.
- Create `service/WorkspaceKnowledgeBaseSourceService.java` (package `com.bytechef.automation.knowledgebase.service`) — interface for the workspace-relation; methods: `getAllByWorkspaceId(Long workspaceId)` (joins through to platform sources to return `List<KnowledgeBaseSource>`), `getAllEnabledByWorkspaceId(Long workspaceId)`, `fetchWorkspaceIdByKnowledgeBaseSourceId(Long sourceId)` returning `Optional<Long>` (used by the GraphQL controller to resolve `workspaceId` for source-id-only mutation inputs), `addWorkspaceKnowledgeBaseSource(Long workspaceId, Long sourceId)`, `removeWorkspaceKnowledgeBaseSource(Long workspaceId, Long sourceId)`. Mirror `WorkspaceKnowledgeBaseService`.

**Files (in `automation-knowledge-base-service`):**
- Create `service/WorkspaceKnowledgeBaseSourceServiceImpl.java` (package `com.bytechef.automation.knowledgebase.service`) — `@Service`. Uses `WorkspaceKnowledgeBaseSourceRepository` for the relation queries + platform `KnowledgeBaseSourceService` to fetch the actual source rows by id when joining. Mirror `WorkspaceKnowledgeBaseServiceImpl` shape.
- The facade impl (`WorkspaceKnowledgeBaseSourceFacadeImpl`) lives in this module too but is implemented in Task 32 alongside the listener.

**Files (tests):**
- `platform-knowledge-base-service/src/test/java/com/bytechef/platform/knowledgebase/service/KnowledgeBaseSourceServiceIntTest.java` — Testcontainers Postgres. Tests: createPersistsAllFields, updateBumpsVersion, updateStatusFlipsStatusAndTimestamps, updateLastSyncMetadataPreservesStatus, setEnabledTogglesFlag, findAllByKnowledgeBaseId, findAllActive.
- `platform-knowledge-base-service/src/test/java/com/bytechef/platform/knowledgebase/service/KnowledgeBaseDocumentServiceIntTest.java` — extend with: testCreateSyncedDocumentPersistsAllSyncFieldsAndPublishesEvent, testReplaceSyncedDocumentClearsDeletedAtAndBumpsHash, testTombstoneUnseenSetsDeletedAtForAbsentRecords, testTombstoneUnseenIgnoresManualUploads (those have `source_id = NULL`).
- `automation-knowledge-base-service/src/test/java/com/bytechef/automation/knowledgebase/service/WorkspaceKnowledgeBaseSourceServiceIntTest.java` — Testcontainers Postgres. Tests: addRelationCreatesRow, removeRelationDeletesRow, getAllByWorkspaceIdJoinsThroughToPlatformSources, fetchWorkspaceIdByKnowledgeBaseSourceIdReturnsCorrectWorkspace.

Run spotless + check + IntTests on both modules:

```bash
./gradlew :server:libs:platform:platform-knowledge-base:platform-knowledge-base-service:check
./gradlew :server:libs:platform:platform-knowledge-base:platform-knowledge-base-service:integrationTest
./gradlew :server:libs:automation:automation-knowledge-base:automation-knowledge-base-service:check
./gradlew :server:libs:automation:automation-knowledge-base:automation-knowledge-base-service:integrationTest
```

Single commit:

```
4855 KB - Add KnowledgeBaseSourceService + sync helpers on document service

KnowledgeBaseSourceRepository + Service live in platform-knowledge-base-{api,service}
post-2026-05-09 KB-to-platform move; CRUD by id only (no workspaceId parameter,
mirroring the post-platform-pivot ContextStoreSourceService shape).

WorkspaceKnowledgeBaseSourceRepository + Service live in automation-knowledge-base-
{api,service}; carry the workspace-scoped read methods (getAllByWorkspaceId,
fetchWorkspaceIdByKnowledgeBaseSourceId) that join through to platform sources.

KnowledgeBaseDocumentService (in platform) gains two package-private sync helpers
used by Phase 13 Task 31's DESTINATION cluster element: createSyncedDocument writes
text to platform KnowledgeBaseFileStorage as <sourceRecordId>.md, persists with
all five sync columns populated, publishes platform KnowledgeBaseDocumentEvent so
the existing chunker pipeline (now in platform-knowledge-base-worker) runs untouched.
replaceSyncedDocument handles change-detection updates and tombstone reappearance
(clears deleted_at).

KnowledgeBaseDocumentRepository.tombstoneUnseen issues a single UPDATE setting
deleted_at = now WHERE source_id = ? AND source_record_id NOT IN (?) AND
deleted_at IS NULL. @Modifying annotation per the workspace-relation lesson.

Plan reference: Phase 13 Task 30 (post-2026-05-09 KB-to-platform layout).
```

### Task 31: KnowledgeBaseItemWriter (DESTINATION cluster element on knowledgeBase component)

**Files (in `server/libs/modules/components/ai/vectorstore/knowledgebase/`):**
- Create `src/main/java/com/bytechef/component/ai/vectorstore/knowledgebase/destination/KnowledgeBaseItemWriter.java`. Implements `ItemWriter`. Imports platform-KB types from `com.bytechef.platform.knowledgebase.*` (the new home for the entity + service interfaces post commit `5cee82ab933`). The component module's `build.gradle.kts` gains a dep on `:server:libs:platform:platform-knowledge-base:platform-knowledge-base-api` if it doesn't already have one transitively. Lifecycle:
  - `open(inputParameters, ...)`:
    - read `sourceId` from inputParameters; load the source via platform `KnowledgeBaseSourceService.get(sourceId)`; cache `kbId = source.getKnowledgeBaseId()`. **No workspace lookup needed** — workspace is implicit via the `workspace_knowledge_base_source` relation table; the writer doesn't care which workspace the source belongs to (same shape as `contextStore.writeToReplica` post-platform-pivot).
    - read `mode` from inputParameters (default `"FULL_REPLACE"`). Validate it's one of `"FULL_REPLACE"` / `"PARTIAL"` — throw `IllegalArgumentException` otherwise. Store on the writer instance for use during `update(...)`. Per spec §12, the writer's per-record logic is mode-independent; only the listener's post-job behavior changes. Storing the value here is purely so it can be flushed to `executionContext` for the listener.
    - Initialize `seenRecordIds = new HashSet<String>()`.
  - `write(records)`: for each record:
    1. `sourceRecordId = String.valueOf(record.get("id"))` — convention: readers produce records with a stable `id` field. Throw if missing.
    2. `payloadHash = PayloadHashUtil.hash(record)` — uses the shared CE platform util at `com.bytechef.commons.util.PayloadHashUtil` (relocated from `automation-context-store-api` to `commons-util` so both CS and KB-Source can share it without crossing the CE/EE boundary).
    3. `existing = documentRepository.findBySourceIdAndSourceRecordId(sourceId, sourceRecordId)` — repository is the platform `KnowledgeBaseDocumentRepository`.
    4. If `existing.isPresent()` and `existing.get().getSyncedPayloadHash().equals(payloadHash)` and `existing.get().getDeletedAt() == null`: bump `lastSeenAt = now` and save (unchanged-record fast path; no chunker re-run).
    5. Else if `existing.isPresent()`: call `documentService.replaceSyncedDocument(existing.get().getId(), name, text, record /* full record as metadata */, payloadHash, now)`.
    6. Else: call `documentService.createSyncedDocument(kbId, sourceId, sourceRecordId, name, text, record, payloadHash, now)`.
    7. `seenRecordIds.add(sourceRecordId)`.
  - `update(...)`:
    - `executionContext.put("knowledgeBaseSource.seenRecordIds", new ArrayList<>(seenRecordIds));`
    - `executionContext.put("knowledgeBaseSource.mode", mode);` — listener reads this back via `JobExecution → StepExecution → ExecutionContext` to choose tombstone behavior.
  - `close()`: no-op (tombstone sweep in the listener; mode-gated).
- Modify the existing `KnowledgeBaseComponentHandler.java` (`@AutoService(ComponentHandler.class)`, located at `src/main/java/com/bytechef/component/ai/vectorstore/knowledgebase/KnowledgeBaseComponentHandler.java`) to add the new cluster element:
  ```java
  ModifiableClusterElementDefinition<KnowledgeBaseItemWriter> writeAsDocumentClusterElement =
      ComponentDsl.<KnowledgeBaseItemWriter>clusterElement("writeAsDocument")
          .title("Write as Knowledge Base Document")
          .description("Upsert source records into the target Knowledge Base as documents. " +
              "FULL_REPLACE mode tombstones documents whose source_record_id is not seen in the sync run; " +
              "PARTIAL mode skips the tombstone sweep for backfills and partial-update workflows.")
          .type(DESTINATION)
          .object(() -> new KnowledgeBaseItemWriter(sourceService, documentService, documentRepository))
          .properties(
              integer("sourceId").label("Knowledge Base Source ID").required(true),
              string("mode")
                  .label("Sync mode")
                  .description("FULL_REPLACE = tombstone records not seen this run (default); " +
                      "PARTIAL = leave unseen records untouched (use for backfills / partial updates).")
                  .options(option("FULL_REPLACE", "FULL_REPLACE"), option("PARTIAL", "PARTIAL"))
                  .defaultValue("FULL_REPLACE")
                  .required(false));
  ```
  Follow the supplier-closure-not-singleton pattern from `ContextStoreComponentHandler` — the writer carries per-job state (`seenRecordIds`, cached `kbId`, `mode`) and is not safe to share across concurrent jobs.

**Files (tests):**
- `KnowledgeBaseItemWriterTest.java` — unit tests with mocked services (uses platform-KB-api types). Cover: createNewDocumentPath, unchangedRecordFastPath (just bumps lastSeenAt), changedRecordReplacePath, reappearedTombstoneClearsDeletedAt, missingIdFieldThrows, seenRecordIdsFlushedToExecutionContext on update(), modeFlushedToExecutionContextOnUpdate (FULL_REPLACE default + explicit PARTIAL), invalidModeRejectedAtOpen.

Run + commit:

```bash
./gradlew :server:libs:modules:components:ai:vectorstore:knowledgebase:check
```

```
4855 KB - Add knowledgeBase.writeAsDocument DESTINATION cluster element

DataStream DESTINATION cluster element on the existing knowledgeBase component
(server/libs/modules/components/ai/vectorstore/knowledgebase/). Open() reads
sourceId from inputParameters and resolves the source row via platform
KnowledgeBaseSourceService.get(sourceId) to get kbId -- no workspace lookup
(workspace is implicit via the workspace_knowledge_base_source relation table,
same shape as contextStore.writeToReplica post-platform-pivot).

Write() per record: hash payload, look up existing by (sourceId, sourceRecordId),
take the unchanged-record fast path if hash matches and deletedAt is null
(just bump lastSeenAt; no chunker re-run), otherwise replace or create via the
platform KnowledgeBaseDocumentService sync helpers from Task 30. Update() flushes
seen record-ids to executionContext for the listener's tombstone sweep.

Reuses commons-util's PayloadHashUtil for change detection (CE-side; same util
CS uses).

Per-job mutable state (seenRecordIds, cached kbId, mode) lives on the writer
instance, which is created via supplier closure on the component handler --
not a Spring singleton, mirroring the CS DESTINATION pattern.

Plan reference: Phase 13 Task 31 (post-2026-05-09 KB-to-platform layout).
```

### Task 32: WorkspaceKnowledgeBaseSourceFacade (workflow auto-gen) + JobExecutionListener (tombstone + status updates)

**Files (in `automation-knowledge-base-api`):**
- Create `facade/WorkspaceKnowledgeBaseSourceFacade.java` (interface, package `com.bytechef.automation.knowledgebase.facade`). All method signatures take `Long workspaceId` as the first parameter — mirrors `WorkspaceContextStoreSourceFacade` exactly:
  - `KnowledgeBaseSource create(Long workspaceId, CreateKnowledgeBaseSourceInput input);`
  - `KnowledgeBaseSource update(Long workspaceId, Long sourceId, UpdateKnowledgeBaseSourceInput input);`
  - `void delete(Long workspaceId, Long sourceId);`
  - `long refreshNow(Long workspaceId, Long sourceId);` // returns Atlas job id
  - `void setEnabled(Long workspaceId, Long sourceId, boolean enabled);`
- Create `dto/CreateKnowledgeBaseSourceInput.java` and `dto/UpdateKnowledgeBaseSourceInput.java` (records) — fields mirror the CS equivalents.

**Files (in `automation-knowledge-base-service`):**
- Create `facade/WorkspaceKnowledgeBaseSourceFacadeImpl.java` (`@Service`). Mirrors `WorkspaceContextStoreSourceFacadeImpl` exactly:
  - `create(workspaceId, input)`:
    1. Call platform `KnowledgeBaseSourceService.create(...)` to INSERT `knowledge_base_source` (status `BUILDING_PREVIEW`).
    2. Call `WorkspaceKnowledgeBaseSourceRepository.save(new WorkspaceKnowledgeBaseSource(workspaceId, sourceId))` to insert the relation row.
    3. Auto-generate workflow `[schedule.cronTrigger(cadence)] → [data-stream.stream(SOURCE=<sourceComponent>.<readerOrAction>, DESTINATION=knowledgeBase.writeAsDocument)]`. DESTINATION's parameters carry `{sourceId, mode: "FULL_REPLACE"}` — emit the `mode` value **explicitly** in the generated workflow definition (defensive: don't rely on the cluster element's default — if the default ever changes, auto-generated workflows must remain immune). Workflow `metadata.knowledgeBaseSourceId = sourceId`.
    4. Persist via `WorkflowService.createWorkflow(...)`. Call platform `KnowledgeBaseSourceService.setWorkflowId(sourceId, workflowId)`.
    5. Auto-create `ProjectDeploymentWorkflow` row tying the workflow to the workspace's auto-generated "private" project (same orchestration the CS facade does — reuse helpers if available, otherwise mirror).
    6. Trigger initial sync via `PrincipalJobFacade.createJob(workflowId, ...)`.
  - `update(workspaceId, sourceId, input)`: call platform `KnowledgeBaseSourceService.update(...)`; on cadence change, mutate the workflow's trigger cron parameter via `WorkflowService.update(...)` (preserve rest of the definition); on enabled flag change, toggle the `ProjectDeploymentWorkflow`. Verify `workspaceId` matches the source's workspace via `WorkspaceKnowledgeBaseSourceRepository.existsByWorkspaceIdAndKnowledgeBaseSourceId(...)`; throw `AccessDeniedException` if not.
  - `delete(workspaceId, sourceId)`: verify workspace ownership; delete `WorkspaceKnowledgeBaseSourceRepository.deleteByKnowledgeBaseSourceId(sourceId)`; cascade through workflow + project_deployment_workflow; call platform `KnowledgeBaseSourceService.delete(sourceId)` (FK ON DELETE SET NULL on `knowledge_base_document.source_id` orphans the docs but doesn't lose them).
  - `refreshNow(workspaceId, sourceId)`: verify workspace ownership; dispatch a manual job via `PrincipalJobFacade.createJob(workflowId, ...)`. Returns the Atlas job id. Admin-only authorization is enforced at the GraphQL controller layer (`@PreAuthorize("hasRole('ROLE_ADMIN')")`).
  - `setEnabled(workspaceId, sourceId, enabled)`: verify workspace ownership; call platform `KnowledgeBaseSourceService.setEnabled(sourceId, enabled)`; toggle the `ProjectDeploymentWorkflow` flag.
- Reuse the existing CS workflow generator if its API is generic enough (it lives in `automation-context-store-service` post-pivot — verify it's reachable from `automation-knowledge-base-service` via build dep, or duplicate the small workflow-generator code into a `KnowledgeBaseSourceWorkflowGenerator` in this module to avoid the cross-module dep). Implementer's choice based on what's easier to read in 6 months.

**Files (in `platform-knowledge-base-service`):**
- Create `listener/KnowledgeBaseSourceSyncJobListener.java` (`JobExecutionListener` implementation, `@Component`, package `com.bytechef.platform.knowledgebase.listener`). **Listener placement is platform-side** — mirrors `ContextStoreSyncJobListener`'s placement in `platform-context-store-service`. The listener operates by `source_id` only (no workspace lookup; the writer / facade have already established source ownership upstream).
  - `beforeJob(JobExecution)`: detect that the job's destination is `knowledgeBase.writeAsDocument`. If the source's status is `BUILDING_PREVIEW` and this is the first run, leave it; otherwise no-op before-side.
  - `afterJob(JobExecution)`:
    - Read `mode` from the destination's `inputParameters` (default `"FULL_REPLACE"` if absent — backward compat with workflows that predate this parameter).
    - Aggregate `seenRecordIds` from every `StepExecution.executionContext.get("knowledgeBaseSource.seenRecordIds")`.
    - On COMPLETED + `mode == FULL_REPLACE`: platform `documentRepository.tombstoneUnseen(sourceId, seenRecordIds, now)`. Then platform `sourceService.updateStatus(sourceId, READY, now, jobExecution.getId())`.
    - On COMPLETED + `mode == PARTIAL`: **skip** the tombstone sweep entirely. Update `lastSyncRunAt` + `lastSyncJobExecutionId` (the run did happen) but do NOT flip `status` — a `BUILDING_PREVIEW` source stays `BUILDING_PREVIEW` until a `FULL_REPLACE` proves the source is ready; a `READY` source stays `READY`. Use `sourceService.updateLastSyncMetadata(sourceId, now, jobExecution.getId())` (added to platform `KnowledgeBaseSourceService` in Task 30).
    - On FAILED: if the source was already READY, **don't** downgrade (transient sync failures don't break a working source — same invariant as CS). If still BUILDING_PREVIEW, flip to FAILED. No tombstone in either mode.
  - Listener acts only when destination is `knowledgeBase.writeAsDocument` — must NOT interfere with other DataStream jobs (especially the CS one with destination `contextStore.writeToReplica`). Detect via job parameter introspection (the workflow's `metadata.knowledgeBaseSourceId` or the destination cluster element name carried in DataStream's job parameters). The CS listener's signal-extraction shape is the canonical reference; mirror it exactly. This is also surfaced as open question 6 in the executor section since the precise JobParameter key shape is to-be-verified during Phase 5 Task 13.
- Create matching unit tests for facade + listener (mirror `WorkspaceContextStoreSourceFacadeImplTest` in automation-CS-service and `ContextStoreSyncJobListenerTest` in platform-CS-service). Listener tests must cover the `FULL_REPLACE` + `PARTIAL` paths separately:
  - `testAfterJobOnCompletedFullReplaceTombstonesUnseen` — default mode; tombstone sweep fires; status flips to READY.
  - `testAfterJobOnCompletedPartialSkipsTombstoneSweep` — mode = PARTIAL; no tombstone sweep; status unchanged; `lastSyncRunAt` + `lastSyncJobExecutionId` updated.
  - `testAfterJobOnCompletedPartialPreservesBuildingPreviewStatus` — pre-condition: source.status = BUILDING_PREVIEW. Mode = PARTIAL run completes. Assert: status stays BUILDING_PREVIEW (only FULL_REPLACE proves readiness).
  - `testAfterJobModeDefaultsToFullReplaceWhenAbsent` — destination params don't include `mode`; listener treats as FULL_REPLACE; tombstone sweep fires.

Run + commit:

```bash
./gradlew :server:libs:platform:platform-knowledge-base:platform-knowledge-base-service:check
./gradlew :server:libs:automation:automation-knowledge-base:automation-knowledge-base-api:check
./gradlew :server:libs:automation:automation-knowledge-base:automation-knowledge-base-service:check
```

```
4855 KB - Add WorkspaceKnowledgeBaseSourceFacade (workflow auto-gen) + sync job listener

WorkspaceKnowledgeBaseSourceFacade interface in automation-knowledge-base-api
+ Impl in automation-knowledge-base-service. All method signatures take
workspaceId as first parameter, mirroring WorkspaceContextStoreSourceFacade
post-2026-05-09 platform/automation split. The Impl uses platform
KnowledgeBaseSourceService for entity CRUD + WorkspaceKnowledgeBaseSourceRepository
(same module) for relation insert/delete + atlas-coordinator for workflow
auto-generation, ProjectDeploymentWorkflow lifecycle, and manual job dispatch.

create() calls platform KnowledgeBaseSourceService.create() to insert the
source row, inserts the workspace_knowledge_base_source relation row,
auto-generates the sync workflow [schedule.cronTrigger -> data-stream.stream(
SOURCE=<reader>, DESTINATION=knowledgeBase.writeAsDocument)] mirroring
ContextStoreSourceFacade, persists via WorkflowService, sets workflow_id on
the source via platform service, and triggers the initial sync via
PrincipalJobFacade.

KnowledgeBaseSourceSyncJobListener lives in platform-knowledge-base-service
(mirrors ContextStoreSyncJobListener placement; operates by source_id only,
no workspace lookup). Listens for Spring Batch JobExecution events and acts
only when the destination is knowledgeBase.writeAsDocument. After COMPLETED:
aggregates seenRecordIds from StepExecutions and tombstones any documents
whose source_record_id was not seen this run via platform
KnowledgeBaseDocumentRepository.tombstoneUnseen(...). Updates source.status to
READY with lastSyncRunAt and lastSyncJobExecutionId. After FAILED: preserves a
READY source's status (transient failures don't downgrade), flips to FAILED
only if the source was still BUILDING_PREVIEW. Mode-aware: PARTIAL skips
tombstone sweep and preserves status.

Plan reference: Phase 13 Task 32 (post-2026-05-09 KB-to-platform layout).
```

### Task 32a: CS retrofit — apply the same `mode` parameter to existing `contextStore.writeToReplica`

**Why this task exists**: Phase 13 ships `knowledgeBase.writeAsDocument` with the `mode` parameter (`FULL_REPLACE` | `PARTIAL`) baked in from day 1 (Tasks 31 + 32). The existing CS surface (`contextStore.writeToReplica`, already shipped through Phase 5 Task 12) has the same composability sharp edge — any custom workflow using `contextStore.writeToReplica` triggers the listener's tombstone sweep on the source, which is wrong for partial-update workflows. Backfill the same `mode` parameter onto the CS surface so both DESTINATION cluster elements have a uniform contract. Backward compatible: workflows missing the parameter get `FULL_REPLACE` (current behavior).

**Files to modify (CS core under `server/ee/libs/platform/platform-context-store/`; component module under `server/ee/libs/modules/components/context-store/`):**
1. `platform-context-store-service/.../listener/ContextStoreSyncJobListener.java`:
   - Add a `MODE_PARAMETER = "mode"` constant.
   - In `afterJob(JobExecution)` on COMPLETED, read `mode` from the destination's `inputParameters` (default `"FULL_REPLACE"`).
   - Branch the existing tombstone path:
     - `FULL_REPLACE`: existing behavior — `contextStoreRecordService.tombstoneUnseen(sourceId, entityName, seenIds, now)` + `sourceService.updateStatus(sourceId, READY, now, jobExecution.getId())`.
     - `PARTIAL`: skip the tombstone sweep. Update `lastSyncRunAt` + `lastSyncJobExecutionId` only — do NOT flip `status` from `BUILDING_PREVIEW` (a partial run does not prove readiness). Use a new `ContextStoreSourceService.updateLastSyncMetadata(sourceId, lastSyncRunAt, jobExecutionId)` method.
2. `platform-context-store-api/.../service/ContextStoreSourceService.java` + `platform-context-store-service/.../service/ContextStoreSourceServiceImpl.java`:
   - Add `void updateLastSyncMetadata(Long id, @Nullable Instant lastSyncRunAt, @Nullable Long jobExecutionId)` — load + setters + save (mirroring the existing `updateStatus` pattern, minus the status field).
3. `server/ee/libs/modules/components/context-store/.../ContextStoreComponentHandler.java`:
   - Modify the existing `writeToReplicaClusterElement` registration to add the `mode` property:
     ```java
     .properties(
         integer(SOURCE_ID).label("Context Source ID").required(true),
         string(ENTITY_NAME).label("Entity Name").required(true),
         string("mode")
             .label("Sync mode")
             .description("FULL_REPLACE = tombstone records not seen this run (default); " +
                 "PARTIAL = leave unseen records untouched (use for backfills / partial updates).")
             .options(option("FULL_REPLACE", "FULL_REPLACE"), option("PARTIAL", "PARTIAL"))
             .defaultValue("FULL_REPLACE")
             .required(false))
     ```
4. `platform-context-store-service/.../util/ContextStoreWorkflowGenerator.java`:
   - Modify the auto-generated workflow's destination parameter map to **explicitly** include `mode: "FULL_REPLACE"` — defensive, immune to a future default flip on the cluster element.
5. Liquibase: **none** — `mode` is a workflow parameter, not a column. Workflows that already exist in the database without the `mode` parameter continue to work because the listener defaults to `FULL_REPLACE` when the key is absent.

**Tests to add / extend:**
- `ContextStoreSyncJobListenerTest`:
  - Existing tests should keep passing (default mode = FULL_REPLACE = current behavior).
  - Add `testAfterJobOnCompletedPartialSkipsTombstoneSweep` — mode = PARTIAL; verify `tombstoneUnseen` is not called and `updateStatus` is not called; `updateLastSyncMetadata` is called.
  - Add `testAfterJobOnCompletedPartialPreservesBuildingPreviewStatus` — pre-condition: source status = BUILDING_PREVIEW, mode = PARTIAL completes; assert status unchanged.
  - Add `testAfterJobModeDefaultsToFullReplaceWhenAbsent` — destination params without `mode` key; old behavior preserved.
- `ContextStoreSourceServiceIntTest`:
  - Add `testUpdateLastSyncMetadataDoesNotChangeStatus` — verify the new helper updates only the two timestamp columns.
- `ContextStoreItemWriterTest`:
  - Add `testOpenReadsModeFromInputParametersDefaultFullReplace` — when `mode` is absent in inputParameters, writer defaults to FULL_REPLACE; when explicitly PARTIAL, writer reads it through.
  - Add `testInvalidModeRejectedAtOpen` — `mode = "BOGUS"` throws `IllegalArgumentException` at `open()`.
- `ContextStoreWorkflowGeneratorTest`:
  - Update existing test that snapshots the auto-generated workflow definition to include `mode: "FULL_REPLACE"` in the DESTINATION parameter map. Delete the old snapshot files from BOTH `src/test/resources/definition/` AND `build/resources/test/definition/` before rerunning (per MEMORY.md "Task dispatcher definition snapshot tests" lesson — applies to any snapshot test).

**Migration concern**: workflows in the database that were created before this task (e.g., from a deployment running Phase 12 in production) **do not** have `mode` in their destination parameters. They continue to work correctly because the listener defaults to `FULL_REPLACE` when the parameter is absent. No migration needed. After this task lands, any future call to `WorkspaceContextStoreSourceFacade.update()` that re-emits the workflow will include the explicit `mode` value.

**Run + commit:**

```bash
./gradlew spotlessApply
./gradlew :server:ee:libs:platform:platform-context-store:platform-context-store-api:check \
          :server:ee:libs:platform:platform-context-store:platform-context-store-service:check \
          :server:ee:libs:modules:components:context-store:check
./gradlew :server:ee:libs:platform:platform-context-store:platform-context-store-service:testIntegration
```

Single commit:

```
4855 Context Store - Retrofit `mode` parameter on contextStore.writeToReplica

Backfills the FULL_REPLACE | PARTIAL mode parameter (introduced for KB-Source
in Phase 13) onto the existing CS DESTINATION cluster element + listener for
parity. FULL_REPLACE (default) keeps current behavior: tombstone sweep on,
source status flips to READY on COMPLETED. PARTIAL skips the tombstone sweep
entirely, updates only lastSyncRunAt + lastSyncJobExecutionId, preserves
status (so a BUILDING_PREVIEW source isn't promoted to READY by a partial
run that touched only a subset of records).

Backward compatible: workflows in the DB without the mode parameter resolve
to FULL_REPLACE at the listener (current behavior). No Liquibase migration.

ContextStoreWorkflowGenerator now emits mode: "FULL_REPLACE" explicitly in
the auto-generated workflow's destination parameter map -- defensive against
a future default-flip on the cluster element.

ContextStoreSourceService gains updateLastSyncMetadata(id, lastSyncRunAt,
jobExecutionId) used by the listener's PARTIAL branch (status-preserving
sync-metadata update; mirrors updateStatus minus the status param).

Plan reference: Phase 13 Task 32a.
```

### Task 33: GraphQL surface + E2E IntTest skeleton (folded into one task)

**Files (in `automation-knowledge-base-graphql`):**
- Modify the existing `*.graphqls` schema file (or create a new `knowledge-base-source.graphqls` if the existing schema is large enough to warrant the split — implementer's choice). Add:
  - `KnowledgeBaseSource` type, `KnowledgeBaseSourceFilter` input, `CreateKnowledgeBaseSourceInput`, `UpdateKnowledgeBaseSourceInput`.
  - `KnowledgeBaseSourceStatus` and `ReaderStrategy` enums.
  - Extend the existing `KnowledgeBaseDocument` type with `sourceId: ID`, `sourceRecordId: String`, `lastSeenAt: Long`, `deletedAt: Long` fields.
  - Queries: `knowledgeBaseSource(id: ID!): KnowledgeBaseSource`, `knowledgeBaseSources(workspaceId: ID!, filter: KnowledgeBaseSourceFilter): [KnowledgeBaseSource!]!`.
  - Mutations: `createKnowledgeBaseSource`, `updateKnowledgeBaseSource`, `deleteKnowledgeBaseSource`, `refreshKnowledgeBaseSource(id: ID!): ID!` (admin-only via `@PreAuthorize`), `setKnowledgeBaseSourceEnabled`.
  - **The `KnowledgeBaseSource` GraphQL type does NOT expose `workspaceId`** — the entity has no `workspace_id` column post-platform-pivot; workspace is implicit via the relation table. Workspace-scoped reads use the `workspaceId` query parameter to resolve via `WorkspaceKnowledgeBaseSourceService.getAllByWorkspaceId(...)`.
- Create `KnowledgeBaseSourceGraphQlController.java` (package `com.bytechef.automation.knowledgebase.web.graphql`). The controller imports cross both packages:
  - From `com.bytechef.platform.knowledgebase.*`: `KnowledgeBaseSource`, `KnowledgeBaseSourceStatus`, `ReaderStrategy`, `KnowledgeBaseSourceService` (for read paths that are not workspace-scoped, e.g. `knowledgeBaseSource(id)` after the controller has resolved `workspaceId` via the relation service).
  - From `com.bytechef.automation.knowledgebase.*`: `WorkspaceKnowledgeBaseSourceFacade` (for all mutations), `WorkspaceKnowledgeBaseSourceService` (for `getAllByWorkspaceId`, `fetchWorkspaceIdByKnowledgeBaseSourceId`).
  - Pattern for source-id-only mutation inputs (e.g. `refreshKnowledgeBaseSource(id: ID!)`): resolve `workspaceId` via `WorkspaceKnowledgeBaseSourceService.fetchWorkspaceIdByKnowledgeBaseSourceId(sourceId)` before calling workspace-aware facade methods. Same pattern as `ContextStoreSourceGraphQlController`.
- Create DTOs (`CreateKnowledgeBaseSourceGraphQlInput`, `UpdateKnowledgeBaseSourceGraphQlInput`, `KnowledgeBaseSourceFilter`).
- Create `KnowledgeBaseSourceGraphQlControllerIntTest` (mirror CS pattern; `@GraphQlTest` slice with `@EnableMethodSecurity` per the lesson learned from CS Task 16).

**Add to `client/codegen.ts`** the new graphqls path so the regenerated `graphql.ts` picks up the types.

**Files (E2E IntTest skeleton, follows CS Task 15 + 27 precedent — `@Disabled`):**
- Place the skeleton wherever the CS E2E IntTest lives — for CS that's `server/ee/libs/platform/platform-context-store/platform-context-store-service/.../ContextStoreSyncE2EIntTest.java` (mirror CS placement). For KB-Source the equivalent is `server/libs/platform/platform-knowledge-base/platform-knowledge-base-service/src/test/java/com/bytechef/platform/knowledgebase/source/KnowledgeBaseSourceSyncE2EIntTest.java`. Place there since the test exercises platform-side sync infrastructure (entity + service + listener + writer); workspace-aware orchestration is a separate facade concern with its own unit/IntTests in automation.
- Class-level Javadoc lists the scenarios: initial sync from `jsonFile.read` source persists docs with `source_id` populated and triggers KB chunker via platform `KnowledgeBaseDocumentEvent`; re-sync with changed payload bumps `synced_payload_hash` and re-runs chunker; re-sync with unchanged payload only bumps `lastSeenAt`; tombstone-on-disappear sets `deleted_at`; deleted source orphans its docs (sets their `source_id = NULL` per the FK ON DELETE SET NULL). All `@Disabled` — manual run only, requires the full Atlas + DataStream + chunker stack.

Run codegen, smoke-test the GraphQL controller IntTest (with method security enabled), commit:

```bash
./gradlew :server:libs:automation:automation-knowledge-base:automation-knowledge-base-graphql:check
./gradlew :server:libs:automation:automation-knowledge-base:automation-knowledge-base-graphql:integrationTest --tests 'KnowledgeBaseSourceGraphQlControllerIntTest'
cd client && npx graphql-codegen
```

```
4855 KB client - Regenerate graphql.ts for knowledge-base-source schema additions

[+ server-side commit covering controller + DTOs + schema + tests]
```

(This task is 2-3 commits depending on how the implementer organizes server-side schema vs client codegen drift; treat the commit boundary as their judgment call.)

### Phase 13 review checklist

After Tasks 29-33 land:
- [ ] `KnowledgeBaseSource` entity exists in `platform-knowledge-base-api` (NO `workspace_id` column); `WorkspaceKnowledgeBaseSource` relation entity exists in `automation-knowledge-base-api`; `EnumOrdinalStabilityTest` pins `KnowledgeBaseSourceStatus` and `ReaderStrategy` ordinals.
- [ ] Platform `knowledge_base_document` has 5 new nullable columns; partial UNIQUE on `(source_id, source_record_id)` enforced; manual uploads keep all 5 NULL and pass `KnowledgeBaseDocumentRepositoryIntTest`. Companion `workspace_knowledge_base_source` relation table exists with UNIQUE `(workspace_id, knowledge_base_source_id)`.
- [ ] Platform `KnowledgeBaseDocumentRepository.tombstoneUnseen` issues a single UPDATE statement (verified via SQL trace in IntTest).
- [ ] `knowledgeBase.writeAsDocument` cluster element registered on the existing `knowledgeBase` component (`server/libs/modules/components/ai/vectorstore/knowledgebase/`) with `sourceId` (required) + `mode` (optional, default `FULL_REPLACE`, allowed values `FULL_REPLACE | PARTIAL`); cluster-element JSON in `src/test/resources/definition/` regenerated. Writer takes only `sourceId` + `mode` via inputParameters (no workspace lookup).
- [ ] `WorkspaceKnowledgeBaseSourceFacade.create()` (in automation-knowledge-base-service) auto-generates a workflow with the correct shape, emits `mode: "FULL_REPLACE"` explicitly in the destination parameter map, populates `knowledge_base_source.workflow_id` via the platform service, AND inserts the `workspace_knowledge_base_source` relation row.
- [ ] `WorkspaceKnowledgeBaseSourceFacade` lives in `automation-knowledge-base-{api,service}` (NOT in platform); facade method signatures all take `workspaceId` as the first parameter; dependency direction is automation → platform only.
- [ ] `KnowledgeBaseSourceSyncJobListener` lives in `platform-knowledge-base-service` (mirrors `ContextStoreSyncJobListener` placement); acts only on jobs whose destination is `knowledgeBase.writeAsDocument`; CS sync jobs are unaffected. Operates by `source_id` only (no workspace lookup). Both `FULL_REPLACE` and `PARTIAL` paths covered by listener tests; `PARTIAL` path skips tombstone sweep and preserves source `status`.
- [ ] **Task 32a CS retrofit**: `contextStore.writeToReplica` cluster element gains the same `mode` parameter; existing `ContextStoreSyncJobListener` honors it; `ContextStoreWorkflowGenerator` emits `mode: "FULL_REPLACE"` explicitly. Backward-compatible: pre-Task-32a workflows in the DB without the parameter resolve to `FULL_REPLACE` at the listener.
- [ ] `QueryKnowledgeBaseToolCallback` (existing CC EE) returns synced and manual docs indistinguishably (no test changes needed; verify by running its existing IntTest after a synced doc has been ingested).
- [ ] `client/src/shared/middleware/graphql.ts` regenerated with `KnowledgeBaseSource` types committed.
- [ ] `refreshKnowledgeBaseSource` is admin-only via `@PreAuthorize`.
- [ ] GraphQL controller (`KnowledgeBaseSourceGraphQlController` in `automation-knowledge-base-graphql`) imports both `com.bytechef.platform.knowledgebase.*` (entity types) and `com.bytechef.automation.knowledgebase.*` (`WorkspaceKnowledgeBaseSourceFacade`/`WorkspaceKnowledgeBaseSourceService`).

---

> **Old Phase 13 tasks (Knowledge Sync as parallel primitive) — deleted from this plan as of the 2026-05-08 redesign.** They live in commits `3c8b1dce19d`...`9b46349ef68` for historical reference; those commits will be reverted before the new Phase 13 starts. Do not run them.

## Phase 14: Semantic search add-on (gated; in MVP)

Per spec §10. Adds optional embedding-based retrieval over `context_store_record` and over `knowledge_base_document_chunk` (which already has it via the existing KB pipeline — this phase only adds the *Context Store* side and one new tool callback for parity with the structured search). Gated by `@ConditionalOnBean(EmbeddingModel.class)` so deployments without an embedding model continue to work — the structured CS surface remains fully functional. Spring AI's `EmbeddingModel` interface is the only dependency; no gateway-specific imports.

The `semantic_index_fields` JSONB column was already created in Phase 2's CS init migration; this phase fills in the embedding pipeline + retrieval surface against that pre-laid storage.

### Task 34: PgVectorStore configuration + ContextStoreSemanticBatchListener

> **Approach (per spec §10 + §16 "Phase 14 prep" decision-log entry)**: use Spring AI's `PgVectorStore` to manage the embedding storage — same pattern as `KnowledgeBasePgVectorConfiguration`. PgVectorStore self-initializes its table at startup via `.initializeSchema(true)`. **No Liquibase changeset is added** for embeddings. Hash-skip cost saver via `JdbcTemplate` query against PgVectorStore's metadata column.

**Files (in `platform-context-store-service`):**

1. `config/ContextStorePgVectorConfiguration.java` — NEW. Mirror `server/libs/platform/platform-knowledge-base/platform-knowledge-base-service/src/main/java/com/bytechef/platform/knowledgebase/config/KnowledgeBasePgVectorConfiguration.java` exactly. Annotations: `@Configuration @EnableConfigurationProperties(PgVectorStoreProperties.class) @ConditionalOnBean(EmbeddingModel.class) @ConditionalOnSingleTenant`. EE Enterprise license + `@version ee`. Two beans:
   - `contextStorePgVectorStore(...)` — `PgVectorStore.builder(jdbcTemplate, embeddingModel)` with `.vectorTableName("cs_" + properties.getTableName())` (mirrors KB's `"kb_" + ...` prefix), `.initializeSchema(true)`, distance/index/dimension from properties, observation/batching pass-throughs. Returns `VectorStore` typed; bean name distinguishes from KB's bean.
   - (Optional, if needed by the search service in Task 35) `contextStoreVectorStoreMetadataService(...)` — KB has a parallel metadata service for `JdbcTemplate`-driven queries against the PgVectorStore table; replicate if Task 35 needs it for the hash-skip query. Defer until Task 35 confirms the need; for Task 34 the listener can call the metadata-lookup SQL via the injected `JdbcTemplate` directly.

2. `listener/ContextStoreSemanticBatchListener.java` — NEW. Annotations: `@Component @ConditionalOnBean(EmbeddingModel.class) @ConditionalOnSingleTenant`. EE license + `@version ee`. Implements `JobExecutionListener`. Constructor-injected:
   - `VectorStore contextStorePgVectorStore` (the `@Qualifier`-annotated bean from `ContextStorePgVectorConfiguration` — verify the bean name matches)
   - `@Qualifier("pgVectorJdbcTemplate") JdbcTemplate jdbcTemplate` (for the hash-skip metadata lookup)
   - `ContextStoreEntityService`, `ContextStoreRecordService` (for fetching entity config + record payloads)
   - `PgVectorStoreProperties properties` (so the listener can compute the table name `cs_{properties.getTableName()}` for the hash-skip query)

   **`afterJob(JobExecution)` body** (mirror `ContextStoreSyncJobListener.afterJob` shape for destination discrimination + StepExecution traversal):
   - Short-circuit unless destination is `contextStore.writeToReplica` AND `BatchStatus == COMPLETED`.
   - Read `sourceId`, `entityName` from destination params (same approach the structured listener uses).
   - Fetch the `ContextStoreEntity` via `entityService.fetchBySourceIdAndEntityName(sourceId, entityName)`. If absent OR `entity.getSemanticIndexFields()` is null/empty, return.
   - Aggregate `seenIds` from each `StepExecution.executionContext.get("contextStore.seenIds")` (the structured writer flushes this — match the same key constant `ContextStoreItemWriter.SEEN_IDS_KEY`).
   - Wrap the entire embedding pass in a single `try { ... } catch (Exception e) { log.warn(...); }` so failures here do NOT propagate to the structured sync. Log failure + a metric (use SLF4J 2.x fluent logger if available; else plain logger).
   - Inside the try block, for each `seenSourceRecordId`:
     - Fetch the `ContextStoreRecord` via `recordService.fetchByKey(sourceId, entityName, sourceRecordId)`. Skip if absent.
     - Hash-skip: query `cs_vector_store` (or whatever the configured table name is — derive from properties) via `jdbcTemplate.queryForObject("SELECT metadata->>'payloadHash' FROM " + tableName + " WHERE id = ?", String.class, record.getId().toString())`. Wrap in try/catch on `EmptyResultDataAccessException` to handle the "no existing embedding" case (returns null → don't skip).
     - If `storedHash != null && storedHash.equals(record.getPayloadHash())`, continue (skip).
     - Build `text` by concatenating `record.payload[field]` for each field in `entity.getSemanticIndexFields().fields()`. Use the existing dotted-path resolver pattern from `ContextStoreItemWriter` (or `ContextStoreQueryService` — check which one already has the helper to share).
     - Build a `Document` with `id=record.getId().toString()`, `content=text`, `metadata=Map.of("recordId", record.getId(), "sourceId", sourceId, "entityName", entityName, "payloadHash", record.getPayloadHash())`.
     - `vectorStore.add(List.of(document))` — PgVectorStore upserts by id (re-embeds the content via the injected `EmbeddingModel`).

**Files (test, in `platform-context-store-service`):**

3. `listener/ContextStoreSemanticBatchListenerTest.java` — NEW unit test. Mocks: `VectorStore`, `JdbcTemplate`, `ContextStoreEntityService`, `ContextStoreRecordService`. Mock `JobExecution` + `StepExecutions` per the `ContextStoreSyncJobListenerTest` pattern (read it). Cover:
   - `testAfterJobEmbedsRecordsWhenSemanticIndexFieldsConfigured`
   - `testAfterJobSkipsRecordsWhenPayloadHashUnchanged` (mock JdbcTemplate to return same hash → verify `vectorStore.add` NOT called)
   - `testAfterJobEmbedsRecordsWhenStoredHashIsNullEmptyResult` (mock JdbcTemplate to throw `EmptyResultDataAccessException` → verify embed proceeds)
   - `testAfterJobSkipsEntityWithoutSemanticIndexFields` (entity has empty `semanticIndexFields` → no `vectorStore.add`)
   - `testAfterJobShortCircuitsForNonContextStoreDestination` (destination is some other component → no work)
   - `testAfterJobOnFailedShortCircuits` (BatchStatus.FAILED → no embedding pass)
   - `testEmbeddingFailuresDoNotPropagate` (`vectorStore.add` throws → listener swallows + logs; test passes)

4. `ContextStoreSemanticEmbeddingE2EIntTest.java` — NEW disabled IntTest skeleton at the same path level as `ContextStoreSyncE2EIntTest`. `@Disabled("Requires real EmbeddingModel + PgVector extension via Testcontainers — manual run only.")`. 4 `@Test @Disabled` methods covering: initial embed; re-sync hash-skip (no new embeddings); per-entity opt-out (entity without `semanticIndexFields`); embedding-API failure doesn't fail structured sync.

**Build.gradle.kts additions** in `platform-context-store-service/build.gradle.kts`:
- `implementation("org.springframework.ai:spring-ai-pgvector-store")` (the Spring AI PgVectorStore artifact; verify exact coordinate by reading KB-service's build.gradle.kts).
- `implementation("org.springframework.ai:spring-ai-autoconfigure-vector-store-pgvector")` (provides `PgVectorStoreProperties`).
- The `@ConditionalOnSingleTenant` annotation lives in the existing `tenant-api` module — likely already on the classpath transitively; verify by reading KB-service's deps.

Run + commit:
```
4855 Context Store - Add ContextStorePgVectorConfiguration + ContextStoreSemanticBatchListener (Phase 14 Task 34)

Lands the storage + listener half of the semantic search add-on per
spec §10 + §16 "Phase 14 prep" decision-log entry. Gated by
@ConditionalOnBean(EmbeddingModel.class) + @ConditionalOnSingleTenant
so deployments without an embedding model OR multi-tenant deployments
see no behavior change.

Storage:
- ContextStorePgVectorConfiguration mirrors KnowledgeBasePgVectorConfig
  uration. PgVectorStore self-initializes the cs_vector_store table at
  startup; no Liquibase changeset added.
- The PgVectorStore Document carries id=record.getId(), content=concat
  ed semantic-index text, metadata={recordId, sourceId, entityName,
  payloadHash}.

Listener:
- ContextStoreSemanticBatchListener picks up after every Context Store
  sync job (mode-agnostic — semantic embedding doesn't care whether
  mode was FULL_REPLACE or PARTIAL, only that records changed).
- For each seen record whose entity has semanticIndexFields configured:
  hash-skip via JdbcTemplate query against cs_vector_store.metadata->>'
  payloadHash'; if same hash, skip. Otherwise build text + embed via
  vectorStore.add (which calls EmbeddingModel.embed transparently).
- Failures inside the embedding pass are caught + logged; structured
  sync stays unaffected. Hash-skip means a transient embedding outage
  retries on the next successful sync.

Plan reference: Phase 14 Task 34.
```

### Task 35: ContextStoreSemanticSearchService + EE tool callback

**Files (in `platform-context-store-api`):**
- `service/ContextStoreSemanticSearchService.java` — interface. Method: `searchSemantic(sourceId, entityName, queryText, k, optional ContextStoreQueryFilter prefilter): List<SemanticHit>` returning `record SemanticHit(ContextStoreRecord record, double similarityScore)`. **Note:** no `workspaceId` argument — workspace authorization happens upstream in the caller (the `workspace_context_store_source` relation table is checked before delegating to this service), matching the post-2026-05-09 "workspace logic stays in automation" pivot already applied to `ContextStoreQueryService`.
- `service/ContextStoreQueryService.java` — **add new helper** `List<Long> searchRecordIds(long sourceId, String entityName, ContextStoreQueryFilter filter)` returning id-only projection for hybrid prefilter use. Avoids the wasteful round-trip of `search().getItems().stream().map(getId)` when only ids are needed.

**Files (in `platform-context-store-service`):**
- `service/ContextStoreSemanticSearchServiceImpl.java` — `@Service @ConditionalOnBean({EmbeddingModel.class, VectorStore.class})` and `@ConditionalOnSingleTenant`. Implementation uses the injected `VectorStore contextStorePgVectorStore` (the bean defined in Task 34's `ContextStorePgVectorConfiguration`):
  - **Mode 2 (pure semantic)**: `vectorStore.similaritySearch(SearchRequest.query(queryText).withTopK(k).withFilterExpression(b -> b.eq("sourceId", sourceId).and(b.eq("entityName", entityName))))`.
  - **Mode 3 (hybrid)**: first call `contextStoreQueryService.searchRecordIds(sourceId, entityName, prefilter)` to get the candidate id-set. Then `vectorStore.similaritySearch(SearchRequest.query(queryText).withTopK(k).withFilterExpression(b -> b.eq("sourceId", sourceId).and(b.eq("entityName", entityName)).and(b.in("recordId", candidateIds))))`. The metadata filter is translated by Spring AI's PgVectorStore into `cs_vector_store.metadata @> ...` JSONB filter clauses — no custom SQL.
  - For each ranked `Document` returned, hydrate the underlying `ContextStoreRecord` by looking up `recordId` from `Document.metadata` via `ContextStoreRecordService.findById(...)`. Wrap into `SemanticHit(record, doc.getMetadata().get("distance"))` — Spring AI exposes the cosine distance as a metadata entry; similarity = `1 - distance`.
  - Single-tenant restriction: PgVectorStore writes to a single shared `cs_vector_store` table without per-tenant isolation. Multi-tenant deployments need to skip this service (the `@ConditionalOnSingleTenant` guard does that automatically).
- Service IntTest using Testcontainers Postgres + pgvector image + a stub `EmbeddingModel` that returns deterministic vectors. Drives the listener (Task 34) to upsert documents, then exercises Mode 2 and Mode 3 search paths.

**Files (in `platform-context-store-api` — EE tool surface):**
- Modify `tool/ContextStoreToolFacade.java` to expose a parallel `getSemanticFunctionToolCallbacks(workspaceId)` method returning `List<ToolCallback>` for sources whose entities have `semanticIndexFields` configured. Conditional on the `ContextStoreSemanticSearchService` bean's presence (use `ObjectProvider<ContextStoreSemanticSearchService>` to keep the EE module compiling without it).
- Modify `tool/ContextStoreToolFacadeImpl.java` to mint per-(source, entity) `semantic_search_<source>_<entity>` callbacks using the same `FunctionToolCallback.builder` pattern as the existing structured callbacks. Input schema: `{queryText: string, k: integer = 10, prefilter?: ContextStoreQueryFilter}`.

**Files (CC EE):**
- `tool/SemanticSearchContextStoreToolCallback.java` (or just augment the existing `SearchContextStoreToolCallback` shape into a sibling) — the chat-surface callback wrapping the same service. `@ConditionalOnBean(ContextStoreSemanticSearchService.class)`. Registered on the ASK agent (read-only) in `AiHubConfiguration` under the same `ifAvailable` pattern used for the structured callbacks.

**Files (MCP server enumeration):**
- Modify `AutomationMcpServerConfiguration.java` (or its EE-side aggregation of `ContextStoreToolFacade`) to also pull in the semantic callbacks alongside the structured ones. Since `ContextStoreToolFacade.getSemanticFunctionToolCallbacks(workspaceId)` returns an empty list when the embedding model isn't configured, the existing aggregation just appends and the result is identical in CE/no-embedding deployments.

Run codegen drift if any GraphQL change is needed (likely none — the semantic surface is a tool callback, not a CRUD entity). Commit (likely 2 commits — one server, one any client follow-up).

### Phase 14 review checklist

- [ ] `cs_vector_store` table is auto-managed by Spring AI's `PgVectorStore.initializeSchema(true)` — **no Liquibase migration**. Schema is: `id UUID PK, content TEXT, metadata JSONB, embedding vector(N)` where N comes from the configured embedding model.
- [ ] `ContextStorePgVectorConfiguration` bean is `@Configuration @ConditionalOnBean(EmbeddingModel.class) @ConditionalOnSingleTenant` — disappears in CE-no-embedding deployments and in multi-tenant deployments.
- [ ] `ContextStoreSemanticBatchListener` only acts when `EmbeddingModel` + `VectorStore` are both present AND at least one entity has `semanticIndexFields`.
- [ ] Listener skips re-embedding records whose `payload_hash` matches the last-embedded hash. Hash-skip implementation queries `cs_vector_store.metadata->>'payloadHash'` directly via `JdbcTemplate` (the standard `VectorStore` API doesn't expose metadata-only reads efficiently — this is the documented escape hatch).
- [ ] Listener failures don't fail the structured sync (separate try/catch + metric).
- [ ] `ContextStoreSemanticSearchService` returns ranked hits with optional structured prefilter. Mode 3 hybrid uses `ContextStoreQueryService.searchRecordIds(...)` + `FilterExpression.in("recordId", candidateIds)` — NOT custom SQL.
- [ ] `semantic_search_<source>_<entity>` MCP callbacks appear when an embedding model is configured AND the source has `semanticIndexFields` populated.
- [ ] No imports of `com.bytechef.ee.platform.ai.gateway.*` anywhere in `platform-context-store/` or `automation-context-store/`.
- [ ] CE-without-embedding-bean smoke test: deploy without `spring.ai.*` config, verify CS still works and the semantic surface is silent (no beans, no callbacks, no errors).
- [ ] Multi-tenant smoke test: deploy with a multi-tenant profile, verify the semantic add-on disappears (no `cs_vector_store` table created, no listener wired, no callbacks).

---

## Phase 15: Client-side UI (in MVP)

Per spec §8. Ships the in-app surfaces for Context Store sources and Knowledge Base sources after the GraphQL backend has stabilized (Phases 9, 13, and 14 complete). The KB-Source UI piggybacks on this phase since it's a near-clone of the CS-Source UI shape and shares the same conventions/components.

The **scope is the visible UI surface** — sources lists, source detail pages, the create-source guided dialog, and the existing-document badges. It does NOT include the chat-surface integration (which already works via the EE CC tool callbacks committed in Phase 11).

### Server-side prerequisites (do these in Task 36a before any UI work starts)

The Phase 15 review surfaced 4 GraphQL gaps that the UI needs but neither Phase 9 nor Phase 13 added. Land them as a single back-end commit before any client commit:

1. **Add `environmentId: ID!` arg to `contextStoreSources` and `knowledgeBaseSources` queries** — both are environment-scoped via the existing `WorkspaceContextStoreSourceFacade` / `WorkspaceKnowledgeBaseSourceFacade` (which already take `environmentId`); the queries currently swallow it. Edit `context-store.graphqls` + `knowledge-base-source.graphqls` to add the arg, edit the resolvers to thread it through, regenerate the Java DTOs. Also add `environmentId: ID` to the `CreateContextStoreSourceInput` / `CreateKnowledgeBaseSourceInput` types so the create-source dialog can pass the active environment.

2. **Add `dataStreamCompatibleConnections(workspaceId: ID!, environmentId: ID!): [Connection!]!` query** to wherever the existing `Connection` GraphQL surface lives (likely `automation-configuration-graphql`). Resolver filters workspace connections to those whose component (resolved via `connectionDefinition.componentName/Version` per the CLAUDE.md note about version mismatch) exposes at least one `ItemReader` cluster element OR at least one action whose name starts with `list`. Returns the standard `Connection` type so the existing connection-display components in the dialog work unchanged. Used by both the CS and KB-Source create-source dialogs.

3. **Add 3 entity-level CRUD mutations to `context-store.graphqls`**:
   - `createContextStoreEntity(sourceId: ID!, input: CreateContextStoreEntityInput!): ContextStoreEntity!`
   - `updateContextStoreEntity(id: ID!, input: UpdateContextStoreEntityInput!): ContextStoreEntity!` — `UpdateContextStoreEntityInput { description, storedFields, indexedFields, semanticIndexFields, parameters }` (no `entityName` change post-create — would invalidate sync rows).
   - `deleteContextStoreEntity(id: ID!): Boolean!` — cascades record deletion via the existing FK; the resolver should refuse if the entity has live records and a `force` flag isn't set, OR document that delete-cascades-records is the expected behavior (recommend the latter — simpler).

   The `ContextStoreEntityService` already has the underlying CRUD methods; this is a pure GraphQL surface task. **Out of scope for Task 36a**: the corresponding edit-entity dialog is added in Task 37 itself; without these mutations, MVP would lock entities as read-only with a "delete and recreate the source to add an entity" caveat — the user picked option (a) to ship full CRUD instead.

4. **Add `setKnowledgeBaseDocumentEnabled` mutation? No.** Just confirming: the existing `setKnowledgeBaseSourceEnabled` mutation already exists on the source side. Document-level enable/disable doesn't apply to MVP.

Each of the 3 changes above includes:
- Resolver wiring + `@PreAuthorize` admin-gating where appropriate (admin for create/update/delete entity; non-admin for the read query).
- `EnumOrdinalStabilityTest` doesn't change (no new enums).
- Unit test for the resolver in the relevant `*GraphQlControllerTest` class.
- Apply Spotless on touched modules.

Commit message: `4855 KB+CS - Add Phase 15 prerequisites (env-scoped queries, dataStream connection filter, entity CRUD mutations)`.

After this commit lands, **regenerate `client/src/shared/middleware/graphql.ts`** via `cd client && npx graphql-codegen` and commit that as a separate "client - Regenerate graphql.ts" commit (no `.graphql` operation files yet — those come in Tasks 36-39).

### Task 36: Context Store sources list + detail pages

**Files (in `client/src/pages/automation/context-store/`):**
- `ContextStoreSources.tsx` — workspace-scoped + environment-scoped table of `contextStoreSources(workspaceId, environmentId)`. Columns: name, source component, entity count, status badge, last sync timestamp, refresh-now button, actions menu (edit / delete / enable-disable). Uses `useFetchInterceptor` for centralized error toasting. Header includes `<EnvironmentSelect />` (matching the `KnowledgeBases.tsx` precedent in `client/src/pages/automation/knowledge-bases/KnowledgeBases.tsx`).
- `ContextStoreSourceDetail.tsx` — selected-source page with: source metadata block, entities list with **inline edit/delete/add-entity controls** (gated on admin role; uses the new `createContextStoreEntity` / `updateContextStoreEntity` / `deleteContextStoreEntity` mutations from Task 36a), last sync block (just the most-recent run via `lastSyncRunAt` + `lastSyncJobExecutionId` — full sync-run history is **deferred to v2**; users dig into the Atlas Workflow Executions page for older runs by clicking the source's `workflowId`), manual refresh button, cadence editor.
- Routing: add to `client/src/routes.tsx` under `/automation/context-store` (mirrors `/automation/datatables` + `/automation/knowledge-bases` precedent at routes.tsx:731-761).
- Sidebar nav in `client/src/App.tsx`: add an entry to `automationNavigation` (around line 88-93, alphabetically between `Connections` and `Data Tables`):
  ```ts
  {
      href: '/automation/context-store',
      icon: BoxesIcon, // or another suitable Lucide icon with the Icon suffix
      name: 'Context Store',
  },
  ```
  Add a feature flag `ff_4855 = useFeatureFlagsStore()('ff-4855')` in App.tsx, and gate the nav entry:
  ```ts
  if (navItem.href === '/automation/context-store') {
      return ff_4855 && edition === EditionType.EE;
  }
  ```
  CS is EE-only; the flag dark-launches the UI separately from the back-end.
- `components/hooks/useContextStoreSources.ts` + `components/hooks/useContextStoreSource.ts` — thin wrappers around the codegen hooks if any state derivation is needed.

**`.graphql` operation files** (under `client/src/graphql/automation/context-store/`): `contextStoreSources.graphql`, `contextStoreSource.graphql`, `createContextStoreSource.graphql`, `updateContextStoreSource.graphql`, `deleteContextStoreSource.graphql`, `refreshContextStoreSource.graphql`, `setContextStoreSourceEnabled.graphql`, `createContextStoreEntity.graphql`, `updateContextStoreEntity.graphql`, `deleteContextStoreEntity.graphql`, `dataStreamCompatibleConnections.graphql`. Add the schema path to `client/codegen.ts` (matches the existing pattern at the top of the file). Run `npx graphql-codegen` to regenerate `graphql.ts`.

Conventions per CLAUDE.md:
- ESLint sort-keys (object keys alphabetical).
- Interface naming `*I` or `*Props`.
- Lucide icons with `Icon` suffix.
- `twMerge` for conditional classes (no `cn()`).
- React hook ordering: `useState` → `useRef` → store hooks → other custom hooks → derived (`useMemo`/`useCallback`) → `useEffect` → return.
- Named imports sorted alphabetically (CLAUDE.md "Client Import Destructure Sort Order").

Tests: `@testing-library/react` interaction tests (NOT Vitest snapshot tests — snapshot tests rot fast in this codebase; the existing `KnowledgeBases.test.tsx` style is what we mirror). Cover empty/loading/populated states by asserting the presence/absence of `EmptyList` vs the table; assert status badge text + className per enum value; assert the refresh button is hidden when the user is non-admin.

### Task 37: Add Context Source guided dialog

**Files (in `client/src/pages/automation/context-store/components/`):**
- `AddContextSourceDialog.tsx` — multi-step (5-7 step) dialog walking the user through:
  1. **Pick a workspace connection** — fetched via the new `dataStreamCompatibleConnections(workspaceId, environmentId)` query (Task 36a item 2). Filters to connections whose component exposes an `ItemReader` cluster element or a `list*` action. The picked connection drives the source component name + version (read from `connectionDefinition.componentName/Version` — NOT from `componentConnection.componentVersion`, per the existing CLAUDE.md note).
  2. **Pick the reader strategy** — `CLUSTER_ELEMENT` vs `LIST_ACTION`. Show only the strategies the source component actually supports (the connection-resolver in Task 36a returns this metadata; if not, fall back to listing both and letting the create mutation reject the wrong one).
  3. **Add one or more entities** — each is `{name, idField, indexedFields, optional storedFields, optional semanticIndexFields}`. The `indexedFields` editor is a tag-style multi-select with inline type selector (TEXT / NUMERIC / TIMESTAMP). The `semanticIndexFields` step appears only when the application info has an embedding model configured (read via `useApplicationInfoStore`).
  4. **Pick cadence** — preset chips (`@manual`, `@hourly`, `@daily`, `@weekly`) + a "Custom..." text input that defers validation to the server. **No client-side cron parsing library is needed**; the existing client has no cron deps and the back-end `validateCadence` (in `WorkspaceContextStoreSourceFacade.create`) already rejects invalid expressions. The submit button shows the server's error message inline if the cadence is rejected.
  5. **Review + submit**. On submit, calls `createContextStoreSource` mutation with `{workspaceId, environmentId, name, sourceComponentName, sourceComponentVersion, readerStrategy, sourceClusterElementName | sourceListActionName, connectionId, cadence, entities}`; on success, navigates to the source detail page and the sync runs immediately.
- `components/IndexedFieldsEditor.tsx` — reusable tag-multi-select for the `indexedFields` step.
- `components/CadencePicker.tsx` — preset chips + custom text input. **No cron library**; just preset string mappings (`@hourly` → `0 * * * *`, etc.) and pass-through for custom input.

The dialog uses `useState` + `useRef` for step state, NOT a Zustand store (Zustand is overkill for an in-page wizard; refs are enough).

Tests: cover (a) connection picker shows only data-stream-compatible connections, (b) cadence preset chips populate the cadence string correctly, (c) submit calls `createContextStoreSource` with the correct payload, (d) server rejection of bad cadence renders inline.

### Task 38: KB-Source UI — "Sources" tab inside KB detail + Add KB Source dialog

**Decision:** KB-Source UI is a **tab inside the existing KB detail page** (`/automation/knowledge-bases/:id`), NOT a sibling top-level page. Rationale: a `KnowledgeBaseSource` always belongs to a specific KB (`knowledgeBaseId` is required), so embedding the source list under the parent KB groups the related data and avoids a "which KB does this source belong to?" guessing game. The existing KB detail layout has a left sidebar listing knowledge bases; the right pane gets a tab strip (Documents / Sources / Settings).

**Files (in `client/src/pages/automation/knowledge-base/`):**
- Modify `KnowledgeBase.tsx` to add a tab strip — Documents (existing), Sources (new), Settings (if a settings tab already exists, leave it; otherwise punt). Use the existing tabs primitive (`shadcn/ui Tabs` or the equivalent already in use elsewhere).
- `components/KnowledgeBaseSourcesTab.tsx` — the Sources tab body. Workspace + environment + KB-scoped table of `knowledgeBaseSources(workspaceId, environmentId, filter: {knowledgeBaseId})` (extend the existing filter input to include `knowledgeBaseId`). Columns mirror CS sources list. Uses the same `SyncSourceStatusBadge` (see below).
- `components/AddKnowledgeBaseSourceDialog.tsx` — 4-step flow (connection → reader → cadence → review). Smaller than the CS dialog because there's no `indexedFields` / `semanticIndexFields` complexity. The KB id is implicit from the page context (no KB-picker step). Same connection-picker query (`dataStreamCompatibleConnections`). Same cadence picker. On submit calls `createKnowledgeBaseSource`.
- `components/KnowledgeBaseSourceDetail.tsx` — opens as a side-sheet or inline expand from the Sources tab row (decide based on existing patterns; if the KB detail page is already cramped, prefer a side-sheet route like `/automation/knowledge-bases/:id/sources/:sourceId`).
- Modify the existing KB document list (find the component that renders `knowledgeBaseDocuments` rows) to add a "Sync source" badge column populated from `knowledgeBaseDocument.sourceId`. Clicking the badge navigates to the source detail. Manual uploads keep the column empty. Add `sourceId` to the `.graphql` operation's selection set if it's not already there (verify in `client/src/graphql/automation/knowledge-base/`); regenerate.

**`.graphql` operation files** (under `client/src/graphql/automation/knowledge-base/`): `knowledgeBaseSources.graphql`, `knowledgeBaseSource.graphql`, `createKnowledgeBaseSource.graphql`, `updateKnowledgeBaseSource.graphql`, `deleteKnowledgeBaseSource.graphql`, `refreshKnowledgeBaseSource.graphql`, `setKnowledgeBaseSourceEnabled.graphql`. Add `sourceId` to the existing document list query's selection set (probably `knowledgeBase.graphql` or `knowledgeBaseDocuments.graphql`). Regenerate.

### Task 38a: Shared `SyncSourceStatusBadge` component

**File:** `client/src/shared/components/SyncSourceStatusBadge.tsx`

A shared status pill used by both `ContextStoreSources.tsx` and `KnowledgeBaseSourcesTab.tsx`. Both backends use the same 5-value enum (`BUILDING_PREVIEW / PREVIEW / READY / FAILED / DISABLED`), so one component handles both — placed in `shared/components/` rather than under either feature folder so there's no cross-feature import.

Visual mapping (verify against the existing `EnvironmentBadge.tsx` and `WorkflowExecutionBadge.tsx` color conventions):
- `READY` → green
- `BUILDING_PREVIEW` → yellow
- `PREVIEW` → blue
- `FAILED` → red
- `DISABLED` → gray

Props: `{status: string}` (accepts either enum since they're string-equivalent at the GraphQL boundary).

Tests: assert each enum value renders with the correct text + className.

### Task 39: GraphQL hooks regeneration + final UI checks

After all `.graphql` operation files from Tasks 36-38 are added (and `client/codegen.ts` references the new schema files), run `cd client && npx graphql-codegen` once more to pick up any drift. Then `npm run check` clean.

Verify:
- `client/src/shared/middleware/graphql.ts` includes typed hooks for every operation referenced in the new pages.
- No unused imports (CLAUDE.md sort-order rules also catch most ordering issues at lint time).
- All test files pass.
- Visual regression check: navigate through CS sources list → detail → Add Source dialog; navigate through KB detail → Sources tab → Add KB Source dialog. Status badges, environment select, refresh button, and admin gating all behave per spec.

### Phase 15 review checklist

- [ ] Task 36a server prerequisites landed in a single commit before any client commit: `environmentId` arg added to `contextStoreSources` + `knowledgeBaseSources` queries; `dataStreamCompatibleConnections` query added; 3 entity-level CRUD mutations added to `context-store.graphqls`; resolvers wired with `@PreAuthorize` admin-gating where appropriate.
- [ ] CS sources list + detail page reachable from `/automation/context-store`; nav entry gated on `ff_4855 && edition === EditionType.EE`.
- [ ] Entities tab on CS source detail supports add/edit/delete (admin-only) via the new entity mutations.
- [ ] CS source detail page does NOT include a sync-run history panel — only the last run's metadata. Older runs are reachable via the source's `workflowId` link to the existing Atlas Workflow Executions page (deferred to v2 per Phase 15 review option 1b).
- [ ] KB-Source list reachable as a "Sources" tab inside `/automation/knowledge-bases/:id` (NOT a top-level sibling page).
- [ ] KB document list rows show a "Sync source" badge linking to the source detail when `sourceId IS NOT NULL`.
- [ ] `SyncSourceStatusBadge` (in `client/src/shared/components/`) is the shared status pill used by both CS and KB-Source pages — colors match existing conventions (green/yellow/blue/red/gray).
- [ ] Connection picker in both Add-Source dialogs uses `dataStreamCompatibleConnections` — only connections whose component supports `ItemReader` cluster elements or `list*` actions appear.
- [ ] Cadence picker is preset chips + custom text input with **no client-side cron library** (existing client has no cron deps; server validates via the existing facade).
- [ ] Add-Source dialog uses `useState` + `useRef` for step state, NOT a Zustand store.
- [ ] Tests: `@testing-library/react` interaction tests (NOT snapshot tests). All pass.
- [ ] `npm run check` passes (prettier + eslint + tsc + vitest).
- [ ] `client/src/shared/middleware/graphql.ts` regenerated; codegen has no drift; commit kept separate from operation-file commits.
- [ ] No imports of CS or KB-Source code from outside the workspace nav scope (e.g., neither page leaks into the embedded nav).

---

## Phase 16: Optional ClickHouse store (post-MVP, separate plan)

Per spec §12a. Swap-in alternative record-repository backend: Postgres (default, MVP) or ClickHouse (replaces Postgres entirely on a per-workspace basis). Both impls satisfy the same `ContextStoreQueryService` contract — agents and the synthetic component don't see the backend choice. Control plane (sources/entities/cadence/status) stays in Postgres regardless.

**Six tasks** at this phase (not detailed here; spelled out when scheduled):

- **Task 40**: Refactor `ContextStoreRecordRepository` into an interface + extract Postgres impl. Repository methods (`findByKey`, `tombstoneUnseen`, etc.) become the contract; current Postgres-specific code becomes `ContextStoreRecordPostgresRepository`. Service layer changes to depend on the interface.
- **Task 41**: Scaffold ClickHouse module: `server/ee/libs/platform/platform-context-store/platform-context-store-clickhouse-service/` with the ClickHouse JDBC driver dependency, configuration class, and a connection-factory bean conditional on `bytechef.context-store.backend=clickhouse`.
- **Task 42**: Per-entity dynamic-table generator. At "Add Context Source" time (when backend=clickhouse), generate `CREATE TABLE context_store_{workspace}_{source}_{entity} (...)` from `indexedFields`. Schema includes typed columns, `_id`, `_payload_hash`, `_last_seen_at`, `_deleted_at`, `_payload JSON` (or String + JSONExtract* for older deployments). Engine: `ReplacingMergeTree(_last_seen_at) ORDER BY (_id)`.
- **Task 43**: `ContextStoreRecordClickHouseRepository` implements the same contract: `INSERT` (relies on RMT to dedup), tombstone via UPDATE `_deleted_at`, query translation. Filter ops translate directly except JSONB ops which become `JSONExtract*` calls.
- **Task 44**: Migration runner for ClickHouse (separate from Liquibase since Liquibase doesn't handle ClickHouse cleanly). Use `clickhouse-migrator` or generate `ALTER TABLE` at "Add/Update Context Source" time.
- **Task 45**: IntTest with Testcontainers ClickHouse — same scenarios as `ContextStoreSyncIntTest` (initial sync, change-detect, tombstone) but on the ClickHouse path.

> **Phase note**: Phase 15 (Atlas dispatch wrapper) was dropped via the 2026-05-08 DataStream pivot — with DataStream as the sync mechanism, Atlas already executes Context Store sync workflows natively (any Worker can pick up a `data-stream.stream` task containing the `contextStore.writeToReplica` DESTINATION). Distribution is built-in; no wrapper layer required. The slot is reused for Client-side UI per the 2026-05-08 ordering pivot.

---

## Phase 17: Incremental sync via DataStream `ItemReader` SPI extension (post-MVP, separate plan)

Both CS and KB-Source MVP do **full pulls** every sync run — the reader emits all upstream records, the writer's hash-skip fast path makes unchanged records cheap (no chunker re-run, no index rebuild, just `last_seen_at` bumped) but the **upstream API still gets queried for everything**. For high-volume sources (HubSpot with 100k contacts, Salesforce with 500k records, Notion with 10k pages), this is wasteful and rate-limit-prone.

This phase extends the existing `com.bytechef.component.definition.datastream.ItemReader` SPI with an optional capability flag so individual readers can opt into incremental syncing without breaking existing readers that don't support it. The DESTINATION cluster elements (`contextStore.writeToReplica`, `knowledgeBase.writeAsDocument`) and the listeners (`ContextStoreSyncJobListener`, `KnowledgeBaseSourceSyncJobListener`) **don't change** — incremental affects only what the SOURCE reader emits.

**Spec reference**: §16 decision log entry "ItemReader.supportsIncremental() capability flag (post-MVP DataStream SPI extension)" + new entry "Phase 17 implementation picks (2026-05-09)" + §3 out-of-scope explainer.

**Phase 17 design decisions (2026-05-09)** — locked in before implementation. All five picks went with option (a):

1. **Where `since` lives in the contract**: reader reads from `ExecutionContext.getString(ItemReader.SINCE_KEY)` itself; new SPI capability is just `default boolean supportsIncremental() { return false; }` + a well-known `String SINCE_KEY = "datastream.since"` constant. Binary-compatible. No new method signatures on `ItemStream.open(...)`.
2. **Who writes `lastSyncStart` and when**: `DataStreamJobExecutionListener.afterJob` writes the run's start instant into the JobExecution's `ExecutionContext` on every COMPLETED run. Cheap, defensive, useful even for non-incremental readers (queryable via `JobExplorer` for forward compatibility with Phase 17b orchestrator auto-wiring).
3. **Pilot scope**: Airtable only. Real upstream API + native `filterByFormula` predicate makes this a clean proof. CSV/JSON `lastModifiedField` is a separate UX/feature; punted.
4. **First-run behavior**: null = first run = full pull. Reader's `open()` checks `executionContext.getString(SINCE_KEY, null)` and falls back to full-pull semantics when absent. No epoch-zero seeding.
5. **Auto-wire incremental into CS/KB-Source orchestration**: NO. SPI ships clean, Airtable proves end-to-end via the IntTest's manual `JobParameter` injection. CS/KB-Source workflows continue to default to FULL_REPLACE every run; users opt in by editing workflow YAML manually. Auto-wiring (extending the workflow generator + adding cadence-pair UI) is deferred to Phase 17b once we have user feedback.

**Six tasks** at this phase:

- **Task 46**: Extend `ItemReader` SPI in `sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/datastream/ItemReader.java`:
  - Add `String SINCE_KEY = "datastream.since"` constant. Convention: value is the **previous run's start time as epoch milliseconds** (Long), stored in the `ExecutionContext` under this key. Readers fetch it via `executionContext.get(SINCE_KEY, Long.class).orElse(null)` and convert to `Instant.ofEpochMilli(...)` if needed.
  - Add `default boolean supportsIncremental() { return false; }` so non-opt-in readers stay at full-pull.
  - Javadoc: explain the contract (read SINCE_KEY in `open()`, return `true` from `supportsIncremental()` only if the reader actually filters upstream by `since` — the orchestrator may use the flag to decide whether to inject `since` from prior state).
  - Binary-compatible: no signature changes on existing methods. Spotless + checkstyle clean.

- **Task 47**: Plumb the `since` JobParameter into the reader's ExecutionContext via `ItemStreamReaderDelegate`:
  - `data-stream.stream` action's `DataStreamStreamActionDefinition` already reads JobParameters at perform time. The new orchestrator-side hook: if the orchestrator passes a JobParameter named `datastream.since` (Long, epoch millis), the delegate's `doBeforeStep(StepExecution)` reads it via `stepExecution.getJobParameters().getLong("datastream.since")` and copies into the per-step `ExecutionContext` under `ItemReader.SINCE_KEY` before calling `itemReader.open(executionContext)`.
  - This keeps readers reading purely from ExecutionContext (clean SPI surface) while orchestrators inject state via JobParameters (clean orchestration). The delegate is the only place that knows about both sides of the wire.
  - For the non-orchestrated path (user-defined workflow that just chains a SOURCE → DESTINATION), nothing changes — no `datastream.since` JobParameter, no copy, reader sees no SINCE_KEY in ExecutionContext.

- **Task 48**: `DataStreamJobExecutionListener.afterJob` writes `lastSyncStart` (= `jobExecution.getStartTime().toInstant().toEpochMilli()`) into `jobExecution.getExecutionContext()` under `ItemReader.SINCE_KEY` on `BatchStatus.COMPLETED`. Spring Batch's `JobRepository` persists the JobExecution ExecutionContext, so `JobExplorer` lookups across runs can read it back even when each cron run is a fresh JobInstance (Phase 17b will use this for orchestrator auto-wiring; Phase 17 just lays the groundwork).

- **Task 49**: Convert `AirtableItemReader` (`server/libs/modules/components/airtable/src/main/java/com/bytechef/component/airtable/datastream/AirtableItemReader.java`) to incremental:
  - Override `supportsIncremental()` → `true`.
  - Add a new optional input parameter `lastModifiedFieldName` (Airtable column holding the last-modified timestamp, e.g. `"Last Modified Time"`). When absent, reader still works (full-pull) — opt-in.
  - In `open()`: read `since` from `executionContext.get(SINCE_KEY, Long.class).orElse(null)`. If `since != null` AND `lastModifiedFieldName != null`, append `filterByFormula=IS_AFTER({<lastModifiedFieldName>}, '<ISO-8601 since>')` to the Airtable list-records request.
  - Update the cluster element definition to advertise the new property.
  - Existing `AirtableItemReaderTest` extended with cases: (a) supportsIncremental returns true, (b) full-pull when `since` absent, (c) filterByFormula appended when both `since` and `lastModifiedFieldName` are set, (d) full-pull when only `lastModifiedFieldName` is set (no `since` yet — first run).

- **Task 50**: IntTest in `server/libs/modules/components/data-stream/src/test/java/com/bytechef/component/datastream/`:
  - `DataStreamIncrementalReadIntTest.java` — uses Spring Batch in-memory infrastructure (mirrors existing DataStream IntTest pattern). Stub `ItemReader` that emits 3 records on first read, 1 record on second read (filtered by `since`); stub `ItemWriter` that captures emitted records.
  - Run job once with no `datastream.since` JobParameter → assert reader's `open()` saw `executionContext.getString(SINCE_KEY, null) == null`, all 3 records emitted.
  - Stash the JobExecution's `executionContext.get(SINCE_KEY)` value (set by Task 48's listener).
  - Run job again with `datastream.since` JobParameter set to the stashed value → assert reader's `open()` saw the previous-run timestamp, only 1 record emitted.

- **Task 51**: Spec + plan updates after implementation:
  - Spec §16 decision-log entry "Phase 17 implementation picks (2026-05-09)" capturing the 5 design decisions.
  - Spec §3 "Explicitly out of scope" → tighten the "incremental sync" entry to mention "SPI shipped in Phase 17 — orchestrator auto-wiring (CS/KB-Source workflow generator + cadence-pair UI) deferred to Phase 17b."
  - Plan: this section's bullets get marked with their landed commit SHAs.
  - Phase 17 self-review checklist appended.

**What Phase 17 explicitly does NOT ship** (deferred to Phase 17b):
- Auto-wiring of `datastream.since` into the auto-generated CS/KB-Source workflows. Users who want incremental have to edit their workflow YAML manually to add the JobParameter (sourcing from `source.lastSyncRunAt` via a small SpEL expression — workflow YAML already supports this pattern via `${...}` syntax).
- Cadence-pair UI (paired hourly-incremental + daily-FULL_REPLACE triggers in the create-source dialog).
- Integration into CS-Source / KB-Source listeners (they continue to maintain `lastSyncRunAt` on the source row, which is the source of truth for orchestrator-side incremental scheduling).

**Tombstone interaction** (unchanged from earlier framing): incremental sync alone cannot derive tombstones. Phase 17b's auto-wiring will choose between (1) periodic FULL_REPLACE on a longer cadence + hourly incremental, or (2) upstream change-feed events. Both options are connector-specific and orthogonal to the SPI extension landed in Phase 17.

### Phase 17 review checklist

- [x] `ItemReader.SINCE_KEY` constant + `default boolean supportsIncremental()` added; binary-compatible (no existing method signature changed). Commit `6c8960e2f78`.
- [x] `ItemStreamReaderDelegate.doBeforeStep` reads `datastream.since` JobParameter and copies into per-step ExecutionContext under `ItemReader.SINCE_KEY`. No copy when JobParameter is absent. Commit `3fc66c076a3` (bundled with listener change so `SINCE_KEY` references compile together).
- [x] `DataStreamJobExecutionListener.afterJob` writes `lastSyncStart` into JobExecution's ExecutionContext on COMPLETED. Listener is silent on non-COMPLETED states (no overwrite). Commit `3fc66c076a3`. Spring Batch 6's `JobExecution.getStartTime()` returns `LocalDateTime`; converted via `toInstant(ZoneOffset.UTC).toEpochMilli()`.
- [x] `AirtableItemReader` opts in: `supportsIncremental()=true`, `lastModifiedFieldName` optional input parameter, `filterByFormula` appended only when both `since` AND `lastModifiedFieldName` are present. Commit `80b403b97e4`. Snapshot `airtable_v1.json` regenerated for new property.
- [x] Unit tests cover: SPI default `supportsIncremental()=false`; delegate copy behavior (with/without JobParameter); listener write-on-COMPLETED + skip-on-FAILED + skip-on-null-startTime; Airtable's 4 cases. Commits `80b403b97e4` (Airtable tests) + `7a13a81c2a5` (delegate + listener tests).
- [x] Round-trip test `DataStreamIncrementalReadTest` runs end-to-end and passes. Commit `7a13a81c2a5`. **Scope reduction**: named `Test` not `IntTest` because the existing `DataStreamComponentHandlerIntTest` is `@Disabled` with no working precedent for full Spring DI wiring; the test still drives a real Spring Batch `JobLauncher` with `ResourcelessJobRepository` + a `SinceCopyingReader` adapter that mirrors what `ItemStreamReaderDelegate.open` does in production. Documented inline in test javadoc.
- [x] No existing reader broke (binary compat verified — `:server:libs:modules:components:airtable:check` + `:server:libs:modules:components:data-stream:check` clean).
- [x] Spec + plan updated with implementation decisions + commit SHAs (this commit).

### What Phase 17 explicitly did NOT ship (scheduled to Phase 17b)

- Auto-wiring of `datastream.since` into the auto-generated CS / KB-Source workflows. Users opt in by editing workflow YAML manually to add the JobParameter (sourcing from `source.lastSyncRunAt` via SpEL).
- Cadence-pair UI (paired hourly-incremental + daily-FULL_REPLACE triggers in the create-source dialog).
- Tombstone-derivation strategy choice for incremental orchestration (periodic FULL_REPLACE on a longer cadence vs upstream change-feed events).
- Other connector pilots beyond Airtable (HubSpot, Salesforce, Notion ship `supportsIncremental() = true` from day 1 when they're added).

---

## Phase 18: KB-Source `metadataFields` whitelist — parity with CS `stored_fields` (post-MVP, separate plan)

Phase 13's KB-Source design intentionally **omits** the field-whitelist mechanism that CS exposes via `ContextStoreEntity.stored_fields`. Today, `KnowledgeBaseDocumentServiceImpl.metadataToTagNames(metadata)` flattens the *entire* incoming `record` Map to `key=value` tag strings on the KB document. That's MVP behavior — every reader-emitted field becomes a tag. For high-cardinality records (HubSpot contacts have 90+ properties), this produces huge tag lists nobody benefits from.

This phase adds a `metadataFields` JSONB whitelist to `KnowledgeBaseSource` mirroring CS's `stored_fields`. Synced KB documents carry only the whitelisted metadata fields as tags; everything else is dropped at write time.

**Three tasks** at this phase (not detailed here; spelled out when scheduled):

- **Task 51**: Add `metadata_fields JSONB` column to `knowledge_base_source`. Per the "do not add liquibase migration, update existing files" rule established earlier, edit the existing changeset on `platform-knowledge-base-service`'s init Liquibase in place (the branch is unmerged when this phase scopes, so destructive in-place edits are fine; if the branch has merged by then, this phase ships its own follow-up changeset). Add `metadataFields MapWrapper` (nullable) to `KnowledgeBaseSource` Java entity + getter/setter in the same dual-MapWrapper pattern CS uses for `stored_fields`. `null` means "include all metadata as tags" per current MVP behavior; `{fields: [...]}` means "only these fields become tags."

- **Task 52**: Modify `KnowledgeBaseDocumentServiceImpl.createSyncedDocument` and `replaceSyncedDocument` to accept the whitelist + apply it before flattening to tags. The signatures gain `@Nullable Map<String, ?> metadataFieldsWhitelist` (or read it from the source row inside the method to keep signatures stable; either works). The `metadataToTagNames` helper becomes `applyWhitelistAndFlatten(metadata, whitelist)`. Update `KnowledgeBaseItemWriter` to read `source.getMetadataFields()` at `open()` and pass it through.

- **Task 53**: Update GraphQL surface (`KnowledgeBaseSourceGraphQlController`) — `Create/UpdateKnowledgeBaseSourceInput` gains `metadataFields: JSON`. Update Phase 15 UI's "Add Knowledge Base Source" dialog to include a `metadataFields` step (multi-select tag input populated from a discovery query against the source component's reader's `getFields()` — same pattern CS uses for the `storedFields`/`indexedFields` editors).

**Backward compatibility**: existing KB-Source rows have `metadata_fields = NULL` after this phase lands; the whitelist code path interprets null as "include everything" (current MVP behavior preserved). Workspaces that want narrower metadata flip the whitelist on later via `updateKnowledgeBaseSource` — no migration of existing synced documents (they keep their existing tag set; only future writes apply the new whitelist; a separate "rewrite tags on existing docs" admin operation can be added if needed).

**Future enhancement (separate plan)**: replace the tag-string flattening with a proper `metadata JSONB` column on `knowledge_base_document`, so synced metadata can be queried structurally (not just via tag containment). That's a bigger schema change touching the KB chunker's metadata model, intentionally out of scope here. The whitelist alone gets ~80% of the value (drop noise) without committing to the bigger refactor.

---

## Phase 19: Public REST API for Context Store (post-MVP, separate plan)

Per spec §11. A workspace-scoped REST API that exposes the same per-(source, entity) read/write/lifecycle surface external clients need without requiring MCP. Mirrors `automation-ai-gateway-public-rest` exactly: API key auth, workspace-scope enforcement on every request, OpenAPI 3.0 spec, SpringDoc-rendered Swagger UI, EE-tier (paid product surface — same positioning as Airbyte's HTTP API).

**Why deferred (per spec §11.5):** the MVP backend ships internal use cases (workflow steps, AI Agent component, AiHub, MCP server). External REST is additional surface area without new use cases — it's a product/integration layer, not a foundational layer. Worth its own phase so it ships after the v1 schema settles and after the per-(source, entity) callback shape has months of real CC/MCP usage to validate it.

**Spec reference:** §11 (full endpoint surface + module placement + auth + OpenAPI + rationale) + §16 decision-log entry "Public REST API as a post-MVP EE phase under `automation-context-store-public-rest`".

### What lands in the MVP to avoid future migrations (already done)

Per spec §11.6, the MVP backend was designed with REST consumption in mind so this phase is a thin adapter:
- **No tables added by this phase.** All endpoints sit on top of existing services (`ContextStoreQueryService`, `ContextStoreSourceService`, `ContextStoreEntityService`, `WorkspaceContextStoreSourceFacade`, `ContextStoreToolFacade`, `ContextStoreSemanticSearchService`).
- **Service interfaces already return clean DTOs** (`ContextStoreSearchResult`, `ContextStoreRecord`, etc.) with no JPA-entity leakage. GraphQL had the same constraint, so this came for free.
- **Workspace authorization is already upstream of the platform query service** (post-2026-05-09 workspace-logic-out-of-platform refactor). The REST controller just resolves the API key's workspace scope, validates the path's `{workspaceId}` matches, and delegates.

### Module placement

```
server/ee/libs/automation/automation-context-store/
  automation-context-store-public-rest/
    build.gradle.kts                                    ← deps: automation-context-store-api,
                                                              automation-context-store-tool-api,
                                                              platform-public-rest-shared (auth + rate limit),
                                                              springdoc-openapi-starter-webmvc-ui
    src/main/java/com/bytechef/ee/automation/contextstore/web/rest/
      config/
        ContextStorePublicRestConfiguration.java        ← @Configuration; SecurityFilterChain for
                                                              /api/v1/workspaces/**/context-store/**
      controller/
        ContextStoreSourceRestController.java           ← source CRUD + lifecycle
        ContextStoreEntityRestController.java           ← entity CRUD (per Phase 15 Task 36a additions)
        ContextStoreSearchRestController.java           ← POST /search (the headline endpoint)
        ContextStoreRecordRestController.java           ← single-record GET by natural key
        ContextStoreSyncRestController.java             ← refresh + sync-runs history
        ContextStoreToolRestController.java             ← GET /tools — enumeration for non-MCP agents
        ContextStoreSemanticSearchRestController.java   ← @ConditionalOnBean(ContextStoreSemanticSearchService.class)
      dto/...                                           ← request/response Java records (no JPA leakage)
      mapper/
        ContextStoreRestMapper.java                     ← MapStruct: domain ↔ REST DTO
      exception/
        ContextStoreRestExceptionHandler.java           ← @RestControllerAdvice; maps service exceptions to ProblemDetail
    src/main/resources/com/bytechef/automation/contextstore/openapi.yaml
    src/test/java/...                                   ← MockMvc IntTests + smoke OpenAPI compatibility test
```

Mirrors `server/ee/libs/automation/automation-ai-gateway/automation-ai-gateway-public-rest/` package layout exactly. Read that module first when scoping; copy the auth/rate-limit/exception-handler patterns verbatim.

### Endpoint surface (verbatim from spec §11)

```
# Sources (admin-only on writes via @PreAuthorize)
GET    /api/v1/workspaces/{workspaceId}/context-store/sources
POST   /api/v1/workspaces/{workspaceId}/context-store/sources
GET    /api/v1/workspaces/{workspaceId}/context-store/sources/{sourceId}
PATCH  /api/v1/workspaces/{workspaceId}/context-store/sources/{sourceId}
DELETE /api/v1/workspaces/{workspaceId}/context-store/sources/{sourceId}
PATCH  /api/v1/workspaces/{workspaceId}/context-store/sources/{sourceId}/enabled
       body: {enabled: boolean}

# Entities (admin-only on writes)
GET    /api/v1/workspaces/{workspaceId}/context-store/sources/{sourceId}/entities
POST   /api/v1/workspaces/{workspaceId}/context-store/sources/{sourceId}/entities
GET    /api/v1/workspaces/{workspaceId}/context-store/sources/{sourceId}/entities/{entityId}
PATCH  /api/v1/workspaces/{workspaceId}/context-store/sources/{sourceId}/entities/{entityId}
DELETE /api/v1/workspaces/{workspaceId}/context-store/sources/{sourceId}/entities/{entityId}

# Read (the headline)
POST   /api/v1/workspaces/{workspaceId}/context-store/search
       body: {sourceId, entity, filters[], sort[], limit, cursor, includeDeleted, fields[]}
       → returns ContextStoreSearchResult { items[], nextCursor }
GET    /api/v1/workspaces/{workspaceId}/context-store/records/{sourceId}/{entity}/{sourceRecordId}
       → returns ContextStoreRecord (full payload + metadata)

# Tool discovery (mirrors Airbyte's context_store_search MCP-shaped enumeration)
GET    /api/v1/workspaces/{workspaceId}/context-store/tools
       → returns the same per-(source, entity) FunctionToolCallback shapes the MCP server enumerates,
         so external agents using HTTP rather than MCP get the same typed-tool list

# Sync ops (admin-only)
POST   /api/v1/workspaces/{workspaceId}/context-store/sources/{sourceId}/refresh
       → returns Atlas JobExecution metadata (status + workflow execution id)
GET    /api/v1/workspaces/{workspaceId}/context-store/sources/{sourceId}/sync-runs?limit=
       → returns recent JobExecution rows for the source's auto-generated workflow

# Optional semantic (only present when @ConditionalOnBean(ContextStoreSemanticSearchService.class))
POST   /api/v1/workspaces/{workspaceId}/context-store/semantic-search
       body: {sourceId, entity, query, k, filters[]}
       → returns [{record, similarityScore}, ...]
```

### Auth & rate limiting

- **API key auth** via the existing `platform-public-rest-shared` mechanism (same as `automation-ai-gateway-public-rest`). Keys are workspace-scoped.
- The `{workspaceId}` path segment must match the API key's scope or returns **403 Forbidden** (handled by a `WorkspaceScopeFilter` in the security config — copy from the AI gateway).
- **Per-key rate limits** via the same machinery (`@RateLimited` annotation or filter, depending on what the AI gateway uses).
- **Admin-only endpoints** use `@PreAuthorize("hasRole('ROLE_ADMIN')")` checked against the API key's principal role (writes + sync ops).

### Tasks

Twelve tasks, starting at Task 54 to avoid collision with Phase 17's 46-51 and Phase 18's sketched 51-53 (which renumber when Phase 18 actually scopes):

- **Task 54**: Module scaffolding. Create `automation-context-store-public-rest/build.gradle.kts` with deps (`automation-context-store-api`, `automation-context-store-tool-api`, `platform-public-rest-shared`, `springdoc-openapi-starter-webmvc-ui`); register in `settings.gradle.kts`; package directory tree; empty `openapi.yaml`. Verify the module compiles standalone.

- **Task 55**: API key auth + workspace scope filter. Copy `automation-ai-gateway-public-rest`'s `SecurityFilterChain` config bean verbatim, swap in the path pattern `/api/v1/workspaces/**/context-store/**`. Wire the `WorkspaceScopeFilter` (mirrors the AI gateway's). IntTest: a valid key for workspace 1 hitting `/workspaces/2/context-store/sources` returns 403.

- **Task 56**: `ContextStoreSourceRestController` — list/get/create/patch/delete + setEnabled. Delegates entirely to `WorkspaceContextStoreSourceFacade`. Request/response DTOs in `dto/source/`. MapStruct mapper for domain ↔ DTO. Admin-only `@PreAuthorize` on writes. MockMvc IntTest covering all 6 operations + 403/401 negative cases.

- **Task 57**: `ContextStoreEntityRestController` — list/get/create/patch/delete. Delegates to `ContextStoreEntityService` (per Phase 15 Task 36a's GraphQL-side mutations — REST surfaces the same operations). Admin-only on writes. MockMvc IntTest.

- **Task 58**: `ContextStoreSearchRestController` — `POST /search`. The headline endpoint. Request body maps 1:1 to `ContextStoreQuery`; response is `ContextStoreSearchResult`. Delegates to `ContextStoreQueryService.search(query)`. **No special auth** beyond the workspace scope check (read access for any valid API key in the workspace). IntTest covers: successful search, cursor pagination round-trip, filter shapes, includeDeleted, sort, fields projection.

- **Task 59**: `ContextStoreRecordRestController` — single-record GET by `(sourceId, entity, sourceRecordId)` natural key. Delegates to `ContextStoreQueryService.get(...)`. Returns 404 when not found. IntTest.

- **Task 60**: `ContextStoreSyncRestController` — refresh-now + sync-runs history. Refresh delegates to `WorkspaceContextStoreSourceFacade.refreshNow(workspaceId, sourceId)`; sync-runs queries Spring Batch `JobExplorer` via the same path the future Phase 17b orchestrator auto-wiring will use. Admin-only. IntTest.

- **Task 61**: `ContextStoreToolRestController` — `GET /tools`. Returns the per-(source, entity) `FunctionToolCallback` JSON shape that the EE MCP server enumerates. Delegates to `ContextStoreToolFacade.getFunctionToolCallbacks(workspaceId)` + `getSemanticFunctionToolCallbacks(workspaceId)` and serializes to a stable JSON shape. **This is what unblocks external agents that don't speak MCP.** IntTest covers: empty workspace returns `[]`; populated workspace returns the right callback names + input schemas.

- **Task 62**: `ContextStoreSemanticSearchRestController` — `POST /semantic-search`. `@ConditionalOnBean(ContextStoreSemanticSearchService.class)`; absent in CE-no-embedding deployments. Delegates to `ContextStoreSemanticSearchService.searchSemantic(...)`. IntTest with a stub `EmbeddingModel`.

- **Task 63**: OpenAPI spec authoring at `src/main/resources/com/bytechef/automation/contextstore/openapi.yaml`. Hand-written YAML (NOT generated from controllers — the explicit spec is the source of truth and the controllers must conform). Verify SpringDoc renders Swagger UI at `/swagger-ui/index.html` correctly. Add `OpenApiCompatibilityIntTest` that loads the YAML, walks every operation, and asserts the controller has a matching `@RequestMapping` (mirrors `automation-ai-gateway-public-rest`'s test pattern).

- **Task 64**: Exception mapping. `ContextStoreRestExceptionHandler` (`@RestControllerAdvice`) maps service-layer exceptions to `ProblemDetail` (RFC 7807). Map `ContextStoreSourceNotFoundException` → 404, `IllegalArgumentException` → 400, etc. Mirror what the AI gateway does. IntTest covers each mapping.

- **Task 65**: Client SDK regen + smoke. Run the existing `sdks/typescript/` and `sdks/python/` codegen pipelines pointed at the new OpenAPI spec; verify they produce sensible client code. **Don't publish** — that's a separate ops decision. Document in the module README how to publish when ready.

### Phase 19 review checklist

- [ ] Module `automation-context-store-public-rest` exists; `./gradlew :server:ee:libs:automation:automation-context-store:automation-context-store-public-rest:check` clean.
- [ ] Every endpoint in spec §11 has a matching controller method + `@RequestMapping`. `OpenApiCompatibilityIntTest` confirms parity.
- [ ] API key auth enforced; `WorkspaceScopeFilter` returns 403 when path workspace ≠ key workspace.
- [ ] All write endpoints (`POST`/`PATCH`/`DELETE`/`/refresh`/`/enabled`) require `ROLE_ADMIN`; non-admin keys get 403.
- [ ] `POST /search` round-trip works against a populated source: filter, sort, pagination, includeDeleted, fields projection all behave as `ContextStoreQueryService.search` does.
- [ ] `GET /tools` returns the same callback enumeration the MCP server produces (verify shape parity by comparing JSON output against `ContextStoreToolFacade.getFunctionToolCallbacks(workspaceId)` directly).
- [ ] `POST /semantic-search` is **absent** when no `EmbeddingModel` bean is loaded (CE smoke test); **present** when one is.
- [ ] Rate limiting middleware applied; per-key rate limits visible in test response headers.
- [ ] OpenAPI YAML at the documented path; SpringDoc renders Swagger UI at `/swagger-ui/index.html`.
- [ ] Exception mapping: service exceptions → ProblemDetail JSON; no stack traces leak to clients.
- [ ] Generated TypeScript + Python client SDKs build cleanly from the OpenAPI spec.
- [ ] No tables added (verified by grep — `automation-context-store-public-rest/src/main/resources/db/changelog/` does not exist).
- [ ] Spec §11 + §16 decision log updated post-implementation with commit SHAs (the existing §11 already documents the design; Task 65 just appends the "shipped" note).

### What Phase 19 explicitly does NOT ship (deferred)

- **KB-Source public REST** (call it Phase 19b): identical shape over `WorkspaceKnowledgeBaseSourceFacade` + `KnowledgeBaseDocumentService`. Same module pattern, same auth, same OpenAPI conventions. Defer until at least one user asks for HTTP access to KB-Source operations beyond what the existing `platform-knowledge-base-rest` (internal, not workspace-scoped) covers. Estimated 8 tasks; mostly mechanical once Phase 19 lands.
- **Webhook ingestion endpoints** (e.g., `POST /context-store/ingest` for upstream change-feed events): valuable but tangential to read access. Phase 17b's tombstone-derivation strategy will need to choose between cron-driven incremental + this push-driven path; probably bundled into 17b's scope.
- **Bulk operations** (e.g., `POST /search/bulk` taking multiple queries in one request): nice-to-have for high-throughput external agents but not needed for v1. Add when a real user asks.
- **Webhooks for sync events** (e.g., notify external service when sync completes): different mental model from REST; deferred to a separate "platform webhooks" phase that handles all platform-event subscriptions uniformly.

---

## Phase 20: Drop LIST_ACTION + Properties renderer in Add Source wizard (shipped)

A pair of follow-up corrections after Phase 15 went live:

1. **`LIST_ACTION` reader strategy was half-shipped**. The enum value, column, workflow-generator branch, and GraphQL discovery all existed, but no runtime adapter ever wrapped a `list*` action invocation into the Spring Batch `ItemReader` lifecycle. Picking `LIST_ACTION` in the wizard would succeed at create time but crash at first sync run. Choice: **drop entirely** rather than build the missing adapter (re-add when there's a real second strategy).

2. **The Add CS Source wizard's Entities step never captured SOURCE input parameters**. Free-text Entity Name + free-text ID Field + free-text Indexed Fields name. No way for the user to configure which Airtable base/table or HubSpot object type to sync — the workflow generator received an empty input map and the sync would fail on first run for any reader with required input properties.

Fix: replace text inputs with the existing `<Properties>` renderer (mounted via `<WorkflowMockProvider>`, the same workflow-less pattern `ConnectionDialog` and `IntegrationInstanceConfigurationDialog` use) for SOURCE cluster element properties; replace free-text ID Field + Indexed Fields name with `<Select>` dropdowns sourced from a new workflow-less `clusterElementFields` GraphQL query that mirrors the existing workflow-less `clusterElementOptions` query but invokes `FieldsProvider.getFields(...)` instead of an `OptionsFunction`. **No new SPI** — the existing `FieldsProvider.getFields()` (already on `ItemReader`) is sufficient.

### Part A — drop `LIST_ACTION` + simplify wizard

**Backend** (commit `de4a9c4cf90`):
- Deleted `ReaderStrategy.java` from CS (`platform-context-store-api`) and KB-Source (`platform-knowledge-base-api`).
- Dropped `readerStrategy` + `sourceListActionName` fields from `ContextStoreSource` + `KnowledgeBaseSource` entities.
- Dropped `reader_strategy` + `source_list_action_name` columns from both Liquibase init changesets (in-place; the branch is unmerged).
- Dropped LIST_ACTION branch from `ContextStoreWorkflowGenerator` + `KnowledgeBaseSourceWorkflowGenerator`. Workflow generator is now a pure JSON-shape function.
- Added auto-pick of the first `ItemReader` cluster element on `WorkspaceContextStoreSourceFacadeImpl.create(...)` and `WorkspaceKnowledgeBaseSourceFacadeImpl.create(...)` when `sourceClusterElementName` is null. Throws `IllegalArgumentException("Component <name> v<version> has no ItemReader cluster element")` when none exists. Service-side; clean separation from the JSON builder.
- Dropped `ContextStoreReaderStrategy` + `DataStreamReaderStrategy` GraphQL enum types and `supportedReaderStrategies` field from `DataStreamCompatibleConnection`. `dataStreamCompatibleConnections` resolver now filters by `ItemReader` cluster element presence only.
- Dropped `readerStrategy` and `sourceListActionName` from `CreateContextStoreSourceInput` / `CreateKnowledgeBaseSourceInput` (facade DTO + GraphQL DTO).
- Updated `EnumOrdinalStabilityTest` for both modules to remove the `ReaderStrategy` assertions.
- Updated `CreateContextStoreSourceToolCallback` (CC EE) — dropped `readerStrategy` from input schema + DTO + validation; `sourceClusterElementName` now optional.

**Frontend** (commit `949f12e3e53`):
- Dropped Reader Strategy step from `AddContextSourceDialog` (CS wizard now 4 steps: Connection → Entities → Cadence → Review) and from `AddKnowledgeBaseSourceDialog` (KB-Source wizard now 3 steps: Connection → Cadence → Review).
- Dropped 5 `.graphql` operation references to `readerStrategy` / `sourceListActionName`. Codegen regenerated.
- Updated test fixtures to drop strategy field assertions.

### Part B — Properties renderer + workflow-less `clusterElementFields` query

**Backend** (commit `850d178b6c1`):
- New domain DTO `Field` at `platform-component-api/.../domain/Field.java` (record: `name`, `label`, `type` — Java class simple name).
- New service method `ClusterElementDefinitionService.executeFields(componentName, componentVersion, clusterElementName, inputParameters, connectionIds): List<Field>`. Implementation in `ClusterElementDefinitionServiceImpl` mirrors `doExecuteOptions(...)`: convert helper builds parameters + context, instance is cast to `FieldsProvider`, `getFields(...)` is called. Returns empty list when the cluster element does not implement `FieldsProvider` (graceful fallback for the wizard).
- New facade method on `ClusterElementDefinitionFacade` + Impl — thin delegate.
- New error type `ClusterElementDefinitionErrorType.EXECUTE_FIELDS`.
- New GraphQL controller `ClusterElementFieldGraphQlController` exposing `clusterElementFields(componentName, componentVersion, clusterElementName, connectionId, inputParameters): [Field!]!`.
- 4 new test cases covering FieldsProvider present / absent / failure paths.

**Codegen regen** (commit `b20e1d8a627`):
- New `.graphql` operation file `clusterElementFields.graphql`.
- `client/src/shared/middleware/graphql.ts` regenerated; `useClusterElementFieldsQuery` now exported.

**Wizard rewiring** (commit `f1bb2d8897b`):
- Extracted `EntityRow` subcomponent to keep `AddContextSourceDialog` readable.
- Mounted `<WorkflowMockProvider><Properties .../></WorkflowMockProvider>` for the SOURCE cluster element's input properties when present (Airtable BASE_ID/TABLE_ID, HubSpot OBJECT_TYPE, etc. all rendered automatically with their existing `OptionsFunction`-driven dropdowns).
- ID Field: conditional `<Select>` using `useClusterElementFieldsQuery` keyed on `(componentName, componentVersion, clusterElementName, connectionId, watched entity parameters)`. Refetches automatically when source-config inputs change. Falls back to free-text `<Input>` when the query returns empty.
- Indexed Fields: `IndexedFieldsEditor` accepts new optional `availableFields` prop. When provided, name field becomes `<Select>`; type selector auto-prefills from the chosen field's Java type (`String` → `TEXT`; `Long`/`Integer`/`Double`/`Float` → `NUMERIC`; `Instant`/`LocalDateTime`/`LocalDate` → `TIMESTAMP`; anything else → `TEXT`). User can override.
- Hoisted single `useForm<{entityParameters: Record<number, ...>}>` to the dialog level. Initial attempt to sync watched parameters back into `useState` via `useEffect` caused infinite re-renders (TDZ-adjacent pitfall flagged by CLAUDE.md); switched to read-only `useWatch` + `getValues()` at submit time. `EntityDraftI` no longer carries `parameters` field — they live in the form.
- Cluster-element name resolution client-side: `useGetComponentDefinitionQuery` + `clusterElements.find(e => e.type === 'SOURCE')`, mirroring the server-side `resolveSourceClusterElementName(...)` from Part A.
- 5 new test cases covering: Properties renders SOURCE properties; ID Field is `<Select>` when fields query returns data; ID Field falls back to `<Input>` when empty; IndexedFieldsEditor uses Select with auto-prefilled type; submit-via-fallback path. (One submit-via-Select-path test had to use the fallback path because Radix Select's `hasPointerCapture` isn't polyfilled in jsdom — known limitation.)

### Phase 20 review checklist

- [x] `ReaderStrategy` enum + `LIST_ACTION` deleted everywhere; in-place Liquibase column drop; `EnumOrdinalStabilityTest` updated.
- [x] Workflow generators dropped the LIST_ACTION branch; auto-pick first `ItemReader` cluster element when `sourceClusterElementName` is null (in the facade, not the generator — preserves generator as pure JSON builder).
- [x] `dataStreamCompatibleConnections` filters by `ItemReader` presence only; `supportedReaderStrategies` field + `DataStreamReaderStrategy` enum gone.
- [x] Wizards collapsed: CS → 4 steps, KB-Source → 3 steps.
- [x] `<WorkflowMockProvider><Properties properties={clusterElementDefinition.properties}>` mounts in CS wizard's Entities step. SOURCE-property dropdowns (Airtable BASE_ID/TABLE_ID, etc.) work via existing workflow-less `clusterElementOptions` query — no new endpoint needed.
- [x] New `clusterElementFields` GraphQL query exposes `FieldsProvider.getFields(...)` for ID Field + Indexed Fields name dropdowns. Returns empty list when component doesn't implement FieldsProvider.
- [x] `IndexedFieldsEditor` `availableFields` prop drives Select rendering + type auto-prefill; falls back to free-text input.
- [x] `entity.parameters` threads from the wizard's hoisted form through `CreateContextStoreEntityInput` to `ContextStoreWorkflowGenerator.buildSourceClusterElement(...)` (the workflow-generator path was already wired in Phase 15; verified end-to-end).
- [x] Backend `:check` clean across all touched modules; client `npm run check` clean.

### What Phase 20 explicitly did NOT ship

- **No new SPI on `ItemReader`** (an earlier draft proposed an `EntityProvider` SPI; rejected after the user pointed out the existing `<Properties>` + `clusterElementOptions` workflow-less pattern handles it).
- ~~**No KB-Source dialog Properties rewiring**~~ — **shipped as Phase 20b** (see below).
- **No re-pilot of incremental sync** through the new wizard. Phase 17's Airtable pilot uses the workflow YAML directly; UI integration of `supportsIncremental()` discovery comes with Phase 17b's orchestrator auto-wiring.

### Phase 20b — KB-Source dialog parity (shipped)

Same `<Properties>` + `<WorkflowMockProvider>` rewiring applied to `AddKnowledgeBaseSourceDialog`. KB-Sources can now configure SOURCE cluster element input parameters (Airtable BASE_ID/TABLE_ID, HubSpot OBJECT_TYPE, etc.) — closing the gap where KB-Source sync would crash on first run for any reader with required input properties.

Smaller scope than Phase 20 Part B because KB-Source has no per-entity layer:
- Source-level `parameters` (single field on `KnowledgeBaseSource`), not per-entity (no `useFieldArray`)
- KB-Source documents are opaque blobs → no ID Field / Indexed Fields → no `clusterElementFields` query reuse
- One `useForm<{sourceParameters: Record<string, unknown>}>` at the dialog level

**Backend** (commit `2bd32c62682`):
- New `parameters MapWrapper` (nullable) field on `KnowledgeBaseSource` entity, mirroring `ContextStoreEntity.parameters` shape. `commons-data` dep added to `platform-knowledge-base-api/build.gradle.kts` (the `MapWrapper` class).
- Liquibase init changeset extended with `parameters JSONB` column on `knowledge_base_source` (in-place edit; the branch is unmerged).
- `CreateKnowledgeBaseSourceInput` (facade DTO) + `CreateKnowledgeBaseSourceGraphQlInput` + GraphQL `CreateKnowledgeBaseSourceInput` schema gained `parameters: Map` (nullable).
- `WorkspaceKnowledgeBaseSourceFacadeImpl.create(...)` calls `sourceToInsert.setParameters(input.parameters())`.
- `KnowledgeBaseSourceWorkflowGenerator.buildSourceClusterElement(...)` replaces the unconditional empty `LinkedHashMap` with `source.getParameters() != null ? new LinkedHashMap<>(source.getParameters()) : new LinkedHashMap<>()`.
- `KnowledgeBaseIntTestConfiguration` registered `MapWrapperToPGObjectConverter` + `PGobjectToMapWrapperConverter` (KB had no prior MapWrapper-mapped field, so no auto-wired converter existed).
- New tests: 2 generator tests (parameters round-trip + empty-default), 1 facade test (parameters propagation through to persisted source + workflow JSON), 1 GraphQL IntTest (round-trip via mutation).

**Client** (commit `4a90fd3b20d`):
- `AddKnowledgeBaseSourceDialog` rewritten to 4 steps: **Connection → Source Configuration → Cadence → Review**.
- Hoisted single `useForm<{sourceParameters: Record<string, unknown>}>`. `getValues('sourceParameters')` at submit time. Same pattern as Phase 20 Part B (no `useEffect`-syncing back to `useState`).
- Step 1 mounts `<WorkflowMockProvider><Properties controlPath="sourceParameters" properties={clusterElementDefinition.properties} ...></WorkflowMockProvider>`. Empty-properties branch renders a "No source configuration required" placeholder (TDZ-safe — no useEffect side effect, in contrast to auto-advancing).
- Cluster element resolution via `useGetComponentDefinitionQuery` + `useGetClusterElementDefinitionQuery` — mirrors the CS dialog's pattern.
- Review step shows a JSON preview of captured parameters or `(none required)` when empty.
- Submit payload includes `parameters: getValues('sourceParameters')`.
- 3 new tests cover: Properties renders with the correct controlPath, empty-properties placeholder, mutation payload includes parameters (empty `{}` when not filled).

**Pivots from the brief:**
- The `<Properties>` component's prop name is `controlPath` (not `path`). Adjusted accordingly.
- Empty-properties placeholder approach over auto-advance (safer; no `useEffect` side effect required, avoids TDZ pitfall).

### Phase 20b review checklist

- [x] `KnowledgeBaseSource.parameters JSONB` column landed (in-place Liquibase edit). Nullable; existing rows interpret null as "no parameters" (current behavior preserved).
- [x] `parameters` threads from GraphQL input → facade DTO → persisted entity → workflow generator's SOURCE cluster element `parameters` map.
- [x] `AddKnowledgeBaseSourceDialog` is now 4-step. Source Configuration step renders SOURCE properties via `<WorkflowMockProvider><Properties>`.
- [x] Empty-properties case shows a placeholder (no auto-advance side effect).
- [x] Backend `:check` clean across all touched modules; client `npm run check` clean.
- [x] No new tables, no new SPI, no new GraphQL queries — pure reuse of the workflow-less `clusterElementOptions` machinery the `<Properties>` renderer already calls.

---

## Self-review checklist

After implementing, before declaring done:

- [ ] All 5 tables exist (`context_store_source`, `context_store_entity`, `context_store_record`, `context_store_record_index`, `workspace_context_store_source`); Liquibase migration applies cleanly. `context_store_sync_run` is **not** created. `workspace_id` is NOT on `context_store_source` or `context_store_record` (workspace scoping flows through `workspace_context_store_source` per the 2026-05-09 platform pivot).
- [ ] All 2 enum classes (`ReaderStrategy`, `ContextStoreSourceStatus`) pinned by `EnumOrdinalStabilityTest`
- [ ] `ContextStoreItemWriter` (DESTINATION cluster element) upsert is idempotent (re-run → no duplicates, change-detect via `payload_hash`)
- [ ] `ContextStoreSyncJobListener` (JobExecutionListener) tombstones on COMPLETED, preserves last good state on FAILED — only acts when destination is `contextStore.writeToReplica`
- [ ] `WorkspaceContextStoreSourceFacade.create()` auto-generates a workflow with the correct shape (cron trigger + data-stream.stream task) and populates `context_store_source.workflow_id`; inserts (directly, since the facade lives in automation alongside the relation repo) the `workspace_context_store_source` relation row.
- [ ] `WorkspaceContextStoreSourceFacade` lives in `automation-context-store-{api,service}` (NOT in platform); facade method signatures all take `workspaceId` as the first parameter; no SPI seam back into platform; dependency direction is automation → platform only.
- [ ] `ContextStoreQueryService.search` translates each filter op correctly; cursor pagination is stable
- [ ] EE component (`server/ee/libs/modules/components/context-store/`) has Actions, the DataStream DESTINATION, and TOOLS cluster elements (the EE-only collapse merged the tool-handler into the same component module)
- [ ] GraphQL schema is added to `client/codegen.ts`; `npm run check` passes
- [ ] `refreshContextStoreSource` mutation is admin-only (`@PreAuthorize`)
- [ ] `ContextStoreToolFacade` (in `platform-context-store-api`) mints typed per-(source, entity) callbacks
- [ ] McpServer aggregates Context Store callbacks alongside `AutomationMcpToolFacade`
- [ ] CC routing agent has 3 new tool callbacks registered
- [ ] No imports of `com.bytechef.ee.platform.ai.gateway.*` anywhere in `platform-context-store/` or `automation-context-store/`
- [ ] `./gradlew check` passes for all touched modules
- [ ] `npm run check` passes
- [ ] Definition JSON for `contextStore` component regenerated under `src/test/resources/definition/`
- [ ] **Phase 13 (KB-Source)**: `KnowledgeBaseSource` entity exists in `platform-knowledge-base-api` (NO `workspace_id`); `WorkspaceKnowledgeBaseSource` relation entity exists in `automation-knowledge-base-api`; platform `knowledge_base_document` has 5 nullable sync columns + partial UNIQUE on `(source_id, source_record_id)`; companion `workspace_knowledge_base_source` table exists; `knowledgeBase.writeAsDocument` cluster element registered; `WorkspaceKnowledgeBaseSourceFacade` (in automation, signatures take `workspaceId` first) auto-generates workflow + inserts relation row; `KnowledgeBaseSourceSyncJobListener` lives in `platform-knowledge-base-service` and operates by `source_id` only.
- [ ] **Phase 14 (Semantic)**: `cs_vector_store` table is auto-managed by Spring AI's `PgVectorStore.initializeSchema(true)` (no Liquibase); `ContextStorePgVectorConfiguration` is `@ConditionalOnBean(EmbeddingModel.class) @ConditionalOnSingleTenant`; `ContextStoreSemanticBatchListener` skips records whose `payload_hash` matches the last-embedded hash via a `JdbcTemplate` query against `cs_vector_store.metadata->>'payloadHash'`; `semantic_search_<source>_<entity>` callbacks appear when embedding model + `semanticIndexFields` are configured; CE-without-embedding deployment passes a no-op smoke test; multi-tenant deployment passes a no-op smoke test.
- [ ] **Phase 15 (UI)**: Task 36a server prerequisites landed (env-arg on sources queries, `dataStreamCompatibleConnections`, 3 entity CRUD mutations); CS sources list/detail/Add dialog reachable from `/automation/context-store` (gated on `ff_4855 && edition === EE`); KB-Source list reachable as a tab inside `/automation/knowledge-bases/:id` (NOT a sibling page); KB document list shows "Sync source" badge for `sourceId IS NOT NULL` rows; `SyncSourceStatusBadge` lives in `client/src/shared/components/` and is reused across CS + KB-Source pages; entity-level CRUD on CS source detail (admin-only); cadence picker has no client-side cron library; tests are `@testing-library/react` interaction tests, not snapshot tests.

---

## Open implementation questions for the executor

These are flagged for the implementer to decide on the spot — they don't block the plan but require judgment:

1. **One workflow per source, or per (source, entity)?** Each source can have multiple entities (e.g. HubSpot source with `contacts` + `deals` + `companies`). The auto-generated workflow either runs one DataStream task that loops entities, or has multiple parallel `data-stream.stream` tasks (one per entity). Recommendation: one task per entity inside the workflow so failures are isolated. Implementer should verify DataStream's parallel-task support fits this shape.

2. **`ContextStoreToolFacade` schema generation** — `generateSchemaFromIndexedFields` should emit a JSON Schema with `filters` as array of `{field, op, value}` and per-field type hints derived from `indexedFields[].type`. Look at how `FromAiInputSchemaUtils.generateInputSchema(...)` (existing util in `platform-ai-tool-api`) shapes schemas for reference.

3. **`ContextStoreToolsComponentHandler` vs CE `ContextStoreComponentHandler` collision** — verify with `ComponentRegistry`/`ComponentDefinitionService` how two handlers for the same `componentName` are merged. If `@AutoService` registration of both classes causes a clash, the EE handler should use `@ConditionalOnEEVersion` and the CE one `@ConditionalOnMissingBean`, or use `AbstractComponentDefinitionWrapper` to extend in place. Confirm pattern by reading existing examples.

4. **Workflow ownership protection** — workflows with `metadata.contextStoreSourceId` set must be read-only in the workflow editor. Verify how the workflow editor surfaces metadata and add a UI guard. Cadence changes round-trip through `WorkspaceContextStoreSourceFacade.update()`, which mutates the trigger directly.

5. **Workspace context propagation in CC tool callbacks** — the `ToolContext` should carry `workspaceId` from the routing-agent's session. Confirm by reading how `QueryKnowledgeBaseToolCallback` resolves `workspaceId`.

6. **JobExecutionListener detection logic** — confirm what shape `JobExecution.getJobParameters()` carries when DataStream invokes Spring Batch. The listener needs to extract the destination component name + cluster element name to discriminate between `contextStore.writeToReplica` and `knowledgeBase.writeAsDocument` (and to ignore plain DataStream jobs that aren't sync workflows). May require DataStream to populate well-known JobParameter keys, or reading directly from the workflow definition via `workflowId` resolved from the JobExecution. The CS listener (Phase 5 Task 13) sets the canonical detection shape — Phase 13 Task 32's KB-Source listener must use the same primitive.

---

## Execution choice

Plan complete and saved to `docs/superpowers/plans/2026-05-08-context-store-mvp.md`. Two execution options:

**1. Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** — Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
