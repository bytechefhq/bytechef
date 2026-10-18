import {APIRequestContext, Page, expect, request, test} from '@playwright/test';

import {
    AUTHENTICATION_BASE_URL,
    AUTHENTICATION_MOCKS_ENABLED,
    AUTHENTICATION_MOCKS_MISSING_REASON,
    INVITED_USER_PASSWORD,
    expectSignedInto,
    inviteAndClaimUser,
    signInAtOidcMock,
    startSocialSignIn,
    totpCode,
    uniqueEmail,
    uniqueSuffix,
} from '../../utils/authenticationUtils';
import {createAdminApiContext} from '../../utils/rbacUtils';

async function submitPasswordLogin(page: Page, email: string, rememberMe = false): Promise<void> {
    await page.goto('/login');

    await page.getByLabel('Email', {exact: true}).fill(email);

    await page.getByLabel('Password', {exact: true}).fill(INVITED_USER_PASSWORD);

    if (rememberMe) {
        await page.getByLabel('Stay logged in').check();
    }

    await page.getByRole('button', {name: /log in/i}).click();
}

async function enterCode(page: Page, code: string): Promise<void> {
    await page.getByLabel('MFA verification code').fill(code);

    await page.getByRole('button', {name: 'Verify'}).click();
}

async function hasRememberMeCookie(page: Page): Promise<boolean> {
    const cookies = await page.context().cookies();

    return cookies.some((cookie) => cookie.name === 'remember-me');
}

/**
 * Enrolls the user's authenticator through the account API, as the profile page does, and returns the secret.
 */
async function enrollThroughApi(email: string): Promise<string> {
    const apiContext = await request.newContext({baseURL: AUTHENTICATION_BASE_URL});

    try {
        const xsrfToken = async () => {
            await apiContext.get('/api/account');

            return (
                (await apiContext.storageState()).cookies.find((cookie) => cookie.name === 'XSRF-TOKEN')?.value || ''
            );
        };

        const loginResponse = await apiContext.post('/api/authentication', {
            form: {password: INVITED_USER_PASSWORD, 'remember-me': 'false', username: email},
            headers: {'X-XSRF-TOKEN': await xsrfToken()},
        });

        expect(loginResponse.status()).toBe(200);

        const setupResponse = await apiContext.post('/api/account/mfa/setup', {
            headers: {'X-XSRF-TOKEN': await xsrfToken()},
        });

        const {secret} = await setupResponse.json();

        const enableResponse = await apiContext.post('/api/account/mfa/enable', {
            data: {code: totpCode(secret)},
            headers: {'X-XSRF-TOKEN': await xsrfToken()},
        });

        expect(enableResponse.status()).toBe(204);

        return secret;
    } finally {
        await apiContext.dispose();
    }
}

test.describe('Two-factor authentication (#1040)', () => {
    test.skip(!AUTHENTICATION_MOCKS_ENABLED, AUTHENTICATION_MOCKS_MISSING_REASON);

    let adminApiContext: APIRequestContext;

    test.beforeAll(async () => {
        adminApiContext = await createAdminApiContext(AUTHENTICATION_BASE_URL);
    });

    test.afterAll(async () => {
        await adminApiContext?.dispose();
    });

    test('a user enrolls on the profile page and then needs the code after the password', async ({page}) => {
        const email = uniqueEmail('totp');

        await inviteAndClaimUser(adminApiContext, email);

        await submitPasswordLogin(page, email);

        await expectSignedInto(page);

        await page.goto('/automation/account');

        await page.getByRole('button', {name: 'Set up 2FA'}).click();

        const secret = (await page.locator('code').first().textContent())!.trim();

        await page.getByPlaceholder('Enter 6-digit code').fill(totpCode(secret));

        await page.getByRole('button', {name: 'Verify & Enable'}).click();

        await expect(page.getByText('Two-factor authentication is enabled.')).toBeVisible();

        await page.context().clearCookies();

        await submitPasswordLogin(page, email);

        await expect(page.getByText('Two-Factor Authentication')).toBeVisible();

        await enterCode(page, '000000');

        await expect(page.getByText('Invalid code. Please try again.')).toBeVisible();

        await enterCode(page, totpCode(secret));

        await expectSignedInto(page);
    });

    test('stay logged in issues the remember-me cookie only after the code is verified', async ({page}) => {
        const email = uniqueEmail('remember');

        await inviteAndClaimUser(adminApiContext, email);

        const secret = await enrollThroughApi(email);

        await submitPasswordLogin(page, email, true);

        await expect(page.getByText('Two-Factor Authentication')).toBeVisible();

        expect(await hasRememberMeCookie(page)).toBe(false);

        expect((await page.request.get('/api/account')).ok()).toBe(false);

        await enterCode(page, totpCode(secret));

        await expectSignedInto(page);

        expect(await hasRememberMeCookie(page)).toBe(true);
    });

    test('a Google sign-in still asks for the code', async ({page}) => {
        const email = uniqueEmail('totp-google');

        await inviteAndClaimUser(adminApiContext, email);

        const secret = await enrollThroughApi(email);

        await startSocialSignIn(page, 'Google');

        await signInAtOidcMock(page, {email, email_verified: true, sub: `google-${uniqueSuffix()}`});

        await expect(page).toHaveURL(/\/login\?mfa=required/);

        await expect(page.getByText('Two-Factor Authentication')).toBeVisible();

        await enterCode(page, totpCode(secret));

        await expectSignedInto(page);
    });
});
