import {expect} from '@playwright/test';

import {loginTest, mergeTests} from '../../fixtures';
import {
    NON_ADMIN_USER,
    auditEventRows,
    openAuditEventsPage,
    queryAuditEvents,
    selectAuditEventType,
} from '../../utils/auditUtils';

const test = mergeTests(loginTest());

test.describe('Audit Events principal masking', () => {
    test.skip(
        !process.env.AUDIT_MASK_PRINCIPALS,
        'Needs a server started with bytechef.audit.mask-principals=true and AUDIT_MASK_PRINCIPALS=true here'
    );

    test('with masking enabled the page and the API show principals masked', async ({authenticatedPage, browser}) => {
        const anonymousContext = await browser.newContext();
        const anonymousPage = await anonymousContext.newPage();

        await anonymousPage.goto('/login');

        await anonymousPage.getByLabel('Email', {exact: true}).fill(NON_ADMIN_USER.email);
        await anonymousPage.getByLabel('Password', {exact: true}).fill('wrong-password');
        await anonymousPage.getByRole('button', {name: /log in/i}).click();

        await anonymousContext.close();

        const authenticationEvents = await queryAuditEvents(authenticatedPage, {eventType: 'AUTHENTICATION_SUCCESS'});

        expect(authenticationEvents.content.map((auditEvent) => auditEvent.principal)).toContain('a***');

        await openAuditEventsPage(authenticatedPage);

        await selectAuditEventType(authenticatedPage, 'AUTHENTICATION_FAILURE');

        await expect(auditEventRows(authenticatedPage).first()).toContainText('u***@localhost.com');
        await expect(authenticatedPage.getByText(NON_ADMIN_USER.email, {exact: true})).toHaveCount(0);
    });
});
