import {type Locator, type Page, expect, mergeTests} from '@playwright/test';

import {loginTest, projectTest, workflowDeploymentTest} from '../../fixtures';
import {NotificationsPage} from '../../pages/notificationsPage';
import {uniqueRecipient, uniqueSuffix} from '../../utils/approvalRunUtils';
import {recipientQuery, waitForMailpitMessage} from '../../utils/mailpitUtils';
import {
    deleteNotificationByName,
    testWebhookNotificationViaApi,
    verifyWebhookSignature,
} from '../../utils/notificationUtils';
import {type CaughtRequestI, type RequestCatcherI, startRequestCatcher} from '../../utils/requestCatcher';
import {skipUnlessMailpitIsReachable} from '../../utils/testEnvironment';

export const test = mergeTests(loginTest(), projectTest, workflowDeploymentTest);

const DATADOG_URL = 'https://event-management-intake.datadoghq.com/api/v2/events';

const NEW_RELIC_URL = 'https://insights-collector.newrelic.com/v1/accounts/YOUR_ACCOUNT_ID/events';

let requestCatcher: RequestCatcherI;

function catcherUrl(path: string): string {
    return `${requestCatcher.baseUrl}${path}`;
}

function caughtRequestsFor(path: string): CaughtRequestI[] {
    return requestCatcher.caughtRequests.filter((caughtRequest) => caughtRequest.path === path);
}

async function waitForCaughtRequestCount(path: string, count: number): Promise<CaughtRequestI> {
    await expect(() => expect(caughtRequestsFor(path)).toHaveLength(count)).toPass({timeout: 30000});

    return caughtRequestsFor(path)[count - 1];
}

async function skipUnlessTheCatcherIsAllowed(page: Page): Promise<void> {
    const probeResult = await testWebhookNotificationViaApi(page, catcherUrl('/probe'));

    test.skip(
        !probeResult.success,
        `The server refuses ${requestCatcher.baseUrl} (${probeResult.error}); start it with ` +
            'bytechef.webhook-delivery.allowed-hosts=127.0.0.1'
    );
}

function headerValueInputs(dialog: Locator): Locator {
    return dialog.getByLabel('Header value', {exact: true});
}

async function expectInputValues(inputs: Locator, expectedValues: string[]): Promise<void> {
    await expect(inputs).toHaveCount(expectedValues.length);

    for (const [index, expectedValue] of expectedValues.entries()) {
        await expect(inputs.nth(index)).toHaveValue(expectedValue);
    }
}

test.describe('Webhook notifications', () => {
    test.beforeAll(async () => {
        requestCatcher = await startRequestCatcher();
    });

    test.afterAll(async () => {
        await requestCatcher.close();
    });

    test.beforeEach(async ({authenticatedPage}) => {
        await skipUnlessTheCatcherIsAllowed(authenticatedPage);
    });

    test('N1 the Datadog preset sends an Events API v2 body, signed once saved', async ({authenticatedPage}) => {
        const notificationsPage = new NotificationsPage(authenticatedPage);
        const name = `N1 Datadog ${uniqueSuffix()}`;
        const datadogPath = `/datadog-${uniqueSuffix()}`;
        const movedPath = `/datadog-moved-${uniqueSuffix()}`;

        try {
            await notificationsPage.goto();

            const createDialog = await notificationsPage.openCreateDialog({
                eventType: 'JOB_FAILED',
                name,
                type: 'WEBHOOK',
            });

            await test.step('The preset fills the URL, header names and body template, with empty values', async () => {
                await notificationsPage.applyWebhookPreset(createDialog, 'Datadog');

                await expect(createDialog.getByPlaceholder('https://example.com/ingest')).toHaveValue(DATADOG_URL);
                await expectInputValues(createDialog.getByRole('textbox', {name: 'Header name'}), [
                    'DD-API-KEY',
                    'DD-APPLICATION-KEY',
                ]);
                await expectInputValues(headerValueInputs(createDialog), ['', '']);
                await expect(createDialog.getByRole('textbox', {name: 'Body template'})).toHaveValue(
                    /"integration_id": "custom-events"/
                );
            });

            await test.step('Test is blocked until the header values are filled in', async () => {
                await createDialog.getByRole('button', {exact: true, name: 'Test'}).click();

                await expect(createDialog.getByText('Enter a value')).toHaveCount(2);
            });

            await test.step('An unsaved test is sent unsigned with the rendered Datadog body', async () => {
                await createDialog.getByPlaceholder('https://example.com/ingest').fill(catcherUrl(datadogPath));

                await headerValueInputs(createDialog).nth(0).fill('dd-api-key-value');
                await headerValueInputs(createDialog).nth(1).fill('dd-application-key-value');

                const testResult = await notificationsPage.runTest(createDialog);

                await expect(testResult).toContainText('Test sent');
                await expect(testResult).toContainText('HTTP 200');
                await expect(testResult).toContainText('{"ok":true}');
                await expect(testResult).toContainText(
                    'Sent without a signature; save the notification to get a signing secret.'
                );

                const caughtRequest = await waitForCaughtRequestCount(datadogPath, 1);

                expect(caughtRequest.headers['dd-api-key']).toBe('dd-api-key-value');
                expect(caughtRequest.headers['dd-application-key']).toBe('dd-application-key-value');
                expect(caughtRequest.headers['x-bytechef-signature']).toBeUndefined();

                const datadogEvent = JSON.parse(caughtRequest.body);

                expect(datadogEvent.data.type).toBe('event');
                expect(datadogEvent.data.attributes.category).toBe('alert');
                expect(datadogEvent.data.attributes.aggregation_key).toMatch(/^bytechef-job-/);
                expect(datadogEvent.data.attributes.attributes.custom.eventType).toBe('JOB_FAILED');
            });

            const signingSecret = await test.step('Saving shows the signing secret once', async () => {
                const shownSigningSecret = await notificationsPage.saveAndReadSigningSecret(createDialog);

                expect(shownSigningSecret).toMatch(/^whsec_/);

                await expect(authenticatedPage.getByText(name)).toBeVisible();

                return shownSigningSecret;
            });

            await test.step('A test of the saved notification is signed and reuses the stored header values', async () => {
                const editDialog = await notificationsPage.openEditDialog(name);

                await expect(headerValueInputs(editDialog).first()).toHaveAttribute(
                    'placeholder',
                    'Saved, leave unchanged to keep'
                );

                const testResult = await notificationsPage.runTest(editDialog);

                await expect(testResult).toContainText('Test sent');
                await expect(testResult).not.toContainText('without a signature');

                const caughtRequest = await waitForCaughtRequestCount(datadogPath, 2);

                expect(caughtRequest.headers['dd-api-key']).toBe('dd-api-key-value');
                expect(
                    verifyWebhookSignature(
                        String(caughtRequest.headers['x-bytechef-signature']),
                        caughtRequest.body,
                        signingSecret
                    )
                ).toBe(true);
            });

            await test.step('Changing the URL asks for the header values again and sends unsigned', async () => {
                const editDialog = notificationsPage.editNotificationDialog;

                await editDialog.getByPlaceholder('https://example.com/ingest').fill(catcherUrl(movedPath));

                await editDialog.getByRole('button', {exact: true, name: 'Test'}).click();

                await expect(editDialog.getByText('Enter a value')).toHaveCount(2);

                expect(caughtRequestsFor(movedPath)).toHaveLength(0);

                await headerValueInputs(editDialog).nth(0).fill('moved-api-key');
                await headerValueInputs(editDialog).nth(1).fill('moved-application-key');

                const testResult = await notificationsPage.runTest(editDialog);

                await expect(testResult).toContainText(
                    'Sent without a signature because the URL changed; save the notification to sign with its secret.'
                );

                const caughtRequest = await waitForCaughtRequestCount(movedPath, 1);

                expect(caughtRequest.headers['dd-api-key']).toBe('moved-api-key');
                expect(caughtRequest.headers['x-bytechef-signature']).toBeUndefined();
            });
        } finally {
            await deleteNotificationByName(authenticatedPage, name);
        }
    });

    test('N2 picking a preset over typed values asks before replacing them', async ({authenticatedPage}) => {
        const notificationsPage = new NotificationsPage(authenticatedPage);

        await notificationsPage.goto();

        const createDialog = await notificationsPage.openCreateDialog({
            eventType: 'JOB_FAILED',
            name: `N2 New Relic ${uniqueSuffix()}`,
            type: 'WEBHOOK',
        });

        const urlInput = createDialog.getByPlaceholder('https://example.com/ingest');

        await urlInput.fill('https://hooks.example.com/typed');

        const replaceDialog = authenticatedPage.getByRole('alertdialog', {
            name: 'Replace the URL, headers and body template?',
        });

        await test.step('Cancel keeps the typed URL', async () => {
            await notificationsPage.applyWebhookPreset(createDialog, 'New Relic');

            await replaceDialog.getByRole('button', {name: 'Cancel'}).click();

            await expect(urlInput).toHaveValue('https://hooks.example.com/typed');
        });

        await test.step('Replace fills the New Relic preset', async () => {
            await notificationsPage.applyWebhookPreset(createDialog, 'New Relic');

            await replaceDialog.getByRole('button', {name: 'Replace'}).click();

            await expect(urlInput).toHaveValue(NEW_RELIC_URL);
            await expectInputValues(createDialog.getByRole('textbox', {name: 'Header name'}), ['Api-Key']);

            const bodyTemplate = await createDialog.getByRole('textbox', {name: 'Body template'}).inputValue();

            expect(bodyTemplate.trim().startsWith('[')).toBe(true);
            expect(bodyTemplate).toContain('"eventType": "ByteChefNotification"');
        });
    });

    test('N3 a receiver error shows its status and body', async ({authenticatedPage}) => {
        const notificationsPage = new NotificationsPage(authenticatedPage);
        const failingPath = `/fail-${uniqueSuffix()}`;

        requestCatcher.respondWith(failingPath, 500, '{"error":"receiver exploded"}');

        await notificationsPage.goto();

        const createDialog = await notificationsPage.openCreateDialog({
            eventType: 'JOB_FAILED',
            name: `N3 failing ${uniqueSuffix()}`,
            type: 'WEBHOOK',
        });

        await createDialog.getByPlaceholder('https://example.com/ingest').fill(catcherUrl(failingPath));

        const testResult = await notificationsPage.runTest(createDialog);

        await expect(testResult).toContainText('Test failed');
        await expect(testResult).toContainText('HTTP 500');
        await expect(testResult).toContainText('receiver exploded');
    });

    test('N4 a link-local URL is refused before anything is sent', async ({authenticatedPage}) => {
        const notificationsPage = new NotificationsPage(authenticatedPage);

        await notificationsPage.goto();

        const createDialog = await notificationsPage.openCreateDialog({
            eventType: 'JOB_FAILED',
            name: `N4 link-local ${uniqueSuffix()}`,
            type: 'WEBHOOK',
        });

        await createDialog
            .getByPlaceholder('https://example.com/ingest')
            .fill('http://169.254.169.254/latest/meta-data');

        const testResult = await notificationsPage.runTest(createDialog);

        await expect(testResult).toContainText('Test failed');
        await expect(testResult).toContainText('not allowed');
    });

    test('N5 a completed job is delivered to the saved webhook and signed', async ({authenticatedPage, deployer}) => {
        const notificationsPage = new NotificationsPage(authenticatedPage);
        const name = `N5 job completed ${uniqueSuffix()}`;
        const jobPath = `/job-${uniqueSuffix()}`;
        const workflowLabel = `N5 workflow ${uniqueSuffix()}`;

        try {
            await notificationsPage.goto();

            const createDialog = await notificationsPage.openCreateDialog({
                eventType: 'JOB_COMPLETED',
                name,
                type: 'WEBHOOK',
            });

            await createDialog.getByPlaceholder('https://example.com/ingest').fill(catcherUrl(jobPath));
            await createDialog.getByRole('textbox', {name: 'Body template'}).clear();

            const signingSecret = await notificationsPage.saveAndReadSigningSecret(createDialog);

            const deployedProject = await deployer.deploy([
                {
                    description: workflowLabel,
                    label: workflowLabel,
                    tasks: [{name: 'var_1', parameters: {type: 'STRING', value: 'done'}, type: 'var/v1/set'}],
                    triggers: [{name: 'trigger_1', parameters: {}, type: 'webhook/v1/autoRespondWithHTTP200'}],
                },
            ]);

            const job = await deployer.triggerWebhookAndWaitForJob(deployedProject);

            await expect(() => {
                const jobDelivery = caughtRequestsFor(jobPath).find(
                    (caughtRequest) => String(JSON.parse(caughtRequest.body).jobId) === String(job.id)
                );

                expect(jobDelivery).toBeTruthy();
            }).toPass({timeout: 60000});

            const jobDelivery = caughtRequestsFor(jobPath).find(
                (caughtRequest) => String(JSON.parse(caughtRequest.body).jobId) === String(job.id)
            ) as CaughtRequestI;

            const jobPayload = JSON.parse(jobDelivery.body);

            expect(jobPayload.eventType).toBe('JOB_COMPLETED');
            expect(jobPayload.job.status).toBe('COMPLETED');
            expect(jobPayload.workflow.name).toBe(workflowLabel);
            expect(jobPayload.deliveryId).toBe(jobDelivery.headers['x-bytechef-delivery']);
            expect(
                verifyWebhookSignature(
                    String(jobDelivery.headers['x-bytechef-signature']),
                    jobDelivery.body,
                    signingSecret
                )
            ).toBe(true);
        } finally {
            await deleteNotificationByName(authenticatedPage, name);
        }
    });
});

test.describe('Email notifications', () => {
    test('N6 Test sends a sample email', async ({authenticatedPage, request}) => {
        await skipUnlessMailpitIsReachable(request);

        const notificationsPage = new NotificationsPage(authenticatedPage);
        const recipient = uniqueRecipient('n6');

        await notificationsPage.goto();

        const createDialog = await notificationsPage.openCreateDialog({
            eventType: 'JOB_FAILED',
            name: `N6 email ${uniqueSuffix()}`,
            type: 'EMAIL',
        });

        await createDialog.getByLabel('Email').fill(recipient);

        const testResult = await notificationsPage.runTest(createDialog);

        await expect(testResult).toContainText('Test sent');

        const mailpitMessage = await waitForMailpitMessage(request, recipientQuery(recipient));

        expect(mailpitMessage.To.map((mailRecipient) => mailRecipient.Address)).toContain(recipient);
    });
});
