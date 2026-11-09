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

The same skills can be given to [Claude Code](/reference/components/claude-code_v1#skills): add the Agent Utils **Skills** element to the **Skills** slot of a Claude Code node.

---

## Web & Files

### Brave Web Search

Searches the web (with optional domain filtering) via the Brave Search API and returns results the agent can reason over.

Brave Web Search uses the server's Brave Search API key, so it needs no connection. It is offered only when the server enables Brave and sets its key ([`BYTECHEF_AI_BRAVE_ENABLED` and `BYTECHEF_AI_BRAVE_API_KEY`](/platform/use-bytechef/self-hosted/configuration/environment-variables#ai-brave-configuration)); otherwise the tool does not appear in the Tools slot. To give an agent web search with its own key, add the [Brave](/reference/components/brave_v1) component's Web Search action as a tool instead.

### Smart Web Fetch

Fetches a web page and returns an AI-summarized version of its content, with caching - useful for pulling in a specific page the agent already has a URL for.

Smart Web Fetch refuses URLs whose host resolves to a private, loopback or link-local address, so an agent cannot use it to reach the server's internal network. An administrator can allow specific internal hosts with [`BYTECHEF_AI_AGENT_UTILS_ALLOWED_PRIVATE_HOSTS`](/platform/use-bytechef/self-hosted/configuration/environment-variables#ai-agent-utils-configuration).

### File System, Glob, Grep, and Shell

A set of coding-agent-style tools for working with files and a shell: read, write, and edit files (**File System Tools**, with a **Read Only** option), match file patterns (**Glob Tool**), regex-search file contents (**Grep Tool**), and execute shell commands with timeouts and background processes (**Shell Tools**). These pair naturally with **Todo Write** and **Task** for agents that carry out multi-step, file-oriented work.

> **Coming soon.** The server setting that turns on File System Tools, Glob Tool, Grep Tool and List
> Directory Tool is on the upcoming release track and is not yet available in the latest released
> version of ByteChef.

File System Tools, Glob Tool, Grep Tool and List Directory Tool are offered only when the server enables them ([`BYTECHEF_AI_AGENT_UTILS_FILE_SYSTEM_ENABLED`](/platform/use-bytechef/self-hosted/configuration/environment-variables#ai-agent-utils-configuration)); otherwise they do not appear in the Tools slot. Shell Tools has its own setting, described below.

#### File System Tools

Gives the agent three tools for text files: **Read**, **Write** and **Edit**.

- **Read** returns a UTF-8 text file with line numbers, up to 2,000 lines by default. For longer files the agent passes an `offset` (the first line) and a `limit` (the number of lines). Lines longer than 2,000 characters are cut, and binary files such as PDFs, images and office documents are not decoded. Reading a missing file or a folder returns an error.
- **Write** creates a file or replaces an existing one, creating missing parent folders.
- **Edit** replaces an exact piece of text (`old_string`) with new text (`new_string`). The edit fails when `old_string` is not found, when it appears more than once and `replace_all` is not set, or when both strings are the same. With `replace_all` set, every occurrence is replaced, which is useful for renaming.

The **Read Only** option *(coming soon)* offers only **Read**, so the agent can look at files but not change them.

#### Glob Tool

**Glob** finds files by name pattern, for example `**/*.java` or `src/**/*.ts`, and returns their paths with the most recently modified first. The agent can pass a `path` to search a subfolder instead of the whole workspace. Searching a path that does not exist, or is not a folder, returns an error.

The advanced **Max Depth** (default 100 folder levels) and **Max Results** (default 1,000 files) options *(coming soon)* limit how far and how much it searches.

#### Grep Tool

**Grep** searches file contents with a regular expression (Java syntax). The agent can narrow the search to a `path`, to file names matching a `glob` such as `*.md`, or to a file `type` such as `java` or `py`, and choose an output mode:

- `files_with_matches` (the default) lists the files that contain a match.
- `content` shows the matching lines, with optional line numbers and lines of context before and after each match.
- `count` shows how many matches each file has.

It can also ignore case, match across lines (`multiline`), and page through long results with `head_limit` and `offset`. An invalid regular expression or a missing path returns an error.

The advanced **Max Depth** (default 100 folder levels) and **Max Output Length** (default 100,000 characters) options *(coming soon)* limit how far it searches and how much it returns.

#### List Directory Tool

> **Coming soon.** The List Directory tool is on the upcoming release track and is not yet available
> in the latest released version of ByteChef.

**ListDirectory** lists a folder: subfolders first, then files, both alphabetically. It skips common noise folders such as `.git`, `node_modules`, `target`, `build` and `dist`. The agent can pass a `path`, a `depth` (1, the default, lists only the folder's direct contents) and a `limit` on the number of entries (default 50); when the limit is reached, the result says so.

#### Agent workspace

> **Coming soon.** The agent workspace, including the **Workspace Name** option, is on the upcoming
> release track and is not yet available in the latest released version of ByteChef.

Each of these tools works only inside the agent's own **workspace** directory; files outside it are not reachable through them. Workspaces live under the server's workspace base directory ([`BYTECHEF_AI_AGENT_UTILS_WORKSPACE_BASEDIR`](/platform/use-bytechef/self-hosted/configuration/environment-variables#ai-agent-utils-configuration)), separated by tenant and then by the run's owner: the project deployment or integration instance, its environment and its workflow:

```
<basedir>/<tenant>/<platform>/<deployment or instance id>/<environment>/<workflow id>/<workspace name>
<basedir>/<tenant>/test/job-<job id>/<workspace name>
```

The second line is for editor test runs. `<platform>` is `automation` or `embedded`, and `<environment>` is the environment name in lowercase, for example `development`.

A workspace belongs to the deployment, not to the person running it: everyone who runs the same deployed workflow, for example everyone chatting with a published agent, works in the same workspace and can see the files earlier runs left there. Use a separate deployment when users must not share files.

The agent sees its workspace as `/workspace`, never as a path on the server. Each tool's description names the workspace, a relative path is resolved against it, and the paths the tools return start with `/workspace`.

Every tool has an advanced **Workspace Name** option (letters, digits, `-` and `_`, default `default`). Tools in the same workflow with the same workspace name share files, even when they belong to different AI Agents, so a file written by File System Tools is visible to Grep Tool and Shell Tools. Tools with different names do not share files, even within one agent; give them different names to keep their files apart.

Editor test runs get a fresh workspace per run. Deployed workflows keep their workspace between runs.

Each ByteChef server removes old workspaces from its own disk once an hour ([`BYTECHEF_AI_AGENT_UTILS_WORKSPACE_CLEANUP_INTERVAL`](/platform/use-bytechef/self-hosted/configuration/environment-variables#ai-agent-utils-configuration)):

- the workspaces of editor test runs, 24 hours after the run ([`BYTECHEF_AI_AGENT_UTILS_WORKSPACE_TEST_RETENTION`](/platform/use-bytechef/self-hosted/configuration/environment-variables#ai-agent-utils-configuration));
- the workspaces of a deleted project deployment or integration instance, of a workflow removed from it, and of a deleted tenant;
- optionally, deployed workspaces nothing has changed for a set time ([`BYTECHEF_AI_AGENT_UTILS_WORKSPACE_IDLE_RETENTION`](/platform/use-bytechef/self-hosted/configuration/environment-variables#ai-agent-utils-configuration)). This is off by default.

Removal happens on the next cleanup, not at the moment you delete something. On a deployment with several servers, each server cleans up its own workspaces.

> **Coming soon.** The workspace size limit is on the upcoming release track and is not yet available in the latest released version of ByteChef.

Each workspace has a size limit ([`BYTECHEF_AI_AGENT_UTILS_WORKSPACE_MAX_SIZE`](/platform/use-bytechef/self-hosted/configuration/environment-variables#ai-agent-utils-configuration), 1 GB by default, `0` for no limit). A **Write** or **Edit** that would take the workspace over the limit is refused, and the agent is told how much space is used and asked to delete files it no longer needs. Shell commands are checked before and after they run: a command that takes the workspace over the limit still finishes, and the agent is told the workspace is full. Until files are deleted, only single commands that start with `rm`, `rmdir` or `truncate -s 0`, or `find ... -delete` commands, are run. Commands joined with `;`, `&&`, `|` or a newline, redirections, command substitutions and `find` with `-exec` are refused. One shell command can therefore go past the limit by the amount it writes.

#### Shell Tools

Gives the agent three tools for running commands:

- **Bash** runs a command and returns its output. A command times out after 2 minutes unless the agent asks for a longer `timeout` (up to 10 minutes), and output longer than 30,000 characters is cut. With `run_in_background` set, the command keeps running and the tool returns a shell id instead of waiting.
- **BashOutput** returns the new output of a background command since the last check, optionally only the lines matching a regular expression.
- **KillShell** stops a background command.

> **Coming soon.** The Shell Tools setting, the Docker and Local execution backends and the setting
> that allows Local are on the upcoming release track and are not yet available in the latest
> released version of ByteChef.

Shell Tools are offered only when the server enables them ([`BYTECHEF_AI_AGENT_UTILS_SHELL_ENABLED`](/platform/use-bytechef/self-hosted/configuration/environment-variables#ai-agent-utils-configuration)); otherwise the tool does not appear in the Tools slot.

By default commands run in a **Docker** container that sees only the agent's workspace, mounted at `/workspace`, which is also the commands' working directory and `HOME`. Each workspace gets its own container, reused by every command of the agent and removed after it has been unused for a while (10 minutes by default). The server needs a Docker-compatible CLI (`docker` or `podman`) on its `PATH`, and the container image must provide `/bin/bash` and `sleep`.

The container uses Docker's default network and runs as the image's default user, usually root. Commands can reach the internet, and they can also reach the server host, other containers and internal addresses such as a cloud metadata endpoint. The private-address rules of Smart Web Fetch do not apply to shell commands. Enable Shell Tools only where the people who build agents are trusted with that network access.

The advanced **Execution Backend** option can switch a Shell Tools element to **Local**, which runs commands directly on the server. Local is offered only when the server allows it ([`BYTECHEF_AI_AGENT_UTILS_SHELL_ALLOW_LOCAL`](/platform/use-bytechef/self-hosted/configuration/environment-variables#ai-agent-utils-configuration)). Local commands start in the workspace with a clean environment, so none of the server's environment variables reach them, but they are not confined to the workspace: allow Local only on a trusted single-tenant installation. Local commands see the workspace at its real path on the server, so refer to workspace files with relative paths.

#### Multi-tenant installations

Workspaces keep tenants' files apart, but the tools are not a complete sandbox. On a server shared by several tenants:

- Keep Shell Tools disabled. A Local shell runs on the server itself with the server's permissions. A Docker shell runs as root on Docker's default network, shares the host's kernel and has no CPU or memory limits.
- Keep the workspace size limit set. It bounds the files one workspace holds, but not the number of workspaces, so many deployments together can still fill the disk under the workspace base directory.
- The file system tools setting applies to every tenant on the server, so enable it only when every tenant may store files on the server's disk.

---

## Attaching Agent Utils Tools

Attach any of these tools individually in the **Tools** slot of an AI Agent, alongside connector actions and workflow tools. The agent decides at runtime which to call based on the user's request. See [Creating an AI Agent](/platform/automation/build/workflows/ai/agent#creating-an-ai-agent) for the attach flow.
