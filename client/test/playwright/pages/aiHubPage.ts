import {type Locator, type Page, expect} from '@playwright/test';

import {AI_HUB_PATH, aiHubChatPath} from '../utils/aiHubUtils';

export const SEND_A_MESSAGE_PLACEHOLDER = 'Send a message...';

export const PENDING_APPROVAL_PLACEHOLDER =
    'Approve or reject the tool call above, or send a message to withdraw it...';

export class AiHubPage {
    readonly addResourcesButton: Locator;
    readonly assistantMessages: Locator;
    readonly buildModeSwitch: Locator;
    readonly chatOptionsButton: Locator;
    readonly chatSearchInput: Locator;
    readonly chatTitle: Locator;
    readonly composerInput: Locator;
    readonly editComposerInput: Locator;
    readonly modelPickerButton: Locator;
    readonly modelPickerSearchInput: Locator;
    readonly newChatLink: Locator;
    readonly page: Page;
    readonly resourcePanel: Locator;
    readonly resourcePickerSearchInput: Locator;
    readonly retryBanner: Locator;
    readonly runErrors: Locator;
    readonly runningToolCallRows: Locator;
    readonly saveEditedMessageButton: Locator;
    readonly sendButton: Locator;
    readonly stopGeneratingButton: Locator;
    readonly toasts: Locator;
    readonly toolApprovalCards: Locator;
    readonly userMessages: Locator;
    readonly workflowChatBadge: Locator;
    readonly workflowChatsMenuTrigger: Locator;

    constructor(page: Page) {
        this.page = page;
        this.addResourcesButton = page.getByRole('button', {name: 'Add resources'});
        this.assistantMessages = page.locator('[data-role="assistant"]');
        this.buildModeSwitch = page.getByRole('switch', {name: /^Build mode/});
        this.chatOptionsButton = page.getByRole('button', {name: 'Chat options'});
        this.chatSearchInput = page.getByPlaceholder('Search chats...');
        this.chatTitle = page.getByRole('heading', {level: 2});
        this.composerInput = page.getByLabel('Message input');
        this.editComposerInput = page.locator('.aui-cc-edit-composer-input');
        this.modelPickerButton = page.getByRole('button', {name: 'Select LLM provider and model'});
        this.modelPickerSearchInput = page.getByLabel('Search providers');
        this.newChatLink = page.getByRole('link', {name: 'New Chat'});
        this.resourcePanel = page
            .locator('div')
            .filter({has: page.getByRole('button', {name: 'Close resource panel'})})
            .last();
        this.resourcePickerSearchInput = page.getByPlaceholder('Search resources…');
        this.retryBanner = page.getByTestId('ai-hub-retry-banner');
        this.runErrors = page.getByTestId('ai-hub-run-error');
        this.runningToolCallRows = this.assistantMessages.getByRole('status');
        this.saveEditedMessageButton = page.getByRole('button', {name: 'Save and resend'});
        this.sendButton = page.getByRole('button', {name: 'Send message'});
        this.stopGeneratingButton = page.getByRole('button', {name: 'Stop generating'});
        this.toasts = page.locator('[data-sonner-toast]');
        this.toolApprovalCards = page
            .locator('div')
            .filter({has: page.getByText('Tool approval required', {exact: true})})
            .filter({hasNot: page.locator('[data-role]')});
        this.userMessages = page.locator('[data-role="user"]');
        this.workflowChatBadge = page.getByText('Workflow chat', {exact: true});
        this.workflowChatsMenuTrigger = page.getByRole('menuitem', {name: 'Workflow chats'});
    }

    async goto(): Promise<void> {
        await this.page.goto(AI_HUB_PATH);

        await expect(this.composerInput).toBeEnabled({timeout: 30000});
    }

    async gotoChat(chatId: number): Promise<void> {
        await this.page.goto(aiHubChatPath(chatId));

        await expect(this.composerInput).toBeEnabled({timeout: 30000});
    }

    chatRow(chatTitle: string): Locator {
        return this.page.getByRole('button', {exact: true, name: chatTitle});
    }

    workflowChatIcon(chatTitle: string): Locator {
        return this.page.getByRole('button', {name: chatTitle}).getByRole('img', {name: 'Workflow chat'});
    }

    workflowChatMenuItem(label: string): Locator {
        return this.page.getByRole('menuitem', {exact: true, name: label});
    }

    async openWorkflowChatsMenu(): Promise<void> {
        await this.workflowChatsMenuTrigger.hover();

        await expect(this.workflowChatsMenuTrigger).toHaveAttribute('data-state', 'open');
    }

    userMessage(text: string): Locator {
        return this.userMessages.filter({hasText: text});
    }

    workflowTab(workflowName: string): Locator {
        return this.page.getByRole('button', {name: `Close ${workflowName}`});
    }

    async send(text: string): Promise<void> {
        await this.composerInput.fill(text);

        await this.sendButton.click();
    }

    async sendAndWaitForTurnEnd(text: string, turnTimeout?: number): Promise<void> {
        await this.send(text);

        await expect(this.userMessage(text)).toBeVisible();

        await this.waitForTurnEnd(turnTimeout);
    }

    async waitForTurnEnd(turnTimeout?: number): Promise<void> {
        await expect(this.stopGeneratingButton).toBeHidden({timeout: turnTimeout});

        await expect(this.sendButton).toBeVisible();
    }

    currentChatId(): number {
        return Number(new URL(this.page.url()).pathname.match(/\/chats\/(\d+)$/)?.[1]);
    }

    async setBuildMode(build: boolean): Promise<void> {
        if ((await this.buildModeSwitch.isChecked()) !== build) {
            await this.buildModeSwitch.click();
        }

        if (build) {
            await expect(this.buildModeSwitch).toBeChecked();
        } else {
            await expect(this.buildModeSwitch).not.toBeChecked();
        }
    }

    async editUserMessage(originalText: string, editedText: string): Promise<void> {
        const userMessage = this.userMessage(originalText);

        await userMessage.hover();

        await userMessage.getByRole('button', {name: 'Edit'}).click();

        await expect(this.editComposerInput).toBeVisible();

        await this.editComposerInput.fill(editedText);

        await this.saveEditedMessageButton.click();
    }

    async reloadLastReply(): Promise<void> {
        const lastAssistantMessage = this.assistantMessages.last();

        await lastAssistantMessage.hover();

        await lastAssistantMessage.getByRole('button', {name: 'Refresh'}).click();
    }

    async openChatActions(chatTitle: string): Promise<void> {
        const chatRow = this.chatRow(chatTitle);

        await chatRow.hover();

        await chatRow.locator('xpath=..').getByRole('button', {name: 'Chat actions'}).click();
    }
}
