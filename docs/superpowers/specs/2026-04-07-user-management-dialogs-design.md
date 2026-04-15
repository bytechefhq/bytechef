# User Management Dialogs Design

**Date:** 2026-04-07
**Status:** Approved

## Overview

Add "Add user" functionality to the existing `ProjectUsersDialog` and create a new `WorkspaceUsersDialog` for managing workspace members. Both dialogs use an inline add row with a searchable user combobox. User display is resolved server-side (email/name instead of raw userId).

## Backend

### 1. New GraphQL Schema: `workspace-user.graphqls`

**Location:** `server/ee/libs/automation/automation-configuration/automation-configuration-graphql/src/main/resources/graphql/workspace-user.graphqls`

```graphql
extend type Query {
    "List all users of a workspace. Requires at least VIEWER workspace role."
    workspaceUsers(workspaceId: ID!): [WorkspaceUser!]!
}

extend type Mutation {
    "Add a user to a workspace. Requires ADMIN workspace role."
    addWorkspaceUser(workspaceId: ID!, userId: ID!, role: WorkspaceRole!): WorkspaceUser!
    "Update a workspace user's role. Requires ADMIN workspace role."
    updateWorkspaceUserRole(workspaceId: ID!, userId: ID!, role: WorkspaceRole!): WorkspaceUser!
    "Remove a user from a workspace. Requires ADMIN workspace role."
    removeWorkspaceUser(workspaceId: ID!, userId: ID!): Boolean!
}

type WorkspaceUser {
    id: ID!
    workspaceId: ID!
    userId: ID!
    workspaceRole: WorkspaceRole
    user: WorkspaceUserInfo
    createdDate: String
}

type WorkspaceUserInfo {
    email: String!
    firstName: String
    lastName: String
}

enum WorkspaceRole {
    ADMIN
    EDITOR
    VIEWER
}
```

### 2. New Controller: `WorkspaceUserGraphQlController`

**Location:** `server/ee/libs/automation/automation-configuration/automation-configuration-graphql/src/main/java/com/bytechef/ee/automation/configuration/web/graphql/WorkspaceUserGraphQlController.java`

Follows the `ProjectUserGraphQlController` pattern:
- `@Controller`, `@ConditionalOnCoordinator`, `@ConditionalOnEEVersion`
- Query: `workspaceUsers(workspaceId)` — `@PreAuthorize("@permissionService.hasWorkspaceRole(#workspaceId, 'VIEWER')")`
- Mutations: `addWorkspaceUser`, `updateWorkspaceUserRole`, `removeWorkspaceUser` — all `@PreAuthorize("@permissionService.hasWorkspaceRole(#workspaceId, 'ADMIN')")`
- `@SchemaMapping(typeName = "WorkspaceUser", field = "user")` resolver that calls `UserService` to resolve `userId` to email/name

### 3. Update `project-user.graphqls`

Add nested user info to `ProjectUser` type:

```graphql
type ProjectUser {
    id: ID!
    projectId: ID!
    userId: ID!
    projectRole: ProjectRole
    customRoleId: ID
    user: ProjectUserInfo
    createdDate: String
}

type ProjectUserInfo {
    email: String!
    firstName: String
    lastName: String
}
```

### 4. Update `ProjectUserGraphQlController`

Add `@SchemaMapping(typeName = "ProjectUser", field = "user")` resolver that calls `UserService` to resolve `userId` to email/name.

### 5. Test Configuration

Add `WorkspaceUserService` mock to `AutomationConfigurationGraphQlConfigurationSharedMocks` and `AutomationConfigurationGraphQlTestConfiguration` (same pattern as existing mocks). Add `UserService` mock if not already present.

## Frontend

### 1. Run Codegen

Run `cd client && npx graphql-codegen` after creating all `.graphql` operation files to generate TypeScript hooks.

### 2. New Client GraphQL Operation Files

**Location:** `client/src/graphql/automation/configuration/`

- `workspaceUsers.graphql` — query WorkspaceUsers with nested user field
- `addWorkspaceUser.graphql` — mutation
- `updateWorkspaceUserRole.graphql` — mutation
- `removeWorkspaceUser.graphql` — mutation

### 3. Update Existing GraphQL Operation Files

Update `projectUsers.graphql`, `addProjectUser.graphql`, `updateProjectUserRole.graphql` to include the nested `user { email firstName lastName }` field in their selection sets.

### 4. Update `ProjectUsersDialog.tsx`

**Changes:**
- Display user email (with firstName/lastName) instead of raw userId in the table
- Add "Add user" button below the table (visible only when `canManageMembers`)
- Clicking reveals an inline row with:
  - Searchable combobox (using `Command` component from cmdk) querying platform users via `useUsersQuery`, filtering out already-added project users
  - `Select` dropdown for role (ADMIN, EDITOR, OPERATOR, VIEWER)
  - Confirm button (check icon) that calls `useAddProjectUserMutation`
  - Cancel button (X icon) that hides the row
- Invalidate `ProjectUsers` query on successful add

### 5. New `WorkspaceUsersDialog.tsx`

**Location:** `client/src/ee/pages/settings/automation/workspaces/components/WorkspaceUsersDialog.tsx`

**Structure (mirrors ProjectUsersDialog):**
- Dialog with table: User (email/name), Role, Added date, Remove button
- Inline "Add user" row with combobox + role selector (ADMIN, EDITOR, VIEWER)
- Management actions (add/edit role/remove) visible only to workspace ADMINs
- Authorization check via `useMyWorkspaceRoleQuery` — show management UI only when role is `ADMIN`

### 6. Update `WorkspaceListItem.tsx`

**Location:** `client/src/ee/pages/settings/automation/workspaces/components/WorkspaceListItem.tsx`

Add a "Members" button/icon that opens `WorkspaceUsersDialog` for the workspace.

## UI Pattern: Inline Add Row

Both dialogs share the same "Add user" inline row pattern:

```
┌─────────────────────────────────────────────────────┐
│ User (email)          │ Role      │ Added    │       │
├───────────────────────┼───────────┼──────────┼───────┤
│ alice@example.com     │ ADMIN  ▼  │ Apr 1    │  🗑   │
│ bob@example.com       │ EDITOR ▼  │ Apr 3    │  🗑   │
├───────────────────────┼───────────┼──────────┼───────┤
│ [🔍 Search users... ] │ [EDITOR▼] │   ✓  ✕   │       │  ← inline add row
└─────────────────────────────────────────────────────┘
                              [ + Add user ]
```

The combobox searches platform users by email/name, filters out users already in the project/workspace, and displays matches as `email — First Last`.

## File Summary

**New files:**
- `server/.../graphql/workspace-user.graphqls`
- `server/.../web/graphql/WorkspaceUserGraphQlController.java`
- `client/src/graphql/automation/configuration/workspaceUsers.graphql`
- `client/src/graphql/automation/configuration/addWorkspaceUser.graphql`
- `client/src/graphql/automation/configuration/updateWorkspaceUserRole.graphql`
- `client/src/graphql/automation/configuration/removeWorkspaceUser.graphql`
- `client/src/ee/pages/settings/automation/workspaces/components/WorkspaceUsersDialog.tsx`

**Modified files:**
- `server/.../graphql/project-user.graphqls` — add `user` field and `ProjectUserInfo` type
- `server/.../web/graphql/ProjectUserGraphQlController.java` — add `@SchemaMapping` user resolver
- `server/.../web/graphql/config/AutomationConfigurationGraphQlConfigurationSharedMocks.java` — add mocks
- `server/.../web/graphql/config/AutomationConfigurationGraphQlTestConfiguration.java` — add mock beans
- `client/src/graphql/automation/configuration/projectUsers.graphql` — add nested user field
- `client/src/graphql/automation/configuration/addProjectUser.graphql` — add nested user field
- `client/src/graphql/automation/configuration/updateProjectUserRole.graphql` — add nested user field
- `client/src/pages/automation/project/.../ProjectUsersDialog.tsx` — add inline add row, show email/name
- `client/src/ee/pages/settings/automation/workspaces/components/WorkspaceListItem.tsx` — add Members button
- `client/src/shared/middleware/graphql.ts` — regenerated by codegen
