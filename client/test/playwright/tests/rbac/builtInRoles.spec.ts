import {type Page, expect} from '@playwright/test';

import {workspaceRolesTest as test} from '../../fixtures/workspaceRoles';
import {type BuiltInRoleType, RBAC_BASE_URL, expectPresence, openAsRoleUser, openPage} from '../../utils/rbacUtils';

interface RoleExpectationsI {
    createDataTable: boolean;
    createDeployment: boolean;
    createMcpServer: boolean;
    createProject: boolean;
    createWorkflow: boolean;
    deleteProject: boolean;
    duplicateProject: boolean;
    editProject: boolean;
    gitConfiguration: boolean;
    inviteMember: boolean;
    pullProjectFromGit: boolean;
    shareProject: boolean;
}

const ROLE_EXPECTATIONS: Record<BuiltInRoleType, RoleExpectationsI> = {
    ADMIN: {
        createDataTable: true,
        createDeployment: true,
        createMcpServer: true,
        createProject: true,
        createWorkflow: true,
        deleteProject: true,
        duplicateProject: true,
        editProject: true,
        gitConfiguration: true,
        inviteMember: true,
        pullProjectFromGit: true,
        shareProject: true,
    },
    EDITOR: {
        createDataTable: true,
        createDeployment: true,
        createMcpServer: true,
        createProject: true,
        createWorkflow: true,
        deleteProject: false,
        duplicateProject: true,
        editProject: true,
        gitConfiguration: false,
        inviteMember: false,
        pullProjectFromGit: false,
        shareProject: false,
    },
    VIEWER: {
        createDataTable: false,
        createDeployment: false,
        createMcpServer: false,
        createProject: false,
        createWorkflow: false,
        deleteProject: false,
        duplicateProject: false,
        editProject: false,
        gitConfiguration: false,
        inviteMember: false,
        pullProjectFromGit: false,
        shareProject: false,
    },
};

test.describe.configure({mode: 'default'});

for (const role of ['VIEWER', 'EDITOR', 'ADMIN'] as BuiltInRoleType[]) {
    const expectations = ROLE_EXPECTATIONS[role];

    test.describe(`${role} workspace member`, () => {
        let page: Page;

        test.beforeEach(async ({browser, rbacWorkspace, roleUsers}) => {
            ({page} = await openAsRoleUser(browser, RBAC_BASE_URL, roleUsers[role], rbacWorkspace));
        });

        test.afterEach(async () => {
            await page?.context().close();
        });

        test('projects page offers only the project actions the role holds', async ({rbacWorkspace}) => {
            await openPage(page, '/automation/projects', new RegExp(rbacWorkspace.projectName));

            await test.step('create project', async () => {
                await expect(page.locator('button[aria-label="Create Project"]')).toHaveCount(
                    expectations.createProject ? 1 : 0
                );
            });

            const projectItem = page.getByLabel(String(rbacWorkspace.projectId), {exact: true});

            await test.step('create workflow', async () => {
                await expect(projectItem.getByRole('button', {name: 'Create Workflow'})).toHaveCount(
                    expectations.createWorkflow ? 1 : 0
                );
            });

            await test.step('project menu', async () => {
                await page.getByTestId(`${rbacWorkspace.projectId}-moreProjectActionsButton`).click();

                await expect(page.getByRole('menuitem', {name: 'Export Project'})).toBeVisible();

                await expectPresence(page, 'Edit Project', expectations.editProject, 'menuitem');
                await expectPresence(page, 'Duplicate Project', expectations.duplicateProject, 'menuitem');
                await expectPresence(page, 'Share Project', expectations.shareProject, 'menuitem');

                if (!expectations.pullProjectFromGit) {
                    await expectPresence(page, 'Pull Project from Git', false, 'menuitem');
                }

                if (!expectations.gitConfiguration) {
                    await expectPresence(page, 'Git Configuration', false, 'menuitem');
                }

                await expectPresence(page, 'Delete Project', expectations.deleteProject, 'menuitem');

                await page.keyboard.press('Escape');
            });
        });

        test('deployments page offers create only to roles that may deploy', async () => {
            await openPage(page, '/automation/deployments', /deployments/i);

            await expectPresence(page, /^(New|Create) Deployment$/, expectations.createDeployment, 'button');
        });

        test('MCP servers page offers create only to roles that may create servers', async () => {
            await openPage(page, '/automation/mcp-servers', /MCP Server/i);

            await expectPresence(page, /^(New|Create) MCP Server$/, expectations.createMcpServer, 'button');
        });

        test('data tables page offers create only to roles that may create tables', async () => {
            await openPage(page, '/automation/datatables', /table/i);

            await expectPresence(page, /^(New|Create) Table$/, expectations.createDataTable, 'button');
        });

        test('connections page offers create to every role', async () => {
            await openPage(page, '/automation/connections', /connection/i);

            await expectPresence(page, /^(New|Create) Connection$/, true, 'button');
        });

        test('workspace users page offers invites only to workspace admins', async () => {
            await openPage(page, '/automation/settings/workspace-users', /users/i);

            await expectPresence(page, 'Invite User', expectations.inviteMember, 'button');
        });
    });
}
