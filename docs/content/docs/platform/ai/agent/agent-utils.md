---
title: Agent Utils Tools
description: A closer look at the built-in Agent Utils tools you can attach to an AI Agent — asking the user questions, tracking a todo list, delegating to sub-agents, long-term memory, web access, and file/shell tools.
---

The **Agent Utils** toolset is a collection of built-in tools you attach individually to an [AI Agent](/platform/ai/agent) in the Tools slot. Each one gives the agent a general-purpose capability that isn't tied to a specific connector. This page describes what each tool does and when to reach for it; the [AI Agent overview](/platform/ai/agent) shows how to attach them.

---

## Interaction

### Ask User Question

Lets the agent **pause mid-run and ask the user for input** instead of guessing. The agent poses one to four multiple-choice questions (each with two to four options, plus an always-available free-text answer), the questions render as an interactive prompt in the chat UI, and the run resumes with the user's answers.

In a workflow AI Agent this uses the platform's [suspend/resume](/automation/build/human-in-the-loop) mechanism — the execution parks durably until the answer arrives, so it can wait as long as needed. Reach for it whenever the agent needs a preference, a clarification, or a decision it shouldn't assume.

### Todo Write

Gives the agent an **external checklist** it maintains itself. Tasks move through *pending → in progress → completed*, with only one task in progress at a time, and the list is fed back into the agent every turn. This keeps a long, multi-step job from silently dropping steps — the classic "lost in the middle" failure. The agent typically reaches for it automatically once a task involves several distinct steps.

---

## Delegation

### Task

Lets the agent **delegate a complex sub-task to a specialized sub-agent** that runs in its own isolated context and reports back only its result — keeping the parent agent's context clean. Built-in sub-agent types cover common shapes (read-only exploration, general-purpose work, planning, and shell operations), and several can run in parallel. Attach a **Model** child to the Task tool to choose which model the sub-agents run on. This is the building block for the [orchestrator-workers agentic pattern](/platform/ai/agentic-patterns).

### Agent Client

Delegates a task to a **remote, external agent** over the open [A2A (Agent2Agent) protocol](https://a2aproject.github.io/A2A/). The remote agent's capabilities are discovered from its agent card, and the task is sent as an A2A message — letting your agent collaborate with agents built on other frameworks.

---

## Memory & Knowledge

### Auto Memory

Persists **durable facts** the agent decides are worth keeping — preferences, learned context, decisions — and makes them available in **every future conversation** of the same deployment. It complements chat memory (which only recalls the current conversation). Memory is scoped to the project deployment in automation workflows, or to the tenant's integration instance in embedded workflows, so one tenant's agent never sees another's memories. During editor test runs the tool is inert — nothing is written until the workflow runs under a real deployment. See [Chat memory vs Auto Memory](/platform/ai/agent#chat-memory-vs-auto-memory).

### Skills

Lets the agent **consult operator-authored [skills](/platform/ai/agent/skills) on demand** — reusable packs of instructions, guidelines, and process documentation. See the [Skills](/platform/ai/agent/skills) page for authoring and the `.skill` archive format.

---

## Web & Files

### Brave Web Search

Searches the web (with optional domain filtering) via the Brave Search API and returns results the agent can reason over.

### Smart Web Fetch

Fetches a web page and returns an AI-summarized version of its content, with caching — useful for pulling in a specific page the agent already has a URL for.

### File System, List Directory, Glob, Grep, and Shell

A set of coding-agent-style tools for working with files and a shell in the agent's sandboxed environment: read, write, and edit files (**File System**), list directories (**List Directory**), match file patterns (**Glob**), regex-search file contents (**Grep**), and execute shell commands with timeouts (**Shell**). These pair naturally with **Todo Write** and **Task** for agents that carry out multi-step, file-oriented work.

---

## Attaching Agent Utils Tools

Attach any of these tools individually in the **Tools** slot of an AI Agent, alongside connector actions and workflow tools. The agent decides at runtime which to call based on the user's request. See [Creating an AI Agent](/platform/ai/agent#creating-an-ai-agent) for the attach flow.
