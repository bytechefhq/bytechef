import {expect, mergeTests} from '@playwright/test';

import {aiHubChatsTest, loginTest} from '../../fixtures';
import {AiHubPage} from '../../pages/aiHubPage';
import {
    AI_HUB_CHAT_URL,
    completedTurn,
    createAgUiGate,
    rejectedRun,
    stubAgUiRun,
    textMessage,
} from '../../utils/agUiStreamStub';
import {prepareAiHub, readRecordedToasts, recordToasts} from '../../utils/aiHubUtils';
import {stubGraphQlOperation} from '../../utils/copilotUtils';

export const test = mergeTests(loginTest(), aiHubChatsTest);

test.describe('AI Hub modes and roles', () => {
    test('a turn carries the mode the composer shows', async ({authenticatedPage: page}) => {
        await prepareAiHub(page);

        const hubRun = await stubAgUiRun(page, AI_HUB_CHAT_URL, [
            completedTurn(textMessage('Answered in Ask.')),
            completedTurn(textMessage('Answered in Build.')),
        ]);

        const aiHubPage = new AiHubPage(page);

        await aiHubPage.goto();

        await test.step('Send in Ask', async () => {
            await aiHubPage.setBuildMode(false);

            await aiHubPage.sendAndWaitForTurnEnd('Which projects do I have?');

            expect((await hubRun.waitForRequest(0)).state).toMatchObject({mode: 'ASK', source: 'AI_HUB'});
        });

        await test.step('Send in Build', async () => {
            await aiHubPage.setBuildMode(true);

            await aiHubPage.sendAndWaitForTurnEnd('Build me a workflow');

            expect((await hubRun.waitForRequest(1)).state).toMatchObject({mode: 'BUILD', source: 'AI_HUB'});
        });
    });

    test('a turn the server refuses for the workspace role keeps the user logged in and says why', async ({
        authenticatedPage: page,
    }) => {
        await prepareAiHub(page);

        await recordToasts(page);

        const refusal = rejectedRun(403, {error: 'Workspace is not accessible to the current user'});

        const hubRun = await stubAgUiRun(page, AI_HUB_CHAT_URL, [refusal, refusal]);

        const aiHubPage = new AiHubPage(page);

        await aiHubPage.goto();

        await aiHubPage.send('Build me a workflow');

        await hubRun.waitForRequest(0);

        await test.step('Assert the refusal is shown in the hub', async () => {
            await expect(page.getByText(/not accessible to the current user|HTTP 403/).first()).toBeVisible();
        });

        await test.step('Assert the user is still logged in on the hub', async () => {
            expect(new URL(page.url()).pathname).toMatch(/^\/automation\/ai-hub/);

            await expect(aiHubPage.composerInput).toBeVisible();

            expect(await readRecordedToasts(page)).not.toContainEqual(expect.objectContaining({text: 'Error'}));
        });
    });

    test('Stop cancels the running turn on the server', async ({authenticatedPage: page}) => {
        const turnGate = createAgUiGate();

        await prepareAiHub(page);

        const hubRun = await stubAgUiRun(page, AI_HUB_CHAT_URL, [
            completedTurn(textMessage('Working on it.'), turnGate, textMessage('This never arrives.')),
        ]);

        const cancelRunCalls: Array<Record<string, unknown>> = [];

        await stubGraphQlOperation(page, 'cancelAiHubRun', (variables) => {
            cancelRunCalls.push(variables);

            return {cancelAiHubRun: true};
        });

        const aiHubPage = new AiHubPage(page);

        await aiHubPage.goto();

        await test.step('Start a turn and stop it', async () => {
            await aiHubPage.send('Build me a workflow');

            await expect(page.getByText('Working on it.')).toBeVisible();

            await aiHubPage.stopGeneratingButton.click();

            await aiHubPage.waitForTurnEnd();
        });

        await test.step('Assert the cancel names the chat and the run', async () => {
            const chatId = new URL(page.url()).pathname.split('/').at(-1);

            await expect
                .poll(() => cancelRunCalls)
                .toEqual([expect.objectContaining({id: chatId, runId: (await hubRun.waitForRequest(0)).runId})]);

            turnGate.release();

            await expect(page.getByText('This never arrives.')).toHaveCount(0);

            await expect(aiHubPage.composerInput).toBeEnabled();
        });
    });
});
