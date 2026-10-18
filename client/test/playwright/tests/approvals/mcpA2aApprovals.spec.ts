import {type Page, expect, mergeTests} from '@playwright/test';

import {loginTest, projectTest, workflowDeploymentTest} from '../../fixtures';
import {type WorkflowDeployerI} from '../../fixtures/workflowDeployment';
import {ApprovalFormPage} from '../../pages/approvalFormPage';
import {
    McpTestClient,
    getA2aStatusText,
    getA2aTask,
    getA2aTaskStatus,
    getToolResultText,
    sendA2aMessage,
} from '../../utils/agentProtocolClients';
import {DEVELOPMENT_ENVIRONMENT_ID, SERVER_URL, graphqlData, graphqlRequest} from '../../utils/apiUtils';
import {uniqueSuffix} from '../../utils/approvalRunUtils';
import {
    extractApprovalToken,
    getTokenExpiryEpochSeconds,
    newAnonymousPage,
    resumeJob,
    waitForPendingApproval,
} from '../../utils/approvalUtils';
import {buildCallableApprovalWorkflow} from '../../utils/approvalWorkflowDefinitions';
import {getProjectVersionWorkflows, publishProject} from '../../utils/deploymentUtils';
import {getTaskOutput, waitForJobStatus} from '../../utils/jobUtils';

export const test = mergeTests(loginTest(), projectTest, workflowDeploymentTest);

interface PublishedCallableWorkflowI {
    label: string;
    projectId: string;
    projectVersion: number;
    workflowId: string;
}

async function publishCallableApprovalWorkflow(
    page: Page,
    deployer: WorkflowDeployerI,
    projectId: string,
    label: string
): Promise<PublishedCallableWorkflowI> {
    await deployer.importWorkflows(projectId, [buildCallableApprovalWorkflow({channels: [], label})]);

    const projectVersion = await publishProject(page, projectId);

    const [versionWorkflow] = await getProjectVersionWorkflows(page, projectId, projectVersion);

    return {label, projectId, projectVersion, workflowId: versionWorkflow.id};
}

async function expectLinkToExpireOneHourAfterTheApproval(page: Page, jobId: string, formUrl: string) {
    const pendingApproval = await waitForPendingApproval(page, jobId);

    const linkLifetimeSeconds =
        getTokenExpiryEpochSeconds(extractApprovalToken(formUrl)) -
        Date.parse(pendingApproval.expiresAt as string) / 1000;

    expect(Math.abs(linkLifetimeSeconds - 3600)).toBeLessThanOrEqual(5);
}

test.describe('Approvals behind MCP and A2A', () => {
    test('E1 an MCP tool runs a workflow that needs approval', async ({
        authenticatedPage,
        browser,
        deployer,
        project,
        request,
    }) => {
        const published = await publishCallableApprovalWorkflow(
            authenticatedPage,
            deployer,
            project.id,
            `E1 MCP approval ${uniqueSuffix()}`
        );

        const mcpServer = await graphqlData<{createWorkspaceMcpServer: {id: string; secretKey: string; url: string}}>(
            authenticatedPage,
            'mutation createWorkspaceMcpServer($input: CreateWorkspaceMcpServerInput!) { createWorkspaceMcpServer(input: $input) { id secretKey url } }',
            {
                input: {
                    authenticationRequired: false,
                    enabled: true,
                    environmentId: DEVELOPMENT_ENVIRONMENT_ID,
                    name: `E1 MCP ${uniqueSuffix()}`,
                    type: 'AUTOMATION',
                    workspaceId: deployer.workspaceId,
                },
            }
        );

        const mcpServerId = mcpServer.createWorkspaceMcpServer.id;

        let mcpProjectId: string | undefined;

        try {
            const mcpProject = await graphqlData<{createMcpProject: {id: string}}>(
                authenticatedPage,
                'mutation createMcpProject($input: CreateMcpProjectInput!) { createMcpProject(input: $input) { id } }',
                {
                    input: {
                        mcpServerId,
                        projectId: published.projectId,
                        projectVersion: published.projectVersion,
                        selectedWorkflowIds: [published.workflowId],
                    },
                }
            );

            mcpProjectId = mcpProject.createMcpProject.id;

            const mcpEndpointUrl = `${SERVER_URL}/api/automation/${mcpServer.createWorkspaceMcpServer.secretKey}/mcp`;

            const findToolName = async (mcpClient: McpTestClient) => {
                const toolNames = await mcpClient.listToolNames();

                const workflowToolName = toolNames.find((toolName) =>
                    toolName.toLowerCase().includes(published.label.split(' ').pop() as string)
                );

                expect(workflowToolName, toolNames.join(', ')).toBeTruthy();

                return workflowToolName as string;
            };

            await test.step('A client without elicitation gets the form link at once', async () => {
                const mcpClient = new McpTestClient(mcpEndpointUrl);

                await mcpClient.initialize();

                const {result} = await mcpClient.callTool(await findToolName(mcpClient), {item: 'plain'});

                expect(result.result?.isError).toBe(false);

                const toolResult = JSON.parse(getToolResultText(result)) as Record<string, unknown>;

                expect(toolResult.status).toBe('approval_required');
                expect(toolResult).not.toHaveProperty('resumeToken');
                expect(String(toolResult.formUrl)).toContain('/resume/v1.');
                expect(String(toolResult.message)).toContain('Approval required');
                expect(String(toolResult.message)).toContain(`Resolve it at: ${toolResult.formUrl}`);

                const jobId = String(toolResult.jobId);

                await expectLinkToExpireOneHourAfterTheApproval(authenticatedPage, jobId, String(toolResult.formUrl));

                const approvalFormPage = new ApprovalFormPage(await newAnonymousPage(browser));

                await approvalFormPage.goto(extractApprovalToken(String(toolResult.formUrl)));

                await approvalFormPage.commentInput.fill('mcp ok');

                await approvalFormPage.approveButton.click();

                await approvalFormPage.expectApproved();

                await waitForJobStatus(authenticatedPage, jobId, 'COMPLETED');

                expect(await getTaskOutput(authenticatedPage, jobId, 'approval_1')).toMatchObject({
                    approved: true,
                    comment: 'mcp ok',
                });
            });

            await test.step('A URL-elicitation client gets the decision on the same tools/call', async () => {
                const mcpClient = new McpTestClient(mcpEndpointUrl, {elicitation: {url: {}}});

                await mcpClient.initialize();

                const {elicitationRequests, result} = await mcpClient.callTool(
                    await findToolName(mcpClient),
                    {item: 'url'},
                    async (elicitationRequest) => {
                        const elicitationUrl = String(elicitationRequest.params?.url);

                        setTimeout(() => {
                            void resumeJob({
                                body: {approved: true, comment: 'via url elicitation'},
                                request,
                                token: extractApprovalToken(elicitationUrl),
                            });
                        }, 2000);

                        return {action: 'accept'};
                    }
                );

                expect(elicitationRequests).toHaveLength(1);
                expect(elicitationRequests[0].params?.mode).toBe('url');
                expect(String(elicitationRequests[0].params?.url)).toContain('/resume/v1.');

                expect(getToolResultText(result)).toContain('"approved":true');
            });

            await test.step('A form-elicitation client answers the approval itself', async () => {
                const mcpClient = new McpTestClient(mcpEndpointUrl, {elicitation: {form: {}}});

                await mcpClient.initialize();

                const {elicitationRequests, result} = await mcpClient.callTool(
                    await findToolName(mcpClient),
                    {item: 'form'},
                    async () => ({action: 'accept', content: {approved: false, comment: 'via form elicitation'}})
                );

                expect(elicitationRequests).toHaveLength(1);
                expect(elicitationRequests[0].params?.mode).toBe('form');

                const requestedSchema = JSON.stringify(elicitationRequests[0].params?.requestedSchema);

                expect(requestedSchema).toContain('approved');
                expect(requestedSchema).toContain('comment');

                const toolResultText = getToolResultText(result);

                expect(toolResultText).toContain('"approved":false');
                expect(toolResultText).toContain('via form elicitation');
            });
        } finally {
            if (mcpProjectId) {
                await graphqlRequest(
                    authenticatedPage,
                    'mutation deleteMcpProject($id: ID!) { deleteMcpProject(id: $id) }',
                    {
                        id: mcpProjectId,
                    }
                );
            }

            await graphqlRequest(
                authenticatedPage,
                'mutation deleteWorkspaceMcpServer($mcpServerId: ID!) { deleteWorkspaceMcpServer(mcpServerId: $mcpServerId) }',
                {mcpServerId}
            );
        }
    });

    test('E2 an A2A task pauses as input-required until the approval', async ({
        authenticatedPage,
        deployer,
        project,
        request,
    }) => {
        const published = await publishCallableApprovalWorkflow(
            authenticatedPage,
            deployer,
            project.id,
            `E2 A2A approval ${uniqueSuffix()}`
        );

        const a2aServer = await graphqlData<{createA2aServer: {id: string; secretKey: string}}>(
            authenticatedPage,
            'mutation createA2aServer($input: CreateA2aServerInput!) { createA2aServer(input: $input) { id secretKey } }',
            {
                input: {
                    authenticationRequired: false,
                    environmentId: DEVELOPMENT_ENVIRONMENT_ID,
                    name: `E2 A2A ${uniqueSuffix()}`,
                },
            }
        );

        const a2aServerId = a2aServer.createA2aServer.id;

        let a2aProjectId: string | undefined;

        try {
            const a2aProject = await graphqlData<{createA2aProject: {id: string}}>(
                authenticatedPage,
                'mutation createA2aProject($input: CreateA2aProjectInput!) { createA2aProject(input: $input) { id } }',
                {
                    input: {
                        a2aServerId,
                        projectId: published.projectId,
                        projectVersion: published.projectVersion,
                        selectedWorkflowIds: [published.workflowId],
                    },
                }
            );

            a2aProjectId = a2aProject.createA2aProject.id;

            const a2aEndpointUrl = `${SERVER_URL}/api/automation/a2a/${a2aServer.createA2aServer.secretKey}`;

            const sentTask = await sendA2aMessage(a2aEndpointUrl, 'lamp');

            const taskId = String(sentTask.result?.id);

            let formUrl = '';

            await test.step('message/send leaves the task input-required with the form link', async () => {
                expect(getA2aTaskStatus(sentTask).state).toBe('input-required');

                const statusText = getA2aStatusText(sentTask);

                expect(statusText).toContain(
                    'Approval required — the workflow run is paused waiting for a human decision. Resolve it at: '
                );

                formUrl = statusText.slice(statusText.indexOf('http'));

                expect(formUrl).toContain('/resume/v1.');

                expect(getA2aTaskStatus(await getA2aTask(a2aEndpointUrl, taskId)).state).toBe('input-required');
            });

            await test.step('Approving through the form completes the task', async () => {
                expect((await request.get(new URL(formUrl).pathname)).status()).toBe(200);

                const resumeResponse = await resumeJob({
                    body: {approved: true, comment: 'a2a ok'},
                    request,
                    token: extractApprovalToken(formUrl),
                });

                expect(resumeResponse.status()).toBe(204);

                await expect(async () => {
                    const completedTask = await getA2aTask(a2aEndpointUrl, taskId);

                    expect(getA2aTaskStatus(completedTask).state).toBe('completed');
                    expect(getA2aStatusText(completedTask)).toContain('"approved":true');
                    expect(getA2aStatusText(completedTask)).toContain('a2a ok');
                }).toPass({timeout: 60000});
            });
        } finally {
            if (a2aProjectId) {
                await graphqlRequest(
                    authenticatedPage,
                    'mutation deleteA2aProject($id: ID!) { deleteA2aProject(id: $id) }',
                    {
                        id: a2aProjectId,
                    }
                );
            }

            await graphqlRequest(authenticatedPage, 'mutation deleteA2aServer($id: ID!) { deleteA2aServer(id: $id) }', {
                id: a2aServerId,
            });
        }
    });
});
