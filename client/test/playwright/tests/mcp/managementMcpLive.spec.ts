import {expect, test} from '@playwright/test';

import getRandomString from '../../utils/getRandomString';
import {McpClient, type McpToolCallResultI, toolResultText} from '../../utils/mcpClient';

const MCP_SECRET_KEY = process.env.PLAYWRIGHT_MCP_SECRET_KEY ?? '';

const SMALL_N8N_WORKFLOW = {
    connections: {
        'When clicking Execute workflow': {main: [[{index: 0, node: 'Edit Fields', type: 'main'}]]},
    },
    name: 'Set a greeting',
    nodes: [
        {
            id: 'manual-trigger',
            name: 'When clicking Execute workflow',
            parameters: {},
            position: [0, 0],
            type: 'n8n-nodes-base.manualTrigger',
            typeVersion: 1,
        },
        {
            id: 'edit-fields',
            name: 'Edit Fields',
            parameters: {
                assignments: {assignments: [{id: 'greeting', name: 'greeting', type: 'string', value: 'Hello'}]},
            },
            position: [220, 0],
            type: 'n8n-nodes-base.set',
            typeVersion: 3.4,
        },
    ],
};

interface ProjectInfoI {
    id: number;
}

interface ProjectWorkflowInfoI {
    workflow_id: string;
}

interface WorkflowDefinitionI {
    tasks?: unknown[];
    triggers?: unknown[];
}

function readStructuredResult<ResultType>(toolCallResult: McpToolCallResultI): ResultType {
    expect(toolCallResult.isError ?? false, toolResultText(toolCallResult)).toBe(false);

    if (toolCallResult.structuredContent !== undefined) {
        return toolCallResult.structuredContent as ResultType;
    }

    return JSON.parse(toolResultText(toolCallResult));
}

const QUESTION_MARKER = 'needs a decision before continuing';

test.describe('@live Management MCP', () => {
    test.skip(!process.env.PLAYWRIGHT_LIVE, 'Set PLAYWRIGHT_LIVE=1 to run against a live server and model');
    test.skip(!MCP_SECRET_KEY, 'Set PLAYWRIGHT_MCP_SECRET_KEY to the management MCP secret key');

    test.describe.configure({timeout: 600000});

    let mcpClient: McpClient;
    let projectIds: number[] = [];

    test.beforeEach(async ({request}) => {
        mcpClient = new McpClient(request, MCP_SECRET_KEY);

        projectIds = [];

        await mcpClient.initialize();
    });

    test.afterEach(async () => {
        for (const projectId of projectIds) {
            await mcpClient.callTool('deleteProject', {projectId}).catch((error) => {
                console.warn(`Failed to delete project ${projectId}:`, error);
            });
        }
    });

    const createProjectWorkflow = async () => {
        const workspaces = readStructuredResult<Array<{id: number}>>(await mcpClient.callTool('listWorkspaces', {}));

        expect(workspaces.length).toBeGreaterThan(0);

        const projectInfo = readStructuredResult<ProjectInfoI>(
            await mcpClient.callTool('createProject', {
                name: `mcp_project_${getRandomString()}`,
                workspaceId: workspaces[0].id,
            })
        );

        projectIds.push(projectInfo.id);

        const projectWorkflowInfo = readStructuredResult<ProjectWorkflowInfoI>(
            await mcpClient.callTool('createProjectWorkflow', {
                name: `mcp_workflow_${getRandomString()}`,
                projectId: projectInfo.id,
            })
        );

        return {projectId: projectInfo.id, workflowId: projectWorkflowInfo.workflow_id, workspaceId: workspaces[0].id};
    };

    test('tools/list offers buildWorkflow but no workflow-part tools, and createProjectWorkflow takes a name, not a definition', async () => {
        const tools = await mcpClient.listTools();

        const toolNames = tools.map((tool) => tool.name);

        expect(toolNames).not.toContain('updateWorkflow');

        expect(toolNames).toContain('buildWorkflow');

        expect(
            toolNames.filter((toolName) =>
                [
                    'configureClusterElement',
                    'getActionDefinition',
                    'getTaskProperties',
                    'listComponents',
                    'listTaskDispatchers',
                    'updateClusterElementTask',
                    'updateScriptComponentCode',
                    'writeScript',
                ].includes(toolName)
            )
        ).toEqual([]);

        const createProjectWorkflowTool = tools.find((tool) => tool.name === 'createProjectWorkflow');

        expect(createProjectWorkflowTool).toBeDefined();

        const inputProperties = Object.keys(createProjectWorkflowTool?.inputSchema?.properties ?? {});

        expect(inputProperties).toContain('name');

        expect(inputProperties).not.toContain('definition');
    });

    test('createProject, createProjectWorkflow and buildWorkflow produce a workflow with a trigger and a task', async () => {
        const {workflowId, workspaceId} = await createProjectWorkflow();

        const buildResult = await mcpClient.callTool('buildWorkflow', {
            request:
                `Update the existing workflow with workflowId ${workflowId}: every day at 9:00, fetch ` +
                'https://example.com/status.json with an HTTP GET request. Use the Schedule and HTTP Client ' +
                'components. Do not ask questions; choose sensible defaults.',
            workspaceId,
        });

        expect(buildResult.isError ?? false, toolResultText(buildResult)).toBe(false);

        const workflowInfo = readStructuredResult<{definition: string}>(
            await mcpClient.callTool('getWorkflow', {workflowId})
        );

        const workflowDefinition = JSON.parse(workflowInfo.definition) as WorkflowDefinitionI;

        expect(workflowDefinition.triggers?.length ?? 0).toBeGreaterThan(0);

        expect(workflowDefinition.tasks?.length ?? 0).toBeGreaterThan(0);
    });

    test('an ambiguous buildWorkflow returns a numbered question and completes once answered', async () => {
        const {workflowId, workspaceId} = await createProjectWorkflow();

        const ambiguousRequest =
            `Update the existing workflow with workflowId ${workflowId} so that, when a new lead arrives through ` +
            'a webhook trigger, it sends a notification to the #leads channel. Before building, ask me which ' +
            'notification service to use.';

        const questionText = toolResultText(
            await mcpClient.callTool('buildWorkflow', {request: ambiguousRequest, workspaceId})
        );

        expect(questionText).toContain(QUESTION_MARKER);

        expect(questionText).toMatch(/^1\. /m);

        expect(questionText).toContain('again, restating the original request');

        const answeredResult = await mcpClient.callTool('buildWorkflow', {
            request: `${ambiguousRequest} Answer to your question: use Slack. Do not ask again.`,
            workspaceId,
        });

        const answeredText = toolResultText(answeredResult);

        expect(answeredResult.isError ?? false, answeredText).toBe(false);

        expect(answeredText).not.toContain(QUESTION_MARKER);

        expect(answeredText).not.toMatch(/"error"/);
    });

    test('importWorkflow with a small n8n workflow never returns a question', async () => {
        const {workflowId, workspaceId} = await createProjectWorkflow();

        const importResult = await mcpClient.callTool('importWorkflow', {
            request: `Import this n8n workflow into the workflow with workflowId ${workflowId}:\n${JSON.stringify(SMALL_N8N_WORKFLOW)}`,
            workspaceId,
        });

        expect(importResult.isError ?? false, toolResultText(importResult)).toBe(false);

        expect(toolResultText(importResult)).not.toContain(QUESTION_MARKER);

        expect(toolResultText(importResult)).not.toMatch(/"error"/);
    });
});

test.describe('@live Management MCP with the Copilot disabled', () => {
    test.skip(!process.env.PLAYWRIGHT_LIVE, 'Set PLAYWRIGHT_LIVE=1 to run against a live server');
    test.skip(!MCP_SECRET_KEY, 'Set PLAYWRIGHT_MCP_SECRET_KEY to the management MCP secret key');
    test.skip(
        !process.env.PLAYWRIGHT_COPILOT_DISABLED,
        'Set PLAYWRIGHT_COPILOT_DISABLED=1 when the server runs with bytechef.ai.copilot.enabled=false'
    );

    test('management tools still answer and the Copilot-backed tools are absent', async ({request}) => {
        const mcpClient = new McpClient(request, MCP_SECRET_KEY);

        await mcpClient.initialize();

        const toolNames = (await mcpClient.listTools()).map((tool) => tool.name);

        expect(toolNames).toContain('listWorkspaces');

        expect(toolNames).toContain('createProject');

        expect(toolNames).not.toContain('buildWorkflow');

        expect(toolNames).not.toContain('importWorkflow');

        const listWorkspacesResult = await mcpClient.callTool('listWorkspaces', {});

        expect(listWorkspacesResult.isError ?? false, toolResultText(listWorkspacesResult)).toBe(false);
    });
});
