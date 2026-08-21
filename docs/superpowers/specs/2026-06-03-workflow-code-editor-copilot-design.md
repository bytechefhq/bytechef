# Copilot: Workflow Code Editor — Design

- **Date:** 2026-06-03
- **Status:** Approved (design)
- **Edition:** EE only
- **Author:** Ivica Cardic
- **Issue:** [#4076](https://github.com/bytechefhq/bytechef/issues/4076) (AI Copilot for the Workflow Code editor)
- **Feature flag:** `ff-4076` (already gates the existing button)

## Context

`WorkflowCodeEditorSheet` is the Monaco editor for the **whole workflow definition** (the YAML/JSON
that defines triggers, tasks, component `type` references, `parameters`, datapills). A copilot button
already exists there, gated by `ff-4076`, opening the chat `CopilotPanel` with `Source.CODE_EDITOR`
and `parameters: {language: 'json'}` (hardcoded).

**The plumbing exists; the agent is wrong.** `Source.CODE_EDITOR` routes to `CodeEditorSpringAIAgent`,
whose system prompt and tools are built for **per-node Script component code** (JS/Python/Ruby — "Your
role is to help with the Script component"), with `ScriptTools`. That same source is *also* used by the
per-node Script editor (gated by `ff-2504`). So the workflow-definition editor is currently served by a
script-focused agent that has no notion of workflow structure.

### Verified current state

- **Client:** `WorkflowCodeEditorSheet.tsx` (Monaco bound to the workflow `definition`,
  `defaultLanguage = workflow.format?.toLowerCase() ?? 'json'`) + `useWorkflowCodeEditorSheet.ts`
  (`handleCopilotClick` sets `source: Source.CODE_EDITOR`, `mode: MODE.ASK`,
  `parameters: {language: 'json'}`). Gated by `ff-4076` + `ai.copilot.enabled`. Renders `CopilotPanel`.
- **Per-node Script editor:** `PropertyCodeEditorDialog` (+ toolbar/hook) also uses `Source.CODE_EDITOR`,
  gated by `ff-2504`, passing the real script `language`. **This feature is correct and stays untouched.**
- **Routing:** the client computes the endpoint as
  `/api/platform/internal/ai/chat/${Source[sourceKey].toLowerCase()}`; `CopilotApiController` maps
  `code_editor` → `code_editor_ask`/`code_editor_build` by `Mode`.
- **Agents:** registered in `CopilotConfiguration` — each `LocalAgent` is built from a
  `@Value("classpath:prompt_<id>.txt")` system prompt + a `List.of(<MCP tools>)`. Existing workflow
  tools available in that config: `ReadProjectWorkflowTools`, `ComponentTools`, `WorkflowValidatorTools`,
  `WorkflowInstructionTools`, `TaskTools`, `FirecrawlTools`.
- **State contributors:** `CopilotRuntimeProvider` merges `useCopilotStateContributorRegistry.contribute()`
  into the agent state (`WorkflowEditorLayout` already contributes `workflowId`, `currentSelectedNode`).

## Goal

Serve the Workflow Code editor with a **dedicated, workflow-definition-aware copilot**:

- **Ask:** explain the definition, answer questions, help debug validation errors — grounded in the
  workflow's structure and the editor's real format (YAML or JSON).
- **Build:** produce a corrected/extended workflow definition the user can **apply into the editor**
  with one click (review + Save stay manual).

EE-only, gated on `ai.copilot.enabled && ff-4076`.

## Non-goals (deferred)

- Any change to the per-node **Script** editor copilot (`Source.CODE_EDITOR`, `ff-2504`,
  `CodeEditorSpringAIAgent`). It stays exactly as-is. (Renaming the shared source was explicitly cut.)
- Agent-side persistence of the definition (no workflow-update tool in this agent) — avoids clashing
  with the sheet's local unsaved buffer.
- Multi-file / multi-workflow editing; streaming apply; auto-Save after apply.
- A new feature flag (reuse `ff-4076`).

## Backend design (EE, `ai-copilot` modules)

1. **New source.** Append `WORKFLOW_CODE_EDITOR` to `com.bytechef.ee.ai.copilot.util.Source` (last, for
   ordinal safety) and to the client `Source` enum (`useCopilotStore`).
2. **Routing.** In `CopilotApiController`, add `else if (agentId.equals("workflow_code_editor"))` →
   `workflow_code_editor_build` / `workflow_code_editor_ask` by `Mode` (mirrors the `code_editor` branch).
3. **New agent.** `WorkflowCodeEditorSpringAIAgent` (in `...copilot.agent`), modeled on
   `CodeEditorSpringAIAgent` but workflow-definition-aware:
   - `createSystemMessage()` injects the editor **format** (`parameters.get("format")` → `yaml`/`json`)
     and the available workflow context (workflowId from state).
   - Registered in `CopilotConfiguration` as two `LocalAgent`s — `workflow_code_editor_ask` and
     `workflow_code_editor_build` — each from a new prompt resource + a workflow tool list
     (`ReadProjectWorkflowTools`, `ComponentTools`, `TaskTools`, `WorkflowValidatorTools`,
     `WorkflowInstructionTools`, `FirecrawlTools`). **No `ScriptTools`.**
4. **Prompts** (`src/main/resources`):
   - `prompt_workflow_code_editor_ask.txt`: role = help the user understand/debug the **workflow
     definition** in the given format; explain structure (triggers, tasks, `type`, `parameters`,
     datapill `${...}` refs); use validate-workflow to diagnose errors; **do not** modify — advise.
   - `prompt_workflow_code_editor_build.txt`: same grounding, but **produce a complete, valid workflow
     definition** in the editor's format inside a single fenced code block (```yaml/```json), preceded by
     a short explanation; the user applies it. Must keep it schema-valid (validate before answering).

## Frontend design

1. **Source switch.** `useWorkflowCodeEditorSheet.handleCopilotClick` sets
   `source: Source.WORKFLOW_CODE_EDITOR` and `parameters: {format: workflow.format?.toLowerCase() ?? 'json'}`
   (real format, not hardcoded `language: 'json'`). `WorkflowCodeEditorSheet` passes
   `source={Source.WORKFLOW_CODE_EDITOR}` to its `CopilotPanel`. `ff-4076` gating unchanged.
2. **Apply-to-editor.** Build returns the definition as a fenced code block in the chat. The
   `CopilotPanel`, when `source === WORKFLOW_CODE_EDITOR`, renders an **"Apply to editor"** action on
   assistant code blocks that calls an apply callback which loads the text into the Monaco buffer (the
   sheet's existing local `definition` state). The user then reviews and clicks the existing **Save**.
   - **Wiring:** `WorkflowCodeEditorSheet` registers an apply handler (a small Zustand registry, e.g.
     `useWorkflowCodeEditorApplyRegistry`, mirroring the state-contributor-registry pattern) while open;
     the panel's code-block action reads it. This keeps `CopilotPanel` generic and the editor decoupled.
   - **Exact assistant-ui rendering hook** (custom message/code-block component vs a client-side tool)
     is finalized during plan grounding; the architecture above (registry + code-block action) is fixed.

## Error handling

- Disabled copilot / missing flag → button hidden (existing gating).
- Build returns invalid JSON/YAML → "Apply to editor" still loads the text (the editor + Save-time
  validation surface errors), but the prompt instructs the agent to validate first; Ask mode helps fix.
- Apply with no registered handler (panel open outside the sheet) → action hidden.

## Testing

- **Backend:** `WorkflowCodeEditorSpringAIAgent` unit test (system message includes format + workflow
  grounding; ask vs build wording differs); `CopilotApiController` routing test for
  `workflow_code_editor` → ask/build by mode; a `CopilotConfiguration` wiring assertion if the existing
  config is test-covered.
- **Frontend:** `useWorkflowCodeEditorSheet` sets the new source + real format; the apply-registry
  store (register/apply/clear); the code-block "Apply to editor" action calls the handler and loads the
  buffer; gating unchanged.

## Architecture summary (units & boundaries)

| Unit | Responsibility | Depends on |
|------|----------------|------------|
| `Source.WORKFLOW_CODE_EDITOR` (server+client) | distinct routing token | — |
| `CopilotApiController` branch | map source → ask/build agent | Mode |
| `WorkflowCodeEditorSpringAIAgent` + 2 prompts | workflow-definition-aware system message | workflow MCP tools |
| `CopilotConfiguration` registration | wire 2 LocalAgents (prompt + tools) | agent, tools |
| `useWorkflowCodeEditorSheet` change | send new source + real format | Source, workflow.format |
| apply registry + code-block action | load build output into Monaco buffer | CopilotPanel, sheet |

Each unit is independently testable; the per-node Script copilot is wholly untouched.
