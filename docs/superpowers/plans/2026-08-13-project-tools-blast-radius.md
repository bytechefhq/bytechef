# Project Tools Blast Radius — Implementation Plan (Piece 1)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stop the workflow-editor and converter agents from being able to publish and delete
projects — capabilities neither of them uses, neither advertises, and both currently hold.

**Architecture:** `ProjectTools` is an all-or-nothing bundle of eight operations. Introduce a narrow
`ProjectAuthoringTools` carrying only the two an authoring agent actually needs, switch the
workflow-editor clients onto it, and remove project tools from the converter entirely.

**Tech Stack:** Java 25 / Spring Boot 4, Spring AI `@Tool` methods, JUnit 5 + Mockito + AssertJ.

**Scope:** This is Piece 1 of `docs/superpowers/specs/2026-08-13-uniform-delegation-design.md`. It is
deliberately landable alone: it fixes a live defect, adds no delegates, moves no tools behind
delegates, and does not commit to Pieces 0, 2 or 3. Those get their own plans.

## Global Constraints

- Work on `0_732`. **The user commits to this branch in parallel** — always fresh commits, never
  amend, and always `git commit -- <paths>` with an explicit pathspec so an unrelated staged file
  cannot be swept in. This has already happened twice; the pathspec form is the fix.
- Never invent a ticket number. Commit messages take the form `732 <description>`.
- CE files (`server/libs/**`) carry the Apache 2.0 header copied verbatim from a neighbouring file.
  Nothing here is EE.
- Java style: one blank line before control statements; one blank line between a variable
  modification and the statement using it; no trailing blank line before a class's closing brace;
  descriptive names; no `_` prefix on private methods. Test method names camelCase with NO
  underscores. No `TODO:` comments, no empty blocks.
- Verify with `./gradlew spotlessApply`, then module-scoped `check`, redirected to a log file with
  `$?` echoed separately — a piped Gradle run hides its exit code.
- **A prompt must never name a tool that is not registered on that agent.** The model will call it
  and the turn dies with "No ToolCallback found" — it compiles perfectly and fails only at runtime.

## The evidence this plan rests on

Both findings were established by reading the agents' own prompts, not by reasoning from names.
Re-verify them before implementing; if either has changed, stop and report.

**The workflow-editor BUILD prompt names exactly two project tools:**

- `searchProjects(query)` — "search for a specific project if asked to create a new one"
- `createProject(name)` — "create a new project if asked to create a new one"

It never mentions `publishProject`, `deleteProject`, `updateProject`, `getProjectStatus`,
`listProjects`, or `getProject`.

**The converter BUILD prompt names no tools at all.** It states its output "MUST be valid JSON with
no explanations, prefixes, or suffixes" — it returns a definition and the caller persists it. So it
needs neither `ProjectTools` nor `ProjectWorkflowTools`.

## The four affected beans

All in `server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/CopilotConfiguration.java`:

| Bean | Currently injects | Should inject |
|---|---|---|
| `workflowEditorBuildSpringAIAgent` | `ProjectTools`, `ProjectWorkflowTools` | `ProjectAuthoringTools`, `ProjectWorkflowTools` |
| `workflowEditorBuildSubAgentChatClient` | `ProjectTools`, `ProjectWorkflowTools` | `ProjectAuthoringTools`, `ProjectWorkflowTools` |
| `converterBuildSpringAIAgent` | `ProjectTools`, `ProjectWorkflowTools` | neither |
| `converterBuildSubAgentChatClient` | `ProjectTools`, `ProjectWorkflowTools` | neither |

Line numbers drift; locate by bean name.

---

### Task 1: Add `ProjectAuthoringTools`

**Files:**
- Create: `server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/ProjectAuthoringTools.java`
- Test: `server/libs/automation/automation-ai/automation-ai-tool/src/test/java/com/bytechef/automation/ai/tool/ProjectAuthoringToolsTest.java`

**Interfaces:**
- Consumes: the same facade/service `ProjectTools` uses — read `ProjectTools` first and mirror its
  constructor, its `@Tool` annotation style, its parameter descriptions, and its return types
  exactly. This class is a strict subset, not a reimplementation.
- Produces: `searchProjects` and `createProject` with **byte-identical tool names, descriptions and
  signatures** to their `ProjectTools` counterparts. The model must not be able to tell which class
  provided them.

- [ ] **Step 1: Read `ProjectTools` in full**

Note its constructor dependencies, how each `@Tool` method is annotated, and the exact text of the
`searchProjects` and `createProject` descriptions. Copy them verbatim in the next step — a
reworded description changes model behaviour for no reason.

- [ ] **Step 2: Write the failing test**

```java
class ProjectAuthoringToolsTest {

    @Test
    void testExposesOnlyAuthoringTools() {
        ProjectAuthoringTools projectAuthoringTools = new ProjectAuthoringTools(/* same deps as ProjectTools */);

        List<String> toolNames = Arrays.stream(ToolCallbacks.from(projectAuthoringTools))
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();

        assertThat(toolNames).containsExactlyInAnyOrder("searchProjects", "createProject");
    }
}
```

Add a second test asserting the two tool *descriptions* match `ProjectTools`' versions exactly, so a
future edit to one cannot silently diverge from the other.

- [ ] **Step 3: Run it to verify it fails**

```bash
./gradlew :server:libs:automation:automation-ai:automation-ai-tool:test --tests '*ProjectAuthoringToolsTest' > /tmp/t1.log 2>&1; echo "exit=$?"
```

Expected: compilation failure — the class does not exist.

- [ ] **Step 4: Implement**

Create the class as a strict subset of `ProjectTools`. Delegate to the same underlying service
rather than duplicating logic. Javadoc must state why it exists: authoring agents need to create a
project to land their output, but must not be able to publish or delete one.

- [ ] **Step 5: Verify**

```bash
./gradlew spotlessApply > /dev/null 2>&1; ./gradlew :server:libs:automation:automation-ai:automation-ai-tool:check > /tmp/t1c.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/t1c.log
```

- [ ] **Step 6: Commit**

```bash
git commit -- server/libs/automation/automation-ai/automation-ai-tool/src -m "732 Add ProjectAuthoringTools for agents that only need to create a project"
```

---

### Task 2: Switch the workflow-editor clients onto it

**Files:**
- Modify: `.../ai-copilot-service/.../config/CopilotConfiguration.java`

**Interfaces:**
- Consumes: `ProjectAuthoringTools` from Task 1.
- Produces: no new API; the two workflow-editor beans lose six project tools.

- [ ] **Step 1: Swap the injection**

In `workflowEditorBuildSpringAIAgent` and `workflowEditorBuildSubAgentChatClient`, replace the
`ProjectTools projectTools` parameter with `ProjectAuthoringTools projectAuthoringTools`, and update
the corresponding `.defaultTools(...)` / `.tools(...)` call. Leave `ProjectWorkflowTools` and every
other tool class untouched.

- [ ] **Step 2: Confirm the prompt still matches**

Re-read `prompt_workflow_editor_build.txt` and confirm every tool it names is still registered. It
should name only `searchProjects` and `createProject` from the project side — but verify rather than
assume, and if it names anything else, STOP and report rather than adding the tool back.

Also check the ASK-side prompt and its bean: this task does not change ASK, but confirm it was not
relying on a BUILD-only tool.

- [ ] **Step 3: Verify**

```bash
./gradlew spotlessApply > /dev/null 2>&1; ./gradlew :server:libs:ai:ai-copilot:ai-copilot-service:check > /tmp/t2.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/t2.log
```

- [ ] **Step 4: Commit**

```bash
git commit -- server/libs/ai/ai-copilot/ai-copilot-service/src -m "732 Stop the workflow editor agents carrying project lifecycle tools"
```

---

### Task 3: Remove project tools from the converter

**Files:**
- Modify: `.../ai-copilot-service/.../config/CopilotConfiguration.java`

**Interfaces:** none.

**Care required.** This is the task most likely to break something, because the converter's prompt
names no tools — so there is no prompt evidence either way about `ProjectWorkflowTools`. The prompt
says it returns JSON, which implies it persists nothing, but "implies" is not "proves".

- [ ] **Step 1: Establish whether the converter persists anything**

Before removing anything, find every caller of `converterBuildSubAgentChatClient` and
`converterBuildSpringAIAgent` and determine what happens to their output. `ConverterAgentToolCallback`
is the delegate wrapper; read it and whatever consumes its return value. Also read
`useConverterN8nToWorkflow.ts` on the client, which calls the converter route directly.

If the caller persists the returned definition, removing both tool classes is correct. If the
converter is expected to persist its own output despite the prompt, STOP and report — the fix is
then a prompt change, not a tool change, and that is a different task.

- [ ] **Step 2: Remove**

Assuming step 1 confirms it persists nothing, drop both `ProjectTools` and `ProjectWorkflowTools`
from both converter beans. Remove the now-unused parameters and imports.

- [ ] **Step 3: Verify, including the client**

```bash
./gradlew spotlessApply > /dev/null 2>&1; ./gradlew :server:libs:ai:ai-copilot:ai-copilot-service:check > /tmp/t3.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/t3.log
```

The client's n8n import path exercises this end to end. It cannot be verified without a running
backend, so record it as a manual check rather than claiming it passes.

- [ ] **Step 4: Commit**

```bash
git commit -- server/libs/ai/ai-copilot/ai-copilot-service/src -m "732 Stop the converter agents carrying project tools they never call"
```

---

## Manual verification

None of this can be proven without a running backend. In priority order:

1. **The capability that was removed.** In the workflow editor's Copilot, Build mode, ask it to
   *"delete this project"* and *"publish this project"*. Expected: it says it cannot, rather than
   doing it. Before this change it would have done it.
2. **The capability that was kept.** In the workflow editor's Copilot, ask for a brand-new workflow
   in a project that does not exist yet. It should still call `createProject` then
   `createProjectWorkflow` and succeed — this is the path the two retained tools exist for.
3. **The converter.** Import an n8n workflow through the client's convert flow and confirm the
   result is still created. If it now fails to persist, Task 3's premise was wrong and it should be
   reverted.

## Self-review notes

**Not in scope**, and deliberately so: `project_agent`, the `workflow_editor_agent` →
`project_workflow_agent` rename, moving leaf tools behind delegates, and the asset-file read
demotion. All are in the spec and get their own plans.

**The riskiest assumption** is Task 3's — that the converter persists nothing. Step 1 exists to
falsify it before any code changes, and the task says to stop rather than proceed on doubt.
