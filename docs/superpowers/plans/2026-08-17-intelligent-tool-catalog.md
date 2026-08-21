# Intelligent Tool Catalog — Implementation Plan (spec step 1)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** One place constructs every intelligent delegate tool; the four per-surface construction
sites become `catalog.get(...)` calls. Pure refactor — same tools, same names, same decoration.

**Architecture:** `IntelligentToolDefinition` + `IntelligentToolContributor` SPI +
`IntelligentToolCatalog` in CE `ai-copilot-tool`. Contributors live in the module owning each
delegate's `ChatClient` bean, closing over it directly — no cross-module `@Qualifier` strings.
Decoration stays per-surface via two decorator parameters (ChatClient-level and ToolCallback-level),
because the three surfaces' stacks are disjoint and each is load-bearing.

**Tech Stack:** Java 25 / Spring Boot 4, Spring AI, JUnit 5 + Mockito + AssertJ.

**Spec:** `docs/superpowers/specs/2026-08-17-uniform-tool-surface-design.md` — read its
"One centralized point for the intelligent tools" section first; the decoration matrix there is the
requirements table for the decorator signatures.

## Global Constraints

- Work on `0_732`. **The user commits to this branch in parallel** — always fresh commits, never
  amend, and always `git commit -m "..." -- <paths>` (message flag BEFORE the `--` pathspec).
- Commit messages: `732 <description>` (server), `732 client - <description>` (client). Never invent
  ticket numbers.
- CE files (`server/libs/**`) carry the Apache 2.0 header copied from a neighbouring file. EE files
  (`server/ee/**`) carry the ByteChef Enterprise header AND a `@version ee` Javadoc tag.
- Java style: one blank line before control statements; one blank line between a variable
  modification and its next use; no trailing blank line before a closing brace; descriptive names;
  test method names camelCase with NO underscores; no `TODO:` comments; no empty blocks.
- Verify with `./gradlew spotlessApply`, then module-scoped compile/check redirected to a log with
  `$?` echoed on its own line, then grep the log for `^> Task .* FAILED`. Never trust a piped tail.
- **`ObjectProvider.ifAvailable()` swallows missing beans.** Every registration this plan moves must
  be re-verified by the parity test, not by eyeballing — that silent failure mode is the single
  biggest risk of this refactor.
- **A prompt must never name a tool that is not registered on that agent.** This plan changes no
  tool names, so no prompt edits — if you find yourself editing a prompt, stop; you have drifted
  out of scope.

## The evidence this plan rests on

Re-derive before implementing; the branch moves.

**The four construction sites for `ProjectWorkflowAgentToolCallback` (every other intelligent
delegate has two):**

```bash
git grep -n "new \(ProjectWorkflowAgent\|ClusterElementAgent\|CodeEditorAgent\|SkillsAgent\|WorkflowExecutionAgent\|CodeWorkflowAgent\|CustomComponentAgent\|IntegrationWorkflowAgent\|Converter\)ToolCallback" -- 'server/**/*.java' | grep -v test
```

Expected sites: `ProjectAgentConfiguration` (panel), `AiHubConfiguration` (hub),
`ToolCallbackContributorConfiguration` (CE MCP), `AutomationCopilotMcpContributorConfiguration`
(EE MCP), `EmbeddedCopilotMcpContributorConfiguration` (embedded MCP).

**The decoration matrix (empirically derived; re-verify):**

| Surface | ChatClient-level | ToolCallback-level |
|---|---|---|
| Copilot panel (`ProjectAgentConfiguration`) | — | `RehydrateContextToolCallback` |
| AI Hub (`AiHubConfiguration`) | `wrapDelegate(chatClient, agentTypeKey, …)` — guardrails + workspace prompt + session memory, keyed per agent type | `ProgressReportingToolCallback` |
| Management MCP (all three contributor configs) | — | `WorkspaceScopedSubAgentToolCallback` |

The hub's `wrapDelegate` needs the **agent type key per delegate** (session memory keys on
`<parentThreadId>:<agentTypeKey>`), which is why both decorators receive the definition, not just
the raw object.

**The nine intelligent delegates and their owning modules:**

| Tool name today | ChatClient bean | Module |
|---|---|---|
| `project_workflow_agent` | `workflowEditorBuildSubAgentChatClient` | CE `ai-copilot-service` |
| `converter_agent` | `converterBuildSubAgentChatClientSupplier` (Supplier!) | CE `ai-copilot-service` |
| `cluster_element_agent` | `clusterElement*SubAgentChatClient` | CE `ai-copilot-service` |
| `code_editor_agent` | `codeEditor*SubAgentChatClient` | CE `ai-copilot-service` |
| `skills_agent` | `skills*SubAgentChatClient` | CE `ai-copilot-service` |
| `workflow_execution_agent` | `workflowExecution*SubAgentChatClient` | CE `ai-copilot-service` |
| `code_workflow_agent` | `codeWorkflow*SubAgentChatClient` | EE `automation-ai-copilot` |
| `custom_component_agent` | `customComponent*SubAgentChatClient` | EE `automation-ai-copilot` |
| `integration_workflow_agent` | embedded workflow-editor client | EE `embedded-ai-copilot` |

Note the ask/build split: MCP and hub take BUILD clients; check each site for which client it
resolves today and preserve it exactly. The converter is constructed from a **Supplier** at one
site — the definition interface must accommodate that (resolve lazily inside `create`).

**Dead enum constants to delete while here:** `JSON_SCHEMA_BUILDER_AGENT` and `SAMPLE_OUTPUT_AGENT`
in `CopilotAgentType` — zero implementations, registrations, or prompt references
(`git grep -n "json_schema_builder_agent\|sample_output_agent"` must return only the enum lines
before you delete them; if it returns more, STOP and report).

---

### Task 1: The catalog types

**Files:**
- Create: `server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/catalog/IntelligentToolScope.java`
- Create: `.../catalog/IntelligentToolDefinition.java`
- Create: `.../catalog/IntelligentToolContributor.java`
- Create: `.../catalog/IntelligentToolCatalog.java`
- Test: `server/libs/ai/ai-copilot/ai-copilot-tool/src/test/java/com/bytechef/ai/copilot/tool/catalog/IntelligentToolCatalogTest.java`

**Interfaces (produces — later tasks and plans depend on these exact shapes):**

```java
public enum IntelligentToolScope {
    PROJECT, MCP_SERVER
}

public interface IntelligentToolDefinition {

    String name();                              // the tool name, e.g. "project_workflow_agent"

    String agentTypeKey();                      // registered AgentTypeRegistry key, for session memory

    Set<IntelligentToolScope> panelScopes();    // which PANELS get it; hub and MCP always get all

    ToolCallback create(ChatClient chatClient); // build the callback over the (possibly decorated) client

    ChatClient chatClient();                    // the raw client the contributor closed over
}

public interface IntelligentToolContributor {

    List<IntelligentToolDefinition> getIntelligentToolDefinitions();
}

@Component
public class IntelligentToolCatalog {

    public IntelligentToolCatalog(List<IntelligentToolContributor> contributors) { ... }

    /** All definitions — for the general-purpose surfaces (AI Hub, management MCP) and for tests. */
    public List<ToolCallback> getAll(
        BiFunction<ChatClient, IntelligentToolDefinition, ChatClient> chatClientDecorator,
        BiFunction<ToolCallback, IntelligentToolDefinition, ToolCallback> callbackDecorator) { ... }

    /** Definitions whose panelScopes contain the given scope — for Copilot panels. */
    public List<ToolCallback> getForPanel(
        IntelligentToolScope scope,
        BiFunction<ChatClient, IntelligentToolDefinition, ChatClient> chatClientDecorator,
        BiFunction<ToolCallback, IntelligentToolDefinition, ToolCallback> callbackDecorator) { ... }

    /** Definition names — for the parity test and for prompt/registration audits. */
    public List<String> getNames() { ... }
}
```

Resolution order inside both getters: `chatClientDecorator.apply(definition.chatClient(), definition)`
→ `definition.create(decoratedClient)` → `callbackDecorator.apply(callback, definition)`. Identity
decorators are `(client, definition) -> client` / `(callback, definition) -> callback`.

- [ ] **Step 1: Write the failing test** — a fake contributor with two definitions (one carrying
  `panelScopes = Set.of(PROJECT)`, one empty); assert `getAll` with identity decorators returns both
  callbacks, `getForPanel(PROJECT, ...)` returns one, `getNames()` returns both names, and that the
  decorators are invoked with the matching definition (capture with a recording BiFunction).
- [ ] **Step 2: Run it to verify it fails** (compilation failure — classes do not exist):
  `./gradlew :server:libs:ai:ai-copilot:ai-copilot-tool:test --tests '*IntelligentToolCatalogTest' > /tmp/cat1.log 2>&1; echo "exit=$?"`
- [ ] **Step 3: Implement the four types.** Javadoc on the catalog must state the design rule: the
  catalog owns construction and identity; decoration belongs to the surface; nothing outside a
  contributor may construct an intelligent delegate callback.
- [ ] **Step 4: Verify:** same test command, expect PASS; then
  `./gradlew spotlessApply > /dev/null 2>&1; ./gradlew :server:libs:ai:ai-copilot:ai-copilot-tool:check > /tmp/cat1c.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/cat1c.log`
- [ ] **Step 5: Commit:**
  `git commit -m "732 Add the intelligent tool catalog types" -- server/libs/ai/ai-copilot/ai-copilot-tool/src`

---

### Task 2: CE contributor + switch the CE surfaces

**Files:**
- Create: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/CopilotIntelligentToolContributor.java`
- Modify: `.../config/ToolCallbackContributorConfiguration.java` (CE MCP contributor)
- Modify: `.../config/ProjectAgentConfiguration.java` (Projects panel)
- Test: `.../config/CopilotIntelligentToolContributorTest.java`

**Interfaces:**
- Consumes: Task 1's types; the six CE `*SubAgentChatClient` beans (resolve via `ObjectProvider` in
  the contributor's `@Bean` method, same as the sites do today — the contributor is itself a
  `@Configuration`-declared bean so a missing client simply yields a shorter definition list, which
  the parity test in Task 5 will catch if wrong).
- Produces: a contributor bean carrying up to six definitions: `project_workflow_agent`
  (panelScopes `{PROJECT}`), `converter_agent` (panelScopes `{PROJECT}`), `cluster_element_agent`,
  `code_editor_agent`, `skills_agent`, `workflow_execution_agent` (all panelScopes empty).

- [ ] **Step 1: Read the two CE sites in full** and record, per delegate: which ChatClient bean it
  resolves, ask or build variant, and the exact wrapper arguments. The contributor must reproduce
  the same client choice per definition.
- [ ] **Step 2: Write the contributor** — one definition per available client;
  `agentTypeKey()` returns the same key the delegate registers today (check
  `CopilotAgentType` — e.g. `PROJECT_WORKFLOW_AGENT.key()`). The converter definition resolves its
  Supplier inside `create`, preserving today's lazy semantics.
- [ ] **Step 3: Switch `ToolCallbackContributorConfiguration`** — replace its five `new
  *AgentToolCallback(...)` constructions with
  `catalog.getForPanel`-equivalent... NO: MCP takes ALL. Use
  `catalog.getAll((client, def) -> client, (callback, def) -> new WorkspaceScopedSubAgentToolCallback(callback, workspaceService))`
  — but ONLY for the delegates this config contributed before. **Careful:** at this point in the
  plan the EE contributors don't exist yet, so `getAll` returns only CE definitions — which is
  exactly the set this CE config contributed. Confirm that equality by listing both sets before
  switching; if the CE config today contributes a delegate the contributor doesn't (or vice versa),
  STOP and report.
- [ ] **Step 4: Switch `ProjectAgentConfiguration`** — replace its two constructions with
  `catalog.getForPanel(IntelligentToolScope.PROJECT, (client, def) -> client, (callback, def) -> /* today's panel decoration, read from the current code — RehydrateContextToolCallback if present on these two */)`.
  Read the current code first: if the panel site today registers the delegates bare (no rehydrate
  wrapper on these two specifically), keep them bare. Preserve, do not improve.
- [ ] **Step 5: Verify:**
  `./gradlew spotlessApply > /dev/null 2>&1; ./gradlew :server:libs:ai:ai-copilot:ai-copilot-service:check > /tmp/cat2.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/cat2.log`
- [ ] **Step 6: Commit:**
  `git commit -m "732 Add the CE intelligent tool contributor and switch the CE surfaces onto the catalog" -- server/libs/ai/ai-copilot/ai-copilot-service/src`

---

### Task 3: EE automation contributor + switch its MCP site

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-copilot/src/main/java/com/bytechef/ee/automation/ai/copilot/config/AutomationIntelligentToolContributor.java`
- Modify: `.../config/AutomationCopilotMcpContributorConfiguration.java`

**Interfaces:**
- Produces: definitions for `custom_component_agent` and `code_workflow_agent` (panelScopes empty).

- [ ] **Step 1:** Same shape as Task 2: read the site, write the contributor over the module's own
  ChatClient beans, switch the MCP contributor config to consume the catalog **filtered to this
  module's two names** — the EE MCP config must contribute only its own two, or they double-register
  beside Task 2's contribution. Filter by name against a local constant list.
- [ ] **Step 2: Verify:** module-scoped check, log + `$?` + FAILED grep, as above.
- [ ] **Step 3: Commit** with pathspec on `server/ee/libs/automation/automation-ai/automation-ai-copilot/src`.

---

### Task 4: Embedded contributor + switch its MCP site

**Files:**
- Create: `server/ee/libs/embedded/embedded-ai/embedded-ai-copilot/src/main/java/com/bytechef/ee/embedded/ai/copilot/config/EmbeddedIntelligentToolContributor.java`
- Modify: `.../config/EmbeddedCopilotMcpContributorConfiguration.java`

Same shape as Task 3 for `integration_workflow_agent`. Note this delegate reuses the shared
`ProjectWorkflowAgentToolCallback` class via its variant constructor — the definition's `create`
calls that constructor with the same arguments the site passes today. Verify, commit with pathspec.

---

### Task 5: Switch AI Hub + the parity test

**Files:**
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/config/AiHubConfiguration.java`
- Test: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/config/IntelligentToolSurfaceParityTest.java`

- [ ] **Step 1: Switch the hub's intelligent-delegate block** (the ~lines 958–1069 constructions;
  locate by class name, not line) to
  `catalog.getAll((client, def) -> wrapDelegate(client, def.agentTypeKey(), ...), (callback, def) -> new ProgressReportingToolCallback(callback, ...))`,
  passing the same wrapDelegate arguments the site passes today. **Only the intelligent delegates
  move** — the hub's CRUD delegates (data_table_agent etc.), research/data_analyst/one-shots, and
  pinned tools stay exactly as they are; they are later plans' business.
- [ ] **Step 2: Write the parity test** — assemble the catalog with the CE + both EE contributors
  (mock the ChatClients) and assert `getNames()` equals the nine expected names; then assert the
  name set the hub registers equals the name set the MCP contributor configs register. This test is
  the plan's finish line: it is what makes surface parity assertable forever after.
- [ ] **Step 3: Delete the dead enum constants** `JSON_SCHEMA_BUILDER_AGENT` and
  `SAMPLE_OUTPUT_AGENT` from `CopilotAgentType` after the grep in the evidence section confirms
  they are referenced nowhere else.
- [ ] **Step 4: Full verify:**
  `./gradlew spotlessApply > /dev/null 2>&1; ./gradlew compileJava compileTestJava --continue > /tmp/cat5.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/cat5.log`
  then module checks for ai-hub-service, ai-copilot-tool, ai-copilot-service.
- [ ] **Step 5: Commit** hub switch and enum deletion as separate commits, each with pathspec.

---

## Manual verification (requires a running backend)

1. Projects panel BUILD: ask for a workflow — the panel still delegates to
   `project_workflow_agent` and succeeds.
2. AI Hub BUILD: same request — delegation works, progress narration still appears (proves
   `ProgressReportingToolCallback` survived the switch).
3. MCP: list the management server's tools — all nine intelligent delegates present, workspace
   scoping works (a call without workspace context returns the workspace-required error, not
   "Workspace context unavailable").

## Self-review notes

- **The riskiest step is Task 2 Step 3** — the moment `getAll` is used while only CE definitions
  exist. The set-equality check before switching is the guard.
- **Not in scope:** renames, narrowing, prompt edits, CRUD delegates, `configureMcpServer`,
  question rendering. Each has its own plan.
