import {type Page, expect, mergeTests} from '@playwright/test';

import {loginTest, projectTest, workflowTest} from '../../fixtures';
import {CopilotPanel} from '../../pages/copilotPanel';
import {WorkflowPage} from '../../pages/workflowPage';
import {
    enableCopilotInApplicationInfo,
    hideReactQueryDevtools,
    waitForAppEnvironmentId,
    waitForAppWorkspaceId,
} from '../../utils/copilotUtils';
import getRandomString from '../../utils/getRandomString';

export const test = mergeTests(loginTest(), projectTest, workflowTest);

const CONNECTIONS_API_PATH = '/api/automation/internal/connections';

async function readXsrfToken(page: Page): Promise<string> {
    const cookies = await page.context().cookies();

    return cookies.find((cookie) => cookie.name === 'XSRF-TOKEN')?.value ?? '';
}

interface ConnectionScopeI {
    environmentId: number;
    workspaceId: number;
}

async function createSlackConnection(page: Page, name: string, connectionScope: ConnectionScopeI): Promise<number> {
    const response = await page.request.post(CONNECTIONS_API_PATH, {
        data: {
            authorizationType: 'OAUTH2_AUTHORIZATION_CODE',
            componentName: 'slack',
            connectionVersion: 1,
            environmentId: connectionScope.environmentId,
            name,
            parameters: {access_token: `xoxb-playwright-${getRandomString()}`},
            workspaceId: connectionScope.workspaceId,
        },
        headers: {'X-XSRF-TOKEN': await readXsrfToken(page)},
    });

    expect(response.ok(), `creating ${name} answered ${response.status()}: ${await response.text()}`).toBe(true);

    return Number(await response.text());
}

test.describe('@live Copilot', () => {
    test.skip(!process.env.PLAYWRIGHT_LIVE, 'Set PLAYWRIGHT_LIVE=1 to run against a live server and model');

    test.describe.configure({timeout: 600000});

    test('BUILD on a real model builds a Slack workflow and shows the select-connection card', async ({
        authenticatedPage: page,
        workflow,
    }) => {
        await hideReactQueryDevtools(page);

        await enableCopilotInApplicationInfo(page);

        const workflowPage = new WorkflowPage(page);

        const appEnvironmentId = waitForAppEnvironmentId(page);
        const appWorkspaceId = waitForAppWorkspaceId(page);

        await workflowPage.goToWorkflowEditor(workflow.projectId, workflow.workflowId);

        const connectionScope = {environmentId: await appEnvironmentId, workspaceId: await appWorkspaceId};

        const slackConnectionNames = [`slack_live_${getRandomString()}`, `slack_live_${getRandomString()}`];

        const slackConnectionIds: number[] = [];

        await test.step('Create two Slack connections', async () => {
            for (const slackConnectionName of slackConnectionNames) {
                slackConnectionIds.push(await createSlackConnection(page, slackConnectionName, connectionScope));
            }
        });

        try {
            const copilotPanel = new CopilotPanel(page);

            await test.step('Ask the Copilot to build a Slack workflow', async () => {
                await copilotPanel.openFromWorkflowEditor();

                await copilotPanel.setBuildMode(true);

                await copilotPanel.send(
                    'Build a workflow that posts "Hello from ByteChef" to the #general Slack channel every morning ' +
                        'at 9:00 UTC. Let me pick the Slack connection.'
                );
            });

            await test.step('Assert the select-connection card offers both connections', async () => {
                const connectionPicker = copilotPanel.root.getByRole('combobox');

                await expect(connectionPicker).toBeVisible({timeout: 300000});

                await copilotPanel.waitForTurnEnd();

                await connectionPicker.click();

                for (const slackConnectionName of slackConnectionNames) {
                    await expect(page.getByRole('option', {name: slackConnectionName})).toBeVisible();
                }
            });
        } finally {
            for (const slackConnectionId of slackConnectionIds) {
                await page.request.delete(`${CONNECTIONS_API_PATH}/${slackConnectionId}`, {
                    headers: {'X-XSRF-TOKEN': await readXsrfToken(page)},
                });
            }
        }
    });
});
