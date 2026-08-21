# Invoking cluster-element components from code

Status: implemented (2026-08-06)
Date: 2026-08-06

## Problem

`context.component.<component>.<action>(...)` now reaches single-connection and multi-connection
actions. It cannot reach the third shape: an action whose perform reads **cluster elements** —
the AI Agent's chat action above all, which resolves its model, tools, memory and RAG from the
node's extensions via `ClusterElementMap.of(extensions)`.

Today such a call fails with a clear message ("its perform needs state that is wired on a workflow
node"), which is honest but final: a code workflow cannot use the AI Agent at all. Since agent
orchestration is one of the things people most want to write in code, that is a real hole.

The obstacle is not dispatch. It is that cluster elements are **wired on a workflow node** by the
editor — a MODEL element with its own component, parameters and connection — and a code workflow's
nodes are all `codeWorkflow/v1/perform`. There is no node to hang them on.

## Design

### Elements are composed at the call site; connections stay declared

Elements are a third argument to the call:

```javascript
{
    name: "ask-agents",
    connections: [
        {componentName: "openAi", name: "openai-prod"},
        {componentName: "anthropic", name: "anthropic-prod"},
        {componentName: "slack", name: "slack-prod"}
    ],
    perform: function (context) {
        const draft = context.component.aiAgent.chat({messages: [...]}, {
            model: {type: "openAi/v1/model", connection: "openai-prod", parameters: {model: "gpt-4o"}},
            tools: [{type: "slack/v1/sendMessage", connection: "slack-prod", name: "post_to_slack"}]
        });

        return context.component.aiAgent.chat({messages: [...draft]}, {
            model: {type: "anthropic/v1/model", connection: "anthropic-prod", parameters: {model: "claude-sonnet-5"}}
        });
    }
}
```

The reason to put them here rather than on the task: **a code task orchestrates, so it may call
several agents** — a drafting model then a reviewing one, a cheap model with a fallback to a strong
one. Declaring one element set per task would force each of those into its own task, which defeats
the point of writing the orchestration as code in the first place.

What must *not* move to the call site is the **connection**. An element names one of the task's
declared connections by name; it cannot describe a connection inline. That is what preserves
authorization — the user wires `openai-prod` in the editor exactly as they do for any other task,
and a name the task never declared fails at save time. The element composition is the code's
business; the credential is the platform's.

The cost, stated plainly: the element composition is **not** visible in the generated definition, so
the platform cannot show "this task uses gpt-4o" the way it can for a visual agent node. That is the
ordinary price of code — the definition records which connections a task may use, not what it does
with them — and it is the trade that makes multiple agents per task possible at all.

### Dispatch

The host builds the `extensions.clusterElements` map the platform already speaks — the same shape a
visual agent node carries — from the call's element argument, resolving each element's `connection`
name against the task's wired connections. `executePerformForPolyglot` gains an `extensions`
argument alongside the connections map it now carries and dispatches the cluster-element perform
shapes the way `executePerform` already does, so the agent resolves its model through the identical
code path a visual node uses.

Nothing is emitted into the workflow definition for elements — they exist only for the duration of
the call.

### Tools name themselves

A TOOLS element carries a `name` — what the model calls it. Absent, it falls back to the action
name. This matters more here than on a canvas: in code the tool list is written by hand, and two
tools from the same component would otherwise be indistinguishable to the model.

## Work

Smaller than it first appears. `AbstractAiAgentChatAction`'s perform is a
`MultipleConnectionsPerformFunction` — `(inputParameters, connectionParameters, extensions, context)`
— so the seam's multi-connection branch **already dispatches it**; it just passes `Map.of()` as
extensions, which is why the agent finds no model. No new dispatch shape is needed.

1. ~~**Seam** — `executePerformForPolyglot` takes an `extensions` map and passes it to the existing
   `executeMultipleConnectionsPerform` call in place of `Map.of()`.~~ **Done.** The fall-through
   error message lost its "such as cluster elements" clause with it: everything still reaching it is
   a *streaming* shape, so blaming cluster elements became wrong.
2. ~~**Host** — `ComponentActionInvoker.invoke` gains the elements argument; `CodeWorkflowTaskContext`
   builds `extensions.clusterElements` from it, resolving each element's `connection` against the
   task's wired connections. An undeclared name fails the call with the same message a missing task
   connection gives.~~ **Done.** An element's connection is wired under the *element's* name, since
   that is the key `AbstractAiAgentChatAction` looks it up by, not the name the task declared it
   under. A tool's `name` also seeds `parameters.toolName`, which is what the model actually reads.
3. ~~**Guest surface** — the third argument on the `context.component.<x>.<y>(input, connectionName,
   clusterElements)` proxy in both polyglot engines, and across the Espresso bridge as JSON.~~
   **Done.** Passing elements means passing something for `connectionName`, so the proxy now treats a
   guest `null` there as absent rather than calling `asString()` on it.
4. ~~**SDK** — the matching parameter on `TaskContext.component(...)` and the Java type for an element
   (type, connection name, parameters, optional tool name).~~ **Done.** The four-argument form is now
   a default delegating with `null`, so existing tasks are untouched. The element type is
   `WorkflowDsl.clusterElement(type).connection(...).name(...).parameter(...)`, gathered by
   `clusterElements().element(...)/.elements(...)` — and **both builders extend `AbstractMap`** rather
   than being records. A Java task runs through two paths (the classloader path hands the host the
   real object; the Espresso path crosses as JSON and arrives as a plain `Map`), so a record would
   have forced the host to accept two shapes and behave identically on both. Being a `Map` means
   nothing new crosses the boundary and the host needed no change at all.
5. ~~**Docs, prompts, starters.**~~ **Done.** A "Calling an AI agent" section in
   `code-workflows.mdx`; the element contract plus the two rules a generator gets wrong (name your
   tools, never describe a connection inline) in both `prompt_code_workflow*_build.txt`; a commented
   example in all six starters.

**Scope decision — the script component does not get this.** `ActionProxyObject` is shared, so the
third argument is syntactically reachable from a script task; `ScriptComponentActionInvoker` rejects a
non-empty one rather than dropping it silently. An element names one of the *caller's declared*
connections, and a script task declares none — it resolves a connection by matching the target
component instead, so there is nothing for an element's `connection` to name. Opening this up later
means extracting the composer out of `CodeWorkflowTaskContext` into a CE home, which would put
`platform-api`, `platform-configuration-api`, `component-api` and `platform-ai-api` onto
`platform-component-polyglot` — a module that today deps on the polyglot API and `commons-util` and
nothing else.

## Risks

- **Nothing about an element is validated at save time**, since elements are written in code rather
  than declared: a wrong element type or parameter name surfaces when the task runs. Only the
  connection reference can be checked, and only at call time. This is the direct consequence of
  choosing the call site, and the reason the failure messages have to name what was wrong
  precisely.
- **The composition is invisible to the platform.** No definition records which model a task uses,
  so nothing can report or govern it — a cost the visual node does not pay.
- **Scope creep toward "the agent, but in code".** This proposal deliberately stops at *using* an
  agent from a code task. Building the agent loop itself in code — custom tool dispatch, a
  hand-rolled conversation — is a different feature, and the SuspendableToolCallingManager machinery
  it would have to reach is not exposed through any seam.
