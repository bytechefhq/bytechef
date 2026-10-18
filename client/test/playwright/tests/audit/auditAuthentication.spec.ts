import {expect} from '@playwright/test';

import {loginTest, mergeTests} from '../../fixtures';
import {
    NON_ADMIN_USER,
    auditEventData,
    auditEventRows,
    filterAuditEvents,
    openAuditEventsPage,
    queryAuditEvents,
    selectAuditEventType,
} from '../../utils/auditUtils';

const test = mergeTests(loginTest());

test.describe('Audit Events authentication', () => {
    test('a failed login records AUTHENTICATION_FAILURE naming the attempted login and the reason', async ({
        authenticatedPage,
        browser,
    }) => {
        const anonymousContext = await browser.newContext();
        const anonymousPage = await anonymousContext.newPage();

        await anonymousPage.goto('/login');

        await anonymousPage.getByLabel('Email', {exact: true}).fill(NON_ADMIN_USER.email);
        await anonymousPage.getByLabel('Password', {exact: true}).fill('wrong-password');
        await anonymousPage.getByRole('button', {name: /log in/i}).click();

        await expect(anonymousPage).toHaveURL(/\/login/);

        await anonymousContext.close();

        await expect(async () => {
            const failures = await queryAuditEvents(authenticatedPage, {
                eventType: 'AUTHENTICATION_FAILURE',
                principal: NON_ADMIN_USER.email,
            });

            expect(failures.totalElements).toBeGreaterThan(0);
            expect(auditEventData(failures.content[0]).type).toBe(
                'org.springframework.security.authentication.BadCredentialsException'
            );
        }).toPass({timeout: 15000});

        await openAuditEventsPage(authenticatedPage);

        await selectAuditEventType(authenticatedPage, 'AUTHENTICATION_FAILURE');
        await filterAuditEvents(authenticatedPage, {principal: NON_ADMIN_USER.email});

        const firstRow = auditEventRows(authenticatedPage).first();

        await expect(firstRow).toContainText('AUTHENTICATION_FAILURE');
        await expect(firstRow).toContainText(NON_ADMIN_USER.email);

        await firstRow.click();

        await expect(authenticatedPage.getByRole('dialog').getByText('Bad credentials')).toBeVisible();
    });

    test('a successful login records AUTHENTICATION_SUCCESS for the user', async ({authenticatedPage}) => {
        await openAuditEventsPage(authenticatedPage);

        await selectAuditEventType(authenticatedPage, 'AUTHENTICATION_SUCCESS');
        await filterAuditEvents(authenticatedPage, {principal: 'admin'});

        const firstRow = auditEventRows(authenticatedPage).first();

        await expect(firstRow).toContainText('AUTHENTICATION_SUCCESS');
        await expect(firstRow).toContainText('admin');
    });
});
