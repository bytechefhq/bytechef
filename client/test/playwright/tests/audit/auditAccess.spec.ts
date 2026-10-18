import {expect} from '@playwright/test';

import {loginTest, mergeTests} from '../../fixtures';
import {
    AUDIT_EVENTS_ROUTE,
    NON_ADMIN_USER,
    auditEventData,
    auditEventsFilterBar,
    openAuditEventsPage,
    postGraphQl,
    queryAuditEvents,
} from '../../utils/auditUtils';
import {login} from '../../utils/login';

const test = mergeTests(loginTest());

test.describe('Audit Events access', () => {
    test('an admin opens Audit Events from the settings navigation and sees the event table', async ({
        authenticatedPage,
    }) => {
        await authenticatedPage.goto('/automation/settings');

        await authenticatedPage.getByRole('link', {exact: true, name: 'Audit Events'}).click();

        await expect(authenticatedPage).toHaveURL(new RegExp(`${AUDIT_EVENTS_ROUTE}$`));
        await expect(auditEventsFilterBar(authenticatedPage)).toBeVisible();

        for (const columnName of ['Date', 'Principal', 'Event Type', 'Result']) {
            await expect(authenticatedPage.getByRole('columnheader', {exact: true, name: columnName})).toBeVisible();
        }

        await expect(
            authenticatedPage.getByRole('cell', {exact: true, name: 'AUTHENTICATION_SUCCESS'}).first()
        ).toBeVisible();
    });

    test('a non-admin opening the Audit Events page is told they are not authorized', async ({browser}) => {
        const nonAdminContext = await browser.newContext();
        const nonAdminPage = await nonAdminContext.newPage();

        await login(nonAdminPage, NON_ADMIN_USER.email, NON_ADMIN_USER.password);

        await nonAdminPage.goto(AUDIT_EVENTS_ROUTE);

        await expect(nonAdminPage.getByText('You are not authorized to access this page.')).toBeVisible();
        await expect(auditEventsFilterBar(nonAdminPage)).toHaveCount(0);
        await expect(nonAdminPage.getByRole('table')).toHaveCount(0);

        await nonAdminContext.close();
    });

    test('a non-admin querying the audit API is denied and the denial is recorded against them', async ({
        authenticatedPage,
        browser,
    }) => {
        const nonAdminContext = await browser.newContext();
        const nonAdminPage = await nonAdminContext.newPage();

        await login(nonAdminPage, NON_ADMIN_USER.email, NON_ADMIN_USER.password);

        await nonAdminPage.goto('/automation/projects');

        for (const query of ['query { auditEventTypes }', 'query { auditEvents { totalElements } }']) {
            const body = await (await postGraphQl(nonAdminPage, query)).json();

            expect(body.data).toBeNull();
            expect(body.errors[0].extensions.errorCode).toBe('ACCESS_DENIED');
        }

        await nonAdminContext.close();

        const deniedEvents = await queryAuditEvents(authenticatedPage, {
            eventType: 'PERMISSION_CHECK',
            principal: 'user',
            size: 200,
        });

        const deniedMethods = deniedEvents.content
            .filter((auditEvent) => auditEvent.outcome === 'DENIED')
            .map((auditEvent) => auditEventData(auditEvent).method);

        expect(deniedMethods).toContain('com.bytechef.ee.platform.audit.facade.AuditEventFacadeImpl.fetchEventTypes');
        expect(deniedMethods).toContain('com.bytechef.ee.platform.audit.facade.AuditEventFacadeImpl.fetchAuditEvents');
    });

    test('an admin reading the audit log does not add read events to it', async ({authenticatedPage}) => {
        await openAuditEventsPage(authenticatedPage);

        await queryAuditEvents(authenticatedPage);

        const readEvents = await queryAuditEvents(authenticatedPage, {
            dataSearch: 'AuditEventFacadeImpl',
            principal: 'admin',
        });

        expect(readEvents.totalElements).toBe(0);
    });
});
