import {type APIRequestContext, type Page, type Response, expect} from '@playwright/test';
import {readFileSync} from 'fs';

import {embeddedRolesTest as test} from '../../fixtures/embeddedRoles';
import {SAMPLE_WORKFLOW_PATH} from '../../utils/constants';
import {embeddedInternalApi, openAsEmbeddedRoleUser} from '../../utils/embeddedRbacUtils';
import getRandomString from '../../utils/getRandomString';
import {ENVIRONMENT_IDS, RBAC_BASE_URL, createRoleUserApiContext, graphql} from '../../utils/rbacUtils';

const HIDDEN_MCP_SERVER_URL_HINT = 'Only members who can edit this server can see its URL.';

const NAME_SUFFIX = getRandomString().slice(0, 8);

const CONNECTION_PASSWORD_MARKER = getRandomString().slice(0, 8);

const CONNECTION_USERNAME_MARKER = getRandomString().slice(0, 8);

const CONFIGURATION_PASSWORD_MARKER = getRandomString().slice(0, 8);

const CONFIGURATION_USERNAME_MARKER = getRandomString().slice(0, 8);

interface CredentialResourcesI {
    configurationId: number;
    configurationName: string;
    connectionId: number;
    connectionName: string;
    integrationId: number;
    mcpServerId: string;
    mcpServerName: string;
    mcpServerSecretKey: string;
}

interface ConnectionI {
    authorizationParameters?: Record<string, unknown> | null;
    connectionParameters?: Record<string, unknown> | null;
    parameters?: Record<string, unknown> | null;
}

interface ConfigurationI {
    connectionAuthorizationParameters?: Record<string, unknown> | null;
    connectionConnectionParameters?: Record<string, unknown> | null;
    connectionParameters?: Record<string, unknown> | null;
}

interface McpServerI {
    id: string;
    name: string;
    secretKey?: string | null;
    url?: string | null;
}

const SECRET_MARKERS = [
    CONNECTION_PASSWORD_MARKER,
    CONNECTION_USERNAME_MARKER,
    CONFIGURATION_PASSWORD_MARKER,
    CONFIGURATION_USERNAME_MARKER,
];

async function createCredentialResources(adminApi: APIRequestContext): Promise<CredentialResourcesI> {
    const connectionName = `rbac_credentials_${NAME_SUFFIX}`;

    const connectionId = await embeddedInternalApi<number>(adminApi, 'POST', '/connections', {
        authorizationType: 'BASIC_AUTH',
        componentName: 'httpClient',
        connectionVersion: 1,
        environmentId: ENVIRONMENT_IDS.DEVELOPMENT,
        name: connectionName,
        parameters: {
            password: `rbac_connection_secret_${CONNECTION_PASSWORD_MARKER}`,
            username: `rbac_connection_user_${CONNECTION_USERNAME_MARKER}`,
        },
        shared: true,
    });

    const integrationId = await embeddedInternalApi<number>(adminApi, 'POST', '/integrations', {
        componentName: 'httpClient',
        multipleInstances: false,
        name: `rbac_credentials_${NAME_SUFFIX}`,
    });

    await embeddedInternalApi(adminApi, 'POST', `/integrations/${integrationId}/workflows`, {
        definition: readFileSync(SAMPLE_WORKFLOW_PATH, 'utf-8'),
    });

    await embeddedInternalApi(adminApi, 'POST', `/integrations/${integrationId}/publish`, {description: 'credentials'});

    const configurationName = `rbac_credentials_${NAME_SUFFIX}`;

    const configurationId = await embeddedInternalApi<number>(
        adminApi,
        'POST',
        '/integration-instance-configurations',
        {
            authorizationType: 'BASIC_AUTH',
            connectionParameters: {
                password: `rbac_configuration_secret_${CONFIGURATION_PASSWORD_MARKER}`,
                username: `rbac_configuration_user_${CONFIGURATION_USERNAME_MARKER}`,
            },
            enabled: false,
            environmentId: ENVIRONMENT_IDS.DEVELOPMENT,
            integrationId,
            integrationVersion: 1,
            name: configurationName,
        }
    );

    const mcpServerName = `rbac_credentials_${NAME_SUFFIX}`;

    const {createEmbeddedMcpServer} = await graphql<{createEmbeddedMcpServer: McpServerI}>(
        adminApi,
        `
            mutation CreateEmbeddedMcpServer($input: CreateEmbeddedMcpServerInput!) {
                createEmbeddedMcpServer(input: $input) {
                    id
                    name
                    secretKey
                    url
                }
            }
        `,
        {input: {enabled: true, environmentId: String(ENVIRONMENT_IDS.DEVELOPMENT), name: mcpServerName}}
    );

    return {
        configurationId,
        configurationName,
        connectionId,
        connectionName,
        integrationId,
        mcpServerId: createEmbeddedMcpServer.id,
        mcpServerName,
        mcpServerSecretKey: createEmbeddedMcpServer.secretKey!,
    };
}

async function deleteCredentialResources(adminApi: APIRequestContext, resources: CredentialResourcesI) {
    const attempt = async (description: string, action: () => Promise<unknown>) => {
        try {
            await action();
        } catch (error) {
            console.warn(`credentials teardown: ${description}: ${error}`);
        }
    };

    await attempt('delete MCP server', () =>
        graphql(adminApi, 'mutation DeleteEmbeddedMcpServer($id: ID!) { deleteEmbeddedMcpServer(mcpServerId: $id) }', {
            id: resources.mcpServerId,
        })
    );

    await attempt('delete configuration', () =>
        embeddedInternalApi(adminApi, 'DELETE', `/integration-instance-configurations/${resources.configurationId}`)
    );

    await attempt('delete integration', () =>
        embeddedInternalApi(adminApi, 'DELETE', `/integrations/${resources.integrationId}`)
    );

    await attempt('delete connection', () =>
        embeddedInternalApi(adminApi, 'DELETE', `/connections/${resources.connectionId}`)
    );
}

async function fetchMcpServer(apiContext: APIRequestContext, mcpServerId: string): Promise<McpServerI> {
    const {embeddedMcpServers} = await graphql<{embeddedMcpServers: McpServerI[]}>(
        apiContext,
        'query EmbeddedMcpServers { embeddedMcpServers { id name secretKey url } }',
        {}
    );

    return embeddedMcpServers.find((mcpServer) => mcpServer.id === mcpServerId)!;
}

function recordResponseBodies(page: Page): string[] {
    const responseBodies: string[] = [];

    page.on('response', async (response: Response) => {
        const url = response.url();

        if (!url.includes('/api/') && !url.includes('/graphql')) {
            return;
        }

        try {
            responseBodies.push(await response.text());
        } catch {
            return;
        }
    });

    return responseBodies;
}

async function openMcpServerConnectTab(page: Page, mcpServerName: string): Promise<void> {
    await page.goto('/embedded/mcp-servers');

    await page.getByText(mcpServerName, {exact: true}).first().click({timeout: 30_000});

    await page.getByRole('tab', {name: 'Connect'}).first().click();
}

test.describe('Embedded credentials', () => {
    test.describe.configure({mode: 'serial'});

    let resources: CredentialResourcesI;

    test.beforeAll(async ({adminApi}) => {
        resources = await createCredentialResources(adminApi);
    });

    test.afterAll(async ({adminApi}) => {
        if (resources) {
            await deleteCredentialResources(adminApi, resources);
        }
    });

    test('a VIEWER gets connections, configurations and MCP servers without credentials', async ({
        adminApi,
        embeddedRoleUsers,
    }) => {
        const viewerApi = await createRoleUserApiContext(RBAC_BASE_URL, embeddedRoleUsers.VIEWER);

        try {
            await test.step('the tenant admin sees the stored credentials', async () => {
                const connection = await embeddedInternalApi<ConnectionI>(
                    adminApi,
                    'GET',
                    `/connections/${resources.connectionId}`
                );

                expect(JSON.stringify(connection)).toContain(CONNECTION_USERNAME_MARKER);

                const configuration = await embeddedInternalApi<ConfigurationI>(
                    adminApi,
                    'GET',
                    `/integration-instance-configurations/${resources.configurationId}`
                );

                expect(JSON.stringify(configuration)).toContain(CONFIGURATION_USERNAME_MARKER);
            });

            await test.step('connection reads carry no credential values', async () => {
                for (const path of [
                    `/connections/${resources.connectionId}`,
                    `/connections?environmentId=${ENVIRONMENT_IDS.DEVELOPMENT}`,
                ]) {
                    const body = JSON.stringify(await embeddedInternalApi(viewerApi, 'GET', path));

                    expect(body).toContain(resources.connectionName);

                    for (const secretMarker of SECRET_MARKERS) {
                        expect(body, `${path} leaks ${secretMarker}`).not.toContain(secretMarker);
                    }
                }

                const connection = await embeddedInternalApi<ConnectionI>(
                    viewerApi,
                    'GET',
                    `/connections/${resources.connectionId}`
                );

                expect(connection.authorizationParameters || {}).toEqual({});
                expect(connection.connectionParameters || {}).toEqual({});
                expect(connection.parameters || {}).toEqual({});
            });

            await test.step('instance configuration reads carry no credential values', async () => {
                for (const path of [
                    `/integration-instance-configurations/${resources.configurationId}`,
                    `/integration-instance-configurations?environmentId=${ENVIRONMENT_IDS.DEVELOPMENT}`,
                ]) {
                    const body = JSON.stringify(await embeddedInternalApi(viewerApi, 'GET', path));

                    expect(body).toContain(resources.configurationName);

                    for (const secretMarker of SECRET_MARKERS) {
                        expect(body, `${path} leaks ${secretMarker}`).not.toContain(secretMarker);
                    }
                }

                const configuration = await embeddedInternalApi<ConfigurationI>(
                    viewerApi,
                    'GET',
                    `/integration-instance-configurations/${resources.configurationId}`
                );

                expect(configuration.connectionAuthorizationParameters || {}).toEqual({});
                expect(configuration.connectionConnectionParameters || {}).toEqual({});
                expect(configuration.connectionParameters || {}).toEqual({});
            });

            await test.step('MCP server reads carry no secret key', async () => {
                const mcpServer = await fetchMcpServer(viewerApi, resources.mcpServerId);

                expect(mcpServer.name).toBe(resources.mcpServerName);
                expect(mcpServer.secretKey ?? null).toBeNull();
                expect(JSON.stringify(mcpServer)).not.toContain(resources.mcpServerSecretKey);
            });
        } finally {
            await viewerApi.dispose();
        }
    });

    test('an EDITOR gets the credentials and the MCP secret key', async ({embeddedRoleUsers}) => {
        const editorApi = await createRoleUserApiContext(RBAC_BASE_URL, embeddedRoleUsers.EDITOR);

        try {
            const connection = await embeddedInternalApi(editorApi, 'GET', `/connections/${resources.connectionId}`);

            expect(JSON.stringify(connection)).toContain(CONNECTION_USERNAME_MARKER);

            const configuration = await embeddedInternalApi(
                editorApi,
                'GET',
                `/integration-instance-configurations/${resources.configurationId}`
            );

            expect(JSON.stringify(configuration)).toContain(CONFIGURATION_USERNAME_MARKER);

            const mcpServer = await fetchMcpServer(editorApi, resources.mcpServerId);

            expect(mcpServer.secretKey).toBe(resources.mcpServerSecretKey);
            expect(mcpServer.url).toContain(resources.mcpServerSecretKey);
        } finally {
            await editorApi.dispose();
        }
    });

    test('a VIEWER browsing the pages receives no credential and sees the MCP URL as hidden', async ({
        browser,
        embeddedRoleUsers,
    }) => {
        const {context, page} = await openAsEmbeddedRoleUser(
            browser,
            RBAC_BASE_URL,
            embeddedRoleUsers.VIEWER,
            ENVIRONMENT_IDS.DEVELOPMENT
        );

        const responseBodies = recordResponseBodies(page);

        try {
            await test.step('connections page', async () => {
                await page.goto('/embedded/connections');

                await expect(page.getByText(resources.connectionName).first()).toBeVisible({timeout: 30_000});

                await expect(page.getByRole('button', {name: 'Connection actions'})).toHaveCount(0);
            });

            await test.step('instance configurations page', async () => {
                await page.goto('/embedded/configurations');

                await expect(page.getByText(resources.configurationName).first()).toBeVisible({timeout: 30_000});
            });

            await test.step('MCP server URL is hidden with its reason', async () => {
                await openMcpServerConnectTab(page, resources.mcpServerName);

                await expect(page.getByText(HIDDEN_MCP_SERVER_URL_HINT).first()).toBeVisible();
                await expect(page.getByRole('button', {name: 'Copy server URL'})).toHaveCount(0);
            });

            await page.waitForLoadState('networkidle');

            await test.step('no response carried a credential value or the MCP secret key', async () => {
                expect(responseBodies.length).toBeGreaterThan(0);

                for (const secretMarker of [...SECRET_MARKERS, resources.mcpServerSecretKey]) {
                    expect(
                        responseBodies.filter((responseBody) => responseBody.includes(secretMarker)),
                        `responses leaking ${secretMarker}`
                    ).toEqual([]);
                }
            });
        } finally {
            await context.close();
        }
    });

    test('an EDITOR sees the MCP server URL', async ({browser, embeddedRoleUsers}) => {
        const {context, page} = await openAsEmbeddedRoleUser(
            browser,
            RBAC_BASE_URL,
            embeddedRoleUsers.EDITOR,
            ENVIRONMENT_IDS.DEVELOPMENT
        );

        try {
            await openMcpServerConnectTab(page, resources.mcpServerName);

            await expect(page.getByRole('button', {name: 'Copy server URL'}).first()).toBeVisible();
            await expect(page.getByText(HIDDEN_MCP_SERVER_URL_HINT)).toHaveCount(0);
        } finally {
            await context.close();
        }
    });
});
