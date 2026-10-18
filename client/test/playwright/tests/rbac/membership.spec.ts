import {expect, request as playwrightRequest} from '@playwright/test';

import {workspaceRolesTest as test} from '../../fixtures/workspaceRoles';
import getRandomString from '../../utils/getRandomString';
import {
    RBAC_BASE_URL,
    type RbacWorkspaceI,
    type RoleUserI,
    createAccount,
    createCustomRole,
    createCustomRoleUser,
    createRbacWorkspace,
    createRoleUserApiContext,
    deleteCustomRole,
    deleteRbacWorkspace,
    deleteRoleUser,
    fetchUserId,
    openAsTenantAdmin,
    postGraphQl,
    readClaimKey,
} from '../../utils/rbacUtils';

const MAILPIT_URL = process.env.PLAYWRIGHT_MAILPIT_URL || 'http://localhost:8025';

const WORKSPACE_USERS = `
    query WorkspaceUsers($workspaceId: ID!) {
        workspaceUsers(workspaceId: $workspaceId) {
            environment
            inherited
            userId
            workspaceRole
            customRoleId
            user {
                email
            }
        }
    }
`;

const ADD_WORKSPACE_USER = `
    mutation AddWorkspaceUser($workspaceId: ID!, $userId: ID!, $role: WorkspaceRole) {
        addWorkspaceUser(workspaceId: $workspaceId, userId: $userId, role: $role) {
            userId
        }
    }
`;

const INVITE_WORKSPACE_USER = `
    mutation InviteWorkspaceUser($workspaceId: ID!, $email: String!, $role: WorkspaceRole) {
        inviteWorkspaceUser(workspaceId: $workspaceId, email: $email, role: $role) {
            userId
        }
    }
`;

const INVITE_USER = 'mutation InviteUser($email: String!, $role: String!) { inviteUser(email: $email, role: $role) }';

const UPDATE_WORKSPACE_USER_ROLE = `
    mutation UpdateWorkspaceUserRole($workspaceId: ID!, $userId: ID!, $role: WorkspaceRole!) {
        updateWorkspaceUserRole(workspaceId: $workspaceId, userId: $userId, role: $role) {
            userId
        }
    }
`;

const REMOVE_WORKSPACE_USER = `
    mutation RemoveWorkspaceUser($workspaceId: ID!, $userId: ID!) {
        removeWorkspaceUser(workspaceId: $workspaceId, userId: $userId)
    }
`;

interface WorkspaceUserI {
    customRoleId: string | null;
    environment: string | null;
    inherited: boolean;
    user: {email: string} | null;
    userId: string;
    workspaceRole: string | null;
}

async function fetchMailCount(email: string): Promise<number> {
    const mailpitContext = await playwrightRequest.newContext({baseURL: MAILPIT_URL});

    try {
        const response = await mailpitContext.get('/api/v1/search', {params: {query: `to:"${email}"`}});

        return (await response.json()).messages_count ?? 0;
    } finally {
        await mailpitContext.dispose();
    }
}

test.describe('Workspace membership and invitations', () => {
    const cleanups: (() => Promise<void>)[] = [];

    test.afterEach(async () => {
        while (cleanups.length) {
            await cleanups.pop()!();
        }
    });

    async function newWorkspace(adminApi: Parameters<typeof createRbacWorkspace>[0]): Promise<RbacWorkspaceI> {
        const workspace = await createRbacWorkspace(adminApi);

        cleanups.push(() => deleteRbacWorkspace(adminApi, workspace));

        return workspace;
    }

    async function newAccount(adminApi: Parameters<typeof createAccount>[0]): Promise<RoleUserI> {
        const account = await createAccount(adminApi, RBAC_BASE_URL);

        cleanups.push(() => deleteRoleUser(adminApi, account));

        return account;
    }

    test('an admin invites a member by email and the mailed claim link lets them set a password and sign in', async ({
        adminApi,
        browser,
        rbacWorkspace,
    }) => {
        const email = `rbac_invitee_${getRandomString().slice(0, 8)}@localhost.com`;
        const login = email;

        cleanups.push(() => deleteRoleUser(adminApi, {email, login, password: '', role: 'VIEWER'}));

        const adminSession = await openAsTenantAdmin(browser, RBAC_BASE_URL, rbacWorkspace);

        cleanups.push(() => adminSession.context.close());

        const page = adminSession.page;

        await page.goto('/automation/settings/workspace-users');

        await page.getByRole('button', {name: 'Invite User'}).click();

        const dialog = page.getByRole('dialog', {name: 'Invite User'});

        await dialog.getByPlaceholder('colleague@example.com').fill(email);

        await dialog.getByLabel('Role', {exact: true}).click();

        await page.getByRole('option', {exact: true, name: 'Viewer'}).click();

        await dialog.getByRole('button', {exact: true, name: 'Invite'}).click();

        await expect(page.getByRole('row').filter({hasText: email})).toBeVisible();

        const claimKey = await readClaimKey(email);

        const inviteeContext = await browser.newContext({baseURL: RBAC_BASE_URL});

        cleanups.push(() => inviteeContext.close());

        const inviteePage = await inviteeContext.newPage();

        await inviteePage.goto(`/accept-invitation?key=${claimKey}`);

        await inviteePage.getByLabel('Password', {exact: true}).fill('Playwright-Invitee-1');

        const [claimResponse] = await Promise.all([
            inviteePage.waitForResponse((response) => response.url().includes('/api/account/accept-invitation')),
            inviteePage.getByRole('button', {name: 'Activate account'}).click(),
        ]);

        expect(claimResponse.status()).toBe(200);

        const inviteeApi = await createRoleUserApiContext(RBAC_BASE_URL, {
            email,
            login,
            password: 'Playwright-Invitee-1',
            role: 'VIEWER',
        });

        cleanups.push(() => inviteeApi.dispose());

        const roleResponse = await postGraphQl<{myWorkspaceRole: string}>(
            inviteeApi,
            'query MyWorkspaceRole($workspaceId: ID!) { myWorkspaceRole(workspaceId: $workspaceId) }',
            {workspaceId: String(rbacWorkspace.id)}
        );

        expect(roleResponse.data?.myWorkspaceRole).toBe('VIEWER');
    });

    test('inviting an address that already has an account reuses it and mails a membership notice', async ({
        adminApi,
    }) => {
        const workspace = await newWorkspace(adminApi);
        const account = await newAccount(adminApi);

        const mailCountBefore = await fetchMailCount(account.email);

        const response = await postGraphQl<{inviteWorkspaceUser: {userId: string}}>(adminApi, INVITE_WORKSPACE_USER, {
            email: account.email,
            role: 'EDITOR',
            workspaceId: String(workspace.id),
        });

        expect(response.errors, JSON.stringify(response.errors)).toBeUndefined();
        expect(response.data?.inviteWorkspaceUser.userId).toBe(account.userId);

        await expect.poll(() => fetchMailCount(account.email), {timeout: 30000}).toBe(mailCountBefore + 1);
    });

    test('a tenant invite for an email that is already in use is refused as a client-correctable error', async ({
        adminApi,
    }) => {
        const account = await newAccount(adminApi);

        const response = await postGraphQl(adminApi, INVITE_USER, {email: account.email, role: 'ROLE_USER'});

        expect(response.errors?.[0]?.message).toBe('Email is already in use!');
        expect(response.errors?.[0]?.extensions).toMatchObject({classification: 'BAD_REQUEST'});
    });

    test('an invitee gets their lowercased email address as the login', async ({adminApi}) => {
        const email = `Rbac_Mixed_${getRandomString().slice(0, 8)}@LocalHost.com`;
        const login = email.toLowerCase();

        const response = await postGraphQl(adminApi, INVITE_USER, {email, role: 'ROLE_USER'});

        expect(response.errors, JSON.stringify(response.errors)).toBeUndefined();

        cleanups.push(() => deleteRoleUser(adminApi, {email, login, password: '', role: ''}));

        const userResponse = await postGraphQl<{user: {email: string} | null}>(
            adminApi,
            'query User($login: String!) { user(login: $login) { email } }',
            {login}
        );

        expect(userResponse.data?.user?.email).toBe(login);
    });

    test('an invitee whose email is longer than a login may be is refused', async ({adminApi}) => {
        const email = `rbac_${getRandomString()}${getRandomString()}@localhost.com`;

        const response = await postGraphQl(adminApi, INVITE_USER, {email, role: 'ROLE_USER'});

        expect(response.errors?.[0]?.message).toBe(`Email ${email} is invalid`);
    });

    test('adding someone who is already a member is refused with ALREADY_MEMBER', async ({adminApi}) => {
        const workspace = await newWorkspace(adminApi);
        const account = await newAccount(adminApi);

        const variables = {role: 'EDITOR', userId: account.userId, workspaceId: String(workspace.id)};

        expect((await postGraphQl(adminApi, ADD_WORKSPACE_USER, variables)).errors).toBeUndefined();

        const response = await postGraphQl(adminApi, ADD_WORKSPACE_USER, variables);

        expect(response.errors?.[0]?.extensions).toMatchObject({classification: 'BAD_REQUEST'});
        expect(response.errors?.[0]?.message).toContain('is already a member of workspace');
    });

    test('the creator of a workspace becomes its ADMIN', async ({adminApi}) => {
        const workspace = await newWorkspace(adminApi);

        const adminUserId = await fetchUserId(adminApi, 'admin');

        const response = await postGraphQl<{workspaceUsers: WorkspaceUserI[]}>(adminApi, WORKSPACE_USERS, {
            workspaceId: String(workspace.id),
        });

        expect(response.errors, JSON.stringify(response.errors)).toBeUndefined();
        expect(response.data?.workspaceUsers).toContainEqual(
            expect.objectContaining({
                environment: null,
                inherited: false,
                userId: adminUserId,
                workspaceRole: 'ADMIN',
            })
        );
    });

    test('the last admin can be neither removed nor demoted', async ({adminApi}) => {
        const workspace = await newWorkspace(adminApi);
        const account = await newAccount(adminApi);

        const adminUserId = await fetchUserId(adminApi, 'admin');
        const workspaceId = String(workspace.id);

        expect(
            (await postGraphQl(adminApi, ADD_WORKSPACE_USER, {role: 'ADMIN', userId: account.userId, workspaceId}))
                .errors
        ).toBeUndefined();

        await test.step('another admin may be removed while one remains', async () => {
            const response = await postGraphQl(adminApi, REMOVE_WORKSPACE_USER, {userId: adminUserId, workspaceId});

            expect(response.errors, JSON.stringify(response.errors)).toBeUndefined();
        });

        await test.step('the remaining admin cannot be removed', async () => {
            const response = await postGraphQl(adminApi, REMOVE_WORKSPACE_USER, {userId: account.userId, workspaceId});

            expect(response.errors?.[0]?.message).toContain('Cannot remove or demote the last admin');
            expect(response.errors?.[0]?.extensions).toMatchObject({classification: 'BAD_REQUEST'});
        });

        await test.step('the remaining admin cannot be demoted', async () => {
            const response = await postGraphQl(adminApi, UPDATE_WORKSPACE_USER_ROLE, {
                role: 'EDITOR',
                userId: account.userId,
                workspaceId,
            });

            expect(response.errors?.[0]?.message).toContain('Cannot remove or demote the last admin');
        });

        const usersResponse = await postGraphQl<{workspaceUsers: WorkspaceUserI[]}>(adminApi, WORKSPACE_USERS, {
            workspaceId,
        });

        expect(usersResponse.data?.workspaceUsers).toContainEqual(
            expect.objectContaining({inherited: false, userId: account.userId, workspaceRole: 'ADMIN'})
        );
    });

    test('a member manager can neither promote themselves nor grant a role above their own', async ({
        adminApi,
        rbacWorkspace,
    }) => {
        const customRole = await createCustomRole(adminApi, [
            'WORKFLOW_VIEW',
            'WORKSPACE_VIEW',
            'WORKSPACE_MEMBER_MANAGE',
        ]);

        cleanups.push(() => deleteCustomRole(adminApi, customRole));

        const manager = await createCustomRoleUser(adminApi, RBAC_BASE_URL, rbacWorkspace, customRole);

        cleanups.push(() => deleteRoleUser(adminApi, manager));

        const managerApi = await createRoleUserApiContext(RBAC_BASE_URL, manager);

        cleanups.push(() => managerApi.dispose());

        const workspaceId = String(rbacWorkspace.id);

        await test.step('promoting themselves to ADMIN is refused', async () => {
            const response = await postGraphQl(managerApi, UPDATE_WORKSPACE_USER_ROLE, {
                role: 'ADMIN',
                userId: manager.userId,
                workspaceId,
            });

            expect(response.errors?.length, JSON.stringify(response.data)).toBeGreaterThan(0);
        });

        await test.step('inviting someone as ADMIN is refused', async () => {
            const response = await postGraphQl(managerApi, INVITE_WORKSPACE_USER, {
                email: `rbac_escalation_${getRandomString().slice(0, 8)}@localhost.com`,
                role: 'ADMIN',
                workspaceId,
            });

            expect(response.errors?.length, JSON.stringify(response.data)).toBeGreaterThan(0);
        });

        const usersResponse = await postGraphQl<{workspaceUsers: WorkspaceUserI[]}>(adminApi, WORKSPACE_USERS, {
            workspaceId,
        });

        expect(usersResponse.data?.workspaceUsers).toContainEqual(
            expect.objectContaining({customRoleId: customRole.id, userId: manager.userId, workspaceRole: null})
        );
    });
});
