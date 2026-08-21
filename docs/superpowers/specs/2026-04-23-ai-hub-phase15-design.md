# AI Hub Phase 15 — Agent Memory

**Status**: Draft
**Date**: 2026-04-23
**Pattern source**: [Spring AI Agentic Patterns 6: Memory Tools](https://spring.io/blog/2026/04/07/spring-ai-agentic-patterns-6-memory-tools).

## Goal

Give the agent a durable, per-user-per-workspace long-term memory it can use to retain and recall facts that outlive the current conversation: user profile ("Alice is a backend engineer who prefers concise replies"), project decisions, feedback corrections, and reference pointers. The agent decides autonomously when to read, create, update, or delete memories via six tool calls.

## Why hand-rolled (not Spring AI's `AutoMemoryToolsAdvisor`)

The Spring AI library version is filesystem-bound — it reads/writes Markdown files in a sandboxed directory. That's fine for local desktop agents; it does not fit ByteChef's PostgreSQL-centric, potentially-distributed deployment. Same rationale as Phases 3/4.5/4+ adopting the TaskTool **pattern** but not the library. Phase 14 reuses the **pattern shape** (6 tools, per-user+workspace scope, system-prompt guidance) with a DB-backed implementation.

## Architecture

### Data model

One new table:

```sql
CREATE TABLE ai_hub_memory (
    id             BIGSERIAL PRIMARY KEY,
    workspace_id   BIGINT       NOT NULL,
    user_id        BIGINT       NOT NULL,
    name           VARCHAR(128) NOT NULL,   -- slugified, e.g. "user_profile"
    title          VARCHAR(255) NOT NULL,   -- human-readable, e.g. "User profile"
    description    TEXT         NULL,       -- one-line summary shown in index
    memory_type    VARCHAR(32)  NOT NULL,   -- USER | FEEDBACK | PROJECT | REFERENCE
    content        TEXT         NOT NULL,   -- full markdown body
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP    NOT NULL,
    UNIQUE (workspace_id, user_id, name)
);
CREATE INDEX idx_ai_hub_memory_user_workspace
    ON ai_hub_memory (workspace_id, user_id, memory_type);
```

### Six tool callbacks (BUILD only — memory writes are mutations)

1. `listMemories` — returns an index of `{name, title, description, memoryType, updatedAt}`. No content bodies. Called by the agent at turn start (cheap); full bodies fetched on demand.
2. `readMemory({name})` — returns the full memory with content.
3. `createMemory({name, title, description?, memoryType, content})` — unique constraint on `(workspace, user, name)`; returns error if name collides.
4. `updateMemory({name, content?, description?, title?})` — partial update; at least one field required.
5. `deleteMemory({name})` — hard delete. (Per 5A.2's undo layer, this would log an artifact for reversal — see "Integration with Phase 6.1/12" below.)
6. `renameMemory({oldName, newName})` — rename the key; content unchanged.

All six are `BUILD`-mode only (create/update/delete/rename are mutations). `listMemories` and `readMemory` are pure reads but live in BUILD for consistency — BUILD is the mode where the agent's system prompt instructs it to use memories. ASK does not mention memories at all in v14.

### System-prompt updates

`prompt_ai_hub_build.txt` gains a **Memory** section at the top (before the other tools):

```
Long-term memory:
You have six memory tools for retaining facts across conversations:
- listMemories() — cheap index at turn start; check before answering "do
  you remember...?" or any personalized question.
- readMemory({name}) — full body when the index description is insufficient.
- createMemory({name, title, description?, memoryType, content}) — persist
  a new fact. memoryType: USER (profile, preferences), FEEDBACK (corrections
  or confirmed approaches), PROJECT (decisions/deadlines), REFERENCE
  (external system pointers — dashboards, boards).
- updateMemory / deleteMemory / renameMemory — evolve memories as facts
  change.

When to write:
- User explicitly says "remember this" / "from now on" / "I prefer...".
- User corrects your output with a generalizable principle ("always use
  real DB in integration tests" — FEEDBACK).
- User establishes a durable constraint ("our conversion marker is
  status='qualified'" — PROJECT).

When NOT to write:
- Conversation-scoped transient context (that's what ChatMemory is for).
- Information already visible in workspace data (files, workflows, tables).
- Trivial or duplicate facts.

Keep memory content short and factual (1-5 sentences per memory). Prefer
update over create when refining an existing memory. Before creating,
check listMemories for duplicates.
```

The agent calls `listMemories` opportunistically. The parent system prompt also appends the current index as a `Context` block so the agent sees memory names + descriptions without a tool call on most turns. `AiHubSpringAIAgent.createSystemMessage` is extended to inject `{state.memoryIndex: [{name, title, description, memoryType}]}` (computed server-side at request time using the current user+workspace).

### REST endpoints (user-facing management)

Users may want to inspect / edit / delete their memories through the UI (similar to the Audit page). New endpoints under `/api/platform/internal/ai-hub/memories`:

- `GET /memories?workspaceId=N` — list current user's memories in the workspace.
- `GET /memories/{id}?workspaceId=N` — full content.
- `PATCH /memories/{id}?workspaceId=N` — update fields.
- `DELETE /memories/{id}?workspaceId=N` — hard delete.

Admin-wide viewing is **out of scope for v14** — memories are per-user, private within the workspace.

### Client

- New route `/automation/ai-hub/memories` (user-gated, not admin-only). `LayoutContainer` pattern, same shape as Audit page from Phase 12.
- Left sidebar: filter by `memoryType`.
- Main body: table — title, memoryType badge, description, updatedAt, actions (View / Edit / Delete).
- Edit dialog: name (read-only), title, description, memoryType select, content textarea.

### Integration with Phase 6.1/12

Memory creates/updates/deletes produce `ConversationArtifact` rows (new kinds):
- `MEMORY_CREATED`
- `MEMORY_UPDATED` (with prior content in `metadata_json` for Phase 12 undo)
- `MEMORY_DELETED` (with prior full memory in metadata for undo)
- `MEMORY_RENAMED`

This gives memories the same audit + undo story as all other agent mutations. Implement the three new `ArtifactReverser`s for the reversible kinds in the same phase.

## Non-goals (v14)

- **Semantic / embedding-based memory retrieval** — simple keyed access only. Future phase: add an embedding column + top-K retrieval.
- **Cross-user memory sharing** — per-user private. Teammate visibility is future.
- **Automatic consolidation** ("merge duplicates, drop stale facts") — the Spring AI pattern suggests a periodic consolidation trigger. Out of scope for v14; user can prune via the management page.
- **Memory scopes beyond workspace + user** — e.g. per-project or per-conversation. One scope per row.
- **Admin-level viewing of other users' memories** — privacy concern; future phase with explicit authorization.

## Testing

### Server
- `ConversationMemoryRepositoryIntTest` — CRUD smoke with Testcontainers.
- `ConversationMemoryServiceImplTest` — ownership checks, unique name constraint, type validation.
- Tool callback tests — 6 files × 3–4 tests each.
- Controller test — 4 endpoints × happy + 403.
- Agent system-message test — `state.memoryIndex` present → context block appended; empty → omitted.

### Client
- Query hook tests.
- Memories page renders + filters + edit flow.

## Commit convention

`CC15 …` / `CC15 client - …`.
