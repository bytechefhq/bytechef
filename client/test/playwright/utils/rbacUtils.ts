import {APIRequestContext, Browser, BrowserContext, Page, expect, request} from '@playwright/test';
import {readFileSync} from 'fs';

import {SAMPLE_WORKFLOW_PATH, TEST_USER} from './constants';
import getRandomString from './getRandomString';
import {login} from './login';

export type BuiltInRoleType = 'ADMIN' | 'EDITOR' | 'VIEWER';

export interface RoleUserI {
    email: string;
    login: string;
    password: string;
    role: string;
    userId?: string;
}

export interface CustomRoleI {
    id: string;
    name: string;
    scopes: string[];
}

export interface RbacWorkspaceI {
    id: number;
    name: string;
    projectId: number;
    projectName: string;
    projectWorkflowId: number;
    workflowId: string;
}

export const RBAC_BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:5173';

const ADMIN_EMAIL = process.env.PLAYWRIGHT_ADMIN_EMAIL || TEST_USER.email;

const ADMIN_PASSWORD = process.env.PLAYWRIGHT_ADMIN_PASSWORD || TEST_USER.password;

const MAILPIT_URL = process.env.PLAYWRIGHT_MAILPIT_URL || 'http://localhost:8025';

const ROLE_USER_PASSWORD = 'Playwright-Role-1';

async function getXsrfToken(apiContext: APIRequestContext): Promise<string> {
    await apiContext.get('/api/account');

    const storageState = await apiContext.storageState();

    return storageState.cookies.find((cookie) => cookie.name === 'XSRF-TOKEN')?.value || '';
}

export async function createAdminApiContext(baseURL: string): Promise<APIRequestContext> {
    const apiContext = await request.newContext({baseURL});

    const healthResponse = await apiContext.get('/actuator/health').catch(() => undefined);

    expect(
        healthResponse?.ok(),
        `the ByteChef server behind ${baseURL} is not answering /actuator/health (status ${healthResponse?.status()}); start it before running the RBAC tests`
    ).toBeTruthy();

    const xsrfToken = await getXsrfToken(apiContext);

    const response = await apiContext.post('/api/authentication', {
        form: {password: ADMIN_PASSWORD, 'remember-me': 'false', username: ADMIN_EMAIL},
        headers: {'X-XSRF-TOKEN': xsrfToken},
    });

    expect(response.ok(), `admin login failed: ${response.status()}`).toBeTruthy();

    return apiContext;
}

async function graphql<T>(
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

async function internalApi<T>(
    apiContext: APIRequestContext,
    method: 'DELETE' | 'GET' | 'POST' | 'PUT',
    path: string,
    data?: unknown
): Promise<T> {
    const xsrfToken = await getXsrfToken(apiContext);

    const response = await apiContext.fetch(`/api/automation/internal${path}`, {
        data,
        headers: {'X-XSRF-TOKEN': xsrfToken},
        method,
    });

    expect(response.ok(), `${method} ${path} failed: ${response.status()} ${await response.text()}`).toBeTruthy();

    const text = await response.text();

    return (text ? JSON.parse(text) : undefined) as T;
}

export async function createRbacWorkspace(apiContext: APIRequestContext): Promise<RbacWorkspaceI> {
    const name = `rbac_${getRandomString()}`;

    const workspace = await internalApi<{id: number}>(apiContext, 'POST', '/workspaces', {name});

    const projectName = `project_${getRandomString()}`;

    const projectId = await internalApi<number>(apiContext, 'POST', '/projects', {
        name: projectName,
        workspaceId: workspace.id,
    });

    const {projectWorkflowId} = await internalApi<{projectWorkflowId: number}>(
        apiContext,
        'POST',
        `/projects/${projectId}/workflows`,
        {definition: readFileSync(SAMPLE_WORKFLOW_PATH, 'utf-8')}
    );

    const projectWorkflows = await internalApi<{id: string; projectWorkflowId: number}[]>(
        apiContext,
        'GET',
        `/projects/${projectId}/workflows`
    );

    const workflowId = projectWorkflows.find((workflow) => workflow.projectWorkflowId === projectWorkflowId)!.id;

    return {id: workspace.id, name, projectId, projectName, projectWorkflowId, workflowId};
}

export async function deleteRbacWorkspace(apiContext: APIRequestContext, workspace: RbacWorkspaceI): Promise<void> {
    await internalApi(apiContext, 'DELETE', `/projects/${workspace.projectId}`).catch(() => undefined);

    await internalApi(apiContext, 'DELETE', `/workspaces/${workspace.id}`).catch(() => undefined);
}

async function readClaimKey(email: string): Promise<string> {
    const mailpitContext = await request.newContext({baseURL: MAILPIT_URL});

    try {
        let claimKey = '';

        await expect(async () => {
            const searchResponse = await mailpitContext.get('/api/v1/search', {
                params: {limit: '1', query: `to:"${email}"`},
            });

            const search = await searchResponse.json();

            const messageId = search.messages?.[0]?.ID;

            expect(messageId, `no claim mail for ${email} yet`).toBeTruthy();

            const messageResponse = await mailpitContext.get(`/api/v1/message/${messageId}`);

            const message = await messageResponse.json();

            const match = `${message.HTML} ${message.Text}`.match(/password-reset\/finish\?key=([A-Za-z0-9]+)/);

            expect(match, `no claim link in the mail to ${email}`).toBeTruthy();

            claimKey = match![1];
        }).toPass({timeout: 30000});

        return claimKey;
    } finally {
        await mailpitContext.dispose();
    }
}

async function claimAccount(baseURL: string, email: string): Promise<void> {
    const claimKey = await readClaimKey(email);

    const anonymousContext = await request.newContext({baseURL});

    try {
        const xsrfToken = await getXsrfToken(anonymousContext);

        const response = await anonymousContext.post('/api/account/reset-password/finish', {
            data: {key: claimKey, newPassword: ROLE_USER_PASSWORD},
            headers: {'X-XSRF-TOKEN': xsrfToken},
        });

        expect(response.ok(), `claiming ${email} failed: ${response.status()}`).toBeTruthy();
    } finally {
        await anonymousContext.dispose();
    }
}

export async function createRoleUser(
    apiContext: APIRequestContext,
    baseURL: string,
    workspace: RbacWorkspaceI,
    role: BuiltInRoleType
): Promise<RoleUserI> {
    const userLogin = `rbac_${role.toLowerCase()}_${getRandomString()}`;

    const email = `${userLogin}@localhost.com`;

    await graphql(
        apiContext,
        `
            mutation InviteUser($email: String!, $role: String!, $workspaces: [WorkspaceAssignmentInput!]) {
                inviteUser(email: $email, role: $role, workspaces: $workspaces)
            }
        `,
        {email, role: 'ROLE_USER', workspaces: [{roleName: role, workspaceId: String(workspace.id)}]}
    );

    await claimAccount(baseURL, email);

    return {email, login: userLogin, password: ROLE_USER_PASSWORD, role};
}

export async function deleteRoleUser(apiContext: APIRequestContext, roleUser: RoleUserI): Promise<void> {
    await graphql(apiContext, 'mutation DeleteUser($login: String!) { deleteUser(login: $login) }', {
        login: roleUser.login,
    }).catch(() => undefined);
}

export async function createCustomRole(apiContext: APIRequestContext, scopes: string[]): Promise<CustomRoleI> {
    const name = `rbac_role_${getRandomString()}`;

    const data = await graphql<{createCustomRole: CustomRoleI}>(
        apiContext,
        `
            mutation CreateCustomRole($input: CreateCustomRoleInput!) {
                createCustomRole(input: $input) {
                    id
                    name
                    scopes
                }
            }
        `,
        {input: {name, scopes}}
    );

    return data.createCustomRole;
}

export async function updateCustomRoleScopes(
    apiContext: APIRequestContext,
    customRole: CustomRoleI,
    scopes: string[]
): Promise<void> {
    await graphql(
        apiContext,
        `
            mutation UpdateCustomRole($id: ID!, $input: UpdateCustomRoleInput!) {
                updateCustomRole(id: $id, input: $input) {
                    id
                }
            }
        `,
        {id: customRole.id, input: {name: customRole.name, scopes}}
    );
}

export async function deleteCustomRole(apiContext: APIRequestContext, customRole: CustomRoleI): Promise<void> {
    await graphql(apiContext, 'mutation DeleteCustomRole($id: ID!) { deleteCustomRole(id: $id) }', {
        id: customRole.id,
    }).catch(() => undefined);
}

export async function createCustomRoleUser(
    apiContext: APIRequestContext,
    baseURL: string,
    workspace: RbacWorkspaceI,
    customRole: CustomRoleI
): Promise<RoleUserI> {
    const userLogin = `rbac_custom_${getRandomString()}`;

    const email = `${userLogin}@localhost.com`;

    const data = await graphql<{inviteWorkspaceUser: {userId: string}}>(
        apiContext,
        `
            mutation InviteWorkspaceUser($workspaceId: ID!, $email: String!, $customRoleId: ID) {
                inviteWorkspaceUser(workspaceId: $workspaceId, email: $email, customRoleId: $customRoleId) {
                    userId
                }
            }
        `,
        {customRoleId: customRole.id, email, workspaceId: String(workspace.id)}
    );

    await claimAccount(baseURL, email);

    return {
        email,
        login: userLogin,
        password: ROLE_USER_PASSWORD,
        role: customRole.name,
        userId: data.inviteWorkspaceUser.userId,
    };
}

export type EnvironmentType = 'DEVELOPMENT' | 'PRODUCTION' | 'STAGING';

const ENVIRONMENT_IDS: Record<EnvironmentType, number> = {
    DEVELOPMENT: 0,
    PRODUCTION: 2,
    STAGING: 1,
};

export async function setEnvironmentRole(
    apiContext: APIRequestContext,
    workspace: RbacWorkspaceI,
    roleUser: RoleUserI,
    environment: EnvironmentType,
    role: BuiltInRoleType
): Promise<void> {
    const userData = await graphql<{user: {id: string}}>(
        apiContext,
        'query User($login: String!) { user(login: $login) { id } }',
        {login: roleUser.login}
    );

    await graphql(
        apiContext,
        `
            mutation SetWorkspaceUserEnvironmentRole(
                $workspaceId: ID!
                $userId: ID!
                $environment: EnvironmentEnum!
                $role: WorkspaceRole
            ) {
                setWorkspaceUserEnvironmentRole(
                    workspaceId: $workspaceId
                    userId: $userId
                    environment: $environment
                    role: $role
                ) {
                    userId
                }
            }
        `,
        {environment, role, userId: userData.user.id, workspaceId: String(workspace.id)}
    );
}

async function preselectWorkspaceAndEnvironment(
    context: BrowserContext,
    workspaceId: number,
    environment: EnvironmentType
): Promise<void> {
    await context.addInitScript(
        ({environmentId, selectedWorkspaceId}) => {
            window.localStorage.setItem(
                'bytechef.workspace',
                JSON.stringify({state: {currentWorkspaceId: selectedWorkspaceId}, version: 0})
            );

            window.localStorage.setItem(
                'bytechef.environment',
                JSON.stringify({state: {currentEnvironmentId: environmentId}, version: 0})
            );
        },
        {environmentId: ENVIRONMENT_IDS[environment], selectedWorkspaceId: workspaceId}
    );
}

export async function openAsRoleUser(
    browser: Browser,
    baseURL: string,
    roleUser: RoleUserI,
    workspace: RbacWorkspaceI,
    environment: EnvironmentType = 'DEVELOPMENT'
): Promise<{context: BrowserContext; page: Page}> {
    const context = await browser.newContext({baseURL});

    await preselectWorkspaceAndEnvironment(context, workspace.id, environment);

    const page = await context.newPage();

    await login(page, roleUser.email, roleUser.password);

    return {context, page};
}

export async function openAsTenantAdmin(
    browser: Browser,
    baseURL: string,
    workspace?: RbacWorkspaceI
): Promise<{context: BrowserContext; page: Page}> {
    const context = await browser.newContext({baseURL});

    if (workspace) {
        await preselectWorkspaceAndEnvironment(context, workspace.id, 'DEVELOPMENT');
    }

    const page = await context.newPage();

    await login(page, ADMIN_EMAIL, ADMIN_PASSWORD);

    return {context, page};
}

export async function expectPresence(
    page: Page,
    locatorName: RegExp | string,
    present: boolean,
    role: 'button' | 'menuitem'
): Promise<void> {
    const locator = page.getByRole(role, {exact: typeof locatorName === 'string', name: locatorName}).first();

    if (present) {
        await expect(locator).toBeVisible();
    } else {
        await expect(locator).toHaveCount(0);
    }
}

export async function requestWorkflowUpdateAs(page: Page, workflowId: string): Promise<number> {
    const cookies = await page.context().cookies();

    const xsrfToken = cookies.find((cookie) => cookie.name === 'XSRF-TOKEN')?.value || '';

    const workflowResponse = await page.request.get(`/api/automation/internal/workflows/${workflowId}`);

    const workflow = await workflowResponse.json();

    const updateResponse = await page.request.put(`/api/automation/internal/workflows/${workflowId}`, {
        data: {__version: workflow.__version, definition: workflow.definition},
        headers: {'X-XSRF-TOKEN': xsrfToken},
    });

    return updateResponse.status();
}

export async function openPage(page: Page, path: string, readyText: RegExp): Promise<void> {
    await page.goto(path);

    await expect(page.getByText(readyText).filter({visible: true}).first()).toBeVisible({timeout: 30000});
}
