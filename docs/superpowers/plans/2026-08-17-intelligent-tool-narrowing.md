# Intelligent Tool Narrowing — Implementation Plan (spec step 4)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Each intelligent tool does exactly one thing. `buildWorkflow` authors workflow content
into an existing workflow and loses project/workflow creation; `importWorkflow` gains a target
workflow id and authors in place on both of its paths. `ProjectAuthoringTools` is deleted.

**Prerequisites:** the catalog plan has landed (the tools are constructed in one place). The rename
plan is assumed landed — this plan uses the new names; substitute the old ones if not.

**This is a capability removal.** A caller that relied on `buildWorkflow` creating a project gets a
failure after this plan. Panels are unaffected (the workflow editor is unreachable outside a
project). AI Hub and MCP callers ARE affected — their prompts/guidance change in the SAME commits,
never after.

**Why narrow-not-pure:** `ProjectWorkflowTools`' own Javadoc records that the fully-pure form
(agent returns a definition, caller persists) was tried as `workflow_builder` and abandoned over
the JSON round-trip cost. The narrowed agent authors **in place** given a `workflowId` — it keeps
its workflow-content tools, and sheds only project/workflow lifecycle.

## Global Constraints

- Work on `0_732`; user commits in parallel — fresh commits, never amend,
  `git commit -m "..." -- <paths>`.
- Commit messages `732 <description>`.
- CE Apache 2.0 headers; EE Enterprise header + `@version ee`. Java style per CLAUDE.md.
- Log-file verification; never piped tails.
- **A prompt must never name a tool that is not registered on that agent** — every tool this plan
  removes must be removed from every prompt that names it, in the same commit.
- **A delegate's behaviour lives in its wiring, not its prompt** (the converter lesson, recorded in
  the 2026-08-13 spec): establish what each path actually persists by reading the code, not the
  prompt, before changing either.

---

### Task 1: Narrow `buildWorkflow`

**Files:**
- Modify: `server/libs/ai/ai-copilot/ai-copilot-service/.../config/CopilotConfiguration.java` —
  the `workflowEditorBuildSpringAIAgent` and `workflowEditorBuildSubAgentChatClient` beans (locate
  by bean name; they currently inject `ProjectAuthoringTools`)
- Modify: `prompt_workflow_editor_build.txt` (shared by panel agent and delegate — one prompt, two
  beans; that sharing is deliberate, do not fork it)
- Modify: `prompt_ai_hub_build.txt`, `prompt_ai_hub_ask.txt`, `prompt_project_build.txt` — the
  delegation passages
- Delete: `server/libs/automation/automation-ai/automation-ai-tool/.../ProjectAuthoringTools.java`
  + its test

- [ ] **Step 1: Establish current state.** Read both beans and the four prompts. Confirm
  `ProjectAuthoringTools` (`searchProjects`, `createProject`) is injected on both beans and that
  the BUILD prompt instructs using them for the "brand-new project" flow. If the branch has moved
  and the wiring differs, STOP and report.
- [ ] **Step 2: Remove `ProjectAuthoringTools`** from both beans (parameter, `.tools(...)` entry,
  import).
- [ ] **Step 3: Rewrite the prompt's project passage.** Replace the "search for / create a project"
  instructions with: the workflow already exists; you receive its id; if asked to create a project
  or a new workflow, answer that the caller must do that first (`createProject` /
  `createProjectWorkflow`) and then re-invoke. Do not reword unrelated passages.
- [ ] **Step 4: Update the caller prompts in the same commit.** The hub BUILD prompt's delegation
  passage gains the sequencing rule: `createProject` → `createProjectWorkflow` → `buildWorkflow`
  (the hub has the flat CRUD tools — verify by grep that `createProject` and
  `createProjectWorkflow` are registered on the hub agent before writing a prompt that names them;
  if they are not, STOP: registering them is a missing precondition, report it).
  `prompt_project_build.txt` similarly — the Projects panel has flat project+workflow tools; the
  delegation instruction becomes "create first, then delegate authoring".
- [ ] **Step 5: Delete `ProjectAuthoringTools` + test.** First prove it has no other consumer:
  `git grep -ln "ProjectAuthoringTools" -- 'server'` must list only the two beans (already edited)
  and the class + test.
- [ ] **Step 6: Verify** (module checks for ai-copilot-service, automation-ai-tool, ai-hub-service;
  log + `$?` + FAILED grep). The prompt closing check: extract backticked tool names from every
  touched prompt; each must be registered on its agent.
- [ ] **Step 7: Commit** with pathspec covering all touched trees in one commit — the prompt and
  wiring changes are one atomic behaviour change.

### Task 2: Converge `importWorkflow`

**Files (re-derive all by grep — this area moved twice recently):**
- `ConverterAgentToolCallback` (delegate input schema + description)
- `WorkflowPersistCaptureUtils` + `ProjectWorkflowTools.capturePersistedWorkflow` (the delegate
  path's persist mechanism)
- `converterBuildSpringAIAgent` / `converterBuildSubAgentChatClient(Supplier)` beans
- `prompt_converter_build.txt` (or current name — the converter prompt)
- Client: `useConverterN8nToWorkflow.ts` (calls the converter panel route directly and persists the
  returned JSON itself)

**Current asymmetry (verify, then eliminate):** the PANEL path returns a definition and the client
persists; the DELEGATE path persists internally via capture. Target: both paths take a target
`workflowId` and author in place via `updateWorkflow`.

- [ ] **Step 1: Map both paths end to end.** Read the delegate callback, the capture utils, the
  bean wiring, and the client hook. Write down (in the task report) exactly who persists what
  today. If the panel path turns out to also persist server-side, the client hook's contract is
  different from this plan's premise — STOP and report.
- [ ] **Step 2: Change the delegate contract:** input schema gains required `workflowId`; the inner
  agent authors into it (the converter client gains `ProjectWorkflowTools`-style update capability
  or routes output through `updateWorkflow` — pick the mechanism `buildWorkflow` uses after Task 1,
  for symmetry). Remove the capture wiring from the converter beans.
- [ ] **Step 3: Align the panel path.** The panel keeps returning the definition to the client
  (client-applied, same as the workflow code editor) OR switches to in-place — decide by what the
  client hook does: if `useConverterN8nToWorkflow` creates the workflow first and applies the
  definition, keep it; it already matches "caller owns creation". Only the delegate was divergent.
- [ ] **Step 4: Retire `WorkflowPersistCaptureUtils`** if and only if
  `git grep -ln "WorkflowPersistCaptureUtils\|capturePersistedWorkflow"` shows no remaining
  consumer. If something else uses it, leave it and record why in the report.
- [ ] **Step 5: Update descriptions and caller prompts** (hub prompts' import passage: create the
  target workflow, then `importWorkflow(workflowId, definition)`).
- [ ] **Step 6: Verify + commit** (same discipline as Task 1). Note in the commit message that the
  n8n import flow needs a manual end-to-end check.

### Task 3: Audit the remaining seven for scope creep

- [ ] For each remaining intelligent tool, list its inner agent's registered tool classes (from its
  contributor definition / bean) and flag anything that is lifecycle CRUD rather than the tool's
  one capability. Known-clean from prior surveys: `configureClusterElement`, `writeScript`,
  `debugWorkflowExecution` (read-only + validators). Check `authorSkill` (its `SkillsTools` carries
  10 tools — confirm they are all skill-content operations, not unrelated lifecycle),
  `buildCodeWorkflow`, `buildCustomComponent`, `buildIntegrationWorkflow`.
- [ ] Fix only clear violations (same pattern as Task 1); report borderline cases rather than
  deciding unilaterally.
- [ ] Commit per fix, with pathspec.

## Manual verification (running backend)

1. **Removed capability:** AI Hub BUILD, "create a project called X with a workflow that does Y" —
   the ROOT must call `createProject` + `createProjectWorkflow` itself, then `buildWorkflow`.
   Before this plan the delegate created the project.
2. **Kept capability:** Workflow editor panel BUILD still authors into the open workflow.
3. **Converter:** import an n8n workflow end to end from the client; then via AI Hub ("import this
   n8n JSON") — both must land the definition in a workflow the caller created.
4. **MCP:** from an external client, the `buildWorkflow` description's precondition line reads
   correctly and the create-first flow works.

## Self-review notes

- **The riskiest task is 2** — the converter has burned an assumption once already (the 2026-08-13
  spec records it). Step 1's stop-conditions exist for that reason.
- **Not in scope:** CRUD delegate unwind, `configureMcpServer`, panel exposure (none needed —
  see the spec's exclusions section).
