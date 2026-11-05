---
title: Agent Utils Tools
description: A closer look at the built-in Agent Utils tools you can attach to an AI Agent - consulting skills, tracking a todo list, delegating to sub-agents, web access, and file/shell tools.
---

The **Agent Utils** toolset is a collection of built-in tools you attach individually to an [AI Agent](/platform/automation/build/workflows/ai/agent) in the Tools slot. Each one gives the agent a general-purpose capability that isn't tied to a specific connector. This page describes what each tool does and when to reach for it; the [AI Agent overview](/platform/automation/build/workflows/ai/agent) shows how to attach them.

---

## Interaction

### Ask User Question

> **Coming soon.** The Ask User Question tool is on the upcoming release track and is not yet
> available in the latest released version of ByteChef.

Lets the agent **pause mid-run and ask the user for input** instead of guessing. The agent poses one to four multiple-choice questions (each with two to four options, plus an always-available free-text answer), the questions render as an interactive prompt in the chat UI, and the run resumes with the user's answers.

In a workflow AI Agent this uses the platform's [suspend/resume](/platform/automation/build/workflows/human-in-the-loop) mechanism - the execution parks durably until the answer arrives, so it can wait as long as needed. Reach for it whenever the agent needs a preference, a clarification, or a decision it shouldn't assume.

### Todo Write

Gives the agent an **external checklist** it maintains itself. Tasks move through *pending → in progress → completed*, with only one task in progress at a time, and the list is fed back into the agent every turn. This keeps a long, multi-step job from silently dropping steps - the classic "lost in the middle" failure. The agent typically reaches for it automatically once a task involves several distinct steps.

---

## Delegation

### Task

Lets the agent **delegate a complex sub-task to a specialized sub-agent** that runs in its own isolated context and reports back only its result - keeping the parent agent's context clean. Built-in sub-agent types cover common shapes (read-only exploration, general-purpose work, planning, and shell operations), and several can run in parallel. Attach a **Model** child to the Task tool to choose which model the sub-agents run on. This is the building block for the [orchestrator-workers agentic pattern](/platform/automation/build/workflows/ai/agentic-patterns).

To delegate a task to a remote agent over the A2A (Agent2Agent) protocol, use the **A2A Agent Tool** of the [A2A](/reference/components/a2a_v1) component - see [Call an A2A agent from an AI Agent](/platform/automation/deploy/a2a-servers#call-an-a2a-agent-from-an-ai-agent).

---

## Memory & Knowledge

### Auto Memory

> **Coming soon.** Auto Memory is on the upcoming release track and is not yet available in the
> latest released version of ByteChef.

Persists **durable facts** the agent decides are worth keeping - preferences, learned context, decisions - and makes them available in **every future conversation** of the same deployment. It complements chat memory (which only recalls the current conversation). Memory is kept per owner **and** per environment. In deployed automation workflows the owner is the project deployment. Connected-user embedded workflows run as automation workflows, so their memory belongs to the connected user's project deployment (it is not shown on the [Memories](/platform/automation/ai/memories) page); embedded integration workflows keep memory per integration instance. During editor test runs the tool is still offered, but every call returns an error explaining that memory works only once the workflow is deployed. In a microservices deployment the tool is unavailable: every call returns an error saying memory requires the ByteChef server application. See [Chat memory vs Auto Memory](/platform/automation/build/workflows/ai/agent#chat-memory-vs-auto-memory).

### Skills

Lets the agent **consult operator-authored [skills](/platform/automation/ai/skills) on demand** - reusable packs of instructions, guidelines, and process documentation. See the [Skills](/platform/automation/ai/skills) page for authoring and the `.skill` archive format.

---

## Web & Files

### Brave Web Search

Searches the web (with optional domain filtering) via the Brave Search API and returns results the agent can reason over.

### Smart Web Fetch

Fetches a web page and returns an AI-summarized version of its content, with caching - useful for pulling in a specific page the agent already has a URL for.

Smart Web Fetch refuses URLs whose host resolves to a private, loopback or link-local address, so an agent cannot use it to reach the server's internal network. An administrator can allow specific internal hosts with [`BYTECHEF_AI_AGENT_UTILS_ALLOWED_PRIVATE_HOSTS`](/platform/use-bytechef/self-hosted/configuration/environment-variables#ai-agent-utils-configuration).

### File System, Glob, Grep, and Shell

A set of coding-agent-style tools for working with files and a shell: read, write, and edit files (**File System Tools**, with a **Read Only** option), match file patterns (**Glob Tool**), regex-search file contents (**Grep Tool**), and execute shell commands with timeouts and background processes (**Shell Tools**). These pair naturally with **Todo Write** and **Task** for agents that carry out multi-step, file-oriented work.

A separate **List Directory** tool is on the upcoming release track and is not yet available in the latest released version of ByteChef.

#### Agent workspace

Each of these tools works only inside the agent's own **workspace** directory; files outside it are not reachable through them. Workspaces live under the server's workspace base directory ([`BYTECHEF_AI_AGENT_UTILS_WORKSPACE_BASEDIR`](/platform/use-bytechef/self-hosted/configuration/environment-variables#ai-agent-utils-configuration)), separated by tenant and then by the run's owner: the project deployment or integration instance, its environment and its workflow.

Every tool has an advanced **Workspace Name** option (letters, digits, `-` and `_`, default `default`). Tools of the same agent with the same workspace name share files, so a file written by File System Tools is visible to Grep Tool and Shell Tools. Give tools different names to keep their files apart.

Editor test runs get a fresh workspace per run, removed after 24 hours. Deployed workflows keep their workspace between runs.

#### Shell Tools

Shell Tools are offered only when the server enables them ([`BYTECHEF_AI_AGENT_UTILS_SHELL_ENABLED`](/platform/use-bytechef/self-hosted/configuration/environment-variables#ai-agent-utils-configuration)); otherwise the tool does not appear in the Tools slot.

By default commands run in a **Docker** container that sees only the agent's workspace, mounted at the same path. Each workspace gets its own container, reused by every command of the agent and removed after it has been unused for a while (10 minutes by default). The server needs a Docker-compatible CLI (`docker` or `podman`) on its `PATH`, and the container image must provide `/bin/bash` and `sleep`.

The container uses Docker's default network and runs as the image's default user, usually root. Commands can reach the internet, and they can also reach the server host, other containers and internal addresses such as a cloud metadata endpoint. The private-address rules of Smart Web Fetch do not apply to shell commands. Enable Shell Tools only where the people who build agents are trusted with that network access.

The advanced **Execution Backend** option can switch a Shell Tools element to **Local**, which runs commands directly on the server. Local is offered only when the server allows it ([`BYTECHEF_AI_AGENT_UTILS_SHELL_ALLOW_LOCAL`](/platform/use-bytechef/self-hosted/configuration/environment-variables#ai-agent-utils-configuration)). Local commands start in the workspace with a clean environment, so none of the server's environment variables reach them, but they are not confined to the workspace: allow Local only on a trusted single-tenant installation.

---

## Attaching Agent Utils Tools

Attach any of these tools individually in the **Tools** slot of an AI Agent, alongside connector actions and workflow tools. The agent decides at runtime which to call based on the user's request. See [Creating an AI Agent](/platform/automation/build/workflows/ai/agent#creating-an-ai-agent) for the attach flow.
