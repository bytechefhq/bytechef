import {afterEach, describe, expect, it, vi} from 'vitest';

import {jsonResponse, lastRequest, mockFetch} from '../test/mockFetch';
import {createBytechefClient} from './client';
import {BytechefAppError, isSignInRequired} from './errors';
import {createCodeChallenge} from './pkce';
import type {AppContext} from './types';

const CONTEXT: AppContext = {
    appId: 1,
    appInstanceId: 2,
    appInstanceName: 'Production',
    appName: 'CRM',
    authorizationEndpoint: 'https://bc.example/oauth2/authorize',
    clientId: 'app-1',
    configuration: {},
    environment: 'PRODUCTION',
    issuer: 'https://bc.example',
    tokenEndpoint: 'https://bc.example/oauth2/token',
    userAuthMode: 'REQUIRED',
};

const CURRENT_USER = {
    email: 'ada@example.com',
    firstName: 'Ada',
    id: 7,
    lastName: 'Lovelace',
    login: 'ada',
    scopes: ['APP_USE'],
    workspaceRole: 'EDITOR',
};

function formOf(body: BodyInit | null | undefined): URLSearchParams {
    return new URLSearchParams(String(body));
}

function routes(tokenResponse: Response, context: AppContext = CONTEXT) {
    return (input: RequestInfo | URL) => {
        const url = String(input);

        if (url.endsWith('/api/app/v1/context')) {
            return jsonResponse({body: context});
        }

        if (url === context.tokenEndpoint) {
            return tokenResponse.clone();
        }

        return jsonResponse({body: CURRENT_USER});
    };
}

describe('auth', () => {
    afterEach(() => {
        vi.restoreAllMocks();
        vi.unstubAllGlobals();
    });

    it('createAuthorizationRequest builds a PKCE S256 URL from /context', async () => {
        const {calls, fetch} = mockFetch(jsonResponse({body: CONTEXT}));
        const client = createBytechefClient({appKey: 'key-1', fetch, url: 'https://bc.example'});

        const authorizationRequest = await client.auth.createAuthorizationRequest({
            redirectUri: 'https://crm.example/auth/callback',
        });

        const url = new URL(authorizationRequest.url);

        expect(`${url.origin}${url.pathname}`).toBe('https://bc.example/oauth2/authorize');
        expect(url.searchParams.get('response_type')).toBe('code');
        expect(url.searchParams.get('client_id')).toBe('app-1');
        expect(url.searchParams.get('redirect_uri')).toBe('https://crm.example/auth/callback');
        expect(url.searchParams.get('scope')).toBe('openid profile app:use');
        expect(url.searchParams.get('state')).toBe(authorizationRequest.state);
        expect(url.searchParams.get('code_challenge_method')).toBe('S256');
        expect(url.searchParams.get('code_challenge')).toBe(
            await createCodeChallenge(authorizationRequest.codeVerifier)
        );
        expect(url.searchParams.has('client_secret')).toBe(false);
        expect(authorizationRequest.codeVerifier).toMatch(/^[A-Za-z0-9\-._~]{43,128}$/);
        expect(calls).toHaveLength(1);
        expect(lastRequest(calls).url).toBe('https://bc.example/api/app/v1/context');
    });

    it('createAuthorizationRequest fails when the instance has no OAuth2 client', async () => {
        const {fetch} = mockFetch(
            jsonResponse({
                body: {...CONTEXT, authorizationEndpoint: null, clientId: null, issuer: null, tokenEndpoint: null},
            })
        );
        const client = createBytechefClient({appKey: 'key-1', fetch, url: 'https://bc.example'});

        await expect(
            client.auth.createAuthorizationRequest({redirectUri: 'https://crm.example/auth/callback'})
        ).rejects.toMatchObject({code: 'SIGN_IN_UNAVAILABLE'});
    });

    it('exchangeCode posts the verifier and returns tokens', async () => {
        vi.spyOn(Date, 'now').mockReturnValue(1_000_000);

        const {calls, fetch} = mockFetch(
            routes(
                jsonResponse({
                    body: {
                        access_token: 'access-1',
                        expires_in: 300,
                        id_token: 'id-1',
                        refresh_token: 'refresh-1',
                        scope: 'openid profile app:use',
                        token_type: 'Bearer',
                    },
                })
            )
        );
        const client = createBytechefClient({appKey: 'key-1', fetch, url: 'https://bc.example'});

        const tokens = await client.auth.exchangeCode({
            code: 'code-1',
            codeVerifier: 'verifier-1',
            redirectUri: 'https://crm.example/auth/callback',
        });

        expect(tokens).toEqual({
            accessToken: 'access-1',
            expiresAt: 1_300_000,
            refreshToken: 'refresh-1',
            scope: 'openid profile app:use',
            tokenType: 'Bearer',
        });

        const sent = lastRequest(calls);
        const form = formOf(sent.body);

        expect(sent.url).toBe('https://bc.example/oauth2/token');
        expect(sent.method).toBe('POST');
        expect(sent.headers.get('content-type')).toBe('application/x-www-form-urlencoded');
        expect(sent.headers.get('authorization')).toBeNull();
        expect(String(sent.body)).not.toContain('key-1');
        expect(Object.fromEntries(form)).toEqual({
            client_id: 'app-1',
            code: 'code-1',
            code_verifier: 'verifier-1',
            grant_type: 'authorization_code',
            redirect_uri: 'https://crm.example/auth/callback',
        });
    });

    it('refresh rotates the refresh token', async () => {
        const {calls, fetch} = mockFetch(
            routes(
                jsonResponse({
                    body: {access_token: 'access-2', expires_in: 300, refresh_token: 'refresh-2', token_type: 'Bearer'},
                })
            )
        );
        const client = createBytechefClient({appKey: 'key-1', fetch, url: 'https://bc.example'});

        const tokens = await client.auth.refresh({refreshToken: 'refresh-1'});

        expect(tokens.accessToken).toBe('access-2');
        expect(tokens.refreshToken).toBe('refresh-2');

        const sent = lastRequest(calls);

        expect(sent.headers.get('authorization')).toBeNull();
        expect(Object.fromEntries(formOf(sent.body))).toEqual({
            client_id: 'app-1',
            grant_type: 'refresh_token',
            refresh_token: 'refresh-1',
        });
    });

    it('refresh fails when the answer carries no new refresh token', async () => {
        const {fetch} = mockFetch(
            routes(jsonResponse({body: {access_token: 'access-2', expires_in: 300, token_type: 'Bearer'}}))
        );
        const client = createBytechefClient({appKey: 'key-1', fetch, url: 'https://bc.example'});

        await expect(client.auth.refresh({refreshToken: 'refresh-1'})).rejects.toMatchObject({
            code: 'INVALID_RESPONSE',
        });
    });

    it('surfaces a refused grant as SESSION_ENDED without the token or the verifier in the message', async () => {
        const {fetch} = mockFetch(
            routes(
                jsonResponse({
                    body: {error: 'invalid_grant', error_description: 'refresh-secret is not valid'},
                    status: 400,
                })
            )
        );
        const client = createBytechefClient({appKey: 'key-1', fetch, url: 'https://bc.example'});

        const refreshError = await client.auth.refresh({refreshToken: 'refresh-secret'}).catch((error) => error);
        const exchangeError = await client.auth
            .exchangeCode({code: 'code-secret', codeVerifier: 'verifier-secret', redirectUri: 'https://crm.example/cb'})
            .catch((error) => error);

        for (const error of [refreshError, exchangeError]) {
            expect(error).toBeInstanceOf(BytechefAppError);
            expect(error).toMatchObject({code: 'SESSION_ENDED', status: 400});
            expect(error.message).toContain('invalid_grant');
            expect(error.message).not.toMatch(/secret/);
        }
    });

    it('getCurrentUser throws APP_USER_REQUIRED without a token', async () => {
        const {calls, fetch} = mockFetch(jsonResponse({body: CURRENT_USER}));
        const client = createBytechefClient({appKey: 'key-1', fetch, url: 'https://bc.example'});

        await expect(client.auth.getCurrentUser()).rejects.toMatchObject({code: 'APP_USER_REQUIRED', status: 401});
        expect(calls).toHaveLength(0);
    });

    it('getCurrentUser returns the user of the token from /me', async () => {
        const {calls, fetch} = mockFetch(jsonResponse({body: CURRENT_USER}));
        const client = createBytechefClient({appKey: 'key-1', fetch, url: 'https://bc.example', userToken: 'access-1'});

        await expect(client.auth.getCurrentUser()).resolves.toEqual(CURRENT_USER);
        expect(lastRequest(calls).url).toBe('https://bc.example/api/app/v1/me');
        expect(lastRequest(calls).headers.get('x-bytechef-user-token')).toBe('access-1');
    });

    it.each([
        ['APP_USER_REQUIRED', 401],
        ['APP_USER_TOKEN_INVALID', 401],
        ['APP_USER_FORBIDDEN', 403],
    ])('surfaces %s from the server as BytechefAppError', async (code, status) => {
        const {fetch} = mockFetch(jsonResponse({body: {code, message: 'refused'}, status}));
        const client = createBytechefClient({appKey: 'key-1', fetch, url: 'https://bc.example', userToken: 'access-1'});

        const error = await client.auth.getCurrentUser().catch((caught) => caught);

        expect(error).toBeInstanceOf(BytechefAppError);
        expect(error).toMatchObject({code, status});
    });

    it('requests carry the user token header only when set', async () => {
        const withToken = mockFetch(jsonResponse({body: []}));
        const withoutToken = mockFetch(jsonResponse({body: []}));
        const withEmptyToken = mockFetch(jsonResponse({body: []}));

        await createBytechefClient({
            appKey: 'key-1',
            fetch: withToken.fetch,
            url: 'https://bc.example',
            userToken: 'access-1',
        }).connections.list();
        await createBytechefClient({appKey: 'key-1', fetch: withoutToken.fetch, url: 'https://bc.example'}).api.call({
            headers: {'X-ByteChef-User-Token': 'forged'},
            method: 'GET',
            path: 'v1/crm/leads',
        });
        await createBytechefClient({
            appKey: 'key-1',
            fetch: withEmptyToken.fetch,
            url: 'https://bc.example',
            userToken: '',
        }).connections.list();

        expect(lastRequest(withToken.calls).headers.get('x-bytechef-user-token')).toBe('access-1');
        expect(lastRequest(withToken.calls).headers.get('authorization')).toBe('Bearer key-1');
        expect(lastRequest(withoutToken.calls).headers.has('x-bytechef-user-token')).toBe(false);
        expect(lastRequest(withEmptyToken.calls).headers.has('x-bytechef-user-token')).toBe(false);
    });

    it('auth refuses to run in a browser', async () => {
        const {calls, fetch} = mockFetch(jsonResponse({body: CONTEXT}));
        const client = createBytechefClient({appKey: 'key-1', fetch, url: 'https://bc.example', userToken: 'access-1'});

        vi.stubGlobal('window', {});

        await expect(
            client.auth.createAuthorizationRequest({redirectUri: 'https://crm.example/auth/callback'})
        ).rejects.toThrowError(/server/i);
        await expect(
            client.auth.exchangeCode({code: 'c', codeVerifier: 'v', redirectUri: 'https://crm.example/cb'})
        ).rejects.toThrowError(/server/i);
        await expect(client.auth.refresh({refreshToken: 'r'})).rejects.toThrowError(/server/i);
        await expect(client.auth.getCurrentUser()).rejects.toThrowError(/server/i);
        expect(() => createBytechefClient({appKey: 'key-1', fetch, url: 'https://bc.example'})).toThrowError(/server/i);
        expect(calls).toHaveLength(0);
    });

    it('refetches the context after SIGN_IN_UNAVAILABLE and succeeds once the server reports a client', async () => {
        const {calls, fetch} = mockFetch(
            jsonResponse({
                body: {...CONTEXT, authorizationEndpoint: null, clientId: null, issuer: null, tokenEndpoint: null},
            }),
            jsonResponse({body: CONTEXT})
        );
        const client = createBytechefClient({appKey: 'key-1', fetch, url: 'https://bc.example'});

        await expect(
            client.auth.createAuthorizationRequest({redirectUri: 'https://crm.example/auth/callback'})
        ).rejects.toMatchObject({code: 'SIGN_IN_UNAVAILABLE'});

        const authorizationRequest = await client.auth.createAuthorizationRequest({
            redirectUri: 'https://crm.example/auth/callback',
        });

        expect(new URL(authorizationRequest.url).searchParams.get('client_id')).toBe('app-1');
        expect(calls).toHaveLength(2);
    });

    it('refetches the context after an invalid_client token error', async () => {
        const replacedClient = {...CONTEXT, clientId: 'app-2'};
        const {calls, fetch} = mockFetch(
            jsonResponse({body: CONTEXT}),
            jsonResponse({body: {error: 'invalid_client'}, status: 401}),
            jsonResponse({body: replacedClient}),
            jsonResponse({body: {access_token: 'access-2', refresh_token: 'refresh-2', token_type: 'Bearer'}})
        );
        const client = createBytechefClient({appKey: 'key-1', fetch, url: 'https://bc.example'});

        await expect(client.auth.refresh({refreshToken: 'refresh-1'})).rejects.toMatchObject({
            code: 'SIGN_IN_MISCONFIGURED',
            status: 401,
        });

        await client.auth.refresh({refreshToken: 'refresh-1'});

        expect(calls).toHaveLength(4);
        expect(lastRequest(calls).url).toBe('https://bc.example/oauth2/token');
        expect(formOf(lastRequest(calls).body).get('client_id')).toBe('app-2');
    });

    it('keeps the cached context after a refused grant', async () => {
        const {calls, fetch} = mockFetch(
            jsonResponse({body: CONTEXT}),
            jsonResponse({body: {error: 'invalid_grant'}, status: 400})
        );
        const client = createBytechefClient({appKey: 'key-1', fetch, url: 'https://bc.example'});

        await expect(client.auth.refresh({refreshToken: 'refresh-1'})).rejects.toMatchObject({code: 'SESSION_ENDED'});
        await expect(client.auth.refresh({refreshToken: 'refresh-1'})).rejects.toMatchObject({code: 'SESSION_ENDED'});

        expect(calls.filter((call) => String(call.input).endsWith('/context'))).toHaveLength(1);
    });

    it.each([
        ['invalid_client', 401, 'SIGN_IN_MISCONFIGURED'],
        ['invalid_request', 400, 'SIGN_IN_MISCONFIGURED'],
        ['unauthorized_client', 400, 'SIGN_IN_MISCONFIGURED'],
        [undefined, 404, 'SIGN_IN_MISCONFIGURED'],
        [undefined, 429, 'RATE_LIMITED'],
        ['server_error', 503, 'HTTP_ERROR'],
    ])('tells a token error %s (%i) from an ended session', async (oauthError, status, code) => {
        const {fetch} = mockFetch(
            routes(jsonResponse({body: oauthError === undefined ? {} : {error: oauthError}, status}))
        );
        const client = createBytechefClient({appKey: 'key-1', fetch, url: 'https://bc.example'});

        const error = await client.auth.refresh({refreshToken: 'refresh-1'}).catch((caught) => caught);

        expect(error).toBeInstanceOf(BytechefAppError);
        expect(error).toMatchObject({code, status});
        expect(isSignInRequired(error)).toBe(false);
    });

    it('does not follow a redirect from the token endpoint', async () => {
        const {calls, fetch} = mockFetch(
            routes(new Response(null, {headers: {location: 'https://elsewhere.example/token'}, status: 307}))
        );
        const client = createBytechefClient({appKey: 'key-1', fetch, url: 'https://bc.example'});

        const error = await client.auth
            .exchangeCode({code: 'code-1', codeVerifier: 'verifier-1', redirectUri: 'https://crm.example/cb'})
            .catch((caught) => caught);

        expect(error).toMatchObject({code: 'SIGN_IN_MISCONFIGURED', status: 307});
        expect(error.message).not.toContain('elsewhere');
        expect(lastRequest(calls).url).toBe('https://bc.example/oauth2/token');
        expect(calls[calls.length - 1]!.init?.redirect).toBe('manual');
        expect(calls).toHaveLength(2);
    });
});
