# AI Hub Phase 6 — Conversation persistence

**Status**: Draft
**Date**: 2026-04-23
**Builds on**: All prior phases merged.
**Scope**: Turn AI Hub conversations into first-class, browseable entities. Users can list past conversations, reopen one, rename it, and archive or delete it. Message contents reuse Spring AI's existing `SPRING_AI_CHAT_MEMORY` table keyed by thread id; a thin new metadata table holds conversation-level fields (title, timestamps, preview, status).

---

## Goal

Today the AI Hub persists the LLM's chat memory (via `SPRING_AI_CHAT_MEMORY` when the JDBC memory provider is enabled) but exposes **nothing** to the user. A user who navigated away cannot find their past conversation. This phase fills that gap: conversations become durable, listable entities with auto-generated titles. Users can open a historical conversation and continue it, rename for clarity, archive to declutter, or delete to reclaim space.

Success in v6: a user asks the agent something on Monday, navigates away, returns on Wednesday, opens a left-side conversation drawer, clicks Monday's conversation, and sees the full message history rehydrated in the chat. The agent resumes with the same context the LLM had at the end of Monday's turn.

## Non-goals (v6)

- **Cross-user sharing**. Conversations are private to the user who created them. Sharing with teammates is a later phase.
- **Full-text search across messages**. Title-only filter in v6.
- **Branching / forking** a conversation.
- **Automatic summarization** of long conversations (beyond the one-shot title generation).
- **Export** (markdown download, copy to file, etc.). Future.
- **Conversation-scoped artifact linkage** (which files were created in which conversation). Would be useful but is a Phase 6.1 extension.

## Architecture overview

### Data model

One new table, `ai_hub_task`:

```sql
CREATE TABLE ai_hub_task (
    id                BIGSERIAL PRIMARY KEY,
    workspace_id      BIGINT        NOT NULL,
    user_id           BIGINT        NOT NULL,
    thread_id         VARCHAR(255)  NOT NULL UNIQUE,   -- FK to SPRING_AI_CHAT_MEMORY.conversationId
    title             VARCHAR(255)  NULL,              -- NULL until auto-generated
    last_preview      TEXT          NULL,              -- last user message, truncated to 200 chars
    message_count     INTEGER       NOT NULL DEFAULT 0,
    status            VARCHAR(16)   NOT NULL,          -- ACTIVE | ARCHIVED | DELETED
    created_at        TIMESTAMP     NOT NULL,
    updated_at        TIMESTAMP     NOT NULL
);

CREATE INDEX idx_ai_hub_conversation_user_workspace_status
    ON ai_hub_task (workspace_id, user_id, status, updated_at DESC);
```

Messages themselves stay in the existing `SPRING_AI_CHAT_MEMORY` table (maintained by Spring AI). We never write to it directly; we only read for rehydration.

### Conversation lifecycle

1. **Create**: when the user clicks "New conversation" in the drawer (or on first page load with no active conversation), the client generates a new `conversationId` (existing `generateConversationId()` in `useCopilotStore`) and POSTs to the server to create the matching `ai_hub_task` row. The server uses the client's id verbatim as `thread_id`.

2. **Send a turn**: the existing AG-UI flow runs. Spring AI chat memory stores messages keyed by `conversationId`. After the client receives the assistant's response, it PATCHes the conversation row with `lastPreview = <first 200 chars of the user's last message>` and `messageCount += 2` (user + assistant).

3. **Auto-title**: after the conversation's `messageCount` reaches 6 (3 user turns), the client POSTs to `.../conversations/{id}/generate-title`. The server loads the first few messages from chat memory, calls the ChatModel with a short "produce a 4–8 word title" prompt, and stores the result. If generation fails, the conversation stays untitled (UI shows "Untitled conversation").

4. **Switch to historical**: user clicks a row in the conversation drawer. Client GETs `.../conversations/{id}/messages` which returns the serialized message history from chat memory. Client sets `conversationId = <selected thread_id>` and replaces `useCopilotStore.messages` with the fetched list. Next agent turn uses the same thread id, so Spring AI pulls the existing context.

5. **Rename**: simple PATCH. Drawer entry updates.

6. **Archive**: PATCH `status = ARCHIVED`. Excluded from the default list view; available under an "Archived" filter.

7. **Delete**: DELETE endpoint. Server cascades to `SPRING_AI_CHAT_MEMORY` (delete rows with matching `conversationId`) and deletes the metadata row. Hard delete in v6; a trash-with-TTL is a later polish.

### State diagram (conversation.status)

```
ACTIVE ──archive──> ARCHIVED ──unarchive──> ACTIVE
ACTIVE ──delete──> (gone)
ARCHIVED ──delete──> (gone)
```

`DELETED` is reserved for a future soft-delete; v6 skips the transition and hard-deletes. Keeping the enum value avoids migration churn later.

### Interaction with pending mutations (Phase 5A)

When the user clicks a different conversation with pending staged mutations, show a confirm dialog:

> You have N unapplied changes from the current conversation.
> **[Apply all and switch]** / **[Discard and switch]** / **[Cancel]**

Apply-all sequentially applies the pending bucket (same as the "Apply all" button's existing behavior), then switches. Discard-and-switch clears the pending store and switches. Cancel does nothing. The pending store is conversation-independent otherwise — refreshing the page loses any unapplied state, same as today.

## Server-side design

### New files

- `server/ee/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ee/ai/copilot/conversation/ConversationStatus.java` — enum.
- `server/ee/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ee/ai/copilot/conversation/Conversation.java` — domain record.
- `server/ee/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ee/ai/copilot/conversation/ConversationService.java` — interface.
- `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/conversation/ConversationServiceImpl.java` — implementation.
- `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/conversation/repository/ConversationRepository.java` — Spring Data JDBC.
- `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/conversation/TitleGenerationService.java` — small service that calls `ChatModel` for title.
- `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/config/liquibase/changelog/ai/copilot/<timestamp>_ai_copilot_conversation_init.xml` — Liquibase changeset.
- `server/ee/libs/ai/ai-copilot/ai-copilot-rest/src/main/java/com/bytechef/ee/ai/copilot/web/rest/ConversationApiController.java` — REST endpoints.

### REST endpoints

All under `/api/platform/internal/ai-hub/conversations`, user + workspace scoped via `UserService.getCurrentUser()` + a `workspaceId` query param (same pattern as `MutationApiController`).

- `POST /conversations` — body `{threadId: string, workspaceId: long}` → creates ACTIVE row with empty title. Returns the row. Idempotent: if the row exists (same thread_id + user + workspace), returns it unchanged.
- `GET  /conversations?workspaceId=N&status=ACTIVE` — list, newest-first, cap at 100.
- `GET  /conversations/{id}/messages?workspaceId=N` — reconstruct from Spring AI chat memory. Returns `[{role, content, createdAt?}]`.
- `PATCH /conversations/{id}?workspaceId=N` — body `{title?, lastPreview?, messageCount?, status?}`. Partial update, ownership-checked.
- `POST /conversations/{id}/generate-title?workspaceId=N` — loads first ~6 messages, calls ChatModel, stores title, returns the row.
- `DELETE /conversations/{id}?workspaceId=N` — hard delete + cascade to chat memory.

All endpoints return 403 if the conversation's `userId` != current user or `workspaceId` != request workspace.

### Title generation prompt

A constant string; no new prompt file (keeps it simple):

```
Given the following short exchange, produce a concise 4-8 word title
that describes the topic. Return only the title, no quotes, no trailing
punctuation.

<paste first 3 user messages + first 2 assistant messages, truncated>
```

### Chat memory integration

Spring AI's `JdbcChatMemoryRepository` already writes to `SPRING_AI_CHAT_MEMORY`. We query it directly (via `ConversationService`'s own `JdbcTemplate`) to reconstruct message history. We do NOT depend on Spring AI's `ChatMemory` interface for reads — that's scoped to agent sessions, not browsing.

The query: `SELECT role, content, timestamp FROM SPRING_AI_CHAT_MEMORY WHERE conversation_id = ? ORDER BY timestamp ASC`. Column names may differ in the managed schema; verify during implementation.

### Delete cascade

Server-side on DELETE: within the same transaction, delete from `SPRING_AI_CHAT_MEMORY WHERE conversation_id = ?`, then delete from `ai_hub_task WHERE id = ?`. Relies on Spring AI's schema which does not enforce FK constraints on its own; we clean up manually.

## Client-side design

### New files

- `client/src/pages/automation/ai-hub/conversations/stores/useAiHubTasksStore.ts` — zustand store: current conversation, list, activeFilter (ACTIVE|ARCHIVED), loading state.
- `client/src/pages/automation/ai-hub/conversations/api/conversations.api.ts` — 6 fetch wrappers.
- `client/src/pages/automation/ai-hub/conversations/hooks/useConversations.ts` — React Query hooks (list + mutations for create/rename/archive/delete/generate-title).
- `client/src/pages/automation/ai-hub/conversations/AiHubTasksDrawer.tsx` — left-side drawer component.
- `client/src/pages/automation/ai-hub/conversations/AiHubTaskItem.tsx` — row item with title, last preview, updatedAt, hover-menu (rename / archive / delete).
- Matching test files for store + drawer.

### Modified files

- `AiHubPanel.tsx` — add "New conversation" button in the header; add a "Conversations" button that toggles the drawer; hydrate current conversation on mount (if conversation id in the store has no matching server row, POST to create).
- `AiHubRuntimeProvider.tsx` — after each successful `onNew`, PATCH the current conversation with `lastPreview` + `messageCount`. After `messageCount` crosses the threshold, fire `generate-title` (fire-and-forget; Drawer refreshes via React Query invalidation).
- `AiHub.tsx` — host the drawer as a collapsible sibling of the chat panel (probably a third pane in the `ResizablePanelGroup` or an overlay sheet).

### Drawer UX

- Width: ~280px, collapsible via a chevron button.
- Header: search input (title filter) + filter tabs (Active | Archived) + "New conversation" button.
- Body: list of rows — title (or "Untitled" if not generated yet), last preview (truncated), updatedAt (relative: "2h ago", "Yesterday", "Apr 10").
- Row click: if different from current, show pending-mutations confirm if any are staged, then switch.
- Row hover: three-dot menu → Rename / Archive (or Unarchive) / Delete.
- Empty state: "No conversations yet. Start a new chat to see it here."

### Pending-mutations interaction

When switching conversations while the pending-mutations store is non-empty, render a small modal asking Apply-all / Discard / Cancel. Reuse the existing `applyPendingMutation` wrapper for Apply-all.

## State contract additions

No AG-UI state changes. The `conversationId` in `useCopilotStore` already drives everything; Phase 6 adds persistence around that id.

## Testing

### Server

- `ConversationServiceImplTest` — CRUD happy paths + ownership errors.
- `ConversationRepositoryIntTest` — Testcontainer Postgres, smoke.
- `ConversationApiControllerTest` — `@WebMvcTest`-style, covers 6 endpoints + 403 on ownership mismatch.
- `TitleGenerationServiceTest` — mocks ChatModel; asserts the prompt contains "title" and the response is used verbatim; truncates if >60 chars.

### Client

- `useAiHubTasksStore` — 4-5 cases.
- `useConversations` hooks — mocked fetch; each hook fires the expected path + body.
- `AiHubTaskItem` — renders title, preview, updated-at; hover menu fires the right actions.
- `AiHubTasksDrawer` — renders list, search filters, new-conversation button triggers create.
- `AiHubRuntimeProvider` — after `onNew`, PATCH is called once with the expected body; after crossing threshold, generate-title is fired.

## Risks and open questions

- **Spring AI memory schema stability**. We query `SPRING_AI_CHAT_MEMORY` directly. The column names + types are stable in Spring AI 1.0.0+ but could shift between major versions. Mitigation: isolate the query in `ConversationServiceImpl.loadMessages(...)` so a future migration is one-place.
- **Title generation cost**. One LLM call per conversation (when crossing 6 messages). Cheap. Could be deferred to a background worker if it's ever an issue.
- **Orphaned chat memory rows**. If a conversation was created under the default `inMemoryChatMemory` provider, there's no persisted data to rehydrate — the drawer will show the row but the message fetch returns empty. UX: show an "empty (memory not persisted)" label for such conversations. In production EE, JDBC memory is the recommended config, so this is a corner case.
- **Pending-mutations edge cases**. Switching while a staged mutation's `applyPendingMutation` is in-flight. Simple guard: disable the drawer's row clicks while `isApplying`.
- **Concurrent turns**. If two AI Hub tabs are open for the same conversation, `messageCount` can race. Accept last-writer-wins — the PATCH is idempotent enough.

## Phase 6+ preview

- **Conversation-scoped artifact linkage** (Phase 6.1): track which files / workflows / mutations originated in which conversation. Shown in the conversation row as a small "3 files, 2 workflow edits" badge.
- **Full-text search** over message contents.
- **Export conversation** as markdown.
- **Shareable conversations** across users in the same workspace (read-only links).

## Commit convention

`CC6 …` / `CC6 client - …`.
