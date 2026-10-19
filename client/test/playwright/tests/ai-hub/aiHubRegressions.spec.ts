import {type Page, type Request, type Route, expect, mergeTests} from '@playwright/test';

import {aiHubChatsTest, loginTest, projectTest, workflowTest} from '../../fixtures';
import {AiHubPage, PENDING_APPROVAL_PLACEHOLDER, SEND_A_MESSAGE_PLACEHOLDER} from '../../pages/aiHubPage';
import {WorkflowPage} from '../../pages/workflowPage';
import {
    AI_HUB_CHAT_URL,
    type AgUiRequestI,
    completedTurn,
    createAgUiGate,
    failedTurn,
    lastMessageOf,
    newToolCallIds,
    rejectedRun,
    runError,
    runFinished,
    runStarted,
    stubAgUiRun,
    textMessage,
    toolCall,
    toolCallResult,
    toolCallStarted,
} from '../../utils/agUiStreamStub';
import {
    AI_HUB_PATH,
    EDIT_NOT_SENT_TOAST,
    type HubWorkflowI,
    PROJECT_WORKFLOW_URL,
    TURN_IN_FLIGHT_TOAST,
    createAiHubChatThroughApi,
    deleteAiHubChatThroughApi,
    deleteWorkflowThroughApi,
    openWorkflowTabArguments,
    openWorkflowTabResult,
    prepareAiHub,
    readAppEnvironmentId,
    readHubWorkflow,
    readRecordedToasts,
    recordToasts,
    stubAiHubAttach,
    stubGraphQlOperationError,
    toolApprovalRequestResult,
    waitForHubWorkspaceId,
} from '../../utils/aiHubUtils';
import {
    AI_PROVIDERS_SETTINGS_PATH,
    ALTERNATE_MODEL,
    DEFAULT_MODEL,
    DISABLED_PROVIDER_CATALOG,
    OPENAI_PROVIDER_KEY,
    lastUsedModelStorageKey,
    readGraphQlOperationName,
    stubGraphQlOperation,
} from '../../utils/copilotUtils';
import {type TestWorkflowI} from '../../utils/projectUtils';

const recordRetryBannerAppearances = (page: Page) =>
    page.addInitScript(() => {
        const bannerAppearances: string[] = [];
        const seenBanners = new WeakSet<Element>();

        Object.assign(window, {__aiHubRetryBannerAppearances: bannerAppearances});

        const recordNewBanners = () => {
            document.querySelectorAll('[data-testid="ai-hub-retry-banner"]').forEach((bannerElement) => {
                if (seenBanners.has(bannerElement)) {
                    return;
                }

                seenBanners.add(bannerElement);

                bannerAppearances.push(bannerElement.textContent ?? '');
            });
        };

        new MutationObserver(recordNewBanners).observe(document, {childList: true, subtree: true});
    });

const readRetryBannerAppearances = (page: Page) =>
    page.evaluate(
        () => (window as unknown as {__aiHubRetryBannerAppearances?: string[]}).__aiHubRetryBannerAppearances ?? []
    );

export const test = mergeTests(loginTest(), aiHubChatsTest, projectTest, workflowTest);

const TEST_CONFIGURATION_URL = /\/api\/platform\/internal\/workflow-test-configurations\/[^/?]+(\?.*)?$/;

const DELETE_PROJECT_APPROVAL = {approvalId: 101, arguments: {projectId: 5}, toolName: 'deleteProject'};

const toHubWorkflow = (page: Page, workflow: TestWorkflowI) =>
    readHubWorkflow(page, {projectId: workflow.projectId, projectWorkflowId: Number(workflow.workflowId)});

const openWorkflowTabCall = (hubWorkflow: HubWorkflowI) =>
    toolCall('openWorkflowTab', openWorkflowTabArguments(hubWorkflow), openWorkflowTabResult(hubWorkflow));

const isHubWorkflowFetch = (request: Request, hubWorkflow: HubWorkflowI) =>
    PROJECT_WORKFLOW_URL(hubWorkflow.projectWorkflowId).test(request.url()) &&
    new URL(request.frame().url()).pathname.startsWith(AI_HUB_PATH);

const lastUserMessageOf = (agUiRequest: AgUiRequestI) =>
    agUiRequest.messages.filter((message) => message.role === 'user').at(-1)!;

interface TruncateCallI {
    id: string;
    keepMessage: boolean;
    messageId: string;
    workspaceId: string;
}

async function stubTruncateChatMessages(page: Page, holdResponse?: Promise<void>): Promise<TruncateCallI[]> {
    const truncateCalls: TruncateCallI[] = [];

    await page.route('**/graphql', async (route: Route) => {
        const request = route.request();

        if (request.method() !== 'POST' || readGraphQlOperationName(request) !== 'truncateAiHubChatMessages') {
            await route.fallback();

            return;
        }

        truncateCalls.push((request.postDataJSON() as {variables: TruncateCallI}).variables);

        await holdResponse;

        await route.fulfill({json: {data: {truncateAiHubChatMessages: 2}}});
    });

    return truncateCalls;
}

test.describe('AI Hub regressions', () => {
    test.describe('H6 workflow tab connection issue after a hub binding', () => {
        test('the missing connection banner clears after saveWorkflowTestConnection without a reload', async ({
            authenticatedPage: page,
            workflow,
        }) => {
            let boundTestConfigurationServed = false;
            let validateRequestsAfterBinding = 0;

            await prepareAiHub(page);

            const hubWorkflow = await toHubWorkflow(page, workflow);

            const hubRun = await stubAgUiRun(page, AI_HUB_CHAT_URL, [
                completedTurn(openWorkflowTabCall(hubWorkflow), textMessage('The workflow is open.')),
                completedTurn(
                    toolCall(
                        'saveWorkflowTestConnection',
                        {connectionId: 7, connectionKey: 'slack', workflowNodeName: 'trigger_1'},
                        JSON.stringify("Connection 7 has been bound to node 'trigger_1' for testing.")
                    ),
                    textMessage('The Slack connection is bound.')
                ),
            ]);

            await page.route(TEST_CONFIGURATION_URL, async (route) => {
                if (route.request().method() !== 'GET') {
                    await route.fallback();

                    return;
                }

                if (hubRun.requests.length < 2) {
                    await route.fulfill({body: '', contentType: 'application/json', status: 200});

                    return;
                }

                boundTestConfigurationServed = true;

                await route.fulfill({
                    json: {
                        connections: [{connectionId: 7, workflowConnectionKey: 'slack', workflowNodeName: 'trigger_1'}],
                        environmentId: 0,
                        inputs: {},
                        workflowId: hubWorkflow.workflowId,
                    },
                });
            });

            await stubGraphQlOperation(page, 'ValidateWorkflow', () => {
                if (boundTestConfigurationServed) {
                    validateRequestsAfterBinding++;
                }

                return {
                    validateWorkflow: {
                        errors: [],
                        nodeIssues: boundTestConfigurationServed
                            ? []
                            : [
                                  {
                                      kind: 'MISSING_CONNECTION',
                                      message: 'Missing required connection: Slack',
                                      nodeName: 'trigger_1',
                                      propertyPath: null,
                                      severity: 'ERROR',
                                  },
                              ],
                        warnings: [],
                    },
                };
            });

            const aiHubPage = new AiHubPage(page);

            await aiHubPage.goto();

            const issuesBanner = page.getByText(/in this workflow$/);

            let pageLoadCount = 0;

            page.on('load', () => {
                pageLoadCount++;
            });

            await test.step('Open the workflow tab and assert the missing connection', async () => {
                await aiHubPage.sendAndWaitForTurnEnd('Open my workflow');

                await expect(aiHubPage.workflowTab(hubWorkflow.name)).toBeVisible();

                await expect(issuesBanner).toHaveText('1 error in this workflow');
            });

            await test.step('Bind the connection through the hub', async () => {
                await aiHubPage.sendAndWaitForTurnEnd('Use my Slack connection');

                await expect(page.getByText('The Slack connection is bound.')).toBeVisible();
            });

            await test.step('Assert the tab revalidates and drops the issue', async () => {
                await expect.poll(() => validateRequestsAfterBinding).toBeGreaterThan(0);

                await expect(issuesBanner).toBeHidden();

                expect(pageLoadCount).toBe(0);
            });
        });
    });

    test.describe('H7 composer after an approval', () => {
        test('the placeholder returns to "Send a message..." once the approval is decided', async ({
            authenticatedPage: page,
        }) => {
            await prepareAiHub(page);

            await stubAgUiRun(page, AI_HUB_CHAT_URL, [
                completedTurn(
                    toolCall(
                        'deleteProject',
                        DELETE_PROJECT_APPROVAL.arguments,
                        toolApprovalRequestResult(DELETE_PROJECT_APPROVAL)
                    )
                ),
            ]);

            await stubGraphQlOperation(page, 'resolveAiHubToolApproval', () => ({
                resolveAiHubToolApproval: {
                    approval: {
                        executionError: null,
                        id: String(DELETE_PROJECT_APPROVAL.approvalId),
                        status: 'APPROVED',
                    },
                    continuationStarted: false,
                    runId: null,
                },
            }));

            const aiHubPage = new AiHubPage(page);

            await aiHubPage.goto();

            await test.step('Ask for a gated tool call', async () => {
                await aiHubPage.sendAndWaitForTurnEnd('Delete the Demo project');

                await expect(page.getByText('Tool approval required')).toBeVisible();

                await expect(aiHubPage.composerInput).toHaveAttribute('placeholder', PENDING_APPROVAL_PLACEHOLDER);
            });

            await test.step('Approve and assert the placeholder', async () => {
                await page.getByRole('button', {name: 'Approve'}).click();

                await expect(page.getByText('Approved - the tool ran.')).toBeVisible();

                await expect(aiHubPage.composerInput).toHaveAttribute('placeholder', SEND_A_MESSAGE_PLACEHOLDER);
            });
        });
    });

    test.describe('H9/H12 a second send while a turn is in flight', () => {
        test('the second tab gets the in-flight notice, stays logged in and shows no error toast', async ({
            authenticatedPage: firstTab,
        }) => {
            const turnGate = createAgUiGate();

            await prepareAiHub(firstTab);

            const hubRun = await stubAgUiRun(firstTab.context(), AI_HUB_CHAT_URL, [
                completedTurn(textMessage('Working on the first message.'), turnGate),
                rejectedRun(409, {error: 'TURN_IN_FLIGHT'}),
            ]);

            const firstHubPage = new AiHubPage(firstTab);

            await firstHubPage.goto();

            await test.step('Start a turn that stays in flight', async () => {
                await firstHubPage.send('First message');

                await expect(firstTab.getByText('Working on the first message.')).toBeVisible();

                await expect(firstHubPage.stopGeneratingButton).toBeVisible();

                await firstTab.waitForURL(/\/automation\/ai-hub\/chats\/\d+$/);
            });

            const chatPath = new URL(firstTab.url()).pathname;

            const secondTab = await firstTab.context().newPage();

            await prepareAiHub(secondTab);

            await recordToasts(secondTab);

            const secondHubPage = new AiHubPage(secondTab);

            await test.step('Send from a second tab of the same chat', async () => {
                await secondTab.goto(chatPath);

                await expect(secondHubPage.composerInput).toBeEnabled({timeout: 30000});

                await secondHubPage.send('Second message');

                await hubRun.waitForRequest(1);
            });

            await test.step('Assert the in-flight notice and nothing else', async () => {
                await expect(secondHubPage.toasts.filter({hasText: TURN_IN_FLIGHT_TOAST})).toBeVisible();

                await expect(secondHubPage.composerInput).toHaveValue('Second message');

                await expect(secondHubPage.userMessage('Second message')).toHaveCount(0);

                expect(await readRecordedToasts(secondTab)).toEqual([{text: TURN_IN_FLIGHT_TOAST, type: 'info'}]);

                expect(new URL(secondTab.url()).pathname).toBe(chatPath);
            });

            await test.step('Finish the first turn', async () => {
                turnGate.release();

                await firstHubPage.waitForTurnEnd();
            });

            await secondTab.close();
        });
    });

    test.describe('H11 a deleted workflow in its tab', () => {
        test('the tab says the workflow no longer exists, without a retry loop or Deploy', async ({
            authenticatedPage: page,
            workflow,
        }) => {
            await prepareAiHub(page);

            const hubWorkflow = await toHubWorkflow(page, workflow);

            await stubAgUiRun(page, AI_HUB_CHAT_URL, [
                completedTurn(openWorkflowTabCall(hubWorkflow), textMessage('The workflow is open.')),
                completedTurn(
                    toolCall(
                        'deleteWorkflow',
                        {workflowId: hubWorkflow.workflowId},
                        JSON.stringify(`Workflow ${hubWorkflow.workflowId} deleted.`)
                    ),
                    textMessage('The workflow is deleted.')
                ),
                completedTurn(textMessage('Anything else?')),
            ]);

            const aiHubPage = new AiHubPage(page);

            await aiHubPage.goto();

            await test.step('Open the workflow tab', async () => {
                await aiHubPage.sendAndWaitForTurnEnd('Open my workflow');

                await new WorkflowPage(page).waitForCanvasReady();

                await expect(page.getByRole('button', {exact: true, name: 'Deploy'})).toBeVisible();
            });

            const workflowRequestsAfterDeletion: string[] = [];

            await test.step('Delete the workflow through the hub', async () => {
                await deleteWorkflowThroughApi(page, hubWorkflow.workflowId);

                page.on('request', (request) => {
                    if (isHubWorkflowFetch(request, hubWorkflow)) {
                        workflowRequestsAfterDeletion.push(request.url());
                    }
                });

                await aiHubPage.sendAndWaitForTurnEnd('Delete it');
            });

            await test.step('Assert the tab shows the deleted workflow once', async () => {
                await expect(page.getByText('This workflow no longer exists. It may have been deleted.')).toBeVisible();

                await expect(page.getByRole('button', {exact: true, name: 'Deploy'})).toHaveCount(0);

                await aiHubPage.sendAndWaitForTurnEnd('Thanks');

                expect(workflowRequestsAfterDeletion).toHaveLength(1);
            });
        });
        test('a tab opened for an already deleted workflow says so, fetches once and offers no Deploy', async ({
            authenticatedPage: page,
            workflow,
        }) => {
            await prepareAiHub(page);

            const hubWorkflow = await toHubWorkflow(page, workflow);

            await deleteWorkflowThroughApi(page, hubWorkflow.workflowId);

            await stubAgUiRun(page, AI_HUB_CHAT_URL, [
                completedTurn(openWorkflowTabCall(hubWorkflow), textMessage('The workflow is open.')),
            ]);

            const workflowRequests: string[] = [];

            page.on('request', (request) => {
                if (isHubWorkflowFetch(request, hubWorkflow)) {
                    workflowRequests.push(request.url());
                }
            });

            const aiHubPage = new AiHubPage(page);

            await aiHubPage.goto();

            await test.step('Open the deleted workflow', async () => {
                await aiHubPage.sendAndWaitForTurnEnd('Open my workflow');

                await expect(aiHubPage.workflowTab(hubWorkflow.name)).toBeVisible();
            });

            await test.step('Assert the tab shows the deleted workflow once', async () => {
                await expect(page.getByText('This workflow no longer exists. It may have been deleted.')).toBeVisible();

                await expect(page.getByRole('button', {exact: true, name: 'Deploy'})).toHaveCount(0);

                await aiHubPage.sendAndWaitForTurnEnd('Thanks');

                expect(workflowRequests).toHaveLength(1);
            });
        });
    });

    test.describe('H13 reloaded approvals and pickers', () => {
        test('a resolved approval and an answered picker come back as cards, with no raw continuation row', async ({
            authenticatedPage: page,
        }) => {
            await prepareAiHub(page);

            const aiHubPage = new AiHubPage(page);

            const hubWorkspaceId = waitForHubWorkspaceId(page);

            await aiHubPage.goto();

            const chat = await createAiHubChatThroughApi(page, {
                environment: await readAppEnvironmentId(page),
                workspaceId: await hubWorkspaceId,
            });

            const approvalId = 701;

            await stubGraphQlOperation(page, 'aiHubChatMessages', () => ({
                aiHubChatMessages: [
                    {
                        content: 'Delete the Demo project',
                        id: 'row-delete-request',
                        role: 'user',
                        timestamp: Date.now() - 60000,
                        toolEventsJson: JSON.stringify([
                            {
                                arguments: JSON.stringify({projectId: 5}),
                                id: 'call-delete-project',
                                kind: 'call',
                                name: 'deleteProject',
                            },
                            {
                                id: 'call-delete-project',
                                kind: 'result',
                                name: 'deleteProject',
                                response: toolApprovalRequestResult({
                                    approvalId,
                                    arguments: {projectId: 5},
                                    toolName: 'deleteProject',
                                }),
                            },
                        ]),
                    },
                    {
                        content: `[tool-approval #${approvalId} approved and executed]\nTool: deleteProject\nResult:\n"Project 5 deleted."`,
                        id: 'row-continuation',
                        role: 'user',
                        timestamp: Date.now() - 50000,
                        toolEventsJson: null,
                    },
                    {
                        content: 'The Demo project is deleted.',
                        id: 'row-delete-reply',
                        role: 'assistant',
                        timestamp: Date.now() - 40000,
                        toolEventsJson: null,
                    },
                    {
                        content: 'Use my Slack connection',
                        id: 'row-slack-request',
                        role: 'user',
                        timestamp: Date.now() - 30000,
                        toolEventsJson: JSON.stringify([
                            {
                                arguments: JSON.stringify({componentName: 'slack'}),
                                id: 'call-select-connection',
                                kind: 'call',
                                name: 'selectConnection',
                            },
                            {
                                id: 'call-select-connection',
                                kind: 'result',
                                name: 'selectConnection',
                                response: JSON.stringify({
                                    componentLabel: 'Slack',
                                    componentName: 'slack',
                                    kind: 'select-connection',
                                }),
                            },
                        ]),
                    },
                    {
                        content: 'User picked: Slack Production (ID: 42)',
                        id: 'row-pick',
                        role: 'user',
                        timestamp: Date.now() - 20000,
                        toolEventsJson: null,
                    },
                    {
                        content: 'Slack Production is bound.',
                        id: 'row-pick-reply',
                        role: 'assistant',
                        timestamp: Date.now() - 10000,
                        toolEventsJson: null,
                    },
                ],
            }));

            await stubGraphQlOperation(page, 'aiHubToolApprovals', () => ({
                aiHubToolApprovals: [
                    {
                        arguments: JSON.stringify({projectId: 5}),
                        chatId: String(chat.id),
                        comment: null,
                        componentName: null,
                        decidedAt: Date.now() - 55000,
                        decidedByUserId: null,
                        executionError: null,
                        expiresAt: Date.now() + 86400000,
                        id: String(approvalId),
                        status: 'APPROVED',
                        toolName: 'deleteProject',
                    },
                ],
            }));

            const assertRestoredCards = async () => {
                await expect(page.getByText('Slack Production is bound.')).toBeVisible();

                await expect(page.getByText('Tool approval required')).toBeVisible();

                await expect(page.getByText('Approved - the tool ran.')).toBeVisible();

                await expect(page.getByRole('button', {name: 'Approve'})).toHaveCount(0);

                await expect(page.getByText('Picked: Slack Production', {exact: true})).toBeVisible();

                await expect(page.getByText(/tool-approval #/)).toHaveCount(0);

                await expect(page.getByTestId('tool-approval-status-line')).toHaveCount(0);
            };

            try {
                await test.step('Open the chat', async () => {
                    await aiHubPage.gotoChat(chat.id);

                    await assertRestoredCards();
                });

                await test.step('Reload and assert the same cards', async () => {
                    await page.reload();

                    await assertRestoredCards();
                });
            } finally {
                await deleteAiHubChatThroughApi(page, chat);
            }
        });
    });

    test.describe('H14 model picker', () => {
        const openHubWithRememberedModel = async (page: Page, rememberedModel: {model: string; provider: string}) => {
            const aiHubPage = new AiHubPage(page);

            const hubWorkspaceId = waitForHubWorkspaceId(page);

            await aiHubPage.goto();

            const workspaceId = await hubWorkspaceId;

            await page.evaluate(
                ({storageKey, storedModel}) => window.localStorage.setItem(storageKey, JSON.stringify(storedModel)),
                {storageKey: lastUsedModelStorageKey(workspaceId), storedModel: rememberedModel}
            );

            await page.reload();

            await expect(aiHubPage.composerInput).toBeEnabled({timeout: 30000});

            return {aiHubPage, workspaceId};
        };

        test('a stale remembered model shows the default, sends no model and is forgotten', async ({
            authenticatedPage: page,
        }) => {
            await prepareAiHub(page);

            const hubRun = await stubAgUiRun(page, AI_HUB_CHAT_URL, [completedTurn(textMessage('Hello.'))]);

            const {aiHubPage, workspaceId} = await openHubWithRememberedModel(page, {
                model: 'retired-model',
                provider: OPENAI_PROVIDER_KEY,
            });

            await test.step('Assert the picker shows the default model', async () => {
                await expect(aiHubPage.modelPickerButton).toHaveText(DEFAULT_MODEL.label);
            });

            await test.step('Assert the request carries no model and the stale entry is forgotten', async () => {
                await aiHubPage.sendAndWaitForTurnEnd('Hello');

                const hubRequest = await hubRun.waitForRequest(0);

                expect(hubRequest.state).not.toHaveProperty('userSelectedLlmModel');

                expect(hubRequest.state).not.toHaveProperty('userSelectedLlmProvider');

                const storedModel = await page.evaluate(
                    (storageKey) => window.localStorage.getItem(storageKey),
                    lastUsedModelStorageKey(workspaceId)
                );

                expect(storedModel).toBeNull();

                await expect(aiHubPage.modelPickerButton).toHaveText(DEFAULT_MODEL.label);
            });
        });

        test('an available remembered model is shown and sent', async ({authenticatedPage: page}) => {
            await prepareAiHub(page);

            const hubRun = await stubAgUiRun(page, AI_HUB_CHAT_URL, [completedTurn(textMessage('Hello.'))]);

            const {aiHubPage, workspaceId} = await openHubWithRememberedModel(page, {
                model: ALTERNATE_MODEL.name,
                provider: OPENAI_PROVIDER_KEY,
            });

            await test.step('Assert the picker shows the remembered model', async () => {
                await expect(aiHubPage.modelPickerButton).toHaveText(ALTERNATE_MODEL.label);
            });

            await test.step('Assert the request carries the remembered model', async () => {
                await aiHubPage.sendAndWaitForTurnEnd('Hello');

                expect((await hubRun.waitForRequest(0)).state).toMatchObject({
                    userSelectedLlmModel: ALTERNATE_MODEL.name,
                    userSelectedLlmProvider: OPENAI_PROVIDER_KEY,
                    workspaceId: String(workspaceId),
                });

                await expect(aiHubPage.modelPickerButton).toHaveText(ALTERNATE_MODEL.label);
            });
        });
    });

    test.describe('P1 running buildWorkflow row', () => {
        test('"Building workflow..." shows while buildWorkflow runs and goes away with its result', async ({
            authenticatedPage: page,
        }) => {
            const buildGate = createAgUiGate();
            const buildWorkflowIds = newToolCallIds();

            await prepareAiHub(page);

            await stubAgUiRun(page, AI_HUB_CHAT_URL, [
                [
                    runStarted(),
                    textMessage('Building it now.'),
                    toolCallStarted('buildWorkflow', {instructions: 'Post hello to Slack'}, buildWorkflowIds),
                    buildGate,
                    toolCallResult(JSON.stringify('Workflow built.'), buildWorkflowIds),
                    textMessage('The workflow is ready.'),
                    runFinished(),
                ],
            ]);

            const aiHubPage = new AiHubPage(page);

            await aiHubPage.goto();

            const buildingRow = page.getByRole('status').filter({hasText: 'Building workflow...'});

            await test.step('Assert the row while the tool runs', async () => {
                await aiHubPage.send('Build a Slack workflow');

                await expect(page.getByText('Building it now.')).toBeVisible();

                await expect(buildingRow).toBeVisible();
            });

            await test.step('Assert the row goes away with the result', async () => {
                buildGate.release();

                await expect(page.getByText('The workflow is ready.')).toBeVisible();

                await expect(buildingRow).toHaveCount(0);

                await aiHubPage.waitForTurnEnd();
            });
        });
    });

    test.describe('E1/H16 no AI provider', () => {
        test('a NO_AI_PROVIDER run error shows the mapped message and the AI Providers link', async ({
            authenticatedPage: page,
        }) => {
            await prepareAiHub(page);

            await stubAgUiRun(page, AI_HUB_CHAT_URL, [
                failedTurn('No AI provider is configured for environment 0', 'NO_AI_PROVIDER'),
            ]);

            const aiHubPage = new AiHubPage(page);

            await aiHubPage.goto();

            await test.step('Send a message into the failing run', async () => {
                await aiHubPage.sendAndWaitForTurnEnd('Hello');
            });

            await test.step('Assert the mapped message and the link', async () => {
                await expect(aiHubPage.runErrors).toContainText(
                    'No AI provider is set up for this environment. Enable one in Settings > AI > Providers.'
                );

                const aiProvidersLink = aiHubPage.runErrors.getByRole('link', {name: 'Open AI Providers'});

                await expect(aiProvidersLink).toHaveAttribute('href', AI_PROVIDERS_SETTINGS_PATH);

                await aiProvidersLink.click();

                await page.waitForURL((url) => url.pathname === AI_PROVIDERS_SETTINGS_PATH);
            });
        });

        test('the no-provider empty state offers "Open AI Providers"', async ({authenticatedPage: page}) => {
            await prepareAiHub(page, {defaultModel: null, providerCatalog: DISABLED_PROVIDER_CATALOG});

            await page.goto('/automation/ai-hub');

            await test.step('Assert the empty state', async () => {
                await expect(page.getByText('No AI providers enabled')).toBeVisible({timeout: 30000});

                await expect(page.getByLabel('Message input')).toHaveCount(0);
            });

            await test.step('Follow the AI Providers action', async () => {
                await page.getByRole('button', {name: 'Open AI Providers'}).click();

                await page.waitForURL((url) => url.pathname === AI_PROVIDERS_SETTINGS_PATH);
            });
        });
    });

    test.describe('Edit and reload', () => {
        test('editing the second user message truncates at its id and runs only the new text', async ({
            authenticatedPage: page,
        }) => {
            await prepareAiHub(page);

            const truncateCalls = await stubTruncateChatMessages(page);

            const hubRun = await stubAgUiRun(page, AI_HUB_CHAT_URL, [
                completedTurn(textMessage('Reply to the first message.')),
                completedTurn(textMessage('Reply to the second message.')),
                completedTurn(textMessage('Reply to the third message.')),
                completedTurn(textMessage('Reply to the edited message.')),
            ]);

            const aiHubPage = new AiHubPage(page);

            await aiHubPage.goto();

            await test.step('Hold a three turn conversation', async () => {
                await aiHubPage.sendAndWaitForTurnEnd('First message');
                await aiHubPage.sendAndWaitForTurnEnd('Second message');
                await aiHubPage.sendAndWaitForTurnEnd('Third message');
            });

            const secondUserMessage = lastUserMessageOf(await hubRun.waitForRequest(1));

            await test.step('Edit the second user message', async () => {
                await aiHubPage.editUserMessage('Second message', 'Second message, edited');

                await expect(page.getByText('Reply to the edited message.')).toBeVisible();

                await aiHubPage.waitForTurnEnd();
            });

            await test.step('Assert the truncation and the new run', async () => {
                const chatId = new URL(page.url()).pathname.split('/').at(-1);

                expect(truncateCalls).toEqual([
                    expect.objectContaining({id: chatId, keepMessage: false, messageId: secondUserMessage.id}),
                ]);

                const editRequest = await hubRun.waitForRequest(3);

                expect(editRequest.messages).toHaveLength(1);

                expect(editRequest.messages[0]).toMatchObject({content: 'Second message, edited', role: 'user'});

                expect(editRequest.messages[0].id).not.toBe(secondUserMessage.id);
            });

            await test.step('Assert the transcript dropped what followed', async () => {
                await expect(aiHubPage.userMessages).toHaveCount(2);

                await expect(aiHubPage.userMessage('Third message')).toHaveCount(0);

                await expect(page.getByText('Reply to the third message.')).toHaveCount(0);

                await expect(page.getByText('Reply to the second message.')).toHaveCount(0);
            });
        });

        test('reloading the last reply truncates after its user message and re-sends it with the same id', async ({
            authenticatedPage: page,
        }) => {
            await prepareAiHub(page);

            const truncateCalls = await stubTruncateChatMessages(page);

            const hubRun = await stubAgUiRun(page, AI_HUB_CHAT_URL, [
                completedTurn(textMessage('Reply to the first message.')),
                completedTurn(textMessage('First reply to the second message.')),
                completedTurn(textMessage('Regenerated reply to the second message.')),
            ]);

            const aiHubPage = new AiHubPage(page);

            await aiHubPage.goto();

            await test.step('Hold a two turn conversation', async () => {
                await aiHubPage.sendAndWaitForTurnEnd('First message');
                await aiHubPage.sendAndWaitForTurnEnd('Second message');
            });

            const secondUserMessage = lastUserMessageOf(await hubRun.waitForRequest(1));

            await test.step('Reload the last reply', async () => {
                await aiHubPage.reloadLastReply();

                await expect(page.getByText('Regenerated reply to the second message.')).toBeVisible();

                await aiHubPage.waitForTurnEnd();
            });

            await test.step('Assert the truncation and the re-sent message', async () => {
                expect(truncateCalls).toEqual([
                    expect.objectContaining({keepMessage: true, messageId: secondUserMessage.id}),
                ]);

                expect((await hubRun.waitForRequest(2)).messages).toEqual([
                    {content: 'Second message', id: secondUserMessage.id, role: 'user'},
                ]);

                await expect(page.getByText('First reply to the second message.')).toHaveCount(0);

                await expect(aiHubPage.userMessages).toHaveCount(2);
            });
        });

        test('an edit whose chat changed while it was saved is not sent and the user is told', async ({
            authenticatedPage: page,
        }) => {
            let releaseTruncate: () => void = () => undefined;

            const truncateHeld = new Promise<void>((resolve) => {
                releaseTruncate = resolve;
            });

            await prepareAiHub(page);

            const truncateCalls = await stubTruncateChatMessages(page, truncateHeld);

            const hubRun = await stubAgUiRun(page, AI_HUB_CHAT_URL, [
                completedTurn(textMessage('Reply to the first message.')),
                completedTurn(textMessage('Reply to the second message.')),
                completedTurn(textMessage('Reply to the edited message.')),
            ]);

            const aiHubPage = new AiHubPage(page);

            await aiHubPage.goto();

            await test.step('Hold a two turn conversation', async () => {
                await aiHubPage.sendAndWaitForTurnEnd('First message');
                await aiHubPage.sendAndWaitForTurnEnd('Second message');
            });

            await test.step('Start an edit and switch to a new chat while it is saved', async () => {
                await aiHubPage.editUserMessage('First message', 'First message, edited');

                await expect.poll(() => truncateCalls.length).toBe(1);

                await aiHubPage.newChatLink.click();

                await page.waitForURL((url) => url.pathname === '/automation/ai-hub');

                await expect(page.getByRole('heading', {name: 'What should we get done?'})).toBeVisible();

                await expect(aiHubPage.userMessages).toHaveCount(0);

                releaseTruncate();
            });

            await test.step('Assert the edit was not sent', async () => {
                await expect(aiHubPage.toasts.filter({hasText: EDIT_NOT_SENT_TOAST})).toBeVisible();

                expect(hubRun.requests).toHaveLength(2);
            });
        });
    });

    test.describe('Retry banner for tool failures', () => {
        test.beforeEach(async ({authenticatedPage: page}) => {
            await prepareAiHub(page);
        });

        test('an unrecovered tool failure shows the retry banner only when the run ends, and Retry resends', async ({
            authenticatedPage: page,
        }) => {
            const runEndGate = createAgUiGate();

            const hubRun = await stubAgUiRun(page, AI_HUB_CHAT_URL, [
                [
                    runStarted(),
                    toolCall(
                        'buildWorkflow',
                        {instructions: 'Post hello to Slack'},
                        JSON.stringify({error: 'Component slack/v9 does not exist'})
                    ),
                    textMessage('That did not work.'),
                    runEndGate,
                    runFinished(),
                ],
                completedTurn(textMessage('Built it this time.')),
            ]);

            const aiHubPage = new AiHubPage(page);

            await aiHubPage.goto();

            await test.step('Assert no banner while the run continues', async () => {
                await aiHubPage.send('Build a Slack workflow');

                await expect(page.getByText('That did not work.')).toBeVisible();

                await expect(aiHubPage.retryBanner).toHaveCount(0);
            });

            await test.step('Assert the banner shows the failure at the run end', async () => {
                runEndGate.release();

                await aiHubPage.waitForTurnEnd();

                await expect(aiHubPage.retryBanner).toHaveText(
                    /buildWorkflow failed: Component slack\/v9 does not exist/
                );
            });

            await test.step('Retry sends the last message again and clears the banner', async () => {
                await aiHubPage.retryBanner.getByRole('button', {name: 'Retry'}).click();

                const retriedRequest = await hubRun.waitForRequest(1);

                expect(lastMessageOf(retriedRequest)).toMatchObject({content: 'Build a Slack workflow', role: 'user'});

                await expect(page.getByText('Built it this time.')).toBeVisible();

                await expect(aiHubPage.retryBanner).toHaveCount(0);
            });
        });

        test('a tool failure the run recovers from shows no banner', async ({authenticatedPage: page}) => {
            await stubAgUiRun(page, AI_HUB_CHAT_URL, [
                completedTurn(
                    toolCall(
                        'buildWorkflow',
                        {instructions: 'Post hello to Slack'},
                        JSON.stringify({error: 'Component slack/v9 does not exist'})
                    ),
                    toolCall(
                        'buildWorkflow',
                        {instructions: 'Post hello to Slack v1'},
                        JSON.stringify('Workflow built.')
                    ),
                    textMessage('The workflow is ready.')
                ),
            ]);

            const aiHubPage = new AiHubPage(page);

            await recordRetryBannerAppearances(page);

            await aiHubPage.goto();

            await aiHubPage.sendAndWaitForTurnEnd('Build a Slack workflow');

            await expect(page.getByText('The workflow is ready.')).toBeVisible();

            await expect(aiHubPage.retryBanner).toHaveCount(0);

            expect(await readRetryBannerAppearances(page)).toEqual([]);
        });

        test('a tool failure followed by a run error shows the run error and no banner', async ({
            authenticatedPage: page,
        }) => {
            await stubAgUiRun(page, AI_HUB_CHAT_URL, [
                [
                    runStarted(),
                    toolCall(
                        'buildWorkflow',
                        {instructions: 'Post hello to Slack'},
                        JSON.stringify({error: 'Component slack/v9 does not exist'})
                    ),
                    runError('The model provider is unavailable'),
                ],
            ]);

            const aiHubPage = new AiHubPage(page);

            await recordRetryBannerAppearances(page);

            await aiHubPage.goto();

            await aiHubPage.sendAndWaitForTurnEnd('Build a Slack workflow');

            await expect(aiHubPage.runErrors).toContainText('The model provider is unavailable');

            await expect(aiHubPage.retryBanner).toHaveCount(0);

            expect(await readRetryBannerAppearances(page)).toEqual([]);
        });

        test('a tool failure followed by Stop shows no banner', async ({authenticatedPage: page}) => {
            const runEndGate = createAgUiGate();

            await stubAgUiRun(page, AI_HUB_CHAT_URL, [
                [
                    runStarted(),
                    toolCall(
                        'buildWorkflow',
                        {instructions: 'Post hello to Slack'},
                        JSON.stringify({error: 'Component slack/v9 does not exist'})
                    ),
                    textMessage('That did not work.'),
                    runEndGate,
                    runFinished(),
                ],
            ]);

            const cancelRunCalls: Array<Record<string, unknown>> = [];

            await stubGraphQlOperation(page, 'cancelAiHubRun', (variables) => {
                cancelRunCalls.push(variables);

                return {cancelAiHubRun: true};
            });

            const aiHubPage = new AiHubPage(page);

            await recordRetryBannerAppearances(page);

            await aiHubPage.goto();

            let hubRunSettled: Promise<unknown> = Promise.resolve();

            await test.step('Stop the run after the failure', async () => {
                const isHubRunRequest = (request: Request) => AI_HUB_CHAT_URL.test(request.url());

                hubRunSettled = Promise.race([
                    page.waitForEvent('requestfailed', isHubRunRequest),
                    page.waitForEvent('requestfinished', isHubRunRequest),
                ]).catch(() => undefined);

                await aiHubPage.send('Build a Slack workflow');

                await expect(page.getByText('That did not work.')).toBeVisible();

                await aiHubPage.stopGeneratingButton.click();

                await aiHubPage.waitForTurnEnd();

                await expect.poll(() => cancelRunCalls.length).toBe(1);

                runEndGate.release();
            });

            await test.step('Assert no banner was shown once the run settled', async () => {
                await hubRunSettled;

                await page.evaluate(
                    () => new Promise((resolve) => requestAnimationFrame(() => requestAnimationFrame(resolve)))
                );

                await expect(page.getByText('That did not work.')).toBeVisible();

                await expect(aiHubPage.retryBanner).toHaveCount(0);

                expect(await readRetryBannerAppearances(page)).toEqual([]);
            });
        });
    });

    test.describe('Approvals for a dropped call', () => {
        test('approving a card whose call was dropped marks it superseded with the reason and runs nothing', async ({
            authenticatedPage: page,
        }) => {
            const conflictMessage = `Approval ${DELETE_PROJECT_APPROVAL.approvalId} no longer matches a tool call in this chat; it was edited or reloaded`;

            await prepareAiHub(page);

            await stubAgUiRun(page, AI_HUB_CHAT_URL, [
                completedTurn(
                    toolCall(
                        'deleteProject',
                        DELETE_PROJECT_APPROVAL.arguments,
                        toolApprovalRequestResult(DELETE_PROJECT_APPROVAL)
                    )
                ),
            ]);

            const {calls: resolveCalls} = await stubGraphQlOperationError(page, 'resolveAiHubToolApproval', {
                errorCode: 'CONFLICT',
                message: conflictMessage,
            });

            const {attachRequests} = await stubAiHubAttach(page, []);

            await recordToasts(page);

            const aiHubPage = new AiHubPage(page);

            await aiHubPage.goto();

            await test.step('Ask for a gated tool call', async () => {
                await aiHubPage.sendAndWaitForTurnEnd('Delete the Demo project');

                await expect(page.getByText('Tool approval required')).toBeVisible();
            });

            await test.step('Approve and assert the card is superseded with the reason', async () => {
                await page.getByRole('button', {name: 'Approve'}).click();

                await expect(
                    aiHubPage.toolApprovalCards
                        .getByRole('status')
                        .filter({hasText: 'This approval no longer applies: its message was edited or reloaded.'})
                ).toBeVisible();

                expect(resolveCalls).toHaveLength(1);

                await expect(page.getByText('Approved - the tool ran.')).toHaveCount(0);

                await expect(page.getByRole('button', {name: 'Approve'})).toHaveCount(0);

                await expect(aiHubPage.composerInput).toHaveAttribute('placeholder', SEND_A_MESSAGE_PLACEHOLDER);

                expect(attachRequests).toEqual([]);

                expect(await readRecordedToasts(page)).toEqual([]);
            });
        });
    });

    test.describe('F5 workflow tab fetches', () => {
        test('opening a workflow tab fetches the workflow once', async ({authenticatedPage: page, workflow}) => {
            await prepareAiHub(page);

            const hubWorkflow = await toHubWorkflow(page, workflow);

            await stubAgUiRun(page, AI_HUB_CHAT_URL, [
                completedTurn(openWorkflowTabCall(hubWorkflow), textMessage('The workflow is open.')),
            ]);

            const workflowRequests: string[] = [];

            page.on('request', (request) => {
                if (isHubWorkflowFetch(request, hubWorkflow)) {
                    workflowRequests.push(request.url());
                }
            });

            const aiHubPage = new AiHubPage(page);

            await aiHubPage.goto();

            await test.step('Open the workflow tab and assert one fetch once the editor shows it', async () => {
                await aiHubPage.sendAndWaitForTurnEnd('Open my workflow');

                await new WorkflowPage(page).waitForCanvasReady();

                expect(workflowRequests).toHaveLength(1);
            });
        });
    });
});
