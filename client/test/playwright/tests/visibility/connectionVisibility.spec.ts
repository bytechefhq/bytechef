import {type APIRequestContext, expect} from '@playwright/test';

import {workspaceRolesTest as test} from '../../fixtures/workspaceRoles';
import {type BuiltInRoleType, deleteRoleUser} from '../../utils/rbacUtils';
import {
    type RoleUserI,
    createConnection,
    createConnectionResponse,
    createOrganizationConnectionId,
    createUserApiContext,
    createWorkspace,
    createWorkspaceMember,
    deleteConnectionResponse,
    deleteOrganizationConnection,
    deleteWorkspace,
    expectGraphQlError,
    expectNoGraphQlErrors,
    fetchConnectionGrants,
    fetchUserId,
    getConnection,
    getConnectionResponse,
    grantConnectionAccess,
    listConnectionIds,
    revokeConnectionAccess,
    updateConnectionResponse,
} from '../../utils/visibilityUtils';

test.describe('Connection visibility', () => {
    const apis: Partial<Record<BuiltInRoleType, APIRequestContext>> = {};

    let outsider: RoleUserI;
    let outsiderApi: APIRequestContext;
    let otherWorkspaceId: number;

    test.beforeAll(async ({adminApi, workerRoleUsers}) => {
        for (const role of ['ADMIN', 'EDITOR', 'VIEWER'] as BuiltInRoleType[]) {
            apis[role] = await createUserApiContext(workerRoleUsers[role].email, workerRoleUsers[role].password);
        }

        otherWorkspaceId = (await createWorkspace(adminApi)).id;

        outsider = await createWorkspaceMember(adminApi, otherWorkspaceId, 'ADMIN');

        outsiderApi = await createUserApiContext(outsider.email, outsider.password);
    });

    test.afterAll(async ({adminApi}) => {
        for (const api of Object.values(apis)) {
            await api.dispose();
        }

        await outsiderApi.dispose();

        await deleteRoleUser(adminApi, outsider);

        await deleteWorkspace(adminApi, otherWorkspaceId);
    });

    test('a connection created without a visibility is WORKSPACE-visible and ACTIVE', async ({rbacWorkspace}) => {
        const connectionId = await createConnection(apis.EDITOR!, rbacWorkspace.id);

        const connection = await getConnection(apis.EDITOR!, connectionId);

        expect(connection).toMatchObject({status: 'ACTIVE', visibility: 'WORKSPACE'});
    });

    test('a workspace connection cannot be created with ORGANIZATION visibility', async ({rbacWorkspace}) => {
        const response = await createConnectionResponse(apis.ADMIN!, rbacWorkspace.id, {visibility: 'ORGANIZATION'});

        expect(response.ok(), await response.text()).toBeFalsy();

        expect(await response.text()).toContain(
            'Only PRIVATE or WORKSPACE visibility can be set on connection creation'
        );
    });

    test('a PRIVATE connection is visible only to its owner and the tenant admin', async ({
        adminApi,
        rbacWorkspace,
    }) => {
        const connectionId = await createConnection(apis.EDITOR!, rbacWorkspace.id, {visibility: 'PRIVATE'});

        expect((await getConnection(apis.EDITOR!, connectionId)).visibility).toBe('PRIVATE');

        expect(await listConnectionIds(apis.EDITOR!, rbacWorkspace.id)).toContain(connectionId);
        expect(await listConnectionIds(adminApi, rbacWorkspace.id)).toContain(connectionId);

        for (const role of ['ADMIN', 'VIEWER'] as BuiltInRoleType[]) {
            expect(await listConnectionIds(apis[role]!, rbacWorkspace.id), role).not.toContain(connectionId);

            expect((await getConnectionResponse(apis[role]!, connectionId)).status(), role).toBe(403);
        }

        expect((await updateConnectionResponse(apis.ADMIN!, connectionId, 'renamed')).status()).toBe(403);
        expect((await deleteConnectionResponse(apis.ADMIN!, connectionId)).status()).toBe(403);

        expect((await getConnectionResponse(adminApi, connectionId)).status()).toBe(200);
    });

    test('a WORKSPACE connection is visible to every member and to nobody outside the workspace', async ({
        rbacWorkspace,
    }) => {
        const connectionId = await createConnection(apis.EDITOR!, rbacWorkspace.id, {visibility: 'WORKSPACE'});

        for (const role of ['ADMIN', 'EDITOR', 'VIEWER'] as BuiltInRoleType[]) {
            expect(await listConnectionIds(apis[role]!, rbacWorkspace.id), role).toContain(connectionId);

            expect((await getConnectionResponse(apis[role]!, connectionId)).status(), role).toBe(200);
        }

        expect((await getConnectionResponse(outsiderApi, connectionId)).status()).toBe(403);
        expect(await listConnectionIds(outsiderApi, otherWorkspaceId)).not.toContain(connectionId);
    });

    test('a grant shares a PRIVATE connection with a named member and revoking it withdraws access', async ({
        adminApi,
        rbacWorkspace,
        workerRoleUsers,
    }) => {
        const connectionId = await createConnection(apis.EDITOR!, rbacWorkspace.id, {visibility: 'PRIVATE'});

        const viewerId = await fetchUserId(adminApi, workerRoleUsers.VIEWER.login);

        expectNoGraphQlErrors(await grantConnectionAccess(apis.EDITOR!, rbacWorkspace.id, connectionId, viewerId));

        expectNoGraphQlErrors(await grantConnectionAccess(apis.EDITOR!, rbacWorkspace.id, connectionId, viewerId));

        const grants = await fetchConnectionGrants(apis.EDITOR!, rbacWorkspace.id, connectionId);

        expectNoGraphQlErrors(grants);
        expect(grants.data!.connectionGrants.map(Number)).toEqual([Number(viewerId)]);

        expect(await listConnectionIds(apis.VIEWER!, rbacWorkspace.id)).toContain(connectionId);
        expect((await getConnectionResponse(apis.VIEWER!, connectionId)).status()).toBe(200);

        expect(await listConnectionIds(apis.ADMIN!, rbacWorkspace.id)).not.toContain(connectionId);

        expectNoGraphQlErrors(await revokeConnectionAccess(apis.EDITOR!, rbacWorkspace.id, connectionId, viewerId));

        expect(await listConnectionIds(apis.VIEWER!, rbacWorkspace.id)).not.toContain(connectionId);
        expect((await getConnectionResponse(apis.VIEWER!, connectionId)).status()).toBe(403);
    });

    test('a grant is refused for a user who is not a member of the owning workspace', async ({rbacWorkspace}) => {
        const connectionId = await createConnection(apis.EDITOR!, rbacWorkspace.id, {visibility: 'PRIVATE'});

        expectGraphQlError(
            await grantConnectionAccess(apis.EDITOR!, rbacWorkspace.id, connectionId, outsider.userId!),
            /is not a member of workspace/
        );

        expect((await getConnectionResponse(outsiderApi, connectionId)).status()).toBe(403);
    });

    test('an ORGANIZATION connection is listed in every workspace and readable by id, but not editable by a workspace admin', async ({
        adminApi,
        rbacWorkspace,
    }) => {
        const connectionId = await createOrganizationConnectionId(adminApi);

        try {
            for (const role of ['ADMIN', 'EDITOR', 'VIEWER'] as BuiltInRoleType[]) {
                expect(await listConnectionIds(apis[role]!, rbacWorkspace.id), role).toContain(connectionId);

                expect((await getConnectionResponse(apis[role]!, connectionId)).status(), role).toBe(200);
            }

            expect(await listConnectionIds(outsiderApi, otherWorkspaceId)).toContain(connectionId);
            expect((await getConnection(outsiderApi, connectionId)).visibility).toBe('ORGANIZATION');

            expect((await updateConnectionResponse(apis.ADMIN!, connectionId, 'renamed')).status()).toBe(403);
            expect((await deleteConnectionResponse(apis.ADMIN!, connectionId)).status()).toBe(403);
        } finally {
            await deleteOrganizationConnection(adminApi, connectionId);
        }
    });
});
