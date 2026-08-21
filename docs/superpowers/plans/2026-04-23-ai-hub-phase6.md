# AI Hub Phase 6 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans.

**Goal:** Durable, listable conversations. Users can reopen past chats, rename, archive, delete.

**Architecture:** New `ai_hub_task` metadata table; messages stay in Spring AI's `SPRING_AI_CHAT_MEMORY`. Thin service + 6 REST endpoints. Left-side conversation drawer. Auto-title after 6 messages.

**Reference spec:** [docs/superpowers/specs/2026-04-23-ai-hub-phase6-design.md](../specs/2026-04-23-ai-hub-phase6-design.md).

**Depends on:** All prior phases merged.

---

## File structure (summary)

### Server
- `conversation/ConversationStatus.java` + `Conversation.java` (records/enums in api)
- `conversation/ConversationService.java` (interface) + `ConversationServiceImpl.java` + `TitleGenerationService.java`
- `conversation/repository/ConversationRepository.java`
- Liquibase changeset
- `web/rest/ConversationApiController.java`

### Client

**Direction change (post-spec):** use ByteChef's standard `LayoutContainer` + `LeftSidebarNav` + `LeftSidebarNavItem` pattern (same as `client/src/pages/automation/chats/Chats.tsx`) rather than a floating drawer. The AI Hub page wraps the existing two-pane chat + resource panel as the main content, with the conversations list as the left sidebar body.

- `conversations/stores/useAiHubTasksStore.ts`
- `conversations/api/conversations.api.ts`
- `conversations/hooks/useConversations.ts` (React Query)
- `conversations/AiHubTasksSidebar.tsx` — uses `LeftSidebarNav` + `LeftSidebarNavItem`, mirrors `ChatsSidebar.tsx` structure; the sidebar is always visible on the route (no show/hide toggle)
- `conversations/AiHubTaskSwitchDialog.tsx` (pending-mutations confirm)
- Modify: `AiHub.tsx` — wrap existing two-pane content in `<LayoutContainer leftSidebarBody={<AiHubTasksSidebar />} leftSidebarHeader={<Header position="sidebar" title="Conversations" />} leftSidebarWidth="64">`; `header` gets a "New conversation" button.
- Modify: `AiHubRuntimeProvider.tsx` for per-turn patch + auto-title.

Rename (or delete) per-item actions moved from the hover-menu design into a row `onClick` for select + a kebab/dropdown for rename/archive/delete — matches `LeftSidebarNavItem` affordances.

### Commit convention
`CC6 …` / `CC6 client - …`.

---

## Task list

### Task 1: Conversation table + domain + repository

- Liquibase changeset: new table + index per spec.
- `ConversationStatus` enum (ACTIVE / ARCHIVED / DELETED).
- `Conversation` record with Spring Data JDBC `@Table` + `@Id`.
- `ConversationRepository extends CrudRepository` with derived finders:
  - `findByWorkspaceIdAndUserIdAndStatusOrderByUpdatedAtDesc(long workspaceId, long userId, ConversationStatus status, Limit limit);`
  - `findByThreadIdAndUserId(String threadId, long userId);`
- Integration test: save + find round-trip (mirror Phase 5A Task 1's repo test approach — may fall back to unit test with mock if Testcontainer setup is heavy).

Commit: `CC6 Add ai_hub_task table + repository`

### Task 2: ConversationService

- `ConversationService` interface + `ConversationServiceImpl`:
  - `create(workspaceId, userId, threadId)` — idempotent; if row exists, returns it.
  - `list(workspaceId, userId, status)` — newest-first, cap at 100.
  - `loadMessages(conversationId, requesterUserId)` — ownership check, then query `SPRING_AI_CHAT_MEMORY` directly via `JdbcTemplate`: `SELECT role, content, timestamp FROM SPRING_AI_CHAT_MEMORY WHERE conversation_id = ? ORDER BY timestamp ASC`. **Inspect the real schema first**: `grep -rn "SPRING_AI_CHAT_MEMORY\|spring-ai-chat-memory" server/ 2>&1 | head -20`. Column names may be different in Spring AI 1.1+ — adjust.
  - `patch(conversationId, requesterUserId, patch)` — partial update (title, lastPreview, messageCount, status). Ownership check.
  - `delete(conversationId, requesterUserId)` — deletes chat-memory rows then the conversation row, same transaction.
- Unit test: mocked repository + JdbcTemplate, 6-8 tests.

Commit: `CC6 Add ConversationService for metadata CRUD + message rehydration`

### Task 3: TitleGenerationService

- `TitleGenerationService` with single method `generateTitle(List<Message> messages): String`.
- Takes the first 3 user + 2 assistant messages, truncates each at 500 chars, builds a one-shot prompt, calls `ChatModel.call(prompt)`. Returns the trimmed response. Empty / null / > 60 chars → fallback to empty string (caller decides to skip).
- Unit test: mock ChatModel, assert prompt format + response handling.

Commit: `CC6 Add TitleGenerationService`

### Task 4: REST controller

- `ConversationApiController` with 6 endpoints per spec.
- Auth + workspace resolution: `UserService.getCurrentUser()` for userId; `workspaceId` as query param (matches `MutationApiController`).
- Ownership check before every mutating / reading op: 403 on mismatch.
- `generate-title` endpoint: loads messages via service, calls `TitleGenerationService`, PATCHes the row, returns the row.
- Tests: 6+ cases covering each endpoint's happy path + 403 cases.

Commit: `CC6 Add ConversationApiController with 6 REST endpoints`

### Task 5: Client conversations store

- `useAiHubTasksStore.ts` zustand store:
  - `activeFilter: 'ACTIVE' | 'ARCHIVED'`
  - `currentConversationId: number | undefined`
  - `setActiveFilter`, `setCurrentConversationId`
- Tests: 3 cases.

Commit: `CC6 client - Add conversations store`

### Task 6: Client API + React Query hooks

- `conversations.api.ts` — 6 fetch wrappers matching the REST endpoints.
- `useConversations.ts`:
  - `useConversationsQuery(workspaceId, status)` — React Query with 30s stale time.
  - `useCreateConversation` / `useRenameConversation` / `useArchiveConversation` / `useDeleteConversation` / `useGenerateTitle` — mutations that invalidate the list query on success.
  - `useConversationMessagesQuery(conversationId)` — used for rehydration on switch.

Commit: `CC6 client - Add conversations API wrappers + React Query hooks`

### Task 7: ConversationItem component

- `AiHubTaskItem.tsx`: title (or "Untitled"), preview, relative-time timestamp, hover three-dot menu with Rename / Archive (or Unarchive) / Delete.
- Rename opens an inline input.
- Tests: renders, hover menu, rename flow, archive flow, delete confirm.

Commit: `CC6 client - Add ConversationItem with hover menu`

### Task 8: Conversations drawer

- `AiHubTasksDrawer.tsx`: header (search input + Active/Archived tabs + New button), body (list of `AiHubTaskItem`s filtered by search + status), empty state.
- Search is title-only, client-side filter on the current list.
- "New" button: create a new conversation row (server POST with a fresh `generateConversationId()`), set it as current, reset `useCopilotStore.messages`.
- Tests: renders list, filters by search, tab switch fetches different status, new-conversation triggers create.

Commit: `CC6 client - Add ConversationsDrawer`

### Task 9: Conversation-switch confirm dialog

- `AiHubTaskSwitchDialog.tsx`: small dialog rendered when the drawer attempts to switch with non-empty pending-mutations store. Three buttons: Apply all / Discard / Cancel.
- Apply all: calls `applyPendingMutation` for each (reuse Phase 5A pattern); on success, switch.
- Discard: `clearAll()` + switch.
- Cancel: close.
- Tests: 3 flows.

Commit: `CC6 client - Add conversation-switch confirm dialog for pending mutations`

### Task 10: Wire drawer into AiHub page + panel

- `AiHub.tsx`: host the drawer as a collapsible sibling (extend `ResizablePanelGroup` or render as an overlay sheet — overlay is simpler for v6).
- `AiHubPanel.tsx`: new "Conversations" button + "New conversation" button in the header (next to existing Clean Messages).
- Default state: drawer closed; opening it fires the conversations query.
- Hydrate: on mount, if `currentConversationId` in the store doesn't have a matching server row, POST to create (makes the existing in-memory conversationId server-side visible).

Commit: `CC6 client - Wire ConversationsDrawer into AiHub layout`

### Task 11: Runtime-provider telemetry (patch per turn, generate title at threshold)

- In `AiHubRuntimeProvider`'s `onNew`:
  - After `await agent.runAgent(...)` resolves, PATCH the current conversation with `lastPreview = <user message, first 200 chars>` and `messageCount += 2`.
  - If the post-patch `messageCount === 6` (threshold), fire `generateTitle` (fire-and-forget). On success, invalidate the conversations list so the drawer refreshes.
- Tests: extended `AiHubRuntimeProvider.test.tsx` — verify PATCH called; verify generate-title fires at threshold.

Commit: `CC6 client - Patch conversation metadata per turn + auto-title at threshold`

### Task 12: Conversation switch — rehydrate messages

- When a user clicks a row in the drawer:
  - If pending mutations exist, show the confirm dialog (Task 9) first.
  - Otherwise: GET `/conversations/{id}/messages`, set `useCopilotStore.messages` to the fetched list, set `useCopilotStore.conversationId = selected thread id`.
  - Close the drawer (mobile) / keep open (desktop).
- Tests: switch flow, rehydration sets messages correctly.

Commit: `CC6 client - Rehydrate messages on conversation switch`

### Task 13: Delete / archive hover actions

Already sketched in Task 7. Task 13 is the integration test: open drawer, archive active conversation → moves to Archived filter; delete from Archived → row is gone. This can be a single `AiHubTasksDrawer` integration test at the end of Task 8 if inline is simpler — or a separate vitest module.

Commit: `CC6 client - Drawer archive / delete integration tests`

### Task 14: Full check + manual verification

- Server tests green across all copilot modules.
- Client `npm run check` green.
- Manual:
  - Open AI Hub, send a message → conversation appears in drawer.
  - Reload page → conversation list persists; current conversation is hydratable.
  - Send 3 turns → title auto-generates and appears after the 3rd.
  - Rename, archive (moves to Archived tab), unarchive, delete (gone).
  - Stage a mutation, try switching → confirm dialog appears.

Final commit if fixups: `CC6 Final formatting + lint fixes`.

---

## Risks to watch

- **Spring AI schema drift**: if the `SPRING_AI_CHAT_MEMORY` columns aren't what we expect, Task 2's `loadMessages` fails. Do a `DESCRIBE`/`\d` on a running dev Postgres in Task 2 before coding.
- **Conversations listed but memory not persisted**: when `bytechef.ai.copilot.memory.provider=in_memory`, empty-message rehydration is expected. Show "(messages not persisted — enable JDBC memory to retain history)" inline in the drawer row.
- **Drawer state + URL**: v6 does not add deep-linkable URLs like `/automation/ai-hub/c/<id>`. Nice-to-have; Phase 6.1.

## Out of scope (deferred)

- **Full-text search** across messages.
- **Sharing** conversations across users.
- **Export as markdown**.
- **Conversation-scoped artifact linkage** (which file was created in which conversation).
- **Deep-linking**.
