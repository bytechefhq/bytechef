import {type BrowserContext, type Page, expect} from '@playwright/test';

import {embeddedRolesTest as test} from '../../fixtures/embeddedRoles';
import {embeddedInternalApi, openAsEmbeddedRoleUser} from '../../utils/embeddedRbacUtils';
import {ENVIRONMENT_IDS, RBAC_BASE_URL, expectPresence, graphql, openAsTenantAdmin} from '../../utils/rbacUtils';

type EmbeddedCallerType = 'ADMIN' | 'EDITOR' | 'TENANT_ADMIN' | 'VIEWER';

interface RoleExpectationsI {
    addMember: boolean;
    apiKeys: boolean;
    createApiKey: boolean;
    createAppEvent: boolean;
    createConfiguration: boolean;
    createConnection: boolean;
    createIntegration: boolean;
    createMcpServer: boolean;
    deleteIntegration: boolean;
    editIntegration: boolean;
    mcpServerUrl: boolean;
    publishIntegration: boolean;
    regenerateMcpServerUrl: boolean;
    signingKeys: boolean;
}

const ROLE_EXPECTATIONS: Record<EmbeddedCallerType, RoleExpectationsI> = {
    ADMIN: {
        addMember: true,
        apiKeys: true,
        createApiKey: true,
        createAppEvent: true,
        createConfiguration: true,
        createConnection: true,
        createIntegration: true,
        createMcpServer: true,
        deleteIntegration: true,
        editIntegration: true,
        mcpServerUrl: true,
        publishIntegration: true,
        regenerateMcpServerUrl: false,
        signingKeys: true,
    },
    EDITOR: {
        addMember: false,
        apiKeys: true,
        createApiKey: true,
        createAppEvent: true,
        createConfiguration: true,
        createConnection: true,
        createIntegration: true,
        createMcpServer: true,
        deleteIntegration: false,
        editIntegration: true,
        mcpServerUrl: true,
        publishIntegration: true,
        regenerateMcpServerUrl: false,
        signingKeys: false,
    },
    TENANT_ADMIN: {
        addMember: true,
        apiKeys: true,
        createApiKey: true,
        createAppEvent: true,
        createConfiguration: true,
        createConnection: true,
        createIntegration: true,
        createMcpServer: true,
        deleteIntegration: true,
        editIntegration: true,
        mcpServerUrl: true,
        publishIntegration: true,
        regenerateMcpServerUrl: true,
        signingKeys: true,
    },
    VIEWER: {
        addMember: false,
        apiKeys: false,
        createApiKey: false,
        createAppEvent: false,
        createConfiguration: false,
        createConnection: false,
        createIntegration: false,
        createMcpServer: false,
        deleteIntegration: false,
        editIntegration: false,
        mcpServerUrl: false,
        publishIntegration: false,
        regenerateMcpServerUrl: false,
        signingKeys: false,
    },
};

const EMBEDDED_PAGE_HREFS = [
    '/embedded/integrations',
    '/embedded/configurations',
    '/embedded/connected-users',
    '/embedded/app-events',
    '/embedded/executions',
    '/embedded/connections',
    '/embedded/mcp-servers',
];

async function openEmbeddedPage(page: Page, path: string, readyText: RegExp | string): Promise<void> {
    await page.goto(path);

    await expect(page.getByText(readyText).filter({visible: true}).first()).toBeVisible({timeout: 30_000});

    await page.waitForLoadState('networkidle');
}

test.describe.configure({mode: 'default'});

for (const caller of ['VIEWER', 'EDITOR', 'ADMIN', 'TENANT_ADMIN'] as EmbeddedCallerType[]) {
    const expectations = ROLE_EXPECTATIONS[caller];

    test.describe(`${caller === 'TENANT_ADMIN' ? 'tenant admin' : `embedded ${caller}`}`, () => {
        let context: BrowserContext;
        let page: Page;

        test.beforeEach(async ({browser, embeddedRoleUsers}) => {
            ({context, page} =
                caller === 'TENANT_ADMIN'
                    ? await openAsTenantAdmin(browser, RBAC_BASE_URL)
                    : await openAsEmbeddedRoleUser(
                          browser,
                          RBAC_BASE_URL,
                          embeddedRoleUsers[caller],
                          ENVIRONMENT_IDS.DEVELOPMENT
                      ));
        });

        test.afterEach(async () => {
            await context?.close();
        });

        test('integrations page offers only the integration actions the role holds', async ({embeddedData}) => {
            await openEmbeddedPage(page, '/embedded/integrations', embeddedData.integrationName);

            for (const href of EMBEDDED_PAGE_HREFS) {
                await expect(page.locator(`a[href="${href}"]`).first(), href).toBeVisible();
            }

            await expect(page.getByRole('button', {name: 'Create Integration'})).toHaveCount(
                expectations.createIntegration ? 1 : 0
            );

            const integrationItem = page
                .locator('div.cursor-pointer')
                .filter({has: page.getByText(embeddedData.integrationName, {exact: true})})
                .last();

            await integrationItem.locator('button[aria-haspopup="menu"]').last().click();

            await expectPresence(page, 'View Workflows', true, 'menuitem');
            await expectPresence(page, 'Edit', expectations.editIntegration, 'menuitem');
            await expectPresence(page, 'Publish', expectations.publishIntegration, 'menuitem');
            await expectPresence(page, 'Delete', expectations.deleteIntegration, 'menuitem');

            await page.keyboard.press('Escape');
        });

        test('configurations, connections and app events offer create only to roles that may create', async ({
            adminApi,
            embeddedData,
        }) => {
            const configuration = await embeddedInternalApi<{name: string}>(
                adminApi,
                'GET',
                `/integration-instance-configurations/${embeddedData.byEnvironment.DEVELOPMENT.instanceConfigurationId}`
            );

            await openEmbeddedPage(page, '/embedded/configurations', configuration.name);

            await expectPresence(
                page,
                /^(New|Create) Instance Configuration$/,
                expectations.createConfiguration,
                'button'
            );

            await openEmbeddedPage(page, '/embedded/connections', /^Connections$/);

            await expectPresence(page, /^(New|Create) Connection$/, expectations.createConnection, 'button');

            await openEmbeddedPage(page, '/embedded/app-events', /^App Events$/);

            await expectPresence(page, 'New App Event', expectations.createAppEvent, 'button');
        });

        test('MCP servers offer create and the server URL only to roles that may edit them', async ({
            adminApi,
            embeddedData,
        }) => {
            const {embeddedMcpServers} = await graphql<{embeddedMcpServers: {id: string; name: string}[]}>(
                adminApi,
                'query EmbeddedMcpServers { embeddedMcpServers { id name } }',
                {}
            );

            const mcpServerName = embeddedMcpServers.find(
                (mcpServer) => mcpServer.id === embeddedData.byEnvironment.DEVELOPMENT.mcpServerId
            )!.name;

            await openEmbeddedPage(page, '/embedded/mcp-servers', mcpServerName);

            await expectPresence(page, /^(New|Create) MCP Server$/, expectations.createMcpServer, 'button');

            await page.getByText(mcpServerName, {exact: true}).first().click();

            await page.getByRole('tab', {name: 'Connect'}).first().click();

            await expect(page.getByRole('button', {name: 'Copy server URL'})).toHaveCount(
                expectations.mcpServerUrl ? 1 : 0
            );
            await expect(page.getByRole('button', {name: 'Regenerate server URL'})).toHaveCount(
                expectations.regenerateMcpServerUrl ? 1 : 0
            );
        });

        test('settings show members, signing keys and API keys by role', async () => {
            await openEmbeddedPage(page, '/embedded/settings/members', /^Members$/);

            await expect(page.getByRole('link', {exact: true, name: 'Members'})).toBeVisible();
            await expect(page.getByRole('link', {exact: true, name: 'Signing Keys'})).toHaveCount(
                expectations.signingKeys ? 1 : 0
            );
            await expect(page.getByRole('link', {exact: true, name: 'API Keys'})).toHaveCount(
                expectations.apiKeys ? 1 : 0
            );

            await expectPresence(page, 'Add member', expectations.addMember, 'button');

            await openEmbeddedPage(page, '/embedded/settings/api-keys', /^API Keys$/);

            await expect(
                page.getByText('You do not have permission to view the API keys of this environment.')
            ).toHaveCount(expectations.apiKeys ? 0 : 1);

            await expectPresence(page, 'New API Key', expectations.createApiKey, 'button');

            await openEmbeddedPage(page, '/embedded/settings/signing-keys', /^Signing Keys$/);

            await expect(
                page.getByText('You do not have permission to manage the signing keys of this environment.')
            ).toHaveCount(expectations.signingKeys ? 0 : 1);
        });
    });
}

test.describe('tenant user who is not an embedded member', () => {
    test('sees no embedded pages and the not-a-member notice on every embedded URL', async ({
        browser,
        embeddedRoleUsers,
    }) => {
        const {context, page} = await openAsEmbeddedRoleUser(
            browser,
            RBAC_BASE_URL,
            embeddedRoleUsers.OUTSIDER,
            ENVIRONMENT_IDS.DEVELOPMENT
        );

        const deniedResponsePromises: Promise<string | null>[] = [];

        page.on('response', (response) => {
            const url = response.url();

            if (url.includes('/api/embedded/') && response.status() === 403) {
                deniedResponsePromises.push(Promise.resolve(`${response.status()} ${url}`));
            } else if (url.includes('/graphql')) {
                deniedResponsePromises.push(
                    response
                        .text()
                        .then((body) =>
                            /"classification":"FORBIDDEN"|"errorCode":"ACCESS_DENIED"/.test(body)
                                ? `${response.status()} ${url} ${response.request().postData()?.slice(0, 120)}`
                                : null
                        )
                        .catch(() => null)
                );
            }
        });

        try {
            for (const path of [...EMBEDDED_PAGE_HREFS, '/embedded/settings/members']) {
                await test.step(path, async () => {
                    await page.goto(path);

                    await expect(page.getByText('No access to Embedded')).toBeVisible({timeout: 30_000});
                    await expect(
                        page.getByText(
                            'You are not an embedded member. Ask an embedded admin or a tenant admin to add you.'
                        )
                    ).toBeVisible();

                    for (const href of EMBEDDED_PAGE_HREFS) {
                        await expect(page.locator(`a[href="${href}"]`), href).toHaveCount(0);
                    }
                });
            }

            await page.waitForLoadState('networkidle');

            const deniedResponses = (await Promise.all(deniedResponsePromises)).filter(
                (deniedResponse): deniedResponse is string => deniedResponse !== null
            );

            expect(deniedResponses).toEqual([]);
        } finally {
            await context.close();
        }
    });
});
