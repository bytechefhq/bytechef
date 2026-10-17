import {expect, mergeTests} from '@playwright/test';

import {loginTest, projectTest, workflowDeploymentTest} from '../../fixtures';
import {ApprovalFormPage} from '../../pages/approvalFormPage';
import {ApprovalTasksPage} from '../../pages/approvalTasksPage';
import {graphqlRequest} from '../../utils/apiUtils';
import {deployAndStartApprovalRun, startApprovalRun, uniqueSuffix} from '../../utils/approvalRunUtils';
import {
    type ApprovalTaskI,
    UPDATE_APPROVAL_TASK_MUTATION,
    getPendingApprovals,
    getTokenExpiryEpochSeconds,
    resumeJob,
    waitForApprovalTaskStatus,
    waitForPendingApproval,
} from '../../utils/approvalUtils';
import {buildApprovalWorkflow, buildPlainTimeWaitWorkflow} from '../../utils/approvalWorkflowDefinitions';
import {stopJob, waitForJobStatus} from '../../utils/jobUtils';

export const test = mergeTests(loginTest(), projectTest, workflowDeploymentTest);

test.describe('Approval Tasks', () => {
    test('A11 the Approval Task channel opens one task per run', async ({authenticatedPage, deployer}) => {
        const formTitle = `A11 approval ${uniqueSuffix()}`;

        const approvalRun = await deployAndStartApprovalRun({
            definition: buildApprovalWorkflow({
                channels: ['approvalTask'],
                expiresIn: 2,
                expiresInUnit: 'DAYS',
                formTitle,
                label: `A11 tasks ${uniqueSuffix()}`,
            }),
            deployer,
            page: authenticatedPage,
        });

        const approvalTasksPage = new ApprovalTasksPage(authenticatedPage);

        await test.step('One Open task named after the form title, due at the approval expiry', async () => {
            const approvalTask = await waitForApprovalTaskStatus(authenticatedPage, formTitle, 'OPEN');

            const pendingApproval = await waitForPendingApproval(authenticatedPage, approvalRun.jobId);

            expect(Date.parse(approvalTask.dueDate as string)).toBe(Date.parse(pendingApproval.expiresAt as string));

            await approvalTasksPage.goto();

            await approvalTasksPage.search(formTitle);

            await expect(approvalTasksPage.taskCard(formTitle)).toBeVisible();
        });

        await test.step('Resolving the run through the task form completes the task at once', async () => {
            await approvalTasksPage.taskCard(formTitle).click();

            const approvalFormPage = new ApprovalFormPage(authenticatedPage);

            await approvalFormPage.approveButton.click();

            await waitForJobStatus(authenticatedPage, approvalRun.jobId, 'COMPLETED');

            await waitForApprovalTaskStatus(authenticatedPage, formTitle, 'COMPLETED');
        });
    });

    test('A12 task rows follow stop and resolution of their runs', async ({authenticatedPage, deployer, request}) => {
        const definition = buildApprovalWorkflow({
            channels: ['approvalTask'],
            formTitle: 'A12 approval ${trigger_1.body.n}',
            label: `A12 reconcile ${uniqueSuffix()}`,
        });

        const deployedProject = await deployer.deploy([definition]);

        const suffix = uniqueSuffix();

        const startNamedRun = (runName: string) =>
            startApprovalRun({body: {n: `${runName}-${suffix}`}, deployedProject, deployer, page: authenticatedPage});

        const stoppedRun = await startNamedRun('stopped');
        const resolvedRun = await startNamedRun('resolved');
        const pendingRun = await startNamedRun('pending');

        await test.step('A stopped run expires its task, a resolved run completes it', async () => {
            expect((await stopJob(authenticatedPage, stoppedRun.jobId)).status()).toBe(204);

            expect((await resumeJob({body: {approved: true}, request, token: resolvedRun.token})).status()).toBe(204);

            await waitForApprovalTaskStatus(authenticatedPage, `A12 approval stopped-${suffix}`, 'EXPIRED');
            await waitForApprovalTaskStatus(authenticatedPage, `A12 approval resolved-${suffix}`, 'COMPLETED');
        });

        await test.step('A row whose run still waits is left Open', async () => {
            await waitForApprovalTaskStatus(authenticatedPage, `A12 approval pending-${suffix}`, 'OPEN');

            await resumeJob({body: {approved: true}, request, token: pendingRun.token});
        });
    });

    test('A13 approval task status rules', async ({authenticatedPage, deployer, request}) => {
        const definition = buildApprovalWorkflow({
            channels: ['approvalTask'],
            formTitle: 'A13 approval ${trigger_1.body.n}',
            label: `A13 status ${uniqueSuffix()}`,
        });

        const deployedProject = await deployer.deploy([definition]);

        const suffix = uniqueSuffix();
        const completedTitle = `A13 approval completed-${suffix}`;
        const openTitle = `A13 approval open-${suffix}`;

        const completedRun = await startApprovalRun({
            body: {n: `completed-${suffix}`},
            deployedProject,
            deployer,
            page: authenticatedPage,
        });

        const openRun = await startApprovalRun({
            body: {n: `open-${suffix}`},
            deployedProject,
            deployer,
            page: authenticatedPage,
        });

        expect((await resumeJob({body: {approved: true}, request, token: completedRun.token})).status()).toBe(204);

        const completedTask = await waitForApprovalTaskStatus(authenticatedPage, completedTitle, 'COMPLETED');
        let openTask = await waitForApprovalTaskStatus(authenticatedPage, openTitle, 'OPEN');

        const updateApprovalTask = (approvalTask: ApprovalTaskI, changes: Partial<ApprovalTaskI>) =>
            graphqlRequest<{updateApprovalTask: ApprovalTaskI}>(authenticatedPage, UPDATE_APPROVAL_TASK_MUTATION, {
                approvalTask: {
                    id: approvalTask.id,
                    name: approvalTask.name,
                    version: approvalTask.version,
                    ...changes,
                },
            });

        await test.step('A completed task is read-only in the UI', async () => {
            const approvalTasksPage = new ApprovalTasksPage(authenticatedPage);

            await approvalTasksPage.goto();

            await approvalTasksPage.showAllStatuses();

            await approvalTasksPage.search(completedTitle);

            await approvalTasksPage.taskCard(completedTitle).click();

            await expect(approvalTasksPage.completedMessage).toBeVisible();

            await expect(approvalTasksPage.detailSelect('Status')).toBeDisabled();
            await expect(approvalTasksPage.detailSelect('Priority')).toBeDisabled();
            await expect(approvalTasksPage.detailSelect('Assignee')).toBeDisabled();
        });

        await test.step('A completed task cannot be reopened or expired', async () => {
            for (const status of ['OPEN', 'EXPIRED'] as const) {
                const updateResponse = await updateApprovalTask(completedTask, {status});

                expect(updateResponse.errors?.[0]?.message).toContain(
                    `Cannot change approval task ${completedTask.id} to ${status}; it is already COMPLETED`
                );
                expect(updateResponse.errors?.[0]?.extensions?.classification).toBe('BAD_REQUEST');
            }
        });

        await test.step('Only the resolution can complete an open task', async () => {
            const updateResponse = await updateApprovalTask(openTask, {status: 'COMPLETED'});

            expect(updateResponse.errors?.[0]?.message).toContain(
                `Cannot mark approval task ${openTask.id} as COMPLETED; only its resolution can`
            );
            expect(updateResponse.errors?.[0]?.extensions?.classification).toBe('BAD_REQUEST');
        });

        await test.step('Open and In Progress can be switched both ways', async () => {
            for (const status of ['IN_PROGRESS', 'OPEN'] as const) {
                const updateResponse = await updateApprovalTask(openTask, {status});

                expect(updateResponse.errors ?? []).toHaveLength(0);

                openTask = updateResponse.data!.updateApprovalTask;

                expect(openTask.status).toBe(status);
            }
        });

        await test.step('A priority-only update keeps status, description and due date', async () => {
            const updateResponse = await updateApprovalTask(openTask, {priority: 'HIGH'});

            expect(updateResponse.errors ?? []).toHaveLength(0);

            const updatedTask = updateResponse.data!.updateApprovalTask;

            expect(updatedTask.priority).toBe('HIGH');
            expect(updatedTask.status).toBe(openTask.status);
            expect(updatedTask.description).toBe(openTask.description);
            expect(updatedTask.dueDate).toBe(openTask.dueDate);

            await resumeJob({body: {approved: true}, request, token: openRun.token});
        });
    });

    test('A14 the pending approvals inbox lists only runs waiting on an approval', async ({
        authenticatedPage,
        context,
        deployer,
        request,
    }) => {
        const suffix = uniqueSuffix();
        const pendingLabel = `A14 pending ${suffix}`;
        const resolvedLabel = `A14 resolved ${suffix}`;
        const cancelledLabel = `A14 cancelled ${suffix}`;
        const failedLabel = `A14 failed ${suffix}`;
        const waitLabel = `A14 plain wait ${suffix}`;

        const deployedProject = await deployer.deploy([
            buildApprovalWorkflow({channels: ['approvalTask'], formTitle: pendingLabel, label: pendingLabel}),
            buildApprovalWorkflow({channels: ['approvalTask'], formTitle: resolvedLabel, label: resolvedLabel}),
            buildApprovalWorkflow({channels: ['approvalTask'], formTitle: cancelledLabel, label: cancelledLabel}),
            buildApprovalWorkflow({channels: ['telegram', 'slack'], formTitle: failedLabel, label: failedLabel}),
            buildPlainTimeWaitWorkflow(waitLabel),
        ]);

        const pendingRuns = [
            await startApprovalRun({deployedProject, deployer, page: authenticatedPage, workflowLabel: pendingLabel}),
            await startApprovalRun({deployedProject, deployer, page: authenticatedPage, workflowLabel: pendingLabel}),
        ];

        const resolvedRun = await startApprovalRun({
            deployedProject,
            deployer,
            page: authenticatedPage,
            workflowLabel: resolvedLabel,
        });

        const cancelledRun = await startApprovalRun({
            deployedProject,
            deployer,
            page: authenticatedPage,
            workflowLabel: cancelledLabel,
        });

        const failedJob = await deployer.triggerWebhookAndWaitForJob(deployedProject, {}, failedLabel);
        const waitJob = await deployer.triggerWebhookAndWaitForJob(deployedProject, {}, waitLabel);

        await test.step('Resolve, cancel and fail the control runs', async () => {
            expect((await resumeJob({body: {approved: true}, request, token: resolvedRun.token})).status()).toBe(204);
            expect((await stopJob(authenticatedPage, cancelledRun.jobId)).status()).toBe(204);

            await waitForJobStatus(authenticatedPage, resolvedRun.jobId, 'COMPLETED');
            await waitForJobStatus(authenticatedPage, cancelledRun.jobId, 'CANCELLED');
            await waitForJobStatus(authenticatedPage, failedJob.id, 'FAILED');
            await waitForJobStatus(authenticatedPage, waitJob.id, 'STOPPED');
        });

        await test.step('The pendingApprovals query lists exactly the waiting approval runs', async () => {
            const pendingApprovals = await getPendingApprovals(authenticatedPage);

            const listedJobIds = pendingApprovals.map((pendingApproval) => pendingApproval.jobId);

            for (const pendingRun of pendingRuns) {
                expect(listedJobIds).toContain(pendingRun.jobId);
            }

            for (const unlistedJobId of [resolvedRun.jobId, cancelledRun.jobId, failedJob.id, waitJob.id]) {
                expect(listedJobIds).not.toContain(unlistedJobId);
            }

            const pendingApproval = pendingApprovals.find(
                (listedApproval) => listedApproval.jobId === pendingRuns[0].jobId
            );

            expect(pendingApproval?.workflowLabel).toBe(pendingLabel);
            expect(pendingApproval?.createdDate).toBeTruthy();
            expect(pendingApproval?.expiresAt).toBeTruthy();

            const linkLifetimeSeconds =
                getTokenExpiryEpochSeconds(pendingRuns[0].token) -
                Date.parse(pendingApproval?.expiresAt as string) / 1000;

            expect(linkLifetimeSeconds).toBeGreaterThan(0);
            expect(linkLifetimeSeconds).toBeLessThanOrEqual(3600 + 5);
        });

        const approvalTasksPage = new ApprovalTasksPage(authenticatedPage);

        await test.step('The inbox shows the waiting runs with started and expires times', async () => {
            await approvalTasksPage.goto();

            await expect(approvalTasksPage.pendingApprovalsHeading).toBeVisible();

            await expect(approvalTasksPage.pendingApprovalItem(pendingLabel)).toHaveCount(2);

            await expect(approvalTasksPage.pendingApprovalItem(pendingLabel).first()).toContainText('started');
            await expect(approvalTasksPage.pendingApprovalItem(pendingLabel).first()).toContainText('expires');

            for (const unlistedLabel of [resolvedLabel, cancelledLabel, failedLabel, waitLabel]) {
                await expect(approvalTasksPage.pendingApprovalItem(unlistedLabel)).toHaveCount(0);
            }
        });

        await test.step('Open form opens the hosted approval form', async () => {
            const popupPromise = context.waitForEvent('page');

            await approvalTasksPage
                .pendingApprovalItem(pendingLabel)
                .first()
                .getByRole('link', {name: 'Open form'})
                .click();

            const formPage = await popupPromise;

            await expect(new ApprovalFormPage(formPage).formTitleHeading(pendingLabel)).toBeVisible();

            await formPage.close();
        });

        for (const pendingRun of pendingRuns) {
            await resumeJob({body: {approved: true}, request, token: pendingRun.token});
        }
    });
});
