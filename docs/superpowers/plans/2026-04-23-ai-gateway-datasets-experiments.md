# AI Gateway — Datasets + Experiments Framework Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let teams (a) curate datasets from production traces or uploads, (b) version them, and (c) run experiments that replay a dataset against a prompt version / model and score the results — enabling prompt regression testing.

**Architecture:** Two new EE submodules sibling to the existing AI gateway:
- `automation-ai-gateway-dataset` — owns `ai_dataset`, `ai_dataset_version`, `ai_dataset_item` and the ingest paths
- `automation-ai-gateway-experiment` — owns `ai_experiment`, `ai_experiment_run`, the replay executor, and the comparison GraphQL surface

Experiment execution fans out one task per dataset item via the existing atlas-coordinator pipeline (reuses `JobFacade.createJob()`). Each run calls `AiGatewayFacade.chatCompletion(...)` programmatically, producing a real trace (new `AiObservabilityTraceSource.EXPERIMENT`). Scoring reuses existing `AiEvalExecutor` evaluator machinery with a new `AiEvalRule.target` discriminator (§12.5 resolution: YES, add discriminator).

**Tech Stack:** Java 25 · Spring Boot 4 · Spring Data JDBC · Liquibase · GraphQL (Spring for GraphQL) · Micrometer · Atlas coordinator · Testcontainers

**Corresponds to:** §8 of `docs/superpowers/specs/2026-04-21-ai-gateway-gaps-spec.md`

**Depends on:** Nothing hard. Works standalone. Benefits when Spec B (External Scores API) ships — external evaluators can score experiment traces.

---

## Phased Execution

Spec C is the largest of the three sub-specs in the gaps bundle. Break execution into 4 phases so early phases ship standalone value:

**Phase 1 — Dataset persistence & CRUD (6 tasks)**
Datasets + versions + items land in the DB. REST endpoints for promote-from-trace, CSV/JSONL upload, and single-item POST. No experiments yet.

**Phase 2 — Experiment execution (5 tasks)**
Trigger an experiment. Fan out via atlas. Generate synthetic traces. Record run status.

**Phase 3 — Scoring + Comparison (3 tasks)**
Extend `AiEvalRule.target` discriminator. Hook eval execution to experiment-run trace completion. Add GraphQL `experimentComparison` query.

**Phase 4 — Integration tests + docs (2 tasks)**
End-to-end integration tests against Testcontainers Postgres. README.

Each phase commits independently. A team could merge Phase 1 alone and start curating datasets before Phase 2 lands.

---

## File Structure (all phases)

### New modules

```
server/ee/libs/automation/automation-ai/automation-ai-gateway/
├── automation-ai-gateway-dataset/
│   ├── automation-ai-gateway-dataset-api/
│   ├── automation-ai-gateway-dataset-service/
│   ├── automation-ai-gateway-dataset-public-rest/
│   └── automation-ai-gateway-dataset-remote-client/
└── automation-ai-gateway-experiment/
    ├── automation-ai-gateway-experiment-api/
    ├── automation-ai-gateway-experiment-service/
    ├── automation-ai-gateway-experiment-public-rest/
    ├── automation-ai-gateway-experiment-graphql/
    └── automation-ai-gateway-experiment-remote-client/
```

### Modified files

- `settings.gradle.kts` — 9 new include lines (4 dataset + 5 experiment)
- `server/ee/apps/coordinator-app/build.gradle.kts` — add new service modules
- `server/ee/apps/server-app/build.gradle.kts` (if exists) — same
- `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityTraceSource.java` — append `EXPERIMENT` (ordinal 3 — append-only)
- `.../domain/AiEvalRule.java` — add `target` field (`LIVE_TRACE | EXPERIMENT_TRACE`)
- `.../domain/AiEvalExecutionTarget.java` — new enum (INT ordinal, append-only)

### New Liquibase changelogs

- `00000000000006_ai_dataset_init.xml` — 3 dataset tables
- `00000000000007_ai_experiment_init.xml` — 2 experiment tables
- `00000000000008_ai_eval_rule_add_target.xml` — adds `target` column to `ai_eval_rule`

---

## Conventions

- All files under `server/ee/**` use the ByteChef Enterprise license block + `@version ee` Javadoc.
- Enums persist as INT ordinals, append-only.
- Metrics via `ObjectProvider<MeterRegistry>`.
- Liquibase changesets use NO `context="ee"` attribute (Spec B Task 1 discovered this — sibling convention is contextless).
- Workspace resolution via `X-ByteChef-Workspace-Id` header (consistent with Specs A and B).
- Cross-workspace writes → 403 via `AiScoreWorkspaceBoundaryException`-style exception.

---

# Phase 1 — Dataset Persistence & CRUD

## Task 1.1: Scaffold the 4 dataset submodules

**Files:**
- Create: `automation-ai-gateway-dataset/{api,service,public-rest,remote-client}/build.gradle.kts`
- Create: `automation-ai-gateway-dataset/{api,service,public-rest,remote-client}/src/main/java/com/bytechef/ee/automation/ai/gateway/dataset/package-info.java` in each module
- Modify: `settings.gradle.kts` — add 4 include lines adjacent to `automation-ai-gateway-otlp-*`

Each `build.gradle.kts` follows the shape of the corresponding sibling under `automation-ai-gateway-*` (reference: Spec A's OTLP module pattern). Api has no Spring dependency. Service adds `api(project(":...:automation-ai-gateway-dataset-api"))` + `implementation("org.springframework.boot:spring-boot-autoconfigure")` + `implementation("org.apache.commons:commons-lang3")`. Public-rest adds `api(project(":...:automation-ai-gateway-dataset-service"))` + `api(project(":server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api"))` (for workspace auth). Remote-client uses the OTLP remote-client as template.

Each `package-info.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

/**
 * Dataset curation and versioning for AI gateway evaluation workflows.
 *
 * @version ee
 */
package com.bytechef.ee.automation.ai.gateway.dataset;
```

Verify: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-dataset:automation-ai-gateway-dataset-api:compileJava` BUILD SUCCESSFUL.

Commit: `Scaffold automation-ai-gateway-dataset submodules`

## Task 1.2: Liquibase migration for dataset tables

Create `automation-ai-gateway-dataset-service/src/main/resources/config/liquibase/changelog/automation/ai_gateway/00000000000006_ai_dataset_init.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="20260423000002" author="ivicac">
        <createTable tableName="ai_dataset">
            <column name="id" type="BIGINT" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="workspace_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="project_id" type="BIGINT"/>
            <column name="name" type="VARCHAR(256)">
                <constraints nullable="false"/>
            </column>
            <column name="description" type="TEXT"/>
            <column name="tags" type="TEXT"/>
            <column name="archived_date" type="DATETIME"/>
            <column name="created_date" type="DATETIME">
                <constraints nullable="false"/>
            </column>
            <column name="created_by" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="last_modified_date" type="DATETIME">
                <constraints nullable="false"/>
            </column>
            <column name="last_modified_by" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="version" type="BIGINT" defaultValueNumeric="0"/>
        </createTable>

        <addUniqueConstraint
                tableName="ai_dataset"
                constraintName="uk_ai_dataset_workspace_name"
                columnNames="workspace_id,name"/>

        <createIndex tableName="ai_dataset" indexName="idx_ai_dataset_workspace">
            <column name="workspace_id"/>
        </createIndex>
    </changeSet>

    <changeSet id="20260423000003" author="ivicac">
        <createTable tableName="ai_dataset_version">
            <column name="id" type="BIGINT" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="dataset_id" type="BIGINT">
                <constraints nullable="false" foreignKeyName="fk_ai_dataset_version_dataset"
                             references="ai_dataset(id)"/>
            </column>
            <column name="version_number" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="label" type="VARCHAR(128)"/>
            <column name="frozen" type="BOOLEAN" defaultValueBoolean="false"/>
            <column name="created_date" type="DATETIME">
                <constraints nullable="false"/>
            </column>
            <column name="created_by" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <addUniqueConstraint
                tableName="ai_dataset_version"
                constraintName="uk_ai_dataset_version_number"
                columnNames="dataset_id,version_number"/>
    </changeSet>

    <changeSet id="20260423000004" author="ivicac">
        <createTable tableName="ai_dataset_item">
            <column name="id" type="BIGINT" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="dataset_id" type="BIGINT">
                <constraints nullable="false" foreignKeyName="fk_ai_dataset_item_dataset"
                             references="ai_dataset(id)"/>
            </column>
            <column name="dataset_version_id" type="BIGINT">
                <constraints nullable="false" foreignKeyName="fk_ai_dataset_item_version"
                             references="ai_dataset_version(id)"/>
            </column>
            <column name="input" type="CLOB">
                <constraints nullable="false"/>
            </column>
            <column name="expected_output" type="CLOB"/>
            <column name="metadata" type="CLOB"/>
            <column name="source_trace_id" type="BIGINT"/>
            <column name="created_date" type="DATETIME">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <createIndex tableName="ai_dataset_item" indexName="idx_ai_dataset_item_version">
            <column name="dataset_version_id"/>
        </createIndex>
    </changeSet>
</databaseChangeLog>
```

**Note:** `context="ee"` attribute OMITTED — sibling AI gateway changelogs are context-less. This was a live-site bug in Spec B Task 1.

Commit: `Add ai_dataset, ai_dataset_version, ai_dataset_item Liquibase migrations`

## Task 1.3: Domain entities + repositories

Under `automation-ai-gateway-dataset-api/src/main/java/com/bytechef/ee/automation/ai/gateway/dataset/`:

- `domain/AiDataset.java` — record-free entity (Spring Data JDBC needs a private no-arg constructor + settable fields)
- `domain/AiDatasetVersion.java`
- `domain/AiDatasetItem.java`

Follow `AiEvalScore.java` pattern: `@Table("ai_dataset")`, `@Id Long id`, `@CreatedDate`, `@LastModifiedDate`, `@Version`, private no-arg constructor, factory method for construction.

Under `automation-ai-gateway-dataset-service/src/main/java/com/bytechef/ee/automation/ai/gateway/dataset/`:

- `repository/AiDatasetRepository.java` — `extends ListCrudRepository<AiDataset, Long>`, adds `findAllByWorkspaceId(Long)`, `findByWorkspaceIdAndName(Long, String)`
- `repository/AiDatasetVersionRepository.java` — `findAllByDatasetId`, `findByDatasetIdAndVersionNumber`, `findByDatasetIdAndLabel`
- `repository/AiDatasetItemRepository.java` — `findAllByDatasetVersionId`, `countByDatasetVersionId`
- `config/AiGatewayDatasetJdbcRepositoryConfiguration.java` — `@AutoConfiguration` + `@EnableJdbcRepositories`
- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` registering the config

Unit tests for each entity's validation (reuse the pattern from `AiEvalScoreTest`).

Commit: `Add AiDataset + AiDatasetVersion + AiDatasetItem domain and repositories`

## Task 1.4: Dataset + version + item services

Under `automation-ai-gateway-dataset-api/.../service/`:

- `AiDatasetService.java` — CRUD + `findByWorkspace`, `archive(long)`
- `AiDatasetVersionService.java` — `createVersion(long datasetId, String label, boolean frozen)`, `getOrCreateUnfrozenVersion(long datasetId)`, `freeze(long versionId)`
- `AiDatasetItemService.java` — `addItem`, `addItems(List)`, `promoteFromTrace(long traceId, Long datasetId)`, `getItemsByVersion`

Under `-service/.../service/`: impls + int tests (Testcontainers).

**Key behavior:** if target version is `frozen`, auto-create a new unfrozen version before insert. Items are copy-on-freeze (each item belongs to exactly one version).

Commit: `Add AiDatasetService + version + item services with copy-on-freeze semantics`

## Task 1.5: REST controller + DTOs

Under `automation-ai-gateway-dataset-public-rest/.../public_/web/rest/`:

- `AiDatasetController.java` — 6 endpoints:
  - `POST /api/ai-gateway/v1/datasets` — create dataset
  - `GET /api/ai-gateway/v1/datasets` — list datasets in workspace
  - `POST /api/ai-gateway/v1/datasets/{id}/items` — single-item POST
  - `POST /api/ai-gateway/v1/datasets/{id}/items/from-trace` — promote from trace
  - `POST /api/ai-gateway/v1/datasets/{id}/items/bulk` — bulk JSONL upload (one item per line)
  - `POST /api/ai-gateway/v1/datasets/{id}/versions/{versionId}/freeze` — freeze a version

Each endpoint requires `X-ByteChef-Workspace-Id` header.

DTOs (records in `-api`'s `dto/` package):

- `CreateDatasetRequest(String name, String description, List<String> tags)`
- `AddItemRequest(Object input, Object expectedOutput, Map<String, Object> metadata)`
- `PromoteFromTraceRequest(Long traceId, Object expectedOutput, Map<String, Object> metadata)`
- `BulkAddItemsRequest(List<AddItemRequest> items)` — with 1000-item cap, mirroring Spec B

Unit tests via `@WebMvcTest`.

Commit: `Add AiDatasetController with 6 REST endpoints`

## Task 1.6: Remote-client stub + Phase 1 final check

Under `automation-ai-gateway-dataset-remote-client/.../remote/client/service/`:

- `RemoteAiDatasetServiceClient.java` — implements `AiDatasetService`, throws `UnsupportedOperationException` on every method. `@ConditionalOnEEVersion`.

Final verify: run `check` on all 4 new dataset modules + ensure existing AI gateway modules still green.

Commit: `Add RemoteAiDatasetServiceClient stub`

---

# Phase 2 — Experiment Execution

## Task 2.1: Scaffold the 5 experiment submodules

Same pattern as Task 1.1 but adds a `-graphql` submodule (mirror `automation-ai-gateway-graphql`).

Verify compile + commit.

## Task 2.2: Liquibase migration for experiment tables

`00000000000007_ai_experiment_init.xml`:

```xml
<changeSet id="20260423000005" author="ivicac">
    <createTable tableName="ai_experiment">
        <column name="id" type="BIGINT" autoIncrement="true">
            <constraints primaryKey="true" nullable="false"/>
        </column>
        <column name="workspace_id" type="BIGINT"><constraints nullable="false"/></column>
        <column name="dataset_version_id" type="BIGINT">
            <constraints nullable="false" foreignKeyName="fk_ai_experiment_dataset_version"
                         references="ai_dataset_version(id)"/>
        </column>
        <column name="prompt_version_id" type="BIGINT"/>
        <column name="model" type="VARCHAR(256)"/>
        <column name="status" type="INT" defaultValueNumeric="0">
            <constraints nullable="false"/>
        </column>
        <column name="metadata" type="CLOB"/>
        <column name="created_by" type="VARCHAR(50)"><constraints nullable="false"/></column>
        <column name="created_date" type="DATETIME"><constraints nullable="false"/></column>
        <column name="started_date" type="DATETIME"/>
        <column name="completed_date" type="DATETIME"/>
    </createTable>

    <createTable tableName="ai_experiment_run">
        <column name="id" type="BIGINT" autoIncrement="true">
            <constraints primaryKey="true" nullable="false"/>
        </column>
        <column name="experiment_id" type="BIGINT">
            <constraints nullable="false" foreignKeyName="fk_ai_experiment_run_experiment"
                         references="ai_experiment(id)"/>
        </column>
        <column name="dataset_item_id" type="BIGINT">
            <constraints nullable="false" foreignKeyName="fk_ai_experiment_run_item"
                         references="ai_dataset_item(id)"/>
        </column>
        <column name="trace_id" type="BIGINT"/>
        <column name="status" type="INT" defaultValueNumeric="0">
            <constraints nullable="false"/>
        </column>
        <column name="latency_ms" type="INT"/>
        <column name="cost" type="DECIMAL(20,6)"/>
        <column name="error_message" type="TEXT"/>
        <column name="created_date" type="DATETIME"><constraints nullable="false"/></column>
    </createTable>

    <createIndex tableName="ai_experiment_run" indexName="idx_ai_experiment_run_experiment">
        <column name="experiment_id"/>
    </createIndex>
</changeSet>
```

Append `AiObservabilityTraceSource.EXPERIMENT` in a separate changelog + Java enum change.

## Task 2.3: Domain + enums

- `AiExperiment.java`, `AiExperimentRun.java`
- `AiExperimentStatus` enum — `{PENDING (0), RUNNING (1), COMPLETED (2), FAILED (3)}`
- `AiExperimentRunStatus` enum — same values

Repos + service for each.

## Task 2.4: Experiment execution — atlas fanout

Create `AiExperimentExecutor` in `-service`:
- Method `execute(long experimentId)` — fetches the experiment, lists dataset items, fires one `JobFacade.createJob(...)` per item (or wraps the whole batch in a single workflow — verify which is simpler with atlas primitives)
- Each per-item task replays the input through `AiGatewayFacade.chatCompletion(...)` with a synthetic `AiObservabilityTracingHeaders` marked `source = EXPERIMENT`
- On completion, updates `ai_experiment_run.{trace_id, status, latency_ms, cost}`
- On all-runs-completed, updates `ai_experiment.{status, completed_date}`

Use `@Async` + `TaskExecutor` for simplicity if atlas integration is high-risk — note as plan deviation.

## Task 2.5: REST controller — POST /api/ai-gateway/v1/experiments

Body: `{ datasetVersionId, promptVersionId, model, evaluatorIds: [...] }`
Returns: `201 Created` with `{ experimentId, status: "PENDING" }`
Fires execution via `ExperimentExecutor.execute(experimentId)` — returns immediately.

Also: `GET /api/ai-gateway/v1/experiments/{id}` — status poll endpoint.

---

# Phase 3 — Scoring + Comparison

## Task 3.1: Add `target` discriminator to `AiEvalRule`

Liquibase migration `00000000000008_ai_eval_rule_add_target.xml` adds `target INT DEFAULT 0 NOT NULL`. Append `AiEvalRuleTarget` enum: `{LIVE_TRACE (0), EXPERIMENT_TRACE (1)}`.

Existing `AiEvalExecutor.evaluateTrace(...)` gains a `target` filter — only evals rules where `rule.target == target` run against the incoming trace.

## Task 3.2: Hook experiment-run trace completion to AiEvalExecutor

After `AiExperimentExecutor` records a successful run, dispatch `AiEvalExecutor.evaluateTrace(traceId, workspaceId)` with `target = EXPERIMENT_TRACE`. Scores land in `AiEvalScore` linked to the run's trace via `ai_eval_score.trace_id` → external scorers (Spec B) can also score experiment traces by POSTing to `/traces/{runTraceId}/scores`.

## Task 3.3: GraphQL `experimentComparison` query

Under `automation-ai-gateway-experiment-graphql/.../web/graphql/`:

```java
@Controller
@ConditionalOnEEVersion
public class AiExperimentGraphQlController {
    @QueryMapping
    @PreAuthorize("@permissionService.hasWorkspaceRole(...)")
    public ExperimentComparisonView experimentComparison(@Argument List<Long> experimentIds) {
        // aggregate scores per experiment, compute deltas
    }
}
```

GraphQL schema: `ExperimentComparisonView { rows: [ExperimentComparisonRow], aggregateScoreDeltas: [ScoreDelta] }`.

---

# Phase 4 — Tests + Docs

## Task 4.1: Integration tests

Facade-level int test (Testcontainers): create dataset, promote from trace, freeze version, trigger experiment, wait for RUNNING state, assert run rows created, assert final status after mocked replay.

HTTP-level int test: POST dataset → POST item → POST freeze → POST experiment → GET status.

## Task 4.2: README

Document the dataset + experiment workflows at module README level.

---

## Self-Review

**Spec coverage:**

| Spec item | Task(s) |
|-----------|---------|
| §8.2 — 5 tables | 1.2, 2.2 |
| §8.3 — 3 ingest paths | 1.5 |
| §8.4 — atlas-coordinator dispatch | 2.4 |
| §8.5 — comparison GraphQL | 3.3 |
| §8.6 — module layout | 1.1, 2.1 |
| Reuse `AiEvalRule` with `target` discriminator (§12.5) | 3.1, 3.2 |
| Remote-client stubs | 1.6, 2.1 (stub per service) |

**Open questions carried forward:**
- §12.3 — do experiment-run traces count against customer's observability retention quota? Leaning no (separate pool). Implementation defers this — experiment traces use `AiObservabilityTraceSource.EXPERIMENT` and the retention job can filter by source.
- §12.4 — auto-freeze on experiment reference vs manual flag. Implementation: manual freeze in Phase 1 (explicit API endpoint), auto-freeze behavior deferred.

**Plan drift hedging:** atlas-coordinator integration (Task 2.4) is the single highest-risk task because the team's experience with dispatching non-workflow jobs via `JobFacade` may be limited. Fallback: Task 2.4 can ship as `@Async`-based with a `TaskExecutor` bean; full atlas integration then becomes a follow-up.

**Scope warning:** Phase 2 task 2.4 is genuinely large (1-2 days of focused work). If splitting into multiple sessions, do Phase 1 first, merge it, then start Phase 2 in a fresh session with the dispatched-via-atlas question freshly scoped.
