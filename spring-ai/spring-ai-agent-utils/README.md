# spring-ai-agent-utils (vendored fork)

This directory is a **temporary, partial fork** of
[`spring-ai-community/spring-ai-agent-utils`](https://github.com/spring-ai-community/spring-ai-agent-utils).

## Why is this vendored?

ByteChef adopts upstream's Claude memory-tool surface (`AutoMemoryTools`), but needs memory
persisted through `AiAutoMemoryService` (the `ai_auto_memory` table by default, or file storage)
rather than on the local filesystem. Upstream hard-wires `java.nio.file.Path` / `Files` and cannot
be extended (`protected` constructor, `private` I/O helpers). We therefore fork **only that class**
and change it so that:

- read + write of memory content flow through a Spring `Resource` / `WritableResource`
  seam (`MemoryResourceResolver`), and
- list / delete / rename / exists flow through an `AutoMemoryDirectoryOps` SPI,
- the resolver and directory-ops implementations are bound to one memory owner when the tools are
  built, and
- the `@Tool` descriptions are rewritten to document ByteChef's entry format — the frontmatter
  block, the entry-name rules, the length limits — and which edits are rejected.

The fork does not reference `java.nio.file.Files`.

ByteChef's AI Agent Utils Auto Memory tool registers the `AutoMemoryTools` callbacks directly, and
the tool descriptions carry the guidance the model needs, so upstream's `AutoMemoryToolsAdvisor`
(which injects a memory system prompt) is not forked.

## Repackaged, not split

The class is repackaged from `org.springaicommunity.agent.tools` to
`com.bytechef.platform.ai.agent.memory`. The upstream `org.springaicommunity:spring-ai-agent-utils`
artifact stays on the classpath (other agent tools — `AskUserQuestionTool`, `FileSystemTools`,
`GrepTool`, etc. — are still consumed from it), so reusing the upstream package would create a
split package / duplicate-class hazard.

## Source provenance

- **Upstream URL**: https://github.com/spring-ai-community/spring-ai-agent-utils
- **Forked from commit**: `5548e80f5fdaa1f31a84128f5bd25ffaa2e26b40`
- **Upstream license**: Apache License 2.0 (see `LICENSE.txt`)

## Modules

| Local Gradle path | Forked upstream classes |
|---|---|
| `:spring-ai:spring-ai-agent-utils:auto-memory` | `AutoMemoryTools` |

## Removal plan

Drop this directory and restore the upstream `AutoMemoryTools`
once upstream exposes a pluggable, non-filesystem storage backend (a store interface
upstream of the `Path`/`Files` calls). The implementations of this fork's
`MemoryResourceResolver` and `AutoMemoryDirectoryOps` SPIs — `AutoMemoryResourceResolver`
and `ServiceBackedAutoMemoryDirectoryOps` in `server/libs/modules/components/ai/agent/utils` —
would be re-pointed at the upstream extension point, along with `UnavailableAutoMemory`, which
implements both for runs that have no memory. The rewritten tool descriptions would have to be carried
over (or an advisor prompt reinstated), since upstream's descriptions describe a filesystem layout.
