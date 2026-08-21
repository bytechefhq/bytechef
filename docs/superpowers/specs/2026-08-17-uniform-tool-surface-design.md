# Uniform Tool Surface — Design

**Status:** design
**Date:** 2026-08-17
**Supersedes:** Pieces 2 and 3 of `2026-08-13-uniform-delegation-design.md` (Pieces 0 and 1 are landed and stand)

## The shape

Every AI surface is a **small agent holding intelligent tools plus ordinary tools, side by side**.

```
surface agent
  ├── intelligent tools   ← one capability each, LLM-backed, non-deterministic
  └── ordinary tools      ← CRUD, deterministic, flat
```

That is the whole design. It is uniform across all three surfaces; only the *scope* of each
list varies.

| Surface | The small agent | Intelligent tools | Ordinary tools |
|---|---|---|---|
| Copilot panel | the page's `*SpringAIAgent` | the ones in the page's scope | the ones in the page's scope |
| AI Hub | `AiHubSpringAIAgent` | all | all |
| Management MCP | **the external MCP client** | all | all |

The panels are already small agents — there are 20+ `*SpringAIAgent` beans, each with its own
prompt. They are not missing an architecture; they are missing the intelligent tools.

## Why this reverses the previous spec

`2026-08-13-uniform-delegation-design.md` proposed the opposite direction: a `project_agent` CRUD
delegate (Piece 2), and moving leaf tools behind delegates (Piece 3). Both were attempts to reduce
schema pressure by hiding tool count behind agents.

That is the wrong axis. **CRUD is deterministic and should stay deterministic.** Wrapping
`createProject` in an LLM turn adds latency, cost, and a failure mode, and buys only a smaller tool
list. An agent earns its place by doing something a function call cannot — not by being a namespace.

So Pieces 2 and 3 are shelved, and the nine CRUD-only delegates that already shipped are recognised
as the same mistake, already made.

## One centralized point for the intelligent tools

This is the core of the design. Everything else follows from it.

Today each intelligent tool is constructed independently at every surface that wants it.
`ProjectWorkflowAgentToolCallback` is built in **four** places:

| Site | Surface |
|---|---|
| `ProjectAgentConfiguration:157` | Copilot panel |
| `AiHubConfiguration:1031` | AI Hub |
| `ToolCallbackContributorConfiguration:75` | Management MCP (CE) |
| `EmbeddedCopilotMcpContributorConfiguration:69` | Management MCP (embedded) |

Every other intelligent tool is built twice. So a rename is a four-site edit, adding a surface means
finding every construction site, and a tool present on one surface and absent from another is
invisible — there is no single list to compare against.

### Why a shared `List<ToolCallback>` bean is the wrong fix

The three surfaces decorate the same delegate completely differently, at two different levels:

| Surface | ChatClient-level | ToolCallback-level |
|---|---|---|
| Copilot panel | — | `RehydrateContextToolCallback` |
| AI Hub | `wrapDelegate(...)` — guardrails + workspace system prompt + session memory | `ProgressReportingToolCallback` |
| Management MCP | — | `WorkspaceScopedSubAgentToolCallback` |

The stacks are disjoint, and each is load-bearing: MCP without the workspace wrapper fails
"Workspace context unavailable"; AI Hub without `ProgressReportingToolCallback` stops narrating into
the AG-UI stream; MCP deliberately omits progress reporting because it has no stream to narrate to.
A pre-built list cannot serve all three.

### The shape: a catalog of definitions, decorated per surface

Centralize **construction and identity**; leave **decoration** to the surface.

```java
// CE, ai-copilot-tool — already a dependency of every surface
public interface IntelligentToolDefinition {
    String name();                    // "buildWorkflow"
    CopilotAgentType agentType();
    Set<ToolScope> scopes();          // which panels it belongs to; ALL for hub + MCP
    ToolCallback create(ChatClient chatClient);
}

public interface IntelligentToolContributor {   // SPI — EE modules contribute
    List<IntelligentToolDefinition> getIntelligentToolDefinitions();
}

@Component
public class IntelligentToolCatalog {
    List<ToolCallback> get(
        ToolScope scope,
        UnaryOperator<ChatClient> chatClientDecorator,
        UnaryOperator<ToolCallback> callbackDecorator);
}
```

Two decorator parameters rather than one, because the matrix above needs both levels. Each surface
then reads as one call:

```java
// Copilot panel
catalog.get(PROJECT, identity(), RehydrateContextToolCallback::new)

// AI Hub
catalog.get(ALL, chatClient -> wrapDelegate(chatClient, ...), ProgressReportingToolCallback::new)

// Management MCP
catalog.get(ALL, identity(), cb -> new WorkspaceScopedSubAgentToolCallback(cb, workspaceService))
```

### Module placement

The catalog, the definition interface and the contributor SPI go in CE **`ai-copilot-tool`**, which
already holds the delegate callback classes and `CopilotAgentType` and is already a dependency of
every surface. A CE catalog must not reference EE bean names, so EE tools arrive via the SPI — the
same pattern `McpServerToolCallbackContributor` already uses to keep the CE MCP server free of EE
imports.

| Contributor | Module | Contributes |
|---|---|---|
| `CopilotIntelligentToolContributor` | CE `ai-copilot-service` | the 7 CE tools |
| `AutomationIntelligentToolContributor` | EE `automation-ai-copilot` | `buildCustomComponent`, `buildCodeWorkflow` |
| `EmbeddedIntelligentToolContributor` | EE `embedded-ai-copilot` | `buildIntegrationWorkflow` |

Each contributor lives in the module that owns the corresponding `ChatClient` bean, so a definition
closes over its own ChatClient instead of resolving a `@Qualifier` string from another module.

### What this buys beyond tidiness

- **A rename becomes one edit** instead of four. That is what makes step "rename the eight" cheap.
- **`ObjectProvider.ifAvailable()`'s silent failure stops applying to these tools.** Today a mistyped
  `@Qualifier` no-ops: the delegate vanishes with no startup error and dies in production as "No
  ToolCallback found". Under the catalog, registration is "the contributor bean exists or it does
  not" — resolved once, in one place, testable by asserting the catalog's contents.
- **Surface parity becomes assertable.** A test can state that AI Hub and MCP resolve identical
  name sets, which is currently only checkable by reading four configuration classes side by side.
- **Adding a surface is one call**, not a per-tool wiring pass.

## Intelligent tools: exactly one thing each

Eight exist today. They are identifiable structurally, not by name: each carries
`workflowInstructionTools` + `workflowValidatorTools` (or, for the code-authoring pair, is validated
by compilation instead).

Each is renamed to describe the capability rather than the agent, and narrowed so it does that one
thing and holds no CRUD.

| Today | Becomes | Its one thing |
|---|---|---|
| `project_workflow_agent` | `buildWorkflow` | author workflow content into an existing workflow |
| `integration_workflow_agent` | `buildIntegrationWorkflow` | the same, for integration-owned workflows |
| `converter_agent` | `importWorkflow` | translate a foreign definition (n8n, Zapier) into a ByteChef one |
| `workflow_execution_agent` | `debugWorkflowExecution` | diagnose why a run failed |
| `cluster_element_agent` | `configureClusterElement` | configure one cluster element |
| `code_editor_agent` | `writeScript` | write a script-component body |
| `code_workflow_agent` | `buildCodeWorkflow` | author a code workflow |
| `custom_component_agent` | `buildCustomComponent` | author a custom component |
| `skills_agent` | `authorSkill` | write a skill |
| `mcp_agent` | `configureMcpServer` | map attached workflows into callable MCP tools |

Names are camelCase because that is what this codebase's real tools use (`createProject`,
`listAssetFiles`); `snake_case` was the delegate convention and it is the convention being retired.

### The narrowing, concretely

`buildWorkflow` today holds `ProjectAuthoringTools` (`createProject`, `searchProjects`) and
`ProjectWorkflowTools` (create/update/delete/get/list/search workflow, plus test connections). Under
"one thing", it keeps only the ability to write workflow content. The caller creates the project and
the workflow container with ordinary tools first:

```
createProject("Billing")                          ← ordinary tool
createProjectWorkflow(projectId, "Invoice sync")  ← ordinary tool
buildWorkflow(workflowId, "sync Stripe invoices to Postgres")   ← intelligent tool
```

This is also what makes `ProjectAuthoringTools` (added in Piece 1) unnecessary: it exists only
because authoring agents needed `createProject`. Under this model they do not. Piece 1 still stands
as a correctness fix for the interim — it removed publish/delete from agents that should never have
had them — but the class becomes dead once the narrowing lands, and should be deleted then rather
than kept.

**How narrow, exactly: it keeps `updateWorkflow`, not nothing.** The purest form — return a
definition and let the caller persist it — was already tried and abandoned: `ProjectWorkflowTools`'
own Javadoc records that it exists to eliminate the JSON round-trip an older `workflow_builder`
required. Passing the whole workflow definition across the boundary twice was the cost. So the
intelligent tool authors *in place* given a `workflowId`, and the caller owns creation only.

This resolves the converter's split behaviour recorded in the previous spec: the panel path returns
JSON while the delegate path persists via `WorkflowPersistCaptureUtils`. Under this model both take
a target id and author in place, and the capture machinery is no longer needed.

## Ordinary tools stay flat and stay everywhere

The nine CRUD-only delegates — `data_table_agent`, `knowledge_base_agent`, `context_store_agent`,
`asset_file_agent`, `mcp_agent`, `project_deployment_agent`, `api_collection_agent`, `task_agent`,
`ai_agent_agent` — are unwound. Their underlying `@Tool` methods are registered directly on each
surface.

This also fixes the duplicate-route defect on its own: `AiHubConfiguration` currently registers both
flat `projectTools, projectWorkflowTools` **and** `ProjectWorkflowAgentToolCallback`, giving the model
two ways to do the same thing with no guidance on which. Once the intelligent tool no longer holds
CRUD, the overlap is gone by construction — nothing needs to be hidden to remove it.

Two of the nine need care rather than a mechanical unwind:

- **`mcp_agent` is promoted, not dissolved** — see below. It is the only one of the nine that
  carries genuine judgment rather than a tool list.
- **`asset_file_agent`** and the others wrap tools that read two disjoint tool-context key families
  (`bytechef.agentTool.*` vs `bytechef.assetFile.*`). Unwinding them onto a surface whose agent
  writes only one family reproduces the Phase 3 defect exactly — two ASK agents that compiled
  cleanly and could not call any of their tools. Each unwind must confirm the surface writes the
  family its newly-flat tools read.

## `configureMcpServer` — the one new intelligent tool

`mcp_agent` is the single CRUD delegate that survives the unwind, promoted to an intelligent tool.
Its prompt (`prompt_mcp_agent.txt`, 67 lines) is not a tool list — it carries knowledge a caller
cannot derive from the tool schemas:

- the `fromAi('<name>', '<TYPE>', {description, required})` expression syntax and its eight types
  (STRING, NUMBER, INTEGER, BOOLEAN, ARRAY, OBJECT, DATE, TIME, DATE_TIME), plus `defaultValue` and
  `options`;
- that the mapping lives on the **attachment** (`mcpProjectWorkflow.parameters`), never in the
  workflow definition — editing the workflow is actively wrong for MCP-exposed workflows;
- that `toolCallable=false` means the workflow lacks a `workflow/newWorkflowCall` trigger and cannot
  be exposed at all until someone adds one in the editor;
- naming conventions (`toolName` a short snake_case verb phrase, unique per server) and what makes a
  `toolDescription` routable by a calling LLM;
- which inputs deserve a `fromAi` expression versus a literal (constants and environment-specific
  settings get literals).

**Its one thing: complete the tool mapping.** Synthesising `toolName`, `toolDescription` and
`fromAi` expressions from a workflow's `inputSchema` is genuine judgment. Everything around it is
deterministic and goes flat:

| Flat (ordinary) | Intelligent |
|---|---|
| `listMcpServers`, `createMcpServer`, `updateMcpServer`, `createMcpProject`, `cloneMcpProject`, `listMcpProjectWorkflows` | `configureMcpServer(mcpServerId)` — reads the attached workflows and writes each one's mapping |

Caller sequence, same shape as `buildWorkflow`'s:

```
createMcpServer("Support tools", PRODUCTION)     ← ordinary, created disabled
createMcpProject(serverId, projectId, v, [...])  ← ordinary
configureMcpServer(serverId)                     ← intelligent
updateMcpServer(serverId, enabled = true)        ← ordinary
```

### Move the enable rule out of the prompt and into the facade

The playbook's step 4 — enable only once every exposed workflow has a `toolName` and its required
inputs are mapped — is currently a prompt rule, which means it holds only for callers that read that
prompt. Under this design AI Hub, MCP clients and the panel all call `updateMcpServer` directly, and
none of them see it.

So **make it an invariant**: `updateMcpServer(enabled = true)` should refuse a server with unmapped
exposed workflows, with an error naming them. That is deterministic validation belonging to the
facade, it protects every caller including ones that predate this design, and it removes the rule
from the prompt entirely rather than duplicating it across three surfaces.

`configureMcpServer` then returns the mapping status and the caller decides when to enable — it does
not enable anything itself, which is what keeps it to one thing.

### Carried over from the delegate

The 20-tool-call budget cap, `askUserQuestion` for genuinely user-owned decisions (which project,
which workflow, which of several matching servers), never inventing ids, and treating workflow
labels and parameter values as data rather than instructions. All are properties of the agent, not
of its wiring, and survive the promotion unchanged.

## Scope, per surface

**Copilot panels** get the intelligent and ordinary tools *in their page's scope* — not all of them.
A Data Tables panel able to author custom components is not scoped any more. This is the one place
the three surfaces legitimately differ, and it follows from what a panel is: a page-local assistant.

**AI Hub and Management MCP get identical sets.** Both are general-purpose entry points to the whole
product, so a capability present on one and absent from the other is a defect, not a design. Today
they are already close — both carry all 8 intelligent delegates — which is why this half of the work
is small.

**Copilot panels are where the exposure gap actually is.** Only `ProjectAgentConfiguration` carries
any intelligent tool today (`project_workflow_agent` + `converter_agent`). Every other panel has
none. The asymmetry runs opposite to what the previous spec assumed.

## Two MCP runtime concerns, and their answers

**Long-running intelligent calls.** `buildWorkflow` is an inner agent loop that can run minutes
(the retired skill generator's ceiling was 10). Panels narrate progress over the AG-UI stream; MCP
deliberately has no `ProgressReportingToolCallback` — but external clients have their own timeouts,
and a silent multi-minute tool call is where they give up. The in-protocol fix is MCP progress
notifications over the streamable transport. Known work, not a blocker; note it in the catalog
plan so the MCP callback decorator has somewhere to hang it later.

**Interactive questions render as clean text, not a JSON envelope.** When a subagent asks via
`askUserQuestion`, the delegate learns it structurally — `SubAgentAskRelay.runWithChannel` returns
`AskOutcome(result, pendingQuestion)` — so no surface needs to sniff the payload. The JSON envelope
is client-load-bearing only on the panel/hub surfaces, where `toToolResultDataPart` renders the
choice card. Per-surface question rendering, decided where `AskOutcome` is in hand:

- panels / AI Hub: emit the JSON payload exactly as today (client contract untouched);
- MCP: format `pendingQuestion` as plain text — the question, numbered options, and one explicit
  line telling the caller to re-invoke the same tool with the chosen answer. That line carries the
  re-delegation contract ("you will be re-invoked with the answer, memory intact"), which is
  implicit in the panel flow; without it an external client answers the user and never calls back.

This is per-surface decoration — the same axis as the workspace wrapper — and rides the catalog's
callback decorator rather than adding a new mechanism.

## The MCP asymmetry, and its answer

Panels and AI Hub carry a system prompt, so sequencing knowledge ("create the workflow, then call
`buildWorkflow`") has somewhere to live. **MCP has no prompt** — the client is the agent, and
`ManagementMcpServerConfiguration` sets only `.serverInfo("mcp-server", "1.0.0")`.

That guidance therefore has to live in the protocol. Three seams exist, in preference order:

1. **`.instructions(...)` on the `McpServer` builder** — the MCP `initialize` result carries an
   `instructions` field for exactly this purpose. The builder supports it and ByteChef does not set
   it. This is the direct fix and it is currently free capability.
2. **Tool descriptions** — each intelligent tool's description states its precondition ("the
   workflow must already exist; create it with `createProjectWorkflow` first"). Works with every
   client, no protocol dependency.
3. **MCP resources** — already wired (`.resources(...)`, used by the MCP App viewers), so a
   capability guide can ride alongside.

Do (1) and (2). A tool whose precondition is only in a prompt is broken on MCP, and that is not
discoverable from the Java side — it fails in someone else's client.

## What actually changes

Ordered so each step is landable alone and none depends on a later one. The catalog comes first
because it turns steps 2–4 from multi-site edits into single-site ones.

1. **Build the catalog** — `IntelligentToolDefinition`, `IntelligentToolContributor`,
   `IntelligentToolCatalog` in CE `ai-copilot-tool`; three contributors; all four surfaces switched
   onto `catalog.get(...)`. Pure refactor: same tools, same names, same decoration, one owner. Prove
   it with a test asserting AI Hub and MCP resolve identical name sets.
2. **Rename the eight** to capability names — now one edit per tool, in its definition.
3. **Set `.instructions(...)`** on the MCP server builder and put preconditions in the intelligent
   tools' descriptions. Independent of everything else.
4. **Narrow the intelligent tools** to their one capability; delete `ProjectAuthoringTools`; retire
   `WorkflowPersistCaptureUtils` once the converter's two paths converge.
5. **Promote `mcp_agent` to `configureMcpServer`**, narrowed to the mapping; move its five other
   tools flat; enforce the enable rule in the facade rather than the prompt.
6. **Unwind the eight remaining CRUD delegates.** Least urgent; can be deferred indefinitely.

### Correction to the earlier scoping estimate

Writing the per-panel scopes out changed step 4's size. Seven panels — Data Tables, Knowledge Base,
Context Store, Asset Files, Project Deployments, API Collections, AI Agents — turn out to have **no
intelligent tool in scope at all**, because no intelligent capability exists for those domains and
inventing one would be the mistake this design rejects. They are already correctly shaped: a small
agent over flat CRUD.

And the editor panels do not get intelligent tools either, for the opposite reason: an editor panel
**is** its capability. The workflow editor's BUILD agent authors workflows itself with its flat
tools — registering `buildWorkflow` on it would be self-delegation, a second LLM hop to do what the
agent already does inline. The same holds for the code editor / cluster element / skills / code
workflow / custom component / execution panels.

Panel exposure therefore reduces to exactly two panels:

- **Projects** — `buildWorkflow` + `importWorkflow`, which `ProjectAgentConfiguration` already
  registers today; the catalog switch-over covers it with no new registration.
- **MCP Servers** — `configureMcpServer`, added when the promotion lands (step 5).

The phase 1–5 listing-page work needs no rework, and there is no separate "expose on panels" step
left. `configureMcpServer` is the single place a genuinely new intelligent tool is warranted:
exposing an MCP server has a real two-step precondition (attach workflows *and* complete the
`fromAi` mapping, or it is not servable) and that knowledge is currently trapped in
`prompt_mcp_agent.txt`.

## Surfaces deliberately excluded, and existing drift

**Workflow Code Editor** (`workflow_code_editor_ask` / `_build`) is a real panel and gets **no**
intelligent tool. Its beans inject `ReadProjectWorkflowTools` — read-only — and the client applies
the returned definition via `resolveAutoApplyDefinition`. It is the same capability as
`buildWorkflow` delivered through a different UI (code sheet rather than canvas), so giving it a
delegate would recreate the duplicate-route problem the narrowing removes.

**`CopilotSpringAIAgent` is abstract** — a base class the concrete panel agents extend, not a
surface. It appears in a `class .*SpringAIAgent` grep and is not a registration site.

**`CopilotAgentType` has drifted in both directions** and must not be trusted as the inventory:

- `JSON_SCHEMA_BUILDER_AGENT` and `SAMPLE_OUTPUT_AGENT` are declared with zero implementations,
  registrations or prompt references. Dead constants; delete them with the catalog work.
- `workflow_code_editor` has working beans and **no enum entry at all**. Panel routing does not
  need one — the beans derive their id as `Source.X.name() + "_" + Mode.Y.name()` — but
  `AgentTypeRegistry` uses these keys to purge session memory, and a missing key leaves sessions
  nothing ever deletes.

Neither direction of drift produces a compile error. This is a second argument for the catalog: one
enumerable list, assertable in a test, rather than an enum that silently no longer matches reality.

## Risks

- **Renaming breaks MCP clients.** Unavoidable; the question is only whether it costs one break or
  two. There is no deprecation seam for tool names.
- **Narrowing is a capability removal.** `buildWorkflow` currently *can* create a project. After
  step 3 a caller that relied on that gets a failure instead. Panels are unaffected (the workflow
  editor is unreachable outside a project), but AI Hub and MCP callers are — their prompts and
  guidance must be updated in the same change, not after it.
- **A prompt naming an unregistered tool is a runtime death, not a compile error.** Every step here
  moves tools between agents. `ObjectProvider.ifAvailable()` swallows a missing bean, so a mistyped
  qualifier ships silently and fails in production as "No ToolCallback found".
- **Step 5 is the largest behavioural surface** and the least urgent. It can be deferred
  indefinitely without blocking 1–4; the delegates work, they are merely the wrong shape.

## Explicitly not doing

- `project_agent` (previous spec, Piece 2) — a CRUD delegate, which is the thing this design rejects.
- Moving leaf tools behind delegates (Piece 3) — same reason; its motivating defect is fixed by the
  narrowing instead.
- Splitting AI Hub and MCP tool sets. They are deliberately identical.
- Touching generative one-shots (`research`, `data_analyst`, `image_generator`, `slide_builder`).
  They remain AI-Hub-only; they are not capabilities of the automation product.
