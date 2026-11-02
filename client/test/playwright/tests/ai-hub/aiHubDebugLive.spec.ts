import {type Page, expect, mergeTests} from '@playwright/test';

import {loginTest} from '../../fixtures';
import {AiHubPage} from '../../pages/aiHubPage';
import {CopilotPanel} from '../../pages/copilotPanel';
import {WorkflowPage} from '../../pages/workflowPage';
import {
    calledToolNames,
    createEnabledProjectDeploymentThroughApi,
    createProjectThroughApi,
    createProjectWorkflowThroughApi,
    deleteProjectDeploymentThroughApi,
    deleteProjectThroughApi,
    listProjectWorkflowIds,
    publishProjectThroughApi,
    readAppEnvironmentId,
    readChatToolEvents,
    readWorkflowNodes,
    readXsrfToken,
    waitForHubWorkspaceId,
} from '../../utils/aiHubUtils';
import {enableCopilotInApplicationInfo, hideReactQueryDevtools} from '../../utils/copilotUtils';
import getRandomString from '../../utils/getRandomString';

const test = mergeTests(loginTest());

const LIVE_TURN_TIMEOUT = 840000;

const TURN_START_TIMEOUT = 15000;

const AUTOMATION_API_PATH = '/api/automation/internal';

const WORKFLOW_LABEL = 'Sync orders';

const FAILING_WORKFLOW = {
    description: 'Fetches the latest orders',
    inputs: [],
    label: WORKFLOW_LABEL,
    tasks: [
        {
            label: 'Fetch orders',
            name: 'httpClient_1',
            parameters: {responseType: {responseFormat: 'JSON'}, uri: 'https://api.example.com/orders'},
            type: 'httpClient/v1/get',
        },
    ],
    triggers: [],
};

const DIAGNOSIS = /responseType/;

const APPLY_THE_FIX = 'Yes, apply the fix.';

interface FailedRunI {
    draftProjectWorkflowId: number;
    environmentId: number;
    projectDeploymentId: number;
    projectId: number;
    projectName: string;
    workspaceId: number;
}

/**
 * Deploys a workflow whose HTTP step has a malformed responseType, runs it once and waits until the run is listed as
 * failed. The draft left after publishing is the workflow the builders are asked to fix.
 */
async function seedFailedRun(page: Page): Promise<FailedRunI> {
    await hideReactQueryDevtools(page);

    const hubWorkspaceId = waitForHubWorkspaceId(page);

    await new AiHubPage(page).goto();

    const workspaceId = await hubWorkspaceId;
    const environmentId = await readAppEnvironmentId(page);
    const projectName = `debug_sample_${getRandomString().slice(0, 8)}`;
    const projectId = await createProjectThroughApi(page, {name: projectName, workspaceId});

    await createProjectWorkflowThroughApi(page, {definition: FAILING_WORKFLOW, projectId});

    const [publishedWorkflowId] = await listProjectWorkflowIds(page, projectId);

    await publishProjectThroughApi(page, projectId, 'v1');

    const projectDeploymentId = await createEnabledProjectDeploymentThroughApi(page, {
        connections: [],
        environmentId,
        name: projectName,
        projectId,
        projectVersion: 1,
        workflowId: publishedWorkflowId,
    });

    const runResponse = await page.request.post(
        `${AUTOMATION_API_PATH}/project-deployments/${projectDeploymentId}/workflows/${publishedWorkflowId}/jobs`,
        {headers: {'X-XSRF-TOKEN': await readXsrfToken(page)}}
    );

    expect(runResponse.ok(), `running the deployed workflow answered ${runResponse.status()}`).toBe(true);

    await expect
        .poll(
            async () => {
                const response = await page.request.get(
                    `${AUTOMATION_API_PATH}/workspaces/${workspaceId}/workflow-executions?projectId=${projectId}` +
                        `&jobStatus=FAILED&environmentId=${environmentId}`
                );

                return response.ok() ? (((await response.json()) as {content?: unknown[]}).content ?? []).length : 0;
            },
            {message: 'the deployed run failed', timeout: 60000}
        )
        .toBeGreaterThan(0);

    const draftResponse = await page.request.get(`${AUTOMATION_API_PATH}/projects/${projectId}/workflows`);
    const [draftWorkflow] = (await draftResponse.json()) as Array<{projectWorkflowId: number}>;

    return {
        draftProjectWorkflowId: draftWorkflow.projectWorkflowId,
        environmentId,
        projectDeploymentId,
        projectId,
        projectName,
        workspaceId,
    };
}

async function readDraftResponseType(page: Page, projectId: number): Promise<unknown> {
    const [draftWorkflowId] = await listProjectWorkflowIds(page, projectId);
    const workflowNodes = await readWorkflowNodes(page, draftWorkflowId);

    return workflowNodes.find((workflowNode) => workflowNode.name === 'httpClient_1')?.parameters.responseType;
}

async function cleanUp(page: Page, failedRun: FailedRunI | undefined): Promise<void> {
    if (!failedRun) {
        return;
    }

    await deleteProjectDeploymentThroughApi(page, failedRun.projectDeploymentId);

    await deleteProjectThroughApi(page, failedRun.projectId);
}

test.describe('@live Debugging a failed run', () => {
    test.skip(!process.env.PLAYWRIGHT_LIVE, 'Set PLAYWRIGHT_LIVE=1 to run against a live server and model');

    test.describe.configure({timeout: 1200000});

    test('the AI Hub explains a failed run, changes nothing until told, then applies the fix', async ({
        authenticatedPage: page,
    }) => {
        let failedRun: FailedRunI | undefined;

        try {
            failedRun = await seedFailedRun(page);

            const aiHubPage = new AiHubPage(page);

            await aiHubPage.goto();
            await aiHubPage.setBuildMode(true);

            await test.step('Diagnose without changing anything', async () => {
                await aiHubPage.sendAndWaitForTurnEnd(
                    `Debug the latest failed execution of the "${WORKFLOW_LABEL}" workflow in the ` +
                        `${failedRun!.projectName} project. Explain what failed, then suggest a fix before changing ` +
                        'anything.',
                    LIVE_TURN_TIMEOUT
                );

                await expect(aiHubPage.runErrors).toHaveCount(0);

                await expect(aiHubPage.assistantMessages.last()).toContainText(DIAGNOSIS);

                const toolNames = calledToolNames(
                    await readChatToolEvents(page, {id: aiHubPage.currentChatId(), workspaceId: failedRun!.workspaceId})
                );

                expect(toolNames).toEqual(expect.arrayContaining(['listWorkflowExecutions', 'getWorkflowExecution']));
                expect(toolNames, 'nothing was changed before the user agreed').not.toContain('buildWorkflow');

                expect(await readDraftResponseType(page, failedRun!.projectId)).toEqual({responseFormat: 'JSON'});
            });

            await test.step('Apply the fix once told to', async () => {
                await aiHubPage.sendAndWaitForTurnEnd(APPLY_THE_FIX, LIVE_TURN_TIMEOUT);

                await expect(aiHubPage.runErrors).toHaveCount(0);

                expect(await readDraftResponseType(page, failedRun!.projectId)).toBe('JSON');
            });
        } finally {
            await cleanUp(page, failedRun);
        }
    });

    test('the Copilot explains a failed run, changes nothing until told, then applies the fix', async ({
        authenticatedPage: page,
    }) => {
        let failedRun: FailedRunI | undefined;

        try {
            failedRun = await seedFailedRun(page);

            await enableCopilotInApplicationInfo(page);

            await new WorkflowPage(page).goToWorkflowEditor(
                String(failedRun.projectId),
                String(failedRun.draftProjectWorkflowId)
            );

            const copilotPanel = new CopilotPanel(page);

            await copilotPanel.openFromWorkflowEditor();
            await copilotPanel.setBuildMode(true);

            const liveTurn = async (text: string) => {
                await copilotPanel.send(text);

                await expect(copilotPanel.stopGeneratingButton)
                    .toBeVisible({timeout: TURN_START_TIMEOUT})
                    .catch(() => undefined);

                await expect(copilotPanel.stopGeneratingButton).toBeHidden({timeout: LIVE_TURN_TIMEOUT});

                await expect(copilotPanel.sendButton).toBeVisible();

                await expect(copilotPanel.runErrors).toHaveCount(0);
            };

            await test.step('Diagnose without changing anything', async () => {
                await liveTurn(
                    'Debug the latest failed execution of this workflow. Explain what failed, then suggest a fix ' +
                        'before changing anything.'
                );

                await expect(copilotPanel.assistantMessages.last()).toContainText(DIAGNOSIS);

                expect(await readDraftResponseType(page, failedRun!.projectId)).toEqual({responseFormat: 'JSON'});
            });

            await test.step('Apply the fix once told to', async () => {
                await liveTurn(APPLY_THE_FIX);

                await expect
                    .poll(() => readDraftResponseType(page, failedRun!.projectId), {timeout: 30000})
                    .toBe('JSON');
            });
        } finally {
            await cleanUp(page, failedRun);
        }
    });
});
