import {mcpAuthenticationTest as test} from '../../fixtures/mcpAuthentication';
import {BackendSession} from '../../utils/mcpAuthentication/backendSession';
import {
    ENVIRONMENT_IDS,
    MCP_TEST_CONFIGURATION,
    MEMBER_CREDENTIALS_MISSING_REASON,
    hasWorkspaceMembers,
} from '../../utils/mcpAuthentication/mcpTestConfiguration';
import {defineStatusCases} from '../../utils/mcpAuthentication/statusCases';

const {expect} = test;

const A2A_SERVERS = `query a2aServers($workspaceId: ID!) {
    a2aServers(workspaceId: $workspaceId) { id name secretKey }
}`;

const A2A_SERVER_NAMES = `query a2aServerNames($workspaceId: ID!) {
    a2aServers(workspaceId: $workspaceId) { id name }
}`;

interface A2aServerI {
    id: string;
    name: string;
    secretKey?: string | null;
}

test.describe('A2A server authentication (#6140)', () => {
    defineStatusCases([
        {
            description: 'no credential',
            expectedStatus: 401,
            id: 'C1',
            protocol: 'a2a',
            url: (mcpEnvironment) => mcpEnvironment.a2aServers.requiredFirstWorkspace.url,
        },
        {
            credential: ({mcpEnvironment}) => ({bearerToken: mcpEnvironment.apiKeys.firstWorkspace}),
            description: 'W1 key on W1 server',
            expectedStatus: 200,
            id: 'C2',
            protocol: 'a2a',
            url: (mcpEnvironment) => mcpEnvironment.a2aServers.requiredFirstWorkspace.url,
        },
        {
            credential: ({mcpEnvironment}) => ({bearerToken: mcpEnvironment.apiKeys.firstWorkspace}),
            description: 'W1 key on W2 server',
            expectedStatus: 401,
            id: 'C3',
            protocol: 'a2a',
            url: (mcpEnvironment) => mcpEnvironment.a2aServers.requiredSecondWorkspace.url,
        },
        {
            credential: ({mcpEnvironment}) => ({bearerToken: mcpEnvironment.apiKeys.secondWorkspace}),
            description: 'W2 key on W2 server',
            expectedStatus: 200,
            id: 'C4',
            protocol: 'a2a',
            url: (mcpEnvironment) => mcpEnvironment.a2aServers.requiredSecondWorkspace.url,
        },
        {
            credential: ({mcpEnvironment}) => ({
                bearerToken: mcpEnvironment.apiKeys.firstWorkspace,
                environment: 'PRODUCTION',
            }),
            description: 'W1 key, X-ENVIRONMENT=PRODUCTION',
            expectedStatus: 401,
            id: 'C5',
            protocol: 'a2a',
            url: (mcpEnvironment) => mcpEnvironment.a2aServers.requiredFirstWorkspace.url,
        },
        {
            credential: ({mcpEnvironment}) => ({
                bearerToken: mcpEnvironment.apiKeys.firstWorkspace,
                environment: 'DEVELOPMENT',
            }),
            description: 'W1 key, X-ENVIRONMENT matches',
            expectedStatus: 200,
            id: 'C6',
            protocol: 'a2a',
            url: (mcpEnvironment) => mcpEnvironment.a2aServers.requiredFirstWorkspace.url,
        },
        {
            credential: ({mcpEnvironment}) => ({bearerToken: mcpEnvironment.apiKeys.firstWorkspaceProduction}),
            description: 'W1 PRODUCTION key on DEVELOPMENT server',
            expectedStatus: 401,
            id: 'C7',
            protocol: 'a2a',
            url: (mcpEnvironment) => mcpEnvironment.a2aServers.requiredFirstWorkspace.url,
        },
        {
            credential: ({mcpEnvironment}) => ({bearerToken: mcpEnvironment.apiKeys.admin}),
            description: 'admin key',
            expectedStatus: 401,
            id: 'C8',
            protocol: 'a2a',
            url: (mcpEnvironment) => mcpEnvironment.a2aServers.requiredFirstWorkspace.url,
        },
        {
            description: 'optional: no credential',
            expectedStatus: 200,
            id: 'C9',
            protocol: 'a2a',
            url: (mcpEnvironment) => mcpEnvironment.a2aServers.optionalFirstWorkspace.url,
        },
        {
            credential: () => ({environment: 'PRODUCTION'}),
            description: 'optional: no credential, X-ENVIRONMENT=PRODUCTION',
            expectedStatus: 401,
            id: 'C10',
            protocol: 'a2a',
            url: (mcpEnvironment) => mcpEnvironment.a2aServers.optionalFirstWorkspace.url,
        },
    ]);

    test.describe('workspaces', () => {
        test('a2a: workspace 1 lists only its servers', async ({mcpEnvironment}) => {
            const data = await mcpEnvironment.adminSession.graphQlData<{a2aServers: A2aServerI[]}>(A2A_SERVER_NAMES, {
                workspaceId: mcpEnvironment.workspaceIds.first,
            });

            const listedIds = data.a2aServers.map((server) => server.id);
            const {optionalFirstWorkspace, requiredFirstWorkspace, requiredSecondWorkspace} = mcpEnvironment.a2aServers;

            expect(listedIds).toEqual(expect.arrayContaining([optionalFirstWorkspace.id, requiredFirstWorkspace.id]));
            expect(listedIds).not.toContain(requiredSecondWorkspace.id);
        });

        test('a2a: workspace 2 lists only its servers', async ({mcpEnvironment}) => {
            const data = await mcpEnvironment.adminSession.graphQlData<{a2aServers: A2aServerI[]}>(A2A_SERVER_NAMES, {
                workspaceId: mcpEnvironment.workspaceIds.second,
            });

            expect(data.a2aServers.map((server) => server.id)).toEqual([
                mcpEnvironment.a2aServers.requiredSecondWorkspace.id,
            ]);
        });
    });

    test.describe('workspace roles', () => {
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

        test('editor of W1: sees the secret keys of W1 A2A servers', async ({mcpEnvironment}) => {
            const data = await editorSession!.graphQlData<{a2aServers: A2aServerI[]}>(A2A_SERVERS, {
                workspaceId: mcpEnvironment.workspaceIds.first,
            });

            const {optionalFirstWorkspace, requiredFirstWorkspace} = mcpEnvironment.a2aServers;

            for (const expectedServer of [optionalFirstWorkspace, requiredFirstWorkspace]) {
                const listedServer = data.a2aServers.find((server) => server.id === expectedServer.id);

                expect(listedServer?.secretKey).toBe(expectedServer.secretKey);
            }
        });

        test('editor of W1: reading the secret key of a W2 A2A server is denied', async ({mcpEnvironment}) => {
            const response = await editorSession!.graphQl<{a2aServers: A2aServerI[]}>(A2A_SERVERS, {
                workspaceId: mcpEnvironment.workspaceIds.second,
            });

            expect(response.errors?.length ?? 0).toBeGreaterThan(0);
            expect(JSON.stringify(response.data ?? {})).not.toContain(
                mcpEnvironment.a2aServers.requiredSecondWorkspace.secretKey
            );
        });

        test('viewer of W1: lists W1 A2A servers with no secret key', async ({mcpEnvironment}) => {
            const data = await viewerSession!.graphQlData<{a2aServers: A2aServerI[]}>(A2A_SERVERS, {
                workspaceId: mcpEnvironment.workspaceIds.first,
            });

            const listedServer = data.a2aServers.find(
                (server) => server.id === mcpEnvironment.a2aServers.requiredFirstWorkspace.id
            );

            expect(listedServer).toBeDefined();
            expect(listedServer?.secretKey ?? null).toBeNull();
        });

        test('viewer of W1: creating an A2A server is denied', async ({mcpEnvironment}) => {
            const response = await viewerSession!.graphQl<{createA2aServer: A2aServerI | null}>(
                `mutation createA2aServer($input: CreateA2aServerInput!) {
                    createA2aServer(input: $input) { id }
                }`,
                {
                    input: {
                        environmentId: String(ENVIRONMENT_IDS.DEVELOPMENT),
                        name: `${mcpEnvironment.namePrefix}-viewer-created`,
                        workspaceId: mcpEnvironment.workspaceIds.first,
                    },
                }
            );

            expect(response.errors?.length ?? 0).toBeGreaterThan(0);
            expect(response.data?.createA2aServer ?? null).toBeNull();
        });
    });
});
