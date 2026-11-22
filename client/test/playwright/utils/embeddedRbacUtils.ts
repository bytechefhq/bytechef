import {APIRequestContext, Browser, BrowserContext, Page, expect, request as playwrightRequest} from '@playwright/test';
import {createSign} from 'crypto';

import getRandomString from './getRandomString';
import {login} from './login';
import {
    type BuiltInRoleType,
    ENVIRONMENT_IDS,
    type EnvironmentType,
    type RoleUserI,
    graphql,
    sendInternalApi,
} from './rbacUtils';

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

export interface EmbeddedDataDraftI {
    byEnvironment: Partial<Record<EmbeddedDataEnvironmentType, Partial<EmbeddedEnvironmentDataI>>>;
    integrationId?: number;
}

export const EMBEDDED_DATA_ENVIRONMENTS: EmbeddedDataEnvironmentType[] = ['DEVELOPMENT', 'PRODUCTION'];

const EMBEDDED_INTERNAL = '/api/embedded/internal';

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

    for (const otherEnvironment of Object.keys(ENVIRONMENT_IDS) as EnvironmentType[]) {
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
    environmentId: number = ENVIRONMENT_IDS.DEVELOPMENT
): Promise<{context: BrowserContext; page: Page}> {
    const context = await browser.newContext({baseURL});

    await context.addInitScript((selectedEnvironmentId) => {
        if (window.sessionStorage.getItem('bytechef.e2e.environmentSeeded')) {
            return;
        }

        window.sessionStorage.setItem('bytechef.e2e.environmentSeeded', 'true');

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

export interface ConnectedUserApiI {
    api: APIRequestContext;
    signingKeyId: number;
}

export async function createConnectedUserApi(
    adminApi: APIRequestContext,
    baseURL: string,
    environment: EnvironmentType,
    externalId: string,
    signingKeyName: string = `rbac_signing_${externalId}`
): Promise<ConnectedUserApiI> {
    const environmentId = ENVIRONMENT_IDS[environment];

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

    const api = await playwrightRequest.newContext({
        baseURL,
        extraHTTPHeaders: {
            Authorization: `Bearer ${signConnectedUserJwt(signingKey.keyId, privateKey, externalId)}`,
            'X-Environment': environment,
        },
    });

    return {api, signingKeyId: signingKey.id};
}

export type TeardownAttemptType = (description: string, action: () => Promise<unknown>) => Promise<void>;

export function createTeardownAttempt(label: string, failures?: string[]): TeardownAttemptType {
    return async (description, action) => {
        try {
            await action();
        } catch (error) {
            const failure = `${description}: ${error instanceof Error ? error.message : String(error)}`;

            if (failures) {
                failures.push(failure);
            } else {
                console.warn(`${label} teardown: ${failure}`);
            }
        }
    };
}

async function createEnvironmentData(
    adminApi: APIRequestContext,
    baseURL: string,
    environment: EmbeddedDataEnvironmentType,
    integrationId: number,
    suffix: string,
    draft: Partial<EmbeddedEnvironmentDataI>
): Promise<EmbeddedEnvironmentDataI> {
    const environmentId = ENVIRONMENT_IDS[environment];

    const label = environment.toLowerCase();

    const signingKeyName = `rbac_signing_${label}_${suffix}`;

    const connectedUserExternalId = `rbac_ext_${label}_${suffix}`;

    const {api: connectedUserApi, signingKeyId} = await createConnectedUserApi(
        adminApi,
        baseURL,
        environment,
        connectedUserExternalId,
        signingKeyName
    );

    draft.signingKeyId = signingKeyId;
    draft.signingKeyName = signingKeyName;

    try {
        const connectedUserResponse = await connectedUserApi.get('/api/embedded/v1/connections');

        expect(connectedUserResponse.status(), await connectedUserResponse.text()).toBe(200);
    } finally {
        await connectedUserApi.dispose();
    }

    const connectedUsersPage = await embeddedInternalApi<{content: {externalId: string; id: number}[]}>(
        adminApi,
        'GET',
        `/connected-users?environmentId=${environmentId}&search=${connectedUserExternalId}`
    );

    const connectedUserId = connectedUsersPage.content.find(
        (candidate) => candidate.externalId === connectedUserExternalId
    )!.id;

    draft.connectedUserExternalId = connectedUserExternalId;
    draft.connectedUserId = connectedUserId;

    const connectionId = await embeddedInternalApi<number>(adminApi, 'POST', '/connections', {
        componentName: 'httpClient',
        connectionVersion: 1,
        environmentId,
        name: `rbac_shared_${label}_${suffix}`,
        parameters: {},
        shared: true,
    });

    draft.connectionId = connectionId;

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

    draft.instanceConfigurationId = instanceConfigurationId;

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

    draft.mcpServerId = mcpServer.createEmbeddedMcpServer.id;

    return {
        connectedUserExternalId,
        connectedUserId,
        connectionId,
        instanceConfigurationId,
        mcpServerId: mcpServer.createEmbeddedMcpServer.id,
        signingKeyId,
        signingKeyName,
    };
}

export async function createEmbeddedData(adminApi: APIRequestContext, baseURL: string): Promise<EmbeddedDataI> {
    const suffix = getRandomString();

    const integrationName = `rbac_integration_${suffix}`;

    const draft: EmbeddedDataDraftI = {byEnvironment: {}};

    try {
        const integrationId = await embeddedInternalApi<number>(adminApi, 'POST', '/integrations', {
            componentName: 'httpClient',
            multipleInstances: false,
            name: integrationName,
        });

        draft.integrationId = integrationId;

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
            const environmentDraft: Partial<EmbeddedEnvironmentDataI> = {};

            draft.byEnvironment[environment] = environmentDraft;

            byEnvironment[environment] = await createEnvironmentData(
                adminApi,
                baseURL,
                environment,
                integrationId,
                suffix,
                environmentDraft
            );
        }

        return {byEnvironment, integrationId, integrationName, workflowUuid: workflows[0].id};
    } catch (error) {
        await deleteEmbeddedData(adminApi, draft);

        throw error;
    }
}

export async function deleteEmbeddedData(adminApi: APIRequestContext, embeddedData: EmbeddedDataDraftI): Promise<void> {
    const failures: string[] = [];

    const attempt = createTeardownAttempt('embeddedData', failures);

    for (const environment of [...EMBEDDED_DATA_ENVIRONMENTS].reverse()) {
        const environmentData = embeddedData.byEnvironment[environment];

        if (!environmentData) {
            continue;
        }

        const {connectedUserId, connectionId, instanceConfigurationId, mcpServerId, signingKeyId} = environmentData;

        if (mcpServerId) {
            await attempt(`delete MCP server ${mcpServerId}`, () =>
                graphql(
                    adminApi,
                    'mutation DeleteEmbeddedMcpServer($id: ID!) { deleteEmbeddedMcpServer(mcpServerId: $id) }',
                    {id: mcpServerId}
                )
            );
        }

        if (instanceConfigurationId) {
            await attempt(`delete instance configuration ${instanceConfigurationId}`, () =>
                embeddedInternalApi(
                    adminApi,
                    'DELETE',
                    `/integration-instance-configurations/${instanceConfigurationId}`
                )
            );
        }

        if (connectionId) {
            await attempt(`delete connection ${connectionId}`, () =>
                embeddedInternalApi(adminApi, 'DELETE', `/connections/${connectionId}`)
            );
        }

        if (connectedUserId) {
            await attempt(`delete connected user ${connectedUserId}`, () =>
                embeddedInternalApi(adminApi, 'DELETE', `/connected-users/${connectedUserId}`)
            );
        }

        if (signingKeyId) {
            await attempt(`delete signing key ${signingKeyId}`, () =>
                embeddedInternalApi(adminApi, 'DELETE', `/signing-keys/${signingKeyId}`)
            );
        }
    }

    if (embeddedData.integrationId) {
        const {integrationId} = embeddedData;

        await attempt(`delete integration ${integrationId}`, () =>
            embeddedInternalApi(adminApi, 'DELETE', `/integrations/${integrationId}`)
        );
    }

    for (const failure of failures) {
        console.warn(`embeddedData teardown: ${failure}`);
    }
}
