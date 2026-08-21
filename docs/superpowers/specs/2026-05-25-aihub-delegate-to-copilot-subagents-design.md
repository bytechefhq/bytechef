# AI Hub — Delegate to Copilot specialists via sub-agent ToolCallbacks

**Status:** Draft
**Date:** 2026-05-25
**Author:** Ivica Cardic

## Motivation

`AiHubConfiguration` currently registers raw MCP tool beans that overlap one-for-one with what the specialist agents in `CopilotConfiguration` already orchestrate. The clearest example: AiHub registers `ReadSkillsTools` and `SkillsTools` directly (`AiHubConfiguration.java:12-13, 301, 445`), and Copilot exposes `skillsAskSpringAIAgent` / `skillsBuildSpringAIAgent` that wrap the same two tool beans with a tuned system prompt and adjacent read tools (`CopilotConfiguration.java:252-286`).

The duplication has three concrete costs:

1. **Prompt drift.** AiHub's system prompt has to teach the LLM how to use raw skill primitives. Copilot's prompt already does this — and Copilot's wording is the one product owns.
2. **Tool surface bloat.** Every raw tool we hang on the AiHub agent grows its tool-search index and inflates the per-turn tool list. Sub-agents are one entry each.
3. **No single source of truth for specialist behaviour.** A Copilot agent today is the canonical "how to do skills work" / "how to edit cluster elements" / etc. AiHub bypassing it means two behaviours that have to be kept in sync by hand.

The fix is to stop hanging raw specialist tools on the AiHub agent and instead delegate to the specialist agents themselves, following the pattern AiHub already uses for `research`, `workflow_builder`, `data_analyst`, `image_generator`, and `slide_builder` (`AiHubConfiguration.java:616-652`).

## Non-goals

- **No change to Copilot's own surfaces.** The Copilot panel in the workflow editor / code editor / cluster-element editor continues to invoke `*SpringAIAgent` directly, unchanged.
- **No new task kinds.** Routing happens via tool call inside an AiHub turn; `AiHubTaskKind` and `AiHubRoutingAgent`'s top-level dispatch are unaffected.
- **No streaming sub-agent transcripts to the parent.** Matches the existing research / workflow-builder contract: sub-agent runs in isolation, only the synthesised result reaches the parent LLM.
- **No deletion of the underlying MCP tool beans** (`SkillsTools`, `ClusterElementTools`, `ProjectTools`, etc.). Copilot still depends on them. We only stop registering them on the AiHub agent.

## Current state

### How the existing sub-agent wrapping works

`ResearchConfiguration` builds a `ChatClient` bean (research-specific system prompt + Firecrawl tools), and `ResearchToolCallback` is a hand-rolled `ToolCallback` whose `call()` invokes that ChatClient with the parent's input and returns the synthesised content. The wrapper carries its own JSON-schema input description ("topic: string") and a free-text DESCRIPTION that teaches the parent LLM when to call it.

`AiHubConfiguration` then instantiates the callback inline once per ASK/BUILD bean method:

```java
researchChatClientProvider.ifAvailable(
    researchChatClient -> toolCallbacks.add(
        new ProgressReportingToolCallback(
            ResearchConfiguration.createResearchToolCallback(researchChatClient), "research")));
```

`ProgressReportingToolCallback` is a thin decorator that streams progress events to the AG-UI subscriber while the sub-agent runs.

### What's different about Copilot agents

Copilot specialists in `CopilotConfiguration` are full `*SpringAIAgent` instances, not bare `ChatClient`s. Each carries:

- A specialist system prompt loaded from a classpath resource.
- A tool list (e.g. `[readSkillsTools, readProjectTools, readProjectWorkflowTools]` for `skillsAskSpringAIAgent`).
- Its own `ChatMemory` (so a Copilot agent's UI conversation has its own thread).
- An optional `overrideChatClientResolver` for per-workspace LLM overrides.

Wrapping the entire `SpringAIAgent` (with its memory) as a `ToolCallback` would double-thread memory in confusing ways — the sub-agent would accumulate state across AiHub turns that aren't part of any "real" Copilot conversation.

## Proposed design

### Wrap a stateless ChatClient, not the full SpringAIAgent

Each Copilot specialist gets a paired `ChatClient` bean that carries the same system prompt and tools as the specialist's `SpringAIAgent`, but no chat memory. The sub-agent ToolCallback delegates to that ChatClient with the parent's input as a single user message and returns the synthesised content — same contract as `ResearchToolCallback`.

This isolates each AiHub delegation to a single LLM round-trip with no cross-turn memory bleed, and matches the existing sub-agent semantics so AiHub's mental model is uniform.

### New ChatClient beans in CopilotConfiguration

For each existing specialist agent, add a sibling `*SubAgentChatClient` bean:

| Specialist agent | New ChatClient bean | Tools | Used by AiHub mode |
| --- | --- | --- | --- |
| `skillsAskSpringAIAgent` | `skillsAskSubAgentChatClient` | `readSkillsTools, readProjectTools, readProjectWorkflowTools` | ASK |
| `skillsBuildSpringAIAgent` | `skillsBuildSubAgentChatClient` | `skillsTools, readProjectTools, readProjectWorkflowTools` | BUILD |
| `clusterElementAskSpringAIAgent` | `clusterElementAskSubAgentChatClient` | `readProjectWorkflowTools, componentTools, taskTools` | ASK |
| `clusterElementBuildSpringAIAgent` | `clusterElementBuildSubAgentChatClient` | `clusterElementTools, readProjectWorkflowTools, componentTools, taskTools` | BUILD |
| `codeEditorAskSpringAIAgent` | `codeEditorAskSubAgentChatClient` | `readProjectWorkflowTools, componentTools, firecrawlTools?` | ASK |
| `codeEditorBuildSpringAIAgent` | `codeEditorBuildSubAgentChatClient` | `readProjectWorkflowTools, scriptTools, componentTools` | BUILD |
| `workflowEditorAskSpringAIAgent` | `workflowEditorAskSubAgentChatClient` | `readProjectTools, readProjectWorkflowTools, componentTools, taskTools, firecrawlTools?` | ASK |
| `workflowEditorBuildSpringAIAgent` | `workflowEditorBuildSubAgentChatClient` | `projectTools, projectWorkflowTools, taskTools, scriptTools` | BUILD |
| `converterBuildSpringAIAgent` | `converterBuildSubAgentChatClient` | `projectTools, projectWorkflowTools, taskTools, scriptTools` | BUILD |

Each ChatClient is built with `ChatClient.builder(chatModel).defaultSystem(prompt).defaultToolCallbacks(ToolCallbacks.from(...))`. The prompt resources are the existing `prompt_*_ask.txt` / `prompt_*_build.txt` files already in `CopilotConfiguration` — no new prompt files.

**Open question — RAG advisor on workflow-editor ASK:** the production `workflowEditorAskSpringAIAgent` registers a `QuestionAnswerAdvisor` on the workflow-editor RAG vector store. Decide whether the sub-agent ChatClient keeps it (consistent answers, marginally higher per-call cost) or drops it (lower latency, parent LLM expected to ground itself another way). Recommendation: keep it — RAG grounding is the whole point of delegating workflow-editor ASK to this specialist.

### New ToolCallback wrappers

A new class per specialist family, mirroring `ResearchToolCallback`:

- `SkillsAgentToolCallback` (one class, takes the ChatClient at construction — same instance is used in both ASK/BUILD AiHub agents but constructed from different upstream ChatClients).
- `ClusterElementAgentToolCallback`
- `CodeEditorAgentToolCallback`
- `WorkflowEditorAgentToolCallback`
- `ConverterAgentToolCallback`

Each carries:

- A `DESCRIPTION` teaching the parent LLM when to delegate (e.g. for skills: "Delegate any user request that creates, updates, lists, or explains workflow Skills — reusable parameterised workflow templates the user can compose into projects. Returns a synthesised report or, for build mode, a summary of mutations performed.").
- An `INPUT_SCHEMA` with a single `request` string field. The sub-agent ChatClient does its own task decomposition.
- A `call(toolInput, ToolContext)` that parses the `request` field and invokes `chatClient.prompt(request).call().content()`.

Co-located with the existing `ResearchToolCallback` under `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/tool/`.

### AiHubConfiguration changes

In `aiHubAskSpringAIAgent`:

- **Remove** the `ReadSkillsTools` constructor parameter and the `ToolCallbacks.from(readSkillsTools)` registration at line 301.
- **Add** registrations:
  ```java
  toolCallbacks.add(new ProgressReportingToolCallback(
      new SkillsAgentToolCallback(skillsAskSubAgentChatClient), "skills_agent"));
  toolCallbacks.add(new ProgressReportingToolCallback(
      new ClusterElementAgentToolCallback(clusterElementAskSubAgentChatClient), "cluster_element_agent"));
  toolCallbacks.add(new ProgressReportingToolCallback(
      new CodeEditorAgentToolCallback(codeEditorAskSubAgentChatClient), "code_editor_agent"));
  toolCallbacks.add(new ProgressReportingToolCallback(
      new WorkflowEditorAgentToolCallback(workflowEditorAskSubAgentChatClient), "workflow_editor_agent"));
  ```

In `aiHubBuildSpringAIAgent`:

- **Remove** the `SkillsTools` constructor parameter and the `ToolCallbacks.from(skillsTools)` registration at line 445.
- **Add** the BUILD variants of the same five wrappers, plus `ConverterAgentToolCallback`.

The `registerSubAgentToolCallbacks` helper at lines 616-652 stays as-is — it covers the existing ChatClient sub-agents (research, workflow_builder, data_analyst, image_generator, slide_builder). A parallel helper `registerCopilotSubAgentToolCallbacks` keeps the new wiring grouped and the bean methods within Checkstyle's per-method line limit.

### What does NOT change

- `AiHubRoutingAgent` — task-kind dispatch is unaffected.
- `SkillsTools` / `ReadSkillsTools` / `ClusterElementTools` / etc. — still exist, still Spring beans, still consumed by the Copilot agents. We just stop registering them on AiHub.
- The Copilot panel's direct invocations of `*SpringAIAgent` — out of scope. Copilot still has its own surfaces with their own conversations and their own memory.
- ASK vs BUILD separation — preserved end-to-end. ASK AiHub only sees ASK Copilot sub-agents; BUILD AiHub only sees BUILD ones.

### Broader removal: AiHub-direct ToolCallbacks superseded by sub-agents

**Decided 2026-05-25 (post-initial-draft):** beyond removing the literal `ReadSkillsTools` / `SkillsTools` MCP beans, also remove every AiHub-direct `ToolCallback` whose *function* a Copilot sub-agent's tool catalog covers. This is the architectural intent the user called out: AiHub stops doing project/workflow/component work directly so the LLM has no choice but to delegate to the specialist.

**Removed from AiHub agents:**

| AiHub callback | Mode | Covered by sub-agent |
| --- | --- | --- |
| `ListWorkflowsToolCallback` | ASK + BUILD | workflow-editor (`ReadProjectWorkflowTools` / `ProjectWorkflowTools`) |
| `GetWorkflowToolCallback` | ASK + BUILD | workflow-editor |
| `CreateWorkflowToolCallback` | BUILD | workflow-editor BUILD |
| `UpdateWorkflowToolCallback` | BUILD | workflow-editor BUILD |
| `ListProjectsToolCallback` | BUILD | workflow-editor BUILD (`ReadProjectTools` / `ProjectTools`) |
| `CreateProjectToolCallback` | BUILD | workflow-editor BUILD |
| `UpdateProjectToolCallback` | BUILD | workflow-editor BUILD |
| `DeleteProjectToolCallback` | BUILD | workflow-editor BUILD |
| `SearchComponentsToolCallback` | ASK + BUILD | every editor specialist (`ComponentTools`) |
| `ListComponentActionsToolCallback` | ASK + BUILD | every editor specialist |
| `DescribeComponentActionToolCallback` | ASK + BUILD | every editor specialist |
| `ToolCallbacks.from(readSkillsTools)` | ASK | skills ASK |
| `ToolCallbacks.from(skillsTools)` | BUILD | skills BUILD |

The `registerComponentDiscoveryToolCallbacks` helper at `AiHubConfiguration.java:673-685` becomes unused after this round and is deleted entirely.

**Kept on AiHub** (no Copilot sub-agent owns the function):

- All UI tab callbacks (`Open*TabToolCallback`).
- All data table callbacks (list/query/add/update/delete row, add column, create-from-csv, clone).
- All knowledge base callbacks (list, query, add/delete document, clone).
- AiHubTask + tool attach: `AttachTaskTool`, `RemoveTaskTool`, `ListTaskTools`, `AskUserQuestion`, `ListAiHubTasks`.
- Connection UI: `CreateConnection`, `SelectConnection`, `ListConnectionsForComponent`.
- `ListWorkflowExecutionsToolCallback` (execution history, not workflow editing).
- API collections, MCP projects, MCP servers (when their facades are present).
- Chat-workflow callbacks (`ListChatWorkflows`, `RunChatWorkflow`, `CreateWorkflowChat`).
- All deployment callbacks (`List` / `Create` / `Update` / `Delete` / `Rollback` / `Toggle` `ProjectDeployment`, `PromoteWorkflow`).
- Personal agent callbacks (`List` / `Open` tab / `Create` / `Update` / `Delete` / `Clone` `AiHubPersonalAgent`).
- Memory callbacks (`registerAutoMemoryToolCallbacks`).
- Context store callbacks (`registerContextStoreToolCallbacks` / `registerContextStoreReadOnlyToolCallbacks` / `registerContextStoreSemanticSearchToolCallback`).
- Asset file callbacks.
- The existing ChatClient sub-agent wrappers (research, workflow_builder, data_analyst, image_generator, slide_builder).

**Constructor parameter cleanup:** removing the callbacks means several services on the bean-method constructors lose consumers. Run a usage check per parameter after each removal and drop unused parameters. Likely casualties on the ASK bean: `actionDefinitionService` (after `register*ComponentDiscovery*` removal). On the BUILD bean: `actionDefinitionService`, possibly `projectFacade` (verify — used by both `ListProjects` and `ListWorkflows`), `projectWorkflowFacade`, `projectService`. `componentDefinitionService` stays on both (still needed by `CreateConnection` / `SelectConnection` / `DescribeSourceComponentEntities`).

## Open questions

1. **Should the AiHub system prompt change?** Today it teaches the LLM about skills primitives. With delegation, it should instead teach when to invoke each Copilot sub-agent. Probably a small follow-up — not blocking but should land in the same PR so the LLM's behaviour stays coherent.
2. **Sub-agent error envelope.** What does the parent see if `chatClient.prompt(...).call().content()` throws or times out? Recommendation: mirror `ResearchToolCallback`'s `ToolErrors` envelope so the parent gets a structured "sub-agent failed: {message}" string instead of a stack trace.
3. **Metering.** `ResearchToolCallback` wraps Firecrawl calls with `MeteredToolCallback` for usage tracking. The Copilot sub-agents' tools (`SkillsTools`, `ScriptTools`, …) aren't metered today. If usage attribution matters for these too, add an entry to `mapFirecrawlToolName`-style mapping logic per sub-agent — but probably ok to defer.
4. **Test strategy.** Sibling tests of `WorkflowBuilderConfigurationTest` (which already covers a `createWorkflowBuilderToolCallback` wrapper) for each new wrapper, plus an integration test that asserts the AiHub bean methods no longer take `ReadSkillsTools` / `SkillsTools` as constructor parameters.

## Out of scope (for this spec, not forever)

- Replacing the current AiHub system prompt with one that names the sub-agents explicitly — beyond the minimal wording change in §"Open questions" item 1.
- Streaming sub-agent intermediate events (Firecrawl scrapes, skill mutations) up to the AG-UI subscriber. The existing pattern returns one synthesised string; matching it.
- Per-workspace LLM overrides for sub-agents. The existing research / workflow_builder ChatClients don't honour `overrideChatClientResolver`; we keep the same simple wiring for the Copilot sub-agents.
