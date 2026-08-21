# Workflow Delegate Rename — Implementation Plan (Piece 0, renames only)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rename two delegate tools so their names describe what they are *for* rather than where
they came from, and land them in the same release as the `_manager` → `_agent` rename so external
MCP clients break once rather than twice.

| From | To |
|---|---|
| `workflow_editor_agent` | `project_workflow_agent` |
| `workflow_editor_embedded_agent` | `integration_workflow_agent` |

**Architecture:** Pure rename. No tools move, no agents are added or removed, no behaviour changes.

**Scope:** This is the rename half of Piece 0 of
`docs/superpowers/specs/2026-08-13-uniform-delegation-design.md`. It deliberately excludes the tool
reorganisation (Pieces 1–3), which have their own plans. Doing the rename first keeps the later
diffs readable: Piece 1 becomes purely "tools removed" rather than "tools removed and agent
renamed", which matters for review and for bisecting a regression.

## Why these names

`workflow_editor_agent` already owns all of `ProjectWorkflowTools`. "Editor" names its *origin* —
the workflow-editor panel — but a delegate is invoked by other agents, which never see that panel.
`project_workflow_agent` names its *role*, and matches the tool class it owns one-to-one.

The embedded twin cannot become `project_workflow_embedded_agent`, because embedded workflows belong
to **integrations**, not projects — its own prompt describes "code workflow integrations".
`integration_workflow_agent` keeps the meaningful pattern (`<owner>_workflow_agent`) instead of the
accidental one (the word "embedded").

Both target names are confirmed free in code; the only current occurrences are in the spec and plan
documents.

## Global Constraints

- Work on `0_732`. **The user commits to this branch in parallel** — always fresh commits, never
  amend, and always `git commit -- <paths>` with an explicit pathspec. A bare `git commit` takes the
  whole index and has already swept unrelated work into a commit twice on this branch.
- Never invent a ticket number. Commit messages take the form `732 <description>`.
- CE files carry Apache 2.0; EE files (`server/ee/**`) carry the ByteChef Enterprise header AND a
  `@version ee` Javadoc tag. This rename touches both trees.
- Java style: one blank line before control statements; no trailing blank line before a class's
  closing brace; test method names camelCase with NO underscores; no `TODO:`; no empty blocks.
- Client style: named imports sorted alphabetically; object keys sorted alphabetically.
- Verify with logs and `$?`, never a piped tail.

## The three failure modes to guard against

**1. A prompt naming a tool that no longer exists.** The model calls it and the turn dies with "No
ToolCallback found". It compiles perfectly. Both AI Hub prompts and `prompt_project_build.txt`
reference `workflow_editor_agent` by name.

**2. A hardcoded tool name in TypeScript.** `client/src/ee/pages/automation/ai-hub/runtime-providers/AiHubRuntimeProvider.tsx`
contains `if (toolCallName === 'workflow_editor_agent' || toolCallName === 'converter_agent')`, and
`tests/stripLeakedToolMarkup.test.ts` references it too. TypeScript will not catch a missed string —
it fails silently at runtime, and the symptom (tool markup leaking into the rendered message) is
several steps removed from the cause.

**3. A `@Qualifier` string that no longer resolves.** These are looked up through
`ObjectProvider.ifAvailable()`, which **swallows a missing bean and no-ops**. A mistyped qualifier
does not fail at startup — the delegate silently vanishes and the failure appears in production on
first use. Every renamed bean name must be checked by eye against its `@Bean` method.

## Known reference sites

Established by `git grep`; re-derive rather than trusting this list, since the branch moves.

**`workflow_editor_agent`** — 20+ files including: `CopilotAgentType`, `WorkflowEditorAgentToolCallback`
(+ its test), `ProjectAgentConfiguration` (+ test), `AiHubConfiguration`,
`PinnedToolSearchToolCallingAdvisor` (+ test), `WorkspaceScopedSubAgentToolCallback`,
`EmbeddedCopilotMcpContributorConfiguration`, `McpServerToolCallbackContributorConfigurationTest`,
`ManagementMcpServerToolCallbackProviderTest`, `ComponentSlugUtils`, `prompt_ai_hub_ask.txt`,
`prompt_ai_hub_build.txt`, `prompt_project_build.txt`, `AiHubRuntimeProvider.tsx`,
`stripLeakedToolMarkup.test.ts`, `docs/feature-mind-map.md`, `docs/feature-mind-map.opml`.

**Do NOT rewrite** anything under `docs/superpowers/**` — those are point-in-time design records.

---

### Task 1: Rename `workflow_editor_agent` → `project_workflow_agent`

**Files:** as enumerated above, re-derived by grep.

**Interfaces:**
- Produces: tool name `project_workflow_agent`; enum constant `PROJECT_WORKFLOW_AGENT`; class
  `ProjectWorkflowAgentToolCallback` (from `WorkflowEditorAgentToolCallback`).

- [ ] **Step 1: Re-derive the reference list**

```bash
git grep -l "workflow_editor_agent\|WorkflowEditorAgentToolCallback\|WORKFLOW_EDITOR_AGENT" -- '*.java' '*.txt' '*.ts' '*.tsx' '*.md' '*.mdx' | grep -v "docs/superpowers"
```

Compare against the list above and report any file that appears in one but not the other.

**Careful:** `workflow_editor_embedded_agent` **contains** `workflow_editor_agent` as a substring in
neither direction — but `WORKFLOW_EDITOR_AGENT` and `WORKFLOW_EDITOR_EMBEDDED_AGENT` are distinct
constants that a careless regex can conflate. Judge each hit; never blanket-replace.

- [ ] **Step 2: Rename the class**

`git mv` `WorkflowEditorAgentToolCallback.java` → `ProjectWorkflowAgentToolCallback.java` and its
test, so history follows. Update the class name, constructor, and the `TOOL_NAME`/`name(...)` value.

- [ ] **Step 3: Rename the enum constant and key**

`WORKFLOW_EDITOR_AGENT("workflow_editor_agent", false)` → `PROJECT_WORKFLOW_AGENT("project_workflow_agent", false)`
in `CopilotAgentType`. Leave `WORKFLOW_EDITOR_ASK` / `_BUILD` / the bare `WORKFLOW_EDITOR` fallback
alone — those are the panel agents and are not being renamed.

- [ ] **Step 4: Update every registration, qualifier and bean name**

Including `ProjectAgentConfiguration`, `AiHubConfiguration`, the CE MCP contributor, and
`PinnedToolSearchToolCallingAdvisor`. Check each `@Qualifier` string against its `@Bean` method name
by eye — see failure mode 3.

- [ ] **Step 5: Update the prompts**

`prompt_ai_hub_ask.txt`, `prompt_ai_hub_build.txt`, `prompt_project_build.txt`. The project BUILD
prompt has a substantial passage instructing delegation to this tool — rename the tool but do not
reword the instruction.

- [ ] **Step 6: Update the client**

`AiHubRuntimeProvider.tsx` and `stripLeakedToolMarkup.test.ts`. These are string comparisons;
TypeScript will not catch a miss.

- [ ] **Step 7: Update the docs**

`docs/feature-mind-map.md` and `.opml`. Not `docs/superpowers/**`.

- [ ] **Step 8: Verify**

```bash
./gradlew spotlessApply > /dev/null 2>&1
./gradlew compileJava > /tmp/r1.log 2>&1; echo "compile exit=$?"
./gradlew :server:libs:ai:ai-copilot:ai-copilot-tool:check :server:libs:ai:ai-copilot:ai-copilot-service:check :server:ee:libs:ai:ai-hub:ai-hub-service:check :server:ee:libs:embedded:embedded-ai:embedded-ai-copilot:check > /tmp/r2.log 2>&1; echo "check exit=$?"
```

Then from `client/`: `npm run check`. **Note:** this branch currently has pre-existing Prettier drift
in `DataTable.tsx`, `Header.tsx` and `LayoutContainer.tsx` from unrelated concurrent work. If
`npm run check` fails only on those, verify your own touched files individually and say so — do not
"fix" someone else's in-progress files.

- [ ] **Step 9: Commit**

```bash
git commit -- <your paths> -m "732 Rename the workflow editor delegate to project_workflow_agent"
```

---

### Task 2: Rename `workflow_editor_embedded_agent` → `integration_workflow_agent`

**Interfaces:**
- Produces: tool name `integration_workflow_agent`; enum constant `INTEGRATION_WORKFLOW_AGENT`;
  class `IntegrationWorkflowAgentToolCallback`.

- [ ] **Step 1: Re-derive the reference list**

```bash
git grep -l "workflow_editor_embedded_agent\|WorkflowEditorEmbeddedAgentToolCallback\|WORKFLOW_EDITOR_EMBEDDED_AGENT" -- '*.java' '*.txt' '*.ts' '*.tsx' '*.md' '*.mdx' | grep -v "docs/superpowers"
```

- [ ] **Step 2: Confirm the premise before renaming**

Read the embedded delegate's prompt and confirm it operates on **integration** workflows rather than
project workflows. The spec asserts this on the basis of a prompt describing "code workflow
integrations" — verify it rather than trusting the assertion. If the embedded surface turns out to
work on projects after all, STOP and report; the name would then be wrong.

- [ ] **Step 3–7: Same shape as Task 1**

Class rename via `git mv`, enum constant, registrations and qualifiers, prompts, client, docs.
Leave `WORKFLOW_EDITOR_EMBEDDED_ASK` / `_BUILD` / the bare fallback alone.

- [ ] **Step 8: Verify** — same commands as Task 1, plus the embedded module's `check`.

- [ ] **Step 9: Commit**

```bash
git commit -- <your paths> -m "732 Rename the embedded workflow delegate to integration_workflow_agent"
```

---

### Task 3: Final sweep

- [ ] **Step 1: Confirm nothing references the old names**

```bash
git grep -n "workflow_editor_agent\|workflow_editor_embedded_agent\|WorkflowEditorAgentToolCallback\|WorkflowEditorEmbeddedAgentToolCallback" -- '*.java' '*.txt' '*.ts' '*.tsx' '*.md' '*.mdx' | grep -v "docs/superpowers"
```

Expected: empty. Report anything remaining and why it is legitimate.

- [ ] **Step 2: Confirm the panel agents were NOT renamed**

```bash
git grep -c "workflow_editor_ask\|workflow_editor_build\|workflow_editor_embedded_ask\|workflow_editor_embedded_build"
```

These must still exist — a rename that caught them would break the panel's routing, since those
strings are simultaneously bean ids, controller route values and session-memory keys.

- [ ] **Step 3: Confirm every prompt names only registered tools**

For each prompt resource you touched, extract its backticked tool names and confirm each is
registered on the agent that loads it.

## Manual verification

Requires a running backend.

1. **AI Hub**, BUILD: ask for a new workflow. The root should delegate to `project_workflow_agent`
   and succeed. Watch for "No ToolCallback found", which would mean a prompt still names the old tool.
2. **Projects panel**, BUILD: ask for a workflow in a project. `project_build` delegates to the
   renamed tool.
3. **MCP**: list the management server's tools and confirm `project_workflow_agent` is present and
   `workflow_editor_agent` is gone.
4. **AI Hub rendering**: confirm delegate tool-call markup still renders correctly — that is what
   `AiHubRuntimeProvider.tsx`'s string comparison controls, and a missed rename there shows up as
   leaked markup in the message body rather than an error.

## Self-review notes

**Not in scope:** any tool moving between agents, `project_agent`, or the asset-file demotion. Those
are Pieces 1–3 and have their own plans.

**The riskiest part** is the client string comparison — it is the one place a missed rename produces
no error at all. Task 1 Step 6 and the manual check at item 4 exist for it specifically.
