# Resource Visibility & Grants — Design

- **Date:** 2026-08-10
- **Branch:** `0_732`
- **Status:** Proposed
- **Supersedes:** `2026-04-06-connection-visibility-phase1-design.md`,
  `2026-06-13-connection-visibility-to-ee-design.md` (both branch-only, never released)
- **Builds on:** `2026-06-19-centralized-idor-authorization-design.md`

## 1. Summary

`ConnectionVisibility` is replaced by a resource-agnostic visibility model so that any workspace-owned
resource can control who sees it. The model has two parts:

1. A **`visibility` column** — the resource's reach. `WORKSPACE` by default, everywhere, in every edition.
2. **Grant rows** — named individual users who may see a resource its owner has withheld.

**Phase 1 wires exactly one resource: `Connection`.** Projects, data tables, knowledge bases, files and
skills are the motivation for making the machinery generic, but none of them are touched in this cycle.

The whole connection-visibility feature is absent from `origin/master` — it exists only on `0_732`. There
is therefore no released database carrying a `visibility` value, no upgrade path to design, and no
persisted-ordinal compatibility to preserve.

## 2. Goals / non-goals

**Goals**

- One visibility vocabulary shared by all resources, replacing the connection-specific enum.
- Let an owner withhold a resource from their workspace, and hand it to named colleagues.
- Keep CE fully collaborative: everything workspace-visible, no controls, no exceptions.
- Make list filtering and by-id authorization agree with each other.
- Leave phase 2 (projects, data assets, skills) as a wiring exercise, not a redesign.

**Non-goals (phase 1)**

- Any resource other than `Connection`.
- Project → workflow visibility inheritance (phase 2; the rule is fixed in §11 so phase 2 need not
  relitigate it).
- Grants carrying permissions. A grant conveys visibility only; what the recipient may then *do* is
  answered by the existing `PermissionScope` / `WorkspaceRole` machinery, unchanged.
- Grants to groups, roles, or anyone outside the owning workspace.
- Cross-workspace or cross-organization sharing beyond the existing `ORGANIZATION` rung.
- Time-limited grants, deny rules, ownership transfer.

## 3. Model

### 3.1 The enum

`ResourceVisibility` lives in `platform-api` (`com.bytechef.platform.security.domain`), alongside
`SecurityUtils`. Every candidate module already depends on `platform-api`, directly or transitively.

```java
public enum ResourceVisibility {
    PRIVATE,        // owner (+ named grantees) only
    WORKSPACE,      // every member of the owning workspace — the default
    ORGANIZATION    // every member of every workspace in the organization
}
```

`ConnectionVisibility` is deleted. Its `getCode()` / `fromCode()` / ordinal-stability apparatus goes with
it: that machinery protected persisted ordinals against enum reordering, and nothing has shipped, so
there are no persisted ordinals to protect. If a future change makes the persisted representation
code-based, it can be reintroduced then with a converter round-trip test.

`canTransitionTo` is also deleted. It encoded a one-way promote-from-private state machine
(`PRIVATE → WORKSPACE`, `ORGANIZATION` terminal) that this model no longer has — an owner may now move a
resource freely between the rungs their resource type supports. What replaces it is a per-resource
declaration of *supported* rungs, validated centrally (§3.2).

### 3.2 Supported rungs per resource

Not every rung means something for every resource. Each module contributes a `ResourceVisibilityPolicy`
declaring the rungs its resource type supports, aggregated into one registry at startup — the same
per-module SPI shape as `PermissionScopeProvider` and `ResourceOwnershipResolver`, with the same
duplicate-registration-is-a-startup-error rule. Every write path validates against the registry, so an
unsupported value is rejected server-side rather than by convention.

| Resource | Supported rungs | Phase |
| --- | --- | --- |
| `Connection` | `PRIVATE`, `WORKSPACE`, `ORGANIZATION` | 1 |
| `Project` | `PRIVATE`, `WORKSPACE` | 2 |
| `DataTable`, `KnowledgeBase`, `File` | `PRIVATE`, `WORKSPACE` | 2 |
| `Skill` | `PRIVATE`, `WORKSPACE` | 2 |

`ORGANIZATION` stays connection-only. A connection is a credential that legitimately serves several
workspaces; a project belongs to one workspace and promoting it organization-wide would surface it in
other workspaces' project lists, which the project model has no other way to express.

### 3.3 The three user-facing states

The picker offers three states. They are two enum values plus the presence of grant rows — one state
machine, not two.

| UI state | Storage |
| --- | --- |
| Shared with workspace *(default)* | `visibility = WORKSPACE` |
| Private | `visibility = PRIVATE`, no grants |
| Specific people | `visibility = PRIVATE` + one grant row per named user |
| Organization *(connections, admin only)* | `visibility = ORGANIZATION` |

Grants on a `WORKSPACE` or `ORGANIZATION` resource are inert — everyone can see it already. They are not
deleted on promotion, so demoting back to `PRIVATE` restores the previous audience rather than silently
dropping it.

### 3.4 Resolution

First match wins:

```
1. actor is workspace ADMIN, or tenant admin      → visible
2. visibility >= WORKSPACE                        → visible
3. actor is the owner (created_by)                → visible
4. a grant row exists for (type, id, actor)       → visible
5. otherwise                                      → not visible
```

Step 1 is the "ADMIN always sees everything" rule. Two distinct admins both satisfy it:
`WorkspaceRole.ADMIN` on the resource's owning workspace (EE), and the tenant-level
`AuthorityConstants.ADMIN` authority — the same pair `PermissionService` already bypasses on
`hasWorkspaceScope*` / `hasResourceScope`, and the same pair today's `ConnectionVisibilityResolverImpl`
approximates with its single `isAdmin` check. Steps 2–4 are additive; there is no deny record.

All of this runs *inside* an already workspace-scoped query — the candidate set is the connections
attached to the workspace being listed. Visibility narrows that set; it never widens it.

### 3.5 Editions

| | CE | EE | Embedded |
| --- | --- | --- | --- |
| Value written on create | `WORKSPACE`, forced | as chosen, default `WORKSPACE` | `PRIVATE`, forced |
| Picker | hidden | shown | n/a |
| Grants | unavailable | available | n/a |

**CE becomes fully collaborative for connections.** Today `ConnectionFacadeImpl:118-131` force-writes
`PRIVATE` in CE; this design inverts that half of the condition to force `WORKSPACE`. That is a
deliberate, requested reversal of the CE half of the Gecko T18 remediation: in CE, one workspace member's
stored credentials become visible and usable by their colleagues by default. It fits CE's stated model —
a permissive, collaborative, single-tenant edition whose `PermissionService` already returns `true` for
every workspace-scope check — but it is a real loosening and §7 covers the check that has to change with
it.

**Embedded is unchanged.** The `PlatformType.EMBEDDED` half of the same condition stays exactly as it is:

```java
if (type == PlatformType.EMBEDDED) {
    connection.setVisibility(ResourceVisibility.PRIVATE);   // unchanged
} else {
    connection.setVisibility(requested);                     // CE: always WORKSPACE
}
```

An embedded connection belongs to a *connected user* — an end user of a customer's product — not to a
workspace member, so "visible to the whole workspace" has no meaning and `WORKSPACE` would be wrong in a
way that crosses customers. To be precise about what this line does and does not buy: embedded isolation
is actually enforced by connected-user / `externalUserId` scoping (Gecko T23). Forcing `PRIVATE` is
defensive redundancy — the column is `NOT NULL`, the row needs a value, and `PRIVATE` is the value that
cannot widen anything if some query reaches the row by another path. This design does not promote it to
being the primary control.

## 4. Removal & schema

### 4.1 Removing the current implementation

The existing implementation is **removed, not evolved**. It was built around a one-way
promote-from-private model that this design inverts, so adapting it in place would leave the shape of the
old model embedded in the new one. 42 server files reference `ConnectionVisibility`, but they split into
two very different piles.

**Deleted outright** — artefacts that exist only to serve the promote/demote model:

| Artefact | Note |
| --- | --- |
| `ConnectionVisibility` + `ConnectionVisibilityTest` | replaced by `ResourceVisibility` |
| `ConnectionVisibilityResolver` (CE api) | replaced by `ResourceVisibilityResolver` |
| `DefaultConnectionVisibilityResolver` + test | replaced by the CE resolver |
| `ConnectionVisibilityResolverImpl` (EE) + test | replaced by the EE resolver |
| `ConnectionVisibilityGraphQlController` | replaced by §10's mutations |
| `connection-visibility.graphqls` | replaced by §10's schema |
| `BulkPromoteResultDTO` + the bulk-promote path | migrating a default that no longer exists |
| `20260407000001_platform_connection_added_column_visibility.xml` | see §4.2 |
| `VisibilityMenuItems.tsx` + test | replaced by the picker |
| `ConnectionListItemVisibilityGate.test.tsx` | gate no longer exists |
| `promoteConnectionToWorkspace.graphql`, `demoteConnectionToPrivate.graphql`, `promoteAllPrivateConnectionsToWorkspace.graphql` | replaced by §10's documents |

**Migrated to `ResourceVisibility`** — code that legitimately carries a visibility value and keeps
working, with only the type changing:

`Connection`, `ConnectionDTO`, `ConnectionService(+Impl)`, `ConnectionFacade(+Impl)`,
`ConnectionCreatedEvent`, `ConnectionRepository`, `AiProviderConnectionRepositoryImpl`,
`WorkspaceConnectionFacade(+Impl, CE and EE)`, `OrganizationConnectionFacade(+Impl)`,
`ConnectionReassignmentFacade(+Impl)`, `ConnectionAuditEvent` and its listener,
`connection_service.proto` + `ConnectionProtoMapper` + both remote clients, the runtime-job-app
`ConnectionServiceImpl`, the REST mappers and `openapi.yaml`, `connection.graphqls`, and the generated
client models.

**Kept, adapted:** `ConnectionScopeBadge` (now renders the new states) and `useVisibilityFeatureEnabled`
(now gates the picker rather than the menu items).

`connection_service.proto` is likewise absent from `origin/master`, so renaming the enum in the gRPC
contract carries no wire-compatibility cost — there is no deployed peer speaking the old shape.

### 4.2 Column

`connection.visibility` already exists on `0_732`. Rather than rewriting the existing changeset, the
changeset **file is deleted** and a new one adds the column fresh with `DEFAULT 1` (`WORKSPACE`).

The changelog directory is loaded by Liquibase `includeAll`, so deleting the file is sufficient — there is
no master changelog to edit, and no reference to unpick. The sibling
`20260407000002_..._add_column_status.xml` is unaffected: `includeAll` orders by filename, so removing an
earlier file leaves later ones applying normally.

Developers holding a `0_732` database need to drop the column (or the schema) before the new changeset
runs — Liquibase will not re-add a column that already exists. No customer database has ever run the old
changeset, so this is a developer-workstation concern only.

Phase 2 adds the same column to `project`, `data_table`, `knowledge_base`, `file`, and the skill table.

### 4.3 Grant table

```sql
create table resource_grant (
    id             bigserial     primary key,
    resource_type  varchar(64)   not null,
    resource_id    bigint        not null,
    user_id        bigint        not null,
    created_by     varchar(255)  not null,
    created_date   timestamp     not null,
    constraint resource_grant_uq unique (resource_type, resource_id, user_id)
);

create index resource_grant_user_idx on resource_grant (user_id, resource_type);
```

- **No permission column.** Grants convey visibility only (§2).
- **No `organization_id`.** The tenant schema provides tenant isolation (Gecko T4) and the resource's own
  workspace scoping bounds the rest. A column here would be a third, unenforced copy of the same fact.
- **`resource_type` is application-controlled**, never an unvalidated client value; it is validated
  against the registered resource types on every write.
- **`user_id`, not a login.** `created_by` elsewhere in the codebase is a login string, but logins change
  and cannot be foreign-keyed. Resolution of the current user's login to an id is already solved by the
  EE `CurrentUserResolver`, which caches `login → userId` per HTTP request.
- **Polymorphic `resource_id`, no FK.** The application validates that the referenced resource exists, is
  in the caller's workspace, and supports grants, before writing. A database FK cannot express that.
- **Idempotent by construction.** Re-granting an existing pair is a no-op absorbed by the unique
  constraint, so a double-click cannot produce duplicate rows or an error.

**The changelog ships in an EE module**, following the `platform-component-policy` precedent. Only the EE
resolver ever reads the table, and only EE write paths ever populate it; a CE install has no use for it.

### 4.4 Naming

`resource_grant`, not `resource_share`. "Shared" is already the name of the `WORKSPACE` state in the UI, so
a `share` table whose rows only matter while a resource is *private* would read backwards at every call
site.

## 5. Module placement

| Artefact | Module | Edition |
| --- | --- | --- |
| `ResourceVisibility` | `platform-api` | CE |
| `ResourceVisibilityPolicy` (supported rungs) | `platform-api` | CE |
| `ResourceVisibilityResolver` (SPI) | `automation-configuration-api` | CE |
| `DefaultResourceVisibilityResolver` | `automation-configuration-service` | CE |
| `ResourceVisibilityResolverImpl` | `ee/.../automation-configuration-service` | EE |
| `ResourceGrant`, repository, service, changelog | new `ee/libs/platform/platform-resource-grant` | EE |

The new EE module mirrors `platform-component-policy` in shape. It sits at platform level rather than
inside `automation-configuration` because phase 2's resources (data tables, knowledge bases, files) are
platform entities.

## 6. Read path

The existing resolver takes an already-loaded list and filters it in memory
(`ConnectionVisibilityResolver.filterVisible`). That shape is kept, generalized, and given a batched grant
lookup:

```java
public interface ResourceVisibilityResolver {

    /**
     * Ids of the given candidates the current actor may see. Never null; may be empty.
     */
    Set<Long> filterVisibleIds(String resourceType, long workspaceId, Collection<VisibilityRecord> candidates);

    record VisibilityRecord(long id, ResourceVisibility visibility, String createdBy) {}
}
```

Each facade maps its DTOs to `VisibilityRecord`, calls the resolver once, and retains the returned ids.
Returning ids rather than filtered DTOs keeps the SPI free of generics gymnastics and lets one
implementation serve every resource type.

- **CE implementation** applies steps 1–3 of §3.4 and never touches the grant table.
- **EE implementation** applies all four steps. Grants are fetched in **one** batched query per call —
  `WHERE resource_type = ? AND user_id = ? AND resource_id IN (:candidateIds)` — so there is no N+1, and
  the query is skipped entirely when no candidate is `PRIVATE`.

### Why not query-level filtering

The originating authorization specification calls for filtering in SQL rather than in memory, and that is the right
long-run answer. It is not phase 1, for a concrete reason: `WorkspaceConnectionFacadeImpl.getConnections`
already bounds its candidate set to one workspace's connections by reading ids out of the
`workspace_connection` join table before loading anything. The set being filtered is small and already
scoped, so pushing the predicate into `ConnectionRepository` — a platform-level repository shared with
the embedded paths — would be invasive for no measurable gain today.

This becomes worth revisiting when phase 2 reaches a resource whose per-workspace row count is unbounded.
The `VisibilityRecord` seam is deliberately narrow so that swapping in a SQL predicate later changes the
resolver's implementation and not its callers.

**AI-provider connections bypass visibility entirely** and must continue to. They are platform- and
environment-scoped, are not in `workspace_connection`, and surface as read-only connections in every
workspace. The existing bypass in `WorkspaceConnectionFacadeImpl` stays as-is.

## 7. Reconciling `hasResourceScope`

This is the correctness problem that the CE change creates, and it is part of this work rather than a
follow-up.

`PermissionService.hasResourceScope` resolves, in CE, to *"tenant admin → true; else resource owner →
`isCurrentUser`; else false"*. Under §3.5 a CE connection is `WORKSPACE`-visible, so a colleague will
**see** it in the list while every by-id operation on it denies — the list filter and the scope check
would disagree, and the user experience is an item that exists but cannot be opened.

Both editions' `hasResourceScope` must therefore consult visibility, not ownership alone:

| | Current | Required |
| --- | --- | --- |
| CE | admin → true; owner → true; else **false** | admin → true; `visibility >= WORKSPACE` → true; owner → true; grant → n/a (CE has none); else false |
| EE | admin → true; workspace-scope → `hasWorkspaceScope` | unchanged, **and** for `PRIVATE` resources additionally require owner-or-grant |

The EE row is the tighter half and matters just as much: today a workspace member with `CONNECTION_EDIT`
passes `hasResourceScope` for *any* connection in their workspace, including one a colleague has made
`PRIVATE`. The list hides it; the by-id path does not. Phase 1 closes that.

The single rule both editions implement: **`hasResourceScope` passes only if the actor can see the
resource under §3.4 *and* holds the requested scope.** Visibility is a precondition to every scope check,
not a parallel filter.

## 8. Write path

### 8.1 Setting visibility

- **Who:** the resource owner, or a workspace `ADMIN` / tenant admin.
- **`ORGANIZATION`** additionally requires admin, and remains reachable only through
  `OrganizationConnectionFacade`.
- **Validation:** the requested rung must be in the resource type's supported set (§3.2). CE ignores the
  requested value and writes `WORKSPACE`; embedded ignores it and writes `PRIVATE`.
- **The `ROLE_ADMIN` gate on `WORKSPACE` is removed.** It existed because `WORKSPACE` was a promotion out
  of the private default; it is now the default itself, and gating the default would make ordinary
  creation fail for non-admins.

### 8.2 Managing grants

- **Who:** the resource owner, or a workspace `ADMIN` / tenant admin.
- **Grantee validation:** must be an active member of the resource's owning workspace. A non-member is
  rejected even when their id is known, and the rejection does not disclose whether the id exists.
- **Idempotent:** re-granting is absorbed by the unique constraint (§4.3).
- **Revocation is a hard delete.** History lives in the audit event, not as an inactive-looking row, so
  no query has to remember to filter on an `active` flag.
- **Transactional:** visibility change and grant mutations commit together with their audit event.

### 8.3 Audit

The existing connection audit surface (`ConnectionAuditEvent`) gains visibility-change and grant
create/revoke events, carrying actor, resource reference, previous and new state, and the affected user
for grants.

## 9. Client

The visibility control moves into the connection **create and edit dialogs** as a picker:

```
◉ Shared with workspace      (default)
○ Private
○ Specific people   [ana ×] [+ add]
○ Organization              (admin only)
```

**Removed:** `VisibilityMenuItems`, the demote-confirm dialog, and
`promoteAllPrivateConnectionsToWorkspace` (a CE→EE migration helper for a migration that no longer
exists, since CE connections are now `WORKSPACE` from birth).

**Kept:** `ConnectionScopeBadge`. It is the only at-a-glance signal in the list that a connection is not
workspace-visible, which matters most in exactly the workspaces where people restrict things.

The picker is hidden entirely in CE, alongside the existing `useVisibilityFeatureEnabled` gate.

## 10. GraphQL

`promoteConnectionToWorkspace` and `demoteConnectionToPrivate` are replaced by a single mutation that
sets the state, plus grant mutations:

```graphql
setConnectionVisibility(workspaceId: ID!, connectionId: ID!, visibility: ResourceVisibility!): Connection!
grantConnectionAccess(workspaceId: ID!, connectionId: ID!, userId: ID!): Boolean!
revokeConnectionAccess(workspaceId: ID!, connectionId: ID!, userId: ID!): Boolean!
connectionGrants(workspaceId: ID!, connectionId: ID!): [User!]!
```

`connectionGrants` requires owner-or-admin: ordinary viewers of a shared connection must not learn who
else it was granted to. Per the established convention, authorization is annotated on the **facade**, not
the GraphQL controller.

## 11. Phase 2 (not implemented here)

Recorded so phase 2 is wiring rather than redesign:

- **Resources:** `Project` — implemented by `2026-08-17-project-visibility-design.md` (with `Workflow`,
  `ProjectWorkflow`, `ProjectDeployment` and `Job` inheriting via the `visibilityResourceType()` provider
  hook). `DataTable`, `KnowledgeBase`, `File`, `Skill` — pending. Each adds a `visibility` column, a
  `ResourceVisibilityPolicy` entry, and a `VisibilityRecord` mapping in its list facade.
- **Workflows inherit dynamically.** A workflow has **no** `visibility` column; its reach *is* its
  project's, resolved at check time. One source of truth, no clamp rule, no synchronization, and no
  incoherent state such as a `WORKSPACE` workflow inside a `PRIVATE` project. This also closes the hole
  where a workspace member with `WORKFLOW_VIEW` could reach a private project's workflow by id.
- **Memories are excluded.** They are written by agents, not users, and already carry their own
  `(principalType, principalId)` ownership model — see
  `2026-08-10-auto-memory-principal-filter-design.md`.
- **MCP servers and API collections are excluded** pending a decision on how visibility interacts with
  whoever holds the API key.

## 12. Testing

- **Enum/policy:** unsupported rung rejected per resource type; `ORGANIZATION` rejected for phase-2
  resource types.
- **Resolver, CE:** everything workspace-visible; a `PRIVATE` row (only reachable via embedded or direct
  insert) hidden from non-owners; admin sees all.
- **Resolver, EE:** each of the five branches of §3.4; grants honoured; grants on a `WORKSPACE` resource
  inert; a single batched grant query for N private candidates (no N+1); no grant query at all when
  nothing is private.
- **`hasResourceScope` reconciliation:** in both editions, a member holding `CONNECTION_EDIT` is denied on
  a colleague's `PRIVATE` connection and allowed once granted. This is the regression test for §7 and the
  most important test in the set.
- **Facade authorization:** grant/revoke denied to non-owner non-admins; cross-workspace grantee rejected;
  `connectionGrants` denied to a plain viewer.
- **Idempotency:** duplicate grant produces one row and no error.
- **Embedded:** create with `visibility = WORKSPACE` in the body still persists `PRIVATE`.
- **CE:** create with `visibility = PRIVATE` in the body still persists `WORKSPACE`.
- EE test classes carry the `@version ee` Javadoc tag.

## 13. Documentation to amend

- **`CLAUDE.md:974`** — *"Sharing is additive to a column — it does not need a relation table."* That rule
  is right about *reach* and stays. It needs a carve-out for named-user grants, which a column provably
  cannot encode without an unbounded array in a cell. Without the amendment, `resource_grant` reads as
  drift rather than as a considered exception.
- **`CLAUDE.md` "Connection Visibility (EE-only feature)"** — rewritten wholesale: the CE default
  inverts, the `ROLE_ADMIN` gate on `WORKSPACE` is gone, the promote/demote mutations are replaced, and
  the metric tag gains the new values.
- **`gecko-remediation-tasks.md`** — T18's CE half is deliberately reversed for connections (§3.5). Record
  it there so the checkmark does not over-claim, and note that §7 is what keeps the EE half honest.

## 14. Risks

1. **Credentials are shared by default, in both editions.** In CE every workspace member can use every
   colleague's stored credentials, because CE force-writes `WORKSPACE`. In EE the same is true for anyone
   who does not open the picker, because `WORKSPACE` is the default there too.

   This was considered twice and kept deliberately. The CE half follows from CE having no authorization
   boundary between members in the first place — its `PermissionService` returns `true` for every
   workspace-scope check, so connection visibility had been an island of strictness in an otherwise
   permissive edition. The EE half was kept for consistency: one default across every resource type means
   the picker's pre-selected option never depends on what is being created.

   **What a shared connection actually exposes is narrower than "the credential".** Two existing
   mechanisms bound it, and both were verified against the code before this risk was written:

   - **The secret is never readable.** `ConnectionApiController.toConnectionModel` obfuscates
     `authorizationParameters` through `ObfuscateUtils.toObfuscatedMap(..., 28, 8)` and sets
     `parameters` to `null`. The embedded controller does the same. The raw values exist on
     `ConnectionDTO` but do not cross the REST boundary, and the GraphQL surface exposes only the
     narrow `DataStreamCompatibleConnectionDTO` projection.
   - **The credential cannot be edited.** `ConnectionFacade` offers only `update(id, tags)` and
     `update(id, name, tags, version)`. No path mutates authorization parameters after creation;
     OAuth refresh is system-driven.

   So `WORKSPACE` grants *use plus existence*, not *read plus write*. A member can run a workflow
   against a colleague's Slack account; they cannot extract the token or repoint it. This is the same
   trade n8n makes — credentials created in a project are usable by every project member, while users
   of a shared credential cannot view or edit its details.

   The residual risk is therefore use-by-proxy, not secret disclosure: actions taken through someone
   else's credential appear to the third-party system as that person. Worth a release note; not the
   secure-by-default violation an earlier draft of this section claimed.

   If it is ever revisited, the change is small and local — `ResourceVisibilityPolicy` already declares
   supported rungs per resource type, so a per-resource *default* is one more field on the same record.
   Nothing else in this design depends on the default being uniform.
2. **Two checks that must not drift.** §7 makes visibility a precondition of `hasResourceScope`. If a
   future resource wires its list filter without wiring the scope check — or the reverse — the two
   disagree again. The test named in §12 is the guard; it should be written first.
3. **Liquibase checksum break.** Rewriting a shipped-to-branch changeset in place will interrupt every
   developer with a `0_732` database. Cheap, but it needs announcing rather than discovering.
4. **`ORGANIZATION` semantics stay connection-only.** If phase 2 acquires a genuine need to promote a
   project organization-wide, §3.2 has to be reopened rather than extended, because the project model has
   no representation for a project outside its workspace.
