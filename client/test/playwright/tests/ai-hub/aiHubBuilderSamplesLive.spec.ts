import {type Page, expect, mergeTests} from '@playwright/test';

import {loginTest, projectTest, workflowTest} from '../../fixtures';
import {AiHubPage} from '../../pages/aiHubPage';
import {CopilotPanel} from '../../pages/copilotPanel';
import {WorkflowPage} from '../../pages/workflowPage';
import {
    type WorkflowNodeI,
    answerInteractiveCards,
    deleteProjectThroughApi,
    findProjectByName,
    listProjectWorkflowIds,
    postGraphQl,
    readHubWorkflow,
    readWorkflowNodes,
    waitForHubWorkspaceId,
} from '../../utils/aiHubUtils';
import {enableCopilotInApplicationInfo, hideReactQueryDevtools} from '../../utils/copilotUtils';
import getRandomString from '../../utils/getRandomString';
import {McpClient, type McpToolCallResultI, toolResultText} from '../../utils/mcpClient';

const test = mergeTests(loginTest(), projectTest, workflowTest);

const MCP_SECRET_KEY = process.env.PLAYWRIGHT_MCP_SECRET_KEY ?? '';

async function readManagementMcpSecretKey(page: Page): Promise<string> {
    const data = await postGraphQl<{managementMcpServerUrl: string}>(page, 'query { managementMcpServerUrl }', {});
    const secretKey = /\/api\/management\/([^/]+)\/mcp/.exec(data.managementMcpServerUrl)?.[1];

    expect(secretKey, 'the management MCP server URL carries its secret key').toBeDefined();

    return secretKey!;
}

const DEVELOPMENT_ENVIRONMENT_ID = '0';

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

function readStructuredResult<ResultType>(toolCallResult: McpToolCallResultI): ResultType {
    expect(toolCallResult.isError ?? false, toolResultText(toolCallResult)).toBe(false);

    return (toolCallResult.structuredContent ?? JSON.parse(toolResultText(toolCallResult))) as ResultType;
}

const LIVE_TURN_TIMEOUT = 840000;

const TURN_START_TIMEOUT = 15000;

const NEW_WORKFLOW_ANSWER = /new workflow/i;

const NOT_YET_ANSWER = /^(not yet|no\b|review)/i;

interface SampleI {
    /** Extra checks on the built nodes. */
    checkNodes?: (workflowNodes: WorkflowNodeI[]) => void;
    /** Every pattern must match the type of at least one node of the built workflow. */
    expectedNodeTypes: RegExp[];
    /** No node type may match these patterns. */
    forbiddenNodeTypes?: RegExp[];
    /** What to build, as one or more full sentences. */
    instruction: string;
    name: string;
}

const findNode = (workflowNodes: WorkflowNodeI[], typePattern: RegExp) =>
    workflowNodes.find((workflowNode) => typePattern.test(workflowNode.type));

const SAMPLES: SampleI[] = [
    {
        checkNodes: (workflowNodes) => {
            const searchEmail = findNode(workflowNodes, /^googleMail\/v1\/searchEmail$/)!;
            const filtersPdf =
                /filename:pdf/i.test(JSON.stringify(searchEmail.parameters)) ||
                workflowNodes.some(
                    (workflowNode) =>
                        /^condition\/v1/.test(workflowNode.type) && /pdf/i.test(JSON.stringify(workflowNode.parameters))
                );

            expect(filtersPdf, 'only PDF attachments are saved').toBe(true);
        },
        expectedNodeTypes: [
            /^schedule\/v1\//,
            /^googleMail\/v1\/searchEmail$/,
            /^googleDrive\/v1\/uploadFile$/,
            /^dataTable\/v1\/createRecords$/,
        ],
        instruction:
            'A workflow that checks Gmail every morning for invoices, ' +
            'saves PDF attachments to Google Drive, and adds a row to a Data Table. Ask me before publishing the ' +
            'workflow.',
        name: 'n8n sample: Gmail invoices to Google Drive and a data table',
    },
    {
        checkNodes: (workflowNodes) => {
            const additionTool = findNode(workflowNodes, /^mathHelper\/v1\//)!;

            expect(JSON.stringify(additionTool.parameters), 'the agent fills the tool inputs').toContain('fromAi(');
        },
        expectedNodeTypes: [
            /^chat\/v1\/newChatRequest$/,
            /^aiAgent\/v1\//,
            /^anthropic\/v1\/model$/,
            /^(chatMemory|inMemoryChatMemory)\/v\d+\/chatMemory$/,
            /^dateHelper\/v1\/getCurrentDate$/,
            /^mathHelper\/v1\//,
            /^pii\/v1\/piiCheck$/,
        ],
        forbiddenNodeTypes: [/^script\//],
        instruction:
            'A customer support chat agent. Users chat with ' +
            'it; it uses an Anthropic Claude model, remembers the conversation, can tell the current date and do ' +
            'arithmetic, and must block messages that contain personal data (PII) before they reach the model.',
        name: 'AI agent: model, memory, tools and a PII guardrail',
    },
    {
        expectedNodeTypes: [
            /^dataStream\/v1\/stream$/,
            /^csvFile\/v1\/reader$/,
            /^(script\/v1\/(javaScript|python|ruby)|dataStreamProcessor\/v1\/\w+)$/,
            /^jsonFile\/v1\/writer$/,
        ],
        instruction:
            'A workflow that uses a Data Stream: read contacts from a CSV ' +
            'file, map the columns first_name and last_name into a single full_name field and keep email, then ' +
            'write the result as a JSON file.',
        name: 'Data stream: CSV reader, field mapping, JSON writer',
    },
    {
        expectedNodeTypes: [
            /^aiAgent\/v1\//,
            /^aiAgentUtils\/v1\/approvalGate$/,
            /^slack\/v1\/sendChannelMessage$/,
            /^chat\/v1\/chat$/,
        ],
        instruction:
            'An AI agent on chat, using an Anthropic Claude ' +
            'model, that can post messages to the #ops Slack channel. Every Slack post must be approved by a human ' +
            'first; send the approval request in the chat.',
        name: 'AI agent: Slack posts behind an approval gate',
    },
    {
        expectedNodeTypes: [
            /^aiAgent\/v1\//,
            /^openAi\/v1\/model$/,
            /^(questionAnswerRag|modularRag)\/v1\/rag$/,
            /^knowledgeBase\/v1\/vectorStore$/,
        ],
        forbiddenNodeTypes: [/^pgVector\//, /^pinecone\//],
        instruction:
            'A chat agent using an OpenAI model that answers ' +
            'employee questions using retrieval (RAG) over our company knowledge base.',
        name: 'AI agent: RAG over the built-in knowledge base',
    },
    {
        expectedNodeTypes: [/^aiAgent\/v1\//, /^(aiAgent\/v1\/aiAgent|aiAgentUtils\/v1\/subagentTool)$/],
        instruction:
            'A chat agent using an Anthropic Claude model that ' +
            'delegates web research to a research subagent with its own model and returns a short summary to the user.',
        name: 'AI agent: a research subagent',
    },
    {
        expectedNodeTypes: [/^webhook\/v1\//, /^knowledgeBase\/v1\//, /^documentSplitter\/v1\//],
        forbiddenNodeTypes: [/^pgVector\//, /^pinecone\//],
        instruction:
            'When I upload a PDF through a webhook, split it ' +
            'into chunks, embed them with OpenAI and load them into our knowledge base.',
        name: 'Knowledge base: load an uploaded PDF',
    },
    {
        checkNodes: (workflowNodes) => {
            expect(
                workflowNodes.some((workflowNode) => JSON.stringify(workflowNode.parameters).includes('.approved}')),
                'the workflow branches on the approval outcome'
            ).toBe(true);
        },
        expectedNodeTypes: [
            /^webhook\/v1\//,
            /^approval\/v1\/requestApproval$/,
            /^slack\/v1\/slack$/,
            /^(email\/v1\/email|googleMail\/v1\/googleMail|microsoftOutlook365\/v1\/microsoftOutlook365)$/,
            /^dataTable\/v1\/createRecords$/,
        ],
        instruction:
            'A webhook receives an expense request (employee ' +
            'email, amount, description). If the amount is over 500, pause the workflow and ask a manager to approve ' +
            'it through Slack and email, with the request expiring after 2 days. If approved, add a row to an ' +
            '"expenses" data table; if rejected, email the employee with the manager comment. Expenses of 500 or ' +
            'less are added to the table directly.',
        name: 'Approval component: a manager approves large expenses through Slack and email',
    },
];

/** The nodes of the one workflow a build created in the project; a second, stray workflow fails the sample. */
async function readOnlyProjectWorkflowNodes(page: Page, projectId: number): Promise<WorkflowNodeI[]> {
    const workflowIds = await listProjectWorkflowIds(page, projectId);

    expect(workflowIds, `project ${projectId} holds exactly the one workflow that was built`).toHaveLength(1);

    return readWorkflowNodes(page, workflowIds[0]);
}

async function expectSampleBuilt(sample: SampleI, workflowNodes: WorkflowNodeI[]): Promise<void> {
    const nodeTypes = workflowNodes.map((workflowNode) => workflowNode.type);

    for (const expectedNodeType of sample.expectedNodeTypes) {
        expect(nodeTypes, `a node matching ${expectedNodeType}`).toContainEqual(
            expect.stringMatching(expectedNodeType)
        );
    }

    for (const forbiddenNodeType of sample.forbiddenNodeTypes ?? []) {
        expect(nodeTypes, `no node matching ${forbiddenNodeType}`).not.toContainEqual(
            expect.stringMatching(forbiddenNodeType)
        );
    }

    sample.checkNodes?.(workflowNodes);
}

/** A new workflow over updating an existing one, "not yet" to a publish question, otherwise the first option. */
const chooseSampleAnswer = (cardText: string, optionNames: string[]) =>
    optionNames.find((optionName) => NEW_WORKFLOW_ANSWER.test(optionName)) ??
    optionNames.find((optionName) => NOT_YET_ANSWER.test(optionName)) ??
    optionNames[0];

const shortName = (prefix: string) => `${prefix}_${getRandomString().slice(0, 8)}`;

test.describe('@live Builder samples', () => {
    test.skip(!process.env.PLAYWRIGHT_LIVE, 'Set PLAYWRIGHT_LIVE=1 to run against a live server and model');

    test.describe.configure({timeout: 1200000});

    for (const sample of SAMPLES) {
        test(`AI Hub - ${sample.name}`, async ({authenticatedPage: page}) => {
            await hideReactQueryDevtools(page);

            const aiHubPage = new AiHubPage(page);
            const hubWorkspaceId = waitForHubWorkspaceId(page);

            await aiHubPage.goto();

            const workspaceId = await hubWorkspaceId;
            const projectName = shortName('hub_sample');

            try {
                await aiHubPage.setBuildMode(true);

                await aiHubPage.sendAndWaitForTurnEnd(
                    `Create a new project "${projectName}" with this workflow. ${sample.instruction}`,
                    LIVE_TURN_TIMEOUT
                );

                await answerInteractiveCards(page, {
                    chooseOption: chooseSampleAnswer,
                    readLastMessage: () => aiHubPage.assistantMessages.last(),
                    waitForTurnEnd: () => aiHubPage.waitForTurnEnd(LIVE_TURN_TIMEOUT),
                });

                await expect(aiHubPage.runErrors).toHaveCount(0);

                const project = await findProjectByName(page, workspaceId, projectName);

                expect(project, `project ${projectName} was created`).toBeDefined();

                await expectSampleBuilt(sample, await readOnlyProjectWorkflowNodes(page, project!.id));
            } finally {
                const project = await findProjectByName(page, workspaceId, projectName);

                if (project) {
                    await deleteProjectThroughApi(page, project.id);
                }
            }
        });

        test(`Copilot - ${sample.name}`, async ({authenticatedPage: page, workflow}) => {
            await hideReactQueryDevtools(page);

            await enableCopilotInApplicationInfo(page);

            await new WorkflowPage(page).goToWorkflowEditor(workflow.projectId, workflow.workflowId);

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

            await copilotPanel.send(`Build this workflow. ${sample.instruction}`);

            await expect(copilotPanel.stopGeneratingButton).toBeVisible({timeout: 60000});

            await waitForCopilotTurnEnd();

            await answerInteractiveCards(page, {
                chooseOption: chooseSampleAnswer,
                readLastMessage: () => copilotPanel.assistantMessages.last(),
                waitForTurnEnd: waitForCopilotTurnEnd,
            });

            const copilotWorkflow = await readHubWorkflow(page, {
                projectId: workflow.projectId,
                projectWorkflowId: Number(workflow.workflowId),
            });

            await expectSampleBuilt(sample, await readWorkflowNodes(page, copilotWorkflow.workflowId));
        });

        test(`Management MCP - ${sample.name}`, async ({authenticatedPage: page, request}) => {
            const apiKeyName = shortName('mcp_sample_key');
            const bearerToken = process.env.PLAYWRIGHT_MCP_BEARER_TOKEN || (await createAdminApiKey(page, apiKeyName));
            const mcpClient = new McpClient(
                request,
                MCP_SECRET_KEY || (await readManagementMcpSecretKey(page)),
                bearerToken
            );
            let projectId: number | undefined;

            try {
                await mcpClient.initialize();

                const workspaces = readStructuredResult<Array<{id: number}>>(
                    await mcpClient.callTool('listWorkspaces', {})
                );

                const mcpProject = readStructuredResult<{id: number}>(
                    await mcpClient.callTool('createProject', {
                        name: shortName('mcp_sample'),
                        workspaceId: workspaces[0].id,
                    })
                );

                projectId = mcpProject.id;

                const buildResult = await mcpClient.callTool('buildWorkflow', {
                    request: `Create a new workflow in project ${mcpProject.id}. ${sample.instruction}`,
                    workspaceId: workspaces[0].id,
                });

                expect(buildResult.isError ?? false, toolResultText(buildResult)).toBe(false);

                await expectSampleBuilt(sample, await readOnlyProjectWorkflowNodes(page, mcpProject.id));
            } finally {
                if (projectId !== undefined) {
                    await deleteProjectThroughApi(page, projectId);
                }

                await deleteAdminApiKey(page, apiKeyName);
            }
        });
    }
});
