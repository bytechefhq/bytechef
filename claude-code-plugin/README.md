# claude-code-plugin

This directory holds the `bytechef-dev` Claude Code plugin (`bytechef-dev/`) and the generator that
derives the management MCP server's instructions from it (`plugin-tools/`). See
`docs/superpowers/specs/2026-09-11-claude-plugin-transport-routing-design.md` for the full design;
this file is the quick reference for the one convention a contributor needs to follow.

## Transport fences

Each `SKILL.md` under `bytechef-dev/skills/*/` declares, per operation, which surface performs it —
`mcp` (the Management MCP server), `cli` (the `bytechef` CLI), or `local` (a shell + checkout, no
running instance). Prose and code specific to one surface are wrapped in an HTML comment fence:

```markdown
<!-- transport: mcp uses: createProjectWorkflow -->
Create the workflow with `createProjectWorkflow`.
<!-- /transport -->

<!-- transport: cli uses: component deploy, configure -->
Deploy through the CLI once it's configured (`bytechef configure ...`):

```bash
bytechef component deploy --file ./my-component.js
```
<!-- /transport -->
```

Rules:

- `uses:` is **required** on `mcp` and `cli` fences (optional on `local`). List every tool name or
  CLI command path the block depends on — including ones only mentioned in prose, like `configure`
  above. One exception: an `edition: ee` `mcp` fence keyed on EE-only tools may name CE tools in
  prose without listing them, so that a CE server, which registers those CE tools but none of the EE
  ones, never receives the section (`bytechef-app-builder`'s workflow fence names `createProject`,
  `buildWorkflow` and `publishProject` this way).
- Each entry must match **exactly**: an `mcp` entry must be a tool name actually registered on the
  management MCP server; a `cli` entry must match a `@Command(name = "...")` literal under
  `cli/commands/**/*Command.java` (a multi-word command path, e.g. `embedded code-workflow deploy`,
  is the concatenation of nested `@Command` names).
- A fence may not span a markdown heading, and fences do not nest.
- A fence body must stand alone — it may not say "from above" or otherwise reference another
  fence's tool. Fragments are assembled independently per registered tool, so a sentence naming a
  tool the reader's server doesn't have would dangle.
- An `mcp` fence tagged `uses: general` (or with `uses:` omitted from a non-`mcp`/`cli` fence)
  contributes to the unconditionally-included `## always` section of the generated MCP instructions
  instead of a per-tool section — reserve it for guidance that applies regardless of which tools are
  registered (see the "Workspace context" fence in `bytechef-workflow-builder/SKILL.md`).
- An `edition: ee` attribute marks a block that depends on an EE-only tool. Default is `ce`.

**Do not** put example fence syntax inside a file under `bytechef-dev/skills/` — the parser reads
every file there, and a documented fence would be parsed as a real one.

## Regenerating the MCP instructions

`server/libs/ai/ai-mcp/ai-mcp-server/src/main/resources/bytechef/mcp-instructions.md` is generated
from the `mcp` fences and committed. It is not wired into `compileJava` — regenerate it by hand after
editing a skill's `mcp` fences:

```bash
./gradlew :claude-code-plugin:plugin-tools:generateMcpInstructions
```

## The drift gate

`PluginSkillTagDriftIntTest` (`server/apps/server-app/src/test/java/com/bytechef/ai/mcp/server/config/`)
fails `./gradlew check` (and `./gradlew testIntegration`) when a skill's tags drift from reality: an
`mcp`/`cli` `uses:` entry that names something that doesn't exist, an `edition: ee` fence naming a
tool absent from the CE+EE inventory, a fence whose transport isn't declared in its `SKILL.md`
frontmatter, a required transport with no fence exercising it, or a committed
`mcp-instructions.md` that no longer matches what the generator produces
(`McpInstructionsStalenessTest` in `claude-code-plugin/plugin-tools`, a plain unit test with no
Spring or Docker). If either fails, the fix is almost always: edit the skill, then run the
regeneration command above.
