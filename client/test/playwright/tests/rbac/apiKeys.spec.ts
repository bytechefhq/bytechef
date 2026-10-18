import {type APIRequestContext, expect} from '@playwright/test';

import {workspaceRolesTest as test} from '../../fixtures/workspaceRoles';
import {
    RBAC_BASE_URL,
    type RoleUserI,
    createRoleUser,
    createRoleUserApiContext,
    createUserApiContext,
    deleteRoleUser,
    graphql,
    postGraphQl,
} from '../../utils/rbacUtils';

interface ApiKeyI {
    id: string;
    name: string;
}

const API_KEYS = 'query ApiKeys { apiKeys(environmentId: "0", type: AUTOMATION) { id name } }';

const ADMIN_API_KEYS = 'query AdminApiKeys { adminApiKeys(environmentId: "0") { id name } }';

const WORKSPACE_API_KEYS = `
    query WorkspaceApiKeys($workspaceId: ID!) {
        workspaceApiKeys(workspaceId: $workspaceId, environmentId: "0") { id name }
    }
`;

const CREATE_WORKSPACE_API_KEY = `
    mutation CreateWorkspaceApiKey($workspaceId: ID!, $name: String!) {
        createWorkspaceApiKey(workspaceId: $workspaceId, name: $name, environmentId: "0")
    }
`;

const WORKSPACE_USERS =
    'query WorkspaceUsers($workspaceId: ID!) { workspaceUsers(workspaceId: $workspaceId) { userId } }';

async function createWorkspaceApiKey(
    apiContext: APIRequestContext,
    workspaceId: number,
    name: string
): Promise<ApiKeyI> {
    await graphql(apiContext, CREATE_WORKSPACE_API_KEY, {name, workspaceId: String(workspaceId)});

    const {apiKeys} = await graphql<{apiKeys: ApiKeyI[]}>(apiContext, API_KEYS, {});

    const apiKey = apiKeys.find((curApiKey) => curApiKey.name === name);

    expect(apiKey, `${name} is not among the caller's own keys`).toBeTruthy();

    return apiKey!;
}

test.describe('API keys', () => {
    const cleanups: (() => Promise<void>)[] = [];

    test.afterEach(async () => {
        while (cleanups.length) {
            await cleanups.pop()!();
        }
    });

    test('a member sees and manages only their own keys', async ({adminApi, rbacWorkspace, roleUsers}) => {
        const owner = roleUsers.EDITOR;

        const otherEditor = await createRoleUser(adminApi, RBAC_BASE_URL, rbacWorkspace, 'EDITOR');

        cleanups.push(() => deleteRoleUser(adminApi, otherEditor));

        const ownerApi = await createRoleUserApiContext(RBAC_BASE_URL, owner);
        const otherEditorApi = await createRoleUserApiContext(RBAC_BASE_URL, otherEditor);

        cleanups.push(() => ownerApi.dispose());
        cleanups.push(() => otherEditorApi.dispose());

        const ownerKey = await createWorkspaceApiKey(ownerApi, rbacWorkspace.id, `rbac_owner_${Date.now()}`);

        await test.step('another member does not see it among their keys', async () => {
            const {apiKeys} = await graphql<{apiKeys: ApiKeyI[]}>(otherEditorApi, API_KEYS, {});

            expect(apiKeys.map((apiKey) => apiKey.id)).not.toContain(ownerKey.id);
        });

        await test.step('another member can neither read, rename nor delete it as a personal key', async () => {
            const readResponse = await postGraphQl(
                otherEditorApi,
                'query ApiKey($id: ID!) { apiKey(id: $id) { id } }',
                {
                    id: ownerKey.id,
                }
            );

            expect(readResponse.errors?.length, JSON.stringify(readResponse)).toBeGreaterThan(0);

            const renameResponse = await postGraphQl(
                otherEditorApi,
                'mutation UpdateApiKey($id: ID!) { updateApiKey(id: $id, name: "hijacked") }',
                {id: ownerKey.id}
            );

            expect(renameResponse.errors?.length, JSON.stringify(renameResponse)).toBeGreaterThan(0);

            const deleteResponse = await postGraphQl(
                otherEditorApi,
                'mutation DeleteApiKey($id: ID!) { deleteApiKey(id: $id) }',
                {
                    id: ownerKey.id,
                }
            );

            expect(deleteResponse.errors?.length, JSON.stringify(deleteResponse)).toBeGreaterThan(0);

            const workspaceRenameResponse = await postGraphQl(
                otherEditorApi,
                'mutation UpdateWorkspaceApiKey($apiKeyId: ID!) { updateWorkspaceApiKey(apiKeyId: $apiKeyId, name: "hijacked") }',
                {apiKeyId: ownerKey.id}
            );

            expect(workspaceRenameResponse.errors?.length, JSON.stringify(workspaceRenameResponse)).toBeGreaterThan(0);
        });

        await test.step('the owner may rename and delete it', async () => {
            const renameResponse = await postGraphQl(
                ownerApi,
                'mutation UpdateApiKey($id: ID!, $name: String!) { updateApiKey(id: $id, name: $name) }',
                {id: ownerKey.id, name: `${ownerKey.name}_renamed`}
            );

            expect(renameResponse.errors, JSON.stringify(renameResponse.errors)).toBeUndefined();

            const {apiKeys} = await graphql<{apiKeys: ApiKeyI[]}>(ownerApi, API_KEYS, {});

            expect(apiKeys).toContainEqual({id: ownerKey.id, name: `${ownerKey.name}_renamed`});

            const deleteResponse = await postGraphQl(
                ownerApi,
                'mutation DeleteWorkspaceApiKey($apiKeyId: ID!) { deleteWorkspaceApiKey(apiKeyId: $apiKeyId) }',
                {apiKeyId: ownerKey.id}
            );

            expect(deleteResponse.errors, JSON.stringify(deleteResponse.errors)).toBeUndefined();
        });
    });

    test('platform API keys are issued and listed only by a tenant admin', async ({adminApi, roleUsers}) => {
        const workspaceAdminApi = await createRoleUserApiContext(RBAC_BASE_URL, roleUsers.ADMIN);

        cleanups.push(() => workspaceAdminApi.dispose());

        const name = `rbac_platform_${Date.now()}`;

        await test.step('a workspace admin is refused', async () => {
            for (const query of [
                `mutation CreateApiKey { createApiKey(name: "${name}", environmentId: "0") }`,
                `mutation CreateApiKey { createApiKey(name: "${name}", environmentId: "0", type: EMBEDDED) }`,
                ADMIN_API_KEYS,
            ]) {
                const response = await postGraphQl(workspaceAdminApi, query, {});

                expect(response.errors?.[0]?.extensions, JSON.stringify(response)).toMatchObject({
                    classification: 'FORBIDDEN',
                });
            }
        });

        await test.step('the tenant admin may issue one, which no workspace member sees', async () => {
            const createResponse = await postGraphQl<{createApiKey: string}>(
                adminApi,
                `mutation CreateApiKey { createApiKey(name: "${name}", environmentId: "0") }`,
                {}
            );

            expect(createResponse.errors, JSON.stringify(createResponse.errors)).toBeUndefined();
            expect(createResponse.data?.createApiKey).toBeTruthy();

            const {adminApiKeys} = await graphql<{adminApiKeys: ApiKeyI[]}>(adminApi, ADMIN_API_KEYS, {});

            const platformKey = adminApiKeys.find((apiKey) => apiKey.name === name)!;

            expect(platformKey).toBeTruthy();

            cleanups.push(async () => {
                await postGraphQl(adminApi, 'mutation DeleteApiKey($id: ID!) { deleteApiKey(id: $id) }', {
                    id: platformKey.id,
                });
            });

            const {apiKeys} = await graphql<{apiKeys: ApiKeyI[]}>(workspaceAdminApi, API_KEYS, {});

            expect(apiKeys.map((apiKey) => apiKey.id)).not.toContain(platformKey.id);

            const deleteResponse = await postGraphQl(
                workspaceAdminApi,
                'mutation DeleteApiKey($id: ID!) { deleteApiKey(id: $id) }',
                {id: platformKey.id}
            );

            expect(deleteResponse.errors?.length, JSON.stringify(deleteResponse)).toBeGreaterThan(0);
        });
    });
});

async function deleteUser(adminApi: APIRequestContext, roleUser: RoleUserI) {
    return postGraphQl(adminApi, 'mutation DeleteUser($login: String!) { deleteUser(login: $login) }', {
        login: roleUser.login,
    });
}

async function fetchWorkspaceUserIds(adminApi: APIRequestContext, workspaceId: number): Promise<string[]> {
    const {workspaceUsers} = await graphql<{workspaceUsers: {userId: string}[]}>(adminApi, WORKSPACE_USERS, {
        workspaceId: String(workspaceId),
    });

    return workspaceUsers.map((workspaceUser) => workspaceUser.userId);
}

async function fetchWorkspaceApiKeyIds(adminApi: APIRequestContext, workspaceId: number): Promise<string[]> {
    const {workspaceApiKeys} = await graphql<{workspaceApiKeys: ApiKeyI[]}>(adminApi, WORKSPACE_API_KEYS, {
        workspaceId: String(workspaceId),
    });

    return workspaceApiKeys.map((apiKey) => apiKey.id);
}

test.describe('User deletion', () => {
    test('removes the memberships of the deleted user and ends their sign-in', async ({adminApi, rbacWorkspace}) => {
        const departingUser = await createRoleUser(adminApi, RBAC_BASE_URL, rbacWorkspace, 'EDITOR');

        expect(await fetchWorkspaceUserIds(adminApi, rbacWorkspace.id)).toContain(departingUser.userId);

        const deleteResponse = await deleteUser(adminApi, departingUser);

        expect(deleteResponse.errors, JSON.stringify(deleteResponse.errors)).toBeUndefined();

        expect(await fetchWorkspaceUserIds(adminApi, rbacWorkspace.id)).not.toContain(departingUser.userId);

        await expect(
            createUserApiContext(RBAC_BASE_URL, departingUser.email, departingUser.password)
        ).rejects.toThrow();
    });

    test('revokes the workspace API keys of the deleted user', async ({adminApi, rbacWorkspace}) => {
        const departingUser = await createRoleUser(adminApi, RBAC_BASE_URL, rbacWorkspace, 'EDITOR');

        const departingApi = await createRoleUserApiContext(RBAC_BASE_URL, departingUser);

        const apiKey = await createWorkspaceApiKey(departingApi, rbacWorkspace.id, `rbac_departing_${Date.now()}`);

        await departingApi.dispose();

        try {
            expect(await fetchWorkspaceApiKeyIds(adminApi, rbacWorkspace.id)).toContain(apiKey.id);

            const deleteResponse = await deleteUser(adminApi, departingUser);

            expect(deleteResponse.errors, JSON.stringify(deleteResponse.errors)).toBeUndefined();

            expect(await fetchWorkspaceApiKeyIds(adminApi, rbacWorkspace.id)).not.toContain(apiKey.id);
            expect(await fetchWorkspaceUserIds(adminApi, rbacWorkspace.id)).not.toContain(departingUser.userId);
        } finally {
            await postGraphQl(
                adminApi,
                'mutation DeleteWorkspaceApiKey($apiKeyId: ID!) { deleteWorkspaceApiKey(apiKeyId: $apiKeyId) }',
                {apiKeyId: apiKey.id}
            );

            await deleteRoleUser(adminApi, departingUser);
        }
    });
});
