import {type BrowserContext, type Page, expect} from '@playwright/test';

import {workspaceRolesTest as test} from '../../fixtures/workspaceRoles';
import {
    type CustomRoleI,
    RBAC_BASE_URL,
    type RoleUserI,
    createCustomRole,
    createCustomRoleUser,
    deleteCustomRole,
    deleteRoleUser,
    expectPresence,
    openAsRoleUser,
    openAsTenantAdmin,
    openPage,
    updateCustomRoleScopes,
} from '../../utils/rbacUtils';

test.describe('Custom role member', () => {
    let context: BrowserContext | undefined;
    let customRole: CustomRoleI | undefined;
    let page: Page;
    let roleUser: RoleUserI | undefined;

    test.afterEach(async ({adminApi}) => {
        await context?.close();

        if (roleUser) {
            await deleteRoleUser(adminApi, roleUser);
        }

        if (customRole) {
            await deleteCustomRole(adminApi, customRole);
        }

        context = undefined;
        customRole = undefined;
        roleUser = undefined;
    });

    test('may create projects without authoring workflows', async ({adminApi, browser, rbacWorkspace}) => {
        customRole = await createCustomRole(adminApi, ['WORKFLOW_VIEW', 'PROJECT_CREATE']);
        roleUser = await createCustomRoleUser(adminApi, RBAC_BASE_URL, rbacWorkspace, customRole);

        ({context, page} = await openAsRoleUser(browser, RBAC_BASE_URL, roleUser, rbacWorkspace));

        await openPage(page, '/automation/projects', new RegExp(rbacWorkspace.projectName));

        await expect(page.locator('button[aria-label="Create Project"]')).toHaveCount(1);

        const projectItem = page.getByLabel(String(rbacWorkspace.projectId), {exact: true});

        await expect(projectItem.getByRole('button', {name: 'Create Workflow'})).toHaveCount(0);

        await page.getByTestId(`${rbacWorkspace.projectId}-moreProjectActionsButton`).click();

        await expect(page.getByRole('menuitem', {name: 'Export Project'})).toBeVisible();

        await expectPresence(page, 'Duplicate Project', true, 'menuitem');
        await expectPresence(page, 'Edit Project', false, 'menuitem');
        await expectPresence(page, 'Share Project', false, 'menuitem');
        await expectPresence(page, 'Delete Project', false, 'menuitem');
    });

    test('may deploy without creating projects', async ({adminApi, browser, rbacWorkspace}) => {
        customRole = await createCustomRole(adminApi, [
            'WORKFLOW_VIEW',
            'WORKFLOW_EDIT',
            'DEPLOYMENT_VIEW',
            'DEPLOYMENT_CREATE',
        ]);
        roleUser = await createCustomRoleUser(adminApi, RBAC_BASE_URL, rbacWorkspace, customRole);

        ({context, page} = await openAsRoleUser(browser, RBAC_BASE_URL, roleUser, rbacWorkspace));

        await openPage(page, '/automation/deployments', /deployments/i);

        await expectPresence(page, /^(New|Create) Deployment$/, true, 'button');

        await openPage(page, '/automation/projects', new RegExp(rbacWorkspace.projectName));

        await expect(page.locator('button[aria-label="Create Project"]')).toHaveCount(0);
    });

    test('may create data tables and MCP servers and nothing else', async ({adminApi, browser, rbacWorkspace}) => {
        customRole = await createCustomRole(adminApi, [
            'WORKFLOW_VIEW',
            'DATA_TABLE_VIEW',
            'DATA_TABLE_CREATE',
            'MCP_VIEW',
            'MCP_CREATE',
        ]);
        roleUser = await createCustomRoleUser(adminApi, RBAC_BASE_URL, rbacWorkspace, customRole);

        ({context, page} = await openAsRoleUser(browser, RBAC_BASE_URL, roleUser, rbacWorkspace));

        await openPage(page, '/automation/datatables', /table/i);

        await expectPresence(page, /^(New|Create) Table$/, true, 'button');

        await openPage(page, '/automation/mcp-servers', /MCP Server/i);

        await expectPresence(page, /^(New|Create) MCP Server$/, true, 'button');

        await openPage(page, '/automation/projects', new RegExp(rbacWorkspace.projectName));

        await expect(page.locator('button[aria-label="Create Project"]')).toHaveCount(0);
    });

    test('may manage members without authoring anything', async ({adminApi, browser, rbacWorkspace}) => {
        customRole = await createCustomRole(adminApi, ['WORKFLOW_VIEW', 'WORKSPACE_VIEW', 'WORKSPACE_MEMBER_MANAGE']);
        roleUser = await createCustomRoleUser(adminApi, RBAC_BASE_URL, rbacWorkspace, customRole);

        ({context, page} = await openAsRoleUser(browser, RBAC_BASE_URL, roleUser, rbacWorkspace));

        await openPage(page, '/automation/settings/workspace-users', /users/i);

        await expectPresence(page, 'Invite User', true, 'button');

        await openPage(page, '/automation/projects', new RegExp(rbacWorkspace.projectName));

        await expect(page.locator('button[aria-label="Create Project"]')).toHaveCount(0);
    });

    test('gains a permission once it is added to the role', async ({adminApi, browser, rbacWorkspace}) => {
        customRole = await createCustomRole(adminApi, ['WORKFLOW_VIEW']);
        roleUser = await createCustomRoleUser(adminApi, RBAC_BASE_URL, rbacWorkspace, customRole);

        ({context, page} = await openAsRoleUser(browser, RBAC_BASE_URL, roleUser, rbacWorkspace));

        await openPage(page, '/automation/projects', new RegExp(rbacWorkspace.projectName));

        await expect(page.locator('button[aria-label="Create Project"]')).toHaveCount(0);

        await updateCustomRoleScopes(adminApi, customRole, ['WORKFLOW_VIEW', 'PROJECT_CREATE']);

        await openPage(page, '/automation/projects', new RegExp(rbacWorkspace.projectName));

        await expect(page.locator('button[aria-label="Create Project"]')).toHaveCount(1);
    });
});

test.describe('Roles page', () => {
    test('lets a tenant admin create, rename and delete a custom role', async ({browser}) => {
        const {context, page} = await openAsTenantAdmin(browser, RBAC_BASE_URL);

        const roleName = `rbac_ui_role_${Date.now()}`;
        const renamedRoleName = `${roleName}_renamed`;

        try {
            await openPage(page, '/automation/settings/global-custom-roles', /Roles/);

            await page.getByRole('tab', {name: 'Custom Roles'}).click();

            await test.step('create', async () => {
                await page.getByRole('button', {name: 'Create Role'}).first().click();

                const dialog = page.getByRole('dialog', {name: 'Create Role'});

                await dialog.getByPlaceholder('Role name').fill(roleName);

                await dialog.getByRole('checkbox').first().click();

                await dialog.getByRole('button', {name: 'Create'}).click();

                await expect(page.getByRole('cell', {exact: true, name: roleName})).toBeVisible();
            });

            await test.step('rename', async () => {
                await page.getByRole('button', {name: `Edit the ${roleName} role`}).click();

                const dialog = page.getByRole('dialog', {name: 'Edit Role'});

                await dialog.getByPlaceholder('Role name').fill(renamedRoleName);

                await dialog.getByRole('button', {name: 'Save'}).click();

                await expect(page.getByRole('cell', {exact: true, name: renamedRoleName})).toBeVisible();
            });

            await test.step('delete', async () => {
                await page.getByRole('button', {name: `Delete the ${renamedRoleName} role`}).click();

                const alertDialog = page.getByRole('alertdialog');

                await expect(alertDialog).toContainText('Are you absolutely sure?');

                await alertDialog.getByRole('button', {name: 'Delete'}).click();

                await expect(page.getByRole('cell', {exact: true, name: renamedRoleName})).toHaveCount(0);
            });
        } finally {
            await context.close();
        }
    });
});
