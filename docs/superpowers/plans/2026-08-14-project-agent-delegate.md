# `project_agent` Delegate — Implementation Plan (Piece 2)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give Projects a delegate on AI Hub and the management MCP server, so all ten Automation
listing-page domains are reachable the same way — and so the AI Hub root sequences project creation
and workflow authoring as two sibling calls rather than either agent doing both.

**Architecture:** Follow the `asset_file_agent` shape from phase 4 — one tool-callbacks source
feeding the panel agents, the AI Hub delegate, and the MCP contribution, so the three cannot drift.

**Scope:** Piece 2 of `docs/superpowers/specs/2026-08-13-uniform-delegation-design.md`. Piece 1 is
already merged. Piece 3 (moving the root's leaf tools behind the delegates) is deliberately NOT in
this plan — it is a separate, more visible behaviour change.

## Global Constraints

- Work on `0_732`. **The user commits to this branch in parallel** — always fresh commits, never
  amend, and always `git commit -- <explicit paths> -m "..."`. A bare `git commit` takes the whole
  index and has swept unrelated work into commits twice on this branch. Run `git status --porcelain`
  before each commit and confirm you recognise every file.
- Never invent a ticket number. Commit messages take the form `732 <description>`.
- CE files (`server/libs/**`) carry Apache 2.0; EE files (`server/ee/**`) carry the ByteChef
  Enterprise header AND a `@version ee` Javadoc tag. This plan touches both trees.
- Java style: one blank line before control statements; one blank line between a variable
  modification and the statement using it; no trailing blank line before a class's closing brace;
  descriptive names; no `_` prefix on private methods. Test method names camelCase with NO
  underscores. No `TODO:` comments, no empty blocks.
- Verify by redirecting Gradle output to a log file and echoing `$?` separately. A piped run hides
  its exit code; a green-looking tail means nothing.

## Two failure modes that compile cleanly

**A prompt naming an unregistered tool** makes the model call it and the turn dies with "No
ToolCallback found". It compiles perfectly and fails only at runtime.

**A mistyped `@Qualifier` does NOT fail at startup.** These are resolved through
`ObjectProvider.ifAvailable()`, which swallows a missing bean and no-ops — so the delegate silently
vanishes and the failure surfaces in production on first use. Check every qualifier string against
its `@Bean` method name by eye.

## Prerequisite the spec did not account for

`ProjectAgentConfiguration` currently publishes **only the two panel agents**
(`projectAskSpringAIAgent`, `projectBuildSpringAIAgent`). Unlike `AssetFileAgentConfiguration`, it
has **no sub-agent `ChatClient` beans** — so there is nothing for a delegate to wrap yet. Task 1
creates them.

Read `AssetFileAgentConfiguration` in full before starting; it is the working template for every
structural decision in this plan.

---

### Task 1: Publish the Project sub-agent ChatClient beans

**Files:**
- Modify: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/ProjectAgentConfiguration.java`
- Test: extend `ProjectAgentConfigurationTest`

**Interfaces:**
- Produces: beans named exactly `projectAskSubAgentChatClient` and `projectBuildSubAgentChatClient`.
  Task 3 and Task 4 look these up by `@Qualifier` string; the names are a contract.

**Design notes, already decided — do not re-litigate:**

- Mirror `AssetFileAgentConfiguration`'s sub-agent beans: `ChatClient.builder(chatModel)` +
  `.defaultSystem(<the same prompt the matching panel agent uses>)` + `.defaultToolCallbacks(<the
  same tool list>)`.
- **The sub-agent clients take the RAW tool list, not the `RehydrateContextToolCallback`-wrapped
  one.** Only the panel agents' lists are wrapped. This asymmetry is deliberate and matches every
  existing slice — the wrapper restores the SecurityContext on the panel's SSE worker thread, while
  a delegate is invoked inline from a tool callback that already has one.
- The BUILD panel agent currently adds `project_workflow_agent` and `converter_agent` delegates to
  its tool list. **The sub-agent BUILD client must NOT carry those.** The panel is the top level and
  must reach them itself; the AI Hub root holds them as siblings and will sequence. Putting them on
  the delegate creates the nesting this design exists to avoid. Extract the shared tool list so the
  panel and the delegate can differ on exactly this point without duplicating the rest.

- [ ] **Step 1: Read the template**

Read `AssetFileAgentConfiguration` and the existing `ProjectAgentConfiguration` side by side. Note
how the asset-file slice separates the tool-list construction from the bean definitions.

- [ ] **Step 2: Write the failing test**

Extend `ProjectAgentConfigurationTest` with assertions that both new beans exist and carry the
expected tool sets. Model the construction on however that test already builds the configuration
(it uses a reflective resource-field helper). Assert specifically that the BUILD sub-agent client's
tool names do **not** include `project_workflow_agent` or `converter_agent`, since that is the
distinction most likely to be got wrong.

- [ ] **Step 3: Run it to verify it fails**

```bash
./gradlew :server:libs:ai:ai-copilot:ai-copilot-service:test --tests '*ProjectAgentConfigurationTest' > /tmp/p2t1.log 2>&1; echo "exit=$?"
```

- [ ] **Step 4: Implement**

- [ ] **Step 5: Verify**

```bash
./gradlew spotlessApply > /dev/null 2>&1
./gradlew :server:libs:ai:ai-copilot:ai-copilot-service:check > /tmp/p2t1c.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/p2t1c.log
```

- [ ] **Step 6: Commit**

```bash
git commit -- server/libs/ai/ai-copilot/ai-copilot-service/src -m "732 Publish the project sub-agent chat clients"
```

---

### Task 2: Add `ProjectAgentToolCallback`

**Files:**
- Create: `server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/ProjectAgentToolCallback.java`
- Test: `.../ai-copilot-tool/src/test/java/com/bytechef/ai/copilot/tool/ProjectAgentToolCallbackTest.java`
- Modify: `CopilotAgentType` — add `PROJECT_AGENT("project_agent", false)`

**Interfaces:**
- Produces: tool name `project_agent`; `CopilotAgentType.PROJECT_AGENT`.

- [ ] **Step 1: Copy the sibling**

`AssetFileAgentToolCallback` is the closest template. Copy its structure exactly — including the
`CurrentAgentContext.callWith(...)` wrapper, the null-result handling, and the two catch arms.

**The load-bearing line is the tool-context forwarding:**
`Map<String, Object> forwardedContext = toolContext == null ? Map.of() : toolContext.getContext();`
passed verbatim to `.toolContext(forwardedContext)`. That is the only path by which workspace and
environment context reaches the leaf tools through a delegate. Do not "improve" it — omitting it
causes every inner tool to fail with "Workspace context unavailable", a defect that has already
shipped once in this codebase.

- [ ] **Step 2: Write the description**

It must describe project *lifecycle* — creating, renaming, publishing, deleting projects, and
listing or searching them. It must NOT claim to author workflow content; that is
`project_workflow_agent`'s job, and a description that overlaps causes the model to pick wrong.

- [ ] **Step 3: Test**

Mirror `AssetFileAgentToolCallbackTest`: assert the tool name, assert a blank request returns the
typed error, and assert the parent `ToolContext` is forwarded verbatim (capture the argument to
`toolContext(...)` with an `ArgumentCaptor`). That last one guards the seam named in Step 1.

- [ ] **Step 4–5: Verify and commit**

```bash
git commit -- server/libs/ai/ai-copilot/ai-copilot-tool/src -m "732 Add the project agent delegate"
```

---

### Task 3: Register on the management MCP server

**Files:**
- Modify: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/ToolCallbackContributorConfiguration.java`

- [ ] **Step 1: Add the contribution**

One `@Qualifier("projectBuildSubAgentChatClient") ObjectProvider<ChatClient>` parameter and one
`ifAvailable` block, following the pattern of every other delegate in that method.

**It must be wrapped in `WorkspaceScopedSubAgentToolCallback`.** An unwrapped delegate forwards an
empty tool context and every inner tool fails — this is the defect phase 0 fixed for all the others.

- [ ] **Step 2: Update the count assertion**

`McpServerToolCallbackContributorConfigurationTest` asserts the contributed tool names. Add
`project_agent`. If it asserts a bare count rather than names, strengthen it to names — a count
passes while the wrong client is wired to the wrong delegate, since every provider is the same type.

- [ ] **Step 3–4: Verify and commit**

---

### Task 4: Register on the AI Hub root, and teach it to sequence

**Files:**
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/config/AiHubConfiguration.java`
- Modify: `.../ai-hub-service/src/main/resources/prompt_ai_hub_ask.txt` and `prompt_ai_hub_build.txt`

- [ ] **Step 1: Register the delegate**

Thread a `@Qualifier("projectAskSubAgentChatClient")` / `...Build...` provider through to the
sub-agent registration method and add an `ifAvailable` block wrapping the callback in
`ProgressReportingToolCallback` and `wrapDelegate(...)`, exactly as the sibling delegates do.

ASK gets the ask client, BUILD gets the build client. Getting these crossed gives an ASK agent
write capability — a defect this codebase has hit before.

- [ ] **Step 2: Document it in both prompts**

Every sibling delegate has a bullet in the AI Hub prompts. Add `project_agent` to both, scoped
correctly: the ASK entry must say it cannot create, publish or delete, and should suggest switching
to BUILD.

- [ ] **Step 3: Teach the root to sequence — this is the point of the piece**

The BUILD prompt must state that creating a project and creating a workflow inside it are **two
delegate calls in order**: `project_agent` to create the project, then `project_workflow_agent` to
author the workflow, with the project id carried from the first result into the second request.

Without this the model hands the whole request to one delegate and gets a partial result. Wiring
alone does not produce the behaviour; the prompt is what makes the root sequence.

- [ ] **Step 4: Verify**

Confirm every tool named in both prompts is registered on the agent that loads it. Then:

```bash
./gradlew spotlessApply > /dev/null 2>&1
./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:check :server:libs:ai:ai-copilot:ai-copilot-service:check > /tmp/p2t4.log 2>&1; echo "exit=$?"
```

- [ ] **Step 5: Commit**

---

### Task 5: Drop `ProjectAuthoringTools` from the workflow-editor beans

**This task is optional and may be deferred.** It is a consequence of Task 4 rather than a
prerequisite for it, and it is only safe once the root actually sequences. If Task 4's manual
verification has not happened yet, stop after Task 4 and report — landing this on an unverified
assumption is how a working path breaks.

**Files:**
- Modify: `CopilotConfiguration.java` (both workflow-editor beans)
- Modify: `prompt_workflow_editor_build.txt`

- [ ] **Step 1: Understand why both lose it, for different reasons**

- The **panel** agent is only reachable from inside a project's workflow editor, so it can never be
  asked to create a project. The tools are already dead weight.
- The **delegate** is reachable from AI Hub with no project context — which is why it needs them
  today — but after Task 4 the root creates the project first.

- [ ] **Step 2: Remove, and edit the shared prompt in the same commit**

Both beans load the same resource, so the prompt's instructions are the *union* of what either
needs — which is why it documents `createProject` at all. Removing the tools without editing the
prompt reproduces "No ToolCallback found". Rewrite the brand-new-workflow passage to assume the
project already exists.

- [ ] **Step 3–4: Verify and commit**

---

## Manual verification

Requires a running backend; none of this is provable from tests.

1. **AI Hub BUILD:** "create a project called Billing with a workflow that syncs Stripe invoices to
   Postgres". Expected: two delegate calls in order — `project_agent`, then
   `project_workflow_agent` — and a working workflow in a new project. This is the behaviour the
   whole piece exists for.
2. **AI Hub ASK:** ask it to delete a project. Expected: it declines and suggests BUILD mode.
3. **MCP:** list the management server's tools; `project_agent` present and workspace-scoped
   (it should accept an optional `workspaceId` and auto-select when the tenant has one workspace).
4. **Regression — the Projects panel is unchanged:** the panel must still create a project and a
   workflow in one turn, delegating content to `project_workflow_agent`. Task 1 deliberately gives
   the panel and the delegate different tool lists; this check proves the panel kept its.
5. **After Task 5 only:** in the workflow editor's Copilot, ask for a workflow in a project that does
   not exist. Expected: it explains it cannot create the project rather than failing opaquely.

## Self-review notes

**Not in scope:** Piece 3 (moving the root's 15 project leaf tools and the two asset-file reads
behind their delegates). That is the visible behaviour change and deserves its own plan and its own
verification.

**The riskiest task is 4**, because the sequencing behaviour comes from prompt text rather than
code — nothing fails loudly if the model ignores it. Manual check 1 is the only real test.

**Task 5 is deliberately last and deliberately optional**, because it removes a working path on the
strength of Task 4's prompt change actually taking effect.
