/* eslint-disable react-hooks/rules-of-hooks, no-empty-pattern -- Playwright fixtures use 'use' callback, not React hooks */
import {APIRequestContext, test as base} from '@playwright/test';

import {
    type BuiltInRoleType,
    type RbacWorkspaceI,
    type RoleUserI,
    createAdminApiContext,
    createRbacWorkspace,
    createRoleUser,
    deleteRbacWorkspace,
    deleteRoleUser,
} from '../utils/rbacUtils';

export type WorkspaceRolesFixturesType = {
    roleUsers: Record<BuiltInRoleType, RoleUserI>;
};

export type WorkspaceRolesWorkerFixturesType = {
    adminApi: APIRequestContext;
    rbacWorkspace: RbacWorkspaceI;
    workerRoleUsers: Record<BuiltInRoleType, RoleUserI>;
};

export const workspaceRolesTest = base.extend<WorkspaceRolesFixturesType, WorkspaceRolesWorkerFixturesType>({
    adminApi: [
        async ({}, use, workerInfo) => {
            const baseURL = workerInfo.project.use.baseURL || 'http://127.0.0.1:5173';

            const adminApi = await createAdminApiContext(baseURL);

            await use(adminApi);

            await adminApi.dispose();
        },
        {scope: 'worker'},
    ],
    rbacWorkspace: [
        async ({adminApi}, use) => {
            const workspace = await createRbacWorkspace(adminApi);

            await use(workspace);

            await deleteRbacWorkspace(adminApi, workspace);
        },
        {scope: 'worker'},
    ],
    roleUsers: async ({workerRoleUsers}, use) => {
        await use(workerRoleUsers);
    },
    workerRoleUsers: [
        async ({adminApi, rbacWorkspace}, use, workerInfo) => {
            const baseURL = workerInfo.project.use.baseURL || 'http://127.0.0.1:5173';

            const roleUsers = {
                ADMIN: await createRoleUser(adminApi, baseURL, rbacWorkspace, 'ADMIN'),
                EDITOR: await createRoleUser(adminApi, baseURL, rbacWorkspace, 'EDITOR'),
                VIEWER: await createRoleUser(adminApi, baseURL, rbacWorkspace, 'VIEWER'),
            };

            await use(roleUsers);

            for (const roleUser of Object.values(roleUsers)) {
                await deleteRoleUser(adminApi, roleUser);
            }
        },
        {scope: 'worker'},
    ],
});
