# Intelligent Tool Renames — Implementation Plan (spec step 2)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rename the nine intelligent tools from agent names to capability names. Pure rename — no
tools move, no behaviour changes.

**Prerequisite:** `docs/superpowers/plans/2026-08-17-intelligent-tool-catalog.md` has landed. Each
tool name then lives in exactly one `IntelligentToolDefinition`, which is what makes this plan an
afternoon instead of a four-site sweep per tool.

| From | To |
|---|---|
| `project_workflow_agent` | `buildWorkflow` |
| `integration_workflow_agent` | `buildIntegrationWorkflow` |
| `converter_agent` | `importWorkflow` |
| `workflow_execution_agent` | `debugWorkflowExecution` |
| `cluster_element_agent` | `configureClusterElement` |
| `code_editor_agent` | `writeScript` |
| `code_workflow_agent` | `buildCodeWorkflow` |
| `custom_component_agent` | `buildCustomComponent` |
| `skills_agent` | `authorSkill` |

(`mcp_agent` → `configureMcpServer` is NOT here — it is a promotion with behaviour change, plan
`2026-08-17-configure-mcp-server.md`.)

camelCase because that is what this codebase's real tools use (`createProject`, `listAssetFiles`);
snake_case was the delegate convention being retired.

## Global Constraints

- Work on `0_732`; user commits in parallel — fresh commits, never amend,
  `git commit -m "..." -- <paths>` (message flag before the pathspec).
- Commit messages `732 <description>` / `732 client - <description>`.
- CE Apache 2.0 headers; EE Enterprise header + `@version ee`.
- Java style per CLAUDE.md; client style: sorted named imports, sorted object keys.
- Verify via log files + `$?` + `grep '^> Task .* FAILED'`; never a piped tail.
- **Do NOT rewrite anything under `docs/superpowers/**`** — point-in-time records.
- **This breaks external MCP clients once.** There is no deprecation seam for MCP tool names. Say
  so in the release notes; do not build a compatibility alias layer (rejected in the spec — the
  catalog would carry double registrations forever).

## The three failure modes (from the executed Piece-0 rename; all three bit or nearly bit)

1. **A prompt naming a tool that no longer exists** — the model calls it, the turn dies at runtime
   with "No ToolCallback found". Compiles perfectly.
2. **A hardcoded tool-name string in TypeScript** — `AiHubRuntimeProvider.tsx` dispatches on tool
   NAME (`toolCallName === 'project_workflow_agent' || toolCallName === 'converter_agent'`), and
   `stripLeakedToolMarkup.test.ts` pins it. A missed string fails silently as leaked tool markup in
   rendered messages, several steps from the cause.
3. **An `agentTypeKey` that stops matching `AgentTypeRegistry`** — session purge reconstructs
   per-specialist keys from the registry; an unregistered key leaves session rows nothing ever
   deletes. **Decision required before implementing:** the tool NAME changes; whether
   `agentTypeKey()` (and the `CopilotAgentType` key string) changes with it affects existing stored
   sessions keyed `<threadId>:<oldKey>`. Recommended: rename enum constants AND key strings for
   coherence, and accept that pre-rename specialist sessions orphan (they are bounded-replay caches,
   not user data; task-delete purges by current keys, so old-key rows linger until their chats are
   deleted — document this in the commit message). If the human partner prefers stable keys under
   renamed tools, `agentTypeKey()` is exactly the seam that allows it: keep the old key string,
   rename only `name()`. ASK which before starting.

## Deliberate non-goals

- **Class renames** (`ProjectWorkflowAgentToolCallback` et al. keep their names). Post-catalog the
  class is internal wiring; the public identity is the definition's `name()`. Churning 20+ files
  per class rename buys nothing — and the shared class serves two delegates (`buildWorkflow` and
  `buildIntegrationWorkflow`), so no single capability name fits it anyway.
- Bean renames (`workflowEditorBuildSubAgentChatClient` etc.). Same reason.

---

### Task 1: Rename in the definitions and enum

**Files:** the three contributors (`CopilotIntelligentToolContributor`,
`AutomationIntelligentToolContributor`, `EmbeddedIntelligentToolContributor`),
`CopilotAgentType`.

- [ ] **Step 1: Re-derive every reference site:**
  ```bash
  git grep -ln "project_workflow_agent\|integration_workflow_agent\|converter_agent\|workflow_execution_agent\|cluster_element_agent\|code_editor_agent\|code_workflow_agent\|custom_component_agent\|skills_agent" -- '*.java' '*.txt' '*.ts' '*.tsx' '*.md' '*.mdx' '*.graphqls' | grep -v docs/superpowers
  ```
  Bucket the hits: definitions/enum (this task), prompts (Task 2), client (Task 3), docs (Task 4).
  Anything that fits no bucket: STOP and report before touching it.
- [ ] **Step 2:** Apply the table to the nine `name()` values and the `CopilotAgentType`
  constants/keys per the decision recorded from the constraint above. **Watch the near-collisions:**
  `skills_agent` vs the `SKILLS` panel type, `code_workflow_agent` vs the `code_workflow` panel
  sources — judge each hit, never blanket-replace.
- [ ] **Step 3:** Update the parity test's expected name list (it MUST fail before this step and
  pass after — that failure is the proof the test guards names).
- [ ] **Step 4:** Compile + module checks (log, `$?`, FAILED grep). Commit with pathspec.

### Task 2: Prompts

**Files (re-derive; known today):** `prompt_ai_hub_ask.txt`, `prompt_ai_hub_build.txt`,
`prompt_project_build.txt`, and any specialist prompt that names a sibling delegate.

- [ ] Rename tool references only; do not reword the delegation instructions around them.
- [ ] **Closing check:** for each touched prompt, extract every backticked tool name and confirm
  each is registered on the agent that loads that prompt.
- [ ] Commit with pathspec.

### Task 3: Client

**Files:** `client/src/ee/pages/automation/ai-hub/runtime-providers/AiHubRuntimeProvider.tsx`,
`client/src/ee/.../tests/stripLeakedToolMarkup.test.ts`, plus whatever Task 1's grep found.

- [ ] String comparisons — TypeScript catches nothing here. Update, then run from `client/`:
  `npm run check > /tmp/rn-client.log 2>&1; echo "exit=$?"`. If it fails ONLY on files you did not
  touch (parallel-work drift), verify your own files individually and say so — do not fix others'
  in-progress files.
- [ ] Commit: `732 client - Rename intelligent delegate tool names to capability names`.

### Task 4: Docs sweep

- [ ] `docs/feature-mind-map.md` / `.opml` and any `.agents/*.md` hits from Task 1's grep. Not
  `docs/superpowers/**`.
- [ ] Final sweep: Task 1's grep must return nothing outside `docs/superpowers`. Report any
  legitimate remainder explicitly.
- [ ] Commit.

## Manual verification

1. AI Hub BUILD: request a workflow — the root calls `buildWorkflow` (visible in the tool-call
   event); no "No ToolCallback found".
2. AI Hub rendering: delegate tool markup renders as cards, not leaked text (failure mode 2).
3. MCP tools/list: new names present, old names absent.
