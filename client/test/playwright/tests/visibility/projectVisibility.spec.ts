import {type APIRequestContext, type Page, expect} from '@playwright/test';
import {execFileSync} from 'node:child_process';

import {workspaceRolesTest as test} from '../../fixtures/workspaceRoles';
import {
    type BuiltInRoleType,
    RBAC_BASE_URL,
    deleteRoleUser,
    openAsRoleUser,
    openAsTenantAdmin,
} from '../../utils/rbacUtils';
import {
    type HttpClientDeploymentI,
    INTERNAL,
    createAccount,
    createConnection,
    createProjectResponse,
    createUserApiContext,
    deployHttpClientProject,
    expectAccessDenied,
    expectGraphQlError,
    expectNoGraphQlErrors,
    fetchProjectGrants,
    fetchUserId,
    getProjectResponse,
    grantProjectAccess,
    internalApi,
    listDeploymentJobs,
    listWorkspaceProjectDeploymentIds,
    listWorkspaceProjectIds,
    listWorkspaceWorkflowExecutionIds,
    postGraphQl,
    revokeProjectAccess,
    runDeployedWorkflowResponse,
    sendInternalApi,
    setProjectVisibility,
} from '../../utils/visibilityUtils';

const DATABASE_NAME = process.env.PLAYWRIGHT_DB_NAME || 'bytechef';

function countProjectGrantRows(projectId: number): number {
    const output = execFileSync(
        'psql',
        [
            '-h',
            process.env.PLAYWRIGHT_DB_HOST || 'localhost',
            '-U',
            process.env.PLAYWRIGHT_DB_USER || 'postgres',
            '-d',
            DATABASE_NAME,
            '-tAc',
            `SELECT count(*) FROM resource_grant WHERE resource_type = 'Project' AND resource_id = ${projectId}`,
        ],
        {env: {...process.env, PGPASSWORD: process.env.PLAYWRIGHT_DB_PASSWORD || 'postgres'}}
    );

    return Number(output.toString().trim());
}

async function graphQlProject(apiContext: APIRequestContext, projectId: number) {
    return postGraphQl<{project: {id: string; visibility: string} | null}>(
        apiContext,
        'query Project($id: ID!) { project(id: $id) { id visibility } }',
        {id: String(projectId)}
    );
}

async function waitForCompletedJob(
    adminApi: APIRequestContext,
    workspaceId: number,
    deployment: HttpClientDeploymentI
) {
    let jobId = 0;

    await expect(async () => {
        const jobs = await listDeploymentJobs(adminApi, workspaceId, deployment);

        expect(jobs.length).toBeGreaterThan(0);

        jobId = jobs[jobs.length - 1].id;
    }).toPass({timeout: 30000});

    return jobId;
}

function projectRow(page: Page, projectId: number) {
    return page.getByLabel(String(projectId), {exact: true});
}

test.describe('Project visibility and sharing', () => {
    const apis: Partial<Record<BuiltInRoleType, APIRequestContext>> = {};

    let viewerId: string;

    test.beforeAll(async ({adminApi, workerRoleUsers}) => {
        for (const role of ['ADMIN', 'EDITOR', 'VIEWER'] as BuiltInRoleType[]) {
            apis[role] = await createUserApiContext(workerRoleUsers[role].email, workerRoleUsers[role].password);
        }

        viewerId = await fetchUserId(adminApi, workerRoleUsers.VIEWER.login);
    });

    test.afterAll(async () => {
        for (const api of Object.values(apis)) {
            await api.dispose();
        }
    });

    test('a PRIVATE project and its workflows, deployments and executions are hidden from other members', async ({
        adminApi,
        rbacWorkspace,
    }) => {
        const connectionId = await createConnection(apis.EDITOR!, rbacWorkspace.id);
        const deployment = await deployHttpClientProject(apis.EDITOR!, rbacWorkspace.id, connectionId);

        expect((await runDeployedWorkflowResponse(apis.EDITOR!, deployment)).ok()).toBeTruthy();

        const jobId = await waitForCompletedJob(adminApi, rbacWorkspace.id, deployment);

        expectNoGraphQlErrors(
            await setProjectVisibility(apis.EDITOR!, rbacWorkspace.id, deployment.projectId, 'PRIVATE')
        );

        expect((await getProjectResponse(apis.EDITOR!, deployment.projectId)).status()).toBe(200);

        for (const role of ['ADMIN', 'VIEWER'] as BuiltInRoleType[]) {
            const api = apis[role]!;

            expect((await getProjectResponse(api, deployment.projectId)).status(), role).toBe(403);
            expect(await listWorkspaceProjectIds(api, rbacWorkspace.id), role).not.toContain(deployment.projectId);
            expectAccessDenied(await graphQlProject(api, deployment.projectId));

            expect(
                (await sendInternalApi(api, 'GET', `${INTERNAL}/projects/${deployment.projectId}/workflows`)).status(),
                role
            ).toBe(403);
            expect(
                (await sendInternalApi(api, 'GET', `${INTERNAL}/workflows/${deployment.workflowId}`)).status(),
                role
            ).toBe(403);

            expect(
                (
                    await sendInternalApi(
                        api,
                        'GET',
                        `${INTERNAL}/project-deployments/${deployment.projectDeploymentId}`
                    )
                ).status(),
                role
            ).toBe(403);
            expect(await listWorkspaceProjectDeploymentIds(api, rbacWorkspace.id), role).not.toContain(
                deployment.projectDeploymentId
            );

            expect((await sendInternalApi(api, 'GET', `${INTERNAL}/workflow-executions/${jobId}`)).status(), role).toBe(
                403
            );
            expect(await listWorkspaceWorkflowExecutionIds(api, rbacWorkspace.id), role).not.toContain(jobId);
        }
    });

    test('the tenant admin sees a PRIVATE project and everything under it', async ({adminApi, rbacWorkspace}) => {
        const connectionId = await createConnection(apis.EDITOR!, rbacWorkspace.id);
        const deployment = await deployHttpClientProject(apis.EDITOR!, rbacWorkspace.id, connectionId);

        expectNoGraphQlErrors(
            await setProjectVisibility(apis.EDITOR!, rbacWorkspace.id, deployment.projectId, 'PRIVATE')
        );

        expect((await getProjectResponse(adminApi, deployment.projectId)).status()).toBe(200);
        expect(await listWorkspaceProjectIds(adminApi, rbacWorkspace.id)).toContain(deployment.projectId);
        expect(await listWorkspaceProjectDeploymentIds(adminApi, rbacWorkspace.id)).toContain(
            deployment.projectDeploymentId
        );
        expect(
            (await sendInternalApi(adminApi, 'GET', `${INTERNAL}/workflows/${deployment.workflowId}`)).status()
        ).toBe(200);
    });

    test('a WORKSPACE project is visible to every member', async ({rbacWorkspace}) => {
        const response = await createProjectResponse(apis.EDITOR!, rbacWorkspace.id);

        expect(response.ok(), await response.text()).toBeTruthy();

        const projectId = Number(await response.text());

        for (const role of ['ADMIN', 'EDITOR', 'VIEWER'] as BuiltInRoleType[]) {
            expect((await getProjectResponse(apis[role]!, projectId)).status(), role).toBe(200);
            expect(await listWorkspaceProjectIds(apis[role]!, rbacWorkspace.id), role).toContain(projectId);
        }

        const project = await graphQlProject(apis.VIEWER!, projectId);

        expectNoGraphQlErrors(project);
        expect(project.data!.project!.visibility).toBe('WORKSPACE');
    });

    test('a project created PRIVATE is private from the start', async ({rbacWorkspace}) => {
        const response = await createProjectResponse(apis.EDITOR!, rbacWorkspace.id, {visibility: 'PRIVATE'});

        expect(response.ok(), await response.text()).toBeTruthy();

        const projectId = Number(await response.text());

        expect((await getProjectResponse(apis.VIEWER!, projectId)).status()).toBe(403);
        expect(await listWorkspaceProjectIds(apis.EDITOR!, rbacWorkspace.id)).toContain(projectId);
    });

    test('sharing a PRIVATE project with a member grants access and revoking withdraws it', async ({rbacWorkspace}) => {
        const response = await createProjectResponse(apis.EDITOR!, rbacWorkspace.id, {visibility: 'PRIVATE'});
        const projectId = Number(await response.text());

        expect((await getProjectResponse(apis.VIEWER!, projectId)).status()).toBe(403);

        expectNoGraphQlErrors(await grantProjectAccess(apis.EDITOR!, rbacWorkspace.id, projectId, viewerId));

        const grants = await fetchProjectGrants(apis.EDITOR!, rbacWorkspace.id, projectId);

        expectNoGraphQlErrors(grants);
        expect(grants.data!.projectGrants.map(String)).toContain(viewerId);

        expect((await getProjectResponse(apis.VIEWER!, projectId)).status()).toBe(200);
        expect(await listWorkspaceProjectIds(apis.VIEWER!, rbacWorkspace.id)).toContain(projectId);
        expect((await getProjectResponse(apis.ADMIN!, projectId)).status()).toBe(403);

        expectNoGraphQlErrors(await revokeProjectAccess(apis.EDITOR!, rbacWorkspace.id, projectId, viewerId));

        expect((await getProjectResponse(apis.VIEWER!, projectId)).status()).toBe(403);
    });

    test('only the owner or a workspace admin may change sharing, and a non-member cannot be granted', async ({
        adminApi,
        rbacWorkspace,
    }) => {
        const response = await createProjectResponse(apis.EDITOR!, rbacWorkspace.id);
        const projectId = Number(await response.text());

        expectAccessDenied(await setProjectVisibility(apis.VIEWER!, rbacWorkspace.id, projectId, 'PRIVATE'));
        expectAccessDenied(await grantProjectAccess(apis.VIEWER!, rbacWorkspace.id, projectId, viewerId));
        expectAccessDenied(await fetchProjectGrants(apis.VIEWER!, rbacWorkspace.id, projectId));

        expectGraphQlError(
            await setProjectVisibility(apis.EDITOR!, rbacWorkspace.id, projectId, 'ORGANIZATION'),
            /does not support ORGANIZATION visibility/
        );

        const outsider = await createAccount(adminApi);

        try {
            expectGraphQlError(
                await grantProjectAccess(apis.EDITOR!, rbacWorkspace.id, projectId, outsider.userId!),
                /not a member of workspace/
            );
        } finally {
            await deleteRoleUser(adminApi, outsider);
        }

        expectNoGraphQlErrors(await setProjectVisibility(apis.ADMIN!, rbacWorkspace.id, projectId, 'PRIVATE'));
        expectNoGraphQlErrors(await setProjectVisibility(apis.EDITOR!, rbacWorkspace.id, projectId, 'WORKSPACE'));
    });

    test('deleting a project removes its grants', async ({adminApi, rbacWorkspace}) => {
        const response = await createProjectResponse(apis.EDITOR!, rbacWorkspace.id, {visibility: 'PRIVATE'});
        const projectId = Number(await response.text());

        expectNoGraphQlErrors(await grantProjectAccess(apis.EDITOR!, rbacWorkspace.id, projectId, viewerId));

        expect(countProjectGrantRows(projectId)).toBe(1);

        await internalApi(adminApi, 'DELETE', `/projects/${projectId}`);

        expect(countProjectGrantRows(projectId)).toBe(0);
    });

    test('the owner changes visibility from the project list picker and other members see a read-only badge', async ({
        browser,
        rbacWorkspace,
        workerRoleUsers,
    }) => {
        const name = `vis_ui_project_${Date.now()}`;
        const response = await createProjectResponse(apis.EDITOR!, rbacWorkspace.id, {name});
        const projectId = Number(await response.text());

        const owner = await openAsRoleUser(browser, RBAC_BASE_URL, workerRoleUsers.EDITOR, rbacWorkspace);

        try {
            await owner.page.goto('/automation/projects');

            const row = projectRow(owner.page, projectId);

            await expect(row).toBeVisible({timeout: 30000});

            await row.getByRole('button', {name: 'Change visibility'}).click();

            await owner.page.getByRole('radio', {name: 'Private'}).click();

            await expect
                .poll(async () => (await graphQlProject(apis.EDITOR!, projectId)).data?.project?.visibility, {
                    timeout: 10000,
                })
                .toBe('PRIVATE');
        } finally {
            await owner.context.close();
        }

        expectNoGraphQlErrors(await setProjectVisibility(apis.EDITOR!, rbacWorkspace.id, projectId, 'WORKSPACE'));

        const member = await openAsRoleUser(browser, RBAC_BASE_URL, workerRoleUsers.VIEWER, rbacWorkspace);

        try {
            await member.page.goto('/automation/projects');

            const row = projectRow(member.page, projectId);

            await expect(row).toBeVisible({timeout: 30000});
            await expect(row.getByText('Workspace', {exact: true})).toBeVisible();
            await expect(row.getByRole('button', {name: 'Change visibility'})).toHaveCount(0);
        } finally {
            await member.context.close();
        }

        const tenantAdmin = await openAsTenantAdmin(browser, RBAC_BASE_URL, rbacWorkspace);

        try {
            await tenantAdmin.page.goto('/automation/projects');

            const row = projectRow(tenantAdmin.page, projectId);

            await expect(row).toBeVisible({timeout: 30000});
            await expect(row.getByRole('button', {name: 'Change visibility'})).toBeVisible();
        } finally {
            await tenantAdmin.context.close();
        }
    });
});
