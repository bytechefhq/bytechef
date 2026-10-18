import {type APIRequestContext, expect} from '@playwright/test';

import {workspaceRolesTest as test} from '../../fixtures/workspaceRoles';
import {ROUTES} from '../../utils/constants';
import {RBAC_BASE_URL, openAsTenantAdmin} from '../../utils/rbacUtils';
import {
    HTTP_CLIENT_WORKFLOW_NODE,
    connectionName,
    createConnection,
    createHttpClientProject,
    createOrganizationConnectionId,
    createUserApiContext,
    deleteOrganizationConnection,
    deployHttpClientProjectResponse,
    expectAccessDenied,
    expectNoGraphQlErrors,
    saveTestConfigurationConnection,
} from '../../utils/visibilityUtils';

test.describe('Connection picker in the workflow editor', () => {
    let editorApi: APIRequestContext;
    let organizationConnectionId: number;
    let organizationConnectionName: string;

    test.beforeAll(async ({adminApi, workerRoleUsers}) => {
        editorApi = await createUserApiContext(workerRoleUsers.EDITOR.email, workerRoleUsers.EDITOR.password);

        organizationConnectionName = connectionName('vis_picker_org');

        organizationConnectionId = await createOrganizationConnectionId(adminApi, {name: organizationConnectionName});
    });

    test.afterAll(async ({adminApi}) => {
        await editorApi.dispose();

        await deleteOrganizationConnection(adminApi, organizationConnectionId);
    });

    test('the picker groups connections by scope, Private then Workspace then Organization, each with its badge', async ({
        adminApi,
        browser,
        rbacWorkspace,
    }) => {
        const privateName = connectionName('vis_picker_private');
        const workspaceName = connectionName('vis_picker_workspace');

        await createConnection(adminApi, rbacWorkspace.id, {name: privateName, visibility: 'PRIVATE'});
        await createConnection(editorApi, rbacWorkspace.id, {name: workspaceName});

        const {projectId, projectWorkflowId} = await createHttpClientProject(adminApi, rbacWorkspace.id);

        const {context, page} = await openAsTenantAdmin(browser, RBAC_BASE_URL, rbacWorkspace);

        try {
            await page.goto(ROUTES.projectWorkflows(projectId, projectWorkflowId));

            await page.getByRole('button', {exact: true, name: `${HTTP_CLIENT_WORKFLOW_NODE} node`}).click();

            await page.getByRole('button', {exact: true, name: 'Connection'}).click();

            await page.getByRole('combobox').filter({hasText: 'Choose Connection...'}).click();

            const listbox = page.getByRole('listbox');

            await expect(listbox.getByRole('group')).toHaveText([/^Private/, /^Workspace/, /^Organization/]);

            await expect(
                listbox.getByRole('group', {name: 'Private'}).getByRole('option', {name: privateName})
            ).toContainText('Private');
            await expect(
                listbox.getByRole('group', {name: 'Workspace'}).getByRole('option', {name: workspaceName})
            ).toContainText('Workspace');
            await expect(
                listbox
                    .getByRole('group', {name: 'Organization'})
                    .getByRole('option', {name: organizationConnectionName})
            ).toContainText('Organization');
        } finally {
            await context.close();
        }
    });

    test("a member attaches a WORKSPACE connection to a project workflow but not a colleague's PRIVATE one", async ({
        adminApi,
        rbacWorkspace,
    }) => {
        const workspaceConnectionId = await createConnection(adminApi, rbacWorkspace.id);
        const privateConnectionId = await createConnection(adminApi, rbacWorkspace.id, {visibility: 'PRIVATE'});

        const {workflowId} = await createHttpClientProject(editorApi, rbacWorkspace.id);

        expectNoGraphQlErrors(await saveTestConfigurationConnection(editorApi, workflowId, workspaceConnectionId));

        expectAccessDenied(await saveTestConfigurationConnection(editorApi, workflowId, privateConnectionId));
    });

    test('a member attaches an ORGANIZATION connection to a project workflow', async ({rbacWorkspace}) => {
        const {workflowId} = await createHttpClientProject(editorApi, rbacWorkspace.id);

        expectNoGraphQlErrors(await saveTestConfigurationConnection(editorApi, workflowId, organizationConnectionId));
    });

    test('a member deploys a workflow that uses an ORGANIZATION connection', async ({rbacWorkspace}) => {
        const {projectId} = await createHttpClientProject(editorApi, rbacWorkspace.id);

        const {response} = await deployHttpClientProjectResponse(editorApi, projectId, organizationConnectionId);

        expect(response.ok(), await response.text()).toBeTruthy();
    });
});
