import {expect} from '@playwright/test';

import {loginTest, mergeTests} from '../../fixtures';
import {
    NON_ADMIN_USER,
    auditEventRows,
    filterAuditEvents,
    openAuditEventsFilterMenu,
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

        const filterMenu = await openAuditEventsFilterMenu(authenticatedPage);

        for (const eventType of ['AUTHENTICATION_SUCCESS', 'PERMISSION_CHECK']) {
            await expect(filterMenu.getByRole('menuitem', {exact: true, name: eventType})).toBeVisible();
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

        const filterMenu = await openAuditEventsFilterMenu(authenticatedPage);

        await filterMenu.getByRole('menuitem', {name: 'Custom range'}).click();

        const customRange = authenticatedPage.getByRole('group', {name: 'Custom range'});

        await customRange.getByRole('button', {name: 'Pick a date'}).first().click();
        await authenticatedPage.getByRole('button', {name: /^Today/}).click();
        await authenticatedPage.keyboard.press('Escape');

        await customRange.getByRole('button', {name: 'Pick a date'}).click();
        await authenticatedPage
            .getByRole('button', {name: /^Today/})
            .last()
            .click();
        await authenticatedPage.keyboard.press('Escape');

        const today = await authenticatedPage.evaluate(() => new Date().toLocaleDateString());

        await expect(authenticatedPage.getByText(`From: ${today}`, {exact: true})).toBeVisible();
        await expect(authenticatedPage.getByText(`To: ${today}`, {exact: true})).toBeVisible();

        await expect(auditEventRows(authenticatedPage).first()).toBeVisible();

        const yesterday = Date.now() - 24 * 60 * 60 * 1000;

        const pastEvents = await queryAuditEvents(authenticatedPage, {
            fromDate: yesterday - 60 * 60 * 1000,
            toDate: yesterday,
        });

        expect(pastEvents.totalElements).toBe(0);
    });

    test('Clear all resets every filter and shows all events again', async ({authenticatedPage}) => {
        await openAuditEventsPage(authenticatedPage);

        await filterAuditEvents(authenticatedPage, {principal: 'nobody-with-this-login'});

        await expect(authenticatedPage.getByText('No audit events match the current filters.')).toBeVisible();

        await authenticatedPage.getByRole('button', {exact: true, name: 'Clear all'}).click();

        await expect(authenticatedPage.getByRole('textbox', {exact: true, name: 'Principal'})).toHaveValue('');
        await expect(authenticatedPage.getByText(/^Principal: /)).toHaveCount(0);
        await expect(auditEventRows(authenticatedPage).first()).toBeVisible();
    });

    test("filtering by an exact login finds that user's events", async ({authenticatedPage}) => {
        await openAuditEventsPage(authenticatedPage);

        await selectAuditEventType(authenticatedPage, 'AUTHENTICATION_SUCCESS');
        await filterAuditEvents(authenticatedPage, {principal: 'admin'});

        await expect(authenticatedPage.getByText('No audit events match the current filters.')).toHaveCount(0);
        await expect(auditEventRows(authenticatedPage).first()).toContainText('AUTHENTICATION_SUCCESS');
    });
});
