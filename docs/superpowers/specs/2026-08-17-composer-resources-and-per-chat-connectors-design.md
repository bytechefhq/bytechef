# Composer resources consolidation & per-chat connector participation

Status: draft (design). Covers queued items 1 and 2. Item 3 (double-rendered AskUserQuestion card)
is a bug and is tracked separately — it needs debugging, not design.

## Problem

The AI Hub composer toolbar has four standalone affordances: `+` (resources), plug (connectors),
paperclip (attach), `/` (skills). Three of them are pickers that differ only in what they list, so
the toolbar reads as four coordinate concepts when it is really "attach a file" plus "reference
something in the workspace".

Separately, the connectors popup's per-connector toggle writes
`setAiHubUserConnectorEnabled`, which is **global for the user**. A user who wants a connector
available in general but silent in one conversation has no way to express that, and turning it off
mid-chat silently changes every other chat.

## Design

### 1. One Resources menu

`ResourcePickerMenu` already carries the seam this needs: `toolsBranch`
(`ResourcePickerToolsBranchI` — `renderRootItem(onEnter)` + `renderBranch(onBack, onClose)`), with
`MenuPathType` tracking a `['tools']` path and delegating rendering to the caller. **No current
caller passes it** — it is a dead extension point left from when Tools moved *out* of the `+` menu
into the connectors button (see the comment at the top of `AiHubComposer.tsx`). This change moves
Connectors and Skills *in*, which is the same seam used as intended.

Generalize the single branch into a list:

```ts
export interface ResourcePickerCustomBranchI {
    key: string;                                            // menu-path segment, e.g. 'connectors'
    renderRootItem: (onEnter: () => void) => ReactNode;
    renderBranch: (onBack: () => void, onClose: () => void) => ReactNode;
}
// props: customBranches?: ResourcePickerCustomBranchI[]
```

`MenuPathType` gains `['custom', string]` in place of `['tools']`. Branch root items render at the
existing `toolsBranch` insertion point (between Knowledge Bases and Workflow Executions), in array
order. Rationale for a list over two more bespoke props (`connectorsBranch`, `skillsBranch`): the
three call sites inside the menu (root item, path match, branch body) would otherwise be duplicated
per branch, and a fourth branch later would duplicate them again.

`AiHubConnectorsMenu` and `AiHubSkillsMenu` lose their `Popover`/`PopoverTrigger`/`Tooltip` wrappers
and become branch **bodies** (`AiHubConnectorsBranch`, `AiHubSkillsBranch`) rendered inside the
picker's command list with a back row. Their data hooks, toggles and "Manage …" links carry over
unchanged. `AiHubChatComposer` drops both standalone buttons; the paperclip stays (it is an upload,
not a reference).

Both composers get this for free: `AiHubComposer` (the `+`) is rendered by `AiHubChatComposer`, and
the home composer renders the same component.

### 2. Per-chat connector participation

**No new table.** `ai_hub_chat_component` already models both scopes in one table: `chat_id` is
nullable, NULL meaning a user-global "added connector" and non-null meaning a chat-scoped binding
(migration `20260701000001_ai_hub_chat_component_user_scope.xml`), and `enabled` is
`BOOLEAN NOT NULL DEFAULT true` on **both** scopes. Uniqueness is already enforced per scope —
`uk_ccc_chat_comp_conn_env` for chat-scoped rows, a Postgres partial unique index for user-global
ones.

A chat-scoped row's `enabled` flag therefore carries the per-chat decision, independently of which
individual tools are attached (those live in `ai_hub_chat_tool`).

**Correction (as-built).** The first draft of this section claimed the override then works for free.
It does not. A chat-scoped row and a user-global row are **independent rows**, and
`AiHubChatBindingToolCallbackResolver` *unions* them. Disabling the chat-scoped row therefore removes
only its own tools from `listChatTools`; the user-global connector keeps streaming its tools in
through `listUserTools`, so the toggle would appear to work and change nothing the agent sees. The
subtraction has to be written explicitly — `listChatDisabledComponents(chatId)`, applied to the
user-global bindings in the resolver — and is regression-pinned by
`testResolveDropsUserConnectorsTheChatSwitchedOff`.

Reuse still avoids a migration and a parallel entity, and the resulting reading of a chat-scoped row
("this component is off in this chat") covers the explicitly-attached case too. But it is a code
change, not a free consequence of the existing schema.

Resulting split:

| Surface | Writes | Meaning |
|---|---|---|
| Connectors page | `setAiHubUserConnectorEnabled` (user-global row) | **Availability** — is this connector usable at all |
| Composer branch toggle | new `setAiHubChatConnectorEnabled(chatId, …)` (chat-scoped row) | **Participation** — does it act in *this* chat |

Rules:
- A connector disabled on the Connectors page does not appear in the composer branch at all.
- Absence of a chat-scoped row means "participating" (default true), so existing chats are
  unaffected and no backfill is needed.
- The composer toggle upserts the chat-scoped row; it never touches the user-global row.

Runtime tool assembly must intersect the two: a connector contributes tools to a turn only when its
user-global row is enabled **and** its chat-scoped row is absent-or-enabled.

## Deliberately out of scope

- The home composer has no chat yet, so its connector branch shows availability only; the per-chat
  toggle appears once a chat exists. (Alternative — provisionally stash toggles and apply them to
  the chat created by the first turn — is more state for a case the user can fix in one click.)
- Skills gain no per-chat concept in this change.

## Risks

- **Two `enabled` flags on one table** is easy to misread. Every read path must say which scope it
  means; a query that forgets `chat_id IS NULL` silently mixes them. The existing partial unique
  index is the guard against duplicate rows, not against a careless read.
- Extracting the two menu bodies loses their popover-local state (`open`-gated lazy queries). The
  branch bodies must gate on the picker's own open state instead, or they will fetch on every
  composer render.

## ⚑ Assumptions to confirm

1. Reusing `ai_hub_chat_component` rather than adding `ai_hub_chat_connector` (the option originally
   selected). No migration, but see the correction above — it costs explicit resolver logic, and it
   overloads a chat-scoped row so that toggling participation for a component the user had also
   attached to the chat directly turns that attachment off too. Still the smaller change; say the
   word and it becomes a dedicated table.
2. Branch order in the root menu: Connectors and Skills placed at the old Tools insertion point
   rather than at the end of the list.

## As-built notes

- Skills could not simply move: `AiHubSkillsMenu`'s popover open state is derived from the composer
  text matching a leading `/`, not from its button. The button was removed and the popover kept, so
  the keyboard path is untouched; the pointer path is a new `AiHubSkillsBranch` writing the same
  store.
- `aiHubUserConnectors` gained an optional `chatId` **which is the AG-UI threadId string**, matching
  the sibling `aiHubChatTools` query — not the numeric chat row id. An absent, not-yet-persisted, or
  foreign thread all resolve to "no chat", so the new `enabledInChat` field degrades to its default
  rather than failing the query or distinguishing the three cases to a prober.
- The home composer shows the connector list without switches, per "deliberately out of scope" above.
