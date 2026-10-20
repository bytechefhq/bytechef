import {type Page, expect, mergeTests} from '@playwright/test';

import {aiHubChatsTest, loginTest} from '../../fixtures';
import {type ChatWorkflowI, stubWorkflowChats, workflowChatLabel} from '../../fixtures/aiHubChats';
import {AiHubPage} from '../../pages/aiHubPage';
import {AI_HUB_CHAT_URL, completedTurn, createAgUiGate, stubAgUiRun, textMessage} from '../../utils/agUiStreamStub';
import {
    type AiHubChatRefI,
    aiHubChatPath,
    createAiHubChatThroughApi,
    deleteAiHubChatThroughApi,
    prepareAiHub,
    readAppEnvironmentId,
    waitForHubWorkspaceId,
} from '../../utils/aiHubUtils';
import {stubGraphQlOperation} from '../../utils/copilotUtils';
import getRandomString from '../../utils/getRandomString';

export const test = mergeTests(loginTest(), aiHubChatsTest);

const chatWorkflow = (projectName: string, workflowLabel: string): ChatWorkflowI => ({
    projectDeploymentId: String(Math.floor(Math.random() * 1000000) + 1),
    projectId: String(Math.floor(Math.random() * 1000000) + 1),
    projectName,
    projectWorkflowId: String(Math.floor(Math.random() * 1000000) + 1),
    workflowExecutionId: getRandomString(),
    workflowId: getRandomString(),
    workflowLabel,
});

interface WorkflowChatsHubI {
    aiHubPage: AiHubPage;
    createCalls: Array<Record<string, unknown>>;
    leadIntake: ChatWorkflowI;
    ticketTriage: ChatWorkflowI;
    workflowChat: AiHubChatRefI;
}

/**
 * Opens the hub with two chat workflows in the picker. Picking either one opens `workflowChat`, a real chat the test
 * must delete.
 */
async function openHubWithWorkflowChats(page: Page): Promise<WorkflowChatsHubI> {
    await prepareAiHub(page);

    const aiHubPage = new AiHubPage(page);

    const hubWorkspaceId = waitForHubWorkspaceId(page);

    await aiHubPage.goto();

    const workflowChat = await createAiHubChatThroughApi(page, {
        environment: await readAppEnvironmentId(page),
        workspaceId: await hubWorkspaceId,
    });

    try {
        const leadIntake = chatWorkflow(`Sales ${getRandomString()}`, 'Lead intake');
        const ticketTriage = chatWorkflow(`Support ${getRandomString()}`, 'Ticket triage');

        const {createCalls} = await stubWorkflowChats(page, {chatWorkflows: [leadIntake, ticketTriage], workflowChat});

        await aiHubPage.goto();

        return {aiHubPage, createCalls, leadIntake, ticketTriage, workflowChat};
    } catch (error) {
        await deleteAiHubChatThroughApi(page, workflowChat);

        throw error;
    }
}

async function pickWorkflowChat(aiHubPage: AiHubPage, chatWorkflowToPick: ChatWorkflowI): Promise<void> {
    await aiHubPage.modelPickerButton.click();

    await aiHubPage.openWorkflowChatsMenu();

    await aiHubPage.workflowChatMenuItem(workflowChatLabel(chatWorkflowToPick)).click();
}

test.describe('AI Hub workflow chats', () => {
    test('the picker lists workflow chats and its search narrows them', async ({authenticatedPage: page}) => {
        const {aiHubPage, leadIntake, ticketTriage, workflowChat} = await openHubWithWorkflowChats(page);

        try {
            await test.step('Open the Workflow chats cascade', async () => {
                await aiHubPage.modelPickerButton.click();

                await aiHubPage.openWorkflowChatsMenu();

                await expect(aiHubPage.workflowChatMenuItem(workflowChatLabel(leadIntake))).toBeVisible();

                await expect(aiHubPage.workflowChatMenuItem(workflowChatLabel(ticketTriage))).toBeVisible();
            });

            await test.step('Search for one workflow', async () => {
                await aiHubPage.modelPickerSearchInput.fill('triage');

                await aiHubPage.openWorkflowChatsMenu();

                await expect(aiHubPage.workflowChatMenuItem(workflowChatLabel(ticketTriage))).toBeVisible();

                await expect(aiHubPage.workflowChatMenuItem(workflowChatLabel(leadIntake))).toHaveCount(0);
            });
        } finally {
            await deleteAiHubChatThroughApi(page, workflowChat);
        }
    });

    test('@ opens the resource picker in a standard chat', async ({authenticatedPage: page}) => {
        await prepareAiHub(page);

        const aiHubPage = new AiHubPage(page);

        await aiHubPage.goto();

        await aiHubPage.composerInput.pressSequentially('@');

        await expect(aiHubPage.resourcePickerSearchInput).toBeVisible();

        await expect(aiHubPage.composerInput).toHaveValue('');
    });

    test('picking a workflow chat creates it and opens it without the LLM controls', async ({
        authenticatedPage: page,
    }) => {
        const {aiHubPage, createCalls, ticketTriage, workflowChat} = await openHubWithWorkflowChats(page);

        const label = workflowChatLabel(ticketTriage);

        try {
            await test.step('Pick the workflow chat', async () => {
                await pickWorkflowChat(aiHubPage, ticketTriage);

                await page.waitForURL((url) => url.pathname === aiHubChatPath(workflowChat.id));

                expect(createCalls).toEqual([
                    expect.objectContaining({
                        projectDeploymentId: ticketTriage.projectDeploymentId,
                        title: label,
                        workflowExecutionId: ticketTriage.workflowExecutionId,
                        workspaceId: String(workflowChat.workspaceId),
                    }),
                ]);
            });

            await test.step('Assert the header shows the workflow badge', async () => {
                await expect(aiHubPage.chatTitle).toHaveText(label);

                await expect(aiHubPage.workflowChatBadge).toBeVisible();
            });

            await test.step('Assert the model picker, resources and @ mention are gone', async () => {
                await expect(aiHubPage.composerInput).toBeEnabled();

                await expect(aiHubPage.modelPickerButton).toHaveCount(0);

                await expect(aiHubPage.addResourcesButton).toHaveCount(0);

                await aiHubPage.composerInput.pressSequentially('@');

                await expect(aiHubPage.composerInput).toHaveValue('@');

                await expect(aiHubPage.resourcePickerSearchInput).toHaveCount(0);
            });

            await test.step('Assert the sidebar row shows the workflow icon', async () => {
                await aiHubPage.chatSearchInput.fill(ticketTriage.projectName);

                await expect(aiHubPage.workflowChatIcon(label)).toBeVisible();
            });
        } finally {
            await deleteAiHubChatThroughApi(page, workflowChat);
        }
    });

    test('Stop in a workflow chat sends one cancel for the running turn', async ({authenticatedPage: page}) => {
        const turnGate = createAgUiGate();

        const {aiHubPage, leadIntake, workflowChat} = await openHubWithWorkflowChats(page);

        const hubRun = await stubAgUiRun(page, AI_HUB_CHAT_URL, [
            completedTurn(textMessage('The workflow is working.'), turnGate, textMessage('This never arrives.')),
        ]);

        const cancelRunCalls: Array<Record<string, unknown>> = [];

        await stubGraphQlOperation(page, 'cancelAiHubRun', (variables) => {
            cancelRunCalls.push(variables);

            return {cancelAiHubRun: true};
        });

        try {
            await pickWorkflowChat(aiHubPage, leadIntake);

            await page.waitForURL((url) => url.pathname === aiHubChatPath(workflowChat.id));

            await expect(aiHubPage.workflowChatBadge).toBeVisible();

            await test.step('Start a turn and stop it', async () => {
                await aiHubPage.send('Qualify this lead');

                await expect(page.getByText('The workflow is working.')).toBeVisible();

                await aiHubPage.stopGeneratingButton.click();

                await aiHubPage.waitForTurnEnd();
            });

            await test.step('Assert exactly one cancel names the chat and the run', async () => {
                const {runId} = await hubRun.waitForRequest(0);

                await expect
                    .poll(() => cancelRunCalls)
                    .toEqual([expect.objectContaining({id: String(workflowChat.id), runId})]);

                turnGate.release();

                await expect(page.getByText('This never arrives.')).toHaveCount(0);

                await expect(aiHubPage.composerInput).toBeEnabled();

                expect(cancelRunCalls).toHaveLength(1);
            });
        } finally {
            turnGate.release();

            await deleteAiHubChatThroughApi(page, workflowChat);
        }
    });
});
