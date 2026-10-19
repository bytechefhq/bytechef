import {type Browser, type Page, expect, request as playwrightRequest} from '@playwright/test';

import {workspaceRolesTest as test} from '../../fixtures/workspaceRoles';
import getRandomString from '../../utils/getRandomString';
import {
    RBAC_BASE_URL,
    createAccount,
    createRoleUserApiContext,
    deleteRoleUser,
    graphql,
    openAsTenantAdmin,
    readClaimKey,
} from '../../utils/rbacUtils';

const MAILPIT_URL = process.env.PLAYWRIGHT_MAILPIT_URL || 'http://localhost:8025';

const INVITEE_PASSWORD = 'Playwright-Invitee-1';

const INVITE_USER = 'mutation InviteUser($email: String!, $role: String!) { inviteUser(email: $email, role: $role) }';

async function readMail(email: string): Promise<string> {
    const mailpitContext = await playwrightRequest.newContext({baseURL: MAILPIT_URL});

    try {
        const searchResponse = await mailpitContext.get('/api/v1/search', {
            params: {limit: '1', query: `to:"${email}"`},
        });

        const messageId = (await searchResponse.json()).messages?.[0]?.ID;

        const message = await (await mailpitContext.get(`/api/v1/message/${messageId}`)).json();

        return `${message.HTML} ${message.Text}`;
    } finally {
        await mailpitContext.dispose();
    }
}

async function openInvitationLink(browser: Browser, key: string): Promise<{close: () => Promise<void>; page: Page}> {
    const context = await browser.newContext({baseURL: RBAC_BASE_URL});

    const page = await context.newPage();

    await page.goto(`/accept-invitation?key=${key}`);

    return {close: () => context.close(), page};
}

async function submitPassword(page: Page): Promise<void> {
    await page.getByLabel('Password', {exact: true}).fill(INVITEE_PASSWORD);

    await page.getByRole('button', {name: 'Activate account'}).click();
}

test.describe('Invitations', () => {
    const cleanups: (() => Promise<void>)[] = [];

    test.afterEach(async () => {
        while (cleanups.length) {
            await cleanups.pop()!();
        }
    });

    test('an invitee stays Pending until they set a password on the mailed link, which then works only once', async ({
        adminApi,
        browser,
        rbacWorkspace,
    }) => {
        const login = `invitee_${getRandomString()}`;
        const email = `${login}@localhost.com`;
        const invitee = {email, login, password: INVITEE_PASSWORD, role: 'VIEWER' as const};

        cleanups.push(() => deleteRoleUser(adminApi, invitee));

        await graphql(adminApi, INVITE_USER, {email, role: 'ROLE_USER'});

        const invitationKey = await readClaimKey(email);

        const invitationMail = await readMail(email);

        expect(invitationMail).toContain(`/accept-invitation?key=${invitationKey}`);
        expect(invitationMail).not.toContain('password-reset');

        const adminSession = await openAsTenantAdmin(browser, RBAC_BASE_URL, rbacWorkspace);

        cleanups.push(() => adminSession.context.close());

        await adminSession.page.goto('/automation/settings/users');

        const inviteeRow = adminSession.page.getByRole('row').filter({hasText: email});

        await expect(inviteeRow).toContainText('Pending');

        const firstVisit = await openInvitationLink(browser, invitationKey);

        cleanups.push(firstVisit.close);

        await submitPassword(firstVisit.page);

        await expect(firstVisit.page.getByText('Your account is ready')).toBeVisible();

        await adminSession.page.reload();

        await expect(inviteeRow).toContainText('Active');

        const inviteeApi = await createRoleUserApiContext(RBAC_BASE_URL, invitee);

        cleanups.push(() => inviteeApi.dispose());

        expect((await inviteeApi.get('/api/account')).status()).toBe(200);

        const secondVisit = await openInvitationLink(browser, invitationKey);

        cleanups.push(secondVisit.close);

        await submitPassword(secondVisit.page);

        await expect(secondVisit.page.getByRole('alert')).toContainText('invalid or has expired');
    });

    test('an unknown invitation key is refused', async ({browser}) => {
        const visit = await openInvitationLink(browser, `unknown${getRandomString()}`);

        cleanups.push(visit.close);

        await submitPassword(visit.page);

        await expect(visit.page.getByRole('alert')).toContainText('invalid or has expired');
    });

    test('a link without a key goes to the login page', async ({browser}) => {
        const context = await browser.newContext({baseURL: RBAC_BASE_URL});

        cleanups.push(() => context.close());

        const page = await context.newPage();

        await page.goto('/accept-invitation');

        await expect(page).toHaveURL(/\/login/);
    });
    test('a password reset link opened in a fresh browser sets the new password', async ({adminApi, browser}) => {
        const account = await createAccount(adminApi, RBAC_BASE_URL);

        cleanups.push(() => deleteRoleUser(adminApi, account));

        const requestContext = await browser.newContext({baseURL: RBAC_BASE_URL});

        cleanups.push(() => requestContext.close());

        const requestPage = await requestContext.newPage();

        await requestPage.goto('/password-reset/init');

        await requestPage.getByLabel('Email').fill(account.email);

        await requestPage.getByRole('button', {name: 'Send link to email'}).click();

        let resetKey = '';

        await expect(async () => {
            const match = (await readMail(account.email)).match(/password-reset\/finish\?key=([A-Za-z0-9]+)/);

            expect(match).toBeTruthy();

            resetKey = match![1];
        }).toPass({timeout: 30000});

        const resetContext = await browser.newContext({baseURL: RBAC_BASE_URL});

        cleanups.push(() => resetContext.close());

        const resetPage = await resetContext.newPage();

        await resetPage.goto(`/password-reset/finish?key=${resetKey}`);

        await resetPage.getByLabel('Password', {exact: true}).fill('Playwright-Reset-1');

        await resetPage.getByRole('button', {name: 'Reset password'}).click();

        await expect(resetPage.getByText('Your password has been successfully reset.')).toBeVisible();

        const accountApi = await createRoleUserApiContext(RBAC_BASE_URL, {...account, password: 'Playwright-Reset-1'});

        cleanups.push(() => accountApi.dispose());

        expect((await accountApi.get('/api/account')).status()).toBe(200);
    });
});
