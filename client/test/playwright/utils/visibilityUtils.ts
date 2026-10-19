import {APIRequestContext, APIResponse, expect, request} from '@playwright/test';

import getRandomString from './getRandomString';
import {
    type BuiltInRoleType,
    type CustomRoleI,
    RBAC_BASE_URL,
    type RbacWorkspaceI,
    type RoleUserI,
    createAccount as createRbacAccount,
    fetchUserId,
} from './rbacUtils';

export {fetchUserId};

export const INTERNAL = '/api/automation/internal';

const SERVER_URL = process.env.PLAYWRIGHT_SERVER_URL || 'http://localhost:9555';

export const HTTP_CLIENT_WORKFLOW_NODE = 'httpClient_1';

export type VisibilityType = 'ORGANIZATION' | 'PRIVATE' | 'WORKSPACE';

export type ConnectionStatusType = 'ACTIVE' | 'PENDING_REASSIGNMENT' | 'REVOKED';

export interface ConnectionI {
    createdBy?: string;
    environmentId?: number;
    id: number;
    name: string;
    status?: ConnectionStatusType;
    version?: number;
    visibility?: VisibilityType;
}

export interface GraphQlErrorI {
    extensions?: Record<string, unknown>;
    message: string;
}

export interface GraphQlResponseI<T> {
    data: T | null;
    errors?: GraphQlErrorI[];
}

async function getXsrfToken(apiContext: APIRequestContext): Promise<string> {
    await apiContext.get('/api/account');

    const storageState = await apiContext.storageState();

    return storageState.cookies.find((cookie) => cookie.name === 'XSRF-TOKEN')?.value || '';
}

export async function graphql<T>(
    apiContext: APIRequestContext,
    query: string,
    variables: Record<string, unknown>
): Promise<T> {
    const xsrfToken = await getXsrfToken(apiContext);

    const response = await apiContext.post('/graphql', {
        data: {query, variables},
        headers: {'X-XSRF-TOKEN': xsrfToken},
    });

    const body = await response.json();

    expect(body.errors, JSON.stringify(body.errors)).toBeUndefined();

    return body.data as T;
}

export async function internalApi<T>(
    apiContext: APIRequestContext,
    method: 'DELETE' | 'GET' | 'PATCH' | 'POST' | 'PUT',
    path: string,
    data?: unknown
): Promise<T> {
    const xsrfToken = await getXsrfToken(apiContext);

    const response = await apiContext.fetch(`${INTERNAL}${path}`, {
        data,
        headers: {'X-XSRF-TOKEN': xsrfToken},
        method,
    });

    expect(response.ok(), `${method} ${path} failed: ${response.status()} ${await response.text()}`).toBeTruthy();

    const text = await response.text();

    return (text ? JSON.parse(text) : undefined) as T;
}

export async function createUserApiContext(email: string, password: string): Promise<APIRequestContext> {
    const apiContext = await request.newContext({baseURL: RBAC_BASE_URL});

    const xsrfToken = await getXsrfToken(apiContext);

    const response = await apiContext.post('/api/authentication', {
        form: {password, 'remember-me': 'false', username: email},
        headers: {'X-XSRF-TOKEN': xsrfToken},
    });

    expect(response.ok(), `login of ${email} failed: ${response.status()}`).toBeTruthy();

    return apiContext;
}

export async function postGraphQl<T = Record<string, unknown>>(
    apiContext: APIRequestContext,
    query: string,
    variables: Record<string, unknown> = {}
): Promise<GraphQlResponseI<T>> {
    const xsrfToken = await getXsrfToken(apiContext);

    const response = await apiContext.post('/graphql', {
        data: {query, variables},
        headers: {'X-XSRF-TOKEN': xsrfToken},
    });

    expect(response.status(), `POST /graphql answered ${response.status()}: ${await response.text()}`).toBe(200);

    return response.json();
}

export async function sendInternalApi(
    apiContext: APIRequestContext,
    method: 'DELETE' | 'GET' | 'PATCH' | 'POST' | 'PUT',
    path: string,
    data?: unknown
): Promise<APIResponse> {
    const xsrfToken = await getXsrfToken(apiContext);

    return apiContext.fetch(path, {data, headers: {'X-XSRF-TOKEN': xsrfToken}, method});
}

export function expectAccessDenied(response: GraphQlResponseI<unknown>): void {
    expect(response.errors, JSON.stringify(response)).toBeDefined();

    expect(JSON.stringify(response.errors)).toMatch(/Access denied|FORBIDDEN|Forbidden/i);
}

export function expectGraphQlError(response: GraphQlResponseI<unknown>, pattern: RegExp): void {
    expect(response.errors, JSON.stringify(response)).toBeDefined();

    expect(JSON.stringify(response.errors)).toMatch(pattern);
}

export function expectNoGraphQlErrors(response: GraphQlResponseI<unknown>): void {
    expect(response.errors, JSON.stringify(response.errors)).toBeUndefined();
}

export async function createAccount(
    adminApi: APIRequestContext,
    authority: 'ROLE_ADMIN' | 'ROLE_USER' = 'ROLE_USER'
): Promise<RoleUserI> {
    return createRbacAccount(adminApi, RBAC_BASE_URL, authority);
}

export async function createWorkspaceMember(
    adminApi: APIRequestContext,
    workspaceId: number,
    role: BuiltInRoleType
): Promise<RoleUserI> {
    const account = await createAccount(adminApi);

    await addWorkspaceMember(adminApi, workspaceId, account, role);

    return {...account, role};
}

export async function createCustomRoleMember(
    adminApi: APIRequestContext,
    workspaceId: number,
    customRole: CustomRoleI
): Promise<RoleUserI> {
    const account = await createAccount(adminApi);

    await graphql(
        adminApi,
        `
            mutation InviteWorkspaceUser($workspaceId: ID!, $email: String!, $customRoleId: ID) {
                inviteWorkspaceUser(workspaceId: $workspaceId, email: $email, customRoleId: $customRoleId) {
                    userId
                }
            }
        `,
        {customRoleId: customRole.id, email: account.email, workspaceId: String(workspaceId)}
    );

    return {...account, role: customRole.name};
}

export async function addWorkspaceMember(
    adminApi: APIRequestContext,
    workspaceId: number,
    user: RoleUserI,
    role: BuiltInRoleType
): Promise<void> {
    await graphql(
        adminApi,
        `
            mutation InviteWorkspaceUser($workspaceId: ID!, $email: String!, $role: WorkspaceRole) {
                inviteWorkspaceUser(workspaceId: $workspaceId, email: $email, role: $role) {
                    userId
                }
            }
        `,
        {email: user.email, role, workspaceId: String(workspaceId)}
    );
}

export async function setWorkspaceRole(
    adminApi: APIRequestContext,
    workspaceId: number,
    user: RoleUserI,
    role: BuiltInRoleType
): Promise<void> {
    await graphql(
        adminApi,
        `
            mutation UpdateWorkspaceUserRole($workspaceId: ID!, $userId: ID!, $role: WorkspaceRole!) {
                updateWorkspaceUserRole(workspaceId: $workspaceId, userId: $userId, role: $role) {
                    id
                }
            }
        `,
        {role, userId: user.userId, workspaceId: String(workspaceId)}
    );
}

export async function removeWorkspaceMember(
    adminApi: APIRequestContext,
    workspaceId: number,
    user: RoleUserI
): Promise<void> {
    await graphql(
        adminApi,
        'mutation RemoveWorkspaceUser($workspaceId: ID!, $userId: ID!) { removeWorkspaceUser(workspaceId: $workspaceId, userId: $userId) }',
        {userId: user.userId, workspaceId: String(workspaceId)}
    );
}

export async function createWorkspace(adminApi: APIRequestContext): Promise<{id: number; name: string}> {
    const name = `vis_${getRandomString()}`;

    const workspace = await internalApi<{id: number}>(adminApi, 'POST', '/workspaces', {name});

    return {id: workspace.id, name};
}

export async function deleteWorkspace(adminApi: APIRequestContext, workspaceId: number): Promise<void> {
    await internalApi(adminApi, 'DELETE', `/workspaces/${workspaceId}`).catch(() => undefined);
}

export function connectionName(prefix = 'vis'): string {
    return `${prefix}_${getRandomString()}`;
}

export async function createConnectionResponse(
    apiContext: APIRequestContext,
    workspaceId: number,
    options: {environmentId?: number; name?: string; visibility?: VisibilityType} = {}
): Promise<APIResponse> {
    return sendInternalApi(apiContext, 'POST', `${INTERNAL}/connections`, {
        componentName: 'httpClient',
        connectionVersion: 1,
        environmentId: options.environmentId ?? 0,
        name: options.name ?? connectionName(),
        parameters: {},
        workspaceId,
        ...(options.visibility ? {visibility: options.visibility} : {}),
    });
}

export async function createConnection(
    apiContext: APIRequestContext,
    workspaceId: number,
    options: {environmentId?: number; name?: string; visibility?: VisibilityType} = {}
): Promise<number> {
    const response = await createConnectionResponse(apiContext, workspaceId, options);

    expect(response.ok(), `create connection failed: ${response.status()} ${await response.text()}`).toBeTruthy();

    return Number(await response.text());
}

export async function getConnectionResponse(apiContext: APIRequestContext, connectionId: number): Promise<APIResponse> {
    return sendInternalApi(apiContext, 'GET', `${INTERNAL}/connections/${connectionId}`);
}

export async function getConnection(apiContext: APIRequestContext, connectionId: number): Promise<ConnectionI> {
    const response = await getConnectionResponse(apiContext, connectionId);

    expect(response.ok(), `get connection ${connectionId}: ${response.status()} ${await response.text()}`).toBeTruthy();

    return response.json();
}

export async function listConnectionIds(
    apiContext: APIRequestContext,
    workspaceId: number,
    environmentId = 0
): Promise<number[]> {
    const response = await sendInternalApi(
        apiContext,
        'GET',
        `${INTERNAL}/workspaces/${workspaceId}/connections?environmentId=${environmentId}`
    );

    expect(response.ok(), `list connections: ${response.status()} ${await response.text()}`).toBeTruthy();

    const connections: ConnectionI[] = await response.json();

    return connections.map((connection) => connection.id);
}

export async function updateConnectionResponse(
    apiContext: APIRequestContext,
    connectionId: number,
    name: string
): Promise<APIResponse> {
    const response = await getConnectionResponse(apiContext, connectionId);

    const version = response.ok() ? (await response.json()).__version : 0;

    return sendInternalApi(apiContext, 'PATCH', `${INTERNAL}/connections/${connectionId}`, {
        __version: version,
        name,
        tags: [],
    });
}

export async function deleteConnectionResponse(
    apiContext: APIRequestContext,
    connectionId: number
): Promise<APIResponse> {
    return sendInternalApi(apiContext, 'DELETE', `${INTERNAL}/connections/${connectionId}`);
}

export async function setConnectionVisibility(
    apiContext: APIRequestContext,
    workspaceId: number,
    connectionId: number,
    visibility: VisibilityType
): Promise<GraphQlResponseI<{setConnectionVisibility: boolean}>> {
    return postGraphQl(
        apiContext,
        `mutation SetConnectionVisibility($workspaceId: ID!, $connectionId: ID!, $visibility: ResourceVisibility!) {
            setConnectionVisibility(workspaceId: $workspaceId, connectionId: $connectionId, visibility: $visibility)
        }`,
        {connectionId: String(connectionId), visibility, workspaceId: String(workspaceId)}
    );
}

export async function grantConnectionAccess(
    apiContext: APIRequestContext,
    workspaceId: number,
    connectionId: number,
    userId: string
): Promise<GraphQlResponseI<{grantConnectionAccess: boolean}>> {
    return postGraphQl(
        apiContext,
        `mutation GrantConnectionAccess($workspaceId: ID!, $connectionId: ID!, $userId: ID!) {
            grantConnectionAccess(workspaceId: $workspaceId, connectionId: $connectionId, userId: $userId)
        }`,
        {connectionId: String(connectionId), userId, workspaceId: String(workspaceId)}
    );
}

export async function revokeConnectionAccess(
    apiContext: APIRequestContext,
    workspaceId: number,
    connectionId: number,
    userId: string
): Promise<GraphQlResponseI<{revokeConnectionAccess: boolean}>> {
    return postGraphQl(
        apiContext,
        `mutation RevokeConnectionAccess($workspaceId: ID!, $connectionId: ID!, $userId: ID!) {
            revokeConnectionAccess(workspaceId: $workspaceId, connectionId: $connectionId, userId: $userId)
        }`,
        {connectionId: String(connectionId), userId, workspaceId: String(workspaceId)}
    );
}

export async function fetchConnectionGrants(
    apiContext: APIRequestContext,
    workspaceId: number,
    connectionId: number
): Promise<GraphQlResponseI<{connectionGrants: (number | string)[]}>> {
    return postGraphQl(
        apiContext,
        'query ConnectionGrants($workspaceId: ID!, $connectionId: ID!) { connectionGrants(workspaceId: $workspaceId, connectionId: $connectionId) }',
        {connectionId: String(connectionId), workspaceId: String(workspaceId)}
    );
}

export async function createOrganizationConnection(
    apiContext: APIRequestContext,
    options: {environmentId?: number; name?: string} = {}
): Promise<GraphQlResponseI<{createOrganizationConnection: string}>> {
    return postGraphQl(
        apiContext,
        'mutation CreateOrganizationConnection($input: CreateOrganizationConnectionInput!) { createOrganizationConnection(input: $input) }',
        {
            input: {
                componentName: 'httpClient',
                connectionVersion: 1,
                environmentId: options.environmentId ?? 0,
                name: options.name ?? connectionName('vis_org'),
                parameters: {},
            },
        }
    );
}

export async function createOrganizationConnectionId(
    adminApi: APIRequestContext,
    options: {environmentId?: number; name?: string} = {}
): Promise<number> {
    const response = await createOrganizationConnection(adminApi, options);

    expectNoGraphQlErrors(response);

    return Number(response.data!.createOrganizationConnection);
}

export async function deleteOrganizationConnection(
    apiContext: APIRequestContext,
    connectionId: number
): Promise<GraphQlResponseI<{deleteOrganizationConnection: boolean}>> {
    return postGraphQl(
        apiContext,
        'mutation DeleteOrganizationConnection($connectionId: ID!) { deleteOrganizationConnection(connectionId: $connectionId) }',
        {connectionId: String(connectionId)}
    );
}

export async function markConnectionsPendingReassignment(
    apiContext: APIRequestContext,
    workspaceId: number,
    userLogin: string
): Promise<
    GraphQlResponseI<{
        markConnectionsPendingReassignment: {failed: number; skipped: number; total: number; updated: number};
    }>
> {
    return postGraphQl(
        apiContext,
        `mutation Mark($workspaceId: ID!, $userLogin: String!) {
            markConnectionsPendingReassignment(workspaceId: $workspaceId, userLogin: $userLogin) {
                total updated skipped failed
            }
        }`,
        {userLogin, workspaceId: String(workspaceId)}
    );
}

export async function reassignConnection(
    apiContext: APIRequestContext,
    workspaceId: number,
    connectionId: number,
    newOwnerLogin: string
): Promise<GraphQlResponseI<{reassignConnection: boolean}>> {
    return postGraphQl(
        apiContext,
        `mutation Reassign($workspaceId: ID!, $connectionId: ID!, $newOwnerLogin: String!) {
            reassignConnection(workspaceId: $workspaceId, connectionId: $connectionId, newOwnerLogin: $newOwnerLogin)
        }`,
        {connectionId: String(connectionId), newOwnerLogin, workspaceId: String(workspaceId)}
    );
}

export async function saveTestConfigurationConnection(
    apiContext: APIRequestContext,
    workflowId: string,
    connectionId: number
): Promise<GraphQlResponseI<{saveWorkflowTestConfigurationConnection: boolean}>> {
    return postGraphQl(
        apiContext,
        `mutation Save($workflowId: String!, $workflowNodeName: String!, $workflowConnectionKey: String!, $connectionId: Long!, $environmentId: Long!) {
            saveWorkflowTestConfigurationConnection(
                workflowId: $workflowId
                workflowNodeName: $workflowNodeName
                workflowConnectionKey: $workflowConnectionKey
                connectionId: $connectionId
                environmentId: $environmentId
            )
        }`,
        {
            connectionId,
            environmentId: 0,
            workflowConnectionKey: 'httpClient',
            workflowId,
            workflowNodeName: HTTP_CLIENT_WORKFLOW_NODE,
        }
    );
}

export function httpClientWorkflowDefinition(triggerType = 'manual/v1/manual'): string {
    return JSON.stringify({
        description: '',
        inputs: [],
        label: `vis_http_${getRandomString()}`,
        tasks: [
            {
                label: 'HTTP Client',
                name: HTTP_CLIENT_WORKFLOW_NODE,
                parameters: {uri: `${SERVER_URL}/actuator/health`},
                type: 'httpClient/v1/get',
            },
        ],
        triggers: [{label: 'Trigger', name: 'trigger_1', parameters: {}, type: triggerType}],
    });
}

export interface HttpClientProjectI {
    projectId: number;
    projectWorkflowId: number;
    workflowId: string;
}

export async function createHttpClientProject(
    apiContext: APIRequestContext,
    workspaceId: number,
    triggerType = 'manual/v1/manual'
): Promise<HttpClientProjectI> {
    const projectId = await internalApi<number>(apiContext, 'POST', '/projects', {
        name: `vis_project_${getRandomString()}`,
        workspaceId,
    });

    const {projectWorkflowId} = await internalApi<{projectWorkflowId: number}>(
        apiContext,
        'POST',
        `/projects/${projectId}/workflows`,
        {definition: httpClientWorkflowDefinition(triggerType)}
    );

    const workflows = await internalApi<{id: string}[]>(apiContext, 'GET', `/projects/${projectId}/workflows`);

    return {projectId, projectWorkflowId, workflowId: workflows[0].id};
}

export interface HttpClientDeploymentI {
    projectDeploymentId: number;
    projectId: number;
    workflowId: string;
}

export async function deployHttpClientProjectResponse(
    apiContext: APIRequestContext,
    projectId: number,
    connectionId: number,
    environmentId = 0
): Promise<{response: APIResponse; workflowId: string}> {
    await internalApi(apiContext, 'POST', `/projects/${projectId}/publish`, {description: 'visibility'}).catch(
        () => undefined
    );

    const workflows = await internalApi<{id: string}[]>(
        apiContext,
        'GET',
        `/projects/${projectId}/versions/1/workflows`
    );

    const response = await sendInternalApi(apiContext, 'POST', `${INTERNAL}/project-deployments`, {
        enabled: false,
        environmentId,
        name: `vis_deployment_${getRandomString()}`,
        projectDeploymentWorkflows: workflows.map((workflow) => ({
            connections: [
                {connectionId, workflowConnectionKey: 'httpClient', workflowNodeName: HTTP_CLIENT_WORKFLOW_NODE},
            ],
            enabled: true,
            workflowId: workflow.id,
        })),
        projectId,
        projectVersion: 1,
    });

    return {response, workflowId: workflows[0].id};
}

export async function deployHttpClientProject(
    apiContext: APIRequestContext,
    workspaceId: number,
    connectionId: number,
    triggerType = 'manual/v1/manual'
): Promise<HttpClientDeploymentI> {
    const {projectId} = await createHttpClientProject(apiContext, workspaceId, triggerType);

    const {response, workflowId} = await deployHttpClientProjectResponse(apiContext, projectId, connectionId);

    expect(response.ok(), `deploy failed: ${response.status()} ${await response.text()}`).toBeTruthy();

    const projectDeploymentId = Number(await response.text());

    await internalApi(apiContext, 'PATCH', `/project-deployments/${projectDeploymentId}/enable/true`);

    return {projectDeploymentId, projectId, workflowId};
}

export async function runDeployedWorkflowResponse(
    apiContext: APIRequestContext,
    deployment: HttpClientDeploymentI
): Promise<APIResponse> {
    return sendInternalApi(
        apiContext,
        'POST',
        `${INTERNAL}/project-deployments/${deployment.projectDeploymentId}/workflows/${deployment.workflowId}/jobs`
    );
}

export interface DeploymentJobI {
    id: number;
    status: string;
    taskExecutionCount: number;
}

export async function listDeploymentJobs(
    adminApi: APIRequestContext,
    workspaceId: number,
    deployment: HttpClientDeploymentI
): Promise<DeploymentJobI[]> {
    const page = await internalApi<{content: {id: number}[]}>(
        adminApi,
        'GET',
        `/workspaces/${workspaceId}/workflow-executions?environmentId=0&projectDeploymentId=${deployment.projectDeploymentId}`
    );

    const jobs: DeploymentJobI[] = [];

    for (const workflowExecution of page.content) {
        const detail = await internalApi<{job: {id: number; status: string; taskExecutions?: unknown[]}}>(
            adminApi,
            'GET',
            `/workflow-executions/${workflowExecution.id}`
        );

        jobs.push({
            id: detail.job.id,
            status: detail.job.status,
            taskExecutionCount: detail.job.taskExecutions?.length ?? 0,
        });
    }

    return jobs.sort((left, right) => left.id - right.id);
}

export async function createProjectResponse(
    apiContext: APIRequestContext,
    workspaceId: number,
    options: {name?: string; visibility?: 'PRIVATE' | 'WORKSPACE'} = {}
): Promise<APIResponse> {
    return sendInternalApi(apiContext, 'POST', `${INTERNAL}/projects`, {
        name: options.name ?? `vis_project_${getRandomString()}`,
        workspaceId,
        ...(options.visibility ? {visibility: options.visibility} : {}),
    });
}

export async function getProjectResponse(apiContext: APIRequestContext, projectId: number): Promise<APIResponse> {
    return sendInternalApi(apiContext, 'GET', `${INTERNAL}/projects/${projectId}`);
}

export async function listWorkspaceProjectIds(apiContext: APIRequestContext, workspaceId: number): Promise<number[]> {
    const projects = await internalApi<{id: number}[]>(apiContext, 'GET', `/workspaces/${workspaceId}/projects`);

    return projects.map((project) => project.id);
}

export async function listWorkspaceProjectDeploymentIds(
    apiContext: APIRequestContext,
    workspaceId: number
): Promise<number[]> {
    const projectDeployments = await internalApi<{id: number}[]>(
        apiContext,
        'GET',
        `/workspaces/${workspaceId}/project-deployments?environmentId=0`
    );

    return projectDeployments.map((projectDeployment) => projectDeployment.id);
}

export async function listWorkspaceWorkflowExecutionIds(
    apiContext: APIRequestContext,
    workspaceId: number
): Promise<number[]> {
    const page = await internalApi<{content: {id: number}[]}>(
        apiContext,
        'GET',
        `/workspaces/${workspaceId}/workflow-executions?environmentId=0`
    );

    return page.content.map((workflowExecution) => workflowExecution.id);
}

export async function setProjectVisibility(
    apiContext: APIRequestContext,
    workspaceId: number,
    projectId: number,
    visibility: VisibilityType
): Promise<GraphQlResponseI<{setProjectVisibility: boolean}>> {
    return postGraphQl(
        apiContext,
        `mutation SetProjectVisibility($workspaceId: ID!, $projectId: ID!, $visibility: ResourceVisibility!) {
            setProjectVisibility(workspaceId: $workspaceId, projectId: $projectId, visibility: $visibility)
        }`,
        {projectId: String(projectId), visibility, workspaceId: String(workspaceId)}
    );
}

export async function grantProjectAccess(
    apiContext: APIRequestContext,
    workspaceId: number,
    projectId: number,
    userId: string
): Promise<GraphQlResponseI<{grantProjectAccess: boolean}>> {
    return postGraphQl(
        apiContext,
        `mutation GrantProjectAccess($workspaceId: ID!, $projectId: ID!, $userId: ID!) {
            grantProjectAccess(workspaceId: $workspaceId, projectId: $projectId, userId: $userId)
        }`,
        {projectId: String(projectId), userId, workspaceId: String(workspaceId)}
    );
}

export async function revokeProjectAccess(
    apiContext: APIRequestContext,
    workspaceId: number,
    projectId: number,
    userId: string
): Promise<GraphQlResponseI<{revokeProjectAccess: boolean}>> {
    return postGraphQl(
        apiContext,
        `mutation RevokeProjectAccess($workspaceId: ID!, $projectId: ID!, $userId: ID!) {
            revokeProjectAccess(workspaceId: $workspaceId, projectId: $projectId, userId: $userId)
        }`,
        {projectId: String(projectId), userId, workspaceId: String(workspaceId)}
    );
}

export async function fetchProjectGrants(
    apiContext: APIRequestContext,
    workspaceId: number,
    projectId: number
): Promise<GraphQlResponseI<{projectGrants: (number | string)[]}>> {
    return postGraphQl(
        apiContext,
        'query ProjectGrants($workspaceId: ID!, $projectId: ID!) { projectGrants(workspaceId: $workspaceId, projectId: $projectId) }',
        {projectId: String(projectId), workspaceId: String(workspaceId)}
    );
}

export async function updateConnectionCredentials(
    apiContext: APIRequestContext,
    connectionId: number,
    parameters: Record<string, unknown>,
    version: number
): Promise<GraphQlResponseI<{updateConnectionCredentials: boolean}>> {
    return postGraphQl(
        apiContext,
        `mutation UpdateConnectionCredentials($input: UpdateConnectionCredentialsInput!) {
            updateConnectionCredentials(input: $input)
        }`,
        {input: {connectionId: String(connectionId), parameters, version}}
    );
}

export {RBAC_BASE_URL};

export type {RbacWorkspaceI, RoleUserI};
