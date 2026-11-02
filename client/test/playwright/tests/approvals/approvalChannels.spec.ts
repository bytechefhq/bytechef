import {expect, mergeTests} from '@playwright/test';

import {loginTest, projectTest, workflowDeploymentTest} from '../../fixtures';
import {ChatsPage} from '../../pages/chatsPage';
import {WorkflowIssuesPanel} from '../../pages/workflowIssuesPanel';
import {deployAndStartApprovalRun, uniqueRecipient, uniqueSuffix} from '../../utils/approvalRunUtils';
import {findApprovalTasksByName} from '../../utils/approvalUtils';
import {
    SYNC_WEBHOOK_CSRF_TOKEN,
    buildApprovalWorkflow,
    buildChatApprovalWorkflow,
    buildStreamingChatApprovalWorkflow,
    buildSyncApprovalWorkflow,
} from '../../utils/approvalWorkflowDefinitions';
import {getProjectWorkflows} from '../../utils/deploymentUtils';
import {getJob, getProjectJobs, getTaskErrorMessage, getTaskOutput, waitForJobStatus} from '../../utils/jobUtils';
import {recipientQuery, waitForMailpitMessage} from '../../utils/mailpitUtils';
import {
    getApprovalRequests,
    getEventNames,
    getResultMessage,
    getStartedJobId,
    resumeJobStreaming,
    sendChatMessage,
} from '../../utils/sseUtils';
import {skipUnlessMailpitIsReachable, skipUnlessOpenAiKeyIsSet} from '../../utils/testEnvironment';

export const test = mergeTests(loginTest(), projectTest, workflowDeploymentTest);

const SYNC_PAUSE_ERROR =
    "Workflows that return a synchronous response can't contain steps that pause the run, such as approval or wait steps. Step 'approval_1' (approval/v1/requestApproval) would pause it";

async function getFailureMessage(page: Parameters<typeof getJob>[0], jobId: string, taskName: string): Promise<string> {
    const job = await getJob(page, jobId);

    return `${job.error?.message ?? ''} ${await getTaskErrorMessage(page, jobId, taskName)}`;
}

test.describe('Approval channel delivery', () => {
    test('A15 a run suspends when one of its channels still delivers', async ({
        authenticatedPage,
        deployer,
        request,
    }) => {
        await skipUnlessMailpitIsReachable(request);

        const recipient = uniqueRecipient('a15');

        const approvalRun = await deployAndStartApprovalRun({
            definition: buildApprovalWorkflow({
                channels: ['email', 'slack'],
                formTitle: `A15 approval ${uniqueSuffix()}`,
                label: `A15 partial ${uniqueSuffix()}`,
                recipient,
            }),
            deployer,
            page: authenticatedPage,
        });

        await test.step('The run is suspended and the mail is delivered despite Slack failing', async () => {
            expect((await getJob(authenticatedPage, approvalRun.jobId)).status).toBe('STOPPED');

            await waitForMailpitMessage(request, recipientQuery(recipient));
        });
    });

    test('A16 the step fails when no channel delivers, naming each cause', async ({authenticatedPage, deployer}) => {
        const deployedProject = await deployer.deploy([
            buildApprovalWorkflow({
                channels: ['telegram', 'slack'],
                formTitle: `A16 approval ${uniqueSuffix()}`,
                label: `A16 total ${uniqueSuffix()}`,
            }),
        ]);

        const startedJob = await deployer.triggerWebhookAndWaitForJob(deployedProject, {n: 'a16'});

        await test.step('The run fails at approval_1 with the aggregate error', async () => {
            await waitForJobStatus(authenticatedPage, startedJob.id, 'FAILED');

            const failureMessage = await getFailureMessage(authenticatedPage, startedJob.id, 'approval_1');

            expect(failureMessage).toContain(
                'None of the 2 configured approval channels could deliver the approval request.'
            );
            expect(failureMessage).toContain('telegram/telegram: 401');
            expect(failureMessage).toContain('Unauthorized');
            expect(failureMessage).toContain('slack/slack: not_authed');
            expect(failureMessage).not.toContain('Acquire function is not defined');
        });
    });

    test('A17 chat channel on a chat run, from the Chats page', async ({authenticatedPage, deployer}) => {
        skipUnlessOpenAiKeyIsSet();

        const deployedProject = await deployer.deploy([
            buildStreamingChatApprovalWorkflow(`A17 chat ${uniqueSuffix()}`),
        ]);

        const chatsPage = new ChatsPage(authenticatedPage);
        const message = `Please approve my request ${uniqueSuffix()}`;

        await test.step('Sending a message renders an approval card', async () => {
            await chatsPage.goto(deployedProject.workflows[0].webhookUrl);

            await chatsPage.sendMessage(message);

            await expect(chatsPage.approveButton).toBeVisible({timeout: 60000});

            await expect(chatsPage.approvalCard).toContainText(`Approval for ${message}`);
            await expect(chatsPage.approvalCard).toContainText('Expires');
            await expect(chatsPage.approvalCommentInput).toBeVisible();
            await expect(chatsPage.discardButton).toBeVisible();
        });

        await test.step('Approving with a comment continues the run', async () => {
            await chatsPage.approvalCommentInput.fill('ok from the card');

            await chatsPage.approveButton.click();

            await expect(chatsPage.approvedStatus).toBeVisible();

            const [chatJob] = await getProjectJobs(authenticatedPage, deployer.workspaceId, deployedProject.projectId);

            await waitForJobStatus(authenticatedPage, chatJob.id, 'COMPLETED');

            expect(await getTaskOutput(authenticatedPage, chatJob.id, 'approval_1')).toMatchObject({
                approved: true,
                comment: 'ok from the card',
            });
        });
    });

    test('A17 a non-streaming chat run that would pause explains the fix on the Chats page', async ({
        authenticatedPage,
        deployer,
    }) => {
        const deployedProject = await deployer.deploy([buildChatApprovalWorkflow(`A17 chat sync ${uniqueSuffix()}`)]);

        const chatsPage = new ChatsPage(authenticatedPage);
        const message = `Please approve my request ${uniqueSuffix()}`;

        await chatsPage.goto(deployedProject.workflows[0].webhookUrl);

        await chatsPage.sendMessage(message);

        await expect(
            chatsPage.text(
                "This chat can't pause the run for an approval or wait step. A chat can only wait on one when it streams its reply, so add a step that streams, such as an AI Agent, after it."
            )
        ).toBeVisible({timeout: 30000});

        await expect(chatsPage.approvalCard).toHaveCount(0);
    });

    test('A17 chat channel on a chat run, over the SSE API', async ({authenticatedPage, deployer, request}) => {
        const deployedProject = await deployer.deploy([buildChatApprovalWorkflow(`A17 sse ${uniqueSuffix()}`)]);

        const message = `REQ-${uniqueSuffix()}`;

        const chatStream = await test.step('The stream carries start, approval_request and suspended', async () => {
            const sentChatStream = await sendChatMessage({
                conversationId: `a17-${uniqueSuffix()}`,
                message,
                request,
                webhookUrl: deployedProject.workflows[0].webhookUrl,
            });

            expect(getEventNames(sentChatStream.events)).toEqual(['start', 'approval_request', 'suspended']);

            const [approvalRequest] = getApprovalRequests(sentChatStream.events);

            expect(approvalRequest.formTitle).toBe(`Approval for ${message}`);
            expect(approvalRequest.formDescription).toBe(message);
            expect(approvalRequest.resumeId).toMatch(/^v1\./);
            expect(approvalRequest.formUrl).toContain('/resume/v1.');
            expect(approvalRequest.resumeUrl).toContain('/job/resume/v1.');
            expect(approvalRequest.expiresAt).toBeTruthy();

            return sentChatStream;
        });

        await test.step('A streaming resume answers with the reply and completes the run', async () => {
            const resumeStream = await resumeJobStreaming({
                body: {approved: true, comment: 'ok'},
                request,
                token: getApprovalRequests(chatStream.events)[0].resumeId,
            });

            expect(resumeStream.response.status()).toBe(200);
            expect(resumeStream.response.headers()['content-type']).toContain('text/event-stream');
            expect(getResultMessage(resumeStream.events)).toBe(`Decision for ${message}: approved=true comment=ok`);

            await waitForJobStatus(authenticatedPage, getStartedJobId(chatStream.events), 'COMPLETED');
        });
    });

    test('A18 chat channel on a non-chat run', async ({authenticatedPage, deployer}) => {
        const workflowLabel = `A18 chat on webhook ${uniqueSuffix()}`;

        const deployedProject = await deployer.deploy([
            buildApprovalWorkflow({
                channels: ['chat'],
                formDescription: 'Please approve',
                formTitle: workflowLabel,
                label: workflowLabel,
            }),
        ]);

        await test.step('The editor warns about the chat-only channel', async () => {
            const [projectWorkflow] = await getProjectWorkflows(authenticatedPage, deployedProject.projectId);

            const workflowIssuesPanel = new WorkflowIssuesPanel(authenticatedPage);

            await workflowIssuesPanel.gotoWorkflowEditor(deployedProject.projectId, projectWorkflow.projectWorkflowId);

            await expect(workflowIssuesPanel.nodeIssueBadge()).toBeVisible();

            await workflowIssuesPanel.openSidebarFromNote('1 warning');

            await expect(workflowIssuesPanel.sidebarHeading).toHaveText('Workflow Issues (1 warning)');

            await expect(workflowIssuesPanel.nodeIssues('approval_1')).toContainText(
                'This task delivers approval requests only to the chat channel, but the workflow does not start from a chat trigger'
            );
        });

        await test.step('A webhook run fails at the approval step', async () => {
            const startedJob = await deployer.triggerWebhookAndWaitForJob(deployedProject);

            await waitForJobStatus(authenticatedPage, startedJob.id, 'FAILED');

            const failureMessage = await getFailureMessage(authenticatedPage, startedJob.id, 'approval_1');

            expect(failureMessage).toContain(
                'None of the 1 configured approval channels could deliver the approval request. chat/chat: The chat approval channel requires a run with a chat origin'
            );
            expect(failureMessage).toContain('Configure a fallback approval channel');
        });
    });

    test('A19 a synchronous webhook rejects a workflow that would pause', async ({
        authenticatedPage,
        deployer,
        request,
    }) => {
        const workflowLabel = `A19 sync ${uniqueSuffix()}`;

        const deployedProject = await deployer.deploy([buildSyncApprovalWorkflow(workflowLabel)]);

        const webhookUrl = deployedProject.workflows[0].webhookUrl;

        await test.step('The webhook answers 400 with and without the CSRF header', async () => {
            const csrfResponse = await request.post(webhookUrl, {
                data: {n: 'a19'},
                headers: {'X-Csrf-Token': SYNC_WEBHOOK_CSRF_TOKEN},
            });

            expect(csrfResponse.status()).toBe(400);
            expect(await csrfResponse.text()).toContain(SYNC_PAUSE_ERROR);

            const plainResponse = await request.post(webhookUrl, {data: {n: 'a19'}});

            expect(plainResponse.status()).toBe(400);
        });

        await test.step('No run and no approval task were created', async () => {
            expect(
                await getProjectJobs(authenticatedPage, deployer.workspaceId, deployedProject.projectId)
            ).toHaveLength(0);

            expect(await findApprovalTasksByName(authenticatedPage, workflowLabel)).toHaveLength(0);
        });
    });
});
