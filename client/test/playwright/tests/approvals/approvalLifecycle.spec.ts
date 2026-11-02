import {expect, mergeTests} from '@playwright/test';

import {loginTest, projectTest, workflowDeploymentTest} from '../../fixtures';
import {NotificationsPage} from '../../pages/notificationsPage';
import {deployAndStartApprovalRun, uniqueRecipient, uniqueSuffix} from '../../utils/approvalRunUtils';
import {
    extractApprovalToken,
    getApprovalForm,
    getApprovalTokenForJob,
    resumeJob,
    waitForApprovalTaskStatus,
    waitForPendingApproval,
} from '../../utils/approvalUtils';
import {buildApprovalWorkflow} from '../../utils/approvalWorkflowDefinitions';
import {waitAndStaySignedIn} from '../../utils/auth';
import {postMattermostAction} from '../../utils/channelCallbackUtils';
import {
    countTaskExecutions,
    getJob,
    getJobErrorMessage,
    restartJob,
    stopJob,
    waitForJobStatus,
} from '../../utils/jobUtils';
import {searchMailpitMessages, subjectQuery, waitForMailpitMessage} from '../../utils/mailpitUtils';
import {deleteNotificationByName, getLinkOnItsOwnLineAfter} from '../../utils/notificationUtils';
import {SLOW_TAG, skipUnlessMailpitIsReachable, skipUnlessSlowRunEnabled} from '../../utils/testEnvironment';

export const test = mergeTests(loginTest(), projectTest, workflowDeploymentTest);

const ONE_HOUR = 60 * 60 * 1000;

const SWEEP_PERIOD = 16 * 60 * 1000;

test.describe('Request Approval lifecycle', () => {
    test('A20 restart guards for waiting and failed runs', async ({authenticatedPage, deployer, request}) => {
        const approvalRun = await deployAndStartApprovalRun({
            definition: buildApprovalWorkflow({
                channels: ['approvalTask'],
                formTitle: `A20 approval ${uniqueSuffix()}`,
                label: `A20 restart ${uniqueSuffix()}`,
            }),
            deployer,
            page: authenticatedPage,
        });

        await test.step('Restarting a run that waits on an approval answers 409', async () => {
            const taskExecutionCount = await countTaskExecutions(authenticatedPage, approvalRun.jobId);

            const restartResponse = await restartJob(authenticatedPage, approvalRun.jobId);

            expect(restartResponse.status()).toBe(409);
            expect(await restartResponse.text()).toContain(
                `Job ${approvalRun.jobId} is waiting on an approval; resolve the approval or stop the job instead of restarting it`
            );

            expect(await countTaskExecutions(authenticatedPage, approvalRun.jobId)).toBe(taskExecutionCount);
        });

        await test.step('The approval link still works', async () => {
            expect((await resumeJob({body: {approved: true}, request, token: approvalRun.token})).status()).toBe(204);

            await waitForJobStatus(authenticatedPage, approvalRun.jobId, 'COMPLETED');
        });

        await test.step('A failed run restarts once; a second restart while it runs answers 400', async () => {
            const failingProject = await deployer.deploy(
                [
                    buildApprovalWorkflow({
                        channels: ['telegram', 'slack'],
                        formTitle: `A20 failing ${uniqueSuffix()}`,
                        label: `A20 failing ${uniqueSuffix()}`,
                    }),
                ],
                {projectId: await deployer.createProject()}
            );

            const failingJob = await deployer.triggerWebhookAndWaitForJob(failingProject);

            await waitForJobStatus(authenticatedPage, failingJob.id, 'FAILED');

            const firstRestartResponse = await restartJob(authenticatedPage, failingJob.id);

            const secondRestartResponse = await restartJob(authenticatedPage, failingJob.id);

            expect(firstRestartResponse.status()).toBe(204);
            expect(secondRestartResponse.status()).toBe(400);
            expect(await secondRestartResponse.text()).toContain(`can't resume job ${failingJob.id} as it is STARTED`);
        });
    });

    test('A21 stopping a run that waits on an approval', async ({authenticatedPage, deployer, request}) => {
        const formTitle = `A21 approval ${uniqueSuffix()}`;

        const approvalRun = await deployAndStartApprovalRun({
            definition: buildApprovalWorkflow({
                channels: ['mattermost', 'approvalTask'],
                formTitle,
                label: `A21 stop ${uniqueSuffix()}`,
            }),
            deployer,
            page: authenticatedPage,
        });

        await test.step('Stop cancels the run and expires its approval task', async () => {
            expect((await stopJob(authenticatedPage, approvalRun.jobId)).status()).toBe(204);

            await waitForJobStatus(authenticatedPage, approvalRun.jobId, 'CANCELLED');

            await waitForApprovalTaskStatus(authenticatedPage, formTitle, 'EXPIRED');

            expect((await stopJob(authenticatedPage, approvalRun.jobId)).status()).toBe(204);
        });

        await test.step('The link, the form and a channel button all report the approval as gone', async () => {
            expect((await resumeJob({body: {approved: true}, request, token: approvalRun.token})).status()).toBe(410);

            const formResponse = await getApprovalForm(request, approvalRun.token);

            expect(formResponse.status()).toBe(404);
            expect(await formResponse.text()).toContain('Approval form is no longer available.');

            const mattermostResponse = await postMattermostAction({
                request,
                token: approvalRun.token,
                userName: 'erin',
            });

            expect(mattermostResponse.status()).toBe(200);
            expect(await mattermostResponse.text()).toContain('This approval is no longer available.');
        });

        await test.step('A cancelled run cannot be restarted', async () => {
            const restartResponse = await restartJob(authenticatedPage, approvalRun.jobId);

            expect(restartResponse.status()).toBe(400);
            expect(await restartResponse.text()).toContain(`can't resume job ${approvalRun.jobId} as it is CANCELLED`);

            expect((await getJob(authenticatedPage, approvalRun.jobId)).status).toBe('CANCELLED');
        });
    });

    test(`A7 an expired approval fails the run on resume and can be restarted ${SLOW_TAG}`, async ({
        authenticatedPage,
        deployer,
        request,
    }) => {
        skipUnlessSlowRunEnabled();

        test.setTimeout(ONE_HOUR + SWEEP_PERIOD + 10 * 60 * 1000);

        const formTitle = `A7 approval ${uniqueSuffix()}`;

        const approvalRun = await deployAndStartApprovalRun({
            definition: buildApprovalWorkflow({
                channels: ['approvalTask'],
                expiresIn: 1,
                formTitle,
                label: `A7 expiry ${uniqueSuffix()}`,
            }),
            deployer,
            page: authenticatedPage,
        });

        await test.step('Wait until the approval expiry has passed', async () => {
            const pendingApproval = await waitForPendingApproval(authenticatedPage, approvalRun.jobId);

            const millisecondsUntilExpiry = Date.parse(pendingApproval.expiresAt as string) - Date.now();

            await waitAndStaySignedIn(authenticatedPage, millisecondsUntilExpiry + 5000);
        });

        await test.step('Resolving after expiry answers 410 and the run fails with the expiry reason', async () => {
            expect((await resumeJob({body: {approved: true}, request, token: approvalRun.token})).status()).toBe(410);

            const failedJob = await waitForJobStatus(authenticatedPage, approvalRun.jobId, 'FAILED');

            expect(getJobErrorMessage(failedJob)).toMatch(/The pending approval expired at .+ without being resolved/);

            expect(await countTaskExecutions(authenticatedPage, approvalRun.jobId, 'var_1')).toBe(0);

            await waitForApprovalTaskStatus(authenticatedPage, formTitle, 'EXPIRED');

            expect((await resumeJob({body: {approved: true}, request, token: approvalRun.token})).status()).toBe(410);
            expect((await getApprovalForm(request, approvalRun.token)).status()).toBe(404);
        });

        await test.step('A20: restarting the expiry-failed run suspends it on a new approval', async () => {
            expect((await restartJob(authenticatedPage, approvalRun.jobId)).status()).toBe(204);

            const restartedJob = await waitForJobStatus(authenticatedPage, approvalRun.jobId, 'STOPPED');

            expect(getJobErrorMessage(restartedJob)).toBe('');

            const newToken = await getApprovalTokenForJob(authenticatedPage, approvalRun.jobId);

            expect(newToken).not.toBe(approvalRun.token);

            expect((await resumeJob({body: {approved: true}, request, token: newToken})).status()).toBe(204);

            await waitForJobStatus(authenticatedPage, approvalRun.jobId, 'COMPLETED');
        });
    });

    test(`A8 an unattended approval expires through its timer or the sweep ${SLOW_TAG}`, async ({
        authenticatedPage,
        deployer,
        request,
    }) => {
        skipUnlessSlowRunEnabled();

        test.setTimeout(ONE_HOUR + SWEEP_PERIOD + 10 * 60 * 1000);

        const formTitle = `A8 approval ${uniqueSuffix()}`;

        const approvalRun = await deployAndStartApprovalRun({
            definition: buildApprovalWorkflow({
                channels: ['approvalTask'],
                expiresIn: 1,
                formTitle,
                label: `A8 expiry ${uniqueSuffix()}`,
            }),
            deployer,
            page: authenticatedPage,
        });

        await test.step('The run fails at expiry without anyone acting', async () => {
            const failedJob = await waitForJobStatus(
                authenticatedPage,
                approvalRun.jobId,
                'FAILED',
                ONE_HOUR + SWEEP_PERIOD
            );

            expect(getJobErrorMessage(failedJob)).toMatch(/The pending approval expired at .+ without being resolved/);

            await waitForApprovalTaskStatus(authenticatedPage, formTitle, 'EXPIRED');
        });

        await test.step('The link answers 410 and the form 404', async () => {
            expect((await resumeJob({body: {approved: true}, request, token: approvalRun.token})).status()).toBe(410);
            expect((await getApprovalForm(request, approvalRun.token)).status()).toBe(404);
        });
    });

    test(`A9 one reminder before expiry, also for runs pending before the subscriber existed ${SLOW_TAG}`, async ({
        authenticatedPage,
        deployer,
        request,
    }) => {
        skipUnlessSlowRunEnabled();

        await skipUnlessMailpitIsReachable(request);

        test.setTimeout(4 * SWEEP_PERIOD + 10 * 60 * 1000);

        const notificationName = `A9 reminder ${uniqueSuffix()}`;
        const reminderRecipient = uniqueRecipient('a9-reminder');
        const workflowLabel = `A9 reminder ${uniqueSuffix()}`;

        const approvalRun = await deployAndStartApprovalRun({
            definition: buildApprovalWorkflow({
                channels: ['approvalTask'],
                formTitle: workflowLabel,
                label: workflowLabel,
            }),
            deployer,
            page: authenticatedPage,
        });

        const reminderQuery = subjectQuery(`Approval pending - Job ${workflowLabel} (ID: ${approvalRun.jobId})`);

        try {
            await test.step('A reminder sweep with no subscriber passes first', async () => {
                await waitAndStaySignedIn(authenticatedPage, SWEEP_PERIOD);
            });

            await test.step('Subscribe an email recipient to "Job approval expiring"', async () => {
                const notificationsPage = new NotificationsPage(authenticatedPage);

                await notificationsPage.goto();

                await notificationsPage.createEmailNotification({
                    email: reminderRecipient,
                    eventType: 'JOB_APPROVAL_EXPIRING',
                    name: notificationName,
                });
            });

            await test.step('The next sweep reminds only the subscriber, once', async () => {
                const reminderMail = await waitForMailpitMessage(request, reminderQuery, {
                    timeout: SWEEP_PERIOD + 60000,
                });

                expect(reminderMail.To.map((recipient) => recipient.Address)).toEqual([reminderRecipient]);
                expect(reminderMail.Subject).toMatch(/approval expires at /);
                expect(reminderMail.Text).toContain('Resolve it before it lapses, or the run will be failed.');

                const resolveLink = getLinkOnItsOwnLineAfter(reminderMail.Text, 'Resolve it here:');

                expect(extractApprovalToken(resolveLink)).toBe(approvalRun.token);
            });

            await test.step('One more sweep sends no second reminder', async () => {
                await waitAndStaySignedIn(authenticatedPage, SWEEP_PERIOD);

                expect(await searchMailpitMessages(request, reminderQuery)).toHaveLength(1);
            });
        } finally {
            await waitAndStaySignedIn(authenticatedPage, 0);

            await deleteNotificationByName(authenticatedPage, notificationName);

            await resumeJob({body: {approved: true}, request, token: approvalRun.token});
        }
    });

    test(`A10 one escalation after the configured delay ${SLOW_TAG}`, async ({
        authenticatedPage,
        deployer,
        request,
    }) => {
        skipUnlessSlowRunEnabled();

        await skipUnlessMailpitIsReachable(request);

        test.setTimeout(3 * SWEEP_PERIOD + 10 * 60 * 1000);

        const notificationName = `A10 escalation ${uniqueSuffix()}`;
        const escalationRecipient = uniqueRecipient('a10-escalation');
        const workflowLabel = `A10 escalation ${uniqueSuffix()}`;

        const approvalRun = await deployAndStartApprovalRun({
            definition: buildApprovalWorkflow({
                channels: ['approvalTask'],
                formTitle: workflowLabel,
                label: workflowLabel,
            }),
            deployer,
            page: authenticatedPage,
        });

        const escalationQuery = subjectQuery(
            `Approval escalation - Job ${workflowLabel} (ID: ${approvalRun.jobId}) still awaiting approval`
        );

        try {
            await test.step('Subscribe an email recipient to "Job approval escalated"', async () => {
                const notificationsPage = new NotificationsPage(authenticatedPage);

                await notificationsPage.goto();

                await notificationsPage.createEmailNotification({
                    email: escalationRecipient,
                    eventType: 'JOB_APPROVAL_ESCALATED',
                    name: notificationName,
                });
            });

            let escalationLink = '';

            await test.step('The escalation sweep mails only the subscriber, once', async () => {
                const escalationMail = await waitForMailpitMessage(request, escalationQuery, {
                    timeout: SWEEP_PERIOD + 5 * 60 * 1000,
                });

                expect(escalationMail.To.map((recipient) => recipient.Address)).toEqual([escalationRecipient]);
                expect(escalationMail.Text).toMatch(/still unresolved \(expires at .+\)\./);

                escalationLink = getLinkOnItsOwnLineAfter(escalationMail.Text, 'Resolve it here:');

                expect(escalationLink).toContain('/resume/');

                await waitAndStaySignedIn(authenticatedPage, SWEEP_PERIOD);

                expect(await searchMailpitMessages(request, escalationQuery)).toHaveLength(1);
            });

            await test.step('Resolving through the escalation link clears the markers', async () => {
                const resumeResponse = await resumeJob({
                    body: {approved: true},
                    request,
                    token: extractApprovalToken(escalationLink),
                });

                expect(resumeResponse.status()).toBe(204);

                const completedJob = await waitForJobStatus(authenticatedPage, approvalRun.jobId, 'COMPLETED');

                expect(completedJob.metadata ?? {}).not.toHaveProperty('approvalEscalatedAt');
                expect(completedJob.metadata ?? {}).not.toHaveProperty('approvalReminderSentAt');
            });
        } finally {
            await waitAndStaySignedIn(authenticatedPage, 0);

            await deleteNotificationByName(authenticatedPage, notificationName);
        }
    });
});
