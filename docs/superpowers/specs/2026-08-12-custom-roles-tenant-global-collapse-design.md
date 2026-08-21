# Custom roles: collapse to tenant-global only

- **Date**: 2026-08-12
- **Status**: Approved
- **Ticket**: 1051 (workspace membership & roles umbrella)
- **Revises**: D5 of `2026-08-11-workspace-membership-and-roles-design.md`

## Context

D5 of the membership spec gave `custom_role` a nullable `workspace_id`: null meant tenant-global
(assignable everywhere, managed by tenant admins), non-null meant owned by one workspace (managed
by anyone holding `WORKSPACE_MEMBER_MANAGE` there). Both tiers were built: dual-branch
`@PreAuthorize` on every `CustomRoleService` method, a `validateRoleTier` cross-tier check on
update/delete, a `findAllVisibleToWorkspace` union query, assignment validation against
cross-workspace role ids, two partial unique indexes, and two settings pages sharing
`CustomRolesManager`.

Reviewing the result, the per-workspace tier buys exactly one capability — a workspace admin who
is not a tenant admin can define roles for their own workspace — and pays for it with a
two-surface management UX ("Roles created here belong to this workspace. Roles that apply
everywhere are managed in tenant settings"), possible name shadowing between tiers in the assign
picker, and the server machinery above. Comparable products (GitHub, GitLab, Datadog) define
custom roles at the org/account level only; per-team role definition is rare. The delegation
benefit is speculative, the complexity is concrete.

`custom_role` is unreleased — absent from `v0.31.2` — so the collapse is a clean deletion with no
migration or compatibility shim.

## Decision

Custom roles are **tenant-global only**. One definition surface (tenant settings), managed by
tenant admins. Assignment stays per-member per-workspace via `workspace_user.custom_role_id`,
unchanged. Workspace admins assign roles; they do not define them.

If per-workspace definition ever becomes a real demand, the column returns as a feature addition
(nullable column, additive changesets), not a migration crisis.

## Design

### Schema

- `CustomRole` loses the `workspaceId` field, the 3-arg constructor, and `getWorkspaceId()`.
- The init changelog `202604061200030_automation_configuration_added_table_custom_role.xml` is
  edited **in place** (unreleased schema, same justification D5 already recorded): drop the
  `workspace_id` column and both partial unique indexes
  (`ux_custom_role_global_name`, `ux_custom_role_workspace_name`); add one plain unique
  constraint on `name`.
- `scripts/dev/sync-local-schema-after-collapse.sh` gains an idempotent patch step: drop the
  column and partial indexes if present, create the unique constraint, fix the changeset md5sum.
  Any workspace-scoped roles in a local dev DB silently become global — acceptable for dev data.
- `CustomRoleRepository.findAllVisibleToWorkspace` is deleted.

### Server

- `createCustomRole`, `updateCustomRole`, `deleteCustomRole` lose the `workspaceId` parameter and
  single-branch to `@PreAuthorize("isTenantAdmin()")`.
- `validateRoleTier` is deleted, along with its tier-mismatch `ConfigurationException` path.
- `getCustomRoles(Long workspaceId)` is the **deliberate remnant**: the read has two audiences —
  the tenant admin managing roles and a workspace admin populating the invite/assign picker. It
  keeps the dual-branch gate (`isTenantAdmin()` when null, `WORKSPACE_MEMBER_MANAGE` on the
  workspace when set) but the body returns `findAll()` either way; `workspaceId` is authorization
  context, not a filter.
- `WorkspaceUserServiceImpl.validateCustomRoleAssignable` shrinks to an existence check. The
  "belongs to another workspace" branch dies; the orphan-id rejection stays (a dangling
  `custom_role_id` would fail closed at permission-check time and invisibly lock the member out,
  so writes must still reject it loudly).
- GraphQL (`custom-role.graphqls`): `workspaceId` is removed from the `CustomRole` type, from
  `CreateCustomRoleInput` and `UpdateCustomRoleInput`, and from the `deleteCustomRole` mutation.
  The `customRoles(workspaceId: ID)` query keeps its argument with docs rewritten to the
  authorization-context semantics. `CustomRoleGraphQlController` mappings follow.

### Client

- Delete the workspace settings page
  (`client/src/ee/pages/settings/automation/custom-roles/CustomRoles.tsx`), its route, and the
  "Custom Roles" entry under Current Workspace in the settings sidebar.
- `GlobalCustomRoles.tsx` (tenant settings) becomes the only management surface.
  `CustomRolesManager` drops the `workspaceId` prop, the Scope column, and the two-tier
  explanatory copy.
- The workspace invite/assign picker keeps its `customRoles(workspaceId)` query unchanged.
- Regenerate the GraphQL client (`npx graphql-codegen`).

### Tests and docs

- Update `CustomRoleServiceTest`, `WorkspaceUserServiceTest`, `PreAuthorizeAnnotationTest`,
  `RbacMigrationsIntTest`, and the client page tests. Tier-mismatch and cross-tier-visibility
  cases are **deleted, not rewritten** — the states they pin no longer exist.
- D5 of the membership spec gets a revision pointer to this document.

## What is given up

A workspace admin who is not a tenant admin can assign custom roles to members but can never
define one. Role definition is centralized. This is on record as the accepted cost.

## Decisions log

- **Keep both tiers** — rejected. The delegation capability is speculative; nobody asked for it,
  and the UX cost (two management surfaces, tier name shadowing) lands on every tenant admin.
- **Hide the workspace UI, keep the schema** — rejected. Keeps all server complexity for a
  feature nobody can reach; worst of both.
- **Single-branch the read too** (`isTenantAdmin()` on `getCustomRoles`) — rejected. It would
  break the invite/assign picker for workspace admins, who legitimately need the role list to
  assign from. The dual-branch read is the one remnant of the two-audience reality.
- **Gate the read on `isAuthenticated()`** — rejected. Role definitions are not secrets, but only
  `WORKSPACE_MEMBER_MANAGE` holders have any use for the list; defaulting to the narrower gate
  costs nothing.
