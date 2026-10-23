import {APIRequestContext, APIResponse, Page, expect, request} from '@playwright/test';
import {createHmac} from 'node:crypto';

import getRandomString from './getRandomString';

export const AUTHENTICATION_BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:5173';

export const AUTHENTICATION_MOCKS_ENABLED = process.env.BYTECHEF_E2E_AUTHENTICATION_MOCKS === '1';

export const AUTHENTICATION_MOCKS_MISSING_REASON =
    'Set BYTECHEF_E2E_AUTHENTICATION_MOCKS=1 after starting test/playwright/docker/authentication and a server ' +
    'configured as described in the "Authentication specs" section of test/playwright/README.md';

const MAILPIT_URL = process.env.PLAYWRIGHT_MAILPIT_URL || 'http://localhost:8025';

const OIDC_MOCK_URL = process.env.BYTECHEF_E2E_OIDC_MOCK_URL || 'http://localhost:18090';

const SAML_IDP_METADATA_URL =
    process.env.BYTECHEF_E2E_SAML_IDP_METADATA_URL || 'http://localhost:18080/simplesaml/saml2/idp/metadata.php';

export const INVITED_USER_PASSWORD = 'Playwright-Auth-1';

export const SAML_IDP_USER = {email: 'user1@example.com', password: 'user1pass', username: 'user1'} as const;

export interface IdentityProviderI {
    domain: string;
    id: string;
    issuerPath: string;
    name: string;
    registrationId: string;
}

export function uniqueSuffix(): string {
    return getRandomString().replace(/-/g, '').slice(0, 12);
}

export function uniqueEmail(prefix: string, domain = 'social.e2e.example.com'): string {
    return `${prefix}-${uniqueSuffix()}@${domain}`;
}

async function getXsrfToken(apiContext: APIRequestContext): Promise<string> {
    await apiContext.get('/api/account');

    const storageState = await apiContext.storageState();

    return storageState.cookies.find((cookie) => cookie.name === 'XSRF-TOKEN')?.value || '';
}

export async function graphql<T>(
    apiContext: APIRequestContext,
    query: string,
    variables: Record<string, unknown> = {}
): Promise<T> {
    const xsrfToken = await getXsrfToken(apiContext);

    const response = await apiContext.post('/graphql', {
        data: {query, variables},
        headers: {'X-XSRF-TOKEN': xsrfToken},
    });

    const body = await response.json();

    expect(body.errors, JSON.stringify(body.errors)).toBeUndefined();

    return body.data as T;
}

const IDENTITY_PROVIDER_FIELDS = 'domains id name registrationId';

export async function createOidcIdentityProvider(
    apiContext: APIRequestContext,
    options: {
        autoProvision?: boolean;
        clientId?: string;
        clientSecret?: string;
        enforced?: boolean;
        issuerUri?: string;
        namePrefix?: string;
    } = {}
): Promise<IdentityProviderI> {
    const suffix = uniqueSuffix();
    const domain = `sso-${suffix}.e2e.example.com`;
    const issuerPath = `sso-${suffix}`;
    const name = `${options.namePrefix || 'E2E OIDC'} ${suffix}`;

    const data = await graphql<{createIdentityProvider: {id: string; registrationId: string}}>(
        apiContext,
        `mutation CreateIdentityProvider($input: IdentityProviderInput!) {
            createIdentityProvider(input: $input) { ${IDENTITY_PROVIDER_FIELDS} }
        }`,
        {
            input: {
                autoProvision: options.autoProvision ?? true,
                clientId: options.clientId || 'e2e-sso',
                clientSecret: options.clientSecret || 'e2e-sso-secret',
                defaultAuthority: 'ROLE_USER',
                domains: [domain],
                enabled: true,
                enforced: options.enforced ?? false,
                issuerUri: options.issuerUri || `${OIDC_MOCK_URL}/${issuerPath}`,
                name,
                type: 'OIDC',
            },
        }
    );

    return {domain, issuerPath, name, ...data.createIdentityProvider};
}

export async function createSamlIdentityProvider(
    apiContext: APIRequestContext,
    options: {domain?: string; metadataUri?: string; namePrefix?: string} = {}
): Promise<IdentityProviderI> {
    const domain = options.domain || 'example.com';
    const name = `${options.namePrefix || 'E2E SAML'} ${uniqueSuffix()}`;

    const data = await graphql<{createIdentityProvider: {id: string; registrationId: string}}>(
        apiContext,
        `mutation CreateIdentityProvider($input: IdentityProviderInput!) {
            createIdentityProvider(input: $input) { ${IDENTITY_PROVIDER_FIELDS} }
        }`,
        {
            input: {
                autoProvision: true,
                defaultAuthority: 'ROLE_USER',
                domains: [domain],
                enabled: true,
                metadataUri: options.metadataUri || SAML_IDP_METADATA_URL,
                name,
                type: 'SAML',
            },
        }
    );

    return {domain, issuerPath: '', name, ...data.createIdentityProvider};
}

export async function setIdentityProviderEnforced(
    apiContext: APIRequestContext,
    identityProvider: IdentityProviderI,
    enforced: boolean
): Promise<void> {
    await graphql(
        apiContext,
        `
            mutation UpdateIdentityProvider($id: ID!, $input: IdentityProviderInput!) {
                updateIdentityProvider(id: $id, input: $input) {
                    id
                }
            }
        `,
        {
            id: identityProvider.id,
            input: {
                autoProvision: true,
                clientId: 'e2e-sso',
                defaultAuthority: 'ROLE_USER',
                domains: [identityProvider.domain],
                enabled: true,
                enforced,
                issuerUri: `${OIDC_MOCK_URL}/${identityProvider.issuerPath}`,
                name: identityProvider.name,
                type: 'OIDC',
            },
        }
    );
}

export async function deleteIdentityProvider(
    apiContext: APIRequestContext,
    identityProvider: IdentityProviderI | undefined
): Promise<void> {
    if (!identityProvider) {
        return;
    }

    await graphql(apiContext, 'mutation DeleteIdentityProvider($id: ID!) { deleteIdentityProvider(id: $id) }', {
        id: identityProvider.id,
    }).catch(() => undefined);
}

/**
 * Removes SAML providers left behind by an interrupted run: they all use the SAML IdP's fixed example.com domain, and
 * two providers on one domain make SSO discovery ambiguous.
 */
export async function deleteLeftoverSamlIdentityProviders(apiContext: APIRequestContext): Promise<void> {
    const data = await graphql<{identityProviders: {id: string; name: string}[]}>(
        apiContext,
        '{ identityProviders { id name } }'
    );

    for (const identityProvider of data.identityProviders) {
        if (identityProvider.name.startsWith('E2E SAML ')) {
            await graphql(apiContext, 'mutation DeleteIdentityProvider($id: ID!) { deleteIdentityProvider(id: $id) }', {
                id: identityProvider.id,
            });
        }
    }
}

export async function generateScimToken(
    apiContext: APIRequestContext,
    identityProvider: IdentityProviderI
): Promise<string> {
    const data = await graphql<{generateIdentityProviderScimApiKey: string}>(
        apiContext,
        'mutation GenerateScimToken($id: ID!) { generateIdentityProviderScimApiKey(id: $id) }',
        {id: identityProvider.id}
    );

    return data.generateIdentityProviderScimApiKey;
}

export async function scimRequest(
    token: string | undefined,
    method: 'DELETE' | 'GET' | 'PATCH' | 'POST' | 'PUT',
    path: string,
    data?: unknown
): Promise<{body: Record<string, unknown> | undefined; response: APIResponse}> {
    const scimContext = await request.newContext({baseURL: AUTHENTICATION_BASE_URL});

    try {
        const response = await scimContext.fetch(`/api/scim/v2${path}`, {
            data,
            headers: {
                'Content-Type': 'application/scim+json',
                ...(token ? {Authorization: `Bearer ${token}`} : {}),
            },
            method,
        });

        const text = await response.text();

        return {body: text ? JSON.parse(text) : undefined, response};
    } finally {
        await scimContext.dispose();
    }
}

export async function createActiveUser(email: string, scimToken: string): Promise<string> {
    const {body, response} = await scimRequest(scimToken, 'POST', '/Users', {
        active: true,
        emails: [{primary: true, value: email}],
        name: {familyName: 'User', givenName: 'E2E'},
        schemas: ['urn:ietf:params:scim:schemas:core:2.0:User'],
        userName: email,
    });

    expect(response.status(), JSON.stringify(body)).toBe(201);

    return String(body!.id);
}

export async function inviteUser(apiContext: APIRequestContext, email: string): Promise<void> {
    await graphql(
        apiContext,
        'mutation InviteUser($email: String!, $role: String!) { inviteUser(email: $email, role: $role) }',
        {email, role: 'ROLE_USER'}
    );
}

export async function inviteAndClaimUser(apiContext: APIRequestContext, email: string): Promise<void> {
    await inviteUser(apiContext, email);

    const mailpitContext = await request.newContext({baseURL: MAILPIT_URL});
    let claimKey = '';

    try {
        await expect(async () => {
            const search = await (
                await mailpitContext.get('/api/v1/search', {params: {limit: '1', query: `to:"${email}"`}})
            ).json();

            const messageId = search.messages?.[0]?.ID;

            expect(messageId, `no claim mail for ${email} yet`).toBeTruthy();

            const message = await (await mailpitContext.get(`/api/v1/message/${messageId}`)).json();

            const match = `${message.HTML} ${message.Text}`.match(/accept-invitation\?key=([A-Za-z0-9]+)/);

            expect(match, `no claim link in the mail to ${email}`).toBeTruthy();

            claimKey = match![1];
        }).toPass({timeout: 30000});
    } finally {
        await mailpitContext.dispose();
    }

    const anonymousContext = await request.newContext({baseURL: AUTHENTICATION_BASE_URL});

    try {
        const response = await anonymousContext.post('/api/account/accept-invitation', {
            data: {key: claimKey, newPassword: INVITED_USER_PASSWORD},
            headers: {'X-XSRF-TOKEN': await getXsrfToken(anonymousContext)},
        });

        expect(response.ok(), `claiming ${email} failed: ${response.status()}`).toBeTruthy();
    } finally {
        await anonymousContext.dispose();
    }
}

/**
 * Completes the mock OIDC provider's sign-in page, which plays both Google and the SSO identity providers.
 */
export async function signInAtOidcMock(page: Page, claims: Record<string, unknown>): Promise<void> {
    await page.waitForURL(/localhost:18090\/.*\/authorize/);

    await page.locator('input[name="username"]').fill(String(claims.sub));

    await page.locator('textarea[name="claims"]').fill(JSON.stringify(claims));

    await page.getByRole('button', {name: 'Sign-in'}).click();
}

export async function signInAtGitHubMock(page: Page, profile: Record<string, unknown>): Promise<void> {
    await page.waitForURL(/localhost:18091\/login\/oauth\/authorize/);

    await page.getByLabel('Claims').fill(JSON.stringify(profile));

    await page.getByRole('button', {name: 'Sign in to GitHub'}).click();
}

export async function signInAtSamlIdp(page: Page): Promise<void> {
    await page.waitForURL(/localhost:18080\/simplesaml/);

    await page.getByLabel('Username').fill(SAML_IDP_USER.username);

    await page.getByLabel('Password').fill(SAML_IDP_USER.password);

    await page.getByRole('button', {name: 'Login'}).click();
}

export async function continueWithSso(page: Page, email: string, identityProvider: IdentityProviderI): Promise<void> {
    await page.goto('/login');

    await page.getByLabel('Email', {exact: true}).fill(email);

    await page.getByLabel('Password', {exact: true}).click();

    await page.getByRole('button', {name: `Continue with ${identityProvider.name}`}).click();
}

export async function startSocialSignIn(page: Page, provider: 'Github' | 'Google'): Promise<void> {
    await page.goto('/login');

    await page.getByRole('button', {name: `Continue with ${provider}`}).click();
}

export async function expectSignedInto(page: Page): Promise<void> {
    await expect(page).toHaveURL(/\/automation\//, {timeout: 30000});
}

export async function expectSignInRefused(page: Page, message: string | RegExp): Promise<void> {
    await expect(page).toHaveURL(/\/account-error/, {timeout: 30000});

    await expect(page.getByText(message)).toBeVisible();
}

export async function currentAccount(
    page: Page
): Promise<{authorities: string[]; email: string; firstName?: string; lastName?: string; login: string}> {
    const response = await page.request.get('/api/account');

    expect(response.ok(), `GET /api/account answered ${response.status()}`).toBeTruthy();

    return response.json();
}

/**
 * The current RFC 6238 code (SHA-1, six digits, 30-second steps) for a base32 secret, as an authenticator app shows it.
 */
export function totpCode(base32Secret: string): string {
    const alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';
    const normalizedSecret = base32Secret.replace(/=+$/, '').toUpperCase();

    let bits = '';

    for (const character of normalizedSecret) {
        bits += alphabet.indexOf(character).toString(2).padStart(5, '0');
    }

    const keyBytes: number[] = [];

    for (let index = 0; index + 8 <= bits.length; index += 8) {
        keyBytes.push(parseInt(bits.slice(index, index + 8), 2));
    }

    const counter = Buffer.alloc(8);

    counter.writeBigUInt64BE(BigInt(Math.floor(Date.now() / 1000 / 30)));

    const digest = createHmac('sha1', Buffer.from(keyBytes)).update(counter).digest();
    const offset = digest[digest.length - 1] & 0x0f;
    const code = (digest.readUInt32BE(offset) & 0x7fffffff) % 1000000;

    return code.toString().padStart(6, '0');
}
