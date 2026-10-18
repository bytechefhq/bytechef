import {APIRequestContext, expect, test} from '@playwright/test';

import {
    AUTHENTICATION_BASE_URL,
    AUTHENTICATION_MOCKS_ENABLED,
    AUTHENTICATION_MOCKS_MISSING_REASON,
    type IdentityProviderI,
    createActiveUser,
    createOidcIdentityProvider,
    deleteIdentityProvider,
    expectSignInRefused,
    expectSignedInto,
    generateScimToken,
    scimRequest,
    signInAtOidcMock,
    startSocialSignIn,
    uniqueEmail,
    uniqueSuffix,
} from '../../utils/authenticationUtils';
import {TEST_USER} from '../../utils/constants';
import {login} from '../../utils/login';
import {createAdminApiContext} from '../../utils/rbacUtils';

const SCIM_ERROR_SCHEMA = 'urn:ietf:params:scim:api:messages:2.0:Error';

test.describe('SCIM provisioning (#1040)', () => {
    test.skip(!AUTHENTICATION_MOCKS_ENABLED, AUTHENTICATION_MOCKS_MISSING_REASON);

    let adminApiContext: APIRequestContext;
    let identityProvider: IdentityProviderI;
    let scimToken: string;

    test.beforeAll(async () => {
        adminApiContext = await createAdminApiContext(AUTHENTICATION_BASE_URL);

        identityProvider = await createOidcIdentityProvider(adminApiContext);

        scimToken = await generateScimToken(adminApiContext, identityProvider);
    });

    test.afterAll(async () => {
        await deleteIdentityProvider(adminApiContext, identityProvider);

        await adminApiContext?.dispose();
    });

    test('an administrator generates a token in the provider dialog and it authenticates SCIM calls', async ({
        page,
    }) => {
        const dialogIdentityProvider = await createOidcIdentityProvider(adminApiContext);

        try {
            await login(page, TEST_USER.email, TEST_USER.password);

            await page.goto('/automation/settings/identity-providers');

            await page
                .getByRole('row', {name: new RegExp(dialogIdentityProvider.name)})
                .getByRole('button')
                .first()
                .click();

            await expect(page.getByLabel('SCIM base URL')).toHaveValue(`${AUTHENTICATION_BASE_URL}/api/scim/v2`);

            await page.getByRole('button', {name: 'Generate SCIM Token'}).click();

            const tokenInput = page.getByLabel('SCIM token');

            await expect(tokenInput).toHaveValue(/^bcscim_/);

            const dialogToken = await tokenInput.inputValue();

            await expect(page.getByRole('button', {name: 'Rotate SCIM Token'})).toBeVisible();

            const {response} = await scimRequest(dialogToken, 'GET', '/ServiceProviderConfig');

            expect(response.status()).toBe(200);
        } finally {
            await deleteIdentityProvider(adminApiContext, dialogIdentityProvider);
        }
    });

    test('requests without a valid token are refused with a SCIM error', async () => {
        for (const token of [undefined, 'bcscim_public_not-a-real-token']) {
            const {body, response} = await scimRequest(token, 'GET', '/Users');

            expect(response.status()).toBe(401);
            expect(body!.schemas).toEqual([SCIM_ERROR_SCHEMA]);
        }
    });

    test('users are created once, found case-insensitively and duplicates are rejected', async () => {
        const email = uniqueEmail('scim');

        const id = await createActiveUser(email, scimToken);

        const duplicate = await scimRequest(scimToken, 'POST', '/Users', {
            emails: [{primary: true, value: email}],
            userName: email,
        });

        expect(duplicate.response.status()).toBe(409);
        expect(duplicate.body!.scimType).toBe('uniqueness');

        const {body} = await scimRequest(
            scimToken,
            'GET',
            `/Users?filter=${encodeURIComponent(`userName eq "${email.toUpperCase()}"`)}`
        );

        expect(body!.totalResults).toBe(1);
        expect((body!.Resources as {id: string}[])[0].id).toBe(id);

        const unsupported = await scimRequest(scimToken, 'GET', `/Users?filter=${encodeURIComponent('title sw "C"')}`);

        expect(unsupported.response.status()).toBe(400);
        expect(unsupported.body!.scimType).toBe('invalidFilter');
    });

    test('deactivating a user stops their next sign-in', async ({page}) => {
        const email = uniqueEmail('deprovisioned');
        const subject = `google-${uniqueSuffix()}`;

        const id = await createActiveUser(email, scimToken);

        await startSocialSignIn(page, 'Google');

        await signInAtOidcMock(page, {email, email_verified: true, sub: subject});

        await expectSignedInto(page);

        const {response} = await scimRequest(scimToken, 'PATCH', `/Users/${id}`, {
            Operations: [{op: 'replace', value: {active: false}}],
            schemas: ['urn:ietf:params:scim:api:messages:2.0:PatchOp'],
        });

        expect(response.status()).toBe(200);

        await page.context().clearCookies();

        await startSocialSignIn(page, 'Google');

        await signInAtOidcMock(page, {email, email_verified: true, sub: subject});

        await expectSignInRefused(page, 'This account has been deactivated. Contact your administrator.');
    });

    test('groups are read-only and deleted users are gone', async () => {
        const groups = await scimRequest(scimToken, 'GET', '/Groups');

        expect(groups.response.status()).toBe(200);

        const groupDelete = await scimRequest(
            scimToken,
            'DELETE',
            `/Groups/${(groups.body!.Resources as {id: string}[])[0].id}`
        );

        expect(groupDelete.response.status()).toBe(501);

        const id = await createActiveUser(uniqueEmail('deleted'), scimToken);

        expect((await scimRequest(scimToken, 'DELETE', `/Users/${id}`)).response.status()).toBe(204);

        const afterDelete = await scimRequest(scimToken, 'GET', `/Users/${id}`);

        expect(afterDelete.response.status()).toBe(404);
        expect(afterDelete.body!.schemas).toEqual([SCIM_ERROR_SCHEMA]);
    });
});
