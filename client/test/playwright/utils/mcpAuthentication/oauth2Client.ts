import {type APIRequestContext, type APIResponse, type Page, expect} from '@playwright/test';
import {createHash, randomBytes} from 'node:crypto';

import {MCP_TEST_CONFIGURATION} from './mcpTestConfiguration';

export const OAUTH2_REDIRECT_URI = 'http://127.0.0.1:33418/callback';

export const REGISTERED_CLIENT_IDS: string[] = [];

let authorizationServerMetadata: AuthorizationServerMetadataI | undefined;

export interface AuthorizationServerMetadataI {
    authorization_endpoint: string;
    issuer: string;
    registration_endpoint: string;
    scopes_supported?: string[];
    token_endpoint: string;
}

export interface OAuth2TokensI {
    accessToken: string;
    clientId: string;
    expiresIn: number;
    refreshToken?: string;
}

interface AuthorizationRequestProps {
    clientId: string;
    codeChallenge: string;
    redirectUri?: string;
    resourceUrl?: string;
    scope?: string;
}

interface ObtainAccessTokenProps {
    resourceUrl: string;
    scope: string;
}

export async function fetchAuthorizationServerMetadata(
    requestContext: APIRequestContext
): Promise<AuthorizationServerMetadataI> {
    if (authorizationServerMetadata) {
        return authorizationServerMetadata;
    }

    const response = await requestContext.get(
        `${MCP_TEST_CONFIGURATION.publicUrl}/.well-known/oauth-authorization-server`
    );

    expect(response.status()).toBe(200);

    authorizationServerMetadata = (await response.json()) as AuthorizationServerMetadataI;

    return authorizationServerMetadata;
}

export async function registerPublicClient(
    requestContext: APIRequestContext,
    redirectUri: string = OAUTH2_REDIRECT_URI
): Promise<string> {
    const metadata = await fetchAuthorizationServerMetadata(requestContext);

    const response = await requestContext.post(metadata.registration_endpoint, {
        data: {
            client_name: 'playwright-mcp-authentication',
            grant_types: ['authorization_code', 'refresh_token'],
            redirect_uris: [redirectUri],
            response_types: ['code'],
            token_endpoint_auth_method: 'none',
        },
    });

    expect([200, 201]).toContain(response.status());

    const registration = (await response.json()) as {client_id: string};

    REGISTERED_CLIENT_IDS.push(registration.client_id);

    return registration.client_id;
}

export function createPkcePair(): {codeChallenge: string; codeVerifier: string} {
    const codeVerifier = randomBytes(48).toString('base64url');

    return {codeChallenge: createHash('sha256').update(codeVerifier).digest('base64url'), codeVerifier};
}

export async function createAuthorizationUrl(
    requestContext: APIRequestContext,
    {clientId, codeChallenge, redirectUri = OAUTH2_REDIRECT_URI, resourceUrl, scope}: AuthorizationRequestProps
): Promise<string> {
    const metadata = await fetchAuthorizationServerMetadata(requestContext);

    const authorizationUrl = new URL(metadata.authorization_endpoint);

    authorizationUrl.searchParams.set('client_id', clientId);
    authorizationUrl.searchParams.set('code_challenge', codeChallenge);
    authorizationUrl.searchParams.set('code_challenge_method', 'S256');
    authorizationUrl.searchParams.set('redirect_uri', redirectUri);
    authorizationUrl.searchParams.set('response_type', 'code');
    authorizationUrl.searchParams.set('state', randomBytes(12).toString('base64url'));

    if (resourceUrl) {
        authorizationUrl.searchParams.set('resource', resourceUrl);
    }

    if (scope) {
        authorizationUrl.searchParams.set('scope', scope);
    }

    return authorizationUrl.toString();
}

async function approveConsentInBrowser(page: Page, authorizationUrl: string): Promise<string> {
    const callbackRequestPromise = page.waitForRequest((callbackRequest) =>
        callbackRequest.url().startsWith(OAUTH2_REDIRECT_URI)
    );

    await page.goto(authorizationUrl);

    await expect(page).toHaveURL(/\/oauth2\/consent/);

    await page.getByRole('button', {name: 'Allow'}).click();

    const callbackRequest = await callbackRequestPromise;

    const authorizationCode = new URL(callbackRequest.url()).searchParams.get('code');

    expect(authorizationCode).toBeTruthy();

    return authorizationCode!;
}

export async function requestTokens(
    requestContext: APIRequestContext,
    form: Record<string, string>
): Promise<APIResponse> {
    const metadata = await fetchAuthorizationServerMetadata(requestContext);

    return requestContext.post(metadata.token_endpoint, {form});
}

export async function obtainAccessToken(
    page: Page,
    requestContext: APIRequestContext,
    {resourceUrl, scope}: ObtainAccessTokenProps
): Promise<OAuth2TokensI> {
    const clientId = await registerPublicClient(requestContext);
    const {codeChallenge, codeVerifier} = createPkcePair();

    const authorizationUrl = await createAuthorizationUrl(requestContext, {
        clientId,
        codeChallenge,
        resourceUrl,
        scope,
    });

    const authorizationCode = await approveConsentInBrowser(page, authorizationUrl);

    const tokenResponse = await requestTokens(requestContext, {
        client_id: clientId,
        code: authorizationCode,
        code_verifier: codeVerifier,
        grant_type: 'authorization_code',
        redirect_uri: OAUTH2_REDIRECT_URI,
        resource: resourceUrl,
    });

    expect(tokenResponse.status()).toBe(200);

    const tokens = (await tokenResponse.json()) as {access_token: string; expires_in: number; refresh_token?: string};

    return {
        accessToken: tokens.access_token,
        clientId,
        expiresIn: tokens.expires_in,
        refreshToken: tokens.refresh_token,
    };
}
