import {type APIRequestContext, expect, request as playwrightRequest} from '@playwright/test';

import {workspaceRolesTest as test} from '../../fixtures/workspaceRoles';
import getRandomString from '../../utils/getRandomString';
import {
    RBAC_BASE_URL,
    type RbacWorkspaceI,
    createRoleUser,
    createRoleUserApiContext,
    deleteRoleUser,
    graphql,
    internalApi,
    postGraphQl,
} from '../../utils/rbacUtils';

interface ApprovalTaskI {
    createdBy: string;
    id: string;
    jobResumeId: string;
    name: string;
}

interface AuditEventI {
    data: {key: string; value: string}[];
    outcome: string | null;
    principal: string;
}

function approvalWorkflowDefinition(formTitle: string, triggerType: string): string {
    return JSON.stringify({
        inputs: [],
        label: 'rbac job principal',
        tasks: [
            {
                clusterElements: {
                    approvalChannels: [
                        {
                            label: 'Approval Task',
                            name: 'approvalTask_1',
                            parameters: {},
                            type: 'approvalTask/v1/approvalTask',
                        },
                    ],
                },
                label: 'Request Approval',
                name: 'approval_1',
                parameters: {formTitle},
                type: 'approval/v1/requestApproval',
            },
        ],
        triggers: [{label: 'Trigger', name: 'trigger_1', parameters: {}, type: triggerType}],
    });
}

const MANUAL_TRIGGER = 'manual/v1/manual';

const WEBHOOK_TRIGGER = 'webhook/v1/autoRespondWithHTTP200';

interface DeploymentI {
    projectDeploymentId: number;
    projectId: number;
    workflowId: string;
}

async function deployAs(
    ownerApi: APIRequestContext,
    workspaceId: number,
    formTitle: string,
    triggerType = WEBHOOK_TRIGGER
): Promise<DeploymentI> {
    const projectId = await internalApi<number>(ownerApi, 'POST', '/projects', {
        name: `rbac_job_principal_${getRandomString()}`,
        workspaceId,
    });

    await internalApi(ownerApi, 'POST', `/projects/${projectId}/workflows`, {
        definition: approvalWorkflowDefinition(formTitle, triggerType),
    });

    await internalApi(ownerApi, 'POST', `/projects/${projectId}/publish`, {description: 'job principal'});

    const workflows = await internalApi<{id: string}[]>(ownerApi, 'GET', `/projects/${projectId}/versions/1/workflows`);

    const projectDeploymentId = await internalApi<number>(ownerApi, 'POST', '/project-deployments', {
        enabled: false,
        environmentId: 0,
        name: `rbac_job_principal_${getRandomString()}`,
        projectDeploymentWorkflows: workflows.map((workflow) => ({enabled: true, workflowId: workflow.id})),
        projectId,
        projectVersion: 1,
    });

    await internalApi(ownerApi, 'PATCH', `/project-deployments/${projectDeploymentId}/enable/true`);

    return {projectDeploymentId, projectId, workflowId: workflows[0].id};
}

async function runJob(apiContext: APIRequestContext, projectDeploymentId: number, workflowId: string): Promise<number> {
    const {jobId} = await internalApi<{jobId: number}>(
        apiContext,
        'POST',
        `/project-deployments/${projectDeploymentId}/workflows/${workflowId}/jobs`
    );

    return jobId;
}

async function fetchApprovalTasks(adminApi: APIRequestContext, formTitle: string): Promise<ApprovalTaskI[]> {
    const {approvalTasks} = await graphql<{approvalTasks: ApprovalTaskI[]}>(
        adminApi,
        'query ApprovalTasks { approvalTasks { createdBy id jobResumeId name } }',
        {}
    );

    return approvalTasks.filter((approvalTask) => approvalTask.name === formTitle);
}

async function fetchCreateApprovalTaskChecks(adminApi: APIRequestContext, principal: string): Promise<AuditEventI[]> {
    const response = await postGraphQl<{auditEvents: {content: AuditEventI[]}}>(
        adminApi,
        `query AuditEvents($principal: String) {
            auditEvents(principal: $principal, eventType: "PERMISSION_CHECK", size: 200) {
                content { data { key value } outcome principal }
            }
        }`,
        {principal}
    );

    expect(response.errors, JSON.stringify(response.errors)).toBeUndefined();

    return response.data!.auditEvents.content.filter((auditEvent) =>
        auditEvent.data.some(
            (entry) => entry.key === 'method' && entry.value.endsWith('ApprovalTaskFacadeImpl.createApprovalTask')
        )
    );
}

async function callWebhook(adminApi: APIRequestContext, deployment: DeploymentI): Promise<void> {
    let staticWebhookUrl = '';

    await expect(async () => {
        const projectDeployment = await internalApi<{projectDeploymentWorkflows: {staticWebhookUrl?: string}[]}>(
            adminApi,
            'GET',
            `/project-deployments/${deployment.projectDeploymentId}`
        );

        staticWebhookUrl = projectDeployment.projectDeploymentWorkflows[0]?.staticWebhookUrl || '';

        expect(staticWebhookUrl, JSON.stringify(projectDeployment)).toContain('/webhooks/');
    }).toPass({timeout: 30000});

    const anonymousContext = await playwrightRequest.newContext({baseURL: RBAC_BASE_URL});

    try {
        const response = await anonymousContext.post(new URL(staticWebhookUrl).pathname, {data: {rbac: true}});

        expect(response.status(), await response.text()).toBeLessThan(300);
    } finally {
        await anonymousContext.dispose();
    }
}

async function countCreateApprovalTaskChecks(
    adminApi: APIRequestContext,
    principal: string,
    outcome?: string
): Promise<number> {
    const auditEvents = await fetchCreateApprovalTaskChecks(adminApi, principal);

    return auditEvents.filter((auditEvent) => !outcome || auditEvent.outcome === outcome).length;
}

test.describe('Job principal', () => {
    const cleanups: (() => Promise<void>)[] = [];

    test.afterEach(async () => {
        while (cleanups.length) {
            await cleanups.pop()!();
        }
    });

    async function newOwner(adminApi: APIRequestContext, rbacWorkspace: RbacWorkspaceI) {
        const owner = await createRoleUser(adminApi, RBAC_BASE_URL, rbacWorkspace, 'EDITOR');

        cleanups.push(() => deleteRoleUser(adminApi, owner));

        const ownerApi = await createRoleUserApiContext(RBAC_BASE_URL, owner);

        cleanups.push(() => ownerApi.dispose());

        return {owner, ownerApi};
    }

    test("an unattended run executes as the deployment's creator", async ({adminApi, rbacWorkspace}) => {
        const {owner, ownerApi} = await newOwner(adminApi, rbacWorkspace);

        const deployment = await deployAs(ownerApi, rbacWorkspace.id, `rbac_job_principal_${getRandomString()}`);

        await callWebhook(adminApi, deployment);

        await expect.poll(() => countCreateApprovalTaskChecks(adminApi, owner.login), {timeout: 60000}).toBe(1);
    });

    test("an unattended run may use the creator's DEPLOYMENT_EDIT to raise an approval task", async ({
        adminApi,
        rbacWorkspace,
    }) => {
        const {owner, ownerApi} = await newOwner(adminApi, rbacWorkspace);

        const formTitle = `rbac_job_principal_${getRandomString()}`;

        const deployment = await deployAs(ownerApi, rbacWorkspace.id, formTitle);

        await callWebhook(adminApi, deployment);

        await expect.poll(() => countCreateApprovalTaskChecks(adminApi, owner.login), {timeout: 60000}).toBe(1);

        expect(await countCreateApprovalTaskChecks(adminApi, owner.login, 'ALLOWED')).toBe(1);

        await expect
            .poll(async () => (await fetchApprovalTasks(adminApi, formTitle)).map((task) => task.createdBy), {
                timeout: 30000,
            })
            .toEqual([owner.login]);
    });

    test('an unattended run of a deployment whose creator was deleted executes as the system principal with no authority', async ({
        adminApi,
        rbacWorkspace,
    }) => {
        const owner = await createRoleUser(adminApi, RBAC_BASE_URL, rbacWorkspace, 'EDITOR');

        const ownerApi = await createRoleUserApiContext(RBAC_BASE_URL, owner);

        const formTitle = `rbac_job_principal_${getRandomString()}`;

        const deployment = await deployAs(ownerApi, rbacWorkspace.id, formTitle);

        await ownerApi.dispose();

        await graphql(adminApi, 'mutation DeleteUser($login: String!) { deleteUser(login: $login) }', {
            login: owner.login,
        });

        const deniedBefore = await countCreateApprovalTaskChecks(adminApi, 'system', 'DENIED');

        await callWebhook(adminApi, deployment);

        await expect
            .poll(() => countCreateApprovalTaskChecks(adminApi, 'system', 'DENIED'), {timeout: 60000})
            .toBe(deniedBefore + 1);

        expect(await fetchApprovalTasks(adminApi, formTitle)).toEqual([]);
    });

    test('a run started by a signed-in member executes as that member, not as the creator', async ({
        adminApi,
        rbacWorkspace,
        roleUsers,
    }) => {
        const deployment = await deployAs(
            adminApi,
            rbacWorkspace.id,
            `rbac_job_principal_${getRandomString()}`,
            MANUAL_TRIGGER
        );

        const editorApi = await createRoleUserApiContext(RBAC_BASE_URL, roleUsers.EDITOR);

        cleanups.push(() => editorApi.dispose());

        const checksBefore = await countCreateApprovalTaskChecks(adminApi, roleUsers.EDITOR.login);

        await runJob(editorApi, deployment.projectDeploymentId, deployment.workflowId);

        await expect
            .poll(() => countCreateApprovalTaskChecks(adminApi, roleUsers.EDITOR.login), {timeout: 60000})
            .toBe(checksBefore + 1);
    });
});
