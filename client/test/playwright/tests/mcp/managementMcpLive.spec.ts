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

interface WorkflowInfoI {
    definition: string;
    id: string;
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
    test.skip(
        !!process.env.PLAYWRIGHT_COPILOT_DISABLED,
        'These need buildWorkflow and importWorkflow, which the Copilot provides'
    );

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

    const createProject = async () => {
        const workspaces = readStructuredResult<Array<{id: number}>>(await mcpClient.callTool('listWorkspaces', {}));

        expect(workspaces.length).toBeGreaterThan(0);

        const projectInfo = readStructuredResult<ProjectInfoI>(
            await mcpClient.callTool('createProject', {
                name: `mcp_project_${getRandomString()}`,
                workspaceId: workspaces[0].id,
            })
        );

        projectIds.push(projectInfo.id);

        return {projectId: projectInfo.id, workspaceId: workspaces[0].id};
    };

    const getOnlyWorkflow = async (projectId: number) => {
        const workflowInfos = readStructuredResult<WorkflowInfoI[]>(
            await mcpClient.callTool('listWorkflows', {projectId})
        );

        expect(workflowInfos).toHaveLength(1);

        return workflowInfos[0];
    };

    test('tools/list offers one authoring path per domain and no near-duplicate tools', async () => {
        const tools = await mcpClient.listTools();

        const toolNames = tools.map((tool) => tool.name);

        expect(toolNames).toEqual(
            expect.arrayContaining([
                'buildWorkflow',
                'configureMcpServer',
                'getAiSkill',
                'getAiSkillFileContent',
                'getAiSkillFilePaths',
                'getAiSkills',
                'getProject',
                'importWorkflow',
                'listMcpServers',
                'listProjects',
                'listWorkflows',
                'searchWorkflows',
            ])
        );

        expect(
            toolNames.filter((toolName) =>
                [
                    'cloneMcpProject',
                    'configureClusterElement',
                    'createAdditionalFilesInSkill',
                    'createAiSkill',
                    'createProjectWorkflow',
                    'getActionDefinition',
                    'getProjectStatus',
                    'getTaskProperties',
                    'listComponents',
                    'listMcpProjectWorkflows',
                    'listTaskDispatchers',
                    'removeFileInSkill',
                    'searchProjects',
                    'updateAiSkill',
                    'updateAiSkillContent',
                    'updateClusterElementTask',
                    'updateScriptComponentCode',
                    'updateWorkflow',
                    'writeScript',
                ].includes(toolName)
            )
        ).toEqual([]);

        for (const listToolName of ['listProjects', 'listWorkflows']) {
            const listTool = tools.find((tool) => tool.name === listToolName);

            expect(Object.keys(listTool?.inputSchema?.properties ?? {})).toContain('query');
        }
    });

    test('createProject and buildWorkflow with the projectId produce a workflow with a trigger and a task', async () => {
        const {projectId, workspaceId} = await createProject();

        const buildResult = await mcpClient.callTool('buildWorkflow', {
            request:
                `Create a new workflow in the project with projectId ${projectId}: every day at 9:00, fetch ` +
                'https://example.com/status.json with an HTTP GET request. Use the Schedule and HTTP Client ' +
                'components. Do not ask questions; choose sensible defaults.',
            workspaceId,
        });

        expect(buildResult.isError ?? false, toolResultText(buildResult)).toBe(false);

        const workflowInfo = await getOnlyWorkflow(projectId);

        const workflowDefinition = JSON.parse(workflowInfo.definition) as WorkflowDefinitionI;

        expect(workflowDefinition.triggers?.length ?? 0).toBeGreaterThan(0);

        expect(workflowDefinition.tasks?.length ?? 0).toBeGreaterThan(0);
    });

    test('an ambiguous buildWorkflow returns a numbered question and completes once answered', async () => {
        const {projectId, workspaceId} = await createProject();

        const ambiguousRequest =
            `Create a new workflow in the project with projectId ${projectId} so that, when a new lead arrives through ` +
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

    test('importWorkflow with the projectId creates the workflow and never returns a question', async () => {
        const {projectId, workspaceId} = await createProject();

        const importResult = await mcpClient.callTool('importWorkflow', {
            projectId,
            request: `Import this n8n workflow:\n${JSON.stringify(SMALL_N8N_WORKFLOW)}`,
            workspaceId,
        });

        expect(importResult.isError ?? false, toolResultText(importResult)).toBe(false);

        expect(toolResultText(importResult)).not.toContain(QUESTION_MARKER);

        expect(toolResultText(importResult)).not.toMatch(/"error"/);

        const workflowInfo = await getOnlyWorkflow(projectId);

        const workflowDefinition = JSON.parse(workflowInfo.definition) as WorkflowDefinitionI;

        expect(workflowDefinition.triggers?.length ?? 0).toBeGreaterThan(0);
    });

    test('importWorkflow with the workflowId converts into the existing workflow without creating another', async () => {
        const {projectId, workspaceId} = await createProject();

        const buildResult = await mcpClient.callTool('buildWorkflow', {
            request:
                `Create a new workflow in the project with projectId ${projectId} with only a manual trigger and ` +
                'no tasks. Do not ask questions.',
            workspaceId,
        });

        expect(buildResult.isError ?? false, toolResultText(buildResult)).toBe(false);

        const existingWorkflow = await getOnlyWorkflow(projectId);

        const importResult = await mcpClient.callTool('importWorkflow', {
            request: `Import this n8n workflow:\n${JSON.stringify(SMALL_N8N_WORKFLOW)}`,
            workflowId: existingWorkflow.id,
            workspaceId,
        });

        expect(importResult.isError ?? false, toolResultText(importResult)).toBe(false);

        expect(toolResultText(importResult)).not.toContain(QUESTION_MARKER);

        const importedWorkflow = await getOnlyWorkflow(projectId);

        expect(importedWorkflow.id).toBe(existingWorkflow.id);

        const workflowDefinition = JSON.parse(importedWorkflow.definition) as WorkflowDefinitionI;

        expect(workflowDefinition.tasks?.length ?? 0).toBeGreaterThan(0);
    });

    test('importWorkflow with both a projectId and a workflowId is a tool error and creates nothing', async () => {
        const {projectId, workspaceId} = await createProject();

        const importResult = await mcpClient.callTool('importWorkflow', {
            projectId,
            request: `Import this n8n workflow:\n${JSON.stringify(SMALL_N8N_WORKFLOW)}`,
            workflowId: 'any-workflow-id',
            workspaceId,
        });

        expect(toolResultText(importResult)).toContain('not both');

        const workflowInfos = readStructuredResult<WorkflowInfoI[]>(
            await mcpClient.callTool('listWorkflows', {projectId})
        );

        expect(workflowInfos).toEqual([]);
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
