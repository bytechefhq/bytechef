# User Management Dialogs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add "Add user" UI to ProjectUsersDialog and create WorkspaceUsersDialog with full backend GraphQL layer.

**Architecture:** Backend adds a new `workspace-user.graphqls` schema and `WorkspaceUserGraphQlController`, plus `@SchemaMapping` user resolvers on both project and workspace user controllers to resolve userId to email/name. Frontend updates ProjectUsersDialog with inline add row, creates WorkspaceUsersDialog with same pattern, and adds codegen schema path for EE automation-configuration module.

**Tech Stack:** Spring GraphQL, Spring Security `@PreAuthorize`, React, TanStack Query, cmdk combobox, shadcn/ui components.

---

### Task 1: Add workspace-user.graphqls schema

**Files:**
- Create: `server/ee/libs/automation/automation-configuration/automation-configuration-graphql/src/main/resources/graphql/workspace-user.graphqls`

- [ ] **Step 1: Create the GraphQL schema file**

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

- [ ] **Step 2: Update project-user.graphqls to add user info fields**

Modify `server/ee/libs/automation/automation-configuration/automation-configuration-graphql/src/main/resources/graphql/project-user.graphqls`. Add the `user` field and `ProjectUserInfo` type:

```graphql
type ProjectUser {
    id: ID!
    projectId: ID!
    userId: ID!
    "Built-in project role, null if using a custom role"
    projectRole: ProjectRole
    "Custom role ID (EE only), null if using a built-in role"
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

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-configuration/automation-configuration-graphql/src/main/resources/graphql/workspace-user.graphqls \
  server/ee/libs/automation/automation-configuration/automation-configuration-graphql/src/main/resources/graphql/project-user.graphqls
git commit -m "Add workspace-user GraphQL schema and user info to ProjectUser type"
```

---

### Task 2: Create WorkspaceUserGraphQlController

**Files:**
- Create: `server/ee/libs/automation/automation-configuration/automation-configuration-graphql/src/main/java/com/bytechef/ee/automation/configuration/web/graphql/WorkspaceUserGraphQlController.java`
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-graphql/build.gradle.kts`

- [ ] **Step 1: Add platform-user-api dependency to build.gradle.kts**

Add this line to the `dependencies` block in `server/ee/libs/automation/automation-configuration/automation-configuration-graphql/build.gradle.kts`:

```kotlin
implementation(project(":server:libs:platform:platform-user:platform-user-api"))
```

- [ ] **Step 2: Create the controller**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
@ConditionalOnEEVersion
@SuppressFBWarnings("EI2")
public class WorkspaceUserGraphQlController {

    private final UserService userService;
    private final WorkspaceUserService workspaceUserService;

    public WorkspaceUserGraphQlController(
        UserService userService, WorkspaceUserService workspaceUserService) {

        this.userService = userService;
        this.workspaceUserService = workspaceUserService;
    }

    @QueryMapping
    @PreAuthorize("@permissionService.hasWorkspaceRole(#workspaceId, 'VIEWER')")
    public List<WorkspaceUser> workspaceUsers(@Argument long workspaceId) {
        return workspaceUserService.getWorkspaceWorkspaceUsers(workspaceId);
    }

    @MutationMapping
    @PreAuthorize("@permissionService.hasWorkspaceRole(#workspaceId, 'ADMIN')")
    public WorkspaceUser addWorkspaceUser(
        @Argument long workspaceId, @Argument long userId, @Argument WorkspaceRole role) {

        return workspaceUserService.addWorkspaceUser(userId, workspaceId, role.name());
    }

    @MutationMapping
    @PreAuthorize("@permissionService.hasWorkspaceRole(#workspaceId, 'ADMIN')")
    public WorkspaceUser updateWorkspaceUserRole(
        @Argument long workspaceId, @Argument long userId, @Argument WorkspaceRole role) {

        return workspaceUserService.updateWorkspaceUserRole(userId, workspaceId, role.name());
    }

    @MutationMapping
    @PreAuthorize("@permissionService.hasWorkspaceRole(#workspaceId, 'ADMIN')")
    public boolean removeWorkspaceUser(@Argument long workspaceId, @Argument long userId) {
        workspaceUserService.deleteWorkspaceUser(userId);

        return true;
    }

    @SchemaMapping(typeName = "WorkspaceUser", field = "user")
    public WorkspaceUserInfo user(WorkspaceUser workspaceUser) {
        User user = userService.getUser(workspaceUser.getUserId());

        return new WorkspaceUserInfo(user.getEmail(), user.getFirstName(), user.getLastName());
    }

    public record WorkspaceUserInfo(String email, String firstName, String lastName) {
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-configuration/automation-configuration-graphql/
git commit -m "Add WorkspaceUserGraphQlController with CRUD operations and user resolver"
```

---

### Task 3: Add @SchemaMapping user resolver to ProjectUserGraphQlController

**Files:**
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-graphql/src/main/java/com/bytechef/ee/automation/configuration/web/graphql/ProjectUserGraphQlController.java`

- [ ] **Step 1: Add UserService field, update constructor, add SchemaMapping resolver**

Add `UserService` import and field. Update the constructor to accept `UserService`. Add the resolver method and inner record:

```java
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
```

Add field:
```java
private final UserService userService;
```

Update constructor to:
```java
public ProjectUserGraphQlController(
    PermissionService permissionService, ProjectUserService projectUserService,
    UserService userService) {

    this.permissionService = permissionService;
    this.projectUserService = projectUserService;
    this.userService = userService;
}
```

Add resolver method and record before the closing brace:
```java
@SchemaMapping(typeName = "ProjectUser", field = "user")
public ProjectUserInfo user(ProjectUser projectUser) {
    User user = userService.getUser(projectUser.getUserId());

    return new ProjectUserInfo(user.getEmail(), user.getFirstName(), user.getLastName());
}

public record ProjectUserInfo(String email, String firstName, String lastName) {
}
```

- [ ] **Step 2: Compile to verify**

Run: `./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-graphql:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-configuration/automation-configuration-graphql/src/main/java/
git commit -m "Add @SchemaMapping user resolver to ProjectUserGraphQlController"
```

---

### Task 4: Update GraphQL test configuration

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-graphql/src/test/java/com/bytechef/automation/configuration/web/graphql/config/AutomationConfigurationGraphQlConfigurationSharedMocks.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-graphql/src/test/java/com/bytechef/automation/configuration/web/graphql/config/AutomationConfigurationGraphQlTestConfiguration.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-graphql/src/test/resources/graphql/test.graphqls`

- [ ] **Step 1: Add WorkspaceUserService and UserService to shared mocks**

In `AutomationConfigurationGraphQlConfigurationSharedMocks.java`, add imports:
```java
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.platform.user.service.UserService;
```

Add to `@MockitoBean(types = {...})`:
```java
UserService.class, WorkspaceUserService.class
```

- [ ] **Step 2: Add mock beans to test configuration**

In `AutomationConfigurationGraphQlTestConfiguration.java`, add imports:
```java
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.platform.user.service.UserService;
```

Add bean methods:
```java
@Bean
@Primary
public UserService userService() {
    return Mockito.mock(UserService.class);
}

@Bean
@Primary
public WorkspaceUserService workspaceUserService() {
    return Mockito.mock(WorkspaceUserService.class);
}
```

- [ ] **Step 3: Add stub types to test.graphqls**

Add to `server/libs/automation/automation-configuration/automation-configuration-graphql/src/test/resources/graphql/test.graphqls`:
```graphql
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

type ProjectUserInfo {
    email: String!
    firstName: String
    lastName: String
}
```

Also update the existing `ProjectUser` stub if present to add the `user` field.

- [ ] **Step 4: Run integration tests to verify**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-graphql:testIntegration`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add server/libs/automation/automation-configuration/automation-configuration-graphql/src/test/
git commit -m "Update GraphQL test config with workspace user and user service mocks"
```

---

### Task 5: Add codegen schema path and create client GraphQL operation files

**Files:**
- Modify: `client/codegen.ts`
- Modify: `client/src/graphql/automation/configuration/projectUsers.graphql`
- Modify: `client/src/graphql/automation/configuration/addProjectUser.graphql`
- Modify: `client/src/graphql/automation/configuration/updateProjectUserRole.graphql`
- Create: `client/src/graphql/automation/configuration/workspaceUsers.graphql`
- Create: `client/src/graphql/automation/configuration/addWorkspaceUser.graphql`
- Create: `client/src/graphql/automation/configuration/updateWorkspaceUserRole.graphql`
- Create: `client/src/graphql/automation/configuration/removeWorkspaceUser.graphql`

- [ ] **Step 1: Add EE automation-configuration schema path to codegen.ts**

Add this line to the `schema` array in `client/codegen.ts` (after the existing `automation-configuration` entry):
```typescript
'../server/ee/libs/automation/automation-configuration/automation-configuration-graphql/src/main/resources/graphql/*.graphqls',
```

- [ ] **Step 2: Update existing project user .graphql files to include user field**

`projectUsers.graphql`:
```graphql
query ProjectUsers($projectId: ID!) {
    projectUsers(projectId: $projectId) {
        id
        projectId
        userId
        projectRole
        user {
            email
            firstName
            lastName
        }
        createdDate
    }
}
```

`addProjectUser.graphql`:
```graphql
mutation AddProjectUser($projectId: ID!, $userId: ID!, $role: ProjectRole!) {
    addProjectUser(projectId: $projectId, userId: $userId, role: $role) {
        id
        projectId
        userId
        projectRole
        user {
            email
            firstName
            lastName
        }
    }
}
```

`updateProjectUserRole.graphql`:
```graphql
mutation UpdateProjectUserRole($projectId: ID!, $userId: ID!, $role: ProjectRole!) {
    updateProjectUserRole(projectId: $projectId, userId: $userId, role: $role) {
        id
        projectRole
    }
}
```

- [ ] **Step 3: Create workspace user .graphql operation files**

`workspaceUsers.graphql`:
```graphql
query WorkspaceUsers($workspaceId: ID!) {
    workspaceUsers(workspaceId: $workspaceId) {
        id
        workspaceId
        userId
        workspaceRole
        user {
            email
            firstName
            lastName
        }
        createdDate
    }
}
```

`addWorkspaceUser.graphql`:
```graphql
mutation AddWorkspaceUser($workspaceId: ID!, $userId: ID!, $role: WorkspaceRole!) {
    addWorkspaceUser(workspaceId: $workspaceId, userId: $userId, role: $role) {
        id
        workspaceId
        userId
        workspaceRole
        user {
            email
            firstName
            lastName
        }
    }
}
```

`updateWorkspaceUserRole.graphql`:
```graphql
mutation UpdateWorkspaceUserRole($workspaceId: ID!, $userId: ID!, $role: WorkspaceRole!) {
    updateWorkspaceUserRole(workspaceId: $workspaceId, userId: $userId, role: $role) {
        id
        workspaceRole
    }
}
```

`removeWorkspaceUser.graphql`:
```graphql
mutation RemoveWorkspaceUser($workspaceId: ID!, $userId: ID!) {
    removeWorkspaceUser(workspaceId: $workspaceId, userId: $userId)
}
```

- [ ] **Step 4: Run codegen**

Run: `cd client && npx graphql-codegen`
Expected: Generates updated `src/shared/middleware/graphql.ts` with `useProjectUsersQuery`, `useAddProjectUserMutation`, `useWorkspaceUsersQuery`, `useAddWorkspaceUserMutation`, etc.

- [ ] **Step 5: Commit**

```bash
git add client/codegen.ts client/src/graphql/automation/configuration/ client/src/shared/middleware/graphql.ts
git commit -m "client - Add workspace user GraphQL operations and update project user operations with user info"
```

---

### Task 6: Update ProjectUsersDialog with Add User UI and email display

**Files:**
- Modify: `client/src/pages/automation/project/components/project-header/components/settings-menu/components/ProjectUsersDialog.tsx`

- [ ] **Step 1: Rewrite ProjectUsersDialog with inline add row and email display**

```tsx
import {Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList} from '@/components/ui/command';
import {Dialog, DialogContent, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {useHasProjectScope} from '@/shared/hooks/useHasProjectScope';
import {
    ProjectRole,
    useAddProjectUserMutation,
    useProjectUsersQuery,
    useRemoveProjectUserMutation,
    useUpdateProjectUserRoleMutation,
    useUsersQuery,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {CheckIcon, PlusIcon, Trash2Icon, XIcon} from 'lucide-react';
import {useState} from 'react';

const PROJECT_ROLES = ['ADMIN', 'EDITOR', 'OPERATOR', 'VIEWER'] as const;

interface ProjectUsersDialogProps {
    onClose: () => void;
    open: boolean;
    projectId: number;
}

const ProjectUsersDialog = ({onClose, open, projectId}: ProjectUsersDialogProps) => {
    const [addingUser, setAddingUser] = useState(false);
    const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
    const [selectedRole, setSelectedRole] = useState<ProjectRole>(ProjectRole.Editor);
    const [userSearchOpen, setUserSearchOpen] = useState(false);

    const queryClient = useQueryClient();
    const canManageMembers = useHasProjectScope(projectId, 'PROJECT_MANAGE_MEMBERS');

    const {data: usersData} = useProjectUsersQuery({projectId: String(projectId)}, {enabled: open});

    const projectUsers = usersData?.projectUsers ?? [];

    const {data: allUsersData} = useUsersQuery({pageNumber: 0, pageSize: 100}, {enabled: open && addingUser});

    const allUsers = allUsersData?.users?.content ?? [];

    const existingUserIds = new Set(projectUsers.map((projectUser) => projectUser.userId));

    const availableUsers = allUsers.filter((user) => !existingUserIds.has(user.id));

    const invalidateProjectUsers = () => {
        queryClient.invalidateQueries({queryKey: ['ProjectUsers']});
    };

    const addUserMutation = useAddProjectUserMutation({onSuccess: invalidateProjectUsers});

    const updateRoleMutation = useUpdateProjectUserRoleMutation({onSuccess: invalidateProjectUsers});

    const removeUserMutation = useRemoveProjectUserMutation({onSuccess: invalidateProjectUsers});

    const handleAddConfirm = () => {
        if (selectedUserId) {
            addUserMutation.mutate({
                projectId: String(projectId),
                role: selectedRole,
                userId: selectedUserId,
            });

            setAddingUser(false);
            setSelectedUserId(null);
            setSelectedRole(ProjectRole.Editor);
        }
    };

    const handleAddCancel = () => {
        setAddingUser(false);
        setSelectedUserId(null);
        setSelectedRole(ProjectRole.Editor);
    };

    const selectedUserLabel =
        availableUsers.find((user) => user.id === selectedUserId)?.email || 'Search users...';

    return (
        <Dialog onOpenChange={onClose} open={open}>
            <DialogContent className="max-w-2xl">
                <DialogHeader>
                    <DialogTitle>Project Members</DialogTitle>
                </DialogHeader>

                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>User</TableHead>

                            <TableHead>Role</TableHead>

                            <TableHead>Added</TableHead>

                            {canManageMembers && <TableHead className="w-12" />}
                        </TableRow>
                    </TableHeader>

                    <TableBody>
                        {projectUsers.map((projectUser) => (
                            <TableRow key={projectUser.id}>
                                <TableCell>
                                    <div>
                                        <div className="text-sm font-medium">
                                            {projectUser.user?.email || `User ${projectUser.userId}`}
                                        </div>

                                        {projectUser.user?.firstName && (
                                            <div className="text-xs text-muted-foreground">
                                                {projectUser.user.firstName} {projectUser.user.lastName}
                                            </div>
                                        )}
                                    </div>
                                </TableCell>

                                <TableCell>
                                    {canManageMembers ? (
                                        <Select
                                            onValueChange={(role) =>
                                                updateRoleMutation.mutate({
                                                    projectId: String(projectId),
                                                    role: role as ProjectRole,
                                                    userId: projectUser.userId,
                                                })
                                            }
                                            value={projectUser.projectRole ?? undefined}
                                        >
                                            <SelectTrigger className="w-32">
                                                <SelectValue />
                                            </SelectTrigger>

                                            <SelectContent>
                                                {PROJECT_ROLES.map((role) => (
                                                    <SelectItem key={role} value={role}>
                                                        {role}
                                                    </SelectItem>
                                                ))}
                                            </SelectContent>
                                        </Select>
                                    ) : (
                                        projectUser.projectRole
                                    )}
                                </TableCell>

                                <TableCell>
                                    {projectUser.createdDate
                                        ? new Date(projectUser.createdDate).toLocaleDateString()
                                        : '-'}
                                </TableCell>

                                {canManageMembers && (
                                    <TableCell>
                                        <button
                                            className="text-destructive hover:text-destructive/80"
                                            onClick={() =>
                                                removeUserMutation.mutate({
                                                    projectId: String(projectId),
                                                    userId: projectUser.userId,
                                                })
                                            }
                                        >
                                            <Trash2Icon className="size-4" />
                                        </button>
                                    </TableCell>
                                )}
                            </TableRow>
                        ))}

                        {addingUser && (
                            <TableRow>
                                <TableCell>
                                    <Popover onOpenChange={setUserSearchOpen} open={userSearchOpen}>
                                        <PopoverTrigger asChild>
                                            <button className="w-full rounded-md border px-3 py-2 text-left text-sm">
                                                {selectedUserId ? selectedUserLabel : 'Search users...'}
                                            </button>
                                        </PopoverTrigger>

                                        <PopoverContent align="start" className="w-64 p-0">
                                            <Command>
                                                <CommandInput placeholder="Search by email..." />

                                                <CommandList>
                                                    <CommandEmpty>No users found.</CommandEmpty>

                                                    <CommandGroup>
                                                        {availableUsers.map((user) => (
                                                            <CommandItem
                                                                key={user.id}
                                                                onSelect={() => {
                                                                    setSelectedUserId(user.id);
                                                                    setUserSearchOpen(false);
                                                                }}
                                                                value={user.email}
                                                            >
                                                                <div>
                                                                    <div className="text-sm">{user.email}</div>

                                                                    {user.firstName && (
                                                                        <div className="text-xs text-muted-foreground">
                                                                            {user.firstName} {user.lastName}
                                                                        </div>
                                                                    )}
                                                                </div>
                                                            </CommandItem>
                                                        ))}
                                                    </CommandGroup>
                                                </CommandList>
                                            </Command>
                                        </PopoverContent>
                                    </Popover>
                                </TableCell>

                                <TableCell>
                                    <Select
                                        onValueChange={(role) => setSelectedRole(role as ProjectRole)}
                                        value={selectedRole}
                                    >
                                        <SelectTrigger className="w-32">
                                            <SelectValue />
                                        </SelectTrigger>

                                        <SelectContent>
                                            {PROJECT_ROLES.map((role) => (
                                                <SelectItem key={role} value={role}>
                                                    {role}
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                </TableCell>

                                <TableCell>
                                    <div className="flex gap-1">
                                        <button
                                            className="rounded p-1 hover:bg-accent disabled:opacity-50"
                                            disabled={!selectedUserId}
                                            onClick={handleAddConfirm}
                                        >
                                            <CheckIcon className="size-4" />
                                        </button>

                                        <button className="rounded p-1 hover:bg-accent" onClick={handleAddCancel}>
                                            <XIcon className="size-4" />
                                        </button>
                                    </div>
                                </TableCell>

                                {canManageMembers && <TableCell />}
                            </TableRow>
                        )}
                    </TableBody>
                </Table>

                {canManageMembers && !addingUser && (
                    <button
                        className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
                        onClick={() => setAddingUser(true)}
                    >
                        <PlusIcon className="size-4" />
                        Add user
                    </button>
                )}
            </DialogContent>
        </Dialog>
    );
};

export default ProjectUsersDialog;
```

- [ ] **Step 2: Verify it compiles**

Run: `cd client && npm run typecheck`
Expected: No errors related to ProjectUsersDialog

- [ ] **Step 3: Commit**

```bash
git add client/src/pages/automation/project/components/project-header/components/settings-menu/components/ProjectUsersDialog.tsx
git commit -m "client - Add inline Add User row and email display to ProjectUsersDialog"
```

---

### Task 7: Create WorkspaceUsersDialog

**Files:**
- Create: `client/src/ee/pages/settings/automation/workspaces/components/WorkspaceUsersDialog.tsx`

- [ ] **Step 1: Create the dialog component**

```tsx
import {Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList} from '@/components/ui/command';
import {Dialog, DialogContent, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {
    useAddWorkspaceUserMutation,
    useMyWorkspaceRoleQuery,
    useRemoveWorkspaceUserMutation,
    useUpdateWorkspaceUserRoleMutation,
    useUsersQuery,
    useWorkspaceUsersQuery,
    WorkspaceRole,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {CheckIcon, PlusIcon, Trash2Icon, XIcon} from 'lucide-react';
import {useState} from 'react';

const WORKSPACE_ROLES = ['ADMIN', 'EDITOR', 'VIEWER'] as const;

interface WorkspaceUsersDialogProps {
    onClose: () => void;
    open: boolean;
    workspaceId: number;
}

const WorkspaceUsersDialog = ({onClose, open, workspaceId}: WorkspaceUsersDialogProps) => {
    const [addingUser, setAddingUser] = useState(false);
    const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
    const [selectedRole, setSelectedRole] = useState<WorkspaceRole>(WorkspaceRole.Editor);
    const [userSearchOpen, setUserSearchOpen] = useState(false);

    const queryClient = useQueryClient();

    const {data: roleData} = useMyWorkspaceRoleQuery(
        {workspaceId: String(workspaceId)},
        {enabled: open}
    );

    const canManageMembers = roleData?.myWorkspaceRole === 'ADMIN';

    const {data: usersData} = useWorkspaceUsersQuery(
        {workspaceId: String(workspaceId)},
        {enabled: open}
    );

    const workspaceUsers = usersData?.workspaceUsers ?? [];

    const {data: allUsersData} = useUsersQuery({pageNumber: 0, pageSize: 100}, {enabled: open && addingUser});

    const allUsers = allUsersData?.users?.content ?? [];

    const existingUserIds = new Set(workspaceUsers.map((workspaceUser) => workspaceUser.userId));

    const availableUsers = allUsers.filter((user) => !existingUserIds.has(user.id));

    const invalidateWorkspaceUsers = () => {
        queryClient.invalidateQueries({queryKey: ['WorkspaceUsers']});
    };

    const addUserMutation = useAddWorkspaceUserMutation({onSuccess: invalidateWorkspaceUsers});

    const updateRoleMutation = useUpdateWorkspaceUserRoleMutation({onSuccess: invalidateWorkspaceUsers});

    const removeUserMutation = useRemoveWorkspaceUserMutation({onSuccess: invalidateWorkspaceUsers});

    const handleAddConfirm = () => {
        if (selectedUserId) {
            addUserMutation.mutate({
                role: selectedRole,
                userId: selectedUserId,
                workspaceId: String(workspaceId),
            });

            setAddingUser(false);
            setSelectedUserId(null);
            setSelectedRole(WorkspaceRole.Editor);
        }
    };

    const handleAddCancel = () => {
        setAddingUser(false);
        setSelectedUserId(null);
        setSelectedRole(WorkspaceRole.Editor);
    };

    const selectedUserLabel =
        availableUsers.find((user) => user.id === selectedUserId)?.email || 'Search users...';

    return (
        <Dialog onOpenChange={onClose} open={open}>
            <DialogContent className="max-w-2xl">
                <DialogHeader>
                    <DialogTitle>Workspace Members</DialogTitle>
                </DialogHeader>

                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>User</TableHead>

                            <TableHead>Role</TableHead>

                            <TableHead>Added</TableHead>

                            {canManageMembers && <TableHead className="w-12" />}
                        </TableRow>
                    </TableHeader>

                    <TableBody>
                        {workspaceUsers.map((workspaceUser) => (
                            <TableRow key={workspaceUser.id}>
                                <TableCell>
                                    <div>
                                        <div className="text-sm font-medium">
                                            {workspaceUser.user?.email || `User ${workspaceUser.userId}`}
                                        </div>

                                        {workspaceUser.user?.firstName && (
                                            <div className="text-xs text-muted-foreground">
                                                {workspaceUser.user.firstName} {workspaceUser.user.lastName}
                                            </div>
                                        )}
                                    </div>
                                </TableCell>

                                <TableCell>
                                    {canManageMembers ? (
                                        <Select
                                            onValueChange={(role) =>
                                                updateRoleMutation.mutate({
                                                    role: role as WorkspaceRole,
                                                    userId: workspaceUser.userId,
                                                    workspaceId: String(workspaceId),
                                                })
                                            }
                                            value={workspaceUser.workspaceRole ?? undefined}
                                        >
                                            <SelectTrigger className="w-32">
                                                <SelectValue />
                                            </SelectTrigger>

                                            <SelectContent>
                                                {WORKSPACE_ROLES.map((role) => (
                                                    <SelectItem key={role} value={role}>
                                                        {role}
                                                    </SelectItem>
                                                ))}
                                            </SelectContent>
                                        </Select>
                                    ) : (
                                        workspaceUser.workspaceRole
                                    )}
                                </TableCell>

                                <TableCell>
                                    {workspaceUser.createdDate
                                        ? new Date(workspaceUser.createdDate).toLocaleDateString()
                                        : '-'}
                                </TableCell>

                                {canManageMembers && (
                                    <TableCell>
                                        <button
                                            className="text-destructive hover:text-destructive/80"
                                            onClick={() =>
                                                removeUserMutation.mutate({
                                                    userId: workspaceUser.userId,
                                                    workspaceId: String(workspaceId),
                                                })
                                            }
                                        >
                                            <Trash2Icon className="size-4" />
                                        </button>
                                    </TableCell>
                                )}
                            </TableRow>
                        ))}

                        {addingUser && (
                            <TableRow>
                                <TableCell>
                                    <Popover onOpenChange={setUserSearchOpen} open={userSearchOpen}>
                                        <PopoverTrigger asChild>
                                            <button className="w-full rounded-md border px-3 py-2 text-left text-sm">
                                                {selectedUserId ? selectedUserLabel : 'Search users...'}
                                            </button>
                                        </PopoverTrigger>

                                        <PopoverContent align="start" className="w-64 p-0">
                                            <Command>
                                                <CommandInput placeholder="Search by email..." />

                                                <CommandList>
                                                    <CommandEmpty>No users found.</CommandEmpty>

                                                    <CommandGroup>
                                                        {availableUsers.map((user) => (
                                                            <CommandItem
                                                                key={user.id}
                                                                onSelect={() => {
                                                                    setSelectedUserId(user.id);
                                                                    setUserSearchOpen(false);
                                                                }}
                                                                value={user.email}
                                                            >
                                                                <div>
                                                                    <div className="text-sm">{user.email}</div>

                                                                    {user.firstName && (
                                                                        <div className="text-xs text-muted-foreground">
                                                                            {user.firstName} {user.lastName}
                                                                        </div>
                                                                    )}
                                                                </div>
                                                            </CommandItem>
                                                        ))}
                                                    </CommandGroup>
                                                </CommandList>
                                            </Command>
                                        </PopoverContent>
                                    </Popover>
                                </TableCell>

                                <TableCell>
                                    <Select
                                        onValueChange={(role) => setSelectedRole(role as WorkspaceRole)}
                                        value={selectedRole}
                                    >
                                        <SelectTrigger className="w-32">
                                            <SelectValue />
                                        </SelectTrigger>

                                        <SelectContent>
                                            {WORKSPACE_ROLES.map((role) => (
                                                <SelectItem key={role} value={role}>
                                                    {role}
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                </TableCell>

                                <TableCell>
                                    <div className="flex gap-1">
                                        <button
                                            className="rounded p-1 hover:bg-accent disabled:opacity-50"
                                            disabled={!selectedUserId}
                                            onClick={handleAddConfirm}
                                        >
                                            <CheckIcon className="size-4" />
                                        </button>

                                        <button className="rounded p-1 hover:bg-accent" onClick={handleAddCancel}>
                                            <XIcon className="size-4" />
                                        </button>
                                    </div>
                                </TableCell>

                                {canManageMembers && <TableCell />}
                            </TableRow>
                        )}
                    </TableBody>
                </Table>

                {canManageMembers && !addingUser && (
                    <button
                        className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
                        onClick={() => setAddingUser(true)}
                    >
                        <PlusIcon className="size-4" />
                        Add user
                    </button>
                )}
            </DialogContent>
        </Dialog>
    );
};

export default WorkspaceUsersDialog;
```

- [ ] **Step 2: Commit**

```bash
git add client/src/ee/pages/settings/automation/workspaces/components/WorkspaceUsersDialog.tsx
git commit -m "client - Create WorkspaceUsersDialog with inline add user and role management"
```

---

### Task 8: Add Members button to WorkspaceListItem

**Files:**
- Modify: `client/src/ee/pages/settings/automation/workspaces/components/WorkspaceListItem.tsx`

- [ ] **Step 1: Add Members menu item and dialog state**

Add import at top:
```tsx
import WorkspaceUsersDialog from '@/ee/pages/settings/automation/workspaces/components/WorkspaceUsersDialog';
import {UsersIcon} from 'lucide-react';
```

Add state:
```tsx
const [showMembersDialog, setShowMembersDialog] = useState(false);
```

Add a "Members" item to the `DropdownMenuContent`, before the separator:
```tsx
<DropdownMenuItem onClick={() => setShowMembersDialog(true)}>
    Members
</DropdownMenuItem>
```

Add the dialog render after the existing `WorkspaceDialog`:
```tsx
{showMembersDialog && (
    <WorkspaceUsersDialog
        onClose={() => setShowMembersDialog(false)}
        open={showMembersDialog}
        workspaceId={workspace.id!}
    />
)}
```

- [ ] **Step 2: Run typecheck**

Run: `cd client && npm run typecheck`
Expected: No errors

- [ ] **Step 3: Run lint and format**

Run: `cd client && npm run format && npm run lint`
Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add client/src/ee/pages/settings/automation/workspaces/components/WorkspaceListItem.tsx
git commit -m "client - Add Members menu item to WorkspaceListItem"
```

---

### Task 9: Final verification

- [ ] **Step 1: Run server-side checks**

Run: `./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-graphql:compileJava`
Expected: BUILD SUCCESSFUL

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-graphql:testIntegration`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run client-side checks**

Run: `cd client && npm run check`
Expected: No errors

- [ ] **Step 3: Run spotless formatting**

Run: `./gradlew spotlessApply`

- [ ] **Step 4: Final commit if any formatting changes**

```bash
git add -A && git commit -m "Apply code formatting"
```
