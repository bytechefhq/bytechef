import {type Page, expect, mergeTests} from '@playwright/test';

import {loginTest, projectTest, workflowDeploymentTest} from '../../fixtures';
import {type WorkflowDeployerI} from '../../fixtures/workflowDeployment';
import {ApprovalFormPage} from '../../pages/approvalFormPage';
import {ApprovalTasksPage} from '../../pages/approvalTasksPage';
import {WorkflowIssuesPanel} from '../../pages/workflowIssuesPanel';
import {
    buildApprovalGate,
    buildApprovalTaskChannel,
    buildChatChannel,
    buildEmailApprovalChannel,
    buildGatedAgentWorkflow,
    buildInvalidPlacementTools,
    buildMailServerInfoTool,
    buildSendNotificationTool,
} from '../../utils/approvalGateWorkflowDefinitions';
import {uniqueRecipient, uniqueSuffix} from '../../utils/approvalRunUtils';
import {
    extractApprovalToken,
    findApprovalTasksByName,
    getApprovalTokenForJob,
    newAnonymousPage,
    resumeJob,
    waitForApprovalTaskStatus,
    waitForPendingApproval,
} from '../../utils/approvalUtils';
import {SYNC_WEBHOOK_CSRF_TOKEN} from '../../utils/approvalWorkflowDefinitions';
import {waitAndStaySignedIn} from '../../utils/auth';
import {type DeployedProjectI, getProjectWorkflows} from '../../utils/deploymentUtils';
import {
    JOB_TIMEOUT,
    getJob,
    getJobErrorMessage,
    getProjectJobs,
    getTaskErrorMessage,
    waitForJobStatus,
} from '../../utils/jobUtils';
import {
    countMailpitMessages,
    extractLinks,
    recipientQuery,
    subjectQuery,
    waitForMailpitMessage,
} from '../../utils/mailpitUtils';
import {
    getApprovalRequests,
    getEventNames,
    getResultMessage,
    getStartedJobId,
    resumeJobStreaming,
    sendChatMessage,
} from '../../utils/sseUtils';
import {
    SLOW_TAG,
    skipUnlessMailpitIsReachable,
    skipUnlessOpenAiKeyIsSet,
    skipUnlessSlowRunEnabled,
} from '../../utils/testEnvironment';
import {validateWorkflowById} from '../../utils/workflowValidationUtils';

export const test = mergeTests(loginTest(), projectTest, workflowDeploymentTest);

const AGENT_TIMEOUT = 4 * 60 * 1000;

const DAY_MILLISECONDS = 24 * 60 * 60 * 1000;

const ONE_HOUR = 60 * 60 * 1000;

function expectExpiryAfter(expiresAt: string | undefined, expectedMilliseconds: number) {
    const millisecondsFromNow = Date.parse(expiresAt as string) - Date.now();

    expect(millisecondsFromNow).toBeGreaterThan(expectedMilliseconds - 10 * 60 * 1000);
    expect(millisecondsFromNow).toBeLessThan(expectedMilliseconds + 60 * 1000);
}

async function getFailureMessage(page: Page, jobId: string): Promise<string> {
    const job = await getJob(page, jobId);

    const taskErrorMessage = await getTaskErrorMessage(page, jobId, 'aiAgent_1').catch(() => '');

    return `${getJobErrorMessage(job)} ${taskErrorMessage}`;
}

async function deployChatGate(deployer: WorkflowDeployerI, recipient: string) {
    return deployer.deploy([
        buildGatedAgentWorkflow({
            label: `C1 gated chat ${uniqueSuffix()}`,
            tools: [
                buildApprovalGate({
                    approvalChannels: [buildChatChannel()],
                    label: 'Mail gate',
                    name: 'Mail gate',
                    tools: [buildSendNotificationTool(recipient)],
                }),
                buildMailServerInfoTool(),
            ],
            trigger: 'chat',
        }),
    ]);
}

async function startGatedWebhookRun(
    deployer: WorkflowDeployerI,
    page: Page,
    deployedProject: DeployedProjectI,
    message: string
) {
    const startedJob = await deployer.triggerWebhookAndWaitForJob(deployedProject, {message});

    const job = await waitForJobStatus(page, startedJob.id, 'STOPPED', 2 * JOB_TIMEOUT);

    return {jobId: job.id, token: await getApprovalTokenForJob(page, job.id)};
}

test.describe('AI agent approval gate', () => {
    test.beforeEach(async ({request}, testInfo) => {
        if (!testInfo.title.startsWith('C12')) {
            skipUnlessOpenAiKeyIsSet();
        }

        await skipUnlessMailpitIsReachable(request);

        test.setTimeout(AGENT_TIMEOUT);
    });

    test('C1 a gated tool runs only after approval', async ({authenticatedPage, deployer, request}) => {
        const deployedProject = await deployChatGate(deployer, uniqueRecipient('c1-tool'));
        const subject = `C1 approve test ${uniqueSuffix()}`;

        const chatStream = await test.step('The tool call raises an approval card and suspends', async () => {
            const sentChatStream = await sendChatMessage({
                conversationId: `c1-${uniqueSuffix()}`,
                message: `Send a notification with subject ${subject}`,
                request,
                webhookUrl: deployedProject.workflows[0].webhookUrl,
            });

            expect(getEventNames(sentChatStream.events)).toEqual(['start', 'approval_request', 'suspended']);

            const [approvalRequest] = getApprovalRequests(sentChatStream.events);

            expect(approvalRequest.formTitle).toBe('Mail gate: approve tool call send_notification_email');
            expect(JSON.stringify(approvalRequest)).toContain(subject);

            expectExpiryAfter(approvalRequest.expiresAt, 60 * DAY_MILLISECONDS);

            await waitForJobStatus(authenticatedPage, getStartedJobId(sentChatStream.events), 'STOPPED');

            return sentChatStream;
        });

        await test.step('No mail is sent before the decision', async () => {
            expect(await countMailpitMessages(request, subjectQuery(subject))).toBe(0);
        });

        await test.step('Approving runs the tool with the original arguments', async () => {
            const resumeStream = await resumeJobStreaming({
                body: {approved: true},
                request,
                token: getApprovalRequests(chatStream.events)[0].resumeId,
            });

            expect(resumeStream.response.status()).toBe(200);
            expect(resumeStream.response.headers()['content-type']).toContain('text/event-stream');

            const sentMail = await waitForMailpitMessage(request, subjectQuery(subject));

            expect(sentMail.Subject).toBe(subject);
            expect(getResultMessage(resumeStream.events)).toContain(sentMail.ID);

            await waitForJobStatus(authenticatedPage, getStartedJobId(chatStream.events), 'COMPLETED');
        });
    });

    test('C2 a rejected tool call never runs', async ({authenticatedPage, deployer, request}) => {
        const deployedProject = await deployChatGate(deployer, uniqueRecipient('c2-tool'));
        const subject = `C2 reject test ${uniqueSuffix()}`;

        const chatStream = await sendChatMessage({
            conversationId: `c2-${uniqueSuffix()}`,
            message: `Send a notification with subject ${subject}`,
            request,
            webhookUrl: deployedProject.workflows[0].webhookUrl,
        });

        const resumeStream = await resumeJobStreaming({
            body: {approved: false, comment: 'Budget freeze - no mail'},
            request,
            token: getApprovalRequests(chatStream.events)[0].resumeId,
        });

        expect(resumeStream.response.status()).toBe(200);

        const resultMessage = getResultMessage(resumeStream.events).toLowerCase();

        expect(resultMessage).toMatch(/denied|rejected|declined|not approved/);
        expect(resultMessage).toContain('budget');

        await waitForJobStatus(authenticatedPage, getStartedJobId(chatStream.events), 'COMPLETED');

        expect(await countMailpitMessages(request, subjectQuery(subject))).toBe(0);
    });

    test('C3 an ungated tool next to a gate runs without approval', async ({authenticatedPage, deployer, request}) => {
        const deployedProject = await deployChatGate(deployer, uniqueRecipient('c3-tool'));

        const chatStream = await sendChatMessage({
            conversationId: `c3-${uniqueSuffix()}`,
            message: 'What version is the mail server running?',
            request,
            webhookUrl: deployedProject.workflows[0].webhookUrl,
        });

        expect(getEventNames(chatStream.events)).toEqual(['start', 'result']);
        expect(getResultMessage(chatStream.events)).toMatch(/v?\d+\.\d+/);

        await waitForJobStatus(authenticatedPage, getStartedJobId(chatStream.events), 'COMPLETED');
    });

    test('C4 two gated calls in one round are approved one after the other', async ({
        authenticatedPage,
        deployer,
        request,
    }) => {
        const deployedProject = await deployChatGate(deployer, uniqueRecipient('c4-tool'));
        const suffix = uniqueSuffix();
        const subjects = [`C4 first ${suffix}`, `C4 second ${suffix}`];

        const chatStream = await sendChatMessage({
            conversationId: `c4-${suffix}`,
            message: `Send two notifications now, in parallel: subject ${subjects[0]}, and subject ${subjects[1]}`,
            request,
            webhookUrl: deployedProject.workflows[0].webhookUrl,
        });

        const [firstApproval] = getApprovalRequests(chatStream.events);

        await test.step('Only one card is raised', async () => {
            expect(getApprovalRequests(chatStream.events)).toHaveLength(1);
        });

        const firstSubject = subjects.find((subject) => JSON.stringify(firstApproval).includes(subject)) as string;
        const secondSubject = subjects.find((subject) => subject !== firstSubject) as string;

        const firstResumeStream = await test.step('Approving the first re-issues the deferred call', async () => {
            const resumeStream = await resumeJobStreaming({
                body: {approved: true},
                request,
                token: firstApproval.resumeId,
            });

            await waitForMailpitMessage(request, subjectQuery(firstSubject));

            const [secondApproval] = getApprovalRequests(resumeStream.events);

            expect(JSON.stringify(secondApproval)).toContain(secondSubject);
            expect(getEventNames(resumeStream.events)).toContain('suspended');

            return resumeStream;
        });

        await test.step('Approving the second completes the run with each mail sent once', async () => {
            const secondResumeStream = await resumeJobStreaming({
                body: {approved: true},
                request,
                token: getApprovalRequests(firstResumeStream.events)[0].resumeId,
            });

            await waitForMailpitMessage(request, subjectQuery(secondSubject));

            expect(getResultMessage(secondResumeStream.events)).toBeTruthy();

            await waitForJobStatus(authenticatedPage, getStartedJobId(chatStream.events), 'COMPLETED');

            expect(await countMailpitMessages(request, subjectQuery(firstSubject))).toBe(1);
            expect(await countMailpitMessages(request, subjectQuery(secondSubject))).toBe(1);
        });

        await test.step('Replaying the first token answers 410', async () => {
            expect((await resumeJob({body: {approved: true}, request, token: firstApproval.resumeId})).status()).toBe(
                410
            );
        });
    });

    test('C5 a gate with email and approval task channels on a webhook run', async ({
        authenticatedPage,
        browser,
        deployer,
        request,
    }) => {
        const approverRecipient = uniqueRecipient('c5-approver');
        const gateName = `C5 mail gate ${uniqueSuffix()}`;
        const formTitle = `${gateName}: approve tool call send_notification_email`;

        const deployedProject = await deployer.deploy([
            buildGatedAgentWorkflow({
                label: `C5 gate ${uniqueSuffix()}`,
                tools: [
                    buildApprovalGate({
                        approvalChannels: [
                            buildEmailApprovalChannel(approverRecipient, 'C5 approval needed'),
                            buildApprovalTaskChannel(),
                        ],
                        label: 'Mail gate',
                        name: gateName,
                        tools: [buildSendNotificationTool(uniqueRecipient('c5-tool'))],
                    }),
                ],
                trigger: 'webhook',
            }),
        ]);

        const firstSubject = `C5 approve test ${uniqueSuffix()}`;

        const firstRun = await startGatedWebhookRun(
            deployer,
            authenticatedPage,
            deployedProject,
            `Send a notification with subject ${firstSubject}`
        );

        let approveLink = '';

        await test.step('The mail and the approval task describe the gated call', async () => {
            const approvalMail = await waitForMailpitMessage(request, recipientQuery(approverRecipient));

            expect(approvalMail.Text).toContain(formTitle);
            expect(approvalMail.Text).toContain(firstSubject);
            expect(approvalMail.Text).toContain('Expires:');

            approveLink = extractLinks(approvalMail.HTML).find((mailLink) => mailLink.endsWith('?approved=true')) ?? '';

            expect(approveLink).toContain('/resume/v1.');
            expect(extractLinks(approvalMail.HTML).some((mailLink) => mailLink.endsWith('?approved=false'))).toBe(true);

            const approvalTask = await waitForApprovalTaskStatus(authenticatedPage, formTitle, 'OPEN');
            const pendingApproval = await waitForPendingApproval(authenticatedPage, firstRun.jobId);

            expect(Date.parse(approvalTask.dueDate as string)).toBe(Date.parse(pendingApproval.expiresAt as string));
        });

        await test.step('A bare GET answers 405 and resolves nothing', async () => {
            expect((await request.get(`/job/resume/${firstRun.token}`)).status()).toBe(405);

            expect((await getJob(authenticatedPage, firstRun.jobId)).status).toBe('STOPPED');
        });

        await test.step('Confirming the mail link runs the tool and completes the task', async () => {
            const approvalFormPage = new ApprovalFormPage(await newAnonymousPage(browser));

            await approvalFormPage.goto(extractApprovalToken(approveLink), true);

            await approvalFormPage.confirmApprovalButton.click();

            await approvalFormPage.expectApproved();

            await waitForMailpitMessage(request, subjectQuery(firstSubject));

            await waitForJobStatus(authenticatedPage, firstRun.jobId, 'COMPLETED', 2 * JOB_TIMEOUT);

            await waitForApprovalTaskStatus(authenticatedPage, formTitle, 'COMPLETED');
        });

        await test.step('A second run is approved from the Approval Tasks page', async () => {
            const secondSubject = `C5 task test ${uniqueSuffix()}`;

            const secondRun = await startGatedWebhookRun(
                deployer,
                authenticatedPage,
                deployedProject,
                `Send a notification with subject ${secondSubject}`
            );

            const approvalTasksPage = new ApprovalTasksPage(authenticatedPage);

            await approvalTasksPage.goto();

            await approvalTasksPage.search(formTitle);

            await approvalTasksPage.taskCard(formTitle).click();

            await new ApprovalFormPage(authenticatedPage).approveButton.click();

            await waitForMailpitMessage(request, subjectQuery(secondSubject));

            await waitForJobStatus(authenticatedPage, secondRun.jobId, 'COMPLETED', 2 * JOB_TIMEOUT);

            await expect(async () => {
                const approvalTasks = await findApprovalTasksByName(authenticatedPage, formTitle);

                expect(approvalTasks.map((approvalTask) => approvalTask.status)).toEqual(['COMPLETED', 'COMPLETED']);
            }).toPass({timeout: JOB_TIMEOUT});
        });
    });

    test('C6 a gate without channels warns and fails on a non-chat run', async ({
        authenticatedPage,
        deployer,
        request,
    }) => {
        const toolRecipient = uniqueRecipient('c6-tool');
        const gateName = `C6 gate ${uniqueSuffix()}`;

        const deployedProject = await deployer.deploy([
            buildGatedAgentWorkflow({
                label: `C6 gate ${uniqueSuffix()}`,
                tools: [
                    buildApprovalGate({
                        label: 'Mail gate',
                        name: gateName,
                        tools: [buildSendNotificationTool(toolRecipient)],
                    }),
                ],
                trigger: 'webhook',
            }),
        ]);

        const gateWarning =
            "Approval gate 'approvalGate_1' in task 'aiAgent_1' delivers approval requests only to the chat channel (the default when no approval channels are configured), but the workflow does not start from a chat trigger";

        await test.step('Validation and the editor warn about the chat-only default', async () => {
            const [projectWorkflow] = await getProjectWorkflows(authenticatedPage, deployedProject.projectId);

            const validationResult = await validateWorkflowById(authenticatedPage, projectWorkflow.id);

            expect(validationResult.nodeIssues.map((nodeIssue) => nodeIssue.message).join('\n')).toContain(gateWarning);

            const workflowIssuesPanel = new WorkflowIssuesPanel(authenticatedPage);

            await workflowIssuesPanel.gotoWorkflowEditor(deployedProject.projectId, projectWorkflow.projectWorkflowId);

            await workflowIssuesPanel.openSidebarFromNote();

            await expect(workflowIssuesPanel.sidebar).toContainText(gateWarning);
        });

        await test.step('A webhook run fails at the agent without delivering anything', async () => {
            const subject = `C6 test ${uniqueSuffix()}`;

            const startedJob = await deployer.triggerWebhookAndWaitForJob(deployedProject, {
                message: `Send a notification with subject ${subject}`,
            });

            await waitForJobStatus(authenticatedPage, startedJob.id, 'FAILED', 2 * JOB_TIMEOUT);

            expect(await getFailureMessage(authenticatedPage, startedJob.id)).toContain(
                'The chat approval channel requires a run with a chat origin, but this run was not started from a chat conversation. Configure a fallback approval channel'
            );

            expect(await countMailpitMessages(request, subjectQuery(subject))).toBe(0);
            expect(
                await findApprovalTasksByName(
                    authenticatedPage,
                    `${gateName}: approve tool call send_notification_email`
                )
            ).toHaveLength(0);
        });
    });

    test('C7 a gate expiry sets the task due date', async ({authenticatedPage, deployer, request}) => {
        const gateName = `C7 gate ${uniqueSuffix()}`;

        const deployedProject = await deployer.deploy([
            buildGatedAgentWorkflow({
                label: `C7 gate ${uniqueSuffix()}`,
                tools: [
                    buildApprovalGate({
                        approvalChannels: [buildApprovalTaskChannel()],
                        expiresIn: 1,
                        expiresInUnit: 'HOURS',
                        label: 'Mail gate',
                        name: gateName,
                        tools: [buildSendNotificationTool(uniqueRecipient('c7-tool'))],
                    }),
                ],
                trigger: 'webhook',
            }),
        ]);

        const gatedRun = await startGatedWebhookRun(
            deployer,
            authenticatedPage,
            deployedProject,
            `Send a notification with subject C7 test ${uniqueSuffix()}`
        );

        const approvalTask = await waitForApprovalTaskStatus(
            authenticatedPage,
            `${gateName}: approve tool call send_notification_email`,
            'OPEN'
        );

        expectExpiryAfter(approvalTask.dueDate, ONE_HOUR);

        await resumeJob({body: {approved: false, comment: 'clean up'}, request, token: gatedRun.token});
    });

    test(`C7 an expired gate fails the run and the tool never runs ${SLOW_TAG}`, async ({
        authenticatedPage,
        deployer,
        request,
    }) => {
        skipUnlessSlowRunEnabled();

        test.setTimeout(ONE_HOUR + 30 * 60 * 1000);

        const gateName = `C7 slow gate ${uniqueSuffix()}`;
        const formTitle = `${gateName}: approve tool call send_notification_email`;

        const deployedProject = await deployer.deploy([
            buildGatedAgentWorkflow({
                label: `C7 slow gate ${uniqueSuffix()}`,
                tools: [
                    buildApprovalGate({
                        approvalChannels: [buildApprovalTaskChannel()],
                        expiresIn: 1,
                        expiresInUnit: 'HOURS',
                        label: 'Mail gate',
                        name: gateName,
                        tools: [buildSendNotificationTool(uniqueRecipient('c7-tool'))],
                    }),
                ],
                trigger: 'webhook',
            }),
        ]);

        const subjects = [`C7 late ${uniqueSuffix()}`, `C7 unattended ${uniqueSuffix()}`];

        const lateRun = await startGatedWebhookRun(
            deployer,
            authenticatedPage,
            deployedProject,
            `Send a notification with subject ${subjects[0]}`
        );

        const unattendedRun = await startGatedWebhookRun(
            deployer,
            authenticatedPage,
            deployedProject,
            `Send a notification with subject ${subjects[1]}`
        );

        const pendingApproval = await waitForPendingApproval(authenticatedPage, lateRun.jobId);

        await waitAndStaySignedIn(
            authenticatedPage,
            Date.parse(pendingApproval.expiresAt as string) - Date.now() + 5000
        );

        await test.step('A late approval answers 410 and fails the run', async () => {
            expect((await resumeJob({body: {approved: true}, request, token: lateRun.token})).status()).toBe(410);

            const failedJob = await waitForJobStatus(authenticatedPage, lateRun.jobId, 'FAILED');

            expect(getJobErrorMessage(failedJob)).toMatch(/The pending approval expired at .+ without being resolved/);
        });

        await test.step('The unattended run fails through its timer or the sweep', async () => {
            await waitForJobStatus(authenticatedPage, unattendedRun.jobId, 'FAILED', 20 * 60 * 1000);

            expect((await resumeJob({body: {approved: true}, request, token: unattendedRun.token})).status()).toBe(410);
        });

        await test.step('Both tasks expired and the tool never ran', async () => {
            const approvalTasks = await findApprovalTasksByName(authenticatedPage, formTitle);

            expect(approvalTasks.map((approvalTask) => approvalTask.status)).toEqual(['EXPIRED', 'EXPIRED']);

            for (const subject of subjects) {
                expect(await countMailpitMessages(request, subjectQuery(subject))).toBe(0);
            }
        });
    });

    test('C8 each gate uses its own channels and expiry', async ({authenticatedPage, deployer, request}) => {
        const suffix = uniqueSuffix();
        const gateARecipient = uniqueRecipient('c8-gate-a');
        const gateAName = `Gate A ${suffix}`;
        const gateBName = `Gate B ${suffix}`;

        const deployedProject = await deployer.deploy([
            buildGatedAgentWorkflow({
                label: `C8 two gates ${suffix}`,
                tools: [
                    buildApprovalGate({
                        approvalChannels: [buildEmailApprovalChannel(gateARecipient, 'C8 gate A approval')],
                        expiresIn: 2,
                        expiresInUnit: 'HOURS',
                        label: 'Gate A',
                        name: gateAName,
                        tools: [buildSendNotificationTool(uniqueRecipient('c8-tool'))],
                    }),
                    buildApprovalGate({
                        approvalChannels: [buildApprovalTaskChannel()],
                        expiresIn: 3,
                        expiresInUnit: 'DAYS',
                        label: 'Gate B',
                        name: gateBName,
                        nodeName: 'approvalGate_2',
                        tools: [buildMailServerInfoTool()],
                    }),
                ],
                trigger: 'webhook',
            }),
        ]);

        await test.step('Gate A mails its approver with a two-hour expiry and opens no task', async () => {
            const gateARun = await startGatedWebhookRun(
                deployer,
                authenticatedPage,
                deployedProject,
                `Send a notification with subject C8 test ${suffix}`
            );

            const approvalMail = await waitForMailpitMessage(request, recipientQuery(gateARecipient));

            expect(approvalMail.Text).toContain(`${gateAName}: approve tool call send_notification_email`);

            const pendingApproval = await waitForPendingApproval(authenticatedPage, gateARun.jobId);

            expectExpiryAfter(pendingApproval.expiresAt, 2 * ONE_HOUR);

            expect(
                await findApprovalTasksByName(
                    authenticatedPage,
                    `${gateAName}: approve tool call send_notification_email`
                )
            ).toHaveLength(0);

            expect((await resumeJob({body: {approved: true}, request, token: gateARun.token})).status()).toBe(204);

            await waitForJobStatus(authenticatedPage, gateARun.jobId, 'COMPLETED', 2 * JOB_TIMEOUT);
        });

        await test.step('Gate B opens a task due in three days and sends no mail', async () => {
            const gateBRun = await startGatedWebhookRun(
                deployer,
                authenticatedPage,
                deployedProject,
                'What version is the mail server running?'
            );

            const approvalTask = await waitForApprovalTaskStatus(
                authenticatedPage,
                `${gateBName}: approve tool call get_mail_server_info`,
                'OPEN'
            );

            expectExpiryAfter(approvalTask.dueDate, 3 * DAY_MILLISECONDS);

            expect(await countMailpitMessages(request, recipientQuery(gateARecipient))).toBe(1);

            expect((await resumeJob({body: {approved: true}, request, token: gateBRun.token})).status()).toBe(204);

            await waitForJobStatus(authenticatedPage, gateBRun.jobId, 'COMPLETED', 2 * JOB_TIMEOUT);
        });
    });

    test('C9 server-owned approval keys cannot be spoofed by channel parameters', async ({
        authenticatedPage,
        deployer,
        request,
    }) => {
        const approverRecipient = uniqueRecipient('c9-approver');
        const gateName = `C9 gate ${uniqueSuffix()}`;
        const subject = `C9 test ${uniqueSuffix()}`;

        const spoofingChannel = buildEmailApprovalChannel(approverRecipient, 'C9 spoof check');

        spoofingChannel.parameters = {
            ...(spoofingChannel.parameters as Record<string, unknown>),
            expiresAt: '2099-01-01T00:00:00Z',
            formDescription: 'SPOOFED DESCRIPTION',
            formTitle: 'SPOOFED TITLE',
            publicUrl: 'http://evil.example',
            resumeId: 'spoofed-id',
            resumeUrl: 'http://evil.example/resume',
        };

        const deployedProject = await deployer.deploy([
            buildGatedAgentWorkflow({
                label: `C9 spoof ${uniqueSuffix()}`,
                tools: [
                    buildApprovalGate({
                        approvalChannels: [spoofingChannel],
                        label: 'Mail gate',
                        name: gateName,
                        tools: [buildSendNotificationTool(uniqueRecipient('c9-tool'))],
                    }),
                ],
                trigger: 'webhook',
            }),
        ]);

        const gatedRun = await startGatedWebhookRun(
            deployer,
            authenticatedPage,
            deployedProject,
            `Send a notification with subject ${subject}`
        );

        await test.step('The mail carries the real title, arguments, expiry and signed links', async () => {
            const approvalMail = await waitForMailpitMessage(request, recipientQuery(approverRecipient));

            const mailContent = `${approvalMail.Text}\n${approvalMail.HTML}`;

            expect(approvalMail.Text).toContain(`${gateName}: approve tool call send_notification_email`);
            expect(approvalMail.Text).toContain(subject);

            for (const spoofedValue of ['SPOOFED', 'evil.example', '2099', 'spoofed-id']) {
                expect(mailContent).not.toContain(spoofedValue);
            }

            const mailLinks = extractLinks(approvalMail.HTML).filter((mailLink) => mailLink.includes('/resume/'));

            expect(mailLinks.length).toBeGreaterThan(0);

            for (const mailLink of mailLinks) {
                expect(mailLink).toMatch(/\/resume\/v1\./);
            }

            const pendingApproval = await waitForPendingApproval(authenticatedPage, gatedRun.jobId);

            expectExpiryAfter(pendingApproval.expiresAt, 60 * DAY_MILLISECONDS);
        });

        await test.step('Approving runs the tool', async () => {
            expect((await resumeJob({body: {approved: true}, request, token: gatedRun.token})).status()).toBe(204);

            await waitForMailpitMessage(request, subjectQuery(subject));

            await waitForJobStatus(authenticatedPage, gatedRun.jobId, 'COMPLETED', 2 * JOB_TIMEOUT);
        });
    });

    for (const {designTimeError, placement, runtimeError} of [
        {
            designTimeError: '[approvalGate_1] An approval gate cannot be placed under a subagent',
            placement: 'a' as const,
            runtimeError: "Approval gate 'approvalGate_1' cannot be attached to subagent 'subagentTool_1'",
        },
        {
            designTimeError: "[requestApproval_1] 'requestApproval' cannot be attached to an approval gate",
            placement: 'b' as const,
            runtimeError: "'requestApproval' cannot be attached to an approval gate",
        },
        {
            designTimeError: "[requestApproval_1] 'requestApproval' cannot be attached to subagent 'subagentTool_1'",
            placement: 'c' as const,
            runtimeError: "'requestApproval' tool",
        },
    ]) {
        test(`C10${placement} an invalid approval placement is rejected before any model call`, async ({
            authenticatedPage,
            deployer,
            request,
        }) => {
            const subject = `C10${placement} test ${uniqueSuffix()}`;

            const deployedProject = await deployer.deploy([
                buildGatedAgentWorkflow({
                    label: `C10${placement} placement ${uniqueSuffix()}`,
                    tools: buildInvalidPlacementTools(placement, uniqueRecipient(`c10${placement}-tool`)),
                    trigger: 'webhook',
                }),
            ]);

            await test.step('Validation reports a design-time error', async () => {
                const [projectWorkflow] = await getProjectWorkflows(authenticatedPage, deployedProject.projectId);

                const validationResult = await validateWorkflowById(authenticatedPage, projectWorkflow.id);

                expect(validationResult.errors.join('\n')).toContain(designTimeError);
            });

            await test.step('A run fails at once and delivers nothing', async () => {
                const startedJob = await deployer.triggerWebhookAndWaitForJob(deployedProject, {
                    message: `Send a notification with subject ${subject}`,
                });

                await waitForJobStatus(authenticatedPage, startedJob.id, 'FAILED');

                expect(await getFailureMessage(authenticatedPage, startedJob.id)).toContain(runtimeError);

                expect(await countMailpitMessages(request, subjectQuery(subject))).toBe(0);
                expect(
                    await findApprovalTasksByName(
                        authenticatedPage,
                        'C10 gate: approve tool call send_notification_email'
                    )
                ).toHaveLength(0);
            });
        });
    }

    test('C11 a gate on a synchronous webhook is rejected up front', async ({authenticatedPage, deployer, request}) => {
        const gateName = `C11 gate ${uniqueSuffix()}`;

        const deployedProject = await deployer.deploy([
            buildGatedAgentWorkflow({
                label: `C11 sync gate ${uniqueSuffix()}`,
                tools: [
                    buildApprovalGate({
                        approvalChannels: [buildApprovalTaskChannel()],
                        label: 'Gate',
                        name: gateName,
                        tools: [buildMailServerInfoTool('httpClient_1')],
                    }),
                ],
                trigger: 'synchronousWebhook',
            }),
        ]);

        const webhookResponse = await request.post(deployedProject.workflows[0].webhookUrl, {
            data: {message: 'What version is the mail server running?'},
            headers: {'X-Csrf-Token': SYNC_WEBHOOK_CSRF_TOKEN},
        });

        expect(webhookResponse.status()).toBe(400);
        expect(await webhookResponse.text()).toContain(
            "Workflows that return a synchronous response can't contain steps that pause the run, such as approval or wait steps. Step 'aiAgent_1' (aiAgent/v1/chat) would pause it"
        );

        expect(await getProjectJobs(authenticatedPage, deployer.workspaceId, deployedProject.projectId)).toHaveLength(
            0
        );
        expect(
            await findApprovalTasksByName(authenticatedPage, `${gateName}: approve tool call get_mail_server_info`)
        ).toHaveLength(0);
    });

    test('C12 the editor shows what each gate holds', async ({authenticatedPage, deployer, project}) => {
        await deployer.importWorkflows(project.id, [
            buildGatedAgentWorkflow({
                label: `C12 two gates ${uniqueSuffix()}`,
                tools: [
                    buildApprovalGate({
                        approvalChannels: [buildEmailApprovalChannel(uniqueRecipient('c12'), 'C12 approval')],
                        label: 'Gate A',
                        name: 'Gate A',
                        tools: [buildSendNotificationTool(uniqueRecipient('c12-tool'))],
                    }),
                    buildApprovalGate({
                        approvalChannels: [buildApprovalTaskChannel()],
                        label: 'Gate B',
                        name: 'Gate B',
                        nodeName: 'approvalGate_2',
                        tools: [buildMailServerInfoTool()],
                    }),
                ],
                trigger: 'webhook',
            }),
        ]);

        const [projectWorkflow] = await getProjectWorkflows(authenticatedPage, project.id);

        const workflowIssuesPanel = new WorkflowIssuesPanel(authenticatedPage);

        const expectGateGroups = async () => {
            const gateAGroup = authenticatedPage.getByRole('group', {name: 'Gate A requires approval'});
            const gateBGroup = authenticatedPage.getByRole('group', {name: 'Gate B requires approval'});

            await expect(gateAGroup).toContainText('Send notification email');
            await expect(gateAGroup).toContainText(/Channels: email/i);
            await expect(gateAGroup).not.toContainText('Mail server info');

            await expect(gateBGroup).toContainText('Mail server info');
            await expect(gateBGroup).toContainText(/Channels: approval ?task/i);
            await expect(gateBGroup).not.toContainText('Send notification email');
        };

        const openAiAgentEditor = async () => {
            await workflowIssuesPanel.gotoWorkflowEditor(project.id, projectWorkflow.projectWorkflowId);

            await authenticatedPage
                .locator('.react-flow__node')
                .filter({hasText: 'aiAgent_1'})
                .locator('button:not([aria-label])')
                .first()
                .click();

            await expect(authenticatedPage.getByText('Tools this agent can use:')).toBeVisible({timeout: 30000});
        };

        await test.step('The AI Agent Editor lists each gate with its own tools and channels', async () => {
            await openAiAgentEditor();

            await expectGateGroups();
        });

        await test.step('The advanced canvas shows each gate with its tools and channels', async () => {
            await authenticatedPage.getByRole('button', {name: 'Advanced'}).click();

            for (const nodeText of [
                'Gate A',
                'Gate B',
                'Send notification email',
                'Mail server info',
                'Approval Task',
            ]) {
                await expect(authenticatedPage.getByText(nodeText, {exact: true}).first()).toBeVisible();
            }

            await authenticatedPage.locator('button:has(svg.lucide-text-initial)').first().click();

            await expect(authenticatedPage.getByText('Tools this agent can use:')).toBeVisible();
        });

        await test.step('A workflow reloaded from the server keeps the grouping', async () => {
            await openAiAgentEditor();

            await expectGateGroups();
        });
    });
});
