# When an intelligent delegate earns its hop

**Status:** accepted. No delegate dissolves as a result of this spec; it records the criterion and
one follow-up gap it surfaced.
**Date:** 2026-08-18
**Ticket:** 732

## Why this exists

Ticket 732's CRUD-delegate unwind dissolved nine one-shot subagent delegates in a row — `task_agent`,
`api_collection_agent`, `project_deployment_agent`, `asset_file_agent`, `data_table_agent`,
`knowledge_base_agent`, `context_store_agent`, `ai_agent_agent`, and `mcp_agent` (promoted into the
catalog as `configureMcpServer`). Each commit argued its own case in its own words. The shape of the
argument was the same every time, but it was never written down, so the natural next question —
"should the nine *intelligent* delegates go the same way?" — had no answer except re-deriving the
whole thing.

This spec writes the criterion down once, applies it to the nine, and records the answer: **no.**

The nine intelligent delegates are `buildWorkflow`, `authorSkill`, `configureClusterElement`,
`writeScript`, `debugWorkflowExecution`, `importWorkflow`, `buildCustomComponent`,
`buildCodeWorkflow`, and `configureMcpServer` (`AiHubConfiguration.INTELLIGENT_TOOL_NAMES`; the
tenth catalog name, `buildIntegrationWorkflow`, is a deliberate hub/MCP parity gap).

## What a delegate costs

A delegate is an extra LLM round trip wrapped in a tool. It costs:

- **A round trip** — latency and tokens for a second model call.
- **Context at the boundary** — only the request string crosses. The specialist cannot see the
  parent's turn.
- **Suspend/resume** — a one-shot subagent cannot pause mid-run and resume where it left off.
  Streaming and suspend/resume contracts must stay on the main agent.

Against that, it buys exactly one thing: one tool schema on the parent instead of N.

## The criterion

A delegate earns its hop if it buys at least one of the following. They are listed in the order they
should be checked, because the first is the one that cannot be worked around.

### 1. The consuming surface has no search tier (primary)

The three surfaces are tiered differently, and this is the fact that decides most cases:

| Surface | Pinned tools | Catalog / tool-search | Delegates |
|---|---|---|---|
| AI Hub (ASK + BUILD) | yes | **yes** | yes |
| Copilot panel | yes | no | — it *is* the agent |
| Management MCP | yes | **no** | yes |

`PinnedToolSearchToolCallingAdvisor` and `ToolSearchAdvisorConfiguration` live only in
`ai-hub-service`. There is no tool-search anywhere under the MCP server modules.

On the AI Hub, dissolving a delegate has a soft landing: demote its tools to the searchable catalog
and pay one `searchTool` round trip instead of standing schema. **On the management MCP surface that
landing does not exist.** The choice there is binary — one bounded tool, or every underlying tool
flat with no discovery mechanism to bound it. A delegate is therefore the only construct that can
hand a search-less surface a bounded, procedural capability.

The codebase already argues a miniature of this: `updateMcpProjectWorkflowParameters` deliberately
stays off the MCP surface because the mutation "lives exclusively inside the `configureMcpServer`
intelligent tool's inner two-tool ChatClient, so flattening it here too would duplicate the mapping
capability on two paths with different judgment behind them"
(`ToolCallbackContributorConfiguration`).

### 2. Reuse of an agent that exists anyway

Per domain and mode there is **one** prompt file, shared by the Copilot panel agent and the
delegate's subagent ChatClient — there are no `_copilot_` prompt twins. `buildWorkflow` and
`workflowEditorBuildSpringAIAgent` load the same `prompt_workflow_editor_build.txt` and register the
same tool sets.

So the panel agent already *is* the flat form of the delegate. Dissolving on the hub does not remove
a layer; it forks a surface — the hub then needs its own copy of a procedure that already exists,
and the two copies must be kept in step by hand. That is the same duplication
`IntelligentToolSurfaceParityTest` exists to catch, one level up.

### 3. The prompt encodes procedure, not description

CRUD prompts were thin because CRUD tools are self-describing: `deleteAiHubTask({id})` needs no
method. A delegate whose prompt encodes ordering, decision rules, or grounding constraints is
carrying something that has to live *somewhere* — dissolving does not relocate it, it deletes it.

Length is a poor proxy. `prompt_workflow_execution_build.txt` is 11 lines and still encodes a
method: diagnose first, locate the root cause, then either fix the definition or explain that the
cause is data/configuration the user controls, and never invent an id or an error message. Read the
prompt for a method; do not measure it.

### 4. Independent conversation state

Each specialist gets a durable per-conversation session keyed `<parentThreadId>:<agentTypeKey>`
(`SubAgentSessionMemoryContributor`). Dissolving collapses that into the parent's single thread.

## Applying it to the nine

**All nine pass on (1) and (2) alone**, before any judgment about their prompts:

- Every one is contributed to the management MCP server, which has no search tier —
  `ToolCallbackContributorConfiguration.INTELLIGENT_TOOL_NAMES` carries seven CE names, with the
  automation- and embedded-owned ones contributed alongside.
- Every one has a Copilot panel twin: `workflowEditorAsk/Build`, `codeEditorAsk/Build`,
  `clusterElementAsk/Build`, `skillsAsk/Build`, `workflowExecutionAsk/Build`, `converterBuild`
  (BUILD-only by design), `customComponentAsk/Build`, `codeWorkflowAsk/Build`,
  `mcpServerAsk/Build`.

`debugWorkflowExecution` — the case that prompted this spec, on the theory that an 11-line prompt
and a two-tool domain made it "the CRUD case wearing a delegate's coat" — additionally passes (3)
and (4). Its prompt encodes a diagnostic method, and its tool set is 22 in BUILD, 20 in ASK, not 2:
the two `WorkflowExecutionTools` reads sit alongside `projectWorkflowTools`, `taskTools`,
`scriptTools`, `workflowValidatorTools` and `workflowInstructionTools` (BUILD), or
`readProjectWorkflowTools` and `componentTools` (ASK).

**Decision: no intelligent delegate dissolves.** The unwind is complete at the CRUD boundary, and
that boundary is where the criterion puts it.

## Follow-up this surfaced

`WorkflowExecutionTools` is absent from **both** AI Hub catalog beans (`aiHubAskGlobalToolCatalog`,
`aiHubBuildGlobalToolCatalog`), while `componentTools`, `taskTools`, `projectWorkflowTools`,
`scriptTools`, `clusterElementTools` and `taskDispatcherTools` are all catalog-reachable.

Consequence: the hub cannot answer "did my run fail?" without delegating to
`debugWorkflowExecution`. A user asking a one-line status question pays a full delegation.

Recommended, and independent of everything above: add `WorkflowExecutionTools` (`getWorkflowExecution`,
`listWorkflowExecutions` — two tools) to both catalog beans. This does **not** dissolve the delegate;
the delegate keeps the diagnostic method and the fix path, while a bare read stops needing it.
`IntelligentToolSurfaceParityTest` should keep passing untouched, since catalog membership is not
what it asserts.

## What would change the answer

Re-run the criterion against a delegate if any of these become true:

- **The MCP surface gains a search tier.** Leg (1) evaporates for every delegate at once, and the
  whole set is worth re-examining.
- **A delegate loses its panel twin.** Leg (2) was the only thing carrying some of them; a delegate
  with no panel agent and a thin prompt is a genuine dissolution candidate.
- **A prompt degrades to description.** If a delegate's prompt stops encoding a method — reduced to
  listing its tools — leg (3) is gone and only (1) and (2) hold it up.

## Evidence

Measured against the tree at the time of writing (branch `claude/copilot-automation-listings-de1455`,
rebased onto `0_732`).

Prompt sizes, the nine BUILD variants:

| Prompt | Lines | Bytes |
|---|---|---|
| `prompt_code_workflow_build` | 230 | 13,454 |
| `prompt_workflow_editor_build` | 203 | 14,926 |
| `prompt_skills_build` | 180 | 9,942 |
| `prompt_custom_component_build` | 155 | 10,240 |
| `prompt_converter_build` | 127 | 6,893 |
| `prompt_code_editor_build` | 97 | 5,873 |
| `prompt_cluster_element_build` | 88 | 4,781 |
| `prompt_mcp_agent` (`configureMcpServer`) | 55 | 3,374 |
| `prompt_workflow_execution_build` | 11 | 1,063 |
| **total** | **1,146** | **70,546** |
| for comparison: `prompt_ai_hub_build` | 629 | 42,947 |

Flattening the nine would replace **9** pinned tool schemas with **67** distinct ones (the union of
their tool classes), and `PinnedToolSearchToolCallingAdvisor` keeps the entire pinned list callable
in every model iteration — so all 67 would ride a turn that only asks to list chats. This is the
weakest of the arguments and is recorded last on purpose: it is a cost argument, and costs can be
engineered away. Legs (1) and (2) are structural, and cannot.

Tool sets per BUILD delegate, as registered:

| Delegate | Tool classes |
|---|---|
| `buildWorkflow` | projectWorkflow, script, simulation, task, workflowInstruction, workflowValidator |
| `writeScript` | component, readProjectWorkflow, script, workflowInstruction, workflowValidator |
| `importWorkflow` | projectWorkflow, script, task, workflowInstruction, workflowValidator |
| `configureClusterElement` | clusterElement, component, readProjectWorkflow, task, workflowInstruction, workflowValidator |
| `authorSkill` | component, readProject, readProjectWorkflow, skills, workflowInstruction, workflowValidator |
| `debugWorkflowExecution` | projectWorkflow, script, task, workflowExecution, workflowInstruction, workflowValidator |
| `buildCodeWorkflow` | codeWorkflow, readCodeWorkflow |
| `buildCustomComponent` | customComponent, readCustomComponent |
