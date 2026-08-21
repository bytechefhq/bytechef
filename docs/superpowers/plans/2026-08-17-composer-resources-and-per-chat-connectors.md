# Plan — Composer resources consolidation & per-chat connector participation

Status: **both phases complete.** See the spec's As-built notes for the two places reality differed
from the design (the skills popover is text-derived, and the per-chat override needed explicit
subtraction in the resolver rather than falling out of the schema).

Spec: `docs/superpowers/specs/2026-08-17-composer-resources-and-per-chat-connectors-design.md`

Phase A (tasks 1–4) is item 1 and is purely client-side and structural. Phase B (tasks 5–7) is item 2
and touches GraphQL + the server. Phase A lands first and is independently shippable.

---

## Phase A — one Resources menu

### Task 1 — Generalize the picker's caller-supplied branch

`client/src/ee/pages/automation/ai-hub/resource-picker/ResourcePickerMenu.tsx`

- Replace `ResourcePickerToolsBranchI` with `ResourcePickerCustomBranchI` (adds `key: string`).
- Replace prop `toolsBranch?: ResourcePickerToolsBranchI` with
  `customBranches?: ResourcePickerCustomBranchI[]`.
- `MenuPathType`: `['tools']` → `['custom', string]`.
- Root menu: render each branch's `renderRootItem(() => setMenuPath(['custom', branch.key]))` in array
  order, at the existing insertion point (after Knowledge Bases, before Workflow Executions).
- Branch body: `menuPath[0] === 'custom'` → look the key up in `customBranches` and call
  `renderBranch(back, close)`. An unknown key renders nothing rather than throwing — the path can only
  be set from a branch that existed at click time, but a branch list that changes mid-open must not
  crash the menu.

Verify: `ResourcePickerMenu.test.tsx` — rewrite the two `toolsBranch` cases for `customBranches`, and
add one asserting two branches both render and each drills into its own body.

### Task 2 — Extract the connectors branch

- New `composer/AiHubConnectorsBranch.tsx`: the body of today's `AiHubConnectorsMenu` popover (list,
  per-connector `Switch`, "Manage connectors" link) rendered as command-menu content with a Back row.
  Props: `onBack`, `onClose`. The "Manage connectors" link calls `onClose`.
- The query loses its `open` gate: the branch only mounts when entered, which gates it more tightly
  than the old popover-open flag did.
- Delete `AiHubConnectorsMenu.tsx`.

### Task 3 — Extract the skills branch, keep the slash path

`AiHubSkillsMenu` is **not** a button-driven menu: its open state is derived from the composer text
matching `SLASH_PATTERN`, and its popover anchors to the textarea via a virtual ref. Typing `/` must
keep working.

- `AiHubSkillsMenu.tsx`: drop the `<button>` + `Tooltip` wrapper only. It keeps the `Popover`,
  `PopoverAnchor virtualRef`, content, and every keyboard effect. It renders nothing visible until `/`
  is typed. Its `handleButtonClick` goes away with the button.
- New `composer/AiHubSkillsBranch.tsx`: lists `useAiSkillsQuery` skills, calls
  `aiHubComposerStore.addSkill` on pick, then `onClose`. Props: `onBack`, `onClose`.

Verify: `AiHubSkillsMenu.test.tsx` — the button-click case is deleted (button is gone); slash-path
cases must still pass untouched.

### Task 4 — Wire and remove the standalone buttons

- `AiHubComposer.tsx`: build `customBranches` = `[connectors, skills]` and pass it. Replace the stale
  header comment (it currently says Tools "moved out to the dedicated connectors button").
- `AiHubChatComposer.tsx`: remove `<AiHubConnectorsMenu />` (line ~430) and its import. Keep
  `<AiHubSkillsMenu />` — now invisible chrome for the slash path. Keep the paperclip.
- `AiHubChatComposer.test.tsx`: drop the `AiHubConnectorsMenu` `vi.mock`.

Verify: `cd client && npm run check`.

---

## Phase B — per-chat connector participation

### Task 5 — Server: chat-scoped enable mutation

`ai_hub_chat_component` already carries nullable `chat_id` and `enabled` on both scopes — no
migration. Add `setAiHubChatConnectorEnabled(workspaceId, chatId, connectorId, enabled)` upserting the
chat-scoped row, alongside the existing user-global `setAiHubUserConnectorEnabled`. Ownership is
checked on the `ai_hub_chat` row, as every other chat-scoped write is.

### Task 6 — Server: intersect the two scopes at tool assembly

A connector contributes tools to a turn only when its user-global row is enabled **and** its
chat-scoped row is absent-or-enabled. Every read must state which scope it means; a query missing
`chat_id IS NULL` silently mixes them.

### Task 7 — Client: branch toggle writes per-chat

- `aiHubUserConnectors` (or a chat-aware sibling) returns both flags so the branch can render the
  chat-scoped state.
- Connectors disabled user-globally are filtered out of the branch entirely.
- The branch's `Switch` calls the new mutation with the current `chatId`; on the home composer (no
  chat yet) the toggle is absent and the list is read-only availability.

---

## Verification

Phase A: `cd client && npm run check`.
Phase B: the above plus `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test` and the ai-hub
GraphQL module's tests.
