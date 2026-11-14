---
title: Agent Utils Tools
description: Reference for the built-in AI Agent Utils tools you attach to an AI Agent, including skills, task tracking, subagents, approvals, web access, and file and shell tools.
---

**AI Agent Utils** is a component of built-in tools that you attach one by one to the **Tools** slot of an [AI Agent](/platform/automation/build/workflows/ai/agent). Each tool gives the agent a general capability that is not tied to a specific connector: asking the user a question, keeping a task list, delegating to subagents, requiring approval, remembering facts, reading the web, and working with files and a shell. This page describes each tool, its options and the server settings that control it. For how to attach tools, see [Creating an AI Agent](/platform/automation/build/workflows/ai/agent#creating-an-ai-agent).

Some tools are offered only when the server enables them. When a tool is not enabled, it does not appear in the tool picker.

| Tool | Offered |
|---|---|
| **Approval Gate Tool**, **Ask User Question Tool**, **Todo Write Tool**, **Task Tool**, **Auto Memory Tool**, **Smart Web Fetch Tool**, **Skills Tool**, AI skill management tools | Always |
| **Brave Web Search Tool** | When `BYTECHEF_AI_BRAVE_ENABLED` is `true` and `BYTECHEF_AI_BRAVE_API_KEY` is set |
| **File System Tools**, **Glob Tool**, **Grep Tool**, **List Directory Tool** | When `BYTECHEF_AI_AGENT_UTILS_FILE_SYSTEM_ENABLED` is `true` |
| **Shell Tools** | When `BYTECHEF_AI_AGENT_UTILS_SHELL_ENABLED` is `true` |

All settings are described in [Environment variables](/platform/use-bytechef/self-hosted/configuration/environment-variables#ai-agent-utils-configuration).

---

## Interaction

### Ask User Question

The **Ask User Question Tool** lets the agent pause and ask the user for input instead of guessing. The agent asks one to four multiple-choice questions, each with two to four options and a free-text answer. The questions appear as an interactive prompt in the chat, and the run resumes with the answers.

The tool requires the **Chat (stream)** action. With the **Chat** action, the agent continues without asking. In a workflow, the run is suspended until the answer arrives (see [Human in the loop](/platform/automation/build/workflows/human-in-the-loop)). Ask User Question is a clarification for the model, not a decision: to require a decision, use the [Approval Gate Tool](#approval-gate-tool).

### Todo Write

The **Todo Write Tool** gives the agent a task list that it maintains itself. Tasks move from *pending* to *in progress* to *completed*, with one task in progress at a time, and the list is sent back to the agent on every turn. Use it for long, multi-step jobs so that steps are not skipped.

### Approval Gate Tool

The **Approval Gate Tool** requires a human approval before the agent calls specific tools. Attach the gate in the agent's **Tools** slot, then attach the tools to protect inside the gate. In the AI Agent Editor, gated tools appear in a group marked **requires approval**, with its own **Add Tool** and **Add Channel** buttons.

Each call to a gated tool pauses the run and shows the reviewer the tool name and the arguments the model chose. **Approve** runs the call with those arguments. **Discard** returns a denial to the agent, which can choose another approach.

| Property | Type | Default | Description |
|---|---|---|---|
| **Name** | STRING | | Shown in the approval request and used to group the gated tools in the editor. Required. |
| **Expires In** | INTEGER | | How long a request from this gate can be resolved. When empty, the platform default of 60 days applies. |
| **Expires In Unit** | STRING | `DAYS` | `HOURS` or `DAYS`. |

Attach approval channels (for example chat, Slack or email) to the gate to choose where the request is delivered. A gate without channels uses the chat channel. A gate cannot be nested in another gate, and neither a gate nor a **Request Approval** tool can be used inside a subagent. See [Human in the loop](/platform/automation/build/workflows/human-in-the-loop#approval-gate) for channels, expiry and the approval inbox.

---

## Delegation

### Task

The **Task Tool** delegates a sub-task to a subagent that runs in its own context and returns only its result, so the parent agent's context stays small. You define the subagents yourself: attach one **Subagent** element to the Task Tool for each subagent.

| Subagent property | Type | Description |
|---|---|---|
| **Name** | STRING | The identifier the parent agent uses when it delegates to this subagent. Required. |
| **Description** | STRING | When the parent agent should delegate to this subagent. Required. |
| **Instructions** | STRING | The system prompt that tells the subagent how to do its work. Required. |

For example, a support agent can hand order lookups to a subagent that has only the order tools attached to its **Tools** slot:

| Property | Value |
|---|---|
| **Name** | `order-researcher` |
| **Description** | `Use when the customer asks about an order: its status, its items or its delivery date. Pass the order number.` |
| **Instructions** | `Look up the order with your tools. Reply with the order status, the expected delivery date and any problem you found, in at most five bullet points. Do not write to the customer.` |

The **Description** tells the parent agent when to delegate, so write it from the parent agent's point of view. The **Instructions** tell the subagent what to return, so keep the result short: only the result is added to the parent agent's context.

Each subagent can use only the tools attached to its own **Tools** slot. Attach a **Model** to a subagent to choose its model, or attach a **Model** to the Task Tool to cover every subagent that has none. A subagent without a model, when the Task Tool has none either, fails with an error. Subagents can run in the background, and the agent collects their results later. The Task Tool is the building block of the [orchestrator-workers pattern](/platform/automation/build/workflows/ai/agentic-patterns).

To delegate a task to a remote agent over the A2A (Agent2Agent) protocol, use the **A2A Agent Tool** of the [A2A](/reference/components/a2a_v1) component. See [Call an A2A agent from an AI Agent](/platform/automation/deploy/a2a-servers#call-an-a2a-agent-from-an-ai-agent).

---

## Memory and Knowledge

### Auto Memory

The **Auto Memory Tool** stores durable facts that the agent decides to keep, such as preferences, learned context and decisions, and makes them available in every later conversation of the same deployment. It complements chat memory, which recalls only the current conversation. See [Chat memory vs Auto Memory](/platform/automation/build/workflows/ai/agent#chat-memory-vs-auto-memory).

Memory is kept per owner and per environment:

- In deployed automation workflows, the owner is the project deployment, so all workflows of the deployment share one memory.
- Connected-user embedded workflows run as automation workflows, so their memory belongs to the connected user's project deployment. It is not shown on the [Memories](/platform/settings/ai/memories) page.
- Embedded integration workflows keep memory per integration instance.

During editor test runs the tool is offered, but every call returns an error saying that memory works only once the workflow is deployed. In a microservices deployment every call returns an error saying that memory requires the ByteChef server application.

### Skills

The **Skills Tool** lets the agent consult operator-authored [skills](/platform/settings/ai/skills) on demand: packs of instructions, guidelines and process documentation. Select the skills in the tool's **Skills** property; at least one is required. See [Skills](/platform/settings/ai/skills) for authoring and the `.skill` archive format.

The same skills can be given to [Claude Code](/reference/components/claude-code_v1#skills): select them in the **Skills** property of its **Chat** action.

### Managing skills

Five tools let the agent create and change skills during a conversation. The same operations are also available as workflow actions of AI Agent Utils.

| Tool | What it does | Properties |
|---|---|---|
| **Create AI Skill** | Creates a skill from instructions. | **Name** (up to 64 characters, required), **Description** (up to 1,024 characters), **Instructions** (the `SKILL.md` content, required), **Additional Files** |
| **Update AI Skill** | Renames a skill or changes its description or files. | **ID** (required), **Name**, **Description**, **Files** |
| **Append Files to AI Skill** | Adds files to an existing skill archive. | **ID** (required), **Additional Files** (required) |
| **Remove File from AI Skill** | Removes one file from a skill archive. | **ID** (required), **Path** (required) |
| **Delete AI Skill** | Deletes a skill. | **ID** (required) |

---

## Web and Files

### Brave Web Search

The **Brave Web Search Tool** searches the web through the Brave Search API, optionally limited to specific domains, and returns the results to the agent. The advanced **Result Count** option sets the number of results per query (default 10).

The tool uses the server's Brave Search API key, so it needs no connection. It is offered only when the server enables Brave and sets its key ([`BYTECHEF_AI_BRAVE_ENABLED` and `BYTECHEF_AI_BRAVE_API_KEY`](/platform/use-bytechef/self-hosted/configuration/environment-variables#ai-brave-configuration)). To give an agent web search with its own key, attach the **Web Search** action of the [Brave](/reference/components/brave_v1) component as a tool instead.

### Smart Web Fetch

The **Smart Web Fetch Tool** fetches a web page and returns a summary of its content, with caching. Use it when the agent already has the URL of the page it needs. The summary is written by a chat model: attach a **Model** element to the tool. Without one, the tool fails with an error.

| Option | Type | Default | Description |
|---|---|---|---|
| **Domain Safety Check** | BOOLEAN | `true` | Checks each domain against an external domain safety service before fetching it. The domain name is sent to that service. Advanced option. |
| **Max Content Length** | INTEGER | `100000` | Maximum number of characters of fetched content passed to the model. Advanced option. |

Smart Web Fetch refuses URLs whose host resolves to a private, loopback or link-local address, so an agent cannot use it to reach the server's internal network. An administrator can allow specific internal hosts with [`BYTECHEF_AI_AGENT_UTILS_ALLOWED_PRIVATE_HOSTS`](/platform/use-bytechef/self-hosted/configuration/environment-variables#ai-agent-utils-configuration).

### File System, Glob, Grep, and Shell

These tools work with files and a shell: read, write and edit files (**File System Tools**), find files by name (**Glob Tool**), search file contents (**Grep Tool**), list folders (**List Directory Tool**) and run shell commands (**Shell Tools**). Combine them with **Todo Write** and **Task** for agents that carry out multi-step work on files.

File System Tools, Glob Tool, Grep Tool and List Directory Tool are offered only when the server enables them ([`BYTECHEF_AI_AGENT_UTILS_FILE_SYSTEM_ENABLED`](/platform/use-bytechef/self-hosted/configuration/environment-variables#ai-agent-utils-configuration), `false` by default). Shell Tools has its own setting, described below.

#### File System Tools

File System Tools gives the agent three tools for text files: **Read**, **Write** and **Edit**.

- **Read** returns a UTF-8 text file with line numbers, up to 2,000 lines by default. For longer files the agent passes an `offset` (the first line) and a `limit` (the number of lines). Lines longer than 2,000 characters are cut, and binary files such as PDFs, images and office documents are not decoded. Reading a missing file or a folder returns an error.
- **Write** creates a file or replaces an existing one, and creates missing parent folders.
- **Edit** replaces an exact piece of text (`old_string`) with new text (`new_string`). The edit fails when `old_string` is not found, when it appears more than once and `replace_all` is not set, or when both strings are the same. With `replace_all` set, every occurrence is replaced.

The **Read Only** option (default off) offers only **Read**, so the agent can read files but not change them.

#### Glob Tool

**Glob** finds files by name pattern, for example `**/*.java` or `src/**/*.ts`, and returns their paths, most recently modified first. The agent can pass a `path` to search a subfolder instead of the whole workspace. A path that does not exist or is not a folder returns an error.

The advanced **Max Depth** (default 100 folder levels) and **Max Results** (default 1,000 files) options limit how deep and how much it searches.

#### Grep Tool

**Grep** searches file contents with a regular expression (Java syntax). The agent can narrow the search to a `path`, to file names that match a `glob` such as `*.md`, or to a file `type` such as `java` or `py`, and choose an output mode:

- `files_with_matches` (the default) lists the files that contain a match.
- `content` shows the matching lines, with optional line numbers and lines of context before and after each match.
- `count` shows how many matches each file has.

It can also ignore case, match across lines (`multiline`), and page through long results with `head_limit` and `offset`. An invalid regular expression or a missing path returns an error.

The advanced **Max Depth** (default 100 folder levels) and **Max Output Length** (default 100,000 characters) options limit how deep it searches and how much it returns.

#### List Directory Tool

**ListDirectory** lists a folder: subfolders first, then files, both in alphabetical order. It skips common generated folders such as `.git`, `node_modules`, `target`, `build` and `dist`. The agent can pass a `path`, a `depth` (default 1, the folder's direct contents only) and a `limit` on the number of entries (default 50). When the limit is reached, the result says so.

#### Agent workspace

Each of these tools works only inside the agent's own **workspace** folder. Files outside it cannot be reached through them. Workspaces are stored under the server's workspace base folder ([`BYTECHEF_AI_AGENT_UTILS_WORKSPACE_BASEDIR`](/platform/use-bytechef/self-hosted/configuration/environment-variables#ai-agent-utils-configuration)), separated by tenant and then by the run's owner: the project deployment or integration instance, its environment and its workflow:

```
<basedir>/<tenant>/<platform>/<deployment or instance id>/<environment>/<workflow id>/<workspace name>
<basedir>/<tenant>/test/job-<job id>/<workspace name>
```

The second line is for editor test runs. `<platform>` is `automation` or `embedded`, and `<environment>` is the environment name in lowercase, for example `development`.

A workspace belongs to the deployment, not to the person running it. Everyone who runs the same deployed workflow, for example everyone chatting with a published agent, works in the same workspace and can see the files that earlier runs left there. Use separate deployments when users must not share files.

The agent sees its workspace as `/workspace`, never as a path on the server. Each tool's description names the workspace, a relative path is resolved against it, and the paths the tools return start with `/workspace`.

Every tool has an advanced **Workspace Name** option (letters, digits, `-` and `_`; default `default`). Tools in the same workflow with the same workspace name share files, even when they belong to different AI Agents, so a file written by File System Tools is visible to Grep Tool and Shell Tools. Tools with different workspace names do not share files, even within one agent.

Editor test runs get a new workspace for each run. Deployed workflows keep their workspace between runs.

Each ByteChef server removes old workspaces from its own disk once an hour ([`BYTECHEF_AI_AGENT_UTILS_WORKSPACE_CLEANUP_INTERVAL`](/platform/use-bytechef/self-hosted/configuration/environment-variables#ai-agent-utils-configuration)):

- the workspaces of editor test runs, 24 hours after the run ([`BYTECHEF_AI_AGENT_UTILS_WORKSPACE_TEST_RETENTION`](/platform/use-bytechef/self-hosted/configuration/environment-variables#ai-agent-utils-configuration));
- the workspaces of a deleted project deployment or integration instance, of a workflow removed from it, and of a deleted tenant;
- optionally, deployed workspaces that have not changed for a set time ([`BYTECHEF_AI_AGENT_UTILS_WORKSPACE_IDLE_RETENTION`](/platform/use-bytechef/self-hosted/configuration/environment-variables#ai-agent-utils-configuration)). This is off by default.

Removal happens on the next cleanup, not when you delete something. On an installation with several servers, each server cleans up its own workspaces.

Each workspace has a size limit ([`BYTECHEF_AI_AGENT_UTILS_WORKSPACE_MAX_SIZE`](/platform/use-bytechef/self-hosted/configuration/environment-variables#ai-agent-utils-configuration), 1 GB by default, `0` for no limit). A **Write** or **Edit** that would take the workspace over the limit is refused, and the agent is told how much space is used and asked to delete files it no longer needs. Shell commands are checked before and after they run: a command that takes the workspace over the limit still finishes, and the agent is told that the workspace is full. Until files are deleted, only single commands that start with `rm`, `rmdir` or `truncate -s 0`, or `find ... -delete` commands, are run. Commands joined with `;`, `&&`, `|` or a newline, redirections, command substitutions and `find` with `-exec` are refused. One shell command can therefore exceed the limit by the amount it writes.

#### Shell Tools

Shell Tools gives the agent three tools for running commands:

- **Bash** runs a command and returns its output. A command times out after 2 minutes unless the agent asks for a longer `timeout` (up to 10 minutes), and output longer than 30,000 characters is cut. With `run_in_background` set, the command keeps running and the tool returns a shell ID instead of waiting.
- **BashOutput** returns the new output of a background command since the last check, optionally only the lines that match a regular expression.
- **KillShell** stops a background command.

Shell Tools is offered only when the server enables it ([`BYTECHEF_AI_AGENT_UTILS_SHELL_ENABLED`](/platform/use-bytechef/self-hosted/configuration/environment-variables#ai-agent-utils-configuration), `false` by default).

By default, commands run in a **Docker** container that sees only the agent's workspace, mounted at `/workspace`, which is also the commands' working directory and `HOME`. Each workspace gets its own container. The container is reused by every command of the agent and removed after it has been unused for 10 minutes ([`BYTECHEF_AI_AGENT_UTILS_SHELL_DOCKER_IDLE_TIMEOUT`](/platform/use-bytechef/self-hosted/configuration/environment-variables#ai-agent-utils-configuration)). The server needs a Docker-compatible CLI (`docker` or `podman`) on its `PATH`, and the container image must provide `/bin/bash` and `sleep`.

The container uses Docker's default network and runs as the image's default user, usually root. Commands can reach the internet, and they can also reach the server host, other containers and internal addresses such as a cloud metadata endpoint. The private-address rules of Smart Web Fetch do not apply to shell commands. Enable Shell Tools only where the people who build agents are trusted with that network access.

The advanced **Execution Backend** option (default **Docker**) can switch a Shell Tools element to **Local**, which runs commands directly on the server. **Local** is offered only when the server allows it ([`BYTECHEF_AI_AGENT_UTILS_SHELL_ALLOW_LOCAL`](/platform/use-bytechef/self-hosted/configuration/environment-variables#ai-agent-utils-configuration), `false` by default). Local commands start in the workspace with a clean environment, so none of the server's environment variables reach them, but they are not confined to the workspace. Allow **Local** only on a trusted single-tenant installation. Local commands see the workspace at its real path on the server, so refer to workspace files with relative paths.

#### Multi-tenant installations

Workspaces keep tenants' files apart, but the tools are not a complete sandbox. On a server shared by several tenants:

- Keep Shell Tools disabled. A Local shell runs on the server itself with the server's permissions. A Docker shell runs as root on Docker's default network, shares the host's kernel and has no CPU or memory limits.
- Keep the workspace size limit set. It bounds the files that one workspace holds, but not the number of workspaces, so many deployments together can still fill the disk under the workspace base folder.
- The file system tools setting applies to every tenant on the server, so enable it only when every tenant may store files on the server's disk.

---

## Related

- [AI Agent](/platform/automation/build/workflows/ai/agent)
- [Human in the loop](/platform/automation/build/workflows/human-in-the-loop)
- [Skills](/platform/settings/ai/skills)
- [Memories](/platform/settings/ai/memories)
- [AI Agent Utils reference](/reference/components/ai_agent-utils_v1)
