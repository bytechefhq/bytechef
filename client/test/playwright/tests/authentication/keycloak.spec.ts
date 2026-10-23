import {APIRequestContext, Browser, Page, expect, test} from '@playwright/test';

import {
    AUTHENTICATION_BASE_URL,
    AUTHENTICATION_MOCKS_ENABLED,
    AUTHENTICATION_MOCKS_MISSING_REASON,
    type IdentityProviderI,
    continueWithSso,
    createOidcIdentityProvider,
    createSamlIdentityProvider,
    currentAccount,
    deleteIdentityProvider,
    expectSignInRefused,
    expectSignedInto,
    fetchSamlSigningCertificate,
    graphqlErrors,
    uniqueSuffix,
} from '../../utils/authenticationUtils';
import {TEST_USER} from '../../utils/constants';
import {
    KEYCLOAK_ISSUER_URI,
    KEYCLOAK_OIDC_CLIENT,
    KEYCLOAK_SAML_METADATA_URI,
    type KeycloakUserI,
    createKeycloakSamlClient,
    createKeycloakUser,
    deleteKeycloakClient,
    deleteKeycloakUser,
    setKeycloakSamlClientRequestSigningCertificate,
    signInAtKeycloak,
} from '../../utils/keycloakUtils';
import {login} from '../../utils/login';
import {createAdminApiContext} from '../../utils/rbacUtils';

function pemBody(pem: string): string {
    return pem.replace(/-----(BEGIN|END) CERTIFICATE-----/g, '').replace(/\s/g, '');
}

function serviceProviderMetadataUrl(identityProvider: IdentityProviderI): string {
    return `${AUTHENTICATION_BASE_URL}/saml2/metadata/${identityProvider.registrationId}`;
}

async function withFreshPage(browser: Browser, callback: (page: Page) => Promise<void>): Promise<void> {
    const browserContext = await browser.newContext();

    try {
        await callback(await browserContext.newPage());
    } finally {
        await browserContext.close();
    }
}

test.describe('Single sign-on with Keycloak (#1040)', () => {
    test.skip(!AUTHENTICATION_MOCKS_ENABLED, AUTHENTICATION_MOCKS_MISSING_REASON);

    let adminApiContext: APIRequestContext;
    let identityProvider: IdentityProviderI | undefined;
    let keycloakSamlClientId: string | undefined;
    let keycloakUser: KeycloakUserI | undefined;

    test.beforeAll(async () => {
        adminApiContext = await createAdminApiContext(AUTHENTICATION_BASE_URL);
    });

    test.afterEach(async () => {
        await deleteIdentityProvider(adminApiContext, identityProvider);
        await deleteKeycloakClient(keycloakSamlClientId);
        await deleteKeycloakUser(keycloakUser);

        identityProvider = undefined;
        keycloakSamlClientId = undefined;
        keycloakUser = undefined;
    });

    test.afterAll(async () => {
        await adminApiContext?.dispose();
    });

    test.describe('OIDC', () => {
        test('the first sign-in provisions a ROLE_USER account with the Keycloak profile', async ({page}) => {
            identityProvider = await createOidcIdentityProvider(adminApiContext, {
                ...KEYCLOAK_OIDC_CLIENT,
                issuerUri: KEYCLOAK_ISSUER_URI,
                namePrefix: 'E2E Keycloak OIDC',
            });

            keycloakUser = await createKeycloakUser(identityProvider.domain);

            await continueWithSso(page, keycloakUser.email, identityProvider);

            await signInAtKeycloak(page, keycloakUser);

            await expectSignedInto(page);

            const account = await currentAccount(page);

            expect(account.email).toBe(keycloakUser.email);
            expect(account.authorities).toEqual(['ROLE_USER']);
            expect(account.firstName).toBe(keycloakUser.firstName);
            expect(account.lastName).toBe(keycloakUser.lastName);
        });

        test('signing in again reaches the same account', async ({browser, page}) => {
            identityProvider = await createOidcIdentityProvider(adminApiContext, {
                ...KEYCLOAK_OIDC_CLIENT,
                issuerUri: KEYCLOAK_ISSUER_URI,
                namePrefix: 'E2E Keycloak OIDC',
            });

            keycloakUser = await createKeycloakUser(identityProvider.domain);

            await continueWithSso(page, keycloakUser.email, identityProvider);

            await signInAtKeycloak(page, keycloakUser);

            await expectSignedInto(page);

            const firstAccount = await currentAccount(page);

            const secondContext = await browser.newContext();

            try {
                const secondPage = await secondContext.newPage();

                await continueWithSso(secondPage, keycloakUser.email, identityProvider);

                await signInAtKeycloak(secondPage, keycloakUser);

                await expectSignedInto(secondPage);

                expect((await currentAccount(secondPage)).login).toBe(firstAccount.login);
            } finally {
                await secondContext.close();
            }
        });

        test('a Keycloak user with an unverified email is refused', async ({page}) => {
            identityProvider = await createOidcIdentityProvider(adminApiContext, {
                ...KEYCLOAK_OIDC_CLIENT,
                issuerUri: KEYCLOAK_ISSUER_URI,
                namePrefix: 'E2E Keycloak OIDC',
            });

            keycloakUser = await createKeycloakUser(identityProvider.domain, {emailVerified: false});

            await continueWithSso(page, keycloakUser.email, identityProvider);

            await signInAtKeycloak(page, keycloakUser);

            await expectSignInRefused(page, /not verified with the provider/);
        });
    });

    test.describe('SAML', () => {
        async function createKeycloakSamlIdentityProvider(signingCertificate?: string): Promise<IdentityProviderI> {
            return createSamlIdentityProvider(adminApiContext, {
                domain: `saml-${uniqueSuffix()}.e2e.example.com`,
                metadataUri: KEYCLOAK_SAML_METADATA_URI,
                namePrefix: 'E2E Keycloak SAML',
                signingCertificate,
            });
        }

        test('the first sign-in provisions an account from the assertion attributes', async ({page}) => {
            identityProvider = await createKeycloakSamlIdentityProvider();

            keycloakSamlClientId = await createKeycloakSamlClient(identityProvider);

            keycloakUser = await createKeycloakUser(identityProvider.domain);

            await continueWithSso(page, keycloakUser.email, identityProvider);

            await signInAtKeycloak(page, keycloakUser);

            await expectSignedInto(page);

            const account = await currentAccount(page);

            expect(account.email).toBe(keycloakUser.email);
            expect(account.authorities).toEqual(['ROLE_USER']);
            expect(account.firstName).toBe(keycloakUser.firstName);
            expect(account.lastName).toBe(keycloakUser.lastName);
        });

        test('a pinned signing certificate accepts assertions Keycloak signs with it', async ({page}) => {
            identityProvider = await createKeycloakSamlIdentityProvider(
                await fetchSamlSigningCertificate(KEYCLOAK_SAML_METADATA_URI)
            );

            keycloakSamlClientId = await createKeycloakSamlClient(identityProvider);

            keycloakUser = await createKeycloakUser(identityProvider.domain);

            await continueWithSso(page, keycloakUser.email, identityProvider);

            await signInAtKeycloak(page, keycloakUser);

            await expectSignedInto(page);
        });

        test('a pinned signing certificate refuses assertions signed by another key', async ({page}) => {
            identityProvider = await createKeycloakSamlIdentityProvider(await fetchSamlSigningCertificate());

            keycloakSamlClientId = await createKeycloakSamlClient(identityProvider);

            keycloakUser = await createKeycloakUser(identityProvider.domain);

            await continueWithSso(page, keycloakUser.email, identityProvider);

            await signInAtKeycloak(page, keycloakUser);

            await expectSignInRefused(page, /Failed to sign in with your identity provider/);
        });

        test('a signing certificate that is not a certificate cannot be saved', async () => {
            const errors = await graphqlErrors(
                adminApiContext,
                `mutation CreateIdentityProvider($input: IdentityProviderInput!) {
                    createIdentityProvider(input: $input) { id }
                }`,
                {
                    input: {
                        domains: [`saml-${uniqueSuffix()}.e2e.example.com`],
                        enabled: true,
                        metadataUri: KEYCLOAK_SAML_METADATA_URI,
                        name: `E2E Keycloak SAML ${uniqueSuffix()}`,
                        signingCertificate: 'not a certificate',
                        type: 'SAML',
                    },
                }
            );

            expect(errors.map((error) => error.message).join('\n')).toMatch(/not a valid X\.509 certificate/);
        });

        test('Keycloak verifies the signed authentication request against the registered SP certificate', async ({
            page,
        }) => {
            identityProvider = await createKeycloakSamlIdentityProvider();

            keycloakSamlClientId = await createKeycloakSamlClient(identityProvider, {
                requestSigningCertificate: await fetchSamlSigningCertificate(
                    serviceProviderMetadataUrl(identityProvider)
                ),
            });

            keycloakUser = await createKeycloakUser(identityProvider.domain);

            await continueWithSso(page, keycloakUser.email, identityProvider);

            await signInAtKeycloak(page, keycloakUser);

            await expectSignedInto(page);
        });

        test('a regenerated SP certificate is refused by Keycloak until it is registered there', async ({
            browser,
            page,
        }) => {
            identityProvider = await createKeycloakSamlIdentityProvider();

            const metadataUrl = serviceProviderMetadataUrl(identityProvider);
            const originalCertificate = await fetchSamlSigningCertificate(metadataUrl);

            keycloakSamlClientId = await createKeycloakSamlClient(identityProvider, {
                requestSigningCertificate: originalCertificate,
            });

            keycloakUser = await createKeycloakUser(identityProvider.domain);

            await login(page, TEST_USER.email, TEST_USER.password);

            await page.goto('/automation/settings/identity-providers');

            await page
                .getByRole('row', {name: new RegExp(identityProvider.name)})
                .getByRole('button')
                .first()
                .click();

            const certificateField = page.getByRole('dialog').getByLabel('SP signing certificate');

            await expect(certificateField).toHaveValue(/BEGIN CERTIFICATE/);

            expect(pemBody(await certificateField.inputValue())).toBe(originalCertificate);

            await page
                .getByRole('dialog')
                .getByRole('button', {name: /regenerate certificate/i})
                .click();

            await page.getByRole('alertdialog').getByRole('button', {name: 'Regenerate'}).click();

            await expect(certificateField).not.toHaveValue(new RegExp(originalCertificate.slice(0, 40)));

            const regeneratedCertificate = await fetchSamlSigningCertificate(metadataUrl);

            expect(regeneratedCertificate).not.toBe(originalCertificate);
            expect(pemBody(await certificateField.inputValue())).toBe(regeneratedCertificate);

            await withFreshPage(browser, async (ssoPage) => {
                await continueWithSso(ssoPage, keycloakUser!.email, identityProvider!);

                await expect(ssoPage.getByText(/invalid requester|invalid signature/i)).toBeVisible();
            });

            await setKeycloakSamlClientRequestSigningCertificate(keycloakSamlClientId, regeneratedCertificate);

            await withFreshPage(browser, async (ssoPage) => {
                await continueWithSso(ssoPage, keycloakUser!.email, identityProvider!);

                await signInAtKeycloak(ssoPage, keycloakUser!);

                await expectSignedInto(ssoPage);
            });
        });
    });
});
