import {type APIRequestContext, type Browser, type Page, expect, test} from '@playwright/test';

import {
    BASE_URL,
    createAdminApiContext,
    createAnonymousApiContext,
    createUserApiContext,
    deleteUser,
    inviteUser,
    readInvitationKey,
    readMail,
    signIn,
} from '../../utils/accountUtils';
import {TEST_USER} from '../../utils/constants';
import getRandomString from '../../utils/getRandomString';
import {login} from '../../utils/login';

const INVITEE_PASSWORD = 'Playwright-Invitee-1';

const RESET_PASSWORD = 'Playwright-Reset-1';

function randomEmail(prefix: string): string {
    return `${prefix}_${getRandomString().slice(0, 8)}@localhost.com`;
}

async function openInvitationLink(browser: Browser, key: string): Promise<{close: () => Promise<void>; page: Page}> {
    const context = await browser.newContext({baseURL: BASE_URL});

    const page = await context.newPage();

    await page.goto(`/accept-invitation?key=${key}`);

    return {close: () => context.close(), page};
}

async function submitPassword(page: Page, password = INVITEE_PASSWORD): Promise<void> {
    await page.getByLabel('Password', {exact: true}).fill(password);

    await page.getByRole('button', {name: 'Activate account'}).click();
}

test.describe('Invitations', () => {
    const cleanups: (() => Promise<void>)[] = [];

    let adminApi: APIRequestContext;

    test.beforeEach(async () => {
        adminApi = await createAdminApiContext();
    });

    test.afterEach(async () => {
        while (cleanups.length) {
            await cleanups.pop()!();
        }

        await adminApi.dispose();
    });

    test('an invitee stays Pending until they set a password on the mailed link, which then works only once', async ({
        browser,
        page,
    }) => {
        const email = randomEmail('invitee');

        cleanups.push(() => deleteUser(adminApi, email));

        await login(page, TEST_USER.email, TEST_USER.password);

        await page.goto('/automation/settings/users');

        await page.getByRole('button', {name: 'Invite User'}).click();

        const inviteDialog = page.getByRole('dialog');

        await expect(inviteDialog.getByText('Password', {exact: true})).toHaveCount(0);

        await inviteDialog.getByPlaceholder('user@example.com').fill(email);

        await inviteDialog.getByRole('button', {name: 'Invite'}).click();

        await expect(inviteDialog).toBeHidden();

        const inviteeRow = page.getByRole('row').filter({hasText: email});

        await expect(inviteeRow).toContainText('Pending');

        const invitationMail = await readInvitationKey(email);

        expect(invitationMail.content).not.toContain('password-reset');

        const anonymousApi = await createAnonymousApiContext();

        cleanups.push(() => anonymousApi.dispose());

        const signInBeforeAccepting = await signIn(anonymousApi, email, INVITEE_PASSWORD);

        expect(signInBeforeAccepting.status()).toBe(401);

        const firstVisit = await openInvitationLink(browser, invitationMail.key);

        cleanups.push(firstVisit.close);

        await submitPassword(firstVisit.page);

        await expect(firstVisit.page.getByText('Your account is ready')).toBeVisible();

        await page.reload();

        await expect(inviteeRow).toContainText('Active');

        const inviteeApi = await createUserApiContext(email, INVITEE_PASSWORD);

        cleanups.push(() => inviteeApi.dispose());

        expect((await inviteeApi.get('/api/account')).status()).toBe(200);

        const secondVisit = await openInvitationLink(browser, invitationMail.key);

        cleanups.push(secondVisit.close);

        await submitPassword(secondVisit.page, 'Playwright-Other-1');

        await expect(secondVisit.page.getByRole('alert')).toContainText('invalid or has expired');
    });

    test('the activation endpoint refuses an invitation key and leaves the invitation usable', async ({browser}) => {
        const email = randomEmail('invitee');

        cleanups.push(() => deleteUser(adminApi, email));

        await inviteUser(adminApi, email);

        const {key} = await readInvitationKey(email);

        const anonymousApi = await createAnonymousApiContext();

        cleanups.push(() => anonymousApi.dispose());

        const activateResponse = await anonymousApi.get(`/api/activate?key=${key}`);

        expect(activateResponse.ok()).toBeFalsy();

        const visit = await openInvitationLink(browser, key);

        cleanups.push(visit.close);

        await submitPassword(visit.page);

        await expect(visit.page.getByText('Your account is ready')).toBeVisible();
    });

    test('an unknown invitation key is refused', async ({browser}) => {
        const visit = await openInvitationLink(browser, `unknown${getRandomString()}`);

        cleanups.push(visit.close);

        await submitPassword(visit.page);

        await expect(visit.page.getByRole('alert')).toContainText('invalid or has expired');
    });

    test('a link without a key goes to the login page', async ({browser}) => {
        const context = await browser.newContext({baseURL: BASE_URL});

        cleanups.push(() => context.close());

        const page = await context.newPage();

        await page.goto('/accept-invitation');

        await expect(page).toHaveURL(/\/login/);
    });

    test('a password reset link opened in a fresh browser sets the new password', async ({browser}) => {
        const email = randomEmail('reset');

        cleanups.push(() => deleteUser(adminApi, email));

        await inviteUser(adminApi, email);

        const {key: invitationKey} = await readInvitationKey(email);

        const invitationVisit = await openInvitationLink(browser, invitationKey);

        cleanups.push(invitationVisit.close);

        await submitPassword(invitationVisit.page);

        await expect(invitationVisit.page.getByText('Your account is ready')).toBeVisible();

        const requestContext = await browser.newContext({baseURL: BASE_URL});

        cleanups.push(() => requestContext.close());

        const requestPage = await requestContext.newPage();

        await requestPage.goto('/password-reset/init');

        await requestPage.getByLabel('Email').fill(email);

        await requestPage.getByRole('button', {name: 'Send link to email'}).click();

        const {key: resetKey} = await readMail(email, /password-reset\/finish\?key=([A-Za-z0-9]+)/);

        const resetContext = await browser.newContext({baseURL: BASE_URL});

        cleanups.push(() => resetContext.close());

        const resetPage = await resetContext.newPage();

        await resetPage.goto(`/password-reset/finish?key=${resetKey}`);

        await resetPage.getByLabel('Password', {exact: true}).fill(RESET_PASSWORD);

        await resetPage.getByRole('button', {name: 'Reset password'}).click();

        await expect(resetPage.getByText('Your password has been successfully reset.')).toBeVisible();

        const accountApi = await createUserApiContext(email, RESET_PASSWORD);

        cleanups.push(() => accountApi.dispose());

        expect((await accountApi.get('/api/account')).status()).toBe(200);
    });
});
