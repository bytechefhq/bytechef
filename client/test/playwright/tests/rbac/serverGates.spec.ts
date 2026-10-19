import {type APIRequestContext, expect} from '@playwright/test';
import {readFileSync} from 'fs';

import {workspaceRolesTest as test} from '../../fixtures/workspaceRoles';
import {SAMPLE_WORKFLOW_PATH} from '../../utils/constants';
import getRandomString from '../../utils/getRandomString';
import {
    type BuiltInRoleType,
    RBAC_BASE_URL,
    type RbacWorkspaceI,
    type RoleUserI,
    createAccount,
    createRoleUserApiContext,
    deleteRoleUser,
    graphql,
    internalApi,
    postGraphQl,
    sendInternalApi,
} from '../../utils/rbacUtils';

const INTERNAL = '/api/automation/internal';

const API_PLATFORM_INTERNAL = '/api/automation/api-platform/internal';

type CallerType = BuiltInRoleType | 'OUTSIDER';

interface ResourcesI {
    connectionId: number;
    dataTableId: string;
    jobId: number;
    projectDeploymentId: number;
    scratchProjectId: number;
}

interface GraphQlProbeI {
    query: string;
    variables: (workspace: RbacWorkspaceI, resources: ResourcesI) => Record<string, unknown>;
}

interface RestProbeI {
    body?: (workspace: RbacWorkspaceI, resources: ResourcesI) => unknown;
    method: 'DELETE' | 'GET' | 'PATCH' | 'POST' | 'PUT';
    path: (workspace: RbacWorkspaceI, resources: ResourcesI) => string;
}

interface WorkflowExecutionTaskExecutionI {
    id: string;
    input?: Record<string, unknown> | null;
    output?: unknown;
}

interface WorkflowExecutionJobI {
    outputs?: Record<string, unknown> | null;
    status: string;
    taskExecutions: WorkflowExecutionTaskExecutionI[];
}

interface GateCaseI {
    allowed?: CallerType[];
    denied: CallerType[];
    graphQl?: GraphQlProbeI;
    module: string;
    name: string;
    rest?: RestProbeI;
}

const GATE_CASES: GateCaseI[] = [
    {
        denied: ['VIEWER', 'EDITOR'],
        graphQl: {
            query: `mutation InviteWorkspaceUser($workspaceId: ID!, $email: String!) {
                inviteWorkspaceUser(workspaceId: $workspaceId, email: $email, role: VIEWER) { userId }
            }`,
            variables: (workspace) => ({
                email: `rbac_gate_${getRandomString().slice(0, 8)}@localhost.com`,
                workspaceId: String(workspace.id),
            }),
        },
        module: 'Workspace',
        name: 'invite a member (WORKSPACE_MEMBER_MANAGE)',
    },
    {
        allowed: ['VIEWER'],
        denied: ['OUTSIDER'],
        graphQl: {
            query: 'query WorkspaceUsers($workspaceId: ID!) { workspaceUsers(workspaceId: $workspaceId) { userId } }',
            variables: (workspace) => ({workspaceId: String(workspace.id)}),
        },
        module: 'Workspace',
        name: 'list members (WORKSPACE_VIEW)',
    },
    {
        denied: ['VIEWER', 'EDITOR'],
        module: 'Workspace',
        name: 'read the Git configuration (WORKSPACE_MANAGE)',
        rest: {
            method: 'GET',
            path: (workspace) => `/api/platform/internal/workspaces/${workspace.id}/git-configuration`,
        },
    },
    {
        denied: ['VIEWER'],
        module: 'Project',
        name: 'create a project (PROJECT_CREATE)',
        rest: {
            body: (workspace) => ({name: `rbac_gate_${getRandomString()}`, workspaceId: workspace.id}),
            method: 'POST',
            path: () => `${INTERNAL}/projects`,
        },
    },
    {
        denied: ['VIEWER', 'EDITOR'],
        module: 'Project',
        name: 'delete a project (PROJECT_DELETE)',
        rest: {method: 'DELETE', path: (workspace, resources) => `${INTERNAL}/projects/${resources.scratchProjectId}`},
    },
    {
        allowed: ['VIEWER'],
        denied: ['OUTSIDER'],
        module: 'Project',
        name: 'read a project (WORKFLOW_VIEW)',
        rest: {method: 'GET', path: (workspace) => `${INTERNAL}/projects/${workspace.projectId}`},
    },
    {
        denied: ['VIEWER'],
        module: 'Workflow',
        name: 'add a workflow to a project (WORKFLOW_CREATE)',
        rest: {
            body: () => ({definition: readFileSync(SAMPLE_WORKFLOW_PATH, 'utf-8')}),
            method: 'POST',
            path: (workspace) => `${INTERNAL}/projects/${workspace.projectId}/workflows`,
        },
    },
    {
        denied: ['VIEWER'],
        module: 'Workflow',
        name: 'delete a workflow (WORKFLOW_DELETE)',
        rest: {method: 'DELETE', path: (workspace) => `${INTERNAL}/workflows/${workspace.workflowId}`},
    },
    {
        allowed: ['VIEWER'],
        denied: ['OUTSIDER'],
        module: 'Workflow',
        name: 'read a workflow (WORKFLOW_VIEW)',
        rest: {method: 'GET', path: (workspace) => `${INTERNAL}/workflows/${workspace.workflowId}`},
    },
    {
        denied: ['VIEWER'],
        module: 'Deployment',
        name: 'create a deployment (DEPLOYMENT_CREATE)',
        rest: {
            body: (workspace) => ({
                enabled: false,
                environmentId: 0,
                name: `rbac_gate_${getRandomString()}`,
                projectDeploymentWorkflows: [],
                projectId: workspace.projectId,
                projectVersion: 1,
            }),
            method: 'POST',
            path: () => `${INTERNAL}/project-deployments`,
        },
    },
    {
        denied: ['VIEWER'],
        module: 'Deployment',
        name: 'disable a deployment (DEPLOYMENT_EDIT)',
        rest: {
            method: 'PATCH',
            path: (workspace, resources) =>
                `${INTERNAL}/project-deployments/${resources.projectDeploymentId}/enable/true`,
        },
    },
    {
        allowed: ['VIEWER'],
        denied: ['OUTSIDER'],
        module: 'Deployment',
        name: 'list deployments (DEPLOYMENT_VIEW)',
        rest: {
            method: 'GET',
            path: (workspace) => `${INTERNAL}/workspaces/${workspace.id}/project-deployments?environmentId=0`,
        },
    },
    {
        allowed: ['ADMIN'],
        denied: ['VIEWER', 'EDITOR'],
        graphQl: {
            query: 'mutation DeleteJobFileLogs($jobId: ID!) { deleteJobFileLogs(jobId: $jobId) }',
            variables: (workspace, resources) => ({jobId: String(resources.jobId)}),
        },
        module: 'Execution',
        name: 'delete the logs of a run (EXECUTION_DELETE)',
    },
    {
        allowed: ['VIEWER'],
        denied: ['OUTSIDER'],
        module: 'Execution',
        name: 'read a run (EXECUTION_VIEW)',
        rest: {method: 'GET', path: (workspace, resources) => `${INTERNAL}/workflow-executions/${resources.jobId}`},
    },
    {
        allowed: ['EDITOR'],
        denied: ['VIEWER', 'OUTSIDER'],
        graphQl: {
            query: 'query JobFileLogs($jobId: ID!) { jobFileLogs(jobId: $jobId) { totalElements } }',
            variables: (workspace, resources) => ({jobId: String(resources.jobId)}),
        },
        module: 'Execution',
        name: 'read the logs of a run (EXECUTION_DETAILS_VIEW)',
    },
    {
        denied: ['VIEWER'],
        module: 'Connection',
        name: 'create a connection (CONNECTION_CREATE)',
        rest: {
            body: (workspace) => ({
                componentName: 'httpClient',
                connectionVersion: 1,
                environmentId: 0,
                name: `rbac_gate_${getRandomString()}`,
                parameters: {},
                workspaceId: workspace.id,
            }),
            method: 'POST',
            path: () => `${INTERNAL}/connections`,
        },
    },
    {
        denied: ['VIEWER', 'EDITOR'],
        module: 'Connection',
        name: 'delete a connection (CONNECTION_DELETE)',
        rest: {method: 'DELETE', path: (workspace, resources) => `${INTERNAL}/connections/${resources.connectionId}`},
    },
    {
        allowed: ['VIEWER'],
        denied: ['OUTSIDER'],
        module: 'Connection',
        name: 'list connections (CONNECTION_VIEW)',
        rest: {
            method: 'GET',
            path: (workspace) => `${INTERNAL}/workspaces/${workspace.id}/connections?environmentId=0`,
        },
    },
    {
        denied: ['VIEWER'],
        graphQl: {
            query: 'mutation CreateDataTable($input: CreateDataTableInput!) { createDataTable(input: $input) }',
            variables: (workspace) => ({
                input: {
                    baseName: `rbac_gate_${Date.now()}`,
                    columns: [{name: 'c1', type: 'STRING'}],
                    environmentId: '0',
                    workspaceId: String(workspace.id),
                },
            }),
        },
        module: 'Data table',
        name: 'create a data table (DATA_TABLE_CREATE)',
    },
    {
        denied: ['VIEWER', 'EDITOR'],
        graphQl: {
            query: 'mutation DropDataTable($input: RemoveTableInput!) { dropDataTable(input: $input) }',
            variables: (workspace, resources) => ({input: {environmentId: '0', tableId: resources.dataTableId}}),
        },
        module: 'Data table',
        name: 'drop a data table (DATA_TABLE_DELETE)',
    },
    {
        allowed: ['VIEWER'],
        denied: ['OUTSIDER'],
        graphQl: {
            query: 'query DataTables($workspaceId: ID!) { dataTables(environmentId: "0", workspaceId: $workspaceId) { id } }',
            variables: (workspace) => ({workspaceId: String(workspace.id)}),
        },
        module: 'Data table',
        name: 'list data tables (DATA_TABLE_VIEW)',
    },
    {
        denied: ['VIEWER'],
        graphQl: {
            query: `mutation CreateKnowledgeBase($workspaceId: ID!) {
                createKnowledgeBase(knowledgeBase: {name: "rbac_gate"}, environmentId: "0", workspaceId: $workspaceId) { id }
            }`,
            variables: (workspace) => ({workspaceId: String(workspace.id)}),
        },
        module: 'Knowledge base',
        name: 'create a knowledge base (KNOWLEDGE_BASE_CREATE)',
    },
    {
        allowed: ['VIEWER'],
        denied: ['OUTSIDER'],
        graphQl: {
            query: 'query KnowledgeBases($workspaceId: ID!) { knowledgeBases(environmentId: "0", workspaceId: $workspaceId) { id } }',
            variables: (workspace) => ({workspaceId: String(workspace.id)}),
        },
        module: 'Knowledge base',
        name: 'list knowledge bases (KNOWLEDGE_BASE_VIEW)',
    },
    {
        denied: ['VIEWER'],
        graphQl: {
            query: `mutation CreateWorkspaceMcpServer($input: CreateWorkspaceMcpServerInput!) {
                createWorkspaceMcpServer(input: $input) { id }
            }`,
            variables: (workspace) => ({
                input: {
                    environmentId: '0',
                    name: `rbac_gate_${getRandomString()}`,
                    type: 'AUTOMATION',
                    workspaceId: String(workspace.id),
                },
            }),
        },
        module: 'MCP',
        name: 'create an MCP server (MCP_CREATE)',
    },
    {
        allowed: ['VIEWER'],
        denied: ['OUTSIDER'],
        graphQl: {
            query: 'query WorkspaceMcpServers($workspaceId: ID!) { workspaceMcpServers(workspaceId: $workspaceId) { id } }',
            variables: (workspace) => ({workspaceId: String(workspace.id)}),
        },
        module: 'MCP',
        name: 'list MCP servers (MCP_VIEW)',
    },
    {
        denied: ['VIEWER'],
        module: 'API Platform',
        name: 'create an API collection (API_PLATFORM_CREATE)',
        rest: {
            body: (workspace) => ({
                contextPath: `rbac${Date.now()}`,
                enabled: false,
                environmentId: 0,
                name: `rbac_gate_${getRandomString()}`,
                projectId: workspace.projectId,
                projectVersion: 1,
                workspaceId: workspace.id,
            }),
            method: 'POST',
            path: () => `${API_PLATFORM_INTERNAL}/api-collections`,
        },
    },
    {
        allowed: ['VIEWER'],
        denied: ['OUTSIDER'],
        module: 'API Platform',
        name: 'list API collections (API_PLATFORM_VIEW)',
        rest: {
            method: 'GET',
            path: (workspace) => `${API_PLATFORM_INTERNAL}/workspaces/${workspace.id}/api-collections?environmentId=0`,
        },
    },
    {
        allowed: ['EDITOR'],
        denied: ['VIEWER', 'OUTSIDER'],
        graphQl: {
            query: 'query WorkspaceApiKeys($workspaceId: ID!) { workspaceApiKeys(workspaceId: $workspaceId, environmentId: "0") { id } }',
            variables: (workspace) => ({workspaceId: String(workspace.id)}),
        },
        module: 'API key',
        name: 'list workspace API keys (API_KEY_VIEW)',
    },
    {
        denied: ['VIEWER'],
        graphQl: {
            query: `mutation CreateWorkspaceApiKey($workspaceId: ID!) {
                createWorkspaceApiKey(workspaceId: $workspaceId, name: "rbac_gate", environmentId: "0")
            }`,
            variables: (workspace) => ({workspaceId: String(workspace.id)}),
        },
        module: 'API key',
        name: 'create a workspace API key (API_KEY_CREATE)',
    },
];

const TENANT_ADMIN_CASES: Omit<GateCaseI, 'allowed' | 'denied' | 'module'>[] = [
    {
        name: 'create a workspace',
        rest: {
            body: () => ({name: `rbac_gate_${getRandomString()}`}),
            method: 'POST',
            path: () => `${INTERNAL}/workspaces`,
        },
    },
    {name: 'list API clients', rest: {method: 'GET', path: () => `${API_PLATFORM_INTERNAL}/api-clients`}},
    {
        graphQl: {query: 'query ApiConnectors { apiConnectors { id } }', variables: () => ({})},
        name: 'list API connectors',
    },
    {
        name: 'create a notification',
        rest: {
            body: () => ({events: [], name: 'rbac_gate', settings: {}, type: 'EMAIL'}),
            method: 'POST',
            path: () => '/api/platform/internal/notifications',
        },
    },
    {
        graphQl: {
            query: 'mutation CreateApprovalTask { createApprovalTask(approvalTask: {name: "rbac_gate"}) { id } }',
            variables: () => ({}),
        },
        name: 'create an approval task',
    },
    {name: 'list embedded connections', rest: {method: 'GET', path: () => '/api/embedded/internal/connections'}},
    {
        graphQl: {
            query: 'query EmbeddedMcpServers { embeddedMcpServers { id } }',
            variables: () => ({}),
        },
        name: 'list embedded MCP servers',
    },
    {name: 'list connected users', rest: {method: 'GET', path: () => '/api/embedded/internal/connected-users'}},
    {
        graphQl: {
            query: 'mutation CreateApiKey { createApiKey(name: "rbac_gate", environmentId: "0") }',
            variables: () => ({}),
        },
        name: 'issue a platform API key',
    },
];

async function expectGraphQlDenied(
    apiContext: APIRequestContext,
    probe: GraphQlProbeI,
    variables: Record<string, unknown>
) {
    const response = await postGraphQl(apiContext, probe.query, variables);

    expect(response.errors, JSON.stringify(response)).toBeDefined();
    expect(response.errors![0]).toMatchObject({
        extensions: {classification: 'FORBIDDEN', errorCode: 'ACCESS_DENIED'},
        message: 'Access denied',
    });
}

async function expectRestStatus(
    apiContext: APIRequestContext,
    probe: RestProbeI,
    workspace: RbacWorkspaceI,
    resources: ResourcesI,
    denied: boolean
) {
    const response = await sendInternalApi(
        apiContext,
        probe.method,
        probe.path(workspace, resources),
        probe.body?.(workspace, resources)
    );

    if (denied) {
        expect(response.status(), await response.text()).toBe(403);
        expect(await response.json()).toMatchObject({detail: 'Access denied', status: 403, title: 'Forbidden'});
    } else {
        expect(response.status(), await response.text()).toBeLessThan(300);
    }
}

async function createResources(adminApi: APIRequestContext, workspace: RbacWorkspaceI): Promise<ResourcesI> {
    const projectId = await internalApi<number>(adminApi, 'POST', '/projects', {
        name: `rbac_gate_deployed_${getRandomString()}`,
        workspaceId: workspace.id,
    });

    await internalApi(adminApi, 'POST', `/projects/${projectId}/workflows`, {
        definition: readFileSync(SAMPLE_WORKFLOW_PATH, 'utf-8'),
    });

    await internalApi(adminApi, 'POST', `/projects/${projectId}/publish`, {description: 'rbac gates'});

    const workflows = await internalApi<{id: string}[]>(adminApi, 'GET', `/projects/${projectId}/versions/1/workflows`);

    const projectDeploymentId = await internalApi<number>(adminApi, 'POST', '/project-deployments', {
        enabled: false,
        environmentId: 0,
        name: `rbac_gate_${getRandomString()}`,
        projectDeploymentWorkflows: workflows.map((workflow) => ({enabled: true, workflowId: workflow.id})),
        projectId,
        projectVersion: 1,
    });

    await internalApi(adminApi, 'PATCH', `/project-deployments/${projectDeploymentId}/enable/true`);

    const {jobId} = await internalApi<{jobId: number}>(
        adminApi,
        'POST',
        `/project-deployments/${projectDeploymentId}/workflows/${workflows[0].id}/jobs`
    );

    const scratchProjectId = await internalApi<number>(adminApi, 'POST', '/projects', {
        name: `rbac_gate_scratch_${getRandomString()}`,
        workspaceId: workspace.id,
    });

    const connectionId = await internalApi<number>(adminApi, 'POST', '/connections', {
        componentName: 'httpClient',
        connectionVersion: 1,
        environmentId: 0,
        name: `rbac_gate_${getRandomString()}`,
        parameters: {},
        workspaceId: workspace.id,
    });

    const baseName = `rbac_gate_${Date.now()}`;

    await graphql(
        adminApi,
        'mutation CreateDataTable($input: CreateDataTableInput!) { createDataTable(input: $input) }',
        {
            input: {
                baseName,
                columns: [{name: 'c1', type: 'STRING'}],
                environmentId: '0',
                workspaceId: String(workspace.id),
            },
        }
    );

    const {dataTables} = await graphql<{dataTables: {baseName: string; id: string}[]}>(
        adminApi,
        'query DataTables($workspaceId: ID!) { dataTables(environmentId: "0", workspaceId: $workspaceId) { id baseName } }',
        {workspaceId: String(workspace.id)}
    );

    const dataTableId = dataTables.find((dataTable) => dataTable.baseName === baseName)!.id;

    return {connectionId, dataTableId, jobId, projectDeploymentId, scratchProjectId};
}

test.describe('Server-side permission gates', () => {
    test.describe.configure({mode: 'serial'});

    const callers: Partial<Record<CallerType, APIRequestContext>> = {};

    let knowledgeBaseEnabled = false;
    let outsider: RoleUserI;
    let resources: ResourcesI;

    test.beforeAll(async ({adminApi, rbacWorkspace, workerRoleUsers}) => {
        resources = await createResources(adminApi, rbacWorkspace);

        const knowledgeBases = await postGraphQl<{knowledgeBases: unknown[] | null}>(
            adminApi,
            'query KnowledgeBases($workspaceId: ID!) { knowledgeBases(environmentId: "0", workspaceId: $workspaceId) { id } }',
            {workspaceId: String(rbacWorkspace.id)}
        );

        knowledgeBaseEnabled = knowledgeBases.data?.knowledgeBases != null;

        outsider = await createAccount(adminApi, RBAC_BASE_URL);

        callers.OUTSIDER = await createRoleUserApiContext(RBAC_BASE_URL, outsider);

        for (const role of ['ADMIN', 'EDITOR', 'VIEWER'] as BuiltInRoleType[]) {
            callers[role] = await createRoleUserApiContext(RBAC_BASE_URL, workerRoleUsers[role]);
        }
    });

    test.afterAll(async ({adminApi}) => {
        for (const caller of Object.values(callers)) {
            await caller?.dispose();
        }

        if (outsider) {
            await deleteRoleUser(adminApi, outsider);
        }
    });

    for (const gateCase of GATE_CASES) {
        test(`${gateCase.module}: ${gateCase.name} is refused for ${gateCase.denied.join(', ')}${
            gateCase.allowed ? ` and allowed for ${gateCase.allowed.join(', ')}` : ''
        }`, async ({rbacWorkspace}) => {
            test.skip(
                gateCase.module === 'Knowledge base' && !knowledgeBaseEnabled,
                'the server runs with bytechef.ai.knowledge-base.enabled=false'
            );

            for (const caller of gateCase.denied) {
                await test.step(`${caller} is refused`, async () => {
                    if (gateCase.graphQl) {
                        await expectGraphQlDenied(
                            callers[caller]!,
                            gateCase.graphQl,
                            gateCase.graphQl.variables(rbacWorkspace, resources)
                        );
                    } else {
                        await expectRestStatus(callers[caller]!, gateCase.rest!, rbacWorkspace, resources, true);
                    }
                });
            }

            for (const caller of gateCase.allowed || []) {
                await test.step(`${caller} is allowed`, async () => {
                    if (gateCase.graphQl) {
                        const response = await postGraphQl(
                            callers[caller]!,
                            gateCase.graphQl.query,
                            gateCase.graphQl.variables(rbacWorkspace, resources)
                        );

                        expect(response.errors, JSON.stringify(response.errors)).toBeUndefined();
                    } else {
                        await expectRestStatus(callers[caller]!, gateCase.rest!, rbacWorkspace, resources, false);
                    }
                });
            }
        });
    }

    test('Connection: an EDITOR deletes a connection they created (CONNECTION_DELETE)', async ({rbacWorkspace}) => {
        const connectionId = await internalApi<number>(callers.EDITOR!, 'POST', '/connections', {
            componentName: 'httpClient',
            connectionVersion: 1,
            environmentId: 0,
            name: `rbac_gate_${getRandomString()}`,
            parameters: {},
            workspaceId: rbacWorkspace.id,
        });

        await expectRestStatus(
            callers.EDITOR!,
            {method: 'DELETE', path: () => `${INTERNAL}/connections/${connectionId}`},
            rbacWorkspace,
            resources,
            false
        );
    });

    test('Execution: a VIEWER reads a run without its step data, an EDITOR with it (EXECUTION_DETAILS_VIEW)', async ({
        adminApi,
    }) => {
        let completedJob: WorkflowExecutionJobI | undefined;

        await expect
            .poll(
                async () => {
                    const workflowExecution = await internalApi<{job: WorkflowExecutionJobI}>(
                        adminApi,
                        'GET',
                        `/workflow-executions/${resources.jobId}`
                    );

                    completedJob = workflowExecution.job;

                    return completedJob.status;
                },
                {timeout: 30_000}
            )
            .toBe('COMPLETED');

        const taskExecutionId = completedJob!.taskExecutions[0].id;

        const viewerWorkflowExecution = await internalApi<{job: WorkflowExecutionJobI}>(
            callers.VIEWER!,
            'GET',
            `/workflow-executions/${resources.jobId}`
        );

        expect(viewerWorkflowExecution.job.status).toBe('COMPLETED');
        expect(viewerWorkflowExecution.job.taskExecutions.map((taskExecution) => taskExecution.id)).toContain(
            taskExecutionId
        );
        expect(viewerWorkflowExecution.job.outputs ?? null).toBeNull();

        const viewerTaskExecution = await internalApi<WorkflowExecutionTaskExecutionI>(
            callers.VIEWER!,
            'GET',
            `/workflow-executions/${resources.jobId}/task-executions/${taskExecutionId}`
        );

        expect(viewerTaskExecution.input ?? null).toBeNull();
        expect(viewerTaskExecution.output ?? null).toBeNull();

        const editorTaskExecution = await internalApi<WorkflowExecutionTaskExecutionI>(
            callers.EDITOR!,
            'GET',
            `/workflow-executions/${resources.jobId}/task-executions/${taskExecutionId}`
        );

        expect(editorTaskExecution.output).toMatchObject({String: 'foo'});
    });

    for (const tenantAdminCase of TENANT_ADMIN_CASES) {
        test(`tenant admin only: ${tenantAdminCase.name} is refused for a workspace ADMIN`, async ({rbacWorkspace}) => {
            if (tenantAdminCase.graphQl) {
                await expectGraphQlDenied(
                    callers.ADMIN!,
                    tenantAdminCase.graphQl,
                    tenantAdminCase.graphQl.variables(rbacWorkspace, resources)
                );
            } else {
                await expectRestStatus(callers.ADMIN!, tenantAdminCase.rest!, rbacWorkspace, resources, true);
            }
        });
    }
});
