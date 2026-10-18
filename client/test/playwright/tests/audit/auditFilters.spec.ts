import {expect} from '@playwright/test';

import {loginTest, mergeTests} from '../../fixtures';
import {
    NON_ADMIN_USER,
    auditEventRows,
    filterAuditEvents,
    openAuditEventsPage,
    postGraphQl,
    queryAuditEvents,
    selectAuditEventType,
} from '../../utils/auditUtils';
import {login} from '../../utils/login';

const test = mergeTests(loginTest());

test.describe('Audit Events filters and paging', () => {
    test.beforeAll(async ({browser}) => {
        const nonAdminContext = await browser.newContext();
        const nonAdminPage = await nonAdminContext.newPage();

        await login(nonAdminPage, NON_ADMIN_USER.email, NON_ADMIN_USER.password);

        await nonAdminPage.goto('/automation/projects');

        for (let index = 0; index < 30; index++) {
            await postGraphQl(nonAdminPage, 'query { auditEventTypes }');
        }

        await nonAdminContext.close();
    });

    test('the event type filter lists recorded types and narrows the table to one type', async ({
        authenticatedPage,
    }) => {
        await openAuditEventsPage(authenticatedPage);

        await authenticatedPage.getByRole('combobox').click();

        for (const eventType of ['AUTHENTICATION_SUCCESS', 'PERMISSION_CHECK']) {
            await expect(authenticatedPage.getByRole('option', {exact: true, name: eventType})).toBeVisible();
        }

        await authenticatedPage.keyboard.press('Escape');

        await selectAuditEventType(authenticatedPage, 'AUTHENTICATION_SUCCESS');

        await expect(auditEventRows(authenticatedPage).first()).toContainText('AUTHENTICATION_SUCCESS');

        for (const rowText of await auditEventRows(authenticatedPage).allInnerTexts()) {
            expect(rowText).toContain('AUTHENTICATION_SUCCESS');
        }
    });

    test('the principal and data filters narrow the table, and an unmatched filter shows the empty state', async ({
        authenticatedPage,
    }) => {
        await openAuditEventsPage(authenticatedPage);

        await filterAuditEvents(authenticatedPage, {dataSearch: 'fetchEventTypes', principal: 'user'});

        await expect(auditEventRows(authenticatedPage).first()).toContainText('DENIED');

        for (const rowText of await auditEventRows(authenticatedPage).allInnerTexts()) {
            expect(rowText).toContain('user');
            expect(rowText).toContain('PERMISSION_CHECK');
        }

        await filterAuditEvents(authenticatedPage, {principal: 'nobody-with-this-login'});

        await expect(authenticatedPage.getByText('No audit events match the current filters.')).toBeVisible();
    });

    test('results page 25 at a time with Next and Previous', async ({authenticatedPage}) => {
        await openAuditEventsPage(authenticatedPage);

        await selectAuditEventType(authenticatedPage, 'PERMISSION_CHECK');
        await filterAuditEvents(authenticatedPage, {dataSearch: 'fetchEventTypes', principal: 'user'});

        await expect(authenticatedPage.getByText(/^Page 1 of \d+$/)).toBeVisible();
        await expect(auditEventRows(authenticatedPage)).toHaveCount(25);
        await expect(authenticatedPage.getByRole('button', {name: 'Previous'})).toBeDisabled();

        await authenticatedPage.getByRole('button', {name: 'Next'}).click();

        await expect(authenticatedPage.getByText(/^Page 2 of \d+$/)).toBeVisible();
        await expect(authenticatedPage.getByRole('button', {name: 'Previous'})).toBeEnabled();
        expect(await auditEventRows(authenticatedPage).count()).toBeGreaterThan(0);

        await authenticatedPage.getByRole('button', {name: 'Previous'}).click();

        await expect(authenticatedPage.getByText(/^Page 1 of \d+$/)).toBeVisible();
    });

    test("a from and to date of today keeps today's events, and a past date range returns none", async ({
        authenticatedPage,
    }) => {
        await openAuditEventsPage(authenticatedPage);

        const datePickerButtons = authenticatedPage.getByRole('button', {name: 'Pick a date'});

        await datePickerButtons.first().click();
        await authenticatedPage.getByRole('button', {name: /^Today/}).click();
        await authenticatedPage.keyboard.press('Escape');

        await authenticatedPage.getByRole('button', {name: 'Pick a date'}).click();
        await authenticatedPage
            .getByRole('button', {name: /^Today/})
            .last()
            .click();
        await authenticatedPage.keyboard.press('Escape');

        const today = await authenticatedPage.evaluate(() => new Date().toLocaleDateString());

        await expect(authenticatedPage.getByText(`${today} → ${today}`)).toBeVisible();

        await expect(auditEventRows(authenticatedPage).first()).toBeVisible();

        const yesterday = Date.now() - 24 * 60 * 60 * 1000;

        const pastEvents = await queryAuditEvents(authenticatedPage, {
            fromDate: yesterday - 60 * 60 * 1000,
            toDate: yesterday,
        });

        expect(pastEvents.totalElements).toBe(0);
    });

    test('Clear filters resets every filter and shows all events again', async ({authenticatedPage}) => {
        await openAuditEventsPage(authenticatedPage);

        await filterAuditEvents(authenticatedPage, {principal: 'nobody-with-this-login'});

        await expect(authenticatedPage.getByText('No audit events match the current filters.')).toBeVisible();

        await authenticatedPage.getByRole('button', {name: 'Clear filters'}).click();

        await expect(authenticatedPage.getByPlaceholder('e.g. admin')).toHaveValue('');
        await expect(auditEventRows(authenticatedPage).first()).toBeVisible();
    });

    test("filtering by the login the principal placeholder suggests finds that user's events", async ({
        authenticatedPage,
    }) => {
        await openAuditEventsPage(authenticatedPage);

        await selectAuditEventType(authenticatedPage, 'AUTHENTICATION_SUCCESS');
        await filterAuditEvents(authenticatedPage, {principal: 'admin'});

        await expect(authenticatedPage.getByText('No audit events match the current filters.')).toHaveCount(0);
        await expect(auditEventRows(authenticatedPage).first()).toContainText('AUTHENTICATION_SUCCESS');
    });
});
