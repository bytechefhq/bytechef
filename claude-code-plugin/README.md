# claude-code-plugin

Skills that let Claude, Claude Code, Codex and other coding agents build ByteChef workflows over the
management MCP server, packaged as the `bytechef-dev` Claude Code plugin.

## Layout

```
claude-code-plugin/
  bytechef-dev/
    .claude-plugin/plugin.json      plugin manifest (name, version, description, author)
    skills/
      bytechef-mcp-setup/SKILL.md         connect a client to the management MCP server
      bytechef-workflow-builder/SKILL.md  author, save, connect and publish a workflow
```

Each skill is a portable `SKILL.md`: a folder named after the skill, holding a Markdown file with YAML
frontmatter. The same files work as a Claude Code plugin skill, a Claude (claude.ai or desktop) skill
upload and a plain skills folder for other coding agents.

## Skill conventions

- **Name:** lowercase and hyphenated, matching the folder name (`bytechef-workflow-builder`).
- **Frontmatter:** `name`, `description` (with the phrases that should trigger the skill) and
  `transports`, which lists the surfaces the skill needs under `required:` and those it can use under
  `optional:`. `mcp` is the management MCP server; `local` is a shell and files on the user's machine.
- **Fences:** text that depends on specific MCP tools is wrapped in an HTML comment fence naming them:

  ```markdown
  <!-- transport: mcp uses: listProjects, createProject -->
  Call `listProjects` to find the project, or `createProject` to make one.
  <!-- /transport -->
  ```

  - Every `uses:` entry is a tool registered on the management MCP server of the branch the skill
    lands on.
  - `uses: general` marks guidance that applies whatever tools are registered.
  - A `local` fence needs no `uses:`.
  - A fence never spans a markdown heading, fences never nest, and a fence body never mentions a tool
    named only in another fence, so each fence still reads correctly on its own.
- **Content:** guidance and the authoring loop, not parameter-by-parameter tool documentation. Tool
  parameters are described by the tools themselves.

Do not put example fence syntax inside a file under `bytechef-dev/skills/`; it would be read as a real
fence.

## Packaging

One command builds every client's output from these sources:

```bash
sh claude-code-plugin/package.sh [outDir]
```

`outDir` defaults to `build/claude-code-plugin` and receives:

- `claude-code/bytechef-dev/`: the plugin directory, also installable from the repository with
  `/plugin marketplace add` through `.claude-plugin/marketplace.json` at the repository root;
- `claude/<skill>.zip`: one zip per skill for upload to Claude, with the skill folder at the zip root;
- `agents/skills/`: a copy of all skill folders for Codex and other coding agents, plus MCP
  configuration snippets.
