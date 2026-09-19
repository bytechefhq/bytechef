import {type BrowserContext, type Page, expect} from '@playwright/test';

import {workspaceRolesTest as test} from '../../fixtures/workspaceRoles';
import {
    RBAC_BASE_URL,
    type RoleUserI,
    createRoleUser,
    deleteRoleUser,
    expectPresence,
    openAsRoleUser,
    openAsTenantAdmin,
    openPage,
    setEnvironmentRole,
} from '../../utils/rbacUtils';

async function expectStillSignedIn(page: Page) {
    await expect(page).not.toHaveURL(/\/login/);

    await expect(page.getByText('Access denied')).toHaveCount(0);
}

test.describe('Member with per-environment roles', () => {
    let context: BrowserContext | undefined;
    let roleUser: RoleUserI | undefined;

    test.afterEach(async ({adminApi}) => {
        await context?.close();

        if (roleUser) {
            await deleteRoleUser(adminApi, roleUser);
        }

        context = undefined;
        roleUser = undefined;
    });

    test('an editor in Development only may deploy there but not in Production', async ({
        adminApi,
        browser,
        rbacWorkspace,
    }) => {
        roleUser = await createRoleUser(adminApi, RBAC_BASE_URL, rbacWorkspace, 'EDITOR');

        await setEnvironmentRole(adminApi, rbacWorkspace, roleUser, 'DEVELOPMENT', 'EDITOR');

        await test.step('Development', async () => {
            const developmentSession = await openAsRoleUser(
                browser,
                RBAC_BASE_URL,
                roleUser!,
                rbacWorkspace,
                'DEVELOPMENT'
            );

            context = developmentSession.context;

            await openPage(developmentSession.page, '/automation/deployments', /deployments/i);

            await expectPresence(developmentSession.page, /^(New|Create) Deployment$/, true, 'button');

            await developmentSession.context.close();
        });

        await test.step('Production is not offered and a stale selection falls back to Development', async () => {
            const productionSession = await openAsRoleUser(
                browser,
                RBAC_BASE_URL,
                roleUser!,
                rbacWorkspace,
                'PRODUCTION'
            );

            context = productionSession.context;

            const page = productionSession.page;

            await page.goto('/automation/deployments');

            await expect
                .poll(
                    () =>
                        page.evaluate(
                            () =>
                                JSON.parse(window.localStorage.getItem('bytechef.environment') || '{}').state
                                    ?.currentEnvironmentId
                        ),
                    {timeout: 30000}
                )
                .toBe(0);

            await expectPresence(page, /^(New|Create) Deployment$/, true, 'button');

            await page
                .getByRole('button', {name: /^DEV(ELOPMENT)?$/})
                .first()
                .click();

            await expect(page.getByRole('menuitemradio').filter({hasText: 'DEVELOPMENT'})).toBeVisible();
            await expect(page.getByRole('menuitemradio').filter({hasText: 'PRODUCTION'})).toHaveCount(0);
            await expect(page.getByRole('menuitemradio').filter({hasText: 'STAGING'})).toHaveCount(0);

            await page.keyboard.press('Escape');

            await expectStillSignedIn(page);
        });
    });

    test('a viewer in Development and an admin in Production gets each environment its own controls', async ({
        adminApi,
        browser,
        rbacWorkspace,
    }) => {
        roleUser = await createRoleUser(adminApi, RBAC_BASE_URL, rbacWorkspace, 'VIEWER');

        await setEnvironmentRole(adminApi, rbacWorkspace, roleUser, 'DEVELOPMENT', 'VIEWER');
        await setEnvironmentRole(adminApi, rbacWorkspace, roleUser, 'PRODUCTION', 'ADMIN');

        await test.step('Development', async () => {
            const developmentSession = await openAsRoleUser(
                browser,
                RBAC_BASE_URL,
                roleUser!,
                rbacWorkspace,
                'DEVELOPMENT'
            );

            context = developmentSession.context;

            await openPage(developmentSession.page, '/automation/deployments', /deployments/i);

            await expectPresence(developmentSession.page, /^(New|Create) Deployment$/, false, 'button');

            await developmentSession.context.close();
        });

        await test.step('Production', async () => {
            const productionSession = await openAsRoleUser(
                browser,
                RBAC_BASE_URL,
                roleUser!,
                rbacWorkspace,
                'PRODUCTION'
            );

            context = productionSession.context;

            await openPage(productionSession.page, '/automation/deployments', /deployments/i);

            await expectPresence(productionSession.page, /^(New|Create) Deployment$/, true, 'button');

            await expectStillSignedIn(productionSession.page);
        });
    });

    test('removing the last environment role keeps that role in every environment', async ({
        adminApi,
        browser,
        rbacWorkspace,
    }) => {
        roleUser = await createRoleUser(adminApi, RBAC_BASE_URL, rbacWorkspace, 'VIEWER');

        await setEnvironmentRole(adminApi, rbacWorkspace, roleUser, 'DEVELOPMENT', 'EDITOR');

        const adminSession = await openAsTenantAdmin(browser, RBAC_BASE_URL, rbacWorkspace);

        context = adminSession.context;

        const page = adminSession.page;

        await openPage(page, '/automation/settings/workspace-users', new RegExp(roleUser.email));

        const memberRow = page.getByRole('row').filter({hasText: roleUser.email});

        await expect(memberRow.getByText('Development')).toBeVisible();

        await memberRow.getByRole('button', {name: 'Remove Development role'}).click();

        const alertDialog = page.getByRole('alertdialog');

        await expect(alertDialog).toContainText('They will keep the Editor role in every environment.');

        await alertDialog.getByRole('button', {name: 'Remove'}).click();

        await expect(memberRow.getByText('Development')).toHaveCount(0);
        await expect(memberRow.getByRole('button', {name: 'Remove Development role'})).toHaveCount(0);
        await expect(memberRow.getByRole('combobox').first()).toContainText('Editor');
    });
});
