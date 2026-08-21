# Auto-Memory Principal Filter

Let the auto-memory GraphQL queries address a principal other than the signed-in user, and give the Memories
page an owner picker, so a workspace can inspect and correct the long-term memory its deployed agents have
accumulated.

## Problem

`AiAutoMemory` rows are owned by a `(principalType, principalId)` pair — `USER`, `PROJECT_DEPLOYMENT`, or
`INTEGRATION_INSTANCE`. The GraphQL surface only ever addresses the first: every resolver hardcodes
`AiAutoMemoryPrincipalType.USER` with the current user's id.

So deployment-owned memory — written by the AI Agent's auto-memory tool during AUTOMATION runs, and the thing
that actually shapes how a production agent behaves on its next run — is invisible and unmanageable outside
the database.

## Decisions

- **`USER` and `PROJECT_DEPLOYMENT` only.** `INTEGRATION_INSTANCE` is deliberately not addressable; see "Why
  integration instances are excluded".
- **Optional arguments, defaulting to `(USER, current user)`.** Today's behaviour is the default, so the
  change is additive and existing queries keep working.
- **Reads open to workspace members; deployment mutations require `ROLE_ADMIN`.**
- **A foreign `USER` id returns empty/null, never an error.** Ids stay unenumerable.
- **The picker is driven by a principals-with-memories query**, not a deployment list, so it never offers an
  owner with nothing to show.
- **The unused AI Hub embedding of the Memories page is removed**, freeing the sidebar unconditionally.

## Component 1 — Schema

All four operations take the same optional pair:

```graphql
aiAutoMemories(workspaceId: ID!, environment: Int!, memoryType: AiAutoMemoryType,
               principalType: AiAutoMemoryPrincipalType, principalId: Long): [AiAutoMemory!]!

aiAutoMemory(workspaceId: ID!, id: ID!,
             principalType: AiAutoMemoryPrincipalType, principalId: Long): AiAutoMemory

deleteAiAutoMemory(workspaceId: ID!, id: ID!,
                   principalType: AiAutoMemoryPrincipalType, principalId: Long): Boolean!
```

`UpdateAiAutoMemoryInput` gains the same two nullable fields.

Supplying exactly one of the pair is an `IllegalArgumentException`. A `principalId` without a
`principalType` is not a partial filter to be helpfully completed — it is a client bug, and defaulting the
missing half would silently address a different principal than the caller named.

The input reuses `AiAutoMemoryPrincipalType` rather than introducing a filter-only enum, because the output
field legitimately returns all three values and a second enum would have to be kept in step with it.

## Component 2 — Authorization

| Principal | Read | Mutate |
| --- | --- | --- |
| `USER`, own id | workspace membership | workspace membership |
| `USER`, another user's id | empty / null | empty / null, surfacing as not-found |
| `PROJECT_DEPLOYMENT` | workspace membership | workspace membership **+ `ROLE_ADMIN`** |
| `INTEGRATION_INSTANCE` | rejected | rejected |

### Why `PROJECT_DEPLOYMENT` needs no ownership lookup

Every service call filters on `workspaceId`, and the resolver verifies the caller's membership of that
workspace before delegating. A deployment in another workspace wrote its rows under *its own* workspaceId, so
a foreign deployment id matches nothing. The workspace check plus the lookup key are jointly sufficient; a
deployment&rarr;workspace resolution would be redundant work that could itself drift.

### Why `USER` cannot rely on the same argument

Two members of one workspace both have rows under that workspaceId. Without an explicit guard, a member could
read a colleague's personal memories by passing their user id. This is the one case where the lookup key does
not isolate, and the only reason the guard exists.

### Why a foreign `USER` id is not an error

The schema already documents this posture for the single fetch: "the same shape on both errors so a probe
cannot enumerate ids across workspaces." Raising `AccessDenied` on a foreign id would confirm the id exists,
which is precisely what that behaviour was written to prevent. Empty and null are indistinguishable from "no
such memory".

### Why `INTEGRATION_INSTANCE` is excluded

Embedded iPaaS has no deployment&rarr;workspace chain, so those rows are written under
`Workspace.DEFAULT_WORKSPACE_ID` and isolated only by the globally-unique integration-instance id (see
`AiAgentUtilsAutoMemoryTool`'s javadoc). Workspace membership therefore isolates nothing for them: every
embedded customer's rows share one bucket, and any member of the default workspace could read all of them.

Addressing them safely needs an "does this integration instance belong to the caller's tenant" check that
this controller has no way to make today. Rejecting the input is loud and explicit — unlike the foreign-user
case, this is a statement about what the API supports, not about whether a particular row exists, so there is
nothing to probe for.

### Why the admin gate is inline

`@PreAuthorize` on the mutation would gate it wholesale, including a user editing their own memory. The check
is conditional on the resolved principal, so it runs inline via
`SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN)` — the same shape as
`ConnectionServiceImpl`'s conditional admin check.

Deployment memory is written by a running workflow, not a person. Editing one changes how a live agent behaves
on its next run, with no notification to anyone, which is why it is gated more tightly than reading.

**Known weakness, pre-existing:** in CE, `WorkspaceFacade.getUserWorkspaces` returns every workspace, so the
membership half of the gate is permissive there. The admin half still applies. This is inherited from the
existing controller, not introduced here.

## Component 3 — Resolution

One private helper in `AiAutoMemoryGraphQlController`:

```java
private @Nullable ResolvedPrincipal resolvePrincipal(
    @Nullable AiAutoMemoryPrincipalType principalType, @Nullable Long principalId, long currentUserId,
    boolean mutating)
```

It returns `null` for "the caller may not address this principal" and throws only for malformed input (a
half-specified pair, or `INTEGRATION_INSTANCE`). Reads map `null` to an empty list or a null row; mutations
map it to the existing not-found path.

Keeping *denial* and *malformed input* on separate return channels is deliberate. Collapsing them into one
exception type is how a probe-resistant empty result later becomes a confirming error during an unrelated
edit.

The four resolvers stay thin — verify workspace, resolve principal, delegate. No service, repository, or
entity changes: `AiAutoMemoryService` already accepts `(workspaceId, principalType, principalId, …)` on every
method.

## Testing

The design is a decision table, so the tests are one case per row, at the controller level:

- Both arguments omitted defaults to the current user (pins the back-compatible path).
- Own user id passes through.
- Another user's id returns empty from the list and null from the single fetch.
- A deployment principal reads for an ordinary workspace member.
- A deployment mutation is denied for a non-admin and allowed for an admin.
- `INTEGRATION_INSTANCE` is rejected on both a read and a mutation.
- A half-specified pair is rejected.

`AiAutoMemoryGraphQlControllerTest#testEverySchemaFieldIsResolvable` continues to cover the type's fields.

## Component 4 — Listing the addressable principals

A picker needs to know which owners exist. Rather than listing every deployment in the workspace and offering
mostly-empty options, a query returns only principals that actually hold memory:

```graphql
aiAutoMemoryPrincipals(workspaceId: ID!, environment: Int!): [AiAutoMemoryPrincipal!]!

type AiAutoMemoryPrincipal {
    principalType: AiAutoMemoryPrincipalType!
    principalId: Long!
    label: String!
    memoryCount: Int!
}
```

Authorization mirrors the read path exactly: workspace membership gates the call, `USER` entries are filtered
to the caller alone, `PROJECT_DEPLOYMENT` entries are all returned, and `INTEGRATION_INSTANCE` rows never appear.
Reusing the same rules matters — a picker that offered an owner the read path then refuses would be a bug
surface, and one that revealed *other users exist and have memories* would leak what the foreign-id guard
protects.

`label` is resolved server-side: the deployment's name via `ProjectDeploymentService`, and "My memories" for
the caller's own `USER` entry. `automation-configuration-api` is already a dependency of the GraphQL module,
so this needs no new wiring. Resolving names on the server keeps the client from making a second, differently
authorized call to reconstruct them.

The repository needs one new finder for the distinct `(principal_type, principal_id)` pairs with counts,
scoped to `(workspace_id, environment)`. Both repository implementations must satisfy it, and the existing
`AiAutoMemoryRepositoryContractTests` is where that contract is pinned so the jdbc and file-storage backends
cannot drift.

## Component 5 — The Memories picker

The Memories page's left sidebar currently holds a `LeftSidebarNav` titled "Type" (the memory *category*
filter). The owner picker becomes a second nav group above it, titled "Owner".

**Label collision.** The Type filter's values include "User", meaning a category of memory content. The Owner
group's values are principals. Two adjacent lists both saying "User" would be genuinely confusing, so the
Owner group uses the resolved `label` from Component 4 — "My memories" and deployment names — and never the
word "User".

**Removing the vestigial embedding.** `Memories.tsx` accepts `renderSidebarNav`/`sidebarTitle` so it can
render inside the AI Hub shell with its sidebar slot taken, falling the Type filter back to the header. That
path has no production caller — the only route mounts `<AiAutoMemoriesPage />` with no props, and the only thing
exercising the prop is a test. It is removed: the props, the conditional `leftSidebarBody`, the conditional
`leftSidebarHeader` title, and the header fallback. Memories always renders its own sidebar, which is what
makes a second nav group unconditionally available.

Selecting an owner re-runs the existing list query with that principal. Selecting nothing means the caller's
own memories, matching the server default.

## Out of scope

- Listing or mutating `INTEGRATION_INSTANCE` memories.
- Notifying anyone when an admin edits a deployment's memory.
- Editing deployment memory inline from the list — the existing edit and delete dialogs carry the principal
  through, but no new bulk affordance is added.
