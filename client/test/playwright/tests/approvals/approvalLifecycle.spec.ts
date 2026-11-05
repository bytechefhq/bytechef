import {expect, mergeTests} from '@playwright/test';

import {loginTest, projectTest, workflowDeploymentTest} from '../../fixtures';
import {deployAndStartApprovalRun, uniqueSuffix} from '../../utils/approvalRunUtils';
import {
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
import {SLOW_TAG, skipUnlessSlowRunEnabled} from '../../utils/testEnvironment';

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
});
