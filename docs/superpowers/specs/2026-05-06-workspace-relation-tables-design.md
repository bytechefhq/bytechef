# Workspace ↔ entity relation-table refactor

**Status:** all 17 spec tables done + 5 ad-hoc eval/observability additions caught during implementation | **Owner:** Ivica | **Created:** 2026-05-06 | **Last updated:** 2026-05-06

## Goal

Replace the `workspace_id` column on a set of entity tables with a `workspace_<entity>` relation table so the entity is workspace-agnostic and the (workspace, entity) link is explicit and shareable. Mirrors the existing pattern used by `workspace_mcp_server`, `workspace_connection`, `workspace_asset_file`, `workspace_knowledge_base`, `workspace_data_table`, `workspace_ai_gateway_provider`, `workspace_ai_gateway_routing_policy`.

The user instruction was to **edit existing init Liquibase files in place** (no separate "drop column" migration), since this schema is not yet deployed beyond local dev DBs.

## Done so far

| Table | Notes | Commit |
|---|---|---|
| `ai_hub_personal_agent` | Variant A. Constructor `(workspaceId, userId)` → `(userId)`. Service joins through `workspace_ai_hub_personal_agent`. | `e3ae3952e5e` |
| `ai_hub_task` (initial) | Variant B (@Transient) — **reverted** in `c8aedc39344`. | `b162fb48a8d` |
| `ai_hub_task` (final) | Variant A. `workspaceId` field gone entirely. Callers go through `taskService.getWorkspaceId(taskId)` helper (sweep across `AiHubRoutingAgent`, `WebhookBridgeAgent`, REST/GraphQL/artifact callers). | `c8aedc39344` |
| `ai_auto_memory` | Variant A. Init migration consolidates the prior environment add-on (deletes 20260429140002). | `f14ef587375` |
| `ai_llm_usage` | Variant A. `AiLlmUsageService.create(usage)` → `create(usage, workspaceId)`. Workspace-aware repo queries JOIN through `workspace_ai_llm_usage`. Cost-dashboard queries take the JOIN. | `79987f18b4d` |
| `ai_tool_usage` | Variant A. Recorder writes the entity + relation in the same `REQUIRES_NEW` transaction. | `9e652bb4df1` |
| `ai_gateway_tag` | Replaced — dropped the standalone tag stack, reused platform `tag` table via thin `(routing_policy_id, tag_id)` join (project_tag-style). | `d1cf47863b9` |
| `ai_gateway_budget` | Variant A. | `eb182f09412` |
| `ai_gateway_project` | Variant A. | `5ac43089e20` |
| `ai_gateway_rate_limit` | Variant A. | `b2bc4ea02e3` |
| `ai_gateway_spend_summary` | Variant A. Cost dashboards take the JOIN through `workspace_ai_gateway_spend_summary`. | `11bb9173752` |
| `ai_observability_alert_rule` | Variant A. | `4586d274ee6` |
| `ai_observability_export_job` | Variant A. | `b4414385e27` |
| `ai_observability_notification_channel` | Variant A. | `bdb01af5a7c` |
| `ai_observability_webhook_subscription` | Variant A. | `58cc02b0b89` |
| `ai_dataset` | Variant A. | `b206b8a4076` |
| `ai_experiment` | Variant A. | `2db8d9829c2` |
| `ai_prompt` | Variant A + extracted to `server/ee/libs/platform/platform-ai/platform-ai-prompt/` so non-gateway agent surfaces can reuse the prompt store. | `b837cfff0c5` |
| `ai_eval_rule` | Variant A. Ad-hoc — not in the original 17 spec tables; surfaced when the user audited remaining `workspaceId` fields. | `ebdca791023` |
| `ai_eval_score_config` | Variant A. Dropped `(workspace_id, name)` unique constraint — the relation table's `(workspace_id, ai_eval_score_config_id)` is now the only DB-level uniqueness gate. Ad-hoc. | `cb5fc150d54` |
| `ai_eval_score` | Variant A. Factory methods `numeric/bool/categorical` no longer take `workspaceId`; passed only to `AiEvalScoreService.create(score, workspaceId)`. Trend SQL aggregation joins through the relation. Ad-hoc. | `83aadd2bcb5` |
| `ai_observability_session` | Variant A. Replaced compound `(workspace_id, external_session_id)` and `(workspace_id, user_id)` indexes with JOIN-based query. Ad-hoc. | `0a059f33ba9` |
| `ai_observability_trace` | Variant A — the highest-volume table. Required deleting follow-up migration `00000000000014_ai_observability_trace_dedup_index_rename.xml` (the index it renamed no longer exists), and renaming the dedup unique index to `uq_ai_obs_trace_ext_trace_id` directly in init. Constraint is now global on `external_trace_id` rather than `(workspace_id, external_trace_id)` because cross-table uniqueness isn't expressible in standard SQL — acceptable because OTLP trace IDs are 16-byte hex (UUID-like, ~2^-64 collision probability). Ad-hoc. | `1387ee74fa7` |

**Use Variant A only.** User decision (2026-05-06): mirror Connection / WorkspaceConnection — the entity is fully workspace-agnostic, no `workspaceId` field of any kind. Callers that need the workspace for an entity ID call a service helper (`getWorkspaceId(entityId)`) that queries the relation row directly.

The earlier `ai_hub_task` commit (`b162fb48a8d`) experimented with a `@Transient workspaceId` shim populated by the service after JOIN. Commit `c8aedc39344` reverted that shim — `task.getWorkspaceId()` is gone, every caller now goes through `taskService.getWorkspaceId(taskId)`. **Do not use the @Transient pattern.**

## Standard relation-table shape

For most of the remaining tables, the relation table follows this shape (verbatim from `workspace_mcp_server` / `workspace_ai_hub_personal_agent`):

```xml
<createTable tableName="workspace_<entity>">
    <column name="id" type="BIGINT" autoIncrement="true">
        <constraints primaryKey="true" nullable="false"/>
    </column>
    <column name="workspace_id" type="BIGINT">
        <constraints nullable="false"/>
    </column>
    <column name="<entity>_id" type="BIGINT">
        <constraints nullable="false"/>
    </column>
    <column name="created_date" type="TIMESTAMP"><constraints nullable="false"/></column>
    <column name="created_by" type="VARCHAR(50)"><constraints nullable="false"/></column>
    <column name="last_modified_date" type="TIMESTAMP"><constraints nullable="false"/></column>
    <column name="last_modified_by" type="VARCHAR(50)"><constraints nullable="false"/></column>
    <column name="version" type="BIGINT"><constraints nullable="false"/></column>
</createTable>

<addUniqueConstraint
    constraintName="uk_workspace_<entity>"
    tableName="workspace_<entity>"
    columnNames="workspace_id, <entity>_id"/>

<createIndex tableName="workspace_<entity>"
             indexName="idx_workspace_<entity>_workspace">
    <column name="workspace_id"/>
</createIndex>

<addForeignKeyConstraint
    baseTableName="workspace_<entity>"
    baseColumnNames="<entity>_id"
    constraintName="fk_workspace_<entity>_<entity>"
    referencedTableName="<entity>"
    referencedColumnNames="id"
    onDelete="CASCADE"/>
```

## Per-table backlog

### Group 1 — platform-ai (3 tables, simplest, do first)

These all live in their own module; touch surface is contained.

#### 1.1 `ai_auto_memory`

- **Module:** `server/libs/platform/platform-ai/platform-ai-auto-memory/`
- **Entity:** `platform-ai-auto-memory-api/.../AiAutoMemory.java`
- **Repository:** `platform-ai-auto-memory-repository/platform-ai-auto-memory-repository-api/.../AiAutoMemoryRepository.java` (interface) + `…-jdbc/JdbcAiAutoMemoryRepository.java`
- **Migration:** `platform-ai-auto-memory-repository-jdbc/src/main/resources/config/liquibase/changelog/platform/ai/auto/memory/20260424000001_ai_auto_memory_init.xml`
- **Approach:** Variant A (entity rewrite). The service uses `findByWorkspace…` queries — straightforward to convert to JOINs.
- **Callers of `getWorkspaceId()`:** check `AiHubConfiguration.buildMemoryIndexResolver`, the 6 tool callbacks under `automation-ai-hub-service/.../tool/memory/`, the GraphQL controller in `platform-ai-auto-memory-graphql`. All pass `workspaceId` in from the caller — no deep infrastructure reads.
- **Leave name out of unique key** on the entity (per user instruction); the relation table's `(workspace_id, ai_auto_memory_id)` is the only DB-level uniqueness gate.
- **Int test reminder:** add `@EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")` on the int-test JDBC config or the audit columns will fail NOT NULL.

#### 1.2 `ai_llm_usage`

- **Module:** `server/ee/libs/platform/platform-ai/platform-ai-llm-usage/`
- **Migration:** `platform-ai-llm-usage-service/src/main/resources/config/liquibase/changelog/platform/ai/llm/usage/00000000000001_ai_llm_usage_init.xml`
- **Approach: Variant A — no denormalized column.** User decision (2026-05-06): every cost-dashboard query (`SUM(cost) GROUP BY workspace_id` style) takes a JOIN through `workspace_ai_llm_usage`. Accepted as a tradeoff for schema consistency. Index the relation's `workspace_id` column heavily; the dashboard queries become `SELECT workspace_id, SUM(cost) FROM ai_llm_usage cct JOIN workspace_ai_llm_usage wcct ON wcct.ai_llm_usage_id = cct.id GROUP BY wcct.workspace_id`.
- **Operational note:** the relation row is on the **insert** path of every LLM call (every billable turn). The membership row is mandatory; failing to insert it would orphan the usage row from analytics. Wrap the entity + membership saves in the same `@Transactional` block in `LlmUsageRecorder` so a half-write rolls back cleanly.
- Earlier session already removed an orphan `ai_hub_usage` migration in `c08bb083f69`; that cleanup left `ai_llm_usage` without an `environment` column. If the dashboards need `GROUP BY environment` too, add an `environment INT` column on `ai_llm_usage` itself in this same edit (kept on the entity since it's not workspace-coupled).

#### 1.3 `ai_tool_usage`

- **Module:** `server/ee/libs/platform/platform-ai/platform-ai-tool-usage/`
- **Migration:** `platform-ai-tool-usage-service/src/main/resources/config/liquibase/changelog/platform/ai/tool/usage/20260505000001_ai_tool_usage_init.xml`
- **Approach: Variant A — same as 1.2.** Insert-path membership row, dashboards take the JOIN.

### Group 2 — ai_gateway core (5 tables)

All under `server/ee/libs/automation/automation-ai/automation-ai-gateway/`. Init migrations sit in `automation-ai-gateway-service/src/main/resources/config/liquibase/changelog/automation/ai_gateway/`.

#### 2.1 `ai_gateway_budget`

- **Init migration:** `00000000000001_ai_gateway_init.xml` (shared file with several gateway tables)
- **Entity:** `automation-ai-gateway-api/.../domain/AiGatewayBudget.java`
- **Approach:** Variant A. Budget is workspace-scoped; lookups go through the facade.

#### 2.2 `ai_gateway_project`

- **Init migration:** `00000000000001_ai_gateway_init.xml`
- **Approach:** Variant A. Same shape as budget — workspace-scoped admin entity.

#### 2.3 `ai_gateway_rate_limit`

- **Init migration:** `00000000000001_ai_gateway_init.xml`
- **Approach:** Variant A.

#### 2.4 `ai_gateway_spend_summary`

- **Init migration:** `00000000000001_ai_gateway_init.xml`
- **Approach: Variant A — no denormalized column.** User decision (2026-05-06): this is an aggregate of `ai_llm_usage` already; analytics queries that join from this table will JOIN through `workspace_ai_gateway_spend_summary` to filter by workspace. Same insert-path membership requirement as `ai_llm_usage`.

#### 2.5 `ai_gateway_tag` — **DIFFERENT SHAPE, DO LAST**

User instruction: "ai_gateway_tag should just be connection to tag table, check project_tag for sample".

- **Current state:** `ai_gateway_tag` is a standalone tag entity (id, workspace_id, name, color) with its own service/controller/REST stack: `AiGatewayTag.java`, `AiGatewayTagService.java`, `AiGatewayTagServiceImpl.java`, `AiGatewayTagApiController.java`, `AiGatewayTagGraphQlController.java`, `AiGatewayTagRepository.java`. Plus a join row `AiGatewayRoutingPolicyTag.java` that points at it.
- **Target state (per `project_tag` sample):** drop the standalone `ai_gateway_tag` table entirely; reuse the existing platform `tag` table (in `automation-configuration`); create thin join tables like `ai_gateway_routing_policy_tag(routing_policy_id, tag_id)` with composite PK, no audit/version columns.
- **The `project_tag` shape** (from `automation-configuration/.../00000000000001_automation_configuration_init.xml`):
  ```xml
  <createTable tableName="project_tag">
      <column name="project_id" type="BIGINT"><constraints nullable="false"/></column>
      <column name="tag_id" type="BIGINT"><constraints nullable="false"/></column>
  </createTable>
  <addPrimaryKey tableName="project_tag" columnNames="project_id,tag_id"/>
  ```
- **Scope of this one:** much bigger than the others — delete the entire AiGatewayTag stack (entity, repo, service, REST/GraphQL controllers, tests), replace the existing `AiGatewayRoutingPolicyTag` join with a plain `(routing_policy_id, tag_id)` composite-PK table referencing the existing `tag` table, migrate any data from `ai_gateway_tag` to `tag`. **This is a separate spec and should not be batched with the others.**

### Group 3 — ai_observability (4 tables)

All under `automation-ai-gateway-service`, init migrations in `00000000000002_ai_observability_init.xml`.

#### 3.1 `ai_observability_alert_rule`
#### 3.2 `ai_observability_export_job`
#### 3.3 `ai_observability_notification_channel`
#### 3.4 `ai_observability_webhook_subscription`

- **Approach:** All Variant A (entity rewrite). These are workspace-scoped admin entities; their queries take `workspaceId` from the request.
- **Common mistake to avoid:** webhook_subscription has a follow-up migration `00000000000011_ai_observability_webhook_subscription_add_last_delivery_error.xml`. If you edit the init file in place, you don't need to touch the follow-up.

### Group 4 — ai_prompt + ai_dataset + ai_experiment (3 tables, separate modules)

#### 4.1 `ai_prompt`

- **Module:** `automation-ai-gateway-service`
- **Init migration:** `00000000000003_ai_prompt_init.xml`
- **Approach:** Variant A. Workspace-scoped library entity.

#### 4.2 `ai_dataset`

- **Module:** `automation-ai-gateway-dataset/automation-ai-gateway-dataset-service`
- **Init migration:** `00000000000006_ai_dataset_init.xml`
- **Follow-up migrations:** `00000000000010_ai_dataset_unique_unfrozen.xml` — if init is edited in place, check whether this follow-up's predicates still match.
- **Approach:** Variant A.

#### 4.3 `ai_experiment`

- **Module:** `automation-ai-gateway-experiment/automation-ai-gateway-experiment-service`
- **Init migration:** `00000000000007_ai_experiment_init.xml`
- **Follow-up migrations:** `00000000000009_ai_experiment_stop_flag.xml` — same caution as datasets.
- **Approach:** Variant A.

## Recommended execution order

Do them in batches, one fresh session per batch. Each batch should be 1 commit per table to keep diff sizes tractable.

1. ~~**Batch 1 (small platform):** ai_auto_memory~~ — done (`f14ef587375`).
2. ~~**Batch 2 (usage tables):** ai_llm_usage + ai_tool_usage~~ — done (`79987f18b4d`, `9e652bb4df1`).
3. **Batch 3 (ai_gateway core):** ai_gateway_budget, ai_gateway_project, ai_gateway_rate_limit, ai_gateway_spend_summary. All Variant A.
4. **Batch 4 (observability):** all 4 ai_observability_*.
5. **Batch 5 (libraries):** ai_prompt, ai_dataset, ai_experiment.
6. **Batch 6 (separate spec):** ai_gateway_tag — full delete + project_tag-style refactor.

## Recipe for one table (Variant A, entity rewrite)

Steps for an entity table whose `workspaceId` is mostly read at the service layer:

1. **Liquibase init.xml:**
   - Drop the `workspace_id` column from `<createTable>`.
   - Drop or update any indexes/unique constraints that included `workspace_id` (e.g. `(workspace_id, user_id, name)` becomes `(user_id, name)` or just `name`).
   - Append the standard `workspace_<entity>` table block (see "Standard relation-table shape" above).

2. **Entity Java class:**
   - Remove the `@Column("workspace_id") private long workspaceId;` field, getter, setter.
   - Constructor: change `<Entity>(long workspaceId, long userId)` to `<Entity>(long userId)` (or just no-arg if no userId either).
   - Update `toString()` to drop `workspaceId`.
   - Update class-level Javadoc — explain the workspace association now lives on `workspace_<entity>`.

3. **New entity:** `Workspace<Entity>.java` — copy `WorkspaceAiHubPersonalAgent.java` as a template, change the column/table names.

4. **New repository:** `Workspace<Entity>Repository.java` — copy `WorkspaceAiHubPersonalAgentRepository.java` template, change names.

5. **Existing repository:** queries that filter on `workspace_id` need to become `@Query` JOINs through the relation. Spring Data JDBC's string-based `@Query` does NOT support `Limit` — use plain `int limit` if needed.

6. **Service impl:**
   - Inject the new `Workspace<Entity>Repository`.
   - In `create()`: after `entityRepository.save(...)`, save a new `Workspace<Entity>(workspaceId, savedId)` row. Wrap in `try/catch (DataIntegrityViolationException)` for the membership unique constraint.
   - In `delete()`: cascading FK on the relation handles cleanup.
   - In list/find queries: they already JOIN through the relation, no extra population needed.

7. **Tests:**
   - Update unit tests: constructor signature change, mock setup for new repository field. The unit-test mock may need `WorkspaceXxxRepository.findByXxxId(...)` to return a fake membership for "ownership check" tests to pass.
   - Update int test: must add `@EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")` on the inner JDBC test config — without it the audit `created_date`/`created_by` columns are null and inserts fail NOT NULL. Save both rows when seeding fixtures.
   - GraphQL/REST controller tests: just constructor signature change.

8. **Verify:**
   ```bash
   ./gradlew :<module>:compileJava :<module>:compileTestJava
   ./gradlew :<module>:test :<module>:testIntegration
   ./gradlew spotlessApply -p <module-parent>
   ```

## When the entity is read by deep infrastructure (alternative to constructor sweep)

For tables whose `getWorkspaceId()` is called from many modules (`ai_hub_task` was the canonical case — `AiHubRoutingAgent`, `WebhookBridgeAgent`, REST/GraphQL controllers, artifact service all read it), the **service helper** approach scales without leaking workspace-state onto the entity:

1. **Service helper:** add `long getWorkspaceId(long entityId)` to the service interface. Impl looks up the membership row:
   ```java
   public long getWorkspaceId(long entityId) {
       return workspace<Entity>Repository.findBy<Entity>Id(entityId)
           .orElseThrow(() -> new NotFoundException("No workspace membership row for <table> id=" + entityId))
           .getWorkspaceId();
   }
   ```

2. **Sweep callsites:** every `entity.getWorkspaceId()` becomes `<service>.getWorkspaceId(entity.getId())`. Use `perl -i -pe 's/task\.getWorkspaceId\(\)/taskService.getWorkspaceId(task.getId())/g'` for the bulk and review the diff.

3. **For ownership checks** (`task.getWorkspaceId() != requesterWorkspaceId`): use `workspace<Entity>Repository.findByWorkspaceIdAndAiHubTaskId(requesterWorkspaceId, task.getId()).isEmpty()` instead. Returning empty means "this workspace doesn't claim that entity" — equivalent to the old inequality.

The cost is per-callsite churn but the entity stays clean and tests stay simple. **Don't use a @Transient shim** — the earlier task experiment was reverted because the entity-with-runtime-only-field is a worse mental model than just "the entity has no workspace; ask the relation".

## Common gotchas (learned the hard way during the 2-table session)

- **`@EnableJdbcAuditing` on int-test config** is NOT inherited from `AbstractIntTestJdbcConfiguration`. You must put it on the per-test `@Configuration public static class IntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration { }` block. Forget it and the audit columns fail NOT NULL.
- **Spring Data JDBC string `@Query` rejects `Limit`** with `UnsupportedOperationException: Queries with Limit are not supported using string-based queries`. Use `int limit` and `LIMIT :limit` in the SQL.
- **Mockito `@InjectMocks` picks up the new field automatically** if you add a `@Mock` of the right type; you don't need to touch the test fixture body unless it has a manual constructor call (search for `new <Service>Impl(`).
- **Spring Data JDBC won't load `@Transient` fields** — the service is solely responsible for populating them. Forgetting to call `populateWorkspaceId` in a new load path leaves `getWorkspaceId() == 0L` silently. Audit existing load paths when adding new ones.
- **Entity package-private setters** (e.g. `void setWorkspaceId(long)`) MUST be made `public` for Variant B — the service is in another package.
- **Liquibase changeset stability:** since the user said edit init in place, the changeset id stays the same. Existing dev DBs with the old schema will FAIL the checksum check on next startup. Document the manual `DROP TABLE` workaround in the commit message, or include a `<preCondition>` so the changeset only runs on fresh DBs (preferred for shared dev DBs).

## Resolved design questions

- **2026-05-06: ai_llm_usage / ai_gateway_spend_summary — Variant A, no denormalized column.** Cost-dashboard queries take the JOIN; pattern consistency wins over per-query overhead. Index `workspace_ai_llm_usage.workspace_id` and `workspace_ai_gateway_spend_summary.workspace_id` so the JOIN is index-only. Insert-path membership rows are mandatory and must share a transaction with the parent insert.

## What this spec is NOT

- Not a migration plan for live data — assumes dev-only schema, edited in place per user instruction. If a deployment has run the existing init migrations, they need DROP + re-run, OR a separate "drop column / add table" migration sequence.
- Not a plan for `ai_gateway_tag` — that's flagged for its own dedicated spec.
