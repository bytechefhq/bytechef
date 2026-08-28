# Separate embedded and automation resource pools (data tables, knowledge bases)

**Status:** design, approved 2026-08-29. Sub-project 1 of 2.

**Goal.** A data table or knowledge base belongs to exactly one of two pools — automation or embedded
— and a workflow run for a connected user can never reach the automation pool.

## Why

The `/embedded` Data Tables and Knowledge Bases pages list the tenant's automation resources.
Reported from the running dev instance: the console showed `table1`, `table22`, `table3`,
`conversations` and `invoices`, all of which are automation's.

The cause is an asymmetry between the two list paths.

| | scoping applied |
|---|---|
| `WorkspaceDataTableFacadeImpl.listTables` | intersects `workspace_data_table` for the workspace |
| `EmbeddedDataTableApiFacadeImpl.getDataTables` | owner only — and "All owners" applies no filter |

The obvious fix — exclude workspace-linked resources from the embedded list — does not work, because
there is no such thing as an embedded resource today. Verified against the dev database: all seven
`data_table` rows and the single `knowledge_base` row are linked to workspace 1049 and none carries an
owner. `WorkspaceDataTableFacadeImpl.createTable` and `WorkspaceKnowledgeBaseFacadeImpl
.createKnowledgeBase` are the only creation paths and both unconditionally assign a workspace. Filtering
workspace-linked resources out would leave both embedded pages permanently empty.

So the pools have to be made to exist.

## Decisions

Settled with the user 2026-08-29; do not relitigate.

- **Two pools, and the split is real, not presentational.**
- **Every existing resource becomes AUTOMATION.** The embedded pool starts empty. Accepted consequence:
  the `ai-conversation-assistant` demo's `conversations`, `assistants` and `messages` become
  automation-only and its integration workflows stop resolving them until they are re-created on the
  embedded side.
- **The vendor creates embedded resources in the `/embedded` console.** Not API-only, not through
  automation with a pool selector.
- **The console reaches full parity with automation** — create, column editing and the row grid. That is
  sub-project 2 and is out of scope here.
- **Visibility is one-way.** A run for a connected user sees the embedded pool only. A run for nobody —
  the vendor — sees both.
- **A vendor run reaches a connected user's rows only by naming that user.** Reading every account's rows
  implicitly is not acceptable.

## The model

Three cases. Alice is a connected user.

| run | pool | rows |
|---|---|---|
| Alice's integration workflow | embedded only | Alice's + unowned |
| Alice's **bridged** automation workflow | embedded only | Alice's + unowned |
| vendor job, no account named | both | unowned only |
| vendor job, account named | both | that account's + unowned |

**The rule underneath all of them: scope on who the run is *for*, not on where the workflow was
authored.**

That distinction is load-bearing rather than stylistic. `PlatformType` separates the wrong two cases:

| run | PlatformType | owner |
|---|---|---|
| Alice's integration workflow | EMBEDDED | connectedUser(Alice) |
| Alice's bridged automation workflow | **AUTOMATION** | connectedUser(Alice) |
| vendor's own automation workflow | AUTOMATION | *empty* |

Rows 2 and 3 share a platform type and must see different things; rows 1 and 2 differ in platform type
and must see the same thing. The embedded bridge dispatches with `PlatformType.AUTOMATION` and a
`ProjectDeploymentId` in place of an `IntegrationInstance` id (`.agents/embedded-bridge.md`), so an
automation workflow is frequently the embedded surface. Scoping the runtime on platform type would hand
Alice's run the vendor's `invoices` table and hide her own data.

`ConnectedUserOwnerResolver` already resolves this correctly — for a non-EMBEDDED principal it finds the
connected user through `connected_user_project` — so the owner is available at the same seam and needs no
new machinery.

### Two ownership axes

Ownership exists at two independent levels and they do **not** follow the same rule. A table can be
unowned while its rows are Alice's, and vice versa.

| axis | who filters | rule for a vendor run naming no account |
|---|---|---|
| **table** — `data_table.owner_id` | `DataTableServiceImpl.isReadableBy` | **unfiltered**: every table in the pool, assigned or not |
| **row** — `owner_id` on each `dt_*`/`edt_*` table | `RowOwnerFilter` | **unowned rows only** |

The asymmetry is deliberate, not an oversight. The two levels answer different questions: a table
assignment is metadata the vendor manages and must be able to see in order to manage, while row contents
are the account's data. So a vendor run sees a table assigned to Alice and reads nothing inside it until
it names her.

Filtering both levels the same way was the alternative and is rejected because it makes the console's
assignment view unusable — you would have to name an account before you could see what is assigned to
them.

An owned run is unaffected by this: table-level `isReadableBy` already hides another account's table from
Alice, and the row filter already hides another account's rows.

**Invariant: a run with an owner never sees the AUTOMATION pool. No exception, no flag.** A bridged
workflow therefore cannot read even a harmless shared lookup table from the automation pool. Reference
data that account workflows need belongs in the embedded pool as unowned rows, which every account
already sees. The model absorbs the need rather than taking an exception, deliberately: the first
exception is what turns an invariant into a policy.

## Architecture

### The discriminator

`platform_type INT NOT NULL DEFAULT 0` on `data_table` and `knowledge_base`, holding a `PlatformType`
ordinal (`AUTOMATION = 0`, `EMBEDDED = 1`).

A column rather than an `embedded_data_table` relation table: the conventions in `CLAUDE.md` reserve
relation tables for genuine many-to-many with no owner concept, the six `workspace_*` tables are legacy
and deliberately not extended, and embedded has no parent id to relate *to* — such a table would be a
boolean wearing a join's clothes.

Not inferred from the absence of a workspace link, which was the cheapest option and is rejected on
evidence: `duplicateTable` shipped without assigning the copy to a workspace (fixed 2026-08-29,
`1e842972a21`), silently manufacturing a resource that a
pool-by-absence rule would have classified as embedded. Defining a pool by absence means every future
path that forgets a write creates a phantom member, indistinguishable from a legitimate one.

The field is `private int platformType` with converting accessors (`PlatformType.values()[platformType]`
/ `.ordinal()`), never a mapped enum field — Spring Data JDBC would persist the String name against an
INT column. `int` rather than `Integer` because, unlike `owner_type`, there is no meaningful null.

### Physical naming

```
AUTOMATION → dt_<envId>_<baseName>      unchanged; no existing table is renamed
EMBEDDED   → edt_<envId>_<baseName>
```

Required, not cosmetic. `data_table` rows are looked up by name and the physical table name is global per
environment, so the first embedded table the demo needs — `conversations`, re-created after its namesake
was classified AUTOMATION — collides with the automation table it was split from. A distinct leading
token avoids the ambiguity a `dt_<env>_e_` infix would create with a table legitimately named
`e_something`.

The column and the prefix must never disagree: a row saying EMBEDDED whose physical table is `dt_…` is
unreachable, and the reverse is invisible. That gets a test.

Knowledge bases need only the column; they have no physical tables.

### Registry keying

Uniqueness moves off bare `name`, but the two registries need different keys because they hold
different things.

| registry | new key | why |
|---|---|---|
| `data_table` | `(name, platform_type)` | the row is environment-agnostic — one logical table, one physical instance per environment |
| `knowledge_base` | `(name, platform_type, environment)` | the row **is** the resource and carries `environment` |

`knowledge_base` needs `environment` in the key to fix a separate pre-existing bug, tracked as
[#5591](https://github.com/bytechefhq/bytechef/issues/5591): `uk_knowledge_base_name` is global while rows are per-environment, so a knowledge base name can
exist in exactly one environment. Data tables had the same symptom from a different cause — a branch
regression in `register`, fixed in `e8ad0e6b676` by restoring find-or-create — and needed no environment
column because `dropTable` and `listTables` already treat the registry row as shared.

Three keyed lookups follow:

```
register(baseName, description)         → find-or-create on (name, platformType)
getIdByBaseName(baseName)               → + PlatformType
hasPhysicalTablesForBaseName(baseName)  → + PlatformType, scanning that pool's prefix only
```

The last is subtle: without it, dropping the embedded `conversations` scans `dt_…` as well, finds
automation's table of the same name, and leaves the embedded registry row alive as an orphan.

### Service signatures

`createTable(...)` and `listTables(...)` take a required `PlatformType`, and **the unscoped overloads are
deleted rather than kept as AUTOMATION-defaulting conveniences.** The compiler then enumerates every call
site — roughly nine option lookups, the AI Hub and Copilot tools, the search asset provider, and both
workspace facades.

That is the difference between this working and this leaking. A defaulting overload lets a future call
site pick a pool by forgetting to think about it, which is how `duplicateTable` orphaned a table and how
`configuration-app` inherited a changelog it could not run. Silent defaults in this codebase get taken.

Both facades keep their shapes: `WorkspaceDataTableFacadeImpl` passes AUTOMATION and then intersects
`workspace_data_table` exactly as now — workspace scoping remains a within-automation concern sitting
below the pool split — and `EmbeddedDataTableApiFacadeImpl` passes EMBEDDED plus the owner filter it
already applies, gaining the create path Plan 4 did not ship.

The pool is an explicit parameter, never inferred from the security context. The owner model rejected
ambient context because it does not survive async hand-offs; the pool follows the same rule.

### Runtime scoping

Pool visibility is derived from the owner, at the seam where `OwnerResolution` already resolves it:

- owner present → EMBEDDED pool, rows already scoped to that owner
- owner absent → both pools

**Fail closed.** Owner resolution fails open on purpose — no resolver means empty owner means see
everything, correct for Community Edition. Pool resolution cannot copy that, because failing open *is*
cross-pool leakage. An unresolvable pool yields nothing.

**The editor/options path needed no new machinery — resolved while planning, 2026-08-29.**
`DataTableUtils.getTableOptions` already receives `OwnerResolution.resolve(...)`, so the options path
resolves the owner itself, including in the editor where it falls through to `resolveCurrentPrincipal()`.
Because scoping keys on the owner rather than the platform type, the pool follows from a value already in
hand and nothing has to be threaded into the request. The concern this replaces was real only for the
earlier platform-type-keyed draft: `ActionContextAware.getPlatformType()` is nullable and
`ActionContextImpl.Data` requires it only on its non-editor branch, so the editor genuinely cannot supply
it.

**A leak vector found in the same pass.** `DataTableUtils.getDataTableInfo(service, baseName,
environmentId)` resolves a table **by name with no scoping at all**, and feeds `rowObjectSchema`,
`createSampleOutput` and the column-properties function. It exposes column metadata rather than row data,
but an account naming `invoices` would learn its structure, and after the split two same-named tables in
different pools would resolve to whichever the scan reached first. It takes the owner and the pool like
every other lookup.

### Naming the account

Data table and knowledge base actions gain an optional account selector — the only way a vendor run
reaches a connected user's rows.

**The account parameter is honoured only when the run has no owner. If the run already belongs to a
connected user, the parameter is ignored — never obeyed.** Without that, Alice's own workflow names Bob
and reads Bob's data. It is a one-line check and a privilege escalation if omitted.

**An unowned write must be deliberate.** An unowned row is visible to every account, and `insertRow`
stamps the owner columns only when the filter carries an owner — so an unresolved owner writes a row
nobody can attribute and everybody can read, permanently, with nothing to correct it afterwards. Unowned
rows do two jobs that are indistinguishable in the data: deliberate shared reference data, and the
accident. They are separable only at the moment of writing, so that is where the rule goes: on a write to
an EMBEDDED table, `unrestricted()` is rejected, an owner is stamped, and `unownedOnly()` doubles as the
explicit "this row is shared" marker. `insertRow`, `updateRow` and `importCsv` all take it — each can
create or relabel a row. AUTOMATION tables are untouched.

This tightens behaviour that already shipped: today `RowOwnerFilter.from(empty)` is `unrestricted()`, so a
vendor run reads every account's rows. Changing it is safe now only because every existing resource is
being classified AUTOMATION and the embedded pool starts empty; with data behind it this would be
breaking.

It also makes the deferred `OwnerResolverGuard` less dangerous rather than more: an EE app missing the
bean now downgrades to "unowned rows only" instead of leaking everything.

### Palette

`IntegrationComponentDefinitionFilter.COMPONENT_NAMES` drops `dataTable` and `knowledgeBase`, keeping
`apiPlatform` and `webhook`. Those two components were hidden from the integration palette because data
tables and knowledge bases were automation-only, and hiding them was the only way to stop embedded authors
reaching automation's resources. This design achieves that by a stronger mechanism, so the weaker one
stops being protection and becomes an obstacle — leave it and the embedded pool is unusable from the
embedded editor, reachable only from hand-authored JSON.

## Migration

A default value and a constraint swap; no data step, no physical renames.

```
ALTER TABLE data_table ADD platform_type INT NOT NULL DEFAULT 0
DROP CONSTRAINT uk_data_table_name
ADD  CONSTRAINT uk_data_table_name_platform_type UNIQUE (name, platform_type)
```

and the same for `knowledge_base`, whose replacement constraint is
`(name, platform_type, environment)`. The default is the migration: every existing row lands on
AUTOMATION.
It stays on the column so a writer that forgets the field gets automation rather than a constraint
violation; the service always sets it explicitly.

## Testing

Negative assertions throughout — a customer's run must be shown *not* to see `invoices`, not merely to
see its own table.

- the three runs: integration workflow, **bridged** workflow, vendor job — pool and rows for each
- the escalation attempt: an owned run supplying the account parameter must not widen
- vendor run with and without an account named
- the column-and-prefix invariant
- each console list excluding the other pool

The bridged case matters most: it is the one the first draft of this design got wrong, and no existing
test covers it.

## Out of scope

- **Sub-project 2**: the `/embedded` console at full parity — create dialog, column editing, row grid, and
  the shared-component extraction Plan 6 deferred (263 files across four page trees, 187 in the two detail
  trees). Designed separately against this API once it exists.
- **Per-environment ownership.** The registry row is shared across environments, so name, description,
  tags and owner are common to all of them while columns are per physical table and may differ. That is
  the existing model, unchanged here.
- **The demo migration.** Re-creating the demo's three tables in the embedded pool is the user's, and the
  reasoning parked in `project_per_account_dt_kb_ownership` needs revisiting since half of it (that
  `dataTable` is authorable in neither palette) stops being true.
