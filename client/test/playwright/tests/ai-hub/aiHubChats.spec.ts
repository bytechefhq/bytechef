import {expect, mergeTests} from '@playwright/test';

import {aiHubChatsTest, loginTest} from '../../fixtures';
import {AiHubPage} from '../../pages/aiHubPage';
import {AI_HUB_CHAT_URL, completedTurn, stubAgUiRun, textMessage} from '../../utils/agUiStreamStub';
import {
    AI_HUB_PATH,
    createAiHubChatThroughApi,
    deleteAiHubChatThroughApi,
    prepareAiHub,
    readAppEnvironmentId,
    waitForHubWorkspaceId,
} from '../../utils/aiHubUtils';
import {readGraphQlOperationName, stubGraphQlOperation} from '../../utils/copilotUtils';
import getRandomString from '../../utils/getRandomString';

export const test = mergeTests(loginTest(), aiHubChatsTest);

test.describe('AI Hub chats', () => {
    test('the first turn creates the chat and it gets a title', async ({authenticatedPage: page}) => {
        const {chatTitle} = await prepareAiHub(page);

        await stubAgUiRun(page, AI_HUB_CHAT_URL, [completedTurn(textMessage('Hello there.'))]);

        const aiHubPage = new AiHubPage(page);

        await aiHubPage.goto();

        const createChatRequest = page.waitForRequest(
            (request) => request.method() === 'POST' && readGraphQlOperationName(request) === 'createAiHubChat'
        );

        await test.step('Send the first message', async () => {
            await aiHubPage.sendAndWaitForTurnEnd('Hello');

            await createChatRequest;
        });

        await test.step('Assert the chat exists under its title', async () => {
            await page.waitForURL(/\/automation\/ai-hub\/chats\/\d+$/);

            await expect(aiHubPage.chatTitle).toHaveText(chatTitle);

            await aiHubPage.chatSearchInput.fill(chatTitle);

            await expect(aiHubPage.chatRow(chatTitle)).toBeVisible();
        });
    });

    test('rename, archive, unarchive and delete a chat from the sidebar', async ({authenticatedPage: page}) => {
        const {chatTitle} = await prepareAiHub(page);

        const renamedTitle = `renamed_${getRandomString()}`;

        await stubAgUiRun(page, AI_HUB_CHAT_URL, [completedTurn(textMessage('Hello there.'))]);

        const aiHubPage = new AiHubPage(page);

        await aiHubPage.goto();

        await aiHubPage.sendAndWaitForTurnEnd('Hello');

        await expect(aiHubPage.chatTitle).toHaveText(chatTitle);

        await test.step('Search for the chat', async () => {
            await aiHubPage.chatSearchInput.fill(chatTitle);

            await expect(aiHubPage.chatRow(chatTitle)).toBeVisible();
        });

        await test.step('Rename it', async () => {
            await aiHubPage.openChatActions(chatTitle);

            await page.getByRole('menuitem', {name: 'Rename'}).click();

            const renameDialog = page.getByRole('dialog', {name: 'Rename chat'});

            await renameDialog.getByRole('textbox').fill(renamedTitle);

            await renameDialog.getByRole('button', {name: 'Rename'}).click();

            await aiHubPage.chatSearchInput.fill(renamedTitle);

            await expect(aiHubPage.chatRow(renamedTitle)).toBeVisible();

            await expect(aiHubPage.chatTitle).toHaveText(renamedTitle);
        });

        await test.step('Archive it', async () => {
            await aiHubPage.openChatActions(renamedTitle);

            await page.getByRole('menuitem', {name: 'Archive'}).click();

            await expect(aiHubPage.chatRow(renamedTitle)).toHaveCount(0);

            await page.getByRole('button', {exact: true, name: 'Archive'}).click();

            await expect(page.getByRole('heading', {exact: true, name: 'Archived chats'})).toBeVisible();

            await expect(aiHubPage.chatRow(renamedTitle)).toBeVisible();
        });

        await test.step('Unarchive it', async () => {
            await aiHubPage.openChatActions(renamedTitle);

            await page.getByRole('menuitem', {name: 'Unarchive'}).click();

            await expect(aiHubPage.chatRow(renamedTitle)).toHaveCount(0);

            await page.getByRole('button', {name: 'Back to active chats'}).click();

            await expect(aiHubPage.chatRow(renamedTitle)).toBeVisible();
        });

        await test.step('Delete it', async () => {
            await aiHubPage.openChatActions(renamedTitle);

            await page.getByRole('menuitem', {name: 'Delete'}).click();

            const deleteDialog = page.getByRole('alertdialog');

            await expect(deleteDialog).toContainText(`"${renamedTitle}" will be permanently deleted.`);

            await deleteDialog.getByRole('button', {name: 'Delete'}).click();

            await expect(aiHubPage.chatRow(renamedTitle)).toHaveCount(0);

            await page.waitForURL((url) => url.pathname === AI_HUB_PATH);
        });
    });

    test('reloading the page restores the transcript with its tool-call cards', async ({authenticatedPage: page}) => {
        await prepareAiHub(page);

        const aiHubPage = new AiHubPage(page);

        const hubWorkspaceId = waitForHubWorkspaceId(page);

        await aiHubPage.goto();

        const chat = await createAiHubChatThroughApi(page, {
            environment: await readAppEnvironmentId(page),
            workspaceId: await hubWorkspaceId,
        });

        await stubGraphQlOperation(page, 'aiHubChatMessages', () => ({
            aiHubChatMessages: [
                {
                    content: 'Which projects do I have?',
                    id: 'row-question',
                    role: 'user',
                    timestamp: Date.now() - 20000,
                    toolEventsJson: JSON.stringify([
                        {arguments: '{}', id: 'call-list-projects', kind: 'call', name: 'listProjects'},
                        {
                            id: 'call-list-projects',
                            kind: 'result',
                            name: 'listProjects',
                            response: JSON.stringify([{id: 1, name: 'Demo'}]),
                        },
                    ]),
                },
                {
                    content: 'You have one project, Demo.',
                    id: 'row-answer',
                    role: 'assistant',
                    timestamp: Date.now() - 10000,
                    toolEventsJson: null,
                },
            ],
        }));

        try {
            await test.step('Open the chat and show the tool calls', async () => {
                await aiHubPage.gotoChat(chat.id);

                await expect(page.getByText('You have one project, Demo.')).toBeVisible();

                await aiHubPage.chatOptionsButton.click();

                await page.getByRole('menuitem', {name: 'Show tool calls'}).click();

                await expect(page.getByRole('button', {name: /listProjects/})).toBeVisible();
            });

            await test.step('Reload and assert the transcript and the card', async () => {
                await page.reload();

                await expect(aiHubPage.userMessage('Which projects do I have?')).toBeVisible();

                await expect(page.getByText('You have one project, Demo.')).toBeVisible();

                await expect(page.getByRole('button', {name: /listProjects/})).toBeVisible();
            });
        } finally {
            await deleteAiHubChatThroughApi(page, chat);
        }
    });

    test('a chat id the user cannot see shows not found and leaks nothing', async ({authenticatedPage: page}) => {
        await prepareAiHub(page);

        const aiHubPage = new AiHubPage(page);

        const hubWorkspaceId = waitForHubWorkspaceId(page);

        await aiHubPage.goto();

        const chat = await createAiHubChatThroughApi(page, {
            environment: await readAppEnvironmentId(page),
            workspaceId: await hubWorkspaceId,
        });

        await deleteAiHubChatThroughApi(page, chat);

        await page.goto(`${AI_HUB_PATH}/chats/${chat.id}`);

        await expect(page.getByText('Chat not found')).toBeVisible({timeout: 30000});

        await expect(page.getByText('This chat does not exist, or you do not have access to it.')).toBeVisible();

        await expect(aiHubPage.composerInput).toHaveCount(0);

        await expect(aiHubPage.userMessages).toHaveCount(0);

        await page.getByRole('link', {name: 'Start a new chat'}).click();

        await page.waitForURL((url) => url.pathname === AI_HUB_PATH);
    });

    test('switching the environment resets the open chat', async ({authenticatedPage: page}) => {
        await prepareAiHub(page);

        await stubAgUiRun(page, AI_HUB_CHAT_URL, [completedTurn(textMessage('Hello there.'))]);

        const aiHubPage = new AiHubPage(page);

        await aiHubPage.goto();

        await aiHubPage.sendAndWaitForTurnEnd('Hello');

        await page.waitForURL(/\/automation\/ai-hub\/chats\/\d+$/);

        await test.step('Switch to staging', async () => {
            await page.getByRole('button', {name: 'DEVELOPMENT'}).click();

            await page.getByRole('menuitemradio', {name: /STAGING/}).click();
        });

        await test.step('Assert the hub went home with an empty chat', async () => {
            await page.waitForURL((url) => url.pathname === AI_HUB_PATH);

            await expect(page.getByRole('heading', {name: 'What should we get done?'})).toBeVisible();

            await expect(aiHubPage.userMessages).toHaveCount(0);
        });
    });
});
