import {type Page, expect, mergeTests} from '@playwright/test';

import {aiHubChatsTest, loginTest, projectTest, workflowTest} from '../../fixtures';
import {AiHubPage} from '../../pages/aiHubPage';
import {WorkflowPage} from '../../pages/workflowPage';
import {
    AI_HUB_CHAT_URL,
    completedTurn,
    lastMessageOf,
    stubAgUiRun,
    textMessage,
    toolCall,
} from '../../utils/agUiStreamStub';
import {
    type HubWorkflowI,
    openWorkflowTabArguments,
    openWorkflowTabResult,
    prepareAiHub,
    readHubWorkflow,
} from '../../utils/aiHubUtils';
import {stubGraphQlOperation, stubWorkspaceConnections} from '../../utils/copilotUtils';
import {type TestWorkflowI} from '../../utils/projectUtils';

export const test = mergeTests(loginTest(), aiHubChatsTest, projectTest, workflowTest);

const SLACK_CONNECTIONS = [
    {id: 7, name: 'Slack Marketing'},
    {id: 8, name: 'Slack Support'},
];

const toHubWorkflow = (page: Page, workflow: TestWorkflowI) =>
    readHubWorkflow(page, {projectId: workflow.projectId, projectWorkflowId: Number(workflow.workflowId)});

const openWorkflowTabCall = (hubWorkflow: HubWorkflowI) =>
    toolCall('openWorkflowTab', openWorkflowTabArguments(hubWorkflow), openWorkflowTabResult(hubWorkflow));

const buildTurn = (hubWorkflow: HubWorkflowI) =>
    completedTurn(
        textMessage('Creating the project and the workflow.'),
        toolCall('createProject', {name: 'Demo'}, JSON.stringify({id: Number(hubWorkflow.projectId), name: 'Demo'})),
        toolCall(
            'createProjectWorkflow',
            {label: hubWorkflow.name, projectId: Number(hubWorkflow.projectId)},
            JSON.stringify({
                projectId: Number(hubWorkflow.projectId),
                projectWorkflowId: hubWorkflow.projectWorkflowId,
                workflowId: hubWorkflow.workflowId,
            })
        ),
        toolCall(
            'buildWorkflow',
            {instructions: 'Post hello to Slack every morning', workflowId: hubWorkflow.workflowId},
            JSON.stringify({projectId: hubWorkflow.projectId, summary: 'Schedule trigger and a Slack message task'})
        ),
        openWorkflowTabCall(hubWorkflow),
        textMessage('The workflow is ready and open on the right.')
    );

test.describe('AI Hub building a workflow', () => {
    test('a build opens the workflow in the right panel', async ({authenticatedPage: page, workflow}) => {
        await prepareAiHub(page);

        const hubWorkflow = await toHubWorkflow(page, workflow);

        await stubAgUiRun(page, AI_HUB_CHAT_URL, [buildTurn(hubWorkflow)]);

        const aiHubPage = new AiHubPage(page);

        await aiHubPage.goto();

        await test.step('Ask for the build', async () => {
            await aiHubPage.sendAndWaitForTurnEnd(
                'Create a project called Demo with a workflow that posts hello to Slack every morning'
            );

            await expect(page.getByText('The workflow is ready and open on the right.')).toBeVisible();
        });

        await test.step('Assert the workflow tab and its editor', async () => {
            await expect(aiHubPage.workflowTab(hubWorkflow.name)).toBeVisible();

            await new WorkflowPage(page).waitForCanvasReady();
        });

        await test.step('Assert the tool calls when shown', async () => {
            await aiHubPage.chatOptionsButton.click();

            await page.getByRole('menuitem', {name: 'Show tool calls'}).click();

            for (const toolName of ['createProject', 'createProjectWorkflow', 'buildWorkflow']) {
                await expect(page.getByRole('button', {name: new RegExp(`^${toolName}\\b`)})).toBeVisible();
            }

            await expect(page.getByRole('button', {name: new RegExp(`${hubWorkflow.name}\\s*Workflow`)})).toBeVisible();
        });
    });

    test('opening the same workflow twice focuses the existing tab', async ({authenticatedPage: page, workflow}) => {
        await prepareAiHub(page);

        const hubWorkflow = await toHubWorkflow(page, workflow);

        await stubAgUiRun(page, AI_HUB_CHAT_URL, [
            completedTurn(openWorkflowTabCall(hubWorkflow), textMessage('Opened it.')),
            completedTurn(openWorkflowTabCall(hubWorkflow), textMessage('Opened it again.')),
        ]);

        const aiHubPage = new AiHubPage(page);

        await aiHubPage.goto();

        await aiHubPage.sendAndWaitForTurnEnd('Open my workflow');

        await expect(aiHubPage.workflowTab(hubWorkflow.name)).toHaveCount(1);

        await aiHubPage.sendAndWaitForTurnEnd('Open it again');

        await expect(page.getByText('Opened it again.')).toBeVisible();

        await expect(aiHubPage.workflowTab(hubWorkflow.name)).toHaveCount(1);
    });

    test('two Slack connections render a select-connection card whose pick is sent and bound', async ({
        authenticatedPage: page,
    }) => {
        await prepareAiHub(page);

        await stubWorkspaceConnections(page, SLACK_CONNECTIONS);

        const hubRun = await stubAgUiRun(page, AI_HUB_CHAT_URL, [
            completedTurn(
                textMessage('This workflow needs a Slack connection.'),
                toolCall(
                    'selectConnection',
                    {componentName: 'slack'},
                    JSON.stringify({
                        awaitingAnswer: true,
                        componentLabel: 'Slack',
                        componentName: 'slack',
                        kind: 'select-connection',
                    })
                )
            ),
            completedTurn(
                toolCall(
                    'saveWorkflowTestConnection',
                    {connectionId: 8, connectionKey: 'slack', workflowNodeName: 'slack_1'},
                    JSON.stringify("Connection 8 has been bound to node 'slack_1' for testing.")
                ),
                textMessage('Slack Support is bound.')
            ),
        ]);

        const aiHubPage = new AiHubPage(page);

        await aiHubPage.goto();

        const connectionPicker = page.getByRole('combobox');

        await test.step('Assert the card at the end of the turn', async () => {
            await aiHubPage.sendAndWaitForTurnEnd('Post new leads to Slack');

            await expect(connectionPicker).toBeVisible();

            await expect(aiHubPage.composerInput).toBeEnabled();
        });

        await test.step('Pick a connection', async () => {
            await connectionPicker.click();

            await expect(page.getByRole('option', {name: 'Slack Marketing'})).toBeVisible();

            await page.getByRole('option', {name: 'Slack Support'}).click();

            await expect(page.getByText('Picked: Slack Support', {exact: true})).toBeVisible();
        });

        await test.step('Assert the pick is sent and bound', async () => {
            expect(lastMessageOf(await hubRun.waitForRequest(1))).toMatchObject({
                content: 'User picked: Slack Support (ID: 8)',
                role: 'user',
            });

            await expect(page.getByText('Slack Support is bound.')).toBeVisible();
        });
    });

    test('no Slack connection renders a create-connection card', async ({authenticatedPage: page}) => {
        await prepareAiHub(page);

        await stubWorkspaceConnections(page, []);

        await stubAgUiRun(page, AI_HUB_CHAT_URL, [
            completedTurn(
                toolCall(
                    'createConnection',
                    {componentName: 'slack'},
                    JSON.stringify({
                        awaitingAnswer: true,
                        componentLabel: 'Slack',
                        componentName: 'slack',
                        kind: 'create-connection',
                        suggestedName: 'Slack',
                    })
                )
            ),
        ]);

        const aiHubPage = new AiHubPage(page);

        await aiHubPage.goto();

        await aiHubPage.sendAndWaitForTurnEnd('Post new leads to Slack');

        const connectButton = page.getByRole('button', {name: 'Connect Slack'});

        await expect(connectButton).toBeVisible();

        await connectButton.click();

        await expect(page.getByRole('dialog')).toBeVisible();
    });

    test('an empty lookup parameter renders a select-property-option card', async ({authenticatedPage: page}) => {
        await prepareAiHub(page);

        const hubRun = await stubAgUiRun(page, AI_HUB_CHAT_URL, [
            completedTurn(
                toolCall(
                    'selectPropertyOption',
                    {componentName: 'slack', propertyName: 'channel'},
                    JSON.stringify({
                        awaitingAnswer: true,
                        componentName: 'slack',
                        kind: 'select-property-option',
                        options: [
                            {label: '#general', value: 'C001'},
                            {label: '#random', value: 'C002'},
                        ],
                        propertyName: 'channel',
                    })
                )
            ),
            completedTurn(textMessage('Messages go to #random.')),
        ]);

        const aiHubPage = new AiHubPage(page);

        await aiHubPage.goto();

        await aiHubPage.sendAndWaitForTurnEnd('Post to a Slack channel');

        await page.getByRole('combobox').click();

        await expect(page.getByRole('option', {name: '#general'})).toBeVisible();

        await page.getByRole('option', {name: '#random'}).click();

        expect(lastMessageOf(await hubRun.waitForRequest(1))).toMatchObject({
            content: 'User picked: #random (value: C002)',
            role: 'user',
        });

        await expect(page.getByText('Messages go to #random.')).toBeVisible();
    });

    test('an ambiguous build asks a question in the hub and the answer continues the build', async ({
        authenticatedPage: page,
    }) => {
        await prepareAiHub(page);

        const hubRun = await stubAgUiRun(page, AI_HUB_CHAT_URL, [
            completedTurn(
                toolCall(
                    'buildWorkflow',
                    {instructions: 'Send alerts'},
                    JSON.stringify({
                        awaitingAnswer: true,
                        kind: 'ask-user-question',
                        questions: [
                            {
                                multiSelect: false,
                                options: [{label: 'Engineering'}, {label: 'Support'}],
                                question: 'Which team should receive the alerts?',
                            },
                        ],
                    })
                )
            ),
            completedTurn(
                toolCall(
                    'buildWorkflow',
                    {instructions: 'Send alerts to Engineering'},
                    JSON.stringify('Workflow built.')
                ),
                textMessage('Alerts go to Engineering.')
            ),
        ]);

        const aiHubPage = new AiHubPage(page);

        await aiHubPage.goto();

        await test.step('Assert the question card', async () => {
            await aiHubPage.sendAndWaitForTurnEnd('Send alerts');

            await expect(page.getByText('Which team should receive the alerts?')).toBeVisible();
        });

        await test.step('Answer and assert the build continues', async () => {
            await page.getByRole('button', {name: 'Engineering'}).click();

            expect(lastMessageOf(await hubRun.waitForRequest(1))).toMatchObject({
                content: 'User picked: Engineering',
                role: 'user',
            });

            await expect(page.getByText('Alerts go to Engineering.')).toBeVisible();

            await aiHubPage.waitForTurnEnd();

            expect(hubRun.requests).toHaveLength(2);
        });
    });

    test('the artifacts card lists created, updated and referenced workflows and opens one', async ({
        authenticatedPage: page,
        workflow,
    }) => {
        await prepareAiHub(page);

        const hubWorkflow = await toHubWorkflow(page, workflow);

        const workflowMetadata = JSON.stringify({
            projectId: hubWorkflow.projectId,
            projectWorkflowId: String(hubWorkflow.projectWorkflowId),
        });

        await stubGraphQlOperation(page, 'aiHubChatArtifactsByAiHubChat', (variables) => ({
            aiHubChatArtifactsByAiHubChat: ['WORKFLOW_CREATED', 'WORKFLOW_UPDATED', 'WORKFLOW_REFERENCED'].map(
                (kind, index) => ({
                    artifactId: hubWorkflow.workflowId,
                    artifactName: `${hubWorkflow.name} ${kind}`,
                    chatId: variables.id,
                    createdAt: Date.now() - index * 1000,
                    environmentId: 0,
                    id: String(index + 1),
                    kind,
                    metadataJson: workflowMetadata,
                    status: 'APPLIED',
                    statusChangedAt: null,
                })
            ),
        }));

        await stubAgUiRun(page, AI_HUB_CHAT_URL, [completedTurn(textMessage('Here is what I worked on.'))]);

        const aiHubPage = new AiHubPage(page);

        await aiHubPage.goto();

        await aiHubPage.sendAndWaitForTurnEnd('What did you change?');

        const artifactsCard = page
            .locator('div')
            .filter({has: page.getByRole('button', {name: /^Artifacts/})})
            .last();

        await test.step('Assert the three rows', async () => {
            await expect(page.getByRole('button', {name: /^Artifacts\s*3/})).toBeVisible();

            for (const kind of ['WORKFLOW_CREATED', 'WORKFLOW_UPDATED', 'WORKFLOW_REFERENCED']) {
                await expect(artifactsCard.getByText(`${hubWorkflow.name} ${kind}`)).toBeVisible();
            }
        });

        await test.step('Open the updated workflow from its row', async () => {
            await artifactsCard.getByText(`${hubWorkflow.name} WORKFLOW_UPDATED`).click();

            await expect(aiHubPage.workflowTab(`${hubWorkflow.name} WORKFLOW_UPDATED`)).toBeVisible();

            await new WorkflowPage(page).waitForCanvasReady();
        });
    });
});
