/* eslint-disable no-empty-pattern -- Playwright fixtures use 'use' callback, not React hooks */
import {APIRequestContext, test as base} from '@playwright/test';

import {
    type EmbeddedDataI,
    createEmbeddedData,
    deleteEmbeddedData,
    removeEmbeddedUser,
    setEmbeddedOnlyEnvironmentRole,
    setEmbeddedRole,
} from '../utils/embeddedRbacUtils';
import {type RoleUserI, createAccount, createAdminApiContext, deleteRoleUser} from '../utils/rbacUtils';

export type EmbeddedRoleUserKeyType =
    'ADMIN' | 'DEVELOPMENT_EDITOR' | 'EDITOR' | 'OUTSIDER' | 'PRODUCTION_EDITOR' | 'VIEWER';

export type EmbeddedRoleUsersType = Record<EmbeddedRoleUserKeyType, RoleUserI>;

export type EmbeddedRolesWorkerFixturesType = {
    adminApi: APIRequestContext;
    embeddedData: EmbeddedDataI;
    embeddedRoleUsers: EmbeddedRoleUsersType;
};

function getBaseUrl(baseURL: string | undefined): string {
    return baseURL || 'http://127.0.0.1:5173';
}

export const embeddedRolesTest = base.extend<object, EmbeddedRolesWorkerFixturesType>({
    adminApi: [
        async ({}, use, workerInfo) => {
            const adminApi = await createAdminApiContext(getBaseUrl(workerInfo.project.use.baseURL));

            await use(adminApi);

            await adminApi.dispose();
        },
        {scope: 'worker'},
    ],
    embeddedData: [
        async ({adminApi}, use, workerInfo) => {
            const embeddedData = await createEmbeddedData(adminApi, getBaseUrl(workerInfo.project.use.baseURL));

            await use(embeddedData);

            await deleteEmbeddedData(adminApi, embeddedData);
        },
        {scope: 'worker'},
    ],
    embeddedRoleUsers: [
        async ({adminApi}, use, workerInfo) => {
            const baseURL = getBaseUrl(workerInfo.project.use.baseURL);

            const createdRoleUsers: RoleUserI[] = [];

            const createRoleUser = async (): Promise<RoleUserI> => {
                const roleUser = await createAccount(adminApi, baseURL);

                createdRoleUsers.push(roleUser);

                return roleUser;
            };

            const cleanUp = async () => {
                for (const roleUser of createdRoleUsers) {
                    await removeEmbeddedUser(adminApi, roleUser.userId!).catch(() => undefined);

                    await deleteRoleUser(adminApi, roleUser).catch((error: unknown) => {
                        console.warn(`embeddedRoleUsers teardown: deleting ${roleUser.login} failed: ${error}`);
                    });
                }
            };

            let embeddedRoleUsers: EmbeddedRoleUsersType;

            try {
                embeddedRoleUsers = {
                    ADMIN: await createRoleUser(),
                    DEVELOPMENT_EDITOR: await createRoleUser(),
                    EDITOR: await createRoleUser(),
                    OUTSIDER: await createRoleUser(),
                    PRODUCTION_EDITOR: await createRoleUser(),
                    VIEWER: await createRoleUser(),
                };

                await setEmbeddedRole(adminApi, embeddedRoleUsers.ADMIN.userId!, 'ADMIN');
                await setEmbeddedRole(adminApi, embeddedRoleUsers.EDITOR.userId!, 'EDITOR');
                await setEmbeddedRole(adminApi, embeddedRoleUsers.VIEWER.userId!, 'VIEWER');

                await setEmbeddedOnlyEnvironmentRole(
                    adminApi,
                    embeddedRoleUsers.PRODUCTION_EDITOR.userId!,
                    'PRODUCTION',
                    'EDITOR'
                );
                await setEmbeddedOnlyEnvironmentRole(
                    adminApi,
                    embeddedRoleUsers.DEVELOPMENT_EDITOR.userId!,
                    'DEVELOPMENT',
                    'EDITOR'
                );
            } catch (error) {
                await cleanUp();

                throw error;
            }

            await use(embeddedRoleUsers);

            await cleanUp();
        },
        {scope: 'worker'},
    ],
});
