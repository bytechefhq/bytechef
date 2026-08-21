# Workspace Relation Table Convention Revision Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Collapse the 28 unreleased `workspace_<entity>` relation tables into a nullable `workspace_id` column on their entities, document the revised rule so the pattern stops spreading, and produce a read-only duplicate audit for the 5 released tables that keep theirs.

**Architecture:** One repeatable per-table recipe (Task 2 is the worked exemplar; Tasks 3-29 apply it). Each table is schema + entity + repository + service + tests, landing as **one self-contained commit** that leaves the build green.

**Tech Stack:** Java 25, Spring Boot 4.0.7, Spring Data JDBC, Liquibase, PostgreSQL (Testcontainers).

**Spec:** `docs/superpowers/specs/2026-07-25-workspace-relation-table-convention-revision.md`

## Global Constraints

- **Only the 28 unreleased tables may be collapsed.** The 5 released ones (`workspace_api_key`, `workspace_connection`, `workspace_data_table`, `workspace_knowledge_base`, `workspace_mcp_server`) and `workspace_user` (genuine many-to-many) are OFF LIMITS — they are in release `v0.31.2`. Verify with `git grep -q 'tableName="<table>"' v0.31.2 -- '*.xml'` before touching any table; a hit means STOP.
- **`workspace_id` is NULLABLE.** Column declared `<column name="workspace_id" type="BIGINT"/>` with no `constraints` element; the entity field is `Long` (boxed), **never** primitive `long`.
- **Do NOT change any existing service OR repository method signature.** Both keep their current `long workspaceId` parameters — repositories sit behind services and follow their signatures. Only two things become nullable: the **DB column** and the **entity field**. Repository *query bodies* change (JOIN → column filter); their parameter lists do not.
  A query taking `long workspaceId` simply never matches a row whose `workspace_id` is null, which is the correct behavior: a workspace-scoped query should not return workspace-less rows.
- **One commit per table.** Each commit is self-contained and leaves the build green. No mega-commit.
- **Do NOT add `UNIQUE(entity_id)` anywhere.** The audit (Task 30) produces evidence; acting on it is a separate decision.
- Edit init changelogs **in place** (valid only because these tables are unreleased) and delete stale `build/resources/` copies — Liquibase sees both on the classpath otherwise.
- CE (`server/libs/`): Apache header, no `@version ee`. EE (`server/ee/`): ByteChef Enterprise header AND `@version ee` (Spotless selects by that tag's presence — an EE file without it gets rewritten and fails).
- Repo style: descriptive names, blank line before control statements, no trailing blank line before class close. `./gradlew spotlessApply` before each commit.
- Run gradle DIRECTLY with `--max-workers=3` (the full build OOMs at default parallelism here) and put `echo "EXIT=$?"` on its own line — piping masks the exit code.

---

### Task 1: Document the revised rule

**Files:** Modify `CLAUDE.md`

- [ ] **Step 1: Add a subsection** stating: a **new** platform-package entity gets a nullable `workspace_id BIGINT` column, not a `workspace_<entity>` relation table; sharing, if later needed, uses a `visibility` enum modeled on `ConnectionVisibility` (`PRIVATE < WORKSPACE < ORGANIZATION`, gated transitions, pinned ordinals) which is additive to a column; a relation table is created ONLY for genuine many-to-many with no owner concept (the `workspace_user` shape). Record that **5 released tables deliberately remain on the old pattern** so the mixed state reads as intentional, not drift.
- [ ] **Step 2: Commit.** `git commit -m "Document the revised workspace scoping rule for new platform entities"`

---

### Task 2: Collapse `workspace_ai_auto_memory` (worked exemplar)

Do this one first and in full: it is the only candidate with a second (file-storage) binding and a cross-provider contract suite, so it exercises every part of the recipe. Tasks 3-29 repeat it.

**Files:**
- Modify: `.../platform-ai-auto-memory-repository-jdbc/src/main/resources/config/liquibase/changelog/platform/ai/auto/memory/20260424000001_ai_auto_memory_init.xml`
- Modify: `.../platform-ai-auto-memory-api/.../AiAutoMemory.java`
- Delete: `.../platform-ai-auto-memory-api/.../WorkspaceAiAutoMemory.java`
- Delete: `.../platform-ai-auto-memory-repository-api/.../WorkspaceAiAutoMemoryRepository.java`
- Delete: `.../platform-ai-auto-memory-repository-jdbc/.../JdbcWorkspaceAiAutoMemoryRepository.java`
- Delete: `.../platform-ai-auto-memory-repository-file-storage/.../FileStorageWorkspaceAiAutoMemoryRepository.java`
- Modify: `.../platform-ai-auto-memory-repository-jdbc/.../JdbcAiAutoMemoryRepository.java`
- Modify: `.../platform-ai-auto-memory-repository-file-storage/.../FileStorageAiAutoMemoryRepository.java`, `.../AiAutoMemoryDocument.java`, `.../config/AiAutoMemoryFileStorageConfiguration.java`
- Modify: `.../platform-ai-auto-memory-service/.../AiAutoMemoryServiceImpl.java`
- Modify tests: `AiAutoMemoryRepositoryContractTests`, `FileStorageAiAutoMemoryRepositoryContractTest`, `JdbcAiAutoMemoryRepositoryContractIntTest`, `AiAutoMemoryRepositoryIntTest`, `FileStorageAiAutoMemoryRepositoryTest`, `AiAutoMemoryServiceTest`

- [ ] **Step 1: Schema.** In the init changelog add to `createTable tableName="ai_auto_memory"`:
```xml
            <column name="workspace_id" type="BIGINT"/>
```
  (no `constraints` — nullable). DELETE the whole `createTable tableName="workspace_ai_auto_memory"` block and every `addUniqueConstraint` / `addForeignKeyConstraint` / `createIndex` referencing it. Then `find . -path '*/build/resources/*' -name '20260424000001_ai_auto_memory_init.xml' -delete`.
- [ ] **Step 2: Entity.** Add to `AiAutoMemory`:
```java
    @Column("workspace_id")
    private @Nullable Long workspaceId;
```
  with `public @Nullable Long getWorkspaceId()` / `public void setWorkspaceId(@Nullable Long workspaceId)`. Fix the class javadoc: it currently claims the entity is workspace-agnostic and shareable across workspaces — say instead that a memory belongs to at most one workspace and the column is null where no workspace applies.
- [ ] **Step 3: Repositories.** Rewrite the three `@Query` **bodies** in `JdbcAiAutoMemoryRepository`, replacing `JOIN workspace_ai_auto_memory wam ON wam.ai_auto_memory_id = m.id WHERE wam.workspace_id = :workspaceId` with `WHERE m.workspace_id = :workspaceId`, preserving each method's extra predicates and `ORDER BY m.updated_at DESC`. **Method signatures are untouched** — they keep `long workspaceId`. Delete the three `Workspace*Repository` types listed above (their table is gone, so the types have nothing to map).
- [ ] **Step 4: File-storage binding.** `AiAutoMemoryDocument.workspaceId` becomes `Long`; `fromDomain(AiAutoMemory)` drops its `workspaceId` parameter and reads it from the memory; `toDomain()` sets it. In `FileStorageAiAutoMemoryRepository`: `save(AiAutoMemory)` reads `memory.getWorkspaceId()`, and the `save(memory, workspaceId)` overload, `resolveWorkspaceId`, `findDocumentByAiAutoMemoryId` and `findDocumentsByWorkspaceId` go if now unused. **Decide and document a stable directory segment for a null workspace** (e.g. the literal `none`) so listing still narrows correctly — a null must not collapse into the same path as workspace `0`. Remove the workspace-repository beans from `AiAutoMemoryFileStorageConfiguration`.
- [ ] **Step 5: Service.** In `AiAutoMemoryServiceImpl.create()` set `memory.setWorkspaceId(workspaceId)` and delete the `workspaceAiMemoryRepository.save(new WorkspaceAiAutoMemory(...))` block plus the now-unused field/constructor parameter. **Do not change any method signature.** The surrounding `catch (DataIntegrityViolationException)` existed to translate a membership-uniqueness race into a duplicate-name error — with the membership gone, confirm whether the remaining insert can still raise it; if not, remove the catch rather than leaving dead handling.
- [ ] **Step 6: Tests.** In `AiAutoMemoryRepositoryContractTests` replace the abstract `saveInWorkspace` hook with a concrete helper that sets `workspaceId` and calls `save` — the hook existed only because membership was backend-specific. Drop the overrides in both subclasses and the `WorkspaceAiAutoMemoryRepository` field/`afterEach` cleanup in the JDBC one. Update `AiAutoMemoryRepositoryIntTest` (it writes membership rows directly around lines 106-107 and 141); its duplicate-membership `DataIntegrityViolationException` test pins a constraint that no longer exists — delete it and say so in the commit message. **Keep every other assertion**, especially the ordering case that inserts oldest-last with explicit timestamps.
- [ ] **Step 7: Verify.**
```
./gradlew :server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-repository:platform-ai-auto-memory-repository-file-storage:check \
          :server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-repository:platform-ai-auto-memory-repository-jdbc:check \
          :server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-service:check \
          --console=plain --max-workers=3
```
  then `echo "EXIT=$?"`. Must be 0, with **both** contract subclasses running the full suite.
- [ ] **Step 8: Commit** (single commit for this table).

---

### Tasks 3-29: Collapse the remaining 27 tables

Apply the Task 2 recipe to each table below, **one commit per table**. Most are simpler than the exemplar — they have a single JDBC binding, no file-storage counterpart and no contract suite.

**The recipe, per table `workspace_<entity>`:**
1. Confirm it is NOT in `v0.31.2` (`git grep -q 'tableName="workspace_<entity>"' v0.31.2 -- '*.xml'` must find nothing).
2. Init changelog: add nullable `workspace_id BIGINT` to the entity's `createTable`; delete the relation table's `createTable` and its constraints/indexes; delete stale `build/resources/` copies.
3. Entity: add `@Column("workspace_id") private @Nullable Long workspaceId;` + accessors; correct any javadoc claiming workspace-agnosticism or shareability.
4. Delete `Workspace<Entity>`, `Workspace<Entity>Repository`, `JdbcWorkspace<Entity>Repository`.
5. Repository: replace JOIN-through-relation query **bodies** with `WHERE <alias>.workspace_id = :workspaceId`, preserving predicates and ordering. **Signatures unchanged** (`long workspaceId`).
6. Service: set `workspaceId` on the entity, delete the membership write and the now-unused repository dependency. **No signature changes.** The May design introduced `getWorkspaceId(entityId)` service helpers that query the relation row — reimplement them to read the entity's column, or delete them if every caller can read the entity directly.
7. Tests: update anything constructing `Workspace<Entity>` or asserting membership behavior.
8. `./gradlew <module>:check --console=plain --max-workers=3`, `echo "EXIT=$?"` must be 0, then commit.

**The 27 tables** (grouped by owning area; order within a group does not matter):

- [ ] **Task 3-4 — Context Store:** `workspace_context_store`, `workspace_context_store_source`
- [ ] **Task 5 — Knowledge base (Context Store-era):** `workspace_knowledge_base_source`
- [ ] **Task 6-7 — AI Hub:** `workspace_ai_hub_personal_agent`, `workspace_ai_hub_task`
- [ ] **Task 8-13 — AI Gateway:** `workspace_ai_gateway_budget`, `workspace_ai_gateway_project`, `workspace_ai_gateway_provider`, `workspace_ai_gateway_rate_limit`, `workspace_ai_gateway_routing_policy`, `workspace_ai_gateway_spend_summary`
- [ ] **Task 14-18 — AI Observability:** `workspace_ai_observability_alert_rule`, `workspace_ai_observability_export_job`, `workspace_ai_observability_session`, `workspace_ai_observability_trace`, `workspace_ai_observability_webhook_subscription`
- [ ] **Task 19 — Notification (STANDALONE):** `workspace_notification`
  Keep this commit **self-contained and unreferenced to any group**. The owner intends to squash it later with the original commit that introduced `workspace_notification`, so its message must stand alone: what collapsed, that `NULL = global` preserves current production behaviour, that signatures were untouched, and how the "no membership row = global" branch became an `IS NULL` check. Its provenance is the July notification-consolidation migration (`20260720000004`, which also repointed `ai_observability_alert_rule_channel.notification_id`) — check that interaction, but do not frame the commit as part of the observability work.
- [ ] **Task 20-24 — AI Eval:** `workspace_ai_eval_dataset`, `workspace_ai_eval_experiment`, `workspace_ai_eval_rule`, `workspace_ai_eval_score`, `workspace_ai_eval_score_config`
- [ ] **Task 25-27 — Usage / prompt:** `workspace_ai_llm_usage`, `workspace_ai_tool_usage`, `workspace_ai_prompt`
- [ ] **Task 28-29 — Workflow:** `workspace_workflow_alert_rule`, `workspace_workflow_execution_cost`

**Known awkward cases — expect these to cost more than the rest:**
- `workspace_ai_observability_trace` — the May design calls it "the highest-volume table" and records that its dedup unique index was reworked during the original conversion; check the index situation carefully rather than assuming symmetry.
- `workspace_ai_observability_session` — the original conversion replaced compound `(workspace_id, external_session_id)` / `(workspace_id, user_id)` indexes with JOIN-based queries. Collapsing restores the ability to have those compound indexes; decide deliberately whether to recreate them (a query-performance judgement, so state the reasoning in the commit).
- `workspace_ai_eval_score_config` — the original conversion **dropped** a `(workspace_id, name)` unique constraint, leaving the relation table's composite unique as the only DB gate. Collapsing removes that gate entirely; note it in the commit rather than silently losing a constraint.
- `workspace_ai_eval_score` — the May notes mention trend SQL aggregating through the relation JOIN; that aggregation must be rewritten, not just the finder methods.
- `workspace_notification` — introduced by the July notification-consolidation migration (`20260720000004`), which also **repointed `ai_observability_alert_rule_channel.notification_id`**. Check that migration's interaction before editing init.
  **Semantics (verified, owner-confirmed):** `notification` is in release `v0.31.2` but `workspace_notification` is NOT, so **notifications are global in production today**. `WorkspaceNotificationRepository.findByNotificationId` returns `Optional`, not `List` — a notification belongs to at most one workspace. The mapping is therefore `NULL = global`, `X = scoped to workspace X`, which preserves production behaviour by default. Keep the capability (owner decision: collapse, do not delete the scoping). The lookup branch that currently treats "no membership row" as global must become an `IS NULL` check — get it wrong and either global notifications stop firing or they fire for every workspace. Consumers to rewire: `WorkspaceNotificationGraphQlController` and `AiObservabilityAlertRuleGraphQlController` (both inject `WorkspaceNotificationService`, whose signatures stay unchanged — only its body reads the column).
- `workspace_ai_hub_task` — the May design specifically removed `workspaceId` from this entity and routed every caller through a `taskService.getWorkspaceId(taskId)` helper across REST/GraphQL/agent surfaces. Those callers can now read the column, but the sweep is wider here than elsewhere.

**If any table turns out to need a service or repository signature change to work correctly, STOP and report it** rather than changing the signature — that is an explicit global constraint, and the exception needs a decision.

---

### Task 30: Duplicate audit for the 5 released tables

**Files:** Create `docs/superpowers/notes/2026-07-25-workspace-relation-duplicate-audit.sql`

- [ ] **Step 1: Write the read-only audit** — one query per released relation table, reporting any entity id with more than one membership row:
```sql
-- Read-only; safe against production. Any row returned is a live bug: the entity is
-- visible in two workspaces with no code path that intended it.
-- workspace_user is EXCLUDED - it is genuinely many-to-many.
SELECT 'workspace_connection' AS relation, connection_id AS entity_id, COUNT(*) AS memberships
FROM workspace_connection GROUP BY connection_id HAVING COUNT(*) > 1
UNION ALL
...
```
  Cover exactly: `workspace_api_key`, `workspace_connection`, `workspace_data_table`, `workspace_knowledge_base`, `workspace_mcp_server`. Read each entity-id column name from its Liquibase `createTable` — the naming is not uniform across the codebase.
- [ ] **Step 2: Header note** — read-only, safe on production, and adding `UNIQUE(entity_id)` in response is a separate decision explicitly out of scope.
- [ ] **Step 3: Commit.**

---

### Task 31: Full verification

- [ ] **Step 1: Confirm no released table was touched.** `git diff v0.31.2..HEAD --name-only -- '*.xml' | xargs grep -l 'workspace_api_key\|workspace_connection\|workspace_data_table\|workspace_knowledge_base\|workspace_mcp_server\|workspace_user' 2>/dev/null` — investigate any hit before proceeding.
- [ ] **Step 2: Confirm 28 relation tables are gone.** Re-run the enumeration (`grep -rhoE 'tableName="workspace_[a-z_]+"' --include='*.xml' server/ | sort -u`); only the 5 released plus `workspace_user` should remain.
- [ ] **Step 3: Full build.** `./gradlew check --continue --console=plain --max-workers=3` then `echo "EXIT=$?"`. Must be 0. If it fails, check the log for `OutOfMemoryError` (an environment problem — lower `--max-workers`) versus real failures.
- [ ] **Step 4: Assemble.** `./gradlew :server:apps:server-app:compileJava --console=plain --max-workers=3`, `echo "EXIT=$?"`.
- [ ] **Step 5: Commit** any fixes.

---

## Self-Review

**Spec coverage:** rule change → Task 1; collapse of all 28 unreleased tables → Tasks 2-29 (exemplar + recipe + explicit table list); duplicate audit for the 5 released → Task 30; verification including a guard that no released table was touched → Task 31. The spec's non-goals (no released-table collapse, no `UNIQUE(entity_id)`, no service signature changes, no speculative `visibility`) are all Global Constraints.

**Placeholder scan:** no TBDs. Tasks 3-29 are deliberately a recipe plus a named table list rather than 27 transcribed task bodies — the per-table work is identical and the exemplar carries the full detail. The genuinely variable parts are called out by name in "Known awkward cases", each with the specific thing to check.

**Type consistency:** `@Nullable Long workspaceId` + accessors, `WHERE <alias>.workspace_id = :workspaceId`, the deletion triple (`Workspace<Entity>`, its repository interface, its JDBC impl), and `AiAutoMemoryDocument.fromDomain(AiAutoMemory)` are used consistently. The nullable/primitive split is stated in three places so it cannot be misread: nullable applies to the DB column and entity field only; service AND repository signatures keep `long workspaceId` (Global Constraints, Task 2 Steps 3/5, recipe step 5).

**Risk gate:** every task begins by proving the table is absent from `v0.31.2`, and Task 31 Step 1 re-checks the whole diff — the released tables are the only irreversible mistake available here.
