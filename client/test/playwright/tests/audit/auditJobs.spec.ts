import {expect} from '@playwright/test';

import {loginTest, mergeTests} from '../../fixtures';
import {
    COMPLETING_WORKFLOW,
    FAILING_WORKFLOW,
    WAITING_WORKFLOW,
    auditEventRows,
    deployWorkflows,
    expectJobAuditEvent,
    filterAuditEvents,
    openAuditEventRowWithDataEntry,
    openAuditEventsPage,
    putApi,
    runDeployedWorkflow,
    selectAuditEventType,
} from '../../utils/auditUtils';
import getRandomString from '../../utils/getRandomString';

const test = mergeTests(loginTest());

test.describe.configure({mode: 'serial'});

test.describe('Audit Events job lifecycle', () => {
    test('a manually run job that completes records JOB_CREATED, JOB_STARTED and JOB_COMPLETED under SYSTEM', async ({
        authenticatedPage,
    }) => {
        const deployedWorkflows = await deployWorkflows(authenticatedPage, `audit_${getRandomString()}`, [
            COMPLETING_WORKFLOW,
        ]);

        const jobId = await runDeployedWorkflow(authenticatedPage, deployedWorkflows, COMPLETING_WORKFLOW.label);

        for (const eventType of ['JOB_CREATED', 'JOB_STARTED', 'JOB_COMPLETED']) {
            await expectJobAuditEvent(authenticatedPage, jobId, eventType);
        }

        await openAuditEventsPage(authenticatedPage);

        await selectAuditEventType(authenticatedPage, 'JOB_COMPLETED');
        await filterAuditEvents(authenticatedPage, {principal: 'SYSTEM'});

        const firstRow = auditEventRows(authenticatedPage).first();

        await expect(firstRow).toContainText('JOB_COMPLETED');
        await expect(firstRow).toContainText('SYSTEM');

        await openAuditEventRowWithDataEntry(authenticatedPage, 'jobId', String(jobId));

        const dialog = authenticatedPage.getByRole('dialog');

        await expect(dialog.locator('dd').filter({hasText: new RegExp(`^${jobId}$`)})).toBeVisible();
        await expect(dialog.getByText('COMPLETED', {exact: true})).toBeVisible();
    });

    test('a job whose task fails records JOB_FAILED for that job', async ({authenticatedPage}) => {
        const deployedWorkflows = await deployWorkflows(authenticatedPage, `audit_${getRandomString()}`, [
            FAILING_WORKFLOW,
        ]);

        const jobId = await runDeployedWorkflow(authenticatedPage, deployedWorkflows, FAILING_WORKFLOW.label);

        await expectJobAuditEvent(authenticatedPage, jobId, 'JOB_FAILED');

        await openAuditEventsPage(authenticatedPage);

        await selectAuditEventType(authenticatedPage, 'JOB_FAILED');

        await openAuditEventRowWithDataEntry(authenticatedPage, 'jobId', String(jobId));

        await expect(
            authenticatedPage
                .getByRole('dialog')
                .locator('dd')
                .filter({hasText: new RegExp(`^${jobId}$`)})
        ).toBeVisible();
    });

    test('stopping a running job records JOB_STOPPED for that job', async ({authenticatedPage}) => {
        const deployedWorkflows = await deployWorkflows(authenticatedPage, `audit_${getRandomString()}`, [
            WAITING_WORKFLOW,
        ]);

        const jobId = await runDeployedWorkflow(authenticatedPage, deployedWorkflows, WAITING_WORKFLOW.label);

        await expectJobAuditEvent(authenticatedPage, jobId, 'JOB_STARTED');

        await putApi(authenticatedPage, `/api/platform/internal/jobs/${jobId}/stop`);

        await expectJobAuditEvent(authenticatedPage, jobId, 'JOB_STOPPED');

        await openAuditEventsPage(authenticatedPage);

        await selectAuditEventType(authenticatedPage, 'JOB_STOPPED');

        await openAuditEventRowWithDataEntry(authenticatedPage, 'jobId', String(jobId));

        await expect(
            authenticatedPage
                .getByRole('dialog')
                .locator('dd')
                .filter({hasText: new RegExp(`^${jobId}$`)})
        ).toBeVisible();
    });
});
