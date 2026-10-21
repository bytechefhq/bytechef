import {type APIRequestContext, type Page, expect} from '@playwright/test';

import {embeddedRolesTest as test} from '../../fixtures/embeddedRoles';
import {TEST_USER} from '../../utils/constants';
import {
    fetchMyEmbeddedScopes,
    openAsEmbeddedRoleUser,
    removeEmbeddedUser,
    setEmbeddedOnlyEnvironmentRole,
    setEmbeddedRole,
} from '../../utils/embeddedRbacUtils';
import {
    ENVIRONMENT_IDS,
    type EnvironmentType,
    RBAC_BASE_URL,
    type RoleUserI,
    createAccount,
    createRoleUserApiContext,
    deleteRoleUser,
    graphql,
    openAsTenantAdmin,
} from '../../utils/rbacUtils';

const ALL_ENVIRONMENTS: EnvironmentType[] = ['DEVELOPMENT', 'STAGING', 'PRODUCTION'];

const INHERITED_ADMIN_TEXT = 'Admin - inherited from tenant admin';

const SPLIT_HINT_TEXT = 'They will lose access to every other environment until you grant it.';

interface EmbeddedUserI {
    embeddedRole: string | null;
    environment: string | null;
    inherited: boolean;
    userId: string;
}

async function fetchEmbeddedUsers(adminApi: APIRequestContext): Promise<EmbeddedUserI[]> {
    const {embeddedUsers} = await graphql<{embeddedUsers: EmbeddedUserI[]}>(
        adminApi,
        'query EmbeddedUsers { embeddedUsers { userId embeddedRole environment inherited } }',
        {}
    );

    return embeddedUsers;
}

async function openMembersPage(page: Page): Promise<void> {
    await page.goto('/embedded/settings/members');

    await expect(page.getByRole('columnheader', {name: 'Email'})).toBeVisible({timeout: 30_000});
}

test.describe('Embedded membership', () => {
    test.describe.configure({mode: 'serial'});

    const accounts: RoleUserI[] = [];

    const createMemberAccount = async (adminApi: APIRequestContext): Promise<RoleUserI> => {
        const account = await createAccount(adminApi, RBAC_BASE_URL);

        accounts.push(account);

        return account;
    };

    test.afterAll(async ({adminApi}) => {
        for (const account of accounts) {
            await removeEmbeddedUser(adminApi, account.userId!).catch(() => undefined);

            await deleteRoleUser(adminApi, account).catch((error: unknown) => {
                console.warn(`membership teardown: deleting ${account.login} failed: ${error}`);
            });
        }
    });

    test('an embedded ADMIN adds an existing account by email and removes it', async ({
        adminApi,
        browser,
        embeddedRoleUsers,
    }) => {
        const account = await createMemberAccount(adminApi);

        const memberApi = await createRoleUserApiContext(RBAC_BASE_URL, account);

        const {context, page} = await openAsEmbeddedRoleUser(
            browser,
            RBAC_BASE_URL,
            embeddedRoleUsers.ADMIN,
            ENVIRONMENT_IDS.DEVELOPMENT
        );

        try {
            await openMembersPage(page);

            const memberRow = page.getByRole('row').filter({hasText: account.email});

            await test.step('add by email', async () => {
                await page.getByRole('button', {name: 'Add member'}).click();

                const addDialog = page.getByRole('dialog');

                await addDialog.getByLabel('Email').fill(account.email);

                await addDialog.getByRole('combobox', {name: 'Role'}).click();

                await page.getByRole('option', {exact: true, name: 'Viewer'}).click();

                await addDialog.getByRole('button', {exact: true, name: 'Add'}).click();

                await expect(memberRow).toBeVisible();
                await expect(memberRow.getByText('All environments')).toBeVisible();
                await expect(memberRow.getByRole('combobox').first()).toContainText('Viewer');

                const embeddedUsers = await fetchEmbeddedUsers(adminApi);

                expect(embeddedUsers.filter((embeddedUser) => embeddedUser.userId === account.userId)).toEqual([
                    expect.objectContaining({embeddedRole: 'VIEWER', environment: null, inherited: false}),
                ]);

                for (const environment of ALL_ENVIRONMENTS) {
                    expect(await fetchMyEmbeddedScopes(memberApi, environment), environment).toContain('EMBEDDED_VIEW');
                }
            });

            await test.step('remove the member', async () => {
                await memberRow.getByRole('button', {name: `Remove ${account.email} from embedded`}).click();

                const alertDialog = page.getByRole('alertdialog');

                await expect(alertDialog).toContainText(`Remove ${account.email} from embedded?`);

                await alertDialog.getByRole('button', {name: 'Remove'}).click();

                await expect(memberRow).toHaveCount(0);

                const embeddedUsers = await fetchEmbeddedUsers(adminApi);

                expect(embeddedUsers.map((embeddedUser) => embeddedUser.userId)).not.toContain(account.userId);

                expect(await fetchMyEmbeddedScopes(memberApi)).toEqual([]);
            });
        } finally {
            await memberApi.dispose();

            await context.close();
        }
    });

    test('an embedded ADMIN changes, grants and removes per-environment roles', async ({
        adminApi,
        browser,
        embeddedRoleUsers,
    }) => {
        const account = await createMemberAccount(adminApi);

        await setEmbeddedOnlyEnvironmentRole(adminApi, account.userId!, 'PRODUCTION', 'VIEWER');

        const memberApi = await createRoleUserApiContext(RBAC_BASE_URL, account);

        const {context, page} = await openAsEmbeddedRoleUser(
            browser,
            RBAC_BASE_URL,
            embeddedRoleUsers.ADMIN,
            ENVIRONMENT_IDS.DEVELOPMENT
        );

        try {
            await openMembersPage(page);

            const memberRow = page.getByRole('row').filter({hasText: account.email});

            await test.step('the member holds a role only in Production', async () => {
                await expect(memberRow.getByRole('combobox', {name: 'Production role'})).toContainText('Viewer');
                await expect(memberRow.getByRole('combobox', {name: 'Development role'})).toHaveCount(0);

                expect(await fetchMyEmbeddedScopes(memberApi, 'DEVELOPMENT')).toEqual([]);
                expect(await fetchMyEmbeddedScopes(memberApi, 'PRODUCTION')).not.toContain('CONNECTION_EDIT');
            });

            await test.step('raise the Production role to Editor', async () => {
                await memberRow.getByRole('combobox', {name: 'Production role'}).click();

                await page.getByRole('option', {exact: true, name: 'Editor'}).click();

                await expect(memberRow.getByRole('combobox', {name: 'Production role'})).toContainText('Editor');

                await expect.poll(() => fetchMyEmbeddedScopes(memberApi, 'PRODUCTION')).toContain('CONNECTION_EDIT');

                expect(await fetchMyEmbeddedScopes(memberApi, 'DEVELOPMENT')).toEqual([]);
            });

            await test.step('grant a Development role', async () => {
                await memberRow.getByRole('combobox', {name: 'Grant a role in another environment'}).click();

                await page.getByRole('option', {exact: true, name: 'Development'}).click();

                await expect(memberRow.getByRole('combobox', {name: 'Development role'})).toBeVisible();

                await expect.poll(() => fetchMyEmbeddedScopes(memberApi, 'DEVELOPMENT')).toContain('EMBEDDED_VIEW');

                expect(await fetchMyEmbeddedScopes(memberApi, 'STAGING')).toEqual([]);
            });

            await test.step('remove the Development role again', async () => {
                await memberRow.getByRole('button', {name: 'Remove Development role'}).click();

                const alertDialog = page.getByRole('alertdialog');

                await expect(alertDialog).toContainText('Remove the Development role?');

                await alertDialog.getByRole('button', {name: 'Remove'}).click();

                await expect(memberRow.getByRole('combobox', {name: 'Development role'})).toHaveCount(0);

                await expect.poll(() => fetchMyEmbeddedScopes(memberApi, 'DEVELOPMENT')).toEqual([]);

                expect(await fetchMyEmbeddedScopes(memberApi, 'PRODUCTION')).toContain('CONNECTION_EDIT');
            });
        } finally {
            await memberApi.dispose();

            await context.close();
        }
    });

    test('tenant admins are listed as inherited admins that nobody can change or remove', async ({
        browser,
        embeddedRoleUsers,
    }) => {
        for (const roleKey of ['ADMIN', 'VIEWER'] as const) {
            await test.step(`as the embedded ${roleKey}`, async () => {
                const {context, page} = await openAsEmbeddedRoleUser(
                    browser,
                    RBAC_BASE_URL,
                    embeddedRoleUsers[roleKey],
                    ENVIRONMENT_IDS.DEVELOPMENT
                );

                try {
                    await openMembersPage(page);

                    const tenantAdminRow = page.getByRole('row').filter({hasText: TEST_USER.email});

                    await expect(tenantAdminRow.getByText(INHERITED_ADMIN_TEXT)).toBeVisible();
                    await expect(tenantAdminRow.getByRole('button', {name: /^Remove .* from embedded$/})).toHaveCount(
                        0
                    );
                    await expect(tenantAdminRow.getByRole('combobox')).toHaveCount(0);

                    const viewerRow = page.getByRole('row').filter({hasText: embeddedRoleUsers.VIEWER.email});

                    if (roleKey === 'ADMIN') {
                        await expect(page.getByRole('button', {name: 'Add member'})).toBeVisible();
                        await expect(
                            viewerRow.getByRole('button', {
                                name: `Remove ${embeddedRoleUsers.VIEWER.email} from embedded`,
                            })
                        ).toBeVisible();
                    } else {
                        await expect(page.getByRole('button', {name: 'Add member'})).toHaveCount(0);
                        await expect(page.getByRole('button', {name: /^Remove .* from embedded$/})).toHaveCount(0);
                        await expect(viewerRow.getByRole('combobox').first()).toBeDisabled();
                    }
                } finally {
                    await context.close();
                }
            });
        }
    });

    test('a tenant admin with no embedded row manages members', async ({browser}) => {
        const {context, page} = await openAsTenantAdmin(browser, RBAC_BASE_URL);

        try {
            await openMembersPage(page);

            await expect(page.getByRole('button', {name: 'Add member'})).toBeVisible();
        } finally {
            await context.close();
        }
    });

    test('scopes refresh after a membership change', async ({adminApi, browser}) => {
        const account = await createMemberAccount(adminApi);

        await setEmbeddedRole(adminApi, account.userId!, 'ADMIN');

        const {context, page} = await openAsEmbeddedRoleUser(
            browser,
            RBAC_BASE_URL,
            account,
            ENVIRONMENT_IDS.DEVELOPMENT
        );

        try {
            await test.step('the member’s own change refreshes their scopes without a reload', async () => {
                await openMembersPage(page);

                await expect(page.getByRole('button', {name: 'Add member'})).toBeVisible();

                const ownRow = page.getByRole('row').filter({hasText: account.email});

                await ownRow.getByRole('combobox').first().click();

                await page.getByRole('option', {exact: true, name: 'Viewer'}).click();

                await expect(page.getByRole('button', {name: 'Add member'})).toHaveCount(0, {timeout: 15_000});
                await expect(ownRow.getByRole('combobox').first()).toBeDisabled();
            });

            await test.step('a change made by someone else applies on the member’s next scope fetch', async () => {
                await page.goto('/embedded/connections');

                await page.waitForLoadState('networkidle');

                await expect(page.getByRole('button', {name: /^(New|Create) Connection$/})).toHaveCount(0);

                await graphql(
                    adminApi,
                    `
                        mutation UpdateEmbeddedUserRole($userId: ID!) {
                            updateEmbeddedUserRole(userId: $userId, role: EDITOR) {
                                userId
                            }
                        }
                    `,
                    {userId: account.userId}
                );

                await page.reload();

                await expect(page.getByRole('button', {name: /^(New|Create) Connection$/}).first()).toBeVisible({
                    timeout: 30_000,
                });
            });
        } finally {
            await context.close();
        }
    });

    test('splitting an embedded-wide role into per-environment roles keeps only the chosen environment', async ({
        adminApi,
        browser,
        embeddedRoleUsers,
    }) => {
        const account = await createMemberAccount(adminApi);

        await setEmbeddedRole(adminApi, account.userId!, 'VIEWER');

        const memberApi = await createRoleUserApiContext(RBAC_BASE_URL, account);

        const {context, page} = await openAsEmbeddedRoleUser(
            browser,
            RBAC_BASE_URL,
            embeddedRoleUsers.ADMIN,
            ENVIRONMENT_IDS.DEVELOPMENT
        );

        try {
            await openMembersPage(page);

            const memberRow = page.getByRole('row').filter({hasText: account.email});

            await memberRow.getByRole('button', {name: 'Use per-environment roles'}).click();

            await expect(memberRow.getByText(SPLIT_HINT_TEXT)).toBeVisible();

            await memberRow.getByRole('combobox', {name: 'Environment to grant first'}).click();

            await page.getByRole('option', {exact: true, name: 'Production'}).click();

            await expect(memberRow.getByRole('combobox', {name: 'Production role'})).toContainText('Viewer');
            await expect(memberRow.getByText('All environments')).toHaveCount(0);

            expect(await fetchMyEmbeddedScopes(memberApi, 'PRODUCTION')).toContain('EMBEDDED_VIEW');

            await expect.poll(() => fetchMyEmbeddedScopes(memberApi, 'DEVELOPMENT')).toEqual([]);

            await expect(memberRow.getByRole('combobox', {name: 'Development role'})).toHaveCount(0);
        } finally {
            await memberApi.dispose();

            await context.close();
        }
    });
});
