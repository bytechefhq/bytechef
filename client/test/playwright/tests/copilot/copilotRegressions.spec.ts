import {type Page, expect, mergeTests} from '@playwright/test';

import {loginTest, projectTest, workflowTest} from '../../fixtures';
import {CopilotPanel} from '../../pages/copilotPanel';
import {WorkflowPage} from '../../pages/workflowPage';
import {
    type AgUiRequestI,
    COPILOT_WORKFLOW_EDITOR_CHAT_URL,
    completedTurn,
    failedTurn,
    stubCopilotRun,
    textMessage,
    toolCall,
} from '../../utils/agUiStreamStub';
import {
    AI_PROVIDERS_SETTINGS_PATH,
    ALTERNATE_MODEL,
    DEFAULT_MODEL,
    DISABLED_PROVIDER_CATALOG,
    OPENAI_PROVIDER_KEY,
    lastUsedModelStorageKey,
    navigateInApp,
    openWorkflowEditorCopilot,
    prepareCopilot,
    readCurrentWorkspaceId,
    stubGraphQlOperation,
} from '../../utils/copilotUtils';
import {type TestWorkflowI, createWorkflow} from '../../utils/projectUtils';

export const test = mergeTests(loginTest(), projectTest, workflowTest);

const AI_SKILLS_PATH = '/automation/settings/ai/skills';

const TEST_CONFIGURATION_URL = /\/api\/platform\/internal\/workflow-test-configurations\/[^/?]+(\?.*)?$/;

const toMessageSummaries = (agUiRequest: AgUiRequestI) =>
    agUiRequest.messages.map((message) => ({content: message.content, role: message.role}));

test.describe('Copilot regressions', () => {
    test.describe('C-H6 connection issue after a Copilot binding', () => {
        test('the missing connection issue clears after saveWorkflowTestConnection without a reload', async ({
            authenticatedPage: page,
            workflow,
        }) => {
            let boundTestConfigurationServed = false;
            let validateRequestsAfterBinding = 0;

            await prepareCopilot(page);

            const copilotRun = await stubCopilotRun(page, COPILOT_WORKFLOW_EDITOR_CHAT_URL, [
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

                if (copilotRun.requests.length === 0) {
                    await route.fulfill({body: '', contentType: 'application/json', status: 200});

                    return;
                }

                const workflowId = new URL(route.request().url()).pathname.split('/').at(-1);

                boundTestConfigurationServed = true;

                await route.fulfill({
                    json: {
                        connections: [{connectionId: 7, workflowConnectionKey: 'slack', workflowNodeName: 'trigger_1'}],
                        environmentId: 0,
                        inputs: {},
                        workflowId,
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

            const copilotPanel = await openWorkflowEditorCopilot(page, workflow);

            const issuesBanner = page.getByText(/in this workflow$/);

            let pageLoadCount = 0;

            page.on('load', () => {
                pageLoadCount++;
            });

            await test.step('Assert the editor reports the missing connection', async () => {
                await expect(issuesBanner).toHaveText('1 error in this workflow');
            });

            await test.step('Bind the connection through the Copilot', async () => {
                await copilotPanel.sendAndWaitForTurnEnd('Use my Slack connection');

                await expect(copilotPanel.root.getByText('The Slack connection is bound.')).toBeVisible();
            });

            await test.step('Assert the editor revalidates and drops the issue', async () => {
                await expect.poll(() => validateRequestsAfterBinding).toBeGreaterThan(0);

                await expect(issuesBanner).toBeHidden();

                await page.getByRole('button', {exact: true, name: 'Workflow Issues'}).click();

                const workflowIssuesPanel = page.getByLabel('Workflow issues');

                await expect(workflowIssuesPanel).toBeVisible();

                await expect(workflowIssuesPanel.getByText('Missing required connection')).toHaveCount(0);

                expect(pageLoadCount).toBe(0);
            });
        });
    });

    test.describe('F6 Copilot mode follows the conversation', () => {
        test.beforeEach(async ({authenticatedPage: page}) => {
            await prepareCopilot(page);
        });

        test('Build mode and the transcript survive closing and reopening the same workflow conversation', async ({
            authenticatedPage: page,
            workflow,
        }) => {
            await stubCopilotRun(page, COPILOT_WORKFLOW_EDITOR_CHAT_URL, [completedTurn(textMessage('Built.'))]);

            const copilotPanel = await openWorkflowEditorCopilot(page, workflow);

            await test.step('Switch to Build and send a message', async () => {
                await copilotPanel.setBuildMode(true);

                await copilotPanel.sendAndWaitForTurnEnd('Build a Slack workflow');
            });

            await test.step('Close and reopen the panel', async () => {
                await copilotPanel.close();

                await copilotPanel.openFromWorkflowEditor();
            });

            await test.step('Assert Build and the transcript are kept', async () => {
                await expect(copilotPanel.buildModeSwitch).toBeChecked();

                await expect(copilotPanel.userMessage('Build a Slack workflow')).toBeVisible();
            });
        });

        test('switching to another workflow with the panel closed starts an empty Ask conversation', async ({
            authenticatedPage: page,
            project,
            workflow,
        }) => {
            const otherWorkflow = await createWorkflow(page, project.id);

            await stubCopilotRun(page, COPILOT_WORKFLOW_EDITOR_CHAT_URL, [completedTurn(textMessage('Built.'))]);

            const copilotPanel = await openWorkflowEditorCopilot(page, workflow);

            await test.step('Leave a Build conversation and close the panel', async () => {
                await copilotPanel.setBuildMode(true);

                await copilotPanel.sendAndWaitForTurnEnd('Build a Slack workflow');

                await copilotPanel.close();
            });

            await test.step('Switch to the other workflow in the app', async () => {
                await navigateInApp(
                    page,
                    `/automation/projects/${otherWorkflow.projectId}/project-workflows/${otherWorkflow.workflowId}`
                );

                await new WorkflowPage(page).waitForCanvasReady();

                await copilotPanel.openFromWorkflowEditor();
            });

            await test.step('Assert the conversation restarted in Ask', async () => {
                await expect(copilotPanel.buildModeSwitch).not.toBeChecked();

                await expect(copilotPanel.userMessages).toHaveCount(0);

                await expect(copilotPanel.emptyTranscriptWelcome).toBeVisible();
            });
        });

        test('opening the Copilot from a non-editor source starts an empty Ask conversation', async ({
            authenticatedPage: page,
            workflow,
        }) => {
            await stubCopilotRun(page, COPILOT_WORKFLOW_EDITOR_CHAT_URL, [completedTurn(textMessage('Built.'))]);

            const copilotPanel = await openWorkflowEditorCopilot(page, workflow);

            await test.step('Leave a Build conversation and close the panel', async () => {
                await copilotPanel.setBuildMode(true);

                await copilotPanel.sendAndWaitForTurnEnd('Build a Slack workflow');

                await copilotPanel.close();
            });

            await test.step('Open the Copilot from the AI Skills page', async () => {
                await navigateInApp(page, AI_SKILLS_PATH);

                await page.getByRole('button', {name: 'Ask Copilot'}).click();

                await expect(copilotPanel.composerInput).toBeVisible();
            });

            await test.step('Assert the conversation is an empty Ask one', async () => {
                await expect(copilotPanel.buildModeSwitch).not.toBeChecked();

                await expect(copilotPanel.userMessages).toHaveCount(0);

                await expect(copilotPanel.emptyTranscriptWelcome).toBeVisible();
            });
        });
    });

    test.describe('Edit and reload', () => {
        test.beforeEach(async ({authenticatedPage: page}) => {
            await prepareCopilot(page);
        });

        test('editing the second user message replaces it and drops the later messages', async ({
            authenticatedPage: page,
            workflow,
        }) => {
            const copilotRun = await stubCopilotRun(page, COPILOT_WORKFLOW_EDITOR_CHAT_URL, [
                completedTurn(textMessage('Reply to the first message.')),
                completedTurn(textMessage('Reply to the second message.')),
                completedTurn(textMessage('Reply to the third message.')),
                completedTurn(textMessage('Reply to the edited message.')),
            ]);

            const copilotPanel = await openWorkflowEditorCopilot(page, workflow);

            await test.step('Hold a three turn conversation', async () => {
                await copilotPanel.sendAndWaitForTurnEnd('First message');
                await copilotPanel.sendAndWaitForTurnEnd('Second message');
                await copilotPanel.sendAndWaitForTurnEnd('Third message');
            });

            await test.step('Edit the second user message', async () => {
                await copilotPanel.editUserMessage('Second message', 'Second message, edited');

                await expect(copilotPanel.root.getByText('Reply to the edited message.')).toBeVisible();

                await copilotPanel.waitForTurnEnd();
            });

            await test.step('Assert the edit replaced the message and dropped what followed', async () => {
                expect(copilotRun.requests).toHaveLength(4);

                expect(toMessageSummaries(copilotRun.requests[3])).toEqual([
                    {content: 'First message', role: 'user'},
                    {content: 'Reply to the first message.', role: 'assistant'},
                    {content: 'Second message, edited', role: 'user'},
                ]);

                await expect(copilotPanel.userMessages).toHaveCount(2);

                await expect(copilotPanel.userMessage('Third message')).toHaveCount(0);

                await expect(copilotPanel.root.getByText('Reply to the third message.')).toHaveCount(0);

                await expect(copilotPanel.root.getByText('Reply to the second message.')).toHaveCount(0);
            });
        });

        test('reload regenerates the last reply for its user message', async ({authenticatedPage: page, workflow}) => {
            const copilotRun = await stubCopilotRun(page, COPILOT_WORKFLOW_EDITOR_CHAT_URL, [
                completedTurn(textMessage('Reply to the first message.')),
                completedTurn(textMessage('First reply to the second message.')),
                completedTurn(textMessage('Regenerated reply to the second message.')),
            ]);

            const copilotPanel = await openWorkflowEditorCopilot(page, workflow);

            await test.step('Hold a two turn conversation', async () => {
                await copilotPanel.sendAndWaitForTurnEnd('First message');
                await copilotPanel.sendAndWaitForTurnEnd('Second message');
            });

            await test.step('Reload the last reply', async () => {
                await copilotPanel.reloadLastReply();

                await expect(copilotPanel.root.getByText('Regenerated reply to the second message.')).toBeVisible();

                await copilotPanel.waitForTurnEnd();
            });

            await test.step('Assert the re-run carried the user message without the old reply', async () => {
                expect(copilotRun.requests).toHaveLength(3);

                expect(toMessageSummaries(copilotRun.requests[2])).toEqual([
                    {content: 'First message', role: 'user'},
                    {content: 'Reply to the first message.', role: 'assistant'},
                    {content: 'Second message', role: 'user'},
                ]);

                await expect(copilotPanel.root.getByText('First reply to the second message.')).toHaveCount(0);

                await expect(copilotPanel.userMessages).toHaveCount(2);
            });
        });

        test('a failed re-run keeps the user message', async ({authenticatedPage: page, workflow}) => {
            await stubCopilotRun(page, COPILOT_WORKFLOW_EDITOR_CHAT_URL, [
                completedTurn(textMessage('Reply to the first message.')),
                completedTurn(textMessage('First reply to the second message.')),
                failedTurn('java.lang.IllegalStateException: The model is overloaded'),
            ]);

            const copilotPanel = await openWorkflowEditorCopilot(page, workflow);

            await test.step('Hold a two turn conversation', async () => {
                await copilotPanel.sendAndWaitForTurnEnd('First message');
                await copilotPanel.sendAndWaitForTurnEnd('Second message');
            });

            await test.step('Reload the last reply into a failing run', async () => {
                await copilotPanel.reloadLastReply();

                await expect(copilotPanel.runErrors).toHaveText('The model is overloaded');

                await copilotPanel.waitForTurnEnd();
            });

            await test.step('Assert the user message is kept', async () => {
                await expect(copilotPanel.userMessage('Second message')).toBeVisible();

                await expect(copilotPanel.userMessages).toHaveCount(2);
            });
        });
    });

    test.describe('Model picker display', () => {
        test.beforeEach(async ({authenticatedPage: page}) => {
            await prepareCopilot(page);
        });

        const openCopilotWithRememberedModel = async (
            page: Page,
            workflow: TestWorkflowI,
            rememberedModel: {model: string; provider: string}
        ) => {
            const workflowPage = new WorkflowPage(page);

            await workflowPage.goToWorkflowEditor(workflow.projectId, workflow.workflowId);

            const workspaceId = await readCurrentWorkspaceId(page);

            await page.evaluate(
                ({storageKey, storedModel}) => window.localStorage.setItem(storageKey, JSON.stringify(storedModel)),
                {storageKey: lastUsedModelStorageKey(workspaceId), storedModel: rememberedModel}
            );

            await page.reload();

            await workflowPage.waitForCanvasReady();

            const copilotPanel = new CopilotPanel(page);

            await copilotPanel.openFromWorkflowEditor();

            return {copilotPanel, workspaceId};
        };

        test('a stale remembered model shows the default and sends no model', async ({
            authenticatedPage: page,
            workflow,
        }) => {
            const copilotRun = await stubCopilotRun(page, COPILOT_WORKFLOW_EDITOR_CHAT_URL, [
                completedTurn(textMessage('Hello.')),
            ]);

            const {copilotPanel, workspaceId} = await openCopilotWithRememberedModel(page, workflow, {
                model: 'retired-model',
                provider: OPENAI_PROVIDER_KEY,
            });

            await test.step('Assert the picker shows the default model', async () => {
                await expect(copilotPanel.modelPickerButton).toHaveText(DEFAULT_MODEL.label);
            });

            await test.step('Assert the request carries no model and the stale entry is forgotten', async () => {
                await copilotPanel.sendAndWaitForTurnEnd('Hello');

                expect(copilotRun.requests[0].state).not.toHaveProperty('userSelectedLlmModel');

                expect(copilotRun.requests[0].state).not.toHaveProperty('userSelectedLlmProvider');

                const storedModel = await page.evaluate(
                    (storageKey) => window.localStorage.getItem(storageKey),
                    lastUsedModelStorageKey(workspaceId)
                );

                expect(storedModel).toBeNull();
            });
        });

        test('an available remembered model is shown and sent', async ({authenticatedPage: page, workflow}) => {
            const copilotRun = await stubCopilotRun(page, COPILOT_WORKFLOW_EDITOR_CHAT_URL, [
                completedTurn(textMessage('Hello.')),
            ]);

            const {copilotPanel, workspaceId} = await openCopilotWithRememberedModel(page, workflow, {
                model: ALTERNATE_MODEL.name,
                provider: OPENAI_PROVIDER_KEY,
            });

            await test.step('Assert the picker shows the remembered model', async () => {
                await expect(copilotPanel.modelPickerButton).toHaveText(ALTERNATE_MODEL.label);
            });

            await test.step('Assert the request carries the remembered model', async () => {
                await copilotPanel.sendAndWaitForTurnEnd('Hello');

                expect(copilotRun.requests[0].state).toMatchObject({
                    userSelectedLlmModel: ALTERNATE_MODEL.name,
                    userSelectedLlmProvider: OPENAI_PROVIDER_KEY,
                    workspaceId: String(workspaceId),
                });
            });
        });
    });

    test.describe('E1 coded run errors', () => {
        test.beforeEach(async ({authenticatedPage: page}) => {
            await prepareCopilot(page);
        });

        test('a NO_AI_PROVIDER run error renders the mapped message and the AI Providers link', async ({
            authenticatedPage: page,
            workflow,
        }) => {
            await stubCopilotRun(page, COPILOT_WORKFLOW_EDITOR_CHAT_URL, [
                failedTurn('No AI provider is configured for environment 0', 'NO_AI_PROVIDER'),
            ]);

            const copilotPanel = await openWorkflowEditorCopilot(page, workflow);

            await test.step('Send a message into the failing run', async () => {
                await copilotPanel.sendAndWaitForTurnEnd('Hello');
            });

            await test.step('Assert the mapped message and the link', async () => {
                await expect(copilotPanel.runErrors).toContainText(
                    'No AI provider is set up for this environment. Enable one in Settings > AI > Providers.'
                );

                const aiProvidersLink = copilotPanel.runErrors.getByRole('link', {name: 'Open AI Providers'});

                await expect(aiProvidersLink).toHaveAttribute('href', AI_PROVIDERS_SETTINGS_PATH);

                await aiProvidersLink.click();

                await page.waitForURL((url) => url.pathname === AI_PROVIDERS_SETTINGS_PATH);
            });
        });

        test('an unknown run error code falls back to the humanized message', async ({
            authenticatedPage: page,
            workflow,
        }) => {
            await stubCopilotRun(page, COPILOT_WORKFLOW_EDITOR_CHAT_URL, [
                failedTurn(
                    'org.springframework.ai.retry.NonTransientAiException: {"error":{"message":"Quota exceeded for this key"}}',
                    'SOMETHING_NEW'
                ),
            ]);

            const copilotPanel = await openWorkflowEditorCopilot(page, workflow);

            await test.step('Send a message into the failing run', async () => {
                await copilotPanel.sendAndWaitForTurnEnd('Hello');
            });

            await test.step('Assert the humanized message and no link', async () => {
                await expect(copilotPanel.runErrors).toHaveText('Quota exceeded for this key');

                await expect(copilotPanel.runErrors.getByRole('link')).toHaveCount(0);
            });
        });
    });

    test.describe('No AI provider', () => {
        test('the Copilot panel shows the empty state with the AI Providers link', async ({
            authenticatedPage: page,
            workflow,
        }) => {
            await prepareCopilot(page, {defaultModel: null, providerCatalog: DISABLED_PROVIDER_CATALOG});

            const workflowPage = new WorkflowPage(page);

            await workflowPage.goToWorkflowEditor(workflow.projectId, workflow.workflowId);

            const copilotPanel = new CopilotPanel(page);

            await test.step('Open the Copilot panel', async () => {
                await copilotPanel.openPanelFromWorkflowEditor();
            });

            await test.step('Assert the empty state', async () => {
                await expect(copilotPanel.root.getByText('No AI providers enabled')).toBeVisible();

                await expect(copilotPanel.composerInput).toHaveCount(0);
            });

            await test.step('Follow the AI Providers link', async () => {
                await copilotPanel.root.getByRole('button', {name: 'Open AI Providers'}).click();

                await page.waitForURL((url) => url.pathname === AI_PROVIDERS_SETTINGS_PATH);
            });
        });
    });
});
