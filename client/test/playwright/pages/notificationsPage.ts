import {type Locator, type Page, expect} from '@playwright/test';

export type NotificationEventType = 'JOB_APPROVAL_ESCALATED' | 'JOB_APPROVAL_EXPIRING';

/**
 * Settings → Notifications.
 */
export class NotificationsPage {
    readonly createNotificationDialog: Locator;
    readonly newNotificationButton: Locator;

    private readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        this.createNotificationDialog = page.getByRole('dialog', {name: 'Create Notification'});
        this.newNotificationButton = page.getByRole('button', {name: 'New Notification'}).first();
    }

    async goto(): Promise<void> {
        await this.page.goto('/automation/settings/notifications');

        await expect(this.newNotificationButton).toBeVisible({timeout: 30000});
    }

    async createEmailNotification({
        email,
        eventType,
        name,
    }: {
        email: string;
        eventType: NotificationEventType;
        name: string;
    }): Promise<void> {
        await this.newNotificationButton.click();

        await expect(this.createNotificationDialog).toBeVisible();

        await this.createNotificationDialog.getByLabel('Name').fill(name);

        await this.createNotificationDialog.getByText('Select events').click();

        await this.page.getByRole('option', {name: eventType}).click();

        await this.page.keyboard.press('Escape');

        await this.createNotificationDialog.getByLabel('Email').fill(email);

        await this.createNotificationDialog.getByRole('button', {name: 'Save'}).click();

        await expect(this.createNotificationDialog).toBeHidden();

        await expect(this.page.getByText(name)).toBeVisible();
    }
}
