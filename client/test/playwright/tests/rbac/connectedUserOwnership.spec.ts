import {type APIRequestContext, expect, request as playwrightRequest} from '@playwright/test';
import {createSign} from 'crypto';

import {workspaceRolesTest as test} from '../../fixtures/workspaceRoles';
import getRandomString from '../../utils/getRandomString';
import {RBAC_BASE_URL, internalApi, sendInternalApi} from '../../utils/rbacUtils';

const EMBEDDED_INTERNAL = '/api/embedded/internal';

const ENVIRONMENT = 'PRODUCTION';

const ENVIRONMENT_ID = 2;

interface SigningKeyI {
    keyId: string;
    privateKey: string;
}

interface EmbeddedConnectionI {
    editable?: boolean;
    id: number;
    name: string;
    shared?: boolean;
}

function base64Url(value: Buffer | string): string {
    return Buffer.from(value).toString('base64url');
}

function signConnectedUserJwt(signingKey: SigningKeyI, externalUserId: string): string {
    const issuedAt = Math.floor(Date.now() / 1000);

    const header = base64Url(JSON.stringify({alg: 'RS256', kid: signingKey.keyId, typ: 'JWT'}));
    const payload = base64Url(JSON.stringify({exp: issuedAt + 3600, iat: issuedAt, sub: externalUserId}));

    const signer = createSign('RSA-SHA256');

    signer.update(`${header}.${payload}`);

    return `${header}.${payload}.${base64Url(signer.sign(signingKey.privateKey))}`;
}

async function createSigningKey(adminApi: APIRequestContext): Promise<SigningKeyI> {
    const name = `rbac_signing_${getRandomString()}`;

    const createResponse = await sendInternalApi(adminApi, 'POST', `${EMBEDDED_INTERNAL}/signing-keys`, {
        environmentId: ENVIRONMENT_ID,
        name,
    });

    expect(createResponse.ok(), await createResponse.text()).toBeTruthy();

    const {privateKey} = await createResponse.json();

    const listResponse = await sendInternalApi(
        adminApi,
        'GET',
        `${EMBEDDED_INTERNAL}/signing-keys?environmentId=${ENVIRONMENT_ID}`
    );

    const signingKeys: {keyId: string; name: string}[] = await listResponse.json();

    return {keyId: signingKeys.find((signingKey) => signingKey.name === name)!.keyId, privateKey};
}

async function createConnectedUserContext(signingKey: SigningKeyI, externalUserId: string, environment = ENVIRONMENT) {
    return playwrightRequest.newContext({
        baseURL: RBAC_BASE_URL,
        extraHTTPHeaders: {
            Authorization: `Bearer ${signConnectedUserJwt(signingKey, externalUserId)}`,
            'X-Environment': environment,
        },
    });
}

async function listOwnConnections(connectedUserApi: APIRequestContext): Promise<EmbeddedConnectionI[]> {
    const response = await connectedUserApi.get('/api/embedded/v1/connections');

    expect(response.status(), await response.text()).toBe(200);

    return response.json();
}

async function findConnectedUserId(adminApi: APIRequestContext, externalUserId: string): Promise<number> {
    const response = await sendInternalApi(
        adminApi,
        'GET',
        `${EMBEDDED_INTERNAL}/connected-users?environmentId=${ENVIRONMENT_ID}&search=${externalUserId}`
    );

    expect(response.ok(), await response.text()).toBeTruthy();

    const body = await response.json();

    const connectedUsers: {externalId: string; id: number}[] = Array.isArray(body) ? body : body.content;

    return connectedUsers.find((connectedUser) => connectedUser.externalId === externalUserId)!.id;
}

async function createConnectedUserConnection(adminApi: APIRequestContext, connectedUserId: number): Promise<number> {
    const response = await sendInternalApi(
        adminApi,
        'POST',
        `${EMBEDDED_INTERNAL}/connected-users/${connectedUserId}/connections`,
        {
            componentName: 'httpClient',
            connectionVersion: 1,
            environmentId: ENVIRONMENT_ID,
            name: `rbac_connected_user_${getRandomString()}`,
            parameters: {},
        }
    );

    expect(response.ok(), await response.text()).toBeTruthy();

    return Number(await response.text());
}

async function createConnectedUserWorkflow(connectedUserApi: APIRequestContext): Promise<string> {
    const createResponse = await connectedUserApi.post('/api/embedded/v1/automation/workflows', {
        data: {
            definition: JSON.stringify({
                label: 'rbac connected user workflow',
                tasks: [
                    {
                        label: 'HTTP',
                        name: 'httpClient_1',
                        parameters: {uri: 'http://localhost'},
                        type: 'httpClient/v1/get',
                    },
                ],
                triggers: [{label: 'Manual', name: 'trigger_1', type: 'manual/v1/manual'}],
            }),
        },
    });

    expect(createResponse.status(), await createResponse.text()).toBe(200);

    return (await createResponse.text()).replaceAll('"', '');
}

test.describe('Connected-user ownership', () => {
    const externalUserA = `rbac_ext_a_${getRandomString()}`;
    const externalUserB = `rbac_ext_b_${getRandomString()}`;

    let connectedUserA: APIRequestContext;
    let connectedUserB: APIRequestContext;
    let connectionOfA: number;
    let connectionOfB: number;
    let sharedConnection: number;
    let signingKey: SigningKeyI;

    test.beforeAll(async ({adminApi}) => {
        signingKey = await createSigningKey(adminApi);

        connectedUserA = await createConnectedUserContext(signingKey, externalUserA);
        connectedUserB = await createConnectedUserContext(signingKey, externalUserB);

        await listOwnConnections(connectedUserA);
        await listOwnConnections(connectedUserB);

        connectionOfA = await createConnectedUserConnection(
            adminApi,
            await findConnectedUserId(adminApi, externalUserA)
        );

        const connectedUserIdOfB = await findConnectedUserId(adminApi, externalUserB);

        const connectionResponse = await sendInternalApi(
            adminApi,
            'POST',
            `${EMBEDDED_INTERNAL}/connected-users/${connectedUserIdOfB}/connections`,
            {
                componentName: 'httpClient',
                connectionVersion: 1,
                environmentId: ENVIRONMENT_ID,
                name: `rbac_b_${getRandomString()}`,
                parameters: {},
            }
        );

        expect(connectionResponse.ok(), await connectionResponse.text()).toBeTruthy();

        connectionOfB = Number(await connectionResponse.text());

        const sharedResponse = await sendInternalApi(adminApi, 'POST', `${EMBEDDED_INTERNAL}/connections`, {
            componentName: 'httpClient',
            connectionVersion: 1,
            environmentId: ENVIRONMENT_ID,
            name: `rbac_shared_${getRandomString()}`,
            parameters: {},
            shared: true,
        });

        expect(sharedResponse.ok(), await sharedResponse.text()).toBeTruthy();

        const sharedBody = await sharedResponse.json();

        sharedConnection = typeof sharedBody === 'number' ? sharedBody : sharedBody.id;
    });

    test.afterAll(async () => {
        await connectedUserA?.dispose();
        await connectedUserB?.dispose();
    });

    test('a connected user lists only the connections they own plus the ones an admin shared', async () => {
        const connectionsOfA = await listOwnConnections(connectedUserA);
        const connectionsOfB = await listOwnConnections(connectedUserB);

        expect(connectionsOfB.map((connection) => connection.id)).toContain(connectionOfB);
        expect(connectionsOfA.map((connection) => connection.id)).not.toContain(connectionOfB);

        const sharedForA = connectionsOfA.find((connection) => connection.id === sharedConnection);

        expect(sharedForA, JSON.stringify(connectionsOfA)).toMatchObject({editable: false, shared: true});
    });

    test("a connected user cannot delete another connected user's connection or a shared one", async () => {
        const foreignResponse = await connectedUserA.delete(`/api/embedded/v1/connections/${connectionOfB}`);

        expect(foreignResponse.status()).toBe(404);

        const sharedResponse = await connectedUserA.delete(`/api/embedded/v1/connections/${sharedConnection}`);

        expect(sharedResponse.status()).toBe(404);

        expect((await listOwnConnections(connectedUserB)).map((connection) => connection.id)).toContain(connectionOfB);
    });

    test("a connected user may bind their own connection but not another connected user's", async () => {
        const workflowUuid = await createConnectedUserWorkflow(connectedUserA);

        const bindResponse = await connectedUserA.put(
            `/api/embedded/v1/automation/workflows/${workflowUuid}/workflow-nodes/httpClient_1/connection/httpClient`,
            {data: {connectionId: connectionOfB}}
        );

        expect(bindResponse.status(), await bindResponse.text()).toBe(403);
        expect(await bindResponse.json()).toMatchObject({detail: 'Access denied', status: 403});

        const ownBindResponse = await connectedUserA.put(
            `/api/embedded/v1/automation/workflows/${workflowUuid}/workflow-nodes/httpClient_1/connection/httpClient`,
            {data: {connectionId: connectionOfA}}
        );

        expect(ownBindResponse.status(), await ownBindResponse.text()).toBeLessThan(300);
    });

    test('a connected user may bind a connection an admin shared with every connected user', async () => {
        const workflowUuid = await createConnectedUserWorkflow(connectedUserA);

        const sharedBindResponse = await connectedUserA.put(
            `/api/embedded/v1/automation/workflows/${workflowUuid}/workflow-nodes/httpClient_1/connection/httpClient`,
            {data: {connectionId: sharedConnection}}
        );

        expect(sharedBindResponse.status(), await sharedBindResponse.text()).toBeLessThan(300);
    });

    test('a connected user is refused on workspace resources they do not own', async ({adminApi, rbacWorkspace}) => {
        const projectResponse = await connectedUserA.get(
            `/api/automation/internal/projects/${rbacWorkspace.projectId}`
        );

        expect(projectResponse.status(), await projectResponse.text()).toBe(403);

        const workflowResponse = await connectedUserA.get(
            `/api/automation/internal/workflows/${rbacWorkspace.workflowId}`
        );

        expect(workflowResponse.status(), await workflowResponse.text()).toBe(403);

        const deleteResponse = await connectedUserA.delete(
            `/api/automation/internal/projects/${rbacWorkspace.projectId}`
        );

        expect(deleteResponse.status(), await deleteResponse.text()).toBe(403);

        const project = await internalApi<{id: number}>(adminApi, 'GET', `/projects/${rbacWorkspace.projectId}`);

        expect(project.id).toBe(rbacWorkspace.projectId);
    });

    test('a connected-user token is refused in an environment its signing key does not belong to', async () => {
        const developmentContext = await createConnectedUserContext(signingKey, externalUserA, 'DEVELOPMENT');

        try {
            const response = await developmentContext.get('/api/embedded/v1/connections');

            expect([401, 403]).toContain(response.status());
        } finally {
            await developmentContext.dispose();
        }
    });
});
