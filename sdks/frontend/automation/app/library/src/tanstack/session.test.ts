import {afterEach, describe, expect, it, vi} from 'vitest';

import {
    clearCookie,
    cookieOptions,
    createSessionSealer,
    createSessionStore,
    MAX_COOKIE_BYTES,
    normalizeBasePath,
    PENDING_LOGIN_COOKIE,
    readCookie,
    resolveSessionSecret,
    safeReturnPath,
    serializeCookie,
    SESSION_COOKIE,
    toSession,
} from './session';

const SECRET = 'a-session-secret-of-forty-characters-xx';

function base64Url(text: string): string {
    return Buffer.from(text).toString('base64url');
}

function realisticAccessToken(): string {
    const header = base64Url(JSON.stringify({alg: 'RS256', kid: '5f0c6a3e-2a4b-4f53-9d7e-0b6c1e9f2a11'}));
    const payload = base64Url(
        JSON.stringify({
            aud: 'app-6f1d2c3b-8a9e-4f70-b1c2-d3e4f5a6b7c8',
            exp: 1_800_000_300,
            iat: 1_800_000_000,
            iss: 'https://bytechef.example.com',
            jti: '0d9b6f0e-3c1a-4e8b-9f2d-7a6c5b4e3d2f',
            nbf: 1_800_000_000,
            scope: ['openid', 'profile', 'app:use'],
            sub: 'ada.lovelace@analytical-engines.example.com',
            tenantId: 'public',
        })
    );

    return `${header}.${payload}.${'s'.repeat(342)}`;
}

describe('session', () => {
    afterEach(() => {
        vi.unstubAllEnvs();
    });

    it('seals and unseals a value', async () => {
        const sealer = createSessionSealer(SECRET);
        const sealed = await sealer.seal(SESSION_COOKIE, {accessToken: 'access-1'});

        expect(sealed).not.toContain('access-1');
        await expect(sealer.unseal(SESSION_COOKIE, sealed)).resolves.toEqual({accessToken: 'access-1'});
    });

    it('uses a new nonce for every seal', async () => {
        const sealer = createSessionSealer(SECRET);

        expect(await sealer.seal(SESSION_COOKIE, {a: 1})).not.toBe(await sealer.seal(SESSION_COOKIE, {a: 1}));
    });

    it('treats a tampered, truncated, wrongly keyed or undecodable cookie as no session', async () => {
        const sealer = createSessionSealer(SECRET);
        const sealed = await sealer.seal(SESSION_COOKIE, {accessToken: 'access-1'});
        const lastCharacter = sealed.at(-2) === 'A' ? 'B' : 'A';
        const tampered = `${sealed.slice(0, -2)}${lastCharacter}${sealed.slice(-1)}`;

        for (const candidate of [
            tampered,
            sealed.slice(0, -5),
            sealed.slice(0, sealed.lastIndexOf('.')),
            `v2${sealed.slice(2)}`,
            'not a cookie',
            '',
            'v1.%%%.###',
            'v1..',
        ]) {
            await expect(sealer.unseal(SESSION_COOKIE, candidate)).resolves.toBeUndefined();
        }

        await expect(
            createSessionSealer('another-secret-of-forty-characters-xxxxxx').unseal(SESSION_COOKIE, sealed)
        ).resolves.toBeUndefined();
        await expect(sealer.unseal(PENDING_LOGIN_COOKIE, sealed)).resolves.toBeUndefined();
    });

    it('rejects a session with a missing field or past its end', () => {
        const session = {accessToken: 'a', accessTokenExpiresAt: 2_000, expiresAt: 5_000, refreshToken: 'r'};

        expect(toSession(session, 1_000)).toEqual(session);
        expect(toSession(session, 5_000)).toBeUndefined();
        expect(toSession({...session, accessToken: ''}, 1_000)).toBeUndefined();
        expect(toSession({...session, refreshToken: 7}, 1_000)).toBeUndefined();
        expect(toSession([session], 1_000)).toBeUndefined();
        expect(toSession('session', 1_000)).toBeUndefined();
    });

    it('refuses a missing or short secret', () => {
        vi.stubEnv('BYTECHEF_SESSION_SECRET', '');

        expect(() => resolveSessionSecret(undefined)).toThrowError(/BYTECHEF_SESSION_SECRET.*32 characters/);
        expect(() => resolveSessionSecret('x'.repeat(31))).toThrowError(/32 characters/);
        expect(resolveSessionSecret('x'.repeat(32))).toBe('x'.repeat(32));

        vi.stubEnv('BYTECHEF_SESSION_SECRET', SECRET);

        expect(resolveSessionSecret(undefined)).toBe(SECRET);
    });

    it('sets HttpOnly, SameSite=Lax, Path and Max-Age, and Secure outside localhost', () => {
        const production = serializeCookie(
            SESSION_COOKIE,
            'v',
            cookieOptions(new Request('https://crm.example.com/x'), '/', 3600)
        );

        expect(production).toBe('bytechef_session=v; Path=/; Max-Age=3600; HttpOnly; SameSite=Lax; Secure');

        for (const url of [
            'http://localhost:3000/',
            'http://127.0.0.1:3000/',
            'http://[::1]:3000/',
            'http://crm.localhost/',
        ]) {
            const local = serializeCookie(SESSION_COOKIE, 'v', cookieOptions(new Request(url), '/', 60));

            expect(local).toBe('bytechef_session=v; Path=/; Max-Age=60; HttpOnly; SameSite=Lax');
        }

        expect(
            serializeCookie(SESSION_COOKIE, 'v', cookieOptions(new Request('http://crm.example.com/'), '/', 1))
        ).toContain('Secure');
        expect(clearCookie(SESSION_COOKIE, {path: '/', secure: true})).toBe(
            'bytechef_session=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax; Secure'
        );
    });

    it.each([
        ['https://crm.example', 'http://127.0.0.1:3000/', true],
        ['https://crm.example', 'http://localhost:3000/', true],
        ['http://localhost:3000', 'https://crm.example.com/', false],
        [undefined, 'http://127.0.0.1:3000/', false],
        [undefined, 'https://crm.example.com/', true],
    ])('with appUrl %s and a request to %s sets and clears the cookies with Secure %s', async (appUrl, url, secure) => {
        const store = createSessionStore({appUrl, sessionSecret: SECRET});
        const request = new Request(url);
        const tokens = {accessToken: 'a', expiresAt: null, refreshToken: 'r', scope: null, tokenType: 'Bearer'};
        const cookies = [
            await store.sessionCookie(request, tokens),
            store.clearSessionCookie(request),
            await store.pendingLoginCookie(request, {
                codeVerifier: 'v'.repeat(43),
                redirectUri: 'https://crm.example/auth/callback',
                returnTo: '/',
                state: 's'.repeat(43),
            }),
            store.clearPendingLoginCookie(request),
        ];

        for (const cookie of cookies) {
            expect(cookie.endsWith('; Secure')).toBe(secure);
        }
    });

    it('keeps a realistic sealed session within one cookie', async () => {
        const sealer = createSessionSealer(SECRET);
        const sealed = await sealer.seal(SESSION_COOKIE, {
            accessToken: realisticAccessToken(),
            accessTokenExpiresAt: 1_800_000_300_000,
            expiresAt: 1_800_003_600_000,
            refreshToken: 'r'.repeat(128),
        });
        const cookie = serializeCookie(
            SESSION_COOKIE,
            sealed,
            cookieOptions(new Request('https://crm.example.com/'), '/', 3600)
        );

        expect(cookie.length).toBeLessThan(2500);
        expect(() =>
            serializeCookie(SESSION_COOKIE, 'x'.repeat(MAX_COOKIE_BYTES), {maxAgeSeconds: 1, path: '/', secure: true})
        ).toThrowError(/4096 bytes/);
    });

    it('reads one cookie out of the Cookie header', () => {
        const request = new Request('https://crm.example.com/', {
            headers: {cookie: 'other=1; bytechef_session=v1.a.b; bytechef_session_x=2'},
        });

        expect(readCookie(request, SESSION_COOKIE)).toBe('v1.a.b');
        expect(readCookie(request, PENDING_LOGIN_COOKIE)).toBeUndefined();
        expect(readCookie(new Request('https://crm.example.com/'), SESSION_COOKIE)).toBeUndefined();
    });

    it('refuses a basePath that leaves the origin once normalised', () => {
        expect(normalizeBasePath(undefined)).toBe('/auth');
        expect(normalizeBasePath('/sign-in/')).toBe('/sign-in');

        for (const basePath of ['/.//evil.com', '/a/..//evil.com', '//evil.com', 'auth']) {
            expect(() => normalizeBasePath(basePath)).toThrowError(/basePath/);
        }
    });

    it.each([
        ['/', '/'],
        ['/orders?status=open#top', '/orders?status=open#top'],
        ['/a/../b', '/b'],
        ['//evil.example', '/'],
        ['///evil.example', '/'],
        ['/\\evil.example', '/'],
        ['\\\\evil.example', '/'],
        ['https://evil.example/', '/'],
        ['javascript:alert(1)', '/'],
        ['evil.example', '/'],
        ['/\t/evil.example', '/'],
        ['/\n/evil.example', '/'],
        ['/.//evil.com', '/'],
        ['/%2e%2e//evil.com', '/'],
        ['/a/..//evil.com', '/'],
        ['/./%2e//evil.com', '/'],
        ['/..//evil.com', '/'],
        ['/.//evil.com?next=1#top', '/'],
        ['/a/..//b/c?x=1#y', '/'],
        ['/a/./b/../c?x=%2F%2Fevil#//evil', '/a/c?x=%2F%2Fevil#//evil'],
        ['', '/'],
        [null, '/'],
        [undefined, '/'],
    ])('keeps the return path %j on the origin', (value, expected) => {
        expect(safeReturnPath(value)).toBe(expected);
    });
});
