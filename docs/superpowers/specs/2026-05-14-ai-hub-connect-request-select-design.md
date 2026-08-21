# AI Hub Connect-Request: Render ConnectionSelect when connections already exist

**Status**: Draft (2026-05-14)
**Owner**: Ivica Cardic
**Scope**: client-only (EE-facing UI in `pages/automation/ai-hub`)

---

## 1. Background

When the AI Hub LLM agent calls the `requestConnection` tool, the server returns a
signaling-only payload (`RequestConnectionToolCallback`):

```json
{
  "kind": "request-connection",
  "componentName": "slack",
  "componentLabel": "Slack",
  "suggestedName": null
}
```

The client renders this inline in the chat thread through
`AiHubConnectRequestMessage` (registered as the `connect-request` data part in
`AiHubMessageContent.tsx`). Today the renderer shows exactly one UI affordance:
a `Connect <ComponentLabel>` button that opens `ConnectionDialog` pre-selected
on the requested component.

This is the right UX **when the user has zero connections for that component**.
When the user already has one or more matching connections (private,
workspace-shared, project-shared, organization), forcing them through the full
creation dialog is wasted work — and the chat thread is the wrong place to
hide existing infrastructure.

## 2. Problem statement

`AiHubConnectRequestMessage` ignores the user's existing connections for the
target component. The renderer should match the affordance already used in the
workflow editor (`ConnectionTabConnectionSelect`), where:

- **0 existing connections for `(componentName, connectionVersion)`** → a full-width
  "Create Connection" button.
- **≥ 1 existing connection** → a `Select` dropdown grouped by visibility
  (Private / Project / Workspace / Organization) **plus** an icon-sized `+` button
  that opens `ConnectionDialog`.

## 3. Goals (in scope)

1. Inside `AiHubConnectRequestMessage`, fetch the user's existing connections
   filtered by `(componentName, connectionDefinition.version)` for the current
   environment.
2. Render a `ConnectionSelect`-style affordance when at least one connection
   exists; render the existing "Create Connection" button when zero exist.
3. Group the dropdown items by visibility (Private → Project → Workspace →
   Organization), matching `ConnectionTabConnectionSelect`'s ordering. Each item
   shows the connection name, `EnvironmentBadge`, `ConnectionScopeBadge`, and tag
   labels — same row composition as the workflow editor.
4. When the user picks an existing connection **or** finishes creating a new one,
   show a small inline confirmation in the message bubble:
   `✓ Connection ready: <connection name>`. The pick replaces the
   select+button row.
5. Keep the existing loading and error branches (`componentDefinitionsLoading`,
   `componentDefinitionsIsError`) wired the same way.
6. Test coverage: extend `tests/AiHubConnectRequestMessage.test.tsx` with cases
   for "no existing connections" (existing behavior), "existing connections shown
   in select", "select item triggers confirmation", "create flow triggers
   confirmation".

## 4. Non-goals (out of scope)

- **No agent-back signalling.** The chosen `connectionId` is **not** auto-posted as a
  follow-up message, and there is **no** new endpoint to tell the agent "user
  picked connection X". When the user resumes typing, the agent's next
  tool call will naturally see the new connection via existing component-listing
  paths. Auto-signal is deferred to a follow-up spec that lands alongside the
  autonomous tool-attach work (see §10).
- **No new reusable `ConnectionSelect` component.** The workflow editor's
  `ConnectionTabConnectionSelect` is tightly coupled with workflow-test-config
  mutations, store hooks, and skip-server-sync refs. Extracting a shared component
  would force premature abstraction. We build a focused inline implementation
  in the AI Hub feature folder and leave extraction as a future cleanup if a
  third caller appears.
- **No backend changes.** `RequestConnectionToolCallback` already returns enough
  data (`componentName`, `componentLabel`). No new payload field, no new tool
  callback.
- **No autonomous tool/component attach by the agent.** That is a separate
  feature (see §10 "Future work").
- **No clear-connection (`X`) button** in the chat affordance. The chat picker
  is one-shot: pick → confirm. The workflow editor needs `X` because its
  selection persists to a workflow's test config; the chat picker has no such
  persistence.

## 5. Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│ AiHubConnectRequestMessage  (existing file, rewritten body)           │
│ ───────────────────────────                                           │
│  componentDefinitions  ← useGetComponentDefinitionsQuery              │
│  targetComponentDef    ← useGetComponentDefinitionQuery               │
│  connectionDefinition  ← useGetConnectionDefinitionQuery   ← NEW      │
│  existingConnections   ← useGetWorkspaceConnectionsQuery   ← NEW      │
│                                                                      │
│  state: chosenConnection?                                            │
│         dialogOpen                                                   │
│                                                                      │
│  render branches:                                                    │
│    ① loading / error          → spinner | error (same as today)      │
│    ② chosenConnection set     → "✓ Connection ready: <name>"         │
│    ③ existingConnections=0    → "Create Connection" button (today)   │
│    ④ existingConnections>0    → <Select> + <+ button>                │
└──────────────────────────────────────────────────────────────────────┘
              │
              ▼
      ConnectionDialog (existing, unchanged)
              │
              ▼
      onConnectionCreate(newId)
              │
              ▼
      setChosenConnection({id, name})  +  invalidate query
```

### Hook composition

```ts
const {data: connectionDefinition} = useGetConnectionDefinitionQuery(
    {componentName: data.componentName, componentVersion: 1},
    Boolean(data.componentName)
);

const {data: existingConnections} = useGetWorkspaceConnectionsQuery(
    {
        componentName: data.componentName,
        connectionVersion: connectionDefinition?.version,
    },
    Boolean(connectionDefinition?.version)
);
```

The `componentVersion: 1` constant is **deliberate** — see the existing inline
comment at `AiHubConnectRequestMessage.tsx:40-46`. Connections are versioned
independently from components, and the platform's
`GetComponentDefinitionRequest` treats `1` as "latest" via the connection-def
lookup. Using `1` here is consistent with the existing behavior in this file.

### State

Two pieces of local state:

```ts
const [dialogOpen, setDialogOpen] = useState(false);
const [chosenConnection, setChosenConnection] = useState<
    {id: number; name: string} | undefined
>(undefined);
```

`chosenConnection` is the post-pick / post-create state. Once set, the select
and button row collapse into a single-line "✓ Connection ready: …" affordance.
The state is **per-message-instance** and **not persisted** — refreshing the
chat thread renders the affordance from scratch (which will correctly show the
new connection in the select).

### Visibility grouping

Same `visibilityOrder` and `groupLabels` constants as
`ConnectionTabConnectionSelect.tsx:264-277`. Implemented inline (the table is
8 lines; a shared util is overkill).

### "+ button" vs "Create Connection" button

When connections exist:

```tsx
<Button icon={<PlusIcon />} onClick={() => setDialogOpen(true)}
        size="icon" title="Create a new connection" variant="outline" />
```

When zero connections (existing behavior, slightly relabeled for consistency
with the workflow editor):

```tsx
<Button className="w-full" icon={<PlusIcon />} label="Create Connection"
        onClick={() => setDialogOpen(true)} variant="outline" />
```

Note: the existing label is `Connect <ComponentLabel>`. We **keep that label**
in the zero-connections branch — it reads more naturally in the chat thread
("Connect Slack" vs "Create Connection") and is closer to what the LLM is
asking. Only the layout (full-width button) is shared with the workflow
editor.

### ConnectionDialog reuse

`ConnectionDialog` already accepts `onConnectionCreate?: (id: number) => void`.
We pass it in:

```tsx
onConnectionCreate={(newId) => {
    setChosenConnection({id: newId, name: /* fetched after refetch */});
    queryClient.invalidateQueries({queryKey: ConnectionKeys.connections});
}}
```

To resolve the new connection's `name`, we rely on the query invalidation +
re-fetch returning the connection in `existingConnections`. Simpler than
threading the dialog's success payload — the dialog already returns the new
`id`, and the next render's `existingConnections.find(c => c.id === newId)`
gives the name. If the refetch hasn't completed when render fires, the
confirmation falls back to "Connection ready" without the name. (Snapshot
documented in test §6.4.)

## 6. Test plan

Extend `client/src/pages/automation/ai-hub/connect/tests/AiHubConnectRequestMessage.test.tsx`:

1. **No existing connections renders Create Connection button** (existing test,
   may need label update if we keep `Connect <Component>` vs renaming).
2. **Existing connections render a Select with grouped items by visibility**:
   mock `useGetWorkspaceConnectionsQuery` to return three connections —
   PRIVATE, WORKSPACE, ORGANIZATION — verify the rendered group labels are in
   order and each item shows the connection name.
3. **Selecting an item flips to confirmation**: click a `SelectItem` for
   connection id `42` named "Slack Prod", verify
   `✓ Connection ready: Slack Prod` appears and the select disappears.
4. **+ button opens dialog; dialog onConnectionCreate flips to confirmation**:
   click `+`, dispatch the mock dialog's success callback with id `99`, verify
   confirmation appears.
5. **Error fetching component definitions still renders the existing error +
   Retry branch** (existing test).
6. **Loading state of `useGetWorkspaceConnectionsQuery`** does not crash the
   component (returns `null` like the existing `componentDefinitionsLoading`
   path).

Mock pattern follows the existing file: `vi.mock` the queries module, mock the
dialog, render via dynamic import. No new mock files.

## 7. Edge cases

| Case | Behavior |
|---|---|
| Connection definition fetch fails | Fall through to error branch (component definitions load failure already covers this; connection definition is a sub-query) — render "Could not load component definitions" + Retry. |
| `existingConnections` is undefined (loading) | Render `null` (matches `componentDefinitionsLoading`) — the chat shows nothing for a brief moment, same as today. |
| Connection was deleted between fetch and click | Select item's `value` falls back to `undefined`; user re-clicks `+` to create. No crash. |
| Component slug returns no `ComponentDefinition` at all (unknown component) | Existing fallback: button label falls back to slug, dialog still opens but cannot pre-select. **Unchanged.** |
| User picks then changes mind | Out of scope for V1. The chosen state is sticky for that message. To re-pick, user clicks the agent's "ask again" path. |
| Multiple `requestConnection` calls in one thread | Each message instance has its own `chosenConnection` state. No cross-message dedup. |

## 8. Edition / visibility

- This file is already AI Hub (EE). No edition gating needed beyond what
  exists.
- The `useGetWorkspaceConnectionsQuery` returns connections filtered by the
  current workspace + environment. The server enforces visibility — we render
  whatever the API returns, grouped by `visibility`.
- The `+` button is **always** shown when ≥ 1 connection exists. Unlike the
  workflow editor, we do **not** gate the `+` button on `connectionDialogAllowed`
  (a workflow-editor-specific flag governing the read-only project view). The
  chat is interactive by definition; if the user got here, they can create.

## 9. Self-review checklist

- [x] Placeholder scan: no TBD / TODO.
- [x] Internal consistency: render branches in §5 match the test plan in §6.
- [x] Scope: one component, one test file, no new shared abstractions.
- [x] Ambiguity: visibility ordering pinned; `componentVersion: 1` rationale
      documented; "no agent-back signal" called out in non-goals.

## 10. Future work (not this spec)

- **Autonomous tool/component attach by the agent.** Add three Spring AI
  `ToolCallback`s — `searchComponentsAndActions`, `attachTool`,
  `setToolConnection` — so the agent can wire up new tools mid-chat without
  the user opening the side panel. The connection picker built here is what
  surfaces in the chat when `attachTool` requires a connection.
- **Signal selection back to the agent.** When that lands, the chosen
  `connectionId` from this picker is auto-posted as a `data` part the agent
  can read (or a thread-scoped store the next tool call resolves) so the
  agent resumes without the user re-prompting.
- **Multi-select for `componentConnectionsCount > 1` actions.** Some actions
  require multiple connections (rare). The current `requestConnection`
  payload assumes one. If we ever need this in chat, the payload grows a
  `componentConnectionKey` field and the renderer loops.

---

## Appendix A — Files touched

| Path | Change |
|---|---|
| `client/src/pages/automation/ai-hub/connect/AiHubConnectRequestMessage.tsx` | Add hooks for connection definition + workspace connections; add `chosenConnection` state; conditional render branches. ~+90 lines. |
| `client/src/pages/automation/ai-hub/connect/tests/AiHubConnectRequestMessage.test.tsx` | Add mocks for `useGetConnectionDefinitionQuery` and `useGetWorkspaceConnectionsQuery`; add four new test cases. ~+150 lines. |

No new files. No backend changes.
