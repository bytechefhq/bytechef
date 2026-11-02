import {type Locator, type Page, expect} from '@playwright/test';

/**
 * Automation → Chats, talking to a deployed chat-trigger workflow, with the approval card a suspended run renders.
 */
export class ChatsPage {
    readonly approvalCard: Locator;
    readonly approvalCommentInput: Locator;
    readonly approveButton: Locator;
    readonly approvedStatus: Locator;
    readonly discardButton: Locator;
    readonly discardedStatus: Locator;
    readonly messageInput: Locator;
    readonly sendButton: Locator;

    private readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        this.approvalCard = page
            .locator('div')
            .filter({has: page.getByText('Approval required', {exact: true})})
            .filter({has: page.getByRole('button', {exact: true, name: 'Approve'})})
            .last();
        this.approvalCommentInput = this.approvalCard.getByLabel('Comment (optional)');
        this.approveButton = this.approvalCard.getByRole('button', {exact: true, name: 'Approve'});
        this.approvedStatus = page.getByText('Approved — the workflow is continuing.');
        this.discardButton = this.approvalCard.getByRole('button', {exact: true, name: 'Discard'});
        this.discardedStatus = page.getByRole('status').filter({hasText: 'Discarded.'});
        this.messageInput = page.getByPlaceholder('Send a message...');
        this.sendButton = page.getByRole('button', {name: 'Send message'});
    }

    /**
     * Opens the chat for a deployed workflow; the chat id is the last segment of its webhook URL.
     */
    async goto(webhookUrl: string): Promise<void> {
        const chatId = new URL(webhookUrl).pathname.split('/').pop();

        await this.page.goto(`/automation/chats/${chatId}`);

        await expect(this.messageInput).toBeVisible({timeout: 30000});
    }

    async sendMessage(message: string): Promise<void> {
        await this.messageInput.fill(message);

        await this.sendButton.click();
    }

    text(content: string | RegExp): Locator {
        return this.page.getByText(content).first();
    }
}
