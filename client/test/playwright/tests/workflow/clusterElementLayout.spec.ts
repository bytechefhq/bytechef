import {type Page, expect, mergeTests} from '@playwright/test';

import {loginTest} from '../../fixtures';
import {WorkflowPage} from '../../pages/workflowPage';
import {PROJECT_WORKFLOW_URL} from '../../utils/aiHubUtils';
import {
    type WorkflowDefinitionType,
    createProjectViaApi,
    deleteProjectViaApi,
    getProjectWorkflows,
    importWorkflowDefinition,
} from '../../utils/deploymentUtils';
import getRandomString from '../../utils/getRandomString';

export const test = mergeTests(loginTest());

const WORKSPACE_PROJECTS_URL = /\/api\/automation\/internal\/workspaces\/(\d+)\/projects(\?.*)?$/;

const SHARED_NODE_NAME = 'chat_1';

const SAVED_APPROVAL_CHANNEL_NAME = 'approvalChatChannel_1';

const AGENT_WITH_SAME_NAMED_APPROVAL_CHANNEL: WorkflowDefinitionType = {
    description: 'An AI agent whose approval channel shares its name',
    label: 'Agent with a same named approval channel',
    tasks: [
        {
            clusterElements: {
                model: {label: 'Anthropic', name: 'model_1', parameters: {}, type: 'anthropic/v1/model'},
                tools: [
                    {
                        clusterElements: {
                            approvalChannels: [
                                {
                                    label: 'Chat',
                                    name: SAVED_APPROVAL_CHANNEL_NAME,
                                    parameters: {},
                                    type: 'chat/v1/chat',
                                },
                            ],
                            tools: [
                                {
                                    label: 'Send Channel Message',
                                    name: 'sendChannelMessage_1',
                                    parameters: {},
                                    type: 'slack/v1/sendChannelMessage',
                                },
                            ],
                        },
                        label: 'Approval Gate',
                        name: 'approvalGate_1',
                        parameters: {name: 'slackGate'},
                        type: 'aiAgentUtils/v1/approvalGate',
                    },
                ],
            },
            label: 'AI Agent',
            name: SHARED_NODE_NAME,
            parameters: {},
            type: 'aiAgent/v1/chat',
        },
    ],
    triggers: [{label: 'Manual', name: 'trigger_1', parameters: {}, type: 'manual/v1/manual'}],
};

async function serveApprovalChannelUnderSharedName(page: Page, projectWorkflowId: number): Promise<{served: number}> {
    const servedWorkflows = {served: 0};

    await page.route(PROJECT_WORKFLOW_URL(projectWorkflowId), async (route) => {
        const response = await route.fetch();

        servedWorkflows.served++;

        await route.fulfill({
            body: (await response.text()).replaceAll(SAVED_APPROVAL_CHANNEL_NAME, SHARED_NODE_NAME),
            response,
        });
    });

    return servedWorkflows;
}

async function readCurrentWorkspaceId(page: Page): Promise<number> {
    const workspaceProjectsRequest = page.waitForRequest(WORKSPACE_PROJECTS_URL);

    await page.goto('/automation/projects');

    const workspaceProjectsUrl = (await workspaceProjectsRequest).url();

    return Number(workspaceProjectsUrl.match(WORKSPACE_PROJECTS_URL)?.[1]);
}

test.describe('Workflow editor cluster element layout', () => {
    test('an agent whose approval channel shares its name renders on the canvas', async ({authenticatedPage: page}) => {
        const pageErrors: string[] = [];

        page.on('pageerror', (pageError) => pageErrors.push(pageError.message));

        page.on('console', (consoleMessage) => {
            if (consoleMessage.type() === 'error') {
                pageErrors.push(consoleMessage.text());
            }
        });

        const workspaceId = await readCurrentWorkspaceId(page);
        const projectId = await createProjectViaApi(page, workspaceId, `cluster_layout_${getRandomString()}`);

        try {
            await importWorkflowDefinition(page, projectId, AGENT_WITH_SAME_NAMED_APPROVAL_CHANNEL);

            const [projectWorkflow] = await getProjectWorkflows(page, projectId);

            const servedWorkflows = await serveApprovalChannelUnderSharedName(page, projectWorkflow.projectWorkflowId);

            const workflowPage = new WorkflowPage(page);

            await workflowPage.goToWorkflowEditor(projectId, String(projectWorkflow.projectWorkflowId));

            expect(servedWorkflows.served).toBeGreaterThan(0);

            await expect(page.getByLabel(`${SHARED_NODE_NAME} node`, {exact: true}).first()).toBeVisible();

            for (const clusterElementName of ['model_1', 'approvalGate_1', 'sendChannelMessage_1']) {
                await expect(page.getByLabel(`${clusterElementName} node`, {exact: true})).toBeVisible();
            }

            await expect(page.getByText('Something went wrong')).toHaveCount(0);

            expect(pageErrors.filter((pageError) => pageError.includes('Maximum call stack size exceeded'))).toEqual(
                []
            );
        } finally {
            await deleteProjectViaApi(page, projectId);
        }
    });
});
