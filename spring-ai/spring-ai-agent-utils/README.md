# spring-ai-agent-utils (vendored fork)

This directory is a **temporary, partial fork** of
[`spring-ai-community/spring-ai-agent-utils`](https://github.com/spring-ai-community/spring-ai-agent-utils).

## Why is this vendored?

ByteChef adopts upstream's Claude memory-tool surface (`AutoMemoryTools`) and its
`AutoMemoryToolsAdvisor`, but needs memory persisted in the `ai_auto_memory` database
table rather than on the filesystem. Upstream hard-wires `java.nio.file.Path` / `Files`
and cannot be extended (`protected` constructor, `private` I/O helpers). We therefore
fork **only those two classes** and change them so that:

- read + write of memory content flow through a Spring `Resource` / `WritableResource`
  seam (`MemoryResourceResolver`), and
- list / delete / rename / exists flow through an `AutoMemoryDirectoryOps` SPI.

The fork no longer references `java.nio.file.Files`.

## Repackaged, not split

The classes are repackaged from `org.springaicommunity.agent.{tools,advisors}` to
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
| `:spring-ai-agent-utils:auto-memory` | `AutoMemoryTools`, `AutoMemoryToolsAdvisor` |

## Removal plan

Drop this directory and restore the upstream `AutoMemoryTools`/`AutoMemoryToolsAdvisor`
once upstream exposes a pluggable, non-filesystem storage backend (a store interface
upstream of the `Path`/`Files` calls). The DB-backed implementations
(`MemoryResourceResolver`, `AutoMemoryDirectoryOps`) live in ByteChef's
`platform-ai-hub-service` and would be re-pointed at the upstream extension point.
