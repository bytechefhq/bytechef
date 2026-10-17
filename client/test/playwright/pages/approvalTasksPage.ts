import {type Locator, type Page, expect} from '@playwright/test';

/**
 * Automation → Approval Tasks, with the "Pending run approvals" inbox above the task detail.
 */
export class ApprovalTasksPage {
    readonly clearStatusFilterButton: Locator;
    readonly completedMessage: Locator;
    readonly pendingApprovalsHeading: Locator;
    readonly searchInput: Locator;

    private readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        this.clearStatusFilterButton = page.getByRole('button', {name: 'Clear status filter'});
        this.completedMessage = page.getByText('This approval task has been completed.');
        this.pendingApprovalsHeading = page.getByRole('heading', {name: /^Pending run approvals \(\d+\)$/});
        this.searchInput = page.getByPlaceholder('Search approval tasks...');
    }

    async goto(): Promise<void> {
        await this.page.goto('/automation/approval-tasks');

        await expect(this.searchInput).toBeVisible({timeout: 30000});
    }

    async showAllStatuses(): Promise<void> {
        if (await this.clearStatusFilterButton.isVisible()) {
            await this.clearStatusFilterButton.click();
        }
    }

    async search(text: string): Promise<void> {
        await this.searchInput.fill(text);
    }

    taskCard(title: string): Locator {
        return this.page.getByRole('heading', {exact: true, level: 3, name: title});
    }

    detailSelect(fieldLabel: 'Assignee' | 'Priority' | 'Status'): Locator {
        return this.page.locator('fieldset').filter({hasText: fieldLabel}).getByRole('combobox').first();
    }

    pendingApprovalItem(workflowLabel: string): Locator {
        return this.page
            .getByRole('listitem')
            .filter({hasText: workflowLabel})
            .filter({has: this.page.getByRole('link', {name: 'Open form'})});
    }
}
