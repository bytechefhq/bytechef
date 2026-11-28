---
name: bytechef-viewers
description: Use when the user asks to "show me the ... workflow", "open the ... data table", "let me see the rows of ...", "show the file ...", "show the source of the code workflow ...", "open the custom component ...", or otherwise wants to look at a ByteChef workflow, data table, file, code workflow or custom component, and the ByteChef management MCP server is connected. Covers finding the object's id and opening it in the read-only viewer instead of pasting its content as text.
transports:
  required: [mcp]
---

# ByteChef Viewers

Some read tools on the ByteChef management MCP server render their result as an interactive,
read-only viewer in clients that support MCP Apps (Claude Desktop, claude.ai). When the user wants to
look at one of these objects, call the read tool: the viewer is the answer. If the server is not
connected yet, use the `bytechef-mcp-setup` skill first.

## Which tool opens which viewer

<!-- transport: mcp uses: getWorkflow, queryDataTable, getAssetFileContent, getCodeWorkflowSource, getCustomComponentSource -->
| The user wants to see | Read tool | How to find the id |
|---|---|---|
| A workflow (its canvas) | `getWorkflow` | `listWorkflows` with a `query`, or `searchWorkflows` across a workspace |
| The rows of a data table | `queryDataTable` | No list tool; ask the user for the `dataTableId` shown in the ByteChef UI |
| A text file stored as an asset | `getAssetFileContent` | No list tool; ask the user for the id shown in the ByteChef UI |
| A code workflow's source | `getCodeWorkflowSource` | `listCodeWorkflows`, then match the name |
| A custom component's source | `getCustomComponentSource` | `listCustomComponents`, then match the name |

`queryDataTable` and `getAssetFileContent` also take a `workspaceId` (from `listWorkspaces`) and an
`environment` (default `DEVELOPMENT`). The code workflow and custom component tools exist only on
ByteChef editions that serve them; if a tool is missing, say so instead of guessing.
<!-- /transport -->

## How to answer

<!-- transport: mcp uses: general -->
- Resolve names to ids yourself with the list tools. Ask the user only when no list tool covers the
  object or when several matches fit the name.
- Call the read tool even when you could describe the object from earlier output; the viewer shows
  the current version.
- Do not repeat the content as text. The client already displays it, so reply with one or two
  sentences: what is shown and anything worth noticing (an error, an empty table, an unusual step).
- In a client without MCP Apps the tool still returns the content as text; then summarize it, and
  quote only the part the user asked about.
- These tools are read-only. To change what the viewer shows, use the authoring tools (see the
  `bytechef-workflow-builder` skill) and call the read tool again afterwards.
<!-- /transport -->
