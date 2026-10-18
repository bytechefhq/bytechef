import {type APIRequestContext, expect} from '@playwright/test';

import {workspaceRolesTest as test} from '../../fixtures/workspaceRoles';
import {RBAC_BASE_URL, openAsRoleUser, openAsTenantAdmin} from '../../utils/rbacUtils';
import {
    connectionName,
    createConnection,
    createOrganizationConnection,
    createOrganizationConnectionId,
    createUserApiContext,
    deleteOrganizationConnection,
    expectAccessDenied,
    expectGraphQlError,
    expectNoGraphQlErrors,
    getConnection,
    getConnectionResponse,
    postGraphQl,
} from '../../utils/visibilityUtils';

const ORGANIZATION_CONNECTIONS_PATH = '/automation/settings/connections';

async function fetchOrganizationConnectionIds(
    apiContext: APIRequestContext,
    environmentId?: number
): Promise<number[]> {
    const response = await postGraphQl<{organizationConnections: {id: string}[]}>(
        apiContext,
        'query OrganizationConnections($environmentId: ID) { organizationConnections(environmentId: $environmentId) { id } }',
        environmentId === undefined ? {} : {environmentId: String(environmentId)}
    );

    expectNoGraphQlErrors(response);

    return response.data!.organizationConnections.map((connection) => Number(connection.id));
}

test.describe('Organization connections', () => {
    test('a tenant admin creates an organization connection from Settings through the shared connection dialog', async ({
        adminApi,
        browser,
        rbacWorkspace,
    }) => {
        const name = connectionName('vis_org_ui');

        const {context, page} = await openAsTenantAdmin(browser, RBAC_BASE_URL, rbacWorkspace);

        try {
            await page.goto(ORGANIZATION_CONNECTIONS_PATH);

            await expect(page.getByText('Organization Connections', {exact: true})).toBeVisible();

            await page.getByRole('button', {name: 'New Connection'}).first().click();

            const dialog = page.getByRole('dialog', {name: 'Create Connection'});

            await expect(dialog.getByText('Organization', {exact: true})).toBeVisible();
            await expect(dialog.getByRole('radio')).toHaveCount(0);

            await dialog.getByRole('combobox').first().click();

            await page.keyboard.type('HTTP Client');

            await page.getByRole('option', {name: 'HTTP Client'}).first().click();

            await dialog.getByRole('textbox', {name: 'Name'}).fill(name);

            await dialog.getByRole('button', {name: 'Save'}).click();

            await expect(dialog).toBeHidden();

            await expect(page.getByRole('row').filter({hasText: name})).toBeVisible();
        } finally {
            await context.close();
        }

        const organizationConnections = await postGraphQl<{organizationConnections: {id: string; name: string}[]}>(
            adminApi,
            'query { organizationConnections { id name } }'
        );

        const created = organizationConnections.data?.organizationConnections.find(
            (connection) => connection.name === name
        );

        expect(created, JSON.stringify(organizationConnections)).toBeDefined();

        await deleteOrganizationConnection(adminApi, Number(created!.id));
    });

    test('a tenant admin deletes an organization connection from Settings', async ({
        adminApi,
        browser,
        rbacWorkspace,
    }) => {
        const name = connectionName('vis_org_delete');

        const connectionId = await createOrganizationConnectionId(adminApi, {name});

        const {context, page} = await openAsTenantAdmin(browser, RBAC_BASE_URL, rbacWorkspace);

        try {
            await page.goto(ORGANIZATION_CONNECTIONS_PATH);

            const row = page.getByRole('row').filter({hasText: name});

            await expect(row).toContainText('HTTP Client');
            await expect(row).toContainText('admin');

            await row.getByRole('button').click();

            await page.getByRole('alertdialog').getByRole('button', {name: 'Delete'}).click();

            await expect(row).toBeHidden();
        } finally {
            await context.close();
        }

        expect((await getConnectionResponse(adminApi, connectionId)).status()).not.toBe(200);
    });

    test('the Organization Connections list follows the environment selected in its header', async ({
        adminApi,
        browser,
        rbacWorkspace,
    }) => {
        const developmentName = connectionName('vis_org_dev');
        const productionName = connectionName('vis_org_prod');

        const developmentId = await createOrganizationConnectionId(adminApi, {environmentId: 0, name: developmentName});
        const productionId = await createOrganizationConnectionId(adminApi, {environmentId: 2, name: productionName});

        const {context, page} = await openAsTenantAdmin(browser, RBAC_BASE_URL, rbacWorkspace);

        try {
            await page.goto(ORGANIZATION_CONNECTIONS_PATH);

            await expect(page.getByRole('row').filter({hasText: developmentName})).toBeVisible();
            await expect(page.getByRole('row').filter({hasText: productionName})).toHaveCount(0);
        } finally {
            await context.close();

            await deleteOrganizationConnection(adminApi, developmentId);
            await deleteOrganizationConnection(adminApi, productionId);
        }
    });

    test('organizationConnections filters by environment when one is named', async ({adminApi}) => {
        const developmentId = await createOrganizationConnectionId(adminApi, {environmentId: 0});
        const productionId = await createOrganizationConnectionId(adminApi, {environmentId: 2});

        try {
            const developmentIds = await fetchOrganizationConnectionIds(adminApi, 0);

            expect(developmentIds).toContain(developmentId);
            expect(developmentIds).not.toContain(productionId);

            const allIds = await fetchOrganizationConnectionIds(adminApi);

            expect(allIds).toEqual(expect.arrayContaining([developmentId, productionId]));
        } finally {
            await deleteOrganizationConnection(adminApi, developmentId);
            await deleteOrganizationConnection(adminApi, productionId);
        }
    });

    test('updateOrganizationConnection renames an organization connection and refuses a workspace connection', async ({
        adminApi,
        rbacWorkspace,
    }) => {
        const connectionId = await createOrganizationConnectionId(adminApi);
        const workspaceConnectionId = await createConnection(adminApi, rbacWorkspace.id);

        const updateMutation = `mutation Update($connectionId: ID!, $name: String!, $version: Int!) {
            updateOrganizationConnection(connectionId: $connectionId, name: $name, version: $version)
        }`;

        try {
            const renamed = connectionName('vis_org_renamed');

            const version = ((await getConnection(adminApi, connectionId)) as unknown as {__version: number}).__version;

            expectNoGraphQlErrors(
                await postGraphQl(adminApi, updateMutation, {
                    connectionId: String(connectionId),
                    name: renamed,
                    version,
                })
            );

            expect((await getConnection(adminApi, connectionId)).name).toBe(renamed);

            expectGraphQlError(
                await postGraphQl(adminApi, updateMutation, {
                    connectionId: String(workspaceConnectionId),
                    name: 'vis_not_org',
                    version: 0,
                }),
                /is not an organization connection/
            );

            expectGraphQlError(
                await deleteOrganizationConnection(adminApi, workspaceConnectionId),
                /is not an organization connection/
            );

            expect((await getConnection(adminApi, workspaceConnectionId)).visibility).toBe('WORKSPACE');
        } finally {
            await deleteOrganizationConnection(adminApi, connectionId);
        }
    });

    test('a member who is not a tenant admin can neither open the page nor call the organization connection API', async ({
        adminApi,
        browser,
        rbacWorkspace,
        workerRoleUsers,
    }) => {
        const workspaceAdminApi = await createUserApiContext(
            workerRoleUsers.ADMIN.email,
            workerRoleUsers.ADMIN.password
        );

        const connectionId = await createOrganizationConnectionId(adminApi);

        try {
            expectAccessDenied(await postGraphQl(workspaceAdminApi, 'query { organizationConnections { id } }'));
            expectAccessDenied(await createOrganizationConnection(workspaceAdminApi));
            expectAccessDenied(await deleteOrganizationConnection(workspaceAdminApi, connectionId));

            const {context, page} = await openAsRoleUser(browser, RBAC_BASE_URL, workerRoleUsers.ADMIN, rbacWorkspace);

            try {
                await page.goto(ORGANIZATION_CONNECTIONS_PATH);

                await expect(page.getByText('Organization Connections', {exact: true})).toHaveCount(0);
                await expect(page.locator(`a[href="${ORGANIZATION_CONNECTIONS_PATH}"]`)).toHaveCount(0);
            } finally {
                await context.close();
            }
        } finally {
            await workspaceAdminApi.dispose();

            await deleteOrganizationConnection(adminApi, connectionId);
        }
    });
});
