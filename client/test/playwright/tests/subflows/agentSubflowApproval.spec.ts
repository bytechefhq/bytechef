import {expect, mergeTests} from '@playwright/test';

import {loginTest, projectTest, workflowDeploymentTest} from '../../fixtures';
import {uniqueRecipient, uniqueSuffix} from '../../utils/approvalRunUtils';
import {
    findApprovalTasksByName,
    getApprovalTasks,
    getApprovalTokenForJob,
    getPendingApprovals,
    resumeJob,
    waitForApprovalTaskStatus,
} from '../../utils/approvalUtils';
import {
    SYNC_WEBHOOK_CSRF_TOKEN,
    buildAgentApprovalToolWorkflow,
    buildAgentCallWorkflowParent,
    buildCallableApprovalWorkflow,
    buildSubflowParent,
} from '../../utils/approvalWorkflowDefinitions';
import {publishProject} from '../../utils/deploymentUtils';
import {
    JOB_TIMEOUT,
    getJob,
    getJobErrorMessage,
    getProjectJobs,
    getTaskErrorMessage,
    getTaskOutput,
    waitForJobByLabelAfter,
    waitForJobStatus,
    waitForNewProjectJob,
} from '../../utils/jobUtils';
import {countMailpitMessages, recipientQuery, subjectQuery, waitForMailpitMessage} from '../../utils/mailpitUtils';
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

const AGENT_TIMEOUT = 3 * 60 * 1000;

test.describe('Sub-workflow approvals through AI agent tools', () => {
    test.beforeEach(async ({request}) => {
        await skipUnlessMailpitIsReachable(request);
    });

    test('D1a an agent tool calls a child in another project that asks for approval', async ({
        authenticatedPage,
        deployer,
        request,
    }) => {
        skipUnlessOpenAiKeyIsSet();

        test.setTimeout(AGENT_TIMEOUT);

        const childLabel = `D1a child ${uniqueSuffix()}`;
        const childRecipient = uniqueRecipient('d1a-child');

        const childProjectId = await deployer.createProject();

        const childProject = await deployer.deploy(
            [
                buildCallableApprovalWorkflow({
                    channels: ['approvalTask', 'email'],
                    label: childLabel,
                    recipient: childRecipient,
                }),
            ],
            {projectId: childProjectId}
        );

        const parentProject = await deployer.deploy([
            buildAgentCallWorkflowParent({
                childWorkflowUuid: childProject.workflows[0].workflowUuid,
                label: `D1a parent ${uniqueSuffix()}`,
            }),
        ]);

        const parentJob = await deployer.triggerWebhookAndWaitForJob(parentProject, {message: 'Order one blue lamp'});

        let childJobId = '';

        await test.step('The parent waits while the child is suspended in its own project', async () => {
            childJobId = await waitForNewProjectJob({
                knownJobIds: [],
                page: authenticatedPage,
                projectId: childProjectId,
                timeout: 2 * JOB_TIMEOUT,
                workspaceId: deployer.workspaceId,
            });

            await waitForJobStatus(authenticatedPage, childJobId, 'STOPPED');
            await waitForJobStatus(authenticatedPage, parentJob.id, 'STOPPED');

            const childMail = await waitForMailpitMessage(request, recipientQuery(childRecipient));

            expect(childMail.Subject.toLowerCase()).toContain('blue lamp');

            const pendingJobIds = (await getPendingApprovals(authenticatedPage)).map(
                (pendingApproval) => pendingApproval.jobId
            );

            expect(pendingJobIds).toContain(childJobId);
            expect(pendingJobIds).not.toContain(parentJob.id);
        });

        await test.step('Approving the child resumes the parent', async () => {
            const childToken = await getApprovalTokenForJob(authenticatedPage, childJobId);

            expect(
                (await resumeJob({body: {approved: true, comment: 'ok'}, request, token: childToken})).status()
            ).toBe(204);

            await waitForJobStatus(authenticatedPage, childJobId, 'COMPLETED');

            expect(await getTaskOutput(authenticatedPage, childJobId, 'approval_1')).toMatchObject({
                approved: true,
                comment: 'ok',
            });

            expect(
                JSON.stringify(
                    await getTaskOutput(authenticatedPage, childJobId, 'responseToWorkflowCall_1')
                ).toLowerCase()
            ).toContain('blue lamp');

            await waitForJobStatus(authenticatedPage, parentJob.id, 'COMPLETED', 2 * JOB_TIMEOUT);

            const agentAnswer = String(await getTaskOutput(authenticatedPage, parentJob.id, 'aiAgent_1'));

            expect(agentAnswer.toLowerCase()).toContain('approved');
            expect(agentAnswer).toContain('ok');

            const childTasks = (await getApprovalTasks(authenticatedPage)).filter((approvalTask) =>
                approvalTask.name.startsWith(childLabel)
            );

            expect(childTasks).toHaveLength(1);

            await waitForApprovalTaskStatus(authenticatedPage, childTasks[0].name, 'COMPLETED');
        });
    });

    test('D1b the child sits in the same project and is discarded', async ({
        authenticatedPage,
        deployer,
        project,
        request,
    }) => {
        skipUnlessOpenAiKeyIsSet();

        test.setTimeout(AGENT_TIMEOUT);

        const childLabel = `D1b child ${uniqueSuffix()}`;
        const parentLabel = `D1b parent ${uniqueSuffix()}`;

        await deployer.importWorkflows(project.id, [
            buildCallableApprovalWorkflow({channels: ['approvalTask'], label: childLabel}),
        ]);

        const childWorkflowUuid = await deployer.getWorkflowUuid(project.id, childLabel);

        const deployedProject = await deployer.deploy([
            buildAgentCallWorkflowParent({childWorkflowUuid, label: parentLabel}),
        ]);

        const parentJob = await deployer.triggerWebhookAndWaitForJob(
            deployedProject,
            {message: 'Order one green mug'},
            parentLabel
        );

        const childJobId = await waitForNewProjectJob({
            knownJobIds: [parentJob.id],
            page: authenticatedPage,
            projectId: project.id,
            timeout: 2 * JOB_TIMEOUT,
            workspaceId: deployer.workspaceId,
        });

        await test.step('Both runs wait until the decision', async () => {
            await waitForJobStatus(authenticatedPage, childJobId, 'STOPPED');
            await waitForJobStatus(authenticatedPage, parentJob.id, 'STOPPED');
        });

        await test.step('Discarding the child resumes the parent with the reason', async () => {
            const childToken = await getApprovalTokenForJob(authenticatedPage, childJobId);

            expect(
                (await resumeJob({body: {approved: false, comment: 'no mugs'}, request, token: childToken})).status()
            ).toBe(204);

            await waitForJobStatus(authenticatedPage, childJobId, 'COMPLETED');

            expect(await getTaskOutput(authenticatedPage, childJobId, 'approval_1')).toMatchObject({
                approved: false,
                comment: 'no mugs',
            });

            await waitForJobStatus(authenticatedPage, parentJob.id, 'COMPLETED', 2 * JOB_TIMEOUT);

            const agentAnswer = String(await getTaskOutput(authenticatedPage, parentJob.id, 'aiAgent_1')).toLowerCase();

            expect(agentAnswer).toMatch(/not approved|rejected|declined|denied|discarded|no mugs/);
        });
    });

    test('D1c a child without an enabled deployment is reported as a tool error', async ({
        authenticatedPage,
        deployer,
    }) => {
        skipUnlessOpenAiKeyIsSet();

        test.setTimeout(AGENT_TIMEOUT);

        const childLabel = `D1c child ${uniqueSuffix()}`;
        const childProjectId = await deployer.createProject();

        await deployer.importWorkflows(childProjectId, [
            buildCallableApprovalWorkflow({channels: ['approvalTask'], label: childLabel}),
        ]);

        await publishProject(authenticatedPage, childProjectId);

        const childWorkflowUuid = await deployer.getWorkflowUuid(childProjectId, childLabel);

        const parentProject = await deployer.deploy([
            buildAgentCallWorkflowParent({childWorkflowUuid, label: `D1c parent ${uniqueSuffix()}`}),
        ]);

        const parentJob = await deployer.triggerWebhookAndWaitForJob(parentProject, {message: 'Order one red pen'});

        await waitForJobStatus(authenticatedPage, parentJob.id, 'COMPLETED', 2 * JOB_TIMEOUT);

        const agentAnswer = String(await getTaskOutput(authenticatedPage, parentJob.id, 'aiAgent_1')).toLowerCase();

        expect(agentAnswer).toContain('deployment');

        expect(await getProjectJobs(authenticatedPage, deployer.workspaceId, childProjectId)).toHaveLength(0);
    });

    test('D1d a plain Subflow task waits for the child approval', async ({authenticatedPage, deployer, request}) => {
        const childLabel = `D1d child ${uniqueSuffix()}`;
        const childProjectId = await deployer.createProject();

        const childProject = await deployer.deploy(
            [buildCallableApprovalWorkflow({channels: ['approvalTask'], label: childLabel})],
            {projectId: childProjectId}
        );

        const parentProject = await deployer.deploy([
            buildSubflowParent({
                childWorkflowUuid: childProject.workflows[0].workflowUuid,
                item: 'plain subflow widget',
                label: `D1d parent ${uniqueSuffix()}`,
            }),
        ]);

        const parentJob = await deployer.triggerWebhookAndWaitForJob(parentProject);

        const childJobId = (
            await waitForJobByLabelAfter({afterJobId: parentJob.id, label: childLabel, page: authenticatedPage})
        ).id;

        await test.step('The child is suspended under its own project while the parent waits', async () => {
            await waitForJobStatus(authenticatedPage, childJobId, 'STOPPED');

            expect(['STARTED', 'STOPPED']).toContain((await getJob(authenticatedPage, parentJob.id)).status);
        });

        await test.step('Discarding the child completes both runs', async () => {
            const childToken = await getApprovalTokenForJob(authenticatedPage, childJobId);

            expect(
                (await resumeJob({body: {approved: false, comment: 'no widgets'}, request, token: childToken})).status()
            ).toBe(204);

            await waitForJobStatus(authenticatedPage, childJobId, 'COMPLETED');
            await waitForJobStatus(authenticatedPage, parentJob.id, 'COMPLETED');

            const childSaid = String(await getTaskOutput(authenticatedPage, parentJob.id, 'var_1'));

            expect(childSaid).toContain('approved=false');
            expect(childSaid).toContain('comment=no widgets');
            expect(childSaid).toContain('item=plain subflow widget');
        });
    });

    test('D2 a synchronous caller cannot pause its child', async ({authenticatedPage, deployer, request}) => {
        skipUnlessOpenAiKeyIsSet();

        test.setTimeout(AGENT_TIMEOUT);

        const childLabel = `D2 child ${uniqueSuffix()}`;
        const childRecipient = uniqueRecipient('d2-child');
        const childProjectId = await deployer.createProject();

        const childProject = await deployer.deploy(
            [
                buildCallableApprovalWorkflow({
                    channels: ['approvalTask', 'email'],
                    label: childLabel,
                    recipient: childRecipient,
                }),
            ],
            {projectId: childProjectId}
        );

        const parentProject = await deployer.deploy([
            buildAgentCallWorkflowParent({
                childWorkflowUuid: childProject.workflows[0].workflowUuid,
                label: `D2 parent ${uniqueSuffix()}`,
                synchronous: true,
            }),
        ]);

        await test.step('The synchronous webhook answers 200 with the agent explaining the refusal', async () => {
            const webhookResponse = await request.post(parentProject.workflows[0].webhookUrl, {
                data: {message: 'Order one yellow eraser'},
                headers: {'X-Csrf-Token': SYNC_WEBHOOK_CSRF_TOKEN},
                timeout: 2 * JOB_TIMEOUT,
            });

            expect(webhookResponse.status()).toBe(200);

            const webhookBody = (await webhookResponse.json()) as {answer?: string};

            expect(webhookBody.answer?.toLowerCase()).toContain('approval');
        });

        await test.step('The parent completes, the child fails before delivering anything', async () => {
            const [parentJob] = await getProjectJobs(authenticatedPage, deployer.workspaceId, parentProject.projectId);

            await waitForJobStatus(authenticatedPage, parentJob.id, 'COMPLETED');

            const childJob = await waitForJobByLabelAfter({
                afterJobId: parentJob.id,
                label: childLabel,
                page: authenticatedPage,
            });

            const failedChildJob = await waitForJobStatus(authenticatedPage, childJob.id, 'FAILED');

            const failureMessage = `${getJobErrorMessage(failedChildJob)} ${await getTaskErrorMessage(
                authenticatedPage,
                childJob.id,
                'approval_1'
            ).catch(() => '')}`;

            expect(failureMessage).toContain(
                "A sub-workflow called synchronously (in the editor, or from a workflow that returns a synchronous response) can't pause. Step 'approval_1' (approval/v1/requestApproval) would pause it"
            );

            expect(await countMailpitMessages(request, recipientQuery(childRecipient))).toBe(0);

            const childTasks = (await getApprovalTasks(authenticatedPage)).filter((approvalTask) =>
                approvalTask.name.startsWith(childLabel)
            );

            expect(childTasks).toHaveLength(0);
        });
    });

    test('AGENT-TOOL an agent asks for approval through the Request Approval tool', async ({
        authenticatedPage,
        deployer,
        request,
    }) => {
        skipUnlessOpenAiKeyIsSet();

        test.setTimeout(AGENT_TIMEOUT);

        const formTitle = `Agent asks to send a notification ${uniqueSuffix()}`;
        const recipient = uniqueRecipient('agent-tool');

        const deployedProject = await deployer.deploy([
            buildAgentApprovalToolWorkflow({formTitle, label: `AGENT-TOOL ${uniqueSuffix()}`, recipient}),
        ]);

        const webhookUrl = deployedProject.workflows[0].webhookUrl;

        await test.step('Approving the card lets the agent send the mail', async () => {
            const approveSubject = `AGENTTOOL approve test ${uniqueSuffix()}`;

            const chatStream = await sendChatMessage({
                conversationId: `agent-tool-${uniqueSuffix()}`,
                message: `Send a notification with subject ${approveSubject}`,
                request,
                webhookUrl,
            });

            expect(getEventNames(chatStream.events)).toEqual(['start', 'approval_request', 'suspended']);

            const [approvalRequest] = getApprovalRequests(chatStream.events);

            expect(approvalRequest.formTitle).toBe(formTitle);
            expect(approvalRequest.expiresAt).toBeTruthy();

            const jobId = getStartedJobId(chatStream.events);

            await waitForJobStatus(authenticatedPage, jobId, 'STOPPED');

            expect(await countMailpitMessages(request, subjectQuery(approveSubject))).toBe(0);

            const resumeStream = await resumeJobStreaming({
                body: {approved: true, comment: 'go ahead'},
                request,
                token: approvalRequest.resumeId,
            });

            expect(resumeStream.response.status()).toBe(200);
            expect(resumeStream.response.headers()['content-type']).toContain('text/event-stream');

            const sentMail = await waitForMailpitMessage(request, subjectQuery(approveSubject));

            const resultMessage = getResultMessage(resumeStream.events);

            expect(resultMessage).toContain(approveSubject);
            expect(resultMessage).toContain(sentMail.ID);

            await waitForJobStatus(authenticatedPage, jobId, 'COMPLETED');
        });

        await test.step('Rejecting the card stops the agent from sending', async () => {
            const rejectSubject = `AGENTTOOL reject test ${uniqueSuffix()}`;

            const chatStream = await sendChatMessage({
                conversationId: `agent-tool-${uniqueSuffix()}`,
                message: `Send a notification with subject ${rejectSubject}`,
                request,
                webhookUrl,
            });

            const [approvalRequest] = getApprovalRequests(chatStream.events);

            const resumeStream = await resumeJobStreaming({
                body: {approved: false, comment: 'not today'},
                request,
                token: approvalRequest.resumeId,
            });

            expect(getResultMessage(resumeStream.events)).toContain('not today');

            await waitForJobStatus(authenticatedPage, getStartedJobId(chatStream.events), 'COMPLETED');

            expect(await countMailpitMessages(request, subjectQuery(rejectSubject))).toBe(0);
            expect(await findApprovalTasksByName(authenticatedPage, formTitle)).toHaveLength(0);
        });
    });
});
