import {APIRequestContext, expect, test} from '@playwright/test';

import {
    AUTHENTICATION_BASE_URL,
    AUTHENTICATION_MOCKS_ENABLED,
    AUTHENTICATION_MOCKS_MISSING_REASON,
    currentAccount,
    expectSignInRefused,
    expectSignedInto,
    inviteAndClaimUser,
    signInAtGitHubMock,
    signInAtOidcMock,
    startSocialSignIn,
    uniqueEmail,
    uniqueSuffix,
} from '../../utils/authenticationUtils';
import {createAdminApiContext} from '../../utils/rbacUtils';

test.describe('Social login (#1874)', () => {
    test.skip(!AUTHENTICATION_MOCKS_ENABLED, AUTHENTICATION_MOCKS_MISSING_REASON);

    let adminApiContext: APIRequestContext;

    test.beforeAll(async () => {
        adminApiContext = await createAdminApiContext(AUTHENTICATION_BASE_URL);
    });

    test.afterAll(async () => {
        await adminApiContext?.dispose();
    });

    test('offers a button for each configured provider', async ({page}) => {
        await page.goto('/login');

        await expect(page.getByRole('button', {name: 'Continue with Google'})).toBeVisible();
        await expect(page.getByRole('button', {name: 'Continue with Github'})).toBeVisible();
    });

    test('an invited user signs in with Google and then with GitHub to the same account', async ({page}) => {
        const email = uniqueEmail('invited');

        await inviteAndClaimUser(adminApiContext, email);

        await startSocialSignIn(page, 'Google');

        await signInAtOidcMock(page, {
            email,
            email_verified: true,
            family_name: 'Invited',
            given_name: 'Gina',
            sub: `google-${uniqueSuffix()}`,
        });

        await expectSignedInto(page);

        const googleAccount = await currentAccount(page);

        expect(googleAccount.email).toBe(email);
        expect(googleAccount.authorities).toEqual(['ROLE_USER']);

        await page.context().clearCookies();

        await startSocialSignIn(page, 'Github');

        await signInAtGitHubMock(page, {email, id: Date.now(), name: 'Gina Invited'});

        await expectSignedInto(page);

        expect((await currentAccount(page)).login).toBe(googleAccount.login);
    });

    test('GitHub reads the primary verified email when the profile hides it', async ({page}) => {
        const email = uniqueEmail('private-email');

        await inviteAndClaimUser(adminApiContext, email);

        await startSocialSignIn(page, 'Github');

        await signInAtGitHubMock(page, {email, id: Date.now(), name: 'Private Email', public_email: false});

        await expectSignedInto(page);

        expect((await currentAccount(page)).email).toBe(email);
    });

    test('an unknown email cannot sign up into an existing organization', async ({page}) => {
        await startSocialSignIn(page, 'Google');

        await signInAtOidcMock(page, {
            email: uniqueEmail('stranger'),
            email_verified: true,
            sub: `google-${uniqueSuffix()}`,
        });

        await expectSignInRefused(page, 'There is no account for this email yet.');
    });

    test('a Google account with an unverified email cannot take over an existing account', async ({page}) => {
        const email = uniqueEmail('victim');

        await inviteAndClaimUser(adminApiContext, email);

        await startSocialSignIn(page, 'Google');

        await signInAtOidcMock(page, {email, email_verified: false, sub: `google-${uniqueSuffix()}`});

        await expectSignInRefused(page, /not verified with the provider/);
    });
});
