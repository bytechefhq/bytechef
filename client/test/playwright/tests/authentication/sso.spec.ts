import {APIRequestContext, expect, test} from '@playwright/test';

import {
    AUTHENTICATION_BASE_URL,
    AUTHENTICATION_MOCKS_ENABLED,
    AUTHENTICATION_MOCKS_MISSING_REASON,
    INVITED_USER_PASSWORD,
    type IdentityProviderI,
    SAML_IDP_USER,
    continueWithSso,
    createOidcIdentityProvider,
    createSamlIdentityProvider,
    currentAccount,
    deleteIdentityProvider,
    deleteLeftoverSamlIdentityProviders,
    expectSignInRefused,
    expectSignedInto,
    inviteAndClaimUser,
    setIdentityProviderEnforced,
    signInAtOidcMock,
    signInAtSamlIdp,
    startSocialSignIn,
    uniqueSuffix,
} from '../../utils/authenticationUtils';
import {TEST_USER} from '../../utils/constants';
import {login} from '../../utils/login';
import {createAdminApiContext} from '../../utils/rbacUtils';

test.describe('Single sign-on (#1040)', () => {
    test.skip(!AUTHENTICATION_MOCKS_ENABLED, AUTHENTICATION_MOCKS_MISSING_REASON);

    let adminApiContext: APIRequestContext;

    test.beforeAll(async () => {
        adminApiContext = await createAdminApiContext(AUTHENTICATION_BASE_URL);
    });

    test.afterAll(async () => {
        await adminApiContext?.dispose();
    });

    test.describe('OIDC', () => {
        let identityProvider: IdentityProviderI | undefined;

        test.afterEach(async () => {
            await deleteIdentityProvider(adminApiContext, identityProvider);

            identityProvider = undefined;
        });

        test('discovery offers the provider and the first sign-in provisions a ROLE_USER account', async ({page}) => {
            identityProvider = await createOidcIdentityProvider(adminApiContext);

            const email = `dana-${uniqueSuffix()}@${identityProvider.domain}`;

            await continueWithSso(page, email, identityProvider);

            await signInAtOidcMock(page, {
                email,
                email_verified: true,
                given_name: 'Dana',
                sub: `sso-${uniqueSuffix()}`,
            });

            await expectSignedInto(page);

            const account = await currentAccount(page);

            expect(account.email).toBe(email);
            expect(account.authorities).toEqual(['ROLE_USER']);
        });

        test('a provider cannot sign in an email outside its domains', async ({page}) => {
            identityProvider = await createOidcIdentityProvider(adminApiContext);

            await continueWithSso(page, `dana-${uniqueSuffix()}@${identityProvider.domain}`, identityProvider);

            await signInAtOidcMock(page, {
                email: TEST_USER.email,
                email_verified: true,
                sub: `sso-${uniqueSuffix()}`,
            });

            await expectSignInRefused(page, /not on a domain of your organization identity provider/);
        });

        test('enforced SSO refuses password and Google sign-in for the domain', async ({page}) => {
            identityProvider = await createOidcIdentityProvider(adminApiContext);

            const email = `erin-${uniqueSuffix()}@${identityProvider.domain}`;

            await inviteAndClaimUser(adminApiContext, email);

            await setIdentityProviderEnforced(adminApiContext, identityProvider, true);

            await page.goto('/login');

            await page.getByLabel('Email', {exact: true}).fill(email);

            await page.getByLabel('Password', {exact: true}).fill(INVITED_USER_PASSWORD);

            await page.getByRole('button', {name: /log in/i}).click();

            await expectSignInRefused(page, /Your organization requires SSO login/);

            await startSocialSignIn(page, 'Google');

            await signInAtOidcMock(page, {email, email_verified: true, sub: `google-${uniqueSuffix()}`});

            await expectSignInRefused(page, /Your organization requires SSO/);
        });

        test('the provider dialog shows the redirect URI to register with the IdP', async ({page}) => {
            identityProvider = await createOidcIdentityProvider(adminApiContext);

            await login(page, TEST_USER.email, TEST_USER.password);

            await page.goto('/automation/settings/identity-providers');

            await page
                .getByRole('row', {name: new RegExp(identityProvider.name)})
                .getByRole('button')
                .first()
                .click();

            const redirectUri = page.getByRole('dialog').locator('input[readonly]').first();

            await expect(redirectUri).toHaveValue(
                `${AUTHENTICATION_BASE_URL}/login/oauth2/code/${identityProvider.registrationId}`
            );
        });
    });

    test.describe('SAML', () => {
        test.describe.configure({mode: 'serial'});

        let identityProvider: IdentityProviderI | undefined;

        test.beforeAll(async () => {
            await deleteLeftoverSamlIdentityProviders(adminApiContext);
        });

        test.afterEach(async () => {
            await deleteIdentityProvider(adminApiContext, identityProvider);

            identityProvider = undefined;
        });

        test('discovery sends an example.com user to the SAML IdP and back into the application', async ({page}) => {
            identityProvider = await createSamlIdentityProvider(adminApiContext);

            await continueWithSso(page, SAML_IDP_USER.email, identityProvider);

            await signInAtSamlIdp(page);

            await expectSignedInto(page);

            expect((await currentAccount(page)).email).toBe(SAML_IDP_USER.email);
        });
    });
});
