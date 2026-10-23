import {APIRequestContext, expect, test} from '@playwright/test';

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
import {
    KEYCLOAK_ISSUER_URI,
    KEYCLOAK_OIDC_CLIENT,
    KEYCLOAK_SAML_METADATA_URI,
    type KeycloakUserI,
    createKeycloakSamlClient,
    createKeycloakUser,
    deleteKeycloakClient,
    deleteKeycloakUser,
    signInAtKeycloak,
} from '../../utils/keycloakUtils';
import {createAdminApiContext} from '../../utils/rbacUtils';

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
    });
});
