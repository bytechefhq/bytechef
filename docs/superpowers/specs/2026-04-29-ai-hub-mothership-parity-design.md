<!-- Source: https://docs.sim.ai/mothership and https://docs.sim.ai/mothership/{workflows,research,files,tables,tasks,knowledge} -->

# AI Hub — Mothership Parity Design Spec

**Status**: Effectively complete (P0 cut, P1/P2/P3.2/P4 shipped, P3.3 deferred)
**Date**: 2026-04-29
**Scope**: Multi-phase plan to close the feature gaps between ByteChef's AI Hub and sim.ai's Mothership.
**Audience**: AI Hub maintainers, Copilot/Agentic-AI module owners, EE platform team.

## Goal

Bring ByteChef's AI Hub to feature parity with sim.ai's Mothership — an "AI command center" where users can describe what they want and the system orchestrates workflows, files, tables, knowledge bases, scheduled jobs, and direct actions through a single chat surface.

## Outcome at a glance

The original spec walked sim.ai's UI surface and reverse-mapped each feature to a ByteChef subsystem to build. Working through the phases revealed a consistent pattern: **most of sim.ai's "subsystems" already exist in ByteChef behind a different mental model** — workflows + scheduler + project deployments + ApiCollection + McpProject + the 180+ component library together cover what sim.ai exposes as discrete features. Most of the actual work was authoring the right chat affordances over those existing primitives, not building parallel infrastructure.

## Priority order & rationale

| # | Gap | Status |
|---|---|---|
| **P0** | Scheduled AI Jobs + Direct Actions + workspace env vars + custom HTTP tools | **Cut.** All four sub-features collapse to chat-authored workflows over existing primitives. See "P0 — cut" below. |
| **P1** | Document, presentation, chart & code-file generation | **Shipped** (7 slices). Per-format generators (MD/CODE/CSV/JSON/DOCX/PPTX/CHART) + 3-pane viewer + Generated tab in Files. |
| **P2** | Knowledge Base auto-sync connectors | **Shipped** as a 6-template workflow gallery (Notion / GDrive / GitHub / Confluence / Slack / Webhook → KB). No backend; existing workflow runtime + components do the sync. |
| **P3** | Workflow polish (chat-driven deploy/rollback) | **Shipped** (3.2 — five thin chat callbacks over `ProjectDeploymentFacade`, `ApiCollectionFacade`, `McpProjectFacade`). 3.1 cut (overlaps with data tables); 3.3 deferred (folder entity doesn't exist). |
| **P4** | Tables polish | **Shipped** (CSV import + filtered CSV export from chat). |

The remainder of this document is organized phase-by-phase. P0 is documented for posterity (why it was cut); P1–P4 retain the original implementation plan with status notes.

---

# P0 — Cut

## Goal (original)

Introduce a "prompt-as-entity" primitive — a saved AI Hub prompt that can run on a schedule, be re-invoked one-shot, or be exposed as a tool. Mothership's "Tasks" page is really four sub-features sitting on this primitive: scheduled jobs, direct actions, workspace env vars, custom HTTP tools.

## Why it was cut

Each sub-feature collapses to **chat-authored workflows over existing primitives** — none require the proposed `ai_hub_prompt` table, separate Quartz wiring, or a parallel prompt-runtime.

| P0 sub-feature | Existing primitive that already covers it |
|---|---|
| Scheduled AI Jobs | Workflow with `schedule/v1/*` trigger + `llm/v1/ask` task + (optional) `dataTable/v1/insertRow` task. Authored via the existing `CreateWorkflowToolCallback`; scheduled, monitored, and version-pinned by the existing `ProjectDeploymentFacade`. The Atlas execution engine already records every run for monitoring. |
| Direct Actions | One-task workflow authored via `CreateWorkflowToolCallback` and run via `principalJobFacade.createJob`. Or, since chat-trigger workflows already exist, `RunChatWorkflowToolCallback` covers the fire-once invocation pattern. |
| Custom HTTP-endpoint tools | `httpClient/v1/*` is a built-in component action. A "custom HTTP tool" is a workflow with one `httpClient` task, exposed as an MCP tool via `createMcpProject` (P3.2). |
| Workspace env vars | Already covered by the existing data-table store (mutable workspace-scoped key-value rows accessible from `Evaluator` placeholders) plus connection credentials (encrypted secrets). The `workflow_variable` table proposed here was the same one cut from P3.1 for the same reason. |

The `prompt-as-entity` mental model is the only thing that doesn't fit — but workflows ARE prompts-as-entities once you accept that a workflow with a single LLM task is exactly that, with version pinning, env scoping, scheduler, monitoring, and audit already built in.

## What this means for chat affordances

The original P0 effort estimate (~3 weeks) was for net-new infrastructure. After cutting, P0 reduces to **zero new tool callbacks** — the existing chat tools already let users describe a scheduled prompt or one-shot action and the agent composes the appropriate workflow JSON, calls `CreateWorkflowToolCallback`, and (if scheduling) `CreateProjectDeploymentToolCallback` from P3.2 to deploy it.

If we ever discover a sub-feature that genuinely needs a separate primitive — e.g., a "prompt template library" UI distinct from workflows — that's a future spec, not P0.

---

# P1 — Document, Presentation, Chart & Code-File Generation

## Goal

Match Mothership's "Files & Documents" capability where the AI generates first-class artifacts — markdown reports, Word/PowerPoint decks, CSV/JSON data files, charts (bar/line/pie), and images — surfaced in a workspace Files panel with a 3-pane (Editor / Preview / Split) viewer.

## Why this is P1, not P0

Highest demo impact, but it's a substantial *new* subsystem (not a remix of existing parts):

- Per-format generators (markdown, PPTX via Apache POI, DOCX via POI, charts via QuickChart or matplotlib-via-GraalVM, images via existing `ImageGeneratorToolCallback`).
- A 3-pane viewer in the client.
- An EE conversation-linkage layer over the existing `asset_file` entity — no new file-storage tier needed.

## Architecture

### Reuse `asset_file`, don't introduce a new entity

ByteChef's [AssetFile](server/libs/automation/automation-asset-file/automation-asset-file-api/src/main/java/com/bytechef/automation/assetfile/domain/AssetFile.java) already covers ~90% of what Mothership calls a "file": dual `source: USER_UPLOAD | AI_GENERATED`, `generatedByAgentSource` (which EE agent produced it), `generatedFromPrompt`, tagging via `asset_file_tag`, workspace scoping via `workspace_asset_file`, audit fields, optimistic version, and a `validate()` invariant that enforces source/AI-field consistency. Reinventing this as `ai_hub_artifact` would create two file stores that have to stay in sync (rename, delete, tag propagation) — a perpetual sync hazard with no upside.

The plan instead:

#### 1. Extend `asset_file` with two nullable columns (CE)

```sql
ALTER TABLE asset_file ADD COLUMN format        VARCHAR(16) NULL;  -- MARKDOWN | DOCX | PPTX | CSV | JSON | CODE | CHART | IMAGE
ALTER TABLE asset_file ADD COLUMN metadata_json TEXT        NULL;  -- chart spec, slide list, etc.
```

Both nullable so existing rows are unaffected. `format` is useful for any caller (uploads benefit from a sniffed format too — `mime_type` alone doesn't distinguish "code file" from "JSON file from a tool"). Keeping these on the existing entity, not split out, is the single-source-of-truth move.

#### 2. New EE-only conversation-linkage join table

```sql
CREATE TABLE ai_hub_task_asset_file (
    conversation_id  BIGINT       NOT NULL REFERENCES ai_hub_task(id) ON DELETE CASCADE,
    asset_file_id    BIGINT       NOT NULL REFERENCES asset_file(id)                  ON DELETE CASCADE,
    relationship     VARCHAR(16)  NOT NULL,    -- AUTHORED | ATTACHED | MENTIONED | READ_BY_TOOL
    created_at       TIMESTAMP    NOT NULL,
    PRIMARY KEY (conversation_id, asset_file_id, relationship)
);

-- An asset_file can be AUTHORED by at most one conversation (partial unique index)
CREATE UNIQUE INDEX uk_asset_file_authored_once
    ON ai_hub_task_asset_file (asset_file_id)
    WHERE relationship = 'AUTHORED';

CREATE INDEX idx_cc_conv_asset_file_conv
    ON ai_hub_task_asset_file (conversation_id);
```

The join lives on the EE side (`ai-copilot` module) so CE schema doesn't carry CC-specific FKs. The partial unique index on `relationship='AUTHORED'` enforces the 1-author rule without a column on `asset_file`.

#### 3. Semantics

| Action | Effect |
|---|---|
| User uploads file outside any conversation | `asset_file` row, `source=USER_UPLOAD`. No join row. |
| Agent generates file in conversation X | `asset_file` row, `source=AI_GENERATED`, `generatedByAgentSource = CC ordinal`, `generatedFromPrompt = the prompt`. **One** join row `(X, file, AUTHORED)`. |
| User attaches existing file in Y | Insert `(Y, file, ATTACHED)`. |
| Agent reads existing file via tool in Y | Insert `(Y, file, READ_BY_TOOL)`. |
| Y's "Files" panel | Query the join table where `conversation_id = Y` and join to `asset_file`. Shows authored-here + referenced-here together. |
| User removes file from Y's panel | Removes Y's join rows. File survives if any other reference exists. Workspace-wide deletion is a separate explicit action. |

#### 4. Distinct from `ai_hub_task_artifact` (existing)

Note the naming: the existing `ai_hub_task_artifact` table is a **mutation log** for the Phase 12 audit/undo system (kinds like `WORKFLOW_CREATED`, `DATA_TABLE_ROW_ADDED`, `MEMORY_RENAMED`). It is unrelated to file storage and is not touched by this phase. The Mothership-style "files" UI uses `asset_file` + the new `ai_hub_task_asset_file` join exclusively.

### Generator pipeline

A new `ArtifactGenerator` interface plus per-format implementations. Generators are synchronous, invoked from agent tool callbacks, and return the persisted `AssetFile` row id once the full artifact is written:

```java
public interface ArtifactGenerator {
    AssetFileFormat format();
    GenerationResult generate(GenerationRequest request);
}
```

Implementations:

| Format | Engine | Notes |
|---|---|---|
| `MARKDOWN` | Plain text passthrough | The LLM already streams the content into chat; the generator captures the final value. |
| `CODE` | Same as markdown | Mime-type set from inferred language. |
| `CSV` / `JSON` | Spring AI structured-output API | Validates schema before write. |
| `DOCX` | Apache POI XWPF | Already a transitive dep via `automation-task`. |
| `PPTX` | Apache POI XSLF | Replaces existing `SlideBuilderToolCallback` (deprecate, redirect). |
| `CHART` | QuickChart HTTP API or in-process via JFreeChart | JFreeChart preferred for offline; QuickChart fallback. |
| `IMAGE` | Existing `ImageGeneratorToolCallback` (DALL-E) | No change. |

### No new transport — reuse the chat tool-call channel

There is **no new WebSocket event type or chunked streaming protocol** for artifacts. The existing chat surface already gives us everything needed:

1. The agent emits a generator tool call (e.g. `generate_chart(spec=…)`) — visible in chat exactly like any other tool call today.
2. Generator runs server-side, writes one `asset_file` row plus one `(conversation_id, asset_file_id, AUTHORED)` join row.
3. Tool result returns `{assetFileId, name, format}` — rendered in chat as a "📄 report.md" chip via the existing tool-call renderer.
4. Click chip → `ArtifactViewer` opens → fetches via existing `asset_file` REST endpoints → renders.

Why no streaming layer:

- **Text formats already stream** through the existing chat token stream. The LLM emits the content into chat as it produces it; the tool call only persists the final shape. Re-streaming the same bytes via a separate `ARTIFACT_CHUNK` event would be duplicate work the user doesn't see.
- **Binary formats can't stream usefully** — half a PPTX, half an image, half a chart is unrenderable. The user sees a "generating…" chip; that's it.
- **Removed code surface**: no chunk sequencing, no base64 encoding, no client buffer reconciliation, no partial-render fallbacks.

For the rare slow generator (LibreOffice PPTX preview render in particular), expose progress as a small **SSE endpoint per artifact** — one-way server→client, dramatically simpler than WebSocket — *only if* generation routinely exceeds a few seconds in practice. Default is synchronous tool execution.

### Viewer — extend the existing component

[AiHubFileViewer.tsx](client/src/pages/automation/ai-hub/AiHubFileViewer.tsx) already exists and already does most of what Mothership shows for files: Monaco code editor with mime/extension language detection, `react-markdown` preview, image rendering, a `viewMode` prop that drives Editor / Preview / Split panes, and a download affordance. The PPTX mime constant is even recognized. So P1's "build a 3-pane viewer" is **not new construction** — it's adding format-specific render modes to an existing viewer:

- **CHART** — render `metadata_json` (chart spec) via Recharts in Preview mode.
- **DOCX** — server-side render to PDF on demand; embed via iframe.
- **PPTX** — same as DOCX. Render path: LibreOffice headless (already containerized in `docker-compose.dev.infra.yml` for similar conversion needs in `automation-knowledge-base`); cache the PDF for 24h. Cheaper alternative open question: render-to-images per slide via POI directly.

Markdown, code, CSV, JSON, and image previews already work in the existing viewer with no changes.

### Client UX

- Files panel sidebar gains a tab: **Generated** (alongside **Uploaded**).
- New chat tool affordance: when a generator tool call is in flight, the chip renders as `[generating: report.md…]` driven by the existing tool-call rendering state machine — no new event subscription. Once the tool result arrives, the chip becomes `📄 report.md` and clicks open the viewer.
- **Open-in-tab plumbing is already shipped.** The existing [OpenFileTabToolCallback](server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/tool/OpenFileTabToolCallback.java) is a signaling-only tool whose result is intercepted by [AiHubRuntimeProvider](client/src/pages/automation/ai-hub/runtime-providers/AiHubRuntimeProvider.tsx) and routed into [useAiHubTabsStore](client/src/pages/automation/ai-hub/stores/useAiHubTabsStore.ts) — which then renders the file in `AiHubFileViewer`. Generators only need to emit `openFileTab(fileId, name)` after creating; no new client wiring.
- Drag from any artifact card → context attachment in the chat (matches Mothership's `@filename` model).

### Effort estimate

~4 weeks. Generator interface + 5 format implementations: 2.5w. Viewer extensions (chart render mode + DOCX/PPTX preview integration into the existing AiHubFileViewer): 1w. Migration of existing SlideBuilder to the new pipeline: 0.5w. (No greenfield viewer or transport work — those already ship.)

### Open questions

1. **Editing-then-regenerating**: Mothership lets you edit a generated file and ask the agent to revise. Do we treat each edit as a new artifact version, or in-place mutation? Recommendation: append-only versioning (new row, same `name`).
2. **PPTX preview engine**: LibreOffice headless adds a heavy dep. Cheaper alternative: render-to-images per slide via POI directly. Tradeoff: fidelity vs. footprint.
3. **Slow generators**: if PPTX render or LibreOffice preview routinely takes more than ~3s, do we expose a progress SSE endpoint per artifact, or accept the longer "generating…" chip wait? Defer until measured — premature optimization otherwise.

---

# P2 — Knowledge Base Auto-Sync (rescoped)

## What changed and why

The original spec proposed a parallel sync subsystem — `knowledge_base_connector` + `knowledge_base_connector_doc` tables, a `ConnectorSyncAdapter` interface, a Quartz orchestrator, and six per-source adapters (~3 weeks of work). **That is duplication.** ByteChef already has all the moving parts:

- **180+ component actions** for the very sources Mothership lists (Notion list-pages, GDrive list-files, GitHub list-contents, Confluence get-pages, Slack channel-history, etc.).
- **Polling and webhook triggers** on each component for "fire on schedule / fire on change" semantics.
- **An action-based "Add Document" on the Knowledge Base component** that takes content + name and runs the same chunk → embed → index pipeline a connector would call.
- **A workflow runtime + scheduler + retry/backoff + audit log** that an in-tree connector subsystem would have to reimplement.

Building a separate `ConnectorSyncAdapter` API on top of these is layer duplication. A workflow with a polling Notion trigger → Notion list-pages → Knowledge Base Add Document does the same thing with the same retry guarantees, in the surface the user already knows how to edit, audit, and version.

## What to ship instead

A **workflow-template gallery for KB sync**. Each template is a single workflow JSON — no new entities, no new orchestrator. Ship six templates:

| Template | Trigger | Steps |
|---|---|---|
| Notion → KB | Polling (hourly) | `notion.searchPages` → loop → `knowledgeBase.addDocument` |
| GDrive → KB | Polling (hourly) | `googleDrive.listFiles` (folder) → loop → `googleDrive.downloadFile` → `knowledgeBase.addDocument` |
| GitHub → KB | Polling (daily) | `github.listRepositoryContents` (path glob) → loop → `github.getFileContent` → `knowledgeBase.addDocument` |
| Confluence → KB | Polling (hourly) | `confluence.searchPages` (space) → loop → `knowledgeBase.addDocument` |
| Slack channel → KB | Polling (daily) | `slack.fetchConversationHistory` → format as markdown → `knowledgeBase.addDocument` |
| Generic webhook → KB | Webhook trigger | webhook payload → `knowledgeBase.addDocument` |

The user clones the template, sets the connection + filter (folder id, db id, channel id), and the workflow runs on the existing scheduler. Delete-propagation is the only thing the workflow doesn't get for free — when it matters, the template can include a "list current KB docs → diff against source listing → delete missing" step using the existing `knowledgeBase.deleteDocument` action.

## Effort estimate

~2 days. Author six workflow JSONs (mostly copy-paste-tweak), drop them into the existing template gallery infrastructure at [client/src/pages/automation/templates/workflow-templates](client/src/pages/automation/templates/workflow-templates), tag them as "Knowledge Base", and write a one-paragraph blurb per template. No backend work.

## Why this is the right answer

- **No second source of truth** for "what's in the KB and where it came from" — the existing `knowledge_base_document` table is the only record, just like for manual uploads.
- **No second scheduler** to operate, monitor, or alert on.
- **Per-source customization is free** — the user writes a workflow expression (`<filter>`, `<transform>`) directly. A `knowledge_base_connector.config_json` blob can't reach that.
- **The component team can ship sources** without coordinating with the KB team. Adding HubSpot KB sync is "publish a HubSpot workflow template," not "write a `HubSpotKnowledgeBaseConnectorSyncAdapter`."

## What we *don't* get from this rescope

- **A polished one-click "connect Notion to my KB" experience.** A workflow template is two clicks (clone → fill auth) plus filling the source filter, vs Mothership's one-click connector picker. If user research shows that gap matters, the right fix is a chat-driven workflow-template instantiator (the Copilot agent fills the workflow params from natural language) — orders of magnitude cheaper than building a sync subsystem and lands inside the Copilot strategy we're already executing.

---

# P3 — Workflow Polish

## Status — landed (3.2 only)

After scoping review the originally proposed P3 collapsed to one sub-feature, and that one shipped:

- **3.1 Workflow global variables** — **cut**. The proposed `workflow_variable` table overlaps too much with the existing `data_table` (workspace-scoped, mutable, schema-bound key-value rows) and `ai_hub_memory` (chat-bound mutable state) to justify a third namespace. The motivating use cases — counters/flags set by a workflow run — fit in data tables; the access patterns don't actually fight there because data-table reads/writes already go through the same Evaluator hooks.
- **3.2 Deploy / rollback / MCP / REST** — **landed**. See "Landed" below.
- **3.3 Chat-driven folder ops** — **deferred**. Workflow folders aren't a domain entity yet. Adding folder mutations on a non-existent aggregate is greenfield design work, not polish.

## 3.2 — what was already built before this work

The original spec under-stated how much existed server-side. The deploy/rollback/MCP/REST machinery is essentially complete:

- **`ProjectDeployment` aggregate** (`server/libs/automation/automation-configuration/.../domain/ProjectDeployment.java`) pins a `(project_id, project_version, environment)` tuple with `enabled` flag and an optimistic-lock `version` column. "Rollback to v2" is literally a different `project_version` value on the same row, not a separate domain concept. The facade enforces the domain rules (project must be PUBLISHED, target version must not be the current DRAFT) and surfaces violations as `ConfigurationException` with a typed `ProjectDeploymentErrorType`.
- **`ApiCollection` aggregate** (EE — `server/ee/libs/automation/automation-api-platform/.../domain/ApiCollection.java`) — this *is* the "Deploy as REST" surface. Each `ApiCollection` references one `ProjectDeployment` (via `AggregateReference<ProjectDeployment, Long>`), bundling it under a `context_path` with its own `collection_version` (the API contract version, evolving on its own axis from the project source version). `ApiCollectionFacade.getOpenApiSpecification(id)` auto-emits the OpenAPI spec from the underlying workflows; `ApiCollectionEndpoint` rows map individual workflows to REST routes. EE-gated.
- **`McpProject` + `McpProjectWorkflow`** (`server/libs/automation/automation-mcp/`) attach a project version's workflows to an existing `McpServer` as MCP tools. `McpProjectFacade.createMcpProject(mcpServerId, projectId, projectVersion, workflowIds)` stands up the binding (which itself creates the underlying `ProjectDeployment`), exposing each workflow as a tool LLM clients can invoke. Workspace MCP servers are tracked in `platform-mcp` with their own `enabled`/environment state and a generated `secret_key` for client auth.
- **Hosted chat** doesn't need a per-project surface — chat-trigger workflows already enumerate via `ListChatWorkflowsToolCallback` and execute via `RunChatWorkflowToolCallback`. The "deploy as hosted chat" affordance is "make sure the workflow has a `chat/newChatRequest` trigger in mode=1 (hosted-chat) inside an enabled deployment" — covered by the existing list+run pair.

What was actually missing was the chat-driven affordance over those existing facades. That's what landed.

## Landed — five thin Spring AI tool callbacks

Each callback is a 1:1 wrapper over the existing facade method. No new domain, no new persistence, no new endpoints. All registered on `aiHubBuildSpringAIAgent` (write-mutations don't belong on the read-only ASK agent).

| Callback | Backing facade | Purpose |
|---|---|---|
| `createProjectDeployment` | `ProjectDeploymentFacade.createProjectDeployment(ProjectDeploymentDTO)` | Pins a published project version to a target environment. Created `enabled=false` so the user wires per-workflow connections in the UI before triggers go live. |
| `rollbackProjectDeployment` | `ProjectDeploymentFacade.getProjectDeployment` + `updateProjectDeployment(DTO)` | Loads the existing deployment, swaps `projectVersion`, persists. The optimistic-lock `version` field is carried through unchanged so the underlying update fails fast on a concurrent edit. Preserves enabled state, environment, and per-workflow connections across the version change. |
| `toggleProjectDeployment` | `ProjectDeploymentFacade.enableProjectDeployment(id, boolean)` | Enable cascades to enabling each contained `ProjectDeploymentWorkflow`'s static-webhook triggers; disable tears them down. |
| `createApiCollection` | `ApiCollectionFacade.createApiCollection(ApiCollectionDTO)` | EE-only "Deploy as REST" — bundles a deployment under a `contextPath` with its own `collectionVersion`. Endpoints (per-workflow REST routes) are added separately via the existing `createApiCollectionEndpoint` flow. |
| `createMcpProject` | `McpProjectFacade.createMcpProject(mcpServerId, projectId, projectVersion, workflowIds)` | Attaches a project version's workflows to an existing MCP server as tools. The MCP server itself must already exist; this callback does not stand up a new server. |

### Wiring notes

- All callbacks accept `WorkspaceInvocationContext` via `ToolContext` for environment/user disambiguation, matching the P1/P4 pattern.
- `ApiCollectionFacade` and `McpProjectFacade` are injected via `ObjectProvider` into `aiHubBuildSpringAIAgent` so the `ai-copilot-service` jar still loads in lightweight deployments without the api-platform / mcp service modules. `ProjectDeploymentFacade` is CE so direct injection is fine.
- Domain-rule violations surface as structured `{error: <message>}` JSON (via `ToolErrors.toolError`), not as runtime-failure stack traces. This pins `ConfigurationException` (from the project-not-published / DRAFT-version checks) into the recoverable arm so the LLM can suggest a fix to the user instead of aborting the agent run.

### EE vs CE split

- `createProjectDeployment`, `rollbackProjectDeployment`, `toggleProjectDeployment` — CE (use only CE facades).
- `createApiCollection`, `createMcpProject` — EE (api-platform-configuration is EE; mcp-server is CE-API but the facade impl ships in EE bundles).

### What it cost

~1 day for callbacks + tests + Spring wiring + spec rewrite. The original 1.5-week estimate dropped to a single-day landing once 3.1 and 3.3 were cut and 3.2 was scoped to "chat affordance over what already exists" instead of "build the deploy machinery from scratch".

---

# P4 — Tables Polish

## Goal

Two small affordances Tables is missing.

## Sub-features

### 4.1 CSV upload → schema inference

New tool callback `CreateDataTableFromCsvToolCallback` that:

1. Reads first ~100 rows.
2. Infers column types (string / number / boolean / date) via heuristic + LLM tiebreaker for ambiguous columns.
3. Calls `dataTableService.createDataTable(...)` with the inferred schema.
4. Bulk-inserts rows.

### 4.2 Filtered CSV export from chat

Extend `QueryDataTableToolCallback` with an `exportToCsv` flag. When true, the result rows are written as a CSV `asset_file` row (`source=AI_GENERATED`, `format=CSV`) and registered with the conversation via the P1 join table, rather than returned inline. Two-line change once P1 is in.

### Effort estimate

~3 days total.

---

# Cross-cutting concerns

## Cost & usage

Every phase produces LLM calls and external-tool calls. They all flow through the Phase 17 usage tables (`ai_hub_usage`, `ai_hub_tool_usage`). Phase 17 was a hard prerequisite for P1–P3.

## EE vs CE split (as shipped)

- ~~P0 — cut.~~
- P1 generation: **CE** for markdown/code/CSV/JSON/charts. **EE** for PPTX/DOCX (heavyweight POI dependency stays in EE app images so the CE footprint is small).
- P2 templates: **CE-friendly** (just workflow JSON; renders against whichever components the running edition has).
- P3.2 deployment callbacks: mixed — `createProjectDeployment` / `rollbackProjectDeployment` / `toggleProjectDeployment` are **CE** (`ProjectDeploymentFacade` is CE); `createApiCollection` is **EE** (api-platform module); `createMcpProject` uses the CE `McpProjectFacade` interface but its impl ships in EE bundles.
- P4: **CE**.

EE files use the ByteChef Enterprise license header and `@version ee` Javadoc tag per repo conventions.

## Security

- ~~Env vars and HTTP tool credentials encrypted at rest~~ — covered by the existing connection-credential storage and data-table secret columns; no new surface added.
- ~~Custom HTTP tool URL blocklisting~~ — handled by the existing `httpClient` component's SSRF protection.
- Workflow-trigger audit goes through the existing `persistent_audit_event` path; no new audit events introduced by the parity work.

## Telemetry / metrics

Added to the `bytechef_ai_hub` meter group:

- `bytechef_ai_hub_artifacts_generated` (Counter, tags: `format`) — from P1.
- ~~`bytechef_ai_hub_job_runs`~~ — not added; scheduled prompts run as workflows so the existing Atlas execution metrics cover this.
- ~~`bytechef_ai_hub_kb_connector_syncs`~~ — same reason; KB sync runs as a workflow.

## Testing strategy

Per repo conventions:

- Integration tests end with `IntTest`, use `@SpringBootTest(webEnvironment = RANDOM_PORT)` + Testcontainers PostgreSQL 15.
- Unit tests for adapters/mappers; integration tests for full sync flows.
- For P1 binary-format generators: snapshot tests against committed reference files (delete `build/resources/test/` before regenerating, per the existing `DefinitionFactoryTest` pattern).
- For P2 KB-sync templates: each template is a workflow JSON validated by the existing workflow loader. Smoke-test by running each template against a fixture connection in CI.

---

# Final status & total effort

| Phase | Estimate (original) | Actual |
|---|---|---|
| P0 | ~3 weeks | **0 days — cut**; covered entirely by existing workflow + scheduler + project-deployment + httpClient component |
| P1 | ~4 weeks | shipped (7 slices) |
| P2 | ~2 days | shipped (6 templates, no backend) |
| P3 | ~1.5 weeks | ~1 day landed (3.2 — five thin chat callbacks); 3.1 cut, 3.3 deferred |
| P4 | ~3 days | shipped |

The parity initiative is **effectively complete**. The single deferred sub-feature (P3.3 — chat-driven folder ops) waits on a folder entity that doesn't exist; reopen when there's a concrete user request.

## What we learned

The original spec's ~9-week estimate assumed parallel infrastructure for sim.ai's surface features. Working through each phase revealed the same pattern over and over: **ByteChef's existing primitives (workflow + scheduler + project deployment + ApiCollection + McpProject + 180+ component library) already cover what sim.ai exposes — just behind a different mental model.** The actual delivered work was authoring chat affordances over those primitives. Future "parity" specs against other AI ai-hub products should start with the question "which existing ByteChef primitive does this map to?" before scoping new infrastructure.
