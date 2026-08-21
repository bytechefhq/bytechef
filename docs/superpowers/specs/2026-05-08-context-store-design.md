# Context Store — design

**Status**: Draft
**Date**: 2026-05-08
**Owner**: Ivica Cardic
**Inspired by**: Airbyte AI Agents — Context Store concept

## 1. Concept

A workspace-scoped, periodically-refreshed, structured replica of selected entities from connected sources, exposed to AI agents as Spring AI `FunctionToolCallback`s and to workflows as a synthetic `contextStore` component (`search` / `get` actions). Agents query the indexed replica instead of hitting live APIs — sub-millisecond reads, no rate-limit risk, deterministic results. Writes still go through normal component actions; the next sync replicates them back into the store.

The design aligns 1:1 with ByteChef's existing tool-calling spine (`McpServer → McpComponent → McpTool → AbstractToolFacade`) and reuses Spring Batch as the sync engine — the same foundation the existing `data-stream` component uses, but **without** wrapping in DataStream's component layer.

## 2. Why

Today, an AI agent answering "find Acme contacts who haven't replied in 30 days" calls `hubspot.searchContacts` — paginated, rate-limited, seconds of latency, can fail mid-stream. With Context Store, the same intent becomes one filter on a local indexed replica — milliseconds, no API contact, deterministic. The agent doesn't know it's hitting a replica vs. live API; the tool surface is what changes.

ByteChef has all the building blocks today (knowledge-base for semantic RAG, data-table for workflow state, 180+ component connectors, polling triggers, MCP server, AI gateway) but no unified, periodically-refreshed, agent-queryable replica of source-entity data. That's the gap this spec fills.

## 2a. Relationship to Knowledge Base (and how Airbyte differs)

ByteChef has two adjacent primitives for "data agents can query" — and they're complementary, not overlapping. The distinction holds **even after** the §10 semantic-search add-on is built, because the abstractions diverge on more than just "where the data came from."

### The load-bearing difference: chunking and identity

| | Context Store (this spec, MVP + §10) | Knowledge Base (existing, `platform-knowledge-base` post-2026-05-09 relocation) |
|---|---|---|
| **Unit of indexing** | Whole record (one source-system entity) | Document chunk (typically 512 tokens, configurable) |
| **Chunking** | **No** — record granularity = source entity granularity | **Yes** — built-in recursive splitter; long documents become many chunks |
| **Schema** | Typed (`indexedFields`, `idField`) | Free-form text + opaque metadata |
| **Identity** | `(workspaceId, sourceId, entity, sourceRecordId)` — round-trips to the live source API | Internal `documentId`; no concept of upstream source identity |
| **Population** | Periodic Spring Batch sync from a connected source | User uploads files (PDF/Word/MD/text) |
| **Updates** | Auto-detected via `payload_hash`; re-embedded only if changed | Re-upload |
| **Query pattern** | Structured filter (often narrows first) ± optional semantic on text fields | Pure semantic similarity |
| **Right when…** | "Find Acme contacts whose `notes` mention pricing concerns" — needs `company.name = "Acme"` AND semantic | "Search across uploaded PDFs / handbooks / runbooks" |

**The acid test**: a Knowledge Base chunks. Context Store does not. If you embed a 50-page Notion page as one Context Store record, semantic retrieval is too coarse. If you chunk it, you've rebuilt KB inside CS. The two serve different content shapes — entity-shaped vs. document-shaped — and the chunking decision is the load-bearing one.

### What they share (infrastructure, not abstraction)

- `AiGatewayEmbeddingModelFactory` — gateway-routed per-workspace embedding model
- Spring AI `VectorStore` interface
- PgVector storage pattern (but **separate tables**: `kb_*` vs `cs_*` (Spring AI PgVectorStore-managed tables))
- Cost rollups via `AiLlmUsage`

Sharing infra is healthy. Sharing the abstraction would force one to grow features it doesn't want — KB doesn't want `(source, entity, sourceRecordId)` round-tripping; CS doesn't want chunking (would break `payload_hash` change-detect).

### Decision rule for users

**Does the data live in a connected source system?**
- Yes → Context Store. (With semantic on text fields if available.)
- No (uploaded directly) → Knowledge Base.

**Is the content document-shaped (long-form, prose, no structured schema)?**
- Yes → Knowledge Base, even if it came from a source system.
- No (entity-shaped — a contact, a deal, a ticket, a page-as-row) → Context Store.

### Airbyte for comparison

Airbyte's Context Store is structurally identical to ours-as-designed for MVP (structured filters, no semantic, periodic sync, per-source isolation). They have **no Knowledge Base equivalent** — no concept for uploaded unstructured docs. Their model is "everything is a structured entity from a connected source." A Notion page in Airbyte is a record with `pages`, `blocks`, `comments` entity types; same `context_store_search` operates on it.

ByteChef diverges by keeping both because real users have both needs: uploaded handbooks **and** SaaS data. Notion pages from the connector flow into Context Store (as structured entities — exactly like Airbyte). PDFs uploaded by hand stay in Knowledge Base. Users with rich text in source systems get filter+semantic via §10 — without "uploading the connector source to KB."

## 3. Scope

### In scope (MVP)

**Context Store (record sink — structured replica):**
- `ContextStoreSource` / `ContextStoreEntity` / `ContextStoreRecord` / `ContextStoreRecordIndex` domain.
- `ContextStoreQueryService` — structured filter API (eq/neq/in/contains/startsWith/gt/gte/lt/lte/between, sort, cursor pagination, projection, `includeDeleted`).
- **Auto-generated sync workflow** per Context Source: `[schedule.cronTrigger] → [data-stream.stream(SOURCE=<component>.ItemReader, DESTINATION=contextStore.writeToReplica)]`. Workflow is owned by the Context Source — created at "Add Context Source" time, trigger updated when cadence changes in the UI, deleted when the source is deleted. Workflow editor edits to a Context-Source-owned workflow are blocked. (Identifier on the workflow's metadata: `workflow.metadata.contextStoreSourceId`.)
- New DESTINATION cluster element `contextStore.writeToReplica` (under the `contextStore` component) — the only new code on the sync mechanism. Implements `ItemWriter<Map<String,Object>>` from the existing DataStream SPI: `open` / `write(List<Map>)` / `update` / `close` lifecycle. Internally does idempotent upsert into `context_store_record` keyed by `(workspaceId, sourceId, entityName, sourceRecordId)`, applies `storedFields` whitelist, computes `payload_hash`, tracks `seenIds` for tombstone-on-completion.
- Tombstone-on-completion: a `JobExecutionListener` registered on the DataStream job that detects `contextStore.writeToReplica` as the destination and calls `ContextStoreRecordService.tombstoneUnseen(...)` with the seenIds collected during the run.
- Atlas workflow engine handles execution distribution natively (DataStream tasks dispatch to Atlas Workers via standard workflow execution). No separate dispatch layer needed.
- Synthetic `contextStore` component (EE) with `search` / `get` Actions for workflow steps + `BaseToolFunction.TOOLS` cluster elements for AI Agent step usage.
- `ContextStoreToolFacade extends AbstractToolFacade` — used by the `McpServer`-driven enumeration path.
- AiHub EE wiring — **two halves**:
  - **Consume** (read-side, agent retrieves from existing sources): `SearchContextStoreToolCallback`, `ListContextSourcesToolCallback`, `GetContextStoreRecordToolCallback`.
  - **Define** (manage-side, agent configures sources via chat — same operations the "Add Context Source" UI exposes): `CreateContextStoreSourceToolCallback`, `UpdateContextStoreSourceToolCallback`, `DeleteContextStoreSourceToolCallback`, `RefreshContextStoreSourceToolCallback`, `SetContextStoreSourceEnabledToolCallback`. Plus two discovery tools that let the LLM explore the source-component catalog before creating: `ListAvailableSourceComponentsToolCallback` (returns components that have an `ItemReader` cluster element — i.e., are syncable into Context Store) and `DescribeSourceComponentEntitiesToolCallback` (returns the schema/fields a given source component emits, so the LLM can pick reasonable `idField` and `indexedFields`).
  - All define-side callbacks delegate through `WorkspaceContextStoreSourceFacade` (Task 14) — same code path the GraphQL controllers and UI use; no parallel implementation.
  - **Authorization**: define-side callbacks require workspace admin role (`@PreAuthorize("hasRole('ROLE_ADMIN')")` on the facade method), matching the GraphQL `refreshContextStoreSource` mutation's authorization model.
  - **Chat-level user confirmation** (per the platform's explicit-permission doctrine for actions that modify workspace infrastructure): the CC routing agent must obtain user confirmation in the chat thread before executing any define-side tool call. The tool callbacks themselves can either (a) refuse to execute when called without an explicit user-confirmation token in their input, or (b) trust the routing agent's pre-confirmation prompt — pick whichever pattern matches existing CC tools that mutate workspace state (`CreateProjectDeploymentToolCallback`, `RollbackProjectDeploymentToolCallback`, `ToggleProjectDeploymentToolCallback`, etc.). Match precedent.
- GraphQL CRUD + `refreshNow` mutation (admin-only via `@PreAuthorize`).
- Tombstone-on-disappear (`deletedAt`) with `includeDeleted` query flag for audit.
- Optional per-entity field whitelist via `storedFields` JSONB column (selective curation).

**Knowledge Base Source (document sink — KB-populating sibling, see §12):**
- `KnowledgeBaseSource` entity on the new `platform-knowledge-base-api` module (post-2026-05-09 Knowledge Base relocation — see §16). Source-side mirrors `ContextStoreSource` post-pivot (no `workspace_id` on the entity; `name`, `sourceComponentName/Version`, `readerStrategy`, `connectionId`, `cadence`, `status`, `enabled`, audit, `@Version`) plus a non-null `knowledgeBaseId` AggregateReference targeting the existing `knowledge_base` table (now also in `platform-knowledge-base-api`). Sync history is captured by Atlas's standard JobExecution rows for the auto-generated workflow.
- `WorkspaceKnowledgeBaseSource` relation entity on `automation-knowledge-base-api` mirroring the existing `WorkspaceKnowledgeBase` shape (`id`, `workspace_id`, `knowledge_base_source_id`, audit, `@Version`, UNIQUE `(workspace_id, knowledge_base_source_id)`, FK ON DELETE CASCADE on `knowledge_base_source_id`). Workspace scoping flows through this relation table — same precedent as `workspace_connection` / `workspace_knowledge_base` / `workspace_context_store_source`.
- **Inline sync metadata on `knowledge_base_document`** (no separate join entity, no per-entity templates layer): five nullable columns — `source_id BIGINT` (FK to `knowledge_base_source.id` ON DELETE SET NULL), `source_record_id VARCHAR(512)`, `synced_payload_hash VARCHAR(16)`, `last_seen_at TIMESTAMP WITH TIME ZONE`, `deleted_at TIMESTAMP WITH TIME ZONE`. The `knowledge_base_document` table itself now lives in the platform Liquibase changelog (per the 2026-05-09 KB-to-platform move); the five sync columns are added by the same platform-side Liquibase changeset that creates `knowledge_base_source`. Manual uploads keep all five NULL. Partial UNIQUE index `(source_id, source_record_id) WHERE source_id IS NOT NULL` enforces idempotent upsert without affecting manual docs.
- **Auto-generated sync workflow** per Knowledge Base source: `[schedule.cronTrigger] → [data-stream.stream(SOURCE=<component>.ItemReader, DESTINATION=knowledgeBase.writeAsDocument)]`. Same ownership/lifecycle semantics as Context Store sync workflows. Auto-generation lives in `automation-knowledge-base-service` (workspace-aware orchestration is automation-side per the same "no workspace logic in platform" rule that applies to CS — see spec §16).
- New DESTINATION cluster element `knowledgeBase.writeAsDocument` on the existing `knowledgeBase` component (`server/libs/modules/components/ai/vectorstore/knowledgebase/`) — change-detection via `synced_payload_hash` (mirrors CS): unchanged records bump `last_seen_at` only (no chunker re-run); changed records replace document content + re-trigger the existing chunker via the platform `KnowledgeBaseDocumentEvent`. Writer takes only `sourceId` + `mode` via inputParameters (no workspace lookup; workspace is implicit via the relation table — same shape as `contextStore.writeToReplica`).
- Tombstone-on-completion via `KnowledgeBaseSourceSyncJobListener` that lives in `platform-knowledge-base-service` (mirrors `ContextStoreSyncJobListener`'s placement in `platform-context-store-service`): on COMPLETED + `mode = FULL_REPLACE`, set `deleted_at = now` for synced documents whose `source_record_id` wasn't in seenIds. Listener operates by `source_id` only — no workspace lookup. Existing `query_knowledge_base` retrieval filters `WHERE deleted_at IS NULL` so synced and manually-uploaded docs coexist transparently in the same KB.
- GraphQL CRUD on `automation-knowledge-base-graphql` for `KnowledgeBaseSource` + extend the existing `KnowledgeBaseDocument` GraphQL type with `sourceId`, `sourceRecordId`, `lastSeenAt`, `deletedAt` so the UI can show "synced from Notion" badges. The GraphQL controller's mutations call `WorkspaceKnowledgeBaseSourceFacade` (in `automation-knowledge-base-service`); read paths call `WorkspaceKnowledgeBaseSourceService`. Imports cross both `com.bytechef.platform.knowledgebase.*` (entity types) and `com.bytechef.automation.knowledgebase.*` (workspace-aware facade + service).
- "Add Knowledge Base Source" UX (parallel to "Add Context Source", but with no per-entity step — sources produce documents directly).

**Semantic search add-on (record sink, gated; ships after Phase 13 — see §10):**
- `cs_vector_store` table (Spring AI `PgVectorStore`-managed; auto-initialized at startup) + `ContextStoreSemanticBatchListener` that re-embeds records whose `payload_hash` differs from the last-embedded hash; failures don't fail the structured sync.
- `ContextStoreSemanticSearchService` with cosine similarity over PgVector + optional structured prefilter (hybrid retrieval).
- Per-(source, entity) `semantic_search_<source>_<entity>` `FunctionToolCallback`s minted by `ContextStoreToolFacade` — appear when an `EmbeddingModel` bean is present AND the entity has `semanticIndexFields` configured. CC EE registers the same callbacks on its read-only ASK agent.
- Deployments without `EmbeddingModel` configured remain fully functional on the structured surface alone — the entire semantic stack is `@ConditionalOnBean(EmbeddingModel.class)`.

**Client-side UI (in MVP — see Phase 15 of the implementation plan):**
- Workspace-scoped + environment-scoped Context Store sources list + detail pages, reachable at `/automation/context-store` (EE-only, gated on `ff_4855`), with status badges (BUILDING_PREVIEW / PREVIEW / READY / FAILED / DISABLED), last-sync timestamp, entity count, refresh-now button, edit/delete/enable-disable actions, and inline entity CRUD on the detail page (admin-only; uses dedicated entity GraphQL mutations).
- "Add Context Source" guided dialog (5-7 steps: connection → reader strategy → entity name + idField + indexedFields → cadence → review → submit). The connection picker is filtered via a new `dataStreamCompatibleConnections` query so only connections whose component exposes `ItemReader` cluster elements or `list*` actions appear.
- Knowledge Base Source list reachable as a **"Sources" tab inside the KB detail page** (`/automation/knowledge-bases/:id`), NOT a sibling top-level page — a `KnowledgeBaseSource` always belongs to a specific KB, so embedding the list under the parent KB groups related data. Same Add-Source dialog shape as CS, minus the per-entity step.
- Existing KB document list gains a "Sync source" badge column populated from `knowledge_base_document.source_id`; clicking it navigates to the source detail page.
- A shared `SyncSourceStatusBadge` component (in `client/src/shared/components/`) renders the 5-value status enum identically across CS and KB-Source pages.
- Recent-sync-run history is **deferred to v2**; the source detail page shows only the last run's metadata, with a click-out to the existing Atlas Workflow Executions page for older history.
- Cadence picker uses preset chips (`@manual` / `@hourly` / `@daily` / `@weekly`) + a custom-cron text input that defers validation to the server — **no client-side cron library**.

### Explicitly out of scope (MVP)

- Cross-workspace / organization-tier sharing of sources.
- Write-back operations (`create` / `update` / `delete` on the source via Context Store). Writes go through normal component actions; matches Airbyte's "writes bypass the store."
- CDC / webhook-driven freshness — schedule-based only.
- Tombstone-audit management UI — admin GraphQL is enough for v1.
- Per-agent ACL on which sources/entities a personal agent can query — future enhancement.
- Public REST API — post-MVP EE phase, see §11. MVP exposes only GraphQL (workspace-internal) and tool-callback paths.
- ClickHouse alternative store — post-MVP, see §12a. MVP stores everything in Postgres (JSONB + sidecar typed index).
- Atlas dispatch wrapper as a separate post-MVP phase — superseded. With the DataStream pivot, Atlas already executes Context Store sync workflows natively (any Worker can pick up a `data-stream.stream` task containing the `contextStore.writeToReplica` DESTINATION). Distribution is built-in; no wrapper layer required.
- Incremental sync (`since: Instant`) at the **SPI level shipped in Phase 17** as a DataStream extension — `ItemReader.SINCE_KEY` constant + default-false `supportsIncremental()` capability flag, plus delegate-side JobParameter→ExecutionContext plumbing and listener-side `lastSyncStart` write on COMPLETED. **Auto-wiring of incremental sync into the CS / KB-Source workflow generators is deferred to Phase 17b** — MVP and Phase 17 ship workflows that do FULL_REPLACE every run; users opt into incremental by editing their workflow YAML manually to add the `datastream.since` JobParameter (sourced from the previous run's persisted `lastSyncStart` via SpEL). Tombstone interaction (incremental alone can't derive deletes) is also a Phase 17b concern — see plan Phase 17 for the periodic-FULL_REPLACE + change-feed-events options.

## 4. Module layout — Option B (everything EE) + 2026-05-09 platform-CS pivot

**Pivot 2026-05-08**: this spec originally proposed Option C (data plane CE, agent plane EE) — see decision log. The owner subsequently chose **Option B**: place the entire feature under EE. Mirrors Airbyte Cloud's positioning where the agent platform is paid-only, leaving Airbyte OSS for raw data integration. ByteChef CE retains data movement (Spring Batch + DataStream component); Context Store is positioned as an EE feature exclusively.

**Pivot 2026-05-09**: CS core moved from `server/ee/libs/automation/automation-context-store/{api,service}/` to `server/ee/libs/platform/platform-context-store/{api,service}/` per the Connection/WorkspaceConnection precedent. `workspace_id` columns were dropped from `context_store_source` and `context_store_record`; a `workspace_context_store_source` relation table was added. See §16 decision log.

**Pivot 2026-05-09 (later same day)**: workspace-aware orchestration (workflow auto-generation, ProjectDeploymentWorkflow lifecycle, manual job dispatch, per-workspace tool-callback enumeration) moved out of the platform module per the "workspace-related logic stays in automation" rule. The `ContextStoreWorkspaceResolver` SPI was deleted; the platform `ContextStoreSourceFacade` (briefly introduced in commit `baa1f1fe311` between the two pivots) was deleted entirely (all 5 of its methods were workspace-aware); a new `WorkspaceContextStoreSourceFacade` in `automation-context-store-{api,service}` carries the orchestration. `ContextStoreToolFacade` moved from platform to automation alongside. Platform-CS is now strictly pure data plane: entities, repositories, CRUD services, query service (no `workspaceId` parameter), sync writer, sync listener. The slimmed `automation-context-store-{api,service}` modules carry the workspace-relation entity + repo/service AND the workflow-orchestration facade + tool facade. See §16 decision log.

**Pivot 2026-05-09 (Knowledge Base)**: Knowledge Base itself moved from automation to platform per the same Connection/WorkspaceConnection precedent (commit `5cee82ab933`). The KB module tree was reshaped: `platform-knowledge-base-{api,service,file-storage,rest,worker}/` host the entity + services + facades + file storage + REST + chunker/embedder ETL pipeline; `automation-knowledge-base-{api,service,graphql}/` host only `WorkspaceKnowledgeBase` (relation) + `WorkspaceKnowledgeBaseService` + `WorkspaceKnowledgeBaseFacade` + the GraphQL controllers + auxiliary classes. **The same pattern applies to KB-Source — see §12 module layout.** KB-Source rides on the same module tree as KB itself (no separate `platform-knowledge-base-source` module): `KnowledgeBaseSource` entity + repo + service + sync listener + sync helpers live on `platform-knowledge-base-{api,service}`; the `WorkspaceKnowledgeBaseSource` relation + workspace-aware facade live on `automation-knowledge-base-{api,service}`; the GraphQL controller stays on `automation-knowledge-base-graphql`. See §16 decision log.

```
EE platform — pure data plane (post-2026-05-09 second pivot):
  server/ee/libs/platform/platform-context-store/
    platform-context-store-api/          ← domain (no workspace_id on entities), query DSL,
                                           ContextStoreSourceService / ContextStoreEntityService /
                                           ContextStoreRecordService / ContextStoreQueryService interfaces.
                                           NO facade. NO tool facade. NO SPI.
    platform-context-store-service/      ← entity-CRUD service impls (no workspace concerns):
                                             - ContextStoreSyncJobListener (tombstone-on-completion via JobExecutionListener;
                                               mode-aware per spec §6 — operates by source_id, no workspace lookup)
                                             - Liquibase (4 platform tables + workspace_context_store_source relation
                                               + the deferred semantic embedding table)
                                             - ContextStoreQueryService impl (signature: search(sourceId, entityName, ...);
                                               no workspaceId param. Caller authorizes workspace ownership upstream.)
                                             - Semantic batch listener / query service (gated @ConditionalOnBean(EmbeddingModel.class))
                                             - cs_vector_store auto-managed by Spring AI PgVectorStore at startup; no Liquibase changeset

EE automation — workspace-relation + workspace-aware orchestration + GraphQL:
  server/ee/libs/automation/automation-context-store/
    automation-context-store-api/        ← WorkspaceContextStoreSource (relation entity),
                                           WorkspaceContextStoreSourceRepository,
                                           WorkspaceContextStoreSourceService interface (CRUD on the relation
                                           + getAllSourcesByWorkspaceId / getAllEnabledSourcesByWorkspaceId joining
                                           through to platform sources),
                                           WorkspaceContextStoreSourceFacade interface (workspace-aware orchestration:
                                           create(workspaceId, ...) / update(workspaceId, ...) /
                                           delete(workspaceId, ...) / refreshNow(workspaceId, ...) /
                                           setEnabled(workspaceId, ...) — auto-generates workflows, manages
                                           ProjectDeploymentWorkflow, dispatches manual jobs),
                                           ContextStoreToolFacade interface (per-(source, entity) tool callbacks
                                           for an enumerated workspace's sources)
    automation-context-store-service/    ← WorkspaceContextStoreSourceServiceImpl,
                                           WorkspaceContextStoreSourceFacadeImpl (uses platform
                                           ContextStoreSourceService for entity CRUD + WorkspaceContextStoreSourceRepository
                                           for relation insert/delete + atlas-coordinator for workflow dispatch),
                                           ContextStoreToolFacadeImpl
    automation-context-store-graphql/    ← workspace CRUD + refreshNow mutation
                                           (controller calls WorkspaceContextStoreSourceFacade for mutations; uses
                                           WorkspaceContextStoreSourceService for read paths; resolves source-id-only
                                           inputs to workspaceId via WorkspaceContextStoreSourceService.fetchWorkspaceId
                                           ByContextStoreSourceId before calling workspace-aware facade methods)
    automation-context-store-public-rest/ ← post-MVP, see §11

EE — synthetic component (workflow Actions + BaseToolFunction.TOOLS cluster elements + DataStream DESTINATION):
  server/ee/libs/modules/components/context-store/                    ← stays under modules/components/, not platform
    src/main/java/com/bytechef/ee/component/contextstore/
      ContextStoreComponentHandler.java          (@AutoService(ComponentHandler.class))
      action/ContextStoreSearchAction.java       ← Action: search   (workflow steps)
                                                    uses ContextStoreWorkspaceResolver to resolve workspaceId
                                                    when constructing workspace-scoped queries
      action/ContextStoreGetAction.java          ← Action: get      (workflow steps)
      tool/ContextStoreSearchTool.java           ← BaseToolFunction.TOOLS cluster element (AI Agent steps)
      tool/ContextStoreGetTool.java              ← BaseToolFunction.TOOLS cluster element
      destination/ContextStoreItemWriter.java    ← DataStream DESTINATION cluster element with mode parameter
                                                    (FULL_REPLACE | PARTIAL); implements ItemWriter<Map<String,Object>>
                                                    upserts into context_store_record + tombstone tracking

EE — AiHub wiring (mirrors existing CC tool-callback idiom):
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/
    src/main/java/com/bytechef/ee/automation/aihub/tool/
      # Consume tools (read-side):
      SearchContextStoreToolCallback.java
      ListContextSourcesToolCallback.java
      GetContextStoreRecordToolCallback.java
      SemanticSearchContextStoreToolCallback.java     ← @ConditionalOnBean(EmbeddingModel.class)
      # Define tools (manage-side, same operations as the Add Context Source UI):
      CreateContextStoreSourceToolCallback.java       ← chat-driven equivalent of "Add Context Source"
      UpdateContextStoreSourceToolCallback.java       ← change name/cadence; updates trigger on auto-generated workflow per spec §6
      DeleteContextStoreSourceToolCallback.java       ← drops source + auto-generated workflow + cascading entities/records
      RefreshContextStoreSourceToolCallback.java      ← manual sync trigger; equivalent of GraphQL refreshContextStoreSource mutation
      SetContextStoreSourceEnabledToolCallback.java   ← enable/disable the workflow's ProjectDeploymentWorkflow without deleting
      # Discovery tools (let the LLM explore the catalog before creating):
      ListAvailableSourceComponentsToolCallback.java  ← returns components with ItemReader cluster elements
      DescribeSourceComponentEntitiesToolCallback.java ← returns the schema a given source component's reader emits
    + *Test.java siblings + registration in the existing CC routing-agent aggregation point
```

**EE Code Conventions** (per CLAUDE.md, applied to every file under `server/ee/`):
- ByteChef Enterprise license header (NOT Apache 2.0)
- `@version ee` Javadoc tag on every class
- Package `com.bytechef.ee.automation.contextstore.*` (and `com.bytechef.ee.component.contextstore.*` for the component)

**Why everything EE:**

1. **Mirrors Airbyte Cloud's positioning.** Airbyte OSS handles data movement. Airbyte Cloud is the paid platform offering Agents + Context Store. ByteChef applies the same line: Spring Batch + DataStream stay CE for raw data movement; Context Store (the indexed-replica + agent-facing layer) is paid.
2. **Single tier; cleaner story.** Option C's CE/EE split was clever but added module boundaries that consumers would have to reason about. Option B is "Context Store is one feature, all in EE." Easier to document, ship, and bill.
3. **No gateway code dependency anywhere.** Spring AI's `EmbeddingModel` interface remains the seam. The semantic add-on continues to gate on `@ConditionalOnBean(EmbeddingModel.class)` so EE deployments without an embedding model still get the structured Context Store. EE deployments with the gateway provided get per-workspace embedding routing transparently.
4. **The previous separate `automation-context-store-tool-{api,service}` EE modules collapse into the main `automation-context-store-{api,service}` modules.** The split only made sense in Option C as the EE half of a CE/EE boundary. With everything EE, it's redundant.

**Decision log entry** for this pivot: see §15.

### 4a. When to use the platform/`Workspace<Entity>`-relation pattern (and when NOT to)

ByteChef has an established pattern for entities that need workspace scoping:

- **Platform-package entities** (e.g. `Connection` in `server/libs/platform/platform-connection/`) live without `workspace_id` on the entity itself; a sibling **automation-package relation table** (e.g. `WorkspaceConnection` in `server/libs/automation/automation-configuration/`) holds `(workspace_id, <entity>_id)`. Workspace-scoped queries join through the relation.
- **Automation-package entities** (e.g. `KnowledgeBase`, `ContextStoreSource`) inline `workspace_id` directly on the entity. No relation table.

The placement choice (platform vs automation, which then determines the workspace pattern) is **driven by whether there's a real case for the entity to be platform-shared** — i.e., consumed by multiple automation contexts independently.

**Rule of thumb**:

| Entity is… | Place in… | Workspace pattern |
|---|---|---|
| Genuinely platform-shared — multiple automation domains consume it independently (e.g. `Connection` is referenced by workflows, projects, OAuth flows, integrations) | `platform-*` module | No `workspace_id` on entity; `Workspace<Entity>` relation table in automation |
| Tightly tied to one automation feature (e.g. `KnowledgeBaseSource` is bound to `KnowledgeBase`; `ContextStoreSource` is bound to the Context Store sync surface) | `automation-*` module | Inline `workspace_id` directly on the entity |

**For this design specifically:**

- `ContextStoreSource`, `ContextStoreEntity`, `ContextStoreRecord`, `ContextStoreRecordIndex` — **platform-placement** under `server/ee/libs/platform/platform-context-store/{api,service}/`. The owner pivoted on 2026-05-09 (after the initial automation-placed shipping in commits `b7d92328c30`...`b1f2cb3471c`) to apply the Connection/WorkspaceConnection precedent to CS too — see §16 "Move CS to platform" entry. The slimmed `server/ee/libs/automation/automation-context-store/{api,service}` module now hosts only the `WorkspaceContextStoreSource` relation entity + repo + service AND the workspace-aware `WorkspaceContextStoreSourceFacade` + `ContextStoreToolFacade` (post-second-pivot 2026-05-09; see §16). `automation-context-store-graphql` stays in automation per "in automation only leave graphql" rule. `workspace_id` is dropped from `context_store_source` and `context_store_record`; workspace scoping flows through `workspace_context_store_source`.
- **Knowledge Base** (the entity + its services + facades + file storage + REST + worker + ETL pipeline) — **platform-placement** under `server/libs/platform/platform-knowledge-base/{api,service,file-storage,rest,worker}/` (commit `5cee82ab933`, 2026-05-09 — see §16). The slimmed `server/libs/automation/automation-knowledge-base/{api,service,graphql}` module now hosts only `WorkspaceKnowledgeBase` (relation), `WorkspaceKnowledgeBaseService`, `WorkspaceKnowledgeBaseFacade`, the GraphQL controllers (which import platform types), and the auxiliary `KnowledgeBaseDocumentNotFoundException` + `KnowledgeBaseJdbcRepositoryConfiguration`. `knowledge_base` does NOT have `workspace_id`; workspace scoping flows through `workspace_knowledge_base`.
- `KnowledgeBaseSource` (Phase 13) — **platform-placement** as an extension of `platform-knowledge-base-{api,service}` (NOT a separate `platform-knowledge-base-source` module — the source entity is a sub-domain of KB and rides on the same module tree). The relation entity `WorkspaceKnowledgeBaseSource` lives in `automation-knowledge-base-api` alongside the existing `WorkspaceKnowledgeBase`. `knowledge_base_source` does NOT have `workspace_id`; workspace scoping flows through `workspace_knowledge_base_source`.
- `ContextStoreRecordEmbedding` (Phase 14 sidecar) — platform-placement (1:1 sidecar to `ContextStoreRecord`); inherits workspace via FK chain through the parent record + the parent's relation row.
- The 5 inline sync columns on `KnowledgeBaseDocument` (`source_id`, `source_record_id`, `synced_payload_hash`, `last_seen_at`, `deleted_at`) — added directly to the platform-side `knowledge_base_document` table by the platform Liquibase changeset that introduces `knowledge_base_source` (Phase 13 Task 29). Workspace inheritance is via the chain `knowledge_base_id → knowledge_base.id → workspace_knowledge_base.workspace_id` (post-KB-relocation; the chain no longer terminates at a `knowledge_base.workspace_id` column).

The 2026-05-09 pivots (CS + KB) reverse the earlier "only if there's a case for it" guidance for CS, KB, and KB-Source specifically. The owner's call: applying the platform-split pattern uniformly across the sync surface is cleaner long-term than leaving CS automation-placed while KB-Source goes platform, AND consolidates KB's status as a platform-shared primitive (workflows, projects, OAuth flows reference KB just as they reference Connection — see §16 KB entry). The general "only if there's a case" guidance still applies to *future* entities; CS, KB, and KB-Source are special-cased.

## 5. Domain model

```
ContextStoreSource                                                  ← lives in platform-context-store-api (post-2026-05-09 pivot)
  id                          BIGSERIAL
  ‹ no workspace_id column ›                                        ← workspace scope flows through workspace_context_store_source relation
  name                        VARCHAR(256) NOT NULL
  source_component_name       VARCHAR(256) NOT NULL
  source_component_version    INT NOT NULL
  reader_strategy             INT NOT NULL                         ← enum ordinal: CLUSTER_ELEMENT(0) | LIST_ACTION(1)
  source_cluster_element_name VARCHAR(256)                         ← when reader_strategy = CLUSTER_ELEMENT
  source_list_action_name     VARCHAR(256)                         ← when reader_strategy = LIST_ACTION
  connection_id               BIGINT                               ← AggregateReference<Connection, Long>
  cadence                     VARCHAR(64) NOT NULL                 ← '@manual' | '@hourly' | '@daily' | cron expr
  status                      INT NOT NULL                         ← BUILDING_PREVIEW(0) | PREVIEW(1) | READY(2) | FAILED(3) | DISABLED(4)
  enabled                     BOOLEAN NOT NULL DEFAULT TRUE
  last_sync_run_at            TIMESTAMP WITH TIME ZONE
  workflow_id                 BIGINT                               ← AggregateReference<Workflow, Long> to the auto-generated sync workflow; nullable for legacy reasons but normally populated immediately after source creation
  last_sync_job_execution_id  BIGINT                               ← FK to Spring Batch BATCH_JOB_EXECUTION (logical, no FK)
  created_by, created_date, last_modified_by, last_modified_date, version
  UNIQUE (name) per the post-2026-05-09 pivot
  (was UNIQUE (workspace_id, name); workspace scope now via workspace_context_store_source relation)

WorkspaceContextStoreSource                                         ← lives in automation-context-store-api (relation entity per
                                                                      Connection/WorkspaceConnection precedent)
  id                          BIGSERIAL
  workspace_id                BIGINT NOT NULL
  context_store_source_id     BIGINT NOT NULL
  created_by, created_date, last_modified_by, last_modified_date, version
  UNIQUE (workspace_id, context_store_source_id)
  FK context_store_source_id  → context_store_source.id ON DELETE CASCADE
  (No FK on workspace_id, mirroring workspace_connection precedent.)

ContextStoreEntity
  id                          BIGSERIAL
  source_id                   BIGINT NOT NULL                      ← AggregateReference<ContextStoreSource, Long>
  entity_name                 VARCHAR(256) NOT NULL                ← e.g. "contacts"
  description                 TEXT
  id_field                    VARCHAR(256) NOT NULL                ← e.g. "id"
  stored_fields               JSONB                                ← {fields: [...]} dotted-path whitelist; null = full payload retained
  indexed_fields              JSONB NOT NULL                       ← {<dotted-path>: <type>} typed sidecar index (Map<String,String>: TEXT|NUMERIC|TIMESTAMP)
  semantic_index_fields       JSONB                                ← {fields: [...]} dotted paths to embed for §10 semantic add-on; null/empty = no embeddings for this entity
  parameters                  JSONB                                ← passed to the reader
  created_*, last_modified_*, version
  UNIQUE (source_id, entity_name)

ContextStoreRecord                                                  ← lives in platform-context-store-api (post-2026-05-09 pivot)
  id                          BIGSERIAL
  ‹ no workspace_id column ›                                        ← workspace scope inherited via source_id → context_store_source.id
                                                                     → workspace_context_store_source(context_store_source_id, workspace_id)
  source_id                   BIGINT NOT NULL
  entity_name                 VARCHAR(256) NOT NULL
  source_record_id            VARCHAR(512) NOT NULL                ← original primary key from source
  payload                     JSONB NOT NULL
  payload_hash                VARCHAR(16) NOT NULL                 ← SHA-256 first 8 bytes hex (16 chars); for change detection (memory rule: prefer SHA-256 first 8 bytes for deterministic long IDs)
  last_seen_at                TIMESTAMP WITH TIME ZONE NOT NULL    ← updated on each sync sweep that includes the record
  deleted_at                  TIMESTAMP WITH TIME ZONE             ← tombstone; set when sweep completes without seeing the record
  created_date, last_modified_date
  UNIQUE (source_id, entity_name, source_record_id) per the post-2026-05-09 pivot
  (was UNIQUE (workspace_id, source_id, entity_name, source_record_id); workspace ownership is implicit via source_id)
  INDEX gin_payload  USING GIN (payload)
  INDEX btree(source_id, entity_name, source_record_id)
  INDEX btree(deleted_at) WHERE deleted_at IS NOT NULL

ContextStoreRecordIndex
  id                          BIGSERIAL
  record_id                   BIGINT NOT NULL                      ← FK to ContextStoreRecord
  field_name                  VARCHAR(256) NOT NULL
  value_text                  TEXT
  value_numeric               NUMERIC
  value_timestamp             TIMESTAMP WITH TIME ZONE
  INDEX btree(record_id, field_name)
  INDEX btree(field_name, value_text)      WHERE value_text IS NOT NULL
  INDEX btree(field_name, value_numeric)   WHERE value_numeric IS NOT NULL
  INDEX btree(field_name, value_timestamp) WHERE value_timestamp IS NOT NULL

ContextStoreSyncRun
  REMOVED — sync history is captured by Atlas's standard JobExecution rows for the auto-generated workflow.
  Friendly status (BUILDING_PREVIEW / PREVIEW / READY / FAILED) is computed from
  ContextStoreSource.status (which is updated by the JobExecutionListener on each sync completion)
  and the latest JobExecution row for the source's workflow.
```

**Workspace placement** follows the Connection/WorkspaceConnection precedent post-2026-05-09 pivot: `platform-context-store-*` owns the core entity (no `workspace_id` column on `context_store_source` or `context_store_record`); `automation-context-store-api` hosts a `WorkspaceContextStoreSource` relation entity mapping `(workspace_id, context_store_source_id)`. Workspace-scoped queries join through the relation. **All workspace-aware orchestration lives in automation** — `WorkspaceContextStoreSourceFacade` (in automation) wraps platform `ContextStoreSourceService` (entity CRUD only). The platform module has no SPI seam back to automation; the dependency direction is automation → platform, exactly the legal direction.

**Enum ordinals** are pinned by an `EnumOrdinalStabilityTest` per the project convention; new values append at the end.

**Cross-table uniqueness**: post-2026-05-09 pivot, the record-level uniqueness key drops `workspace_id` and becomes `(source_id, entity_name, source_record_id)`. Workspace ownership is enforced by the `workspace_context_store_source` relation row + the `context_store_record.source_id → context_store_source.id` FK (a record's workspace is implicit in its source's workspace).

### 5a. Field roles — deciding where each source-record field goes

Each source record arrives as a `Map<String, Object>` (potentially nested). When configuring a `ContextStoreEntity`, the user (or the agent assisting them) decides how each field is stored and retrieved by partitioning it across **four roles**:

| Role | JSONB column on `ContextStoreEntity` | Storage effect | Read effect |
|---|---|---|---|
| **`stored_fields` whitelist** | `stored_fields` | Only listed dotted-paths land in `context_store_record.payload`; everything else is dropped at write time. `null` = retain full payload. | Anything not whitelisted is gone — no recovery without re-syncing. Use to drop sensitive or junk fields permanently. |
| **`indexed_fields` typed sidecar** | `indexed_fields` (required, may be empty) | Each `{<dotted-path>: <type>}` entry copies the value into `context_store_record_index` with one of `value_text` / `value_numeric` / `value_timestamp` populated. B-tree indexes per `(field_name, value_*)`. | Filters/sorts on these fields hit B-tree indexes — sub-millisecond at scale. |
| **`semantic_index_fields` embedded** | `semantic_index_fields` (Phase 14, optional) | Each listed dotted-path's *text* contributes to a single embedding vector per record, stored in the Spring AI PgVectorStore-managed `cs_vector_store` table with HNSW index over the embedding column. PgVector hnsw index auto-created at startup. | Available only via `ContextStoreSemanticSearchService` / `semantic_search_context_store` tool callback; gated by `@ConditionalOnBean(EmbeddingModel.class)`. |
| **payload-only** | (none) | Field stays in `context_store_record.payload` JSONB but has no sidecar entry. | Filterable via JSONB ops on `payload`, but slower (see §7a non-indexed fallback). Sortable: rejected with HTTP 400. |

**The decision rule for the user (or the agent):**

| If the field is… | Put it in… |
|---|---|
| Frequently filtered or sorted on (the hot read path) | `indexed_fields` |
| Prose / free-form text where similarity search adds value (notes, descriptions, ticket bodies) | `semantic_index_fields` (Phase 14) |
| Stored for occasional retrieval but never the filter target | payload-only (drop from `indexed_fields` and `semantic_index_fields`) |
| Sensitive, junk, or contractually unwanted | drop from `stored_fields` (or omit it from the whitelist if `stored_fields` is non-null) |

**What counts as prose** for `semantic_index_fields`? A field is prose when its meaning lives in the wording. Embeddings work by mapping text into a vector space where similar meanings cluster — that requires the text to actually carry meaning. If you can imagine writing two sentences that say the same thing differently, the field is prose.

| Type | Example fields | Embed in `semantic_index_fields`? | Why |
|---|---|---|---|
| Prose | HubSpot contact `notes`, Salesforce opportunity `description`, Jira issue body, Notion page summary, Zendesk ticket subject + description, support email body | ✅ Yes | Meaning is in the wording — "concerned about pricing" vs "happy with onboarding" only differ via NLU. |
| Enum / category | `lifecycle_stage = "SQL"`, `status = "OPEN"`, `priority = "HIGH"` | ❌ No | Discrete labels; filter by exact match. Embedding adds noise. Put in `indexed_fields` instead. |
| Numeric | `amount = 50000`, `count = 12` | ❌ No | Filter by `>` / `<` / `between`. Similarity over numbers is meaningless. |
| Timestamp | `last_replied_at`, `closed_date` | ❌ No | Range filters or sort. Use `indexed_fields` with `TIMESTAMP` type. |
| ID / structured key | `id`, `email`, `domain`, URLs | ❌ No | Exact match. Strings, but not meaning-bearing. |
| Boolean / flag | `is_active`, `verified` | ❌ No | Two values; trivial filter. |

A typical HubSpot contacts source might end up:

```yaml
stored_fields:         {fields: ["id", "email", "company.name", "company.domain",
                                 "lifecycle_stage", "last_replied_at", "phone_e164",
                                 "tags", "notes"]}        # drop the other 90+ HubSpot props
indexed_fields:        {"company.name": "TEXT",
                        "lifecycle_stage": "TEXT",
                        "last_replied_at": "TIMESTAMP"}    # the hot filter columns
semantic_index_fields: {fields: ["notes"]}                 # prose only (Phase 14 — opt-in)
```

The same record's `payload` JSONB still carries `phone_e164`, `email`, `tags`, etc. (because they're in `stored_fields`), they're just not pre-extracted into the sidecar index. Filtering on `payload->>'phone_e164' = '+15555550100'` works at the JSONB-ops slow path; sorting by `phone_e164` returns 400.

## 6. Sync engine (DataStream + Atlas workflow engine)

The sync mechanism is the existing `data-stream.stream` action wrapped in an auto-generated workflow per Context Source. **No new sync engine, scheduler, or launcher** — DataStream is the engine; Atlas workflow engine handles cron, retry, observability, distribution.

### Lifecycle of a sync

1. **Source creation** — `WorkspaceContextStoreSourceFacade.create(workspaceId, input)` does:
   1. INSERT `context_store_source` row with `status = BUILDING_PREVIEW`.
   2. INSERT `context_store_entity` rows.
   3. **Auto-generate** a workflow with shape:
      ```yaml
      label: "Context Store sync — <source name>"
      metadata:
        contextStoreSourceId: <id>           # ownership marker; UI edits blocked
      triggers:
        - name: scheduledSync
          type: schedule/v1/cron             # or interval, depending on cadence
          parameters:
            cron: "<cron expression from source.cadence>"
      tasks:
        - name: sync
          type: data-stream/v1/stream
          clusterElements:
            SOURCE:
              componentName: <source component>
              componentVersion: 1
              clusterElementName: <picked ItemReader cluster element>
              parameters: {<entity-specific params>}
            DESTINATION:
              componentName: contextStore
              componentVersion: 1
              clusterElementName: writeToReplica
              parameters:
                sourceId: <id>
                entityName: <entity>
                idField: <field>
                indexedFields: [...]
                storedFields: [...]            # null = full payload
      ```
   4. Persist via `WorkflowService.createWorkflow(...)` and `ProjectDeploymentWorkflowService` (existing services).
   5. Set `context_store_source.workflow_id` to the persisted workflow's id.
   6. Trigger the workflow once immediately (initial sync).

2. **Cadence change** — when the user changes cadence in the Context Source UI, `WorkspaceContextStoreSourceFacade.update(workspaceId, id, input)` updates **only the trigger** of the existing workflow (not regenerating the whole workflow). The workflow's `tasks` and other config stay intact. Use existing `WorkflowService` mutation APIs.

3. **Manual refresh** — exposed via GraphQL `refreshContextStoreSource(id)` mutation; calls `JobService.create(...)` against the source's workflow (or equivalent existing manual-run mechanism). No new launcher.

4. **Source deletion** — `WorkspaceContextStoreSourceFacade.delete(workspaceId, id)` cascades to deleting the workflow (and its `ProjectDeploymentWorkflow`) plus all `context_store_record`/`context_store_record_index`/`context_store_entity` rows.

5. **Workflow ownership** — workflows with `metadata.contextStoreSourceId` set are read-only in the workflow editor. Users edit cadence via the Context Source UI (which round-trips to the trigger). Custom flow modifications (adding a PROCESSOR for filtering, etc.) are deferred to a future enhancement; for MVP the auto-generated workflow is system-owned.

### `ContextStoreItemWriter` (DESTINATION cluster element)

Lives in the EE component module: `server/ee/libs/modules/components/context-store/src/main/java/com/bytechef/ee/component/contextstore/destination/ContextStoreItemWriter.java`.

Implements ByteChef's existing `ItemWriter` SPI (the same interface Airtable/CSV/JSON destinations use). Key methods:

- `open(inputParameters, connectionParameters, context, executionContext)` — reads `sourceId`, `entityName`, `idField`, `indexedFields`, `storedFields`, and **`mode`** from `inputParameters`. Initializes an in-memory `seenIds` set in `executionContext`.
- `write(List<Map<String, Object>> records)` — for each record:
  1. Apply `storedFields` whitelist (drop unwanted keys per dotted-path).
  2. Compute `payload_hash = PayloadHashUtil.hash(filteredRecord)`.
  3. Look up existing `ContextStoreRecord` by `(workspaceId, sourceId, entityName, sourceRecordId)`. (`workspaceId` resolved from `context_store_source.workspace_id` at open() time.)
  4. If unchanged hash + already exists → `UPDATE last_seen_at = now()` (cheap path).
  5. If changed hash or new → upsert payload + payload_hash + clear `deleted_at`, rebuild `context_store_record_index` rows from `indexed_fields`.
  6. Add `sourceRecordId` to the in-memory `seenIds` set.
- `update(...)` — Spring Batch lifecycle; persist progress for resumability if needed. Also flushes `seenIds` AND the `mode` value into the `ExecutionContext` so the listener can act consistently.
- `close()` — no special cleanup needed; tombstone happens via `JobExecutionListener` at job-end.

#### `mode` parameter (FULL_REPLACE | PARTIAL)

`contextStore.writeToReplica` accepts a string `mode` parameter — defaults to `FULL_REPLACE`. The two values change only the **post-job listener behavior**, not the writer's per-record logic:

| `mode` value | Writer behavior | Listener behavior on COMPLETED |
|---|---|---|
| `FULL_REPLACE` (default) | Standard upsert + `seenIds` aggregation | Tombstone sweep: every record for `(sourceId, entityName)` whose `sourceRecordId` is not in `seenIds` gets `deleted_at = now()`. Source status flips to `READY`. This is the auto-generated workflow's mode — full replacement semantics, idempotent, every sync run reflects the upstream truth. |
| `PARTIAL` | Standard upsert + `seenIds` aggregation (kept for reuse if the listener ever needs to inspect what was touched) | **Skip tombstone sweep.** Records not in `seenIds` are left untouched. Source status still updates `last_sync_run_at` + `last_sync_job_execution_id` (the run happened) but does not flip the status field. Use this mode for backfills, partial-update workflows, ad-hoc imports — anything where the workflow processes a subset of source records and you don't want unprocessed records to be tombstoned. |

`WorkspaceContextStoreSourceFacade.create(workspaceId, input)` always emits `mode: FULL_REPLACE` explicitly in the auto-generated workflow's destination parameters — so the auto-generated path is immune to a future default-flip. Custom workflow authors who omit `mode` get `FULL_REPLACE` (current behavior; backward compatible). Authors who pick `PARTIAL` get safe partial-update semantics.

### Tombstone-on-completion

A small `@Component` `ContextStoreSyncJobListener implements JobExecutionListener`, registered globally in the EE side. In `afterJob`:

1. Inspect `JobExecution.parameters` to detect whether the destination was `contextStore.writeToReplica`. Short-circuit if not (other DataStream jobs are passed through unchanged).
2. Read the `mode` parameter from the destination's `inputParameters` (default `FULL_REPLACE` if absent).
3. If `JobExecution.status == COMPLETED`:
   - **`FULL_REPLACE`**: pull `seenIds` from the step's `ExecutionContext`, call `ContextStoreRecordService.tombstoneUnseen(sourceId, entityName, seenIds, now())`. Update `ContextStoreSource.status = READY`, `last_sync_run_at = now()`, `last_sync_job_execution_id = jobExecution.getId()`.
   - **`PARTIAL`**: skip the tombstone sweep entirely. Update `last_sync_run_at` + `last_sync_job_execution_id` (the run did happen) but do NOT flip `status` — a `BUILDING_PREVIEW` source stays `BUILDING_PREVIEW` (only a full sync proves the source is ready); a `READY` source stays `READY`.
4. If `FAILED`: update `ContextStoreSource.status = FAILED` (or preserve last-good-state when the source was previously `READY` — transient sync failures don't downgrade a working source). No tombstone in either mode.

### Why this is better than the original Spring-Batch-direct design

- **Reuses the workflow engine** — cron, retry, observability, manual runs, distributed execution are all already there as Atlas primitives. Building parallel `ContextStoreSyncLauncher` / `ContextStoreSyncScheduler` was rebuilding those one tier down.
- **Sync UI is workflow-execution UI** — no separate "Context Store sync history" view needed.
- **Atlas dispatch is automatic** — workflows already run on any available Worker. Eliminates the previously-separate "Atlas dispatch wrapper" phase entirely (the old Phase 15 slot is reused by Client-side UI per the 2026-05-08 ordering pivot — see decision log).
- **One new cluster element** (`writeToReplica` DESTINATION) replaces ~7 classes (`ContextStoreBatchConfiguration`, `contextStoreSyncJob`, `contextStoreSyncStep`, custom `JobLauncher`-based `ContextStoreSyncLauncher`, `ContextStoreSyncScheduler`, `ContextStoreSyncRun` table + service + repository).

### Sources that need to opt in

For a component to be Context Store-syncable, it must implement an `ItemReader` cluster element. Today: Airtable, CSV, JSON. Future: HubSpot/Salesforce/etc. would each need a Reader implementation. The plan's earlier "ListActionReaderAdapter" idea (using any list action as a fallback source) is dropped for MVP — a future DataStream SPI extension can generalize this if needed.

## 7. Read side — query model and tool surfaces

### 7a. Query model (`ContextStoreQueryService`, CE)

```
ContextStoreQuery
  workspaceId, sourceId, entityName
  filters         List<Filter>      ← {field, op, value}; ops: eq, neq, in, contains, startsWith, gt, gte, lt, lte, between
  sort            List<Sort>        ← {field, dir(ASC|DESC)}
  limit           int               ← default 50, max 500
  cursor          String?           ← opaque base64 of (lastSortValue, lastRecordId)
  includeDeleted  boolean           ← default false
  fields          List<String>?     ← projection; null = full payload

ContextStoreQueryService
  search(query): SearchResult{items, nextCursor}
  get(workspaceId, sourceId, entityName, sourceRecordId): ContextStoreRecord?
```

Filters fan out to one of two SQL shapes depending on whether the target field is in `indexed_fields`:

```sql
-- Indexed field (e.g. "company.name" with type TEXT in indexed_fields):
-- B-tree on context_store_record_index, fast at scale.
WHERE EXISTS (SELECT 1 FROM context_store_record_index i
              WHERE i.record_id = r.id
                AND i.field_name = 'company.name'
                AND i.value_text = 'Acme')

-- Non-indexed field (e.g. "phone_e164" present in payload but not in indexed_fields):
-- JSONB extraction on the payload column, helped slightly by GIN-on-payload for containment ops.
-- Slower row-by-row at scale.
WHERE r.payload->>'phone_e164' = '+15555550100'

-- Non-indexed contains on a JSON-array field:
WHERE r.payload->'tags' ? 'enterprise'
```

`ContextStoreQueryService` enforces a tiered policy on non-indexed access:

- **Filter on a non-indexed field** — allowed (low-volume escape hatch). Logs a WARN with the field name so the operator can see when a filter would benefit from being added to `indexed_fields`.
- **Sort on a non-indexed field** — rejected with HTTP 400 (`"Cannot sort on non-indexed field 'X'; add it to the entity's indexedFields"`). Sorting via JSONB extraction is too expensive to allow silently.

Cursor is `(lastSortValue, lastRecordId)` to ensure stable pagination under concurrent inserts. Records inserted with sort values past the cursor's last value won't appear until the next cursor exhausts the current page.

Example call (AiHub agent or workflow step — same shape):

```
search_context_store(
    sourceId: 17, entity: "contacts",
    filters: [
        {field: "company.name",    op: "eq", value: "Acme"},
        {field: "last_replied_at", op: "lt", value: "2026-04-08T00:00Z"}
    ],
    sort:   [{field: "last_replied_at", dir: "desc"}],
    limit:  50
)
→ {items: [...], nextCursor: "..."}
```

Both filters above hit the B-tree on `context_store_record_index` (assuming `company.name` and `last_replied_at` are in `indexed_fields`) — sub-millisecond at scale. The sort is also indexed, so cursor pagination is stable.

### 7b. Tool surface — three consumer paths

**The AI Agent's tool selection model (the constraint that shapes everything):**

`AiAgentToolFacade.getFunctionToolCallback(ClusterElement, ...)` reads from `ClusterElement` rows (one per user-picked tool), each storing `(componentName, componentVersion, clusterElementName, parameters)`. Every tool the AI Agent can invoke is a TOOLS-typed cluster element (`BaseToolFunction.TOOLS`) on some component. There is no tool category outside this model — Slack tools, HubSpot tools, Context Store tools all flow through the same picker.

| Path | Layer | Surfacing idiom | Tool shape LLM sees |
|---|---|---|---|
| **Workflow steps (non-AI)** | **CE** | User adds a `contextStore.search` (or `contextStore.get`) Action as a workflow step. Static parameters; bound at design time or via expressions. No LLM involved. | N/A — direct call into `ContextStoreQueryService`. |
| **AI Agent component in workflow** | **EE** (TOOLS cluster elements) | User picks `contextStore.search` `BaseToolFunction.TOOLS` cluster element + binds `(sourceId, entity)` at design time. LLM-supplied args (`filters`, `limit`, `sort`, `cursor`) come via `fromAi(...)` placeholders. Multiple entities ⇒ multiple picks. | One `FunctionToolCallback` per pick, named `CONTEXT_STORE_SEARCH` (or user-overridden), bound to one `(source, entity)`. `inputSchema` reflects `fromAi` placeholders only. |
| **External agent via MCP / `McpServer`** | **EE** (`ContextStoreToolFacade`) | `ContextStoreToolFacade.getFunctionToolCallbacks(workspaceId)` enumerates per-`(source, entity)` callbacks. Plumbed into the same `McpServer` enumerator that aggregates `AutomationMcpToolFacade` outputs. Auth via `McpServer.secretKey`. | Many typed callbacks: `CONTEXT_STORE_HUBSPOT_CONTACTS_SEARCH`, `CONTEXT_STORE_HUBSPOT_DEALS_SEARCH`, etc. `inputSchema` generated from `ContextStoreEntity.indexedFields` → typed properties. |
| **AiHub Personal Agent / routing agent** | **EE** (CC tool callbacks) | Three (or four with semantic) hand-rolled callback classes registered in the CC routing-agent aggregation point, alongside `QueryKnowledgeBaseToolCallback`. Constant tool count regardless of source count. | **Consume**: `list_context_sources()` (discovery — returns sources + entities + indexedFields), `search_context_store(sourceId, entity, filters, sort, limit, cursor)`, `get_context_store_record(sourceId, entity, sourceRecordId)`, `semantic_search_context_store(...)` when add-on present. **Define**: `list_available_source_components()` and `describe_source_component_entities(componentName, componentVersion)` for discovery; `create_context_store_source(input)`, `update_context_store_source(id, input)`, `delete_context_store_source(id)`, `refresh_context_store_source(id)`, `set_context_store_source_enabled(id, enabled)`. The latter group requires admin role + chat-level user confirmation before execution. |

All paths delegate to the same CE `ContextStoreQueryService`. EE-layer code (tool facade, CC callbacks) calls CE directly; CE has no upward dependency on EE.

### 7c. Synthetic `contextStore` component — single EE handler

The `contextStore` component is registered by a single handler in EE under `server/ee/libs/modules/components/context-store/` per Option B (see decision log). The earlier draft proposed splitting handler + Action layer across CE and EE via `AbstractComponentDefinitionWrapper`; that split was abandoned with Option B since the entire feature lives in EE. The handler exposes both Actions (for workflow steps) and TOOLS cluster elements (for the AI Agent component) from one place.

**CE — `server/libs/modules/components/context-store/`:**

```
@AutoService(ComponentHandler.class)
ContextStoreComponentHandler — name "contextStore", version 1
  Action: search   (workflow steps)
    sourceId  (dynamic options from ContextStoreSourceService.getSources(workspaceId))
    entity    (dynamic options from ContextStoreEntityService.getEntities(sourceId), optionsLookupDependsOn=sourceId)
    filters   (object — JSON)
    sort      (array)
    limit, cursor
    includeDeleted (boolean, default false)
    → returns {items: [...], nextCursor: ?}

  Action: get
    sourceId, entity, sourceRecordId
    → returns single payload or null

  —— NO TOOLS cluster elements here — the EE module below adds them ——
```

**EE — `server/ee/libs/automation/automation-context-store/automation-context-store-tool-service/`:**

```
@AutoService(ComponentHandler.class)
ContextStoreToolsComponentHandler  (extends the CE handler via AbstractComponentDefinitionWrapper)
  adds TOOLS cluster element: search   (type = BaseToolFunction.TOOLS)
    same parameter shape as CE Action: search; static (source, entity) at design time;
    other args wrapped in fromAi() so the LLM supplies them at run time
  adds TOOLS cluster element: get      (type = BaseToolFunction.TOOLS)
    same parameter shape as CE Action: get
```

CE Actions and EE TOOLS cluster elements share the same underlying handler logic — both delegate to `ContextStoreQueryService`. CE deployments without the EE module installed get the workflow actions only; EE deployments get both.

## 8. UX — creating a sync source

Guided "Add Context Source" dialog (in the workspace's Context Store screen, GraphQL-driven):

1. **Pick a connection**. Drives the source component (e.g., HubSpot connection ⇒ source component = `hubspot`).
2. **Pick `ItemReader` cluster element** on the source component. Components without an `ItemReader` cluster element are not Context Store-syncable in MVP.
3. **Pick entity type**. Dynamic options from the chosen reader's `getFields()`.
4. **Configure `idField`** — auto-detected from `getFields()` if obvious (`id` / `_id` / `pk`).
5. **Configure `indexedFields`** — multi-select of fields the user wants to filter/sort on. Each picked field gets a typed column in `context_store_record_index` on first record write.
6. **Pick cadence**: `@manual` / `@hourly` / `@daily` / cron expression.
7. **Save** → workflow auto-generated → `ContextStoreSource.status = BUILDING_PREVIEW` → first sync runs immediately → `PREVIEW` after first chunk → `READY` after full sweep.

Power users can hit `refreshNow` from the source detail screen or via the GraphQL mutation. Behind the scenes, this triggers a manual run of the auto-generated workflow.

### Chat-driven creation (AiHub, EE)

The same operations are also available via AiHub's chat surface. Typical interaction flow:

1. User: "Set up a Context Store sync for our HubSpot contacts that runs hourly."
2. Routing agent calls `list_available_source_components()` — returns the catalog of components with `ItemReader` cluster elements (CSV, JSON, Airtable today; HubSpot/Salesforce/Notion/etc. once Reader implementations land).
3. If HubSpot is in the catalog: agent calls `describe_source_component_entities("hubspot", 1)` to learn what entities the HubSpot reader can emit and what fields each carries. If HubSpot is not yet a source: agent reports "HubSpot doesn't have an ItemReader cluster element yet — currently syncable: ...".
4. Agent proposes a configuration to the user: source name, cadence, entity, idField, suggested `indexedFields` based on the schema. **Asks for explicit confirmation** before proceeding (per the platform's explicit-permission doctrine; modifying workspace infrastructure is on the confirm-required side of the line).
5. On confirmation: agent calls `create_context_store_source(input)` — facade auto-generates the workflow per spec §6, persists everything, triggers the initial sync.
6. Agent can poll `list_context_sources()` (or `refresh_context_store_source(id)` to manually re-trigger) to report sync status to the user.

Subsequent management — changing cadence, disabling temporarily, deleting — flows through `update_context_store_source`, `set_context_store_source_enabled`, `delete_context_store_source` respectively. Each requires the same admin role + chat confirmation before the agent executes.

The chat-driven flow uses **the same `WorkspaceContextStoreSourceFacade`** that the UI's "Add Context Source" dialog uses — no parallel implementation, no opportunity for the two paths to drift.

## 9. GraphQL surface

```graphql
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
  workflow: Workflow                # auto-generated sync workflow; sync history surfaced via workflow's JobExecution rows
  entities: [ContextStoreEntity!]!
}

enum ContextStoreReaderStrategy { CLUSTER_ELEMENT  LIST_ACTION }
enum ContextStoreSourceStatus   { BUILDING_PREVIEW  PREVIEW  READY  FAILED  DISABLED }

type Mutation {
  createContextStoreSource(input: CreateContextStoreSourceInput!): ContextStoreSource!
  updateContextStoreSource(id: ID!, input: UpdateContextStoreSourceInput!): ContextStoreSource!
  deleteContextStoreSource(id: ID!): Boolean!
  refreshContextStoreSource(id: ID!): JobExecution!            # admin-only via @PreAuthorize; returns Atlas JobExecution row
  setContextStoreSourceEnabled(id: ID!, enabled: Boolean!): ContextStoreSource!
}
```

Enum values use SCREAMING_SNAKE_CASE per project convention.

## 10. Semantic search add-on (gated; in MVP, ships after Phase 13)

**Placement**: lives in the same EE module as the structured backend (`platform-context-store-service`), gated by `@ConditionalOnBean(EmbeddingModel.class)`. Mirrors `platform-knowledge-base-service` exactly (post-2026-05-09 KB-to-platform move; commit `5cee82ab933`) — depends only on Spring AI's `EmbeddingModel` interface, no gateway-specific imports. The `semantic_index_fields` column already landed in the Phase-2 MVP migrations to avoid a follow-up migration; this phase fills in the embedding pipeline + retrieval surface against that pre-laid storage. Ships **after Phase 13 (KnowledgeBaseSource)** so the broader sync surface is stable before adding optional retrieval modes on top.

The add-on is a **parallel path on the same records** — both indexes coexist: structured columns for filters + sorts, vector embeddings for similarity ranking. Queries can use either alone or hybrid (semantic ranking within a structured-filter result), and the structured path keeps working unchanged whether the add-on is active or not.

### Configuration — per-entity opt-in

When creating a Context Source, the user picks `semanticIndexFields` per entity — a list of dotted-path text fields to embed. Empty/unset = no embeddings for this entity, even when an `EmbeddingModel` is configured.

```yaml
ContextStoreEntity:
  entity_name: "contacts"
  indexed_fields:        {"company.name": "TEXT",
                          "lifecycle_stage": "TEXT",
                          "last_replied_at": "TIMESTAMP"}        # B-tree (filter/sort)
  semantic_index_fields: {fields: ["notes", "description"]}      # embedded (similarity)
```

**Cost-control**: only fields with prose content get embedded (see §5a "Field roles"). A `lifecycle_stage` enum field stays in the structured index only — embedding it would be wasteful (every "SQL" embeds to roughly the same vector, drowning out the meaningful field embeddings in similarity search). The narrower the `semantic_index_fields` whitelist, the cheaper the sync and the more focused the similarity space.

### Write path — embedding during sync

`ContextStoreSemanticBatchListener` runs as a Spring Batch `JobExecutionListener` after each Context Store sync job:

```
afterJob:
  for each ContextStoreRecord seen in this run (from the StepExecution
                               seenIds populated by ContextStoreItemWriter)
                               AND entity.semantic_index_fields is non-empty:

    storedHash = jdbcTemplate.queryForObject(                        ← hash-skip via PgVectorStore's
        "SELECT metadata->>'payloadHash' FROM cs_vector_store           metadata column (escape hatch
         WHERE id = :recordId", String.class, recordId.toString())      since Spring AI has no findById)

    if (storedHash == record.getPayloadHash())  continue              ← cost saver: zero embeddings on
                                                                         re-sync of unchanged records

    text     = concat(record.payload[field] for field in entity.semantic_index_fields)
    document = new Document(
        recordId.toString(),
        text,
        Map.of(
            "recordId",    recordId,
            "sourceId",    sourceId,
            "entityName",  entityName,
            "payloadHash", payloadHash))
    vectorStore.add(List.of(document))                                ← PgVectorStore upsert by id;
                                                                         calls embeddingModel.embed
                                                                         transparently

  failures here do NOT fail the sync — structured replica is still queryable
```

Two key cost-savers:
- **Hash-skip**: records whose `payload_hash` matches the metadata-stored `payloadHash` from the previous embed are skipped. Re-syncing 10 000 unchanged records costs zero embeddings.
- **Per-entity opt-out**: records whose entity has no `semantic_index_fields` are skipped entirely — the listener's per-record loop short-circuits before reading the payload.

### Read path — three modes

Query callers (the CC EE `semantic_search_context_store` tool callback and the EE per-(source, entity) MCP `FunctionToolCallback`s minted by `ContextStoreToolFacade`) all dispatch to one of three modes inside `ContextStoreSemanticSearchService` based on what the caller supplied. There is **no** dedicated GraphQL query for semantic search — the read path is tool-callback-only, mirroring the structured `search_context_store` callback's pattern. (GraphQL exposes only CRUD + lifecycle on `ContextStoreSource` / `ContextStoreEntity`, not the read path.) Public REST `POST /context-store/semantic-search` is documented in §11 and lands in the post-MVP REST phase.

**Mode 1 — pure structured** (the MVP path; no embedding involved):

```
search_context_store(filters: [...], sort: [...])
  → JOIN context_store_record_index on indexed columns (or JSONB ops on payload for non-indexed)
  → returns rows in deterministic sort order
```

**Mode 2 — pure semantic** (rank by similarity, no filter):

```
semantic_search_context_store(query: "contacts concerned about pricing", k: 20)
  → similaritySearch via Spring AI PgVectorStore:
      SearchRequest.query(query).withTopK(k)
        ← embedding + cosine-distance search are encapsulated by PgVectorStore; the
          underlying table is `cs_vector_store` (auto-managed by Spring AI, schema =
          id, content, metadata JSONB, embedding vector(N))
  → for each ranked Document, look up the underlying ContextStoreRecord by the
    recordId metadata field to hydrate the full payload
  → returns [{record, similarityScore}, ...] ranked by score
```

**Mode 3 — hybrid** (structured filter + semantic ranking — the killer mode):

```
semantic_search_context_store(
    filters: [{field: "company.name", op: "eq", value: "Acme"}],
    query:   "concerned about pricing",
    k: 20
)
  → candidateIds = contextStoreQueryService.searchRecordIds(
        sourceId, entity, filters)         ← pre-filter via context_store_record_index
                                            (returns List<Long> — id-only projection
                                             helper added in this phase)
  → similaritySearch via Spring AI PgVectorStore:
      SearchRequest.query(query)
        .withTopK(k)
        .withFilterExpression(b -> b.in("recordId", candidateIds))
                                            ← Spring AI's FilterExpression DSL maps to
                                              PgVectorStore's metadata JSONB filter
  → for each ranked Document, look up the underlying ContextStoreRecord by recordId
    metadata field; return [{record, similarityScore}, ...]
```

This is what Knowledge Base **does not do** — KB stores chunk-level vectors in PgVectorStore with no parallel structured index, so it can only deliver Mode 2. Context Store puts the typed-column index alongside the embedding (linked by record id in PgVectorStore metadata), so Mode 3's two-step "pre-filter via index → similarity search filtered by id-set" works without a custom SQL join through PgVectorStore's internal table.

Workspace scoping: caller is responsible for ensuring `sourceId` belongs to a workspace the caller has access to (resolved via the `workspace_context_store_source` relation in automation; same rule the structured query service uses post-2026-05-09 workspace-logic-out-of-platform pivot — `context_store_record` no longer carries `workspace_id`).

Prefilter expression scope in MVP is single-field equality + range (matches the spec §7a indexed-filter ops). Multi-clause boolean prefilters (OR, NOT) are deferred — Mode 3 currently expresses them via the candidate-id-set passed in the FilterExpression. The `searchRecordIds` helper added on `ContextStoreQueryService` for this phase returns just `List<Long>` (id-only projection); don't double-fetch via `search().getItems().map(getId)` — full-row fetch is wasteful when we only need ids.

### What the LLM sees when the add-on is active

In addition to the MVP's `search_context_store` and `get_context_store_record`, the routing agent gains a third tool callback (also `@ConditionalOnBean(ContextStoreSemanticSearchService.class)` so it disappears when no embedding model is configured):

```
semantic_search_context_store(
    sourceId: int, entity: string,
    query: string,
    k: int = 10,
    filters?: List<{field, op, value}>)
→ returns: [{record, similarityScore}, ...] ranked by similarity
```

Both tools coexist. The agent picks based on query shape:

| User query | Agent picks | Reason |
|---|---|---|
| "deals over $50k closing this quarter" | `search_context_store` (Mode 1) | crisp criteria, all expressible as filters + sort |
| "contacts unhappy with onboarding" | `semantic_search_context_store` (Mode 2) | "unhappy" is a fuzzy notion — no field equals it directly |
| "Acme contacts unhappy with onboarding" | `semantic_search_context_store` with `filters=[{company.name, eq, "Acme"}]` (Mode 3) | crisp filter narrows the candidate set, then semantic ranks within |
| "deals where the rep mentioned pricing pushback" | `semantic_search_context_store` over the deal's `notes` field | meaning is in the wording |

The same per-(source, entity) MCP `FunctionToolCallback`s are minted by `ContextStoreToolFacade.getSemanticFunctionToolCallbacks(workspaceId)` so external agents using MCP get the same surface.

### Storage

Embeddings are managed by Spring AI's `PgVectorStore` — same pattern Knowledge Base uses for its chunk-level vectors. PgVectorStore creates and manages its own table at startup (`initializeSchema(true)` on the builder); CS does NOT add a Liquibase changeset for the embedding table.

```java
// ContextStorePgVectorConfiguration (mirrors KnowledgeBasePgVectorConfiguration).
// @Configuration @ConditionalOnBean(EmbeddingModel.class) @ConditionalOnSingleTenant
@Bean
public VectorStore contextStorePgVectorStore(
    @Qualifier("pgVectorJdbcTemplate") JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel,
    PgVectorStoreProperties properties, ...) {
    return PgVectorStore.builder(jdbcTemplate, embeddingModel)
        .vectorTableName("cs_" + properties.getTableName())   // → cs_vector_store
        .dimensions(properties.getDimensions())               // matches EmbeddingModel.dimensions()
        .distanceType(properties.getDistanceType())
        .indexType(properties.getIndexType())                 // HNSW
        .initializeSchema(true)
        ...
        .build();
}
```

PgVectorStore stores each embedding as a `Document(id, content, metadata, embedding)`. The `id` is the `ContextStoreRecord.id` as String. The `metadata` JSONB carries:
- `recordId: long` — the underlying record id (for Mode 3's `FilterExpression.in("recordId", ...)`)
- `sourceId: long`
- `entityName: string`
- `payloadHash: string` — for the listener's hash-skip cost saver

`@ConditionalOnSingleTenant` mirrors the KB precedent — multi-tenant deployments handle vectorstores differently and are out of scope for this add-on.

**Hash-skip cost saver**: before embedding a record, the listener queries PgVectorStore's underlying table directly via `JdbcTemplate` (escape hatch — Spring AI's high-level API has no `findById`):
```sql
SELECT metadata->>'payloadHash' FROM cs_vector_store WHERE id = :recordId;
```
If the stored hash matches `record.getPayloadHash()`, skip the re-embed. Otherwise, embed + `vectorStore.add(List.of(document))` — PgVectorStore upserts by id.

Dimension is locked at app startup from `EmbeddingModel.dimensions()` via `PgVectorStoreProperties`; switching embedding-model dimensionality requires dropping the PgVectorStore-managed table (it'll re-init at next startup) and re-embedding all records on the next sync via the hash-skip miss path. Document this constraint at the deploy-config layer.

### Bean wiring — how the gateway integrates without coupling

| Deployment | `EmbeddingModel` bean source | Behavior |
|---|---|---|
| **No `spring.ai.*` config** | No bean | Semantic add-on stays inactive (`@ConditionalOnBean` excludes the listener, the service, the tool callback). Structured CS still works. |
| **With `spring.ai.openai.api-key` (or equivalent)** | Spring AI auto-config | Single global embedding model. Semantic CS works workspace-wide on a single model. |
| **EE with gateway** | Gateway-provided workspace-aware `EmbeddingModel` impl (in `platform-ai-gateway-service`) replaces the auto-config | Per-workspace provider routing, `AiLlmUsage` rollups. **Context Store code unchanged.** |

Critically: Context Store has zero `import com.bytechef.ee.platform.ai.gateway.*` lines. The gateway integration is invisible to Context Store — it simply calls `embeddingModel.embed(text)` and gets workspace-appropriate behavior at runtime.

### Failure isolation

| Failure | Effect on structured CS | Effect on semantic |
|---|---|---|
| Embedding API is down or returns 429/5xx | None — sync still completes; `search_context_store` and `get_context_store_record` keep working | Vectors stay stale until the next successful sync's listener pass; any record whose `payload_hash` differs from the stored hash in PgVectorStore metadata will be retried then |
| `EmbeddingModel` bean unconfigured | None — pure-structured mode is the entire stack | The PgVectorStore bean, listener, service, and tool callback never register (`@ConditionalOnBean(EmbeddingModel.class)`); semantic surface is invisible to LLMs and to MCP enumeration |
| PgVector extension missing on a target Postgres | None | PgVectorStore's `initializeSchema(true)` fails fast at startup with a clear error; operator must enable PgVector before the add-on can run |
| Multi-tenant deployment | None | `@ConditionalOnSingleTenant` excludes the entire semantic stack (PgVectorStore + listener + service + tool callback). Multi-tenant operators handle vectorstores via the gateway tenancy layer; out of scope for this add-on |

The hash-skip on the listener side also means a transient embedding outage simply pushes the re-embed to the next successful sync — there is no "embedding queue" to drain or partial-state to reconcile. The `payload_hash` equality is the recovery primitive.

### Cost / observability

In EE deployments, every `embeddingModel.embed(...)` call goes through the gateway-provided implementation, which logs to `AiLlmUsage` internally. Context Store semantic indexing thereby shows up in the same usage rollups as Knowledge Base, chat completions, etc. — **without** the Context Store module depending on `AiLlmUsage` or the gateway. No new cost-tracking surface needed; gateway transparency does the work.

### What's new vs reused

| New (semantic-only) | Reused |
|---|---|
| `ContextStorePgVectorConfiguration` (mirrors `KnowledgeBasePgVectorConfiguration`; `@ConditionalOnBean(EmbeddingModel.class) @ConditionalOnSingleTenant`) | `EmbeddingModel` + `PgVectorStore` (Spring AI) — PgVectorStore manages its own `cs_vector_store` table at startup; no Liquibase changeset added |
| `ContextStoreSemanticBatchListener` (Spring Batch JobExecutionListener) | PgVector storage pattern (`platform-knowledge-base` precedent) — same `PgVectorStore` builder, different table prefix |
| `ContextStoreSemanticSearchService` interface + impl | `AiGatewayEmbeddingModelFactory` (transparent via bean replacement; no Context Store import) |
| `SemanticSearchContextStoreToolCallback` (CC EE chat surface) | `AiLlmUsage` cost rollups (gateway-internal) |
| Per-(source, entity) `semantic_search_*` MCP `FunctionToolCallback`s minted by `ContextStoreToolFacade` | All structured query infra (filters, cursor, projection, `context_store_record_index`) |
| `ContextStoreQueryService.searchRecordIds(...)` helper (id-only projection) for Mode 3's pre-filter | All sync infra (DataStream `data-stream.stream`, `ContextStoreItemWriter`, `ContextStoreSyncJobListener`) |

Net: ~5 new classes + 1 PgVector configuration bean + 1 `searchRecordIds` query-service helper. Zero Liquibase changesets added — PgVectorStore self-initializes its table. Everything else is composition of existing primitives. No new framework, no new abstraction layer — the add-on is a small parallel listener + service that rides on the structured infra without touching it.

## 11. Public REST API (post-MVP, EE)

A workspace-scoped REST API for external clients (third-party agents not using MCP, custom integrations, automation scripts, the user's own backends) — same role as `automation-ai-gateway-public-rest` plays for the AI gateway. **Out of MVP scope; ships as a separate phase after the structured backend is shipped.**

### Placement

```
server/ee/libs/automation/automation-context-store/
  automation-context-store-public-rest/
    src/main/java/com/bytechef/ee/automation/contextstore/web/rest/
      ContextStoreSearchRestController.java
      ContextStoreRecordRestController.java
      ContextStoreSourceRestController.java
      ContextStoreSyncRestController.java
      dto/...
    src/main/resources/com/bytechef/automation/contextstore/openapi.yaml   ← OpenAPI 3.0 spec; SpringDoc auto-renders
```

Mirrors the AI gateway's `automation-ai-gateway-public-rest` pattern. The REST module depends on the CE `automation-context-store-api` for service interfaces and on the EE `automation-context-store-tool-api` for tool-listing endpoints. **EE-tier** because external API access is a hosted/paid product surface — same positioning as Airbyte's HTTP API.

### Endpoint surface (v1)

```
# Read
GET    /api/v1/workspaces/{workspaceId}/context-store/sources
GET    /api/v1/workspaces/{workspaceId}/context-store/sources/{sourceId}
GET    /api/v1/workspaces/{workspaceId}/context-store/sources/{sourceId}/entities
POST   /api/v1/workspaces/{workspaceId}/context-store/search
       body: {sourceId, entity, filters[], sort[], limit, cursor, includeDeleted, fields[]}
GET    /api/v1/workspaces/{workspaceId}/context-store/records/{sourceId}/{entity}/{sourceRecordId}

# Tool discovery (mirrors Airbyte's context_store_search MCP-shaped operations)
GET    /api/v1/workspaces/{workspaceId}/context-store/tools
       returns the same per-(source, entity) FunctionToolCallback shapes the MCP server enumerates,
       so external agents using HTTP rather than MCP get the same typed-tool list

# Operations (admin-only via @PreAuthorize)
POST   /api/v1/workspaces/{workspaceId}/context-store/sources/{sourceId}/refresh
       returns Atlas JobExecution (status + workflow execution id)
GET    /api/v1/workspaces/{workspaceId}/context-store/sources/{sourceId}/sync-runs?limit=
       returns recent JobExecution rows for the source's auto-generated workflow
PATCH  /api/v1/workspaces/{workspaceId}/context-store/sources/{sourceId}/enabled
       body: {enabled: boolean}

# Optional semantic (only present when @ConditionalOnBean(SemanticSearchContextStoreToolFacade.class))
POST   /api/v1/workspaces/{workspaceId}/context-store/semantic-search
       body: {sourceId, entity, query, k, filters[]}
```

### Authentication

API-key auth via existing platform mechanism (same as other public REST surfaces). Keys are workspace-scoped; the `{workspaceId}` path segment must match the key's scope or returns 403. Per-key rate limits applied via the same machinery the AI gateway uses.

### OpenAPI

Spec at `src/main/resources/com/bytechef/automation/contextstore/openapi.yaml`. SpringDoc auto-renders the Swagger UI at `/swagger-ui/index.html`. Generated TypeScript/Python clients can be published from the OpenAPI spec the same way the AI gateway's `public-rest` does today.

### Why post-MVP

The MVP backend ships internal use cases (workflow steps, AI Agent component, AiHub, MCP server). External REST is additional surface area without new use cases — it's a product/integration layer, not a foundational layer. Worth its own phase so it can ship after the core has stabilized and after any v1 schema adjustments have settled.

### What lands in MVP to avoid future migrations

- Nothing schema-side: REST is a thin adapter on top of `ContextStoreQueryService` and `ContextStoreToolFacade`. No tables added by this phase.
- Service interfaces (`ContextStoreQueryService`, `ContextStoreSourceService`) should be designed in MVP with REST consumption in mind (clear DTO boundaries, no JPA-entity leakage). They will be anyway because GraphQL has the same constraint.

---

## 12. Knowledge Base Source — sync into existing KB documents (in MVP)

A `KnowledgeBaseSource` configures periodic ingestion of document-shaped content (Notion pages, Confluence articles, Google Drive files, etc.) into an **existing** Knowledge Base. Each synced source record becomes a row in the existing `knowledge_base_document` table — with sync metadata stored inline as nullable columns rather than in a separate join entity. The existing KB chunking + embedding pipeline runs unchanged on every synced document.

This collapses what could have been two parallel primitives ("Knowledge Sync" + "KnowledgeBaseSyncedDocument") into one new entity (`KnowledgeBaseSource`) plus five new nullable columns on the existing KB document table. **Post-2026-05-09 KB-to-platform move (commit `5cee82ab933`)**, KB-Source rides on the same platform-vs-automation split as Knowledge Base itself: the `KnowledgeBaseSource` entity + repo + service + sync listener live on `platform-knowledge-base-{api,service}`; the `WorkspaceKnowledgeBaseSource` relation entity + workspace-aware facade live on `automation-knowledge-base-{api,service}`; the GraphQL controller stays in `automation-knowledge-base-graphql`. There is no parallel `automation-knowledge-sync` module tree, no separate document entity, and no separate Spring Batch listener tree.

### Why inline columns on `knowledge_base_document` (not a separate `KnowledgeBaseSyncedDocument` table)

The relationship between "synced KB doc" and "source record" is 1:1 — one source record produces exactly one KB document, identified by its `(source_id, source_record_id)` pair. A separate join table would force three-way reads (doc → join → source) for what is effectively a single denormalized fact about the document. Inlining four nullable columns onto the existing table:

- expresses the constraint via a partial UNIQUE index (`WHERE source_id IS NOT NULL`) — manual uploads keep all sync columns NULL and never participate in the constraint;
- collapses every "is this synced?" question into a column read on the table the consumer already has loaded;
- requires no new repository, no new service for the linkage, no new domain entity to remember;
- lets the existing semantic-search retrieval over `knowledge_base_document_chunk` work *unchanged* — the chunker doesn't care whether the parent doc came from a sync source or a manual upload, and `query_knowledge_base` returns both kinds of hits indistinguishably;
- makes filtering trivial: "only manual" = `WHERE source_id IS NULL`; "synced from a specific source" = `WHERE source_id = ?`; both are cheap B-tree lookups once `idx_kb_doc_source(source_id)` exists.

### Module layout

Mirrors the post-2026-05-09 KB and CS platform/automation split. KB-Source is a sub-domain of KB and extends the same module tree (no new `platform-knowledge-base-source` module).

```
CE — Knowledge Base platform side (extended; pure data plane — no workspace concerns):
  server/libs/platform/platform-knowledge-base/
    platform-knowledge-base-api/
      + domain/KnowledgeBaseSource.java                 ← new entity, NO workspace_id; package
                                                          com.bytechef.platform.knowledgebase.domain
      + domain/KnowledgeBaseSourceStatus.java           ← parallel to ContextStoreSourceStatus
      + domain/ReaderStrategy.java                      ← parallel to CS ReaderStrategy
      + repository/KnowledgeBaseSourceRepository.java   ← CRUD by id only; NO findAllByWorkspaceId
      + service/KnowledgeBaseSourceService.java         ← interface; CRUD by id only
                                                          (no workspaceId param on any method)
      + KnowledgeBaseDocument: + sourceId, sourceRecordId, syncedPayloadHash, lastSeenAt, deletedAt fields
      + KnowledgeBaseDocumentRepository: + findBySourceIdAndSourceRecordId, + tombstoneUnseen
      ‹ existing files unchanged ›

    platform-knowledge-base-service/
      + service/KnowledgeBaseSourceServiceImpl.java
      + service/KnowledgeBaseDocumentServiceImpl gains package-private helpers:
          createSyncedDocument(...) — writes new synced doc + sync columns + emits
            KnowledgeBaseDocumentEvent so the existing chunker pipeline runs untouched
          replaceSyncedDocument(...) — change-detection update path; swaps file storage,
            clears chunks, re-emits event
      + listener/KnowledgeBaseSourceSyncJobListener.java
            ← Spring Batch JobExecutionListener; tombstone + status updates by source_id only;
              same placement as ContextStoreSyncJobListener in platform-context-store-service
              (operates by source_id, no workspace lookup)
      + Liquibase (in src/main/resources/.../changelog/platform/knowledge_base/):
          new changeset: creates knowledge_base_source table (no workspace_id column);
                         adds 5 nullable sync columns to existing knowledge_base_document table
                         (which is now a platform table); adds the partial UNIQUE index
      ‹ existing files unchanged ›

CE — Knowledge Base automation side (workspace-relation + workspace-aware orchestration + GraphQL):
  server/libs/automation/automation-knowledge-base/
    automation-knowledge-base-api/
      + domain/WorkspaceKnowledgeBaseSource.java        ← relation entity:
                                                          (id, workspace_id, knowledge_base_source_id, audit, @Version);
                                                          mirrors WorkspaceKnowledgeBase shape
      + repository/WorkspaceKnowledgeBaseSourceRepository.java
      + service/WorkspaceKnowledgeBaseSourceService.java ← interface; getAllByWorkspaceId,
                                                           getAllEnabledByWorkspaceId joining through
                                                           to platform sources, fetchWorkspaceIdByKnowledgeBaseSourceId,
                                                           etc.
      + facade/WorkspaceKnowledgeBaseSourceFacade.java   ← workspace-aware orchestration interface;
                                                           method signatures all take workspaceId as
                                                           first parameter:
                                                             create(workspaceId, CreateKnowledgeBaseSourceInput)
                                                             update(workspaceId, sourceId, UpdateInput)
                                                             delete(workspaceId, sourceId)
                                                             refreshNow(workspaceId, sourceId): long
                                                             setEnabled(workspaceId, sourceId, boolean)
      ‹ existing WorkspaceKnowledgeBase, KnowledgeBaseDocumentNotFoundException, etc. unchanged ›

    automation-knowledge-base-service/
      + service/WorkspaceKnowledgeBaseSourceServiceImpl.java
      + facade/WorkspaceKnowledgeBaseSourceFacadeImpl.java
            ← uses platform KnowledgeBaseSourceService for entity CRUD;
              + WorkspaceKnowledgeBaseSourceRepository (same module) for relation insert/delete;
              + atlas-coordinator/workflow-execution for workflow auto-generation,
                ProjectDeploymentWorkflow lifecycle, manual job dispatch.
              Mirrors WorkspaceContextStoreSourceFacadeImpl exactly.
      ‹ existing WorkspaceKnowledgeBaseFacadeImpl, WorkspaceKnowledgeBaseServiceImpl unchanged ›

    automation-knowledge-base-graphql/
      + KnowledgeBaseSourceGraphQlController.java
            ← imports both com.bytechef.platform.knowledgebase.* (entity types) AND
              com.bytechef.automation.knowledgebase.* (WorkspaceKnowledgeBaseSourceFacade for mutations,
              WorkspaceKnowledgeBaseSourceService for read paths)
      + schema additions: KnowledgeBaseSource type, CRUD mutations, refresh mutation,
        and extend KnowledgeBaseDocument with sourceId/sourceRecordId/lastSeenAt/deletedAt
      ‹ existing controllers unchanged ›

CE — knowledgeBase component, extended (no new component module):
  server/libs/modules/components/ai/vectorstore/knowledgebase/
    + destination/KnowledgeBaseItemWriter.java   ← new DESTINATION cluster element on the existing
                                                    knowledgeBase component; implements ItemWriter<Map<String,Object>>;
                                                    takes sourceId + mode via inputParameters;
                                                    calls platform KnowledgeBaseSourceService.get(sourceId)
                                                    + KnowledgeBaseDocumentService.{createSyncedDocument,
                                                    replaceSyncedDocument} (no workspace lookup —
                                                    workspace is implicit via the relation table)
    ‹ existing actions/cluster elements unchanged ›
```

EE additions (none required for MVP). The existing CC EE callbacks (`QueryKnowledgeBaseToolCallback` etc.) keep working without change. If chat-driven source creation is desired later, an EE add-on phase can register `CreateKnowledgeBaseSourceToolCallback` etc., mirroring the CC chat-define-CS pattern from §16's 2026-05-08 decision.

### Domain

```
KnowledgeBaseSource (new entity, lives in platform-knowledge-base-api)
  id                         ← BIGSERIAL
  ‹ no workspace_id column ›  ← workspace scope flows through workspace_knowledge_base_source relation
  name                       ← VARCHAR(256) NOT NULL; UNIQUE (per the post-2026-05-09 KB platform pattern)
  knowledgeBaseId            ← target KB; FK to knowledge_base.id ON DELETE CASCADE
  sourceComponentName, sourceComponentVersion
  readerStrategy             ← CLUSTER_ELEMENT | LIST_ACTION (mirrors CS)
  sourceClusterElementName?  ← only when readerStrategy = CLUSTER_ELEMENT
  sourceListActionName?      ← only when readerStrategy = LIST_ACTION
  connectionId?              ← AggregateReference<Connection, Long>
  cadence, status (BUILDING_PREVIEW|PREVIEW|READY|FAILED|DISABLED), enabled
  workflowId?                ← String, set after auto-generated workflow is persisted
  lastSyncRunAt?, lastSyncJobExecutionId?
  audit fields, @Version

WorkspaceKnowledgeBaseSource (new relation entity, lives in automation-knowledge-base-api)
  id                         ← BIGSERIAL
  workspaceId                ← BIGINT NOT NULL
  knowledgeBaseSourceId      ← BIGINT NOT NULL
  audit fields, @Version
  UNIQUE (workspace_id, knowledge_base_source_id)
  FK knowledge_base_source_id → knowledge_base_source.id ON DELETE CASCADE
  (No FK on workspace_id, mirroring workspace_knowledge_base / workspace_connection precedent.)

KnowledgeBaseDocument (existing entity, now in platform-knowledge-base-api; +5 nullable columns)
  ‹ existing: id, knowledgeBaseId, name, document FileEntry, status, tags, audit, @Version ›
  + sourceId            BIGINT NULL                         ← FK to knowledge_base_source.id; NULL = manually uploaded
  + sourceRecordId      VARCHAR(512) NULL                   ← upstream-system stable id within the source
  + syncedPayloadHash   VARCHAR(16) NULL                    ← change-detection signal (mirrors CS)
  + lastSeenAt          TIMESTAMP WITH TIME ZONE NULL       ← bumped on every sync run that re-saw this record
  + deletedAt           TIMESTAMP WITH TIME ZONE NULL       ← tombstone for synced docs that disappeared upstream

  Partial UNIQUE INDEX (source_id, source_record_id) WHERE source_id IS NOT NULL
  Index idx_kb_doc_source (source_id) for "list synced docs for this source" queries
  FK source_id → knowledge_base_source.id ON DELETE SET NULL (deleting a source orphans its docs but doesn't lose them)
```

There is **no `KnowledgeBaseSyncedDocument` entity, no `KnowledgeSyncEntity` per-entity-templates layer, and no separate `KnowledgeSyncRun` table.** Sync history is captured by Atlas's standard JobExecution rows for the source's auto-generated workflow.

The sync metadata is **document-level, not entity-level**: each `KnowledgeBaseSource` produces one stream of documents. There is no per-entity name template or per-entity text template anymore — the source's reader yields records that already have a `name` and a `text` field (or whatever fields the writer's input parameters map to), and the writer sets those directly on the KB document. Sources whose records need composite naming (`"{{title}} ({{id}})"`) handle that transformation in the reader (where source-specific knowledge already lives) rather than in a sync-side template engine.

### Sync engine — DataStream + auto-generated workflow

Mirrors CS (§6) exactly, with the `knowledgeBase.writeAsDocument` DESTINATION instead of `contextStore.writeToReplica`. On `WorkspaceKnowledgeBaseSourceFacade.create(workspaceId, input)` (in `automation-knowledge-base-service`):

1. Call platform `KnowledgeBaseSourceService.create(...)` to INSERT `knowledge_base_source` row (status `BUILDING_PREVIEW`).
2. INSERT `workspace_knowledge_base_source` relation row via `WorkspaceKnowledgeBaseSourceRepository`.
3. Auto-generate a workflow `[schedule.cronTrigger] → [data-stream.stream(SOURCE=<sourceComponent>.<itemReader>, DESTINATION=knowledgeBase.writeAsDocument)]`. DESTINATION parameters carry only `sourceId` + `mode` (the writer reads back everything else from the source row; no workspace lookup). Workflow `metadata.knowledgeBaseSourceId = sourceId` for ownership marking.
4. Persist via `WorkflowService.createWorkflow(...)`. Update `knowledge_base_source.workflow_id` via the platform service.
5. Trigger initial sync via `PrincipalJobFacade.createJob(workflowId, ...)`.

Cadence/manual-refresh/delete lifecycle mirrors CS exactly. The facade's other methods (`update`, `delete`, `refreshNow`, `setEnabled`) all take `workspaceId` as the first parameter and orchestrate against both the platform entity service and the workspace-relation repository in this module.

```
KnowledgeBaseItemWriter (DESTINATION cluster element on the existing knowledgeBase component)
  open(inputParameters, ...):
    sourceId   = inputParameters.getRequiredLong("sourceId")
    mode       = inputParameters.getString("mode", "FULL_REPLACE")    ← FULL_REPLACE | PARTIAL
    source     = knowledgeBaseSourceService.get(sourceId)
    kbId       = source.getKnowledgeBaseId()
    seenRecordIds = new HashSet<>()
  write(records): for each record:
    sourceRecordId = String.valueOf(record.get("id"))   ← reader-supplied stable key (convention: field "id")
    payloadHash    = PayloadHashUtil.hash(record)
    existing       = knowledgeBaseDocumentRepository.findBySourceIdAndSourceRecordId(sourceId, sourceRecordId)
    if (existing.isPresent()) {
        if (existing.get().getSyncedPayloadHash().equals(payloadHash) && existing.get().getDeletedAt() == null) {
            existing.get().setLastSeenAt(now)        ← unchanged-record fast path; no chunker re-run
            knowledgeBaseDocumentRepository.save(existing.get())
        } else {
            // changed payload OR was tombstoned and reappeared: replace content, re-trigger chunker
            knowledgeBaseDocumentService.replaceSyncedDocument(
                existing.get().getId(), record.get("name"), record.get("text"), record /* metadata */, payloadHash, now)
        }
    } else {
        // new: insert document, set sync fields, kick off chunker via existing KnowledgeBaseDocumentEvent
        knowledgeBaseDocumentService.createSyncedDocument(
            kbId, sourceId, sourceRecordId, record.get("name"), record.get("text"), record /* metadata */, payloadHash, now)
    }
    seenRecordIds.add(sourceRecordId)
  update(...):
    executionContext.put("knowledgeBaseSource.seenRecordIds", new ArrayList<>(seenRecordIds))
    executionContext.put("knowledgeBaseSource.mode", mode)
  close():
    no-op; tombstone sweep in the listener (mode-gated)

KnowledgeBaseSourceSyncJobListener implements JobExecutionListener
  beforeJob: detect DESTINATION = knowledgeBase.writeAsDocument; flip source.status to BUILDING_PREVIEW if first sync
  afterJob(COMPLETED):
    mode          = read from destination.inputParameters (default FULL_REPLACE)
    seenRecordIds = aggregate from all StepExecution executionContexts
    if (mode == "FULL_REPLACE"):
        knowledgeBaseDocumentRepository.tombstoneUnseen(sourceId, seenRecordIds, now)
                                                        ← single SQL UPDATE: set deleted_at = now WHERE
                                                          source_id = ? AND source_record_id NOT IN (...) AND deleted_at IS NULL
        source.status = READY; lastSyncRunAt = now; lastSyncJobExecutionId = jobExecution.getId()
    else /* PARTIAL */:
        // skip tombstone sweep; only the records that came through THIS run were touched.
        // status doesn't flip — a BUILDING_PREVIEW source stays BUILDING_PREVIEW until a FULL_REPLACE proves it's ready.
        lastSyncRunAt = now; lastSyncJobExecutionId = jobExecution.getId()
  afterJob(FAILED):
    if source.status was READY: keep it (transient sync failures don't downgrade a working source)
    else: source.status = FAILED (matches CS)
    no tombstone in either mode
```

`WorkspaceKnowledgeBaseSourceFacade.create()` always emits `mode: FULL_REPLACE` explicitly in the auto-generated workflow's destination parameters. Custom workflow authors who pick `PARTIAL` get safe partial-update semantics — they can write a subset of records into a KB-Source's document set without tombstoning the rest. Same shape and semantics as CS's `mode` parameter (§6).

The two service methods (`createSyncedDocument`, `replaceSyncedDocument`) live alongside `KnowledgeBaseDocumentServiceImpl` in `platform-knowledge-base-service` (package-private, not exposed on the public interface). They write the sync metadata columns and emit the existing platform `KnowledgeBaseDocumentEvent` so the chunker + embedder pipeline (which is also platform-side post-relocation) runs untouched.

### KB document semantic search keeps working unchanged

`QueryKnowledgeBaseToolCallback` reads from `knowledge_base_document_chunk`; chunks have a parent FK to `knowledge_base_document.id`. The chunker doesn't read the sync columns. Synced and manually-uploaded documents are indistinguishable to the existing retrieval surface — they coexist in the same KB and the same vector index.

If a UI surface wants to badge "synced from Notion" on a hit, it joins the parent row through the chunk's parent FK and checks `source_id IS NOT NULL`.

### Tool surface

No new LLM tools needed in MVP. Agents already use `QueryKnowledgeBaseToolCallback` for KB retrieval; that callback transparently surfaces synced documents alongside manual ones. Optional EE follow-up (post-MVP):
- `CreateKnowledgeBaseSourceToolCallback` — chat-driven source creation, mirroring the CC EE chat-define-CS pattern from §16's 2026-05-08 decision.
- `RefreshKnowledgeBaseSourceToolCallback` — admin-only force-resync.

These ship as a small EE add-on after MVP; not in the in-MVP redesign scope.

### GraphQL surface

Mirrors CS with parallel mutations under the existing `automation-knowledge-base-graphql` module:

```graphql
extend type Query {
    knowledgeBaseSource(id: ID!): KnowledgeBaseSource
    knowledgeBaseSources(workspaceId: ID!, filter: KnowledgeBaseSourceFilter): [KnowledgeBaseSource!]!
}

extend type Mutation {
    createKnowledgeBaseSource(input: CreateKnowledgeBaseSourceInput!): KnowledgeBaseSource!
    updateKnowledgeBaseSource(id: ID!, input: UpdateKnowledgeBaseSourceInput!): KnowledgeBaseSource!
    deleteKnowledgeBaseSource(id: ID!): Boolean!
    refreshKnowledgeBaseSource(id: ID!): ID!     # returns Atlas JobExecution id; admin-only via @PreAuthorize
    setKnowledgeBaseSourceEnabled(id: ID!, enabled: Boolean!): KnowledgeBaseSource!
}

type KnowledgeBaseSource {
    id: ID!
    name: String!
    knowledgeBaseId: ID!
    sourceComponentName: String!
    sourceComponentVersion: Int!
    readerStrategy: ReaderStrategy!
    sourceClusterElementName: String
    sourceListActionName: String
    connectionId: ID
    cadence: String!
    status: KnowledgeBaseSourceStatus!
    enabled: Boolean!
    lastSyncRunAt: Long
    lastSyncJobExecutionId: ID
    workflowId: ID
}

enum KnowledgeBaseSourceStatus { BUILDING_PREVIEW PREVIEW READY FAILED DISABLED }

# extend the existing KnowledgeBaseDocument GraphQL type (added to the existing -graphql module's schema)
extend type KnowledgeBaseDocument {
    sourceId: ID
    sourceRecordId: String
    lastSeenAt: Long
    deletedAt: Long
}
```

Unlike CS, all CRUD mutations route through `WorkspaceKnowledgeBaseSourceFacade` (workflow auto-gen happens transparently). No separate "entities" CRUD because there are no per-entity rows in this design. The controller resolves `sourceId`-only inputs (e.g., `refreshKnowledgeBaseSource(id: ID!)`) to `workspaceId` via `WorkspaceKnowledgeBaseSourceService.fetchWorkspaceIdByKnowledgeBaseSourceId(...)` before calling workspace-aware facade methods — same pattern as `ContextStoreSourceGraphQlController`.

### UX

Single "Add Knowledge Base Source" guided dialog (parallel to "Add Context Source", but with no per-entity step):

1. Pick target Knowledge Base (or create a new one inline).
2. Pick a connection (drives source component).
3. Pick reader: cluster element OR list action on the source component (mirrors CS).
4. Pick cadence.
5. (Optional) configure connection-specific reader parameters (e.g. Notion workspace id) — passed through to the reader unchanged.
6. Save → first sync runs immediately.

The existing KB document list UI gains a "Sync source" badge column populated from `knowledge_base_document.source_id` (NULL = manual upload). Clicking the badge navigates to the source detail page.

### Open questions for KB-Source

1. **`name` and `text` field convention**: the writer assumes records carry `name` and `text` fields. Most readers naturally produce that shape (Notion pages, Confluence articles, Google Docs all have a title + body). For readers that produce different field names, the source's reader parameters can include simple field-aliasing (`nameField: "title"`, `textField: "markdown"`) — implementer's choice whether to add this to MVP or punt to a follow-up. **Default: punt.** Most readers will be designed alongside this feature and follow the convention.
2. **Multiple sources targeting the same KB**: allowed. The partial UNIQUE on `(source_id, source_record_id)` makes per-source namespaces independent — two Notion sources can both produce a "page-id-42" without collision because the index key includes `source_id`.
3. **Tombstone vs hard-delete**: tombstone (soft-delete via `deleted_at`) for safety/audit. The chunker can be extended to skip embedding regen for tombstoned docs; the retrieval surface filters `WHERE deleted_at IS NULL`. A future "permanent delete after N days" sweeper can be added without schema changes.
4. **`ON DELETE` semantics for `source_id`**: `SET NULL` rather than `CASCADE` — deleting a source orphans its synced docs (they become indistinguishable from manual uploads), preserving the user's KB content. A separate "purge documents from this source" admin mutation can hard-delete on demand.

## 12a. Optional ClickHouse store (post-MVP)

The MVP stores `context_store_record` and `context_store_record_index` in **Postgres** (JSONB payload + sidecar typed-column index). For high-volume analytical workloads, **ClickHouse is a post-MVP swap-in alternative** — not a dual-write or sync target. A workspace-level config flag (or per-source binding) would route storage to ClickHouse instead of Postgres; the control plane (sources/entities/cadence/status) stays in Postgres regardless. `ContextStoreQueryService` and `ContextStoreItemWriter` are the only swap points; both already abstract their storage. Other layers (workflow auto-generation, tool facade, GraphQL, CC callbacks) are unchanged.

### Architecture

- `ContextStoreRecordRepository` becomes pluggable: `ContextStoreRecordPostgresRepository` (default, current MVP) or `ContextStoreRecordClickHouseRepository` (new post-MVP impl).
- Selection: workspace-level config (`context_store.backend = postgres|clickhouse` on `ContextStoreSource`, or global app config) picks the impl per source.
- Both impls satisfy the same `ContextStoreQueryService` contract — agents and the synthetic component don't see the backend choice.

### ClickHouse schema strategy

- Per-entity dynamic tables: at "Add Context Source" time, generate `CREATE TABLE context_store_{workspace}_{source}_{entity} (...)` from the entity's `indexedFields`.
  - Each indexed field becomes a typed column (`String`, `Decimal(N, M)`, `DateTime`, etc.).
  - Non-indexed payload stored in a `JSON` column (ClickHouse 25+ has proper JSON type) or `String` (use `JSONExtract*` functions to project) for older deployments.
  - `_id String` for the source record key, `_payload_hash String`, `_last_seen_at DateTime`, `_deleted_at Nullable(DateTime)` (tombstone).
  - Engine: `ReplacingMergeTree(_last_seen_at)` ORDER BY `(_id)` — gives idempotent upsert (latest version wins) and natural tombstone via `_deleted_at`.

### Filter translation

Most CS filter ops translate directly: `eq`/`neq`/`in`/`gt`/`gte`/`lt`/`lte`/`between` map to ClickHouse SQL. `contains`/`startsWith` use `position`/`startsWith`. JSONB-payload queries don't apply (substitute with `JSONExtractString(_payload, 'a.b.c') = ...`).

### Migrations

ClickHouse migrations are separate from Liquibase (no JDBC-driver-based migrator that handles ClickHouse cleanly). Use `clickhouse-migrator` or generate `ALTER TABLE` statements at "Add Context Source" / "Update Context Source" time.

### Operational

ClickHouse adds a hard infra dependency. Positioned as an EE+ tier option for high-volume deployments. Default remains Postgres for everyone else.

---

## 13. Testing strategy

Covers Context Store, the Knowledge Base Source primitive, and the gated semantic add-on. CS and KB-Source share the same DataStream-driven sync mechanism (auto-generated workflow + DESTINATION cluster element + JobExecutionListener), so the test surface is parallel between the two.

### Unit — EE (Context Store core + tools)
- `ContextStoreItemWriter` (DESTINATION cluster element) — upsert, change-detection (payload hash), tombstone tracking via seenIds, index-row rebuild on change, `storedFields` whitelist.
- `ContextStoreQueryService` — filter translation correctness for each op, cursor stability, projection, `includeDeleted`.
- `WorkspaceContextStoreSourceFacade` — workflow auto-generation on create, trigger update on cadence change, workflow + source delete cascade.
- `ContextStoreSyncJobListener` — destination detection (only acts on `contextStore.writeToReplica`), tombstone on COMPLETED, status transitions on FAILED.
- `ContextStoreSearchAction`, `ContextStoreGetAction` — workflow-step parameter handling.
- `ContextStoreToolFacadeImpl` — minted per-(source, entity) callbacks delegate to `ContextStoreQueryService` with correct args; `inputSchema` reflects `indexedFields`.
- `ContextStoreToolsComponentHandler` — TOOLS cluster elements registered.
- CC EE callbacks — `SearchContextStoreToolCallback`, `ListContextSourcesToolCallback`, `GetContextStoreRecordToolCallback`, plus the 5 define-side callbacks (`Create/Update/Delete/Refresh/SetEnabledContextStoreSource…`) and the 2 discovery callbacks (`ListAvailableSourceComponents…`, `DescribeSourceComponentEntities…`).
- `SemanticSearchContextStoreToolCallback` — present only when `ContextStoreSemanticSearchService` bean is wired; otherwise excluded by `@ConditionalOnBean`.

### Unit — CE (Knowledge Base Source — Phase 13)
- `KnowledgeBaseItemWriter` (DESTINATION cluster element on the existing `knowledgeBase` component) — change-detection via `synced_payload_hash` (unchanged-record fast path), replace path on hash mismatch, reappearance-clears-deletedAt path, new-record insertion path, `seenRecordIds` aggregation flushed to `executionContext`.
- `KnowledgeBaseSourceFacade` — workflow auto-generation on create with the correct DESTINATION (`knowledgeBase.writeAsDocument`), cadence-change updates the trigger, source delete cascades through `project_deployment_workflow` and the workflow row, `source_id` on documents transitions to NULL via the `ON DELETE SET NULL` FK rather than cascading delete on the documents themselves.
- `KnowledgeBaseSourceSyncJobListener` — discriminates `knowledgeBase.writeAsDocument` from `contextStore.writeToReplica`, tombstones unseen records via `KnowledgeBaseDocumentRepository.tombstoneUnseen`, preserves `READY` status on transient FAILED.
- `KnowledgeBaseDocumentService.createSyncedDocument` / `replaceSyncedDocument` — file-storage write + FileEntry pointer swap on replace + `KnowledgeBaseDocumentEvent` emission to kick the existing chunker pipeline.
- `EnumOrdinalStabilityTest` (KB module) — pins `KnowledgeBaseSourceStatus` and `ReaderStrategy` ordinals.

### Integration (`*IntTest`, Testcontainers PostgreSQL) — EE
- `ContextStoreSyncIntTest` — end-to-end via the auto-generated workflow + fake `ItemReader` source: initial sync, change-detect, deletion-tombstone, resume after failure (driven by DataStream's existing Spring Batch retry semantics).
- `ContextStoreQueryServiceIntTest` — JOINs against `context_store_record_index`; non-indexed-field fallback.
- `ContextStoreGraphQlIntTest` — CRUD + `refreshContextStoreSource` mutation auth; round-trip workflow_id population.
- `ContextStoreSemanticSearchServiceIntTest` (Phase 14, when `EmbeddingModel` bean present in test context) — embedding listener, similarity search, hybrid pre-filter via direct subquery against `context_store_record_index`.

### Integration (`*IntTest`, Testcontainers PostgreSQL) — CE (Knowledge Base Source — Phase 13)
- `KnowledgeBaseSourceServiceIntTest` — CRUD, partial UNIQUE on `(source_id, source_record_id)`, `findAllByKnowledgeBaseId`, `updateStatus` lifecycle.
- `KnowledgeBaseDocumentRepositoryIntTest` (extended) — `findBySourceIdAndSourceRecordId`, `tombstoneUnseen` issues a single UPDATE statement and ignores manual uploads (`source_id IS NULL`).
- `KnowledgeBaseSourceFacadeIntTest` — workflow auto-gen, refreshNow JobExecution round-trip, delete-source-orphans-docs (sets `source_id` to NULL but doesn't delete the documents).
- `KnowledgeBaseSourceGraphQlControllerIntTest` — CRUD + `refreshKnowledgeBaseSource` admin-only via `@PreAuthorize` + the extended `KnowledgeBaseDocument` GraphQL type returns the new sync fields.
- `KnowledgeBaseSourceSyncE2EIntTest` — `@Disabled` skeleton matching the CS `ContextStoreSyncE2EIntTest` precedent; pins the full DataStream + chunker stack scenarios for manual run.

### Integration — EE
- `ContextStoreToolFacadeIntTest` — full enumeration → callback minting → CE query path.
- `ContextStoreMcpIntegrationIntTest` — Context Store tools surfaced through `McpServer` enumeration alongside `AutomationMcpToolFacade` outputs.

### Component
- `ContextStoreComponentHandlerIntTest` (auto-generated JSON definition under `src/test/resources/definition/`) — CE actions only.
- `ContextStoreToolsComponentHandlerIntTest` — EE TOOLS cluster element registration.

### Snapshot
- `EnumOrdinalStabilityTest` extended for `ContextStoreReaderStrategy`, `ContextStoreSourceStatus`.

### AiHub EE wiring
- `SearchContextStoreToolCallbackTest`, `ListContextSourcesToolCallbackTest`, `GetContextStoreRecordToolCallbackTest`, `SemanticSearchContextStoreToolCallbackTest` — alongside existing CC tool-callback tests.

### Public REST (post-MVP, EE) — added in that phase
- `ContextStoreSearchRestControllerIntTest`, `ContextStoreSourceRestControllerIntTest`, `ContextStoreSyncRestControllerIntTest` — auth (API key scope, workspace match), endpoint contract, OpenAPI spec validation.

## 14. Migration strategy

- New Liquibase changeset (EE) creates `context_store_source`, `context_store_entity`, `context_store_record`, `context_store_record_index` plus their indexes. `context_store_source.workflow_id BIGINT` is a logical reference (no FK) to the auto-generated sync workflow. `semantic_index_fields JSONB` and `stored_fields JSONB` columns included in `context_store_entity` from day 1 (the latter unused until `ContextStoreItemWriter` consumes it; the former unused until §10 ships). `context_store_sync_run` is **not** created — sync history is captured by Atlas's standard JobExecution rows for the source's workflow.
- New CE Liquibase changeset on the `platform-knowledge-base-service` module (Phase 13; under `src/main/resources/config/liquibase/changelog/platform/knowledge_base/`): creates the `knowledge_base_source` table with the standard column shape — NO `workspace_id` column (post-2026-05-09 platform pivot; workspace flows through `workspace_knowledge_base_source` relation in the automation module); name VARCHAR(256) NOT NULL UNIQUE, sourceComponentName/Version, readerStrategy/clusterElement/listAction discriminators, connectionId, knowledgeBaseId NOT NULL FK to `knowledge_base.id` ON DELETE CASCADE, cadence, status, enabled, workflow_id, last_sync_run_at, last_sync_job_execution_id, audit, version — plus its indexes (`idx_kb_source_knowledge_base`).
- Same platform changeset adds five nullable columns to the existing `knowledge_base_document` table (which is now a platform table per commit `5cee82ab933`): `source_id BIGINT` (FK to `knowledge_base_source.id` ON DELETE SET NULL), `source_record_id VARCHAR(512)`, `synced_payload_hash VARCHAR(16)`, `last_seen_at TIMESTAMP WITH TIME ZONE`, `deleted_at TIMESTAMP WITH TIME ZONE`. Plus a partial UNIQUE index `(source_id, source_record_id) WHERE source_id IS NOT NULL` (manual uploads keep `source_id = NULL` and don't participate) and `idx_kb_doc_source (source_id)` + `idx_kb_doc_deleted_at (deleted_at) WHERE deleted_at IS NOT NULL`.
- Companion automation-side Liquibase changeset on `automation-knowledge-base-service` creates the `workspace_knowledge_base_source` relation table (`id`, `workspace_id`, `knowledge_base_source_id`, audit, `version`, UNIQUE `(workspace_id, knowledge_base_source_id)`, FK `knowledge_base_source_id → knowledge_base_source.id ON DELETE CASCADE`). Mirrors the existing `workspace_knowledge_base` table shape.
- Phase 14 introduces no Liquibase changeset for embeddings — Spring AI `PgVectorStore` auto-initializes its `cs_vector_store` table at startup. Hash-skip via PgVectorStore metadata column. No data-side migration of existing `context_store_record` rows — embeddings are filled in lazily by the listener as records are re-synced or first-seen.
- No backfill needed for any phase — KB-Source feature is greenfield (no existing synced docs); semantic add-on opt-in per-entity via `semanticIndexFields`.
- Spring Batch `BATCH_*` tables already exist (DataStream uses them); the `data-stream.stream` action driving Context Store and Knowledge Base Source workflows uses them.

## 15. Open questions

These were considered and answered during brainstorming, but recorded here so the implementation plan can revisit if assumptions change:

1. **Why not per-entity physical tables?** Premature; shared-table + sidecar-index gives Postgres GIN-on-JSONB plus typed B-tree on the hot fields. Per-entity tables can come later if profiling demands it; would require a non-trivial migration.
2. **Why not just a workflow per sync (à la "auto-generated workflow")?** The DataStream component already exists for explicit user-orchestrated batch ETL; Context Store wants a managed, system-driven sync — a sibling, not a fork. Reusing only Spring Batch (not DataStream's component layer) gives us `JobRepository`/durability for free without forcing each sync to be a workflow row.
3. **Why is the AI Agent component path "user picks tools" instead of "auto-injecting all workspace sources"?** Consistency with how every other tool is selected today (via `ClusterElement` rows). Auto-injection would break `AiAgentToolFacade`'s contract that runtime tool callbacks come from authored cluster elements.
4. **Why is AiHub EE wiring hand-rolled callbacks instead of facade-minted?** Mirrors the existing CC idiom (`QueryKnowledgeBaseToolCallback`, `ListDataTablesToolCallback`, etc.) and avoids tool-count blowup in the routing agent's static tool list.
5. **Why everything EE (Option B) instead of an Option-C-style CE/EE split?** Owner pivot mid-implementation. Rationale: Airbyte Cloud's positioning maps cleanly — their agent platform is paid-only, OSS handles raw data movement. Spring Batch + DataStream stay CE; Context Store moves entirely to EE. Simpler tier story, fewer module boundaries, fewer cross-edition stub layers. Preserved in the decision log under "Option B (everything EE) — supersedes the earlier Option C decision". The previously-separated `automation-context-store-tool-{api,service}` EE modules collapse into the main platform-context-store + automation-context-store modules since the CE/EE boundary they served no longer exists. Knowledge Base Source (Phase 13) is the one carve-out — it stays CE, riding on `platform-knowledge-base-{api,service}` (the new home for KB itself per commit `5cee82ab933`) plus a slim `automation-knowledge-base-{api,service}` workspace-relation + facade — because it's a small extension of an existing CE primitive, not a new agent-tier surface.
6. **Why doesn't Context Store subsume Knowledge Base?** Knowledge Base handles user-uploaded unstructured documents (PDFs, Word, Markdown); Context Store handles periodically-replicated structured entities from connected sources. Airbyte has no Knowledge Base equivalent — they only do the Context Store half. ByteChef keeps both because they serve different sources of truth (uploaded files vs. live SaaS systems). See §2a.
7. **Workflow ownership and protection**: auto-generated workflows are owned by their Context Source — the workflow editor must block edits to workflows where `metadata.contextStoreSourceId` is set. UI flow: cadence changes in Context Source UI mutate the workflow's trigger. Source deletion cascades to workflow deletion. Implementation deferred to Task 14a (auto-generation) plus a small UI guard.
8. **Reader strategy in MVP**: components must implement an `ItemReader` cluster element to be Context Store-syncable. Airtable/CSV/JSON work today; HubSpot/Salesforce/Notion/etc. need new Reader implementations. The previously-planned `ListActionReaderAdapter` (using any `list` action as fallback) is dropped from MVP — sources need explicit `ItemReader` cluster element. A future DataStream SPI extension can generalize.

## 16. Decision log

| Date | Decision | Reason |
|---|---|---|
| 2026-05-08 | Fork B (parallel primitive + ToolFacade) over A (synthetic component only) for the AI tool surface | Source/entity binding lifecycle (cadence, status, indexedFields) doesn't fit a `componentName/componentVersion` shape — it's its own thing |
| 2026-05-08 | Fork C (also add synthetic `contextStore` component for workflow steps) | Workflow steps consume component actions, not raw `FunctionToolCallback`s; need a component doorway — and BaseToolFunction.TOOLS cluster elements on the same component cover the AI-Agent-in-workflow case |
| 2026-05-08 | Spring Batch directly, not DataStream-component wrapping | DataStream's component layer assumes user-orchestrated workflow composition; Context Store wants system-driven sync. Spring Batch foundation is reusable; DataStream's component wrapper is not the right abstraction |
| 2026-05-08 | Two reader strategies: `CLUSTER_ELEMENT` and `LIST_ACTION` | `ItemReader` cluster elements exist for only 3 components today; supporting `list` actions broadens day-1 coverage to most of the catalog |
| 2026-05-08 | ~~Structured filters only for MVP; semantic deferred to a separate phase~~ **Superseded by 2026-05-08 ordering pivot** — semantic search add-on is now **in MVP, after Phase 13**. Architecture unchanged (CE backend gated on `EmbeddingModel`, EE tool surface, Spring AI primitives, no gateway coupling) — only the schedule moves. Rationale: the semantic surface is small once `semantic_index_fields` is already in Phase-2 migrations and the Spring AI `EmbeddingModel` integration follows the established Knowledge Base precedent (`platform-knowledge-base` post-2026-05-09 relocation); deferring it created unnecessary "MVP feels half-shipped" optics for the chat surface |
| 2026-05-08 | Tombstone via `deleted_at`, not hard delete | Audit; matches Airbyte; cheap (one indexed column) |
| 2026-05-08 | ~~Option C — split: data plane CE, agent plane EE~~ **Superseded by Option B (everything EE)** — see the row tagged "Option B (everything EE) — supersedes the earlier Option C decision" below. Original rationale (Airbyte's positioning) preserved on the supersession entry; the data-plane/agent-plane split was abandoned in favor of a single EE tree to simplify the dependency graph and module count |
| 2026-05-08 | Workspace inlined `workspace_id` on `context_store_source` | Project memory: automation-package entities inline `workspaceId`; no relation table |
| 2026-05-08 | `BaseToolFunction.TOOLS` cluster elements (plural; `multipleElements=true, required=false`) on synthetic component | Confirmed constant in `component-api/.../ai/agent/BaseToolFunction.java:26` |
| 2026-05-08 | Context Store does NOT subsume Knowledge Base; they coexist | Different sources of truth: KB = user-uploaded unstructured docs, CS = replicated structured entities from connected sources. Airbyte's model has no KB equivalent — ByteChef keeps both. Notion-style "document" connectors flow into Context Store as structured entities (pages/blocks/comments), like Airbyte does, with semantic search over text fields available via the semantic add-on |
| 2026-05-08 | Semantic add-on backend depends on Spring AI `EmbeddingModel` interface only — no gateway-specific imports | Mirrors the Knowledge Base precedent (`platform-knowledge-base` post-2026-05-09 relocation). Spring AI's `EmbeddingModel` is the seam: deployments without an embedding model bean see the entire semantic stack as inactive (`@ConditionalOnBean(EmbeddingModel.class)`); EE gateway transparently provides a workspace-aware impl when configured for per-workspace provider routing + `AiLlmUsage` rollups. Context Store code never references `AiGatewayEmbeddingModelFactory`. Lives in the EE `platform-context-store-service` module (same module as the structured backend after the Option B unification + 2026-05-09 platform-CS pivot) |
| 2026-05-08 | Public REST API as a post-MVP EE phase under `automation-context-store-public-rest` | Mirrors `automation-ai-gateway-public-rest`. Agent-plane / external-integration surface — EE tier. Out of MVP scope; deferred to a separate phase so the v1 schema can settle before being externally committed |
| 2026-05-08 | **Option B (everything EE) — supersedes the earlier Option C decision** | Owner pivot mid-implementation (after 5 of 28 plan tasks completed). Rationale: simpler tier story; mirrors Airbyte Cloud positioning (their agent platform is paid-only, OSS handles raw data movement). Spring Batch + DataStream stay CE; Context Store moves entirely to EE. The previously-separated `automation-context-store-tool-{api,service}` EE modules collapse into the main `automation-context-store-{api,service}` modules since the CE/EE boundary they served no longer exists |
| 2026-05-08 | ~~Add Knowledge Sync as a sibling primitive in MVP~~ **Superseded by 2026-05-08 KB-Source redesign** — see the next row. The "Knowledge Sync" name + parallel module tree (`automation-knowledge-sync-{api,service,graphql}`) + parallel `KnowledgeSyncSource`/`KnowledgeSyncEntity` entities all dropped. The MVP still bridges connected sources to the existing Knowledge Base, but via a much smaller surface (one new entity + four nullable columns on the existing KB document table) |
| 2026-05-08 | **KB-Source redesign — drop the "Knowledge Sync" parallel-primitive framing; inline sync metadata onto the existing `KnowledgeBaseDocument`** | Owner pivot mid-Phase-13 (after 4 of 8 KS tasks shipped). Rationale: the KS-vs-KB parallel framing was creating a join-table-of-no-purpose (`KnowledgeSyncEntity` per-entity templates, then per-record `KnowledgeBaseSyncedDocument` linkage), each of which expressed a 1:1 relationship as a separate row in a separate table. Inlining four nullable columns on the existing `knowledge_base_document` (`source_id`, `source_record_id`, `synced_payload_hash`, `last_seen_at`, `deleted_at`) plus one new `KnowledgeBaseSource` entity expresses the same constraint with no extra service, no extra repository, no extra domain class to remember, and no migration path for "two primitives that turned out to coexist on every read". Existing semantic-search retrieval over `knowledge_base_document_chunk` works unchanged because the chunker/retriever doesn't care whether the parent doc is synced or manual. Phase 13 shrinks from 8 tasks to 5; old commits `3c8b1dce19d`, `f78e47cc297`, `52d8f0b93a0`, `40b6fb2e14f`, `9b46349ef68` are reverted/redone |
| 2026-05-08 | **Client-side UI is in MVP scope** (was: out of MVP plan, separate follow-on) | Owner ordering pivot: ship the GraphQL surface alongside a working UI rather than as a backend-only milestone. Mirror the existing CS source list/detail UI shape; add KB-Source UI as a parallel surface |
| 2026-05-08 | **DESTINATION `mode` parameter (`FULL_REPLACE` default \| `PARTIAL`) for safe composability** | Discovered while reviewing CS composability: `ContextStoreSyncJobListener` discriminates only by destination component+cluster-element name, so any workflow using `contextStore.writeToReplica` triggers the listener's tombstone sweep — including custom partial-update workflows that intend to upsert a subset and leave the rest alone. Sharp edge. Fix: add a `mode` parameter on the destination. `FULL_REPLACE` (default; auto-generated workflows always set this explicitly) keeps current behavior. `PARTIAL` skips the tombstone sweep, leaves status intact (only updates `lastSyncRunAt` + `lastSyncJobExecutionId`). Backward compatible (workflows missing the param get FULL_REPLACE = current behavior). Phase 13 ships with the param baked into `knowledgeBase.writeAsDocument` from day 1; Task 32a backfills the same param onto the existing `contextStore.writeToReplica` for parity |
| 2026-05-08 | **Apply the platform/`Workspace<Entity>`-relation pattern only when there's a real case for it** (see §4a) | The Connection/WorkspaceConnection split exists because `Connection` is genuinely platform-shared — multiple automation contexts (workflows, projects, OAuth flows, integrations) consume connections independently. The split earns its module-count cost by enabling clean cross-domain reuse. Considered applying the same split speculatively to `KnowledgeBaseSource` (Phase 13) and retroactively to `ContextStoreSource` (already shipped); rejected because: (a) `KnowledgeBaseSource` is tightly tied to `KnowledgeBase` via a `knowledgeBaseId` FK and has no other consumer; (b) `ContextStoreSource` is similarly tied to the CS sync surface with no platform-shared use case; (c) introducing a `Workspace<Entity>` relation table without an actual reuse case is overhead. Both stay automation-placed with `workspace_id` inlined per the existing convention. The platform-split refactor remains available if a future feature surfaces a genuine multi-consumer case |
| 2026-05-08 | **Move `PayloadHashUtil` from `automation-context-store-api` (EE) to `commons-util` (CE)** | KB-Source change-detection logic (Phase 13 Task 31) needs the same SHA-256-first-8-bytes hashing primitive CS uses. Importing from CS-api would put a CE → EE import on the CE KB module — forbidden direction. The util is domain-agnostic (canonicalize + hash a Map; not CS-specific). Relocating it to `commons-util` lets both CS (EE) and KB-Source (CE) consume it cleanly without the CE/EE bleed |
| 2026-05-09 | **Move CS to platform; reverse the §4a "only if there's a case" guidance for CS specifically** | Owner pivot one day after §4a was written. The rationale for §4a's "only if there's a case for it" rule still holds for *future* sync primitives, but CS and KB-Source are special-cased: applying the platform-split pattern uniformly across both is cleaner long-term than leaving CS automation-placed while KB-Source goes platform. **Concrete changes**: CS core (`ContextStoreSource`, `ContextStoreEntity`, `ContextStoreRecord`, `ContextStoreRecordIndex`, repos, services, facade, listener, query service, tool facade, JDBC config, Liquibase) moved from `server/ee/libs/automation/automation-context-store/{api,service}/` to `server/ee/libs/platform/platform-context-store/{api,service}/`. Java package renamed `com.bytechef.ee.automation.contextstore.*` → `com.bytechef.ee.platform.contextstore.*`. `workspace_id` columns dropped from `context_store_source` and `context_store_record`. New `workspace_context_store_source` relation table mirrors `workspace_connection`. Slim `automation-context-store-{api,service}` modules now host only `WorkspaceContextStoreSource` entity + repo + service. `automation-context-store-graphql` stays in automation per "in automation only leave graphql". A new `ContextStoreWorkspaceResolver` SPI in platform-CS-api lets the platform facade resolve workspace-derived data (e.g., the workspace's "private" project that owns auto-generated sync workflows) without importing automation-side code; the SPI is implemented by `WorkspaceContextStoreSourceServiceImpl` on the automation side. Liquibase init updated in place (no new migration); changeset id unchanged but file path renamed `automation/context_store/` → `platform/context_store/` (master.xml updated accordingly). Existing CS Liquibase changeset is destructive in-place since the branch is unmerged. Commit `baa1f1fe311` — 91 files changed |
| 2026-05-09 (later same day) | **Move workspace-aware logic out of platform-CS; delete `ContextStoreWorkspaceResolver` SPI** | Owner refinement after reviewing the platform-CS structure that landed in `baa1f1fe311`. The brief intermediate state had a `ContextStoreSourceFacade` in platform with 5 workspace-aware methods (create/update/delete/refreshNow/setEnabled — all needing workspace project context for workflow auto-generation, ProjectDeploymentWorkflow lifecycle, and manual job dispatch), bridged to automation via the `ContextStoreWorkspaceResolver` SPI. The owner ruled this violates the principle "workspace-related logic stays in automation"; even though the SPI keeps the import direction legal (platform never imports automation), the workspace-aware *concept* still leaks into platform. **Concrete changes**: deleted `ContextStoreWorkspaceResolver` SPI; deleted platform `ContextStoreSourceFacade` + Impl entirely. New `WorkspaceContextStoreSourceFacade` in `automation-context-store-api` + Impl in `-service` carries all 5 methods now signed `(Long workspaceId, ...)`. The Impl talks to `WorkspaceContextStoreSourceRepository` directly (same module) for relation insert/delete + to platform `ContextStoreSourceService` (entity CRUD only) + to atlas-coordinator/workflow-execution for orchestration. `ContextStoreToolFacade` interface + Impl moved from platform to automation alongside. Platform `ContextStoreQueryService.search(...)` signature dropped its `workspaceId` parameter — caller authorizes workspace ownership upstream. EE component module actions drop the SPI dependency; receive `sourceId` via inputParameters and call platform query service directly. Net: platform-CS is now strictly pure data plane; automation-CS owns all workspace orchestration; dependency direction is unambiguously automation → platform. Commit `64bf8e1fc5d` — 46 files changed |
| 2026-05-09 (Phase 20) | **Drop `LIST_ACTION` reader strategy + use existing `<Properties>` renderer in Add CS Source wizard — no new SPI** | Two follow-up corrections after Phase 15 went live: (1) `LIST_ACTION` was half-shipped (enum + column + workflow-generator branch + GraphQL discovery existed, but no runtime adapter ever wrapped a `list*` action invocation into the Spring Batch `ItemReader` lifecycle — picking it would crash at first sync); (2) the Add CS Source wizard's Entities step never captured SOURCE input parameters (free-text inputs only — no way to configure Airtable's BASE_ID/TABLE_ID, HubSpot's OBJECT_TYPE, etc., so the workflow generator received empty `inputParameters` and sync would fail for any reader with required input properties). **Decisions**: (a) drop `LIST_ACTION` entirely (drop the enum, the column, the workflow-generator branch, the GraphQL discovery, the wizard step) — re-add later if a real second strategy is needed; (b) use the existing `<Properties>` renderer mounted via `<WorkflowMockProvider>` (proven workflow-less pattern from `ConnectionDialog` and `IntegrationInstanceConfigurationDialog`) for SOURCE input parameters — auto-wires SOURCE-property dropdowns via the existing workflow-less `clusterElementOptions` GraphQL query; (c) add ONE new workflow-less GraphQL query `clusterElementFields(componentName, componentVersion, clusterElementName, connectionId, inputParameters): [Field!]!` mirroring `clusterElementOptions` but invoking `FieldsProvider.getFields(...)` directly — used by ID Field + Indexed Fields name dropdowns. **No new SPI on `ItemReader`** (an earlier draft proposed an `EntityProvider` SPI; rejected after the existing `<Properties>` + `clusterElementOptions` workflow-less pattern was confirmed sufficient). **Separation of concerns**: workflow generator stays a pure JSON-shape function; auto-pick-first-`ItemReader` logic landed in `WorkspaceContextStoreSourceFacadeImpl` and `WorkspaceKnowledgeBaseSourceFacadeImpl` (Spring DI available there). **Form-state strategy**: hoisted single `useForm<{entityParameters: Record<number, ...>}>` to dialog level; read-only `useWatch` + `getValues()` at submit time (initial attempt to sync watched parameters back into `useState` via `useEffect` caused infinite re-renders). **Phase 20b shipped same day**: same `<Properties>` rewiring applied to `AddKnowledgeBaseSourceDialog`; new `parameters MapWrapper` field added to `KnowledgeBaseSource` (source-level rather than entity-level since KB-Source has no per-entity layer). Commits: `de4a9c4cf90` (Part A backend) → `949f12e3e53` (Part A client) → `850d178b6c1` (Part B backend) → `b20e1d8a627` (Part B codegen) → `f1bb2d8897b` (Part B wizard rewiring) → `2bd32c62682` (Part 20b backend — new parameters field on KnowledgeBaseSource + workflow generator wiring) → `4a90fd3b20d` (Part 20b client — Source Configuration step in KB-Source dialog) |
| 2026-05-09 (Phase 17 implementation) | **Phase 17 incremental-sync SPI shipped — orchestrator auto-wiring deferred to Phase 17b** | Phase 17 was originally sketched as 5 high-level tasks. Locked in 5 design picks (all option a) before implementation: (1) Reader reads `since` from `ExecutionContext` via well-known `String SINCE_KEY = "datastream.since"` constant on `ItemReader`; new SPI capability is just `default boolean supportsIncremental() { return false; }`. Binary-compatible — no existing method signatures changed. (2) `DataStreamJobExecutionListener.afterJob` writes `lastSyncStart` (= `jobExecution.getStartTime().toInstant(ZoneOffset.UTC).toEpochMilli()`) into the JobExecution's `ExecutionContext` on `BatchStatus.COMPLETED`. Spring Batch's `JobRepository` persists this so `JobExplorer` can read it back across runs (forward-compat with Phase 17b). (3) Airtable-only pilot — `AirtableItemReader` opts in via `supportsIncremental() = true` + a new optional `lastModifiedFieldName` property; `open()` reads `since` and appends `filterByFormula=IS_AFTER({<field>}, '<ISO-8601 since>')` when both are present. (4) Null `since` = first run = full pull (no epoch-zero seeding). (5) **Don't auto-wire incremental into CS / KB-Source workflow generators** — the SPI ships clean and the Airtable pilot proves end-to-end via test-injected JobParameters; users opt in by editing workflow YAML manually to add the `datastream.since` JobParameter. CS / KB-Source workflows continue to default to FULL_REPLACE every run; auto-wiring (workflow-generator extension + cadence-pair UI + listener integration) is **deferred to Phase 17b**. **Plumbing seam**: `ItemStreamReaderDelegate.doBeforeStep` reads the `datastream.since` JobParameter and `open()` copies it into the per-step `ExecutionContext` under `ItemReader.SINCE_KEY` — keeps readers reading purely from ExecutionContext (clean SPI surface) while orchestrators inject state via JobParameters (clean orchestration). The delegate is the only piece that bridges both sides. **Tombstone interaction (unchanged)**: incremental sync alone can't derive tombstones — Phase 17b will choose between periodic-FULL_REPLACE-on-longer-cadence + hourly-incremental, or upstream change-feed events per connector. Commits: `6c8960e2f78` (SPI extension) → `3fc66c076a3` (delegate + listener plumbing) → `80b403b97e4` (Airtable pilot) → `7a13a81c2a5` (round-trip + delegate + listener tests) |
| 2026-05-09 (Phase 15 prep) | **Phase 15 client-side UI surfaces 4 GraphQL gaps + 5 design choices** | Reviewing Phase 15 against the actual GraphQL surface that landed in Tasks 33 + Phase 9 surfaced four backend gaps the UI needs and five design decisions the plan had left open. **Backend gaps** (all addressed in a new "Task 36a" server-prerequisites commit before any client work): (1) `contextStoreSources` and `knowledgeBaseSources` queries are environment-scoped on the back end but the GraphQL signatures don't accept `environmentId` — both queries gain an `environmentId: ID!` arg + the create-input types gain `environmentId: ID`; (2) the create-source dialog's connection picker needs a capability filter — added `dataStreamCompatibleConnections(workspaceId, environmentId): [Connection!]!` query that filters connections to those whose component exposes an `ItemReader` cluster element or a `list*` action; (3) no entity-level CRUD mutations exist (the only path was to delete-and-recreate the source), so 3 new mutations added: `createContextStoreEntity` / `updateContextStoreEntity` / `deleteContextStoreEntity`; (4) sync-run history query — owner picked **option (b) defer**: source detail shows only the last run's metadata via `lastSyncRunAt` + `lastSyncJobExecutionId`; older runs are reachable via the source's `workflowId` link to the existing Atlas Workflow Executions page (deferred to v2). **Design choices**: (a) shared `SyncSourceStatusBadge` component lives in `client/src/shared/components/` (not under either feature folder) so cross-feature import is avoided; (b) CS nav entry gated on `ff_4855 && edition === EE` (dark-launch the UI separately from the back-end); (c) KB-Source UI is a "Sources" tab inside the existing KB detail page (`/automation/knowledge-bases/:id`), NOT a sibling top-level page — a `KnowledgeBaseSource` always belongs to a specific KB so embedding the list under the parent KB groups related data; (d) cadence picker is preset chips + a custom-cron text input that defers validation to the server — **no client-side cron library** (audit confirmed the existing client has zero cron deps); (e) tests are `@testing-library/react` interaction tests, not Vitest snapshot tests, matching `KnowledgeBases.test.tsx` precedent. Phase 15 plan rewritten end-to-end with these adjustments |
| 2026-05-09 (Phase 14 prep) | **Phase 14 semantic add-on uses Spring AI `PgVectorStore` (option A) — not a custom `vector(N)` column** | Reviewing Phase 14 against current code surfaced two contradictions: (a) spec text said "1:1 sidecar with `vector(N)` column" while also saying "mirrors `KnowledgeBasePgVectorConfiguration`" — but KB uses Spring AI `PgVectorStore` (high-level abstraction managing its own table), NOT a raw `vector(N)` column on its own entity. (b) spec §10 Mode 3 hybrid SQL still referenced `r.workspace_id` from `context_store_record` — that column was dropped in commit `64bf8e1fc5d`. Owner picked option A (PgVectorStore). **Concrete changes to spec/plan**: Storage subsection rewritten — embeddings live in PgVectorStore-managed `cs_vector_store` table (auto-created via `initializeSchema(true)`); no Liquibase changeset added for embeddings. Hash-skip cost saver done via `JdbcTemplate` query against `cs_vector_store.metadata->>payloadHash` (escape hatch since Spring AI has no findById on PgVectorStore). Mode 3 hybrid uses Spring AI `FilterExpression.in("recordId", candidateIds)` after pre-filtering via a new `ContextStoreQueryService.searchRecordIds(...)` helper (id-only projection). Workspace scoping is upstream-authorized (caller ensures `sourceId` belongs to its workspace via the relation table) — same rule as the structured query service post-workspace-logic refactor. `@ConditionalOnSingleTenant` added to PgVectorStore config + listener + service to mirror the KB precedent. Phase 14 plan Task 34 rewritten: no Liquibase, just `ContextStorePgVectorConfiguration` bean + `ContextStoreSemanticBatchListener` with PgVectorStore-driven upsert. Net counts in §10 "What's new vs reused" updated: 5 new classes + 1 PgVector config bean + 1 query-service helper; zero Liquibase changesets |
| 2026-05-08 | **Postgres JSONB+sidecar typed index stays for MVP**; ClickHouse alternative deferred to post-MVP §12a | Postgres can't cleanly do per-entity dynamic tables (DDL on hot tables, naming clashes), so JSONB + sidecar typed-column index is the right hybrid for arbitrary-entity flexibility. ClickHouse changes the story: columnar + per-entity tables + `ReplacingMergeTree` is idiomatic and high-volume-friendly. Documented as a pluggable backend post-MVP without affecting MVP architecture |
| 2026-05-08 | **DataStream pivot for sync orchestration** (replaces Spring-Batch-direct design from earlier in this spec) | Auto-generated workflow + new `contextStore.writeToReplica` DESTINATION cluster element + DataStream's existing `data-stream.stream` action. Reuses workflow engine for cron/retry/observability/manual-run/distribution. Sync history = workflow execution history. Drops `ContextStoreSyncJob`/`Launcher`/`Scheduler`/`SyncRun` table. Net: 1 new cluster element + 1 listener vs. ~7 new classes |
| 2026-05-08 | **Atlas execution delegate moves to MVP** (was post-MVP Phase 15 in earlier draft) | With DataStream as the sync mechanism, Atlas already runs every workflow natively — distribution is automatic via existing Worker dispatch. The standalone "Atlas dispatch wrapper" phase is superseded |
| 2026-05-08 | **Postgres for record store in MVP; ClickHouse as post-MVP swap (not dual-write)** | Postgres handles millions of records comfortably with GIN-on-JSONB + B-tree-on-typed-index. ClickHouse becomes a swap-in alternative behind `ContextStoreQueryService` + `ContextStoreItemWriter`. Control plane stays Postgres |
| 2026-05-08 | **Drop `context_store_sync_run` table** (was originally a status overlay over Spring Batch JobExecution) | Atlas already persists JobExecution rows for the auto-generated workflow. Friendly status (BUILDING_PREVIEW/PREVIEW/READY/FAILED) lives on `ContextStoreSource.status` + `last_sync_run_at` + `last_sync_job_execution_id`. JobExecution rows are queryable for sync history |
| 2026-05-08 | **`ItemReader.supportsIncremental()` capability flag (post-MVP DataStream SPI extension)** | Default false; sources that support `since: Instant` opt in. State persisted across runs via the existing `ExecutionContext.put/get` lifecycle. MVP is full pulls. Documented as a future-extension shape so the SPI design doesn't paint into a corner |
| 2026-05-08 | **CC chat surface gets full Context Store define tools (not just consume)** | Five define tool callbacks (create / update / delete / refresh / setEnabled) plus two discovery tool callbacks (list available source components / describe source component entities). All delegate to `WorkspaceContextStoreSourceFacade` — same code path as GraphQL/UI; no parallel implementation. Admin-role authorization on the facade methods + chat-level user-confirmation prompt before execution (per the platform's explicit-permission doctrine for workspace-infrastructure mutations). Lets users say "set up a HubSpot Contacts sync" and have the agent walk them through it instead of forcing them to leave the chat for the UI |
| 2026-05-09 (later same day) | **Move Knowledge Base to platform; apply the same pattern to KB-Source as it ships in Phase 13** | Owner pivot one day after the CS platform pivot. Rationale: KB itself is genuinely platform-shared — workflows, projects, OAuth flows, and AI agent surfaces reference KB the same way they reference `Connection`. The Connection/WorkspaceConnection split applied to KB earns its module-count cost: the entity, services, facades, file storage, REST surface, and the chunker/embedder ETL pipeline all become platform primitives that automation contexts consume independently. **Concrete changes (KB itself, commit `5cee82ab933`)**: `KnowledgeBase`, `KnowledgeBaseDocument`, `KnowledgeBaseDocumentChunk`, `KnowledgeBaseDocumentTag`, `KnowledgeBaseTag` entities, all repos/services/facades, `DocumentStatusUpdate` DTO, message-broker route, file-storage api+impl, REST controllers, and the worker module (chunker/embedder/OCR pipeline) all moved from `server/libs/automation/automation-knowledge-base/{api,service,...}/` to `server/libs/platform/platform-knowledge-base/{api,service,...}/`. Java package renamed `com.bytechef.automation.knowledgebase.*` → `com.bytechef.platform.knowledgebase.*`. The slim `automation-knowledge-base-{api,service,graphql}` modules now host only `WorkspaceKnowledgeBase` (relation), `WorkspaceKnowledgeBaseService`, `WorkspaceKnowledgeBaseFacade`, the GraphQL controllers (which import platform types), `KnowledgeBaseDocumentNotFoundException`, and `KnowledgeBaseJdbcRepositoryConfiguration`. Liquibase init also moved from `automation/knowledge_base/` to `platform/knowledge_base/` directory; logical paths pinned to the new locations (commit `de95f0819be`). **Implications for KB-Source (Phase 13)**: it follows the same pattern. `KnowledgeBaseSource` entity + repo + service + sync listener + sync helpers on `KnowledgeBaseDocumentService` live on `platform-knowledge-base-{api,service}` (no separate `platform-knowledge-base-source` module — KB-Source is a sub-domain of KB and rides on the same module tree). `WorkspaceKnowledgeBaseSource` relation + `WorkspaceKnowledgeBaseSourceFacade` (workflow auto-gen + ProjectDeploymentWorkflow lifecycle + manual job dispatch — all workspace-aware orchestration) live on `automation-knowledge-base-{api,service}` alongside the existing `WorkspaceKnowledgeBase`. The GraphQL controller stays in `automation-knowledge-base-graphql` per "in automation only leave graphql" — its imports cross both packages. The five inline sync columns on `knowledge_base_document` are added by the platform-side Liquibase changeset (since `knowledge_base_document` is now a platform table); the `workspace_knowledge_base_source` relation table is added by a companion automation-side changeset. Facade method signatures all take `workspaceId` as the first parameter — same shape as `WorkspaceContextStoreSourceFacade`. Listener placement (`platform-knowledge-base-service`) mirrors `ContextStoreSyncJobListener`'s placement in `platform-context-store-service` exactly: operates by `source_id`, no workspace lookup |
