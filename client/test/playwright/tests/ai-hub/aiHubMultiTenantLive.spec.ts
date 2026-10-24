import {type APIRequestContext, type Browser, type Page, expect, test} from '@playwright/test';

import {AiHubPage} from '../../pages/aiHubPage';
import {CopilotPanel} from '../../pages/copilotPanel';
import {WorkflowPage} from '../../pages/workflowPage';
import {
    createDataTableThroughApi,
    createProjectThroughApi,
    createProjectWorkflowThroughApi,
    createSlackConnectionThroughApi,
    findProjectByName,
    listConnections,
    listWorkspaceProjects,
    postGraphQl,
    readWorkflowNodes,
} from '../../utils/aiHubUtils';
import {AUTOMATION_API_PATH, apiFetch, apiJson} from '../../utils/apiUtils';
import {newAnonymousPage} from '../../utils/approvalUtils';
import {enableCopilotInApplicationInfo, hideReactQueryDevtools} from '../../utils/copilotUtils';
import getRandomString from '../../utils/getRandomString';
import {login} from '../../utils/login';
import {
    McpClient,
    type McpToolCallResultI,
    buildWorkflowAnsweringQuestions,
    toolResultText,
} from '../../utils/mcpClient';
import {skipUnlessMultiTenant} from '../../utils/testEnvironment';

const LIVE_TURN_TIMEOUT = 840000;

const TURN_START_TIMEOUT = 15000;

const TENANT_PASSWORD = 'PlaywrightTenant!1';

const DEVELOPMENT_ENVIRONMENT_ID = 0;

const BUILD_INSTRUCTION =
    'every day at 9:00, post the message "hello" to the #general Slack channel. Use the Schedule ' +
    "component's daily trigger and the Slack component's send message action, and nothing else. Do not ask " +
    'questions; choose sensible defaults.';

const SLACK_TASK_TYPE = /^slack\/v1\//;

interface TenantI {
    name: string;
    page: Page;
    workspaceId: number;
}

interface TenantArtifactsI {
    chatMarker: string;
    connectionName: string;
    dataTableName: string;
    projectNames: string[];
    workflowIds: string[];
}

const shortName = (prefix: string) => `${prefix}_${getRandomString().slice(0, 8)}`;

/**
 * Signs up a fresh account, which on a multi-tenant server creates its own tenant, and logs it in.
 */
async function signUpTenant(browser: Browser, name: string): Promise<TenantI> {
    const page = await newAnonymousPage(browser);
    const email = `pw-hub-${name}-${getRandomString().slice(0, 8)}@example.com`;

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

    return {name, page, workspaceId: workspace.id};
}

async function readProjectWorkflows(page: Page, projectId: number) {
    return apiJson<Array<{id: string; projectWorkflowId: number}>>({
        method: 'GET',
        page,
        path: `${AUTOMATION_API_PATH}/projects/${projectId}/workflows`,
    });
}

async function expectSlackWorkflowInProject(tenant: TenantI, projectName: string): Promise<string> {
    const project = await findProjectByName(tenant.page, tenant.workspaceId, projectName);

    expect(project, `tenant ${tenant.name} sees the project ${projectName} it built`).toBeDefined();

    const [projectWorkflow] = await readProjectWorkflows(tenant.page, project!.id);

    expect(projectWorkflow, `project ${projectName} holds the built workflow`).toBeDefined();

    const workflowNodes = await readWorkflowNodes(tenant.page, projectWorkflow.id);

    expect(workflowNodes.map((workflowNode) => workflowNode.type)).toContainEqual(
        expect.stringMatching(SLACK_TASK_TYPE)
    );

    return projectWorkflow.id;
}

async function buildThroughHub(tenant: TenantI, projectName: string, chatMarker: string): Promise<string> {
    await hideReactQueryDevtools(tenant.page);

    const aiHubPage = new AiHubPage(tenant.page);

    await aiHubPage.goto();

    await aiHubPage.setBuildMode(true);

    await aiHubPage.sendAndWaitForTurnEnd(
        `Create a new project "${projectName}" with one workflow that, ${BUILD_INSTRUCTION} ` +
            `Name the workflow ${chatMarker}.`,
        LIVE_TURN_TIMEOUT
    );

    await expect(aiHubPage.runErrors).toHaveCount(0);

    return expectSlackWorkflowInProject(tenant, projectName);
}

async function buildThroughCopilot(tenant: TenantI, projectName: string): Promise<string> {
    const projectId = await createProjectThroughApi(tenant.page, {
        name: projectName,
        workspaceId: tenant.workspaceId,
    });

    await createProjectWorkflowThroughApi(tenant.page, {
        definition: {description: '', inputs: [], label: projectName, tasks: [], triggers: []},
        projectId,
    });

    const [projectWorkflow] = await readProjectWorkflows(tenant.page, projectId);

    await hideReactQueryDevtools(tenant.page);

    await enableCopilotInApplicationInfo(tenant.page);

    await new WorkflowPage(tenant.page).goToWorkflowEditor(
        String(projectId),
        String(projectWorkflow.projectWorkflowId)
    );

    const copilotPanel = new CopilotPanel(tenant.page);

    await copilotPanel.openFromWorkflowEditor();

    await copilotPanel.setBuildMode(true);

    await copilotPanel.send(`Build this workflow: ${BUILD_INSTRUCTION}`);

    await expect(copilotPanel.stopGeneratingButton)
        .toBeVisible({timeout: TURN_START_TIMEOUT})
        .catch(() => undefined);

    await expect(copilotPanel.stopGeneratingButton).toBeHidden({timeout: LIVE_TURN_TIMEOUT});

    return expectSlackWorkflowInProject(tenant, projectName);
}

async function readManagementMcpSecretKey(page: Page): Promise<string> {
    const data = await postGraphQl<{managementMcpServerUrl: string}>(page, 'query { managementMcpServerUrl }', {});
    const secretKey = /\/api\/management\/([^/]+)\/mcp/.exec(data.managementMcpServerUrl)?.[1];

    expect(secretKey, 'the management MCP server URL carries its secret key').toBeDefined();

    return secretKey!;
}

async function createAdminApiKey(page: Page, name: string): Promise<string> {
    const data = await postGraphQl<{createApiKey: string}>(
        page,
        'mutation createApiKey($name: String!, $environmentId: ID!) { createApiKey(name: $name, environmentId: $environmentId) }',
        {environmentId: String(DEVELOPMENT_ENVIRONMENT_ID), name}
    );

    return data.createApiKey;
}

function readStructuredResult<ResultType>(toolCallResult: McpToolCallResultI): ResultType {
    expect(toolCallResult.isError ?? false, toolResultText(toolCallResult)).toBe(false);

    return JSON.parse(toolResultText(toolCallResult)) as ResultType;
}

async function buildThroughMcp(
    tenant: TenantI,
    request: APIRequestContext,
    mcpCredentials: {bearerToken: string; secretKey: string},
    projectName: string
): Promise<string> {
    const mcpClient = new McpClient(request, mcpCredentials.secretKey, mcpCredentials.bearerToken);

    await mcpClient.initialize();

    const workspaces = readStructuredResult<Array<{id: number}>>(await mcpClient.callTool('listWorkspaces', {}));

    expect(workspaces.map((workspace) => workspace.id)).toContain(tenant.workspaceId);

    const mcpProject = readStructuredResult<{id: number}>(
        await mcpClient.callTool('createProject', {name: projectName, workspaceId: tenant.workspaceId})
    );

    const buildResult = await buildWorkflowAnsweringQuestions(mcpClient, {
        request: `Create a new workflow in project ${mcpProject.id} that, ${BUILD_INSTRUCTION}`,
        workspaceId: tenant.workspaceId,
    });

    await test.info().attach(`${tenant.name} buildWorkflow reply`, {body: toolResultText(buildResult)});

    expect(buildResult.isError ?? false, toolResultText(buildResult)).toBe(false);

    return expectSlackWorkflowInProject(tenant, projectName);
}

async function listMcpProjectNames(
    request: APIRequestContext,
    mcpCredentials: {bearerToken: string; secretKey: string},
    workspaceId: number
): Promise<string[]> {
    const mcpClient = new McpClient(request, mcpCredentials.secretKey, mcpCredentials.bearerToken);

    await mcpClient.initialize();

    const listResult = await mcpClient.callTool('listProjects', {workspaceId});

    expect(listResult.isError ?? false, toolResultText(listResult)).toBe(false);

    return (JSON.parse(toolResultText(listResult)) as Array<{name: string}>).map((project) => project.name);
}

async function readHubChatTexts(page: Page, workspaceId: number): Promise<string> {
    const data = await postGraphQl<{aiHubChats: Array<{id: string; title: string | null}>}>(
        page,
        'query aiHubChats($workspaceId: ID!, $environment: Int!) { aiHubChats(workspaceId: $workspaceId, environment: $environment) { id title } }',
        {environment: DEVELOPMENT_ENVIRONMENT_ID, workspaceId: String(workspaceId)}
    );

    const chatTexts: string[] = [];

    for (const chat of data.aiHubChats) {
        const messages = await postGraphQl<{aiHubChatMessages: Array<{content: string | null}>}>(
            page,
            'query aiHubChatMessages($workspaceId: ID!, $id: ID!) { aiHubChatMessages(workspaceId: $workspaceId, id: $id) { content } }',
            {id: chat.id, workspaceId: String(workspaceId)}
        );

        chatTexts.push(chat.title ?? '', ...messages.aiHubChatMessages.map((message) => message.content ?? ''));
    }

    return chatTexts.join('\n');
}

async function expectTenantSeesNothingOf(
    tenant: TenantI,
    otherTenant: TenantI,
    otherArtifacts: TenantArtifactsI,
    mcpCredentials: {bearerToken: string; secretKey: string},
    request: APIRequestContext
): Promise<void> {
    const projectNames = (await listWorkspaceProjects(tenant.page, tenant.workspaceId)).map((project) => project.name);

    for (const projectName of otherArtifacts.projectNames) {
        expect(projectNames, `tenant ${tenant.name} does not list ${otherTenant.name}'s project`).not.toContain(
            projectName
        );
    }

    for (const workflowId of otherArtifacts.workflowIds) {
        const workflowResponse = await tenant.page.request.get(`${AUTOMATION_API_PATH}/workflows/${workflowId}`);

        expect(
            workflowResponse.ok(),
            `tenant ${tenant.name} cannot read ${otherTenant.name}'s workflow ${workflowId}`
        ).toBe(false);
    }

    const connectionNames = (
        await listConnections(tenant.page, {
            componentName: 'slack',
            environmentId: DEVELOPMENT_ENVIRONMENT_ID,
            workspaceId: tenant.workspaceId,
        })
    ).map((connection) => connection.name);

    expect(connectionNames).not.toContain(otherArtifacts.connectionName);

    const dataTableResponse = await postGraphQl<{dataTables: Array<{baseName: string}>}>(
        tenant.page,
        'query dataTables($environmentId: ID!, $workspaceId: ID!) { dataTables(environmentId: $environmentId, workspaceId: $workspaceId) { baseName } }',
        {environmentId: String(DEVELOPMENT_ENVIRONMENT_ID), workspaceId: String(tenant.workspaceId)}
    );

    expect(dataTableResponse.dataTables.map((dataTable) => dataTable.baseName)).not.toContain(
        otherArtifacts.dataTableName
    );

    expect(await readHubChatTexts(tenant.page, tenant.workspaceId)).not.toContain(otherArtifacts.chatMarker);

    const mcpProjectNames = await listMcpProjectNames(request, mcpCredentials, tenant.workspaceId);

    for (const projectName of otherArtifacts.projectNames) {
        expect(mcpProjectNames).not.toContain(projectName);
    }
}

test.describe('@live AI Hub across tenants', () => {
    test.skip(!process.env.PLAYWRIGHT_LIVE, 'Set PLAYWRIGHT_LIVE=1 to run against a live server and model');

    test.describe.configure({timeout: 2400000});

    test.beforeEach(() => {
        skipUnlessMultiTenant();
    });

    test('what one tenant builds through the hub, the Copilot and the MCP stays invisible to another', async ({
        browser,
        request,
    }) => {
        const tenantA = await signUpTenant(browser, 'a');
        const tenantB = await signUpTenant(browser, 'b');

        const mcpCredentials = new Map<TenantI, {bearerToken: string; secretKey: string}>();

        for (const tenant of [tenantA, tenantB]) {
            mcpCredentials.set(tenant, {
                bearerToken: await createAdminApiKey(tenant.page, shortName(`mcp_${tenant.name}`)),
                secretKey: await readManagementMcpSecretKey(tenant.page),
            });
        }

        const artifactsA: TenantArtifactsI = {
            chatMarker: shortName('tenant_a_marker'),
            connectionName: shortName('tenant_a_slack'),
            dataTableName: shortName('tenant_a_table'),
            projectNames: [],
            workflowIds: [],
        };

        await test.step('Tenant A creates a connection and a data table', async () => {
            await createSlackConnectionThroughApi(tenantA.page, {
                environmentId: DEVELOPMENT_ENVIRONMENT_ID,
                name: artifactsA.connectionName,
                workspaceId: tenantA.workspaceId,
            });

            await createDataTableThroughApi(tenantA.page, {
                baseName: artifactsA.dataTableName,
                columns: [{name: 'invoice', type: 'STRING'}],
                environmentId: DEVELOPMENT_ENVIRONMENT_ID,
                workspaceId: tenantA.workspaceId,
            });
        });

        await test.step('Tenant A builds through the hub', async () => {
            const projectName = shortName('tenant_a_hub');

            artifactsA.workflowIds.push(await buildThroughHub(tenantA, projectName, artifactsA.chatMarker));
            artifactsA.projectNames.push(projectName);
        });

        await test.step('Tenant A builds through the Copilot', async () => {
            const projectName = shortName('tenant_a_copilot');

            artifactsA.workflowIds.push(await buildThroughCopilot(tenantA, projectName));
            artifactsA.projectNames.push(projectName);
        });

        await test.step('Tenant A builds through the management MCP', async () => {
            const projectName = shortName('tenant_a_mcp');

            artifactsA.workflowIds.push(
                await buildThroughMcp(tenantA, request, mcpCredentials.get(tenantA)!, projectName)
            );
            artifactsA.projectNames.push(projectName);
        });

        await test.step("Tenant B sees none of tenant A's work", async () => {
            await expectTenantSeesNothingOf(tenantB, tenantA, artifactsA, mcpCredentials.get(tenantB)!, request);
        });

        await test.step("Tenant B's hub cannot find tenant A's work either", async () => {
            const aiHubPage = new AiHubPage(tenantB.page);

            await hideReactQueryDevtools(tenantB.page);

            await aiHubPage.goto();

            await aiHubPage.sendAndWaitForTurnEnd(
                'List the names of all my projects, connections and data tables.',
                LIVE_TURN_TIMEOUT
            );

            const answer = await aiHubPage.assistantMessages.last().innerText();

            for (const artifactName of [
                ...artifactsA.projectNames,
                artifactsA.connectionName,
                artifactsA.dataTableName,
            ]) {
                expect(answer, `tenant B's hub answer does not name ${artifactName}`).not.toContain(artifactName);
            }
        });

        await test.step("Each tenant's MCP key opens only its own tenant", async () => {
            const crossedCredentials = [
                {
                    bearerToken: mcpCredentials.get(tenantB)!.bearerToken,
                    secretKey: mcpCredentials.get(tenantA)!.secretKey,
                },
                {
                    bearerToken: mcpCredentials.get(tenantA)!.bearerToken,
                    secretKey: mcpCredentials.get(tenantB)!.secretKey,
                },
            ];

            for (const credentials of crossedCredentials) {
                const projectNames = await listMcpProjectNames(request, credentials, tenantA.workspaceId).catch(
                    () => undefined
                );

                if (projectNames === undefined) {
                    continue;
                }

                const bearerOwnerIsB = credentials.bearerToken === mcpCredentials.get(tenantB)!.bearerToken;

                if (bearerOwnerIsB) {
                    for (const projectName of artifactsA.projectNames) {
                        expect(projectNames, "tenant B's key never lists tenant A's projects").not.toContain(
                            projectName
                        );
                    }
                }
            }
        });
    });
});
