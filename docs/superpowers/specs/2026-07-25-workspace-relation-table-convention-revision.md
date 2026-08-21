# Workspace relation tables: convention revision (scoped)

**Date:** 2026-07-25
**Status:** Implemented — all 28 unreleased tables collapsed; the 5 released tables retained (see the duplicate audit in `docs/superpowers/notes/`)
**Author:** Ivica Cardic

**Supersedes (partially):** `2026-05-06-workspace-relation-tables-design.md`. That design deliberately
replaced `workspace_id` columns with `workspace_<entity>` relation tables across 17 spec'd + 5 ad-hoc
tables, to make entities workspace-agnostic and the link "explicit and shareable". This document does
**not** undo that work. It revises the rule going forward and fixes one dev-only table, on evidence
that the shareability the tables were built for is unused and that a better sharing mechanism already
exists in the codebase.

## Evidence

Gathered 2026-07-25 against the current tree:

1. **34 live `workspace_*` relation tables.** (35 declared; `workspace_ai_observability_notification_channel`
   is created in init and dropped by `20260720000004_migrate_notification_channels.xml`.)
2. **Exactly one is genuinely many-to-many: `workspace_user`.** A user really does belong to several
   workspaces.
3. **The other 33 are 1:1 in practice.** Each relation class is instantiated at exactly one site,
   creating one row per entity. `WorkspaceAiHubPersonalAgent` has two sites, but both are in the same
   service (create and clone paths), each writing a single row for a single workspace.
4. **The shareability is never exercised.** There is no share/move/add-to-workspace operation anywhere
   in the codebase for any of the 33.
5. **No table enforces the 1:1 it relies on.** Every unique constraint is the composite
   `UNIQUE(workspace_id, entity_id)`; none declares `UNIQUE(entity_id)`. So the schema permits an
   entity in two workspaces everywhere, while the code assumes it never happens.
6. **A better sharing model already exists.** `ConnectionVisibility` models sharing as owner + reach —
   `PRIVATE(0) < WORKSPACE(1) < ORGANIZATION(2)` — with a `canTransitionTo` state machine and pinned
   ordinals. Membership rows give multi-membership but no owner and no permission gradient, so
   "shared" would mean uniformly shared with no way to express who owns it.
7. **Deployment status, established against `master` and the released tag `v0.31.2`** (commit dates
   are misleading — `0_732` is a long-lived branch, so a table added in May can still be unreleased):
   - **29 tables exist only on `0_732`** and are therefore not in production. 28 are live candidates
     (the 29th, `workspace_ai_observability_notification_channel`, is already dropped).
   - **6 are on `master` and in release `v0.31.2`**: `workspace_api_key`, `workspace_connection`,
     `workspace_data_table`, `workspace_knowledge_base`, `workspace_mcp_server`, and `workspace_user`.
     Excluding `workspace_user` (genuine many-to-many) leaves **5 released candidates**.

## Why the scope is what it is

An earlier draft of this document scoped the collapse to a single table, believing 32 of the 33 were
in production. Fact 7 inverts that: only **5** are released, and **28 are unreleased**, which makes
them init-changelog edits with no data migration — the same reasoning the May 2026 design used when it
edited init files in place.

So the collapse covers **all 28 unreleased tables**. The 5 released ones are excluded: collapsing them
means real migrations against customer data (add column, backfill, verify, drop table) for a refactor
with no user-facing benefit. That trade is worth taking when it is free and not worth taking when it
is not.

## Decisions

### 1. Rule change, going forward

For a **new** platform-package entity:

- Give it a `workspace_id` column. Do not create a `workspace_<entity>` relation table.
- If it later needs to be shared beyond its owning workspace, add a `visibility` enum modeled on
  `ConnectionVisibility` (owner preserved, reach widened through gated transitions). Sharing is
  additive to a column — it does not require a relation table.
- Create a relation table **only** for a genuinely many-to-many relationship with no owner concept,
  i.e. the `workspace_user` shape. The bar is: can the same row legitimately belong to two workspaces
  with equal standing, today, with an API that does it?

This supersedes the "Variant A everywhere" instruction in the May design for new work only.

### 2. Collapse all 28 unreleased relation tables

Per table, uniformly:

- The entity gains a `workspace_id` column; the `workspace_<entity>` table is removed.
- Edit the init changelog **in place** (unreleased — same approach the May design used for the same
  reason). No data migration.
- The `Workspace<Entity>` domain class, its repository interface, and its JDBC implementation are
  deleted, along with the membership write in the owning service.
- Workspace-aware repository queries stop JOINing and filter on the column.

`workspace_ai_auto_memory` is the exemplar: it is the only one with a second (file-storage) binding and
a cross-provider contract suite, so it goes first and proves the shape.

**One commit per table** (owner decision). This branch is long-lived and gets rebased against master,
so a 28-table mega-commit would be painful to merge and impossible to bisect. Each table's schema,
entity, repository, service and test changes land together in a single self-contained commit that
leaves the build green.
- `AiAutoMemoryRepository`'s workspace-aware queries stop JOINing and filter on the column.
- `WorkspaceAiAutoMemoryRepository` and `WorkspaceAiAutoMemory` are deleted, along with the membership
  write in `AiAutoMemoryServiceImpl.create()`.
- The file-storage binding already carries `workspaceId` on the memory document, so this makes the
  relational binding consistent with it rather than the other way round. The
  `saveInWorkspace` hook in `AiAutoMemoryRepositoryContractTests` collapses to a plain `save`, and
  `FileStorageWorkspaceAiAutoMemoryRepository`'s `UnsupportedOperationException` on `save` disappears
  with the interface.
- The cross-provider contract suite must stay green throughout — it is the regression net for this
  change.

**The column is NULLABLE** (`Long`, not `long`). An earlier draft of this document specified `NOT NULL`
on the grounds that it would make an orphan memory unrepresentable. That was wrong: **embedded has no
concept of a workspace**, and auto-memory already spans both surfaces —
`AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE` is the embedded principal. A `NOT NULL` column would
force embedded rows to invent a workspace.

This is a pre-existing defect, not one this change introduces: `AiAutoMemoryServiceImpl` currently takes
a primitive `long workspaceId` on every method and unconditionally writes a membership row, so an
embedded memory cannot be represented today either. The collapse must therefore widen the type:

- The entity's `workspaceId` field is `Long` (boxed), never primitive `long`.
- The DB column has no `constraints` element.
- `AiAutoMemoryDocument.workspaceId` (file-storage binding) is `Long`, not the primitive `long` it
  currently uses — otherwise the file backends inherit the same inability to represent an absent
  workspace, and the two backends diverge again.
- **Existing service method signatures are NOT changed** (owner decision). They keep their current
  `long workspaceId` parameters.

The last point has a consequence worth stating plainly rather than implying otherwise: **this change
does not deliver embedded support.** It makes the schema and entity *capable* of representing a
workspace-less row, but every service entry point still demands a workspace, so nothing can create one
yet. Embedded support would additionally require widening those signatures and their callers — a
separate, larger change deliberately out of scope here. The value taken now is that the column will not
have to be migrated later when that work happens.

There is no reliable per-table signal for deciding `NOT NULL` selectively: none of the candidate
entities carries a `type`/`PlatformType` column, and auto-memory's embedded dimension comes from
`AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE` (a principal type), which does not generalise. So the
rule is uniform. A specific entity may opt into `NOT NULL` later as a deliberate, evidenced exception.

Consequence to accept honestly: a nullable column reopens the orphan case for automation rows — a
memory with a null workspace that no workspace query returns. The real invariant is conditional
("automation principals have a workspace; embedded principals do not"), which a plain `NOT NULL` cannot
express. Two options, deferred rather than decided here:

1. Leave it to the service layer (status quo for every other nullable scoping column), or
2. Add a CHECK constraint keyed on `principal_type` (`INTEGRATION_INSTANCE` ⇒ NULL, otherwise NOT NULL).

Option 2 is stricter but would need revisiting if embedded later gains workspaces, which the owner
flagged as possible ("for now"). Do not add it as part of this change without an explicit decision.

A separate simplification still comes free: `FileStorageAiAutoMemoryRepository.save(memory)` currently
resolves the workspace by looking up the existing stored document, because the domain object does not
carry it. Once `AiAutoMemory` has `workspaceId`, that overload and its lookup disappear.

### 3. Duplicate audit for the 5 released tables

The released tables keep their relation tables, so their unenforced 1:1 invariant remains. Do **not**
add `UNIQUE(entity_id)` blind: on production data the constraint fails if duplicates already exist, and
a failed migration on customer data is worse than the gap it closes.

Instead, ship a read-only audit first — for each of the 5:

```sql
SELECT <entity>_id, COUNT(*) FROM workspace_<entity> GROUP BY <entity>_id HAVING COUNT(*) > 1;
```

Any hit is a live bug: that entity is currently visible in two workspaces with no code path that
intended it. The audit's output decides the next step per table (add the constraint, or investigate
the duplicates first). Adding the constraints is explicitly **out of scope here** — this document
only commits to producing the evidence.

## Non-goals

- Collapsing the 5 released relation tables. Explicitly rejected on risk/benefit.
- Adding `UNIQUE(entity_id)` to released tables in this change (audit first).
- **Changing any existing service method signature.** Owner decision: the sweep stays mechanical
  (schema, entity, repository). Services keep their current `long workspaceId` parameters. See the
  consequence recorded under the nullability decision.
- Introducing `visibility` on any entity now. It is the designated mechanism when sharing is actually
  wanted, not something to add speculatively — that would repeat the mistake this document is
  correcting.
- Changing `workspace_user`.

## Risks

- **Auto-memory collapse touches a shipped-in-dev schema.** Mitigated: dev-only by the owner's
  confirmation, init edited in place, and the contract suite covers both bindings.
- **Rule change creates a mixed codebase** — 32 tables on the old pattern, new entities on the new
  one. Accepted deliberately: the alternative is 32 production migrations. The rule must be documented
  in `CLAUDE.md` so the inconsistency is understood as intentional rather than read as drift.
- **The audit may surface real duplicates.** That is the point; it converts an unknown into a known.
