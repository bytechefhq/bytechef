import {type Page, expect, mergeTests} from '@playwright/test';

import {aiHubChatsTest, loginTest, projectTest, workflowTest} from '../../fixtures';
import {AiHubPage} from '../../pages/aiHubPage';
import {CopilotPanel} from '../../pages/copilotPanel';
import {WorkflowPage} from '../../pages/workflowPage';
import {
    AI_HUB_PATH,
    type AiHubChatRefI,
    TURN_IN_FLIGHT_TOAST,
    type WorkflowComponentTypesI,
    calledToolNames,
    createAnthropicConnectionThroughApi,
    createEnabledProjectDeploymentThroughApi,
    createProjectThroughApi,
    createProjectWorkflowThroughApi,
    createSlackConnectionThroughApi,
    createToolApprovalRuleThroughApi,
    deleteAiHubChatThroughApi,
    deleteConnectionThroughApi,
    deleteProjectDeploymentThroughApi,
    deleteProjectThroughApi,
    deleteToolApprovalRuleThroughApi,
    findProjectByName,
    listProjectDeployments,
    listProjectWorkflowIds,
    publishProjectThroughApi,
    readAppEnvironmentId,
    readChatToolEvents,
    readDefaultGatedToolNames,
    readHubWorkflow,
    readProject,
    readRecordedToasts,
    readToolApprovalRules,
    readToolApprovals,
    readWorkflowComponentTypes,
    recordToasts,
    waitForHubWorkspaceId,
} from '../../utils/aiHubUtils';
import {
    enableCopilotInApplicationInfo,
    hideReactQueryDevtools,
    readGraphQlOperationName,
} from '../../utils/copilotUtils';
import getRandomString from '../../utils/getRandomString';
import {McpClient, type McpToolCallResultI, toolResultText} from '../../utils/mcpClient';
import workflowChatMemory from './fixtures/workflow-chat-memory.json';

export const test = mergeTests(loginTest(), aiHubChatsTest, projectTest, workflowTest);

const LIVE_TURN_TIMEOUT = 540000;

const MCP_SECRET_KEY = process.env.PLAYWRIGHT_MCP_SECRET_KEY ?? '';

const HUB_CONFIG = process.env.PLAYWRIGHT_HUB_CONFIG ?? '';

const READ_ONLY_TOOL_NAME_PATTERN = /^(list|get|search|read|find|openWorkflowTab$|select|ask)/;

const ANTHROPIC_API_KEY = process.env.PLAYWRIGHT_ANTHROPIC_API_KEY ?? '';

const WORKFLOW_CHAT_AGENT_NODE_NAME = 'aiAgent_1';

const WORKFLOW_CHAT_MODEL_NODE_NAME = 'anthropic_1';

const shortName = (prefix: string) => `${prefix}_${getRandomString().slice(0, 8)}`;

function readStructuredResult<ResultType>(toolCallResult: McpToolCallResultI): ResultType {
    expect(toolCallResult.isError ?? false, toolResultText(toolCallResult)).toBe(false);

    return (toolCallResult.structuredContent ?? JSON.parse(toolResultText(toolCallResult))) as ResultType;
}

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

    return {aiHubPage, environmentId: await readAppEnvironmentId(page), workspaceId: await hubWorkspaceId};
}

const liveTurn = (aiHubPage: AiHubPage, text: string) => aiHubPage.sendAndWaitForTurnEnd(text, LIVE_TURN_TIMEOUT);

const WORKFLOW_CHAT_SEND_INTERVAL = 2500;

/**
 * Returns a function to await before every send in one workflow chat. It waits until WORKFLOW_CHAT_SEND_INTERVAL ms
 * have passed since the previous send, because the workflow chat guard refuses a turn that starts within 2 s of the
 * previous one.
 */
const createWorkflowChatSendPacer = (page: Page) => {
    let lastSendTime: number | undefined;

    return async () => {
        if (lastSendTime !== undefined) {
            const remainingInterval = lastSendTime + WORKFLOW_CHAT_SEND_INTERVAL - Date.now();

            if (remainingInterval > 0) {
                await page.waitForTimeout(remainingInterval);
            }
        }

        lastSendTime = Date.now();
    };
};

const expectWorkflowReply = async (
    aiHubPage: AiHubPage,
    paceWorkflowChatSend: () => Promise<void>,
    text: string,
    expectedText?: string
) => {
    const assistantMessageCount = await aiHubPage.assistantMessages.count();

    await paceWorkflowChatSend();

    await liveTurn(aiHubPage, text);

    await expect(aiHubPage.runErrors).toHaveCount(0);

    await expect.poll(() => aiHubPage.assistantMessages.count()).toBeGreaterThan(assistantMessageCount);

    const lastAssistantMessage = aiHubPage.assistantMessages.last();

    await expect(lastAssistantMessage).not.toBeEmpty();

    if (expectedText) {
        await expect(lastAssistantMessage).toContainText(expectedText);
    }
};

const currentChat = (aiHubPage: AiHubPage, workspaceId: number): Omit<AiHubChatRefI, 'threadId'> => ({
    id: aiHubPage.currentChatId(),
    workspaceId,
});

const expectCalledInOrder = (toolNames: string[], expectedOrder: string[]) => {
    const positions = expectedOrder.map((toolName) => toolNames.indexOf(toolName));

    expect(positions, `called tools: ${toolNames.join(', ')}`).not.toContain(-1);

    expect(
        [...positions].sort((left, right) => left - right),
        `called tools: ${toolNames.join(', ')}`
    ).toEqual(positions);
};

const readToolResult = async (page: Page, chat: Omit<AiHubChatRefI, 'threadId'>, toolName: string) => {
    const toolEvents = await readChatToolEvents(page, chat);

    const toolCallIds = toolEvents
        .filter((toolEvent) => toolEvent.kind === 'call' && toolEvent.name === toolName)
        .map((toolEvent) => toolEvent.id);

    return toolEvents
        .filter((toolEvent) => toolEvent.kind === 'result' && toolCallIds.includes(toolEvent.id))
        .map((toolEvent) => toolEvent.response ?? '');
};

const expectSuccessfulToolResult = async (
    page: Page,
    chat: Omit<AiHubChatRefI, 'threadId'>,
    toolNames: string[]
): Promise<string> => {
    const toolResults = (await Promise.all(toolNames.map((toolName) => readToolResult(page, chat, toolName)))).flat();

    expect(toolResults.length, `none of ${toolNames.join(', ')} was called`).toBeGreaterThan(0);

    const lastToolResult = toolResults.at(-1)!;

    expect(lastToolResult, `${toolNames.join(' or ')} failed`).not.toMatch(/"error"/);

    return lastToolResult;
};

const MAX_APPROVALS_PER_TURN = 3;

const approvePendingApprovals = async (page: Page, aiHubPage: AiHubPage) => {
    const approveButton = page.getByRole('button', {name: 'Approve'});

    for (let approvalCount = 0; approvalCount < MAX_APPROVALS_PER_TURN; approvalCount++) {
        if ((await approveButton.count()) === 0) {
            return;
        }

        const approvedStatusCount = await page.getByText('Approved - the tool ran.').count();

        await approveButton.first().click();

        await expect(page.getByText('Approved - the tool ran.')).toHaveCount(approvedStatusCount + 1, {
            timeout: 60000,
        });

        await aiHubPage.waitForTurnEnd(LIVE_TURN_TIMEOUT);
    }
};

test.describe('@live AI Hub', () => {
    test.skip(!process.env.PLAYWRIGHT_LIVE, 'Set PLAYWRIGHT_LIVE=1 to run against a live server and model');

    test.describe.configure({timeout: 900000});

    test.describe('2. Chats', () => {
        test('a real turn titles the chat and a reload restores its tool-call cards', async ({
            authenticatedPage: page,
        }) => {
            const {aiHubPage} = await openLiveHub(page);

            await aiHubPage.setBuildMode(false);

            await liveTurn(aiHubPage, 'List the projects in this workspace. Use your project listing tool.');

            await expect.poll(() => aiHubPage.chatTitle.textContent(), {timeout: 120000}).not.toBe('New Chat');

            await aiHubPage.chatOptionsButton.click();

            await page.getByRole('menuitem', {name: 'Show tool calls'}).click();

            const listProjectsCard = page.getByRole('button', {name: /^listProjects/});

            await expect(listProjectsCard.first()).toBeVisible();

            await page.reload();

            await expect(listProjectsCard.first()).toBeVisible({timeout: 30000});
        });
    });

    test.describe('3. Building a workflow', () => {
        test('BUILD creates the project and workflow, opens it, and binds the picked Slack connection', async ({
            authenticatedPage: page,
        }) => {
            const {aiHubPage, environmentId, workspaceId} = await openLiveHub(page);

            const projectName = shortName('hub_live_demo');
            const slackConnectionNames = [shortName('slack_live'), shortName('slack_live')];
            const slackConnectionIds: number[] = [];

            try {
                for (const slackConnectionName of slackConnectionNames) {
                    slackConnectionIds.push(
                        await createSlackConnectionThroughApi(page, {
                            environmentId,
                            name: slackConnectionName,
                            workspaceId,
                        })
                    );
                }

                await test.step('Ask for the build', async () => {
                    await liveTurn(
                        aiHubPage,
                        `Create a project called ${projectName} with a workflow that posts "hello" to the #general ` +
                            'Slack channel every morning at 9:00. Use the Schedule and Slack components. Open the ' +
                            'workflow when it is built, and let me pick the Slack connection.'
                    );
                });

                const chat = currentChat(aiHubPage, workspaceId);

                await test.step('Assert the tool sequence and the open tab', async () => {
                    expectCalledInOrder(calledToolNames(await readChatToolEvents(page, chat)), [
                        'createProject',
                        'createProjectWorkflow',
                        'buildWorkflow',
                        'openWorkflowTab',
                    ]);

                    await expect(page.getByRole('button', {name: /^Close /}).first()).toBeVisible();

                    await new WorkflowPage(page).waitForCanvasReady();
                });

                await test.step('Pick a Slack connection from the card', async () => {
                    const connectionPicker = page.getByRole('combobox').filter({hasText: /Choose Slack connection/});

                    await expect(connectionPicker).toBeVisible();

                    await connectionPicker.click();

                    for (const slackConnectionName of slackConnectionNames) {
                        await expect(page.getByRole('option', {name: slackConnectionName})).toBeVisible();
                    }

                    await page.getByRole('option', {name: slackConnectionNames[1]}).click();

                    await aiHubPage.waitForTurnEnd(LIVE_TURN_TIMEOUT);

                    await expectSuccessfulToolResult(page, chat, ['saveWorkflowTestConnection']);
                });
            } finally {
                for (const slackConnectionId of slackConnectionIds) {
                    await deleteConnectionThroughApi(page, slackConnectionId);
                }

                const project = await findProjectByName(page, workspaceId, projectName);

                if (project) {
                    await deleteProjectThroughApi(page, project.id);
                }
            }
        });
    });

    test.describe('5. Projects and deployments', () => {
        test('list, search, create, update and publish projects from the chat', async ({authenticatedPage: page}) => {
            const {aiHubPage, workspaceId} = await openLiveHub(page);

            const projectName = shortName('hub_live_project');
            const renamedProjectName = `${projectName}_renamed`;

            try {
                await test.step('Create a project', async () => {
                    await liveTurn(aiHubPage, `Create a project called ${projectName}. Do not add any workflows.`);

                    expect(await findProjectByName(page, workspaceId, projectName)).toBeDefined();
                });

                await test.step('Rename it', async () => {
                    await liveTurn(aiHubPage, `Rename the project ${projectName} to ${renamedProjectName}.`);

                    expect(await findProjectByName(page, workspaceId, renamedProjectName)).toBeDefined();
                });

                await test.step('Search for it in Ask', async () => {
                    await aiHubPage.setBuildMode(false);

                    await liveTurn(aiHubPage, `Which of my projects have a name containing "${projectName}"?`);

                    await expect(aiHubPage.assistantMessages.last()).toContainText(renamedProjectName);

                    await aiHubPage.setBuildMode(true);
                });

                await test.step('Publish it', async () => {
                    await liveTurn(aiHubPage, `Publish the project ${renamedProjectName}.`);

                    await approvePendingApprovals(page, aiHubPage);

                    const project = await findProjectByName(page, workspaceId, renamedProjectName);

                    await expect
                        .poll(async () => (await readProject(page, project!.id))?.lastPublishedDate ?? null)
                        .not.toBeNull();
                });
            } finally {
                for (const name of [projectName, renamedProjectName]) {
                    const project = await findProjectByName(page, workspaceId, name);

                    if (project) {
                        await deleteProjectThroughApi(page, project.id);
                    }
                }
            }
        });

        test('create, enable, disable, update, roll back and promote a deployment from the chat', async ({
            authenticatedPage: page,
            project,
        }) => {
            const {aiHubPage, workspaceId} = await openLiveHub(page);

            const projectId = Number(project.id);

            await publishProjectThroughApi(page, projectId, 'First version');
            await publishProjectThroughApi(page, projectId, 'Second version');

            const chatTurn = async (text: string, toolNames: string[]) => {
                await liveTurn(aiHubPage, `${text} Do not ask questions.`);

                await approvePendingApprovals(page, aiHubPage);

                return expectSuccessfulToolResult(page, currentChat(aiHubPage, workspaceId), toolNames);
            };

            const versionChangeToolNames = ['rollbackProjectDeployment', 'updateProjectDeployment'];

            const developmentDeployment = async () =>
                (await listProjectDeployments(page, {projectId, workspaceId})).find(
                    (deployment) => (deployment.environmentId ?? 0) === 0
                );

            try {
                await test.step('Create a deployment', async () => {
                    await chatTurn(
                        `Create a deployment of project ${projectId} with project version 1 in the DEVELOPMENT environment.`,
                        ['createProjectDeployment']
                    );

                    await expect.poll(async () => (await developmentDeployment())?.projectVersion).toBe(1);
                });

                await test.step('Enable and disable it', async () => {
                    await chatTurn('Enable that deployment.', ['toggleProjectDeployment']);

                    await expect.poll(async () => (await developmentDeployment())?.enabled).toBe(true);

                    await chatTurn('Now disable that deployment.', ['toggleProjectDeployment']);

                    await expect.poll(async () => (await developmentDeployment())?.enabled).toBe(false);
                });

                await test.step('Update it and roll it back', async () => {
                    await chatTurn('Update that deployment to project version 2.', versionChangeToolNames);

                    await expect.poll(async () => (await developmentDeployment())?.projectVersion).toBe(2);

                    await chatTurn('Roll that deployment back to project version 1.', versionChangeToolNames);

                    await expect.poll(async () => (await developmentDeployment())?.projectVersion).toBe(1);
                });

                await test.step('Promote a workflow', async () => {
                    await chatTurn(`Promote project ${projectId} version 2 to the STAGING environment.`, [
                        'promoteWorkflow',
                    ]);

                    await expect
                        .poll(async () =>
                            (await listProjectDeployments(page, {projectId, workspaceId})).some(
                                (deployment) => deployment.environmentId === 1
                            )
                        )
                        .toBe(true);
                });
            } finally {
                for (const deployment of await listProjectDeployments(page, {projectId, workspaceId})) {
                    await deleteProjectDeploymentThroughApi(page, deployment.id);
                }
            }
        });
    });

    test.describe('Workflow chats', () => {
        test.skip(!ANTHROPIC_API_KEY, 'Set PLAYWRIGHT_ANTHROPIC_API_KEY for the deployed workflow model');

        test('a deployed chat workflow remembers across turns, asks a question, resumes and stops', async ({
            authenticatedPage: page,
        }) => {
            const {aiHubPage, environmentId, workspaceId} = await openLiveHub(page);

            const projectName = shortName('hub_live_wf_chat');

            const paceWorkflowChatSend = createWorkflowChatSendPacer(page);

            let connectionId: number | undefined;
            let projectId: number | undefined;
            let workflowChat: Omit<AiHubChatRefI, 'threadId'> | undefined;

            try {
                await test.step('Deploy the chat workflow', async () => {
                    connectionId = await createAnthropicConnectionThroughApi(page, {
                        apiKey: ANTHROPIC_API_KEY,
                        environmentId,
                        name: shortName('anthropic_live'),
                        workspaceId,
                    });

                    projectId = await createProjectThroughApi(page, {name: projectName, workspaceId});

                    await createProjectWorkflowThroughApi(page, {definition: workflowChatMemory, projectId});

                    const [workflowId] = await listProjectWorkflowIds(page, projectId);

                    await publishProjectThroughApi(page, projectId, 'Workflow chat');

                    await createEnabledProjectDeploymentThroughApi(page, {
                        connections: [
                            {
                                connectionId,
                                workflowConnectionKey: WORKFLOW_CHAT_MODEL_NODE_NAME,
                                workflowNodeName: WORKFLOW_CHAT_AGENT_NODE_NAME,
                            },
                        ],
                        environmentId,
                        name: projectName,
                        projectId,
                        projectVersion: 1,
                        workflowId,
                    });
                });

                await test.step('Pick the workflow from the Workflow chats cascade', async () => {
                    await aiHubPage.goto();

                    await aiHubPage.modelPickerButton.click();

                    await aiHubPage.openWorkflowChatsMenu();

                    const createWorkflowChatResponse = page.waitForResponse(
                        (response) =>
                            response.request().method() === 'POST' &&
                            response.url().endsWith('/graphql') &&
                            readGraphQlOperationName(response.request()) === 'createWorkflowChatAiHubChat'
                    );

                    await aiHubPage.workflowChatMenuItem(`${projectName} — ${workflowChatMemory.label}`).click();

                    const createWorkflowChatBody = (await (await createWorkflowChatResponse).json()) as {
                        data?: {createWorkflowChatAiHubChat?: {id: string} | null};
                    };

                    const createdChatId = createWorkflowChatBody.data?.createWorkflowChatAiHubChat?.id;

                    if (createdChatId) {
                        workflowChat = {id: Number(createdChatId), workspaceId};
                    }

                    await page.waitForURL(/\/automation\/ai-hub\/chats\/\d+$/);

                    workflowChat = currentChat(aiHubPage, workspaceId);

                    expect(String(workflowChat.id)).toBe(createdChatId);

                    await expect(aiHubPage.workflowChatBadge).toBeVisible();
                });

                await test.step('The workflow memory carries the name across turns', async () => {
                    await expectWorkflowReply(aiHubPage, paceWorkflowChatSend, 'My name is Mira.');

                    await expectWorkflowReply(aiHubPage, paceWorkflowChatSend, 'What is my name?', 'Mira');
                });

                await test.step('The workflow asks which city and the answer resumes it', async () => {
                    await paceWorkflowChatSend();

                    await liveTurn(aiHubPage, 'Plan my trip.');

                    const lisbonOption = page.getByRole('button', {exact: true, name: 'Lisbon'});
                    const otherOption = page.getByRole('button', {name: /^Other\b/i});

                    await expect(lisbonOption.or(otherOption).first()).toBeVisible();

                    if ((await lisbonOption.count()) > 0) {
                        await paceWorkflowChatSend();

                        await lisbonOption.click();
                    } else {
                        await otherOption.first().click();

                        const answerInput = page.getByPlaceholder('Type your answer…');

                        await answerInput.fill('Lisbon');

                        await paceWorkflowChatSend();

                        await answerInput.press('Enter');
                    }

                    await expect(aiHubPage.userMessage('Lisbon')).toBeVisible();

                    await aiHubPage.waitForTurnEnd(LIVE_TURN_TIMEOUT);

                    await expect(aiHubPage.runErrors).toHaveCount(0);

                    await expect(aiHubPage.assistantMessages.last()).toContainText('Lisbon');
                });

                await test.step('Stop ends a long turn and the chat accepts a new message', async () => {
                    const assistantMessageCountBeforeLongTurn = await aiHubPage.assistantMessages.count();

                    await paceWorkflowChatSend();

                    await aiHubPage.send(
                        'Write a detailed day-by-day itinerary for a three-week trip through Portugal, with at ' +
                            'least ten sentences for every day.'
                    );

                    await expect
                        .poll(() => aiHubPage.assistantMessages.count(), {timeout: LIVE_TURN_TIMEOUT})
                        .toBeGreaterThan(assistantMessageCountBeforeLongTurn);

                    await expect
                        .poll(async () => ((await aiHubPage.assistantMessages.last().textContent()) ?? '').trim(), {
                            timeout: LIVE_TURN_TIMEOUT,
                        })
                        .not.toBe('');

                    await expect(aiHubPage.stopGeneratingButton.or(aiHubPage.sendButton)).toBeVisible({
                        timeout: LIVE_TURN_TIMEOUT,
                    });

                    let stopClicked = false;

                    if (await aiHubPage.stopGeneratingButton.isVisible()) {
                        try {
                            await aiHubPage.stopGeneratingButton.click({timeout: 5000});

                            stopClicked = true;
                        } catch (error) {
                            if (await aiHubPage.stopGeneratingButton.isVisible()) {
                                throw error;
                            }
                        }
                    }

                    test.info().annotations.push({
                        description: stopClicked ? 'clicked' : 'turn finished first',
                        type: 'stop',
                    });

                    await aiHubPage.waitForTurnEnd();

                    const assistantMessageCount = await aiHubPage.assistantMessages.count();

                    let attempt = 0;

                    await expect(async () => {
                        attempt++;

                        await paceWorkflowChatSend();

                        await liveTurn(aiHubPage, `Reply with the single word OK. (attempt ${attempt})`);

                        await expect(aiHubPage.runErrors).toHaveCount(0);

                        expect(await aiHubPage.assistantMessages.count()).toBeGreaterThan(assistantMessageCount);
                    }).toPass({intervals: [10000], timeout: 180000});
                });
            } finally {
                const runCleanup = async (description: string, cleanup: () => Promise<void>) => {
                    try {
                        await cleanup();
                    } catch (error) {
                        console.warn(`Failed to ${description}:`, error);
                    }
                };

                if (workflowChat) {
                    const workflowChatToDelete = workflowChat;

                    await runCleanup('delete the workflow chat', () =>
                        deleteAiHubChatThroughApi(page, workflowChatToDelete)
                    );
                }

                if (projectId !== undefined) {
                    const projectIdToDelete = projectId;

                    let deployments: Awaited<ReturnType<typeof listProjectDeployments>> = [];

                    await runCleanup('list the project deployments', async () => {
                        deployments = await listProjectDeployments(page, {projectId: projectIdToDelete, workspaceId});
                    });

                    for (const deployment of deployments) {
                        await runCleanup(`delete the project deployment ${deployment.id}`, () =>
                            deleteProjectDeploymentThroughApi(page, deployment.id)
                        );
                    }

                    await runCleanup('delete the project', () => deleteProjectThroughApi(page, projectIdToDelete));
                }

                if (connectionId !== undefined) {
                    const connectionIdToDelete = connectionId;

                    await runCleanup('delete the connection', () =>
                        deleteConnectionThroughApi(page, connectionIdToDelete)
                    );
                }
            }
        });
    });

    test.describe('6. Tool approvals', () => {
        test.describe.configure({mode: 'default'});

        const askToDeleteProject = async (aiHubPage: AiHubPage, project: {id: string; name: string}) => {
            await liveTurn(aiHubPage, `Delete the project ${project.name} (id ${project.id}). Do not ask questions.`);
        };

        const requireDeleteProjectGate = async (page: Page, workspaceId: number) => {
            const gatedByDefault = (await readDefaultGatedToolNames(page)).includes('deleteProject');
            const rules = await readToolApprovalRules(page, workspaceId);

            test.skip(
                !gatedByDefault || rules.some((rule) => rule.toolName === 'deleteProject'),
                'deleteProject must be gated by default with no workspace rule for it'
            );
        };

        test('a delete renders an approval card, nothing runs until decided, and Approve runs it once', async ({
            authenticatedPage: page,
            project,
        }) => {
            const {aiHubPage, workspaceId} = await openLiveHub(page);

            await requireDeleteProjectGate(page, workspaceId);

            await askToDeleteProject(aiHubPage, project);

            await expect(page.getByText('Tool approval required')).toBeVisible();

            expect(await readProject(page, Number(project.id))).toBeDefined();

            await page.getByRole('button', {name: 'Approve'}).dblclick();

            await expect(page.getByText('Approved - the tool ran.')).toBeVisible({timeout: 60000});

            await aiHubPage.waitForTurnEnd(LIVE_TURN_TIMEOUT);

            await expect.poll(() => readProject(page, Number(project.id))).toBeUndefined();

            expect(
                (await readToolApprovals(page, currentChat(aiHubPage, workspaceId))).map((approval) => approval.status)
            ).toEqual(['APPROVED']);
        });

        test('Reject runs nothing and the agent is told', async ({authenticatedPage: page, project}) => {
            const {aiHubPage, workspaceId} = await openLiveHub(page);

            await requireDeleteProjectGate(page, workspaceId);

            await askToDeleteProject(aiHubPage, project);

            const assistantMessageCount = await aiHubPage.assistantMessages.count();

            await page.getByRole('button', {name: 'Reject'}).click();

            await expect(page.getByText('Rejected.')).toBeVisible();

            await aiHubPage.waitForTurnEnd(LIVE_TURN_TIMEOUT);

            await expect.poll(() => aiHubPage.assistantMessages.count()).toBeGreaterThan(assistantMessageCount);

            expect(await readProject(page, Number(project.id))).toBeDefined();

            expect(
                (await readToolApprovals(page, currentChat(aiHubPage, workspaceId))).map((approval) => approval.status)
            ).toEqual(['REJECTED']);
        });

        test('a new message withdraws the pending approval', async ({authenticatedPage: page, project}) => {
            const {aiHubPage, workspaceId} = await openLiveHub(page);

            await requireDeleteProjectGate(page, workspaceId);

            await askToDeleteProject(aiHubPage, project);

            await expect(page.getByText('Tool approval required')).toBeVisible();

            await liveTurn(aiHubPage, 'Never mind, do not delete anything.');

            expect(
                (await readToolApprovals(page, currentChat(aiHubPage, workspaceId))).map((approval) => approval.status)
            ).toEqual(['SUPERSEDED']);

            expect(await readProject(page, Number(project.id))).toBeDefined();
        });

        test('an EXEMPT rule lifts the deleteProject gate and a REQUIRE rule gates publishProject', async ({
            authenticatedPage: page,
            project,
        }) => {
            const {aiHubPage, workspaceId} = await openLiveHub(page);

            await requireDeleteProjectGate(page, workspaceId);

            const existingRules = await readToolApprovalRules(page, workspaceId);

            test.skip(
                existingRules.some((rule) => rule.toolName === 'publishProject'),
                'The workspace already holds a publishProject rule'
            );

            const gatedProjectName = shortName('hub_live_gated');
            const gatedProjectId = await createProjectThroughApi(page, {name: gatedProjectName, workspaceId});

            const ruleIds: string[] = [];

            try {
                ruleIds.push(
                    await createToolApprovalRuleThroughApi(page, {
                        mode: 'EXEMPT',
                        toolName: 'deleteProject',
                        workspaceId,
                    })
                );
                ruleIds.push(
                    await createToolApprovalRuleThroughApi(page, {
                        mode: 'REQUIRE',
                        toolName: 'publishProject',
                        workspaceId,
                    })
                );

                await test.step('Delete runs without an approval', async () => {
                    await askToDeleteProject(aiHubPage, project);

                    await expect(page.getByText('Tool approval required')).toHaveCount(0);

                    await expect.poll(() => readProject(page, Number(project.id))).toBeUndefined();
                });

                await test.step('Publish waits for an approval', async () => {
                    await liveTurn(aiHubPage, `Publish the project ${gatedProjectName} (id ${gatedProjectId}).`);

                    await expect(page.getByText('Tool approval required')).toBeVisible();

                    await expect(page.getByText('publishProject', {exact: true})).toBeVisible();

                    expect((await readProject(page, gatedProjectId))?.lastPublishedDate ?? null).toBeNull();

                    await page.getByRole('button', {name: 'Reject'}).click();

                    await aiHubPage.waitForTurnEnd(LIVE_TURN_TIMEOUT);
                });
            } finally {
                for (const ruleId of ruleIds) {
                    await deleteToolApprovalRuleThroughApi(page, {ruleId, workspaceId});
                }

                await deleteProjectThroughApi(page, gatedProjectId);
            }
        });
    });

    test.describe('7. Modes and roles', () => {
        test('ASK never mutates', async ({authenticatedPage: page, project}) => {
            const {aiHubPage, workspaceId} = await openLiveHub(page);

            await aiHubPage.setBuildMode(false);

            await liveTurn(
                aiHubPage,
                `Delete the project ${project.name} (id ${project.id}), then create a project called ${shortName('hub_live_ask')}.`
            );

            const toolNames = calledToolNames(await readChatToolEvents(page, currentChat(aiHubPage, workspaceId)));

            expect(
                toolNames.filter((toolName) => !READ_ONLY_TOOL_NAME_PATTERN.test(toolName)),
                `called tools: ${toolNames.join(', ')}`
            ).toEqual([]);

            expect(await readProject(page, Number(project.id))).toBeDefined();
        });

        test('a second tab gets "a turn is already running" and Stop cancels the running turn', async ({
            authenticatedPage: firstTab,
        }) => {
            const {aiHubPage: firstHubPage, workspaceId} = await openLiveHub(firstTab);

            const projectName = shortName('hub_live_stop');

            try {
                await test.step('Start a long build', async () => {
                    await firstHubPage.send(
                        `Create a project called ${projectName} with a workflow that every hour fetches ` +
                            'https://example.com/status.json and posts the status to the #ops Slack channel.'
                    );

                    await firstTab.waitForURL(/\/automation\/ai-hub\/chats\/\d+$/);

                    await expect(firstHubPage.stopGeneratingButton).toBeVisible();
                });

                const secondTab = await firstTab.context().newPage();

                await hideReactQueryDevtools(secondTab);

                await recordToasts(secondTab);

                const secondHubPage = new AiHubPage(secondTab);

                await test.step('Send from a second tab', async () => {
                    await secondTab.goto(new URL(firstTab.url()).pathname);

                    await expect(secondHubPage.composerInput).toBeEnabled({timeout: 30000});

                    await secondHubPage.send('Also add an error notification.');

                    await expect(secondHubPage.toasts.filter({hasText: TURN_IN_FLIGHT_TOAST})).toBeVisible();

                    expect(new URL(secondTab.url()).pathname).toMatch(/^\/automation\/ai-hub\/chats\/\d+$/);

                    expect(
                        (await readRecordedToasts(secondTab)).filter((recordedToast) => recordedToast.type === 'error')
                    ).toEqual([]);

                    await secondTab.close();
                });

                await test.step('Stop the running turn and send again', async () => {
                    await firstHubPage.stopGeneratingButton.click();

                    await firstHubPage.waitForTurnEnd();

                    const assistantMessageCount = await firstHubPage.assistantMessages.count();

                    await liveTurn(firstHubPage, 'Reply with the single word OK.');

                    await expect
                        .poll(() => firstHubPage.assistantMessages.count())
                        .toBeGreaterThan(assistantMessageCount);

                    await expect(firstHubPage.toasts.filter({hasText: TURN_IN_FLIGHT_TOAST})).toHaveCount(0);
                });
            } finally {
                const project = await findProjectByName(firstTab, workspaceId, projectName);

                if (project) {
                    await deleteProjectThroughApi(firstTab, project.id);
                }
            }
        });
    });

    test.describe('8. Parity across the three doors', () => {
        test.skip(!MCP_SECRET_KEY, 'Set PLAYWRIGHT_MCP_SECRET_KEY to the management MCP secret key');

        const PARITY_INSTRUCTION =
            'every day at 9:00, post the message "hello" to the #general Slack channel. Use the Schedule ' +
            "component's daily trigger and the Slack component's send message action, and nothing else. Do not ask " +
            'questions; choose sensible defaults.';

        test('the MCP, the Copilot and the hub build the same trigger and task types', async ({
            authenticatedPage: page,
            request,
            workflow,
        }) => {
            const builtTypes: Record<string, WorkflowComponentTypesI> = {};
            const projectIdsToDelete: number[] = [];

            const mcpClient = new McpClient(request, MCP_SECRET_KEY);

            try {
                await test.step('Build through the management MCP', async () => {
                    await mcpClient.initialize();

                    const workspaces = readStructuredResult<Array<{id: number}>>(
                        await mcpClient.callTool('listWorkspaces', {})
                    );

                    const mcpProject = readStructuredResult<{id: number}>(
                        await mcpClient.callTool('createProject', {
                            name: shortName('hub_parity_mcp'),
                            workspaceId: workspaces[0].id,
                        })
                    );

                    projectIdsToDelete.push(mcpProject.id);

                    const mcpWorkflow = readStructuredResult<{workflow_id: string}>(
                        await mcpClient.callTool('createProjectWorkflow', {
                            name: shortName('hub_parity_mcp_workflow'),
                            projectId: mcpProject.id,
                        })
                    );

                    const buildResult = await mcpClient.callTool('buildWorkflow', {
                        request: `Update the existing workflow with workflowId ${mcpWorkflow.workflow_id}: ${PARITY_INSTRUCTION}`,
                        workspaceId: workspaces[0].id,
                    });

                    expect(buildResult.isError ?? false, toolResultText(buildResult)).toBe(false);

                    builtTypes.mcp = await readWorkflowComponentTypes(page, mcpWorkflow.workflow_id);
                });

                await test.step('Build through the workflow editor Copilot', async () => {
                    await hideReactQueryDevtools(page);

                    await enableCopilotInApplicationInfo(page);

                    const workflowPage = new WorkflowPage(page);

                    await workflowPage.goToWorkflowEditor(workflow.projectId, workflow.workflowId);

                    const copilotPanel = new CopilotPanel(page);

                    await copilotPanel.openFromWorkflowEditor();

                    await copilotPanel.setBuildMode(true);

                    await copilotPanel.send(`Build this workflow: ${PARITY_INSTRUCTION}`);

                    await expect(copilotPanel.stopGeneratingButton).toBeHidden({timeout: LIVE_TURN_TIMEOUT});

                    const copilotWorkflow = await readHubWorkflow(page, {
                        projectId: workflow.projectId,
                        projectWorkflowId: Number(workflow.workflowId),
                    });

                    builtTypes.copilot = await readWorkflowComponentTypes(page, copilotWorkflow.workflowId);
                });

                await test.step('Build through the AI Hub', async () => {
                    const {aiHubPage, workspaceId} = await openLiveHub(page);

                    const hubProjectName = shortName('hub_parity_hub');

                    await liveTurn(
                        aiHubPage,
                        `Create a project called ${hubProjectName} with one workflow that, ${PARITY_INSTRUCTION}`
                    );

                    const hubProject = await findProjectByName(page, workspaceId, hubProjectName);

                    expect(hubProject).toBeDefined();

                    projectIdsToDelete.push(hubProject!.id);

                    const [hubWorkflowId] = await listProjectWorkflowIds(page, hubProject!.id);

                    builtTypes.hub = await readWorkflowComponentTypes(page, hubWorkflowId);
                });

                await test.step('Compare the three workflows', async () => {
                    expect(builtTypes.mcp.triggerTypes.length).toBeGreaterThan(0);

                    expect(builtTypes.copilot, JSON.stringify(builtTypes)).toEqual(builtTypes.mcp);

                    expect(builtTypes.hub, JSON.stringify(builtTypes)).toEqual(builtTypes.mcp);
                });
            } finally {
                for (const projectId of projectIdsToDelete) {
                    await deleteProjectThroughApi(page, projectId);
                }
            }
        });
    });
});

test.describe('@live AI Hub configuration', () => {
    test.skip(
        !process.env.PLAYWRIGHT_LIVE || !HUB_CONFIG,
        'Set PLAYWRIGHT_LIVE=1 and PLAYWRIGHT_HUB_CONFIG to on, hub-off or copilot-off to match how the server runs'
    );

    const readApplicationAiInfo = async (page: Page) => {
        const response = await page.request.get('/actuator/info');

        return ((await response.json()) as {ai: {copilot: {enabled: string}; hub: {enabled: string}}}).ai;
    };

    test(`the hub, the Copilot and the MCP follow the "${HUB_CONFIG}" configuration`, async ({
        authenticatedPage: page,
        request,
        workflow,
    }) => {
        const hubOn = HUB_CONFIG === 'on';
        const copilotOn = HUB_CONFIG !== 'copilot-off';

        await test.step('Assert the reported flags', async () => {
            const aiInfo = await readApplicationAiInfo(page);

            expect(aiInfo.hub.enabled).toBe(String(HUB_CONFIG !== 'hub-off'));

            expect(aiInfo.copilot.enabled).toBe(String(copilotOn));
        });

        await test.step('Assert the navigation and the routes', async () => {
            await page.goto('/automation');

            await page.waitForURL((url) => url.pathname === (hubOn ? AI_HUB_PATH : '/automation/projects'));

            const aiHubLinks = page
                .getByRole('navigation', {name: 'Main navigation'})
                .locator(`a[href="${AI_HUB_PATH}"]`);

            await expect(aiHubLinks).toHaveCount(hubOn ? 1 : 0);

            const hubEndpointResponse = await page.request.get(
                `/api/platform/internal/ai/chat/ai_hub/${getRandomString()}/attach`
            );

            if (hubOn) {
                expect(await hubEndpointResponse.json()).toEqual({error: 'Unknown thread'});
            } else {
                expect(hubEndpointResponse.ok()).toBe(false);

                expect(await hubEndpointResponse.text()).not.toContain('Unknown thread');
            }
        });

        await test.step('Assert the Copilot', async () => {
            const workflowPage = new WorkflowPage(page);

            await workflowPage.goToWorkflowEditor(workflow.projectId, workflow.workflowId);

            await expect(page.getByRole('button', {exact: true, name: 'Copilot'})).toHaveCount(copilotOn ? 1 : 0);
        });

        if (!MCP_SECRET_KEY) {
            return;
        }

        await test.step('Assert the management MCP', async () => {
            const mcpClient = new McpClient(request, MCP_SECRET_KEY);

            await mcpClient.initialize();

            const toolNames = (await mcpClient.listTools()).map((tool) => tool.name);

            expect(toolNames).toContain('listProjects');

            if (copilotOn) {
                expect(toolNames).toContain('buildWorkflow');
            } else {
                expect(toolNames).not.toContain('buildWorkflow');
            }
        });
    });
});
