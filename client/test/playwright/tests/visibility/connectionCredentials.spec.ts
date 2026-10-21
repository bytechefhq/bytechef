import {type APIRequestContext, type Page, expect} from '@playwright/test';

import {workspaceRolesTest as test} from '../../fixtures/workspaceRoles';
import {
    type BuiltInRoleType,
    RBAC_BASE_URL,
    deleteRoleUser,
    openAsRoleUser,
    openAsTenantAdmin,
} from '../../utils/rbacUtils';
import {
    connectionName,
    createAccount,
    createConnection,
    createOrganizationConnectionId,
    createUserApiContext,
    createWorkspaceMember,
    deleteOrganizationConnection,
    deployHttpClientProject,
    expectAccessDenied,
    expectNoGraphQlErrors,
    getConnectionResponse,
    listDeploymentJobs,
    reassignConnection,
    runDeployedWorkflowResponse,
    setWorkspaceRole,
    updateConnectionCredentials,
} from '../../utils/visibilityUtils';

const CONNECTIONS_PATH = '/automation/connections';

async function connectionVersion(apiContext: APIRequestContext, connectionId: number): Promise<number> {
    const response = await getConnectionResponse(apiContext, connectionId);

    expect(response.ok(), await response.text()).toBeTruthy();

    return (await response.json()).__version;
}

function connectionRow(page: Page, name: string) {
    return page.locator('main ul > li').filter({hasText: name});
}

async function openEditDialog(page: Page, name: string) {
    await page.goto(CONNECTIONS_PATH);

    const row = connectionRow(page, name);

    await expect(row).toBeVisible({timeout: 30000});

    await row.getByRole('button', {name: 'Connection actions'}).click();

    await page.getByRole('menuitem', {name: 'Edit'}).click();

    return page.getByRole('dialog');
}

test.describe('Connection credential replacement', () => {
    const apis: Partial<Record<BuiltInRoleType, APIRequestContext>> = {};

    test.beforeAll(async ({workerRoleUsers}) => {
        for (const role of ['ADMIN', 'EDITOR', 'VIEWER'] as BuiltInRoleType[]) {
            apis[role] = await createUserApiContext(workerRoleUsers[role].email, workerRoleUsers[role].password);
        }
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
        test(`an owner who is a workspace ${ownerRole} ${allowed ? 'replaces' : 'cannot replace'} the credentials`, async ({
            adminApi,
            rbacWorkspace,
        }) => {
            const owner = await createWorkspaceMember(adminApi, rbacWorkspace.id, 'EDITOR');
            const ownerApi = await createUserApiContext(owner.email, owner.password);

            try {
                const connectionId = await createConnection(ownerApi, rbacWorkspace.id);

                await setWorkspaceRole(adminApi, rbacWorkspace.id, owner, ownerRole);

                const version = await connectionVersion(adminApi, connectionId);
                const response = await updateConnectionCredentials(ownerApi, connectionId, {}, version);

                if (allowed) {
                    expectNoGraphQlErrors(response);
                } else {
                    expectAccessDenied(response);
                    expect(await connectionVersion(adminApi, connectionId)).toBe(version);
                }
            } finally {
                await ownerApi.dispose();

                await deleteRoleUser(adminApi, owner);
            }
        });
    }

    test("a tenant admin cannot replace the credentials of a colleague's connection", async ({
        adminApi,
        rbacWorkspace,
    }) => {
        const connectionId = await createConnection(apis.EDITOR!, rbacWorkspace.id);

        const version = await connectionVersion(adminApi, connectionId);

        expectAccessDenied(await updateConnectionCredentials(adminApi, connectionId, {}, version));
        expect(await connectionVersion(adminApi, connectionId)).toBe(version);
    });

    test("a second tenant admin replaces the credentials of another admin's organization connection", async ({
        adminApi,
    }) => {
        const secondAdmin = await createAccount(adminApi, 'ROLE_ADMIN');
        const secondAdminApi = await createUserApiContext(secondAdmin.email, secondAdmin.password);
        const connectionId = await createOrganizationConnectionId(adminApi);

        try {
            const version = await connectionVersion(adminApi, connectionId);

            expectAccessDenied(await updateConnectionCredentials(apis.ADMIN!, connectionId, {}, version));
            expectNoGraphQlErrors(await updateConnectionCredentials(secondAdminApi, connectionId, {}, version));
        } finally {
            await secondAdminApi.dispose();

            await deleteOrganizationConnection(adminApi, connectionId);

            await deleteRoleUser(adminApi, secondAdmin);
        }
    });

    test('the member a connection is reassigned to replaces its credentials and the previous owner cannot', async ({
        adminApi,
        rbacWorkspace,
        workerRoleUsers,
    }) => {
        const previousOwner = await createWorkspaceMember(adminApi, rbacWorkspace.id, 'EDITOR');
        const previousOwnerApi = await createUserApiContext(previousOwner.email, previousOwner.password);

        try {
            const connectionId = await createConnection(previousOwnerApi, rbacWorkspace.id);

            expectNoGraphQlErrors(
                await reassignConnection(adminApi, rbacWorkspace.id, connectionId, workerRoleUsers.EDITOR.login)
            );

            const version = await connectionVersion(adminApi, connectionId);

            expectAccessDenied(await updateConnectionCredentials(previousOwnerApi, connectionId, {}, version));
            expectNoGraphQlErrors(await updateConnectionCredentials(apis.EDITOR!, connectionId, {}, version));
        } finally {
            await previousOwnerApi.dispose();

            await deleteRoleUser(adminApi, previousOwner);
        }
    });

    test('a member who is neither the owner nor a tenant admin cannot replace the credentials', async ({
        adminApi,
        rbacWorkspace,
    }) => {
        const connectionId = await createConnection(adminApi, rbacWorkspace.id);

        const version = await connectionVersion(adminApi, connectionId);

        for (const role of ['ADMIN', 'EDITOR', 'VIEWER'] as BuiltInRoleType[]) {
            expectAccessDenied(await updateConnectionCredentials(apis[role]!, connectionId, {}, version));
        }

        expect(await connectionVersion(adminApi, connectionId)).toBe(version);
    });

    test('a stale version is refused', async ({adminApi, rbacWorkspace}) => {
        const connectionId = await createConnection(apis.EDITOR!, rbacWorkspace.id);

        const version = await connectionVersion(adminApi, connectionId);

        const response = await updateConnectionCredentials(apis.EDITOR!, connectionId, {}, version + 5);

        expect(JSON.stringify(response.errors)).toMatch(/modified by someone else/);
    });

    test('a deployed workflow still runs on the connection after its credentials were replaced', async ({
        adminApi,
        rbacWorkspace,
    }) => {
        const connectionId = await createConnection(apis.EDITOR!, rbacWorkspace.id);

        const deployment = await deployHttpClientProject(apis.EDITOR!, rbacWorkspace.id, connectionId);

        const version = await connectionVersion(adminApi, connectionId);

        expectNoGraphQlErrors(await updateConnectionCredentials(apis.EDITOR!, connectionId, {}, version));

        const runResponse = await runDeployedWorkflowResponse(apis.EDITOR!, deployment);

        expect(runResponse.ok(), await runResponse.text()).toBeTruthy();

        await expect(async () => {
            const jobs = await listDeploymentJobs(adminApi, rbacWorkspace.id, deployment);

            expect(jobs.map((job) => job.status)).toContain('COMPLETED');
        }).toPass({timeout: 60000});
    });

    test('an EDITOR owner is offered Update credentials; a tenant admin sees them hidden; a VIEWER owner and non-owners get no Edit', async ({
        adminApi,
        browser,
        rbacWorkspace,
        workerRoleUsers,
    }) => {
        const owner = await createWorkspaceMember(adminApi, rbacWorkspace.id, 'EDITOR');
        const ownerApi = await createUserApiContext(owner.email, owner.password);
        const name = connectionName('vis_ui_credentials');

        try {
            await createConnection(ownerApi, rbacWorkspace.id, {name});

            const editorOwner = await openAsRoleUser(browser, RBAC_BASE_URL, owner, rbacWorkspace);

            try {
                const dialog = await openEditDialog(editorOwner.page, name);

                await expect(dialog.getByRole('button', {name: 'Update credentials'})).toBeVisible();
            } finally {
                await editorOwner.context.close();
            }

            await setWorkspaceRole(adminApi, rbacWorkspace.id, owner, 'VIEWER');

            const viewerOwner = await openAsRoleUser(browser, RBAC_BASE_URL, owner, rbacWorkspace);

            try {
                await viewerOwner.page.goto(CONNECTIONS_PATH);

                const row = connectionRow(viewerOwner.page, name);

                await expect(row).toBeVisible({timeout: 30000});
                await expect(row.getByRole('button', {name: 'Connection actions'})).toHaveCount(0);
            } finally {
                await viewerOwner.context.close();
            }

            const tenantAdmin = await openAsTenantAdmin(browser, RBAC_BASE_URL, rbacWorkspace);

            try {
                const dialog = await openEditDialog(tenantAdmin.page, name);

                await expect(dialog.getByText('Hidden').first()).toBeVisible();
                await expect(dialog.getByRole('button', {name: 'Update credentials'})).toHaveCount(0);
            } finally {
                await tenantAdmin.context.close();
            }

            for (const roleUser of [workerRoleUsers.ADMIN, workerRoleUsers.EDITOR, workerRoleUsers.VIEWER]) {
                const member = await openAsRoleUser(browser, RBAC_BASE_URL, roleUser, rbacWorkspace);

                try {
                    await member.page.goto(CONNECTIONS_PATH);

                    const row = connectionRow(member.page, name);

                    await expect(row).toBeVisible({timeout: 30000});
                    await expect(row.getByRole('button', {name: 'Connection actions'})).toHaveCount(0);
                } finally {
                    await member.context.close();
                }
            }
        } finally {
            await ownerApi.dispose();

            await deleteRoleUser(adminApi, owner);
        }
    });
});
