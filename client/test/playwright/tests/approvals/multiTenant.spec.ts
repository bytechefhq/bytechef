import {type Browser, type Page, expect, test} from '@playwright/test';

import {AUTOMATION_API_PATH, apiFetch, apiJson} from '../../utils/apiUtils';
import {uniqueSuffix} from '../../utils/approvalRunUtils';
import {getPendingApprovals, getShortId, newAnonymousPage} from '../../utils/approvalUtils';
import {buildApprovalWorkflow, buildChatApprovalWorkflow} from '../../utils/approvalWorkflowDefinitions';
import {
    buildDiscordButtonInteraction,
    postDiscordInteraction,
    postTelegramCallback,
} from '../../utils/channelCallbackUtils';
import {
    type ConnectionComponentNameType,
    type DeployedProjectI,
    type WorkflowDefinitionType,
    createConnection,
    createProjectViaApi,
    deployProject,
    importWorkflowDefinition,
} from '../../utils/deploymentUtils';
import {getProjectJobs, getTaskOutput, postWebhook, waitForJobStatus, waitForNewProjectJob} from '../../utils/jobUtils';
import {login} from '../../utils/login';
import {
    getApprovalRequests,
    getEventNames,
    getResultMessage,
    getStartedJobId,
    resumeJobStreaming,
    sendChatMessage,
} from '../../utils/sseUtils';
import {skipUnlessDiscordKeyIsSet, skipUnlessMultiTenant} from '../../utils/testEnvironment';

const TENANT_PASSWORD = 'PlaywrightTenant!1';

interface TenantI {
    deploy: (definition: WorkflowDefinitionType) => Promise<DeployedProjectI>;
    name: string;
    page: Page;
    startRun: (deployedProject: DeployedProjectI) => Promise<string>;
    telegramSecretToken: string;
}

/**
 * Signs up a fresh account, which on a multi-tenant server creates its own tenant, and logs it in.
 */
async function signUpTenant(browser: Browser, name: string): Promise<TenantI> {
    const page = await newAnonymousPage(browser);
    const email = `pw-${name}-${uniqueSuffix()}@example.com`;
    const telegramSecretToken = `telegram-secret-${name}-${uniqueSuffix()}`;

    await page.goto('/login');

    const registerResponse = await apiFetch({
        body: {email, langKey: 'en', login: email, password: TENANT_PASSWORD},
        method: 'POST',
        page,
        path: '/api/register',
    });

    expect(registerResponse.status()).toBe(201);

    await login(page, email, TENANT_PASSWORD);

    const [workspace] = await apiJson<Array<{id: number}>>({
        method: 'GET',
        page,
        path: `${AUTOMATION_API_PATH}/workspaces`,
    });

    const connectionIds = new Map<ConnectionComponentNameType, number>();

    const resolveConnectionId = async (componentName: ConnectionComponentNameType) => {
        let connectionId = connectionIds.get(componentName);

        if (connectionId === undefined) {
            connectionId = await createConnection(
                page,
                workspace.id,
                componentName,
                componentName === 'telegram' ? {webhookSecretToken: telegramSecretToken} : {}
            );

            connectionIds.set(componentName, connectionId);
        }

        return connectionId;
    };

    return {
        deploy: async (definition) => {
            const projectId = await createProjectViaApi(page, workspace.id);

            await importWorkflowDefinition(page, projectId, definition);

            return deployProject({connectionIdResolver: resolveConnectionId, page, projectId});
        },
        name,
        page,
        startRun: async (deployedProject) => {
            const projectJobs = await getProjectJobs(page, workspace.id, deployedProject.projectId);

            await postWebhook(page.request, deployedProject.workflows[0].webhookUrl, {});

            return waitForNewProjectJob({
                knownJobIds: projectJobs.map((job) => job.id),
                page,
                projectId: deployedProject.projectId,
                workspaceId: workspace.id,
            });
        },
        telegramSecretToken,
    };
}

/**
 * Starts runs in whichever tenant is behind until both tenants' next runs get the same job id, which is what makes
 * a cross-tenant mix-up observable.
 */
async function alignNextJobIds(tenants: [TenantI, TenantI], fillerProjects: [DeployedProjectI, DeployedProjectI]) {
    const latestJobIds = [0, 0];

    for (let tenantIndex = 0; tenantIndex < 2; tenantIndex++) {
        latestJobIds[tenantIndex] = Number(await tenants[tenantIndex].startRun(fillerProjects[tenantIndex]));
    }

    for (let attempt = 0; attempt < 50 && latestJobIds[0] !== latestJobIds[1]; attempt++) {
        const laggingIndex = latestJobIds[0] < latestJobIds[1] ? 0 : 1;

        latestJobIds[laggingIndex] = Number(await tenants[laggingIndex].startRun(fillerProjects[laggingIndex]));
    }

    expect(latestJobIds[0]).toBe(latestJobIds[1]);
}

test.describe('Approvals across tenants', () => {
    test.beforeEach(() => {
        skipUnlessMultiTenant();
    });

    test('G1 chat approvals stay inside their own tenant', async ({browser}) => {
        test.setTimeout(5 * 60 * 1000);

        const tenants: [TenantI, TenantI] = [await signUpTenant(browser, 'a'), await signUpTenant(browser, 'b')];

        const fillerDefinition = {
            description: 'filler',
            label: `G1 filler ${uniqueSuffix()}`,
            tasks: [{name: 'var_1', parameters: {type: 'STRING', value: 'filler'}, type: 'var/v1/set'}],
            triggers: [{name: 'trigger_1', parameters: {}, type: 'webhook/v1/autoRespondWithHTTP200'}],
        };

        const fillerProjects: [DeployedProjectI, DeployedProjectI] = [
            await tenants[0].deploy(fillerDefinition),
            await tenants[1].deploy(fillerDefinition),
        ];

        const chatProjects = [
            await tenants[0].deploy(buildChatApprovalWorkflow(`G1 chat ${uniqueSuffix()}`)),
            await tenants[1].deploy(buildChatApprovalWorkflow(`G1 chat ${uniqueSuffix()}`)),
        ];

        await alignNextJobIds(tenants, fillerProjects);

        const messages = ['TENANT-A-REQ', 'TENANT-B-REQ'];

        const chatStreams = await test.step('Both chat streams carry only their own events', async () => {
            const sentChatStreams = await Promise.all(
                chatProjects.map((chatProject, tenantIndex) =>
                    sendChatMessage({
                        conversationId: `g1-${tenantIndex}-${uniqueSuffix()}`,
                        message: messages[tenantIndex],
                        request: tenants[tenantIndex].page.request,
                        webhookUrl: chatProject.workflows[0].webhookUrl,
                    })
                )
            );

            sentChatStreams.forEach((sentChatStream, tenantIndex) => {
                expect(getEventNames(sentChatStream.events)).toEqual(['start', 'approval_request', 'suspended']);

                const streamText = JSON.stringify(sentChatStream.events);

                expect(streamText).toContain(messages[tenantIndex]);
                expect(streamText).not.toContain(messages[1 - tenantIndex]);
            });

            return sentChatStreams;
        });

        await test.step('Concurrent streaming resumes resolve each tenant run', async () => {
            const decisions = [
                {approved: true, comment: 'from A'},
                {approved: false, comment: 'from B'},
            ];

            const resumeStreams = await Promise.all(
                chatStreams.map((chatStream, tenantIndex) =>
                    resumeJobStreaming({
                        body: decisions[tenantIndex],
                        request: tenants[tenantIndex].page.request,
                        token: getApprovalRequests(chatStream.events)[0].resumeId,
                    })
                )
            );

            expect(getResultMessage(resumeStreams[0].events)).toBe(
                'Decision for TENANT-A-REQ: approved=true comment=from A'
            );
            expect(getResultMessage(resumeStreams[1].events)).toBe(
                'Decision for TENANT-B-REQ: approved=false comment=from B'
            );

            for (let tenantIndex = 0; tenantIndex < 2; tenantIndex++) {
                await waitForJobStatus(
                    tenants[tenantIndex].page,
                    getStartedJobId(chatStreams[tenantIndex].events),
                    'COMPLETED'
                );
            }
        });

        await test.step('Each pending inbox lists only its own tenant runs', async () => {
            const pendingLabels = [`G1 pending A ${uniqueSuffix()}`, `G1 pending B ${uniqueSuffix()}`];

            for (let tenantIndex = 0; tenantIndex < 2; tenantIndex++) {
                const pendingProject = await tenants[tenantIndex].deploy(
                    buildApprovalWorkflow({
                        channels: ['approvalTask'],
                        formTitle: pendingLabels[tenantIndex],
                        label: pendingLabels[tenantIndex],
                    })
                );

                const pendingJobId = await tenants[tenantIndex].startRun(pendingProject);

                await waitForJobStatus(tenants[tenantIndex].page, pendingJobId, 'STOPPED');
            }

            for (let tenantIndex = 0; tenantIndex < 2; tenantIndex++) {
                const listedLabels = (await getPendingApprovals(tenants[tenantIndex].page)).map(
                    (pendingApproval) => pendingApproval.workflowLabel
                );

                expect(listedLabels).toContain(pendingLabels[tenantIndex]);
                expect(listedLabels).not.toContain(pendingLabels[1 - tenantIndex]);
            }
        });
    });

    test('G3 Telegram and Discord short ids resolve in every tenant', async ({browser}) => {
        skipUnlessDiscordKeyIsSet();

        test.setTimeout(5 * 60 * 1000);

        const tenants: [TenantI, TenantI] = [await signUpTenant(browser, 'a'), await signUpTenant(browser, 'b')];

        const definition = buildApprovalWorkflow({
            channels: ['approvalTask', 'telegram', 'discord'],
            formTitle: `G3 short ids ${uniqueSuffix()}`,
            label: `G3 short ids ${uniqueSuffix()}`,
        });

        const approvalProjects: [DeployedProjectI, DeployedProjectI] = [
            await tenants[0].deploy(definition),
            await tenants[1].deploy(definition),
        ];

        await alignNextJobIds(tenants, approvalProjects);

        const jobIds = [await tenants[0].startRun(approvalProjects[0]), await tenants[1].startRun(approvalProjects[1])];

        expect(jobIds[0]).toBe(jobIds[1]);

        const shortIds: string[] = [];

        for (let tenantIndex = 0; tenantIndex < 2; tenantIndex++) {
            await waitForJobStatus(tenants[tenantIndex].page, jobIds[tenantIndex], 'STOPPED');

            const [pendingApproval] = (await getPendingApprovals(tenants[tenantIndex].page)).filter(
                (listedApproval) => listedApproval.jobId === jobIds[tenantIndex]
            );

            const token = (pendingApproval.formUrl as string).split('/resume/')[1];

            shortIds.push(await getShortId(tenants[tenantIndex].page.request, token));
        }

        await test.step("Tenant B's Telegram callback resolves only tenant B's run", async () => {
            const telegramResponse = await postTelegramCallback({
                callbackData: `${shortIds[1]}:a`,
                request: tenants[1].page.request,
                secretToken: tenants[1].telegramSecretToken,
                userName: 'tenant_b_user',
            });

            expect(telegramResponse.status()).toBe(200);

            await waitForJobStatus(tenants[1].page, jobIds[1], 'COMPLETED');

            expect(await getTaskOutput(tenants[1].page, jobIds[1], 'approval_1')).toMatchObject({
                approvedBy: '@tenant_b_user',
            });

            await waitForJobStatus(tenants[0].page, jobIds[0], 'STOPPED');
        });

        await test.step("Tenant A's Discord interaction resolves only tenant A's run", async () => {
            const discordResponse = await postDiscordInteraction({
                body: buildDiscordButtonInteraction(`${shortIds[0]}:a`, 'tenant_a_user'),
                request: tenants[0].page.request,
            });

            expect(JSON.stringify(await discordResponse.json())).toContain('✅ Approved by @tenant_a_user.');

            await waitForJobStatus(tenants[0].page, jobIds[0], 'COMPLETED');
        });

        await test.step('Clicking again reports the approval as already resolved', async () => {
            const repeatedDiscordResponse = await postDiscordInteraction({
                body: buildDiscordButtonInteraction(`${shortIds[0]}:d`, 'tenant_a_user'),
                request: tenants[0].page.request,
            });

            expect(JSON.stringify(await repeatedDiscordResponse.json())).toContain(
                'This approval was already resolved'
            );

            const repeatedTelegramResponse = await postTelegramCallback({
                callbackData: `${shortIds[1]}:d`,
                request: tenants[1].page.request,
                secretToken: tenants[1].telegramSecretToken,
            });

            expect(repeatedTelegramResponse.status()).toBe(200);

            expect(await getTaskOutput(tenants[1].page, jobIds[1], 'approval_1')).toMatchObject({approved: true});
        });
    });
});
