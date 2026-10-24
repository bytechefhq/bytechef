import {type Page, expect, mergeTests} from '@playwright/test';

import {loginTest} from '../../fixtures';
import {CopilotPanel} from '../../pages/copilotPanel';
import {answerInteractiveCards, readXsrfToken} from '../../utils/aiHubUtils';
import {enableCopilotInApplicationInfo, hideReactQueryDevtools} from '../../utils/copilotUtils';
import getRandomString from '../../utils/getRandomString';

const test = mergeTests(loginTest());

const LIVE_TURN_TIMEOUT = 840000;

const TURN_START_TIMEOUT = 15000;

const EMBEDDED_API_PATH = '/api/embedded/internal';

interface IntegrationWorkflowI {
    definition?: string;
    id: string;
    integrationWorkflowId: number;
}

async function createIntegration(page: Page, name: string): Promise<number> {
    const response = await page.request.post(`${EMBEDDED_API_PATH}/integrations`, {
        data: {componentName: 'slack', multipleInstances: false, name},
        headers: {'X-XSRF-TOKEN': await readXsrfToken(page)},
    });

    expect(response.ok(), `creating integration ${name} answered ${response.status()}: ${await response.text()}`).toBe(
        true
    );

    const body = (await response.json()) as number | {id: number};

    return typeof body === 'number' ? body : body.id;
}

async function listIntegrationWorkflows(page: Page, integrationId: number): Promise<IntegrationWorkflowI[]> {
    const response = await page.request.get(`${EMBEDDED_API_PATH}/integrations/${integrationId}/workflows`);

    expect(response.ok(), `listing the workflows of integration ${integrationId}`).toBe(true);

    return (await response.json()) as IntegrationWorkflowI[];
}

async function readNodeTypes(page: Page, integrationId: number): Promise<string[]> {
    const [integrationWorkflow] = await listIntegrationWorkflows(page, integrationId);
    const definition = JSON.parse(integrationWorkflow.definition ?? '{}') as {
        tasks?: Array<{type: string}>;
        triggers?: Array<{type: string}>;
    };

    return [...(definition.triggers ?? []), ...(definition.tasks ?? [])].map((node) => node.type);
}

test.describe('@live Embedded Copilot', () => {
    test.skip(!process.env.PLAYWRIGHT_LIVE, 'Set PLAYWRIGHT_LIVE=1 to run against a live server and model');

    test.describe.configure({timeout: 1200000});

    test('BUILD in the integration workflow editor builds a webhook to Slack workflow', async ({
        authenticatedPage: page,
    }) => {
        await hideReactQueryDevtools(page);

        await enableCopilotInApplicationInfo(page);

        await page.goto('/embedded/integrations');

        const integrationId = await createIntegration(page, `embedded_copilot_${getRandomString().slice(0, 8)}`);

        try {
            const workflowResponse = await page.request.post(
                `${EMBEDDED_API_PATH}/integrations/${integrationId}/workflows`,
                {
                    data: {definition: JSON.stringify({label: 'Signup Alert', tasks: []})},
                    headers: {'X-XSRF-TOKEN': await readXsrfToken(page)},
                }
            );

            expect(workflowResponse.ok(), `adding a workflow answered ${workflowResponse.status()}`).toBe(true);

            const [integrationWorkflow] = await listIntegrationWorkflows(page, integrationId);

            await page.goto(
                `/embedded/integrations/${integrationId}/integration-workflows/${integrationWorkflow.integrationWorkflowId}`
            );

            const copilotPanel = new CopilotPanel(page);

            await copilotPanel.openFromWorkflowEditor();
            await copilotPanel.setBuildMode(true);

            const waitForTurnEnd = async () => {
                await expect(copilotPanel.stopGeneratingButton)
                    .toBeVisible({timeout: TURN_START_TIMEOUT})
                    .catch(() => undefined);

                await expect(copilotPanel.stopGeneratingButton).toBeHidden({timeout: LIVE_TURN_TIMEOUT});

                await expect(copilotPanel.sendButton).toBeVisible();
            };

            await copilotPanel.send(
                'Build this workflow: when a webhook receives a new signup (name, email), post a message about it to ' +
                    'the #signups Slack channel.'
            );

            await waitForTurnEnd();

            await answerInteractiveCards(page, {
                readLastMessage: () => copilotPanel.assistantMessages.last(),
                waitForTurnEnd,
            });

            await expect(copilotPanel.runErrors).toHaveCount(0);

            await expect
                .poll(() => readNodeTypes(page, integrationId), {timeout: 30000})
                .toEqual(expect.arrayContaining([expect.stringMatching(/^slack\/v1\/sendChannelMessage$/)]));

            expect(
                await listIntegrationWorkflows(page, integrationId),
                'the build stayed in one workflow'
            ).toHaveLength(1);
        } finally {
            await page.request.delete(`${EMBEDDED_API_PATH}/integrations/${integrationId}`, {
                headers: {'X-XSRF-TOKEN': await readXsrfToken(page)},
            });
        }
    });
});
