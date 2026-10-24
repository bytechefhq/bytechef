import {type Page, expect, mergeTests} from '@playwright/test';

import {loginTest} from '../../fixtures';
import {AiHubPage} from '../../pages/aiHubPage';
import {CopilotPanel} from '../../pages/copilotPanel';
import {WorkflowPage} from '../../pages/workflowPage';
import {
    type WorkflowNodeI,
    answerInteractiveCards,
    createProjectThroughApi,
    createProjectWorkflowThroughApi,
    deleteProjectThroughApi,
    postGraphQl,
    readWorkflowNodes,
    waitForHubWorkspaceId,
} from '../../utils/aiHubUtils';
import {enableCopilotInApplicationInfo, hideReactQueryDevtools} from '../../utils/copilotUtils';
import getRandomString from '../../utils/getRandomString';
import {McpClient, buildWorkflowAnsweringQuestions, toolResultText} from '../../utils/mcpClient';
import editDataStream from './fixtures/edit-data-stream.json';
import editInvoiceSchedule from './fixtures/edit-invoice-schedule.json';
import editOrderSync from './fixtures/edit-order-sync.json';
import editRemoveStep from './fixtures/edit-remove-step.json';
import editSupportAgent from './fixtures/edit-support-agent.json';

const test = mergeTests(loginTest());

const LIVE_TURN_TIMEOUT = 840000;

const TURN_START_TIMEOUT = 15000;

const DEVELOPMENT_ENVIRONMENT_ID = '0';

const PROJECTS_API_PATH = '/api/automation/internal/projects';

interface EditCaseI {
    /** Extra checks on the edited nodes. */
    checkNodes?: (workflowNodes: WorkflowNodeI[]) => void;
    /** Every pattern must match the type of at least one node after the edit. */
    expectedNodeTypes: RegExp[];
    instruction: string;
    name: string;
    /** Node names the edit must remove. */
    removedNodeNames?: string[];
    /** The saved workflow the edit starts from. */
    seed: {label: string};
    /** Node names, with their type pattern, that the edit must keep. */
    unchangedNodes: Record<string, RegExp>;
}

const findNode = (workflowNodes: WorkflowNodeI[], name: string) =>
    workflowNodes.find((workflowNode) => workflowNode.name === name);

const EDIT_CASES: EditCaseI[] = [
    {
        expectedNodeTypes: [/^slack\/v1\//, /^wait\/v1\/afterTimeInterval$/],
        instruction:
            'Update this workflow so failed orders are sent to Slack, then retry the API request after 10 minutes.',
        name: 'n8n sample: failed orders go to Slack and the request is retried after 10 minutes',
        seed: editOrderSync,
        unchangedNodes: {httpClient_1: /^httpClient\/v1\/post$/, trigger_1: /^webhook\/v1\//},
    },
    {
        expectedNodeTypes: [
            /^dateHelper\/v1\/getCurrentDate$/,
            /^aiAgentUtils\/v1\/approvalGate$/,
            /^slack\/v1\/sendChannelMessage$/,
        ],
        instruction:
            'Give the agent a tool that tells the current date, and let it post messages to the #ops Slack channel, ' +
            'but only after a human approves each post in the chat.',
        name: 'AI agent: add a tool and an approval-gated Slack post',
        seed: editSupportAgent,
        unchangedNodes: {
            aiAgent_1: /^aiAgent\/v1\/chat$/,
            anthropic_1: /^anthropic\/v1\/model$/,
            chatMemory_1: /^chatMemory\/v2\/chatMemory$/,
        },
    },
    {
        checkNodes: (workflowNodes) => {
            expect(findNode(workflowNodes, 'trigger_1')?.parameters).toMatchObject({hour: 7, minute: 30});

            expect(JSON.stringify(findNode(workflowNodes, 'searchEmail_1')?.parameters)).toMatch(
                /has:attachment|filename:pdf/
            );

            expect(workflowNodes, 'no step was added for a parameter change').toHaveLength(2);
        },
        expectedNodeTypes: [],
        instruction:
            'Change the schedule to run at 7:30 instead of 8:00, and only find emails that have a PDF attachment.',
        name: 'Parameters: change the schedule time and narrow the email search',
        seed: editInvoiceSchedule,
        unchangedNodes: {searchEmail_1: /^googleMail\/v1\/searchEmail$/, trigger_1: /^schedule\/v1\/everyDay$/},
    },
    {
        expectedNodeTypes: [],
        instruction: 'Remove the logging step; keep posting the signup to Slack.',
        name: 'Remove a step and keep the rest',
        removedNodeNames: ['logger_1'],
        seed: editRemoveStep,
        unchangedNodes: {sendChannelMessage_1: /^slack\/v1\/sendChannelMessage$/, trigger_1: /^webhook\/v1\//},
    },
    {
        expectedNodeTypes: [/^(script\/v1\/(javaScript|python|ruby)|dataStreamProcessor\/v1\/\w+)$/],
        instruction: 'Add a processor to the data stream that converts every contact name to upper case.',
        name: 'Data stream: add a processor between the reader and the writer',
        seed: editDataStream,
        unchangedNodes: {
            reader_1: /^csvFile\/v1\/reader$/,
            stream_1: /^dataStream\/v1\/stream$/,
            writer_1: /^jsonFile\/v1\/writer$/,
        },
    },
];

interface SeededWorkflowI {
    projectId: number;
    projectName: string;
    projectWorkflowId: number;
    workflowId: string;
}

async function listProjectWorkflows(page: Page, projectId: number) {
    const response = await page.request.get(`${PROJECTS_API_PATH}/${projectId}/workflows`);

    expect(response.ok(), `listing the workflows of project ${projectId}`).toBe(true);

    return (await response.json()) as Array<{id: string; projectWorkflowId: number}>;
}

async function seedWorkflow(page: Page, workspaceId: number, seed: object): Promise<SeededWorkflowI> {
    const projectName = `edit_sample_${getRandomString().slice(0, 8)}`;
    const projectId = await createProjectThroughApi(page, {name: projectName, workspaceId});

    await createProjectWorkflowThroughApi(page, {definition: seed, projectId});

    const [projectWorkflow] = await listProjectWorkflows(page, projectId);

    return {
        projectId,
        projectName,
        projectWorkflowId: projectWorkflow.projectWorkflowId,
        workflowId: projectWorkflow.id,
    };
}

async function expectEdited(page: Page, editCase: EditCaseI, seededWorkflow: SeededWorkflowI): Promise<void> {
    const projectWorkflows = await listProjectWorkflows(page, seededWorkflow.projectId);

    expect(projectWorkflows, 'the edit updated the workflow instead of adding another').toHaveLength(1);

    const workflowNodes = await readWorkflowNodes(page, projectWorkflows[0].id);
    const nodeTypes = workflowNodes.map((workflowNode) => workflowNode.type);

    for (const [nodeName, typePattern] of Object.entries(editCase.unchangedNodes)) {
        expect(findNode(workflowNodes, nodeName)?.type, `${nodeName} is kept`).toMatch(typePattern);
    }

    for (const expectedNodeType of editCase.expectedNodeTypes) {
        expect(nodeTypes, `a node matching ${expectedNodeType}`).toContainEqual(
            expect.stringMatching(expectedNodeType)
        );
    }

    for (const removedNodeName of editCase.removedNodeNames ?? []) {
        expect(findNode(workflowNodes, removedNodeName), `${removedNodeName} is removed`).toBeUndefined();
    }

    editCase.checkNodes?.(workflowNodes);
}

async function openSeededWorkspace(page: Page): Promise<number> {
    await hideReactQueryDevtools(page);

    const hubWorkspaceId = waitForHubWorkspaceId(page);

    await new AiHubPage(page).goto();

    return hubWorkspaceId;
}

async function createAdminApiKey(page: Page, name: string): Promise<string> {
    const data = await postGraphQl<{createApiKey: string}>(
        page,
        'mutation createApiKey($name: String!, $environmentId: ID!) { createApiKey(name: $name, environmentId: $environmentId) }',
        {environmentId: DEVELOPMENT_ENVIRONMENT_ID, name}
    );

    return data.createApiKey;
}

async function deleteAdminApiKey(page: Page, name: string): Promise<void> {
    const data = await postGraphQl<{adminApiKeys: Array<{id: string; name: string}>}>(
        page,
        'query adminApiKeys($environmentId: ID!) { adminApiKeys(environmentId: $environmentId) { id name } }',
        {environmentId: DEVELOPMENT_ENVIRONMENT_ID}
    );

    for (const apiKey of data.adminApiKeys.filter((candidate) => candidate.name === name)) {
        await postGraphQl(page, 'mutation deleteApiKey($id: ID!) { deleteApiKey(id: $id) }', {id: apiKey.id});
    }
}

async function readManagementMcpSecretKey(page: Page): Promise<string> {
    const data = await postGraphQl<{managementMcpServerUrl: string}>(page, 'query { managementMcpServerUrl }', {});

    return /\/api\/management\/([^/]+)\/mcp/.exec(data.managementMcpServerUrl)?.[1] ?? '';
}

test.describe('@live Builder edits of existing workflows', () => {
    test.skip(!process.env.PLAYWRIGHT_LIVE, 'Set PLAYWRIGHT_LIVE=1 to run against a live server and model');

    test.describe.configure({timeout: 1200000});

    for (const editCase of EDIT_CASES) {
        test(`AI Hub - ${editCase.name}`, async ({authenticatedPage: page}) => {
            const workspaceId = await openSeededWorkspace(page);
            const seededWorkflow = await seedWorkflow(page, workspaceId, editCase.seed);

            try {
                const aiHubPage = new AiHubPage(page);

                await aiHubPage.goto();
                await aiHubPage.setBuildMode(true);

                await aiHubPage.sendAndWaitForTurnEnd(
                    `In the project "${seededWorkflow.projectName}", update the existing workflow ` +
                        `"${editCase.seed.label}". ${editCase.instruction}`,
                    LIVE_TURN_TIMEOUT
                );

                await answerInteractiveCards(page, {
                    readLastMessage: () => aiHubPage.assistantMessages.last(),
                    waitForTurnEnd: () => aiHubPage.waitForTurnEnd(LIVE_TURN_TIMEOUT),
                });

                await expect(aiHubPage.runErrors).toHaveCount(0);

                await expectEdited(page, editCase, seededWorkflow);
            } finally {
                await deleteProjectThroughApi(page, seededWorkflow.projectId);
            }
        });

        test(`Copilot - ${editCase.name}`, async ({authenticatedPage: page}) => {
            const workspaceId = await openSeededWorkspace(page);
            const seededWorkflow = await seedWorkflow(page, workspaceId, editCase.seed);

            try {
                await enableCopilotInApplicationInfo(page);

                await new WorkflowPage(page).goToWorkflowEditor(
                    String(seededWorkflow.projectId),
                    String(seededWorkflow.projectWorkflowId)
                );

                const copilotPanel = new CopilotPanel(page);

                await copilotPanel.openFromWorkflowEditor();
                await copilotPanel.setBuildMode(true);

                const waitForCopilotTurnEnd = async () => {
                    await expect(copilotPanel.stopGeneratingButton)
                        .toBeVisible({timeout: TURN_START_TIMEOUT})
                        .catch(() => undefined);

                    await expect(copilotPanel.stopGeneratingButton).toBeHidden({timeout: LIVE_TURN_TIMEOUT});

                    await expect(copilotPanel.sendButton).toBeVisible();
                };

                await copilotPanel.send(editCase.instruction);

                await waitForCopilotTurnEnd();

                await answerInteractiveCards(page, {
                    readLastMessage: () => copilotPanel.assistantMessages.last(),
                    waitForTurnEnd: waitForCopilotTurnEnd,
                });

                await expectEdited(page, editCase, seededWorkflow);
            } finally {
                await deleteProjectThroughApi(page, seededWorkflow.projectId);
            }
        });

        test(`Management MCP - ${editCase.name}`, async ({authenticatedPage: page, request}) => {
            const workspaceId = await openSeededWorkspace(page);
            const seededWorkflow = await seedWorkflow(page, workspaceId, editCase.seed);
            const apiKeyName = `mcp_edit_key_${getRandomString().slice(0, 8)}`;

            try {
                const mcpClient = new McpClient(
                    request,
                    process.env.PLAYWRIGHT_MCP_SECRET_KEY || (await readManagementMcpSecretKey(page)),
                    process.env.PLAYWRIGHT_MCP_BEARER_TOKEN || (await createAdminApiKey(page, apiKeyName))
                );

                await mcpClient.initialize();

                const buildResult = await buildWorkflowAnsweringQuestions(mcpClient, {
                    request: `Update the existing workflow with workflowId ${seededWorkflow.workflowId}. ${editCase.instruction}`,
                    workspaceId,
                });

                await test.info().attach('buildWorkflow reply', {body: toolResultText(buildResult)});

                expect(buildResult.isError ?? false, toolResultText(buildResult)).toBe(false);

                await expectEdited(page, editCase, seededWorkflow);
            } finally {
                await deleteProjectThroughApi(page, seededWorkflow.projectId);

                await deleteAdminApiKey(page, apiKeyName);
            }
        });
    }
});
