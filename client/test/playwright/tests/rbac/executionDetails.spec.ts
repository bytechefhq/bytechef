import {type APIRequestContext, type Page, expect} from '@playwright/test';
import {readFileSync} from 'fs';

import {workspaceRolesTest as test} from '../../fixtures/workspaceRoles';
import {SAMPLE_WORKFLOW_PATH} from '../../utils/constants';
import getRandomString from '../../utils/getRandomString';
import {
    type BuiltInRoleType,
    RBAC_BASE_URL,
    type RbacWorkspaceI,
    internalApi,
    openAsRoleUser,
} from '../../utils/rbacUtils';

const DETAILS_HIDDEN_TEXT = 'You do not have permission to see the inputs, outputs and logs of this run.';

async function createCompletedRun(adminApi: APIRequestContext, workspace: RbacWorkspaceI): Promise<string> {
    const projectName = `rbac_run_details_${getRandomString()}`;

    const projectId = await internalApi<number>(adminApi, 'POST', '/projects', {
        name: projectName,
        workspaceId: workspace.id,
    });

    await internalApi(adminApi, 'POST', `/projects/${projectId}/workflows`, {
        definition: readFileSync(SAMPLE_WORKFLOW_PATH, 'utf-8'),
    });

    await internalApi(adminApi, 'POST', `/projects/${projectId}/publish`, {description: 'run details'});

    const workflows = await internalApi<{id: string}[]>(adminApi, 'GET', `/projects/${projectId}/versions/1/workflows`);

    const projectDeploymentId = await internalApi<number>(adminApi, 'POST', '/project-deployments', {
        enabled: false,
        environmentId: 0,
        name: projectName,
        projectDeploymentWorkflows: workflows.map((workflow) => ({enabled: true, workflowId: workflow.id})),
        projectId,
        projectVersion: 1,
    });

    await internalApi(adminApi, 'PATCH', `/project-deployments/${projectDeploymentId}/enable/true`);

    const {jobId} = await internalApi<{jobId: number}>(
        adminApi,
        'POST',
        `/project-deployments/${projectDeploymentId}/workflows/${workflows[0].id}/jobs`
    );

    await expect
        .poll(
            async () => {
                const workflowExecution = await internalApi<{job: {status: string}}>(
                    adminApi,
                    'GET',
                    `/workflow-executions/${jobId}`
                );

                return workflowExecution.job.status;
            },
            {timeout: 30_000}
        )
        .toBe('COMPLETED');

    return projectName;
}

async function openRunTaskOutput(page: Page, projectName: string): Promise<void> {
    await page.goto('/automation/executions');

    const executionsTable = page.getByTestId('workflow-executions-table');

    await executionsTable.getByText(projectName).first().click();

    const sheet = page.getByRole('dialog');

    await expect(sheet).toBeVisible();

    await sheet.getByText('Var', {exact: true}).first().click();

    await sheet.getByRole('tab', {name: 'Output'}).click();
}

test.describe('Run details', () => {
    let projectName: string;

    test.beforeAll(async ({adminApi, rbacWorkspace}) => {
        projectName = await createCompletedRun(adminApi, rbacWorkspace);
    });

    const TEST_TITLES: Partial<Record<BuiltInRoleType, string>> = {
        EDITOR: 'an EDITOR sees the step output and the logs',
        VIEWER: 'a VIEWER sees the run but not its step data or logs',
    };

    for (const role of ['VIEWER', 'EDITOR'] as BuiltInRoleType[]) {
        test(TEST_TITLES[role]!, async ({browser, rbacWorkspace, roleUsers}) => {
            const {context, page} = await openAsRoleUser(browser, RBAC_BASE_URL, roleUsers[role], rbacWorkspace);

            const logRequests: string[] = [];

            page.on('request', (request) => {
                if (request.postData()?.includes('FileLogs')) {
                    logRequests.push(request.url());
                }
            });

            try {
                await openRunTaskOutput(page, projectName);

                const sheet = page.getByRole('dialog');

                if (role === 'VIEWER') {
                    await expect(sheet.getByText(DETAILS_HIDDEN_TEXT)).toBeVisible();
                    await expect(sheet.getByText('array foo')).toHaveCount(0);

                    await sheet.getByRole('tab', {name: 'Logs'}).click();

                    await expect(sheet.getByText(DETAILS_HIDDEN_TEXT)).toBeVisible();

                    expect(logRequests).toEqual([]);
                } else {
                    await expect(sheet.getByText('array foo').first()).toBeVisible();
                    await expect(sheet.getByText(DETAILS_HIDDEN_TEXT)).toHaveCount(0);

                    await sheet.getByRole('tab', {name: 'Logs'}).click();

                    await expect.poll(() => logRequests.length).toBeGreaterThan(0);
                    await expect(sheet.getByText(DETAILS_HIDDEN_TEXT)).toHaveCount(0);
                }
            } finally {
                await context.close();
            }
        });
    }
});
