# Task Tool Subagents as Cluster Elements — Design

**Status:** proposed
**Date:** 2026-08-03
**Ticket:** 4482 (follow-up)

## Summary

Replace the AI Agent Task Tool's built-in Claude subagents with subagents defined as
`SUBAGENT` cluster elements in the workflow editor, so that each subagent's tools are chosen by the
workflow builder rather than fixed by the library.

## Problem

`AiAgentUtilsTaskTool` (`server/libs/modules/components/ai/agent/utils`) exposes a `taskTool`
`TOOLS` cluster element that wraps `TaskTool` from `org.springaicommunity:spring-ai-agent-utils:0.10.0`.
It builds subagents via `ClaudeSubagentType`:

```java
ToolCallback taskToolCallback = TaskTool.builder()
    .subagentTypes(ClaudeSubagentType.builder()
        .chatClientBuilder("default", chatClientBuilder)
        .build())
    .build();
```

`ClaudeSubagentType.Builder.build()` calls a private `defaultClaudeSubagentTools()` that injects
`FileSystemTools`, `GlobTool`, `GrepTool`, **`ShellTools`**, `BraveWebSearchTool`,
`SmartWebFetchTool` and `TodoWriteTool` into the executor. `ShellTools.bash(String, Long, String,
Boolean)` is a plain `Process` execution, and `ShellTools.Builder` exposes no configuration at all —
no working directory, no allowlist, no sandbox.

The library's per-subagent `tools:` frontmatter does not contain this. Reading all four shipped
personas:

| Persona | `tools:` declared | Effective shell access |
|---|---|---|
| Explore | *absent* ("All tools") | yes — an empty allowlist means no filter |
| general-purpose | *absent* ("*") | yes |
| Plan | `Bash, Glob, Grep, Read, …` | yes, explicitly |
| Bash | `Bash` | yes |

Explore's prompt states `CRITICAL: READ-ONLY MODE`, but that is a prompt instruction, not an
enforced constraint — it still holds the tool.

The defect is not that these capabilities exist. `AiAgentUtilsComponentHandler` already registers
`AiAgentUtilsShellTools`, `AiAgentUtilsFileSystemTools`, `AiAgentUtilsGrepTool`,
`AiAgentUtilsGlobTool`, `AiAgentUtilsListDirectoryTool`, `AiAgentUtilsBraveWebSearchTool` and
`AiAgentUtilsSmartWebFetchTool` as first-class cluster elements a builder may attach to any AI
Agent deliberately — the component is explicitly modelled on Claude Code. Which tools a builder
grants an agent is the builder's decision, and is out of scope here.

The defect is that **the library decides a subagent's tools, and the builder cannot**. A builder who
attaches the Task Tool gets four subagents whose grants they never chose and cannot see, resolved
from frontmatter shipped inside a jar. Explore is the clearest case: it declares no `tools:` key, the
executor reads that as "no filter", and it therefore holds a shell tool while its own prompt asserts
`CRITICAL: READ-ONLY MODE`. A prompt is a request; an allowlist is enforcement, and here the
allowlist is not the builder's to write.

**Not yet released.** Added under ticket 4482; `git ls-tree v1.1.5` contains no files under
`components/ai/agent/utils/` at all. The design can change freely without migration.

## Decisions

1. **ByteChef-native tools only.** Never construct `ClaudeSubagentType`, so `ShellTools` and
   `FileSystemTools` never enter the object graph. This is removal, not filtering.
2. **Subagents are cluster elements**, defined per workflow in the editor, not markdown personas
   shipped in the library.
3. **Workspace scope from the job principal**, no user impersonation. Decision 2 turns out to
   satisfy this by inheritance rather than by new machinery — see *Why cluster elements resolve two
   problems* below — so no workspace-scoped service principal is introduced.
4. **Background tasks are in-process and disposable.** A subagent interrupted by a crash restarts
   from the beginning on resume. No durable task storage, and no coordination with the checkpoint.
5. **Read/write separation is a documented pattern**, expressed by which TOOLS an author attaches,
   not by shipped persona files.

## Architecture

```
AI Agent
 └─ TOOLS: taskTool
      ├─ MODEL          default model for subagents
      └─ SUBAGENT[]     new type, multipleElements = true
           ├─ parameters: label, description, instructions
           ├─ TOOLS[]   this subagent's tools
           └─ MODEL?    optional per-subagent override
```

`ByteChefSubagentType` implements `SubagentType` and `ByteChefSubagentExecutor` implements
`SubagentExecutor`, both from `org.springaicommunity:spring-ai-agent-utils-common:0.10.0`. The SPI
is public and separate from the Claude implementation, so no fork or patch is required —
`TaskTool.builder().subagentTypes(...)` accepts any implementation.

At execution time `AiAgentUtilsTaskTool.apply` reads the SUBAGENT elements from its `extensions` via
`ClusterElementMap.of(extensions)` — the same call it already uses to resolve its MODEL child — and
builds one `SubagentDefinition` per element. `ByteChefSubagentExecutor` constructs a `ChatClient` per
subagent from that subagent's own MODEL (falling back to the Task Tool's MODEL) plus its own TOOLS.

### Why cluster elements resolve two problems rather than solving them

**The allowlist stops being a validation problem.** With markdown personas, `tools: Bash, Grep` is a
list of names that must be validated against what the author may actually reach. With cluster
elements there is nothing to validate: the tools *are* the elements the author wired. An author
cannot name a tool they did not attach.

**No new identity model is needed.** An earlier draft fed subagents the `automation-ai-tool`
callbacks, which call `Workspace*Facade` methods that Gecko T18–T25 guarded against a *user's*
permissions — requiring a workspace-scoped service principal that does not exist. Because a
subagent's tools are ordinary TOOLS cluster elements, they are the same tools the parent AI Agent
already runs, and inherit its existing auth and context behaviour unchanged.

### Platform support already present

Both halves of the nesting are already supported; neither needs platform work.

- **Server:** `ClusterElement.getExtensions()` may itself contain `clusterElements`, and
  `ClusterElementMap.of()` recurses into it. `fetchClusterElementRecursively`,
  `findNestedClusterElement` and `searchClusterElementInside` walk arbitrary depth.
- **Client:** `createClusterElementNodes`
  (`client/src/pages/platform/cluster-element-editor/utils/createClusterElementsNodes.ts`) calls
  itself recursively at lines 86 and 146 whenever an element has `clusterElements`, in both the
  multiple-elements and single-element branches, with no depth cap. A nested cluster root must
  appear in `nestedClusterRootsDefinitions`, which is keyed by component name.

The Task Tool is already a nested cluster root at depth 2 (AI Agent → taskTool → MODEL), so depth 3
(→ SUBAGENT → TOOLS) uses the same path.

### New cluster element type

`ClusterElementType` is a plain record. Note the component order — `multipleElements` precedes
`required`, and both are unlabelled booleans, so transposing them compiles cleanly and fails only at
runtime:

```java
record ClusterElementType(String name, String key, String label, boolean multipleElements, boolean required)
```

so SUBAGENT is declared:

```java
ClusterElementType SUBAGENT = new ClusterElementType("SUBAGENT", "subagent", "Subagent", true, false);
```

`multipleElements = true` (a builder may define as many subagents as the workflow needs),
`required = false` (a Task Tool with no subagents is valid but inert) — the same pair of values
`BaseToolFunction.TOOLS` uses.

## Background tasks

`TaskTool.Builder.taskRepository(TaskRepository)` is a supported seam. The interface is four methods:
`getTasks(String)`, `putTask(String, Supplier<String>)`, `removeTask(String)`, `clear()`.

The default implementation is unusable here: `DefaultTaskRepository` is a `Map<String, BackgroundTask>`
over an `ExecutorService`, and `BackgroundTask` wraps a `CompletableFuture<String>` — JVM-local by
construction.

This collides with existing agent behaviour. `SuspendableToolCallingManager` checkpoints
`AiAgentConversationCheckpoint` into `Data.Scope.CURRENT_EXECUTION` after each completed tool round,
so a crash-resumed job replays the conversation. A background task is inherently cross-round — the
model receives an id in round N and reads output in round N+1 — so a resumed conversation would hold
task ids whose futures died with the JVM, and would call `TaskOutput` on a task the repository has
never heard of.

Rather than coordinate with the checkpoint, background tasks are treated as disposable: **a subagent
interrupted by a crash restarts from the beginning.** This is a deliberate simplification for now.

`ByteChefTaskRepository` therefore decorates `DefaultTaskRepository` and changes exactly one
behaviour — `getTasks(id)` for an unknown id returns a completed `BackgroundTask` whose result tells
the model the task was lost and should be re-issued, instead of returning `null`. That covers the
resume case (checkpoint restored, futures gone) without any drain, any durable storage, or any seam
between the Task Tool and the agent's checkpoint path. It also hardens the tool generally, since
`TaskOutputTool` calls `isCompleted()`/`getResult()` directly on the returned task with no visible
null-guard.

The cost is explicit and accepted: work done by an interrupted background subagent is thrown away,
and the model pays for re-running it. Durable tasks backed by real ByteChef jobs were considered and
rejected as substantially overlapping subflow machinery.

**Tenant propagation** (added during implementation, not foreseen here). Tenant id is a thread local
that selects the database schema. A task handed to the executor runs on a pool thread that never had
one, so any tool the subagent calls would read the wrong schema — or none. `ByteChefTaskRepository`
captures the tenant on the submitting thread and reinstates it around the task body via
`TenantContext.callWithTenantId`. Anything else that is thread-local and load-bearing — Spring
Security's context, for instance — has the same exposure and is *not* propagated today; the parent
agent's tools already run without a request security context, so this matches existing behaviour
rather than extending it.

The cost is explicit: a background task cannot outlive its round. Parallel fan-out within a round
works; long-running detached tasks do not. Durable tasks backed by real ByteChef jobs were considered
and rejected for now as substantially overlapping subflow machinery.

## Files

**Create**
- `.../ai/agent/utils/cluster/subagent/ByteChefSubagentType.java`
- `.../ai/agent/utils/cluster/subagent/ByteChefSubagentExecutor.java`
- `.../ai/agent/utils/cluster/subagent/ByteChefSubagentDefinition.java`
- `.../ai/agent/utils/cluster/subagent/ByteChefTaskRepository.java`
- `.../ai/agent/utils/cluster/AiAgentUtilsSubagent.java` — the SUBAGENT cluster element definition

**Modify**
- `.../ai/agent/utils/cluster/AiAgentUtilsTaskTool.java` — swap `ClaudeSubagentType` for
  `ByteChefSubagentType`; declare SUBAGENT as an accepted child type
- `sdks/backend/java/component-api/.../ai/agent/SubagentFunction.java` (new) — declares the
  `SUBAGENT` `ClusterElementType`, alongside the existing `BaseToolFunction.TOOLS`
- `.../ai/agent/utils/build.gradle.kts` — add `spring-ai-agent-utils-common`

**Tests**
- `ByteChefSubagentTypeTest` — definitions built from cluster elements; a subagent with no TOOLS gets
  an empty callback list
- `ByteChefSubagentExecutorTest` — per-subagent model override falls back to the Task Tool's MODEL;
  a subagent receives only its own attached tools
- `ByteChefTaskRepositoryTest` — `drain()` joins outstanding tasks; a drained repository reports no
  incomplete tasks
- `AiAgentUtilsTaskToolTest` — regression: `ShellTools` and `FileSystemTools` never appear in any
  subagent's callbacks

## Phasing

**Phase 1 — the fix.** `ByteChefSubagentType`/`Executor`, the SUBAGENT element type, and the
`AiAgentUtilsTaskTool` rewire. Shell, filesystem and web tools are gone. Ships independently.

**Phase 2 — background tasks.** `ByteChefTaskRepository` and the drain hook. Pure addition on
Phase 1.

## Open questions

1. **Reaching `ToolCallback`s from a TOOLS element outside the agent action.**
   `AbstractAiAgentChatAction` builds callbacks for a single TOOLS entry privately (around line
   1030). The Task Tool needs the same capability. Either extract a shared seam or resolve via
   `ClusterElementDefinitionService`, as it already does for MODEL. Preference: the latter, to avoid
   widening the agent action's API.
2. ~~**Empty-state behaviour.**~~ **Resolved 2026-08-03:** a Task Tool with no SUBAGENT elements
   attached does nothing, and that is intended. No personas ship with the product — the builder
   authors each subagent's name, description and instructions on the SUBAGENT element itself. A
   shipped persona carrying baked-in tool grants is precisely how Explore ended up holding a shell.
3. ~~**The drain seam (Phase 2 blocker).**~~ **Resolved 2026-08-03:** no seam is needed. Accepting
   that an interrupted subagent restarts from the beginning removes the requirement to drain
   before the checkpoint, so the Task Tool and the agent's checkpoint path stay unaware of each
   other. See *Background tasks* above.
4. **Web fetch.** `SmartWebFetchTool` is deliberately excluded from v1. Component-level SSRF
   validation is the deferred half of Gecko T15, so re-adding outbound fetch should wait for that
   rather than open a hole ahead of it.

## Non-goals

- Durable background tasks surviving restart or spanning nodes.
- LLM-defined subagents. The tool allowlist must come from human-authored configuration; a
  prompt-injected agent that could define its own subagent would grant itself any tool.
- Reusing the AI Hub subagent prompts (research, data_analyst, …). Those assume a chat surface with a
  live user.
