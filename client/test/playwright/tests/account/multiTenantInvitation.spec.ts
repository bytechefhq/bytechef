import {type APIRequestContext, type Browser, expect, test} from '@playwright/test';

import {
    BASE_URL,
    createAnonymousApiContext,
    createUserApiContext,
    deleteUser,
    graphql,
    readInvitationKey,
    registerAccount,
    signIn,
} from '../../utils/accountUtils';
import getRandomString from '../../utils/getRandomString';

const MULTI_TENANT = process.env.BYTECHEF_MULTI_TENANT === 'true';

const OWNER_PASSWORD = 'Playwright-Owner-1';

const INVITEE_PASSWORD = 'Playwright-Invitee-1';

const FIRST_TENANT_OWNER = 'playwright_tenant_first@localhost.com';

const SECOND_TENANT_OWNER = 'playwright_tenant_second@localhost.com';

interface UsersResponseI {
    users: {content: {activated: boolean; email: string}[]};
}

function randomEmail(prefix: string): string {
    return `${prefix}_${getRandomString().slice(0, 8)}@localhost.com`;
}

async function ensureTenantOwner(email: string): Promise<void> {
    const status = await registerAccount(email, OWNER_PASSWORD);

    expect([201, 400], `sign-up of ${email} answered ${status}`).toContain(status);
}

async function tenantUsers(apiContext: APIRequestContext): Promise<Map<string, boolean>> {
    const data = await graphql<UsersResponseI>(
        apiContext,
        'query Users { users(pageNumber: 0, pageSize: 1000) { content { activated email } } }',
        {}
    );

    return new Map(data.users.content.map((user) => [user.email, user.activated]));
}

async function inviteErrors(apiContext: APIRequestContext, email: string): Promise<string[]> {
    await apiContext.get('/api/authenticate');

    const storageState = await apiContext.storageState();

    const xsrfToken = storageState.cookies.find((cookie) => cookie.name === 'XSRF-TOKEN')?.value || '';

    const response = await apiContext.post('/graphql', {
        data: {
            query: 'mutation InviteUser($email: String!, $role: String!) { inviteUser(email: $email, role: $role) }',
            variables: {email, role: 'ROLE_USER'},
        },
        headers: {'X-XSRF-TOKEN': xsrfToken},
    });

    const body = await response.json();

    return (body.errors || []).map((error: {message: string}) => error.message);
}

async function acceptInvitationInBrowser(browser: Browser, key: string): Promise<void> {
    const context = await browser.newContext({baseURL: BASE_URL});

    try {
        const page = await context.newPage();

        await page.goto(`/accept-invitation?key=${key}`);

        await page.getByLabel('Password', {exact: true}).fill(INVITEE_PASSWORD);

        await page.getByRole('button', {name: 'Activate account'}).click();

        await expect(page.getByText('Your account is ready')).toBeVisible();
    } finally {
        await context.close();
    }
}

test.describe('Invitations across tenants', () => {
    test.skip(!MULTI_TENANT, 'Needs a server running with bytechef.tenant.mode=multi and BYTECHEF_MULTI_TENANT=true');

    test.describe.configure({mode: 'serial'});

    const cleanups: (() => Promise<void>)[] = [];

    let firstTenantApi: APIRequestContext;
    let secondTenantApi: APIRequestContext;

    test.beforeAll(async () => {
        await ensureTenantOwner(FIRST_TENANT_OWNER);
        await ensureTenantOwner(SECOND_TENANT_OWNER);
    });

    test.beforeEach(async () => {
        firstTenantApi = await createUserApiContext(FIRST_TENANT_OWNER, OWNER_PASSWORD);
        secondTenantApi = await createUserApiContext(SECOND_TENANT_OWNER, OWNER_PASSWORD);
    });

    test.afterEach(async () => {
        while (cleanups.length) {
            await cleanups.pop()!();
        }

        await firstTenantApi.dispose();
        await secondTenantApi.dispose();
    });

    test('an invitation from the second tenant activates the invitee in that tenant only', async ({browser}) => {
        const email = randomEmail('tenant_invitee');

        cleanups.push(() => deleteUser(secondTenantApi, email));

        expect(await inviteErrors(secondTenantApi, email)).toEqual([]);

        expect((await tenantUsers(secondTenantApi)).get(email)).toBe(false);

        const {key} = await readInvitationKey(email);

        await acceptInvitationInBrowser(browser, key);

        expect((await tenantUsers(secondTenantApi)).get(email)).toBe(true);

        expect((await tenantUsers(firstTenantApi)).has(email)).toBe(false);

        const inviteeApi = await createUserApiContext(email, INVITEE_PASSWORD);

        cleanups.push(() => inviteeApi.dispose());

        const accountResponse = await inviteeApi.get('/api/account');

        expect(accountResponse.status()).toBe(200);

        expect((await accountResponse.json()).email).toBe(email);
    });

    test('an email with a pending invitation in one tenant cannot be invited by another', async ({browser}) => {
        const email = randomEmail('tenant_pending');

        cleanups.push(() => deleteUser(firstTenantApi, email));

        expect(await inviteErrors(firstTenantApi, email)).toEqual([]);

        expect(await inviteErrors(secondTenantApi, email)).toEqual([expect.stringContaining('already in use')]);

        expect(await inviteErrors(secondTenantApi, email.toUpperCase())).toEqual([
            expect.stringContaining('already in use'),
        ]);

        const {key} = await readInvitationKey(email);

        await acceptInvitationInBrowser(browser, key);

        expect((await tenantUsers(firstTenantApi)).get(email)).toBe(true);

        expect((await tenantUsers(secondTenantApi)).has(email)).toBe(false);
    });

    test('an invitation cannot take over a user of another tenant', async () => {
        expect(await inviteErrors(secondTenantApi, FIRST_TENANT_OWNER)).toEqual([
            expect.stringContaining('already in use'),
        ]);

        const anonymousApi = await createAnonymousApiContext();

        cleanups.push(() => anonymousApi.dispose());

        expect((await signIn(anonymousApi, FIRST_TENANT_OWNER, OWNER_PASSWORD)).status()).toBe(200);

        expect((await tenantUsers(secondTenantApi)).has(FIRST_TENANT_OWNER)).toBe(false);
    });
});
