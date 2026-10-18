import {expect} from '@playwright/test';

import {loginTest, mergeTests} from '../../fixtures';
import {auditEventRows, filterAuditEvents, openAuditEventsPage, selectAuditEventType} from '../../utils/auditUtils';
import {createProject} from '../../utils/projectUtils';

const test = mergeTests(loginTest());

test.describe('Audit Events permission checks', () => {
    test('creating a project in the UI shows an ALLOWED permission check by the admin naming the facade method', async ({
        authenticatedPage,
    }) => {
        await createProject(authenticatedPage);

        await openAuditEventsPage(authenticatedPage);

        await selectAuditEventType(authenticatedPage, 'PERMISSION_CHECK');
        await filterAuditEvents(authenticatedPage, {dataSearch: 'ProjectFacadeImpl.createProject', principal: 'admin'});

        const firstRow = auditEventRows(authenticatedPage).first();

        await expect(firstRow).toContainText('PERMISSION_CHECK');
        await expect(firstRow).toContainText('admin');
        await expect(firstRow).toContainText('ALLOWED');

        await firstRow.click();

        const dialog = authenticatedPage.getByRole('dialog');

        await expect(
            dialog.getByText('com.bytechef.automation.configuration.facade.ProjectFacadeImpl.createProject')
        ).toBeVisible();
        await expect(dialog.getByText('method', {exact: true})).toBeVisible();
        await expect(dialog.getByText('result', {exact: true})).toBeVisible();
    });
});
