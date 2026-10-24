import {type Page, expect, mergeTests} from '@playwright/test';

import {loginTest, projectTest, workflowTest} from '../../fixtures';
import {CopilotPanel} from '../../pages/copilotPanel';
import {WorkflowPage} from '../../pages/workflowPage';
import {
    answerInteractiveCards,
    createDataTableThroughApi,
    deleteConnectionThroughApi,
    deleteProjectThroughApi,
    dropDataTableThroughApi,
    listConnections,
    readHubWorkflow,
    readWorkflowNodes,
} from '../../utils/aiHubUtils';
import {
    enableCopilotInApplicationInfo,
    hideReactQueryDevtools,
    waitForAppEnvironmentId,
    waitForAppWorkspaceId,
} from '../../utils/copilotUtils';
import getRandomString from '../../utils/getRandomString';

const test = mergeTests(loginTest(), projectTest, workflowTest);

const LIVE_TURN_TIMEOUT = 840000;

const TURN_START_TIMEOUT = 15000;

const shortName = (prefix: string) => `${prefix}_${getRandomString().slice(0, 8)}`;

interface LiveCopilotI {
    copilotPanel: CopilotPanel;
    environmentId: number;
    waitForTurnEnd: () => Promise<void>;
    workspaceId: number;
}

async function openLiveCopilot(page: Page, workflow: {projectId: string; workflowId: string}): Promise<LiveCopilotI> {
    await hideReactQueryDevtools(page);

    await enableCopilotInApplicationInfo(page);

    const appEnvironmentId = waitForAppEnvironmentId(page);
    const appWorkspaceId = waitForAppWorkspaceId(page);

    await new WorkflowPage(page).goToWorkflowEditor(workflow.projectId, workflow.workflowId);

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

    return {
        copilotPanel,
        environmentId: await appEnvironmentId,
        waitForTurnEnd,
        workspaceId: await appWorkspaceId,
    };
}

async function liveTurn(liveCopilot: LiveCopilotI, text: string): Promise<void> {
    await liveCopilot.copilotPanel.send(text);

    await expect(liveCopilot.copilotPanel.stopGeneratingButton).toBeVisible({timeout: 60000});

    await liveCopilot.waitForTurnEnd();

    await expect(liveCopilot.copilotPanel.runErrors).toHaveCount(0);
}

const answerCards = (page: Page, liveCopilot: LiveCopilotI) =>
    answerInteractiveCards(page, {
        readLastMessage: () => liveCopilot.copilotPanel.assistantMessages.last(),
        waitForTurnEnd: liveCopilot.waitForTurnEnd,
    });

async function readEditorWorkflowNodes(page: Page, workflow: {projectId: string; workflowId: string}) {
    const editorWorkflow = await readHubWorkflow(page, {
        projectId: workflow.projectId,
        projectWorkflowId: Number(workflow.workflowId),
    });

    return {nodes: await readWorkflowNodes(page, editorWorkflow.workflowId), workflowId: editorWorkflow.workflowId};
}

async function readBoundConnectionIds(
    page: Page,
    {environmentId, workflowId, workflowNodeName}: {environmentId: number; workflowId: string; workflowNodeName: string}
): Promise<number[]> {
    const response = await page.request.get(
        `/api/platform/internal/workflow-test-configurations/${workflowId}/workflow-nodes/${workflowNodeName}` +
            `/connections?environmentId=${environmentId}`
    );

    expect(response.ok(), `reading the connections of ${workflowNodeName} answered ${response.status()}`).toBe(true);

    return ((await response.json()) as Array<{connectionId: number}>).map((connection) => connection.connectionId);
}

test.describe('@live Copilot cards', () => {
    test.skip(!process.env.PLAYWRIGHT_LIVE, 'Set PLAYWRIGHT_LIVE=1 to run against a live server and model');

    test.describe.configure({timeout: 1200000});

    test('a question the Copilot asks is answered from its card and the answer reaches the workflow', async ({
        authenticatedPage: page,
        workflow,
    }) => {
        const liveCopilot = await openLiveCopilot(page, workflow);

        await liveTurn(
            liveCopilot,
            'Build a workflow: when a webhook receives a new signup (name, email, plan), post a message about it to ' +
                'Slack. I have not decided which Slack channel yet, so ask me which channel to use.'
        );

        const answers = await answerCards(page, liveCopilot);
        const pickedChannel = answers.find((cardAnswer) => /channel\?/i.test(cardAnswer.cardText))?.answer;

        expect(pickedChannel, 'the Copilot asked for the channel').toBeDefined();

        const {nodes} = await readEditorWorkflowNodes(page, workflow);
        const slackNode = nodes.find((workflowNode) => workflowNode.type.startsWith('slack/v1/'));

        expect(slackNode, 'the workflow posts to Slack').toBeDefined();

        expect(JSON.stringify(slackNode!.parameters)).toContain(pickedChannel!.replace(/^#/, ''));
    });

    test('Connect creates a connection from the card and the Copilot binds it to the model inside the agent', async ({
        authenticatedPage: page,
        workflow,
    }) => {
        const liveCopilot = await openLiveCopilot(page, workflow);
        const {copilotPanel, environmentId, workspaceId} = liveCopilot;
        const connectionName = shortName('mistral_copilot_card');

        expect(
            await listConnections(page, {componentName: 'mistral', environmentId, workspaceId}),
            'the Connect card only appears when no Mistral connection exists yet'
        ).toHaveLength(0);

        try {
            await liveTurn(
                liveCopilot,
                'Build a chat agent workflow that answers questions with a Mistral model (mistral-large-latest). ' +
                    'Then help me connect Mistral.'
            );

            await answerCards(page, liveCopilot);

            await test.step('Create the connection from the Connect card', async () => {
                await copilotPanel.root.getByRole('button', {name: 'Connect Mistral'}).last().click();

                const dialog = page.getByRole('dialog', {name: 'Create Connection'});

                await dialog.getByRole('textbox', {name: 'Name'}).fill(connectionName);
                await dialog.getByRole('textbox', {name: 'Token *'}).fill(`mistral-playwright-${getRandomString()}`);
                await dialog.getByRole('button', {name: 'Save'}).click();

                await expect(copilotPanel.root.getByText(/Connection ready/).last()).toBeVisible({timeout: 60000});
            });

            await test.step('The Copilot binds it to the model element', async () => {
                await liveTurn(liveCopilot, `I created the ${connectionName} connection. Bind it to the workflow.`);

                const connection = (
                    await listConnections(page, {componentName: 'mistral', environmentId, workspaceId})
                ).find((candidate) => candidate.name === connectionName);

                expect(connection, 'the card created the connection').toBeDefined();

                const {nodes, workflowId} = await readEditorWorkflowNodes(page, workflow);
                const agentNode = nodes.find((workflowNode) => workflowNode.type.startsWith('aiAgent/v1/'));

                expect(agentNode, 'the workflow has an AI Agent').toBeDefined();

                expect(
                    await readBoundConnectionIds(page, {environmentId, workflowId, workflowNodeName: agentNode!.name}),
                    'the connection is bound to the model inside the agent'
                ).toContain(connection!.id);
            });
        } finally {
            await deleteProjectThroughApi(page, Number(workflow.projectId));

            for (const connection of await listConnections(page, {
                componentName: 'mistral',
                environmentId,
                workspaceId,
            })) {
                if (connection.name === connectionName) {
                    await deleteConnectionThroughApi(page, connection.id);
                }
            }
        }
    });

    test('a property option card lists the real data tables and the pick reaches the workflow', async ({
        authenticatedPage: page,
        workflow,
    }) => {
        const liveCopilot = await openLiveCopilot(page, workflow);
        const {copilotPanel, environmentId, workspaceId} = liveCopilot;
        const tableNames = [shortName('copilot_ledger'), shortName('copilot_archive')];
        const tableIds: string[] = [];

        try {
            for (const tableName of tableNames) {
                tableIds.push(
                    await createDataTableThroughApi(page, {
                        baseName: tableName,
                        columns: [{name: 'note', type: 'STRING'}],
                        environmentId,
                        workspaceId,
                    })
                );
            }

            await liveTurn(
                liveCopilot,
                'Build a workflow: a manual trigger, then add a record with a "note" field set to "hello" to one of ' +
                    'my existing data tables. Do not create a table; let me pick which existing data table to use.'
            );

            const pickedTableName = tableNames[1];
            const lastMessage = copilotPanel.assistantMessages.last();
            const optionPicker = lastMessage.getByRole('combobox');

            if ((await optionPicker.count()) > 0) {
                await optionPicker.first().click();

                await page.getByRole('option', {name: pickedTableName}).click();
            } else {
                await lastMessage.getByRole('button', {exact: true, name: pickedTableName}).click();
            }

            await liveCopilot.waitForTurnEnd();

            const {nodes} = await readEditorWorkflowNodes(page, workflow);
            const createRecordsNode = nodes.find((workflowNode) => workflowNode.type === 'dataTable/v1/createRecords');

            expect(createRecordsNode?.parameters.table).toBe(pickedTableName);
        } finally {
            for (const tableId of tableIds) {
                await dropDataTableThroughApi(page, {environmentId, tableId});
            }
        }
    });
});
