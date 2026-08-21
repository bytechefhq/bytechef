# AI Hub Phase 6.1 — Conversation Artifacts

**Status**: Draft
**Date**: 2026-04-23
**Builds on**: Phase 6 (conversation persistence).

## Goal

Tie every agent-produced side-effect (created file, applied mutation, workflow execution kick-off, generated image/deck) back to the conversation that produced it. The sidebar surfaces a per-conversation count badge and an expand-to-list that shows exactly what was produced, with quick-open links.

## Data model

One new table, FK-joined to Phase 6's `ai_hub_task`:

```sql
CREATE TABLE ai_hub_task_artifact (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT        NOT NULL REFERENCES ai_hub_task(id) ON DELETE CASCADE,
    kind            VARCHAR(64)   NOT NULL,
    artifact_id     VARCHAR(255)  NOT NULL,  -- the target entity id (file id, workflow id, row id, etc.)
    artifact_name   VARCHAR(255)  NOT NULL,  -- display snapshot at creation time
    metadata_json   TEXT          NULL,      -- optional extra context (e.g. {projectId, columnName})
    created_at      TIMESTAMP     NOT NULL
);
CREATE INDEX idx_cc_conversation_artifact_conversation
    ON ai_hub_task_artifact (conversation_id, created_at DESC);
```

Artifact kinds (enum `ConversationArtifactKind`):

- `FILE_CREATED` — text file via `createAssetFile`
- `BINARY_FILE_CREATED` — image / pptx via `createBinaryAssetFile`
- `WORKFLOW_CREATED`
- `WORKFLOW_UPDATED`
- `DATA_TABLE_ROW_ADDED`
- `DATA_TABLE_ROW_UPDATED`
- `DATA_TABLE_ROW_DELETED`
- `DATA_TABLE_COLUMN_ADDED`
- `KB_DOCUMENT_ADDED`
- `KB_DOCUMENT_DELETED`
- `WORKFLOW_EXECUTION_STARTED` — `runChatWorkflow` fired

`ON DELETE CASCADE` means deleting a conversation also drops its artifact rows. Clean.

## Recording path

Two contexts where artifacts get written:

1. **Synchronous agent tool calls** — run in the parent agent's thread during an AG-UI turn. Conversation id is available as `state.threadId` → resolve to `conversation_id` once per call. Affected callbacks:
   - `CreateAssetFileToolCallback`
   - `CreateBinaryAssetFileToolCallback`
   - `RunChatWorkflowToolCallback`

2. **Mutation applier path** (Phase 5A) — runs when the user clicks Apply in the Pending Changes bar. No AG-UI context. The **client** passes the current `conversationId` (from `useAiHubTasksStore`) in the apply REST body. Each `MutationApplier` receives it and, on success, records the artifact.

For context propagation (1): `WorkspaceInvocationContext` gains a `threadId` field (already in AG-UI state as the conversation's thread id). `AiHubSpringAIAgent.buildInvocationContext(...)` fills it. Each synchronous callback pulls it via `WorkspaceContextProvider.currentThreadId()`.

For path (2): the `applyPendingMutation` API wrapper takes an optional `threadId` → sent as body field; `MutationApiController` forwards it to the applier registry; each applier records the artifact.

## Service

`ConversationArtifactService` (ai-copilot-api):
- `void record(String threadId, long userId, ConversationArtifactKind kind, String artifactId, String artifactName, Map<String, Object> metadata)` — resolves `threadId` → conversation via `ConversationService.findByThreadIdAndUserId(...)`. Silently no-ops if the conversation doesn't exist (e.g., anonymous / non-ai-hub tool invocation).
- `List<ConversationArtifact> listByConversation(long conversationId, long requesterUserId)` — ownership check via `ConversationService`, then repo query.
- `int countByConversation(long conversationId)` — for the sidebar badge.

## REST endpoint

Extend `ConversationApiController`:

- `GET /conversations/{id}/artifacts?workspaceId=N` — returns `List<ConversationArtifact>` newest-first. Ownership-checked.

No write endpoint — artifacts are server-authored.

## Client

- New React Query hook `useAiHubTaskArtifactsQuery(conversationId)` — 1 minute stale time.
- Conversation row (`AiHubTaskItem` equivalent in the sidebar) gains a small badge when artifacts exist: e.g. `📄 3` / `🔧 2` / `📊 5` / combined.
- Expanding a row (new behavior: click the badge, or a small chevron) reveals the artifact list with per-row icons keyed by kind. Each row has a quick-open affordance:
  - `FILE_CREATED` / `BINARY_FILE_CREATED` — opens the file tab via the existing tabs store.
  - `WORKFLOW_CREATED` / `WORKFLOW_UPDATED` — opens the workflow tab.
  - `DATA_TABLE_ROW_*` / `DATA_TABLE_COLUMN_*` — opens the data-table tab.
  - `KB_DOCUMENT_*` — opens the KB tab.
  - `WORKFLOW_EXECUTION_STARTED` — deep-link to the workflow execution page.

Applying pending mutations now sends `{toolName, inputJson, threadId}` where `threadId` comes from `useCopilotStore.conversationId`.

## Non-goals (v6.1)

- **No artifact editing / retagging**.
- **No cross-conversation reconciliation** (e.g. "this file was last edited in conversation B; show that").
- **No undo hooks** — undo is Phase 5A.2. When it ships, it can consume this artifact log.

## Testing

### Server
- `ConversationArtifactRepositoryIntTest` (Testcontainers) — save + find + cascade on conversation delete.
- `ConversationArtifactServiceImplTest` — record + list + count + no-op on unknown threadId.
- REST `GET /artifacts` test added to `ConversationApiControllerTest`.
- Each affected tool-callback test asserts the service is called with the right kind+id+name on the success path.
- Each applier test similarly asserts the service call.

### Client
- Query hook test.
- Sidebar badge renders with artifact count.
- Expand-to-list shows correct icons + quick-open fires the right tab action.
- Apply request includes `threadId` in body.

## Commit convention

`CC6.1 …` / `CC6.1 client - …`.
