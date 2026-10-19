import {expect, mergeTests} from '@playwright/test';

import {loginTest, projectTest, workflowTest} from '../../fixtures';
import {
    COPILOT_WORKFLOW_EDITOR_CHAT_URL,
    completedTurn,
    lastMessageOf,
    stubCopilotRun,
    textMessage,
    toolCall,
} from '../../utils/agUiStreamStub';
import {openWorkflowEditorCopilot, prepareCopilot} from '../../utils/copilotUtils';

export const test = mergeTests(loginTest(), projectTest, workflowTest);

const DELEGATED_QUESTION_RESULT = JSON.stringify({
    awaitingAnswer: true,
    kind: 'ask-user-question',
    questions: [
        {
            header: 'Schedule',
            multiSelect: false,
            options: [
                {description: 'Runs at 9:00 every weekday', label: 'Every weekday'},
                {description: 'Runs at 9:00 every Monday', label: 'Every Monday'},
            ],
            question: 'How often should the report run?',
        },
    ],
});

test.describe('Copilot delegated specialist question', () => {
    test.beforeEach(async ({authenticatedPage: page}) => {
        await prepareCopilot(page);
    });

    test('a buildWorkflow result carrying an ask-user-question payload renders the question card', async ({
        authenticatedPage: page,
        workflow,
    }) => {
        const copilotRun = await stubCopilotRun(page, COPILOT_WORKFLOW_EDITOR_CHAT_URL, [
            completedTurn(
                toolCall('buildWorkflow', {request: 'Send a weekly report'}, DELEGATED_QUESTION_RESULT),
                textMessage('The workflow builder needs one decision from you.')
            ),
            completedTurn(textMessage('The report runs every Monday.')),
        ]);

        const copilotPanel = await openWorkflowEditorCopilot(page, workflow);

        await test.step('Render the delegated question', async () => {
            await copilotPanel.setBuildMode(true);

            await copilotPanel.sendAndWaitForTurnEnd('Send a weekly report');

            await expect(copilotPanel.root.getByText('How often should the report run?')).toBeVisible();

            await expect(copilotPanel.root.getByRole('button', {name: 'Other…'})).toBeVisible();
        });

        await test.step('Answer the delegated question', async () => {
            await copilotPanel.root.getByRole('button', {name: 'Every Monday'}).click();

            await expect(copilotPanel.root.getByText('The report runs every Monday.')).toBeVisible();
        });

        await test.step('Assert one User picked message was sent', async () => {
            expect(copilotRun.requests).toHaveLength(2);

            expect(lastMessageOf(await copilotRun.waitForRequest(1))).toMatchObject({
                content: 'User picked: Every Monday',
                role: 'user',
            });

            const userPickedMessages = (await copilotRun.waitForRequest(1)).messages.filter(
                (message) => message.role === 'user' && message.content?.startsWith('User picked:')
            );

            expect(userPickedMessages).toHaveLength(1);
        });
    });

    test('a buildWorkflow result of prose starting with a brace renders no card and logs no warning', async ({
        authenticatedPage: page,
        workflow,
    }) => {
        const consoleWarnings: string[] = [];

        page.on('console', (consoleMessage) => {
            if (consoleMessage.type() === 'warning') {
                consoleWarnings.push(consoleMessage.text());
            }
        });

        await stubCopilotRun(page, COPILOT_WORKFLOW_EDITOR_CHAT_URL, [
            completedTurn(
                toolCall(
                    'buildWorkflow',
                    {request: 'Explain the workflow'},
                    '{Weekly report} is built: a schedule trigger feeds a Slack message task.'
                ),
                textMessage('The workflow is ready.')
            ),
        ]);

        const copilotPanel = await openWorkflowEditorCopilot(page, workflow);

        await test.step('Run the turn', async () => {
            await copilotPanel.sendAndWaitForTurnEnd('Explain the workflow');

            await expect(copilotPanel.root.getByText('The workflow is ready.')).toBeVisible();
        });

        await test.step('Assert no card and no parse warning', async () => {
            await expect(copilotPanel.root.getByRole('button', {name: 'Other…'})).toHaveCount(0);

            await expect(copilotPanel.root.getByRole('combobox')).toHaveCount(0);

            expect(consoleWarnings.filter((warning) => warning.includes('Failed to parse tool-result JSON'))).toEqual(
                []
            );
        });
    });
});
