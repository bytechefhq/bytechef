# Uniform delegation — one delegate per domain on AI Hub and MCP

**Status:** design, not yet planned or implemented
**Date:** 2026-08-13
**Supersedes:** an earlier `project_manager` draft (committed as `f2d9bf4ff76`, removed in the same
commit as this file). That draft proposed a narrower lifecycle-vs-content split and used the
pre-rename `project_manager` name; both are obsolete.
**Verified against:** `0_732` at `f2d9bf4ff76`, plus the in-flight `_manager` → `_agent` rename

## The principle

Every domain reachable from the AI Hub root agent and from the management MCP server is reached
through **exactly one delegate tool**. The root carries no per-domain leaf tools.

Today the codebase is split between two shapes. Nine listing-page domains follow the delegate shape
(`data_table_agent`, `knowledge_base_agent`, …). Projects does not — its 15 leaf tools sit directly
on the root. Asset files are a hybrid: writes are delegated, reads are pinned. This design makes all
of them uniform.

## Three separable pieces

They are listed smallest-first and are independently landable. Piece 1 is a genuine defect and is
worth doing even if 2 and 3 are dropped.

---

### Piece 0 — one agent per tool class, and name agents for their role

This supersedes the "lifecycle vs content" split the rest of this spec originally proposed. The
cleaner boundary is **one tool class, one agent**:

| Agent | Owns | Count |
|---|---|---|
| `project_agent` (new) | `ProjectTools` — project lifecycle | 8 |
| `project_workflow_agent` (renamed from `workflow_editor_agent`) | `ProjectWorkflowTools`, plus the task / script / validator / simulation tools it already has | 7 + authoring |

**`ProjectWorkflowTools` is not split.** It moves as a unit. That closes two questions this spec
originally left open: `updateWorkflow` and `saveWorkflowTestConnection` both simply stay with
`project_workflow_agent`, because there is no longer a boundary running through the class.

**The rename is the point, not decoration.** `workflow_editor_agent` already holds all of
`ProjectWorkflowTools` — the rename makes the name describe what the agent *is for* rather than
where it came from. "Editor" names its origin (the workflow-editor panel), but a delegate is called
by other agents, which never see that panel.

**The embedded twin renames too**, to `integration_workflow_agent` (from
`workflow_editor_embedded_agent`). Embedded workflows belong to *integrations*, not projects — its
own prompt describes "code workflow integrations" — so `project_workflow_embedded_agent` would be
actively wrong. Renaming to `integration_workflow_agent` keeps the meaningful pattern
(`<owner>_workflow_agent`) rather than the accidental one (the word "embedded").

Both renames are further breaking changes to published MCP tool names, on top of the
`_manager` → `_agent` rename. They should land in the same release, not trickle.

### Piece 1 — split `ProjectTools` by blast radius

**The defect.** `ProjectTools` is an all-or-nothing bundle of eight operations:
`createProject`, `updateProject`, `deleteProject`, `getProject`, `listProjects`, `searchProjects`,
`publishProject`, `getProjectStatus`.

Four agents inject the whole bundle:

- `workflowEditorBuildSpringAIAgent` (panel)
- `workflowEditorBuildSubAgentChatClient` (delegate)
- `converterBuildSpringAIAgent` (panel)
- `converterBuildSubAgentChatClient` (delegate)

So the workflow editor and the format converter can both **delete and publish projects**, which
neither name nor description advertises. `publishProject` is the sharpest case: publishing is a
deliberate release action a user should trigger, not a side effect available to a workflow-editing
turn.

**Why this is not simply "remove `projectTools` from those agents".** The converter turns an
external format (e.g. an n8n workflow) into a ByteChef one, and plausibly needs `createProject` and
`createProjectWorkflow` to land its output. Blanket removal would break the converter's ability to
deliver a result. The problem is granularity, not presence.

**The change.** Introduce a narrower write class — working name `ProjectAuthoringTools` — carrying
only what an authoring agent needs to land its output (`createProject`, `createProjectWorkflow`),
alongside the existing read-only `ReadProjectTools`. The full `ProjectTools` bundle is then used
only by the agent that owns project lifecycle.

**Before implementing, verify empirically rather than by reasoning from names:** read the four
agents' prompts and confirm which project operations each is actually instructed to perform. The
tool split should follow observed intent, not our guess at it.

---

### Piece 2 — introduce `project_agent`

A delegate wrapping the Project slice's BUILD chat client, contributed to:

- the AI Hub root agent (via `AiHubConfiguration`, alongside the other delegates), and
- the management MCP server (via the CE copilot contributor).

**On MCP it must be wrapped in `WorkspaceScopedSubAgentToolCallback`.** An unwrapped delegate
forwards an empty tool context and every inner tool fails with "Workspace context unavailable" —
this exact defect shipped once already and was fixed for all the other delegates.

Follow the `asset_file_agent` shape: one tool-callbacks factory feeding the panel agents, the AI Hub
delegate, and the MCP contribution, so the three cannot drift apart.

Naming: `project_agent`, consistent with the `_manager` → `_agent` rename. The superseded spec's
`project_manager` name is obsolete.

#### The root sequences; delegates stay narrow

Once `project_agent` exists, the AI Hub root holds it and `project_workflow_agent` as **siblings**.
"Create a project called Billing with a workflow that syncs Stripe invoices" is then two calls from
the root — `project_agent` to create the container, then `project_workflow_agent` to author the
content — with the root holding the ids between them.

The alternative, `project_workflow_agent` calling `project_agent` itself, is delegate-to-delegate
nesting: three levels and two prose handoffs instead of two and two. It is also worse placed to
decide — the root received the whole request, while the delegate would only have received "add a
workflow that syncs Stripe invoices" and would have to infer the project does not exist.

**The rule:** give a delegate enough tools to be self-sufficient only when nothing above it can
sequence. Root above it → root sequences → delegate stays narrow. Delegate *is* the top (a panel
agent) → it must be self-sufficient. This is why the same domain legitimately has different tool
sets on the panel and the delegate despite sharing a tool factory.

**This requires prompt work, not just wiring.** The AI Hub BUILD prompt must state that creating a
project and creating a workflow inside it are two delegate calls in that order. Without it the model
will hand the whole request to one delegate and get a partial result.

#### Consequence: `ProjectAuthoringTools` comes off the workflow-editor beans

`ProjectAuthoringTools` (`searchProjects` + `createProject`) was added in Piece 1 so authoring agents
could land their output without inheriting `publishProject`/`deleteProject`. Piece 2 makes it
unnecessary on both workflow-editor beans, for two *different* reasons:

- **The panel agent** (`workflowEditorBuildSpringAIAgent`) is only reachable from inside a project's
  workflow editor, so it can never be asked to create a project. The tools are already dead weight
  today — harmless, but two schemas per turn that can never fire.
- **The delegate** (`workflowEditorBuildSubAgentChatClient`) is reachable from AI Hub with no project
  context, which is why it needs them *now* — but once the root sequences, it does not.

That leaves the converter as `ProjectAuthoringTools`' only consumer.

**Watch the shared prompt.** Both beans load the same resource
(`promptWorkflowEditorBuildResource`), so its instructions are the *union* of what either might
need — which is why it documents `createProject` at all. Removing the tools without editing that
prompt reproduces the "No ToolCallback found" failure. This shared-prompt effect is also what made
an earlier reading of this spec wrong: the prompt describes the union, and only the wiring tells you
the split.

---

### Piece 3 — move the leaf tools behind their delegates

**Projects.** All 15 leaf tools come off the AI Hub root; the root keeps only `project_agent`.
Likewise on the management MCP server, where `ManagementMcpServerConfiguration` injects
`ProjectTools` / `ProjectWorkflowTools` directly today.

**Asset files.** `listAssetFiles` and `getAssetFileContent` come off both the ASK and BUILD roots;
the root keeps only `asset_file_agent`. This completes what phase 4 left as a hybrid.

#### The justification is duplicate routes, not schema size

The root currently carries **both** `projectWorkflowTools` (flat, `AiHubConfiguration:718`) **and**
`project_workflow_agent` (delegate, `:1031`). It can therefore create or update a workflow two
different ways, with nothing to tell the model which to prefer. That ambiguity is live today and is
exactly what makes a model pick badly — it is the defect this piece removes.

Two arguments previously advanced *against* this piece do not survive scrutiny and are recorded here
so they are not re-raised:

- *"It adds a hop to AI Hub's core flow."* It does not. Authoring workflow content — the core flow —
  already goes through `project_workflow_agent`. Only project *creation* gains a hop, and that is
  comparatively rare.
- *"`ProjectWorkflowTools` exists to avoid round-trips, so re-adding one is a regression."* That
  comment refers to eliminating the JSON serialise/deserialise churn an older `workflow_builder`
  required. It is not about delegate hops and does not bear on this decision.

**The governing rule: the root is an orchestrator, not a worker.** One decision per domain — which
delegate owns it — rather than a per-domain judgement about which tools are "hot enough" to stay
flat. A rule with convenience exceptions decays: the next person adds a domain, copies the nearest
example, and the arrangement drifts.

**The cost this concentrates in one place:** with the root as a pure orchestrator, sequencing lives
*entirely* in the root's prompt. Nothing fails loudly if the model hands a whole multi-step request
to a single delegate. That makes the root prompt the highest-value thing to get right and the first
thing to verify against a live backend — it is the only place the orchestration exists.

#### One thing that must NOT move

`ViewerToolMcpContributorConfiguration` registers `getAssetFileContent` as a **flat MCP tool** and
attaches an `McpAppUiDescriptor` pointing at `McpAppResources.FILE_VIEWER_URI`, with a `shapeFile`
result-shaper that maps the tool's `{id, name, mimeType, content}` output into what the viewer
renders.

That descriptor is keyed on the tool name being **directly contributed**. Put it behind the delegate
and the in-chat file viewer silently stops working, because a delegate returns a synthesised prose
report rather than the structured shape the viewer parses. The same applies to `queryDataTable` →
data-table viewer in the same class.

**So: the MCP viewer contributor keeps its flat `getAssetFileContent`. Only the AI Hub root's copy
is demoted.** This is the general rule — a tool with a non-model consumer (a UI layer that parses
its structured output) must stay directly callable by that consumer.

#### Prompts move in the same commit

The AI Hub prompts name these tools: `listAssetFiles` 5 times and `getAssetFileContent` 3 times
across `prompt_ai_hub_ask.txt` and `prompt_ai_hub_build.txt`, plus the project tools. A prompt
naming a tool that is no longer registered makes the model call it and the turn dies with "No
ToolCallback found" — it compiles perfectly and fails only at runtime.

## The cost this design accepts

Delegation is not free, and the trade should be recorded rather than discovered later.

**Grounding gets slower.** The root agent currently uses read results to decide *whether* to
delegate — it can check a file or project exists before handing off. Behind a delegate it must
delegate speculatively and let the sub-agent discover there is nothing to act on, turning a
one-line answer into a full model round-trip.

**Multi-step authoring gains hops.** "Create a project with a workflow that syncs X" becomes
root → `project_agent` (create project) → root → `project_agent` (create empty workflow) →
root → `workflow_editor_agent` (author it). Three natural-language handoffs, each a place where
detail can be lost.

**There is a documented precedent against re-adding hops here.** `AiHubConfiguration`'s Javadoc
notes that `ProjectWorkflowTools` exists specifically to eliminate the JSON round-trip that an older
`workflow_builder` required. Piece 3 partially re-adds a hop that someone deliberately removed.

**What is bought:** a uniform mental model, a much smaller per-turn schema surface on the root, and
— the real prize — an end to specialists silently owning tools outside their advertised scope.

## Sequencing

Land Piece 1 first and independently. It fixes a real defect, is small, and removes the overlap that
currently muddies Pieces 2 and 3. Then Piece 2. Then Piece 3, which is the only one that changes
behaviour users will notice.

## Open questions

1. **The converter is the remaining hard case — and its two paths behave oppositely.** Established
   empirically while implementing Piece 1, which stopped here rather than proceeding on a wrong
   premise.

   The **panel** path (`converterBuildSpringAIAgent`) does return JSON: the client's
   `useConverterN8nToWorkflow.ts` parses the result into local editor state. It persists nothing, and
   the prompt's "output MUST be valid JSON with no explanations" describes exactly this contract.

   The **delegate** path (`converterBuildSubAgentChatClient` / `...Supplier`, wrapped by
   `ConverterAgentToolCallback` and registered as `converter_agent`) **does persist its own output**,
   and there is purpose-built machinery for it: `WorkflowPersistCaptureUtils` plus
   `ProjectWorkflowTools.capturePersistedWorkflow`, whose job is to let the subagent save the
   workflow via `createProjectWorkflow`/`updateWorkflow` and report the real ids back to its caller.
   Removing `ProjectWorkflowTools` from that bean would break it.

   **The lesson for the rest of this design:** the prompt describes the panel contract only. A
   delegate's behaviour lives in its *wiring*, not its prompt — so "what does the prompt say it
   does" is not sufficient evidence for any of the remaining pieces.

   Note also a **third** converter-tool-carrying bean, `converterBuildSubAgentChatClientSupplier`,
   which the "four affected beans" table above omits.

   Three ways out, none obviously best:
   - give the delegate a narrow authoring tool set — keeps the granularity split alive for this one
     consumer, and is the smallest change now that `ProjectAuthoringTools` exists;
   - have it call `project_agent` — but delegate-to-delegate nesting is what this design is trying
     to avoid;
   - change its contract so it *returns* a definition and the caller persists it, matching the panel
     path — cleanest separation, but it means unpicking `WorkflowPersistCaptureUtils`, which exists
     precisely to support the current behaviour.
2. After Piece 3, does anything still need `ReadProjectTools` / `ReadProjectWorkflowTools` on the
   ASK root, or do those come off too?
3. Do the panel agents change at all, or is this AI-Hub-and-MCP-only? (Assumed the latter — but note
   `project_build` must keep `project_workflow_agent` in its tool list, because the panel *is* the
   top level there and has no root above it to reach that delegate. `project_agent` should NOT carry
   it, since the AI Hub root holds both as siblings. Same factory, different delegate lists.)
4. Does the `_agent` suffix still earn its place once every delegate has it? It distinguishes a
   delegate from the panel agents (`_ask`/`_build`) and the coarse fallback (bare name), so probably
   yes — but worth a deliberate answer rather than inertia.
