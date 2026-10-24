import {type Page, expect, mergeTests} from '@playwright/test';

import {loginTest} from '../../fixtures';
import {AiHubPage} from '../../pages/aiHubPage';
import {
    type AiHubChatRefI,
    answerInteractiveCards,
    calledToolNames,
    createAnthropicConnectionThroughApi,
    createBearerTokenConnectionThroughApi,
    createDataTableThroughApi,
    deleteConnectionThroughApi,
    deleteProjectThroughApi,
    dropDataTableThroughApi,
    findProjectByName,
    listConnections,
    listProjectWorkflowIds,
    postGraphQl,
    readAppEnvironmentId,
    readChatToolEvents,
    readWorkflowNodes,
    waitForHubWorkspaceId,
} from '../../utils/aiHubUtils';
import {hideReactQueryDevtools} from '../../utils/copilotUtils';
import getRandomString from '../../utils/getRandomString';

const test = mergeTests(loginTest());

const LIVE_TURN_TIMEOUT = 840000;

interface LiveHubI {
    aiHubPage: AiHubPage;
    environmentId: number;
    workspaceId: number;
}

async function openLiveHub(page: Page): Promise<LiveHubI> {
    await hideReactQueryDevtools(page);

    const aiHubPage = new AiHubPage(page);
    const hubWorkspaceId = waitForHubWorkspaceId(page);

    await aiHubPage.goto();

    const workspaceId = await hubWorkspaceId;

    await aiHubPage.setBuildMode(true);

    return {aiHubPage, environmentId: await readAppEnvironmentId(page), workspaceId};
}

const liveTurn = async (aiHubPage: AiHubPage, text: string) => {
    await aiHubPage.sendAndWaitForTurnEnd(text, LIVE_TURN_TIMEOUT);

    await expect(aiHubPage.runErrors).toHaveCount(0);
};

const shortName = (prefix: string) => `${prefix}_${getRandomString().slice(0, 8)}`;

const currentChat = (aiHubPage: AiHubPage, workspaceId: number): Omit<AiHubChatRefI, 'threadId'> => ({
    id: aiHubPage.currentChatId(),
    workspaceId,
});

async function readProjectWorkflowNodes(page: Page, workspaceId: number, projectName: string) {
    const project = await findProjectByName(page, workspaceId, projectName);

    expect(project, `project ${projectName} was created`).toBeDefined();

    const [workflowId] = await listProjectWorkflowIds(page, project!.id);

    return readWorkflowNodes(page, workflowId);
}

async function readLastToolResults(page: Page, chat: Omit<AiHubChatRefI, 'threadId'>, toolName: string) {
    const toolEvents = await readChatToolEvents(page, chat);
    const toolCallIds = toolEvents
        .filter((toolEvent) => toolEvent.kind === 'call' && toolEvent.name === toolName)
        .map((toolEvent) => toolEvent.id);

    return toolEvents
        .filter((toolEvent) => toolEvent.kind === 'result' && toolCallIds.includes(toolEvent.id))
        .map((toolEvent) => toolEvent.response ?? '');
}

const answerCards = (page: Page, aiHubPage: AiHubPage, answerPickers = true) =>
    answerInteractiveCards(page, {
        answerPickers,
        readLastMessage: () => aiHubPage.assistantMessages.last(),
        waitForTurnEnd: () => aiHubPage.waitForTurnEnd(LIVE_TURN_TIMEOUT),
    });

async function deleteProjectByName(page: Page, workspaceId: number, projectName: string) {
    const project = await findProjectByName(page, workspaceId, projectName);

    if (project) {
        await deleteProjectThroughApi(page, project.id);
    }
}

test.describe('@live AI Hub cards', () => {
    test.skip(!process.env.PLAYWRIGHT_LIVE, 'Set PLAYWRIGHT_LIVE=1 to run against a live server and model');

    test.describe.configure({timeout: 1200000});

    test('a question the builder asks is answered from its card and the answer reaches the workflow', async ({
        authenticatedPage: page,
    }) => {
        const {aiHubPage, workspaceId} = await openLiveHub(page);
        const projectName = shortName('hub_question');

        try {
            await liveTurn(
                aiHubPage,
                `Create a new project "${projectName}" with a workflow: when a webhook receives a new signup ` +
                    '(name, email, plan), post a message about it to Slack. I have not decided which Slack channel ' +
                    'yet, so ask me which channel to use before you finish the workflow.'
            );

            const answers = await answerCards(page, aiHubPage);
            const pickedChannel = answers.find((cardAnswer) => /channel/i.test(cardAnswer.cardText))?.answer;

            expect(pickedChannel, 'the builder asked for the channel').toBeDefined();

            const workflowNodes = await readProjectWorkflowNodes(page, workspaceId, projectName);
            const slackNode = workflowNodes.find((workflowNode) => workflowNode.type.startsWith('slack/v1/'));

            expect(slackNode, 'the workflow posts to Slack').toBeDefined();

            expect(JSON.stringify(slackNode!.parameters)).toContain(pickedChannel!.replace(/^#/, ''));
        } finally {
            await deleteProjectByName(page, workspaceId, projectName);
        }
    });

    test('Connect creates a connection from the card and the agent binds it to the model inside the agent', async ({
        authenticatedPage: page,
    }) => {
        const {aiHubPage, environmentId, workspaceId} = await openLiveHub(page);
        const projectName = shortName('hub_connect');
        const connectionName = shortName('mistral_card');

        expect(
            await listConnections(page, {componentName: 'mistral', environmentId, workspaceId}),
            'the Connect card only appears when no Mistral connection exists yet'
        ).toHaveLength(0);

        try {
            await liveTurn(
                aiHubPage,
                `Create a new project "${projectName}" with a workflow: a chat agent that answers questions with a ` +
                    'Mistral model (mistral-large-latest). Then help me connect Mistral.'
            );

            await answerCards(page, aiHubPage);

            await test.step('Create the connection from the Connect card', async () => {
                await aiHubPage.assistantMessages.last().getByRole('button', {name: 'Connect Mistral'}).click();

                const dialog = page.getByRole('dialog', {name: 'Create Connection'});

                await dialog.getByRole('textbox', {name: 'Name'}).fill(connectionName);
                await dialog.getByRole('textbox', {name: 'Token *'}).fill(`mistral-playwright-${getRandomString()}`);
                await dialog.getByRole('button', {name: 'Save'}).click();

                await expect(page.getByText(/Connection ready/).last()).toBeVisible({timeout: 60000});
            });

            await test.step('The agent binds it to the model element', async () => {
                await liveTurn(aiHubPage, `I created the ${connectionName} connection. Bind it to the workflow.`);

                const bindResults = await readLastToolResults(
                    page,
                    currentChat(aiHubPage, workspaceId),
                    'saveWorkflowTestConnection'
                );

                expect(bindResults.at(-1), 'the model connection was bound').toMatch(/has been bound/);
            });
        } finally {
            await deleteProjectByName(page, workspaceId, projectName);

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
    }) => {
        const {aiHubPage, environmentId, workspaceId} = await openLiveHub(page);
        const projectName = shortName('hub_table_pick');
        const tableNames = [shortName('ledger'), shortName('archive')];
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
                aiHubPage,
                `Create a new project "${projectName}" with a workflow: a manual trigger, then add a record with a ` +
                    '"note" field set to "hello" to one of my existing data tables. Do not create a table; let me ' +
                    'pick which existing data table to use.'
            );

            const pickedTableName = tableNames[1];
            const lastMessage = aiHubPage.assistantMessages.last();
            const optionPicker = lastMessage.getByRole('combobox');

            if ((await optionPicker.count()) > 0) {
                await optionPicker.first().click();

                await page.getByRole('option', {name: pickedTableName}).click();
            } else {
                await lastMessage.getByRole('button', {exact: true, name: pickedTableName}).click();
            }

            await aiHubPage.waitForTurnEnd(LIVE_TURN_TIMEOUT);

            const workflowNodes = await readProjectWorkflowNodes(page, workspaceId, projectName);
            const createRecordsNode = workflowNodes.find(
                (workflowNode) => workflowNode.type === 'dataTable/v1/createRecords'
            );

            expect(createRecordsNode?.parameters.table).toBe(pickedTableName);

            expect(
                calledToolNames(await readChatToolEvents(page, currentChat(aiHubPage, workspaceId))),
                'the table options came from the real data tables'
            ).toEqual(
                expect.arrayContaining([expect.stringMatching(/^(lookupActionPropertyOptions|selectPropertyOption)$/)])
            );
        } finally {
            await deleteProjectByName(page, workspaceId, projectName);

            for (const tableId of tableIds) {
                await dropDataTableThroughApi(page, {environmentId, tableId});
            }
        }
    });

    test('a cluster element option card lists the real knowledge bases and the pick reaches the agent', async ({
        authenticatedPage: page,
    }) => {
        const {aiHubPage, environmentId, workspaceId} = await openLiveHub(page);
        const projectName = shortName('hub_knowledge_pick');
        const knowledgeBases: Array<{id: string; name: string}> = [];
        const openAiConnectionId = await createBearerTokenConnectionThroughApi(page, {
            componentName: 'openAi',
            environmentId,
            name: shortName('openai_card'),
            token: `sk-playwright-${getRandomString()}`,
            workspaceId,
        });

        try {
            for (const knowledgeBaseName of [shortName('handbook'), shortName('policies')]) {
                const data = await postGraphQl<{createKnowledgeBase: {id: string; name: string}}>(
                    page,
                    'mutation createKnowledgeBase($knowledgeBase: KnowledgeBaseInput!, $environmentId: ID!, ' +
                        '$workspaceId: ID!) { createKnowledgeBase(knowledgeBase: $knowledgeBase, ' +
                        'environmentId: $environmentId, workspaceId: $workspaceId) { id name } }',
                    {
                        environmentId: String(environmentId),
                        knowledgeBase: {name: knowledgeBaseName},
                        workspaceId: String(workspaceId),
                    }
                );

                knowledgeBases.push(data.createKnowledgeBase);
            }

            await liveTurn(
                aiHubPage,
                `Create a new project "${projectName}" with a workflow: a chat agent using an OpenAI model that ` +
                    'answers questions with retrieval (RAG) over one of our knowledge bases. Let me pick which ' +
                    'knowledge base.'
            );

            await answerCards(page, aiHubPage, false);

            const pickedKnowledgeBase = knowledgeBases[1];
            const optionPicker = aiHubPage.assistantMessages.last().getByRole('combobox');

            await expect(optionPicker.first()).toBeVisible();

            await optionPicker.first().click();

            await page.getByRole('option', {name: pickedKnowledgeBase.name}).click();

            await aiHubPage.waitForTurnEnd(LIVE_TURN_TIMEOUT);

            const workflowNodes = await readProjectWorkflowNodes(page, workspaceId, projectName);
            const vectorStoreNode = workflowNodes.find(
                (workflowNode) => workflowNode.type === 'knowledgeBase/v1/vectorStore'
            );

            expect(String(vectorStoreNode?.parameters.knowledgeBaseId)).toBe(pickedKnowledgeBase.id);

            expect(calledToolNames(await readChatToolEvents(page, currentChat(aiHubPage, workspaceId)))).toContain(
                'selectClusterElementPropertyOption'
            );
        } finally {
            await deleteProjectByName(page, workspaceId, projectName);

            for (const knowledgeBase of knowledgeBases) {
                await postGraphQl(page, 'mutation deleteKnowledgeBase($id: ID!) { deleteKnowledgeBase(id: $id) }', {
                    id: knowledgeBase.id,
                });
            }

            await deleteConnectionThroughApi(page, openAiConnectionId);
        }
    });

    test('a trigger option card lists the real data tables and the pick reaches the trigger', async ({
        authenticatedPage: page,
    }) => {
        const {aiHubPage, environmentId, workspaceId} = await openLiveHub(page);
        const projectName = shortName('hub_trigger_pick');
        const tableNames = [shortName('signups'), shortName('orders')];
        const tableIds: string[] = [];

        try {
            for (const tableName of tableNames) {
                tableIds.push(
                    await createDataTableThroughApi(page, {
                        baseName: tableName,
                        columns: [{name: 'email', type: 'STRING'}],
                        environmentId,
                        workspaceId,
                    })
                );
            }

            await liveTurn(
                aiHubPage,
                `Create a new project "${projectName}" with a workflow: when a record is added to one of my existing ` +
                    'data tables, log the record. Do not create a table; let me pick which existing data table ' +
                    'triggers it.'
            );

            await answerCards(page, aiHubPage, false);

            const pickedTableName = tableNames[1];
            const lastMessage = aiHubPage.assistantMessages.last();
            const optionPicker = lastMessage.getByRole('combobox');

            if ((await optionPicker.count()) > 0) {
                await optionPicker.first().click();

                await page.getByRole('option', {name: pickedTableName}).click();
            } else {
                await lastMessage.getByRole('button', {exact: true, name: pickedTableName}).click();
            }

            await aiHubPage.waitForTurnEnd(LIVE_TURN_TIMEOUT);

            const workflowNodes = await readProjectWorkflowNodes(page, workspaceId, projectName);
            const triggerNode = workflowNodes.find(
                (workflowNode) => workflowNode.type === 'dataTable/v1/recordCreated'
            );

            expect(triggerNode?.parameters.table).toBe(pickedTableName);
        } finally {
            await deleteProjectByName(page, workspaceId, projectName);

            for (const tableId of tableIds) {
                await dropDataTableThroughApi(page, {environmentId, tableId});
            }
        }
    });

    test('the connection picker binds an existing connection to the model inside an agent', async ({
        authenticatedPage: page,
    }) => {
        const {aiHubPage, environmentId, workspaceId} = await openLiveHub(page);
        const projectName = shortName('hub_model_pick');
        const connectionNames = [shortName('anthropic_a'), shortName('anthropic_b')];
        const connectionIds: number[] = [];

        try {
            for (const connectionName of connectionNames) {
                connectionIds.push(
                    await createAnthropicConnectionThroughApi(page, {
                        apiKey: `sk-ant-playwright-${getRandomString()}`,
                        environmentId,
                        name: connectionName,
                        workspaceId,
                    })
                );
            }

            await liveTurn(
                aiHubPage,
                `Create a new project "${projectName}" with a workflow: a chat agent that answers questions with an ` +
                    'Anthropic Claude model (claude-sonnet-4-5). Let me pick which Anthropic connection the model ' +
                    'uses.'
            );

            await answerCards(page, aiHubPage, false);

            const connectionPicker = page.getByRole('combobox').filter({hasText: /Choose Anthropic connection/});

            await expect(connectionPicker.last()).toBeVisible();

            await connectionPicker.last().click();

            await page.getByRole('option', {name: connectionNames[1]}).click();

            await aiHubPage.waitForTurnEnd(LIVE_TURN_TIMEOUT);

            const bindResults = await readLastToolResults(
                page,
                currentChat(aiHubPage, workspaceId),
                'saveWorkflowTestConnection'
            );

            expect(bindResults.at(-1), 'the picked connection was bound to the model').toMatch(/has been bound/);
        } finally {
            await deleteProjectByName(page, workspaceId, projectName);

            for (const connectionId of connectionIds) {
                await deleteConnectionThroughApi(page, connectionId);
            }
        }
    });
});
