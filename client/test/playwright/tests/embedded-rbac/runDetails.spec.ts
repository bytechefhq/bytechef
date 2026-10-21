import {type APIRequestContext, type Page, expect} from '@playwright/test';
import {readFileSync} from 'fs';

import {embeddedRolesTest as test} from '../../fixtures/embeddedRoles';
import {SAMPLE_WORKFLOW_PATH} from '../../utils/constants';
import {
    createConnectedUserApi,
    createTeardownAttempt,
    embeddedInternalApi,
    openAsEmbeddedRoleUser,
} from '../../utils/embeddedRbacUtils';
import getRandomString from '../../utils/getRandomString';
import {ENVIRONMENT_IDS, RBAC_BASE_URL, createRoleUserApiContext} from '../../utils/rbacUtils';

const RUN_INPUT_MARKER = `rbac_run_input_${getRandomString().slice(0, 8)}`;

const DETAILS_HIDDEN_TEXT = 'You do not have permission to see the inputs, outputs and logs of this run.';

interface EmbeddedRunI {
    configurationId: number;
    configurationName: string;
    connectedUserApi: APIRequestContext;
    connectedUserId?: number;
    integrationId: number;
    integrationInstanceId?: number;
    jobId?: number;
    signingKeyId: number;
}

interface TaskExecutionI {
    id: string;
    input?: Record<string, unknown> | null;
    output?: unknown;
}

interface WorkflowExecutionI {
    id: number;
    job: {
        inputs?: Record<string, unknown> | null;
        outputs?: Record<string, unknown> | null;
        status: string;
        taskExecutions: TaskExecutionI[];
    };
}

async function createCompletedEmbeddedRun(adminApi: APIRequestContext): Promise<EmbeddedRunI> {
    const suffix = getRandomString().slice(0, 8);

    const integrationId = await embeddedInternalApi<number>(adminApi, 'POST', '/integrations', {
        componentName: 'httpClient',
        multipleInstances: false,
        name: `rbac_run_details_${suffix}`,
    });

    const definition = JSON.parse(readFileSync(SAMPLE_WORKFLOW_PATH, 'utf-8'));

    definition.triggers = [{label: 'Request', name: 'trigger_1', type: 'request/v1/autoRespondWithHTTP200'}];

    await embeddedInternalApi(adminApi, 'POST', `/integrations/${integrationId}/workflows`, {
        definition: JSON.stringify(definition),
    });

    await embeddedInternalApi(adminApi, 'POST', `/integrations/${integrationId}/publish`, {description: 'run details'});

    const [versionWorkflow] = await embeddedInternalApi<{id: string; workflowUuid: string}[]>(
        adminApi,
        'GET',
        `/integrations/${integrationId}/versions/1/workflows`
    );

    const configurationName = `rbac_run_details_${suffix}`;

    const configurationId = await embeddedInternalApi<number>(
        adminApi,
        'POST',
        '/integration-instance-configurations',
        {
            authorizationType: 'BASIC_AUTH',
            connectionParameters: {},
            enabled: false,
            environmentId: ENVIRONMENT_IDS.DEVELOPMENT,
            integrationId,
            integrationInstanceConfigurationWorkflows: [{enabled: true, inputs: {}, workflowId: versionWorkflow.id}],
            integrationVersion: 1,
            name: configurationName,
        }
    );

    await embeddedInternalApi(adminApi, 'PATCH', `/integration-instance-configurations/${configurationId}/enable/true`);

    const {api: connectedUserApi, signingKeyId} = await createConnectedUserApi(
        adminApi,
        RBAC_BASE_URL,
        'DEVELOPMENT',
        `rbac_run_details_${suffix}`
    );

    const embeddedRun: EmbeddedRunI = {
        configurationId,
        configurationName,
        connectedUserApi,
        integrationId,
        signingKeyId,
    };

    const connectionsResponse = await connectedUserApi.get('/api/embedded/v1/connections');

    expect(connectionsResponse.status(), await connectionsResponse.text()).toBe(200);

    const instanceResponse = await connectedUserApi.post(`/api/embedded/v1/integrations/${integrationId}/instances`, {
        data: {connection: {parameters: {password: 'rbac', username: 'rbac'}}},
    });

    expect(instanceResponse.status(), await instanceResponse.text()).toBe(200);

    embeddedRun.integrationInstanceId = Number(await instanceResponse.text());

    const enableResponse = await connectedUserApi.post(
        `/api/embedded/v1/integration-instances/${embeddedRun.integrationInstanceId}/workflows/${versionWorkflow.workflowUuid}/enable`
    );

    expect(enableResponse.status(), await enableResponse.text()).toBeLessThan(300);

    const runResponse = await connectedUserApi.post(`/api/embedded/v1/workflows/${versionWorkflow.workflowUuid}`, {
        data: {rbacRunInput: RUN_INPUT_MARKER},
    });

    expect(runResponse.status(), await runResponse.text()).toBe(200);

    await expect
        .poll(
            async () => {
                const workflowExecutions = await embeddedInternalApi<{content: WorkflowExecutionI[]}>(
                    adminApi,
                    'GET',
                    `/workflow-executions?environmentId=${ENVIRONMENT_IDS.DEVELOPMENT}&integrationId=${integrationId}`
                );

                const [workflowExecution] = workflowExecutions.content;

                embeddedRun.jobId = workflowExecution?.id;

                return workflowExecution?.job.status;
            },
            {timeout: 30_000}
        )
        .toBe('COMPLETED');

    const connectedUsers = await embeddedInternalApi<{content: {externalId: string; id: number}[]}>(
        adminApi,
        'GET',
        `/connected-users?environmentId=${ENVIRONMENT_IDS.DEVELOPMENT}&search=rbac_run_details_${suffix}`
    );

    embeddedRun.connectedUserId = connectedUsers.content[0]?.id;

    return embeddedRun;
}

async function deleteEmbeddedRun(adminApi: APIRequestContext, embeddedRun: EmbeddedRunI): Promise<void> {
    const attempt = createTeardownAttempt('runDetails');

    if (embeddedRun.integrationInstanceId) {
        await attempt('delete integration instance', () =>
            embeddedRun.connectedUserApi.delete(
                `/api/embedded/v1/integration-instances/${embeddedRun.integrationInstanceId}`
            )
        );
    }

    await embeddedRun.connectedUserApi.dispose();

    await attempt('delete configuration', () =>
        embeddedInternalApi(adminApi, 'DELETE', `/integration-instance-configurations/${embeddedRun.configurationId}`)
    );

    if (embeddedRun.connectedUserId) {
        await attempt('delete connected user', () =>
            embeddedInternalApi(adminApi, 'DELETE', `/connected-users/${embeddedRun.connectedUserId}`)
        );
    }

    await attempt('delete integration', () =>
        embeddedInternalApi(adminApi, 'DELETE', `/integrations/${embeddedRun.integrationId}`)
    );

    await attempt('delete signing key', () =>
        embeddedInternalApi(adminApi, 'DELETE', `/signing-keys/${embeddedRun.signingKeyId}`)
    );
}

async function openRunTaskOutput(page: Page, configurationName: string): Promise<void> {
    await page.goto('/embedded/executions');

    await page.getByRole('cell', {exact: true, name: configurationName}).first().click({timeout: 30_000});

    const sheet = page.getByRole('dialog');

    await expect(sheet).toBeVisible();

    await sheet.getByText('Var', {exact: true}).first().click();

    await sheet.getByRole('tab', {name: 'Output'}).click();
}

test.describe('Embedded run details', () => {
    test.describe.configure({mode: 'serial'});

    let embeddedRun: EmbeddedRunI;

    test.beforeAll(async ({adminApi}) => {
        test.setTimeout(120_000);

        embeddedRun = await createCompletedEmbeddedRun(adminApi);
    });

    test.afterAll(async ({adminApi}) => {
        if (embeddedRun) {
            await deleteEmbeddedRun(adminApi, embeddedRun);
        }
    });

    test('the server sends a VIEWER the run without its step data and an EDITOR with it', async ({
        embeddedRoleUsers,
    }) => {
        const viewerApi = await createRoleUserApiContext(RBAC_BASE_URL, embeddedRoleUsers.VIEWER);
        const editorApi = await createRoleUserApiContext(RBAC_BASE_URL, embeddedRoleUsers.EDITOR);

        try {
            const viewerExecution = await embeddedInternalApi<WorkflowExecutionI>(
                viewerApi,
                'GET',
                `/workflow-executions/${embeddedRun.jobId}`
            );

            const editorExecution = await embeddedInternalApi<WorkflowExecutionI>(
                editorApi,
                'GET',
                `/workflow-executions/${embeddedRun.jobId}`
            );

            expect(editorExecution.job.status).toBe('COMPLETED');
            expect(JSON.stringify(editorExecution.job.inputs)).toContain(RUN_INPUT_MARKER);

            expect(viewerExecution.job.status).toBe('COMPLETED');
            expect(viewerExecution.job.inputs || {}).toEqual({});
            expect(viewerExecution.job.outputs ?? null).toBeNull();
            expect(JSON.stringify(viewerExecution)).not.toContain(RUN_INPUT_MARKER);

            const taskExecutionId = viewerExecution.job.taskExecutions[0].id;

            const viewerTaskExecution = await embeddedInternalApi<TaskExecutionI>(
                viewerApi,
                'GET',
                `/workflow-executions/${embeddedRun.jobId}/task-executions/${taskExecutionId}`
            );

            expect(viewerTaskExecution.input ?? null).toBeNull();
            expect(viewerTaskExecution.output ?? null).toBeNull();

            const editorTaskExecution = await embeddedInternalApi<TaskExecutionI>(
                editorApi,
                'GET',
                `/workflow-executions/${embeddedRun.jobId}/task-executions/${taskExecutionId}`
            );

            expect(editorTaskExecution.output).toMatchObject({String: 'foo'});
        } finally {
            await viewerApi.dispose();
            await editorApi.dispose();
        }
    });

    for (const roleKey of ['VIEWER', 'EDITOR'] as const) {
        const title =
            roleKey === 'VIEWER'
                ? 'a VIEWER sees the run but not its step data, and no step is fetched'
                : 'an EDITOR sees the step output';

        test(title, async ({browser, embeddedRoleUsers}) => {
            const {context, page} = await openAsEmbeddedRoleUser(
                browser,
                RBAC_BASE_URL,
                embeddedRoleUsers[roleKey],
                ENVIRONMENT_IDS.DEVELOPMENT
            );

            const taskExecutionRequests: string[] = [];

            page.on('request', (request) => {
                if (request.url().includes('/task-executions/')) {
                    taskExecutionRequests.push(request.url());
                }
            });

            try {
                await openRunTaskOutput(page, embeddedRun.configurationName);

                const sheet = page.getByRole('dialog');

                if (roleKey === 'VIEWER') {
                    await expect(sheet.getByText(DETAILS_HIDDEN_TEXT).first()).toBeVisible();
                    await expect(sheet.getByText('array foo')).toHaveCount(0);

                    await page.waitForLoadState('networkidle');

                    expect(taskExecutionRequests).toEqual([]);
                } else {
                    await expect(sheet.getByText('array foo').first()).toBeVisible();
                    await expect(sheet.getByText(DETAILS_HIDDEN_TEXT)).toHaveCount(0);

                    expect(taskExecutionRequests.length).toBeGreaterThan(0);
                }
            } finally {
                await context.close();
            }
        });
    }
});
