import {type BrowserContext, expect} from '@playwright/test';

import {workspaceRolesTest as test} from '../../fixtures/workspaceRoles';
import {
    type CustomRoleI,
    RBAC_BASE_URL,
    type RoleUserI,
    createCustomRole,
    createCustomRoleUser,
    deleteCustomRole,
    deleteRoleUser,
    openAsRoleUser,
    openAsTenantAdmin,
    postGraphQl,
} from '../../utils/rbacUtils';

test.describe('Denied requests in the client', () => {
    let context: BrowserContext | undefined;
    let customRole: CustomRoleI | undefined;
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

    test('a refused REST call shows an Access denied toast, is not retried and keeps the member signed in', async ({
        adminApi,
        browser,
        rbacWorkspace,
    }) => {
        customRole = await createCustomRole(adminApi, ['WORKFLOW_VIEW']);
        roleUser = await createCustomRoleUser(adminApi, RBAC_BASE_URL, rbacWorkspace, customRole);

        let page;

        ({context, page} = await openAsRoleUser(browser, RBAC_BASE_URL, roleUser, rbacWorkspace));

        const refusedRequests: string[] = [];

        page.on('response', (response) => {
            if (response.status() === 403 && response.url().includes('/api/automation/internal/project-deployments')) {
                refusedRequests.push(response.url());
            }
        });

        await page.goto('/automation/deployments');

        const toast = page.locator('[data-sonner-toast]').filter({hasText: 'Access denied'});

        await expect(toast).toBeVisible({timeout: 30000});
        await expect(toast).toContainText('You do not have permission to perform this action.');

        await page.waitForTimeout(3000);

        const refusedUrls = new Set(refusedRequests);

        expect(refusedRequests.length).toBeLessThanOrEqual(refusedUrls.size * 2);

        await expect(page).not.toHaveURL(/\/login/);

        await page.goto('/automation/projects');

        await expect(page.getByText(rbacWorkspace.projectName).first()).toBeVisible({timeout: 30000});
        await expect(page).not.toHaveURL(/\/login/);
    });

    test('a custom role that grants no scope is refused so no member can be locked out by it', async ({adminApi}) => {
        const response = await postGraphQl(
            adminApi,
            `
                mutation CreateCustomRole($input: CreateCustomRoleInput!) {
                    createCustomRole(input: $input) {
                        id
                    }
                }
            `,
            {input: {name: `rbac_empty_role_${Date.now()}`, scopes: []}}
        );

        expect(response.data).toBeNull();
        expect(response.errors?.[0]?.extensions).toMatchObject({classification: 'BAD_REQUEST'});
        expect(response.errors?.[0]?.message).toContain("'scopes' must contain at least one permission");
    });
});

test.describe('Tenant-admin-only settings', () => {
    test('a workspace admin does not see the tenant settings entries and is refused on their pages', async ({
        browser,
        rbacWorkspace,
        roleUsers,
    }) => {
        const {context, page} = await openAsRoleUser(browser, RBAC_BASE_URL, roleUsers.ADMIN, rbacWorkspace);

        try {
            await page.goto('/automation/settings');

            await expect(page).toHaveURL(/\/automation\/settings\/workspace-users/, {timeout: 30000});

            for (const entry of ['Workspaces', 'Audit Events', 'Notifications', 'Roles', 'API Connectors']) {
                await expect(page.getByRole('link', {exact: true, name: entry})).toHaveCount(0);
            }

            for (const path of ['users', 'global-custom-roles', 'audit-events', 'api-connectors']) {
                await page.goto(`/automation/settings/${path}`);

                await expect(page.getByText('You are not authorized to access this page.')).toBeVisible();
            }

            await page.goto('/automation/api-platform/api-clients');

            await expect(page.getByText('You are not authorized to access this page.')).toBeVisible();
        } finally {
            await context.close();
        }
    });

    test('a tenant admin is sent to Workspaces from the settings index', async ({browser}) => {
        const {context, page} = await openAsTenantAdmin(browser, RBAC_BASE_URL);

        try {
            await page.goto('/automation/settings');

            await expect(page).toHaveURL(/\/automation\/settings\/workspaces/, {timeout: 30000});
        } finally {
            await context.close();
        }
    });
});
