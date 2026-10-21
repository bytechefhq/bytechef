import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {jsonResponse} from '../test/mockFetch';
import {
    cookiePair,
    fakeBytechef,
    type FakeBytechefOptionsI,
    findSetCookie,
    SESSION_SECRET,
    sessionCookieHeader,
    testSessionStore,
    tokenResponse,
} from '../test/authServer';
import {callbackHandler, loginHandler, logoutHandler} from './authHandlers';
import {PENDING_LOGIN_COOKIE, SESSION_COOKIE} from './session';

const APP = 'https://crm.example.com';

function options(fake: ReturnType<typeof fakeBytechef>) {
    return {appKey: 'key-1', fetch: fake.fetch, sessionSecret: SESSION_SECRET, url: 'https://bc.example'};
}

async function startLogin(fakeOptions: FakeBytechefOptionsI = {}, returnTo = '/orders?status=open') {
    const fake = fakeBytechef(fakeOptions);
    const response = await loginHandler(options(fake))({
        request: new Request(`${APP}/auth/login?returnTo=${encodeURIComponent(returnTo)}`),
    });
    const authorizeUrl = new URL(response.headers.get('location')!);
    const pendingCookie = cookiePair(findSetCookie(response, PENDING_LOGIN_COOKIE)!);

    return {authorizeUrl, fake, pendingCookie, response, state: authorizeUrl.searchParams.get('state')!};
}

function callbackRequest(query: string, cookie?: string): {request: Request} {
    return {request: new Request(`${APP}/auth/callback?${query}`, {headers: cookie ? {cookie} : {}})};
}

describe('loginHandler', () => {
    afterEach(() => {
        vi.unstubAllEnvs();
    });

    it('redirects to the authorization endpoint and keeps the pending sign-in in a sealed cookie', async () => {
        const {authorizeUrl, response} = await startLogin();

        expect(response.status).toBe(302);
        expect(response.headers.get('cache-control')).toBe('no-store');
        expect(`${authorizeUrl.origin}${authorizeUrl.pathname}`).toBe('https://bc.example/oauth2/authorize');
        expect(authorizeUrl.searchParams.get('redirect_uri')).toBe(`${APP}/auth/callback`);
        expect(authorizeUrl.searchParams.get('code_challenge_method')).toBe('S256');

        const pending = findSetCookie(response, PENDING_LOGIN_COOKIE)!;

        expect(pending).toMatch(/; Path=\/auth; Max-Age=600; HttpOnly; SameSite=Lax; Secure$/);
        expect(pending).not.toContain(authorizeUrl.searchParams.get('state')!);
        expect(pending).not.toContain('orders');
    });

    it('takes the redirect URI from appUrl when set', async () => {
        const fake = fakeBytechef();
        const response = await loginHandler({...options(fake), appUrl: 'https://public.example.com/'})({
            request: new Request('http://10.0.0.5:3000/auth/login'),
        });

        expect(new URL(response.headers.get('location')!).searchParams.get('redirect_uri')).toBe(
            'https://public.example.com/auth/callback'
        );
    });

    it('marks the cookies Secure from appUrl when a proxy forwards a local host', async () => {
        const fake = fakeBytechef({token: () => tokenResponse('access-1', 'refresh-1')});
        const proxied = {...options(fake), appUrl: 'https://crm.example'};
        const login = await loginHandler(proxied)({request: new Request('http://127.0.0.1:3000/auth/login')});
        const pendingCookie = findSetCookie(login, PENDING_LOGIN_COOKIE)!;
        const state = new URL(login.headers.get('location')!).searchParams.get('state')!;
        const callback = await callbackHandler(proxied)({
            request: new Request(`http://127.0.0.1:3000/auth/callback?code=code-1&state=${state}`, {
                headers: {cookie: cookiePair(pendingCookie)},
            }),
        });
        const logout = await logoutHandler(proxied)({
            request: new Request('http://127.0.0.1:3000/auth/logout', {
                headers: {origin: 'https://crm.example'},
                method: 'POST',
            }),
        });

        expect(pendingCookie).toMatch(/; Secure$/);
        expect(findSetCookie(callback, SESSION_COOKIE)).toMatch(/; Secure$/);
        expect(findSetCookie(callback, PENDING_LOGIN_COOKIE)).toMatch(/Max-Age=0; HttpOnly; SameSite=Lax; Secure$/);
        expect(findSetCookie(logout, SESSION_COOKIE)).toMatch(/Max-Age=0; HttpOnly; SameSite=Lax; Secure$/);
    });

    it('refuses to start a sign-in without a session secret', async () => {
        vi.stubEnv('BYTECHEF_SESSION_SECRET', 'too-short');

        const fake = fakeBytechef();

        await expect(
            loginHandler({appKey: 'key-1', fetch: fake.fetch, url: 'https://bc.example'})({
                request: new Request(`${APP}/auth/login`),
            })
        ).rejects.toThrowError(/BYTECHEF_SESSION_SECRET/);
    });

    it.each(['/.//evil.com', '/%2e%2e//evil.com', '/a/..//evil.com', '/./%2e//evil.com', '/..//evil.com'])(
        'never redirects to another origin from %j with sign-in off',
        async (returnTo) => {
            const fake = fakeBytechef({mode: 'NONE'});
            const response = await loginHandler(options(fake))({
                request: new Request(`${APP}/auth/login?returnTo=${encodeURIComponent(returnTo)}`),
            });
            const location = response.headers.get('location')!;

            expect(location).toBe('/');
            expect(new URL(location, APP).origin).toBe(APP);
        }
    );

    it('explains a sign-in that is not set up instead of failing', async () => {
        const fake = fakeBytechef({noClient: true});
        const response = await loginHandler(options(fake))({
            request: new Request(`${APP}/auth/login?returnTo=/orders`),
        });

        expect(response.status).toBe(503);
        expect(response.headers.get('cache-control')).toBe('no-store');
        expect(response.headers.get('location')).toBeNull();
        expect(await response.text()).toMatch(/Sign-in is not set up for this app yet/);
        expect(findSetCookie(response, PENDING_LOGIN_COOKIE)).toBeUndefined();
    });

    it('never starts a sign-in when the instance has sign-in off', async () => {
        const fake = fakeBytechef({mode: 'NONE'});
        const response = await loginHandler(options(fake))({
            request: new Request(`${APP}/auth/login?returnTo=/orders`),
        });

        expect(response.status).toBe(302);
        expect(response.headers.get('location')).toBe('/orders');
        expect(findSetCookie(response, PENDING_LOGIN_COOKIE)).toBeUndefined();
    });
});

describe('callbackHandler', () => {
    it('refuses a state that does not match the pending sign-in', async () => {
        const {fake, pendingCookie, state} = await startLogin({token: () => tokenResponse('access-1', 'refresh-1')});
        const forged = `${state.slice(0, -1)}${state.endsWith('A') ? 'B' : 'A'}`;

        const response = await callbackHandler(options(fake))(
            callbackRequest(`code=code-1&state=${forged}`, pendingCookie)
        );

        expect(response.status).toBe(400);
        expect(fake.tokenCalls()).toHaveLength(0);
        expect(findSetCookie(response, SESSION_COOKIE)).toBeUndefined();
        expect(findSetCookie(response, PENDING_LOGIN_COOKIE)).toMatch(/Max-Age=0/);
        expect(await response.text()).not.toContain(forged);
    });

    it('refuses a callback without a pending sign-in or with a tampered one', async () => {
        const {fake, pendingCookie, state} = await startLogin({token: () => tokenResponse('access-1', 'refresh-1')});
        const handler = callbackHandler(options(fake));

        const withoutCookie = await handler(callbackRequest(`code=code-1&state=${state}`));
        const tampered = await handler(
            callbackRequest(`code=code-1&state=${state}`, `${pendingCookie.slice(0, -3)}AAA`)
        );

        expect(withoutCookie.status).toBe(400);
        expect(tampered.status).toBe(400);
        expect(fake.tokenCalls()).toHaveLength(0);
    });

    it('exchanges the code, stores the session and returns to the path the sign-in started from', async () => {
        const {authorizeUrl, fake, pendingCookie, state} = await startLogin({
            token: () => tokenResponse('access-1', 'refresh-1'),
        });

        const response = await callbackHandler(options(fake))(
            callbackRequest(`code=code-1&state=${state}`, pendingCookie)
        );

        expect(response.status).toBe(302);
        expect(response.headers.get('location')).toBe('/orders?status=open');
        expect(response.headers.get('cache-control')).toBe('no-store');

        const tokenForm = new URLSearchParams(fake.tokenCalls()[0]!.body);

        expect(tokenForm.get('code')).toBe('code-1');
        expect(tokenForm.get('redirect_uri')).toBe(authorizeUrl.searchParams.get('redirect_uri'));
        expect(tokenForm.get('code_verifier')).toMatch(/^[A-Za-z0-9_-]{43}$/);

        const sessionCookie = findSetCookie(response, SESSION_COOKIE)!;

        expect(sessionCookie).toMatch(/; Path=\/; Max-Age=3600; HttpOnly; SameSite=Lax; Secure$/);
        expect(sessionCookie).not.toContain('access-1');
        expect(findSetCookie(response, PENDING_LOGIN_COOKIE)).toMatch(/^bytechef_login=; Path=\/auth; Max-Age=0/);

        const read = await testSessionStore().readSession(
            new Request(`${APP}/`, {headers: {cookie: cookiePair(sessionCookie)}})
        );

        expect(read.session).toMatchObject({accessToken: 'access-1', refreshToken: 'refresh-1'});
    });

    it('answers access_denied with 403 and makes no token request', async () => {
        const {fake, pendingCookie, state} = await startLogin();

        const response = await callbackHandler(options(fake))(
            callbackRequest(`error=access_denied&error_description=secret-detail&state=${state}`, pendingCookie)
        );

        expect(response.status).toBe(403);
        expect(await response.text()).not.toContain('secret-detail');
        expect(fake.tokenCalls()).toHaveLength(0);
    });

    it.each([
        [{error: 'invalid_grant'}, 400, 400],
        [{error: 'invalid_client'}, 401, 500],
        [{error: 'temporarily_unavailable'}, 503, 502],
    ])('answers a token error %j without its details', async (body, status, expected) => {
        const {fake, pendingCookie, state} = await startLogin({
            token: () => jsonResponse({body: {...body, error_description: 'code-1 verifier-x'}, status}),
        });

        const response = await callbackHandler(options(fake))(
            callbackRequest(`code=code-1&state=${state}`, pendingCookie)
        );
        const text = await response.text();

        expect(response.status).toBe(expected);
        expect(text).not.toContain('code-1');
        expect(text).not.toContain('verifier');
        expect(findSetCookie(response, SESSION_COOKIE)).toBeUndefined();
        expect(findSetCookie(response, PENDING_LOGIN_COOKIE)).toMatch(/Max-Age=0/);
    });

    it.each(['/.//evil.com', '/%2e%2e//evil.com', '/a/..//evil.com', '/./%2e//evil.com', '/..//evil.com'])(
        'returns to / after sign-in when the return path %j normalises to another origin',
        async (returnTo) => {
            const {fake, pendingCookie, state} = await startLogin(
                {token: () => tokenResponse('access-1', 'refresh-1')},
                returnTo
            );

            const response = await callbackHandler(options(fake))(
                callbackRequest(`code=code-1&state=${state}`, pendingCookie)
            );
            const location = response.headers.get('location')!;

            expect(location).toBe('/');
            expect(new URL(location, APP).origin).toBe(APP);
        }
    );

    it('falls back to / when the stored return path leaves the origin', async () => {
        const {fake, pendingCookie, state} = await startLogin(
            {token: () => tokenResponse('access-1', 'refresh-1')},
            '//evil.example/steal'
        );

        const response = await callbackHandler(options(fake))(
            callbackRequest(`code=code-1&state=${state}`, pendingCookie)
        );

        expect(response.headers.get('location')).toBe('/');
    });
});

describe('logoutHandler', () => {
    let sessionCookie: string;

    beforeEach(async () => {
        sessionCookie = await sessionCookieHeader('access-1', 'refresh-1', Date.now() + 300_000);
    });

    it('clears the session cookie on a POST and sends the user home', async () => {
        const fake = fakeBytechef();
        const response = await logoutHandler(options(fake))({
            request: new Request(`${APP}/auth/logout`, {
                headers: {cookie: sessionCookie, origin: APP},
                method: 'POST',
            }),
        });

        expect(response.status).toBe(303);
        expect(response.headers.get('location')).toBe('/');
        expect(findSetCookie(response, SESSION_COOKIE)).toBe(
            'bytechef_session=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax; Secure'
        );
        expect(fake.calls).toHaveLength(0);
    });

    it('refuses a GET', async () => {
        const response = await logoutHandler(options(fakeBytechef()))({
            request: new Request(`${APP}/auth/logout`, {headers: {cookie: sessionCookie}}),
        });

        expect(response.status).toBe(405);
        expect(response.headers.get('allow')).toBe('POST');
        expect(findSetCookie(response, SESSION_COOKIE)).toBeUndefined();
    });

    it('refuses a cross-site POST', async () => {
        const handler = logoutHandler(options(fakeBytechef()));

        const crossSiteHeaders: Array<Record<string, string>> = [
            {origin: 'https://evil.example'},
            {'sec-fetch-site': 'cross-site'},
        ];

        for (const headers of crossSiteHeaders) {
            const response = await handler({
                request: new Request(`${APP}/auth/logout`, {
                    headers: {...headers, cookie: sessionCookie},
                    method: 'POST',
                }),
            });

            expect(response.status).toBe(403);
            expect(findSetCookie(response, SESSION_COOKIE)).toBeUndefined();
        }
    });
});
