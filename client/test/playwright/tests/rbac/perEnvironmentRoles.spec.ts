import {type BrowserContext, type Page, expect} from '@playwright/test';

import {workspaceRolesTest as test} from '../../fixtures/workspaceRoles';
import {
    PAGE_BODY_TEXT,
    RBAC_BASE_URL,
    type RoleUserI,
    createRoleUser,
    deleteRoleUser,
    expectPresence,
    openAsRoleUser,
    openAsTenantAdmin,
    openPage,
    postGraphQl,
    publishRbacWorkspaceProject,
    setEnvironmentRole,
} from '../../utils/rbacUtils';

const WORKSPACE_USERS = `
    query WorkspaceUsers($workspaceId: ID!) {
        workspaceUsers(workspaceId: $workspaceId) {
            customRoleId
            environment
            userId
            workspaceRole
        }
    }
`;

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

        await publishRbacWorkspaceProject(adminApi, rbacWorkspace);

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

            await openPage(developmentSession.page, '/automation/deployments', PAGE_BODY_TEXT.deployments);

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

        await publishRbacWorkspaceProject(adminApi, rbacWorkspace);

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

            await openPage(developmentSession.page, '/automation/deployments', PAGE_BODY_TEXT.deployments);

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

            await openPage(productionSession.page, '/automation/deployments', PAGE_BODY_TEXT.deployments);

            await expectPresence(productionSession.page, /^(New|Create) Deployment$/, true, 'button');

            await expectStillSignedIn(productionSession.page);
        });
    });

    test('removing the last environment role turns it into a workspace-wide role', async ({
        adminApi,
        browser,
        rbacWorkspace,
    }) => {
        roleUser = await createRoleUser(adminApi, RBAC_BASE_URL, rbacWorkspace, 'VIEWER');

        await publishRbacWorkspaceProject(adminApi, rbacWorkspace);

        await setEnvironmentRole(adminApi, rbacWorkspace, roleUser, 'DEVELOPMENT', 'EDITOR');
        await setEnvironmentRole(adminApi, rbacWorkspace, roleUser, 'STAGING', 'VIEWER');

        const adminSession = await openAsTenantAdmin(browser, RBAC_BASE_URL, rbacWorkspace);

        context = adminSession.context;

        const page = adminSession.page;

        await openPage(page, '/automation/settings/workspace-users', new RegExp(roleUser.email));

        const memberRow = page.getByRole('row').filter({hasText: roleUser.email});
        const alertDialog = page.getByRole('alertdialog');

        await test.step('removing one of two environment roles denies only that environment', async () => {
            await expect(memberRow.getByText('Development')).toBeVisible();
            await expect(memberRow.getByText('Staging')).toBeVisible();

            await memberRow.getByRole('button', {name: 'Remove Staging role'}).click();

            await expect(alertDialog).toContainText('Remove the Staging role?');
            await expect(alertDialog).toContainText(
                'They will lose access to Staging. Their roles in the other environments are unchanged.'
            );

            await alertDialog.getByRole('button', {name: 'Remove'}).click();

            await expect(memberRow.getByText('Staging')).toHaveCount(0);
            await expect(memberRow.getByRole('combobox', {name: 'Development role'})).toContainText('Editor');
        });

        await test.step('removing the last environment role keeps it in every environment', async () => {
            await memberRow.getByRole('button', {name: 'Remove Development role'}).click();

            await expect(alertDialog).toContainText('Remove the Development role?');
            await expect(alertDialog).toContainText(
                'This is their only environment role, so it becomes a workspace-wide role. They will keep the ' +
                    'Editor role in every environment.'
            );

            await alertDialog.getByRole('button', {name: 'Remove'}).click();

            await expect(memberRow.getByText('Development')).toHaveCount(0);
            await expect(memberRow.getByText('All environments')).toBeVisible();
            await expect(memberRow.getByRole('combobox').first()).toContainText('Editor');
        });

        await test.step('the server holds one workspace-wide Editor row', async () => {
            const response = await postGraphQl<{
                workspaceUsers: {
                    customRoleId: string | null;
                    environment: string | null;
                    userId: string;
                    workspaceRole: string | null;
                }[];
            }>(adminApi, WORKSPACE_USERS, {workspaceId: String(rbacWorkspace.id)});

            expect(response.errors).toBeUndefined();

            const memberRows = response.data!.workspaceUsers.filter(
                (workspaceUser) => workspaceUser.userId === roleUser!.userId
            );

            expect(memberRows).toEqual([
                expect.objectContaining({customRoleId: null, environment: null, workspaceRole: 'EDITOR'}),
            ]);
        });

        await adminSession.context.close();

        for (const environment of ['STAGING', 'PRODUCTION'] as const) {
            await test.step(`the member keeps the Editor role in ${environment}`, async () => {
                const roleUserSession = await openAsRoleUser(
                    browser,
                    RBAC_BASE_URL,
                    roleUser!,
                    rbacWorkspace,
                    environment
                );

                context = roleUserSession.context;

                await openPage(roleUserSession.page, '/automation/deployments', PAGE_BODY_TEXT.deployments);

                await expect
                    .poll(() =>
                        roleUserSession.page.evaluate(
                            () =>
                                JSON.parse(window.localStorage.getItem('bytechef.environment') || '{}').state
                                    ?.currentEnvironmentId
                        )
                    )
                    .toBe(environment === 'STAGING' ? 1 : 2);

                await expectPresence(roleUserSession.page, /^(New|Create) Deployment$/, true, 'button');

                await expectStillSignedIn(roleUserSession.page);

                await roleUserSession.context.close();
            });
        }
    });
});
