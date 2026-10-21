import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {jsonResponse} from '../test/mockFetch';
import {
    cookiePair,
    fakeBytechef,
    findSetCookie,
    SESSION_SECRET,
    sessionCookieHeader,
    testSessionStore,
    tokenResponse,
    user,
} from '../test/authServer';
import {
    type BytechefAuthContextI,
    bytechefAuthMiddleware,
    bytechefRequestMiddleware,
    MINIMUM_TANSTACK_START_VERSION,
    requireCsrfMiddlewareFactory,
} from './authMiddleware';
import {SESSION_COOKIE} from './session';

const APP = 'https://crm.example.com';

const NOW = 1_800_000_000_000;

interface RunResultI {
    context?: BytechefAuthContextI;
    response: Response;
}

type Middleware = ReturnType<typeof bytechefAuthMiddleware>;

function middlewareFor(fake: ReturnType<typeof fakeBytechef>): Middleware {
    return bytechefAuthMiddleware({
        appKey: 'key-1',
        fetch: fake.fetch,
        sessionSecret: SESSION_SECRET,
        url: 'https://bc.example',
    });
}

async function run(
    middleware: Middleware,
    path: string,
    cookie?: string,
    handlerType: 'router' | 'serverFn' = 'router',
    beforeNext?: (context: BytechefAuthContextI) => Promise<void>
): Promise<RunResultI> {
    const request = new Request(`${APP}${path}`, {headers: cookie ? {cookie} : {}});
    const pathname = new URL(request.url).pathname;
    let context: BytechefAuthContextI | undefined;

    const next = async (options?: {context?: BytechefAuthContextI}) => {
        context = options?.context;

        if (beforeNext && context) {
            await beforeNext(context);
        }

        return {context, pathname, request, response: new Response('page', {headers: {'x-page': '1'}})};
    };

    const result = (await middleware.options.server!({
        context: {},
        handlerType,
        next,
        pathname,
        request,
    } as never)) as unknown as Response | {response: Response};

    return {context, response: result instanceof Response ? result : result.response};
}

async function userTokenOf(context: BytechefAuthContextI, fake: ReturnType<typeof fakeBytechef>) {
    await context.bytechef.connections.list();

    return fake.calls.at(-1)!.userToken;
}

describe('bytechefAuthMiddleware', () => {
    beforeEach(() => {
        vi.useFakeTimers({now: NOW, toFake: ['Date']});
    });

    afterEach(() => {
        vi.useRealTimers();
        vi.unstubAllEnvs();
    });

    it('redirects a request without a session to sign-in in REQUIRED mode', async () => {
        const fake = fakeBytechef({mode: 'REQUIRED'});
        const {context, response} = await run(middlewareFor(fake), '/orders?status=open');

        expect(context).toBeUndefined();
        expect(response.status).toBe(302);
        expect(response.headers.get('location')).toBe('/auth/login?returnTo=%2Forders%3Fstatus%3Dopen');
        expect(response.headers.get('cache-control')).toBe('no-store');
    });

    it('answers a server function call without a session with 401 in REQUIRED mode', async () => {
        const fake = fakeBytechef({mode: 'REQUIRED'});
        const {context, response} = await run(middlewareFor(fake), '/_serverFn/abc', undefined, 'serverFn');

        expect(context).toBeUndefined();
        expect(response.status).toBe(401);
        expect(await response.json()).toMatchObject({code: 'APP_USER_REQUIRED'});
    });

    it('lets a request without a session through in OPTIONAL mode, without a user', async () => {
        const fake = fakeBytechef({mode: 'OPTIONAL'});
        const {context, response} = await run(middlewareFor(fake), '/orders');

        expect(response.headers.get('x-page')).toBe('1');
        expect(context!.user).toBeUndefined();
        expect(await userTokenOf(context!, fake)).toBeNull();
    });

    it('never reads a session or adds a user token in NONE mode', async () => {
        vi.stubEnv('BYTECHEF_SESSION_SECRET', '');

        const fake = fakeBytechef({mode: 'NONE', users: {'access-1': user('ada')}});
        const cookie = await sessionCookieHeader('access-1', 'refresh-1', NOW + 300_000);
        const {context, response} = await run(
            bytechefAuthMiddleware({appKey: 'key-1', fetch: fake.fetch, url: 'https://bc.example'}),
            '/orders',
            cookie
        );

        expect(response.headers.get('x-page')).toBe('1');
        expect(context!.user).toBeUndefined();
        expect(await userTokenOf(context!, fake)).toBeNull();
        expect(fake.calls.some((call) => call.url.endsWith('/me'))).toBe(false);
    });

    it('refuses to read a session without a session secret', async () => {
        vi.stubEnv('BYTECHEF_SESSION_SECRET', '');

        const fake = fakeBytechef({mode: 'OPTIONAL'});

        await expect(
            run(bytechefAuthMiddleware({appKey: 'key-1', fetch: fake.fetch, url: 'https://bc.example'}), '/orders')
        ).rejects.toThrowError(/BYTECHEF_SESSION_SECRET/);
    });

    it('puts the signed-in user and a client with their token on the context', async () => {
        const fake = fakeBytechef({users: {'access-1': user('ada', 7)}});
        const cookie = await sessionCookieHeader('access-1', 'refresh-1', NOW + 300_000);
        const {context, response} = await run(middlewareFor(fake), '/orders', cookie);

        expect(response.headers.get('x-page')).toBe('1');
        expect(context!.user).toMatchObject({id: 7, login: 'ada'});
        expect(await userTokenOf(context!, fake)).toBe('access-1');
        expect(findSetCookie(response, SESSION_COOKIE)).toBeUndefined();
    });

    it('treats a tampered cookie as no session and clears it', async () => {
        const fake = fakeBytechef({mode: 'REQUIRED', users: {'access-1': user('ada')}});
        const cookie = await sessionCookieHeader('access-1', 'refresh-1', NOW + 300_000);
        const tampered = `${cookie.slice(0, -4)}AAAA`;
        const {response} = await run(middlewareFor(fake), '/orders', tampered);

        expect(response.status).toBe(302);
        expect(response.headers.get('location')).toBe('/auth/login?returnTo=%2Forders');
        expect(findSetCookie(response, SESSION_COOKIE)).toMatch(/Max-Age=0/);
        expect(fake.calls.some((call) => call.url.endsWith('/me'))).toBe(false);
    });

    it('treats a tampered cookie as no session in OPTIONAL mode too', async () => {
        const fake = fakeBytechef({mode: 'OPTIONAL'});
        const {context, response} = await run(middlewareFor(fake), '/orders', `${SESSION_COOKIE}=v1.garbage.value`);

        expect(response.headers.get('x-page')).toBe('1');
        expect(context!.user).toBeUndefined();
        expect(findSetCookie(response, SESSION_COOKIE)).toMatch(/Max-Age=0/);
    });

    it('refreshes an expired access token once and stores the rotated tokens', async () => {
        const fake = fakeBytechef({
            token: () => tokenResponse('access-2', 'refresh-2'),
            users: {'access-2': user('ada')},
        });
        const cookie = await sessionCookieHeader('access-1', 'refresh-1', NOW - 1);
        const {context, response} = await run(middlewareFor(fake), '/orders', cookie);

        expect(fake.tokenCalls()).toHaveLength(1);
        expect(new URLSearchParams(fake.tokenCalls()[0]!.body).get('refresh_token')).toBe('refresh-1');
        expect(context!.user).toMatchObject({login: 'ada'});
        expect(await userTokenOf(context!, fake)).toBe('access-2');

        const stored = findSetCookie(response, SESSION_COOKIE)!;

        expect(stored).toMatch(/; Path=\/; Max-Age=3600; HttpOnly; SameSite=Lax; Secure$/);

        const read = await testSessionStore().readSession(
            new Request(`${APP}/`, {headers: {cookie: cookiePair(stored)}})
        );

        expect(read.session).toMatchObject({accessToken: 'access-2', refreshToken: 'refresh-2'});
    });

    it('marks a refreshed or cleared session cookie Secure from appUrl', async () => {
        const fake = fakeBytechef({
            token: () => tokenResponse('access-2', 'refresh-2'),
            users: {'access-2': user('ada')},
        });
        const middleware = bytechefAuthMiddleware({
            appKey: 'key-1',
            appUrl: 'https://crm.example',
            fetch: fake.fetch,
            sessionSecret: SESSION_SECRET,
            url: 'https://bc.example',
        });
        const cookie = await sessionCookieHeader('access-1', 'refresh-1', NOW - 1);
        const request = (sent: string) =>
            middleware.options.server!({
                context: {},
                handlerType: 'router',
                next: async () => ({response: new Response('page')}),
                pathname: '/orders',
                request: new Request('http://127.0.0.1:3000/orders', {headers: {cookie: sent}}),
            } as never) as unknown as Promise<Response | {response: Response}>;
        const toResponse = (result: Response | {response: Response}) =>
            result instanceof Response ? result : result.response;

        const refreshed = toResponse(await request(cookie));
        const cleared = toResponse(await request(`${SESSION_COOKIE}=v1.garbage.value`));

        expect(findSetCookie(refreshed, SESSION_COOKIE)).toMatch(/Max-Age=3600; HttpOnly; SameSite=Lax; Secure$/);
        expect(findSetCookie(cleared, SESSION_COOKIE)).toMatch(/Max-Age=0; HttpOnly; SameSite=Lax; Secure$/);
    });

    it('refreshes a nearly expired access token', async () => {
        const fake = fakeBytechef({
            token: () => tokenResponse('access-2', 'refresh-2'),
            users: {'access-2': user('ada')},
        });
        const cookie = await sessionCookieHeader('access-1', 'refresh-1', NOW + 10_000);

        await run(middlewareFor(fake), '/orders', cookie);

        expect(fake.tokenCalls()).toHaveLength(1);
    });

    it('shares one refresh between concurrent requests of one session', async () => {
        let release: () => void = () => undefined;
        const released = new Promise<void>((resolve) => {
            release = resolve;
        });
        const fake = fakeBytechef({
            token: async () => {
                await released;

                return tokenResponse('access-2', 'refresh-2');
            },
            users: {'access-2': user('ada')},
        });
        const middleware = middlewareFor(fake);
        const cookie = await sessionCookieHeader('access-1', 'refresh-1', NOW - 1);

        const first = run(middleware, '/a', cookie);
        const second = run(middleware, '/b', cookie);

        await vi.waitFor(() => expect(fake.tokenCalls()).toHaveLength(1));

        release();

        const results = await Promise.all([first, second]);

        expect(fake.tokenCalls()).toHaveLength(1);

        for (const {context, response} of results) {
            expect(context!.user).toMatchObject({login: 'ada'});
            expect(findSetCookie(response, SESSION_COOKIE)).toBeDefined();
        }

        const late = await run(middleware, '/c', cookie);

        expect(fake.tokenCalls()).toHaveLength(1);
        expect(late.context!.user).toMatchObject({login: 'ada'});
    });

    it('ends the session when the refresh is refused', async () => {
        const fake = fakeBytechef({
            mode: 'REQUIRED',
            token: () => jsonResponse({body: {error: 'invalid_grant'}, status: 400}),
        });
        const cookie = await sessionCookieHeader('access-1', 'refresh-1', NOW - 1);
        const {response} = await run(middlewareFor(fake), '/orders', cookie);

        expect(response.status).toBe(302);
        expect(response.headers.get('location')).toBe('/auth/login?returnTo=%2Forders');
        expect(findSetCookie(response, SESSION_COOKIE)).toMatch(/Max-Age=0/);
    });

    it('ends the session in OPTIONAL mode when the refresh is refused, without a redirect', async () => {
        const fake = fakeBytechef({
            mode: 'OPTIONAL',
            token: () => jsonResponse({body: {error: 'invalid_grant'}, status: 400}),
        });
        const cookie = await sessionCookieHeader('access-1', 'refresh-1', NOW - 1);
        const {context, response} = await run(middlewareFor(fake), '/orders', cookie);

        expect(response.headers.get('x-page')).toBe('1');
        expect(context!.user).toBeUndefined();
        expect(findSetCookie(response, SESSION_COOKIE)).toMatch(/Max-Age=0/);
    });

    it('surfaces a misconfigured sign-in as an error instead of a redirect loop', async () => {
        const fake = fakeBytechef({token: () => jsonResponse({body: {error: 'invalid_client'}, status: 401})});
        const cookie = await sessionCookieHeader('access-1', 'refresh-1', NOW - 1);

        await expect(run(middlewareFor(fake), '/orders', cookie)).rejects.toMatchObject({
            code: 'SIGN_IN_MISCONFIGURED',
        });
    });

    it('keeps the rotated tokens when reading the user fails after a refresh', async () => {
        const fake = fakeBytechef({
            token: () => tokenResponse('access-2', 'refresh-2'),
            users: {'access-2': jsonResponse({body: {code: 'INTERNAL', message: 'down'}, status: 500})},
        });
        const cookie = await sessionCookieHeader('access-1', 'refresh-1', NOW - 1);
        const {context, response} = await run(middlewareFor(fake), '/orders', cookie);

        expect(context).toBeUndefined();
        expect(response.status).toBe(503);
        expect(response.headers.get('cache-control')).toBe('no-store');

        const stored = findSetCookie(response, SESSION_COOKIE)!;
        const read = await testSessionStore().readSession(
            new Request(`${APP}/`, {headers: {cookie: cookiePair(stored)}})
        );

        expect(read.session).toMatchObject({accessToken: 'access-2', refreshToken: 'refresh-2'});
    });

    it('surfaces a failure to read the user when no tokens were rotated', async () => {
        const fake = fakeBytechef({
            users: {'access-1': jsonResponse({body: {code: 'INTERNAL', message: 'down'}, status: 500})},
        });
        const cookie = await sessionCookieHeader('access-1', 'refresh-1', NOW + 300_000);

        await expect(run(middlewareFor(fake), '/orders', cookie)).rejects.toThrow();
    });

    it('ends the session when ByteChef rejects the user token', async () => {
        const fake = fakeBytechef({mode: 'REQUIRED', users: {}});
        const cookie = await sessionCookieHeader('access-1', 'refresh-1', NOW + 300_000);
        const {response} = await run(middlewareFor(fake), '/orders', cookie);

        expect(response.status).toBe(302);
        expect(findSetCookie(response, SESSION_COOKIE)).toMatch(/Max-Age=0/);
    });

    it('answers 403 when the signed-in user may not use the instance', async () => {
        const fake = fakeBytechef({
            users: {'access-1': jsonResponse({body: {code: 'APP_USER_FORBIDDEN', message: 'no'}, status: 403})},
        });
        const cookie = await sessionCookieHeader('access-1', 'refresh-1', NOW + 300_000);
        const {context, response} = await run(middlewareFor(fake), '/orders', cookie);

        expect(context).toBeUndefined();
        expect(response.status).toBe(403);
        expect(findSetCookie(response, SESSION_COOKIE)).toBeUndefined();
    });

    it('keeps each request on its own user token and the shared client on none', async () => {
        const fake = fakeBytechef({users: {'access-ada': user('ada', 1), 'access-bob': user('bob', 2)}});
        const middleware = middlewareFor(fake);
        const adaCookie = await sessionCookieHeader('access-ada', 'refresh-ada', NOW + 300_000);
        const bobCookie = await sessionCookieHeader('access-bob', 'refresh-bob', NOW + 300_000);
        let releaseAda: () => void = () => undefined;
        const adaWaits = new Promise<void>((resolve) => {
            releaseAda = resolve;
        });
        const seen: Array<[string, string | null]> = [];

        const ada = run(middleware, '/a', adaCookie, 'router', async (context) => {
            await adaWaits;

            seen.push([context.user!.login, await userTokenOf(context, fake)]);
        });
        const bob = run(middleware, '/b', bobCookie, 'router', async (context) => {
            seen.push([context.user!.login, await userTokenOf(context, fake)]);

            releaseAda();
        });
        const [adaResult, bobResult] = await Promise.all([ada, bob]);
        expect(seen).toEqual([
            ['bob', 'access-bob'],
            ['ada', 'access-ada'],
        ]);
        expect(adaResult.context!.bytechef).not.toBe(bobResult.context!.bytechef);
        expect(
            fake.calls.filter((call) => call.url.endsWith('/context')).every((call) => call.userToken === null)
        ).toBe(true);
    });

    it('leaves the sign-in routes to their handlers', async () => {
        const fake = fakeBytechef({mode: 'REQUIRED'});
        const middleware = middlewareFor(fake);

        for (const path of ['/auth/login', '/auth/callback?code=1&state=2', '/auth/logout']) {
            const {context, response} = await run(middleware, path);

            expect(response.headers.get('x-page')).toBe('1');
            expect(context!.user).toBeUndefined();
        }

        const {response} = await run(middleware, '/authors');

        expect(response.status).toBe(302);
    });

    it('refuses to run inside a server function chain, where it cannot write cookies', async () => {
        const fake = fakeBytechef();

        await expect(
            middlewareFor(fake).options.server!({
                context: {},
                next: async () => ({}),
                request: new Request(`${APP}/_serverFn/abc`),
            } as never)
        ).rejects.toThrowError(/requestMiddleware/);
    });

    it('does not create a client until the server hook runs', () => {
        (globalThis as {window?: unknown}).window = {};

        try {
            expect(() => bytechefAuthMiddleware()).not.toThrow();
        } finally {
            delete (globalThis as {window?: unknown}).window;
        }
    });
});

describe('bytechefRequestMiddleware', () => {
    interface ChainContextI {
        context: Record<string, unknown>;
        handlerType: 'router' | 'serverFn';
        pathname: string;
        request: Request;
    }

    type ChainServer = (options: ChainContextI & {next: (options?: {context?: object}) => Promise<unknown>}) => unknown;

    beforeEach(() => {
        vi.useFakeTimers({now: NOW, toFake: ['Date']});
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    function composed(fake: ReturnType<typeof fakeBytechef>, appUrl?: string) {
        return bytechefRequestMiddleware({
            appKey: 'key-1',
            appUrl,
            fetch: fake.fetch,
            sessionSecret: SESSION_SECRET,
            url: 'https://bc.example',
        });
    }

    async function runChain(
        chain: ReturnType<typeof bytechefRequestMiddleware>,
        request: Request,
        handlerType: 'router' | 'serverFn'
    ): Promise<RunResultI> {
        const servers = chain.map((middleware) => middleware.options.server as unknown as ChainServer);
        const pathname = new URL(request.url).pathname;
        let reached: BytechefAuthContextI | undefined;

        const dispatch = async (index: number, chainContext: ChainContextI): Promise<unknown> => {
            if (index === servers.length) {
                reached = chainContext.context as unknown as BytechefAuthContextI;

                return {context: chainContext.context, pathname, request, response: new Response('handled')};
            }

            return servers[index]!({
                ...chainContext,
                next: (options) =>
                    dispatch(index + 1, {...chainContext, context: {...chainContext.context, ...options?.context}}),
            });
        };

        const result = (await dispatch(0, {context: {}, handlerType, pathname, request})) as
            | Response
            | {response: Response};

        return {context: reached, response: result instanceof Response ? result : result.response};
    }

    it('refuses to start without the CSRF middleware of @tanstack/react-start', () => {
        expect(() => requireCsrfMiddlewareFactory(undefined)).toThrow(
            `@tanstack/react-start ${MINIMUM_TANSTACK_START_VERSION} or later`
        );
        expect(() => requireCsrfMiddlewareFactory('createCsrfMiddleware')).toThrow('createCsrfMiddleware');
    });

    it('accepts the CSRF middleware factory of the installed @tanstack/react-start', () => {
        const factory = () => undefined;

        expect(requireCsrfMiddlewareFactory(factory)).toBe(factory);
    });

    it('registers the CSRF check before the auth middleware', () => {
        const chain = composed(fakeBytechef());

        expect(chain).toHaveLength(2);
        expect(chain.every((middleware) => typeof middleware.options.server === 'function')).toBe(true);
    });

    it.each([
        ['a cross-site Sec-Fetch-Site', {'sec-fetch-site': 'cross-site'}],
        ['a same-site Sec-Fetch-Site', {'sec-fetch-site': 'same-site'}],
        ['a foreign Origin', {origin: 'https://evil.example'}],
        ['no origin information', {}],
    ])('refuses a server function request with %s', async (_label, headers) => {
        const fake = fakeBytechef({mode: 'OPTIONAL', users: {'access-1': user('ada')}});
        const cookie = await sessionCookieHeader('access-1', 'refresh-1', NOW + 300_000);
        const {context, response} = await runChain(
            composed(fake),
            new Request(`${APP}/_serverFn/abc`, {headers: {...headers, cookie}, method: 'POST'}),
            'serverFn'
        );

        expect(response.status).toBe(403);
        expect(context).toBeUndefined();
        expect(fake.calls).toHaveLength(0);
    });

    it('lets a same-origin server function request through to the auth middleware', async () => {
        const fake = fakeBytechef({users: {'access-1': user('ada')}});
        const cookie = await sessionCookieHeader('access-1', 'refresh-1', NOW + 300_000);
        const {context, response} = await runChain(
            composed(fake),
            new Request(`${APP}/_serverFn/abc`, {
                headers: {cookie, 'sec-fetch-site': 'same-origin'},
                method: 'POST',
            }),
            'serverFn'
        );

        expect(await response.text()).toBe('handled');
        expect(context!.user).toMatchObject({login: 'ada'});
    });

    it('accepts the Origin of appUrl behind a proxy that forwards a local host', async () => {
        const fake = fakeBytechef({users: {'access-1': user('ada')}});
        const cookie = await sessionCookieHeader('access-1', 'refresh-1', NOW + 300_000);
        const {context} = await runChain(
            composed(fake, 'https://crm.example'),
            new Request('http://127.0.0.1:3000/_serverFn/abc', {
                headers: {cookie, origin: 'https://crm.example'},
                method: 'POST',
            }),
            'serverFn'
        );

        expect(context!.user).toMatchObject({login: 'ada'});
    });

    it('leaves page requests to the auth middleware, a cross-site navigation included', async () => {
        const fake = fakeBytechef({mode: 'OPTIONAL'});
        const {context, response} = await runChain(
            composed(fake),
            new Request(`${APP}/orders`, {headers: {'sec-fetch-site': 'cross-site'}}),
            'router'
        );

        expect(await response.text()).toBe('handled');
        expect(context!.user).toBeUndefined();
    });
});
