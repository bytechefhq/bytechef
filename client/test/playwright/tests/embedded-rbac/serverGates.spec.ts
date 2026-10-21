import {type APIRequestContext, expect, request as playwrightRequest} from '@playwright/test';
import {readFileSync} from 'fs';

import {type EmbeddedRoleUserKeyType, embeddedRolesTest as test} from '../../fixtures/embeddedRoles';
import {SAMPLE_WORKFLOW_PATH} from '../../utils/constants';
import {type EmbeddedDataI, embeddedInternalApi, signConnectedUserJwt} from '../../utils/embeddedRbacUtils';
import getRandomString from '../../utils/getRandomString';
import {RBAC_BASE_URL, createRoleUserApiContext, graphql, postGraphQl, sendInternalApi} from '../../utils/rbacUtils';

const EMBEDDED_INTERNAL = '/api/embedded/internal';

const GATE_PREFIX = `rbac_embedded_gate_${getRandomString()}`;

const GATE_CONNECTED_USER_EXTERNAL_ID = `rbac_gate_${getRandomString().slice(0, 8)}`;

type CallerType = Exclude<EmbeddedRoleUserKeyType, 'DEVELOPMENT_EDITOR' | 'PRODUCTION_EDITOR'>;

interface ResourcesI {
    appEventId: number;
    configurationId: number;
    connectedUserId: number;
    connectionId: number;
    connectedUserConnectionId: number;
    connectedUserScratchConnectionId: number;
    integrationId: number;
    integrationWorkflowId: number;
    mcpServerId: string;
    rotationMcpServerId: string;
    scratchConfigurationId: number;
    gateConnectedUserId?: number;
    gateSigningKeyId: number;
    scratchIntegrationId: number;
    scratchMcpServerId: string;
    viewerUserId: string;
    workflowDefinition: string;
    workflowId: string;
    workflowVersion: number;
}

interface GraphQlProbeI {
    query: string;
    variables: (resources: ResourcesI) => Record<string, unknown>;
}

interface RestProbeI {
    body?: (resources: ResourcesI) => unknown;
    method: 'DELETE' | 'GET' | 'PATCH' | 'POST' | 'PUT';
    path: (resources: ResourcesI) => string;
}

interface GateCaseI {
    allowed?: CallerType[];
    denied: CallerType[];
    graphQl?: GraphQlProbeI;
    module: string;
    name: string;
    rest?: RestProbeI;
}

interface IntegrationWorkflowI {
    __version: number;
    definition: string;
    id: string;
    integrationWorkflowId: number;
}

const GATE_CASES: GateCaseI[] = [
    {
        allowed: ['EDITOR'],
        denied: ['VIEWER', 'OUTSIDER'],
        module: 'Integration',
        name: 'create an integration (INTEGRATION_CREATE)',
        rest: {
            body: () => ({componentName: 'httpClient', multipleInstances: false, name: `${GATE_PREFIX}_integration`}),
            method: 'POST',
            path: () => `${EMBEDDED_INTERNAL}/integrations`,
        },
    },
    {
        allowed: ['VIEWER'],
        denied: ['OUTSIDER'],
        module: 'Integration',
        name: 'read an integration (WORKFLOW_VIEW)',
        rest: {method: 'GET', path: (resources) => `${EMBEDDED_INTERNAL}/integrations/${resources.integrationId}`},
    },
    {
        allowed: ['EDITOR'],
        denied: ['VIEWER'],
        module: 'Integration',
        name: 'publish an integration (INTEGRATION_PUBLISH)',
        rest: {
            body: () => ({description: 'rbac gate'}),
            method: 'POST',
            path: (resources) => `${EMBEDDED_INTERNAL}/integrations/${resources.scratchIntegrationId}/publish`,
        },
    },
    {
        allowed: ['ADMIN'],
        denied: ['VIEWER', 'EDITOR'],
        graphQl: {
            query: `mutation UpdateIntegrationWorkflowPermissionExpression($integrationWorkflowId: ID!) {
                updateIntegrationWorkflowPermissionExpression(
                    integrationWorkflowId: $integrationWorkflowId, permissionExpression: null
                ) { id }
            }`,
            variables: (resources) => ({integrationWorkflowId: String(resources.integrationWorkflowId)}),
        },
        module: 'Integration',
        name: 'change a workflow permission expression (INTEGRATION_SETTINGS)',
    },
    {
        allowed: ['ADMIN'],
        denied: ['VIEWER', 'EDITOR'],
        module: 'Integration',
        name: 'delete an integration (INTEGRATION_DELETE)',
        rest: {
            method: 'DELETE',
            path: (resources) => `${EMBEDDED_INTERNAL}/integrations/${resources.scratchIntegrationId}`,
        },
    },
    {
        allowed: ['EDITOR'],
        denied: ['VIEWER'],
        module: 'Workflow',
        name: 'add a workflow to an integration (WORKFLOW_CREATE)',
        rest: {
            body: () => ({definition: readFileSync(SAMPLE_WORKFLOW_PATH, 'utf-8')}),
            method: 'POST',
            path: (resources) => `${EMBEDDED_INTERNAL}/integrations/${resources.integrationId}/workflows`,
        },
    },
    {
        allowed: ['EDITOR'],
        denied: ['VIEWER'],
        module: 'Workflow',
        name: 'save a workflow in Development (WORKFLOW_EDIT)',
        rest: {
            body: (resources) => ({__version: resources.workflowVersion, definition: resources.workflowDefinition}),
            method: 'PUT',
            path: (resources) => `${EMBEDDED_INTERNAL}/workflows/${resources.workflowId}`,
        },
    },
    {
        allowed: ['VIEWER'],
        denied: ['OUTSIDER'],
        module: 'Configuration',
        name: 'list instance configurations (INTEGRATION_INSTANCE_CONFIGURATION_VIEW)',
        rest: {method: 'GET', path: () => `${EMBEDDED_INTERNAL}/integration-instance-configurations?environmentId=0`},
    },
    {
        allowed: ['EDITOR'],
        denied: ['VIEWER'],
        module: 'Configuration',
        name: 'create an instance configuration (INTEGRATION_INSTANCE_CONFIGURATION_CREATE)',
        rest: {
            body: (resources) => ({
                authorizationType: 'BASIC_AUTH',
                connectionParameters: {},
                enabled: false,
                environmentId: 1,
                integrationId: resources.integrationId,
                integrationVersion: 1,
                name: `${GATE_PREFIX}_configuration`,
            }),
            method: 'POST',
            path: () => `${EMBEDDED_INTERNAL}/integration-instance-configurations`,
        },
    },
    {
        allowed: ['EDITOR'],
        denied: ['VIEWER'],
        module: 'Configuration',
        name: 'change the tags of an instance configuration (INTEGRATION_INSTANCE_CONFIGURATION_EDIT)',
        rest: {
            body: () => ({tags: []}),
            method: 'PUT',
            path: (resources) =>
                `${EMBEDDED_INTERNAL}/integration-instance-configurations/${resources.configurationId}/tags`,
        },
    },
    {
        allowed: ['ADMIN'],
        denied: ['VIEWER', 'EDITOR'],
        module: 'Configuration',
        name: 'delete an instance configuration (INTEGRATION_INSTANCE_CONFIGURATION_DELETE)',
        rest: {
            method: 'DELETE',
            path: (resources) =>
                `${EMBEDDED_INTERNAL}/integration-instance-configurations/${resources.scratchConfigurationId}`,
        },
    },
    {
        allowed: ['VIEWER'],
        denied: ['OUTSIDER'],
        module: 'Connected user',
        name: 'list connected users (CONNECTED_USER_VIEW)',
        rest: {method: 'GET', path: () => `${EMBEDDED_INTERNAL}/connected-users?environmentId=0`},
    },
    {
        allowed: ['EDITOR'],
        denied: ['VIEWER'],
        module: 'Connected user',
        name: 'enable a connected user (CONNECTED_USER_MANAGE)',
        rest: {
            method: 'PATCH',
            path: (resources) => `${EMBEDDED_INTERNAL}/connected-users/${resources.connectedUserId}/enable/true`,
        },
    },
    {
        denied: ['VIEWER', 'EDITOR'],
        module: 'Connected user',
        name: 'delete a connected user (CONNECTED_USER_DELETE)',
        rest: {
            method: 'DELETE',
            path: (resources) => `${EMBEDDED_INTERNAL}/connected-users/${resources.connectedUserId}`,
        },
    },
    {
        allowed: ['VIEWER'],
        denied: ['OUTSIDER'],
        module: 'App event',
        name: 'list app events (APP_EVENT_VIEW)',
        rest: {method: 'GET', path: () => `${EMBEDDED_INTERNAL}/app-events`},
    },
    {
        allowed: ['EDITOR'],
        denied: ['VIEWER'],
        module: 'App event',
        name: 'create an app event (APP_EVENT_CREATE)',
        rest: {
            body: () => ({name: `${GATE_PREFIX}_app_event`, schema: '{}'}),
            method: 'POST',
            path: () => `${EMBEDDED_INTERNAL}/app-events`,
        },
    },
    {
        allowed: ['ADMIN'],
        denied: ['VIEWER', 'EDITOR'],
        module: 'App event',
        name: 'delete an app event (APP_EVENT_DELETE)',
        rest: {method: 'DELETE', path: (resources) => `${EMBEDDED_INTERNAL}/app-events/${resources.appEventId}`},
    },
    {
        allowed: ['VIEWER'],
        denied: ['OUTSIDER'],
        module: 'Execution',
        name: 'list runs (EXECUTION_VIEW)',
        rest: {method: 'GET', path: () => `${EMBEDDED_INTERNAL}/workflow-executions?environmentId=0`},
    },
    {
        allowed: ['VIEWER'],
        denied: ['OUTSIDER'],
        module: 'Connection',
        name: 'list connections (CONNECTION_VIEW)',
        rest: {method: 'GET', path: () => `${EMBEDDED_INTERNAL}/connections?environmentId=0`},
    },
    {
        allowed: ['EDITOR'],
        denied: ['VIEWER'],
        module: 'Connection',
        name: 'create a connection (CONNECTION_CREATE)',
        rest: {
            body: () => ({
                componentName: 'httpClient',
                connectionVersion: 1,
                environmentId: 0,
                name: `${GATE_PREFIX}_connection`,
                parameters: {},
                shared: true,
            }),
            method: 'POST',
            path: () => `${EMBEDDED_INTERNAL}/connections`,
        },
    },
    {
        allowed: ['EDITOR'],
        denied: ['VIEWER'],
        module: 'Connection',
        name: 'change the tags of a connection a connected user created (CONNECTION_EDIT)',
        rest: {
            body: () => ({tags: []}),
            method: 'PUT',
            path: (resources) => `${EMBEDDED_INTERNAL}/connections/${resources.connectedUserConnectionId}/tags`,
        },
    },
    {
        allowed: ['EDITOR'],
        denied: ['VIEWER'],
        module: 'Connection',
        name: 'delete a connection a connected user created (CONNECTION_DELETE)',
        rest: {
            method: 'DELETE',
            path: (resources) => `${EMBEDDED_INTERNAL}/connections/${resources.connectedUserScratchConnectionId}`,
        },
    },
    {
        allowed: ['VIEWER'],
        denied: ['OUTSIDER'],
        graphQl: {query: 'query EmbeddedMcpServers { embeddedMcpServers { id } }', variables: () => ({})},
        module: 'MCP',
        name: 'list MCP servers (MCP_VIEW)',
    },
    {
        allowed: ['EDITOR'],
        denied: ['VIEWER'],
        graphQl: {
            query: `mutation CreateEmbeddedMcpServer($input: CreateEmbeddedMcpServerInput!) {
                createEmbeddedMcpServer(input: $input) { id }
            }`,
            variables: () => ({input: {enabled: false, environmentId: '0', name: `${GATE_PREFIX}_mcp`}}),
        },
        module: 'MCP',
        name: 'create an MCP server (MCP_CREATE)',
    },
    {
        allowed: ['EDITOR'],
        denied: ['VIEWER'],
        graphQl: {
            query: `mutation UpdateEmbeddedMcpServerTags($id: ID!) {
                updateEmbeddedMcpServerTags(id: $id, tags: []) { id }
            }`,
            variables: (resources) => ({id: resources.mcpServerId}),
        },
        module: 'MCP',
        name: 'change the tags of an MCP server (MCP_EDIT)',
    },
    {
        allowed: ['ADMIN'],
        denied: ['VIEWER', 'EDITOR'],
        graphQl: {
            query: 'mutation DeleteEmbeddedMcpServer($id: ID!) { deleteEmbeddedMcpServer(mcpServerId: $id) }',
            variables: (resources) => ({id: resources.scratchMcpServerId}),
        },
        module: 'MCP',
        name: 'delete an MCP server (MCP_DELETE)',
    },
    {
        allowed: ['ADMIN'],
        denied: ['VIEWER', 'EDITOR'],
        module: 'Signing key',
        name: 'list signing keys (EMBEDDED_SETTINGS_MANAGE)',
        rest: {method: 'GET', path: () => `${EMBEDDED_INTERNAL}/signing-keys?environmentId=0`},
    },
    {
        allowed: ['EDITOR'],
        denied: ['VIEWER', 'OUTSIDER'],
        graphQl: {
            query: 'query EmbeddedApiKeys { embeddedApiKeys(environmentId: "0") { id } }',
            variables: () => ({}),
        },
        module: 'API key',
        name: 'list API keys (API_KEY_VIEW)',
    },
    {
        allowed: ['EDITOR'],
        denied: ['VIEWER'],
        graphQl: {
            query: `mutation CreateEmbeddedApiKey($input: CreateEmbeddedApiKeyInput!) {
                createEmbeddedApiKey(input: $input)
            }`,
            variables: () => ({input: {environmentId: '0', name: `${GATE_PREFIX}_api_key`}}),
        },
        module: 'API key',
        name: 'create an API key (API_KEY_CREATE)',
    },
    {
        allowed: ['VIEWER'],
        denied: ['OUTSIDER'],
        graphQl: {query: 'query EmbeddedUsers { embeddedUsers { userId } }', variables: () => ({})},
        module: 'Member',
        name: 'list members (EMBEDDED_VIEW)',
    },
    {
        allowed: ['ADMIN'],
        denied: ['VIEWER', 'EDITOR'],
        graphQl: {
            query: `mutation UpdateEmbeddedUserRole($userId: ID!) {
                updateEmbeddedUserRole(userId: $userId, role: VIEWER) { userId }
            }`,
            variables: (resources) => ({userId: resources.viewerUserId}),
        },
        module: 'Member',
        name: 'change a member role (EMBEDDED_MEMBER_MANAGE)',
    },
];

async function expectGraphQlDenied(apiContext: APIRequestContext, probe: GraphQlProbeI, resources: ResourcesI) {
    const response = await postGraphQl(apiContext, probe.query, probe.variables(resources));

    expect(response.errors, JSON.stringify(response)).toBeDefined();
    expect(response.errors![0]).toMatchObject({
        extensions: {classification: 'FORBIDDEN', errorCode: 'ACCESS_DENIED'},
        message: 'Access denied',
    });
}

async function expectGraphQlAllowed(apiContext: APIRequestContext, probe: GraphQlProbeI, resources: ResourcesI) {
    const response = await postGraphQl(apiContext, probe.query, probe.variables(resources));

    expect(response.errors, JSON.stringify(response.errors)).toBeUndefined();
}

async function expectRestStatus(
    apiContext: APIRequestContext,
    probe: RestProbeI,
    resources: ResourcesI,
    denied: boolean
) {
    const response = await sendInternalApi(apiContext, probe.method, probe.path(resources), probe.body?.(resources));

    if (denied) {
        expect(response.status(), await response.text()).toBe(403);
        expect(await response.json()).toMatchObject({detail: 'Access denied', status: 403, title: 'Forbidden'});
    } else {
        expect(response.status(), await response.text()).toBeLessThan(300);
    }
}

async function createMcpServer(adminApi: APIRequestContext, name: string): Promise<string> {
    const {createEmbeddedMcpServer} = await graphql<{createEmbeddedMcpServer: {id: string}}>(
        adminApi,
        `
            mutation CreateEmbeddedMcpServer($input: CreateEmbeddedMcpServerInput!) {
                createEmbeddedMcpServer(input: $input) {
                    id
                }
            }
        `,
        {input: {enabled: false, environmentId: '0', name}}
    );

    return createEmbeddedMcpServer.id;
}

interface GateConnectedUserI {
    connectedUserConnectionId: number;
    connectedUserScratchConnectionId: number;
    gateSigningKeyId: number;
}

async function createConnectedUserConnections(adminApi: APIRequestContext): Promise<GateConnectedUserI> {
    const signingKeyName = `${GATE_PREFIX}_signing_key`;

    const {privateKey} = await embeddedInternalApi<{privateKey: string}>(adminApi, 'POST', '/signing-keys', {
        environmentId: 0,
        name: signingKeyName,
    });

    const signingKeys = await embeddedInternalApi<{id: number; keyId: string; name: string}[]>(
        adminApi,
        'GET',
        '/signing-keys?environmentId=0'
    );

    const signingKey = signingKeys.find((candidate) => candidate.name === signingKeyName)!;

    const connectedUserApi = await playwrightRequest.newContext({
        baseURL: RBAC_BASE_URL,
        extraHTTPHeaders: {
            Authorization: `Bearer ${signConnectedUserJwt(signingKey.keyId, privateKey, GATE_CONNECTED_USER_EXTERNAL_ID)}`,
            'X-Environment': 'DEVELOPMENT',
        },
    });

    try {
        const createConnectedUserConnection = async (name: string): Promise<number> => {
            const response = await connectedUserApi.post('/api/embedded/v1/components/httpClient/connections', {
                data: {connectionVersion: 1, name, parameters: {}},
            });

            expect(response.status(), await response.text()).toBe(200);

            return Number(await response.text());
        };

        return {
            connectedUserConnectionId: await createConnectedUserConnection(`${GATE_PREFIX}_connected_user_connection`),
            connectedUserScratchConnectionId: await createConnectedUserConnection(
                `${GATE_PREFIX}_connected_user_scratch_connection`
            ),
            gateSigningKeyId: signingKey.id,
        };
    } finally {
        await connectedUserApi.dispose();
    }
}

async function createResources(
    adminApi: APIRequestContext,
    embeddedData: EmbeddedDataI,
    viewerUserId: string
): Promise<ResourcesI> {
    const developmentData = embeddedData.byEnvironment.DEVELOPMENT;

    const integrationWorkflows = await embeddedInternalApi<IntegrationWorkflowI[]>(
        adminApi,
        'GET',
        `/integrations/${embeddedData.integrationId}/workflows`
    );

    const integrationWorkflow = integrationWorkflows.find((workflow) => workflow.id === embeddedData.workflowUuid)!;

    const scratchIntegrationId = await embeddedInternalApi<number>(adminApi, 'POST', '/integrations', {
        componentName: 'httpClient',
        multipleInstances: false,
        name: `${GATE_PREFIX}_scratch_integration`,
    });

    await embeddedInternalApi(adminApi, 'POST', `/integrations/${scratchIntegrationId}/workflows`, {
        definition: readFileSync(SAMPLE_WORKFLOW_PATH, 'utf-8'),
    });

    const configurationIntegrationId = await embeddedInternalApi<number>(adminApi, 'POST', '/integrations', {
        componentName: 'httpClient',
        multipleInstances: false,
        name: `${GATE_PREFIX}_configuration_integration`,
    });

    await embeddedInternalApi(adminApi, 'POST', `/integrations/${configurationIntegrationId}/workflows`, {
        definition: readFileSync(SAMPLE_WORKFLOW_PATH, 'utf-8'),
    });

    await embeddedInternalApi(adminApi, 'POST', `/integrations/${configurationIntegrationId}/publish`, {
        description: 'rbac gates',
    });

    const scratchConfigurationId = await embeddedInternalApi<number>(
        adminApi,
        'POST',
        '/integration-instance-configurations',
        {
            authorizationType: 'BASIC_AUTH',
            connectionParameters: {},
            enabled: false,
            environmentId: 0,
            integrationId: configurationIntegrationId,
            integrationVersion: 1,
            name: `${GATE_PREFIX}_scratch_configuration`,
        }
    );

    const gateConnectedUser = await createConnectedUserConnections(adminApi);

    const connectedUsers = await embeddedInternalApi<{content: {externalId: string; id: number}[]}>(
        adminApi,
        'GET',
        `/connected-users?environmentId=0&search=${GATE_CONNECTED_USER_EXTERNAL_ID}`
    );

    const appEventId = await embeddedInternalApi<number>(adminApi, 'POST', '/app-events', {
        name: `${GATE_PREFIX}_scratch_app_event`,
        schema: '{}',
    });

    return {
        appEventId,
        configurationId: developmentData.instanceConfigurationId,
        connectedUserConnectionId: gateConnectedUser.connectedUserConnectionId,
        connectedUserId: developmentData.connectedUserId,
        connectedUserScratchConnectionId: gateConnectedUser.connectedUserScratchConnectionId,
        connectionId: developmentData.connectionId,
        gateConnectedUserId: connectedUsers.content.find(
            (connectedUser) => connectedUser.externalId === GATE_CONNECTED_USER_EXTERNAL_ID
        )?.id,
        gateSigningKeyId: gateConnectedUser.gateSigningKeyId,
        integrationId: embeddedData.integrationId,
        integrationWorkflowId: integrationWorkflow.integrationWorkflowId,
        mcpServerId: developmentData.mcpServerId,
        rotationMcpServerId: await createMcpServer(adminApi, `${GATE_PREFIX}_rotation_mcp`),
        scratchConfigurationId,
        scratchIntegrationId,
        scratchMcpServerId: await createMcpServer(adminApi, `${GATE_PREFIX}_scratch_mcp`),
        viewerUserId,
        workflowDefinition: integrationWorkflow.definition,
        workflowId: integrationWorkflow.id,
        workflowVersion: integrationWorkflow.__version,
    };
}

async function deleteGateResources(
    adminApi: APIRequestContext,
    editorApi: APIRequestContext | undefined,
    resources: ResourcesI | undefined
) {
    const isGateResource = (name: string | null | undefined) => !!name?.startsWith(GATE_PREFIX);

    const attempt = async (description: string, action: () => Promise<unknown>) => {
        try {
            await action();
        } catch (error) {
            console.warn(`embedded serverGates teardown: ${description}: ${error}`);
        }
    };

    for (const environmentId of [0, 1, 2]) {
        const configurations = await embeddedInternalApi<{id: number; name: string}[]>(
            adminApi,
            'GET',
            `/integration-instance-configurations?environmentId=${environmentId}`
        );

        for (const configuration of configurations.filter((candidate) => isGateResource(candidate.name))) {
            await attempt(`delete configuration ${configuration.id}`, () =>
                embeddedInternalApi(adminApi, 'DELETE', `/integration-instance-configurations/${configuration.id}`)
            );
        }

        const connections = await embeddedInternalApi<{id: number; name: string}[]>(
            adminApi,
            'GET',
            `/connections?environmentId=${environmentId}`
        );

        for (const connection of connections.filter((candidate) => isGateResource(candidate.name))) {
            await attempt(`delete connection ${connection.id}`, () =>
                embeddedInternalApi(adminApi, 'DELETE', `/connections/${connection.id}`)
            );
        }
    }

    const integrations = await embeddedInternalApi<{id: number; name: string}[]>(adminApi, 'GET', '/integrations');

    for (const integration of integrations.filter((candidate) => isGateResource(candidate.name))) {
        await attempt(`delete integration ${integration.id}`, () =>
            embeddedInternalApi(adminApi, 'DELETE', `/integrations/${integration.id}`)
        );
    }

    const appEvents = await embeddedInternalApi<{id: number; name: string}[]>(adminApi, 'GET', '/app-events');

    for (const appEvent of appEvents.filter((candidate) => isGateResource(candidate.name))) {
        await attempt(`delete app event ${appEvent.id}`, () =>
            embeddedInternalApi(adminApi, 'DELETE', `/app-events/${appEvent.id}`)
        );
    }

    const {embeddedMcpServers} = await graphql<{embeddedMcpServers: {id: string; name: string}[]}>(
        adminApi,
        'query EmbeddedMcpServers { embeddedMcpServers { id name } }',
        {}
    );

    for (const mcpServer of embeddedMcpServers.filter((candidate) => isGateResource(candidate.name))) {
        await attempt(`delete MCP server ${mcpServer.id}`, () =>
            graphql(
                adminApi,
                'mutation DeleteEmbeddedMcpServer($id: ID!) { deleteEmbeddedMcpServer(mcpServerId: $id) }',
                {
                    id: mcpServer.id,
                }
            )
        );
    }

    if (resources?.gateConnectedUserId) {
        const {gateConnectedUserId} = resources;

        await attempt(`delete connected user ${gateConnectedUserId}`, () =>
            embeddedInternalApi(adminApi, 'DELETE', `/connected-users/${gateConnectedUserId}`)
        );
    }

    if (resources?.gateSigningKeyId) {
        const {gateSigningKeyId} = resources;

        await attempt(`delete signing key ${gateSigningKeyId}`, () =>
            embeddedInternalApi(adminApi, 'DELETE', `/signing-keys/${gateSigningKeyId}`)
        );
    }

    if (editorApi) {
        const {embeddedApiKeys} = await graphql<{embeddedApiKeys: {id: string; name: string}[]}>(
            editorApi,
            'query EmbeddedApiKeys { embeddedApiKeys(environmentId: "0") { id name } }',
            {}
        );

        for (const apiKey of embeddedApiKeys.filter((candidate) => isGateResource(candidate.name))) {
            await attempt(`delete API key ${apiKey.id}`, () =>
                graphql(editorApi, 'mutation DeleteEmbeddedApiKey($id: ID!) { deleteEmbeddedApiKey(id: $id) }', {
                    id: apiKey.id,
                })
            );
        }
    }
}

test.describe('Embedded server-side permission gates', () => {
    test.describe.configure({mode: 'serial'});

    const callers: Partial<Record<EmbeddedRoleUserKeyType, APIRequestContext>> = {};

    let resources: ResourcesI;

    test.beforeAll(async ({adminApi, embeddedData, embeddedRoleUsers}) => {
        for (const [key, roleUser] of Object.entries(embeddedRoleUsers)) {
            callers[key as EmbeddedRoleUserKeyType] = await createRoleUserApiContext(RBAC_BASE_URL, roleUser);
        }

        resources = await createResources(adminApi, embeddedData, embeddedRoleUsers.VIEWER.userId!);
    });

    test.afterAll(async ({adminApi}) => {
        await deleteGateResources(adminApi, callers.EDITOR, resources).catch((error: unknown) => {
            console.warn(`embedded serverGates teardown failed: ${error}`);
        });

        for (const caller of Object.values(callers)) {
            await caller?.dispose();
        }
    });

    for (const gateCase of GATE_CASES) {
        test(`${gateCase.module}: ${gateCase.name} is refused for ${gateCase.denied.join(', ')}${
            gateCase.allowed ? ` and allowed for ${gateCase.allowed.join(', ')}` : ''
        }`, async () => {
            for (const caller of gateCase.denied) {
                await test.step(`${caller} is refused`, async () => {
                    if (gateCase.graphQl) {
                        await expectGraphQlDenied(callers[caller]!, gateCase.graphQl, resources);
                    } else {
                        await expectRestStatus(callers[caller]!, gateCase.rest!, resources, true);
                    }
                });
            }

            for (const caller of gateCase.allowed || []) {
                await test.step(`${caller} is allowed`, async () => {
                    if (gateCase.graphQl) {
                        await expectGraphQlAllowed(callers[caller]!, gateCase.graphQl, resources);
                    } else {
                        await expectRestStatus(callers[caller]!, gateCase.rest!, resources, false);
                    }
                });
            }
        });
    }

    test('Environment: a Production-only EDITOR is refused on Development and allowed on Production', async ({
        embeddedData,
    }) => {
        const productionEditor = callers.PRODUCTION_EDITOR!;

        const configurationTagsProbe = (configurationId: number): RestProbeI => ({
            body: () => ({tags: []}),
            method: 'PUT',
            path: () => `${EMBEDDED_INTERNAL}/integration-instance-configurations/${configurationId}/tags`,
        });

        await test.step('Development instance configuration is refused', async () => {
            await expectRestStatus(
                productionEditor,
                configurationTagsProbe(embeddedData.byEnvironment.DEVELOPMENT.instanceConfigurationId),
                resources,
                true
            );
        });

        await test.step('Production instance configuration is allowed', async () => {
            await expectRestStatus(
                productionEditor,
                configurationTagsProbe(embeddedData.byEnvironment.PRODUCTION.instanceConfigurationId),
                resources,
                false
            );
        });

        await test.step('Development connections are refused, Production connections are listed', async () => {
            await expectRestStatus(
                productionEditor,
                {method: 'GET', path: () => `${EMBEDDED_INTERNAL}/connections?environmentId=0`},
                resources,
                true
            );

            await expectRestStatus(
                productionEditor,
                {method: 'GET', path: () => `${EMBEDDED_INTERNAL}/connections?environmentId=2`},
                resources,
                false
            );
        });

        await test.step('saving a workflow, which is checked in Development, is refused', async () => {
            await expectRestStatus(
                productionEditor,
                {
                    body: () => ({__version: resources.workflowVersion, definition: resources.workflowDefinition}),
                    method: 'PUT',
                    path: () => `${EMBEDDED_INTERNAL}/workflows/${resources.workflowId}`,
                },
                resources,
                true
            );
        });

        await test.step('a Development-only EDITOR is refused on the Production instance configuration', async () => {
            await expectRestStatus(
                callers.DEVELOPMENT_EDITOR!,
                configurationTagsProbe(embeddedData.byEnvironment.PRODUCTION.instanceConfigurationId),
                resources,
                true
            );
        });
    });

    test('tenant admin only: rotating an MCP server URL is refused for an embedded ADMIN and allowed for a tenant admin', async ({
        adminApi,
    }) => {
        const rotationProbe: GraphQlProbeI = {
            query: 'mutation UpdateEmbeddedMcpServerUrl($id: ID!) { updateEmbeddedMcpServerUrl(id: $id) { id } }',
            variables: () => ({id: resources.rotationMcpServerId}),
        };

        await expectGraphQlDenied(callers.ADMIN!, rotationProbe, resources);

        await expectGraphQlAllowed(adminApi, rotationProbe, resources);
    });
});
