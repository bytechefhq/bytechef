import {AUTOMATION_SCOPE, MANAGEMENT_SCOPE, mcpAuthenticationTest as test} from '../../fixtures/mcpAuthentication';
import {BackendSession} from '../../utils/mcpAuthentication/backendSession';
import {
    MCP_TEST_CONFIGURATION,
    MEMBER_CREDENTIALS_MISSING_REASON,
    hasWorkspaceMembers,
} from '../../utils/mcpAuthentication/mcpTestConfiguration';
import {defineStatusCases} from '../../utils/mcpAuthentication/statusCases';

const {expect} = test;

const WORKSPACE_MCP_SERVERS = `query workspaceMcpServers($workspaceId: ID!) {
    workspaceMcpServers(workspaceId: $workspaceId) { id secretKey url }
}`;

const WORKSPACE_MCP_SERVER_URLS = `query workspaceMcpServerUrls($workspaceId: ID!) {
    workspaceMcpServers(workspaceId: $workspaceId) { id url }
}`;

const UPDATE_MCP_SERVER_URL = `mutation updateMcpServerUrl($id: ID!) {
    updateMcpServerUrl(id: $id)
}`;

interface WorkspaceMcpServerI {
    id: string;
    secretKey?: string | null;
    url: string;
}

test.describe('Automation MCP server authentication (#6139)', () => {
    defineStatusCases([
        {
            description: 'no credential',
            expectedStatus: 401,
            id: 'A1',
            url: (mcpEnvironment) => mcpEnvironment.automationMcpServers.requiredFirstWorkspace.url,
        },
        {
            credential: ({mcpEnvironment}) => ({bearerToken: mcpEnvironment.apiKeys.firstWorkspace}),
            description: 'W1 key on W1 server',
            expectedStatus: 200,
            id: 'A2',
            url: (mcpEnvironment) => mcpEnvironment.automationMcpServers.requiredFirstWorkspace.url,
        },
        {
            credential: ({mcpEnvironment}) => ({
                bearerToken: mcpEnvironment.apiKeys.firstWorkspace,
                environment: 'DEVELOPMENT',
            }),
            description: 'W1 key, X-ENVIRONMENT matches',
            expectedStatus: 200,
            id: 'A3',
            url: (mcpEnvironment) => mcpEnvironment.automationMcpServers.requiredFirstWorkspace.url,
        },
        {
            credential: ({mcpEnvironment}) => ({
                bearerToken: mcpEnvironment.apiKeys.firstWorkspace,
                environment: 'PRODUCTION',
            }),
            description: 'W1 key, X-ENVIRONMENT=PRODUCTION (server DEVELOPMENT)',
            expectedStatus: 401,
            id: 'A4',
            url: (mcpEnvironment) => mcpEnvironment.automationMcpServers.requiredFirstWorkspace.url,
        },
        {
            credential: ({mcpEnvironment}) => ({bearerToken: mcpEnvironment.apiKeys.firstWorkspace}),
            description: 'W1 key on W2 server',
            expectedStatus: 401,
            id: 'A5',
            url: (mcpEnvironment) => mcpEnvironment.automationMcpServers.requiredSecondWorkspace.url,
        },
        {
            credential: ({mcpEnvironment}) => ({bearerToken: mcpEnvironment.apiKeys.secondWorkspace}),
            description: 'W2 key on W2 server',
            expectedStatus: 200,
            id: 'A6',
            url: (mcpEnvironment) => mcpEnvironment.automationMcpServers.requiredSecondWorkspace.url,
        },
        {
            credential: ({mcpEnvironment}) => ({bearerToken: mcpEnvironment.apiKeys.firstWorkspaceProduction}),
            description: 'W1 PRODUCTION key on DEVELOPMENT server',
            expectedStatus: 401,
            id: 'A7',
            url: (mcpEnvironment) => mcpEnvironment.automationMcpServers.requiredFirstWorkspace.url,
        },
        {
            credential: ({mcpEnvironment}) => ({bearerToken: mcpEnvironment.apiKeys.admin}),
            description: 'admin key',
            expectedStatus: 401,
            id: 'A8',
            url: (mcpEnvironment) => mcpEnvironment.automationMcpServers.requiredFirstWorkspace.url,
        },
        {
            credential: ({mcpEnvironment}) => ({bearerToken: mcpEnvironment.apiKeys.embedded}),
            description: 'embedded-type key',
            expectedStatus: 401,
            id: 'A9',
            url: (mcpEnvironment) => mcpEnvironment.automationMcpServers.requiredFirstWorkspace.url,
        },
        {
            credential: ({mcpEnvironment}) => ({session: mcpEnvironment.adminSession}),
            description: 'UI session cookie',
            expectedStatus: 401,
            id: 'A10',
            url: (mcpEnvironment) => mcpEnvironment.automationMcpServers.requiredFirstWorkspace.url,
        },
        {
            credential: () => ({bearerToken: 'garbage'}),
            description: 'garbage bearer',
            expectedStatus: 401,
            id: 'A11',
            url: (mcpEnvironment) => mcpEnvironment.automationMcpServers.requiredFirstWorkspace.url,
        },
        {
            credential: async ({mcpEnvironment, oauth2Tokens}) => ({
                bearerToken: await oauth2Tokens.getAccessToken(
                    'admin',
                    mcpEnvironment.automationMcpServers.requiredFirstWorkspace.url,
                    AUTOMATION_SCOPE
                ),
            }),
            description: 'OAuth admin token',
            expectedStatus: 200,
            id: 'A12',
            url: (mcpEnvironment) => mcpEnvironment.automationMcpServers.requiredFirstWorkspace.url,
        },
        {
            credential: async ({mcpEnvironment, oauth2Tokens}) => ({
                bearerToken: await oauth2Tokens.getAccessToken(
                    'admin',
                    mcpEnvironment.automationMcpServers.requiredFirstWorkspace.url,
                    AUTOMATION_SCOPE
                ),
                environment: 'PRODUCTION',
            }),
            description: 'OAuth admin token, X-ENVIRONMENT=PRODUCTION',
            expectedStatus: 403,
            id: 'A13',
            url: (mcpEnvironment) => mcpEnvironment.automationMcpServers.requiredFirstWorkspace.url,
        },
        {
            credential: async ({mcpEnvironment, oauth2Tokens}) => ({
                bearerToken: await oauth2Tokens.getAccessToken(
                    'admin',
                    mcpEnvironment.automationMcpServers.requiredFirstWorkspace.url,
                    AUTOMATION_SCOPE
                ),
                environment: 'DEVELOPMENT',
            }),
            description: 'OAuth admin token, X-ENVIRONMENT matches',
            expectedStatus: 200,
            id: 'A14',
            url: (mcpEnvironment) => mcpEnvironment.automationMcpServers.requiredFirstWorkspace.url,
        },
        {
            credential: async ({mcpEnvironment, oauth2Tokens}) => ({
                bearerToken: await oauth2Tokens.getAccessToken(
                    'admin',
                    mcpEnvironment.automationMcpServers.requiredSecondWorkspace.url,
                    AUTOMATION_SCOPE
                ),
            }),
            description: 'OAuth admin token on W2 (tenant admin)',
            expectedStatus: 200,
            id: 'A15',
            url: (mcpEnvironment) => mcpEnvironment.automationMcpServers.requiredSecondWorkspace.url,
        },
        {
            credential: async ({mcpEnvironment, oauth2Tokens}) => ({
                bearerToken: await oauth2Tokens.getAccessToken(
                    'editor',
                    mcpEnvironment.automationMcpServers.requiredFirstWorkspace.url,
                    AUTOMATION_SCOPE
                ),
            }),
            description: 'OAuth W1 member on W1 server',
            expectedStatus: 200,
            id: 'A16',
            requiresWorkspaceMembers: true,
            url: (mcpEnvironment) => mcpEnvironment.automationMcpServers.requiredFirstWorkspace.url,
        },
        {
            credential: async ({mcpEnvironment, oauth2Tokens}) => ({
                bearerToken: await oauth2Tokens.getAccessToken(
                    'editor',
                    mcpEnvironment.automationMcpServers.requiredSecondWorkspace.url,
                    AUTOMATION_SCOPE
                ),
            }),
            description: 'OAuth W1 member on W2 server (not a member)',
            expectedStatus: 403,
            id: 'A17',
            requiresWorkspaceMembers: true,
            url: (mcpEnvironment) => mcpEnvironment.automationMcpServers.requiredSecondWorkspace.url,
        },
        {
            credential: async ({mcpEnvironment, oauth2Tokens}) => ({
                bearerToken: await oauth2Tokens.getAccessToken(
                    'admin',
                    mcpEnvironment.automationMcpServers.requiredFirstWorkspace.url,
                    AUTOMATION_SCOPE
                ),
            }),
            description: 'OAuth token issued for AR1 used on AR2',
            expectedStatus: 401,
            id: 'A18',
            url: (mcpEnvironment) => mcpEnvironment.automationMcpServers.requiredSecondWorkspace.url,
        },
        {
            credential: async ({mcpEnvironment, oauth2Tokens}) => ({
                bearerToken: await oauth2Tokens.getAccessToken(
                    'admin',
                    mcpEnvironment.managementMcpUrls.tenantLess,
                    MANAGEMENT_SCOPE
                ),
            }),
            description: 'OAuth mcp:management token on automation',
            expectedStatus: 403,
            id: 'A19',
            url: (mcpEnvironment) => mcpEnvironment.automationMcpServers.requiredFirstWorkspace.url,
        },
        {
            description: 'optional: no credential',
            expectedStatus: 200,
            id: 'B1',
            url: (mcpEnvironment) => mcpEnvironment.automationMcpServers.optionalFirstWorkspace.url,
        },
        {
            credential: () => ({environment: 'DEVELOPMENT'}),
            description: 'optional: no credential, X-ENVIRONMENT matches',
            expectedStatus: 200,
            id: 'B2',
            url: (mcpEnvironment) => mcpEnvironment.automationMcpServers.optionalFirstWorkspace.url,
        },
        {
            credential: () => ({environment: 'PRODUCTION'}),
            description: 'optional: no credential, X-ENVIRONMENT=PRODUCTION',
            expectedStatus: 401,
            id: 'B3',
            url: (mcpEnvironment) => mcpEnvironment.automationMcpServers.optionalFirstWorkspace.url,
        },
        {
            credential: ({mcpEnvironment}) => ({bearerToken: mcpEnvironment.apiKeys.firstWorkspace}),
            description: 'optional: W1 key',
            expectedStatus: 200,
            id: 'B4',
            url: (mcpEnvironment) => mcpEnvironment.automationMcpServers.optionalFirstWorkspace.url,
        },
        {
            credential: ({mcpEnvironment}) => ({session: mcpEnvironment.adminSession}),
            description: 'optional: UI session',
            expectedStatus: 200,
            id: 'B5',
            url: (mcpEnvironment) => mcpEnvironment.automationMcpServers.optionalFirstWorkspace.url,
        },
        {
            credential: () => ({bearerToken: 'garbage'}),
            description: 'optional: garbage bearer',
            expectedStatus: 401,
            id: 'B6',
            url: (mcpEnvironment) => mcpEnvironment.automationMcpServers.optionalFirstWorkspace.url,
        },
    ]);

    test.describe('server URL access', () => {
        test.describe.configure({mode: 'serial'});

        let editorSession: BackendSession | undefined;
        let viewerSession: BackendSession | undefined;

        test.beforeAll(async () => {
            if (!hasWorkspaceMembers()) {
                return;
            }

            editorSession = await BackendSession.signIn(MCP_TEST_CONFIGURATION.editor!);
            viewerSession = await BackendSession.signIn(MCP_TEST_CONFIGURATION.viewer!);
        });

        test.afterAll(async () => {
            await editorSession?.dispose();

            await viewerSession?.dispose();
        });

        test.beforeEach(() => {
            test.skip(!hasWorkspaceMembers(), MEMBER_CREDENTIALS_MISSING_REASON);
        });

        test('editor of W1: sees the secret key and the full URL of the W1 servers', async ({mcpEnvironment}) => {
            const data = await editorSession!.graphQlData<{workspaceMcpServers: WorkspaceMcpServerI[]}>(
                WORKSPACE_MCP_SERVERS,
                {workspaceId: mcpEnvironment.workspaceIds.first}
            );

            const {optionalFirstWorkspace, requiredFirstWorkspace} = mcpEnvironment.automationMcpServers;

            for (const expectedServer of [optionalFirstWorkspace, requiredFirstWorkspace]) {
                const listedServer = data.workspaceMcpServers.find((server) => server.id === expectedServer.id);

                expect(listedServer?.secretKey).toBe(expectedServer.secretKey);
                expect(listedServer?.url).toContain(`/api/automation/${expectedServer.secretKey}/mcp`);
            }
        });

        test('editor of W1: rotates the URL of a W1 server (secret changes)', async ({mcpEnvironment}) => {
            const rotationServer = mcpEnvironment.automationMcpServers.rotationFirstWorkspace;

            const rotation = await editorSession!.graphQl<{updateMcpServerUrl: string}>(UPDATE_MCP_SERVER_URL, {
                id: rotationServer.id,
            });

            expect(rotation.errors ?? []).toEqual([]);

            const data = await editorSession!.graphQlData<{workspaceMcpServers: WorkspaceMcpServerI[]}>(
                WORKSPACE_MCP_SERVERS,
                {workspaceId: mcpEnvironment.workspaceIds.first}
            );

            const rotatedServer = data.workspaceMcpServers.find((server) => server.id === rotationServer.id);

            expect(rotatedServer?.secretKey).toBeTruthy();
            expect(rotatedServer?.secretKey).not.toBe(rotationServer.secretKey);
        });

        test('editor of W1: rotating the URL of a W2 server is denied', async ({mcpEnvironment}) => {
            const rotation = await editorSession!.graphQl<{updateMcpServerUrl: string}>(UPDATE_MCP_SERVER_URL, {
                id: mcpEnvironment.automationMcpServers.requiredSecondWorkspace.id,
            });

            expect(rotation.errors?.length ?? 0).toBeGreaterThan(0);
        });

        test('viewer of W1: `secretKey` denied, `url` returned masked (`/api/automation/********/mcp`)', async ({
            mcpEnvironment,
        }) => {
            const withSecretKey = await viewerSession!.graphQl<{workspaceMcpServers: WorkspaceMcpServerI[]}>(
                WORKSPACE_MCP_SERVERS,
                {workspaceId: mcpEnvironment.workspaceIds.first}
            );

            expect(withSecretKey.errors?.length ?? 0).toBeGreaterThan(0);

            const urlsOnly = await viewerSession!.graphQlData<{workspaceMcpServers: WorkspaceMcpServerI[]}>(
                WORKSPACE_MCP_SERVER_URLS,
                {workspaceId: mcpEnvironment.workspaceIds.first}
            );

            const listedServer = urlsOnly.workspaceMcpServers.find(
                (server) => server.id === mcpEnvironment.automationMcpServers.requiredFirstWorkspace.id
            );

            expect(listedServer?.url).toContain('/api/automation/********/mcp');
        });

        test('viewer of W1: rotating a URL is denied', async ({mcpEnvironment}) => {
            const rotation = await viewerSession!.graphQl<{updateMcpServerUrl: string}>(UPDATE_MCP_SERVER_URL, {
                id: mcpEnvironment.automationMcpServers.requiredFirstWorkspace.id,
            });

            expect(rotation.errors?.length ?? 0).toBeGreaterThan(0);
        });
    });
});
