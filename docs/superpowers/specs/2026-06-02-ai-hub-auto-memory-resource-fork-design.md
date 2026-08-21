# AI Hub Auto-Memory: adopt upstream AutoMemoryTools/Advisor with a Spring Resource read/write seam

- **Date:** 2026-06-02
- **Status:** Design — pending review
- **Author:** Ivica Cardic
- **Branch:** `0_732`

## Summary

Replace ByteChef's six bespoke auto-memory `ToolCallback` classes
(`List/Read/Create/Update/Delete/RenameAutoMemoryToolCallback`) with the upstream
[`spring-ai-community/spring-ai-agent-utils`](https://github.com/spring-ai-community/spring-ai-agent-utils)
Claude memory-tool surface (`MemoryView`, `MemoryCreate`, `MemoryStrReplace`,
`MemoryInsert`, `MemoryDelete`, `MemoryRename`) plus its `AutoMemoryToolsAdvisor`
(system-prompt augmentation + consolidation trigger).

Upstream is hard-wired to `java.nio.file.Path` / `Files` and cannot be extended
(constructor is `protected`, the I/O is inlined in `private` helpers). We therefore
**vendor-fork only those two classes** into a new root module `spring-ai-agent-utils/`,
following the existing `spring-ai-tool-search-tool/` precedent, and change them so that:

- **read + write of memory content** flow through a Spring `Resource` /
  `WritableResource` seam (instead of `Files.readString` / `Files.writeString`), and
- **list / delete / rename / exists** flow through a thin `AutoMemoryDirectoryOps` SPI
  (instead of `Files.list` / `Files.walk` / `Files.delete` / `Files.move`).

The fork no longer references `java.nio.file.Files` at all.

The `ai_auto_memory` **database table remains the source of truth.** ByteChef supplies
DB-backed implementations of both seams over the existing `AiAutoMemoryService`, scoped
per `(workspace, user, environment)` resolved from the Spring AI `ToolContext`. The
existing REST/GraphQL surface and the memory-management UI are untouched, because they
sit on `AiAutoMemoryService`, which does not change.

Additionally, remove the dead "reverser" vaporware: the unused `reversible` flag on
`AiHubTaskArtifactKind`, the `ArtifactReverser` Javadoc references (the class never
existed), and the unused `AiHubTaskArtifactStatus.REVERSED` status (Java + GraphQL +
client).

## Goals

1. Expose the upstream `AutoMemoryTools` + `AutoMemoryToolsAdvisor` as the AI Hub BUILD
   agent's memory tool surface, replacing the six bespoke callbacks.
2. Route memory **content read + write** through a Spring `Resource` abstraction so the
   content backend is pluggable (the explicit motivation: "memory can be stored even
   inside a database").
3. Keep the `ai_auto_memory` table as the source of truth; preserve multi-tenant
   isolation per `(workspace, user, environment)`.
4. Keep the existing memory REST/GraphQL APIs and management UI working unchanged.
5. Remove the reverser vaporware (`reversible` flag, `ArtifactReverser` mentions,
   `REVERSED` status).

## Non-goals

- Implementing an actual undo/reversal pipeline (none exists today). LLM-driven memory
  changes become irreversible — this is an accepted, deliberate outcome.
- A *generic* pluggable `ResourceLoader` for arbitrary backends (filesystem, S3, etc.).
  The Resource seam is the abstraction point, but the only implementation shipped is the
  DB-backed one over `ai_auto_memory` (per D4, the content path is DB-backed from day one).
- Removing or migrating the `ai_auto_memory` table, `AiAutoMemoryService`, its
  repositories, REST/GraphQL, or the management UI.
- Removing the dormant `MEMORY_*` values from `AiHubTaskArtifactKind` (blocked by the
  ordinal-stability rule — see Decisions).

## Key decisions (resolved during brainstorming)

| # | Decision | Rationale |
|---|----------|-----------|
| D1 | Vendor-fork **only** `AutoMemoryTools` + `AutoMemoryToolsAdvisor` into a new root module `spring-ai-agent-utils/`. | Upstream can't be subclassed (`protected` ctor, `private` `Files` I/O). Mirrors the `spring-ai-tool-search-tool/` vendored-fork precedent. |
| D2 | `Resource` / `WritableResource` seam covers **read + write content only**. | The user's explicit scope: "I just want it for read+write." |
| D3 | list / delete / rename / exists go through a thin **`AutoMemoryDirectoryOps` SPI**, not `Files`. | The DB is the source of truth, so these can't run against a filesystem. A small SPI is cleaner than bending `ResourcePatternResolver` to express delete/rename, which `Resource` cannot do natively. |
| D4 | **DB (`ai_auto_memory`) stays the source of truth.** | User's explicit choice. Keeps REST/GraphQL + UI working. |
| D5 | Per-tenant isolation via `ToolContext`: forked `@Tool` methods take a `ToolContext` param; ByteChef resolves `(workspace, user, environment)` from `AiHubToolInvocationContext`. | Upstream's flat single-dir model has no tenant notion; `ToolContext` is the only per-request channel since the tools object is a singleton built by the advisor. |
| D6 | **Drop undo/audit artifact recording** for memory. | No reverser exists anywhere; recording was write-only. Removing the six callbacks removes the recording automatically. |
| D7 | **Keep** the `MEMORY_*` enum constants (ordinals 11–14) dormant. | Ordinal-stability rule: they sit mid-enum; deleting them shifts ordinals 15–22 and corrupts persisted `ai_hub_task_artifact` rows. Pinned by `EnumOrdinalStabilityTest`. |
| D8 | **Remove** the `reversible` flag, `ArtifactReverser` Javadoc, and `AiHubTaskArtifactStatus.REVERSED`. | Pure vaporware. `reversible` is read nowhere; `ArtifactReverser` exists only in Javadoc; `REVERSED` is never set and is the last ordinal (safe to remove). |
| D9 | `MEMORY.md` becomes a **read-only virtual index** synthesized from `AiAutoMemoryService.listByUserAndWorkspace`. | DB is truth; there is no real MEMORY.md file. Replaces today's `MemoryIndexResolver` system-prompt injection with an on-demand `MemoryView("MEMORY.md")` read. |

## Architecture

### Module layout (new vendored fork)

```
spring-ai-agent-utils/                         (new root module dir; mirrors spring-ai-tool-search-tool/)
├── README.md                                  (provenance: upstream commit 5548e80, Apache-2.0, removal plan)
├── LICENSE.txt                                (Apache License 2.0)
└── auto-memory/
    ├── build.gradle.kts                       (java-library-conventions; spring-ai-bom, spring-ai-client-chat, spring-ai-model)
    └── src/main/
        ├── java/org/springaicommunity/agent/
        │   ├── tools/AutoMemoryTools.java     (forked: Resource read/write + AutoMemoryDirectoryOps SPI)
        │   ├── tools/AutoMemoryDirectoryOps.java   (NEW SPI: list/delete/rename/exists — part of the fork)
        │   └── advisors/AutoMemoryToolsAdvisor.java (forked: takes AutoMemoryTools instance + Resource prompt)
        └── resources/prompt/
            └── AUTO_MEMORY_TOOLS_SYSTEM_PROMPT.md   (default prompt resource the advisor references)
```

`settings.gradle.kts` gains `include("spring-ai-agent-utils:auto-memory")` next to the
existing `spring-ai-tool-search-tool:*` includes (lines 29–30).

> **Fork constraint honored:** the fork contains the two upstream classes plus the
> minimal additions required to make them compile against the new seams: the
> `AutoMemoryDirectoryOps` interface and the default prompt resource. No ByteChef logic
> lives in the fork.

### The two seams (in the forked `AutoMemoryTools`)

```java
// Read+write content seam (Spring Resource). Resolves a relative memory path -> Resource.
// ByteChef supplies a DB-backed resolver; the default impl can be a FileSystemResource resolver.
interface MemoryResourceResolver {            // or reuse org.springframework.core.io.ResourceLoader
    Resource resolve(String relativePath, @Nullable ToolContext toolContext);
}

// Metadata seam (D3). Replaces upstream's Files-based directory operations.
public interface AutoMemoryDirectoryOps {
    String list(@Nullable String path, @Nullable ToolContext toolContext);   // MemoryView on a directory / MEMORY.md
    boolean exists(String path, @Nullable ToolContext toolContext);
    void delete(String path, @Nullable ToolContext toolContext);
    void rename(String oldPath, String newPath, @Nullable ToolContext toolContext);
}
```

Forked `@Tool` method mapping after the change:

| `@Tool` method | Backed by |
|----------------|-----------|
| `MemoryView` (file) | `MemoryResourceResolver.resolve(...).getInputStream()` |
| `MemoryView` (dir / `MEMORY.md`) | `AutoMemoryDirectoryOps.list(...)` (virtual index for `MEMORY.md`, D9) |
| `MemoryCreate` | `WritableResource.getOutputStream()` (write); `exists()` precheck |
| `MemoryStrReplace` | resolve → read → replace → write (all via `Resource`) |
| `MemoryInsert` | resolve → read → insert → write (all via `Resource`) |
| `MemoryDelete` | `AutoMemoryDirectoryOps.delete(...)` |
| `MemoryRename` | `AutoMemoryDirectoryOps.rename(...)` |

Each `@Tool` method gains a `ToolContext toolContext` parameter (Spring AI injects it via
`MethodToolCallbackProvider`), threaded into both seams for tenant scoping (D5).

### Forked `AutoMemoryToolsAdvisor`

- Builder drops `memoriesRootDirectory(String)`.
- Builder gains `autoMemoryTools(AutoMemoryTools)` (the already-constructed, seam-backed
  instance) and keeps `memorySystemPrompt(Resource)`, `order(int)`,
  `memoryConsolidationTrigger(BiPredicate<...>)`.
- `before(...)` keeps the existing behavior: augment the system message with the memory
  system prompt (+ optional consolidation reminder) and append the memory tool callbacks
  to `ToolCallingChatOptions`. No tenant logic here — scoping is per-call via `ToolContext`.

### ByteChef EE wiring (`automation-ai-hub-service`)

- New `DbAutoMemoryResourceResolver` and `DbAutoMemoryDirectoryOps` (EE, `@version ee`,
  Enterprise license header) implemented over `AiAutoMemoryService`. Both resolve
  `(workspaceId, userId, environment)` from `AiHubToolInvocationContext.fromToolContext(...)`
  and reuse `AutoMemoryToolSupport`'s slug + size validation and `contextError` guards.
  - **content read** → `AiAutoMemoryService.read(...).getContent()` as an
    `InputStreamResource` / `ByteArrayResource`.
  - **content write** → parse frontmatter + body, then `create(...)` / `update(...)`.
  - **list** → synthesize the `MEMORY.md` virtual index from `listByUserAndWorkspace(...)`
    (D9), reusing the formatting currently in `buildMemoryIndexResolver`.
  - **delete / rename** → `AiAutoMemoryService.delete(...)` / `rename(...)`.
- In `AiHubConfiguration`:
  - Delete `registerAutoMemoryToolCallbacks(...)` and its call site.
  - Build the seam-backed `AutoMemoryTools`, wrap it in the forked `AutoMemoryToolsAdvisor`,
    and register the advisor on the BUILD agent via the existing
    `AiHubSpringAIAgent.Builder.advisor(...)` seam.
  - Remove `.memoryIndexResolver(buildMemoryIndexResolver(...))` and the
    `buildMemoryIndexResolver` helper (superseded by the virtual `MEMORY.md`, D9). Confirm
    whether `AiHubSpringAIAgent.MemoryIndexResolver` has other consumers; if not, remove it.

### Data flow (write example)

```
LLM calls MemoryCreate(path="user_profile.md", fileText="---\n...---\nbody")
  → forked AutoMemoryTools.memoryCreate(path, fileText, toolContext)
    → MemoryResourceResolver.resolve("user_profile.md", toolContext)   // tenant-scoped
      → DbAutoMemoryResourceResolver: AiHubToolInvocationContext.fromToolContext(toolContext)
      → WritableResource.getOutputStream() writes content
        → DbAutoMemory...: parse frontmatter → AiAutoMemoryService.create(workspaceId,userId,env,name,title,desc,type,content)
  → returns "Successfully created file: user_profile.md"
```

## What is removed vs. kept

**Removed:**
- `ReadAutoMemoryToolCallback`, `CreateAutoMemoryToolCallback`,
  `UpdateAutoMemoryToolCallback`, `DeleteAutoMemoryToolCallback`,
  `RenameAutoMemoryToolCallback`, `ListAutoMemoriesToolCallback` (six callbacks) and their
  four `*Test` classes (`Create/Update/Delete/Rename...CallbackTest`).
- `registerAutoMemoryToolCallbacks` + `buildMemoryIndexResolver` in `AiHubConfiguration`.
- `AiHubTaskArtifactKind.reversible` field, constructor arg, `reversible()` accessor, and
  the `ArtifactReverser` / `ArtifactReverserRegistry` Javadoc (3 sites: the enum + the two
  memory-callback Javadocs, which are deleted with the callbacks).
- `AiHubTaskArtifactStatus.REVERSED` (Java enum value) + its `EnumOrdinalStabilityTest`
  assertion + GraphQL `AiHubTaskArtifactStatus.REVERSED` + client `graphql-types.ts`
  `Reversed`, `tasks.api.ts` union member, and the `AiHubArtifactHistoryPage` display map
  entry.
- `AutoMemoryToolSupport.recordArtifact` / `recordMissingThreadId` (undo recording, D6) —
  unless still used elsewhere; the rest of `AutoMemoryToolSupport` (context resolution,
  validation, timestamp) is **reused** by the new DB-backed seam impls.

**Kept (unchanged):**
- `ai_auto_memory` table, `AiAutoMemory`, `AiAutoMemoryType`, `AiAutoMemoryService` +
  repositories, and all memory REST/GraphQL + management UI (D4).
- `AiHubTaskArtifactKind.MEMORY_CREATED/UPDATED/DELETED/RENAMED` constants — dormant but
  ordinal-stable (D7). `EnumOrdinalStabilityTest` / `WireFormatTest` assertions for these
  stay as-is.
- All non-memory artifact recording (`FILE_CREATED`, `WORKFLOW_*`, `DATA_TABLE_*`, `KB_*`,
  `*_REFERENCED`) — those call sites never read `reversible()`, so dropping the flag is
  inert for them.
- `AiHubTaskArtifactStatus.APPLIED/EXPIRED/IRREVERSIBLE`.

## Per-tenant isolation & security

- Every forked `@Tool` method receives `ToolContext`; the DB-backed seams call
  `AiHubToolInvocationContext.fromToolContext(...)` and reject the call with the existing
  `contextError(...)` message when workspace/user is absent. There is no flat shared store
  and no filesystem path, so the upstream path-traversal guard is not needed (DB scoping by
  `(workspace, user, environment)` is the isolation boundary). Slug validation
  (`^[a-z0-9_-]{1,64}$`) and size bounds are enforced in the write seam via
  `AiAutoMemory` setters / `AutoMemoryToolSupport`.

## Testing strategy

- **Fork unit tests** (in `spring-ai-agent-utils:auto-memory`): drive `AutoMemoryTools`
  with in-memory fakes of `MemoryResourceResolver` + `AutoMemoryDirectoryOps`; assert each
  `@Tool` method calls the right seam and formats output like upstream (view/create/
  str_replace/insert/delete/rename, plus error paths: missing file, duplicate create,
  non-unique `old_str`).
- **DB-seam tests** (EE service module): `DbAutoMemoryResourceResolver` /
  `DbAutoMemoryDirectoryOps` over a mocked `AiAutoMemoryService`; assert tenant resolution
  from `ToolContext`, frontmatter parse/round-trip, `MEMORY.md` virtual-index synthesis,
  and `contextError` on missing context.
- **Advisor test**: forked `AutoMemoryToolsAdvisor.before(...)` augments the system message
  and appends the tool callbacks; consolidation-trigger branch toggles the reminder.
- **Wiring**: a focused test that the BUILD agent registers the advisor and no longer
  registers the six callbacks.
- **Cleanup regression**: `EnumOrdinalStabilityTest` updated to drop the `REVERSED`
  assertion (and confirm `MEMORY_*` kinds remain at 11–14); GraphQL schema + client
  `npm run check` after removing `REVERSED`.
- Delete the four obsolete memory-callback test classes.

## Known limitations

- `delete` / `rename` / `list` are **not** expressed through Spring `Resource` — `Resource`
  cannot model them. They use the `AutoMemoryDirectoryOps` SPI instead (D3). The "use
  Spring Resource" requirement is satisfied for the read+write content path only, as
  scoped.
- No undo for LLM-driven memory changes (D6). Acceptable: no reverser ever existed.
- The forked `MemoryView` directory-listing output format is synthesized by the SPI rather
  than mirroring upstream's byte-size filesystem listing; the LLM-facing contract for
  `MEMORY.md` is the virtual index (D9).

## Fork removal plan

Drop `spring-ai-agent-utils/auto-memory` and restore the upstream
`org.springaicommunity:spring-ai-agent-utils` dependency once upstream exposes a supported
extension point for a non-filesystem storage backend (e.g. a pluggable store interface
upstream of the `Path`/`Files` calls). Tracked in the module README, mirroring the
`spring-ai-tool-search-tool/README.md` removal-plan section.
