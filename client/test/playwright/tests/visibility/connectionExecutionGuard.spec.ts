import {type APIRequestContext, expect, request as playwrightRequest, test} from '@playwright/test';

import {createAdminApiContext, deleteRoleUser} from '../../utils/rbacUtils';
import {
    RBAC_BASE_URL,
    type RoleUserI,
    createConnection,
    createHttpClientProject,
    createUserApiContext,
    createWorkspace,
    createWorkspaceMember,
    deleteWorkspace,
    deployHttpClientProject,
    deployHttpClientProjectResponse,
    expectNoGraphQlErrors,
    getConnection,
    internalApi,
    listDeploymentJobs,
    reassignConnection,
    removeWorkspaceMember,
    runDeployedWorkflowResponse,
    saveTestConfigurationConnection,
    sendInternalApi,
} from '../../utils/visibilityUtils';

const WEBHOOK_TRIGGER = 'webhook/v1/autoRespondWithHTTP200';

test.describe('Workflow execution on non-ACTIVE connections', () => {
    let adminApi: APIRequestContext;
    let workspaceId: number;
    const members: RoleUserI[] = [];

    test.beforeAll(async () => {
        adminApi = await createAdminApiContext(RBAC_BASE_URL);

        workspaceId = (await createWorkspace(adminApi)).id;
    });

    test.afterAll(async () => {
        for (const member of members) {
            await deleteRoleUser(adminApi, member);
        }

        await deleteWorkspace(adminApi, workspaceId);

        await adminApi.dispose();
    });

    async function createPendingConnectionOwner(): Promise<{connectionId: number; owner: RoleUserI}> {
        const owner = await createWorkspaceMember(adminApi, workspaceId, 'EDITOR');

        members.push(owner);

        const ownerApi = await createUserApiContext(owner.email, owner.password);

        try {
            return {connectionId: await createConnection(ownerApi, workspaceId), owner};
        } finally {
            await ownerApi.dispose();
        }
    }

    async function markPending(owner: RoleUserI, connectionId: number): Promise<void> {
        await removeWorkspaceMember(adminApi, workspaceId, owner);

        await expect
            .poll(async () => (await getConnection(adminApi, connectionId)).status, {timeout: 15000})
            .toBe('PENDING_REASSIGNMENT');
    }

    async function callWebhook(projectDeploymentId: number): Promise<void> {
        let staticWebhookUrl = '';

        await expect(async () => {
            const projectDeployment = await internalApi<{projectDeploymentWorkflows: {staticWebhookUrl?: string}[]}>(
                adminApi,
                'GET',
                `/project-deployments/${projectDeploymentId}`
            );

            staticWebhookUrl = projectDeployment.projectDeploymentWorkflows[0]?.staticWebhookUrl || '';

            expect(staticWebhookUrl, JSON.stringify(projectDeployment)).toContain('/webhooks/');
        }).toPass({timeout: 30000});

        const anonymousContext = await playwrightRequest.newContext({baseURL: RBAC_BASE_URL});

        try {
            const response = await anonymousContext.post(new URL(staticWebhookUrl).pathname, {
                data: {visibility: true},
            });

            expect(response.status(), await response.text()).toBeLessThan(300);
        } finally {
            await anonymousContext.dispose();
        }
    }

    test('a user-initiated run is refused while a connection it uses is PENDING_REASSIGNMENT and allowed again after reassignment', async () => {
        const {connectionId, owner} = await createPendingConnectionOwner();

        const deployment = await deployHttpClientProject(adminApi, workspaceId, connectionId);

        const activeResponse = await runDeployedWorkflowResponse(adminApi, deployment);

        expect(activeResponse.ok(), await activeResponse.text()).toBeTruthy();

        await markPending(owner, connectionId);

        const blockedResponse = await runDeployedWorkflowResponse(adminApi, deployment);

        expect(blockedResponse.ok()).toBeFalsy();
        expect(await blockedResponse.text()).toContain('Workflow execution blocked');

        expectNoGraphQlErrors(await reassignConnection(adminApi, workspaceId, connectionId, 'admin'));

        const reactivatedResponse = await runDeployedWorkflowResponse(adminApi, deployment);

        expect(reactivatedResponse.ok(), await reactivatedResponse.text()).toBeTruthy();
    });

    test('a trigger-initiated run is not started while a connection it uses is PENDING_REASSIGNMENT', async () => {
        const {connectionId, owner} = await createPendingConnectionOwner();

        const deployment = await deployHttpClientProject(adminApi, workspaceId, connectionId, WEBHOOK_TRIGGER);

        await callWebhook(deployment.projectDeploymentId);

        await expect
            .poll(async () => (await listDeploymentJobs(adminApi, workspaceId, deployment)).length, {timeout: 30000})
            .toBe(1);

        expect((await listDeploymentJobs(adminApi, workspaceId, deployment))[0].taskExecutionCount).toBe(1);

        await markPending(owner, connectionId);

        await callWebhook(deployment.projectDeploymentId);

        await expect
            .poll(async () => (await listDeploymentJobs(adminApi, workspaceId, deployment)).length, {timeout: 30000})
            .toBe(2);

        expect((await listDeploymentJobs(adminApi, workspaceId, deployment))[1]).toMatchObject({
            status: 'FAILED',
            taskExecutionCount: 0,
        });

        expectNoGraphQlErrors(await reassignConnection(adminApi, workspaceId, connectionId, 'admin'));

        await callWebhook(deployment.projectDeploymentId);

        await expect
            .poll(async () => (await listDeploymentJobs(adminApi, workspaceId, deployment)).length, {timeout: 30000})
            .toBe(3);

        expect((await listDeploymentJobs(adminApi, workspaceId, deployment))[2].taskExecutionCount).toBe(1);
    });

    test('a test run in the workflow editor fails at the task that uses a PENDING_REASSIGNMENT connection', async () => {
        const {connectionId, owner} = await createPendingConnectionOwner();

        const {workflowId} = await createHttpClientProject(adminApi, workspaceId);

        expectNoGraphQlErrors(await saveTestConfigurationConnection(adminApi, workflowId, connectionId));

        await markPending(owner, connectionId);

        const response = await sendInternalApi(
            adminApi,
            'POST',
            `/api/platform/internal/workflows/${workflowId}/tests?environmentId=0`,
            {}
        );

        const body = await response.text();

        expect(response.ok(), body).toBeTruthy();
        expect(body).toContain('FAILED');
        expect(body).toContain('has status PENDING_REASSIGNMENT and cannot be used for execution');
    });

    test('a deployment refuses a connection from a different environment', async () => {
        const connectionId = await createConnection(adminApi, workspaceId, {environmentId: 0});

        const {projectId} = await createHttpClientProject(adminApi, workspaceId);

        const {response} = await deployHttpClientProjectResponse(adminApi, projectId, connectionId, 2);

        expect(response.ok()).toBeFalsy();
        expect(await response.text()).toContain('environment does not match deployment environment PRODUCTION');
    });
});
