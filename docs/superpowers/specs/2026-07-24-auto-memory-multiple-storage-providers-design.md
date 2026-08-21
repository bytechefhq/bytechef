# Auto-memory: multiple storage providers (JDBC, filesystem, AWS)

**Date:** 2026-07-24
**Status:** Implemented — JDBC (default), FILESYSTEM and AWS providers, with a cross-provider contract suite
**Author:** Ivica Cardic

## Motivation

AI auto-memory persists per-principal memories (`ai_auto_memory`) and their workspace membership
(`workspace_ai_auto_memory`). Today the only backend is relational JDBC. Deployments that do not want
auto-memory in the primary database — air-gapped/single-node installs, or scale-out installs that
prefer object storage — have no option.

The storage seam already exists and was designed for this: `AiAutoMemoryRepository` and
`WorkspaceAiAutoMemoryRepository` (in `platform-ai-auto-memory-repository-api`) are plain Java
interfaces, and the former's javadoc states *"Backend-agnostic on purpose: the JDBC implementation
lives in `platform-ai-auto-memory-repository-jdbc`. Workspace-aware queries JOIN through
`workspace_ai_auto_memory` — non-JDBC backends MUST honor the documented filter shape."* Only the
JDBC binding was ever written.

## Goal

Support `JDBC` (today, unchanged and default), `FILESYSTEM`, and `AWS` as auto-memory storage
providers, selected by configuration, with identical observable behavior for the documented query
contract.

## Reference patterns in this repo

- **Built-in chat memory** (`server/libs/config/ai-chat-memory-config/*`): `bytechef.ai.memory.provider`
  selects among jdbc/aws/redis/in-memory; each provider module pairs a `@ConditionalOnProperty`
  `@Configuration` with an `EnvironmentPostProcessor` that excludes the non-selected Spring AI
  autoconfiguration.
- **File storage** (`ApplicationProperties.FileStorage`): a nested `Provider {AWS, FILESYSTEM, JDBC}`
  enum plus `FileStorageServiceRegistry.getFileStorageService(provider.name())`.
- **Feature-specific provider over the shared registry** — the precedent this design follows
  (`FileStorageConfiguration.triggerFileStorage`): a feature reads its OWN provider property and
  resolves an implementation from the SHARED registry:
  ```java
  OutputStorage.Provider provider = applicationProperties.getWorkflow().getOutputStorage().getProvider();
  return new TriggerFileStorageImpl(fileStorageServiceRegistry.getFileStorageService(provider.name()));
  ```

## Key constraint driving the design

Auto-memory's contract requires real queries:

```java
findByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentOrderByUpdatedAtDesc(
    long workspaceId, int principalType, long principalId, int environment)
```

filter on four fields plus `ORDER BY updatedAt DESC`.

**Correction (2026-07-24): the listing capability already exists.** An earlier draft of this design
claimed `FileStorageService` had no listing method and proposed adding `list()` to the SPI. That was
wrong — `Set<FileEntry> getFileEntries(String directory)` is already declared on
`FileStorageService` (`file-storage-api`, line 41) and implemented by all three backends
(`FilesystemFileStorageService`, `Base64FileStorageService`, EE `AwsFileStorageServiceImpl`). **No SPI
change is required**, which removes the largest risk from this feature.

What the existing implementations actually do — and where they disagree:

| Aspect | `FilesystemFileStorageService` | EE `AwsFileStorageServiceImpl` |
|---|---|---|
| Traversal | `Files.walk(dir)` — **recursive**, all nested files | `s3Template.listObjects(bucket, prefix)` — prefix match, effectively recursive |
| Returned `FileEntry.name` | `path.getFileName()` → `memo.json` | `filename.substring(filename.lastIndexOf('/'))` → `/memo.json` (**leading slash**) |
| Metadata | name + URL only | name + URL only |

Two real defects fall out, both pre-existing and both must be addressed for cross-provider parity:

1. **AWS name off-by-one**: `substring(lastIndexOf('/'))` includes the separator; it should be
   `lastIndexOf('/') + 1`. Today the two providers return different name formats for the same object.
2. **No metadata on `FileEntry`** — there is no `lastModified`/size. Ordering therefore MUST come from
   the `updatedAt` field inside each memory's JSON, not from file metadata. This is acceptable because
   the repository has to read the content anyway to return `AiAutoMemory` objects, but it means a
   workspace query reads every object in the principal's subtree.

Recursive-by-default listing is fine — in fact useful — for this design, because the directory layout
is chosen so a query's directory prefix already narrows to exactly the rows it wants.

## Decisions (locked)

1. **No SPI change.** Reuse the existing `getFileEntries(String directory)`; fix the AWS name
   off-by-one so providers agree, and pin the agreed semantics with tests.
2. **One CE repository implementation** over `FileStorageService`; `FILESYSTEM` and `AWS` are the same
   code path, differing only in which `FileStorageService` the registry returns. **No new EE module** —
   AWS works automatically wherever the EE `AwsFileStorageService` bean is on the classpath.
3. **Best-effort, single-writer semantics** for file-backed providers: whole-object writes,
   last-write-wins, no cross-object transaction. Documented, not hidden.
4. **Fail fast** when the selected provider has no registered `FileStorageService`
   (`FileStorageServiceRegistry.getFileStorageService` returns `null`, it does not throw).
5. **Workspace membership folded into the memory object** for file backends (no second collection).
6. **Ordering comes from JSON content** (`updatedAt`), never from file metadata.

## Architecture

### 1. Harmonize the existing `getFileEntries` (no new SPI method)

`Set<FileEntry> getFileEntries(String directory)` already exists on `FileStorageService`. Required
work is corrective, not additive:

- **EE `AwsFileStorageServiceImpl`**: fix `filename.substring(filename.lastIndexOf('/'))` →
  `filename.substring(filename.lastIndexOf('/') + 1)` so the returned name matches the filesystem
  provider (no leading slash). This is a pre-existing bug; any current caller comparing names across
  providers is already affected.
- **Document + pin the contract** on the interface javadoc: recursive/prefix traversal, `FileEntry`
  carries name+URL only, ordering unspecified, empty set when the directory does not exist, and it
  must not throw for a missing directory.
- **`FilesystemFileStorageService`**: `Files.walk` throws `NoSuchFileException` for a missing
  directory — return an empty set instead, so a query for a principal with no memories yet behaves
  like an empty result rather than an error.
- **`Base64FileStorageService`**: encodes content into the `FileEntry` itself and keeps no directory,
  so there is genuinely nothing to enumerate — confirm it returns an empty set and does not throw.
  Base64 is not offered as an auto-memory provider.
- **S3 pagination**: verify `S3Template.listObjects` returns all keys rather than a single 1000-key
  page; if it truncates, page with a continuation token. Covered by a test that writes >1000 objects,
  or — if that is too slow for CI — an explicit note plus a smaller bound test.

### 2. New CE module `platform-ai-auto-memory-repository-file-storage`

Implements both existing interfaces:
- `FileStorageAiAutoMemoryRepository implements AiAutoMemoryRepository`
- `FileStorageWorkspaceAiAutoMemoryRepository implements WorkspaceAiAutoMemoryRepository`

**Storage layout** — the directory path encodes the filter tuple so listing stays narrow and the
common queries need no scan of unrelated data:

```
ai_auto_memory/{workspaceId}/{principalType}_{principalId}/{environment}/{id}.json
```

**Corrected during implementation.** An earlier draft used hyphens
(`ai-auto-memory/.../{principalType}-{principalId}/...`) and an explicit `{tenantId}` segment. Both were
wrong:

- `FilesystemFileStorageService` normalizes directories to `[0-9a-zA-Z/_]` and lowercases them, so every
  hyphen is stripped. The hyphenated layout would have written to one path and listed another, silently
  returning **zero memories**. (The underlying write/read asymmetry is fixed, but the layout still avoids
  stripped characters — otherwise `ai-auto-memory` and `ai_auto_memory` collide into the same directory.)
- Tenant isolation is already applied inside `FileStorageService` (the filesystem backend resolves under
  `baseDir/{tenantId}`), so repeating `{tenantId}` in the layout would have nested it twice.

**Serialization**: one JSON document per memory, holding every `AiAutoMemory` field (`id`,
`principalId`, `principalType`, `name`, `title`, `description`, `memoryType`, `environment`,
`content`, `createdAt`, `updatedAt`) **plus** the workspace membership (`workspaceId`) that the JDBC
backend keeps in `workspace_ai_auto_memory`. Folding membership into the object keeps every write a
single-object write, which is what makes last-write-wins coherent; a document store does not need the
join table the relational model requires.

**Query implementation**: `getFileEntries(directory)` for the encoded tuple → read each object →
deserialize → filter remaining predicates (e.g. `memoryType`, `name`) →
`sort(comparing(updatedAt).reversed())`. Ordering comes from the JSON `updatedAt` field, since
`FileEntry` exposes no metadata. The ordering is part of the contract ("Consumers depend on the
ordering") and is enforced by the shared contract tests.

**`findById(long)` / `deleteById(long)`**: id alone does not encode the directory. Resolve by listing
the tenant subtree and matching id. Document the cost; acceptable because these are point operations
on a small per-principal corpus, and the workspace-scoped queries (the hot path) are narrow.

**IDs**: file backends have no DB sequence. Generate a positive `long` derived from a UUID (never
`hashCode()`, per the repo's ID rule), preserving the existing `findById(long)` contract.

### 3. Provider selection

`ApplicationProperties` gains, under the existing `Ai` section:

```java
bytechef.ai.auto-memory.provider = JDBC (default) | FILESYSTEM | AWS
```

as a typed nested `AutoMemory` class with a `Provider` enum (the class is
`@ConfigurationProperties(prefix = "bytechef", ignoreUnknownFields = false)`, so an undeclared
property fails startup binding — it MUST be declared).

Wiring:
- `JDBC` → today's `platform-ai-auto-memory-repository-jdbc` beans, unchanged. This is the default, so
  **existing deployments are byte-for-byte unaffected**.
- `FILESYSTEM` / `AWS` → the file-storage repositories, constructed with
  `fileStorageServiceRegistry.getFileStorageService(provider.name())`.
- If that lookup returns `null` (e.g. `AWS` selected in a CE deployment without the EE AWS module),
  **throw at startup** with an actionable message naming the provider and the missing module. Never
  silently fall back to another provider — a silent fallback would write memories somewhere the
  operator did not choose.
- The existing JDBC repository beans must become conditional so exactly one implementation of each
  interface is active.

## Semantics and limitations (explicit)

- **Last-write-wins**: two concurrent edits of the SAME memory can clobber one another on file
  backends. Auto-memory's real usage is an agent maintaining its own memories, so this is acceptable;
  it is documented rather than silently tolerated.
- **Duplicate-name check stays at the service layer.** The repository javadoc already designates
  `AiAutoMemoryServiceImpl.create()` as the policy gate and notes the DB no longer enforces name
  uniqueness, so no behavior is lost relative to JDBC.
- **No cross-object transaction** on file backends.
- **Tenant isolation** is by path prefix (`{tenantId}` as the first path segment).

## Testing

A **shared contract test suite** exercising the documented behavior (filter shape, `updatedAt DESC`
ordering, membership scoping, duplicate names, delete/`findById`) is run against every backend:
- JDBC (existing `AiAutoMemoryRepositoryIntTest` pattern, Testcontainers PG),
- filesystem (temp directory),
- AWS (LocalStack via Testcontainers, matching the existing `AwsFileStorageIntTest` setup).

This is the mechanism that prevents providers from drifting apart. Plus tests pinning
`getFileEntries` semantics per implementation (name format identical across filesystem and AWS,
empty set for a missing directory, S3 pagination) and the fail-fast provider resolution.

## Risks

- **Cross-provider `getFileEntries` divergence** (the AWS leading-slash bug) — fixed and pinned by a
  test asserting both providers return the same name for the same object.
- **S3 listing pagination**: if `S3Template.listObjects` truncates at 1000 keys, a workspace with many
  memories silently loses rows. Must be verified, not assumed.
- **Read amplification**: ordering by `updatedAt` requires reading every object in the queried
  directory because `FileEntry` carries no metadata. Acceptable — the repository must read content
  anyway to return `AiAutoMemory` — but it makes the directory layout load-bearing for performance.
- **`findById` cost** on file backends (subtree scan) — documented; hot-path queries are narrow.
- **Base64 backend** is not a meaningful auto-memory target; it is not offered as a provider value.

## Rollout

Additive and default-off: the provider defaults to `JDBC`, so existing deployments see no change. No
schema change. The relational tables remain the source of truth for JDBC deployments; there is **no
migration path between providers** in this scope (switching providers does not move existing
memories) — called out so it is not mistaken for a supported operation.
