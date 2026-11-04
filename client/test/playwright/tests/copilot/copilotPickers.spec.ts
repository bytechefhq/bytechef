import {type Locator, expect, mergeTests} from '@playwright/test';

import {loginTest, projectTest, workflowTest} from '../../fixtures';
import {
    COPILOT_WORKFLOW_EDITOR_CHAT_URL,
    completedTurn,
    lastMessageOf,
    stubCopilotRun,
    textMessage,
    toolCall,
} from '../../utils/agUiStreamStub';
import {openWorkflowEditorCopilot, prepareCopilot, stubWorkspaceConnections} from '../../utils/copilotUtils';

export const test = mergeTests(loginTest(), projectTest, workflowTest);

const SLACK_CONNECTIONS = [
    {id: 101, name: 'Slack Marketing'},
    {id: 102, name: 'Slack Support'},
    {id: -1, name: 'Slack Unsaved'},
];

const SELECT_SLACK_CONNECTION_RESULT = JSON.stringify({
    awaitingAnswer: true,
    componentLabel: 'Slack',
    componentName: 'slack',
    kind: 'select-connection',
});

const readAncestorOpacity = (pickerLocator: Locator) =>
    pickerLocator.evaluate((pickerElement) => {
        let effectiveOpacity = 1;

        for (
            let ancestorElement = pickerElement.parentElement;
            ancestorElement !== null;
            ancestorElement = ancestorElement.parentElement
        ) {
            effectiveOpacity *= Number(window.getComputedStyle(ancestorElement).opacity);
        }

        return effectiveOpacity;
    });

const selectSlackConnectionTurn = () =>
    completedTurn(
        textMessage('This workflow needs a Slack connection.'),
        toolCall('selectConnection', {componentName: 'slack'}, SELECT_SLACK_CONNECTION_RESULT)
    );

test.describe('Copilot interactive pickers', () => {
    test.beforeEach(async ({authenticatedPage: page}) => {
        await prepareCopilot(page);

        await stubWorkspaceConnections(page, SLACK_CONNECTIONS);
    });

    test('select-connection card lists the workspace connections and sends the pick', async ({
        authenticatedPage: page,
        workflow,
    }) => {
        const copilotRun = await stubCopilotRun(page, COPILOT_WORKFLOW_EDITOR_CHAT_URL, [
            selectSlackConnectionTurn(),
            completedTurn(textMessage('Slack Support is now bound.')),
        ]);

        const copilotPanel = await openWorkflowEditorCopilot(page, workflow);

        const connectionPicker = copilotPanel.root.getByRole('combobox');

        await test.step('Ask for a Slack workflow and wait for the turn to end', async () => {
            await copilotPanel.sendAndWaitForTurnEnd('Post new leads to Slack');

            await expect(connectionPicker).toBeVisible();

            await expect(copilotPanel.composerInput).toBeEnabled();
        });

        await test.step('Pick a connection', async () => {
            await connectionPicker.click();

            await expect(page.getByRole('option', {name: 'Slack Marketing'})).toBeVisible();

            await page.getByRole('option', {name: 'Slack Support'}).click();

            await expect(copilotPanel.root.getByText('Picked: Slack Support', {exact: true})).toBeVisible();
        });

        await test.step('Assert the pick is the next request last message', async () => {
            await expect.poll(() => copilotRun.requests.length).toBe(2);

            expect(lastMessageOf(await copilotRun.waitForRequest(1))).toMatchObject({
                content: 'User picked: Slack Support (ID: 102)',
                role: 'user',
            });

            await expect(copilotPanel.root.getByText('Slack Support is now bound.')).toBeVisible();
        });
    });

    test('create-connection card opens the connection dialog and its submit posts', async ({
        authenticatedPage: page,
        workflow,
    }) => {
        const createConnectionResult = JSON.stringify({
            awaitingAnswer: true,
            componentLabel: 'Gotify',
            componentName: 'gotify',
            kind: 'create-connection',
            suggestedName: 'Gotify alerts',
        });

        await stubCopilotRun(page, COPILOT_WORKFLOW_EDITOR_CHAT_URL, [
            completedTurn(
                toolCall(
                    'createConnection',
                    {componentName: 'gotify', suggestedName: 'Gotify alerts'},
                    createConnectionResult
                )
            ),
        ]);

        const createdConnectionRequests: Array<Record<string, unknown>> = [];

        await page.route(/\/api\/automation\/internal\/connections(\?.*)?$/, async (route) => {
            if (route.request().method() !== 'POST') {
                await route.fallback();

                return;
            }

            createdConnectionRequests.push(route.request().postDataJSON());

            await route.fulfill({json: 9001});
        });

        const copilotPanel = await openWorkflowEditorCopilot(page, workflow);

        await test.step('Render the create-connection card', async () => {
            await copilotPanel.sendAndWaitForTurnEnd('Send alerts to Gotify');

            await copilotPanel.root.getByRole('button', {name: 'Connect Gotify'}).click();
        });

        const connectionDialog = page.getByRole('dialog');

        await test.step('Fill and submit the connection dialog', async () => {
            await connectionDialog.getByPlaceholder('My Connection').fill('Gotify alerts');

            const connectionPropertyInputs = connectionDialog.locator('[aria-label$=" property"]').getByRole('textbox');

            await expect(connectionPropertyInputs.first()).toBeVisible();

            const connectionPropertyInputCount = await connectionPropertyInputs.count();

            for (let inputIndex = 0; inputIndex < connectionPropertyInputCount; inputIndex++) {
                await connectionPropertyInputs.nth(inputIndex).fill(`http://gotify.example.com/${inputIndex}`);
            }

            await connectionDialog.getByRole('button', {name: 'Save'}).click();
        });

        await test.step('Assert the connection was posted and the card shows it', async () => {
            await expect.poll(() => createdConnectionRequests.length).toBe(1);

            expect(createdConnectionRequests[0]).toMatchObject({componentName: 'gotify', name: 'Gotify alerts'});

            await expect(copilotPanel.root.getByText('Connection ready:')).toBeVisible();
        });
    });

    test('select-property-option card lists the options and sends the picked value', async ({
        authenticatedPage: page,
        workflow,
    }) => {
        const selectPropertyOptionResult = JSON.stringify({
            awaitingAnswer: true,
            componentName: 'slack',
            kind: 'select-property-option',
            options: [
                {label: '#general', value: 'C0GENERAL'},
                {label: '#random', value: 'C0RANDOM'},
            ],
            propertyName: 'channel',
            truncated: false,
        });

        const copilotRun = await stubCopilotRun(page, COPILOT_WORKFLOW_EDITOR_CHAT_URL, [
            completedTurn(
                toolCall(
                    'selectPropertyOption',
                    {actionName: 'sendChannelMessage', componentName: 'slack', propertyName: 'channel'},
                    selectPropertyOptionResult
                )
            ),
            completedTurn(textMessage('The channel is set.')),
        ]);

        const copilotPanel = await openWorkflowEditorCopilot(page, workflow);

        await test.step('Render the property option picker', async () => {
            await copilotPanel.sendAndWaitForTurnEnd('Pick the Slack channel');

            await copilotPanel.root.getByRole('combobox').click();

            await expect(page.getByRole('option', {name: '#general'})).toBeVisible();

            await page.getByRole('option', {name: '#random'}).click();
        });

        await test.step('Assert the picked value is sent', async () => {
            await expect.poll(() => copilotRun.requests.length).toBe(2);

            expect(lastMessageOf(await copilotRun.waitForRequest(1))).toMatchObject({
                content: 'User picked: #random (value: C0RANDOM)',
                role: 'user',
            });
        });
    });

    test('ask-user-question card sends the answer as a User picked message', async ({
        authenticatedPage: page,
        workflow,
    }) => {
        const askUserQuestionResult = JSON.stringify({
            awaitingAnswer: true,
            kind: 'ask-user-question',
            questions: [
                {
                    header: 'Channel',
                    multiSelect: false,
                    options: [
                        {description: 'The engineering team channel', label: 'Engineering'},
                        {description: null, label: 'Support'},
                    ],
                    question: 'Which team should receive the alerts?',
                },
            ],
        });

        const copilotRun = await stubCopilotRun(page, COPILOT_WORKFLOW_EDITOR_CHAT_URL, [
            completedTurn(toolCall('askUserQuestion', {questions: []}, askUserQuestionResult)),
            completedTurn(textMessage('Alerts go to Engineering.')),
        ]);

        const copilotPanel = await openWorkflowEditorCopilot(page, workflow);

        await test.step('Answer the question', async () => {
            await copilotPanel.sendAndWaitForTurnEnd('Route the alerts');

            await expect(copilotPanel.root.getByText('Which team should receive the alerts?')).toBeVisible();

            await copilotPanel.root.getByRole('button', {name: 'Engineering'}).click();
        });

        await test.step('Assert the answer is sent once', async () => {
            await expect.poll(() => copilotRun.requests.length).toBe(2);

            expect(lastMessageOf(await copilotRun.waitForRequest(1))).toMatchObject({
                content: 'User picked: Engineering',
                role: 'user',
            });

            await expect(copilotPanel.root.getByText('Alerts go to Engineering.')).toBeVisible();

            expect(copilotRun.requests).toHaveLength(2);
        });
    });

    test('a picker followed by a newer message is dimmed and disabled', async ({authenticatedPage: page, workflow}) => {
        await stubCopilotRun(page, COPILOT_WORKFLOW_EDITOR_CHAT_URL, [
            selectSlackConnectionTurn(),
            completedTurn(textMessage('Fine, no Slack.')),
        ]);

        const copilotPanel = await openWorkflowEditorCopilot(page, workflow);

        const connectionPicker = copilotPanel.root.getByRole('combobox');

        await test.step('Render the picker', async () => {
            await copilotPanel.sendAndWaitForTurnEnd('Post new leads to Slack');

            await expect(connectionPicker).toBeEnabled();

            await expect.poll(() => readAncestorOpacity(connectionPicker)).toBe(1);
        });

        await test.step('Send a newer message', async () => {
            await copilotPanel.sendAndWaitForTurnEnd('Never mind, skip Slack');
        });

        await test.step('Assert the picker is dimmed and disabled', async () => {
            await expect(connectionPicker).toBeDisabled();

            await expect.poll(() => readAncestorOpacity(connectionPicker)).toBeLessThan(1);
        });
    });

    test('a connection with a negative id is never offered', async ({authenticatedPage: page, workflow}) => {
        await stubCopilotRun(page, COPILOT_WORKFLOW_EDITOR_CHAT_URL, [selectSlackConnectionTurn()]);

        const copilotPanel = await openWorkflowEditorCopilot(page, workflow);

        await test.step('Open the connection picker', async () => {
            await copilotPanel.sendAndWaitForTurnEnd('Post new leads to Slack');

            await copilotPanel.root.getByRole('combobox').click();
        });

        await test.step('Assert only the persisted connections are listed', async () => {
            await expect(page.getByRole('option', {name: 'Slack Marketing'})).toBeVisible();

            await expect(page.getByRole('option', {name: 'Slack Support'})).toBeVisible();

            await expect(page.getByRole('option', {name: 'Slack Unsaved'})).toHaveCount(0);
        });
    });

    test('ASK mode sends mode ASK, offers no connection picker, and Build switches the mode', async ({
        authenticatedPage: page,
        workflow,
    }) => {
        const copilotRun = await stubCopilotRun(page, COPILOT_WORKFLOW_EDITOR_CHAT_URL, [
            completedTurn(textMessage('In Ask mode I can only explain. Switch to Build to change the workflow.')),
            selectSlackConnectionTurn(),
        ]);

        const copilotPanel = await openWorkflowEditorCopilot(page, workflow);

        await test.step('Send in Ask mode', async () => {
            await expect(copilotPanel.buildModeSwitch).not.toBeChecked();

            await copilotPanel.sendAndWaitForTurnEnd('Post new leads to Slack');
        });

        await test.step('Assert the Ask request and UI', async () => {
            expect((await copilotRun.waitForRequest(0)).state).toMatchObject({mode: 'ASK', source: 'WORKFLOW_EDITOR'});

            expect(((await copilotRun.waitForRequest(0)).tools ?? []).map((tool) => tool.name)).not.toContain(
                'selectConnection'
            );

            await expect(copilotPanel.root.getByRole('combobox')).toHaveCount(0);
        });

        await test.step('Switch to Build and send again', async () => {
            await copilotPanel.setBuildMode(true);

            await copilotPanel.sendAndWaitForTurnEnd('Build it');

            expect((await copilotRun.waitForRequest(1)).state).toMatchObject({mode: 'BUILD'});

            await expect(copilotPanel.root.getByRole('combobox')).toBeVisible();
        });
    });
});
