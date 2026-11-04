---
name: bytechef-ai-agents
description: Use when the user asks to "build an AI Agent in ByteChef", "create a ByteChef agent", "give my agent a Slack tool", "add a knowledge base or skill to the agent", "run the agent on a schedule", "change the agent's model or instructions", or "publish the agent", and the ByteChef management MCP server is connected. Covers configuring the agent yourself, step by step, without an AI provider on the ByteChef server.
transports:
  required: [mcp]
---

# ByteChef AI Agents

Builds ByteChef AI Agents over the management MCP server. An AI Agent is a chat-first automation that
runs on a model instead of a hand-built workflow: instructions, one model with its connection, tools,
skills, knowledge bases, sub-agents and the channels it is reached through. You configure it yourself
with deterministic tool calls; ByteChef generates the agent's workflow from that configuration. If the
server is not connected yet, use the `bytechef-mcp-setup` skill first.

## When to use

<!-- transport: mcp uses: general -->
Use this loop whenever the user wants a ByteChef AI Agent created or changed: a new assistant, a
different model, an extra tool, a schedule, new instructions. Agree on the outline with the user first
(what the agent is for, which model and provider it runs on, which apps it may act in, how it is
reached), then build it one element at a time, so each call stays small and each error points at one
change.

Ids are never guessed. Element and channel ids come from the agent's current configuration; tool
coordinates, skill ids and knowledge base ids come from the searches; connection ids come from the
user's connections. Ask the user before deleting anything.
<!-- /transport -->

## Create or find the agent

<!-- transport: mcp uses: listAiAgents, createAiAgent, getAiAgent -->
`listAiAgents` takes the workspace id and returns every agent in it, with each agent's id and
`projectId`. Use it to find an agent the user names, or to check that a new one does not exist yet.

Create a new agent with `createAiAgent`, passing the workspace id and a title, optionally a
description and the `projectId` of an existing project. Without a `projectId`, the server creates a
new project named after the agent. When the user names a project, find or create it first and pass
its id. A new agent has no model, no tools and only its two permanent channels, `chat` and
`workflowCall`; it cannot be published until it has a MODEL element.

Call `getAiAgent` with the agent id before every change. It returns the instructions, every element
and channel with its id, kind or type, parameters and connection, the settings object, and whether
there are unpublished changes. Use the ids it returns for every later update or delete.
<!-- /transport -->

## Find tools, skills and knowledge bases

<!-- transport: mcp uses: searchAiAgentTools, searchAiAgentReferences -->
An agent tool is a component's tool element, not one of its actions, and only components that
declare one can be added. Call `searchAiAgentTools` with a few words describing what the agent should
be able to do, such as "send slack message" or "current date". Each match has a `componentName`,
`componentVersion` and `actionName`; keep all three exactly as returned.

For a skill or a knowledge base, call `searchAiAgentReferences` with the workspace id, `kind`
`SKILL` or `KNOWLEDGE_BASE`, and the name the user gave as `query` (omit it to list them all). Keep
the `referenceId` of the match unchanged. If several match, ask the user which one to use.
<!-- /transport -->

## Add and change elements

<!-- transport: mcp uses: addAiAgentElement, updateAiAgentElement, deleteAiAgentElement, listConnections -->
Elements are the agent's building blocks. Add each with `addAiAgentElement`, passing the agent id and
the `kind`:

- **MODEL**: the model the agent runs on; exactly one, required before publishing. `parameters` is
  `{"provider": "<model component name, such as anthropic>", "model": "<model id>"}`, plus optional
  model settings under a nested `parameters` object. It also needs a `connectionId`: call
  `listConnections` with the workspace id, filtered by the provider's component name, and use the id
  of the connection the user picks. It lists `DEVELOPMENT` connections unless you pass `environment`
  as `STAGING` or `PRODUCTION`. If none exists, ask the user to create one in the ByteChef UI and
  list again when they are done.
- **TOOL**: `parameters` holds the `componentName`, `componentVersion` and `actionName` from the tool
  search, an optional `parameters` object of fixed inputs and an optional `requiresApproval`. When the
  tool's component needs a connection, find it with `listConnections` the same way.
- **SKILL** and **KNOWLEDGE_BASE**: `referenceId` from the reference search. Repeat for each one.
- **SUB_AGENT**: `referenceId` is another agent's id from the workspace's agent list; cycles are
  rejected.
- **CHAT_MEMORY**: per-conversation memory; one at most, no parameters.
- **APPROVAL_GATE**: one at most; makes every `requiresApproval` tool wait for a human. It takes an
  optional expiry (`approvalExpiresIn` with `approvalExpiresInUnit` `HOURS` or `DAYS`).
- **APPROVAL_TOOL**: one at most, no parameters; gives the model its own "ask a human" tool.

To change an element, call `updateAiAgentElement` with the agent id, the element id and the new
`parameters` or `connectionId`. `parameters` replaces the stored map as a whole, so send every key
you want to keep. To swap the model, update the MODEL element rather than adding a second one.
Remove an element with `deleteAiAgentElement` only after the user confirms; removing the MODEL
element blocks publishing until a new one is added.
<!-- /transport -->

## Channels

<!-- transport: mcp uses: addAiAgentChannel, deleteAiAgentChannel -->
Channels are how the agent is reached. Every agent keeps `chat` and `workflowCall`; they cannot be
added again or deleted. Add another with `addAiAgentChannel`, passing the agent id and a
`channelType` such as `schedule`, `slack` or `telegram`. An unknown type is rejected with the list of
types this server supports and which of them need a `connectionId`.

A `schedule` channel needs no connection. Its `parameters` require `prompt` (the message the agent
receives on every run) and `expression`, a cron expression such as `0 9 ? * MON-FRI` for weekdays at
9:00, with an optional `timezone` (default UTC). For the other channel types, leave `parameters`
empty unless the user gives values.

Remove a channel with `deleteAiAgentChannel` and the channel id, only after the user confirms; the
agent stops being reachable through it.
<!-- /transport -->

## Instructions and settings

<!-- transport: mcp uses: updateAiAgent, updateAiAgentSettings -->
`updateAiAgent` changes the agent's title, description and `instructions`, the system prompt the
model follows on every turn. Only the fields you send change. Write the instructions with the user:
who the agent is, what it may do, which tools to use for what, and when to ask before acting.

`updateAiAgentSettings` replaces the whole settings object: `streamResponse` and the `builtInTools`
switches. Any key you leave out resets to its default, so start from the agent's current settings,
change only what the user asked for and send the complete object.
<!-- /transport -->

## Test and publish

<!-- transport: mcp uses: publishProject -->
Every change is live in the agent's test chat in the ByteChef UI straight away, so the user can try
it there. Deployed channels see it only after publishing. An agent is published by publishing
its project: call `publishProject` with the agent's project id and a short description of what
changed, when the user asks or agrees. It versions every workflow and agent in that project.
<!-- /transport -->

## When to use buildAiAgent

<!-- transport: mcp uses: buildAiAgent -->
`buildAiAgent` hands a plain-language request to an AI agent running on the ByteChef server, which
configures the agent for you. Use it only when the user asks for it, or when a large build would fill
your context. Pass the workspace id, the instruction, and either a `projectId` for a new agent or an
`aiAgentId` to change an existing one. It needs an AI provider configured on the ByteChef server. If
it reports that no provider is available, do not retry; build the agent yourself with the agent
tools. Each call is independent and re-reads the current agent, so restate the context it needs with
every call. When it answers with a question, call it again with the original instruction plus the
answer.
<!-- /transport -->
