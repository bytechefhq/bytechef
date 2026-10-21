---
name: ByteChef Workflow Builder
description: This skill should be used when the user asks to "build a workflow in ByteChef", "create a project and workflow", "add a step to my workflow", "wire up a trigger", "import an n8n/Make/Zapier workflow", or wants Claude to operate a connected ByteChef instance over the Management MCP server rather than author code locally.
transports:
  required: [mcp]
---

# ByteChef Workflow Builder

Operates a running ByteChef instance directly over its **Management MCP server**: creating a
project, creating a workflow, building or importing it, and publishing it. This assumes the server
is already connected (see the sibling **ByteChef Management MCP Setup** skill) — this skill is
about what to call once it is, not how to configure `.mcp.json`.

## Two kinds of tools

<!-- transport: mcp uses: general -->
Ordinary tools are deterministic CRUD — call them directly and expect an immediate result.
Intelligent tools run an inner AI agent and may take minutes. Call an intelligent tool for judgment
work — "build this", "import that" — never for CRUD a plain tool already does.
<!-- /transport -->

For this skill specifically, `buildWorkflow` and `importWorkflow` are the intelligent tools. Other
intelligent tools exist outside this skill's scope (`configureClusterElement`, `writeScript`,
`buildCodeWorkflow`, `buildCustomComponent`, `authorSkill`, `debugWorkflowExecution`) and may or may
not be registered depending on edition and configuration.

## Creating a project and workflow

<!-- transport: mcp uses: listProjects, createProject, createProjectWorkflow -->
Check whether the target project already exists with `listProjects`. If not, create it with
`createProject`. Either way, create the workflow inside it with `createProjectWorkflow` — its
result is the `workflowId` that every later workflow operation keys on.
<!-- /transport -->

## Building a workflow from a plain-language instruction

<!-- transport: mcp uses: buildWorkflow -->
Call `buildWorkflow` with the `workflowId` returned when the workflow was created and a
plain-language instruction describing what the workflow should do.

It is also the only way to change an existing workflow's content. There is no direct write tool, so
even a small edit — renaming a task, tweaking one parameter — is a `buildWorkflow` call with the
`workflowId` and the change described in plain language.

Each intelligent-tool call is independent: it re-reads the current state of the workflow rather
than remembering earlier calls in this conversation. To keep building, call `buildWorkflow` again
with the next instruction and restate any context it still needs — do not assume it recalls what
was asked for previously.

`buildWorkflow` may come back with a question and numbered options instead of a finished result.
When that happens, present the options to the user, then call `buildWorkflow` again with the
**original request together with the chosen answer** — the call that asked the question is gone by
the time the answer arrives, so the answer on its own is not enough for a fresh call to act on.
Ordinary CRUD tools never do this.
<!-- /transport -->

## Importing from n8n, Make, Zapier, or Workato

<!-- transport: mcp uses: importWorkflow -->
Call `importWorkflow` with the `workflowId` and the source workflow definition (the exported
n8n/Make/Zapier/Workato JSON). It has no project- or workflow-creation tools of its own — create the
project and an empty workflow to import into first, then call `importWorkflow`. Each call is
independent and may return a clarifying question instead of a result; handle it the same way.
<!-- /transport -->

## Discovering what a step can do

<!-- transport: mcp uses: listComponents, searchActions, getActionDefinition, getProperties -->
Before wiring a step by hand, find the component with `listComponents`, find the action with
`searchActions`, then read its full shape with `getActionDefinition` and `getProperties` to see
what parameters a task needs.
<!-- /transport -->

## Discovering what a trigger can do

<!-- transport: mcp uses: listTriggers, searchTriggers, getTriggerDefinition -->
Same pattern for triggers: `listTriggers`/`searchTriggers` to find the one to wire up, then
`getTriggerDefinition` for its parameter shape before setting it on the workflow.
<!-- /transport -->

## Reading a workflow definition

<!-- transport: mcp uses: getWorkflow -->
`getWorkflow` returns the current definition — MCP App-capable clients render it as an interactive
canvas alongside the conversation. It is read-only: it shows a definition but never changes one.
<!-- /transport -->

## Publishing

<!-- transport: mcp uses: publishProject -->
A built or imported workflow is not live until its project is published. Once it looks right, call
`publishProject`.
<!-- /transport -->

## Workspace context

<!-- transport: mcp uses: general -->
Most tools require workspace context. A tool that returns a `workspace_required` error names the
valid ids in its `workspaces` field — retry the same call with one of those as `workspaceId`. This
applies uniformly across ordinary CRUD tools; no special handling is needed when the account has
exactly one workspace.
<!-- /transport -->

## Follow-up: not covered here

Exposing a published workflow as a tool for *other* MCP clients — `createMcpServer` →
`createMcpProject` → `configureMcpServer` → `updateMcpServer` — is a separate job with its own
sequencing (including an enable-guard that fails with the specific unmapped workflows) and belongs
in its own skill, not this one.
