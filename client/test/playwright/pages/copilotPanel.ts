import {type Locator, type Page, expect} from '@playwright/test';

export class CopilotPanel {
    readonly assistantMessages: Locator;
    readonly buildModeSwitch: Locator;
    readonly closeButton: Locator;
    readonly composerInput: Locator;
    readonly editComposerInput: Locator;
    readonly editorCopilotButton: Locator;
    readonly emptyTranscriptWelcome: Locator;
    readonly modelPickerButton: Locator;
    readonly root: Locator;
    readonly runErrors: Locator;
    readonly sendButton: Locator;
    readonly stopGeneratingButton: Locator;
    readonly updateEditedMessageButton: Locator;
    readonly userMessages: Locator;

    constructor(page: Page) {
        this.root = page.locator('aside').filter({has: page.getByRole('heading', {name: 'AI Copilot'})});
        this.assistantMessages = this.root.locator('[data-role="assistant"]');
        this.buildModeSwitch = this.root.getByRole('switch', {name: /^Build mode/});
        this.closeButton = this.root.getByRole('button', {name: 'Close Copilot panel'});
        this.composerInput = this.root.getByLabel('Message input');
        this.editComposerInput = this.root.locator('[data-slot="aui_edit-composer-wrapper"] textarea');
        this.editorCopilotButton = page.getByRole('button', {exact: true, name: 'Copilot'});
        this.emptyTranscriptWelcome = this.root.getByRole('heading', {name: 'How can I help you today?'});
        this.modelPickerButton = this.root.getByRole('button', {name: 'Select LLM provider and model'});
        this.runErrors = this.root.getByRole('alert');
        this.sendButton = this.root.getByRole('button', {name: 'Send message'});
        this.stopGeneratingButton = this.root.getByRole('button', {name: 'Stop generating'});
        this.updateEditedMessageButton = this.root.getByRole('button', {name: 'Update'});
        this.userMessages = this.root.locator('[data-role="user"]');
    }

    userMessage(text: string): Locator {
        return this.userMessages.filter({hasText: text});
    }

    async openFromWorkflowEditor(): Promise<void> {
        await this.openPanelFromWorkflowEditor();

        await expect(this.composerInput).toBeVisible();
    }

    async openPanelFromWorkflowEditor(): Promise<void> {
        await expect(this.editorCopilotButton).toBeVisible({timeout: 30000});

        await this.editorCopilotButton.click();

        await expect(this.root).toBeVisible();
    }

    async close(): Promise<void> {
        await this.closeButton.click();

        await expect(this.root).toBeHidden();
    }

    async send(text: string): Promise<void> {
        await this.composerInput.fill(text);

        await this.sendButton.click();
    }

    async sendAndWaitForTurnEnd(text: string): Promise<void> {
        await this.send(text);

        await expect(this.userMessage(text)).toBeVisible();

        await this.waitForTurnEnd();
    }

    async waitForTurnEnd(): Promise<void> {
        await expect(this.stopGeneratingButton).toBeHidden();

        await expect(this.sendButton).toBeVisible();
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

        await this.updateEditedMessageButton.click();
    }

    async reloadLastReply(): Promise<void> {
        const lastAssistantMessage = this.assistantMessages.last();

        await lastAssistantMessage.hover();

        await lastAssistantMessage.getByRole('button', {name: 'Refresh'}).click();
    }
}
