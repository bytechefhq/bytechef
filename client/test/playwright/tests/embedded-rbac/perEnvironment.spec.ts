import {type APIRequestContext, type Page, expect} from '@playwright/test';

import {embeddedRolesTest as test} from '../../fixtures/embeddedRoles';
import {
    type EmbeddedDataI,
    embeddedInternalApi,
    openAsEmbeddedRoleUser,
    removeEmbeddedUser,
    setEmbeddedEnvironmentRole,
    setEmbeddedRole,
} from '../../utils/embeddedRbacUtils';
import {
    ENVIRONMENT_IDS,
    RBAC_BASE_URL,
    type RoleUserI,
    createAccount,
    deleteRoleUser,
    graphql,
} from '../../utils/rbacUtils';

const NO_ACCESS_MESSAGE =
    'You have no role in this environment. Switch to an environment you have access to, or ask an embedded admin for access.';

interface EnvironmentNamesI {
    configurationName: string;
    connectionName: string;
    mcpServerName: string;
}

type EnvironmentNamesByEnvironmentType = Record<'DEVELOPMENT' | 'PRODUCTION', EnvironmentNamesI>;

async function fetchEnvironmentNames(
    adminApi: APIRequestContext,
    embeddedData: EmbeddedDataI
): Promise<EnvironmentNamesByEnvironmentType> {
    const {embeddedMcpServers} = await graphql<{embeddedMcpServers: {id: string; name: string}[]}>(
        adminApi,
        'query EmbeddedMcpServers { embeddedMcpServers { id name } }',
        {}
    );

    const environmentNames = {} as EnvironmentNamesByEnvironmentType;

    for (const environment of ['DEVELOPMENT', 'PRODUCTION'] as const) {
        const environmentData = embeddedData.byEnvironment[environment];

        const configuration = await embeddedInternalApi<{name: string}>(
            adminApi,
            'GET',
            `/integration-instance-configurations/${environmentData.instanceConfigurationId}`
        );

        const connection = await embeddedInternalApi<{name: string}>(
            adminApi,
            'GET',
            `/connections/${environmentData.connectionId}`
        );

        environmentNames[environment] = {
            configurationName: configuration.name,
            connectionName: connection.name,
            mcpServerName: embeddedMcpServers.find((mcpServer) => mcpServer.id === environmentData.mcpServerId)!.name,
        };
    }

    return environmentNames;
}

async function expectOfferedEnvironments(page: Page, offered: string[], notOffered: string[]): Promise<void> {
    await page
        .getByRole('button', {name: /^(DEVELOPMENT|STAGING|PRODUCTION)$/})
        .first()
        .click();

    for (const environmentLabel of offered) {
        await expect(page.getByRole('menuitemradio').filter({hasText: environmentLabel})).toBeVisible();
    }

    for (const environmentLabel of notOffered) {
        await expect(page.getByRole('menuitemradio').filter({hasText: environmentLabel})).toHaveCount(0);
    }
}

async function openListPage(page: Page, path: string, itemName: string): Promise<void> {
    await page.goto(path);

    await expect(page.getByText(itemName, {exact: true}).first()).toBeVisible({timeout: 30_000});
}

async function navigateInApp(page: Page, path: string, itemName: string): Promise<void> {
    await page.locator(`a[href="${path}"]`).first().click();

    await expect(page).toHaveURL(new RegExp(`${path}$`));

    await expect(page.getByText(itemName, {exact: true}).first()).toBeVisible({timeout: 30_000});
}

async function expectSelectedEnvironmentId(page: Page, environmentId: number): Promise<void> {
    await expect
        .poll(
            () =>
                page.evaluate(
                    () =>
                        JSON.parse(window.localStorage.getItem('bytechef.environment') || '{}').state
                            ?.currentEnvironmentId
                ),
            {timeout: 30_000}
        )
        .toBe(environmentId);
}

test.describe('Embedded per-environment roles', () => {
    let environmentNames: EnvironmentNamesByEnvironmentType;

    test.beforeAll(async ({adminApi, embeddedData}) => {
        environmentNames = await fetchEnvironmentNames(adminApi, embeddedData);
    });

    test('a Production-only EDITOR with Development selected lands in Production and works there', async ({
        browser,
        embeddedRoleUsers,
    }) => {
        const {context, page} = await openAsEmbeddedRoleUser(
            browser,
            RBAC_BASE_URL,
            embeddedRoleUsers.PRODUCTION_EDITOR,
            ENVIRONMENT_IDS.DEVELOPMENT
        );

        try {
            await test.step('the stale Development selection falls back to Production, not to a blank page', async () => {
                await openListPage(page, '/embedded/configurations', environmentNames.PRODUCTION.configurationName);

                await expectSelectedEnvironmentId(page, ENVIRONMENT_IDS.PRODUCTION);

                await expect(page.getByText(NO_ACCESS_MESSAGE)).toHaveCount(0);
                await expect(page.getByText(environmentNames.DEVELOPMENT.configurationName)).toHaveCount(0);
            });

            await test.step('the environment selector offers only Production', async () => {
                await expectOfferedEnvironments(page, ['PRODUCTION'], ['DEVELOPMENT', 'STAGING']);

                await page.keyboard.press('Escape');
            });

            await test.step('Production configurations can be created', async () => {
                await expect(page.getByRole('button', {name: 'New Instance Configuration'})).toBeVisible();
            });

            await test.step('Production connections can be created', async () => {
                await navigateInApp(page, '/embedded/connections', environmentNames.PRODUCTION.connectionName);

                await expect(page.getByText(environmentNames.DEVELOPMENT.connectionName)).toHaveCount(0);
                await expect(page.getByRole('button', {name: 'New Connection'})).toBeVisible();
            });

            await test.step('Production MCP servers can be created', async () => {
                await navigateInApp(page, '/embedded/mcp-servers', environmentNames.PRODUCTION.mcpServerName);

                await expect(page.getByText(environmentNames.DEVELOPMENT.mcpServerName)).toHaveCount(0);
                await expect(page.getByRole('button', {name: 'New MCP Server'})).toBeVisible();
            });
        } finally {
            await context.close();
        }
    });

    test('a Development-only EDITOR with Production selected lands in Development and edits workflows', async ({
        browser,
        embeddedData,
        embeddedRoleUsers,
    }) => {
        const {context, page} = await openAsEmbeddedRoleUser(
            browser,
            RBAC_BASE_URL,
            embeddedRoleUsers.DEVELOPMENT_EDITOR,
            ENVIRONMENT_IDS.PRODUCTION
        );

        try {
            await test.step('the stale Production selection falls back to Development', async () => {
                await openListPage(page, '/embedded/connections', environmentNames.DEVELOPMENT.connectionName);

                await expectSelectedEnvironmentId(page, ENVIRONMENT_IDS.DEVELOPMENT);

                await expect(page.getByText(NO_ACCESS_MESSAGE)).toHaveCount(0);
            });

            await test.step('the environment selector offers only Development', async () => {
                await expectOfferedEnvironments(page, ['DEVELOPMENT'], ['PRODUCTION', 'STAGING']);

                await page.keyboard.press('Escape');
            });

            await test.step('Development connections can be created', async () => {
                await expect(page.getByText(environmentNames.PRODUCTION.connectionName)).toHaveCount(0);
                await expect(page.getByRole('button', {name: 'New Connection'})).toBeVisible();
            });

            await test.step('the Integration editor opens editable', async () => {
                const [integrationWorkflow] = await embeddedInternalApi<{integrationWorkflowId: number}[]>(
                    page.request,
                    'GET',
                    `/integrations/${embeddedData.integrationId}/workflows`
                );

                await page.goto(
                    `/embedded/integrations/${embeddedData.integrationId}/integration-workflows/${integrationWorkflow.integrationWorkflowId}`
                );

                await expect(page.getByRole('button', {name: 'Components & Flow Controls'})).toBeVisible({
                    timeout: 30_000,
                });
                await expect(page.getByRole('status', {name: 'View only'})).toHaveCount(0);
            });
        } finally {
            await context.close();
        }
    });

    test('a Development VIEWER who is a Production EDITOR gets each environment its own controls', async ({
        adminApi,
        browser,
        embeddedData,
    }) => {
        let roleUser: RoleUserI | undefined;

        try {
            roleUser = await createAccount(adminApi, RBAC_BASE_URL);

            await setEmbeddedRole(adminApi, roleUser.userId!, 'VIEWER');
            await setEmbeddedEnvironmentRole(adminApi, roleUser.userId!, 'PRODUCTION', 'EDITOR');

            const {context, page} = await openAsEmbeddedRoleUser(
                browser,
                RBAC_BASE_URL,
                roleUser,
                ENVIRONMENT_IDS.DEVELOPMENT
            );

            try {
                await test.step('Development offers no create controls', async () => {
                    await openListPage(page, '/embedded/connections', environmentNames.DEVELOPMENT.connectionName);

                    await expect(page.getByRole('button', {name: 'New Connection'})).toHaveCount(0);

                    await openListPage(
                        page,
                        '/embedded/configurations',
                        environmentNames.DEVELOPMENT.configurationName
                    );

                    await expect(page.getByRole('button', {name: 'New Instance Configuration'})).toHaveCount(0);
                });

                await test.step('Production offers the create controls', async () => {
                    await expectOfferedEnvironments(page, ['DEVELOPMENT', 'STAGING', 'PRODUCTION'], []);

                    await page.getByRole('menuitemradio').filter({hasText: 'PRODUCTION'}).click();

                    await expect(page.getByText(environmentNames.PRODUCTION.configurationName).first()).toBeVisible({
                        timeout: 30_000,
                    });

                    await expect(page.getByRole('button', {name: 'New Instance Configuration'})).toBeVisible();

                    await navigateInApp(page, '/embedded/connections', environmentNames.PRODUCTION.connectionName);

                    await expect(page.getByRole('button', {name: 'New Connection'})).toBeVisible();
                });

                await test.step('the Integration editor redirects away from Production, because workflows are edited in Development', async () => {
                    const [integrationWorkflow] = await embeddedInternalApi<{integrationWorkflowId: number}[]>(
                        page.request,
                        'GET',
                        `/integrations/${embeddedData.integrationId}/workflows`
                    );

                    await page.goto(
                        `/embedded/integrations/${embeddedData.integrationId}/integration-workflows/${integrationWorkflow.integrationWorkflowId}`
                    );

                    await expect(page).toHaveURL(/\/embedded\/configurations/, {timeout: 30_000});
                    await expect(page.getByRole('button', {name: 'Components & Flow Controls'})).toHaveCount(0);
                });
            } finally {
                await context.close();
            }
        } finally {
            if (roleUser) {
                await removeEmbeddedUser(adminApi, roleUser.userId!).catch(() => undefined);

                await deleteRoleUser(adminApi, roleUser).catch((error: unknown) => {
                    console.warn(`perEnvironment teardown: deleting ${roleUser!.login} failed: ${error}`);
                });
            }
        }
    });
});
