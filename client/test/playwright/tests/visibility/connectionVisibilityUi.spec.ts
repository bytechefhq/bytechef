import {type APIRequestContext, type Page, expect} from '@playwright/test';

import {workspaceRolesTest as test} from '../../fixtures/workspaceRoles';
import {
    type BuiltInRoleType,
    RBAC_BASE_URL,
    createCustomRole,
    deleteCustomRole,
    deleteRoleUser,
    openAsRoleUser,
    openAsTenantAdmin,
} from '../../utils/rbacUtils';
import {
    type RoleUserI,
    connectionName,
    createConnection,
    createCustomRoleMember,
    createUserApiContext,
    createWorkspaceMember,
    fetchConnectionGrants,
    fetchUserId,
    getConnection,
    listConnectionIds,
    removeWorkspaceMember,
    setWorkspaceRole,
} from '../../utils/visibilityUtils';

const CONNECTIONS_PATH = '/automation/connections';

function connectionRow(page: Page, name: string) {
    return page.locator('main ul > li').filter({hasText: name});
}

async function openConnections(page: Page, name: string) {
    await page.goto(CONNECTIONS_PATH);

    await expect(connectionRow(page, name)).toBeVisible({timeout: 30000});

    return connectionRow(page, name);
}

test.describe('Connection visibility UI', () => {
    let editorApi: APIRequestContext;

    test.beforeAll(async ({workerRoleUsers}) => {
        editorApi = await createUserApiContext(workerRoleUsers.EDITOR.email, workerRoleUsers.EDITOR.password);
    });

    test.afterAll(async () => {
        await editorApi.dispose();
    });

    test('the create dialog offers Shared with workspace and Private, and a Private connection shows the Private badge', async ({
        browser,
        rbacWorkspace,
        workerRoleUsers,
    }) => {
        const name = connectionName('vis_ui_create');

        const {context, page} = await openAsRoleUser(browser, RBAC_BASE_URL, workerRoleUsers.EDITOR, rbacWorkspace);

        try {
            await page.goto(CONNECTIONS_PATH);

            await page
                .getByRole('button', {name: /^(New|Create) Connection$/})
                .first()
                .click();

            const dialog = page.getByRole('dialog', {name: 'Create Connection'});

            await dialog.getByRole('combobox').first().click();

            await page.keyboard.type('HTTP Client');

            await page.getByRole('option', {name: 'HTTP Client'}).first().click();

            await dialog.getByRole('textbox', {name: 'Name'}).fill(name);

            await expect(dialog.getByRole('radio')).toHaveCount(2);
            await expect(dialog.getByRole('radio', {name: 'Shared with workspace'})).toBeChecked();
            await expect(dialog.getByRole('radio', {name: 'Specific people'})).toHaveCount(0);

            await dialog.getByRole('radio', {name: 'Private'}).click();

            await dialog.getByRole('button', {name: 'Save'}).click();

            await expect(dialog).toBeHidden();

            await expect(connectionRow(page, name).getByRole('button', {name: 'Change visibility'})).toHaveText(
                'Private'
            );
        } finally {
            await context.close();
        }
    });

    test('the owner shares a private connection with specific people from its badge, then with the whole workspace', async ({
        adminApi,
        browser,
        rbacWorkspace,
        workerRoleUsers,
    }) => {
        const name = connectionName('vis_ui_share');

        const connectionId = await createConnection(editorApi, rbacWorkspace.id, {name, visibility: 'PRIVATE'});

        const viewerId = await fetchUserId(adminApi, workerRoleUsers.VIEWER.login);

        const {context, page} = await openAsRoleUser(browser, RBAC_BASE_URL, workerRoleUsers.EDITOR, rbacWorkspace);

        try {
            const row = await openConnections(page, name);

            const badge = row.getByRole('button', {name: 'Change visibility'});

            await expect(badge).toHaveText('Private');

            await badge.click();

            const menu = page.getByRole('menu', {name: 'Change visibility'});

            await menu.getByRole('radio', {name: 'Specific people'}).click();

            await menu.getByRole('combobox', {name: 'Add person'}).click();

            await page.getByRole('option', {name: workerRoleUsers.VIEWER.email}).click();

            await expect(menu.getByText(workerRoleUsers.VIEWER.email)).toBeVisible();

            await expect
                .poll(async () =>
                    (
                        (await fetchConnectionGrants(editorApi, rbacWorkspace.id, connectionId)).data
                            ?.connectionGrants ?? []
                    ).map(Number)
                )
                .toEqual([Number(viewerId)]);

            await page.keyboard.press('Escape');

            await expect(menu).toBeHidden();

            await expect(badge).toHaveText('Specific people');

            const viewerApi = await createUserApiContext(workerRoleUsers.VIEWER.email, workerRoleUsers.VIEWER.password);

            try {
                expect(await listConnectionIds(viewerApi, rbacWorkspace.id)).toContain(connectionId);
            } finally {
                await viewerApi.dispose();
            }

            await badge.click();

            await menu.getByRole('radio', {name: 'Shared with workspace'}).click();

            await expect.poll(async () => (await getConnection(editorApi, connectionId)).visibility).toBe('WORKSPACE');

            await page.keyboard.press('Escape');

            await expect(menu).toBeHidden();

            await expect(badge).toHaveText('Workspace');
        } finally {
            await context.close();
        }
    });

    test('a member who neither owns a connection nor is a tenant admin sees a read-only badge and no edit or delete', async ({
        adminApi,
        browser,
        rbacWorkspace,
        workerRoleUsers,
    }) => {
        const name = connectionName('vis_ui_readonly');

        await createConnection(adminApi, rbacWorkspace.id, {name});

        for (const roleUser of [workerRoleUsers.ADMIN, workerRoleUsers.EDITOR, workerRoleUsers.VIEWER]) {
            const {context, page} = await openAsRoleUser(browser, RBAC_BASE_URL, roleUser, rbacWorkspace);

            try {
                const row = await openConnections(page, name);

                await expect(row.getByText('Workspace', {exact: true})).toBeVisible();
                await expect(row.getByRole('button', {name: 'Change visibility'})).toHaveCount(0);

                const actions = row.getByRole('button', {name: 'Connection actions'});

                if ((await actions.count()) > 0) {
                    await actions.click();

                    await expect(page.getByRole('menuitem', {name: 'Edit'})).toHaveCount(0);
                    await expect(page.getByRole('menuitem', {name: 'Delete'})).toHaveCount(0);
                }
            } finally {
                await context.close();
            }
        }
    });

    test('an EDITOR owner is offered the visibility picker, edit and delete', async ({
        browser,
        rbacWorkspace,
        workerRoleUsers,
    }) => {
        const name = connectionName('vis_ui_owner');

        await createConnection(editorApi, rbacWorkspace.id, {name});

        const {context, page} = await openAsRoleUser(browser, RBAC_BASE_URL, workerRoleUsers.EDITOR, rbacWorkspace);

        try {
            const row = await openConnections(page, name);

            await expect(row.getByRole('button', {name: 'Change visibility'})).toHaveText('Workspace');

            await row.getByRole('button', {name: 'Connection actions'}).click();

            await expect(page.getByRole('menuitem', {name: 'Edit'})).toBeVisible();
            await expect(page.getByRole('menuitem', {name: 'Delete'})).toBeVisible();
        } finally {
            await context.close();
        }
    });

    for (const [ownerRole, offered] of [
        ['VIEWER', false],
        ['ADMIN', true],
    ] as [BuiltInRoleType, boolean][]) {
        test(`an owner who is a workspace ${ownerRole} ${offered ? 'is offered' : 'is not offered'} edit and delete`, async ({
            adminApi,
            browser,
            rbacWorkspace,
        }) => {
            const owner = await createWorkspaceMember(adminApi, rbacWorkspace.id, 'EDITOR');
            const ownerApi = await createUserApiContext(owner.email, owner.password);
            const name = connectionName('vis_ui_owner_role');

            try {
                await createConnection(ownerApi, rbacWorkspace.id, {name});

                await setWorkspaceRole(adminApi, rbacWorkspace.id, owner, ownerRole);

                const {context, page} = await openAsRoleUser(browser, RBAC_BASE_URL, owner, rbacWorkspace);

                try {
                    const row = await openConnections(page, name);

                    if (offered) {
                        await row.getByRole('button', {name: 'Connection actions'}).click();

                        await expect(page.getByRole('menuitem', {name: 'Edit'})).toBeVisible();
                        await expect(page.getByRole('menuitem', {name: 'Delete'})).toBeVisible();
                    } else {
                        await expect(row.getByRole('button', {name: 'Connection actions'})).toHaveCount(0);
                    }
                } finally {
                    await context.close();
                }
            } finally {
                await ownerApi.dispose();

                await deleteRoleUser(adminApi, owner);
            }
        });
    }

    test("a tenant admin renames a colleague's connection through the Edit action it is offered", async ({
        browser,
        rbacWorkspace,
    }) => {
        const name = connectionName('vis_ui_admin_edit');
        const renamed = `${name}_renamed`;

        const connectionId = await createConnection(editorApi, rbacWorkspace.id, {name});

        const {context, page} = await openAsTenantAdmin(browser, RBAC_BASE_URL, rbacWorkspace);

        try {
            const row = await openConnections(page, name);

            await row.getByRole('button', {name: 'Connection actions'}).click();

            await page.getByRole('menuitem', {name: 'Edit'}).click();

            const dialog = page.getByRole('dialog');

            await dialog.getByRole('textbox', {name: 'Name'}).fill(renamed);

            await dialog.getByRole('button', {name: 'Save'}).click();

            await expect.poll(async () => (await getConnection(editorApi, connectionId)).name).toBe(renamed);
        } finally {
            await context.close();
        }
    });

    test('a custom-role member holding every connection scope is offered no picker, edit or delete on a colleague connection', async ({
        adminApi,
        browser,
        rbacWorkspace,
    }) => {
        const customRole = await createCustomRole(adminApi, ['CONNECTION_VIEW', 'CONNECTION_CREATE']);

        let member: RoleUserI | undefined;

        try {
            member = await createCustomRoleMember(adminApi, rbacWorkspace.id, customRole);

            const name = connectionName('vis_ui_custom');

            await createConnection(editorApi, rbacWorkspace.id, {name});

            const {context, page} = await openAsRoleUser(browser, RBAC_BASE_URL, member, rbacWorkspace);

            try {
                const row = await openConnections(page, name);

                await expect(row.getByText('Workspace', {exact: true})).toBeVisible();
                await expect(row.getByRole('button', {name: 'Change visibility'})).toHaveCount(0);
                await expect(row.getByRole('button', {name: 'Connection actions'})).toHaveCount(0);
            } finally {
                await context.close();
            }
        } finally {
            if (member) {
                await deleteRoleUser(adminApi, member);
            }

            await deleteCustomRole(adminApi, customRole);
        }
    });

    test.describe('reassignment', () => {
        async function createPendingConnection(
            adminApi: APIRequestContext,
            workspaceId: number
        ): Promise<{connectionId: number; leaver: RoleUserI; name: string}> {
            const leaver = await createWorkspaceMember(adminApi, workspaceId, 'EDITOR');

            const leaverApi = await createUserApiContext(leaver.email, leaver.password);

            const name = connectionName('vis_ui_pending');

            let connectionId: number;

            try {
                connectionId = await createConnection(leaverApi, workspaceId, {name});
            } finally {
                await leaverApi.dispose();
            }

            await removeWorkspaceMember(adminApi, workspaceId, leaver);

            await expect
                .poll(async () => (await getConnection(adminApi, connectionId)).status, {timeout: 15000})
                .toBe('PENDING_REASSIGNMENT');

            return {connectionId, leaver, name};
        }

        test('a tenant admin reassigns a pending connection from the connection list', async ({
            adminApi,
            browser,
            rbacWorkspace,
            workerRoleUsers,
        }) => {
            const {connectionId, leaver, name} = await createPendingConnection(adminApi, rbacWorkspace.id);

            const {context, page} = await openAsTenantAdmin(browser, RBAC_BASE_URL, rbacWorkspace);

            try {
                const row = await openConnections(page, name);

                await row.getByRole('button', {name: 'Connection actions'}).click();

                await page.getByRole('menuitem', {name: 'Reassign owner'}).click();

                const dialog = page.getByRole('dialog', {name: 'Reassign Connections'});

                await expect(dialog).toContainText(`The user ${leaver.login} owns 1 connection`);
                await expect(dialog.getByRole('row').filter({hasText: name})).toContainText('Workspace');

                await dialog.getByRole('combobox').filter({hasText: 'Select new owner...'}).click();

                await page.getByRole('option', {name: new RegExp(workerRoleUsers.EDITOR.email)}).click();

                await dialog.getByRole('button', {name: 'Reassign All'}).click();

                await expect(dialog).toBeHidden();

                await expect
                    .poll(async () => {
                        const connection = await getConnection(adminApi, connectionId);

                        return `${connection.status} ${connection.createdBy}`;
                    })
                    .toBe(`ACTIVE ${workerRoleUsers.EDITOR.login}`);
            } finally {
                await context.close();

                await deleteRoleUser(adminApi, leaver);
            }
        });

        test('a workspace admin who is not a tenant admin is offered neither Reassign owner nor Edit', async ({
            adminApi,
            browser,
            rbacWorkspace,
            workerRoleUsers,
        }) => {
            const {leaver, name} = await createPendingConnection(adminApi, rbacWorkspace.id);

            const {context, page} = await openAsRoleUser(browser, RBAC_BASE_URL, workerRoleUsers.ADMIN, rbacWorkspace);

            try {
                const row = await openConnections(page, name);

                await expect(row.getByText('Workspace', {exact: true})).toBeVisible();
                await expect(row.getByRole('button', {name: 'Connection actions'})).toHaveCount(0);
                await expect(page.getByRole('menuitem', {name: 'Reassign owner'})).toHaveCount(0);
            } finally {
                await context.close();

                await deleteRoleUser(adminApi, leaver);
            }
        });
    });
});
