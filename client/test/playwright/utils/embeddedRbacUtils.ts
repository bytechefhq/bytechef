import {APIRequestContext, Browser, BrowserContext, Page, expect, request as playwrightRequest} from '@playwright/test';
import {createSign} from 'crypto';

import getRandomString from './getRandomString';
import {login} from './login';
import {type BuiltInRoleType, type EnvironmentType, type RoleUserI, graphql, sendInternalApi} from './rbacUtils';

export type EmbeddedDataEnvironmentType = 'DEVELOPMENT' | 'PRODUCTION';

export interface EmbeddedEnvironmentDataI {
    connectedUserExternalId: string;
    connectedUserId: number;
    connectionId: number;
    instanceConfigurationId: number;
    mcpServerId: string;
    signingKeyId: number;
    signingKeyName: string;
}

export interface EmbeddedDataI {
    byEnvironment: Record<EmbeddedDataEnvironmentType, EmbeddedEnvironmentDataI>;
    integrationId: number;
    integrationName: string;
    workflowUuid: string;
}

export const EMBEDDED_DATA_ENVIRONMENTS: EmbeddedDataEnvironmentType[] = ['DEVELOPMENT', 'PRODUCTION'];

const EMBEDDED_INTERNAL = '/api/embedded/internal';

export const EMBEDDED_ENVIRONMENT_IDS: Record<EnvironmentType, number> = {
    DEVELOPMENT: 0,
    PRODUCTION: 2,
    STAGING: 1,
};

export async function embeddedInternalApi<T>(
    apiContext: APIRequestContext,
    method: 'DELETE' | 'GET' | 'PATCH' | 'POST' | 'PUT',
    path: string,
    data?: unknown
): Promise<T> {
    const response = await sendInternalApi(apiContext, method, `${EMBEDDED_INTERNAL}${path}`, data);

    expect(response.ok(), `${method} ${path} failed: ${response.status()} ${await response.text()}`).toBeTruthy();

    const text = await response.text();

    if (!text) {
        return undefined as T;
    }

    try {
        return JSON.parse(text) as T;
    } catch {
        return text as T;
    }
}

export async function setEmbeddedRole(
    adminApi: APIRequestContext,
    userId: string,
    role: BuiltInRoleType
): Promise<void> {
    await graphql(
        adminApi,
        `
            mutation AddEmbeddedUser($userId: ID!, $role: WorkspaceRole!) {
                addEmbeddedUser(userId: $userId, role: $role) {
                    userId
                }
            }
        `,
        {role, userId}
    );
}

export async function setEmbeddedEnvironmentRole(
    adminApi: APIRequestContext,
    userId: string,
    environment: EnvironmentType,
    role: BuiltInRoleType
): Promise<void> {
    await graphql(
        adminApi,
        `
            mutation SetEmbeddedUserEnvironmentRole(
                $userId: ID!
                $environment: EnvironmentEnum!
                $role: WorkspaceRole!
            ) {
                setEmbeddedUserEnvironmentRole(userId: $userId, environment: $environment, role: $role) {
                    userId
                }
            }
        `,
        {environment, role, userId}
    );
}

export async function removeEmbeddedEnvironmentRole(
    adminApi: APIRequestContext,
    userId: string,
    environment: EnvironmentType
): Promise<void> {
    await graphql(
        adminApi,
        `
            mutation RemoveEmbeddedUserEnvironmentRole($userId: ID!, $environment: EnvironmentEnum!) {
                removeEmbeddedUserEnvironmentRole(userId: $userId, environment: $environment)
            }
        `,
        {environment, userId}
    );
}

export async function setEmbeddedOnlyEnvironmentRole(
    adminApi: APIRequestContext,
    userId: string,
    environment: EnvironmentType,
    role: BuiltInRoleType
): Promise<void> {
    await setEmbeddedRole(adminApi, userId, role);

    await setEmbeddedEnvironmentRole(adminApi, userId, environment, role);

    for (const otherEnvironment of Object.keys(EMBEDDED_ENVIRONMENT_IDS) as EnvironmentType[]) {
        if (otherEnvironment !== environment) {
            await removeEmbeddedEnvironmentRole(adminApi, userId, otherEnvironment);
        }
    }
}

export async function removeEmbeddedUser(adminApi: APIRequestContext, userId: string): Promise<void> {
    await graphql(adminApi, 'mutation RemoveEmbeddedUser($userId: ID!) { removeEmbeddedUser(userId: $userId) }', {
        userId,
    });
}

export async function fetchMyEmbeddedScopes(
    apiContext: APIRequestContext,
    environment?: EnvironmentType
): Promise<string[]> {
    const data = await graphql<{myEmbeddedScopes: string[]}>(
        apiContext,
        `
            query MyEmbeddedScopes($environment: EnvironmentEnum) {
                myEmbeddedScopes(environment: $environment)
            }
        `,
        {environment: environment || null}
    );

    return data.myEmbeddedScopes;
}

export async function openAsEmbeddedRoleUser(
    browser: Browser,
    baseURL: string,
    roleUser: RoleUserI,
    environmentId: number = EMBEDDED_ENVIRONMENT_IDS.DEVELOPMENT
): Promise<{context: BrowserContext; page: Page}> {
    const context = await browser.newContext({baseURL});

    await context.addInitScript((selectedEnvironmentId) => {
        window.localStorage.setItem(
            'bytechef.environment',
            JSON.stringify({state: {currentEnvironmentId: selectedEnvironmentId}, version: 0})
        );
    }, environmentId);

    const page = await context.newPage();

    await login(page, roleUser.email, roleUser.password);

    return {context, page};
}

function base64Url(value: Buffer | string): string {
    return Buffer.from(value).toString('base64url');
}

export function signConnectedUserJwt(keyId: string, privateKey: string, externalUserId: string): string {
    const issuedAt = Math.floor(Date.now() / 1000);

    const header = base64Url(JSON.stringify({alg: 'RS256', kid: keyId, typ: 'JWT'}));
    const payload = base64Url(JSON.stringify({exp: issuedAt + 3600, iat: issuedAt, sub: externalUserId}));

    const signer = createSign('RSA-SHA256');

    signer.update(`${header}.${payload}`);

    return `${header}.${payload}.${base64Url(signer.sign(privateKey))}`;
}

async function createEnvironmentData(
    adminApi: APIRequestContext,
    baseURL: string,
    environment: EmbeddedDataEnvironmentType,
    integrationId: number,
    suffix: string
): Promise<EmbeddedEnvironmentDataI> {
    const environmentId = EMBEDDED_ENVIRONMENT_IDS[environment];

    const label = environment.toLowerCase();

    const signingKeyName = `rbac_signing_${label}_${suffix}`;

    const {privateKey} = await embeddedInternalApi<{privateKey: string}>(adminApi, 'POST', '/signing-keys', {
        environmentId,
        name: signingKeyName,
    });

    const signingKeys = await embeddedInternalApi<{id: number; keyId: string; name: string}[]>(
        adminApi,
        'GET',
        `/signing-keys?environmentId=${environmentId}`
    );

    const signingKey = signingKeys.find((candidate) => candidate.name === signingKeyName)!;

    const connectedUserExternalId = `rbac_ext_${label}_${suffix}`;

    const connectedUserApi = await playwrightRequest.newContext({
        baseURL,
        extraHTTPHeaders: {
            Authorization: `Bearer ${signConnectedUserJwt(signingKey.keyId, privateKey, connectedUserExternalId)}`,
            'X-Environment': environment,
        },
    });

    try {
        const connectedUserResponse = await connectedUserApi.get('/api/embedded/v1/connections');

        expect(connectedUserResponse.status(), await connectedUserResponse.text()).toBe(200);
    } finally {
        await connectedUserApi.dispose();
    }

    const connectedUsersBody = await embeddedInternalApi<
        {content: {externalId: string; id: number}[]} | {externalId: string; id: number}[]
    >(adminApi, 'GET', `/connected-users?environmentId=${environmentId}&search=${connectedUserExternalId}`);

    const connectedUsers = Array.isArray(connectedUsersBody) ? connectedUsersBody : connectedUsersBody.content;

    const connectedUserId = connectedUsers.find((candidate) => candidate.externalId === connectedUserExternalId)!.id;

    const connectionId = await embeddedInternalApi<number>(adminApi, 'POST', '/connections', {
        componentName: 'httpClient',
        connectionVersion: 1,
        environmentId,
        name: `rbac_shared_${label}_${suffix}`,
        parameters: {},
        shared: true,
    }).then((body) => (typeof body === 'number' ? body : (body as unknown as {id: number}).id));

    const instanceConfigurationId = await embeddedInternalApi<number>(
        adminApi,
        'POST',
        '/integration-instance-configurations',
        {
            authorizationType: 'BASIC_AUTH',
            connectionParameters: {},
            enabled: false,
            environmentId,
            integrationId,
            integrationVersion: 1,
            name: `rbac_instance_${label}_${suffix}`,
        }
    );

    const mcpServer = await graphql<{createEmbeddedMcpServer: {id: string}}>(
        adminApi,
        `
            mutation CreateEmbeddedMcpServer($input: CreateEmbeddedMcpServerInput!) {
                createEmbeddedMcpServer(input: $input) {
                    id
                }
            }
        `,
        {input: {enabled: true, environmentId: String(environmentId), name: `rbac_mcp_${label}_${suffix}`}}
    );

    return {
        connectedUserExternalId,
        connectedUserId,
        connectionId,
        instanceConfigurationId,
        mcpServerId: mcpServer.createEmbeddedMcpServer.id,
        signingKeyId: signingKey.id,
        signingKeyName,
    };
}

export async function createEmbeddedData(adminApi: APIRequestContext, baseURL: string): Promise<EmbeddedDataI> {
    const suffix = getRandomString();

    const integrationName = `rbac_integration_${suffix}`;

    const integrationId = await embeddedInternalApi<number>(adminApi, 'POST', '/integrations', {
        componentName: 'httpClient',
        multipleInstances: false,
        name: integrationName,
    });

    const workflowDefinition = JSON.stringify({
        label: 'rbac embedded workflow',
        tasks: [
            {label: 'HTTP', name: 'httpClient_1', parameters: {uri: 'http://localhost'}, type: 'httpClient/v1/get'},
        ],
        triggers: [{label: 'Manual', name: 'trigger_1', type: 'manual/v1/manual'}],
    });

    await embeddedInternalApi(adminApi, 'POST', `/integrations/${integrationId}/workflows`, {
        definition: workflowDefinition,
    });

    await embeddedInternalApi(adminApi, 'POST', `/integrations/${integrationId}/publish`, {description: 'rbac'});

    const workflows = await embeddedInternalApi<{id: string}[]>(
        adminApi,
        'GET',
        `/integrations/${integrationId}/workflows`
    );

    const byEnvironment = {} as Record<EmbeddedDataEnvironmentType, EmbeddedEnvironmentDataI>;

    for (const environment of EMBEDDED_DATA_ENVIRONMENTS) {
        byEnvironment[environment] = await createEnvironmentData(adminApi, baseURL, environment, integrationId, suffix);
    }

    return {byEnvironment, integrationId, integrationName, workflowUuid: workflows[0].id};
}

export async function deleteEmbeddedData(adminApi: APIRequestContext, embeddedData: EmbeddedDataI): Promise<void> {
    const failures: string[] = [];

    const attempt = async (description: string, action: () => Promise<unknown>) => {
        try {
            await action();
        } catch (error) {
            failures.push(`${description}: ${error instanceof Error ? error.message : String(error)}`);
        }
    };

    for (const environment of [...EMBEDDED_DATA_ENVIRONMENTS].reverse()) {
        const environmentData = embeddedData.byEnvironment[environment];

        await attempt(`delete MCP server ${environmentData.mcpServerId}`, () =>
            graphql(
                adminApi,
                'mutation DeleteEmbeddedMcpServer($id: ID!) { deleteEmbeddedMcpServer(mcpServerId: $id) }',
                {
                    id: environmentData.mcpServerId,
                }
            )
        );

        await attempt(`delete instance configuration ${environmentData.instanceConfigurationId}`, () =>
            embeddedInternalApi(
                adminApi,
                'DELETE',
                `/integration-instance-configurations/${environmentData.instanceConfigurationId}`
            )
        );

        await attempt(`delete connection ${environmentData.connectionId}`, () =>
            embeddedInternalApi(adminApi, 'DELETE', `/connections/${environmentData.connectionId}`)
        );

        await attempt(`delete connected user ${environmentData.connectedUserId}`, () =>
            embeddedInternalApi(adminApi, 'DELETE', `/connected-users/${environmentData.connectedUserId}`)
        );

        await attempt(`delete signing key ${environmentData.signingKeyId}`, () =>
            embeddedInternalApi(adminApi, 'DELETE', `/signing-keys/${environmentData.signingKeyId}`)
        );
    }

    await attempt(`delete integration ${embeddedData.integrationId}`, () =>
        embeddedInternalApi(adminApi, 'DELETE', `/integrations/${embeddedData.integrationId}`)
    );

    for (const failure of failures) {
        console.warn(`embeddedData teardown: ${failure}`);
    }
}
