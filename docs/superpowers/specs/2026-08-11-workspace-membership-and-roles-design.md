<!-- Source: https://docs.sim.ai/de/platform/permissions -->

# Workspace Membership, Invitations and Roles

**Status:** Design
**Date:** 2026-08-11
**Scope:** EE

Three related changes to how people get into a workspace and what roles they can hold there.
They share a surface (Settings → Users) and are specified together, but they are independently
landable and should be filed as three issues.

| Phase | Change |
| --- | --- |
| **P1** | Workspace-level invitations (automation), plus a workspace-scoped Users page |
| **P2** | Workspace-scoped custom roles |
| **P3** | Explicit role inheritance in the membership view |

## Problem

### P1 — nobody can be invited into a workspace

Getting a colleague onto a team takes two operations, on two screens, and the first requires a
tenant administrator:

| Operation | Caller | Input |
| --- | --- | --- |
| `inviteUser` | tenant `ROLE_ADMIN` | `email`, `password`, `ROLE_USER`/`ROLE_ADMIN` — creates a tenant account, knows nothing about workspaces |
| `addWorkspaceUser` | scope `WORKSPACE_MEMBER_MANAGE` | `workspaceId`, an **existing** `userId`, `WorkspaceRole` |

A workspace admin — already trusted with every shared credential in their workspace — cannot
onboard someone who has no account. They must ask a tenant admin.

**The invited user lands nowhere.** This is a live defect. `UserManagementFacadeImpl.inviteUser`
creates the `User`, assigns the tenant authority, sends the mail, and writes **no `workspace_user`
row**. `WorkspaceFacadeImpl.getUserWorkspaces` then filters by membership for non-admins:

```java
if (!permissionService.isTenantAdmin()) {
    List<Long> userWorkspaceIds = workspaceUserService.getUserWorkspaceUsers(id)
        .stream()
        .map(WorkspaceUser::getWorkspaceId)
        .toList();

    workspaces = workspaces.stream()
        .filter(workspace -> userWorkspaceIds.contains(workspace.getId()))
        .toList();
}
```

So an invited `ROLE_USER` activates, signs in, and sees an **empty workspace list**. It goes
unreported because inviting *admins* skips the filter entirely, and the inviter usually follows up
by hand.

**Credentials travel by email.** `inviteUser` takes a `password` — generated client-side by
`useInviteUserDialogStore` via `crypto.getRandomValues`, not typed by the administrator, but shown
to them and regenerable — and `invitationEmail.html` prints it in the body:

```html
Your temporary password is: <code th:text="${password}">PASSWORD</code>
```

`registerUser` already creates the account with `activated = false` and a generated
`activationKey`, and `UserRepository.findByActivationKey` exists — the claim-link machinery is
present and used by self-registration. Only the invite path bypasses it.

### P2 — custom roles are tenant-global

`custom_role` has no `workspace_id`. A role defined for one team is visible and assignable in every
workspace in the tenant. There is no way to give one workspace its own role vocabulary, and the
role list grows into a shared namespace that no single workspace owns.

### P3 — inherited access is invisible

`isTenantAdmin()` short-circuits to `true` in every method of `PermissionServiceImpl`, and
`ResourceVisibilityResolverImpl` admits tenant admins to `PRIVATE` resources. So a tenant admin
*is* an admin of every workspace. But they hold no `workspace_user` row, so `workspaceUsers` does
not list them: the members page shows four people when six can administer the workspace.

## Decisions

### D1 — Workspace admins may invite, with the plan quota as the backstop

A workspace admin can invite an unknown email, provisioning a tenant account and consuming a
billable seat without tenant-admin sign-off.

Considered and rejected:

- **Existing users only** — inviting a new email stays `ROLE_ADMIN`. Leaves the common case (a new
  hire) unsolved.
- **Request/approve** — a workspace admin raises an invitation, a tenant admin approves. Correct,
  and disproportionate machinery.

`UserServiceImpl.registerUser` already calls `enforceMemberQuota()`, which reads the plan's
`maxMembers` and throws `QuotaLimitExceededException` (HTTP 403) at the ceiling, counting **all**
user rows so pending invitations hold a seat. Any invite path routed through `registerUser`
inherits it with no new code. A workspace admin already holds authority over every shared
credential in their workspace; withholding seat spend from them is an inconsistent line to draw.

### D2 — Do not auto-join invited users to the default workspace

Rejected as a fix for the land-nowhere defect:

- **No defensible role.** `VIEWER` makes the workspace useless; `EDITOR` over-grants to someone not
  yet placed on a team.
- **Arbitrary at scale.** `DEFAULT_WORKSPACE_ID` is an undeletable, unrenameable singleton. In a
  tenant organised around three real workspaces, placing every hire in the vestigial one grants
  real access.
- **Does not solve the problem.** Workspace admins still could not onboard anyone.

D1 makes the defect unreachable: invitation and placement become one operation.

### D3 — Claim link, not a chosen password

The new mutation takes no `password`. It provisions with `activated = false` and a generated
`activationKey`, and mails a link on which the recipient sets their own password. Beyond removing a
plaintext credential from email, this matters more under D1 than it did before: the existing
signature would otherwise let a workspace admin set a colleague's initial password and see it.

**The tenant-level `inviteUser` moves to the same claim link.** Its signature and dialog are
already being changed by D7, so aligning both paths now costs little and avoids shipping two
invitation emails with two different credential stories. The counter-argument — that a tenant admin
sometimes wants a password they can read out over a call — was considered and declined; an account
whose credential is dictated verbally is one whose credential the dictating party knows, which is
the property being removed.

Consequences, all in P1:

- `inviteUser` drops its `password` argument. This is a **breaking GraphQL change**: the mutation
  and `client/src/graphql/platform/user/inviteUser.graphql` both change, and the operation must be
  regenerated (`npx graphql-codegen`).
- `invitationEmail.html` loses the temporary-password paragraph, leaving greeting + claim link. The
  `email.invitation.*` message keys are unaffected.
- `useInviteUserDialogStore` loses `invitePassword` and `regeneratePassword`;
  `pages/settings/platform/users/util/password-utils.ts` is deleted along with its tests, unless
  another caller turns up — `generatePassword` and `isValidPassword` are exported, so check before
  removing.
- Both invitations converge on one template and one flow, differing only in whether workspace
  memberships accompany them.

### D4 — Automation invites at the workspace level; embedded invites at the tenant level

These are different surfaces with different tenancy, and both are correct as such:

- **Automation** — work is organised into workspaces, so membership and the invitation that
  creates it belong at the workspace level.
- **Embedded** — no embedded domain entity carries a `workspaceId`. Embedded is tenant-scoped, with
  `ConnectedUser` as its external-user axis: connected users are provisioned by the customer's
  application through the embedded SDK and belong to a connected user rather than a workspace
  member, which is exactly why embedded connections are force-written `PRIVATE`. Staff
  administering an embedded deployment are plain tenant users.

The existing tenant-level `inviteUser` is therefore **retained deliberately**, not deprecated — it
is the right primitive for embedded, and remains the way to provision an account with no workspace
(legitimate when creating another tenant admin). Its page already sits in `platformSettingsRoutes`,
which is mounted under both `/automation` and `/embedded`, so embedded needs no new surface.

### D7 — The tenant invite picks a list of workspaces, with a role for each

Inviting from the tenant Users page accepts an optional list of workspace assignments, so one
invitation can place someone on several teams at once and a single claim link grants all of it.
Only tenant admins can reach that page, and a tenant admin already administers every workspace, so
there is no privilege question to answer — the picker grants nothing they could not grant in
separate steps.

Per-workspace role rather than one role across the selection: a new hire is routinely `EDITOR` on
their own team and `VIEWER` elsewhere, and the input type costs nothing extra.

```graphql
input WorkspaceRoleAssignmentInput {
    workspaceId: ID!
    role: WorkspaceRole!
}
```

The list stays **optional**. Provisioning an account with no workspace remains legitimate — it is
how a second tenant admin is created — so an empty list must keep working rather than being
rejected. It does mean the land-nowhere state stays reachable from this path by choice, which is
the difference between a default and a constraint.

Together with D1 this closes the gap from both directions: a tenant admin can place someone in many
workspaces at invite time, and a workspace admin can invite into their own.

### D5 — Custom roles get a nullable `workspace_id`; null means tenant-global

> **Revised 2026-08-12**: the per-workspace tier was removed before release — custom roles are
> tenant-global only. See `2026-08-12-custom-roles-tenant-global-collapse-design.md`. The text
> below is kept for the original rationale.

Following the convention for new platform entities: a nullable `workspace_id BIGINT` column, not a
`workspace_custom_role` relation table. A custom role belongs to at most one workspace, so a
membership table would express a relationship that does not exist.

- **`NULL`** — tenant-global. Visible and assignable in every workspace; created by tenant admins.
  Existing rows keep working unchanged.
- **Non-null** — owned by that workspace. Visible and assignable only there; created by anyone with
  `WORKSPACE_MEMBER_MANAGE` in it.

`custom_role` is **unreleased** — absent from `v0.31.2` and introduced by a commit that is not an
ancestor of `master` (it exists only on the feature branch). The column is therefore added to the
init changelog **in place** rather than as a follow-up changeset. Local dev databases that already
ran the old init will drift; `scripts/dev/sync-local-schema-after-collapse.sh` patches both the
schema and the stale md5sums.

Assignment must be validated: `workspace_user.custom_role_id` may reference a role only if that
role is global or belongs to the same workspace. Without that check a workspace admin could assign
another workspace's private role by id.

### D6 — Inheritance is synthesized in the read path, never materialized

Tenant admins appear in the membership view as ADMIN with an `inherited` marker. Two ways to do it:

- **Materialize rows** — write a `workspace_user` row per tenant admin per workspace, on workspace
  creation and on promotion to tenant admin. Rejected: it duplicates a fact that
  `PermissionServiceImpl` already decides, and the copy drifts the moment a tenant admin is demoted
  or a workspace is created by a path that forgets the hook. A stale row would grant real access.
- **Synthesize on read** — `workspaceUsers` unions the stored rows with tenant admins projected as
  ADMIN. The authorization check remains the single source of truth; the view is derived from it
  and cannot disagree.

Synthesized entries are **locked**: role change and removal are rejected, because there is no row
to change. This matches the source model, where inherited roles are automatic and not editable at
the lower level.

## Design

### P1 — Server

New mutation in `workspace-user.graphqls`:

```graphql
"Invite a user to a workspace by email, provisioning a tenant account if none exists.
 Requires the WORKSPACE_MEMBER_MANAGE scope."
inviteWorkspaceUser(workspaceId: ID!, email: String!, role: WorkspaceRole!): WorkspaceUser!
```

Guarded exactly as `addWorkspaceUser` — `hasPermission(#workspaceId, 'Workspace',
'WORKSPACE_MEMBER_MANAGE')`, a scope rather than a role, so custom roles carrying it work with no
special case.

In one transaction:

1. Resolve the email. Reuse an existing `User`; otherwise provision via `userService.registerUser`
   with `ROLE_USER`, inheriting `enforceMemberQuota()`, the non-activated-user cleanup, and
   `activationKey` generation.
2. Write the membership through the existing `WorkspaceUserService.addWorkspaceUser`, sharing its
   `ALREADY_MEMBER` guard and `evictWorkspaceScopeCache` call rather than duplicating them.
3. Mail a claim link for a new account, or a "you were added to *workspace*" notice for an existing
   user — who has a password already and must not be sent another.

Failures: `ALREADY_MEMBER` when the email is already in this workspace; `QuotaLimitExceededException`
→ 403 at the member ceiling (a capacity limit, not retryable); `InvalidEmailException` as
`registerUser` already raises.

### P1 — Client

**New page: Settings → Users, scoped to the current workspace**, at
`client/src/ee/pages/settings/automation/users`. Gated on the `WORKSPACE_MEMBER_MANAGE` scope for
the current workspace (via `myWorkspaceScopes`), **not** on `ROLE_ADMIN` — that gate is what makes
the page reachable by a workspace admin at all.

It must offer both ways in, because they are different jobs:

- **Invite by email** → `inviteWorkspaceUser`. For someone with no account.
- **Add existing user** → `addWorkspaceUser`. For someone already in the tenant; a picker over
  existing users, no email, no seat consumed.

Plus change role and remove, against the existing mutations.

**Tenant Users page** (`pages/settings/platform/users`, `ROLE_ADMIN` + EE, mounted under both
`/automation` and `/embedded`) gains a workspace picker in its invite dialog per D7: a multi-select
of workspaces each with its own `WorkspaceRole`, empty by default. `inviteUser` gains the
corresponding optional `workspaces: [WorkspaceRoleAssignmentInput!]` argument and writes the
memberships after provisioning, in the same transaction. Per D3 it also drops `password`:
`useInviteUserDialogStore` gains the workspace-selection state and loses `invitePassword` /
`regeneratePassword`, and the dialog shows a workspace picker where the generated password used to
be.

The embedded surface shows the same dialog with the picker empty and no workspaces to choose —
acceptable, since embedded tenants have only the default workspace and never populate it.

Today the only *workspace* member-management surface is `WorkspaceUsersDialog`, reached from an overflow menu
in `ee/pages/settings/automation/workspaces` — a tenant-admin screen listing every workspace. A
workspace admin who cannot see that list cannot reach the dialog. The dialog stays: it answers a
different question ("manage the members of *that* workspace" from a cross-workspace view) and can
share the member-list and role-select components with the new page.

Existing guards to surface as inline errors rather than generic toasts: `validateNotSelfDemotion`
(an admin cannot demote themselves) and `validateNotLastAdmin` (the final admin cannot be removed
or demoted).

### P2 — Workspace-scoped custom roles

- `custom_role` gains nullable `workspace_id BIGINT` (init changelog, in place — see D5).
- `CustomRole` gains a `Long workspaceId` field — the boxed type, since null is a real state.
- `CustomRoleService` list/fetch become workspace-aware: a workspace sees its own roles plus
  globals. `customRoles` gains a `workspaceId` argument.
- `createCustomRole` accepts an optional `workspaceId`; creating a workspace-scoped role requires
  `WORKSPACE_MEMBER_MANAGE` there, creating a global one requires `ROLE_ADMIN`.
- `addWorkspaceUser` / `updateWorkspaceUserRole` validate that a referenced `custom_role_id` is
  global or belongs to the target workspace.
- Client: the custom-roles screen gains a scope column and a workspace selector on create.

### P3 — Explicit inheritance

- `WorkspaceUser` GraphQL type gains `inherited: Boolean!` (default false).
- `workspaceUsers` unions stored rows with tenant admins projected as `ADMIN, inherited: true`,
  deduplicating anyone who also holds a real row (their stored role wins — it is what the
  authorization path would use if they lost tenant admin).
- `updateWorkspaceUserRole` / `removeWorkspaceUser` reject a synthesized entry with a typed error.
- Client: inherited rows render with an "Inherited — tenant admin" badge and disabled actions.

## Testing

- `WorkspaceUserServiceImplTest` — invite provisions when unknown, reuses when known, rejects an
  existing member, propagates the quota rejection.
- Authorization test that `inviteWorkspaceUser` requires `WORKSPACE_MEMBER_MANAGE`, following the
  `UserManagementFacadeAuthorizationTest` pattern.
- **Regression tests for the land-nowhere defect**, both directions, since D7 keeps the empty case
  legitimate:
  - `inviteWorkspaceUser` — the invitee's `getUserWorkspaces` contains that workspace. Always.
  - `inviteUser` **with** workspace assignments — the invitee's `getUserWorkspaces` contains each
    assigned workspace.
  - `inviteUser` **without** assignments — still lands in no workspace, and that is asserted as
    intended behaviour rather than left untested, so a later change that "helpfully" auto-joins a
    default workspace fails loudly (see D2).
- `CustomRoleServiceTest` — a workspace sees its own roles plus globals and not another
  workspace's; assigning a foreign workspace's role by id is rejected.
- `PermissionServiceVisibilityTest` companion — a synthesized inherited entry cannot be updated or
  removed.
- Client: the page renders for a workspace admin without `ROLE_ADMIN`; last-admin, self-demotion
  and inherited-row errors render inline.

## As built

Recorded after implementation. Where the design assumed something that turned out not to exist, the
assumption is corrected here rather than quietly dropped.

**D7 needed a seam.** `platform-user-graphql` cannot depend on the automation modules, so the tenant
invite could not reach workspace membership directly. It inverts through a
`WorkspaceMembershipAssigner` SPI declared in `platform-user-api` and implemented in
`automation-configuration-service`. This turned out better than the direct call: `assign` runs inside
the invite's transaction, so a failed placement rolls the provisioned account back rather than
leaving a user half-placed.

**D3 was mostly deletion.** The claim-link mechanism already existed and was unused —
`creationEmail.html` links to `password-reset/finish?key=${user.resetKey}` and
`MailService.sendCreationEmail` had no production callers. Provisioning, activation and the mail were
extracted into `UserInvitationService` so both invite surfaces share one implementation.

**P2's client UI did not exist and was built.** The design said "the custom-roles screen gains a
scope column"; there was no such screen. `ee/pages/settings/automation/custom-roles/CustomRoles.tsx`
now lists roles with their scope (This workspace / All workspaces) and composes new workspace-owned
ones. Creating a tenant-global role is deliberately not offered here — it is assignable everywhere,
so it is a tenant-wide act.

**Custom role assignment did not exist and was built.** `WorkspaceUser.forCustomRole` appeared only
in a test, so P2's validation had no call site. `WorkspaceUserService.assignCustomRole` supplies one:
it rejects a role belonging to another workspace (the id travels in the request, so hiding it from
the list is not enough) and refuses to convert the last built-in admin, since a custom role's scopes
are no guarantee it can manage anything. Exposed as `assignWorkspaceUserCustomRole`; custom roles
appear in the members table's role picker alongside the built-in ones.

**Inherited entries now report the real cause.** They were rejected by `NOT_MEMBER`, whose message
("User 99 is not a member of workspace 7") describes the row rather than the access and reads as a
bug for someone who demonstrably administers the workspace. `missingMembership` checks whether the
target holds `ROLE_ADMIN` and raises `INHERITED_MEMBERSHIP` naming tenant admin as the source, with
the remedy.

**The add-existing-user picker is tenant-admin only.** Listing every account in the tenant is
`ROLE_ADMIN`-gated and deliberately so — it exposes the whole organisation's user list. A workspace
admin adds an existing colleague through invite-by-email instead, which reuses the account and
consumes no extra seat. The picker renders only for a tenant admin.

**Last-admin and self-demotion errors render inline.** The Users page carries an alert region fed by
each mutation's `onError`. These errors answer what the operator just tried, so they belong beside
the control rather than in a toast that outlives the page.
