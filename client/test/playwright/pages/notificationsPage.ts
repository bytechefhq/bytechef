import {type Locator, type Page, expect} from '@playwright/test';

export type NotificationEventType = 'JOB_COMPLETED' | 'JOB_FAILED';

export type WebhookPresetLabelType = 'Custom' | 'Datadog' | 'New Relic';

export class NotificationsPage {
    readonly createNotificationDialog: Locator;
    readonly editNotificationDialog: Locator;
    readonly newNotificationButton: Locator;
    readonly signingSecretDialog: Locator;

    private readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        this.createNotificationDialog = page.getByRole('dialog', {name: 'Create Notification'});
        this.editNotificationDialog = page.getByRole('dialog', {name: 'Edit Notification'});
        this.newNotificationButton = page.getByRole('button', {name: 'New Notification'}).first();
        this.signingSecretDialog = page.getByRole('dialog', {name: 'Signing Secret'});
    }

    async openCreateDialog({
        eventType,
        name,
        type,
    }: {
        eventType: NotificationEventType;
        name: string;
        type: 'EMAIL' | 'SLACK' | 'WEBHOOK';
    }): Promise<Locator> {
        await this.newNotificationButton.click();

        await expect(this.createNotificationDialog).toBeVisible();

        await this.createNotificationDialog.getByLabel('Name').fill(name);

        await this.createNotificationDialog.getByText('Select events').click();

        await this.page.getByRole('option', {name: eventType}).click();

        await this.page.keyboard.press('Escape');

        if (type !== 'EMAIL') {
            await this.createNotificationDialog.getByRole('combobox').filter({hasText: 'EMAIL'}).click();

            await this.page.getByRole('option', {exact: true, name: type}).click();
        }

        return this.createNotificationDialog;
    }

    async openEditDialog(name: string): Promise<Locator> {
        const notificationRow = this.page.getByRole('row').filter({hasText: name});

        await notificationRow.getByRole('button', {name: 'Edit notification'}).click();

        await expect(this.editNotificationDialog).toBeVisible();

        return this.editNotificationDialog;
    }

    async applyWebhookPreset(dialog: Locator, presetLabel: WebhookPresetLabelType): Promise<void> {
        await dialog.getByRole('combobox', {name: 'Preset'}).click();

        await this.page.getByRole('option', {exact: true, name: presetLabel}).click();
    }

    async runTest(dialog: Locator): Promise<Locator> {
        await dialog.getByRole('button', {exact: true, name: 'Test'}).click();

        const testResult = dialog.getByRole('status').filter({hasText: /Test (sent|failed)/});

        await expect(testResult).toBeVisible({timeout: 45000});

        return testResult;
    }

    async saveAndReadSigningSecret(dialog: Locator): Promise<string> {
        await dialog.getByRole('button', {exact: true, name: 'Save'}).click();

        await expect(this.signingSecretDialog).toBeVisible();

        const signingSecret = await this.signingSecretDialog.getByRole('textbox').inputValue();

        await this.signingSecretDialog.getByRole('button', {name: 'Done'}).click();

        await expect(this.signingSecretDialog).toBeHidden();

        return signingSecret;
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
