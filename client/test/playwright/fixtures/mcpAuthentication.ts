/* eslint-disable react-hooks/rules-of-hooks -- Playwright fixtures use 'use' callback, not React hooks */
import {
    type APIRequest,
    type APIResponse,
    type Browser,
    type BrowserContext,
    type Page,
    test as base,
} from '@playwright/test';

import getRandomString from '../utils/getRandomString';
import {login} from '../utils/login';
import {BackendSession} from '../utils/mcpAuthentication/backendSession';
import {
    createUnsignedConnectedUserJwt,
    generateUnrelatedPrivateKeyPem,
    signConnectedUserJwt,
} from '../utils/mcpAuthentication/connectedUserJwt';
import {postMcpInitialize} from '../utils/mcpAuthentication/endpointRequests';
import {
    ENVIRONMENT_IDS,
    MCP_TEST_CONFIGURATION,
    type UserCredentialsI,
    hasWorkspaceMembers,
} from '../utils/mcpAuthentication/mcpTestConfiguration';
import {type OAuth2TokensI, REGISTERED_CLIENT_IDS, obtainAccessToken} from '../utils/mcpAuthentication/oauth2Client';

export const MANAGEMENT_SCOPE = 'mcp:management';

export const AUTOMATION_SCOPE = 'mcp:automation';

export interface ServerEndpointI {
    id: string;
    secretKey: string;
    url: string;
}

export interface McpAuthenticationEnvironmentI {
    a2aServers: {
        optionalFirstWorkspace: ServerEndpointI;
        requiredFirstWorkspace: ServerEndpointI;
        requiredSecondWorkspace: ServerEndpointI;
    };
    adminSession: BackendSession;
    apiKeys: {
        admin: string;
        embedded: string;
        firstWorkspace: string;
        firstWorkspaceProduction: string;
        secondWorkspace: string;
    };
    automationMcpServers: {
        optionalFirstWorkspace: ServerEndpointI;
        requiredFirstWorkspace: ServerEndpointI;
        requiredSecondWorkspace: ServerEndpointI;
        rotationFirstWorkspace: ServerEndpointI;
    };
    connectedUserJwts: {
        developmentSigned: string;
        disabledConnectedUser: string;
        stagingSigned: string;
        unrelatedKeySigned: string;
        unsigned: string;
    };
    embeddedMcpServers: {
        disabled: ServerEndpointI;
        optional: ServerEndpointI;
        required: ServerEndpointI;
    };
    managementMcpUrls: {
        secret: string;
        tenantLess: string;
    };
    namePrefix: string;
    workspaceIds: {
        first: string;
        second: string;
    };
}

export type OAuth2PrincipalType = 'admin' | 'editor';

export interface OAuth2TokenProviderI {
    getAccessToken(principal: OAuth2PrincipalType, resourceUrl: string, scope: string): Promise<string>;
    getPage(principal: OAuth2PrincipalType): Promise<Page>;
    issueTokens(principal: OAuth2PrincipalType, resourceUrl: string, scope: string): Promise<OAuth2TokensI>;
}

export type McpAuthenticationWorkerFixturesType = {
    mcpEnvironment: McpAuthenticationEnvironmentI;
    oauth2Tokens: OAuth2TokenProviderI;
};

interface CleanupStepI {
    description: string;
    run: () => Promise<unknown>;
}

interface SigningKeyI {
    environmentId: number;
    id: number;
    keyId: string;
    name: string;
}

const CREATE_WORKSPACE_MCP_SERVER = `mutation createWorkspaceMcpServer($input: CreateWorkspaceMcpServerInput!) {
    createWorkspaceMcpServer(input: $input) { id secretKey }
}`;

const CREATE_A2A_SERVER = `mutation createA2aServer($input: CreateA2aServerInput!) {
    createA2aServer(input: $input) { id secretKey }
}`;

const CREATE_EMBEDDED_MCP_SERVER = `mutation createEmbeddedMcpServer($input: CreateEmbeddedMcpServerInput!) {
    createEmbeddedMcpServer(input: $input) { id secretKey }
}`;

const UPDATE_EMBEDDED_MCP_SERVER = `mutation updateEmbeddedMcpServer($id: ID!, $input: McpServerUpdateInput!) {
    updateEmbeddedMcpServer(id: $id, input: $input) { id }
}`;

const CREATE_WORKSPACE_API_KEY = `mutation createWorkspaceApiKey($workspaceId: ID!, $name: String!, $environmentId: ID!) {
    createWorkspaceApiKey(workspaceId: $workspaceId, name: $name, environmentId: $environmentId)
}`;

const CREATE_API_KEY = `mutation createApiKey($name: String!, $environmentId: ID!, $type: PlatformType) {
    createApiKey(name: $name, environmentId: $environmentId, type: $type)
}`;

const MANAGEMENT_MCP_URLS = `query managementMcpServerUrls {
    managementMcpServerCanonicalUrl
    managementMcpServerUrl
}`;

async function expectOk(responsePromise: Promise<APIResponse>): Promise<void> {
    const response = await responsePromise;

    if (!response.ok()) {
        throw new Error(`${response.url()} returned ${response.status()}`);
    }
}

async function runCleanup(cleanupSteps: CleanupStepI[]): Promise<void> {
    const failures: string[] = [];

    for (const cleanupStep of [...cleanupSteps].reverse()) {
        try {
            await cleanupStep.run();
        } catch (error) {
            failures.push(`${cleanupStep.description}: ${String(error)}`);
        }
    }

    if (failures.length) {
        console.warn(`MCP authentication cleanup left work behind:\n${failures.join('\n')}`);
    }
}

async function createWorkspace(adminSession: BackendSession, name: string): Promise<string> {
    const workspace = await adminSession.restJson<{id: number}>('POST', '/api/automation/internal/workspaces', {
        name,
    });

    return String(workspace.id);
}

async function createWorkspaceMcpServer(
    adminSession: BackendSession,
    name: string,
    workspaceId: string,
    authenticationRequired: boolean
): Promise<ServerEndpointI> {
    const data = await adminSession.graphQlData<{createWorkspaceMcpServer: {id: string; secretKey: string}}>(
        CREATE_WORKSPACE_MCP_SERVER,
        {
            input: {
                authenticationRequired,
                enabled: true,
                environmentId: String(ENVIRONMENT_IDS.DEVELOPMENT),
                name,
                type: 'AUTOMATION',
                workspaceId,
            },
        }
    );

    const {id, secretKey} = data.createWorkspaceMcpServer;

    return {id, secretKey, url: `${MCP_TEST_CONFIGURATION.publicUrl}/api/automation/${secretKey}/mcp`};
}

async function createA2aServer(
    adminSession: BackendSession,
    name: string,
    workspaceId: string,
    authenticationRequired: boolean
): Promise<ServerEndpointI> {
    const data = await adminSession.graphQlData<{createA2aServer: {id: string; secretKey: string}}>(CREATE_A2A_SERVER, {
        input: {
            authenticationRequired,
            environmentId: String(ENVIRONMENT_IDS.DEVELOPMENT),
            name,
            workspaceId,
        },
    });

    const {id, secretKey} = data.createA2aServer;

    return {id, secretKey, url: `${MCP_TEST_CONFIGURATION.publicUrl}/api/automation/a2a/${secretKey}`};
}

async function createEmbeddedMcpServer(
    adminSession: BackendSession,
    cleanupSteps: CleanupStepI[],
    name: string,
    enabled: boolean,
    authenticationRequired: boolean
): Promise<ServerEndpointI> {
    const data = await adminSession.graphQlData<{createEmbeddedMcpServer: {id: string; secretKey: string}}>(
        CREATE_EMBEDDED_MCP_SERVER,
        {input: {enabled, environmentId: String(ENVIRONMENT_IDS.DEVELOPMENT), name}}
    );

    const {id, secretKey} = data.createEmbeddedMcpServer;

    cleanupSteps.push({
        description: `delete embedded MCP server ${id}`,
        run: () =>
            adminSession.graphQlData(
                'mutation deleteEmbeddedMcpServer($id: ID!) { deleteEmbeddedMcpServer(mcpServerId: $id) }',
                {id}
            ),
    });

    await adminSession.graphQlData(UPDATE_EMBEDDED_MCP_SERVER, {id, input: {authenticationRequired, enabled}});

    return {id, secretKey, url: `${MCP_TEST_CONFIGURATION.publicUrl}/api/embedded/${secretKey}/mcp`};
}

async function createSigningKey(
    adminSession: BackendSession,
    name: string,
    environmentId: number
): Promise<SigningKeyI & {privateKeyPem: string}> {
    const {privateKey} = await adminSession.restJson<{privateKey: string}>(
        'POST',
        '/api/embedded/internal/signing-keys',
        {environmentId, name}
    );

    const signingKeys = await adminSession.restJson<SigningKeyI[]>(
        'GET',
        `/api/embedded/internal/signing-keys?environmentId=${environmentId}`
    );

    const signingKey = signingKeys.find((candidate) => candidate.name === name);

    if (!signingKey) {
        throw new Error(`Signing key ${name} was not listed after it was created`);
    }

    return {...signingKey, privateKeyPem: privateKey};
}

async function findConnectedUserIds(adminSession: BackendSession, externalUserId: string): Promise<number[]> {
    const connectedUserPage = await adminSession.restJson<{content: {externalId: string; id: number}[]}>(
        'GET',
        `/api/embedded/internal/connected-users?environmentId=${ENVIRONMENT_IDS.DEVELOPMENT}&search=${encodeURIComponent(externalUserId)}`
    );

    return connectedUserPage.content
        .filter((connectedUser) => connectedUser.externalId === externalUserId)
        .map((connectedUser) => connectedUser.id);
}

async function deleteApiKeyByName(adminSession: BackendSession, listQuery: string, name: string): Promise<void> {
    const data = await adminSession.graphQlData<Record<string, {id: string; name: string}[]>>(listQuery);

    const apiKeys = Object.values(data)[0] || [];

    for (const apiKey of apiKeys.filter((candidate) => candidate.name === name)) {
        await adminSession.graphQlData('mutation deleteApiKey($id: ID!) { deleteApiKey(id: $id) }', {id: apiKey.id});
    }
}

async function deleteWorkspaceApiKeyByName(
    adminSession: BackendSession,
    workspaceId: string,
    environmentId: number,
    name: string
): Promise<void> {
    const data = await adminSession.graphQlData<{workspaceApiKeys: {id: string; name: string}[]}>(
        `query workspaceApiKeys($workspaceId: ID!, $environmentId: ID!) {
            workspaceApiKeys(workspaceId: $workspaceId, environmentId: $environmentId) { id name }
        }`,
        {environmentId: String(environmentId), workspaceId}
    );

    for (const apiKey of data.workspaceApiKeys.filter((candidate) => candidate.name === name)) {
        await adminSession.graphQlData(
            'mutation deleteWorkspaceApiKey($apiKeyId: ID!) { deleteWorkspaceApiKey(apiKeyId: $apiKeyId) }',
            {apiKeyId: apiKey.id}
        );
    }
}

async function revokeRegisteredClients(adminSession: BackendSession): Promise<void> {
    const data = await adminSession.graphQlData<{registeredClients: {clientId: string; id: string}[]}>(
        'query registeredClients { registeredClients { id clientId } }'
    );

    const registeredClients = data.registeredClients.filter((registeredClient) =>
        REGISTERED_CLIENT_IDS.includes(registeredClient.clientId)
    );

    for (const registeredClient of registeredClients) {
        await adminSession.graphQl(
            'mutation revokeRegisteredClientGrants($id: ID!) { revokeRegisteredClientGrants(registeredClientId: $id) }',
            {id: registeredClient.id}
        );

        await adminSession.graphQl('mutation deleteRegisteredClient($id: ID!) { deleteRegisteredClient(id: $id) }', {
            id: registeredClient.id,
        });
    }
}

async function createMcpAuthenticationEnvironment(
    apiRequest: APIRequest,
    adminSession: BackendSession,
    cleanupSteps: CleanupStepI[]
): Promise<McpAuthenticationEnvironmentI> {
    const namePrefix = `mcp-e2e-${getRandomString().slice(0, 8)}`;

    cleanupSteps.push({description: 'revoke OAuth2 clients', run: () => revokeRegisteredClients(adminSession)});

    let firstWorkspaceId = MCP_TEST_CONFIGURATION.memberWorkspaceId;

    if (!hasWorkspaceMembers() || !firstWorkspaceId) {
        const createdWorkspaceId = await createWorkspace(adminSession, `${namePrefix}-w1`);

        cleanupSteps.push({
            description: `delete workspace ${createdWorkspaceId}`,
            run: () =>
                expectOk(adminSession.rest('DELETE', `/api/automation/internal/workspaces/${createdWorkspaceId}`)),
        });

        firstWorkspaceId = createdWorkspaceId;
    }

    const secondWorkspaceId = await createWorkspace(adminSession, `${namePrefix}-w2`);

    cleanupSteps.push({
        description: `delete workspace ${secondWorkspaceId}`,
        run: () => expectOk(adminSession.rest('DELETE', `/api/automation/internal/workspaces/${secondWorkspaceId}`)),
    });

    const trackWorkspaceMcpServer = (server: ServerEndpointI): ServerEndpointI => {
        cleanupSteps.push({
            description: `delete MCP server ${server.id}`,
            run: () =>
                adminSession.graphQlData(
                    'mutation deleteWorkspaceMcpServer($id: ID!) { deleteWorkspaceMcpServer(mcpServerId: $id) }',
                    {id: server.id}
                ),
        });

        return server;
    };

    const automationMcpServers = {
        optionalFirstWorkspace: trackWorkspaceMcpServer(
            await createWorkspaceMcpServer(adminSession, `${namePrefix}-ao1`, firstWorkspaceId, false)
        ),
        requiredFirstWorkspace: trackWorkspaceMcpServer(
            await createWorkspaceMcpServer(adminSession, `${namePrefix}-ar1`, firstWorkspaceId, true)
        ),
        requiredSecondWorkspace: trackWorkspaceMcpServer(
            await createWorkspaceMcpServer(adminSession, `${namePrefix}-ar2`, secondWorkspaceId, true)
        ),
        rotationFirstWorkspace: trackWorkspaceMcpServer(
            await createWorkspaceMcpServer(adminSession, `${namePrefix}-rot`, firstWorkspaceId, true)
        ),
    };

    const trackA2aServer = (server: ServerEndpointI): ServerEndpointI => {
        cleanupSteps.push({
            description: `delete A2A server ${server.id}`,
            run: () =>
                adminSession.graphQlData('mutation deleteA2aServer($id: ID!) { deleteA2aServer(id: $id) }', {
                    id: server.id,
                }),
        });

        return server;
    };

    const a2aServers = {
        optionalFirstWorkspace: trackA2aServer(
            await createA2aServer(adminSession, `${namePrefix}-a2ao1`, firstWorkspaceId, false)
        ),
        requiredFirstWorkspace: trackA2aServer(
            await createA2aServer(adminSession, `${namePrefix}-a2a1`, firstWorkspaceId, true)
        ),
        requiredSecondWorkspace: trackA2aServer(
            await createA2aServer(adminSession, `${namePrefix}-a2a2`, secondWorkspaceId, true)
        ),
    };

    const createWorkspaceApiKey = async (name: string, workspaceId: string, environmentId: number) => {
        const data = await adminSession.graphQlData<{createWorkspaceApiKey: string}>(CREATE_WORKSPACE_API_KEY, {
            environmentId: String(environmentId),
            name,
            workspaceId,
        });

        cleanupSteps.push({
            description: `delete workspace API key ${name}`,
            run: () => deleteWorkspaceApiKeyByName(adminSession, workspaceId, environmentId, name),
        });

        return data.createWorkspaceApiKey;
    };

    const createPlatformApiKey = async (name: string, type: 'EMBEDDED' | null, listQuery: string) => {
        const data = await adminSession.graphQlData<{createApiKey: string}>(CREATE_API_KEY, {
            environmentId: String(ENVIRONMENT_IDS.DEVELOPMENT),
            name,
            type,
        });

        cleanupSteps.push({
            description: `delete API key ${name}`,
            run: () => deleteApiKeyByName(adminSession, listQuery, name),
        });

        return data.createApiKey;
    };

    const apiKeys = {
        admin: await createPlatformApiKey(
            `${namePrefix}-admin`,
            null,
            `query { adminApiKeys(environmentId: ${ENVIRONMENT_IDS.DEVELOPMENT}) { id name } }`
        ),
        embedded: await createPlatformApiKey(
            `${namePrefix}-embedded`,
            'EMBEDDED',
            `query { apiKeys(environmentId: ${ENVIRONMENT_IDS.DEVELOPMENT}, type: EMBEDDED) { id name } }`
        ),
        firstWorkspace: await createWorkspaceApiKey(`${namePrefix}-k1`, firstWorkspaceId, ENVIRONMENT_IDS.DEVELOPMENT),
        firstWorkspaceProduction: await createWorkspaceApiKey(
            `${namePrefix}-k1prod`,
            firstWorkspaceId,
            ENVIRONMENT_IDS.PRODUCTION
        ),
        secondWorkspace: await createWorkspaceApiKey(
            `${namePrefix}-k2`,
            secondWorkspaceId,
            ENVIRONMENT_IDS.DEVELOPMENT
        ),
    };

    const managementUrls = await adminSession.graphQlData<{
        managementMcpServerCanonicalUrl: string;
        managementMcpServerUrl: string;
    }>(MANAGEMENT_MCP_URLS);

    const trackSigningKey = (signingKey: SigningKeyI & {privateKeyPem: string}) => {
        cleanupSteps.push({
            description: `delete signing key ${signingKey.name}`,
            run: () => expectOk(adminSession.rest('DELETE', `/api/embedded/internal/signing-keys/${signingKey.id}`)),
        });

        return signingKey;
    };

    const developmentSigningKey = trackSigningKey(
        await createSigningKey(adminSession, `${namePrefix}-signing-development`, ENVIRONMENT_IDS.DEVELOPMENT)
    );
    const stagingSigningKey = trackSigningKey(
        await createSigningKey(adminSession, `${namePrefix}-signing-staging`, ENVIRONMENT_IDS.STAGING)
    );

    const embeddedMcpServers = {
        disabled: await createEmbeddedMcpServer(adminSession, cleanupSteps, `${namePrefix}-embd`, false, true),
        optional: await createEmbeddedMcpServer(adminSession, cleanupSteps, `${namePrefix}-embo`, true, false),
        required: await createEmbeddedMcpServer(adminSession, cleanupSteps, `${namePrefix}-emb`, true, true),
    };

    const connectedUserExternalId = `${namePrefix}-user`;
    const disabledConnectedUserExternalId = `${namePrefix}-disabled`;

    for (const externalUserId of [connectedUserExternalId, disabledConnectedUserExternalId]) {
        cleanupSteps.push({
            description: `delete connected user ${externalUserId}`,
            run: async () => {
                for (const connectedUserId of await findConnectedUserIds(adminSession, externalUserId)) {
                    await expectOk(
                        adminSession.rest('DELETE', `/api/embedded/internal/connected-users/${connectedUserId}`)
                    );
                }
            },
        });
    }

    const signDevelopmentJwt = (subject: string) =>
        signConnectedUserJwt({
            keyId: developmentSigningKey.keyId,
            privateKeyPem: developmentSigningKey.privateKeyPem,
            subject,
        });

    const disabledConnectedUserJwt = signDevelopmentJwt(disabledConnectedUserExternalId);

    const unauthenticatedRequestContext = await apiRequest.newContext();

    try {
        const firstUseStatus = await postMcpInitialize(unauthenticatedRequestContext, embeddedMcpServers.required.url, {
            bearerToken: disabledConnectedUserJwt,
            environment: 'DEVELOPMENT',
        });

        if (firstUseStatus !== 200) {
            throw new Error(`Creating the connected user to disable returned ${firstUseStatus}`);
        }
    } finally {
        await unauthenticatedRequestContext.dispose();
    }

    for (const connectedUserId of await findConnectedUserIds(adminSession, disabledConnectedUserExternalId)) {
        const disableResponse = await adminSession.rest(
            'PATCH',
            `/api/embedded/internal/connected-users/${connectedUserId}/enable/false`
        );

        if (!disableResponse.ok()) {
            throw new Error(`Disabling connected user ${connectedUserId} returned ${disableResponse.status()}`);
        }
    }

    return {
        a2aServers,
        adminSession,
        apiKeys,
        automationMcpServers,
        connectedUserJwts: {
            developmentSigned: signDevelopmentJwt(connectedUserExternalId),
            disabledConnectedUser: disabledConnectedUserJwt,
            stagingSigned: signConnectedUserJwt({
                keyId: stagingSigningKey.keyId,
                privateKeyPem: stagingSigningKey.privateKeyPem,
                subject: connectedUserExternalId,
            }),
            unrelatedKeySigned: signConnectedUserJwt({
                keyId: developmentSigningKey.keyId,
                privateKeyPem: generateUnrelatedPrivateKeyPem(),
                subject: connectedUserExternalId,
            }),
            unsigned: createUnsignedConnectedUserJwt(developmentSigningKey.keyId, connectedUserExternalId),
        },
        embeddedMcpServers,
        managementMcpUrls: {
            secret: managementUrls.managementMcpServerUrl,
            tenantLess: managementUrls.managementMcpServerCanonicalUrl,
        },
        namePrefix,
        workspaceIds: {first: firstWorkspaceId, second: secondWorkspaceId},
    };
}

function getPrincipalCredentials(principal: OAuth2PrincipalType): UserCredentialsI {
    if (principal === 'admin') {
        return MCP_TEST_CONFIGURATION.admin;
    }

    if (!MCP_TEST_CONFIGURATION.editor) {
        throw new Error('Editor credentials are not configured');
    }

    return MCP_TEST_CONFIGURATION.editor;
}

function createOAuth2TokenProvider(browser: Browser): {dispose: () => Promise<void>; provider: OAuth2TokenProviderI} {
    const browserContexts = new Map<OAuth2PrincipalType, Promise<{context: BrowserContext; page: Page}>>();
    const accessTokens = new Map<string, Promise<OAuth2TokensI>>();

    const getSignedInPage = (principal: OAuth2PrincipalType): Promise<{context: BrowserContext; page: Page}> => {
        let signedInPage = browserContexts.get(principal);

        if (!signedInPage) {
            signedInPage = (async () => {
                const context = await browser.newContext({baseURL: MCP_TEST_CONFIGURATION.baseUrl});
                const page = await context.newPage();
                const credentials = getPrincipalCredentials(principal);

                await login(page, credentials.email, credentials.password);

                return {context, page};
            })();

            browserContexts.set(principal, signedInPage);
        }

        return signedInPage;
    };

    const issueTokens = async (principal: OAuth2PrincipalType, resourceUrl: string, scope: string) => {
        const {context, page} = await getSignedInPage(principal);

        return obtainAccessToken(page, context.request, {resourceUrl, scope});
    };

    const provider: OAuth2TokenProviderI = {
        getAccessToken: async (principal, resourceUrl, scope) => {
            const cacheKey = `${principal} ${scope} ${resourceUrl}`;

            let tokens = accessTokens.get(cacheKey);

            if (!tokens) {
                tokens = issueTokens(principal, resourceUrl, scope);

                accessTokens.set(cacheKey, tokens);
            }

            return (await tokens).accessToken;
        },
        getPage: async (principal) => (await getSignedInPage(principal)).page,
        issueTokens,
    };

    return {
        dispose: async () => {
            for (const signedInPage of browserContexts.values()) {
                const {context} = await signedInPage;

                await context.close();
            }
        },
        provider,
    };
}

export const mcpAuthenticationTest = base.extend<object, McpAuthenticationWorkerFixturesType>({
    mcpEnvironment: [
        async ({playwright}, use) => {
            const cleanupSteps: CleanupStepI[] = [];
            const adminSession = await BackendSession.signIn(MCP_TEST_CONFIGURATION.admin);

            try {
                const mcpEnvironment = await createMcpAuthenticationEnvironment(
                    playwright.request,
                    adminSession,
                    cleanupSteps
                );

                await use(mcpEnvironment);
            } finally {
                await runCleanup(cleanupSteps);

                await adminSession.dispose();
            }
        },
        {scope: 'worker', timeout: 180000},
    ],
    oauth2Tokens: [
        async ({browser}, use) => {
            const {dispose, provider} = createOAuth2TokenProvider(browser);

            try {
                await use(provider);
            } finally {
                await dispose();
            }
        },
        {scope: 'worker', timeout: 180000},
    ],
});

export const mcpAuthenticationExpect = mcpAuthenticationTest.expect;
