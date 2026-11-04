---
name: bytechef-workflow-builder
description: Use when the user asks to "build a workflow in ByteChef", "create a ByteChef project and workflow", "add a step to my ByteChef workflow", "wire up a trigger", "connect my Slack/Gmail/... to the workflow", or "publish the project", and the ByteChef management MCP server is connected. Covers authoring the workflow definition yourself, step by step, without an AI provider on the ByteChef server.
transports:
  required: [mcp]
---

# ByteChef Workflow Builder

Builds ByteChef workflows over the management MCP server. You write the workflow definition yourself:
read the format, look up the components, save, fix what the server reports, bind connections and
publish. Every step is a deterministic tool call. If the server is not connected yet, use the
`bytechef-mcp-setup` skill first.

## When to use

<!-- transport: mcp uses: general -->
Use this loop whenever the user wants a ByteChef workflow created or changed: a new automation, an
extra step, a different trigger, a fixed parameter. You are the author; the server stores, validates
and runs what you write. Agree on the outline with the user first (what starts the workflow, which
apps it touches, what it does with the data), then build it in small saves rather than one large
definition, so each validation round stays short.
<!-- /transport -->

## Find or create the project

<!-- transport: mcp uses: listProjects, createProject -->
A workflow lives in a project. Call `listProjects` with the workspace id (and a `query` to narrow by
name) to see whether a suitable project exists. If not, create one with `createProject` in that
workspace. Keep the project id; every workflow in this build is created inside it.
<!-- /transport -->

## Read the build instructions

<!-- transport: mcp uses: getWorkflowBuildInstructions, getClusterElementsInstructions, getScriptCodeInstructions -->
Before writing a definition, call `getWorkflowBuildInstructions`. It returns the JSON shape of a
workflow, the expression syntax for referencing earlier outputs, and guidance on filling task
parameters. Read it once per session and follow it over anything you remember from elsewhere. The
instructions may mention data-table tools, which are not available over MCP; skip those steps, or ask
the user which existing table to use.

When the workflow contains an action with nested sub-components (an AI Agent with its model, memory
and tools, or a data pipeline with a source and destination), also call
`getClusterElementsInstructions`. When a step runs custom code in the Script component, call
`getScriptCodeInstructions` for the language-specific contract.
<!-- /transport -->

## Look up components

<!-- transport: mcp uses: searchComponents, getComponent, searchActions, getActionDefinition, getProperties, getOutputProperty, searchTriggers, getTriggerDefinition, searchClusterElements, getClusterElement, searchTasks, getTaskDefinition -->
Never guess a component, action or parameter name. Look each one up:

- Find the app with `searchComponents` and confirm its version and actions with `getComponent`.
- Find a step with `searchActions`, then take its exact type and parameter template from
  `getActionDefinition`. When a parameter is nested or conditional, read the full property tree of
  the action, trigger or cluster element with `getProperties`.
- Find what starts the workflow with `searchTriggers` and read it with `getTriggerDefinition`.
- Flow control (condition, loop, each, branch, parallel) comes from task dispatchers: find them with
  `searchTasks` and read them with `getTaskDefinition`.
- For nested sub-components, find the options for a cluster element type with
  `searchClusterElements` and read the chosen one with `getClusterElement`.
- Before a later step references an earlier step's result, check what that step returns with
  `getOutputProperty`, and reference only fields that exist.

Fill every required parameter whose value the user has already given. Some parameters take their
options from the user's account (a channel, a folder, a spreadsheet) and need a connection to list;
ask the user for the value, or leave it empty and tell the user to pick it in the ByteChef UI. Never
invent ids.
<!-- /transport -->

## Create and save the workflow

<!-- transport: mcp uses: createProjectWorkflow, getWorkflow, updateWorkflow -->
Create an empty workflow in the project with `createProjectWorkflow`. Keep the `workflow_id` string
from its result, not the numeric `id`; it is the workflow id every later workflow call takes. Then
call `getWorkflow` with it to read the current definition and `version`.

Save with `updateWorkflow`. It always takes the whole definition, never a fragment, plus the `version`
the definition is based on. To change an existing workflow, read it with `getWorkflow`, edit the full
definition and send it back. A successful save returns the new version; use it for the next save. If
the save is rejected because the workflow changed in the meantime, read it again with `getWorkflow`,
re-apply your edit and retry.

`updateWorkflow` is the authoritative check, and its result has two kinds of findings:

- **Errors** block the save; nothing is stored. Fix every reported error and call it again with the
  same version.
- **Warnings** come back after a successful save. Typical ones are a missing connection or a
  parameter the user still has to fill. The workflow is stored; resolve the warnings next or report
  them to the user.

`updateWorkflow` takes an optional `environment` (`DEVELOPMENT`, `STAGING` or `PRODUCTION`, default
`DEVELOPMENT`), which is the environment validation runs in. Use the same environment for the whole
build.
<!-- /transport -->

## Connections and test-connect

<!-- transport: mcp uses: listConnections, saveWorkflowTestConnection -->
A component that talks to an outside app needs a connection. The workflow definition only declares
which component a step needs; it never holds a connection id.

Call `listConnections` with the workspace id and the environment of the build, optionally filtered by
component name, to find the user's existing connections. If several match, ask the user which one to
use. If none exists, ask the user to create one in the ByteChef UI for that environment and list
again when they are done.

Bind the chosen connection to each step that needs it with `saveWorkflowTestConnection`, passing the
workflow id, the step name, the connection key (usually the component name, or a key declared in the
step's `connections` block) and the connection id, in the same environment. This binding is what
lets the user test-run the workflow.
<!-- /transport -->

## Publish

<!-- transport: mcp uses: publishProject -->
A saved workflow is a draft until its project is published. When the user is happy with the
workflow, call `publishProject` with the project id and a short description of what changed. This
creates a new project version, which can then be deployed to an environment from the ByteChef UI.
Publish only when the user asks or agrees; it versions every workflow in the project.
<!-- /transport -->

## When to use buildWorkflow

<!-- transport: mcp uses: buildWorkflow -->
`buildWorkflow` hands a plain-language request to an AI agent running on the ByteChef server, which
writes the workflow for you. Use it only when the user asks for it, or when a large build would fill
your context. It needs an AI provider configured on the ByteChef server. If it reports that no
provider is available, do not retry; author the workflow yourself with the authoring tools. Each
call is independent and re-reads the current workflow, so restate the context it needs with every
call.
<!-- /transport -->

## Concepts

<!-- transport: mcp uses: general -->
- **Trigger**: what starts a workflow, such as a schedule, a webhook, a new email or a manual run.
  A new workflow starts with a manual trigger; replace it when the user wants something else.
- **Task**: one step. Most tasks are component actions (send a Slack message, add a spreadsheet row).
  Task dispatchers (condition, loop, branch, parallel) hold their own nested tasks.
- **Cluster elements**: sub-components nested inside one task, keyed by element type, such as an AI
  Agent's model, memory and tools.
- **Expressions**: `${taskName.property}` references an earlier step's output, for example
  `${getMail_1.subject}` or `${rows_1.items[0].email}`. The task name is the step's `name`, not its
  label. Only reference fields the step actually returns.
- **Environment**: connections, test bindings and deployments are kept per environment
  (`DEVELOPMENT`, `STAGING`, `PRODUCTION`); build and test in one of them, usually `DEVELOPMENT`.
<!-- /transport -->
