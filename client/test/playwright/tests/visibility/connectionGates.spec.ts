import {type APIRequestContext, expect} from '@playwright/test';

import {workspaceRolesTest as test} from '../../fixtures/workspaceRoles';
import {type BuiltInRoleType, createCustomRole, deleteCustomRole, deleteRoleUser} from '../../utils/rbacUtils';
import {
    type RoleUserI,
    createConnection,
    createCustomRoleMember,
    createUserApiContext,
    createWorkspace,
    createWorkspaceMember,
    deleteConnectionResponse,
    deleteWorkspace,
    deployHttpClientProject,
    expectAccessDenied,
    expectGraphQlError,
    expectNoGraphQlErrors,
    fetchConnectionGrants,
    fetchUserId,
    getConnection,
    getConnectionResponse,
    grantConnectionAccess,
    revokeConnectionAccess,
    setConnectionVisibility,
    setWorkspaceRole,
    updateConnectionResponse,
} from '../../utils/visibilityUtils';

test.describe('Connection permission gates', () => {
    const apis: Partial<Record<BuiltInRoleType, APIRequestContext>> = {};

    let viewerId: string;

    test.beforeAll(async ({adminApi, workerRoleUsers}) => {
        for (const role of ['ADMIN', 'EDITOR', 'VIEWER'] as BuiltInRoleType[]) {
            apis[role] = await createUserApiContext(workerRoleUsers[role].email, workerRoleUsers[role].password);
        }

        viewerId = await fetchUserId(adminApi, workerRoleUsers.VIEWER.login);
    });

    test.afterAll(async () => {
        for (const api of Object.values(apis)) {
            await api.dispose();
        }
    });

    for (const [ownerRole, allowed] of [
        ['VIEWER', false],
        ['EDITOR', true],
        ['ADMIN', true],
    ] as [BuiltInRoleType, boolean][]) {
        test(`an owner who is a workspace ${ownerRole} ${allowed ? 'renames and deletes' : 'can neither rename nor delete'} their connection`, async ({
            adminApi,
            rbacWorkspace,
        }) => {
            const owner = await createWorkspaceMember(adminApi, rbacWorkspace.id, 'EDITOR');
            const ownerApi = await createUserApiContext(owner.email, owner.password);

            try {
                const connectionId = await createConnection(ownerApi, rbacWorkspace.id);

                await setWorkspaceRole(adminApi, rbacWorkspace.id, owner, ownerRole);

                const renameResponse = await updateConnectionResponse(ownerApi, connectionId, 'vis_renamed_by_owner');

                expect(renameResponse.status(), await renameResponse.text()).toBe(allowed ? 204 : 403);

                const deleteResponse = await deleteConnectionResponse(ownerApi, connectionId);

                if (allowed) {
                    expect(deleteResponse.status(), await deleteResponse.text()).toBeLessThan(300);
                } else {
                    expect(deleteResponse.status(), await deleteResponse.text()).toBe(403);
                    expect((await getConnection(adminApi, connectionId)).name).not.toBe('vis_renamed_by_owner');
                }
            } finally {
                await ownerApi.dispose();

                await deleteRoleUser(adminApi, owner);
            }
        });
    }

    test('a custom-role owner holding CONNECTION_EDIT and CONNECTION_DELETE renames and deletes their connection', async ({
        adminApi,
        rbacWorkspace,
    }) => {
        const customRole = await createCustomRole(adminApi, [
            'CONNECTION_CREATE',
            'CONNECTION_DELETE',
            'CONNECTION_EDIT',
            'CONNECTION_VIEW',
        ]);

        let member: RoleUserI | undefined;
        let memberApi: APIRequestContext | undefined;

        try {
            member = await createCustomRoleMember(adminApi, rbacWorkspace.id, customRole);

            memberApi = await createUserApiContext(member.email, member.password);

            const connectionId = await createConnection(memberApi, rbacWorkspace.id);

            const renameResponse = await updateConnectionResponse(memberApi, connectionId, 'vis_custom_owner_rename');

            expect(renameResponse.status(), await renameResponse.text()).toBe(204);

            const deleteResponse = await deleteConnectionResponse(memberApi, connectionId);

            expect(deleteResponse.status(), await deleteResponse.text()).toBeLessThan(300);
        } finally {
            await memberApi?.dispose();

            if (member) {
                await deleteRoleUser(adminApi, member);
            }

            await deleteCustomRole(adminApi, customRole);
        }
    });

    test('a member who is neither the owner nor a tenant admin cannot rename or delete it', async ({
        adminApi,
        rbacWorkspace,
    }) => {
        const connectionId = await createConnection(adminApi, rbacWorkspace.id);

        for (const role of ['ADMIN', 'EDITOR', 'VIEWER'] as BuiltInRoleType[]) {
            expect((await getConnectionResponse(apis[role]!, connectionId)).status()).toBe(200);

            expect((await updateConnectionResponse(apis[role]!, connectionId, `vis_hijacked_${role}`)).status()).toBe(
                403
            );
            expect((await deleteConnectionResponse(apis[role]!, connectionId)).status()).toBe(403);
        }

        expect((await getConnection(adminApi, connectionId)).name).not.toMatch(/vis_hijacked/);
    });

    test("a tenant admin renames and deletes a colleague's WORKSPACE connection", async ({adminApi, rbacWorkspace}) => {
        const connectionId = await createConnection(apis.EDITOR!, rbacWorkspace.id);

        const renameResponse = await updateConnectionResponse(adminApi, connectionId, 'vis_renamed_by_tenant_admin');

        expect(renameResponse.status(), await renameResponse.text()).toBe(204);

        const deleteResponse = await deleteConnectionResponse(adminApi, connectionId);

        expect(deleteResponse.status(), await deleteResponse.text()).toBeLessThan(300);
    });

    test('the owner changes visibility and manages grants', async ({rbacWorkspace}) => {
        const connectionId = await createConnection(apis.EDITOR!, rbacWorkspace.id);

        expectNoGraphQlErrors(await setConnectionVisibility(apis.EDITOR!, rbacWorkspace.id, connectionId, 'PRIVATE'));
        expect((await getConnection(apis.EDITOR!, connectionId)).visibility).toBe('PRIVATE');

        expectNoGraphQlErrors(await grantConnectionAccess(apis.EDITOR!, rbacWorkspace.id, connectionId, viewerId));
        expectNoGraphQlErrors(await fetchConnectionGrants(apis.EDITOR!, rbacWorkspace.id, connectionId));
        expectNoGraphQlErrors(await revokeConnectionAccess(apis.EDITOR!, rbacWorkspace.id, connectionId, viewerId));

        expectNoGraphQlErrors(await setConnectionVisibility(apis.EDITOR!, rbacWorkspace.id, connectionId, 'WORKSPACE'));
        expect((await getConnection(apis.EDITOR!, connectionId)).visibility).toBe('WORKSPACE');
    });

    test('a member who does not own the connection and is not a workspace admin cannot change its sharing', async ({
        rbacWorkspace,
    }) => {
        const connectionId = await createConnection(apis.ADMIN!, rbacWorkspace.id);

        for (const role of ['EDITOR', 'VIEWER'] as BuiltInRoleType[]) {
            expectAccessDenied(await setConnectionVisibility(apis[role]!, rbacWorkspace.id, connectionId, 'PRIVATE'));
            expectAccessDenied(await grantConnectionAccess(apis[role]!, rbacWorkspace.id, connectionId, viewerId));
            expectAccessDenied(await revokeConnectionAccess(apis[role]!, rbacWorkspace.id, connectionId, viewerId));
            expectAccessDenied(await fetchConnectionGrants(apis[role]!, rbacWorkspace.id, connectionId));
        }

        expect((await getConnection(apis.ADMIN!, connectionId)).visibility).toBe('WORKSPACE');
    });

    test("a workspace admin changes the sharing of a colleague's connection", async ({rbacWorkspace}) => {
        const connectionId = await createConnection(apis.EDITOR!, rbacWorkspace.id);

        expectNoGraphQlErrors(await setConnectionVisibility(apis.ADMIN!, rbacWorkspace.id, connectionId, 'PRIVATE'));
        expectNoGraphQlErrors(await grantConnectionAccess(apis.ADMIN!, rbacWorkspace.id, connectionId, viewerId));
        expectNoGraphQlErrors(await fetchConnectionGrants(apis.ADMIN!, rbacWorkspace.id, connectionId));

        expect((await getConnectionResponse(apis.VIEWER!, connectionId)).status()).toBe(200);
    });

    test('ORGANIZATION visibility cannot be set through setConnectionVisibility', async ({adminApi, rbacWorkspace}) => {
        const connectionId = await createConnection(apis.EDITOR!, rbacWorkspace.id);

        for (const api of [apis.EDITOR!, adminApi]) {
            expectGraphQlError(
                await setConnectionVisibility(api, rbacWorkspace.id, connectionId, 'ORGANIZATION'),
                /ORGANIZATION visibility is set through the organization connection facade/
            );
        }

        expect((await getConnection(apis.EDITOR!, connectionId)).visibility).toBe('WORKSPACE');
    });

    test('narrowing a connection to PRIVATE is refused while a deployment references it', async ({
        adminApi,
        rbacWorkspace,
    }) => {
        const connectionId = await createConnection(apis.EDITOR!, rbacWorkspace.id);

        await deployHttpClientProject(adminApi, rbacWorkspace.id, connectionId);

        expectGraphQlError(
            await setConnectionVisibility(apis.EDITOR!, rbacWorkspace.id, connectionId, 'PRIVATE'),
            /is used by a deployment/
        );

        expect((await getConnection(apis.EDITOR!, connectionId)).visibility).toBe('WORKSPACE');
    });

    test('sharing changes are refused when the named workspace does not own the connection', async ({
        adminApi,
        rbacWorkspace,
    }) => {
        const connectionId = await createConnection(apis.EDITOR!, rbacWorkspace.id);

        const otherWorkspace = await createWorkspace(adminApi);

        try {
            expectGraphQlError(
                await setConnectionVisibility(adminApi, otherWorkspace.id, connectionId, 'PRIVATE'),
                /does not belong to workspace/
            );
            expectGraphQlError(
                await grantConnectionAccess(adminApi, otherWorkspace.id, connectionId, viewerId),
                /does not belong to workspace/
            );
        } finally {
            await deleteWorkspace(adminApi, otherWorkspace.id);
        }
    });

    test('a custom-role member holding every connection scope cannot change the sharing, rename or delete a colleague connection', async ({
        adminApi,
        rbacWorkspace,
    }) => {
        const customRole = await createCustomRole(adminApi, ['CONNECTION_VIEW', 'CONNECTION_CREATE']);

        let member: RoleUserI | undefined;
        let memberApi: APIRequestContext | undefined;

        try {
            member = await createCustomRoleMember(adminApi, rbacWorkspace.id, customRole);

            memberApi = await createUserApiContext(member.email, member.password);

            const connectionId = await createConnection(apis.EDITOR!, rbacWorkspace.id);

            expectAccessDenied(await setConnectionVisibility(memberApi, rbacWorkspace.id, connectionId, 'PRIVATE'));

            expect((await updateConnectionResponse(memberApi, connectionId, 'vis_custom_role_rename')).status()).toBe(
                403
            );
            expect((await deleteConnectionResponse(memberApi, connectionId)).status()).toBe(403);
        } finally {
            await memberApi?.dispose();

            if (member) {
                await deleteRoleUser(adminApi, member);
            }

            await deleteCustomRole(adminApi, customRole);
        }
    });
});
